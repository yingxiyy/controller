package devicemaintenance.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 统一批次实体
 * 
 * 用于管理所有类型的批处理操作：
 * - BACKUP: 备份批次
 * - RESTORE: 恢复批次
 * - UPGRADE: 升级批次（关联 UpgradeWorkflow）
 * 
 * 设计原则：
 * - 通用字段对所有类型适用
 * - 类型特定字段（如 base_path, file_path）对不适用的类型可为 NULL
 * - Workflow 通过独立的 UpgradeWorkflow 表关联（仅 UPGRADE 类型）
 */
@Entity
@Table(name = "dm_batch")
@Data
@NoArgsConstructor
public class Batch {

    // ========== 通用字段 ==========
    
    @Id
    @Column(name = "batch_id", length = 64)
    private String batchId;

    @Column(name = "batch_name", length = 200, nullable = false)
    private String batchName;

    @Column(name = "batch_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private BatchType batchType;

    @Column(name = "device_count", nullable = false)
    private Integer deviceCount = 0;

    @Column(name = "status", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.READY_TO_PROCEED;  // ⭐ 默认：准备执行

    @Column(name = "status_detail", length = 500)
    private String statusDetail;

    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    @Column(name = "running_count", nullable = false)
    private Integer runningCount = 0;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(name = "started_time")
    private LocalDateTime startedTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    @Column(name = "batch_action_time")
    private Long batchActionTime;

    @Column(name = "scheduled_time")
    private Long scheduledTime;  // 定时执行时间戳（单位：毫秒）

    @Column(name = "scheduled_mode", length = 20)
    @Enumerated(EnumType.STRING)
    private ScheduledMode scheduledMode = ScheduledMode.IMMEDIATE;

    // ✅ 每日定时任务字段（主要用于 BACKUP 类型批次）
    @Column(name = "enable_daily_task", nullable = false)
    private Boolean enableDailyTask = false;  // 是否启用每日循环执行

    @Column(name = "daily_execution_time", length = 10)
    private String dailyExecutionTime;  // 每日执行时间 (HH:mm格式，如 02:00)

    @Column(name = "sftp_server_name", length = 100)
    private String sftpServerName;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "debug", nullable = false)
    private Boolean debug = false;

    // ========== 备份/恢复专用字段 ==========
    
    /**
     * 备份/恢复基础路径
     * - BACKUP: 备份文件存储路径
     * - RESTORE: 恢复文件查找路径
     * - UPGRADE: NULL
     */
    @Column(name = "base_path", length = 500)
    private String basePath;

    // ========== 升级专用字段 ==========
    
    /**
     * 升级包文件路径
     * - UPGRADE: 升级包路径
     * - BACKUP/RESTORE: NULL
     */
    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "target_version", length = 100)
    private String targetVersion;

    /**
     * 升级时备份基础路径
     * - UPGRADE: 升级前备份的存储路径（如果启用备份）
     * - BACKUP/RESTORE: NULL
     */
    @Column(name = "backup_base_path", length = 500)
    private String backupBasePath;

    /**
     * 执行模式
     * - MANUAL: 手动模式（每步需手动触发）
     * - AUTOMATIC: 自动模式（步骤自动进行）
     * - 仅 UPGRADE 类型使用，BACKUP/RESTORE: NULL
     */
    @Column(name = "execution_mode", length = 20)
    @Enumerated(EnumType.STRING)
    private ExecutionMode executionMode;

    /**
     * 操作间隔（秒）
     * - 仅 UPGRADE 类型使用
     */
    @Column(name = "operation_interval")
    private Integer operationInterval = 1;

    /**
     * 最大重试次数
     * - 仅 UPGRADE 类型使用
     */
    @Column(name = "max_retry_count")
    private Integer maxRetryCount = 3;

    // Workflow 步骤配置（仅 UPGRADE 类型使用）
    @Column(name = "enable_download")
    private Boolean enableDownload = true;

    @Column(name = "enable_backup")
    private Boolean enableBackup = true;

    @Column(name = "enable_upgrade")
    private Boolean enableUpgrade = true;

    // ========== 枚举定义 ==========

    /**
     * 批次类型
     */
    public enum BatchType {
        DOWNLOAD,     // 下载批次
        BACKUP,       // 备份批次
        RESTORE,      // 恢复批次
        UPGRADE,      // 升级批次（关联 UpgradeWorkflow）
        ROLLBACK      // 回滚批次
    }

