package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.softwareDb.SoftwareDbOperationEventMessage;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 *
 * 2025/10/5
 *
 * @author musa
 * @version 1.0
 **/

@Slf4j
public class NeOpsSender {

    @Setter
    public static PublishService publishService;

    public static void publishNeOps(SoftwareDbOperationEventMessage eventMessage) {
        log.debug("publish current ne ops event:{}", eventMessage);
        String key = eventMessage.getNeId();
        publishService.send(KafkaTopics.NE_OPERATION_TOPIC, key, eventMessage);
    }

    public static void publishNeOps(SoftwareDbOperationEventMessage eventMessage, String key) {
        log.debug("publish current ne ops event with key: {}, event:{}", key, eventMessage);
        publishService.send(KafkaTopics.NE_OPERATION_TOPIC, key, eventMessage);
    }

    public static void initializeTopics() {
        publishService.newTopic(KafkaTopics.NE_OPERATION_TOPIC);
    }

}
