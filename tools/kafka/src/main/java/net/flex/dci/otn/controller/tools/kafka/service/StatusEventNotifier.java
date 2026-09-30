package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 * 2025/8/26
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class StatusEventNotifier {

    private static final String topic = KafkaTopics.STATUS_EVENT_TOPIC;
    @Setter
    private static PublishService publishService;

    public static void sendMessage(StatusChangeEvent message) {
        log.debug("send ne conn status  message: {}", message);
        publishService.send(topic, message.getObjectId(), message);
    }

    public static void sendMessage(Object message, String key) {
        log.debug("send ne conn status message with key: {}, message: {}", key, message);
        publishService.send(topic, key, message);
    }

    public static void initializeTopics() {
        publishService.newTopic(topic);
    }
}
