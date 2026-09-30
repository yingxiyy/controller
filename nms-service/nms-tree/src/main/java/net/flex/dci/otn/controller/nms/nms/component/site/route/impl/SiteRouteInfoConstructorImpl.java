package net.flex.dci.otn.controller.nms.nms.component.site.route.impl;

import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_FIST_INDEX;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.nms.constructs.CMUX64Constructor;
import net.flex.dci.otn.controller.nms.constructs.MUXPANELConstructor;
import net.flex.dci.otn.controller.nms.nms.component.site.route.SiteRouteInfoConstructor;
import net.flex.dci.otn.controller.nms.nms.dto.RouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRefPhyNodeRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDetailDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDetailSequenceDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDto;
import net.flex.dci.otn.controller.nms.nms.enums.TunnelSiteType;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteInput.RouteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.TpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.link.hop.SupportSiteLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.TpHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.SiteNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/19 14:32
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class SiteRouteInfoConstructorImpl implements SiteRouteInfoConstructor {

    private final PhyNodeDao phyNodeDao;

    private final PhyLinkDao phyLinkDao;

    private final SiteNodeDao siteNodeDao;

    private final SiteLinkDao siteLinkDao;

    private final CrossConnectionsDao crossConnectionsDao;

    private final HashMap<String, Node> nodesMap = new HashMap<>();

    private final HashMap<String, Map<String, TerminationPoint>> nodeTpsMap = new HashMap<>();

    private final HashMap<String, Map<String, Equipments>> nodeEquipMap = new HashMap<>();

    @Override
    public List<RouteInfo> constructSiteRouteInfo(SiteRouteDto siteRoute,
            TunnelSiteType tunnelSiteType, RouteType routeType) {
        try {
            String siteId = siteRoute.getSiteId();
            log.debug("construct site route info for the site,the siteId is:{}", siteId);
            SiteRouteDetailDto primary = siteRoute.getPrimary();
            SiteRouteDetailDto secondary = siteRoute.getSecondary();
            RouteInfo routeInfo = null;
            if (tunnelSiteType.equals(TunnelSiteType.DEST_SITE) || tunnelSiteType.equals(
                    TunnelSiteType.SOURCE_SITE)) {
                routeInfo = buildTerminalSiteRouteInfo(primary, secondary, tunnelSiteType);
            } else {
                routeInfo = buildTransitSiteRouteInfo(primary, secondary, routeType);
            }
            return Collections.singletonList(routeInfo);
        } finally {
            nodeEquipMap.clear();
            nodesMap.clear();
            nodeTpsMap.clear();
        }
    }


    /**
     * build the tunnel  terminal site  route info
     *
     * @param primary
     * @param secondary
     * @return
     */
    private RouteInfo buildTerminalSiteRouteInfo(SiteRouteDetailDto primary,
            SiteRouteDetailDto secondary, TunnelSiteType tunnelSiteType) {
        boolean isReverse = !tunnelSiteType.equals(TunnelSiteType.SOURCE_SITE);
        RouteDto primaryRoute = buildRoute(primary, isReverse);

        RouteInfoBuilder routeInfoBuilder = new RouteInfoBuilder();
        routeInfoBuilder.setIndex((short) 1);
        routeInfoBuilder.setKey(new RouteInfoKey((short) 1));
        routeInfoBuilder.setPrimary(
                new PrimaryBuilder().setRouteSequence(primaryRoute.getRouteSequences())
                        .setSiteSequence(primaryRoute.getSites())
                        .setCrossConnections(primaryRoute.getCrossConnections())
                        .build());
        if (secondary != null) {
            RouteDto secondaryRoute = buildRoute(secondary, isReverse);
            routeInfoBuilder.setSecondary(
                    new SecondaryBuilder().setRouteSequence(secondaryRoute.getRouteSequences())
                            .setSiteSequence(secondaryRoute.getSites())
                            .setCrossConnections(secondaryRoute.getCrossConnections())
                            .build());
        }
        return routeInfoBuilder.build();
    }


    /**
     * build the transit site route info
     *
     * @param primary
     * @param secondary
     * @return
     */
    private RouteInfo buildTransitSiteRouteInfo(SiteRouteDetailDto primary,
            SiteRouteDetailDto secondary, RouteType routeType) {
        RouteInfoBuilder routeInfoBuilder = new RouteInfoBuilder();
        routeInfoBuilder.setIndex((short) 1);
        routeInfoBuilder.setKey(new RouteInfoKey((short) 1));
        if (routeType == null || routeType.equals(RouteType.BOTH)) {
            if (primary != null) {
                RouteDto primaryRoute = buildTransitSiteRoute(primary);
                routeInfoBuilder.setPrimary(
                        new PrimaryBuilder().setRouteSequence(primaryRoute.getRouteSequences())
                                .setSiteSequence(primaryRoute.getSites())
                                .setCrossConnections(primaryRoute.getCrossConnections())
                                .build());
            }
            if (secondary != null) {
                RouteDto secondaryRoute = buildTransitSiteRoute(secondary);
                routeInfoBuilder.setSecondary(
                        new SecondaryBuilder().setRouteSequence(secondaryRoute.getRouteSequences())
                                .setSiteSequence(secondaryRoute.getSites())
                                .setCrossConnections(secondaryRoute.getCrossConnections())
                                .build());
            }
        } else if (routeType.equals(RouteType.SECONDARY)) {
            if (secondary != null) {
                RouteDto secondaryRoute = buildTransitSiteRoute(secondary);
                routeInfoBuilder.setSecondary(
                        new SecondaryBuilder().setRouteSequence(secondaryRoute.getRouteSequences())
                                .setSiteSequence(secondaryRoute.getSites())
                                .setCrossConnections(secondaryRoute.getCrossConnections())
                                .build());
            }
        } else if (routeType.equals(RouteType.PRIMARY)) {
            RouteDto primaryRoute = buildTransitSiteRoute(primary);

            routeInfoBuilder.setPrimary(
                    new PrimaryBuilder().setRouteSequence(primaryRoute.getRouteSequences())
                            .setSiteSequence(primaryRoute.getSites())
                            .setCrossConnections(primaryRoute.getCrossConnections())
                            .build());
        }

        return routeInfoBuilder.build();
    }

    private RouteDto buildTransitSiteRoute(SiteRouteDetailDto route) {
        Map<String, SiteRefPhyNodeRouteDto> refPhyNodeRouteDtoMap = new HashMap<>();
        List<SiteRouteDetailSequenceDto> siteRouteSequences = route.getSiteRouteDetails();
        LinkedList<String> sites = new LinkedList<>();
        Set<String> visitedTp = new HashSet<>();
        String muxPanelTpId = null;
        String siteId = route.getSiteId();
        List<RouteSequence> routeSequences = new ArrayList<>();
        long seqIndex = 1l;
        for (SiteRouteDetailSequenceDto routeSequence : siteRouteSequences) {
            String objectId = routeSequence.getRefId();
            ResourceType resourceType = null;
            if (PhysicalTpIdNamingRule.isTpId(objectId)) {
                if (!visitedTp.contains(objectId)) {
                    log.debug("the tp id is tpId:{},muxPanelTpID:{}", objectId, muxPanelTpId);
                    resourceType = getTerminationPointResourceType(objectId, muxPanelTpId);
                    PhyTp tp = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp) resourceType).getTpHop()
                            .getPhyTp();
                    if (tp.getPhysical().getPortType().equals(PortType.MUXChannel)) {
                        muxPanelTpId = tp.getTpId().getValue();
                    }
                    visitedTp.add(objectId);
                    String refSiteId = PhysicalTpIdNamingRule.getSiteId(objectId);
                    if (!sites.contains(refSiteId)) {
                        sites.add(refSiteId);
                    }
                    RouteSequence sequence = buildRouteSequence(resourceType, seqIndex);
                    routeSequences.add(sequence);
                    seqIndex++;
                }
            } else if (PhysicalLinkIdNamingRule.isPhysicalLinkId(objectId)) {
                resourceType = getPhyLinkResourceType(objectId, muxPanelTpId);
                routeSequences.add(buildRouteSequence(resourceType, seqIndex));
                seqIndex++;
            } else if (PhysicalLinkIdNamingRule.isOtsLink(objectId)) {
                resourceType = getOtsLinkRefResource(objectId, siteId);
                routeSequences.add(buildRouteSequence(resourceType, seqIndex));
                seqIndex++;
            }
        }
