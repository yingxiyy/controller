/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.configuration;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.gateway.common.properties.route.RouteMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @date: 2021/3/24
 */
@Configuration
@Slf4j
public class JSONFileConfiguration {

//    @Bean
//    public RpcModulePathMap rpcModuleMap() throws IOException {
//        Resource resource = new ClassPathResource("rpcModulePath.json");
//        InputStream inputStream = resource.getInputStream();
//        RpcModulePathMap rpcModulePathMap = JSON.parseObject(inputStream, RpcModulePathMap.class);
//        return rpcModulePathMap;
//    }

//    @Bean
//    public NMSOperationsHandlersMap nmsOperationsHandlers() throws IOException {
//        Resource resource = new ClassPathResource("NMSOperations.json");
//        InputStream inputStream = resource.getInputStream();
//        NMSOperationsHandlersMap nmsOperationsHandlersMap = JSON
//                .parseObject(inputStream, NMSOperationsHandlersMap.class);
//        return nmsOperationsHandlersMap;
//    }
//
//    @Bean
//    public NMSOperationsHandler nmsOperationsHandler(
//            NMSOperationsHandlersMap nmsOperationsHandlersMap,
//            NetconfTopology netconfTopology
//    ) {
//        return new NMSOperationsHandler(nmsOperationsHandlersMap, netconfTopology);
//    }

    @Bean
    public RouteMap routeMap() throws IOException {
        Resource resource = new ClassPathResource("routeMap.json");
        InputStream inputStream = resource.getInputStream();
        RouteMap routeMap = JSON.parseObject(inputStream, RouteMap.class);
        return routeMap;
    }


}
