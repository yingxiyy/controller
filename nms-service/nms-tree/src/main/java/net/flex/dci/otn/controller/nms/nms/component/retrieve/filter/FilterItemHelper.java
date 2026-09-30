package net.flex.dci.otn.controller.nms.nms.component.retrieve.filter;

import static net.flex.dci.otc.common.constants.Constants.POUND;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otn.controller.nms.enums.AdditionalProperty;
import net.flex.dci.otn.controller.nms.nms.dto.FilterDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2023/2/1 14:34
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FilterItemHelper implements Serializable {

    private final PhyNodeNameFilterHelper phyNodeNameFilterHelper;

    private final SiteNodeNameFilterHelper siteNodeNameFilterHelper;

    private final TerminationPointFilterHelper terminationPointFilterHelper;


    /**
     * special method
     *
     * @return
     */
    public List<FilterItem> getFilterItems(FilterDto filterDto, NMSConvertType nmsConvertType) {
        log.debug("start to generate the filter item");
        List<FilterItem> filterItems = new ArrayList<>();
        List<FilterItem> directFilterItems = filterDto.getDirectFilterItems();
        filterItems.addAll(directFilterItems);
        List<FilterItem> additionalItems = filterDto.getAdditionalFilterItems();
        if (!additionalItems.isEmpty()) {
            List<FilterItem> additionalFilterItems = transfer2DirectFilter(additionalItems,
                    nmsConvertType);
//            if (!directFilterItems.isEmpty()) {
//                additionalFilterItems.forEach(
//                        filterItem -> filterItem.setLogicalOp(FilterLogicalOp.AND));
//            } else {
//                int additionalItemSize = additionalFilterItems.size();
//                for (int i = 1; i < additionalItemSize; i++) {
//                    additionalFilterItems.get(i).setLogicalOp(FilterLogicalOp.AND);
//                }
//            }
            filterItems.addAll(additionalFilterItems);
        }
        return filterItems;
    }

    private List<FilterItem> transfer2DirectFilter(List<FilterItem> additionalItems,
            NMSConvertType nmsConvertType) {
        log.debug("transfer additional filter to direct filter");
        List<FilterItem> filterItems = additionalItems.stream().map(filterItem -> {
            return convertAdditionFilter(filterItem, nmsConvertType);
        }).collect(Collectors.toList());
        return filterItems;
    }


    /**
     * convert additionFilter only support contains operation
     *
     * @param filterItem
     * @param nmsConvertType
     * @return
     */
    private FilterItem convertAdditionFilter(FilterItem filterItem, NMSConvertType nmsConvertType) {
        log.debug("convert additional filter to direct filter");
        String filter = filterItem.getFilter();
        String specialKey = filter.substring(filter.lastIndexOf(POUND) + 1);
        AdditionalProperty additionalProperty = AdditionalProperty.getAdditionalProperty(
                specialKey);
        FilterItem convertFilterItem = null;
        switch (Objects.requireNonNull(additionalProperty)) {
            case SOURCE_NODE_NAME:
            case DEST_NODE_NAME:
                convertFilterItem = phyNodeNameFilterHelper.transferFilterItem(filterItem,
                        additionalProperty,
                        nmsConvertType);
                break;
            case SOURCE_TP_NAME:
            case DEST_TP_NAME:
                convertFilterItem = terminationPointFilterHelper.transferFilterItem(
                        filterItem,
                        additionalProperty,
                        nmsConvertType);
                break;
            case DEST_SITE_NAME:
            case SOURCE_SITE_NAME:
                convertFilterItem = siteNodeNameFilterHelper.transferFilterItem(filterItem,
                        additionalProperty,
                        nmsConvertType);
                break;

        }
        return convertFilterItem;
    }

}
