package net.flex.dci.otn.controller.nms.utils;

import static net.flex.dci.otc.common.constants.Constants.POUND;
import static org.apache.commons.lang3.StringUtils.SPACE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterLogicalOp;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterOperation;
import net.flex.dci.otn.controller.nms.enums.AdditionalProperty;
import net.flex.dci.otn.controller.nms.nms.dto.FilterItemDto;
import org.apache.commons.lang3.StringUtils;

/**
 * extract filter items from filter string
 *
 * @version 1.0
 * @date 2022/6/6 15:29
 */
@Slf4j
public class FilterItemsUtils {

    private static final String OPERATION_ELEMENT_REGEX = "";

    private static final String AND = "and";

    private static final String OR = "or";

    private static final String SPLIT_CONSTANTS = " and | or ";

    public static List<FilterItemDto> extractFilterItems(String filterStr) {
        if (StringUtils.isEmpty(filterStr)) {
            return new ArrayList<>();
        }
        log.debug("start to extract the filter item,filter string is:{}", filterStr);
        String[] subFilters = filterStr.split(SPLIT_CONSTANTS);
        List<String> logicalOperators = extractLogicalOperators(filterStr);
        List<FilterItemDto> filterItemDtos = extractFilterItems(subFilters, logicalOperators);
        return filterItemDtos;
    }

    private static List<FilterItemDto> extractFilterItems(String[] subFilters,
            List<String> logicalOperators) {
        log.debug("extract filter item details");
        List<FilterItemDto> filterItemDtos = new ArrayList<>();
        int subSize = subFilters.length;
        if (subSize == 1) {
            FilterItemDto filterItemDto = extractFilterItemDto(subFilters[0], null);
            filterItemDtos.add(filterItemDto);
        } else {
            for (int i = 0; i < subSize; i++) {
                FilterItemDto filterItemDto;
                if (i == 0) {
                    filterItemDto = extractFilterItemDto(subFilters[i], null);
                } else {
                    filterItemDto = extractFilterItemDto(subFilters[i],
                            logicalOperators.get(i - 1));
                }
                filterItemDtos.add(filterItemDto);
            }
        }
        return filterItemDtos;
    }

    /**
     * key operator value
     *
     * @param subFilter
     * @param logicalOperator
     * @return
     */
    private static FilterItemDto extractFilterItemDto(String subFilter, String logicalOperator) {
        String[] subItems = subFilter.trim().split(SPACE);

        String keyName = subItems[0];
        String operator = subItems[1];
        String value = subItems[2];
        return FilterItemDto.builder().filter(keyName)
                .filterItem(value)
                .filterOp(FilterOperation.getOperation(operator))
                .logicalOp(
                        logicalOperator == null ? null
                                : FilterLogicalOp.getLogicalOp(logicalOperator))
                .isAdditional(isAdditionalKeyName(keyName))
                .build();
    }

    /**
     * judge is the key is additional
     *
     * @param keyName
     * @return
     */
    private static Boolean isAdditionalKeyName(String keyName) {
        String specialKey = keyName.substring(keyName.lastIndexOf(POUND) + 1);
        AdditionalProperty additionalProperty = AdditionalProperty.getAdditionalProperty(
                specialKey);
        if (null != additionalProperty) {
            return true;
        }
        return false;
    }

    /**
     * get extract logicalOperators like or and
     *
     * @param filterStr
     * @return
     */
    private static List<String> extractLogicalOperators(String filterStr) {
        String[] elements = filterStr.split(SPACE);
        List<String> logicalOperatorList = new ArrayList<>();
        Arrays.stream(elements).forEach(element -> {
            if (element.equals(AND) || element.equals(OR)) {
                logicalOperatorList.add(element);
            }
        });
        return logicalOperatorList;
    }
}
