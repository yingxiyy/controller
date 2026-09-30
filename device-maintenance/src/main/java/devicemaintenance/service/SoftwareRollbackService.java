package devicemaintenance.service;

import devicemaintenance.dto.SoftwareRollbackDto;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.UpgradeWorkflow;
import devicemaintenance.helper.BatchHelper;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.UpgradeWorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 软件回滚服务
 * 用于回滚到之前的软件版本
 * 
 * 支持：
 * - 单设备回滚
 * - 批量设备回滚（每个设备对应不同的回滚文件）
 */
@Slf4j
@Service
@Transactional
public class SoftwareRollbackService {

    @Autowired
    private DeviceTaskRepository deviceTaskRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private NeMgrIntegrationService neMgrIntegrationService;

    @Autowired
    private RealServiceExecutionHelper executionHelper;

    @Autowired
    private TaskCreationHelper taskCreationHelper;

    @Autowired
    private RpcDebugHelper rpcDebugHelper;

    @Autowired
    private DeviceTaskUniquenessService uniquenessService;

    @Autowired
    private TaskInfoNotificationService taskInfoNotificationService;

    @Autowired
    private UpgradeWorkflowRepository workflowRepository;

    @Autowired
    private WorkflowManagementService workflowManagementService;

    @Autowired
    private BatchUpgradeService batchUpgradeService;

    @Autowired
    @Qualifier("batchTaskExecutor")
    private Executor batchTaskExecutor;

    /**
     * 启动软件回滚任务（公共接口）
     */
    public SoftwareRollbackDto.RollbackResponse startRollback(SoftwareRollbackDto.RollbackRequest request) {
        return startRollback(request, false);
    }

    public SoftwareRollbackDto.RollbackResponse retryRollbackTask(String taskId) {
        DeviceTask task = deviceTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Rollback task not found: " + taskId));

        if (task.getTaskType() != DeviceTask.TaskType.ROLLBACK) {
            throw new IllegalArgumentException("Task is not a rollback task: " + taskId);
        }

        if (task.getStatus() != DeviceTask.TaskStatus.FAILED) {
            throw new IllegalStateException("Only failed rollback tasks can be retried: " + taskId);
        }

        if (task.getFilePath() == null || task.getFilePath().trim().isEmpty()) {
            throw new IllegalStateException("Rollback task file name is missing: " + taskId);
        }

        if (task.getSftpServerName() == null || task.getSftpServerName().trim().isEmpty()) {
            throw new IllegalStateException("Rollback task SFTP server is missing: " + taskId);
        }

        uniquenessService.checkAndGetAvailableTask(task.getDeviceId(), DeviceTask.TaskType.ROLLBACK, task.getBatchId());

        task.setStatus(DeviceTask.TaskStatus.PENDING);
        task.setErrorMessage(null);
        task.setDebugPayload(null);
        task.setStartedTime(null);
        task.setCompletedTime(null);
        task.setUpdatedTime(LocalDateTime.now());
        deviceTaskRepository.save(task);

        executeRealRollback(task, false);
        return convertToResponse(task);
    }

    /**
     * 启动软件回滚任务（支持debug）
     */
    public SoftwareRollbackDto.RollbackResponse startRollback(SoftwareRollbackDto.RollbackRequest request, boolean debug) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [任务创建] 软件回滚任务");
        log.info("  设备ID: {}", request.getDeviceId());
        log.info("  SFTP服务器: {}", request.getSftpServerName());
        log.info("  软件包文件名: {}", request.getFileName());
        log.info("  Debug模式: {}", debug);

        // ✅ 对于立即执行的任务，先验证设备信息
        // ⚠️ 验证失败直接抛异常，不创建任务
        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;  // ⭐ 定义在外层作用域
        if (request.getScheduledTime() == null) {
            log.info("🔍 [Pre-validation] Validating device...");
            try {
                // 验证设备是否存在，并获取友好名称和IP
                deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(request.getDeviceId());
                log.info("  ✓ Device validated: {} ({}) - IP: {}", 
                    deviceInfo.getFriendlyName(), request.getDeviceId(), deviceInfo.getIp());
            } catch (Exception e) {
                log.error("❌ [Pre-validation failed] {}", e.getMessage());
                throw new RuntimeException("Pre-validation failed: " + e.getMessage(), e);
            }
        }

        // 创建任务（只有预验证通过才会到这里）
        DeviceTask task = createRollbackTask(request);
        
