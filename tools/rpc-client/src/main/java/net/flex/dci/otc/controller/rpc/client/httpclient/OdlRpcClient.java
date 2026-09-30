/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.httpclient;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.utils.RpcUtils;
import org.asynchttpclient.Response;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/15 21:23
 */
@Slf4j
public class OdlRpcClient {

    private final SimpleHttpClient simpleHttpClient;
    private Long connectionTimeout;
    private Long readTimeout;

    public OdlRpcClient(SimpleHttpClient simpleHttpClient) {
        this.simpleHttpClient = simpleHttpClient;
    }

    public OdlRpcClient setConfig(OdlRpcClientConfig config) {
        if (config.getUser() != null && config.getPassword() != null) {
            this.simpleHttpClient.setAuthorization(
                    RpcUtils.basicAuthorization(config.getUser(), config.getPassword()));
        }
        this.connectionTimeout = config.getConnectionTimeout();
        this.readTimeout = config.getReadTimeout();
        return this;
    }


    /**
     * method for odl netconf get
     *
     * @return
     */
    public String get(String url) throws ExecutionException, InterruptedException, IOException {
        log.debug("start to handle the method for the get :{}", url);
        String result = simpleHttpClient.get(url);
        log.debug("the result is {}", result);
        return result;
    }

    public Response getRes(String url)
            throws ExecutionException, InterruptedException, IOException {
        log.debug("start to handle the method for the get :{}", url);
        Response result = simpleHttpClient.getResponse(url);
        log.debug("the result is {}", result);
        return result;
    }


    /**
     * method fot odl netconf post
     *
     * @return
     */
    public String post(String url, String body)
            throws ExecutionException, InterruptedException {
        log.debug("start to handle the method for the post :{} body is {}", url, body);
        String result = simpleHttpClient.post(url, body);
        log.debug("the result is {}", result);
        return result;
    }

    public String post(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        log.debug("start to handle the method for the post :{} body is {} requestTimeout:{}", url,
                body, requestTimeout);
        String result = simpleHttpClient.post(url, body, requestTimeout);
        log.debug("the result is {}", result);
        return result;
    }

    public Response postReq(String url, String body)
            throws ExecutionException, InterruptedException {
        log.debug("start to handle the method for the post :{} body is {}", url, body);
        Response result = simpleHttpClient.postReq(url, body);
        log.debug("the result is {}", result);
        return result;
    }

    public Response postReq(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        log.debug("start to handle the method for the post :{} body is {}", url, body);
        Response result = simpleHttpClient.postReq(url, body, requestTimeout);
        log.debug("the result is {}", result);
        return result;
    }

    public String longTimePost(String url, String body)
            throws ExecutionException, InterruptedException {
        log.debug("start to handle the method for the long post :{} body is {}", url, body);
        String result = simpleHttpClient.futurePost(url, body);
        log.debug("the result is {}", result);
        return result;
    }

    /**
     * method for odl netconf delete
     *
     * @return
     */
    public DataObject delete() {
        return null;
    }

    /**
     * method for odl netconf put
     *
     * @return
     */
    public DataObject put() {
        return null;
    }

    /**
     * execute odl method for http method
     *
     * @return
     */
    private DataObject executeRequest() {
        return null;
    }
}
