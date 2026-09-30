package devicemaintenance.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.Batch;
import devicemaintenance.dto.DeviceOperationStatus;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.service.BatchLogService;
import devicemaintenance.service.DeviceMaintenanceStatusService;
import devicemaintenance.service.DownloadTerminalEvidencePolicy;
import devicemaintenance.service.SimpleNotificationListenerService;
import devicemaintenance.utils.DeviceMaintenanceLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * System-Change 通知监听器（重构版）
 * 
 * 核心变化：
 * 1. ❌ 不再给单个设备任务发送 TaskInfo 通知
 * 2. ✅ 更新任务时不限状态（不仅限 RUNNING）
 * 3. ✅ 任务匹配：同设备 + 同类型 + 最近创建的未完成任务
 * 4. ✅ 批次级防抖（3秒，按 batchId 合并）
 * 5. ✅ 防抖触发时同步更新 dm_maintenance_batch 表
 * 6. ✅ 完整的批次状态计算逻辑（6种状态）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SystemChangeNotificationListener {

    private final DeviceTaskRepository deviceTaskRepository;
    private final BatchRepository maintenanceBatchRepository;
    private final ObjectMapper objectMapper;
    private final SimpleNotificationListenerService debugService;
    private final TaskInfoNotificationService taskInfoNotificationService;
    private final TaskScheduler taskScheduler;
    private final devicemaintenance.repository.UpgradeWorkflowRepository workflowRepository;
    private final BatchRepository upgradeBatchRepository;  // 统一批次Repository
    private final devicemaintenance.service.WorkflowManagementService workflowManagementService;
    private final devicemaintenance.service.BatchUpgradeService batchUpgradeService;
    private final devicemaintenance.service.UnifiedBatchService unifiedBatchService;
    private final BatchLogService batchLogService;
    private final DeviceMaintenanceStatusService deviceMaintenanceStatusService;
    
    @Value("${kafka.notifications.debug:true}")
    private boolean debugEnabled;
    
    /**
     * 批次更新防抖调度器
     * Key: batchId
     * Value: ScheduledFuture（延迟任务）
     */
    private final Map<String, ScheduledFuture<?>> batchUpdateSchedulers = new ConcurrentHashMap<>();

    /**
     * 自动流转下一步的延迟任务调度器
     * Key: batchId
     * Value: ScheduledFuture（延迟触发下一步的任务）
     */
    private final Map<String, ScheduledFuture<?>> batchAutoStepSchedulers = new ConcurrentHashMap<>();

    /**
     * 自动流转下一步的待执行步骤
     * Key: batchId
     * Value: nextStep（DOWNLOAD/BACKUP/UPGRADE/COMMIT）
     */
    private final Map<String, String> batchAutoStepTargets = new ConcurrentHashMap<>();

    /**
     * 批次自动触发锁（防止并发触发同一批次的下一步）
     * Key: batchId
     * Value: Object（用作锁对象）
     */
    private final Map<String, Object> batchTriggerLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📡 System-Change 通知监听器已初始化");
        log.info("  ✅ 批次级防抖：3秒");
        log.info("  ✅ 任务匹配策略：同设备+同类型+最近未完成");
        log.info("  ✅ 批次状态自动同步");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 监听 system-change topic
     */
    @KafkaListener(topics = "system-change", groupId = "device-maintenance-consumer")
    @Transactional
    public void handleSystemChangeNotification(ConsumerRecord<String, String> record) {
        try {
            DeviceMaintenanceLogContext.ensureTraceId();
            DeviceMaintenanceLogContext.setPhase("KAFKA_RECV", "system-change");
            // ⭐ 临时启用：用于调试通知接收问题
            log.info("📨 [Kafka通知] 接收到 system-change 通知");
            log.info("  Payload 长度: {} 字符", record.value() != null ? record.value().length() : 0);
            log.debug("  Payload 内容: {}", record.value());

            // 1. ⚠️ 已关闭：存储到调试缓存（通知量过大，缓存不过来）
            // if (debugEnabled && debugService != null) {
            //     debugService.storeNotification(record.topic(), record.key(), record.value());
            // }

            // 2. 解析通知
            @SuppressWarnings("unchecked")
            Map<String, String> notification = objectMapper.readValue(record.value(), Map.class);

            String deviceId = notification.get("neId");
            if (deviceId == null || deviceId.isEmpty()) {
                log.warn("⚠️ 通知中缺少设备ID (neId)，跳过处理");
                return;
            }
            DeviceMaintenanceLogContext.setIdentifiers(null, null, null, deviceId, null, null);

            log.info("  设备ID: {}", deviceId);
            
            // 3. ⭐ 识别通知的主操作类型（避免误处理复合通知）
            // 先显示通知中包含的所有操作相关字段
            logNotificationFields(notification);
            
            String primaryOperation = identifyPrimaryOperation(notification);
            log.info("  主操作类型: {}", primaryOperation);
            
            // 4. 根据主操作类型处理通知
            switch (primaryOperation) {
                case "ROLLBACK":
                    processRollbackNotification(deviceId, notification);
                    break;
                case "COMPLETE":
                    // ⭐ upgrade.upgrade-state = COMPLETE 无法区分是 ROLLBACK 还是 UPGRADE
                    // 需要查询数据库，找到该设备最近一次未完成的任务类型
                    log.info("  upgrade.upgrade-state=COMPLETE，查询设备最近的未完成任务...");
                    
                    DeviceTask latestTask = findLatestIncompleteTaskOfAnyType(deviceId);
                    if (latestTask != null) {
                        DeviceTask.TaskType taskType = latestTask.getTaskType();
                        log.info("  ✅ 找到最近任务: taskId={}, taskType={}, status={}", 
                            latestTask.getTaskId(), taskType, latestTask.getStatus());
                        
                        if (taskType == DeviceTask.TaskType.ROLLBACK) {
                            log.info("  → 判定为 ROLLBACK COMPLETE");
                            processRollbackNotification(deviceId, notification);
                        } else if (taskType == DeviceTask.TaskType.UPGRADE) {
                            log.info("  → 判定为 UPGRADE COMPLETE");
                            processUpgradeNotification(deviceId, notification);
                        } else if (taskType == DeviceTask.TaskType.COMMIT) {
                            log.info("  → 判定为 COMMIT COMPLETE");
                            processCommitNotification(deviceId, notification);
                        } else if (taskType == DeviceTask.TaskType.BACKUP) {
                            log.info("  → 判定为 BACKUP COMPLETE");
                            processBackupNotification(deviceId, notification);
                        } else if (taskType == DeviceTask.TaskType.RESTORE) {
                            log.info("  → 判定为 RESTORE COMPLETE");
                            processRestoreNotification(deviceId, notification);
                        } else {
                            log.warn("  ⚠️ 未知的任务类型: {}", taskType);
                        }
                    } else {
                        log.info("  ℹ️ 未找到该设备的未完成任务（可能任务已完成或这是重复通知）");
                        log.debug("     说明: 该设备当前没有未完成的任务（PENDING/SCHEDULED/RUNNING 状态）");
                        log.debug("     可能原因: 1) 任务已处理完成  2) 设备发送了重复通知  3) 收到未知操作的通知");
                    }
                    break;
                case "COMMIT":
                    processCommitNotification(deviceId, notification);
                    break;
                case "UPGRADE":
                    processUpgradeNotification(deviceId, notification);
                    break;
                case "RESTORE":
                    processRestoreNotification(deviceId, notification);
                    break;
                case "BACKUP":
                    processBackupNotification(deviceId, notification);
                    break;
                case "DOWNLOAD":
                    processDownloadNotification(deviceId, notification);
                    break;
                default:
                    log.debug("  未识别的操作类型，尝试按字段逐个处理");
                    // 兜底：按原来的方式逐个处理
            processDownloadNotification(deviceId, notification);
            processBackupNotification(deviceId, notification);
            processRestoreNotification(deviceId, notification);
            processUpgradeNotification(deviceId, notification);
            processCommitNotification(deviceId, notification);
                    processRollbackNotification(deviceId, notification);
            }

            // log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ 处理 system-change 通知失败", e);
            log.error("  原始消息: {}", record.value());
            // log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        } finally {
            DeviceMaintenanceLogContext.clearAll();
        }
    }

    /**
     * 记录通知中包含的操作相关字段（用于调试）
     */
    private void logNotificationFields(Map<String, String> notification) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        
        if (notification.get("download.download-state") != null) {
            fields.add(String.format("download.download-state=%s", notification.get("download.download-state")));
        }
        if (notification.get("backup.backup-state") != null) {
            fields.add(String.format("backup.backup-state=%s", notification.get("backup.backup-state")));
        }
        if (notification.get("restore.restore-state") != null) {
            fields.add(String.format("restore.restore-state=%s", notification.get("restore.restore-state")));
        }
        if (notification.get("upgrade.upgrade-state") != null) {
            fields.add(String.format("upgrade.upgrade-state=%s", notification.get("upgrade.upgrade-state")));
        }
        if (notification.get("commit.commit-state") != null) {
            fields.add(String.format("commit.commit-state=%s", notification.get("commit.commit-state")));
        }
        if (notification.get("rollback.rollback-state") != null) {
            fields.add(String.format("rollback.rollback-state=%s", notification.get("rollback.rollback-state")));
        }
        if (notification.get("rollback.rollback-software") != null) {
            fields.add(String.format("rollback.rollback-software=%s", notification.get("rollback.rollback-software")));
        }
        if (notification.get("upgrade.rollback-software") != null) {
            fields.add(String.format("upgrade.rollback-software=%s", notification.get("upgrade.rollback-software")));
        }
        
        if (!fields.isEmpty()) {
            log.info("  通知字段: [{}]", String.join(", ", fields));
        }
    }

    /**
     * 识别通知的主操作类型（优先级判断）
     * 
     * 优先级从高到低：
     * 1. ROLLBACK (upgrade.upgrade-state = ROLLBACK/ROLLBACK-FAIL 或 rollback.rollback-state 存在)
     * 2. COMMIT (commit.commit-state 存在)
     * 3. UPGRADE (upgrade.upgrade-state = ACTIVE/ACTIVE_COMPLETE/ACTIVE_FAIL)
     * 4. RESTORE (restore.restore-state 存在)
     * 5. BACKUP (backup.backup-state 存在)
     * 6. DOWNLOAD (download.download-state 存在)
     * 
     * 逻辑：高优先级操作的通知可能包含低优先级字段（如 ROLLBACK 包含 download）
     */
    private String identifyPrimaryOperation(Map<String, String> notification) {
        // 1️⃣ 最高优先级：ROLLBACK
        // - upgrade.upgrade-state = "ROLLBACK" 或 "ROLLBACK-FAIL"
        // - rollback.rollback-state 存在（旧格式）
        String upgradeState = normalizeState(notification.get("upgrade.upgrade-state"));
        String rollbackState = notification.get("rollback.rollback-state");
        
        // ⭐ 最高优先级：upgrade.upgrade-state = ROLLBACK/ROLLBACK-FAIL
        if ("ROLLBACK".equals(upgradeState) ||
            "ROLLBACK_FAIL".equals(upgradeState)) {
            return "ROLLBACK";
        }
        
        // ⭐ rollback.rollback-state 存在（旧格式，如果有的话）
        if (rollbackState != null) {
            return "ROLLBACK";
        }
        
        // ⭐ 第二优先级：upgrade.upgrade-state = COMPLETE
        // 注意：UPGRADE COMPLETE 和 ROLLBACK COMPLETE 的通知字段完全相同！
        // 无法通过字段区分，需要查询数据库找到该设备最近一次未完成的任务类型
        if ("COMPLETE".equals(upgradeState)) {
            return "COMPLETE";
        }
        
        // 2️⃣ COMMIT
        String commitState = notification.get("commit.commit-state");
        if (commitState != null) {
            return "COMMIT";
        }
        
        // 3️⃣ UPGRADE (ACTIVE/ACTIVE_COMPLETE/ACTIVE_FAIL)
        if ("ACTIVE".equals(upgradeState) ||
            "ACTIVE_COMPLETE".equals(upgradeState) ||
            "ACTIVE_FAIL".equals(upgradeState)) {
            return "UPGRADE";
        }
        
        // 4️⃣ RESTORE
        String restoreState = notification.get("restore.restore-state");
        if (restoreState != null) {
            return "RESTORE";
        }
        
        // 5️⃣ BACKUP
        String backupState = notification.get("backup.backup-state");
        if (backupState != null) {
            return "BACKUP";
        }
        
        // 6️⃣ DOWNLOAD（独立的下载操作）
        String downloadState = notification.get("download.download-state");
        if (downloadState != null) {
            return "DOWNLOAD";
        }
        
        // 未识别
        return "UNKNOWN";
    }

    /**
     * 处理下载操作通知
     */
    private void processDownloadNotification(String deviceId, Map<String, String> notification) {
        String downloadState = notification.get("download.download-state");
        if (downloadState == null) {
            return;
        }

        log.info("🔽 处理下载操作通知: deviceId={}, 状态={}", deviceId, downloadState);

        // 查找任务：同设备 + 同类型 + 最近创建的未完成任务
        DeviceTask task = findLatestIncompleteTask(deviceId, DeviceTask.TaskType.DOWNLOAD);

        if (task == null) {
            log.warn("notification skipped reason=noMatchingTask operation=DOWNLOAD state={}", downloadState);
            return;
        }

        DeviceMaintenanceLogContext.setTaskContext(task);
        log.info("  ✅ 找到匹配任务: taskId={}, 当前状态={}, batchId={}, workflowId={}",
            task.getTaskId(), task.getStatus(), task.getBatchId(), task.getWorkflowId());

        // 更新任务状态
        boolean statusChanged = updateTaskStatusForDownload(task, notification);

        if (statusChanged) {
            DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", "download-notification");
            DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
            deviceTaskRepository.save(task);
            syncBatchTargetVersionFromDownload(task);
            log.info("notification applied operation=DOWNLOAD state={} finalStatus={}", downloadState, task.getStatus());
            logBatchStateIfNeeded(task, "download-notification");

            // ✅ 如果任务属于批次，触发批次更新（防抖）
            // ⚠️ UPGRADE 批次通过 workflow 更新，不走这里
            if (task.getBatchId() != null && task.getWorkflowId() == null) {
                log.debug("  📤 触发批次更新通知: batchId={}", task.getBatchId());
                scheduleBatchUpdate(task.getBatchId());
            } else if (task.getWorkflowId() != null) {
                log.debug("  ⏭️  跳过批次更新（workflow 任务）: workflowId={}", task.getWorkflowId());
            }
            
            // ✅ 检查并更新升级工作流
            checkAndUpdateUpgradeWorkflow(deviceId, "DOWNLOAD", downloadState);
        }
    }

    /**
     * 处理备份操作通知
     */
    private void processBackupNotification(String deviceId, Map<String, String> notification) {
        String backupState = notification.get("backup.backup-state");
        if (backupState == null) {
            return;
        }

        log.debug("💾 处理备份操作通知: 状态={}", backupState);

        // 查找任务
        DeviceTask task = findLatestIncompleteTask(deviceId, DeviceTask.TaskType.BACKUP);

        if (task == null) {
            log.warn("notification skipped reason=noMatchingTask operation=BACKUP state={}", backupState);
            return;
        }

        DeviceMaintenanceLogContext.setTaskContext(task);
        log.debug("  ✅ 找到匹配任务: taskId={}, 当前状态={}, batchId={}",
            task.getTaskId(), task.getStatus(), task.getBatchId());

        // 更新任务状态
        boolean statusChanged = updateTaskStatusForBackup(task, notification);

        if (statusChanged) {
            DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", "backup-notification");
            DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
            deviceTaskRepository.save(task);
            log.info("notification applied operation=BACKUP state={} finalStatus={}", backupState, task.getStatus());
            logBatchStateIfNeeded(task, "backup-notification");
            
            // ⭐ 如果任务属于批次，触发批次更新（防抖）
            // ⚠️ UPGRADE 批次通过 workflow 更新，不走这里
            if (task.getBatchId() != null && task.getWorkflowId() == null) {
                scheduleBatchUpdate(task.getBatchId());
            }
            
            // ✅ 检查并更新升级工作流
            checkAndUpdateUpgradeWorkflow(deviceId, "BACKUP", backupState);
        }
    }

    /**
     * 处理恢复操作通知
     */
    private void processRestoreNotification(String deviceId, Map<String, String> notification) {
        String restoreState = notification.get("restore.restore-state");
        if (restoreState == null) {
            return;
        }

        log.debug("📥 处理恢复操作通知: 状态={}", restoreState);

        // 查找任务
        DeviceTask task = findLatestIncompleteTask(deviceId, DeviceTask.TaskType.RESTORE);

        if (task == null) {
            log.warn("notification skipped reason=noMatchingTask operation=RESTORE state={}", restoreState);
            return;
        }

        DeviceMaintenanceLogContext.setTaskContext(task);
        log.debug("  ✅ 找到匹配任务: taskId={}, 当前状态={}, batchId={}",
            task.getTaskId(), task.getStatus(), task.getBatchId());

        // 更新任务状态
        boolean statusChanged = updateTaskStatusForRestore(task, notification);

        if (statusChanged) {
            DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", "restore-notification");
            DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
            deviceTaskRepository.save(task);
            log.info("notification applied operation=RESTORE state={} finalStatus={}", restoreState, task.getStatus());
            logBatchStateIfNeeded(task, "restore-notification");
            
            // ⭐ 如果任务属于批次，触发批次更新（防抖）
            // ⚠️ UPGRADE 批次通过 workflow 更新，不走这里
            if (task.getBatchId() != null && task.getWorkflowId() == null) {
                scheduleBatchUpdate(task.getBatchId());
            } else if (task.getBatchId() == null) {
                // ✅ 单设备任务：发送单任务级别的 TaskInfo 更新通知
                try {
                    taskInfoNotificationService.notifyTaskCompleted(task);
                    log.debug("  📨 已发送单任务 TaskInfo 更新通知: taskId={}, status={}", 
                        task.getTaskId(), task.getStatus());
                } catch (Exception e) {
                    log.warn("  ⚠️ 发送单任务 TaskInfo 通知失败: {}", e.getMessage());
                }
            }
            
            // ✅ 检查并更新升级工作流（虽然恢复不在升级流程中，但保留接口一致性）
            // checkAndUpdateUpgradeWorkflow(deviceId, "RESTORE", restoreState);
        }
    }

    /**
     * 处理升级操作通知
     */
    private void processUpgradeNotification(String deviceId, Map<String, String> notification) {
        String upgradeState = notification.get("upgrade.upgrade-state");
        if (upgradeState == null) {
            return;
        }
        
        log.debug("⬆️ 处理升级操作通知: 状态={}", upgradeState);
        
        // 查找任务
        DeviceTask task = findLatestIncompleteTask(deviceId, DeviceTask.TaskType.UPGRADE);
        
        if (task == null) {
            log.warn("notification skipped reason=noMatchingTask operation=UPGRADE state={}", upgradeState);
            return;
        }
        
        DeviceMaintenanceLogContext.setTaskContext(task);
        log.debug("  ✅ 找到匹配任务: taskId={}, 当前状态={}, batchId={}", 
            task.getTaskId(), task.getStatus(), task.getBatchId());
        
        // 更新任务状态
        boolean statusChanged = updateTaskStatusForUpgrade(task, notification);
        
        if (statusChanged) {
            DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", "upgrade-notification");
            DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
            deviceTaskRepository.save(task);
            log.info("notification applied operation=UPGRADE state={} finalStatus={}", upgradeState, task.getStatus());
            logBatchStateIfNeeded(task, "upgrade-notification");
            
            // ✅ 如果任务属于批次，触发批次更新（防抖）
            // ⚠️ UPGRADE 批次通过 workflow 更新，不走这里
            if (task.getBatchId() != null && task.getWorkflowId() == null) {
                scheduleBatchUpdate(task.getBatchId());
            }
            
            // ✅ 检查并更新升级工作流
            checkAndUpdateUpgradeWorkflow(deviceId, "UPGRADE", upgradeState);
        }
    }

    /**
     * 处理提交操作通知
     * 
     * ⚠️ Commit 没有独立的 commit-state 通知
     * 而是通过设备状态从 ACTIVE_COMPLETE → COMPLETE 来判断
     */
    private void processCommitNotification(String deviceId, Map<String, String> notification) {
        String upgradeState = normalizeState(notification.get("upgrade.upgrade-state"));
        if (!"COMPLETE".equals(upgradeState)) {
            return;  // 只处理 COMPLETE 状态
        }
        
        log.debug("✅ 处理提交操作通知: 状态={} (ACTIVE_COMPLETE → COMPLETE)", upgradeState);
        
        // 查找 COMMIT 任务
        DeviceTask task = findLatestIncompleteTask(deviceId, DeviceTask.TaskType.COMMIT);
        
        if (task == null) {
            log.warn("notification skipped reason=noMatchingTask operation=COMMIT state={}", upgradeState);
            return;
        }
        
        DeviceMaintenanceLogContext.setTaskContext(task);
        log.debug("  ✅ 找到匹配任务: taskId={}, 当前状态={}",
            task.getTaskId(), task.getStatus());

        // 更新任务状态为完成
        // ✅ 防止乱序通知：只有非终态才能转为 COMPLETED
        if (isNonFinalState(task.getStatus())) {
            task.setStatus(DeviceTask.TaskStatus.COMPLETED);
            task.setCompletedTime(LocalDateTime.now());
            task.setUpdatedTime(LocalDateTime.now());
            DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", "commit-notification");
            DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
            deviceTaskRepository.save(task);
            log.info("notification applied operation=COMMIT state={} finalStatus={}", upgradeState, task.getStatus());
            logBatchStateIfNeeded(task, "commit-notification");

            // ⭐ 如果任务属于 workflow，触发 Workflow 和 Batch 状态更新
            if (task.getWorkflowId() != null) {
                log.debug("  🔄 触发 Workflow 状态更新: workflowId={}", task.getWorkflowId());
                checkAndUpdateUpgradeWorkflow(deviceId, "COMMIT", "COMPLETE");
            }
        } else {
            log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", task.getStatus());
        }
    }

    /**
     * 处理回滚操作通知
     */
    private void processRollbackNotification(String deviceId, Map<String, String> notification) {
        // ⭐ 支持两种回滚通知格式：
        // 1. rollback.rollback-state (旧格式，如果存在)
        // 2. upgrade.upgrade-state = ROLLBACK/ROLLBACK-FAIL/COMPLETE (新格式)
        String rollbackState = notification.get("rollback.rollback-state");
        String upgradeState = normalizeState(notification.get("upgrade.upgrade-state"));
        
        // 合并两种格式的状态
        String effectiveState = null;
        if (rollbackState != null) {
            // 旧格式：rollback.rollback-state 直接使用
            effectiveState = rollbackState;
        } else if ("ROLLBACK".equals(upgradeState)) {
            // 新格式：upgrade.upgrade-state = ROLLBACK
            effectiveState = "ROLLING_BACK";
        } else if ("ROLLBACK_FAIL".equals(upgradeState)) {
            // 新格式：upgrade.upgrade-state = ROLLBACK-FAIL
            effectiveState = "FAIL";
        } else if ("COMPLETE".equals(upgradeState)) {
            // ⭐ 新格式：upgrade.upgrade-state = COMPLETE
            // 注意：此时无法区分是 ROLLBACK COMPLETE 还是 UPGRADE COMPLETE
            // 由调用方（handleSystemChangeNotification）通过查找任务类型来判断
            effectiveState = "COMPLETE";
        }
        
        if (effectiveState == null) {
            return;
        }
        
        log.info("🔙 处理回滚操作通知: deviceId={}, 状态={} (来源: upgrade.upgrade-state={}, rollback.rollback-state={})", 
            deviceId, effectiveState, upgradeState, rollbackState);
        
        // 查找任务
        DeviceTask task = findLatestIncompleteTask(deviceId, DeviceTask.TaskType.ROLLBACK);
        
        if (task == null) {
            log.warn("notification skipped reason=noMatchingTask operation=ROLLBACK state={}", effectiveState);
            return;
        }
        
        DeviceMaintenanceLogContext.setTaskContext(task);
        log.info("  ✅ 找到匹配任务: taskId={}, 当前状态={}, batchId={}, workflowId={}", 
            task.getTaskId(), task.getStatus(), task.getBatchId(), task.getWorkflowId());
        
        // 更新任务状态
        boolean statusChanged = updateTaskStatusForRollback(task, effectiveState, notification);
        
        if (statusChanged) {
            DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", "rollback-notification");
            DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
            deviceTaskRepository.save(task);
            log.info("notification applied operation=ROLLBACK state={} finalStatus={}", effectiveState, task.getStatus());
            logBatchStateIfNeeded(task, "rollback-notification");

            if (task.getStatus() == DeviceTask.TaskStatus.COMPLETED) {
                removeReadyToCommitUpgradeWorkflowsAfterRollback(deviceId);
            }
            
            // ⭐ 如果任务属于批次，触发批次更新（防抖）
            // ⚠️ UPGRADE 批次通过 workflow 更新，不走这里
            if (task.getBatchId() != null && task.getWorkflowId() == null) {
                scheduleBatchUpdate(task.getBatchId());
            } else if (task.getBatchId() == null) {
            // ✅ 单设备任务：发送单任务级别的 TaskInfo 更新通知
            try {
                taskInfoNotificationService.notifyTaskCompleted(task);
                log.info("  📨 已发送单任务 TaskInfo 更新通知: taskId={}, status={}", 
                    task.getTaskId(), task.getStatus());
            } catch (Exception e) {
                log.warn("  ⚠️ 发送单任务 TaskInfo 通知失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 查找最近创建的未完成任务
     * 
     * 匹配条件：
     * - 设备ID一致
     * - 任务类型一致
     * - 状态为未完成（PENDING, SCHEDULED, RUNNING）
     * - 按创建时间倒序，取最新的
     */
    private DeviceTask findLatestIncompleteTask(String deviceId, DeviceTask.TaskType taskType) {
        List<DeviceTask> tasks = deviceTaskRepository
            .findIncompleteTasksByDeviceAndType(deviceId, taskType);
        
        return tasks.isEmpty() ? null : tasks.get(0);  // 已按 createdTime DESC 排序
    }

    /**
     * 回滚成功后，将原升级批次中仍处于 READY_TO_COMMIT 的网元移出 workflow。
     *
     * 预期行为：
     * - 已从该次升级回滚成功的网元，不再参与原升级批次的后续 COMMIT
     * - 原升级批次只保留仍处于待提交状态的网元
     */
    private void removeReadyToCommitUpgradeWorkflowsAfterRollback(String deviceId) {
        try {
            List<devicemaintenance.entity.UpgradeWorkflow> workflows = workflowRepository.findByDeviceId(deviceId);
            if (workflows.isEmpty()) {
                return;
            }

            workflowManagementService.computeAndSetWorkflowStatuses(workflows);

            for (devicemaintenance.entity.UpgradeWorkflow workflow : workflows) {
                if (workflow.getStatus() != devicemaintenance.entity.UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS) {
                    continue;
                }

                Optional<Batch> batchOpt = upgradeBatchRepository.findById(workflow.getBatchId());
                if (!batchOpt.isPresent()) {
                    continue;
                }

                Batch batch = batchOpt.get();
                if (batch.getBatchType() != Batch.BatchType.UPGRADE ||
                    batch.getStatus() != Batch.BatchStatus.READY_TO_COMMIT) {
                    continue;
                }

                log.info("🔄 回滚成功后移除原升级 workflow: batchId={}, deviceId={}, workflowId={}",
                    workflow.getBatchId(), deviceId, workflow.getWorkflowId());
                batchUpgradeService.removeWorkflow(workflow.getBatchId(), deviceId);
            }
        } catch (Exception e) {
            log.error("回滚成功后清理原升级 workflow 失败: deviceId={}", deviceId, e);
        }
    }
    
    /**
     * 查找设备最近的未完成任务（不限类型）
     * 
     * 用于处理 upgrade.upgrade-state=COMPLETE 通知时，判断是 ROLLBACK 还是 UPGRADE 完成
     * 
     * @param deviceId 设备ID
     * @return 最近的未完成任务，如果没有则返回 null
     */
    private DeviceTask findLatestIncompleteTaskOfAnyType(String deviceId) {
        // ⭐ 查找该设备所有未完成的任务，按创建时间降序排列
        // 注意：这里查询所有类型的任务（DOWNLOAD, BACKUP, UPGRADE, ROLLBACK, COMMIT, RESTORE）
        List<DeviceTask> tasks = deviceTaskRepository.findByDeviceIdAndStatusIn(
            deviceId,
            java.util.Arrays.asList(
                DeviceTask.TaskStatus.PENDING,
                DeviceTask.TaskStatus.SCHEDULED,
                DeviceTask.TaskStatus.RUNNING
            )
        );
        
        // 返回第一个（最新的），如果列表为空则返回 null
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    private void logBatchStateIfNeeded(DeviceTask task, String trigger) {
        if (task != null && task.getBatchId() != null) {
            batchLogService.logBatchSnapshotByBatchId(task.getBatchId(), trigger, task.getDeviceId());
        }
    }
    
    /**
     * 判断是否为非终态（可以继续转换状态）
     * 非终态：NOT_START, PENDING, SCHEDULED, RUNNING
     * 终态：COMPLETED, FAILED, CANCELLED, EXPIRED, IDLE
     */
    private boolean isNonFinalState(DeviceTask.TaskStatus status) {
        return status == DeviceTask.TaskStatus.NOT_START ||
               status == DeviceTask.TaskStatus.PENDING ||
               status == DeviceTask.TaskStatus.SCHEDULED ||
               status == DeviceTask.TaskStatus.RUNNING;
    }

    /**
     * 更新下载任务状态
     */
    private boolean updateTaskStatusForDownload(DeviceTask task, Map<String, String> notification) {
        String downloadState = normalizeState(notification.get("download.download-state"));
        boolean changed = false;
        DeviceTask.TaskStatus currentStatus = task.getStatus();
        
        log.info("  updateTaskStatusForDownload: downloadState={}, currentStatus={}", downloadState, currentStatus);

        if ("COMPLETE".equals(downloadState) || "FAIL".equals(downloadState)) {
            DownloadTerminalEvidencePolicy.Validation validation =
                DownloadTerminalEvidencePolicy.validate(task, notification);
            if (!validation.isAccepted()) {
                log.warn(
                    "notification skipped operation=DOWNLOAD taskId={} deviceId={} state={} reason={}",
                    task.getTaskId(),
                    task.getDeviceId(),
                    downloadState,
                    validation.getReason()
                );
                return false;
            }
        }
        
        if ("COMPLETE".equals(downloadState)) {
            // ✅ 防止乱序通知：只有非终态才能转为 COMPLETED
            if (isNonFinalState(currentStatus)) {
                task.setStatus(DeviceTask.TaskStatus.COMPLETED);
                task.setCompletedTime(LocalDateTime.now());
                changed = true;
                log.info("  ✓ 任务状态: {} → COMPLETED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else if ("FAIL".equals(downloadState)) {
            // ✅ 防止乱序通知：只有非终态才能转为 FAILED
            if (isNonFinalState(currentStatus)) {
                task.setStatus(DeviceTask.TaskStatus.FAILED);
                task.setCompletedTime(LocalDateTime.now());
                task.setErrorMessage("Download failed");
                changed = true;
                log.info("  ✓ 任务状态: {} → FAILED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else if ("DOWNLOADING".equals(downloadState)) {
            // ✅ 防止乱序通知：只有非终态才能转为 RUNNING
            if (isNonFinalState(currentStatus) && currentStatus != DeviceTask.TaskStatus.RUNNING) {
                task.setStatus(DeviceTask.TaskStatus.RUNNING);
                task.setStartedTime(LocalDateTime.now());
                changed = true;
                log.info("  ✓ 任务状态: {} → RUNNING", currentStatus);
            } else if (!isNonFinalState(currentStatus)) {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else {
            log.warn("  ⚠️ 未知的下载状态: {}", downloadState);
        }
        
        // 保存附加信息
        String fileName = firstNonBlank(
            notification.get("download.file-name"),
            notification.get("file-name"));
        String version = firstNonBlank(
            notification.get("download.software-version"),
            notification.get("software-version"));
        if (fileName != null && (task.getFilePath() == null || task.getFilePath().trim().isEmpty())) {
            task.setFilePath(fileName);
            changed = true;
        }
        if (version != null && !version.equals(task.getTargetVersion())) {
            task.setTargetVersion(version);
            changed = true;
        }
        
        return changed;
    }

    private void syncBatchTargetVersionFromDownload(DeviceTask task) {
        String batchId = task.getBatchId();
        String targetVersion = task.getTargetVersion();
        String deviceId = task.getDeviceId();

        if (batchId == null || batchId.trim().isEmpty()) {
            return;
        }
        if (targetVersion == null || targetVersion.trim().isEmpty()) {
            return;
        }

        syncRelatedTaskTargetVersionFromDownload(batchId, deviceId, targetVersion, task.getTaskId());

        try {
            Optional<Batch> batchOpt = maintenanceBatchRepository.findById(batchId);
            if (!batchOpt.isPresent()) {
                log.warn("  ⚠️ 下载版本回写跳过，批次不存在: batchId={}", batchId);
                return;
            }

            Batch batch = batchOpt.get();
            if (targetVersion.equals(batch.getTargetVersion())) {
                return;
            }

            String previousTargetVersion = batch.getTargetVersion();
            batch.setTargetVersion(targetVersion);
            maintenanceBatchRepository.save(batch);
            log.info(
                "  ✅ 下载通知已回写批次目标版本: batchId={}, oldTargetVersion={}, newTargetVersion={}",
                batchId, previousTargetVersion, targetVersion);
        } catch (Exception e) {
            log.warn("  ⚠️ 下载版本回写批次失败: batchId={}, error={}", batchId, e.getMessage());
        }
    }

    /**
     * 更新备份任务状态
     */
    private void syncRelatedTaskTargetVersionFromDownload(
            String batchId,
            String deviceId,
            String targetVersion,
            String sourceTaskId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return;
        }

        try {
            List<DeviceTask> relatedTasks = deviceTaskRepository.findByBatchIdAndDeviceId(batchId, deviceId);
            List<DeviceTask> tasksToUpdate = new ArrayList<>();

            for (DeviceTask relatedTask : relatedTasks) {
                if (relatedTask == null || sourceTaskId.equals(relatedTask.getTaskId())) {
                    continue;
                }
                if (targetVersion.equals(relatedTask.getTargetVersion())) {
                    continue;
                }
                relatedTask.setTargetVersion(targetVersion);
                tasksToUpdate.add(relatedTask);
            }

            if (tasksToUpdate.isEmpty()) {
                return;
            }

            deviceTaskRepository.saveAll(tasksToUpdate);
            log.info(
                "  ✅ 下载通知已同步设备任务目标版本: batchId={}, deviceId={}, targetVersion={}, updatedTasks={}",
                batchId, deviceId, targetVersion, tasksToUpdate.size());
        } catch (Exception e) {
            log.warn(
                "  ⚠️ 下载版本回写设备任务失败: batchId={}, deviceId={}, error={}",
                batchId, deviceId, e.getMessage());
        }
    }

    private boolean updateTaskStatusForBackup(DeviceTask task, Map<String, String> notification) {
        String backupState = normalizeState(notification.get("backup.backup-state"));
        boolean changed = false;
        DeviceTask.TaskStatus currentStatus = task.getStatus();
        
        if ("COMPLETE".equals(backupState)) {
            // ✅ 防止乱序通知
            if (isNonFinalState(currentStatus)) {
                task.setStatus(DeviceTask.TaskStatus.COMPLETED);
                task.setCompletedTime(LocalDateTime.now());
                changed = true;
                log.debug("  ✓ 任务状态: {} → COMPLETED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else if ("FAIL".equals(backupState)) {
            // ✅ 防止乱序通知
            if (isNonFinalState(currentStatus)) {
                task.setStatus(DeviceTask.TaskStatus.FAILED);
                task.setCompletedTime(LocalDateTime.now());
                task.setErrorMessage("Backup failed");
                changed = true;
                log.debug("  ✓ 任务状态: {} → FAILED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        }
        
        // 保存附加信息
        String backupFile = notification.get("backup.backup-file");
        if (backupFile != null) {
            task.setBackupFileName(backupFile);
            // ⚠️ 不要覆盖 backupFilePath！它已经包含完整路径（目录+文件名）
            // Kafka通知里的 backup-file 只是文件名，不是完整路径
            changed = true;
        }
        
        return changed;
    }

    /**
     * 更新恢复任务状态
     */
    private boolean updateTaskStatusForRestore(DeviceTask task, Map<String, String> notification) {
        String restoreState = normalizeState(notification.get("restore.restore-state"));
        boolean changed = false;
        DeviceTask.TaskStatus currentStatus = task.getStatus();
        
        if ("COMPLETE".equals(restoreState)) {
            // ✅ 防止乱序通知
            if (isNonFinalState(currentStatus)) {
                task.setStatus(DeviceTask.TaskStatus.COMPLETED);
                task.setCompletedTime(LocalDateTime.now());
                changed = true;
                log.debug("  ✓ 任务状态: {} → COMPLETED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else if ("FAIL".equals(restoreState)) {
            // ✅ 防止乱序通知
            if (isNonFinalState(currentStatus)) {
                task.setStatus(DeviceTask.TaskStatus.FAILED);
                task.setCompletedTime(LocalDateTime.now());
                task.setErrorMessage("Restore failed");
                changed = true;
                log.debug("  ✓ 任务状态: {} → FAILED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        }
        
        // 保存附加信息
        String restoreFile = notification.get("restore.restore-from-file");
        if (restoreFile != null) {
            // ⚠️ restore-from-file 是恢复源文件名，保存为 backupFileName
            task.setBackupFileName(restoreFile);
            changed = true;
        }
        
        return changed;
    }

    /**
     * 更新回滚任务状态
     * 
     * @param task DeviceTask
     * @param effectiveState 有效状态（已合并 rollback.rollback-state 和 upgrade.upgrade-state）
     * @param notification 原始通知
     */
    private boolean updateTaskStatusForRollback(DeviceTask task, String effectiveState, Map<String, String> notification) {
        effectiveState = normalizeState(effectiveState);
        boolean changed = false;
        DeviceTask.TaskStatus currentStatus = task.getStatus();
        
        log.info("  updateTaskStatusForRollback: effectiveState={}, currentStatus={}", effectiveState, currentStatus);
        
        if ("COMPLETE".equals(effectiveState)) {
            // ✅ 防止乱序通知
            if (isNonFinalState(currentStatus)) {
                task.setStatus(DeviceTask.TaskStatus.COMPLETED);
                task.setCompletedTime(LocalDateTime.now());
                changed = true;
                log.info("  ✓ 任务状态: {} → COMPLETED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else if ("FAIL".equals(effectiveState)) {
            // ✅ 防止乱序通知
            if (isNonFinalState(currentStatus)) {
                task.setStatus(DeviceTask.TaskStatus.FAILED);
                task.setCompletedTime(LocalDateTime.now());
                task.setErrorMessage("Rollback failed");
                changed = true;
                log.info("  ✓ 任务状态: {} → FAILED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else if ("ROLLING_BACK".equals(effectiveState)) {
            // ✅ 回滚进行中
            if (isNonFinalState(currentStatus) && currentStatus != DeviceTask.TaskStatus.RUNNING) {
                task.setStatus(DeviceTask.TaskStatus.RUNNING);
                task.setStartedTime(LocalDateTime.now());
                changed = true;
                log.info("  ✓ 任务状态: {} → RUNNING", currentStatus);
            } else if (!isNonFinalState(currentStatus)) {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else {
            log.warn("  ⚠️ 未知的回滚状态: {}", effectiveState);
        }
        
        return changed;
    }

    /**
     * 更新升级任务状态
     */
    private boolean updateTaskStatusForUpgrade(DeviceTask task, Map<String, String> notification) {
        String upgradeState = normalizeState(notification.get("upgrade.upgrade-state"));
        boolean changed = false;
        DeviceTask.TaskStatus currentStatus = task.getStatus();
        
        log.debug("  updateTaskStatusForUpgrade: upgradeState={}, currentStatus={}", upgradeState, currentStatus);
        
        if ("ACTIVE_COMPLETE".equals(upgradeState)) {
            changed = updateUpgradeTaskStatusAfterActivationConfirmation(task, currentStatus, upgradeState, notification);
        } else if ("COMPLETE".equals(upgradeState)) {
            log.info(
                "  ℹ️ 收到 COMPLETE，但该状态属于 COMMIT 后状态，忽略 UPGRADE 任务更新: taskId={}, targetVersion={}",
                task.getTaskId(), task.getTargetVersion());
        } else if ("FAIL".equals(upgradeState) || "FAILED".equals(upgradeState) || "ACTIVE_FAIL".equals(upgradeState)) {
            // ✅ 防止乱序通知和重复处理
            if (isNonFinalState(currentStatus)) {
            task.setStatus(DeviceTask.TaskStatus.FAILED);
            task.setCompletedTime(LocalDateTime.now());
                task.setErrorMessage("Upgrade failed");
            changed = true;
                log.debug("  ✓ 任务状态: {} → FAILED", currentStatus);
            } else {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        } else if ("ACTIVE".equals(upgradeState) || "ACTIVATE".equals(upgradeState)) {
            // ✅ 升级进行中（ACTIVATE 或 ACTIVE 都表示升级正在执行）
            if (isNonFinalState(currentStatus) && currentStatus != DeviceTask.TaskStatus.RUNNING) {
                task.setStatus(DeviceTask.TaskStatus.RUNNING);
                if (task.getStartedTime() == null) {
                    task.setStartedTime(LocalDateTime.now());
                }
                changed = true;
                log.debug("  ✓ 任务状态: {} → RUNNING (upgradeState={})", currentStatus, upgradeState);
            } else if (!isNonFinalState(currentStatus)) {
                log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            }
        }
        
        // 保存附加信息
        String rollbackFile = notification.get("upgrade.rollback-file");
        String rollbackSoftware = notification.get("upgrade.rollback-software");
        if (rollbackFile != null) {
            // ⚠️ rollback-file 是回滚文件名，保存为 backupFileName
            task.setBackupFileName(rollbackFile);
            changed = true;
        }
        if (rollbackSoftware != null) {
            task.setCurrentVersion(rollbackSoftware);
            changed = true;
        }
        
        return changed;
    }

    private boolean updateUpgradeTaskStatusAfterActivationConfirmation(
            DeviceTask task,
            DeviceTask.TaskStatus currentStatus,
            String upgradeState,
            Map<String, String> notification) {
        if (!isNonFinalState(currentStatus)) {
            log.warn("  ⚠️ 忽略乱序通知：任务已是终态 {}", currentStatus);
            return false;
        }

        DeviceOperationStatus deviceStatus;
        try {
            deviceStatus = deviceMaintenanceStatusService.getDeviceOperationStatus(task.getDeviceId());
        } catch (Exception e) {
            log.warn("  ⚠️ 无法在 {} 时查询设备当前版本，保持等待确认: deviceId={}, error={}",
                upgradeState, task.getDeviceId(), e.getMessage());
            return false;
        }

        String currentSoftware = firstNonBlank(
            extractCurrentSoftwareFromNotification(notification),
            deviceStatus != null ? deviceStatus.getCurrentSoftware() : null
        );
        if (currentSoftware != null) {
            task.setCurrentVersion(currentSoftware);
        }

        String expectedVersion = resolveExpectedUpgradeVersion(notification, task, deviceStatus);
        if (expectedVersion == null || expectedVersion.trim().isEmpty()) {
            task.setStatus(DeviceTask.TaskStatus.COMPLETED);
            task.setCompletedTime(LocalDateTime.now());
            log.warn(
                "  ⚠️ {} 时缺少可用的目标版本信息，无法校验激活后版本，按完成处理: taskId={}, currentSoftware={}, taskTargetVersion={}",
                upgradeState, task.getTaskId(), currentSoftware, task.getTargetVersion());
            return true;
        }

        if (currentSoftware == null || currentSoftware.trim().isEmpty()) {
            log.warn("  ⚠️ {} 时未获取到设备当前版本，继续等待激活后同步: taskId={}, expectedVersion={}",
                upgradeState, task.getTaskId(), expectedVersion);
            return false;
        }

        if (isVersionMatch(currentSoftware, expectedVersion)) {
            task.setStatus(DeviceTask.TaskStatus.COMPLETED);
            task.setCompletedTime(LocalDateTime.now());
            log.info(
                "  ✅ 设备激活后版本确认成功: taskId={}, upgradeState={}, currentSoftware={}, expectedVersion={}, taskTargetVersion={}",
                task.getTaskId(), upgradeState, currentSoftware, expectedVersion, task.getTargetVersion());
            return true;
        }

        task.setStatus(DeviceTask.TaskStatus.FAILED);
        task.setCompletedTime(LocalDateTime.now());
        task.setErrorMessage(String.format(
            "Activation did not take effect: upgradeState=%s, currentSoftware=%s, expectedVersion=%s, taskTargetVersion=%s",
            upgradeState,
            currentSoftware, expectedVersion, task.getTargetVersion()));
        log.warn(
            "  ❌ 设备激活后版本确认失败，升级回退或启动异常: taskId={}, upgradeState={}, currentSoftware={}, expectedVersion={}, taskTargetVersion={}",
            task.getTaskId(), upgradeState, currentSoftware, expectedVersion, task.getTargetVersion());
        return true;
    }

    private String resolveExpectedUpgradeVersion(
            Map<String, String> notification,
            DeviceTask task,
            DeviceOperationStatus deviceStatus) {
        return firstNonBlank(
            notification.get("download.software-version"),
            notification.get("software-version"),
            deviceStatus != null
                && deviceStatus.getSoftwareOperations() != null
                && deviceStatus.getSoftwareOperations().getDownload() != null
                    ? deviceStatus.getSoftwareOperations().getDownload().getSoftwareVersion()
                    : null,
            task != null ? task.getTargetVersion() : null
        );
    }

    private String extractCurrentSoftwareFromNotification(Map<String, String> notification) {
        return firstNonBlank(
            notification.get("current-software"),
            notification.get("software.current-software"),
            notification.get("system.software.state.current-software")
        );
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isVersionMatch(String currentSoftware, String targetVersion) {
        return normalizeVersion(currentSoftware).equals(normalizeVersion(targetVersion));
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }

        String normalized = version.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("release_")) {
            normalized = normalized.substring("release_".length());
        } else if (normalized.startsWith("package_")) {
            normalized = normalized.substring("package_".length());
        } else if (normalized.startsWith("software_")) {
            normalized = normalized.substring("software_".length());
        }
        return normalized;
    }

    private String normalizeState(String state) {
        if (state == null) {
            return null;
        }
        return state.trim().toUpperCase().replace('-', '_');
    }

    /**
     * 调度批次更新（防抖）
     * 
     * 机制：
     * - 每次调用时，取消之前的定时任务
     * - 创建新的3秒延迟任务
     * - 如果3秒内又有新的更新，则重新计时
     * - 最终只执行一次批次更新
     */
    public void scheduleBatchUpdate(String batchId) {
        synchronized (batchUpdateSchedulers) {
            // 取消之前的任务
            ScheduledFuture<?> oldTask = batchUpdateSchedulers.get(batchId);
            if (oldTask != null && !oldTask.isDone()) {
                oldTask.cancel(false);
                log.debug("  🔄 取消之前的批次更新任务: batchId={}", batchId);
            }
            
            // 创建新任务（3秒后执行）
            ScheduledFuture<?> newTask = taskScheduler.schedule(
                () -> {
                    try {
                        sendBatchUpdateNotification(batchId);
                    } finally {
                        batchUpdateSchedulers.remove(batchId);
                    }
                },
                Instant.now().plusSeconds(3)
            );
            
            batchUpdateSchedulers.put(batchId, newTask);
            log.debug("  ⏱️  已调度批次更新任务: batchId={}, 延迟=3秒", batchId);
        }
    }

    /**
     * 发送批次更新通知
     * 
     * 步骤：
     * 1. 查询批次所有任务
     * 2. 计算批次状态
     * 3. 更新 dm_maintenance_batch 表
     * 4. 发送 TaskInfo 通知
     */
    @Transactional
    public void sendBatchUpdateNotification(String batchId) {
        try {
            log.debug("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.debug("📤 [批次更新] 开始处理批次: {}", batchId);
            
            // 1. 查询批次记录（获取 actionTime）
            Optional<Batch> batchOpt = maintenanceBatchRepository.findById(batchId);
            if (!batchOpt.isPresent()) {
                log.warn("  ⚠️ 批次 {} 在数据库中不存在，跳过", batchId);
                return;
            }
            
            Batch batch = batchOpt.get();
            
            // 2. 查询批次所有任务
            List<DeviceTask> batchTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
            
            if (batchTasks.isEmpty()) {
                log.warn("  ⚠️ 批次 {} 中没有任务，跳过", batchId);
                return;
            }
            
            log.debug("  批次包含 {} 个任务", batchTasks.size());
            
            // 3. 计算批次状态
            BatchStatusResult statusResult = calculateBatchStatus(batchTasks);
            
            log.debug("  批次状态: {}", statusResult.status);
            log.debug("  成功: {}, 失败: {}, 运行中: {}, 待执行: {}, 已取消: {}", 
                statusResult.completedCount, 
                statusResult.failedCount, 
                statusResult.runningCount,
                statusResult.pendingCount,
                statusResult.cancelledCount);
            
            // 4. 更新 dm_maintenance_batch 表
            updateBatchRecord(batchId, batchTasks, statusResult);
            
            // 5. 发送 TaskInfo 通知
            // ✅ 统一从 Batch 表读取 actionTime 和 createdBy（与 UPGRADE 批次保持一致）
            Long batchActionTime = batch.getBatchActionTime();
            String createdBy = batch.getCreatedBy();  // ⭐ 读取批次创建者
            
            if (batchActionTime == null) {
                log.error("  ⚠️ 批次 {} 的 batchActionTime 为空，无法发送 TaskInfo 通知", batchId);
            } else {
                taskInfoNotificationService.notifyBatchTaskCompleted(
                    batchId,
                    batch.getBatchName(),  // ✅ 从 Batch 读取
                    batchTasks.get(0).getTaskType(),
                    batchTasks,
                    batchActionTime,  // ✅ 统一数据来源
                    createdBy  // ⭐ 传入批次创建者（保持用户一致性）
                );
                log.debug("  ✅ 批次更新通知已发送");
            }
            
            log.debug("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
        } catch (Exception e) {
            log.error("❌ 发送批次更新通知失败: batchId={}", batchId, e);
            log.debug("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    /**
     * 计算批次状态
     */
    private BatchStatusResult calculateBatchStatus(List<DeviceTask> tasks) {
        BatchStatusResult result = new BatchStatusResult();
        
        long total = tasks.size();
        result.completedCount = tasks.stream().filter(t -> t.getStatus() == DeviceTask.TaskStatus.COMPLETED).count();
        result.failedCount = tasks.stream().filter(t -> t.getStatus() == DeviceTask.TaskStatus.FAILED).count();
        result.runningCount = tasks.stream().filter(t -> t.getStatus() == DeviceTask.TaskStatus.RUNNING).count();
        result.cancelledCount = tasks.stream().filter(t -> t.getStatus() == DeviceTask.TaskStatus.CANCELLED).count();
        result.pendingCount = tasks.stream().filter(t -> 
            t.getStatus() == DeviceTask.TaskStatus.PENDING || 
            t.getStatus() == DeviceTask.TaskStatus.SCHEDULED
        ).count();
        
        // 状态判断逻辑
        if (result.completedCount == total) {
            // 全部成功
            result.status = Batch.BatchStatus.COMPLETED;
        } else if (result.failedCount == total) {
            // 全部失败
            result.status = Batch.BatchStatus.FAILED;
        } else if (result.completedCount + result.failedCount == total && result.failedCount > 0) {
            // 全部完成，但有失败
            result.status = Batch.BatchStatus.COMPLETED_WITH_ERRORS;
        } else if (result.cancelledCount > 0 && result.runningCount == 0 && result.pendingCount == 0) {
            // 批次被取消
            result.status = Batch.BatchStatus.CANCELLED;
        } else if (result.runningCount > 0 || result.pendingCount > 0) {
            // 还有任务在执行或待执行
            result.status = Batch.BatchStatus.RUNNING;
        } else {
            // 兜底
            result.status = Batch.BatchStatus.RUNNING;
        }
        
        return result;
    }

    /**
     * 更新 dm_maintenance_batch 表
     */
    private void updateBatchRecord(String batchId, List<DeviceTask> tasks, BatchStatusResult statusResult) {
        try {
            Optional<Batch> batchOpt = maintenanceBatchRepository.findById(batchId);
            
            if (!batchOpt.isPresent()) {
                log.warn("  ⚠️ 批次 {} 在数据库中不存在，跳过更新", batchId);
                return;
            }
            
            Batch batch = batchOpt.get();
            
            // 更新状态
            batch.updateBatchStatus(statusResult.status);
            
            // 更新统计数量
            batch.updateCounts(
                (int) statusResult.completedCount,
                (int) statusResult.failedCount,
                (int) statusResult.runningCount
            );
            
            // 更新详情JSON（可选，包含设备列表）
            String detailJson = buildBatchDetailJson(tasks);
            batch.updateDetail(detailJson);
            
            maintenanceBatchRepository.save(batch);
            
            log.debug("  ✅ dm_maintenance_batch 表已更新");
            
        } catch (Exception e) {
            log.error("  ❌ 更新 dm_maintenance_batch 表失败: batchId={}", batchId, e);
        }
    }

    /**
     * 构建批次详情JSON
     */
    private String buildBatchDetailJson(List<DeviceTask> tasks) {
        try {
            List<Map<String, Object>> deviceSummaries = new ArrayList<>();
            
            for (DeviceTask task : tasks) {
                Map<String, Object> deviceInfo = new HashMap<>();
                deviceInfo.put("taskId", task.getTaskId());
                deviceInfo.put("deviceId", task.getDeviceId());
                deviceInfo.put("deviceName", task.getDeviceName());
                deviceInfo.put("deviceIp", task.getDeviceIp());
                deviceInfo.put("status", task.getStatus().name());
                deviceInfo.put("updatedTime", task.getUpdatedTime() != null ? task.getUpdatedTime().toString() : null);
                if (task.getErrorMessage() != null) {
                    deviceInfo.put("errorMessage", task.getErrorMessage());
                }
                deviceSummaries.add(deviceInfo);
            }
            
            return objectMapper.writeValueAsString(deviceSummaries);
            
        } catch (JsonProcessingException e) {
            log.error("构建批次detail JSON失败", e);
            return "[]";
        }
    }

    /**
     * 批次状态计算结果
     */
    private static class BatchStatusResult {
        Batch.BatchStatus status;
        long completedCount;
        long failedCount;
        long runningCount;
        long pendingCount;
        long cancelledCount;
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UpgradeWorkflow 相关处理（新架构）
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 检查并更新升级批次的工作流状态
     * 当设备操作状态变更时调用
     * 
     * ⚠️ 注意：此方法需要被 UpgradeTaskPollingService 调用，因此设为 public
     */
    public void checkAndUpdateUpgradeWorkflow(String deviceId, String operationType, String operationStatus) {
        try {
            // 1. 查找该设备相关的所有工作流（不再根据 status 过滤，因为 status 是计算字段）
            List<devicemaintenance.entity.UpgradeWorkflow> workflows = 
                workflowRepository.findByDeviceId(deviceId);

            if (workflows.isEmpty()) {
                return;
            }
            
            // 计算所有workflow的状态
            workflowManagementService.computeAndSetWorkflowStatuses(workflows);
            
            log.debug("  发现 {} 个相关的升级工作流", workflows.size());
            
            // ⭐ 收集所有 batchId（用于后续更新 Batch 状态）
            Set<String> allBatchIds = workflows.stream()
                .map(devicemaintenance.entity.UpgradeWorkflow::getBatchId)
                .collect(java.util.stream.Collectors.toSet());
            
            // 过滤出未完成的workflow（用于触发下一步和更新工作流状态）
            List<devicemaintenance.entity.UpgradeWorkflow> unfinishedWorkflows = workflows.stream()
                .filter(wf -> !wf.isFinished())
                .collect(java.util.stream.Collectors.toList());

            // 2. 更新未完成的工作流状态
            for (devicemaintenance.entity.UpgradeWorkflow workflow : unfinishedWorkflows) {
                // ⭐ 只更新工作流状态，不再重复更新 DeviceTask
                workflowManagementService.updateWorkflowStatus(workflow.getWorkflowId());
            }

            // 3. 检查是否需要自动触发下一步（自动模式）
            // ✅ 为每个批次触发检查，让 checkAndAutoTriggerNextStep 内部决定是否需要触发
            // 注意：该方法内部会重新查询批次的所有 workflows，并筛选出就绪的设备，
            // 因此即使被多次调用也是安全的（幂等性）
            Set<String> triggeredBatchIds = new HashSet<>();
            for (devicemaintenance.entity.UpgradeWorkflow workflow : unfinishedWorkflows) {
                String batchId = workflow.getBatchId();
                // 在当前通知处理过程中，每个批次只触发一次检查（避免无意义的重复查询）
                if (triggeredBatchIds.add(batchId)) {
                    checkAndAutoTriggerNextStep(batchId);
                }
            }
            
            // 4. ⭐ 更新所有相关批次的状态（包括已完成/失败的）
            for (String batchId : allBatchIds) {
                updateUpgradeBatchStatusImmediately(batchId);
            }

        } catch (Exception e) {
            log.error("更新升级工作流失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 自动升级批次自愈入口：
     * - 重新计算批次状态
     * - 再次检查是否有已完成当前步但尚未触发下一步的 workflow
     *
     * 用于补偿 Kafka 通知丢失、调度时序抖动等场景。
     */
    public void reconcileAutomaticBatchProgress(String batchId) {
        try {
            Optional<Batch> batchOpt = upgradeBatchRepository.findById(batchId);
            if (!batchOpt.isPresent()) {
                log.debug("自动批次巡检跳过，批次不存在: {}", batchId);
                return;
            }

            Batch batch = batchOpt.get();
            if (batch.getBatchType() != Batch.BatchType.UPGRADE) {
                return;
            }
            if (batch.getExecutionMode() != Batch.ExecutionMode.AUTOMATIC) {
                return;
            }
            if (batch.getStatus() == Batch.BatchStatus.CANCELLED ||
                batch.getStatus() == Batch.BatchStatus.COMPLETED ||
                batch.getStatus() == Batch.BatchStatus.COMPLETED_WITH_ERRORS ||
                batch.getStatus() == Batch.BatchStatus.FAILED) {
                return;
            }

            log.debug("开始自动升级批次巡检: batchId={}, status={}", batchId, batch.getStatus());
            checkAndAutoTriggerNextStep(batchId);
            updateUpgradeBatchStatusImmediately(batchId);
        } catch (Exception e) {
            log.error("自动升级批次巡检失败: batchId={}", batchId, e);
        }
    }

    /**
     * 更新工作流的步骤状态（通过更新关联的 DeviceTask）
     */
    private void updateWorkflowStepStatus(
        devicemaintenance.entity.UpgradeWorkflow workflow,
        String operationType,
        String operationStatus
    ) {
        devicemaintenance.entity.DeviceTask.TaskStatus taskStatus = 
            mapOperationStatusToTaskStatus(operationStatus);

        devicemaintenance.entity.DeviceTask.TaskType taskType = 
            mapOperationTypeToTaskType(operationType);
        if (taskType == null) {
            return;
        }

        log.debug("  更新工作流关联任务: workflowId={}, taskType={}, status={}", 
            workflow.getWorkflowId(), taskType, taskStatus);

        // 查找并更新对应的 DeviceTask
        List<devicemaintenance.entity.DeviceTask> tasks = 
            deviceTaskRepository.findByWorkflowId(workflow.getWorkflowId());
        
        for (devicemaintenance.entity.DeviceTask task : tasks) {
            if (task.getTaskType() == taskType) {
                // ✅ 设置状态和时间戳
                task.markStatus(taskStatus);
                
                // ✅ 根据状态设置对应的时间戳
                if (taskStatus == devicemaintenance.entity.DeviceTask.TaskStatus.RUNNING) {
                    if (task.getStartedTime() == null) {
                        task.setStartedTime(LocalDateTime.now());
                    }
                } else if (taskStatus == devicemaintenance.entity.DeviceTask.TaskStatus.COMPLETED) {
                    if (task.getCompletedTime() == null) {
                        task.setCompletedTime(LocalDateTime.now());
                    }
                } else if (taskStatus == devicemaintenance.entity.DeviceTask.TaskStatus.FAILED) {
                    if (task.getCompletedTime() == null) {
                        task.setCompletedTime(LocalDateTime.now());
                    }
                    task.setError("步骤执行失败");
                }
                
                deviceTaskRepository.save(task);
                log.debug("  ✅ DeviceTask 状态已更新: status={}, completedTime={}", 
                    task.getStatus(), task.getCompletedTime());
                
                // 更新工作流状态
                workflowManagementService.updateWorkflowStatus(workflow.getWorkflowId());
                break;
            }
        }
    }

    /**
     * 映射操作类型到任务类型
     */
    private devicemaintenance.entity.DeviceTask.TaskType mapOperationTypeToTaskType(String operationType) {
        if (operationType == null) return null;
        
        switch (operationType.toUpperCase()) {
            case "DOWNLOAD":
            case "SOFTWARE_DOWNLOAD":
                return devicemaintenance.entity.DeviceTask.TaskType.DOWNLOAD;
            case "BACKUP":
            case "DATABASE_BACKUP":
                return devicemaintenance.entity.DeviceTask.TaskType.BACKUP;
            case "UPGRADE":
            case "SOFTWARE_UPGRADE":
            case "ACTIVATE":
                return devicemaintenance.entity.DeviceTask.TaskType.UPGRADE;
            case "RESTORE":
            case "DATABASE_RESTORE":
                return devicemaintenance.entity.DeviceTask.TaskType.RESTORE;
            case "ROLLBACK":
            case "SOFTWARE_ROLLBACK":
                return devicemaintenance.entity.DeviceTask.TaskType.ROLLBACK;
            case "COMMIT":
            case "SOFTWARE_COMMIT":
                return devicemaintenance.entity.DeviceTask.TaskType.COMMIT;
            default:
                return null;
        }
    }

    /**
     * 映射操作状态到任务状态
     */
    private devicemaintenance.entity.DeviceTask.TaskStatus mapOperationStatusToTaskStatus(String operationStatus) {
        if (operationStatus == null) {
            return devicemaintenance.entity.DeviceTask.TaskStatus.PENDING;
        }

        switch (normalizeState(operationStatus)) {
            case "DOWNLOADING":  // ⭐ 下载中
            case "RUNNING":
            case "IN_PROGRESS":
            case "PROCESSING":
                return devicemaintenance.entity.DeviceTask.TaskStatus.RUNNING;
            case "COMPLETE":     // ⭐ Kafka 通知使用 "COMPLETE" 不是 "COMPLETED"！
            case "SUCCESS":
            case "COMPLETED":
            case "DONE":
                return devicemaintenance.entity.DeviceTask.TaskStatus.COMPLETED;
            case "FAIL":         // ⭐ Kafka 通知使用 "FAIL" 不是 "FAILED"！
            case "FAILED":
            case "ERROR":
                return devicemaintenance.entity.DeviceTask.TaskStatus.FAILED;
            case "CANCELLED":
            case "CANCELED":
                return devicemaintenance.entity.DeviceTask.TaskStatus.CANCELLED;
            case "SCHEDULED":
                return devicemaintenance.entity.DeviceTask.TaskStatus.SCHEDULED;
            default:
                log.warn("  ⚠️ 未识别的操作状态: {} - 默认为 PENDING", operationStatus);
                return devicemaintenance.entity.DeviceTask.TaskStatus.PENDING;
        }
    }

    /**
     * 立即更新升级批次状态（不用防抖）+ 调度 TaskInfo 通知（防抖）
     */
    private void updateUpgradeBatchStatusImmediately(String batchId) {
        try {
            // 1. 查询批次
            Optional<Batch> batchOpt = upgradeBatchRepository.findById(batchId);
            if (!batchOpt.isPresent()) {
                log.warn("  批次不存在: {}", batchId);
                return;
            }

            Batch batch = batchOpt.get();

            // 2. 查询所有工作流
            List<devicemaintenance.entity.UpgradeWorkflow> workflows = 
                workflowManagementService.getWorkflowsByBatchId(batchId);

            if (workflows.isEmpty()) {
                log.warn("  批次没有工作流: {}", batchId);
                return;
            }

            // ✅ 计算所有workflow的状态
            workflowManagementService.computeAndSetWorkflowStatuses(workflows);

            // ⭐ 始终重新计算批次状态（移除跳过逻辑，确保 retry/delete 后状态正确更新）
            // 3. 计算批次状态和统计信息
            Batch.BatchStatus newStatus = batchUpgradeService.calculateBatchStatusPublic(batch, workflows);
            
            // 计算成功/失败数量
            long completedCount = workflows.stream().filter(wf -> wf.isCompleted()).count();
            long failedCount = workflows.stream().filter(wf -> wf.isFailed()).count();
            long runningCount = workflows.stream().filter(wf -> !wf.isFinished()).count();
            
            // ⭐ 立即更新批次记录（不用防抖）
            batch.updateBatchStatus(newStatus);
            batch.updateCounts((int)completedCount, (int)failedCount, (int)runningCount);
            upgradeBatchRepository.save(batch);

            log.debug("  ✅ 批次状态已更新: {} (成功:{}, 失败:{}, 运行中:{})", 
                newStatus, completedCount, failedCount, runningCount);
            
            // 4. 调度 TaskInfo 通知（防抖，避免频繁网络调用）
            scheduleUpgradeBatchNotification(batchId);

        } catch (Exception e) {
            log.error("更新升级批次状态失败: batchId={}", batchId, e);
        }
    }

    /**
     * 调度升级批次通知（防抖）- 仅用于 TaskInfo 通知
     */
    private void scheduleUpgradeBatchNotification(String batchId) {
        String key = "UPGRADE_" + batchId;

        // 取消之前的调度
        ScheduledFuture<?> existingTask = batchUpdateSchedulers.get(key);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
            log.debug("  ⏱️ 取消之前的批次通知调度: {}", key);
        }

        // 调度新的通知（1秒后）- 减少延迟，提高响应速度
        ScheduledFuture<?> newTask = taskScheduler.schedule(
            () -> sendUpgradeBatchNotification(batchId),
            Instant.now().plus(Duration.ofSeconds(1))
        );

        batchUpdateSchedulers.put(key, newTask);
        log.debug("  ⏱️ 已调度批次通知（1秒后）: {}", key);
    }

    /**
     * 发送升级批次通知到 TaskInfo（仅发送通知，不更新批次状态）
     */
    private void sendUpgradeBatchNotification(String batchId) {
        try {
            log.debug("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.debug("📤 [防抖触发] 发送 TaskInfo 通知: batchId={}", batchId);

            // 1. 查询批次
            Optional<Batch> batchOpt = upgradeBatchRepository.findById(batchId);
            if (!batchOpt.isPresent()) {
                log.warn("  批次不存在: {}", batchId);
                return;
            }

            Batch batch = batchOpt.get();

            // ✅ 使用 UnifiedBatchService 构建完整的批次详情 JSON（与 getBatchDetail API 完全一致）
            String detailJson = unifiedBatchService.convertBatchToJsonForNotification(batchId);

            // ⭐ 根据批次类型动态设置 taskType
            // UPGRADE 使用 BATCH_UPGRADE，BACKUP/RESTORE 使用简单格式
            String taskType;
            if (batch.getBatchType() == devicemaintenance.entity.Batch.BatchType.UPGRADE) {
                taskType = "BATCH_UPGRADE";
            } else {
                taskType = batch.getBatchType().name();  // BACKUP 或 RESTORE
            }

            // 发送 TaskInfo 通知（⭐ 传入批次创建者，保持用户一致性）
            taskInfoNotificationService.sendTaskInfoNotification(
                batch.getBatchId(),
                batch.getBatchName(),
                batch.getBatchActionTime(),
                taskType,
                detailJson,
                batch.getStatus() == Batch.BatchStatus.COMPLETED || 
                batch.getStatus() == Batch.BatchStatus.COMPLETED_WITH_ERRORS,
                batch.getCreatedBy()  // ⭐ 传入批次创建者
            );

            log.debug("  ✅ TaskInfo 通知已发送");
            log.debug("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("发送 TaskInfo 通知失败: batchId={}", batchId, e);
        } finally {
            batchUpdateSchedulers.remove("UPGRADE_" + batchId);
        }
    }

    /**
     * 计算升级批次状态
     * 
     * 逻辑优先级：
     * 1. 终态判断（FAILED, COMPLETED, COMPLETED_WITH_ERRORS）
     * 2. 执行中判断：
     *    - 自动模式: RUNNING
     *    - 手动模式: 根据 currentStep 判断
     *      - 有任务正在运行: DOWNLOADING/BACKING_UP/UPGRADING/COMMITTING
     *      - 所有当前步骤已完成: READY_TO_BACKUP/READY_TO_UPGRADE/READY_TO_COMMIT
     * 
     * ⚠️ 注意：在调用此方法前，必须先调用 workflowManagementService.computeAndSetWorkflowStatuses(workflows)
     */
    private Batch.BatchStatus calculateUpgradeBatchStatus(
        Batch batch,
        List<devicemaintenance.entity.UpgradeWorkflow> workflows
    ) {
        if (workflows.isEmpty()) {
            return Batch.BatchStatus.CANCELLED;
        }

        // ✅ 使用 isCompleted() 和 isFailed() 方法判断
        long completedCount = workflows.stream()
            .filter(wf -> wf.isCompleted())
            .count();

        long failedCount = workflows.stream()
            .filter(wf -> wf.isFailed())
            .count();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 1️⃣ 终态判断：优先级最高
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        // ⭐ 只要有失败就是 FAILED（不需要等所有都结束）
        if (failedCount > 0) {
            log.info("  终态判断: 有 {} 个失败，批次状态 = FAILED", failedCount);
            return Batch.BatchStatus.FAILED;
        }

        // 全部成功
        if (completedCount == workflows.size()) {
            return Batch.BatchStatus.COMPLETED;
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 2️⃣ 执行中状态判断
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        // 自动模式：直接返回 RUNNING
        if (batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC) {
            return Batch.BatchStatus.RUNNING;
        }

        // 手动模式：根据 currentStep 和任务状态细分
        String currentStep;
        try {
            currentStep = workflowManagementService.resolveCurrentStep(workflows);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("inconsistent currentStep values")) {
                log.warn("  ⚠️ 手动模式检测到混合步骤，返回 RUNNING: batchId={}, message={}",
                    batch.getBatchId(), ex.getMessage());
                return Batch.BatchStatus.RUNNING;
            }
            throw ex;
        }
        
        if (currentStep == null) {
            log.warn("  ⚠️ 所有 workflow 都失败了，无法获取 currentStep");
            return Batch.BatchStatus.FAILED;
        }
        
        // 检查是否有任务正在运行
        boolean allCurrentStepCompleted = workflowManagementService.areAllCurrentStepsCompleted(workflows);
        
        if (!allCurrentStepCompleted) {
            // ⭐ 进一步检查：是"未开始"还是"正在运行"
            boolean hasRunningTask = workflowManagementService.hasRunningTasks(workflows, currentStep);
            
            if (hasRunningTask) {
                // 有任务正在运行，返回对应的执行中状态
                switch (currentStep.toUpperCase()) {
                    case "DOWNLOAD":
                        return Batch.BatchStatus.DOWNLOADING;
                    case "BACKUP":
                        return Batch.BatchStatus.BACKING_UP;
                    case "UPGRADE":
                        return Batch.BatchStatus.UPGRADING;
                    case "COMMIT":
                        return Batch.BatchStatus.COMMITTING;
                    default:
                        log.warn("  ⚠️ 未知的步骤类型: {}", currentStep);
                        return Batch.BatchStatus.RUNNING;
                }
            } else {
                // 任务未开始（NOT_START），返回 READY_TO_PROCEED
                log.debug("  ✅ 任务未开始（NOT_START），批次状态: READY_TO_PROCEED");
                return Batch.BatchStatus.READY_TO_PROCEED;
            }
        }
        
        // 所有当前步骤已完成，返回 READY_TO_XXX 状态
        String nextStep = workflowManagementService.getNextEnabledStep(batch, currentStep);
        if (nextStep == null) {
            // ⭐ 特殊处理：如果当前步骤是 UPGRADE，且没有下一步，说明等待 COMMIT
            if ("UPGRADE".equalsIgnoreCase(currentStep)) {
                log.debug("  ✅ UPGRADE 步骤已完成，等待手动 COMMIT");
                return Batch.BatchStatus.READY_TO_COMMIT;
            }
            
            // 其他情况：没有更多启用的步骤，批次完成
            // 例如：只启用了 DOWNLOAD，完成后直接 COMPLETED
            log.debug("  ✅ 已完成所有启用的步骤: currentStep={}", currentStep);
            return Batch.BatchStatus.COMPLETED;
        }
        
        switch (nextStep.toUpperCase()) {
            case "BACKUP":
                log.debug("  ✅ 批次状态: READY_TO_BACKUP (当前步骤 {} 已完成)", currentStep);
                return Batch.BatchStatus.READY_TO_BACKUP;
            case "UPGRADE":
                log.debug("  ✅ 批次状态: READY_TO_UPGRADE (当前步骤 {} 已完成)", currentStep);
                return Batch.BatchStatus.READY_TO_UPGRADE;
            case "COMMIT":
                log.debug("  ✅ 批次状态: READY_TO_COMMIT (当前步骤 {} 已完成)", currentStep);
                return Batch.BatchStatus.READY_TO_COMMIT;
            default:
                log.warn("  ⚠️ 未知的下一步类型: {}", nextStep);
                return Batch.BatchStatus.RUNNING;
        }
    }


    /**
     * 检查并自动触发下一步（仅 AUTOMATIC 模式）
     *
     * 并发安全性：
     * - 使用批次级别的 synchronized 锁，确保同一批次在同一时间只有一个线程执行触发逻辑
     * - 多个设备的通知可能并发到达，但只有一个线程能获取锁并执行
     * - 锁对象存储在 batchTriggerLocks 中，批次级别隔离
     *
     * 工作原理：
     * - AUTOMATIC 模式：自动触发下一步（延迟 operationInterval）
     * - MANUAL 模式：不自动触发，批次状态变为 READY_TO_XXX，等待用户手动调用 proceedToNextStep
     */
    private void checkAndAutoTriggerNextStep(String batchId) {
        // 获取批次级别的锁对象（computeIfAbsent 保证原子性创建）
        Object lock = batchTriggerLocks.computeIfAbsent(batchId, k -> new Object());

        // 同步块：确保同一批次在同一时间只有一个线程执行
        synchronized (lock) {
            try {
            // 1. 查询批次
            Optional<Batch> batchOpt = upgradeBatchRepository.findById(batchId);
            if (!batchOpt.isPresent()) {
                return;
            }

            Batch batch = batchOpt.get();

            // ✅ 只有 AUTOMATIC 模式才自动触发下一步
            if (batch.getExecutionMode() != Batch.ExecutionMode.AUTOMATIC) {
                log.debug("  ⏸️  MANUAL 模式，批次状态将变为 READY_TO_XXX，等待用户手动触发");
                return;
            }

            // ✅ 如果批次已被取消，不再触发下一步
            if (batch.getStatus() == Batch.BatchStatus.CANCELLED) {
                log.debug("  ⏸️  批次已被取消，不再触发下一步: batchId={}", batchId);
                return;
            }

            // ❌ COMMIT 永远不自动触发！必须手动触发！
            // 当批次状态为 READY_TO_COMMIT 时，应该停在那里等待用户手动触发
            if (batch.getStatus() == Batch.BatchStatus.READY_TO_COMMIT) {
                log.debug("  ⏸️  批次状态为 READY_TO_COMMIT，等待用户手动触发 COMMIT: batchId={}", batchId);
                return;
            }

            // 3. 查询所有工作流
            List<devicemaintenance.entity.UpgradeWorkflow> workflows = 
                workflowManagementService.getWorkflowsByBatchId(batchId);

            if (workflows.isEmpty()) {
                return;
            }
            
            // ✅ 计算所有workflow的状态
            workflowManagementService.computeAndSetWorkflowStatuses(workflows);

            // 4. 筛选出当前步骤已完成且可以继续的工作流（排除失败的）
            List<devicemaintenance.entity.UpgradeWorkflow> readyWorkflows = findReadyWorkflows(batch, workflows);

            if (readyWorkflows.isEmpty()) {
                log.debug("  没有可以继续的工作流: batchId={}", batchId);
                return;
            }

            // 5. 自动模式下允许设备各走各的。
            // 只要有设备当前步骤已完成且存在下一步，就调度一次批次巡检推进。
            boolean hasAnyNextStep = readyWorkflows.stream()
                .map(devicemaintenance.entity.UpgradeWorkflow::getCurrentStep)
                .filter(java.util.Objects::nonNull)
                .anyMatch(step -> workflowManagementService.getNextEnabledStep(batch, step) != null);
            if (!hasAnyNextStep) {
                log.debug("  所有步骤已完成，无需继续: batchId={}", batchId);
                return;
            }

            // ✅ 获取 operationInterval（秒）
            Integer operationInterval = batch.getOperationInterval();
            if (operationInterval == null || operationInterval < 0) {
                operationInterval = 0;  // 默认立即执行
            }

            ScheduledFuture<?> existingAutoStepTask = batchAutoStepSchedulers.get(batchId);
            String pendingStep = batchAutoStepTargets.get(batchId);
            if (existingAutoStepTask != null && !existingAutoStepTask.isDone()) {
                if ("AUTO_PROGRESS".equalsIgnoreCase(pendingStep)) {
                    log.debug("  ⏭️  已存在自动流转调度，跳过重复触发: batchId={}", batchId);
                    return;
                }

                log.debug("  ⏱️  取消过期的自动流转调度: batchId={}, oldTarget={}", batchId, pendingStep);
                existingAutoStepTask.cancel(false);
                batchAutoStepSchedulers.remove(batchId);
                batchAutoStepTargets.remove(batchId);
            }

            log.debug("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.debug("🤖 [自动流转] 调度批次巡检推进: batchId={}, readyCount={}/{}, interval={}秒",
                batchId, readyWorkflows.size(), workflows.size(), operationInterval);

            // 6. 延迟触发下一步（延迟 operationInterval 秒）
            // ⚠️ 使用 taskScheduler 调度延迟任务，而不是 CompletableFuture.runAsync()
            final int finalInterval = operationInterval;
            ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                () -> executeScheduledAutoStep(batchId),
                Instant.now().plus(Duration.ofSeconds(finalInterval))
            );

            batchAutoStepSchedulers.put(batchId, scheduledFuture);
            batchAutoStepTargets.put(batchId, "AUTO_PROGRESS");

            } catch (Exception e) {
                log.error("检查自动触发失败: batchId={}", batchId, e);
            }
        } // synchronized 块结束
    }

    private List<devicemaintenance.entity.UpgradeWorkflow> findReadyWorkflows(
        Batch batch,
        List<devicemaintenance.entity.UpgradeWorkflow> workflows
    ) {
        List<devicemaintenance.entity.UpgradeWorkflow> readyWorkflows = new ArrayList<>();

        for (devicemaintenance.entity.UpgradeWorkflow wf : workflows) {
            if (wf.isFailed() || wf.getCurrentStep() == null) {
                continue;
            }

            List<devicemaintenance.entity.DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceId(
                wf.getBatchId(),
                wf.getDeviceId()
            );

            boolean currentStepCompleted = isWorkflowStepConfirmedByOp(batch, wf, tasks);

            if (!currentStepCompleted) {
                continue;
            }

            String nextStep = workflowManagementService.getNextEnabledStep(batch, wf.getCurrentStep());
            if (nextStep != null) {
                readyWorkflows.add(wf);
            }
        }

        return readyWorkflows;
    }

    boolean isWorkflowStepConfirmedByOp(
        Batch batch,
        devicemaintenance.entity.UpgradeWorkflow workflow,
        List<devicemaintenance.entity.DeviceTask> tasks
    ) {
        String currentStep = workflow.getCurrentStep();
        if (currentStep == null) {
            return false;
        }

        try {
            DeviceOperationStatus deviceStatus =
                deviceMaintenanceStatusService.getDeviceOperationStatus(workflow.getDeviceId());
            String deviceDisplay = buildWorkflowDeviceDisplay(workflow, tasks, deviceStatus);
            if (deviceStatus == null) {
                log.info("  ℹ️ OP状态为空，自动推进保持等待: batchId={}, deviceId={}, device={}, step={}",
                    batch.getBatchId(), workflow.getDeviceId(), deviceDisplay, currentStep);
                return false;
            }

            switch (currentStep.toUpperCase()) {
                case "DOWNLOAD":
                    String downloadState = normalizeState(
                        deviceStatus.getSoftwareOperations() != null &&
                        deviceStatus.getSoftwareOperations().getDownload() != null
                            ? deviceStatus.getSoftwareOperations().getDownload().getState()
                            : null
                    );
                    boolean persistedDownloadCompleted =
                        DownloadTerminalEvidencePolicy.isLatestDownloadCompleted(tasks);
                    boolean downloadReady = "COMPLETE".equals(downloadState)
                        && persistedDownloadCompleted;
                    log.info("  🔎 [自动推进-OP确认] batchId={}, deviceId={}, device={}, step=DOWNLOAD, op.downloadState={}, persistedCompleted={}, taskCount={}, ready={}",
                        batch.getBatchId(), workflow.getDeviceId(), deviceDisplay, downloadState,
                        persistedDownloadCompleted, tasks.size(), downloadReady);
                    return downloadReady;
                case "BACKUP":
                    String backupState = normalizeState(
                        deviceStatus.getDatabaseOperations() != null &&
                        deviceStatus.getDatabaseOperations().getBackup() != null
                            ? deviceStatus.getDatabaseOperations().getBackup().getState()
                            : null
                    );
                    boolean backupReady = "COMPLETE".equals(backupState);
                    log.info("  🔎 [自动推进-OP确认] batchId={}, deviceId={}, device={}, step=BACKUP, op.backupState={}, taskCount={}, ready={}",
                        batch.getBatchId(), workflow.getDeviceId(), deviceDisplay, backupState, tasks.size(), backupReady);
                    return backupReady;
                case "UPGRADE":
                    String upgradeState = normalizeState(
                        deviceStatus.getSoftwareOperations() != null &&
                        deviceStatus.getSoftwareOperations().getUpgrade() != null
                            ? deviceStatus.getSoftwareOperations().getUpgrade().getState()
                            : null
                    );
                    String targetVersion = firstNonBlank(
                        deviceStatus.getSoftwareOperations() != null &&
                        deviceStatus.getSoftwareOperations().getDownload() != null
                            ? deviceStatus.getSoftwareOperations().getDownload().getSoftwareVersion()
                            : null,
                        resolveTargetVersion(batch, tasks)
                    );
                    String currentSoftware = deviceStatus.getCurrentSoftware();
                    boolean upgradeReady = ("ACTIVE_COMPLETE".equals(upgradeState) || "COMPLETE".equals(upgradeState))
                        && targetVersion != null
                        && currentSoftware != null
                        && normalizeVersion(currentSoftware).equals(normalizeVersion(targetVersion));
                    log.info("  🔎 [自动推进-OP确认] batchId={}, deviceId={}, device={}, step=UPGRADE, op.upgradeState={}, op.currentSoftware={}, targetVersion={}, ready={}",
                        batch.getBatchId(), workflow.getDeviceId(), deviceDisplay, upgradeState, currentSoftware, targetVersion, upgradeReady);
                    if (!"ACTIVE_COMPLETE".equals(upgradeState) && !"COMPLETE".equals(upgradeState)) {
                        return false;
                    }
                    return upgradeReady;
                default:
                    boolean fallbackReady = tasks.stream().anyMatch(task -> task.getTaskType() != null
                        && task.getTaskType().name().equalsIgnoreCase(currentStep)
                        && task.getStatus() == devicemaintenance.entity.DeviceTask.TaskStatus.COMPLETED);
                    log.info("  🔎 [自动推进-Task回退确认] batchId={}, deviceId={}, device={}, step={}, taskCount={}, ready={}",
                        batch.getBatchId(), workflow.getDeviceId(), deviceDisplay, currentStep, tasks.size(), fallbackReady);
                    return fallbackReady;
            }
        } catch (Exception e) {
            log.warn("  ⚠️ OP状态确认失败，跳过自动推进: batchId={}, deviceId={}, step={}, error={}",
                batch.getBatchId(), workflow.getDeviceId(), currentStep, e.getMessage());
            return false;
        }
    }

    private String resolveTargetVersion(Batch batch, List<devicemaintenance.entity.DeviceTask> tasks) {
        return tasks.stream()
            .map(devicemaintenance.entity.DeviceTask::getTargetVersion)
            .filter(version -> version != null && !version.trim().isEmpty())
            .reduce((first, second) -> second)
            .orElse(batch.getTargetVersion());
    }

    private String buildWorkflowDeviceDisplay(
        devicemaintenance.entity.UpgradeWorkflow workflow,
        List<devicemaintenance.entity.DeviceTask> tasks,
        DeviceOperationStatus deviceStatus
    ) {
        for (devicemaintenance.entity.DeviceTask task : tasks) {
            if (task.getDeviceIp() != null && !task.getDeviceIp().trim().isEmpty()
                && task.getDeviceName() != null && !task.getDeviceName().trim().isEmpty()) {
                return task.getDeviceIp().trim() + " + " + task.getDeviceName().trim();
            }
            if (task.getDeviceIp() != null && !task.getDeviceIp().trim().isEmpty()) {
                return task.getDeviceIp().trim();
            }
            if (task.getDeviceName() != null && !task.getDeviceName().trim().isEmpty()) {
                return task.getDeviceName().trim();
            }
        }

        if (deviceStatus != null && deviceStatus.getDeviceName() != null && !deviceStatus.getDeviceName().trim().isEmpty()) {
            return deviceStatus.getDeviceName().trim();
        }

        return workflow.getDeviceId();
    }

    private void executeScheduledAutoStep(String batchId) {
        Object lock = batchTriggerLocks.computeIfAbsent(batchId, k -> new Object());

        synchronized (lock) {
            try {
                Optional<Batch> batchOpt = upgradeBatchRepository.findById(batchId);
                if (!batchOpt.isPresent()) {
                    log.debug("  自动流转执行跳过，批次不存在: batchId={}", batchId);
                    return;
                }

                Batch batch = batchOpt.get();
                if (batch.getExecutionMode() != Batch.ExecutionMode.AUTOMATIC ||
                    batch.getStatus() == Batch.BatchStatus.CANCELLED ||
                    batch.getStatus() == Batch.BatchStatus.COMPLETED ||
                    batch.getStatus() == Batch.BatchStatus.COMPLETED_WITH_ERRORS ||
                    batch.getStatus() == Batch.BatchStatus.FAILED ||
                    batch.getStatus() == Batch.BatchStatus.READY_TO_COMMIT) {
                    log.debug("  自动流转执行跳过，批次状态不允许继续: batchId={}, status={}", batchId, batch.getStatus());
                    return;
                }

                List<devicemaintenance.entity.UpgradeWorkflow> workflows =
                    workflowManagementService.getWorkflowsByBatchId(batchId);
                if (workflows.isEmpty()) {
                    log.debug("  自动流转执行跳过，没有工作流: batchId={}", batchId);
                    return;
                }

                workflowManagementService.computeAndSetWorkflowStatuses(workflows);
                List<devicemaintenance.entity.UpgradeWorkflow> readyWorkflows = findReadyWorkflows(batch, workflows);
                if (readyWorkflows.isEmpty()) {
                    log.debug("  自动流转执行跳过，没有就绪工作流: batchId={}", batchId);
                    return;
                }

                java.util.Map<String, List<devicemaintenance.entity.UpgradeWorkflow>> readyGroups =
                    groupReadyWorkflowsByCurrentStep(readyWorkflows);
                if (readyGroups.isEmpty()) {
                    log.debug("  自动流转执行跳过，没有可分组的就绪工作流: batchId={}", batchId);
                    return;
                }

                for (java.util.Map.Entry<String, List<devicemaintenance.entity.UpgradeWorkflow>> entry : readyGroups.entrySet()) {
                    String currentStep = entry.getKey();
                    String nextStep = workflowManagementService.getNextEnabledStep(batch, currentStep);
                    if (nextStep == null) {
                        continue;
                    }

                    batchUpgradeService.triggerNextStepForWorkflows(batchId, entry.getValue());
                    log.debug("  ✅ 自动触发下一步成功: batchId={}, currentStep={}, nextStep={}, workflowCount={}",
                        batchId, currentStep, nextStep, entry.getValue().size());
                }
            } catch (Exception e) {
                log.error("自动触发下一步失败: batchId={}", batchId, e);
            } finally {
                batchAutoStepSchedulers.remove(batchId);
                batchAutoStepTargets.remove(batchId);
            }
        }
    }

    private java.util.Map<String, List<devicemaintenance.entity.UpgradeWorkflow>> groupReadyWorkflowsByCurrentStep(
        List<devicemaintenance.entity.UpgradeWorkflow> readyWorkflows
    ) {
        java.util.Map<String, List<devicemaintenance.entity.UpgradeWorkflow>> grouped = readyWorkflows.stream()
            .filter(wf -> wf.getCurrentStep() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                devicemaintenance.entity.UpgradeWorkflow::getCurrentStep,
                java.util.LinkedHashMap::new,
                java.util.stream.Collectors.toList()
            ));

        java.util.List<String> orderedSteps = java.util.Arrays.asList("DOWNLOAD", "BACKUP", "UPGRADE");
        java.util.Map<String, List<devicemaintenance.entity.UpgradeWorkflow>> ordered = new java.util.LinkedHashMap<>();
        for (String step : orderedSteps) {
            if (grouped.containsKey(step)) {
                ordered.put(step, grouped.get(step));
            }
        }
        for (java.util.Map.Entry<String, List<devicemaintenance.entity.UpgradeWorkflow>> entry : grouped.entrySet()) {
            ordered.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return ordered;
    }
}
