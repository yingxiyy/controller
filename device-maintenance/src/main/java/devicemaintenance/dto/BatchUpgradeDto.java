package devicemaintenance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批量升级相关的数据传输对象
 */
public class BatchUpgradeDto {

    /**
     * 创建批量升级请求（按照 Swagger 定义）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBatchUpgradeRequest {
        /**
         * 批次名称（必填）：升级操作必须在batch中进行，因为需要工作流管理
         */
        @NotBlank(message = "批次名称不能为空")
        private String batchName;

        /**
         * 设备ID列表
         */
        @NotEmpty(message = "设备列表不能为空")
        private List<String> deviceIds;

        /**
         * 升级文件路径（Swagger 字段名：upgradeFilePath）
         */
        @NotBlank(message = "升级文件路径不能为空")
        private String upgradeFilePath;
        
        /**
         * 目标版本（升级步骤需要）
         */
        private String targetVersion;

        /**
         * 执行模式：MANUAL（手动流转）、AUTO（自动流转）
         */
        @NotBlank(message = "执行模式不能为空")
        private String executionMode;

        /**
         * 调度模式：IMMEDIATE（立即执行）、SCHEDULED（定时执行）
         */
        private String scheduledMode = "IMMEDIATE";

        /**
         * 定时执行时间戳（仅当scheduledMode为SCHEDULED时有效，单位：毫秒）
         */
        private Long scheduledTime;

        /**
         * 是否启用下载步骤
         */
        private Boolean enableDownload = true;

        /**
         * 是否启用备份步骤
         */
        private Boolean enableBackup = true;

        /**
         * 是否启用升级步骤
         */
        private Boolean enableUpgrade = true;

        /**
         * 操作间隔时间（分钟），取值范围 1-60
         */
        private Integer operationInterval = 1;

        /**
         * SFTP服务器名称（可选）
         */
        private String sftpServerName;
        
        /**
         * 备份根路径（启用备份时必填）
         */
        private String backupBasePath;

        /**
         * 最大重试次数
         */
        private Integer maxRetryCount = 3;

