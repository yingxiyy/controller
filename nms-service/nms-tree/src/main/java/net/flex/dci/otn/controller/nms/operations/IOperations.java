/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.operations;


import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * operations executors interface
 *
 * @date: 2021/3/30
 */
public interface IOperations {

    default String executeRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return null;
    }


    default String executeRemoteRequest(ServerHttpRequest request,
                                        ServerHttpResponse response) {
        return null;
    }

}
