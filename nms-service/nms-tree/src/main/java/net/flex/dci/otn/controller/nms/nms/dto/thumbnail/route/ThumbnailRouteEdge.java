package net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.nms.nms.enums.ThumbnailEdgeSiteType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.ResourceType;

/**
 * @version 1.0
 * @date 2022/12/24 19:41
 */
@Data
@Builder
public class ThumbnailRouteEdge implements Serializable {

    private String siteId;

    private ResourceType edge;

    private ThumbnailEdgeSiteType edgeSiteType;
}
