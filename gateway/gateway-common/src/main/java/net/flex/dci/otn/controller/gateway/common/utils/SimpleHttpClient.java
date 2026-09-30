/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.utils;


import com.google.common.net.HttpHeaders;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLException;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

/**
 * ASYNC HTTP CLIENT FOR HTTP REQUEST
 *
 * @author: xinyzhao
 * @date: 2021/3/24
 */
@Slf4j
public class SimpleHttpClient {

    private final static int TIME_OUT = 12000;
    private final static int MAX_CONNECTIONS = 50;
    private AsyncHttpClient asyncHttpClient;

    private String authorization;

    public SimpleHttpClient() {
        SslContext sslContext = null;
        try {
            sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            AsyncHttpClientConfig config = Dsl.config()
                    .setMaxConnections(MAX_CONNECTIONS)
                    .setMaxConnectionsPerHost(MAX_CONNECTIONS)
                    .setConnectTimeout(TIME_OUT)
                    .setAcquireFreeChannelTimeout(TIME_OUT)
                    .setReadTimeout(TIME_OUT)
                    .setShutdownTimeout(TIME_OUT)
                    .setSslSessionTimeout(TIME_OUT)
                    .setMaxRequestRetry(3)
                    .setSslContext(sslContext)
                    .build();
            asyncHttpClient = new DefaultAsyncHttpClient(config);
        } catch (SSLException e) {
            log.error("failed to init ssl context,the reason is {}", e.getCause());
        }
    }

    /**
     * http method for get
     *
     * @param url
     * @return
     */
    public String get(String url) throws ExecutionException, InterruptedException {
        RequestBuilder rb = Dsl.get(url)
                .setHeader(HttpHeaders.USER_AGENT, Constants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (!StringUtils.isEmpty(authorization)) {
            rb.setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        Request request = rb.build();
        return execute(request);
    }

    /**
     * http method for post
     *
     * @param url
     * @param body
     * @return
     */
    public String post(String url, String body) throws ExecutionException, InterruptedException {
        Request request = Dsl.post(url)
                .setHeader(HttpHeaders.USER_AGENT, Constants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        return execute(request);
    }

    /**
     * get response body
     *
     * @param url
     * @param body
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Response postBody(String url, String body)
            throws ExecutionException, InterruptedException {
        Request request = Dsl.post(url)
                .setHeader(HttpHeaders.USER_AGENT, Constants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        return executeResponse(request);
    }

    /**
     * execute the request for http method
     *
     * @param request
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private String execute(Request request) throws ExecutionException, InterruptedException {
        Response rsp = executeResponse(request);
        if (rsp.getStatusCode() == 200) {
            return rsp.getResponseBody();
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to get response from " + rsp.getUri().getPath());
        }
    }

    private Response executeResponse(Request request)
            throws ExecutionException, InterruptedException {
        Response res = this.asyncHttpClient.executeRequest(request).get();
        return res;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }
}
