package net.flex.dci.otn.controller.taskinfo.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otn.controller.taskinfo.core.listener.TaskInfoKafkaListener;
import net.flex.dci.otn.controller.tools.kafka.service.ConsumerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2021/11/5 16:38
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class KafkaConfiguration {

    @Value("${kafka.topic.broadcast:taskInfoTopic}")
    private String taskInfoMessageTopic = Constant.TASKINFO_TOPIC;


    private final TaskInfoKafkaListener taskInfoKafkaListener;

    @Bean
    public ConsumerService registerKafkaMessageListener(ConsumerService consumerService) {
        log.info("start to register message listener for the kafka");
        consumerService.addListener(taskInfoMessageTopic, taskInfoKafkaListener);
        return consumerService;
    }
}
