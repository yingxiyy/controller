package net.flex.dci.otc.controller.ne.manager.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * @version 1.0
 * @date 7/17/2023 2:39 PM
 */
@Data
@Builder
public class MissAlignEquipmentDto implements Serializable {

    private String equipmentId;

    private Equipments designEquipment;

    private Equipments realEquipment;
}
