package net.flex.dci.otn.controller.user.configuration;

import net.flex.dci.otc.cache.redis.configuration.RedisConfiguration;
import net.flex.dci.otc.cache.redis.properties.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.ClusterServersConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** Retains shared Redis operations/locks while sizing only this application's client threads. */
@Configuration
public class UserRedisConfiguration extends RedisConfiguration {
    @Value("${redis.redisson.threads:2}")
    private int threads;
    @Value("${redis.redisson.nettyThreads:4}")
    private int nettyThreads;

    private Config clientConfig() {
        Config config = new Config();
        config.setThreads(threads);
        config.setNettyThreads(nettyThreads);
        return config;
    }

    // Override client factories only; preserve each mode's codec, authentication and pool settings.
    @Override
    @Bean
    @ConditionalOnProperty(prefix = "redis.redisson", name = "mode", havingValue = "single", matchIfMissing = true)
    public RedissonClient redissonSingle(RedisProperties properties) {
        Config config = clientConfig();
        SingleServerConfig server = config.useSingleServer().setAddress(properties.getAddress())
                .setTimeout(properties.getTimeout()).setConnectionPoolSize(properties.getConnectPoolSize())
                .setConnectionMinimumIdleSize(properties.getConnectionMinimumIdleSize());
        if (StringUtils.hasText(properties.getPassword())) {
            server.setPassword(properties.getPassword());
        }
        return Redisson.create(config);
    }

    @Override
    @Bean
    @ConditionalOnProperty(prefix = "redis.redisson", name = "mode", havingValue = "sentinel")
    public RedissonClient redissonSentinel(RedisProperties properties) {
        Config config = clientConfig();
        config.setCheckLockSyncedSlaves(false);
        config.setCodec(new org.redisson.codec.Kryo5Codec());
        SentinelServersConfig server = config.useSentinelServers()
                .addSentinelAddress(properties.getSentinelAddresses()).setMasterName(properties.getMasterName())
                .setTimeout(properties.getTimeout()).setMasterConnectionPoolSize(properties.getMasterConnectionPoolSize())
                .setSlaveConnectionPoolSize(properties.getSlaveConnectionPoolSize());
        if (StringUtils.hasText(properties.getPassword())) {
            server.setPassword(properties.getPassword());
        }
        return Redisson.create(config);
    }

    @Override
    @Bean
    @ConditionalOnProperty(prefix = "redis.redisson", name = "mode", havingValue = "cluster")
    public RedissonClient redissonCluster(RedisProperties properties) {
        Config config = clientConfig();
        ClusterServersConfig server = config.useClusterServers().addNodeAddress(properties.getClusterNodeAddresses())
                .setConnectTimeout(properties.getTimeout()).setMasterConnectionPoolSize(properties.getMasterConnectionPoolSize())
                .setSlaveConnectionPoolSize(properties.getSlaveConnectionPoolSize())
                .setSubscriptionConnectionPoolSize(properties.getConnectPoolSize()).setTimeout(properties.getTimeout());
        if (StringUtils.hasText(properties.getPassword())) {
            server.setPassword(properties.getPassword());
        }
        return Redisson.create(config);
    }
}
