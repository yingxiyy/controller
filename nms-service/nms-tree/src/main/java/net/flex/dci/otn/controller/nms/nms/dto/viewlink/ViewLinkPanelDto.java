package net.flex.dci.otn.controller.nms.nms.dto.viewlink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @version 1.0
 * @date 1/22/2024 2:21 PM
 */
@Data
@Builder
public class ViewLinkPanelDto implements Serializable {

    private String plane;

    private Link viewLink;
}
