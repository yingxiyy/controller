package devicemaintenance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 设备恢复相关的数据传输对象
 */
public class DeviceRestoreDto {

    /**
     * 批量设备恢复请求
     * 
     * 使用说明：
     * - 单设备恢复：在deviceIds中只传入一个设备ID即可
     * - 多设备恢复：在deviceIds中传入多个设备ID
     * 
     * 自动查找最新备份规则：
     * - basePath: 备份根路径，如 "/dbbackup"
     * - dateDir: 日期目录（yyyyMMdd格式），如 "20251027"
     * - 自动查找路径：basePath/device_name/dateDir/ 下时间戳最新的 .db 文件
     * - 示例：/dbbackup/升级测试/20251027/015421.db
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRestoreRequest {
        /**
         * 批次名称（可选）：如果提供，则创建Batch对象并发送TaskInfo通知；如果为空，则只创建DeviceTask，不创建Batch
         */
        private String batchName;
        
        /**
         * 设备ID列表
         */
        @NotNull(message = "Device IDs list cannot be null")
        private java.util.List<String> deviceIds;
        
        /**
         * 备份基础路径（用于自动查找最新备份）
         * 示例："/dbbackup"
         */
        private String basePath;
        
        /**
         * 日期目录（yyyyMMdd格式）
         * 用于在 basePath/device_name/dateDir/ 下查找最新备份
         * 示例："20251027"
         */
        private String dateDir;
        
        /**
         * SFTP服务器名称（所有设备使用同一SFTP）
         */
        private String sftpServerName;
        
        /**
         * 备注
         */
        private String remark;
        
        /**
         * 定时执行时间戳（可选），为空则立即执行，单位：毫秒
         */
        private Long scheduledTime;
        
        /**
         * 调试模式：是否在API响应和DeviceTask中保存debugPayload
         */
        private Boolean debug = false;
    }

    /**
     * 单设备恢复请求（旧版，保留兼容）
     * @deprecated 请使用 BatchRestoreRequest，单设备传1个元素的数组
     */
    @Deprecated
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoreRequest {
        /**
         * 批次名称（可选，不提供则自动生成"恢复-设备名-时间戳"）
         */
        private String batchName;

        /**
         * 设备ID
         */
        @NotBlank(message = "Device ID cannot be blank")
        private String deviceId;

        /**
         * 备份文件路径（完整路径）
         * 可选：如果不提供，则使用 basePath 自动查找最新备份
         * 注意：filePath 和 basePath 至少提供一个
         */
        private String filePath;

        /**
         * 备份基础路径（用于自动查找最新备份）
         * 当 filePath 为空时必填
         * 格式：basePath/device_name/yyyyMMdd/yyyyMMdd_HHmmss.db
         */
        private String basePath;

        /**
         * SFTP服务器ID（可选，用于从SFTP服务器获取备份文件）
         */
        private String sftpServerName;

        /**
         * 计划执行时间戳（可选，为空则立即执行，单位：毫秒）
         */
        private Long scheduledTime;

        /**
         * 恢复描述
         */
        private String description;

        /**
         * 调试模式：是否在API响应中返回debugPayload
         */
        private Boolean debug = false;
    }

    /**
     * 设备恢复响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoreResponse {
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
         * 备份文件路径
         */
        private String backupFilePath;

        /**
         * 任务状态
         */
        private String status;

        /**
         * 恢复状态
         */
        private String restoreStatus;

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
     * 恢复进度通知
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoreProgressNotification {
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
         * 恢复状态：NOT_STARTED, DOWNLOADING_BACKUP, VALIDATING_BACKUP, RESTORING_DATA, 
         * RESTORING_CONFIG, RESTARTING_DEVICE, COMPLETED, FAILED
         */
        private String restoreStatus;

        /**
         * 当前步骤描述
         */
        private String currentStep;

        /**
         * 进度消息
         */
        private String message;

        /**
         * 备份文件路径
         */
        private String backupFilePath;

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
         * 通知时间戳
         */
        private LocalDateTime timestamp;

        public RestoreProgressNotification(String taskId, String deviceId, String status, 
                String restoreStatus, String message) {
            this.taskId = taskId;
            this.deviceId = deviceId;
            this.status = status;
            this.restoreStatus = restoreStatus;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * 恢复验证结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoreValidationResult {
        /**
         * 验证是否通过
         */
        private boolean valid;

        /**
         * 备份文件信息
         */
        private String backupFileInfo;

        /**
         * 设备兼容性检查结果
         */
        private boolean deviceCompatible;

        /**
         * 版本兼容性检查结果
         */
        private boolean versionCompatible;

        /**
         * 验证消息
         */
        private String validationMessage;

        /**
         * 警告信息
         */
        private String warnings;
    }

    /**
     * 批量恢复汇总响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRestoreSummary {
        private String batchId;
        private String batchName;
        private String basePath;
        private String dateDir;
        private String sftpServerName;
        private Integer totalDevices;
        private Integer scheduledDevices;  // 已调度设备数
        private Integer runningDevices;    // 运行中设备数
        private Integer successDevices;    // 成功设备数
        private Integer failedDevices;     // 失败设备数
        private String status;
        private String remark;
        private Long scheduledTime;  // 定时执行时间戳（可选，单位：毫秒）
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
        private java.util.List<DeviceRestoreResult> deviceResults;
    }

    /**
     * 设备恢复结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceRestoreResult {
        private String deviceId;
        private String deviceName;
        private String backupFilePath;  // 自动找到的备份文件路径
        private String status;
        private String message;  // 统一使用 message 字段（成功/失败信息都用这个）
    }
}
