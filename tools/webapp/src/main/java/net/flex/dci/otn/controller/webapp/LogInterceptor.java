/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.webapp;

import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {
  private AtomicLong id = new AtomicLong(1);

  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (RequestAttributes.isAPI(request)) {
      request.setAttribute(RequestAttributes.RequestID, id.incrementAndGet());
    }
    return true;
  }

  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
    if (RequestAttributes.isAPI(request)) {
      response.setContentType("application/json");
    }
//    HandlerMethod handlerMethod = (HandlerMethod) handler;
//    WebConfiguration configuration = handlerMethod.getBean().getClass().getAnnotation(WebConfiguration.class);
  }
}
