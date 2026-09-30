/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.kafka.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.kafka.config.KafkaConfig;
import net.flex.dci.otn.controller.tools.kafka.properties.KafkaProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Lazy
@RequiredArgsConstructor
public class ConsumerService {

    private final KafkaConfig config;

    private final KafkaProperties kafkaProperties;


    public void addListener(String topic, Object bean, Method method) {
        ContainerProperties newProperties = new ContainerProperties(topic);
        newProperties.setMessageListener((MessageListener<String, String>) record -> {
            try {
                method.invoke(bean, record);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        newProperties.setAckMode(AckMode.RECORD);
        ConcurrentMessageListenerContainer<String, String> newContainer = new ConcurrentMessageListenerContainer<>(
                config.consumerFactory(), newProperties);

        newContainer.setBeanName(bean.getClass().getSimpleName());
        newContainer.setConcurrency(5);

        log.info("start process message.....");
        newContainer.start();
    }

    public void addListener(String topic, MessageListener listener) {
        ContainerProperties newProperties = new ContainerProperties(topic);
        newProperties.setMessageListener(listener);
        newProperties.setAckMode(AckMode.RECORD);

        ConcurrentMessageListenerContainer<String, String> newContainer = new ConcurrentMessageListenerContainer<>(
                config.consumerFactory(), newProperties);

        newContainer.setBeanName(listener.getClass().getSimpleName());
        newContainer.setConcurrency(kafkaProperties.getClientConcurrency());
        log.info("start process message.....");
        newContainer.start();
    }

    public void addListener(String topic, Class deserializerClass, MessageListener listener) {
        ContainerProperties newProperties = new ContainerProperties(topic);
        newProperties.setMessageListener(listener);
        newProperties.setAckMode(AckMode.RECORD);

        ConcurrentMessageListenerContainer<String, String> newContainer = new ConcurrentMessageListenerContainer<>(
                config.consumerFactory(deserializerClass), newProperties);

        newContainer.setBeanName(listener.getClass().getSimpleName());
        newContainer.setConcurrency(kafkaProperties.getClientConcurrency());
        log.info("start process message.....");
        newContainer.start();
    }


    public void addListener(String topic, AcknowledgingMessageListener listener) {
        ContainerProperties newProperties = new ContainerProperties(topic);
        newProperties.setMessageListener(listener);
        newProperties.setAckMode(AckMode.MANUAL_IMMEDIATE);

        ConcurrentMessageListenerContainer<String, String> newContainer = new ConcurrentMessageListenerContainer<>(
                config.consumerFactory(), newProperties);

        newContainer.setBeanName(listener.getClass().getSimpleName());
        newContainer.setConcurrency(kafkaProperties.getClientConcurrency());
        log.info("start process message.....");
        newContainer.start();
    }

    public void addListener(String topic, Class<?> deserializerClass,
            AcknowledgingMessageListener listener) {
        ContainerProperties newProperties = new ContainerProperties(topic);
        newProperties.setMessageListener(listener);
        newProperties.setAckMode(AckMode.MANUAL);

        ConcurrentMessageListenerContainer<String, String> newContainer = new ConcurrentMessageListenerContainer<>(
                config.consumerManualCommitFactory(deserializerClass), newProperties);

        newContainer.setBeanName(listener.getClass().getSimpleName());
        newContainer.setConcurrency(kafkaProperties.getClientConcurrency());
        log.info("start process message.....");
        newContainer.start();
    }

    public void addBatchListener(String topic, Class<?> deserializerClass,
            BatchAcknowledgingMessageListener listener) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setBatchListener(true);
        factory.setConsumerFactory(config.consumerBatchFactory(deserializerClass));
        factory.getContainerProperties().setMessageListener(listener);
        factory.getContainerProperties().setSubBatchPerPartition(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setBatchErrorHandler(new BatchLoggingErrorHandler());
        factory.getContainerProperties().setSyncCommitTimeout(Duration.ofSeconds(5));
        factory.getContainerProperties().setAckOnError(false);
        factory.getContainerProperties().setShutdownTimeout(10000L);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        ConcurrentMessageListenerContainer<String, Object> newContainer =
                factory.createContainer(topic);

        newContainer.setBeanName(listener.getClass().getSimpleName());
        newContainer.setupMessageListener(listener);
        newContainer.setConcurrency(kafkaProperties.getClientConcurrency());
        log.info("batch consumer factory start process message.....");
        newContainer.start();
    }

}