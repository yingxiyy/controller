package devicemaintenance.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 批次设备关联实体
 * 根据swagger.yaml简化，只保留必要字段
 * 
 * 设备操作链支持：
 * - 支持重试计数器（retryCount）
 * - 记录最后失败步骤（lastFailedStep）
 * - 追踪设备状态（deviceStatus）
 */
@Entity
@Table(name = "dm_batch_device")
@Data
@NoArgsConstructor
public class BatchDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "batch_id", length = 64, nullable = false)
    private String batchId;

    @Column(name = "device_id", length = 100, nullable = false)
    private String deviceId;

    @Column(name = "device_name", length = 200)
    private String deviceName;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "vendor_type", length = 50)
    private String vendorType;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 设备操作链相关字段
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 重试次数
     * 每次手动重试时自增，达到最大重试次数后设备状态变为 MAX_RETRIES_EXCEEDED
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * 最后失败的步骤
     * 取值：DOWNLOAD, BACKUP, UPGRADE
     * 用于重试时从失败步骤开始
     */
    @Column(name = "last_failed_step", length = 20)
    private String lastFailedStep;

    /**
     * 设备状态
     * 追踪单个设备在批次中的执行状态
     */
    @Column(name = "device_status", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceStatus deviceStatus = DeviceStatus.PENDING;

    /**
     * 最后更新时间
     * 每次状态变更时更新
     */
    @Column(name = "last_updated_time")
    private LocalDateTime lastUpdatedTime;

    /**
     * 设备状态枚举
     */
    public enum DeviceStatus {
        /** 待执行 */
        PENDING,
        
        /** 执行中 */
        IN_PROGRESS,
        
        /** 成功 */
        SUCCESS,
        
        /** 失败（可重试） */
        FAILED,
        
        /** 超过最大重试次数 */
        MAX_RETRIES_EXCEEDED,
        
        /** 已取消 */
        CANCELLED
    }

    public BatchDevice(String batchId, String deviceId, String deviceName) {
        this.batchId = batchId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.createdTime = LocalDateTime.now();
        this.deviceStatus = DeviceStatus.PENDING;
        this.retryCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
        if (deviceStatus == null) {
            deviceStatus = DeviceStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 设备操作链辅助方法
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 更新设备状态
     */
    public void updateStatus(DeviceStatus status) {
        this.deviceStatus = status;
        this.lastUpdatedTime = LocalDateTime.now();
    }

    /**
     * 记录失败步骤
     */
    public void markStepFailed(String stepName) {
        this.lastFailedStep = stepName;
        this.deviceStatus = DeviceStatus.FAILED;
        this.lastUpdatedTime = LocalDateTime.now();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastUpdatedTime = LocalDateTime.now();
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry(int maxRetryCount) {
        return this.retryCount < maxRetryCount 
            && this.deviceStatus == DeviceStatus.FAILED;
    }

    /**
     * 标记为超过最大重试次数
     */
    public void markMaxRetriesExceeded() {
        this.deviceStatus = DeviceStatus.MAX_RETRIES_EXCEEDED;
        this.lastUpdatedTime = LocalDateTime.now();
    }

    /**
     * 重置为待执行状态（用于重试）
     */
    public void resetForRetry() {
        this.deviceStatus = DeviceStatus.IN_PROGRESS;
        this.lastUpdatedTime = LocalDateTime.now();
    }

    /**
     * 标记为成功
     */
    public void markSuccess() {
        this.deviceStatus = DeviceStatus.SUCCESS;
        this.lastFailedStep = null;
        this.lastUpdatedTime = LocalDateTime.now();
    }
}
