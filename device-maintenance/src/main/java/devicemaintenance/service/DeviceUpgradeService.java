package devicemaintenance.service;

import devicemaintenance.dto.DeviceUpgradeDto;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.Batch;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.utils.DeviceMaintenanceLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
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
 * 设备升级服务
 * 处理设备软件升级相关的业务逻辑
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceUpgradeService {

    private final DeviceTaskRepository deviceTaskRepository;
    private final BatchRepository batchRepository;
    private final TaskCreationHelper taskCreationHelper;
    private final RealServiceExecutionHelper executionHelper;
    private final RpcDebugHelper rpcDebugHelper;
    private final DeviceTaskUniquenessService uniquenessService;
    
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
     * 启动设备升级任务
     */
    @Transactional
    /**
     * 启动设备升级（公共接口）
     */
    public DeviceUpgradeDto.UpgradeResponse startUpgrade(DeviceUpgradeDto.UpgradeRequest request) {
        return startUpgrade(request, null, null, false);
    }

    /**
     * 启动设备升级（内部接口，支持batch关联和debug）
     */
    public DeviceUpgradeDto.UpgradeResponse startUpgrade(DeviceUpgradeDto.UpgradeRequest request, String batchId, String workflowId, boolean debug) {
        DeviceMaintenanceLogContext.setIdentifiers(null, batchId, workflowId, request.getDeviceId(), null, "UPGRADE");
        DeviceMaintenanceLogContext.setPhase("API_IN", "start-upgrade");
        log.info("启动设备升级任务: deviceId={}, filePath={}, targetVersion={}", 
                request.getDeviceId(), request.getFilePath(), request.getTargetVersion());
        if (batchId != null) {
            log.info("  批次ID: {}", batchId);
        }
        if (workflowId != null) {
            log.info("  工作流ID: {}", workflowId);
        }

        try {
            // 验证升级文件路径
            if (request.getFilePath() == null || request.getFilePath().trim().isEmpty()) {
                throw new IllegalArgumentException("Upgrade file path cannot be empty");
            }

            // ⭐ targetVersion 是可选的业务信息字段（用于记录和显示），不是 RPC 必要参数
            if (request.getTargetVersion() == null || request.getTargetVersion().trim().isEmpty()) {
                log.warn("⚠️  目标版本未提供，升级操作仍会继续（targetVersion 仅用于记录）");
            }

            // ✅ 对于立即执行的任务，先验证设备信息
            // ⚠️ 验证失败直接抛异常，不创建任务
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;  // ⭐ 在外部声明
            String deviceFriendlyName = null;
            String deviceIp = null;
            String currentSoftwareVersion = null;  // ⭐ 原版本号（升级前的版本）
            if (request.getScheduledTime() == null) {
                long precheckStart = System.currentTimeMillis();
                DeviceMaintenanceLogContext.setPhase("PRECHECK", "validate-device");
                log.info("🔍 [Pre-validation] Validating device...");
                try {
                    // 验证设备是否存在，并获取友好名称、IP和当前版本
                    deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(request.getDeviceId());
                    deviceFriendlyName = deviceInfo.getFriendlyName();
                    deviceIp = deviceInfo.getIp();
                    currentSoftwareVersion = deviceInfo.getSoftwareVersion();  // ⭐ 获取原版本号
                    log.info("  ✓ Device validated: {} ({}) - IP: {}", deviceFriendlyName, request.getDeviceId(), deviceIp);
                    if (currentSoftwareVersion != null) {
                        log.info("  ✓ Current software version: {}", currentSoftwareVersion);
                    }
                    log.info("precheck success elapsedMs={}", System.currentTimeMillis() - precheckStart);
                } catch (Exception e) {
                    log.error("precheck failed elapsedMs={} error={}", System.currentTimeMillis() - precheckStart, e.getMessage());
                    throw new RuntimeException("Pre-validation failed: " + e.getMessage(), e);
                }
            }

            // 创建设备任务记录（只有预验证通过才会到这里）
            DeviceTask task = createDeviceTask(request, batchId, workflowId);
            
            // ✅ 如果获取到了设备信息，保存到任务记录中
            if (deviceInfo != null) {
                if (deviceFriendlyName != null) {
                    task.setDeviceName(deviceFriendlyName);
                    log.info("  ✓ Device friendly name set: {}", deviceFriendlyName);
                }
                if (deviceIp != null) {
                    task.setDeviceIp(deviceIp);
                    log.info("  ✓ Device IP set: {}", deviceIp);
                }
                if (currentSoftwareVersion != null) {
                    task.setCurrentVersion(currentSoftwareVersion);  // ⭐ 保存原版本号
                    if (task.getPreviousVersion() == null) {
                        task.setPreviousVersion(currentSoftwareVersion);
                    }
                    log.info("  ✓ Current version set: {}", currentSoftwareVersion);
                }
                if (deviceInfo.getVendorType() != null) {
                    task.setVendorType(deviceInfo.getVendorType());   // ⭐ 保存厂商类型
                    log.info("  ✓ Vendor type set: {}", deviceInfo.getVendorType());
                }
                if (deviceInfo.getVendorName() != null) {
                    task.setVendorName(deviceInfo.getVendorName());   // ⭐ 保存厂商名称
                    log.info("  ✓ Vendor name set: {}", deviceInfo.getVendorName());
                }
            }
            
            DeviceMaintenanceLogContext.setPhase("TASK_PERSIST", "save-device-task");
            task = deviceTaskRepository.save(task);
            DeviceMaintenanceLogContext.setTaskContext(task);

            log.info("设备升级任务已创建: taskId={}, status={}, scheduledTime={}", 
                     task.getTaskId(), task.getStatus(), task.getScheduledTime());

            // 只有非定时任务（立即执行）才调用执行服务
            if (taskCreationHelper.shouldExecuteImmediately(task)) {
                // 调用真实的nemgr微服务
                executeRealUpgrade(task, debug);
            } else {
                log.info("定时任务已创建，等待调度执行: taskId={}, scheduledTime={}", 
                         task.getTaskId(), task.getScheduledTime());
            }

            // 返回响应
            DeviceUpgradeDto.UpgradeResponse response = convertToResponse(task);
            response.setSuccess(true);
            response.setMessage("设备升级任务已启动");

            log.info("设备升级任务创建成功: taskId={}, deviceId={}", task.getTaskId(), request.getDeviceId());
            DeviceMaintenanceLogContext.setPhase("API_OUT", "start-upgrade");
            log.info("request completed finalStatus={}", task.getStatus());
            DeviceMaintenanceLogContext.clearAll();
            return response;

        } catch (Exception e) {
            log.error("启动设备升级任务失败: deviceId={}", request.getDeviceId(), e);
            DeviceUpgradeDto.UpgradeResponse errorResponse = new DeviceUpgradeDto.UpgradeResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("启动升级任务失败: " + e.getMessage());
            errorResponse.setDeviceId(request.getDeviceId());
            errorResponse.setFilePath(request.getFilePath());
            errorResponse.setTargetVersion(request.getTargetVersion());
            DeviceMaintenanceLogContext.clearAll();
            return errorResponse;
        }
    }

    /**
     * 创建或复用设备任务记录
     * 
     * ⭐ 两种模式：
     * 1. Workflow 内任务：复用现有任务记录（更新类型和状态）
     * 2. 独立批次任务：创建新任务记录
     */
    private DeviceTask createDeviceTask(DeviceUpgradeDto.UpgradeRequest request, String batchId, String workflowId) {
        // ✅ 可用性检查：检查设备是否有未完成任务
        // 如果是 workflow 任务且前一步骤已完成，返回该任务供复用
        DeviceTask existingTask = uniquenessService.checkAndGetAvailableTask(
            request.getDeviceId(),
            DeviceTask.TaskType.UPGRADE,
            batchId  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
        );
        
        DeviceTask task;
        
        if (existingTask != null) {
            // ⭐ 复用模式：更新现有任务
            task = existingTask;
            log.info("  ♻️  复用现有任务: taskId={}, 前状态={}",
                task.getTaskId(), task.getStatus());

            // 更新任务类型和状态
            task.setTaskType(DeviceTask.TaskType.UPGRADE);
            task.setStatus(DeviceTask.TaskStatus.PENDING);  // 重置为 PENDING
            task.setErrorMessage(null);  // 清除之前的错误信息
            task.setCompletedTime(null);  // 清除完成时间

            log.info("  ✅ 任务已更新为 UPGRADE 类型");

        } else {
            // ⭐ 新建模式：创建新任务记录
            String taskId = taskCreationHelper.generateTaskId();
            task = new DeviceTask(taskId, request.getDeviceId(), DeviceTask.TaskType.UPGRADE);
            log.info("  ✅ 创建新升级任务: taskId={}", taskId);
            
            // 设置批次ID和workflow ID
            task.setBatchId(batchId);
            if (workflowId != null) {
                task.setWorkflowId(workflowId);
                log.info("  ✓ 关联到工作流: workflowId={}", workflowId);
            }
        }
        
        // 设置升级相关字段
        task.setFilePath(request.getFilePath());
        
        // targetVersion 只保留用户提供值；真实版本在下载完成后由通知回写
        String targetVersion = request.getTargetVersion();
        if (targetVersion != null && !targetVersion.trim().isEmpty()) {
            task.setTargetVersion(targetVersion.trim());
        } else {
            log.info("  未提供目标版本，等待下载完成后按设备上报版本回写");
            task.setTargetVersion(null);
        }
        
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
     * 执行真实升级（调用nemgr微服务）
     */
    @Async("taskSchedulerExecutor")
    private void executeRealUpgrade(DeviceTask task, boolean debug) {
        DeviceMaintenanceLogContext.setTaskContext(task);
        DeviceMaintenanceLogContext.setPhase("RPC_SEND", "software-activate");
        long rpcStart = System.currentTimeMillis();
        log.info("rpc dispatch start sftpServer={} filePath={} debug={}", 
            task.getSftpServerName(), task.getFilePath(), debug);
        
        try {
            executionHelper.updateTaskToRunning(task);
            
            // ✅ 调用 softwareActivate，现在返回 RpcResult 包含完整的 debugPayload
            NeMgrIntegrationService.RpcResult result = neMgrIntegrationService.softwareActivate(
                task.getDeviceId(), 
                task.getSftpServerName(),
                task.getFilePath()
            );
            
            // ✅ 保存设备友好名称（统一方法）
            rpcDebugHelper.saveFriendlyNameIfPresent(task, result);
            
            NeSoftwareOperateOutput output = (NeSoftwareOperateOutput) result.getOutput();
            String rpcResult = output.getResult();
            
            if (executionHelper.isSuccessResult(rpcResult)) {
                // ⭐ 升级命令下发成功，保持 RUNNING 状态，等待 Kafka 通知 ACTIVE_COMPLETE
                DeviceMaintenanceLogContext.setPhase("RPC_ACK", "software-activate");
                log.info("rpc ack success elapsedMs={} result={}", System.currentTimeMillis() - rpcStart, rpcResult);
                DeviceMaintenanceLogContext.setPhase("ASYNC_WAIT", "system-change");
                log.info("waiting for async states ACTIVE ACTIVE_COMPLETE FAIL");
                
                // ✅ 保存 debug payload（如果启用）
                if (debug && result.getDebugPayload() != null) {
                    rpcDebugHelper.saveSuccessDebugPayload(task, result.getDebugPayload(), debug);
                }
                
                // ⭐ 保存任务（状态保持 RUNNING，不设置 completedTime）
                deviceTaskRepository.save(task);
                
            } else {
                // ✅ 失败：使用真实的 debugPayload
                log.error("rpc ack failed elapsedMs={} result={}", System.currentTimeMillis() - rpcStart, rpcResult);
                executionHelper.completeTaskFailure(task, "设备升级", rpcResult, result.getDebugPayload(), output, debug);
            }
            
        } catch (Exception e) {
            // ✅ 异常情况下无法获取完整的 debugPayload
            executionHelper.handleTaskError(task, "设备升级", e, null);
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
        log.info("执行定时升级任务: taskId={}, deviceId={}", task.getTaskId(), task.getDeviceId());
        
        // ⭐ 确定 debug 开关（从 Batch 读取）
        boolean debug = false;
        if (task.getBatchId() != null) {
            Batch batch = batchRepository.findById(task.getBatchId()).orElse(null);
            if (batch != null) {
                debug = batch.getDebug();
                log.debug("从 Batch 读取 debug 开关: batchId={}, debug={}", task.getBatchId(), debug);
            }
        }
        
        // 构造请求对象
        DeviceUpgradeDto.UpgradeRequest request = new DeviceUpgradeDto.UpgradeRequest();
        request.setDeviceId(task.getDeviceId());
        request.setFilePath(task.getFilePath());
        request.setTargetVersion(task.getTargetVersion());
        request.setSftpServerName(task.getSftpServerName());
        
        log.info("使用真实服务执行定时升级任务: taskId={}, debug={}", task.getTaskId(), debug);
        executeRealUpgrade(task, debug);
    }

    /**
     * 转换为响应DTO
     */
    private DeviceUpgradeDto.UpgradeResponse convertToResponse(DeviceTask task) {
        DeviceUpgradeDto.UpgradeResponse response = new DeviceUpgradeDto.UpgradeResponse();
        response.setTaskId(task.getTaskId());
        response.setDeviceId(task.getDeviceId());
        response.setFilePath(task.getFilePath());
        response.setCurrentVersion(task.getCurrentVersion());
        response.setTargetVersion(task.getTargetVersion());
        response.setStatus(task.getStatus().name());
        response.setUpgradeStatus("N/A");
        response.setErrorMessage(task.getErrorMessage());
        response.setCreatedTime(task.getCreatedTime());
        response.setUpdatedTime(task.getUpdatedTime());
        response.setCompletedTime(task.getCompletedTime());
        return response;
    }

    /**
     * 验证升级文件
     */
    public DeviceUpgradeDto.UpgradeFileValidationResult validateUpgradeFile(String deviceId, String filePath) {
        log.info("验证升级文件: deviceId={}, filePath={}", deviceId, filePath);
        
        try {
            // TODO: 实现真实的升级文件验证逻辑
            // 这里返回Mock验证结果
            DeviceUpgradeDto.UpgradeFileValidationResult result = new DeviceUpgradeDto.UpgradeFileValidationResult();
            result.setValid(true);
            result.setFileInfo("升级包信息: 版本 v2.1.5, 大小: 256MB");
            result.setFileSize(268435456L); // 256MB
            result.setFileVersion("v2.1.5");
            result.setDeviceCompatible(true);
            result.setValidationMessage("升级文件验证通过");
            result.setChecksum("SHA256:a1b2c3d4e5f6...");
            
            return result;
            
        } catch (Exception e) {
            log.error("验证升级文件失败: deviceId={}, filePath={}", deviceId, filePath, e);
            DeviceUpgradeDto.UpgradeFileValidationResult result = new DeviceUpgradeDto.UpgradeFileValidationResult();
            result.setValid(false);
            result.setValidationMessage("升级文件验证失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 检查版本兼容性
     */
    public DeviceUpgradeDto.VersionCompatibilityResult checkVersionCompatibility(
            String deviceId, String currentVersion, String targetVersion) {
        log.info("检查版本兼容性: deviceId={}, currentVersion={}, targetVersion={}", 
                deviceId, currentVersion, targetVersion);
        
        try {
            // TODO: 实现真实的版本兼容性检查逻辑
            // 这里返回Mock检查结果
            DeviceUpgradeDto.VersionCompatibilityResult result = new DeviceUpgradeDto.VersionCompatibilityResult();
            result.setCurrentVersion(currentVersion);
            result.setTargetVersion(targetVersion);
            
            // 简单的版本比较逻辑（实际应用中需要更复杂的逻辑）
            if (targetVersion.compareTo(currentVersion) > 0) {
                result.setCompatible(true);
                result.setCompatibilityMessage("版本兼容，可以正常升级");
                result.setRequiresForceUpgrade(false);
                result.setWarnings("升级过程中设备将会重启，请确保网络连接稳定");
                result.setPrerequisites("请确保设备存储空间足够，建议至少预留500MB空间");
            } else if (targetVersion.equals(currentVersion)) {
                result.setCompatible(false);
                result.setCompatibilityMessage("目标版本与当前版本相同，无需升级");
                result.setRequiresForceUpgrade(true);
            } else {
                result.setCompatible(false);
                result.setCompatibilityMessage("目标版本低于当前版本，建议使用强制升级");
                result.setRequiresForceUpgrade(true);
                result.setWarnings("降级操作可能导致数据丢失，请谨慎操作");
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("检查版本兼容性失败: deviceId={}", deviceId, e);
            DeviceUpgradeDto.VersionCompatibilityResult result = new DeviceUpgradeDto.VersionCompatibilityResult();
            result.setCompatible(false);
            result.setCompatibilityMessage("版本兼容性检查失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 批量升级
     * 说明：不同设备升级到同一版本，使用同一个SFTP
     */
    @Transactional
    /**
     * 批量升级（外部调用，会发送TaskInfo通知）
     */
    public DeviceUpgradeDto.BatchUpgradeResponse batchUpgrade(DeviceUpgradeDto.BatchUpgradeRequest request) {
        return batchUpgrade(request, false);  // 外部调用，发送通知
    }
    
    /**
     * 批量升级（支持外部调用和内部调用）
     * @param request 升级请求
     * @param isInternalCall 是否为内部调用（true=不发送TaskInfo通知，false=发送通知）
     */
    public DeviceUpgradeDto.BatchUpgradeResponse batchUpgrade(
            DeviceUpgradeDto.BatchUpgradeRequest request,
            boolean isInternalCall) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [批量升级] 创建批量软件升级任务（并发处理）");
        log.info("  批次名称: {}", request.getBatchName());
        log.info("  设备数量: {}", request.getDeviceIds().size());
        log.info("  升级文件: {} (所有设备升级到同一版本)", request.getUpgradeFilePath());
        log.info("  目标版本: {}", request.getTargetVersion());
        log.info("  SFTP服务器: {} (所有设备使用同一SFTP)", request.getSftpServerName());
        log.info("  定时执行: {}", request.getScheduledTime() != null ? request.getScheduledTime() : "立即执行");
        log.info("  Debug模式: {}", request.getDebug() != null && request.getDebug());
        log.info("  最大并发数: 20 (受线程池控制)");
        
        // ⭐ 工作流内部调用：使用提供的 batchId（不创建新Batch）
        boolean isWorkflowCall = org.springframework.util.StringUtils.hasText(request.getBatchId());
        final java.util.Map<String, String> workflowIds = request.getWorkflowIds();
        
        if (isWorkflowCall) {
            log.info("  🔄 工作流内部调用: batchId={}, workflowCount={}", request.getBatchId(), workflowIds != null ? workflowIds.size() : 0);
        }
        
        // ⭐ 如果是定时执行，使用定时任务逻辑
        if (request.getScheduledTime() != null) {
            return createScheduledBatchUpgrade(request);
        }
        
        // ✅ 立即执行：使用CompletableFuture + 线程池实现并发处理
        List<CompletableFuture<DeviceUpgradeDto.DeviceUpgradeResult>> futures = 
            request.getDeviceIds().stream()
            .map(deviceId -> CompletableFuture.supplyAsync(() -> {
                // ⭐ 获取该设备对应的 workflowId（如果是工作流内部调用）
                String workflowId = (workflowIds != null) ? workflowIds.get(deviceId) : null;
                return processDeviceUpgrade(deviceId, request, workflowId);
            }, batchTaskExecutor)) // ✅ 使用统一线程池，最大并发数20
            .collect(Collectors.toList());
        
        log.info("📤 已提交 {} 个并发升级任务到线程池", futures.size());
        
        // 等待所有任务完成（受线程池大小限制并发数）
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join(); // 阻塞等待所有任务完成
        
        // 收集所有结果
        List<DeviceUpgradeDto.DeviceUpgradeResult> deviceResults = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        // 统计结果
        int successCount = (int) deviceResults.stream().filter(r -> "RUNNING".equals(r.getStatus())).count();
        int failedCount = (int) deviceResults.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        
        // ⭐ 检查是否有失败：记录日志，不抛异常（让调用方通过 DeviceTask 状态判断）
        if (failedCount > 0 && successCount == 0) {
            // 全部失败
            String firstError = deviceResults.stream()
                .filter(r -> r.getErrorMessage() != null)
                .map(DeviceUpgradeDto.DeviceUpgradeResult::getErrorMessage)
                .findFirst()
                .orElse("未知错误");
            
            log.error("❌ 批量升级全部失败: failedCount={}, 第一个错误: {}", failedCount, firstError);
            // ✅ 不抛异常，让 DeviceTask、Workflow、Batch 状态自动传播失败信息
        }
        
        // 构建批量响应
        DeviceUpgradeDto.BatchUpgradeResponse response = new DeviceUpgradeDto.BatchUpgradeResponse();
        response.setBatchName(request.getBatchName());
        response.setUpgradeFilePath(request.getUpgradeFilePath());
        response.setTargetVersion(request.getTargetVersion());
        response.setSftpServerName(request.getSftpServerName());
        response.setTotalDevices(request.getDeviceIds().size());
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setStatus(failedCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAILED" : "PARTIAL_SUCCESS"));
        response.setCreatedTime(LocalDateTime.now());
        response.setDeviceResults(deviceResults);
        
        log.info("✅ 批量升级任务创建完成（并发处理）");
        log.info("  成功: {}, 失败: {}", successCount, failedCount);
        
        // ⭐ 如果有部分失败，记录警告
        if (failedCount > 0) {
            log.warn("⚠️  批量升级部分失败: successCount={}, failedCount={}", successCount, failedCount);
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // 打印可读格式的结果摘要（设备操作映射）
        log.info("📊 设备升级结果摘要（设备→操作→状态映射）:");
        for (DeviceUpgradeDto.DeviceUpgradeResult result : deviceResults) {
            log.info("  {} → 软件升级 → {} [任务ID: {}]", 
                result.getDeviceName() != null ? result.getDeviceName() : result.getDeviceId(),
                result.getStatus(),
                result.getTaskId() != null ? result.getTaskId() : "N/A");
        }
        
        return response;
    }
    
    /**
     * 创建定时批量升级任务
     * ⭐ 所有设备在指定时间一起进入线程池执行
     */
    private DeviceUpgradeDto.BatchUpgradeResponse createScheduledBatchUpgrade(
            DeviceUpgradeDto.BatchUpgradeRequest request) {
        log.info("⏰ [定时批量升级] 创建定时执行的批量升级任务");
        log.info("  定时执行时间: {}", request.getScheduledTime());
        
        // 为每个设备创建定时升级任务
        List<DeviceUpgradeDto.DeviceUpgradeResult> deviceResults = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        
        for (String deviceId : request.getDeviceIds()) {
            try {
                // 创建单个设备的升级请求
                DeviceUpgradeDto.UpgradeRequest upgradeRequest = new DeviceUpgradeDto.UpgradeRequest();
                upgradeRequest.setDeviceId(deviceId);
                upgradeRequest.setFilePath(request.getUpgradeFilePath());
                upgradeRequest.setTargetVersion(request.getTargetVersion());
                upgradeRequest.setSftpServerName(request.getSftpServerName());
                upgradeRequest.setScheduledTime(request.getScheduledTime());
                // Note: UpgradeRequest没有setRemark方法，跳过remark设置
                
                // 调用单步升级服务（定时模式）
                DeviceUpgradeDto.UpgradeResponse upgradeResponse = startUpgrade(upgradeRequest, null, null, request.getDebug() != null && request.getDebug());
                
                DeviceUpgradeDto.DeviceUpgradeResult result = new DeviceUpgradeDto.DeviceUpgradeResult();
                result.setDeviceId(deviceId);
                result.setDeviceName(null);  // UpgradeResponse没有getDeviceName方法
                result.setTaskId(upgradeResponse.getTaskId());
                result.setStatus("SCHEDULED");
                result.setErrorMessage(null);
                deviceResults.add(result);
                successCount++;
                
            } catch (Exception e) {
                log.error("设备{}创建定时升级任务失败: {}", deviceId, e.getMessage());
                DeviceUpgradeDto.DeviceUpgradeResult result = new DeviceUpgradeDto.DeviceUpgradeResult();
                result.setDeviceId(deviceId);
                result.setStatus("FAILED");
                result.setErrorMessage("Creation failed: " + e.getMessage());
                deviceResults.add(result);
                failedCount++;
            }
        }
        
        // 构建批量响应
        DeviceUpgradeDto.BatchUpgradeResponse response = new DeviceUpgradeDto.BatchUpgradeResponse();
        response.setBatchName(request.getBatchName());
        response.setUpgradeFilePath(request.getUpgradeFilePath());
        response.setTargetVersion(request.getTargetVersion());
        response.setSftpServerName(request.getSftpServerName());
        response.setTotalDevices(request.getDeviceIds().size());
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setStatus(failedCount == 0 ? "SCHEDULED" : (successCount == 0 ? "FAILED" : "PARTIAL_SUCCESS"));
        response.setCreatedTime(LocalDateTime.now());
        response.setDeviceResults(deviceResults);
        
        log.info("✅ 定时批量升级任务创建完成");
        log.info("  成功: {}, 失败: {}", successCount, failedCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return response;
    }

    /**
     * 处理单个设备的升级（线程池中执行）
     * ✅ 重用单步服务逻辑，字段处理方式一致
     */
    private DeviceUpgradeDto.DeviceUpgradeResult processDeviceUpgrade(
            String deviceId, DeviceUpgradeDto.BatchUpgradeRequest batchRequest, String workflowId) {
        String deviceName = null;
        String deviceIp = null;
        NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;  // ⭐ 保存完整的设备信息
        
        try {
            // 获取设备友好名称和IP
            try {
                deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                deviceName = deviceInfo.getFriendlyName();
                deviceIp = deviceInfo.getIp();
            } catch (Exception e) {
                log.debug("  ⚠️ 无法获取设备信息: {}", deviceId);
            }
            
            // 创建单个设备的升级请求
            DeviceUpgradeDto.UpgradeRequest upgradeRequest = new DeviceUpgradeDto.UpgradeRequest();
            upgradeRequest.setDeviceId(deviceId);
            upgradeRequest.setFilePath(batchRequest.getUpgradeFilePath());
            upgradeRequest.setTargetVersion(batchRequest.getTargetVersion());
            upgradeRequest.setSftpServerName(batchRequest.getSftpServerName());
            upgradeRequest.setDescription(batchRequest.getRemark());
            
            // ✅ 重用单步服务逻辑，传递 batchId, workflowId 和 debug 参数
            String batchId = batchRequest.getBatchId();  // 工作流内部调用会提供 batchId
            DeviceUpgradeDto.UpgradeResponse response = startUpgrade(upgradeRequest, batchId, workflowId, batchRequest.getDebug() != null && batchRequest.getDebug());
            
            // 构建设备升级结果（设备名 + RPC返回结果）
            DeviceUpgradeDto.DeviceUpgradeResult result = new DeviceUpgradeDto.DeviceUpgradeResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setTaskId(response.getTaskId());
            result.setStatus(response.isSuccess() ? "RUNNING" : "FAILED");
            result.setRpcResult(response.isSuccess() ? "Task created successfully" : null);
            result.setErrorMessage(response.isSuccess() ? null : response.getMessage());
            result.setCreatedTime(LocalDateTime.now());
            
            log.debug("  ✓ [{}] 升级到 {} - 任务ID: {}", 
                deviceName != null ? deviceName : deviceId, 
                batchRequest.getTargetVersion(),
                response.getTaskId());
            
            return result;
            
        } catch (Exception e) {
            log.error("  ✗ [{}] 升级任务创建失败: {}", 
                deviceName != null ? deviceName : deviceId,
                e.getMessage());
            
            // ⭐ 参数验证失败时，也要创建 DeviceTask 并标记为 FAILED，确保失败能传播到 Workflow 和 Batch
            DeviceTask task = uniquenessService.checkAndGetAvailableTask(
                deviceId,
                DeviceTask.TaskType.UPGRADE,
                batchRequest.getBatchId()  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
            );
            
            if (task == null) {
                String taskId = taskCreationHelper.generateTaskId();
                task = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.UPGRADE);
            }
            
            // 设置任务基本信息
            task.setBatchId(batchRequest.getBatchId());
            task.setBatchName(batchRequest.getBatchName());
            if (workflowId != null) {
                task.setWorkflowId(workflowId);
            }
            
            // ⭐ 使用统一方法设置所有设备信息
            taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);
            
            task.setFilePath(batchRequest.getUpgradeFilePath());
            task.setTargetVersion(batchRequest.getTargetVersion());
            task.setSftpServerName(batchRequest.getSftpServerName());
            // ⭐ taskInfoActionTime 在工作流调用时不需要设置（由 Batch 统一管理）
            
            // 标记为失败
            task.setStatus(DeviceTask.TaskStatus.FAILED);
            task.setErrorMessage("Upgrade task creation failed: " + e.getMessage());
            task.setCompletedTime(LocalDateTime.now());
            deviceTaskRepository.save(task);
            
            log.info("  ✅ DeviceTask 已创建并标记为 FAILED: taskId={}", task.getTaskId());
            
            // ⭐ 手动触发 Workflow 和 Batch 状态更新（因为没有 Kafka 通知）
            if (workflowId != null) {
                try {
                    log.info("  🔄 触发 Workflow 状态更新: workflowId={}", workflowId);
                    workflowManagementService.updateWorkflowStatus(workflowId);
                    
                    // 获取 batchId 并更新 Batch 状态
                    String batchId = batchRequest.getBatchId();
                    if (batchId != null) {
                        log.info("  🔄 触发 Batch 状态更新: batchId={}", batchId);
                        updateBatchStatusAfterTaskFailure(batchId);
                    }
                } catch (Exception updateEx) {
                    log.error("  ⚠️ 更新 Workflow/Batch 状态失败", updateEx);
                }
            }
            
            // 返回失败结果
            DeviceUpgradeDto.DeviceUpgradeResult result = new DeviceUpgradeDto.DeviceUpgradeResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setTaskId(task.getTaskId());
            result.setStatus("FAILED");
            result.setRpcResult(null);
            result.setErrorMessage(e.getMessage());
            result.setCreatedTime(task.getCreatedTime());
            
            return result;
        }
    }
    
    /**
     * 在任务失败后更新 Batch 状态
     * （用于没有 Kafka 通知的场景，如参数验证失败）
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
