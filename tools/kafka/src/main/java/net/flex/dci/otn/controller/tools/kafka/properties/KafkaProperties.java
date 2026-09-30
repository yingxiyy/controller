package net.flex.dci.otn.controller.tools.kafka.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/2/22 16:07
 */
@Configuration
@ConfigurationProperties(prefix = "kafka")
@ConditionalOnProperty("kafka.bootstrap-servers")
@Data
public class KafkaProperties {

    private String bootstrapServers;

    private String consumerGroup;

    private int partitionNumber = 1;

    private short replicationFactor = 1;

    //    @Value("${kafka.client-concurrency:1}")
    private int clientConcurrency = 1;


    /**
     * 每次 poll 拉取的最大消息数
     */
    private int maxPollRecords = 500;

    /**
     * poll 间隔最大值，默认 5 分钟，避免处理超时触发 rebalance
     */
    private int maxPollIntervalMs = 300000;

    /**
     * 会话超时时间，默认 10 秒，心跳丢失后触发 rebalance
     */
    private int sessionTimeoutMs = 10000;

    /**
     * 心跳间隔，默认 3 秒
     */
    private int heartbeatIntervalMs = 3000;

    /**
     * 是否自动提交 offset
     */
    private boolean enableAutoCommit = true;

    /**
     * 自动提交间隔
     */
    private int autoCommitIntervalMs = 5000;

    private int fetchMaxWaitMs = 500;

    /**
     * 自动偏移量重置策略：earliest/latest/none
     */
    private String autoOffsetReset = "earliest";

    /**
     * 是否允许自动创建主题
     */
    private boolean allowAutoCreateTopics = false;

    /**
     * 分区分配策略：RoundRobinAssignor/RangeAssignor
     */
    private String partitionAssignmentStrategy = "org.apache.kafka.clients.consumer.RoundRobinAssignor";

    /**
     * 每个分区最大拉取字节数
     */
    private int maxPartitionFetchBytes = 16777216;

    /**
     * 单次 fetch 最大字节数
     */
    private int fetchMaxBytes = 52428800;

    private int fetchMinBytes = 10240;

    private int producerMaxRequestSize = 16777216;

    private long producerBufferMemory = 67108864L;

    private String producerCompressionType = "lz4";

    private int producerRetries = 3;

    private int producerRetryBackoffMs = 1000;

    private int producerBatchSize = 16384;

    private int producerLingerMs = 5;
}
