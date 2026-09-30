package net.flex.dci.otn.controller.tools.kafka.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 * @version 1.0
 * @date 2022/6/7 11:15
 */
@Slf4j
public class DCIAppAlarmMessager {

    private static final String topic = KafkaTopics.DCI_APP_ALARM_TOPIC;
    @Setter
    private static PublishService publishService;

//    static {
//        publishService = SpringBeanFinder.getBean(PublishService.class);
//        publishService.newTopic(topic);
//    }

    public static void generateAlarm(Object alarm) {
        log.debug("send alarm : {}", alarm);
        publishService.send(topic, alarm);
    }

    public static void generateAlarm(Object alarm, String key) {
        log.debug("send alarm with key: {}, alarm: {}", key, alarm);
        publishService.send(topic, key, alarm);
    }

    public static void clearAlarm(Object alarm) {
        log.debug("send alarm : {}", alarm);
        publishService.send(topic, alarm);
    }

    public static void clearAlarm(Object alarm, String key) {
        log.debug("send alarm with key: {}, alarm: {}", key, alarm);
        publishService.send(topic, key, alarm);
    }

    public static void initializeTopics() {
        publishService.newTopic(topic);
    }
}
