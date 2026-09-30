package net.flex.dci.otn.controller.nms.nms.component.route.retriever;

import java.util.List;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

/**
 * @version 1.0
 * @date 2022/6/29 11:06
 */
public interface RouteRetriever {

    List<RouteInfo> retrieverRouteInfo(RouteInfoDto routeDto);

    RouteSequenceDto retrieverRouteSequence(Route route, String sourceSite, String destSite);
}
