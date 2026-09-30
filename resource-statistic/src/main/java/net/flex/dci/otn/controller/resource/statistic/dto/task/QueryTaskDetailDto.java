package net.flex.dci.otn.controller.resource.statistic.dto.task;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.task.display.QueryTaskDisplayDto;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class QueryTaskDetailDto implements Serializable {

    private String queryTaskId;

    private String type;

    private ResourceQueryType queryCategory;

    private UnifiedQueryParam queryParam;

    private QueryTaskDisplayDto displayParam;
}
