/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.properties;


import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.io.InputStream;
import net.flex.dci.otn.controller.tools.lifecycle.properties.dto.LifecycleOperationsDetailMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/29 13:13
 */
@Configuration
public class LifecycleModuleProperties {

    @Bean
    public LifecycleOperationsDetailMap lifecycleOperationsDetailMap() throws IOException {
        Resource resource = new ClassPathResource("lifecycleModule.json");
        InputStream inputStream = resource.getInputStream();
        LifecycleOperationsDetailMap nmsOperationsHandlersMap = JSON
                .parseObject(inputStream, LifecycleOperationsDetailMap.class);
        return nmsOperationsHandlersMap;
    }
   

}