        /**
         * 是否启用调试模式（保存RPC payload）
         */
        private Boolean debug = false;
    }

    /**
     * 批量升级响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchUpgradeResponse {
        /**
         * 操作是否成功
         */
        private boolean success;

        /**
         * 响应消息
         */
        private String message;

        /**
         * 批次ID
         */
        private String batchId;

        /**
         * 批次名称
         */
        private String batchName;
        
        /**
         * 升级文件路径
         */
        private String upgradeFilePath;
        
        /**
         * SFTP服务器名称
         */
        private String sftpServerName;
        
        /**
         * 操作间隔（分钟）
         */
        private Integer operationInterval;

        /**
         * 设备数量
         */
        private Integer deviceCount;

        /**
         * 执行模式：MANUAL/AUTO
         */
        private String executionMode;
        
        /**
         * 启动时机：IMMEDIATE/SCHEDULED
         */
        private String scheduledMode;

        /**
         * 定时执行时间戳（单位：毫秒）
         */
        private Long scheduledTime;
        
        /**
         * 是否启用下载步骤
         */
        private Boolean enableDownload;
        
        /**
         * 是否启用备份步骤
         */
        private Boolean enableBackup;
        
        /**
         * 是否启用升级步骤
         */
        private Boolean enableUpgrade;
        
        /**
         * 最大重试次数
         */
        private Integer maxRetryCount;
        
        /**
         * 是否启用调试模式
         */
        private Boolean debug;

        /**
         * 当前执行步骤
         */
        private String currentStep;

        /**
         * 批次状态
         */
        private String status;
        
        // ⚠️ 已删除 downloadStatus, backupStatus, upgradeStatus, commitStatus 字段
        
        /**
         * 成功任务数
         */
        private Integer successCount;
        
        /**
         * 失败任务数
         */
        private Integer failedCount;

        /**
         * 创建时间
         */
        private LocalDateTime createdTime;

        /**
         * 更新时间
         */
        private LocalDateTime updatedTime;

        /**
         * 开始执行时间
         */
        private LocalDateTime startedTime;

        /**
         * 完成时间
         */
        private LocalDateTime completedTime;
    }

    /**
     * 查询批量升级请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetUpgradeBatchesRequest {
        /**
         * 状态过滤条件（可选）
         */
        private String status;

        /**
         * 批次名称过滤条件（可选）
         */
        private String batchName;
    }

    /**
     * 批量升级批次详情（用于列表查询）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpgradeBatchSummary {
        private String batchId;
        private String batchName;
        private String upgradeFilePath;
        private String sftpServerName;
        private Integer operationInterval;
        private Integer deviceCount;
        
        // 执行模式配置
        private String executionMode;
        private String scheduledMode;
        private Long scheduledTime;
        
        // 工作流配置
        private Boolean enableDownload;
        private Boolean enableBackup;
        private Boolean enableUpgrade;
        
        // 重试配置
        private Integer maxRetryCount;
        
        // 调试配置
        private Boolean debug;
        
        // 状态信息
        private String status;
        // ⚠️ 已删除 downloadStatus, backupStatus, upgradeStatus, commitStatus 字段
        
        // 统计信息
        private Integer successCount;
        private Integer failedCount;
        
        // 任务列表
        private List<DeviceTaskSummary> tasks;
        
        // 时间信息
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
        private LocalDateTime startedTime;
        private LocalDateTime completedTime;
    }

    /**
     * 设备任务摘要（用于批次列表和任务明细）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceTaskSummary {
        private String taskId;
        private String batchId;
        private String taskType;
        private String deviceId;
        private String deviceName;
        private String sftpServerName;
        private String filePath;
        private String status;
        private String errorMessage;
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
        private LocalDateTime startedTime;
        private LocalDateTime completedTime;
        private String currentVersion;
        private String previousVersion;
        private String targetVersion;
        private Integer retryCount;
        
        /**
         * RPC调试payload（仅在debug模式下返回）
         */
        private Object debugPayload;
    }

    /**
     * 步骤状态
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepStatus {
        /**
         * 步骤名称
         */
        private String stepName;

        /**
         * 步骤状态：PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
         */
        private String status;

        /**
         * 成功数量
         */
        private Integer successCount;

        /**
         * 失败数量
         */
        private Integer failedCount;

        /**
         * 待执行数量
         */
        private Integer pendingCount;

        /**
         * 执行中数量
         */
        private Integer runningCount;
    }

    /**
     * 设备任务详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceTaskDetail {
        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 设备ID
         */
        private String deviceId;

        /**
         * 设备名称（友好名称）
         */
        private String deviceName;

        /**
         * 任务类型：DOWNLOAD, BACKUP, UPGRADE
         */
        private String taskType;

        /**
         * 开始时间
         */
        private LocalDateTime startTime;

        /**
         * 任务状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
         */
        private String status;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * RPC Payload（debug=true时包含）
         */
        private Object debugPayload;
    }

    /**
     * 启动批量升级请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartBatchUpgradeRequest {
        /**
         * 批次ID
         */
        @NotBlank(message = "批次ID不能为空")
        private String batchId;
    }

    /**
     * 重试批量升级请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryBatchUpgradeRequest {
        /**
         * 批次ID
         */
        @NotBlank(message = "批次ID不能为空")
        private String batchId;

        /**
         * 要重试的设备ID列表（可选，为空时重试所有失败的设备）
         */
        private List<String> deviceIds;

        /**
         * 要重试的步骤（可选，为空时从失败的步骤开始）
         */
        private String fromStep;
    }

    /**
     * 升级确认请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitUpgradeRequest {
        /**
         * 批次ID
         */
        @NotBlank(message = "批次ID不能为空")
        private String batchId;

        /**
         * 确认描述
         */
        private String description;
    }

    /**
     * 升级回滚请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackUpgradeRequest {
        /**
         * 批次ID
         */
        @NotBlank(message = "批次ID不能为空")
        private String batchId;

        /**
         * 回滚描述
         */
        private String description;
    }

    /**
     * 批量操作结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOperationResult {
        /**
         * 成功数量
         */
        private int successCount;

        /**
         * 失败数量
         */
        private int failureCount;

        /**
         * 成功的批次ID列表
         */
        private List<String> successBatchIds;

        /**
         * 失败详情（批次ID -> 失败原因）
         */
        private Map<String, String> failureDetails;
    }

}
