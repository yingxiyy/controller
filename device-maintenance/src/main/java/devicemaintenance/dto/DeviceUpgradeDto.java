package devicemaintenance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 设备升级相关的数据传输对象
 */
public class DeviceUpgradeDto {

    /**
     * 设备升级请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpgradeRequest {
        /**
         * 设备ID
         */
        @NotBlank(message = "设备ID不能为空")
        private String deviceId;

        /**
         * 升级文件路径
         */
        @NotBlank(message = "升级文件路径不能为空")
        private String filePath;

        /**
         * 目标版本
         */
        @NotBlank(message = "目标版本不能为空")
        private String targetVersion;

        /**
         * SFTP服务器ID（可选，用于从SFTP服务器获取升级文件）
         */
        private String sftpServerName;

        /**
         * 计划执行时间戳（可选，为空则立即执行，单位：毫秒）
         */
        private Long scheduledTime;

        /**
         * 升级描述
         */
        private String description;
    }

    /**
     * 设备升级响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpgradeResponse {
        /**
         * 操作是否成功
         */
        private boolean success;

        /**
         * 响应消息
         */
        private String message;

        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 设备ID
         */
        private String deviceId;

        /**
         * 升级文件路径
         */
        private String filePath;

        /**
         * 当前版本
         */
        private String currentVersion;

        /**
         * 目标版本
         */
        private String targetVersion;

        /**
         * 任务状态
         */
        private String status;

        /**
         * 升级状态
         */
        private String upgradeStatus;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 创建时间
         */
        private LocalDateTime createdTime;

        /**
         * 更新时间
         */
        private LocalDateTime updatedTime;

        /**
         * 完成时间
         */
        private LocalDateTime completedTime;
    }

    /**
     * 升级进度通知
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpgradeProgressNotification {
        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 设备ID
         */
        private String deviceId;

        /**
         * 任务状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
         */
        private String status;

        /**
         * 升级状态：NOT_STARTED, DOWNLOADING_FILE, BACKUP_DEVICE, VALIDATING_FILE, 
         * INSTALLING_SOFTWARE, RESTARTING_DEVICE, SUCCESS, FAILED
         */
        private String upgradeStatus;

        /**
         * 当前步骤描述
         */
        private String currentStep;

        /**
         * 进度消息
         */
        private String message;

        /**
         * 升级文件路径
         */
        private String filePath;

        /**
         * 当前版本
         */
        private String currentVersion;

        /**
         * 目标版本
         */
        private String targetVersion;

        /**
         * 错误详情
         */
        private String errorDetails;

        /**
         * 是否需要重启
         */
        private Boolean needRestart;

        /**
         * 重启状态
         */
        private String restartStatus;

        /**
         * 备份文件路径（如果进行了备份）
         */
        private String backupFilePath;

        /**
         * 通知时间戳
         */
        private LocalDateTime timestamp;

        public UpgradeProgressNotification(String taskId, String deviceId, String status, 
                String upgradeStatus, String message) {
            this.taskId = taskId;
            this.deviceId = deviceId;
            this.status = status;
            this.upgradeStatus = upgradeStatus;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * 版本兼容性检查结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionCompatibilityResult {
        /**
         * 是否兼容
         */
        private boolean compatible;

        /**
         * 当前版本
         */
        private String currentVersion;

        /**
         * 目标版本
         */
        private String targetVersion;

        /**
         * 兼容性检查消息
         */
        private String compatibilityMessage;

        /**
         * 是否需要强制升级
         */
        private boolean requiresForceUpgrade;

        /**
         * 警告信息
         */
        private String warnings;

        /**
         * 升级前置条件
         */
        private String prerequisites;
    }

    /**
     * 升级文件验证结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpgradeFileValidationResult {
        /**
         * 文件是否有效
         */
        private boolean valid;

        /**
         * 文件信息
         */
        private String fileInfo;

        /**
         * 文件大小
         */
        private Long fileSize;

        /**
         * 文件版本
         */
        private String fileVersion;

        /**
         * 设备兼容性检查结果
         */
        private boolean deviceCompatible;

        /**
         * 验证消息
         */
        private String validationMessage;

        /**
         * 文件校验和
         */
        private String checksum;
    }

    /**
     * 批量升级请求
     * 说明：批量升级是指多个设备升级到同一版本，使用同一个SFTP
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchUpgradeRequest {
        @NotBlank(message = "批次名称不能为空")
        private String batchName;
        
        // ⭐ 工作流内部调用字段：用于关联现有Batch和Workflow（外部调用时为null）
        private String batchId;      // 现有批次ID（工作流内部调用时使用）
        private java.util.Map<String, String> workflowIds;   // deviceId -> workflowId 映射（工作流内部调用时使用）

        @NotNull(message = "设备列表不能为空")
        private java.util.List<String> deviceIds;

        @NotBlank(message = "升级文件路径不能为空")
        private String upgradeFilePath;

        // ⭐ targetVersion 是可选的业务信息字段（用于记录和显示），不是 RPC 必要参数
        private String targetVersion;

        @NotBlank(message = "SFTP服务器名称不能为空")
        private String sftpServerName;

        private String remark;
        
        // ⭐ 新增：定时执行时间戳（可选，null表示立即执行，单位：毫秒）
        private Long scheduledTime;
        
        // ⭐ 新增：Debug模式（保存RPC payload到DeviceTask）
        private Boolean debug;
    }

    /**
     * 批量升级响应
     * 说明：批量升级不使用batch管理，只是并发创建多个独立任务，因此不返回batchId
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchUpgradeResponse {
        // ✅ 不包含batchId，因为批量升级不是batch概念
        private String batchName;
        private String upgradeFilePath;
        private String targetVersion;
        private String sftpServerName;
        private Integer totalDevices;
        private Integer successCount;
        private Integer failedCount;
        private String status;
        private LocalDateTime createdTime;
        private java.util.List<DeviceUpgradeResult> deviceResults;
    }

    /**
     * 设备升级结果（设备名 + RPC返回结果）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceUpgradeResult {
        private String deviceId;
        private String deviceName;
        private String taskId;
        private String status;
        private String rpcResult;
        private String errorMessage;
        private LocalDateTime createdTime;

        /**
         * 格式化输出（可读性增强）
         */
        public String toReadableString() {
            return String.format("[%s] 升级 → %s - 状态: %s %s",
                deviceName != null ? deviceName : deviceId,
                taskId != null ? taskId : "N/A",
                status,
                errorMessage != null ? "错误: " + errorMessage : "");
        }
    }
}
