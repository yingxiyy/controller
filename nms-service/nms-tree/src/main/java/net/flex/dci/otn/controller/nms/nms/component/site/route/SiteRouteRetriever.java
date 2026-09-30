package net.flex.dci.otn.controller.nms.nms.component.site.route;

import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.TunnelSiteRouteInfoDto;

/**
 * @version 1.0
 * @date 2022/12/19 11:34
 */
public interface SiteRouteRetriever {

    TunnelSiteRouteInfoDto retrieveTunnelSiteRoute(String tunnelId,
            RouteSequenceDto routeSequenceDto);
}
