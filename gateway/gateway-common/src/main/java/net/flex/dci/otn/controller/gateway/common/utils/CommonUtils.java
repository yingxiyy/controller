/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.utils;

import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

/**
 * common filter
 *
 * @author: xinyzhao
 * @date: 2021/3/25
 */
@Slf4j
public class CommonUtils {

    /**
     * generate base authorization token
     *
     * @param username
     * @param password
     * @return
     */
    public static String basicAuthorization(String username, String password) {
        String auth = username + ":" + password;
        String authHeader = "Basic " + Base64.getUrlEncoder().encodeToString(auth.getBytes());
        return authHeader;
    }
    
}
