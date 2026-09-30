package net.flex.dci.otn.controller.nms.properties.equip;

import java.io.Serializable;
import lombok.Data;
import net.flex.dci.otn.controller.nms.enums.EquipmentCategory;

/**
 *
 * @version 1.0
 * @date 9/3/2025 1:42 PM
 */
@Data
public class EquipmentType implements Serializable {

    private String name;

    private EquipmentCategory category;
}
