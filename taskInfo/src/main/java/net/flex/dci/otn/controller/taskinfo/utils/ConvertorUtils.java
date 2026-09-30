package net.flex.dci.otn.controller.taskinfo.utils;

import com.alibaba.fastjson.JSON;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.taskinfo.dto.PageTaskInfoDto;
import net.flex.dci.otn.controller.taskinfo.dto.TaskInfoDto;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;
import org.springframework.data.domain.Page;

/**
 * @version 1.0
 * @date 2022/4/16 13:04
 */
@Slf4j
public class ConvertorUtils {

    public static PageTaskInfoDto convertTaskInfoPaged(Page<TaskInfo> taskInfoPage) {
        PageTaskInfoDto pageIdcData = PageTaskInfoDto.builder()
                .taskInfoList(taskInfoPage.toList())
                .currentPage(Long.valueOf(taskInfoPage.getPageable().getPageNumber()) + 1L)
                .totalPages(taskInfoPage.getTotalPages())
                .totalElements(taskInfoPage.getTotalElements()).build();

        return pageIdcData;
    }

    public static TaskInfo convertTaskInfoMsg2Entity(TaskInfoMessage taskInfoMessage) {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setResourceId(taskInfoMessage.getResourceId());
        taskInfo.setId(taskInfoMessage.getId());
        taskInfo.setResourceId(taskInfoMessage.getResourceId());
        taskInfo.setActionTime(taskInfoMessage.getActionTime());
        taskInfo.setEndTime(taskInfoMessage.getEndTime() == null ? null
                : taskInfoMessage.getEndTime());
        taskInfo.setActionType(taskInfoMessage.getActionType());
        taskInfo.setWho(taskInfoMessage.getWho());
        taskInfo.setResourceType(taskInfoMessage.getResourceType());
        taskInfo.setSuccessfully(taskInfoMessage.getSuccessfully());
        taskInfo.setHasDetail(taskInfoMessage.hasDetail());
        taskInfo.setResourceName(taskInfoMessage.getResourceName());
        taskInfo.setErrorReason(taskInfoMessage.getErrorReason());
        taskInfo.setGroupId(
                taskInfoMessage.getGroupId() == null ? 0 : taskInfoMessage.getGroupId());
        taskInfo.setRoot(taskInfoMessage.getRoot() == null || taskInfoMessage.getRoot());
        taskInfo.setObjectId(taskInfoMessage.getObjectId());
        taskInfo.setObjectType(taskInfoMessage.getObjectType());
        taskInfo.setScanResultId(taskInfoMessage.getScanResultId());

        return taskInfo;
    }


    public static TaskInfo updateTaskInfoFromMessage(TaskInfo existingEntity,
            TaskInfoMessage message) {
        if (existingEntity == null) {
            // 如果没有传入现有实体，创建一个新的
            return convertTaskInfoMsg2Entity(message);
        }

        if (message == null) {
            // 如果没有传入消息，直接返回现有实体
            return existingEntity;
        }

        // 只更新非空字段
        if (message.getResourceId() != null) {
            existingEntity.setResourceId(message.getResourceId());
        }
        if (message.getId() != null) {
            existingEntity.setId(message.getId());
        }
        if (message.getActionTime() != null) {
            existingEntity.setActionTime(message.getActionTime());
        }
        if (message.getEndTime() != null) {
            existingEntity.setEndTime(message.getEndTime());
        }
        if (message.getActionType() != null) {
            existingEntity.setActionType(message.getActionType());
        }
        if (message.getWho() != null) {
            existingEntity.setWho(message.getWho());
        }
        if (message.getResourceType() != null) {
            existingEntity.setResourceType(message.getResourceType());
        }
        if (message.getSuccessfully() != null) {
            existingEntity.setSuccessfully(message.getSuccessfully());
        }
        // hasDetail 是 boolean 类型，不能为 null，但我们可以检查是否设置了值
        existingEntity.setHasDetail(message.hasDetail());
        if (message.getResourceName() != null) {
            existingEntity.setResourceName(message.getResourceName());
        }
        if (message.getErrorReason() != null) {
            existingEntity.setErrorReason(message.getErrorReason());
        }
        if (message.getGroupId() != null) {
            existingEntity.setGroupId(message.getGroupId());
        } else {
            existingEntity.setGroupId(0L);
        }
        if (message.getRoot() != null) {
            existingEntity.setRoot(message.getRoot());
        } else {
            existingEntity.setRoot(true);
        }
        log.debug("update entity is:{}", existingEntity);
        // 返回更新后的实体
        return existingEntity;
    }


    public static TaskInfoDto convertTaskInfoEntity2Dto(TaskInfo taskInfoMessage,
            TaskInfoMessage taskMessage) {
        TaskInfoDto taskInfo = new TaskInfoDto();
        taskInfo.setResourceId(taskInfoMessage.getResourceId());
        taskInfo.setId(taskInfoMessage.getId());
        taskInfo.setResourceId(taskInfoMessage.getResourceId());
        taskInfo.setActionTime(new Date(taskInfoMessage.getActionTime()));
        taskInfo.setEndTime(taskInfoMessage.getEndTime() == null ? null
                : new Date(taskInfoMessage.getEndTime()));
        taskInfo.setActionType(taskInfoMessage.getActionType());
        taskInfo.setWho(taskInfoMessage.getWho());
        taskInfo.setResourceType(taskInfoMessage.getResourceType());
        taskInfo.setSuccessfully(taskInfoMessage.getSuccessfully());
        taskInfo.setHasDetail(taskInfoMessage.getHasDetail());
        taskInfo.setResourceName(taskInfoMessage.getResourceName());
        taskInfo.setErrorReason(taskInfoMessage.getErrorReason());
        if (taskInfoMessage.getHasDetail()) {
            Object details = JSON.parse(taskMessage.getDetail());
            taskInfo.setDetail(details);
        }
        taskInfo.setGroupId(taskInfoMessage.getGroupId());
        taskInfo.setRoot(taskInfoMessage.getRoot());
        taskInfo.setObjectId(taskInfoMessage.getObjectId());
        taskInfo.setScanResultId(taskInfoMessage.getScanResultId());
        taskInfo.setObjectType(taskInfoMessage.getObjectType());
        return taskInfo;
    }
}
