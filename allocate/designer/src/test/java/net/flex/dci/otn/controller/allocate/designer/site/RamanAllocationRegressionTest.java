package net.flex.dci.otn.controller.allocate.designer.site;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.SiteModelConfig;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteNodeInput;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.Ne;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

/** Real equipment/TP/XC/link generation; only the site-model card-class lookup is stubbed. */
class RamanAllocationRegressionTest {
    private NeInfo model() throws Exception {
        String resource = "COHERENT-CHASSIS-ByteDance-OD-card.json";
        return new NeInfo(new ObjectMapper().readValue(getClass().getClassLoader()
                .getResourceAsStream(resource), Ne.class), resource);
    }

    @Test
    void pairedEndpointsUseOnlySigInsideAndLineBetweenSites() throws Exception {
        Route route = allocate(true, "IRA", "IRA");
        assertEquals(new HashSet<>(Arrays.asList(
                "A:1:LINE->A:5:SIG", "A:5:LINE->B:5:LINE", "B:5:SIG->B:1:LINE")), links(route));
    }

    @Test
    void ilaUsesWestAndEastRamanWithoutReusingSignalPorts() throws Exception {
        assertTransit("ILA");
    }

    @Test
    void dgeUsesWestAndEastRamanWithoutReusingSignalPorts() throws Exception {
        assertTransit("DGE");
    }

    @Test
    void singleDirectionTransitRamanUsesSlotFiveOnItsSelectedSide() throws Exception {
        for (String cardClass : Arrays.asList("ILA", "DGE")) {
            Set<String> west = links(allocate(new boolean[] {true, false}, "IRA", cardClass, "IRA"));
            assertTrue(west.contains("B:5:SIG->B:1:LINE_WEST"));
            assertTrue(west.contains("A:5:LINE->B:5:LINE"));
            assertFalse(west.stream().anyMatch(link -> link.contains("B:6:")));
            Set<String> east = links(allocate(new boolean[] {false, true}, "IRA", cardClass, "IRA"));
            assertTrue(east.contains("B:1:LINE_EAST->B:5:SIG"));
            assertTrue(east.contains("B:5:LINE->C:5:LINE"));
            assertFalse(east.stream().anyMatch(link -> link.contains("B:6:")));
        }
    }

    private void assertTransit(String cardClass) throws Exception {
        Route route = allocate(true, "IRA", cardClass, "IRA");
        assertEquals(new HashSet<>(Arrays.asList("A:1:LINE->A:5:SIG",
                "A:5:LINE->B:5:LINE", "B:5:SIG->B:1:LINE_WEST",
                "B:1:LINE_EAST->B:6:SIG", "B:6:LINE->C:5:LINE",
                "C:5:SIG->C:1:LINE")), links(route));
    }

    @Test
    void ramanXcIdsReferencePhysicalPortsWithoutNullLayer() throws Exception {
        Route route = allocate(true, "IRA", "IRA");
        assertEquals(2, route.getXcs().stream().filter(xc -> xc.getDescription().endsWith("-RAMAN")).count());
        route.getXcs().stream().filter(xc -> xc.getDescription().endsWith("-RAMAN")).forEach(xc -> {
            assertFalse(xc.getCrossConnectionId().getValue().contains("/null"));
            assertTrue(xc.getSourceTp().get(0).getTpRef().getValue().endsWith("-SIG"));
            assertTrue(xc.getDestinationTp().get(0).getTpRef().getValue().endsWith("-LINE"));
        });
    }

    @Test
    void ramanStartsDisabledWithGainInsideItsDeclaredRange() throws Exception {
        Route route = allocate(true, "IRA", "IRA");
        route.getXcs().stream().filter(xc -> xc.getDescription().endsWith("-RAMAN")).forEach(xc -> {
            assertEquals(0, BigDecimal.TEN.compareTo(xc.getAmplifier().getTargetGain()));
            assertFalse(xc.getAmplifier().isEnable());
            assertTrue(xc.getAmplifier().isAutoPowerReduction());
        });
    }

    @Test
    void rejectsRamanGainOutsideModelBounds() throws Exception {
        XCRepo repo = new XCRepo();
        inject(repo, "jsonYangConverter", new JsonYangConverter());
        CrossConnection xc = model().getCardByCardVendor("RAMANCL_16").getCrossConnections().get(0);
        for (double gain : new double[] {9, 19}) {
            xc.getAmplifier().setTargetGain(gain);
            assertThrows(NeDesignerException.class, () -> repo.getAmplifier(xc));
        }
        for (double gain : new double[] {10, 17}) {
            xc.getAmplifier().setTargetGain(gain);
            assertDoesNotThrow(() -> repo.getAmplifier(xc));
        }
    }

