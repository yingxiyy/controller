package net.flex.dci.otn.controller.allocate.link.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

class RouteTest {

    @Test
    void selectsOlpApsFromSlaveAndThirdRouteMembershipWhenFmuxComesFirst() {
        String nodeId = "Site-A#Ne-A";
        String olpB = nodeId + "#OLP-B";
        String olpC = nodeId + "#OLP-C";
        CrossConnections fmuxAps = apsXc(nodeId, "FMUX-APS",
                Arrays.asList(nodeId + "#FMUX-COM1", nodeId + "#FMUX-COM2"),
                Arrays.asList(nodeId + "#FMUX-SIGA", nodeId + "#FMUX-SIGB"));
        CrossConnections olpAps = apsXc(nodeId, "OLP-APS",
                Collections.singletonList(nodeId + "#OLP-SIG"),
                Arrays.asList(nodeId + "#OLP-A", olpB, olpC));

        Route.ApsRouteSelection selected = Route.selectAEndApsRoute(
                Arrays.asList(fmuxAps, olpAps), nodeId + "#CLIENT",
                Collections.singletonList(link(olpB, nodeId + "#SLAVE-PEER")),
                Collections.singletonList(link(olpC, nodeId + "#THIRD-PEER")));

        assertSame(olpAps, selected.getApsXc());
        assertEquals(olpB, selected.getSlaveStartTp());
        assertEquals(olpC, selected.getThirdStartTp());
    }

    @Test
    void selectsSameOlpApsWhenOlpComesBeforeFmux() {
        String nodeId = "Site-A#Ne-A";
        String olpB = nodeId + "#OLP-B";
        String olpC = nodeId + "#OLP-C";
        CrossConnections olpAps = apsXc(nodeId, "OLP-APS",
                Collections.singletonList(nodeId + "#OLP-SIG"),
                Arrays.asList(nodeId + "#OLP-A", olpB, olpC));
        CrossConnections fmuxAps = apsXc(nodeId, "FMUX-APS",
                Arrays.asList(nodeId + "#FMUX-COM1", nodeId + "#FMUX-COM2"),
                Arrays.asList(nodeId + "#FMUX-SIGA", nodeId + "#FMUX-SIGB"));

        Route.ApsRouteSelection selected = Route.selectAEndApsRoute(
                Arrays.asList(olpAps, fmuxAps), nodeId + "#CLIENT",
                Collections.singletonList(link(nodeId + "#SLAVE-PEER", olpB)),
                Collections.singletonList(link(nodeId + "#THIRD-PEER", olpC)));

        assertSame(olpAps, selected.getApsXc());
        assertEquals(olpB, selected.getSlaveStartTp());
        assertEquals(olpC, selected.getThirdStartTp());
    }

    @Test
    void selectsOlpApsWhenFanOutIsOnSourceSide() {
        String nodeId = "Site-A#Ne-A";
        String olpB = nodeId + "#OLP-B";
        String olpC = nodeId + "#OLP-C";
        CrossConnections fmuxAps = apsXc(nodeId, "FMUX-APS",
                Arrays.asList(nodeId + "#FMUX-COM1", nodeId + "#FMUX-COM2"),
                Arrays.asList(nodeId + "#FMUX-SIGA", nodeId + "#FMUX-SIGB"));
        CrossConnections reversedOlpAps = apsXc(nodeId, "OLP-APS",
                Arrays.asList(nodeId + "#OLP-A", olpB, olpC),
                Collections.singletonList(nodeId + "#OLP-SIG"));

        Route.ApsRouteSelection selected = Route.selectAEndApsRoute(
                Arrays.asList(fmuxAps, reversedOlpAps), nodeId + "#CLIENT",
                Collections.singletonList(link(olpB, nodeId + "#SLAVE-PEER")),
                Collections.singletonList(link(olpC, nodeId + "#THIRD-PEER")));

        assertSame(reversedOlpAps, selected.getApsXc());
        assertEquals(olpB, selected.getSlaveStartTp());
        assertEquals(olpC, selected.getThirdStartTp());
    }

