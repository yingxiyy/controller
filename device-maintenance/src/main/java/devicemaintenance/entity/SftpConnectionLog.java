package devicemaintenance.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * SFTP连接日志实体类
 */
@Entity
@Table(name = "dm_sftp_connection_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SftpConnectionLog {

    @Id
    @Column(name = "log_id", length = 64)
    private String logId;

    @Column(name = "server_id", length = 64, nullable = false)
    private String serverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 50, nullable = false)
    private OperationType operationType;

    @Column(name = "operation_path", length = 1000)
    private String operationPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_status", length = 20, nullable = false)
    private OperationStatus operationStatus;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "operation_result", columnDefinition = "TEXT")
    private String operationResult;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        if (logId == null) {
            logId = "log_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        }
        createdTime = LocalDateTime.now();
    }

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        CONNECTION_TEST,    // 连接测试
        LIST_DIRECTORY,     // 目录浏览
        UPLOAD,             // 上传
        DOWNLOAD            // 下载
    }

    /**
     * 操作状态枚举
     */
    public enum OperationStatus {
        SUCCESS,    // 成功
        FAILED      // 失败
    }
}
