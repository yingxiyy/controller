/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;
import net.flex.dci.otn.controller.allocate.designer.config.SiteModelConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.RamanSupport;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteNodeInput;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.site.model.LinkOutput;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import net.flex.dci.otn.controller.allocate.ne.OcmGridGroup;
import net.flex.dci.otc.mongo.enums.NeSubType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiteRepo {

    @Autowired
    private NEInfoConfig neInfoConfig;
    @Autowired
    private SiteModelConfig siteModelConfig;
    @Autowired
    private NeNodeRepo neNodeRepo;
    @Autowired
    private LinkService linkService;

    @Autowired
    private SiteNodeService siteNodeService;

    private final static Set<String> SLAVE_NODE_TYPES = new HashSet<>(Arrays.asList("I", "D"));

    public RouteInfo allocate(SiteInput input) throws NeDesignerException {

        validate(input);

        //allocate
        NeInfo neInfo = getNeInfo(input);
        Map<RoutingType, Route> routes = allocateRoutes(input, neInfo);

        //construct output
        Route main = null;
        Route slave = null;
        Route third = null;
        for (Entry<RoutingType, Route> entry : routes.entrySet()) {
            RoutingType role = entry.getKey();
            switch (role) {
                case Main:
                    main = entry.getValue();
                    break;
                case Slave:
                    slave = entry.getValue();
                    break;
                case Third:
                    third = entry.getValue();
                    break;
                default:
                    throw new NeDesignerException("Unsupported role: " + role);
            }

        }
        return RouteInfo.builder().main(main).slave(slave).third(third).build();

    }

    private Map<RoutingType, Route> allocateRoutes(SiteInput input, NeInfo neInfo) throws NeDesignerException {

        Map<RoutingType, Route> result = new HashMap<>();
        Map<RoutingType, List<SiteNodeInput>> nodesMap = input.getNodesMap();
        SiteResourceLayout resourceLayout = SiteResourceLayoutFactory.create(input);

        Set<String> busyTpIds = new HashSet<>();

        //allocate main firstly
        AllocatedSiteInfo mainSiteInfo = null;
        try {
            mainSiteInfo = allocateMain(input, nodesMap.get(RoutingType.Main), neInfo, busyTpIds, RoutingType.Main);
        } catch (Throwable e) {
            log.error("Failed to allocate main.", e);
            throw new NeDesignerException("Failed to allocate main.", e);
        }

        //allocate slave
        if (nodesMap.containsKey(RoutingType.Slave)) {
            List<SiteNodeInput> slaveNodes = nodesMap.get(RoutingType.Slave);
            if (resourceLayout.requiresDedicatedProtectionNode()) {
                slaveNodes = addProtectionPeerNodes(nodesMap.get(RoutingType.Main), slaveNodes,
                        RoutingType.Slave, null, null);
            }
            AllocatedSiteInfo slaveSiteInfo = getAllocatedSiteSlave(input, neInfo, mainSiteInfo,
                    slaveNodes, busyTpIds, RoutingType.Slave);
            mainSiteInfo.getSiteRoute().getNodes().set(0, slaveSiteInfo.getUpdatedProtectedNodes().get(0));
            mainSiteInfo.getSiteRoute().getNodes().set(mainSiteInfo.getSiteRoute().getNodes().size() - 1, slaveSiteInfo.getUpdatedProtectedNodes().get(1));
            refreshProtectedNodeSnapshots(mainSiteInfo, slaveSiteInfo);
            result.put(RoutingType.Slave, slaveSiteInfo.siteRoute);

            if (resourceLayout.requiresDedicatedProtectionNode()
                    && nodesMap.containsKey(RoutingType.Third)) {
                List<Node> slaveRouteNodes = slaveSiteInfo.getSiteRoute().getNodes();
                // Layout decides whether the third leg shares the slave peer NE or owns
                // another peer NE for chassis-failure isolation.
                Node leftPeerNode = resourceLayout.reuseProtectionPeerNodeForThird()
                        ? slaveRouteNodes.get(0) : null;
                Node rightPeerNode = resourceLayout.reuseProtectionPeerNodeForThird()
                        ? slaveRouteNodes.get(slaveRouteNodes.size() - 1) : null;
                nodesMap.put(RoutingType.Third, addProtectionPeerNodes(
                        nodesMap.get(RoutingType.Main), nodesMap.get(RoutingType.Third),
                        RoutingType.Third, leftPeerNode, rightPeerNode));
            }
        }

        //allocate third
        if (nodesMap.containsKey(RoutingType.Third)) {
            AllocatedSiteInfo thirdSiteInfo = getAllocatedSiteSlave(input, neInfo, mainSiteInfo, nodesMap.get(RoutingType.Third), busyTpIds, RoutingType.Third);
            mainSiteInfo.getSiteRoute().getNodes().set(0, thirdSiteInfo.getUpdatedProtectedNodes().get(0));
            mainSiteInfo.getSiteRoute().getNodes().set(mainSiteInfo.getSiteRoute().getNodes().size() - 1, thirdSiteInfo.getUpdatedProtectedNodes().get(1));
            refreshProtectedNodeSnapshots(mainSiteInfo, thirdSiteInfo);
            result.put(RoutingType.Third, thirdSiteInfo.siteRoute);
        }

        result.put(RoutingType.Main, mainSiteInfo.siteRoute);
        return result;
    }

    private List<SiteNodeInput> addProtectionPeerNodes(List<SiteNodeInput> mainNodes,
            List<SiteNodeInput> routeNodes, RoutingType role, Node leftNode, Node rightNode) {
        List<SiteNodeInput> result = new ArrayList<>();
        result.add(createProtectionPeerInput(mainNodes.get(0).getSiteId(), role, leftNode));
        result.addAll(getProtectionTransitNodes(routeNodes));
        result.add(createProtectionPeerInput(mainNodes.get(mainNodes.size() - 1).getSiteId(), role,
                rightNode));
        return result;
    }

    private List<SiteNodeInput> getProtectionTransitNodes(List<SiteNodeInput> routeNodes) {
        if (routeNodes == null || routeNodes.isEmpty()) {
            return Collections.emptyList();
        }
        int from = isTerminalProtectionPlaceholder(routeNodes.get(0)) ? 1 : 0;
        int to = isTerminalProtectionPlaceholder(routeNodes.get(routeNodes.size() - 1))
                ? routeNodes.size() - 1 : routeNodes.size();
        if (from >= to) {
            return Collections.emptyList();
        }
        // Dedicated Bone2.0 protection peers own the T/R endpoints; keep only real transit nodes.
        return new ArrayList<>(routeNodes.subList(from, to));
    }

    private boolean isTerminalProtectionPlaceholder(SiteNodeInput node) {
        return "T".equals(node.getNodeType()) || "R".equals(node.getNodeType());
    }

    private SiteNodeInput createProtectionPeerInput(String siteId, RoutingType role, Node node) {
        // Bone2.0 1+2 protection peer placement is selected by SiteResourceLayout.
        return SiteNodeInput.builder().siteId(siteId).nodeType("I")
                .neSubType(NeSubType.OPC_ILA).ipNode(node).protectionPeerRole(role).build();
    }

    private void refreshProtectedNodeSnapshots(AllocatedSiteInfo mainSiteInfo,
            AllocatedSiteInfo protectionSiteInfo) {
        List<Node> updated = protectionSiteInfo.getUpdatedProtectedNodes();
        if (updated.size() >= 2) {
            mainSiteInfo.getProtectedNodes().get(0).setNode(updated.get(0));
            mainSiteInfo.getProtectedNodes().get(1).setNode(updated.get(1));
        }
    }

    private AllocatedSiteInfo getAllocatedSiteSlave(SiteInput input, NeInfo neInfo, AllocatedSiteInfo mainSiteInfo, List<SiteNodeInput> slaveNodes, Set<String> busyTpIds, RoutingType routingType)
            throws NeDesignerException {
        SiteNodeInfo leftProtectedNode;
        SiteNodeInfo rightProtectedNode;
        try {
            leftProtectedNode = mainSiteInfo.getProtectedNodes().get(0);
            rightProtectedNode = mainSiteInfo.getProtectedNodes().get(1);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            String errorMessage = "Failed to get protected node from MAIN";
            log.error(errorMessage, e);
            throw new NeDesignerException(errorMessage, e);
        }

        return allocate(input, neInfo, slaveNodes, leftProtectedNode, rightProtectedNode, busyTpIds, routingType);
    }

    private void validate(SiteInput input) throws NeDesignerException {
        Map<RoutingType, List<SiteNodeInput>> nodesMap = input.getNodesMap();
        SiteResourceLayout resourceLayout = SiteResourceLayoutFactory.create(input);

        for (Entry<RoutingType, List<SiteNodeInput>> nodesEntry : nodesMap.entrySet()) {
            if (nodesEntry.getKey() == RoutingType.Main) {
                List<SiteNodeInput> mainNodes = nodesEntry.getValue();
                if (!mainNodes.get(0).getNodeType().equals("T") && !mainNodes.get(0).getNodeType().equals("R")) {
                    throw new NeDesignerException("The first node must be T or R.");
                }
                if (!mainNodes.get(mainNodes.size() - 1).getNodeType().equals("T") && !mainNodes.get(mainNodes.size() - 1).getNodeType().equals("R")) {
                    throw new NeDesignerException("The last node must be T or R.");
                }
                log.debug("The main nodes is:{}", mainNodes.stream().map(SiteNodeInput::getNodeType).collect(Collectors.joining()));
            } else {
                List<SiteNodeInput> slaveNodes = resourceLayout.requiresDedicatedProtectionNode()
                        ? getProtectionTransitNodes(nodesEntry.getValue()) : nodesEntry.getValue();
                log.debug("The {} nodes is:{}", nodesEntry.getKey());
                for (SiteNodeInput slaveNode : slaveNodes) {
                    log.debug(slaveNode.getNodeType());
                    if (!SLAVE_NODE_TYPES.contains(slaveNode.getNodeType())) {
                        log.error("Invalid slave node:{}, the slave node can only be {}", slaveNode, SLAVE_NODE_TYPES);
                        throw new NeDesignerException("The slave node can only be I or D.");
                    }
                }

            }
        }
    }


    private Route getEmptyRoute() {
        return Route.builder().links(Collections.EMPTY_LIST).nodes(Collections.emptyList()).xcs(Collections.EMPTY_LIST).build();
    }

    private AllocatedSiteInfo allocateMain(SiteInput input, List<SiteNodeInput> mainNodes, NeInfo neInfo, Set<String> busyTpIds, RoutingType main) throws NeDesignerException {
        return allocate(input, neInfo, mainNodes, null, null, busyTpIds, main);
    }

    public Node createSingleNode(SiteInput input, SiteNodeInput siteNodeInput, boolean reversed)
            throws NeDesignerException {
        NeInfo neInfo = getNeInfo(input);
        return createSingleSiteNodeInfo(input, siteNodeInput, neInfo, reversed).getNode();
    }

    private SiteNodeInfo createSingleSiteNodeInfo(SiteInput input, SiteNodeInput siteNodeInput, NeInfo neInfo,
            boolean reversed) throws NeDesignerException {
        Node node = siteNodeInput.getIpNode();
        if (node == null) {
            node = neNodeRepo.createEmptyNode(input.getVendorName(), neInfo.getProductType(),
                    siteNodeInput.getSiteId(), NodeType.OD, neInfo.getFixEquipModel(), neInfo.getEmptyCard(),
                    input.getPlane(), input.getPlaneId(), input.getRiskGroupName(), siteNodeInput.getNeSubType());
            log.debug("Create empty node without ip:{}", node.getNodeId().getValue());
        }

        SiteResourceLayout resourceLayout = SiteResourceLayoutFactory.create(input);
        SiteResourceLayout nodeLayout = siteNodeInput.getProtectionPeerRole() != null
                || "T".equals(siteNodeInput.getNodeType())
                ? resourceLayout : new DefaultSiteResourceLayout(resourceLayout.supportsTilaClassMode());
        List<String> cardTypes = siteNodeInput.getProtectionPeerRole() == null
                ? Collections.emptyList() : nodeLayout.getProtectionPeerCardClasses();
        if (cardTypes.isEmpty()) {
            cardTypes = siteModelConfig.getMainCardTypes(siteNodeInput.getNodeType(), input.getGrid(),
                    input.getIsProtected(), input.getLinkModel(), input.getWdmBand());
            cardTypes = ByteDance2CardChainResolver.resolveFixedOtm(input.getVendorName(), input.getVendorType(),
                    input.getWdmBand(), input.getGrid(), input.getIsProtected(), siteNodeInput.getNodeType(), cardTypes);
            // Keep siteModelC shared by legacy networks; only Bone2.0 Flex C replaces OA and sizes FMUX.
            cardTypes = ByteDance2CardChainResolver.resolve(input.getVendorName(), input.getVendorType(),
                    input.getWdmBand(), input.getGrid(), input.getBandwidth(), cardTypes);
        }
        if (siteNodeInput.getProtectionPeerRole() == null) {
            cardTypes = nodeLayout.resolveMainCardClasses(cardTypes);
        }
        List<String> slaveCardTypes = siteModelConfig.getSlaveCardTypes(siteNodeInput.getNodeType(), input.getGrid(),
                input.getIsProtected(), input.getLinkModel(), input.getWdmBand());
        // Bone2.0 OMSP uses a second TILA; the legacy site model names that peer as OA.
        slaveCardTypes = ByteDance2CardChainResolver.resolveProtectedPeers(input.getVendorName(), input.getVendorType(),
                slaveCardTypes);
        if (nodeLayout.requiresDedicatedProtectionNode()) {
            slaveCardTypes = Collections.emptyList();
        }
        OcmGridGroup ocmGripGroupDefinition = siteModelConfig.getOcmGridGroup(input.getGrid());

        SiteNodeInfo siteNodeInfo = siteNodeService.createSiteNode(input.getGrid(), input.getWdmBand(), node,
                cardTypes, slaveCardTypes, siteNodeInput.getCardTypeVendors(),
                siteNodeInput.isRamanOnLeft(), siteNodeInput.isRamanOnRight(), neInfo, input.getProtectionType(),
                ocmGripGroupDefinition, reversed, nodeLayout,
                siteNodeInput.getProtectionPeerRole());
        return siteNodeInfo;
    }

    private AllocatedSiteInfo allocate(SiteInput input, NeInfo neInfo, List<SiteNodeInput> nodes, SiteNodeInfo leftMainNode, SiteNodeInfo rightMainNode, Set<String> busyIds, RoutingType routingType)
            throws NeDesignerException {
        List<Node> outputNodes = new ArrayList<>();
        List<Node> outputProtectedNodes = new ArrayList<>();
        List<CrossConnections> xcs = new ArrayList<>();
        List<Link> links = new ArrayList<>();
//        Set<String> busyIds = new HashSet<>();
        int nodeSize = nodes == null ? 0 : nodes.size();
        SiteNodeInfo previousNode = leftMainNode;
        SiteResourceLayout resourceLayout = SiteResourceLayoutFactory.create(input);

        List<SiteNodeInfo> protectedNodes = new ArrayList<>();

        //Add slave link,slave xc for leftProtectedNode
        if (routingType.equals(RoutingType.Slave)) {
            links.addAll(leftMainNode.slaveLinks);
            xcs.addAll(leftMainNode.slaveXcs);
        } else if (routingType.equals(RoutingType.Third)) {
            links.addAll(leftMainNode.thirdLinks);
            xcs.addAll(leftMainNode.thirdXcs);
        }

        for (int i = 0; i < nodeSize; i++) {
            SiteNodeInput siteNodeInput = nodes.get(i);
            SiteNodeInfo siteNodeInfo;
            if (i == nodeSize - 1) {
                siteNodeInfo = createSingleSiteNodeInfo(input, siteNodeInput, neInfo, true);
            } else {
                siteNodeInfo = createSingleSiteNodeInfo(input, siteNodeInput, neInfo, false);
            }
            if (routingType.equals(RoutingType.Main)) {// means main
                if (i == 0 || i == nodeSize - 1) {//use the first and last node as protectedNode by default
                    protectedNodes.add(siteNodeInfo);
                }
            }

            //create link between node
            if (previousNode != null) {
                CardTps dest = siteNodeInfo.getLeftPeer();
                CardTps source = i == 0
                        ? (routingType == RoutingType.Third ? previousNode.getThirdRightPeer()
                                : previousNode.getSlaveRightPeer())
                        : previousNode.getRightPeer();
                try {
                    Map<String, List<ExternalLinkTo>> fromToMap = neInfo.getExternalLinkInfoFromTo(source.getCard().getCardType(), dest.getCard()
                            .getCardType());
                    fromToMap = RamanSupport.selectSpanLinks(fromToMap,
                            source.getCard().getCardType(), dest.getCard().getCardType());
                    fromToMap = selectRouteFmuxTilaLinks(resourceLayout, fromToMap, source, dest,
                            routingType);
                    if (isMainRouteZEnd(routingType, i, nodeSize)) {
                        fromToMap = resourceLayout.selectZEndExternalLinks(fromToMap,
                                dest.getCard().getCardType());
                    }

                    Map<String, String> srcPortNameTPMap;
                    Map<String, String> dstPortNameTPMap;
                    if (i == 0) {//slave
                        srcPortNameTPMap = getRoutePortMap(source, routingType);
                        if (dest.getSlavePortNameTpMap() != null && !dest.getSlavePortNameTpMap().isEmpty()) {
                            dstPortNameTPMap = dest.slavePortNameTpMap;
                        } else {
                            dstPortNameTPMap = dest.portNameTpMap;
                        }
                    } else {
                        srcPortNameTPMap = source.portNameTpMap;
                        dstPortNameTPMap = dest.portNameTpMap;
                    }
                    LinkOutput linkOutPut = createRouteLinks(input, previousNode.getNode().getNodeId().getValue(),
                            siteNodeInfo.getNode().getNodeId().getValue(), fromToMap, srcPortNameTPMap,
                            dstPortNameTPMap, busyIds);
                    links.addAll(linkOutPut.getLinks());
                    Node srcNode = neNodeRepo.updateInternalLink_And_BusyTp(previousNode.getNode(), linkOutPut.getInternalLinks(), linkOutPut.getBusyTpIds());

                    if (i == 0) {
                        outputProtectedNodes.add(srcNode);
                    } else {
                        outputNodes.add(srcNode);
                    }

                    List<InternalLinks> destInternalLinks = linkOutPut.getLinks().stream()
                            .map(link -> linkService.createInternalLink(siteNodeInfo.getNode().getNodeId().getValue(), link)).collect(Collectors.toList());
                    Node destNode = neNodeRepo.updateInternalLink_And_BusyTp(siteNodeInfo.getNode(), destInternalLinks, linkOutPut.getBusyTpIds());
                    siteNodeInfo.setNode(destNode);


                } catch (Exception e) {
                    String msg = String.format("Failed to create link between node, from %s to %s ", source.getCard().getCardType(), dest.getCard()
                            .getCardType());
                    log.error(msg, e);
                    throw new NeDesignerException(msg, e);
                }
            }

            xcs.addAll(siteNodeInfo.getXcs());
            links.addAll(siteNodeInfo.getLinks());
            previousNode = siteNodeInfo;
        }

        //create link to the right main node, if exists
        if (rightMainNode != null) {
            CardTps source = nodeSize == 0 ? (routingType.equals(RoutingType.Slave) ? previousNode.getSlaveRightPeer() : previousNode.getThirdRightPeer()) : previousNode
                    .getRightPeer();
            CardTps dest = routingType.equals(RoutingType.Slave) ? rightMainNode.getSlaveLeftPeer() : rightMainNode.getThirdLeftPeer();
            try {
                Map<String, List<ExternalLinkTo>> fromToMap = neInfo.getExternalLinkInfoFromTo(source.getCard().getCardType(), dest.getCard().getCardType());
                fromToMap = RamanSupport.selectSpanLinks(fromToMap,
                        source.getCard().getCardType(), dest.getCard().getCardType());
                fromToMap = selectRouteFmuxTilaLinks(resourceLayout, fromToMap, source, dest,
                        routingType);
                fromToMap = resourceLayout.selectZEndExternalLinks(fromToMap,
                        dest.getCard().getCardType());
                Map<String, String> srcPortNameTPMap;
                Map<String, String> dstPortNameTPMap;
                //slave
                srcPortNameTPMap = getRoutePortMap(source, routingType);
                dstPortNameTPMap = getRoutePortMap(dest, routingType);

                LinkOutput linkOutPut = createRouteLinks(input, previousNode.getNode().getNodeId().getValue(),
                        rightMainNode.getNode().getNodeId().getValue(), fromToMap, srcPortNameTPMap,
                        dstPortNameTPMap, busyIds);

                links.addAll(linkOutPut.getLinks());

                Node srcNode = neNodeRepo.updateInternalLink_And_BusyTp(previousNode.getNode(), linkOutPut.getInternalLinks(), linkOutPut.getBusyTpIds());
                previousNode.setNode(srcNode);

                List<InternalLinks> destInternalLinks = linkOutPut.getLinks().stream()
                        .map(link -> linkService.createInternalLink(rightMainNode.getNode().getNodeId().getValue(), link)).collect(Collectors.toList());
                Node destNode = neNodeRepo.updateInternalLink_And_BusyTp(rightMainNode.getNode(), destInternalLinks, linkOutPut.getBusyTpIds());
                outputProtectedNodes.add(destNode);

            } catch (Exception e) {
                String msg = String.format("Failed to create link between node, from %s to %s ", source.getCard().getCardType(), dest.getCard().getCardType());
                log.error(msg, e);
                throw new NeDesignerException(msg, e);
            }
            if (routingType.equals(RoutingType.Slave)) {
                links.addAll(rightMainNode.slaveLinks);
                xcs.addAll(rightMainNode.slaveXcs);
            } else if (routingType.equals(RoutingType.Third)) {
                links.addAll(rightMainNode.thirdLinks);
                xcs.addAll(rightMainNode.thirdXcs);
            }
        }
        if (nodeSize == 0) {
            outputProtectedNodes.add(0, previousNode.getNode());
        } else {
            outputNodes.add(previousNode.getNode());
        }

        return AllocatedSiteInfo.builder().siteRoute(Route.builder().links(links).xcs(xcs).nodes(outputNodes).build()).
                protectedNodes(protectedNodes).
                updatedProtectedNodes(outputProtectedNodes).
                build();
    }

    static boolean isMainRouteZEnd(RoutingType routingType, int nodeIndex, int nodeSize) {
        return routingType == RoutingType.Main && nodeIndex == nodeSize - 1;
    }

    private Map<String, List<ExternalLinkTo>> selectRouteFmuxTilaLinks(
            SiteResourceLayout resourceLayout, Map<String, List<ExternalLinkTo>> fromToMap,
            CardTps source, CardTps dest, RoutingType routingType) {
        String fmuxPort = getRouteFmuxPort(routingType);
        if (fmuxPort == null) {
            return fromToMap;
        }
        // Dedicated Bone2.0 Flex32 protection nodes are separate NEs, so their FMUX/TILA
        // access links are created by SiteRepo rather than SiteNodeService.
        return resourceLayout.selectFmuxTilaLinks(fromToMap, source.getCard().getCardType(),
                dest.getCard().getCardType(), fmuxPort);
    }

    private String getRouteFmuxPort(RoutingType routingType) {
        if (routingType == RoutingType.Slave) {
            return "SIGB";
        }
        return null;
    }

    private Map<String, String> getRoutePortMap(CardTps cardTps, RoutingType routingType) {
        if (routingType == RoutingType.Third && cardTps.getThirdPortNameTpMap() != null
                && !cardTps.getThirdPortNameTpMap().isEmpty()) {
            return cardTps.getThirdPortNameTpMap();
        }
        if (routingType == RoutingType.Slave && cardTps.getSlavePortNameTpMap() != null
                && !cardTps.getSlavePortNameTpMap().isEmpty()) {
            return cardTps.getSlavePortNameTpMap();
        }
        return cardTps.getPortNameTpMap();
    }

    private LinkOutput createRouteLinks(SiteInput input, String sourceNodeId, String destNodeId,
                                        Map<String, List<ExternalLinkTo>> fromToMap,
                                        Map<String, String> srcPortNameTPMap,
                                        Map<String, String> dstPortNameTPMap,
                                        Set<String> busyIds) throws NeDesignerException {
        // Bone2.0 protection access between OLP3_3 and local TILA is an OMS segment;
        // only links that leave the site remain OTS fiber spans.
        if (isByteDance2(input) && isSameSite(sourceNodeId, destNodeId)) {
            return linkService.createOmsLinks(sourceNodeId, fromToMap, srcPortNameTPMap, dstPortNameTPMap, busyIds);
        }
        return linkService.createOtsLinks(sourceNodeId, fromToMap, srcPortNameTPMap, dstPortNameTPMap, busyIds);
    }

    private boolean isByteDance2(SiteInput input) {
        return input != null && ProductTypeResolver.isBone20ProductType(input.getVendorName(), input.getVendorType());
    }

    private boolean isSameSite(String sourceNodeId, String destNodeId) {
        return getSiteId(sourceNodeId).equals(getSiteId(destNodeId));
    }

    private String getSiteId(String nodeId) {
        int neIndex = nodeId == null ? -1 : nodeId.indexOf("#Ne-");
        return neIndex > 0 ? nodeId.substring(0, neIndex) : String.valueOf(nodeId);
    }


    private NeInfo getNeInfo(SiteInput input) throws NeDesignerException {
        return neInfoConfig.getNeInfo(input.getVendorName(), input.getVendorType(), NodeType.OD.name());
    }

}