    /**
     * 批次状态
     * 
     * 初始状态:
     * - SCHEDULED: 定时任务，等待时间到达
     * - READY_TO_PROCEED: 手动模式，等待手动触发第一步（下载）
     * 
     * 执行中状态:
     * - 手动模式细分状态: DOWNLOADING, READY_TO_BACKUP, BACKING_UP, READY_TO_UPGRADE, UPGRADING, READY_TO_COMMIT, COMMITTING
     * - 自动模式状态: RUNNING（不细分步骤）
     * 
     * 终态:
     * - COMPLETED: 全部成功
     * - COMPLETED_WITH_ERRORS: 部分失败（有成功+有失败，全部已结束）
     * - FAILED: 全部失败
     * - CANCELLED: 已取消
     */
    public enum BatchStatus {
        // 初始状态
        SCHEDULED,             // 已调度（定时任务）
        READY_TO_PROCEED,      // 准备执行（MANUAL 模式创建后，等待手动触发第一步）
        
        // 手动模式执行中状态（细分）
        DOWNLOADING,           // 正在下载（手动模式）
        READY_TO_BACKUP,       // 准备备份（下载完成，手动模式）
        BACKING_UP,            // 正在备份（手动模式）
        READY_TO_UPGRADE,      // 准备升级（备份完成，手动模式）
        UPGRADING,             // 正在升级（手动模式）
        READY_TO_COMMIT,       // 准备提交（升级完成，手动模式）
        COMMITTING,            // 正在提交（手动模式）
        
        // 自动模式执行中状态
        RUNNING,               // 执行中（自动模式）
        
        // 终态
        COMPLETED,             // 已完成（全部成功）
        COMPLETED_WITH_ERRORS, // 已完成但有错误（部分失败）
        FAILED,                // 失败（全部失败）
        CANCELLED              // 已取消
    }

    /**
     * 调度模式
     */
    public enum ScheduledMode {
        IMMEDIATE,    // 立即执行
        SCHEDULED     // 定时执行
    }

    /**
     * 执行模式（仅升级批次）
     */
    public enum ExecutionMode {
        MANUAL,       // 手动模式
        AUTOMATIC     // 自动模式
    }

    // ========== 构造函数 ==========

    public Batch(String batchId, String batchName, BatchType batchType, 
                 Integer deviceCount, Long batchActionTime) {
        this.batchId = batchId;
        this.batchName = batchName;
        this.batchType = batchType;
        this.deviceCount = deviceCount;
        this.batchActionTime = batchActionTime;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    // ========== JPA 生命周期回调 ==========

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
        updatedTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    // ========== 业务方法 ==========

    /**
     * 更新批次状态
     */
    public void updateBatchStatus(BatchStatus status) {
        this.status = status;
        this.updatedTime = LocalDateTime.now();
        
        if (status == BatchStatus.RUNNING && startedTime == null) {
            this.startedTime = LocalDateTime.now();
        } else if (isTerminalStatus(status) && completedTime == null) {
            this.completedTime = LocalDateTime.now();
        }
    }

    /**
     * 更新批次状态和详情
     */
    public void updateBatchStatus(BatchStatus status, String statusDetail) {
        this.status = status;
        this.statusDetail = statusDetail;
        this.updatedTime = LocalDateTime.now();
        
        if (status == BatchStatus.RUNNING && startedTime == null) {
            this.startedTime = LocalDateTime.now();
        } else if (isTerminalStatus(status) && completedTime == null) {
            this.completedTime = LocalDateTime.now();
        }
    }

    /**
     * 更新统计数量
     */
    public void updateCounts(Integer successCount, Integer failedCount, Integer runningCount) {
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.runningCount = runningCount;
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 更新详情JSON
     */
    public void updateDetail(String detail) {
        this.detail = detail;
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 设置错误信息并标记为失败
     */
    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = BatchStatus.FAILED;
        this.statusDetail = "Batch execution failed: " + errorMessage;
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 检查批次是否处于终态
     */
    public static boolean isTerminalStatus(BatchStatus status) {
        return status == BatchStatus.COMPLETED 
            || status == BatchStatus.COMPLETED_WITH_ERRORS
            || status == BatchStatus.FAILED 
            || status == BatchStatus.CANCELLED;
    }

    /**
     * 检查是否为升级批次
     */
    public boolean isUpgradeBatch() {
        return this.batchType == BatchType.UPGRADE;
    }

    /**
     * 检查是否为备份批次
     */
    public boolean isBackupBatch() {
        return this.batchType == BatchType.BACKUP;
    }

    /**
     * 检查是否为恢复批次
     */
    public boolean isRestoreBatch() {
        return this.batchType == BatchType.RESTORE;
    }

    /**
     * 自定义 getter - 覆盖 Lombok 生成的，用于调试
     * ⚠️ 临时调试代码，排查 scheduledTime 为何为空
     */
    public Long getScheduledTime() {
        System.out.println("🔍 DEBUG: Batch.getScheduledTime() called, batchId=" + this.batchId + ", value=" + this.scheduledTime);
        return this.scheduledTime;
    }
}

