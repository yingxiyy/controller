/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.*;
import net.flex.dci.otn.controller.allocate.Dijkstra.DJNode;
import net.flex.dci.otn.controller.allocate.Dijkstra.Dijkstra;
import net.flex.dci.otn.controller.allocate.Dijkstra.Graph;
import net.flex.dci.otn.controller.allocate.astar.MandatoryRouteAStar;
import net.flex.dci.otn.controller.allocate.common.util.CommonUtils;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2OutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reuesed.och.ReuesedOch;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reuesed.och.ReuesedOchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reuesed.och.reuesed.och.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reuesed.och.reuesed.och.LinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ReusedRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ReusedRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ShortPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ShortPathBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RouteRestriction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.PrimaryFriendly;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.PrimaryFriendlyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.PrimaryReg;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.PrimaryRegBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class TunnelComputer2 {

    public static final String SITE_LINK_PREFIX = "SiteLink";
    public static final String WSS_LINK_PREFIX = "WssLink";
    @Value("${computed.route.limit:5}")
    private Integer COMPUTED_ROUTE_LIMIT;
    @Value("${tunnel.route.mandatory-resources-ordered:false}")
    private Boolean mandatoryResourcesOrdered;
    @Autowired
    private TopologyDao topologyDao;
    @Autowired
    private SiteLinkDao siteLinkDao;
    @Autowired
    private OchLinkDao ochLinkDao;
    @Autowired
    private PhyLinkDao phyLinkDao;
    @Autowired
    private TunnelUtils tunnelUtils;
    @Autowired
    private NeDesigner neDesigner;
    @Autowired
    private TunnelUtilService tunnelUtilService;
    @Autowired
    private SiteNodeDao siteNodeDao;

    public ComputeTunnels2Output doIt(ComputeTunnels2Input input) throws CommonException {

        validate(input);
        List<ShortPath> shortPath = getShortPaths(input);

        ComputeTunnels2Output output = new ComputeTunnels2OutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .setReuesedOch(Collections.EMPTY_LIST)
//                .setReuesedOch(reusedOchPair.getLeft())
                .setReusedRoute(Collections.EMPTY_LIST)
//                .setReusedRoute(reusedRoutes)
                .setShortPath(shortPath).build();
        return output;
    }

    /**
     * Computes an unprotected third-leg route at the frequency already used by the OCH.
     * The result deliberately exposes only siteLinks and possible REG sites; WSS links are
     * recomputed after the UI chooses REG or ROADM and submits bind-tunnel.
     */
    public List<SiteLinkRoute> getBindingRoutes(Link ochLink, Tunnel tunnel) throws NeDesignerException {
        Och och = ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
        Set<String> occupiedSiteLinks = ochLink.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .collect(Collectors.toSet());
        if (occupiedSiteLinks.isEmpty()) {
            log.debug("No occupied siteLink found for binding tunnel:{}, ochLink:{}",
                    tunnel.getTunnelId().getValue(), ochLink.getLinkId().getValue());
            return Collections.emptyList();
        }

        Link referenceSiteLink = siteLinkDao.getSiteLinkById(occupiedSiteLinks.iterator().next());
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site site =
                referenceSiteLink.getAugmentation(Link1.class).getSite();
        WDM_Band wdmBand = CommonUtils.getWDMBand(site);
        Boolean originalHasDummy = getBindingDummyMode(occupiedSiteLinks);

        String srcSiteId = PhysicalTpIdNamingRule.getSiteId(ochLink.getSource().getSourceTp().getValue());
        String destSiteId = PhysicalTpIdNamingRule.getSiteId(ochLink.getDestination().getDestTp().getValue());
        long centerFrequency = (och.getLowerFrequency().getValue().longValue()
                + och.getUpperFrequency().getValue().longValue()) / 2;
        long lowerFrequency = och.getLowerFrequency().getValue().longValue();
        long upperFrequency = och.getUpperFrequency().getValue().longValue();

        String bindingGrid = getBindingGridFromOch(och);
        List<String> grids = Arrays.asList(TunnelUtils.GRID_0, bindingGrid);
        // Diagnostic logs keep the binding filters visible without changing route selection.
        log.debug("Start getBindingRoutes tunnel:{}, ochLink:{}, srcSite:{}, destSite:{}, centerFrequency:{}, bindingGrid:{}, "
                        + "wdmBand:{}, occupiedSiteLinks:{}",
                tunnel.getTunnelId().getValue(), ochLink.getLinkId().getValue(), srcSiteId, destSiteId, centerFrequency,
                bindingGrid, wdmBand, occupiedSiteLinks);
        Map<String, List<Link>> routePools = getGridSitelinkMapFromDB(site.getRiskGroupName(),
                site.getPlaneName(), site.getPlaneId(), grids, occupiedSiteLinks, wdmBand);
        routePools = normalizeBindingRoutePools(routePools, bindingGrid);
        // Binding third leg must keep the same dummy-injection mode as the original OCH route.
        routePools = filterBindingRoutePools(routePools, originalHasDummy);
        Map<String, Link> routePoolLinks = getRoutePoolLinksById(routePools);
        log.debug("Binding route pools after exclude occupied and incompatible siteLinks: {}",
                routePools.entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> getLinkIds(entry.getValue()))));
        List<ShortPath> candidates = new ArrayList<>();
        for (Map.Entry<String, List<Link>> entry : routePools.entrySet()) {
            String fixGrid = entry.getKey();
            List<Link> pool = entry.getValue();
            List<Link> sourceLinks = pool.stream()
                    .filter(link -> inSite(link, srcSiteId) && isOtm(link, srcSiteId))
                    .collect(Collectors.toList());
            List<Link> destLinks = pool.stream()
                    .filter(link -> inSite(link, destSiteId) && isOtm(link, destSiteId))
                    .collect(Collectors.toList());
            log.debug("Binding grid:{} poolSize:{}, sourceLinks:{}, destLinks:{}",
                    fixGrid, pool.size(), getLinkIds(sourceLinks), getLinkIds(destLinks));
            List<ShortPath> gridCandidates = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId,
                    sourceLinks, destLinks, pool, 1, och.getCardType(), wdmBand);
            log.debug("Binding grid:{} candidate count:{}, candidates:{}",
                    fixGrid, gridCandidates.size(), summarizeShortPaths(gridCandidates));
            candidates.addAll(gridCandidates);
        }
        log.debug("Binding candidate count before exact frequency filter:{}, centerFrequency:{}",
                candidates.size(), centerFrequency);

        List<ShortPath> exactFrequencyRoutes = candidates.stream()
                .filter(path -> {
                    boolean availableForOch = isBindingFrequencyAvailable(path, routePoolLinks,
                            lowerFrequency, upperFrequency);
                    log.debug("Binding candidate exact-frequency check centerFrequency:{}, lowerFrequency:{}, "
                                    + "upperFrequency:{}, availableForOch:{}, candidate:{}",
                            centerFrequency, lowerFrequency, upperFrequency, availableForOch,
                            summarizeShortPath(path));
                    return availableForOch;
                })
                .map(path -> {
                    List<String> siteLinkIds = path.getSiteLinkRoute().getPrimary().stream()
                            .filter(SiteLinkIdNamingRule::isSiteLink)
                            .collect(Collectors.toList());
                    SiteLinkRoute route = new SiteLinkRouteBuilder(path.getSiteLinkRoute())
                            .setPrimary(siteLinkIds)
                            .setCentralFrequencies(Collections.singletonList(centerFrequency))
                            .build();
                    return new ShortPathBuilder(path).setSiteLinkRoute(route).build();
                })
                .limit(COMPUTED_ROUTE_LIMIT)
                .collect(Collectors.toList());
        log.debug("Binding exact-frequency route count:{}, routeLimit:{}, routes:{}",
                exactFrequencyRoutes.size(), COMPUTED_ROUTE_LIMIT, summarizeShortPaths(exactFrequencyRoutes));
        return addFriendlyNameAndReg(exactFrequencyRoutes).stream()
                .map(ShortPath::getSiteLinkRoute)
                .collect(Collectors.toList());
    }

    private Map<String, Link> getRoutePoolLinksById(Map<String, List<Link>> routePools) {
        return routePools.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(link -> link.getLinkId().getValue(), link -> link,
                        (left, right) -> left));
    }

    private Map<String, List<Link>> filterBindingRoutePools(Map<String, List<Link>> routePools,
                                                            Boolean originalHasDummy) {
        return routePools.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().stream()
                        .filter(siteLink -> isImplementedSiteLink(siteLink))
                        .filter(siteLink -> isDummyModeMatched(originalHasDummy, hasDummy(siteLink)))
                        .collect(Collectors.toList())));
    }

    private Boolean getBindingDummyMode(Set<String> occupiedSiteLinks) {
        Boolean hasDummy = null;
        for (String siteLinkId : occupiedSiteLinks) {
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            boolean currentHasDummy = hasDummy(siteLink);
            if (hasDummy == null) {
                hasDummy = currentHasDummy;
            } else if (!hasDummy.equals(currentHasDummy)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Inconsistent dummy link");
            }
        }
        return hasDummy;
    }

    private boolean isImplementedSiteLink(Link siteLink) {
        return siteLink != null
                && siteLink.getAugmentation(Link1.class) != null
                && siteLink.getAugmentation(Link1.class).getSite() != null
                && siteLink.getAugmentation(Link1.class).getSite().getImplementState() == ImplementState.Implement;
    }

    private boolean hasDummy(Link siteLink) {
        if (siteLink == null || siteLink.getAugmentation(Link1.class) == null
                || siteLink.getAugmentation(Link1.class).getSite() == null) {
            return false;
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site site =
                siteLink.getAugmentation(Link1.class).getSite();
        return site.getDummyLink() != null && !site.getDummyLink().isEmpty();
    }

    boolean isDummyModeMatched(Boolean originalHasDummy, Boolean candidateHasDummy) {
        return originalHasDummy != null && originalHasDummy.equals(candidateHasDummy);
    }

    private boolean isBindingFrequencyAvailable(ShortPath path, Map<String, Link> routePoolLinks,
                                                long lowerFrequency, long upperFrequency) {
        List<String> siteLinkIds = path.getSiteLinkRoute().getPrimary().stream()
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .collect(Collectors.toList());
        if (siteLinkIds.isEmpty()) {
            return false;
        }

        return siteLinkIds.stream()
                .allMatch(siteLinkId -> {
                    Link siteLink = routePoolLinks.get(siteLinkId);
                    // Binding a new protection leg consumes spectrum on every traversed siteLink.
                    // Occupied same-frequency OCHs are not treated as free capacity here.
                    boolean available = siteLink != null
                            && CommonUtils.isSiteLinkAvailableForOch(siteLink, lowerFrequency, upperFrequency);
                    log.debug("Binding siteLink frequency check siteLink:{}, lowerFrequency:{}, "
                                    + "upperFrequency:{}, available:{}",
                            siteLinkId, lowerFrequency, upperFrequency, available);
                    return available;
                });
    }

    private List<String> getLinkIds(List<Link> links) {
        if (links == null) {
            return Collections.emptyList();
        }
        return links.stream().map(link -> link.getLinkId().getValue()).collect(Collectors.toList());
    }

    private List<String> summarizeShortPaths(List<ShortPath> paths) {
        if (paths == null) {
            return Collections.emptyList();
        }
        return paths.stream().map(this::summarizeShortPath).collect(Collectors.toList());
    }

    private String summarizeShortPath(ShortPath path) {
        if (path == null || path.getSiteLinkRoute() == null) {
            return "null";
        }
        SiteLinkRoute siteLinkRoute = path.getSiteLinkRoute();
        return "primary=" + siteLinkRoute.getPrimary() + ", centralFrequencies="
                + siteLinkRoute.getCentralFrequencies();
    }

    private String getBindingGridFromOch(Och och) throws NeDesignerException {
        long lowerFrequency = och.getLowerFrequency().getValue().longValue();
        long upperFrequency = och.getUpperFrequency().getValue().longValue();
        long frequencyWidth = upperFrequency - lowerFrequency;
        if (frequencyWidth <= 0 || frequencyWidth % 1000 != 0) {
            throw new NeDesignerException("Invalid OCH frequency width for binding: " + frequencyWidth);
        }

        // Binding must reuse the existing OCH spectrum width. Flex-grid siteLinks
        // have grid=0, but using their default 75GHz grid would miss 100/150GHz OCHs.
        String bindingGrid = String.valueOf(frequencyWidth / 1000);
        tunnelUtils.getGridByString(bindingGrid);
        return bindingGrid;
    }

    private Map<String, List<Link>> normalizeBindingRoutePools(Map<String, List<Link>> routePools,
                                                               String bindingGrid) {
        if (routePools.containsKey(bindingGrid)) {
            return routePools;
        }

        List<Link> links = routePools.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (links.isEmpty()) {
            return routePools;
        }

        // getGridSitelinkMapFromDB maps pure flex-grid pools to 75GHz by default.
        // For binding, those same flex resources must be evaluated with the OCH width.
        Map<String, List<Link>> normalized = new HashMap<>();
        normalized.put(bindingGrid, links);
        return normalized;
    }

    private void validate(ComputeTunnels2Input input) {
        if (input.getSrcSite().equals(input.getDstSite())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "src and dst site can't be the sama");
        }

    }

    protected List<ShortPath> getShortPaths(ComputeTunnels2Input input) {
        ParamCompute2 param = new ParamCompute2();
        param.parser(input);
        MandatoryRouteConstraints routeConstraints =
                MandatoryRouteConstraints.from(input.getRouteRestriction());
        boolean useMandatoryAStar = validateAndShouldUseMandatoryAStar(
                routeConstraints, mandatoryResourcesOrdered);
        if (!routeConstraints.getAllMandatorySiteLinkIds().isEmpty()) {
            // The reused-OCH DAO can use the union as a coarse filter. Exact primary,
            // secondary and third matching is still checked after each route is built.
            param.setMandatorySiteLinks(new ArrayList<>(
                    routeConstraints.getAllMandatorySiteLinkIds()));
        }
        if (useMandatoryAStar) {
            log.debug("Use A* for mandatory tunnel route. primary sites:{}, links:{}; "
                            + "secondary sites:{}, links:{}; third sites:{}, links:{}",
                    routeConstraints.get(0).getSiteIds(), routeConstraints.get(0).getSiteLinkIds(),
                    routeConstraints.get(1).getSiteIds(), routeConstraints.get(1).getSiteLinkIds(),
                    routeConstraints.get(2).getSiteIds(), routeConstraints.get(2).getSiteLinkIds());
        }

        WDM_Band wdmBand = null;
        String opMode = null;
        String vendorNameFlag = input.getVendorOccupationRate().get(0).getVendorName();
        String productTypeFlag = input.getVendorOccupationRate().get(0).getProductType();
        try {

            Pair<String, WDM_Band> pair = tunnelUtilService.getOpModeAndWdm(vendorNameFlag, productTypeFlag, input.getFrequenceWidth(), input.getCardType(), input
                    .getServiceType());
            opMode = pair.getLeft();
            wdmBand = pair.getRight();
        } catch (NeDesignerException e) {
            log.error("Failed to get opMode or wdmBand.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "failed to get opMode or wdmBand" + e.toString(), e);

        }
        List<ShortPath> shortPath = new ArrayList<>();

        Pair<List<ReuesedOch>, List<ReusedRoute>> reusedOchPair = getReUsedOchScenario(param, opMode);
        List<ReusedRoute> reusedRoutes = reusedOchPair.getRight();
        if (useMandatoryAStar) {
            // DAO filtering sees the OCH as a whole; validate each protection leg against
            // its own index before a reused route can be offered.
            reusedRoutes = reusedRoutes.stream()
                    .filter(route -> matchesMandatoryResources(
                            route.getSiteLinkRoute(), routeConstraints))
                    .collect(Collectors.toList());
        }
        List<ShortPath> shortPathReused = convertToShortPath(reusedRoutes);
        shortPath.addAll(shortPathReused);

//        List<Link> reusedOch = ochLinkDao.filter(param.getSrcSite(), param.getDesSite(),param.getMandatorySiteLinks() , param.getProtectionType(), param.getRiskGroupName(), param
//                .getPlaneName(), param.getOchOduGranularity(),param.getCardType(),param.getServiceType());

        try {
            // Ordinary requests retain the original Dijkstra implementation byte-for-byte.
            // A* is selected only when at least one route leg has mandatory resources.
            shortPath = useMandatoryAStar
                    ? getShortPathWithMandatoryResources(param, wdmBand, routeConstraints)
                    : getShortPath(param, wdmBand);
        } catch (NeDesignerException e) {
            log.error("Failed to do TunnelComputer2 for shortPath.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "compute shortPath error:" + e.toString(), e);
        }

        shortPath = filterRestrict(shortPath, input.getRouteRestriction(), param, opMode);

//        if(shortPath.isEmpty()){
//            throw new NeDesignerException("No available route");
//        }

        //comment: because recommend to use mandatory option  to produce the required path.
     /*   if (param.getIsProtected()) {
            List<ShortPath> newShotPath = new ArrayList<>(shortPath);
            for (ShortPath s : newShotPath) {
                shortPath.add(reversePrimarySecondary(s));
            }
        }*/
//        shortPath = removeDuplicateWithReuseRoute(shortPath, reusedRoutes);

        //sort by route size
        shortPath.sort(Comparator.comparing(s -> s.getSiteLinkRoute().getPrimary().size()));
        if (shortPath.size() > COMPUTED_ROUTE_LIMIT) {
            shortPath = shortPath.subList(0, COMPUTED_ROUTE_LIMIT);
        }
        try {
            shortPath = addFriendlyNameAndReg(shortPath);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "compute shortPath error:" + e.toString(), e);
        }
        return shortPath;
    }

    static boolean validateAndShouldUseMandatoryAStar(
            MandatoryRouteConstraints constraints, Boolean mandatoryResourcesOrdered) {
        boolean hasMandatoryResources = constraints.hasMandatoryResources();
        if (hasMandatoryResources && Boolean.TRUE.equals(mandatoryResourcesOrdered)) {
            // Ordered mandatory resources are deliberately rejected until an ordered-state
            // search is defined; silently treating them as unordered would change semantics.
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Ordered mandatory-node and mandatory-site-link are not supported");
        }
        return hasMandatoryResources;
    }

    /**
     * scenario: no sitelink available, only reuse och
     *
     * @param reusedRouteList
     * @return
     */
    List<ShortPath> convertToShortPath(List<ReusedRoute> reusedRouteList) {
        if (reusedRouteList == null || reusedRouteList.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        List<ShortPath> shortPathReused = new ArrayList<>();

        for (ReusedRoute reusedRoute : reusedRouteList) {
            Integer reusedRouteBundle = reusedRoute.getSiteLinkRoute().getBundleNumber();
            if (reusedRouteBundle == 0) {
                ShortPath shortPath = new ShortPathBuilder().setSiteLinkRoute(reusedRoute.getSiteLinkRoute()).build();
                shortPathReused.add(shortPath);
            }
        }

        return shortPathReused;
    }

    private List<ShortPath> addFriendlyNameAndReg(List<ShortPath> shortPath) throws NeDesignerException {
        List<ShortPath> result = new ArrayList<>();
        for (ShortPath path : shortPath) {
            List<String> linkIds = path.getSiteLinkRoute().getPrimary();
            List<PrimaryFriendly> linkFriendlyList = new ArrayList<>();
            for (String linkId : linkIds) {
                //只显示sitelink给客户看
                if (!linkId.startsWith("SiteLink")) {
                    continue;
                }
                String linkFriendlyName = siteLinkDao.getFriendlyName(linkId);
                PrimaryFriendly primaryFriendly = new PrimaryFriendlyBuilder().setName(linkFriendlyName).setLinkId(linkId).build();
                linkFriendlyList.add(primaryFriendly);
            }
            SiteLinkRoute siteLinkRouteFri = new SiteLinkRouteBuilder(path.getSiteLinkRoute()).setPrimaryFriendly(linkFriendlyList)
                    .setPrimaryReg(getRegs(linkIds))
                    .build();
            ShortPath newPath = new ShortPathBuilder(path).setSiteLinkRoute(siteLinkRouteFri).build();
            result.add(newPath);
        }
        return result;
    }

    private List<PrimaryReg> getRegs(List<String> linkIds) throws NeDesignerException {
        List<PrimaryReg> result = new ArrayList<>();

        List<String> siteLinkIds = linkIds.stream().filter(linkId -> SiteLinkIdNamingRule.isSiteLink(linkId)).collect(Collectors.toList());
        int size = siteLinkIds.size();
        if (size < 2) {
            return result;
        }

        for (int i = 0; i < size - 1; i++) {
            String firstLinkId = siteLinkIds.get(i);
            String secondLinkId = siteLinkIds.get(i + 1);
            String siteId;
            try {
                siteId = tunnelUtils.getRegSite(firstLinkId, secondLinkId);
            } catch (NeDesignerException e) {
                log.debug("Invalid path for REG between :{} and {},drop", firstLinkId, secondLinkId, e);
                continue;
            }
            String siteFriendlyName = siteNodeDao.getSiteFriendlyName(siteId);
            result.add(new PrimaryRegBuilder().setSiteId(siteId).setSiteName(siteFriendlyName).build());

        }
        return result;
    }


    List<ShortPath> filterRestrict(List<ShortPath> shortPath, List<RouteRestriction> routeRestrictions, ParamCompute2 param, String opMode) {
        MandatoryRouteConstraints constraints = MandatoryRouteConstraints.from(routeRestrictions);
        List<ShortPath> filteredPaths = new ArrayList<>();
        for (ShortPath path : shortPath) {
            if (!isValidBundleNumber(path, param, opMode)) {
                continue;
            }
            if (!constraints.hasMandatoryResources()) {
                filteredPaths.add(path);
                continue;
            }
            SiteLinkRoute route = path.getSiteLinkRoute();
            if (matchesMandatoryResources(route.getPrimary(), constraints.get(0))
                    && matchesMandatoryResources(route.getSecondary(), constraints.get(1))
                    && matchesMandatoryResources(route.getThird(), constraints.get(2))) {
                filteredPaths.add(path);
            }
        }
        return filteredPaths;
    }

    private boolean matchesMandatoryResources(List<String> route,
                                               MandatoryRouteConstraints.LegConstraint constraint) {
        if (!constraint.hasMandatoryResources()) {
            return true;
        }
        if (route == null || route.isEmpty()) {
            return false;
        }
        Set<String> routeResources = new HashSet<>(route);
        if (!routeResources.containsAll(constraint.getSiteLinkIds())) {
            return false;
        }
        // Reuse the existing SiteLink/physical-link membership rule so final A* and
        // reused-OCH validation use the same topology semantics.
        return constraint.getSiteIds().stream()
                .allMatch(siteId -> passMandatorySite(siteId, route));
    }

    // Reused OCH candidates are validated by leg because supporting-link is a union
    // and cannot distinguish primary, secondary and third route requirements.
    boolean matchesMandatoryResources(
            SiteLinkRoute route, MandatoryRouteConstraints constraints) {
        return matchesMandatoryResources(route.getPrimary(), constraints.get(0))
                && matchesMandatoryResources(route.getSecondary(), constraints.get(1))
                && matchesMandatoryResources(route.getThird(), constraints.get(2));
    }

    private boolean isValidBundleNumber(ShortPath path, ParamCompute2 param, String opMode) {

        if (!param.getIsReusedOch()) {
            if (path.getSiteLinkRoute().getBundleNumber() < param.bundleNumber) {
                return false;
            }
            return true;
        }
        String vendorName = path.getSiteLinkRoute().getVendorName();
        String productType = path.getSiteLinkRoute().getProductType();
        List<Link> reusedOch = tunnelUtilService.getReusedOchList_IgnoreReg(path.getSiteLinkRoute(), param, opMode);
        int ochBundle = reusedOch.stream()
                .mapToInt(reuseOchLink -> tunnelUtilService.getMatchedOchReusedOdu(reuseOchLink, param, vendorName, productType))
                .sum();
        int pathTotalBundle = ochBundle + path.getSiteLinkRoute().getBundleNumber();
        if (pathTotalBundle < param.bundleNumber) {
            return false;
        }
        return true;
    }


    private List<ShortPath> removeDuplicateWithReuseRoute(List<ShortPath> shortPaths, List<ReusedRoute> reusedRoutes) {
        if (reusedRoutes == null || reusedRoutes.isEmpty()) {
            return shortPaths;
        }

        List<ShortPath> result = new ArrayList<>();
        for (ShortPath shortPath : shortPaths) {
            SiteLinkRoute shortPathSiteLinkRoute = shortPath.getSiteLinkRoute();
            for (ReusedRoute reusedRoute : reusedRoutes) {
                SiteLinkRoute reusedSiteLinkRoute = reusedRoute.getSiteLinkRoute();
                if (!shortPathSiteLinkRoute.getVendorName().equals(reusedSiteLinkRoute.getVendorName())) {
                    result.add(shortPath);
                    continue;
                }
                if (!shortPathSiteLinkRoute.getCentralFrequencies().equals(reusedSiteLinkRoute.getCentralFrequencies())) {
                    result.add(shortPath);
                    continue;
                }
                if (!shortPathSiteLinkRoute.getPrimary().equals(reusedSiteLinkRoute.getPrimary())) {
                    result.add(shortPath);
                    continue;
                }
                if (shortPathSiteLinkRoute.getSecondary() != null) {
                    if (reusedSiteLinkRoute.getSecondary() == null) {
                        result.add(shortPath);
                        continue;
                    } else {
                        if (!reusedSiteLinkRoute.getSecondary().equals(shortPathSiteLinkRoute.getSecondary())) {
                            result.add(shortPath);
                            continue;
                        }
                    }
                } else if (reusedSiteLinkRoute.getSecondary() != null) {
                    result.add(shortPath);
                    continue;
                }

            }
        }
        return result;
    }

    Pair<List<ReuesedOch>, List<ReusedRoute>> getReUsedOchScenario(ParamCompute2 param, String opMode) {
        if (!param.getIsReusedOch()) {
            return Pair.of(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        }
        //为了得到reused och route，所以这里是不过滤vendor，cardtype，L口速率之类的，那个留着getMatchedOchReusedOdu去做

        List<Link> matchedOchLinks = ochLinkDao.filter(param.getSrcSite(), param.getDesSite(), param.getMandatorySiteLinks(), param.getProtectionType(), param.getRiskGroupName(), param
                .getPlaneName(), param.getOchOduGranularity(), param.getCardType(), param.getServiceType(), opMode);
        //get reused och route
        List<ReusedRoute> reusedRoutes = getReUsedOchRoute(param, matchedOchLinks);

        //get reused och link
        List<ReuesedOch> reUsedOchs = getReUsedOch(param, matchedOchLinks);

        return Pair.of(reUsedOchs, reusedRoutes);
    }

    private List<ReuesedOch> getReUsedOch(ParamCompute2 param, List<Link> matchedOchLinks) {
        List<ReuesedOch> reUsedOchs = new ArrayList<>();
        for (VendorOccupationRate occupationRate : param.getVendorOccupationRateList()) {
            String vendorName = occupationRate.getVendorName();
            String productType = occupationRate.getProductType();

            List<Links> linksList = new ArrayList<>();
            for (Link ochLink : matchedOchLinks) {
                int bundleNumber = tunnelUtilService.getMatchedOchReusedOdu(ochLink, param, vendorName, productType);

                if (bundleNumber > 0) {
                    //add reusedOch
                    Links links = new LinksBuilder().setLinkId(ochLink.getLinkId().getValue()).setBundleNumber(bundleNumber).build();
                    linksList.add(links);
                }
            }
            ReuesedOch ReuesedOch = new ReuesedOchBuilder()
                    .setLinks(linksList)
                    .setVendorName(vendorName)
                    .setProductType(productType)
                    .build();
            reUsedOchs.add(ReuesedOch);
        }
        return reUsedOchs;
    }

    private List<ReusedRoute> getReUsedOchRoute(ParamCompute2 param, List<Link> matchedOchLinks) {
        Set<ReusedRouteTemp> reUsedOchRouteTemps = new HashSet<>();
        for (Link ochLink : matchedOchLinks) {
            try {
                //generate reusedOchRoute
                Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
                Route ochRoute = ochLinkAttr.getExplictRoute().getRoute().get(0);
                ReusedRouteTemp reusedRouteTmp = buildReusedRouteTemp(ochRoute, ochLinkAttr.getVendorName(), ochLinkAttr.getProductType());
                if (ochLink.getDestination().getDestNode().getValue().contains(param.getSrcSite().getNodeId().getValue())) {
                    reusedRouteTmp.reverse();
                }
                if (!Collections.disjoint(param.getExcludeLinks(), reusedRouteTmp.primary)) {
                    continue;
                }
                if (reusedRouteTmp.secondary != null && !Collections.disjoint(param.getExcludeLinks(), reusedRouteTmp.secondary)) {
                    continue;
                }
                reUsedOchRouteTemps.add(reusedRouteTmp);
            } catch (Exception e) {
                log.error("Failed to get reused och route by och link:{}", ochLink.getLinkId().getValue(), e);
                continue;
            }
        }

        List<ReusedRoute> reusedRoutes = new ArrayList<>();
        Iterator<ReusedRouteTemp> iterator = reUsedOchRouteTemps.iterator();
        while (iterator.hasNext()) {
            ReusedRouteTemp reusedRouteTemp = iterator.next();
            ReusedRoute reusedRoute = null;
            try {
                reusedRoute = buildReusedRoute(reusedRouteTemp, param.getClientLineRate());
            } catch (NeDesignerException e) {
                log.error("Failed to build reused route by:{}", reusedRouteTemp, e);
                continue;
            }
            if (reusedRoute != null) {
                reusedRoutes.add(reusedRoute);
            }
        }
        return reusedRoutes;
    }

    private boolean passMandatorySite(String mandatorySiteId, List<String> primaryRoute) {
        if (mandatorySiteId == null) {
            return true;
        }
        for (String linkId : primaryRoute) {
            //siteLink
            if (linkId.startsWith(SITE_LINK_PREFIX)) {
                boolean passSite = siteLinkDao.passSite(linkId, mandatorySiteId);
                if (passSite) {
                    return true;
                }
                continue;
            }
            //wssLink
            if (linkId.contains(mandatorySiteId)) {
                return true;
            }

        }
        return false;
    }


    private ReusedRoute buildReusedRoute(ReusedRouteTemp reusedRouteTemp, int clientRateNumber) throws NeDesignerException {
        List<Long> freqs = getJointFrequency(reusedRouteTemp.primary, reusedRouteTemp.fixGrid);

        if (freqs == null || freqs.isEmpty()) {
//            return null;//no reused route, because no joint frequency
            freqs = new ArrayList<>();
        }
        if (reusedRouteTemp.secondary != null && !reusedRouteTemp.secondary.isEmpty()) {
            List<Long> secondFreq = getJointFrequency(reusedRouteTemp.secondary, reusedRouteTemp.fixGrid);
            freqs.retainAll(secondFreq);
//            if (freqs.isEmpty()) {
//                return null;
//            }
        }
        int bundleNum = freqs.size() * clientRateNumber;
        SiteLinkRoute tunnelRoute = new SiteLinkRouteBuilder()
                .setBundleNumber(bundleNum)
                .setCentralFrequencies(freqs)
                .setFixGrid(reusedRouteTemp.fixGrid)
                .setPrimary(reusedRouteTemp.primary)
                .setSecondary(reusedRouteTemp.secondary)
                .setVendorName(reusedRouteTemp.vendorName)
                .setProductType(reusedRouteTemp.productType)
                .build();

        return new ReusedRouteBuilder()
                .setSiteLinkRoute(tunnelRoute)
                .build();

    }

    private List<Long> getJointFrequency(ArrayList<String> LinkList, String fixGrid) throws NeDesignerException {
        GridType fixGridType = tunnelUtils.getGridByString(fixGrid);

        List<Long> commonFreqs = null;
        for (String linkId : LinkList) {
            if (linkId.startsWith(WSS_LINK_PREFIX)) {
                continue;
            }
            Link siteLink = siteLinkDao.getSiteLinkById(linkId);
            List<Long> centralFrequencies = new FrequencyAvailable(siteLink).getAllPossibleCentFrequency(fixGridType);
            if (commonFreqs == null) {
                commonFreqs = new ArrayList<>(centralFrequencies);
            } else {
                commonFreqs.retainAll(centralFrequencies);
            }
            if (commonFreqs.isEmpty()) {
                return null;
            }
        }
        return commonFreqs;
    }

    private ReusedRouteTemp buildReusedRouteTemp(Route ochRoute, String vendorName, String productType) throws NeDesignerException {
        List<ExplicitRouteObjects> primaryExplicitRouteObjects = ochRoute.getPrimary().getExplicitRouteObjects();
        ArrayList<String> ochPrimary = getSiteLinkRoute(primaryExplicitRouteObjects);//include siteLinkId and wssLinkId
        GridType fixGrid = getFixGridForRoute(ochPrimary);
        ArrayList<String> ochSecondary = null;
        if (ochRoute.getSecondary() != null && ochRoute.getSecondary().getExplicitRouteObjects() != null) {
            ochSecondary = getSiteLinkRoute(ochRoute.getSecondary().getExplicitRouteObjects());
        }

        ArrayList<String> ochThird = null;
        if (ochRoute.getThird() != null && !ochRoute.getThird().isEmpty() && ochRoute.getThird().get(0).getExplicitRouteObjects() != null) {
            ochThird = getSiteLinkRoute(ochRoute.getThird().get(0).getExplicitRouteObjects());
        }

        return new ReusedRouteTemp(ochPrimary, ochSecondary, ochThird, vendorName, productType, tunnelUtils.getGridString(fixGrid));
    }

    /**
     * 同一条route里面，有一下几种组合： 1， 全是flex， 则fixGrid返回grid75 2. flex+mux96（grid50），则fixGrid返回grid50 3. flex+mux64（grid75），则fixGrid返回grid75 其他的就是非法的。
     *
     * @param route
     * @return
     */
    private GridType getFixGridForRoute(ArrayList<String> route) throws NeDesignerException {
        ArrayList<String> siteLinkIds = getSiteLinkListFromRoute(route);
        Set<GridType> gridSet = getGridTypesForSiteLinks(siteLinkIds);

        if (gridSet.size() > 2) {
            log.error("Invalid route, because this route have more grids as: {}, route is :{}", gridSet, route);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Invalid route, because has more than 2 different Grid.");
        }

        Iterator<GridType> gridIterator = gridSet.iterator();
        GridType grid = gridIterator.next();

        if (gridSet.size() == 1) {
            if (grid.equals(GridType._0)) {
                return GridType._75;
            }
            return grid;
        }

        if (!grid.equals(GridType._0)) {
            return grid;
        }
        return gridIterator.next();
    }

    private Set<GridType> getGridTypesForSiteLinks(ArrayList<String> siteLinkIds) throws NeDesignerException {
        Set<String> gridSet = siteLinkDao.getGrids(siteLinkIds);
        if (gridSet == null || gridSet.isEmpty()) {
            log.error("Got empty grid from siteLinks:{}", siteLinkIds);
            throw new NeDesignerException("Got empty grid from siteLinks");
        }
        Set<GridType> result = new HashSet<>();
        for (String gridString : gridSet) {
            try {
                result.add(tunnelUtils.getGridByString(gridString));
            } catch (Exception e) {
                log.error("Failed to get grid type for grid string :{} from siteLinks:{}", gridString, siteLinkIds, e);
                throw new NeDesignerException("Failed to get grid type from gridString:" + gridString);
            }

        }

        return result;
    }

    /**
     * route include sitelink id and wsslink id as this way: s,w,s,w,s
     *
     * @param route
     * @return
     */
    private ArrayList<String> getSiteLinkListFromRoute(ArrayList<String> route) {
        ArrayList<String> result = new ArrayList<>();
        int size = route.size();
        for (int i = 0; i < size; i += 2) {
            result.add(route.get(i));
        }
        return result;
    }

    private ArrayList<String> getSiteLinkRoute(List<ExplicitRouteObjects> primaryExplicitRouteObjects) {
        ArrayList<String> ochPrimary = primaryExplicitRouteObjects.get(0).getPathRouteObject().stream()
                .filter(item -> isSiteLink(item) || isWssLink(item))
                .map(siteLink -> ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) siteLink.getResourceType())
                        .getLinkHop()
                        .getLinkRef()
                        .getValue()).collect(
                        Collectors.toCollection(ArrayList::new));
        return ochPrimary;
    }

    private boolean isSiteLink(PathRouteObject item) {
        return item.getTopologyRef().getValue().equals(TopoNameConstants.Site_Topo_Key) &&
                item.getResourceType().getImplementedInterface().getName()
                        .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class.getName());
    }

    private boolean isWssLink(PathRouteObject item) {
        return item.getTopologyRef().getValue().equals(TopoNameConstants.Phy_Topo_Key) &&
                item.getResourceType().getImplementedInterface().getName()
                        .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class.getName())
                && ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) item.getResourceType()).getLinkHop()
                .getLinkRef().getValue().startsWith(WSS_LINK_PREFIX);
    }

    /**
     * Computes routes with unordered mandatory resources by A*. Database filtering,
     * SiteLink eligibility, WSS adjacency and frequency calculation remain shared with
     * the original route implementation.
     */
    private List<ShortPath> getShortPathWithMandatoryResources(
            ParamCompute2 param, WDM_Band wdmBand, MandatoryRouteConstraints constraints)
            throws NeDesignerException {
        String srcSiteId = param.getSrcSite().getNodeId().getValue();
        String destSiteId = param.getDesSite().getNodeId().getValue();
        Map<String, List<Link>> gridSiteLinksMap = getGridSitelinkMapFromDB(
                param.getRiskGroupName(), param.getPlaneName(), param.getPlaneId(),
                param.getMatchedGrids(), param.getExcludeLinks(), wdmBand);
        List<ShortPath> output = new ArrayList<>();

        for (Entry<String, List<Link>> entry : gridSiteLinksMap.entrySet()) {
            List<Link> allSiteLinks = entry.getValue();
            if (allSiteLinks == null || allSiteLinks.isEmpty()) {
                continue;
            }
            String fixGrid = param.getFrequencyWidthOfLinePort() != null
                    ? String.valueOf(param.getFrequencyWidthOfLinePort().getIntValue())
                    : entry.getKey();
            List<Link> srcSiteLinks = allSiteLinks.stream()
                    .filter(item -> inSite(item, srcSiteId) && isOtm(item, srcSiteId))
                    .collect(Collectors.toList());
            List<Link> destSiteLinks = allSiteLinks.stream()
                    .filter(item -> inSite(item, destSiteId) && isOtm(item, destSiteId))
                    .collect(Collectors.toList());
            if (srcSiteLinks.isEmpty() || destSiteLinks.isEmpty()) {
                continue;
            }

            // Direct SiteLinks are retained in endpoint lists but omitted from the WSS graph,
            // matching the original getShortPath implementation.
            List<Link> graphSiteLinks = allSiteLinks.stream()
                    .filter(item -> !(inSite(item, destSiteId) && inSite(item, srcSiteId)))
                    .collect(Collectors.toList());
            List<ShortPath> primaryPaths = getAStarPaths(
                    fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks,
                    graphSiteLinks, param.getClientLineRate(), param.getCardType(), wdmBand,
                    constraints.get(0), Collections.<String>emptySet(),
                    Collections.<String>emptySet());
            if (param.getProtectionType().equals(ProtectionUnprotected.class)) {
                output.addAll(primaryPaths);
                continue;
            }

            for (ShortPath primary : primaryPaths) {
                Set<String> usedSiteLinks = getRouteSiteLinkIds(
                        primary.getSiteLinkRoute().getPrimary());
                Set<String> usedSites = getRouteIntermediateSites(
                        primary.getSiteLinkRoute().getPrimary(), srcSiteId, destSiteId);
                List<ShortPath> secondaryPaths = getAStarPaths(
                        fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks,
                        graphSiteLinks, param.getClientLineRate(), param.getCardType(), wdmBand,
                        constraints.get(1), usedSiteLinks, usedSites);

                for (ShortPath secondary : secondaryPaths) {
                    ShortPath protectedPair = buildProtectedShortPath(
                            primary, secondary, param.getClientLineRate(), true,
                            srcSiteId, destSiteId);
                    if (protectedPair == null) {
                        continue;
                    }
                    if (param.getProtectionType().equals(ProtectionBidir1To1.class)) {
                        output.add(protectedPair);
                        continue;
                    }

                    Set<String> thirdExcludedLinks = new HashSet<>(usedSiteLinks);
                    thirdExcludedLinks.addAll(getRouteSiteLinkIds(
                            secondary.getSiteLinkRoute().getPrimary()));
                    Set<String> thirdExcludedSites = new HashSet<>(usedSites);
                    thirdExcludedSites.addAll(getRouteIntermediateSites(
                            secondary.getSiteLinkRoute().getPrimary(), srcSiteId, destSiteId));
                    List<ShortPath> thirdPaths = getAStarPaths(
                            fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks,
                            graphSiteLinks, param.getClientLineRate(), param.getCardType(), wdmBand,
                            constraints.get(2), thirdExcludedLinks, thirdExcludedSites);
                    for (ShortPath third : thirdPaths) {
                        ShortPath primaryThird = buildProtectedShortPath(
                                primary, third, param.getClientLineRate(), true,
                                srcSiteId, destSiteId);
                        ShortPath secondaryThird = buildProtectedShortPath(
                                secondary, third, param.getClientLineRate(), true,
                                srcSiteId, destSiteId);
                        if (primaryThird == null || secondaryThird == null) {
                            continue;
                        }
                        ShortPath protectedTriple = buildProtectedAStarPath(
                                primary, secondary, third);
                        if (protectedTriple != null) {
                            output.add(protectedTriple);
                        }
                    }
                }
            }
        }
        return output;
    }

    private List<ShortPath> getAStarPaths(
            String fixGrid, String srcSiteId, String destSiteId,
            List<Link> srcSiteLinks, List<Link> destSiteLinks, List<Link> graphSiteLinks,
            int clientLineRateNumber, String cardType, WDM_Band wdmBand,
            MandatoryRouteConstraints.LegConstraint constraint,
            Set<String> excludedSiteLinks, Set<String> excludedSites)
            throws NeDesignerException {
        GridType fixGridType = tunnelUtils.getGridByString(fixGrid);
        Map<String, AStarPathGroup> pathsBySiteLinks = new HashMap<>();
        Map<String, Set<String>> mandatorySiteLinkCoverage =
                getMandatorySiteLinkCoverage(constraint.getSiteIds(),
                        srcSiteLinks, destSiteLinks, graphSiteLinks);
        Set<String> effectiveExcludedSiteLinks = new HashSet<>(excludedSiteLinks);
        effectiveExcludedSiteLinks.addAll(getSiteLinksCoveringSites(
                excludedSites, srcSiteLinks, destSiteLinks, graphSiteLinks));

        for (Link srcSiteLink : srcSiteLinks) {
            String srcSiteLinkId = srcSiteLink.getLinkId().getValue();
            if (effectiveExcludedSiteLinks.contains(srcSiteLinkId)) {
                continue;
            }
            String srcVendorName = srcSiteLink.getAugmentation(Link1.class)
                    .getSite().getVendorName();
            DJNode srcNode = buildNode(srcSiteLink, fixGridType, cardType, wdmBand);
            for (Link destSiteLink : destSiteLinks) {
                String destSiteLinkId = destSiteLink.getLinkId().getValue();
                if (effectiveExcludedSiteLinks.contains(destSiteLinkId)
                        || (!srcSiteLinkId.equals(destSiteLinkId)
                        && destSiteLinkId.contains(srcSiteId))) {
                    continue;
                }
                String destVendorName = destSiteLink.getAugmentation(Link1.class)
                        .getSite().getVendorName();
                if (!srcVendorName.equals(destVendorName)) {
                    continue;
                }
                DJNode destNode = srcSiteLinkId.equals(destSiteLinkId)
                        ? srcNode : buildNode(destSiteLink, fixGridType, cardType, wdmBand);
                List<Long> commonFrequencies = new ArrayList<>(
                        srcNode.getAvailableCentFrequency());
                commonFrequencies.retainAll(destNode.getAvailableCentFrequency());
                if (commonFrequencies.isEmpty()) {
                    continue;
                }

                Graph graph = srcSiteLinkId.equals(destSiteLinkId)
                        ? new Graph()
                        : buildGraph(srcSiteId, destSiteId, graphSiteLinks,
                        new HashSet<>(commonFrequencies), fixGridType, cardType, wdmBand);
                DJNode graphSource = srcSiteLinkId.equals(destSiteLinkId)
                        ? srcNode : graph.getNodeByLinkId(srcSiteLinkId);
                DJNode graphDestination = srcSiteLinkId.equals(destSiteLinkId)
                        ? destNode : graph.getNodeByLinkId(destSiteLinkId);
                if (graphSource == null || graphDestination == null) {
                    continue;
                }

                for (Long frequency : commonFrequencies) {
                    List<DJNode> nodes = MandatoryRouteAStar.findPath(
                            graph, graphSource, graphDestination, srcSiteId, destSiteId,
                            frequency, constraint.getSiteIds(), constraint.getSiteLinkIds(),
                            mandatorySiteLinkCoverage,
                            excludedSites, effectiveExcludedSiteLinks);
                    if (nodes.isEmpty() || !isValidPath(nodes, srcSiteId, destSiteId)) {
                        continue;
                    }
                    String signature = nodes.stream().map(DJNode::getSiteLinkId)
                            .collect(Collectors.joining("\u0000"));
                    AStarPathGroup group = pathsBySiteLinks.get(signature);
                    if (group == null) {
                        group = new AStarPathGroup(nodes, srcVendorName);
                        pathsBySiteLinks.put(signature, group);
                    }
                    group.frequencies.add(frequency);
                }
            }
        }

        List<ShortPath> result = new ArrayList<>();
        for (AStarPathGroup group : pathsBySiteLinks.values()) {
            Collections.sort(group.frequencies);
            ShortPath path = buildAStarShortPath(fixGrid, group.nodes,
                    group.frequencies, clientLineRateNumber, group.vendorName);
            if (path != null) {
                result.add(path);
            }
        }
        return result;
    }

    private Map<String, Set<String>> getMandatorySiteLinkCoverage(
            Set<String> mandatorySiteIds, List<Link> srcSiteLinks,
            List<Link> destSiteLinks, List<Link> graphSiteLinks) {
        Map<String, Link> candidateLinks = getCandidateSiteLinks(
                srcSiteLinks, destSiteLinks, graphSiteLinks);
        Map<String, Set<String>> result = new HashMap<>();
        for (String siteId : mandatorySiteIds) {
            result.put(siteId, getSiteLinksCoveringSite(siteId, candidateLinks));
        }
        return result;
    }

    private Set<String> getSiteLinksCoveringSites(
            Set<String> siteIds, List<Link> srcSiteLinks,
            List<Link> destSiteLinks, List<Link> graphSiteLinks) {
        Map<String, Link> candidateLinks = getCandidateSiteLinks(
                srcSiteLinks, destSiteLinks, graphSiteLinks);
        Set<String> result = new HashSet<>();
        for (String siteId : siteIds) {
            result.addAll(getSiteLinksCoveringSite(siteId, candidateLinks));
        }
        return result;
    }

    private Map<String, Link> getCandidateSiteLinks(
            List<Link> srcSiteLinks, List<Link> destSiteLinks,
            List<Link> graphSiteLinks) {
        Map<String, Link> candidateLinks = new HashMap<>();
        for (Link link : graphSiteLinks) {
            candidateLinks.put(link.getLinkId().getValue(), link);
        }
        for (Link link : srcSiteLinks) {
            candidateLinks.put(link.getLinkId().getValue(), link);
        }
        for (Link link : destSiteLinks) {
            candidateLinks.put(link.getLinkId().getValue(), link);
        }
        return candidateLinks;
    }

    private Set<String> getSiteLinksCoveringSite(
            String siteId, Map<String, Link> candidateLinks) {
        // SiteLinkDao.passSite applies the same supporting-link containment rule in
        // Mongo. Use the already loaded Link objects here to avoid one query per pair.
        return candidateLinks.values().stream()
                .filter(link -> link.getSupportingLink() != null
                        && link.getSupportingLink().stream()
                        .anyMatch(supportingLink -> supportingLink.getLinkRef()
                                .getValue().contains(siteId)))
                .map(link -> link.getLinkId().getValue())
                .collect(Collectors.toSet());
    }

    private ShortPath buildAStarShortPath(String fixGrid, List<DJNode> nodes,
                                           List<Long> frequencies, int clientLineRateNumber,
                                           String vendorName) {
        ShortPath basePath = buildShortPath(
                fixGrid, nodes, clientLineRateNumber, vendorName);
        if (basePath == null || frequencies.isEmpty()) {
            return null;
        }
        int bundleNumber = getBundleNum(
                clientLineRateNumber, frequencies.size(), getMinBandwidth(nodes));
        SiteLinkRoute route = new SiteLinkRouteBuilder(basePath.getSiteLinkRoute())
                .setCentralFrequencies(new ArrayList<>(frequencies))
                .setBundleNumber(bundleNumber)
                .build();
        return new ShortPathBuilder().setSiteLinkRoute(route).build();
    }

    /**
     * Combines three already-disjoint A* legs while retaining only frequencies and
     * bundle capacity available on every leg.
     */
    ShortPath buildProtectedAStarPath(
            ShortPath primary, ShortPath secondary, ShortPath third) {
        List<Long> commonFrequencies = new ArrayList<>(
                primary.getSiteLinkRoute().getCentralFrequencies());
        commonFrequencies.retainAll(secondary.getSiteLinkRoute().getCentralFrequencies());
        commonFrequencies.retainAll(third.getSiteLinkRoute().getCentralFrequencies());
        if (commonFrequencies.isEmpty()) {
            return null;
        }
        int bundleNumber = Math.min(primary.getSiteLinkRoute().getBundleNumber(),
                Math.min(secondary.getSiteLinkRoute().getBundleNumber(),
                        third.getSiteLinkRoute().getBundleNumber()));
        SiteLinkRoute route = new SiteLinkRouteBuilder(primary.getSiteLinkRoute())
                .setBundleNumber(bundleNumber)
                .setCentralFrequencies(commonFrequencies)
                .setSecondary(secondary.getSiteLinkRoute().getPrimary())
                .setThird(third.getSiteLinkRoute().getPrimary())
                .build();
        return new ShortPathBuilder().setSiteLinkRoute(route).build();
    }

    private Set<String> getRouteSiteLinkIds(List<String> route) {
        return route.stream().filter(SiteLinkIdNamingRule::isSiteLink)
                .collect(Collectors.toSet());
    }

    private Set<String> getRouteIntermediateSites(
            List<String> route, String srcSiteId, String destSiteId) {
        return route.stream()
                .flatMap(resourceId -> getSites(resourceId, srcSiteId, destSiteId).stream())
                .collect(Collectors.toSet());
    }


    protected List<ShortPath> getShortPath(ParamCompute2 param, WDM_Band wdmBand) throws NeDesignerException {
        String riskGroupName = param.getRiskGroupName();
        String planeId = param.getPlaneId();
        String planeName = param.getPlaneName();
        String srcSiteId = param.getSrcSite().getNodeId().getValue();
        String destSiteId = param.getDesSite().getNodeId().getValue();
        List<String> matchedGrids = param.getMatchedGrids();
        Set<String> excludeLinks = param.getExcludeLinks();

        List<ShortPath> output = new ArrayList<>();

        Map<String, List<Link>> gridSiteLinksMap = getGridSitelinkMapFromDB(riskGroupName, planeName, planeId, matchedGrids, param.getExcludeLinks(), wdmBand);
        for (Entry<String, List<Link>> entry : gridSiteLinksMap.entrySet()) {
//            List<Link> siteLinks = entry.getValue().stream().filter(link -> !excludeLinks.contains(link.getLinkId().getValue())).collect(Collectors.toList());
            List<Link> siteLinks = entry.getValue();
            if (siteLinks.isEmpty()) {
                continue;
            }
            String fixGrid = param.getFrequencyWidthOfLinePort() != null ? String.valueOf(param.getFrequencyWidthOfLinePort().getIntValue()) : entry.getKey();
            List<Link> srcSiteLinks = siteLinks.stream().filter(item -> inSite(item, srcSiteId) && isOtm(item, srcSiteId)).collect(Collectors.toList());
            if (srcSiteLinks == null || srcSiteLinks.isEmpty()) {
                log.debug("No source siteLink found from site:{}, at Grid:{}, riskGroupName:{},planeId:{}", srcSiteId, fixGrid, riskGroupName, planeId);
                continue;
            }
//            srcSiteLinks = handleIraSiteLink(srcSiteLinks);

            List<Link> destSiteLinks = siteLinks.stream().filter(item -> inSite(item, destSiteId) && isOtm(item, destSiteId)).collect(Collectors.toList());

            if (destSiteLinks == null || destSiteLinks.isEmpty()) {
                log.debug("No dest siteLink found to site:{}, at Grid:{}, riskGroupName:{},planeId:{}", destSiteId, fixGrid, riskGroupName, planeId);
                continue;
            }
//            destSiteLinks = handleIraSiteLink(destSiteLinks);

            String mandatorySiteLinkId = param.getMandatorySiteLinkId();

            siteLinks = siteLinks.stream()
                    .filter(item -> !(inSite(item, destSiteId) && inSite(item, srcSiteId)))
                    .collect(Collectors.toList());//过滤掉路径就是一条复用段的。这个srcSiteLinks会cover

            if (param.getProtectionType().equals(ProtectionUnprotected.class)) {
                List<ShortPath> shortPathNonProtected;
                if (mandatorySiteLinkId == null) {
                    shortPathNonProtected = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param.getClientLineRate(), param
                            .getCardType(), wdmBand);
                } else {
                    shortPathNonProtected = getShortPathsNonProtectd_WithMandatory(param, srcSiteId, destSiteId, siteLinks, fixGrid, srcSiteLinks, destSiteLinks, mandatorySiteLinkId, wdmBand);
                    if (shortPathNonProtected == null || shortPathNonProtected.isEmpty()) {
                        continue;
                    }
                }

                output.addAll(shortPathNonProtected);
            } else if (param.getProtectionType().equals(ProtectionBidir1To1.class)) {
                List<ShortPath> shortPathPrimary;
                boolean hasMandatorySiteLinkId = mandatorySiteLinkId != null ? true : false;
                if (!hasMandatorySiteLinkId) {
                    List<ShortPath> shortPathNonProtected = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param
                            .getClientLineRate(), param.getCardType(), wdmBand);
                    if (shortPathNonProtected == null || shortPathNonProtected.isEmpty()) {
                        continue;
                    }
                    //在所有最短路径中，配对primary+secondary,如果有满足的，就不重新calculate路径了
                    List<ShortPath> shortPaths = getShortPathProtected(shortPathNonProtected, param.getClientLineRate(), param.getSrcSite()
                                    .getNodeId()
                                    .getValue(),
                            param.getDesSite().getNodeId().getValue());
                    if (!shortPaths.isEmpty()) {
                        output.addAll(shortPaths);
                        continue;
                    }

                    shortPathPrimary = shortPathNonProtected;//不能配对出secondary，留着后面重新calculate secondary
                } else {
                    shortPathPrimary = getShortPathsNonProtectd_WithMandatory(param, srcSiteId, destSiteId, siteLinks, fixGrid, srcSiteLinks, destSiteLinks, mandatorySiteLinkId, wdmBand);
                    if (shortPathPrimary == null || shortPathPrimary.isEmpty()) {
                        continue;
                    }

                }

                //根据每一个primary最短路径，重新计算寻找最短路径
                for (ShortPath shortPath : shortPathPrimary) {
                    List<ShortPath> shortPathProtected = getShortPathProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param.getClientLineRate(), shortPath,
                            hasMandatorySiteLinkId, param.getSrcSite().getNodeId().getValue(), param.getDesSite()
                                    .getNodeId()
                                    .getValue(), param.getCardType(), wdmBand);
                    if (shortPathProtected != null) {
                        output.addAll(shortPathProtected);
                    }
                }
            } else {
                //1:2 protection
                List<ShortPath> shortPathPrimary;
                boolean hasMandatorySiteLinkId = mandatorySiteLinkId != null ? true : false;
                if (!hasMandatorySiteLinkId) {
                    List<ShortPath> shortPathNonProtected = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param
                            .getClientLineRate(), param.getCardType(), wdmBand);
                    if (shortPathNonProtected == null || shortPathNonProtected.isEmpty()) {
                        continue;
                    }
                    //在所有最短路径中，配对primary+secondary+third,如果有满足的，就不重新calculate路径了
                    List<ShortPath> shortPaths = getShortPathProtected2(shortPathNonProtected, param.getClientLineRate(), param.getSrcSite()
                                    .getNodeId()
                                    .getValue(),
                            param.getDesSite().getNodeId().getValue());
                    if (!shortPaths.isEmpty()) {
                        output.addAll(shortPaths);
                        continue;
                    }

                    shortPathPrimary = shortPathNonProtected;//不能配对出secondary，留着后面重新calculate secondary
                } else {
                    shortPathPrimary = getShortPathsNonProtectd_WithMandatory(param, srcSiteId, destSiteId, siteLinks, fixGrid, srcSiteLinks, destSiteLinks, mandatorySiteLinkId, wdmBand);
                    if (shortPathPrimary == null || shortPathPrimary.isEmpty()) {
                        continue;
                    }

                }

                //根据每一个primary最短路径，重新计算寻找最短路径
                for (ShortPath shortPath : shortPathPrimary) {
                    List<ShortPath> shortPathProtected = getShortPathProtected2(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param.getClientLineRate(), shortPath,
                            hasMandatorySiteLinkId, param.getSrcSite().getNodeId().getValue(), param.getDesSite()
                                    .getNodeId()
                                    .getValue(), param.getCardType());//todo p2p not need
                    if (shortPathProtected != null) {
                        output.addAll(shortPathProtected);
                    }
                }
            }
        }

        return output;
    }

    private List<Link> handleIraSiteLink(List<Link> srcSiteLinks) {

        return srcSiteLinks.stream()
                .filter(l -> l.getLinkId().getValue().endsWith("MUX")
                        || (l.getAugmentation(Link1.class).getSite().getAExternal() != null && !l.getAugmentation(Link1.class)
                        .getSite()
                        .getAExternal()
                        .getAddDropLink()
                        .isEmpty()))
                .collect(Collectors.toList());
    }

    private List<ShortPath> getShortPathsNonProtectd_WithMandatory(ParamCompute2 param, String srcSiteId, String destSiteId, List<Link> siteLinks, String fixGrid, List<Link> srcSiteLinks,
                                                                   List<Link> destSiteLinks,
                                                                   String mandatorySiteLinkId, WDM_Band wdmBand) throws NeDesignerException {
        List<ShortPath> shortPathNonProtected;
        Boolean isMandatorySiteLinkIdAsStartEnd = false;
        if (mandatorySiteLinkId != null) {
            if (mandatorySiteLinkId.contains(srcSiteId)) {
                srcSiteLinks = srcSiteLinks.stream().filter(item -> item.getLinkId().getValue().equals(mandatorySiteLinkId)).collect(Collectors.toList());
                isMandatorySiteLinkIdAsStartEnd = true;
            } else if (mandatorySiteLinkId.contains(destSiteId)) {
                destSiteLinks = destSiteLinks.stream().filter(item -> item.getLinkId().getValue().equals(mandatorySiteLinkId)).collect(Collectors.toList());
                isMandatorySiteLinkIdAsStartEnd = true;
            }
        }

        if (isMandatorySiteLinkIdAsStartEnd) {
            shortPathNonProtected = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param.getClientLineRate(), param
                    .getCardType(), wdmBand);
        } else {
            Optional<Link> mandatorySiteLinkOptional = siteLinks.stream().filter(item -> item.getLinkId().getValue().equals(mandatorySiteLinkId)).findAny();
            if (!mandatorySiteLinkOptional.isPresent()) {
                log.debug("Failed to find mandatorySiteLink:{} in grid group:{}, then no path in this grid group.", mandatorySiteLinkId, fixGrid);
                return null;
            }
            List<Link> mandatorySiteLinks = Arrays.asList(mandatorySiteLinkOptional.get());

            //note: 这里的srcSiteId和destSiteId仍然不变，不需要替换成mandatorySiteLink的src/dest，是因为性能考虑，不想算那么多次，关于siteId的相关处理，连接两个half的时候会处理
            List<ShortPath> shortPathFirstHalf = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, mandatorySiteLinks, siteLinks, param.getClientLineRate(), param
                    .getCardType(), wdmBand);
            if (shortPathFirstHalf.isEmpty()) {
                log.debug("No path found from src:{} to mandatorySiteLink:{} in grid group:{}", srcSiteId, mandatorySiteLinkId, fixGrid);
                return null;
            }
            List<ShortPath> shortPathLastHalf = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, mandatorySiteLinks, destSiteLinks, siteLinks, param.getClientLineRate(), param
                    .getCardType(), wdmBand);
            if (shortPathLastHalf.isEmpty()) {
                log.debug("No path found from mandatorySiteLink:{} to dest:{} in grid group:{}", mandatorySiteLinkId, destSiteId, fixGrid);
                return null;
            }
            shortPathNonProtected = connectTwoHalf(shortPathFirstHalf, shortPathLastHalf, param.getClientLineRate());

        }
        return shortPathNonProtected;
    }

    private List<ShortPath> connectTwoHalf(List<ShortPath> shortPathFirstHalf, List<ShortPath> shortPathLastHalf, Integer clientLineNumber) {
        List<ShortPath> shortPaths = new ArrayList<>();
        for (ShortPath fistHalf : shortPathFirstHalf) {
            SiteLinkRoute firstHalfSiteLinkRoute = fistHalf.getSiteLinkRoute();
            List<Long> fistHalfFreq = firstHalfSiteLinkRoute.getCentralFrequencies();
            List<String> firstPrimary = firstHalfSiteLinkRoute.getPrimary();
            String firstPrimaryLastWssLinkSiteId = PhysicalNodeIdNamingRule.getSiteId(PhysicalLinkIdNamingRule.getNodeAId(firstPrimary.get(firstPrimary.size() - 2)));

            for (ShortPath lastHalf : shortPathLastHalf) {
                //step1: 检查frequency是否有交集
                List<Long> commonFreqs = new ArrayList<>(fistHalfFreq);
                commonFreqs.retainAll(lastHalf.getSiteLinkRoute().getCentralFrequencies());
                if (commonFreqs.isEmpty()) {
                    continue;
                }
                //step2: 过滤掉half连接点上，连续两个wss link在同一个site的情况
                List<String> lastPrimary = lastHalf.getSiteLinkRoute().getPrimary();
                String lastPrimaryFirstWssLinkSiteId = PhysicalNodeIdNamingRule.getSiteId(PhysicalLinkIdNamingRule.getNodeAId(lastPrimary.get(1)));
                if (firstPrimaryLastWssLinkSiteId.equals(lastPrimaryFirstWssLinkSiteId)) {
                    continue;
                }
                //step3: 连接前后半段，生成新的path
                List<String> primary = new ArrayList<>(firstPrimary);
                primary.remove(primary.size() - 1);//最后一个点肯定是连接点的sitelink，后半部分已经包含了。
                primary.addAll(lastPrimary);
                //todo: mandatory will refactor later, then hack here
                int bundleNum = getBundleNum(clientLineNumber, commonFreqs.size(), commonFreqs.size());

                SiteLinkRoute siteLinkRoute = new SiteLinkRouteBuilder()
                        .setFixGrid(firstHalfSiteLinkRoute.getFixGrid())
                        .setBundleNumber(bundleNum)
                        .setCentralFrequencies(commonFreqs)
                        .setVendorName(firstHalfSiteLinkRoute.getVendorName())
                        .setPrimary(primary).build();

                shortPaths.add(new ShortPathBuilder().setSiteLinkRoute(siteLinkRoute).build());
            }

        }
        return shortPaths;
    }

    //在所有最短路径中，配对primary+secondary
    private List<ShortPath> getShortPathProtected(List<ShortPath> shortPathTotal, int clientLineNumber, String srcSite, String desSite) {
        List<ShortPath> output = new ArrayList<>();
        int size = shortPathTotal.size();
        for (int i = 0; i < size; i++) {
            ShortPath primaryShortPath = shortPathTotal.get(i);
            for (int j = i + 1; j < size; j++) {
                ShortPath shortPathSecondary = shortPathTotal.get(j);
                ShortPath shortPath = buildProtectedShortPath(primaryShortPath, shortPathSecondary, clientLineNumber, false, srcSite, desSite);
                if (shortPath != null) {
                    output.add(shortPath);
                }
            }
        }
        return output;
    }

    //在所有最短路径中，配对primary+secondary+third
    private List<ShortPath> getShortPathProtected2(List<ShortPath> shortPathTotal, int clientLineNumber, String srcSite, String desSite) {
        List<ShortPath> output = new ArrayList<>();
        int size = shortPathTotal.size();
        for (int i = 0; i < size; i++) {
            ShortPath primaryShortPath = shortPathTotal.get(i);
            for (int j = i + 1; j < size; j++) {
                ShortPath shortPathSecondary = shortPathTotal.get(j);
                ShortPath shortPath = buildProtectedShortPath(primaryShortPath, shortPathSecondary, clientLineNumber, false, srcSite, desSite);
                if (shortPath != null) {
                    for (int h = j + 1; h < size; h++) {
                        ShortPath shortPathThird = shortPathTotal.get(h);
                        shortPath = buildProtectedShortPath(primaryShortPath, shortPathThird, clientLineNumber, false, srcSite, desSite);
                        if (shortPath != null) {
                            shortPath = buildProtectedShortPath(shortPathSecondary, shortPathThird, clientLineNumber, false, srcSite, desSite);
                            if (shortPath != null) {

                                SiteLinkRoute siteLinkRoute = new SiteLinkRouteBuilder(shortPath.getSiteLinkRoute())
                                        .setPrimary(primaryShortPath.getSiteLinkRoute().getPrimary())
                                        .setSecondary(shortPathSecondary.getSiteLinkRoute().getPrimary())
                                        .setThird(shortPathThird.getSiteLinkRoute().getPrimary())
                                        .build();
                                shortPath = new ShortPathBuilder().setSiteLinkRoute(siteLinkRoute).build();
                                output.add(shortPath);
                            }
                        }
                    }
                }
            }
        }
        return output;
    }

    private List<ShortPath> getShortPathProtected2(String fixGrid, String srcSiteId, String destSiteId, List<Link> srcSiteLinks, List<Link> destSiteLinks, List<Link> siteLinks,
                                                   int clientRateNumber,
                                                   ShortPath primaryShortPath, boolean hasMandatorySiteLinkId, String srcSite, String desSite, String cardType) throws NeDesignerException {
//        HashSet<String> primarySiteLinks = new HashSet<>(primaryShortPath.getSiteLinkRoute().getPrimary());
//        String primarySrcSiteLink = primaryShortPath.getSiteLinkRoute().getPrimary().get(0);
//        String primaryDestSiteLink = primaryShortPath.getSiteLinkRoute().getPrimary().get(primaryShortPath.getSiteLinkRoute().getPrimary().size() - 1);
//        List<Link> secondarySiteLinksPool = siteLinks.stream()
//                .filter(item -> !primarySiteLinks.contains(item.getLinkId().getValue()))
//                .collect(Collectors.toList());
//        List<Link> secondarySrcSiteLinks = srcSiteLinks.stream()
//                .filter(item -> !item.getLinkId().getValue().equals(primarySrcSiteLink))
//                .collect(Collectors.toList());
//        List<Link> secondaryDestSiteLinks = destSiteLinks.stream()
//                .filter(item -> !item.getLinkId().getValue().equals(primaryDestSiteLink))
//                .collect(Collectors.toList());
//
//        List<ShortPath> shortPathsSecondary = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, secondarySrcSiteLinks, secondaryDestSiteLinks, secondarySiteLinksPool, clientRateNumber,
//                cardType);
//        List<ShortPath> output = new ArrayList<>();
//
//        for (ShortPath shortPathSecondary : shortPathsSecondary) {
//            ShortPath shortPath = buildProtectedShortPath(primaryShortPath, shortPathSecondary, clientRateNumber, hasMandatorySiteLinkId, srcSite, desSite);
//            if (shortPath != null) {
//                output.add(shortPath);
//            }
//        }
//        return output;
        return Collections.EMPTY_LIST;
    }

    private List<ShortPath> getShortPathProtected(String fixGrid, String srcSiteId, String destSiteId, List<Link> srcSiteLinks, List<Link> destSiteLinks, List<Link> siteLinks,
                                                  int clientRateNumber,
                                                  ShortPath primaryShortPath, boolean hasMandatorySiteLinkId, String srcSite, String desSite, String cardType, WDM_Band wdmBand) throws NeDesignerException {
        HashSet<String> primarySiteLinks = new HashSet<>(primaryShortPath.getSiteLinkRoute().getPrimary());
        String primarySrcSiteLink = primaryShortPath.getSiteLinkRoute().getPrimary().get(0);
        String primaryDestSiteLink = primaryShortPath.getSiteLinkRoute().getPrimary().get(primaryShortPath.getSiteLinkRoute().getPrimary().size() - 1);
        List<Link> secondarySiteLinksPool = siteLinks.stream()
                .filter(item -> !primarySiteLinks.contains(item.getLinkId().getValue()))
                .collect(Collectors.toList());
        List<Link> secondarySrcSiteLinks = srcSiteLinks.stream()
                .filter(item -> !item.getLinkId().getValue().equals(primarySrcSiteLink))
                .collect(Collectors.toList());
        List<Link> secondaryDestSiteLinks = destSiteLinks.stream()
                .filter(item -> !item.getLinkId().getValue().equals(primaryDestSiteLink))
                .collect(Collectors.toList());

        List<ShortPath> shortPathsSecondary = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, secondarySrcSiteLinks, secondaryDestSiteLinks, secondarySiteLinksPool, clientRateNumber,
                cardType, wdmBand);
        List<ShortPath> output = new ArrayList<>();

        for (ShortPath shortPathSecondary : shortPathsSecondary) {
            ShortPath shortPath = buildProtectedShortPath(primaryShortPath, shortPathSecondary, clientRateNumber, hasMandatorySiteLinkId, srcSite, desSite);
            if (shortPath != null) {
                output.add(shortPath);
            }
        }
        return output;
    }

    private ShortPath reversePrimarySecondary(ShortPath shortPath) {
        SiteLinkRoute oldSiteLinkRoute = shortPath.getSiteLinkRoute();
        SiteLinkRoute siteLinkRoute = new SiteLinkRouteBuilder()
                .setFixGrid(oldSiteLinkRoute.getFixGrid())
                .setBundleNumber(oldSiteLinkRoute.getBundleNumber())
                .setCentralFrequencies(oldSiteLinkRoute.getCentralFrequencies())
                .setVendorName(oldSiteLinkRoute.getVendorName())
                .setPrimary(oldSiteLinkRoute.getSecondary())
                .setSecondary(oldSiteLinkRoute.getPrimary())
                .build();

        return new ShortPathBuilder().setSiteLinkRoute(siteLinkRoute).build();
    }


    /**
     * 两条路径可以组成primary+secondary的必要条件是：
     * <p>
     * 1. 有共同的frequency
     * <p>
     * 2. 途径的sitelink不重合
     *
     * @param primaryShortPath
     * @param shortPathSecondary
     * @param hasMandatorySiteLinkId
     * @param srcSite
     * @param desSite
     * @return
     */
    private ShortPath buildProtectedShortPath(ShortPath primaryShortPath, ShortPath shortPathSecondary, int clientLineNumber, boolean hasMandatorySiteLinkId, String srcSite, String desSite) {
        List<Long> jointFrequency = new ArrayList<>(primaryShortPath.getSiteLinkRoute().getCentralFrequencies());
        jointFrequency.retainAll(shortPathSecondary.getSiteLinkRoute().getCentralFrequencies());

        if (jointFrequency.isEmpty()) {
            //no joint frequency
            return null;
        }
        List<String> jointSiteLinks = new ArrayList<>(primaryShortPath.getSiteLinkRoute().getPrimary());
        jointSiteLinks.retainAll(shortPathSecondary.getSiteLinkRoute().getPrimary());
        if (!jointSiteLinks.isEmpty()) {
            //路径有交集
            return null;
        }
        int bundleNum = getBundleNum(clientLineNumber, jointFrequency.size(), jointFrequency.size());
        int primaryBundle = primaryShortPath.getSiteLinkRoute().getBundleNumber();
        int secondaryBundle = shortPathSecondary.getSiteLinkRoute().getBundleNumber();
        if (bundleNum > primaryBundle) {
            bundleNum = primaryBundle;
        }
        if (bundleNum > secondaryBundle) {
            bundleNum = secondaryBundle;
        }
        List<String> primary = primaryShortPath.getSiteLinkRoute().getPrimary();
        List<String> secondary = shortPathSecondary.getSiteLinkRoute().getPrimary();

        Set<String> primarySites = primary.stream().flatMap(item -> getSites(item, srcSite, desSite).stream()).collect(Collectors.toSet());
        Set<String> secondarySites = secondary.stream().flatMap(item -> getSites(item, srcSite, desSite).stream()).collect(Collectors.toSet());
        if (!Collections.disjoint(primarySites, secondarySites)) {
            log.debug("No route, because primarySites:{} has common with secondarySites:{}", primarySites, secondarySites);
            return null;
        }

        //put the shorter path on primary, when there is no mandatory
        if (!hasMandatorySiteLinkId) {
            if (primary.size() > secondary.size()) {
                primary = shortPathSecondary.getSiteLinkRoute().getPrimary();
                secondary = primaryShortPath.getSiteLinkRoute().getPrimary();
            }
        }

        SiteLinkRoute siteLinkRoute = new SiteLinkRouteBuilder()
                .setFixGrid(primaryShortPath.getSiteLinkRoute().getFixGrid())
                .setBundleNumber(bundleNum)
                .setCentralFrequencies(jointFrequency)
                .setVendorName(primaryShortPath.getSiteLinkRoute().getVendorName())
                .setPrimary(primary)
                .setSecondary(secondary)
                .build();

        return new ShortPathBuilder().setSiteLinkRoute(siteLinkRoute).build();
    }

    private Set<String> getSites(String linkId, String srcSite, String desSite) {
        if (linkId.startsWith(WSS_LINK_PREFIX)) {
            Set<String> result = new HashSet<>();
            result.add(PhysicalLinkIdNamingRule.getSiteAId(linkId));
            return result;
        }
        Link siteLink = siteLinkDao.getSiteLinkById(linkId);
        return siteLink.getAugmentation(Link1.class)
                .getSite()
                .getExplictRoute()
                .getRoute()
                .get(0)
                .getPrimary()
                .getExplicitRouteObjects()
                .get(0)
                .getPathRouteObject()
                .stream()
                .filter(item -> item.getResourceType().getImplementedInterface().getName()
                        .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp.class.getName()))
                .map(item -> ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) item.getResourceType())
                        .getTpHop()
                        .getSiteRef()
                        .getValue())
                        .
                filter(item -> !item.equals(srcSite) && !item.equals(desSite))
                .collect(Collectors.toSet());
    }

    private boolean inSite(Link siteLink, String siteId) {
        return siteLink.getSource().getSourceNode().getValue().equals(siteId) || siteLink.getDestination().getDestNode().getValue().equals(siteId);
    }

    private boolean isOtm(Link siteLink, String siteId) {
        if (!siteLink.getAugmentation(Link1.class).getSite().getLinkGroup().equals("C+L")) {
            return true;
        }
        AExternal aExternal = siteLink.getAugmentation(Link1.class).getSite().getAExternal();
        if (aExternal != null && aExternal.getAddDropLink() != null) {
            Optional<AddDropLink> aMuxOptional = aExternal.getAddDropLink().stream()
                    .filter(a -> a.getLinkRef().contains(siteId) && a.getConnnectorType().equals(EquipType.MUXPANEL)).findFirst();
            if (aMuxOptional.isPresent()) {
                return true;
            }
        }

        ZExternal zExternal = siteLink.getAugmentation(Link1.class).getSite().getZExternal();
        if (zExternal != null && zExternal.getAddDropLink() != null) {
            Optional<AddDropLink> zMuxOptional = zExternal.getAddDropLink().stream()
                    .filter(z -> z.getLinkRef().contains(siteId) && z.getConnnectorType().equals(EquipType.MUXPANEL)).findFirst();
            if (zMuxOptional.isPresent()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<Link>> getGridSitelinkMapFromDB(String riskGroupName, String planeName, String planeId, List<String> matchedGrids, Set<String> excludeLinks, WDM_Band wdmBand) {
        Map<String, List<Link>> fixGridSiteLinksMap = new HashMap<>();//key is fix grid, e.g. 50,75
        List<Link> flexGridSiteLinks = null;
        List<String> matchedWdmBands = tunnelUtils.getMatchedWdmBands(wdmBand);
        for (String matchedGrid : matchedGrids) {
            List<Link> siteLinks = siteLinkDao.filterGridAvailable(matchedGrid, riskGroupName, planeName, planeId, excludeLinks, matchedWdmBands);

            if (siteLinks == null || siteLinks.isEmpty()) {
                log.debug("No siteLink found at Grid:{}, riskGroupName:{},planeName:{}", matchedGrid, riskGroupName, planeName);
                continue;
            }
            if (matchedGrid.equals(tunnelUtils.GRID_0)) {
                flexGridSiteLinks = siteLinks;
            } else {
                fixGridSiteLinksMap.put(matchedGrid, siteLinks);
            }
        }
        if (fixGridSiteLinksMap.isEmpty()) {
            if (flexGridSiteLinks == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("Group[%s] and plane[%s] can't find available siteLink", riskGroupName,
                                planeName));
            }
            //all site links are flex grid
            fixGridSiteLinksMap.put(tunnelUtils.GRID_75, flexGridSiteLinks);//flex默认用75的间隔

        } else {
            if (flexGridSiteLinks != null) {
                for (Entry<String, List<Link>> entry : fixGridSiteLinksMap.entrySet()) {
                    entry.getValue().addAll(flexGridSiteLinks);//flex can connect to any grid.
                }
            }

        }
        return fixGridSiteLinksMap;
    }


    /**
     * 取出每一个srcSiteLinkId最短的路径
     *
     * @param fixGrid
     * @param destSiteId
     * @param srcSiteLinks
     * @param destSiteLinks
     * @param siteLinks
     * @param clientLineRateNumber
     * @param wdmBand
     * @return
     */
    private List<ShortPath> getShortPathNonProtected(String fixGrid, String srcSiteId, String destSiteId, List<Link> srcSiteLinks, List<Link> destSiteLinks, List<Link> siteLinks,
                                                     int clientLineRateNumber, String cardType, WDM_Band wdmBand) throws NeDesignerException {
        List<ShortPath> result = new ArrayList<>();
        GridType fixGridType = null;
        try {
            fixGridType = tunnelUtils.getGridByString(fixGrid);
        } catch (NeDesignerException e) {
            log.error("Failed to getShortPathNonProtected because invalid fixGrid:{}", fixGrid, e);
            return result;
        }
        for (Link srcSiteLink : srcSiteLinks) {
            //scenario1: single siteLink is the route
            String srcSiteLinkId = srcSiteLink.getLinkId().getValue();
//            List<DJNode> shortPathNodes = new ArrayList<>();
            String srcVendorName = srcSiteLink.getAugmentation(Link1.class).getSite().getVendorName();

            DJNode srcNode = buildNode(srcSiteLink, fixGridType, cardType, wdmBand);
            if (srcSiteLinkId.contains(destSiteId)) {//最短路径就是一个复用段
                ShortPath shortPath = buildShortPath(fixGrid, Arrays.asList(srcNode), clientLineRateNumber, srcVendorName);
                if (shortPath != null) {
                    result.add(shortPath);//这就是这个srcSiteLink出发的最短的一条
                }
                continue;
            }

            List<Long> srcFreq = srcNode.getAvailableCentFrequency();
            for (Link destSiteLink : destSiteLinks) {
                if (destSiteLink.getLinkId().getValue().contains(srcSiteId)) {//最短路径就是一个复用段,在src已经处理了
                    continue;
                }
                String dstVendorName = destSiteLink.getAugmentation(Link1.class).getSite().getVendorName();
                if (!srcVendorName.equals(dstVendorName)) {
                    continue;
                }
                DJNode destNode = buildNode(destSiteLink, fixGridType, cardType, wdmBand);
                List<Long> dstFreq = destNode.getAvailableCentFrequency();
                List<Long> commonFreq = new ArrayList<>(srcFreq);
                commonFreq.retainAll(dstFreq);
                if (commonFreq.isEmpty()) {//frequency没有交集
                    continue;
                }
                List<DJNode> shortPathNodes = calculateShortPath(srcSiteId, destSiteId, siteLinks, srcNode, destNode, new HashSet<>(commonFreq), fixGridType, cardType, wdmBand);
                if (shortPathNodes.isEmpty()) {
                    log.debug("No short path available between {} and {}.", srcSiteLinkId, destSiteLink.getLinkId().getValue());
                    continue;
                }
                if (!isValidPath(shortPathNodes, srcSiteId, destSiteId)) {
                    continue;
                }
                ShortPath shortPath = buildShortPath(fixGrid, shortPathNodes, clientLineRateNumber, srcVendorName);
                if (shortPath == null) {
                    continue;
                }
                result.add(shortPath);
            }

        }

        return result;

    }

    /**
     * 判断一条路径 List<DJNode> 是否是合法路径，即：
     * <p>
     * 从指定起点 srcSiteId 出发
     * <p>
     * 到达指定终点 destSiteId
     * <p>
     * 所有边是连续连接的
     * <p>
     * 中间不包含任何重复访问的站点（即不成环）
     *
     * @param path
     * @param srcSiteId
     * @param destSiteId
     * @return
     */
    private boolean isValidPath(List<DJNode> path, String srcSiteId, String destSiteId) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        Set<String> visitedSites = new HashSet<>();
        String current = srcSiteId;
        visitedSites.add(current);

        for (DJNode edge : path) {
            String a = edge.getSiteA();
            String z = edge.getSiteZ();

            // 判断当前边是否与 current 相连
            String next;
            if (a.equals(current)) {
                next = z;
            } else if (z.equals(current)) {
                next = a;
            } else {
                // 当前边不连续，路径非法
                return false;
            }

            // 如果 next 已访问过，说明路径中包含环
            if (visitedSites.contains(next)) {
                return false;
            }

            visitedSites.add(next);
            current = next;
        }

        // 最后是否到达目标站点
        return current.equals(destSiteId);
    }

    private List<DJNode> calculateShortPath(String srcSiteId, String destSiteId, List<Link> siteLinks, DJNode srcNode, DJNode destNode, Set<Long> commonFreq, GridType fixGridType, String cardType,
                                            WDM_Band wdmBand) {
        Graph graph = buildGraph(srcSiteId, destSiteId, siteLinks, commonFreq, fixGridType, cardType, wdmBand);
        DJNode djSourceNode = graph.getNodeByLinkId(srcNode.getSiteLinkId());
        DJNode djDstNode = graph.getNodeByLinkId(destNode.getSiteLinkId());
        if (djSourceNode == null || djDstNode == null) {
            return Collections.EMPTY_LIST;
        }
        Dijkstra.calculateShortestPathFromSource(graph, djSourceNode);
        List<DJNode> shortPathNodes = djDstNode.getShortestPath();
        if (shortPathNodes.isEmpty()) {
            return shortPathNodes;
        }
        shortPathNodes.add(djDstNode);//因为算出来的path不包括目的的
        return shortPathNodes;
    }

    private ShortPath buildShortPath(String fixGrid, List<DJNode> nodes, int clientRateNumber, String vendorName) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        List<String> primary = new ArrayList<>();
        List<Long> freqs = nodes.get(0).getAvailableCentFrequency();
        if (freqs.isEmpty()) {
            return null;
        }
        List<Long> commonFreqs = new ArrayList<>(freqs);
        int size = nodes.size();
        String preWssLink = null;

        for (int i = 0; i < size; i++) {
            DJNode node = nodes.get(i);
            commonFreqs.retainAll(node.getAvailableCentFrequency());
            if (commonFreqs.isEmpty()) {
                return null;
            }
            primary.add(node.getSiteLinkId());
            if (i != size - 1) {
                DJNode nextNode = nodes.get(i + 1);
                String wssLinkId = node.getWssLinkId(nextNode.getSiteLinkId());
                if (wssLinkId == null) {
                    continue;
//                    String msg = String.format("Build short path error, because failed to get wssLinkId from %s to %s", node.getSiteLinkId(), nextNode.getSiteLinkId());
//                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);

                }
                if (preWssLink != null) {
                    String preWssLinkSiteId = PhysicalTpIdNamingRule.getSiteId(PhysicalLinkIdNamingRule.getTpAId(preWssLink));
                    String currentWssLinkSiteId = PhysicalTpIdNamingRule.getSiteId(PhysicalLinkIdNamingRule.getTpAId(wssLinkId));
                    if (preWssLinkSiteId.equals(currentWssLinkSiteId)) {
                        log.debug("Not valid path, because preWssLink:{} and currentWssLink:{} in same site.", preWssLinkSiteId, wssLinkId);
                        return null;
                    }

                }
                primary.add(wssLinkId);
                preWssLink = wssLinkId;
            }
        }

        if (commonFreqs.isEmpty()) {
            return null;
        }
        Integer minBandwidth = getMinBandwidth(nodes);
        int freqSize = freqs.size();
        int bundleNum = getBundleNum(clientRateNumber, freqSize, minBandwidth);
        SiteLinkRoute siteLinkRoute = new SiteLinkRouteBuilder()
                .setFixGrid(fixGrid)
                .setBundleNumber(bundleNum)
                .setCentralFrequencies(commonFreqs)
                .setVendorName(vendorName)
                .setPrimary(primary).build();

        return new ShortPathBuilder().setSiteLinkRoute(siteLinkRoute).build();
    }

    private Integer getMinBandwidth(List<DJNode> nodes) {
        List<String> siteLinks = nodes.stream().map(DJNode::getSiteLinkId).collect(Collectors.toList());
        Integer minBandwidth = siteLinkDao.getMinimumBandwidthByIds(siteLinks);
        return minBandwidth;
    }

    private int getBundleNum(int clientRateNumber, int freqSize, int minBandwidth) {
        int avaSize = freqSize > minBandwidth ? minBandwidth : freqSize;
        return avaSize * clientRateNumber;
    }

    protected Graph buildGraph(String srcSiteId, String destSiteId, List<Link> siteLinks, Set<Long> commonFreq, GridType fixGridType, String cardType, WDM_Band wdmBand) {

        List<String> siteLinkIds = siteLinks.stream().map(item -> item.getLinkId().getValue()).collect(Collectors.toList());
        List<Link> wssLinks = phyLinkDao.filterAllWssLink(siteLinkIds, srcSiteId, destSiteId);
        Map<String, DJNode> nodeMap = new HashMap<>();
        for (Link wssLink : wssLinks) {
            try {
                List<SupportedLink> supportedLink = wssLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical().getSupportedLink();
                String siteLinkAId = supportedLink.get(0).getLinkRef().getValue();
                DJNode nodeA = nodeMap.get(siteLinkAId);
                if (nodeA == null) {
                    Link siteLinkA = siteLinkDao.getSiteLinkById(siteLinkAId);
                    List<Long> centralFrequencies = new FrequencyAvailable(siteLinkA).getAllPossibleCentFrequency(fixGridType);
                    if (Collections.disjoint(commonFreq, centralFrequencies)) {
                        //no joint frequency
                        continue;
                    }
                    nodeA = buildNode(siteLinkA, fixGridType, cardType, wdmBand);
                }

                String siteLinkZId = supportedLink.get(1).getLinkRef().getValue();
                DJNode nodeZ = nodeMap.get(siteLinkZId);
                if (nodeZ == null) {
                    Link siteLinkZ = siteLinkDao.getSiteLinkById(siteLinkZId);
                    List<Long> centralFrequencies = new FrequencyAvailable(siteLinkZ).getAllPossibleCentFrequency(fixGridType);
                    if (Collections.disjoint(commonFreq, centralFrequencies)) {
                        continue;
                    }
                    nodeZ = buildNode(siteLinkZ, fixGridType, cardType, wdmBand);
                }

                List<Long> azJointFreq = new ArrayList<>(nodeA.getAvailableCentFrequency());
                azJointFreq.retainAll(nodeZ.getAvailableCentFrequency());
                if (Collections.disjoint(commonFreq, azJointFreq)) {
                    continue;
                }

                nodeA.addDestination(nodeZ, 1);
                nodeA.addWssLink(siteLinkZId, wssLink.getLinkId().getValue());
                nodeZ.addDestination(nodeA, 1);
                nodeZ.addWssLink(siteLinkAId, wssLink.getLinkId().getValue());
                nodeMap.put(siteLinkAId, nodeA);
                nodeMap.put(siteLinkZId, nodeZ);
            } catch (Exception e) {
                log.error("Drop invalid wssLink when build graph.{}.", wssLink, e);
            }
        }
        Graph graph = new Graph();
        for (Entry<String, DJNode> entry : nodeMap.entrySet()) {
            graph.addNode(entry.getKey(), entry.getValue());
        }
        return graph;
    }

