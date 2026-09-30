/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.handler.ConnectionsHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOpsConnectionsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOpsConnectionsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ops.connections.output.OpsConnections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyOPSConnections extends BaseNms {


    private final String GET_OPS_CONNECTIONS = "nms:get-ops-connections";

    @Autowired
    private ConnectionsHandler connectionsHandler;

    public TopologyOPSConnections(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody) throws CommonException {
        String returnValue = null;
        if (cmd.equals(GET_OPS_CONNECTIONS)) {
            returnValue = getOpsConnections(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException("unknown opc connection operation ");
        }
        return returnValue;
    }

    /**
     * get ops connections
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getOpsConnections(String cmd, String requestBody) throws CommonException {
        log.info("get ops connections input");
        try {
            GetOpsConnectionsInput input = parseInput(cmd, requestBody,
                    GetOpsConnectionsInput.class);
            GetOpsConnectionsOutputBuilder outputBuilder = new GetOpsConnectionsOutputBuilder();
            List<OpsConnections> connections = this.connectionsHandler.getOpsConnections(input);
            outputBuilder.setOpsConnections(connections);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }
}
