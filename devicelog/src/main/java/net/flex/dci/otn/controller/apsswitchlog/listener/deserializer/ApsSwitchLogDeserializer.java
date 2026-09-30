package net.flex.dci.otn.controller.apsswitchlog.listener.deserializer;

import com.alibaba.fastjson.JSON;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.db.jpa.entity.ApsSwitchLog;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * 2026/5/17
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class ApsSwitchLogDeserializer implements Deserializer<ApsSwitchLog> {

    private final String encoding = "UTF8";

    @Override
    public ApsSwitchLog deserialize(String s, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            log.debug("start to deserialize the aps switch log,data :{}", bytes);
            String jsonStr = new String(bytes, StandardCharsets.UTF_8);

            Object parse = JSON.parse(jsonStr);
            if (parse instanceof String) {
                return JSON.parseObject((String) parse, ApsSwitchLog.class);
            } else {
                return JSON.parseObject(jsonStr, ApsSwitchLog.class);
            }

        } catch (Exception e) {
            log.error(
                    "Error when deserializing byte[] to string due to unsupported UTF8_encoding "
                            + encoding, e);
            return null;
        }

    }
}
