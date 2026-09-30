package net.flex.dci.otn.controller.db.monitor.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.db.monitor.core.dto.Property;

/**
 * @version 1.0
 * @date 2022/11/17 13:12
 */
@Slf4j
public class PhysicalPropertyTool {

    public static void putKeyValue(List<Property> pList, String key, String value) {
        if (value == null) {
            return;
        }
        Map<String, String> propertyMap = pList.stream().collect(HashMap::new,
                (map, property) -> map.put(property.getName(), property.getValue()),
                HashMap::putAll);
        propertyMap.put(key, value);
        List<Property> properties = propertyMap.entrySet().stream()
                .map(entry -> Property.builder().name(entry.getKey())
                        .value(entry.getValue())
                        .build()).collect(Collectors.toList());
        pList.clear();
        pList.addAll(properties);
    }

    public static List<Property> mergeProperty(List<Property> propertyList,
            List<Property> additionalProperty) {
        if (propertyList == null || propertyList.isEmpty()) {
            return additionalProperty;
        }
        Map<String, String> propertyMap = propertyList.stream().collect(HashMap::new,
                (map, property) -> map.put(property.getName(), property.getValue()),
                HashMap::putAll);
        additionalProperty.forEach(property -> {
            propertyMap.put(property.getName(), property.getValue());
        });
        List<Property> properties = propertyMap.entrySet().stream()
                .map(entry -> Property.builder().name(entry.getKey())
                        .value(entry.getValue())
                        .build()).collect(Collectors.toList());
        return properties;
    }
}
