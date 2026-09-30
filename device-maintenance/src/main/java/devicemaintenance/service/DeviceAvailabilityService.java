package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.repository.DeviceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备可用性检查服务
 * 
 * 功能：
 * - 检查设备是否可以执行新的维护操作
 * - 查询设备当前的任务状态
 * - 提供批量设备可用性检查
 * 
 * ✅ v2.0 升级：集成严格模式 Pre-validation
 * - 设备存在性验证
 * - 设备可用性验证
 * - SFTP 服务器验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAvailabilityService {

    private final DeviceTaskRepository deviceTaskRepository;
    private final PreValidationService preValidationService;

    /**
     * 检查单个设备是否可用
     * 
     * 规则：
     * - 设备没有任务记录 → 可用
     * - 设备最新任务状态为 COMPLETED/FAILED/CANCELLED/EXPIRED/IDLE → 可用
     * - 设备最新任务状态为 SCHEDULED/PENDING/RUNNING → 不可用
     * 
     * @param deviceId 设备ID
     * @return true=可用，false=不可用
     */
    public boolean isDeviceAvailable(String deviceId) {
        log.debug("检查设备可用性: deviceId={}", deviceId);
        
        // 查询设备的所有任务，按创建时间倒序（最新的在前）
        List<DeviceTask> tasks = deviceTaskRepository.findByDeviceIdOrderByCreatedTimeDesc(deviceId);
        
        if (tasks.isEmpty()) {
            log.debug("  设备无任务记录，可用");
            return true;
        }
        
        // 获取最新任务
        DeviceTask latestTask = tasks.get(0);
        DeviceTask.TaskStatus status = latestTask.getStatus();
        
        // 终态：可用
        if (status == DeviceTask.TaskStatus.COMPLETED ||
            status == DeviceTask.TaskStatus.FAILED ||
            status == DeviceTask.TaskStatus.CANCELLED ||
            status == DeviceTask.TaskStatus.EXPIRED ||
            status == DeviceTask.TaskStatus.IDLE) {
            log.debug("  设备最新任务已完成/失败/取消/过期/空闲，可用: status={}", status);
            return true;
        }
        
        // 进行中状态：不可用
        log.debug("  设备最新任务进行中，不可用: status={}, taskId={}, taskType={}", 
            status, latestTask.getTaskId(), latestTask.getTaskType());
        return false;
    }

    /**
     * 获取设备当前状态
     * 
     * @param deviceId 设备ID
     * @return 设备状态信息
     */
    public DeviceAvailabilityInfo getDeviceAvailability(String deviceId) {
        log.info("查询设备可用性: deviceId={}", deviceId);
        
        List<DeviceTask> tasks = deviceTaskRepository.findByDeviceIdOrderByCreatedTimeDesc(deviceId);
        
        DeviceAvailabilityInfo info = new DeviceAvailabilityInfo();
        info.setDeviceId(deviceId);
        
        if (tasks.isEmpty()) {
            info.setAvailable(true);
            info.setStatus("IDLE");
            info.setMessage("设备空闲，无任务记录");
            return info;
        }
        
        DeviceTask latestTask = tasks.get(0);
        info.setLatestTaskId(latestTask.getTaskId());
        info.setLatestTaskType(latestTask.getTaskType() != null ? latestTask.getTaskType().name() : null);
        info.setLatestTaskStatus(latestTask.getStatus().name());
        info.setBatchId(latestTask.getBatchId());
        info.setScheduledTime(latestTask.getScheduledTime());
        info.setCreatedTime(latestTask.getCreatedTime());
        info.setUpdatedTime(latestTask.getUpdatedTime());
        info.setErrorMessage(latestTask.getErrorMessage());
        
        DeviceTask.TaskStatus status = latestTask.getStatus();
        boolean available = (status == DeviceTask.TaskStatus.COMPLETED ||
                            status == DeviceTask.TaskStatus.FAILED ||
                            status == DeviceTask.TaskStatus.CANCELLED ||
                            status == DeviceTask.TaskStatus.EXPIRED ||
                            status == DeviceTask.TaskStatus.IDLE);
        
        info.setAvailable(available);
        info.setStatus(status.name());
        
        if (available) {
            info.setMessage("Device is available");
        } else {
            info.setMessage(String.format("Device is busy: %s task in progress", 
                latestTask.getTaskType() != null ? latestTask.getTaskType().name() : "UNKNOWN"));
        }
        
        log.info("  设备可用性: available={}, status={}, taskType={}", 
            info.isAvailable(), info.getStatus(), info.getLatestTaskType());
        
        return info;
    }

    /**
     * 批量检查设备可用性
     * 
     * @param deviceIds 设备ID列表
     * @return 设备可用性信息列表
     */
    public List<DeviceAvailabilityInfo> batchCheckDeviceAvailability(List<String> deviceIds) {
        log.info("批量检查设备可用性: deviceCount={}", deviceIds.size());
        
        return deviceIds.stream()
                .map(this::getDeviceAvailability)
                .collect(Collectors.toList());
    }

    /**
     * 验证设备列表可用性，如果有不可用的设备则抛出异常
     * 
     * ⚠️ 已弃用：建议使用 validateDevicesAvailableStrict() 进行完整的三项验证
     * 
     * @param deviceIds 设备ID列表
     * @param operationType 操作类型（用于错误提示）
     * @throws IllegalStateException 如果有设备不可用
     * @deprecated 使用 validateDevicesAvailableStrict() 替代
     */
    @Deprecated
    public void validateDevicesAvailable(List<String> deviceIds, String operationType) {
        log.info("验证设备可用性: deviceCount={}, operationType={}", deviceIds.size(), operationType);
        
        List<DeviceAvailabilityInfo> unavailableDevices = deviceIds.stream()
                .map(this::getDeviceAvailability)
                .filter(info -> !info.isAvailable())
                .collect(Collectors.toList());
        
        if (!unavailableDevices.isEmpty()) {
            String errorMsg = String.format(
                "以下设备当前不可用，无法执行%s操作：\n%s",
                operationType,
                unavailableDevices.stream()
                    .map(info -> String.format("  - %s: %s (任务类型: %s, 状态: %s)", 
                        info.getDeviceId(), 
                        info.getMessage(),
                        info.getLatestTaskType(),
                        info.getStatus()))
                    .collect(Collectors.joining("\n"))
            );
            
            log.warn("设备可用性验证失败: {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        log.info("  ✅ 所有设备可用");
    }

    /**
     * 验证设备列表可用性（严格模式）
     * 
     * ✅ 严格模式 Pre-validation 包含三项检查：
     * 1. SFTP 服务器验证（如果提供）
     * 2. 设备存在性验证
     * 3. 设备可用性验证（无冲突任务）
     * 
     * 特点：
     * - 一个设备失败，整个验证失败（不创建任何资源）
     * - 遇到第一个失败立即停止，不继续验证后续设备
     * - 返回设备信息缓存（避免重复查询）
     * 
     * @param deviceIds 设备ID列表
     * @param taskType 任务类型（用于检查设备可用性）
     * @param sftpServerName SFTP服务器名称（可选）
     * @param operationType 操作类型（用于错误提示）
     * @return 设备信息缓存 Map（deviceId → DevicePhysicalInfo）
     * @throws RuntimeException 如果有设备验证失败
     */
    public Map<String, devicemaintenance.integration.NeMgrIntegrationService.DevicePhysicalInfo> 
    validateDevicesAvailableStrict(
            List<String> deviceIds, 
            DeviceTask.TaskType taskType,
            String sftpServerName,
            String operationType) {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 [严格模式] 验证设备可用性");
        log.info("  设备数量: {}", deviceIds.size());
        log.info("  操作类型: {}", operationType);
        log.info("  任务类型: {}", taskType);
        if (sftpServerName != null) {
            log.info("  SFTP服务器: {}", sftpServerName);
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // 调用 PreValidationService 进行严格验证
        PreValidationService.BatchValidationResult result = 
            preValidationService.validateBatchStrict(deviceIds, taskType, sftpServerName);
        
        if (!result.isAllValid()) {
            // 验证失败，抛出异常
            PreValidationService.DeviceValidationResult firstFailure = result.getFirstFailure();
            String errorMsg = String.format(
                "%s操作 Pre-validation 失败：设备 %s - %s",
                operationType,
                firstFailure.getDeviceId(),
                firstFailure.getErrorMessage()
            );
            
            log.error("❌ {}", errorMsg);
            log.error("⚠️  批次创建失败，不创建任何 Batch 和 DeviceTask");
            throw new RuntimeException(errorMsg);
        }
        
        log.info("✅ 严格模式验证通过，所有设备可用");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // 返回设备信息缓存
        return result.getDeviceInfoMap();
    }

    /**
     * 设备可用性信息
     */
    public static class DeviceAvailabilityInfo {
        private String deviceId;
        private boolean available;
        private String status;  // 当前状态
        private String message;  // 描述信息
        
        // 最新任务信息
        private String latestTaskId;
        private String latestTaskType;
        private String latestTaskStatus;
        private String batchId;
        private Long scheduledTime;  // 定时执行时间戳（毫秒）
        private java.time.LocalDateTime createdTime;
        private java.time.LocalDateTime updatedTime;
        private String errorMessage;

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getLatestTaskId() { return latestTaskId; }
        public void setLatestTaskId(String latestTaskId) { this.latestTaskId = latestTaskId; }
        
        public String getLatestTaskType() { return latestTaskType; }
        public void setLatestTaskType(String latestTaskType) { this.latestTaskType = latestTaskType; }
        
        public String getLatestTaskStatus() { return latestTaskStatus; }
        public void setLatestTaskStatus(String latestTaskStatus) { this.latestTaskStatus = latestTaskStatus; }
        
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        
        public Long getScheduledTime() { return scheduledTime; }
        public void setScheduledTime(Long scheduledTime) { this.scheduledTime = scheduledTime; }
        
        public java.time.LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(java.time.LocalDateTime createdTime) { this.createdTime = createdTime; }
        
        public java.time.LocalDateTime getUpdatedTime() { return updatedTime; }
        public void setUpdatedTime(java.time.LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}

