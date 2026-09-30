/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otn.controller.tools.lifecycle.handler.DefaultLifecycleLogHandler;
import net.flex.dci.otn.controller.tools.lifecycle.handler.ILifecycleLog;
import net.flex.dci.otn.controller.tools.lifecycle.properties.dto.LifecycleOperationsDetailMap;
import net.flex.dci.otn.controller.tools.lifecycle.properties.dto.ModuleOperations;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/10/8 14:54
 */
@Slf4j
public class LogHandlerFactory {

    private static String HANDLER_PACKAGE_PREFIX = "net.flex.dci.otn.controller.tools.lifecycle.handler.";

    public static ILifecycleLog getHandler(String module) {
        log.debug("start to handler the lifecycle log handler for the module:{}", module);
        String handlerName = getModuleHandlerName(module);
        if (handlerName == null) {
            return SpringBeanFinder.getBean(DefaultLifecycleLogHandler.class);
        } else {
            handlerName = HANDLER_PACKAGE_PREFIX + handlerName;
            return (ILifecycleLog) SpringBeanFinder.getBean(handlerName);
        }
    }

    private static String getModuleHandlerName(String module) {
        LifecycleOperationsDetailMap logHandlerMap = SpringBeanFinder.getBean(
                LifecycleOperationsDetailMap.class);
        List<ModuleOperations> handlerMap = logHandlerMap.getMap();
        AtomicReference<String> name = new AtomicReference<>();
        handlerMap.forEach(moduleOperation -> {
            if (moduleOperation.getModuleName().equals(module)) {
                name.set(moduleOperation.getHandler());
            }
        });
        return name.get();
    }
}
