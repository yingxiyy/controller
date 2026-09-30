package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.repository.DeviceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 定时任务调度执行器
 * 
 * 功能：
 * 1. 定期扫描数据库中到期的 SCHEDULED 状态任务
 * 2. 异步并发执行到期任务（20线程池）
 * 3. 支持所有任务类型：DOWNLOAD, BACKUP, UPGRADE, RESTORE
 * 
 * 调度策略：
 * - 每分钟执行一次扫描
 * - 按 scheduledTime 升序处理（先到期的先执行）
 * - 异步并发执行，不阻塞主线程
 * - 异常任务自动标记为 FAILED
 * 
 * 注意：
 * - 过期任务（超过1小时）在启动时已标记为EXPIRED
 * - 不使用锁机制，单实例部署
 * - 不支持重试，失败任务直接标记FAILED
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledTaskExecutor {

    private final DeviceTaskRepository deviceTaskRepository;
    private final SoftwareDownloadService softwareDownloadService;
    private final DeviceBackupService deviceBackupService;
    private final DeviceUpgradeService deviceUpgradeService;
    private final DeviceRestoreService deviceRestoreService;
    
    @Qualifier("taskSchedulerExecutor")
    private final Executor taskExecutor;

    /**
     * 每10秒检查一次到期的定时任务
     * 
     * fixedRate = 10000ms = 10秒
     * initialDelay = 10000ms = 启动后10秒开始第一次扫描
     * 
     * ⚠️ 优化：从60秒改为10秒，减少定时任务的最大延迟
     */
    @Scheduled(fixedRate = 10000, initialDelay = 10000)
    public void checkAndExecuteScheduledTasks() {
        Long now = System.currentTimeMillis();
        
        try {
            // 查询所有到期的 SCHEDULED 任务
            List<DeviceTask> scheduledTasks = deviceTaskRepository.findScheduledTasksToExecute(
                DeviceTask.TaskStatus.SCHEDULED, 
                now
            );
            
            if (scheduledTasks.isEmpty()) {
                log.debug("当前没有到期的定时任务");
                return;
            }
            
            log.info("发现 {} 个到期的定时任务，开始异步执行", scheduledTasks.size());
            
            // 异步并发执行所有任务
            for (DeviceTask task : scheduledTasks) {
                taskExecutor.execute(() -> executeScheduledTaskAsync(task));
            }
            
            log.info("{} 个定时任务已提交到线程池异步执行", scheduledTasks.size());
            
        } catch (Exception e) {
            log.error("定时任务调度器执行异常", e);
        }
    }

    /**
     * 异步执行单个定时任务
     * 在独立线程中执行，不阻塞调度器主线程
     * 
     * @param task 待执行的任务
     */
    private void executeScheduledTaskAsync(DeviceTask task) {
        try {
            log.info("开始执行定时任务: taskId={}, taskType={}, deviceId={}, scheduledTime={}, thread={}", 
                     task.getTaskId(), task.getTaskType(), task.getDeviceId(), 
                     task.getScheduledTime(), Thread.currentThread().getName());
            
            // 重新从数据库加载任务，避免并发问题
            DeviceTask freshTask = deviceTaskRepository.findById(task.getTaskId())
                .orElse(null);
            
            if (freshTask == null) {
                log.warn("任务不存在: taskId={}", task.getTaskId());
                return;
            }
            
            // 检查状态，只处理SCHEDULED状态的任务
            if (freshTask.getStatus() != DeviceTask.TaskStatus.SCHEDULED) {
                log.warn("任务状态不是SCHEDULED，跳过执行: taskId={}, status={}", 
                         freshTask.getTaskId(), freshTask.getStatus());
                return;
            }
            
            // 更新任务状态为 PENDING（准备执行）
            freshTask.setStatus(DeviceTask.TaskStatus.PENDING);
            freshTask.setStartedTime(LocalDateTime.now());
            freshTask.setUpdatedTime(LocalDateTime.now());
            freshTask = deviceTaskRepository.save(freshTask);
            
            // 根据任务类型调用对应服务执行
            switch (freshTask.getTaskType()) {
                case DOWNLOAD:
                    softwareDownloadService.executeScheduledTask(freshTask);
                    break;
                    
                case BACKUP:
                    deviceBackupService.executeScheduledTask(freshTask);
                    break;
                    
                case UPGRADE:
                    deviceUpgradeService.executeScheduledTask(freshTask);
                    break;
                    
                case RESTORE:
                    deviceRestoreService.executeScheduledTask(freshTask);
                    break;
                    
                case ROLLBACK:
                case COMMIT:
                    log.warn("任务类型 {} 暂不支持定时执行: taskId={}", 
                             freshTask.getTaskType(), freshTask.getTaskId());
                    freshTask.setStatus(DeviceTask.TaskStatus.FAILED);
                    freshTask.setErrorMessage("This task type does not support scheduled execution");
                    deviceTaskRepository.save(freshTask);
                    break;
                    
                default:
                    log.error("不支持的任务类型: taskId={}, taskType={}", 
                             freshTask.getTaskId(), freshTask.getTaskType());
                    freshTask.setStatus(DeviceTask.TaskStatus.FAILED);
                    freshTask.setErrorMessage("Unsupported task type: " + freshTask.getTaskType());
                    deviceTaskRepository.save(freshTask);
                    break;
            }
            
            log.info("定时任务已提交执行: taskId={}, taskType={}", 
                     freshTask.getTaskId(), freshTask.getTaskType());
            
        } catch (Exception e) {
            log.error("执行定时任务失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
            
            try {
                // 标记任务为失败
                DeviceTask failedTask = deviceTaskRepository.findById(task.getTaskId())
                    .orElse(task);
                failedTask.setStatus(DeviceTask.TaskStatus.FAILED);
                failedTask.setErrorMessage("Scheduled execution failed: " + e.getMessage());
                failedTask.setUpdatedTime(LocalDateTime.now());
                deviceTaskRepository.save(failedTask);
            } catch (Exception ex) {
                log.error("保存失败状态时出错: taskId={}", task.getTaskId(), ex);
            }
        }
    }
}
