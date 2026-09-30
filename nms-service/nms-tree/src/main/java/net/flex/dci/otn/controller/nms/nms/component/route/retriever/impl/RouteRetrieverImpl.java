package net.flex.dci.otn.controller.nms.nms.component.route.retriever.impl;

import static net.flex.dci.otn.controller.nms.utils.NMSUtils.getSecondary;
import static net.flex.dci.otn.controller.nms.utils.NMSUtils.getThird;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.component.route.link.RouteSequenceRetriever;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteSequenceDtoRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.LinkRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.RouteDetailDto;
import net.flex.dci.otn.controller.nms.nms.dto.RouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.nms.dto.ThirdRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.route.LinkRouteDetailsDto;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.ThirdBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.ExplicitRouteHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/6/29 11:07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteRetrieverImpl implements RouteRetriever {


    private final RouteSequenceRetriever routeSequenceRetriever;

    private final RouteSequenceDtoRetriever routeSequenceDtoRetriever;

    @Override
    public List<RouteInfo> retrieverRouteInfo(RouteInfoDto routeDto) {
        log.info("[TIMING] retrieverRouteInfo start");
        log.debug("retrieve the route info for routes:{}", routeDto);
        String source = routeDto.getSource();
        String destination = routeDto.getDestination();
        List<Route> detailRoute = routeDto.getRoutes();
        List<RouteInfo> routeInfoList = retrieveRoute(detailRoute, source, destination,
                routeDto.getConnectionId());
        return routeInfoList;
    }

    @Override
    public RouteSequenceDto retrieverRouteSequence(Route route, String sourceSite,
            String destination) {
        Primary primary = route.getPrimary();
        Secondary secondary = route.getSecondary();
        RouteSequenceDto routeSequenceDto = retrieveRouteSequence(
                primary.getExplicitRouteObjects().get(0).getPathRouteObject(), sourceSite,
                destination);
//        routeSequenceDto.setXcs(route.getPrimary().getCrossConnections());
        routeSequenceDto.setXcIds(route.getPrimary().getCrossConnections().stream()
                .map(xc -> xc.getCrossConnectionId().getValue()).collect(
                        Collectors.toList()));
        if (secondary != null) {
            RouteSequenceDto secondaryRouteSequenceDto = retrieveRouteSequence(
                    secondary.getExplicitRouteObjects().get(0).getPathRouteObject(), sourceSite,
                    destination);
            routeSequenceDto.setSecondary(secondaryRouteSequenceDto);
        }
        return routeSequenceDto;
    }

    /**
     * retrieve the route
     *
     * @param detailRoute
     * @param source
     * @param destination
     * @return
     */
    private List<RouteInfo> retrieveRoute(List<Route> detailRoute, String source,
            String destination, String connectionId) {
        log.debug("retrieve the detail route ,source is:{},destination is:{},connectionId:{}",
                source, destination, connectionId);
        List<RouteInfo> routeInfos = new ArrayList<>();
        long t0 = System.currentTimeMillis();
        detailRoute.sort(Comparator.comparing(Route::getIndex));
        long t1 = System.currentTimeMillis();
        log.info("[TIMING] retrieveRoute sort: {}ms", t1 - t0);
        detailRoute.forEach(route -> {
            long r0 = System.currentTimeMillis();
            Short index = route.getIndex();
            Primary primaryRoute = route.getPrimary();
            Secondary secondaryRoute = route.getSecondary();
            //add third and more route
            List<Third> thirdRoute = route.getThird();
            //if the route have the secondary route for the primary  like tunnel och
            LinkRouteDto linkRouteDto
                    = retrievePrimaryRoute(primaryRoute, source, destination);
            long r1 = System.currentTimeMillis();
            log.info("[TIMING] retrievePrimaryRoute: {}ms", r1 - r0);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary secondaryRouteInfo
                    = retrieveSecondaryRoute(secondaryRoute, source, destination);
            long r2 = System.currentTimeMillis();
            log.info("[TIMING] retrieveSecondaryRoute: {}ms", r2 - r1);
            ThirdRouteDto thirdRouteInfo = retrieveThirdRoute(thirdRoute, source, destination);
            long r3 = System.currentTimeMillis();
            log.info("[TIMING] retrieveThirdRoute: {}ms", r3 - r2);
            RouteInfoBuilder routeInfoBuilder = new RouteInfoBuilder();
            routeInfoBuilder.setIndex(index);
            routeInfoBuilder.setKey(new RouteInfoKey(index));
            routeInfoBuilder.setPrimary(linkRouteDto.getPrimary());

            routeInfoBuilder.setSecondary(
                    secondaryRouteInfo == null ? linkRouteDto.getSecondary() : secondaryRouteInfo);
            routeInfoBuilder.setThird(
                    thirdRouteInfo == null ? linkRouteDto.getThird()
                            : thirdRouteInfo.getRouteDetails());
            routeInfos.add(routeInfoBuilder.build());
            long r4 = System.currentTimeMillis();
            log.info("[TIMING] build RouteInfo: {}ms", r4 - r3);
        });
        return routeInfos;
    }


    /**
     * retrieve primary route
     *
     * @param primaryRoute
     * @param source
     * @param destination
     * @return
     */
    private LinkRouteDto retrievePrimaryRoute(
            Primary primaryRoute, String source, String destination) {
        log.debug("start to retrieve the primary route ");
        long p0 = System.currentTimeMillis();
        List<ExplicitRouteObjects> explicitRoutes = primaryRoute.getExplicitRouteObjects();
        List<CrossConnections> crossConnections = primaryRoute.getCrossConnections();
        RouteDetailDto routeDetailDto = extractRouteSequence(explicitRoutes, source,
                destination);
        long p1 = System.currentTimeMillis();
        log.info("[TIMING] extractRouteSequence: {}ms", p1 - p0);
        crossConnections.addAll(routeDetailDto.getPrimary().getCrossConnections());
        PrimaryBuilder primaryBuilder = new PrimaryBuilder();
        primaryBuilder.setRouteSequence(routeDetailDto.getPrimary().getRouteSequences());
        crossConnections = CrossConnectionUtils.refactorSequenceCrossConnections(crossConnections);
        long p2 = System.currentTimeMillis();
        log.info("[TIMING] refactorXc+setRs: {}ms", p2 - p1);
        primaryBuilder.setCrossConnections(crossConnections);
        primaryBuilder.setSiteSequence(routeDetailDto.getPrimary().getSites());
        //have the second route detail
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary secondary = null;
        if (routeDetailDto.getSecondary() != null) {
            secondary = buildSecondaryRouteInfo(routeDetailDto.getSecondary());
        }
        //have the third route detail
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Third> tertiary = null;
        if (!CollectionUtils.isEmpty(routeDetailDto.getTertiary())) {
            tertiary = buildTertiaryRoutInfo(routeDetailDto.getTertiary());
        }
        return LinkRouteDto.builder().primary(primaryBuilder.build()).secondary(secondary)
                .third(tertiary).build();
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary buildSecondaryRouteInfo(
            RouteDto secondaryRouteDto) {
        log.debug("build secondary route info");
        SecondaryBuilder secondaryBuilder = new SecondaryBuilder();
        secondaryBuilder.setRouteSequence(secondaryRouteDto.getRouteSequences());
        List<CrossConnections> secondXcs = secondaryRouteDto.getCrossConnections();
        secondXcs = CrossConnectionUtils.refactorSequenceCrossConnections(secondXcs);
        secondaryBuilder.setCrossConnections(secondXcs);
        secondaryBuilder.setSiteSequence(
                secondaryRouteDto.getSites());
        return secondaryBuilder.build();
    }

    /**
     * build tertiary route info
     *
     * @param tertiary
     * @return
     */
    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Third> buildTertiaryRoutInfo(
            List<RouteDto> tertiary) {
        log.debug("build tertiary route info");
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Third> thirds = new ArrayList<>();
        short index = 1;
        for (RouteDto routeDto : tertiary) {
            ThirdBuilder thirdBuilder = new ThirdBuilder();
            List<CrossConnections> xcs = routeDto.getCrossConnections();
            xcs = CrossConnectionUtils.refactorSequenceCrossConnections(xcs);
            thirdBuilder.setCrossConnections(xcs);
            thirdBuilder.setIndex(index++);
            thirdBuilder.setSiteSequence(routeDto.getSites());
            thirdBuilder.setRouteSequence(routeDto.getRouteSequences());
            thirds.add(thirdBuilder.build());
        }

        return thirds;
    }


    /**
     * retrieve secondary rout
     *
     * @param secondaryRoute
     * @param source
     * @param destination
     * @return
     */
    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary retrieveSecondaryRoute(
            Secondary secondaryRoute, String source, String destination) {
        if (secondaryRoute == null) {
            log.debug("the route don't have the secondary path,discard it");
            return null;
        }
        List<ExplicitRouteObjects> explicitRoutes = secondaryRoute.getExplicitRouteObjects();
        List<CrossConnections> crossConnections = secondaryRoute.getCrossConnections();
        LinkRouteDetailsDto linkRouteDetailsDto = retrieveRouteDetail(explicitRoutes,
                crossConnections, source, destination);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary secondary = getSecondary(
                linkRouteDetailsDto);
        return secondary;
    }


    private ThirdRouteDto retrieveThirdRoute(List<Third> thirdRoutes, String source,
            String destination) {
        log.debug("retrieve the one or more third role route");
        if (CollectionUtils.isEmpty(thirdRoutes)) {
            log.info("the route don't have more than one third rout,discard it");
            return null;
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Third> thirdRouts = new ArrayList<>();
        for (Third thirdRoute : thirdRoutes) {
            LinkRouteDetailsDto linkRouteDetailsDto = retrieveRouteDetail(
                    thirdRoute.getExplicitRouteObjects(), thirdRoute.getCrossConnections(), source,
                    destination);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Third third = getThird(
                    linkRouteDetailsDto, thirdRoute.getIndex());
            thirdRouts.add(third);
        }
        return ThirdRouteDto.builder().routeDetails(thirdRouts).build();
    }

    private LinkRouteDetailsDto retrieveRouteDetail(
            List<ExplicitRouteObjects> explicitRouteObjects,
            List<CrossConnections> crossConnections, String source, String destination) {
        log.debug("get current explicitRoute objects from source:{} to destination:{}", source,
                destination);
        RouteDetailDto routeDetailDto = extractRouteSequence(explicitRouteObjects, source,
                destination);
        List<CrossConnections> currentRouteCrossConnections = crossConnections;
        currentRouteCrossConnections.addAll(routeDetailDto.getPrimary().getCrossConnections());
        crossConnections = CrossConnectionUtils.refactorSequenceCrossConnections(crossConnections);
        return LinkRouteDetailsDto.builder().routeDetailDto(routeDetailDto)
                .refCrossConnections(crossConnections).build();
    }


    private RouteDetailDto extractRouteSequence(List<ExplicitRouteObjects> explicitRoutes,
            String source, String destination) {
        log.debug("extract explicit route object");
        //assem the explicit route list only have one element
        RouteDetailDto routeDetailDto = RouteDetailDto.builder().build();
        if (explicitRoutes.isEmpty() || explicitRoutes.get(0) == null) {
            return routeDetailDto;
        }
        long e0 = System.currentTimeMillis();
        List<PathRouteObject> pathRoutes = explicitRoutes.get(
                0).getPathRouteObject();
        List<PathRouteObject> sortedPathRoutes = sortPathRoutes(pathRoutes, source, destination);
        long e1 = System.currentTimeMillis();
        log.info("[TIMING] sortPathRoutes: {}ms, pathRoutes.size={}", e1 - e0, pathRoutes.size());
        RouteSequenceDto routeSequenceDto = retrieveRouteSequence(sortedPathRoutes, source,
                destination);
        long e2 = System.currentTimeMillis();
        log.info("[TIMING] retrieveRouteSequence: {}ms, hopCount={}", e2 - e1,
                sortedPathRoutes.size());
        routeDetailDto = routeSequenceDtoRetriever.retrieveRouteDetail(
                routeSequenceDto.getPrimary());
        long e3 = System.currentTimeMillis();
        log.info("[TIMING] retrieveRouteDetail: {}ms", e3 - e2);
        return routeDetailDto;
    }

    private RouteSequenceDto retrieveRouteSequence(List<PathRouteObject> sortedPathRoutes,
            String source, String destination) {
        log.debug("retrieve the path route");
        long s0 = System.currentTimeMillis();
        RouteSequenceDto routeSequenceDto = new RouteSequenceDto();
        RouteSequenceDto routeSequenceDtoRoot = new RouteSequenceDto();
        routeSequenceDtoRoot.setPrimary(routeSequenceDto);
        int tpCount = 0;
        int linkCount = 0;
        int hopIdx = 0;
        for (PathRouteObject pathRouteObject : sortedPathRoutes) {
            long h0 = System.currentTimeMillis();
            hopIdx++;
            ResourceType resourceType = pathRouteObject.getResourceType();
            Class<?> clazz = resourceType.getImplementedInterface();
            if (clazz.isAssignableFrom(Tp.class)) {
                tpCount++;
                routeSequenceDto = routeSequenceRetriever.retrieveTpHopRouteSequence(
                        pathRouteObject, routeSequenceDto);
            } else if (clazz.isAssignableFrom(Link.class)) {
                linkCount++;
                log.debug("get route hop for the link: {}",
                        pathRouteObject.getTopologyRef().getValue());
                routeSequenceDto = routeSequenceRetriever.retrieveLinkRouteSequence(
                        pathRouteObject, routeSequenceDto, source, destination);
            }
            long h1 = System.currentTimeMillis();
            long hopCost = h1 - h0;
            if (hopCost > 1) {
                log.info("[TIMING] routeHop[{}]: {}ms, type={}", hopIdx, hopCost,
                        clazz.isAssignableFrom(Link.class) ? "Link" : "Tp");
            }
        }
        long s1 = System.currentTimeMillis();
        log.info("[TIMING] retrieveRouteSequence loop: {}ms, tpHops={}, linkHops={}", s1 - s0,
                tpCount, linkCount);
        return routeSequenceDtoRoot.getPrimary();
    }


    private RouteDetailDto retrieveRouteSequenceDto(RouteSequenceDto routeSequenceDto) {
        List<RouteSequence> rss = new ArrayList<>();
        List<CrossConnections> xcs = new ArrayList<>();
        HashSet<String> siteSet = new HashSet<>();
        RouteSequenceDto routeSequence = routeSequenceDto;
        long seqIndex = 1L;
        while (routeSequence != null) {
            RouteSequenceBuilder routeSequenceBuilder = new RouteSequenceBuilder();
            routeSequenceBuilder.setSequence(seqIndex);
//            if (routeSequence.getResourceType() != null) {
//                routeSequenceBuilder.setResourceType(routeSequence.getResourceType());
//                routeSequenceBuilder.setTopologyRef(routeSequence.getTopologyRef());
//                routeSequenceBuilder.setKey(new RouteSequenceKey(seqIndex));
//                rss.add(routeSequenceBuilder.build());
//                xcs.addAll(routeSequence.getXcs());
//                siteSet.addAll(routeSequence.getSiteSet());
//                seqIndex++;
//            }
            routeSequence = routeSequence.getPrimary();
        }

        return RouteDetailDto.builder()
                .build();
    }

    private List<PathRouteObject> sortPathRoutes(List<PathRouteObject> pathRoutes, String source,
            String destination) {
        pathRoutes.sort(Comparator.comparing(ExplicitRouteHop::getIndex));
        //todo:get direction for the route
        PathRouteObject firstSeq = pathRoutes.get(0);
        Tp resourceType = (Tp) firstSeq.getResourceType();
        String routeSite = resourceType.getTpHop().getSiteRef().getValue();
        if (routeSite.equalsIgnoreCase(source)) {
            pathRoutes.sort(Comparator.comparing(ExplicitRouteHop::getIndex));
        } else if (routeSite.equalsIgnoreCase(destination)) {
            pathRoutes.sort(Comparator.comparing(ExplicitRouteHop::getIndex).reversed());
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the route is invalided");
        }
        return pathRoutes;
    }


}
