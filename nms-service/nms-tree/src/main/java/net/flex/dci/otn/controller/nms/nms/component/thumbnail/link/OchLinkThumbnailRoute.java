package net.flex.dci.otn.controller.nms.nms.component.thumbnail.link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailSequenceDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/12/12 10:04
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OchLinkThumbnailRoute extends AbstractLinkThumbnailRoute {

    private final OchLinkDao ochLinkDao;


    @Override
    public ThumbnailRouteDto getThumbnailSequence(String id) {
        log.info("get och link thumbnail route for the och link id :{}", id);
        log.debug("get och link thumbnail route sequence ,link id is:{}", id);
        Link ochLink = ochLinkDao.getOchLinkByLinkId(id);
        if (null == ochLink) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the och link is not existed");
        }
        Och ochPhysical = ochLink.getAugmentation(
                Link1.class).getOch();
        ExplictRoute explictRoute = ochPhysical.getExplictRoute();
        ThumbnailSequenceDto thumbnailSequenceDto = retrieveThumbnailRoute(explictRoute);
        ThumbnailRouteDto thumbnailRouteDto = retrieveThumbnailRouteSequence(thumbnailSequenceDto);
        return thumbnailRouteDto;
    }

    /**
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
        return thumbnailSequenceDtoList.get(0);
    }

    private ThumbnailSequenceDto retrieveThumbnailRoute(Route route) {
        log.trace("retrieve the och link thumbnail route :{}", route);
        ThumbnailSequenceDto thumbnailSequenceDto = new ThumbnailSequenceDto();
        ThumbnailSequenceDto primaryThumbnail = retrieveThumbnailDetailRoute(
                route.getPrimary().getExplicitRouteObjects());
        thumbnailSequenceDto.setPrimary(primaryThumbnail);
        if (route.getSecondary() != null) {
            ThumbnailSequenceDto secondary = retrieveThumbnailDetailRoute(
                    route.getSecondary().getExplicitRouteObjects());
            thumbnailSequenceDto.setSecondary(secondary);
        }
        if (!CollectionUtils.isEmpty(route.getThird())) {
            ThumbnailSequenceDto tertiary = retrieveTertiaryThumbnailDetailRoute(route.getThird());
            thumbnailSequenceDto.setTertiary(tertiary);
        }
        return thumbnailSequenceDto;
    }

    private ThumbnailSequenceDto retrieveTertiaryThumbnailDetailRoute(List<Third> thirds) {
        log.debug("retrieve tertiary thumbnail route detail");
        //assemble the third have only one
        Third third = thirds.get(0);
        List<ExplicitRouteObjects> explicitRouteObjects = third.getExplicitRouteObjects();
        return retrieveThumbnailDetailRoute(explicitRouteObjects);
    }

    private ThumbnailSequenceDto retrieveThumbnailDetailRoute(
            List<ExplicitRouteObjects> explicitRouteObjects) {
        log.debug("retrieve the detail thumbnail route ");
        ThumbnailSequenceDto thumbnailSequenceDto = new ThumbnailSequenceDto();
        ThumbnailSequenceDto current = thumbnailSequenceDto;
        List<PathRouteObject> pathRouteObjects = explicitRouteObjects.get(0).getPathRouteObject();
        Set<String> visitedSiteNodes = new HashSet<>();
        for (PathRouteObject pathRouteObject : pathRouteObjects) {
            ResourceType resourceType = pathRouteObject.getResourceType();
            Class<?> clazz = resourceType.getImplementedInterface();
            if (clazz.isAssignableFrom(Tp.class)) {
                Tp tp = (Tp) resourceType;
                TpHop tpHop = tp.getTpHop();
                String refSiteId = tpHop.getSiteRef().getValue();
                if (!visitedSiteNodes.contains(refSiteId)) {
                    ThumbnailSequenceDto siteThumbnailSequence = new ThumbnailSequenceDto();
                    siteThumbnailSequence.setNodeId(refSiteId);
                    siteThumbnailSequence.setRefClazz(clazz);
                    current.setPrimary(siteThumbnailSequence);
                    current = siteThumbnailSequence;
                    visitedSiteNodes.add(refSiteId);
                }

            } else if (clazz.isAssignableFrom(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class)) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link link = (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) resourceType;
                LinkHop linkHop = link.getLinkHop();
                String topologyRef = linkHop.getTopologyRef().getValue();
                String linkId = linkHop.getLinkRef().getValue();
                if (topologyRef.equals(Constants.SITE_TOPO_KEY) && SiteLinkIdNamingRule.isSiteLink(
                        linkId)) {
                    ThumbnailSequenceDto linkThumbnailSequence = new ThumbnailSequenceDto();
                    linkThumbnailSequence.setLinkId(linkId);
                    linkThumbnailSequence.setRefClazz(clazz);
                    current.setPrimary(linkThumbnailSequence);
                    current = linkThumbnailSequence;
                }
            }

        }

        return thumbnailSequenceDto.getPrimary();
    }

    @Override
    public String getLINK_TYPE() {
        return Constants.OCH_LINK;
    }
}
