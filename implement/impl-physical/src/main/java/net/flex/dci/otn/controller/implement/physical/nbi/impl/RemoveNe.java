/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;


import static net.flex.dci.otc.common.util.YangConstants.SITE_TOPO_KEY;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.InternalLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Component
@Scope("prototype")
public class RemoveNe extends BaseImpl {

//    private final ZkResourceLock locker = new ZkResourceLock();

    @Autowired
    private MultipleTransaction mongoTransaction;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private SiteNodeDao siteNodeDao;


    public RemoveNe() {

    }

    @Override
    public RemoveNeOutput removeNe(RemoveNeInput input) throws CommonException {
//        changedObject = new ChangedObject();
        log.debug("start to remove the ne,the remove ne input is :{}", input);
        String nodeId = input.getNodeId().getValue();
        checkParam(nodeId);
        Node configNode = phyNodeDao.getConfigPhyNodeById(nodeId);
        String dispString = "";
        ZkResourceLock locker = new ZkResourceLock();
        try {

            lockResource(nodeId, locker);
            dispString = configNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
            if (dispString == null || dispString.isEmpty()) {
                dispString = configNode.getNodeId().getValue();
            }
            taskInfoMessage.setResourceId(nodeId);
            taskInfoMessage.setResourceName(dispString);

            Node siteNode = updateRefSite(input.getNodeId());
            List<String> refPhyLinkIds = removeInternalPhyLink(configNode);
            removeNeWithRelative(nodeId, siteNode, refPhyLinkIds);
//            changedObject.addRemovedPhyNode(input.getNodeId().getValue());
//            mongoTransaction.save(changedObject);

            log.debug("remove Ne success");
            logMessage(BroadCastConstant.REMOVE_NE, dispString, BLANK);
        } catch (Exception e) {
            log.error("remove Ne error", e);
            logMessage(BroadCastConstant.REMOVE_NE, dispString, e.getMessage());
        } finally {
            locker.unlock();
        }
        return new RemoveNeOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .build();
    }

    private void removeNeWithRelative(String nodeId, Node siteNode, List<String> refPhyLinkIds) {
        log.debug("remove nodeId:{},update siteNode:{} and remove refPhyLinkIds:{}", nodeId,
                siteNode, refPhyLinkIds);
        ChangedObject changedObject = new ChangedObject();
        changedObject.addRemovedPhyNode(nodeId);
        if (Objects.nonNull(siteNode)) {
            changedObject.addChangedSiteNode(siteNode);
        }
        if (!CollectionUtils.isEmpty(refPhyLinkIds)) {
            changedObject.setRemovedPhyLinkIdList(new HashSet<>(refPhyLinkIds));
        }
        mongoTransaction.save(changedObject);
    }

    private void lockResource(String nodeId, ZkResourceLock locker) throws CommonException {
        locker.addResource(nodeId);
        locker.getLock();
    }

    private List<String> removeInternalPhyLink(Node node) {

        List<InternalLinks> ilList = node.getAugmentation(Node1.class).getPhysical()
                .getInternalLinks();
        if (ilList == null || ilList.isEmpty()) {
            return new ArrayList<>();
        }
        return ilList.stream().filter(internalLink -> Objects.nonNull(internalLink.getLinkRef()))
                .map(InternalLinkAttributes::getLinkRef)
                .collect(
                        Collectors.toList());
    }

    private void checkParam(String nodeId) {
        log.debug("check remove ne :{} parameters", nodeId);
        Node phyNode = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (phyNode == null) {
            throw new CommonException(
                    CommonExceptionType.INVALID_PARAMETER,
                    "cannot find node " + nodeId);
        }
        Physical nodePhysical = phyNode.getAugmentation(Node1.class).getPhysical();
        if (nodePhysical.getIp() != null || (nodePhysical.getIp() != null && !nodePhysical.getIp()
                .isEmpty())) {
            throw new CommonException(
                    CommonExceptionType.INVALID_PARAMETER, "please remove IP at first");
        }

        checkClientLink(phyNode);
    }

