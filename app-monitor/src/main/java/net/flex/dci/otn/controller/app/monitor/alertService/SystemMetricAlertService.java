package net.flex.dci.otn.controller.app.monitor.alertService;

import static net.flex.dci.otn.controller.app.monitor.util.AppMonitorUtils.generateAlarmIdForUsage;
import static net.flex.dci.otn.controller.app.monitor.util.Constants.DISK_USAGE_THRESHOLD;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.zk.common.entity.DiskUsage;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zk.common.entity.SystemPerformance;
import net.flex.dci.otn.controller.app.monitor.service.IAlarmService;
import net.flex.dci.otn.controller.app.monitor.util.AppMonitorUtils;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import org.springframework.stereotype.Service;

/**
 *
 * 2025/12/7
 *
 * @author musa
 * @version 1.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemMetricAlertService {


    private final IAlarmService alarmService;
    private final Cache<String, Long> alertCoolDownCache = Caffeine.newBuilder().maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private final String DISK_USAGE_ALERT_TEMPLATE = "Server Disk Usage Alert - Host: %s, Mount: %s, Utilization: %.1f%% (Capacity: %.1fGB)";

    private final String DISK_USAGE_RESOLVE_TEMPLATE = "Server Disk Usage Normal - Host: %s, Mount: %s, Utilization: %.1f%% (Capacity: %.1fGB)";

    public void checkSystemMetricAlert(InstanceDetails instanceDetails) {
        log.debug("check disk usage details for the instance Details:{}", instanceDetails.getId());
        SystemPerformance currentSystemPerformance = instanceDetails.getSystemPerformance();
        if (currentSystemPerformance == null) {
            log.debug("current instance :{} have no systemc performance metric data, skip",
                    instanceDetails.getId());
            return;
        }
        try {
            log.debug("check system metric alert,the instance details is:{}", instanceDetails);
            //current check the disk usage

            checkCurrentDiskUsage(currentSystemPerformance);
        } catch (Exception ex) {
            log.error("failed to check instance:{} refer system metrics ", instanceDetails.getId(),
                    ex);
        }
    }

    private void checkCurrentDiskUsage(SystemPerformance currentSystemPerformance) {
        log.debug("check current disk usage ");
        String hostname = currentSystemPerformance.getHostname() == null ? "unknown-host"
                : currentSystemPerformance.getHostname();
        Map<String, DiskUsage> diskUsage = currentSystemPerformance.getDiskUsageMap();
        for (Map.Entry<String, DiskUsage> diskUsageEntry : diskUsage.entrySet()) {
            String mountPoint = diskUsageEntry.getKey();
            DiskUsage diskInfo = diskUsageEntry.getValue();
            if (diskInfo.getUsagePercent() > DISK_USAGE_THRESHOLD) {
                handleDiskAlert(hostname, mountPoint, diskInfo);
            } else {
                handleDiskRecovery(hostname, mountPoint, diskInfo);
            }
        }
    }

    private void handleDiskAlert(String hostname, String mountPoint, DiskUsage diskInfo) {
        log.info("handle disk alert the mount point:{} and disk info:{}", mountPoint, diskInfo);
        String resourceIdentifier = AppMonitorUtils.diskMountKeyGenerate(hostname, mountPoint);
        if (alertCoolDownCache.getIfPresent(resourceIdentifier) == null) {
            double totalSpace = diskInfo.getTotalSpace() / (1024.0 * 1024.0 * 1024.0);
            String alertMessage = String.format(DISK_USAGE_ALERT_TEMPLATE, hostname,
                    diskInfo.getMountPoint(), diskInfo.getUsagePercent(), totalSpace);
            log.warn("current alertMessage:{}", alertMessage);
            //todo:notify the alert and generate the alarm
            AlarmRecord diskUsageAlarm = alarmService.generateDiskUsageAlarm(hostname, mountPoint,
                    diskInfo);
            log.debug("diskUsage Alarm:{}", diskUsageAlarm);
            notifyAlarm(alertMessage, BroadCastConstant.DISK_USAGE_ALARM, true);
            alertCoolDownCache.put(resourceIdentifier, System.currentTimeMillis());
        } else {
            log.debug("Alert cooling down, skipping: {}:{}, usage: {}", hostname, mountPoint,
                    diskInfo.getUsagePercent());
        }
    }


    /**
     * clear cool down record
     *
     * @param hostname
     * @param mountPoint
     */
    private void handleDiskRecovery(String hostname, String mountPoint, DiskUsage diskUsage) {
        log.info("current time hostname:{} mountPoint:{} usage normalized", hostname, mountPoint);
        String resourceIdentifier = AppMonitorUtils.diskMountKeyGenerate(hostname, mountPoint);
        String alarmId = generateAlarmIdForUsage(hostname, mountPoint);
        AlarmRecord alarmRecord = alarmService.getCurrentAlarmById(alarmId);
        if (alarmRecord != null) {
            alarmService.clearDiskUsageAlarm(hostname, mountPoint, diskUsage);
            sendRecoveryNotification(hostname, mountPoint, diskUsage);
            alertCoolDownCache.invalidate(resourceIdentifier);
            log.info("Disk usage recovery handled for {}", resourceIdentifier);
        } else {
            log.info("No active alarm found for {}, nothing to clear", resourceIdentifier);
        }
        log.debug("Disk usage normalized, cache records cleared:{}", resourceIdentifier);
        //clear alarm for the disk usage

    }

    private void sendRecoveryNotification(String hostname, String mountPoint, DiskUsage diskUsage) {
        double totalSpace = diskUsage.getTotalSpace() / (1024.0 * 1024.0 * 1024.0);
        String recoveryMessage = String.format(
                DISK_USAGE_RESOLVE_TEMPLATE,
                // 这里可以从 resolvedAlarm 中提取主机名和挂载点，或直接传递参数
                hostname,
                mountPoint,
                diskUsage.getUsagePercent(),
                totalSpace
        );
        //send to notification
        notifyAlarm(recoveryMessage, BroadCastConstant.DISK_USAGE_ALARM_RESOLVE, false);
        log.info(recoveryMessage);
    }


    /**
     *
     * @param alertMessage
     */
    private void notifyAlarm(String alertMessage, String title, Boolean error) {
        log.debug("notify alarm disk usage alarm");
        BroadcastMessage broadcastMessage = BroadcastMessage.builder().message(alertMessage)
                .error(error).title(title).build();
        BroadcastMessager.publishKafkaMessage(broadcastMessage);
    }
}
