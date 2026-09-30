/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.core.kafka.deserilizer;

import com.alibaba.fastjson.JSON;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * @version 1.0
 * @date 2021/12/9 13:29
 */
@Slf4j
public class AlarmDeserializer implements Deserializer<List<Alarm>> {

    private final String encoding = "UTF8";

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public List<Alarm> deserialize(String topic, byte[] data) {
        try {
            int messageSizeBytes = data.length;
            log.debug("start to deserialize the message,data  size:{}",
                    String.format("%.2f", messageSizeBytes / 1024.0));
            if (data == null) {
                return new ArrayList<>();
            } else {
                String message = new String(data, encoding);
                log.debug("end to deserialize the message,data {}", data);
                return JSON.parseArray(message, Alarm.class);
            }
        } catch (Exception e) {
            log.error(
                    "Error when deserializing byte[] to string due to unsupported UTF8_encoding "
                            + encoding, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void close() {

    }
}
