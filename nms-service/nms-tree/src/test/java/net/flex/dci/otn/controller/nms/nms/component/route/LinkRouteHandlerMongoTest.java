/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.component.route;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.flex.dci.otc.mongo.base.core.MongoDaoImpl;
import net.flex.dci.otc.mongo.base.core.SimpleMongoDao;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.NeSystemDefaultDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.OchNodeDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.RackDao;
import net.flex.dci.otc.mongo.dao.ScheduleDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.dao.TopologyDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.dao.impl.CrossConnectionsDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.EquipmentsDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.NeSystemDefaultDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.OchLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.OchNodeDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.PhyLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.PhyNodeDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.RackDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.ScheduleDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.SiteLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.SiteNodeDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.TerminationPointDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.TopologyDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.TunnelDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.ViewLinkDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.ViewNodeDaoImpl;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otc.serialization.autoconfigure.SerializationAutoConfigure;
import net.flex.dci.otn.controller.nms.cache.NmsCacheManager;
import net.flex.dci.otn.controller.nms.nms.component.route.link.OchLinkRouteSequenceRetriever;
import net.flex.dci.otn.controller.nms.nms.component.route.link.PhyLinkRouteSequenceRetriever;
import net.flex.dci.otn.controller.nms.nms.component.route.link.RouteSequenceRetriever;
import net.flex.dci.otn.controller.nms.nms.component.route.link.SiteLinkRouteSequenceRetriever;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteSequenceDtoRetriever;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.impl.RouteRetrieverImpl;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.impl.RouteSequenceDtoRetrieverImpl;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;

class LinkRouteHandlerMongoTest {

    private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE = "sotn";
    private static JsonUtil jsonUtil;

