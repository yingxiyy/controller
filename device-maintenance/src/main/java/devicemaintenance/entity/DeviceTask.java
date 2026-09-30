package devicemaintenance.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 设备任务实体（统一管理下载、备份、恢复、升级等通用任务）
 */
@Entity
@Table(name = "dm_device_task")
@Data
@NoArgsConstructor
public class DeviceTask {

    @Id
    @Column(name = "task_id", length = 64)
    private String taskId;

    @Column(name = "batch_id", length = 64)
    private String batchId;

    @Column(name = "batch_name", length = 200)
    private String batchName;  // 批次名称，用于显示和TaskInfo绑定

    @Column(name = "workflow_id", length = 64)
    private String workflowId;  // 工作流ID，用于关联升级工作流

    @Column(name = "task_type", length = 20)
    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    @Column(name = "device_id", length = 64, nullable = false)
    private String deviceId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "device_ip", length = 50)
    private String deviceIp;  // 设备IP地址

    @Column(name = "vendor_type", length = 50)
    private String vendorType;  // 设备厂商类型（如 "CHASSIS", "DC908"）

    @Column(name = "vendor_name", length = 50)
    private String vendorName;  // 设备厂商名称（如 "COHERENT", "HUAWEI"）

    @Column(name = "sftp_server_id", length = 100)
    private String sftpServerName;  // 映射到数据库的sftp_server_id列，但存储的是SFTP服务器的name值（如"192.168.3.206:22"）

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "backup_file_path", length = 500)
    private String backupFilePath;

    @Column(name = "backup_file_name", length = 200)
    private String backupFileName;

    @Column(name = "current_version", length = 100)
    private String currentVersion;

    @Column(name = "previous_version", length = 100)
    private String previousVersion;

    @Column(name = "target_version", length = 100)
    private String targetVersion;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "debug_payload", columnDefinition = "TEXT")
    private String debugPayload;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(name = "scheduled_time")
    private Long scheduledTime;  // 定时执行时间戳（单位：毫秒）

    @Column(name = "started_time")
    private LocalDateTime startedTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    /**
     * TaskInfo 通知使用的 actionTime（毫秒时间戳）
     * 用于删除通知时能够精确匹配原任务
     */
    @Column(name = "taskinfo_action_time")
    private Long taskInfoActionTime;

    public DeviceTask(String taskId, String deviceId, TaskType taskType) {
        this.taskId = taskId;
        this.deviceId = deviceId;
        this.taskType = taskType;
        this.status = TaskStatus.PENDING;
    }

    public void markStatus(TaskStatus status) {
        this.status = status;
        this.updatedTime = LocalDateTime.now();
    }

    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedTime = LocalDateTime.now();
    }

    public enum TaskType {
        DOWNLOAD, BACKUP, RESTORE, UPGRADE, ROLLBACK, COMMIT
    }

    public enum TaskStatus {
        IDLE,       // 空闲 - 设备当前无维护任务，可接受新任务
        NOT_START,  // 未开始 - MANUAL模式下预创建的任务，等待手动触发
        SCHEDULED,  // 计划中 - 任务已创建，计划在之后执行，但现在还未开始
        PENDING,    // 待执行 - 任务已准备好，等待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        CANCELLED,  // 已取消
        EXPIRED     // 已过期 - 任务超时未执行
    }
}
