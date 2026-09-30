package devicemaintenance.service;

import devicemaintenance.dto.SoftwareCommitDto;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.repository.DeviceTaskRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 软件提交服务
 * 用于确认已激活的软件版本
 */
@Slf4j
@Service
@Transactional
public class SoftwareCommitService {

    @Autowired
    private DeviceTaskRepository deviceTaskRepository;

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
    @Qualifier("batchTaskExecutor")
    private Executor batchTaskExecutor;

    /**
     * 批量启动软件提交任务（公共接口）
     */
    public SoftwareCommitDto.BatchCommitResponse batchCommit(SoftwareCommitDto.BatchCommitRequest request) {
        return batchCommit(request, false);
    }

    /**
     * 批量启动软件提交任务（支持debug）
     */
    public SoftwareCommitDto.BatchCommitResponse batchCommit(SoftwareCommitDto.BatchCommitRequest request, boolean debug) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [批量提交] 创建批量软件提交任务（并发处理）");
        log.info("  设备数量: {}", request.getDeviceIds().size());
        log.info("  SFTP服务器: {}", request.getSftpServerName());
        log.info("  软件包文件名: {}", request.getFileName());
        log.info("  定时执行: {}", request.getScheduledTime() != null ? request.getScheduledTime() : "立即执行");
        log.info("  Debug模式: {}", debug);

        // ✅ 立即执行：使用CompletableFuture + 线程池实现并发处理
        List<CompletableFuture<SoftwareCommitDto.DeviceCommitResult>> futures = 
            request.getDeviceIds().stream()
            .map(deviceId -> CompletableFuture.supplyAsync(() -> {
                return processDeviceCommit(deviceId, request, debug);
            }, batchTaskExecutor))
            .collect(Collectors.toList());
        
        log.info("📤 已提交 {} 个并发提交任务到线程池", futures.size());
        
        // 等待所有任务完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();
        
        // 收集所有结果
        List<SoftwareCommitDto.DeviceCommitResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        // 统计成功和失败数
        int successCount = (int) results.stream().filter(r -> "RUNNING".equals(r.getStatus()) || "PENDING".equals(r.getStatus())).count();
        int failedCount = (int) results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        
        log.info("✅ 批量提交任务创建完成（并发处理）");
        log.info("  成功: {}, 失败: {}", successCount, failedCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // 构建响应
        SoftwareCommitDto.BatchCommitResponse response = new SoftwareCommitDto.BatchCommitResponse();
        response.setSftpServerName(request.getSftpServerName());
        response.setFileName(request.getFileName());
        response.setTotalCount(request.getDeviceIds().size());
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setResults(results);
        
        return response;
    }

