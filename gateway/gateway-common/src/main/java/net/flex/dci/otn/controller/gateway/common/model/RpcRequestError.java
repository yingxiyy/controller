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
import net.flex.dci.otc.common.exception.CommonHttpException;

/**
 * @author: xinyzhao
 * @date: 2021/4/26
 */
@Data
public class RpcRequestError implements Serializable {

    private String detail;

    private String message;

    private Integer code;

    public RpcRequestError(CommonHttpException ex) {
        this.code = ex.getCode();
        this.message = ex.getMessage();
        this.detail = ex.getDetail();
    }
}
