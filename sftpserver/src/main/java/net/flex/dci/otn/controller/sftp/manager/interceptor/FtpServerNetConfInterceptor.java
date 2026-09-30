/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.interceptor;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.sftp.manager.service.SftpServerTreeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;


@Component
@Slf4j
public class FtpServerNetConfInterceptor implements HandlerInterceptor {

    @Autowired
    private SftpServerTreeService service;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws CommonException {
        log.debug("get sftp server request start.....");
        RequestMethod requestMethod = getRequestMethod(handler);
        if (!requestMethod.equals(RequestMethod.GET)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the api not support request Method:" + requestMethod.name());
        }
        postInterceptor(request, response);
        return false;
    }

    private void postInterceptor(HttpServletRequest request, HttpServletResponse response) {
        try {
            String returnValue = service.executeRequest(request, response);
            sendJsonResponse(response, returnValue);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    private void sendJsonResponse(HttpServletResponse response, String returnValue)
            throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.print(returnValue);
        writer.close();
        response.flushBuffer();
    }

    private RequestMethod getRequestMethod(Object handler) {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        RequestMethod[] requestMethod = requestMapping
                .method();
        return requestMethod[0];
    }
}
