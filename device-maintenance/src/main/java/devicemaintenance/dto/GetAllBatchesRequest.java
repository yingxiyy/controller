package devicemaintenance.dto;

import devicemaintenance.entity.Batch;
import lombok.Data;

/**
 * 获取所有批次请求DTO
 */
@Data
public class GetAllBatchesRequest {
    
    /**
     * 页码（从1开始，默认1）
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小（默认20，最大1000）
     */
    private Integer pageSize = 20;
    
    /**
     * 批次类型过滤（可选）
     * UPGRADE, BACKUP, RESTORE, DOWNLOAD
     */
    private String batchType;
    
    /**
     * 批次状态过滤（可选）
     * RUNNING, COMPLETED, FAILED, etc.
     */
    private String status;
}

