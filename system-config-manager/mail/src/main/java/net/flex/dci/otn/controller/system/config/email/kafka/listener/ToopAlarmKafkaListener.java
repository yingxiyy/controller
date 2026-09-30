/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.kafka.listener;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import net.flex.dci.otn.controller.system.config.email.service.MailImplService;
import net.flex.dci.otn.controller.system.config.subscribe.filter.SubscribeAlarmTopicFilterImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/12/9 13:15
 */
@Component
@Slf4j
public class ToopAlarmKafkaListener implements MessageListener<String, List<ToopAlarm>> {

    @Autowired
    private MailImplService mailImplService;

    @Autowired
    private SubscribeAlarmTopicFilterImpl subscribeAlarmTopicFilter;

    @Log
    @Override
    public void onMessage(ConsumerRecord<String, List<ToopAlarm>> stringListConsumerRecord) {
        List<ToopAlarm> alarms = stringListConsumerRecord.value();
        if (alarms == null || alarms.isEmpty()) {
            log.warn("Null message received.");
            return;
        }

        log.info("Received alarms:  {} in total.", alarms.size());
        List<ToopAlarm> filterAlarms = subscribeAlarmTopicFilter.filterBySubscribeAlarmTopic(
                alarms);

        log.info("Got filtered alarms: {} in total.", filterAlarms.size());
        mailImplService.sendAlarms(filterAlarms);
//        for (ToopAlarm alarm : filterAlarms) {
//            try {
//                log.info("Received alarm data: {}", alarm);
//                mailImplService.send(alarm);
//            } catch (Exception e) {
//                log.error("Failed to handle alarm data.{}", alarm, e);
//                continue;
//            }
//        }
        log.debug("Completed to handle alarm data.");
    }
}
