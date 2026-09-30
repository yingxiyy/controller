/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.service.impl;

import static net.flex.dci.otc.common.constants.Constants.CONFIG_URL_PREFIX;
import static net.flex.dci.otc.serialization.JsonUtil.OPERATIONAL_URL_PREFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.BLANK;
import static net.flex.dci.otn.controller.nms.utils.Constants.CHARSET_UTF8;

import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.component.ITopologyTree;
import net.flex.dci.otn.controller.nms.service.INetworkTopologyService;
import net.flex.dci.otn.controller.nms.tasks.NetconfTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/3/26
 */
@Component
@Slf4j
public class NetworkTopologyService implements INetworkTopologyService {

    @Autowired
    private ITopologyTree ITopologyTree;

    /**
     * to find netconf identifier for the netconf operation
     *
     * @param uri
     * @return
     */
    @SneakyThrows
    private String getNetConfIdentifier(String uri) {
        String identifier = null;
        if (uri.contains(OPERATIONAL_URL_PREFIX)) {
            identifier = uri.replace(OPERATIONAL_URL_PREFIX, BLANK);
        } else if (uri.contains(CONFIG_URL_PREFIX)) {
            identifier = uri.replace(CONFIG_URL_PREFIX, BLANK);
        }
        assert identifier != null;
        identifier = URLDecoder.decode(identifier, CHARSET_UTF8);
        return identifier;
    }

//    /**
//     * get mapping request method
//     *
//     * @param handler
//     * @return
//     */
//    private RequestMethod getRequestMethod(Object handler) {
//        HandlerMethod handlerMethod = (HandlerMethod) handler;
//        Method method = handlerMethod.getMethod();
//        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
//        RequestMethod[] requestMethod = requestMapping
//                .method();
//        return requestMethod[0];
//    }


    @Override
    public String executeRequest(HttpServletRequest request,
            HttpServletResponse response) {

        String uri = request.getRequestURI();
        String identifier = getNetConfIdentifier(uri);
        HttpMethod requestMethod = HttpMethod.resolve(request.getMethod());
        NetconfTask netconfTask = new NetconfTask(requestMethod, request, identifier,
                this.ITopologyTree);
        String returnValue = netconfTask.execute();
        return returnValue;
    }
}
