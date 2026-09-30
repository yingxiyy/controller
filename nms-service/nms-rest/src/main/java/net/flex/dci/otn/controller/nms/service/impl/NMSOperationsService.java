/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.service.impl;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.operations.impl.NMSOperations;
import net.flex.dci.otn.controller.nms.service.INmsOperationsService;
import net.flex.dci.otn.controller.nms.tasks.NmsOperationTask;
import org.springframework.stereotype.Service;

/**
 * @date: 2021/3/30
 */
@Service
@Slf4j
public class NMSOperationsService implements INmsOperationsService {

    private NMSOperations nmsOperations;


    public NMSOperationsService(
            NMSOperations nmsOperations) {
        this.nmsOperations = nmsOperations;
    }


    @Override
    public String executeRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
//        RpcOperationTask rpcOperationTask = new RpcOperationTask(nmsOperations,
//                rpcTransferOperations, request, response);
        NmsOperationTask nmsOperationTask = new NmsOperationTask(nmsOperations, request, response);
        String result = nmsOperationTask.execute();
        return result;
    }
}
