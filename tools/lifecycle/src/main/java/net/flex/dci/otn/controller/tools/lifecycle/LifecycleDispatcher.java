/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle;

import static net.flex.dci.otc.common.constants.Constants.OPERATIONS_URL_PREFIX;
import static net.flex.dci.otn.controller.tools.lifecycle.utils.LFConstants.LOGGER_ENTITY;
import static net.flex.dci.otn.controller.tools.lifecycle.utils.LFConstants.LOGGER_SEND_TIME;

import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.http.HttpUtils;
import net.flex.dci.otn.controller.tools.lifecycle.core.HttpResponseWrapper;
import net.flex.dci.otn.controller.tools.lifecycle.dto.LifeCycleLog;
import net.flex.dci.otn.controller.tools.lifecycle.dto.OperationInfo;
import net.flex.dci.otn.controller.tools.lifecycle.enums.ExecuteStatus;
import net.flex.dci.otn.controller.tools.lifecycle.handler.DefaultLifecycleLogHandler;
import net.flex.dci.otn.controller.tools.lifecycle.handler.ILifecycleLog;
import net.flex.dci.otn.controller.tools.lifecycle.utils.LogHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/16 13:22
 */

@Component
@Slf4j
public class LifecycleDispatcher {

    @Autowired
    private DefaultLifecycleLogHandler defaultLifecycleLogHandler;

    public LifecycleDispatcher() {
    }

    public void recordLog(HttpServletRequest request, HttpServletResponse response, Exception ex)
            throws UnsupportedEncodingException {
        log.info("start to record the execute");
        String method = request.getMethod();
        String url = HttpUtils.extractURL(request);
        if (!url.contains(OPERATIONS_URL_PREFIX)) {
            return;
        }
        if (method.equals(HttpMethod.POST)) {
            recordDispatcher(request, response, ex);
        }
    }

    /**
     * start to dispatcher the record log handler for the request
     *
     * @param request
     * @param response
     * @param ex
     */
    private void recordDispatcher(HttpServletRequest request, HttpServletResponse response,
            Exception ex) throws UnsupportedEncodingException {
        log.debug("start to dispatcher the log to record");

        LifeCycleLog lifeCycleLog = extractLifecycleLog(request, response, ex);
        OperationInfo operationInfo = OperationInfo.extractOperationInfo(
                lifeCycleLog.getOper_url().replace(OPERATIONS_URL_PREFIX, ""));

        //todo:do something for the life cyclelog
        lifeCycleLog.setOperModel(operationInfo.getModule());
        lifeCycleLog.setOperation(operationInfo.getOperation());
        lifeCycleLog.setOperationName(operationInfo.getOperationName());
        ILifecycleLog logHandler = LogHandlerFactory.getHandler(operationInfo.getModule());
//        defaultLifecycleLogHandler.recordLog(lifeCycleLog);

        logHandler.recordLog(lifeCycleLog);
    }


    private LifeCycleLog extractLifecycleLog(HttpServletRequest request,
            HttpServletResponse response, Exception e) {
        LifeCycleLog lifeCycleLog = (LifeCycleLog) request.getAttribute(LOGGER_ENTITY);
        Long createTimestamp = (Long) request.getAttribute(LOGGER_SEND_TIME);
        String status = getResponseStatus(response.getStatus());
        lifeCycleLog.setOperStatus(status);
        lifeCycleLog.setCreateTimestamp(createTimestamp);
        if (e != null) {
            lifeCycleLog.setExp_msg(
                    stackTraceToString(e.getClass().getName(), e.getMessage(), e.getStackTrace()));
        }
        HttpResponseWrapper responseWrapper = (HttpResponseWrapper) response;
        byte[] bytes = responseWrapper.getBytes();
        String resp_param = new String(bytes);
        lifeCycleLog.setOper_resp_param(resp_param);
        return lifeCycleLog;
    }


    protected String getResponseStatus(int status) {
        if (status == 200) {
            return ExecuteStatus.SUCCESS.getStatus();
        }
        return ExecuteStatus.FAILED.getStatus();
    }

    protected String stackTraceToString(String exceptionName, String exceptionMessage,
            StackTraceElement[] elements) {
        StringBuffer strbuff = new StringBuffer();
        for (StackTraceElement stet : elements) {
            strbuff.append(stet + "\n");
        }
        String message = exceptionName + ":" + exceptionMessage + "\n\t" + strbuff.toString();
        return message;
    }
}
