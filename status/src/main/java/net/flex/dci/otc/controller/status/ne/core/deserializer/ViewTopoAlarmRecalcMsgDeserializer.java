package net.flex.dci.otc.controller.status.ne.core.deserializer;

import static net.flex.dci.otc.controller.status.util.Constants.UTF8_encoding;

import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.view.ViewTopoAlarmRecalcMsg;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * 2026/2/22
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class ViewTopoAlarmRecalcMsgDeserializer implements Deserializer<ViewTopoAlarmRecalcMsg> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public ViewTopoAlarmRecalcMsg deserialize(String s, byte[] data) {
        try {
            log.debug("start to deserialize the View topo alarm recalc  message,data {}", data);
            if (data == null) {
                return null;
            } else {
                String message = new String(data, UTF8_encoding);
                log.debug("end to deserialize the View topo alarm recalc message,data {}", message);
                return JSON.parseObject(message, ViewTopoAlarmRecalcMsg.class);
            }
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException(
                    "Error when deserializing byte[] to string due to unsupported UTF8_encoding "
                            + UTF8_encoding);
        }
    }
}