    @Test
    void keepsLegacyIndexOrderingWhenThereIsOnlyOneAps() {
        String nodeId = "Site-A#Ne-A";
        String legacyB = nodeId + "#LEGACY-B";
        String legacyC = nodeId + "#LEGACY-C";
        CrossConnections legacyAps = apsXc(nodeId, "LEGACY-APS",
                Arrays.asList(nodeId + "#LEGACY-A", legacyB, legacyC),
                Collections.singletonList(nodeId + "#LEGACY-SIG"));

        Route.ApsRouteSelection selected = Route.selectAEndApsRoute(
                Collections.singletonList(legacyAps), nodeId + "#CLIENT",
                Collections.emptyList(), Collections.emptyList());

        assertSame(legacyAps, selected.getApsXc());
        assertEquals(legacyB, selected.getSlaveStartTp());
        assertEquals(legacyC, selected.getThirdStartTp());
    }

    @Test
    void keepsCollectionOrderForExistingTwoRouteProtection() {
        String nodeId = "Site-A#Ne-A";
        CrossConnections existingAps = apsXc(nodeId, "EXISTING-APS",
                Arrays.asList(nodeId + "#EXISTING-A", nodeId + "#EXISTING-B"),
                Collections.singletonList(nodeId + "#EXISTING-SIG"));
        CrossConnections laterAps = apsXc(nodeId, "LATER-APS",
                Collections.singletonList(nodeId + "#LATER-SIG"),
                Arrays.asList(nodeId + "#LATER-A", nodeId + "#LATER-B", nodeId + "#LATER-C"));

        Route.ApsRouteSelection selected = Route.selectAEndApsRoute(
                Arrays.asList(existingAps, laterAps), nodeId + "#CLIENT",
                Collections.singletonList(link(nodeId + "#EXISTING-B", nodeId + "#PEER")),
                null);

        assertSame(existingAps, selected.getApsXc());
        assertEquals(nodeId + "#EXISTING-B", selected.getSlaveStartTp());
        assertNull(selected.getThirdStartTp());
    }

    @Test
    void rejectsAmbiguousOlpApsCandidatesInsteadOfUsingCollectionOrder() {
        String nodeId = "Site-A#Ne-A";
        String olpB = nodeId + "#OLP-B";
        String olpC = nodeId + "#OLP-C";
        CrossConnections firstOlp = apsXc(nodeId, "OLP-APS-1",
                Collections.singletonList(nodeId + "#OLP-SIG-1"),
                Arrays.asList(nodeId + "#OLP-A-1", olpB, olpC));
        CrossConnections secondOlp = apsXc(nodeId, "OLP-APS-2",
                Collections.singletonList(nodeId + "#OLP-SIG-2"),
                Arrays.asList(nodeId + "#OLP-A-2", olpB, olpC));

        CommonException error = assertThrows(CommonException.class,
                () -> Route.selectAEndApsRoute(Arrays.asList(firstOlp, secondOlp),
                        nodeId + "#CLIENT",
                        Collections.singletonList(link(olpB, nodeId + "#SLAVE-PEER")),
                        Collections.singletonList(link(olpC, nodeId + "#THIRD-PEER"))));

        assertTrue(error.getMessage().contains("OLP-APS-1"));
        assertTrue(error.getMessage().contains("OLP-APS-2"));
    }

    @Test
    void resolvesSameEquipmentAEndThroughRouteXc() {
        String prefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-";

        TpId endpoint = Route.resolveSameEquipmentEndpoint(
                prefix + "MPO1", new TpId(prefix + "SIG"), new TpId(prefix + "COM2"),
                Arrays.asList(new CrossConnectionsBuilder()
                        .setSourceTp(Arrays.asList(
                                source(prefix + "MPO1"), source(prefix + "MPO2"),
                                source(prefix + "MPO3"), source(prefix + "MPO4")))
                        .setDestinationTp(Arrays.asList(destination(prefix + "SIG")))
                        .build()));

        assertEquals(prefix + "SIG", endpoint.getValue());
    }

    @Test
    void resolvesSameEquipmentDEndThroughRouteXc() {
        String prefix = "Site-Z#Ne-Z#LINECARD-1-3#PORT-1-3-";

        TpId endpoint = Route.resolveSameEquipmentEndpoint(
                prefix + "SIGA", new TpId(prefix + "SIG"), new TpId(prefix + "COM2"),
                Arrays.asList(new CrossConnectionsBuilder()
                        .setSourceTp(Arrays.asList(source(prefix + "COM2")))
                        .setDestinationTp(Arrays.asList(
                                destination(prefix + "SIGA"), destination(prefix + "SIGB")))
                        .setDirection(LinkDirection.Bidirection)
                        .build()));

        assertEquals(prefix + "COM2", endpoint.getValue());
    }

