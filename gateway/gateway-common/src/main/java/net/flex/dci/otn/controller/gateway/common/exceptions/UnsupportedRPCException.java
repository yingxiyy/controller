/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.exceptions;

/**
 * @author: xinyzhao
 * @date: 2021/3/25
 */
public class UnsupportedRPCException extends RuntimeException {

    public UnsupportedRPCException(String message) {
        super(message);
    }

    public UnsupportedRPCException(String message, Exception ex) {
        super(message, ex);
    }

    protected UnsupportedRPCException(String message, Throwable cause, boolean enableSuppression,
            boolean writeableStackTrace) {
        super(message, cause, enableSuppression, writeableStackTrace);
    }
}
