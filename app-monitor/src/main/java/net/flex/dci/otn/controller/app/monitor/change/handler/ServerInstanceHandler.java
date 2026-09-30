/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.change.handler;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.zk.common.constants.DciClientConstants;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.core.handler.ZkEventHandler;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otn.controller.app.monitor.leader.LeaderElector;
import net.flex.dci.otn.controller.app.monitor.service.FullStackMonitorService;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ServerInstanceHandler implements ZkEventHandler {

    private final FullStackMonitorService fullStackMonitorService;

    private final LeaderElector leaderElector;

    @Override
    public void handle(Type type, InstanceDetails detail) {
        if (!leaderElector.isLeaderShip()) {
            log.info("not a leader,skip the zk event handler");
            return;
        }
        log.info("start to handle the type:{},instance details is:{}", type, detail);
        long now = System.currentTimeMillis();
        try {
            switch (type) {
                case NODE_ADDED:
                    nodeAdd(detail);
                    break;
                case NODE_REMOVED:
                    nodeDead(detail, now);
                    break;
                case NODE_UPDATED:
                    nodeDataUpdate(detail);
                    break;
            }
        } catch (Exception exception) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    exception.getMessage(), exception);
        }

    }


    @Override
    public List<String> supportModuleNames() {
        return Collections.singletonList(DciClientConstants.MODULE_ALL);
    }

    private void nodeDead(InstanceDetails detail, long now) throws Exception {
        log.debug("service instance dead for module :{},instance id is:{}", detail.getModule(),
                detail.getId());
        InstanceDetails updateInstanceDetail = detail;
        updateInstanceDetail.setLatestDeadTime(now);
        DciInstancesUtils.updateConfigInstance(updateInstanceDetail);
    }

    private void nodeAdd(InstanceDetails detail) throws Exception {
        log.debug("add service instance for module :{},instance id is:{}", detail.getModule(),
                detail.getId());
        InstanceDetails updateInstanceDetail = detail;
        int restartTimes = updateInstanceDetail.getRestartTimes() + 1;
        updateInstanceDetail.setRestartTimes(restartTimes);
        DciInstancesUtils.updateConfigInstance(updateInstanceDetail);
    }

    private void nodeDataUpdate(InstanceDetails detail) {
        log.debug("service instance data update module:{} instance id:{}", detail.getModule(),
                detail.getId());
        InstanceDetails updateInstanceDetail = detail;
        fullStackMonitorService.checkSystemMetric(updateInstanceDetail);
    }
}