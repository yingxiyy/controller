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
import net.flex.dci.otc.controller.rpc.client.constants.RpcCommand.ImplementRpcCmd;
import net.flex.dci.otc.controller.rpc.client.rpcs.ImplementorRpc;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateSitelinkSyncInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateSitelinkSyncOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelSyncInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelSyncOutput;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/9 11:27
 */
@Component
@Slf4j
public class ImplementorRpcImpl extends BasicRpc implements ImplementorRpc {


    public ImplementorRpcImpl() {
        MODULE_NAME = "implementor";
    }

    @Override
    public UpdateSitelinkSyncOutput updateSiteLink(UpdateSitelinkSyncInput input)
            throws CommonException {
        log.info("send update site link command ,detail :{}", input);
        try {
            String requestBody = formRpcInput(ImplementRpcCmd.UPDATE_SITE_LINK_SYNC, input);
            String result = executeRequest(ImplementRpcCmd.UPDATE_SITE_LINK_SYNC, requestBody);
            UpdateSitelinkSyncOutput output = (UpdateSitelinkSyncOutput) formRpcOutPut(
                    ImplementRpcCmd.UPDATE_SITE_LINK_SYNC, result);
            return output;
        } catch (Exception ex) {
            log.error("failed to execute update site link rpc cmd", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to execute update site link rpc", ex);
        }
    }


    @Override
    public UpdateLinkOutput updateLink(UpdateLinkInput input) throws CommonException {
        log.info("send update site link command ,detail :{}", input);
        try {
            String requestBody = formRpcInput(ImplementRpcCmd.UPDATE_LINK, input);
            String result = executeRequest(ImplementRpcCmd.UPDATE_LINK, requestBody);
            UpdateLinkOutput output = (UpdateLinkOutput) formRpcOutPut(
                    ImplementRpcCmd.UPDATE_LINK, result);
            return output;
        } catch (Exception ex) {
            log.error("failed to execute update  link rpc cmd", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to execute update link rpc", ex);
        }
    }

    @Override
    public UpdateTunnelOutput updateTunnel(UpdateTunnelInput input) throws CommonException {
        log.info("send update tunnel command ,detail :{}", input);
        try {
            String requestBody = formRpcInput(ImplementRpcCmd.UPDATE_TUNNEL, input);
            String result = executeRequest(ImplementRpcCmd.UPDATE_TUNNEL, requestBody);
            UpdateTunnelOutput output = (UpdateTunnelOutput) formRpcOutPut(
                    ImplementRpcCmd.UPDATE_TUNNEL, result);
            return output;
        } catch (Exception ex) {
            log.error("failed to execute update tunnel rpc cmd", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to execute update tunnel rpc", ex);
        }
    }

    @Override
    public UpdateTunnelSyncOutput updateTunnelSync(UpdateTunnelSyncInput input)
            throws CommonException {
        log.info("send update tunnel sync command ,detail :{}", input);
        try {
            String requestBody = formRpcInput(ImplementRpcCmd.UPDATE_TUNNEL_SYNC, input);
            String result = executeRequest(ImplementRpcCmd.UPDATE_TUNNEL_SYNC, requestBody);
            UpdateTunnelSyncOutput output = (UpdateTunnelSyncOutput) formRpcOutPut(
                    ImplementRpcCmd.UPDATE_TUNNEL_SYNC, result);
            return output;
        } catch (Exception ex) {
            log.error("failed to execute update tunnel sync  rpc cmd", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to execute update tunnel sync  rpc", ex);
        }
    }


}
