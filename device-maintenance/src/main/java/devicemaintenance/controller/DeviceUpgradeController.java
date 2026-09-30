package devicemaintenance.controller;

import devicemaintenance.dto.DeviceUpgradeDto;
import devicemaintenance.service.DeviceUpgradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 设备升级控制器
 * 提供设备软件升级相关的RESTful API
 */
@RestController
@RequestMapping("/restconf/operations")
@Slf4j
@RequiredArgsConstructor
@Validated
public class DeviceUpgradeController {

    private final DeviceUpgradeService deviceUpgradeService;

    /**
     * 批量升级设备软件（注意：升级操作必须在batch中进行，batchName为必填项）
     * POST /restconf/operations/device-maintenance:batch-upgrade-software
     * 
     * 说明：
     * - 单设备升级：deviceIds传入1个元素，但batchName仍然必须提供
     * - 批量升级：deviceIds传入多个元素，batchName必须提供
     * - 升级操作需要工作流管理，因此必须在batch中进行
     */
    @PostMapping("/device-maintenance:batch-upgrade-software")
    public ResponseEntity<ApiResponse<DeviceUpgradeDto.BatchUpgradeResponse>> batchUpgradeSoftware(
            @Valid @RequestBody DeviceUpgradeDto.BatchUpgradeRequest request) {
        log.info("接收到批量软件升级请求: batchName={}, deviceCount={}, targetVersion={}", 
                request.getBatchName(), request.getDeviceIds().size(), request.getTargetVersion());
        // 直接调用服务，异常由全局异常处理器捕获
        DeviceUpgradeDto.BatchUpgradeResponse response = deviceUpgradeService.batchUpgrade(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Batch software upgrade task started"));
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
            return new ApiResponse<>(true, "操作成功", data);
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
