package devicemaintenance.service;

import devicemaintenance.dto.DeviceBackupDto;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.Batch;
import devicemaintenance.helper.BatchHelper;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.utils.DeviceMaintenanceLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 设备备份服务
 * 支持单个设备和全设备备份功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceBackupService {

    private final DeviceTaskRepository deviceTaskRepository;
    private final BatchRepository batchRepository;
    private final DeviceMaintenanceStatusService maintenanceStatusService;
    private final TaskCreationHelper taskCreationHelper;
    private final RealServiceExecutionHelper executionHelper;
    private final TaskInfoNotificationService taskInfoNotificationService;
    private final DeviceTaskUniquenessService uniquenessService;
    private final DeviceAvailabilityService deviceAvailabilityService;
    private final RpcDebugHelper rpcDebugHelper;
    
    @Autowired(required = false)
    private NeMgrIntegrationService neMgrIntegrationService;
    
    @Autowired(required = false)
    private WorkflowManagementService workflowManagementService;
    
    /**
     * 批次任务执行线程池（统一线程池）
     * 用于并发下发所有类型的 RPC 操作，最大并发数20
     */
    @Autowired
    @Qualifier("batchTaskExecutor")
    private Executor batchTaskExecutor;


    /**
     * 单设备备份（内部接口，仅供批量升级等内部流程使用）
     * 使用独立事务避免批量创建时的锁冲突
     * 
     * @param request 备份请求
     * @param batchId 批次ID
     * @param batchActionTime 批次统一的 actionTime
     * @param batchName 批次名称
     */
    // ⚠️ 移除事务！失败就失败，不需要回滚！
    public DeviceBackupDto.BackupResponse backupSingleDevice(
            DeviceBackupDto.SingleDeviceBackupRequest request,
            String batchId,
            Long batchActionTime,
            String batchName,
            boolean debug) {
        DeviceMaintenanceLogContext.setIdentifiers(null, batchId, null, request.getDeviceId(), null, "BACKUP");
        DeviceMaintenanceLogContext.setPhase("API_IN", "start-backup");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [内部调用] 设备备份任务");
        log.info("  设备ID: {}", request.getDeviceId());
            log.info("  批次ID: {}", batchId);
        log.info("  批次名称: {}", batchName);
        log.info("  批次ActionTime: {}", batchActionTime);
        log.info("  备份文件: {}", request.getFilePath());
        log.info("  SFTP服务器: {}", request.getSftpServerName());

        // ✅ 预验证设备和SFTP服务器，并获取设备信息用于生成路径
        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;  // ⭐ 在外部声明
        String deviceFriendlyName = null;
        String deviceIp = null;
        if (request.getScheduledTime() == null) {
            long precheckStart = System.currentTimeMillis();
            DeviceMaintenanceLogContext.setPhase("PRECHECK", "validate-device-and-sftp");
            log.info("🔍 [Pre-validation] 验证设备和SFTP服务器...");
            try {
                deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(request.getDeviceId());
                deviceFriendlyName = deviceInfo.getFriendlyName();
                deviceIp = deviceInfo.getIp();
                log.info("  ✓ 设备验证通过: {} ({}) - IP: {}", deviceFriendlyName, request.getDeviceId(), deviceIp);
                
                if (request.getSftpServerName() != null) {
                    neMgrIntegrationService.getSftpServerById(request.getSftpServerName());
                    log.info("  ✓ SFTP服务器验证通过: {}", request.getSftpServerName());
                }
                log.info("precheck success elapsedMs={}", System.currentTimeMillis() - precheckStart);
            } catch (Exception e) {
                log.error("precheck failed elapsedMs={} error={}", System.currentTimeMillis() - precheckStart, e.getMessage());
                throw new RuntimeException("Pre-validation failed: " + e.getMessage(), e);
            }
        }

        // ✅ 如果提供了 basePath 但没有 filePath，自动生成完整路径
        if (request.getBasePath() != null && request.getFilePath() == null) {
            if (deviceFriendlyName == null) {
                // 如果是定时任务，还没有获取设备信息，现在获取
                try {
                    deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(request.getDeviceId());
                    deviceFriendlyName = deviceInfo.getFriendlyName();
                    deviceIp = deviceInfo.getIp();
                } catch (Exception e) {
                    log.error("❌ 获取设备信息失败，无法生成备份路径: {}", e.getMessage());
                    throw new RuntimeException("Failed to get device information: " + e.getMessage(), e);
                }
            }
            
            // 生成路径：basePath/deviceName/yyyyMMdd/HHmmss.db
            // 只替换文件系统非法字符：/ \ : * ? " < > |
            String sanitizedDeviceName = deviceFriendlyName.replaceAll("[/\\\\:*?\"<>|]", "-");
            String dateDir = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String timeStamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmmss"));
            String generatedFilePath = String.format("%s/%s/%s/%s.db", 
                request.getBasePath(), sanitizedDeviceName, dateDir, timeStamp);
            
            request.setFilePath(generatedFilePath);
            log.info("  ✅ 自动生成备份路径: {}", generatedFilePath);
            
            // ✅ 调用SFTP创建目录
            try {
                String dirPath = String.format("%s/%s/%s", request.getBasePath(), sanitizedDeviceName, dateDir);
                neMgrIntegrationService.createRemoteDirectory(request.getSftpServerName(), dirPath);
                log.info("  ✅ 远程目录已创建: {}", dirPath);
            } catch (Exception e) {
                log.warn("  ⚠️ 创建远程目录失败（可能已存在）: {}", e.getMessage());
                // 不抛异常，因为目录可能已经存在
            }
        }

        // 检查设备维护状态
        DeviceMaintenanceStatusService.MaintenanceStatusCheckResult statusCheck = 
            maintenanceStatusService.checkDeviceAvailableForTask(request.getDeviceId(), DeviceTask.TaskType.BACKUP);
        
        if (!statusCheck.isAvailable()) {
            throw new RuntimeException(statusCheck.getReason());
        }

        // 创建备份任务（单设备备份，workflowId 为 null）
        DeviceTask task = createBackupTask(request, batchId, batchName, null);
        
        if (deviceInfo != null) {
        if (deviceFriendlyName != null) {
            task.setDeviceName(deviceFriendlyName);
            log.info("  ✓ Device friendly name set: {}", deviceFriendlyName);
            }
            if (deviceIp != null) {
                task.setDeviceIp(deviceIp);
                log.info("  ✓ Device IP set: {}", deviceIp);
            }
            if (deviceInfo.getVendorType() != null) {
                task.setVendorType(deviceInfo.getVendorType());
                log.info("  ✓ Vendor type set: {}", deviceInfo.getVendorType());
            }
            if (deviceInfo.getVendorName() != null) {
                task.setVendorName(deviceInfo.getVendorName());
                log.info("  ✓ Vendor name set: {}", deviceInfo.getVendorName());
            }
            if (deviceInfo.getSoftwareVersion() != null) {
                task.setCurrentVersion(deviceInfo.getSoftwareVersion());
                if (task.getPreviousVersion() == null) {
                    task.setPreviousVersion(deviceInfo.getSoftwareVersion());
                }
                log.info("  ✓ Current version set: {}", deviceInfo.getSoftwareVersion());
            }
        }
        
        if (batchActionTime != null) {
            task.setTaskInfoActionTime(batchActionTime);
            log.info("  ✓ Batch ActionTime set: {}", batchActionTime);
        }
        
        DeviceMaintenanceLogContext.setPhase("TASK_PERSIST", "save-device-task");
        DeviceTask savedTask = deviceTaskRepository.save(task);
        DeviceMaintenanceLogContext.setTaskContext(savedTask);

        log.info("✅ 任务记录已保存到数据库");
        log.info("  任务ID: {}", savedTask.getTaskId());
        log.info("  初始状态: {}", savedTask.getStatus());

        // 只有非定时任务才执行
        if (taskCreationHelper.shouldExecuteImmediately(savedTask)) {
            log.info("🔧 执行模式: Real (真实neMgr RPC)");
            log.info("🐛 调试模式: {}", debug ? "开启" : "关闭");
            executeRealBackup(savedTask, debug);
            
            // ⚠️ 移除flush()调用，避免与外层事务冲突导致死锁
            // Spring会在事务结束时自动提交
            
            // ❌ 不再发送设备级通知！
            // 注意：这是内部调用，不发送批次级别通知
            // 批次级别通知由 SystemChangeNotificationListener 统一管理（3秒防抖）
        } else {
            log.info("📅 定时任务已创建，等待调度执行");
            log.info("  计划执行时间: {}", savedTask.getScheduledTime());
        }

        DeviceMaintenanceLogContext.setPhase("API_OUT", "start-backup");
        log.info("request completed finalStatus={}", savedTask.getStatus());
        DeviceMaintenanceLogContext.clearAll();
        return convertToResponse(savedTask);
    }

    /**
     * 创建或复用设备备份任务
     * 
     * ⭐ 两种模式：
     * 1. Workflow 内任务：复用现有任务记录（更新类型和状态）
     * 2. 独立批次任务：创建新任务记录
     */
    private DeviceTask createBackupTask(DeviceBackupDto.SingleDeviceBackupRequest request, String batchId, String batchName, String workflowId) {
        // ✅ 可用性检查：检查设备是否有未完成任务
        // 如果是 workflow 任务且前一步骤已完成，返回该任务供复用
        DeviceTask existingTask = uniquenessService.checkAndGetAvailableTask(
            request.getDeviceId(),
            DeviceTask.TaskType.BACKUP,
            batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
        );
                
        DeviceTask task;
        
        if (existingTask != null) {
            // ⭐ 复用模式：更新现有任务
            task = existingTask;
            log.info("  ♻️  复用现有任务: taskId={}, 前状态={}",
                task.getTaskId(), task.getStatus());

            // 更新任务类型和状态
            task.setTaskType(DeviceTask.TaskType.BACKUP);
            task.setStatus(DeviceTask.TaskStatus.PENDING);  // 重置为 PENDING
            task.setErrorMessage(null);  // 清除之前的错误信息
            task.setCompletedTime(null);  // 清除完成时间

            log.info("  ✅ 任务已更新为 BACKUP 类型");

        } else {
            // ⭐ 新建模式：创建新任务记录
            String taskId = taskCreationHelper.generateTaskId();
            task = new DeviceTask(taskId, request.getDeviceId(), DeviceTask.TaskType.BACKUP);
            log.info("  ✅ 创建新备份任务: taskId={}", taskId);
            
            // 设置批次ID和workflow ID
            task.setBatchId(batchId);
            task.setBatchName(batchName);
            if (workflowId != null) {
                task.setWorkflowId(workflowId);
                log.info("  ✓ 关联到工作流: workflowId={}", workflowId);
            }
        }

        // 设置备份文件路径（从 filePath 解析）
        if (request.getFilePath() != null) {
            String filePath = request.getFilePath();
            task.setBackupFilePath(filePath);
            
            // 解析出目录和文件名
            int lastSlash = filePath.lastIndexOf('/');
            if (lastSlash > 0) {
                String dirPath = filePath.substring(0, lastSlash);
                String fileName = filePath.substring(lastSlash + 1);
                task.setFilePath(dirPath);
                task.setBackupFileName(fileName);
                log.info("  ✓ 解析路径 - 目录: {}, 文件名: {}", dirPath, fileName);
            } else {
                task.setFilePath("/dbbackup");
                task.setBackupFileName(filePath);
                log.info("  ✓ 无目录分隔符，使用默认目录: /dbbackup, 文件名: {}", filePath);
            }
            log.info("  ✓ 完整路径(backupFilePath): {}", task.getBackupFilePath());
        }

        // 直接存储SFTP服务器name（如果提供了）
        if (request.getSftpServerName() != null) {
            task.setSftpServerName(request.getSftpServerName());
        }

        // ✅ 设置批次ID和批次名称（可能为null，这是正常的）
            task.setBatchId(batchId);
        task.setBatchName(batchName);

        // 设置其他字段
        task.setRetryCount(0);
        
        // 使用统一的定时任务配置逻辑
        taskCreationHelper.configureTaskScheduling(task, request.getScheduledTime());

        return task;
    }

    /**
     * 执行真实备份（调用nemgr微服务）
     */
    @Async("taskSchedulerExecutor")
    private void executeRealBackup(DeviceTask task, boolean debug) {
        DeviceMaintenanceLogContext.setTaskContext(task);
        DeviceMaintenanceLogContext.setPhase("RPC_SEND", "database-backup");
        long rpcStart = System.currentTimeMillis();
        log.info("rpc dispatch start sftpServer={} backupFilePath={} debug={}", 
                task.getSftpServerName(), task.getBackupFilePath(), debug);
        
        try {
            executionHelper.updateTaskToRunning(task);
            
            // 调用完整版本的databaseBackup（包含SFTP信息和debug支持）
            NeMgrIntegrationService.RpcResult result = neMgrIntegrationService.databaseBackup(
                task.getDeviceId(),          // nodeId (Site-xxx#Ne-xxx)
                task.getSftpServerName(),    // sftpServerName（实际是SFTP服务器的name值）
                task.getBackupFilePath(),    // 完整备份文件路径
                debug                        // 是否启用调试模式
            );
            
            // ✅ 保存 debug payload 并持久化（统一方法）
            if (result != null) {
                rpcDebugHelper.saveDebugPayloadAndPersist(task, result.getDebugPayload(), debug);
            }
            
            // ✅ 检查RPC响应：验证命令是否成功下发
            if (result == null || result.getOutput() == null) {
                log.error("rpc ack invalid elapsedMs={} reason=nullOutput", System.currentTimeMillis() - rpcStart);
                // RPC调用失败（返回null），标记为失败
                executionHelper.completeTaskFailure(task, "Device Backup", "RPC call failed, returned null", 
                    result != null ? result.getDebugPayload() : null, null, debug);
            } else {
                // ✅ 检查 RPC 响应的 result 字段
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput output = 
                    (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput) result.getOutput();
                String rpcResult = output.getResult();
                
                // ✅ 使用统一的失败判断方法
                if (executionHelper.isFailureResult(rpcResult)) {
                    // ❌ RPC 返回失败
                    log.error("rpc ack failed elapsedMs={} result={}", System.currentTimeMillis() - rpcStart, rpcResult);
                    executionHelper.completeTaskFailure(task, "Device Backup", "RPC execution failed, result: " + rpcResult, 
                        result.getDebugPayload(), output, debug);
                } else {
                    // ✅ RPC调用成功，命令已下发，状态保持为RUNNING
                    // 最终状态将由 Kafka 异步通知更新
                    DeviceMaintenanceLogContext.setPhase("RPC_ACK", "database-backup");
                    log.info("rpc ack success elapsedMs={} result={}", System.currentTimeMillis() - rpcStart, rpcResult);
                    DeviceMaintenanceLogContext.setPhase("ASYNC_WAIT", "system-change");
                    log.info("waiting for async notification timeoutManagedByPolling=true");
                }
            }
            
        } catch (Exception e) {
            // ✅ 异常情况下无法获取完整的 debugPayload
            executionHelper.handleTaskError(task, "Device Backup", e, null);
        } finally {
            DeviceMaintenanceLogContext.clearPhase();
        }
    }

    /**
     * 执行定时任务（供调度器调用）
     * 
     * @param task 待执行的任务（状态已更新为PENDING）
     */
    public void executeScheduledTask(DeviceTask task) {
        log.info("执行定时备份任务: taskId={}, deviceId={}", task.getTaskId(), task.getDeviceId());
        
        // ⭐ 确定 debug 开关（从 Batch 读取）
        boolean debug = false;
        if (task.getBatchId() != null) {
            Batch batch = batchRepository.findById(task.getBatchId()).orElse(null);
            if (batch != null) {
                debug = batch.getDebug();
                log.debug("从 Batch 读取 debug 开关: batchId={}, debug={}", task.getBatchId(), debug);
            }
        }
        
        // ⭐ 生成备份文件路径（使用实际执行时间）
        if (task.getBackupFilePath() == null) {
            try {
                String basePath = task.getFilePath();  // 创建时保存的 basePath
                if (basePath == null || basePath.isEmpty()) {
                    throw new IllegalArgumentException("basePath is not set");
                }
                
                // 获取设备名称（用于目录名）
                String deviceName = task.getDeviceName();
                String sanitizedDeviceName = (deviceName != null && !deviceName.isEmpty()) 
                    ? sanitizeDirectoryName(deviceName)
                    : sanitizeDirectoryName(task.getDeviceId());
                
                // 生成路径：basePath/device_name/yyyyMMdd/yyyyMMdd_HHmmss.db
                String dateDir = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                String fileName = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".db";
                
                String directoryPath = String.format("%s/%s/%s", basePath, sanitizedDeviceName, dateDir);
                String backupFilePath = directoryPath + "/" + fileName;

                task.setBackupFilePath(backupFilePath);
                task.setBackupFileName(fileName);
                deviceTaskRepository.save(task);

                log.info("  ✓ 生成备份文件路径: {}", backupFilePath);

                // ⭐ 创建SFTP目录（与立即执行保持一致）
                try {
                    neMgrIntegrationService.createRemoteDirectory(
                        task.getSftpServerName(),
                        directoryPath
                    );
                    log.info("  ✓ SFTP目录已创建: {}", directoryPath);
                } catch (Exception dirEx) {
                    log.error("  ❌ SFTP目录创建失败: {}", dirEx.getMessage());
                    executionHelper.handleTaskError(task, "Device Backup", dirEx, null);
                    return;
                }
            } catch (Exception e) {
                log.error("  ❌ 生成备份文件路径失败: {}", e.getMessage());
                executionHelper.handleTaskError(task, "Device Backup", e, null);
                return;
            }
        }

        log.info("使用真实服务执行定时备份任务: taskId={}, backupFilePath={}, debug={}",
            task.getTaskId(), task.getBackupFilePath(), debug);
        executeRealBackup(task, debug);
        
        // ❌ 不再发送设备级通知！
        // 批次任务通过 SystemChangeNotificationListener 的批次级防抖通知来更新 TaskInfo
    }

    /**
     * 批量设备备份（指定设备列表）
     * 
     * 新规则：
     * - basePath: 只需指定根路径（如 "/dbbackup"）
     * - 自动生成：basePath/device_name/yyyyMMdd/HHmmss.db
     * - 会自动调用SFTP mkdir创建目录
     */
    /**
     * 批量备份（外部调用，会发送TaskInfo通知）
     */
    public DeviceBackupDto.BatchBackupSummary batchBackupDevices(DeviceBackupDto.BatchBackupRequest request) {
        return batchBackupDevices(request, false);  // 外部调用，发送通知
    }
    
    /**
     * 批量备份（支持外部调用和内部调用）
     * @param request 备份请求
     * @param isInternalCall 是否为内部调用（true=不发送TaskInfo通知，false=发送通知）
     */
    public DeviceBackupDto.BatchBackupSummary batchBackupDevices(
            DeviceBackupDto.BatchBackupRequest request,
            boolean isInternalCall) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [批量备份] 创建批量设备备份任务（并发处理）");
        log.info("  批次名称: {}", request.getBatchName());
        log.info("  设备数量: {}", request.getDeviceIds().size());
        log.info("  备份根路径: {} (自动生成完整路径)", request.getBasePath());
        log.info("  SFTP服务器: {} (所有设备使用同一SFTP)", request.getSftpServerName());
        log.info("  最大并发数: 20 (受线程池控制)");
        
        // ✅ 严格模式 Pre-validation（三项检查：SFTP + 设备存在 + 设备可用）
        // 注意：这里会抛出异常如果验证失败，所以后续代码不会执行
        java.util.Map<String, devicemaintenance.integration.NeMgrIntegrationService.DevicePhysicalInfo> deviceInfoMap =
            deviceAvailabilityService.validateDevicesAvailableStrict(
                request.getDeviceIds(), 
                DeviceTask.TaskType.BACKUP,
                request.getSftpServerName(),
                "批量备份"
            );
        
        // 定时执行检查
        if (request.getScheduledTime() != null) {
            log.info("  ⏰ 定时执行时间: {}", request.getScheduledTime());
            if (request.getScheduledTime() < System.currentTimeMillis()) {
                throw new IllegalArgumentException("Scheduled time must not be earlier than current time");
            }
        } else {
            log.info("  🚀 执行模式: 立即执行");
        }
        
        // ⭐ 判断是否为批量模式（基于batchName是否提供）
        boolean isBatchMode = org.springframework.util.StringUtils.hasText(request.getBatchName());
        // ⭐ 工作流内部调用：使用提供的 batchId（不创建新Batch）
        boolean isWorkflowCall = org.springframework.util.StringUtils.hasText(request.getBatchId());
        
        final String batchName = request.getBatchName();
        
        // ✅ 检查批次名称唯一性（仅批量模式且非工作流内部调用时检查）
        if (isBatchMode && !isWorkflowCall && batchName != null) {
            if (batchRepository.existsByBatchName(batchName)) {
                throw new IllegalArgumentException("Batch name already exists: " + batchName);
            }
        }
        final String batchId;
        final Long batchActionTime;
        final java.util.Map<String, String> workflowIds = request.getWorkflowIds();
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 批量备份模式检测:");
        log.info("  isBatchMode: {}", isBatchMode);
        log.info("  isWorkflowCall: {}", isWorkflowCall);
        log.info("  batchName: {}", request.getBatchName());
        log.info("  request.batchId: {}", request.getBatchId());
        log.info("  request.workflowIds: {}", request.getWorkflowIds());
        
        if (isWorkflowCall) {
            // 工作流内部调用：使用现有 batchId，不创建新 Batch
            batchId = request.getBatchId();
            batchActionTime = null;  // 不需要 actionTime（不发送通知）
            log.info("  🔄 工作流内部调用: batchId={}, workflowCount={}", batchId, workflowIds != null ? workflowIds.size() : 0);
        } else if (isBatchMode) {
            // 批量模式：生成批次ID和统一的 actionTime
            batchId = java.util.UUID.randomUUID().toString();
            batchActionTime = System.currentTimeMillis();
            
            log.info("  📦 批量模式: batchName={}", batchName);
            log.info("  批次ID: {}", batchId);
            log.info("  批次ActionTime: {}", batchActionTime);
        } else {
            batchId = null;
            batchActionTime = null;
            log.info("  🔧 单设备模式: 不创建Batch对象");
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        Batch batch = null;
        
        // ⭐ 如果是定时任务，只创建任务记录，不立即执行
        if (request.getScheduledTime() != null) {
            return createScheduledBatchBackup(request, batchId, batchActionTime, batchName, isInternalCall);
        }
        
        // ⭐ 立即执行：如果是批量模式（且不是工作流内部调用），先创建并保存 Batch 实体
        if (isBatchMode && !isWorkflowCall) {
            BatchHelper.BatchCreationContext context = new BatchHelper.BatchCreationContext();
            context.setBasePath(request.getBasePath());
            context.setSftpServerName(request.getSftpServerName());
            context.setRemark(request.getRemark());
            context.setDebug(request.getDebug());
            
            batch = BatchHelper.createBatch(
                batchId,
                batchName,
                batchActionTime,
                Batch.BatchType.BACKUP,
                request.getDeviceIds().size(),
                Batch.BatchStatus.RUNNING,  // 立即执行，初始状态为 RUNNING
                context
            );
            batchRepository.save(batch);
            
            log.info("  ✅ 批次记录已创建: batchId={}, status=RUNNING", batchId);
        }
        
        // ✅ 立即执行：使用CompletableFuture + 线程池实现并发处理
        List<CompletableFuture<DeviceBackupDto.DeviceBackupResult>> futures = new ArrayList<>();
        
        for (String deviceId : request.getDeviceIds()) {
            CompletableFuture<DeviceBackupDto.DeviceBackupResult> future = CompletableFuture.supplyAsync(() -> {
                // ⭐ 获取该设备对应的 workflowId（如果是工作流内部调用）
                String workflowId = (workflowIds != null) ? workflowIds.get(deviceId) : null;
                return processDeviceBackup(deviceId, request, batchId, batchActionTime, batchName, workflowId);
            }, batchTaskExecutor); // ✅ 使用统一线程池，最大并发数20
            
            futures.add(future);
        }
        
        log.info("📤 已提交 {} 个并发备份任务到线程池", futures.size());
        
        // 等待所有任务完成（受线程池大小限制并发数）
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join(); // 阻塞等待所有任务完成
        
        // 收集所有结果
        List<DeviceBackupDto.DeviceBackupResult> deviceResults = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        // ⭐ 如果是批量模式 且 不是内部调用，查询所有设备任务并发送 TaskInfo 批次通知
        if (isBatchMode && !isInternalCall) {
            List<DeviceTask> deviceTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
            
            try {
                // ⭐ 传入批次创建者，保证用户一致性
                String createdBy = (batch != null) ? batch.getCreatedBy() : null;
                taskInfoNotificationService.notifyBatchTaskStarted(
                    batchId,
                    batchName,
                    DeviceTask.TaskType.BACKUP,
                    deviceTasks,
                    batchActionTime,
                    createdBy
                );
                log.info("📨 已发送批次级别 TaskInfo 通知");
            } catch (Exception e) {
                log.warn("发送批次TaskInfo通知失败（不影响任务执行）: {}", e.getMessage());
            }
        }
        
        // 统计结果
        int pendingCount = (int) deviceResults.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        int runningCount = (int) deviceResults.stream().filter(r -> "RUNNING".equals(r.getStatus())).count();
        int completedCount = (int) deviceResults.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count();
        int failedCount = (int) deviceResults.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        
        // ⭐ 检查是否有失败：记录日志，不抛异常（让调用方通过 DeviceTask 状态判断）
        if (failedCount > 0 && runningCount == 0 && completedCount == 0 && pendingCount == 0) {
            // 全部失败
            String firstError = deviceResults.stream()
                .filter(r -> r.getErrorMessage() != null)
                .map(DeviceBackupDto.DeviceBackupResult::getErrorMessage)
                .findFirst()
                .orElse("未知错误");
            
            log.error("❌ 批量备份全部失败: failedCount={}, 第一个错误: {}", failedCount, firstError);
            // ✅ 不抛异常，让 DeviceTask、Workflow、Batch 状态自动传播失败信息
        }
        
        // ⭐ 如果是批量模式，更新 Batch 状态
        if (isBatchMode) {
            Batch.BatchStatus finalStatus;
            if (failedCount == 0 && runningCount == 0 && pendingCount == 0) {
                // 全部完成
                finalStatus = Batch.BatchStatus.COMPLETED;
            } else if (failedCount > 0 && runningCount == 0 && completedCount == 0 && pendingCount == 0) {
                // 全部失败
                finalStatus = Batch.BatchStatus.FAILED;
            } else if (failedCount > 0 && completedCount > 0 && runningCount == 0 && pendingCount == 0) {
                // 部分成功/失败（全部已结束）
                finalStatus = Batch.BatchStatus.COMPLETED_WITH_ERRORS;
            } else {
                // 还在运行中
                finalStatus = Batch.BatchStatus.RUNNING;
            }
            
            batch.setStatus(finalStatus);
            batchRepository.save(batch);
            
            log.info("  ✅ 批次状态已更新: status={}", finalStatus);
        }
        
        // 构建批量响应
        DeviceBackupDto.BatchBackupSummary summary = new DeviceBackupDto.BatchBackupSummary();
        summary.setBatchId(batchId);  // ✅ 添加 batchId
        summary.setBatchName(batchName);
        summary.setBasePath(request.getBasePath());
        summary.setSftpServerName(request.getSftpServerName());
        summary.setTotalDevices(request.getDeviceIds().size());
        summary.setPendingCount(pendingCount);
        summary.setRunningCount(runningCount);
        summary.setCompletedCount(completedCount);
        summary.setFailedCount(failedCount);
        summary.setStatus(failedCount == 0 ? "SUCCESS" : (runningCount + completedCount == 0 ? "FAILED" : "PARTIAL_SUCCESS"));
        summary.setRemark(request.getRemark());
        summary.setCreatedTime(java.time.LocalDateTime.now());
        summary.setUpdatedTime(java.time.LocalDateTime.now());
        summary.setDeviceResults(deviceResults);
        
        log.info("✅ 批量备份任务创建完成（并发处理）");
        log.info("  成功: {}, 失败: {}", runningCount + completedCount, failedCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // 打印可读格式的结果摘要（设备操作映射）
        log.info("📊 设备备份结果摘要（设备→操作→状态映射）:");
        for (DeviceBackupDto.DeviceBackupResult result : deviceResults) {
            log.info("  {} → 数据库备份 → {} [任务ID: {}, 文件: {}]", 
                result.getDeviceName() != null ? result.getDeviceName() : result.getDeviceId(),
                result.getStatus(),
                result.getTaskId() != null ? result.getTaskId() : "N/A",
                result.getBackupFileName());
        }
        
        return summary;
    }

    /**
     * 处理单个设备的备份（线程池中执行）
     * 
     * 新逻辑：
     * 1. 获取设备友好名称
     * 2. 生成路径：basePath/device_name/yyyyMMdd/HHmmss.db
     * 3. 调用 SFTP mkdir 创建目录
     * 4. 执行备份
     * 
     * @param deviceId 设备ID
     * @param batchRequest 批量备份请求
     * @param batchId 批次ID
     * @param batchActionTime 批次统一的 actionTime
     */
    private DeviceBackupDto.DeviceBackupResult processDeviceBackup(
            String deviceId, 
            DeviceBackupDto.BatchBackupRequest batchRequest,
            String batchId,
            Long batchActionTime,
            String batchName,
            String workflowId) {  // ⭐ 新增 workflowId 参数
        String deviceName = null;
        String deviceIp = null;
        String sanitizedDeviceName = null;
        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;  // ⭐ 保存完整的设备信息
        
        try {
            // 1. 获取设备友好名称和IP
            try {
                deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                deviceName = deviceInfo.getFriendlyName();
                deviceIp = deviceInfo.getIp();
                
                // 替换非法字符为 -
                sanitizedDeviceName = sanitizeDirectoryName(deviceName);
                log.debug("  设备名称: {} → 目录名: {}, IP: {}", deviceName, sanitizedDeviceName, deviceIp);
            } catch (Exception e) {
                log.warn("  ⚠️ 无法获取设备名称: {}，使用设备ID", deviceId);
                // 如果无法获取设备名称，使用设备ID
                sanitizedDeviceName = sanitizeDirectoryName(deviceId);
            }
            
            // 2. 生成路径：basePath/device_name/yyyyMMdd/yyyyMMdd_HHmmss.db
            String dateDir = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".db";
            
            // 完整的目录路径（不包含文件名）
            String directoryPath = String.format("%s/%s/%s",
                batchRequest.getBasePath(),
                sanitizedDeviceName,
                dateDir);
            
            // 完整的文件路径（包含文件名）
            String backupFilePath = directoryPath + "/" + fileName;
            
            log.debug("  生成备份路径: {}", backupFilePath);
            
            // 3. 创建单个设备的备份请求
            DeviceBackupDto.SingleDeviceBackupRequest backupRequest = new DeviceBackupDto.SingleDeviceBackupRequest();
            backupRequest.setDeviceId(deviceId);
            backupRequest.setFilePath(backupFilePath);  // 完整路径+文件名
            backupRequest.setSftpServerName(batchRequest.getSftpServerName());
            
            // 4. 调用 SFTP mkdir 创建目录（自动创建多层目录）
            try {
                neMgrIntegrationService.createRemoteDirectory(
                    batchRequest.getSftpServerName(),
                    directoryPath
                );
                log.debug("  ✓ SFTP目录已创建: {}", directoryPath);
            } catch (Exception e) {
                log.error("  ✗ SFTP目录创建失败: {}", e.getMessage());
                
                // ⭐ 目录创建失败：先创建 DeviceTask，再标记为失败
                DeviceTask task = uniquenessService.checkAndGetAvailableTask(
                    deviceId,
                    DeviceTask.TaskType.BACKUP,
                    batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
                );
                
                if (task == null) {
                    String taskId = taskCreationHelper.generateTaskId();
                    task = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.BACKUP);
                }
                
                // 设置任务基本信息
                task.setBatchId(batchId);
                task.setBatchName(batchName);
                if (workflowId != null) {
                    task.setWorkflowId(workflowId);
                }
                
                // ⭐ 使用统一方法设置所有设备信息
                taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);
                
                task.setFilePath(directoryPath);
                task.setBackupFilePath(backupFilePath);
                task.setBackupFileName(fileName);
                task.setSftpServerName(batchRequest.getSftpServerName());
                task.setTaskInfoActionTime(batchActionTime);
                
                // 标记为失败
                task.setStatus(DeviceTask.TaskStatus.FAILED);
                task.setErrorMessage("SFTP directory creation failed: " + e.getMessage());
                task.setCompletedTime(LocalDateTime.now());
                deviceTaskRepository.save(task);
                
                log.info("  ✅ DeviceTask 已创建并标记为 FAILED: taskId={}", task.getTaskId());
                
                // ⭐ 手动触发 Workflow 和 Batch 状态更新（因为没有 Kafka 通知）
                if (workflowId != null) {
                    try {
                        log.info("  🔄 触发 Workflow 状态更新: workflowId={}", workflowId);
                        workflowManagementService.updateWorkflowStatus(workflowId);
                        
                        // 获取 batchId 并更新 Batch 状态
                        if (batchId != null) {
                            log.info("  🔄 触发 Batch 状态更新: batchId={}", batchId);
                            updateBatchStatusAfterTaskFailure(batchId);
                        }
                    } catch (Exception updateEx) {
                        log.error("  ⚠️ 更新 Workflow/Batch 状态失败", updateEx);
                    }
                }
                
                // 返回失败结果
                DeviceBackupDto.DeviceBackupResult failResult = new DeviceBackupDto.DeviceBackupResult();
                failResult.setDeviceId(deviceId);
                failResult.setDeviceName(deviceName);
                failResult.setTaskId(task.getTaskId());
                failResult.setBackupFileName(fileName);
                failResult.setStatus("FAILED");
                failResult.setRpcResult(null);
                failResult.setErrorMessage("SFTP directory creation failed: " + e.getMessage());
                failResult.setCreatedTime(task.getCreatedTime());
                return failResult;
            }
            
            // 5. 执行备份（传入 batchId, batchActionTime, batchName 和 workflowId）
            DeviceBackupDto.BackupResponse response = backupSingleDeviceWithBatch(
                backupRequest, batchId, batchActionTime, batchName, workflowId,
                batchRequest.getDebug() != null ? batchRequest.getDebug() : false);
            
            // 构建设备备份结果（设备名 + RPC返回结果）
            DeviceBackupDto.DeviceBackupResult result = new DeviceBackupDto.DeviceBackupResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setTaskId(response.getTaskId());
            result.setBackupFileName(fileName);  // 只返回文件名部分
            result.setStatus(response.getStatus());
            result.setRpcResult("Backup task created successfully");
            result.setErrorMessage(response.getErrorMessage());
            result.setCreatedTime(response.getCreatedTime());
            
            log.debug("  ✓ [{}] 备份文件: {} - 任务ID: {}", 
                deviceName != null ? deviceName : deviceId,
                fileName,
                response.getTaskId());
            
            return result;
            
        } catch (Exception e) {
            log.error("  ✗ [{}] 备份任务创建失败: {}", 
                deviceName != null ? deviceName : deviceId,
                e.getMessage());
            
            DeviceBackupDto.DeviceBackupResult result = new DeviceBackupDto.DeviceBackupResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setTaskId(null);
            result.setBackupFileName(null);
            result.setStatus("FAILED");
            result.setRpcResult(null);
            result.setErrorMessage(e.getMessage());
            result.setCreatedTime(java.time.LocalDateTime.now());
            
            return result;
        }
    }
    
    /**
     * 清理目录名中的非法字符
     * 将不适合作为目录名的字符替换为 -
     */
    /**
     * 单设备备份（批次模式，带统一的 actionTime）
     * 
     * @param request 备份请求
     * @param batchId 批次ID
     * @param batchActionTime 批次统一的 actionTime
     * @return 备份响应
     */
    private DeviceBackupDto.BackupResponse backupSingleDeviceWithBatch(
            DeviceBackupDto.SingleDeviceBackupRequest request,
            String batchId,
            Long batchActionTime,
            String batchName,
            String workflowId,  // ⭐ 新增 workflowId 参数
            boolean debug) {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [批次子任务] 设备备份任务");
        log.info("  设备ID: {}", request.getDeviceId());
        log.info("  批次ID: {}", batchId);
        log.info("  批次名称: {}", batchName);
        log.info("  批次ActionTime: {}", batchActionTime);
        log.info("  备份文件: {}", request.getFilePath());
        
        // 验证设备和SFTP（预验证）
        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;
        try {
            deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(request.getDeviceId());
            log.info("  ✓ 设备验证通过: {} ({}) - IP: {}", 
                deviceInfo.getFriendlyName(), request.getDeviceId(), deviceInfo.getIp());
        } catch (Exception e) {
            log.error("❌ 设备验证失败: {}", e.getMessage());
            throw new RuntimeException("Pre-validation failed: " + e.getMessage(), e);
        }
        
        // 检查设备维护状态
        DeviceMaintenanceStatusService.MaintenanceStatusCheckResult statusCheck = 
            maintenanceStatusService.checkDeviceAvailableForTask(
                request.getDeviceId(), DeviceTask.TaskType.BACKUP);
        
        if (!statusCheck.isAvailable()) {
            throw new RuntimeException(statusCheck.getReason());
        }
        
        // 创建备份任务（传递 workflowId）
        DeviceTask task = createBackupTask(request, batchId, batchName, workflowId);
        
        // ⭐ 使用统一方法设置所有设备信息
        taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);
        
        // ⭐ 设置批次统一的 actionTime
        task.setTaskInfoActionTime(batchActionTime);
        
        // 保存任务
        DeviceTask savedTask = deviceTaskRepository.save(task);
        log.info("✅ 任务记录已保存到数据库");
        log.info("  任务ID: {}", savedTask.getTaskId());
        log.info("  TaskInfo ActionTime: {}", batchActionTime);
        
        // 执行备份（立即执行）
        log.info("🔧 执行模式: Real (真实neMgr RPC)");
        executeRealBackup(savedTask, debug);
        
        // ❌ 不再发送设备级通知！
        // 批次任务通过 SystemChangeNotificationListener 的批次级防抖通知来更新 TaskInfo
        DeviceTask updatedTask = deviceTaskRepository.findById(savedTask.getTaskId()).orElse(savedTask);
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return convertToResponse(updatedTask);
    }

    private String sanitizeDirectoryName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        // 替换以下字符为 -: / \ : * ? " < > |  以及空格
        return name.replaceAll("[/\\\\:*?\"<>|\\s]+", "-");
    }

    /**
     * 转换为响应DTO
     */
    private DeviceBackupDto.BackupResponse convertToResponse(DeviceTask task) {
        DeviceBackupDto.BackupResponse response = new DeviceBackupDto.BackupResponse();
        response.setTaskId(task.getTaskId());
        response.setBatchId(task.getBatchId());
        response.setDeviceId(task.getDeviceId());
        response.setDeviceName(task.getDeviceName());
        response.setTaskType(task.getTaskType().name());
        response.setStatus(task.getStatus().name());
        response.setBackupPath(task.getFilePath());
        response.setBackupFileName(task.getBackupFileName());
        response.setSftpServerName(task.getSftpServerName());
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedTime(task.getCreatedTime());
        response.setUpdatedTime(task.getUpdatedTime());
        response.setStartedTime(task.getStartedTime());
        response.setCompletedTime(task.getCompletedTime());
        return response;
    }

    /**
     * 获取批量备份状态汇总
     */
    public DeviceBackupDto.BatchBackupSummary getBatchBackupStatus(String batchId) {
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
        if (tasks.isEmpty()) {
            throw new RuntimeException("Batch does not exist: " + batchId);
        }

        long pendingCount = tasks.stream().filter(t -> DeviceTask.TaskStatus.PENDING.equals(t.getStatus())).count();
        long runningCount = tasks.stream().filter(t -> DeviceTask.TaskStatus.RUNNING.equals(t.getStatus())).count();
        long completedCount = tasks.stream().filter(t -> DeviceTask.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        long failedCount = tasks.stream().filter(t -> DeviceTask.TaskStatus.FAILED.equals(t.getStatus())).count();

        String overallStatus;
        if (completedCount == tasks.size()) {
            overallStatus = "COMPLETED";
        } else if (failedCount > 0 && (completedCount + failedCount) == tasks.size()) {
            overallStatus = "PARTIAL_FAILED";
        } else if (runningCount > 0) {
            overallStatus = "RUNNING";
        } else {
            overallStatus = "PENDING";
        }

        DeviceTask firstTask = tasks.get(0);
        DeviceBackupDto.BatchBackupSummary summary = new DeviceBackupDto.BatchBackupSummary();
        // ✅ 批量备份状态查询也不使用batchId
        summary.setBatchName("批量备份_" + batchId.substring(0, 8));
        summary.setTotalDevices(tasks.size());
        summary.setPendingCount((int) pendingCount);
        summary.setRunningCount((int) runningCount);
        summary.setCompletedCount((int) completedCount);
        summary.setFailedCount((int) failedCount);
        summary.setStatus(overallStatus);
        summary.setCreatedTime(firstTask.getCreatedTime());
        summary.setUpdatedTime(LocalDateTime.now());

        // 转换任务列表为DeviceBackupResult
        List<DeviceBackupDto.DeviceBackupResult> deviceResults = tasks.stream()
            .map(task -> {
                DeviceBackupDto.DeviceBackupResult result = new DeviceBackupDto.DeviceBackupResult();
                result.setDeviceId(task.getDeviceId());
                result.setDeviceName(task.getDeviceName());
                result.setTaskId(task.getTaskId());
                result.setBackupFileName(task.getBackupFileName());
                result.setStatus(task.getStatus().name());
                result.setRpcResult(task.getStatus() == DeviceTask.TaskStatus.COMPLETED ? "Backup completed" : "Backup in progress");
                result.setErrorMessage(task.getErrorMessage());
                result.setCreatedTime(task.getCreatedTime());
                return result;
            })
            .collect(Collectors.toList());
        summary.setDeviceResults(deviceResults);

        return summary;
    }
    
    /**
     * 创建定时批量备份任务
     */
    private DeviceBackupDto.BatchBackupSummary createScheduledBatchBackup(
            DeviceBackupDto.BatchBackupRequest request,
            String batchId,
            Long batchActionTime,
            String batchName,
            boolean isInternalCall) {
        
        // 判断是否为批量模式
        boolean isBatchMode = org.springframework.util.StringUtils.hasText(batchName);
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("⏰ [定时备份] 创建定时批量备份任务");
        if (isBatchMode) {
            log.info("  批次ID: {}", batchId);
            log.info("  批次名称: {}", batchName);
        } else {
            log.info("  🔧 单设备模式（不创建Batch）");
        }
        log.info("  定时执行时间: {}", request.getScheduledTime());
        log.info("  设备数量: {}", request.getDeviceIds().size());
        
        // 如果是批量模式，创建 Batch 记录
        if (isBatchMode) {
            BatchHelper.BatchCreationContext context = new BatchHelper.BatchCreationContext();
            context.setBasePath(request.getBasePath());
            context.setSftpServerName(request.getSftpServerName());
            context.setRemark(request.getRemark());
            context.setDebug(request.getDebug());
            context.setScheduledTime(request.getScheduledTime());
            
            Batch batch = BatchHelper.createBatch(
                batchId,
                batchName,
                batchActionTime,
                Batch.BatchType.BACKUP,
                request.getDeviceIds().size(),
                Batch.BatchStatus.SCHEDULED,
                context
            );
            batchRepository.save(batch);
            
            log.info("  ✅ 批次记录已创建: status=SCHEDULED");
        }
        
        // 为每个设备创建 SCHEDULED 状态的 DeviceTask
        List<DeviceBackupDto.DeviceBackupResult> deviceResults = new ArrayList<>();
        
        for (String deviceId : request.getDeviceIds()) {
            try {
                // 获取设备信息
                String deviceFriendlyName = null;
                String deviceIp = null;
                NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;  // ⭐ 定义在外层作用域
                try {
                    deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                    deviceFriendlyName = deviceInfo.getFriendlyName();
                    deviceIp = deviceInfo.getIp();
                } catch (Exception e) {
                    log.warn("  获取设备信息失败: deviceId={}, 错误: {}", deviceId, e.getMessage());
                }
                
                // ✅ 可用性检查：检查设备是否有未完成任务（如有则抛异常）
                uniquenessService.checkAndGetAvailableTask(
                    deviceId,
                    DeviceTask.TaskType.BACKUP,
                    batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
                );
                
                // 设备空闲，创建新定时任务记录
                DeviceTask task = new DeviceTask();
                task.setTaskId(UUID.randomUUID().toString());
                task.setTaskType(DeviceTask.TaskType.BACKUP);
                task.setDeviceId(deviceId);
                log.info("  ✅ 创建新定时任务: deviceId={}, taskId={}", deviceId, task.getTaskId());
                
                // 设置/更新字段
                task.setBatchId(batchId);
                task.setBatchName(batchName);
                task.setTaskInfoActionTime(batchActionTime);
                
                // ⭐ 使用统一方法设置所有设备信息
                if (deviceInfo != null) {
                    taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);
                } else {
                    // 降级处理：如果获取设备信息失败，只设置基本信息
                    if (deviceFriendlyName != null) {
                        task.setDeviceName(deviceFriendlyName);
                    }
                    if (deviceIp != null) {
                        task.setDeviceIp(deviceIp);
                    }
                    log.warn("  ⚠️ 设备信息获取失败，仅设置基本信息");
                }
                
                task.setSftpServerName(request.getSftpServerName());
                task.setStatus(DeviceTask.TaskStatus.SCHEDULED);
                task.setScheduledTime(request.getScheduledTime());
                
                // ⭐ 保存 basePath，在定时执行时再生成完整路径
                // 原因：备份文件名包含时间戳，应该使用实际执行时间，而不是创建时间
                task.setFilePath(request.getBasePath());
                // backupFilePath 和 backupFileName 在定时执行时生成
                
                deviceTaskRepository.save(task);
                
                DeviceBackupDto.DeviceBackupResult result = new DeviceBackupDto.DeviceBackupResult();
                result.setDeviceId(deviceId);
                result.setDeviceName(deviceFriendlyName);
                result.setTaskId(task.getTaskId());
                result.setStatus("SCHEDULED");
                result.setScheduledTime(request.getScheduledTime());
                deviceResults.add(result);
                
                log.info("  ✅ 设备任务已创建: deviceId={}, taskId={}, status=SCHEDULED", deviceId, task.getTaskId());
                
            } catch (Exception e) {
                log.error("  ❌ 创建设备任务失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);
                
                DeviceBackupDto.DeviceBackupResult result = new DeviceBackupDto.DeviceBackupResult();
                result.setDeviceId(deviceId);
                result.setStatus("FAILED");
                result.setErrorMessage("Task creation failed: " + e.getMessage());
                deviceResults.add(result);
            }
        }
        
        // ⭐ 如果是批量模式 且 不是内部调用，发送 TaskInfo 批次通知（定时任务创建）
        if (isBatchMode && !isInternalCall) {
            try {
                List<DeviceTask> deviceTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
                // ⭐ 批次创建通知：从数据库查询 Batch 对象获取 createdBy（第一次通知，必须设置 operator）
                String createdBy = batchRepository.findById(batchId)
                    .map(Batch::getCreatedBy)
                    .orElse(null);
                // ✅ 定时任务创建时使用 notifyBatchTaskStarted（表示任务已创建/调度）
                taskInfoNotificationService.notifyBatchTaskStarted(
                    batchId,
                    batchName,
                    DeviceTask.TaskType.BACKUP,
                    deviceTasks,
                    batchActionTime,
                    createdBy
                );
                log.info("  ✅ TaskInfo 批次通知已发送 (SCHEDULED - 任务已创建)");
            } catch (Exception e) {
                log.warn("  ⚠️ 发送 TaskInfo 批次通知失败: {}", e.getMessage());
            }
        }
        
        // 构建响应
        DeviceBackupDto.BatchBackupSummary summary = new DeviceBackupDto.BatchBackupSummary();
        summary.setBatchId(batchId);
        summary.setBatchName(batchName);
        summary.setTotalDevices(request.getDeviceIds().size());
        summary.setScheduledDevices(deviceResults.size());
        summary.setRunningDevices(0);
        summary.setSuccessDevices(0);
        summary.setFailedDevices(0);
        summary.setStatus("SCHEDULED");
        summary.setScheduledTime(request.getScheduledTime());
        summary.setDeviceResults(deviceResults);
        summary.setCreatedTime(LocalDateTime.now());
        summary.setUpdatedTime(LocalDateTime.now());
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ 定时批量备份任务创建完成");
        log.info("  批次ID: {}", batchId);
        log.info("  定时执行时间: {}", request.getScheduledTime());
        log.info("  总设备数: {}", summary.getTotalDevices());
        log.info("  已调度设备数: {}", summary.getScheduledDevices());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return summary;
    }
    
    /**
     * 在任务失败后更新 Batch 状态
     * （用于没有 Kafka 通知的场景，如 SFTP 目录创建失败）
     */
    private void updateBatchStatusAfterTaskFailure(String batchId) {
        try {
            // 1. 查询批次
            Optional<Batch> batchOpt = batchRepository.findById(batchId);
            if (!batchOpt.isPresent()) {
                log.warn("  批次不存在: {}", batchId);
                return;
            }
            
            Batch batch = batchOpt.get();
            
            // 2. 查询所有工作流
            if (workflowManagementService == null) {
                log.warn("  WorkflowManagementService 未注入，跳过状态更新");
                return;
            }
            
            List<devicemaintenance.entity.UpgradeWorkflow> workflows = 
                workflowManagementService.getWorkflowsByBatchId(batchId);
            
            if (workflows.isEmpty()) {
                log.warn("  批次没有工作流: {}", batchId);
                return;
            }
            
            // 3. 计算所有workflow的状态
            workflowManagementService.computeAndSetWorkflowStatuses(workflows);
            
            // 4. 计算批次状态
            long failedCount = workflows.stream().filter(wf -> wf.isFailed()).count();
            long completedCount = workflows.stream().filter(wf -> wf.isCompleted()).count();
            long runningCount = workflows.stream().filter(wf -> !wf.isFinished()).count();
            
            Batch.BatchStatus newStatus;
            if (failedCount > 0) {
                newStatus = Batch.BatchStatus.FAILED;
            } else if (completedCount == workflows.size()) {
                newStatus = Batch.BatchStatus.COMPLETED;
            } else {
                newStatus = Batch.BatchStatus.RUNNING;
            }
            
            // 5. 更新批次
            batch.updateBatchStatus(newStatus);
            batch.updateCounts((int)completedCount, (int)failedCount, (int)runningCount);
            batchRepository.save(batch);
            
            log.info("  ✅ Batch 状态已更新: {} (成功:{}, 失败:{}, 运行中:{})", 
                newStatus, completedCount, failedCount, runningCount);
            
        } catch (Exception e) {
            log.error("  ⚠️ 更新 Batch 状态失败: batchId={}", batchId, e);
        }
    }
}
