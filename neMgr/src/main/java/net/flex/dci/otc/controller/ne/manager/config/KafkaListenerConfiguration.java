package net.flex.dci.otc.controller.ne.manager.config;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.core.kafka.NeChangeConsumer;
import net.flex.dci.otc.controller.ne.manager.core.kafka.deserializer.ElementChangeDeserializer;
import net.flex.dci.otn.controller.tools.kafka.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/3/25 13:37
 */
@Configuration
@Slf4j
public class KafkaListenerConfiguration {

//    @Value("${ne.communicate.topic:ne-conn-status}")
//    private String neStateTopic;

    @Value("${ne.communicate.topic:ne-conn-status}")
    private String neChangeTopic;


    @Autowired
    private NeChangeConsumer neChangeConsumer;

    @Bean
    ConsumerService registerConsumer(ConsumerService consumerService) {
        log.debug("start to register consumer ");
//        consumerService.addListener(neStateTopic, NeStateDeserializer.class,
//                communicateStateConsumer);
        consumerService.addListener(neChangeTopic, ElementChangeDeserializer.class,
                neChangeConsumer);
        return consumerService;
    }
}
