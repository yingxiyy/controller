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
import net.flex.dci.otc.controller.rpc.client.constants.RpcCommand.AdapterManagerRpcCmd;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterManagerRpc;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.CreateAdapterInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.CreateAdapterInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.CreateAdapterOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.DeleteAdapterInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.DeleteAdapterInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.DeleteAdapterOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/30 14:33
 */
@Component
@Slf4j
public class AdapterManagerRpcImpl extends BasicRpc implements AdapterManagerRpc {


    public AdapterManagerRpcImpl() {
        this.NAMESPACE = "eml-manager";
        MODULE_NAME = "adapterMgr";
    }

//    @PostConstruct
//    private void init() {
//        credential = ModuleUtils.getCredential(MODULE_NAME);
//
//    }

    @Override
    public void createAdapter(Adapter adapter) throws CommonException {
        log.info("start to create a new Adapter ");
        try {
            CreateAdapterInput input = convert2AddAdapter(adapter);
            String requestOp = AdapterManagerRpcCmd.CREATE_ADAPTER;
            String requestBody = formRpcInput(requestOp, input);
            String result = executeRequest(requestOp, requestBody);
            CreateAdapterOutput output = (CreateAdapterOutput) formRpcOutPut(requestOp, result);
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
    public void deleteAdapter(Adapter adapter) throws CommonException {
        log.info("start to delete the adapter {}", adapter.getName().getValue());
        try {
            String requestOp = AdapterManagerRpcCmd.DELETE_ADAPTER;
            DeleteAdapterInput input = new DeleteAdapterInputBuilder().setName(
                    adapter.getName().getValue()).setForce(true).build();
            String requestBody = formRpcInput(requestOp, input);
            String result = executeRequest(requestOp, requestBody);
            DeleteAdapterOutput output = (DeleteAdapterOutput) formRpcOutPut(requestOp, result);
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
    public void createAdapter(CreateAdapterInput input) throws CommonException {
        log.info("start to create a new Adapter ");
        try {
            String requestOp = AdapterManagerRpcCmd.CREATE_ADAPTER;
            String requestBody = formRpcInput(requestOp, input);
            String result = executeRequest(requestOp, requestBody);
            CreateAdapterOutput output = (CreateAdapterOutput) formRpcOutPut(requestOp, result);
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
    public void deleteAdapter(DeleteAdapterInput input) throws CommonException {
        log.info("start to delete the adapter {}", input.getName());
        try {
            String requestOp = AdapterManagerRpcCmd.DELETE_ADAPTER;
            String requestBody = formRpcInput(requestOp, input);
            String result = executeRequest(requestOp, requestBody);
            DeleteAdapterOutput output = (DeleteAdapterOutput) formRpcOutPut(requestOp, result);
            if (!output.getReturnCode().equals(RpcResultType.Success)) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        output.getReturnMessage());
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    private CreateAdapterInput convert2AddAdapter(Adapter adapter) {
        CreateAdapterInputBuilder builder = new CreateAdapterInputBuilder();
        builder.setName(adapter.getName());// need Uri
        builder.setIp(adapter.getIp());
        builder.setPort(adapter.getPort());
        builder.setLoginName(adapter.getLoginName());
        builder.setLoginPasswd(adapter.getLoginPasswd());
        builder.setApiVersion(adapter.getApiVersion());
        builder.setAdapterVersion(adapter.getAdapterVersion());
        builder.setSupportedNeApiVersion(adapter.getSupportedNeApiVersion());
//		builder.setCommunicationStatus(null);// need enum
//		builder.setNe(null);// List<Ne>
//		builder.setVersionMatch(null);// boolean
        return builder.build();
    }


}
