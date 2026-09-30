package net.flex.dci.otn.controller.allocate.designer.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

class SiteResourceLayoutFactoryTest {

    @Test
    void flexOneToOneKeepsBothTilaCardsOnTheMainNode() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(32,
                ProtectionBidir1To1.class));

        assertTrue(layout instanceof ByteDance2FlexOneToOneResourceLayout);
        assertFalse(layout.requiresDedicatedProtectionNode());
        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "ILA"),
                layout.resolveMainCardClasses(Arrays.asList("legacy")));
        assertTrue(layout.getProtectionPeerCardClasses().isEmpty());
    }

    @Test
    void flex32SelfLinkSelectsCom2() {
        Map<String, List<ExternalLinkTo>> relations = new LinkedHashMap<>();
        relations.put("SIG", Arrays.asList(
                target("FMUX_32", "COM1"), target("FMUX_32", "COM2")));

        Map<String, List<ExternalLinkTo>> selected =
                new ByteDance2FlexOneToOneResourceLayout()
                        .selectCardSelfLinks(relations, "FMUX_32");

        assertEquals(1, selected.get("SIG").size());
        assertEquals("COM2", selected.get("SIG").get(0).getPort());
    }

    @Test
    void selectsBone20OneToTwoLayoutOnlyForExactScenario() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(32,
                ProtectionBidir1To2.class));

        assertTrue(layout instanceof ByteDance2OneToTwoResourceLayout);
        assertTrue(layout.requiresDedicatedProtectionNode());
        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "OP", "ILA"),
                layout.resolveMainCardClasses(Arrays.asList("legacy")));
        assertEquals(Integer.valueOf(1), layout.getMainNodeSlot("OLP3_3", 0));
        assertEquals(Integer.valueOf(3), layout.getMainNodeSlot("FMUX_32", 0));
        assertEquals(Integer.valueOf(5), layout.getMainNodeSlot("TILA", 0));
        assertEquals("1B", layout.getOlpLinePort(RoutingType.Slave));
        assertEquals("1C", layout.getOlpLinePort(RoutingType.Third));
    }

    @Test
    void bandwidth64AddsSecondFmuxAtSlotSeven() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(64,
                ProtectionBidir1To2.class));

        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "CMUX", "OP", "ILA"),
                layout.resolveMainCardClasses(Arrays.asList("legacy")));
        assertEquals(Integer.valueOf(7), layout.getMainNodeSlot("FMUX_32", 1));
        assertEquals(Integer.valueOf(1), layout.getProtectionNodeSlot(RoutingType.Slave));
        assertEquals(Integer.valueOf(3), layout.getProtectionNodeSlot(RoutingType.Third));
        assertTrue(layout.usesDualFmux64Topology());
        assertTrue(layout.routesDualFmux64ThroughOlp());
        assertTrue(layout.useFmux32Com1ProtectionXc("FMUX_32"));
        assertTrue(layout.hasCardSelfLinks("FMUX_32"));

        Map<String, List<ExternalLinkTo>> relations = new LinkedHashMap<>();
        relations.put("SIG", Arrays.asList(
                target("FMUX_32", "COM1"), target("FMUX_32", "COM2")));
        Map<String, List<ExternalLinkTo>> selected =
                layout.selectCardSelfLinks(relations, "FMUX_32");
        assertEquals(1, selected.get("SIG").size());
        assertEquals("COM2", selected.get("SIG").get(0).getPort());
    }

    @Test
    void bandwidth32KeepsSingleFmuxOneToTwoLayout() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(32,
                ProtectionBidir1To2.class));

        assertFalse(layout.usesDualFmux64Topology());
        assertFalse(layout.routesDualFmux64ThroughOlp());
        assertFalse(layout.useFmux32Com1ProtectionXc("FMUX_32"));
        assertFalse(layout.hasCardSelfLinks("FMUX_32"));
    }

    @Test
    void oneToTwoMainTilaUsesWestForInternalOlpOnBothEndpointDirections() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(32,
                ProtectionBidir1To2.class));

        Map<String, List<ExternalLinkTo>> olpToTila = new LinkedHashMap<>();
        olpToTila.put("1A", Arrays.asList(
                target("TILA", "LINE_EAST"), target("TILA", "LINE_WEST")));
        Map<String, List<ExternalLinkTo>> selectedAToZ = layout.selectOlpTilaLinks(
                olpToTila, "OLP3_3", "TILA");

        assertEquals(1, selectedAToZ.size());
        assertEquals(1, selectedAToZ.get("1A").size());
        assertEquals("LINE_WEST", selectedAToZ.get("1A").get(0).getPort());

        Map<String, List<ExternalLinkTo>> tilaToOlp = new LinkedHashMap<>();
        tilaToOlp.put("LINE_EAST", Collections.singletonList(target("OLP3_3", "1A")));
        tilaToOlp.put("LINE_WEST", Collections.singletonList(target("OLP3_3", "1A")));
        Map<String, List<ExternalLinkTo>> selectedZToA = layout.selectOlpTilaLinks(
                tilaToOlp, "TILA", "OLP3_3");

        assertEquals(1, selectedZToA.size());
        assertTrue(selectedZToA.containsKey("LINE_WEST"));
        assertEquals("1A", selectedZToA.get("LINE_WEST").get(0).getPort());
    }

    @Test
    void bone20ZEndTilaUsesEastExternalOutputWithoutChangingJsonRelation() {
        ExternalLinkTo westTarget = new ExternalLinkTo();
        westTarget.setNeType("OD");
        westTarget.setCardType("TILA");
        westTarget.setPort("LINE_WEST");
        Map<String, List<ExternalLinkTo>> jsonRelation = new LinkedHashMap<>();
        jsonRelation.put("LINE_EAST", Collections.singletonList(westTarget));

        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(32,
                ProtectionUnprotected.class));
        Map<String, List<ExternalLinkTo>> selected =
                layout.selectZEndExternalLinks(jsonRelation, "TILA");

        assertTrue(layout instanceof ByteDance2DefaultResourceLayout);
        assertEquals("LINE_EAST", selected.get("LINE_EAST").get(0).getPort());
        assertEquals("LINE_WEST", jsonRelation.get("LINE_EAST").get(0).getPort());
        assertEquals("OTA_WEST", layout.resolveTilaClassMode("TILA", true, null));
        assertEquals("ILA", new DefaultSiteResourceLayout(layout.supportsTilaClassMode())
                .resolveTilaClassMode("TILA", true, null));
    }

    @Test
    void mainRouteAppliesZEndOrientationOnlyToItsFinalNode() {
        assertTrue(SiteRepo.isMainRouteZEnd(RoutingType.Main, 3, 4));
        assertFalse(SiteRepo.isMainRouteZEnd(RoutingType.Main, 2, 4));
        assertFalse(SiteRepo.isMainRouteZEnd(RoutingType.Slave, 3, 4));
    }

    @Test
    void nonBone20LayoutKeepsOriginalZEndPort() {
        ExternalLinkTo westTarget = new ExternalLinkTo();
        westTarget.setCardType("TILA");
        westTarget.setPort("LINE_WEST");
        Map<String, List<ExternalLinkTo>> relation = new LinkedHashMap<>();
        relation.put("LINE_EAST", Collections.singletonList(westTarget));

        Map<String, List<ExternalLinkTo>> selected = new DefaultSiteResourceLayout()
                .selectZEndExternalLinks(relation, "TILA");

        assertEquals("LINE_WEST", selected.get("LINE_EAST").get(0).getPort());
    }

    @Test
    void unprotectedBone20Flex32UsesHardwareSlotsAndKeepsEndpointOrientation() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(32,
                ProtectionUnprotected.class));

        assertFalse(layout.requiresDedicatedProtectionNode());
        assertTrue(layout instanceof ByteDance2DefaultResourceLayout);
        assertEquals(Arrays.asList("legacy"),
                layout.resolveMainCardClasses(Arrays.asList("legacy")));
        assertEquals(Integer.valueOf(1), layout.getMainNodeSlot("TILA", 0));
        assertEquals(Integer.valueOf(3), layout.getMainNodeSlot("FMUX_32", 0));
        assertEquals(null, layout.getMainNodeSlot("TILA", 1));
        assertEquals(null, layout.getMainNodeSlot("FMUX_32", 1));

        Map<String, List<ExternalLinkTo>> relations = new LinkedHashMap<>();
        relations.put("SIG", Arrays.asList(
                target("TILA", "LINE_EAST"), target("TILA", "LINE_WEST")));
        Map<String, List<ExternalLinkTo>> selected = layout.selectFmuxTilaLinks(
                relations, "FMUX_32", "TILA", "SIG");
        assertEquals(1, selected.get("SIG").size());
        assertEquals("LINE_WEST", selected.get("SIG").get(0).getPort());
    }

    @Test
    void flex32SlotLayoutDoesNotApplyOutsideExactScenario() {
        SiteResourceLayout fixedGrid = SiteResourceLayoutFactory.create(input(32,
                ProtectionUnprotected.class, 75));
        SiteResourceLayout protectedFlex = SiteResourceLayoutFactory.create(input(32,
                ProtectionBidir1To1.class));
        SiteInput nonBone20Input = input(32, ProtectionUnprotected.class);
        nonBone20Input.setVendorName("OTHER");
        nonBone20Input.setVendorType("OTHER");
        SiteResourceLayout nonBone20 = SiteResourceLayoutFactory.create(nonBone20Input);
        SiteInput lBandInput = input(32, ProtectionUnprotected.class);
        lBandInput.setWdmBand(WDM_Band.L);
        SiteResourceLayout lBand = SiteResourceLayoutFactory.create(lBandInput);

        assertEquals(ByteDance2DefaultResourceLayout.class, fixedGrid.getClass());
        assertEquals(null, fixedGrid.getMainNodeSlot("TILA", 0));
        assertTrue(protectedFlex instanceof ByteDance2FlexOneToOneResourceLayout);
        assertEquals(Integer.valueOf(1), protectedFlex.getMainNodeSlot("TILA", 0));
        assertEquals(DefaultSiteResourceLayout.class, nonBone20.getClass());
        assertEquals(null, nonBone20.getMainNodeSlot("TILA", 0));
        assertEquals(ByteDance2DefaultResourceLayout.class, lBand.getClass());
        assertEquals(null, lBand.getMainNodeSlot("TILA", 0));
    }

    @Test
    void unprotectedBone20Flex64UsesDualFmuxEndpointLayout() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(64,
                ProtectionUnprotected.class));

        assertTrue(layout instanceof ByteDance2Flex64ResourceLayout);
        assertTrue(layout.usesDualFmux64Topology());
        assertFalse(layout.routesDualFmux64ThroughOlp());
        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "CMUX", "ILA"),
                layout.resolveMainCardClasses(Arrays.asList("legacy")));
        assertEquals(Integer.valueOf(3), layout.getMainNodeSlot("FMUX_32", 0));
        assertEquals(Integer.valueOf(7), layout.getMainNodeSlot("FMUX_32", 1));
        assertEquals(Integer.valueOf(1), layout.getMainNodeSlot("TILA", 0));
        assertEquals("SIGA", layout.getFmuxMainLinePort(false));
        assertEquals("OTA_WEST", layout.resolveTilaClassMode("TILA", true, null));
    }

    @Test
    void protectedBone20Flex64KeepsDualFmuxEndpointLayout() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(64,
                ProtectionBidir1To1.class));

        assertTrue(layout instanceof ByteDance2Flex64ResourceLayout);
        assertTrue(layout.usesDualFmux64Topology());
        assertFalse(layout.routesDualFmux64ThroughOlp());
        assertEquals(Arrays.asList("PANEL", "MUXPANEL", "CMUX", "CMUX", "ILA"),
                layout.resolveMainCardClasses(Arrays.asList("legacy")));
        assertEquals(Integer.valueOf(3), layout.getMainNodeSlot("FMUX_32", 0));
        assertEquals(Integer.valueOf(7), layout.getMainNodeSlot("FMUX_32", 1));
        assertEquals(Integer.valueOf(1), layout.getMainNodeSlot("TILA", 0));
        assertEquals("SIGA", layout.getFmuxMainLinePort(true));
        assertTrue(layout.useFmux32Com1ProtectionXc("FMUX_32"));
        assertTrue(layout.hasCardSelfLinks("FMUX_32"));

        Map<String, List<ExternalLinkTo>> relations = new LinkedHashMap<>();
        relations.put("SIG", Arrays.asList(
                target("FMUX_32", "COM1"), target("FMUX_32", "COM2")));
        Map<String, List<ExternalLinkTo>> selected =
                layout.selectCardSelfLinks(relations, "FMUX_32");
        assertEquals(1, selected.get("SIG").size());
        assertEquals("COM2", selected.get("SIG").get(0).getPort());
    }

    @Test
    void fixedGridOneToTwoUsesDedicatedMuxOlpTilaLayout() {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(64,
                ProtectionBidir1To2.class, 75));
        SiteResourceLayout grid150Layout = SiteResourceLayoutFactory.create(input(32,
                ProtectionBidir1To2.class, 150));

        assertTrue(layout instanceof ByteDance2FixedOneToTwoResourceLayout);
        assertTrue(grid150Layout instanceof ByteDance2FixedOneToTwoResourceLayout);
        assertTrue(layout.requiresDedicatedProtectionNode());
        assertEquals(Arrays.asList("PANEL", "MUX", "OP", "ILA"),
                layout.resolveMainCardClasses(Arrays.asList("legacy")));
        assertEquals(Arrays.asList("PANEL", "ILA"), layout.getProtectionPeerCardClasses());
        assertEquals(Integer.valueOf(1), layout.getMainNodeSlot("OLP3_3", 0));
        assertEquals(Integer.valueOf(3), layout.getMainNodeSlot("TILA", 0));
        assertEquals("1B", layout.getOlpLinePort(RoutingType.Slave));
        assertEquals("1C", layout.getOlpLinePort(RoutingType.Third));
    }

    private ExternalLinkTo target(String cardType, String port) {
        ExternalLinkTo target = new ExternalLinkTo();
        target.setCardType(cardType);
        target.setPort(port);
        return target;
    }

    private SiteInput input(int bandwidth,
            Class<? extends org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType> protectionType) {
        return input(bandwidth, protectionType, 0);
    }

    private SiteInput input(int bandwidth,
            Class<? extends org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType> protectionType,
            int grid) {
        return SiteInput.builder()
                .nodesMap(java.util.Collections.emptyMap())
                .vendorName("COHERENT")
                .vendorType("CHASSIS2.0")
                .bandwidth(bandwidth)
                .isProtected(!ProtectionUnprotected.class.equals(protectionType))
                .grid(grid)
                .plane("p")
                .planeId("p")
                .riskGroupName("r")
                .linkModel(ProtectionBidir1To2.class.equals(protectionType) ? "10" : "6")
                .wdmBand(WDM_Band.C)
                .protectionType(protectionType)
                .build();
    }
}
