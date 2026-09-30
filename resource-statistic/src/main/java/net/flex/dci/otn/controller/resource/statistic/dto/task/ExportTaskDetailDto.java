package net.flex.dci.otn.controller.resource.statistic.dto.task;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.resource.statistic.dto.ResourceSearchParameter;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;

/**
 * 2026/6/29
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ExportTaskDetailDto implements Serializable {

    private String exportTaskId;

    private String type;

    private ResourceSearchParameter searchParameter;

    private UnifiedExportRequest unifiedExportRequest;
}
