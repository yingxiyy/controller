/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.service.impl;

import static net.flex.dci.otn.controller.app.monitor.util.Constants.ADAPTER;
import static net.flex.dci.otn.controller.app.monitor.util.Constants.GATEWAY;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zk.common.entity.InstanceInfo;
import net.flex.dci.otc.zk.common.entity.TimeInfo;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otn.controller.app.monitor.retriever.AppInstanceRetriever;
import net.flex.dci.otn.controller.app.monitor.service.IAlarmService;
import net.flex.dci.otn.controller.app.monitor.service.IAppMonitorService;
import net.flex.dci.otn.controller.app.monitor.util.ConvertorUtils;
import net.flex.dci.otn.controller.app.monitor.util.SortUtils;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.GetSystemInfoOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.GetVersionOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.get.system.info.output.Server;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.pmc.rev200303.get.version.output.Apps;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2021/12/17 10:04
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AppMonitorServiceImpl implements IAppMonitorService {


    private final IAlarmService alarmService;

    private final AppInstanceRetriever appInstanceRetriever;

    @Override
    public String getSystemInfo() {
        log.debug("get all system info state");
        List<InstanceDetails> details = DciInstancesUtils.getAllStateInstances();
        GetSystemInfoOutputBuilder builder = new GetSystemInfoOutputBuilder();
        if (details != null && !details.isEmpty()) {
            checkAndAdjustTime(details);
            List<InstanceDetails> sortedDetails = SortUtils.sortInstance(details);
            List<Server> servers = ConvertorUtils.convertIns2ServerList(sortedDetails);

            builder.setServer(servers);
        }
        return SerializeUtil.serializeRpcOutput2Json(builder.build());
    }


    @Override
    public String getModuleVersion() throws CommonException {
        log.debug("get every module ");
        try {
            List<InstanceInfo> infos = appInstanceRetriever.getAllAppInstances();
            if (infos != null && !infos.isEmpty()) {
                infos = calculateCompatible(infos);
            }
            List<InstanceInfo> sortedInfos = SortUtils.sortInstanceInfo(infos);

            List<Apps> apps = ConvertorUtils.convertIns2ListApps(sortedInfos);

            GetVersionOutputBuilder builder = new GetVersionOutputBuilder();
            builder.setApps(apps);
            return SerializeUtil.serializeRpcOutput2Json(builder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    @Override
    public void removeModuleInstance(List<InstanceInfo> instanceInfos) {
        log.debug("remove module instance :{}", instanceInfos);
        List<InstanceDetails> instanceDetails = instanceInfos.stream()
                .map(InstanceInfo::getData).collect(
                        Collectors.toList());
        instanceDetails.forEach(instanceDetail -> {
            try {
                DciInstancesUtils.removeInstance(instanceDetail);
                alarmService.clearAppAlarm(instanceDetail);
            } catch (Exception e) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        e.getMessage(), e);
            }
        });
    }

    @Override
    public void checkStartupModuleStatus() {
        log.debug("check startup module status when current app monitor start");
        List<InstanceInfo> instanceInfos = appInstanceRetriever.getAllAppInstances();
        List<InstanceInfo> waitCalculateAppOutAlarmInstance = instanceInfos.stream()
                .filter(instanceInfo -> !instanceInfo.isAlive()).collect(Collectors.toList());
        List<AlarmRecord> appAlarms = alarmService.getAllAppAlarm();
        List<InstanceDetails> waitGenerateAppDownAlarmInstances = calculateAppDownInstance(
                appAlarms,
                waitCalculateAppOutAlarmInstance);
        waitGenerateAppDownAlarmInstances.forEach(instanceDetail -> {
            log.debug("generate app down alarm instance :{}", instanceDetail.getId());
            AlarmRecord alarmRecord = alarmService.buildAlarmWhenIsolate(instanceDetail.getModule(),
                    instanceDetail.getId());
            alarmService.generateAlarm(alarmRecord);
        });
    }

    private List<InstanceDetails> calculateAppDownInstance(List<AlarmRecord> appAlarms,
            List<InstanceInfo> waitCalculateAppOutAlarmInstance) {
        log.debug("calculate app down instance");
        List<String> downInstanceIds = appAlarms.stream().map(AlarmRecord::getNmlKeyName)
                .collect(Collectors.toList());
        Map<String, InstanceDetails> instanceMap = waitCalculateAppOutAlarmInstance.stream()
                .collect(HashMap::new,
                        (map, instance) -> map.put(instance.getData().getId(), instance.getData()),
                        HashMap::putAll);
        List<InstanceDetails> downInstanceDetails = instanceMap.entrySet().stream()
                .filter(entry -> !downInstanceIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return downInstanceDetails;
    }


    /**
     * calculate compatible for the adapter and controller
     *
     * @param infos
     */
    private List<InstanceInfo> calculateCompatible(List<InstanceInfo> infos) {
        Map<String, List<InstanceInfo>> map = infos.stream()
                .collect(Collectors.groupingBy(instanceInfo -> instanceInfo.getData().getModule()));
        List<InstanceInfo> controllerModule = map.get(GATEWAY);
        if (controllerModule == null || controllerModule.isEmpty()) {
            return infos;
        }
        //temp method for only one controller
        List<String> sbiVersions = controllerModule.stream()
                .flatMap(instanceInfo -> instanceInfo.getData().getSbiVersion().stream()).collect(
                        Collectors.toList());
        List<InstanceInfo> adapterModules = map.get(ADAPTER);
        if (adapterModules == null || adapterModules.isEmpty()) {
            return infos;
        }
        adapterModules.stream().forEach(instanceInfo -> {
            if (sbiVersions.contains(instanceInfo.getData().getNbiVersion())) {
                instanceInfo.setCompatible(true);
            } else {
                instanceInfo.setCompatible(false);
            }
        });
        return map.values().stream().flatMap(Collection::stream).collect(
                Collectors.toList());
    }

    private void checkAndAdjustTime(List<InstanceDetails> details) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'XXX";
        DateFormat sdf = new SimpleDateFormat(pattern);
        long max = -1;
        long min = -1;
        for (InstanceDetails detail : details) {
            TimeInfo timeInfo = detail.getTimeInfo();
            if (timeInfo.getNtpSynchronized() == null || timeInfo.getCurrentDataTime() == null) {
                continue;
            }
            if (!timeInfo.getNtpSynchronized()) {
                continue;
            }
            try {
                Date date = sdf.parse(timeInfo.getCurrentDataTime());
                long time = date.getTime();
                if (max == -1 || time > max) {
                    max = time;
                }
                if (min == -1 || time < min) {
                    min = time;
                }
            } catch (ParseException e) {
                log.error("parse time error:", e);
            }
        }
        if (max == -1 || min == -1) {
            return;
        }
        if (max - min > 10 * 1000) {
            return;
        }
        String oneTime = null;
        for (InstanceDetails detail : details) {
            TimeInfo timeInfo = detail.getTimeInfo();
            if (timeInfo.getNtpSynchronized() == null || timeInfo.getCurrentDataTime() == null) {
                continue;
            }
            if (!timeInfo.getNtpSynchronized()) {
                continue;
            }
            if (oneTime == null) {
                oneTime = timeInfo.getCurrentDataTime();
            } else {
                timeInfo.setCurrentDataTime(oneTime);
            }
        }
    }

}
