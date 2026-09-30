package devicemaintenance.repository;

import devicemaintenance.entity.BatchDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 批次设备Repository
 */
@Repository
public interface BatchDeviceRepository extends JpaRepository<BatchDevice, Long> {

    /**
     * 根据批次ID查找所有设备
     */
    List<BatchDevice> findByBatchId(String batchId);

    /**
     * 根据批次ID和设备ID查找设备
     */
    Optional<BatchDevice> findByBatchIdAndDeviceId(String batchId, String deviceId);

    /**
     * 根据批次ID查找设备数量
     */
    long countByBatchId(String batchId);

    /**
     * 删除批次的所有设备
     */
    void deleteByBatchId(String batchId);

    /**
     * 根据批次ID查找设备列表（按创建时间排序）
     */
    List<BatchDevice> findByBatchIdOrderByCreatedTime(String batchId);

    /**
     * 根据批次ID和设备ID列表查找设备
     */
    List<BatchDevice> findByBatchIdAndDeviceIdIn(String batchId, List<String> deviceIds);
}
