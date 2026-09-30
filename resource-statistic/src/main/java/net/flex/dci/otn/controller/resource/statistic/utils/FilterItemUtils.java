package net.flex.dci.otn.controller.resource.statistic.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otn.controller.resource.statistic.dto.FilterCondition;
import org.springframework.util.CollectionUtils;

/**
 * 2026/1/29
 *
 * @author musa
 * @version 1.0
 **/
public class FilterItemUtils {

    public static List<FilterItem> getFilterItems(List<FilterCondition> filterConditions) {
        if (CollectionUtils.isEmpty(filterConditions)) {
            return new ArrayList<>();
        }
        List<FilterItem> filterItems = filterConditions.stream()
                .map(FilterItemUtils::convert2FilterItem)
                .collect(Collectors.toList());
        return filterItems;
    }

    private static FilterItem convert2FilterItem(FilterCondition filterCondition) {
        return FilterItem.builder()
                .filter(filterCondition.getField())
                .filterItem((String) filterCondition.getValue())
                .filterOp(filterCondition.getOperation())
                .build();

    }

}
