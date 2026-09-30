/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.configuration;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.core.BaseNms;
import net.flex.dci.otn.controller.nms.properties.equip.EquipmentTypeConfiguration;
import net.flex.dci.otn.controller.nms.properties.nms.NMSOperationsHandlersMap;
import net.flex.dci.otn.controller.nms.properties.scan.TelecomScanPortConfiguration;
import net.flex.dci.otn.controller.nms.rpc.NMSOperationsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @date: 2021/3/24
 */
@Configuration
@Slf4j
public class NMSOperationConfiguration {


    @Bean
    public NMSOperationsHandlersMap nmsOperationsHandlers() throws IOException {
        Resource resource = new ClassPathResource("NMSOperations.json");
        InputStream inputStream = resource.getInputStream();
        NMSOperationsHandlersMap nmsOperationsHandlersMap = JSON
                .parseObject(inputStream, NMSOperationsHandlersMap.class);
        return nmsOperationsHandlersMap;
    }

    @Bean(initMethod = "loadNmsMap")
    public NMSOperationsHandler nmsOperationsHandler(
            NMSOperationsHandlersMap nmsOperationsHandlersMap,
            List<BaseNms> nmsList
    ) {
        return new NMSOperationsHandler(nmsOperationsHandlersMap, nmsList);
    }

    @Bean
    public TelecomScanPortConfiguration telecomScanPortConfiguration() throws IOException {
        Resource resource = new ClassPathResource("config/TelecomModelScanPortConfiguration.json");
        if (!resource.exists()) {
            resource = new ClassPathResource("TelecomModelScanPortConfiguration.json");
        }
        InputStream inputStream = resource.getInputStream();
        TelecomScanPortConfiguration telecomScanPortConfiguration = JSON.parseObject(inputStream,
                TelecomScanPortConfiguration.class);
        return telecomScanPortConfiguration;
    }

    @Bean
    public EquipmentTypeConfiguration equipmentTypeConfiguration() throws IOException {
        Resource resource = new ClassPathResource("config/EquipTypesConfiguration.json");
        if (!resource.exists()) {
            resource = new ClassPathResource("EquipTypesConfiguration.json");
        }
        InputStream inputStream = resource.getInputStream();
        EquipmentTypeConfiguration equipmentTypeConfiguration = JSON.parseObject(inputStream,
                EquipmentTypeConfiguration.class);
        return equipmentTypeConfiguration;
    }


}
