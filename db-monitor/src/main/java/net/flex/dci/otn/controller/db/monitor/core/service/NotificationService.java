package net.flex.dci.otn.controller.db.monitor.core.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.ObjectChangeMessage;
import net.flex.dci.otc.common.util.SwitchCaseUtils;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.NetConfEventChangeDto;
import net.flex.dci.otn.controller.tools.kafka.service.ObjectNotifiMessager;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.json.StrictJsonWriter;
import org.bson.types.Decimal128;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/4 15:16
 */
@Component
@Slf4j
public class NotificationService {

    private static final int KAFKA_MAX_INFLIGHT = 500;
    private static final ExecutorService KAFKA_EXECUTOR =
            new ThreadPoolExecutor(
                    4, 4, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(KAFKA_MAX_INFLIGHT),
                    r -> {
                        Thread t = new Thread(r, "kafka-sender-" + System.nanoTime());
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy());

    @Log
    public void publishNotification(EventType eventType, NetConfEventChangeDto eventChangeDto) {
        log.debug("start to send notification event type is {},change data is :{}", eventType,
                eventChangeDto);
        try {
            if (eventChangeDto == null) {
                return;
            }

            String data = serializeDocument2json(eventChangeDto.getChangeData());
            ObjectChangeMessage objectChangeMessage = ObjectChangeMessage.builder()
                    .eventType(SwitchCaseUtils.lowerFirstCase(eventType.name()))
                    .data(data)
                    .dataStoreType(eventChangeDto.getDataStoreType())
                    .timestamp(System.currentTimeMillis())
                    .objectType(SwitchCaseUtils.lowerFirstCase(eventChangeDto.getObjectType()))
                    .topologyRef(eventChangeDto.getTopologyRef())
                    .topologyType(eventChangeDto.getTopologyType())
                    .build();

            CompletableFuture.runAsync(() -> {
                try {
                    ObjectNotifiMessager.publishKafkaMessage(objectChangeMessage);
                    log.info("Kafka message sent successfully");
                } catch (Exception e) {
                    log.error("Failed to send Kafka notification: {}", e.getMessage(), e);
                }
            }, KAFKA_EXECUTOR);

            log.info("notification submitted to async queue");
        } catch (Exception e) {
            log.error("Failed to prepare notification: {}", e.getMessage(), e);
        }
    }

    private String serializeDocument2json(Document document) {
        JsonWriterSettings jsonWriterSettings = JsonWriterSettings.builder()
                .outputMode(JsonMode.EXTENDED)
                .int64Converter((Long value, StrictJsonWriter writer) -> writer.writeString(
                        Long.toString(value)))
                .int32Converter((Integer value, StrictJsonWriter writer) -> writer.writeNumber(
                        Integer.toString(value)))
                .doubleConverter((Double value, StrictJsonWriter writer) -> writer.writeNumber(
                        Double.toString(value)))
                .decimal128Converter(
                        (Decimal128 value, StrictJsonWriter writer) -> writer.writeNumber(
                                value.toString()))
                .build();
        return document.toJson(jsonWriterSettings);
    }


}
