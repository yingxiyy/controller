package net.flex.dci.otn.controller.resource.statistic.export.task;

import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;
import net.flex.dci.otn.controller.resource.statistic.dto.ExportResultPreviewDto;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;

/**
 * 导出任务管理服务
 *
 * @author musa
 * @version 1.0
 * @date 2026/6/11
 **/
public interface ExportTaskService {


    String submitExportTask(UnifiedExportRequest exportRequest, HttpServletRequest request);

    ExportTaskDto getTaskStatus(String taskId);


    Path getTaskFilePath(String taskId);

    ExportResultPreviewDto getExportResultPreviewDto(String taskId, int page, int limit,
            String keyword);
}
