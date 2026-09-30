package net.flex.dci.otc.controller.otdr.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterOperation;
import net.flex.dci.otn.db.jpa.service.dao.dto.otdr.FilterItemDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.otdr.SortItemDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.get.otdr.results.input.FilterParam;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.sort.query.params.SortInfos;

/**
 * @version 1.0
 * @date 2023/1/9 11:11
 */
@Slf4j
public class SortFilterItemUtils {

    public static List<SortItemDto> parseSortItem(List<SortInfos> sortInfos) {
        log.debug("start to parse sort item dtos");
        if (sortInfos == null || sortInfos.isEmpty()) {
            return new ArrayList<>();
        }
        List<SortItemDto> sortItemDtos = sortInfos.stream().map(sortInfo -> {
            String sortName = firstLetterLow(sortInfo.getSortName().name());
            Boolean ascending = sortInfo.isAscending();
            return SortItemDto.builder().sortName(sortName).ascending(ascending).build();
        }).collect(Collectors.toList());
        return sortItemDtos;
    }

    public static List<FilterItemDto> parseFilterItem(List<FilterParam> filterParams) {
        log.debug("parse the filter item");
        if (filterParams == null || filterParams.isEmpty()) {
            return new ArrayList<>();
        }
        List<FilterItemDto> filterItemDtos = filterParams.stream().map(filterParam -> {
            String filterName = firstLetterLow(filterParam.getAttributeName().name());
            String filterValue = filterParam.getAttributeValue();
            if (filterName.equals(firstLetterLow(MonitorDirection.class.getSimpleName()))) {
                filterValue = String.valueOf(MonitorDirection.valueOf(filterValue).getIntValue());
            }
            String operator = filterParam.getOperator();

            return FilterItemDto.builder().filterParam(filterName).filter(filterValue).filterOp(
                    FilterOperation.getOperation(operator)).build();
        }).collect(Collectors.toList());
        return filterItemDtos;
    }

    private static String firstLetterLow(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c) && i == 0) {
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
