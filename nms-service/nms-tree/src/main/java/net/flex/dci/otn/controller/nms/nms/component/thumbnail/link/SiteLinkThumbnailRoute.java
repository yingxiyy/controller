package net.flex.dci.otn.controller.nms.nms.component.thumbnail.link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailSequenceDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * site link thumbnail route
 *
 * @version 1.0
 * @date 2022/12/6 16:52
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkThumbnailRoute extends AbstractLinkThumbnailRoute {

    private final SiteLinkDao siteLinkDao;

    @Override
    public ThumbnailRouteDto getThumbnailSequence(String id) {
        log.info("get site like thumbnail for the site link,link id is:{}", id);
        log.debug("get site link thumbnail sequence start,link id is :{}", id);
        Link siteLink = siteLinkDao.getSiteLinkById(id);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the site link is not existed");
        }
        Site siteLinkPhysical = siteLink.getAugmentation(Link1.class).getSite();
        ExplictRoute explictRoute = siteLinkPhysical.getExplictRoute();
        ThumbnailSequenceDto thumbnailSequenceDto = retrieveThumbnailRoute(explictRoute);
        ThumbnailRouteDto thumbnailRouteDto = retrieveThumbnailRouteSequence(thumbnailSequenceDto);
        return thumbnailRouteDto;
    }


    /**
     * retrieve thumbnail sequence dto
     *
     * @param explictRoute
     * @return
     */
    private ThumbnailSequenceDto retrieveThumbnailRoute(ExplictRoute explictRoute) {
        List<Route> routeList = explictRoute.getRoute();
        List<ThumbnailSequenceDto> thumbnailSequenceDtoList = new ArrayList<>();
        routeList.forEach(route -> {
            ThumbnailSequenceDto thumbnailSequenceDto = retrieveThumbnailRoute(route);
            thumbnailSequenceDtoList.add(thumbnailSequenceDto);
        });
        //assumption
        return thumbnailSequenceDtoList.get(0);
    }

    /**
     * create retrieve thumbnailRoute
     *
     * @param route
     * @return
     */
    private ThumbnailSequenceDto retrieveThumbnailRoute(Route route) {
        ThumbnailSequenceDto thumbnailSequenceDto = new ThumbnailSequenceDto();
        ThumbnailSequenceDto primaryThumbnail = retrieveThumbnailDetailRoute(
                route.getPrimary().getExplicitRouteObjects());
        thumbnailSequenceDto.setPrimary(primaryThumbnail);
        if (route.getSecondary() != null) {
            log.debug("get secondary thumbnail route sequence");
            Secondary secondary = route.getSecondary();
            ThumbnailSequenceDto secondaryThumbnail = retrieveThumbnailDetailRoute(
                    secondary.getExplicitRouteObjects());
            thumbnailSequenceDto.setSecondary(secondaryThumbnail);
        }
        if (!CollectionUtils.isEmpty(route.getThird())) {
            log.debug("get third thumbnail route sequence ");
            ThumbnailSequenceDto tertiaryThumbnail = retrieveTertiaryThumbnailDetailRoute(
                    route.getThird());
            thumbnailSequenceDto.setTertiary(tertiaryThumbnail);
        }

        return thumbnailSequenceDto;
    }

    /**
     * retrieve Tertiary
     *
     * @param thirds
     * @return
     */
    private ThumbnailSequenceDto retrieveTertiaryThumbnailDetailRoute(List<Third> thirds) {
        log.debug("retrieve tertiary thumbnail detail route size:{}", thirds.size());
        //assemble only have one tertiary route
        Third third = thirds.get(0);
        List<ExplicitRouteObjects> tertiaryExplicitRouteObjects = third.getExplicitRouteObjects();
        ThumbnailSequenceDto tertiaryThumbnailSequence = retrieveThumbnailDetailRoute(
                tertiaryExplicitRouteObjects);
        return tertiaryThumbnailSequence;
    }

    /**
     * @param explicitRouteObjects
     * @return
     */
    private ThumbnailSequenceDto retrieveThumbnailDetailRoute(
            List<ExplicitRouteObjects> explicitRouteObjects) {
        log.debug("retrieve thumbnail detail route");
        ThumbnailSequenceDto thumbnailSequenceDto = new ThumbnailSequenceDto();
        ThumbnailSequenceDto currentSequence = thumbnailSequenceDto;
        Set<String> siteNodeSet = new HashSet<>();
        //assumption the explicit route only have one
        List<PathRouteObject> pathRouteObjects = explicitRouteObjects.get(
                0).getPathRouteObject();
        for (PathRouteObject pathRouteObject : pathRouteObjects) {
            ResourceType resourceType = pathRouteObject.getResourceType();
            Class<?> clazz = resourceType.getImplementedInterface();
            if (clazz.isAssignableFrom(Tp.class)) {
                TpHop tpHop = ((Tp) resourceType).getTpHop();
                String siteNodeId = tpHop.getSiteRef().getValue();
                if (!siteNodeSet.contains(siteNodeId)) {
                    ThumbnailSequenceDto siteThumbnailSequenceDto = new ThumbnailSequenceDto();
                    siteThumbnailSequenceDto.setNodeId(siteNodeId);
                    siteThumbnailSequenceDto.setRefClazz(Tp.class);
                    siteNodeSet.add(siteNodeId);
                    currentSequence.setPrimary(siteThumbnailSequenceDto);
                    currentSequence = siteThumbnailSequenceDto;

                }

            } else if (clazz.isAssignableFrom(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class)) {
                LinkHop linkHop = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) resourceType).getLinkHop();
                String linkId = linkHop.getLinkRef().getValue();
                if (PhysicalLinkIdNamingRule.isOtsLink(linkId)) {
                    ThumbnailSequenceDto phyLinkThumbnailSequenceDto = new ThumbnailSequenceDto();
                    phyLinkThumbnailSequenceDto.setLinkId(linkId);
                    phyLinkThumbnailSequenceDto.setRefClazz(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class);
                    phyLinkThumbnailSequenceDto.setTopologyRef(linkHop.getTopologyRef());
                    currentSequence.setPrimary(phyLinkThumbnailSequenceDto);
                    currentSequence = phyLinkThumbnailSequenceDto;
                }
            }
        }
        return thumbnailSequenceDto.getPrimary();
    }

    @Override
    public String getLINK_TYPE() {
        return Constants.SITE_LINK;
    }
}
