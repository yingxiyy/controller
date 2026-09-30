/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.interceptor;

import java.io.IOException;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * @date: 2021/3/26
 */
@Deprecated
public interface IBaseInterceptorService {

    String executeRequest(ServerHttpRequest request, ServerHttpResponse response)
            throws IOException;


}
