/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.core.kafka.deserilizer.AlarmDeserializer;
import net.flex.dci.otc.controller.status.alarm.core.kafka.deserilizer.AppAlarmDeserializer;
import net.flex.dci.otc.controller.status.alarm.core.kafka.listener.AlarmMessageListener;
import net.flex.dci.otc.controller.status.alarm.core.kafka.listener.AppAlarmListener;
import net.flex.dci.otc.controller.status.alarm.core.kafka.listener.ViewTopoAlarmRecalcConsumer;
import net.flex.dci.otc.controller.status.core.handler.StateChangeChainHandler;
import net.flex.dci.otc.controller.status.ne.core.deserializer.NeStatusMessageDeserializer;
import net.flex.dci.otc.controller.status.ne.core.deserializer.StatusChangeEventDeserializer;
import net.flex.dci.otc.controller.status.ne.core.deserializer.ViewTopoAlarmRecalcMsgDeserializer;
import net.flex.dci.otc.controller.status.ne.core.listener.NeStatusListener;
import net.flex.dci.otc.controller.status.ne.core.listener.StatusEventsListener;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;
import net.flex.dci.otn.controller.tools.kafka.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Data
public class DciStatusConfiguration {

    @Value("${status.alarm.alarmKeyword:null}")
    private String alarmKeyword;

    @Value("${isShowLinkAlarm:false}")
    private boolean isShowLinkAlarm;


    @Value("${alarm.topic:odl-alarm}")
    private String alarmTopic;

    @Value("${ne.status.topic:ne-status}")
    private String statusTopic;

    @Value("${app.alarm.topic:dci-app-alarm}")
    private String appAlarmTopic;

    @Value("${ne.status.events.topic:status-events}")
    private String statusEventTopic;

    @Autowired
    private AlarmMessageListener alarmMessageListener;

    @Autowired
    private NeStatusListener neStatusListener;

    @Autowired
    private AppAlarmListener appAlarmListener;

    @Autowired
    private StatusEventsListener statusEventsListener;

    @Autowired
    private ViewTopoAlarmRecalcConsumer viewTopoAlarmRecalcConsumer;

    @Bean
    @ConditionalOnBean(StateChangeChainHandler.class)
    public ConsumerService registerKafkaMessageListener(ConsumerService consumerService) {
        log.info("start to register message listener for the kafka");
        consumerService.addBatchListener(alarmTopic, AlarmDeserializer.class,
                alarmMessageListener);
        consumerService.addBatchListener(statusTopic, NeStatusMessageDeserializer.class,
                neStatusListener);
        consumerService.addListener(appAlarmTopic, AppAlarmDeserializer.class, appAlarmListener);
        consumerService.addBatchListener(statusEventTopic, StatusChangeEventDeserializer.class,
                statusEventsListener);
        consumerService.addListener(KafkaTopics.VIEW_TOPO_ALARM_RECALC_TOPIC,
                ViewTopoAlarmRecalcMsgDeserializer.class, viewTopoAlarmRecalcConsumer);
        return consumerService;
    }
}
