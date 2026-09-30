package devicemaintenance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.Batch;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次详情 DTO
 * 统一的批次详情响应，适用于所有批次类型
 */
public class BatchDetailDto {

    /**
     * 批次详情响应
     */
    @Data
    public static class BatchDetailResponse {
        // 批次基本信息
        private String batchId;
        private String batchName;
        private String batchType;  // UPGRADE, BACKUP, RESTORE
        private String taskType;   // BATCH_UPGRADE, BATCH_BACKUP, BATCH_RESTORE (与 TaskInfo 通知保持一致)
        private String status;
        private Integer deviceCount;
        private Integer successCount;
        private Integer failedCount;
        private Integer runningCount;
        
        // 时间信息
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
        private LocalDateTime startedTime;
        private LocalDateTime completedTime;
        private String createdBy;
        
        // 工作流信息 (可选 - 仅工作流批次有值)
        private String executionMode;  // MANUAL/AUTO, null = 非工作流
        private String scheduledMode;  // IMMEDIATE/SCHEDULED
        private Long scheduledTime;  // 定时执行时间戳（单位：毫秒）
        private Boolean enableDownload;
        private Boolean enableBackup;
        private Boolean enableUpgrade;
        private Integer maxRetryCount;
        
        // ⚠️ 已删除 downloadStatus, backupStatus, upgradeStatus, commitStatus 字段
        // 这些字段没有被正确维护，状态应该从 workflow 和 deviceTask 计算得出
        
        // 文件路径
        private String filePath;  // 升级文件路径
        private String basePath;  // 备份/恢复基础路径
        private String backupBasePath;  // 升级时备份基础路径
        private String sftpServerName;
        private Integer operationInterval;
        private String remark;  // 备注信息
        
        // 错误信息
        private String errorMessage;
        
        // 详情信息
        private String detail;
        
        // 关联的工作流 (仅工作流批次有值)
        private List<WorkflowInfo> workflows;
        
        // 关联的设备任务
        private List<DeviceTaskInfo> deviceTasks;
        
        // 是否为工作流批次
        private Boolean isWorkflowBatch;
    }

    /**
     * 工作流信息（简化版 - 仅流程控制信息）
     * 详细任务信息请查看 deviceTasks
     */
    @Data
    public static class WorkflowInfo {
        private String workflowId;
        private String deviceId;
        
        // 工作流核心状态
        private String status;  // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
        private String displayStatus;  // RETRYING, RETRY_FAILED, RETRY_SUCCESS, RUNNING...
        private String statusText;     // 中文展示态
        private String currentStep;  // DOWNLOAD, BACKUP, UPGRADE
        
        // 重试信息
        private Integer retryCount;
        private Integer maxRetryCount;
    }

    /**
     * 设备任务信息（包含任务详细状态、错误信息、时间戳等）
     */
    @Data
    public static class DeviceTaskInfo {
        private String taskId;
        private String batchId;
        private String batchName;
        private String workflowId;  // 关联的工作流ID（如果属于工作流）
        private String deviceId;
        private String deviceName;
        private String deviceIp;
        
        private String taskType;  // DOWNLOAD, BACKUP, RESTORE, UPGRADE, ROLLBACK, COMMIT
        private String status;    // IDLE, SCHEDULED, PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, EXPIRED
        private String displayStatus;  // RETRYING, RETRY_FAILED, RETRY_SUCCESS, RUNNING...
        private String statusText;     // 中文展示态
        
        private String filePath;
        private String backupFilePath;
        private String backupFileName;
        private String currentVersion;
        private String previousVersion;
        private String targetVersion;
        
        private Integer retryCount;
        private String errorMessage;
        
        /**
         * 调试信息（JSON字符串）
         * 包含：stack trace, 请求参数, RPC payload 等
         * 仅当创建batch时设置 debug=true 才返回
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String debugPayload;
        
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
        private LocalDateTime startedTime;
        private LocalDateTime completedTime;
    }

    /**
     * 批次任务列表请求
     */
    @Data
    public static class BatchTasksRequest {
        private String batchId;
    }

    /**
     * 批次工作流列表请求
     */
    @Data
    public static class BatchWorkflowsRequest {
        private String batchId;
    }
}

