package net.flex.dci.otn.controller.resource.statistic.core.manager;

import java.util.List;
import java.util.function.Consumer;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;

/**
 * 2026/7/12
 *
 * @author musa
 * @version 1.0
 **/
public interface QueryTaskManager {

    InventoryExportData<?> getQueryData(UnifiedResourceQueryParam unifiedResourceQueryParam);

    InventoryExportData<?> queryData(UnifiedQueryParam unifiedQueryParam);

    default void queryStream(UnifiedQueryParam queryParam, Consumer<List<?>> rowConsumer) {

    }
}
