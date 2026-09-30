package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

@Slf4j
public class BroadcastMessager {


    private static final String topic = KafkaTopics.BROADCAST_TOPIC;
    @Setter
    private static PublishService publishService;

//    static {
//        publishService = SpringBeanFinder.getBean(PublishService.class);
//        publishService.newTopic(topic);
//    }

    public static void publishKafkaMessage(BroadcastMessage message) {
        log.debug("send broadcast message: {}", message);
        publishService.send(topic, message.getTitle(), message);
    }

    public static void publishKafkaMessage(Object message, String key) {
        log.debug("send broadcast message with key: {}, message: {}", key, message);
        publishService.send(topic, key, message);
    }

    public static void publishMessage(String specTopic, byte[] message) {
        log.debug("send broadcast byte message size: {} on TOPIC {}", message.length, specTopic);
        publishService.send(specTopic, message);
    }

    public static void initializeTopics() {
        publishService.newTopic(topic);
    }
}
