package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.mongo.dto.FilterItem;

/**
 * @version 1.0
 * @date 2023/2/1 11:24
 */
@Data
@Builder
public class FilterDto implements Serializable {

    private List<FilterItem> directFilterItems;

    private List<FilterItem> additionalFilterItems;
}
