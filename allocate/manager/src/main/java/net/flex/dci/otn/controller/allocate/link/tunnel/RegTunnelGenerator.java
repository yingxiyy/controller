/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTpService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtTransceiverService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtXcService;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelUtils;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLine;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnle.segment.info.TunnelSegments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * deprecated, can be removed later
 */
@Service
@Slf4j
public class RegTunnelGenerator extends TunnelGenerator {

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


    @Override
    protected ParamTakeover getParamTakeover(TakeoverTunnelsInput input) {
        ParamTakeover paramTakeover = new RegParamTakeover();
        paramTakeover.parserTakeOver(input);
        return paramTakeover;
    }

    /**
     * 1.当前逻辑是，如果要找到已存在的TP，就会更新params相关信息，厂商这些。
     *
     * 2.只有找到一个具备L口频率，C口频率的TP，才会return true，停止遍历。
     *
     * 3，条件2的信息会覆盖条件1的信息。
     *
     * @param paramTakeover
     * @param node
     * @param tpFriendlyName
     * @return
     */
    @Override
    protected Boolean updateParams(ParamTakeover paramTakeover, Node node, String tpFriendlyName) throws NeDesignerException {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical = node.getAugmentation(Node1.class).getPhysical();

        List<TerminationPoint> tpList = node.getTerminationPoint();
        if (tpList == null || tpList.isEmpty()) {
            return false;
        }

        Optional<TerminationPoint> tpOptional = tpList.stream().filter(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName().equals(tpFriendlyName)).findAny();
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
            OtuLine otuLine = tpPhysical.getOtuLine();
            if (otuLine != null) {
                Class<? extends SignalProtocolType> linePortTpSignalRate = otuLine.getSignalRate();
                FrequencyType centralFreq = otuLine.getCentralFrequency();
                if (centralFreq != null) {
                    paramTakeover.centFreq = centralFreq.getValue();
                }
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
                    OtuClient otuClient = cPortTpOptinal.get().getAugmentation(TerminationPoint1.class).getPhysical().getOtuClient();
                    if (otuClient == null) {
                        return false;
                    }
                    Class<? extends SignalProtocolType> otuClientSignalRate = otuClient.getSignalRate();
                    if (otuClientSignalRate != null) {
                        paramTakeover.setTunnelSignalRate(otuClientSignalRate);
                    }
                    Class<? extends ETHERNETCOMPLIANCECODE> ethComplianceCode = otuClient.getClient() == null ? null : otuClient.getClient().getEthComplianceCode();
                    if (ethComplianceCode != null) {
                        paramTakeover.setClientMedium(ethComplianceCode);
                    }
                    if (centralFreq != null && ethComplianceCode != null && otuClientSignalRate != null) {
                        return true;
                    }

                }
            }

        }
        return false;
    }

    @Override
    protected OchRoute createOchRoute(ParamTakeover param, Map<String, Node> totalNodesMap) throws NeDesignerException {

        Set<String> busyTpSet = new HashSet<>();//used to store busy TP set by this takeover,  to differ from existed busy TP which is invalid

        //create links,internal links,och route for och route
        Pair<List<CrossConnections>, List<Link>> ochPrimaryOchRoute = allocateOchRoute(param, param.primarySegments, totalNodesMap, busyTpSet);

        //validate if param get all info by segments
        if (param.ochEndSegment == null) {
            String msg = String.format("Failed to find segment by dest site:%s", param.getDesSite().getNodeId().getValue());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
        if (param.ochSrcTp == null) {
            String msg = String.format("Failed to get och start tp.");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
        if (param.ochSrcTp == null) {
            String msg = String.format("Failed to get och end tp.");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

        //create secondary och route, if protected
        Pair<List<CrossConnections>, List<Link>> ochSecondOchRoute = null;
        if (param.secondarySegments != null) {
            ochSecondOchRoute = allocateOchRoute(param, param.secondarySegments, totalNodesMap, busyTpSet);
        }

        if (ochSecondOchRoute != null) {//protected
            //find or create start OP xc
            CrossConnections startOp6Xc = getOrCreateOPXc(param.ochSrcSegment.getDestinationTp(), param.ochSrcSegment.getDestinationIp(), totalNodesMap);
            ochPrimaryOchRoute.getLeft().add(0, startOp6Xc);

            //find or create end OP xc
            CrossConnections endOp6Xc = getOrCreateOPXc(param.ochEndSegment.getSourceTp(), param.ochEndSegment.getSourceIp(), totalNodesMap);
            ochPrimaryOchRoute.getLeft().add(endOp6Xc);
        }
        return constructOchRoute(ochPrimaryOchRoute, ochSecondOchRoute, param.ochSrcTp, param.ochDestTp);
    }

    private CrossConnections getOrCreateOPXc(String opSigTpFriendlyName, String nodeIp, Map<String, Node> totalNodesMap) throws NeDesignerException {

        Node node = totalNodesMap.get(nodeIp);

        String opSigPortTpId = nodeUtils.getTpIdByTpFriendlyName(node, opSigTpFriendlyName);
        List<CrossConnections> existedXcs = nodeUtils.getXcs(node);
        Optional<CrossConnections> existedOptional = existedXcs.stream()
                .filter(xc -> xc.getSourceTp().get(0).getTpRef().getValue().equals(opSigPortTpId) || xc.getDestinationTp().get(0).getTpRef().getValue().equals(opSigPortTpId)).findAny();
        if (existedOptional.isPresent()) {
            return existedOptional.get();
        }

        //not found exited xc, need to create
        Card opCardInfo = nodeUtils.getCardInfoByTpId(node, opSigPortTpId);
        CrossConnections newOp6Xc = otXcService.createOp6XCs(opSigPortTpId, node, opCardInfo).get(0);//实际只有一条

        //fresh node
        existedXcs.add(newOp6Xc);
        node = neNodeRepo.refreshNodeByXcs(node, existedXcs);
        totalNodesMap.put(nodeIp, node);

        return newOp6Xc;

    }

    private Pair<List<CrossConnections>, List<Link>> allocateOchRoute(ParamTakeover param, List<TunnelSegments> tunnelSegments, Map<String, Node> totalNodesMap, Set<String> busyTpSet)
            throws NeDesignerException {
        List<Link> links = new ArrayList<>();
        List<CrossConnections> lPortOchXcs = new ArrayList<>();
        int segSize = tunnelSegments.size();
        for (int i = 0; i < segSize; i++) {
            TunnelSegments tunnelSegment = tunnelSegments.get(i);
            //update source
            String sourceIp = tunnelSegment.getSourceIp();
            TerminationPoint srcTp = validateAndUpdateTp(param, tunnelSegment.getSourceTp(), sourceIp, totalNodesMap, busyTpSet);
            String srcTpId = srcTp.getTpId().getValue();

            //update dest
            String destinationIp = tunnelSegment.getDestinationIp();
            TerminationPoint destTp = validateAndUpdateTp(param, tunnelSegment.getDestinationTp(), destinationIp, totalNodesMap, busyTpSet);
            String destTpId = destTp.getTpId().getValue();

            if (i == 0) {
                if (!srcTpId.contains(param.getSrcSite().getNodeId().getValue())) {
                    log.error("The first segment:{} not in the srcSite:{}", tunnelSegment, param.getSrcSite());
                    String msg = String.format("The first segment should be in ths srcSite.");
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
                }
                if (param.ochSrcTp == null) {
                    param.ochSrcTp = srcTpId;
                }
            } else if (param.ochEndSegment == null) {
                if (destTpId.contains(param.getDesSite().getNodeId().getValue())) {//two scenario: 1. Sig-L1, 2, L1-L1
                    if (isLPort(destTpId)) {
                        param.ochEndSegment = tunnelSegment;
                        param.ochDestTp = destTpId;
                    }
                }
            }

            //create link
            Link osLink = linkRepo.createLink(srcTpId, destTpId, LinkType.OsLink);
            links.add(osLink);

            //create internal link
            createInternalLink(sourceIp, totalNodesMap, osLink);
            if (!sourceIp.equals(destinationIp)) {
                createInternalLink(destinationIp, totalNodesMap, osLink);
            }

            //create L-L och xc
            if (nodeUtils.isLPortTp(srcTp) && nodeUtils.isLPortTp(destTp)) {
                lPortOchXcs.add(createLPortOchXc(srcTpId, sourceIp, totalNodesMap));
                lPortOchXcs.add(createLPortOchXc(destTpId, destinationIp, totalNodesMap));
            }
            //add to tp busy set
            busyTpSet.add(srcTpId);
            busyTpSet.add(destTpId);
        }
        return Pair.of(lPortOchXcs, links);
    }

    private boolean isLPort(String tpId) {
        String portName = PhysicalTpIdNamingRule.getPortNameByTpId(tpId);
        return portName.matches("L\\d");
    }


    private void createInternalLink(String nodeIp, Map<String, Node> totalNodesMap, Link osLink) {
        //fetch node
        Node node = totalNodesMap.get(nodeIp);
//        if (node == null) {
//            node = getNodeByIp(nodeIp);
//        }

        InternalLinks newInternalLink = linkRepo.createInternalLink(node.getNodeId().getValue(), osLink);
        List<InternalLinks> existedInternalLinks = nodeUtils.getInternalLinks(node);
        Optional<InternalLinks> existed = existedInternalLinks.stream().filter(internalLink -> internalLink.getLinkRef().equals(newInternalLink)).findAny();
        if (existed.isPresent()) {
            return;
        }

        //fresh node
        existedInternalLinks.add(newInternalLink);
        node = neNodeRepo.refreshNode(node, existedInternalLinks);
        totalNodesMap.put(nodeIp, node);
    }

    private CrossConnections createLPortOchXc(String lPortTpId, String nodeIp, Map<String, Node> totalNodesMap) throws NeDesignerException {

        //fetch node
        Node node = totalNodesMap.get(nodeIp);
        String nodeId = node.getNodeId().getValue();
        Card otCardInfo = nodeUtils.getCardInfoByTpId(node, lPortTpId);
        CrossConnections newOchXc = otXcService.createOchXc(nodeId, lPortTpId, otCardInfo);

        List<CrossConnections> existedXcs = nodeUtils.getXcs(node);
        Optional<CrossConnections> existed = existedXcs.stream().filter(xc -> xc.getCrossConnectionId().getValue().equals(newOchXc.getCrossConnectionId().getValue())).findAny();
        if (existed.isPresent()) {
            return existed.get();
        }

        //fresh node
        existedXcs.add(newOchXc);
        node = neNodeRepo.refreshNodeByXcs(node, existedXcs);
        totalNodesMap.put(nodeIp, node);
        return newOchXc;
    }


}
