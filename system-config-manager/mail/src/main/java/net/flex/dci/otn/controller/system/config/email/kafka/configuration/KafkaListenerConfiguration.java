/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.kafka.configuration;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.email.kafka.deserializer.ToopAlarmDeserializer;
import net.flex.dci.otn.controller.system.config.email.kafka.listener.ToopAlarmKafkaListener;
import net.flex.dci.otn.controller.tools.kafka.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2021/12/9 13:12
 */
@Configuration
@Slf4j
public class KafkaListenerConfiguration {

    @Value("${spring.alarm.topic:odl-alarm}")
    private String alarmTopic;

    @Autowired
    private ToopAlarmKafkaListener alarmKafkaListener;

    @Bean
    public ConsumerService registerKafkaMessageListener(ConsumerService consumerService)
            throws NoSuchMethodException {
        log.info("start to register message listener for the kafka");
        consumerService.addListener(alarmTopic, ToopAlarmDeserializer.class, alarmKafkaListener);
        return consumerService;
    }

}
