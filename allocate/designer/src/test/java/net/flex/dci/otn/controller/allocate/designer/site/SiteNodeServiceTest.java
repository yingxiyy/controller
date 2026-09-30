package net.flex.dci.otn.controller.allocate.designer.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import net.flex.dci.otn.controller.allocate.ne.Aps;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.CrossConnectionPoint;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import net.flex.dci.otn.controller.allocate.ne.Ne;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

class SiteNodeServiceTest {

    @Test
    void selectsTheRequestedMuxPanelMpoGroupForEachFmux() throws Exception {
        Map<String, List<ExternalLinkTo>> relations = new LinkedHashMap<>();
        relations.put("MPO?,1,4,1", Arrays.asList(target("FMUX_32", "MPO?,1,4,1")));
        relations.put("MPO?,5,8,1", Arrays.asList(target("FMUX_32", "MPO?,1,4,1")));

        Map<String, List<ExternalLinkTo>> direct = SiteNodeService.selectPortRelation(
                relations, "FMUX_32", "MPO?,1,4,1", "MPO?,1,4,1");
        Map<String, List<ExternalLinkTo>> extension = SiteNodeService.selectPortRelation(
                relations, "FMUX_32", "MPO?,5,8,1", "MPO?,1,4,1");

        assertEquals("MPO?,1,4,1", direct.keySet().iterator().next());
        assertEquals("MPO?,5,8,1", extension.keySet().iterator().next());
    }

    @Test
    void rejectsMissingDualFmuxPortRelation() {
        assertThrows(NeDesignerException.class, () -> SiteNodeService.selectPortRelation(
                new LinkedHashMap<>(), "FMUX_32", "MPO?,5,8,1", "MPO?,1,4,1"));
    }

