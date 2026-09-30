/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.rpc.client.rpcs.NmsRpc;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkPagedOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelPagedOutput;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/9/5 16:01
 */
@Component
@Slf4j
public class NmsRpcImpl extends BasicRpc implements NmsRpc {

    public NmsRpcImpl() {
        NAMESPACE = "nms";
        MODULE_NAME = "gateway";
    }


    @Override
    public GetSiteLinkPagedOutput getSiteLinkPaged(GetSiteLinkPagedInput siteLinkPagedInput)
            throws CommonException {
        log.info("start to get site link paged {} ", siteLinkPagedInput);
        try {
            String requestOp = "get-site-link-paged";
            String requestBody = formRpcInput(requestOp, siteLinkPagedInput);
            String responseBody = executeRequest(requestOp, requestBody);
            GetSiteLinkPagedOutput output = (GetSiteLinkPagedOutput) formRpcOutPut(requestOp,
                    responseBody);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site link paged " + ex.getMessage(), ex);
        }
    }

    @Override
    public GetTunnelPagedOutput getTunnelPaged(GetTunnelPagedInput tunnelPagedInput)
            throws CommonException {
        log.info("start to get tunnel paged {} ", tunnelPagedInput);
        try {
            String requestOp = "get-tunnel-paged";
            String requestBody = formRpcInput(requestOp, tunnelPagedInput);
            String responseBody = executeRequest(requestOp, requestBody);
            GetTunnelPagedOutput output = (GetTunnelPagedOutput) formRpcOutPut(requestOp,
                    responseBody);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get tunnel paged " + ex.getMessage(), ex);
        }
    }


}
