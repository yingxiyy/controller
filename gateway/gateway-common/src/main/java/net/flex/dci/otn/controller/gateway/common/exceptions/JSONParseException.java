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
 * @date: 2021/4/12
 */
public class JSONParseException extends RuntimeException {

    public JSONParseException(Exception ex) {
        super(ex);
    }

    public JSONParseException(String message, Exception ex) {
        super(message, ex);
    }

    public JSONParseException(String s) {
        super(s);
    }
}
