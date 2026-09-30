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
import net.flex.dci.otc.controller.rpc.client.constants.RpcCommand.TelemetryManagerRpcCmd;
import net.flex.dci.otc.controller.rpc.client.dto.TelemetryServer;
import net.flex.dci.otc.controller.rpc.client.rpcs.TelemetryServerManagerRpc;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.CreateTelemetryServerInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.CreateTelemetryServerInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.CreateTelemetryServerOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.DeleteTelemetryServerInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.DeleteTelemetryServerInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.DeleteTelemetryServerOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/12/20 11:11
 */
@Slf4j
@Component
public class TelemetryServerManagerRpcImpl extends BasicRpc implements TelemetryServerManagerRpc {


    public TelemetryServerManagerRpcImpl() {
        MODULE_NAME = "telemetryServerMgr";
    }

    @Override
    public CreateTelemetryServerOutput addTelemetryServer(TelemetryServer telemetryServer) {
        log.debug("add telemetry server :{}", telemetryServer);

        CreateTelemetryServerInput input = convert2CreateTelemetryServerInput(telemetryServer);
        try {
            String requestBody = formRpcInput(TelemetryManagerRpcCmd.CREATE_TELEMETRY_SERVER,
                    input);
            String responseBody = executeRequest(TelemetryManagerRpcCmd.CREATE_TELEMETRY_SERVER,
                    requestBody);
            CreateTelemetryServerOutput result = (CreateTelemetryServerOutput) formRpcOutPut(
                    TelemetryManagerRpcCmd.CREATE_TELEMETRY_SERVER,
                    responseBody);
            log.info("finish send create telemetry server cmd,result is  {}", result);
            return result;

        } catch (Exception ex) {
            log.error("failed to execute the create telemetry server rpc reason is :{}", ex);
            throw new CommonException(CommonExceptionType.MODULE_ERROR,
                    "failed to execute add telemetry server rpc");
        }
    }


    @Override
    public DeleteTelemetryServerOutput deleteTelemetryServer(TelemetryServer telemetryServer) {
        log.debug("delete telemetry server:{} ", telemetryServer);
        DeleteTelemetryServerInput input = convert2DeleteTelemetryServerInput(telemetryServer);
        try {
            String requestBody = formRpcInput(TelemetryManagerRpcCmd.DELETE_TELEMETRY_SERVER,
                    input);
            String responseBody = executeRequest(TelemetryManagerRpcCmd.DELETE_TELEMETRY_SERVER,
                    requestBody);
            DeleteTelemetryServerOutput output = (DeleteTelemetryServerOutput) formRpcOutPut(
                    TelemetryManagerRpcCmd.DELETE_TELEMETRY_SERVER, responseBody);
            return output;
        } catch (Exception ex) {
            log.error("failed to execute the delete telemetry server rpc reason is {}", ex);
            throw new CommonException(CommonExceptionType.MODULE_ERROR,
                    "failed to execute delete telemetry server rpc");
        }

    }

    private DeleteTelemetryServerInput convert2DeleteTelemetryServerInput(
            TelemetryServer telemetryServer) {
        return new DeleteTelemetryServerInputBuilder()
                .setName(new Uri(telemetryServer.getName()))
                .setIp(telemetryServer.getIpv4Address())
                .build();
    }


    /**
     * convert to telemetry server manager
     *
     * @param telemetryServer
     * @return
     */
    private CreateTelemetryServerInput convert2CreateTelemetryServerInput(
            TelemetryServer telemetryServer) {
        return new CreateTelemetryServerInputBuilder()
                .setSupportedNeVersion(telemetryServer.getSupportedNeVersion())
                .setPort(new PortNumber(telemetryServer.getPort()))
                .setTelemetryVersion(telemetryServer.getTelemetryVersion())
                .setApiVersion(telemetryServer.getApiVersion())
                .setName(new Uri(telemetryServer.getName()))
                .setIp(telemetryServer.getIpv4Address())
                .build();

    }

}
