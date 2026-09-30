/*
 * Copyright (c) 2019 Network flex Any comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.interceptor;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.nms.service.impl.NetworkTopologyService;
import net.flex.dci.otn.controller.nms.utils.SendResponseUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * interceptor for network topology request include config and operational database
 *
 * @date: 2021/3/26
 */
@Slf4j
@Component
public class NetworkTopologyInterceptor extends BaseInterceptor<NetworkTopologyService> implements
        HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) {
        log.debug("get network topology request start.....");
        long start = System.currentTimeMillis();
        postInterceptor(request, response, handler);
        long duration = System.currentTimeMillis() - start;
        log.info("Completed request uri={}, duration={} ms", request.getRequestURI(), duration);
        return false;
    }

    /**
     * handler the interceptor
     *
     * @param request
     * @param response
     * @param handler
     */
    protected void postInterceptor(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws CommonException, UnsupportedOperationException {

        String returnValue = service.executeRequest(request, response);
        SendResponseUtils.sendResponse(request, response, returnValue);


    }
}
