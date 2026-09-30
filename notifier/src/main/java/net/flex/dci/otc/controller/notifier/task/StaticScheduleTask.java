/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.notifier.task;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.notifier.core.websocket.WebSocketServerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/10/27 13:30
 */
@Component
@EnableScheduling
@Slf4j
public class StaticScheduleTask {

    @Autowired
    private WebSocketServerManager webSocketServer;

    /**
     * to make sure the websocket is not terminated;
     *
     * @throws Exception
     */
    @Scheduled(fixedRate = 58 * 1000)
    private void pingPong() throws Exception {
        log.debug("start to send websocket heartbeat");
        webSocketServer.sendMessage("PING");
    }
}
