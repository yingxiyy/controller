package net.flex.dci.otc.controller.status.ne.core.deserializer;


import static net.flex.dci.otc.controller.status.util.Constants.UTF8_encoding;

import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 *
 * @version 1.0
 * @date 8/26/2025 1:32 PM
 */
@Slf4j
public class StatusChangeEventDeserializer implements Deserializer<StatusChangeEvent> {


    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public StatusChangeEvent deserialize(String topic, byte[] data) {
        try {
            log.debug("start to deserialize the status change message,data {}", data);
            if (data == null) {
                return null;
            } else {
                String message = new String(data, UTF8_encoding);
                log.debug("end to deserialize the status change message,data {}", message);
                return JSON.parseObject(message, StatusChangeEvent.class);
            }
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException(
                    "Error when deserializing byte[] to string due to unsupported UTF8_encoding "
                            + UTF8_encoding);
        }
    }

    @Override
    public void close() {

    }
}
