package net.flex.dci.otn.controller.resource.statistic.task;

import static net.flex.dci.otc.common.constants.BroadCastConstant.EXPORT_FILE;
import static net.flex.dci.otc.common.constants.BroadCastConstant.RESOURCE_QUERY;
import static net.flex.dci.otc.common.constants.Constants.UNDER_LINE;
import static net.flex.dci.otn.controller.resource.statistic.utils.CommonUtils.buildNameWithFirst;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.DEFAULT_USER;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.QUERY_PREFIX;

import com.alibaba.fastjson.JSON;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.BroadcastMessage.BroadcastMessageBuilder;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;
import net.flex.dci.otn.controller.resource.statistic.converter.ResourceQueryConvertor;
import net.flex.dci.otn.controller.resource.statistic.converter.ResourceSearchConvertor;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.dto.PreviewList;
import net.flex.dci.otn.controller.resource.statistic.dto.ResourceSearchParameter;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.task.ExportTaskDetailDto;
import net.flex.dci.otn.controller.resource.statistic.dto.task.QueryTaskDetailDto;
import net.flex.dci.otn.controller.resource.statistic.dto.task.display.QueryTaskDisplayDto;
import net.flex.dci.otn.controller.resource.statistic.export.task.ExportTaskDto;
import net.flex.dci.otn.controller.resource.statistic.export.task.ExportTaskStatus;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/6/22
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ExportTaskManager {


    private final ResourceSearchConvertor resourceSearchConverter;

    private final ResourceQueryConvertor resourceQueryConvertor;


    public void onTaskSubmitted(ExportTaskDto exportTaskDto, String operator) {
        try {
            TaskInfoMessage taskInfoMessage = buildBaseMessage(exportTaskDto, operator);
            taskInfoMessage.setSuccessfully(false);
            taskInfoMessage.setActionTime(exportTaskDto.getCreateTimestamp());
            TaskInfoMessager.sendMessage(taskInfoMessage);
            //todo broadcast
            log.debug("task center message sent: task submitted, taskId={}",
                    exportTaskDto.getTaskId());
            broadcastExportTask(exportTaskDto);

        } catch (Exception e) {
            log.warn("task center notify failed, export continues", e);
        }
    }

    private void broadcastExportTask(ExportTaskDto exportTaskDto) {
        log.debug("broadcast export task,the export task is:{}", exportTaskDto);
        BroadcastMessageBuilder broadcastMessageBuilder = BroadcastMessage.builder()
                .title(EXPORT_FILE)
                .message(exportTaskDto.getFileName());
        boolean error = false;
        if (exportTaskDto.getStatus() == ExportTaskStatus.COMPLETED
                || exportTaskDto.getStatus() == ExportTaskStatus.FAILED) {
            error = exportTaskDto.getStatus() != ExportTaskStatus.COMPLETED;
        }
        broadcastMessageBuilder.error(error);
        String message =
                exportTaskDto.getFileName() + " export " + exportTaskDto.getStatus().name();
        broadcastMessageBuilder.message(message);
        BroadcastMessager.publishKafkaMessage(
                broadcastMessageBuilder.build()
        );
    }

    private void broadcastQueryTask(ExportTaskDto exportTaskDto) {
        log.debug("broadcast query task,the export task is:{}", exportTaskDto);
        UnifiedQueryParam queryParam = exportTaskDto.getQueryParam();
//        String queryMessage = buildQueryMessage(exportTaskDto.getQueryParam());
        BroadcastMessageBuilder broadcastMessageBuilder = BroadcastMessage.builder()
                .title(RESOURCE_QUERY);
        boolean error = false;
        if (exportTaskDto.getStatus() == ExportTaskStatus.COMPLETED
                || exportTaskDto.getStatus() == ExportTaskStatus.FAILED) {
            error = exportTaskDto.getStatus() != ExportTaskStatus.COMPLETED;
        }
        broadcastMessageBuilder.error(error);
        String message =
                queryParam.getResourceQueryType().name() + " resource query "
                        + exportTaskDto.getStatus()
                        .name();
        broadcastMessageBuilder.message(message);
        BroadcastMessager.publishKafkaMessage(
                broadcastMessageBuilder.build()
        );
    }


    private TaskInfoMessage buildBaseMessage(ExportTaskDto exportTaskDto, String operator) {
        ExportTaskDetailDto details = buildDetailJson(exportTaskDto);
        String detailJson = JSON.toJSONString(details);
        TaskInfoMessage msg = new TaskInfoMessage(
                operator != null ? operator : DEFAULT_USER,
                ResourceType.FILE_EXPORT,
                ActionType.EXPORT,
                detailJson);

        msg.setResourceId(exportTaskDto.getTaskId());
        msg.setResourceName(buildResourceName(details.getSearchParameter(),
                details.getUnifiedExportRequest().getUnifiedType()));
        msg.setRoot(true);
        return msg;
    }

    private TaskInfoMessage buildQueryTaskMessage(ExportTaskDto holder, String operator) {
        QueryTaskDetailDto details = buildQueryDetailJson(holder);
        String detailJson = JSON.toJSONString(details);
        TaskInfoMessage msg = new TaskInfoMessage(
                operator != null ? operator : DEFAULT_USER,
                ResourceType.RESOURCE,
                ActionType.ResourceQuery,
                detailJson);

        msg.setResourceId(holder.getTaskId());
        msg.setResourceName(buildQueryResourceName(holder.getQueryParam()));
        msg.setRoot(true);
        return msg;
    }

    /**
     * build query resource name
     *
     * @param param queryParam
     * @return resourceName
     */
    private String buildQueryResourceName(UnifiedQueryParam param) {
        String base = QUERY_PREFIX + param.getResourceQueryType().name();
        if (!CollectionUtils.isEmpty(param.getSubnet())) {
            return base + UNDER_LINE + param.getSubnet().get(0)
                    + (param.getSubnet().size() > 1 ? UNDER_LINE + param.getSubnet().size()
                    + "SUBNET"
                    : "");
        }
        return base + "_ALL";
    }

    private QueryTaskDetailDto buildQueryDetailJson(ExportTaskDto holder) {
        UnifiedQueryParam queryParam = holder.getQueryParam();
        QueryTaskDisplayDto displayDto = resourceQueryConvertor.resolveDisplay(
                queryParam);
        QueryTaskDetailDto queryTaskDetailDto = QueryTaskDetailDto.builder()
                .queryCategory(queryParam.getResourceQueryType())
                .queryTaskId(holder.getTaskId())
                .type(queryParam.getResourceQueryType().name())
                .queryParam(queryParam)
                .displayParam(displayDto)
                .build();
        return queryTaskDetailDto;
    }

    private String buildResourceName(ResourceSearchParameter param,
            InventoryType unifiedType) {
        String base = "EXPORT";
        base += "_" + unifiedType;

        if (isNotEmpty(param.getSite())) {
            return buildNameWithFirst(base, "SITE", param.getSite().getItems());
        }
        if (isNotEmpty(param.getSubnet())) {
            return buildNameWithFirst(base, "SUBNET", param.getSubnet());
        }
        if (isNotEmpty(param.getNe())) {
            return buildNameWithFirst(base, "NE", param.getNe().getItems());
        }
        if (isNotEmpty(param.getSiteLink())) {
            return buildNameWithFirst(base, "SITELINK", param.getSiteLink().getItems());
        }
        if (isNotEmpty(param.getPhyLink())) {
            return buildNameWithFirst(base, "PHYLINK", param.getPhyLink().getItems());
        }
        if (isNotEmpty(param.getTunnel())) {
            return buildNameWithFirst(base, "TUNNEL", param.getTunnel().getItems());
        }

        return base + "_ALL";
    }


    public void onTaskCompleted(ExportTaskDto task) {
        TaskInfoMessage msg = buildBaseMessage(task, task.getOperator());
        msg.setSuccessfully(task.getStatus() == ExportTaskStatus.COMPLETED);
        msg.setActionTime(task.getCreateTimestamp());
        msg.setEndTime(task.getCompleteTimestamp());
        msg.setErrorReason(task.getErrorMessage());

        TaskInfoMessager.sendMessage(msg);
        log.info(
                "task center message sent: task completed, taskId={}, status={}, operator={}, resourceName={}",
                task.getTaskId(), task.getStatus(), task.getOperator(), msg.getResourceName());
        broadcastExportTask(task);
    }


    private ExportTaskDetailDto buildDetailJson(ExportTaskDto task) {
        log.debug("build detail Json the task:{}", task.getTaskId());
        String taskId = task.getTaskId();
        UnifiedExportRequest exportRequest = task.getExportRequest();
        ResourceSearchParameter searchParameter = resourceSearchConverter.convertSearchParameter(
                exportRequest);
        ExportTaskDetailDto exportTaskDetail = ExportTaskDetailDto.builder().exportTaskId(taskId)
                .searchParameter(searchParameter)
                .unifiedExportRequest(exportRequest)
                .build();
        return exportTaskDetail;
//        try {
//            return objectMapper.writeValueAsString(exportTaskDetail);
//        } catch (JsonProcessingException e) {
//            log.warn("failed to build detail json", e);
//            return "";
//        }
    }


    @Deprecated
    public static String buildResourceName(UnifiedExportRequest req) {
        String base = "EXPORT_" + req.getUnifiedType() + "(" + req.getFormat() + ")";

        if (isNotEmpty(req.getNeIds())) {
            return base + "-" + req.getNeIds().size() + "NE";
        }
        if (isNotEmpty(req.getSiteIds())) {
            return base + "-" + req.getSiteIds().size() + "SITE";
        }
        if (isNotEmpty(req.getTunnelIds())) {
            return base + "-" + req.getTunnelIds().size() + "TUNNEL";
        }
        if (isNotEmpty(req.getPhyLinkIds())) {
            return base + "-" + req.getPhyLinkIds().size() + "PLINK";
        }
        if (isNotEmpty(req.getSubnet())) {
            if (req.getSubnet().size() == 1) {
                return base + "-" + req.getSubnet().get(0);
            }
            return base + "-" + req.getSubnet().size() + "SUBNET";
        }

        return base + "-ALL";
    }

    private static boolean isNotEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private boolean isNotEmpty(PreviewList<String> list) {
        return list != null && !CollectionUtils.isEmpty(list.getItems());
    }

    /**
     * build on query task submitted
     *
     * @param holder
     * @param operator
     */
    public void onQueryTaskSubmitted(ExportTaskDto holder, String operator) {
        try {
            TaskInfoMessage taskInfoMessage = buildQueryTaskMessage(holder, operator);
            taskInfoMessage.setSuccessfully(false);
            taskInfoMessage.setActionTime(holder.getCreateTimestamp());
            TaskInfoMessager.sendMessage(taskInfoMessage);
            //todo broadcast
            log.debug("task center message sent: task submitted, taskId={}",
                    holder.getTaskId());
            broadcastQueryTask(holder);

        } catch (Exception e) {
            log.warn("task center notify failed, export continues", e);
        }
    }


    public void onQueryTaskCompleted(ExportTaskDto task) {
        TaskInfoMessage msg = buildQueryTaskMessage(task, task.getOperator());
        msg.setSuccessfully(task.getStatus() == ExportTaskStatus.COMPLETED);
        msg.setActionTime(task.getCreateTimestamp());
        msg.setEndTime(task.getCompleteTimestamp());
        msg.setErrorReason(task.getErrorMessage());

        TaskInfoMessager.sendMessage(msg);
        log.info(
                "task center message sent: query task completed, taskId={}, status={}, operator={}, resourceName={}",
                task.getTaskId(), task.getStatus(), task.getOperator(), msg.getResourceName());
        broadcastQueryTask(task);
    }
}
