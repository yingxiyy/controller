package net.flex.dci.otn.controller.configuration;

import net.flex.dci.otn.controller.apsswitchlog.listener.ApsSwitchLogMessageConsumer;
import net.flex.dci.otn.controller.apsswitchlog.listener.deserializer.ApsSwitchLogDeserializer;
import net.flex.dci.otn.controller.tools.kafka.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 2025/9/13
 *
 * @author musa
 * @version 1.0
 **/
@Configuration
public class ApsSwitchLogConfiguration {

    @Value("${ApsSwitchLogTopic:aps-switch-log}")
    private String ApsSwitchLogTopic;

    private final ApsSwitchLogMessageConsumer apsSwitchLogMessageConsumer;

    @Autowired
    public ApsSwitchLogConfiguration(ApsSwitchLogMessageConsumer apsSwitchLogMessageConsumer) {
        this.apsSwitchLogMessageConsumer = apsSwitchLogMessageConsumer;
    }

    @Bean
    public ConsumerService registerKafkaMessageConsumerService(ConsumerService consumerService) {
        consumerService.addListener(ApsSwitchLogTopic, ApsSwitchLogDeserializer.class,
                apsSwitchLogMessageConsumer);
        return consumerService;
    }
}
