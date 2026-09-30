/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.subscribe.utils;

import java.util.regex.Pattern;

/**
 * @version 1.0
 * @date 2021/12/13 10:33
 */
public class PatternUtils {

    public static Pattern buildAlarmMessageRegex(String message) {

        String regexMessage = message.replace("xxxx", "(.*)").replace("?", "(.*)");
        Pattern pattern = Pattern.compile(regexMessage);
        return pattern;

    }
}
