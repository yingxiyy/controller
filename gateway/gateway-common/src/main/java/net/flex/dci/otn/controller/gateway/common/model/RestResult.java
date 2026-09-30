/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.model;

import java.io.Serializable;
import lombok.Data;

/**
 * @author:
 * @date: 2021/3/23
 */
@Data
public class RestResult implements Serializable {

    private Integer code;
    private String detail;
    private String message;

    public RestResult() {

    }

    public RestResult(Integer code, String detail, String message) {
        this.code = code;
        this.detail = detail;
        this.message = message;
    }


}
