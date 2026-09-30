package net.flex.dci.otn.controller.nms.nms.component.site.route;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteInput.RouteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;

/**
 * @version 1.0
 * @date 2022/12/16 16:23
 */
public interface TunnelSiteRouteRetriever {

    List<RouteInfo> retrieveSiteRouteForTunnel(String tunnelId, String siteId, RouteType routeType);

}
