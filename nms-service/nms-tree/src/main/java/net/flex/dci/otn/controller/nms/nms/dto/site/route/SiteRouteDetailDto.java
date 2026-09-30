package net.flex.dci.otn.controller.nms.nms.dto.site.route;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/12/19 9:50
 */
@Data
@Builder
public class SiteRouteDetailDto implements Serializable {

    private String siteId;

    /**
     * detail site route
     */
    private List<SiteRouteDetailSequenceDto> siteRouteDetails;

    private List<String> crossConnections;
}
