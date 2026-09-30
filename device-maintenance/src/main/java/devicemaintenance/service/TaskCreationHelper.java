package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 任务创建辅助类
 * 提供统一的定时任务处理逻辑
 */
@Component
@Slf4j
public class TaskCreationHelper {

    /**
     * 配置任务的定时执行逻辑
     * 
     * @param task 任务实体
     * @param scheduledTime 计划执行时间戳（可为null，表示立即执行，单位：毫秒）
     */
    public void configureTaskScheduling(DeviceTask task, Long scheduledTime) {
        if (scheduledTime != null) {
            // 定时任务：设置计划时间和 SCHEDULED 状态
            task.setScheduledTime(scheduledTime);
            task.setStatus(DeviceTask.TaskStatus.SCHEDULED);
            log.info("创建定时任务: taskId={}, scheduledTime={}", task.getTaskId(), scheduledTime);
        } else {
            // 立即执行：设置 PENDING 状态
            task.setStatus(DeviceTask.TaskStatus.PENDING);
            task.setStartedTime(LocalDateTime.now());
            log.info("创建立即执行任务: taskId={}", task.getTaskId());
        }
    }

    /**
     * 生成任务ID
     */
    public String generateTaskId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 统一设置设备任务的所有设备信息属性
     * 确保所有任务类型的设备信息一致性
     * 
     * @param task 任务实体
     * @param deviceInfo 设备物理信息
     */
    public void setDeviceInfoToTask(DeviceTask task, devicemaintenance.integration.NeMgrIntegrationService.DevicePhysicalInfo deviceInfo) {
        if (deviceInfo == null) {
            log.warn("  ⚠️ deviceInfo is null, skipping device info setting for task: {}", task.getTaskId());
            return;
        }
        
        // 设备名称
        if (deviceInfo.getFriendlyName() != null) {
            task.setDeviceName(deviceInfo.getFriendlyName());
            log.debug("  ✓ Device name set: {}", deviceInfo.getFriendlyName());
        }
        
        // 设备IP
        if (deviceInfo.getIp() != null) {
            task.setDeviceIp(deviceInfo.getIp());
            log.debug("  ✓ Device IP set: {}", deviceInfo.getIp());
        }
        
        // 厂商类型
        if (deviceInfo.getVendorType() != null) {
            task.setVendorType(deviceInfo.getVendorType());
            log.debug("  ✓ Vendor type set: {}", deviceInfo.getVendorType());
        }
        
        // 厂商名称
        if (deviceInfo.getVendorName() != null) {
            task.setVendorName(deviceInfo.getVendorName());
            log.debug("  ✓ Vendor name set: {}", deviceInfo.getVendorName());
        }
        
        // 当前软件版本
        if (deviceInfo.getSoftwareVersion() != null) {
            task.setCurrentVersion(deviceInfo.getSoftwareVersion());
            if (task.getPreviousVersion() == null) {
                task.setPreviousVersion(deviceInfo.getSoftwareVersion());
            }
            log.debug("  ✓ Current version set: {}", deviceInfo.getSoftwareVersion());
        }
        
        log.info("  ✅ Device info set: name={}, ip={}, vendor={}/{}, currentVersion={}", 
            deviceInfo.getFriendlyName(), 
            deviceInfo.getIp(), 
            deviceInfo.getVendorType(), 
            deviceInfo.getVendorName(),
            deviceInfo.getSoftwareVersion());
    }

    /**
     * 判断任务是否应该立即执行
     * 
     * @param task 任务实体
     * @return true 表示应该立即执行，false 表示等待调度
     */
    public boolean shouldExecuteImmediately(DeviceTask task) {
        return task.getStatus() != DeviceTask.TaskStatus.SCHEDULED;
    }
}
