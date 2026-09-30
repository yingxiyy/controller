package net.flex.dci.otn.controller.db.monitor.core.processor;

import static net.flex.dci.otn.controller.db.monitor.utils.Constants.DB_CHANGE_TOPIC;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.CDCListener;
import net.flex.dci.otn.controller.tools.kafka.service.ConsumerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 2026/9/19
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "db-monitor.processor.enabled", havingValue = "true")
public class DataChangeProcessor {


    private static final JsonConverter VALUE_CONVERTER = new JsonConverter();

    static {
        Map<String, String> config = new HashMap<>();
        config.put("schemas.enable", "true");
        VALUE_CONVERTER.configure(config, false);
    }

    private final CDCListener cdcListener;

    private final ConsumerService consumerService;

    @PostConstruct
    public void start() {
        MessageListener<String, byte[]> listener = (ConsumerRecord<String, byte[]> record) -> {
            try {
                byte[] valueBytes = record.value();
                SchemaAndValue sav = VALUE_CONVERTER.toConnectData(DB_CHANGE_TOPIC, valueBytes);
                Struct value = (Struct) sav.value();
                SourceRecord sourceRecord = new SourceRecord(null, null, DB_CHANGE_TOPIC,
                        sav.schema(), value);
                cdcListener.processEvent(sourceRecord);
            } catch (Exception e) {
                log.error("db-change consume error", e);
            }
        };
        consumerService.addListener(DB_CHANGE_TOPIC, ByteArrayDeserializer.class, listener);
        log.info("[db-monitor] DataChangeProcessor subscribed to topic {}", DB_CHANGE_TOPIC);
    }
}
