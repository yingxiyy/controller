/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.exceptions;

/**
 * exception for the authorization exception
 *
 * @author: xinyzhao
 * @date: 2021/3/23
 */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable ex) {
        super(message, ex);
    }

    public AuthorizationException(Throwable cause) {
        super(cause);
    }

    protected AuthorizationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
