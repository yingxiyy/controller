package net.flex.dci.otn.controller.taskinfo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.taskinfo.dto.PageQueryParamDto;
import net.flex.dci.otn.controller.taskinfo.dto.PageTaskInfoDto;
import net.flex.dci.otn.controller.taskinfo.dto.TaskInfoDto;
import net.flex.dci.otn.controller.taskinfo.message.TaskInfoMessager;
import net.flex.dci.otn.controller.taskinfo.service.TaskInfoService;
import net.flex.dci.otn.controller.taskinfo.utils.ConvertorUtils;
import net.flex.dci.otn.controller.taskinfo.utils.TaskInfoValidatorUtils;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;
import net.flex.dci.otn.db.jpa.service.dao.TaskInfoDaoService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskInfoServiceImpl implements TaskInfoService {


    private final TaskInfoDaoService taskInfoDaoService;

    private final TaskInfoMessager taskInfoMessager;

    public PageTaskInfoDto listAllTaskInfoByCondition(int offset, int limit) {
        log.debug("list all taskinfo data offset is {},limit is {}", offset, limit);
        Page<TaskInfo> taskInfoPage = taskInfoDaoService.findAllPaged(offset, limit);
        PageTaskInfoDto pageIdcData = ConvertorUtils.convertTaskInfoPaged(taskInfoPage);
        return pageIdcData;
    }


    @Override
    public void deleteTaskInfo(Long taskInfoId) {
        log.debug("delete task info ,task id is:{}", taskInfoId);
        TaskInfoValidatorUtils.validateTaskInfoId(taskInfoId);
        TaskInfo taskInfoMessage = taskInfoDaoService.findById(taskInfoId);
        if (taskInfoMessage.getRoot() && taskInfoMessage.getGroupId() != null) {
            List<TaskInfo> subtaskInfos = taskInfoDaoService.listAllTaskInfoByGroupId(
                    taskInfoMessage.getGroupId());
            List<Long> subTaskIds = subtaskInfos.stream().map(TaskInfo::getId)
                    .collect(Collectors.toList());
            log.debug("delete sub task id:{}", subTaskIds);
            taskInfoDaoService.deleteInBatch(subtaskInfos);
        }
        taskInfoDaoService.deleteInfo(taskInfoId);
        taskInfoMessager.notifyTaskInfoDelete(
                TaskInfoDto.builder().id(taskInfoId).build());
    }

    @Override
    public TaskInfo getTaskInfoById(Long taskInfoId) {
        log.debug("get task info by id,task id is:{}", taskInfoId);
        TaskInfoValidatorUtils.validateTaskInfoId(taskInfoId);
        TaskInfo taskInfo = taskInfoDaoService.findById(taskInfoId);
        return taskInfo;
    }

    @Override
    public PageTaskInfoDto getSubTaskPagedByRootTask(long taskInfoId,
            PageQueryParamDto pageQueryParamDto) {
        log.debug("get subTask info Paged by root task,task id:{} limit:{} page:{}", taskInfoId,
                pageQueryParamDto.getLimit(), pageQueryParamDto.getPage());
        TaskInfo rootTaskInfo = taskInfoDaoService.findById(taskInfoId);
        if (Objects.isNull(rootTaskInfo)) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "task info id:" + taskInfoId + " is not found");
        }
        Long subGroupId = rootTaskInfo.getGroupId();
        if (Objects.isNull(subGroupId) || subGroupId == 0L) {
            throw new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR,
                    "there's no sub taskInfo by the root task :" + taskInfoId);
        }
        Page<TaskInfo> subTaskPaged = taskInfoDaoService.retrieveAllSubTaskInfoPagedByGroupIdCondition(
                subGroupId,
                pageQueryParamDto.getPage(),
                pageQueryParamDto.getLimit(),
                pageQueryParamDto.getDirection(),
                pageQueryParamDto.getOrderElement().name(),
                pageQueryParamDto.getKeywords(),
                pageQueryParamDto.getResourceType(),
                pageQueryParamDto.getActionType(),
                pageQueryParamDto.getSuccess());
        PageTaskInfoDto pageIdcData = ConvertorUtils.convertTaskInfoPaged(subTaskPaged);
        return pageIdcData;
    }


    @Override
    public JSONObject getDetail(Long taskInfoId) {
        log.debug("get task detail for task id ,task id is:{}", taskInfoId);
        TaskInfoValidatorUtils.validateTaskInfoId(taskInfoId);
        String message = taskInfoDaoService.getDetail(taskInfoId);
        JSONObject jsonObject = JSON.parseObject(message);
        return jsonObject;
    }

    @Override
    public PageTaskInfoDto listAllTaskInfoByCondition(PageQueryParamDto pageQueryParamDto) {
        int offset = pageQueryParamDto.getPage();
        int limit = pageQueryParamDto.getLimit();
        log.debug("list all task Info data offset is {},limit is {}", offset, limit);
        Page<TaskInfo> taskInfoPage = taskInfoDaoService.listAllRootTaskInfoByCondition(
                pageQueryParamDto.getPage(),
                pageQueryParamDto.getLimit(),
                pageQueryParamDto.getDirection(),
                pageQueryParamDto.getOrderElement().name(),
                pageQueryParamDto.getUserName(),
                pageQueryParamDto.getKeywords(),
                pageQueryParamDto.getResourceType(),
                pageQueryParamDto.getActionType(),
                pageQueryParamDto.getSuccess());
        PageTaskInfoDto pageIdcData = ConvertorUtils.convertTaskInfoPaged(taskInfoPage);
        return pageIdcData;
    }
}