    /**
     * 处理单个设备的提交任务
     */
    private SoftwareCommitDto.DeviceCommitResult processDeviceCommit(
            String deviceId, 
            SoftwareCommitDto.BatchCommitRequest batchRequest,
            boolean debug) {
        String deviceName = null;
        String deviceIp = null;
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎯 [任务创建] 软件提交任务");
        log.info("  设备ID: {}", deviceId);
        log.info("  SFTP服务器: {}", batchRequest.getSftpServerName());
        log.info("  软件包文件名: {}", batchRequest.getFileName());
        
        try {
            boolean isWorkflowTask = batchRequest.getBatchId() != null
                && batchRequest.getWorkflowIds() != null
                && batchRequest.getWorkflowIds().containsKey(deviceId);

            DeviceTask task = null;

            // 工作流提交必须先留下 COMMIT 任务。否则预校验失败时仍会显示上一轮激活任务已完成，
            // 看起来像提交完成，但实际没有向该网元下发提交 RPC。
            if (isWorkflowTask) {
                task = createCommitTask(deviceId, batchRequest);
                task = deviceTaskRepository.save(task);
                log.info("✅ 工作流提交任务记录已保存到数据库");
                log.info("  任务ID: {}", task.getTaskId());
            }

            // ✅ 对于立即执行的任务，先验证设备信息
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = null;  // ⭐ 定义在外层作用域
            if (batchRequest.getScheduledTime() == null) {
                log.info("🔍 [Pre-validation] Validating device...");
                try {
                    deviceInfo = neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
                    deviceName = deviceInfo.getFriendlyName();
                    deviceIp = deviceInfo.getIp();
                    log.info("  ✓ Device validated: {} ({}) - IP: {}", 
                        deviceInfo.getFriendlyName(), deviceId, deviceInfo.getIp());
                } catch (Exception e) {
                    log.error("❌ [Pre-validation failed] {}", e.getMessage());
                    if (task != null) {
                        executionHelper.handleTaskError(task, "Software Commit pre-validation", e, null);
                    }
                    // 验证失败，返回失败结果
                    SoftwareCommitDto.DeviceCommitResult result = new SoftwareCommitDto.DeviceCommitResult();
                    if (task != null) {
                        result.setTaskId(task.getTaskId());
                    }
                    result.setDeviceId(deviceId);
                    result.setDeviceName(deviceName);
                    result.setDeviceIp(deviceIp);
                    result.setStatus("FAILED");
                    result.setErrorMessage("Pre-validation failed: " + e.getMessage());
                    result.setCreatedTime(LocalDateTime.now());
                    return result;
                }
            }

            // 创建任务
            if (task == null) {
                task = createCommitTask(deviceId, batchRequest);
            }
            
            // ⭐ 使用统一方法设置所有设备信息
            taskCreationHelper.setDeviceInfoToTask(task, deviceInfo);
            
            task = deviceTaskRepository.save(task);
            log.info("✅ 任务记录已保存到数据库");
            log.info("  任务ID: {}", task.getTaskId());

            // 立即执行
            if (batchRequest.getScheduledTime() == null) {
                log.info("🚀 立即执行提交任务");
                executeRealCommit(task, debug);
            } else {
                log.info("⏰ 任务已计划在 {} 执行", batchRequest.getScheduledTime());
            }

            log.info("✅ 任务执行成功");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // 返回成功结果
            SoftwareCommitDto.DeviceCommitResult result = new SoftwareCommitDto.DeviceCommitResult();
            result.setTaskId(task.getTaskId());
            result.setDeviceId(deviceId);
            result.setDeviceName(task.getDeviceName() != null ? task.getDeviceName() : deviceName);
            result.setDeviceIp(task.getDeviceIp() != null ? task.getDeviceIp() : deviceIp);
            result.setStatus(task.getStatus().name());
            result.setCreatedTime(task.getCreatedTime());
            return result;
            
        } catch (Exception e) {
            log.error("❌ 任务执行失败: {}", e.getMessage(), e);
            log.warn("⚠️ 任务失败但DeviceTask记录已保留");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // 返回失败结果
            SoftwareCommitDto.DeviceCommitResult result = new SoftwareCommitDto.DeviceCommitResult();
            result.setDeviceId(deviceId);
            result.setDeviceName(deviceName);
            result.setDeviceIp(deviceIp);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
            result.setCreatedTime(LocalDateTime.now());
            return result;
        }
    }

