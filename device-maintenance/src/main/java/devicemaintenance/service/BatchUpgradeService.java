package devicemaintenance.service;

import devicemaintenance.dto.BatchUpgradeDto;
import devicemaintenance.dto.DeviceOperationStatus;
import devicemaintenance.dto.SoftwareDownloadDto;
import devicemaintenance.dto.DeviceBackupDto;
import devicemaintenance.dto.DeviceUpgradeDto;
import devicemaintenance.dto.SoftwareCommitDto;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.BatchDevice;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.UpgradeWorkflow;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.repository.BatchDeviceRepository;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.UpgradeWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import org.springframework.util.StringUtils;

/**
 * 批量升级服务
 * 处理批量升级的业务逻辑，包括创建批次、执行升级、状态管理等
 * 
 * 使用统一线程池实现并发 RPC 调用：
 * - batchTaskExecutor: 用于所有批次操作的 RPC 并发调用
 * - 线程数是总体计数，不按命令分别计数
 * - download、backup、upgrade、commit、rollback 都使用这个线程池
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BatchUpgradeService {

    private final BatchRepository upgradeBatchRepository;
    private final BatchDeviceRepository batchDeviceRepository;
    private final DeviceTaskRepository deviceTaskRepository;
    private final SoftwareDownloadService softwareDownloadService;
    private final DeviceBackupService deviceBackupService;
    private final DeviceUpgradeService deviceUpgradeService;
    private final SoftwareCommitService softwareCommitService;
    private final TaskInfoNotificationService taskInfoNotificationService;
    private final WorkflowManagementService workflowManagementService;
    private final UpgradeWorkflowRepository workflowRepository;
    private final DeviceAvailabilityService deviceAvailabilityService;
    private final NeMgrIntegrationService neMgrIntegrationService;
    private final DeviceTaskUniquenessService uniquenessService;
    private final UnifiedBatchService unifiedBatchService;
    private final TaskCreationHelper taskCreationHelper;
    private final DeviceMaintenanceStatusService deviceMaintenanceStatusService;
    
    /**
     * JPA EntityManager (用于清除一级缓存)
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 统一批次任务线程池
     * 用于并发下发所有类型的 RPC 操作（download/backup/upgrade/commit/rollback）
     * maxPoolSize 控制所有操作的总并发数
     */
    @Qualifier("batchTaskExecutor")
    private final Executor batchTaskExecutor;

    /**
     * 查询批量升级任务列表
     */
    @Transactional(readOnly = true)
    public List<BatchUpgradeDto.UpgradeBatchSummary> getUpgradeBatches(
            BatchUpgradeDto.GetUpgradeBatchesRequest request) {

        Batch.BatchStatus statusFilter = null;
        String batchNameFilter = null;

        if (request != null) {
            if (StringUtils.hasText(request.getStatus())) {
                try {
                    statusFilter = Batch.BatchStatus.valueOf(request.getStatus().trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Invalid batch status: " + request.getStatus());
                }
            }

            if (StringUtils.hasText(request.getBatchName())) {
                batchNameFilter = request.getBatchName().trim().toLowerCase();
            }
        }

        List<Batch> batches;
        final Batch.BatchStatus statusFilterFinal = statusFilter;
        if (statusFilterFinal != null) {
            batches = upgradeBatchRepository.findByStatusOrderByCreatedTimeDesc(statusFilterFinal);
        } else {
            batches = upgradeBatchRepository.findAllByOrderByCreatedTimeDesc();
        }

        final String batchNameFilterFinal = batchNameFilter;
        if (batchNameFilterFinal != null) {
            batches = batches.stream()
                    .filter(batch -> batch.getBatchName() != null &&
                            batch.getBatchName().toLowerCase().contains(batchNameFilterFinal))
                    .collect(Collectors.toList());
        }

        return batches.stream()
                .map(this::convertToSummary)
                .collect(Collectors.toList());
    }

    private BatchUpgradeDto.UpgradeBatchSummary convertToSummary(Batch batch) {
        BatchUpgradeDto.UpgradeBatchSummary summary = new BatchUpgradeDto.UpgradeBatchSummary();
        summary.setBatchId(batch.getBatchId());
        summary.setBatchName(batch.getBatchName());
        summary.setUpgradeFilePath(batch.getFilePath());
        summary.setSftpServerName(batch.getSftpServerName());
        summary.setOperationInterval(batch.getOperationInterval());
        summary.setDeviceCount(batch.getDeviceCount());
        
        // 执行模式配置
        summary.setExecutionMode(batch.getExecutionMode() != null ? batch.getExecutionMode().name() : null);
        summary.setScheduledMode(batch.getScheduledMode() != null ? batch.getScheduledMode().name() : null);
        summary.setScheduledTime(batch.getScheduledTime());
        
        // 工作流配置
        summary.setEnableDownload(batch.getEnableDownload());
        summary.setEnableBackup(batch.getEnableBackup());
        summary.setEnableUpgrade(batch.getEnableUpgrade());
        
        // 重试配置
        summary.setMaxRetryCount(batch.getMaxRetryCount());
        
        // 调试配置
        summary.setDebug(batch.getDebug());
        
        // 状态信息
        summary.setStatus(batch.getStatus() != null ? batch.getStatus().name() : null);
        // ⚠️ 已删除步骤状态字段
        
        // 统计信息
        summary.setSuccessCount(batch.getSuccessCount());
        summary.setFailedCount(batch.getFailedCount());
        
        // 时间信息
        summary.setCreatedTime(batch.getCreatedTime());
        summary.setUpdatedTime(batch.getUpdatedTime());
        summary.setStartedTime(batch.getStartedTime());
        summary.setCompletedTime(batch.getCompletedTime());

        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batch.getBatchId());
        List<BatchUpgradeDto.DeviceTaskSummary> taskSummaries = tasks.stream()
                .map(this::convertToTaskSummary)
                .collect(Collectors.toList());
        summary.setTasks(taskSummaries);

        return summary;
    }

    private BatchUpgradeDto.DeviceTaskSummary convertToTaskSummary(DeviceTask task) {
        return convertToTaskSummary(task, false);
    }

    private BatchUpgradeDto.DeviceTaskSummary convertToTaskSummary(DeviceTask task, boolean includeDebugPayload) {
        BatchUpgradeDto.DeviceTaskSummary summary = new BatchUpgradeDto.DeviceTaskSummary();
        summary.setTaskId(task.getTaskId());
        summary.setBatchId(task.getBatchId());
        summary.setTaskType(task.getTaskType() != null ? task.getTaskType().name() : null);
        summary.setDeviceId(task.getDeviceId());
        summary.setDeviceName(task.getDeviceName());
        summary.setSftpServerName(task.getSftpServerName());
        summary.setFilePath(task.getFilePath());
        summary.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        summary.setErrorMessage(task.getErrorMessage());
        summary.setCreatedTime(task.getCreatedTime());
        summary.setUpdatedTime(task.getUpdatedTime());
        summary.setStartedTime(task.getStartedTime());
        summary.setCompletedTime(task.getCompletedTime());
        summary.setCurrentVersion(task.getCurrentVersion());
        summary.setPreviousVersion(task.getPreviousVersion());
        summary.setTargetVersion(task.getTargetVersion());
        summary.setRetryCount(task.getRetryCount());
        
        // ✅ debug模式下包含RPC payload
        if (includeDebugPayload && task.getDebugPayload() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object debugPayloadObj = objectMapper.readValue(task.getDebugPayload(), Object.class);
                summary.setDebugPayload(debugPayloadObj);
                log.debug("  ✓ Debug payload loaded for task: {}", task.getTaskId());
            } catch (Exception e) {
                log.warn("  Failed to parse debug payload for task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
        
        return summary;
    }

    /**
     * 创建批量升级任务
     */
    @Transactional
    public BatchUpgradeDto.BatchUpgradeResponse createBatchUpgrade(
            BatchUpgradeDto.CreateBatchUpgradeRequest request) {
        log.info("创建批量升级任务: batchName={}, deviceCount={}, executionMode={}", 
                request.getBatchName(), request.getDeviceIds().size(), request.getExecutionMode());

        try {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 📋 第一阶段：Pre-validation（所有验证必须通过，才能创建任何资源）
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("📋 Pre-validation 开始");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // ✅ 1. 检查批次名称是否已存在
            if (upgradeBatchRepository.existsByBatchName(request.getBatchName())) {
                throw new IllegalArgumentException("Batch name already exists: " + request.getBatchName());
            }
            log.info("  ✓ 批次名称可用: {}", request.getBatchName());

            // ✅ 2. 步骤配置验证
            Boolean enableDownload = request.getEnableDownload() != null ? request.getEnableDownload() : true;
            Boolean enableBackup = request.getEnableBackup() != null ? request.getEnableBackup() : true;
            Boolean enableUpgrade = request.getEnableUpgrade() != null ? request.getEnableUpgrade() : true;
            
            if (!enableDownload && !enableBackup && !enableUpgrade) {
                throw new IllegalArgumentException("At least one step (download, backup, or upgrade) must be enabled");
            }
            validateDevicesBeforeBatchCreation(request.getDeviceIds());
            log.info("  ✓ 步骤配置: download={}, backup={}, upgrade={}", enableDownload, enableBackup, enableUpgrade);
            
            // ✅ 3. 如果启用备份，必须提供备份路径
            if (enableBackup && request.getBackupBasePath() == null) {
                throw new IllegalArgumentException("backupRootPath parameter is required when backup is enabled");
            }
            if (enableBackup) {
                log.info("  ✓ 备份根路径: {}", request.getBackupBasePath());
            }

            // ✅ 4. 验证定时执行时间
            if ("SCHEDULED".equals(request.getScheduledMode()) && 
                (request.getScheduledTime() == null || request.getScheduledTime() < System.currentTimeMillis())) {
                throw new IllegalArgumentException("Scheduled time cannot be empty and must not be earlier than current time");
            }
            if ("SCHEDULED".equals(request.getScheduledMode())) {
                log.info("  ✓ 定时执行时间: {}", request.getScheduledTime());
            }
            
            // ✅ 5. 检查设备可用性（设备不在其他任务中）
            deviceAvailabilityService.validateDevicesAvailable(request.getDeviceIds(), "批量升级");
            log.info("  ✓ 所有设备可用性检查通过: {} 台设备", request.getDeviceIds().size());
            
            // ✅ 6. 对于 IMMEDIATE 模式，预先验证所有设备信息和SFTP服务器
            if ("IMMEDIATE".equals(request.getScheduledMode())) {
                log.info("  🔍 IMMEDIATE 模式，预验证设备信息和SFTP服务器...");
                
                // 验证所有设备信息
                for (String deviceId : request.getDeviceIds()) {
                    try {
                        neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
        } catch (Exception e) {
                        throw new RuntimeException(
                            "Device information unavailable: "
                                + resolveDeviceDisplay(null, deviceId)
                                + " - " + e.getMessage(),
                            e
                        );
                    }
                }
                log.info("  ✓ 所有设备信息验证通过");
                
                // 验证 SFTP 服务器（如果提供了）
                if (request.getSftpServerName() != null) {
                    try {
                        neMgrIntegrationService.getSftpServerById(request.getSftpServerName());
                        log.info("  ✓ SFTP 服务器验证通过: {}", request.getSftpServerName());
        } catch (Exception e) {
                        throw new RuntimeException("SFTP server unavailable: " + request.getSftpServerName() + " - " + e.getMessage(), e);
                    }
                }
            }
            
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ Pre-validation 全部通过，开始创建批次");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // 创建批次
            String batchId = UUID.randomUUID().toString();
            Long batchActionTime = System.currentTimeMillis();  // 生成批次创建时间戳，用于 TaskInfo 绑定
            
            Batch batch = new Batch(
                batchId, 
                request.getBatchName(), 
                Batch.BatchType.UPGRADE,
                request.getDeviceIds().size(),
                batchActionTime
            );

            // ⭐ 设置批次创建者（从 HTTP 请求获取）
            batch.setCreatedBy(getCurrentUserFromRequest());

            // 设置批次属性（按照 Swagger 定义）
            batch.setSftpServerName(request.getSftpServerName());
            batch.setOperationInterval(request.getOperationInterval());
            batch.setScheduledTime(request.getScheduledTime());
            batch.setBatchActionTime(batchActionTime);  // 保存时间戳
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 🗄️ 第二阶段：创建数据库资源（Batch、BatchDevice、Workflow）
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            log.info("🗄️ 创建批次和工作流...");
            
            // ✅ 设置步骤启用配置
            batch.setEnableDownload(enableDownload);
            batch.setEnableBackup(enableBackup);
            batch.setEnableUpgrade(enableUpgrade);
            
            // ✅ 设置执行模式
            batch.setExecutionMode(Batch.ExecutionMode.valueOf(request.getExecutionMode() != null ? request.getExecutionMode() : "MANUAL"));
            
            // ✅ 设置调度模式
            batch.setScheduledMode(Batch.ScheduledMode.valueOf(request.getScheduledMode() != null ? request.getScheduledMode() : "IMMEDIATE"));
            
            // ✅ 设置升级文件路径（如果启用下载或升级）
            if (enableDownload || enableUpgrade) {
                if (request.getUpgradeFilePath() == null || request.getUpgradeFilePath().trim().isEmpty()) {
                    throw new IllegalArgumentException("upgradeFilePath cannot be empty when download or upgrade step is enabled");
                }
                batch.setFilePath(request.getUpgradeFilePath());
            }
            
            // ✅ 设置备份根路径（如果启用备份）
            if (enableBackup) {
                if (request.getBackupBasePath() == null || request.getBackupBasePath().trim().isEmpty()) {
                    throw new IllegalArgumentException("backupBasePath cannot be empty when backup step is enabled");
                }
                batch.setBackupBasePath(request.getBackupBasePath());
            }
            
            // targetVersion 只保留用户提供值；真实版本在下载完成后由通知回写
            String targetVersion = request.getTargetVersion();
            if (targetVersion != null && !targetVersion.trim().isEmpty()) {
                log.info("  ✓ 使用用户提供的目标版本: {}", targetVersion);
                batch.setTargetVersion(targetVersion.trim());
            } else {
                log.info("  ℹ️  未提供目标版本，等待下载完成后按设备上报版本回写");
                batch.setTargetVersion(null);
            }
            
            // 设置重试参数
            if (request.getMaxRetryCount() != null) {
                batch.setMaxRetryCount(request.getMaxRetryCount());
            }

            // 设置调试模式
            if (request.getDebug() != null) {
                batch.setDebug(request.getDebug());
            }
            
            // ✅ 获取调度模式和执行模式（提前获取，用于设置批次状态）
            String scheduledMode = request.getScheduledMode() != null ? request.getScheduledMode() : "IMMEDIATE";
            Batch.ExecutionMode executionMode = batch.getExecutionMode();  // ⭐ 从 batch 获取 executionMode
            
            // ✅ 设置批次初始状态（根据 executionMode 和 scheduledMode 的组合）
            if ("SCHEDULED".equals(scheduledMode)) {
                // ⭐ 定时模式：无论手动还是自动，都是 SCHEDULED
                batch.setStatus(Batch.BatchStatus.SCHEDULED);
                log.info("  ✓ 批次状态: SCHEDULED ({}模式，等待定时触发)", 
                    Batch.ExecutionMode.MANUAL.equals(executionMode) ? "手动" : "自动");
            } else if (Batch.ExecutionMode.MANUAL.equals(executionMode)) {
                // ⭐ 手动+立即: READY_TO_PROCEED（等待用户触发）
                batch.setStatus(Batch.BatchStatus.READY_TO_PROCEED);
                log.info("  ✓ 批次状态: READY_TO_PROCEED (手动立即，等待用户触发)");
            } else {
                // ⭐ 自动+立即: RUNNING（立即开始执行）
                batch.setStatus(Batch.BatchStatus.RUNNING);
                log.info("  ✓ 批次状态: RUNNING (自动立即，准备开始执行)");
            }

            batch = upgradeBatchRepository.save(batch);
            log.info("  ✓ Batch 已保存: batchId={}", batchId);
            log.info("  📊 保存后的 Batch 字段验证:");
            log.info("    - backupBasePath: {}", batch.getBackupBasePath());
            log.info("    - sftpServerName: {}", batch.getSftpServerName());
            log.info("    - debug: {}", batch.getDebug());
            log.info("    - filePath: {}", batch.getFilePath());
            log.info("    - enableDownload: {}", batch.getEnableDownload());
            log.info("    - enableBackup: {}", batch.getEnableBackup());
            log.info("    - enableUpgrade: {}", batch.getEnableUpgrade());
            log.info("    - scheduledMode: {}", batch.getScheduledMode());
            log.info("    - scheduledTime: {}", batch.getScheduledTime());

            // 创建批次设备关联
            List<BatchDevice> batchDevices = createBatchDevices(batchId, request.getDeviceIds());
            batchDeviceRepository.saveAll(batchDevices);
            log.info("  ✓ BatchDevice 已保存: {} 台设备", batchDevices.size());

            // ✅ 创建工作流（新架构）
            List<UpgradeWorkflow> workflows = workflowManagementService.createWorkflows(
                batch, 
                request.getDeviceIds()
            );
            log.info("  ✓ UpgradeWorkflow 已创建: {} 个工作流", workflows.size());

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 🚀 第三阶段：创建 DeviceTask 和执行策略
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            // ✅ 获取第一个启用的步骤（所有模式都需要）
            String firstStep = workflowManagementService.getNextEnabledStep(batch, null);
            if (firstStep == null) {
                throw new IllegalStateException("Batch has no enabled steps");
            }
            log.info("  第一步: {}", firstStep);
            
            if ("IMMEDIATE".equals(scheduledMode)) {
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // 🎯 IMMEDIATE 模式
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                if (Batch.ExecutionMode.MANUAL.equals(executionMode)) {
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // ⭐ MANUAL + IMMEDIATE: 立即创建 NOT_START 状态的 DeviceTask
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    log.info("📋 MANUAL + IMMEDIATE 模式");
                    log.info("  创建 NOT_START 状态的 DeviceTask，等待手动触发");
                    
                    // ✅ 为每个设备创建 NOT_START 状态的 DeviceTask
                    for (UpgradeWorkflow workflow : workflows) {
                        // 获取设备信息
                        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;
                        try {
                            deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(workflow.getDeviceId());
                        } catch (Exception e) {
                            log.warn("  ⚠️ 获取设备信息失败: deviceId={}, 继续创建任务", workflow.getDeviceId());
                        }
                        
                        DeviceTask task = createNotStartDeviceTask(
                            workflow.getDeviceId(),
                            workflow.getWorkflowId(),
                            batchId,
                            batch.getBatchName(),
                            firstStep,
                            batch,
                            deviceInfo
                        );
                        deviceTaskRepository.save(task);
                        log.debug("  ✓ 创建NOT_START任务: deviceId={}, taskType={}, status=NOT_START", 
                            workflow.getDeviceId(), firstStep);
                    }
                    
                    log.info("  ✅ 已创建 {} 个 NOT_START 状态的 DeviceTask", workflows.size());
                    log.info("  批次状态: READY_TO_PROCEED (等待手动触发)");
                    
                    // 📤 发送 TaskInfo 通知（批次已创建，待手动触发）
                    log.info("📤 通知 TaskInfo 创建批次升级任务");
                    taskInfoNotificationService.notifyBatchUpgradeCreated(
                        batchId,
                        request.getBatchName(),
                        batchActionTime,
                        request.getDeviceIds().size(),
                        request.getUpgradeFilePath(),
                        batch.getCreatedBy()  // ⭐ 传入批次创建者，确保一致性
                    );
                    log.info("  ✅ TaskInfo 通知已发送");
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        } else {
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // ⭐ AUTO + IMMEDIATE: 立即执行（创建 DeviceTask 并执行 RPC）
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    log.info("🚀 AUTO + IMMEDIATE 模式，立即执行第一步");
                    startBatch(batchId);
                    log.info("  ✅ 批次已启动执行");
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            } else if ("SCHEDULED".equals(scheduledMode)) {
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // 🎯 SCHEDULED 模式
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("📅 SCHEDULED 模式，定时执行时间: {}", request.getScheduledTime());
                
                if (Batch.ExecutionMode.MANUAL.equals(executionMode)) {
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // ⭐ MANUAL + SCHEDULED: 创建 NOT_START 状态的 DeviceTask
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    log.info("  执行模式: MANUAL");
                    log.info("  创建 NOT_START 状态的 DeviceTask，等待手动触发");
                    
                    // ✅ 为每个设备创建 NOT_START 状态的 DeviceTask
                    for (UpgradeWorkflow workflow : workflows) {
                        // 获取设备信息
                        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;
                        try {
                            deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(workflow.getDeviceId());
                    } catch (Exception e) {
                            log.warn("  ⚠️ 获取设备信息失败: deviceId={}, 继续创建任务", workflow.getDeviceId());
                        }
                        
                        DeviceTask task = createNotStartDeviceTask(
                            workflow.getDeviceId(),
                            workflow.getWorkflowId(),
                            batchId,
                            batch.getBatchName(),
                            firstStep,
                            batch,
                            deviceInfo
                        );
                        deviceTaskRepository.save(task);
                        log.debug("  ✓ 创建NOT_START任务: deviceId={}, taskType={}, status=NOT_START", 
                            workflow.getDeviceId(), firstStep);
                    }
                    
                    log.info("  ✅ 已创建 {} 个 NOT_START 状态的 DeviceTask", workflows.size());
                    log.info("  批次状态: READY_TO_PROCEED (等待手动触发)");
                } else {
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // ⭐ AUTO + SCHEDULED: 创建 SCHEDULED 状态的 DeviceTask
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    log.info("  执行模式: AUTO");
                    log.info("  创建 SCHEDULED 状态的 DeviceTask，等待定时触发");
                    
                    // ✅ 为每个设备创建 SCHEDULED 状态的 DeviceTask
                    for (UpgradeWorkflow workflow : workflows) {
                        // 获取设备信息
                        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;
                        try {
                            deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(workflow.getDeviceId());
                        } catch (Exception e) {
                            log.warn("  ⚠️ 获取设备信息失败: deviceId={}, 继续创建任务", workflow.getDeviceId());
                        }
                        
                        DeviceTask task = createScheduledDeviceTask(
                            workflow.getDeviceId(),
                            workflow.getWorkflowId(),
                            batchId,
                            batch.getBatchName(),
                            firstStep,
                            request.getScheduledTime(),
                            batch,
                            deviceInfo
                        );
                        deviceTaskRepository.save(task);
                        log.debug("  ✓ 创建SCHEDULED任务: deviceId={}, taskType={}, status=SCHEDULED", 
                            workflow.getDeviceId(), firstStep);
                    }
                    
                    log.info("  ✅ 已创建 {} 个 SCHEDULED 状态的 DeviceTask", workflows.size());
                    log.info("  批次状态: SCHEDULED (等待定时触发)");
                }
                
                // 📤 SCHEDULED 模式也需要通知 TaskInfo（创建时就通知）
                log.info("📤 通知 TaskInfo 创建批次升级任务");
                taskInfoNotificationService.notifyBatchUpgradeCreated(
                    batchId,
                    request.getBatchName(),
                    batchActionTime,
                    request.getDeviceIds().size(),
                    request.getUpgradeFilePath(),
                    batch.getCreatedBy()  // ⭐ 传入批次创建者，确保一致性
                );
                log.info("  ✅ TaskInfo 通知已发送");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }

            // 返回响应
            BatchUpgradeDto.BatchUpgradeResponse response = convertToResponse(batch);
            response.setSuccess(true);
            response.setMessage("批量升级任务创建成功");

            log.info("批量升级任务创建成功: batchId={}, batchName={}", batchId, request.getBatchName());
            return response;

        } catch (Exception e) {
            log.error("Failed to create batch upgrade task: batchName={}", request.getBatchName(), e);
            // 改为重新抛出异常，由全局异常处理器统一处理
            throw new RuntimeException("Failed to create batch upgrade task: " + e.getMessage(), e);
        }
    }

    /**
     * 启动批次执行（执行第一步）
     * 用于 IMMEDIATE 模式立即启动，或 SCHEDULED 模式定时触发
     * 
     * ⚠️ 注意：不能加 @Transactional！
     * 因为内部会调用 executeStepForWorkflows -> CompletableFuture.join()
     * 如果在事务中等待，会与子线程的 REQUIRES_NEW 事务产生死锁
     */
    public void startBatch(String batchId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 启动批次执行: batchId={}", batchId);

        Batch batch = upgradeBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));

        // ⭐ 如果批次是 SCHEDULED 状态，说明定时器已触发，需要更新状态
        if (batch.getStatus() == Batch.BatchStatus.SCHEDULED) {
            log.info("  定时批次触发，从 SCHEDULED 状态开始执行");
            // 根据执行模式设置初始状态
            if (batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC) {
                // 自动模式：SCHEDULED -> RUNNING
                batch.updateBatchStatus(Batch.BatchStatus.RUNNING);
                log.info("  状态更新: SCHEDULED -> RUNNING (自动模式)");
            } else {
                // 手动模式：SCHEDULED -> READY_TO_PROCEED
                batch.updateBatchStatus(Batch.BatchStatus.READY_TO_PROCEED);
                log.info("  状态更新: SCHEDULED -> READY_TO_PROCEED (手动模式)");
            }
            upgradeBatchRepository.save(batch);
        }

        List<UpgradeWorkflow> workflows = workflowManagementService.getWorkflowsByBatchId(batchId);
        if (workflows.isEmpty()) {
            throw new IllegalStateException("Batch has no workflows");
        }

        // 获取第一个启用的步骤
        String firstStep = workflowManagementService.getNextEnabledStep(batch, null);
        if (firstStep == null) {
            throw new IllegalStateException("Batch has no enabled steps");
        }

        log.info("  第一步: {}", firstStep);

        // 执行第一步（会等待所有RPC调用完成）
        // ⚠️ 这里没有事务，所以 CompletableFuture.join() 不会死锁
        executeStepForWorkflows(batch, workflows, firstStep);

        // ✅ 重新计算批次状态（基于最新的workflow和deviceTask状态）
        // 因为 executeStepForWorkflows 现在会等待所有任务完成
        workflowManagementService.computeAndSetWorkflowStatuses(workflows);
        Batch.BatchStatus newStatus = calculateBatchStatus(batch, workflows);
        
        // ✅ 在独立事务中更新批次状态
        updateBatchStatusAfterExecution(batchId, newStatus);

        // 通知 TaskInfo
        notifyBatchUpgradeStatus(batch);

        log.info("✅ 批次已完成第一步: step={}, status={}", firstStep, newStatus);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    /**
     * 在独立事务中更新批次状态
     * 避免与执行任务的事务冲突
     */
    @Transactional
    private void updateBatchStatusAfterExecution(String batchId, Batch.BatchStatus newStatus) {
        Batch batch = upgradeBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));
        
        batch.updateBatchStatus(newStatus);
        if (batch.getStartedTime() == null) {
            batch.setStartedTime(LocalDateTime.now());
        }
        
        // 如果所有任务都失败或完成，设置完成时间
        if (newStatus == Batch.BatchStatus.FAILED || 
            newStatus == Batch.BatchStatus.COMPLETED) {
            batch.setCompletedTime(LocalDateTime.now());
            log.info("  批次已结束: status={}", newStatus);
            }

            upgradeBatchRepository.save(batch);
    }

    /**
     * 根据步骤名称获取对应的任务类型
     * 用于状态统计等辅助功能
     */
    private DeviceTask.TaskType getTaskTypeByStep(String step) {
        switch (step.toUpperCase()) {
            case "DOWNLOAD":
                return DeviceTask.TaskType.DOWNLOAD;
            case "BACKUP":
                return DeviceTask.TaskType.BACKUP;
            case "UPGRADE":
                return DeviceTask.TaskType.UPGRADE;
            case "COMMIT":
                return DeviceTask.TaskType.COMMIT;
            default:
                throw new IllegalArgumentException("Unknown step type: " + step);
        }
    }

    /**
     * 检查步骤在批次中是否启用
     */
    private boolean isStepEnabledInBatch(Batch batch, String step) {
        switch (step.toUpperCase()) {
            case "DOWNLOAD":
                return batch.getEnableDownload();
            case "BACKUP":
                return batch.getEnableBackup();
            case "UPGRADE":
                return batch.getEnableUpgrade();
            case "COMMIT":
                return true;  // ⭐ COMMIT 始终启用（手动模式专用）
            default:
                return false;
        }
    }

    /**
     * 创建批次设备关联
     */
    private List<BatchDevice> createBatchDevices(String batchId, List<String> deviceIds) {
        List<BatchDevice> batchDevices = new ArrayList<>();
        
        for (String deviceId : deviceIds) {
            // 获取设备信息（使用Mock数据）
            String deviceName = deviceId;
            String vendorType = "Unknown";
            try {
                NeMgrIntegrationService.DevicePhysicalInfo deviceInfo =
                        neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                if (deviceInfo != null) {
                    if (deviceInfo.getFriendlyName() != null && !deviceInfo.getFriendlyName().trim().isEmpty()) {
                        deviceName = deviceInfo.getFriendlyName();
                    }
                    if (deviceInfo.getVendorType() != null && !deviceInfo.getVendorType().trim().isEmpty()) {
                        vendorType = deviceInfo.getVendorType();
                    }
                }
            } catch (Exception e) {
                log.warn("创建 BatchDevice 时获取设备名称失败: deviceId={}, error={}", deviceId, e.getMessage());
            }
            
            BatchDevice batchDevice = new BatchDevice(batchId, deviceId, deviceName);
            batchDevice.setDeviceType("Router");
            batchDevice.setVendorType(vendorType);
            
            batchDevices.add(batchDevice);
        }
        
        return batchDevices;
    }

    /**
     * 计算步骤状态统计
     */
    private Map<String, BatchUpgradeDto.StepStatus> calculateStepStatuses(
            Batch batch, List<BatchDevice> devices) {
        
        Map<String, BatchUpgradeDto.StepStatus> stepStatuses = new HashMap<>();

        stepStatuses.put("DOWNLOAD", buildStepStatus("DOWNLOAD", batch.getStatus(), devices));
        stepStatuses.put("BACKUP", buildStepStatus("BACKUP", batch.getStatus(), devices));
        stepStatuses.put("UPGRADE", buildStepStatus("UPGRADE", batch.getStatus(), devices));

        return stepStatuses;
    }

    private BatchUpgradeDto.StepStatus buildStepStatus(String stepName, Batch.BatchStatus batchStatus, List<BatchDevice> devices) {
        BatchUpgradeDto.StepStatus stepStatus = new BatchUpgradeDto.StepStatus();
        stepStatus.setStepName(stepName);
        stepStatus.setStatus(batchStatus != null ? batchStatus.name() : null);

        int successCount = 0;
        int failedCount = 0;
        int pendingCount = devices.size();
        int runningCount = 0;

        List<DeviceTask> stepTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(
                devices.isEmpty() ? null : devices.get(0).getBatchId());

        for (DeviceTask task : getLatestTasksByType(stepTasks, getTaskTypeByStep(stepName))) {
            pendingCount--;
            switch (task.getStatus()) {
                case COMPLETED:
                    successCount++;
                    break;
                case FAILED:
                    failedCount++;
                    break;
                case RUNNING:
                    runningCount++;
                    break;
                default:
                    break;
            }
        }

        stepStatus.setSuccessCount(successCount);
        stepStatus.setFailedCount(failedCount);
        stepStatus.setPendingCount(Math.max(pendingCount, 0));
        stepStatus.setRunningCount(runningCount);
        return stepStatus;
    }

    /**
     * 计算单个步骤的统计数据
     * 通过DeviceTask表查询批次相关任务的状态统计
     */
    private void calculateStepCounts(BatchUpgradeDto.StepStatus stepStatus, 
                                   List<BatchDevice> devices, String step) {
        int successCount = 0;
        int failedCount = 0;
        int pendingCount = 0;
        int runningCount = 0;

        // 通过DeviceTask表查询该批次该步骤的所有任务
        DeviceTask.TaskType taskType = getTaskTypeByStep(step);
        List<DeviceTask> stepTasks = new ArrayList<>();
        
        if (!devices.isEmpty()) {
            try {
                List<DeviceTask> allBatchTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(devices.get(0).getBatchId());
                stepTasks = getLatestTasksByType(allBatchTasks, taskType);
            } catch (Exception e) {
                log.error("查询批次任务失败: batchId={}, step={}, 错误: {}", devices.get(0).getBatchId(), step, e.getMessage());
                // 查询失败时使用空列表
            }
        }

        // 如果任务数量少于设备数量，说明还有任务未创建
        if (stepTasks.size() < devices.size()) {
            pendingCount = devices.size() - stepTasks.size();
        }

        // 统计已创建任务的状态
        for (DeviceTask task : stepTasks) {
            switch (task.getStatus()) {
                case COMPLETED:
                    successCount++;
                    break;
                case FAILED:
                    failedCount++;
                    break;
                case RUNNING:
                    runningCount++;
                    break;
                case PENDING:
                default:
                    pendingCount++;
                    break;
            }
        }

        stepStatus.setSuccessCount(successCount);
        stepStatus.setFailedCount(failedCount);
        stepStatus.setPendingCount(pendingCount);
        stepStatus.setRunningCount(runningCount);
    }

    /**
     * 构建设备任务详情
     * 通过DeviceTask表查询每个设备的任务状态
     */
    private List<BatchUpgradeDto.DeviceTaskDetail> buildDeviceTaskDetails(List<BatchDevice> devices) {
        if (devices.isEmpty()) {
            return new ArrayList<>();
        }
        
        String batchId = devices.get(0).getBatchId();
        List<DeviceTask> allBatchTasks = new ArrayList<>();
        
        try {
            allBatchTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
        } catch (Exception e) {
            log.error("查询批次任务详情失败: batchId={}, 错误: {}", batchId, e.getMessage());
            // 查询失败时使用空列表，返回基本的设备信息
        }

        final List<DeviceTask> finalBatchTasks = allBatchTasks;
        return devices.stream().map(device -> {
            BatchUpgradeDto.DeviceTaskDetail detail = new BatchUpgradeDto.DeviceTaskDetail();
            detail.setDeviceId(device.getDeviceId());
            detail.setDeviceName(device.getDeviceName());
            detail.setErrorMessage(finalBatchTasks.stream()
                .filter(t -> t.getDeviceId().equals(device.getDeviceId()))
                .map(DeviceTask::getErrorMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null));
            // 旧方法已废弃，不再设置currentStep和lastUpdated
            return detail;
        }).collect(Collectors.toList());
    }

    private String evaluateCurrentStep(List<DeviceTask> deviceTasks) {
        return deviceTasks.stream()
                .max(Comparator.comparing(DeviceTask::getUpdatedTime, Comparator.nullsFirst(LocalDateTime::compareTo)))
                .map(task -> task.getTaskType() != null ? task.getTaskType().name() : null)
                .orElse("UNKNOWN");
    }

    /**
     * 从实际的DeviceTask列表构建任务详情（新版，返回具体任务信息）
     */
    private List<BatchUpgradeDto.DeviceTaskDetail> buildDeviceTaskDetailsFromTasks(List<DeviceTask> deviceTasks, boolean debug) {
        return deviceTasks.stream().map(task -> {
            BatchUpgradeDto.DeviceTaskDetail detail = new BatchUpgradeDto.DeviceTaskDetail();
            detail.setTaskId(task.getTaskId());
            detail.setDeviceId(task.getDeviceId());
            detail.setDeviceName(task.getDeviceName() != null ? task.getDeviceName() : task.getDeviceId()); // ✅ 使用友好名称
            detail.setTaskType(task.getTaskType().name());
            detail.setStartTime(task.getStartedTime());
            detail.setStatus(task.getStatus().name());
            detail.setErrorMessage(task.getErrorMessage());
            
            // ✅ debug模式下包含RPC payload（从数据库读取）
            if (debug && task.getDebugPayload() != null) {
                try {
                    // 解析JSON字符串为Map对象
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Object debugPayloadObj = objectMapper.readValue(task.getDebugPayload(), Object.class);
                    detail.setDebugPayload(debugPayloadObj);
                    log.debug("  ✓ Debug payload loaded for task: {}", task.getTaskId());
                } catch (Exception e) {
                    log.warn("  Failed to parse debug payload for task {}: {}", task.getTaskId(), e.getMessage());
                    detail.setDebugPayload(null);
                }
            }
            
            return detail;
        }).collect(Collectors.toList());
    }

    /**
     * 根据设备ID和任务类型获取任务状态
     */
    private String getTaskStatusByDeviceAndType(List<DeviceTask> allTasks, String deviceId, DeviceTask.TaskType taskType) {
        Optional<DeviceTask> taskOpt = selectLatestTask(allTasks.stream()
            .filter(task -> task.getDeviceId().equals(deviceId) && task.getTaskType() == taskType)
            .collect(Collectors.toList()));

        return taskOpt.map(task -> task.getStatus().name()).orElse("NOT_STARTED");
    }

    private String requireConsistentCurrentStep(List<UpgradeWorkflow> workflows, String batchId) {
        String currentStep = resolveCurrentStep(workflows);
        if (currentStep == null) {
            throw new IllegalStateException("No active workflow currentStep found for batch: " + batchId);
        }
        return currentStep;
    }

    private String resolveCurrentStep(List<UpgradeWorkflow> workflows) {
        return workflowManagementService.resolveCurrentStep(workflows);
    }

    private List<DeviceTask> getLatestTasksByType(List<DeviceTask> tasks, DeviceTask.TaskType taskType) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<DeviceTask>> tasksByDevice = tasks.stream()
            .filter(task -> task.getTaskType() == taskType)
            .filter(task -> task.getDeviceId() != null)
            .collect(Collectors.groupingBy(DeviceTask::getDeviceId));

        return tasksByDevice.values().stream()
            .map(this::selectLatestTask)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Optional<DeviceTask> selectLatestTask(List<DeviceTask> tasks) {
        return tasks.stream()
            .max(Comparator
                .comparing(DeviceTask::getUpdatedTime, Comparator.nullsFirst(LocalDateTime::compareTo))
                .thenComparing(DeviceTask::getCreatedTime, Comparator.nullsFirst(LocalDateTime::compareTo)));
    }

    private Optional<DeviceTask> selectRetryableFailedTask(List<DeviceTask> tasks) {
        List<DeviceTask> failedTasks = tasks.stream()
            .filter(task -> task.getStatus() == DeviceTask.TaskStatus.FAILED)
            .collect(Collectors.toList());

        Optional<DeviceTask> retryableTask = selectLatestTask(failedTasks.stream()
            .filter(this::hasRetryExecutionContext)
            .collect(Collectors.toList()));

        return retryableTask.isPresent() ? retryableTask : selectLatestTask(failedTasks);
    }

    private boolean hasRetryExecutionContext(DeviceTask task) {
        if (task == null || task.getTaskType() == null) {
            return false;
        }

        switch (task.getTaskType()) {
            case DOWNLOAD:
                return StringUtils.hasText(task.getSftpServerName())
                    && StringUtils.hasText(task.getFilePath());
            case BACKUP:
                return StringUtils.hasText(task.getSftpServerName())
                    && StringUtils.hasText(task.getBackupFilePath());
            case UPGRADE:
            case COMMIT:
                return StringUtils.hasText(task.getFilePath());
            default:
                return false;
        }
    }

    private void rehydrateRetryTaskContext(DeviceTask task, Batch batch) {
        if (task == null || batch == null || task.getTaskType() == null) {
            return;
        }

        switch (task.getTaskType()) {
            case DOWNLOAD:
                if (!StringUtils.hasText(task.getSftpServerName())) {
                    task.setSftpServerName(batch.getSftpServerName());
                }
                if (!StringUtils.hasText(task.getFilePath())) {
                    task.setFilePath(batch.getFilePath());
                }
                break;
            case BACKUP:
                if (!StringUtils.hasText(task.getSftpServerName())) {
                    task.setSftpServerName(batch.getSftpServerName());
                }
                if (!StringUtils.hasText(task.getBackupFilePath())) {
                    task.setBackupFilePath(batch.getBackupBasePath());
                }
                break;
            case UPGRADE:
                if (!StringUtils.hasText(task.getFilePath())) {
                    task.setFilePath(batch.getFilePath());
                }
                if (!StringUtils.hasText(task.getTargetVersion())) {
                    task.setTargetVersion(batch.getTargetVersion());
                }
                break;
            case COMMIT:
                if (!StringUtils.hasText(task.getSftpServerName())) {
                    task.setSftpServerName(batch.getSftpServerName());
                }
                if (!StringUtils.hasText(task.getFilePath())) {
                    task.setFilePath(batch.getFilePath());
                }
                break;
            default:
                break;
        }
    }

    /**
     * 计算批次进度
     */
    private Integer calculateBatchProgress(Batch batch, 
                                         Map<String, BatchUpgradeDto.StepStatus> stepStatuses) {
        return 0;
    }

    /**
     * 通知 TaskInfo 批次升级状态
     * 
     * <p>访问级别：包内可见
     * <p>用途：供 RealServiceExecutionHelper 在事务提交后调用
     */
    void notifyBatchUpgradeStatus(Batch batch) {
        try {
            if (batch.getBatchActionTime() == null) {
                log.warn("批次 actionTime 为空，跳过 TaskInfo 通知: batchId={}", batch.getBatchId());
                return;
            }

            // ⭐ 使用完整的 batch detail 通知，而不是简化版
            // 这样 TaskInfo 会收到包含所有 deviceTasks 和 workflows 的完整信息
            String detailJson = unifiedBatchService.convertBatchToJsonForNotification(batch.getBatchId());

            // ✅ taskType: UPGRADE保持BATCH_UPGRADE，BACKUP/RESTORE使用简单格式
            String taskType;
            if (batch.getBatchType() == devicemaintenance.entity.Batch.BatchType.UPGRADE) {
                taskType = "BATCH_UPGRADE";
            } else {
                taskType = batch.getBatchType().name();  // BACKUP 或 RESTORE
            }
            
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
            
            log.info("  ✅ TaskInfo 通知已发送（完整 detail）");
        } catch (Exception e) {
            log.error("通知 TaskInfo 批次状态失败: batchId={}", batch.getBatchId(), e);
        }
    }

    /**
     * 转换为响应DTO
     */
    private BatchUpgradeDto.BatchUpgradeResponse convertToResponse(Batch batch) {
        BatchUpgradeDto.BatchUpgradeResponse response = new BatchUpgradeDto.BatchUpgradeResponse();
        response.setBatchId(batch.getBatchId());
        response.setBatchName(batch.getBatchName());
        response.setUpgradeFilePath(batch.getFilePath());
        response.setSftpServerName(batch.getSftpServerName());
        response.setOperationInterval(batch.getOperationInterval());
        response.setDeviceCount(batch.getDeviceCount());
        
        // 执行模式配置
        response.setExecutionMode(batch.getExecutionMode() != null ? batch.getExecutionMode().name() : null);
        response.setScheduledMode(batch.getScheduledMode() != null ? batch.getScheduledMode().name() : null);
        response.setScheduledTime(batch.getScheduledTime());
        
        // 工作流配置
        response.setEnableDownload(batch.getEnableDownload());
        response.setEnableBackup(batch.getEnableBackup());
        response.setEnableUpgrade(batch.getEnableUpgrade());
        
        // 重试配置
        response.setMaxRetryCount(batch.getMaxRetryCount());
        
        // 调试配置
        response.setDebug(batch.getDebug());
        
        // 当前步骤 - 使用与批次状态计算一致的解析逻辑，避免重试后被旧 workflow 顺序污染
        List<UpgradeWorkflow> workflows = workflowManagementService.getWorkflowsByBatchId(batch.getBatchId());
        workflowManagementService.computeAndSetWorkflowStatuses(workflows);
        String currentStep;
        try {
            currentStep = workflowManagementService.resolveCurrentStep(workflows);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("inconsistent currentStep values")) {
                log.warn("Batch detail currentStep fallback due to inconsistent workflow steps: batchId={}, message={}",
                    batch.getBatchId(), ex.getMessage());
                currentStep = workflows.stream()
                    .map(UpgradeWorkflow::getCurrentStep)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            } else {
                throw ex;
            }
        }
        response.setCurrentStep(currentStep);
        
        // 状态信息
        response.setStatus(batch.getStatus().name());
        // ⚠️ 已删除步骤状态字段
        
        // 统计信息
        response.setSuccessCount(batch.getSuccessCount());
        response.setFailedCount(batch.getFailedCount());
        
        // 时间信息
        response.setCreatedTime(batch.getCreatedTime());
        response.setUpdatedTime(batch.getUpdatedTime());
        response.setStartedTime(batch.getStartedTime());
        response.setCompletedTime(batch.getCompletedTime());
        
        return response;
    }

    /**
     * 获取所有批次列表
     */
    /**
     * 获取所有批次（分页，支持所有类型）
     * 
     * @param request 查询请求（包含分页参数和过滤条件）
     * @return 分页结果
     */
    public PagedBatchResult getAllBatches(devicemaintenance.dto.GetAllBatchesRequest request) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 获取所有批次: pageNum={}, pageSize={}, batchType={}, status={}", 
            request.getPageNum(), request.getPageSize(), 
            request.getBatchType(), request.getStatus());
        
        // 1. 构建查询条件
        List<Batch> allBatches;
        
        if (request.getBatchType() != null && !request.getBatchType().isEmpty() 
            && request.getStatus() != null && !request.getStatus().isEmpty()) {
            // 按类型和状态查询
            Batch.BatchType batchType = Batch.BatchType.valueOf(request.getBatchType().toUpperCase());
            Batch.BatchStatus status = Batch.BatchStatus.valueOf(request.getStatus().toUpperCase());
            allBatches = upgradeBatchRepository.findByBatchTypeAndStatus(batchType, status);
            log.info("  查询条件: batchType={}, status={}", batchType, status);
            
        } else if (request.getBatchType() != null && !request.getBatchType().isEmpty()) {
            // 按类型查询
            Batch.BatchType batchType = Batch.BatchType.valueOf(request.getBatchType().toUpperCase());
            allBatches = upgradeBatchRepository.findByBatchTypeOrderByCreatedTimeDesc(batchType);
            log.info("  查询条件: batchType={}", batchType);
            
        } else if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            // 按状态查询
            Batch.BatchStatus status = Batch.BatchStatus.valueOf(request.getStatus().toUpperCase());
            allBatches = upgradeBatchRepository.findByStatusOrderByCreatedTimeDesc(status);
            log.info("  查询条件: status={}", status);
            
        } else {
            // 查询所有
            allBatches = upgradeBatchRepository.findAllByOrderByCreatedTimeDesc();
            log.info("  查询条件: 所有批次");
        }
        
        int totalCount = allBatches.size();
        log.info("  查询到 {} 个批次", totalCount);
        
        // 2. 分页
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCount);
        
        List<Batch> pagedBatches;
        if (startIndex >= totalCount) {
            pagedBatches = new ArrayList<>();
        } else {
            pagedBatches = allBatches.subList(startIndex, endIndex);
        }
        
        log.info("  分页: 第{}/{}页，每页{}条，返回{}条", 
            pageNum, (totalCount + pageSize - 1) / pageSize, pageSize, pagedBatches.size());
        
        // 3. 转换为DTO
        List<devicemaintenance.dto.BatchSummaryDto> summaries = pagedBatches.stream()
            .map(this::convertToBatchSummary)
                .collect(Collectors.toList());
        
        log.info("✅ 获取批次列表成功");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return new PagedBatchResult(summaries, totalCount, pageNum, pageSize);
    }
    
    /**
     * 转换Batch实体为BatchSummaryDto
     */
    private devicemaintenance.dto.BatchSummaryDto convertToBatchSummary(Batch batch) {
        devicemaintenance.dto.BatchSummaryDto summary = new devicemaintenance.dto.BatchSummaryDto();
        
        // 基本信息
        summary.setBatchId(batch.getBatchId());
        summary.setBatchName(batch.getBatchName());
        summary.setBatchType(batch.getBatchType().name());
        Batch.BatchStatus realtimeStatus = batch.getStatus();
        // executionMode 仅 UPGRADE 批次有值，其他类型为 null
        summary.setExecutionMode(batch.getExecutionMode() != null ? batch.getExecutionMode().name() : null);
        summary.setDescription(null); // Batch实体没有description字段
        
        // 时间信息
        summary.setCreatedTime(batch.getCreatedTime());
        summary.setStartedTime(batch.getStartedTime());
        summary.setCompletedTime(batch.getCompletedTime());
        
        // 统计信息
        if (batch.getBatchType() == Batch.BatchType.UPGRADE) {
            // UPGRADE类型：统计workflow
            List<UpgradeWorkflow> workflows = workflowRepository.findByBatchId(batch.getBatchId());
            workflowManagementService.computeAndSetWorkflowStatuses(workflows);
            realtimeStatus = calculateBatchStatusPublic(batch, workflows);
            summary.setTotalCount(workflows.size());
            
            int completedCount = (int) workflows.stream().filter(w -> w.isCompleted()).count();
            int failedCount = (int) workflows.stream().filter(w -> w.isFailed()).count();
            
            summary.setSuccessCount(completedCount);
            summary.setFailedCount(failedCount);
            // ⭐ 修复：runningCount 应该是"既未完成也未失败"的数量
            summary.setRunningCount(workflows.size() - completedCount - failedCount);
        } else {
            // 其他类型：统计DeviceTask
            List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batch.getBatchId());
            summary.setTotalCount(tasks.size());
            summary.setSuccessCount((int) tasks.stream()
                .filter(t -> t.getStatus() == DeviceTask.TaskStatus.COMPLETED)
                .count());
            summary.setFailedCount((int) tasks.stream()
                .filter(t -> t.getStatus() == DeviceTask.TaskStatus.FAILED)
                .count());
            summary.setRunningCount((int) tasks.stream()
                .filter(t -> t.getStatus() == DeviceTask.TaskStatus.RUNNING 
                    || t.getStatus() == DeviceTask.TaskStatus.PENDING
                    || t.getStatus() == DeviceTask.TaskStatus.SCHEDULED)
                .count());
        }
        
        summary.setStatus(realtimeStatus.name());

        return summary;
    }
    
    /**
     * 分页结果包装类
     */
    public static class PagedBatchResult {
        private List<devicemaintenance.dto.BatchSummaryDto> data;
        private int total;
        private int pageNum;
        private int pageSize;
        private int totalPages;
        
        public PagedBatchResult(List<devicemaintenance.dto.BatchSummaryDto> data, int total, int pageNum, int pageSize) {
            this.data = data;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.totalPages = (total + pageSize - 1) / pageSize;
        }
        
        public List<devicemaintenance.dto.BatchSummaryDto> getData() {
            return data;
        }
        
        public int getTotal() {
            return total;
        }
        
        public int getPageNum() {
            return pageNum;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
    }

    /**
     * 取消批次任务
     * 
     * 功能：
     * 1. 取消批次本身（状态变为CANCELLED）
     * 2. 级联取消批次下所有 RUNNING/SCHEDULED 状态的任务
     * 3. 发送TaskInfo通知（如果任务已通知）
     */
    @Transactional
    public void cancelBatch(String batchId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚫 取消批次: batchId={}", batchId);
        
        // 1. 查找批次
        Optional<Batch> batchOpt = upgradeBatchRepository.findById(batchId);
        if (!batchOpt.isPresent()) {
            throw new IllegalArgumentException("Batch does not exist: " + batchId);
        }
        
            Batch batch = batchOpt.get();
            
        // 2. 检查批次状态
        if (batch.getStatus() == Batch.BatchStatus.CANCELLED) {
            log.warn("⚠️ 批次已经被取消，无需重复操作");
            return;
        }
        
        if (batch.getStatus() == Batch.BatchStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed batch");
        }
        
        log.info("  批次类型: {}", batch.getBatchType());
        log.info("  当前批次状态: {}", batch.getStatus());
        
        // 3. 取消所有 Workflow (仅适用于 UPGRADE 类型)
        int cancelledWorkflowCount = 0;
        if (batch.getBatchType() == Batch.BatchType.UPGRADE) {
        List<UpgradeWorkflow> workflows = workflowManagementService.getWorkflowsByBatchId(batchId);
        log.info("  批次下共有 {} 个工作流", workflows.size());
        
        // ✅ 先计算所有workflow的状态（设置到 @Transient 字段）
        workflowManagementService.computeAndSetWorkflowStatuses(workflows);
        
        for (UpgradeWorkflow workflow : workflows) {
            // 只取消未完成的 workflow (不取消已完成或已失败的)
            if (!workflow.isFinished()) {
                
                log.info("  取消工作流: workflowId={}, deviceId={}, status={}", 
                    workflow.getWorkflowId(), workflow.getDeviceId(), workflow.getStatus());
                
                // ⚠️ Workflow 的 status 是计算字段，不需要设置
                // 状态会根据关联的 DeviceTask 自动计算
                // 只需确保 currentStep 被清空即可
                workflow.setCurrentStep(null);
                
                cancelledWorkflowCount++;
            }
        }
        
        // 批量保存所有被取消的 workflow
        if (cancelledWorkflowCount > 0) {
            workflowRepository.saveAll(workflows);
            log.info("  ✅ 已批量保存 {} 个被取消的工作流", cancelledWorkflowCount);
        }
        
        log.info("  已取消 {} 个工作流", cancelledWorkflowCount);
        } else {
            log.info("  非升级批次，跳过工作流处理");
        }
        
        // 4. 取消所有未完成的 DeviceTask
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
        log.info("  批次下共有 {} 个设备任务", tasks.size());
        
        int cancelledTaskCount = 0;
        List<DeviceTask> tasksToUpdate = new ArrayList<>();
        for (DeviceTask task : tasks) {
            if (task.getStatus() == DeviceTask.TaskStatus.RUNNING ||
                task.getStatus() == DeviceTask.TaskStatus.SCHEDULED ||
                task.getStatus() == DeviceTask.TaskStatus.PENDING ||
                task.getStatus() == DeviceTask.TaskStatus.NOT_START) {

                log.info("  取消任务: taskId={}, type={}, status={}",
                    task.getTaskId(), task.getTaskType(), task.getStatus());

                task.setStatus(DeviceTask.TaskStatus.CANCELLED);
                task.setCompletedTime(LocalDateTime.now());
                task.setUpdatedTime(LocalDateTime.now());
                task.setErrorMessage("Batch cancelled");
                tasksToUpdate.add(task);

                cancelledTaskCount++;
            }
        }
        
        // 批量保存所有被取消的 DeviceTask
        if (!tasksToUpdate.isEmpty()) {
            deviceTaskRepository.saveAll(tasksToUpdate);
            log.info("  ✅ 已批量保存 {} 个被取消的设备任务", cancelledTaskCount);
        }
        
        // 5. 更新批次状态为CANCELLED
        batch.updateBatchStatus(Batch.BatchStatus.CANCELLED);
        batch.setCompletedTime(LocalDateTime.now());
        batch.setErrorMessage("Batch cancelled by user");
        upgradeBatchRepository.save(batch);
        
        // 6. 📤 通知 TaskInfo 批次取消
        try {
            notifyBatchUpgradeStatus(batch);
            log.info("  ✓ 已通知 TaskInfo 批次取消");
        } catch (Exception e) {
            log.warn("  通知 TaskInfo 失败: {}", e.getMessage());
        }
        
        log.info("✅ 批次取消成功");
        log.info("  批次ID: {}", batchId);
        log.info("  取消工作流数: {}", cancelledWorkflowCount);
        log.info("  取消设备任务数: {}", cancelledTaskCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // ⚠️ 注意：取消操作不需要通知 TaskInfo
        log.info("✅ 批次取消完成: batchId={}", batchId);
    }

    /**
     * 删除批次
     * 
     * 规则：
     * - RUNNING 状态的批次无法直接删除，需要先取消
     * - 其他状态（PENDING/COMPLETED/FAILED/CANCELLED）可以删除
     */
    @Transactional
    public void deleteBatch(String batchId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🗑️ 删除批次: batchId={}", batchId);
        
        // 1. 查找批次
        Batch batch = upgradeBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));
        
        // 2. 检查批次状态
            if (batch.getStatus() == Batch.BatchStatus.RUNNING) {
            throw new IllegalStateException("Cannot delete running batch, please cancel it first");
        }
        
        log.info("  批次类型: {}", batch.getBatchType());
        log.info("  批次状态: {}", batch.getStatus());
        
        // 3. 删除所有 Workflow (仅适用于 UPGRADE 类型)
        int deletedWorkflowCount = 0;
        if (batch.getBatchType() == Batch.BatchType.UPGRADE) {
        List<UpgradeWorkflow> workflows = workflowManagementService.getWorkflowsByBatchId(batchId);
        if (!workflows.isEmpty()) {
                deletedWorkflowCount = workflows.size();
            workflowRepository.deleteAll(workflows);
                log.info("  已删除 {} 个工作流", deletedWorkflowCount);
            }
        } else {
            log.info("  非升级批次，跳过工作流删除");
        }
        
        // 4. 重置所有 DeviceTask 为空闲状态 (不删除，只重置)
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
        if (!tasks.isEmpty()) {
            for (DeviceTask task : tasks) {
                // 清空 taskType，设置 status = IDLE
                task.setTaskType(null);                     // 清空操作类型
                task.setStatus(DeviceTask.TaskStatus.IDLE); // 状态设为空闲
                task.setBatchId(null);                      // 清除批次绑定
                task.setBatchName(null);                    // 清除批次名称
                task.setErrorMessage(null);                 // 清除错误信息
                task.setFilePath(null);                     // 清除文件路径
                task.setBackupFilePath(null);               // 清除备份路径
                task.setBackupFileName(null);               // 清除备份文件名
                task.setCurrentVersion(null);               // 清除当前版本
                task.setPreviousVersion(null);              // 清除升级前版本
                task.setTargetVersion(null);                // 清除目标版本
                task.setRetryCount(0);                      // 重置重试次数
                task.setDebugPayload(null);                 // 清除调试信息
                task.setTaskInfoActionTime(null);           // 清除 TaskInfo 时间戳
                task.setCompletedTime(java.time.LocalDateTime.now()); // 记录重置时间
                
                deviceTaskRepository.save(task);
            }
            log.info("  已重置 {} 个设备任务为空闲状态", tasks.size());
        }
        
        // 5. 删除关联的批次设备
        List<BatchDevice> batchDevices = batchDeviceRepository.findByBatchId(batchId);
        if (!batchDevices.isEmpty()) {
            batchDeviceRepository.deleteAll(batchDevices);
            log.info("  已删除 {} 个批次设备关联", batchDevices.size());
        }
        
        // 6. 通知 TaskInfo 批次删除（如果批次已通知）
        if (batch.getBatchActionTime() != null) {
            try {
                // ✅ 使用正确的方法：notifyBatchDeleted（根据批次类型动态设置 taskType 和 ActionType）
                taskInfoNotificationService.notifyBatchDeleted(batch);
                log.info("  ✓ 已通知 TaskInfo 批次删除");
            } catch (Exception e) {
                log.warn("  通知 TaskInfo 失败: {}", e.getMessage());
            }
        }
        
        // 7. 删除批次本身
            upgradeBatchRepository.delete(batch);
            
        log.info("✅ 批次已删除");
        log.info("  批次ID: {}", batchId);
        log.info("  批次类型: {}", batch.getBatchType());
        log.info("  已删除工作流: {} 个", deletedWorkflowCount);
        log.info("  已重置设备任务: {} 个 (设为空闲状态)", tasks.size());
        log.info("  已删除批次设备: {} 个", batchDevices.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }


    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 新架构方法：基于 Workflow 的批次管理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 触发下一步（内部方法，供自动模式和手动模式共用）
     */
    @Transactional
    public void triggerNextStep(String batchId, boolean isAutoMode) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 触发下一步: batchId={}, mode={}", batchId, isAutoMode ? "自动" : "手动");

        Batch batch = upgradeBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));

        List<UpgradeWorkflow> workflows = workflowManagementService.getWorkflowsByBatchId(batchId);
        workflowManagementService.computeAndSetWorkflowStatuses(workflows);

        // ⭐ 特殊处理：如果批次状态是 READY_TO_COMMIT，直接执行 COMMIT
        if (batch.getStatus() == Batch.BatchStatus.READY_TO_COMMIT) {
            log.info("  下一步: COMMIT");
            
            // ⭐ 过滤出未失败的工作流（只为成功的设备执行 COMMIT）
            List<UpgradeWorkflow> activeWorkflows = workflows.stream()
                .filter(wf -> !wf.isFailed())
                .collect(java.util.stream.Collectors.toList());
            
            if (activeWorkflows.isEmpty()) {
                log.warn("  所有工作流都已失败，无法执行 COMMIT");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return;
            }
            
            if (activeWorkflows.size() < workflows.size()) {
                log.info("  ⚠️ 有 {} 个工作流已失败，只为剩余 {} 个工作流执行 COMMIT",
                    workflows.size() - activeWorkflows.size(), activeWorkflows.size());
            }
            
            // ⭐ 新增：预检查所有设备是否可用（避免状态更新后才发现设备忙）
            log.info("  🔍 预检查设备可用性...");
            java.util.List<String> busyDevices = new java.util.ArrayList<>();
            for (UpgradeWorkflow workflow : activeWorkflows) {
                try {
                    uniquenessService.checkAndGetAvailableTask(
                        workflow.getDeviceId(),
                        devicemaintenance.entity.DeviceTask.TaskType.COMMIT,
                        batch.getBatchId()
                    );
                } catch (Exception e) {
                    // ⭐ 获取设备名称（优先使用 BatchDevice 中的 deviceName）
                    String deviceDisplayName = workflow.getDeviceId();
                    try {
                        java.util.Optional<BatchDevice> batchDevice = batchDeviceRepository.findByBatchIdAndDeviceId(
                            batch.getBatchId(), workflow.getDeviceId());
                        if (batchDevice.isPresent() && batchDevice.get().getDeviceName() != null 
                            && !batchDevice.get().getDeviceName().trim().isEmpty()) {
                            deviceDisplayName = batchDevice.get().getDeviceName();
                        }
                    } catch (Exception ex) {
                        // 忽略查询异常，使用 deviceId
                    }
                    
                    String deviceInfo = String.format("%s: %s",
                        resolveDeviceDisplay(batch.getBatchId(), workflow.getDeviceId()),
                        e.getMessage());
                    busyDevices.add(deviceInfo);
                    log.warn("    ⚠️ 设备不可用: {}", deviceInfo);
                }
            }
            
            if (!busyDevices.isEmpty()) {
                String errorMsg = String.format(
                    "Cannot execute COMMIT: %d device(s) are busy or unavailable:\n  - %s",
                    busyDevices.size(),
                    String.join("\n  - ", busyDevices)
                );
                log.error("  ❌ {}", errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            log.info("  ✅ 所有 {} 个设备可用", activeWorkflows.size());
            
            // ✅ 通过预检查后，再更新状态
            log.info("  📝 更新 Batch 状态: READY_TO_COMMIT → COMMITTING");
            batch.updateBatchStatus(Batch.BatchStatus.COMMITTING);
            upgradeBatchRepository.save(batch);
            
            executeStepForWorkflows(batch, activeWorkflows, "COMMIT");
            
            // ⭐ RPC 同步失败场景说明：
            // - 异步线程已经通过 RealServiceExecutionHelper.updateBatchStatus() 发送了 TaskInfo 通知
            // - 主线程无需再次发送通知（避免竞态条件导致的状态覆盖问题）
            // 
            // ⚠️ 原因：事务隔离级别（REPEATABLE READ）导致主线程读不到异步线程的更新
            
            log.info("  ✅ 步骤已提交执行");
            log.info("  ℹ️  状态通知已由 RPC 回调或 Kafka 监听器发送，主线程不再发送");
            
            log.info("✅ 下一步已触发: step=COMMIT");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        // ⭐ 特殊处理：READY_TO_PROCEED 状态（刚创建的批次，还没开始第一步）
        boolean isFirstStep = batch.getStatus() == Batch.BatchStatus.READY_TO_PROCEED;
        
        if (!isFirstStep && !workflowManagementService.areAllCurrentStepsCompleted(workflows)) {
            if (!isAutoMode) {
                throw new IllegalStateException("Not all workflows' current steps are completed");
            }
            log.warn("  并非所有工作流都完成，跳过自动触发");
            return;
        }

        // ⭐ 过滤出未失败的工作流（防止为已失败的设备创建后续任务）
        List<UpgradeWorkflow> activeWorkflows = workflows.stream()
            .filter(wf -> !wf.isFailed())
            .collect(java.util.stream.Collectors.toList());
        
        if (activeWorkflows.isEmpty()) {
            log.warn("  所有工作流都已失败，无法继续");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }
        
        if (activeWorkflows.size() < workflows.size()) {
            log.info("  ⚠️ 有 {} 个工作流已失败，只为剩余 {} 个工作流执行步骤",
                workflows.size() - activeWorkflows.size(), activeWorkflows.size());
        }
        
        // 确定要执行的步骤（从未失败的 workflow 获取）
        String currentStep = requireConsistentCurrentStep(activeWorkflows, batchId);
        String stepToExecute;
        
        if (isFirstStep) {
            // ⭐ 第一步：执行 currentStep 本身（这是 workflow 初始化时设置的第一个启用步骤）
            stepToExecute = currentStep;
            log.info("  执行第一步: {}", stepToExecute);
        } else {
            // ⭐ 后续步骤：当前步骤已完成，执行下一步
            stepToExecute = workflowManagementService.getNextEnabledStep(batch, currentStep);
            if (stepToExecute == null) {
                log.info("  所有步骤已完成");
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                return;
            }
            log.info("  当前步骤 {} 已完成，执行下一步: {}", currentStep, stepToExecute);
        }

        try {
            // ⭐ 新增：预检查所有设备是否可用（避免状态更新后才发现设备忙）
            log.info("  🔍 预检查设备可用性...");
            DeviceTask.TaskType taskType = getTaskTypeByStep(stepToExecute);
            java.util.List<String> busyDevices = new java.util.ArrayList<>();
            for (UpgradeWorkflow workflow : activeWorkflows) {
                try {
                    uniquenessService.checkAndGetAvailableTask(
                        workflow.getDeviceId(),
                        taskType,
                        batch.getBatchId()
                    );
                } catch (Exception e) {
                    // ⭐ 获取设备名称（优先使用 BatchDevice 中的 deviceName）
                    String deviceDisplayName = workflow.getDeviceId();
                    try {
                        java.util.Optional<BatchDevice> batchDevice = batchDeviceRepository.findByBatchIdAndDeviceId(
                            batch.getBatchId(), workflow.getDeviceId());
                        if (batchDevice.isPresent() && batchDevice.get().getDeviceName() != null 
                            && !batchDevice.get().getDeviceName().trim().isEmpty()) {
                            deviceDisplayName = batchDevice.get().getDeviceName();
                        }
                    } catch (Exception ex) {
                        // 忽略查询异常，使用 deviceId
                    }
                    
                    String deviceInfo = String.format("%s: %s",
                        resolveDeviceDisplay(batch.getBatchId(), workflow.getDeviceId()),
                        e.getMessage());
                    busyDevices.add(deviceInfo);
                    log.warn("    ⚠️ 设备不可用: {}", deviceInfo);
                }
            }
            
            if (!busyDevices.isEmpty()) {
                String errorMsg = String.format(
                    "Cannot execute %s: %d device(s) are busy or unavailable:\n  - %s",
                    stepToExecute,
                    busyDevices.size(),
                    String.join("\n  - ", busyDevices)
                );
                log.error("  ❌ {}", errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            log.info("  ✅ 所有 {} 个设备可用", activeWorkflows.size());
            
            // ✅ 通过预检查后，再更新状态
            // ✅ 统一逻辑：自动模式和手动模式都使用细分状态
            Batch.BatchStatus newStatus;
                switch (stepToExecute.toUpperCase()) {
                    case "DOWNLOAD":
                        newStatus = Batch.BatchStatus.DOWNLOADING;
                        break;
                    case "BACKUP":
                        newStatus = Batch.BatchStatus.BACKING_UP;
                        break;
                    case "UPGRADE":
                        newStatus = Batch.BatchStatus.UPGRADING;
                        break;
                    case "COMMIT":
                        newStatus = Batch.BatchStatus.COMMITTING;
                        break;
                    default:
                        newStatus = Batch.BatchStatus.RUNNING;
                        log.warn("  ⚠️ 未知的步骤类型: {}，使用 RUNNING 状态", stepToExecute);
            }
            
            log.info("  📝 更新 Batch 状态: {} → {}", batch.getStatus(), newStatus);
            batch.updateBatchStatus(newStatus);  // ⭐ updateBatchStatus 内部会自动设置 startedTime
            upgradeBatchRepository.save(batch);
            
            executeStepForWorkflows(batch, activeWorkflows, stepToExecute);
            
            // ⭐ RPC 同步失败场景说明：
            // - 异步线程已经通过 RealServiceExecutionHelper.updateBatchStatus() 发送了 TaskInfo 通知
            // - 主线程无需再次发送通知（避免竞态条件导致的状态覆盖问题）
            // 
            // ⚠️ 为什么主线程不发送通知？
            // 1. 事务隔离级别（REPEATABLE READ）：主线程事务开始时的快照，读不到异步线程的更新
            // 2. 即使清除 JPA 缓存，仍然读取的是事务快照中的旧数据
            // 3. 主线程查询到的状态永远是 DOWNLOADING，会覆盖异步线程发送的 FAILED 通知
            // 
            // ✅ 正确的通知逻辑：
            // - RPC 同步失败：由 RealServiceExecutionHelper.updateBatchStatus() 发送 ✅
            // - Kafka 异步通知：由 SystemChangeNotificationListener 发送 ✅
            // - 主线程：不发送通知（避免覆盖）✅
            
            log.info("  ✅ 步骤已提交执行");
            log.info("  ℹ️  状态通知已由 RPC 回调或 Kafka 监听器发送，主线程不再发送");

            log.info("✅ 下一步已触发: step={}", stepToExecute);
        } catch (Exception e) {
            // ❌ 执行失败：更新批次为失败状态
            log.error("❌ 执行步骤失败: step={}, error={}", stepToExecute, e.getMessage(), e);
            
            batch.updateBatchStatus(Batch.BatchStatus.FAILED);
            batch.setErrorMessage("Failed to execute " + stepToExecute + " step: " + e.getMessage());
            upgradeBatchRepository.save(batch);
            
            // 发送失败通知
            notifyBatchUpgradeStatus(batch);
            
            // 重新抛出异常，让调用者知道失败
            throw new RuntimeException("Failed to execute " + stepToExecute + " step: " + e.getMessage(), e);
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 手动触发下一步
     * 
     * ⚠️ 注意：
     * - MANUAL 模式：可以触发所有 READY_TO_XXX 状态
     * - AUTOMATIC 模式：只能触发 READY_TO_COMMIT 状态（因为 COMMIT 需要手动确认）
     */
    @Transactional
    public void proceedToNextStep(String batchId) {
        Batch batch = upgradeBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));

        // ⭐ 检查批次模式和状态
        if (batch.getExecutionMode() == Batch.ExecutionMode.MANUAL) {
            // 手动模式：支持所有 READY_TO_XXX 状态
            if (batch.getStatus() != Batch.BatchStatus.READY_TO_PROCEED &&
                batch.getStatus() != Batch.BatchStatus.READY_TO_BACKUP && 
                batch.getStatus() != Batch.BatchStatus.READY_TO_UPGRADE &&
                batch.getStatus() != Batch.BatchStatus.READY_TO_COMMIT) {
                throw new IllegalStateException("Batch status does not allow executing next step: " + batch.getStatus());
            }
        } else if (batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC) {
            // 自动模式：只支持 READY_TO_COMMIT 状态（COMMIT 需要手动触发）
            if (batch.getStatus() != Batch.BatchStatus.READY_TO_COMMIT) {
                throw new IllegalStateException("In AUTO mode, only READY_TO_COMMIT status can manually trigger COMMIT");
            }
        } else {
            throw new IllegalStateException("Unknown execution mode: " + batch.getExecutionMode());
        }

        triggerNextStep(batchId, false);
    }

    /**
     * 自动模式：为指定的工作流列表触发下一步
     * （用于自动模式下，只为成功完成当前步骤的工作流触发下一步）
     */
    @Transactional
    public void triggerNextStepForWorkflows(String batchId, List<UpgradeWorkflow> readyWorkflows) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🤖 [自动模式] 为部分工作流触发下一步: batchId={}, count={}", batchId, readyWorkflows.size());

        Batch batch = upgradeBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));

        if (readyWorkflows.isEmpty()) {
            log.warn("  没有就绪的工作流");
            return;
        }

        // 获取下一步（所有就绪工作流的下一步应该一致）
        String currentStep = requireConsistentCurrentStep(readyWorkflows, batchId);
        String nextStep = workflowManagementService.getNextEnabledStep(batch, currentStep);
        if (nextStep == null) {
            log.info("  所有步骤已完成");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        log.info("  下一步: {}", nextStep);

        // 仅为就绪的工作流执行下一步
        executeStepForWorkflows(batch, readyWorkflows, nextStep);

        // 更新批次状态
        List<UpgradeWorkflow> allWorkflows = workflowManagementService.getWorkflowsByBatchId(batchId);
        workflowManagementService.computeAndSetWorkflowStatuses(allWorkflows);  // ⭐ 必须先计算工作流状态
        Batch.BatchStatus newStatus = calculateBatchStatus(batch, allWorkflows);
        batch.updateBatchStatus(newStatus);
        upgradeBatchRepository.save(batch);

        notifyBatchUpgradeStatus(batch);

        log.info("✅ 部分工作流下一步已触发: step={}, count={}", nextStep, readyWorkflows.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 重试工作流
     * 
     * ⚠️ 修复说明：
     * - 不再调用批量接口（避免死锁）
     * - 直接调用 executeScheduledTask() 触发单设备 RPC
     * - 立即执行，不等待任何触发器
     */
    public void retryWorkflow(String batchId, String deviceId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 重试工作流: batchId={}, deviceId={}", batchId, deviceId);

        Batch batch = upgradeBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));

        // 1. 查找失败的 DeviceTask，确定要重试的步骤
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceId(batchId, deviceId);
        DeviceTask failedTask = selectRetryableFailedTask(tasks)
            .orElseThrow(() -> new IllegalStateException("无法找到失败的任务: deviceId=" + deviceId));
        
        String failedStep = failedTask.getTaskType().name();
        log.info("  失败步骤: {}, 任务ID: {}", failedStep, failedTask.getTaskId());
        
        // 2. ⭐ 更新 Workflow 的 retryCount（不改 DeviceTask 状态，避免死锁）
        UpgradeWorkflow workflow = workflowRepository.findByBatchIdAndDeviceId(batchId, deviceId)
            .orElseThrow(() -> new IllegalArgumentException("工作流不存在"));

        if (isFailedStepAlreadyCompletedInOp(batch, failedTask)) {
            log.info("  ℹ️ OP库确认失败步骤实际上已完成，转换为状态同步而非重试: deviceId={}, step={}", deviceId, failedStep);
            syncFailedTaskCompletionFromOp(batch, workflow, failedTask);
            return;
        }
        
        // ⭐ 人工重试：重置 workflow 重试计数，不受之前次数限制
        workflow = workflowManagementService.updateWorkflowStatus(workflow.getWorkflowId());
        workflow.setRetryCount(0);
        if (!workflow.canRetry()) {
            throw new IllegalStateException("Workflow cannot be retried: status=" + workflow.getStatus() + 
                ", retryCount=" + workflow.getRetryCount() + "/" + workflow.getMaxRetryCount());
        }
        
        workflow.incrementRetryCount();
        workflow.setCurrentStep(workflowManagementService.mapTaskTypeToStep(failedTask.getTaskType()));
        workflowRepository.save(workflow);
        log.info("  Workflow retryCount 已更新: {}/{}", workflow.getRetryCount(), workflow.getMaxRetryCount());

        // 3. ⭐ 重试启动后，任务不应继续显示 FAILED。
        //    先切到 PENDING，明确表达“已进入重试流程，等待重新下发 RPC”。
        rehydrateRetryTaskContext(failedTask, batch);
        failedTask.setStatus(DeviceTask.TaskStatus.PENDING);
        failedTask.setErrorMessage(null);
        failedTask.setCompletedTime(null);
        failedTask.setStartedTime(null);
        deviceTaskRepository.save(failedTask);
        log.info("  任务状态已切换为 PENDING，等待重试执行: taskId={}, deviceId={}, step={}",
            failedTask.getTaskId(), failedTask.getDeviceId(), failedStep);

        // 4. ⭐ 直接调用 executeScheduledTask() - 立即触发 RPC
        //    内部会：updateTaskToRunning (PENDING→RUNNING) → 调用RPC → 等待Kafka通知
        switch (failedStep.toUpperCase()) {
            case "DOWNLOAD":
                softwareDownloadService.executeScheduledTask(failedTask);
                break;
            case "BACKUP":
                deviceBackupService.executeScheduledTask(failedTask);
                break;
            case "UPGRADE":
                deviceUpgradeService.executeScheduledTask(failedTask);
                break;
            case "COMMIT":
                softwareCommitService.executeScheduledTask(failedTask);
                break;
            default:
                throw new IllegalArgumentException("Unknown step type: " + failedStep);
        }

        // 5. ⭐ 更新批次状态（重试开始后，失败的 workflow 变成运行中，批次状态应该更新）
        List<UpgradeWorkflow> allWorkflows = workflowManagementService.getWorkflowsByBatchId(batchId);
        workflowManagementService.computeAndSetWorkflowStatuses(allWorkflows);
        Batch.BatchStatus newStatus = calculateBatchStatus(batch, allWorkflows);
        batch.updateBatchStatus(newStatus);
        upgradeBatchRepository.save(batch);
        log.info("  批次状态已更新: {}", newStatus);

        // 6. ⭐ 通知 TaskInfo
        notifyBatchUpgradeStatus(batch);

        log.info("✅ 工作流重试已启动（RPC 已发出）");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private boolean isFailedStepAlreadyCompletedInOp(Batch batch, DeviceTask failedTask) {
        try {
            DeviceOperationStatus deviceStatus =
                deviceMaintenanceStatusService.getDeviceOperationStatus(failedTask.getDeviceId());
            String deviceDisplay = buildDeviceDisplay(
                failedTask.getDeviceId(),
                failedTask.getDeviceIp(),
                failedTask.getDeviceName() != null ? failedTask.getDeviceName() : (deviceStatus != null ? deviceStatus.getDeviceName() : null)
            );
            if (deviceStatus == null || failedTask.getTaskType() == null) {
                log.info("  ℹ️ [重试前-OP确认] deviceId={}, device={}, step={}, opStatusMissing=true",
                    failedTask.getDeviceId(), deviceDisplay, failedTask.getTaskType());
                return false;
            }

            switch (failedTask.getTaskType()) {
                case DOWNLOAD:
                    String downloadState = normalizeState(
                        deviceStatus.getSoftwareOperations() != null &&
                        deviceStatus.getSoftwareOperations().getDownload() != null
                            ? deviceStatus.getSoftwareOperations().getDownload().getState()
                            : null
                    );
                    log.info("  🔎 [重试前-OP确认] batchId={}, deviceId={}, device={}, step=DOWNLOAD, op.downloadState={}, taskStatus={}, ready=false, reason=downloadSnapshotAmbiguousAfterRollback",
                        batch.getBatchId(), failedTask.getDeviceId(), deviceDisplay, downloadState, failedTask.getStatus());
                    return false;
                case BACKUP:
                    String backupState = normalizeState(
                        deviceStatus.getDatabaseOperations() != null &&
                        deviceStatus.getDatabaseOperations().getBackup() != null
                            ? deviceStatus.getDatabaseOperations().getBackup().getState()
                            : null
                    );
                    boolean backupCompleted = "COMPLETE".equals(backupState);
                    log.info("  🔎 [重试前-OP确认] batchId={}, deviceId={}, device={}, step=BACKUP, op.backupState={}, taskStatus={}, ready={}",
                        batch.getBatchId(), failedTask.getDeviceId(), deviceDisplay, backupState, failedTask.getStatus(), backupCompleted);
                    return backupCompleted;
                case UPGRADE:
                    String upgradeState = normalizeState(
                        deviceStatus.getSoftwareOperations() != null &&
                        deviceStatus.getSoftwareOperations().getUpgrade() != null
                            ? deviceStatus.getSoftwareOperations().getUpgrade().getState()
                            : null
                    );
                    String currentSoftware = deviceStatus.getCurrentSoftware();
                    String targetVersion = resolveTargetVersion(batch, failedTask);
                    boolean upgradeCompleted = ("ACTIVE_COMPLETE".equals(upgradeState) || "COMPLETE".equals(upgradeState))
                        && currentSoftware != null
                        && targetVersion != null
                        && normalizeVersion(currentSoftware).equals(normalizeVersion(targetVersion));
                    log.info("  🔎 [重试前-OP确认] batchId={}, deviceId={}, device={}, step=UPGRADE, op.upgradeState={}, op.currentSoftware={}, targetVersion={}, taskStatus={}, ready={}",
                        batch.getBatchId(), failedTask.getDeviceId(), deviceDisplay, upgradeState, currentSoftware, targetVersion, failedTask.getStatus(), upgradeCompleted);
                    return upgradeCompleted;
                case COMMIT:
                    String commitState = normalizeState(
                        deviceStatus.getSoftwareOperations() != null &&
                        deviceStatus.getSoftwareOperations().getUpgrade() != null
                            ? deviceStatus.getSoftwareOperations().getUpgrade().getState()
                            : null
                    );
                    boolean commitCompleted = "COMPLETE".equals(commitState);
                    log.info("  🔎 [重试前-OP确认] batchId={}, deviceId={}, device={}, step=COMMIT, op.commitSourceState={}, taskStatus={}, ready={}",
                        batch.getBatchId(), failedTask.getDeviceId(), deviceDisplay, commitState, failedTask.getStatus(), commitCompleted);
                    return commitCompleted;
                default:
                    log.info("  ℹ️ [重试前-OP确认] batchId={}, deviceId={}, device={}, step={}, unsupported=true",
                        batch.getBatchId(), failedTask.getDeviceId(), deviceDisplay, failedTask.getTaskType());
                    return false;
            }
        } catch (Exception e) {
            log.warn("  ⚠️ 重试前查询OP真实状态失败，按普通重试处理: deviceId={}, step={}, error={}",
                failedTask.getDeviceId(), failedTask.getTaskType(), e.getMessage());
            return false;
        }
    }

    private void syncFailedTaskCompletionFromOp(Batch batch, UpgradeWorkflow workflow, DeviceTask failedTask) {
        DeviceOperationStatus deviceStatus = deviceMaintenanceStatusService.getDeviceOperationStatus(failedTask.getDeviceId());
        String deviceDisplay = buildDeviceDisplay(
            failedTask.getDeviceId(),
            failedTask.getDeviceIp(),
            failedTask.getDeviceName() != null ? failedTask.getDeviceName() : (deviceStatus != null ? deviceStatus.getDeviceName() : null)
        );
        log.info("  🧭 [状态同步] batchId={}, deviceId={}, device={}, failedStep={}, op.currentSoftware={}, taskStatus={}→COMPLETED",
            batch.getBatchId(),
            failedTask.getDeviceId(),
            deviceDisplay,
            failedTask.getTaskType(),
            deviceStatus != null ? deviceStatus.getCurrentSoftware() : null,
            failedTask.getStatus());
        failedTask.setStatus(DeviceTask.TaskStatus.COMPLETED);
        failedTask.setErrorMessage(null);
        failedTask.setCompletedTime(LocalDateTime.now());
        if (deviceStatus != null && deviceStatus.getCurrentSoftware() != null) {
            failedTask.setCurrentVersion(deviceStatus.getCurrentSoftware());
        }
        deviceTaskRepository.save(failedTask);

        workflowManagementService.updateWorkflowStatus(workflow.getWorkflowId());

        List<UpgradeWorkflow> allWorkflows = workflowManagementService.getWorkflowsByBatchId(batch.getBatchId());
        workflowManagementService.computeAndSetWorkflowStatuses(allWorkflows);
        Batch.BatchStatus newStatus = calculateBatchStatus(batch, allWorkflows);
        log.info("  🧭 [状态同步] batchId={}, workflowId={}, newBatchStatus={}",
            batch.getBatchId(), workflow.getWorkflowId(), newStatus);
        batch.updateBatchStatus(newStatus);
        upgradeBatchRepository.save(batch);
        notifyBatchUpgradeStatus(batch);
    }

    private String resolveTargetVersion(Batch batch, DeviceTask task) {
        if (task.getTargetVersion() != null && !task.getTargetVersion().trim().isEmpty()) {
            return task.getTargetVersion();
        }
        return batch.getTargetVersion();
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

    private String normalizeState(String state) {
        if (state == null) {
            return null;
        }
        return state.trim().toUpperCase().replace('-', '_');
    }

    private void validateDevicesBeforeBatchCreation(List<String> deviceIds) {
        List<String> activeCompleteDevices = new ArrayList<>();

        for (String deviceId : deviceIds) {
            DeviceOperationStatus deviceStatus =
                deviceMaintenanceStatusService.getDeviceOperationStatus(deviceId);
            String upgradeState =
                deviceStatus != null
                    && deviceStatus.getSoftwareOperations() != null
                    && deviceStatus.getSoftwareOperations().getUpgrade() != null
                        ? deviceStatus.getSoftwareOperations().getUpgrade().getState()
                        : null;

            log.info("  🔎 [批次创建校验] deviceId={}, upgradeState={}", deviceId, upgradeState);

            if ("ACTIVE_COMPLETE".equals(normalizeState(upgradeState))) {
                String deviceName = deviceStatus != null ? deviceStatus.getDeviceName() : null;
                String deviceDisplay = StringUtils.hasText(deviceName)
                    ? deviceName.trim()
                    : resolveDeviceDisplay(null, deviceId);
                activeCompleteDevices.add(deviceDisplay + " [" + deviceId + "]");
            }
        }

        if (!activeCompleteDevices.isEmpty()) {
            String errorMsg = String.format(
                "Cannot create batch update: %d device(s) are in active-complete state and have not been committed:\n  - %s",
                activeCompleteDevices.size(),
                String.join("\n  - ", activeCompleteDevices)
            );
            log.error("  ❌ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

    /**
     * 移除失败的工作流
     */
    @Transactional
    public void removeWorkflow(String batchId, String deviceId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🗑️ 移除工作流: batchId={}, deviceId={}", batchId, deviceId);

        Batch batch = upgradeBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));

        workflowManagementService.removeWorkflow(batchId, deviceId);
        batchDeviceRepository.findByBatchIdAndDeviceId(batchId, deviceId)
            .ifPresent(batchDeviceRepository::delete);

        List<UpgradeWorkflow> remainingWorkflows = workflowManagementService.getWorkflowsByBatchId(batchId);
        
        // 直接使用实际工作流数量，避免数据不一致
        batch.setDeviceCount(remainingWorkflows.size());
        if (remainingWorkflows.isEmpty()) {
            batch.updateBatchStatus(Batch.BatchStatus.CANCELLED);
        } else {
            workflowManagementService.computeAndSetWorkflowStatuses(remainingWorkflows);  // ⭐ 必须先计算工作流状态
            Batch.BatchStatus newStatus = calculateBatchStatus(batch, remainingWorkflows);
            batch.updateBatchStatus(newStatus);
        }

        upgradeBatchRepository.save(batch);

        notifyBatchUpgradeStatus(batch);

        log.info("✅ 工作流已移除，剩余工作流数: {}", remainingWorkflows.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 为所有工作流执行指定步骤（高复用方法）
     */
    private void executeStepForWorkflows(
        Batch batch,
        List<UpgradeWorkflow> workflows,
        String step
    ) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 执行{}步骤: batchId={}, workflowCount={}", step, batch.getBatchId(), workflows.size());

        try {
            // 根据 Batch 配置判断步骤是否启用
            boolean stepEnabled = isStepEnabledInBatch(batch, step);
            if (!stepEnabled) {
                log.info("  步骤{}未启用，跳过", step);
                return;
            }

            List<UpgradeWorkflow> activeWorkflows = workflows;
            log.info("  需要执行该步骤的工作流数: {}", activeWorkflows.size());

            // 先批量更新工作流的 currentStep（避免锁冲突）
            // ⚠️ 注意：Workflow 的 status 是计算字段，不需要设置
            for (UpgradeWorkflow workflow : activeWorkflows) {
                workflow.setCurrentStep(step);
            }
            workflowRepository.saveAll(activeWorkflows);
            log.info("  ✅ 已更新{}个工作流 currentStep={}", activeWorkflows.size(), step);

            // ⭐ 调用批量接口（一次性处理所有设备）
            try {
                switch (step.toUpperCase()) {
                    case "DOWNLOAD":
                        executeDownloadForWorkflow(batch, activeWorkflows);
                        break;
                    case "BACKUP":
                        executeBackupForWorkflow(batch, activeWorkflows);
                        break;
                    case "UPGRADE":
                        executeUpgradeForWorkflow(batch, activeWorkflows);
                        break;
                    case "COMMIT":
                        executeCommitForWorkflow(batch, activeWorkflows);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown step type: " + step);
                }
                log.info("✅ 批量任务执行完成: {} 个设备", activeWorkflows.size());
            } catch (Exception e) {
                log.error("⚠️ 批量任务执行失败: {}", e.getMessage());
                // 对于MANUAL模式，任何失败都应该停止批次
                if (batch.getExecutionMode() == Batch.ExecutionMode.MANUAL) {
                    throw new RuntimeException("Batch task failed in MANUAL mode, stopping batch execution: " + e.getMessage(), e);
                }
                // AUTOMATIC模式：记录错误但继续
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            // ⚠️ 外层 catch：记录日志后重新抛出，不吞异常
            log.error("执行{}步骤失败: batchId={}", step, batch.getBatchId(), e);
            throw new RuntimeException("Failed to execute step: " + e.getMessage(), e);
        }
    }


    /**
     * 为工作流执行下载步骤（调用 batch 接口，内部调用模式）
     */
    private void executeDownloadForWorkflow(Batch batch, List<UpgradeWorkflow> workflows) {
        // ⭐ 构建批量下载请求
        SoftwareDownloadDto.BatchDownloadRequest downloadRequest = new SoftwareDownloadDto.BatchDownloadRequest();
        
        // ⭐ 工作流内部调用：不传 batchName（避免创建新 Batch），传 batchId 和 workflowIds
        downloadRequest.setBatchId(batch.getBatchId());  // 关联到现有Batch
        downloadRequest.setWorkflowIds(workflows.stream()
            .collect(Collectors.toMap(
                UpgradeWorkflow::getDeviceId,
                UpgradeWorkflow::getWorkflowId
            )));  // deviceId -> workflowId 映射
        
        downloadRequest.setDeviceIds(workflows.stream()
            .map(UpgradeWorkflow::getDeviceId)
            .collect(Collectors.toList()));
        downloadRequest.setSoftwarePackage(batch.getFilePath());  // 文件路径
        downloadRequest.setSftpServerId(batch.getSftpServerName());
        downloadRequest.setDebug(batch.getDebug());
        downloadRequest.setRemark("Workflow batch download");
        
        // ⭐ 调用 batch 接口，isInternalCall=true（不发送TaskInfo通知）
        SoftwareDownloadDto.BatchDownloadResponse downloadResponse =
            softwareDownloadService.batchDownload(downloadRequest, true);
        validateWorkflowDownloadResult(workflows, downloadResponse);
        
        log.info("✅ 批量下载任务已创建: {} 个设备", workflows.size());
    }

    /**
     * 为工作流执行备份步骤（调用 batch 接口，内部调用模式）
     */
    private void executeBackupForWorkflow(Batch batch, List<UpgradeWorkflow> workflows) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 [工作流-备份] 开始执行批量备份");
        log.info("  批次ID: {}", batch.getBatchId());
        log.info("  工作流数: {}", workflows.size());
        log.info("  备份路径: {}", batch.getBackupBasePath());
        log.info("  SFTP服务器: {}", batch.getSftpServerName());
        
        // ⭐ 防御性检查：理论上在批次创建时已验证，这里再次确认
        if (batch.getBackupBasePath() == null || batch.getBackupBasePath().trim().isEmpty()) {
            String errorMsg = "备份基础路径未设置（数据不一致），无法执行备份步骤";
            log.error("❌ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        if (batch.getSftpServerName() == null || batch.getSftpServerName().trim().isEmpty()) {
            String errorMsg = "SFTP服务器未设置（数据不一致），无法执行备份步骤";
            log.error("❌ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        // ⭐ 构建批量备份请求
        DeviceBackupDto.BatchBackupRequest backupRequest = new DeviceBackupDto.BatchBackupRequest();
        
        // ⭐ 工作流内部调用：不传 batchName（避免创建新 Batch），传 batchId 和 workflowIds
        backupRequest.setBatchId(batch.getBatchId());  // 关联到现有Batch
        backupRequest.setWorkflowIds(workflows.stream()
            .collect(Collectors.toMap(
                UpgradeWorkflow::getDeviceId,
                UpgradeWorkflow::getWorkflowId
            )));  // deviceId -> workflowId 映射
        
        backupRequest.setDeviceIds(workflows.stream()
            .map(UpgradeWorkflow::getDeviceId)
            .collect(Collectors.toList()));
        backupRequest.setBasePath(batch.getBackupBasePath());
        backupRequest.setSftpServerName(batch.getSftpServerName());
        backupRequest.setDebug(batch.getDebug());
        backupRequest.setRemark("Workflow batch backup");
        
        log.info("  ✓ 备份请求已构建");
        log.info("    - batchId: {}", backupRequest.getBatchId());
        log.info("    - workflowIds: {}", backupRequest.getWorkflowIds());
        log.info("    - deviceIds: {}", backupRequest.getDeviceIds());
        log.info("    - basePath: {}", backupRequest.getBasePath());
        log.info("    - sftpServerName: {}", backupRequest.getSftpServerName());
        log.info("    - debug: {}", backupRequest.getDebug());
        log.info("  📊 来源 Batch 对象的值:");
        log.info("    - batch.backupBasePath: {}", batch.getBackupBasePath());
        log.info("    - batch.sftpServerName: {}", batch.getSftpServerName());
        log.info("    - batch.debug: {}", batch.getDebug());
        
        try {
            // ⭐ 调用 batch 接口，isInternalCall=true（不发送TaskInfo通知）
            DeviceBackupDto.BatchBackupSummary backupResponse =
                deviceBackupService.batchBackupDevices(backupRequest, true);
            validateWorkflowBackupResult(workflows, backupResponse);
            log.info("✅ 批量备份任务已创建: {} 个设备", workflows.size());
        } catch (Exception e) {
            log.error("❌ Batch backup task creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Batch backup failed: " + e.getMessage(), e);
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 为工作流执行升级步骤（调用 batch 接口，内部调用模式）
     */
    private void executeUpgradeForWorkflow(Batch batch, List<UpgradeWorkflow> workflows) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 [工作流-升级] 开始执行批量升级");
        log.info("  批次ID: {}", batch.getBatchId());
        log.info("  工作流数: {}", workflows.size());
        log.info("  升级文件: {}", batch.getFilePath());
        log.info("  目标版本: {}", batch.getTargetVersion());
        log.info("  SFTP服务器: {}", batch.getSftpServerName());
        
        // ⭐ 防御性检查：理论上在批次创建时已验证，这里再次确认
        if (batch.getFilePath() == null || batch.getFilePath().trim().isEmpty()) {
            String errorMsg = "升级文件路径未设置（数据不一致），无法执行升级步骤";
            log.error("❌ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        if (batch.getSftpServerName() == null || batch.getSftpServerName().trim().isEmpty()) {
            String errorMsg = "SFTP服务器未设置（数据不一致），无法执行升级步骤";
            log.error("❌ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        // ⭐ targetVersion 是可选的业务信息字段，为空也可以继续执行
        if (batch.getTargetVersion() == null) {
            log.info("  ℹ️  目标版本未提供，升级操作仍会继续（targetVersion 仅用于记录）");
        }
        
        // ⭐ 构建批量升级请求
        DeviceUpgradeDto.BatchUpgradeRequest upgradeRequest = new DeviceUpgradeDto.BatchUpgradeRequest();
        
        // ⭐ 工作流内部调用：不传 batchName（避免创建新 Batch），传 batchId 和 workflowIds
        upgradeRequest.setBatchId(batch.getBatchId());  // 关联到现有Batch
        upgradeRequest.setWorkflowIds(workflows.stream()
            .collect(Collectors.toMap(
                UpgradeWorkflow::getDeviceId,
                UpgradeWorkflow::getWorkflowId
            )));  // deviceId -> workflowId 映射
        
        upgradeRequest.setDeviceIds(workflows.stream()
            .map(UpgradeWorkflow::getDeviceId)
            .collect(Collectors.toList()));
        upgradeRequest.setUpgradeFilePath(batch.getFilePath());
        upgradeRequest.setTargetVersion(batch.getTargetVersion());
        upgradeRequest.setSftpServerName(batch.getSftpServerName());
        upgradeRequest.setDebug(batch.getDebug());
        upgradeRequest.setRemark("Workflow batch upgrade");
        
        log.info("  ✓ 升级请求已构建");
        log.info("    - batchId: {}", upgradeRequest.getBatchId());
        log.info("    - workflowIds: {}", upgradeRequest.getWorkflowIds());
        log.info("    - deviceIds: {}", upgradeRequest.getDeviceIds());
        log.info("    - upgradeFilePath: {}", upgradeRequest.getUpgradeFilePath());
        log.info("    - targetVersion: {}", upgradeRequest.getTargetVersion());
        log.info("    - sftpServerName: {}", upgradeRequest.getSftpServerName());
        log.info("    - debug: {}", upgradeRequest.getDebug());
        log.info("  📊 来源 Batch 对象的值:");
        log.info("    - batch.filePath: {}", batch.getFilePath());
        log.info("    - batch.targetVersion: {}", batch.getTargetVersion());
        log.info("    - batch.sftpServerName: {}", batch.getSftpServerName());
        log.info("    - batch.debug: {}", batch.getDebug());
        
        try {
            // ⭐ 调用 batch 接口，isInternalCall=true（不发送TaskInfo通知）
            DeviceUpgradeDto.BatchUpgradeResponse upgradeResponse =
                deviceUpgradeService.batchUpgrade(upgradeRequest, true);
            validateWorkflowUpgradeResult(workflows, upgradeResponse);
            log.info("✅ 批量升级任务已创建: {} 个设备", workflows.size());
        } catch (Exception e) {
            log.error("❌ Batch upgrade task creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Batch upgrade failed: " + e.getMessage(), e);
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void validateWorkflowDownloadResult(
        List<UpgradeWorkflow> workflows,
        SoftwareDownloadDto.BatchDownloadResponse response
    ) {
        if (response == null || response.getDeviceResults() == null) {
            throw new IllegalStateException("Download RPC result is empty");
        }

        Set<String> returnedDeviceIds = response.getDeviceResults().stream()
            .map(SoftwareDownloadDto.DeviceDownloadResult::getDeviceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<String> missingDevices = workflows.stream()
            .map(UpgradeWorkflow::getDeviceId)
            .filter(deviceId -> !returnedDeviceIds.contains(deviceId))
            .collect(Collectors.toList());

        if (!missingDevices.isEmpty()) {
            markWorkflowStepFailures(workflows, DeviceTask.TaskType.DOWNLOAD,
                missingDevices.stream().collect(Collectors.toMap(
                    deviceId -> deviceId,
                    deviceId -> "Download RPC result missing"
                )),
                Collections.emptyMap(),
                true);
            throw new IllegalStateException(String.format(
                "Download RPC result missing for %d device(s): %s",
                missingDevices.size(),
                missingDevices.stream()
                    .map(deviceId -> resolveDeviceDisplay(null, deviceId))
                    .collect(Collectors.joining(", "))
            ));
        }

        if (response.getFailedCount() != null && response.getFailedCount() > 0) {
            Map<String, String> failedTaskIds = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .filter(result -> StringUtils.hasText(result.getTaskId()))
                .collect(Collectors.toMap(
                    SoftwareDownloadDto.DeviceDownloadResult::getDeviceId,
                    SoftwareDownloadDto.DeviceDownloadResult::getTaskId,
                    (left, right) -> left
                ));
            Map<String, String> failedDeviceErrors = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .collect(Collectors.toMap(
                    SoftwareDownloadDto.DeviceDownloadResult::getDeviceId,
                    result -> result.getErrorMessage() != null ? result.getErrorMessage() : "Download RPC failed",
                    (left, right) -> left
                ));
            markWorkflowStepFailures(workflows, DeviceTask.TaskType.DOWNLOAD, failedDeviceErrors, failedTaskIds, false);
            String failedDevices = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .map(result -> formatDeviceFailure(resolveDeviceDisplay(null, result.getDeviceId()), result.getErrorMessage()))
                .collect(Collectors.joining(", "));
            throw new IllegalStateException(String.format(
                "Download RPC was not issued or failed for %d device(s): %s",
                response.getFailedCount(),
                failedDevices
            ));
        }
    }

    private void validateWorkflowBackupResult(
        List<UpgradeWorkflow> workflows,
        DeviceBackupDto.BatchBackupSummary response
    ) {
        if (response == null || response.getDeviceResults() == null) {
            throw new IllegalStateException("Backup RPC result is empty");
        }

        Set<String> returnedDeviceIds = response.getDeviceResults().stream()
            .map(DeviceBackupDto.DeviceBackupResult::getDeviceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<String> missingDevices = workflows.stream()
            .map(UpgradeWorkflow::getDeviceId)
            .filter(deviceId -> !returnedDeviceIds.contains(deviceId))
            .collect(Collectors.toList());

        if (!missingDevices.isEmpty()) {
            markWorkflowStepFailures(workflows, DeviceTask.TaskType.BACKUP,
                missingDevices.stream().collect(Collectors.toMap(
                    deviceId -> deviceId,
                    deviceId -> "Backup RPC result missing"
                )),
                Collections.emptyMap(),
                true);
            throw new IllegalStateException(String.format(
                "Backup RPC result missing for %d device(s): %s",
                missingDevices.size(),
                missingDevices.stream()
                    .map(deviceId -> resolveDeviceDisplay(null, deviceId))
                    .collect(Collectors.joining(", "))
            ));
        }

        if (response.getFailedCount() != null && response.getFailedCount() > 0) {
            Map<String, String> failedTaskIds = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .filter(result -> StringUtils.hasText(result.getTaskId()))
                .collect(Collectors.toMap(
                    DeviceBackupDto.DeviceBackupResult::getDeviceId,
                    DeviceBackupDto.DeviceBackupResult::getTaskId,
                    (left, right) -> left
                ));
            Map<String, String> failedDeviceErrors = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .collect(Collectors.toMap(
                    DeviceBackupDto.DeviceBackupResult::getDeviceId,
                    result -> result.getErrorMessage() != null ? result.getErrorMessage() : "Backup RPC failed",
                    (left, right) -> left
                ));
            markWorkflowStepFailures(workflows, DeviceTask.TaskType.BACKUP, failedDeviceErrors, failedTaskIds, false);
            String failedDevices = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .map(result -> formatDeviceFailure(resolveDeviceDisplay(null, result.getDeviceId()), result.getErrorMessage()))
                .collect(Collectors.joining(", "));
            throw new IllegalStateException(String.format(
                "Backup RPC was not issued or failed for %d device(s): %s",
                response.getFailedCount(),
                failedDevices
            ));
        }
    }

    private void validateWorkflowUpgradeResult(
        List<UpgradeWorkflow> workflows,
        DeviceUpgradeDto.BatchUpgradeResponse response
    ) {
        if (response == null || response.getDeviceResults() == null) {
            throw new IllegalStateException("Upgrade RPC result is empty");
        }

        Set<String> returnedDeviceIds = response.getDeviceResults().stream()
            .map(DeviceUpgradeDto.DeviceUpgradeResult::getDeviceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<String> missingDevices = workflows.stream()
            .map(UpgradeWorkflow::getDeviceId)
            .filter(deviceId -> !returnedDeviceIds.contains(deviceId))
            .collect(Collectors.toList());

        if (!missingDevices.isEmpty()) {
            markWorkflowStepFailures(workflows, DeviceTask.TaskType.UPGRADE,
                missingDevices.stream().collect(Collectors.toMap(
                    deviceId -> deviceId,
                    deviceId -> "Upgrade RPC result missing"
                )),
                Collections.emptyMap(),
                true);
            throw new IllegalStateException(String.format(
                "Upgrade RPC result missing for %d device(s): %s",
                missingDevices.size(),
                missingDevices.stream()
                    .map(deviceId -> resolveDeviceDisplay(null, deviceId))
                    .collect(Collectors.joining(", "))
            ));
        }

        if (response.getFailedCount() != null && response.getFailedCount() > 0) {
            Map<String, String> failedTaskIds = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .filter(result -> StringUtils.hasText(result.getTaskId()))
                .collect(Collectors.toMap(
                    DeviceUpgradeDto.DeviceUpgradeResult::getDeviceId,
                    DeviceUpgradeDto.DeviceUpgradeResult::getTaskId,
                    (left, right) -> left
                ));
            Map<String, String> failedDeviceErrors = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .collect(Collectors.toMap(
                    DeviceUpgradeDto.DeviceUpgradeResult::getDeviceId,
                    result -> result.getErrorMessage() != null ? result.getErrorMessage() : "Upgrade RPC failed",
                    (left, right) -> left
                ));
            markWorkflowStepFailures(workflows, DeviceTask.TaskType.UPGRADE, failedDeviceErrors, failedTaskIds, false);
            String failedDevices = response.getDeviceResults().stream()
                .filter(result -> "FAILED".equals(result.getStatus()))
                .map(result -> formatDeviceFailure(resolveDeviceDisplay(null, result.getDeviceId()), result.getErrorMessage()))
                .collect(Collectors.joining(", "));
            throw new IllegalStateException(String.format(
                "Upgrade RPC was not issued or failed for %d device(s): %s",
                response.getFailedCount(),
                failedDevices
            ));
        }
    }

    private String formatDeviceFailure(String deviceId, String errorMessage) {
        String message = errorMessage != null ? errorMessage : "unknown error";
        return deviceId + " (" + message + ")";
    }

    private String formatCommitFailure(SoftwareCommitDto.DeviceCommitResult result) {
        return formatDisplayWithMessage(
            buildDeviceDisplay(result.getDeviceId(), result.getDeviceIp(), result.getDeviceName()),
            result.getErrorMessage()
        );
    }

    private String formatWorkflowFailure(String batchId, UpgradeWorkflow workflow) {
        return formatDisplayWithMessage(
            resolveDeviceDisplay(batchId, workflow.getDeviceId()),
            "状态: " + workflow.getStatus()
        );
    }

    private String formatDisplayWithMessage(String deviceDisplay, String message) {
        String resolvedDisplay = StringUtils.hasText(deviceDisplay) ? deviceDisplay : "unknown-device";
        String resolvedMessage = StringUtils.hasText(message) ? message : "unknown error";
        return resolvedDisplay + " (" + resolvedMessage + ")";
    }

    private String resolveDeviceDisplay(String batchId, String deviceId) {
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceIdOrderByCreatedTimeAsc(batchId, deviceId);
        for (int i = tasks.size() - 1; i >= 0; i--) {
            DeviceTask task = tasks.get(i);
            String display = buildDeviceDisplay(task.getDeviceId(), task.getDeviceIp(), task.getDeviceName());
            if (!deviceId.equals(display)) {
                return display;
            }
        }

        try {
            Optional<BatchDevice> batchDevice = batchDeviceRepository.findByBatchIdAndDeviceId(batchId, deviceId);
            if (batchDevice.isPresent()) {
                String display = buildDeviceDisplay(deviceId, null, batchDevice.get().getDeviceName());
                if (!deviceId.equals(display)) {
                    return display;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve batch device display: batchId={}, deviceId={}, error={}",
                batchId, deviceId, e.getMessage());
        }

        try {
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
            return buildDeviceDisplay(deviceId, deviceInfo.getIp(), deviceInfo.getFriendlyName());
        } catch (Exception e) {
            log.debug("Failed to query device identity for display: deviceId={}, error={}", deviceId, e.getMessage());
        }

        return deviceId;
    }

    private String buildDeviceDisplay(String deviceId, String deviceIp, String deviceName) {
        boolean hasIp = StringUtils.hasText(deviceIp);
        boolean hasName = StringUtils.hasText(deviceName);
        if (hasIp && hasName) {
            return deviceIp.trim() + " + " + deviceName.trim();
        }
        if (hasIp) {
            return deviceIp.trim();
        }
        if (hasName) {
            return deviceName.trim();
        }
        return deviceId;
    }

    private void markWorkflowStepFailures(
        List<UpgradeWorkflow> workflows,
        DeviceTask.TaskType taskType,
        Map<String, String> failedDeviceErrors,
        Map<String, String> failedTaskIds,
        boolean allowCreatePlaceholder
    ) {
        if (failedDeviceErrors == null || failedDeviceErrors.isEmpty()) {
            return;
        }

        Map<String, UpgradeWorkflow> workflowByDeviceId = workflows.stream()
            .collect(Collectors.toMap(
                UpgradeWorkflow::getDeviceId,
                workflow -> workflow,
                (left, right) -> left
            ));

        for (Map.Entry<String, String> failure : failedDeviceErrors.entrySet()) {
            UpgradeWorkflow workflow = workflowByDeviceId.get(failure.getKey());
            if (workflow == null) {
                continue;
            }

            DeviceTask task = null;
            String taskId = failedTaskIds != null ? failedTaskIds.get(failure.getKey()) : null;
            if (StringUtils.hasText(taskId)) {
                task = deviceTaskRepository.findById(taskId).orElse(null);
            }

            List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceId(
                workflow.getBatchId(),
                workflow.getDeviceId()
            );

            if (task == null) {
                task = selectLatestTask(tasks.stream()
                    .filter(existing -> existing.getTaskType() == taskType)
                    .collect(Collectors.toList()))
                    .orElse(null);
            }

            if (task == null && !allowCreatePlaceholder) {
                log.warn("Skip duplicate workflow failure placeholder: batchId={}, workflowId={}, deviceId={}, taskType={}, taskId={}",
                    workflow.getBatchId(), workflow.getWorkflowId(), workflow.getDeviceId(), taskType, taskId);
                continue;
            }

            if (task == null) {
                task = new DeviceTask(taskCreationHelper.generateTaskId(), workflow.getDeviceId(), taskType);
            }

            task.setBatchId(workflow.getBatchId());
            task.setWorkflowId(workflow.getWorkflowId());
            task.setTaskType(taskType);
            task.setStatus(DeviceTask.TaskStatus.FAILED);
            task.setErrorMessage(failure.getValue());
            task.setCompletedTime(LocalDateTime.now());
            task.setUpdatedTime(LocalDateTime.now());
            deviceTaskRepository.save(task);

            try {
                workflowManagementService.updateWorkflowStatus(workflow.getWorkflowId());
            } catch (Exception e) {
                log.warn("Failed to update workflow status after marking step failure: workflowId={}, error={}",
                    workflow.getWorkflowId(), e.getMessage());
            }
        }
    }

    /**
     * 为工作流执行提交步骤（调用 batch 接口，内部调用模式）
     */
    private void executeCommitForWorkflow(Batch batch, List<UpgradeWorkflow> workflows) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔄 [工作流-提交] 开始执行批量提交");
        log.info("  批次ID: {}", batch.getBatchId());
        log.info("  工作流数: {}", workflows.size());
        log.info("  SFTP服务器: {}", batch.getSftpServerName());
        log.info("  文件路径: {}", batch.getFilePath());
        
        // ⭐ 前置验证：确保所有 workflow 都是 ACTIVATE_SUCCESS
        workflowManagementService.computeAndSetWorkflowStatuses(workflows);
        
        List<UpgradeWorkflow> notReadyWorkflows = workflows.stream()
            .filter(wf -> wf.getStatus() != UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS)
            .collect(Collectors.toList());
        
        if (!notReadyWorkflows.isEmpty()) {
            String errorMsg = String.format(
                "批次中有 %d 个设备未完成升级，无法提交。未完成设备: %s",
                notReadyWorkflows.size(),
                notReadyWorkflows.stream()
                    .map(wf -> formatWorkflowFailure(batch.getBatchId(), wf))
                    .collect(Collectors.joining(", "))
            );
            log.error("❌ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        log.info("  ✅ 验证通过: 所有 {} 个设备都已完成升级", workflows.size());
        
        // ⭐ 构建批量提交请求
        SoftwareCommitDto.BatchCommitRequest commitRequest = new SoftwareCommitDto.BatchCommitRequest();
        
        commitRequest.setDeviceIds(workflows.stream()
            .map(UpgradeWorkflow::getDeviceId)
            .collect(Collectors.toList()));
        
        commitRequest.setSftpServerName(batch.getSftpServerName());
        
        // ✅ 传递完整文件路径（不再只提取文件名）
        commitRequest.setFileName(batch.getFilePath());
        
        commitRequest.setDescription("Workflow batch commit");
        
        // ⭐ 设置 batchId 和 workflowIds（用于 workflow 内部调用）
        commitRequest.setBatchId(batch.getBatchId());
        commitRequest.setWorkflowIds(workflows.stream()
            .collect(Collectors.toMap(
                UpgradeWorkflow::getDeviceId,
                UpgradeWorkflow::getWorkflowId
            )));
        
        log.info("  ✓ 提交请求已构建");
        log.info("    - deviceIds: {}", commitRequest.getDeviceIds());
        log.info("    - sftpServerName: {}", commitRequest.getSftpServerName());
        log.info("    - fileName: {}", commitRequest.getFileName());
        log.info("    - batchId: {}", commitRequest.getBatchId());
        log.info("    - workflowIds: {}", commitRequest.getWorkflowIds());
        
        try {
            // ⭐ 调用 batch commit 接口
            SoftwareCommitDto.BatchCommitResponse commitResponse =
                softwareCommitService.batchCommit(commitRequest, batch.getDebug());

            if (commitResponse == null || commitResponse.getResults() == null) {
                markWorkflowStepFailures(workflows, DeviceTask.TaskType.COMMIT,
                    workflows.stream().collect(Collectors.toMap(
                        UpgradeWorkflow::getDeviceId,
                        workflow -> "Commit RPC result is empty"
                    )),
                    Collections.emptyMap(),
                    true);
                throw new IllegalStateException("Commit RPC result is empty");
            }

            Set<String> returnedDeviceIds = commitResponse.getResults().stream()
                .map(SoftwareCommitDto.DeviceCommitResult::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            List<String> missingDevices = workflows.stream()
                .map(UpgradeWorkflow::getDeviceId)
                .filter(deviceId -> !returnedDeviceIds.contains(deviceId))
                .collect(Collectors.toList());

            if (!missingDevices.isEmpty()) {
                markWorkflowStepFailures(workflows, DeviceTask.TaskType.COMMIT,
                    missingDevices.stream().collect(Collectors.toMap(
                        deviceId -> deviceId,
                        deviceId -> "Commit RPC result missing"
                    )),
                    Collections.emptyMap(),
                    true);
                throw new IllegalStateException(String.format(
                    "Commit RPC result missing for %d device(s): %s",
                    missingDevices.size(),
                    missingDevices.stream()
                        .map(deviceId -> resolveDeviceDisplay(batch.getBatchId(), deviceId))
                        .collect(Collectors.joining(", "))
                ));
            }

            if (commitResponse.getFailedCount() > 0) {
                Map<String, String> failedTaskIds = commitResponse.getResults().stream()
                    .filter(result -> "FAILED".equals(result.getStatus()))
                    .filter(result -> StringUtils.hasText(result.getTaskId()))
                    .collect(Collectors.toMap(
                        SoftwareCommitDto.DeviceCommitResult::getDeviceId,
                        SoftwareCommitDto.DeviceCommitResult::getTaskId,
                        (left, right) -> left
                    ));
                Map<String, String> failedDeviceErrors = commitResponse.getResults().stream()
                    .filter(result -> "FAILED".equals(result.getStatus()))
                    .collect(Collectors.toMap(
                        SoftwareCommitDto.DeviceCommitResult::getDeviceId,
                        result -> result.getErrorMessage() != null ? result.getErrorMessage() : "Commit RPC failed",
                        (left, right) -> left
                    ));
                markWorkflowStepFailures(workflows, DeviceTask.TaskType.COMMIT, failedDeviceErrors, failedTaskIds, false);
                String failedDevices = commitResponse.getResults().stream()
                    .filter(result -> "FAILED".equals(result.getStatus()))
                    .map(this::formatCommitFailure)
                    .collect(Collectors.joining(", "));
                throw new IllegalStateException(String.format(
                    "Commit RPC was not issued or failed for %d device(s): %s",
                    commitResponse.getFailedCount(),
                    failedDevices
                ));
            }

            log.info("✅ 批量提交任务已创建: {} 个设备", workflows.size());
        } catch (Exception e) {
            log.error("❌ Batch commit task creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Batch commit failed: " + e.getMessage(), e);
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 计算批次状态（基于 Workflows）
     * 
     * 逻辑优先级：
     * 1. 终态判断（FAILED, COMPLETED, COMPLETED_WITH_ERRORS）
     * 2. 执行中判断：
     *    - 自动模式: RUNNING
     *    - 手动模式: 根据 currentStep 判断
     *      - 有任务正在运行: DOWNLOADING/BACKING_UP/UPGRADING/COMMITTING
     *      - 所有当前步骤已完成: READY_TO_BACKUP/READY_TO_UPGRADE/READY_TO_COMMIT
     */
    private Batch.BatchStatus calculateBatchStatus(
        Batch batch,
        List<UpgradeWorkflow> workflows
    ) {
        if (workflows.isEmpty()) {
            return Batch.BatchStatus.CANCELLED;
        }

        // ⚠️ 注意：调用前必须先调用 workflowManagementService.computeAndSetWorkflowStatuses()
        long completedCount = workflows.stream()
            .filter(wf -> wf.isCompleted())
            .count();

        long failedCount = workflows.stream()
            .filter(wf -> wf.isFailed())
            .count();

        long runningCount = workflows.stream()
            .filter(wf -> !wf.isFinished())
            .count();

        List<UpgradeWorkflow> activeWorkflows = workflows.stream()
            .filter(wf -> !wf.isFailed())
            .collect(Collectors.toList());

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 1️⃣ 终态判断：优先级最高
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        if (batch.getExecutionMode() == Batch.ExecutionMode.MANUAL) {
            // ⭐ 手动模式：任一 workflow 失败，整批立即 FAILED，禁止继续推进
            if (failedCount > 0) {
                log.info("  终态判断: 手动模式存在 {} 个失败，批次状态 = FAILED", failedCount);
                return Batch.BatchStatus.FAILED;
            }
        } else {
            // ⭐ 自动模式：只有全部失败才 FAILED，部分失败允许其余设备继续推进
            if (failedCount == workflows.size()) {
                log.info("  终态判断: 自动模式全部 {} 个 workflow 失败，批次状态 = FAILED", failedCount);
                return Batch.BatchStatus.FAILED;
            }

            // ⭐ 自动模式：部分失败且其余都已结束，不允许 COMMIT，落为 COMPLETED_WITH_ERRORS
            if (failedCount > 0 && completedCount + failedCount == workflows.size()) {
                log.info("  终态判断: 自动模式部分失败且全部已结束，批次状态 = COMPLETED_WITH_ERRORS (success={}, failed={})",
                    completedCount, failedCount);
                return Batch.BatchStatus.COMPLETED_WITH_ERRORS;
            }
        }

        // 全部成功
        if (completedCount == workflows.size()) {
            log.info("  终态判断: 全部成功，批次状态 = COMPLETED");
            return Batch.BatchStatus.COMPLETED;
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 2️⃣ 执行中状态判断（自动模式和手动模式分别处理）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        // ⭐ 从未失败的 workflow 中获取 currentStep
        if (batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC) {
            Set<String> activeSteps = activeWorkflows.stream()
                .map(UpgradeWorkflow::getCurrentStep)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            if (activeSteps.size() > 1) {
                log.info("  自动模式检测到混合步骤，批次保持 RUNNING: batchId={}, activeSteps={}, activeCount={}, completedCount={}, runningCount={}",
                    batch.getBatchId(), activeSteps, activeWorkflows.size(), completedCount, runningCount);
                return Batch.BatchStatus.RUNNING;
            }
        }

        boolean allActivateSucceeded = !activeWorkflows.isEmpty()
            && activeWorkflows.size() == workflows.size()
            && activeWorkflows.stream()
                .allMatch(wf -> wf.getStatus() == UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS);
        if (allActivateSucceeded) {
            log.info("  ✅ 所有 workflow 均已激活成功，批次状态 = READY_TO_COMMIT");
            return Batch.BatchStatus.READY_TO_COMMIT;
        }

        String currentStep;
        try {
            currentStep = resolveCurrentStep(workflows);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("inconsistent currentStep values")) {
                log.warn("  ⚠️ 批次存在混合步骤，返回 RUNNING: batchId={}, message={}",
                    batch.getBatchId(), ex.getMessage());
                return Batch.BatchStatus.RUNNING;
            }
            throw ex;
        }
        
        if (currentStep == null) {
            log.warn("  ⚠️ 所有 workflow 都失败了，无法获取 currentStep");
            return Batch.BatchStatus.FAILED;
        }
        
        log.info("  calculateBatchStatus: 开始判断批次状态, currentStep={}, executionMode={}", 
            currentStep, batch.getExecutionMode());
        
        // ⭐⭐⭐ 关键修复：先判断当前步骤是否完成
        boolean allCurrentStepCompleted = workflowManagementService.areAllCurrentStepsCompleted(workflows);
        log.info("  calculateBatchStatus: allCurrentStepCompleted={}", allCurrentStepCompleted);
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 2.1️⃣ 如果当前步骤已完成，返回 READY_TO_XXX 状态
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if (allCurrentStepCompleted) {
            log.info("  → 当前步骤已完成，计算下一步");
            
        String nextStep = workflowManagementService.getNextEnabledStep(batch, currentStep);
            log.info("  → nextStep={}", nextStep);
            
        if (nextStep == null) {
            // ⭐ 特殊处理：如果当前步骤是 UPGRADE，且没有下一步，说明等待 COMMIT
            if ("UPGRADE".equalsIgnoreCase(currentStep)) {
                    if (batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC && failedCount > 0) {
                        log.info("  自动模式存在 {} 个失败 workflow，禁止进入 READY_TO_COMMIT，批次状态保持 RUNNING", failedCount);
                        return Batch.BatchStatus.RUNNING;
                    }
                    log.info("  ✅ UPGRADE 步骤已完成，等待 COMMIT (mode={})", 
                        batch.getExecutionMode());
                return Batch.BatchStatus.READY_TO_COMMIT;
            }
            
            // 其他情况：没有更多启用的步骤，批次完成
            // 例如：只启用了 DOWNLOAD，完成后直接 COMPLETED
            log.info("  ✅ 已完成所有启用的步骤: currentStep={}", currentStep);
            return Batch.BatchStatus.COMPLETED;
        }
        
            // 返回对应的 READY_TO_XXX 状态
            // ⭐ 区分手动和自动模式
            if (batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC) {
                // ⭐ 自动模式：统一返回 RUNNING（除了 READY_TO_COMMIT）
                if ("COMMIT".equalsIgnoreCase(nextStep)) {
                    if (failedCount > 0) {
                        log.info("  自动模式存在 {} 个失败 workflow，禁止进入 READY_TO_COMMIT，批次状态保持 RUNNING", failedCount);
                        return Batch.BatchStatus.RUNNING;
                    }
                    log.info("  → 返回 READY_TO_COMMIT (自动模式，等待确认提交)");
                    return Batch.BatchStatus.READY_TO_COMMIT;
                } else {
                    log.info("  → 返回 RUNNING (自动模式，当前步骤已完成，准备执行下一步: {})", nextStep);
                    return Batch.BatchStatus.RUNNING;
                }
            } else {
                // ⭐ 手动模式：返回细分的 READY_TO_XXX 状态
        switch (nextStep.toUpperCase()) {
            case "BACKUP":
                        log.info("  → 返回 READY_TO_BACKUP (手动模式)");
                return Batch.BatchStatus.READY_TO_BACKUP;
            case "UPGRADE":
                        log.info("  → 返回 READY_TO_UPGRADE (手动模式)");
                return Batch.BatchStatus.READY_TO_UPGRADE;
            case "COMMIT":
                        log.info("  → 返回 READY_TO_COMMIT (手动模式)");
                return Batch.BatchStatus.READY_TO_COMMIT;
            default:
                log.warn("  ⚠️ 未知的下一步类型: {}", nextStep);
                return Batch.BatchStatus.RUNNING;
                }
            }
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 2.2️⃣ 如果当前步骤未完成，判断是运行中还是未开始
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        log.info("  → 当前步骤未完成，检查是否有运行中的任务");
        
        boolean hasRunningTask = workflowManagementService.hasRunningTasks(workflows, currentStep);
        log.info("  → hasRunningTask={}", hasRunningTask);
        
        if (hasRunningTask) {
            // 有任务正在运行
            // ⭐ 特殊处理：COMMIT 步骤无论手动还是自动都显示 COMMITTING
            if ("COMMIT".equalsIgnoreCase(currentStep)) {
                log.info("  → 返回 COMMITTING (提交中)");
                return Batch.BatchStatus.COMMITTING;
            }
            
            // ⭐ 区分手动和自动模式
            if (batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC) {
                // ⭐ 自动模式：统一返回 RUNNING
                log.info("  → 返回 RUNNING (自动模式，任务运行中)");
                return Batch.BatchStatus.RUNNING;
            } else {
                // ⭐ 手动模式：返回细分的执行中状态
                switch (currentStep.toUpperCase()) {
                    case "DOWNLOAD":
                        log.info("  → 返回 DOWNLOADING (手动模式，下载中)");
                        return Batch.BatchStatus.DOWNLOADING;
                    case "BACKUP":
                        log.info("  → 返回 BACKING_UP (手动模式，备份中)");
                        return Batch.BatchStatus.BACKING_UP;
                    case "UPGRADE":
                        log.info("  → 返回 UPGRADING (手动模式，升级中)");
                        return Batch.BatchStatus.UPGRADING;
                    default:
                        log.warn("  ⚠️ 未知的步骤类型: {}, 返回 RUNNING", currentStep);
                        return Batch.BatchStatus.RUNNING;
                }
            }
        } else {
            // 任务未开始（NOT_START）
            // ⭐ 区分手动和自动模式
            if (batch.getExecutionMode() == Batch.ExecutionMode.AUTOMATIC) {
                // ⭐ 自动模式：返回 RUNNING（自动模式没有 READY_TO_PROCEED）
                log.warn("  ⚠️ 自动模式，任务未开始，返回 RUNNING");
                log.warn("  ⚠️ currentStep={}, workflowCount={}", currentStep, workflows.size());
                return Batch.BatchStatus.RUNNING;
            } else {
                // ⭐ 手动模式：返回 READY_TO_PROCEED
                log.warn("  ⚠️ 手动模式，任务未开始，返回 READY_TO_PROCEED");
                log.warn("  ⚠️ currentStep={}, workflowCount={}", currentStep, workflows.size());
                return Batch.BatchStatus.READY_TO_PROCEED;
            }
        }
    }
    
    /**
     * 公开的状态计算方法（供 UnifiedBatchService 调用）
     * 
     * @param batch 批次对象
     * @param workflows 工作流列表（必须已调用 computeAndSetWorkflowStatuses）
     * @return 计算的批次状态
     */
    public Batch.BatchStatus calculateBatchStatusPublic(Batch batch, List<UpgradeWorkflow> workflows) {
        return calculateBatchStatus(batch, workflows);
    }
    
    /**
     * 创建定时 DeviceTask（SCHEDULED 状态）
     * 
     * @param deviceId 设备ID
     * @param workflowId 工作流ID
     * @param batchId 批次ID
     * @param batchName 批次名称
     * @param taskType 任务类型（DOWNLOAD/BACKUP/UPGRADE）
     * @param scheduledTime 定时执行时间（毫秒）
     * @param batch 批次对象（用于获取配置信息）
     * @param deviceInfo 设备物理信息（可为null）
     * @return DeviceTask
     */
    /**
     * 创建 NOT_START 状态的 DeviceTask（MANUAL 模式使用）
     * 
     * @param deviceId 设备ID
     * @param workflowId 工作流ID
     * @param batchId 批次ID
     * @param batchName 批次名称
     * @param taskType 任务类型（DOWNLOAD/BACKUP/UPGRADE）
     * @param batch 批次对象
     * @param deviceInfo 设备信息
     * @return DeviceTask 实例
     */
    private DeviceTask createNotStartDeviceTask(
            String deviceId,
            String workflowId,
            String batchId,
            String batchName,
            String taskType,
            Batch batch,
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo) {
        
        // ✅ 可用性检查：检查设备是否有未完成任务（如有则抛异常）
        uniquenessService.checkAndGetAvailableTask(
            deviceId,
            DeviceTask.TaskType.valueOf(taskType),
            batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
        );
        
        // 设备空闲，创建新任务记录
        String taskId = java.util.UUID.randomUUID().toString();
        DeviceTask task = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.valueOf(taskType));
        log.debug("  ✅ 创建新任务: taskId={}", taskId);
        
        // ✅ 设置批次信息
        task.setBatchId(batchId);
        task.setBatchName(batchName);
        task.setWorkflowId(workflowId);
        task.setTaskInfoActionTime(batch.getBatchActionTime());
        
        // ✅ 设置设备基础信息（如果可用）
        if (deviceInfo != null) {
            task.setDeviceName(deviceInfo.getFriendlyName());
            task.setDeviceIp(deviceInfo.getIp());
            task.setVendorType(deviceInfo.getVendorType());
            task.setVendorName(deviceInfo.getVendorName());
            task.setCurrentVersion(deviceInfo.getSoftwareVersion());
            task.setPreviousVersion(deviceInfo.getSoftwareVersion());
            log.debug("  ✓ Device info set: name={}, ip={}, vendor={}/{}, currentVersion={}", 
                deviceInfo.getFriendlyName(), deviceInfo.getIp(), 
                deviceInfo.getVendorType(), deviceInfo.getVendorName(), 
                deviceInfo.getSoftwareVersion());
        }
        
        // ✅ 设置状态为 NOT_START（等待手动触发）
        task.setStatus(DeviceTask.TaskStatus.NOT_START);
        
        // ✅ 设置任务配置（根据任务类型）
        switch (taskType) {
            case "DOWNLOAD":
                task.setFilePath(batch.getFilePath());
                task.setSftpServerName(batch.getSftpServerName());
                break;
                
            case "BACKUP":
                task.setSftpServerName(batch.getSftpServerName());
                task.setBackupFilePath(batch.getBackupBasePath());
                break;
                
            case "UPGRADE":
                task.setFilePath(batch.getFilePath());
                task.setTargetVersion(batch.getTargetVersion());
                break;
        }
        
        // ✅ 设置重试次数
        task.setRetryCount(0);
        
        return task;
    }
    
    /**
     * 创建 SCHEDULED 状态的 DeviceTask（AUTO + SCHEDULED 模式使用）
     * 
     * @param deviceId 设备ID
     * @param workflowId 工作流ID
     * @param batchId 批次ID
     * @param batchName 批次名称
     * @param taskType 任务类型（DOWNLOAD/BACKUP/UPGRADE）
     * @param scheduledTime 定时执行时间
     * @param batch 批次对象
     * @param deviceInfo 设备信息
     * @return DeviceTask 实例
     */
    private DeviceTask createScheduledDeviceTask(
            String deviceId,
            String workflowId,
            String batchId,
            String batchName,
            String taskType,
            Long scheduledTime,
            Batch batch,
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo) {
        
        // ✅ 可用性检查：检查设备是否有未完成任务（如有则抛异常）
        uniquenessService.checkAndGetAvailableTask(
            deviceId,
            DeviceTask.TaskType.valueOf(taskType),
            batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
        );
        
        // 设备空闲，创建新任务记录
        String taskId = java.util.UUID.randomUUID().toString();
        DeviceTask task = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.valueOf(taskType));
        log.debug("  ✅ 创建新任务: taskId={}", taskId);
        
        // ✅ 设置批次信息
        task.setBatchId(batchId);
        task.setBatchName(batchName);
        task.setWorkflowId(workflowId);
        task.setTaskInfoActionTime(batch.getBatchActionTime());
        
        // ✅ 设置设备基础信息（如果可用）
        if (deviceInfo != null) {
            task.setDeviceName(deviceInfo.getFriendlyName());
            task.setDeviceIp(deviceInfo.getIp());
            task.setVendorType(deviceInfo.getVendorType());        // ⭐ 保存厂商类型
            task.setVendorName(deviceInfo.getVendorName());        // ⭐ 保存厂商名称
            task.setCurrentVersion(deviceInfo.getSoftwareVersion()); // ⭐ 保存当前软件版本
            task.setPreviousVersion(deviceInfo.getSoftwareVersion()); // ⭐ 保存升级前版本
            log.debug("  ✓ Device info set: name={}, ip={}, vendor={}/{}, currentVersion={}", 
                deviceInfo.getFriendlyName(), deviceInfo.getIp(), 
                deviceInfo.getVendorType(), deviceInfo.getVendorName(), 
                deviceInfo.getSoftwareVersion());
        }
        
        // ✅ 设置定时信息
        task.setStatus(DeviceTask.TaskStatus.SCHEDULED);
        task.setScheduledTime(scheduledTime);
        
        // ✅ 设置任务配置（根据任务类型）
        switch (taskType) {
            case "DOWNLOAD":
                task.setFilePath(batch.getFilePath());
                task.setSftpServerName(batch.getSftpServerName());
                break;
                
            case "BACKUP":
                task.setSftpServerName(batch.getSftpServerName());
                task.setBackupFilePath(batch.getBackupBasePath());
                break;
                
            case "UPGRADE":
                task.setFilePath(batch.getFilePath());
                task.setTargetVersion(batch.getTargetVersion());
                break;
        }
        
        // ✅ 设置重试次数（DeviceTask 只有 retryCount 字段）
        task.setRetryCount(0);
        
        // ⚠️ 注意：DeviceTask 没有 debug 和 maxRetryCount 字段
        // debug 模式通过 debugPayload 字段体现
        // maxRetryCount 在批次级别管理
        
        return task;
    }

    /**
     * 从当前 HTTP 请求获取用户名
     * 
     * @return 用户名，如果无法获取则返回 null
     */
    private String getCurrentUserFromRequest() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                javax.servlet.http.HttpServletRequest request = attributes.getRequest();
                // Gateway 使用 "user" header 传递用户名
                String user = request.getHeader("user");
                
                if (user != null && !user.trim().isEmpty()) {
                    return user;
                }
            }
        } catch (Exception e) {
            // 忽略异常（可能在非HTTP上下文中调用）
        }
        
        return null;  // ⭐ 无法获取时返回 null（不使用默认值）
    }
}

