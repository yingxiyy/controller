package net.flex.dci.otn.controller.implement.common.ase;


import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.*;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.utils.WssXcIdGenerator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class DummyOchLinkConstructor {

    private ChangedObject changedObject;
    private Set<CrossConnectionAttributes> aseXcList;
    private List<String> otmNodeList;
    private Link businessOchLink;
    private boolean skipTilaAseInjection;


    public DummyOchLinkConstructor(ChangedObject changedObject) {
        this.changedObject = changedObject;
        aseXcList = new HashSet<>();
    }

    public static DummyOchLinkConstructor defaultConstructor(ChangedObject changedObject) {
        return new DummyOchLinkConstructor(changedObject);
    }

    public static DummyOchLinkConstructor businessOchConstructor(ChangedObject changedObject, Link businessOchLink) {
        return new DummyOchLinkConstructor(changedObject).useDgeDirectionFromOch(businessOchLink);
    }

    private DummyOchLinkConstructor useDgeDirectionFromOch(Link businessOchLink) {
        // 补出来的 dummy OCH 在 DGE 上新建 WSS XC 时，需要借用触发它的业务 OCH 方向。
        this.businessOchLink = businessOchLink;
        return this;
    }


    public synchronized void remove(Link ochLink) {
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        log.info("remove dummy OCH from database {}", ochLinkAttr.getFriendlyName());
        try {
            long lower = ochLinkAttr.getLowerFrequency().getValue().longValue();
            long upper = ochLinkAttr.getUpperFrequency().getValue().longValue();
            long centerFrequency = lower + (upper - lower) / 2;

            String ochLinkId = ochLink.getLinkId().getValue();
            changedObject.addRemovedOchLink(ochLinkId);

            //find out related siteLink
            List<String> siteLinkIdList = ochLink.getSupportingLink().stream().map(sl -> {
                String linkId = sl.getLinkRef().getValue();
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    return linkId;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            //remove dummyOCH in each siteLink
            siteLinkIdList.forEach(linkId -> {
                Link siteLink = changedObject.getChangedSiteLink(linkId);
                Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
                List<String> dummyLinkList = new ArrayList<>(siteLinkAttr.getDummyLink());
                dummyLinkList.remove(ochLinkId);
                Link newSiteLink = new LinkBuilder(siteLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                        .setSite(new SiteBuilder(siteLinkAttr)
                                                .setDummyLink(dummyLinkList)
                                                .build())
                                        .build())
                        .build();
                changedObject.addChangedSiteLink(newSiteLink);
            });

            //remove XC in related node
            RouteInfo rInfo = new RouteInfo();
            rInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
            rInfo.getXcIdList().forEach(xcId -> {
                String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);

                Node node = changedObject.getChangedPhyNode(nodeId);
                Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

                List<CrossConnections> xcList = new ArrayList<>(nodeAttr.getCrossConnections());
                xcList.removeIf(xc -> xc.getCrossConnectionId().getValue().equals(xcId));

                Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                                .setPhysical(new PhysicalBuilder(nodeAttr)
                                        .setCrossConnections(xcList)
                                        .build())
                                .build())
                        .build();
                changedObject.addChangedPhyNode(newNode);
            });
        } catch (Exception e) {
            log.error("some thing wring", e);
            throw e;
        }
    }


    public synchronized Link create(String aTp, String zTp, Available frequency, List<String> amplifierXcList, List<String> otmNodeList, LinkId siteLinkId) {
        this.otmNodeList = otmNodeList;
        this.skipTilaAseInjection = false;
        long lower = frequency.getLowerFrequency().getValue().longValue();
        long upper = frequency.getUpperFrequency().getValue().longValue();
        long centerFrequency = lower + (upper - lower) / 2;

        String friendlyName = String.format("ASE:%d-%d", lower, upper);
        LinkId linkId = new LinkId(OchLinkIdNamingRule.generateId(String.format("#%d-%d#-%s", lower, upper, aTp), zTp));

        List<SupportingLink> slList = new ArrayList<>();
        slList.add(new SupportingLinkBuilder()
                .setLinkRef(siteLinkId)
                .setKey(new SupportingLinkKey(siteLinkId))
                .build());

        log.debug("start create fake och link at {}--{}", lower, upper);
        Link ochLink = new LinkBuilder()
                .setLinkId(linkId)
                .setKey(new LinkKey(linkId))
                .setSupportingLink(slList)
                .addAugmentation(Link1.class,
                        new Link1Builder()
                                .setOch(getOchLinkAttr(amplifierXcList, frequency, friendlyName, aTp, zTp))
                                .build())
                .setSource(new SourceBuilder()
                        .setSourceNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(aTp)))
                        .setSourceTp(new TpId(aTp))
                        .build())
                .setDestination(new DestinationBuilder()
                        .setDestNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(zTp)))
                        .setDestTp(new TpId(zTp))
                        .build())
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1Builder()
                                .setSupportedTunnel(new LinkedList<>())
                                .build())
                .build();


        insertAseXcInPhyNode();
        changedObject.addChangedOchLink(ochLink);

        return ochLink;
    }

    public synchronized Link createWithAseXcEndpoints(String aTp, String zTp, Available frequency,
            List<AseXcEndpoint> aseXcEndpoints, List<String> amplifierXcList,
            List<String> otmNodeList, LinkId siteLinkId) {
        this.otmNodeList = otmNodeList;
        this.skipTilaAseInjection = true;
        long lower = frequency.getLowerFrequency().getValue().longValue();
        long upper = frequency.getUpperFrequency().getValue().longValue();
        String friendlyName = String.format("ASE:%d-%d", lower, upper);
        LinkId linkId = new LinkId(OchLinkIdNamingRule.generateId(
                String.format("#%d-%d#-%s", lower, upper, aTp), zTp));

        List<SupportingLink> slList = new ArrayList<>();
        slList.add(new SupportingLinkBuilder().setLinkRef(siteLinkId)
                .setKey(new SupportingLinkKey(siteLinkId)).build());
        log.debug("start create fake och link at {}--{} with resolved ASE endpoints", lower, upper);

        Link ochLink = new LinkBuilder()
                .setLinkId(linkId)
                .setKey(new LinkKey(linkId))
                .setSupportingLink(slList)
                .addAugmentation(Link1.class, new Link1Builder()
                        .setOch(getOchLinkAttr(aseXcEndpoints, amplifierXcList,
                                frequency, friendlyName)).build())
                .setSource(new SourceBuilder()
                        .setSourceNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(aTp)))
                        .setSourceTp(new TpId(aTp)).build())
                .setDestination(new DestinationBuilder()
                        .setDestNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(zTp)))
                        .setDestTp(new TpId(zTp)).build())
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1Builder()
                                .setSupportedTunnel(new LinkedList<>()).build())
                .build();

        insertAseXcInPhyNode();
        changedObject.addChangedOchLink(ochLink);
        return ochLink;
    }

    private void insertAseXcInPhyNode() {
        log.info("add ase xc into node");

        for (CrossConnectionAttributes xc : aseXcList) {
            String nodeId = xc.getNodeRef().getValue();
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            ArrayList<CrossConnections> newXcList = new ArrayList<>(nodeAttr.getCrossConnections());
            newXcList.add(new CrossConnectionsBuilder(xc).build());
            Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(nodeAttr)
                                    .setCrossConnections(newXcList)
                                    .build())
                            .build())
                    .build();
            changedObject.addChangedPhyNode(newNode);
        };
    }

    private Och getOchLinkAttr(List<String> amplifierXcList, Available frequencyScope, String friendlyName, String aTp, String zTp) {
        Och och = new OchBuilder()
                .setPlaneName(null)
                .setRiskGroupName(null)
                .setFriendlyName(friendlyName)
                .setCreationTime(getCurrentTime())
                .setAdminState(AdminStatus.Down)
                .setAlarmState(AlarmSeverity.Unknown)
                .setProtectionType(ProtectionUnprotected.class)
                .setOchpCardType(null)
                .setOperationalState(OperStatus.Unknown)
                .setImplementState(ImplementState.Allocate)
                .setAlignmentStatus(AlignmentStatusType.Unknown)  //this status is useless for link
                .setExplictRoute(buildRoute(amplifierXcList, frequencyScope))
                .setLowerFrequency(frequencyScope.getLowerFrequency())
                .setUpperFrequency(frequencyScope.getUpperFrequency())
                .build();

        return och;
    }

    private Och getOchLinkAttr(List<AseXcEndpoint> aseXcEndpoints,
            List<String> amplifierXcList, Available frequencyScope, String friendlyName) {
        return new OchBuilder()
                .setPlaneName(null)
                .setRiskGroupName(null)
                .setFriendlyName(friendlyName)
                .setCreationTime(getCurrentTime())
                .setAdminState(AdminStatus.Down)
                .setAlarmState(AlarmSeverity.Unknown)
                .setProtectionType(ProtectionUnprotected.class)
                .setOchpCardType(null)
                .setOperationalState(OperStatus.Unknown)
                .setImplementState(ImplementState.Allocate)
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .setExplictRoute(buildRouteFromEndpoints(
                        aseXcEndpoints, amplifierXcList, frequencyScope))
                .setLowerFrequency(frequencyScope.getLowerFrequency())
                .setUpperFrequency(frequencyScope.getUpperFrequency())
                .build();
    }

    private ExplictRoute buildRoute(List<String> amplifierXcList, Available scope) {
        amplifierXcList.forEach(amplifierXC -> toAseXc(amplifierXC, scope));

//        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> routeXC =
//                aseXcList.stream().map(neXc->new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(neXc)
//                                .build()).collect(Collectors.toList());
        AtomicInteger index = new AtomicInteger(1); // 从 1 开始计数

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> routeXC = aseXcList.stream()
                .map(neXc -> new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(neXc)
                        .setSequence(Long.valueOf(index.getAndIncrement())) // 获取当前值并递增
                        .build()).collect(Collectors.toList());

        Primary primary = new PrimaryBuilder()
                .setCrossConnections(routeXC)
                .setExplicitRouteObjects(new ArrayList<>())
                .build();
        Route aseOchRoute = new RouteBuilder()
                .setIndex((short) 1)
                .setKey(new RouteKey((short) 1))
                .setPrimary(primary)
                .setSecondary(null)
                .setThird(null)
                .build();

        List<Route> routeList = new ArrayList<>();
        routeList.add(aseOchRoute);
        return new ExplictRouteBuilder().setRoute(routeList).build();
    }

    private ExplictRoute buildRouteFromEndpoints(List<AseXcEndpoint> aseXcEndpoints,
            List<String> amplifierXcList, Available scope) {
        // Bone2.0 adds FMUX endpoint XCs. Middle-node processing remains unchanged:
        // TILA/ILA do not inject, while DGE creates the frequency WSS XC.
        aseXcEndpoints.forEach(endpoint -> aseXcList.add(buildInjectXcOnDGE(scope,
                endpoint.getSource(), endpoint.getDestination(), endpoint.getEquipment())));
        amplifierXcList.forEach(amplifierXC -> toAseXc(amplifierXC, scope));

        AtomicInteger index = new AtomicInteger(1);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> routeXC = aseXcList.stream()
                .map(neXc -> new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(neXc)
                        .setSequence(Long.valueOf(index.getAndIncrement()))
                        .build())
                .collect(Collectors.toList());
        Primary primary = new PrimaryBuilder()
                .setCrossConnections(routeXC)
                .setExplicitRouteObjects(new ArrayList<>())
                .build();
        Route aseOchRoute = new RouteBuilder()
                .setIndex((short) 1)
                .setKey(new RouteKey((short) 1))
                .setPrimary(primary)
                .setSecondary(null)
                .setThird(null)
                .build();
        return new ExplictRouteBuilder()
                .setRoute(Collections.singletonList(aseOchRoute))
                .build();
    }

    private void toAseXc(String amplifierXCId, Available scope) {
        String nodeId = PhysicalXcIdNamingRule.getNodeId(amplifierXCId);
        Node node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        CrossConnections xc = nodeAttr.getCrossConnections().stream().filter(x -> x.getCrossConnectionId().getValue().equals(amplifierXCId)).findAny().orElseThrow(() ->
                new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find XC %s in node", amplifierXCId))
        );
        // RAMAN 只参与复用段光放配置，不创建假波 WSS 交叉，也不能按 DGE 查找两个 LINE 端口。
        if (xc.getDescription() != null && xc.getDescription().contains("RAMAN")) {
            return;
        }
        String eqId = PhysicalTpIdNamingRule.getEquipId(xc.getSourceTp().get(0).getTpRef().getValue());
        Equipments eq = nodeAttr.getEquipments().stream().filter(x -> x.getEquipmentId().equalsIgnoreCase(eqId)).findAny().orElseThrow(() ->
                new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find EQ %s in node", eqId))
        );
        if (isInjectionNotRequired(eq)) {
            //inject NOT required.
            return;
        }
        log.debug("create ase xc on {} ({}) and frequency [{},{}]",
                eq.getEquipType(), eq.getEquipmentId(),
                scope.getLowerFrequency().getValue().longValue(), scope.getUpperFrequency().getValue().longValue());

        if (eq.getEquipType().equals(EquipType.IRA)) {
            List<TerminationPoint> tps = getInjectXcTPOnIRA(node, eq);
            CrossConnections aseXc = buildInjectXcOnIRA(scope, tps.get(0), tps.get(1), eq);
            aseXcList.add(aseXc);
        } else {
            List<TerminationPoint> tps = getInjectXcTPOnDGE(node, eq);
            tps = alignDgeTpsWithBusinessOch(nodeId, tps);
            CrossConnections aseXc = buildInjectXcOnDGE(scope, tps.get(0), tps.get(1), eq);
            aseXcList.add(aseXc);
        }
    }

    private boolean isInjectionNotRequired(Equipments eq) {
        return eq.getEquipType().equals(EquipType.ILA)
                || skipTilaAseInjection && isTilaEquipment(eq);
    }

    private boolean isTilaEquipment(Equipments eq) {
        return "TILA".equals(eq.getEquipTypeVendorSpecific())
                || "TILA".equals(eq.getEquipTypeConfiged());
    }

    private List<TerminationPoint> alignDgeTpsWithBusinessOch(String nodeId, List<TerminationPoint> tps) {
        if (businessOchLink == null || tps.size() != 2) {
            return tps;
        }

        Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> businessXc =
                findBusinessDgeWssXc(nodeId, tps);
        if (businessXc.isPresent()) {
            String sourceTp = businessXc.get().getSourceTp().get(0).getTpRef().getValue();
            String destinationTp = businessXc.get().getDestinationTp().get(0).getTpRef().getValue();
            // 直接按业务 OCH 中同一对 DGE TP 的 WSS XC 方向构建新 dummy WSS XC。
            return tps.stream().filter(tp -> sourceTp.equals(tp.getTpId().getValue())).findAny()
                    .flatMap(source -> tps.stream().filter(tp -> destinationTp.equals(tp.getTpId().getValue())).findAny()
                            .map(destination -> Arrays.asList(source, destination)))
                    .orElse(tps);
        }
        log.debug("skip align DGE dummy XC direction on node {}, no business WSS XC matches inject TPs {}",
                nodeId, tps.stream().map(tp -> tp.getTpId().getValue()).collect(Collectors.toList()));
        return tps;
    }

    private Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections>
    findBusinessDgeWssXc(String nodeId, List<TerminationPoint> tps) {
        Link1 ochAug = businessOchLink.getAugmentation(Link1.class);
        if (ochAug == null || ochAug.getOch() == null || ochAug.getOch().getExplictRoute() == null ||
                ochAug.getOch().getExplictRoute().getRoute() == null) {
            return Optional.empty();
        }
        String tpA = tps.get(0).getTpId().getValue();
        String tpZ = tps.get(1).getTpId().getValue();
        for (Route route : ochAug.getOch().getExplictRoute().getRoute()) {
            Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xc =
                    findBusinessDgeWssXc(route, nodeId, tpA, tpZ);
            if (xc.isPresent()) {
                return xc;
            }
        }
        return Optional.empty();
    }

    private Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections>
    findBusinessDgeWssXc(Route route, String nodeId, String tpA, String tpZ) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcs = new ArrayList<>();
        if (route.getPrimary() != null && route.getPrimary().getCrossConnections() != null) {
            xcs.addAll(route.getPrimary().getCrossConnections());
        }
        if (route.getSecondary() != null && route.getSecondary().getCrossConnections() != null) {
            xcs.addAll(route.getSecondary().getCrossConnections());
        }
        if (route.getThird() != null) {
            route.getThird().forEach(third -> {
                if (third.getCrossConnections() != null) {
                    xcs.addAll(third.getCrossConnections());
                }
            });
        }
        return xcs.stream().filter(xc -> isSameDgeWssXc(xc, nodeId, tpA, tpZ)).findAny();
    }

    private boolean isSameDgeWssXc(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc,
            String nodeId, String tpA, String tpZ) {
        if (xc.getWssChannel() == null || xc.getNodeRef() == null ||
                xc.getSourceTp() == null || xc.getSourceTp().size() != 1 ||
                xc.getDestinationTp() == null || xc.getDestinationTp().size() != 1 ||
                !nodeId.equals(xc.getNodeRef().getValue())) {
            return false;
        }
        String sourceTp = xc.getSourceTp().get(0).getTpRef().getValue();
        String destinationTp = xc.getDestinationTp().get(0).getTpRef().getValue();
        return (tpA.equals(sourceTp) && tpZ.equals(destinationTp)) ||
                (tpA.equals(destinationTp) && tpZ.equals(sourceTp));
    }

    private CrossConnections buildInjectXcOnIRA(Available ava, TerminationPoint src, TerminationPoint dst, Equipments eq) {
        CrossConnectionsBuilder xcBuilder = buildBasicInjectXc(ava, src, dst, eq);
        xcBuilder.setWssChannel(createWssInfoOnIRA(ava));
        return xcBuilder.build();
    }

    private CrossConnections buildInjectXcOnDGE(Available ava, TerminationPoint src, TerminationPoint dst, Equipments eq) {
        CrossConnectionsBuilder xcBuilder = buildBasicInjectXc(ava, src, dst, eq);
        xcBuilder.setWssChannel(createWssInfoOnDGE(ava));
//        //宿端功率自动控制门限
//        Properties newProp = PropertyTool.addProperty(xcBuilder.getProperties(), "auto-control-active-threshold-dest", "0.5");
//        //源端功率自动控制门限
//        newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-source", "0.5");
//        //授权的功率自动控制范围
//        newProp = PropertyTool.addProperty(newProp, "auto-control-range", "6");
//        //源端to宿端的功率控制模式
//        newProp = PropertyTool.addProperty(newProp, "source-to-dest-power-control-mode", "MANUAL");
//        newProp = PropertyTool.addProperty(newProp, "dest-to-source-power-control-mode", "MANUAL");
//        newProp = PropertyTool.addProperty(newProp, "ase-control-mode", "DISABLE");
//        xcBuilder.setProperties(newProp);

        return xcBuilder.build();
    }

    private WssChannel createWssInfoOnIRA(Available ava) {
        //配置IRA_CL-1-1的media-channel: EXP33--LINE的所有通道噪声为15
        BigDecimal voa = new BigDecimal(15);

//        //宿端功率自动控制门限
//        Properties newProp = PropertyTool.addProperty(null, "auto-control-active-threshold-dest", "0.5");
//        //授权的功率自动控制范围
//        newProp = PropertyTool.addProperty(newProp, "auto-control-range", "6");
        //源端to宿端的功率控制模式

        Properties newProp = PropertyTool.addProperty(null, "dest-to-source-power-control-mode", "MANUAL");


        //所有新建的假波交叉这个值都是DISABLED
        Properties properties = PropertyTool.addProperty(null, "ase-control-mode", "ASE_DISABLED");

        return new WssChannelBuilder()
                .setLowerFrequency(ava.getLowerFrequency())
                .setUpperFrequency(ava.getUpperFrequency())
                .setSourceToDestVoa(new BigDecimal(15))
                .setDestToSourceVoa(new BigDecimal(0))
                .setProperties(properties)
                .build();
    }

    private WssChannel createWssInfoOnDGE(Available ava) {
        BigDecimal voa = new BigDecimal(3);

        return new WssChannelBuilder()
                .setLowerFrequency(ava.getLowerFrequency())
                .setUpperFrequency(ava.getUpperFrequency())
                .setSourceToDestVoa(voa)
                .setDestToSourceVoa(voa)
                .build();
    }
    //TODO:  假波还需要改光放的line 口属性  配置OTS-1-1-LINE的CHANNEL模型的功率控制模式为APC
