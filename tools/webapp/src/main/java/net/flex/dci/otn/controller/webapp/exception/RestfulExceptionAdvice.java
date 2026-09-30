/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.webapp.exception;

import net.flex.dci.otc.common.exception.CommonException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 2021/10/31
 *
 * @version 1.0
 **/
@RestControllerAdvice(annotations = RestController.class)
public class RestfulExceptionAdvice extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse.ApiErrorResponseBuilder()
                .withStatus(status)
                .withErrorCode(String.valueOf(status.value()))
                .withMessage(ex.getLocalizedMessage())
                .build();

        return new ResponseEntity<>(apiErrorResponse, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status,
            WebRequest request) {
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse.ApiErrorResponseBuilder()
                .withStatus(status)
                .withErrorCode(String.valueOf(status.value()))
                .withMessage(ex.getLocalizedMessage())
                .build();

        return new ResponseEntity<>(apiErrorResponse, status);
    }

    @ExceptionHandler({CommonException.class})
    protected ResponseEntity<Object> handleCommonException(CommonException ex) {
        ApiErrorResponse apiErrorResponse = new ApiErrorResponse.ApiErrorResponseBuilder()
                .withStatus(ex.getType().getHttpStatus())
                .withErrorCode(ex.getType().getValue())
                .withMessage(ex.getDetail())
                .build();
        return new ResponseEntity<>(apiErrorResponse, ex.getType().getHttpStatus());
    }


    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatus status,
            WebRequest request) {
        ApiErrorResponse response = new ApiErrorResponse.ApiErrorResponseBuilder()
                .withStatus(status)
                .withErrorCode(HttpStatus.NOT_ACCEPTABLE.name())
                .withMessage(ex.getLocalizedMessage())
                .withDetail(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, status);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        ApiErrorResponse response = new ApiErrorResponse.ApiErrorResponseBuilder()
                .withStatus(status)
                .withErrorCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                .withMessage(ex.getLocalizedMessage())
                .withDetail(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, status);

    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(
            ConversionNotSupportedException ex, HttpHeaders headers, HttpStatus status,
            WebRequest request) {
        ApiErrorResponse response = new ApiErrorResponse.ApiErrorResponseBuilder()
                .withStatus(status)
                .withErrorCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .withMessage(ex.getLocalizedMessage())
                .withDetail(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, status);
    }


    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status,
            WebRequest request) {
//        return this.handleExceptionInternal(ex, (Object) null, headers, status, request);

        ApiErrorResponse response = new ApiErrorResponse.ApiErrorResponseBuilder()
                .withStatus(status)
                .withErrorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                .withMessage(ex.getLocalizedMessage())
                .withDetail(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
