/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.webapp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;


/**
 * @version 1.0
 */
@Component
@Slf4j
public class LoggingFilter implements Filter {

    private final String POINT_EXCLUSION_PATTERN = "^([^.]+)$";

    private final List<String> STREAMING_KEYWORDS = Arrays.asList("export", "download",
            "download_template");

    private final List<String> STREAMING_PATH_SUFFIXES = Arrays.asList(".zip", ".xlsx", ".csv");


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;

        String requestURI = servletRequest.getRequestURI();
        String contextPath = servletRequest.getContextPath();
        String fullRequestURI = contextPath + requestURI;

        corsSetting(servletRequest, servletResponse);
        if (requestURI.equals("/") || requestURI.equals("")) {
//        if(!requestURI.equals(contextPath) &&
//                !RequestAttributes.isAPI(servletRequest) &&
//                requestURI.matches(POINT_EXCLUSION_PATTERN) // Check if there are no "." in requested URL
//        ) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("/index.html");
            dispatcher.forward(request, response);
            return;
        }
        boolean isStreamingRequest = isStreamingRequest(requestURI, fullRequestURI);
        if (isStreamingRequest) {
            logStreamingRequest(servletRequest, servletResponse, chain);
        } else {
            logNormalRequest(servletRequest, servletResponse, chain);
        }
    }


    /**
     * detective current streaming request is streaming request or not
     *
     * @param requestURI
     * @param fullRequestURI
     * @return
     */
    private boolean isStreamingRequest(String requestURI,
            String fullRequestURI) {
        String lowerURI = requestURI.toLowerCase();
        boolean matchKeyword = STREAMING_KEYWORDS.stream()
                .anyMatch(keyword -> lowerURI.contains("/" + keyword + "/")
                        || lowerURI.endsWith("/" + keyword) || lowerURI.contains(keyword));

        boolean matchSuffix = STREAMING_PATH_SUFFIXES.stream()
                .anyMatch(suffix -> requestURI.endsWith(suffix) || fullRequestURI.endsWith(suffix));

        return matchKeyword || matchSuffix;
    }

    protected void corsSetting(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN/*"Access-Control-Allow-Origin"*/,
                request.getHeader("Origin"));
        //允许浏览器携带cookie
//        response.setHeader("Access-Control-Allow-Credentials","true");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        //response.setHeader("Access-Control-Allow-Headers", "token");
//        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type,Access-Token");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
//        response.setHeader("Access-Control-Allow-Headers", "Content-Type,Access-Token");
//        response.setHeader("Access-Control-Expose-Headers", "*");
    }

    private void logStreamingRequest(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Instant start = Instant.now();

        filterChain.doFilter(request, response);
        Instant end = Instant.now();
        log.debug("HTTP [{}] {} {} {}, take: {}ms (stream download，do not read request body)",
                request.getAttribute(RequestAttributes.RequestID),
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                Duration.between(start, end).toMillis());
    }

    protected void logNormalRequest(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        Instant start = Instant.now();
        filterChain.doFilter(requestWrapper, responseWrapper);
        Instant end = Instant.now();

        String requestBody = readRequest(requestWrapper);
        String responseBody = new String(responseWrapper.getContentAsByteArray(),
                StandardCharsets.UTF_8);
        responseWrapper.copyBodyToResponse();

        if (RequestAttributes.isAPI(request) && (
                request.getMethod().equals(HttpMethod.POST.name()) ||
                        request.getMethod().equals(HttpMethod.PUT.name()) ||
                        request.getMethod().equals(HttpMethod.PATCH.name()))) {
            log.debug("HTTP [{}] {} {} {}, take: {}ms\n requestBody: {}\n responseBody: {}\n",
                    request.getAttribute(RequestAttributes.RequestID),
                    request.getMethod(),
                    request.getRequestURI(),
                    responseWrapper.getStatus(),
                    Duration.between(start, end).toMillis(),
                    requestBody.length() > 1000 ? requestBody.substring(0, 250) + "......"
                            : requestBody,
                    responseBody.length() > 1000 ? responseBody.substring(0, 250) + "......"
                            : responseBody);
        } else {
            log.debug("HTTP [{}] {} {} {}, take: {}ms\n ",
                    request.getAttribute(RequestAttributes.RequestID),
                    request.getMethod(),
                    request.getRequestURI(),
                    responseWrapper.getStatus(),
                    Duration.between(start, end).toMillis());
        }
    }

    private String readRequest(ContentCachingRequestWrapper request) {
        return new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
