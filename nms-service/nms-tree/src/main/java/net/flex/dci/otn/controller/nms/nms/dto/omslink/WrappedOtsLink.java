package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @version 1.0
 * @date 8/20/2025 2:21 PM
 */
@Data
@Builder
public class WrappedOtsLink implements Serializable {

    private Boolean aligned;

    private Link link;
}
