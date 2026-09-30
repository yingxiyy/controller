package net.flex.dci.otn.controller.resource.statistic.converter.resource;

import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.task.display.QueryTaskDisplayDto;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
public interface IResourceQueryConvertor {

    QueryTaskDisplayDto resolveDisplay(UnifiedQueryParam queryParam);

    ResourceQueryType resourceQueryType();
}
