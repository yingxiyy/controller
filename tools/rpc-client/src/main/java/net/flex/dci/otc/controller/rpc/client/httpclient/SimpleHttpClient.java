/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.httpclient;


import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.utils.RpcConstants;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * @version 1.0
 * @date 2021/8/15 21:06
 */
@Slf4j
public class SimpleHttpClient {

    private final static int TIME_OUT = 1200000;
    private final static int MAX_CONNECTIONS = 500;


    private AsyncHttpClient asyncHttpClient;

    @Setter
    @Getter
    private String authorization;


    public SimpleHttpClient() {
        SslContext sslContext = null;
        try {
            sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            AsyncHttpClientConfig config = Dsl.config()
                    .setMaxConnections(MAX_CONNECTIONS)
                    .setMaxConnectionsPerHost(MAX_CONNECTIONS)
                    .setConnectTimeout(30000)
                    .setRequestTimeout(TIME_OUT)
                    .setReadTimeout(TIME_OUT)
                    .setSslSessionTimeout(TIME_OUT)
                    .setMaxRequestRetry(3)
                    .setIoThreadsCount(Runtime.getRuntime().availableProcessors() * 2)
                    .setSslContext(sslContext)
                    .setThreadPoolName("ASYNC-HTTP-CLIENT")
                    .build();
            asyncHttpClient = new DefaultAsyncHttpClient(config);
        } catch (SSLException e) {
            log.error("failed to init ssl context,the reason is {}", e.getMessage(), e.getCause());
        }
    }

    public String get(String url) throws ExecutionException, InterruptedException, IOException {
        return get(url, null);
    }

    /**
     * http method for get
     *
     * @param url
     * @return
     */
    public String get(String url, Long requestTimeout)
            throws ExecutionException, InterruptedException, IOException {
        RequestBuilder rb = Dsl.get(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (!StringUtils.isEmpty(authorization)) {
            rb.setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        Request request = rb.build();
        return execute(request, requestTimeout);
    }


    public Response getResponse(String url)
            throws IOException, ExecutionException, InterruptedException {
        return getResponse(url, null);
    }

    /**
     * http method for get
     *
     * @param url
     * @return
     */
    public Response getResponse(String url, Long requestTimeout)
            throws ExecutionException, InterruptedException, IOException {

        RequestBuilder rb = Dsl.get(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (!StringUtils.isEmpty(authorization)) {
            rb.setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }

        Request request = rb.build();
        return executeReq(request, requestTimeout);
    }


    public String post(String url, String body) throws ExecutionException, InterruptedException {
        return post(url, body, null);
    }

    /**
     * http method for post
     *
     * @param url
     * @param body
     * @return
     */
    public String post(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        Request request = Dsl.post(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        return execute(request, requestTimeout);
    }

    public Response postReq(String url, String body)
            throws ExecutionException, InterruptedException {
        return postReq(url, body, null);
    }

    public Response postReq(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        Request request = Dsl.post(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        return executeReq(request, requestTimeout);
    }

    public String futurePost(String url, String body)
            throws ExecutionException, InterruptedException {
        return futurePost(url, body, null);
    }

    public String futurePost(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        CompletableFuture<Response> whenResponse = asyncHttpClient
                .preparePost(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .execute()
                .toCompletableFuture()
                .exceptionally(t -> {
                    log.error("failed to execute the request url ,ex: {}", t);
                    return (Response) t;
                })
                .thenApply(response -> {
                    return response;
                });
        whenResponse.join(); // wait for completion
        return whenResponse.get().getResponseBody();
    }


    public Response futurePostReq(String url, String body)
            throws ExecutionException, InterruptedException {
        return futurePostReq(url, body, null);
    }

    public Response futurePostReq(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        CompletableFuture<Response> whenResponse = asyncHttpClient
                .preparePost(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .execute()
                .toCompletableFuture()
                .exceptionally(t -> {
                    log.error("failed to execute the request url ,ex: {}", t.getMessage(), t);
                    return (Response) t;
                })
                .thenApply(response -> {
                    return response;
                });
        whenResponse.join(); // wait for completion
        return whenResponse.get();
    }


    public String put(String url, String body) throws ExecutionException, InterruptedException {
        return put(url, body, null);
    }

    /**
     * support http method for put
     *
     * @param url
     * @param body
     * @return
     */
    public String put(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        Request request = Dsl.put(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        return execute(request, requestTimeout);
    }


    public String delete(String url, String body) throws ExecutionException, InterruptedException {
        return delete(url, body, null);
    }

    /**
     * execute the delete method
     *
     * @param url
     * @param body
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public String delete(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        Request request = Dsl.delete(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        return execute(request, requestTimeout);
    }


    public Response postBody(String url, String body)
            throws ExecutionException, InterruptedException {
        return postBody(url, body, null);
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
    public Response postBody(String url, String body, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        Request request = Dsl.post(url)
                .setHeader(HttpHeaders.USER_AGENT, RpcConstants.USER_AGENT)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        return executeResponse(request, requestTimeout);
    }

    /**
     * execute the request for http method
     *
     * @param request
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private String execute(Request request, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        Response rsp = executeResponse(request, requestTimeout);
        if (rsp.getStatusCode() == HttpStatus.OK.value()) {
            return rsp.getResponseBody();
        } else {
            throw new RestClientException(
                    "Failed to get response from " + rsp.getUri().getPath() + " resp is :"
                            + rsp.getResponseBody());
        }
    }

    private Response executeReq(Request request, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        Response rsp = executeResponse(request, requestTimeout);
        return rsp;
    }

    private Response executeResponse(Request request)
            throws ExecutionException, InterruptedException {
//        try {
        Response res = this.asyncHttpClient.executeRequest(request).get();
        return res;
//        } finally {
//
//        }
    }


    private Response executeResponse(Request request, Long requestTimeout)
            throws ExecutionException, InterruptedException {
        long timeout = requestTimeout != null ? requestTimeout : TIME_OUT;
//        try {
//            return this.asyncHttpClient.executeRequest(request).get(timeout, TimeUnit.MILLISECONDS);
//        } catch (TimeoutException e) {
//            log.error("Request timed out after {} ms: {}", timeout, request.getUrl());
//            throw new ExecutionException("Request timed out", e);
//        }
        if (requestTimeout != null && requestTimeout > 0) {
            request = request.toBuilder()
                    .setRequestTimeout(requestTimeout.intValue())
                    .setReadTimeout(requestTimeout.intValue())
                    .build();
        }

        org.asynchttpclient.ListenableFuture<Response> future = null;
        try {
            future = this.asyncHttpClient.executeRequest(request);
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("Request timed out after {} ms: {}", timeout, request.getUrl());
            if (future != null) {
                future.cancel(true);
            }
            throw new ExecutionException("Request timed out", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SimpleHttpClient");
        try {
            if (!asyncHttpClient.isClosed()) {
                asyncHttpClient.close();
            }
        } catch (IOException e) {
            log.error("Failed to close AsyncHttpClient", e);
        }
    }

}
