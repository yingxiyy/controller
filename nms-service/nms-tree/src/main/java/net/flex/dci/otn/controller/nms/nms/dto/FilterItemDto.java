package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.common.enums.FilterQuerySelector;

/**
 * @version 1.0
 * @date 2022/5/27 10:41
 */
@Data
@Builder
public class FilterItemDto implements Serializable {

    private String filter;

    private String filterItem;

    private FilterQuerySelector.FilterLogicalOp logicalOp;

    private FilterQuerySelector.FilterOperation filterOp;

    private Boolean isAdditional;
}
