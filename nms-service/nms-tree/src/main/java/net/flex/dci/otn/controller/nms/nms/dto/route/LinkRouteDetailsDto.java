package net.flex.dci.otn.controller.nms.nms.dto.route;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.nms.nms.dto.RouteDetailDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;

/**
 * 2025/6/15
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class LinkRouteDetailsDto implements Serializable {

    private List<CrossConnections> refCrossConnections;

    private RouteDetailDto routeDetailDto;
}
