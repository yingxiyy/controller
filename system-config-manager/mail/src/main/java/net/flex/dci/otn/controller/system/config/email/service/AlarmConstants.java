/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.service;

public class AlarmConstants {

    public static String getSeverity(String severity) {
        String upperSeverity = severity.toUpperCase();
        switch (upperSeverity) {
            case "CRITICAL":
                return "紧急";
            case "MAJOR":
                return "主要";
            case "MINOR":
                return "次要";
            case "WARNING":
                return "警告";
            case "FAILED":
                return "异常";
            case "unknown":
            default:
                return "未知";
        }
    }

    public static String getIsClear(Boolean isClear) {
        return isClear ? "清除" : "产生";
    }
}
