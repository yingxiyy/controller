package devicemaintenance.repository;

import devicemaintenance.entity.Batch;
import devicemaintenance.entity.Batch.BatchStatus;
import devicemaintenance.entity.Batch.BatchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 统一批次Repository
 * 
 * 支持所有类型的批次操作：BACKUP, RESTORE, UPGRADE
 */
@Repository
public interface BatchRepository extends JpaRepository<Batch, String> {

    /**
     * 根据批次ID查找
     */
    Optional<Batch> findByBatchId(String batchId);

    /**
     * 根据批次类型查找所有批次
     */
    List<Batch> findByBatchType(BatchType batchType);

    /**
     * 根据批次类型和状态查找
     */
    List<Batch> findByBatchTypeAndStatus(BatchType batchType, BatchStatus status);

    /**
     * 根据状态查找所有批次
     */
    List<Batch> findByStatus(BatchStatus status);

    /**
     * 根据状态查找所有批次，按创建时间倒序
     */
    List<Batch> findByStatusOrderByCreatedTimeDesc(BatchStatus status);

    /**
     * 根据批次动作时间查找
     */
    Optional<Batch> findByBatchActionTime(Long batchActionTime);

    /**
     * 查找所有定时批次（scheduled_time 不为空且在指定时间之前）
     * @param scheduledTime 时间戳（毫秒）
     */
    List<Batch> findByScheduledTimeLessThanEqualAndStatus(
        Long scheduledTime, 
        BatchStatus status
    );

    /**
     * 根据批次类型查找所有批次，按创建时间倒序
     */
    List<Batch> findByBatchTypeOrderByCreatedTimeDesc(BatchType batchType);

    /**
     * 查找所有批次，按创建时间倒序
     */
    List<Batch> findAllByOrderByCreatedTimeDesc();

    /**
     * 根据批次名称模糊查询
     */
    List<Batch> findByBatchNameContaining(String batchName);

    /**
     * 查找指定时间范围内的批次
     */
    List<Batch> findByCreatedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定类型的批次数量
     */
    long countByBatchType(BatchType batchType);

    /**
     * 统计指定类型和状态的批次数量
     */
    long countByBatchTypeAndStatus(BatchType batchType, BatchStatus status);

    /**
     * 删除指定批次
     */
    void deleteByBatchId(String batchId);

    /**
     * 检查批次ID是否存在
     */
    boolean existsByBatchId(String batchId);

    /**
     * 检查批次名称是否存在
     */
    boolean existsByBatchName(String batchName);

    /**
     * 使用原生 SQL 删除所有非执行中状态的批次
     * 
     * 只保留以下执行中状态：
     * - RUNNING
     * - DOWNLOADING
     * - BACKING_UP
     * - UPGRADING
     * - COMMITTING
     * 
     * 其他所有状态（包括旧的 NOT_STARTED、ONGOING、PARTIAL_COMPLETE）都会被删除
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM dm_batch WHERE status NOT IN ('RUNNING', 'DOWNLOADING', 'BACKING_UP', 'UPGRADING', 'COMMITTING')", nativeQuery = true)
    int deleteAllNonRunningBatchesNative();

    /**
     * 使用原生 SQL 查询所有非执行中状态的批次 ID 和名称
     * 用于日志记录，避免 JPA 枚举映射问题
     */
    @Query(value = "SELECT batch_id, batch_name, batch_type, status FROM dm_batch WHERE status NOT IN ('RUNNING', 'DOWNLOADING', 'BACKING_UP', 'UPGRADING', 'COMMITTING')", nativeQuery = true)
    List<Object[]> findAllNonRunningBatchInfoNative();

    /**
     * 查找所有启用每日任务的批次
     * @param batchType 批次类型
     * @param enableDailyTask 是否启用每日任务
     */
    List<Batch> findByBatchTypeAndEnableDailyTask(BatchType batchType, Boolean enableDailyTask);
}

