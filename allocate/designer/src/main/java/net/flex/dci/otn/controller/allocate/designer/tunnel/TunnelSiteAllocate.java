/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.allocate.common.service.WdmUtilService;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NoAvailableTpException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.SegmentRouteInfo;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class TunnelSiteAllocate {
    public static final String ROUTE_LEG_PRIMARY = "primary";
    public static final String ROUTE_LEG_SECONDARY = "secondary";
    public static final String ROUTE_LEG_THIRD = "third";

    @Autowired
    private WdmUtilService wdmUtilService;
    @Autowired
    private EquipmentRepo equipmentRepo;

    @Autowired
    private NEInfoConfig neInfoConfig;

    @Autowired
    private OlsNodeService olsNodeService;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    TunnelUtils tunnelUtils;

    @Autowired
    NodeUtils nodeUtils;

    @Value("${server.yangModel}")
    private String yangModel;

    /**
     * 1. TP is free
     * <p>
     * 2. TP的频率满足要求。
     * <p>
     * 3. 头尾的TP名字必须一样，比如都为M1D1
     *
     * @param freeFrequency
     * @param srcNe
     * @param desNe
     * @return
     * @throws NeDesignerException
     */
    public ImmutablePair<String, String> pickMuxTpPair(String freeFrequency, Node srcNe, Node desNe) throws NeDesignerException {

        List<TerminationPoint> srcTps = srcNe.getTerminationPoint(); List<TerminationPoint> destTps = desNe.getTerminationPoint();

        //get idle mux TPs in pair
        for (TerminationPoint srcTp : srcTps) {
            if (isAvailableTp(srcTp, freeFrequency, srcNe)) {
                String srcTpKey = PhysicalTpIdNamingRule.getShortTpByTpId(srcTp.getTpId().getValue());

                for (TerminationPoint desTp : destTps) {
                    if (isAvailableTp(desTp, freeFrequency, desNe)) {
                        String desTpKey = PhysicalTpIdNamingRule.getShortTpByTpId(desTp.getTpId().getValue()); if (srcTpKey.equals(desTpKey)) {
                            return new ImmutablePair<String, String>(srcTp.getTpId().getValue(), desTp.getTpId().getValue());
                        }
                    }
                }
            }
        } throw new NeDesignerException("There is no idle MUX TP available.");
    }

    private boolean isAvailableTp(TerminationPoint tp, String freeFrequency, Node node) throws NeDesignerException {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpPhysical = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        if (PortType.MUXChannel != tpPhysical.getPortType()) {
            return false;
        } if (ConnectionStatus.Idle != tpPhysical.getConnectionStatus()) {
            return false;
        }

        String tpSlot = tunnelUtils.getTpSlotBTp(tpPhysical);

        if (tpSlot == null) {
            if (!yangModel.equals(NeYangModel.ByteDance.name())) {
                return true;// for flex mux, any free TP is ok

            } WDM_Band siteLinkWdm = nodeUtils.getWdmBand(node); if (!siteLinkWdm.equals(WDM_Band.C_L)) {
                return true;
            }

            String tpId = tp.getTpId().getValue();
//            if (tpId.endsWith("64") || tpId.endsWith("32")) {
//                return false;
//            }
            WDM_Band wdmBandFre = wdmUtilService.getWdmBandByFre(freeFrequency); WDM_Band wdmBandPort = wdmUtilService.getWdmBandByMdPort(tpId); return wdmBandFre == wdmBandPort;

        }

        return tpSlot.equals(freeFrequency);// for fix MUX, only the same frequency is ok.

    }


    /**
     * 1. create external muxpanel XC/ internal MUX XC
     * <p>
     * 2. create internal Link by osLink
     *
     * @param tpStart e.g. "Site-1621320693433#Ne-1621320704693#LINECARD-1-1#PORT-1-1-M1DI";
     * @param osLink
     * @param siteLink
     * @param frequency e.g.  "/frequency=195975000,196025000"
     * @param centFreq
     * @return
     */
    public SegmentRouteInfo allocateSegment(String tpStart, Link osLink, Link siteLink, String frequency, BigInteger centFreq, Node olsNode) throws NeDesignerException {
        try {

            String siteId = PhysicalTpIdNamingRule.getSiteId(tpStart); String nodeId = olsNode.getNodeId().getValue();

            String equipId = PhysicalTpIdNamingRule.getEquipId(tpStart); Equipments equipment = equipmentRepo.getEquipment(olsNode, equipId);
            // Resolve the card from the model stored on this OD node; legacy nodes fall back to server.yangModel.
            Card card = nodeUtils.getNeInfoByNode(olsNode, NodeType.OD)
                    .getCardByCardVendor(equipmentRepo.getCardVendorType(equipment));

            String xcPortName = PhysicalTpIdNamingRule.getPortNameByTpId(tpStart);

            String fromTpId = tpStart; List<CrossConnections> xcs = new ArrayList<>();

            /*Here is for mux XC only, and as known, mux XC is bidirectional, then handled a little hard coded. If needed, can refactor later.*/
            //create xc
            for (CrossConnection crossConnection : card.getCrossConnections()) {
                List<String> fromNames = NeInfoUtil.getNameList(crossConnection.getFrom().getPort()); List<String> toNames = NeInfoUtil.getNameList(crossConnection.getTo().getPort());

                if (fromNames.contains(xcPortName)) {

                    for (String toName : toNames) {
                        String toTpId = PhysicalTpIdNamingRule.createTPId(equipment, toName);

                        CrossConnections xc = olsNodeService.createXC(crossConnection, nodeId, fromTpId, Arrays.asList(toTpId), frequency, centFreq); xcs.add(xc);
                    } break;
                }
            }

            //create external xc(e.g MUXPANEL to OA); 其实这个OCH的交叉只有一条，只是因为json的定义里面数据结构是list
            for (CrossConnection crossConnection : card.getExternalCrossConnections()) {
                List<String> fromNames = NeInfoUtil.getNameList(crossConnection.getFrom().getPort()); List<String> toNames = NeInfoUtil.getNameList(crossConnection.getTo().getPort());

                if (fromNames.contains(xcPortName)) {

                    for (String toName : toNames) {
                        List<String> toTpId = findTpId(siteId, siteLink, toName); CrossConnections xc = olsNodeService.createXC(crossConnection, nodeId, fromTpId, toTpId, frequency, centFreq);
                        xcs.add(xc);
                    }

                    break;
                }
            }

            Node updatedOlsNode = olsNodeService.updatedOlsNode(olsNode, Arrays.asList(osLink), tpStart, xcs);

            return SegmentRouteInfo.builder().node(updatedOlsNode).xcs(xcs).build();

        } catch (Exception e) {
            String msg = "Failed to create XC from tp: " + tpStart; log.error(msg, e); throw new NeDesignerException(msg, e);
        }
    }


    private List<String> findTpId(String siteId, Link siteLink, String portName) throws NeDesignerException {
        try {
            List<String> tpIds = new ArrayList<>();

            //get tpId for primary
            PathRouteObject primaryTpPathRoute = siteLink.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute().get(0).getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject()
                    .stream().filter(item -> item.getResourceType() instanceof Tp && ((Tp) item.getResourceType()).getTpHop().getTpRef().getValue().endsWith(portName)
                            && ((Tp) item.getResourceType()).getTpHop().getSiteRef().getValue().equals(siteId)).findFirst().get();
            tpIds.add(((Tp) primaryTpPathRoute.getResourceType()).getTpHop().getTpRef().getValue());

            //get tpId for secondary, if exists
            Secondary secondary = siteLink.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute().get(0).getSecondary(); if (secondary != null) {
                Optional<PathRouteObject> secondaryTpPathRouteOption = secondary.getExplicitRouteObjects().get(0).getPathRouteObject().stream()
                        .filter(item -> item.getResourceType() instanceof Tp && ((Tp) item.getResourceType()).getTpHop().getTpRef().getValue().endsWith(portName)
                                && ((Tp) item.getResourceType()).getTpHop().getSiteRef().getValue().equals(siteId)).findFirst();

                if (secondaryTpPathRouteOption.isPresent()) {
                    tpIds.add(((Tp) secondaryTpPathRouteOption.get().getResourceType()).getTpHop().getTpRef().getValue());
                }
            } return tpIds;
        } catch (Exception e) {
            String msg = String.format("Failed to get TP ID by siteId:%s, linkHop:%s, portName:%s", siteId, siteLink, portName); log.error(msg, e); throw new NeDesignerException(msg, e);
        }
    }

    public Pair<String, String> pickMuxTpMpoTp(String frequencyString, Node node, Set<String> muxEquipIds, Set<String> excludeExpTpIds) throws NeDesignerException, NoAvailableTpException {
        return pickMuxTpMpoTp(frequencyString, node, muxEquipIds, excludeExpTpIds,
                new DefaultOchpResourceLayout(), null);
    }

    public Pair<String, String> pickMuxTpMpoTp(String frequencyString, Node node,
            Set<String> muxEquipIds, Set<String> excludeExpTpIds,
            OchpResourceLayout resourceLayout) throws NeDesignerException, NoAvailableTpException {
        return pickMuxTpMpoTp(frequencyString, node, muxEquipIds, excludeExpTpIds, resourceLayout, null);
    }

    public Pair<String, String> pickMuxTpMpoTp(String frequencyString, Node node,
            Set<String> muxEquipIds, Set<String> excludeExpTpIds, String preferredMdPortName)
            throws NeDesignerException, NoAvailableTpException {
        return pickMuxTpMpoTp(frequencyString, node, muxEquipIds, excludeExpTpIds,
                new DefaultOchpResourceLayout(), preferredMdPortName);
    }

    /**
     * Selects an M?D? port while applying both the OCHP resource layout and the optional
     * preferred port index. If the preferred port is unavailable, the original smallest
     * available port selection remains the fallback.
     */
    public Pair<String, String> pickMuxTpMpoTp(String frequencyString, Node node,
            Set<String> muxEquipIds, Set<String> excludeExpTpIds,
            OchpResourceLayout resourceLayout, String preferredMdPortName)
            throws NeDesignerException, NoAvailableTpException {
        Card muxPanelCardInfo = nodeUtils.getCardInfoByEquipId(node, muxEquipIds.iterator().next());

        //pick free mdTP
        Set<String> excludeMdPortName = getMDPortNamesMappingByExp(muxPanelCardInfo,
                excludeExpTpIds);
        if (preferredMdPortName != null && !preferredMdPortName.isEmpty()) {
            // A preferred third-leg port is valid only when the selected 32/64-wave layout allows it.
            String preferredMdTpId = pickPreferredMdTp(frequencyString, node, muxEquipIds,
                    excludeMdPortName, preferredMdPortName, resourceLayout);
            if (preferredMdTpId != null) {
                return buildMuxTpMpoTpPair(muxPanelCardInfo, preferredMdTpId);
            }
        }
        String mdTpId = pickFreeMdTp(frequencyString, node, muxEquipIds, tp -> {
            String portName = PhysicalTpIdNamingRule.getPortNameByTpId(
                    tp.getTpId().getValue());
            // Bone2.0 32/64 filtering is applied before selection so an invalid free port is never picked.
            return !excludeMdPortName.contains(portName) && resourceLayout.allowsMdPort(portName);
        });

        //find mapping mpo tp
        return buildMuxTpMpoTpPair(muxPanelCardInfo, mdTpId);
    }

    private Pair<String, String> buildMuxTpMpoTpPair(Card muxPanelCardInfo, String mdTpId) throws NeDesignerException {
        String mdPortName = PhysicalTpIdNamingRule.getPortNameByTpId(mdTpId);
        String mpoPortName = NeInfoUtil.getMpoPortName(muxPanelCardInfo, mdPortName);
        if (mpoPortName != null) {
            String mpoTp = getReplacedMpoTpByPortName(mdTpId, mpoPortName);
            return new ImmutablePair<>(mdTpId, mpoTp);
        }
        return new ImmutablePair<>(mdTpId, null);
    }

    private String pickPreferredMdTp(String frequencyString, Node node, Set<String> equipIds,
            Set<String> excludeMdPortName, String preferredMdPortName,
            OchpResourceLayout resourceLayout) throws NeDesignerException {
        for (TerminationPoint tp : node.getTerminationPoint()) {
            String tpId = tp.getTpId().getValue();
            String portName = PhysicalTpIdNamingRule.getPortNameByTpId(tpId);
            if (!preferredMdPortName.equalsIgnoreCase(portName) || excludeMdPortName.contains(portName)) {
                continue;
            }
            if (!resourceLayout.allowsMdPort(portName)) {
                continue;
            }
            if (!equipIds.contains(tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef())) {
                continue;
            }
            if (isAvailableTp(tp, frequencyString, node)) {
                return tpId;
            }
        }
        return null;
    }

    public Pair<String, String> pickFreeMdTpByExpTp(String frequencyString, Node node, Set<String> muxEquipIds, String wssTpId) throws NeDesignerException, NoAvailableTpException {
        Card muxPanelCardInfo = nodeUtils.getCardInfoByEquipId(node, muxEquipIds.iterator().next());
        //pick free mdTP
        Set<String> includeMdPortNames = getMDPortNamesMappingByExp(muxPanelCardInfo, Collections.singleton(wssTpId));
        String mdTpId = pickFreeMdTpInclude(frequencyString, node, muxEquipIds, includeMdPortNames);

        //find mapping mpo tp
        String mdPortName = PhysicalTpIdNamingRule.getPortNameByTpId(mdTpId); String mpoPortName = NeInfoUtil.getMpoPortName(muxPanelCardInfo, mdPortName); if (mpoPortName != null) {
            String mpoTp = getReplacedMpoTpByPortName(mdTpId, mpoPortName); return new ImmutablePair<>(mdTpId, mpoTp);
        } return new ImmutablePair<>(mdTpId, null);
    }

    public Set<String> getMDPortNamesMappingByExp(Card muxPanelCardInfo, Set<String> expTpIds) throws NeDesignerException {
        Set<String> result = new HashSet<>(); for (String expTpId : expTpIds) {
            String expPortName = PhysicalTpIdNamingRule.getPortNameByTpId(expTpId); result.addAll(NeInfoUtil.getMDPortName(muxPanelCardInfo, expPortName));
        } return result;
    }

    private String pickFreeMdTpInclude(String frequencyString, Node node, Set<String> equipIds, Set<String> includeMdPortName) throws NoAvailableTpException, NeDesignerException {

        return pickFreeMdTp(frequencyString, node, equipIds, tp -> {
            String portName = PhysicalTpIdNamingRule.getPortNameByTpId(tp.getTpId().getValue()); return includeMdPortName.contains(portName);
        });
    }


    private String pickFreeMdTp(String frequencyString, Node node, Set<String> equipIds, Predicate<TerminationPoint> isValidTp) throws NoAvailableTpException, NeDesignerException {
        List<TerminationPoint> tps = new ArrayList<>(node.getTerminationPoint()); tps.sort((tp1, tp2) -> {
            try {
                return compareTp(tp1, tp2);
            } catch (NeDesignerException e) {
                throw new RuntimeException(e);
            }
        });

        String mdTpId = null;
        //get idle mux TPs in pair
        for (TerminationPoint tp : tps) {
            if (!isValidTp.test(tp)) {
                continue;
            } if (!equipIds.contains(tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef())) {
                continue;
            } if (isAvailableTp(tp, frequencyString, node)) {
                mdTpId = tp.getTpId().getValue(); break;
            }
        } if (mdTpId == null) {
            throw new NoAvailableTpException("There is no idle MUX TP available for node:" + node.getNodeId().getValue() + " by frequencyString: " + frequencyString + " by equipId " + equipIds);
        } return mdTpId;
    }

    private int compareTp(TerminationPoint tp1, TerminationPoint tp2) throws NeDesignerException {
        String tp1Equipid = tp1.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef();
        String tp2Equipid = tp2.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef(); int equipCompare = tp1Equipid.compareTo(tp2Equipid); if (equipCompare == 0) {
            return getPortNumber(tp1).compareTo(getPortNumber(tp2));
        } return equipCompare;

    }

    private String getReplacedMpoTpByPortName(String mdTpId, String mpoPortName) {
        if (!mdTpId.endsWith(mpoPortName)) {
            return mdTpId.substring(0, mdTpId.lastIndexOf("-") + 1) + mpoPortName;

        } return mdTpId;
    }

    private Integer getPortNumber(TerminationPoint tp) throws NeDesignerException {
        Pattern pattern = Pattern.compile("M(\\d+)D(\\d+)", Pattern.CASE_INSENSITIVE); Matcher matcher = pattern.matcher(tp.getTpId().getValue()); try {
            Integer number = matcher.find() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE; return number;
        } catch (Exception e) {
            log.error("Failed to pix MUX port for tp:{}", tp, e); throw new NeDesignerException("Failed to pick mux port for " + tp.getTpId().getValue(), e);
        }


    }

    /**
     * Note:  为了所有复用段处理保持一致，XC都不更新到node里面，由create-tunnel阶段更新。
     *
     * @param startMdPortTp
     * @param endMdPortTp
     * @param siteLink
     * @param frequency
     * @param centFreq
     * @return
     * @throws NeDesignerException
     */
    public List<CrossConnections> allocateSegmentNetwork(String startMdPortTp, String endMdPortTp, Link siteLink, String frequency, Long centFreq, String preWssLinkId, String nextWssLinkId)
            throws NeDesignerException {
        return allocateSegmentNetwork(startMdPortTp, endMdPortTp, siteLink, frequency, centFreq,
                preWssLinkId, nextWssLinkId, ROUTE_LEG_PRIMARY);
    }

    public List<CrossConnections> allocateSegmentNetwork(String startMdPortTp, String endMdPortTp, Link siteLink, String frequency, Long centFreq, String preWssLinkId, String nextWssLinkId,
            String routeLeg)
            throws NeDesignerException {

        //prepare
        String siteLinkId = siteLink.getLinkId().getValue(); String siteLinkSrcNodeId = PhysicalTpIdNamingRule.getNodeId(siteLink.getSource().getSourceTp().getValue());
        NeInfo neInfo = getNeInFoByNodeID(siteLinkSrcNodeId); Set<String> tunnelXcPortNames = neInfo.getTunnelXcPortNames(); BigInteger centFreqB = BigInteger.valueOf(centFreq);
        GridType grid = siteLink.getAugmentation(Link1.class).getSite().getGrid(); Boolean isEnd = endMdPortTp != null;//是否是起始的第一个复用段
        Boolean isStart = startMdPortTp != null;//是否是起始的第一个复用段
        String startMuxNodeId = startMdPortTp == null ? null : PhysicalTpIdNamingRule.getNodeId(startMdPortTp);
        String endMuxNodeId = endMdPortTp == null ? null : PhysicalTpIdNamingRule.getNodeId(endMdPortTp); if (isStart) {
            if (startMdPortTp == null) {
                log.error("muxMdPortTp is null when isStart is:{},isEnd is:{}", isStart, isEnd); throw new NeDesignerException("muxMdPortTp can not be null when start or end.");
            }
        } if (isEnd) {
            if (endMdPortTp == null) {
                log.error("muxMdPortTp is null when isStart is:{},isEnd is:{}", isStart, isEnd); throw new NeDesignerException("muxMdPortTp can not be null when start or end.");
            }
        }

        //init output
        List<CrossConnections> newXcs = new ArrayList<>();

        try {
            Route route = siteLink.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute().get(0);
            List<PathRouteObject> primary = getPathRouteObjects(route, siteLink, routeLeg); ArrayList<String> primaryTps = getTpsFromRoute(primary);
            Set<String> siteLinkEquipIds = primaryTps.stream().map(tpId -> PhysicalTpIdNamingRule.getEquipId(tpId)).collect(Collectors.toSet());
            String preWssTp = preWssLinkId == null ? null : getOwnWssTp(preWssLinkId, siteLinkEquipIds, siteLinkId);
            String nextWssTp = nextWssLinkId == null ? null : getOwnWssTp(nextWssLinkId, siteLinkEquipIds, siteLinkId);

            if (isStart) {
                if (!primaryTps.get(0).contains(startMuxNodeId)) {
                    Collections.reverse(primaryTps);
                }
            } else if (isEnd) {
                if (primaryTps.get(0).contains(endMuxNodeId)) {
                    Collections.reverse(primaryTps);
                }
            } else {
                //todo: 看没有了wss link以后，如何判断是否要reverse
//                String preWssTpNodeId = PhysicalTpIdNamingRule.getNodeId(preWssTp);
//                if (!primaryTps.get(0).contains(preWssTpNodeId)) {
//                    Collections.reverse(primaryTps);
//                }
            }

            //为首尾 添加M?D? TP

            if (isStart) {
                primaryTps.add(0, startMdPortTp);
            } if (isEnd) {
                primaryTps.add(endMdPortTp);
            }

            //add wss exp
            if (preWssTp != null) {
                if (grid.equals(GridType._0)) {
                    primaryTps.remove(0);//remove mpo for muxPanel
                    primaryTps.remove(0);//remove cmux mpo
                    primaryTps.remove(0);//remove cmux A
                    primaryTps.remove(0);//remove wss adddrop
                } else {
                    primaryTps.remove(0);//remove muxdmux
                    primaryTps.remove(0);//remove wss adddrop
                } primaryTps.add(0, preWssTp);
            }

            if (nextWssTp != null) {
                if (grid.equals(GridType._0)) {
                    primaryTps.remove(primaryTps.size() - 1);//remove mpo for muxPanel
                    primaryTps.remove(primaryTps.size() - 1);//remove cmux mpo
                    primaryTps.remove(primaryTps.size() - 1);//remove cmux A
                    primaryTps.remove(primaryTps.size() - 1);//remove wss adddrop
                } else {
                    primaryTps.remove(primaryTps.size() - 1);//remove muxdmux
                    primaryTps.remove(primaryTps.size() - 1);//remove wss adddrop
                } primaryTps.add(nextWssTp);
            }

            //create xc
            int i = 0;
            while (i < primaryTps.size() - 1) {
                String fromTpId = primaryTps.get(i); String fromPortName = PhysicalTpIdNamingRule.getPortNameByTpId(fromTpId);
                if (!tunnelXcPortNames.contains(fromPortName)) {
                    i++;
                    continue;
                }
                String nodeId = PhysicalTpIdNamingRule.getNodeId(fromTpId);
                Card card = getCardInfoByTpId(neInfo, fromTpId); List<CrossConnection> xcInfos = card.getCrossConnections();

                String toTpId = primaryTps.get(i + 1);
                String toNodeId = PhysicalTpIdNamingRule.getNodeId(toTpId);
                if (!nodeId.equals(toNodeId)) {
                    i++; continue;
//                    log.error("Failed to create xc in siteLink:{}, because fromTp:{} and toTp:{} are not same node.", siteLinkId, fromTpId, toTpId);
//                    throw new NeDesignerException("Failed to create xc in siteLink" + siteLinkId);
                }
                String toPortName = PhysicalTpIdNamingRule.getPortNameByTpId(toTpId);

                for (CrossConnection crossConnection : xcInfos) {
                    if (crossConnection.getInitiated()) {
                        continue;
                    }
                    List<String> fromNames = NeInfoUtil.getNameList(crossConnection.getFrom().getPort());
                    List<String> toNames = NeInfoUtil.getNameList(crossConnection.getTo().getPort());

                    if (fromNames.contains(fromPortName) && toNames.contains(toPortName)) {
                        Pair<String, String> mediaChannelEndpoints = getMediaChannelEndpoints(primaryTps, i, fromTpId, toTpId, neInfo);
                        CrossConnections xc = olsNodeService.createXC(crossConnection, nodeId, mediaChannelEndpoints.getLeft(),
                                Arrays.asList(mediaChannelEndpoints.getRight()), frequency, centFreqB);
                        newXcs.add(xc); break;
                    } if (crossConnection.getBiDirection()) {
                        if (fromNames.contains(toPortName) && toNames.contains(fromPortName)) {
                            Pair<String, String> mediaChannelEndpoints = getMediaChannelEndpoints(primaryTps, i, toTpId, fromTpId, neInfo);
                            CrossConnections xc = olsNodeService.createXC(crossConnection, nodeId, mediaChannelEndpoints.getLeft(),
                                    Arrays.asList(mediaChannelEndpoints.getRight()), frequency, centFreqB);
                            newXcs.add(xc); break;
                        }
                    }

                } i = i + 2;
            }

            //create ext xc
            if (grid.equals(GridType._0) && (startMdPortTp != null || endMdPortTp != null)) {// only flex need ext xc
                String extXcSiteId = null;
                if (isStart || isEnd) {
                    extXcSiteId = isStart ? PhysicalTpIdNamingRule.getSiteId(startMdPortTp) : PhysicalTpIdNamingRule.getSiteId(endMdPortTp);
                }
                Map<String, List<String>> primaryTpsByNode = primaryTps.stream().collect(Collectors.groupingBy(tpId -> PhysicalTpIdNamingRule.getNodeId(tpId)));
                Map<String, List<String>> secondaryTpsByNode = new HashMap<>();
                if (tunnelUtils.isOmsp(siteLink)) {
                    List<PathRouteObject> secondary = route.getSecondary().getExplicitRouteObjects().get(0).getPathRouteObject(); ArrayList<String> secondaryTps = getTpsFromRoute(secondary);
                    secondaryTpsByNode = secondaryTps.stream().collect(Collectors.groupingBy(tpId -> PhysicalTpIdNamingRule.getNodeId(tpId)));
                }
                for (Entry<String, List<String>> tpEntry : primaryTpsByNode.entrySet()) {
                    List<String> sameNodeTps = tpEntry.getValue();
                    String nodeId = tpEntry.getKey();
                    if (secondaryTpsByNode.containsKey(nodeId)) {
                        sameNodeTps.addAll(secondaryTpsByNode.get(nodeId));
                    }
                    Boolean isAdditional = tunnelUtils.isAdditionalMux(siteLink, isStart ? startMdPortTp : endMdPortTp);
                    Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
                    CrossConnections extXc = createExtXcForNode(frequency, neInfo, centFreqB, node, sameNodeTps, isAdditional);
                    if (extXc != null) {
                        if (isStart) {
                            String currentSiteId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
                            if (currentSiteId.equals(extXcSiteId)) {
                                newXcs.add(0, extXc); continue;
                            }
                        }
                        newXcs.add(extXc);
                    }
                }
            }

//            //处理nextWssjiaoc
//            if (nextWssTp != null) {
//                newXcs.add(createWssXc(frequency, nextWssTp, neInfo, centFreqB));
//            }
            return newXcs;


        } catch (Exception e) {
            String msg = "Failed to create XC for siteLink " + siteLinkId; log.error(msg, e); throw new NeDesignerException(msg, e);
        }
    }

    private List<PathRouteObject> getPathRouteObjects(Route route, Link siteLink, String routeLeg) {
        if (tunnelUtils.isBone20OneToTwo(siteLink)) {
            // Bone2.0 model=10 keeps OLP3-3 legs inside one siteLink; use the leg requested
            // by OCH route expansion while leaving normal siteLinks on their primary route.
            if (ROUTE_LEG_SECONDARY.equals(routeLeg) && route.getSecondary() != null) {
                return route.getSecondary().getExplicitRouteObjects().get(0).getPathRouteObject();
            }
            if (ROUTE_LEG_THIRD.equals(routeLeg) && route.getThird() != null && !route.getThird().isEmpty()) {
                return route.getThird().get(0).getExplicitRouteObjects().get(0).getPathRouteObject();
            }
        }
        return route.getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject();
    }

    public CrossConnections createExtXcForNode(String frequency, NeInfo neInfo, BigInteger centFreqB, Node node, List<String> sameNodeTps, Boolean isAdditional) throws NeDesignerException {
        //get wdmBand
        WDM_Band wdmBand = WDM_Band.fromString(getCardInfoByTpId(sameNodeTps.get(0), node).getWdmBand());
        String nodeId = node.getNodeId().getValue();
        for (String fromTpId : sameNodeTps) {

            Card card = neInfo.getDefaultCardByCardClass(NeInfo.MUXPANEL_CARD_CLASS, wdmBand); List<CrossConnection> extXCDefinition = card.getExternalCrossConnections();
            if (extXCDefinition == null || extXCDefinition.isEmpty()) {
                continue;
            }

            String fromPortName = PhysicalTpIdNamingRule.getPortNameByTpId(fromTpId); CrossConnections extXc = null; for (CrossConnection crossConnection : extXCDefinition) {
                if (!crossConnection.getAdditional() == isAdditional) {
                    continue;
                } List<String> fromNames = NeInfoUtil.getNameList(crossConnection.getFrom().getPort()); List<String> extToNames = NeInfoUtil.getNameList(crossConnection.getTo().getPort());
//                    String extToName = crossConnection.getTo().getPort();//因为ext xc，只有cmux到OA的，为了简单，这里hardcode了

                if (fromNames.contains(fromPortName)) {
                    List<String> extToTpIds = getToTpIdByName(sameNodeTps, extToNames); if (extToTpIds == null) {
                        continue;
                    }

             /*   if (route.getSecondary() != null) {
                    ArrayList<String> secondaryTps = getTpsFromRoute(route.getSecondary().getExplicitRouteObjects().get(0).getPathRouteObject());
                    String extToTpId = getToTpIdByName(sameNodeTps, extToNames);
                    extToTpIds.addAll(findTpsByNodeAndPortName(muxNodeId, secondaryTps, extToName));
                }*/

                    return olsNodeService.createXC(crossConnection, nodeId, fromTpId, extToTpIds, frequency, centFreqB);

                }
            }
//                    if (extXc == null) {
//                        log.error("Failed to create ExternalCrossConnection fromTpId:{}, from tps:{}", sameNodeTps, fromTpId, sameNodeTps);
//                        throw new NeDesignerException("Failed to create ExternalCrossConnection fromTpId: " + fromTpId);
//                    }

        } log.warn("Failed to create ExternalCrossConnection for node:{}, tps:{}", nodeId, sameNodeTps);
//            throw new NeDesignerException("Failed to create ExternalCrossConnection fromTpId: " + fromTpId);

        return null;

    }

    private Pair<String, String> getMediaChannelEndpoints(List<String> routeTps, int fromIndex, String sourceTpId,
                                                           String destinationTpId, NeInfo neInfo) throws NeDesignerException {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
        for (int index = 0; index < routeTps.size(); index++) {
            String routeTp = routeTps.get(index);
            if (!nodeId.equals(PhysicalTpIdNamingRule.getNodeId(routeTp))
                    || !NeInfo.RAMAN_LINE_PORT.equals(PhysicalTpIdNamingRule.getPortNameByTpId(routeTp))) {
                continue;
            }
            Card routeCard = getCardInfoByTpId(neInfo, routeTp);
            if (!NeInfo.RAMAN_CARD_TYPE.equals(routeCard.getCardType())) {
                continue;
            }
            if (index < fromIndex) {
                return replaceMediaChannelEndpoint(sourceTpId, destinationTpId, routeTps.get(fromIndex), routeTp);
            }
            if (index > fromIndex + 1) {
                return replaceMediaChannelEndpoint(sourceTpId, destinationTpId, routeTps.get(fromIndex + 1), routeTp);
            }
        }
        return new ImmutablePair<>(sourceTpId, destinationTpId);
    }

    private Pair<String, String> replaceMediaChannelEndpoint(String sourceTpId, String destinationTpId,
                                                              String nodeBoundaryTpId, String ramanLineTpId) {
        if (sourceTpId.equals(nodeBoundaryTpId)) {
            return new ImmutablePair<>(ramanLineTpId, destinationTpId);
        }
        if (destinationTpId.equals(nodeBoundaryTpId)) {
            return new ImmutablePair<>(sourceTpId, ramanLineTpId);
        }
        return new ImmutablePair<>(sourceTpId, destinationTpId);
    }

    private String getOwnWssTp(String wssLinkId, Set<String> siteLinkEquipIds, String siteLinkId) throws NeDesignerException {
        String wssTpAId = PhysicalLinkIdNamingRule.getTpAId(wssLinkId); String wssTpAEquipId = PhysicalTpIdNamingRule.getEquipId(wssTpAId); if (siteLinkEquipIds.contains(wssTpAEquipId)) {
            return wssTpAId;
        } String wssTpZId = PhysicalLinkIdNamingRule.getTpZId(wssLinkId); String wssTpZEquipId = PhysicalTpIdNamingRule.getEquipId(wssTpZId); if (siteLinkEquipIds.contains(wssTpZEquipId)) {
            return wssTpZId;
        } log.error("Failed to get wss tp from wssLink:{}, siteLinkId:{}, siteLinkEquipIds:{}", wssLinkId, siteLinkId, siteLinkEquipIds);
        throw new NeDesignerException("Failed to find wss tp from wssLink: " + wssLinkId + " for siteLink: " + siteLinkId);

    }

    private List<String> getToTpIdByName(List<String> sameNodeTps, List<String> extToNames) {
        for (String extToName : extToNames) {
            List<String> extToTpIds = sameNodeTps.stream().filter(item -> item.endsWith(extToName)).collect(Collectors.toList()); if (!extToTpIds.isEmpty()) {
                return extToTpIds;
            }
        } return null;
    }

    private List<String> findTpsByNodeAndPortName(String muxNodeId, List<String> primaryTps, String extToName) {
        return primaryTps.stream().filter(tp -> tp.endsWith(extToName) && PhysicalTpIdNamingRule.getNodeId(tp).equals(muxNodeId)).collect(Collectors.toList());
    }

//    private CrossConnections createWssXc(String frequency, String preWssTp, NeInfo neInfo, BigInteger centFreqB) throws NeDesignerException {
//        List<CrossConnection> xcInfos = getCardInfoByTpId(neInfo, preWssTp).getCrossConnections();
//        String fromWssPortName = PhysicalTpIdNamingRule.getPortNameByTpId(preWssTp);
//        String wssNodeId = PhysicalTpIdNamingRule.getNodeId(preWssTp);
//        for (CrossConnection crossConnection : xcInfos) {
//            if (crossConnection.getInitiated()) {
//                continue;
//            }
//            List<String> fromNames = NeInfoUtil.getNameList(crossConnection.getFrom().getPort());
//            if (fromNames.contains(fromWssPortName)) {
//                String toWssName = crossConnection.getTo().getPort();//因为这里只能时SIG，为了方便直接取
//                String toTpId = preWssTp.replace(fromWssPortName, toWssName);
//
//                return olsNodeService.createXC(crossConnection, wssNodeId, preWssTp, Arrays.asList(toTpId), frequency, centFreqB);
//
//            }
//        }
//        log.error("Failed to create wss XC for TP:{}, because failed to find XC definition from port:{}", preWssTp, fromWssPortName);
//        throw new NeDesignerException("Failed to find WSS CrossConnections from TP: " + preWssTp);
//    }

    private Card getCardInfoByTpId(NeInfo neInfo, String fromTpId) throws NeDesignerException {
        String equipId = PhysicalTpIdNamingRule.getEquipId(fromTpId); String nodeId = PhysicalTpIdNamingRule.getNodeId(fromTpId); String cardVendorType = phyNodeDao.getCardVendorType(nodeId, equipId);
        return neInfo.getCardByCardVendor(cardVendorType);
    }

    private Card getCardInfoByTpId(String fromTpId, Node node) throws NeDesignerException {
        String equipId = PhysicalTpIdNamingRule.getEquipId(fromTpId);
        return nodeUtils.getCardInfoByEquipId(node, equipId);
    }

    private ArrayList<String> getTpsFromRoute(List<PathRouteObject> pathRouteObjects) {
        return pathRouteObjects.stream().filter(item -> item.getResourceType() instanceof Tp).map(item -> ((Tp) item.getResourceType()).getTpHop().getTpRef().getValue())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private NeInfo getNeInFoByNodeID(String nodeId) throws NeDesignerException {
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId); return nodeUtils.getNeInfoByNode(node, NodeType.OD);//这里肯定是获取光的

    }

    /**
     * 1. For flex, create xc:  m?d?->line
     * <p>
     * 2. For fix, create xc: m?d?->muxdumux
     *
     * @param startMuxMdPortTp
     * @param endMuxMdPortTp
     * @param siteLink
     * @param frequency
     * @param cenFrequency
     * @return
     * @throws NeDesignerException
     */
    public List<CrossConnections> allocateSingleSegment(String startMuxMdPortTp, String endMuxMdPortTp, Link siteLink, String frequency, Long cenFrequency) throws NeDesignerException {

        //prepare
        String siteLinkId = siteLink.getLinkId().getValue(); String siteLinkSrcNodeId = PhysicalTpIdNamingRule.getNodeId(siteLink.getSource().getSourceTp().getValue());
        NeInfo neInfo = getNeInFoByNodeID(siteLinkSrcNodeId); BigInteger centFreqB = BigInteger.valueOf(cenFrequency); GridType grid = siteLink.getAugmentation(Link1.class).getSite().getGrid();
        Card muxCard = getCardInfoByTpId(neInfo, startMuxMdPortTp);

        //init output
        List<CrossConnections> newXcs = new ArrayList<>();

        try {
            Boolean isNetWorkInvolved = siteLink.getAugmentation(Link1.class).getSite().isInvolvedNetwork();
            Route route = siteLink.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute().get(0);
            ArrayList<String> primaryTps = getTpsFromRoute(route.getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject());
            ArrayList<String> secondaryTps = route.getSecondary() == null ? null : getTpsFromRoute(route.getSecondary().getExplicitRouteObjects().get(0).getPathRouteObject());

            newXcs.addAll(createMDXcs(isNetWorkInvolved, grid, frequency, centFreqB, neInfo, muxCard, primaryTps, secondaryTps, startMuxMdPortTp));
            newXcs.addAll(createMDXcs(isNetWorkInvolved, grid, frequency, centFreqB, neInfo, muxCard, primaryTps, secondaryTps, endMuxMdPortTp)); return newXcs;

        } catch (Exception e) {
            String msg = "Failed to create XC for siteLink " + siteLinkId; log.error(msg, e); throw new NeDesignerException(msg, e);
        }

    }

    private List<CrossConnections> createMDXcs(Boolean isNetWorkInvolved, GridType grid, String frequency, BigInteger centFreqB, NeInfo neInfo, Card muxCard, ArrayList<String> primaryTps,
            ArrayList<String> secondaryTps, String mdTp) throws NeDesignerException {
        String mdPort = PhysicalTpIdNamingRule.getPortNameByTpId(mdTp); String mdNodeId = PhysicalTpIdNamingRule.getNodeId(mdTp); List<String> tps = new ArrayList<>(primaryTps);
        if (secondaryTps != null) {
            tps.addAll(secondaryTps);
        } tps = tps.stream().filter(tp -> tp.contains(mdNodeId)).collect(Collectors.toList());

        List<CrossConnections> mdXcs = new ArrayList<>();

        //create md->muxdmux
        if (!grid.equals(GridType._0)) {
            mdXcs.add(createMDXc(frequency, centFreqB, primaryTps, mdTp, mdPort, mdNodeId, tps, muxCard.getCrossConnections()));
        }

        //create ext xc: md->line
        if (grid.equals(GridType._0) || isNetWorkInvolved) {
            tps.add(mdTp);
            Node mdNode = phyNodeDao.getConfigPhyNodeById(mdNodeId);
            CrossConnections extXc = createExtXcForNode(frequency, neInfo, centFreqB, mdNode, tps, false);
            if (extXc != null) {
                mdXcs.add(extXc);
            } else {
                log.error("Failed to create ext XC on tps:{}", tps);
            }

        } return mdXcs;
    }

    private CrossConnections createMDXc(String frequency, BigInteger centFreqB, List<String> primaryTps, String mdTp, String mdPort, String mdNodeId, List<String> tps,
            List<CrossConnection> crossConnections) throws NeDesignerException {
        for (CrossConnection crossConnection : crossConnections) {
            if (crossConnection.getInitiated()) {
                continue;
            } List<String> fromNames = NeInfoUtil.getNameList(crossConnection.getFrom().getPort());

            if (fromNames.contains(mdPort)) {
                String extToName = crossConnection.getTo().getPort();//因为M?D?的交叉，to只会有一个port，这里就hard coded了
                List<String> extToTpIds = findTpsByNodeAndPortName(mdNodeId, tps, extToName); return olsNodeService.createXC(crossConnection, mdNodeId, mdTp, extToTpIds, frequency, centFreqB);

            }
        }

        log.warn("Failed to create mux xc from {}, for xc definition:{}, during tps:{}", mdTp, crossConnections, tps); return null;
//        throw new NeDesignerException("Failed to create mux xc from " + mdTp);
    }


}
