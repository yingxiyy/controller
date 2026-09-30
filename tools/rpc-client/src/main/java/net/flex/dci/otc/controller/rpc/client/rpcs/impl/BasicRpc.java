/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.COLON;
import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.HTTP_PREFIX;

import com.alibaba.fastjson.JSON;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.controller.rpc.client.dto.ApiErrorResponse;
import net.flex.dci.otc.controller.rpc.client.dto.ModuleCredential;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClient;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClientConfig;
import net.flex.dci.otc.controller.rpc.client.utils.ModuleUtils;
import net.flex.dci.otc.controller.rpc.client.utils.RpcConstants;
import net.flex.dci.otc.serialization.JsonUtil;
import org.asynchttpclient.Response;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/24 16:32
 */
public abstract class BasicRpc {

    protected static final String DEFAULT_ACCOUNT = "admin";
    protected static final String DEFAULT_PWD = "admin";

    protected String NAMESPACE;

    protected String MODULE_NAME;

    protected ModuleCredential credential;

    @Autowired
    protected JsonUtil jsonUtil;

    @Autowired
    protected OdlRpcClient odlRpcClient;


    protected DataObject formRpcOutPut(String opService, String result) {
        if (opService.contains(COLON)) {
            String[] req = opService.split(COLON);
            return jsonUtil.fromJsonToDataObject(req[0], req[1], result, false);
        }
        return jsonUtil.fromJsonToDataObject(NAMESPACE, opService, result, false);
    }


    protected <T> T formRpcOutPut(String opService, String result, Class<T> clazz) {
        DataObject dataObject;
        if (opService.contains(COLON)) {
            String[] req = opService.split(COLON);
            dataObject = jsonUtil.fromJsonToDataObject(req[0], req[1], result, false);
        } else {
            dataObject = jsonUtil.fromJsonToDataObject(NAMESPACE, opService, result, false);
        }

        // 类型安全检查
        if (clazz.isInstance(dataObject)) {
            return clazz.cast(dataObject);
        } else {
            throw new ClassCastException("Expected " + clazz.getName() + " but got " +
                    (dataObject != null ? dataObject.getClass().getName() : "null"));
        }
    }


    protected String formRpcInput(String opService, DataObject dataObject) {
        if (opService.contains(COLON)) {
            String[] req = opService.split(COLON);
            return jsonUtil.fromDataObjectToJson(req[0], req[1], dataObject, true);
        }
        return jsonUtil.fromDataObjectToJson(NAMESPACE, opService, dataObject, true);
    }

    protected String getManagerUrl(String requestOp) throws Exception {
        credential = ModuleUtils.getCredential(MODULE_NAME);
        if (credential != null) {
            String hostIp = CommonUtil.formateIpAddress(credential.getIp());
            if (!requestOp.contains(COLON)) {
                return HTTP_PREFIX + hostIp + COLON
                        + credential.getPort()
                        + RpcConstants.RPC_PREFIX
                        + NAMESPACE
                        + COLON
                        + requestOp;
            } else {
                return HTTP_PREFIX + hostIp + COLON
                        + credential.getPort()
                        + RpcConstants.RPC_PREFIX
                        + requestOp;
            }
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the module " + MODULE_NAME + " is not active please restart again!");
        }
    }


    protected String getManagerUrl(Adapter adapter, String requestOp) {
        String hostIp = CommonUtil.formateIpAddress(adapter.getIp());
        return HTTP_PREFIX + hostIp + COLON
                + adapter.getPort().getValue()   //adapter.getPort()
                + RpcConstants.RPC_PREFIX
                + NAMESPACE
                + COLON
                + requestOp;
    }


    protected String executeRequest(String requestOp, String requestBody)
            throws Exception {
        credential = ModuleUtils.getCredential(MODULE_NAME);
        if (credential == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the request module " + MODULE_NAME + " is already down");
        }
        String account =
                credential.getUsername() != null ? credential.getUsername() : DEFAULT_ACCOUNT;
        String password = credential.getPassword() != null ? credential.getPassword() : DEFAULT_PWD;
        String result = odlRpcClient.setConfig(
                        new OdlRpcClientConfig.Builder().user(account).password(password).build())
                .post(getManagerUrl(requestOp), requestBody);
        return result;
    }


    protected Response executeReq(String requestOp, String requestBody)
            throws Exception {
        credential = ModuleUtils.getCredential(MODULE_NAME);
        if (credential == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the request module " + MODULE_NAME + " is already down");
        }
        String account =
                credential.getUsername() != null ? credential.getUsername() : DEFAULT_ACCOUNT;
        String password = credential.getPassword() != null ? credential.getPassword() : DEFAULT_PWD;
        Response result = odlRpcClient.setConfig(
                        new OdlRpcClientConfig.Builder().user(account).password(password).build())
                .postReq(getManagerUrl(requestOp), requestBody);
        return result;
    }


    protected <T> T form2DataObject(InstanceIdentifier iid, String result, Class<T> neClass) {
        return (T) jsonUtil.fromJsonToDataObject(jsonUtil.fromInstanceIdentifierToString(iid),
                result);
    }

    protected String getErrorDetail(String rspBody) {
        ApiErrorResponse apiErrorResponse = JSON.parseObject(rspBody, ApiErrorResponse.class);
        return apiErrorResponse.getMessage();
    }


}
