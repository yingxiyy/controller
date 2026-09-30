package net.flex.dci.otn.controller.nms.nms.component.site.route;

import java.util.List;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDto;
import net.flex.dci.otn.controller.nms.nms.enums.TunnelSiteType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteInput.RouteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;

/**
 * @version 1.0
 * @date 2022/12/19 14:30
 */
public interface SiteRouteInfoConstructor {

    /**
     * construct site route info
     *
     * @param siteRoute
     * @param terminalSite
     * @return
     */
    List<RouteInfo> constructSiteRouteInfo(SiteRouteDto siteRoute,
            TunnelSiteType siteType,
            RouteType routeType);
}
