/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others. All rights reserved.
 */
package net.flex.dci.otn.controller.allocate.designer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetSupportedFrequencyInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetSupportedFrequencyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetVendorListInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetVendorListOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.vendor.list.output.Vendor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class NEInfoConfigTest {

    private NEInfoConfig neInfoConfig;

    @BeforeEach
    void setUp() throws Exception {
        neInfoConfig = new NEInfoConfig();
        setField("yangModel", "ByteDance");
        setField("resourceLoader", new DefaultResourceLoader());
        neInfoConfig.load();
    }

    @Test
    void legacyLookupKeepsByteDanceProductType() throws Exception {
        NeInfo neInfo = neInfoConfig.getNeInfo("COHERENT", "CHASSIS", "OD");

        assertTrue(neInfo.getJsonFileName().contains("ByteDance-OD-card.json"));
    }

    @Test
    void bone20ProductTypeSelectsChassis20CardJson() throws Exception {
        NeInfo neInfo = neInfoConfig.getNeInfo("COHERENT", "CHASSIS2.0", "OD");

        assertTrue(neInfo.getJsonFileName().contains("CHASSIS2.0-ByteDance-OD-card.json"));
        assertEquals("150GHz", neInfo.getSupportedFrequency().stream()
                .filter("150GHz"::equals).findFirst().orElse(null));
    }

    @Test
    void bone20TdLookupKeepsOriginalChassisTdResource() throws Exception {
        NeInfo neInfo = neInfoConfig.getNeInfo("COHERENT", "CHASSIS2.0", "TD");

        assertTrue(neInfo.getJsonFileName().contains("CHASSIS-ByteDance-TD-card.json"));
    }

    @Test
    void byteDance20SelectsConcreteMuxByFixedGrid() throws Exception {
        NeInfo neInfo = neInfoConfig.getNeInfo("COHERENT", "CHASSIS2.0", "OD");

        assertEquals("MUX_C64", neInfo.getDefaultCardByCardClass("MUX", WDM_Band.C, 75)
                .getCardType());
        assertEquals("MUX_32", neInfo.getDefaultCardByCardClass("MUX", WDM_Band.C, 150)
                .getCardType());
    }

    @Test
    void byteDance20OscTransceiverTemplatesDoNotRepeatPortNames() throws Exception {
        NeInfo neInfo = neInfoConfig.getNeInfo("COHERENT", "CHASSIS2.0", "OD");

        assertEquals("TRANSCEIVER?OSC", neInfo.getPort("IRA_32").get("SIG").getTransceiver().getName());
        assertEquals("TRANSCEIVER?OSC", neInfo.getPort("IRA_32").get("LINE").getTransceiver().getName());
        assertEquals("TRANSCEIVER?OSC", neInfo.getPort("TILA").get("LINE_EAST").getTransceiver().getName());
        assertEquals("TRANSCEIVER?OSC", neInfo.getPort("TILA").get("LINE_WEST").getTransceiver().getName());
        assertEquals("TRANSCEIVER?OSC", neInfo.getPort("DGE").get("LINE_EAST").getTransceiver().getName());
        assertEquals("TRANSCEIVER?OSC", neInfo.getPort("DGE").get("LINE_WEST").getTransceiver().getName());

        assertEquals("TRANSCEIVER-1-1-LINEOSC", NameGenerator.getTransceiverName(
                neInfo.getPort("IRA_32").get("LINE").getTransceiver().getName(), "LINE", 1, 1));
        assertEquals("TRANSCEIVER-1-1-LINE_EASTOSC", NameGenerator.getTransceiverName(
                neInfo.getPort("TILA").get("LINE_EAST").getTransceiver().getName(), "LINE_EAST", 1, 1));
        assertEquals("TRANSCEIVER-1-1-LINE_WESTOSC", NameGenerator.getTransceiverName(
                neInfo.getPort("DGE").get("LINE_WEST").getTransceiver().getName(), "LINE_WEST", 1, 1));
    }

    @Test
    void supportedFrequencyDefaultsToLegacyResourceModel() throws Exception {
        GetSupportedFrequencyOutput output = neInfoConfig.getSupportedFrequency(
                new GetSupportedFrequencyInputBuilder()
                        .setVendorName("COHERENT")
                        .setProductType("CHASSIS")
                        .build());

        assertTrue(output.getFrequency().contains("75GHz"));
        assertFalse(output.getFrequency().contains("150GHz"));
    }

    @Test
    void supportedFrequencyUsesBone20ProductType() throws Exception {
        GetSupportedFrequencyOutput output = neInfoConfig.getSupportedFrequency(
                new GetSupportedFrequencyInputBuilder()
                        .setVendorName("COHERENT")
                        .setProductType("CHASSIS2.0")
                        .build());

        assertTrue(output.getFrequency().contains("75GHz"));
        assertTrue(output.getFrequency().contains("150GHz"));
    }

    @Test
    void vendorListExposesBone20ProductFromResourceFileOnce() {
        GetVendorListOutput output = neInfoConfig.getVendorList(
                new GetVendorListInputBuilder().setNodeType(NodeType.OD).build());

        List<Vendor> vendors = output.getVendor();
        assertTrue(vendors.stream().anyMatch(vendor -> "COHERENT".equals(vendor.getVendorName())
                && "CHASSIS".equals(vendor.getProductType())));
        assertEquals(1, vendors.stream().filter(vendor -> "COHERENT".equals(vendor.getVendorName())
                && "CHASSIS2.0".equals(vendor.getProductType())).count());
    }

    @Test
    void vendorListDoesNotExposeBone20ProductWhenResourceIsUnavailable() throws Exception {
        Field field = NEInfoConfig.class.getDeclaredField("neInfoMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, NeInfo> neInfoMap = (Map<String, NeInfo>) field.get(neInfoConfig);
        neInfoMap.remove("COHERENT-CHASSIS2.0-BYTEDANCE-OD");

        GetVendorListOutput output = neInfoConfig.getVendorList(
                new GetVendorListInputBuilder().setNodeType(NodeType.OD).build());

        assertFalse(output.getVendor().stream().anyMatch(vendor -> "COHERENT".equals(vendor.getVendorName())
                && "CHASSIS2.0".equals(vendor.getProductType())));
    }

    @Test
    void l3x8c7FriendlyNamesUseTelemetryPortNumbersForEveryServiceType() throws Exception {
        NeInfo neInfo = neInfoConfig.getNeInfo("COHERENT", "CHASSIS", "TD");
        Card card = neInfo.getCardByCardType("RL3X8C7_FLEX");
        JsonYangConverter converter = new JsonYangConverter();

        for (CrossConnection crossConnection : card.getCrossConnections()) {
            SERVICETYPE serviceType = converter.getServiceType(crossConnection.getServiceType());
            if (serviceType.name().startsWith("REG")) {
                assertTelemetryAligned(
                        NeInfoUtil.getLPortNameMappingByServiceType(card, serviceType), "L");
                continue;
            }
            assertTelemetryAligned(
                    NeInfoUtil.getCPortNameMappingByServiceType(card, serviceType), "C");
            assertTelemetryAligned(
                    NeInfoUtil.getLPortNameMappingByServiceType(card, serviceType), "L");
        }
    }

    private void assertTelemetryAligned(Map<String, String> namesByPhysicalPort, String role) {
        assertFalse(namesByPhysicalPort.isEmpty());
        namesByPhysicalPort.forEach((physicalPort, friendlyName) ->
                assertEquals(role + physicalPort, friendlyName));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = NEInfoConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(neInfoConfig, value);
    }
}
