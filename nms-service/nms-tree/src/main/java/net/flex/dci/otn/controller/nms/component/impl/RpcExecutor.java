/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.component.impl;

import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.httpclient.SimpleHttpClient;
import net.flex.dci.otn.controller.nms.component.IRpcExecutor;
import net.flex.dci.otn.controller.nms.exceptions.RpcException;
import net.flex.dci.otn.controller.nms.properties.Account;
import net.flex.dci.otn.controller.nms.utils.CommonUtils;
import org.asynchttpclient.Response;
import org.springframework.stereotype.Component;

/**
 * Rpc executor service
 *
 * @author: xinyzhao
 * @date: 2021/3/25
 */
@Slf4j
@Component
public class RpcExecutor implements IRpcExecutor {

    private SimpleHttpClient httpClient;

    public RpcExecutor(Account account) {
        httpClient = new SimpleHttpClient();
        String authorization = CommonUtils
                .basicAuthorization(account.getAccount(), account.getPassword());
        httpClient.setAuthorization(authorization);
    }

    @Override
    public String executeRpc(String requestBody, String url)
            throws RpcException, ExecutionException, InterruptedException {
        Response response = null;
        response = httpClient.postBody(url, requestBody);
//        if (response.getStatusCode() != HttpStatus.OK.value()) {
//            String errors = response.getResponseBody();
//            ApiErrorResponse apiError = JSON.parseObject(errors, ApiErrorResponse.class);
//            throw new CommonException(CommonExceptionType.forValue(apiError.getError_code()),
//                    apiError.getMessage());
//        }
        return response.getResponseBody();
    }
}
