package devicemaintenance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import devicemaintenance.dto.BatchDetailDto;
import devicemaintenance.entity.*;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一批次查询服务
 * 提供跨所有批次类型的统一查询接口
 */
@Service
@Slf4j
public class UnifiedBatchService {

    private final BatchRepository batchRepository;
    private final DeviceTaskRepository deviceTaskRepository;
    private final UpgradeWorkflowRepository upgradeWorkflowRepository;
    private final WorkflowManagementService workflowManagementService;
    private final BatchUpgradeService batchUpgradeService;
    private final DeviceBackupService deviceBackupService;
    private final DeviceRestoreService deviceRestoreService;
    private final SoftwareDownloadService softwareDownloadService;
    private final TaskInfoNotificationService taskInfoNotificationService;
    
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    
    // 构造器注入，使用 @Lazy 避免循环依赖
    public UnifiedBatchService(
            BatchRepository batchRepository,
            DeviceTaskRepository deviceTaskRepository,
            UpgradeWorkflowRepository upgradeWorkflowRepository,
            WorkflowManagementService workflowManagementService,
            @Lazy BatchUpgradeService batchUpgradeService,
            DeviceBackupService deviceBackupService,
            DeviceRestoreService deviceRestoreService,
            SoftwareDownloadService softwareDownloadService,
            TaskInfoNotificationService taskInfoNotificationService) {
        this.batchRepository = batchRepository;
        this.deviceTaskRepository = deviceTaskRepository;
        this.upgradeWorkflowRepository = upgradeWorkflowRepository;
        this.workflowManagementService = workflowManagementService;
        this.batchUpgradeService = batchUpgradeService;
        this.deviceBackupService = deviceBackupService;
        this.deviceRestoreService = deviceRestoreService;
        this.softwareDownloadService = softwareDownloadService;
        this.taskInfoNotificationService = taskInfoNotificationService;
    }

    /**
     * 获取批次详情 (统一接口)
     * 
     * @param batchId 批次ID
     * @return 批次详情
     */
    public BatchDetailDto.BatchDetailResponse getBatchDetail(String batchId) {
        log.info("获取批次详情: batchId={}", batchId);
        
        // 从统一的 Batch 表查找
        Optional<Batch> batchOpt = batchRepository.findById(batchId);
        if (!batchOpt.isPresent()) {
            throw new IllegalArgumentException("Batch does not exist: " + batchId);
        }
        
        return convertBatchToDetail(batchOpt.get());
    }
    
    /**
     * 将批次详情 DTO 转换为 JSON 字符串（供 TaskInfo 通知使用）
     * 
     * @param batchId 批次ID
     * @return JSON 字符串
     */
    public String convertBatchToJsonForNotification(String batchId) {
        try {
            BatchDetailDto.BatchDetailResponse detail = getBatchDetail(batchId);
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            log.error("转换批次详情到JSON失败: batchId={}", batchId, e);
            return "{}";
        }
    }


