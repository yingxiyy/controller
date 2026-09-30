package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.ObjectChangeMessage;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 * objectUpdateTopic
 *
 * @version 1.0
 * @date 2022/3/18 10:46
 */
@Slf4j
public class ObjectNotifiMessager {

    @Setter
    private static PublishService publishService;


    public static void publishKafkaMessage(ObjectChangeMessage message) {
        log.debug("send notification message: {}", message);
        publishService.send(KafkaTopics.OBJECT_NOTIFICATION_TOPIC, message.getObjectType(),
                message);
    }

    public static void publishKafkaMessage(Object message, String key) {
        log.debug("send notification message with key: {}, message: {}", key, message);
        publishService.send(KafkaTopics.OBJECT_NOTIFICATION_TOPIC, key, message);
    }

    public static void initializeTopics() {
        publishService.newTopic(KafkaTopics.OBJECT_NOTIFICATION_TOPIC);
    }
}
