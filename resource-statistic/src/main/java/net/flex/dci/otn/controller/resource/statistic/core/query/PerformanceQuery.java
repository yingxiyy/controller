package net.flex.dci.otn.controller.resource.statistic.core.query;

import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
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
public class PerformanceQuery extends AbstractResourceQuery {

    @Override
    public ResourceQueryType queryType() {
        return ResourceQueryType.Performance;
    }

    @Override
    public InventoryExportData<?> queryData(UnifiedQueryParam unifiedQueryParam) {
        return null;
    }

    @Override
    public void queryStream(UnifiedQueryParam unifiedQueryParam, Consumer<List<?>> rowConsumer) {

    }
}
