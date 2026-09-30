/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.ne.core.deserializer;

import static net.flex.dci.otc.controller.status.util.Constants.UTF8_encoding;

import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * @version 1.0
 * @date 2021/12/9 13:29
 */
@Slf4j
public class NeStatusMessageDeserializer implements Deserializer<NeStatusMessage> {


    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public NeStatusMessage deserialize(String topic, byte[] data) {
        try {
            log.debug("start to deserialize the ne status message,data {}", data);
            if (data == null) {
                return null;
            } else {
                String message = new String(data, UTF8_encoding);
                log.debug("end to deserialize the ne status message,data {}", message);
                return JSON.parseObject(message, NeStatusMessage.class);
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
