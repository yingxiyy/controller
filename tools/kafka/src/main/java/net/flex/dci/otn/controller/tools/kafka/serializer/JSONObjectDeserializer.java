package net.flex.dci.otn.controller.tools.kafka.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * @version 1.0
 * @date 2022/5/9 23:23
 */
public class JSONObjectDeserializer implements Deserializer<JSONObject> {

    @Override
    public JSONObject deserialize(String s, byte[] bytes) {
        return JSON.parseObject(bytes, JSONObject.class);
    }
}
