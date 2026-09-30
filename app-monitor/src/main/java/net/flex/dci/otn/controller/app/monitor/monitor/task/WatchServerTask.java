/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.monitor.task;

import static net.flex.dci.otn.controller.app.monitor.util.Constants.SERVER_NTP_OOS_GROUP;
import static net.flex.dci.otn.controller.app.monitor.util.Constants.SERVER_PREFIX;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otn.controller.app.monitor.leader.LeaderElector;
import net.flex.dci.otn.controller.app.monitor.service.IAppMonitorService;
import net.flex.dci.otn.controller.app.monitor.service.impl.AlarmServiceImpl;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定时扫描各服务节点
 */
@Slf4j
@Component
public class WatchServerTask implements Runnable {

    @Autowired
    AlarmServiceImpl alarmService;

    @Autowired
    IAppMonitorService appMonitorService;

    @Autowired
    private LeaderElector leaderElector;

    private static final Pattern MODULE_NTP_ALARM_PATTERN = Pattern.compile(
            ".+ Ntp Unsynchronized;.+");

    @Override
    public void run() {
        if (!leaderElector.isLeaderShip()) {
            log.debug("not leader, skip task");
            return;
        }
        log.debug("WatchServerTask run...");
        appMonitorService.checkStartupModuleStatus();
        appNtpStateCheck();
        clearNotExitsAppOOSAlarm();
    }

    private void clearNotExitsAppOOSAlarm() {
        log.info("start to clear not exits app out of service alarm");
        List<InstanceDetails> details = DciInstancesUtils.getAllConfigInstances();
        Set<String> existKeys = details.stream()
                .map(InstanceDetails::getId)
                .collect(Collectors.toSet());
        List<AlarmRecord> alarmRecords = alarmService.getAllAppAlarm();
        List<AlarmRecord> toRemoveAlarms = alarmRecords.stream()
                .filter(a -> !existKeys.contains(a.getNmlKeyName()))
                .collect(Collectors.toList());
        for (AlarmRecord removeRecord : toRemoveAlarms) {
            alarmService.clearAlarm(removeRecord.getAlarmId());
        }

    }


    private void appNtpStateCheck() {
        try {
            log.info("start to check current app ntp state");
            List<InstanceDetails> details = Optional.ofNullable(
                            DciInstancesUtils.getAllStateInstances())
                    .orElse(Collections.emptyList()).stream()
                    .filter(detail -> !"data-analyzer".equals(detail.getModule()))
                    .collect(Collectors.toList());

            if (details.isEmpty()) {
                log.info("no valid instance details, skip ntp check");
                return;
            }
            //assemble
            Map<String, Boolean> serverNtpStateMap = details.stream()
                    .collect(Collectors.groupingBy(this::getStandardServerId, Collectors.reducing(
                            true,
                            detail -> Optional.ofNullable(detail.getTimeInfo().getNtpSynchronized())
                                    .orElse(true),
                            (a, b) -> a && b
                    )));
            Set<String> existingNtpAlarmIds = getExistingNtpAlarms();
            serverNtpStateMap.forEach((standardServerId, isNtpSynced) -> {
                String serverNtpAlarmId = alarmService.generateAlarmIdWhenNtp(standardServerId);
                handleNtpAlarm(standardServerId, serverNtpAlarmId, isNtpSynced,
                        existingNtpAlarmIds);
            });
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to check ntp", ex);
        }
//        Set<String> existingAlarmIds = alarmService.getAllAppAlarm().stream()
//                .map(AlarmRecord::getAlarmId)
//                .collect(Collectors.toSet());
//        details.stream()
//                .filter(detail -> !detail.getModule().equals("data-analyzer"))
//                .map(detail -> {
//                    String module = detail.getModule();
//                    String detailModuleId = detail.getId();
//                    Boolean ntpSynced = detail.getTimeInfo().getNtpSynchronized();
//                    String alarmId = alarmService.generateAlarmIdWhenNtp(module, detailModuleId);
//                    String wrongAlarmId = null;
//                    if (alarmId.contains(":")) {
//                        wrongAlarmId = alarmService.generateAlarmIdWhenNtp(
//                                alarmId.split(":")[0],
//                                alarmId.split(":")[1]);
//                    }
//                    return new AppNtpAlarm(module, detailModuleId, alarmId, wrongAlarmId,
//                            ntpSynced);
//                })
//                .filter(entry -> entry.ntpSynced != null)
//                .forEach(entry -> handleNtpAlarm(entry,
//                        existingAlarmIds));
    }

