package devicemaintenance.controller;

import devicemaintenance.dto.BatchDetailDto;
import devicemaintenance.exception.InvalidParameterException;
import devicemaintenance.service.BatchLogService;
import devicemaintenance.service.UnifiedBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一批次管理Controller
 * 处理所有类型批次（DOWNLOAD、BACKUP、RESTORE、UPGRADE）的通用操作
 */
@RestController
@RequestMapping("/restconf/operations")
@Slf4j
@RequiredArgsConstructor
public class UnifiedBatchController {

    private final UnifiedBatchService unifiedBatchService;
    private final BatchLogService batchLogService;

    /**
     * 获取批次详情（所有类型）
     * POST /restconf/operations/device-maintenance:get-batch-detail
     * 
     * 功能说明：
     * - 获取任意类型批次的详细信息（DOWNLOAD、BACKUP、RESTORE、UPGRADE）
     * - 包括批次基本信息、设备任务列表
     * - 对于UPGRADE类型，还包括工作流信息
     */
    @PostMapping("/device-maintenance:get-batch-detail")
    public ResponseEntity<ApiResponse<BatchDetailDto.BatchDetailResponse>> getBatchDetail(
            @RequestBody Map<String, String> request) {
        
        String batchId = request.get("batchId");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 获取批次详情: batchId={}", batchId);

        // 参数校验 - 抛出异常让全局异常处理器处理
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new InvalidParameterException("batchId cannot be empty");
        }

        // 直接调用服务，异常会被全局异常处理器捕获
        BatchDetailDto.BatchDetailResponse detail = unifiedBatchService.getBatchDetail(batchId);

        // ⭐ 调试日志：查看返回给前端的 scheduledTime 值
        log.info("✅ 获取批次详情成功: batchName={}, type={}, status={}, scheduledTime={}",
            detail.getBatchName(), detail.getBatchType(), detail.getStatus(), detail.getScheduledTime());
        batchLogService.logBatchSnapshot(detail, "api:get-batch-detail");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return ResponseEntity.ok(ApiResponse.success(detail, "Batch detail retrieved successfully"));
    }

    /**
     * 获取批次的工作流列表（分页）
     * POST /restconf/operations/device-maintenance:get-batch-workflows
     * 
     * 功能说明：
     * - 获取指定批次的工作流列表（仅支持 UPGRADE 类型批次）
     * - 支持分页查询
     * - 返回总数、当前页数据
     * 
     * 请求参数：
     * - batchId: 批次ID（必填）
     * - pageNum: 页码（可选，默认1，从1开始）
     * - pageSize: 每页大小（可选，默认20）
     */
    @PostMapping("/device-maintenance:get-batch-workflows")
    public ResponseEntity<ApiResponse<UnifiedBatchService.PagedResult<BatchDetailDto.WorkflowInfo>>> getBatchWorkflows(
            @RequestBody Map<String, Object> request) {
        
        String batchId = (String) request.get("batchId");
        Integer pageNum = request.containsKey("pageNum") ? 
            Integer.parseInt(request.get("pageNum").toString()) : 1;
        Integer pageSize = request.containsKey("pageSize") ? 
            Integer.parseInt(request.get("pageSize").toString()) : 20;
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 获取批次工作流列表: batchId={}, pageNum={}, pageSize={}", batchId, pageNum, pageSize);

        // 参数校验 - 抛出异常让全局异常处理器处理
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new InvalidParameterException("batchId cannot be empty");
        }
        
        if (pageNum < 1) {
            throw new InvalidParameterException("pageNum must be greater than 0");
        }
        
        if (pageSize < 1 || pageSize > 1000) {
            throw new InvalidParameterException("pageSize must be between 1 and 1000");
        }

        // 直接调用服务，异常会被全局异常处理器捕获
        UnifiedBatchService.PagedResult<BatchDetailDto.WorkflowInfo> result = 
            unifiedBatchService.getBatchWorkflowsPaged(batchId, pageNum, pageSize);
        
        log.info("✅ 获取工作流列表成功: total={}, pageNum={}, pageSize={}, returned={}", 
            result.getTotal(), result.getPageNum(), result.getPageSize(), result.getData().size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return ResponseEntity.ok(ApiResponse.success(result, "Workflow list retrieved successfully"));
    }

    /**
     * 取消所有运行中的批次（所有类型）
     * POST /restconf/operations/device-maintenance:cancel-all-batches
     * 
     * 功能说明：
     * - 自动查询所有 RUNNING 状态的批次（包括 DOWNLOAD、BACKUP、RESTORE、UPGRADE）
     * - 逐个取消每个批次
     * - 对于 UPGRADE 类型：调用完整的工作流取消逻辑
     * - 对于其他类型：更新批次状态为 CANCELLED，取消关联的 DeviceTask
     * 
     * 无需输入参数
     */
    @PostMapping("/device-maintenance:cancel-all-batches")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelAllBatches() {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚫 接收到取消所有批次请求");

        // 直接调用服务，异常会被全局异常处理器捕获
        Map<String, Object> result = unifiedBatchService.cancelAllRunningBatches();
        
        int successCount = (int) result.get("successCount");
        int failureCount = (int) result.get("failureCount");
        
        String message = String.format("取消所有运行批次完成: 成功%d个，失败%d个", 
            successCount, failureCount);
        
        log.info("✅ {}", message);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    /**
     * 删除所有非运行中的批次（所有类型）
     * POST /restconf/operations/device-maintenance:delete-all-batches
     * 
     * 功能说明：
     * - 自动查询所有非 RUNNING 状态的批次（COMPLETED、FAILED、CANCELLED、PENDING）
     * - 包括所有类型：DOWNLOAD、BACKUP、RESTORE、UPGRADE
     * - 逐个删除每个批次
     * - 对于 UPGRADE 类型：调用完整的工作流删除逻辑
     * - 对于其他类型：删除批次记录和关联的 DeviceTask
     * 
     * 无需输入参数
     * 
     * 注意：RUNNING 状态的批次不会被删除，需要先使用 cancel-all-batches 接口取消
     */
    @PostMapping("/device-maintenance:delete-all-batches")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAllNonRunningBatches() {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🗑️ 接收到删除所有非运行批次请求");

        // 直接调用服务，异常会被全局异常处理器捕获
        Map<String, Object> result = unifiedBatchService.deleteAllNonRunningBatches();
        
        int successCount = (int) result.get("successCount");
        int failureCount = (int) result.get("failureCount");
        
        String message = String.format("删除所有非运行批次完成: 成功%d个，失败%d个", 
            successCount, failureCount);
        
        log.info("✅ {}", message);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    /**
     * 统一API响应格式
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String timestamp;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = java.time.LocalDateTime.now().toString();
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}
