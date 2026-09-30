/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.websocket.service;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author YYX
 * @version 1.0
 */
public class SpringWebSocketHandlerInterceptor extends HttpSessionHandshakeInterceptor {
  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
          Map<String, Object> attributes) throws Exception {
    System.out.println("Before Handshake");
    if (request instanceof ServletServerHttpRequest) {
      ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
      HttpSession session = servletRequest.getServletRequest().getSession(false);
      if (session != null) {
        //使用userName区分WebSocketHandler，以便定向发送消息
        String userName = (String) session.getAttribute("SESSION_USERNAME");  //一般直接保存user实体
        if (userName!=null) {
          attributes.put("WEBSOCKET_USERID",userName);
        }

      }
    }
    return super.beforeHandshake(request, response, wsHandler, attributes);
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
          Exception ex) {
    super.afterHandshake(request, response, wsHandler, ex);
  }
}