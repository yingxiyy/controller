package net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.sequence.RouteSequence;

/**
 * @version 1.0
 * @date 2022/12/7 15:44
 */
@Data
@Builder
public class ThumbnailRouteDetailDto implements Serializable {

    private List<RouteSequence> routeSequences;

}
