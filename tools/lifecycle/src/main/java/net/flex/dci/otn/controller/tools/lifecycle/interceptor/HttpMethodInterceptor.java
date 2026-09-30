/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.interceptor;

import static net.flex.dci.otn.controller.tools.lifecycle.utils.LFConstants.LOGGER_ENTITY;
import static net.flex.dci.otn.controller.tools.lifecycle.utils.LFConstants.LOGGER_SEND_TIME;

import com.alibaba.fastjson.JSON;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.Constants;
import net.flex.dci.otc.common.util.http.HttpUtils;
import net.flex.dci.otn.controller.tools.lifecycle.LifecycleDispatcher;
import net.flex.dci.otn.controller.tools.lifecycle.core.HttpRequestWrapper;
import net.flex.dci.otn.controller.tools.lifecycle.dto.LifeCycleLog;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/16 11:24
 */
@Slf4j
public class HttpMethodInterceptor extends WebRequestHandlerInterceptorAdapter {


    private LifecycleDispatcher lifecycleDispatcher;

    public HttpMethodInterceptor(
            WebRequestInterceptor requestInterceptor, LifecycleDispatcher lifecycleDispatcher) {
        super(requestInterceptor);
        this.lifecycleDispatcher = lifecycleDispatcher;
    }


    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        log.debug("request path:[{}] uri[{}]", request.getServletPath(), request.getRequestURI());
        Long userId = getUserId(request.getHeader(Constants.TOKEN_HEADER));
        LifeCycleLog lifeCycleLog = new LifeCycleLog();
        if (!(request instanceof HttpRequestWrapper)) {
            return true;
        }
        try {
            HttpRequestWrapper httpRequestWrapper = (HttpRequestWrapper) request;
            lifeCycleLog.setSessionId(userId);
            String param = JSON.toJSONString(JSON.parse(httpRequestWrapper.getBody()), false);
            lifeCycleLog.setOper_req_param(param);
            lifeCycleLog.setClientIp(HttpUtils.getRequestIp(httpRequestWrapper));
            lifeCycleLog.setMethod(httpRequestWrapper.getMethod());
            lifeCycleLog.setOper_url(HttpUtils.extractURL(httpRequestWrapper));
            request.setAttribute(LOGGER_SEND_TIME, System.currentTimeMillis());
            request.setAttribute(LOGGER_ENTITY, lifeCycleLog);
        } catch (Exception ex) {
            log.error("failed to execute the request");
        }
//        String reqParam = HttpUtils.getRequestBody(request);

        return true;
    }

    private Long getUserId(String header) {
        //method to get user id
        return 10202l;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            @Nullable ModelAndView modelAndView) throws Exception {
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, @Nullable Exception ex) throws Exception {
        lifecycleDispatcher.recordLog(request, response, ex);
    }
}
