package net.flex.dci.otn.controller.nms.nms.component.thumbnail.link;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.nms.nms.component.thumbnail.link.TunnelThumbnailRoute.PathRouteObjects.PathRouteObjectsBuilder;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailSequenceDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * tunnel thumbnail route
 *
 * @version 1.0
 * @date 2022/12/6 16:52
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelThumbnailRoute extends AbstractLinkThumbnailRoute {

    private final TunnelDao tunnelDao;

    private final OchLinkDao ochLinkDao;

    private final OchLinkWithoutSiteLinkRoute withoutSiteLinkRoute;

    @Override
    public ThumbnailRouteDto getThumbnailSequence(String id) {
        log.info("start to get thumbnail route sequence for the tunnel,tunnel id is:{}", id);
        log.debug("get thumbnail route sequence for tunnel,tunnel id :{}", id);
        Tunnel refTunnel = tunnelDao.getTunnelById(id);
        if (refTunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the tunnel is not existed");
        }
        //adding by yyx, some tunnel is running over ochLink, and ochLink hasn't related with siteLink, checking branch
        ThumbnailRouteDto thumbnailRouteDto = null;
        if (refTunnel.getSupportingLink() != null && !refTunnel.getSupportingLink().isEmpty()) {
            LinkId ochLinkId = refTunnel.getSupportingLink().get(0).getLinkRef();
            thumbnailRouteDto = withoutSiteLinkRoute.checkingThumbnailRouteWithoutSiteLink(ochLinkDao.getOchLinkByLinkId(ochLinkId.getValue()));
        }
        if (thumbnailRouteDto == null) {
            ThumbnailSequenceDto thumbnailSequenceDto = retrieveThumbnailRouteForTunnel(refTunnel);
            thumbnailRouteDto = retrieveThumbnailRouteSequence(thumbnailSequenceDto);
        }
        return thumbnailRouteDto;
    }


    /**
     * retrieve the thumbnail route for the tunnel
     *
     * @param refTunnel
     * @return
     */
    private ThumbnailSequenceDto retrieveThumbnailRouteForTunnel(Tunnel refTunnel) {
        log.debug("start to retrieve the thumbnail ");
        ExplictRoute explictRoute = refTunnel.getExplictRoute();
        List<Route> routeList = explictRoute.getRoute();
        List<ThumbnailSequenceDto> thumbnailSequenceDtos = new ArrayList<>();
        for (Route route : routeList) {
            ThumbnailSequenceDto thumbnailSequenceDto = retrieveThumbnailRoute(route);
            thumbnailSequenceDtos.add(thumbnailSequenceDto);
        }
        //assum there only have one route
        return thumbnailSequenceDtos.get(0);
    }

    /**
     * retrieve route detail
     *
     * @param route
     * @return
     */
    private ThumbnailSequenceDto retrieveThumbnailRoute(Route route) {
        ThumbnailSequenceDto thumbnailSequenceDto = new ThumbnailSequenceDto();
        Primary primary = route.getPrimary();
        Secondary secondary = route.getSecondary();
        List<Third> tertiary = route.getThird();
        ThumbnailSequenceDto primarySequenceDto = retrieveThumbnailDetailRoute(
                primary.getExplicitRouteObjects());
        thumbnailSequenceDto.setPrimary(primarySequenceDto.getPrimary());
        if (primarySequenceDto.getSecondary() != null) {
            thumbnailSequenceDto.setSecondary(primarySequenceDto.getSecondary());
        }
        //tertiary route show
        if (primarySequenceDto.getTertiary() != null) {
            thumbnailSequenceDto.setTertiary(primarySequenceDto.getTertiary());
        }
        if (secondary != null) {
            ThumbnailSequenceDto secondarySequenceDto = retrieveThumbnailDetailRoute(
                    secondary.getExplicitRouteObjects());
            thumbnailSequenceDto.setSecondary(secondarySequenceDto);
        }
        if (!CollectionUtils.isEmpty(tertiary)) {
            ThumbnailSequenceDto tertiarySequenceDto = retrieveTertiaryThumbnailDetailRoute(
                    tertiary);
            thumbnailSequenceDto.setTertiary(tertiarySequenceDto);
        }

        return thumbnailSequenceDto;
    }

    private ThumbnailSequenceDto retrieveTertiaryThumbnailDetailRoute(List<Third> tertiary) {
        log.debug("retrieve tertiary thumbnail detail route");
        Third third = tertiary.get(0);
        List<ExplicitRouteObjects> routeObjects = third.getExplicitRouteObjects();
        return retrieveThumbnailDetailRoute(routeObjects);
    }

    /**
     * retrieve detail
     *
     * @param explicitRouteObjects
     * @return
     */
    private ThumbnailSequenceDto retrieveThumbnailDetailRoute(
            List<ExplicitRouteObjects> explicitRouteObjects) {
        log.debug("retrieve detail for the explicit route object");
        ThumbnailSequenceDto thumbnailSequenceDto = new ThumbnailSequenceDto();
        List<PathRouteObject> pathRoutes = explicitRouteObjects.get(
                0).getPathRouteObject();
        PathRouteObjects pathRouteObjects = spreadRouteObjectsToOch(pathRoutes);
        ThumbnailSequenceDto primarySequenceDto = retrieveThumbnailSequence(
                pathRouteObjects.primary);
        ThumbnailSequenceDto secondarySequenceDto = retrieveThumbnailSequence(
                pathRouteObjects.secondary);
        ThumbnailSequenceDto tertiarySequenceDto = retrieveThumbnailSequence(
                pathRouteObjects.tertiary);
        thumbnailSequenceDto.setPrimary(primarySequenceDto);
        thumbnailSequenceDto.setSecondary(secondarySequenceDto);
        thumbnailSequenceDto.setTertiary(tertiarySequenceDto);
        return thumbnailSequenceDto;
    }

    private ThumbnailSequenceDto retrieveThumbnailSequence(List<PathRouteObject> pathRouteObjects) {
        Set<String> siteNodeSet = new HashSet<>();
        ThumbnailSequenceDto thumbnailSequenceDto = new ThumbnailSequenceDto();
        ThumbnailSequenceDto current = thumbnailSequenceDto;
        for (PathRouteObject pathRouteObject : pathRouteObjects) {
            ResourceType resourceType = pathRouteObject.getResourceType();
            Class<?> clazz = resourceType.getImplementedInterface();
            if (clazz.isAssignableFrom(Tp.class)) {
                TpHop tpHop = ((Tp) resourceType).getTpHop();
                String siteId = tpHop.getSiteRef().getValue();
                if (!siteNodeSet.contains(siteId)) {
                    ThumbnailSequenceDto siteSequenceDto = new ThumbnailSequenceDto();
                    siteSequenceDto.setRefClazz(clazz);
                    siteSequenceDto.setNodeId(siteId);
                    siteNodeSet.add(siteId);
                    current.setPrimary(siteSequenceDto);
                    current = siteSequenceDto;
                }
            } else if (clazz.isAssignableFrom(Link.class)) {
                LinkHop linkHop = ((Link) resourceType).getLinkHop();
                String linkId = linkHop.getLinkRef().getValue();
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    ThumbnailSequenceDto siteLinkSequenceDto = new ThumbnailSequenceDto();
                    siteLinkSequenceDto.setLinkId(linkId);
                    siteLinkSequenceDto.setRefClazz(clazz);
                    current.setPrimary(siteLinkSequenceDto);
                    current = siteLinkSequenceDto;
                }
            }
        }
        return thumbnailSequenceDto.getPrimary();
    }


    /**
     * spread the route objects to och level
     *
     * @param pathRoutes
     * @return
     */
    private PathRouteObjects spreadRouteObjectsToOch(List<PathRouteObject> pathRoutes) {
        List<PathRouteObject> primaryRouteObjects = new ArrayList<>();
        List<PathRouteObject> secondaryRouteObjects = new ArrayList<>();
        List<PathRouteObject> tertiaryRouteObjects = new ArrayList<>();
        for (PathRouteObject pathRouteObject : pathRoutes) {
            ResourceType resourceType = pathRouteObject.getResourceType();
            if (resourceType instanceof Tp) {
                primaryRouteObjects.add(pathRouteObject);
            } else if (resourceType instanceof Link) {
                //add detail for the och link route detail
                PathRouteObjects pathRouteObjects = getOchRefDetailRouteObjects(resourceType);
                primaryRouteObjects.addAll(pathRouteObjects.primary);
                if (pathRouteObjects.secondary != null) {
                    secondaryRouteObjects.addAll(pathRouteObjects.secondary);
                }
                if (pathRouteObjects.tertiary != null) {
                    tertiaryRouteObjects.addAll(pathRouteObjects.tertiary);
                }
            }
        }
        return PathRouteObjects.builder().primary(primaryRouteObjects)
                .secondary(secondaryRouteObjects)
                .tertiary(tertiaryRouteObjects)
                .build();
    }

    private PathRouteObjects getOchRefDetailRouteObjects(ResourceType resourceType) {
        LinkHop linkHop = ((Link) resourceType).getLinkHop();
        String refOchLinkId = linkHop.getLinkRef().getValue();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link ochLink = ochLinkDao.getOchLinkByLinkId(
                refOchLinkId);
        Och ochPhysical = ochLink.getAugmentation(Link1.class).getOch();
        List<Route> ochRoutes = ochPhysical.getExplictRoute().getRoute();
        //todo: to support ochp route work
        Route route = ochRoutes.get(0);
        PathRouteObjectsBuilder pathRouteObjects = PathRouteObjects.builder()
                .primary(route.getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject());
        if (route.getSecondary() != null) {
            pathRouteObjects.secondary(
                    route.getSecondary().getExplicitRouteObjects().get(0).getPathRouteObject());
        }
        if (!CollectionUtils.isEmpty(route.getThird())) {
            pathRouteObjects.tertiary(
                    route.getThird().get(0).getExplicitRouteObjects().get(0).getPathRouteObject());
        }
        return pathRouteObjects.build();
    }

    @Override
    public String getLINK_TYPE() {
        return Constants.SITE_TUNNEL;
    }

    @Data
    @Builder
    public static class PathRouteObjects implements Serializable {

        private List<PathRouteObject> primary;

        private List<PathRouteObject> secondary;

        private List<PathRouteObject> tertiary;
    }
}
