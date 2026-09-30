package net.flex.dci.otc.controller.ne.manager.core.kafka.deserializer;

import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * @version 1.0
 * @date 2022/4/27 16:02
 */
@Slf4j
public class ElementChangeDeserializer implements Deserializer<ElementChange> {

    @Override
    public ElementChange deserialize(String topic, byte[] data) {
        log.debug("deserializer topic :{}", topic);
        String encoding = "UTF8";
        try {
            log.debug("start to deserialize the message,data {}", data);
            if (data == null) {
                return null;
            } else {
                String message = new String(data, encoding);
                log.debug("end to deserialize the message,data {}", data);
                return JSON.parseObject(message, ElementChange.class);
            }
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException(
                    "Error when deserializing byte[] to string due to unsupported encoding "
                            + encoding);
        }
    }
}