    private Node updateRefSite(NodeId nodeId) {
        String siteId = PhysicalNodeIdNamingRule.getSiteId(nodeId.getValue());
        log.debug("start to remove ne from siteNode{}", siteId);
        Node siteNode = siteNodeDao.getSiteNodeById(siteId);
        if (siteNode == null) {
            return null;
        }
        //update supporting node
        NodeBuilder siteNodeBuilder = new NodeBuilder(siteNode);
        if (siteNodeBuilder.getSupportingNode() != null) {
            List<SupportingNode> supportNodes = siteNodeBuilder.getSupportingNode().stream()
                    .filter(sn -> !sn.getNodeRef().getValue().equals(nodeId.getValue())).collect(
                            Collectors.toList());
            siteNodeBuilder.setSupportingNode(supportNodes);
        }

        Site siteAttr = siteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite();
        SupportingRack relatedRack = getSupportingRack(nodeId, siteAttr);
        if (relatedRack != null) {
            List<SupportingRack> updateRacks = updateSiteNodeRack(siteNode, relatedRack,
                    nodeId.getValue());
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 node1 = siteNode.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
            Site sitePhysical = node1.getSite();
            siteNodeBuilder.addAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder(
                            node1)
                            .setSite(new SiteBuilder(sitePhysical).setSupportingRack(
                                            updateRacks)
                                    .build()).build());
        }

