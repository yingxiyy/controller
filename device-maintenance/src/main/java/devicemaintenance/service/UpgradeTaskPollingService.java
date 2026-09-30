package devicemaintenance.service;

import devicemaintenance.dto.DeviceOperationStatus;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.listener.SystemChangeNotificationListener;
import devicemaintenance.repository.DeviceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 升级批次任务状态轮询服务
 * 
 * 功能说明：
 * - 定时扫描长时间运行的升级批次步骤任务
 * - 主动查询MongoDB获取设备实际状态
 * - 作为Kafka通知的容错机制，确保任务状态最终一致
 * - 对超时任务自动标记为失败，触发workflow和batch更新
 * 
 * 触发条件：
 * - 任务启动超过指定时间（可配置）后开始轮询
 * - 任务状态仍为RUNNING
 * - 任务类型为DOWNLOAD、BACKUP、UPGRADE、COMMIT、ROLLBACK或RESTORE
 * 
 * 轮询频率：
 * - 每5分钟扫描一次（可配置）
 * 
 * 超时策略：
 * - 任务运行超过最大等待时间（可配置）仍未完成则标记为失败
 * - 失败处理与RPC失败保持一致（更新workflow、batch、发送TaskInfo通知）
 * 
 * 防重机制：
 * - 幂等性保证：只更新RUNNING状态的任务
 * - 与Kafka通知路径使用相同的状态更新逻辑
 * - 重新加载任务状态，避免并发冲突
 * 
 * MongoDB状态字段：
 * - DOWNLOAD: download.download-state (COMPLETE/FAIL)
 * - BACKUP: backup.backup-state (COMPLETE/FAIL)
 * - UPGRADE/ROLLBACK/COMMIT: upgrade.upgrade-state
 * - RESTORE: restore.restore-state (COMPLETE/FAIL)
 * 
 * 配置项（application.yml）：
 * device-maintenance:
 *   polling:
 *     enabled: true                    # 是否启用轮询（默认true）
 *     fixed-rate-ms: 300000            # 轮询频率（毫秒，5分钟=300000）
 *     task-timeout-minutes: 5          # 首次轮询阈值（分钟，默认5）
 *     max-wait-minutes: 60             # 最大等待时间（分钟，默认60）
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "device-maintenance.polling",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true  // 默认启用
)
public class UpgradeTaskPollingService {

    private final DeviceTaskRepository deviceTaskRepository;
    private final DeviceMaintenanceStatusService maintenanceStatusService;
    private final SystemChangeNotificationListener systemChangeNotificationListener;
    private final TaskInfoNotificationService taskInfoNotificationService;

    /**
     * 任务超时阈值（分钟）
     * 超过此时间的RUNNING任务才会被轮询
     */
    @Value("${device-maintenance.polling.task-timeout-minutes:5}")
    private int taskTimeoutMinutes;

    /**
     * 最大等待时间（分钟）
     * 任务运行超过此时间仍未完成则标记为失败
     */
    @Value("${device-maintenance.polling.max-wait-minutes:60}")
    private int maxWaitMinutes;

    /**
     * 定时扫描长时间运行的升级批次步骤任务
     * 
     * 执行频率：可配置（默认10分钟）
     * 
     * 处理逻辑：
     * 1. 查询启动超过指定阈值且仍为RUNNING的任务
     * 2. 检查任务是否超过最大等待时间，是则标记失败
     * 3. 未超时的任务查询MongoDB获取实际状态
     * 4. 根据MongoDB状态更新任务和workflow/batch
     * 
     * 注意：
     * - 使用 fixedRate 而不是 cron，确保固定间隔执行
     * - 默认10分钟 = 600000毫秒
     */
    @Scheduled(fixedRateString = "${device-maintenance.polling.fixed-rate-ms:600000}")
    public void pollLongRunningUpgradeTasks() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 [轮询] 开始扫描长时间运行的升级批次步骤任务");
        log.info("  首次轮询阈值: {}分钟, 最大等待时间: {}分钟", taskTimeoutMinutes, maxWaitMinutes);
        
