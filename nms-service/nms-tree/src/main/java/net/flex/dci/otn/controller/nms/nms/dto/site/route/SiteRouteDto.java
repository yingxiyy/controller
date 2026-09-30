package net.flex.dci.otn.controller.nms.nms.dto.site.route;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/12/16 16:11
 */
@Data
@Builder
public class SiteRouteDto implements Serializable {

    private String siteId;


    private SiteRouteDetailDto primary;

    private SiteRouteDetailDto secondary;


}