//    protected Graph buildGraph(String srcSiteId, String destSiteId, List<Link> siteLinks, Set<Long> commonFreq, GridType fixGridType, String cardType) {
//        List<String> siteLinkIds = siteLinks.stream().map(item -> item.getLinkId().getValue()).collect(Collectors.toList());
//        List<Link> wssLinks = phyLinkDao.filterAllWssLink(siteLinkIds, srcSiteId, destSiteId);
//        Map<String, DJNode> nodeMap = new HashMap<>();
//        for (Link wssLink : wssLinks) {
//            try {
//                List<SupportedLink> supportedLink = wssLink.getAugmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical().getSupportedLink();
//                String siteLinkAId = supportedLink.get(0).getLinkRef().getValue();
//                DJNode nodeA = nodeMap.get(siteLinkAId);
//                if (nodeA == null) {
//                    Link siteLinkA = siteLinkDao.getSiteLinkById(siteLinkAId);
//                    List<Long> centralFrequencies = new FrequencyAvailable(siteLinkA).getAllPossibleCentFrequency(fixGridType);
//                    if (Collections.disjoint(commonFreq, centralFrequencies)) {
//                        //no joint frequency
//                        continue;
//                    }
//                    nodeA = buildNode(siteLinkA, fixGridType, cardType);
//                }
//
//                String siteLinkZId = supportedLink.get(1).getLinkRef().getValue();
//                DJNode nodeZ = nodeMap.get(siteLinkZId);
//                if (nodeZ == null) {
//                    Link siteLinkZ = siteLinkDao.getSiteLinkById(siteLinkZId);
//                    List<Long> centralFrequencies = new FrequencyAvailable(siteLinkZ).getAllPossibleCentFrequency(fixGridType);
//                    if (Collections.disjoint(commonFreq, centralFrequencies)) {
//                        continue;
//                    }
//                    nodeZ = buildNode(siteLinkZ, fixGridType, cardType);
//                }
//
//                List<Long> azJointFreq = new ArrayList<>(nodeA.getAvailableCentFrequency());
//                azJointFreq.retainAll(nodeZ.getAvailableCentFrequency());
//                if (Collections.disjoint(commonFreq, azJointFreq)) {
//                    continue;
//                }
//
//                nodeA.addDestination(nodeZ, 1);
//                nodeA.addWssLink(siteLinkZId, wssLink.getLinkId().getValue());
//                nodeZ.addDestination(nodeA, 1);
//                nodeZ.addWssLink(siteLinkAId, wssLink.getLinkId().getValue());
//                nodeMap.put(siteLinkAId, nodeA);
//                nodeMap.put(siteLinkZId, nodeZ);
//            } catch (Exception e) {
//                log.error("Drop invalid wssLink when build graph.{}.", wssLink, e);
//            }
//        }
//        Graph graph = new Graph();
//        for (Entry<String, DJNode> entry : nodeMap.entrySet()) {
//            graph.addNode(entry.getKey(), entry.getValue());
//        }
//        return graph;
//    }

    protected DJNode buildNode(Link siteLink, GridType fixGridType, String cardType, WDM_Band wdmBand) throws NeDesignerException {
//        String vendorName = siteLink.getAugmentation(Link1.class).getSite().getVendorName();
//        String productType = siteLink.getAugmentation(Link1.class).getSite().getProductType();
//        WDM_Band wdmBand = getWdmBand(vendorName, productType, cardType);
        List<Long> centralFrequencies = FrequencyAvailable.getFreeCentFrequency(Arrays.asList(siteLink), wdmBand, fixGridType);
        return new DJNode(siteLink.getLinkId().getValue(), centralFrequencies);
    }

    private WDM_Band getWdmBand(String vendorName, String productType, String cardType) throws NeDesignerException {
        return WDM_Band.fromString(neDesigner.getNeInfo(vendorName, productType, NodeType.TD.name()).getCardByCardType(cardType).getWdmBand());
    }

    private static final class AStarPathGroup {
        private final List<DJNode> nodes;
        private final String vendorName;
        private final List<Long> frequencies = new ArrayList<>();

        private AStarPathGroup(List<DJNode> nodes, String vendorName) {
            this.nodes = new ArrayList<>(nodes);
            this.vendorName = vendorName;
        }
    }

    class ReusedRouteTemp {

        private final ArrayList<String> third;
        private ArrayList<String> primary;
        private ArrayList<String> secondary;
        private String vendorName;
        private String productType;
        private String fixGrid;

        public ReusedRouteTemp(ArrayList<String> primary, ArrayList<String> secondary, ArrayList<String> ochThird, String vendorName, String productType, String fixGrid) {
            this.primary = primary;
            this.secondary = secondary;
            this.third = ochThird;
            this.vendorName = vendorName;
            this.productType = productType;
            this.fixGrid = fixGrid;
        }

        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        public boolean equals(Object anObject) {
            if (this == anObject) {
                return true;
            }
            if (anObject instanceof ReusedRouteTemp) {
                ReusedRouteTemp another = (ReusedRouteTemp) anObject;
                if (!another.vendorName.equals(vendorName)) {
                    return false;
                }
                if (!another.productType.equals(productType)) {
                    return false;
                }
                if (!another.primary.equals(primary)) {
                    return false;
                }
                if (this.secondary == null) {
                    if (another.secondary != null) {
                        return false;
                    }
                } else if (another.secondary == null || !this.secondary.equals(((ReusedRouteTemp) anObject).secondary)) {
                    return false;
                }

                if (this.third == null) {
                    if (another.third == null) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (another.third == null) {
                        return false;
                    } else {
                        return this.third.equals(((ReusedRouteTemp) anObject).third);
                    }
                }
            }
            return false;
        }


        public void reverse() {
            if (this.primary != null) {
                Collections.reverse(this.primary);
            }
            if (this.secondary != null) {
                Collections.reverse(this.secondary);
            }
            if (this.third != null) {
                Collections.reverse(this.third);
            }
        }
    }
}
