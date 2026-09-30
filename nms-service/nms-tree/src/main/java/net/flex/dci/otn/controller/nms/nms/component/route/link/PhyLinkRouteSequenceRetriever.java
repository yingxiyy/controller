package net.flex.dci.otn.controller.nms.nms.component.route.link;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.nms.enums.RouteHopType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/6/29 16:23
 */
@Component
@Slf4j
public class PhyLinkRouteSequenceRetriever extends AbstractRouteSequenceRetriever {

    @Override
    public RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject,
            String source,
            String destination) {
        log.debug("start to retrieve the phy link route ,source site is:{},dest site is:{}", source,
                destination);
        RouteSequenceDto routeSequenceDto = new RouteSequenceDto();
        TopologyId topologyId = pathRouteObject.getTopologyRef();
        routeSequenceDto.setTopologyRef(topologyId.getValue());
        Link linkHop = (Link) pathRouteObject.getResourceType();
        String linkId = linkHop.getLinkHop().getLinkRef().getValue();
        routeSequenceDto.setRouteHopType(RouteHopType.fromClazz(Link.class));
        routeSequenceDto.setLinkId(linkId);
        return routeSequenceDto;
    }

    @Override
    public RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject) {
        log.debug("start to retrieve the phy link route sequence");
        RouteSequenceDto routeSequenceDto = new RouteSequenceDto();
        TopologyId topologyId = pathRouteObject.getTopologyRef();
        routeSequenceDto.setTopologyRef(topologyId.getValue());
        Link linkHop = (Link) pathRouteObject.getResourceType();
        String linkId = linkHop.getLinkHop().getLinkRef().getValue();
        routeSequenceDto.setRouteHopType(RouteHopType.fromClazz(Link.class));
        routeSequenceDto.setLinkId(linkId);
        return routeSequenceDto;
    }

}