//newProp = CommonUtils.addProperty(newProp, "channel-optical-power-adjustment.total-power-stability-condition", "false");
//
    private CrossConnectionsBuilder buildBasicInjectXc(Available ava, TerminationPoint src, TerminationPoint dst, Equipments eq) {
        long lower = ava.getLowerFrequency().getValue().longValue();
        long upper = ava.getUpperFrequency().getValue().longValue();

        String freqSlot = String.format("/frequency=%d,%d", lower, upper);
        long center = lower + (upper - lower) / 2;
        SourceTp xcSrc = new SourceTpBuilder()
                .setTpRef(src.getTpId())
                .setKey(new SourceTpKey(src.getTpId()))
                .setSlot(freqSlot)
                .build();
        List<SourceTp> srcTpList = new ArrayList<>();
        srcTpList.add(xcSrc);

        DestinationTp xcDst = new DestinationTpBuilder()
                .setTpRef(dst.getTpId())
                .setKey(new DestinationTpKey(dst.getTpId()))
                .setSlot(freqSlot)
                .build();
        List<DestinationTp> dstTpList = new ArrayList<>();
        dstTpList.add(xcDst);

        Uri xcId = createXCId(src.getTpId().getValue(), dst.getTpId().getValue(), center);
        return new CrossConnectionsBuilder()
                .setCrossConnectionId(xcId)
                .setKey(new CrossConnectionsKey(xcId))
                .setDescription(String.format("ASE-%s/%d,%d", eq.getFriendlyName(), lower, upper))
                .setNodeRef(new NodeId(PhysicalEqpIdNamingRule.getNodeId(eq.getEquipmentId())))
                .setAdminState(AdminStatus.Down)
                .setImplementState(ImplementState.Allocate)
                .setFixed(false)
                .setDirection(LinkDirection.Bidirection)
                .setDestinationTp(dstTpList)
                .setSourceTp(srcTpList)
                .setProperties(new PropertiesBuilder().setProperty(new ArrayList<>()).build());
    }


    private Uri createXCId(String srcTpId, String dstTpId, long centerFreq) {
        return WssXcIdGenerator.getXcId(changedObject, srcTpId, dstTpId, centerFreq);
    }

    private List<TerminationPoint> getInjectXcTPOnIRA(Node node, Equipments eq) {
        List<TerminationPoint> xcTps = new ArrayList<>();

        List<TerminationPoint> lineList = node.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().contains(eq.getEquipmentId()) &&
                        tp.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1.class).getPhysical().getPortType()
                                .equals(PortType.OALine)).collect(Collectors.toList());

        if (lineList.size() != 1) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find line port on IRA %s", eq.getEquipmentId()));
        }
        List<TerminationPoint> exp33List = node.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().contains("EXP33")).collect(Collectors.toList());

        if (exp33List.size() != 1) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find exp33 port on IRA %s", eq.getEquipmentId()));
        }

        xcTps.add(exp33List.get(0));
        xcTps.add(lineList.get(0));

        return xcTps;
    }

    private List<TerminationPoint> getInjectXcTPOnDGE(Node node, Equipments eq) {
        List<TerminationPoint> xcTps = new ArrayList<>();

        xcTps.addAll(node.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().contains(eq.getEquipmentId()) &&
                        tp.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1.class)
                                .getPhysical().getPortType().name().toUpperCase().contains("ILA")).collect(Collectors.toList()));

        if (xcTps.size() != 2) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find 2 line ports on DGE %s", eq.getEquipmentId()));
        }

        return xcTps;
    }


    private static DateAndTime getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String str = sdf.format(new Date());
        String str1 = str.substring(0, str.length() - 2);
        String str2 = str.substring(str.length() - 2);
        StringBuilder sb = new StringBuilder();
        sb.append(str1);
        sb.append(":");
        sb.append(str2);
        return DateAndTime.getDefaultInstance(sb.toString());
    }

    static class AseXcEndpoint {
        private final TerminationPoint source;
        private final TerminationPoint destination;
        private final Equipments equipment;

        AseXcEndpoint(TerminationPoint source, TerminationPoint destination, Equipments equipment) {
            this.source = source;
            this.destination = destination;
            this.equipment = equipment;
        }

        TerminationPoint getSource() {
            return source;
        }

        TerminationPoint getDestination() {
            return destination;
        }

        Equipments getEquipment() {
            return equipment;
        }
    }

}
