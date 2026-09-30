package net.flex.dci.otc.controller.status.alarm.core.kafka.deserilizer;

import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.AppAlarm;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * @version 1.0
 * @date 2022/6/7 13:49
 */
@Slf4j
public class AppAlarmDeserializer implements Deserializer<AppAlarm> {

    private final String encoding = "UTF8";


    @Override
    public AppAlarm deserialize(String topic, byte[] data) {
        try {
            log.debug("start to deserialize the message,data {}", data);
            if (data == null) {
                return null;
            } else {
                String message = new String(data, encoding);
                log.debug("end to deserialize the message,data {}", data);
                return JSON.parseObject(message, AppAlarm.class);
            }
        } catch (UnsupportedEncodingException e) {
            log.error(
                    "Error when deserializing byte[] to string due to unsupported UTF8_encoding "
                            + encoding, e);
            return null;
        }
    }
}
