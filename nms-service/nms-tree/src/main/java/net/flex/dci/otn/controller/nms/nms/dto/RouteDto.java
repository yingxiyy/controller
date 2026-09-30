package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;

/**
 * @version 1.0
 * @date 2022/12/2 16:10
 */
@Data
@Builder
public class RouteDto implements Serializable {

    @Tolerate
    public RouteDto() {

    }

    private List<RouteSequence> routeSequences = new ArrayList<>();

    private List<CrossConnections> crossConnections = new ArrayList<>();

    private LinkedList<String> sites = new LinkedList<>();
}
