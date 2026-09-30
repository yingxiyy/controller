package devicemaintenance.controller;

import devicemaintenance.dto.DeviceBackupDto;
import devicemaintenance.service.DeviceBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 设备备份控制器
 * 提供设备备份相关的REST API
 */
@RestController
@RequestMapping("/restconf/operations")
@Slf4j
@RequiredArgsConstructor
public class DeviceBackupController {

    private final DeviceBackupService deviceBackupService;

    /**
     * 批量设备数据备份（指定设备列表）
     * 注意：单设备备份也使用此接口，只需在deviceIds中传入一个设备ID即可
     */
    @PostMapping("/device-maintenance:batch-backup-devices")
    public ResponseEntity<ApiResponse<DeviceBackupDto.BatchBackupSummary>> batchBackupDevices(
            @Valid @RequestBody DeviceBackupDto.BatchBackupRequest request) {
        
        log.info("接收到批量设备备份请求: batchName={}, deviceCount={}", 
            request.getBatchName(), request.getDeviceIds() != null ? request.getDeviceIds().size() : 0);
        
        // 直接调用服务，异常由全局异常处理器捕获
        DeviceBackupDto.BatchBackupSummary response = deviceBackupService.batchBackupDevices(request);
        
        return ResponseEntity.ok(ApiResponse.success(
            "批量设备备份任务启动成功",
            response
        ));
    }

    /**
     * 获取批量备份状态
     */
    @PostMapping("/device-maintenance:get-batch-backup-status")
    public ResponseEntity<ApiResponse<DeviceBackupDto.BatchBackupSummary>> getBatchBackupStatus(
            @RequestBody BatchStatusRequest request) {
        
        log.info("获取批量备份状态: batchId={}", request.getBatchId());
        
        // 直接调用服务，异常由全局异常处理器捕获
        DeviceBackupDto.BatchBackupSummary response = deviceBackupService.getBatchBackupStatus(request.getBatchId());
        
        return ResponseEntity.ok(ApiResponse.success(
            "获取批量备份状态成功",
            response
        ));
    }

    /**
     * 批次状态查询请求
     */
    public static class BatchStatusRequest {
        private String batchId;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
    }

    /**
     * 统一API响应格式
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String timestamp;

        public ApiResponse() {
            this.timestamp = java.time.LocalDateTime.now().toString();
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = true;
            response.message = message;
            response.data = data;
            return response;
        }

        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = false;
            response.message = message;
            response.data = null;
            return response;
        }

        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
