package net.flex.dci.otn.controller.resource.statistic.core.query;

import java.util.List;
import java.util.function.Consumer;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.BaseQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;

/**
 * 2026/7/13
 *
 * @author musa
 * @version 1.0
 **/
public interface InventoryQuery<T> {

    InventoryType inventoryScope();

    default InventoryExportData<T> queryData(UnifiedResourceQueryParam unifiedResourceQueryParam) {
        return null;
    }

    default InventoryExportData<?> queryData(BaseQueryDTO queryDTO, List<String> subnet) {
        return null;
    }

    default void queryStream(BaseQueryDTO baseQueryDTO, List<String> subnet,
            Consumer<List<?>> rowConsumer) {

    }
}
