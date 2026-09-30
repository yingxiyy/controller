package net.flex.dci.otc.controller.ne.manager.properties;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.TelemetryConfig.TELEMETRY_SENSOR_GROUP_FILE;

import com.alibaba.fastjson.JSON;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.model.SensorGroup;
import net.flex.dci.otc.controller.ne.manager.model.TelemetrySensorGroup;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 6/11/2025 10:52 AM
 */
@Configuration
@Slf4j
public class TelemetrySensorGroupProperties {


    private static final String VENDOR_CONFIG_PREFIX = "telemetry-vendor-";
    private static final String VENDOR_CONFIG_SUFFIX = ".json";
    private final Map<String, SensorGroup> telemetrySensorGroupMap;
    private final Map<String, Map<String, SensorGroup>> telemetryVendorSensorGroupMap = new ConcurrentHashMap<>();

    public TelemetrySensorGroupProperties() throws IOException {
        log.info("load telemetry Sensor Group properties");
        InputStream inputStream = NeManagerUtils.loadFileInputStream(TELEMETRY_SENSOR_GROUP_FILE);
        TelemetrySensorGroup telemetrySensorGroup = JSON.parseObject(inputStream,
                TelemetrySensorGroup.class);
        this.telemetrySensorGroupMap = telemetrySensorGroup.getTelemetrySensorGroup().stream()
                .flatMap(sensorGroupInfo -> sensorGroupInfo.getSensorGroups().stream()
                                .map(sensorGroup ->
//                                new AbstractMap.SimpleEntry<>(
//                                sensorGroupInfo.getVersion().toUpperCase()
//                                        + sensorGroup.getSensorGroupName(), sensorGroup))
                                {
                                    String key = sensorGroupInfo.getVersion().toUpperCase()
                                            + sensorGroup.getSensorGroupName();
                                    sensorGroup.setSensorGroupName(key);
                                    return new AbstractMap.SimpleEntry<>(key, sensorGroup);
                                })
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (existing, replacement) -> existing));
        scanAndLoadVendorConfig();
    }


    private void scanAndLoadVendorConfig() {
        log.info("load telemetry sensor group by vendor json");
        List<String> vendorConfigFiles = findVendorFiles();
        if (vendorConfigFiles.isEmpty()) {
            log.info("No vendor-specific configuration files found");
            return;
        }
        int loadVendorEntries = 0;
        for (String fileName : vendorConfigFiles) {
            try {
                int loaded = loadSingleVendorConfig(fileName);
                loadVendorEntries += loaded;
            } catch (Exception e) {
                log.error("Failed to load vendor config file: {}", fileName, e);
            }
        }
        log.info("Successfully loaded {} entries from {} vendor config file(s)",
                loadVendorEntries, vendorConfigFiles.size());
    }

    private int loadSingleVendorConfig(String fileName) throws IOException {
        InputStream inputStream = NeManagerUtils.loadFileInputStream(fileName);
        if (inputStream == null) {
            throw new FileNotFoundException("Vendor config file not found: " + fileName);
        }

        // 从文件名提取厂商标识
        String vendor = extractVendorFromFileName(fileName);
        if (vendor == null || vendor.isEmpty()) {
            log.warn("Could not extract vendor from filename: {}, skipping", fileName);
            return 0;
        }
        TelemetrySensorGroup telemetrySensorGroup = JSON.parseObject(inputStream,
                TelemetrySensorGroup.class);
        Map<String, SensorGroup> sensorGroupMap = new ConcurrentHashMap<>();
        sensorGroupMap = telemetrySensorGroup.getTelemetrySensorGroup().stream()
                .flatMap(sensorGroupInfo -> sensorGroupInfo.getSensorGroups().stream()
                        .map(sensorGroup -> new AbstractMap.SimpleEntry<>(
                                sensorGroupInfo.getVersion().toUpperCase()
                                        + sensorGroup.getSensorGroupName(), sensorGroup)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (existing, replacement) -> existing));
        telemetryVendorSensorGroupMap.put(vendor, sensorGroupMap);
        return 1;
    }

    private String extractVendorFromFileName(String fileName) {
        if (!fileName.startsWith(VENDOR_CONFIG_PREFIX) ||
                !fileName.endsWith(VENDOR_CONFIG_SUFFIX)) {
            return null;
        }

        String vendorPart = fileName.substring(
                VENDOR_CONFIG_PREFIX.length(),
                fileName.length() - VENDOR_CONFIG_SUFFIX.length()
        ).trim();

        int dashIndex = vendorPart.indexOf('-');
        if (dashIndex > 0) {
            return vendorPart.substring(0, dashIndex);
        }

        return vendorPart;
    }

    private List<String> findVendorFiles() {
        List<String> vendorFiles = new ArrayList<>();
        String rootDir = System.getProperty("user.dir");
        File configDir = new File(rootDir, "config");
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) ->
                    name.startsWith(VENDOR_CONFIG_PREFIX) && name.endsWith(VENDOR_CONFIG_SUFFIX));

            if (files != null) {
                for (File file : files) {
                    vendorFiles.add(file.getName());
                    log.debug("Found vendor config in filesystem: {}", file.getName());
                }
            }
        }
        if (vendorFiles.isEmpty()) {
            log.debug("No vendor configs found in filesystem, trying classpath...");
            try {
                String[] knownVendors = {"huawei", "accelink", "coherent"};
                for (String vendor : knownVendors) {
                    String fileName = VENDOR_CONFIG_PREFIX + vendor + VENDOR_CONFIG_SUFFIX;
                    try {
                        InputStream is = NeManagerUtils.loadFileInputStream(fileName);
                        if (is != null) {
                            vendorFiles.add(fileName);
                            log.debug("Found vendor config in classpath: {}", fileName);
                        }
                    } catch (IOException e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                log.warn("Error scanning classpath for vendor configs", e);
            }
        }
        return vendorFiles;
    }

    public SensorGroup getTelemetrySensorGroup(String neYangVersion, NodeType nodeType) {
        log.debug("get telemetry sensor path and group by neYangVersion:{} nodeType:{}",
                neYangVersion, nodeType);
        String sensorGroupKey = neYangVersion.toUpperCase() + nodeType.name().toUpperCase();
        return telemetrySensorGroupMap.getOrDefault(sensorGroupKey, null);
    }

    public SensorGroup getTelemetrySensorGroup(String vendor, String neYangVersion,
            NodeType nodeType) {
        log.debug("get telemetry sensor path and group by vendor:{} neYangVersion:{} nodeType:{}",
                vendor,
                neYangVersion, nodeType);

        String sensorGroupKey = neYangVersion.toUpperCase() + nodeType.name().toUpperCase();
//        if (!groupNamePrefix.equals(BLANK)) {
//            sensorGroupKey = groupNamePrefix + sensorGroupKey;
//        }
        if (StringUtils.hasText(vendor)) {
            Map<String, SensorGroup> vendorMap = telemetryVendorSensorGroupMap.get(
                    vendor.toLowerCase());
            if (vendorMap != null) {
                SensorGroup vendorConfig = vendorMap.get(sensorGroupKey);
                if (vendorConfig != null) {
                    log.debug("Found vendor-specific config for {}: {}", vendor, sensorGroupKey);
                    return deepCopySensorGroup(vendorConfig);
                }
                log.debug("No vendor-specific config found for {}: {}, falling back to default",
                        vendor, sensorGroupKey);
            }
        }
        SensorGroup defaultConfig = telemetrySensorGroupMap.get(sensorGroupKey);
        if (defaultConfig != null) {
            log.debug("Using default config: {}", sensorGroupKey);
            return deepCopySensorGroup(defaultConfig);
        }

        log.warn("No telemetry config found for Vendor: {}, Version: {}, Type: {}",
                vendor, neYangVersion, nodeType.name());
        return null;
    }


    private SensorGroup deepCopySensorGroup(SensorGroup original) {
        if (original == null) {
            return null;
        }

        return JSON.parseObject(JSON.toJSONString(original), SensorGroup.class);
    }
}
