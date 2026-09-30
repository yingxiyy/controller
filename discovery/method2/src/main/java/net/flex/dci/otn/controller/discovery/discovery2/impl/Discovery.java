/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.discovery2.impl;

import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.discovery.common.MyExecutor;
import net.flex.dci.otn.controller.discovery.common.impl.InternalLinkMachine;
import net.flex.dci.otn.controller.discovery.common.impl.OcmGroupMachine;
import net.flex.dci.otn.controller.discovery.common.impl.TunnelMachine;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

@Slf4j
public class Discovery {

    private DiscoveryResource resource;
    private ZkResourceLock locker;

    public void start(List<String> resourceId) {
        log.debug("start discovery based on nodeList: {}", resourceId);

        //由于耗时, 把这个同步命令改为异步

        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        executor.lazyDo(new Runnable() {
            @Override
            public void run() {
                lazy(resourceId);
            }
        });
    }

    private void lazy(List<String> nodeIdList) {
        try {
            resource = new DiscoveryResource();
            resource.prepareResource(nodeIdList);

            locker = lockResouce();


            /**
             * 基于siteLink检查其上面的所有OCH/Tunnel，
             * 如果检查的时候对应的资源没有Impl，其下层的各种link都会从waitingList中移除
             * 检查规则是 cfg/op 的
             *  tpId一样， op的adminStatus=adminUp
             *  xcId一样， op的adminStatus=adminUp
             *     ===需要加一个internalLink的比对判断
             *  最终用Machine，完成link、tp,xc,eq，itlLink, ne资源的adminStatus状态修改
             */
            Iterator<Link> iter = resource.getWaitingSiteLinkList().values().iterator();
            while (iter.hasNext()) {
                Link link = iter.next();
                try {
                    checkSiteLinkResource(link);
                } catch (CommonException e) {
                    iter.remove(); //this site link discovery fail, remove it;
                    log.debug("siteLink discovery fail , remove it {}",
                            link.getLinkId().getValue());
                }
            }

            for (Node opNode : resource.getOpNodeMap().values()) {
                String nodeId = opNode.getNodeId().getValue();
                Node cfgNode = resource.getCfgNodeMap().get(nodeId);

                if (cfgNode == null) {
                    log.debug("cannot find config node with opNodeID {}", nodeId);
                    continue;
                }
                Node updatedCfgNode = new InternalLinkMachine(cfgNode, opNode).start();
                updatedCfgNode = new OcmGroupMachine(updatedCfgNode, opNode).start();
                resource.getCfgNodeMap().put(cfgNode.getNodeId().getValue(), updatedCfgNode);
            }

            ChangedObject changedObject = markChangedData();
            for (String ochLinkId : resource.getWaitingTunnelList().keySet()) {
                for (Tunnel tunnel : resource.getWaitingTunnelList().get(ochLinkId)) {
                    new TunnelMachine(changedObject, tunnel).markImpl();
                }
            }

            MultipleTransaction multipleTransaction = SpringBeanFinder.getBean(
                    MultipleTransaction.class);
            multipleTransaction.save(changedObject);
            BroadcastMessager.publishKafkaMessage(
                    BroadcastMessage.builder()
                            .title("network discovery")
                            .message("network discovery is done")
                            .error(false)
                            .build());
        } catch (Exception e) {
            log.error("discovery error", e);
        } finally {
            locker.unlock();
        }
    }

    private ZkResourceLock lockResouce() {
        ZkResourceLock locker = new ZkResourceLock();
        locker.addResource(TopoNameConstants.Site_Topo_Key); //锁全网
        for (String nodeId : resource.getCfgNodeMap().keySet()) {
            locker.addResource(nodeId);
        }
        for (String linkId : resource.getWaitingSiteLinkList().keySet()) {
            locker.addResource(linkId);
        }
        for (String linkId : resource.getWaitingOchLinkList().keySet()) {
            locker.addResource(linkId);
        }
        for (String linkId : resource.getWaitingTunnelList().keySet()) {
            locker.addResource(linkId);
        }
        locker.getLock();
        return locker;
    }

    private ChangedObject markChangedData() {
        ChangedObject changedObject = new ChangedObject();
        for (Node cfgNode : resource.getCfgNodeMap().values()) {
            changedObject.addChangedPhyNode(cfgNode);
        }
        for (String ochLinkId : resource.getWaitingTunnelList().keySet()) {
            for (Tunnel tunnel : resource.getWaitingTunnelList().get(ochLinkId)) {
                changedObject.addChangedTunnel(tunnel);
            }
        }
        for (String siteLinkId : resource.getWaitingOchLinkList().keySet()) {
            for (Link ochLink : resource.getWaitingOchLinkList().get(siteLinkId)) {
                changedObject.addChangedOchLink(ochLink);
            }
        }
        for (Link siteLink : resource.getWaitingSiteLinkList().values()) {
            changedObject.addChangedOchLink(siteLink);
        }
        return changedObject;
    }

    private void checkSiteLinkResource(Link siteLink) throws CommonException {
        String siteLinkId = siteLink.getLinkId().getValue();
        log.debug("discovery site link {}", siteLinkId);

        try {
            new SiteLinkDiscovery(resource).start(siteLink);
        } catch (CommonException e) {
            log.debug("site link hasn't pass discovery {}", siteLinkId);
            log.debug("remove related ochLink, tunnel");
            for (Link ochLink : resource.getWaitingOchLinkList().get(siteLinkId)) {
                String ochLinkId = ochLink.getLinkId().getValue();
                resource.getWaitingTunnelList().remove(ochLinkId);
            }
            resource.getWaitingOchLinkList().remove(siteLinkId);
            throw e;
        }

        Iterator<Link> iter = resource.getWaitingOchLinkList().get(siteLinkId).iterator();
        while (iter.hasNext()) {
            Link ochLink = iter.next();
            try {
                checkOchLinkResource(ochLink);
            } catch (CommonException e) {
                log.debug("ochLink discovery fail , remove it {}", ochLink.getLinkId().getValue());
                iter.remove(); //this och link discovery fail, remove it;
            }
        }
    }

    private void checkOchLinkResource(Link ochLink) throws CommonException {
        String ochLinkId = ochLink.getLinkId().getValue();
        log.debug("discovery och link {}", ochLinkId);

        try {
            new OchLinkDiscovery(resource).start(ochLink);
        } catch (CommonException e) {
            log.debug("och link hasn't pass discovery {}", ochLinkId);
            log.debug("remove related  tunnel");
            resource.getWaitingTunnelList().remove(ochLinkId);
            throw e;
        }

        Iterator<Tunnel> iter = resource.getWaitingTunnelList().get(ochLinkId).iterator();
        while (iter.hasNext()) {
            Tunnel tunnel = iter.next();
            try {
                checkTunnelResource(tunnel);
            } catch (CommonException e) {
                log.debug("tunnel discovery fail , remove it {}", tunnel.getTunnelId().getValue());
                iter.remove();
            }
        }

    }

    private void checkTunnelResource(Tunnel tunnel) throws CommonException {
        String tunnelId = tunnel.getTunnelId().getValue();
        log.debug("discovery tunnel {}", tunnelId);

        try {
            new TunnelDiscovery(resource).start(tunnel);
        } catch (CommonException e) {
            log.debug("tunnel hasn't pass discovery {}", tunnelId);
            throw e;
        }
    }
}
