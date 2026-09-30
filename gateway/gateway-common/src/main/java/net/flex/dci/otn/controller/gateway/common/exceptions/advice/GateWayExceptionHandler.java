/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.exceptions.advice;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.gateway.common.exceptions.AuthorizationException;
import net.flex.dci.otn.controller.gateway.common.model.ApiErrorResponse;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @date: 2021/3/23
 */
@Slf4j
public class GateWayExceptionHandler extends DefaultErrorWebExceptionHandler {


    public GateWayExceptionHandler(
            ErrorAttributes errorAttributes,
            Resources resources,
            ErrorProperties errorProperties,
            ApplicationContext applicationContext) {
        super(errorAttributes, resources, errorProperties, applicationContext);
    }

    @Override
    public int getHttpStatus(Map<String, Object> errorAttributes) {
        return super.getHttpStatus(errorAttributes);
    }

    @Override
    public RouterFunction<ServerResponse> getRoutingFunction(
            ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    public static Map<String, Object> response(int status, String errorMsg) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", status);
        map.put("message", errorMsg);
        return map;
    }


    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request,
            ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = new LinkedHashMap<>();
        Throwable error = getError(request);
        log.error("error occur is :{}", error.getMessage(), error);
        MergedAnnotation<ResponseStatus> responseStatusAnnotation = MergedAnnotations
                .from(error.getClass(), MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .get(ResponseStatus.class);
        handleException(errorAttributes, determineException(error));
        return errorAttributes;
    }

    /**
     * @param errorAttributes
     * @param error
     */
    private void handleException(Map<String, Object> errorAttributes, ApiErrorResponse error) {
        errorAttributes.put("status", error.getStatus());
        errorAttributes.put("error_code", error.getError_code());
        if (error.getDetails() != null) {
            errorAttributes.put("detail", error.getDetails());
        }
        errorAttributes.put("message", error.getMessage());

    }

    /**
     * @param error
     * @return
     */
    private ApiErrorResponse determineException(Throwable error) {
        ApiErrorResponse resp = null;
        if (error instanceof CommonException) {
            CommonException ex = (CommonException) error;
            resp = ApiErrorResponse.builder()
                    .status(ex.getType().getHttpStatus().value())
                    .error_code(ex.getType().getValue())
                    .message(ex.getDetail())
                    .build();
        } else if (error instanceof AuthorizationException) {
            resp = ApiErrorResponse.builder().error_code(HttpStatus.UNAUTHORIZED.name())
                    .message(error.getMessage()).build();
        }
        return resp;
    }

    /**
     * determine the response
     *
     * @param error
     * @param responseStatusAnnotation
     * @return
     */
    private HttpStatus determineHttpStatus(Throwable error,
            MergedAnnotation<ResponseStatus> responseStatusAnnotation) {
        if (error instanceof AuthorizationException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (error instanceof CommonException) {
            CommonException ex = (CommonException) error;

            return ex.getType().getHttpStatus();
        }
        return responseStatusAnnotation.getValue("code", HttpStatus.class)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
