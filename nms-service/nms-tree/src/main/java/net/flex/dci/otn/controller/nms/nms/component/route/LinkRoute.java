package net.flex.dci.otn.controller.nms.nms.component.route;

import java.util.List;
import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;

/**
 * @version 1.0
 * @date 2022/5/23 14:16
 */
public interface LinkRoute {

//    List<RouteInfo> getRoute(GetRouteInput input) throws Exception;

    List<RouteInfo> getRouteById(String id) throws CommonException;
}
