/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.websocket.nbi;

import net.flex.dci.otn.controller.tools.websocket.service.SpringWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Service
public class WebSocketService{

  @Bean//这个注解会从Spring容器拿出Bean
  public SpringWebSocketHandler infoHandler() {
    return new SpringWebSocketHandler();
  }

  public void send(String message) {
    infoHandler().sendMessageToUsers(new TextMessage(message));
  }
}
