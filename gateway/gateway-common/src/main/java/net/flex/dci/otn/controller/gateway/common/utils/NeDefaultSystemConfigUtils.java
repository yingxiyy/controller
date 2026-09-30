/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: xinyzhao
 * @date: 2021/4/8
 */
@Slf4j
public class NeDefaultSystemConfigUtils {

    private static NeDefaultSystemConfigUtils instance;


    private NeDefaultSystemConfigUtils() {

    }

    public static NeDefaultSystemConfigUtils getInstance() {
        if (instance == null) {
            synchronized (NeDefaultSystemConfigUtils.class) {
                instance = new NeDefaultSystemConfigUtils();
            }
        }
        return instance;
    }

}
