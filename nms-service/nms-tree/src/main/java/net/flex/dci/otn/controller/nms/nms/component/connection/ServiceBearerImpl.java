package net.flex.dci.otn.controller.nms.nms.component.connection;

import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath.PRIMARY;
import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath.SECONDARY;
import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath.THIRD;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.dto.link.ProtectedLinkSiteLinkInfo;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 9/15/2025 3:07 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceBearerImpl implements ServiceBearer {

    private final NetconfTopology netconfTopology;

    /**
     * get the site link ref tunnel
     *
     * @param tunnelId
     * @param siteLinkId
     * @return
     */
    @Override
    public ApsPath getProtectedTunnelSiteLinkLocation(String tunnelId, String siteLinkId,
            Link ochLink) {
        log.debug("get siteLink:{}  location path", siteLinkId);
        ProtectedLinkSiteLinkInfo protectedLinkSiteLinkInfo = getProtectedTunnelSiteLinkInfoRefOch(
                ochLink);
        ApsPath apsPath = PRIMARY;
        if (protectedLinkSiteLinkInfo.getPrimarySiteLinkIds().contains(siteLinkId)) {
            apsPath = PRIMARY;
        } else if (protectedLinkSiteLinkInfo.getSecondarySiteLinkIds().contains(siteLinkId)) {
            apsPath = SECONDARY;
        } else if (protectedLinkSiteLinkInfo.getTertiarySiteLinkIds().contains(siteLinkId)) {
            apsPath = THIRD;
        }
        return apsPath;
    }

    private ProtectedLinkSiteLinkInfo getProtectedTunnelSiteLinkInfoRefOch(Link ochLink) {
        log.debug("get protected och link :{} info", ochLink.getLinkId().getValue());
        Och ochAttribute = ochLink.getAugmentation(Link1.class).getOch();
        Route route = ochAttribute.getExplictRoute().getRoute().get(0);
        Primary primary = route.getPrimary();
        Secondary secondary = route.getSecondary();
        Third third = route.getThird() == null ? null : route.getThird().get(0);

        List<String> primarySiteLinkIds = getRouteSiteLinkIds(
                primary.getExplicitRouteObjects().get(0));
        List<String> secondarySiteLinkIds = getRouteSiteLinkIds(
                secondary.getExplicitRouteObjects().get(0));
        List<String> tertiarySiteLinkIds = third == null ? new ArrayList<>() : getRouteSiteLinkIds(
                third.getExplicitRouteObjects().get(0));

        return ProtectedLinkSiteLinkInfo.builder().primarySiteLinkIds(primarySiteLinkIds)
                .secondarySiteLinkIds(secondarySiteLinkIds).tertiarySiteLinkIds(tertiarySiteLinkIds)
                .build();
    }

    private List<String> getRouteSiteLinkIds(ExplicitRouteObjects routeObjects) {
        List<PathRouteObject> pathRouteObjects = routeObjects.getPathRouteObject();
        List<String> siteLinkIds = new ArrayList<>();
        for (PathRouteObject pathRouteObject : pathRouteObjects) {
            ResourceType resourceType = pathRouteObject.getResourceType();
            if (resourceType instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {

                String linkId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                        .tunnel.types.rev180515.resource.type.resource.type.Link) resourceType)
                        .getLinkHop().getLinkRef().getValue();
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    siteLinkIds.add(linkId);
                }
            }
        }

        return siteLinkIds;
    }
}