        try {
            // 1. 计算超时时间点
            LocalDateTime timeoutCutoff = LocalDateTime.now().minusMinutes(taskTimeoutMinutes);
            
            // 2. 查询超时的 RUNNING 任务（升级批次中的各步骤）
            List<DeviceTask> longRunningTasks = deviceTaskRepository
                .findLongRunningTasks(
                    Arrays.asList(
                        DeviceTask.TaskType.DOWNLOAD,
                        DeviceTask.TaskType.BACKUP,
                        DeviceTask.TaskType.UPGRADE,
                        DeviceTask.TaskType.COMMIT,
                        DeviceTask.TaskType.ROLLBACK,
                        DeviceTask.TaskType.RESTORE
                    ),
                    DeviceTask.TaskStatus.RUNNING,
                    timeoutCutoff
                );
            
            if (longRunningTasks.isEmpty()) {
                log.info("  ✅ 没有需要轮询的超时任务");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return;
            }
            
            log.info("  📋 发现 {} 个超时任务需要检查", longRunningTasks.size());
            
            // 3. 逐个检查任务状态
            int checkedCount = 0;
            int updatedCount = 0;
            int skippedCount = 0;
            int timedOutCount = 0;
            int errorCount = 0;
            
            for (DeviceTask task : longRunningTasks) {
                checkedCount++;
                try {
                    TaskCheckResult result = checkAndUpdateTaskStatus(task);
                    switch (result) {
                        case UPDATED:
                            updatedCount++;
                            break;
                        case TIMED_OUT:
                            timedOutCount++;
                            break;
                        case SKIPPED:
                            skippedCount++;
                            break;
                        case ERROR:
                            errorCount++;
                            break;
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error("  ❌ 检查任务失败: taskId={}, deviceId={}, error={}", 
                        task.getTaskId(), task.getDeviceId(), e.getMessage());
                }
            }
            
            // 4. 输出统计
            log.info("  📊 轮询统计: 检查={}个, 更新={}个, 超时失败={}个, 跳过={}个, 错误={}个", 
                checkedCount, updatedCount, timedOutCount, skippedCount, errorCount);
            
        } catch (Exception e) {
            log.error("  ❌ 轮询任务执行异常: {}", e.getMessage(), e);
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 任务检查结果枚举
     */
    private enum TaskCheckResult {
        UPDATED,     // 任务状态已更新（完成或失败）
        TIMED_OUT,   // 任务超时被标记为失败
        SKIPPED,     // 跳过（状态已变更或仍在进行中）
        ERROR        // 检查过程出错
    }

    /**
     * 检查并更新单个任务的状态
     * 
     * @param task 待检查的任务
     * @return TaskCheckResult 检查结果
     */
    private TaskCheckResult checkAndUpdateTaskStatus(DeviceTask task) {
        String deviceId = task.getDeviceId();
        String taskId = task.getTaskId();
        DeviceTask.TaskType taskType = task.getTaskType();
        
        // 计算运行时长
        long runningMinutes = 0;
        if (task.getStartedTime() != null) {
            runningMinutes = Duration.between(task.getStartedTime(), LocalDateTime.now()).toMinutes();
        }
        
        log.info("  🔍 检查任务: taskId={}, type={}, deviceId={}, 运行时长={}分钟", 
            taskId, taskType, deviceId, runningMinutes);

        // 1. 先查询设备真实状态，避免“设备已完成但通知丢失”时被直接判定为超时失败
        DeviceOperationStatus deviceStatus;
        try {
            deviceStatus = maintenanceStatusService.getDeviceOperationStatus(deviceId);
        } catch (Exception e) {
            log.warn("    ⚠️ 无法获取设备状态: deviceId={}, error={}", deviceId, e.getMessage());
            if (runningMinutes > maxWaitMinutes) {
                log.warn("    ⏰ 获取设备状态失败且任务已超时: 运行时长={}分钟, 最大等待={}分钟, 标记为失败",
                    runningMinutes, maxWaitMinutes);
                return handleTimedOutTask(task, runningMinutes);
            }
            return TaskCheckResult.ERROR;
        }
        
        if (deviceStatus == null) {
            log.warn("    ⚠️ 设备不存在或无状态信息: deviceId={}", deviceId);
            if (runningMinutes > maxWaitMinutes) {
                log.warn("    ⏰ 设备无状态信息且任务已超时: 运行时长={}分钟, 最大等待={}分钟, 标记为失败",
                    runningMinutes, maxWaitMinutes);
                return handleTimedOutTask(task, runningMinutes);
            }
            return TaskCheckResult.ERROR;
        }
        
        // 2. 根据任务类型获取对应的状态字段
        String mongoState = extractStateByTaskType(taskType, deviceStatus);
        String stateFieldName = resolveStateFieldName(taskType);
        
        if (mongoState == null) {
            if (runningMinutes > maxWaitMinutes) {
                log.warn("    ⏰ 设备状态存在但{}为空且任务已超时: 运行时长={}分钟, 最大等待={}分钟, 标记为失败",
                    stateFieldName, runningMinutes, maxWaitMinutes);
                return handleTimedOutTask(task, runningMinutes);
            }
            log.debug("    ℹ️ 设备无{}状态信息", stateFieldName);
            return TaskCheckResult.SKIPPED;
        }
        
        log.info("    📡 MongoDB {}={}", stateFieldName, mongoState);
        
        // 3. 判断是否完成
        DeviceTask.TaskStatus newStatus = determineTaskStatus(task, mongoState, taskType, deviceStatus);
        
        if (newStatus == null) {
            if (runningMinutes > maxWaitMinutes) {
                log.warn("    ⏰ 任务运行超时且设备仍未进入终态: 运行时长={}分钟, 最大等待={}分钟, 当前设备状态={}",
                    runningMinutes, maxWaitMinutes, mongoState);
                return handleTimedOutTask(task, runningMinutes);
            }
            log.debug("    ⏳ 任务仍在进行中: {}={}", stateFieldName, mongoState);
            return TaskCheckResult.SKIPPED;
        }
        
        // 4. 更新任务状态（幂等操作）
        boolean updated = updateTaskStatusIfStillRunning(task, newStatus, mongoState);
        return updated ? TaskCheckResult.UPDATED : TaskCheckResult.SKIPPED;
    }

    /**
     * 处理超时任务：标记为失败并触发workflow更新
     * 
     * @param task 超时的任务
     * @param runningMinutes 运行时长
     * @return TaskCheckResult.TIMED_OUT
     */
    @Transactional
    public TaskCheckResult handleTimedOutTask(DeviceTask task, long runningMinutes) {
        String taskId = task.getTaskId();
        
        // 1. 重新从数据库加载，确保是最新状态
        Optional<DeviceTask> freshTaskOpt = deviceTaskRepository.findById(taskId);
        if (!freshTaskOpt.isPresent()) {
            log.warn("    ⚠️ 任务不存在: taskId={}", taskId);
            return TaskCheckResult.ERROR;
        }
        
        DeviceTask freshTask = freshTaskOpt.get();
        DeviceTask.TaskStatus currentStatus = freshTask.getStatus();
        
        // 2. 幂等性检查：只有RUNNING状态的任务才标记失败
        if (currentStatus != DeviceTask.TaskStatus.RUNNING) {
            log.info("    ℹ️ 任务状态已变更: taskId={}, currentStatus={}, 跳过超时处理", 
                taskId, currentStatus);
            return TaskCheckResult.SKIPPED;
        }
        
        // 3. 标记任务为失败（与RPC失败处理方式一致）
        freshTask.setStatus(DeviceTask.TaskStatus.FAILED);
        freshTask.setCompletedTime(LocalDateTime.now());
        freshTask.setErrorMessage(String.format(
            "%s step timed out: running time=%d minute(s), max wait time=%d minute(s), completion notification not received",
            freshTask.getTaskType(),
            runningMinutes,
            maxWaitMinutes));
        
        deviceTaskRepository.save(freshTask);
        
        log.error("    ❌ 任务已标记为失败（超时）: taskId={}, type={}, deviceId={}, 运行时长={}分钟", 
            taskId, freshTask.getTaskType(), freshTask.getDeviceId(), runningMinutes);
        
        // 4. 触发 workflow 和 batch 更新（与RPC失败和Kafka通知路径保持一致）
        triggerWorkflowUpdateForFailure(freshTask);
        
        return TaskCheckResult.TIMED_OUT;
    }

    /**
     * 从设备状态中提取下载状态
     */
    private String extractDownloadState(DeviceOperationStatus deviceStatus) {
        if (deviceStatus.getSoftwareOperations() == null) {
            return null;
        }

        DeviceOperationStatus.SoftwareOperations.SoftwareDownload download =
            deviceStatus.getSoftwareOperations().getDownload();

        if (download == null) {
            return null;
        }

        return download.getState();
    }

    /**
     * 从设备状态中提取备份状态
     */
    private String extractBackupState(DeviceOperationStatus deviceStatus) {
        if (deviceStatus.getDatabaseOperations() == null) {
            return null;
        }

        DeviceOperationStatus.DatabaseOperations.DatabaseBackup backup =
            deviceStatus.getDatabaseOperations().getBackup();

        if (backup == null) {
            return null;
        }

        return backup.getState();
    }

    /**
     * 从设备状态中提取升级状态（用于 UPGRADE/ROLLBACK/COMMIT 任务）
     */
    private String extractUpgradeState(DeviceOperationStatus deviceStatus) {
        if (deviceStatus.getSoftwareOperations() == null) {
            return null;
        }
        
        DeviceOperationStatus.SoftwareOperations.SoftwareUpgrade upgrade = 
            deviceStatus.getSoftwareOperations().getUpgrade();
        
        if (upgrade == null) {
            return null;
        }
        
        return upgrade.getState();
    }

    /**
     * 从设备状态中提取恢复状态（用于 RESTORE 任务）
     */
    private String extractRestoreState(DeviceOperationStatus deviceStatus) {
        if (deviceStatus.getDatabaseOperations() == null) {
            return null;
        }
        
        DeviceOperationStatus.DatabaseOperations.DatabaseRestore restore = 
            deviceStatus.getDatabaseOperations().getRestore();
        
        if (restore == null) {
            return null;
        }
        
        return restore.getState();
    }

    /**
     * 根据MongoDB状态和任务类型判断任务应该的状态
     * 
     * 状态映射：
     * 
     * DOWNLOAD (download.download-state):
     * - COMPLETE → null（回滚后的当前状态快照不能证明本次下载成功）
     * - FAIL/FAILED → FAILED
     * - 其他状态 → null（仍在进行中）
     * 
     * BACKUP (backup.backup-state):
     * - COMPLETE → COMPLETED
     * - FAIL/FAILED → FAILED
     * - 其他状态 → null（仍在进行中）
     * 
     * UPGRADE/ROLLBACK/COMMIT (upgrade.upgrade-state):
     * - UPGRADE: ACTIVE_COMPLETE → 校验 current-software 是否等于 targetVersion，匹配才算 COMPLETED
     * - UPGRADE: COMPLETE → 属于后续 COMMIT 阶段状态，忽略，不更新 UPGRADE 任务
     * - COMMIT: COMPLETE → COMPLETED
     * - ROLLBACK: COMPLETE → COMPLETED
     * - FAIL → FAILED（升级失败）
     * - ROLLBACK-FAIL → FAILED（回滚失败，仅对ROLLBACK任务）
     * - 其他状态（ACTIVE, ROLLBACK等）→ null（仍在进行中）
     * 
     * RESTORE (restore.restore-state):
     * - COMPLETE → COMPLETED（恢复成功）
     * - FAIL → FAILED（恢复失败）
     * - 其他状态（RESTORING等）→ null（仍在进行中）
     * 
     * @param mongoState MongoDB中的状态值
     * @param taskType 任务类型
     * @return 新的任务状态，如果不需要更新返回null
     */
    DeviceTask.TaskStatus determineTaskStatus(
            DeviceTask task,
            String mongoState,
            DeviceTask.TaskType taskType,
            DeviceOperationStatus deviceStatus) {
        if (mongoState == null) {
            return null;
        }
        
        String stateUpper = normalizeState(mongoState);

        if (taskType == DeviceTask.TaskType.DOWNLOAD) {
            if ("COMPLETE".equals(stateUpper)) {
                log.info(
                    "    ℹ️ 轮询看到 DOWNLOAD COMPLETE，但当前状态快照无法证明本次下载成功，保持等待: taskId={}",
                    task.getTaskId()
                );
                return null;
            }
            if ("FAIL".equals(stateUpper) || "FAILED".equals(stateUpper)) {
                return DeviceTask.TaskStatus.FAILED;
            }
            return null;
        }

        if (taskType == DeviceTask.TaskType.BACKUP) {
            if ("COMPLETE".equals(stateUpper)) {
                return DeviceTask.TaskStatus.COMPLETED;
            }
            if ("FAIL".equals(stateUpper) || "FAILED".equals(stateUpper)) {
                return DeviceTask.TaskStatus.FAILED;
            }
            return null;
        }
        
        // ========== RESTORE 任务状态判断 ==========
        if (taskType == DeviceTask.TaskType.RESTORE) {
            // 恢复完成
            if ("COMPLETE".equals(stateUpper)) {
                return DeviceTask.TaskStatus.COMPLETED;
            }
            // 恢复失败
            if ("FAIL".equals(stateUpper) || "FAILED".equals(stateUpper)) {
                return DeviceTask.TaskStatus.FAILED;
            }
            // 其他状态（RESTORING等）表示仍在进行中
            return null;
        }
        
        // ========== UPGRADE/ROLLBACK/COMMIT 任务状态判断 ==========
        if ("ACTIVE_COMPLETE".equals(stateUpper)
                && taskType == DeviceTask.TaskType.UPGRADE) {
            return determineUpgradeCompletionAfterActivation(task, deviceStatus, stateUpper);
        }

        if ("COMPLETE".equals(stateUpper)) {
            if (taskType == DeviceTask.TaskType.UPGRADE) {
                log.debug(
                    "    ℹ️ 轮询看到 COMPLETE，但该状态属于 COMMIT 后状态，忽略 UPGRADE 任务更新: taskId={}, targetVersion={}",
                    task.getTaskId(), task.getTargetVersion());
                return null;
            }
            return DeviceTask.TaskStatus.COMPLETED;
        }
        
        // 失败状态
        if ("FAIL".equals(stateUpper) || "FAILED".equals(stateUpper)) {
            return DeviceTask.TaskStatus.FAILED;
        }
        
        // 回滚失败（仅对ROLLBACK任务）
        if ("ROLLBACK-FAIL".equals(stateUpper) && taskType == DeviceTask.TaskType.ROLLBACK) {
            return DeviceTask.TaskStatus.FAILED;
        }
        
        // 其他状态（ACTIVE, ROLLBACK等）表示仍在进行中
        return null;
    }

    private DeviceTask.TaskStatus determineUpgradeCompletionAfterActivation(
            DeviceTask task,
            DeviceOperationStatus deviceStatus,
            String upgradeState) {
        String targetVersion = resolveExpectedUpgradeVersion(task, deviceStatus);
        String currentSoftware = deviceStatus != null ? deviceStatus.getCurrentSoftware() : null;

        if (targetVersion == null || targetVersion.trim().isEmpty()) {
            log.warn(
                "    ⚠️ 轮询看到 {}，但缺少可用的目标版本信息，无法校验激活后版本，按完成处理: taskId={}, currentSoftware={}, taskTargetVersion={}",
                upgradeState, task.getTaskId(), currentSoftware, task.getTargetVersion());
            return DeviceTask.TaskStatus.COMPLETED;
        }

        if (currentSoftware == null || currentSoftware.trim().isEmpty()) {
            log.info(
                "    ℹ️ 轮询看到 {}，但当前版本尚未同步，继续等待: taskId={}, targetVersion={}",
                upgradeState, task.getTaskId(), targetVersion);
            return null;
        }

        task.setCurrentVersion(currentSoftware);
        if (isVersionMatch(currentSoftware, targetVersion)) {
            return DeviceTask.TaskStatus.COMPLETED;
        }

        log.warn(
            "    ❌ 轮询确认升级未生效，设备已回退或启动失败: taskId={}, upgradeState={}, currentSoftware={}, targetVersion={}",
            task.getTaskId(), upgradeState, currentSoftware, targetVersion);
        return DeviceTask.TaskStatus.FAILED;
    }

    private String resolveExpectedUpgradeVersion(DeviceTask task, DeviceOperationStatus deviceStatus) {
        if (deviceStatus != null
            && deviceStatus.getSoftwareOperations() != null
            && deviceStatus.getSoftwareOperations().getDownload() != null) {
            String downloadVersion = deviceStatus.getSoftwareOperations().getDownload().getSoftwareVersion();
            if (downloadVersion != null && !downloadVersion.trim().isEmpty()) {
                return downloadVersion.trim();
            }
        }
        return task.getTargetVersion();
    }

    private boolean isVersionMatch(String currentSoftware, String targetVersion) {
        return normalizeVersion(currentSoftware).equals(normalizeVersion(targetVersion));
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }

        String normalized = version.trim().toUpperCase().replace('-', '_');
        if (normalized.startsWith("RELEASE_")) {
            normalized = normalized.substring("RELEASE_".length());
        } else if (normalized.startsWith("PACKAGE_")) {
            normalized = normalized.substring("PACKAGE_".length());
        } else if (normalized.startsWith("SOFTWARE_")) {
            normalized = normalized.substring("SOFTWARE_".length());
        }
        return normalized;
    }

    private String normalizeState(String mongoState) {
        return mongoState.trim().toUpperCase().replace('-', '_');
    }

    /**
     * 幂等更新任务状态
     * 
     * 只有当任务仍为RUNNING状态时才更新，防止与Kafka通知冲突
     * 
     * @param task 任务对象
     * @param newStatus 新状态
     * @param mongoState MongoDB中的状态值（用于日志）
     * @return true 如果更新成功，false 如果跳过
     */
    @Transactional
    public boolean updateTaskStatusIfStillRunning(
            DeviceTask task, 
            DeviceTask.TaskStatus newStatus, 
            String mongoState) {
        
        String taskId = task.getTaskId();
        
        // 1. 重新从数据库加载，确保是最新状态（防止并发问题）
        Optional<DeviceTask> freshTaskOpt = deviceTaskRepository.findById(taskId);
        if (!freshTaskOpt.isPresent()) {
            log.warn("    ⚠️ 任务不存在: taskId={}", taskId);
            return false;
        }
        
        DeviceTask freshTask = freshTaskOpt.get();
        DeviceTask.TaskStatus currentStatus = freshTask.getStatus();
        
        // 2. 幂等性检查：只有RUNNING状态的任务才更新
        if (currentStatus != DeviceTask.TaskStatus.RUNNING) {
            log.info("    ℹ️ 任务状态已变更（可能Kafka通知已到达）: " +
                "taskId={}, currentStatus={}, 跳过轮询更新", 
                taskId, currentStatus);
            return false;
        }
        
        // 3. 更新状态和错误信息
        freshTask.setStatus(newStatus);
        freshTask.setCompletedTime(LocalDateTime.now());
        if (task.getCurrentVersion() != null) {
            freshTask.setCurrentVersion(task.getCurrentVersion());
        }
        
        // 设置错误信息（说明状态更新来源）
        if (newStatus == DeviceTask.TaskStatus.FAILED && task.getTargetVersion() != null
                && freshTask.getCurrentVersion() != null) {
            freshTask.setErrorMessage(String.format(
                "Activation did not take effect after reboot: currentSoftware=%s, targetVersion=%s [Polling update]",
                freshTask.getCurrentVersion(), task.getTargetVersion()));
        } else {
            String pollingRemark = String.format(
                "[Polling update] MongoDB state=%s, updated at=%s",
                mongoState,
                LocalDateTime.now().toString());
            freshTask.setErrorMessage(pollingRemark);
        }
        
        deviceTaskRepository.save(freshTask);
        
        log.info("    ✅ 任务状态已更新（轮询）: taskId={}, {} → {}, MongoDB状态={}", 
            taskId, DeviceTask.TaskStatus.RUNNING, newStatus, mongoState);
        
        // 4. 触发 workflow 和 batch 更新（与Kafka通知路径保持一致）
        // ⭐ 如果任务属于批次且没有workflow，触发批次更新
        // ⚠️ 如果任务属于workflow，通过 checkAndUpdateUpgradeWorkflow() 更新
        if (freshTask.getBatchId() != null && freshTask.getWorkflowId() == null) {
            // 批次任务（BACKUP/RESTORE/ROLLBACK）：触发批次状态聚合
            triggerBatchUpdate(freshTask);
        } else if (freshTask.getWorkflowId() != null) {
            // Workflow任务（UPGRADE with DOWNLOAD/BACKUP/UPGRADE）：触发workflow更新
            triggerWorkflowUpdate(freshTask, mongoState);
        } else {
            notifySingleTaskCompleted(freshTask);
        }
        
        return true;
    }

    private void notifySingleTaskCompleted(DeviceTask task) {
        try {
            taskInfoNotificationService.notifyTaskCompleted(task);
            log.info("    TaskInfo single-task completion notification sent: taskId={}, status={}",
                    task.getTaskId(), task.getStatus());
        } catch (Exception e) {
            log.warn("    Failed to send TaskInfo single-task completion notification: taskId={}, error={}",
                    task.getTaskId(), e.getMessage());
        }
    }

    /**
     * 触发 workflow 和 batch 更新
     * 
     * ⚠️ 与 SystemChangeNotificationListener 保持一致：
     * - 不发送单任务级别的 TaskInfo 通知
     * - 而是通过 checkAndUpdateUpgradeWorkflow() 更新整个 workflow
     * - workflow 再触发 batch 级别的 TaskInfo 通知
     */
    private void triggerWorkflowUpdate(DeviceTask task, String mongoState) {
        String deviceId = task.getDeviceId();
        DeviceTask.TaskType taskType = task.getTaskType();
        
        try {
            // 根据任务类型和MongoDB状态，调用 workflow 更新逻辑
            String operationType = taskType.name();  // "UPGRADE" or "ROLLBACK"
            String operationStatus = convertMongoStateToOperationStatus(mongoState, taskType);
            
            log.info("    🔄 触发 Workflow 更新: deviceId={}, operationType={}, operationStatus={}", 
                deviceId, operationType, operationStatus);
            
            // ⭐ 调用 SystemChangeNotificationListener 的 workflow 更新逻辑
            // 这与 Kafka 通知路径使用相同的逻辑
            systemChangeNotificationListener.checkAndUpdateUpgradeWorkflow(
                deviceId, operationType, operationStatus);
            
        } catch (Exception e) {
            log.warn("    ⚠️ 触发 Workflow 更新失败: deviceId={}, error={}", 
                deviceId, e.getMessage());
        }
    }

    /**
     * 将 MongoDB 状态转换为 operationStatus
     * 
     * MongoDB upgrade.state → operationStatus:
     * - ACTIVE_COMPLETE → ACTIVE_COMPLETE
     * - COMPLETE → COMPLETE  
     * - FAIL → FAIL
     * - ROLLBACK-FAIL → ROLLBACK-FAIL
     */
    private String convertMongoStateToOperationStatus(String mongoState, DeviceTask.TaskType taskType) {
        if (mongoState == null) {
            return "UNKNOWN";
        }
        
        // MongoDB 状态直接使用（与 system-change 通知格式一致）
        return normalizeState(mongoState);
    }

    /**
     * 触发批次更新（用于非Workflow的批次任务，如 BACKUP/RESTORE/ROLLBACK）
     * 
     * 与 SystemChangeNotificationListener 保持一致：
     * - 调用 scheduleBatchUpdate() 触发批次状态聚合
     * - 批次更新会发送批次级别的 TaskInfo 通知
     */
    private void triggerBatchUpdate(DeviceTask task) {
        String batchId = task.getBatchId();
        
        try {
            log.info("    📦 触发批次更新: batchId={}, taskType={}", batchId, task.getTaskType());
            
            // ⭐ 调用 SystemChangeNotificationListener 的批次更新逻辑
            // 这与 Kafka 通知路径使用相同的逻辑
            systemChangeNotificationListener.scheduleBatchUpdate(batchId);
            
        } catch (Exception e) {
            log.warn("    ⚠️ 触发批次更新失败: batchId={}, error={}", batchId, e.getMessage());
        }
    }

    /**
     * 触发 workflow 更新（用于超时失败的任务）
     * 
     * 与 RPC 失败处理保持一致：
     * - UPGRADE/ROLLBACK: 通过 checkAndUpdateUpgradeWorkflow() 更新 workflow 状态
     * - RESTORE: 通过 scheduleBatchUpdate() 触发批次状态更新
     */
    private void triggerWorkflowUpdateForFailure(DeviceTask task) {
        String deviceId = task.getDeviceId();
        DeviceTask.TaskType taskType = task.getTaskType();
        
        try {
            // 对于超时失败，使用 "FAIL" 作为 operationStatus
            String operationType = taskType.name();
            String operationStatus = "FAIL";
            
            log.info("    🔄 触发更新（超时失败）: deviceId={}, operationType={}, operationStatus={}", 
                deviceId, operationType, operationStatus);
            
            // ⭐ 根据任务类型选择不同的更新逻辑
            if (taskType == DeviceTask.TaskType.RESTORE) {
                // RESTORE任务：触发批次状态更新
                if (task.getBatchId() != null) {
                    systemChangeNotificationListener.scheduleBatchUpdate(task.getBatchId());
                }
            } else {
                // UPGRADE/ROLLBACK任务：触发 workflow 更新
                systemChangeNotificationListener.checkAndUpdateUpgradeWorkflow(
                    deviceId, operationType, operationStatus);
            }
            
        } catch (Exception e) {
            log.warn("    ⚠️ 触发更新失败: deviceId={}, error={}", 
                deviceId, e.getMessage());
        }
    }

    private String extractStateByTaskType(DeviceTask.TaskType taskType, DeviceOperationStatus deviceStatus) {
        switch (taskType) {
            case DOWNLOAD:
                return extractDownloadState(deviceStatus);
            case BACKUP:
                return extractBackupState(deviceStatus);
            case UPGRADE:
            case COMMIT:
            case ROLLBACK:
                return extractUpgradeState(deviceStatus);
            case RESTORE:
                return extractRestoreState(deviceStatus);
            default:
                return null;
        }
    }

    private String resolveStateFieldName(DeviceTask.TaskType taskType) {
        switch (taskType) {
            case DOWNLOAD:
                return "download.download-state";
            case BACKUP:
                return "backup.backup-state";
            case UPGRADE:
            case COMMIT:
            case ROLLBACK:
                return "upgrade.upgrade-state";
            case RESTORE:
                return "restore.restore-state";
            default:
                return "unknown";
        }
    }
}

