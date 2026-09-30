/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.exceptions;

import lombok.Data;

/**
 * exception for rpc
 *
 * @author: xinyzhao
 * @date: 2021/4/14
 */
@Data
public class RpcException extends RuntimeException {

    private String moduleName;

    private String message;

    public RpcException() {

    }

    public RpcException(String msg) {
        super(msg);
    }

    public RpcException(String moduleName, String message) {
        this.moduleName = moduleName;
        this.message = message;
    }

    public RpcException(String moduleName, String msg, Throwable cause) {
        super(cause);
        this.moduleName = moduleName;
        this.message = message;
    }
}
