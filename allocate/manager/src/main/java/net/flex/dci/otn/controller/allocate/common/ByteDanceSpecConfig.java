package net.flex.dci.otn.controller.allocate.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.allocate.network.bytedance.ase.ByteDanceSpec;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FIXEDGAINRANGE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GAINRANGE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.HIGHGAINRANGE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LOWGAINRANGE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.AmpMode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ByteDanceSpecConfig {
    private static final String CONFIG_PATH = "bytedanceSpec.json";
    private static final String DEFAULT_SELECTOR = "DEFAULT";

    public Map<String, ScopedAmplifierProfile> amplifierProfiles = new LinkedHashMap<>();
    public Map<String, ScopedProperties> terminationPointProperties = new LinkedHashMap<>();
    public Map<String, Map<String, String>> equipmentProperties = new LinkedHashMap<>();

    public static ByteDanceSpecConfig load() {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassLoader classLoader = ByteDanceSpecConfig.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(CONFIG_PATH)) {
            if (inputStream == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Cannot find ByteDance spec config: " + CONFIG_PATH);
            }
            return objectMapper.readValue(inputStream, ByteDanceSpecConfig.class);
        } catch (IOException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Failed to load ByteDance spec config: " + CONFIG_PATH, e);
        }
    }

    public AmplifierProfile getAmplifierProfile(String key, WDM_Band wdmBand, GridType grid) {
        ScopedAmplifierProfile profile = amplifierProfiles.get(key);
        if (profile == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Missing amplifier profile config: " + key);
        }
        return profile.resolve(key, wdmBand, grid);
    }

    public Map<String, String> getTerminationPointProperties(String key, WDM_Band wdmBand, GridType grid) {
        ScopedProperties properties = terminationPointProperties.get(key);
        if (properties == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Missing termination point config: " + key);
        }
        return Collections.unmodifiableMap(properties.resolve(key, wdmBand, grid));
    }

    public String getTerminationPointProperty(String key, String propertyKey, WDM_Band wdmBand, GridType grid) {
        return getRequiredProperty(getTerminationPointProperties(key, wdmBand, grid), key, propertyKey);
    }

    public Map<String, String> getEquipmentProperties(String key) {
        Map<String, String> properties = equipmentProperties.get(key);
        if (properties == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Missing equipment config: " + key);
        }
        return Collections.unmodifiableMap(properties);
    }

    public String getEquipmentProperty(String key, String propertyKey) {
        return getRequiredProperty(getEquipmentProperties(key), key, propertyKey);
    }

    public static class AmplifierProfile {
        public String targetGainSource;
        public Float fixedTargetGain;
        public String gainRangeSource;
        public String fixedGainRange;
        public Boolean autoPowerReduction;
        public Float targetGainTilt;
        public String ampMode;
        public Float targetAttenuation;
        public Boolean enable;
        public Map<String, String> properties = new LinkedHashMap<>();

        public String getProperty(String key, String propertyKey) {
            return getRequiredProperty(properties, key, propertyKey);
        }

        public void validate(String key) {
            if (targetGainSource == null && fixedTargetGain == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Amplifier profile target gain is not configured: " + key);
            }
            if (gainRangeSource == null && fixedGainRange == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Amplifier profile gain range is not configured: " + key);
            }
            if (ampMode == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Amplifier profile amp mode is not configured: " + key);
            }
        }

        public AmpMode resolveAmpMode(String key) {
            try {
                return AmpMode.valueOf(ampMode);
            } catch (IllegalArgumentException e) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Unsupported amp mode config in " + key + ": " + ampMode, e);
            }
        }

        public Class<? extends GAINRANGE> resolveGainRange(String key, float targetGain) {
            String gainRange = fixedGainRange;
            if ("TARGET_GAIN".equals(gainRangeSource)) {
                gainRange = targetGain > 20 ? "HIGH" : "LOW";
            }
            if ("HIGH".equals(gainRange)) {
                return HIGHGAINRANGE.class;
            }
            if ("LOW".equals(gainRange)) {
                return LOWGAINRANGE.class;
            }
            if ("FIXED".equals(gainRange)) {
                return FIXEDGAINRANGE.class;
            }
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Unsupported gain range config in " + key + ": " + gainRange);
        }

        public float resolveTargetGain(String key, ByteDanceSpec.GainValueProvider gainValueProvider) {
            if ("AMPLIFIER_GAIN".equals(targetGainSource)) {
                return gainValueProvider.get();
            }
            if (fixedTargetGain != null) {
                return fixedTargetGain;
            }
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Unsupported target gain config in " + key);
        }
    }

    public static class ScopedAmplifierProfile {
        public Map<String, GridScopedAmplifierProfile> wdmBand = new LinkedHashMap<>();

        public AmplifierProfile resolve(String key, WDM_Band currentWdmBand, GridType currentGrid) {
            GridScopedAmplifierProfile bandScoped = resolveByKeys(wdmBand, toBandKeys(currentWdmBand));
            if (bandScoped == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Missing wdmBand config for amplifier profile: " + key);
            }
            return bandScoped.resolve(key, currentGrid);
        }
    }

    public static class GridScopedAmplifierProfile {
        public Map<String, AmplifierProfile> grid = new LinkedHashMap<>();

        public AmplifierProfile resolve(String key, GridType currentGrid) {
            AmplifierProfile profile = resolveByKeys(grid, toGridKeys(currentGrid));
            if (profile == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Missing grid config for amplifier profile: " + key);
            }
            return profile;
        }
    }

    public static class ScopedProperties {
        public Map<String, GridScopedProperties> wdmBand = new LinkedHashMap<>();

        public Map<String, String> resolve(String key, WDM_Band currentWdmBand, GridType currentGrid) {
            GridScopedProperties bandScoped = resolveByKeys(wdmBand, toBandKeys(currentWdmBand));
            if (bandScoped == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Missing wdmBand config for termination point: " + key);
            }
            return bandScoped.resolve(key, currentGrid);
        }
    }

    public static class GridScopedProperties {
        public Map<String, Map<String, String>> grid = new LinkedHashMap<>();

        public Map<String, String> resolve(String key, GridType currentGrid) {
            Map<String, String> properties = resolveByKeys(grid, toGridKeys(currentGrid));
            if (properties == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Missing grid config for termination point: " + key);
            }
            return properties;
        }
    }

    private static String getRequiredProperty(Map<String, String> properties, String key, String propertyKey) {
        String value = properties.get(propertyKey);
        if (value == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Missing property config: " + key + "." + propertyKey);
        }
        return value;
    }

    private static List<String> toBandKeys(WDM_Band wdmBand) {
        if (wdmBand == null) {
            return Collections.singletonList(DEFAULT_SELECTOR);
        }
        return Arrays.asList(wdmBand.name(), wdmBand.toString(), DEFAULT_SELECTOR);
    }

    private static List<String> toGridKeys(GridType grid) {
        if (grid == null) {
            return Collections.singletonList(DEFAULT_SELECTOR);
        }
        return Arrays.asList(String.valueOf(grid.getIntValue()), grid.name(), DEFAULT_SELECTOR);
    }

    private static <T> T resolveByKeys(Map<String, T> valueMap, List<String> preferredKeys) {
        if (valueMap == null || valueMap.isEmpty()) {
            return null;
        }
        for (String preferredKey : preferredKeys) {
            T exact = valueMap.get(preferredKey);
            if (exact != null) {
                return exact;
            }
        }
        return null;
    }
}
