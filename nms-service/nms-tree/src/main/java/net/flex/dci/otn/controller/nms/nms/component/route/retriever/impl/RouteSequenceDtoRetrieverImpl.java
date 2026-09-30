package net.flex.dci.otn.controller.nms.nms.component.route.retriever.impl;

import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_FIST_INDEX;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PORT_IN_ID_SUFFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.PORT_OUT_ID_SUFFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.NMSUtils.getPortType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otn.controller.nms.constructs.MUXPANELConstructor;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteSequenceDtoRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.RouteDetailDto;
import net.flex.dci.otn.controller.nms.nms.dto.RouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.nms.enums.CustomMuxEquipTypeHandler;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DirectionTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.TpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.TpHop;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/12/2 16:04
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RouteSequenceDtoRetrieverImpl implements RouteSequenceDtoRetriever {

    private final NetconfTopology netconfTopology;


    private final CrossConnectionsDao crossConnectionsDao;


    @Override
    public RouteDetailDto retrieveRouteDetail(RouteSequenceDto routeSequenceDto) {
        long t0 = System.currentTimeMillis();
        log.info("=== [ROUTE-TIMING] start retrieveRouteDetail ===");

        RouteCacheContext routeCacheContext = new RouteCacheContext(netconfTopology,
                crossConnectionsDao);
        preloadRouteCacheContext(routeSequenceDto, routeCacheContext);

        long t1 = System.currentTimeMillis();
        log.info("[ROUTE-TIMING] preload end: {}ms", t1 - t0);

        RouteInfo primaryRoute = retrieveRoute(routeSequenceDto, routeCacheContext);

        long t2 = System.currentTimeMillis();
        log.info("[ROUTE-TIMING] primaryRoute end: {}ms", t2 - t1);

        RouteDto secondRouteSequence = null;
        List<RouteDto> tertiaryRouteSequences = new ArrayList<>();
        if (primaryRoute.secondRouteSequence != null) {
            secondRouteSequence = retrieveRoute(
                    primaryRoute.secondRouteSequence, routeCacheContext).routeDto;
            long t3 = System.currentTimeMillis();
            log.info("[ROUTE-TIMING] secondaryRoute end: {}ms", t3 - t2);
        }
        if (!CollectionUtils.isEmpty(primaryRoute.tertiaryRouteSequences)) {
            tertiaryRouteSequences = primaryRoute.tertiaryRouteSequences.stream()
                    .map(routeSequence -> retrieveRoute(routeSequence,
                            routeCacheContext).routeDto)
                    .collect(
                            Collectors.toList());
            long t4 = System.currentTimeMillis();
            log.info("[ROUTE-TIMING] tertiaryRoute end: {}ms", t4 - t2);
        }

        long tEnd = System.currentTimeMillis();
        log.info("=== [ROUTE-TIMING] total: {}ms ===", tEnd - t0);

        return RouteDetailDto.builder().primary(primaryRoute.routeDto)
                .secondary(secondRouteSequence).tertiary(tertiaryRouteSequences).build();

    }

    private void preloadRouteCacheContext(RouteSequenceDto routeSequenceDto,
            RouteCacheContext routeCacheContext) {
        log.debug("Preloading route cache context for route sequence");

        if (routeSequenceDto == null) {
            log.warn("Route sequence dto is null, skip preloading");
            return;
        }

        Set<String> neId = new HashSet<>();
        Set<String> tpIds = new HashSet<>();
        Set<String> siteLinkIds = new HashSet<>();
        Set<String> siteIds = new HashSet<>();
        Set<String> phyLinkIds = new HashSet<>();
        Set<RouteSequenceDto> visited = new HashSet<>();
        Set<String> crossConnectionIds = new HashSet<>();
        Deque<RouteSequenceDto> stack = new ArrayDeque<>();
        stack.push(routeSequenceDto);
        while (!stack.isEmpty()) {
            RouteSequenceDto current = stack.pop();
            if (current == null || visited.contains(current)) {
                continue;
            }
            visited.add(current);
            crossConnectionIds.addAll(current.getXcIds());
            collectResourceIds(current, neId, tpIds, siteLinkIds, siteIds, phyLinkIds);
            if (!CollectionUtils.isEmpty(current.getTertiary())) {
                List<RouteSequenceDto> tertiary = current.getTertiary();
                tertiary.forEach(stack::push);
            }
            if (current.getSecondary() != null) {
                stack.push(current.getSecondary());
            }
            if (current.getPrimary() != null) {
                stack.push(current.getPrimary());
            }
        }
        Set<String> equipIds = tpIds.stream().map(PhysicalTpIdNamingRule::getEquipId).collect(
                Collectors.toSet());
        //preload everything

        log.debug("Preloading ne:{} and site:{},phyLink :{},siteLink:{}", neId.size(),
                siteIds.size(), phyLinkIds.size(), siteLinkIds.size());
        routeCacheContext.preLoad(neId, siteIds, phyLinkIds, siteLinkIds, tpIds, equipIds,
                crossConnectionIds);

    }

    private void collectResourceIds(RouteSequenceDto current, Set<String> neIds, Set<String> tpIds,
            Set<String> siteLinkIds, Set<String> siteIds, Set<String> phyLinkIds) {
        log.debug("collect resource ids");
        Class<?> clazz = current.getRouteHopType().getClazz();
        if (clazz.isAssignableFrom(Tp.class)) {
            String tpId = current.getTpId();
            if (tpId.endsWith(MPO_SUFFIX)) {
                tpId = tpId + MPO_FIST_INDEX;
            }
            tpIds.add(tpId);
            String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
            String siteId = PhysicalTpIdNamingRule.getSiteId(tpId);
            neIds.add(neId);
            siteIds.add(siteId);
        } else if (clazz.isAssignableFrom(Link.class)) {
            String linkId = current.getLinkId();
            String topologyRef = current.getTopologyRef();
            if (PHY_TOPO_KEY.equals(topologyRef)) {
                phyLinkIds.add(linkId);
                preloadLinkEndpoints(linkId, neIds, siteIds, tpIds);
            } else if (SITE_TOPO_KEY.equals(topologyRef)) {
                siteLinkIds.add(linkId);
                preloadSiteLinkEndpoints(linkId, neIds, siteIds, tpIds);
            }
        }
    }

    private void preloadSiteLinkEndpoints(String linkId, Set<String> neIds, Set<String> siteIds,
            Set<String> tpIds) {
        try {
            String sourceTpId = SiteLinkIdNamingRule.getTpAId(linkId);
            String destTpId = SiteLinkIdNamingRule.getTpZId(linkId);
            tpIds.add(sourceTpId);
            tpIds.add(destTpId);

            String sourceNeId = SiteLinkIdNamingRule.getNodeA(linkId);
            String destNeId = SiteLinkIdNamingRule.getNodeZ(linkId);

            String sourceSiteId = SiteLinkIdNamingRule.getSiteA(linkId);
            String destSiteId = SiteLinkIdNamingRule.getSiteZ(linkId);

            neIds.add(sourceNeId);
            neIds.add(destNeId);

            siteIds.add(sourceSiteId);
            siteIds.add(destSiteId);

        } catch (Exception e) {
            log.warn("Failed to parse site link endpoints for linkId: {}", linkId, e);
        }
    }

    private void preloadLinkEndpoints(String linkId, Set<String> neIds, Set<String> siteIds,
            Set<String> tpIds) {
        try {
            String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
            String destTpId = PhysicalLinkIdNamingRule.getTpZId(linkId);
            tpIds.add(sourceTpId);
            tpIds.add(destTpId);

            neIds.add(PhysicalTpIdNamingRule.getNodeId(sourceTpId));
            neIds.add(PhysicalTpIdNamingRule.getNodeId(destTpId));

            siteIds.add(PhysicalTpIdNamingRule.getSiteId(sourceTpId));
            siteIds.add(PhysicalTpIdNamingRule.getSiteId(destTpId));
        } catch (Exception e) {
            log.warn("Failed to parse link endpoints for linkId: {}", linkId, e);
        }
    }


    /**
     * retrieve all the route sequence dto
     *
     * @param routeSequenceDto
     * @param routeCacheContext
     * @return
     */
    private RouteInfo retrieveRoute(RouteSequenceDto routeSequenceDto,
            RouteCacheContext routeCacheContext) {
        log.debug("start to retrieve  sequence route ");
        RouteSequenceDto primary = routeSequenceDto;
        RouteSequenceDto secondarySequence = getSecondarySequence(routeSequenceDto);
        List<RouteSequenceDto> tertiarySequence = getTertiarySequence(routeSequenceDto);
        List<RouteSequence> routeSequences = new ArrayList<>();
        List<CrossConnections> crossConnections = new ArrayList<>();

        LinkedList<String> sites = new LinkedList<>();
        Set<String> visitedTp = new HashSet<>();
        String muxPanelTpId = null;
        long seq = 1L;
        long regPortNum = 0;
        while (primary != null) {
            Class<?> clazz = primary.getRouteHopType().getClazz();
            List<CrossConnections> xcs = routeCacheContext.getCrossConnectionsByXcIds(
                    primary.getXcIds());
            crossConnections.addAll(xcs);
//            List<String> xcIds = primary.getXcIds();

            ResourceType resourceType = null;
            if (clazz.isAssignableFrom(Tp.class)) {
                String tpId = primary.getTpId();
                if (visitedTp.contains(tpId)) {
                    primary = primary.getPrimary();
                    continue;
                }

                log.debug("the tp id is tpId:{},muxPanelTpID:{}", tpId, muxPanelTpId);
                resourceType = getTerminationPointResourceType(tpId, muxPanelTpId,
                        routeCacheContext);
                PhyTp tp = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp) resourceType).getTpHop()
                        .getPhyTp();
                PortType portType = tp.getPhysical().getPortType();
                if (portType != null) {
                    if (portType.equals(PortType.MUXChannel)) {
                        muxPanelTpId = tp.getTpId().getValue();
                    } else if (portType.equals(PortType.OTULine)) {
                        boolean isRegPort = isRegPort(tpId, routeCacheContext);
                        if (isRegPort) {
                            List<ResourceType> rxTxTps = generateRxTxTps(resourceType, regPortNum);
                            for (ResourceType rt : rxTxTps) {
                                seq = addRouteSequence(routeSequences, rt, primary.getTopologyRef(),
                                        seq);
                            }
                            visitedTp.add(tpId);
                            String refSiteId = PhysicalTpIdNamingRule.getSiteId(tpId);
                            if (!sites.contains(refSiteId)) {
                                sites.add(refSiteId);
                            }
                            regPortNum++;
                            primary = primary.getPrimary();
                            continue;
                        }
                    }
                }
                visitedTp.add(tpId);
                String refSiteId = PhysicalTpIdNamingRule.getSiteId(tpId);
                if (!sites.contains(refSiteId)) {
                    sites.add(refSiteId);
                }

            } else if (clazz.isAssignableFrom(Link.class)) {
                String linkId = primary.getLinkId();
                String topologyRef = primary.getTopologyRef();
                if (Constants.SITE_TOPO_KEY.equals(topologyRef)) {
                    List<CrossConnections> cachedXcs = routeCacheContext.getSiteLinkXcs(linkId);
                    if (cachedXcs != null) {
                        crossConnections.addAll(cachedXcs);
                    }
                }
                log.debug("retrieve  topology:{} link id:{}", topologyRef, linkId);
                resourceType = getLinkResourceType(linkId, topologyRef, muxPanelTpId,
                        routeCacheContext);
            }
            seq = addRouteSequence(routeSequences, resourceType, primary.getTopologyRef(), seq);
            primary = primary.getPrimary();
        }
        return RouteInfo.builder().routeDto(
                        RouteDto.builder().routeSequences(routeSequences).crossConnections(crossConnections)
                                .sites(sites).build()).secondRouteSequence(secondarySequence)
                .tertiaryRouteSequences(tertiarySequence).build();
    }


    private long addRouteSequence(List<RouteSequence> routeSequences,
            ResourceType resourceType,
            String topologyRef,
            long seq) {
        RouteSequenceBuilder builder = new RouteSequenceBuilder();
        builder.setResourceType(resourceType);
        builder.setTopologyRef(TopologyId.getDefaultInstance(topologyRef));
        builder.setKey(new RouteSequenceKey(seq));
        routeSequences.add(builder.build());
        return seq + 1;
    }

    /**
     * generate Rx TX TPs FROM bidirectional tp
     *
     * @param tpResourceType
     * @return
     */
    private List<ResourceType> generateRxTxTps(ResourceType tpResourceType, long regPortNum) {
        TpHop tpHop = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp) tpResourceType).getTpHop();
        PhyTp tp = tpHop.getPhyTp();
        String tpId = tp.getTpId().getValue();
        log.debug("generate rx tx tps from the bidirectional tp :{}", tp.getTpId());
        List<ResourceType> resourceTypes = new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpPhysical = tp.getPhysical();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder rxTpPhysicalBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                tpPhysical);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder txTpPhysicalBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                tpPhysical);
