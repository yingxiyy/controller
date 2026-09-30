package net.flex.dci.otn.controller.nms.nms.dto.route;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;

/**
 * 2026/1/17
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ExternalRouteDetailsDto implements Serializable {

    private List<PathRouteObject> pathRouteObjects;

    private List<CrossConnections> crossConnections;

}
