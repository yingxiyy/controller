/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.db.monitor.core;

import static io.debezium.data.Envelope.FieldName.AFTER;
import static io.debezium.data.Envelope.FieldName.OPERATION;
import static io.debezium.data.Envelope.FieldName.SOURCE;
import static java.util.stream.Collectors.toMap;
import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DB_CHANGE_TOPIC;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.debezium.config.Configuration;
import io.debezium.connector.mongodb.SourceInfo;
import io.debezium.data.Envelope.Operation;
import io.debezium.embedded.EmbeddedEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.service.DataChangeService;
import net.flex.dci.otn.controller.db.monitor.properties.DbMonitorProperties;
import net.flex.dci.otn.controller.tools.kafka.service.PublishService;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.bson.Document;
import org.eclipse.xtext.xbase.lib.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/3 10:54
 */
@Slf4j
@Component
public class CDCListener {

    @Autowired
    private DataChangeService dataChangeService;

    private final AtomicBoolean running = new AtomicBoolean(false);


    private ExecutorService engineExecutor;

    private static final int WORKER_COUNT = 4;
    private final List<ExecutorService> workers = new ArrayList<>(WORKER_COUNT);

    private EmbeddedEngine engine;

    private final Configuration customerConfiguration;

    private final boolean haEnabled;

    private final boolean processorEnabled;

    private final int partitions;

    private final PublishService publishService;

    private static final Gson gson = new Gson();

    private static final JsonConverter VALUE_CONVERTER = new JsonConverter();

    static {
        Map<String, String> config = new HashMap<>();
        config.put("schemas.enable", "true");
        VALUE_CONVERTER.configure(config, false);
    }

