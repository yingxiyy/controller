package devicemaintenance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 软件回滚 DTO
 */
public class SoftwareRollbackDto {

    /**
     * 软件回滚请求（单设备）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackRequest {
        /**
         * 设备ID
         */
        @NotBlank(message = "设备ID不能为空")
        private String deviceId;

        /**
         * SFTP服务器名称（必需）
         */
        @NotBlank(message = "SFTP服务器名称不能为空")
        private String sftpServerName;

        /**
         * 软件包文件名（必需）
         */
        @NotBlank(message = "软件包文件名不能为空")
        private String fileName;

        /**
         * 计划执行时间戳（可选，为空则立即执行，单位：毫秒）
         */
        private Long scheduledTime;

        /**
         * 回滚描述
         */
        private String description;
    }

    /**
     * 软件回滚响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackResponse {
        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 设备ID
         */
        private String deviceId;

        /**
         * SFTP服务器名称
         */
        private String sftpServerName;

        /**
         * 软件包文件名
         */
        private String fileName;

        /**
         * 任务状态
         */
        private String status;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 创建时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdTime;

        /**
         * 更新时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedTime;

        /**
         * 完成时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime completedTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryRollbackRequest {
        @NotBlank(message = "任务ID不能为空")
        private String taskId;
    }

    /**
     * 批量回滚请求
     * 
     * 注意：每个设备对应一个回滚文件
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRollbackRequest {
        /**
         * 批次名称
         */
        @NotBlank(message = "批次名称不能为空")
        private String batchName;

        /**
         * 设备ID列表
         */
        @NotEmpty(message = "设备ID列表不能为空")
        private List<String> deviceIds;

        /**
         * 回滚文件名列表（与设备ID一一对应）
         * 
         * 例如：
         * deviceIds = ["Site-1#Ne-1", "Site-2#Ne-2"]
         * fileNames = ["Release_v1.0.tar", "Release_v2.0.tar"]
         */
        @NotEmpty(message = "回滚文件名列表不能为空")
        private List<String> fileNames;

        /**
         * SFTP服务器名称（所有设备使用同一SFTP）
         */
        @NotBlank(message = "SFTP服务器名称不能为空")
        private String sftpServerName;

        /**
         * 计划执行时间戳（可选，为空则立即执行，单位：毫秒）
         */
        private Long scheduledTime;

        /**
         * 备注
         */
        private String remark;
    }

    /**
     * 批量回滚响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRollbackSummary {
        private String batchId;
        private String batchName;
        private String sftpServerName;
        private Integer totalDevices;
        private Integer scheduledDevices;
        private Integer runningDevices;
        private Integer successDevices;
        private Integer failedDevices;
        private String status;
        private String remark;
        private Long scheduledTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedTime;
        
        private List<DeviceRollbackResult> deviceResults;
    }

    /**
     * 单个设备的回滚结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceRollbackResult {
        private String deviceId;
        private String deviceName;
        private String taskId;
        private String fileName;
        private String status;
        private String rpcResult;
        private String errorMessage;
        private Long scheduledTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdTime;
    }
}

