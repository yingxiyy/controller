/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.notifier.core.websocket;

import java.io.IOException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.notifier.core.NotifierApplicationContextAware;
import org.springframework.stereotype.Component;

@Getter
@ServerEndpoint("/create-notification-stream/{sid}")
@Component
@Slf4j
public class WebSocketServer {

    private Session session;


    //接收sid
    private String sid = "";

    @Override
    public boolean equals(Object o) {
        if (o instanceof WebSocketServer) {
            WebSocketServer other = (WebSocketServer) o;
            if (this.sid == other.sid) {
                return true;
            }
        }
        return false;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("a new connection for sid:{},session is:{}", sid, session.getId());
        this.session = session;
        WebSocketServerManager webSocketServerManager = NotifierApplicationContextAware
                .getApplicationContext()
                .getBean(WebSocketServerManager.class);
        this.sid = sid;
        webSocketServerManager.addWebSocketServer(this);
        try {
            sendMessage("connection success!");
        } catch (IOException e) {
            log.error("websocket IO异常");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到来自窗口" + sid + "的信息:" + message);
    }

    @OnClose
    public void OnClose() {
        try {
            session.close();
            WebSocketServerManager webSocketServerManager = NotifierApplicationContextAware
                    .getApplicationContext()
                    .getBean(WebSocketServerManager.class);
            webSocketServerManager.removeWebSocketServer(this);
        } catch (IOException ex) {
            log.error("failed to delete session,the reason is {}", ex.getMessage(), ex);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("exception is :{} for the session:{}", error.getMessage(), session, error);
        error.printStackTrace();
    }

    public void sendMessage(Object object) {
        try {
            String jsonMessage = (String) object;

            synchronized (WebSocketServer.class) {
                sendMessage(jsonMessage);
            }

        } catch (IOException e) {
            log.error("failed to send web socket message,the reason is {}", e.getMessage());
        }
    }


    private void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
}