package devicemaintenance.controller;

import devicemaintenance.service.DeviceAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备可用性查询接口（辅助测试）
 */
@Slf4j
@RestController
@RequestMapping("/restconf/operations")
@RequiredArgsConstructor
public class DeviceAvailabilityController {

    private final DeviceAvailabilityService deviceAvailabilityService;

    /**
     * 查询单个设备可用性
     * 
     * POST /restconf/operations/device-maintenance:check-device-availability
     */
    @PostMapping("/device-maintenance:check-device-availability")
    public ResponseEntity<UnifiedBatchController.ApiResponse<DeviceAvailabilityService.DeviceAvailabilityInfo>> checkDeviceAvailability(
            @RequestBody CheckDeviceAvailabilityRequest request) {
        
        log.info("接收到设备可用性查询请求: deviceId={}", request.getDeviceId());
        
        // 直接调用服务，异常由全局异常处理器捕获
        DeviceAvailabilityService.DeviceAvailabilityInfo info = 
            deviceAvailabilityService.getDeviceAvailability(request.getDeviceId());
        
        return ResponseEntity.ok(UnifiedBatchController.ApiResponse.success(
            info,
            info.isAvailable() ? "Device is available" : "Device is busy"
        ));
    }

    /**
     * 批量查询设备可用性
     * 
     * POST /restconf/operations/device-maintenance:batch-check-device-availability
     */
    @PostMapping("/device-maintenance:batch-check-device-availability")
    public ResponseEntity<UnifiedBatchController.ApiResponse<List<DeviceAvailabilityService.DeviceAvailabilityInfo>>> batchCheckDeviceAvailability(
            @RequestBody BatchCheckDeviceAvailabilityRequest request) {
        
        log.info("接收到批量设备可用性查询请求: deviceCount={}", request.getDeviceIds().size());
        
        // 直接调用服务，异常由全局异常处理器捕获
        List<DeviceAvailabilityService.DeviceAvailabilityInfo> infos = 
            deviceAvailabilityService.batchCheckDeviceAvailability(request.getDeviceIds());
        
        long availableCount = infos.stream().filter(DeviceAvailabilityService.DeviceAvailabilityInfo::isAvailable).count();
        long busyCount = infos.size() - availableCount;
        
        return ResponseEntity.ok(UnifiedBatchController.ApiResponse.success(
            infos,
            String.format("Query completed: %d device(s) available, %d device(s) busy", availableCount, busyCount)
        ));
    }

    /**
     * 单个设备可用性查询请求
     */
    public static class CheckDeviceAvailabilityRequest {
        private String deviceId;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    /**
     * 批量设备可用性查询请求
     */
    public static class BatchCheckDeviceAvailabilityRequest {
        private List<String> deviceIds;

        public List<String> getDeviceIds() { return deviceIds; }
        public void setDeviceIds(List<String> deviceIds) { this.deviceIds = deviceIds; }
    }
}

