package devicemaintenance.controller;

import devicemaintenance.dto.SoftwareDownloadDto;
import devicemaintenance.service.SoftwareDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 设备软件下载控制器
 * 提供设备软件下载相关的RESTful API
 */
@RestController
@RequestMapping("/restconf/operations")
@Slf4j
@RequiredArgsConstructor
@Validated
public class SoftwareDownloadController {

    private final SoftwareDownloadService softwareDownloadService;

    /**
     * 批量设备软件下载（统一接口，支持单设备或多设备）
     * POST /restconf/operations/device-maintenance:batch-download-software
     * 
     * 说明：
     * - 单设备操作：deviceIds传入1个元素，batchName传null即可
     * - 批量操作：deviceIds传入多个元素，batchName必须提供
     * 
     * @param request 批量下载请求
     */
    @PostMapping("/device-maintenance:batch-download-software")
    public ResponseEntity<ApiResponse<SoftwareDownloadDto.BatchDownloadResponse>> batchDownloadSoftware(
            @Valid @RequestBody SoftwareDownloadDto.BatchDownloadRequest request) {
        
        log.info("接收到批量软件下载请求: batchName={}, deviceCount={}, package={}", 
                request.getBatchName(), request.getDeviceIds().size(), request.getSoftwarePackage());

        // 直接调用服务，异常由全局异常处理器捕获
        SoftwareDownloadDto.BatchDownloadResponse response = softwareDownloadService.batchDownload(request);
        
        // ✅ 根据实际结果返回成功或失败
        // ⚠️ 注意：这里判断的是 successCount/failedCount，而不是字符串状态
        if (response.getFailedCount() > 0 && response.getSuccessCount() == 0) {
            // 所有设备都失败（pre-validation 或 RPC 失败）
            ApiResponse<SoftwareDownloadDto.BatchDownloadResponse> errorResponse = 
                new ApiResponse<>(false, "批量软件下载任务失败：所有设备都无法执行", response);
            return ResponseEntity.ok(errorResponse);
        } else if (response.getFailedCount() > 0 && response.getSuccessCount() > 0) {
            // 部分成功
            return ResponseEntity.ok(ApiResponse.success(response, 
                String.format("批量软件下载任务部分成功：%d 成功，%d 失败", 
                    response.getSuccessCount(), response.getFailedCount())));
        } else {
            // 全部成功（failedCount == 0）
            return ResponseEntity.ok(ApiResponse.success(response, "Batch software download task started"));
        }
    }

    // 任务查询功能已移动到 DeviceTaskController 中，提供通用的设备任务查询功能

    // 任务状态更新已移至内部服务逻辑中，不再暴露为REST接口
    // Mock服务会直接更新数据库状态，真实服务通过Kafka消息更新状态

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