//        rxTpPhysicalBuilder.setFriendlyName(friendlyName);
        rxTpPhysicalBuilder.setDirection(DirectionTerminationPoint.Sink);

//        txTpPhysicalBuilder.setFriendlyName(friendlyName);
        txTpPhysicalBuilder.setDirection(DirectionTerminationPoint.Source);

        //rx
        PhyTpBuilder rxPhyTpBuilder = new PhyTpBuilder(tp);
        rxPhyTpBuilder.setPhysical(rxTpPhysicalBuilder.build());
        rxPhyTpBuilder.setTpId(TpId.getDefaultInstance(tpId + Constants.PORT_IN_ID_SUFFIX));

        PhyTpBuilder txPhyTpBuilder = new PhyTpBuilder(tp);
        txPhyTpBuilder.setPhysical(txTpPhysicalBuilder.build());
        txPhyTpBuilder.setTpId(TpId.getDefaultInstance(tpId + Constants.PORT_OUT_ID_SUFFIX));

        ResourceType rxResourceType = new TpBuilder()
                .setTpHop(new TpHopBuilder(tpHop)
                        .setPhyTp(rxPhyTpBuilder.build())
                        .build())
                .build();
        //tx
        ResourceType txResourceType = new TpBuilder()
                .setTpHop(new TpHopBuilder(tpHop)
                        .setPhyTp(txPhyTpBuilder.build())
                        .build())
                .build();

        if ((regPortNum & 1) == 0) {
            resourceTypes.add(rxResourceType);
            resourceTypes.add(txResourceType);
        } else {
            resourceTypes.add(txResourceType);
            resourceTypes.add(rxResourceType);
        }

        return resourceTypes;
    }

    /**
     * detect current tp is reg port or not
     *
     * @param tpId
     * @param routeCacheContext
     * @return
     */
    private boolean isRegPort(String tpId, RouteCacheContext routeCacheContext) {
        log.debug("detective current port is reg port or not ,tpId:{}", tpId);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> phyLinks = routeCacheContext.getTpRefPhyLinks(
                tpId);
        long unidirectionalPhyLinkCount = phyLinks.stream().map(phyLink -> phyLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                .getPhysical()).filter(linkAttribute -> linkAttribute.getDirection().equals(
                LinkDirection.Unidirection)).count();
        return unidirectionalPhyLinkCount == 2L;
    }


    /**
     * GET LINK RESOURCE TYPE by topology type sitelink and phy link
     *
     * @param linkId
     * @param topologyRef
     * @param muxPanelTpId
     * @param routeCacheContext
     * @return
     */
    private ResourceType getLinkResourceType(String linkId, String topologyRef,
            String muxPanelTpId, RouteCacheContext routeCacheContext) {
        log.debug("build link resource linkId:{} topologyRef:{} muxPanelTpId:{}", linkId,
                topologyRef, muxPanelTpId);
        ResourceType linkResource = new LinkBuilder().build();
        if (topologyRef.equals(PHY_TOPO_KEY)) {
            linkResource = buildPhyLinkResource(linkId, muxPanelTpId, routeCacheContext);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            linkResource = buildSiteLinkResource(linkId, muxPanelTpId, routeCacheContext);
        }

//        ResourceType linkResource = new LinkBuilder().setLinkHop(lb.build()).build();
        return linkResource;

    }

    /**
     * build site link resource
     *
     * @param linkId
     * @param routeCacheContext
     * @return
     */
    private ResourceType buildSiteLinkResource(String linkId, String muxPanelTpId,
            RouteCacheContext routeCacheContext) {
        log.debug("build site link resource,site link:{}", linkId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink = routeCacheContext.getSiteLink(
                linkId);
        if (Objects.isNull(siteLink)) {
            log.error("cannot find required site link:{}", linkId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find required site link:" + linkId);
        }
        TpId sourceTp = getRefTpWithMuxPanelTpId(siteLink.getSource().getSourceTp(), muxPanelTpId,
                routeCacheContext);
        TpId destTp = getRefTpWithMuxPanelTpId(siteLink.getDestination().getDestTp(), muxPanelTpId,
                routeCacheContext);
        NodeId sourceNode = siteLink.getSource().getSourceNode();
        NodeId destNode = siteLink.getDestination().getDestNode();
        LinkHopBuilder linkHopBuilder = new LinkHopBuilder();
        linkHopBuilder.fieldsFrom(siteLink);
        Site siteLinkPhysical = siteLink.getAugmentation(
                Link1.class).getSite();
        linkHopBuilder.setSource(new SourceBuilder().setSourceTp(sourceTp).setSourceNode(
                sourceNode).build());
        linkHopBuilder.setDestination(
                new DestinationBuilder().setDestTp(destTp).setDestNode(destNode).build());
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder physicalBuilder
                = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder();
        physicalBuilder.fieldsFrom(siteLinkPhysical);
        linkHopBuilder.setPhysical(physicalBuilder.build());
        return new LinkBuilder().setLinkHop(linkHopBuilder.build()).build();
    }

    private TpId getRefTpWithMuxPanelTpId(TpId tp, String muxPanelTpId,
            RouteCacheContext routeCacheContext) {
        String tpId = tp.getValue();
        log.debug("get ref tp:{} with mux panel Tp Id:{}", tpId, muxPanelTpId);
        String equipmentId = PhysicalTpIdNamingRule.getEquipId(tpId);
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Equipments equipment = routeCacheContext.getNodeEquips(neId).get(equipmentId);
        EquipType equipType = equipment.getEquipType();
        if (equipType != EquipType.MUXPANEL && equipType != EquipType.CMUX64) {
            return tp;
        }
        CustomMuxEquipTypeHandler handler = CustomMuxEquipTypeHandler.valueOf(equipType.name());
        String virtualizingTpId = handler.getMpoTpId(tpId, muxPanelTpId,
                equipment.getEquipTypeVendorSpecific());
        return TpId.getDefaultInstance(virtualizingTpId);
    }

    /**
     * build phy link resource
     *
     * @param linkId
     * @param muxPanelTpId
     * @param routeCacheContext
     * @return
     */
    private ResourceType buildPhyLinkResource(String linkId, String muxPanelTpId,
            RouteCacheContext routeCacheContext) {
        log.debug("build phyLink resource,link id:{} and muxPanelTpId:{}", linkId, muxPanelTpId);
        LinkHopBuilder lb = new LinkHopBuilder();
        if (linkId.endsWith(MPO_SUFFIX)) {
            lb = MUXPANELConstructor.generateMpoLinkRouteDetail(muxPanelTpId, lb, linkId);
        } else {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link phyLink =
                    routeCacheContext.getPhyLink(linkId);

            if (phyLink == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find required phy link:" + linkId);
            }

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 linkPhysical =
                    phyLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
            LinkDirection linkDirection = linkPhysical.getPhysical().getDirection();
            if (linkDirection.equals(LinkDirection.Unidirection)) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder phyLinkBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder(
                        phyLink);
                String sourceTpId = phyLink.getSource().getSourceTp().getValue();
                String destTpId = phyLink.getDestination().getDestTp().getValue();
                String sourceNeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
                String destNeId = PhysicalTpIdNamingRule.getNodeId(destTpId);
                TerminationPoint sourceTp = routeCacheContext.getNodeTps(sourceNeId)
                        .get(sourceTpId);
                TerminationPoint destTp = routeCacheContext.getNodeTps(destNeId)
                        .get(destTpId);
                PortType sourceTpPortType = getPortType(sourceTp);
                PortType destTpPortType = getPortType(destTp);
                if (sourceTpPortType.equals(PortType.OTULine)) {
                    //out tp
                    sourceTpId = sourceTpId + PORT_OUT_ID_SUFFIX;
                    phyLinkBuilder.setSource(new SourceBuilder(phyLink.getSource()).setSourceTp(
                            TpId.getDefaultInstance(sourceTpId)).build());
                } else if (destTpPortType.equals(PortType.OTULine)) {
                    //in tp
                    destTpId = destTpId + PORT_IN_ID_SUFFIX;
                    phyLinkBuilder.setDestination(
                            new DestinationBuilder(phyLink.getDestination()).setDestTp(
                                    TpId.getDefaultInstance(destTpId)).build());
                }
                String phyLinkId = PhysicalLinkIdNamingRule.createLinkId(sourceTpId, destTpId,
                        LinkType.OsLink);
                phyLinkBuilder.setLinkId(LinkId.getDefaultInstance(phyLinkId));
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link builtLink = phyLinkBuilder.build();

                lb.setSource(builtLink.getSource());
                lb.setDestination(builtLink.getDestination());
                lb.setPhysical(linkPhysical.getPhysical());
                lb.setLinkId(LinkId.getDefaultInstance(phyLinkId));
            } else {
//                lb.fieldsFrom(phyLink);
//                lb.setPhysical(linkPhysical.getPhysical());
                lb.setSource(phyLink.getSource());
                lb.setDestination(phyLink.getDestination());
                lb.setLinkId(phyLink.getLinkId());
                lb.setPhysical(linkPhysical.getPhysical());
            }
        }
        return new LinkBuilder().setLinkHop(lb.build()).build();
    }


    private ResourceType getTerminationPointResourceType(String tpId, String muxTpId,
            RouteCacheContext routeCacheContext) {
        if (!PhysicalTpIdNamingRule.isTpId(tpId)) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the route info is invalided");
        }

        String siteId = PhysicalTpIdNamingRule.getSiteId(tpId);
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);

        Node phyNode = routeCacheContext.getPhyNode(neId);
        Node siteNode = routeCacheContext.getSiteNode(siteId);

        Equipments refEquipments = routeCacheContext.getNodeEquips(neId).get(equipId);
        EquipType equipType = refEquipments.getEquipType();
        String equipTypeVendorSpecific = refEquipments.getEquipTypeVendorSpecific();
        TerminationPoint terminationPoint = getRefTerminationPoint(
                routeCacheContext.getNodeTps(neId), tpId,
                equipType, equipTypeVendorSpecific, muxTpId);

        SiteNodeBuilder siteNodeBuilder = new SiteNodeBuilder();
        siteNodeBuilder.setNodeId(siteNode.getNodeId());
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 siteAug =
                siteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        if (siteAug != null) {
            siteNodeBuilder.setSite(siteAug.getSite());
        }

