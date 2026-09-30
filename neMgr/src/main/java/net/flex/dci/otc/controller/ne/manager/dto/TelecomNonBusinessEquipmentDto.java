package net.flex.dci.otc.controller.ne.manager.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * @version 1.0
 * @date 11/21/2023 4:33 PM
 */
@Data
@Builder
public class TelecomNonBusinessEquipmentDto implements Serializable {

    //in config but not in real ne nonBusiness equip
    private List<Equipments> inConfigEquipments;

    //not in config but in real ne nonBusiness equip
    private List<Equipments> notInConfigEquipments;

    private List<MissAlignEquipmentDto> missAlignEquipmentDtos;
}
