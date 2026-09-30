package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

/**
 *
 * @version 1.0
 * @date 9/3/2025 1:23 PM
 */
@Data
@Builder
public class MpoTpInfo implements Serializable {

    private String tpId;

    private EquipType equipType;

}