    @Test
    void unselectedRamanKeepsLegacyDirectLinkAndLayeredEdfaIds() throws Exception {
        Route route = allocate(false, "IRA", "IRA");
        assertEquals(Collections.singleton("A:1:LINE->B:1:LINE"), links(route));
        assertFalse(route.getXcs().stream().anyMatch(xc -> xc.getDescription().endsWith("-RAMAN")));
        assertTrue(route.getXcs().stream().anyMatch(xc -> xc.getAmplifier() != null));
        assertTrue(route.getXcs().stream().filter(xc -> xc.getAmplifier() != null)
                .allMatch(xc -> xc.getCrossConnectionId().getValue().contains("/C")
                        || xc.getCrossConnectionId().getValue().contains("/L")));
    }

    private Route allocate(boolean raman, String... cardClasses) throws Exception {
        boolean[] spans = new boolean[cardClasses.length - 1];
        Arrays.fill(spans, raman);
        return allocate(spans, cardClasses);
    }

    private Route allocate(boolean[] spans, String... cardClasses) throws Exception {
        JsonYangConverter converter = new JsonYangConverter();
        EquipmentRepo equipment = new EquipmentRepo();
        inject(equipment, "jsonYangConverter", converter);
        TpRepo tps = new TpRepo();
        inject(tps, "jsonYangConverter", converter);
        XCRepo xcs = new XCRepo();
        inject(xcs, "jsonYangConverter", converter);
        XCService xcService = new XCService();
        inject(xcService, "xcRepo", xcs);
        LinkService links = new LinkService();
        inject(links, "linkRepo", new LinkRepo());
        NeNodeRepo nodes = new NeNodeRepo();
        inject(nodes, "tpRepo", tps);
        SiteNodeService service = new SiteNodeService();
        inject(service, "equipmentRepo", equipment);
        inject(service, "tpRepo", tps);
        inject(service, "xcService", xcService);
        inject(service, "linkService", links);
        inject(service, "nodeUtils", new NodeUtils());
        inject(service, "neNodeRepo", nodes);
        SiteModelConfig config = mock(SiteModelConfig.class);
        when(config.getMainCardTypes(anyString(), anyInt(), anyBoolean(), anyString(), any()))
                .thenAnswer(call -> Collections.singletonList(call.getArgument(0)));
        when(config.getSlaveCardTypes(anyString(), anyInt(), anyBoolean(), anyString(), any()))
                .thenReturn(Collections.emptyList());
        SiteRepo repo = new SiteRepo();
        inject(repo, "siteModelConfig", config);
        inject(repo, "siteNodeService", service);
        inject(repo, "neNodeRepo", nodes);
        inject(repo, "linkService", links);
        List<SiteNodeInput> inputs = new ArrayList<>();
        for (int i = 0; i < cardClasses.length; i++) {
            String id = Character.toString((char) ('A' + i));
            Node node = new NodeBuilder().setNodeId(new NodeId("Site-" + id + "#Ne-" + id))
                    .setTerminationPoint(new ArrayList<>())
                    .addAugmentation(Node1.class, new Node1Builder().setPhysical(new PhysicalBuilder()
                            .setEquipments(new ArrayList<>()).setInternalLinks(new ArrayList<>())
                            .setCrossConnections(new ArrayList<>()).build()).build()).build();
            inputs.add(SiteNodeInput.builder().siteId("Site-" + id).nodeType(cardClasses[i])
                    .neSubType(NeSubType.OPC_OTM).ipNode(node)
                    .ramanOnLeft(i > 0 && spans[i - 1])
                    .ramanOnRight(i < spans.length && spans[i]).build());
        }
        SiteInput input = SiteInput.builder().nodesMap(Collections.emptyMap()).vendorName("COHERENT")
                .vendorType("CHASSIS").isProtected(false).grid(0).plane("p").planeId("p")
                .riskGroupName("r").linkModel("2").wdmBand(WDM_Band.C_L)
                .protectionType(ProtectionUnprotected.class).build();
        Method allocate = SiteRepo.class.getDeclaredMethod("allocate", SiteInput.class, NeInfo.class,
                List.class, SiteNodeInfo.class, SiteNodeInfo.class, Set.class, RoutingType.class);
        allocate.setAccessible(true);
        return ((AllocatedSiteInfo) allocate.invoke(repo, input, model(), inputs, null, null,
                new HashSet<>(), RoutingType.Main)).getSiteRoute();
    }

    private Set<String> links(Route route) {
        List<String> result = route.getLinks().stream().map(link -> shortTp(link.getSource().getSourceTp().getValue())
                + "->" + shortTp(link.getDestination().getDestTp().getValue())).collect(Collectors.toList());
        assertEquals(result.size(), new HashSet<>(result).size(), "Duplicate links");
        return new HashSet<>(result);
    }

    private String shortTp(String id) {
        return id.replaceAll("Site-(.)#Ne-.#LINECARD-1-(\\d+)#PORT-1-\\d+-", "$1:$2:");
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