        // ⭐ 使用统一方法设置所有设备信息
        taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);
        
        task = deviceTaskRepository.save(task);
        log.info("✅ 任务记录已保存到数据库");
        log.info("  任务ID: {}", task.getTaskId());
        removeReadyToCommitUpgradeWorkflowsForRollback(task.getDeviceId());

        // ⭐ 单设备任务：立即发送 TaskInfo 创建通知（PENDING 状态）
        try {
            taskInfoNotificationService.notifyTaskStarted(task);
            log.info("📨 已发送单设备任务创建通知（PENDING 状态）");
        } catch (Exception e) {
            log.warn("发送TaskInfo创建通知失败（不影响任务执行）: {}", e.getMessage());
        }

        // 立即执行
        if (request.getScheduledTime() == null) {
            log.info("🚀 立即执行回滚任务");
            executeRealRollback(task, debug);
            // ⭐ RUNNING 状态通知已在 updateTaskToRunning 中发送
        } else {
            log.info("⏰ 任务已计划在 {} 执行", request.getScheduledTime());
            // ⭐ 创建通知已在上面统一发送，这里不需要重复
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return convertToResponse(task);
    }

    /**
     * 创建回滚任务
     */
    private DeviceTask createRollbackTask(SoftwareRollbackDto.RollbackRequest request) {
        // ✅ 可用性检查：检查设备是否有未完成任务（如有则抛异常）
        uniquenessService.checkAndGetAvailableTask(
            request.getDeviceId(),
            DeviceTask.TaskType.ROLLBACK,
            null  // Rollback 不属于 batch，传 null
        );
        
        // 设备空闲，创建新回滚任务记录
        String taskId = taskCreationHelper.generateTaskId();
        DeviceTask task = new DeviceTask(taskId, request.getDeviceId(), DeviceTask.TaskType.ROLLBACK);
        log.info("  ✅ 创建新回滚任务: taskId={}", taskId);
        
        // 设置SFTP服务器
        if (request.getSftpServerName() != null) {
            task.setSftpServerName(request.getSftpServerName());
        }
        
        // 设置文件名
        if (request.getFileName() != null) {
            task.setFilePath(request.getFileName());
        }
        
        // 设置描述
        if (request.getDescription() != null) {
            task.setErrorMessage(request.getDescription());
        }
        
        // 设置调度时间
        if (request.getScheduledTime() != null) {
            task.setScheduledTime(request.getScheduledTime());
            task.setStatus(DeviceTask.TaskStatus.PENDING);
        } else {
            task.setStatus(DeviceTask.TaskStatus.PENDING);
        }
        
        task.setCreatedTime(LocalDateTime.now());
        task.setUpdatedTime(LocalDateTime.now());
        
        return task;
    }

    /**
     * 执行真实回滚（调用nemgr微服务）
     */
    @Async("taskSchedulerExecutor")
    private void executeRealRollback(DeviceTask task, boolean debug) {
        log.info("执行真实设备回滚: taskId={}, deviceId={}, sftpServer={}, fileName={}, debug={}", 
            task.getTaskId(), task.getDeviceId(), task.getSftpServerName(), task.getFilePath(), debug);
        
        try {
            executionHelper.updateTaskToRunning(task);
            
            // ✅ 调用 softwareRollback，现在返回 RpcResult 包含完整的 debugPayload
            NeMgrIntegrationService.RpcResult result = neMgrIntegrationService.softwareRollback(
                task.getDeviceId(), 
                task.getSftpServerName(),
                task.getFilePath()
            );
            
            // ✅ 保存设备友好名称（统一方法）
            rpcDebugHelper.saveFriendlyNameIfPresent(task, result);
            
            // ✅ 检查RPC响应：只验证命令是否成功下发
            if (result == null || result.getOutput() == null) {
                // RPC调用失败（返回null），标记为失败
                executionHelper.completeTaskFailure(task, "Software Rollback", "RPC call failed, returned null", 
                    result != null ? result.getDebugPayload() : null, null, debug);
            } else {
                NeSoftwareOperateOutput output = (NeSoftwareOperateOutput) result.getOutput();
                String rpcResult = output.getResult();
                
                // ✅ 使用统一的失败判断方法
                if (executionHelper.isFailureResult(rpcResult)) {
                    // ❌ RPC 返回失败
                    log.error("❌ RPC returned fail: result={}", rpcResult);
                    executionHelper.completeTaskFailure(task, "Software Rollback", "RPC execution failed, result: " + rpcResult, 
                        result.getDebugPayload(), output, debug);
                } else {
                    // ✅ RPC调用成功，命令已下发，状态保持为RUNNING
                    // 最终状态将由 Kafka 异步通知更新
                    log.info("✅ Rollback command successfully issued to neMgr, result: {}", rpcResult);
                    log.info("⏳ Task status: RUNNING - waiting for Kafka notification");
                    
                    // ✅ 保存 debug payload 并持久化（统一方法）
                    rpcDebugHelper.saveDebugPayloadAndPersist(task, result.getDebugPayload(), debug);
                }
            }
            
        } catch (Exception e) {
            // ✅ 异常情况下无法获取完整的 debugPayload
            executionHelper.handleTaskError(task, "Software Rollback", e, null);
        }
    }

    /**
     * 执行定时任务（供调度器调用）
     */
    public void executeScheduledTask(DeviceTask task) {
        log.info("执行定时回滚任务: taskId={}, deviceId={}", task.getTaskId(), task.getDeviceId());
        executeRealRollback(task, false);  // 定时任务不启用debug
    }

    /**
     * 转换为响应对象
     */
    private SoftwareRollbackDto.RollbackResponse convertToResponse(DeviceTask task) {
        SoftwareRollbackDto.RollbackResponse response = new SoftwareRollbackDto.RollbackResponse();
        response.setTaskId(task.getTaskId());
        response.setDeviceId(task.getDeviceId());
        response.setSftpServerName(task.getSftpServerName());
        response.setFileName(task.getFilePath());
        response.setStatus(task.getStatus().name());
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedTime(task.getCreatedTime());
        response.setUpdatedTime(task.getUpdatedTime());
        response.setCompletedTime(task.getCompletedTime());
        return response;
    }

    // ==================== 批量回滚 ====================

    /**
     * 批量设备回滚（外部调用，会发送TaskInfo通知）
     * 
     * ⭐ 关键特性：
     * - 每个设备对应一个回滚文件（deviceIds 和 fileNames 按顺序一一对应）
     * - 使用 CompletableFuture + 线程池实现并发处理
     * - 单个设备失败不影响其他设备（异常隔离）
     * - TaskInfo 通知、operator、endTime 处理与 batch backup 完全一致
     */
    public SoftwareRollbackDto.BatchRollbackSummary batchRollback(SoftwareRollbackDto.BatchRollbackRequest request) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [批量回滚] 创建批量设备回滚任务（并发处理）");
        log.info("  批次名称: {}", request.getBatchName());
        log.info("  设备数量: {}", request.getDeviceIds().size());
        log.info("  SFTP服务器: {}", request.getSftpServerName());
        log.info("  最大并发数: 20 (受线程池控制)");

        // 1. 参数校验：deviceIds 和 fileNames 数量必须一致
        if (request.getDeviceIds().size() != request.getFileNames().size()) {
            throw new IllegalArgumentException(
                String.format("设备ID数量(%d)与文件名数量(%d)不一致，必须一一对应",
                    request.getDeviceIds().size(), request.getFileNames().size()));
        }

        // 2. 生成批次ID和统一的 actionTime
        String batchId = UUID.randomUUID().toString();
        String batchName = request.getBatchName();
        Long batchActionTime = System.currentTimeMillis();

        log.info("  批次ID: {}", batchId);
        log.info("  批次ActionTime: {}", batchActionTime);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 3. 创建并保存 Batch 实体
        BatchHelper.BatchCreationContext context = new BatchHelper.BatchCreationContext();
        context.setSftpServerName(request.getSftpServerName());
        context.setRemark(request.getRemark());
        context.setScheduledTime(request.getScheduledTime());

        Batch.BatchStatus initialStatus = (request.getScheduledTime() != null) 
            ? Batch.BatchStatus.SCHEDULED 
            : Batch.BatchStatus.RUNNING;

        Batch batch = BatchHelper.createBatch(
            batchId,
            batchName,
            batchActionTime,
            Batch.BatchType.ROLLBACK,
            request.getDeviceIds().size(),
            initialStatus,
            context
        );
        batchRepository.save(batch);

        log.info("  ✅ 批次记录已创建: batchId={}, status={}", batchId, initialStatus);

        // 4. 如果是定时任务，创建定时任务记录并返回
        if (request.getScheduledTime() != null) {
            return createScheduledBatchRollback(request, batchId, batchActionTime, batchName);
        }

        // 5. 立即执行：使用CompletableFuture + 线程池实现并发处理
        List<CompletableFuture<SoftwareRollbackDto.DeviceRollbackResult>> futures = new ArrayList<>();

        for (int i = 0; i < request.getDeviceIds().size(); i++) {
            String deviceId = request.getDeviceIds().get(i);
            String fileName = request.getFileNames().get(i);

            CompletableFuture<SoftwareRollbackDto.DeviceRollbackResult> future = 
                CompletableFuture.supplyAsync(() -> {
                    return processDeviceRollback(deviceId, fileName, request.getSftpServerName(),
                        batchId, batchActionTime, batchName);
                }, batchTaskExecutor);

            futures.add(future);
        }

        log.info("📤 已提交 {} 个并发回滚任务到线程池", futures.size());

        // 等待所有任务完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();

        // 收集所有结果
        List<SoftwareRollbackDto.DeviceRollbackResult> deviceResults = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // 6. 发送 TaskInfo 批次通知
        List<DeviceTask> deviceTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);

        try {
            String createdBy = batch.getCreatedBy();
            taskInfoNotificationService.notifyBatchTaskStarted(
                batchId,
                batchName,
                DeviceTask.TaskType.ROLLBACK,
                deviceTasks,
                batchActionTime,
                createdBy
            );
            log.info("📨 已发送批次级别 TaskInfo 通知");
        } catch (Exception e) {
            log.warn("发送批次TaskInfo通知失败（不影响任务执行）: {}", e.getMessage());
        }

        // 7. 统计结果
        int runningCount = (int) deviceResults.stream().filter(r -> "RUNNING".equals(r.getStatus())).count();
        int failedCount = (int) deviceResults.stream().filter(r -> "FAILED".equals(r.getStatus())).count();

        // 8. 更新 Batch 状态
        Batch.BatchStatus finalStatus;
        if (failedCount == 0) {
            finalStatus = Batch.BatchStatus.RUNNING;
        } else if (failedCount == deviceResults.size()) {
            finalStatus = Batch.BatchStatus.FAILED;
        } else {
            finalStatus = Batch.BatchStatus.RUNNING;
        }

        batch.setStatus(finalStatus);
        batchRepository.save(batch);

        log.info("  ✅ 批次状态已更新: status={}", finalStatus);

        // 9. 构建批量响应
        SoftwareRollbackDto.BatchRollbackSummary summary = new SoftwareRollbackDto.BatchRollbackSummary();
        summary.setBatchId(batchId);
        summary.setBatchName(batchName);
        summary.setSftpServerName(request.getSftpServerName());
        summary.setTotalDevices(request.getDeviceIds().size());
        summary.setRunningDevices(runningCount);
        summary.setFailedDevices(failedCount);
        summary.setStatus(finalStatus.name());
        summary.setRemark(request.getRemark());
        summary.setScheduledTime(request.getScheduledTime());
        summary.setCreatedTime(batch.getCreatedTime());
        summary.setUpdatedTime(batch.getUpdatedTime());
        summary.setDeviceResults(deviceResults);

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ 批量回滚任务已提交完成");
        log.info("  批次ID: {}", batchId);
        log.info("  设备总数: {}", request.getDeviceIds().size());
        log.info("  运行中: {}", runningCount);
        log.info("  失败: {}", failedCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return summary;
    }

    /**
     * 处理单个设备的回滚（线程池中执行）
     * 
     * @param deviceId 设备ID
     * @param fileName 回滚文件名
     * @param sftpServerName SFTP服务器名称
     * @param batchId 批次ID
     * @param batchActionTime 批次统一的 actionTime
     * @param batchName 批次名称
     */
    private SoftwareRollbackDto.DeviceRollbackResult processDeviceRollback(
            String deviceId,
            String fileName,
            String sftpServerName,
            String batchId,
            Long batchActionTime,
            String batchName) {

        String deviceName = null;
        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;

        try {
            // 1. 获取设备友好名称和IP
            try {
                deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                deviceName = deviceInfo.getFriendlyName();
                log.debug("  设备名称: {}, IP: {}", deviceName, deviceInfo.getIp());
            } catch (Exception e) {
                log.warn("  ⚠️ 无法获取设备名称: {}，使用设备ID", deviceId);
            }

            // 2. 检查设备可用性
            uniquenessService.checkAndGetAvailableTask(deviceId, DeviceTask.TaskType.ROLLBACK, batchId);

            // 3. 创建回滚任务
            String taskId = taskCreationHelper.generateTaskId();
            DeviceTask task = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.ROLLBACK);

            task.setBatchId(batchId);
            task.setBatchName(batchName);
            task.setSftpServerName(sftpServerName);
            task.setFilePath(fileName);
            task.setTaskInfoActionTime(batchActionTime);
            task.setStatus(DeviceTask.TaskStatus.PENDING);
            task.setCreatedTime(LocalDateTime.now());
            task.setUpdatedTime(LocalDateTime.now());

            // ⭐ 使用统一方法设置所有设备信息
            taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);

            // 保存任务
            DeviceTask savedTask = deviceTaskRepository.save(task);
            log.info("✅ 任务记录已保存到数据库");
            log.info("  任务ID: {}", savedTask.getTaskId());
            log.info("  TaskInfo ActionTime: {}", batchActionTime);
            removeReadyToCommitUpgradeWorkflowsForRollback(deviceId);

            // 4. 执行回滚（立即执行）
            log.info("🔧 执行模式: Real (真实neMgr RPC)");
            executeRealRollback(savedTask, false);

            // 构建设备回滚结果
            SoftwareRollbackDto.DeviceRollbackResult result = new SoftwareRollbackDto.DeviceRollbackResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setTaskId(savedTask.getTaskId());
            result.setFileName(fileName);
            result.setStatus("RUNNING");
            result.setRpcResult("Rollback task created successfully");
            result.setCreatedTime(savedTask.getCreatedTime());

            log.debug("  ✓ [{}] 回滚文件: {} - 任务ID: {}",
                deviceName != null ? deviceName : deviceId,
                fileName,
                savedTask.getTaskId());

            return result;

        } catch (Exception e) {
            log.error("  ✗ [{}] 回滚任务创建失败: {}",
                deviceName != null ? deviceName : deviceId,
                e.getMessage());

            SoftwareRollbackDto.DeviceRollbackResult result = new SoftwareRollbackDto.DeviceRollbackResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setTaskId(null);
            result.setFileName(fileName);
            result.setStatus("FAILED");
            result.setRpcResult(null);
            result.setErrorMessage(e.getMessage());
            result.setCreatedTime(LocalDateTime.now());

            return result;
        }
    }

    /**
     * 创建定时批量回滚任务
     */
    private SoftwareRollbackDto.BatchRollbackSummary createScheduledBatchRollback(
            SoftwareRollbackDto.BatchRollbackRequest request,
            String batchId,
            Long batchActionTime,
            String batchName) {

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("⏰ [定时回滚] 创建定时批量回滚任务");
        log.info("  批次ID: {}", batchId);
        log.info("  批次名称: {}", batchName);
        log.info("  定时执行时间: {}", request.getScheduledTime());
        log.info("  设备数量: {}", request.getDeviceIds().size());

        // 为每个设备创建 SCHEDULED 状态的 DeviceTask
        List<SoftwareRollbackDto.DeviceRollbackResult> deviceResults = new ArrayList<>();

        for (int i = 0; i < request.getDeviceIds().size(); i++) {
            String deviceId = request.getDeviceIds().get(i);
            String fileName = request.getFileNames().get(i);

            try {
                // 获取设备信息
                String deviceFriendlyName = null;
                NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;
                try {
                    deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                    deviceFriendlyName = deviceInfo.getFriendlyName();
                } catch (Exception e) {
                    log.warn("  获取设备信息失败: deviceId={}, 错误: {}", deviceId, e.getMessage());
                }

                // 检查设备可用性
                uniquenessService.checkAndGetAvailableTask(deviceId, DeviceTask.TaskType.ROLLBACK, batchId);

                // 创建定时任务记录
                DeviceTask task = new DeviceTask();
                task.setTaskId(UUID.randomUUID().toString());
                task.setTaskType(DeviceTask.TaskType.ROLLBACK);
                task.setDeviceId(deviceId);

                // 设置批次信息
                task.setBatchId(batchId);
                task.setBatchName(batchName);
                task.setSftpServerName(request.getSftpServerName());
                task.setFilePath(fileName);
                task.setScheduledTime(request.getScheduledTime());
                task.setTaskInfoActionTime(batchActionTime);
                task.setStatus(DeviceTask.TaskStatus.SCHEDULED);
                task.setCreatedTime(LocalDateTime.now());
                task.setUpdatedTime(LocalDateTime.now());

                // 设置设备信息
                taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);

                // 保存任务
                deviceTaskRepository.save(task);
                removeReadyToCommitUpgradeWorkflowsForRollback(deviceId);

                SoftwareRollbackDto.DeviceRollbackResult result = new SoftwareRollbackDto.DeviceRollbackResult();
                result.setDeviceId(deviceId);
                result.setDeviceName(deviceFriendlyName);
                result.setTaskId(task.getTaskId());
                result.setFileName(fileName);
                result.setStatus("SCHEDULED");
                result.setScheduledTime(request.getScheduledTime());
                deviceResults.add(result);

                log.info("  ✅ 设备任务已创建: deviceId={}, taskId={}, status=SCHEDULED", deviceId, task.getTaskId());

            } catch (Exception e) {
                log.error("  ❌ 创建设备任务失败: deviceId={}, 错误: {}", deviceId, e.getMessage(), e);

                SoftwareRollbackDto.DeviceRollbackResult result = new SoftwareRollbackDto.DeviceRollbackResult();
                result.setDeviceId(deviceId);
                result.setFileName(fileName);
                result.setStatus("FAILED");
                result.setErrorMessage("Task creation failed: " + e.getMessage());
                deviceResults.add(result);
            }
        }

        // 发送 TaskInfo 批次通知（定时任务创建）
        try {
            List<DeviceTask> deviceTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
            String createdBy = batchRepository.findById(batchId)
                .map(Batch::getCreatedBy)
                .orElse(null);

            taskInfoNotificationService.notifyBatchTaskStarted(
                batchId,
                batchName,
                DeviceTask.TaskType.ROLLBACK,
                deviceTasks,
                batchActionTime,
                createdBy
            );
            log.info("  ✅ TaskInfo 批次通知已发送 (SCHEDULED - 任务已创建)");
        } catch (Exception e) {
            log.warn("  ⚠️ 发送 TaskInfo 批次通知失败: {}", e.getMessage());
        }

        // 构建响应
        SoftwareRollbackDto.BatchRollbackSummary summary = new SoftwareRollbackDto.BatchRollbackSummary();
        summary.setBatchId(batchId);
        summary.setBatchName(batchName);
        summary.setSftpServerName(request.getSftpServerName());
        summary.setTotalDevices(request.getDeviceIds().size());
        summary.setScheduledDevices(deviceResults.size());
        summary.setStatus("SCHEDULED");
        summary.setRemark(request.getRemark());
        summary.setScheduledTime(request.getScheduledTime());
        summary.setDeviceResults(deviceResults);

        log.info("  ✅ 定时批量回滚任务已创建");
        log.info("  批次ID: {}", batchId);
        log.info("  定时执行时间: {}", request.getScheduledTime());
        log.info("  总设备数: {}", summary.getTotalDevices());
        log.info("  已调度设备数: {}", summary.getScheduledDevices());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return summary;
    }

    /**
     * 设备一旦被拿去执行 rollback，就不再保留在原升级批次的 READY_TO_COMMIT 集合中。
     * 这样即使 rollback 最终失败，用户回到原升级批次时也不会再把该设备一起 commit。
     */
    private void removeReadyToCommitUpgradeWorkflowsForRollback(String deviceId) {
        try {
            List<UpgradeWorkflow> workflows = workflowRepository.findByDeviceId(deviceId);
            if (workflows.isEmpty()) {
                return;
            }

            workflowManagementService.computeAndSetWorkflowStatuses(workflows);

            for (UpgradeWorkflow workflow : workflows) {
                if (workflow.getStatus() != UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS) {
                    continue;
                }

                Optional<Batch> batchOpt = batchRepository.findById(workflow.getBatchId());
                if (!batchOpt.isPresent()) {
                    continue;
                }

                Batch batch = batchOpt.get();
                if (batch.getBatchType() != Batch.BatchType.UPGRADE ||
                    batch.getStatus() != Batch.BatchStatus.READY_TO_COMMIT) {
                    continue;
                }

                log.info("🔄 rollback 创建后移除原升级 workflow: batchId={}, deviceId={}, workflowId={}",
                    workflow.getBatchId(), deviceId, workflow.getWorkflowId());
                batchUpgradeService.removeWorkflow(workflow.getBatchId(), deviceId);
            }
        } catch (Exception e) {
            log.error("rollback 创建后清理原升级 workflow 失败: deviceId={}", deviceId, e);
        }
    }
}

