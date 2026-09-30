package devicemaintenance.controller;

import devicemaintenance.dto.BatchUpgradeDto;
import devicemaintenance.exception.BatchStateException;
import devicemaintenance.exception.InvalidParameterException;
import devicemaintenance.exception.ResourceNotFoundException;
import devicemaintenance.service.BatchUpgradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;
import java.util.List;

/**
 * 批量升级控制器
 * 提供批量升级相关的RESTful API
 */
@RestController
@RequestMapping("/restconf/operations")
@Slf4j
@RequiredArgsConstructor
@Validated
public class BatchUpgradeController {

    private final BatchUpgradeService batchUpgradeService;

    /**
     * 创建批量升级任务
     * POST /restconf/operations/device-maintenance:create-upgrade-batch
     */
    @PostMapping("/device-maintenance:create-upgrade-batch")
    public ResponseEntity<ApiResponse<BatchUpgradeDto.BatchUpgradeResponse>> createBatchUpgrade(
            @Valid @RequestBody BatchUpgradeDto.CreateBatchUpgradeRequest request) {
        
        log.info("接收到创建批量升级请求: batchName={}, deviceCount={}, executionMode={}", 
                request.getBatchName(), request.getDeviceIds().size(), request.getExecutionMode());

        // 直接调用服务，异常由全局异常处理器捕获
        BatchUpgradeDto.BatchUpgradeResponse response = batchUpgradeService.createBatchUpgrade(request);
        
        // Service层错误已改为抛出异常，这里只处理成功情况
        return ResponseEntity.ok(ApiResponse.success(response, "Batch upgrade task created successfully"));
    }

    /**
     * 查询批量升级任务列表
     * POST /restconf/operations/device-maintenance:get-upgrade-batches
     */
    @PostMapping("/device-maintenance:get-upgrade-batches")
    public ResponseEntity<ApiResponse<List<BatchUpgradeDto.UpgradeBatchSummary>>> getUpgradeBatches(
            @RequestBody(required = false) BatchUpgradeDto.GetUpgradeBatchesRequest request) {

        log.info("查询批量升级任务列表: status={}, batchName={}",
                request != null ? request.getStatus() : null,
                request != null ? request.getBatchName() : null);

        // 直接调用服务，异常由全局异常处理器捕获
        List<BatchUpgradeDto.UpgradeBatchSummary> batches = batchUpgradeService.getUpgradeBatches(request);
        return ResponseEntity.ok(ApiResponse.success(batches,
                String.format("Query successful, found %d batch(es)", batches.size())));
    }