    private CDCListener(Configuration customerConfiguration,
            DbMonitorProperties dbMonitorProperties, PublishService publishService) {
        this.haEnabled = dbMonitorProperties.getHa().isEnabled();
        this.processorEnabled = dbMonitorProperties.getProcessor().isEnabled();
        this.partitions = dbMonitorProperties.getProcessor().getPartitions();
        this.customerConfiguration = customerConfiguration;
        this.publishService = publishService;
        this.engineExecutor = newEngineExecutor();
        for (int i = 0; i < WORKER_COUNT; i++) {
            ExecutorService worker = new ThreadPoolExecutor(
                    1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(500),
                    new ThreadFactoryBuilder().setNameFormat("cdc-worker-" + i).build(),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
            this.workers.add(worker);
        }
    }

    private ExecutorService newEngineExecutor() {
        return new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1),
                new ThreadFactoryBuilder().setNameFormat("debezium-engine").build()
        );
    }

    @PostConstruct
    private void start() {
//        this.engineExecutor.execute(engine);
        if (processorEnabled) {
            ensureDbChangeTopic();
        }
        if (!haEnabled) {
            startEngine();
        }
    }

    @PreDestroy
    private void stop() {
//        this.engine.stop();
//        this.engineExecutor.shutdown();
        stopEngine();
        this.workers.forEach(ExecutorService::shutdown);
        for (ExecutorService w : this.workers) {
            try {
                w.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                w.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void startEngine() {
        if (running.get()) {
            return;
        }
        if (engineExecutor == null || engineExecutor.isShutdown()) {
            this.engineExecutor = newEngineExecutor();
        }
        log.info("[db-monitor] taking leadership, starting CDC engine");
        this.engine = EmbeddedEngine.create().using(customerConfiguration)
                .notifying(this::handleEvent).build();
        this.engineExecutor.execute(engine);
        running.set(true);
    }

    public synchronized void stopEngine() {
        if (!running.get()) {
            return;
        }
        log.warn("[db-monitor] losing leadership, stopping CDC engine");
        try {
            this.engine.stop();
            this.engineExecutor.shutdown();
            this.engineExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("engine stop error", e);
        } finally {
            running.set(false);
        }
    }

    public void handleEvent(SourceRecord sourceRecord) {
        Struct value = (Struct) sourceRecord.value();
        if (value == null) {
            return;
        }
        if (processorEnabled) {
            sendChange2Kafka(sourceRecord);
        } else {
            processEvent(sourceRecord);
        }
    }


    public void processEvent(SourceRecord sourceRecord) {
        Struct sourceRecordValue = (Struct) sourceRecord.value();
        if (sourceRecordValue == null) {
            return;
        }

        Operation operation = Operation.forCode(
                (String) sourceRecordValue.get(OPERATION));
        if (operation == Operation.READ || operation == Operation.DELETE) {
            return;
        }
        log.debug("start to handle the source record, {}", sourceRecordValue);
        Object key = sourceRecord.key();
//        String filter = (String) sourceRecordValue.get(MongoDbFieldName.FILTER);
//        Struct source = (Struct) sourceRecordValue.get(SOURCE);
//        String collection = (String) source.get(SourceInfo.COLLECTION);
        String routingKey = routingKeyOf(sourceRecordValue, key);
        int idx = Math.abs(routingKey.hashCode()) % WORKER_COUNT;

        this.workers.get(idx).execute(() -> {
            try {
                Map<String, Object> payload = sourceRecordValue.schema().fields().stream()
                        .map(Field::name)
                        .filter(fieldName -> sourceRecordValue.get(fieldName) != null)
                        .map(fieldName -> Pair.of(fieldName,
                                sourceRecordValue.get(fieldName)))
                        .collect(toMap(Pair::getKey, Pair::getValue));
                log.debug("the pay load is {}", payload);

                dataChangeService.handlerDataChange(operation, payload);
            } catch (Exception e) {
                log.error("cdc event handle error", e);
            }
        });
    }


    private void sendChange2Kafka(SourceRecord sourceRecord) {
        Struct value = (Struct) sourceRecord.value();
        Object key = sourceRecord.key();
        Operation operation = Operation.forCode((String) value.get(OPERATION));
        if (operation == Operation.READ || operation == Operation.DELETE) {
            return;
        }

        String routingKey = routingKeyOf(value, key);
        byte[] bytes = structToJsonBytes(value);
        publishService.send(DB_CHANGE_TOPIC, routingKey, bytes);
    }

    private String routingKeyOf(Struct value, Object documentKey) {
        Struct source = (Struct) value.get(SOURCE);
        String collection = (String) source.get(SourceInfo.COLLECTION);
        Object id = extractId(documentKey, value);
        return collection + ":" + (id == null ? "" : id.toString());
    }

    private Object extractId(Object documentKey, Struct value) {
        if (documentKey instanceof Struct) {
            Object oid = extractOidHex(((Struct) documentKey).get("id"));
            if (oid != null) {
                return oid;
            }
        }
        String after = (String) value.get(AFTER);
//        if (after == null) {
//            after = (String) value.get(BEFORE);
//        }
        if (after != null) {
            Document afterDoc = Document.parse(after);
            Object oid = afterDoc.get("_id");
            if (oid != null) {
                return oid;
            }
        }
        return null;
    }

    private Object extractOidHex(Object id) {
        if (id == null) {
            return null;
        }
        if (id instanceof Struct) {
            Object oid = ((Struct) id).get("$oid");
            return oid != null ? oid : id;
        }
        if (id instanceof String) {
            String s = (String) id;

            if (s.startsWith("{") && s.contains("$oid")) {
                try {
                    JsonElement el = gson.fromJson(s, JsonElement.class);
                    if (el != null && el.isJsonObject() && el.getAsJsonObject().has("$oid")) {
                        return el.getAsJsonObject().get("$oid").getAsString();
                    }
                } catch (Exception e) {
                    log.error("failed to parse the oid the exception is:{}", e.getMessage(), e);
                }
            }
            return s;
        }
        return id;
    }

    private byte[] structToJsonBytes(Struct value) {
        return VALUE_CONVERTER.fromConnectData(DB_CHANGE_TOPIC, value.schema(), value);
    }

    private void ensureDbChangeTopic() {
        try {
            int partitions = this.partitions;
            publishService.createTopicIfNotExists(DB_CHANGE_TOPIC, partitions);
            log.info("[db-monitor] ensured topic {} with partitions {}", DB_CHANGE_TOPIC,
                    partitions);
        } catch (Exception e) {
            log.warn("[db-monitor] ensure topic {} failed: {}", DB_CHANGE_TOPIC, e.getMessage());
        }
    }
}
