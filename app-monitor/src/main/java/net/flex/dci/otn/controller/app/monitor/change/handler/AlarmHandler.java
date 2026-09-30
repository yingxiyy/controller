/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.change.handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.enums.HealthStatus;
import net.flex.dci.otc.zk.common.constants.DciClientConstants;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.core.handler.ZkEventHandler;
import net.flex.dci.otn.controller.app.monitor.health.ModuleHealthChecker;
import net.flex.dci.otn.controller.app.monitor.health.TaskDelayQueue;
import net.flex.dci.otn.controller.app.monitor.leader.LeaderElector;
import net.flex.dci.otn.controller.app.monitor.service.impl.AlarmServiceImpl;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmHandler implements ZkEventHandler {


    private final AlarmServiceImpl alarmService;

    private final ModuleHealthChecker moduleHealthChecker;

    private final TaskDelayQueue taskDelayQueue;

    private final LeaderElector leaderElector;

    @Value("${alarm.delay.seconds:30}")
    private int alarmDelaySeconds;

    private final Cache<String, Boolean> pendingAlertCache = Caffeine.newBuilder()
            .expireAfterWrite(alarmDelaySeconds + 10, TimeUnit.SECONDS) // 比告警延迟稍长
            .build();


    @Override
    public void handle(Type type, InstanceDetails detail) {
        if (!leaderElector.isLeaderShip()) {
            log.info("not a leader,skip the zk event handler");
            return;
        }
        try {

//            log.debug("conf namespace:[{}], detail namespace:[{}] ", namespace,
//                    detail.getNamespace());
//            if (namespace != null && detail.getNamespace() != null && namespace
//                    .equals(detail.getNamespace())) {
            String id = detail.getId();
            String module = detail.getModule();
            String alarmId = alarmService.generateAlarmIdWhenIsolate(module, id);
            // 告警事件判断
            switch (type) {
                case NODE_REMOVED:
                    log.info("node remove alarmRecord:\n" + detail);
//                    AlarmRecord alarmRecord = alarmService.buildAlarmWhenIsolate(module, id);
//                    alarmService.generateAlarm(alarmRecord);
                    handleNodeRemovedWithValidation(module, id, alarmId, detail);
                    break;
                case NODE_ADDED:
                    log.info("node recover, clear alarmRecord:\n" + detail);
                    alarmService.clearAlarm(alarmId);
                    String taskId = buildTaskId(module, id);
                    taskDelayQueue.cancel(taskId);
                    pendingAlertCache.invalidate(taskId);
                    break;
                default:
                    break;
            }
//            }
        } catch (Exception e) {
            log.error("execute zk alarm failed!", e);
        }
    }

    private void handleNodeRemovedWithValidation(String module, String id, String alarmId,
            InstanceDetails detail) {
        String taskId = buildTaskId(module, id);
        if (pendingAlertCache.getIfPresent(taskId) != null) {
            log.warn("the instance:{} isolation task is already existed,discard it and do nothing",
                    id);
            return;
        }
        Runnable alarmTask = () -> {
            log.warn(
                    "Instance failed to recover within grace period, triggering isolation alert. Module: [{}], Instance: [{}]",
                    module, id);
            AlarmRecord alarmRecord = alarmService.buildAlarmWhenIsolate(module, id);
            alarmService.generateAlarm(alarmRecord);
            pendingAlertCache.invalidate(taskId);
        };
        pendingAlertCache.put(taskId, Boolean.TRUE);
        taskDelayQueue.submit(alarmTask, alarmDelaySeconds, TimeUnit.SECONDS, taskId);
        log.debug(
                "already submit isolation alert.module:{} instance:{} taskId:{} will execute after {} s",
                detail.getModule(), id, taskId, alarmDelaySeconds);
        CompletableFuture.runAsync(() -> {
            HealthStatus healthStatus = moduleHealthChecker.checkInstanceHealthStatus(detail);
            if (healthStatus == HealthStatus.UP) {
                log.warn(
                        "health check pass! instance alive cancel delay alarm. module:{} instance:{}",
                        detail.getModule(), id);
                taskDelayQueue.cancel(taskId);
                pendingAlertCache.invalidate(taskId);
            } else {
                log.info(
                        "Health check confirms instance is unhealthy or unreachable. Module: [{}], Instance: [{}], Status: {}",
                        module, id, healthStatus);
            }
        }).exceptionally(e -> {
            log.error("health check exception the module is :[{}], instance:[{}]", module, id, e);
            return null;
        });

    }

    private String buildTaskId(String module, String id) {
        return "ALARM_" + module + "_" + id;
    }

    @Override
    public List<String> supportModuleNames() {
        return Collections.singletonList(DciClientConstants.MODULE_ALL);
    }
}