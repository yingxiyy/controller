package net.flex.dci.otn.controller.tools.kafka.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.kafka.config.KafkaConfig;
import net.flex.dci.otn.controller.tools.kafka.initializer.KafkaTopicInitializer;
import net.flex.dci.otn.controller.tools.kafka.properties.KafkaProperties;
import net.flex.dci.otn.controller.tools.kafka.service.AlarmMessageSender;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.ConsumerService;
import net.flex.dci.otn.controller.tools.kafka.service.DCIAppAlarmMessager;
import net.flex.dci.otn.controller.tools.kafka.service.ElementChangeMessager;
import net.flex.dci.otn.controller.tools.kafka.service.NeConnStatusNotifier;
import net.flex.dci.otn.controller.tools.kafka.service.NeOpsSender;
import net.flex.dci.otn.controller.tools.kafka.service.ObjectNotifiMessager;
import net.flex.dci.otn.controller.tools.kafka.service.PublishService;
import net.flex.dci.otn.controller.tools.kafka.service.StatusEventNotifier;
import net.flex.dci.otn.controller.tools.kafka.service.StatusMessageSender;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import net.flex.dci.otn.controller.tools.kafka.service.ViewTopoAlarmRecalcSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version 1.0
 * @date 2022/3/17 10:40
 */
@Configuration
@Slf4j
@Import(KafkaConfig.class)
@ConditionalOnClass(KafkaConfig.class)
public class DciKafkaAutoconfigure {

    @Bean
    @ConditionalOnClass(value = {KafkaProperties.class, KafkaConfig.class})
    public ConsumerService consumerService(KafkaProperties kafkaProperties,
            KafkaConfig kafkaConfig) {
        return new ConsumerService(kafkaConfig, kafkaProperties);
    }

    @Bean
    @ConditionalOnClass(value = {KafkaProperties.class, KafkaConfig.class})
    public PublishService publishService(KafkaConfig kafkaConfig) {
        PublishService publishService = new PublishService(kafkaConfig);
        BroadcastMessager.setPublishService(publishService);
        ObjectNotifiMessager.setPublishService(publishService);
        AlarmMessageSender.setPublishService(publishService);
        StatusMessageSender.setPublishService(publishService);
        TaskInfoMessager.setPublishService(publishService);
        ElementChangeMessager.setPublishService(publishService);
        DCIAppAlarmMessager.setPublishService(publishService);
        NeConnStatusNotifier.setPublishService(publishService);
        StatusEventNotifier.setPublishService(publishService);
        NeOpsSender.setPublishService(publishService);
        ViewTopoAlarmRecalcSender.setPublishService(publishService);
        return publishService;
    }


    @Bean
    @ConditionalOnBean(PublishService.class)
    public KafkaTopicInitializer kafkaTopicInitializer(PublishService publishService) {
        return new KafkaTopicInitializer(publishService);
    }

}
