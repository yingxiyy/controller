package net.flex.dci.otn.controller.resource.statistic.core.manager;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.core.query.AbstractInventoryQuery;
import net.flex.dci.otn.controller.resource.statistic.core.query.AbstractResourceQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;
import org.springframework.stereotype.Component;

/**
 * 2026/7/12
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class QueryTaskManagerImpl implements QueryTaskManager {

    private final Map<InventoryType, AbstractInventoryQuery> queryMap;

    private final Map<ResourceQueryType, AbstractResourceQuery> resourceQueryTypeMap;

    public QueryTaskManagerImpl(List<AbstractInventoryQuery> inventoryQueries,
            List<AbstractResourceQuery> resourceQueryList) {
        this.queryMap = inventoryQueries.stream()
                .peek(inv -> log.info("Registered inventory exporter for type: {}",
                        inv.inventoryScope()))
                .collect(Collectors.toMap(
                        AbstractInventoryQuery::inventoryScope,
                        Function.identity()
                ));

        this.resourceQueryTypeMap = resourceQueryList.stream()
                .peek(inv -> log.info("Registered query for type:{}", inv.queryType()))
                .collect(Collectors.toMap(AbstractResourceQuery::queryType, Function.identity()));

    }


    @Override
    public InventoryExportData<?> getQueryData(
            UnifiedResourceQueryParam unifiedResourceQueryParam) {
        log.debug("start to get query data the query parameter is :{}", unifiedResourceQueryParam);
        InventoryType inventoryType = unifiedResourceQueryParam.getUnifiedType();
        InventoryExportData inventoryExportData = queryMap.get(inventoryType)
                .queryData(unifiedResourceQueryParam);
        return inventoryExportData;
    }

    @Override
    public InventoryExportData<?> queryData(UnifiedQueryParam unifiedQueryParam) {
        log.debug("start to query data the query parameter is:{}", unifiedQueryParam);
        ResourceQueryType resourceQueryType = unifiedQueryParam.getResourceQueryType();
        InventoryExportData<?> inventoryExportData = resourceQueryTypeMap.get(resourceQueryType)
                .queryData(unifiedQueryParam);
        return inventoryExportData;
    }

    @Override
    public void queryStream(UnifiedQueryParam unifiedQueryParam, Consumer<List<?>> rowConsumer) {
        ResourceQueryType resourceQueryType = unifiedQueryParam.getResourceQueryType();
        resourceQueryTypeMap.get(resourceQueryType)
                .queryStream(unifiedQueryParam, rowConsumer);
    }
}
