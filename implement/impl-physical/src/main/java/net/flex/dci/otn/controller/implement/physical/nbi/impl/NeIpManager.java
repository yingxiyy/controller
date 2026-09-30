/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import net.flex.dci.otn.db.jpa.service.dao.ApsSwitchLogDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.CommonAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NeIpManager extends BaseImpl {

    private final PhyNodeDao phyNodeDao;

    private final NeManagerRpc neManagerRpc;

    private final PhyLinkDao phyLinkDao;

    private final AlarmDaoService alarmDao;

    private final ApsSwitchLogDaoService switchLogDao;

    /**
     * remove Ip as class name
     *
     * @param input
     * @return
     */
    public RemoveIpOutput removeNeIp(RemoveIpInput input) throws CommonException {
        String nodeId = input.getNodeId().getValue();
        log.debug("start remove ip for ne {}", nodeId);
        ZkResourceLock locker = new ZkResourceLock();
        try {
            locker.addResource(nodeId);
            locker.getLock();
            validateRemoveIp(nodeId, input.isForce() != null && input.isForce());
//            Node cfgNode = check(nodeId);
            removeIp(nodeId);
        } catch (Exception e) {
            log.error("failed remove ip for ne:{} reason is:{}", nodeId, e.getMessage(), e);
            throw e;
        } finally {
            locker.unlock();
        }
        return new RemoveIpOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .build();
    }

    /**
     * validate remove ip for nodeId
     *
     * @param nodeId
     * @param isForce
     */
    private void validateRemoveIp(String nodeId, boolean isForce) {
        log.debug("validate node:{} before remove ip", nodeId);
        boolean existed = phyNodeDao.existsCfgNode(nodeId);
        if (!existed) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find node " + nodeId);
        }
        String phyNeFriendlyName = phyNodeDao.getFriendlyName(nodeId);
        if (!StringUtils.hasLength(phyNeFriendlyName)) {
            phyNeFriendlyName = nodeId;
        }
        boolean supervised = phyNodeDao.isCurrentPhyNodeSupervised(nodeId);
        log.info("current nodeId:{} is supervisied :{}", nodeId, supervised);
        if (supervised) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "cannot remove current Ne:%s(%s) Ip because current Ne is Supervised",
                    phyNeFriendlyName, nodeId));
        }
        if (isForce) {
            return;
        }

        log.debug("find out the node related link resource {}", nodeId);
        List<Link> refPhyLinks = phyLinkDao.getPhyLinksByNodeId(nodeId);
        if (CollectionUtils.isEmpty(refPhyLinks)) {
            log.debug("current ref physical links is empty for node id:{}", nodeId);
            return;
        }
        List<Link> implPhyLinks = refPhyLinks.stream()
                .filter(phyLink -> {
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical physical = phyLink.getAugmentation(
                            Link1.class).getPhysical();
                    return physical.getImplementState() != ImplementState.Plan
                            && physical.getImplementState() != ImplementState.Allocate;
                })
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(implPhyLinks)) {
            List<String> implPhyLinkIds = implPhyLinks.stream()
                    .map(link -> link.getLinkId().getValue()).collect(
                            Collectors.toList());
            List<String> linkNames = implPhyLinks.stream().map(link -> link.getAugmentation(
                            Link1.class).getPhysical())
                    .map(CommonAttributes::getFriendlyName)
                    .collect(Collectors.toList());
            int maxShow = 3;
            String linkNameStr = linkNames.stream()
                    .limit(maxShow)
                    .collect(Collectors.joining(", "))
                    + (linkNames.size() > maxShow ? ", ..." : "");
            log.warn("the node {}, {}, related link {} still working in non-allocate status",
                    nodeId, phyNeFriendlyName, implPhyLinkIds);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "the node %s still has implemented link, such as %s", phyNeFriendlyName,
                    linkNameStr));
        }

    }


    public void removeIp(String neId) throws CommonException {
        log.debug("start removeIp node:{} ip", neId);
        Node ne = phyNodeDao.getConfigPhyNodeById(neId);
        String friendlyName = ne.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        if (!StringUtils.hasText(friendlyName)) {
            friendlyName = neId;
        }
        taskInfoMessage.setResourceId(neId);
        try {
            removeNeFromAdapter(NodeId.getDefaultInstance(neId));
            updateConfigDB(ne);
            removeNeLatestAlarmAndApsLog(neId);
            logMessage(BroadCastConstant.REMOVE_IP, friendlyName, BLANK);
            log.debug("remove Ip for ne:{} name:{}  success done", neId, friendlyName);
        } catch (Exception e) {
            logMessage(BroadCastConstant.REMOVE_IP, friendlyName, e.getMessage());
            log.error("ne:{} name:{} remove IP error", neId, friendlyName, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "remove IP error",
                    e);
        }
    }

    private void removeNeLatestAlarmAndApsLog(String neId) {
        log.debug("remove current ne latest alarm neId:{}", neId);
        alarmDao.deleteAlarmByNeId(neId);
        switchLogDao.deleteApsSwitchLogByNeId(neId);
    }

    private void updateConfigDB(Node cfgNode) throws CommonException {
        PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
        phyNodeDao.removeNeIp(cfgNode.getNodeId().getValue());
        ChangedObject changedObject = new ChangedObject();

        Physical nodePhyAttr = cfgNode.getAugmentation(Node1.class).getPhysical();
        Node newNode = new NodeBuilder(cfgNode).addAugmentation(Node1.class,
                        new Node1Builder()
                                .setPhysical(new PhysicalBuilder(nodePhyAttr)
                                        .setIp(null)
                                        .setAdminState(AdminStatus.Unknown)
                                        .setOperationalState(OperStatus.Unknown)
                                        .setAlarmState(AlarmSeverity.Unknown)
                                        .setAlignmentStatus(AlignmentStatusType.Unknown)
                                        .setImplementState(ImplementState.Allocate)
                                        .build())
                                .build())
                .build();

        changedObject.addChangedPhyNode(newNode);
        MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
        mongoTransaction.save(changedObject);

    }

    private void removeNeFromAdapter(NodeId nodeId) throws CommonException {
        UnregisteNeOutput result = neManagerRpc.unregisteredNe(nodeId);
        if (result.getReturnCode() != RpcResultType.Success) {
            log.error("Failed to do unregiste ne {}", nodeId.getValue());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Failed to remove Ip [%s]", nodeId.getValue()));
        }
    }

    private Node check(String nodeId) throws CommonException {
        log.debug("check node in config db {}", nodeId);
        PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
        Node cfgNode = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (cfgNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find node " + nodeId);
        }

        checkResource(cfgNode);

        return cfgNode;
    }

    //
    //find out the node related phy link resource, and all of them should be allocated
    //
    private void checkResource(Node cfgNode) throws CommonException {
        String dispString = cfgNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        if (dispString == null || dispString.isEmpty()) {
            dispString = cfgNode.getNodeId().getValue();
        }

        String nodeId = cfgNode.getNodeId().getValue();
        log.debug("find out the node related link resource {}", nodeId);
        PhyLinkDao phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);
        List<Link> phyLinkList = phyLinkDao.getPhyLinksByNodeId(nodeId);
        for (Link link : phyLinkList) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical linkPhyAttr = link.getAugmentation(
                    Link1.class).getPhysical();
            if (linkPhyAttr.getImplementState() != ImplementState.Allocate &&
                    linkPhyAttr.getImplementState() != ImplementState.Plan) {
                log.warn("the node {}, {}, related link {} still working in non-allocate status",
                        nodeId, dispString, link.getLinkId());

//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
//                        "the node %s still has implemented link, such as %s", dispString,
//                        linkPhyAttr.getFriendlyName()));
            }
        }
    }
}
