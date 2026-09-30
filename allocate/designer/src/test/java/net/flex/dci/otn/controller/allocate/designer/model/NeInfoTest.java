/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import net.flex.dci.otn.controller.allocate.ne.Ne;
import net.flex.dci.otn.controller.allocate.ne.Port;
import org.junit.jupiter.api.Test;

class NeInfoTest {

    @Test
    void getSlotByCentralFrequency() throws NeDesignerException {
//
//        assertEquals("/frequency=196025000,196075000",NeInfo.getSlotByCentralFrequency("196050000,191300000,50000","m1d1"));
//        assertEquals("/frequency=195975000,196025000",NeInfo.getSlotByCentralFrequency("196050000,191300000,50000","m2d2"));
//        assertEquals("/frequency=195925000,195975000",NeInfo.getSlotByCentralFrequency("196050000,191300000,50000","m3d3"));
//        assertEquals("/frequency=196025,196075",NeInfo.getSlotByCentralFrequency("196050,191300,50","m1d1"));
    }

    @Test
    void bone20OdCardHasConsistentIraAndMuxPanelLinks() throws Exception {
        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        InputStream input = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(input, "Missing test resource: " + resource);

        NeInfo neInfo;
        try (InputStream json = input) {
            Ne ne = new ObjectMapper().readValue(json, Ne.class);
            neInfo = new NeInfo(ne, resource);
        }

        assertTrue(neInfo.getPort("IRA_32").containsKey("SIG"));
        assertTrue(neInfo.getPort("IRA_32").containsKey("LINE"));

        Port fmuxMonitorPort = neInfo.getPort("FMUX_32").get("EXP33");
        assertNotNull(fmuxMonitorPort);
        assertEquals("WSS-Sig", fmuxMonitorPort.getPortType());
        assertEquals("9", fmuxMonitorPort.getIndex());
        assertEquals("OA-Sig", neInfo.getPort("FMUX_32").get("SIG").getPortType());
        assertEquals("OP-A", neInfo.getPort("FMUX_32").get("SIGA").getPortType());
        assertEquals("OP-B", neInfo.getPort("FMUX_32").get("SIGB").getPortType());
        assertEquals("OP-SIG1", neInfo.getPort("FMUX_32").get("COM1").getPortType());
        assertEquals("OP-SIG2", neInfo.getPort("FMUX_32").get("COM2").getPortType());

        Map<String, java.util.List<ExternalLinkTo>> iraToPanel =
                neInfo.getExternalLinkInfoFromTo("IRA_32", "MUXPANEL_64");
        assertTrue(iraToPanel.containsKey("MPO?,1,3,1"));
        assertTrue(iraToPanel.containsKey("EXP?,1,8,1"));

        Map<String, java.util.List<ExternalLinkTo>> panelToIra =
                neInfo.getExternalLinkInfoFromTo("MUXPANEL_64", "IRA_32");
        assertTrue(panelToIra.containsKey("MPO?,1,3,1"));
        assertTrue(panelToIra.containsKey("MPO4"));

        assertNotNull(neInfo.getExternalLinkInfoFromTo("OLP3_3", "MUX_C64"));
        assertNotNull(neInfo.getExternalLinkInfoFromTo("PANEL", "MUX_C64"));
        assertNotNull(neInfo.getExternalLinkInfoFromTo("PANEL", "MUX_32"));

        Card fmux32 = neInfo.getCardByCardVendor("FMUX_32");
        CrossConnection mpoSig = fmux32.getCrossConnections().stream()
                .filter(xc -> "MPO?,1,4,1".equals(xc.getFrom().getPort()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing FMUX32 MPO-SIG XC"));
        assertEquals("SIG", mpoSig.getTo().getPort());
        assertEquals("OCH", mpoSig.getType());
        assertTrue(mpoSig.getInitiated());
        assertTrue(mpoSig.getIsFixed());
        assertTrue(mpoSig.getBiDirection());
        assertEquals(Integer.valueOf(1), mpoSig.getMultiple());

        CrossConnection aps = fmux32.getCrossConnections().stream()
                .filter(xc -> xc.getAps() != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing existing FMUX32 APS XC"));
        assertEquals("APS", aps.getType());
        assertEquals("SIGA,SIGB", aps.getFrom().getPort());
        assertEquals("COM1,COM2", aps.getTo().getPort());
        assertTrue(aps.getInitiated());
    }

    @Test
    void bone20Flex64ResourceDefinesBothFmuxMpoGroupsAndCascade() throws Exception {
        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        NeInfo neInfo;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "Missing test resource: " + resource);
            Ne ne = new ObjectMapper().readValue(input, Ne.class);
            neInfo = new NeInfo(ne, resource);
        }

        Map<String, List<ExternalLinkTo>> panelToFmux =
                neInfo.getExternalLinkInfoFromTo("MUXPANEL_64", "FMUX_32");
        assertExternalTarget(panelToFmux, "MPO?,1,4,1", "FMUX_32", "MPO?,1,4,1");
        assertExternalTarget(panelToFmux, "MPO?,5,8,1", "FMUX_32", "MPO?,1,4,1");

        Map<String, List<ExternalLinkTo>> fmuxToFmux =
                neInfo.getExternalLinkInfoFromTo("FMUX_32", "FMUX_32");
        assertExternalTarget(fmuxToFmux, "SIG", "FMUX_32", "COM1");

        Map<String, List<ExternalLinkTo>> fmuxToTila =
                neInfo.getExternalLinkInfoFromTo("FMUX_32", "TILA");
        assertExternalTarget(fmuxToTila, "SIG", "TILA", "LINE_WEST");
    }

    @Test
    void bone20FixedOtmUsesOneInterSiteTilaLinkAndLayeredAmplifierXcs() throws Exception {
        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        NeInfo neInfo;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "Missing test resource: " + resource);
            Ne ne = new ObjectMapper().readValue(input, Ne.class);
            neInfo = new NeInfo(ne, resource);
        }