        log.debug("remove ne from siteNode{}", nodeId.getValue());
        return siteNodeBuilder.build();
    }

    private List<SupportingRack> updateSiteNodeRack(Node siteNode, SupportingRack relatedRack,
            String nodeId) {
        log.debug(
                "update site Node relatedRack,siteNode:{} and relatedRack:{} remove supporting node is:{}",
                siteNode.getNodeId(), relatedRack.getRackId(), nodeId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 node1 = siteNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        Site sitePhysical = node1.getSite();
        List<SupportingRack> spRacksExcludeRelatedRack = sitePhysical.getSupportingRack().stream()
                .filter(supportingRack -> !supportingRack.getRackId()
                        .equals(relatedRack.getRackId())).collect(
                        Collectors.toList());

        if (!relatedRack.getSupportingNe().isEmpty()) {
            log.debug("update relatedRack:{} when remove neId:{}", relatedRack.getRackId(), nodeId);
            SupportingRackBuilder srBuilder = new SupportingRackBuilder(relatedRack);
            List<SupportingNe> filteredSupportingNes = srBuilder.getSupportingNe().stream()
                    .filter(supportingNe -> !supportingNe.getNodeRef().getValue().equals(nodeId))
                    .collect(
                            Collectors.toList());
            srBuilder.setSupportingNe(filteredSupportingNes);
            spRacksExcludeRelatedRack.add(srBuilder.build());
        }

        return spRacksExcludeRelatedRack;
    }

    private Node delRackFromSite(Node siteNode, SupportingRackKey rackKey) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 node1 =
                siteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        List<SupportingRack> supportingRacks = node1.getSite().getSupportingRack();
        supportingRacks.removeIf(sr -> sr.getKey().equals(rackKey));
        NodeBuilder siteNodeBuilder = new NodeBuilder(siteNode).addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder(
                        node1)
                        .setSite(new SiteBuilder(node1.getSite()).setSupportingRack(supportingRacks)
                                .build()).build());
        return siteNodeBuilder.build();
    }

    private Node delSupportingNodeFromSite(Node siteNode, SupportingRackKey rackKey,
            NodeId supportingNodeId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 node1 =
                siteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        List<SupportingRack> supportingRacks = node1.getSite().getSupportingRack();
        Optional<SupportingRack> optSupportingRack = supportingRacks.stream()
                .filter(sr -> sr.getKey().equals(rackKey)).findAny();
        if (optSupportingRack.isPresent()) {
            List<SupportingNe> supportingNes = optSupportingRack.get().getSupportingNe();
            supportingNes.removeIf(sn -> sn.getKey().getNodeRef().equals(supportingNodeId));
            SupportingRackBuilder srBuilder = new SupportingRackBuilder(
                    optSupportingRack.get()).setSupportingNe(supportingNes);
            supportingRacks.removeIf(sr -> sr.getKey().equals(rackKey));
            supportingRacks.add(srBuilder.build());
        }
        NodeBuilder siteNodeBuilder = new NodeBuilder(siteNode).addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder(
                        node1)
                        .setSite(new SiteBuilder(node1.getSite()).setSupportingRack(supportingRacks)
                                .build()).build());
        return siteNodeBuilder.build();
    }

    private SupportingRack getSupportingRack(NodeId nodeId, Site siteAttr) {
        log.debug("get supporting rack for the nodeId:{}", nodeId);
//        for (SupportingRack sr : siteAttr.getSupportingRack()) {
//            if (sr.getSupportingNe() != null) {
//                for (SupportingNe supportingNe : sr.getSupportingNe()) {
//                    if (supportingNe.getNodeRef().getValue().equals(nodeId.getValue())) {
//                        relatedRack = sr;
//                        break;
//                    }
//                }
//            }
//        }
//        return relatedRack;
        String neId = nodeId.getValue();
        return siteAttr.getSupportingRack().stream()
                .filter(sr -> sr.getSupportingNe() != null
                        && sr.getSupportingNe().stream()
                        .anyMatch(ne -> neId.equals(ne.getNodeRef().getValue())))
                .findFirst()
                .orElse(null);
    }

    private void checkClientLink(Node phyNode) throws CommonException {
        log.debug("check the node is used on link,the nodeId is:{}",
                phyNode.getNodeId().getValue());
        //siteLink or tunnel
//    checkIsAnyPhyLinkBasedOnTheNode(phyNode);
        checkIsAnySiteLinkBasedOnTheNode(phyNode);
        checkIsAnyTunnelBasedOnTheNode(phyNode);
    }

    private void checkIsAnyPhyLinkBasedOnTheNode(Node phyNode) {
        String nodeId = phyNode.getNodeId().getValue();
        String neName = phyNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        log.debug("check the node have any phy link, phy node is:{}", nodeId);
        PhyLinkDao phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);

        List<Link> phyLinks = phyLinkDao.getPhyLinksByNodeId(nodeId);
        if (phyLinks != null && !phyLinks.isEmpty()) {
            String name = phyLinks.get(0).getAugmentation(Link1.class).getPhysical()
                    .getFriendlyName();
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "This Node %s [%s] is on SiteLink,please remove phyLink resource (%s) first",
                    neName, nodeId, name));
        }
    }

    private void checkIsAnyTunnelBasedOnTheNode(Node phyNode) {
        String nodeId = phyNode.getNodeId().getValue();
        log.debug("check the node have any site link,phy node is:{}", nodeId);
        String neName = phyNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        PhyLinkDao phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);

        List<Link> phyLinks = phyLinkDao.getPhyLinksByNodeId(nodeId);
        List<String> siteLinkIds = phyLinks.stream()
                .collect(ArrayList::new, (list, link) -> list.addAll(
                                link.getAugmentation(Link1.class).getPhysical().getSupportedLink().stream()
                                        .filter(sl -> sl.getTopologyRef().getValue().equals(SITE_TOPO_KEY))
                                        .map(sl -> sl.getLinkRef().getValue()).collect(
                                                Collectors.toList())),
                        ArrayList::addAll);
        if (!siteLinkIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "This Node %s [%s] is on SiteLink,please remove siteLink resource first",
                    neName, nodeId));
        }

    }

    private void checkIsAnySiteLinkBasedOnTheNode(Node phyNode) {
        String nodeId = phyNode.getNodeId().getValue();
        String neName = phyNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        log.debug("check the node have any tunnel,phy node is:{}", nodeId);

        PhyLinkDao phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);
        OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

        List<Link> phyLinks = phyLinkDao.getPhyLinksByNodeId(nodeId);
        List<String> phyLinkIds = phyLinks.stream().map(phyLink -> phyLink.getLinkId().getValue())
                .collect(Collectors.toList());
        List<Link> ochLinks = ochLinkDao.getAllOchLinksUnderPhyLinkIds(phyLinkIds);
        List<String> supportedTunnelIds = ochLinks.stream()
                .collect(ArrayList::new, (list, ochLink) -> list.addAll(ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                        .getSupportedTunnel().stream()
                        .map(tunnel -> tunnel.getTunnelRef().getValue())
                        .collect(Collectors.toList())), ArrayList::addAll);
        if (!supportedTunnelIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "This Node:%s [%s] is on Tunnel,please remove Tunnel resource first",
                    neName, nodeId));
        }
    }


}
