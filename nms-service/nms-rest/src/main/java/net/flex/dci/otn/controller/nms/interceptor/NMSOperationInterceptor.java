/*
 * Copyright (c) 2019 Network flex Any comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.interceptor;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.service.impl.NMSOperationsService;
import net.flex.dci.otn.controller.nms.utils.SendResponseUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * INTERCEPTOR FOR RPC REQUEST
 *
 * @date: 2021/3/24
 */
@Slf4j
@Component
public class NMSOperationInterceptor extends BaseInterceptor<NMSOperationsService> implements
        HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws IOException {
        log.info("send operations execute command start ...");
        long start = System.currentTimeMillis();
        postInterceptor(request, response, handler);
        long duration = System.currentTimeMillis() - start;
        log.info("Completed request uri={}, duration={} ms", request.getRequestURI(), duration);
        return false;
    }

    /**
     * post handler for interceptor
     *
     * @param request
     * @param response
     * @param handler
     */
    protected void postInterceptor(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws IOException {

        String returnValue = service.executeRequest(request, response);
        SendResponseUtils.sendResponse(request, response, returnValue);

    }

}
