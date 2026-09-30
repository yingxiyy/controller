package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 * 2025/6/20
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class NeConnStatusNotifier {

    @Setter
    private static PublishService publishService;

    public static void sendMessage(Object message) {
        log.debug("send ne conn status  message: {}", message);
        publishService.send(KafkaTopics.NE_CONN_STATUS_TOPIC, message);
    }

    public static void sendMessage(Object message, String key) {
        log.debug("send ne conn status message with key: {}, message: {}", key, message);
        publishService.send(KafkaTopics.NE_CONN_STATUS_TOPIC, key, message);
    }

    public static void initializeTopics() {
        publishService.newTopic(KafkaTopics.NE_CONN_STATUS_TOPIC);
    }

}
