package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 * @version 1.0
 * @date 2022/4/1 17:03
 */
@Slf4j
public class AlarmMessageSender {

    private static final String topic = KafkaTopics.DCI_ALARM_TOPIC;
    //        publishService.newTopic(topic);
    @Setter
    private static PublishService publishService;

//    static {
//        publishService = SpringBeanFinder.getBean(PublishService.class);
//        publishService.newTopic(topic);
//    }


    public static void sendMessage(Object message) {
        log.debug("send alarm  message: {}", message);
        publishService.send(topic, message);

    }

    public static void sendMessage(Object message, String key) {
        log.debug("send alarm message with key: {}, message: {}", key, message);
        publishService.send(topic, key, message);
    }

    public static void initializeTopics() {
        publishService.newTopic(topic);
    }
}
