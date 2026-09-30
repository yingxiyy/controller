package net.flex.dci.otn.controller.resource.statistic.export.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;

/**
 * 导出任务状态 DTO，返回给前端
 *
 * @author musa
 * @version 1.0
 * @date 2026/6/11
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportTaskDto {

    private String taskId;

    private String taskName;

    private String operator;

    private ExportTaskStatus status;

    private UnifiedExportRequest exportRequest;

    private UnifiedResourceQueryParam query;

    private UnifiedQueryParam queryParam;

    private String filePath;

    private String fileName;

    private Long fileSize;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;


    private Long createTimestamp;

    private Long completeTimestamp;
}
