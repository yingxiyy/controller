/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTpService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTransceiverService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtXcService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelUtils;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.Port;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLine;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel3Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel3InputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel3Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.Vendor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.VendorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.NewOchTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.NewOchTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfosBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.LinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.LinksKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnle.segment.info.TunnelSegments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TunnelGenerator {

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private LinkRepo linkRepo;

    @Autowired
    private NodeUtils nodeUtils;

    @Autowired
    private OtXcService otXcService;

    @Autowired
    private OtTpService otTpService;

    @Autowired
    private TunnelUtils tunnelUtils;

    @Autowired
    private TpRepo tpRepo;

    @Autowired
    private NeNodeRepo neNodeRepo;

    @Autowired
    private NEInfoConfig neInfoConfig;

    @Autowired
    private EquipmentRepo equipmentRepo;
    @Autowired
    private OtTransceiverService otTransceiverService;
    @Autowired
    private JsonOutputer jsonOutputer;

    public void doIt(TakeoverTunnelsInput input, TaskInfoMessage taskInfoMessage) throws NeDesignerException {

        //init param
        ParamTakeover paramTakeover = getParamTakeover(input);

        //init var to store temp info
        Map<String, Node> totalNodesMap = new HashMap<>();//key is ip
        if (paramTakeover.isBaseOnNe) {
            updateParamsByExistedNode(paramTakeover, totalNodesMap);
            paramTakeover.parseUpdate();
        }

        paramTakeover.tpcNeInfo = neInfoConfig.getNeInfo(paramTakeover.vendorName, paramTakeover.productType, NodeType.TPC4.name());

        OchRoute ochRoute = createOchRoute(paramTakeover, totalNodesMap);
        List<TpcRoute> tpcRoutes = createTpcRoutes(ochRoute, paramTakeover, totalNodesMap);

        CreateTunnel3Input createTunnel3Input = constructCreateTunnelInput(ochRoute, tpcRoutes, totalNodesMap, input, paramTakeover);
        log.debug("The createTunnel3Input from takeover is: {}", jsonOutputer.format(createTunnel3Input));
        TunnelCreator3 tunnelCreator = new TunnelCreator3();
        CreateTunnel3Output output = tunnelCreator.setTaskInfo(taskInfoMessage).doIt(createTunnel3Input);
    }


    protected ParamTakeover getParamTakeover(TakeoverTunnelsInput input) {
        ParamTakeover paramTakeover = new ParamTakeover();
        //因为基类ParamCreaionBasic会对srcSite和destSite做validate， 所以这里在parser之前，稍微改造一下
        String srcSiteId = PhysicalNodeIdNamingRule.getSiteId(getNodeByIp(paramTakeover.primarySegments.get(0).getSourceIp()).getNodeId().getValue());
        String destSiteId = PhysicalNodeIdNamingRule.getSiteId(
                getNodeByIp(paramTakeover.primarySegments.get(paramTakeover.primarySegments.size() - 1).getDestinationIp()).getNodeId().getValue());
        TakeoverTunnelsInput newInput = new TakeoverTunnelsInputBuilder(input).setSrcSite(new NodeId(srcSiteId)).setDstSite(new NodeId(destSiteId)).build();
        paramTakeover.parserTakeOver(newInput);
        return paramTakeover;
    }

    /**
     * 只要找到一个node可以更新param需要的所有信息，就返回
     *
     * @param paramTakeover
     * @param totalNodesMap
     */
    private void updateParamsByExistedNode(ParamTakeover paramTakeover, Map<String, Node> totalNodesMap) throws NeDesignerException {
        for (TunnelSegments segment : paramTakeover.primarySegments) {
            Node srcNode = getNodeByIp(segment.getSourceIp());
            if (srcNode != null) {
                totalNodesMap.put(segment.getSourceIp(), srcNode);
                Boolean nodeCanUpdateParams = updateParams(paramTakeover, srcNode, segment.getSourceTp());
                if (nodeCanUpdateParams) {
                    return;
                }
            }

            Node destNode = getNodeByIp(segment.getDestinationIp());
            if (destNode != null) {
                totalNodesMap.put(segment.getDestinationIp(), destNode);
                Boolean nodeHasParamInfo = updateParams(paramTakeover, destNode, segment.getDestinationTp());
                if (nodeHasParamInfo) {
                    return;
                }
            }
        }
        if (paramTakeover.secondarySegments != null) {
            for (TunnelSegments segment : paramTakeover.secondarySegments) {
                Node srcNode = getNodeByIp(segment.getSourceIp());
                if (srcNode != null) {
                    totalNodesMap.put(segment.getSourceIp(), srcNode);
                    Boolean nodeCanUpdateParams = updateParams(paramTakeover, srcNode, segment.getSourceTp());
                    if (nodeCanUpdateParams) {
                        return;
                    }
                }

                Node destNode = getNodeByIp(segment.getDestinationIp());
                if (destNode != null) {
                    totalNodesMap.put(segment.getDestinationIp(), destNode);
                    Boolean nodeHasParamInfo = updateParams(paramTakeover, destNode, segment.getDestinationTp());
                    if (nodeHasParamInfo) {
                        return;
                    }
                }
            }

        }

    }

    /**
     * 1.当前逻辑是，遍历所有segment，寻找可用的L口，C口的频率，validate， update param
     * <p>
     * 2. 更新param里面的tp为
     * <p>
     * 3，条件2的信息会覆盖条件1的信息。
     *
     * @param paramTakeover
     * @param node
     * @param tpFriendlyName
     * @return
     */
    protected Boolean updateParams(ParamTakeover paramTakeover, Node node, String tpFriendlyName) throws NeDesignerException {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical = node.getAugmentation(Node1.class)
                .getPhysical();

        List<TerminationPoint> tpList = node.getTerminationPoint();
        if (tpList == null || tpList.isEmpty()) {
            return false;
        }

        Optional<TerminationPoint> tpOptional = tpList.stream()
                .filter(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName().equals(tpFriendlyName))
                .findAny();
        if (!tpOptional.isPresent()) {
            return false;
        }
        TerminationPoint tp = tpOptional.get();
        Physical tpPhysical = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        paramTakeover.vendorName = physical.getVendorName();
        paramTakeover.productType = physical.getVendorType();
        paramTakeover.setRiskGroupName(physical.getRiskGroupName());
        paramTakeover.setPlaneName(physical.getPlaneName());

        if (tpPhysical.getPortType().equals(PortType.OTULine)) {
            Class<? extends SignalProtocolType> linePortTpSignalRate = tpPhysical.getOtuLine() == null ? null : tpPhysical.getOtuLine().getSignalRate();
            if (linePortTpSignalRate != null) {
                //update Line Port related params
                paramTakeover.setLinePortSignalRate(linePortTpSignalRate);
                paramTakeover.setCardType(nodeUtils.getCardInfoByTpId(node, tp.getTpId().getValue()).getCardType());

                //check if any C port TP match, then update params
                String equipId = tpPhysical.getEquipmentRef();
                Optional<TerminationPoint> cPortTpOptinal = tpList.stream()
                        .filter(cPortTp -> cPortTp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef().equals(equipId)
                                && cPortTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuClient() != null
                                && cPortTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuClient().getSignalRate() != null
                                && cPortTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuClient().getClient() != null
                                && cPortTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuClient().getClient().getEthComplianceCode() != null
                        ).findAny();
                if (!cPortTpOptinal.isPresent()) {
                    return false;
                }
                TerminationPoint cPortTp = cPortTpOptinal.get();
                paramTakeover.setTunnelSignalRate(cPortTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuClient().getSignalRate());
                paramTakeover.setClientMedium(cPortTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuClient().getClient().getEthComplianceCode());
                return true;
            }
        }
        return false;
    }


    private CreateTunnel3Input constructCreateTunnelInput(OchRoute ochRoute, List<TpcRoute> tpcRoutes, Map<String, Node> totalNodesMap, TakeoverTunnelsInput takeoverTunnelsInput,
                                                          ParamTakeover paramTakeover) {
        TunnelRouteInfos tunnelRouteInfo = new TunnelRouteInfosBuilder().setOchRoute(ochRoute).setTpcRoute(tpcRoutes).build();
        NewOchTunnel newOchTunnel = new NewOchTunnelBuilder()
                .setTunnelRouteInfos(Arrays.asList(tunnelRouteInfo)).build();
        Vendor vendor = new VendorBuilder().setVendorName(paramTakeover.vendorName).setProductType(paramTakeover.productType)
                .setNewOchTunnel(newOchTunnel)
                .build();
        List<Nodes> nodes = totalNodesMap.values().stream().map(node -> RouteYangDataConverter.getTunnelNodes(node)).collect(Collectors.toList());
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.create.tunnel._3.input.TunnelAllocateResult2 tunnelAllocateResult = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.create.tunnel._3.input.TunnelAllocateResult2Builder()
                .setVendor(Arrays.asList(vendor))
                .setNodes(nodes).build();

        return new CreateTunnel3InputBuilder()
                .setTunnelAllocateResult2(tunnelAllocateResult)
                .setBundleNumber(takeoverTunnelsInput.getBundleNumber())
                .setClientPhysicalMedium(paramTakeover.clientMedium)
                .setLineSignalRate(paramTakeover.getLinePortSignalRate())
                .setProtectionType(paramTakeover.protectionType)
                .setSrcSite(takeoverTunnelsInput.getSrcSite())
                .setDstSite(takeoverTunnelsInput.getDstSite())
                .setUiInfo(takeoverTunnelsInput.getUiInfo())
                .setSignalRate(paramTakeover.tunnelSignalRate)
                .setCardType(paramTakeover.cardType)
                .setRiskGroupInfo(paramTakeover.riskGroupInfos)
                .setCustomer(paramTakeover.getCustomer())
                .setOrderId(paramTakeover.getOrderId())
                .build();
    }

    protected OchRoute createOchRoute(ParamTakeover param, Map<String, Node> totalNodesMap) throws NeDesignerException {

//        //create links,internal links for och route
//        List<Link> ochPrimaryLinks = handleLinksInOchRoute(param, param.primarySegments, totalNodesMap);
//
//        //validate if param get all info by segments
//        if (param.ochEndSegment == null) {
//            String msg = String.format("Failed to find segment by dest site:%s", param.getDesSite().getNodeId().getValue());
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
//        }
//        if (param.ochSrcTp == null) {
//            String msg = String.format("Failed to get och start tp.");
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
//        }
//        if (param.ochSrcTp == null) {
//            String msg = String.format("Failed to get och end tp.");
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
//        }
//
//        //create secondary link, if protected
//        List<Link> ochSecondLinks = null;
//        if (param.secondarySegments != null) {
//            ochSecondLinks = handleLinksInOchRoute(param, param.secondarySegments, totalNodesMap);
//        }
//
//        List<CrossConnections> opXcs = new ArrayList<>();
//        if (ochSecondLinks != null) {//protected
//            //find or create start OP xc
//            CrossConnections startOp6Xc = getOrCreateOPXc(param.ochSrcSegment.getDestinationTp(), param.ochSrcSegment.getDestinationIp(), totalNodesMap);
//            opXcs.add(startOp6Xc);
//
//            //find or create end OP xc
//            CrossConnections endOp6Xc = getOrCreateOPXc(param.ochEndSegment.getSourceTp(), param.ochEndSegment.getSourceIp(), totalNodesMap);
//            opXcs.add(endOp6Xc);
//        }
//        return constructOchRoute(ochPrimaryLinks, ochSecondLinks, opXcs, param.ochSrcTp, param.ochDestTp);
        return null;
    }


    private List<Links> yangConvertToLinksList(List<Link> links) {
        if (links == null) {
            return null;
        }

        return links.stream().map(item -> yangConvertToLinks(item)).collect(Collectors.toList());
    }

    private Links yangConvertToLinks(Link link) {
        return new LinksBuilder().setLinkId(link.getLinkId())
                .setKey(new LinksKey(link.getKey().getLinkId()))
                .setSource(link.getSource())
                .setDestination(link.getDestination())
                .setPhysical(link.getAugmentation(Link1.class).getPhysical())
                .build();
    }

    protected OchRoute constructOchRoute(Pair<List<CrossConnections>, List<Link>> ochPrimaryRoute, Pair<List<CrossConnections>, List<Link>> ochSecondRoute, String ochSrcTp, String ochDstTp) {
//        String ochSrcTp = ochPrimaryRoute.get(0).getSource().getSourceTp().getValue();//OCH start L port TP
//        String ochDstTp = ochPrimaryRoute.get(ochPrimaryRoute.size() - 1).getSource().getSourceTp().getValue();//OCH end L port
        Primary primary = new PrimaryBuilder().setCrossConnections(ochPrimaryRoute.getLeft())
                .setLinks(yangConvertToLinksList(ochPrimaryRoute.getRight()))
                .build();

        Secondary secondary = null;
        if (ochSecondRoute != null) {
            secondary = new SecondaryBuilder().setCrossConnections(ochSecondRoute.getLeft())
                    .setLinks(yangConvertToLinksList(ochSecondRoute.getRight()))
                    .build();
        }
        return new OchRouteBuilder().setSourceTp(ochSrcTp).setDestTp(ochDstTp).setPrimary(primary).setSecondary(secondary).build();
    }


    /**
     * 1. If basedOnNe is true, then when mismatch(inePortSignalRate) happen between NE an input, use NE as base
     * <p>
     * 2. But if  NEs have different config, throw exception
     *
     * @param param
     * @param tpFriendlyName
     * @param nodeIp
     * @param totalNodesMap
     * @param busyTpSet
     * @return
     */

    protected TerminationPoint validateAndUpdateTp(ParamTakeover param, String tpFriendlyName, String nodeIp, Map<String, Node> totalNodesMap, Set<String> busyTpSet)
            throws NeDesignerException {

        //fetch node
        Node node = totalNodesMap.get(nodeIp);
        if (node == null) {
            node = getNodeByIp(nodeIp);
        }

        List<TerminationPoint> tpList = node.getTerminationPoint();
        TerminationPoint targetTp = null;
        if (tpList == null) {
            node = neNodeRepo.refreshEmptyNode(param.vendorName, param.productType, param.tpcNeInfo, node, NodeType.TPC4, param.getPlaneName(), param.getPlaneId(), param
                    .getRiskGroupName());
            tpList = node.getTerminationPoint();
        } else {
            for (int i = 0; i < tpList.size(); i++) {
                TerminationPoint tp = tpList.get(i);
                if (!tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName().equals(tpFriendlyName)) {
                    continue;
                }

                targetTp = tp;
                String tpId = tp.getTpId().getValue();

                //check if TP is available(not busy)
                if (busyTpSet.contains(tpId)) {
                    return targetTp;//此TP已经处理过了
                }
                //todo: temp comment
//                if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().equals(ConnectionStatus.Busy)) {
//                    String msg = String.format("Failed to takeover, because Tp :%s in node:%s is busy", tpFriendlyName, nodeIp);
//                    log.error(msg);
//                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
//                }

                tpList.set(i, getUpdatedBusyTp(tp, param));
                break;
            }
        }
        Equipments newEquipment = null;
        if (targetTp == null) {//not found
            //create equip
            String cardType = param.getCardTypeByTpFriendlyName(tpFriendlyName);
            Integer newEquipSlot = param.getEquipSlotBypFriendlyName(tpFriendlyName);
            //validate slot and cardType
            Equipments existedEquipOfThisSlot = nodeUtils.getLineCardEquipBySlot(node, newEquipSlot);
            if (existedEquipOfThisSlot != null) {
                log.error("Failed to create equip from tpFriendlyName:{}, for node:{}, because existed equip found:{}", tpFriendlyName, node, existedEquipOfThisSlot);
                String msg = String.format("Failed to create equip from tpFriendlyName:%s for node:%s, because equip :%s existed already, but matched TP not found.", tpFriendlyName, nodeIp,
                        existedEquipOfThisSlot.getEquipmentId());
                log.error(msg);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
            }
            Card cardInfo = param.tpcNeInfo.getCardByCardVendor(cardType);
            if (!cardInfo.getPossibleSlot().contains(newEquipSlot)) {
                String msg = String.format("Failed to create equip from tpFriendlyName:%s for node:%s, because slot:%d is invalid, valid slots should be:%s",
                        tpFriendlyName, nodeIp, newEquipSlot, cardInfo.getPossibleSlot());
                log.error(msg);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
            }

            newEquipment = equipmentRepo.createEquipment(node.getNodeId().getValue(), newEquipSlot, cardType, param.tpcNeInfo);

            //create TP
            List<Port> ports = cardInfo.getPorts();
            ports.sort(Comparator.comparing(Port::getName));
            for (Port port : ports) {
                List<String> portNames = NeInfoUtil.getNameList(port.getName());
                List<String> portIndexs = NeInfoUtil.getNameList(port.getIndex());

                int size = portNames.size();
                for (int i = 0; i < size; i++) {
                    String portName = portNames.get(i);
                    String portIndex = portIndexs.get(i);
                    TerminationPoint newTp = tpRepo.createTp(newEquipment, cardType, portName, port, i, Integer.parseInt(portIndex));
                    if (tpFriendlyName.equals(newTp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName())) {
                        newTp = getUpdatedBusyTp(newTp, param);
                        targetTp = newTp;
                    }
                    tpList.add(newTp);
                }
            }
        }

        //fresh node
        node = neNodeRepo.refreshNodeBy_Tps_newEquip_checkStuffed(node, tpList, newEquipment);
        totalNodesMap.put(nodeIp, node);
        busyTpSet.add(targetTp.getTpId().getValue());
        return targetTp;
    }

    private TerminationPoint getUpdatedBusyTp(TerminationPoint tp, ParamTakeover param) {
        TerminationPoint newBusyTp;
        //check L PORT TP lineSignalRate
        Physical tpPhysical = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        if (tpPhysical.getPortType().equals(PortType.OTULine)) {
            OtuLine tpOtuLine = tpPhysical.getOtuLine();
            Class<? extends SignalProtocolType> tpSignalRate = tpOtuLine == null ? null : tpOtuLine.getSignalRate();
            if (tpSignalRate == null) {
                tpSignalRate = param.getLinePortSignalRate();

            } else if (!tpSignalRate.equals(param.getLinePortSignalRate())) {
                String msg;
                if (!param.isBaseOnNe) {
                    msg = String.format("Invalid input lineSignalRate:%s, because found existed lineSignalRate is %s, in tp:%s", param.getLinePortSignalRate(),
                            tpSignalRate, tp.getTpId().getValue());
                } else {
                    msg = String.format("Different lineSignalRate %s and %s, in NEs", param.getLinePortSignalRate(), tpSignalRate);
                }
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
            }
            newBusyTp = otTpService.createBusyLPortTp(tp, tpSignalRate, param.centFreq, param.outputPower, null, null);
//            newBusyTp = tpRepo.getLookbackEnableTp(newBusyTp);
        } else {
            newBusyTp = tpRepo.getBusyTp(tp);
        }
        return newBusyTp;
    }


    private Node getNodeByIp(String ip) {

        if (ip == null || ip.isEmpty()) {
            log.error("Invalid segment:{}, no valid ip provided.");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "ip is mandatory for tunnelSegment");
        }
        Node node = phyNodeDao.getConfigPhyNodeByIp(ip);
        if (node == null) {
            log.info("Failed to find phy node by ip:{}.", ip);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to find phy node by ip:" + ip);
//            node = neNodeRepo.createEmptyNode()

        }
        return node;
    }

    /**
     * note: 当存在XC时，则默认不需要创建transceiver了
     *
     * @param ochRoute
     * @param param
     * @param totalNodesMap
     * @return
     * @throws NeDesignerException
     */
    public List<TpcRoute> createTpcRoutes(OchRoute ochRoute, ParamTakeover param, Map<String, Node> totalNodesMap)
            throws NeDesignerException {

        String tpcStartNodeIp = param.ochSrcSegment.getSourceIp();
        Node startOtNode = totalNodesMap.get(tpcStartNodeIp);

        String tpcEndNodeIp = param.ochEndSegment.getDestinationIp();
        Node endOtNode = totalNodesMap.get(tpcEndNodeIp);

        String startLPortTpId = ochRoute.getSourceTp();
        String endLPortTpId = ochRoute.getDestTp();

        List<CrossConnections> existedStartXcs = startOtNode.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                .filter(xc -> xc.getCrossConnectionId().getValue().contains(param.ochSrcTp) && xc.getDirection().equals(LinkDirection.Bidirection)).collect(
                        Collectors.toList());
        Set<String> busyCPortTpsStart = existedStartXcs.stream().map(xc -> xc.getSourceTp().get(0).getTpRef().getValue()).collect(Collectors.toSet());
        List<CrossConnections> existedEndXcs = endOtNode.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                .filter(xc -> xc.getCrossConnectionId().getValue().contains(param.ochDestTp) && xc.getDirection().equals(LinkDirection.Bidirection)).collect(
                        Collectors.toList());
        Set<String> busyCPortTpsEnd = existedEndXcs.stream().map(xc -> xc.getSourceTp().get(0).getTpRef().getValue()).collect(Collectors.toSet());

        //validate bundle number
        int existedStartXcsSize = existedStartXcs == null ? 0 : existedStartXcs.size();
        int existedEndXcsSize = existedEndXcs == null ? 0 : existedEndXcs.size();
        if (param.bundleNumber < existedEndXcsSize || param.bundleNumber < existedStartXcsSize) {
            String msg = String.format("Invalid bundle number:%d, because already existed takeover xc:%d and %d,  the bundle number can not less than existed takeover XC number.", param.bundleNumber,
                    existedStartXcs, existedEndXcsSize);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

        List<TpcRoute> tpcRoutes = new ArrayList<>();
        for (int i = 0; i < param.bundleNumber; i++) {

            CrossConnections tpcXcStart;
            if (existedStartXcs.size() > i) {
                tpcXcStart = existedStartXcs.get(i);//note: 当存在XC时，则默认不需要创建transceiver了
            } else {
                tpcXcStart = createTpcXcAndUpdateNode(tpcStartNodeIp, startLPortTpId, busyCPortTpsStart, param, totalNodesMap);
            }

            CrossConnections tpcXcEnd;
            if (existedEndXcs.size() > i) {
                tpcXcEnd = existedEndXcs.get(i);
            } else {
                tpcXcEnd = createTpcXcAndUpdateNode(tpcEndNodeIp, endLPortTpId, busyCPortTpsEnd, param, totalNodesMap);
            }

            TpcRoute tpcRoute = new TpcRouteBuilder()
                    .setSourceTp(tpcXcStart.getSourceTp().get(0).getTpRef().getValue())
                    .setDestTp(tpcXcEnd.getSourceTp().get(0).getTpRef().getValue())
                    .setCrossConnections(Arrays.asList(tpcXcStart, tpcXcEnd)).build();
            tpcRoutes.add(tpcRoute);
        }
        return tpcRoutes;

    }

    private CrossConnections createTpcXcAndUpdateNode(String nodeIp, String lPortTpId, Set<String> busyCPortTps, ParamTakeover param, Map<String, Node> totalNodesMap) throws NeDesignerException {
        Node node = totalNodesMap.get(nodeIp);
        CrossConnections newTpcXc = createTpcXc(param.getLinePortSignalRate(), param.getTunnelSignalRate(), node, lPortTpId, busyCPortTps, param.getServiceType());
        String cPortTpId = newTpcXc.getSourceTp().get(0).getTpRef().getValue();//因为这里是定死了的，C端口永远是source
        List<TerminationPoint> tps = node.getTerminationPoint();
        Card otCardInfo = null;
        for (int i = 0; i < tps.size(); i++) {
            TerminationPoint tp = tps.get(i);
            if (tp.getTpId().getValue().equals(cPortTpId)) {
                OtuClient otuClient = tp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuClient();
                if (otuClient != null) {
                    //check signalRate
                    Class<? extends SignalProtocolType> tpSignalRate = otuClient.getSignalRate();
                    if (tpSignalRate != null && !tpSignalRate.equals(param.tunnelSignalRate)) {
                        String msg;
                        if (!param.isBaseOnNe) {
                            msg = String.format("Invalid input tunnelSignalRate:%s, because found existed tunnelSignalRate is %s, in tp:%s", param.getTunnelSignalRate(),
                                    tpSignalRate, tp.getTpId().getValue());
                        } else {
                            msg = String.format("Different lineSignalRate %s and %s, in NEs", param.getTunnelSignalRate(), tpSignalRate);
                        }
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);

                    }
                    //check clientMedium
                    Class<? extends ETHERNETCOMPLIANCECODE> tpClientMedium = otuClient.getClient()
                            .getEthComplianceCode();
                    if (tpClientMedium != null && !tpClientMedium.equals(param.getClientMedium())) {
                        String msg;
                        if (!param.isBaseOnNe) {
                            msg = String.format("Invalid input clientMediumNE:%s, because found existed clientMediumNE is %s, in tp:%s", param.getClientMedium(),
                                    tpClientMedium, tp.getTpId().getValue());
                        } else {
                            msg = String.format("Different clientMedium %s and %s, in NEs", param.getClientMedium(), tpClientMedium);
                            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
                        }
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);

                    }
                }
                //update C port TP
                tp = otTpService.createBusyCPortTp(tp, param.getTunnelSignalRate(), param.getClientMedium());
                tps.set(i, tp);

                otCardInfo = nodeUtils.getCardInfoByEquipId(node, tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef());
                break;
            }
        }

        //创建transceiver
        if (otCardInfo == null) {
            throw new NeDesignerException("Internal code error, not got otCardInfo when set C port TP:" + cPortTpId);
        }
        List<Equipments> transceivers = otTransceiverService.createTransceiver(node, cPortTpId, lPortTpId, otCardInfo, param.getClientMedium());
        List<Equipments> equipList = nodeUtils.getEquipments(node);
        equipList.addAll(transceivers);
        //fresh node
        List<CrossConnections> xcs = nodeUtils.getXcs(node);
        xcs.add(newTpcXc);
        node = neNodeRepo.refreshNodeByXcsTpsEquips(node, xcs, tps, equipList);
        totalNodesMap.put(nodeIp, node);
        param.cardType = otCardInfo.getCardType();

        return newTpcXc;
    }

    private CrossConnections createTpcXc(Class<? extends SignalProtocolType> lineSignalRate, Class<? extends SignalProtocolType> tunnelSignalRate, Node otNode, String lPortTpId,
                                         Set<String> busyCPortTps, SERVICETYPE servicetype) throws NeDesignerException {

        Card otCardInfo = nodeUtils.getCardInfoByTpId(otNode, lPortTpId);
        String nodeId = otNode.getNodeId().getValue();
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(lPortTpId);
        CrossConnection xcInfo = tunnelUtils.getOtXcInfo(otCardInfo, lPortName, lineSignalRate, tunnelSignalRate, servicetype);

        List<String> cportNames = NeInfoUtil.getNameList(xcInfo.getFrom().getPort());

        for (String cPortName : cportNames) {
            String availableCPortTp = nodeUtils.getCPortTp(cPortName, lPortTpId);
            if (busyCPortTps.contains(availableCPortTp)) {
                continue;
            }
            busyCPortTps.add(availableCPortTp);
            return otXcService.createXC(nodeId, availableCPortTp, lPortTpId, xcInfo);


        }
        log.error("Failed to create tpc xc for tp:{}, because no available C port can picked, with busyCPortTps:{}. Check the NE json config.", lPortTpId, busyCPortTps);
        throw new NeDesignerException("Failed to create tpc XC for L port:" + lPortTpId + " ,Check the NE json config");
    }

}
