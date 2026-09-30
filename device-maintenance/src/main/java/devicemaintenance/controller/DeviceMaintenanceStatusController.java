package devicemaintenance.controller;

import devicemaintenance.dto.DeviceOperationStatus;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.service.DeviceMaintenanceStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备维护状态控制器
 * 提供设备维护状态查询接口
 */
@RestController
@RequestMapping("/restconf/operations")
@Slf4j
@RequiredArgsConstructor
public class DeviceMaintenanceStatusController {

    private final DeviceMaintenanceStatusService maintenanceStatusService;
    
    @Autowired(required = false)
    private PhyNodeDao phyNodeDao;

    /**
     * 检查设备是否可以执行指定任务
     */
    @GetMapping("/status/check")
    public ResponseEntity<Map<String, Object>> checkDeviceStatus(
            @RequestParam String deviceId,
            @RequestParam String taskType) {
        
        log.info("检查设备维护状态: deviceId={}, taskType={}", deviceId, taskType);
        
        try {
            DeviceTask.TaskType type = DeviceTask.TaskType.valueOf(taskType.toUpperCase());
            
            DeviceMaintenanceStatusService.MaintenanceStatusCheckResult result = 
                maintenanceStatusService.checkDeviceAvailableForTask(deviceId, type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deviceId", deviceId);
            response.put("taskType", taskType);
            response.put("available", result.isAvailable());
            
            if (!result.isAvailable()) {
                response.put("ongoingTaskType", result.getOngoingTaskType().name());
                response.put("ongoingTaskId", result.getOngoingTaskId());
                response.put("reason", result.getReason());
            }
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("无效的任务类型: {}", taskType);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid task type: " + taskType);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        } catch (Exception e) {
            log.error("检查设备状态失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to check device status: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 获取设备当前维护状态详情
     */
    @GetMapping("/status/{deviceId}")
    public ResponseEntity<Map<String, Object>> getDeviceMaintenanceStatus(
            @PathVariable String deviceId) {
        
        log.info("获取设备维护状态详情: deviceId={}", deviceId);
        
        try {
            DeviceMaintenanceStatusService.DeviceMaintenanceStatus status = 
                maintenanceStatusService.getDeviceMaintenanceStatus(deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deviceId", status.getDeviceId());
            response.put("isBusy", status.isBusy());
            
            if (status.isBusy()) {
                response.put("currentTaskType", status.getCurrentTaskType().name());
                response.put("currentTaskId", status.getCurrentTaskId());
                response.put("taskStartTime", status.getTaskStartTime().toString());
            }
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取设备状态失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get device status: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 获取设备操作状态
     * 从MongoDB查询设备的所有维护操作状态信息
     */
    @PostMapping("/device-maintenance:get-device-operations-status")
    public ResponseEntity<Map<String, Object>> getDeviceOperationStatus(
            @RequestBody Map<String, String> request) {
        
        String deviceIdentifier = request.get("deviceIdentifier");
        log.info("获取设备操作状态: deviceIdentifier={}", deviceIdentifier);
        
        if (deviceIdentifier == null || deviceIdentifier.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Missing required parameter: deviceIdentifier");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(errorResponse);
        }
        
        try {
            DeviceOperationStatus operationStatus = 
                maintenanceStatusService.getDeviceOperationStatus(deviceIdentifier);
            
            Map<String, Object> response = new HashMap<>();
            
            if (operationStatus != null) {
                response.put("success", true);
                response.put("data", operationStatus);
            } else {
                response.put("success", false);
                response.put("message", "Device not found: " + deviceIdentifier);
            }
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取设备操作状态失败: deviceIdentifier={}", deviceIdentifier, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get device operation status: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 获取所有主设备的操作状态
     * 从MongoDB查询所有主设备（Site-xxx#Ne-xxx格式，1个#）的维护操作状态信息
     * 支持可选的过滤条件
     */
    @PostMapping("/device-maintenance:get-all-device-operations-status")
    public ResponseEntity<Map<String, Object>> getAllDeviceOperationStatus(
            @RequestBody(required = false) Map<String, String> filterRequest) {
        
        log.info("获取所有主设备操作状态, 过滤条件: {}", filterRequest);
        
        try {
            java.util.List<DeviceOperationStatus> allDeviceStatus = 
                maintenanceStatusService.getAllDeviceOperationStatus(filterRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", allDeviceStatus);
            response.put("totalCount", allDeviceStatus.size());
            if (filterRequest != null && !filterRequest.isEmpty()) {
                response.put("filters", filterRequest);
            }
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取所有设备操作状态失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get all device operation statuses: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 获取全网所有主设备列表
     * POST /restconf/operations/device-maintenance:get-all-devices
     * 
     * 功能说明：
     * - 调用 phyNodeDao 查询所有主设备（Site-xxx#Ne-xxx格式）
     * - 自动过滤掉子设备（equipment-id，包含2个或更多#）
     * - 支持分页查询
     * - 支持简化模式（只返回deviceId列表）和详细模式（返回完整Node信息）
     * 
     * 请求参数：
     * - pageNum: 页码（可选，默认1，从1开始）
     * - pageSize: 每页大小（可选，默认20，最大1000）
     * - includeOperational: 是否包含运行态设备（可选，默认false，只返回配置态）
     * - simplified: 是否简化输出（可选，默认true，只返回deviceId列表）
     * 
     * 主设备定义：
     * - neId格式：Site-xxx#Ne-xxx（包含1个#分隔符）
     * - 排除子设备：Site-xxx#Ne-xxx#CHASSIS-1（包含2个或更多#）
     * 
     * 返回数据（简化模式 simplified=true）：
     * - success: 操作是否成功
     * - deviceIds: 设备ID数组 ["Site-xxx#Ne-xxx", ...]
     * - pageNum: 当前页码
     * - pageSize: 每页大小
     * - totalCount: 总设备数
     * - totalPages: 总页数
     * 
     * 返回数据（详细模式 simplified=false）：
     * - success: 操作是否成功
     * - data: 设备数组，每个设备包含完整的Node信息
     * - 设备字段包括：nodeId, terminationPoint, implementedInterface 等
     * - pageNum, pageSize, totalCount, totalPages: 分页信息
     * 
     * 使用场景：
     * - 批量升级/备份前获取设备ID列表（使用simplified=true）
     * - 设备详细信息查询（使用simplified=false）
     * - 系统监控和统计
     */
    @PostMapping("/device-maintenance:get-all-devices")
    public ResponseEntity<Map<String, Object>> getAllDevices(
            @RequestBody(required = false) Map<String, Object> request) {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 获取全网设备列表");
        
        // 解析请求参数
        Integer pageNum = 1;
        Integer pageSize = 20;
        Boolean includeOperational = false;
        Boolean simplified = true; // 默认简化模式：只返回deviceId列表
        
        if (request != null) {
            if (request.containsKey("pageNum")) {
                pageNum = ((Number) request.get("pageNum")).intValue();
            }
            if (request.containsKey("pageSize")) {
                pageSize = ((Number) request.get("pageSize")).intValue();
            }
            if (request.containsKey("includeOperational")) {
                includeOperational = (Boolean) request.get("includeOperational");
            }
            if (request.containsKey("simplified")) {
                simplified = (Boolean) request.get("simplified");
            }
        }
        
        // 参数校验
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 1000) {
            pageSize = 20;
        }
        
        log.info("  请求参数: pageNum={}, pageSize={}, includeOperational={}, simplified={}", 
                pageNum, pageSize, includeOperational, simplified);
        
        try {
            // 检查 phyNodeDao 是否可用
            if (phyNodeDao == null) {
                log.error("  ❌ PhyNodeDao 未注入，无法查询设备");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Device query service is unavailable (PhyNodeDao not injected)");
                errorResponse.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(errorResponse);
            }
            
            List<Node> devices;
            int totalCount;
            int totalPages;
            
            if (includeOperational) {
                // 获取运行态设备（不支持分页）
                log.info("  📡 查询运行态设备（无分页）...");
                devices = phyNodeDao.listOperPhyNodes();
                
                if (devices == null) {
                    devices = new ArrayList<>();
                }
                
                // 过滤掉子设备（只保留主设备：包含1个#的设备）
                devices = devices.stream()
                    .filter(node -> {
                        if (node == null || node.getNodeId() == null) {
                            return false;
                        }
                        String nodeId = node.getNodeId().getValue();
                        // 只保留包含1个#的设备（主设备）
                        long hashCount = nodeId.chars().filter(ch -> ch == '#').count();
                        return hashCount == 1;
                    })
                    .collect(Collectors.toList());
                
                totalCount = devices.size();
                
                // 手动分页
                int fromIndex = (pageNum - 1) * pageSize;
                int toIndex = Math.min(fromIndex + pageSize, totalCount);
                
                if (fromIndex < totalCount) {
                    devices = devices.subList(fromIndex, toIndex);
                } else {
                    devices = new ArrayList<>();
                }
                
                totalPages = (totalCount + pageSize - 1) / pageSize;
                
                log.info("  ✅ 成功获取运行态设备: 总数={}, 当前页={}/{}, 返回={}条", 
                        totalCount, pageNum, totalPages, devices.size());
                
            } else {
                // 获取配置态设备（支持原生分页）
                log.info("  📡 查询配置态设备（分页查询）...");
                PageResult<Node> pageResult = phyNodeDao.listConfigPhyNodesPaged(pageNum, pageSize);
                
                if (pageResult == null || pageResult.getList() == null) {
                    devices = new ArrayList<>();
                    totalCount = 0;
                    totalPages = 0;
                } else {
                    devices = pageResult.getList();
                    if (devices == null) {
                        devices = new ArrayList<>();
                    }
                    
                    // 过滤掉子设备（只保留主设备：包含1个#的设备）
                    devices = devices.stream()
                        .filter(node -> {
                            if (node == null || node.getNodeId() == null) {
                                return false;
                            }
                            String nodeId = node.getNodeId().getValue();
                            // 只保留包含1个#的设备（主设备）
                            long hashCount = nodeId.chars().filter(ch -> ch == '#').count();
                            return hashCount == 1;
                        })
                        .collect(Collectors.toList());
                    
                    totalCount = pageResult.getTotal() != null ? pageResult.getTotal().intValue() : 0;
                    totalPages = pageResult.getPages() != null ? pageResult.getPages() : 0;
                }
                
                log.info("  ✅ 成功获取配置态设备: 总数={}, 当前页={}/{}, 返回={}条", 
                        totalCount, pageNum, totalPages, devices.size());
            }
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            if (simplified) {
                // 简化模式：只返回deviceId列表
                List<String> deviceIds = devices.stream()
                    .map(node -> node.getNodeId() != null ? node.getNodeId().getValue() : null)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toList());
                
                response.put("deviceIds", deviceIds);
                log.info("  📦 简化模式：返回deviceId列表");
            } else {
                // 详细模式：返回完整Node信息
                response.put("data", devices);
                log.info("  📦 详细模式：返回完整Node信息");
            }
            
            response.put("pageNum", pageNum);
            response.put("pageSize", pageSize);
            response.put("totalCount", totalCount);
            response.put("totalPages", totalPages);
            response.put("currentPageCount", devices.size());
            response.put("includeOperational", includeOperational);
            response.put("simplified", simplified);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("  ❌ 获取全网设备列表失败", e);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get the full device list: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
}
