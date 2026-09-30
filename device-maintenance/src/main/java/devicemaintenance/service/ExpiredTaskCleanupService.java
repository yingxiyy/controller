package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.repository.DeviceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 过期任务清理服务
 * 
 * 功能：
 * 1. 应用启动时检测过期的SCHEDULED任务
 * 2. 将过期任务标记为EXPIRED状态
 * 3. 过期阈值可配置（默认1小时）
 * 
 * 说明：
 * - 只在应用启动时执行一次
 * - 过期任务不会被调度器执行
 * - EXPIRED状态的任务可以手动删除
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExpiredTaskCleanupService {

    private final DeviceTaskRepository deviceTaskRepository;

    /**
     * 过期阈值（小时）
     * 默认1小时，可通过配置文件修改
     */
    @Value("${task.scheduler.expiry-threshold-hours:1}")
    private int expiryThresholdHours;

    /**
     * 应用启动完成后执行过期任务检测
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用启动完成，开始检测过期任务...");
        cleanupExpiredTasks();
    }

    /**
     * 清理过期任务
     */
    public void cleanupExpiredTasks() {
        try {
            Long now = System.currentTimeMillis();
            Long expiryTime = now - (expiryThresholdHours * 3600 * 1000);

            log.info("查找过期任务: 当前时间戳={}, 过期阈值={}小时, 截止时间戳={}", 
                     now, expiryThresholdHours, expiryTime);

            // 查找所有过期的SCHEDULED任务
            List<DeviceTask> expiredTasks = deviceTaskRepository
                .findScheduledTasksToExecute(DeviceTask.TaskStatus.SCHEDULED, expiryTime);

            if (expiredTasks.isEmpty()) {
                log.info("未发现过期任务");
                return;
            }

            log.warn("发现 {} 个过期任务，开始标记为EXPIRED状态", expiredTasks.size());

            int count = 0;
            for (DeviceTask task : expiredTasks) {
                try {
                    task.setStatus(DeviceTask.TaskStatus.EXPIRED);
                    task.setErrorMessage(String.format(
                        "任务已过期（计划时间戳: %s, 过期阈值: %d小时）", 
                        task.getScheduledTime(), 
                        expiryThresholdHours
                    ));
                    task.setUpdatedTime(java.time.LocalDateTime.now());
                    deviceTaskRepository.save(task);
                    count++;

                    log.info("标记过期任务: taskId={}, taskType={}, deviceId={}, scheduledTime={}", 
                             task.getTaskId(), task.getTaskType(), task.getDeviceId(), task.getScheduledTime());

                } catch (Exception e) {
                    log.error("标记过期任务失败: taskId={}", task.getTaskId(), e);
                }
            }

            log.info("过期任务处理完成: 总数={}, 成功标记={}", expiredTasks.size(), count);

        } catch (Exception e) {
            log.error("清理过期任务时发生异常", e);
        }
    }
}
