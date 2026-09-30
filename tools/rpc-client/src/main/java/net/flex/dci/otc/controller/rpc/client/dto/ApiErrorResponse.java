/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * 2021/10/31
 *
 * @version 1.0
 **/
@Data
public class ApiErrorResponse {

    private Integer status;

    @JsonInclude(Include.NON_NULL)
    private String details;

    private String error_code;

    private String message;

    private ApiErrorResponse() {

    }


    public static final class ApiErrorResponseBuilder {

        private HttpStatus status;


        private String details;

        private String error_code;

        private String message;

        public ApiErrorResponseBuilder() {

        }

        public static ApiErrorResponseBuilder apiErrorResponseBuilder() {
            return new ApiErrorResponseBuilder();
        }

        public ApiErrorResponseBuilder withStatus(HttpStatus status) {
            this.status = status;
            return this;
        }

        public ApiErrorResponseBuilder withDetail(String details) {
            this.details = details;
            return this;
        }

        public ApiErrorResponseBuilder withErrorCode(String errorCode) {
            this.error_code = errorCode;
            return this;
        }

        public ApiErrorResponseBuilder withMessage(String message) {
            this.message = message;
            return this;
        }

        public ApiErrorResponse build() {
            ApiErrorResponse apiErrorResponse = new ApiErrorResponse();
            apiErrorResponse.error_code = this.error_code;
            apiErrorResponse.status = this.status.value();
            apiErrorResponse.message = this.message;
            apiErrorResponse.details = this.details;
            return apiErrorResponse;
        }

    }


}