    @Test
    void resolvesUnidirectionalXcFromSourceToDestination() {
        String prefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-";

        TpId endpoint = Route.resolveSameEquipmentEndpoint(
                prefix + "MPO1", new TpId(prefix + "SIG"), new TpId(prefix + "COM2"),
                Arrays.asList(new CrossConnectionsBuilder()
                        .setSourceTp(Arrays.asList(source(prefix + "MPO1")))
                        .setDestinationTp(Arrays.asList(destination(prefix + "SIG")))
                        .setDirection(LinkDirection.Unidirection)
                        .build()));

        assertEquals(prefix + "SIG", endpoint.getValue());
    }

    @Test
    void doesNotResolveUnidirectionalXcFromDestinationToSource() {
        String prefix = "Site-Z#Ne-Z#LINECARD-1-3#PORT-1-3-";

        TpId endpoint = Route.resolveSameEquipmentEndpoint(
                prefix + "SIGA", new TpId(prefix + "SIG"), new TpId(prefix + "COM2"),
                Arrays.asList(new CrossConnectionsBuilder()
                        .setSourceTp(Arrays.asList(source(prefix + "COM2")))
                        .setDestinationTp(Arrays.asList(destination(prefix + "SIGA")))
                        .setDirection(LinkDirection.Unidirection)
                        .build()));

        assertNull(endpoint);
    }

    @Test
    void aggregatesLegacyCmux64AndBone20Fmux32MpoFanout() {
        assertTrue(Route.isMpoAggregatingEquipType(EquipType.CMUX64));
        assertTrue(Route.isMpoAggregatingEquipType(EquipType.FMUX32));
        assertFalse(Route.isMpoAggregatingEquipType(EquipType.TILA));
        assertFalse(Route.isMpoAggregatingEquipType(EquipType.MUXPANEL));
    }

    @Test
    void tracksBothFmuxAndMuxAtZEndWithoutSkippingAEndFmuxChain() {
        String zTp = "Site-Z#Ne-Z#MUX-1-50#PORT-1-50-MPO";
        String aMux = "Site-A#Ne-A#MUX-1-50";
        List<String> aMerged = new ArrayList<>();
        Route.trackMergedMpoEquipments(aMerged, aMux,
                "Site-A#Ne-A#LINECARD-1-7#PORT-1-7-MPO5", zTp);
        assertEquals(Arrays.asList(aMux), aMerged);

        String zFmux = "Site-Z#Ne-Z#LINECARD-1-7";
        String zMux = "Site-Z#Ne-Z#MUX-1-50";
        List<String> zMerged = new ArrayList<>();
        Route.trackMergedMpoEquipments(zMerged, zFmux,
                "Site-Z#Ne-Z#MUX-1-50#PORT-1-50-MPO5", zTp);
        assertEquals(Arrays.asList(zFmux, zMux), zMerged);
    }

