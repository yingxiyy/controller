package devicemaintenance.repository;

import devicemaintenance.entity.DeviceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备任务Repository
 */
@Repository
public interface DeviceTaskRepository extends JpaRepository<DeviceTask, String> {

    /**
     * 根据任务类型查找任务
     */
    List<DeviceTask> findByTaskTypeOrderByCreatedTimeDesc(DeviceTask.TaskType taskType);

    /**
     * 根据批次ID查找所有任务（升序）
     */
    List<DeviceTask> findByBatchIdOrderByCreatedTimeAsc(String batchId);
    
    /**
     * 根据批次ID查找所有任务（倒序） ⭐ 新增
     */
    List<DeviceTask> findByBatchIdOrderByCreatedTimeDesc(String batchId);

    /**
     * 根据设备ID和批次ID查找任务
     */
    Optional<DeviceTask> findByDeviceIdAndBatchId(String deviceId, String batchId);

    /**
     * 根据设备ID和任务类型查找任务
     */
    List<DeviceTask> findByDeviceIdAndTaskType(String deviceId, DeviceTask.TaskType taskType);

    /**
     * 查找指定时间之前创建的任务
     */
    List<DeviceTask> findByCreatedTimeBefore(LocalDateTime beforeTime);

    /**
     * 根据设备ID列表查找任务（按创建时间倒序）
     */
    List<DeviceTask> findByDeviceIdInOrderByCreatedTimeDesc(List<String> deviceIds);

    /**
     * 根据设备ID列表查找任务
     */
    List<DeviceTask> findByDeviceIdIn(List<String> deviceIds);

    /**
     * 查找设备的最新任务
     */
    @Query("SELECT t FROM DeviceTask t WHERE t.deviceId = :deviceId " +
           "ORDER BY t.createdTime DESC")
    List<DeviceTask> findLatestTasksByDevice(@Param("deviceId") String deviceId);

    /**
     * 删除指定时间之前的历史任务
     */
    void deleteByCreatedTimeBefore(LocalDateTime beforeTime);

    /**
     * 根据设备ID、任务类型和状态查找任务
     */
    List<DeviceTask> findByDeviceIdAndTaskTypeAndStatus(String deviceId, DeviceTask.TaskType taskType, DeviceTask.TaskStatus status);

    /**
     * 根据任务类型和状态查找任务
     */
    List<DeviceTask> findByTaskTypeAndStatus(DeviceTask.TaskType taskType, DeviceTask.TaskStatus status);

    /**
     * 根据设备ID查找任务（按创建时间倒序）
     */
    List<DeviceTask> findByDeviceIdOrderByCreatedTimeDesc(String deviceId);

    /**
     * 根据状态查找任务（按创建时间倒序）
     */
    List<DeviceTask> findByStatusOrderByCreatedTimeDesc(DeviceTask.TaskStatus status);

    /**
     * 查找所有任务（按创建时间倒序）
     */
    List<DeviceTask> findAllByOrderByCreatedTimeDesc();

    /**
     * 检查设备是否已有进行中的恢复任务
     */
    boolean existsByDeviceIdAndTaskTypeAndStatus(String deviceId, DeviceTask.TaskType taskType, DeviceTask.TaskStatus status);

    /**
     * 根据设备ID和状态查找任务
     */
    List<DeviceTask> findByDeviceIdAndStatus(String deviceId, DeviceTask.TaskStatus status);

    /**
     * 查找到期的定时任务
     * 用于调度器扫描需要执行的SCHEDULED任务
     * @param currentTime 当前时间戳（毫秒）
     */
    @Query("SELECT t FROM DeviceTask t WHERE t.status = :status " +
           "AND t.scheduledTime IS NOT NULL " +
           "AND t.scheduledTime <= :currentTime " +
           "ORDER BY t.scheduledTime ASC")
    List<DeviceTask> findScheduledTasksToExecute(
        @Param("status") DeviceTask.TaskStatus status,
        @Param("currentTime") Long currentTime);

    /**
     * 根据设备名称模糊查找任务（用于批次任务查询）
     */
    List<DeviceTask> findByDeviceNameContainingOrderByCreatedTimeAsc(String deviceName);

