package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.utils.DeviceMaintenanceLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 真实服务执行辅助类
 * 
 * 职责：封装任务状态更新、Debug Payload保存和TaskInfo通知的通用逻辑
 * Clean Code: DRY - 避免在每个Service中重复相同代码
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RealServiceExecutionHelper {

    private final DeviceTaskRepository deviceTaskRepository;
    private final TaskInfoNotificationService taskInfoNotificationService;
    private final RpcDebugHelper rpcDebugHelper;
    private final BatchLogService batchLogService;
    
    // ⭐ 使用 @Lazy 避免循环依赖
    @Autowired
    @Lazy
    private WorkflowManagementService workflowManagementService;
    
    @Autowired
    @Lazy
    private devicemaintenance.repository.UpgradeWorkflowRepository workflowRepository;
    
    @Autowired
    @Lazy
    private devicemaintenance.repository.BatchRepository upgradeBatchRepository;
    
    @Autowired
    @Lazy
    private BatchUpgradeService batchUpgradeService;

    public void updateTaskToRunning(DeviceTask task) {
        DeviceMaintenanceLogContext.setTaskContext(task);
        DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", "mark-running");
        task.setStatus(DeviceTask.TaskStatus.RUNNING);
        task.setStartedTime(LocalDateTime.now());
        DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
        deviceTaskRepository.save(task);
        log.info("statusChange from=PENDING to=RUNNING reason=rpcAccepted");

        // ⭐ 单设备任务：发送 RUNNING 状态通知
        // ⭐ 批次任务：依赖 SystemChangeNotificationListener 的批次级防抖通知
        boolean isSingleDeviceTask = (task.getBatchId() == null);
        if (isSingleDeviceTask) {
            log.info("📨 [单设备任务] 状态已更新为 RUNNING: taskId={}", task.getTaskId());
            try {
                taskInfoNotificationService.notifyTaskStarted(task);
                log.info("   ✅ 已发送 TaskInfo RUNNING 状态通知");
            } catch (Exception e) {
                log.warn("   ⚠️ 发送 TaskInfo 通知失败: {}", e.getMessage());
            }
        } else {
            log.debug("batch notification will be handled later for running task");
        }
        logBatchStateIfNeeded(task, "task-running");
        DeviceMaintenanceLogContext.clearPhase();
    }

    /**
     * 完成任务（成功）
     */
    public void completeTaskSuccess(DeviceTask task, String operationName, String result) {
        completeTaskSuccess(task, operationName, result, null, null, false);
    }

    /**
     * 完成任务（成功）- 支持 debug payload
     * 
     * @param task 任务
     * @param operationName 操作名称
     * @param result RPC 结果
     * @param rpcResult RPC 完整结果对象（用于保存 debug payload）
     * @param requestParams 请求参数
     * @param debug 是否启用调试模式
     */
    public void completeTaskSuccess(DeviceTask task, String operationName, String result, 
                                    NeMgrIntegrationService.RpcResult rpcResult, 
                                    Map<String, Object> requestParams, 
                                    boolean debug) {
        DeviceMaintenanceLogContext.setTaskContext(task);
        DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", operationName);
        log.info("statusChange from={} to=COMPLETED reason=rpcSuccess result={}", task.getStatus(), result);
        task.setStatus(DeviceTask.TaskStatus.COMPLETED);
        task.setCompletedTime(LocalDateTime.now());
        DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
        
        // ✅ 保存 debug payload（如果启用）
        if (debug && requestParams != null) {
            rpcDebugHelper.saveSuccessDebugPayload(task, requestParams, debug);
        }
        
        deviceTaskRepository.save(task);
        logBatchStateIfNeeded(task, operationName + "-success");
        DeviceMaintenanceLogContext.clearPhase();
        
        // ❌ 不再发送设备级通知！
        // 批次任务通过 SystemChangeNotificationListener 的批次级防抖通知来更新 TaskInfo
    }

    /**
     * 完成任务（失败）
     */
    public void completeTaskFailure(DeviceTask task, String operationName, String result) {
        completeTaskFailure(task, operationName, result, null, null, false);
    }

    /**
     * 完成任务（失败）- 支持 debug payload
     * 
     * @param task 任务
     * @param operationName 操作名称
     * @param result RPC 失败结果
     * @param requestParams 请求参数
     * @param rpcResponse RPC 响应对象（可能为 null）
     * @param debug 是否启用调试模式
     */
    public void completeTaskFailure(DeviceTask task, String operationName, String result,
                                    Map<String, Object> requestParams,
                                    Object rpcResponse,
                                    boolean debug) {
        DeviceMaintenanceLogContext.setTaskContext(task);
        DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", operationName);
        log.error("statusChange from={} to=FAILED reason=rpcFailure result={}", task.getStatus(), result);
        task.setStatus(DeviceTask.TaskStatus.FAILED);
        
        // ⭐ 提取用户友好的错误信息（隐藏敏感的 XML 和密码）
        String userFriendlyError = extractUserFriendlyError(result);
        task.setErrorMessage(operationName + " failed: " + userFriendlyError);
        task.setCompletedTime(LocalDateTime.now());
        DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
        
        // ✅ 保存 debug payload（如果启用）
        if (debug) {
            log.info("🐛 Debug mode enabled, saving RPC failure debug payload...");
            // ⚠️ 使用 saveRpcFailureDebugPayload：RPC 返回失败不是异常，不应该有堆栈跟踪
            rpcDebugHelper.saveRpcFailureDebugPayload(task, operationName + " failed: " + result, requestParams, rpcResponse);
            log.info("🐛 Debug payload after save: {}", task.getDebugPayload() != null ? task.getDebugPayload().substring(0, Math.min(100, task.getDebugPayload().length())) : "NULL");
        }
        
        deviceTaskRepository.save(task);
        log.info("task persisted after failure debugPayload={}", task.getDebugPayload() != null ? "SET" : "NULL");
        
        // ⭐ 如果任务属于工作流，手动触发 Workflow 和 Batch 状态更新
        // （因为 RPC 直接返回 fail 时不会有 Kafka 通知）
        log.info("checking workflow follow-up after failure");
        triggerWorkflowUpdateIfNeeded(task);
        logBatchStateIfNeeded(task, operationName + "-failed");
        DeviceMaintenanceLogContext.clearPhase();
        
        // ❌ 不再发送设备级通知！
        // 批次任务通过 SystemChangeNotificationListener 的批次级防抖通知来更新 TaskInfo
    }

    public void handleTaskError(DeviceTask task, String operationName, Exception e) {
        handleTaskError(task, operationName, e, null);
    }

    /**
     * 处理任务错误（增强版）
     * 
     * @param task 任务
     * @param operationName 操作名称
     * @param e 异常
     * @param requestPayload RPC请求参数（可选，用于调试）
     */
    public void handleTaskError(DeviceTask task, String operationName, Exception e, Map<String, Object> requestPayload) {
        DeviceMaintenanceLogContext.setTaskContext(task);
        DeviceMaintenanceLogContext.setPhase("TASK_STATUS_UPDATE", operationName);
        log.error("statusChange from={} to=FAILED reason=exception error={}", task.getStatus(), e.getMessage(), e);
        
        // 设置错误消息
        task.setStatus(DeviceTask.TaskStatus.FAILED);
        task.setErrorMessage(operationName + " error: " + e.getMessage());
        task.setCompletedTime(LocalDateTime.now());
        DeviceMaintenanceLogContext.setStatus(task.getStatus().name());
        
        // ✅ 使用 RpcDebugHelper 保存 debug payload（总是保存，因为这是错误情况）
        rpcDebugHelper.saveFailureDebugPayload(task, e, requestPayload, null);
        
        deviceTaskRepository.save(task);
        
        // ⭐ 如果任务属于工作流，手动触发 Workflow 和 Batch 状态更新
        // （因为异常情况下不会有 Kafka 通知）
        triggerWorkflowUpdateIfNeeded(task);
        logBatchStateIfNeeded(task, operationName + "-error");
        DeviceMaintenanceLogContext.clearPhase();
        
        // ❌ 不再发送设备级通知！
        // 批次任务通过 SystemChangeNotificationListener 的批次级防抖通知来更新 TaskInfo
    }

    public boolean isSuccessResult(String result) {
        return result != null && result.toUpperCase().contains("SUCCESS");
    }

    private void logBatchStateIfNeeded(DeviceTask task, String trigger) {
        if (task != null && task.getBatchId() != null) {
            batchLogService.logBatchSnapshotByBatchId(task.getBatchId(), trigger, task.getDeviceId());
        }
    }

    /**
     * 判断 RPC 结果是否为失败
     * 
     * 支持的失败模式：
     * 1. 完全匹配："fail", "failed", "error"
     * 2. 包含关键字："xxx failed: yyy", "error: zzz"
     * 
     * 示例：
     * - "fail" → true
     * - "failed" → true
     * - "database-operate failed: The sftp server's uploadpath/filename is not set for NE." → true
     * - "success" → false
     */
    public boolean isFailureResult(String result) {
        if (result == null || result.isEmpty()) {
            return false;
        }
        
        String lowerResult = result.toLowerCase();
        
        // 完全匹配
        if ("fail".equals(lowerResult) || "failed".equals(lowerResult) || "error".equals(lowerResult)) {
            return true;
        }
        
        // ⭐ RPC 异常响应的常见模式
        // 例如："Failed to execute the RPC ...", "rpcCall failed with error!"
        if (lowerResult.contains("failed to") || 
            lowerResult.contains("rpccall failed") ||
            lowerResult.contains("<rpc-error")) {
            return true;
        }
        
        // 包含关键字（避免误判，只检查明确的失败模式）
        // 例如："database-operate failed: xxx", "error: yyy"
        if (lowerResult.contains(" failed:") || 
            lowerResult.contains("error:") ||
            lowerResult.startsWith("failed:") ||
            lowerResult.startsWith("error:") ||
            lowerResult.endsWith(" failed") ||
            lowerResult.endsWith(" fail")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 提取用户友好的错误信息
     * 
     * 从完整的 RPC 错误中提取关键信息，隐藏敏感的 XML 和密码
     * 
     * @param fullError 完整的错误信息
     * @return 用户友好的错误信息
     */
    private String extractUserFriendlyError(String fullError) {
        if (fullError == null || fullError.isEmpty()) {
            return "Unknown error";
        }
        
        // 1. 尝试提取 <error-message> 中的内容（最友好）
        int errorMsgStart = fullError.indexOf("<error-message");
        if (errorMsgStart > 0) {
            int contentStart = fullError.indexOf(">", errorMsgStart);
            int contentEnd = fullError.indexOf("</error-message>", contentStart);
            if (contentStart > 0 && contentEnd > contentStart) {
                String errorMessage = fullError.substring(contentStart + 1, contentEnd).trim();
                // 移除 XML 属性（如 unknown:lang="en"）
                errorMessage = errorMessage.replaceAll("^[^>]+>", "").trim();
                if (!errorMessage.isEmpty()) {
                    return errorMessage;
                }
            }
        }
        
        // 2. 如果有 "Failed to execute"，只保留第一行
        if (fullError.startsWith("Failed to execute")) {
            int newLineIndex = fullError.indexOf('\n');
            if (newLineIndex > 0) {
                return fullError.substring(0, newLineIndex).trim();
            }
        }
        
        // 3. 如果太长（超过200字符），截断并添加省略号
        if (fullError.length() > 200) {
            // 尝试在句号、换行符处截断
            int cutPoint = fullError.substring(0, 200).lastIndexOf('.');
            if (cutPoint < 50) {
                cutPoint = fullError.substring(0, 200).indexOf('\n');
            }
            if (cutPoint < 50) {
                cutPoint = 200;
            }
            return fullError.substring(0, cutPoint).trim() + "...";
        }
        
        // 4. 原样返回（短错误信息）
        return fullError;
    }
    
    /**
     * 触发 Workflow 和 Batch 状态更新（如果任务属于工作流）
     * 
     * @param task 设备任务
     */
    private void triggerWorkflowUpdateIfNeeded(DeviceTask task) {
        if (task.getWorkflowId() != null) {
            try {
                // ⭐ 防御性检查：确保 workflowManagementService 已注入
                if (workflowManagementService == null) {
                    log.error("  ❌ WorkflowManagementService 未注入，无法更新 Workflow/Batch 状态！");
                    return;
                }
                
                log.info("  🔄 触发 Workflow 状态更新: workflowId={}", task.getWorkflowId());
                
                // 1. 更新 Workflow 状态
                workflowManagementService.updateWorkflowStatus(task.getWorkflowId());
                
                // 2. 获取 Workflow 的 batchId
                String batchId = task.getBatchId();
                if (batchId == null) {
                    // 如果 DeviceTask 没有 batchId，从 Workflow 中获取
                    devicemaintenance.entity.UpgradeWorkflow workflow = 
                        workflowRepository.findById(task.getWorkflowId()).orElse(null);
                    if (workflow != null) {
                        batchId = workflow.getBatchId();
                    }
                }
                
                // 3. 更新 Batch 状态
                if (batchId != null) {
                    log.info("  🔄 触发 Batch 状态更新: batchId={}", batchId);
                    updateBatchStatus(batchId);
                } else {
                    log.warn("  ⚠️ 无法获取 batchId，跳过 Batch 状态更新");
                }
                
            } catch (Exception e) {
                log.error("  ⚠️ 更新 Workflow/Batch 状态失败: workflowId={}", task.getWorkflowId(), e);
            }
        }
    }
    
    /**
     * 更新 Batch 状态
     * 
     * @param batchId 批次ID
     */
    public void updateBatchStatus(String batchId) {
        try {
            log.info("  📊 开始更新 Batch 状态: batchId={}", batchId);
            
            // 1. 查询批次
            java.util.Optional<devicemaintenance.entity.Batch> batchOpt = upgradeBatchRepository.findById(batchId);
            if (!batchOpt.isPresent()) {
                log.warn("  ❌ 批次不存在: {}", batchId);
                return;
            }

            devicemaintenance.entity.Batch batch = batchOpt.get();
            log.info("  ✓ 批次当前状态: {}", batch.getStatus());

            // 2. 查询所有工作流
            java.util.List<devicemaintenance.entity.UpgradeWorkflow> workflows = 
                workflowManagementService.getWorkflowsByBatchId(batchId);

            if (workflows.isEmpty()) {
                log.warn("  ❌ 批次没有工作流: {}", batchId);
                return;
            }
            
            log.info("  ✓ 找到 {} 个工作流", workflows.size());

            // 3. 计算所有workflow的状态
            workflowManagementService.computeAndSetWorkflowStatuses(workflows);

            // 4. 统计workflow状态
            long failedCount = workflows.stream().filter(wf -> wf.isFailed()).count();
            long completedCount = workflows.stream().filter(wf -> wf.isCompleted()).count();
            
            // ⭐ 修复：runningCount 应该是"既未完成也未失败"的数量
            // 这包括了各种中间状态（ACTIVATE_SUCCESS, DOWNLOAD_SUCCESS 等）
            long runningCount = workflows.size() - completedCount - failedCount;
            
            log.info("  📊 工作流统计: 总数={}, 成功={}, 失败={}, 运行中={}", 
                workflows.size(), completedCount, failedCount, runningCount);
            
            // 打印每个 workflow 的状态（用于调试）
            for (devicemaintenance.entity.UpgradeWorkflow wf : workflows) {
                log.info("    - Workflow {}: status={}, isFailed={}, isCompleted={}, isFinished={}", 
                    wf.getWorkflowId(), wf.getStatus(), wf.isFailed(), wf.isCompleted(), wf.isFinished());
            }
            
            // 5. 使用统一的状态计算逻辑（与 SystemChangeNotificationListener 一致）
            devicemaintenance.entity.Batch.BatchStatus newStatus = 
                batchUpgradeService.calculateBatchStatusPublic(batch, workflows);
            
            log.info("  📊 计算批次状态: {} (成功:{}, 失败:{}, 运行中:{})", 
                newStatus, completedCount, failedCount, runningCount);
            
            // 6. 更新批次记录
            devicemaintenance.entity.Batch.BatchStatus oldStatus = batch.getStatus();
            batch.updateBatchStatus(newStatus);
            batch.updateCounts((int)completedCount, (int)failedCount, (int)runningCount);
            upgradeBatchRepository.save(batch);

            log.info("  ✅ Batch 状态已更新: {} → {} (成功:{}, 失败:{}, 运行中:{})", 
                oldStatus, newStatus, completedCount, failedCount, runningCount);
            
            // ⭐ 6. 发送 TaskInfo 通知（智能选择同步或异步）
            // 说明：
            // - 如果在事务中：延迟到事务提交后发送（确保 DeviceTask 数据已持久化）
            // - 如果不在事务中：立即发送
            if (batch.getBatchType() == devicemaintenance.entity.Batch.BatchType.UPGRADE) {
                sendBatchNotification(batch);
            }
            
        } catch (Exception e) {
            log.error("  ❌ 更新 Batch 状态失败: batchId={}", batchId, e);
        }
    }
    
    /**
     * 发送批次通知（智能选择同步或异步）
     * 
     * <p>核心逻辑：
     * <ul>
     *   <li>在事务中：注册到事务提交后发送（解决脏读问题）</li>
     *   <li>不在事务中：立即发送（保持原有行为）</li>
     *   <li>事务回滚：不发送（afterCommit 不会执行）</li>
     * </ul>
     * 
     * @param batch 批次对象
     */
    private void sendBatchNotification(devicemaintenance.entity.Batch batch) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // ✅ 在事务中：注册到事务提交后发送
            log.info("  📤 注册 TaskInfo 通知到事务提交后（避免脏读）");
            final String batchId = batch.getBatchId();
            
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            log.info("  🔔 [afterCommit] 事务已提交，开始发送 TaskInfo 通知");
                            log.info("  🔔 [afterCommit] batchId={}", batchId);
                            
                            // ⭐ 重新查询最新数据（事务已提交，DeviceTask 已持久化）
                            devicemaintenance.entity.Batch latestBatch = 
                                upgradeBatchRepository.findById(batchId).orElse(null);
                            
                            if (latestBatch != null) {
                                log.info("  🔔 [afterCommit] 查询到最新批次状态: {}", latestBatch.getStatus());
                                log.info("  🔔 [afterCommit] 批次统计: 成功={}, 失败={}, 运行中={}", 
                                    latestBatch.getSuccessCount(), latestBatch.getFailedCount(), latestBatch.getRunningCount());
                                log.info("  📤 发送 TaskInfo 通知（事务已提交，数据已持久化）");
                                
                                batchUpgradeService.notifyBatchUpgradeStatus(latestBatch);
                                
                                log.info("  ✅ TaskInfo 通知已发送: status={}, actionTime={}", 
                                    latestBatch.getStatus(), latestBatch.getBatchActionTime());
                            } else {
                                log.warn("  ⚠️ 批次不存在，跳过通知: batchId={}", batchId);
                            }
                        } catch (Exception e) {
                            log.error("  ⚠️ 发送 TaskInfo 通知失败（不影响主流程）: {}", e.getMessage(), e);
                        }
                    }
                }
            );
        } else {
            // ⚠️ 不在事务中：立即发送（保持原有行为）
            try {
                log.info("  📤 发送 TaskInfo 通知（立即发送，无事务环境）");
                batchUpgradeService.notifyBatchUpgradeStatus(batch);
                log.info("  ✅ TaskInfo 通知已发送");
            } catch (Exception e) {
                log.error("  ⚠️ 发送 TaskInfo 通知失败（不影响主流程）: {}", e.getMessage());
            }
        }
    }
}

