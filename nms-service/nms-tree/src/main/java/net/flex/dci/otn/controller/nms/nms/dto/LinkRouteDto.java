package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Third;

/**
 * @version 1.0
 * @date 2022/12/2 11:06
 */
@Data
@Builder
public class LinkRouteDto implements Serializable {


    private Primary primary;

    private Secondary secondary;

    private List<Third> third;

}
