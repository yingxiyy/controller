/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.dto;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Data;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otn.controller.tools.lifecycle.properties.dto.LifecycleOperationsDetailMap;
import net.flex.dci.otn.controller.tools.lifecycle.properties.dto.ModuleOperations;
import net.flex.dci.otn.controller.tools.lifecycle.properties.dto.OperationDetailInfo;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/30 12:29
 */
@Data
public class OperationInfo implements Serializable {

    private String module;

    private String operation;

    private String operationName;

    public static OperationInfo extractOperationInfo(String arg) {
        OperationInfo operationInfo = new OperationInfo();
        String[] args = arg.split(":");
        operationInfo.setModule(args[0]);
        operationInfo.setOperation(args[1]);
        String operationName = extractOperationName(args[0], args[1]);
        operationInfo.setOperationName(operationName);
        return operationInfo;
    }

    private static String extractOperationName(String module, String operation) {
        LifecycleOperationsDetailMap logHandlerMap = SpringBeanFinder.getBean(
                LifecycleOperationsDetailMap.class);
        List<ModuleOperations> handlerMap = logHandlerMap.getMap();
        AtomicReference<String> name = new AtomicReference<>();
        handlerMap.forEach(moduleOperation -> {
            if (moduleOperation.getModuleName().equals(module)) {
                for (OperationDetailInfo operationInfo :
                        moduleOperation.getOperationInfos()) {
                    if (operation.equals(operationInfo.getOperation())) {
                        name.set(operationInfo.getName());
                    }
                }
            }
        });
        return name.get();
    }
}