    private Set<String> getExistingNtpAlarms() {
        Set<String> oldModuleNtpAlarmIds = clearOldModuleNtpAlarms();
        //server ntp alarm
        List<AlarmRecord> serverNtpAlarms = alarmService.getAlarmByAlarmGroup(SERVER_NTP_OOS_GROUP);
        Set<String> serverNtpAlarmIds = serverNtpAlarms.stream().map(AlarmRecord::getAlarmId)
                .collect(
                        Collectors.toSet());
        log.info("server ntp alarm ids:{}", serverNtpAlarms);
        Set<String> alarmIds = new HashSet<>();
        alarmIds.addAll(oldModuleNtpAlarmIds);
        alarmIds.addAll(serverNtpAlarmIds);
        return alarmIds;
    }


    private Set<String> clearOldModuleNtpAlarms() {
        log.debug("clear old module ntp Alarms");
        List<AlarmRecord> appAlarms = alarmService.getAllAppAlarm();
        List<AlarmRecord> oldModuleNtpAlarms = appAlarms.stream()
                .filter(alarm -> MODULE_NTP_ALARM_PATTERN.matcher(alarm.getAlarmId()).matches())
                .collect(Collectors.toList());
        for (AlarmRecord oldNtpAlarm : oldModuleNtpAlarms) {
            log.info("clear old module ntp alarm:{}", oldNtpAlarm);
            alarmService.clearAlarm(oldNtpAlarm.getAlarmId());
        }
        return appAlarms.stream()
                .map(AlarmRecord::getAlarmId)
                .filter(alarmId -> !MODULE_NTP_ALARM_PATTERN.matcher(alarmId).matches())
                .collect(Collectors.toSet());
    }

    private String getStandardServerId(InstanceDetails instanceDetails) {
        return SERVER_PREFIX + instanceDetails.getMyIp();
    }

    private void handleNtpAlarm(String serverId, String serverNtpAlarmId,
            Boolean isNtpSynced, Set<String> existingAlarmIds) {
        log.info("handle the ntp alarm id is:{}", serverNtpAlarmId);
        if (isNtpSynced) {
            if (existingAlarmIds.contains(serverNtpAlarmId)) {
                log.info("server [{}]ntp synchronized，clear alarm：{}", serverId, serverNtpAlarmId);
                alarmService.clearAlarm(serverNtpAlarmId);
            }
        } else {
            if (!existingAlarmIds.contains(serverNtpAlarmId)) {
                log.info("server [{}] ntp is not synchronized,raise alarm:{}", serverId,
                        serverNtpAlarmId);
                AlarmRecord alarmRecord = alarmService.buildServerNtpAlarm(serverId);
                alarmService.generateAlarm(alarmRecord);
            }
        }
    }

    private void handleNtpAlarm(AppNtpAlarm ntpAlarm, Set<String> existingAlarmIds) {
        boolean ntpSynced = ntpAlarm.getNtpSynced();
        String alarmId = ntpAlarm.getAlarmId();
        if (ntpSynced) {
            String wrongAlarmId = ntpAlarm.getWrongAlarmId();
            if (existingAlarmIds.contains(alarmId)) {
                alarmService.clearAlarm(alarmId);
            }
            if (wrongAlarmId != null && existingAlarmIds.contains(wrongAlarmId)) {
                alarmService.clearAlarm(wrongAlarmId);
            }
        } else {
            if (!existingAlarmIds.contains(alarmId)) {
                AlarmRecord alarmRecord = alarmService.buildAlarmWhenNtp(ntpAlarm.module,
                        ntpAlarm.detailModuleId);
                alarmService.generateAlarm(alarmRecord);
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class AppNtpAlarm {

        private String module;

        private String detailModuleId;

        private String alarmId;

        private String wrongAlarmId;

        private Boolean ntpSynced;
    }
}