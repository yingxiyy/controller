/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.operations.impl;

import static net.flex.dci.otn.controller.nms.utils.Constants.BLANK;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.core.BaseNms;
import net.flex.dci.otn.controller.nms.operations.IOperations;
import net.flex.dci.otn.controller.nms.rpc.NMSOperationsHandler;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.HttpUtils;
import org.springframework.stereotype.Component;

/**
 * nms rpc operations for nms
 *
 * @author: musa2333
 * @date: 2021/3/29
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NMSOperations implements IOperations {

    private final NMSOperationsHandler nmsOperationsHandler;


    /**
     * final get nms operations
     *
     * @param url
     * @return
     */
    private String getNMSOperation(String url) {
        return url.replace(Constants.RPC_URL_PREFIX, BLANK);
    }

    @Override
    public String executeRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String url = request.getRequestURI();
        String nmsOperation = getNMSOperation(url);
        String requestBody = HttpUtils.getRequestBody(request);
        String returnValue = null;
        if (nmsOperationsHandler.containsKey(nmsOperation)) {
            BaseNms nmsRpc = (BaseNms) nmsOperationsHandler.getHandlerByCMD(nmsOperation);
            returnValue = nmsRpc.executeRequest(nmsOperation, requestBody);
        } else {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "unsupported nms operation method");
        }
        return returnValue;
    }
}
