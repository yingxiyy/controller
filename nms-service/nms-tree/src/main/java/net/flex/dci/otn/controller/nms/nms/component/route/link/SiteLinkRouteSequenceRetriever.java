package net.flex.dci.otn.controller.nms.nms.component.route.link;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.nms.enums.RouteHopType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/6/29 16:24
 */
@Component
@Slf4j
public class SiteLinkRouteSequenceRetriever extends AbstractRouteSequenceRetriever {

    @Override
    public RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject,
            String source) {
        log.debug("start to retrieve the route for the site link");
        LinkHop linkHop = ((Link) pathRouteObject.getResourceType()).getLinkHop();
        TopologyId topologyRef = pathRouteObject.getTopologyRef();
        String linkId = linkHop.getLinkRef().getValue();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink
                = netconfTopology.getSiteLink(linkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "invalid route data,please contact administrator");
        }
        Link1 sitePhysical = siteLink.getAugmentation(Link1.class);
        List<Route> siteLinkRoutes = sitePhysical.getSite()
                .getExplictRoute().getRoute();
        RouteSequenceDto routeSequenceDto = new RouteSequenceDto();
        for (Route route : siteLinkRoutes) {
            Primary primaryRoute = route.getPrimary();
            Secondary secondaryRoute = route.getSecondary();
            List<PathRouteObject> primaryPathRouteObjects = getPathRoutes(
                    primaryRoute.getExplicitRouteObjects());
            RouteSequenceDto primarySequenceDto = retrieveRoute(
                    primaryPathRouteObjects, primaryRoute.getCrossConnections(),
                    source);

            routeSequenceDto.setPrimary(primarySequenceDto);
            if (secondaryRoute != null) {
                List<PathRouteObject> secondaryPathRoute = getPathRoutes(
                        secondaryRoute.getExplicitRouteObjects());
                RouteSequenceDto secondarySequenceDto = retrieveRoute(
                        secondaryPathRoute,
                        secondaryRoute.getCrossConnections(), source);
                routeSequenceDto.setSecondary(secondarySequenceDto);
            }
        }

        RouteSequenceDto rootRouteSequenceDto = new RouteSequenceDto();
        rootRouteSequenceDto.setLinkId(linkId);
        rootRouteSequenceDto.setTopologyRef(topologyRef.getValue());
        rootRouteSequenceDto.setRouteHopType(RouteHopType.fromClazz(Link.class));
        routeSequenceDto = getRightDirectionRouteSequence(routeSequenceDto, source);
        rootRouteSequenceDto.setSubSequenceDto(routeSequenceDto);
        return rootRouteSequenceDto;
    }

    private RouteSequenceDto retrieveRoute(List<PathRouteObject> pathRouteObjects,
            List<CrossConnections> crossConnections, String source) {
        RouteSequenceDto node = new RouteSequenceDto();
        RouteSequenceDto current = node;
//        List<CrossConnections> realXc = getRealCrossConnection(crossConnections);
//        node.getXcs().addAll(realXc);
        node.getXcIds()
                .addAll(crossConnections.stream().map(xc -> xc.getCrossConnectionId().getValue())
                        .collect(
                                Collectors.toList()));
        //assem there is only one explicitRouteObjects
//        List<PathRouteObject> pathRouteObjects = explicitRouteObjects.get(0).getPathRouteObject();
        for (PathRouteObject pro : pathRouteObjects) {
            ResourceType resourceType = pro.getResourceType();
            Class<?> clazz = resourceType.getImplementedInterface();

            if (clazz.isAssignableFrom(Tp.class)) {
                current = routeSequenceRetriever.retrieveTpHopRouteSequence(pro, current);
            } else if (clazz.isAssignableFrom(Link.class)) {
                log.debug("get route hop for the link");
                current = routeSequenceRetriever.retrievePhyLinkRouteSequence(pro, current);
            }
        }
//        node.getPrimary().setXcs(node.getXcs());
        node.getPrimary().setXcIds(node.getXcIds());
        return node.getPrimary();
    }

    @Override
    public RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject,
            String source,
            String destination) {
        log.debug("start to retrieve the route for the site link");
        LinkHop linkHop = ((Link) pathRouteObject.getResourceType()).getLinkHop();
        TopologyId topologyRef = pathRouteObject.getTopologyRef();
        String linkId = linkHop.getLinkRef().getValue();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink
                = netconfTopology.getSiteLink(linkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "invalid route data,please contact administrator");
        }
        Link1 sitePhysical = siteLink.getAugmentation(Link1.class);
        List<Route> siteLinkRoutes = sitePhysical.getSite()
                .getExplictRoute().getRoute();
        RouteSequenceDto routeSequenceDto = new RouteSequenceDto();
        for (Route route : siteLinkRoutes) {
            Primary primaryRoute = route.getPrimary();
            Secondary secondaryRoute = route.getSecondary();
            List<PathRouteObject> primaryPathRouteObjects = getPathRoutes(
                    primaryRoute.getExplicitRouteObjects());
            RouteSequenceDto primarySequenceDto = retrieveRoute(
                    primaryPathRouteObjects, primaryRoute.getCrossConnections(),
                    source, destination);

            routeSequenceDto.setPrimary(primarySequenceDto);
            if (secondaryRoute != null) {
                List<PathRouteObject> secondaryPathRoute = getPathRoutes(
                        secondaryRoute.getExplicitRouteObjects());
                RouteSequenceDto secondarySequenceDto = retrieveRoute(
                        secondaryPathRoute,
                        secondaryRoute.getCrossConnections(), source, destination);
                routeSequenceDto.setSecondary(secondarySequenceDto);
            }
        }

        RouteSequenceDto rootRouteSequenceDto = new RouteSequenceDto();
        rootRouteSequenceDto.setLinkId(linkId);
        rootRouteSequenceDto.setTopologyRef(topologyRef.getValue());
        rootRouteSequenceDto.setRouteHopType(RouteHopType.fromClazz(Link.class));
        routeSequenceDto = getRightDirectionRouteSequence(routeSequenceDto, source);
        rootRouteSequenceDto.setSubSequenceDto(routeSequenceDto);
        return rootRouteSequenceDto;
    }

    /**
     * get right route sequence for the route
     *
     * @param routeSequenceDto
     * @param source
     * @return
     */
    private RouteSequenceDto getRightDirectionRouteSequence(RouteSequenceDto routeSequenceDto,
            String source) {
        log.debug("get the right direction of the route sequence ");
        RouteSequenceDto primaryRouteSequenceDto = routeSequenceDto.getPrimary();
        RouteSequenceDto secondaryRouteSequenceDto = routeSequenceDto.getSecondary();
        RouteSequenceDto newSequence = new RouteSequenceDto();
        RouteSequenceDto rotatedPrimary = rotationRoute(source, primaryRouteSequenceDto);
        newSequence.setPrimary(rotatedPrimary);
        if (secondaryRouteSequenceDto != null) {
            RouteSequenceDto rotatedSecondary = rotationRoute(source, secondaryRouteSequenceDto);
            newSequence.setSecondary(rotatedSecondary);
        }
        return newSequence;
//        RouteSequenceDto current = routeSequenceDto.getPrimary();
//        //assumption: the start point is tp
//        String sourceTp = current.getTpId();
//        String sourceSite = PhysicalTpIdNamingRule.getSiteId(sourceTp);
//        if (sourceSite.equals(source)) {
//            return routeSequenceDto;
//        } else {
//            //rotate it direction
//            RouteSequenceDto head = routeSequenceDto.getPrimary();
//            RouteSequenceDto pre = null;
//            while (head != null) {
//                RouteSequenceDto next = head.getPrimary();
//                head.setPrimary(pre);
//
//                pre = head;
//                head = next;
//
//            }
//            RouteSequenceDto newSequence = new RouteSequenceDto();
//            newSequence.setPrimary(pre);
//            return newSequence;
//        }
//        return routeSequenceDto;

    }


    public RouteSequenceDto rotationRoute(String source, RouteSequenceDto current) {
        String sourceTp = current.getTpId();
        String sourceSite = PhysicalTpIdNamingRule.getSiteId(sourceTp);
        if (sourceSite.equals(source)) {
            return current;
        } else {
            //rotate it direction
            RouteSequenceDto head = current;
            RouteSequenceDto pre = null;
            while (head != null) {
                RouteSequenceDto next = head.getPrimary();
                head.setPrimary(pre);

                pre = head;
                head = next;

            }
            return pre;
        }
    }

    public RouteSequenceDto retrieveBriefRouteSequence(PathRouteObject pathRouteObject) {

        LinkHop linkHop = ((Link) pathRouteObject.getResourceType()).getLinkHop();
        TopologyId topologyRef = pathRouteObject.getTopologyRef();
        String linkId = linkHop.getLinkRef().getValue();
        log.debug("start to retrieve the brief route for the site link:{}", linkId);

//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink
//                = netconfTopology.getSiteLink(linkId);
//        if (Objects.isNull(siteLink)) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "invalid route data,please contact administrator");
//        }
//        List<CrossConnections> crossConnections = getSiteLinkRefCrossConnections(siteLink);
        RouteSequenceDto routeSequenceDto = new RouteSequenceDto();
        routeSequenceDto.setLinkId(linkId);
        routeSequenceDto.setTopologyRef(topologyRef.getValue());
        routeSequenceDto.setIsVirtual(true);
        routeSequenceDto.setRouteHopType(RouteHopType.fromClazz(Link.class));
//        routeSequenceDto.setXcs(new ArrayList<>());
        routeSequenceDto.setXcIds(new ArrayList<>());

        return routeSequenceDto;
    }

    /**
     * get site link ref cross connections
     *
     * @param siteLink
     * @return
     */
    private List<CrossConnections> getSiteLinkRefCrossConnections(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink) {
        log.debug("start get site link ref cross connections,the site link id :{}",
                siteLink.getLinkId().getValue());
        Site siteLinkPhysical = siteLink.getAugmentation(
                Link1.class).getSite();
        ExplictRoute explicitRoute = siteLinkPhysical.getExplictRoute();
        List<Route> routes = explicitRoute.getRoute();
        List<CrossConnections> refCrossConnections = new ArrayList<>();
        for (Route route : routes) {
            List<CrossConnections> routeXcs = getRouteCrossConnections(route);
            refCrossConnections.addAll(routeXcs);
        }
        return refCrossConnections;
    }

    /**
     * get route cross connections
     *
     * @param route
     * @return
     */
    private List<CrossConnections> getRouteCrossConnections(Route route) {
        log.debug("get route cross connection");
        Primary primary = route.getPrimary();
        Secondary secondary = route.getSecondary();
        List<Third> tertiaries = route.getThird();
        List<CrossConnections> crossConnections = new ArrayList<>();

        List<CrossConnections> primaryXcs = primary.getCrossConnections();
        crossConnections.addAll(primaryXcs);
        if (secondary != null) {
            List<CrossConnections> secondaryXcs = secondary.getCrossConnections();
            crossConnections.addAll(secondaryXcs);
        }
        if (!CollectionUtils.isEmpty(tertiaries)) {
            List<CrossConnections> tertiaryXcs = tertiaries.stream()
                    .flatMap(tertiary -> tertiary.getCrossConnections().stream()).collect(
                            Collectors.toList());
            crossConnections.addAll(tertiaryXcs);
        }
        List<CrossConnections> realCrossConnections = getRealCrossConnection(crossConnections);
        return realCrossConnections;
    }

//    private RouteSequenceDto retrieveRoute(List<ExplicitRouteObjects> explicitRouteObjects,
//            List<CrossConnections> crossConnections, String source,
//            String destination) {
//
//        RouteSequenceDto node = new RouteSequenceDto();
//        RouteSequenceDto current = node;
//        node.getXcs().addAll(crossConnections);
//        //assem there is only one explicitRouteObjects
//        List<PathRouteObject> pathRouteObjects = explicitRouteObjects.get(0).getPathRouteObject();
//        for (PathRouteObject pro : pathRouteObjects) {
//            ResourceType resourceType = pro.getResourceType();
//            Class<?> clazz = resourceType.getImplementedInterface();
//
//            if (clazz.isAssignableFrom(Tp.class)) {
//                current = routeSequenceRetriever.retrieveTpHopRouteSequence(pro, current);
//            } else if (clazz.isAssignableFrom(Link.class)) {
//                log.debug("get route hop for the link");
//                current = routeSequenceRetriever.retrieveLinkRouteSequence(pro, current, source,
//                        destination);
//            }
//        }
//        return node.getPrimary();
//    }


}