    @Test
    void rewritesOnlyFmux32ApsCrossConnection() {
        SiteNodeService service = new SiteNodeService();
        CrossConnection ordinary = crossConnection("MPO?,1,4,1", "SIG", null);
        CrossConnection aps = crossConnection("SIGA,SIGB", "COM1,COM2", new Aps());

        assertSame(ordinary, service.adaptFmux32ProtectionXc(ordinary));
        CrossConnection adaptedAps = service.adaptFmux32ProtectionXc(aps);
        assertEquals("COM1,COM2", adaptedAps.getFrom().getPort());
        assertEquals("SIGA,SIGB", adaptedAps.getTo().getPort());
        assertNotNull(adaptedAps.getAps());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsPhysicalMpoSigFanInThroughFmux32NodePath() throws Exception {
        XCService xcService = new XCService();
        XCRepo repo = new XCRepo();
        Field jsonYangConverter = XCRepo.class.getDeclaredField("jsonYangConverter");
        jsonYangConverter.setAccessible(true);
        jsonYangConverter.set(repo, new JsonYangConverter());
        Field xcRepo = XCService.class.getDeclaredField("xcRepo");
        xcRepo.setAccessible(true);
        xcRepo.set(xcService, repo);

        SiteNodeService service = new SiteNodeService();
        Field serviceXc = SiteNodeService.class.getDeclaredField("xcService");
        serviceXc.setAccessible(true);
        serviceXc.set(service, xcService);

        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        Ne ne = new ObjectMapper().readValue(
                getClass().getClassLoader().getResourceAsStream(resource), Ne.class);
        Card fmux32 = new NeInfo(ne, resource).getCardByCardVendor("FMUX_32");

        Map<String, String> ports = new LinkedHashMap<>();
        ports.put("MPO1", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO1");
        ports.put("MPO2", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO2");
        ports.put("MPO3", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO3");
        ports.put("MPO4", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO4");
        ports.put("SIG", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-SIG");
        ports.put("COM1", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-COM1");
        ports.put("COM2", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-COM2");
        ports.put("SIGA", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-SIGA");
        ports.put("SIGB", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-SIGB");

        CardTps cardTps = CardTps.builder()
                .card(fmux32)
                .portNameTpMap(ports)
                .slavePortNameTpMap(Collections.emptyMap())
                .thirdPortNameTpMap(Collections.emptyMap())
                .build();
        Method createNodeXcs = SiteNodeService.class.getDeclaredMethod("createNodeXcs",
                Integer.class, Boolean.class, List.class, String.class, SiteResourceLayout.class);
        createNodeXcs.setAccessible(true);
        List<CrossConnections> result = (List<CrossConnections>) createNodeXcs.invoke(service,
                50, true, Arrays.asList(cardTps), "Site-A#Ne-A",
                new ByteDance2FlexOneToOneResourceLayout());

        CrossConnections mpoSig = result.stream()
                .filter(xc -> xc.getAps() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing physical FMUX32 MPO-SIG XC"));
        assertEquals(4, mpoSig.getSourceTp().size());
        assertEquals(1, mpoSig.getDestinationTp().size());
        assertEquals("Site-A#Ne-A#LINECARD-1-3#PORT-1-3-SIG",
                mpoSig.getDestinationTp().get(0).getTpRef().getValue());

        CrossConnections aps = result.stream()
                .filter(xc -> xc.getAps() != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing FMUX32 APS XC"));
        assertEquals(2, aps.getSourceTp().size());
        assertEquals(2, aps.getDestinationTp().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void protectsOnlySlotThreeFmuxInDualFmux64Layout() throws Exception {
        XCService xcService = new XCService();
        XCRepo repo = new XCRepo();
        Field jsonYangConverter = XCRepo.class.getDeclaredField("jsonYangConverter");
        jsonYangConverter.setAccessible(true);
        jsonYangConverter.set(repo, new JsonYangConverter());
        Field xcRepo = XCService.class.getDeclaredField("xcRepo");
        xcRepo.setAccessible(true);
        xcRepo.set(xcService, repo);

        SiteNodeService service = new SiteNodeService();
        Field serviceXc = SiteNodeService.class.getDeclaredField("xcService");
        serviceXc.setAccessible(true);
        serviceXc.set(service, xcService);

        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        Ne ne = new ObjectMapper().readValue(
                getClass().getClassLoader().getResourceAsStream(resource), Ne.class);
        Card fmux32 = new NeInfo(ne, resource).getCardByCardVendor("FMUX_32");
        CardTps mainFmux = fmuxCardTps(fmux32, 3);
        CardTps extensionFmux = fmuxCardTps(fmux32, 7);

        Method createNodeXcs = SiteNodeService.class.getDeclaredMethod("createNodeXcs",
                Integer.class, Boolean.class, List.class, String.class, SiteResourceLayout.class);
        createNodeXcs.setAccessible(true);
        List<CrossConnections> result = (List<CrossConnections>) createNodeXcs.invoke(service,
                50, true, Arrays.asList(mainFmux, extensionFmux), "Site-A#Ne-A",
                new ByteDance2OneToTwoResourceLayout(64));

        List<CrossConnections> protectedApsXcs = result.stream()
                .filter(xc -> xc.getAps() != null)
                .filter(xc -> xc.getSourceTp().size() == 2)
                .filter(xc -> xc.getSourceTp().stream()
                        .allMatch(tp -> tp.getTpRef().getValue().contains("-COM")))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, protectedApsXcs.size());
        assertEquals("Site-A#Ne-A#LINECARD-1-3#PORT-1-3-COM1",
                protectedApsXcs.get(0).getSourceTp().get(0).getTpRef().getValue());

        List<CrossConnections> reversedResult = (List<CrossConnections>) createNodeXcs.invoke(service,
                50, true, Arrays.asList(extensionFmux, mainFmux), "Site-A#Ne-A",
                new ByteDance2OneToTwoResourceLayout(64));
        List<CrossConnections> reversedProtectedApsXcs = reversedResult.stream()
                .filter(xc -> xc.getAps() != null)
                .filter(xc -> xc.getSourceTp().size() == 2)
                .filter(xc -> xc.getSourceTp().stream()
                        .allMatch(tp -> tp.getTpRef().getValue().contains("-COM")))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, reversedProtectedApsXcs.size());
        assertEquals("Site-A#Ne-A#LINECARD-1-3#PORT-1-3-COM1",
                reversedProtectedApsXcs.get(0).getSourceTp().get(0).getTpRef().getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsMode10Flex64DualFmuxLinksThroughOlpAndKeepsPointToPointDirect() throws Exception {
        LinkService linkService = new LinkService();
        Field linkRepo = LinkService.class.getDeclaredField("linkRepo");
        linkRepo.setAccessible(true);
        linkRepo.set(linkService, new LinkRepo());

        SiteNodeService service = new SiteNodeService();
        Field serviceLink = SiteNodeService.class.getDeclaredField("linkService");
        serviceLink.setAccessible(true);
        serviceLink.set(service, linkService);

        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        Ne ne = new ObjectMapper().readValue(
                getClass().getClassLoader().getResourceAsStream(resource), Ne.class);
        NeInfo neInfo = new NeInfo(ne, resource);
        List<CardTps> cards = Arrays.asList(
                cardTps(neInfo.getCardByCardVendor("PANEL"), 40,
                        names("MUX"), Collections.emptyMap(), Collections.emptyMap()),
                cardTps(neInfo.getCardByCardVendor("MUXPANEL_64"), 50,
                        names("MUX", "MPO1", "MPO2", "MPO3", "MPO4",
                                "MPO5", "MPO6", "MPO7", "MPO8"),
                        Collections.emptyMap(), Collections.emptyMap()),
                fmuxCardTps(neInfo.getCardByCardVendor("FMUX_32"), 3),
                fmuxCardTps(neInfo.getCardByCardVendor("FMUX_32"), 7),
                cardTps(neInfo.getCardByCardVendor("OLP3_3"), 1,
                        names("1SIG", "1A"), names("1B"), names("1C")),
                cardTps(neInfo.getCardByCardVendor("TILA"), 5,
                        names("LINE_WEST", "LINE_EAST"),
                        Collections.emptyMap(), Collections.emptyMap()));

        Method createLinks = SiteNodeService.class.getDeclaredMethod("createDualFmux64Links",
                SiteResourceLayout.class, NeInfo.class, String.class, List.class,
                boolean.class, String.class, List.class, List.class, Set.class);
        createLinks.setAccessible(true);

        for (boolean reversed : Arrays.asList(false, true)) {
            List<Link> links = new ArrayList<>();
            List<InternalLinks> internalLinks = new ArrayList<>();
            Object protectedCard = createLinks.invoke(service,
                    new ByteDance2OneToTwoResourceLayout(64), neInfo, "Site-A#Ne-A",
                    cards, reversed, "SIGA", links, internalLinks, new HashSet<>());

            assertSame(cards.get(4), protectedCard);
            assertEquals(13, links.size());
            assertEquals(13, internalLinks.size());
            assertLink(links, "LINECARD-1-7#PORT-1-7-SIG", "LINECARD-1-3#PORT-1-3-COM1");
            assertLink(links, "LINECARD-1-3#PORT-1-3-SIG", "LINECARD-1-3#PORT-1-3-COM2");
            assertLink(links, "LINECARD-1-3#PORT-1-3-SIGA", "LINECARD-1-1#PORT-1-1-1SIG");
            assertLink(links, "LINECARD-1-1#PORT-1-1-1A", "LINECARD-1-5#PORT-1-5-LINE_WEST");
            assertLink(links, "LINECARD-1-50#PORT-1-50-MPO1", "LINECARD-1-3#PORT-1-3-MPO1");
            assertLink(links, "LINECARD-1-50#PORT-1-50-MPO5", "LINECARD-1-7#PORT-1-7-MPO1");
        }

        List<Link> pointToPointLinks = new ArrayList<>();
        Object pointToPointProtectedCard = createLinks.invoke(service,
                new ByteDance2Flex64ResourceLayout(true), neInfo, "Site-A#Ne-A",
                cards, false, "SIGA", pointToPointLinks, new ArrayList<InternalLinks>(),
                new HashSet<>());
        assertSame(cards.get(2), pointToPointProtectedCard);
        assertEquals(12, pointToPointLinks.size());
        assertLink(pointToPointLinks, "LINECARD-1-3#PORT-1-3-SIGA",
                "LINECARD-1-5#PORT-1-5-LINE_WEST");
        assertEquals(0, pointToPointLinks.stream()
                .filter(link -> link.getSource().getSourceTp().getValue().contains("LINECARD-1-1"))
                .count());

        List<CardTps> withoutOlp = cards.stream()
                .filter(card -> !"OLP3_3".equals(card.getCard().getCardType()))
                .collect(java.util.stream.Collectors.toList());
        InvocationTargetException missingOlp = assertThrows(InvocationTargetException.class,
                () -> createLinks.invoke(service, new ByteDance2OneToTwoResourceLayout(64),
                        neInfo, "Site-A#Ne-A", withoutOlp, false, "SIGA", new ArrayList<Link>(),
                        new ArrayList<InternalLinks>(), new HashSet<>()));
        assertTrue(missingOlp.getCause() instanceof NeDesignerException);
    }

    @Test
    void dedicatedFlex64OneToTwoPeerSkipsDualFmuxMainBuilder() throws Exception {
        SiteNodeService service = serviceWithRealXcService();
        NeInfo neInfo = bone20NeInfo();
        NodeTp peer = tilaOnlyNodeTp(neInfo, RoutingType.Slave);
        Method createNeXcLink = createNeXcLinkMethod();

        assertDoesNotThrow(() -> createNeXcLink.invoke(service, 0, peer, neInfo,
                ProtectionBidir1To2.class, false));
    }

    @Test
    void roleNullFlex64NodeStillRequiresCompleteMainCardChain() throws Exception {
        SiteNodeService service = serviceWithRealXcService();
        NeInfo neInfo = bone20NeInfo();
        NodeTp incompleteMain = tilaOnlyNodeTp(neInfo, null);
        Method createNeXcLink = createNeXcLinkMethod();

        InvocationTargetException error = assertThrows(InvocationTargetException.class,
                () -> createNeXcLink.invoke(service, 0, incompleteMain, neInfo,
                        ProtectionBidir1To2.class, false));
        assertTrue(error.getCause() instanceof NeDesignerException);
        assertTrue(error.getCause().getMessage()
                .contains("Cannot find required PANEL occurrence 0 for Bone2.0 Flex64"));
    }

    @Test
    void unprotectedBone20Flex32CreatesTilaInSlotOneAndFmuxInSlotThree() throws Exception {
        SiteResourceLayout layout = SiteResourceLayoutFactory.create(
                bone20UnprotectedFlex32Input());
        SiteNodeService service = serviceWithRealEquipmentRepositories();
        List<Equipments> equipments = new ArrayList<>();

        service.createNeEquipAndTp(0, ProtectionUnprotected.class, WDM_Band.C,
                "Site-A#Ne-A", layout.resolveMainCardClasses(
                        Arrays.asList("PANEL", "MUXPANEL", "CMUX", "ILA")),
                Collections.emptyList(), bone20NeInfo(), Collections.emptySet(), equipments,
                new ArrayList<TerminationPoint>(), layout, null);

        assertEquipmentSlot(equipments, "PANEL", "40");
        assertEquipmentSlot(equipments, "MUXPANEL_64", "50");
        assertEquipmentSlot(equipments, "TILA", "1");
        assertEquipmentSlot(equipments, "FMUX_32", "3");
    }

    private SiteNodeService serviceWithRealXcService() throws Exception {
        XCService xcService = new XCService();
        XCRepo repo = new XCRepo();
        Field jsonYangConverter = XCRepo.class.getDeclaredField("jsonYangConverter");
        jsonYangConverter.setAccessible(true);
        jsonYangConverter.set(repo, new JsonYangConverter());
        Field xcRepo = XCService.class.getDeclaredField("xcRepo");
        xcRepo.setAccessible(true);
        xcRepo.set(xcService, repo);

        SiteNodeService service = new SiteNodeService();
        Field serviceXc = SiteNodeService.class.getDeclaredField("xcService");
        serviceXc.setAccessible(true);
        serviceXc.set(service, xcService);
        return service;
    }

    private NeInfo bone20NeInfo() throws Exception {
        String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
        Ne ne = new ObjectMapper().readValue(
                getClass().getClassLoader().getResourceAsStream(resource), Ne.class);
        return new NeInfo(ne, resource);
    }

    private SiteInput bone20UnprotectedFlex32Input() {
        return SiteInput.builder()
                .nodesMap(Collections.emptyMap())
                .vendorName("COHERENT")
                .vendorType("CHASSIS2.0")
                .bandwidth(32)
                .isProtected(false)
                .grid(0)
                .plane("p")
                .planeId("p")
                .riskGroupName("r")
                .linkModel("6")
                .wdmBand(WDM_Band.C)
                .protectionType(ProtectionUnprotected.class)
                .build();
    }

    private SiteNodeService serviceWithRealEquipmentRepositories() throws Exception {
        JsonYangConverter converter = new JsonYangConverter();
        EquipmentRepo equipmentRepo = new EquipmentRepo();
        setField(equipmentRepo, "jsonYangConverter", converter);
        TpRepo tpRepo = new TpRepo();
        setField(tpRepo, "jsonYangConverter", converter);

        SiteNodeService service = new SiteNodeService();
        setField(service, "nodeUtils", new NodeUtils());
        setField(service, "equipmentRepo", equipmentRepo);
        setField(service, "tpRepo", tpRepo);
        return service;
    }

    private void assertEquipmentSlot(List<Equipments> equipments, String cardType, String slot) {
        Equipments equipment = equipments.stream()
                .filter(item -> cardType.equals(item.getEquipTypeConfiged()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing equipment " + cardType));
        assertEquals(slot, equipment.getSlot());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private NodeTp tilaOnlyNodeTp(NeInfo neInfo, RoutingType protectionPeerRole) throws Exception {
        CardTps tila = cardTps(neInfo.getCardByCardVendor("TILA"), 1,
                names("LINE_WEST", "LINE_EAST"),
                Collections.emptyMap(), Collections.emptyMap());
        return NodeTp.builder()
                .nodeId("Site-A#Ne-Peer")
                .nodeTpList(Collections.emptyList())
                .equipments(Collections.emptyList())
                .cardTps(new ArrayList<>(Arrays.asList(tila)))
                .slaveCardTps(new ArrayList<>())
                .thirdCardTps(new ArrayList<>())
                .resourceLayout(new ByteDance2OneToTwoResourceLayout(64))
                .protectionPeerRole(protectionPeerRole)
                .build();
    }

    private Method createNeXcLinkMethod() throws Exception {
        Method method = SiteNodeService.class.getDeclaredMethod("createNeXcLink",
                Integer.class, NodeTp.class, NeInfo.class, Class.class, Boolean.class);
        method.setAccessible(true);
        return method;
    }

    private CardTps fmuxCardTps(Card card, int slot) {
        Map<String, String> ports = new LinkedHashMap<>();
        String prefix = "Site-A#Ne-A#LINECARD-1-" + slot + "#PORT-1-" + slot + "-";
        ports.put("MPO1", prefix + "MPO1");
        ports.put("MPO2", prefix + "MPO2");
        ports.put("MPO3", prefix + "MPO3");
        ports.put("MPO4", prefix + "MPO4");
        ports.put("SIG", prefix + "SIG");
        ports.put("COM1", prefix + "COM1");
        ports.put("COM2", prefix + "COM2");
        ports.put("SIGA", prefix + "SIGA");
        ports.put("SIGB", prefix + "SIGB");
        return CardTps.builder()
                .card(card)
                .portNameTpMap(ports)
                .slavePortNameTpMap(Collections.emptyMap())
                .thirdPortNameTpMap(Collections.emptyMap())
                .build();
    }

    private CardTps cardTps(Card card, int slot, Map<String, String> mainPorts,
            Map<String, String> slavePorts, Map<String, String> thirdPorts) {
        return CardTps.builder()
                .card(card)
                .portNameTpMap(withTpIds(slot, mainPorts))
                .slavePortNameTpMap(withTpIds(slot, slavePorts))
                .thirdPortNameTpMap(withTpIds(slot, thirdPorts))
                .build();
    }

    private Map<String, String> names(String... portNames) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String portName : portNames) {
            result.put(portName, portName);
        }
        return result;
    }

    private Map<String, String> withTpIds(int slot, Map<String, String> ports) {
        Map<String, String> result = new LinkedHashMap<>();
        String prefix = "Site-A#Ne-A#LINECARD-1-" + slot + "#PORT-1-" + slot + "-";
        ports.forEach((name, value) -> result.put(name, prefix + value));
        return result;
    }

    private void assertLink(List<Link> links, String firstTpSuffix, String secondTpSuffix) {
        assertTrue(links.stream().anyMatch(link -> {
            String source = link.getSource().getSourceTp().getValue();
            String destination = link.getDestination().getDestTp().getValue();
            return source.endsWith(firstTpSuffix) && destination.endsWith(secondTpSuffix)
                    || source.endsWith(secondTpSuffix) && destination.endsWith(firstTpSuffix);
        }), "Missing physical link between " + firstTpSuffix + " and " + secondTpSuffix);
    }

    private CrossConnection crossConnection(String fromPort, String toPort, Aps aps) {
        CrossConnectionPoint from = new CrossConnectionPoint();
        from.setPort(fromPort);
        CrossConnectionPoint to = new CrossConnectionPoint();
        to.setPort(toPort);
        CrossConnection crossConnection = new CrossConnection();
        crossConnection.setFrom(from);
        crossConnection.setTo(to);
        crossConnection.setAps(aps);
        return crossConnection;
    }

    private ExternalLinkTo target(String cardType, String port) {
        ExternalLinkTo target = new ExternalLinkTo();
        target.setCardType(cardType);
        target.setPort(port);
        return target;
    }
}