    /**
     * 创建或复用提交任务记录
     * 
     * ⭐ 两种模式：
     * 1. Workflow 内任务：复用现有任务记录（更新类型和状态）
     * 2. 独立批次任务：创建新任务记录
     */
    private DeviceTask createCommitTask(String deviceId, SoftwareCommitDto.BatchCommitRequest request) {
        // ✅ 可用性检查：检查设备是否有未完成任务
        // 如果是 workflow 任务且前一步骤已完成，返回该任务供复用
        DeviceTask existingTask = uniquenessService.checkAndGetAvailableTask(
            deviceId,
            DeviceTask.TaskType.COMMIT,
            request.getBatchId()  // ⭐ 传入 batchId，确保只复用同一 batch 的任务
        );
        
        DeviceTask task;
        
        if (existingTask != null) {
            // ⭐ 复用模式：更新现有任务
            task = existingTask;
            log.info("  ♻️  复用现有任务: taskId={}, 前状态={}",
                task.getTaskId(), task.getStatus());

            // 更新任务类型和状态
            task.setTaskType(DeviceTask.TaskType.COMMIT);
            task.setStatus(DeviceTask.TaskStatus.PENDING);  // 重置为 PENDING
            task.setErrorMessage(null);  // 清除之前的错误信息
            task.setCompletedTime(null);  // 清除完成时间

            log.info("  ✅ 任务已更新为 COMMIT 类型");

        } else {
            // ⭐ 新建模式：创建新任务记录
            String taskId = taskCreationHelper.generateTaskId();
            task = new DeviceTask(taskId, deviceId, DeviceTask.TaskType.COMMIT);
            log.info("  ✅ 创建新提交任务: taskId={}", taskId);
            
            // 设置 batchId 和 workflowId
            if (request.getBatchId() != null) {
                task.setBatchId(request.getBatchId());
                log.info("  ✓ 设置 batchId: {}", request.getBatchId());
            }
            
            if (request.getWorkflowIds() != null && request.getWorkflowIds().containsKey(deviceId)) {
                String workflowId = request.getWorkflowIds().get(deviceId);
                task.setWorkflowId(workflowId);
                log.info("  ✓ 设置 workflowId: {}", workflowId);
            }
        }
        
        // 设置/更新任务参数（无论新建还是复用都需要）
        if (request.getSftpServerName() != null) {
            task.setSftpServerName(request.getSftpServerName());
        }
        
        if (request.getFileName() != null) {
            task.setFilePath(request.getFileName());
        }
        
        // 设置描述（注意：这里使用 errorMessage 字段存储描述）
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
     * 执行真实提交（调用nemgr微服务）
     */
    @Async("taskSchedulerExecutor")
    private void executeRealCommit(DeviceTask task, boolean debug) {
        log.info("执行真实设备提交: taskId={}, deviceId={}, sftpServer={}, fileName={}, debug={}", 
            task.getTaskId(), task.getDeviceId(), task.getSftpServerName(), task.getFilePath(), debug);
        
        try {
            executionHelper.updateTaskToRunning(task);
            
            // ✅ 调用 softwareCommit，现在返回 RpcResult 包含完整的 debugPayload
            NeMgrIntegrationService.RpcResult result = neMgrIntegrationService.softwareCommit(
                task.getDeviceId(), 
                task.getSftpServerName(),
                task.getFilePath()
            );
            
            // ✅ 保存设备友好名称（统一方法）
            rpcDebugHelper.saveFriendlyNameIfPresent(task, result);
            
            // ✅ 检查RPC响应：只验证命令是否成功下发
            if (result == null || result.getOutput() == null) {
                // RPC调用失败（返回null），标记为失败
                executionHelper.completeTaskFailure(task, "Software Commit", "RPC call failed, returned null", 
                    result != null ? result.getDebugPayload() : null, null, debug);
            } else {
                NeSoftwareOperateOutput output = (NeSoftwareOperateOutput) result.getOutput();
                String rpcResult = output.getResult();
                
                // ✅ 使用统一的失败判断方法
                if (executionHelper.isFailureResult(rpcResult)) {
                    // ❌ RPC 返回失败
                    log.error("❌ RPC returned fail: result={}", rpcResult);
                    executionHelper.completeTaskFailure(task, "Software Commit", "RPC execution failed, result: " + rpcResult, 
                        result.getDebugPayload(), output, debug);
                } else {
                    // ✅ RPC调用成功，命令已下发，状态保持为RUNNING
                    // 最终状态将由 Kafka 异步通知更新
                    log.info("✅ Commit command successfully issued to neMgr, result: {}", rpcResult);
                    log.info("⏳ Task status: RUNNING - waiting for Kafka notification");
                    
                    // ✅ 保存 debug payload 并持久化（统一方法）
                    rpcDebugHelper.saveDebugPayloadAndPersist(task, result.getDebugPayload(), debug);
                }
            }
            
        } catch (Exception e) {
            // ✅ 异常情况下无法获取完整的 debugPayload
            executionHelper.handleTaskError(task, "Software Commit", e, null);
        }
    }

    /**
     * 执行定时任务（供调度器调用）
     */
    public void executeScheduledTask(DeviceTask task) {
        log.info("执行定时提交任务: taskId={}, deviceId={}", task.getTaskId(), task.getDeviceId());
        executeRealCommit(task, false);  // 定时任务不启用debug
    }
}