    /**
     * 根据batchId和设备名称模糊查找任务
     */
    List<DeviceTask> findByBatchIdAndDeviceNameContainingOrderByCreatedTimeAsc(String batchId, String deviceName);

    /**
     * 根据batchId和设备ID查找任务列表
     */
    List<DeviceTask> findByBatchIdAndDeviceIdOrderByCreatedTimeAsc(String batchId, String deviceId);
    
    /**
     * 根据batchId和设备ID查找任务列表（简化版，不排序）
     */
    List<DeviceTask> findByBatchIdAndDeviceId(String batchId, String deviceId);

    /**
     * 根据设备名称和设备ID模糊查找任务
     */
    List<DeviceTask> findByDeviceNameContainingAndDeviceIdOrderByCreatedTimeAsc(String deviceName, String deviceId);

    /**
     * 根据batchId、设备名称和设备ID查找任务
     */
    List<DeviceTask> findByBatchIdAndDeviceNameContainingAndDeviceIdOrderByCreatedTimeAsc(String batchId, String deviceName, String deviceId);
    
    /**
     * 查找设备的指定类型的未完成任务（用于通知匹配）
     * 状态：NOT_START, PENDING, SCHEDULED, RUNNING
     * 注意：虽然 NOT_START 任务通常不会收到通知，但为了完整性包含它
     */
    @Query("SELECT t FROM DeviceTask t WHERE t.deviceId = :deviceId " +
           "AND t.taskType = :taskType " +
           "AND t.status IN ('NOT_START', 'PENDING', 'SCHEDULED', 'RUNNING') " +
           "ORDER BY t.createdTime DESC")
    List<DeviceTask> findIncompleteTasksByDeviceAndType(
        @Param("deviceId") String deviceId, 
        @Param("taskType") DeviceTask.TaskType taskType);
    
    /**
     * 查找设备的指定类型的最近完成任务（用于复用）
     * 状态：COMPLETED, FAILED, CANCELLED
     */
    @Query("SELECT t FROM DeviceTask t WHERE t.deviceId = :deviceId " +
           "AND t.taskType = :taskType " +
           "AND t.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "ORDER BY t.completedTime DESC")
    List<DeviceTask> findCompletedTasksByDeviceAndType(
        @Param("deviceId") String deviceId, 
        @Param("taskType") DeviceTask.TaskType taskType);
    
    /**
     * 根据工作流ID查找所有关联的任务
     */
    List<DeviceTask> findByWorkflowId(String workflowId);
    
    /**
     * 查找设备的所有未完成任务（不限类型）
     * 
     * 用于处理 upgrade.upgrade-state=COMPLETE 通知时，判断是 ROLLBACK 还是 UPGRADE 完成
     * 
     * @param deviceId 设备ID
     * @param statuses 状态列表（PENDING, SCHEDULED, RUNNING）
     * @return 按创建时间倒序排列的任务列表
     */
    @Query("SELECT t FROM DeviceTask t WHERE t.deviceId = :deviceId " +
           "AND t.status IN :statuses " +
           "ORDER BY t.createdTime DESC")
    List<DeviceTask> findByDeviceIdAndStatusIn(
        @Param("deviceId") String deviceId, 
        @Param("statuses") List<DeviceTask.TaskStatus> statuses);
    
    /**
     * 查找长时间运行的任务
     *
     * 用于轮询机制：查找指定时间之前开始、仍在 RUNNING 状态的任务。
     * 主要用于升级批次中的各步骤（DOWNLOAD/BACKUP/UPGRADE/COMMIT/ROLLBACK/RESTORE）。
     *
     * @param taskTypes 任务类型列表
     * @param status 任务状态（RUNNING）
     * @param startedBefore 开始时间早于此时间点的任务
     * @return 符合条件的任务列表
     */
    @Query("SELECT t FROM DeviceTask t WHERE t.taskType IN :taskTypes " +
           "AND t.status = :status " +
           "AND t.startedTime IS NOT NULL " +
           "AND t.startedTime < :startedBefore " +
           "ORDER BY t.startedTime ASC")
    List<DeviceTask> findLongRunningTasks(
        @Param("taskTypes") List<DeviceTask.TaskType> taskTypes,
        @Param("status") DeviceTask.TaskStatus status,
        @Param("startedBefore") LocalDateTime startedBefore);
}
