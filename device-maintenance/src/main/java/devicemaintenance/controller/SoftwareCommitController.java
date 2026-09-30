package devicemaintenance.controller;

import devicemaintenance.dto.SoftwareCommitDto;
import devicemaintenance.service.SoftwareCommitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 软件提交控制器
 */
@Slf4j
@RestController
@RequestMapping("/restconf/operations")
@RequiredArgsConstructor
@Validated
public class SoftwareCommitController {

    private final SoftwareCommitService softwareCommitService;

    /**
     * 批量提交软件版本（确认激活的版本）
     * POST /restconf/operations/device-maintenance:batch-commit-upgrade
     */
    @PostMapping("/device-maintenance:batch-commit-upgrade")
    public ResponseEntity<ApiResponse<SoftwareCommitDto.BatchCommitResponse>> batchCommitSoftware(
            @Valid @RequestBody SoftwareCommitDto.BatchCommitRequest request) {
        
        log.info("接收到批量软件提交请求: deviceCount={}, sftpServerName={}", 
            request.getDeviceIds().size(), request.getSftpServerName());
        
        // 直接调用服务，异常由全局异常处理器捕获
        SoftwareCommitDto.BatchCommitResponse response = softwareCommitService.batchCommit(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Batch software commit task started"));
    }

    /**
     * API响应封装
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, data, message);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, null, message);
        }
    }
}

