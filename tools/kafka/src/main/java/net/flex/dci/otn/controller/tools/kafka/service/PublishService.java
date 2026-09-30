/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.kafka.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.kafka.config.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishService {

    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    private static final int ADMIN_OPERATION_TIMEOUT_SECONDS = 30;

    private final KafkaConfig config;

    private KafkaTemplate<String, Object> template;
    private KafkaTemplate<String, byte[]> byteTemplate;

    private KafkaTemplate<String, Object> stringTemplate;

    private AdminClient adminClient;

//    public ProducerFactory<String, String> publishFactory() {
//        Map<String, Object> configProps = new HashMap<>();
//        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrap());
//        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, DefaultPartitioner.class);
//        return new DefaultKafkaProducerFactory<>(configProps);
//    }

    @PostConstruct
    public void init() {
        adminClient = AdminClient.create(config.getAdminProps());
        template = new KafkaTemplate<>(config.publishFactory());
        byteTemplate = new KafkaTemplate<>(config.publishByteFactory());
        stringTemplate = new KafkaTemplate<>(config.publishStringFactory());
        log.info("Kafka PublishService initialized with AdminClient");
    }

    public void send(String topic, byte[] message) {
        log.trace("send kafka topic {}, byte message size: {}", topic, message.length);
        byteTemplate.send(topic, message);
    }

    public void send(String topic, String key, byte[] message) {
        log.debug("send kafka topic is {}, the key is {}, byte message size: {}", topic, key,
                message.length);
        byteTemplate.send(topic, key, message);
    }

    public void send(String topic, Object message) {
        log.trace("send kafka topic {}, message: {}", topic, message);
        ListenableFuture<SendResult<String, Object>> future = template.send(topic, message);
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onFailure(Throwable throwable) {
                log.error("send message failed | topic={} ",
                        topic, throwable);
            }

            @Override
            public void onSuccess(SendResult<String, Object> result) {
                log.debug(
                        "send message success | topic={} | partition={} | offset={} ",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void send(String topic, String key, Object message) {
        log.debug("send kafka topic is {},the key is {},message:{}", topic, key, message);
        template.send(topic, key, message);
    }

    public void send(String topic, Integer partition, Object message) {
        log.debug("send kafka topic is {},partition is {},message:{}", topic, partition, message);
        ListenableFuture<SendResult<String, Object>> future = template.send(topic, partition, null, message);
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onFailure(Throwable throwable) {
                log.error("send message failed | topic={} | partition={}",
                        topic, partition, throwable);
            }

            @Override
            public void onSuccess(SendResult<String, Object> result) {
                log.debug(
                        "send message success | topic={} | partition={} | offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void send(String topic, Integer partition, String key, Object message) {
        log.debug("send kafka topic is {},partition is {},the key is {},message:{}", 
                topic, partition, key, message);
        ListenableFuture<SendResult<String, Object>> future = template.send(topic, partition, key, message);
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onFailure(Throwable throwable) {
                log.error("send message failed | topic={} | partition={} | key={}",
                        topic, partition, key, throwable);
            }

            @Override
            public void onSuccess(SendResult<String, Object> result) {
                log.debug(
                        "send message success | topic={} | partition={} | key={} | offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        key,
                        result.getRecordMetadata().offset());
            }
        });
    }

    public ListenableFuture<SendResult<String, Object>> sendWithFuture(String topic,
            Object message) {
        log.trace("send kafka topic {} with future, message: {}", topic, message);
        ListenableFuture<SendResult<String, Object>> future = template.send(topic, message);
        return future;
    }

    public void sendStringMessage(String topic, String message) {
        log.trace("send kafka topic {},message:{} ", topic, message);
        stringTemplate.send(topic, message);
    }

    public void newTopic(String topic) {
        int attempt = 0;
        long retryDelayMs = INITIAL_RETRY_DELAY_MS;
        boolean successful = false;
        while (!successful && attempt < MAX_RETRIES) {
            attempt++;
            try {
                createTopicIfNotExists(topic);
                successful = true;
                log.info("Topic {} created/verified successfully", topic);
            } catch (TimeoutException | RetriableException e) {
                log.warn("Retryable error creating topic '{}' (attempt {}/{}): {}",
                        topic, attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // 指数退避策略
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Topic creation retry interrupted for topic: {}", topic);
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Non-retryable error creating topic '{}': {}", topic, e.getMessage());
                break;
            }
        }
        if (!successful) {
            log.error("Failed to create topic '{}' after {} attempts", topic, MAX_RETRIES);
        }
    }

    public void createTopicIfNotExists(String topic)
            throws ExecutionException, InterruptedException, TimeoutException {
        createTopicIfNotExists(topic, null);
    }

    public void createTopicIfNotExists(String topic, Integer partitionCount)
            throws ExecutionException, InterruptedException, TimeoutException {
        try {
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> existingTopics = topics.names()
                    .get(ADMIN_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!existingTopics.contains(topic)) {
                int partitions = partitionCount != null ? partitionCount : config.getPartitionNumber();
                NewTopic newTopic = new NewTopic(topic, partitions,
                        config.getReplicationFactor());
                newTopic.configs(config.defaultTopicConfig());
                CreateTopicsResult createTopicResult = adminClient.createTopics(
                        Collections.singleton(newTopic));
                createTopicResult.all().get(ADMIN_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.info("Topic created successfully: {}, partitions: {}", topic, partitions);
            } else {
                log.info("Topic already exists: {}", topic);
            }
        } catch (InterruptedException | ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                log.debug("Topic '{}' already exists (caught by exception)", topic);
            } else {
                throw e;
            }
        }
    }


    @PreDestroy
    public void shutdown() {
        if (adminClient != null) {
            try {
                adminClient.close(Duration.ofSeconds(10));
            } catch (Exception e) {
                log.warn("Error closing AdminClient", e);
            }
        }
    }
}
