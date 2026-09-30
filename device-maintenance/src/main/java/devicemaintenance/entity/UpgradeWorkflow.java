package devicemaintenance.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * 升级工作流实体 - 轻量级流程控制器
 * 
 * 职责：
 * 1. 记录工作流当前执行到哪一步（currentStep）
 * 2. 记录工作流整体状态（PENDING/RUNNING/COMPLETED/FAILED/CANCELLED）
 * 3. 控制重试次数
 * 
 * 不包含：
 * - 各步骤的详细状态、错误信息、时间戳等 → 由 DeviceTask 存储
 * - 步骤启用配置（enableDownload/Backup/Upgrade）→ 由 UpgradeBatch 统一配置
 */
@Entity
@Table(name = "dm_upgrade_workflow")
@Data
@NoArgsConstructor
public class UpgradeWorkflow {

    @Id
    @Column(name = "workflow_id", length = 64)
    private String workflowId;

    @Column(name = "batch_id", length = 64, nullable = false)
    private String batchId;

    @Column(name = "device_id", length = 64, nullable = false)
    private String deviceId;

    // ===== Workflow 核心状态 =====
    
    /**
     * ⚠️ 状态字段不再持久化到数据库
     * 通过 @Transient 标记为临时字段，运行时动态计算
     * 
     * 使用 setComputedStatus() 设置计算后的状态
     */
    @Transient
    private WorkflowStatus computedStatus;

    /**
     * 当前执行步骤（DOWNLOAD/BACKUP/ACTIVATE）
     * 用于控制流程进度，不存储步骤细节
     */
    @Column(name = "current_step", length = 20)
    private String currentStep;

    // ===== 重试控制 =====
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 3;

    // ===== 构造函数 =====
    public UpgradeWorkflow(String workflowId, String batchId, String deviceId) {
        this.workflowId = workflowId;
        this.batchId = batchId;
        this.deviceId = deviceId;
        this.computedStatus = WorkflowStatus.WORKFLOW_PENDING;  // 默认状态
        this.retryCount = 0;
    }

    // ===== 枚举定义 =====
    
    /**
     * Workflow 状态（计算状态，格式：STEP_STATUS）
     * 
     * 状态不再存储在数据库，而是根据关联的 DeviceTask 动态计算
     */
    public enum WorkflowStatus {
        // 下载相关
        DOWNLOAD_NOTSTART,   // 下载未开始
        DOWNLOAD_PENDING,    // 下载待执行
        DOWNLOAD_RUNNING,    // 下载进行中
        DOWNLOAD_SUCCESS,    // 下载成功
        DOWNLOAD_FAILED,     // 下载失败
        
        // 备份相关
        BACKUP_NOTSTART,     // 备份未开始
        BACKUP_PENDING,      // 备份待执行
        BACKUP_RUNNING,      // 备份进行中
        BACKUP_SUCCESS,      // 备份成功
        BACKUP_FAILED,       // 备份失败
        
        // 激活相关
        ACTIVATE_NOTSTART,   // 激活未开始
        ACTIVATE_PENDING,    // 激活待执行
        ACTIVATE_RUNNING,    // 激活进行中
        ACTIVATE_SUCCESS,    // 激活成功
        ACTIVATE_FAILED,     // 激活失败
        
        // 提交相关
        COMMIT_NOTSTART,     // 提交未开始
        COMMIT_PENDING,      // 提交待执行
        COMMIT_RUNNING,      // 提交进行中
        COMMIT_SUCCESS,      // 提交成功
        COMMIT_FAILED,       // 提交失败
        
        // 整体状态
        WORKFLOW_PENDING,    // 工作流待执行（尚未开始任何步骤）
        WORKFLOW_COMPLETED,  // 工作流已完成（所有步骤成功）
        WORKFLOW_CANCELLED   // 工作流已取消
    }

    // ===== 业务方法 =====

    /**
     * 获取计算后的状态
     * 
     * ⚠️ 注意：状态不存储在数据库，需要外部调用时先通过 setComputedStatus() 设置
     */
    public WorkflowStatus getStatus() {
        return this.computedStatus != null ? this.computedStatus : WorkflowStatus.WORKFLOW_PENDING;
    }

    /**
     * 设置计算后的状态
     * 
     * 此方法由 WorkflowManagementService 调用，根据 DeviceTask 状态计算
     */
    public void setComputedStatus(WorkflowStatus status) {
        this.computedStatus = status;
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        WorkflowStatus status = getStatus();
        return (status.name().endsWith("_FAILED") || status == WorkflowStatus.WORKFLOW_CANCELLED) 
               && this.retryCount < this.maxRetryCount;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    /**
     * 检查工作流是否已完成（成功或失败）
     * ⭐ COMMIT_SUCCESS 也算作终态（表示整个升级流程已成功完成）
     */
    public boolean isFinished() {
        WorkflowStatus status = getStatus();
        return status == WorkflowStatus.WORKFLOW_COMPLETED 
               || status == WorkflowStatus.COMMIT_SUCCESS
               || status == WorkflowStatus.WORKFLOW_CANCELLED
               || status.name().endsWith("_FAILED");
    }
    
    /**
     * 检查工作流是否成功完成
     * ⭐ COMMIT_SUCCESS 也算作完成状态（表示整个升级流程已成功完成）
     */
    public boolean isCompleted() {
        WorkflowStatus status = getStatus();
        return status == WorkflowStatus.WORKFLOW_COMPLETED 
               || status == WorkflowStatus.COMMIT_SUCCESS;
    }
    
    /**
     * 检查工作流是否失败
     */
    public boolean isFailed() {
        return getStatus().name().endsWith("_FAILED");
    }

}

