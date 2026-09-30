package net.flex.dci.otn.controller.allocate.network.bytedance.ase;


import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
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
import java.util.stream.Collectors;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class DummyOchLinkConstructor {

    private ChangedObject changedObject;
    private Set<CrossConnectionAttributes> aseXcList;
    private List<SupportingLink> supportingLinks;
    private List<String> otmNodeList;


    public DummyOchLinkConstructor(ChangedObject changedObject, LinkId siteLinkId) {
        this.changedObject = changedObject;
        aseXcList = new HashSet<>();

        supportingLinks = new ArrayList<>();
        supportingLinks.add(new SupportingLinkBuilder().setLinkRef(siteLinkId).build());
    }

    public Link create(String aTp, String zTp, Available frequency, List<String> amplifierXcList, List<String> otmNodeList) {
        this.otmNodeList = otmNodeList;
        long lower = frequency.getLowerFrequency().getValue().longValue();
        long upper = frequency.getUpperFrequency().getValue().longValue();
        long centerFrequency = lower + (upper - lower) / 2;

        String friendlyName = String.format("ASE:%d-%d", lower, upper);
        LinkId linkId = new LinkId(OchLinkIdNamingRule.generateId(String.format("#%d-%d#-%s", lower, upper, aTp), zTp));

        log.debug("start create fake och link at {}--{}", lower, upper);
        Link ochLink = new LinkBuilder()
                .setLinkId(linkId)
                .setKey(new LinkKey(linkId))
                .setSupportingLink(supportingLinks)
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

        insertAxeXcInPhyNode();

        changedObject.addChangedOchLink(ochLink);
        return ochLink;
    }

    private void insertAxeXcInPhyNode() {
        log.info("add ase xc into node");
        aseXcList.forEach(xc -> {
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
        });
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

    private void toAseXc(String amplifierXCId, Available scope) {
        String nodeId = PhysicalXcIdNamingRule.getNodeId(amplifierXCId);
        Node node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Optional<CrossConnections> xcOp = nodeAttr.getCrossConnections().stream().filter(xc -> xc.getCrossConnectionId().getValue().equals(amplifierXCId)).findAny();
        if (!xcOp.isPresent()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find XC %s in node", amplifierXCId));
        }

        CrossConnections xc = xcOp.get();
        String eqId = PhysicalTpIdNamingRule.getEquipId(xc.getSourceTp().get(0).getTpRef().getValue());
        Optional<Equipments> eqOp = nodeAttr.getEquipments().stream().filter(eq -> eq.getEquipmentId().equalsIgnoreCase(eqId)).findAny();
        if (!eqOp.isPresent()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find EQ %s in node", eqId));
        }
        Equipments eq = eqOp.get();
        if (eq.getEquipType().equals(EquipType.ILA)) {
            //inject NOT required.
            return;
        }
        log.debug("create ase xc on {} ({}) and frequency [{},{}]",
                eq.getEquipType(), eq.getEquipmentId(),
                scope.getLowerFrequency().getValue().longValue(), scope.getUpperFrequency().getValue().longValue());

        List<TerminationPoint> tps = new ArrayList<>();
        if (eq.getEquipType().equals(EquipType.IRA)) {
            getInjectXcTPOnIRA(node, eq, tps);
        } else {
            getInjectXcTPOnDGE(node, eq, tps);
        }
        CrossConnections aseXc = buildInjectXc(scope, tps.get(0), tps.get(1), eq);
        aseXcList.add(aseXc);
    }

    private CrossConnections buildInjectXc(Available ava, TerminationPoint src, TerminationPoint dst, Equipments eq) {
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
                .setWssChannel(createWssInfo(ava, eq.getEquipType()))
                .build();
    }

    private WssChannel createWssInfo(Available ava, EquipType equipType) {
        BigDecimal voa = new BigDecimal(15);
        if (equipType.equals(EquipType.DGE)) {
            voa = new BigDecimal(3);
        }
        return new WssChannelBuilder()
                .setLowerFrequency(ava.getLowerFrequency())
                .setUpperFrequency(ava.getUpperFrequency())
                .setSourceToDestVoa(voa)
//                .setProperties(buildProperties(implConfig.getAllDefaultValues(), equipType))
                .build();
    }

    private Uri createXCId(String src, String dst, long centerFreq) {
        List<String> tpIds = new ArrayList<>();
        tpIds.add(String.format("%s/%d", src, centerFreq));
        tpIds.add(String.format("%s/%d", dst, centerFreq));

        return new Uri(tpIds.stream().sorted().collect(Collectors.joining("-", "XC-", "")));
    }

    private void getInjectXcTPOnIRA(Node node, Equipments eq, List<TerminationPoint> xcTps) {
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
    }

    private void getInjectXcTPOnDGE(Node node, Equipments eq, List<TerminationPoint> xcTps) {
        xcTps.addAll(node.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().contains(eq.getEquipmentId()) &&
                        tp.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1.class)
                                .getPhysical().getPortType().name().toUpperCase().contains("ILA")).collect(Collectors.toList()));

        if (xcTps.size() != 2) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find 2 line ports on DGE %s", eq.getEquipmentId()));
        }
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

}
