package net.flex.dci.otn.controller.resource.statistic.core.query;

import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.core.query.detail.CardQuery;
import net.flex.dci.otn.controller.resource.statistic.core.query.detail.TransceiverQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.CustomQueryConditionDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.MaterialQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.enums.MaterialCategory;
import org.springframework.stereotype.Component;

/**
 * 2026/7/20
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class MaterialQuery extends AbstractResourceQuery {

    private final TransceiverQuery transceiverQuery;

    private final CardQuery cardQuery;

    @Override
    public ResourceQueryType queryType() {
        return ResourceQueryType.Material;
    }

    @Override
    public InventoryExportData<?> queryData(UnifiedQueryParam unifiedQueryParam) {
        log.debug("get material query the conditional parameter:{}",
                unifiedQueryParam.getQueryCondition());
        CustomQueryConditionDTO queryCondition = unifiedQueryParam.getQueryCondition();
        MaterialQueryDTO materialQueryDTO = queryCondition.getMaterialQuery();
        List<String> subnet = unifiedQueryParam.getSubnet();
        if (isModule(materialQueryDTO)) {
            return transceiverQuery.queryData(materialQueryDTO, subnet);
        } else {
            return cardQuery.queryData(materialQueryDTO, subnet);
        }
    }

    @Override
    public void queryStream(UnifiedQueryParam unifiedQueryParam, Consumer<List<?>> rowConsumer) {
        log.debug(" material query the conditional parameter:{}",
                unifiedQueryParam.getQueryCondition());
        CustomQueryConditionDTO queryCondition = unifiedQueryParam.getQueryCondition();
        MaterialQueryDTO materialQueryDTO = queryCondition.getMaterialQuery();
        List<String> subnet = unifiedQueryParam.getSubnet();
        if (isModule(materialQueryDTO)) {
            transceiverQuery.queryStream(materialQueryDTO, subnet, rowConsumer);
        } else {
            cardQuery.queryStream(materialQueryDTO, subnet, rowConsumer);
        }
    }

    private boolean isModule(MaterialQueryDTO materialQueryDTO) {
        return materialQueryDTO.getMaterialCategory() == MaterialCategory.TRANSCEIVER;
    }
}
