package devicemaintenance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备软件下载相关DTO类
 */
public class SoftwareDownloadDto {

    /**
     * 软件下载请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadRequest {
        @NotBlank(message = "Device ID cannot be blank")
        @Size(max = 100, message = "Device ID must not exceed 100 characters")
        private String deviceId;

        @NotBlank(message = "Software file path cannot be blank")
        @Size(max = 500, message = "Software file path must not exceed 500 characters")
        private String filePath;

        @Size(max = 100, message = "SFTP server name must not exceed 100 characters")
        @JsonProperty(value = "sftpServerName", access = JsonProperty.Access.WRITE_ONLY)
        private String sftpServerName;  // 兼容sftpServerId和sftpServerName两个字段名
        
        // 提供sftpServerId的setter，实际设置到sftpServerName
        @JsonProperty("sftpServerId")
        public void setSftpServerId(String sftpServerId) {
            this.sftpServerName = sftpServerId;
        }

        @Size(max = 255, message = "Device IP must not exceed 255 characters")
        private String deviceIp;

        @Size(max = 50, message = "Software version must not exceed 50 characters")
        private String version;

        private Long scheduledTime;  // 计划执行时间戳（可选，为空则立即执行，单位：毫秒）

        @Size(max = 1000, message = "Remark must not exceed 1000 characters")
        private String remark;
        
        private Boolean debug = false;  // 调试模式：是否保存debugPayload到数据库
    }

    /**
     * 软件下载响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadResponse {
        private String taskId;
        private String deviceId;
        private String filePath;
        private String sftpServerName;
        private String status;
        private String errorMessage;
        private LocalDateTime createdTime;
        private LocalDateTime updatedTime;
        
        /** 调试信息：发送给neMgr的完整payload（仅当debug=true时包含） */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, Object> debugPayload;
    }

    /**
     * 下载任务状态更新请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatusUpdate {
        @NotBlank(message = "Task ID cannot be blank")
        private String taskId;

        @NotBlank(message = "Task status cannot be blank")
        private String status;

        private String errorMessage;
        private String resultMessage;
    }

    /**
     * 下载进度通知
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadProgressNotification {
        private String taskId;
        private String deviceId;
        private String filePath;
        private String status;
        private String message;
        private String errorDetails;
        private LocalDateTime timestamp;
    }

    /**
     * 批量软件下载请求
     * 说明：批量下载是指不同设备下载同一个文件，使用同一个SFTP
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchDownloadRequest {
        // 批次名称（可选）：如果提供，则创建Batch对象并发送TaskInfo通知；如果为空，则只创建DeviceTask，不创建Batch
        private String batchName;
        
        // ⭐ 工作流内部调用字段：用于关联现有Batch和Workflow（外部调用时为null）
        private String batchId;                              // 现有批次ID（工作流内部调用时使用）
        private java.util.Map<String, String> workflowIds;   // deviceId -> workflowId 映射（工作流内部调用时使用）

        @NotEmpty(message = "Device list cannot be empty")
        private java.util.List<String> deviceIds;

        @NotBlank(message = "Software package cannot be blank (all devices download the same file)")
        private String softwarePackage;

        @NotBlank(message = "SFTP server ID cannot be blank (all devices use the same SFTP)")
        @Size(max = 100, message = "SFTP server ID must not exceed 100 characters")
        private String sftpServerId;

        private String remark;
        
        // ⭐ 新增：定时执行时间戳（可选，null表示立即执行，单位：毫秒）
        private Long scheduledTime;
        
        // ⭐ 新增：Debug模式（保存RPC payload到DeviceTask）
        private Boolean debug;
    }

    /**
     * 批量下载响应
     * 说明：批量下载不使用batch管理，只是并发创建多个独立任务，因此不返回batchId
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchDownloadResponse {
        // ✅ 不再包含batchId，因为批量下载不是batch概念
        private String batchName;
        private String softwarePackage;
        private String sftpServerId;
        private Integer totalDevices;
        private Integer successCount;
        private Integer failedCount;
        private String status;
        private LocalDateTime createdTime;
        private java.util.List<DeviceDownloadResult> deviceResults;
    }

    /**
     * 设备下载结果（设备名 + RPC返回结果）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceDownloadResult {
        private String deviceId;
        private String deviceName;
        private String taskId;
        private String status;
        private String rpcResult;  // RPC返回的详细结果
        private String errorMessage;
        private LocalDateTime createdTime;
        
        /**
         * 格式化输出（可读性增强）
         */
        public String toReadableString() {
            return String.format("[%s] %s - 状态: %s %s", 
                deviceName != null ? deviceName : deviceId,
                taskId != null ? "任务ID: " + taskId : "创建失败",
                status,
                errorMessage != null ? "错误: " + errorMessage : "");
        }
    }
}
