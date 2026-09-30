package net.flex.dci.otn.controller.resource.statistic.converter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.converter.resource.IResourceQueryConvertor;
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
public class ResourceQueryConvertor {

    private final Map<ResourceQueryType, IResourceQueryConvertor> resourceQueryConvertorMap = new ConcurrentHashMap<>();

    public ResourceQueryConvertor(List<IResourceQueryConvertor> resourceQueryConvertors) {
        resourceQueryConvertors.forEach(convertor -> {
            resourceQueryConvertorMap.put(convertor.resourceQueryType(), convertor);
        });
    }

    public QueryTaskDisplayDto resolveDisplay(UnifiedQueryParam queryParam) {
        log.debug("resolve display the query param:{}", queryParam);
        ResourceQueryType resourceQueryType = queryParam.getResourceQueryType();
        return resourceQueryConvertorMap.get(resourceQueryType).resolveDisplay(queryParam);
    }
}
