package net.flex.dci.otn.controller.nms.nms.dto.site.route;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/12/16 16:14
 */
@Data
@Builder
public class SiteRouteDetailSequenceDto implements Serializable {

    private String refId;

    private Long sequences;

}
