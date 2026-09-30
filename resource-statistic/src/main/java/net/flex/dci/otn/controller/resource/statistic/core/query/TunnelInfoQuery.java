package net.flex.dci.otn.controller.resource.statistic.core.query;

import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.core.query.detail.CircuitQuery;
import net.flex.dci.otn.controller.resource.statistic.core.query.detail.LLDPQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.CircuitQueryDTO;
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
public class TunnelInfoQuery extends AbstractResourceQuery {

    private final LLDPQuery lldpQuery;

    private final CircuitQuery circuitQuery;

    @Override
    public ResourceQueryType queryType() {
        return ResourceQueryType.Tunnel;
    }

    @Override
    public InventoryExportData<?> queryData(UnifiedQueryParam unifiedQueryParam) {
        log.info("tunnel query data condition is:{}",
                unifiedQueryParam.getQueryCondition().getTunnelQuery());
        CircuitQueryDTO tunnelQueryCondition = unifiedQueryParam.getQueryCondition()
                .getTunnelQuery();
        List<String> subnet = unifiedQueryParam.getSubnet();
        boolean isLLDP = tunnelQueryCondition.isIncludeLLdp();
        if (isLLDP) {
            return lldpQuery.queryData(tunnelQueryCondition, subnet);
        } else {

        }
        return null;
    }

    @Override
    public void queryStream(UnifiedQueryParam unifiedQueryParam, Consumer<List<?>> rowConsumer) {
        log.info("tunnel query data condition is:{}", unifiedQueryParam.getQueryCondition());
        CircuitQueryDTO circuitQueryDTO = unifiedQueryParam.getQueryCondition().getTunnelQuery();
        List<String> subnet = unifiedQueryParam.getSubnet();
        boolean isLLDP = circuitQueryDTO.isIncludeLLdp();
        if (isLLDP) {
            lldpQuery.queryStream(circuitQueryDTO, subnet, rowConsumer);
        } else {
            //tunnel base info
            circuitQuery.queryStream(circuitQueryDTO, subnet, rowConsumer);
        }
    }
}
