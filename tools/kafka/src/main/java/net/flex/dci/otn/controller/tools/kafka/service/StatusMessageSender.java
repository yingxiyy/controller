package net.flex.dci.otn.controller.tools.kafka.service;

import java.util.Collections;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.ne.NeStatus;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 * @version 1.0
 * @date 2022/4/2 14:51
 */
@Slf4j
public class StatusMessageSender {

    private static final String STATUS_TOPIC = KafkaTopics.NE_STATUS_TOPIC;
    @Setter
    private static PublishService publishService;


    public static void sendMessage(NeStatusMessage message) {
        if (message == null || message.getNeStatuses() == null || message.getNeStatuses()
                .isEmpty()) {
            return;
        }

        if (message.getNeStatuses().size() == 1) {
            NeStatus status = message.getNeStatuses().get(0);
            publishService.send(STATUS_TOPIC, status.getNeId(), message);
            return;
        }

        for (NeStatus status : message.getNeStatuses()) {
            NeStatusMessage singleMessage = NeStatusMessage.builder()
                    .neStatuses(Collections.singletonList(status))
                    .build();
            publishService.send(STATUS_TOPIC, status.getNeId(), singleMessage);
        }
    }

    public static void sendMessage(NeStatusMessage message, String key) {
        publishService.send(STATUS_TOPIC, key, message);
    }

    public static void initializeTopics() {
        publishService.newTopic(STATUS_TOPIC);
    }

}
