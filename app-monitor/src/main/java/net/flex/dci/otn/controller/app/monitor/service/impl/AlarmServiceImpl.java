/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.service.impl;

import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.COLON;
import static net.flex.dci.otn.controller.app.monitor.util.AppMonitorUtils.generateAlarmIdForUsage;
import static net.flex.dci.otn.controller.app.monitor.util.Constants.DISK_USAGE;
import static net.flex.dci.otn.controller.app.monitor.util.Constants.DISK_USAGE_THRESHOLD;
import static net.flex.dci.otn.controller.app.monitor.util.Constants.SERVER_NTP_OOS_GROUP;
import static net.flex.dci.otn.controller.app.monitor.util.Constants.SERVER_PREFIX;
import static net.flex.dci.otn.controller.app.monitor.util.Constants.SYSTEM_MONITOR_DISK_USAGE;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.model.alarm.AppAlarm;
import net.flex.dci.otc.zk.common.entity.DiskUsage;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zk.common.entity.InstanceInfo;
import net.flex.dci.otn.controller.app.monitor.service.IAlarmService;
import net.flex.dci.otn.controller.app.monitor.util.MainCache;
import net.flex.dci.otn.controller.tools.kafka.service.DCIAppAlarmMessager;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements IAlarmService {

    private final AlarmDaoService alarmDaoService;


    @Override
    public void generateAlarm(AlarmRecord alarmRecord) {
//    alarmOdlRpc.generateAlarm(alarmRecord);
        AppAlarm appAlarm = convert2Alarm(alarmRecord);
//        alarmRpc.generateAlarm(alarm);
        DCIAppAlarmMessager.generateAlarm(appAlarm);
//        if (!MainCache.alarmMap.containsKey(alarmRecord.getAlarmId())) {
//            MainCache.alarmMap.put(alarmRecord.getAlarmId(), alarmRecord);
//        }
    }


    @Override
    public void clearAlarm(String alarmId) {
//        if (MainCache.alarmMap.containsKey(alarmId)) {
//            alarmOdlRpc.clearAlarm(alarmId);
//            alarmRpc.clearAlarm(alarmId);
        log.debug("clear alarm the alarm id:{}", alarmId);
        AppAlarm appAlarm = AppAlarm.builder().alarmId(alarmId).isClear(true).build();
        DCIAppAlarmMessager.clearAlarm(appAlarm, appAlarm.getAlarmId());
//            MainCache.alarmMap.remove(alarmId);
//        }
    }

    @Override
    public void clearAlarm(List<String> alarmIds) {
        alarmIds.forEach(this::clearAlarm);
    }

    /**
     * 从数据库查询所有alarmType=APP的当前告警
     */
    @Override
    public List<AlarmRecord> getAllAppAlarm() throws CommonException {
        log.debug("retrieve all app alarm");
        return alarmDaoService.getAppAlarm();
    }

    @Override
    public String generateAlarmIdWhenIsolate(String module, String detailId) {
        return module + " isolation;" + detailId;
    }

    @Override
    public String generateAlarmIdWhenNtp(String module, String detailId) {
        return module + " Ntp UnSynchronized;" + detailId;
    }

    @Override
    public String generateAlarmIdWhenNtp(String serverId) {
        return "Server:" + serverId + " Ntp UnSynchronized;";
    }

    /**
     * App断连时产生告警
     */
    @Override
    public AlarmRecord buildAlarmWhenIsolate(String module, String detailId) {
        Long currentTimestamp = System.currentTimeMillis();
        AlarmRecord alarmRecord = new AlarmRecord();
        alarmRecord.setAlarmId(generateAlarmIdWhenIsolate(module, detailId));
        alarmRecord.setCreationTime(currentTimestamp);
        alarmRecord.setNmlReceivedTime(currentTimestamp);
        alarmRecord.setResourceRef(detailId);
        alarmRecord.setComponentRef(detailId);
        alarmRecord.setNmlKey(detailId);
        alarmRecord.setIp(getIp(detailId));
        String groupName = module + "_OOS";
        String alarmText =
                module + "_Connection_Failure;" + " APP-Monitor finds  out of service.";
        alarmRecord.setAlarmGroup(groupName);
        alarmRecord.setAlarmText(alarmText);
        alarmRecord.setSa(false);
        alarmRecord.setAlarmTypeId("APP");
        alarmRecord.setSeverity(AlarmSeverity.Major);
        return alarmRecord;
    }

    /**
     * APP的NTP同步有误时，产生告警
     */
    @Override
    public AlarmRecord buildAlarmWhenNtp(String module, String detailId) {
        AlarmRecord alarmRecord = new AlarmRecord();
        alarmRecord.setAlarmId(generateAlarmIdWhenNtp(module, detailId));
        alarmRecord.setCreationTime(System.currentTimeMillis());
        alarmRecord.setNmlReceivedTime(System.currentTimeMillis());
        alarmRecord.setResourceRef(detailId);
        alarmRecord.setComponentRef(detailId);
        alarmRecord.setNmlKey(detailId);
        alarmRecord.setIp(getIp(detailId));
        // 告警模块判断
        String alarmGroup = module + "_OOS";
        String alarmText = module + "'s Ntp Server Unsynchronized.";
        alarmRecord.setAlarmGroup(alarmGroup);
        alarmRecord.setAlarmText(alarmText);
        alarmRecord.setSa(false);
        alarmRecord.setAlarmTypeId("APP");
        alarmRecord.setSeverity(AlarmSeverity.Major);
        return alarmRecord;
    }

    @Override
    public AlarmRecord generateDiskUsageAlarm(String hostname, String mountPoint,
            DiskUsage diskInfo) {
        Long currentTimestamp = System.currentTimeMillis();
        AlarmRecord alarmRecord = new AlarmRecord();
        alarmRecord.setAlarmId(generateAlarmIdForUsage(hostname, mountPoint));
        alarmRecord.setCreationTime(currentTimestamp);
        alarmRecord.setNmlReceivedTime(currentTimestamp);
        String resourceIdentifier = hostname + COLON + mountPoint;

        alarmRecord.setResourceRef(resourceIdentifier);
        alarmRecord.setComponentRef(resourceIdentifier);
        alarmRecord.setNmlKey(resourceIdentifier);

        String alarmText = buildDiskAlarmText(hostname, mountPoint, diskInfo);

        alarmRecord.setAlarmGroup(SYSTEM_MONITOR_DISK_USAGE);
        alarmRecord.setAlarmText(alarmText);
        alarmRecord.setSa(false);
        alarmRecord.setAlarmTypeId(DISK_USAGE);
        alarmRecord.setSeverity(AlarmSeverity.Critical);
        generateAlarm(alarmRecord);
        return alarmRecord;
    }

    @Override
    public void clearDiskUsageAlarm(String hostname, String mountPoint, DiskUsage diskInfo) {
        log.info("clear disk usage alarm,hostname:{} mountPoint:{} diskInfo:{}", hostname,
                mountPoint, diskInfo);
        String alarmId = generateAlarmIdForUsage(hostname, mountPoint);
        log.info("current alarmId :{}", alarmId);
        clearAlarm(alarmId);
    }

    private String buildDiskAlarmText(String hostname, String mountPoint, DiskUsage diskInfo) {

        double totalGB = diskInfo.getTotalSpace() / (1024.0 * 1024.0 * 1024.0);
        double usedGB = diskInfo.getUsedSpace() / (1024.0 * 1024.0 * 1024.0);
        double freeGB = totalGB - usedGB;

        return String.format(
                "SERVER_Disk_Usage_Exceeded; " +
                        "Host: %s, " +
                        "Mount: %s, " +
                        "Current: %.1f%%, " +
                        "Threshold: %.1f%%, " +
                        "Used: %.1fGB/%.1fGB, " +
                        "Free: %.1fGB",
                // 告警开头
                hostname,
                mountPoint,
                diskInfo.getUsagePercent(),
                DISK_USAGE_THRESHOLD,
                usedGB,
                totalGB,
                freeGB
        );

    }


    @Override
    public void clearAppAlarm(InstanceDetails instanceDetail) {
        String alarmIsolateId = generateAlarmIdWhenIsolate(
                instanceDetail.getModule(), instanceDetail.getId());
        String alarmNtpErrorId = generateAlarmIdWhenNtp(
                instanceDetail.getModule(), instanceDetail.getId());
        clearAlarm(alarmIsolateId);
        clearAlarm(alarmNtpErrorId);
    }

    @Override
    public AlarmRecord getCurrentAlarmById(String alarmId) {
        log.debug("get current alarm by id:{}", alarmId);
        return alarmDaoService.getAlarm(alarmId);
    }

    @Override
    public AlarmRecord buildServerNtpAlarm(String serverId) {
        AlarmRecord alarmRecord = new AlarmRecord();

        alarmRecord.setAlarmId(generateAlarmIdWhenNtp(serverId));

        alarmRecord.setCreationTime(System.currentTimeMillis());
        alarmRecord.setNmlReceivedTime(System.currentTimeMillis());

        alarmRecord.setResourceRef(serverId);
        alarmRecord.setComponentRef(serverId);
        alarmRecord.setNmlKey(serverId);
        alarmRecord.setIp(serverId.replace(SERVER_PREFIX, ""));

        // 清晰标识服务器NTP异常，兼容原有分组命名规则
        alarmRecord.setAlarmGroup(SERVER_NTP_OOS_GROUP);

        String alarmText =
                "NTP SERVER UNSYNCHRONIZED;Server [" + serverId + "] NTP Server UnSynchronized.";
        alarmRecord.setAlarmText(alarmText);

        // 8. 其他固定字段：完全复用老方法的配置，确保兼容性
        alarmRecord.setSa(false);                  // 和老方法一致
        alarmRecord.setAlarmTypeId("SERVER");         // 保持原有类型（若需区分可改为"SERVER"，建议先兼容）
        alarmRecord.setSeverity(AlarmSeverity.Major);  // 告警级别和老方法一致

        return alarmRecord;
    }

    @Override
    public List<AlarmRecord> getAlarmByAlarmGroup(String alarmGroup) {
        return alarmDaoService.getAlarmByAlarmGroup(alarmGroup);
    }

    private AppAlarm convert2Alarm(AlarmRecord alarmRecord) {
        return AppAlarm.builder()
                .index(alarmRecord.getIndex())
                .alarmGroup(alarmRecord.getAlarmGroup())
                .alarmId(alarmRecord.getAlarmId())
                .alarmText(alarmRecord.getAlarmText())
                .alarmTypeId(alarmRecord.getAlarmTypeId())
                .creationTime(alarmRecord.getCreationTime())
                .nmlKey(alarmRecord.getNmlKey())
                .nmlKeyName(alarmRecord.getNmlKeyName())
                .nmlReceivedTime(alarmRecord.getNmlReceivedTime())
                .componentRef(alarmRecord.getComponentRef())
                .resourceRef(alarmRecord.getResourceRef())
                .ip(alarmRecord.getIp())
                .neId(alarmRecord.getNeId())
                .sa(alarmRecord.getSa())
                .severity(alarmRecord.getSeverity())
                .isClear(false)
                .build();
    }

    private String getIp(String detailId) {
        String[] array = detailId.split("_");
        if (array.length == 2) {
            return array[1];
        }
        return "";
    }

    /**
     * generate alarm for the app instance info
     *
     * @param instanceInfo
     */
    public AlarmRecord generateAlarm(InstanceInfo instanceInfo) {
        log.debug("generate alarm about instance {}", instanceInfo);
        String alarmId = generateAlarmIdWhenIsolate(instanceInfo.getData().getModule(),
                instanceInfo.getId());
        if (MainCache.alarmMap.containsKey(alarmId)) {
            // 从告警中取死亡时间
            AlarmRecord alarmRecord = MainCache.alarmMap.get(alarmId);
            return alarmRecord;
        }
        return null;
    }
}