        Map<String, List<ExternalLinkTo>> tilaToTila =
                neInfo.getExternalLinkInfoFromTo("TILA", "TILA");
        assertEquals(1, tilaToTila.size());
        assertEquals("LINE_WEST", tilaToTila.get("LINE_EAST").get(0).getPort());

        Card tila = neInfo.getCardByCardVendor("TILA");
        assertEquals(2, tila.getCrossConnections().size());
        tila.getCrossConnections().forEach(xc -> {
            assertEquals("C", xc.getFrom().getLayer());
            assertEquals("C", xc.getTo().getLayer());
        });
    }

    @Test
    void bone20DgeUsesOneInterSiteLinkAndAllAmplifierXcsHaveLayers() throws Exception {
        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        NeInfo neInfo;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "Missing test resource: " + resource);
            Ne ne = new ObjectMapper().readValue(input, Ne.class);
            neInfo = new NeInfo(ne, resource);
        }

        assertSingleEastToWestLink(neInfo, "DGE", "DGE");
        assertSingleEastToWestLink(neInfo, "DGE", "TILA");
        assertSingleEastToWestLink(neInfo, "TILA", "DGE");

        Card dge = neInfo.getCardByCardVendor("DGE");
        dge.getCrossConnections().stream()
                .filter(xc -> xc.getAmplifier() != null)
                .forEach(xc -> {
                    assertEquals("C", xc.getFrom().getLayer());
                    assertEquals("C", xc.getTo().getLayer());
                });

        Port iraAsePort = neInfo.getPort("IRA_32").get("EXP33");
        assertNotNull(iraAsePort);
        assertEquals("WSS-Sig", iraAsePort.getPortType());
        assertEquals("13", iraAsePort.getIndex());
    }

    private void assertSingleEastToWestLink(NeInfo neInfo, String sourceCard,
            String destinationCard) throws NeDesignerException {
        Map<String, List<ExternalLinkTo>> links =
                neInfo.getExternalLinkInfoFromTo(sourceCard, destinationCard);
        assertEquals(1, links.size());
        assertEquals("LINE_WEST", links.get("LINE_EAST").get(0).getPort());
    }

    private void assertExternalTarget(Map<String, List<ExternalLinkTo>> links, String sourcePort,
            String destinationCard, String destinationPort) {
        assertTrue(links.containsKey(sourcePort), "Missing source port " + sourcePort);
        assertTrue(links.get(sourcePort).stream()
                .anyMatch(target -> destinationCard.equals(target.getCardType())
                        && destinationPort.equals(target.getPort())),
                "Missing target " + destinationCard + " " + destinationPort);
    }

    @Test
    void coherentModelContainsRamanCl16() throws Exception {
        String resource = "COHERENT-CHASSIS-ByteDance-OD-card.json";
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            NeInfo neInfo = new NeInfo(mapper.readValue(input, Ne.class), resource);
            Card raman = neInfo.getCardByCardVendor("RAMANCL_16");

            assertEquals("RAMAN", neInfo.getCardClassByCardVendor("RAMANCL_16"));
            assertEquals("RAMAN_CL", raman.getCardType());
            assertEquals(Arrays.asList("LINE", "SIG"),
                    Arrays.asList(raman.getPorts().get(0).getName(), raman.getPorts().get(1).getName()));

            CrossConnection xc = raman.getCrossConnections().get(0);
            assertEquals("SIG", xc.getFrom().getPort());
            assertEquals("LINE", xc.getTo().getPort());
            assertEquals("BACKWARD_RAMAN", xc.getAmplifier().getParams().stream()
                    .filter(param -> "amplifier-type".equals(param.getName()))
                    .findFirst().orElseThrow(AssertionError::new).getValue());
            assertEquals("17.00", xc.getAmplifier().getParams().stream()
                    .filter(param -> "target-gain-range-higher".equals(param.getName()))
                    .findFirst().orElseThrow(AssertionError::new).getValue());

            Map<String, List<ExternalLinkTo>> iraToRaman =
                    neInfo.getExternalLinkInfoFromTo("IRA_CL", "RAMAN_CL");
            assertEquals("SIG", iraToRaman.get("LINE").get(0).getPort());

            Map<String, List<ExternalLinkTo>> ramanToIra =
                    neInfo.getExternalLinkInfoFromTo("RAMAN_CL", "IRA_CL");
            assertEquals("LINE", ramanToIra.get("SIG").get(0).getPort());
        }
    }
}
