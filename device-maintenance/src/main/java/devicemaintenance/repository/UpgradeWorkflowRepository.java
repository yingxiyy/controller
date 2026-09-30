package devicemaintenance.repository;

import devicemaintenance.entity.UpgradeWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 升级工作流 Repository
 */
@Repository
public interface UpgradeWorkflowRepository extends JpaRepository<UpgradeWorkflow, String> {

    /**
     * 根据批次ID查询所有工作流
     */
    List<UpgradeWorkflow> findByBatchId(String batchId);

    /**
     * 根据批次ID和设备ID查询工作流
     */
    Optional<UpgradeWorkflow> findByBatchIdAndDeviceId(String batchId, String deviceId);

    /**
     * 根据设备ID查询所有工作流
     */
    List<UpgradeWorkflow> findByDeviceId(String deviceId);

    // ⚠️ 以下方法已废弃，因为 status 字段不再存储在数据库中
    // status 现在是 @Transient 计算字段，不能用于 Repository 查询
    
    // List<UpgradeWorkflow> findByDeviceIdAndStatusIn(String deviceId, List<UpgradeWorkflow.WorkflowStatus> statuses);
    // List<UpgradeWorkflow> findByBatchIdAndStatus(String batchId, UpgradeWorkflow.WorkflowStatus status);
    // List<UpgradeWorkflow> findByBatchIdAndStatusIn(String batchId, List<UpgradeWorkflow.WorkflowStatus> statuses);

    /**
     * 根据批次ID和当前步骤查询工作流
     */
    List<UpgradeWorkflow> findByBatchIdAndCurrentStep(String batchId, String currentStep);

    // ⚠️ 以下方法已废弃，因为 status 字段不再存储在数据库中
    // @Query("SELECT w.status, COUNT(w) FROM UpgradeWorkflow w WHERE w.batchId = :batchId GROUP BY w.status")
    // List<Object[]> countByStatusForBatch(@Param("batchId") String batchId);
    // long countByBatchIdAndStatus(String batchId, UpgradeWorkflow.WorkflowStatus status);

    /**
     * 统计批次中的工作流总数
     */
    long countByBatchId(String batchId);

    /**
     * 删除批次的所有工作流
     */
    void deleteByBatchId(String batchId);

    /**
     * 检查批次中是否存在指定设备的工作流
     */
    boolean existsByBatchIdAndDeviceId(String batchId, String deviceId);

    // ⚠️ 以下方法已废弃，因为 status 字段不再存储在数据库中
    // 需要先查询所有 workflow，计算状态后再过滤
    // @Query("SELECT w FROM UpgradeWorkflow w WHERE w.batchId = :batchId " +
    //        "AND w.status = 'FAILED' AND w.retryCount < w.maxRetryCount")
    // List<UpgradeWorkflow> findRetryableWorkflows(@Param("batchId") String batchId);
}