//        for (SiteRouteDetailSequenceDto routeSequence : siteRouteSequences) {
//            String objectId = routeSequence.getRefId();
//            ResourceType resourceType = null;
//            if (PhysicalTpIdNamingRule.isTpId(objectId)) {
//                if (!visitedTp.contains(objectId)) {
//                    log.debug("the tp id is tpId:{},muxPanelTpID:{}", objectId, muxPanelTpId);
//                    resourceType = getTerminationPointResourceType(objectId, muxPanelTpId);
//                    PhyTp tp = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp) resourceType).getTpHop()
//                            .getPhyTp();
//                    if (tp.getPhysical().getPortType().equals(PortType.MUXChannel)) {
//                        muxPanelTpId = tp.getTpId().getValue();
//                    }
//                    visitedTp.add(objectId);
//                    String refSiteId = PhysicalTpIdNamingRule.getSiteId(objectId);
//                    if (!sites.contains(refSiteId)) {
//                        sites.add(refSiteId);
//                    }
//
//                    updateRefPhyNodeRouteMap(resourceType, refPhyNodeRouteDtoMap);
//                }
//            } else if (PhysicalLinkIdNamingRule.isOsLink(objectId)
//                    || PhysicalLinkIdNamingRule.isCableLink(objectId)
//                    || PhysicalLinkIdNamingRule.isOmsLink(objectId)
//                    || PhysicalLinkIdNamingRule.isWssLink(objectId)) {
//                resourceType = getPhyLinkResourceType(objectId, muxPanelTpId);
//                updateRefPhyNodeRouteMap(resourceType, refPhyNodeRouteDtoMap);
//            } else if (PhysicalLinkIdNamingRule.isOtsLink(objectId)) {
//                resourceType = getOtsLinkRefResource(objectId, siteId);
//                updateRefPhyNodeRouteMap(resourceType, refPhyNodeRouteDtoMap);
//            }
//        }
//        List<RouteSequence> routeSequences = new ArrayList<>();
//        long seqIndex = 1l;
//        for (Map.Entry<String, SiteRefPhyNodeRouteDto> entry : refPhyNodeRouteDtoMap.entrySet()) {
//            SiteRefPhyNodeRouteDto siteRefPhyNodeRouteDto = entry.getValue();
//            List<RouteSequence> ilaRouteSequences = buildRouteSequence(
//                    siteRefPhyNodeRouteDto.getILATps(), seqIndex);
//            seqIndex += ilaRouteSequences.size();
//            List<RouteSequence> oaRouteSequences = buildRouteSequence(
//                    siteRefPhyNodeRouteDto.getOATps(), seqIndex);
//            seqIndex += oaRouteSequences.size();
//            List<RouteSequence> wssRouteSequences = buildRouteSequence(
//                    siteRefPhyNodeRouteDto.getWSSTps(), seqIndex);
//            seqIndex += wssRouteSequences.size();
//            List<RouteSequence> muxSequences = buildRouteSequence(
//                    siteRefPhyNodeRouteDto.getMUXTps(), seqIndex);
//            seqIndex += muxSequences.size();
//            List<RouteSequence> muxPanelSequences = buildRouteSequence(
//                    siteRefPhyNodeRouteDto.getMUXTps(), seqIndex);
//            seqIndex += muxPanelSequences.size();
//            List<RouteSequence> phyLinkSequences = buildRouteSequence(
//                    siteRefPhyNodeRouteDto.getPhyLinks(), seqIndex);
//            seqIndex += phyLinkSequences.size();
//            routeSequences.addAll(ilaRouteSequences);
//            routeSequences.addAll(oaRouteSequences);
//            routeSequences.addAll(wssRouteSequences);
//            routeSequences.addAll(muxSequences);
//            routeSequences.addAll(muxPanelSequences);
//            routeSequences.addAll(phyLinkSequences);
//        }

        List<CrossConnections> crossConnections = getRefCrossConnections(
                route.getCrossConnections());
        return RouteDto.builder().sites(sites).routeSequences(routeSequences)
                .crossConnections(crossConnections).build();
    }

    private RouteSequence buildRouteSequence(ResourceType resourceType, long seqIndex) {
        log.debug("build route sequence resource type is :{},index :{}", resourceType, seqIndex);
        RouteSequenceBuilder routeSequenceBuilder = new RouteSequenceBuilder();
        routeSequenceBuilder.setResourceType(resourceType);
        routeSequenceBuilder.setTopologyRef(
                TopologyId.getDefaultInstance(Constants.PHY_TOPO_KEY));
        routeSequenceBuilder.setKey(new RouteSequenceKey(seqIndex));
        routeSequenceBuilder.setSequence(seqIndex);
        return routeSequenceBuilder.build();
    }

    private List<RouteSequence> buildRouteSequence(List<ResourceType> resources, long seqIndex) {
        List<RouteSequence> routeSequences = new ArrayList<>();
        for (ResourceType resource : resources) {
            RouteSequenceBuilder routeSequenceBuilder = new RouteSequenceBuilder();
            routeSequenceBuilder.setResourceType(resource);
            routeSequenceBuilder.setTopologyRef(
                    TopologyId.getDefaultInstance(Constants.PHY_TOPO_KEY));
            routeSequenceBuilder.setKey(new RouteSequenceKey(seqIndex));
            routeSequences.add(routeSequenceBuilder.build());
            seqIndex++;
        }
        return routeSequences;
    }

    private void updateRefPhyNodeRouteMap(ResourceType resourceType,
            Map<String, SiteRefPhyNodeRouteDto> refPhyNodeRouteDtoMap) {
        if (resourceType instanceof Tp) {
            PhyTp tp = ((Tp) resourceType).getTpHop()
                    .getPhyTp();
            PortType portType = tp.getPhysical().getPortType();
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tp.getTpId().getValue());
            SiteRefPhyNodeRouteDto siteRefPhyNodeRoute = null;
            if (refPhyNodeRouteDtoMap.containsKey(nodeId)) {
                siteRefPhyNodeRoute = refPhyNodeRouteDtoMap.get(
                        nodeId);
            } else {
                siteRefPhyNodeRoute = new SiteRefPhyNodeRouteDto();
            }
            if (portType.equals(PortType.ILALINEA) || portType.equals(PortType.ILALINEB)) {
                List<ResourceType> ilaResourceType = siteRefPhyNodeRoute.getILATps();
                ilaResourceType.add(resourceType);
                siteRefPhyNodeRoute.setILATps(ilaResourceType);
            } else if (portType.equals(PortType.OALine) || portType.equals(PortType.OASig)) {
                List<ResourceType> oaResourceType = siteRefPhyNodeRoute.getOATps();
                oaResourceType.add(resourceType);
                siteRefPhyNodeRoute.setOATps(oaResourceType);
            } else if (portType.equals(PortType.WSSAddDrop) || portType.equals(PortType.WSSMesh)
                    || portType.equals(PortType.WSSSig)) {
                List<ResourceType> wssResourceType = siteRefPhyNodeRoute.getWSSTps();
                wssResourceType.add(resourceType);
                siteRefPhyNodeRoute.setWSSTps(wssResourceType);
            } else if (portType.equals(PortType.MUXChannel)) {
                List<ResourceType> muxPanelTpS = siteRefPhyNodeRoute.getMuxPanelTpS();
                muxPanelTpS.add(resourceType);
                siteRefPhyNodeRoute.setMuxPanelTpS(muxPanelTpS);
            } else if (portType.equals(PortType.MUXMUX) || portType.equals(PortType.MUXDEMUX)) {
                List<ResourceType> muxTps = siteRefPhyNodeRoute.getMUXTps();
                muxTps.add(resourceType);
                siteRefPhyNodeRoute.setMUXTps(muxTps);
            }
            refPhyNodeRouteDtoMap.put(nodeId, siteRefPhyNodeRoute);
        } else if (resourceType instanceof Link) {
            Link link = (Link) resourceType;
            LinkHop linkHop = link.getLinkHop();
            String sourceNodeId = linkHop.getSource().getSourceNode().getValue();
            if (refPhyNodeRouteDtoMap.containsKey(sourceNodeId)) {
                SiteRefPhyNodeRouteDto siteRefPhyNodeRoute = refPhyNodeRouteDtoMap.get(
                        sourceNodeId);
                siteRefPhyNodeRoute.getPhyLinks().add(resourceType);
            } else {
                SiteRefPhyNodeRouteDto siteRefPhyNodeRouteDto = new SiteRefPhyNodeRouteDto();
                List<ResourceType> phyLinkResource = new ArrayList<>();
                phyLinkResource.add(resourceType);
                siteRefPhyNodeRouteDto.setPhyLinks(phyLinkResource);
                refPhyNodeRouteDtoMap.put(sourceNodeId, siteRefPhyNodeRouteDto);
            }
        }

    }

    private RouteDto buildRoute(SiteRouteDetailDto route, boolean isReverse) {
        long seqIndex = 1l;
        String siteId = route.getSiteId();
        List<SiteRouteDetailSequenceDto> siteRouteSequences = route.getSiteRouteDetails();
        if (isReverse) {
            Collections.reverse(siteRouteSequences);
        }
        List<RouteSequence> routeSequences = new ArrayList<>();
        LinkedList<String> sites = new LinkedList<>();
        Set<String> visitedTp = new HashSet<>();
        String muxPanelTpId = null;
        for (SiteRouteDetailSequenceDto routeSequence : siteRouteSequences) {
            String objectId = routeSequence.getRefId();
            ResourceType resourceType = null;
            if (PhysicalTpIdNamingRule.isTpId(objectId)) {
                if (!visitedTp.contains(objectId)) {

                    resourceType = getTerminationPointResourceType(objectId, muxPanelTpId);
                    PhyTp tp = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp) resourceType).getTpHop()
                            .getPhyTp();
                    if (tp.getPhysical().getPortType().equals(PortType.MUXChannel)) {
                        muxPanelTpId = tp.getTpId().getValue();
                    }
                    visitedTp.add(objectId);
                    String refSiteId = PhysicalTpIdNamingRule.getSiteId(objectId);
                    if (!sites.contains(refSiteId)) {
                        sites.add(refSiteId);
                    }
                }
            } else if (PhysicalLinkIdNamingRule.isPhysicalLinkId(objectId)) {
                resourceType = getPhyLinkResourceType(objectId, muxPanelTpId);
            } else if (PhysicalLinkIdNamingRule.isOtsLink(objectId)) {
                resourceType = getOtsLinkRefResource(objectId, siteId);
            }
            RouteSequenceBuilder routeSequenceBuilder = new RouteSequenceBuilder();
            routeSequenceBuilder.setResourceType(resourceType);
            routeSequenceBuilder.setTopologyRef(
                    TopologyId.getDefaultInstance(Constants.PHY_TOPO_KEY));
            routeSequenceBuilder.setKey(new RouteSequenceKey(seqIndex));
            routeSequences.add(routeSequenceBuilder.build());
            seqIndex++;
        }
        List<CrossConnections> crossConnections = getRefCrossConnections(
                route.getCrossConnections());
        return RouteDto.builder().sites(sites).routeSequences(routeSequences)
                .crossConnections(crossConnections).build();
    }

    /**
     * special method for ots link
     *
     * @param linkId
     * @param siteId
     * @return
     */
    private ResourceType getOtsLinkRefResource(String linkId, String siteId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link otsLink =
                phyLinkDao.getPhyLinkById(linkId);
        LinkHopBuilder lb = new LinkHopBuilder();
        if (null == otsLink) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find required ots phy link:" + linkId);
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 linkPhysical =
                otsLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);

        lb.fieldsFrom(otsLink);
        lb.fieldsFrom(linkPhysical);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> refSiteLinks = siteLinkDao.listAllSiteLinkBasedOnPhyLinks(
                Collections.singletonList(linkId));
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link refSiteLink = refSiteLinks.get(
                0);
        String refSiteLinkSourceSite = refSiteLink.getSource().getSourceNode().getValue();
        String refSiteLinkDestSite = refSiteLink.getDestination().getDestNode().getValue();
        String sourceSite = null;
        String destSite = null;
        if (siteId.equals(refSiteLinkSourceSite)) {
            sourceSite = refSiteLinkSourceSite;
            destSite = refSiteLinkDestSite;
        } else if (siteId.equals(refSiteLinkDestSite)) {
            sourceSite = refSiteLinkDestSite;
            destSite = refSiteLinkSourceSite;
        }
        SupportSiteLinkBuilder supportSiteLinkBuilder = new SupportSiteLinkBuilder();
        supportSiteLinkBuilder.setLinkId(refSiteLink.getLinkId());
        supportSiteLinkBuilder.setDestSite(NodeId.getDefaultInstance(destSite));
        supportSiteLinkBuilder.setSourceSite(NodeId.getDefaultInstance(sourceSite));
        lb.setSupportSiteLink(supportSiteLinkBuilder.build());
        ResourceType linkResource = new LinkBuilder().setLinkHop(lb.build()).build();
        return linkResource;
    }

    private List<CrossConnections> getRefCrossConnections(
            List<String> crossConnections) {
        List<CrossConnections> routeXc = new ArrayList<>();
        for (String xcId : crossConnections) {
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> xcs = crossConnectionsDao.getXCByXcIdRegexLike(
                    xcId);
            CrossConnectionsBuilder xcBuilder = new CrossConnectionsBuilder();
            if (!xcs.isEmpty()) {
                xcBuilder.fieldsFrom(xcs.get(0));
                routeXc.add(xcBuilder.build());

            }
        }

        return CrossConnectionUtils.refactorSequenceCrossConnections(routeXc);
    }

    /**
     * build phy link resource type
     *
     * @param muxPanelTpId
     * @return
     */
    private ResourceType getPhyLinkResourceType(String linkId, String muxPanelTpId) {
        LinkHopBuilder lb = new LinkHopBuilder();
        if (linkId.endsWith(MPO_SUFFIX)) {
            lb = MUXPANELConstructor.generateMpoLinkRouteDetail(muxPanelTpId, lb, linkId);
        } else {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link phyLink =
                    phyLinkDao.getPhyLinkById(linkId);

            if (phyLink == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find required phy link:" + linkId);
            }

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 linkPhysical =
                    phyLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);

            lb.fieldsFrom(phyLink);
            lb.fieldsFrom(linkPhysical);
        }
        ResourceType linkResource = new LinkBuilder().setLinkHop(lb.build()).build();
        return linkResource;
    }


    private ResourceType getTerminationPointResourceType(String tpId, String muxPanelTpId) {
        if (!PhysicalTpIdNamingRule.isTpId(tpId)) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the route info is invalided");
        }

        String siteId = PhysicalTpIdNamingRule.getSiteId(tpId);
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);

        Node phyNode = getRefNode(neId);
        Node siteNode = getRefSiteNode(siteId);

        Equipments refEquipments = nodeEquipMap.get(neId).get(equipId);
        EquipType equipType = refEquipments.getEquipType();
        TerminationPoint terminationPoint = getRefTerminationPoint(nodeTpsMap.get(neId), tpId,
                equipType, muxPanelTpId);

        SiteNodeBuilder siteNodeBuilder = new SiteNodeBuilder();
        siteNodeBuilder.fieldsFrom(siteNode);
        siteNodeBuilder.fieldsFrom(siteNode.getAugmentation(Node1.class));

        PhyTpBuilder phyTpBuilder = new PhyTpBuilder();
        phyTpBuilder.fieldsFrom(terminationPoint);
        phyTpBuilder.fieldsFrom(terminationPoint.getAugmentation(TerminationPoint1.class));

        Node drawNode = drawNode(phyNode, refEquipments, terminationPoint);
        PhyNodeBuilder phyNodeBuilder = new PhyNodeBuilder();
        phyNodeBuilder.fieldsFrom(drawNode);
        phyNodeBuilder.fieldsFrom(drawNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class));

        ResourceType resourceType = new TpBuilder()
                .setTpHop(new TpHopBuilder()
                        .setSiteNode(siteNodeBuilder.build())
                        .setPhyNode(phyNodeBuilder.build())
                        .setPhyTp(phyTpBuilder.build())
                        .build())
                .build();
        return resourceType;
    }

    private TerminationPoint getRefTerminationPoint(
            Map<String, TerminationPoint> terminationPointMap, String tpId, EquipType equipType,
            String muxTpId) {

        TerminationPoint terminationPoint = terminationPointMap.get(tpId);
        if (terminationPoint == null && tpId.endsWith(MPO_SUFFIX)) {
            switch (equipType) {
                case CMUX64:
                    if (muxTpId != null) {
                        tpId = CMUX64Constructor.getMPOTpId(tpId, muxTpId);
                        terminationPoint = terminationPointMap.get(tpId);
                    } else {
                        tpId = tpId + MPO_FIST_INDEX;
                        terminationPoint = CMUX64Constructor.virtualizeMPOTP(
                                terminationPointMap.get(tpId),
                                tpId);
                    }

                    break;
                case MUXPANEL:
                    if (muxTpId != null) {
                        tpId = CMUX64Constructor.getMPOTpId(tpId, muxTpId);
                        terminationPoint = terminationPointMap.get(tpId);
                    } else {
                        tpId = tpId + MPO_FIST_INDEX;
                        terminationPoint = MUXPANELConstructor.virtualizeMPOTP(
                                terminationPointMap.get(tpId),
                                tpId);
                    }

                    break;
            }
        }

        return terminationPoint;
    }

    private Node drawNode(Node phyNode, Equipments equipments, TerminationPoint terminationPoint) {
        Physical physical = phyNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                .getPhysical();

        List<Equipments> eqList = new LinkedList<>();
        eqList.add(equipments);

        Node1Builder node1Builder = new Node1Builder();
        node1Builder.setPhysical(
                new PhysicalBuilder(physical)
                        .setCrossConnections(null)
                        .setInternalLinks(null)
                        .setEquipments(eqList)
                        .setOCMGripGroups(null)
                        .setSystem(null)
                        .build());
        return new NodeBuilder().setNodeId(phyNode.getNodeId()).addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class,
                node1Builder.build()).setTerminationPoint(
                Collections.singletonList(terminationPoint)).build();
    }

    private Node getRefSiteNode(String siteNodeId) {
        Node siteNode = null;
        if (nodesMap.containsKey(siteNodeId)) {
            siteNode = nodesMap.get(siteNodeId);
        } else {
            siteNode = siteNodeDao.getSiteNodeById(siteNodeId);
            NodeBuilder nodeBuilder = new NodeBuilder(siteNode);
            Node1 siteNodePhysical = filterSiteData(
                    siteNode);
            nodeBuilder.setSupportingNode(null);
            nodeBuilder.addAugmentation(
                    Node1.class,
                    siteNodePhysical);

            siteNode = nodeBuilder.build();
            nodesMap.put(siteNodeId, siteNode);
        }
        return siteNode;
    }

    private Node1 filterSiteData(
            Node siteTopoNode) {
        Node1 yangSite =
                siteTopoNode.getAugmentation(
                        Node1.class);

        SiteBuilder sb = new SiteBuilder(yangSite.getSite());
        sb.setSupportingRack(null);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder nb = new
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder();
        nb.setSite(sb.build());
        return nb.build();
    }

    private Node getRefNode(String neId) {
        Node phyNode = null;
        if (nodesMap.containsKey(neId)) {
            phyNode = nodesMap.get(neId);
        } else {
            phyNode = phyNodeDao.getPhyNodeById(neId);
            //refresh the termination point map and termination point map
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1 phyNodePhysical = phyNode.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class);

            List<TerminationPoint> terminationPoints = phyNode.getTerminationPoint();
            Map<String, TerminationPoint> terminationPointMap = terminationPoints.stream().collect(
                    Collectors.toMap(terminationPoint -> terminationPoint.getTpId().getValue(),
                            terminationPoint -> terminationPoint));
            Map<String, Equipments> equipmentsMap = phyNodePhysical.getPhysical()
                    .getEquipments()
                    .stream()
                    .collect(
                            Collectors.toMap(Equipments::getEquipmentId, equipments -> equipments));
            nodesMap.put(neId, phyNode);
            nodeEquipMap.put(neId, equipmentsMap);
            nodeTpsMap.put(neId, terminationPointMap);
        }
        return phyNode;
    }
}
