/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.webapp;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2021/12/9 15:06
 */
@Data
public class Result implements Serializable {

    private int code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object content;

    public Result(int code, String message, Object content) {
        this.code = code;
        this.message = message;
        this.content = content;
    }

    public Result(ResultStatus status) {
        this.code = status.getCode();
        this.message = status.getMessage();
    }

    public Result(ResultStatus status, Object content) {
        this.code = status.getCode();
        this.message = status.getMessage();
        this.content = content;
    }

    public static Result ok() {
        return new Result(ResultStatus.SUCCESS, null);
    }

    public static Result ok(Object content) {
        return new Result(ResultStatus.SUCCESS, content);
    }

    public static Result nok() {
        return new Result(ResultStatus.FAILED, null);
    }

    public static Result nok(Object content) {
        return new Result(ResultStatus.FAILED, content);
    }

}
