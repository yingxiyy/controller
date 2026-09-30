package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.UpgradeWorkflow;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.UpgradeWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 工作流管理服务（重构版）
 * 
 * 架构说明：
 * - UpgradeWorkflow：轻量级流程控制器，只存储 workflowId, deviceId, status, currentStep, retryCount
 * - DeviceTask：存储所有任务细节（deviceName, deviceIp, taskType, status, errorMessage, 时间戳等）
 * - Batch：存储全局配置（enableDownload/Backup/Upgrade, executionMode等）
 * 
 * 职责：
 * - 创建轻量级工作流（不再设置设备信息、步骤状态等）
 * - 更新工作流整体状态（基于关联的 DeviceTask 状态）
 * - 重试和移除工作流
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowManagementService {

    private final UpgradeWorkflowRepository workflowRepository;
    private final DeviceTaskRepository deviceTaskRepository;

    /**
     * 为批次创建工作流（批次创建时调用）
     * 注意：只创建轻量级工作流记录，不包含设备详情和步骤状态
     */
    @Transactional
    public List<UpgradeWorkflow> createWorkflows(Batch batch, List<String> deviceIds) {
        log.info("为批次创建工作流: batchId={}, deviceCount={}", batch.getBatchId(), deviceIds.size());

        // ⭐ 获取第一个启用的步骤
        String firstStep = getNextEnabledStep(batch, null);
        if (firstStep == null) {
            throw new IllegalStateException("Batch has no enabled steps");
        }
        log.info("  第一步: {}", firstStep);

        List<UpgradeWorkflow> workflows = new ArrayList<>();

        for (String deviceId : deviceIds) {
            String workflowId = UUID.randomUUID().toString();
            UpgradeWorkflow workflow = new UpgradeWorkflow(workflowId, batch.getBatchId(), deviceId);

            // 继承批次的重试配置
            workflow.setMaxRetryCount(batch.getMaxRetryCount());

            // ⭐ 设置初始步骤（修复：避免 currentStep 为 null 导致状态误判）
            workflow.setCurrentStep(firstStep);

            // ⚠️ 不设置 status，因为 status 是 @Transient 计算字段，不持久化

            workflows.add(workflow);
        }

        // 批量保存
        workflows = workflowRepository.saveAll(workflows);
        log.info("工作流创建完成: count={}, firstStep={}", workflows.size(), firstStep);

        return workflows;
    }

    /**
     * 更新工作流状态（基于关联的 DeviceTask 状态）
     * 
     * ⚠️ 注意：此方法现在主要用于更新 currentStep，不再更新 status（status 是计算字段）
     */
    @Transactional
    public UpgradeWorkflow updateWorkflowStatus(String workflowId) {
        log.debug("更新工作流状态: workflowId={}", workflowId);

        Optional<UpgradeWorkflow> workflowOpt = workflowRepository.findById(workflowId);
        if (!workflowOpt.isPresent()) {
            log.warn("工作流不存在: workflowId={}", workflowId);
            return null;
        }

        UpgradeWorkflow workflow = workflowOpt.get();

        // 查询工作流关联的所有 DeviceTask（按 batchId + deviceId）
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceId(
            workflow.getBatchId(), 
            workflow.getDeviceId()
        );

        // 根据 DeviceTask 状态计算工作流状态（设置到 @Transient 字段）
        UpgradeWorkflow.WorkflowStatus newStatus = calculateWorkflowStatusFromTasks(workflow, tasks);
        workflow.setComputedStatus(newStatus);
        alignCurrentStepWithComputedStatus(workflow, newStatus);

        // 仅在工作流成功完成或取消时清空 currentStep。
        // 失败场景保留当前步骤，便于定位失败点并支持按失败步骤重试。
        if (newStatus == UpgradeWorkflow.WorkflowStatus.WORKFLOW_COMPLETED || 
            newStatus == UpgradeWorkflow.WorkflowStatus.COMMIT_SUCCESS ||
            newStatus == UpgradeWorkflow.WorkflowStatus.WORKFLOW_CANCELLED) {
            workflow.setCurrentStep(null);
        }

        // ⚠️ 注意：只有 currentStep 会被持久化，status 不会
        workflow = workflowRepository.save(workflow);
        log.debug("工作流状态已更新: workflowId={}, computedStatus={}", workflow.getWorkflowId(), newStatus);

        return workflow;
    }

    /**
     * 根据关联的 DeviceTask 状态计算工作流整体状态
     * 
     * 新的状态格式：STEP_STATUS（如 DOWNLOAD_RUNNING, ACTIVATE_FAILED）
     * 
     * @param workflow 工作流实体
     * @param tasks 关联的所有 DeviceTask（按 batchId + deviceId 查询）
     * @return 计算后的工作流状态（STEP_STATUS 格式）
     */
    private UpgradeWorkflow.WorkflowStatus calculateWorkflowStatusFromTasks(
        UpgradeWorkflow workflow, 
        List<DeviceTask> tasks
    ) {
        if (tasks.isEmpty()) {
            return UpgradeWorkflow.WorkflowStatus.WORKFLOW_PENDING;
        }

        // 查找当前有效任务：优先按更新时间，其次按创建时间，避免旧步骤任务干扰状态判断
        DeviceTask latestTask = selectLatestTask(tasks).orElse(null);

        if (latestTask == null) {
            return UpgradeWorkflow.WorkflowStatus.WORKFLOW_PENDING;
        }

        // 根据最后一个任务的 taskType 和 status 计算 workflow 状态
        String step = mapTaskTypeToStep(latestTask.getTaskType());
        String status = mapTaskStatusToStepStatus(latestTask.getStatus());
        
        // 组合状态：STEP_STATUS
        String workflowStatusName = step + "_" + status;
        
        try {
            return UpgradeWorkflow.WorkflowStatus.valueOf(workflowStatusName);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 无法映射工作流状态: {}, 使用 WORKFLOW_PENDING", workflowStatusName);
            return UpgradeWorkflow.WorkflowStatus.WORKFLOW_PENDING;
        }
    }
    
    /**
     * 将 DeviceTask.TaskType 映射为 Workflow 步骤名称
     */
    public String mapTaskTypeToStep(DeviceTask.TaskType taskType) {
        if (taskType == null) {
            return "WORKFLOW";
        }
        
        switch (taskType) {
            case DOWNLOAD:
                return "DOWNLOAD";
            case BACKUP:
                return "BACKUP";
            case UPGRADE:
                return "ACTIVATE";  // Upgrade = Activate
            case COMMIT:
                return "COMMIT";  // ⭐ 修正：COMMIT 映射为 COMMIT
            case ROLLBACK:
                return "ROLLBACK";
            default:
                return "WORKFLOW";
        }
    }
    
    /**
     * 将 DeviceTask.TaskStatus 映射为 Workflow 步骤状态
     */
    private String mapTaskStatusToStepStatus(DeviceTask.TaskStatus taskStatus) {
        if (taskStatus == null) {
            return "PENDING";
        }
        
        switch (taskStatus) {
            case IDLE:
            case SCHEDULED:
                return "NOTSTART";
            case PENDING:
                return "PENDING";
            case RUNNING:
                return "RUNNING";
            case COMPLETED:
                return "SUCCESS";
            case FAILED:
            case EXPIRED:
                return "FAILED";
            case CANCELLED:
                return "CANCELLED";
            default:
                return "PENDING";
        }
    }

    /**
     * 获取工作流的下一个启用步骤（根据 Batch 配置）
     * 
     * ⚠️ 注意：UPGRADE 之后不会自动返回 COMMIT
     * - COMMIT 需要通过特殊逻辑判断（批次状态为 READY_TO_COMMIT 时）
     * - 自动模式：Kafka 监听器检测到 READY_TO_COMMIT 状态后触发
     * - 手动模式：用户手动调用 proceed-upgrade-batch 触发
     */
    public String getNextEnabledStep(Batch batch, String currentStep) {
        if (currentStep == null || currentStep.isEmpty()) {
            // 第一步
            if (batch.getEnableDownload()) return "DOWNLOAD";
            if (batch.getEnableBackup()) return "BACKUP";
            if (batch.getEnableUpgrade()) return "UPGRADE";
            return null;
        }

        switch (currentStep.toUpperCase()) {
            case "DOWNLOAD":
                if (batch.getEnableBackup()) return "BACKUP";
                if (batch.getEnableUpgrade()) return "UPGRADE";
                return null;
            case "BACKUP":
                if (batch.getEnableUpgrade()) return "UPGRADE";
                return null;
            case "UPGRADE":
                // ⚠️ UPGRADE 之后不直接返回下一步
                // 因为 COMMIT 需要特殊处理（用户确认或自动触发）
                return null;
            default:
                return null;
        }
    }

    /**
     * 检查工作流是否可以重试
     */
    public boolean canRetry(UpgradeWorkflow workflow) {
        return workflow.canRetry();
    }

    /**
     * 解析批次当前有效步骤。
     * 仅统计未失败 workflow 的 non-null currentStep；若存在多个不同步骤，说明批次处于混合态。
     */
    public String resolveCurrentStep(List<UpgradeWorkflow> workflows) {
        java.util.Set<String> steps = workflows.stream()
            .filter(wf -> !wf.isFailed())
            .map(this::getEffectiveCurrentStep)
            .filter(step -> step != null && !step.trim().isEmpty())
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        if (steps.isEmpty()) {
            return null;
        }

        if (steps.size() > 1) {
            throw new IllegalStateException("Active workflows have inconsistent currentStep values: " + steps);
        }

        return steps.iterator().next();
    }

    /**
     * 重试工作流（增加重试计数，重置失败的 DeviceTask）
     */
    @Transactional
    public UpgradeWorkflow retryWorkflow(String batchId, String deviceId) {
        log.info("重试工作流: batchId={}, deviceId={}", batchId, deviceId);

        Optional<UpgradeWorkflow> workflowOpt = workflowRepository.findByBatchIdAndDeviceId(batchId, deviceId);
        if (!workflowOpt.isPresent()) {
            throw new IllegalArgumentException("Workflow does not exist: batchId=" + batchId + ", deviceId=" + deviceId);
        }

        UpgradeWorkflow workflow = workflowOpt.get();

        // ⭐ 先计算工作流状态（因为 status 是 @Transient 字段，从数据库加载后需要重新计算）
        workflow = updateWorkflowStatus(workflow.getWorkflowId());

        if (!workflow.canRetry()) {
            throw new IllegalStateException("Workflow cannot be retried: status=" + workflow.getStatus() + 
                ", retryCount=" + workflow.getRetryCount() + "/" + workflow.getMaxRetryCount());
        }

        // 增加重试计数
        workflow.incrementRetryCount();
        // ⚠️ 不设置 status，因为 status 是计算字段

        // 查找失败的 DeviceTask 并重置状态
        List<DeviceTask> tasks = deviceTaskRepository.findByWorkflowId(workflow.getWorkflowId());
        String failedStep = null;
        for (DeviceTask task : tasks) {
            if (task.getStatus() == DeviceTask.TaskStatus.FAILED) {
                // 记录失败的步骤（用于重新设置 workflow.currentStep）
                if (failedStep == null) {
                    failedStep = mapTaskTypeToStep(task.getTaskType());
                }
                
                // 重置任务状态
                task.markStatus(DeviceTask.TaskStatus.PENDING);
                task.setErrorMessage(null);
                // ⭐ 清除时间戳（避免显示错误的完成时间）
                task.setCompletedTime(null);
                task.setStartedTime(null);
                deviceTaskRepository.save(task);
                log.info("已重置失败任务: taskId={}, taskType={}", task.getTaskId(), task.getTaskType());
            }
        }
        
        // ⭐ 重新设置 currentStep 为失败的步骤
        if (failedStep != null) {
            workflow.setCurrentStep(failedStep);
            log.info("重新设置 workflow.currentStep: {}", failedStep);
        }

        workflow = workflowRepository.save(workflow);

        log.info("工作流重试已设置: workflowId={}, retryCount={}", 
            workflow.getWorkflowId(), workflow.getRetryCount());

        return workflow;
    }

    /**
     * 移除工作流（同时重置关联的 DeviceTask）
     */
    @Transactional
    public void removeWorkflow(String batchId, String deviceId) {
        log.info("移除工作流: batchId={}, deviceId={}", batchId, deviceId);

        Optional<UpgradeWorkflow> workflowOpt = workflowRepository.findByBatchIdAndDeviceId(batchId, deviceId);
        if (!workflowOpt.isPresent()) {
            throw new IllegalArgumentException("Workflow does not exist: batchId=" + batchId + ", deviceId=" + deviceId);
        }

        UpgradeWorkflow workflow = workflowOpt.get();

        // 重置关联的 DeviceTask
        List<DeviceTask> tasks = deviceTaskRepository.findByWorkflowId(workflow.getWorkflowId());
        for (DeviceTask task : tasks) {
            task.setWorkflowId(null);
            task.setBatchId(null);
            task.setBatchName(null);
            task.setTaskType(null);
            task.markStatus(DeviceTask.TaskStatus.IDLE);
            deviceTaskRepository.save(task);
        }

        // 删除工作流
        workflowRepository.delete(workflow);
        log.info("工作流已移除: workflowId={}, deviceId={}", workflow.getWorkflowId(), deviceId);
    }

    /**
     * 查询批次的所有工作流
     */
    public List<UpgradeWorkflow> getWorkflowsByBatchId(String batchId) {
        return workflowRepository.findByBatchId(batchId);
    }

    /**
     * 查询批次中指定状态的工作流
     * 
     * ⚠️ 已废弃：status 不再存储在数据库中，无法直接查询
     * 请使用 getWorkflowsByBatchId() + computeAndSetWorkflowStatuses() + 手动过滤
     */
    @Deprecated
    public List<UpgradeWorkflow> getWorkflowsByStatus(String batchId, UpgradeWorkflow.WorkflowStatus status) {
        // 需要先查询所有，计算状态，再过滤
        List<UpgradeWorkflow> workflows = getWorkflowsByBatchId(batchId);
        computeAndSetWorkflowStatuses(workflows);
        return workflows.stream()
            .filter(wf -> wf.getStatus() == status)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 统计批次中各状态的工作流数量
     * 
     * ⚠️ 已废弃：status 不再存储在数据库中，无法直接查询
     * 请使用 getWorkflowsByBatchId() + computeAndSetWorkflowStatuses() + 手动过滤
     */
    @Deprecated
    public long countWorkflowsByStatus(String batchId, UpgradeWorkflow.WorkflowStatus status) {
        // 需要先查询所有，计算状态，再过滤
        List<UpgradeWorkflow> workflows = getWorkflowsByBatchId(batchId);
        computeAndSetWorkflowStatuses(workflows);
        return workflows.stream()
            .filter(wf -> wf.getStatus() == status)
            .count();
    }

    /**
     * 检查批次中是否有工作流失败
     * 
     * ⚠️ 注意：调用前必须先调用 computeAndSetWorkflowStatuses()
     */
    public boolean hasAnyFailedWorkflow(List<UpgradeWorkflow> workflows) {
        return workflows.stream()
            .anyMatch(wf -> wf.isFailed());
    }

    /**
     * 检查批次中所有工作流是否都完成
     * 
     * ⚠️ 注意：调用前必须先调用 computeAndSetWorkflowStatuses()
     */
    public boolean areAllWorkflowsCompleted(List<UpgradeWorkflow> workflows) {
        if (workflows.isEmpty()) {
            return false;
        }

        return workflows.stream()
            .allMatch(wf -> wf.isCompleted());
    }
    /**
     * 检查是否有任务正在运行（PENDING/SCHEDULED/RUNNING 状态）
     * 
     * @param workflows 工作流列表
     * @param currentStep 当前步骤
     * @return true 如果有任务正在运行，false 否则
     */
    public boolean hasRunningTasks(List<UpgradeWorkflow> workflows, String currentStep) {
        if (workflows.isEmpty() || currentStep == null) {
            log.debug("  hasRunningTasks: workflows.isEmpty={}, currentStep={}", workflows.isEmpty(), currentStep);
            return false;
        }
        
        DeviceTask.TaskType taskType = getTaskTypeByStep(currentStep);
        log.debug("  hasRunningTasks: currentStep={}, taskType={}, workflowCount={}", 
            currentStep, taskType, workflows.size());
        
        int runningCount = 0;
        int notStartCount = 0;
        int completedCount = 0;
        int otherCount = 0;
        
        for (UpgradeWorkflow workflow : workflows) {
            // 跳过失败的工作流
            if (workflow.isFailed()) {
                continue;
            }

            String effectiveStep = getEffectiveCurrentStep(workflow);
            if (effectiveStep == null || !currentStep.equalsIgnoreCase(effectiveStep)) {
                continue;
            }
            
            // ⚠️ 修复：使用 batchId + deviceId 查询，更可靠
            // 因为 workflowId 可能没有被正确设置到 DeviceTask
            List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceId(
                workflow.getBatchId(), 
                workflow.getDeviceId()
            );
            
            Optional<DeviceTask> currentTask = selectLatestTask(tasks.stream()
                .filter(task -> task.getTaskType() == taskType)
                .collect(java.util.stream.Collectors.toList()));
            
            if (currentTask.isPresent()) {
                DeviceTask.TaskStatus status = currentTask.get().getStatus();
                log.debug("    workflow={}, deviceId={}, taskType={}, status={}", 
                    workflow.getWorkflowId(), workflow.getDeviceId(), taskType, status);
                
                // 检查是否为运行中状态
                if (status == DeviceTask.TaskStatus.PENDING ||
                    status == DeviceTask.TaskStatus.SCHEDULED ||
                    status == DeviceTask.TaskStatus.RUNNING) {
                    runningCount++;
                } else if (status == DeviceTask.TaskStatus.IDLE || 
                           status == DeviceTask.TaskStatus.NOT_START) {
                    notStartCount++;
                } else if (status == DeviceTask.TaskStatus.COMPLETED) {
                    completedCount++;
                } else {
                    otherCount++;
                }
            } else {
                log.debug("    workflow={}, deviceId={}, taskType={} - 任务不存在", 
                    workflow.getWorkflowId(), workflow.getDeviceId(), taskType);
                notStartCount++;
            }
        }
        
        boolean hasRunning = runningCount > 0;
        log.debug("  hasRunningTasks 结果: hasRunning={}, runningCount={}, notStartCount={}, completedCount={}, otherCount={}", 
            hasRunning, runningCount, notStartCount, completedCount, otherCount);
        
        return hasRunning;
    }

    /**
     * 检查批次中所有工作流的当前步骤是否都完成
     * （通过查询关联的 DeviceTask 来判断）
     */
    public boolean areAllCurrentStepsCompleted(List<UpgradeWorkflow> workflows) {
        if (workflows.isEmpty()) {
            return false;
        }

        // ⭐ 过滤出未失败的工作流
        List<UpgradeWorkflow> activeWorkflows = workflows.stream()
            .filter(wf -> !wf.isFailed())
            .collect(java.util.stream.Collectors.toList());
        
        // ⭐ 如果所有工作流都失败了，返回 false
        if (activeWorkflows.isEmpty()) {
            return false;
        }

        for (UpgradeWorkflow workflow : activeWorkflows) {
            String effectiveStep = getEffectiveCurrentStep(workflow);

            // ⭐ 修复：如果工作流没有 currentStep
            if (effectiveStep == null) {
                // 检查工作流状态
                // ⭐ COMMIT_SUCCESS 和 WORKFLOW_COMPLETED 都算作"已完成"
                if (workflow.getStatus() == UpgradeWorkflow.WorkflowStatus.WORKFLOW_COMPLETED ||
                    workflow.getStatus() == UpgradeWorkflow.WorkflowStatus.COMMIT_SUCCESS) {
                    // 已完成的工作流，算作"当前步骤完成"
                    continue;
                } else {
                    // 还没开始的工作流（WORKFLOW_PENDING），算作"未完成"
                    return false;
                }
            }

            // ⚠️ 修复：使用 batchId + deviceId 查询，更可靠
            // 因为 workflowId 可能没有被正确设置到 DeviceTask
            List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceId(
                workflow.getBatchId(), 
                workflow.getDeviceId()
            );
            
            DeviceTask.TaskType taskType = getTaskTypeByStep(effectiveStep);
            
            Optional<DeviceTask> currentTask = selectLatestTask(tasks.stream()
                .filter(task -> task.getTaskType() == taskType)
                .collect(java.util.stream.Collectors.toList()));

            // ⭐ 修复：如果任务不存在，说明还没开始，返回 false
            if (!currentTask.isPresent()) {
                log.debug("    areAllCurrentStepsCompleted: workflow={}, deviceId={}, taskType={} - 任务不存在", 
                    workflow.getWorkflowId(), workflow.getDeviceId(), taskType);
                return false;
            }
            
            // ⭐ 修复：检查任务状态
            DeviceTask.TaskStatus taskStatus = currentTask.get().getStatus();
            log.debug("    areAllCurrentStepsCompleted: workflow={}, deviceId={}, taskType={}, status={}", 
                workflow.getWorkflowId(), workflow.getDeviceId(), taskType, taskStatus);
            
            // NOT_START 状态：任务已创建但未开始，算作"未完成"
            // PENDING/SCHEDULED/RUNNING 状态：任务正在执行，算作"未完成"
            // COMPLETED 状态：任务已完成
            // FAILED/CANCELLED 状态：任务已结束，但不算"完成"
            if (taskStatus != DeviceTask.TaskStatus.COMPLETED) {
                return false;
            }
        }

        return true;
    }

    /**
     * 辅助方法：根据步骤名称获取任务类型
     */
    public DeviceTask.TaskType getTaskTypeByStep(String step) {
        switch (step.toUpperCase()) {
            case "DOWNLOAD":
                return DeviceTask.TaskType.DOWNLOAD;
            case "BACKUP":
                return DeviceTask.TaskType.BACKUP;
            case "UPGRADE":
                return DeviceTask.TaskType.UPGRADE;
            case "COMMIT":
                return DeviceTask.TaskType.COMMIT;
            case "ROLLBACK":
                return DeviceTask.TaskType.ROLLBACK;
            case "RESTORE":
                return DeviceTask.TaskType.RESTORE;
            default:
                throw new IllegalArgumentException("Unknown step type: " + step);
        }
    }
    
    /**
     * 批量计算并设置工作流状态（用于 DTO 转换时）
     * 
     * 此方法会查询每个 workflow 关联的 DeviceTask，计算状态并设置到 computedStatus 字段
     * 
     * @param workflows 工作流列表
     * @return 设置了计算状态的工作流列表（原对象修改，同时返回）
     */
    public List<UpgradeWorkflow> computeAndSetWorkflowStatuses(List<UpgradeWorkflow> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            return workflows;
        }
        
        for (UpgradeWorkflow workflow : workflows) {
            // 查询该工作流关联的所有 DeviceTask
            List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceId(
                workflow.getBatchId(), 
                workflow.getDeviceId()
            );
            
            // 计算状态
            UpgradeWorkflow.WorkflowStatus computedStatus = calculateWorkflowStatusFromTasks(workflow, tasks);
            
            // 设置到 @Transient 字段
            workflow.setComputedStatus(computedStatus);
            alignCurrentStepWithComputedStatus(workflow, computedStatus);
            
            log.debug("计算工作流状态: workflowId={}, deviceId={}, status={}", 
                workflow.getWorkflowId(), workflow.getDeviceId(), computedStatus);
        }
        
        return workflows;
    }
    
    /**
     * 单个工作流的状态计算（用于实时查询）
     */
    public UpgradeWorkflow.WorkflowStatus computeWorkflowStatus(UpgradeWorkflow workflow) {
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdAndDeviceId(
            workflow.getBatchId(), 
            workflow.getDeviceId()
        );
        
        UpgradeWorkflow.WorkflowStatus status = calculateWorkflowStatusFromTasks(workflow, tasks);
        workflow.setComputedStatus(status);
        alignCurrentStepWithComputedStatus(workflow, status);
        
        return status;
    }

    private void alignCurrentStepWithComputedStatus(
        UpgradeWorkflow workflow,
        UpgradeWorkflow.WorkflowStatus computedStatus
    ) {
        String statusStep = getStepFromWorkflowStatus(computedStatus);
        if (statusStep == null) {
            return;
        }

        String currentStep = workflow.getCurrentStep();
        if (!statusStep.equalsIgnoreCase(currentStep)) {
            log.info("校正 workflow.currentStep: workflowId={}, deviceId={}, from={}, to={}, status={}",
                workflow.getWorkflowId(), workflow.getDeviceId(), currentStep, statusStep, computedStatus);
            workflow.setCurrentStep(statusStep);
        }
    }

    private String getEffectiveCurrentStep(UpgradeWorkflow workflow) {
        String statusStep = getStepFromWorkflowStatus(workflow.getStatus());
        if (statusStep != null) {
            return statusStep;
        }
        return workflow.getCurrentStep();
    }

    private String getStepFromWorkflowStatus(UpgradeWorkflow.WorkflowStatus status) {
        if (status == null) {
            return null;
        }

        String statusName = status.name();
        int separatorIndex = statusName.indexOf('_');
        if (separatorIndex <= 0) {
            return null;
        }

        String step = statusName.substring(0, separatorIndex);
        switch (step) {
            case "DOWNLOAD":
            case "BACKUP":
            case "COMMIT":
            case "ROLLBACK":
                return step;
            case "ACTIVATE":
                return "UPGRADE";
            default:
                return null;
        }
    }

    private Optional<DeviceTask> selectLatestTask(List<DeviceTask> tasks) {
        return tasks.stream()
            .max(Comparator
                .comparing(DeviceTask::getUpdatedTime, Comparator.nullsFirst(LocalDateTime::compareTo))
                .thenComparing(DeviceTask::getCreatedTime, Comparator.nullsFirst(LocalDateTime::compareTo)));
    }
}
