/*
 *
 *  * Copyright (c) 2021-2020 Network Flex Any Comp. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package net.flex.dci.otn.controller.allocate.link.site;

import static net.flex.dci.otn.controller.allocate.link.tunnel.ParamCreaionBasic.DEFAULT_OP3_CARD_TYPE;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.common.RouteYangDataConverter;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.RamanSupport;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteNodeInput;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.link.common.CreateSiteLinkParam;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ComputeLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ComputeLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ComputeLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.Segment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.Main;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.Slave;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.Third;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Service
public class SiteLinkComputer {

    @Autowired
    private NeDesigner neDesigner;
    @Autowired
    private NeNodeRepo neNodeRepo;
    @Autowired
    private PhyNodeDao phyNodeDao;

    /**
     * computeSite 生成网元的BOM 信息
     *
     * @param input
     * @return
     * @throws CommonException
     */
    public ComputeLinkOutput doIt(ComputeLinkInput input) throws CommonException {
        log.debug("compute siteLink start...");
        //所有的必要参数在checkInput中都提取，且放在类变量中

        CreateSiteLinkParam param = new CreateSiteLinkParam();
        param.parser(input);

        RouteInfo routeInfo = null;
        try {
            routeInfo = getRouteResource(param);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "compute link error" + e.toString(), e);
        }
        try {
            return constructOutput(routeInfo);
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "convert Yang error" + e.toString(), e);
        }

    }

    static ComputeLinkOutput constructOutput(RouteInfo routeInfo) {
        Main main = RouteYangDataConverter.getMain(routeInfo.getMain());
        Slave slave = RouteYangDataConverter.getSlave(routeInfo.getSlave());
        Third third = RouteYangDataConverter.getThird(routeInfo.getThird());
        return new ComputeLinkOutputBuilder().setMain(main).setSlave(slave).setThird(third)
                .setReturnCode(RpcResultType.Success).build();
    }


    /**
     * 根据输入的segment list
     *
     * @return
     * @throws CommonException
     */
    public RouteInfo getRouteResource(final CreateSiteLinkParam param, final Map<String, Node> ipNodesMap, List<Node> reusedNodesSnapshot) throws CommonException, NeDesignerException {
        log.debug("build site link route info start...");
        List<SiteNodeInput> primaryNodes = new ArrayList<>();
        List<SiteNodeInput> secondaryNodes = new ArrayList<>();
        Collections.sort(param.getSegments(), new SortByIndex());
        validateRamanPairs(param.getSegments());
        Map<RoutingType, List<SiteNodeInput>> designerNodes = prepareDesignerNode(param.getSegments(), ipNodesMap, reusedNodesSnapshot, param);
        // Protection is defined by link-model/product semantics, not only by how many route roles are present.
        boolean isProtected = param.isProtected();

//        String lastNodePId = "";
//        String lastNodeSId = "";
//        boolean isProtected = false;
//
//        for (Segment segment : param.getSegments()) {
//            if (segment.getRole().equals(RoutingType.Main)) {
//                lastNodePId = prepareDesignerNode(primaryNodes, segment, lastNodePId, ipNodesMap, reusedNodesSnapshot, param);
//            } else {
//                isProtected = true;
//                lastNodeSId = prepareDesignerNode(secondaryNodes, segment, lastNodeSId, ipNodesMap, reusedNodesSnapshot, param);
//            }
//        }
//        if (!secondaryNodes.isEmpty()) {
//            secondaryNodes.remove(0);
//            secondaryNodes.remove(secondaryNodes.size() - 1);
//        }

        SiteInput input = SiteInput.builder()
                .nodesMap(designerNodes)
                .vendorName(param.getVendorName())
                .vendorType(param.getVendorType())
                .bandwidth(param.getBandwidth())
                .grid(param.getGrid().getIntValue())
                .isProtected(isProtected)
                .protectionType(param.getProtectionType())
                .plane(param.getPlaneName())
                .planeId(param.getPlaneId())
                .riskGroupName(param.getRiskGroupName())
                .linkModel(param.getLinkModel())
                .wdmBand(param.getLinkGroup())
                .build();

        RouteInfo info = null;
        try {
            info = neDesigner.allocateSite(input);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "neDesigner error" + e.getCause().getMessage(), e);
        }
        log.debug("build site link route info done. {}");
        return info;
    }

    static void validateRamanPairs(List<Segment> segments) throws NeDesignerException {
        for (Segment segment : segments) {
            if (isRamanSegment(segment)) {
                continue;
            }
            RamanSupport.validatePaired(segment.getSourceCardVendor(), segment.getDestinationCardVendor(),
                    segment.getSource(), segment.getDestination());
        }
    }

    private static boolean isRamanSegment(Segment segment) {
        return Boolean.TRUE.equals(segment.isRaman());
    }

    private Map<RoutingType, List<SiteNodeInput>> prepareDesignerNode(List<Segment> segments, final Map<String, Node> ipNodesMap, List<Node> reusedNodesSnapshot, CreateSiteLinkParam param) {

        Map<RoutingType, List<Segment>> groupByRoleSegments = segments.stream().collect(Collectors.groupingBy(Segment::getRole));
        Map<RoutingType, List<SiteNodeInput>> result = new HashMap<>();
        for (Map.Entry<RoutingType, List<Segment>> segEntry : groupByRoleSegments.entrySet()) {
            List<Segment> segs = segEntry.getValue();
            int size = segs.size();
            Collections.sort(segs, Comparator.comparingInt(Segment::getIndex));
            List<SiteNodeInput> segmentNodes = segs.stream().flatMap(segment -> {
                try {
                    return createDesignerSiteNodes(segment, size, ipNodesMap, reusedNodesSnapshot, param).stream();
                } catch (NeDesignerException e) {
                    throw new RuntimeException(e);
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            result.put(segEntry.getKey(), mergeDesignerNodes(segmentNodes));

        }
        return result;
    }

    private List<SiteNodeInput> mergeDesignerNodes(List<SiteNodeInput> segmentNodes) {
        Map<String, SiteNodeInput> merged = new LinkedHashMap<>();
        for (SiteNodeInput node : segmentNodes) {
            SiteNodeInput existing = merged.get(node.getSiteId());
            if (existing == null) {
                merged.put(node.getSiteId(), node);
                continue;
            }
            Set<String> cardVendors = new HashSet<>(existing.getCardTypeVendors());
            cardVendors.addAll(node.getCardTypeVendors());
            existing.setCardTypeVendors(cardVendors);
            existing.setRamanOnLeft(existing.isRamanOnLeft() || node.isRamanOnLeft());
            existing.setRamanOnRight(existing.isRamanOnRight() || node.isRamanOnRight());
            if (existing.getIpNode() == null) {
                existing.setIpNode(node.getIpNode());
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<SiteNodeInput> createDesignerSiteNodes(Segment segment, int size, Map<String, Node> ipNodesMap, List<Node> reusedNodesSnapshot, CreateSiteLinkParam param) throws NeDesignerException {
        List<SiteNodeInput> result = new ArrayList<>();
        boolean ramanSegment = isRamanSegment(segment);
        if (segment.getRole() == RoutingType.Main || segment.getIndex() != 0) {
            result.add(createDesignerSiteNode(segment.getSource(), segment.getSourceNodeType(), segment.getSourceCardVendor(), segment.getSourceNodeIp(), true, false, ramanSegment, ipNodesMap,
                    reusedNodesSnapshot,
                    param));
        }
        if (segment.getRole() == RoutingType.Main || segment.getIndex() != size - 1) {
            result.add(createDesignerSiteNode(segment.getDestination(), segment.getDestinationNodeType(), segment.getDestinationCardVendor(), segment.getDestinationNodeIp(), false, true, ramanSegment,
                    ipNodesMap,
                    reusedNodesSnapshot,
                    param));
        }
        return result;
    }

    private String prepareDesignerNode(List<SiteNodeInput> inputNodeList, Segment segment, String lastNodeId, final Map<String, Node> ipNodesMap, List<Node> reusedNodesSnapshot,
            CreateSiteLinkParam param)
            throws NeDesignerException {
        if (lastNodeId.isEmpty()) {
            inputNodeList.add(
                    createDesignerSiteNode(segment.getSource(), segment.getSourceNodeType(), segment.getSourceCardVendor(), segment.getSourceNodeIp(), ipNodesMap, reusedNodesSnapshot, param));
            inputNodeList.add(createDesignerSiteNode(segment.getDestination(), segment.getDestinationNodeType(), segment.getDestinationCardVendor(), segment.getDestinationNodeIp(), ipNodesMap,
                    reusedNodesSnapshot, param));
            lastNodeId = segment.getDestination();
        } else {
            if (segment.getSource().equals(lastNodeId)) {
                inputNodeList.add(createDesignerSiteNode(segment.getDestination(), segment.getDestinationNodeType(), segment.getDestinationCardVendor(), segment
                                .getDestinationNodeIp(), ipNodesMap,
                        reusedNodesSnapshot, param));
                lastNodeId = segment.getDestination();
            } else {
                inputNodeList.add(createDesignerSiteNode(segment.getSource(), segment.getSourceNodeType(), segment.getSourceCardVendor(), segment.getSourceNodeIp(), ipNodesMap, reusedNodesSnapshot,
                        param));
                lastNodeId = segment.getSource();
            }
        }
        return lastNodeId;
    }

    private SiteNodeInput createDesignerSiteNode(String siteNodeId, LinkTerminationNodeType nodeType, List<String> cardVendors, String nodeIp, final Map<String, Node> ipNodesMap,
            List<Node> reusedNodesSnapshot, CreateSiteLinkParam param)
            throws NeDesignerException {
        return createDesignerSiteNode(siteNodeId, nodeType, cardVendors, nodeIp, false, false, ipNodesMap, reusedNodesSnapshot, param);
    }

    private SiteNodeInput createDesignerSiteNode(String siteNodeId, LinkTerminationNodeType nodeType, List<String> cardVendors, String nodeIp,
            boolean sourceSide, boolean destinationSide, final Map<String, Node> ipNodesMap,
            List<Node> reusedNodesSnapshot, CreateSiteLinkParam param)
            throws NeDesignerException {
        return createDesignerSiteNode(siteNodeId, nodeType, cardVendors, nodeIp, sourceSide, destinationSide, false, ipNodesMap,
                reusedNodesSnapshot, param);
    }

    private SiteNodeInput createDesignerSiteNode(String siteNodeId, LinkTerminationNodeType nodeType, List<String> cardVendors, String nodeIp,
            boolean sourceSide, boolean destinationSide, boolean ramanSegment, final Map<String, Node> ipNodesMap,
            List<Node> reusedNodesSnapshot, CreateSiteLinkParam param)
            throws NeDesignerException {
        NeSubType neSubType;
        String siteNodeType = "";
        switch (nodeType) {
            case SITE:
            case OTM:
            case REG:
                siteNodeType = "T";
                neSubType=NeSubType.OPC_OTM;
                break;
            case ILA:
                siteNodeType = "I";
                neSubType=NeSubType.OPC_ILA;
                break;
            case DGE:
                siteNodeType = "D";
                neSubType=NeSubType.OPC_DGE;
                break;
            case ROADM:
                siteNodeType = "R";
                neSubType=NeSubType.OPC_ROADM;
                break;
            default:
                throw new NeDesignerException("Unsupported nodeType:" + nodeType);
        }
        Node ipNode = null;
        if (nodeIp != null) {
            ipNode = ipNodesMap.get(nodeIp);
            if (ipNode == null) {
                ipNode = getOrCreateNodeWithIp(siteNodeId, nodeIp, reusedNodesSnapshot, param,neSubType);
            }
        }
        Set cardVendorList = cardVendors == null ? new HashSet<>() : new HashSet<>(cardVendors);
        if (!param.getSpareSegment().isEmpty()) {
            cardVendorList.add(DEFAULT_OP3_CARD_TYPE);
        }
        return SiteNodeInput.builder()
                .nodeType(siteNodeType)
                .siteId(siteNodeId)
                .cardTypeVendors(cardVendorList)
                .ramanOnLeft(destinationSide && (ramanSegment || RamanSupport.containsRaman(cardVendorList)))
                .ramanOnRight(sourceSide && (ramanSegment || RamanSupport.containsRaman(cardVendorList)))
                .ipNode(ipNode)
                .neSubType(neSubType)
                .build();
    }

    private Node getOrCreateNodeWithIp(String siteId, String nodeIp, List<Node> reusedNodesSnapshot, CreateSiteLinkParam param, NeSubType neSubType) throws NeDesignerException {
        if (nodeIp == null || nodeIp.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Invalid node ip(null or empty) for " + siteId);
        }
        Node node = phyNodeDao.getConfigPhyNodeByIp(nodeIp);
        if (node != null) {
            if (reusedNodesSnapshot != null) {
                reusedNodesSnapshot.add(node);
            }
            return node;
        }

        return neNodeRepo.createEmptyNodeWithIp(siteId, param.getPlaneName(),param.getPlaneId(), param.getRiskGroupName(), param.getVendorName(), param.getVendorType(), nodeIp, NodeType.OD,neSubType);
    }

    public RouteInfo getRouteResource(CreateSiteLinkParam param) throws NeDesignerException {
        return getRouteResource(param, Collections.EMPTY_MAP, null);
    }


    class SortByIndex implements Comparator<Segment> {

        @Override
        public int compare(Segment arg0, Segment arg1) {
            return arg0.getIndex().compareTo(arg1.getIndex());
        }
    }
}
