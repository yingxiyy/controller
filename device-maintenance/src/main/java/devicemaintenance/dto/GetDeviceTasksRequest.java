package devicemaintenance.dto;

import lombok.Data;

/**
 * 获取设备任务列表请求DTO
 */
@Data
public class GetDeviceTasksRequest {
    
    /**
     * 任务类型过滤条件
     */
    private String taskType;
    
    /**
     * 状态过滤条件
     */
    private String status;
    
    /**
     * 设备ID过滤条件
     */
    private String deviceId;
    
    /**
     * 批次ID过滤条件 ⭐ 新增
     */
    private String batchId;
    
    /**
     * 页码（从0开始）
     */
    private Integer page = 0;
    
    /**
     * 每页大小
     */
    private Integer size = 20;
    
}
