package devicemaintenance.controller;

import devicemaintenance.dto.SoftwareRollbackDto;
import devicemaintenance.service.SoftwareRollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 软件回滚控制器
 */
@Slf4j
@RestController
@RequestMapping("/restconf/operations")
@RequiredArgsConstructor
@Validated
public class SoftwareRollbackController {

    private final SoftwareRollbackService softwareRollbackService;

    /**
     * 回滚软件版本（单设备）
     * POST /restconf/operations/device-maintenance:rollback-software
     */
    @PostMapping("/device-maintenance:rollback-software")
    public ResponseEntity<ApiResponse<SoftwareRollbackDto.RollbackResponse>> rollbackSoftware(
            @Valid @RequestBody SoftwareRollbackDto.RollbackRequest request) {
        
        log.info("接收到软件回滚请求: deviceId={}, sftpServerName={}", 
            request.getDeviceId(), request.getSftpServerName());
        
        // 直接调用服务，异常由全局异常处理器捕获
        SoftwareRollbackDto.RollbackResponse response = softwareRollbackService.startRollback(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Software rollback task started"));
    }

    /**
     * 重试失败的回滚任务（复用当前任务记录）
     * POST /restconf/operations/device-maintenance:retry-rollback-task
     */
    @PostMapping("/device-maintenance:retry-rollback-task")
    public ResponseEntity<ApiResponse<SoftwareRollbackDto.RollbackResponse>> retryRollbackTask(
            @Valid @RequestBody SoftwareRollbackDto.RetryRollbackRequest request) {

        log.info("接收到回滚重试请求: taskId={}", request.getTaskId());
        SoftwareRollbackDto.RollbackResponse response = softwareRollbackService.retryRollbackTask(request.getTaskId());
        return ResponseEntity.ok(ApiResponse.success(response, "Software rollback task retried"));
    }

    /**
     * 批量回滚软件版本（多设备，每个设备对应不同的回滚文件）
     * POST /restconf/operations/device-maintenance:batch-rollback-software
     * 
     * 请求示例：
     * {
     *   "batchName": "批量回滚测试",
     *   "deviceIds": ["Site-1#Ne-1", "Site-2#Ne-2"],
     *   "fileNames": ["Release_v1.0.tar", "Release_v2.0.tar"],
     *   "sftpServerName": "backup-sftp-server",
     *   "scheduledTime": null,
     *   "remark": "紧急回滚"
     * }
     */
    @PostMapping("/device-maintenance:batch-rollback-software")
    public ResponseEntity<ApiResponse<SoftwareRollbackDto.BatchRollbackSummary>> batchRollbackSoftware(
            @Valid @RequestBody SoftwareRollbackDto.BatchRollbackRequest request) {
        
        log.info("接收到批量回滚请求: batchName={}, 设备数量={}, SFTP={}",
            request.getBatchName(), request.getDeviceIds().size(), request.getSftpServerName());
        
        // 调用批量回滚服务
        SoftwareRollbackDto.BatchRollbackSummary summary = softwareRollbackService.batchRollback(request);
        return ResponseEntity.ok(ApiResponse.success(summary, "Batch rollback task submitted"));
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

