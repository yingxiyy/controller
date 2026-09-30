///*
// *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
// *
// *  This program and the accompanying materials are made available under the
// *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
// *  and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//
//package net.flex.dci.otn.controller.gateway.tasks;
//
//import static net.flex.dci.otn.controller.gateway.common.filter.Constants.NMS_PREFIX;
//import static net.flex.dci.otn.controller.gateway.common.filter.Constants.RPC_URL_PREFIX;
//
//import java.io.IOException;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otn.controller.gateway.tree.operations.impl.NMSOperations;
//import net.flex.dci.otn.controller.gateway.tree.operations.impl.RpcTransferOperations;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//
///**
// * @author: xinyzhao
// * @date: 2021/4/14
// */
//@Slf4j
//public class RpcOperationTask {
//
//    private NMSOperations nmsOperations;
//
//    private RpcTransferOperations rpcTransferOperations;
//
//
//    private ServerHttpRequest request;
//
//    private ServerHttpResponse response;
//
//
//    public RpcOperationTask(NMSOperations nmsOperations,
//            RpcTransferOperations rpcTransferOperations,
//            ServerHttpRequest request,
//            ServerHttpResponse response) {
//        this.nmsOperations = nmsOperations;
//        this.rpcTransferOperations = rpcTransferOperations;
//        this.request = request;
//        this.response = response;
//    }
//
//    public Object run()
//            throws CommonException, UnsupportedOperationException, IOException {
//        log.info("start to execute the rpc operations");
//        String url = request.getURI().getPath();
//        String operations = url.replace(RPC_URL_PREFIX, "");
//        Object returnValue = null;
//        if (operations.startsWith(NMS_PREFIX)) {
//            returnValue = nmsOperations.executeRequest(request, response);
//        } else {
//            returnValue = rpcTransferOperations.executeRemoteRequest(request, response);
//        }
//        return returnValue;
//    }
//}
