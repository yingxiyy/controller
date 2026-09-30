package net.flex.dci.otn.controller.allocate.link.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;

class SiteLinkCreatorExternalTest {

    @Test
    void selectsChassis20FrequencyModelOnlyForBone20Product() {
        assertEquals(NeYangModel.Chassis20, SiteLinkCreator.resolveSiteLinkYangModel(
                NeYangModel.ByteDance, "COHERENT", "CHASSIS2.0"));
        assertEquals(NeYangModel.ByteDance, SiteLinkCreator.resolveSiteLinkYangModel(
                NeYangModel.ByteDance, "COHERENT", "CHASSIS"));
        assertEquals(NeYangModel.ByteDance, SiteLinkCreator.resolveSiteLinkYangModel(
                NeYangModel.ByteDance, "OTHER", "CHASSIS2.0"));
    }

    @Test
    void classifiesAllBone20Flex64ProtectionModesForMuxPanelEndpoints() {
        assertTrue(SiteLinkCreator.isByteDance2Flex64("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 64, ProtectionUnprotected.class));
        assertTrue(SiteLinkCreator.isByteDance2Flex64("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 64, ProtectionBidir1To1.class));
        assertTrue(SiteLinkCreator.isByteDance2Flex64("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 64, ProtectionBidir1To2.class));
        assertFalse(SiteLinkCreator.isByteDance2Flex64("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 32, ProtectionBidir1To1.class));
        assertFalse(SiteLinkCreator.isByteDance2Flex64("COHERENT", "CHASSIS",
                0, WDM_Band.C, 64, ProtectionBidir1To1.class));
    }

    @Test
    void locatesReversedEndpointsBySiteForAllProtectedFlexModes() {
        assertTrue(SiteLinkCreator.isByteDance2FlexProtected("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 32, ProtectionBidir1To1.class));
        assertTrue(SiteLinkCreator.isByteDance2FlexProtected("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 64, ProtectionBidir1To1.class));
        assertTrue(SiteLinkCreator.isByteDance2FlexProtected("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 32, ProtectionBidir1To2.class));
        assertFalse(SiteLinkCreator.isByteDance2FlexProtected("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 64, ProtectionUnprotected.class));
        assertTrue(SiteLinkCreator.isByteDance2FlexProtected("COHERENT", "CHASSIS2.0",
                0, WDM_Band.C, 64, ProtectionBidir1To2.class));
    }

    @Test
    void readsMuxPanelMpoNumberFromEitherLinkDirection() {
        assertEquals(5, SiteLinkCreator.getMuxPanelMpoNumber(
                "OmsLink-Site-A#Ne-A#MUX-1-50#PORT-1-50-MPO5-"
                        + "Site-A#Ne-A#LINECARD-1-7#PORT-1-7-MPO1"));
        assertEquals(8, SiteLinkCreator.getMuxPanelMpoNumber(
                "OmsLink-Site-Z#Ne-Z#LINECARD-1-7#PORT-1-7-MPO4-"
                        + "Site-Z#Ne-Z#MUX-1-50#PORT-1-50-MPO8"));
    }

    @Test
    void resolvesExternalEndpointBySiteWhenProtectedRouteNodesAreReversed() {
        Node routeStart = new NodeBuilder().setNodeId(new NodeId("Site-Z#Ne-Z")).build();
        Node routeEnd = new NodeBuilder().setNodeId(new NodeId("Site-A#Ne-A")).build();

        assertEquals(routeEnd, SiteLinkCreator.findEndpointNode(
                Arrays.asList(routeStart, routeEnd), "Site-A"));
        assertEquals(routeStart, SiteLinkCreator.findEndpointNode(
                Arrays.asList(routeStart, routeEnd), "Site-Z"));
    }
}