    /**
     * 获取批次的工作流列表 (仅工作流批次) - 不分页
     * 
     * @param batchId 批次ID
     * @return 工作流列表
     */
    public List<BatchDetailDto.WorkflowInfo> getBatchWorkflows(String batchId) {
        log.info("获取批次工作流列表: batchId={}", batchId);
        
        // 检查是否为工作流批次（升级类型）
        Optional<Batch> batchOpt = batchRepository.findById(batchId);
        if (!batchOpt.isPresent()) {
            throw new IllegalArgumentException("Batch does not exist: " + batchId);
        }
        
        Batch batch = batchOpt.get();
        if (batch.getBatchType() != Batch.BatchType.UPGRADE) {
            log.warn("批次不是升级批次，无工作流: batchId={}, type={}", batchId, batch.getBatchType());
            return new ArrayList<>();
        }
        
        List<UpgradeWorkflow> workflows = upgradeWorkflowRepository.findByBatchId(batchId);
        
        // ✅ 批量计算并设置工作流状态（设置到 @Transient 字段）
        workflowManagementService.computeAndSetWorkflowStatuses(workflows);
        
        return workflows.stream()
                .map(this::convertWorkflowToInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取批次的工作流列表（分页）
     * 
     * @param batchId 批次ID
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PagedResult<BatchDetailDto.WorkflowInfo> getBatchWorkflowsPaged(String batchId, Integer pageNum, Integer pageSize) {
        log.info("获取批次工作流列表（分页）: batchId={}, pageNum={}, pageSize={}", batchId, pageNum, pageSize);
        
        // 检查是否为工作流批次（升级类型）
        Optional<Batch> batchOpt = batchRepository.findById(batchId);
        if (!batchOpt.isPresent()) {
            throw new IllegalArgumentException("Batch does not exist: " + batchId);
        }
        
        Batch batch = batchOpt.get();
        if (batch.getBatchType() != Batch.BatchType.UPGRADE) {
            log.warn("批次不是升级批次，无工作流: batchId={}, type={}", batchId, batch.getBatchType());
            return new PagedResult<>(new ArrayList<>(), 0, pageNum, pageSize);
        }
        
        // 获取所有工作流
        List<UpgradeWorkflow> allWorkflows = upgradeWorkflowRepository.findByBatchId(batchId);
        int totalCount = allWorkflows.size();
        
        // ✅ 批量计算并设置工作流状态（设置到 @Transient 字段）
        workflowManagementService.computeAndSetWorkflowStatuses(allWorkflows);
        
        // 手动分页
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCount);
        
        List<UpgradeWorkflow> pagedWorkflows;
        if (startIndex >= totalCount) {
            // 超出范围，返回空列表
            pagedWorkflows = new ArrayList<>();
        } else {
            pagedWorkflows = allWorkflows.subList(startIndex, endIndex);
        }
        
        List<BatchDetailDto.WorkflowInfo> workflowInfos = pagedWorkflows.stream()
                .map(this::convertWorkflowToInfo)
                .collect(Collectors.toList());
        
        log.info("✅ 分页查询完成: total={}, pageNum={}, pageSize={}, returned={}", 
            totalCount, pageNum, pageSize, workflowInfos.size());
        
        return new PagedResult<>(workflowInfos, totalCount, pageNum, pageSize);
    }
    
    /**
     * 分页结果封装
     */
    public static class PagedResult<T> {
        private List<T> data;
        private int total;
        private int pageNum;
        private int pageSize;
        private int totalPages;
        
        public PagedResult(List<T> data, int total, int pageNum, int pageSize) {
            this.data = data;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        }
        
        public List<T> getData() {
            return data;
        }
        
        public void setData(List<T> data) {
            this.data = data;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
        
        public int getPageNum() {
            return pageNum;
        }
        
        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 私有转换方法
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 转换 Batch 到详情响应（统一方法，支持所有类型）
     */
    private BatchDetailDto.BatchDetailResponse convertBatchToDetail(Batch batch) {
        BatchDetailDto.BatchDetailResponse response = new BatchDetailDto.BatchDetailResponse();

        // ⭐ 调试日志：查看从数据库读取的 scheduledTime 值
        log.debug("convertBatchToDetail: batchId={}, batchType={}, scheduledMode={}, scheduledTime={}",
            batch.getBatchId(), batch.getBatchType(), batch.getScheduledMode(), batch.getScheduledTime());

        // 判断是否为升级批次（有工作流）
        boolean isUpgradeBatch = batch.getBatchType() == Batch.BatchType.UPGRADE;
        
        // ⭐ 对于升级批次，实时计算状态和统计
        int successCount = batch.getSuccessCount();
        int failedCount = batch.getFailedCount();
        int runningCount = batch.getRunningCount() != null ? batch.getRunningCount() : 0;
        Batch.BatchStatus resolvedBatchStatus = batch.getStatus();
        List<UpgradeWorkflow> workflows = Collections.emptyList();
        
        if (isUpgradeBatch) {
            // 获取工作流并计算实时状态
            workflows = upgradeWorkflowRepository.findByBatchId(batch.getBatchId());
            workflowManagementService.computeAndSetWorkflowStatuses(workflows);
            
            // ⭐ 实时计算统计数量
            successCount = (int) workflows.stream().filter(wf -> wf.isCompleted()).count();
            failedCount = (int) workflows.stream().filter(wf -> wf.isFailed()).count();
            // ⭐ 修复：runningCount 应该是"既未完成也未失败"的数量
            runningCount = workflows.size() - successCount - failedCount;
            
            log.info("  实时统计: 成功={}, 失败={}, 运行中={}", successCount, failedCount, runningCount);
            
            // ⭐ 总是实时计算批次状态（不依赖数据库中的旧状态）
            Batch.BatchStatus realtimeStatus = calculateRealtimeStatusSafely(batch, workflows);
            resolvedBatchStatus = realtimeStatus;
            
            log.info("  状态对比: 数据库={}, 实时={}", batch.getStatus(), realtimeStatus);
            
            // ⭐ 检查是否需要同步更新 Batch 状态（防止 Kafka 消息丢失或延迟）
            boolean needsSync = (realtimeStatus != batch.getStatus()) ||
                               (successCount != batch.getSuccessCount()) ||
                               (failedCount != batch.getFailedCount()) ||
                               (runningCount != (batch.getRunningCount() != null ? batch.getRunningCount() : 0));
            
            if (needsSync) {
                log.warn("  ⚠️ 发现状态或统计数据不一致，主动同步 Batch");
                log.warn("    数据库: 状态={}, 成功={}, 失败={}, 运行中={}", 
                    batch.getStatus(), batch.getSuccessCount(), batch.getFailedCount(), batch.getRunningCount());
                log.warn("    实时:   状态={}, 成功={}, 失败={}, 运行中={}", 
                    realtimeStatus, successCount, failedCount, runningCount);
                
                // 更新 Batch 状态和统计
                batch.updateBatchStatus(realtimeStatus);
                batch.updateCounts(successCount, failedCount, runningCount);
                batchRepository.save(batch);
                
                log.info("  ✅ Batch 状态已同步");
            }
        }
        
        // 基本信息（所有类型通用）
        response.setBatchId(batch.getBatchId());
        response.setBatchName(batch.getBatchName());
        response.setBatchType(batch.getBatchType().name());
        // ⭐ 添加 taskType 字段（与首次创建通知保持一致）
        response.setTaskType("BATCH_" + batch.getBatchType().name());
        response.setStatus(resolvedBatchStatus.name());
        response.setDeviceCount(batch.getDeviceCount());
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setRunningCount(runningCount);
        
        // 时间信息（所有类型通用）
        response.setCreatedTime(batch.getCreatedTime());
        response.setUpdatedTime(batch.getUpdatedTime());
        response.setStartedTime(batch.getStartedTime());
        response.setCompletedTime(batch.getCompletedTime());
        response.setCreatedBy(batch.getCreatedBy());

        response.setIsWorkflowBatch(isUpgradeBatch);

        // ⭐ 定时执行相关字段（所有类型的批次都可能需要定时执行）
        response.setScheduledMode(batch.getScheduledMode() != null ? batch.getScheduledMode().name() : null);
        response.setScheduledTime(batch.getScheduledTime());

        // ⭐ 调试日志：查看从数据库读取和设置的值
        log.info("  批次定时字段: scheduledMode={}, scheduledTime={} (from DB: {})",
            response.getScheduledMode(), response.getScheduledTime(), batch.getScheduledTime());

        if (isUpgradeBatch) {
            // 升级批次特有字段
            response.setExecutionMode(batch.getExecutionMode() != null ? batch.getExecutionMode().name() : null);

            response.setEnableDownload(batch.getEnableDownload());
            response.setEnableBackup(batch.getEnableBackup());
            response.setEnableUpgrade(batch.getEnableUpgrade());
            response.setMaxRetryCount(batch.getMaxRetryCount());
            response.setFilePath(batch.getFilePath());
            response.setBackupBasePath(batch.getBackupBasePath());
            response.setOperationInterval(batch.getOperationInterval());
            
            // ⭐ 工作流列表（已在前面查询并计算状态）
            response.setWorkflows(workflows.stream()
                    .map(this::convertWorkflowToInfo)
                    .collect(Collectors.toList()));
        } else {
            // 备份/恢复批次特有字段
            response.setBasePath(batch.getBasePath());
            response.setDetail(batch.getDetail());
        }
        
        // 通用字段
        response.setSftpServerName(batch.getSftpServerName());
        response.setRemark(batch.getRemark());
        response.setErrorMessage(batch.getErrorMessage());
        
        // 获取设备任务列表
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batch.getBatchId());
        boolean includeDebug = batch.getDebug() != null && batch.getDebug();
        
        // 对于升级批次，需要匹配 workflowId
        Map<String, String> deviceToWorkflowMap = new HashMap<>();
        if (isUpgradeBatch && response.getWorkflows() != null) {
            deviceToWorkflowMap = response.getWorkflows().stream()
                .collect(Collectors.toMap(
                    BatchDetailDto.WorkflowInfo::getDeviceId, 
                    BatchDetailDto.WorkflowInfo::getWorkflowId, 
                    (a, b) -> a
                ));
        }
        
        final Map<String, String> workflowMap = deviceToWorkflowMap;
        response.setDeviceTasks(tasks.stream()
                .map(task -> {
                    BatchDetailDto.DeviceTaskInfo info = convertDeviceTaskToInfo(task, includeDebug);
                    if (workflowMap.containsKey(task.getDeviceId())) {
                        info.setWorkflowId(workflowMap.get(task.getDeviceId()));
                    }
                    return info;
                })
                .collect(Collectors.toList()));
        
        return response;
    }

    private Batch.BatchStatus calculateRealtimeStatusSafely(Batch batch, List<UpgradeWorkflow> workflows) {
        try {
            return batchUpgradeService.calculateBatchStatusPublic(batch, workflows);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("inconsistent currentStep values")) {
                log.warn("Batch detail fallback to persisted status due to inconsistent workflow steps: batchId={}, status={}, message={}",
                    batch.getBatchId(), batch.getStatus(), ex.getMessage());
                return batch.getStatus();
            }
            throw ex;
        }
    }

    /**
     * 转换 UpgradeWorkflow 到工作流信息（简化版 - 只包含流程控制信息）
     */
    private BatchDetailDto.WorkflowInfo convertWorkflowToInfo(UpgradeWorkflow workflow) {
        BatchDetailDto.WorkflowInfo info = new BatchDetailDto.WorkflowInfo();
        
        info.setWorkflowId(workflow.getWorkflowId());
        info.setDeviceId(workflow.getDeviceId());
        
        // 工作流核心状态
        String workflowStatus = workflow.getStatus() != null ? workflow.getStatus().name() : null;
        info.setStatus(workflowStatus);
        info.setCurrentStep(workflow.getCurrentStep());
        
        // 重试信息
        info.setRetryCount(workflow.getRetryCount());
        info.setMaxRetryCount(workflow.getMaxRetryCount());
        info.setDisplayStatus(buildWorkflowDisplayStatus(workflowStatus, workflow.getRetryCount(), workflow.getCurrentStep()));
        info.setStatusText(buildWorkflowStatusText(workflowStatus, workflow.getRetryCount(), workflow.getCurrentStep()));
        
        return info;
    }

    /**
     * 转换 DeviceTask 到任务信息（包含所有任务细节）
     * 
     * @param task DeviceTask 实体
     * @param includeDebug 是否包含调试信息（由batch的debug字段控制）
     */
    private BatchDetailDto.DeviceTaskInfo convertDeviceTaskToInfo(DeviceTask task, boolean includeDebug) {
        BatchDetailDto.DeviceTaskInfo info = new BatchDetailDto.DeviceTaskInfo();
        
        info.setTaskId(task.getTaskId());
        info.setBatchId(task.getBatchId());
        info.setBatchName(task.getBatchName());
        // workflowId 将由调用方根据 deviceId 匹配设置
        info.setDeviceId(task.getDeviceId());
        info.setDeviceName(task.getDeviceName());
        info.setDeviceIp(task.getDeviceIp());
        
        info.setTaskType(task.getTaskType() != null ? task.getTaskType().name() : null);
        String taskStatus = task.getStatus() != null ? task.getStatus().name() : null;
        info.setStatus(taskStatus);
        info.setDisplayStatus(buildTaskDisplayStatus(taskStatus, task.getRetryCount(), info.getTaskType()));
        info.setStatusText(buildTaskStatusText(taskStatus, task.getRetryCount(), info.getTaskType()));
        
        info.setFilePath(task.getFilePath());
        info.setBackupFilePath(task.getBackupFilePath());
        info.setBackupFileName(task.getBackupFileName());
        info.setCurrentVersion(task.getCurrentVersion());
        info.setPreviousVersion(task.getPreviousVersion());
        info.setTargetVersion(task.getTargetVersion());
        
        info.setRetryCount(task.getRetryCount());
        info.setErrorMessage(task.getErrorMessage());
        
        // ✅ 总是包含 debugPayload 字段，即使为 null（用户要求明确显示该字段）
        info.setDebugPayload(task.getDebugPayload());
        
        info.setCreatedTime(task.getCreatedTime());
        info.setUpdatedTime(task.getUpdatedTime());
        info.setStartedTime(task.getStartedTime());
        info.setCompletedTime(task.getCompletedTime());
        
        return info;
    }

    private String buildWorkflowDisplayStatus(String workflowStatus, Integer retryCount, String currentStep) {
        int retries = retryCount != null ? retryCount : 0;
        if (retries > 0) {
            if (workflowStatus != null && (workflowStatus.endsWith("_PENDING") || workflowStatus.endsWith("_RUNNING"))) {
                return "RETRYING";
            }
            if (workflowStatus != null && workflowStatus.endsWith("_FAILED")) {
                return "RETRY_FAILED";
            }
            if ("WORKFLOW_COMPLETED".equals(workflowStatus) || "COMMIT_SUCCESS".equals(workflowStatus)
                || (workflowStatus != null && workflowStatus.endsWith("_SUCCESS"))) {
                return "RETRY_SUCCESS";
            }
        }

        if (workflowStatus == null) {
            return null;
        }
        if (workflowStatus.endsWith("_PENDING")) {
            return "PENDING";
        }
        if (workflowStatus.endsWith("_RUNNING")) {
            return "RUNNING";
        }
        if (workflowStatus.endsWith("_FAILED")) {
            return "FAILED";
        }
        if ("WORKFLOW_COMPLETED".equals(workflowStatus) || "COMMIT_SUCCESS".equals(workflowStatus)
            || workflowStatus.endsWith("_SUCCESS")) {
            return "COMPLETED";
        }
        if ("WORKFLOW_CANCELLED".equals(workflowStatus)) {
            return "CANCELLED";
        }
        return workflowStatus;
    }

    private String buildWorkflowStatusText(String workflowStatus, Integer retryCount, String currentStep) {
        String stepText = stepToText(resolveWorkflowDisplayStep(workflowStatus, currentStep));
        int retries = retryCount != null ? retryCount : 0;
        if (retries > 0) {
            if (workflowStatus != null && (workflowStatus.endsWith("_PENDING") || workflowStatus.endsWith("_RUNNING"))) {
                return stepText + "重试中";
            }
            if (workflowStatus != null && workflowStatus.endsWith("_FAILED")) {
                return stepText + "重试失败";
            }
            if ("WORKFLOW_COMPLETED".equals(workflowStatus) || "COMMIT_SUCCESS".equals(workflowStatus)
                || (workflowStatus != null && workflowStatus.endsWith("_SUCCESS"))) {
                return stepText + "重试成功";
            }
        }

        if (workflowStatus == null) {
            return null;
        }
        if (workflowStatus.endsWith("_PENDING")) {
            return stepText + "待执行";
        }
        if (workflowStatus.endsWith("_RUNNING")) {
            return stepText + "执行中";
        }
        if (workflowStatus.endsWith("_FAILED")) {
            return stepText + "失败";
        }
        if ("WORKFLOW_COMPLETED".equals(workflowStatus) || "COMMIT_SUCCESS".equals(workflowStatus)
            || workflowStatus.endsWith("_SUCCESS")) {
            return stepText + "成功";
        }
        if ("WORKFLOW_CANCELLED".equals(workflowStatus)) {
            return "已取消";
        }
        if ("WORKFLOW_PENDING".equals(workflowStatus)) {
            return "待执行";
        }
        return workflowStatus;
    }

    private String resolveWorkflowDisplayStep(String workflowStatus, String currentStep) {
        if (workflowStatus != null) {
            int separator = workflowStatus.indexOf('_');
            if (separator > 0) {
                return workflowStatus.substring(0, separator);
            }
        }
        return currentStep;
    }

    private String buildTaskDisplayStatus(String taskStatus, Integer retryCount, String taskType) {
        int retries = retryCount != null ? retryCount : 0;
        if (retries > 0) {
            if ("PENDING".equals(taskStatus) || "RUNNING".equals(taskStatus)) {
                return "RETRYING";
            }
            if ("FAILED".equals(taskStatus)) {
                return "RETRY_FAILED";
            }
            if ("COMPLETED".equals(taskStatus)) {
                return "RETRY_SUCCESS";
            }
        }
        return taskStatus;
    }

    private String buildTaskStatusText(String taskStatus, Integer retryCount, String taskType) {
        String stepText = stepToText(taskType);
        int retries = retryCount != null ? retryCount : 0;
        if (retries > 0) {
            if ("PENDING".equals(taskStatus) || "RUNNING".equals(taskStatus)) {
                return stepText + "重试中";
            }
            if ("FAILED".equals(taskStatus)) {
                return stepText + "重试失败";
            }
            if ("COMPLETED".equals(taskStatus)) {
                return stepText + "重试成功";
            }
        }

        if (taskStatus == null) {
            return null;
        }
        switch (taskStatus) {
            case "NOT_START":
                return stepText + "未开始";
            case "SCHEDULED":
                return stepText + "已计划";
            case "PENDING":
                return stepText + "待执行";
            case "RUNNING":
                return stepText + "执行中";
            case "COMPLETED":
                return stepText + "成功";
            case "FAILED":
                return stepText + "失败";
            case "CANCELLED":
                return stepText + "已取消";
            case "EXPIRED":
                return stepText + "已过期";
            case "IDLE":
                return "空闲";
            default:
                return taskStatus;
        }
    }

    private String stepToText(String step) {
        if (step == null || step.trim().isEmpty()) {
            return "任务";
        }
        switch (step.toUpperCase()) {
            case "DOWNLOAD":
                return "下载";
            case "BACKUP":
                return "备份";
            case "UPGRADE":
            case "ACTIVATE":
                return "升级";
            case "COMMIT":
                return "提交";
            case "RESTORE":
                return "恢复";
            case "ROLLBACK":
                return "回滚";
            default:
                return step;
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 批次取消和删除操作（跨所有类型）
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 取消所有运行中的批次（所有类型）
     */
    public Map<String, Object> cancelAllRunningBatches() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚫 取消所有运行中的批次（所有类型）");
        
        // 1. 查询所有可取消状态的批次（执行中 + 等待中）
        List<Batch> runningBatches = new ArrayList<>();
        // 自动模式执行中状态
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.RUNNING));
        // 手动模式执行中状态（细粒度）
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.DOWNLOADING));
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.BACKING_UP));
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.UPGRADING));
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.COMMITTING));
        // 手动模式等待状态
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.READY_TO_PROCEED));
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.READY_TO_BACKUP));
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.READY_TO_UPGRADE));
        runningBatches.addAll(batchRepository.findByStatus(Batch.BatchStatus.READY_TO_COMMIT));
        log.info("  找到 {} 个运行中/等待中的批次", runningBatches.size());
        
        if (runningBatches.isEmpty()) {
            log.info("✅ 没有运行中的批次需要取消");
            return createOperationResult(0, 0, new ArrayList<>(), new HashMap<>());
        }
        
        // 2. 按类型分组统计
        Map<Batch.BatchType, Long> typeCount = runningBatches.stream()
            .collect(Collectors.groupingBy(Batch::getBatchType, Collectors.counting()));
        log.info("  批次类型分布: {}", typeCount);
        
        // 3. 逐个取消
        List<String> successBatchIds = new ArrayList<>();
        Map<String, String> failureDetails = new HashMap<>();
        
        for (Batch batch : runningBatches) {
            String batchId = batch.getBatchId();
            Batch.BatchType batchType = batch.getBatchType();
            
            try {
                log.info("  取消批次: {} ({}) - 类型: {}", batchId, batch.getBatchName(), batchType);
                
                // 根据批次类型调用对应的取消方法
                if (batchType == Batch.BatchType.UPGRADE) {
                    // 升级批次：使用 BatchUpgradeService 的完整工作流取消逻辑
                    batchUpgradeService.cancelBatch(batchId);
                } else {
                    // 其他类型批次：使用通用取消逻辑
                    cancelNonUpgradeBatch(batch);
                }
                
                successBatchIds.add(batchId);
                log.info("  ✓ 取消成功");
            } catch (Exception e) {
                log.error("  ✗ 取消失败: {}", e.getMessage(), e);
                failureDetails.put(batchId, e.getMessage());
            }
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ 取消所有运行批次完成: 成功{}个，失败{}个", successBatchIds.size(), failureDetails.size());
        
        return createOperationResult(successBatchIds.size(), failureDetails.size(), successBatchIds, failureDetails);
    }

    /**
     * 删除所有非运行中的批次（所有类型）
     * 
     * ⚠️ 一对多架构下的处理策略：
     * - 删除批次记录本身
     * - ✅ 保留所有 DeviceTask 历史记录（包括 debugPayload）
     * - ✅ 解除 DeviceTask 的批次关联（batchId 置空）
     * - ⚠️ 保留 Workflow 记录（升级批次）
     * - 只删除真正执行中状态以外的批次（RUNNING、DOWNLOADING、BACKING_UP、UPGRADING、COMMITTING）
     */
    public Map<String, Object> deleteAllNonRunningBatches() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🗑️ 删除所有非运行中的批次（所有类型）");
        
        try {
            // 1. 先查询要删除的批次信息（用于日志），使用原生 SQL 避免枚举映射问题
            List<Object[]> batchInfoList = batchRepository.findAllNonRunningBatchInfoNative();
            log.info("  找到 {} 个非运行中的批次（包括旧状态）", batchInfoList.size());
            
            if (batchInfoList.isEmpty()) {
                log.info("✅ 没有非运行中的批次需要删除");
                return createOperationResult(0, 0, new ArrayList<>(), new HashMap<>());
            }
            
            // 2. 记录要删除的批次信息
            List<String> batchIdsToDelete = new ArrayList<>();
            Map<String, Integer> typeCount = new HashMap<>();
            
            for (Object[] row : batchInfoList) {
                String batchId = (String) row[0];
                String batchName = (String) row[1];
                String batchType = (String) row[2];
                String status = (String) row[3];
                
                batchIdsToDelete.add(batchId);
                typeCount.put(batchType, typeCount.getOrDefault(batchType, 0) + 1);
                
                log.info("  将删除: {} ({}) - 类型: {}, 状态: {}", batchId, batchName, batchType, status);
            }
            
            log.info("  批次类型分布: {}", typeCount);
            
            // 3. ✅ 解除 Workflow 关联的 DeviceTask 的批次关联（不删除记录）
            int unlinkedWorkflowTasks = 0;
            for (String batchId : batchIdsToDelete) {
                List<UpgradeWorkflow> workflows = upgradeWorkflowRepository.findByBatchId(batchId);
                if (!workflows.isEmpty()) {
                    for (UpgradeWorkflow workflow : workflows) {
                        // ✅ 解除 DeviceTask 的批次关联（不删除记录）
                        List<DeviceTask> tasks = deviceTaskRepository.findByWorkflowId(workflow.getWorkflowId());
                        for (DeviceTask task : tasks) {
                            task.setBatchId(null);  // 解除批次关联
                            // ✅ 保留所有其他字段（包括 debugPayload）
                        }
                        deviceTaskRepository.saveAll(tasks);
                        unlinkedWorkflowTasks += tasks.size();
                        log.debug("    解除 {} 个 DeviceTask 的批次关联 (workflowId: {})", tasks.size(), workflow.getWorkflowId());
                    }
                    // ⚠️ 保留 Workflow 记录（历史数据）
                    // upgradeWorkflowRepository.deleteAll(workflows);  // 不再删除
                }
            }
            log.info("  ✅ 已解除 {} 个 Workflow 关联的 DeviceTask 的批次关联（保留历史记录）", unlinkedWorkflowTasks);
            
            // 4. ✅ 解除非升级类型批次关联的 DeviceTask 的批次关联（不删除记录）
            int unlinkedTasks = 0;
            for (String batchId : batchIdsToDelete) {
                List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
                if (!tasks.isEmpty()) {
                    for (DeviceTask task : tasks) {
                        task.setBatchId(null);  // 解除批次关联
                        // ✅ 保留所有其他字段（包括 debugPayload）
                    }
                    deviceTaskRepository.saveAll(tasks);
                    unlinkedTasks += tasks.size();
                }
            }
            log.info("  ✅ 已解除 {} 个非升级批次的 DeviceTask 的批次关联（保留历史记录）", unlinkedTasks);
            
            // 5. 使用原生 SQL 批量删除批次记录
            int deletedCount = batchRepository.deleteAllNonRunningBatchesNative();
            log.info("  ✅ 已删除 {} 个批次记录", deletedCount);
            
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ 删除所有非运行批次完成: 删除批次={}, 保留DeviceTask={}", deletedCount, unlinkedWorkflowTasks + unlinkedTasks);
            
            return createOperationResult(deletedCount, 0, batchIdsToDelete, new HashMap<>());
            
        } catch (Exception e) {
            log.error("❌ 删除非运行批次失败: {}", e.getMessage(), e);
            Map<String, String> failureDetails = new HashMap<>();
            failureDetails.put("error", e.getMessage());
            return createOperationResult(0, 1, new ArrayList<>(), failureDetails);
        }
    }

    /**
     * 取消非升级类型的批次（BACKUP/RESTORE/DOWNLOAD）
     */
    private void cancelNonUpgradeBatch(Batch batch) {
        String batchId = batch.getBatchId();
        
        // 1. 更新批次状态为 CANCELLED
        batch.setStatus(Batch.BatchStatus.CANCELLED);
        batch.setUpdatedTime(java.time.LocalDateTime.now());
        batch.setCompletedTime(java.time.LocalDateTime.now());
        batch.setErrorMessage("Batch cancelled by user");
        batchRepository.save(batch);
        log.info("    已将批次状态更新为 CANCELLED");
        
        // 2. 取消所有关联的 DeviceTask（设置状态为 CANCELLED）
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
        if (tasks != null && !tasks.isEmpty()) {
            int cancelledCount = 0;
            for (DeviceTask task : tasks) {
                // 取消 RUNNING、PENDING、SCHEDULED 状态的任务
                if (task.getStatus() == DeviceTask.TaskStatus.RUNNING || 
                    task.getStatus() == DeviceTask.TaskStatus.PENDING ||
                    task.getStatus() == DeviceTask.TaskStatus.SCHEDULED) {
                    task.setStatus(DeviceTask.TaskStatus.CANCELLED);
                    task.setUpdatedTime(java.time.LocalDateTime.now());
                    task.setCompletedTime(java.time.LocalDateTime.now());
                    task.setErrorMessage("Batch cancelled");
                    cancelledCount++;
                }
            }
            if (cancelledCount > 0) {
                deviceTaskRepository.saveAll(tasks);
                log.info("    已取消 {} 个关联的设备任务", cancelledCount);
            }
        }
        
        // 3. ✅ 通知 TaskInfo 批次取消（如果批次已通知）
        if (batch.getBatchActionTime() != null) {
            try {
                batchUpgradeService.notifyBatchUpgradeStatus(batch);
                log.info("    ✓ 已通知 TaskInfo 批次取消");
            } catch (Exception e) {
                log.warn("    通知 TaskInfo 失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 删除非升级类型的批次（BACKUP/RESTORE/DOWNLOAD）
     * 
     * ⚠️ 一对多架构下不删除 DeviceTask，只解除批次关联
     * - 保留历史任务记录（包括 debugPayload）
     * - 将任务的 batchId 置空
     * - 标记批次名称为"已删除"
     */
    private void deleteNonUpgradeBatch(Batch batch) {
        String batchId = batch.getBatchId();
        
        // 1. ✅ 解除 DeviceTask 的批次关联（不删除记录）
        List<DeviceTask> tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc(batchId);
        if (tasks != null && !tasks.isEmpty()) {
            for (DeviceTask task : tasks) {
                task.setBatchId(null);  // 解除批次关联
                task.setBatchName(batch.getBatchName() + " (批次已删除)");  // 标记批次已删除
                // ✅ 保留所有其他字段（包括 debugPayload）
            }
            deviceTaskRepository.saveAll(tasks);
            log.info("    ✅ 已解除 {} 个设备任务的批次关联（保留历史记录）", tasks.size());
        }
        
        // 2. ✅ 通知 TaskInfo 批次删除（如果批次已通知）
        if (batch.getBatchActionTime() != null) {
            try {
                taskInfoNotificationService.notifyBatchDeleted(batch);
                log.info("    ✓ 已通知 TaskInfo 批次删除");
            } catch (Exception e) {
                log.warn("    通知 TaskInfo 失败: {}", e.getMessage());
            }
        }
        
        // 3. 删除批次记录
        batchRepository.delete(batch);
        log.info("    ✅ 已删除批次记录");
    }

    /**
     * 创建操作结果
     */
    private Map<String, Object> createOperationResult(
            int successCount, 
            int failureCount, 
            List<String> successBatchIds, 
            Map<String, String> failureDetails) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("successBatchIds", successBatchIds);
        result.put("failureDetails", failureDetails);
        return result;
    }
}

