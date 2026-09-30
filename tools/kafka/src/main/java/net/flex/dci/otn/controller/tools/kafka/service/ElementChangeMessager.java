package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.element.ElementChangeNotification;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 * @version 1.0
 * @date 2022/5/5 15:04
 */
@Slf4j
public class ElementChangeMessager {

    @Setter
    private static PublishService publishService;

    public static void publishChangeMessage(ElementChangeNotification message) {
        log.debug("send element change notification message: {}", message);

        publishService.send(KafkaTopics.ELEMENT_CHANGE_TOPIC, message.getElementType().name(),
                message);
    }

    public static void publishChangeMessage(Object message, String key) {
        log.debug("send element change notification message with key: {}, message: {}", key,
                message);
        publishService.send(KafkaTopics.ELEMENT_CHANGE_TOPIC, key, message);
    }

    public static void initializeTopics() {
        publishService.newTopic(KafkaTopics.ELEMENT_CHANGE_TOPIC);
    }
}
