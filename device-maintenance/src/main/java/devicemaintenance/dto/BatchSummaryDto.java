package devicemaintenance.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 批次摘要DTO（用于列表展示）
 */
@Data
public class BatchSummaryDto {
    
    /**
     * 批次ID
     */
    private String batchId;
    
    /**
     * 批次名称
     */
    private String batchName;
    
    /**
     * 批次类型
     * UPGRADE, BACKUP, RESTORE, DOWNLOAD
     */
    private String batchType;
    
    /**
     * 批次状态
     * RUNNING, COMPLETED, FAILED, CANCELLED, etc.
     */
    private String status;
    
    /**
     * 执行模式
     * MANUAL, AUTOMATIC, SCHEDULED
     */
    private String executionMode;
    
    /**
     * 总设备数/工作流数
     */
    private Integer totalCount;
    
    /**
     * 成功数
     */
    private Integer successCount;
    
    /**
     * 失败数
     */
    private Integer failedCount;
    
    /**
     * 运行中/待执行数
     */
    private Integer runningCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startedTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
    
    /**
     * 描述信息
     */
    private String description;
}

