package net.flex.dci.otn.controller.taskinfo.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.taskinfo.dto.PageQueryParamDto;
import net.flex.dci.otn.controller.taskinfo.dto.PageTaskInfoDto;
import net.flex.dci.otn.controller.taskinfo.enums.OrderElement;
import net.flex.dci.otn.controller.taskinfo.service.impl.TaskInfoServiceImpl;
import net.flex.dci.otn.controller.webapp.Result;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/1/26 14:14
 */
@RestController
@Slf4j
public class TaskInfoController {

    @Autowired
    private TaskInfoServiceImpl taskInfoService;

    @RequestMapping(value = "/taskinfo", method = RequestMethod.GET)
    public ResponseEntity<?> listAllTaskInfoByCondition(
            @RequestParam(value = "page", defaultValue = "0", required = false) int offset,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit,
            @RequestParam(value = "sort", defaultValue = "desc", required = false) String sort,
            @RequestParam(value = "order", defaultValue = "id", required = false) OrderElement order,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "success", required = false) Boolean success,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "actionType", required = false) String actionType)
            throws CommonException {
        log.info("list all task info by condition");
        offset = offset < 0 ? 0 : offset;
        limit = limit < 0 ? 20 : limit;
        Direction direction =
                sort == null || !sort.equalsIgnoreCase("asc") ? Direction.DESC
                        : Direction.ASC;
        order = order == null ? OrderElement.id : order;
        PageQueryParamDto pageQueryParamDto = PageQueryParamDto.builder()
                .page(offset)
                .limit(limit)
                .direction(direction)
                .orderElement(order)
                .keywords(keywords)
                .userName(user)
                .resourceType(resourceType)
                .actionType(actionType)
                .success(success)
                .build();
        PageTaskInfoDto pageIdcData = taskInfoService.listAllTaskInfoByCondition(pageQueryParamDto);
        return new ResponseEntity<>(Result.ok(pageIdcData), HttpStatus.OK);
    }

    @RequestMapping(value = "/taskinfo/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getTaskInfoById(@PathVariable("id") Long taskInfoId) {
        log.info("get taskInfo detail {}", taskInfoId);
        TaskInfo taskInfo = taskInfoService.getTaskInfoById(taskInfoId);
        return new ResponseEntity<>(Result.ok(taskInfo), HttpStatus.OK);
    }

    @RequestMapping(value = "/taskinfo/{id}/subTask", method = RequestMethod.GET)
    public ResponseEntity<?> getSubTaskInfoListById(@PathVariable("id") long taskInfoId,
            @RequestParam(value = "page", defaultValue = "0", required = false) int offset,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit,
            @RequestParam(value = "sort", defaultValue = "desc", required = false) String sort,
            @RequestParam(value = "order", defaultValue = "id", required = false) OrderElement order,
            @RequestParam(value = "success", required = false) Boolean success,
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "keywords", required = false) String keyword) {
        log.info("get subTaskInfo list by root taskId:{}", taskInfoId);
        offset = offset < 0 ? 0 : offset;
        limit = limit < 0 ? 20 : limit;
        Direction direction =
                sort == null || !sort.equalsIgnoreCase("asc") ? Direction.DESC
                        : Direction.ASC;
        order = order == null ? OrderElement.id : order;
        PageQueryParamDto pageQueryParamDto = PageQueryParamDto.builder()
                .page(offset)
                .limit(limit)
                .direction(direction)
                .orderElement(order)
                .keywords(keyword)
                .success(success)
                .actionType(actionType)
                .resourceType(resourceType)
                .build();
        PageTaskInfoDto pageTaskInfoDto = taskInfoService.getSubTaskPagedByRootTask(taskInfoId,
                pageQueryParamDto);
        return new ResponseEntity<>(Result.ok(pageTaskInfoDto), HttpStatus.OK);
    }

    @RequestMapping(value = "/taskinfo/detail/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getTaskInfoDetail(
            @PathVariable("id") Long taskInfoId)
            throws CommonException {
        log.info("get taskInfo detail {}", taskInfoId);
        JSONObject jsonObject = taskInfoService.getDetail(taskInfoId);
        return new ResponseEntity<>(Result.ok(jsonObject), HttpStatus.OK);
    }

    @RequestMapping(value = "/taskinfo/delete/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteTaskInfo(@PathVariable("id") Long taskInfoId)
            throws CommonException {
        log.info("delete taskInfo {}", taskInfoId);
        taskInfoService.deleteTaskInfo(taskInfoId);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }
}
