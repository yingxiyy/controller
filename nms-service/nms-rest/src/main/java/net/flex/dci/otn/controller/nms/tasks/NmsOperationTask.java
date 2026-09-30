/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.tasks;


import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.nms.operations.impl.NMSOperations;

/**
 * @date: 2021/4/14
 */
@Slf4j
public class NmsOperationTask {

    private NMSOperations nmsOperations;


    private HttpServletRequest request;

    private HttpServletResponse response;


    public NmsOperationTask(NMSOperations nmsOperations,
            HttpServletRequest request,
            HttpServletResponse response) {
        this.nmsOperations = nmsOperations;
        this.request = request;
        this.response = response;
    }

    public String execute()
            throws CommonException, UnsupportedOperationException, IOException {
        log.info("start to execute the rpc operations");
//        String url = request.getURI().getPath();
//        String operations = url.replace(RPC_URL_PREFIX, BLANK);
//        Object returnValue = null;
//        if (operations.startsWith(NMS_PREFIX)) {
        String returnValue = nmsOperations.executeRequest(request, response);
//        }
        return returnValue;
    }
}
