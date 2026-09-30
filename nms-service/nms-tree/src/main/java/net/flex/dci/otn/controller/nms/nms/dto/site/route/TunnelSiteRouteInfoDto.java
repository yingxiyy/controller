package net.flex.dci.otn.controller.nms.nms.dto.site.route;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/12/16 16:50
 */
@Data
@Builder
public class TunnelSiteRouteInfoDto implements Serializable {

    private String tunnelId;

    private List<SiteRouteDto> siteRoutes;

}
