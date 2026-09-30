package net.flex.dci.otn.controller.tools.kafka.serializer;

import com.alibaba.fastjson.JSON;
import org.apache.kafka.common.serialization.Serializer;

/**
 * @version 1.0
 * @date 2022/5/9 23:19
 */
public class JSONObjectSerializer implements Serializer<Object> {


    @Override
    public byte[] serialize(String s, Object jsonObject) {
        return JSON.toJSONBytes(jsonObject);
    }
}
