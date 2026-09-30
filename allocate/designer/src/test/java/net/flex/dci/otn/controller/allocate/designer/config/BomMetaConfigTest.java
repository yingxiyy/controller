/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others. All rights reserved.
 */
package net.flex.dci.otn.controller.allocate.designer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class BomMetaConfigTest {

    private BomMetaConfig bomMetaConfig;

    @BeforeEach
    void setUp() throws Exception {
        bomMetaConfig = new BomMetaConfig();
        setYangModel("ByteDance");
        Field resourceLoader = BomMetaConfig.class.getDeclaredField("resourceLoader");
        resourceLoader.setAccessible(true);
        resourceLoader.set(bomMetaConfig, new DefaultResourceLoader());
        bomMetaConfig.load();
    }

    @Test
    void legacyKeyUsesConfiguredYangModel() {
        assertEquals("COHERENT-CHASSIS-BYTEDANCE-MUX64",
                bomMetaConfig.getKey("COHERENT", "CHASSIS", "MUX64", null));
    }

    @Test
    void bone20ProductTypeSelectsChassis20BomKey() {
        assertEquals("COHERENT-CHASSIS2.0-BYTEDANCE-MUX_32",
                bomMetaConfig.getKey("COHERENT", "CHASSIS2.0", "MUX_32", null));
    }

    @Test
    void nonCoherentChassis20Keeps2606YangModelKeyRule() throws Exception {
        setYangModel("ByteDance2.0");

        assertEquals("OTHER-CHASSIS2.0-BYTEDANCE2.0-MUX_32",
                bomMetaConfig.getKey("OTHER", "CHASSIS2.0", "MUX_32", null));
    }

    @Test
    void byteDance2BomContainsMux32UsedByCardJson() {
        String key = bomMetaConfig.getKey("COHERENT", "CHASSIS2.0", "MUX_32", null);

        assertEquals("MUX_32", bomMetaConfig.getBomMetaInfo(key).getEquipTypeConfiged());
        assertNotNull(bomMetaConfig.getBomMetaInfo(key).getPn());
        assertNotNull(bomMetaConfig.getBomMetaInfo(key).getFru());
    }

    @Test
    void loadDoesNotCreateResourceModelAliasKeys() throws Exception {
        Field bomMetaMapField = BomMetaConfig.class.getDeclaredField("bomMetaMap");
        bomMetaMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> bomMetaMap = (Map<String, ?>) bomMetaMapField.get(bomMetaConfig);

        assertFalse(bomMetaMap.keySet().stream().anyMatch(key -> key.contains("-BYTEDANCE2.0-")));
    }

    private void setYangModel(String model) throws Exception {
        Field yangModel = BomMetaConfig.class.getDeclaredField("yangModel");
        yangModel.setAccessible(true);
        yangModel.set(bomMetaConfig, model);
    }
}
