package net.flex.dci.otn.controller.nms.nms.component.route.link;

import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;

/**
 * @version 1.0
 * @date 2022/6/29 16:22
 */
public interface IRouteSequenceRetriever {

    default RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject,
            String source) {
        return null;
    }


    default RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject,
            RouteSequenceDto routeSequenceDto, String source,
            String destination) {
        return null;
    }

    default RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject,
            String source,
            String destination) {
        return null;
    }

    default RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject) {
        return null;
    }

    default RouteSequenceDto retrieveTpHopRouteSequence(PathRouteObject pathRouteObject,
            RouteSequenceDto routeSequenceDto) {
        return null;
    }
}
