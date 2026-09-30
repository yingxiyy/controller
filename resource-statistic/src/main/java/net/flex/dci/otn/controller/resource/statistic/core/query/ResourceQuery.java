package net.flex.dci.otn.controller.resource.statistic.core.query;

import java.util.List;
import java.util.function.Consumer;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;

/**
 * 2026/7/20
 *
 * @author musa
 * @version 1.0
 **/
public interface ResourceQuery {

    ResourceQueryType queryType();

    InventoryExportData<?> queryData(UnifiedQueryParam unifiedQueryParam);

    void queryStream(UnifiedQueryParam unifiedQueryParam, Consumer<List<?>> rowConsumer);
}
