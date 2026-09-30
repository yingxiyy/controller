package net.flex.dci.otc.controller.ne.manager.core.kafka.deserializer;

import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.NeState;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * @version 1.0
 * @date 2022/3/25 13:42
 */
@Deprecated
@Slf4j
public class NeStateDeserializer implements Deserializer<NeState> {


    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public NeState deserialize(String topic, byte[] data) {
        log.debug("deserializer topic :{}", topic);
        String encoding = "UTF8";
        try {
            log.debug("start to deserialize the message,data {}", data);
            if (data == null) {
                return null;
            } else {
                String message = new String(data, encoding);
                log.debug("end to deserialize the message,data {}", data);
                return JSON.parseObject(message, NeState.class);
            }
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException(
                    "Error when deserializing byte[] to string due to unsupported encoding "
                            + encoding);
        }
    }


    @Override
    public NeState deserialize(String topic, Headers headers, byte[] data) {
        return Deserializer.super.deserialize(topic, headers, data);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
