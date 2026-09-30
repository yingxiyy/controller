package net.flex.dci.otn.controller.taskinfo.service;

import com.alibaba.fastjson.JSONObject;
import net.flex.dci.otn.controller.taskinfo.dto.PageQueryParamDto;
import net.flex.dci.otn.controller.taskinfo.dto.PageTaskInfoDto;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;

/**
 * @version 1.0
 * @date 2022/5/5 10:52
 */
public interface TaskInfoService {

    PageTaskInfoDto listAllTaskInfoByCondition(PageQueryParamDto pageQueryParamDto);

    JSONObject getDetail(Long taskInfoId);

    void deleteTaskInfo(Long taskInfoId);

    TaskInfo getTaskInfoById(Long taskInfoId);

    /**
     * get sub task Info paged by root task
     *
     * @param taskInfoId
     * @param pageQueryParamDto
     * @return
     */
    PageTaskInfoDto getSubTaskPagedByRootTask(long taskInfoId, PageQueryParamDto pageQueryParamDto);
}
