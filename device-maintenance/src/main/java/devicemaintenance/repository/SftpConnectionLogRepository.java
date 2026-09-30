package devicemaintenance.repository;

import devicemaintenance.entity.SftpConnectionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SFTP连接日志Repository
 */
@Repository
public interface SftpConnectionLogRepository extends JpaRepository<SftpConnectionLog, String> {

    /**
     * 根据服务器ID查询日志，按时间倒序
     */
    List<SftpConnectionLog> findByServerIdOrderByCreatedTimeDesc(String serverId);

    /**
     * 根据服务器ID和操作类型查询日志
     */
    List<SftpConnectionLog> findByServerIdAndOperationTypeOrderByCreatedTimeDesc(
        String serverId, SftpConnectionLog.OperationType operationType);

    /**
     * 查询指定时间范围内的日志
     */
    @Query("SELECT log FROM SftpConnectionLog log WHERE log.serverId = :serverId " +
           "AND log.createdTime BETWEEN :startTime AND :endTime " +
           "ORDER BY log.createdTime DESC")
    List<SftpConnectionLog> findLogsByServerIdAndTimeRange(
        @Param("serverId") String serverId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的日志（用于日志清理）
     */
    void deleteByCreatedTimeBefore(LocalDateTime cutoffTime);
}
