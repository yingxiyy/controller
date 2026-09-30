package devicemaintenance.controller;

import devicemaintenance.dto.DeviceRestoreDto;
import devicemaintenance.service.DeviceRestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 设备恢复控制器
 * 提供设备数据和配置恢复相关的RESTful API
 */
@RestController
@RequestMapping("/restconf/operations")
@Slf4j
@RequiredArgsConstructor
@Validated
public class DeviceRestoreController {

    private final DeviceRestoreService deviceRestoreService;

    /**
     * 启动设备数据恢复
     * POST /restconf/operations/device-maintenance:restore-device-data
     */
    @PostMapping("/device-maintenance:restore-device-data")
    public ResponseEntity<ApiResponse<DeviceRestoreDto.RestoreResponse>> restoreDeviceData(
            @Valid @RequestBody DeviceRestoreDto.RestoreRequest request) {
        
        log.info("接收到设备恢复请求: deviceId={}, filePath={}", 
                request.getDeviceId(), request.getFilePath());

        // 直接调用服务，异常由全局异常处理器捕获
        DeviceRestoreDto.RestoreResponse response = deviceRestoreService.startRestore(request);
        
        // Service层错误已改为抛出异常，这里只处理成功情况
        return ResponseEntity.ok(ApiResponse.success(response, "Device restore task started"));
    }

    /**
     * 批量设备恢复
     * POST /restconf/operations/device-maintenance:batch-restore-devices
     * 
     * 支持：
     * 1. 多设备批量恢复
     * 2. 自动查找最新备份（基于basePath + dateDir）
     * 3. 定时执行
     * 4. Debug模式
     */
    @PostMapping("/device-maintenance:batch-restore-devices")
    public ResponseEntity<ApiResponse<DeviceRestoreDto.BatchRestoreSummary>> batchRestoreDevices(
            @Valid @RequestBody DeviceRestoreDto.BatchRestoreRequest request) {
        
        log.info("接收到批量恢复请求: 设备数={}, basePath={}, dateDir={}", 
                request.getDeviceIds().size(), request.getBasePath(), request.getDateDir());

        // 直接调用服务，异常由全局异常处理器捕获
        DeviceRestoreDto.BatchRestoreSummary summary = 
            deviceRestoreService.batchRestoreDevices(request);
        
        return ResponseEntity.ok(ApiResponse.success(summary, "Batch restore task submitted"));
    }

    // 任务查询功能已由 DeviceTaskController 统一提供

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
}
