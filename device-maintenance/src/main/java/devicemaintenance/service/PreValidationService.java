package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.integration.NeMgrIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Pre-validation 服务
 * 提供统一的设备、SFTP等资源的预验证逻辑
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PreValidationService {

    private final NeMgrIntegrationService neMgrIntegrationService;
    private final DeviceTaskUniquenessService uniquenessService;

    /**
     * 批量验证结果
     */
    public static class BatchValidationResult {
        private boolean allValid;
        private List<DeviceValidationResult> deviceResults;
        private String sftpServerName;
        private Map<String, NeMgrIntegrationService.DevicePhysicalInfo> deviceInfoMap;

        public BatchValidationResult() {
            this.deviceResults = new ArrayList<>();
            this.deviceInfoMap = new HashMap<>();
        }

        public boolean isAllValid() { return allValid; }
        public void setAllValid(boolean allValid) { this.allValid = allValid; }
        
        public List<DeviceValidationResult> getDeviceResults() { return deviceResults; }
        public void setDeviceResults(List<DeviceValidationResult> deviceResults) { 
            this.deviceResults = deviceResults; 
        }
        
        public String getSftpServerName() { return sftpServerName; }
        public void setSftpServerName(String sftpServerName) { this.sftpServerName = sftpServerName; }
        
        public Map<String, NeMgrIntegrationService.DevicePhysicalInfo> getDeviceInfoMap() { 
            return deviceInfoMap; 
        }
        public void setDeviceInfoMap(Map<String, NeMgrIntegrationService.DevicePhysicalInfo> deviceInfoMap) { 
            this.deviceInfoMap = deviceInfoMap; 
        }

        /**
         * 获取第一个失败的设备结果
         */
        public DeviceValidationResult getFirstFailure() {
            return deviceResults.stream()
                .filter(r -> !r.isValid())
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * 单个设备验证结果
     */
    public static class DeviceValidationResult {
        private String deviceId;
        private boolean valid;
        private String errorMessage;
        private NeMgrIntegrationService.DevicePhysicalInfo deviceInfo;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public NeMgrIntegrationService.DevicePhysicalInfo getDeviceInfo() { return deviceInfo; }
        public void setDeviceInfo(NeMgrIntegrationService.DevicePhysicalInfo deviceInfo) { 
            this.deviceInfo = deviceInfo; 
        }
    }

    /**
     * 验证单个设备（不抛异常，返回验证结果）
     * 
     * @param deviceId 设备ID
     * @param taskType 任务类型（用于检查设备可用性）
     * @return 验证结果
     */
    public DeviceValidationResult validateDevice(String deviceId, DeviceTask.TaskType taskType) {
        DeviceValidationResult result = new DeviceValidationResult();
        result.setDeviceId(deviceId);

        try {
            // 1. 验证设备是否存在，并获取设备信息
            NeMgrIntegrationService.DevicePhysicalInfo deviceInfo = 
                neMgrIntegrationService.getDeviceInfoByNodeId(deviceId);
            result.setDeviceInfo(deviceInfo);
            
            log.debug("  ✓ 设备存在: {} ({})", deviceInfo.getFriendlyName(), deviceId);

            // 2. 验证设备是否空闲（检查是否有冲突的任务）
            try {
                uniquenessService.checkAndGetAvailableTask(deviceId, taskType, null);
                log.debug("  ✓ 设备空闲: {}", deviceId);
            } catch (RuntimeException e) {
                // 设备正忙
                result.setValid(false);
                result.setErrorMessage("Device busy: " + e.getMessage());
                return result;
            }

            // 验证通过
            result.setValid(true);
            return result;

        } catch (Exception e) {
            // 设备不存在或其他错误
            result.setValid(false);
            result.setErrorMessage("Device validation failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * 验证SFTP服务器（不抛异常，返回验证结果）
     * 
     * @param sftpServerName SFTP服务器名称
     * @return true=有效, false=无效
     */
    public boolean validateSftpServer(String sftpServerName) {
        if (sftpServerName == null || sftpServerName.trim().isEmpty()) {
            return true;  // 不验证空值
        }

        try {
            neMgrIntegrationService.getSftpServerById(sftpServerName);
            log.debug("  ✓ SFTP服务器存在: {}", sftpServerName);
            return true;
        } catch (Exception e) {
            log.warn("  ✗ SFTP服务器验证失败: {} - {}", sftpServerName, e.getMessage());
            return false;
        }
    }

    /**
     * 批量验证设备（严格模式）
     * 遇到第一个失败立即停止，不继续验证后续设备
     * 
     * @param deviceIds 设备ID列表
     * @param taskType 任务类型
     * @param sftpServerName SFTP服务器名称（可选）
     * @return 批量验证结果
     */
    public BatchValidationResult validateBatchStrict(
            List<String> deviceIds, 
            DeviceTask.TaskType taskType,
            String sftpServerName) {
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📋 批量 Pre-validation 开始（严格模式）");
        log.info("  设备数量: {}", deviceIds.size());
        log.info("  任务类型: {}", taskType);
        if (sftpServerName != null) {
            log.info("  SFTP服务器: {}", sftpServerName);
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        BatchValidationResult batchResult = new BatchValidationResult();
        batchResult.setSftpServerName(sftpServerName);

        // 1. ✅ 验证SFTP服务器（如果提供）
        if (sftpServerName != null && !sftpServerName.trim().isEmpty()) {
            if (!validateSftpServer(sftpServerName)) {
                log.error("❌ SFTP服务器验证失败: {}", sftpServerName);
                batchResult.setAllValid(false);
                
                // 所有设备都标记为失败（因为SFTP失败）
                for (String deviceId : deviceIds) {
                    DeviceValidationResult deviceResult = new DeviceValidationResult();
                    deviceResult.setDeviceId(deviceId);
                    deviceResult.setValid(false);
                    deviceResult.setErrorMessage("SFTP server validation failed: " + sftpServerName);
                    batchResult.getDeviceResults().add(deviceResult);
                }
                
                return batchResult;
            }
            log.info("✅ SFTP服务器验证通过");
        }

        // 2. ✅ 逐个验证设备（遇到失败立即停止）
        log.info("开始验证设备...");
        for (int i = 0; i < deviceIds.size(); i++) {
            String deviceId = deviceIds.get(i);
            log.info("  [{}/{}] 验证设备: {}", i + 1, deviceIds.size(), deviceId);

            DeviceValidationResult deviceResult = validateDevice(deviceId, taskType);
            batchResult.getDeviceResults().add(deviceResult);

            if (!deviceResult.isValid()) {
                // ❌ 验证失败，立即停止
                log.error("❌ 设备验证失败: {} - {}", deviceId, deviceResult.getErrorMessage());
                log.error("⚠️  停止验证，剩余 {} 个设备不再检查", deviceIds.size() - i - 1);
                batchResult.setAllValid(false);
                
                // 标记剩余设备为"未验证"
                for (int j = i + 1; j < deviceIds.size(); j++) {
                DeviceValidationResult skippedResult = new DeviceValidationResult();
                skippedResult.setDeviceId(deviceIds.get(j));
                skippedResult.setValid(false);
                skippedResult.setErrorMessage("Pre-validation stopped: previous device failed");
                batchResult.getDeviceResults().add(skippedResult);
                }
                
                return batchResult;
            }

            // ✅ 验证通过，保存设备信息
            if (deviceResult.getDeviceInfo() != null) {
                batchResult.getDeviceInfoMap().put(deviceId, deviceResult.getDeviceInfo());
            }
            log.info("  ✓ 设备验证通过: {}", deviceId);
        }

        // 3. ✅ 全部验证通过
        batchResult.setAllValid(true);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ 批量 Pre-validation 全部通过");
        log.info("  验证通过设备数: {}", deviceIds.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return batchResult;
    }

    /**
     * 验证单个设备和SFTP（快捷方法，抛异常）
     * 用于单设备操作
     * 
     * @param deviceId 设备ID
     * @param taskType 任务类型
     * @param sftpServerName SFTP服务器名称（可选）
     * @return 设备信息
     * @throws RuntimeException 验证失败时抛出
     */
    public NeMgrIntegrationService.DevicePhysicalInfo validateSingleDeviceOrThrow(
            String deviceId,
            DeviceTask.TaskType taskType,
            String sftpServerName) {
        
        log.info("🔍 [Pre-validation] 验证设备和SFTP服务器...");

        try {
            // 1. 验证SFTP服务器（如果提供）
            if (sftpServerName != null && !sftpServerName.trim().isEmpty()) {
                if (!validateSftpServer(sftpServerName)) {
                    throw new RuntimeException("SFTP server validation failed: " + sftpServerName);
                }
                log.info("  ✓ SFTP服务器验证通过: {}", sftpServerName);
            }

            // 2. 验证设备
            DeviceValidationResult deviceResult = validateDevice(deviceId, taskType);
            if (!deviceResult.isValid()) {
                throw new RuntimeException(deviceResult.getErrorMessage());
            }

            log.info("  ✓ 设备验证通过: {} ({})", 
                deviceResult.getDeviceInfo().getFriendlyName(), deviceId);

            return deviceResult.getDeviceInfo();

        } catch (Exception e) {
            log.error("❌ [Pre-validation failed] {}", e.getMessage());
            throw new RuntimeException("Pre-validation failed: " + e.getMessage(), e);
        }
    }
}

