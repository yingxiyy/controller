/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.websocket;

import net.flex.dci.otn.controller.tools.websocket.nbi.WebSocketService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author YYX
 * @version 1.0
 */
@Controller
public class WebSocketController {
  @Autowired
  private WebSocketService websocket;
  /**
   * for test webSocket
   * @param request
   * @return
   */
  @RequestMapping("/websocket/send")
  @ResponseBody
  public String send(HttpServletRequest request) {
    websocket.send("给所有用户发消息, sending message to all");
    return null;
  }

}
