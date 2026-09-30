package devicemaintenance.service;

import devicemaintenance.dto.SoftwareDownloadDto;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.helper.BatchHelper;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.utils.DeviceMaintenanceLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
import org.springframework.beans.BeanUtils;
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
 * 设备软件下载服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SoftwareDownloadService {

    private final DeviceTaskRepository deviceTaskRepository;
    private final BatchRepository batchRepository;
    private final TaskInfoNotificationService taskInfoNotificationService;
    private final TaskCreationHelper taskCreationHelper;
    private final RealServiceExecutionHelper executionHelper;
    private final RpcDebugHelper rpcDebugHelper;
    private final DeviceTaskUniquenessService uniquenessService;
    private final PreValidationService preValidationService;
    
    @Autowired(required = false)
    private NeMgrIntegrationService neMgrIntegrationService;
    
    /**
     * 批次任务执行线程池（统一线程池）
     * 用于并发下发所有类型的 RPC 操作，最大并发数20
     */
    @Autowired
    @Qualifier("batchTaskExecutor")
    private Executor batchTaskExecutor;

    /**
     * 启动设备软件下载任务
     * 
     * @param request 下载请求
     * @param debug 调试模式：true时响应中包含发送给neMgr的完整payload
     */
    @Transactional
    public SoftwareDownloadDto.DownloadResponse startDownload(SoftwareDownloadDto.DownloadRequest request, boolean debug) {
        return startDownload(request, debug, null, null, null);
    }

    /**
     * 启动设备软件下载任务（批次模式）
     * 使用独立事务避免批量创建时的锁冲突
     * 
     * @param request 下载请求
     * @param debug 调试模式：true时响应中包含发送给neMgr的完整payload
     * @param batchId 批次ID（可选，批次任务时传入）
     */
    public SoftwareDownloadDto.DownloadResponse startDownload(SoftwareDownloadDto.DownloadRequest request, boolean debug, String batchId) {
        return startDownload(request, debug, batchId, null, null);
    }
    
    /**
     * 启动设备软件下载任务（workflow 模式，兼容旧调用）
     * 
     * @param request 下载请求
     * @param debug 调试模式
     * @param batchId 批次ID
     * @param workflowId workflow ID
     */
    public SoftwareDownloadDto.DownloadResponse startDownload(
            SoftwareDownloadDto.DownloadRequest request, 
            boolean debug, 
            String batchId, 
            String workflowId) {
        return startDownload(request, debug, batchId, workflowId, null);
    }

    /**
     * 启动设备软件下载任务（workflow 模式）
     * 
     * @param request 下载请求
     * @param debug 调试模式
     * @param batchId 批次ID
     * @param workflowId workflow ID（workflow 任务时传入）
     * @param cachedDeviceInfo 缓存的设备信息（避免重复查询，可为null）
     */
    // ⚠️ 移除事务！失败就失败，不需要回滚！
    public SoftwareDownloadDto.DownloadResponse startDownload(
            SoftwareDownloadDto.DownloadRequest request, 
            boolean debug, 
            String batchId, 
            String workflowId,
            NeMgrIntegrationService.DevicePhysicalInfo cachedDeviceInfo) {
        DeviceMaintenanceLogContext.setIdentifiers(null, batchId, workflowId, request.getDeviceId(), null, "DOWNLOAD");
        DeviceMaintenanceLogContext.setPhase("API_IN", "start-download");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [任务创建] 软件下载任务");
        log.info("  设备ID: {}", request.getDeviceId());
        log.info("  文件路径: {}", request.getFilePath());
        log.info("  SFTP服务器: {}", request.getSftpServerName() != null ? request.getSftpServerName() : "未指定");
        log.info("request accepted scheduledTime={} debug={}", request.getScheduledTime(), debug);

        // ✅ 优先使用缓存的设备信息（批量操作传入），避免重复查询
        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = cachedDeviceInfo;
        
        // ✅ 对于立即执行的任务，如果没有缓存的设备信息，则验证设备和SFTP服务器
        // ⚠️ 验证失败直接抛异常，不创建任务
        if (request.getScheduledTime() == null && deviceInfo == null) {
            long precheckStart = System.currentTimeMillis();
            DeviceMaintenanceLogContext.setPhase("PRECHECK", "validate-device-and-sftp");
            log.info("🔍 [Pre-validation] Validating device and SFTP server...");
            try {
                // 1. 验证设备是否存在，并获取设备信息
                deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(request.getDeviceId());
                log.info("  ✓ Device validated: {} ({})", deviceInfo.getFriendlyName(), request.getDeviceId());
                log.info("  ✓ Device IP: {}, Vendor: {}, Version: {}", 
                    deviceInfo.getIp(), deviceInfo.getVendorType(), deviceInfo.getSoftwareVersion());
                
                // 2. 验证SFTP服务器是否存在
                if (request.getSftpServerName() != null) {
                    neMgrIntegrationService.getSftpServerById(request.getSftpServerName());
                    log.info("  ✓ SFTP server validated: {}", request.getSftpServerName());
                }
                log.info("precheck success elapsedMs={}", System.currentTimeMillis() - precheckStart);
            } catch (Exception e) {
                log.error("precheck failed elapsedMs={} error={}", System.currentTimeMillis() - precheckStart, e.getMessage());
                throw new RuntimeException("Pre-validation failed: " + e.getMessage(), e);
            }
        } else if (deviceInfo != null) {
            log.debug("  ✓ 使用缓存的设备信息: {}", deviceInfo.getFriendlyName());
        }

        // 创建任务记录（只有预验证通过才会到这里）
        DeviceTask task = createDeviceTask(request, batchId, workflowId);
        
        // ✅ 如果获取到了设备信息，保存到任务记录中
        if (deviceInfo != null) {
            task.setDeviceName(deviceInfo.getFriendlyName());
            task.setDeviceIp(deviceInfo.getIp());
            task.setVendorType(deviceInfo.getVendorType());        // ⭐ 保存厂商类型
            task.setVendorName(deviceInfo.getVendorName());        // ⭐ 保存厂商名称
            task.setCurrentVersion(deviceInfo.getSoftwareVersion()); // ⭐ 保存当前软件版本
            if (task.getPreviousVersion() == null) {
                task.setPreviousVersion(deviceInfo.getSoftwareVersion());
            }
            log.info("  ✓ Device info set: name={}, ip={}, vendor={}/{}, currentVersion={}", 
                deviceInfo.getFriendlyName(), deviceInfo.getIp(), 
                deviceInfo.getVendorType(), deviceInfo.getVendorName(), 
                deviceInfo.getSoftwareVersion());
        }
        
        DeviceMaintenanceLogContext.setPhase("TASK_PERSIST", "save-device-task");
        DeviceTask savedTask = deviceTaskRepository.save(task);
        DeviceMaintenanceLogContext.setTaskContext(savedTask);

        log.info("✅ 任务记录已保存到数据库");
        log.info("  任务ID: {}", savedTask.getTaskId());
        log.info("  初始状态: {}", savedTask.getStatus());
        log.info("task persisted source={} batchMode={}", task.getBatchId() == null ? "single" : "batch", task.getBatchId() != null);
        if (savedTask.getScheduledTime() != null) {
            log.info("  计划执行时间: {}", savedTask.getScheduledTime());
        }

        // 准备响应（debug模式下会包含payload）
        java.util.Map<String, Object> debugPayload = null;
        
        // 只有非定时任务（立即执行）才调用执行服务
        DeviceTask finalTask = savedTask;
        if (taskCreationHelper.shouldExecuteImmediately(savedTask)) {
            DeviceMaintenanceLogContext.setPhase("RPC_SEND", "software-download");
            log.info("🔧 执行模式: Real (真实neMgr RPC)");
            debugPayload = executeRealDownload(savedTask, debug);
            
            // 检查执行结果：如果任务失败，记录但不抛异常（保留DeviceTask记录）
            // ⚠️ 移除flush()调用，避免与外层事务冲突导致死锁
            DeviceTask updatedTask = deviceTaskRepository.findById(savedTask.getTaskId()).orElse(savedTask);
            finalTask = updatedTask;  // 使用更新后的任务状态
            
            if (updatedTask.getStatus() == DeviceTask.TaskStatus.FAILED) {
                String errorMsg = updatedTask.getErrorMessage() != null ? 
                    updatedTask.getErrorMessage() : "任务执行失败";
                log.error("❌ 任务执行失败: {}", errorMsg);
                log.warn("⚠️ 任务失败但DeviceTask记录已保留");
                // ⚠️ 不抛异常，让失败的DeviceTask保留在数据库中
                // 对于批量操作，调用方会检查返回的status来判断成功/失败
            } else {
                log.info("✅ 任务执行成功");
            }
            
            // ⚠️ 单步下载任务不发送TaskInfo通知
            log.debug("  ℹ️ 单步下载任务不发送TaskInfo通知");
        } else {
            // 定时任务：稍后由调度器执行
            log.info("⏰ 定时任务模式 - 等待调度器执行");
            log.info("  计划时间: {}", savedTask.getScheduledTime());
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        SoftwareDownloadDto.DownloadResponse response = convertToResponse(finalTask);
        if (debug && debugPayload != null) {
            response.setDebugPayload(debugPayload);
            log.info("🐛 调试模式: 已包含debugPayload字段");
        }
        DeviceMaintenanceLogContext.setPhase("API_OUT", "start-download");
        log.info("request completed finalStatus={}", finalTask.getStatus());
        DeviceMaintenanceLogContext.clearAll();
        return response;
    }

    // updateTaskStatus 方法已删除 - 任务状态更新现在由内部服务直接处理
    // Mock服务直接操作数据库，真实服务通过Kafka消息处理器更新状态

    // 任务查询功能已移动到 DeviceTaskController 中

    /**
     * 创建或复用设备任务记录
     * 
     * ⭐ 两种模式：
     * 1. Workflow 内任务：复用现有任务记录（更新类型和状态）
     * 2. 独立批次任务：创建新任务记录
     */
    private DeviceTask createDeviceTask(SoftwareDownloadDto.DownloadRequest request, String batchId, String workflowId) {
        // ✅ 可用性检查：检查设备是否有未完成任务
        // 如果是 workflow 任务且前一步骤已完成，返回该任务供复用
        DeviceTask existingTask = uniquenessService.checkAndGetAvailableTask(
            request.getDeviceId(),
            DeviceTask.TaskType.DOWNLOAD,
            batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
        );
        
        DeviceTask task;
        
        if (existingTask != null) {
            // ⭐ 复用模式：更新现有任务
            task = existingTask;
            log.info("  ♻️  复用现有任务: taskId={}, 前状态={}",
                task.getTaskId(), task.getStatus());

            // 更新任务类型和状态
            task.setTaskType(DeviceTask.TaskType.DOWNLOAD);
            task.setStatus(DeviceTask.TaskStatus.PENDING);  // 重置为 PENDING
            task.setErrorMessage(null);  // 清除之前的错误信息
            task.setCompletedTime(null);  // 清除完成时间

            log.info("  ✅ 任务已更新为 DOWNLOAD 类型");

        } else {
            // ⭐ 新建模式：创建新任务记录
            String taskId = taskCreationHelper.generateTaskId();
            task = new DeviceTask(taskId, request.getDeviceId(), DeviceTask.TaskType.DOWNLOAD);
            log.info("  ✅ 创建新下载任务: taskId={}", taskId);
            
            // 设置批次ID和workflow ID（可能为null，这是正常的）
            task.setBatchId(batchId);
            task.setWorkflowId(workflowId);
            task.setRetryCount(0);
        }
        
        // 设置/更新任务参数（无论新建还是复用都需要）
        task.setFilePath(request.getFilePath());
        
        // 直接存储SFTP服务器name（如果提供了）
        if (request.getSftpServerName() != null) {
            task.setSftpServerName(request.getSftpServerName());
        }
        
        // 使用统一的定时任务配置逻辑
        taskCreationHelper.configureTaskScheduling(task, request.getScheduledTime());

        return task;
    }

    private java.util.Map<String, Object> executeRealDownload(DeviceTask task, boolean debug) {
        DeviceMaintenanceLogContext.setTaskContext(task);
        DeviceMaintenanceLogContext.setPhase("RPC_SEND", "software-download");
        long rpcStart = System.currentTimeMillis();
        log.info("rpc dispatch start sftpServer={} filePath={} debug={}",
                task.getSftpServerName(), task.getFilePath(), debug);
        
        java.util.Map<String, Object> debugPayload = null;
        try {
            executionHelper.updateTaskToRunning(task);
            
            // 调用完整版本的softwareDownload（包含SFTP和telnet-info）
            NeMgrIntegrationService.RpcResult result = neMgrIntegrationService.softwareDownload(
                task.getDeviceId(),          // nodeId (Site-xxx#Ne-xxx)
                task.getSftpServerName(),    // sftpServerName（实际是SFTP服务器的name值）
                task.getFilePath()           // 完整文件路径
            );
            
            // ✅ 保存设备友好名称（统一方法）
            rpcDebugHelper.saveFriendlyNameIfPresent(task, result);
            
            // ✅ 保存 debug payload 并持久化（统一方法）
            if (result != null) {
                rpcDebugHelper.saveDebugPayloadAndPersist(task, result.getDebugPayload(), debug);
                debugPayload = result.getDebugPayload();
            }
            
            // ✅ 检查RPC响应：验证命令是否成功下发
            if (result == null || result.getOutput() == null) {
                log.error("rpc ack invalid elapsedMs={} reason=nullOutput", System.currentTimeMillis() - rpcStart);
                // RPC调用失败（返回null），标记为失败
                executionHelper.completeTaskFailure(task, "Software Download", "RPC call failed, returned null", 
                    result != null ? result.getDebugPayload() : null, null, debug);
            } else {
                // ✅ 检查 RPC 响应的 result 字段
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput output = 
                    (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput) result.getOutput();
                String rpcResult = output.getResult();
                
                // ✅ 使用统一的失败判断方法
                if (executionHelper.isFailureResult(rpcResult)) {
                    // ❌ RPC 返回失败
                    log.error("rpc ack failed elapsedMs={} result={}", System.currentTimeMillis() - rpcStart, rpcResult);
                    executionHelper.completeTaskFailure(task, "Software Download", "RPC execution failed, result: " + rpcResult, 
                        result.getDebugPayload(), output, debug);
                } else {
                    // ✅ RPC调用成功，命令已下发，状态保持为RUNNING
                    // 等待Kafka异步通知更新最终状态
                    DeviceMaintenanceLogContext.setPhase("RPC_ACK", "software-download");
                    log.info("rpc ack success elapsedMs={} result={}", System.currentTimeMillis() - rpcStart, rpcResult);
                    DeviceMaintenanceLogContext.setPhase("ASYNC_WAIT", "system-change");
                    log.info("waiting for async notification timeoutManagedByPolling=true");
                }
            }
            
        } catch (Exception e) {
            // 传入请求参数用于调试（异常情况下无法获取完整的 debugPayload）
            executionHelper.handleTaskError(task, "Software Download", e, null);
        } finally {
            DeviceMaintenanceLogContext.clearPhase();
        }
        return debugPayload;
    }

    /**
     * 执行定时任务（供调度器调用）
     * 
     * @param task 待执行的任务（状态已更新为PENDING）
     */
    public void executeScheduledTask(DeviceTask task) {
        log.info("Executing scheduled download task: taskId={}, deviceId={}", task.getTaskId(), task.getDeviceId());
        
        log.info("Using real service for scheduled download task: {}", task.getTaskId());
        
        // ✅ 定时任务不需要 debug（或从 Batch 获取，但这里简化为 false）
        executeRealDownload(task, false);
        
        // ⚠️ 单步下载任务（包括定时任务）不发送TaskInfo通知
        // 批次级别的通知由 SystemChangeNotificationListener 处理
        log.debug("  ℹ️ 单步下载定时任务不发送TaskInfo通知");
    }

    /**
     * 批量软件下载（外部调用，会发送TaskInfo通知）
     * 说明：不同设备下载同一个文件，使用同一个SFTP
     */
    public SoftwareDownloadDto.BatchDownloadResponse batchDownload(SoftwareDownloadDto.BatchDownloadRequest request) {
        return batchDownload(request, false);  // 外部调用，发送通知
    }
    
    /**
     * 批量下载（支持外部调用和内部调用）
     * @param request 下载请求
     * @param isInternalCall 是否为内部调用（true=不发送TaskInfo通知，false=发送通知）
     */
    public SoftwareDownloadDto.BatchDownloadResponse batchDownload(
            SoftwareDownloadDto.BatchDownloadRequest request, 
            boolean isInternalCall) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [批量下载] 创建批量软件下载任务（并发处理）");
        log.info("  设备数量: {}", request.getDeviceIds().size());
        log.info("  软件包: {} (所有设备下载同一文件)", request.getSoftwarePackage());
        log.info("  SFTP服务器: {} (所有设备使用同一SFTP)", request.getSftpServerId());
        log.info("  定时执行: {}", request.getScheduledTime() != null ? request.getScheduledTime() : "立即执行");
        log.info("  Debug模式: {}", request.getDebug() != null && request.getDebug());
        
        // ⭐ 判断是否为批量模式（基于batchName是否提供）
        boolean isBatchMode = org.springframework.util.StringUtils.hasText(request.getBatchName());
        // ⭐ 工作流内部调用：使用提供的 batchId（不创建新Batch）
        boolean isWorkflowCall = org.springframework.util.StringUtils.hasText(request.getBatchId());
        
        final String batchName = request.getBatchName();
        final String batchId;
        final Long batchActionTime;
        final java.util.Map<String, String> workflowIds = request.getWorkflowIds();
        
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
        
        // ⭐ 如果是定时执行，使用定时任务逻辑
        if (request.getScheduledTime() != null) {
            return createScheduledBatchDownload(request, batchId, batchActionTime, batchName, isInternalCall);
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ 严格模式 Pre-validation：一个失败全失败
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        PreValidationService.BatchValidationResult validationResult = 
            preValidationService.validateBatchStrict(
                request.getDeviceIds(),
                DeviceTask.TaskType.DOWNLOAD,
                request.getSftpServerId()
            );
        
        if (!validationResult.isAllValid()) {
            // ❌ Pre-validation 失败，不创建任何资源
            PreValidationService.DeviceValidationResult firstFailure = validationResult.getFirstFailure();
            String errorMsg = String.format(
                "批量下载 Pre-validation 失败：设备 %s - %s",
                firstFailure.getDeviceId(),
                firstFailure.getErrorMessage()
            );
            log.error("❌ {}", errorMsg);
            log.error("⚠️  批次创建失败，不创建任何 Batch 和 DeviceTask");
            
            // 构建失败响应
            SoftwareDownloadDto.BatchDownloadResponse response = new SoftwareDownloadDto.BatchDownloadResponse();
            response.setBatchName(batchName);
            response.setSoftwarePackage(request.getSoftwarePackage());
            response.setSftpServerId(request.getSftpServerId());
            response.setTotalDevices(request.getDeviceIds().size());
            response.setSuccessCount(0);
            response.setFailedCount(request.getDeviceIds().size());
            response.setStatus("FAILED");
            response.setCreatedTime(LocalDateTime.now());
            
            // 构建设备结果列表（所有设备都失败）
            List<SoftwareDownloadDto.DeviceDownloadResult> deviceResults = new ArrayList<>();
            for (PreValidationService.DeviceValidationResult deviceResult : validationResult.getDeviceResults()) {
                SoftwareDownloadDto.DeviceDownloadResult result = new SoftwareDownloadDto.DeviceDownloadResult();
                result.setDeviceId(deviceResult.getDeviceId());
                result.setDeviceName(null);
                result.setTaskId(null);
                result.setStatus("FAILED");
                result.setRpcResult(null);
                result.setErrorMessage(deviceResult.getErrorMessage());
                result.setCreatedTime(LocalDateTime.now());
                deviceResults.add(result);
            }
            response.setDeviceResults(deviceResults);
            
            return response;
        }
        
        log.info("✅ 批量 Pre-validation 全部通过，开始创建批次和任务");
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ 创建 Batch 实体（Pre-validation 全部通过后）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        Batch batch = null;
        if (isBatchMode && !isWorkflowCall) {
            BatchHelper.BatchCreationContext context = new BatchHelper.BatchCreationContext();
            context.setSftpServerName(request.getSftpServerId());
            context.setRemark(request.getRemark());
            context.setDebug(request.getDebug());
            
            batch = BatchHelper.createBatch(
                batchId,
                batchName,
                batchActionTime,
                Batch.BatchType.DOWNLOAD,
                request.getDeviceIds().size(),
                Batch.BatchStatus.RUNNING,
                context
            );
            batchRepository.save(batch);
            
            log.info("  ✅ 批次记录已创建: batchId={}, status=RUNNING", batchId);
        }
        
        // ✅ 立即执行：使用CompletableFuture + 线程池实现并发处理
        // ⭐ 将预验证获取的设备信息传递下去，避免重复查询
        final java.util.Map<String, NeMgrIntegrationService.DevicePhysicalInfo> deviceInfoMap = 
            validationResult.getDeviceInfoMap();
        
        List<CompletableFuture<SoftwareDownloadDto.DeviceDownloadResult>> futures = 
            request.getDeviceIds().stream()
            .map(deviceId -> CompletableFuture.supplyAsync(() -> {
                // ⭐ 获取该设备对应的 workflowId（如果是工作流内部调用）
                String workflowId = (workflowIds != null) ? workflowIds.get(deviceId) : null;
                NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = deviceInfoMap.get(deviceId);
                return processDeviceDownload(deviceId, request, batchId, batchActionTime, batchName, workflowId, deviceInfo);
            }, batchTaskExecutor)) // ✅ 使用统一线程池，最大并发数20
            .collect(Collectors.toList());
        
        log.info("📤 已提交 {} 个并发下载任务到线程池", futures.size());
        
        // 等待所有任务完成（受线程池大小限制并发数）
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join(); // 阻塞等待所有任务完成
        
        // 收集所有结果
        List<SoftwareDownloadDto.DeviceDownloadResult> deviceResults = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        // ⭐ 如果是批量模式 且 不是内部调用，查询所有设备任务并发送 TaskInfo 批次通知
        if (isBatchMode && !isInternalCall) {
            List<DeviceTask> deviceTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
            
            try {
                // ⭐ 批次创建通知：从数据库查询 Batch 对象获取 createdBy（第一次通知，必须设置 operator）
                String createdBy = batchRepository.findById(batchId)
                    .map(devicemaintenance.entity.Batch::getCreatedBy)
                    .orElse(null);
                taskInfoNotificationService.notifyBatchTaskStarted(
                    batchId,
                    batchName,
                    DeviceTask.TaskType.DOWNLOAD,
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
        int successCount = (int) deviceResults.stream().filter(r -> "RUNNING".equals(r.getStatus())).count();
        int failedCount = (int) deviceResults.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        
        // ⭐ 如果是批量模式，更新 Batch 状态
        if (isBatchMode) {
            Batch.BatchStatus finalStatus;
            if (failedCount == 0) {
                // 全部成功（RUNNING状态等待后续通知）
                finalStatus = Batch.BatchStatus.RUNNING;
            } else if (successCount == 0) {
                // 全部失败
                finalStatus = Batch.BatchStatus.FAILED;
            } else {
                // 部分成功/失败（还在运行中）
                finalStatus = Batch.BatchStatus.RUNNING;
            }
            
            batch.setStatus(finalStatus);
            batchRepository.save(batch);
            
            log.info("  ✅ 批次状态已更新: status={}", finalStatus);
        }
        
        // 构建批量响应
        SoftwareDownloadDto.BatchDownloadResponse response = new SoftwareDownloadDto.BatchDownloadResponse();
        response.setBatchName(batchName);
        response.setSoftwarePackage(request.getSoftwarePackage());
        response.setSftpServerId(request.getSftpServerId());
        response.setTotalDevices(request.getDeviceIds().size());
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setStatus(failedCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAILED" : "PARTIAL_SUCCESS"));
        response.setCreatedTime(LocalDateTime.now());
        response.setDeviceResults(deviceResults);
        
        log.info("✅ 批量下载任务创建完成（并发处理）");
        log.info("  成功: {}, 失败: {}", successCount, failedCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return response;
    }
    
    /**
     * 创建定时批量下载任务
     * ⭐ 所有设备在指定时间一起进入线程池执行
     */
    private SoftwareDownloadDto.BatchDownloadResponse createScheduledBatchDownload(
            SoftwareDownloadDto.BatchDownloadRequest request,
            String batchId,
            Long batchActionTime,
            String batchName,
            boolean isInternalCall) {
        
        // 判断是否为批量模式
        boolean isBatchMode = org.springframework.util.StringUtils.hasText(batchName);
        
        log.info("⏰ [定时批量下载] 创建定时执行的批量下载任务");
        if (isBatchMode) {
            log.info("  批次ID: {}", batchId);
            log.info("  批次名称: {}", batchName);
        } else {
            log.info("  🔧 单设备模式（不创建Batch）");
        }
        log.info("  定时执行时间: {}", request.getScheduledTime());
        
        // 如果是批量模式，创建 Batch 记录
        if (isBatchMode) {
            BatchHelper.BatchCreationContext context = new BatchHelper.BatchCreationContext();
            context.setSftpServerName(request.getSftpServerId());
            context.setRemark(request.getRemark());
            context.setDebug(request.getDebug());
            context.setScheduledTime(request.getScheduledTime());
            
            Batch batch = BatchHelper.createBatch(
                batchId,
                batchName,
                batchActionTime,
                Batch.BatchType.DOWNLOAD,
                request.getDeviceIds().size(),
                Batch.BatchStatus.SCHEDULED,
                context
            );
            batchRepository.save(batch);
            
            log.info("  ✅ 批次记录已创建: status=SCHEDULED");
        }
        
        // 为每个设备创建定时下载任务
        List<SoftwareDownloadDto.DeviceDownloadResult> deviceResults = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        
        for (String deviceId : request.getDeviceIds()) {
            try {
            // 创建单个设备的下载请求
            SoftwareDownloadDto.DownloadRequest downloadRequest = new SoftwareDownloadDto.DownloadRequest();
            downloadRequest.setDeviceId(deviceId);
            // 直接使用用户提供的文件路径（不再强制添加 /software/ 前缀）
            downloadRequest.setFilePath(request.getSoftwarePackage());
            downloadRequest.setSftpServerName(request.getSftpServerId());
            downloadRequest.setScheduledTime(request.getScheduledTime());
            downloadRequest.setDebug(request.getDebug() != null && request.getDebug());
                
                // ✅ 智能拼接remark（避免"null - xxx"）
                String remark = batchName != null 
                    ? batchName + " - " + request.getRemark()
                    : request.getRemark();
                downloadRequest.setRemark(remark);
                
                // 调用单步下载服务（定时模式）
                SoftwareDownloadDto.DownloadResponse downloadResponse = startDownload(downloadRequest, false);
                
                SoftwareDownloadDto.DeviceDownloadResult result = new SoftwareDownloadDto.DeviceDownloadResult();
                result.setDeviceId(deviceId);
                result.setDeviceName(null);  // 定时任务暂不设置设备名称
                result.setTaskId(downloadResponse.getTaskId());
                result.setStatus("SCHEDULED");
                result.setErrorMessage(null);
                deviceResults.add(result);
                successCount++;
                
            } catch (Exception e) {
                log.error("设备{}创建定时下载任务失败: {}", deviceId, e.getMessage());
                SoftwareDownloadDto.DeviceDownloadResult result = new SoftwareDownloadDto.DeviceDownloadResult();
                result.setDeviceId(deviceId);
                result.setStatus("FAILED");
                result.setErrorMessage("Creation failed: " + e.getMessage());
                deviceResults.add(result);
                failedCount++;
            }
        }
        
        // ⭐ 如果是批量模式 且 不是内部调用，发送 TaskInfo 批次通知（定时任务创建）
        if (isBatchMode && !isInternalCall) {
            try {
                List<DeviceTask> deviceTasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
                // ⭐ 批次创建通知：从数据库查询 Batch 对象获取 createdBy（第一次通知，必须设置 operator）
                String createdBy = batchRepository.findById(batchId)
                    .map(devicemaintenance.entity.Batch::getCreatedBy)
                    .orElse(null);
                // ✅ 定时任务创建时使用 notifyBatchTaskStarted（表示任务已创建/调度）
                taskInfoNotificationService.notifyBatchTaskStarted(
                    batchId,
                    batchName,
                    DeviceTask.TaskType.DOWNLOAD,
                    deviceTasks,
                    batchActionTime,
                    createdBy
                );
                log.info("  ✅ TaskInfo 批次通知已发送 (SCHEDULED - 任务已创建)");
            } catch (Exception e) {
                log.warn("  ⚠️ 发送 TaskInfo 批次通知失败: {}", e.getMessage());
            }
        }
        
        // 构建批量响应
        SoftwareDownloadDto.BatchDownloadResponse response = new SoftwareDownloadDto.BatchDownloadResponse();
        response.setBatchName(batchName);
        response.setSoftwarePackage(request.getSoftwarePackage());
        response.setSftpServerId(request.getSftpServerId());
        response.setTotalDevices(request.getDeviceIds().size());
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setStatus(failedCount == 0 ? "SCHEDULED" : (successCount == 0 ? "FAILED" : "PARTIAL_SUCCESS"));
        response.setCreatedTime(LocalDateTime.now());
        response.setDeviceResults(deviceResults);
        
        log.info("✅ 定时批量下载任务创建完成");
        log.info("  成功: {}, 失败: {}", successCount, failedCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return response;
    }

    /**
     * 处理单个设备的下载（线程池中执行）
     * ✅ 重用单步服务逻辑，字段处理方式一致
     */
    private SoftwareDownloadDto.DeviceDownloadResult processDeviceDownload(
            String deviceId, 
            SoftwareDownloadDto.BatchDownloadRequest batchRequest,
            String batchId,
            Long batchActionTime,
            String batchName,
            String workflowId,
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo) {  // ⭐ 新增 deviceInfo 参数（避免重复查询）
        String deviceName = null;
        
        try {
            // ✅ 使用预验证获取的设备信息（避免重复查询）
            if (deviceInfo != null) {
                deviceName = deviceInfo.getFriendlyName();
                log.debug("  ✓ 使用预验证的设备信息: {}", deviceName);
            } else {
                // 兜底：如果没有提供设备信息，才查询（例如工作流内部调用）
                try {
                    deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                    deviceName = deviceInfo.getFriendlyName();
                } catch (Exception e) {
                    log.debug("  ⚠️ 无法获取设备名称: {}", deviceId);
                }
            }
            
            // 创建单个设备的下载请求
            SoftwareDownloadDto.DownloadRequest downloadRequest = new SoftwareDownloadDto.DownloadRequest();
            downloadRequest.setDeviceId(deviceId);
            // 直接使用用户提供的文件路径（不再强制添加 /software/ 前缀）
            downloadRequest.setFilePath(batchRequest.getSoftwarePackage());
            downloadRequest.setSftpServerName(batchRequest.getSftpServerId());
            downloadRequest.setRemark(batchRequest.getRemark());
            downloadRequest.setDebug(batchRequest.getDebug() != null && batchRequest.getDebug()); // ⭐ Debug模式
            
            // ✅ 重用单步服务逻辑（传递 batchId、workflowId 和 deviceInfo）
            // ⭐ 传递缓存的设备信息，避免重复查询
            boolean debug = batchRequest.getDebug() != null && batchRequest.getDebug();
            SoftwareDownloadDto.DownloadResponse response = startDownload(downloadRequest, debug, batchId, workflowId, deviceInfo);
            
            // 构建设备下载结果（设备名 + RPC返回结果）
            SoftwareDownloadDto.DeviceDownloadResult result = new SoftwareDownloadDto.DeviceDownloadResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setTaskId(response.getTaskId());
            result.setStatus(response.getStatus());
            result.setRpcResult("Task created successfully");
            result.setErrorMessage(response.getErrorMessage());
            result.setCreatedTime(response.getCreatedTime());
            
            log.debug("  ✓ [{}] {} 下载任务创建成功 - 任务ID: {}", 
                deviceName != null ? deviceName : deviceId, 
                batchRequest.getSoftwarePackage(),
                response.getTaskId());
            
            return result;
            
        } catch (Exception e) {
            log.error("  ✗ [{}] {} 下载任务创建失败: {}", 
                deviceName != null ? deviceName : deviceId,
                batchRequest.getSoftwarePackage(),
                e.getMessage());
            
            SoftwareDownloadDto.DeviceDownloadResult result = new SoftwareDownloadDto.DeviceDownloadResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setTaskId(null);
            result.setStatus("FAILED");
            result.setRpcResult(null);
            result.setErrorMessage(e.getMessage());
            result.setCreatedTime(LocalDateTime.now());
            
            return result;
        }
    }

    /**
     * 转换为响应DTO
     */
    private SoftwareDownloadDto.DownloadResponse convertToResponse(DeviceTask task) {
        SoftwareDownloadDto.DownloadResponse response = new SoftwareDownloadDto.DownloadResponse();
        response.setTaskId(task.getTaskId());
        response.setDeviceId(task.getDeviceId());
        response.setFilePath(task.getFilePath());
        response.setSftpServerName(task.getSftpServerName());
        response.setStatus(task.getStatus().name());
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedTime(task.getCreatedTime());
        response.setUpdatedTime(task.getUpdatedTime());
        return response;
    }
}
