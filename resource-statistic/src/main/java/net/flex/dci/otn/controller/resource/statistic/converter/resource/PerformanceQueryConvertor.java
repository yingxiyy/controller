package net.flex.dci.otn.controller.resource.statistic.converter.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.task.display.QueryTaskDisplayDto;
import org.springframework.stereotype.Component;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class PerformanceQueryConvertor extends AbstractResourceQueryConvertor {

    @Override
    public QueryTaskDisplayDto resolveDisplay(UnifiedQueryParam queryParam) {
        return null;
    }

    @Override
    public ResourceQueryType resourceQueryType() {
        return ResourceQueryType.Performance;
    }
}