    @Test
    void displayTunnelRouteGraph() throws Exception {
        System.setProperty("routeTunnelId",
            "Tunnel-Site-2083017033158627328#Ne-2084903014245732352#LINECARD-1-3#PORT-1-3-C3-Site-2083017032311377920#Ne-2084903014325424128#LINECARD-1-3#PORT-1-3-C3");

        String mongoUri = System.getProperty("mongoUri", DEFAULT_MONGO_URI);
        String databaseName = System.getProperty("mongoDatabase", DEFAULT_DATABASE);

        try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
            MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
            LinkRouteHandler routeHandler = buildRouteHandler(mongoTemplate);

            String tunnelId = readRouteTunnelId();

            List<RouteInfo> routes = routeHandler.getRoute(buildTunnelInput(tunnelId));

            assertNotNull(routes, "getRoute result is null for tunnel " + tunnelId);
            assertFalse(routes.isEmpty(), "getRoute result is empty for tunnel " + tunnelId);

            System.out.println("routeDisplayTunnelId=" + tunnelId);
            System.out.println("routeInfoCount=" + routes.size());
            // 输出前端打印的json到文件
            NetconfTopology netconfTopology = (NetconfTopology) getField(routeHandler,
                    "netconfTopology");
            PhyNodeDao phyNodeDao = (PhyNodeDao) getField(netconfTopology, "phyNodeDao");
            Path routesJsonPath = writeRoutesJson(tunnelId, routes, phyNodeDao);
            System.out.println("routesJsonPath=" + routesJsonPath.toAbsolutePath());
            printRouteSummary(routes);
            printRegResourceSummary(routesJsonPath);
            // 读取文件json，转成html图形化
            new RouteInfoSvgRenderTest().shouldRenderRouteInfoJsonAsStaticHtmlSvg();
        }
    }

    private static Path writeRoutesJson(String tunnelId, List<RouteInfo> routes,
            PhyNodeDao phyNodeDao) throws Exception {
        Path outputDir = Paths.get("target", "route-display-graph");
        Files.createDirectories(outputDir);
        Path output = outputDir.resolve("route-info-" + Integer.toHexString(
                tunnelId.hashCode()) + ".json");
        String routesJson = jsonUtil().fromDataObjectToJson("nms", "get-route",
                new GetRouteOutputBuilder().setRouteInfo(routes).build(), false);
        routesJson = addRegResourceSummaries(routesJson, phyNodeDao);
        Files.write(output, routesJson.getBytes(StandardCharsets.UTF_8));
        return output;
    }

    /**
     * RouteInfo contains the REG TP/link hops, but the two REG self-XCs and the corresponding
     * node internal-links are physical-node resources rather than route-sequence entries. Add a
     * test-only summary to each leg payload so the SVG can show the exact remove-leg scope.
     */
    private static String addRegResourceSummaries(String routesJson, PhyNodeDao phyNodeDao) {
        JsonObject root = new JsonParser().parse(routesJson).getAsJsonObject();
        JsonArray routeInfos = root.getAsJsonObject("output").getAsJsonArray("route-info");
        for (JsonElement routeElement : routeInfos) {
            JsonObject route = routeElement.getAsJsonObject();
            addRegResourceSummary(route.getAsJsonObject("primary"), phyNodeDao);
            addRegResourceSummary(route.getAsJsonObject("secondary"), phyNodeDao);
            JsonArray third = jsonArray(route, "third");
            if (third != null) {
                for (JsonElement thirdElement : third) {
                    addRegResourceSummary(thirdElement.getAsJsonObject(), phyNodeDao);
                }
            }
        }
        return root.toString();
    }

    private static void addRegResourceSummary(JsonObject leg, PhyNodeDao phyNodeDao) {
        if (leg == null) {
            return;
        }
        JsonArray sequences = jsonArray(leg, "route-sequence");
        if (sequences == null) {
            return;
        }
        Map<String, RegResourceSummary> summaries = new LinkedHashMap<>();
        for (JsonElement sequenceElement : sequences) {
            JsonObject sequence = sequenceElement.getAsJsonObject();
            JsonObject tpHop = jsonObject(sequence, "tp-hop");
            JsonObject phyNode = jsonObject(tpHop, "phy-node");
            JsonObject physicalNode = jsonObject(phyNode, "physical");
            if (!"EPC-REG".equals(jsonString(physicalNode, "customed-type"))) {
                continue;
            }
            String nodeId = jsonString(phyNode, "node-id");
            RegResourceSummary summary = summaries.get(nodeId);
            if (summary == null) {
                JsonObject siteNode = jsonObject(tpHop, "site-node");
                JsonObject site = jsonObject(siteNode, "site");
                summary = new RegResourceSummary(nodeId,
                        jsonString(physicalNode, "friendly-name"),
                        jsonString(siteNode, "node-id"), jsonString(site, "friendly-name"));
                summaries.put(nodeId, summary);
            }
            JsonObject phyTp = jsonObject(tpHop, "phy-tp");
            String tpId = jsonString(phyTp, "tp-id");
            if (!tpId.isEmpty()) {
                summary.endpointTpIds.add(tpId);
                summary.lPortIds.add(regPortId(tpId));
                JsonObject physicalTp = jsonObject(phyTp, "physical");
                String equipmentId = jsonString(physicalTp, "equipment-ref");
                if (!equipmentId.isEmpty()) {
                    summary.equipmentIds.add(equipmentId);
                }
            }
        }
        if (summaries.isEmpty()) {
            return;
        }

        for (JsonElement sequenceElement : sequences) {
            JsonObject linkHop = jsonObject(sequenceElement.getAsJsonObject(), "link-hop");
            JsonObject source = jsonObject(linkHop, "source");
            JsonObject destination = jsonObject(linkHop, "destination");
            String sourceNode = jsonString(source, "source-node");
            String destinationNode = jsonString(destination, "dest-node");
            String sourceTp = jsonString(source, "source-tp");
            String destinationTp = jsonString(destination, "dest-tp");
            String linkId = jsonString(linkHop, "link-id");
            for (RegResourceSummary summary : summaries.values()) {
                if (!linkId.isEmpty() && (summary.nodeId.equals(sourceNode)
                        || summary.nodeId.equals(destinationNode))) {
                    summary.osLinkIds.add(linkId);
                    summary.osLinkEndpointPairs.add(Arrays.asList(sourceTp, destinationTp));
                }
            }
        }
        for (RegResourceSummary summary : summaries.values()) {
            enrichRegNodeResources(summary, phyNodeDao.getConfigPhyNodeById(summary.nodeId));
        }
        JsonArray resources = new JsonArray();
        summaries.values().stream().map(RegResourceSummary::toJson).forEach(resources::add);
        leg.add("reg-resources", resources);
    }

    private static void enrichRegNodeResources(RegResourceSummary summary, Node node) {
        Node1 nodeAttr = node == null ? null : node.getAugmentation(Node1.class);
        if (nodeAttr == null || nodeAttr.getPhysical() == null) {
            return;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514
                .phy.node.attributes.Physical physical = nodeAttr.getPhysical();
        if (physical.getCrossConnections() != null) {
            physical.getCrossConnections().stream()
                    .filter(xc -> xc.getCrossConnectionId() != null)
                    .filter(xc -> (xc.getSourceTp() != null && xc.getSourceTp().stream()
                            .anyMatch(tp -> tp.getTpRef() != null
                                    && summary.matchesLPort(tp.getTpRef().getValue())))
                            || (xc.getDestinationTp() != null && xc.getDestinationTp().stream()
                            .anyMatch(tp -> tp.getTpRef() != null
                                    && summary.matchesLPort(tp.getTpRef().getValue()))))
                    .forEach(xc -> summary.xcIds.add(xc.getCrossConnectionId().getValue()));
        }
        if (physical.getInternalLinks() != null) {
            physical.getInternalLinks().stream()
                    .filter(link -> link.getLinkRef() != null
                            && summary.matchesOsLink(link.getLinkRef()))
                    .forEach(link -> summary.internalLinkIds.add(link.getLinkRef()));
        }
    }

    private static String regPortId(String tpId) {
        return tpId.replaceFirst("#(IN|OUT)$", "");
    }

    private static JsonObject jsonObject(JsonObject parent, String name) {
        if (parent == null || !parent.has(name) || parent.get(name).isJsonNull()
                || !parent.get(name).isJsonObject()) {
            return null;
        }
        return parent.getAsJsonObject(name);
    }

    private static JsonArray jsonArray(JsonObject parent, String name) {
        if (parent == null || !parent.has(name) || parent.get(name).isJsonNull()
                || !parent.get(name).isJsonArray()) {
            return null;
        }
        return parent.getAsJsonArray(name);
    }

    private static String jsonString(JsonObject parent, String name) {
        return parent == null || !parent.has(name) || parent.get(name).isJsonNull()
                ? "" : parent.get(name).getAsString();
    }

    private static final class RegResourceSummary {
        private final String nodeId;
        private final String nodeName;
        private final String siteId;
        private final String siteName;
        private final Set<String> lPortIds = new LinkedHashSet<>();
        private final Set<String> endpointTpIds = new LinkedHashSet<>();
        private final Set<String> equipmentIds = new LinkedHashSet<>();
        private final Set<String> xcIds = new LinkedHashSet<>();
        private final Set<String> osLinkIds = new LinkedHashSet<>();
        private final Set<List<String>> osLinkEndpointPairs = new LinkedHashSet<>();
        private final Set<String> internalLinkIds = new LinkedHashSet<>();

        private RegResourceSummary(String nodeId, String nodeName, String siteId,
                String siteName) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.siteId = siteId;
            this.siteName = siteName;
        }

        private boolean matchesLPort(String tpId) {
            return lPortIds.contains(regPortId(tpId));
        }

        private boolean matchesOsLink(String linkRef) {
            if (osLinkIds.contains(linkRef)) {
                return true;
            }
            boolean sameEndpoints = osLinkEndpointPairs.stream().anyMatch(pair -> pair.size() == 2
                    && !pair.get(0).isEmpty() && !pair.get(1).isEmpty()
                    && linkRef.contains(pair.get(0)) && linkRef.contains(pair.get(1)));
            // Some InternalLink records store the reverse-direction OS-link id. On an EPC-REG
            // node, matching one of this leg's exact L-port ids still scopes the link safely.
            return sameEndpoints || lPortIds.stream().anyMatch(linkRef::contains);
        }

        private JsonObject toJson() {
            JsonObject result = new JsonObject();
            result.addProperty("node-id", nodeId);
            result.addProperty("node-name", nodeName);
            result.addProperty("site-id", siteId);
            result.addProperty("site-name", siteName);
            result.add("l-ports", stringsJson(lPortIds));
            result.add("endpoint-tps", stringsJson(endpointTpIds));
            result.add("equipment", stringsJson(equipmentIds));
            result.add("xcs", stringsJson(xcIds));
            result.add("os-links", stringsJson(osLinkIds));
            result.add("internal-links", stringsJson(internalLinkIds));
            return result;
        }

        private static JsonArray stringsJson(Set<String> values) {
            JsonArray result = new JsonArray();
            values.forEach(result::add);
            return result;
        }
    }

    private static void printRouteSummary(List<RouteInfo> routes) {
        for (int i = 0; i < routes.size(); i++) {
            RouteInfo route = routes.get(i);
            int primarySequenceSize = route.getPrimary() == null
                    || route.getPrimary().getRouteSequence() == null
                    ? 0 : route.getPrimary().getRouteSequence().size();
            int secondarySequenceSize = route.getSecondary() == null
                    || route.getSecondary().getRouteSequence() == null
                    ? 0 : route.getSecondary().getRouteSequence().size();
            int thirdCount = route.getThird() == null ? 0 : route.getThird().size();
            System.out.println("route[" + i + "].index=" + route.getIndex()
                    + ", primarySequenceSize=" + primarySequenceSize
                    + ", secondarySequenceSize=" + secondarySequenceSize
                    + ", thirdCount=" + thirdCount);
        }
    }

    private static void printRegResourceSummary(Path routesJsonPath) throws Exception {
        JsonObject root = new JsonParser().parse(new String(Files.readAllBytes(routesJsonPath),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray routeInfos = root.getAsJsonObject("output").getAsJsonArray("route-info");
        for (int routeIndex = 0; routeIndex < routeInfos.size(); routeIndex++) {
            JsonObject route = routeInfos.get(routeIndex).getAsJsonObject();
            printLegRegResourceSummary(routeIndex + 1, "primary", route.getAsJsonObject("primary"));
            printLegRegResourceSummary(routeIndex + 1, "secondary", route.getAsJsonObject("secondary"));
            JsonArray third = jsonArray(route, "third");
            if (third != null) {
                for (int i = 0; i < third.size(); i++) {
                    printLegRegResourceSummary(routeIndex + 1, "third-" + (i + 1),
                            third.get(i).getAsJsonObject());
                }
            }
        }
    }

    private static void printLegRegResourceSummary(int routeIndex, String leg, JsonObject payload) {
        JsonArray resources = jsonArray(payload, "reg-resources");
        if (resources == null) {
            return;
        }
        for (JsonElement element : resources) {
            JsonObject resource = element.getAsJsonObject();
            System.out.println("route[" + routeIndex + "]." + leg + ".REG: node="
                    + jsonString(resource, "node-id") + ", LPorts="
                    + jsonArray(resource, "l-ports").size() + ", selfXcs="
                    + jsonArray(resource, "xcs").size() + ", osLinks="
                    + jsonArray(resource, "os-links").size() + ", internalLinks="
                    + jsonArray(resource, "internal-links").size() + ", equipment="
                    + jsonArray(resource, "equipment").size());
        }
    }

    private static GetRouteInput buildTunnelInput(String tunnelId) {
        return new GetRouteInputBuilder()
                .setTopologyRef(TopologyId.getDefaultInstance(Constants.SITE_TOPO_KEY))
                .setTunnelRef(Uri.getDefaultInstance(tunnelId))
                .build();
    }

    private static String readRouteTunnelId() {
        String tunnelId = System.getProperty("routeTunnelId");
        assumeTrue(tunnelId != null && !tunnelId.trim().isEmpty(),
                "pass -DrouteTunnelId=... with the tunnel changed by add/remove protection leg");
        return tunnelId.trim();
    }

    private static LinkRouteHandler buildRouteHandler(MongoTemplate mongoTemplate) throws Exception {
        NetconfTopology netconfTopology = buildNetconfTopology(mongoTemplate);
        CrossConnectionsDao crossConnectionsDao = (CrossConnectionsDao) getField(netconfTopology,
                "crossConnectionsDao");
        EquipmentsDao equipmentsDao = (EquipmentsDao) getField(netconfTopology, "equipmentsDao");

        RouteSequenceRetriever routeSequenceRetriever = buildRouteSequenceRetriever(
                netconfTopology, crossConnectionsDao, equipmentsDao);
        RouteSequenceDtoRetriever routeSequenceDtoRetriever =
                new RouteSequenceDtoRetrieverImpl(netconfTopology, crossConnectionsDao);
        RouteRetriever routeRetriever =
                new RouteRetrieverImpl(routeSequenceRetriever, routeSequenceDtoRetriever);

        SiteLinkRoute siteLinkRoute = new SiteLinkRoute(netconfTopology, routeRetriever);
        TunnelRoute tunnelRoute = new TunnelRoute(netconfTopology, routeRetriever);
        PhyLinkRoute phyLinkRoute = new PhyLinkRoute(netconfTopology, routeRetriever);
        OchLinkRoute ochLinkRoute = new OchLinkRoute(netconfTopology, routeRetriever);
        for (AbstractLinkRoute route : Arrays.asList(siteLinkRoute, tunnelRoute, phyLinkRoute,
                ochLinkRoute)) {
            setField(route, "crossConnectionsDao", crossConnectionsDao);
        }

        return new LinkRouteHandler(netconfTopology,
                Arrays.asList(siteLinkRoute, tunnelRoute, phyLinkRoute, ochLinkRoute));
    }

    private static NetconfTopology buildNetconfTopology(MongoTemplate mongoTemplate)
            throws Exception {
        TopologyDao topologyDao = wire(new TopologyDaoImpl(), mongoTemplate);
        TunnelDao tunnelDao = wire(new TunnelDaoImpl(), mongoTemplate);
        SiteLinkDao siteLinkDao = wire(new SiteLinkDaoImpl(), mongoTemplate);
        TerminationPointDao terminationPointDao = wire(new TerminationPointDaoImpl(), mongoTemplate);
        CrossConnectionsDao crossConnectionsDao =
                nullSafeCrossConnectionsDao(wire(new CrossConnectionsDaoImpl(), mongoTemplate));
        PhyNodeDao phyNodeDao = fullNodePhyNodeDao(wire(new PhyNodeDaoImpl(), mongoTemplate));
        SiteNodeDao siteNodeDao = wire(new SiteNodeDaoImpl(), mongoTemplate);
        OchNodeDao ochNodeDao = wire(new OchNodeDaoImpl(), mongoTemplate);
        PhyLinkDao phyLinkDao = wire(new PhyLinkDaoImpl(), mongoTemplate);
        EquipmentsDao equipmentsDao = wire(new EquipmentsDaoImpl(), mongoTemplate);
        ViewLinkDao viewLinkDao = wire(new ViewLinkDaoImpl(), mongoTemplate);
        ViewNodeDao viewNodeDao = wire(new ViewNodeDaoImpl(), mongoTemplate);
        OchLinkDao ochLinkDao = wire(new OchLinkDaoImpl(), mongoTemplate);
        RackDao rackDao = wire(new RackDaoImpl(), mongoTemplate);
        ScheduleDao scheduleDao = wire(new ScheduleDaoImpl(), mongoTemplate);
        NeSystemDefaultDao neSystemDefaultDao = wire(new NeSystemDefaultDaoImpl(), mongoTemplate);
        SubNetTreeNodeDao subNetTreeNodeDao = emptyDao(SubNetTreeNodeDao.class);

        NetconfTopology netconfTopology = new NetconfTopology(topologyDao, tunnelDao, siteLinkDao,
                terminationPointDao, crossConnectionsDao, phyNodeDao, siteNodeDao, ochNodeDao,
                phyLinkDao, equipmentsDao, viewLinkDao, viewNodeDao, ochLinkDao, rackDao,
                scheduleDao, neSystemDefaultDao, subNetTreeNodeDao);
        return netconfTopology;
    }

    private static RouteSequenceRetriever buildRouteSequenceRetriever(NetconfTopology netconfTopology,
            CrossConnectionsDao crossConnectionsDao, EquipmentsDao equipmentsDao) throws Exception {
        RouteSequenceRetriever routeSequenceRetriever = new RouteSequenceRetriever();
        PhyLinkRouteSequenceRetriever phy = new PhyLinkRouteSequenceRetriever();
        OchLinkRouteSequenceRetriever och = new OchLinkRouteSequenceRetriever();
        SiteLinkRouteSequenceRetriever site = new SiteLinkRouteSequenceRetriever();

        for (Object retriever : Arrays.asList(phy, och, site)) {
            setField(retriever, "netconfTopology", netconfTopology);
            setField(retriever, "routeSequenceRetriever", routeSequenceRetriever);
            setField(retriever, "crossConnectionsDao", crossConnectionsDao);
        }
        setField(och, "equipmentsDao", equipmentsDao);

        setField(routeSequenceRetriever, "phyLinkRouteSequenceRetriever", phy);
        setField(routeSequenceRetriever, "ochLinkRouteSequenceRetriever", och);
        setField(routeSequenceRetriever, "siteLinkRouteSequenceRetriever", site);
        setField(routeSequenceRetriever, "netconfTopology", netconfTopology);
        setField(routeSequenceRetriever, "nmsCacheManager", inMemoryNmsCacheManager());
        return routeSequenceRetriever;
    }

    private static NmsCacheManager inMemoryNmsCacheManager() {
        Map<String, RouteSequenceDto> routes = new ConcurrentHashMap<>();
        return new NmsCacheManager(null) {
            @Override
            public RouteSequenceDto getConnectionRouteSequence(String connectionId) {
                return routes.get(connectionId);
            }

            @Override
            public RouteSequenceDto getOchLinkRoute(String linkId) {
                return routes.get(linkId);
            }

            @Override
            public void setOchLinkRoute(String linkId, RouteSequenceDto linkRouteSequence) {
                routes.put(linkId, linkRouteSequence);
            }
        };
    }

    private static <T> T wire(T dao, MongoTemplate mongoTemplate) throws Exception {
        SimpleMongoDao simpleMongoDao = new SimpleMongoDao(mongoTemplate);

        MongoDaoImpl mongoDao = new MongoDaoImpl();
        setField(mongoDao, "mongoManager", simpleMongoDao);
        setField(mongoDao, "jsonUtil", jsonUtil());

        setField(dao, "mongoDao", mongoDao);
        setField(dao, "mongoTemplate", mongoTemplate);
        return dao;
    }

    @SuppressWarnings("unchecked")
    private static CrossConnectionsDao nullSafeCrossConnectionsDao(CrossConnectionsDao delegate) {
        return (CrossConnectionsDao) Proxy.newProxyInstance(
                LinkRouteHandlerMongoTest.class.getClassLoader(),
                new Class[]{CrossConnectionsDao.class},
                (proxy, method, args) -> {
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException e) {
                        Throwable target = e.getTargetException();
                        if ("getXCByXcIdRegexLike".equals(method.getName())
                                && target instanceof NullPointerException) {
                            return Collections.emptyList();
                        }
                        throw target;
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static PhyNodeDao fullNodePhyNodeDao(PhyNodeDao delegate) {
        return (PhyNodeDao) Proxy.newProxyInstance(
                LinkRouteHandlerMongoTest.class.getClassLoader(),
                new Class[]{PhyNodeDao.class},
                (proxy, method, args) -> {
                    try {
                        if ("listLightOpPhyNodeByIds".equals(method.getName())) {
                            Method fullMethod = findSingleArgumentMethod(delegate.getClass(),
                                    "listOperPhyNodeByIds");
                            return fullMethod.invoke(delegate, args);
                        }
                        if ("listLightConfigPhyNodeByIds".equals(method.getName())) {
                            Method fullMethod = findSingleArgumentMethod(delegate.getClass(),
                                    "listConfigPhyNodeByIds");
                            return fullMethod.invoke(delegate, args);
                        }
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                });
    }

    private static Method findSingleArgumentMethod(Class<?> type, String methodName)
            throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (methodName.equals(method.getName()) && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + methodName);
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyDao(Class<T> daoType) {
        return (T) Proxy.newProxyInstance(
                LinkRouteHandlerMongoTest.class.getClassLoader(),
                new Class[]{daoType},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (boolean.class.equals(returnType) || Boolean.class.equals(returnType)) {
                        return false;
                    }
                    if (int.class.equals(returnType) || Integer.class.equals(returnType)) {
                        return 0;
                    }
                    if (Optional.class.equals(returnType)) {
                        return Optional.empty();
                    }
                    if (method.getName().contains("Paged")) {
                        return new PageImpl<>(Collections.emptyList());
                    }
                    if (List.class.equals(returnType)) {
                        return Collections.emptyList();
                    }
                    return null;
                });
    }

    private static JsonUtil jsonUtil() {
        if (jsonUtil != null) {
            return jsonUtil;
        }
        try {
            jsonUtil = new SerializationAutoConfigure().initJsonUtil();
            return jsonUtil;
        } catch (Exception e) {
            throw new IllegalStateException("failed to initialize JsonUtil for mongo dao test", e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
