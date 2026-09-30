package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;

/**
 * 2026/9/23
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtsLinkRamanInfo implements Serializable {

    private String name;

    private String neId;

    private String crossConnectionId;

    private CrossConnections amplifierXc;
}
