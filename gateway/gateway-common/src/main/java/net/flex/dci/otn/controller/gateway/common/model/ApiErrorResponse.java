package net.flex.dci.otn.controller.gateway.common.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 2022/2/14 15:19
 */
@Data
@Builder
public class ApiErrorResponse {


    private Integer status;

    private String details;

    private String error_code;

    private String message;

    @Tolerate
    private ApiErrorResponse() {

    }
}
