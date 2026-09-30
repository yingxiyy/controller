/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.rpc;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.nms.exceptions.JSONParseException;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.SerializeUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @date: 2021/3/30
 */
@Slf4j
public abstract class BaseRpc implements IRpc {

    protected String RPC_NAMESPACE;

    protected NetconfTopology netconfTopology;

    public BaseRpc(NetconfTopology netconfTopology) {

        this.netconfTopology = netconfTopology;
    }

    protected String getServiceFromCmd(String cmd) {
        return cmd.replace(RPC_NAMESPACE + ":", "");
    }


    protected String serializeDataObject(String cmd, DataObject dataObject) throws CommonException {
        long t1 = System.currentTimeMillis();
        String result = SerializeUtils
                .serializeDataObject(RPC_NAMESPACE, getServiceFromCmd(cmd), dataObject);
        long t2 = System.currentTimeMillis();
        log.info("[ROUTE-TIMING] serializeDataObject: {}ms, length={}", t2 - t1, result.length());
        return result;
    }

    protected <T> T parseInput(String cmd, String requestBody, Class<?> T)
            throws JSONParseException {
        try {
            return (T) SerializeUtils
                    .parseRpcInput(RPC_NAMESPACE, getServiceFromCmd(cmd), requestBody);
        } catch (Exception ex) {
            throw new JSONParseException("the request body input for the nms is invalid");
        }
    }

    private String requestBody;

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

}
