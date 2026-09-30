package net.flex.dci.otn.controller.tools.kafka.initializer;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.kafka.service.AlarmMessageSender;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.DCIAppAlarmMessager;
import net.flex.dci.otn.controller.tools.kafka.service.ElementChangeMessager;
import net.flex.dci.otn.controller.tools.kafka.service.NeConnStatusNotifier;
import net.flex.dci.otn.controller.tools.kafka.service.NeOpsSender;
import net.flex.dci.otn.controller.tools.kafka.service.ObjectNotifiMessager;
import net.flex.dci.otn.controller.tools.kafka.service.PublishService;
import net.flex.dci.otn.controller.tools.kafka.service.StatusMessageSender;
import net.flex.dci.otn.controller.tools.kafka.service.ViewTopoAlarmRecalcSender;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 2025/7/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class KafkaTopicInitializer {

    private final PublishService publishService;

    public KafkaTopicInitializer(PublishService publishService) {
        this.publishService = publishService;
    }

    /**
     * initializeTopics BroadcastMessager.setPublishService(publishService);
     * ObjectNotifiMessager.setPublishService(publishService);
     * AlarmMessageSender.setPublishService(publishService);
     * StatusMessageSender.setPublishService(publishService);
     * TaskInfoMessager.setPublishService(publishService);
     * ElementChangeMessager.setPublishService(publishService);
     * DCIAppAlarmMessager.setPublishService(publishService);
     * NeConnStatusNotifier.setPublishService(publishService);
     * ViewTopoAlarmRecalcSender.initializeTopics();
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeTopics() {
        try {
            log.info("initialize kafka topics");
            BroadcastMessager.initializeTopics();
            ObjectNotifiMessager.initializeTopics();
            AlarmMessageSender.initializeTopics();
            StatusMessageSender.initializeTopics();
            ElementChangeMessager.initializeTopics();
            DCIAppAlarmMessager.initializeTopics();
            NeConnStatusNotifier.initializeTopics();
            StatusMessageSender.initializeTopics();
            NeOpsSender.initializeTopics();
            ViewTopoAlarmRecalcSender.initializeTopics();
        } catch (Exception ex) {
            log.error("Failed to initialize Kafka topics", ex);
        }
    }


}
