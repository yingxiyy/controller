/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.OtRouteInfoNewOch;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RegSiteInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RoadmService {

    public static final List<Integer> AVAILABLE_DIMENSIONS = Collections.unmodifiableList(
            IntStream.rangeClosed(1, 8).boxed().collect(Collectors.toList())
    );
    @Autowired
    private TunnelUtils tunnelUtils;

    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private LinkRepo linkRepo;
    @Autowired
    private PhyLinkDao phyLinkDao;
    @Autowired
    private TpRepo tpRepo;
    @Autowired
    private OlsNodeService olsNodeService;
    @Autowired
    private NodeUtils nodeUtils;

    public OtRouteInfoNewOch allocateRoadm(String siteLinkId, String nextSiteLinkId, RegSiteInfo regSiteInfo, Map<String, Node> inMemoryNode,
                                           String frequencyString, Long cenFrequency) throws NeDesignerException {
        return allocateRoadm(siteLinkId, nextSiteLinkId, regSiteInfo.getSiteId(), inMemoryNode, frequencyString, cenFrequency);
    }

    public OtRouteInfoNewOch allocateRoadm(String siteLinkId, String nextSiteLinkId, String siteId, Map<String, Node> inMemoryNode,
                                           String frequencyString, Long cenFrequency) throws NeDesignerException {
        // ROADM allocation only needs the shared site id between two adjacent siteLinks.
        String srcSiteId = tunnelUtils.resolvePeerSiteId(siteLinkId, siteId);
        String destSiteId = tunnelUtils.resolvePeerSiteId(nextSiteLinkId, siteId);
        List<String> siteLinkIds = Arrays.asList(siteLinkId, nextSiteLinkId);
        List<Link> wssLinks = phyLinkDao.filterAllWssLink(siteLinkIds, srcSiteId, destSiteId);
        String aSiteEquipId = tunnelUtils.resolveEquipIdBySiteId(siteLinkId, siteId);
        Node aSiteLinkWssNode = getNodeByEquipId(aSiteEquipId, inMemoryNode);
        String zSiteEquipId = tunnelUtils.resolveEquipIdBySiteId(nextSiteLinkId, siteId);
        Node zSiteLinkWssNode = getNodeByEquipId(zSiteEquipId, inMemoryNode);

        //init
        Link wssLink;

        if (wssLinks.isEmpty()) {
            Set<Integer> usedDimensions = new HashSet<>();
            Optional<Pair<TerminationPoint, TerminationPoint>> tpPairOpt = pickAvailableTpPair(
                    aSiteLinkWssNode, aSiteEquipId,
                    zSiteLinkWssNode, zSiteEquipId, usedDimensions
            );

            if (!tpPairOpt.isPresent()) {
                String msg = String.format("Failed to allocate roadm for site:%s, between sitelink:%s, and sitelink:%s, because no available wssTP.",
                        siteId, siteLinkId, nextSiteLinkId);
                throw new NeDesignerException(msg);
            }

            TerminationPoint aSiteLinkWssTp = tpPairOpt.get().getLeft();
            TerminationPoint zSiteLinkWssTp = tpPairOpt.get().getRight();
            //create wss link
            String aWssTpId = aSiteLinkWssTp.getTpId().getValue();
            String zWssTpId = zSiteLinkWssTp.getTpId().getValue();
            wssLink = linkRepo.createLink(aWssTpId, zWssTpId, LinkType.WssLink, siteLinkIds);

        } else {
            wssLink = wssLinks.get(0);
        }

        //wss link已经存在，就只需要create xc
        List<Link> osLinks = new ArrayList<>();
        osLinks.add(wssLink);
        //create xc
        Card iraCardInfoA = nodeUtils.getCardInfoByEquipId(aSiteLinkWssNode, aSiteEquipId);
        String aWssTpId = tunnelUtils.resolveTpIdByEquipId(wssLink, aSiteEquipId);
        CrossConnections aWssXc = olsNodeService.createOchXC(
                aSiteLinkWssNode.getNodeId().getValue(), aWssTpId, iraCardInfoA, cenFrequency, frequencyString);

        Card iraCardInfoZ = nodeUtils.getCardInfoByEquipId(zSiteLinkWssNode, zSiteEquipId);
        String zWssTpId = tunnelUtils.resolveTpIdByEquipId(wssLink, zSiteEquipId);
        CrossConnections zWssXc = olsNodeService.createOchXC(
                zSiteLinkWssNode.getNodeId().getValue(), zWssTpId, iraCardInfoZ, cenFrequency, frequencyString);
        List<CrossConnections> wssXcs = Arrays.asList(aWssXc, zWssXc);
        //update node
        List<Node> nodeSnapshots = new ArrayList<>();
        if (!inMemoryNode.containsKey(aSiteLinkWssNode.getNodeId().getValue())) {
            nodeSnapshots.add(nodeUtils.getNodeCopy(aSiteLinkWssNode));
        }
        if (!inMemoryNode.containsKey(zSiteLinkWssNode.getNodeId().getValue())) {
            nodeSnapshots.add(nodeUtils.getNodeCopy(zSiteLinkWssNode));
        }

        Node wssNodeUpdatedA = olsNodeService.updatedOlsNode(aSiteLinkWssNode, osLinks, aWssTpId, Arrays.asList(aWssXc));
        Node wssNodeUpdatedZ = olsNodeService.updatedOlsNode(zSiteLinkWssNode, osLinks, zWssTpId, Arrays.asList(zWssXc));
        inMemoryNode.put(wssNodeUpdatedA.getNodeId().getValue(), wssNodeUpdatedA);
        inMemoryNode.put(wssNodeUpdatedZ.getNodeId().getValue(), wssNodeUpdatedZ);

        return OtRouteInfoNewOch.builder()
                .inMemoryNodes(inMemoryNode)
                .osLinks(osLinks)
                .tpcXcs(Collections.EMPTY_LIST)
                .ochXcs(wssXcs)
                .snapshotNodes(nodeSnapshots)
                .muxMdPortTpSrcPair(null)
                .build();


    }

    public Optional<Pair<TerminationPoint, TerminationPoint>> pickAvailableTpPair(
            Node aNode, String aEquipId,
            Node zNode, String zEquipId, Set<Integer> usedDimensions) throws NeDesignerException {

//        Set<Integer> aUsedDimensions = getUsedDimensions(aEquipId, aNode);
//        Set<Integer> zUsedDimensions = getUsedDimensions(zEquipId, zNode);
        Integer aNodeDimension = getOwnDimension(aEquipId, aNode);
        Integer zNodeDimension = getOwnDimension(zEquipId, zNode);
        if (aNodeDimension == null) {
            if (zNodeDimension == null) {
                aNodeDimension = AVAILABLE_DIMENSIONS.get(0);
                zNodeDimension = AVAILABLE_DIMENSIONS.get(1);
            } else {
//                Set<Integer> zUsedDimensions = getUsedDimensions(zEquipId, zNode);
                Optional<Integer> aNodeDimensionOptional = AVAILABLE_DIMENSIONS.stream().filter(d -> !usedDimensions.contains(d)).findFirst();
                if (!aNodeDimensionOptional.isPresent()) {
                    log.error("No available dimension for {}, zUsedDimensions is:{}", aEquipId, usedDimensions);
                    throw new NeDesignerException("No available dimension for :" + aEquipId);
                }
                aNodeDimension = aNodeDimensionOptional.get();
            }
        } else if (zNodeDimension == null) {
//            Set<Integer> aUsedDimensions = getUsedDimensions(aEquipId, aNode);
            Optional<Integer> zNodeDimensionOptional = AVAILABLE_DIMENSIONS.stream().filter(d -> !usedDimensions.contains(d)).findFirst();
            if (!zNodeDimensionOptional.isPresent()) {
                log.error("No available dimension for {}, aUsedDimensions is:{}", zEquipId, usedDimensions);
                throw new NeDesignerException("No available dimension for :" + zEquipId);
            }
            zNodeDimension = zNodeDimensionOptional.get();
        }
        // 注意：siteA 用 dimensionZ，siteZ 用 dimensionA（反向）
        Optional<TerminationPoint> aTpOpt = getSiteLinkWssTpOptional(zNodeDimension, aNode, aEquipId);
        Optional<TerminationPoint> zTpOpt = getSiteLinkWssTpOptional(aNodeDimension, zNode, zEquipId);

        if (aTpOpt.isPresent() && zTpOpt.isPresent()) {
            return Optional.of(Pair.of(aTpOpt.get(), zTpOpt.get()));
        }
        log.error("No wss tp for aEquipId:{},portNumber:{};zEquipId:{}, portNumber:{}", aEquipId, zNodeDimension, zEquipId, aNodeDimension);
        throw new NeDesignerException(String.format("No available wss tp pair "));
    }

    private Integer getOwnDimension(String equipId, Node node) {
        List<InternalLinks> internalLinks = node.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        if (internalLinks != null) {
            for (InternalLinks internalLink : internalLinks) {
                if (!internalLink.getLinkType().equals(LinkType.WssLink)) {
                    continue;
                }
                if (!internalLink.getLinkRef().contains(equipId)) {
                    continue;
                }
                String peerTpId = internalLink.getSrcTp().contains(equipId) ? internalLink.getDstTp() : internalLink.getSrcTp();
                return PhysicalTpIdNamingRule.getPortNumberInteger(peerTpId);//对方的port才是自己的维度
            }
        }
        return null;
    }


    private Optional<TerminationPoint> getSiteLinkWssTpOptional(Integer dimension, Node node, String equipId) {
        return node.getTerminationPoint().stream()
                .filter(tp -> {
                    TerminationPoint1 aug = tp.getAugmentation(TerminationPoint1.class);
                    return aug.getPhysical().getEquipmentRef().equals(equipId)
                            && aug.getPhysical().getPortType().equals(PortType.WSSMesh)
                            && aug.getPhysical().getConnectionStatus().equals(ConnectionStatus.Idle)
                            && PhysicalTpIdNamingRule.getPortNameByTpId(tp.getTpId().getValue()).contains("EXP")
                            && PhysicalTpIdNamingRule.getPortNumberInteger(tp.getTpId().getValue()) == dimension;
                }).findAny();
    }

//    private Set<Integer> getUsedDimensions(String equipId, Node node) {
//        List<InternalLinks> internalLinks = node.getAugmentation(Node1.class).getPhysical().getInternalLinks();
//
//        Set<Integer> usedDimension = new HashSet<>();
//        if (internalLinks != null) {
//            for (InternalLinks internalLink : internalLinks) {
//                if (!internalLink.getLinkType().equals(LinkType.WssLink)) {
//                    continue;
//                }
//                if (!internalLink.getLinkRef().contains(equipId)) {
//                    continue;
//                }
//                usedDimension.add(PhysicalTpIdNamingRule.getPortNumberInteger(internalLink.getSrcTp()));
//                usedDimension.add(PhysicalTpIdNamingRule.getPortNumberInteger(internalLink.getDstTp()));
//            }
//        }
//        return usedDimension;
//    }

    private Node getNodeByEquipId(String equipId, Map<String, Node> inMemoryNode) {
        String nodeId = PhysicalEqpIdNamingRule.getNodeId(equipId);

        Node node = Optional.ofNullable(inMemoryNode.get(nodeId))
                .orElseGet(() -> phyNodeDao.getConfigPhyNodeById(nodeId));
        return node;
    }

}
