/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.config;

import io.debezium.config.Configuration.Builder;
import java.util.List;
import net.flex.dci.otc.mongo.configuration.MongoDBProperties;
import net.flex.dci.otn.controller.db.monitor.core.NetConfEventChangeHandlerDispatcher;
import net.flex.dci.otn.controller.db.monitor.core.handler.AbstractBodyChangeHandler;
import net.flex.dci.otn.controller.db.monitor.properties.DbMonitorProperties;
import net.flex.dci.otn.controller.tools.kafka.properties.KafkaProperties;
import net.flex.dci.otn.topology.cache.EnableDciCache;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @version 1.0
 * @date 2021/11/2 17:03
 */
@ConditionalOnClass({MongoDBProperties.class})
@Configuration
@EnableConfigurationProperties(DbMonitorProperties.class)
@EnableDciCache
public class DbMonitorConfiguration {

    @Value("${db-monitor.offset.replication-factor:3}")
    private int offsetRf;

    @Bean(name = "eventChangeHandlerDispatcher")
    public NetConfEventChangeHandlerDispatcher netConfEventChangeHandlerDispatcher(
            DciTopologyCacheManager dciTopologyCacheManager,
            List<AbstractBodyChangeHandler> handlers) {
        return new NetConfEventChangeHandlerDispatcher(dciTopologyCacheManager, handlers);
    }


    @Bean
    @DependsOn("eventChangeHandlerDispatcher")
    public io.debezium.config.Configuration customerConnector(MongoDBProperties mongoProperties,
            KafkaProperties kafkaProperties) {
        Builder configurationBuilder = io.debezium.config.Configuration.create()
                .with("offset.storage", "org.apache.kafka.connect.storage.KafkaOffsetBackingStore")
                .with("offset.storage.topic", "db-monitor-offsets")
                .with("offset.storage.partitions", "1")
                .with("bootstrap.servers", kafkaProperties.getBootstrapServers())
                .with("offset.storage.replication.factor", String.valueOf(offsetRf))
                .with("offset.flush.interval.ms", 5000)
                .with("name", "db-monitor")
                .with("connector.class", "io.debezium.connector.mongodb.MongoDbConnector")
                .with("mongodb.hosts",
                        mongoProperties.getServers())
                .with("mongodb.name", mongoProperties.getDatabase() + "-mongodb-connector")
//                .with("transforms", "v2tov1")
//                .with("transforms.v2tov1.type",
//                        "net.flex.dci.otn.controller.db.monitor.transforms.V2ToV1Patch")
//                // (可选) 保留原 patch 字段备份
//                .with("transforms.v2tov1.preserveOriginal", "true")
                .with("database.include.list", mongoProperties.getDatabase());
        if (StringUtils.isNotBlank(mongoProperties.getUser())) {
            configurationBuilder.with("mongodb.user", mongoProperties.getUser())
                    .with("mongodb.password", mongoProperties.getPwd())
                    .with("mongodb.authsource", MongoDBProperties.AUTH_DB);
        }
        return configurationBuilder.build();
    }

}
