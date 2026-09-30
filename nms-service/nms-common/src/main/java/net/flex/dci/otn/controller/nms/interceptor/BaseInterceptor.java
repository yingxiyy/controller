/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @date: 2021/3/26
 */

public abstract class BaseInterceptor<E extends IBaseInterceptorService> {

    @Autowired
    protected E service;

    /**
     * poster for the interceptor
     *
     * @param request
     * @param response
     * @param handler
     * @throws Exception
     */
    protected abstract void postInterceptor(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception;
}
