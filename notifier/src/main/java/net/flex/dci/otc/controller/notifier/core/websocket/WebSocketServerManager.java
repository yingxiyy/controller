/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.notifier.core.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketServerManager {

    private static final String OBJECT_NOTIFICATION_HEADER = "notification:object-notification";
    private static final String BROADCAST_NOTIFICATION_HEADER = "notification:broadcast-notification";
    private static final String ALARM_NOTIFICATION_HEADER = "alarm:alarm-notification";
    private static final String ELEMENT_NOTIFICATION_HEADER = "notification:element-change-notification";
    private static final int onlineCount = 0;
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketServer>> webSocketServerSidMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Session, List<WebSocketServer>> webSocketServerMap = new ConcurrentHashMap<>();


    public void addWebSocketServer(WebSocketServer webSocketServer) {
        String sid = webSocketServer.getSid();
        if (!webSocketServerSidMap.containsKey(sid)) {
            CopyOnWriteArraySet<WebSocketServer> webSocketServers = new CopyOnWriteArraySet<>();
            webSocketServers.add(webSocketServer);
            webSocketServerSidMap.put(sid, webSocketServers);
        } else {
            webSocketServerSidMap.get(sid).add(webSocketServer);
        }
        addSessionMap(webSocketServer);
    }


    public void removeWebSocketServer(WebSocketServer webSocketServer) {
        String sid = webSocketServer.getSid();
        webSocketServerSidMap.get(sid).remove(webSocketServer);
        removeSessionMap(webSocketServer);
    }


    public void sendObjectChangeMessage(String message) throws Exception {
        sendMessage(webSocketServerSidMap.get(OBJECT_NOTIFICATION_HEADER), message);
    }

    public void sendBroadcastMessage(String message) throws Exception {
        sendMessage(webSocketServerSidMap.get(BROADCAST_NOTIFICATION_HEADER), message);
    }

    public void sendAlarmChangeMessage(String message) throws Exception {
        sendMessage(webSocketServerSidMap.get(ALARM_NOTIFICATION_HEADER), message);
    }

    public void sendElementChangeMessage(String message) throws Exception {
        sendMessage(webSocketServerSidMap.get(ELEMENT_NOTIFICATION_HEADER), message);
    }

    private void sendMessage(CopyOnWriteArraySet<WebSocketServer> webSocketServers,
            String message) {
        if (webSocketServers != null && !webSocketServers.isEmpty()) {
            webSocketServers.forEach(webSocketServer -> {
                webSocketServer.sendMessage(message);
            });
        }
    }


    public void sendMessage(String value) throws Exception {
        sendAlarmChangeMessage(value);
        sendObjectChangeMessage(value);
        sendBroadcastMessage(value);
        sendElementChangeMessage(value);
    }

    private void addSessionMap(WebSocketServer webSocketServer) {
        Session session = webSocketServer.getSession();
        List<WebSocketServer> webSocketServers = webSocketServerMap.getOrDefault(session,
                new ArrayList<>());
        webSocketServers.add(webSocketServer);
        webSocketServerMap.put(session, webSocketServers);
        log.debug("online session count is {}", webSocketServerMap.size() / 2);
    }


    private void removeSessionMap(WebSocketServer webSocketServer) {
        Session session = webSocketServer.getSession();
        List<WebSocketServer> webSocketServers = webSocketServerMap.get(session);
        webSocketServers.remove(webSocketServer);
        if (webSocketServers.isEmpty()) {
            webSocketServerMap.remove(session);
        }
        log.debug("online session count is {}", webSocketServerMap.size() / 2);
    }

}
