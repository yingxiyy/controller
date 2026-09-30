package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Third;

/**
 * 2025/6/15
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ThirdRouteDto implements Serializable {

    private List<Third> routeDetails;
}
