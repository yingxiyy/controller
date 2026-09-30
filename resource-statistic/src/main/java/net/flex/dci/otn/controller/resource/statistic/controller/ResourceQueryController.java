package net.flex.dci.otn.controller.resource.statistic.controller;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.export.task.QueryTaskService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/7/11
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/resource/resource/query")
public class ResourceQueryController {

    private final QueryTaskService queryTaskService;


    @PostMapping
    public ResponseEntity<?> submitQuery(
            @RequestBody UnifiedQueryParam unifiedQueryParam, HttpServletRequest request) {
        try {
            log.info("submit async query task, {}",
                    unifiedQueryParam);
            String taskId = queryTaskService.submitQueryTask(unifiedQueryParam, request);
            return ResponseEntity.ok(Result.ok(taskId));
        } catch (Exception e) {
            log.error("submit export task failed", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }


}
