package net.flex.dci.otn.controller.nms.properties.equip;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import net.flex.dci.otn.controller.nms.enums.EquipmentCategory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

/**
 * 2025/7/4
 *
 * @author musa
 * @version 1.0
 **/
@Data
public class EquipmentTypeConfiguration implements Serializable {

    private String description;

    private String version;

    private WaveDivisionMultiplexing waveDivisionMultiplexing;

    private MpoPortSupported mpoPortSupported;

    public boolean isWaveDivisionMultiplexing(EquipType equipType) {
        return waveDivisionMultiplexing.getEquipmentTypes().contains(equipType.name());
    }

    public boolean isMpoPortSupported(EquipType equipType) {
        List<String> mpoEquipTypes = mpoPortSupported.getEquipmentTypes().stream()
                .map(EquipmentType::getName).collect(
                        Collectors.toList());
        return mpoEquipTypes.contains(equipType.name());
    }

    public boolean isMuxMpoSupported(EquipType equipType) {
        List<String> mpoEquipTypes = mpoPortSupported.getEquipmentTypes().stream()
                .filter(equipmentType -> equipmentType.getCategory().equals(EquipmentCategory.MUX))
                .map(EquipmentType::getName).collect(
                        Collectors.toList());
        return mpoEquipTypes.contains(equipType.name());
    }
}
