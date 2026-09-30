package net.flex.dci.otn.controller.nms.nms.dto.route;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

/**
 * 2026/1/19
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class MpoPortInfo implements Serializable {

    private String tpId;

    private String equipmentId;

    private TerminationPoint tp;

}
