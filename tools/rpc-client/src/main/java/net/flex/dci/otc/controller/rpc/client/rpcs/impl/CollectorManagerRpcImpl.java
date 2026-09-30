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
import net.flex.dci.otc.controller.rpc.client.rpcs.CollectorManagerRpc;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.CreateTelemetryServerInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.CreateTelemetryServerInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.CreateTelemetryServerOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.DeleteTelemetryServerInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.DeleteTelemetryServerInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.DeleteTelemetryServerOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/8/30 14:17
 */
@Component
@Deprecated
@Slf4j
public class CollectorManagerRpcImpl extends BasicRpc implements CollectorManagerRpc {


    public CollectorManagerRpcImpl() {
        this.NAMESPACE = "telemetry-manager";
        MODULE_NAME = "telemetryServerMgr";
    }

    @Override
    public void createTelemetryCollector(TelemetryServer telemetryServer) throws CommonException {
        log.info("start to create the telemetry server collector {}", telemetryServer);
        try {
            CreateTelemetryServerInput input = convert2CreateTelemetryServer(telemetryServer);
            String requestOp = "create-telemetry-server";
            String requestBody = formRpcInput(requestOp, input);
            String responseBody = executeRequest(requestOp, requestBody);
            CreateTelemetryServerOutput output = (CreateTelemetryServerOutput) formRpcOutPut(
                    requestOp, responseBody);
            if (!output.getReturnCode().equals(RpcResultType.Success)) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        output.getReturnMessage());
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }


    @Override
    public void deleteTelemetryCollector(TelemetryServer telemetryServer) {
        log.info("start to delete the telemetry server collector {}", telemetryServer);
        try {
            DeleteTelemetryServerInput input = new DeleteTelemetryServerInputBuilder().setName(
                    telemetryServer.getName()).build();
            String requestOp = "delete-telemetry-server";
            String requestBody = formRpcInput(requestOp, input);
            String responseBody = executeRequest(requestOp, requestBody);
            DeleteTelemetryServerOutput output = (DeleteTelemetryServerOutput) formRpcOutPut(
                    requestOp, responseBody);
            if (!output.getReturnCode().equals(RpcResultType.Success)) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        output.getReturnMessage());
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }


    /**
     * convert to telemetry input
     *
     * @param telemetryServer
     * @return
     */
    private CreateTelemetryServerInput convert2CreateTelemetryServer(
            TelemetryServer telemetryServer) {
        CreateTelemetryServerInputBuilder inputBuilder = new CreateTelemetryServerInputBuilder();
        inputBuilder.setApiVersion(telemetryServer.getApiVersion());
        inputBuilder.setName(telemetryServer.getName());
        inputBuilder.setIp(telemetryServer.getIp());
        inputBuilder.setPort(telemetryServer.getPort());
        inputBuilder.setLoginName(telemetryServer.getLoginName());
        inputBuilder.setLoginPasswd(telemetryServer.getLoginPasswd());
        inputBuilder.setTelemetryVersion(telemetryServer.getTelemetryVersion());
        inputBuilder.setSupportedNeVersion(telemetryServer.getSupportedNeVersion());
        return inputBuilder.build();
    }

}
