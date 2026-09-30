/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoggerController {
    String PACKAGE_NAME = this.getClass().getPackage().getName();

    @RequestMapping(value = "/log/debug")
    public String setDebug()  {
        return setLogLevel(LogLevel.DEBUG);
    }

    @RequestMapping(value = "/log/info")
    public String setInfo() {
        return setLogLevel(LogLevel.INFO);
    }

    @RequestMapping(value = "/log/trace")
    public String setTrace() {
        return setLogLevel(LogLevel.TRACE);
    }

    public String setLogLevel(LogLevel logLevel) {

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        if (logLevel == LogLevel.DEBUG) {
            loggerContext.getLogger(PACKAGE_NAME).setLevel(Level.DEBUG);
            return "Log level is DEBUG.";
        }
        if (logLevel == LogLevel.TRACE) {
            loggerContext.getLogger(PACKAGE_NAME).setLevel(Level.TRACE);
            return "Log level is TRACE.";
        }
        loggerContext.getLogger(PACKAGE_NAME).setLevel(Level.INFO);
        return "Log level is INFO.";


    }

    enum LogLevel {
        DEBUG, INFO,TRACE
    }
}
