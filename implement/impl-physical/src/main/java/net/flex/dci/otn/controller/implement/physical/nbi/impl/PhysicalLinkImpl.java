/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.ase.DummyOchLinkConstructor;
import net.flex.dci.otn.controller.implement.common.impl.*;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @version 1.0
 * @date 2021/11/9 15:06
 */
@Slf4j
public class PhysicalLinkImpl {

    private Link link;
    private String who;

    public PhysicalLinkImpl(String who, Link link) {
        this.who = who;
        this.link = link;
    }

    public void start() {
        Physical linkAttr = link.getAugmentation(Link1.class).getPhysical();
        if (AdminStatus.Up == linkAttr.getAdminState() || ImplementState.Implement == linkAttr.getImplementState())  {
            log.debug("start impl phyLink {} ({})", linkAttr.getFriendlyName(), link.getLinkId().getValue());
            changeAs(true);
        } else {
            log.debug("start deImpl phyLink {} ({})", linkAttr.getFriendlyName(), link.getLinkId().getValue());
            changeAs(false);
        }
    }

    private void changeAs(boolean isImpl) {
        String friendlyName = link.getAugmentation(Link1.class).getPhysical().getFriendlyName();
        LifeCycleSevice lifeService = new LifeCycleSevice();
        lifeService.logStartLinkImpl(link.getLinkId().getValue(),
                TaskInfoMessage.ResourceType.phyLink,
                friendlyName,
                isImpl ? TaskInfoMessage.ActionType.implement : TaskInfoMessage.ActionType.deimplement,
                who, null);

        CompletableFuture<Void> future = new CompletableFuture<>();
        ZkResourceLock locker = new ZkResourceLock();
        locker.addResource(link.getLinkId().getValue());
        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(link);
        new LinkImplementState(LinkImplementState.LinkType.PhyLink,
                link.getLinkId().getValue(),
                friendlyName,
                rInfo,
                (success, error) -> {
                    if (success) {
                        log.info("Action on Scan link successfully: {} {}", isImpl ? "Implement -> " : "Allocate -> ", friendlyName);

                        future.complete(null); // 标记成功
                    } else {
                        log.error("Action on Scan link failure: {} {}", isImpl ? "Implement -> " : "Allocate -> ", friendlyName, error);
                        future.completeExceptionally(error != null ? error : new RuntimeException("Action on Scan link fail " + friendlyName));
                    }
                })
                .setActionType(isImpl ? ImplActionType.Implement : ImplActionType.Deimplement)
            .changeAs(new ChangedObject(), lifeService);

        String msg = "";
        try {
            future.get();
        } catch (Exception e) {
            msg = String.format("Implement scan link error %s", e.getMessage());
            log.error(msg, e);
        }
    }
}
