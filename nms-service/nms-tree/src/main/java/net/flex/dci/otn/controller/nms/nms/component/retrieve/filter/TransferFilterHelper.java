package net.flex.dci.otn.controller.nms.nms.component.retrieve.filter;

import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otn.controller.nms.enums.AdditionalProperty;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;

/**
 * @version 1.0
 * @date 2023/2/2 11:26
 */
public interface TransferFilterHelper {

    FilterItem transferFilterItem(FilterItem filterItem, AdditionalProperty additionalProperty,
            NMSConvertType nmsConvertType);
}
