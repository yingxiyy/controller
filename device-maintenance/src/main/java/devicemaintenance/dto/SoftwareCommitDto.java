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
 * 软件提交 DTO
 */
public class SoftwareCommitDto {

    /**
     * 批量软件提交请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchCommitRequest {
        /**
         * 设备ID列表
         */
        @NotEmpty(message = "设备ID列表不能为空")
        private List<String> deviceIds;

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
         * 提交描述
         */
        private String description;

        /**
         * 批次ID（可选，用于 workflow 内部调用）
         */
        private String batchId;

        /**
         * 工作流ID映射（可选，用于 workflow 内部调用）
         * Key: deviceId, Value: workflowId
         */
        private java.util.Map<String, String> workflowIds;
    }

    /**
     * 单个设备提交结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceCommitResult {
        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 设备ID
         */
        private String deviceId;

        /**
         * 设备名称
         */
        private String deviceName;

        /**
         * 设备IP
         */
        private String deviceIp;

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
    }

    /**
     * 批量软件提交响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchCommitResponse {
        /**
         * SFTP服务器名称
         */
        private String sftpServerName;

        /**
         * 软件包文件名
         */
        private String fileName;

        /**
         * 总设备数
         */
        private int totalCount;

        /**
         * 成功数
         */
        private int successCount;

        /**
         * 失败数
         */
        private int failedCount;

        /**
         * 每个设备的提交结果
         */
        private List<DeviceCommitResult> results;
    }
}