    /**
     * 获取所有批次列表（支持所有类型，带分页）
     * POST /restconf/operations/device-maintenance:get-all-batches
     * 
     * 功能说明：
     * - 查询所有类型的批次（UPGRADE、BACKUP、RESTORE、DOWNLOAD）
     * - 支持分页查询
     * - 支持按类型、状态过滤
     * 
     * 请求参数：
     * - pageNum: 页码（可选，默认1）
     * - pageSize: 每页大小（可选，默认20，最大1000）
     * - batchType: 批次类型（可选：UPGRADE, BACKUP, RESTORE, DOWNLOAD）
     * - status: 批次状态（可选：RUNNING, COMPLETED, FAILED等）
     */
    @PostMapping("/device-maintenance:get-all-batches")
    public ResponseEntity<ApiResponse<BatchUpgradeService.PagedBatchResult>> getAllBatches(
            @RequestBody devicemaintenance.dto.GetAllBatchesRequest request) {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 接收到获取所有批次请求");
        log.info("  pageNum={}, pageSize={}, batchType={}, status={}", 
            request.getPageNum(), request.getPageSize(), 
            request.getBatchType(), request.getStatus());

        // 参数校验
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        
        if (request.getPageSize() == null || request.getPageSize() < 1 || request.getPageSize() > 1000) {
            request.setPageSize(20);
        }

        // 直接调用服务，异常由全局异常处理器捕获
        BatchUpgradeService.PagedBatchResult result = batchUpgradeService.getAllBatches(request);
        
        log.info("✅ 获取批次列表成功: total={}, pageNum={}, pageSize={}, returned={}", 
            result.getTotal(), result.getPageNum(), result.getPageSize(), result.getData().size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return ResponseEntity.ok(ApiResponse.success(result, 
            String.format("Query successful, found %d batch(es)", result.getTotal())));
    }

    /**
     * 重试批量升级任务
     * POST /restconf/operations/device-maintenance:retry-batch-upgrade
     */
    @PostMapping("/device-maintenance:retry-upgrade-task")
    public ResponseEntity<ApiResponse<String>> retryBatchUpgrade(
            @Valid @RequestBody BatchUpgradeDto.RetryBatchUpgradeRequest request) {
        
        log.info("重试批量升级任务: batchId={}", request.getBatchId());

        // TODO: 实现重试逻辑，异常由全局异常处理器捕获
        return ResponseEntity.ok(ApiResponse.success("Retry feature not yet implemented", "Retry request received"));
    }

    /**
     * 升级确认
     * POST /restconf/operations/device-maintenance:commit-batch-upgrade
     */
    @PostMapping("/device-maintenance:commit-upgrade-task")
    public ResponseEntity<ApiResponse<String>> commitBatchUpgrade(
            @Valid @RequestBody BatchUpgradeDto.CommitUpgradeRequest request) {
        
        log.info("升级确认: batchId={}", request.getBatchId());

        // TODO: 实现升级确认逻辑（Mock接口），异常由全局异常处理器捕获
        return ResponseEntity.ok(ApiResponse.success("Upgrade confirmed successfully", "Upgrade has been confirmed"));
    }

    /**
     * 升级回滚
     * POST /restconf/operations/device-maintenance:rollback-batch-upgrade
     */
    @PostMapping("/device-maintenance:rollback-upgrade-task")
    public ResponseEntity<ApiResponse<String>> rollbackBatchUpgrade(
            @Valid @RequestBody BatchUpgradeDto.RollbackUpgradeRequest request) {
        
        log.info("升级回滚: batchId={}", request.getBatchId());

        // TODO: 实现升级回滚逻辑（Mock接口），异常由全局异常处理器捕获
        return ResponseEntity.ok(ApiResponse.success("Upgrade rolled back successfully", "Upgrade has been rolled back"));
    }

    /**
     * 取消批次任务（适用于所有批次类型）
     * POST /restconf/operations/device-maintenance:cancel-upgrade-batch
     * 
     * 支持批次类型：UPGRADE、BACKUP、RESTORE、DOWNLOAD
     */
    @PostMapping("/device-maintenance:cancel-upgrade-batch")
    public ResponseEntity<ApiResponse<String>> cancelBatchUpgrade(
            @RequestBody Map<String, String> request) {
        String batchId = request.get("batchId");
        
        log.info("取消批次任务: batchId={}", batchId);

        // 直接调用服务，异常由全局异常处理器捕获
        batchUpgradeService.cancelBatch(batchId);
        
        return ResponseEntity.ok(ApiResponse.success("Cancelled successfully", "Batch task has been cancelled, all in-progress tasks have been stopped"));
    }


    /**
     * 删除批次任务（适用于所有批次类型）
     * POST /restconf/operations/device-maintenance:delete-upgrade-batch
     * 
     * 支持批次类型：UPGRADE、BACKUP、RESTORE、DOWNLOAD
     * 注意：RUNNING状态的批次无法直接删除，需要先取消
     */
    @PostMapping("/device-maintenance:delete-upgrade-batch")
    public ResponseEntity<ApiResponse<String>> deleteBatchUpgrade(
            @RequestBody Map<String, String> request) {
        String batchId = request.get("batchId");
        
        log.info("删除批次任务: batchId={}", batchId);

        // 直接调用服务，异常由全局异常处理器捕获
        batchUpgradeService.deleteBatch(batchId);
        
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully", "Batch task has been deleted"));
    }


    /**
     * 统一API响应格式
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private long timestamp;

        public ApiResponse() {
            this.timestamp = System.currentTimeMillis();
        }

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, "Operation successful", data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 新架构接口：基于 Workflow 的批次管理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * 手动模式：触发下一步
     * POST /restconf/operations/device-maintenance:proceed-upgrade-batch
     */
    @PostMapping("/device-maintenance:proceed-upgrade-batch")
    public ResponseEntity<ApiResponse<String>> proceedToNextStep(@RequestBody Map<String, String> request) {
        String batchId = request.get("batchId");
        
        log.info("接收到手动触发下一步请求: batchId={}", batchId);

        // 直接调用服务，异常由全局异常处理器捕获
        // IllegalStateException 会被映射为 409 CONFLICT
        // IllegalArgumentException 会被映射为 400 BAD_REQUEST
        batchUpgradeService.proceedToNextStep(batchId);
        return ResponseEntity.ok(ApiResponse.success("Next step triggered"));
    }

    /**
     * 重试工作流
     * POST /restconf/operations/device-maintenance:retry-upgrade-workflow
     */
    @PostMapping("/device-maintenance:retry-upgrade-workflow")
    public ResponseEntity<ApiResponse<String>> retryWorkflow(@RequestBody Map<String, String> request) {
        String batchId = request.get("batchId");
        String deviceId = request.get("deviceId");
        
        log.info("接收到重试工作流请求: batchId={}, deviceId={}", batchId, deviceId);

        // 直接调用服务，异常由全局异常处理器捕获
        batchUpgradeService.retryWorkflow(batchId, deviceId);
        return ResponseEntity.ok(ApiResponse.success("Workflow retry started"));
    }

    /**
     * 移除工作流
     * POST /restconf/operations/device-maintenance:remove-upgrade-workflow
     */
    @PostMapping("/device-maintenance:remove-upgrade-workflow")
    public ResponseEntity<ApiResponse<String>> removeWorkflow(@RequestBody Map<String, String> request) {
        String batchId = request.get("batchId");
        String deviceId = request.get("deviceId");
        
        log.info("接收到移除工作流请求: batchId={}, deviceId={}", batchId, deviceId);

        // 直接调用服务，异常由全局异常处理器捕获
        batchUpgradeService.removeWorkflow(batchId, deviceId);
        return ResponseEntity.ok(ApiResponse.success("Workflow removed"));
    }
}
