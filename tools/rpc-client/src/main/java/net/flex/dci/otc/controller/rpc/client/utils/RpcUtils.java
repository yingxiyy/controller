/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.utils;

import java.util.Base64;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/24 17:10
 */
public class RpcUtils {

    public static String basicAuthorization(String username, String password) {
        String auth = username + ":" + password;
        String authHeader = "Basic " + Base64.getUrlEncoder().encodeToString(auth.getBytes());
        return authHeader;
    }
}
