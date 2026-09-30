package devicemaintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备备份相关DTO
 */
public class DeviceBackupDto {

    /**
     * 单个设备备份请求（内部使用）
     * 注意：此DTO仅供内部流程使用，外部API已统一使用 BatchBackupRequest
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleDeviceBackupRequest {
        private String deviceId;
        private String filePath;  // 完整文件路径（如 /dbbackup/device_name/20251020/172800.db）
        private String basePath;  // 备份基础路径（可选，如果提供则自动生成 filePath）
        private String sftpServerName;
        private Long scheduledTime;  // 计划执行时间戳（可选，为空则立即执行，单位：毫秒）
    }

    /**
     * 批量备份请求（指定设备列表）
     * 
     * 使用说明：
     * - 单设备备份：在deviceIds中只传入一个设备ID即可
     * - 多设备备份：在deviceIds中传入多个设备ID
     * 
     * 新规则：
     * - basePath: 只需指定根路径，如 "/dbbackup"
     * - 自动生成完整路径：basePath/device_name/yyyyMMdd/HHmmss.db
     *   - device_name: 设备友好名称（非法字符替换为 -）
     *   - yyyyMMdd: 当前日期（如 20251020）
     *   - HHmmss.db: 时间戳文件名（如 172800.db）
     * - 会自动调用SFTP mkdir创建两层目录（设备名/日期）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchBackupRequest {
        // 批次名称（可选）：如果提供，则创建Batch对象并发送TaskInfo通知；如果为空，则只创建DeviceTask，不创建Batch
        private String batchName;
        
        // ⭐ 工作流内部调用字段：用于关联现有Batch和Workflow（外部调用时为null）
        private String batchId;      // 现有批次ID（工作流内部调用时使用）
        private java.util.Map<String, String> workflowIds;   // deviceId -> workflowId 映射（工作流内部调用时使用）
        
        @NotNull(message = "Device IDs list cannot be null")
        private java.util.List<String> deviceIds;
        
        @JsonProperty("basePath")
        @NotNull(message = "Backup base path cannot be null")
        private String basePath;  // 备份基础目录，如 "/dbbackup"
        
        // 兼容旧的字段名
        @JsonProperty("rootPath")
        public void setRootPath(String rootPath) {
            this.basePath = rootPath;
        }
        
        @JsonProperty("filePath")
        public void setFilePath(String filePath) {
            this.basePath = filePath;
        }
        
        private String sftpServerName;  // 所有设备使用同一SFTP
        private String remark;
        private Long scheduledTime;  // 定时执行时间戳（可选），为空则立即执行，单位：毫秒
        private Boolean debug = false;  // 调试模式：是否在API响应中返回debugPayload
    }

    /**
     * 备份响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackupResponse {
        private String taskId;
        private String batchId; // 批量备份时才有值
        private String deviceId;
        private String deviceName;
        private String taskType;
        private String status;
        private String backupPath;
        private String backupFileName;
        private String sftpServerName;
        private String errorMessage;
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
        private LocalDateTime startedTime;
        private LocalDateTime completedTime;
    }

    /**
     * 备份进度通知
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackupProgressNotification {
        private String taskId;
        private String batchId;
        private String deviceId;
        private String deviceName;
        private String backupPath;
        private String backupFileName;
        private String status;
        private String message;
        private String errorDetails;
        private LocalDateTime timestamp;
    }

    /**
     * 批量备份响应汇总
     * 说明：批量备份不使用batch管理，只是并发创建多个独立任务，因此不返回batchId
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchBackupSummary {
        private String batchId;
        private String batchName;
        private String basePath;  // 备份基础路径
        private String sftpServerName;
        private Integer totalDevices;
        private Integer scheduledDevices;  // 已调度设备数
        private Integer runningDevices;    // 运行中设备数
        private Integer successDevices;    // 成功设备数
        private Integer failedDevices;     // 失败设备数
        private Integer pendingCount;      // 待执行设备数（已废弃，保留兼容性）
        private Integer runningCount;      // 运行中设备数（已废弃，保留兼容性）
        private Integer completedCount;    // 已完成设备数（已废弃，保留兼容性）
        private Integer failedCount;       // 失败设备数（已废弃，保留兼容性）
        private String status;
        private String remark;
        private Long scheduledTime;  // 定时执行时间戳（可选，单位：毫秒）
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
        private List<DeviceBackupResult> deviceResults;
    }

    /**
     * 设备备份结果（设备名 + RPC返回结果）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceBackupResult {
        private String deviceId;
        private String deviceName;
        private String taskId;
        private String backupFileName;  // 自动生成的备份文件名
        private String status;
        private String rpcResult;  // RPC返回的详细结果
        private String errorMessage;
        private Long scheduledTime;  // 定时执行时间戳（可选，单位：毫秒）
        private LocalDateTime createdTime;
        
        /**
         * 格式化输出（可读性增强）
         */
        public String toReadableString() {
            return String.format("[%s] 备份文件: %s - 状态: %s %s", 
                deviceName != null ? deviceName : deviceId,
                backupFileName != null ? backupFileName : "未生成",
                status,
                errorMessage != null ? "错误: " + errorMessage : "");
        }
    }
}