//        PhyTpBuilder phyTpBuilder = new PhyTpBuilder();
//        phyTpBuilder.fieldsFrom(terminationPoint);
//        phyTpBuilder.fieldsFrom(terminationPoint.getAugmentation(TerminationPoint1.class));
//        log.debug("tp port type is :{}", phyTpBuilder.getPhysical().getPortType());
//
//        Node drawNode = drawNode(phyNode, refEquipments, terminationPoint);
//        PhyNodeBuilder phyNodeBuilder = new PhyNodeBuilder();
//        phyNodeBuilder.fieldsFrom(drawNode);
//        phyNodeBuilder.fieldsFrom(drawNode.getAugmentation(
//                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class));
        PhyTpBuilder phyTpBuilder = new PhyTpBuilder();
        phyTpBuilder.setTpId(terminationPoint.getTpId());
        phyTpBuilder.setPhysical(
                terminationPoint.getAugmentation(TerminationPoint1.class).getPhysical());
        log.debug("tp port type is :{}", phyTpBuilder.getPhysical().getPortType());

        Node drawNode = drawNode(phyNode, refEquipments, terminationPoint);
        PhyNodeBuilder phyNodeBuilder = new PhyNodeBuilder();
        phyNodeBuilder.setNodeId(drawNode.getNodeId());
        phyNodeBuilder.setPhysical(drawNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                .getPhysical());

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
            String equipVendorSpecific,
            String muxTpId) {

        TerminationPoint terminationPoint = terminationPointMap.get(tpId);
        PortType portType =
                terminationPoint != null ? terminationPoint.getAugmentation(TerminationPoint1.class)
                        .getPhysical()
                        .getPortType() : null;
        if (portType != null && portType.equals(PortType.OPCMPOLAG)) {
            terminationPoint = null;
        }
        if (terminationPoint == null && tpId.endsWith(MPO_SUFFIX)) {
            CustomMuxEquipTypeHandler handler = CustomMuxEquipTypeHandler.valueOf(equipType.name());
            if (handler.supportsMux() && muxTpId != null) {
                tpId = handler.getMpoTpId(tpId, muxTpId, equipVendorSpecific);
                terminationPoint = terminationPointMap.get(tpId);
            } else {
                tpId = tpId + MPO_FIST_INDEX;
                TerminationPoint baseTp = terminationPointMap.get(tpId);
                terminationPoint = handler.virtualize(baseTp, tpId);
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

    private Node extractSiteNode(Node siteNode) {
        NodeBuilder nodeBuilder = new NodeBuilder(siteNode);
        Node1 siteNodePhysical = filterSiteData(
                siteNode);
        nodeBuilder.setSupportingNode(null);
        nodeBuilder.addAugmentation(
                Node1.class,
                siteNodePhysical);

        return nodeBuilder.build();
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

//    ResourceType resourceType = pathRouteObject.getResourceType();
//        TopologyId topologyId = pathRouteObject.getTopologyRef();
//        RouteSequenceDto routeSequence = new RouteSequenceDto();
//        routeSequence.setTopologyRef(topologyId);
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType tpResourceType
//                = convertTpHop2DetailResource((Tp) resourceType);
//        routeSequence.setResourceType(tpResourceType);
//        String refSiteId = ((Tp) resourceType).getTpHop().getSiteRef().getValue();
//        routeSequence.setSiteSet(Collections.singleton(refSiteId));
//        routeSequenceDto.setPrimary(routeSequence);
//        routeSequenceDto = routeSequence;
//        return routeSequenceDto;

    private List<RouteSequenceDto> getTertiarySequence(RouteSequenceDto routeSequenceDto) {
        List<RouteSequenceDto> tertiaryNodes = null;
        while (routeSequenceDto != null) {
            if (routeSequenceDto.getTertiary() != null) {
                tertiaryNodes = routeSequenceDto.getTertiary();
            }
            routeSequenceDto = routeSequenceDto.getPrimary();
        }
        return tertiaryNodes;
    }


    private RouteSequenceDto getSecondarySequence(RouteSequenceDto routeSequenceDto) {
        RouteSequenceDto secondNode = null;
        while (routeSequenceDto != null) {
            if (routeSequenceDto.getSecondary() != null) {
                secondNode = routeSequenceDto.getSecondary();
            }
            routeSequenceDto = routeSequenceDto.getPrimary();
        }
        return secondNode;
    }


    @Data
    @Builder
    private static class RouteInfo {

        private RouteDto routeDto;

        private RouteSequenceDto secondRouteSequence;

        private List<RouteSequenceDto> tertiaryRouteSequences;
    }


    private static class RouteCacheContext {

        private final NetconfTopology netconfTopology;

        private final CrossConnectionsDao crossConnectionsDao;

        private final Map<String, Node> siteNodeCache = new ConcurrentHashMap<>();
        private final Map<String, Node> phyNodeCache = new ConcurrentHashMap<>();
        private final Map<String, CrossConnections> crossConnectionsCache = new ConcurrentHashMap<>();
        private final Map<String, List<CrossConnections>> siteLinkXcCache = new ConcurrentHashMap<>();
        private final Map<String, Map<String, TerminationPoint>> nodeTpsCache = new ConcurrentHashMap<>();
        private final Map<String, Map<String, Equipments>> nodeEquipCache = new ConcurrentHashMap<>();
        private final Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinkCache = new ConcurrentHashMap<>();
        private final Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> phyLinkCache = new ConcurrentHashMap<>();
        private final Map<String, List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link>> tpRefPhyLinksCache = new ConcurrentHashMap<>();

        RouteCacheContext(NetconfTopology netconfTopology,
                CrossConnectionsDao crossConnectionsDao) {
            this.netconfTopology = netconfTopology;
            this.crossConnectionsDao = crossConnectionsDao;
        }

        void preLoad(Set<String> neIds, Set<String> siteIds, Set<String> phyLinkIds,
                Set<String> siteLinkIds, Set<String> tpIds, Set<String> equipIds,
                Set<String> crossConnectionIds) {
            long t0 = System.currentTimeMillis();
            Map<String, CrossConnections> realXcMap = crossConnectionsDao
                    .listAllRealXcByXcIds(new ArrayList<>(crossConnectionIds))
                    .stream()
                    .collect(Collectors.toMap(
                            xc -> xc.getCrossConnectionId().getValue(),
                            xc -> new CrossConnectionsBuilder(xc).build()
                    ));
            crossConnectionsCache.putAll(
                    realXcMap
            );
            long t1 = System.currentTimeMillis();
            siteNodeCache.putAll(netconfTopology.batchGetSiteNodesLight(new ArrayList<>(siteIds)));
            long t2 = System.currentTimeMillis();
            long tn = t2 - t1;
            long t7 = System.currentTimeMillis();
            Map<String, List<CrossConnections>> xcMap = buildXcMap(siteLinkIds);
            siteLinkXcCache.putAll(xcMap);
            long t8 = System.currentTimeMillis();
            long tx = t8 - t7;
            phyNodeCache.putAll(netconfTopology.batchGetNeNodesLight(new ArrayList<>(neIds)));
            long t3 = System.currentTimeMillis();

            phyLinkCache.putAll(netconfTopology.batchGetPhyLinksLight(phyLinkIds));
            long t4 = System.currentTimeMillis();

            siteLinkCache.putAll(netconfTopology.batchGetSiteLinksLight(siteLinkIds));
            long t5 = System.currentTimeMillis();

            if (!CollectionUtils.isEmpty(tpIds)) {
                nodeTpsCache.putAll(netconfTopology.batchGetNeTpMap(neIds, tpIds));
            }
            long t6 = System.currentTimeMillis();
            if (!CollectionUtils.isEmpty(equipIds)) {
                nodeEquipCache.putAll(netconfTopology.batchGetNeEquipMap(neIds, equipIds));
            }
            if (!CollectionUtils.isEmpty(tpIds)) {
                List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> allPhyLinks =
                        netconfTopology.getTpsRefPhyLinks(new ArrayList<>(tpIds));
                for (String tpId : tpIds) {
                    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> linksForTp = allPhyLinks.stream()
                            .filter(link -> tpId.equals(link.getSource().getSourceTp().getValue())
                                    || tpId.equals(link.getDestination().getDestTp().getValue()))
                            .collect(Collectors.toList());
                    tpRefPhyLinksCache.put(tpId, linksForTp);
                }
            }
            log.info(
                    "[CACHE-TIMING] preload: siteNodes={}ms | siteLinkXcs={}ms/{} | neNodes={}ms/{} | phyLinks={}ms/{} | siteLinks={}ms/{} | tps={}ms/{} | total={}ms",
                    tn, tx, siteLinkIds.size(), t3 - t2, neIds.size(), t4 - t3, phyLinkIds.size(),
                    t5 - t4, siteLinkIds.size(), t6 - t5, nodeTpsCache.size(), t5 - t0);
        }

        private Map<String, List<CrossConnections>> buildXcMap(Set<String> siteLinkIds) {
            Map<String, List<String>> xcMap =
                    netconfTopology.aggregateSiteLinkXcs(siteLinkIds);
            List<String> allXcIds = xcMap.values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toList());
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcList = crossConnectionsDao.listAllRealXcByXcIds(
                    allXcIds);
            Map<String, List<String>> xcIdToLinkMap = new HashMap<>();
            xcMap.forEach((linkId, xcs) ->
                    xcs.forEach(xc ->
                            xcIdToLinkMap.computeIfAbsent(
                                    xc, k -> new ArrayList<>()).add(linkId)
                    )
            );

            Map<String, List<CrossConnections>> realXcMap = new HashMap<>();
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections realXc : realXcList) {
                String xcId = realXc.getCrossConnectionId().getValue();
                List<String> linkIds = xcIdToLinkMap.get(xcId);
                if (linkIds != null) {
                    for (String linkId : linkIds) {
                        realXcMap.computeIfAbsent(linkId, k -> new ArrayList<>())
                                .add(new CrossConnectionsBuilder(realXc).build());
                    }
                }
            }
            return realXcMap;
        }

        Node getPhyNode(String neId) {
            if (!phyNodeCache.containsKey(neId)) {
                log.warn("[CACHE-MISS] getPhyNode neId: {}", neId);
            }
            return phyNodeCache.computeIfAbsent(neId, netconfTopology::getNeNode);
        }

        Node getSiteNode(String siteId) {
            if (!siteNodeCache.containsKey(siteId)) {
                log.warn("[CACHE-MISS] getSiteNode siteId: {}", siteId);
            }
            return siteNodeCache.computeIfAbsent(siteId, netconfTopology::getSiteNode);
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link getSiteLink(
                String linkId) {
            return siteLinkCache.computeIfAbsent(linkId, netconfTopology::getSiteLink);
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link getPhyLink(
                String linkId) {
            return phyLinkCache.computeIfAbsent(linkId, netconfTopology::getPhyLink);
        }

        Map<String, TerminationPoint> getNodeTps(String neId) {
            return nodeTpsCache.computeIfAbsent(neId, k -> new HashMap<>());
        }

        Map<String, Equipments> getNodeEquips(String neId) {
            return nodeEquipCache.computeIfAbsent(neId, k -> new HashMap<>());
        }

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> getTpRefPhyLinks(
                String tpId) {
            return tpRefPhyLinksCache.computeIfAbsent(tpId, netconfTopology::getTpRefPhyLinks);
        }

        public List<CrossConnections> getSiteLinkXcs(String linkId) {
            return siteLinkXcCache.get(linkId);
        }

        public List<CrossConnections> getCrossConnectionsByXcIds(List<String> xcIds) {
            return xcIds.stream()
                    .map(crossConnectionsCache::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }
}