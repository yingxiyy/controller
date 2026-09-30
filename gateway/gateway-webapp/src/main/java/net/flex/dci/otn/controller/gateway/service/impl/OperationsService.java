///*
// *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
// *
// *  This program and the accompanying materials are made available under the
// *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
// *  and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//
//package net.flex.dci.otn.controller.gateway.service.impl;
//
//import java.io.IOException;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otn.controller.gateway.service.IOperationsService;
//import net.flex.dci.otn.controller.gateway.tasks.RpcOperationTask;
//import net.flex.dci.otn.controller.gateway.tree.operations.impl.NMSOperations;
//import net.flex.dci.otn.controller.gateway.tree.operations.impl.RpcTransferOperations;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Service;
//
///**
// * @date: 2021/3/30
// */
//@Service
//@Slf4j
//public class OperationsService implements IOperationsService {
//
//    private NMSOperations nmsOperations;
//
//    private RpcTransferOperations rpcTransferOperations;
//
//
//    public OperationsService(RpcTransferOperations rpcTransferOperations,
//            NMSOperations nmsOperations) {
//        this.nmsOperations = nmsOperations;
//        this.rpcTransferOperations = rpcTransferOperations;
//    }
//
////    public Object executeRequest(HttpServletRequest request, HttpServletResponse response,
////            Object handler) {
////        try {
////            RpcOperationTask rpcOperationTask = new RpcOperationTask(nmsOperations,
////                    rpcTransferOperations, request, response, handler);
//////            FutureTask<String> futureTask = this.submitTask(rpcOperationTask);
////            return rpcOperationTask.run();
////        } catch (IOException ex) {
//////            if (ex instanceof ExecutionException) {
//////                ExecutionException exception = (ExecutionException) ex;
//////                Throwable cause = exception.getCause();
//////                if (cause instanceof UnsupportedRPCException) {
//////                    throw new UnsupportedRPCException(cause.getMessage());
//////                }
//////                if (cause instanceof CommonException) {
//////                    throw new CommonException(((CommonException) cause).getType(),
//////                            ((CommonException) cause).getDetail());
//////                }
//////                if (cause instanceof JSONParseException) {
//////                    throw new JSONParseException(cause.getMessage());
//////                }
//////                if (cause instanceof Exception) {
//////                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//////                            cause.getMessage());
//////                }
//////            }
////            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
////        }
////    }
//
//    @Override
//    public String executeRequest(ServerHttpRequest request, ServerHttpResponse response)
//            throws IOException {
//        RpcOperationTask rpcOperationTask = new RpcOperationTask(nmsOperations,
//                rpcTransferOperations, request, response);
//        String result = (String) rpcOperationTask.run();
//        return result;
//    }
//}
