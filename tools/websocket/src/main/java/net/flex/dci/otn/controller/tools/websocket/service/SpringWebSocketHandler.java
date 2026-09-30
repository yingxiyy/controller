/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.websocket.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class SpringWebSocketHandler extends TextWebSocketHandler {

  //Map来存储WebSocketSession，key用USER_ID 即在线用户列表
  private static final Map<String, WebSocketSession> users =  new HashMap<String, WebSocketSession>();

  public SpringWebSocketHandler() {}

  /**
   * 连接成功时候，会触发页面上onopen方法
   */
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {

    String userId = getUserID(session);
    log.debug(userId + " 成功建立websocket连接!");
    users.put(userId,session);
    log.debug("当前线上用户数量:"+users.size());
  }


  public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
    String userId= getUserID(session);
    log.debug(userId + " 用户已退出！");
    users.remove(userId);
    log.debug("剩余在线用户"+users.size());
  }

  /**
   * js调用websocket.send时候，会调用该方法
   */
  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

    super.handleTextMessage(session, message);

    /**
     * 收到消息，自定义处理机制，实现业务
     */
    log.debug("服务器收到消息："+message);
    sendMessageToUsers(new TextMessage("I get client message: " + message.getPayload()));
  }

  private String getUserID(WebSocketSession session) {
    return session.getId();
  }

  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    if(session.isOpen()){
      session.close();
    }
    String userId= getUserID(session);
    log.debug(userId + " 传输出现异常，关闭websocket连接... ");
    users.remove(userId);
  }

  public boolean supportsPartialMessages() {
    return false;
  }

  public void sendMessageToUser(String toUser, TextMessage message) {
    Iterator<String> iter = users.keySet().iterator();
    while (iter.hasNext()) {
      String userId = iter.next();
      if (userId.equals(toUser)) {
        try {
          if (users.get(userId).isOpen()) {
            users.get(userId).sendMessage(message);
          }
        } catch (IOException e) {
          users.remove(userId);
          log.error(userId + " send web socket message error. remove it");
        }
        break;
      }
    }
  }

  public void sendMessageToUsers(TextMessage message) {
    Iterator<String> iter = users.keySet().iterator();
    while (iter.hasNext()) {
      String userId = iter.next();
      try {
        if (users.get(userId).isOpen()) {
          users.get(userId).sendMessage(message);
        }
      } catch (IOException e) {
        users.remove(userId);
        log.error(userId + " send web socket message error. remove it", e);
      }
    }
  }

}
