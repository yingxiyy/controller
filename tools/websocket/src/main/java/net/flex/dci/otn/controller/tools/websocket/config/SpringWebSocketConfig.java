/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.websocket.config;

import net.flex.dci.otn.controller.tools.websocket.service.SpringWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
@EnableWebSocket
public class SpringWebSocketConfig implements WebSocketConfigurer {

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(webSocketHandler(),"/websocket/notification:object-notification")
//            .addInterceptors(new SpringWebSocketHandlerInterceptor())
            .setAllowedOrigins("*");
//
//   registry.addHandler(webSocketHandler(), "/sockjs/socketServer.do")
//            .addInterceptors(new SpringWebSocketHandlerInterceptor()).withSockJS();
  }

  @Bean
  public TextWebSocketHandler webSocketHandler(){
    return new SpringWebSocketHandler();
  }

}