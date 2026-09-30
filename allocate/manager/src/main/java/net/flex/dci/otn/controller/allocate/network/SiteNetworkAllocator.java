/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.allocate.common.RouteYangDataConverter;
import net.flex.dci.otn.controller.allocate.common.YangDataConverter;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.NetworkInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.ReallocateDataModel;
import net.flex.dci.otn.controller.allocate.designer.model.site.WssNetworkInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.WssNetworkInput;
import net.flex.dci.otn.controller.allocate.designer.reallocate.ReallocateEquipRepo;
import net.flex.dci.otn.controller.allocate.link.common.BomGenerator;
import net.flex.dci.otn.controller.allocate.link.common.CreateSiteLinkParam;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkComputer;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.site.NeBomInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateNetworkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateNetworkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateNetworkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetConnectableNetworkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetConnectableNetworkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetConnectableNetworkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.CreateLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.CreateLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.Roadms;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.RoadmsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.result.BomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.Segment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.SegmentBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.connectable.network.output.NodeRelations;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.connectable.network.output.NodeRelationsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.connectable.network.output.SiteLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.connectable.network.output.site.links.SiteANe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.connectable.network.output.site.links.SiteANeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.connectable.network.output.site.links.SiteZNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.connectable.network.output.site.links.SiteZNeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.SiteLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.WssLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiteNetworkAllocator {

    @Autowired
    private SiteLinkComputer siteLinkComputer;

    @Autowired
    private NeDesigner neDesigner;

    @Autowired
    private SiteLinkDao siteLinkDao;
    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private SiteNodeDao siteNodeDao;
    @Autowired
    private BomGenerator bomGenerator;

    @Autowired
    private NodeUtils nodeUtils;

    @Autowired
    private ReallocateEquipRepo reallocateEquipRepo;

    public AllocateNetworkOutput doIt(AllocateNetworkInput input) {

        //create new site links
        List<CreateLinks> createLinksInput = input.getCreateLinks();
        Map<String, RouteInfo> computedSiteLinks = new HashMap<>();//key is the input site link friendly name
        Map<String, Node> totalNodesMap = new HashMap<>();
        Map<String, Node> totalIpNodesMap = new HashMap<>();//kye is ip. Only put node has ip
        List<Node> reusedNodesSnapshot = new ArrayList<>();// Put reused nodes snapshot from DB.
        NeInfo neInfo = null;

        List<String> siteLinkNames = new ArrayList<>();//keep the order as input
        for (CreateLinks siteLink : createLinksInput) {
            CreateLinks updatedSiteLink = getUpdatedCreateLinks(siteLink);
            CreateSiteLinkParam param = new CreateSiteLinkParam();
            param.parser(updatedSiteLink);
            siteLinkNames.add(siteLink.getFriendlyName());
            RouteInfo linkRouteInfo = null;
            try {
                linkRouteInfo = siteLinkComputer.getRouteResource(param, totalIpNodesMap, reusedNodesSnapshot);
            } catch (NeDesignerException e) {
                log.error("Failed to compute siteLink for:{}.", input, e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to compute siteLink." + e.getCause().getMessage(), e);
            }
            if (input.isIsCompacted()) {
                if (neInfo == null) {
                    try {
                        neInfo = neDesigner.getNeInfo(param.getVendorName(), param.getVendorType(), NodeType.OD.name());
                    } catch (NeDesignerException e) {
                        log.error("Failed to getNeInfo.", e);
                        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to get NeInfo." + e.getCause().getMessage(), e);
                    }
                }
                Set<String> noIpSites = getSiteIdsWithOutIpConfig(siteLink);
                linkRouteInfo = getReusedRoute(neInfo, totalNodesMap, linkRouteInfo, noIpSites);
            }
            updateTotalNodeMap(totalNodesMap, totalIpNodesMap, linkRouteInfo);
            computedSiteLinks.put(siteLink.getFriendlyName(), linkRouteInfo);
        }
//        updateReusedNodeInRoute(totalNodesMap, computedSiteLinks);

        //create new WSS connections between site links
        NetworkInfo allocatedNetwork = handleWssConnection(totalNodesMap, computedSiteLinks, input.getRoadms());//在这里，顺便把route里面的node也根据reuse情况，更新成reuse以后的最终状态

        //convert to Output format
        @NonNull Map<String, RouteInfo> allocatedSiteLinks = allocatedNetwork.getCreateLinks();
        List<SiteLinks> outputSiteLinks = new ArrayList<>();
        for (Entry<String, RouteInfo> entry : allocatedSiteLinks.entrySet()) {
            RouteInfo routeInfo = entry.getValue();
            SiteLinks outputSiteLink = RouteYangDataConverter.convertToOutputSitelink(routeInfo, entry.getKey());
            outputSiteLinks.add(outputSiteLink);
        }

        List<WssLinks> wssLinks = RouteYangDataConverter.convertToOutputWsslinks(allocatedNetwork.getWssLinks());

        //generate BOM
        BomInfo bomInfo;
        try {
            bomInfo = constructBomInfo(siteLinkNames, allocatedSiteLinks, reusedNodesSnapshot, input);
        } catch (
                Exception e) {
            log.error("Failed to generate BOM.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to generate BOM." + e.getCause().getMessage(), e);
        }

        return new AllocateNetworkOutputBuilder().
                setWssLinks(wssLinks).
                setSiteLinks(outputSiteLinks).
                setBomInfo(new BomInfoBuilder(bomInfo).build()).
                setRoadms(allocatedNetwork.getRoadms()).
                setReturnCode(RpcResultType.Success).setCreateLinks(input.getCreateLinks()).
                build();
    }

    private Set<String> getSiteIdsWithOutIpConfig(CreateLinks siteLink) {
        return siteLink.getSegment().stream().flatMap(segment -> {
            List<String> sites = new ArrayList<>();
            if (segment.getSourceNodeIp() == null || segment.getSourceNodeIp().isEmpty()) {
                sites.add(segment.getSource());//add source site id
            }
            if (segment.getDestinationNodeIp() == null || !segment.getDestinationNodeIp().isEmpty()) {
                sites.add(segment.getDestination());//add dest site id
            }
            return sites.stream();
        }).collect(Collectors.toSet());
    }

    private void updateTotalIPNodeMap(Map<String, Node> totalIpNodesMap, RouteInfo linkRouteInfo) {

    }

//    private void updateReusedNodeInRoute(Map<String, Node> totalNodesMap, Map<String, RouteInfo> computedSiteLinks) {
//        if (totalNodesMap.isEmpty()) {
//            return;
//        }
//        Collection<RouteInfo> routeInfos = computedSiteLinks.values();
//        for (RouteInfo routeInfo : routeInfos) {
//            List<Node> mainNodes = routeInfo.getMain().getNodes();
//            updatedNodes(totalNodesMap, mainNodes);
//            if (routeInfo.getSlave() != null && routeInfo.getSlave().getNodes() != null && !routeInfo.getSlave().getNodes().isEmpty()) {
//                updatedNodes(totalNodesMap, routeInfo.getSlave().getNodes());
//            }
//        }
//    }
//


    private void updateTotalNodeMap(Map<String, Node> totalNodeMap, Map<String, Node> totalIpNodesMap, RouteInfo linkRouteInfo) {

        List<Node> newNodes = linkRouteInfo.getMain().getNodes();
        Route slave = linkRouteInfo.getSlave();
        if (slave != null && slave.getNodes() != null) {
            newNodes.addAll(newNodes);
        }

        for (Node newNode : newNodes) {
            totalNodeMap.put(newNode.getNodeId().getValue(), newNode);
            String nodeIp = nodeUtils.getIp(newNode);
            if (nodeIp != null) {
                totalIpNodesMap.put(nodeIp, newNode);
            }
        }
    }

    private boolean isNodeStuff(Node node) {
        return nodeUtils.isStuffed(node);
    }

    /**
     * 1. 现在只支持重用同一次创建过程中，内存里的node，不支持从数据库获取
     * <p>
     * 2. 目前重用原则是必须能重用所有linecard才重用
     * <p>
     * 3. 这里的重用，主要是针对compact选项的，此选项只对没有ip的node有效
     *
     * @param neInfo
     * @param totalNodeMap
     * @param linkRouteInfo
     * @param noIpSites
     * @return
     */
    private RouteInfo getReusedRoute(NeInfo neInfo, final Map<String, Node> totalNodeMap, RouteInfo linkRouteInfo, Set<String> noIpSites) {
        //current sitelink segments all have ip config, then not reuse here.
        if (noIpSites.isEmpty()) {
            return linkRouteInfo;//not reuse
        }

        //prepare reused node map: filter no ip  and unStuff node
        Map<String, List<Node>> siteNodeMap = totalNodeMap.values().stream().filter(node -> nodeUtils.getIp(node) == null && !isNodeStuff(node))
                .collect(Collectors.groupingBy(node -> PhysicalNodeIdNamingRule.getSiteId(node.getNodeId().getValue())));
        if (siteNodeMap.isEmpty()) {
            return linkRouteInfo;//not reuse
        }

        Route main = linkRouteInfo.getMain();
        List<Node> originalNodes = main.getNodes();
        Route slave = linkRouteInfo.getSlave();
        if (slave != null && slave.getNodes() != null) {
            originalNodes.addAll(slave.getNodes());
        }
        ReallocateDataModel reallocateDataModel = getReusedEquipMap(neInfo, siteNodeMap, originalNodes, noIpSites);
        if (reallocateDataModel == null) {
            return linkRouteInfo;//not reuse
        }

        //reallocate
        RouteInfo routeInfoOutput;
        try {
            routeInfoOutput = neDesigner.reallocateSite(linkRouteInfo, reallocateDataModel);
        } catch (NeDesignerException e) {
            log.error("Failed to reallocate by ne designer.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "neDesigner error" + e.getCause().getMessage(), e);
        }
        return routeInfoOutput;

    }

    private ReallocateDataModel getReusedEquipMap(NeInfo neInfo, Map<String, List<Node>> siteNodeMap, List<Node> originalNodes, Set<String> noIpSites) {

        Map<String, Map<String, String>> reallocateNodeEquipMap = new HashMap<>();//<oldNodeId,<oldEquipId,newEquipId>>
        Map<String, String> totalEquipMap = new HashMap<>();//<oldEquipId,newEquipId>
        Set<String> pickedReusedNodes = new HashSet<>();
        List<Node> reusedNodesSnapshot = new ArrayList<>();

        for (Node oriNode : originalNodes) {
            String siteId = PhysicalNodeIdNamingRule.getSiteId(oriNode.getNodeId().getValue());
            if (!noIpSites.contains(siteId)) {
                continue;
            }
            List<Node> reusedNodes =
                    siteNodeMap.containsKey(siteId) ? siteNodeMap.get(siteId).stream().filter(item -> !pickedReusedNodes.contains(item)).collect(Collectors.toList()) : Collections.EMPTY_LIST;
            if (reusedNodes.isEmpty()) {
                continue;
            }
            List<Equipments> lineCardEquips = nodeUtils.getCardEquipments(oriNode);
            Map<String, List<Integer>> lineCardPossibleSlots = getLineCardPossibleSlots(neInfo, lineCardEquips);
            Map<String, String> lineCardReused = new HashMap<>();//<oldEquipId,newEquipId>
            int needEquip = lineCardEquips.size();
            String reusedNodeId = null;
            Node reusedNodePicked = null;
            for (Node reusedNode : reusedNodes) {
                if (lineCardReused.size() == needEquip) {
                    break;
                }
                lineCardReused.clear();
                Set<Integer> emptySlots = getEmptySlot(reusedNode);
                if (emptySlots.size() < needEquip) {
                    continue;
                }
                reusedNodeId = reusedNode.getNodeId().getValue();
                reusedNodePicked = reusedNode;
                for (Entry<String, List<Integer>> entry : lineCardPossibleSlots.entrySet()) {
                    List<Integer> equipPossibleSlots = entry.getValue();
                    String oldEquipId = entry.getKey();
                    for (Integer slot : equipPossibleSlots) {
                        if (emptySlots.contains(slot)) {
                            String newEquipId = reallocateEquipRepo.reallocateEquipId(oldEquipId, reusedNodeId, slot.toString());
                            lineCardReused.put(oldEquipId, newEquipId);
                            emptySlots.remove(slot);
                            break;
                        }
                        //todo:将来可以改善，当可选的empty里不满足时，可以去lineCardPickSlot看看是否可以调换下。
                    }
                }
            }

            if (lineCardReused.size() == needEquip) {//找到了可以重用的node
                reallocateNodeEquipMap.put(oriNode.getNodeId().getValue(), lineCardReused);
                totalEquipMap.putAll(lineCardReused);
                pickedReusedNodes.add(reusedNodeId);
                reusedNodesSnapshot.add(reusedNodePicked);
            }

        }

        if (reusedNodesSnapshot.isEmpty()) {
            return null;// can not reuse node
        }
        return ReallocateDataModel.builder()
                .reusedNodesSnapshot(reusedNodesSnapshot)
                .reusedNodesSnapshotOp(Collections.EMPTY_LIST)
                .reallocateNodeEquipMap(reallocateNodeEquipMap)
                .reallocateEquipMap(totalEquipMap)
                .reallocateMapInsideCompute(Collections.EMPTY_MAP).build();
    }

    private Set<Integer> getEmptySlot(Node reusedNode) {
        try {
            return nodeUtils.getEmptySlot(reusedNode);
        } catch (NeDesignerException e) {
            log.error("Failed to getEmptySlot for node:{}", reusedNode, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to getEmptySlot." + e.getCause().getMessage(), e);
        }
    }

    private Map<String, List<Integer>> getLineCardPossibleSlots(NeInfo neInfo, List<Equipments> lineCardEquips) {
        Map<String, List<Integer>> result = new HashMap<>();
        for (Equipments equipment : lineCardEquips) {
            List<Integer> possibleSlots;
            try {
                possibleSlots = getPossibleSlots(equipment, neInfo);
            } catch (NeDesignerException e) {
                log.error("Failed to get possibleSlots for equip:{}", equipment, e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "failed get possibleSlots for equip." + e.getCause().getMessage(), e);
            }
            result.put(equipment.getEquipmentId(), possibleSlots);
        }
        return result;
    }

    private List<Integer> getPossibleSlots(Equipments equipment, NeInfo neInfo) throws NeDesignerException {
        String cardVendorType = equipment.getEquipTypeVendorSpecific();
        Card card = neInfo.getCardByCardVendor(cardVendorType);
        return card.getPossibleSlot();
    }

    /**
     * modify all "OTM" node type to "SITE"
     *
     * @param siteLink
     * @return
     */
    private CreateLinks getUpdatedCreateLinks(CreateLinks siteLink) {
        List<Segment> segments = siteLink.getSegment();
        List<Segment> updatedSegments = new ArrayList<>();
        for (Segment segment : segments) {
            SegmentBuilder segmentBuilder = new SegmentBuilder(segment);
//            if (segment.getSourceNodeType().equals(LinkTerminationNodeType.OTM)) {
//                segmentBuilder.setSourceNodeType(LinkTerminationNodeType.ROADM);//在network中，不管是否有WSSlink，都自动创建wss卡，所以是ROADM
//            }
//            if (segment.getDestinationNodeType().equals(LinkTerminationNodeType.OTM)) {
//                segmentBuilder.setDestinationNodeType(LinkTerminationNodeType.ROADM);
//            }
            updatedSegments.add(segmentBuilder.build());
        }
        return new CreateLinksBuilder(siteLink).setSegment(updatedSegments).build();
    }

    private NetworkInfo handleWssConnection(Map<String, Node> totalNodesMap, Map<String, RouteInfo> siteLinks, List<Roadms> roadms) {
        List<Link> wssLinks = new ArrayList<>();
        List<Roadms> updatedRoadms = new ArrayList<>();
        if (roadms == null) {
            roadms = Collections.emptyList();
        }

        for (Roadms roadm : roadms) {
            String siteId = roadm.getSiteId();
            List<SiteLinkRelation> siteLinkRelations = roadm.getSiteLinkRelation();
            List<SiteLinkRelation> siteLinkRelationsUpdated = new ArrayList<>();
            Set<Integer> usedDimensions = new HashSet<>();
            for (SiteLinkRelation siteLinkRelation : siteLinkRelations) {
                String linkaName = siteLinkRelation.getLinka();
                String linkzName = siteLinkRelation.getLinkz();
                RouteInfo linkA = siteLinks.get(linkaName);
                if (linkA == null) {
                    linkA = getSiteLinkDataFromDb(linkaName, siteId);
                } else {
                    updateLinkReusedNodes(totalNodesMap, linkA);
                }
                RouteInfo linkZ = siteLinks.get(linkzName);
                if (linkZ == null) {
                    linkZ = getSiteLinkDataFromDb(linkzName, siteId);
                } else {
                    updateLinkReusedNodes(totalNodesMap, linkZ);
                }

                WssNetworkInfo wssNetworkInfo;
                WssNetworkInput wssNetworkInput = WssNetworkInput.builder().totalNodesMap(totalNodesMap).linkAName(linkaName).linkA(linkA).linkAPort(siteLinkRelation.getLinkaPort())
                        .linkZName(linkzName).linkZ(linkZ).linkZPort(siteLinkRelation.getLinkzPort()).siteId(siteId).usedDimensions(usedDimensions).build();
                try {
                    wssNetworkInfo = neDesigner.allocateWssConnection(wssNetworkInput);
                } catch (NeDesignerException e) {
                    log.error("Failed to allocate WSS connection for: {}", siteLinkRelation, e);
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "neDesigner error" + e.getCause().getMessage(), e);
                }
                totalNodesMap = wssNetworkInfo.getTotalNodesMap();
                siteLinks.put(linkaName, wssNetworkInfo.getLinkA());
                siteLinks.put(linkzName, wssNetworkInfo.getLinkZ());
                wssLinks.add(wssNetworkInfo.getWssLink());
                usedDimensions.add(PhysicalTpIdNamingRule.getPortNumberInteger(PhysicalLinkIdNamingRule.getTpAId(wssNetworkInfo.getWssLink().getLinkId().getValue())));
                usedDimensions.add(PhysicalTpIdNamingRule.getPortNumberInteger(PhysicalLinkIdNamingRule.getTpZId(wssNetworkInfo.getWssLink().getLinkId().getValue())));
                siteLinkRelationsUpdated.add(new SiteLinkRelationBuilder()
                        .setIndex(siteLinkRelation.getIndex())
                        .setLinka(linkaName)
                        .setLinkz(linkzName)
                        .setWssLinkIdBetweenAZ(wssNetworkInfo.getWssLink().getLinkId().getValue()).build());
            }
            updatedRoadms.add(new RoadmsBuilder().setSiteLinkRelation(siteLinkRelationsUpdated).setSiteId(roadm.getSiteId()).build());
        }

        updateSiteLinksByReusedNode(siteLinks, totalNodesMap);
        return NetworkInfo.builder().createLinks(siteLinks).wssLinks(wssLinks).roadms(updatedRoadms).build();
    }

    private void updateSiteLinksByReusedNode(Map<String, RouteInfo> siteLinks, Map<String, Node> totalNodesMap) {
        for (Entry<String, RouteInfo> entry : siteLinks.entrySet()) {
            updateLinkReusedNodes(totalNodesMap, entry.getValue());
        }
    }

    private void updateLinkReusedNodes(Map<String, Node> totalNodesMap, RouteInfo routeInfo) {
        if (totalNodesMap.isEmpty()) {
            return;
        }

        List<Node> mainNodes = routeInfo.getMain().getNodes();
        updatedNodes(totalNodesMap, mainNodes);
        if (routeInfo.getSlave() != null && routeInfo.getSlave().getNodes() != null && !routeInfo.getSlave().getNodes().isEmpty()) {
            updatedNodes(totalNodesMap, routeInfo.getSlave().getNodes());
        }
    }

    /**
     * Note: this method will change the input nodes list directly
     *
     * @param totalNodesMap
     * @param nodes
     */
    private void updatedNodes(Map<String, Node> totalNodesMap, List<Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            Node oriNode = nodes.get(i);
            Node updateNode = totalNodesMap.get(oriNode.getNodeId().getValue());
            if (updateNode == null) {
                continue;
            }
            nodes.set(i, updateNode);
        }
    }


    private RouteInfo getSiteLinkDataFromDb(String siteLinkFriendlyName, String siteId) {
        Link siteLink = siteLinkDao.getLinkByFriendlyName(siteLinkFriendlyName);
        String nodeId;
        if (siteLink == null) {
            String msg = String.format("Failed to get site link from DB by friendly name:%s", siteLinkFriendlyName);
            log.error(msg);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
        if (siteLink.getSource().getSourceNode().getValue().contains(siteId)) {
            nodeId = PhysicalTpIdNamingRule.getNodeId(siteLink.getSource().getSourceTp().getValue());
        } else if (siteLink.getDestination().getDestNode().getValue().contains(siteId)) {
            nodeId = PhysicalTpIdNamingRule.getNodeId(siteLink.getDestination().getDestTp().getValue());
        } else {
            String msg = String.format("Invalid data, the site link %s doesn't in site:%s", siteLinkFriendlyName, siteId);
            log.error(msg);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (node == null) {
            String msg = String.format("Failed to get node:%s of site link:%s from DB.", nodeId, siteLinkFriendlyName);
            log.error(msg);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }

        return RouteInfo.builder().main(Route.builder().nodes(Arrays.asList(node)).build()).build();
    }

    private BomInfo constructBomInfo(List<String> siteLinkNames, @NonNull Map<String, RouteInfo> siteLinks, List<Node> reusedNodesSnapshot, AllocateNetworkInput input) throws NeDesignerException {
        String vendorName = input.getCreateLinks().get(0).getVendorOccupationRate().get(0).getVendorName();
        String productType = input.getCreateLinks().get(0).getVendorOccupationRate().get(0).getProductType();
//        Set<String> createdSiteLinkNames = input.getCreateLinks().stream().map(CreationParams::getFriendlyName).collect(Collectors.toSet());

        Map<String, Node> bomNodes = new HashMap<>();

        //因为node会被重用，所以必须严格按照siteLinkNames的顺序来， 后面的node才会覆盖前面的。
        for (String siteLinkName : siteLinkNames) {
            RouteInfo routeInfo = siteLinks.get(siteLinkName);
            if (routeInfo == null) {
                continue;
            }
            bomNodes.putAll(routeInfo.getMain().getNodes().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity(), (n1, n2) -> n2)));
        }

//        Map<String, Node> bomNodes = siteLinks.entrySet().stream()
//                .filter(item -> createdSiteLinkNames.contains(item.getKey()))
//                .flatMap(item -> item.getValue().getMain().getNodes().stream())
//                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), Function.identity(), (n1, n2) -> n2));

        List<Nodes> reusedNodesSnapshotUI = YangDataConverter.convertToUINodeList(reusedNodesSnapshot);

        Map<String, Map<String, NeBomInfo>> bomMap = bomGenerator.generateBomMap(bomNodes, vendorName, productType, reusedNodesSnapshotUI);
        return bomGenerator.constructBomInfo(bomMap);
    }

    public GetConnectableNetworkOutput getConnectableNetwork(GetConnectableNetworkInput input) {
        String grid = input.getFrequencyGrid() == null ? null : input.getFrequencyGrid().toString();
        if (grid != null && grid.equals("0")) {
            grid = null;//因为flex grid可以和任何grid相连，所以过滤条件不需要加grid
        }
        String siteAId = input.getSiteId();

        List<Link> links = siteLinkDao.filterNetwork(siteAId, grid, input.isAll(), input.getPlaneName());
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.connectable.network.output.SiteLinks> siteLinks = new ArrayList<>();
        Set<String> siteANodes = new HashSet<>();//留着后续做node relation用
        for (Link link : links) {
            String srcSiteId = link.getSource().getSourceNode().getValue();
            String srcNodeId = PhysicalTpIdNamingRule.getNodeId(link.getSource().getSourceTp().getValue());
            String destNodeId = PhysicalTpIdNamingRule.getNodeId(link.getDestination().getDestTp().getValue());
            String siteANodeId = srcNodeId;
            String siteZNodeId = destNodeId;
            String siteZId = link.getDestination().getDestNode().getValue();
            if (!srcSiteId.equals(siteAId)) {
                siteANodeId = destNodeId;
                siteZNodeId = srcNodeId;
                siteZId = srcSiteId;
            }
            SiteType siteZType = siteNodeDao.getSiteType(siteZId);

            String siteANeFriendlyName = phyNodeDao.getFriendlyName(siteANodeId);
            String siteZNeFriendlyName = phyNodeDao.getFriendlyName(siteZNodeId);
            SiteANe siteANe = new SiteANeBuilder().setFriendlyName(siteANeFriendlyName).setNodeId(siteANodeId).build();
            SiteZNe siteZNe = new SiteZNeBuilder().setFriendlyName(siteZNeFriendlyName).setNodeId(siteZNodeId).build();
            siteANodes.add(siteANodeId);

            Site linkSiteData = link.getAugmentation(Link1.class).getSite();
            siteLinks.add(new SiteLinksBuilder()
                    .setLinkId(link.getLinkId().getValue())
                    .setFriendlyName(linkSiteData.getFriendlyName())
                    .setGrid(linkSiteData.getGrid())
                    .setSitezType(siteZType)
                    .setSiteANe(siteANe)
                    .setSiteZNe(siteZNe)
                    .setNetworkId(linkSiteData.getNetworkId())
                    .build());

        }

        //prepare the node relation connected by wss link
        List<NodeRelations> nodeRelateions = new ArrayList<>();
        List<SiteLinkRelation> siteARelations = siteNodeDao.getSiteNodeById(siteAId).getAugmentation(Node1.class).getSite().getSiteLinkRelation();
        if (siteARelations != null) {
            for (SiteLinkRelation siteLinkRelation : siteARelations) {
                String wssLinkId = siteLinkRelation.getWssLinkIdBetweenAZ();
                String nodeAId = PhysicalLinkIdNamingRule.getNodeAId(wssLinkId);
                String nodeZId = PhysicalLinkIdNamingRule.getNodeZId(wssLinkId);
                if (siteANodes.contains(nodeAId) && siteANodes.contains(nodeZId)) {
                    nodeRelateions.add(new NodeRelationsBuilder().setNodeaId(nodeAId).setNodezId(nodeZId).build());
                }
            }
        }

        return new GetConnectableNetworkOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .setSiteLinks(siteLinks)
                .setNodeRelations(nodeRelateions).build();

    }
}