    @Test
    void deduplicatesPhysicalMpoFanoutByVirtualEquipmentPair() {
        String muxPrefix = "Site-A#Ne-A#MUX-1-50#PORT-1-50-MPO";
        String mainFmuxPrefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO";
        String extensionFmuxPrefix = "Site-A#Ne-A#LINECARD-1-7#PORT-1-7-MPO";

        String mainPort1 = Route.mergedMpoLinkKey(muxPrefix + "1", mainFmuxPrefix + "1");
        String mainPort2 = Route.mergedMpoLinkKey(muxPrefix + "2", mainFmuxPrefix + "2");
        String extensionPort1 = Route.mergedMpoLinkKey(
                muxPrefix + "5", extensionFmuxPrefix + "1");
        String extensionPort2 = Route.mergedMpoLinkKey(
                muxPrefix + "6", extensionFmuxPrefix + "2");

        assertEquals(mainPort1, mainPort2);
        assertEquals(extensionPort1, extensionPort2);
        assertEquals(2, new HashSet<>(Arrays.asList(
                mainPort1, mainPort2, extensionPort1, extensionPort2)).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exposesBothDualFmuxBranchesWhileKeepingTheSpanLink() throws Exception {
        String a = "Site-A#Ne-A#";
        String z = "Site-Z#Ne-Z#";
        String aMux = a + "MUX-1-50#PORT-1-50-MPO";
        String zMux = z + "MUX-1-50#PORT-1-50-MPO";
        String aMainFmuxMpo = a + "LINECARD-1-3#PORT-1-3-MPO";
        String aExtensionFmuxMpo = a + "LINECARD-1-7#PORT-1-7-MPO";
        String zMainFmuxMpo = z + "LINECARD-1-3#PORT-1-3-MPO";
        String zExtensionFmuxMpo = z + "LINECARD-1-7#PORT-1-7-MPO";
        String aTila = a + "LINECARD-1-1#PORT-1-1-LINE_WEST";
        String zTila = z + "LINECARD-1-1#PORT-1-1-LINE_WEST";

        List<Link> links = Arrays.asList(
                link(aMux + "5", a + "LINECARD-1-7#PORT-1-7-MPO1"),
                link(a + "LINECARD-1-7#PORT-1-7-SIG",
                        a + "LINECARD-1-3#PORT-1-3-COM1"),
                link(a + "LINECARD-1-3#PORT-1-3-SIGA", aTila),
                link(aMux + "1", a + "LINECARD-1-3#PORT-1-3-MPO1"),
                link(aTila, zTila),
                link(z + "LINECARD-1-3#PORT-1-3-SIGA", zTila),
                link(z + "LINECARD-1-7#PORT-1-7-SIG",
                        z + "LINECARD-1-3#PORT-1-3-COM1"),
                link(zMux + "5", z + "LINECARD-1-7#PORT-1-7-MPO1"),
                link(zMux + "1", z + "LINECARD-1-3#PORT-1-3-MPO1"));

        Route route = siteLinkRoute();
        setField(route, "hasMpoAggregatingCard", true);
        setField(route, "hasDualFmux32", true);
        setField(route, "tpZ", zMux);
        Method build = Route.class.getDeclaredMethod("buildPathRoutObject",
                List.class, String.class, List.class);
        build.setAccessible(true);

        List<PathRouteObject> result = (List<PathRouteObject>) build.invoke(
                route, links, aMux, Collections.emptyList());
        List<String> tpIds = result.stream()
                .filter(item -> item.getResourceType() instanceof Tp)
                .map(item -> ((Tp) item.getResourceType()).getTpHop().getTpRef().getValue())
                .collect(Collectors.toList());
        List<String> linkIds = result.stream()
                .filter(item -> item.getResourceType()
                        instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                                .resource.type.resource.type.Link)
                .map(item -> ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                        .resource.type.resource.type.Link) item.getResourceType())
                        .getLinkHop().getLinkRef().getValue())
                .collect(Collectors.toList());

        assertTrue(linkIds.contains(aTila + "--" + zTila));
        assertTrue(tpIds.containsAll(Arrays.asList(
                aMainFmuxMpo, aExtensionFmuxMpo,
                zMainFmuxMpo, zExtensionFmuxMpo)));
        assertEquals(1, countLinksBetween(linkIds, aMux, aMainFmuxMpo));
        assertEquals(1, countLinksBetween(linkIds, aMux, aExtensionFmuxMpo));
        assertEquals(1, countLinksBetween(linkIds, zMux, zMainFmuxMpo));
        assertEquals(1, countLinksBetween(linkIds, zMux, zExtensionFmuxMpo));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesMpoNamedOneToOneXcId() throws Exception {
        String prefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-";
        String originalId = "LEGACY-XC-MPO-UNCHANGED";

        SpringBeanFinder beanFinder = new SpringBeanFinder();
        ApplicationContext previousContext = SpringBeanFinder.getApplicationContext();
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("allocatorConfig", new AllocatorConfig());
        beanFinder.setApplicationContext(context);
        Route route;
        try {
            route = new Route(null, Route.RouteType.SiteLink);
        } finally {
            beanFinder.setApplicationContext(previousContext);
            context.close();
        }
        Field aggregate = Route.class.getDeclaredField("hasMpoAggregatingCard");
        aggregate.setAccessible(true);
        aggregate.set(route, true);
        Method convert = Route.class.getDeclaredMethod("getRouteXC", List.class, List.class);
        convert.setAccessible(true);

        CrossConnections physicalXc = new CrossConnectionsBuilder()
                .setCrossConnectionId(new Uri(originalId))
                .setSourceTp(Arrays.asList(source(prefix + "MPO")))
                .setDestinationTp(Arrays.asList(destination(prefix + "SIG")))
                .build();
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                .cross.connection.route.sequence.CrossConnections> result =
                (List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                        .cross.connection.route.sequence.CrossConnections>)
                        convert.invoke(route, Arrays.asList(physicalXc), Collections.emptyList());

        assertEquals(originalId, result.get(0).getCrossConnectionId().getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesFmuxMpoSigPhysicalXcsForSiteLink() throws Exception {
        String prefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-";
        String secondPrefix = "Site-A#Ne-A#LINECARD-1-7#PORT-1-7-";

        SpringBeanFinder beanFinder = new SpringBeanFinder();
        ApplicationContext previousContext = SpringBeanFinder.getApplicationContext();
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("allocatorConfig", new AllocatorConfig());
        beanFinder.setApplicationContext(context);
        Route route;
        try {
            route = new Route(null, Route.RouteType.SiteLink);
        } finally {
            beanFinder.setApplicationContext(previousContext);
            context.close();
        }
        Field aggregate = Route.class.getDeclaredField("hasMpoAggregatingCard");
        aggregate.setAccessible(true);
        aggregate.set(route, true);
        Method convert = Route.class.getDeclaredMethod("getRouteXC", List.class, List.class);
        convert.setAccessible(true);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                .cross.connection.route.sequence.CrossConnections> result =
                (List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                        .cross.connection.route.sequence.CrossConnections>)
                        convert.invoke(route, Arrays.asList(
                                physicalFanIn(prefix), physicalFanIn(secondPrefix)),
                                Collections.emptyList());

        assertEquals(2, result.size());
        result.forEach(xc -> assertEquals(4, xc.getSourceTp().size()));
        assertEquals(Arrays.asList(prefix + "MPO1", prefix + "MPO2", prefix + "MPO3",
                        prefix + "MPO4"),
                result.get(0).getSourceTp().stream()
                        .map(tp -> tp.getTpRef().getValue())
                        .collect(Collectors.toList()));
        assertEquals(prefix + "SIG", result.get(0).getDestinationTp().get(0).getTpRef().getValue());
        assertEquals(Arrays.asList(secondPrefix + "MPO1", secondPrefix + "MPO2",
                        secondPrefix + "MPO3", secondPrefix + "MPO4"),
                result.get(1).getSourceTp().stream()
                        .map(tp -> tp.getTpRef().getValue())
                        .collect(Collectors.toList()));
        assertEquals(secondPrefix + "SIG", result.get(1).getDestinationTp().get(0).getTpRef().getValue());
        List<String> expectedXcIds = Arrays.asList(
                physicalFanIn(prefix).getCrossConnectionId().getValue(),
                physicalFanIn(secondPrefix).getCrossConnectionId().getValue());
        List<String> xcIds = result.stream()
                .map(xc -> xc.getCrossConnectionId().getValue())
                .collect(Collectors.toList());
        assertEquals(expectedXcIds, xcIds);
        Set<String> uniqueXcIds = new HashSet<>(xcIds);
        assertEquals(result.size(), uniqueXcIds.size(), "Physical XC IDs must be unique");
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsLegacyEightPortMpoRouteAggregation() throws Exception {
        String prefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-";

        SpringBeanFinder beanFinder = new SpringBeanFinder();
        ApplicationContext previousContext = SpringBeanFinder.getApplicationContext();
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("allocatorConfig", new AllocatorConfig());
        beanFinder.setApplicationContext(context);
        Route route;
        try {
            route = new Route(null, Route.RouteType.SiteLink);
        } finally {
            beanFinder.setApplicationContext(previousContext);
            context.close();
        }
        Field aggregate = Route.class.getDeclaredField("hasMpoAggregatingCard");
        aggregate.setAccessible(true);
        aggregate.set(route, true);
        Method convert = Route.class.getDeclaredMethod("getRouteXC", List.class, List.class);
        convert.setAccessible(true);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                .cross.connection.route.sequence.CrossConnections> result =
                (List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                        .cross.connection.route.sequence.CrossConnections>)
                        convert.invoke(route, Arrays.asList(physicalEightPortFanIn(prefix)),
                                Collections.emptyList());

        assertEquals(1, result.get(0).getSourceTp().size());
        assertEquals(prefix + "MPO", result.get(0).getSourceTp().get(0).getTpRef().getValue());
        assertEquals("XC-" + prefix + "MPO-" + prefix + "SIG",
                result.get(0).getCrossConnectionId().getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesFmuxMpoSigPhysicalXcsForOppositeDirections() throws Exception {
        String prefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-";

        SpringBeanFinder beanFinder = new SpringBeanFinder();
        ApplicationContext previousContext = SpringBeanFinder.getApplicationContext();
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("allocatorConfig", new AllocatorConfig());
        beanFinder.setApplicationContext(context);
        Route route;
        try {
            route = new Route(null, Route.RouteType.SiteLink);
        } finally {
            beanFinder.setApplicationContext(previousContext);
            context.close();
        }
        Field aggregate = Route.class.getDeclaredField("hasMpoAggregatingCard");
        aggregate.setAccessible(true);
        aggregate.set(route, true);
        Method convert = Route.class.getDeclaredMethod("getRouteXC", List.class, List.class);
        convert.setAccessible(true);

        CrossConnections bidirectional = reversePhysicalFanOut(prefix, LinkDirection.Bidirection);
        CrossConnections unidirectional = reversePhysicalFanOut(prefix, LinkDirection.Unidirection);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                .cross.connection.route.sequence.CrossConnections> result =
                (List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                        .cross.connection.route.sequence.CrossConnections>)
                        convert.invoke(route, Arrays.asList(bidirectional, unidirectional),
                                Collections.emptyList());

        assertEquals(bidirectional.getCrossConnectionId().getValue(),
                result.get(0).getCrossConnectionId().getValue());
        assertEquals(unidirectional.getCrossConnectionId().getValue(),
                result.get(1).getCrossConnectionId().getValue());
        result.forEach(xc -> assertEquals(4, xc.getDestinationTp().size()));
        assertEquals(prefix + "SIG", result.get(0).getSourceTp().get(0).getTpRef().getValue());
        assertEquals(Arrays.asList(prefix + "MPO1", prefix + "MPO2", prefix + "MPO3",
                        prefix + "MPO4"),
                result.get(0).getDestinationTp().stream()
                        .map(tp -> tp.getTpRef().getValue())
                        .collect(Collectors.toList()));
        assertEquals(prefix + "SIG", result.get(1).getSourceTp().get(0).getTpRef().getValue());
        assertEquals(Arrays.asList(prefix + "MPO1", prefix + "MPO2", prefix + "MPO3",
                        prefix + "MPO4"),
                result.get(1).getDestinationTp().stream()
                        .map(tp -> tp.getTpRef().getValue())
                        .collect(Collectors.toList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void putsRouteVisibleFmuxComFirstWithoutChangingPhysicalApsMembership() throws Exception {
        String prefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-";
        CrossConnections aps = apsXc("Site-A#Ne-A", "FMUX-APS",
                Arrays.asList(prefix + "COM1", prefix + "COM2"),
                Arrays.asList(prefix + "SIGA", prefix + "SIGB"));
        Route route = siteLinkRoute();
        Method convert = Route.class.getDeclaredMethod("getRouteXC", List.class, List.class);
        convert.setAccessible(true);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                .cross.connection.route.sequence.CrossConnections> result =
                (List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                        .cross.connection.route.sequence.CrossConnections>)
                        convert.invoke(route, Collections.singletonList(aps),
                                Collections.singletonList(link(prefix + "COM2", prefix + "SIGA")));

        assertEquals(Arrays.asList(prefix + "COM2", prefix + "COM1"),
                result.get(0).getSourceTp().stream()
                        .map(tp -> tp.getTpRef().getValue())
                        .collect(Collectors.toList()));
        assertEquals(Arrays.asList(prefix + "SIGA", prefix + "SIGB"),
                result.get(0).getDestinationTp().stream()
                        .map(tp -> tp.getTpRef().getValue())
                        .collect(Collectors.toList()));
        assertEquals("FMUX-APS", result.get(0).getCrossConnectionId().getValue());
    }

    private CrossConnections physicalFanIn(String prefix) {
        return new CrossConnectionsBuilder()
                .setCrossConnectionId(new Uri("XC-" + prefix + "MPO1-" + prefix + "MPO2-"
                        + prefix + "MPO3-" + prefix + "MPO4-" + prefix + "SIG"))
                .setSourceTp(Arrays.asList(
                        source(prefix + "MPO1"), source(prefix + "MPO2"),
                        source(prefix + "MPO3"), source(prefix + "MPO4")))
                .setDestinationTp(Arrays.asList(destination(prefix + "SIG")))
                .build();
    }

    private CrossConnections apsXc(String nodeId, String xcId, List<String> sourceTpIds,
            List<String> destinationTpIds) {
        return new CrossConnectionsBuilder()
                .setCrossConnectionId(new Uri(xcId))
                .setNodeRef(new NodeId(nodeId))
                .setSourceTp(sourceTpIds.stream().map(this::source).collect(Collectors.toList()))
                .setDestinationTp(destinationTpIds.stream().map(this::destination)
                        .collect(Collectors.toList()))
                .setAps(new ApsBuilder().build())
                .build();
    }

    private Link link(String sourceTp, String destinationTp) {
        return new LinkBuilder()
                .setLinkId(new LinkId(sourceTp + "--" + destinationTp))
                .setSource(new SourceBuilder().setSourceNode(new NodeId("source-node"))
                        .setSourceTp(new TpId(sourceTp)).build())
                .setDestination(new DestinationBuilder()
                        .setDestNode(new NodeId("destination-node"))
                        .setDestTp(new TpId(destinationTp)).build())
                .build();
    }

    private Route siteLinkRoute() {
        SpringBeanFinder beanFinder = new SpringBeanFinder();
        ApplicationContext previousContext = SpringBeanFinder.getApplicationContext();
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("allocatorConfig", new AllocatorConfig());
        beanFinder.setApplicationContext(context);
        try {
            return new Route(null, Route.RouteType.SiteLink);
        } finally {
            beanFinder.setApplicationContext(previousContext);
            context.close();
        }
    }

    private void setField(Route route, String name, Object value) throws Exception {
        Field field = Route.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(route, value);
    }

    private long countLinksBetween(List<String> linkIds, String firstTpId, String secondTpId) {
        return linkIds.stream()
                .filter(linkId -> linkId.contains(firstTpId) && linkId.contains(secondTpId))
                .count();
    }

    private CrossConnections reversePhysicalFanOut(String prefix, LinkDirection direction) {
        return new CrossConnectionsBuilder()
                .setCrossConnectionId(new Uri("XC-" + prefix + "SIG-" + prefix + "MPO1-"
                        + prefix + "MPO2-" + prefix + "MPO3-" + prefix + "MPO4"))
                .setSourceTp(Arrays.asList(source(prefix + "SIG")))
                .setDestinationTp(Arrays.asList(
                        destination(prefix + "MPO1"), destination(prefix + "MPO2"),
                        destination(prefix + "MPO3"), destination(prefix + "MPO4")))
                .setDirection(direction)
                .build();
    }

    private CrossConnections physicalEightPortFanIn(String prefix) {
        List<SourceTp> sourceTps = new ArrayList<>();
        StringBuilder xcId = new StringBuilder("XC-");
        for (int port = 1; port <= 8; port++) {
            sourceTps.add(source(prefix + "MPO" + port));
            xcId.append(prefix).append("MPO").append(port).append("-");
        }
        xcId.append(prefix).append("SIG");
        return new CrossConnectionsBuilder()
                .setCrossConnectionId(new Uri(xcId.toString()))
                .setSourceTp(sourceTps)
                .setDestinationTp(Arrays.asList(destination(prefix + "SIG")))
                .build();
    }

    private SourceTp source(String id) {
        return new SourceTpBuilder().setTpRef(new TpId(id)).build();
    }

    private DestinationTp destination(String id) {
        return new DestinationTpBuilder().setTpRef(new TpId(id)).build();
    }
}
