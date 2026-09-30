/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.kafka.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.kafka.partitioner.DefaultPartitioner;
import net.flex.dci.otn.controller.tools.kafka.properties.KafkaProperties;
import net.flex.dci.otn.controller.tools.kafka.serializer.JSONObjectSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

@Slf4j
@Configuration
@Data
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    @Autowired
    private KafkaProperties kafkaProperties;


    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumerGroup() == null ? "default"
                        : kafkaProperties.getConsumerGroup());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                kafkaProperties.getMaxPollIntervalMs());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaProperties.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
                kafkaProperties.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProperties.getMaxPollRecords());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaProperties.isEnableAutoCommit());
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,
                kafkaProperties.getAutoCommitIntervalMs());

        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, kafkaProperties.getFetchMinBytes());
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, kafkaProperties.getFetchMaxWaitMs());
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
                kafkaProperties.getMaxPartitionFetchBytes());
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, kafkaProperties.getFetchMaxBytes());
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                kafkaProperties.getPartitionAssignmentStrategy());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG,
                kafkaProperties.isAllowAutoCreateTopics());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    public ConsumerFactory<String, Object> consumerFactory(Class valueDeserializerClass) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumerGroup() == null ? "default"
                        : kafkaProperties.getConsumerGroup());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                kafkaProperties.getMaxPollIntervalMs());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaProperties.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
                kafkaProperties.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProperties.getMaxPollRecords());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaProperties.isEnableAutoCommit());
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,
                kafkaProperties.getAutoCommitIntervalMs());

        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
                kafkaProperties.getMaxPartitionFetchBytes());
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, kafkaProperties.getFetchMaxBytes());
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                kafkaProperties.getPartitionAssignmentStrategy());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    public ConsumerFactory<String, Object> consumerManualCommitFactory(
            Class valueDeserializerClass) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumerGroup() == null ? "default"
                        : kafkaProperties.getConsumerGroup());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);

        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                kafkaProperties.getMaxPollIntervalMs());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaProperties.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
                kafkaProperties.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProperties.getMaxPollRecords());

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
                kafkaProperties.getMaxPartitionFetchBytes());
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, kafkaProperties.getFetchMaxBytes());
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                kafkaProperties.getPartitionAssignmentStrategy());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    public ProducerFactory<String, Object> publishFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JSONObjectSerializer.class);
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
                kafkaProperties.getProducerMaxRequestSize());
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
                kafkaProperties.getProducerCompressionType());
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG,
                kafkaProperties.getProducerBufferMemory());
        configProps.put(ProducerConfig.RETRIES_CONFIG, kafkaProperties.getProducerRetries());
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG,
                kafkaProperties.getProducerRetryBackoffMs());
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProperties.getProducerBatchSize());
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProperties.getProducerLingerMs());
        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, DefaultPartitioner.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    public ProducerFactory<String, byte[]> publishByteFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
                kafkaProperties.getProducerMaxRequestSize());
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
                kafkaProperties.getProducerCompressionType());
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG,
                kafkaProperties.getProducerBufferMemory());
        configProps.put(ProducerConfig.RETRIES_CONFIG, kafkaProperties.getProducerRetries());
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG,
                kafkaProperties.getProducerRetryBackoffMs());
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProperties.getProducerBatchSize());
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProperties.getProducerLingerMs());
        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, DefaultPartitioner.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    public ProducerFactory<String, Object> publishStringFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
                kafkaProperties.getProducerMaxRequestSize());
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
                kafkaProperties.getProducerCompressionType());
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG,
                kafkaProperties.getProducerBufferMemory());
        configProps.put(ProducerConfig.RETRIES_CONFIG, kafkaProperties.getProducerRetries());
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG,
                kafkaProperties.getProducerRetryBackoffMs());
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProperties.getProducerBatchSize());
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProperties.getProducerLingerMs());
        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, DefaultPartitioner.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    public Map<String, Object> getAdminProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return configProps;
    }

    public short getReplicationFactor() {
        return kafkaProperties.getReplicationFactor();
    }

    public int getPartitionNumber() {
        return kafkaProperties.getPartitionNumber();
    }

    public Map<String, String> defaultTopicConfig() {
        Map<String, String> topicConfig = new HashMap<>();
        topicConfig.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
                String.valueOf(kafkaProperties.getProducerMaxRequestSize()));
        return topicConfig;
    }

    public ConsumerFactory<String, Object> consumerBatchFactory(
            Class<?> deserializerClass) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumerGroup() == null ? "default-batch-group"
                        : kafkaProperties.getConsumerGroup());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializerClass);

        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                kafkaProperties.getMaxPollIntervalMs());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaProperties.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
                kafkaProperties.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProperties.getMaxPollRecords());

        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, kafkaProperties.getFetchMinBytes());

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
                kafkaProperties.getMaxPartitionFetchBytes());
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, kafkaProperties.getFetchMaxBytes());
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, kafkaProperties.getFetchMaxWaitMs());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG,
                kafkaProperties.isAllowAutoCreateTopics());
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                kafkaProperties.getPartitionAssignmentStrategy());
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
