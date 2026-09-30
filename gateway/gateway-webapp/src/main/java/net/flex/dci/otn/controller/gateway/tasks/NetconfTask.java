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
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otn.controller.gateway.tree.component.ITopologyTree;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//
///**
// * @author: xinyzhao
// * @date: 2021/4/14
// */
//@Slf4j
//public class NetconfTask {
//
//    private HttpMethod method;
//
//    //    private HttpServletRequest request;
//    private ServerHttpRequest request;
//
//    private String identifier;
//
//    private ITopologyTree topologyTree;
//
//    public NetconfTask(HttpMethod method, ServerHttpRequest request, String identifier,
//            ITopologyTree topologyTree) {
//        this.method = method;
//        this.request = request;
//        this.identifier = identifier;
//        this.topologyTree = topologyTree;
//    }
//
//
//    public String run() throws CommonException, UnsupportedOperationException {
//        log.info("start to get rest conf network");
//        String returnValue = null;
//        switch (method) {
//            case GET:
//                returnValue = topologyTree.getNetWorkTopology(identifier);
//                break;
//            case PUT:
//            case POST:
//                returnValue = topologyTree.mergeNetworkTopology(request, identifier);
//                break;
//            default:
//                throw new UnsupportedOperationException("the method is not supported");
//        }
//
//        return returnValue;
//    }
//}
