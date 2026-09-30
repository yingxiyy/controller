package net.flex.dci.otn.controller.resource.statistic.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.flex.dci.otc.common.enums.FilterQuerySelector;

/**
 * 2026/1/29
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FilterCondition implements Serializable {

    private String field;

    private Object value;

    private FilterQuerySelector.FilterOperation operation;

}
