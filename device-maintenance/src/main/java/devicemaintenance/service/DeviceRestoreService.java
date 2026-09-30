package devicemaintenance.service;

import devicemaintenance.dto.DeviceRestoreDto;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.Batch;
import devicemaintenance.helper.BatchHelper;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 设备恢复服务
 * 处理设备数据和配置恢复相关的业务逻辑
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceRestoreService {

    private final DeviceTaskRepository deviceTaskRepository;
    private final BatchRepository maintenanceBatchRepository;
    private final TaskCreationHelper taskCreationHelper;
    private final RealServiceExecutionHelper executionHelper;
    private final TaskInfoNotificationService taskInfoNotificationService;
    private final DeviceTaskUniquenessService uniquenessService;
    private final DeviceAvailabilityService deviceAvailabilityService;
    private final RpcDebugHelper rpcDebugHelper;
    
    @Autowired(required = false)
    private NeMgrIntegrationService neMgrIntegrationService;
    
    @Autowired
    @Qualifier("batchTaskExecutor")
    private Executor batchTaskExecutor;

    /**
     * 启动单设备恢复任务（简化版，无批次）
     * 
     * ✅ 单设备恢复特点：
     * - 必须提供 filePath（不支持 basePath 自动查找）
     * - 不创建批次（batchId = null）
     * - 发送单任务级 TaskInfo 通知
     */
    @Transactional
    public DeviceRestoreDto.RestoreResponse startRestore(DeviceRestoreDto.RestoreRequest request) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [单设备恢复] 启动");
        log.info("  设备ID: {}", request.getDeviceId());
        log.info("  备份文件: {}", request.getFilePath());
        log.info("  SFTP服务器: {}", request.getSftpServerName());
        log.info("  定时执行: {}", request.getScheduledTime() != null);
        
        String deviceId = request.getDeviceId();
        
        // 1. ✅ 验证 filePath 必填（单设备恢复不支持 basePath 自动查找）
        if (request.getFilePath() == null || request.getFilePath().trim().isEmpty()) {
            log.error("❌ filePath 不能为空");
            throw new devicemaintenance.exception.InvalidParameterException(
                "filePath is required for single-device restore"
            );
        }
        
        // 2. ✅ 检查设备可用性
        uniquenessService.checkAndGetAvailableTask(deviceId, DeviceTask.TaskType.RESTORE, null);

        // 3. ✅ 获取设备信息（定时和立即执行都需要，用于设置 deviceName）
        String deviceFriendlyName = null;
        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;

        try {
            // 获取设备信息（无论是定时还是立即执行）
            deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
            deviceFriendlyName = deviceInfo.getFriendlyName();
            log.info("  ✓ 设备信息已获取: {} ({})", deviceFriendlyName, deviceId);
        } catch (Exception e) {
            log.warn("  ⚠️ 获取设备信息失败: deviceId={}, 错误: {}", deviceId, e.getMessage());
        }

        // 4. ✅ Pre-validation SFTP（仅立即执行时）
        if (request.getScheduledTime() == null) {
            log.info("🔍 [Pre-validation] 验证SFTP服务器...");
            try {
                // 验证SFTP服务器是否存在
                if (request.getSftpServerName() != null) {
                    neMgrIntegrationService.getSftpServerById(request.getSftpServerName());
                    log.info("  ✓ SFTP服务器验证通过: {}", request.getSftpServerName());
                }
            } catch (Exception e) {
                log.error("❌ [SFTP验证失败] {}", e.getMessage());
                throw new RuntimeException("SFTP validation failed: " + e.getMessage(), e);
            }
        }
        
        // 4. ✅ 创建 DeviceTask（无批次）
        String taskId = taskCreationHelper.generateTaskId();
        DeviceTask task = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.RESTORE);
        log.info("  ✅ 创建新恢复任务: taskId={}", taskId);
        
        // ✅ 单设备恢复：不设置批次相关字段
        task.setBatchId(null);
        task.setBatchName(null);
        task.setTaskInfoActionTime(null);
        
        // 设置恢复相关字段
        task.setBackupFilePath(request.getFilePath());
        task.setSftpServerName(request.getSftpServerName());
        task.setRetryCount(0);
        
        // ⭐ 使用统一方法设置所有设备信息（包括 vendorType）
        if (deviceInfo != null) {
            taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);
            log.info("  ✓ 设备信息已设置: name={}, vendorType={}", 
                deviceInfo.getFriendlyName(), deviceInfo.getVendorType());
        } else if (deviceFriendlyName != null) {
            task.setDeviceName(deviceFriendlyName);
            log.warn("  ⚠️ 仅设置了设备名称，vendorType 可能缺失");
        }
        
        // 处理定时任务配置
        taskCreationHelper.configureTaskScheduling(task, request.getScheduledTime());
        
        task = deviceTaskRepository.save(task);
        log.info("  ✅ DeviceTask已保存: taskId={}, batchId={}", task.getTaskId(), task.getBatchId());

        // ⭐ 单设备任务：立即发送 TaskInfo 创建通知（PENDING 状态）
        try {
            taskInfoNotificationService.notifyTaskStarted(task);
            deviceTaskRepository.save(task);
            log.info("📨 已发送单设备任务创建通知（PENDING 状态）");
        } catch (Exception e) {
            log.warn("发送TaskInfo创建通知失败（不影响任务执行）: {}", e.getMessage());
        }

        // 5. ✅ 立即执行（非定时任务）
        if (taskCreationHelper.shouldExecuteImmediately(task)) {
            log.info("🔧 执行模式: 立即执行 RPC");

            boolean debug = Boolean.TRUE.equals(request.getDebug());
            executeRestoreRpc(task, debug);

            // ⭐ RUNNING 状态通知已在 updateTaskToRunning 中发送
        } else {
            log.info("📅 定时任务已创建，等待调度执行");
            log.info("  计划执行时间: {}", task.getScheduledTime());
            // ⭐ 创建通知已在上面统一发送（142行），这里不需要重复
        }
        
        // 6. ✅ 返回响应
        DeviceRestoreDto.RestoreResponse response = new DeviceRestoreDto.RestoreResponse();
        response.setSuccess(true);
        response.setTaskId(task.getTaskId());
        response.setDeviceId(deviceId);
        response.setStatus(task.getStatus().name());
        response.setMessage("Device restore task has been started");
        
        log.info("✅ 单设备恢复任务创建成功: taskId={}", task.getTaskId());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return response;
    }
    
    /**
     * 批量设备恢复
     * 
     * @param request 批量恢复请求
     * @return 批量恢复摘要
     */
    public DeviceRestoreDto.BatchRestoreSummary batchRestoreDevices(DeviceRestoreDto.BatchRestoreRequest request) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📦 [批量设备恢复] 开始");
        log.info("  设备数量: {}", request.getDeviceIds().size());
        log.info("  根路径: {}", request.getBasePath());
        log.info("  日期目录: {}", request.getDateDir());
        log.info("  定时执行: {}", request.getScheduledTime() != null);
        log.info("  调试模式: {}", request.getDebug());
        
        // ⭐ 判断是否为批量模式（基于batchName是否提供）
        boolean isBatchMode = org.springframework.util.StringUtils.hasText(request.getBatchName());
        final String batchName = request.getBatchName();
        
        // ✅ 检查批次名称唯一性（仅批量模式时检查）
        if (isBatchMode && batchName != null) {
            if (maintenanceBatchRepository.existsByBatchName(batchName)) {
                throw new IllegalArgumentException("Batch name already exists: " + batchName);
            }
        }
        
        final String batchId;
        final Long batchActionTime;
        
        if (isBatchMode) {
            // 批量模式：生成批次ID和actionTime
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
        
        // ✅ 严格模式 Pre-validation（三项检查：SFTP + 设备存在 + 设备可用）
        // 注意：这里会抛出异常如果验证失败，所以后续代码不会执行
        java.util.Map<String, devicemaintenance.integration.NeMgrIntegrationService.DevicePhysicalInfo> deviceInfoMap =
            deviceAvailabilityService.validateDevicesAvailableStrict(
                request.getDeviceIds(), 
                DeviceTask.TaskType.RESTORE,
                request.getSftpServerName(),
                "批量恢复"
            );
        
        // ⭐ 处理定时执行 vs 立即执行
        if (request.getScheduledTime() != null) {
            return createScheduledBatchRestore(request, batchId, batchActionTime, batchName);
        } else {
            return executeImmediateBatchRestore(request, batchId, batchActionTime, batchName);
        }
    }
    
    /**
     * 创建定时批量恢复任务
     */
    private DeviceRestoreDto.BatchRestoreSummary createScheduledBatchRestore(
            DeviceRestoreDto.BatchRestoreRequest request,
            String batchId,
            Long batchActionTime,
            String batchName) {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("⏰ [定时恢复] 创建定时批量恢复任务");
        log.info("  批次ID: {}", batchId);
        log.info("  批次名称: {}", batchName);
        log.info("  定时执行时间: {}", request.getScheduledTime());
        log.info("  设备数量: {}", request.getDeviceIds().size());
        
        // ⭐ 创建 Batch 记录
        BatchHelper.BatchCreationContext context = new BatchHelper.BatchCreationContext();
        context.setBasePath(request.getBasePath());
        context.setSftpServerName(request.getSftpServerName());
        context.setRemark(request.getRemark());
        context.setDebug(request.getDebug());
        context.setScheduledTime(request.getScheduledTime());
        
        // 判断是否为批量模式
        boolean isBatchMode = org.springframework.util.StringUtils.hasText(batchName);
        
        // 如果是批量模式，创建Batch记录
        if (isBatchMode) {
            Batch batch = BatchHelper.createBatch(
                batchId,
                batchName,
                batchActionTime,
                Batch.BatchType.RESTORE,
                request.getDeviceIds().size(),
                Batch.BatchStatus.SCHEDULED,
                context
            );
            maintenanceBatchRepository.save(batch);
        }
        
        log.info("  ✅ 批次记录已创建: status=SCHEDULED");
        
        // ⭐ 为每个设备创建 SCHEDULED 状态的 DeviceTask
        java.util.List<DeviceRestoreDto.DeviceRestoreResult> deviceResults = new java.util.ArrayList<>();
        
        for (String deviceId : request.getDeviceIds()) {
            try {
                // 获取设备信息
                String deviceFriendlyName = null;
                String deviceIp = null;
                String backupFilePath = null;
                NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;  // ⭐ 定义在外层作用域
                
                try {
                    deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                    deviceFriendlyName = deviceInfo.getFriendlyName();
                    deviceIp = deviceInfo.getIp();
                    
                    // ⭐ 尝试查找备份文件路径（如果失败，在定时触发时再查找）
                    try {
                        backupFilePath = findLatestBackupFile(
                            request.getBasePath(),
                            deviceId,
                            request.getDateDir(),
                            request.getSftpServerName()
                        );
                        log.info("  ✅ 找到备份文件: deviceId={}, path={}", deviceId, backupFilePath);
                    } catch (Exception e) {
                        log.warn("  ⚠️ 暂时无法找到备份文件，将在定时触发时重试: deviceId={}, 错误: {}", 
                                deviceId, e.getMessage());
                    }
                    
                } catch (Exception e) {
                    log.warn("  获取设备信息失败: deviceId={}, 错误: {}", deviceId, e.getMessage());
                }
                
                // ✅ 可用性检查：检查设备是否有未完成任务（如有则抛异常）
                uniquenessService.checkAndGetAvailableTask(
                    deviceId,
                    DeviceTask.TaskType.RESTORE,
                    batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
                );
                
                // 设备空闲，创建新定时任务记录
                DeviceTask task = new DeviceTask();
                task.setTaskId(java.util.UUID.randomUUID().toString());
                task.setTaskType(DeviceTask.TaskType.RESTORE);
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
                task.setBackupFilePath(backupFilePath);  // 可能为null
                
                deviceTaskRepository.save(task);
                
                // ⭐ 构造结果
                DeviceRestoreDto.DeviceRestoreResult result = new DeviceRestoreDto.DeviceRestoreResult();
                result.setDeviceId(deviceId);
                result.setDeviceName(deviceFriendlyName);
                result.setBackupFilePath(backupFilePath);
                result.setStatus("SCHEDULED");
                result.setMessage("Task scheduled at " + request.getScheduledTime());
                deviceResults.add(result);
                
                log.info("  ✅ 设备任务已创建: deviceId={}, taskId={}, status=SCHEDULED", 
                        deviceId, task.getTaskId());
                
            } catch (Exception e) {
                log.error("  ❌ 创建设备任务失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);
                
                DeviceRestoreDto.DeviceRestoreResult result = new DeviceRestoreDto.DeviceRestoreResult();
                result.setDeviceId(deviceId);
                result.setStatus("FAILED");
                result.setMessage("创建任务失败: " + e.getMessage());
                deviceResults.add(result);
            }
        }
        
        // ⭐ 如果是批量模式，发送 TaskInfo 批次通知（定时任务创建）
        if (isBatchMode) {
            try {
                java.util.List<DeviceTask> deviceTasks = 
                    deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
                // ⭐ 批次创建通知：从数据库查询 Batch 对象获取 createdBy（第一次通知，必须设置 operator）
                String createdBy = maintenanceBatchRepository.findById(batchId)
                    .map(devicemaintenance.entity.Batch::getCreatedBy)
                    .orElse(null);
                // ✅ 定时任务创建时使用 notifyBatchTaskStarted（表示任务已创建/调度）
                taskInfoNotificationService.notifyBatchTaskStarted(
                    batchId,
                    batchName,
                    DeviceTask.TaskType.RESTORE,
                    deviceTasks,
                    batchActionTime,
                    createdBy
                );
                log.info("  ✅ TaskInfo 批次通知已发送 (SCHEDULED - 任务已创建)");
            } catch (Exception e) {
                log.warn("  ⚠️ 发送 TaskInfo 批次通知失败: {}", e.getMessage());
            }
        }
        
        // ⭐ 构建响应
        long scheduledCount = deviceResults.stream()
            .filter(r -> "SCHEDULED".equals(r.getStatus())).count();
        long failedCount = deviceResults.stream()
            .filter(r -> "FAILED".equals(r.getStatus())).count();
        
        DeviceRestoreDto.BatchRestoreSummary summary = new DeviceRestoreDto.BatchRestoreSummary();
        summary.setBatchId(batchId);
        summary.setBatchName(batchName);
        summary.setBasePath(request.getBasePath());
        summary.setDateDir(request.getDateDir());
        summary.setSftpServerName(request.getSftpServerName());
        summary.setTotalDevices((int) request.getDeviceIds().size());
        summary.setScheduledDevices((int) scheduledCount);
        summary.setRunningDevices(0);
        summary.setSuccessDevices(0);
        summary.setFailedDevices((int) failedCount);
        summary.setStatus("SCHEDULED");
        summary.setScheduledTime(request.getScheduledTime());
        summary.setRemark(request.getRemark());
        summary.setDeviceResults(deviceResults);
        summary.setCreatedTime(java.time.LocalDateTime.now());
        summary.setUpdatedTime(java.time.LocalDateTime.now());
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ 定时批量恢复任务创建完成");
        log.info("  批次ID: {}", batchId);
        log.info("  定时执行时间: {}", request.getScheduledTime());
        log.info("  总设备数: {}", summary.getTotalDevices());
        log.info("  已调度设备数: {}", summary.getScheduledDevices());
        log.info("  失败设备数: {}", summary.getFailedDevices());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return summary;
    }
    
    /**
     * 立即执行批量恢复
     */
    private DeviceRestoreDto.BatchRestoreSummary executeImmediateBatchRestore(
            DeviceRestoreDto.BatchRestoreRequest request,
            String batchId,
            Long batchActionTime,
            String batchName) {
        
        // 判断是否为批量模式
        boolean isBatchMode = org.springframework.util.StringUtils.hasText(batchName);
        
        log.info("🚀 立即执行批量恢复");
        
        // ⭐ 如果是批量模式，先创建并保存 Batch 实体
        Batch batch = null;
        if (isBatchMode) {
            BatchHelper.BatchCreationContext context = new BatchHelper.BatchCreationContext();
            context.setBasePath(request.getBasePath());
            context.setSftpServerName(request.getSftpServerName());
            context.setRemark(request.getRemark());
            context.setDebug(request.getDebug());
            
            batch = BatchHelper.createBatch(
                batchId,
                batchName,
                batchActionTime,
                Batch.BatchType.RESTORE,
                request.getDeviceIds().size(),
                Batch.BatchStatus.RUNNING,  // 立即执行，初始状态为 RUNNING
                context
            );
            maintenanceBatchRepository.save(batch);
            
            log.info("  ✅ 批次记录已创建: batchId={}, status=RUNNING", batchId);
        }
        
        // ⭐ 使用CompletableFuture并行处理所有设备
        java.util.List<java.util.concurrent.CompletableFuture<DeviceRestoreDto.DeviceRestoreResult>> futures = 
            request.getDeviceIds().stream()
                .<java.util.concurrent.CompletableFuture<DeviceRestoreDto.DeviceRestoreResult>>map(deviceId -> 
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> 
                        processDeviceRestore(deviceId, request, batchId, batchActionTime, batchName),
                        batchTaskExecutor
                    )
                )
                .collect(java.util.stream.Collectors.toList());
        
        // ⭐ 等待所有任务完成
        java.util.concurrent.CompletableFuture<Void> allOf = 
            java.util.concurrent.CompletableFuture.allOf(
                futures.toArray(new java.util.concurrent.CompletableFuture[0])
            );
        
        // ⭐ 收集结果
        allOf.join();  // 阻塞等待所有任务完成
        
        java.util.List<DeviceRestoreDto.DeviceRestoreResult> results = futures.stream()
            .map(java.util.concurrent.CompletableFuture::join)
            .collect(java.util.stream.Collectors.toList());
        
        // ⭐ 统计结果
        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failedCount = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        long runningCount = results.stream().filter(r -> "RUNNING".equals(r.getStatus())).count();
        long scheduledCount = results.stream().filter(r -> "SCHEDULED".equals(r.getStatus())).count();
        
        // ⭐ 如果是批量模式，更新 Batch 状态
        if (isBatchMode) {
            Batch.BatchStatus finalStatus;
            if (failedCount == 0 && runningCount == 0 && scheduledCount == 0) {
                // 全部完成
                finalStatus = Batch.BatchStatus.COMPLETED;
            } else if (failedCount > 0 && runningCount == 0 && successCount == 0 && scheduledCount == 0) {
                // 全部失败
                finalStatus = Batch.BatchStatus.FAILED;
            } else if (failedCount > 0 && successCount > 0 && runningCount == 0 && scheduledCount == 0) {
                // 部分成功/失败（全部已结束）
                finalStatus = Batch.BatchStatus.COMPLETED_WITH_ERRORS;
            } else {
                // 还在运行中
                finalStatus = Batch.BatchStatus.RUNNING;
            }
            
            batch.setStatus(finalStatus);
            maintenanceBatchRepository.save(batch);
            
            log.info("  ✅ 批次状态已更新: status={}", finalStatus);
        }
        
        // ⭐ 构造响应
        DeviceRestoreDto.BatchRestoreSummary summary = new DeviceRestoreDto.BatchRestoreSummary();
        summary.setBatchId(batchId);
        summary.setBatchName(batchName);
        summary.setBasePath(request.getBasePath());
        summary.setDateDir(request.getDateDir());
        summary.setSftpServerName(request.getSftpServerName());
        summary.setTotalDevices((int) request.getDeviceIds().size());
        summary.setSuccessDevices((int) successCount);
        summary.setFailedDevices((int) failedCount);
        summary.setRunningDevices((int) runningCount);
        summary.setScheduledDevices((int) scheduledCount);
        summary.setStatus(determineOverallStatus(successCount, failedCount, runningCount, scheduledCount, request.getDeviceIds().size()));
        summary.setRemark(request.getRemark());
        summary.setScheduledTime(request.getScheduledTime());
        summary.setCreatedTime(java.time.LocalDateTime.now());
        summary.setUpdatedTime(java.time.LocalDateTime.now());
        summary.setDeviceResults(results);
        
        log.info("✅ 批量恢复完成");
        log.info("  总设备数: {}", summary.getTotalDevices());
        log.info("  成功: {}", summary.getSuccessDevices());
        log.info("  失败: {}", summary.getFailedDevices());
        log.info("  执行中: {}", summary.getRunningDevices());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return summary;
    }
    
    /**
     * 处理单个设备的恢复
     */
    private DeviceRestoreDto.DeviceRestoreResult processDeviceRestore(
            String deviceId,
            DeviceRestoreDto.BatchRestoreRequest request,
            String batchId,
            Long batchActionTime,
            String batchName) {
        
        DeviceRestoreDto.DeviceRestoreResult result = new DeviceRestoreDto.DeviceRestoreResult();
        result.setDeviceId(deviceId);
        
        DeviceTask deviceTask = null;
        
        try {
            log.info("🔄 处理设备恢复: {}", deviceId);
            
            // ⭐ 1. 获取设备信息
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = 
                neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
            result.setDeviceName(deviceInfo.getFriendlyName());
            
            // ✅ 2. 可用性检查：检查设备是否有未完成任务（如有则抛异常）
            uniquenessService.checkAndGetAvailableTask(
                deviceId,
                DeviceTask.TaskType.RESTORE,
                batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
            );
            
            // 设备空闲，创建新恢复任务记录
            String taskId = taskCreationHelper.generateTaskId();
            deviceTask = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.RESTORE);
            log.info("  ✅ 创建新恢复任务: taskId={}", taskId);
            
            // 设置/更新批次信息和设备信息
            deviceTask.setBatchId(batchId);
            deviceTask.setBatchName(batchName);
            deviceTask.setTaskInfoActionTime(batchActionTime);
            
            // ⭐ 使用统一方法设置所有设备信息
            taskCreationHelper.setDeviceInfoToTask(deviceTask, deviceInfo);
            
            deviceTask.setStatus(DeviceTask.TaskStatus.RUNNING);
            deviceTask = deviceTaskRepository.save(deviceTask);
            log.info("  ✅ DeviceTask已保存: taskId={}", deviceTask.getTaskId());
            
            // ⭐ 3. 自动查找最新备份文件（可能失败，需要捕获）
            String backupFilePath;
            try {
                backupFilePath = findLatestBackupFile(
                    request.getBasePath(),
                    deviceId,
                    request.getDateDir(),
                    request.getSftpServerName()
                );
                result.setBackupFilePath(backupFilePath);
                log.info("  ✅ 找到备份文件: {}", backupFilePath);
            } catch (Exception e) {
                log.error("❌ 查找备份文件失败: {}", e.getMessage());
                
                // 更新 DeviceTask 为 FAILED，并保存 debug payload
                deviceTask.setStatus(DeviceTask.TaskStatus.FAILED);
                deviceTask.setErrorMessage("Failed to find backup file: " + e.getMessage());
                
                // 如果启用了 debug，保存详细的错误信息
                if (Boolean.TRUE.equals(request.getDebug())) {
                    // 构造 debug payload
                    Map<String, Object> debugPayload = new LinkedHashMap<>();
                    debugPayload.put("operation", "list-backup-files");
                    debugPayload.put("base-path", request.getBasePath());
                    debugPayload.put("device-id", deviceId);
                    debugPayload.put("date-dir", request.getDateDir());
                    debugPayload.put("sftp-server", request.getSftpServerName());
                    debugPayload.put("timestamp", System.currentTimeMillis());
                    
                    rpcDebugHelper.saveFailureDebugPayload(deviceTask, e, debugPayload, true);
                }
                
                deviceTaskRepository.save(deviceTask);
                
                // 设置返回结果
                result.setStatus("FAILED");
                result.setMessage("Failed to find backup file: " + e.getMessage());
                return result;
            }
            
            // ⭐ 4. 更新 DeviceTask 的备份文件路径
            deviceTask.setBackupFilePath(backupFilePath);
            deviceTask.setSftpServerName(request.getSftpServerName());
            deviceTaskRepository.save(deviceTask);
            
            // ⭐ 5. 执行实际的恢复 RPC 调用
            boolean debug = Boolean.TRUE.equals(request.getDebug());
            executeRestoreRpc(deviceTask, debug);
            
            // ⭐ 6. 设置结果（RPC下发成功，实际状态由Kafka通知更新）
            result.setStatus("RUNNING");
            result.setMessage(null);  // 清除错误信息
            
        } catch (Exception e) {
            log.error("❌ 设备恢复失败: {}", deviceId, e);
            
            // ⭐ 确保创建 DeviceTask（即使 pre-validation 失败）
            if (deviceTask == null) {
                try {
                    deviceTask = uniquenessService.checkAndGetAvailableTask(
                        deviceId,
                        DeviceTask.TaskType.RESTORE,
                        batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
                    );
                    
                    if (deviceTask == null) {
                        String taskId = taskCreationHelper.generateTaskId();
                        deviceTask = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.RESTORE);
                        log.info("  ✅ 创建失败任务记录: taskId={}", taskId);
                    }
                    
                    // 设置批次信息
                    deviceTask.setBatchId(batchId);
                    deviceTask.setBatchName(batchName);
                    deviceTask.setTaskInfoActionTime(batchActionTime);
                } catch (Exception createEx) {
                    log.error("  ⚠️ 创建失败任务记录时出错: {}", createEx.getMessage());
                }
            }
            
            // 更新任务状态为失败
            if (deviceTask != null) {
                deviceTask.setStatus(DeviceTask.TaskStatus.FAILED);
                deviceTask.setErrorMessage(e.getMessage());
                deviceTask.setCompletedTime(LocalDateTime.now());
                
                // 如果启用了 debug，保存详细的错误信息
                if (Boolean.TRUE.equals(request.getDebug())) {
                    Map<String, Object> debugPayload = new LinkedHashMap<>();
                    debugPayload.put("operation", "restore-device");
                    debugPayload.put("device-id", deviceId);
                    debugPayload.put("timestamp", System.currentTimeMillis());
                    
                    rpcDebugHelper.saveFailureDebugPayload(deviceTask, e, debugPayload, true);
                }
                
                deviceTaskRepository.save(deviceTask);
            }
            
            result.setStatus("FAILED");
            result.setMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 确定批次的整体状态
     */
    private String determineOverallStatus(long successCount, long failedCount, 
                                         long runningCount, long scheduledCount, 
                                         int totalCount) {
        if (scheduledCount > 0) {
            return "SCHEDULED";
        } else if (runningCount > 0) {
            return "RUNNING";
        } else if (failedCount == totalCount) {
            return "FAILED";
        } else if (successCount == totalCount) {
            return "COMPLETED";
        } else {
            return "COMPLETED_WITH_ERRORS";
        }
    }
    
    /**
     * 查找最新的备份文件
     * 
     * 路径结构: basePath/device_name/yyyyMMdd/yyyyMMdd_HHmmss.db
     * 
     * @param basePath 备份基础路径
     * @param deviceId 设备ID
     * @param sftpServerName SFTP服务器名称
     * @return 最新备份文件的完整路径
     */
    private String findLatestBackupFile(String basePath, String deviceId, String sftpServerName) {
        return findLatestBackupFile(basePath, deviceId, null, sftpServerName);
    }

    /**
     * 自动查找最新备份文件（支持指定日期目录）
     * 
     * @param basePath 备份基础路径
     * @param deviceId 设备ID
     * @param dateDir 日期目录（yyyyMMdd格式），如果为空则自动查找最新日期
     * @param sftpServerName SFTP服务器名称
     * @return 最新备份文件的完整路径
     */
    private String findLatestBackupFile(String basePath, String deviceId, 
                                       String dateDir, String sftpServerName) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 [自动查找最新备份]");
        log.info("  基础路径: {}", basePath);
        log.info("  设备ID: {}", deviceId);
        log.info("  指定日期: {}", dateDir != null ? dateDir : "自动查找最新");
        
        try {
            // 1. 获取设备名称（用于构造设备目录）
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = 
                neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
            String deviceName = deviceInfo.getFriendlyName();
            
            // 2. 清理设备名称（与备份时保持一致）
            String sanitizedDeviceName = sanitizeDirectoryName(deviceName);
            log.info("  设备名称: {} -> {}", deviceName, sanitizedDeviceName);
            
            // 3. 构造设备目录路径
            String deviceDir = basePath + "/" + sanitizedDeviceName;
            log.info("  设备目录: {}", deviceDir);
            
            // 4. 确定日期目录
            String targetDateDir;
            if (dateDir != null && !dateDir.trim().isEmpty()) {
                // 使用指定的日期目录
                targetDateDir = dateDir.trim();
                log.info("  📅 使用指定日期目录: {}", targetDateDir);
            } else {
                // 自动查找最新日期目录
                java.util.List<NeMgrIntegrationService.SftpFileInfo> dateDirs = 
                    neMgrIntegrationService.listRemoteDirectory(sftpServerName, deviceDir);
                
                // 过滤出目录（日期目录格式：yyyyMMdd）
                java.util.List<String> dateDirNames = dateDirs.stream()
                    .filter(NeMgrIntegrationService.SftpFileInfo::isDirectory)
                    .map(NeMgrIntegrationService.SftpFileInfo::getName)
                    .filter(name -> name.matches("\\d{8}"))  // 只保留8位数字的目录
                    .sorted(java.util.Comparator.reverseOrder())  // 降序排序（最新的在前）
                    .collect(java.util.stream.Collectors.toList());
                
                if (dateDirNames.isEmpty()) {
                    throw new IllegalStateException("No date directories found in " + deviceDir);
                }
                
                targetDateDir = dateDirNames.get(0);
                log.info("  ✅ 找到最新日期目录: {}", targetDateDir);
            }
            
            // 5. 列出日期目录下的备份文件
            String latestDatePath = deviceDir + "/" + targetDateDir;
            java.util.List<NeMgrIntegrationService.SftpFileInfo> backupFiles = 
                neMgrIntegrationService.listRemoteDirectory(sftpServerName, latestDatePath);
            
            // 过滤出.db文件（格式：HHmmss.db 或 yyyyMMdd_HHmmss.db）
            java.util.List<String> backupFileNames = backupFiles.stream()
                .filter(f -> !f.isDirectory())
                .map(NeMgrIntegrationService.SftpFileInfo::getName)
                .filter(name -> name.endsWith(".db"))  // 所有.db文件
                .sorted(java.util.Comparator.reverseOrder())  // 降序排序（最新的在前）
                .collect(java.util.stream.Collectors.toList());
            
            if (backupFileNames.isEmpty()) {
                throw new IllegalStateException("No backup files found in " + latestDatePath);
            }
            
            String latestBackupFile = backupFileNames.get(0);
            log.info("  ✅ 找到最新备份文件: {}", latestBackupFile);
            
            // 6. 构造完整路径
            String fullPath = latestDatePath + "/" + latestBackupFile;
            log.info("  ✅ 最新备份路径: {}", fullPath);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            return fullPath;
            
        } catch (Exception e) {
            log.error("❌ 查找最新备份失败", e);
            throw new RuntimeException("Failed to find latest backup: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理目录名（与备份服务保持一致）
     * 只替换文件系统非法字符，保留中文等 Unicode 字符
     */
    private String sanitizeDirectoryName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        // 只替换文件系统非法字符: / \ : * ? " < > |  以及空格
        // 保留中文、日文等 Unicode 字符
        return name.replaceAll("[/\\\\:*?\"<>|\\s]+", "-");
    }

    /**
     * 启动设备恢复任务（内部接口，支持批次关联）
     */
    @Transactional
    public DeviceRestoreDto.RestoreResponse startRestoreWithBatch(
            DeviceRestoreDto.RestoreRequest request,
            String batchId,
            Long batchActionTime,
            String batchName) {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [任务创建] 设备恢复任务");
        log.info("  设备ID: {}", request.getDeviceId());
        log.info("  批次ID: {}", batchId);
        log.info("  批次ActionTime: {}", batchActionTime);
        log.info("  备份文件: {}", request.getFilePath());
        log.info("  SFTP服务器: {}", request.getSftpServerName());

        try {
            // 验证备份文件路径
            if (request.getFilePath() == null || request.getFilePath().trim().isEmpty()) {
                throw new IllegalArgumentException("Backup file path cannot be empty");
            }

            // ✅ 对于立即执行的任务，先验证设备和SFTP服务器
            // 验证失败则不创建任务，直接抛异常
            String deviceFriendlyName = null;
            if (request.getScheduledTime() == null) {
                log.info("🔍 [Pre-validation] 验证设备和SFTP服务器...");
                try {
                    // 1. 验证设备是否存在，并获取友好名称
                    NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = 
                        neMgrIntegrationService.getDeviceInfoByNodeId(request.getDeviceId());
                    deviceFriendlyName = deviceInfo.getFriendlyName();
                    log.info("  ✓ 设备验证通过: {} ({})", deviceFriendlyName, request.getDeviceId());
                    
                    // 2. 验证SFTP服务器是否存在
                    if (request.getSftpServerName() != null) {
                        neMgrIntegrationService.getSftpServerById(request.getSftpServerName());
                        log.info("  ✓ SFTP服务器验证通过: {}", request.getSftpServerName());
                    }
                } catch (Exception e) {
                    log.error("❌ [Pre-validation failed] {}", e.getMessage());
                    // 验证失败：不创建任务，直接抛异常
                    throw new RuntimeException("Pre-validation failed: " + e.getMessage(), e);
                }
            }

            // 检查设备是否已有进行中的恢复任务
            boolean hasRunningRestore = deviceTaskRepository.existsByDeviceIdAndTaskTypeAndStatus(
                    request.getDeviceId(), 
                    DeviceTask.TaskType.RESTORE, 
                    DeviceTask.TaskStatus.RUNNING);

            if (hasRunningRestore) {
                throw new IllegalStateException("Device " + request.getDeviceId() + " already has a running restore task");
            }

            // 创建设备任务记录
            DeviceTask task = createDeviceTask(request, batchId);
            
            // ✅ 如果获取到了设备友好名称，保存到任务记录中
            if (deviceFriendlyName != null) {
                task.setDeviceName(deviceFriendlyName);
                log.info("  ✓ Device friendly name set: {}", deviceFriendlyName);
            }
            
            // ⭐ 设置批次统一的 actionTime
            if (batchActionTime != null) {
                task.setTaskInfoActionTime(batchActionTime);
                log.info("  ✓ Batch ActionTime set: {}", batchActionTime);
            }
            
            task = deviceTaskRepository.save(task);

            log.info("✅ 任务记录已保存到数据库");
            log.info("  任务ID: {}", task.getTaskId());
            log.info("  初始状态: {}", task.getStatus());

            // 只有非定时任务（立即执行）才调用执行服务
            if (taskCreationHelper.shouldExecuteImmediately(task)) {
                log.info("🔧 执行模式: Real (真实neMgr RPC)");
                executeRealRestore(task, false);  // 不启用debug
                
                // 刷新任务状态
                // ⚠️ 移除flush()调用，避免死锁
                DeviceTask updatedTask = deviceTaskRepository.findById(task.getTaskId()).orElse(task);
                
                // ⭐ 发送批次级别的 TaskInfo 通知（单设备批次）
                if (batchId != null && batchName != null && batchActionTime != null) {
                    try {
                        List<DeviceTask> batchTasks = java.util.Arrays.asList(updatedTask);
                        // ⭐ 批次创建通知：从数据库查询 Batch 对象获取 createdBy（第一次通知，必须设置 operator）
                        String createdBy = maintenanceBatchRepository.findById(batchId)
                            .map(devicemaintenance.entity.Batch::getCreatedBy)
                            .orElse(null);
                        taskInfoNotificationService.notifyBatchTaskStarted(
                            batchId,
                            batchName,
                            DeviceTask.TaskType.RESTORE,
                            batchTasks,
                            batchActionTime,
                            createdBy
                        );
                        log.info("📨 已发送批次级别 TaskInfo 通知（单设备批次）");
                    } catch (Exception e) {
                        log.warn("发送批次TaskInfo通知失败（不影响任务执行）: {}", e.getMessage());
                    }
                }
                
                // ❌ 不再发送设备级通知！
                // 批次任务通过 SystemChangeNotificationListener 的批次级防抖通知来更新 TaskInfo
            } else {
                log.info("📅 定时任务已创建，等待调度执行");
                log.info("  计划执行时间: {}", task.getScheduledTime());
            }

            // 返回响应
            DeviceRestoreDto.RestoreResponse response = convertToResponse(task);
            response.setSuccess(true);
            response.setMessage("Device restore task has been started");

            log.info("✅ 设备恢复任务创建成功: taskId={}, deviceId={}", task.getTaskId(), request.getDeviceId());
            return response;

        } catch (Exception e) {
            log.error("❌ 启动设备恢复任务失败: deviceId={}", request.getDeviceId(), e);
            DeviceRestoreDto.RestoreResponse errorResponse = new DeviceRestoreDto.RestoreResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to start restore task: " + e.getMessage());
            errorResponse.setDeviceId(request.getDeviceId());
            errorResponse.setBackupFilePath(request.getFilePath());
            return errorResponse;
        }
    }

    /**
     * 创建设备任务记录
     */
    private DeviceTask createDeviceTask(DeviceRestoreDto.RestoreRequest request, String batchId) {
        // ✅ 可用性检查：检查设备是否有未完成任务（如有则抛异常）
        uniquenessService.checkAndGetAvailableTask(
            request.getDeviceId(),
            DeviceTask.TaskType.RESTORE,
            batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
        );
        
        // 设备空闲，创建新恢复任务记录
        String taskId = taskCreationHelper.generateTaskId();
        DeviceTask task = new DeviceTask(taskId, request.getDeviceId(), DeviceTask.TaskType.RESTORE);
        log.info("  ✅ 创建新恢复任务: taskId={}", taskId);
        
        // ✅ 设置批次ID（可能为null，这是正常的）
        task.setBatchId(batchId);
        
        // 设置恢复相关字段
        task.setBackupFilePath(request.getFilePath());
        
        // 直接存储SFTP服务器name（如果提供了）
        if (request.getSftpServerName() != null) {
            task.setSftpServerName(request.getSftpServerName());
        }
        
        // 设置额外的字段
        task.setRetryCount(0);
        
        // 使用统一的定时任务配置逻辑
        taskCreationHelper.configureTaskScheduling(task, request.getScheduledTime());

        return task;
    }

    /**
     * 执行真实恢复（调用nemgr微服务，异步）
     */
    @Async("taskSchedulerExecutor")
    private void executeRealRestore(DeviceTask task, boolean debug) {
        executeRestoreRpc(task, debug);
    }
    
    /**
     * 执行真实恢复RPC调用（同步方法，供批量恢复使用）
     */
    private void executeRestoreRpc(DeviceTask task, boolean debug) {
        log.info("执行真实设备恢复: taskId={}, deviceId={}, sftpServer={}, debug={}", 
                task.getTaskId(), task.getDeviceId(), task.getSftpServerName(), debug);
        
        try {
            executionHelper.updateTaskToRunning(task);
            
            // 调用完整版本的databaseRestore（包含SFTP信息和debug支持）
            NeMgrIntegrationService.RpcResult result = neMgrIntegrationService.databaseRestore(
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
                // RPC调用失败（返回null），标记为失败
                executionHelper.completeTaskFailure(task, "Device Restore", "RPC call failed, returned null", 
                    result != null ? result.getDebugPayload() : null, null, debug);
            } else {
                // ✅ 检查 RPC 响应的 result 字段
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput output = 
                    (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput) result.getOutput();
                String rpcResult = output.getResult();
                
                // ✅ 使用统一的失败判断方法
                if (executionHelper.isFailureResult(rpcResult)) {
                    // ❌ RPC 返回失败
                    log.error("❌ RPC returned fail: result={}", rpcResult);
                    executionHelper.completeTaskFailure(task, "Device Restore", "RPC execution failed, result: " + rpcResult, 
                        result.getDebugPayload(), output, debug);
                } else {
                    // ✅ RPC调用成功，命令已下发，状态保持为RUNNING
                    // 最终状态将由 Kafka 异步通知更新
                    log.info("✅ Restore command successfully issued to neMgr, result: {}", rpcResult);
                    log.info("⏳ Task status: RUNNING - waiting for Kafka notification");
                }
            }
            
        } catch (Exception e) {
            // ✅ 异常情况下无法获取完整的 debugPayload
            executionHelper.handleTaskError(task, "Device Restore", e, null);
        }
    }

    /**
     * 执行定时任务（供调度器调用）
     * 
     * @param task 待执行的任务（状态已更新为PENDING）
     */
    public void executeScheduledTask(DeviceTask task) {
        log.info("执行定时恢复任务: taskId={}, deviceId={}", task.getTaskId(), task.getDeviceId());
        
        // ⭐ 确定 debug 开关（从 Batch 读取）
        boolean debug = false;
        Batch batch = null;
        if (task.getBatchId() != null) {
            batch = maintenanceBatchRepository.findById(task.getBatchId()).orElse(null);
            if (batch != null) {
                debug = batch.getDebug();
                log.debug("从 Batch 读取 debug 开关: batchId={}, debug={}", task.getBatchId(), debug);
            }
        }
        
        // ⭐ 如果创建时未找到备份文件，现在重试查找
        if (task.getBackupFilePath() == null) {
            log.warn("  ⚠️ 任务创建时未找到备份文件，现在重试查找");
            
            try {
                // 优先从 task.getFilePath() 获取 basePath（创建时保存）
                // 如果没有，再从 Batch 获取
                String basePath = task.getFilePath();
                if (basePath == null || basePath.isEmpty()) {
                    basePath = batch != null ? batch.getBasePath() : null;
                }
                
                String dateDir = null;  // 查找最新的
                
                if (basePath != null && !basePath.isEmpty()) {
                    String backupFilePath = findLatestBackupFile(
                        basePath,
                        task.getDeviceId(),
                        dateDir,
                        task.getSftpServerName()
                    );
                    
                    task.setBackupFilePath(backupFilePath);
                    deviceTaskRepository.save(task);
                    
                    log.info("  ✅ 找到备份文件: {}", backupFilePath);
                } else {
                    log.error("  ❌ 无法获取 basePath，无法查找备份文件");
                    executionHelper.handleTaskError(task, "Device Restore", 
                        new IllegalArgumentException("Failed to get backup path configuration"), null);
                    return;
                }
            } catch (Exception e) {
                log.error("  ❌ 查找备份文件失败: {}", e.getMessage());
                executionHelper.handleTaskError(task, "Device Restore", e, null);
                return;
            }
        }
        
        // 构造请求对象
        DeviceRestoreDto.RestoreRequest request = new DeviceRestoreDto.RestoreRequest();
        request.setDeviceId(task.getDeviceId());
        request.setFilePath(task.getBackupFilePath());
        request.setSftpServerName(task.getSftpServerName());
        
        log.info("使用真实服务执行定时恢复任务: taskId={}, backupFilePath={}, debug={}", 
            task.getTaskId(), task.getBackupFilePath(), debug);
        executeRealRestore(task, debug);
        
        // ❌ 不再发送设备级通知！
        // 批次任务通过 SystemChangeNotificationListener 的批次级防抖通知来更新 TaskInfo
    }

    /**
     * 转换为响应DTO
     */
    private DeviceRestoreDto.RestoreResponse convertToResponse(DeviceTask task) {
        DeviceRestoreDto.RestoreResponse response = new DeviceRestoreDto.RestoreResponse();
        response.setTaskId(task.getTaskId());
        response.setDeviceId(task.getDeviceId());
        response.setBackupFilePath(task.getBackupFilePath());
        response.setStatus(task.getStatus().name());
        response.setRestoreStatus("N/A");
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedTime(task.getCreatedTime());
        response.setUpdatedTime(task.getUpdatedTime());
        response.setCompletedTime(task.getCompletedTime());
        return response;
    }

    /**
     * 验证备份文件
     */
    public DeviceRestoreDto.RestoreValidationResult validateBackupFile(String deviceId, String backupFilePath) {
        log.info("验证备份文件: deviceId={}, backupFilePath={}", deviceId, backupFilePath);
        
        try {
            // TODO: 实现真实的备份文件验证逻辑
            // 这里返回Mock验证结果
            DeviceRestoreDto.RestoreValidationResult result = new DeviceRestoreDto.RestoreValidationResult();
            result.setValid(true);
            result.setBackupFileInfo("Backup file size: 128MB, created at: 2025-09-25 10:30:00");
            result.setDeviceCompatible(true);
            result.setVersionCompatible(true);
            result.setValidationMessage("Backup file validation passed");
            result.setWarnings("The device will restart during restore. Make sure the network connection is stable.");
            
            return result;
            
        } catch (Exception e) {
            log.error("验证备份文件失败: deviceId={}, backupFilePath={}", deviceId, backupFilePath, e);
            DeviceRestoreDto.RestoreValidationResult result = new DeviceRestoreDto.RestoreValidationResult();
            result.setValid(false);
            result.setValidationMessage("Backup file validation failed: " + e.getMessage());
            return result;
        }
    }
}
