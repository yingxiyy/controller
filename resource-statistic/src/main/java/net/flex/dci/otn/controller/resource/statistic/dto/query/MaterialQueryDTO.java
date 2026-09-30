package net.flex.dci.otn.controller.resource.statistic.dto.query;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.flex.dci.otn.controller.resource.statistic.enums.DeviceType;
import net.flex.dci.otn.controller.resource.statistic.enums.MaterialCategory;

/**
 * 2026/7/20
 *
 * @author musa
 * @version 1.0
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class MaterialQueryDTO extends BaseQueryDTO {

    private List<String> site;

    private List<String> deviceId;

    private DeviceType deviceType = DeviceType.ALL;

    private MaterialCategory materialCategory = MaterialCategory.CARD;


}
