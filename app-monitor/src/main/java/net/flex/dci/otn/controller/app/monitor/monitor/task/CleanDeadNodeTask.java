/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.monitor.task;

import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zk.common.entity.InstanceInfo;
import net.flex.dci.otn.controller.app.monitor.leader.LeaderElector;
import net.flex.dci.otn.controller.app.monitor.retriever.AppInstanceRetriever;
import net.flex.dci.otn.controller.app.monitor.service.IAppMonitorService;
import net.flex.dci.otn.controller.app.monitor.service.impl.AlarmServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定时清除CONFIG下的死了的永久节点
 */
@Component
@Slf4j
public class CleanDeadNodeTask implements Runnable {

    private static final long sessionTimeout = 86400000;
    @Autowired
    AlarmServiceImpl alarmService;
    @Autowired
    IAppMonitorService appMonitorService;
    @Autowired
    private AppInstanceRetriever appInstanceRetriever;

    @Autowired
    private LeaderElector leaderElector;

    @SneakyThrows
    @Override
    public void run() {
        if (!leaderElector.isLeaderShip()) {
            log.debug("not leader, skip task");
            return;
        }
        log.info("CleanDeadNodeTask run...");
        List<InstanceInfo> instanceInfos = appInstanceRetriever.getAllAppInstances();
        List<InstanceInfo> wait2RemoveInstance = instanceInfos.stream()
                .filter(instanceInfo -> !instanceInfo.isAlive()).collect(Collectors.toList());
        List<InstanceInfo> removeList = getRemoveList(wait2RemoveInstance);
        if (!removeList.isEmpty()) {
            appMonitorService.removeModuleInstance(removeList);
        }
        log.info("end clean dead node task");
    }

    /**
     * get remove list
     *
     * @return
     */
    private List<InstanceInfo> getRemoveList(List<InstanceInfo> wait2RemoveInstance) {
        log.debug("start to get remove list from dead instance {}", wait2RemoveInstance);
        List<InstanceInfo> removeList = wait2RemoveInstance.stream()
                .filter(this::calculateRemovable)
                .collect(Collectors.toList());
        return removeList;
    }

    private Boolean calculateRemovable(InstanceInfo instanceInfo) {
        log.debug("judge the instance if or not do delete ,info :{}", instanceInfo);
        long now = System.currentTimeMillis();
        long latestDeadTime = instanceInfo.getData().getLatestDeadTime();
        return now - latestDeadTime > sessionTimeout;
//        Map<String, ServerInfo> deadServerMap = MainCache.deadServerMap;
//        String id = instanceInfo.getId();
//        if (!deadServerMap.containsKey(id)) {
//            ServerInfo serverInfo = new ServerInfo(id);
//            AlarmRecord alarmRecord = alarmService.generateAlarm(instanceInfo);
//            if (alarmRecord == null) {
//                serverInfo.setLastDeadTime(now);
//            } else {
//                serverInfo.setLastDeadTime(alarmRecord.getCreationTime().getTime());
//            }
//            deadServerMap.put(id, serverInfo);
//
//            return false;
//        }
//
//        return now - deadServerMap.get(id).getLastDeadTime() > sessionTimeout;

    }
}