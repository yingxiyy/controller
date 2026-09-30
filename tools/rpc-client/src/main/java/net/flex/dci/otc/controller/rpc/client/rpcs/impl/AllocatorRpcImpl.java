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
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClientConfig;
import net.flex.dci.otc.controller.rpc.client.rpcs.AllocatorRpc;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelOutput;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/9 11:28
 */
@Slf4j
@Component
public class AllocatorRpcImpl extends BasicRpc implements AllocatorRpc {

    private static final String CREATE_SITELINK = "site-topology:create-link";

    private static final String REMOVE_SITE_LINK = "site-topology:remove-link";

    private static final String REMOVE_TUNNEL = "tunnel:remove-tunnel";

    private static final String CREATE_TUNNEL = "tunnel:create-tunnel";


    public AllocatorRpcImpl() {
        MODULE_NAME = "allocator";
    }

    @Override
    public CreateLinkOutput createSiteLink(CreateLinkInput input) throws CommonException {
        log.info("send create site link rpc cmd ,link detail is {}", input);
        try {
            String requestBody = formRpcInput(CREATE_SITELINK, input);
            String responseBody = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(credential.getUsername())
                                    .password(credential.getPassword()).build())
                    .post(getManagerUrl(CREATE_SITELINK), requestBody);
            CreateLinkOutput createLinkOutput = (CreateLinkOutput) formRpcOutPut(CREATE_SITELINK,
                    responseBody);
            log.info("finish send create site link cmd,result is  {}", createLinkOutput);
            return createLinkOutput;
        } catch (Exception e) {
            log.error("failed to execute the create site link rpc cmd", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to execute the create site link rpc cmd", e);
        }
    }

    @Override
    public RemoveLinkOutput removeSiteLink(RemoveLinkInput input) throws CommonException {
        log.info("send remove site link {} rpc cmd", input.getLinkId());
        try {
            String requestBody = formRpcInput(REMOVE_SITE_LINK, input);
            String responseBody = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(credential.getUsername())
                                    .password(credential.getPassword()).build())
                    .post(getManagerUrl(REMOVE_SITE_LINK), requestBody);
            RemoveLinkOutput output = (RemoveLinkOutput) formRpcOutPut(REMOVE_SITE_LINK,
                    responseBody);
            return output;
        } catch (Exception ex) {
            log.error("failed to execute the remove site link rpc cmd", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to execute the remove site link rpc cmd", ex);
        }

    }

    @Override
    public CreateTunnelOutput createTunnel(CreateTunnelInput input) throws CommonException {
        log.info("send create tunnel rpc cmd,detail is {}", input);
        try {
            String requestBody = formRpcInput(CREATE_TUNNEL, input);
            String responseBody = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(credential.getUsername())
                                    .password(credential.getPassword()).build())
                    .post(getManagerUrl(CREATE_TUNNEL), requestBody);
            CreateTunnelOutput output = (CreateTunnelOutput) formRpcOutPut(CREATE_TUNNEL,
                    responseBody);
            return output;
        } catch (Exception ex) {
            log.error("failed to execute the create tunnel rpc cmd", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to execute the create tunnel rpc cmd", ex);
        }
    }

    @Override
    public RemoveTunnelOutput removeTunnel(RemoveTunnelInput input) throws CommonException {
        log.info("send remove tunnel rpc cmd,detail is {}", input);
        try {
            String requestBody = formRpcInput(REMOVE_TUNNEL, input);
            String responseBody = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(credential.getUsername())
                                    .password(credential.getPassword()).build())
                    .post(getManagerUrl(REMOVE_TUNNEL), requestBody);
            RemoveTunnelOutput output = (RemoveTunnelOutput) formRpcOutPut(REMOVE_TUNNEL,
                    responseBody);
            return output;
        } catch (Exception e) {
            log.error("failed to execute the remove tunnel rpc cmd", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to execute the remove tunnel rpc cmd", e);
        }
    }


}
