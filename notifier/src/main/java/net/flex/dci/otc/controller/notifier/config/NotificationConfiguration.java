package net.flex.dci.otc.controller.notifier.config;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.notifier.core.listener.AlarmKafkaListener;
import net.flex.dci.otc.controller.notifier.core.listener.BroadcastMessageKafkaListener;
import net.flex.dci.otc.controller.notifier.core.listener.ElementChangeNotificationListener;
import net.flex.dci.otc.controller.notifier.core.listener.ObjectUpdateNotificationListener;
import net.flex.dci.otn.controller.tools.kafka.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2021/11/5 16:38
 */
@Configuration
@Slf4j
public class NotificationConfiguration {


    @Value("${kafka.topic.object:objectUpdateTopic}")
    private String objectTopic;

    @Value("${kafka.topic.alarm:dci-alarm}")
    private String alarmTopic;

    @Value("${kafka.topic.broadcast:broadcastMessageTopic}")
    private String broadcastMessageTopic;

    @Value("${kafka.topic.element:element-change}")
    private String elementChangeTopic;

    @Autowired
    private ObjectUpdateNotificationListener objectUpdateNotificationListener;
    @Autowired
    private AlarmKafkaListener alarmKafkaListener;
    @Autowired
    private BroadcastMessageKafkaListener broadcastMessageKafkaListener;
    @Autowired
    private ElementChangeNotificationListener elementChangeNotificationListener;

    @Bean
    public ConsumerService registerKafkaMessageListener(ConsumerService consumerService)
            throws NoSuchMethodException {
        log.info("start to register message listener for the kafka");
        consumerService.addListener(objectTopic, objectUpdateNotificationListener);
        consumerService.addListener(alarmTopic, alarmKafkaListener);
        consumerService.addListener(broadcastMessageTopic, broadcastMessageKafkaListener);
        consumerService.addListener(elementChangeTopic, elementChangeNotificationListener);
        return consumerService;
    }
}
