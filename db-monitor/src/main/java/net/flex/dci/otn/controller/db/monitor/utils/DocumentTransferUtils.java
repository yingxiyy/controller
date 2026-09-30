package net.flex.dci.otn.controller.db.monitor.utils;

import static java.util.stream.Collectors.toMap;

import com.alibaba.fastjson.JSON;
import java.util.Map;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.eclipse.xtext.xbase.lib.Pair;

/**
 * @version 1.0
 * @date 2021/11/11 11:31
 */
public class DocumentTransferUtils {

    /**
     * extract pay load
     *
     * @param document
     * @return
     */
    public static Map<String, Object> extractPayLoad(Document document) {
        Map<String, Object> updatePayLoad = document.keySet().stream()
                .map(fieldName -> Pair.of(fieldName, document.get(fieldName)))
                .collect(toMap(Pair::getKey, Pair::getValue));
        return updatePayLoad;
    }


    public static <T> Document toDocument(T object) {
        String json = JSON.toJSONString(object);
        Document document = Document.parse(json);
        return document;
    }

    public static <T> T toBean(Document document, Class<T> clazz) {
        String realJson = document.toJson(JsonWriterSettings.builder().build());
        T obj = JSON.parseObject(realJson, clazz);
        return obj;
    }


    /**
     * 根据以点分隔的字段路径获取嵌套字段值
     *
     * @param doc 根 Document
     * @param fieldPath 完整路径，如 "data.node.0.otn-phy-topology:physical.implement-state"
     * @return 对应的值或 null
     */
    public static Object getNestedField(Document doc, String fieldPath) {
        String[] keys = fieldPath.split("\\.");
        Object current = doc;
        for (String key : keys) {
            if (current instanceof Document) {
                current = ((Document) current).get(key);
            } else {
                return null;
            }
        }
        return current;
    }


}
