/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import static net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput.PROTECTED_1TO2;

import com.google.common.collect.ImmutableSet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NoAvailableTpException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.site.LinkService;
import net.flex.dci.otn.controller.allocate.designer.site.model.LinkOutput;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.AllocateOtStartInfo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.OtNewOchNodeInfo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.OtRouteInfo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.OtRouteInfoNewOch;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.PickedOtResource;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.PickedOtTps;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.ExternalLink;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import net.flex.dci.otn.controller.allocate.ne.Port;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RegSiteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OTAllocate {

    public static final String OP6_PORT_SIG = "SIG";
    public static final String OP_CARD_CLASS = "OP";
    public static final String L_PORT_PREFIX = "L";
    public static final String OTU_LINE = "OTU-Line";
    public static final String OTU_CLIENT = "OTU-Client";
    public static final String OTHER = "Other";
    public static final String EXP = "EXP";
    //    public static final String OP6_CARD_TYPE = "OP6";//there is only one card type for OP6

    // 提取到类顶部的常量
    private static final String AUTO_CONTROL_RANGE = "auto-control-range";
    private static final String AUTO_CONTROL_VALUE = "15";
    private static final String SELECTED_ROLE_OT = "OT";
    private static final String SELECTED_ROLE_OP = "OP";


    @Autowired
    private NeNodeRepo neNodeRepo;

    @Autowired
    private EquipmentRepo equipmentRepo;
    @Autowired
    private TpRepo tpRepo;
    @Autowired
    private OtXcService otXcService;

    @Autowired
    private OtNodeService otNodeService;

    @Autowired
    private OlsNodeService olsNodeService;

    @Autowired
    private PhyLinkDao phyLinkDao;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private JsonOutputer jsonOutputer;

    @Autowired
    private TunnelLinkService tunnelLinkService;
    @Autowired
    private TunnelUtils tunnelUtils;
    @Autowired
    private TunnelSiteAllocate tunnelSiteAllocate;


    @Autowired
    private OtReusedStrategy otReusedStrategy;
    @Autowired
    private NodeUtils nodeUtils;
    @Autowired
    private OtTransceiverService otTransceiverService;
    @Autowired
    private LinkService linkService;
    @Autowired
    private NEInfoConfig neInfoConfig;
    @Value("${server.yangModel}")
    private String yangModel;
    @Autowired
    private SiteLinkDao siteLinkDao;

    @Autowired
    private SiteNodeDao siteNodeDao;
    @Autowired
    private RoadmService roadmService;

    @Autowired
    private OchLinkDao ochLinkDao;

    /**
     * 1. 在start这边的site，可以找到利旧的资源，那在end那头的site，也会有同样的利旧资源 2. 如果需要建立OCH，那么cenFrequency在start时就选好，end那头必须和start一样 *** deprecated  ***
     *
     * @param input
     * @param siteId
     * @param siteLink
     * @param neInfo
     * @param reusedNodesInDbSrc
     * @param reusedNodesInMemorySrc
     * @return
     * @throws NeDesignerException
     */
    public AllocateOtStartInfo allocateOtStart(TunnelInput input, String siteId, Link siteLink, NeInfo neInfo, @NonNull List<Node> reusedNodesInDbSrc, List<Node> reusedNodesInMemorySrc)
            throws NeDesignerException {

        Card otCardInfo = neInfo.getCardByCardType(input.getCardType());
        PickedOtResource pickedOtResource = otReusedStrategy
                .getReusedNode(otCardInfo, input.getLineSignalRate(), reusedNodesInDbSrc, reusedNodesInMemorySrc, siteLink.getLinkId()
                        .getValue(), input.getServicetype());

        //当有已存在的L端口，并且L端口已经有OSLink，且L端口还有时隙可用， 此时，不需要创建新的OSLink
        if (pickedOtResource != null && pickedOtResource.getReusedLPort()) {
            //因为不需要建立新的OSLink，所以相关信息就为空。
            OtRouteInfo routeInfo = allocateOt(pickedOtResource, input, neInfo, siteId, null, null, false);
            return AllocateOtStartInfo.builder().otRouteInfo(routeInfo).build();

        } else {//需要建立一条新的OsLink
            //Pick frequency from available
            Available freeFrequency = tunnelUtils.pickFreeFrequency(siteLink, input.getLineSignalRate(), input.getFrequencyAvailable(), otCardInfo);
            if (freeFrequency == null) {
                log.error("There is no free frequency to pick for lineSignalRate: {}, site link: {}", input.getLineSignalRate()
                        .getSimpleName(), siteLink.getLinkId().getValue());
                throw new NeDesignerException("There is no free frequency to pick.");
            }
            log.debug("Pick the frequency: ", freeFrequency);
            String frequencyString = tunnelUtils.getTpSlotFrequencyString(freeFrequency);//e.g. /frequency=196025000,19675000
            BigInteger centFreq = tunnelUtils.getCentFreq(freeFrequency);

            ImmutablePair<String, String> olsPeerTps = tunnelSiteAllocate
                    .pickMuxTpPair(frequencyString, input.getSiteLinkSrcNode(), input.getSiteLinkDestNode());//get free start/end mux tp to connect to OT card L port

            OtRouteInfo routeInfo = allocateOt(pickedOtResource, input, neInfo, siteId, olsPeerTps.getLeft(), centFreq, false);
            return AllocateOtStartInfo.builder().centFreq(centFreq).frequencyString(frequencyString).olsPeerTps(olsPeerTps).otRouteInfo(routeInfo).build();
        }
    }


    private OtRouteInfo allocateOt(PickedOtResource pickedOtResource, TunnelInput input, NeInfo neInfo, String siteId, String olsTp, BigInteger cenFrequency, Boolean isReversed)
            throws NeDesignerException {

        Card otCardInfo = neInfo.getCardByCardType(input.getCardType());
        Card emptyCardInfo = neInfo.getEmptyCard();

        Boolean isStuffed = null;
        if (pickedOtResource != null) {
            PickedOtTps pickedOtTps = pickedOtResource.getPickedOtTps();
            Node reusedNode = pickedOtResource.getNode();

            if (pickedOtTps == null) {//没有可用的TP
                //create card
                Integer newEquipSlot = nodeUtils.pickedAvailableSlot(reusedNode, otCardInfo);
                pickedOtResource = createCard(otCardInfo, emptyCardInfo, reusedNode, newEquipSlot);
                isStuffed = false;

            }
        } else {
            //create new node
            Node newNode = neNodeRepo.createEmptyNode(input.getVendorName(), neInfo.getProductType(), siteId, NodeType.TD, neInfo.getFixEquipModel(), neInfo.getEmptyCard(), input
                            .getPlane(), input.getPlane(),
                    input.getRiskGroupName(), NeSubType.EPC_OTM);
            Integer newEquipSlot = otCardInfo.getPossibleSlot().get(0);
            pickedOtResource = createCard(otCardInfo, emptyCardInfo, newNode, newEquipSlot);
            isStuffed = false;
        }

        //create link, xc, transceiver
        CrossConnections newXc = otXcService.createXC(pickedOtResource, input);
        Link newOlsLink = olsTp == null ? null : tunnelLinkService.createOsLink(pickedOtResource, olsTp, isReversed);
        Node updatedOtNode = otNodeService.updatedOtNode(pickedOtResource, otCardInfo, newOlsLink, newXc, input, cenFrequency, isStuffed);

        return OtRouteInfo.builder()
                .node(updatedOtNode)
                .link(newOlsLink)
                .xc(newXc).build();

    }


    private PickedOtResource createCard(Card otCardInfo, Card emptyCardInfo, Node node, Integer newEquipSlot) throws NeDesignerException {
        List<Equipments> equipments = node.getAugmentation(Node1.class).getPhysical().getEquipments();
        Set<Integer> newEquipSlots = equipmentRepo.getEquipUsedSlots(newEquipSlot, otCardInfo.getWidth(), otCardInfo.getHeight());
        Equipments newEquipment = equipmentRepo.createEquipment(node.getNodeId()
                .getValue(), EquipType.OT, otCardInfo, newEquipSlot, newEquipSlots, true);
        List<Integer> emptyPossibleSlots = new ArrayList<>(emptyCardInfo.getPossibleSlot());
        emptyPossibleSlots.removeAll(newEquipSlots);

        //replace empty card with line card
        boolean isReplaced = false;
        for (int i = equipments.size() - 1; i >= 0; i--) {
            Equipments item = equipments.get(i);
            if (item.getEquipType() == null || item.getEquipType().equals(EquipType.Other)) {
                continue;
            }
            Integer itemSlot = Integer.parseInt(item.getSlot());
            emptyPossibleSlots.remove(itemSlot);

            if (item.getEquipType().equals(EquipType.EMPTY)) {
                if (newEquipSlots.contains(itemSlot)) {
                    if (itemSlot.equals(Integer.parseInt(newEquipment.getSlot()))) {
                        isReplaced = true;
                        equipments.set(i, newEquipment);
                    } else {
                        equipments.remove(i);
                    }
                }
            }
        }
        if (!isReplaced) {
            int index = equipmentRepo.getTrafficCardIndex(emptyCardInfo);
            equipments.add(index, newEquipment);
            log.error("Invalid node:{}, because can not replace empty linecard by new equip:{}, then add it in index:{}.", node.getNodeId()
                    .getValue(), newEquipment.getEquipmentId(), index);
        }

        //create TP
        List<TerminationPoint> tps = new ArrayList<>();
        List<Port> ports = otCardInfo.getPorts();
        ports.sort(Comparator.comparing(Port::getName));
        String ctp = null;
        String ltp = null;
        for (Port port : ports) {
            List<String> portNames = NeInfoUtil.getNameList(port.getName());
            List<String> portIndexs = NeInfoUtil.getNameList(port.getIndex());
            int size = portNames.size();
            for (int i = 0; i < size; i++) {
                String portName = portNames.get(i);
                String portIndex = portIndexs.get(i);
                TerminationPoint newTp = tpRepo.createTp(newEquipment, otCardInfo.getCardType(), portName, port, i, Integer.parseInt(portIndex));
                if (portName.toUpperCase().startsWith("C") && ctp == null) {
                    ctp = newTp.getTpId().getValue();
                }
                if (portName.toUpperCase().startsWith(L_PORT_PREFIX) && ltp == null) {
                    ltp = newTp.getTpId().getValue();
                }
                tps.add(newTp);
            }
        }
        tps.addAll(node.getTerminationPoint());

        //construct node
        Physical physical = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical()).setStuffed(false)
                .setEquipments(equipments)
                .build();//对于创建的新卡，stuff永远是false
        Node1 phyNode = new Node1Builder().setPhysical(physical).build();
        Node newNode = new NodeBuilder().setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .setTerminationPoint(tps)
                .addAugmentation(Node1.class, phyNode)
                .build();

        String lPortLayer = NeInfoUtil.getLPortXcLayers(otCardInfo).get(0);//因为这是张新卡，所以总是根据json配置，按照顺序获取第一个。

        return PickedOtResource.builder().node(newNode).pickedOtTps(PickedOtTps.builder().ctp(ctp).ltp(ltp).lPortSlot(lPortLayer).build()).build();
    }


    public OtRouteInfo allocateOtEnd(TunnelInput input, String siteId, NeInfo neInfo, String olsTpEnd, BigInteger cenFrequency, List<Node> reusedNodesInDbDst, List<Node> reusedNodesInMemoryDst,
                                     String siteLinkId)
            throws NeDesignerException {
        Card otCardInfo = neInfo.getCardByCardType(input.getCardType());
        PickedOtResource pickedOtResource = otReusedStrategy
                .getReusedNode(otCardInfo, input.getLineSignalRate(), reusedNodesInDbDst, reusedNodesInMemoryDst, siteLinkId, input.getServicetype());

        return allocateOt(pickedOtResource, input, neInfo, siteId, olsTpEnd, cenFrequency, true);
    }

    private OtNewOchNodeInfo createNodeWithOtCard(String siteId, NeInfo tpcNeInfo, @NonNull String plane, @NonNull String planeId, @NonNull String riskGroupName, Card otCardInfo,
                                                  SERVICETYPE serviceType)
            throws NeDesignerException {
        //create new empty node
        String vendorName = tpcNeInfo.getVendor();
        String vendorType = tpcNeInfo.getProductType();
        Node newNode = neNodeRepo.createEmptyNode(vendorName, vendorType, siteId, NodeType.TD, tpcNeInfo.getFixEquipModel(), tpcNeInfo.getEmptyCard(), plane, planeId, riskGroupName,
                NeSubType.EPC_OTM);
        Integer newEquipSlot = nodeUtils.pickedAvailableSlot(newNode, otCardInfo);
        Pair<String, Node> lPair = createOtOp6Card(newNode, otCardInfo, tpcNeInfo.getEmptyCard(), EquipType.OT, newEquipSlot, serviceType);

        Boolean isReg = tunnelUtils.isReg(serviceType);
        if (!isReg) {
            return OtNewOchNodeInfo.builder().lTp(lPair.getLeft()).lTpNode(lPair.getRight()).snapshotNode(new ArrayList<>())
                    .build();
        }

        //handle REG
        String cardVendor = otCardInfo.getVendorType();
        String ltp2 = otReusedStrategy.getAnotherLPort(lPair, serviceType, cardVendor);
        newNode = lPair.getRight();
        if (ltp2 == null) {
            newNode = lPair.getRight();
            Integer newEquipSlot2 = nodeUtils.pickedAvailableSlot(newNode, otCardInfo);
            Card emptyCardInfo = tpcNeInfo.getEmptyCard();
            Pair<String, Node> pair2 = createOtOp6Card(newNode, otCardInfo, emptyCardInfo, EquipType.OT, newEquipSlot2, serviceType);
            ltp2 = pair2.getLeft();
            newNode = pair2.getRight();


        }
        return OtNewOchNodeInfo.builder().lTpNode(newNode).lTp(lPair.getLeft()).lTp2(ltp2).snapshotNode(new ArrayList<>()).build();


    }

    private OtNewOchNodeInfo createNodeWithOtOp6Card(String siteId, NeInfo tpcNeInfo, @NonNull String plane, @NonNull String planeId, @NonNull String riskGroupName, Card otCardInfo, Card op6CardInfo,
                                                     SERVICETYPE serviceType)
            throws NeDesignerException {
        //create new empty node
        String vendorName = tpcNeInfo.getVendor();
        String vendorType = tpcNeInfo.getProductType();
        Node newNode = neNodeRepo.createEmptyNode(vendorName, vendorType, siteId, NodeType.TD, tpcNeInfo.getFixEquipModel(), tpcNeInfo.getEmptyCard(), plane, planeId, riskGroupName,
                NeSubType.EPC_OTM);
        Pair<Integer, Integer> availableSlot = otReusedStrategy.getAvailableOpOtSlotPair(newNode, op6CardInfo, otCardInfo, serviceType);
        Pair<String, Node> op6pair = createOtOp6Card(newNode, op6CardInfo, tpcNeInfo.getEmptyCard(), EquipType.OP, availableSlot.getLeft(), serviceType);
        Pair<String, Node> lPair = createOtOp6Card(op6pair.getRight(), otCardInfo, tpcNeInfo.getEmptyCard(), EquipType.OT, availableSlot.getRight(), serviceType);
        return OtNewOchNodeInfo.builder()
                .op6Node(lPair.getRight())
                .op6Tp(op6pair.getLeft())
                .lTp(lPair.getLeft())
                .lTpNode(lPair.getRight())
                .snapshotNode(Collections.emptyList())
                .build();//ltp和op6是同一个node
    }

    private static class SelectedCardRef {
        private final String role;
        private final String cardId;
        private final String siteId;
        private final String nodeId;
        private final Integer slot;

        private SelectedCardRef(String role, String cardId, String siteId, String nodeId, Integer slot) {
            this.role = role;
            this.cardId = cardId;
            this.siteId = siteId;
            this.nodeId = nodeId;
            this.slot = slot;
        }
    }

    private List<SelectedCardRef> getSelectedCards(TunnelNewOchInput input, String siteId,
                                                   String role) throws NeDesignerException {
        List<String> cardIds = input.getSelectedCardIdsBySite() == null
                ? Collections.emptyList() : input.getSelectedCardIdsBySite().get(siteId);
        if (cardIds == null || cardIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SelectedCardRef> selectedCards = new ArrayList<>();
        for (String cardId : cardIds) {
            SelectedCardRef selectedCard = parseSelectedCard(cardId);
            if (role.equals(selectedCard.role)) {
                // failure-rate拆分出的不同group必须隔离TD设备。
                // 用户指定板卡优先级高于自动选板，所以这里必须显式拦截excludeNodes，
                // 否则指定card会绕过TunnelAllocator2传入的failure group排除集合。
                if (input.getExcludeNodes() != null
                        && input.getExcludeNodes().contains(selectedCard.nodeId)) {
                    throw new NeDesignerException("Selected card is in excluded failure group node: "
                            + selectedCard.cardId);
                }
                selectedCards.add(selectedCard);
            }
        }
        return selectedCards;
    }

    private SelectedCardRef parseSelectedCard(String value) throws NeDesignerException {
        String role = SELECTED_ROLE_OT;
        String cardId = value;
        int roleIndex = value.indexOf(":");
        if (roleIndex > 0) {
            role = value.substring(0, roleIndex).toUpperCase();
            cardId = value.substring(roleIndex + 1);
        }
        if (!SELECTED_ROLE_OT.equals(role) && !SELECTED_ROLE_OP.equals(role)) {
            throw new NeDesignerException("Invalid selected card role: " + value);
        }
        String[] parts = cardId.split("#");
        if (parts.length < 3) {
            throw new NeDesignerException("Invalid selected card-id: " + value);
        }
        String nodeId = parts[0] + "#" + parts[1];
        String cardPart = parts[2];
        Integer slot = parseSelectedCardSlot(cardPart, value);
        return new SelectedCardRef(role, cardId, parts[0], nodeId, slot);
    }

    private Integer parseSelectedCardSlot(String cardPart, String original) throws NeDesignerException {
        String[] cardParts = cardPart.split("-");
        if (cardParts.length < 3) {
            throw new NeDesignerException("Invalid selected card-id: " + original);
        }
        try {
            return Integer.valueOf(cardParts[cardParts.length - 1]);
        } catch (NumberFormatException e) {
            throw new NeDesignerException("Invalid selected card slot: " + original, e);
        }
    }

    private boolean hasSelectedCards(TunnelNewOchInput input, String siteId) {
        return input.getSelectedCardIdsBySite() != null
                && input.getSelectedCardIdsBySite().containsKey(siteId)
                && input.getSelectedCardIdsBySite().get(siteId) != null
                && !input.getSelectedCardIdsBySite().get(siteId).isEmpty();
    }

    private Node getSelectedNode(SelectedCardRef selectedCard, Map<String, Node> inMemoryNode)
            throws NeDesignerException {
        Node node = inMemoryNode.get(selectedCard.nodeId);
        if (node != null) {
            return node;
        }
        node = phyNodeDao.getConfigPhyNodeById(selectedCard.nodeId);
        if (node == null) {
            throw new NeDesignerException("Selected NE does not exist: " + selectedCard.nodeId);
        }
        if (!node.getNodeId().getValue().contains(selectedCard.siteId)) {
            throw new NeDesignerException("Selected card crosses site: " + selectedCard.cardId);
        }
        return node;
    }

    private void addSnapshotIfNeeded(List<Node> snapshots, Node node, Map<String, Node> inMemoryNode) {
        String nodeId = node.getNodeId().getValue();
        if (inMemoryNode.containsKey(nodeId)) {
            return;
        }
        boolean exists = snapshots.stream().anyMatch(snapshot -> snapshot.getNodeId().equals(node.getNodeId()));
        if (!exists) {
            snapshots.add(nodeUtils.getNodeCopy(node));
        }
    }

    private Pair<String, Node> pickOrCreateSelectedCard(SelectedCardRef selectedCard, Card cardInfo,
                                                        Card emptyCardInfo, EquipType equipType,
                                                        Map<String, Node> inMemoryNode,
                                                        List<Node> snapshots,
                                                        SERVICETYPE serviceType)
            throws NeDesignerException {
        Node node = getSelectedNode(selectedCard, inMemoryNode);
        Optional<Equipments> existingEquipment = node.getAugmentation(Node1.class).getPhysical()
                .getEquipments().stream()
                .filter(equipment -> selectedCard.cardId.equals(equipment.getEquipmentId()))
                .findAny();
        if (existingEquipment.isPresent() && !EquipType.EMPTY.equals(existingEquipment.get().getEquipType())) {
            if (!equipType.equals(existingEquipment.get().getEquipType())) {
                throw new NeDesignerException("Selected card type is not " + equipType + ": " + selectedCard.cardId);
            }
            String idleTp = pickIdleTpOnCard(node, selectedCard.cardId, equipType);
            if (idleTp == null) {
                throw new NeDesignerException("Selected card has no idle " + equipType + " port: " + selectedCard.cardId);
            }
            addSnapshotIfNeeded(snapshots, node, inMemoryNode);
            return Pair.of(idleTp, node);
        }

        if (existingEquipment.isPresent() && !EquipType.EMPTY.equals(existingEquipment.get().getEquipType())) {
            throw new NeDesignerException("Selected slot is not empty: " + selectedCard.cardId);
        }
        if (!nodeUtils.getEmptySlot(node).contains(selectedCard.slot)) {
            throw new NeDesignerException("Selected slot is not available: " + selectedCard.cardId);
        }
        addSnapshotIfNeeded(snapshots, node, inMemoryNode);
        return createOtOp6Card(node, cardInfo, emptyCardInfo, equipType, selectedCard.slot, serviceType);
    }

    private String pickIdleTpOnCard(Node node, String cardId, EquipType equipType) {
        PortType portType = EquipType.OP.equals(equipType) ? PortType.OPSig : PortType.OTULine;
        return node.getTerminationPoint().stream()
                .filter(tp -> {
                    return cardId.equals(tp.getAugmentation(TerminationPoint1.class).getPhysical()
                            .getEquipmentRef())
                            && portType.equals(tp.getAugmentation(TerminationPoint1.class)
                            .getPhysical().getPortType())
                            && ConnectionStatus.Idle.equals(tp.getAugmentation(TerminationPoint1.class)
                            .getPhysical().getConnectionStatus());
                })
                .map(tp -> tp.getTpId().getValue())
                .sorted()
                .findFirst()
                .orElse(null);
    }

    private OtNewOchNodeInfo getUserDefinedOtNewOchNodeInfo(String siteId, TunnelNewOchInput input,
                                                            NeInfo tpcNeInfo,
                                                            Map<String, Node> inMemoryNode,
                                                            Card otCardInfo,
                                                            SERVICETYPE serviceType)
            throws NeDesignerException {
        // Shared by unprotected A/Z and REG allocation. REG may appear on primary, secondary, or third routes.
        List<SelectedCardRef> selectedOtCards = getSelectedCards(input, siteId, SELECTED_ROLE_OT);
        if (selectedOtCards.isEmpty()) {
            return null;
        }
        Card emptyCardInfo = tpcNeInfo.getEmptyCard();
        List<Node> snapshots = new ArrayList<>();
        Pair<String, Node> firstLPort = pickOrCreateSelectedCard(selectedOtCards.get(0), otCardInfo,
                emptyCardInfo, EquipType.OT, inMemoryNode, snapshots, serviceType);

        if (!tunnelUtils.isReg(serviceType)) {
            return OtNewOchNodeInfo.builder()
                    .lTp(firstLPort.getLeft())
                    .lTpNode(firstLPort.getRight())
                    .snapshotNode(snapshots)
                    .build();
        }

        String cardVendor = otCardInfo.getVendorType();
        String lTp2 = otReusedStrategy.getAnotherLPort(firstLPort, serviceType, cardVendor);
        Node lTp2Node = firstLPort.getRight();
        if (lTp2 == null) {
            for (int i = 1; i < selectedOtCards.size(); i++) {
                Pair<String, Node> secondLPort = pickOrCreateSelectedCard(selectedOtCards.get(i),
                        otCardInfo, emptyCardInfo, EquipType.OT, inMemoryNode, snapshots, serviceType);
                lTp2 = secondLPort.getLeft();
                lTp2Node = secondLPort.getRight();
                break;
            }
        }
        if (lTp2 == null) {
            throw new NeDesignerException("Selected REG cards cannot provide two idle L ports for site: " + siteId);
        }
        return OtNewOchNodeInfo.builder()
                .lTp(firstLPort.getLeft())
                .lTpNode(firstLPort.getRight())
                .lTp2(lTp2)
                .lTp2Node(lTp2Node)
                .snapshotNode(snapshots)
                .build();
    }

    // Primary here means the protected endpoint OP/OT primary-side resource, not only the primary route.
    private OtNewOchNodeInfo getUserDefinedNodePrimary(String siteId, TunnelNewOchInput input,
                                                       NeInfo tpcNeInfo,
                                                       Map<String, Node> inMemoryNode,
                                                       Card opCardInfo,
                                                       Card otCardInfo)
            throws NeDesignerException {
        if (!hasSelectedCards(input, siteId)) {
            return null;
        }
        Card emptyCardInfo = tpcNeInfo.getEmptyCard();
        List<Node> snapshots = new ArrayList<>();
        List<SelectedCardRef> selectedOtCards = getSelectedCards(input, siteId, SELECTED_ROLE_OT);
        List<SelectedCardRef> selectedOpCards = getSelectedCards(input, siteId, SELECTED_ROLE_OP);
        if (selectedOtCards.isEmpty() && selectedOpCards.isEmpty()) {
            return null;
        }

        if (selectedOpCards.isEmpty()) {
            return getUserDefinedNodePrimaryWithSelectedOtOnly(selectedOtCards.get(0), input,
                    inMemoryNode, opCardInfo, otCardInfo, emptyCardInfo, snapshots);
        }

        if (selectedOtCards.isEmpty()) {
            return getUserDefinedNodePrimaryWithSelectedOpOnly(selectedOpCards.get(0), input,
                    inMemoryNode, opCardInfo, otCardInfo, emptyCardInfo, snapshots);
        }

        Pair<String, Node> opPair = pickOrCreateSelectedCard(selectedOpCards.get(0), opCardInfo,
                emptyCardInfo, EquipType.OP, inMemoryNode, snapshots, input.getServiceType());
        Pair<String, Node> otPair = getSelectedPrimaryLPort(selectedOtCards.get(0), input,
                inMemoryNode, otCardInfo, emptyCardInfo, snapshots);
        return buildPrimaryNodeInfo(opPair, otPair, snapshots);
    }

    private OtNewOchNodeInfo getUserDefinedNodePrimaryWithSelectedOtOnly(SelectedCardRef selectedOtCard,
                                                                         TunnelNewOchInput input,
                                                                         Map<String, Node> inMemoryNode,
                                                                         Card opCardInfo,
                                                                         Card otCardInfo,
                                                                         Card emptyCardInfo,
                                                                         List<Node> snapshots)
            throws NeDesignerException {
        Pair<String, Node> otPair = getSelectedPrimaryLPort(selectedOtCard, input, inMemoryNode,
                otCardInfo, emptyCardInfo, snapshots);
        Pair<String, Node> opPair = pickOrCreateAutoCardOnNode(selectedOtCard.nodeId, opCardInfo,
                emptyCardInfo, EquipType.OP, inMemoryNode, snapshots, input.getServiceType());
        return buildPrimaryNodeInfo(opPair, otPair, snapshots);
    }

    private OtNewOchNodeInfo getUserDefinedNodePrimaryWithSelectedOpOnly(SelectedCardRef selectedOpCard,
                                                                         TunnelNewOchInput input,
                                                                         Map<String, Node> inMemoryNode,
                                                                         Card opCardInfo,
                                                                         Card otCardInfo,
                                                                         Card emptyCardInfo,
                                                                         List<Node> snapshots)
            throws NeDesignerException {
        Pair<String, Node> opPair = pickOrCreateSelectedCard(selectedOpCard, opCardInfo,
                emptyCardInfo, EquipType.OP, inMemoryNode, snapshots, input.getServiceType());
        Pair<String, Node> otPair = pickOrCreateAutoCardOnNode(selectedOpCard.nodeId, otCardInfo,
                emptyCardInfo, EquipType.OT, inMemoryNode, snapshots, input.getServiceType());
        return buildPrimaryNodeInfo(opPair, otPair, snapshots);
    }

    private Pair<String, Node> getSelectedPrimaryLPort(SelectedCardRef selectedOtCard,
                                                       TunnelNewOchInput input,
                                                       Map<String, Node> inMemoryNode,
                                                       Card otCardInfo,
                                                       Card emptyCardInfo,
                                                       List<Node> snapshots)
            throws NeDesignerException {
        return pickOrCreateSelectedCard(selectedOtCard, otCardInfo, emptyCardInfo, EquipType.OT,
                inMemoryNode, snapshots, input.getServiceType());
    }

    private OtNewOchNodeInfo buildPrimaryNodeInfo(Pair<String, Node> opPair,
                                                  Pair<String, Node> otPair,
                                                  List<Node> snapshots) {
        return OtNewOchNodeInfo.builder()
                .op6Tp(opPair.getLeft())
                .op6Node(opPair.getRight())
                .lTp(otPair.getLeft())
                .lTpNode(otPair.getRight())
                .snapshotNode(snapshots)
                .build();
    }

    private Pair<String, Node> pickOrCreateAutoCardOnNode(String nodeId, Card cardInfo,
                                                          Card emptyCardInfo, EquipType equipType,
                                                          Map<String, Node> inMemoryNode,
                                                          List<Node> snapshots,
                                                          SERVICETYPE serviceType)
            throws NeDesignerException {
        Node node = inMemoryNode.containsKey(nodeId) ? inMemoryNode.get(nodeId)
                : phyNodeDao.getConfigPhyNodeById(nodeId);
        if (node == null) {
            throw new NeDesignerException("Selected NE does not exist: " + nodeId);
        }
        Optional<Equipments> existingEquipment = node.getAugmentation(Node1.class).getPhysical()
                .getEquipments().stream()
                .filter(equipment -> equipType.equals(equipment.getEquipType()))
                .findFirst();
        if (existingEquipment.isPresent()) {
            String idleTp = pickIdleTpOnCard(node, existingEquipment.get().getEquipmentId(), equipType);
            if (idleTp != null) {
                addSnapshotIfNeeded(snapshots, node, inMemoryNode);
                return Pair.of(idleTp, node);
            }
        }
        Integer slot = nodeUtils.pickedAvailableSlot(node, cardInfo);
        addSnapshotIfNeeded(snapshots, node, inMemoryNode);
        return createOtOp6Card(node, cardInfo, emptyCardInfo, equipType, slot, serviceType);
    }

    private Pair<String, Node> createOtOp6Card(Node reusedNode, Card cardInfo, Card emptyCardInfo, EquipType equipType, Integer newEquipSlot, SERVICETYPE serviceType) throws NeDesignerException {
        List<Equipments> oldEquipments = reusedNode.getAugmentation(Node1.class).getPhysical().getEquipments();
//        Integer newEquipSlot = nodeUtils.pickedAvailableSlot(reusedNode, cardInfo);
        Set<Integer> newEquipSlots = equipmentRepo.getEquipUsedSlots(newEquipSlot, cardInfo.getWidth(), cardInfo.getHeight());
        Equipments newEquipment = equipmentRepo.createEquipment(reusedNode.getNodeId()
                        .getValue(), equipType, cardInfo, newEquipSlot, newEquipSlots,
                true);
        if (newEquipment.getEquipType().equals(EquipType.OT)) {
            newEquipment = new EquipmentsBuilder(newEquipment).setServiceType(serviceType).build();
        }
        List<Equipments> newEquipments = new ArrayList<>();
        newEquipments.addAll(oldEquipments);

        //replace empty card with line card
        boolean isReplaced = false;
        boolean isStuffed = true;
        for (int i = newEquipments.size() - 1; i >= 0; i--) {
            Equipments item = newEquipments.get(i);
            if (!item.getEquipType().equals(EquipType.EMPTY)) {
                continue;
            }
            Integer itemSlot = Integer.parseInt(item.getSlot());

            if (newEquipSlots.contains(itemSlot)) {
                if (itemSlot.equals(Integer.parseInt(newEquipment.getSlot()))) {
                    isReplaced = true;
                    newEquipments.set(i, newEquipment);
                } else {
                    newEquipments.remove(i);
                }
            } else {
                isStuffed = false;//还有empty card
            }

        }
        if (!isReplaced) {
            int index = equipmentRepo.getTrafficCardIndex(emptyCardInfo);
            newEquipments.add(index, newEquipment);
            log.error("Invalid reusedNode:{}, because can not replace empty lineCard by new equip:{}, then add it in index:{}.", reusedNode.getNodeId()
                            .getValue(), newEquipment.getEquipmentId(),
                    index);
        }

        //create TP
        List<TerminationPoint> tps = new ArrayList<>();
        List<Port> ports = cardInfo.getPorts();
        ports.sort(Comparator.comparing(Port::getName));

        Boolean isOP6Card = equipType.equals(EquipType.OP);
        String pickedTp = null;//L port TP for OT card, SIG TP for op6 card

        for (Port port : ports) {
            List<String> portNames = NeInfoUtil.getNameList(port.getName());
            List<String> portIndexs = port.getIndex() != null ? NeInfoUtil.getNameList(port.getIndex()) : Collections.EMPTY_LIST;
            int size = portNames.size();
            Map<String, String> lPortNames = null;
            Map<String, String> cPortNames = null;
            if (port.getPortType().equals(OTHER) && lPortNames == null) {
                lPortNames = NeInfoUtil.getLPortNameMappingByServiceType(cardInfo, serviceType);
                if (lPortNames.isEmpty()) {
                    throw new NeDesignerException("Failed to get L port definition for card: " + cardInfo.getCardType() + ",serviceType" + serviceType);
                }
                cPortNames = NeInfoUtil.getCPortNameMappingByServiceType(cardInfo, serviceType);
//                if (cPortNames.isEmpty()) {
//                    throw new NeDesignerException("Failed to get C port definition for card: " + cardInfo.getCardType() + ",serviceType" + serviceType);
//                }
            }
            for (int i = 0; i < size; i++) {
                String portName = portNames.get(i);
                TerminationPoint newTp;
                //For sort of L3X8C7, card type is flex and decided by serviceType
                if (port.getPortType().equals(OTHER)) {
                    if (lPortNames.keySet().contains(portName)) {
                        port.setPortType(OTU_LINE);
                        port.setFriendlyName(lPortNames.get(portName));
                        port.setIndex(String.valueOf(i));
                    } else if (cPortNames.keySet().contains(portName)) {
                        port.setPortType(OTU_CLIENT);
                        port.setFriendlyName(cPortNames.get(portName));
                        port.setIndex(String.valueOf(i + 20));//因为排序希望C口在L口后面
                    } else {
                        port.setIndex(String.valueOf(i + 40));//因为排序希望不用的端口最后
                    }
                    newTp = tpRepo.createTp(newEquipment, cardInfo.getCardType(), portName, port, i, Integer.parseInt(port.getIndex()));

                    //因为单例，用了记得复原
                    port.setPortType(OTHER);
                    port.setFriendlyName(null);
                    port.setIndex(null);
                } else {
                    newTp = tpRepo.createTp(newEquipment, cardInfo.getCardType(), portName, port, i, Integer.parseInt(portIndexs.get(i)));
                }

                if (pickedTp == null) {
                    PortType tpPortType = nodeUtils.getTpPortType(newTp);
                    if (isOP6Card) {
                        if (tpPortType.equals(PortType.OPSig)) {
                            pickedTp = newTp.getTpId().getValue();
                        }
                    } else {
                        if (tpPortType.equals(PortType.OTULine)) {
                            pickedTp = newTp.getTpId().getValue();
                        }
                    }
                }
                tps.add(newTp);
            }
        }
        tps.addAll(reusedNode.getTerminationPoint());

        //construct reusedNode
        Physical physical = new PhysicalBuilder(reusedNode.getAugmentation(Node1.class).getPhysical()).setStuffed(isStuffed)
                .setEquipments(newEquipments)
                .build();//对于创建的新卡，stuff永远是false
        Node1 phyNode = new Node1Builder().setPhysical(physical).build();
        Node newNode = new NodeBuilder().setNodeId(reusedNode.getNodeId())
                .setKey(new NodeKey(reusedNode.getNodeId()))
                .setTerminationPoint(tps)
                .addAugmentation(Node1.class, phyNode)
                .build();
//        newNode = nodeUtils.sortTp(newNode);

        return Pair.of(pickedTp, newNode);

    }

    /**
     * Note：因为默认OP6和3张OT卡应该占满同一个node里，只有在reuse mandatory node时，才需要着两张卡分别在2个node上。 所以整个策略都是，尽量让op6和OT卡在一个node上。
     * <p>
     * 以下场景按照顺序，优先选择
     * <p>
     * Scenario1 同一个node有可重用的OP6和L口
     * <p>
     * Scenario2 同一个node有可重用的OP6口，有槽位可以创建OT卡，则创建OT卡
     * <p>
     * Scenario3 同一个node有可重用的L口，有槽位可以创建OP6卡，则创建OP6卡
     * <p>
     * Scenario4 同一个node，有足够个槽位可以创建OP卡和OT卡，则创建OT卡，OP6卡
     * <p>
     * Scenario5 不同的node，存在可重用的L口和 OP6口(只有mandatory场景采用)
     * <p>
     * Scenario6 只存在op6或者L口，或者不存在可重用的，都返回null。 因为默认是需要OP6和OT卡在同一个node上，这种OP6和OT在不同node的场景只允许相关node都出现在Mandatory node中。
     */
    private OtNewOchNodeInfo getReusedNodePrimary(Map<String, Node> inMemoryNode, Collection<Node> reUsedNodes, String siteId, Card op6CardInfo, Card otCardInfo, Card emptyCardInfo,
                                                  Boolean isMandatory, SERVICETYPE serviceType, String protectionType, Set<String> excludeNodes)
            throws NeDesignerException {

        if (reUsedNodes == null || reUsedNodes.isEmpty()) {
            return null;
        }

        List<Node> nodeSnapshots = new ArrayList<>();

        List<Node> op6UnstuffedNodes = new ArrayList<>();//可重用op口，需要创建ot card
        List<Node> unstuffedNodes = new ArrayList<>();//可以创建一张OP6卡和一张OT卡

        //Scenario1: 最佳场景，都可以重用
        // Protection allocation must use the same site-local rack ordering as non-protection/REG.
        // This keeps repeated allocation from depending on HashMap or Mongo return order.
        List<Node> sortedNodes = sortNodesByRackLocation(siteId, reUsedNodes);
        for (Node node : sortedNodes) {
            String nodeId = node.getNodeId().getValue();
            if (!nodeId.contains(siteId)) {
                continue;//不是这个site的
            }
            if (excludeNodes.contains(nodeId)) {
                continue;
            }
            if (inMemoryNode.containsKey(nodeId)) {
                node = inMemoryNode.get(nodeId);//此node已经在内存中了，则node已被改变，获取内存中最新的node
            }
            boolean isStuffed = nodeUtils.isStuffed(node);

            List<String> op6SigTpIds = nodeUtils.getTps(node).stream().filter(tp ->
                            tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPSig)
                                    && tp.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().equals(ConnectionStatus.Idle))
                    .map(tp -> tp.getTpId().getValue())
                    .sorted()
                    .collect(Collectors.toList());
            if (!op6SigTpIds.isEmpty()) {
                Pair<String, String> op6LinePair = otReusedStrategy.getReusedOp6LPortPair(node, op6CardInfo, otCardInfo, serviceType, op6SigTpIds);
                if (op6LinePair != null) {
                    if (!inMemoryNode.containsKey(nodeId)) {
                        nodeSnapshots.add(nodeUtils.getNodeCopy(node));
                    }
                    return OtNewOchNodeInfo.builder()
                            .op6Node(node)
                            .op6Tp(op6LinePair.getLeft())
                            .lTp(op6LinePair.getRight())
                            .lTpNode(node)
                            .snapshotNode(nodeSnapshots)
                            .build();
                }
                //为其他场景的op6做准备
                if (!isStuffed) {
                    op6UnstuffedNodes.add(node);
                    unstuffedNodes.add(node);
                    continue;
                }
            } else {
                if (!isStuffed) {
                    unstuffedNodes.add(node);
                }
            }

        }

        //Scenario2: 重用op6卡，并且在同一个node上面创建OT卡
        for (Node node : op6UnstuffedNodes) {
            List<String> op6SigTpIds = nodeUtils.getTps(node).stream().filter(tp ->
                            tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPSig)
                                    && tp.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().equals(ConnectionStatus.Idle))
                    .map(tp -> tp.getTpId().getValue())
                    .sorted()
                    .collect(Collectors.toList());
            for (String op6SigTpId : op6SigTpIds) {
                List<Integer> sortedPossibleOtSlots = otReusedStrategy.getPossibleOtSlotsSorted(op6SigTpId, otCardInfo, serviceType);
                Integer newEquipSlot = nodeUtils.pickedAvailableSlot(node, sortedPossibleOtSlots, otCardInfo.getWidth());
                if (newEquipSlot == null) {
                    continue;
                }

                Pair<String, Node> lportPair = createOtOp6Card(node, otCardInfo, emptyCardInfo, EquipType.OT, newEquipSlot, serviceType);
                if (!inMemoryNode.containsKey(node.getNodeId().getValue())) {
                    nodeSnapshots.add(nodeUtils.getNodeCopy(node));
                }
                return OtNewOchNodeInfo.builder()
                        .op6Node(lportPair.getRight())
                        .lTp(lportPair.getLeft())
                        .op6Tp(op6SigTpId)
                        .lTpNode(lportPair.getRight())
                        .snapshotNode(nodeSnapshots)
                        .build();//此时LTP和op6TP是同一个node，选用ltpPair中的node，是因为此node已经被改变


            }
        }

        //Scenario4: 选取有匹配的空槽位的node，创建OT,OP6卡
        for (Node node : unstuffedNodes) {

            Pair<Integer, Integer> availableSlot = otReusedStrategy.getAvailableOpOtSlotPair(node, op6CardInfo, otCardInfo, serviceType);
            if (availableSlot != null) {
                if (!inMemoryNode.containsKey(node.getNodeId().getValue())) {
                    nodeSnapshots.add(nodeUtils.getNodeCopy(node));
                }

                Pair<String, Node> op6pair = createOtOp6Card(node, op6CardInfo, emptyCardInfo, EquipType.OP, availableSlot.getLeft(), serviceType);
                Pair<String, Node> lPair = createOtOp6Card(op6pair.getRight(), otCardInfo, emptyCardInfo, EquipType.OT, availableSlot.getRight(), serviceType);
                return OtNewOchNodeInfo.builder().op6Node(lPair.getRight()).lTp(lPair.getLeft()).op6Tp(op6pair.getLeft()).lTpNode(lPair.getRight())
                        .snapshotNode(nodeSnapshots)
                        .build();//此时LTP和op6TP是同一个node，选用lPair中的node，是因为此node已经被改变
            }

        }
        //对于正常场景，只要不能reuse同一个node（op+ot），就返回null

        if (!isMandatory) {
            return null;//正常的场景，op6必须和OT在一个node上
        } else {
            throw new NeDesignerException("Even isMandatory, not support ot/op on different node.");
        }

    }

    /**
     * 非保护场景，获取可重用的node。
     * <p>
     * Scenario1: 有可重用的L口
     * <p>
     * Scenario2: 有空槽位，创建OT卡
     * <p>
     * Scenario3: 没有可重用的，返回null
     */
    private OtNewOchNodeInfo getReusedNode(String siteId, Card otCardInfo, Card emptyCardInfo, Collection<Node> nodes, Map<String, Node> inMemoryNode, SERVICETYPE serviceType,
                                           Set<String> excludeNodes)
            throws NeDesignerException {
        List<Node> snapshotNode = new ArrayList<>();
        List<Node> unStuffedNodes = new ArrayList<>();
        Boolean isReg = tunnelUtils.isReg(serviceType);
        String cardVendor = otCardInfo.getVendorType();
        List<Node> sortedNodes = sortNodesByRackLocation(siteId, nodes);
        for (Node node : sortedNodes) {
            if (!node.getNodeId().getValue().contains(siteId)) {//不是这个site的
                continue;
            }
            if (excludeNodes.contains(node.getNodeId().getValue())) {
                continue;
            }
            if (inMemoryNode.containsKey(node.getNodeId().getValue())) {
                node = inMemoryNode.get(node.getNodeId().getValue());
            }
            Pair<String, Node> pair = otReusedStrategy.getReusedLport(node, cardVendor, serviceType);
            if (pair != null) {
                String ltp2 = null;
                if (isReg) {
                    ltp2 = otReusedStrategy.getAnotherLPort(pair, serviceType, cardVendor);
                }
                if ((isReg && ltp2 != null) || !isReg) {
                    if (!inMemoryNode.containsKey(pair.getRight().getNodeId().getValue())) {
                        snapshotNode.add(new NodeBuilder(pair.getRight()).build());
                    }
                    return OtNewOchNodeInfo.builder().lTpNode(pair.getRight()).lTp(pair.getLeft()).lTp2(ltp2).snapshotNode(snapshotNode).build();
                }
            }
            if (!nodeUtils.isStuffed(node)) {
                if (!isReg || (isReg && isRegUnsuffedNode(node, otCardInfo, serviceType))) {
                    unStuffedNodes.add(node);
                }
            }
        }

        for (Node unStuffedNode : unStuffedNodes) {
            if (!inMemoryNode.containsKey(unStuffedNode.getNodeId().getValue())) {
                snapshotNode.add(new NodeBuilder(unStuffedNode).build());
            }
            try {
                Integer newEquipSlot = nodeUtils.pickedAvailableSlot(unStuffedNode, otCardInfo);
                Pair<String, Node> pair = createOtOp6Card(unStuffedNode, otCardInfo, emptyCardInfo, EquipType.OT, newEquipSlot, serviceType);
                String lTp = pair.getLeft();
                Node reusedNode = pair.getRight();
                String lTp2 = null;
                if (isReg) {
                    int lPortRegCardSize = NeInfoUtil.getLPortNameSizeReg(otCardInfo, serviceType);
                    if (lPortRegCardSize == 1) {
                        Integer newEquipSlot2 = nodeUtils.pickedAvailableSlot(pair.getRight(), otCardInfo);
                        Pair<String, Node> pair2 = createOtOp6Card(pair.getRight(), otCardInfo, emptyCardInfo, EquipType.OT, newEquipSlot2, serviceType);
                        lTp2 = pair2.getLeft();
                        reusedNode = pair2.getRight();

                    } else {
                        lTp2 = otReusedStrategy.getAnotherLPort(pair, serviceType, cardVendor);
                    }
                }
                return OtNewOchNodeInfo.builder().lTpNode(reusedNode).lTp(lTp).lTp2(lTp2).snapshotNode(snapshotNode).build();
            } catch (NeDesignerException e) {
                log.debug("unStuffedNode:{} not  available, because:{}", unStuffedNode.getNodeId().getValue(), e.getMessage());
                NodeId unStuffedNodeId = unStuffedNode.getNodeId();
                snapshotNode.removeIf(node ->
                        node.getNodeId().equals(unStuffedNodeId)
                );
            }
        }

        return null;
    }

    /**
     * TD候选网元先按siteNode中的rack location从大到小选择，保持A/Z端和REG站点尽量使用
     * 相同rack顺序。后续L口复用、空槽位检查仍按原逻辑执行；这里只调整候选顺序。
     */
    private List<Node> sortNodesByRackLocation(String siteId, Collection<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> locationByNode = getRackLocationByNode(siteId);
        List<Node> sortedNodes = new ArrayList<>(nodes);
        sortedNodes.sort(Comparator
                .comparing((Node node) -> locationByNode.getOrDefault(node.getNodeId().getValue(),
                        Integer.MIN_VALUE))
                .reversed()
                .thenComparing(node -> node.getNodeId().getValue()));
        if (!locationByNode.isEmpty()) {
            log.debug("sorted candidate TD nodes by rack location on site {}, nodes:{}",
                    siteId, sortedNodes.stream()
                            .map(node -> node.getNodeId().getValue() + "="
                                    + locationByNode.getOrDefault(node.getNodeId().getValue(),
                                    Integer.MIN_VALUE))
                            .collect(Collectors.toList()));
        }
        return sortedNodes;
    }

    private Map<String, Integer> getRackLocationByNode(String siteId) {
        Map<String, Integer> locationByNode = new HashMap<>();
        // Keep allocation usable in focused/unit builders that do not provide the optional
        // rack-ordering DAO. Rack location only affects candidate preference, not correctness.
        if (siteNodeDao == null) {
            return locationByNode;
        }
        Node siteNode = siteNodeDao.getSiteNodeById(siteId);
        if (siteNode == null) {
            return locationByNode;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 siteNodeAttr =
                siteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        if (siteNodeAttr == null || siteNodeAttr.getSite() == null
                || siteNodeAttr.getSite().getSupportingRack() == null) {
            return locationByNode;
        }
        for (SupportingRack rack : siteNodeAttr.getSite().getSupportingRack()) {
            if (rack.getSupportingNe() == null) {
                continue;
            }
            for (SupportingNe ne : rack.getSupportingNe()) {
                try {
                    String nodeId = ne.getNodeRef().getValue();
                    Integer location = Integer.parseInt(ne.getLocation());
                    locationByNode.merge(nodeId, location, Math::max);
                } catch (Exception e) {
                    log.debug("ignore invalid rack location on site {}, supportingNe:{}", siteId, ne);
                }
            }
        }
        return locationByNode;
    }

    private boolean isRegUnsuffedNode(Node unStuffNode, Card otCardInfo, SERVICETYPE serviceType) throws NeDesignerException {
        // 获取REG端口数量配置
        int regPortCount = NeInfoUtil.getLPortNameSizeReg(otCardInfo, serviceType);

        // 获取节点空槽位 & 卡片可用槽位
        Set<Integer> emptySlots = nodeUtils.getEmptySlot(unStuffNode);
        List<Integer> possibleSlots = otCardInfo.getPossibleSlot();

        // 计算【实际可用槽位数量】（交集大小）
        long availableSlotCount = possibleSlots.stream()
                .filter(emptySlots::contains)
                .count();

        // 规则1：无可用槽位 → 直接不满足
        if (availableSlotCount < 1) {
            return false;
        }

        // 规则2：REG需要双端口，端口数=1时必须 ≥2个可用槽位
        if (regPortCount == 1) {
            return availableSlotCount > 1;
        }

        // 其他情况均满足条件
        return true;
    }

    public OtRouteInfoNewOch allocateOtStartNewOchPrimary(Integer tunnelToCreate, String srcSiteId, TunnelNewOchInput input, Long cenFrequency, NeInfo
                                                                  tpcNeInfo, Map<String, Node> inMemoryNode,
                                                          String frequencyString, Node srcSiteLinkNode, Link siteLink) throws NeDesignerException {
        return allocateOtNewOchPrimary(tunnelToCreate, srcSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, srcSiteLinkNode,
                siteLink, input.getClientMediumA());
    }

    /**
     * 1.创建tcp node上的交叉 2 创建os Link 3，更新TPC node note： 这个方法改变的node只有tpcNode，至于opcNode，放到下一步，复用段的allocate去做。
     *
     * @param tunnelToCreate
     * @param srcSiteId
     * @param input
     * @param cenFrequency
     * @param tpcNeInfo
     * @param inMemoryNode
     * @param frequencyString
     * @return
     * @throws NeDesignerException
     */
    public OtRouteInfoNewOch allocateOtNewOchPrimary(Integer tunnelToCreate, String srcSiteId, TunnelNewOchInput input, Long cenFrequency, NeInfo tpcNeInfo, Map<String, Node> inMemoryNode,
                                                     String frequencyString, Node srcSiteLinkNode, Link siteLink, Class<? extends ETHERNETCOMPLIANCECODE> clientMedium) throws NeDesignerException {
        //prepare input
        Card otCardInfo = tpcNeInfo.getCardByCardType(input.getCardType());
        Card emptyCardInfo = tpcNeInfo.getEmptyCard();
        WDM_Band wdmBand = tunnelUtils.getWdmBand(siteLink);
        Card opCardInfo = getOp6CardInfo(input, tpcNeInfo, wdmBand);

        List<Node> nodeSnapShots = new ArrayList<>();

        OtNewOchNodeInfo otNewOchNodeInfo = getUserDefinedNodePrimary(srcSiteId, input,
                tpcNeInfo, inMemoryNode, opCardInfo, otCardInfo);

        //选择顺序1： 首先看优先级最高的includeNodes可以pick重用的node不
        if (otNewOchNodeInfo == null) {
            otNewOchNodeInfo = getReusedNodePrimary(inMemoryNode, input.getReusedIncludeNodes(), srcSiteId, opCardInfo, otCardInfo, emptyCardInfo, true, input
                    .getServiceType(), input.getProtectionType(), input.getExcludeNodes());
        }

        //选择顺序2： 从inMemoryNode选取
        String srcSiteLinkNodeId = srcSiteLinkNode.getNodeId().getValue();
        if (otNewOchNodeInfo == null) {
            Collection<Node> reUsedInMemoryNodePool = inMemoryNode.values();
            if (!input.getIsReusedMixed()) {
                reUsedInMemoryNodePool = reUsedInMemoryNodePool.stream()
                        .filter(n -> n.getAugmentation(Node1.class).getPhysical().getNodeType().equals(NodeType.TD))
                        .collect(Collectors.toList());
            }
//            if (!input.getIsReusedMixed()) {
//                reUsedInMemoryNodePool = inMemoryNode.values().stream().filter(node -> canTpcNodeReused(node, srcSiteLinkNodeId))
//                        .collect(Collectors.toList());
//            }
            otNewOchNodeInfo = getReusedNodePrimary(inMemoryNode, reUsedInMemoryNodePool, srcSiteId, opCardInfo, otCardInfo, emptyCardInfo, false, input.getServiceType(), input
                            .getProtectionType(),
                    input
                            .getExcludeNodes());
        }

        //选择顺序3：从reusedNodesInDb选取可重用的op6端口以及(重用的/新建的)L口
        if (otNewOchNodeInfo == null) {
//            List<Node> reusedNodeInDb = input.getReusedNodesInDb().stream().filter(node -> canTpcNodeReused(node, srcSiteLinkNodeId))
//                    .collect(Collectors.toList());
            List<Node> reusedNodeInDb = input.getReusedNodesInDb();
            otNewOchNodeInfo = getReusedNodePrimary(inMemoryNode, reusedNodeInDb, srcSiteId, opCardInfo, otCardInfo, emptyCardInfo, false, input.getServiceType(), input
                    .getProtectionType(), input
                    .getExcludeNodes());
        }

        //没有可重用的，创建新的node
        if (otNewOchNodeInfo != null) {
            nodeSnapShots.addAll(otNewOchNodeInfo.getSnapshotNode());
        } else {
            otNewOchNodeInfo = createNodeWithOtOp6Card(srcSiteId, tpcNeInfo, input.getPlane(), input.getPlaneId(), input.getRiskGroupName(), otCardInfo, opCardInfo, input
                    .getServiceType());
        }

        //创建XC, OS link, transceiver
        String lPortTp = otNewOchNodeInfo.getLTp();
        String opSigPortTp = otNewOchNodeInfo.getOp6Tp();
        String op6APortTp = tunnelUtils.getOpPrimaryTp(opSigPortTp, opCardInfo);
        String op6BPortTp = tunnelUtils.getOpSecondaryTp(opSigPortTp, opCardInfo);
        String op6CPortTp = input.getProtectionType().equals(PROTECTED_1TO2) ? tunnelUtils.getOpThirdTp(opSigPortTp, opCardInfo) : null;
        Node otNode = otNewOchNodeInfo.getLTpNode();
        Node op6Node = otNewOchNodeInfo.getOp6Node();

        List<CrossConnections> newOtXcs = otXcService.createOtXCsForNewOch(lPortTp, otNode, tunnelToCreate, otCardInfo, input.getLineSignalRate(), input.getTunnelSignalRate(), input
                .getServiceType());
        List<CrossConnections> newOp6Xc = otXcService.createOp6XCs(opSigPortTp, op6Node, opCardInfo);//实际只有一条
        List<CrossConnections> newTotalXcs = new ArrayList<>();
        newTotalXcs.addAll(newOtXcs);
        newTotalXcs.addAll(newOp6Xc);
        Map<String, String> portIdFriendlyNameMapOt = otNode.getTerminationPoint().stream()
                .filter(tp -> tp.getAugmentation(TerminationPoint1.class)
                        .getPhysical()
                        .getPortType()
                        .equals(PortType.OTUClient) || tp.getAugmentation(TerminationPoint1.class).getPhysical()
                        .getPortType().equals(PortType.OTULine)).collect(Collectors.toMap(
                        tp -> tp.getTpId().getValue(), tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName()
                ));
        List<Equipments> newTransceivers = otTransceiverService.createTransceiversNewOch(otNode.getNodeId()
                .getValue(), newOtXcs, otCardInfo, clientMedium, portIdFriendlyNameMapOt);
        Link newPrimaryOsLink = tunnelLinkService.createOsLink(lPortTp, opSigPortTp, false);

        //pick md port,mpo port
        Pair<Pair<Node, Link>, Pair<String, String>> muxMdPair = null;
        try {
            muxMdPair = pickMdTpMpoTp(srcSiteId, frequencyString, srcSiteLinkNode, siteLink);
        } catch (NoAvailableTpException e) {
            log.error("Failed to allocateOtNewOchPrimary", e);
            throw new NeDesignerException(e.getMessage());
        }
        srcSiteLinkNode = muxMdPair.getLeft().getLeft();
        siteLink = muxMdPair.getLeft().getRight();//todo 这里的sitelink并没有最后输出，到时看需要。
        Pair<String, String> muxMdPortTpSrcPair = muxMdPair.getRight();
        String muxMdPortTp = muxMdPortTpSrcPair.getLeft();
        String muxMpoPortTp = muxMdPortTpSrcPair.getRight();

        Link newPrimaryOlsLink = tunnelLinkService.createOsLink(op6APortTp, muxMdPortTp, false);
        List<Link> newTotalLinks = new ArrayList<>();
        newTotalLinks.add(newPrimaryOsLink);
        newTotalLinks.add(newPrimaryOlsLink);

        //update TPC node
        if (otNode.getNodeId().getValue().equals(op6Node.getNodeId().getValue())) {
            Node updatedOtNode = otNodeService.updatedOtNodeNewOch(otNode, newTotalLinks, newOtXcs, newOp6Xc, newTransceivers, input, cenFrequency, clientMedium);
            inMemoryNode.put(updatedOtNode.getNodeId().getValue(), updatedOtNode);
        } else {
            Node updatedOtNode = otNodeService.updatedOtNodeNewOch(otNode, Arrays.asList(newPrimaryOsLink), newOtXcs, Collections.EMPTY_LIST, newTransceivers, input, cenFrequency, clientMedium);
            Node updatedOp6Node = otNodeService.updatedOtNodeNewOch(op6Node, Arrays.asList(newPrimaryOlsLink, newPrimaryOlsLink), Collections.EMPTY_LIST, newOp6Xc, Collections.EMPTY_LIST, input,
                    cenFrequency, clientMedium);
            inMemoryNode.put(updatedOtNode.getNodeId().getValue(), updatedOtNode);
            inMemoryNode.put(updatedOp6Node.getNodeId().getValue(), updatedOp6Node);
        }

        //update OPC node
        CrossConnections expXC = null;
        Link mpoLink = null;//fix75 has no mpo link
        if (muxMpoPortTp != null) {
            mpoLink = getMpoLink(siteLink, muxMpoPortTp, muxMdPortTp);
            newTotalLinks.add(mpoLink);
            expXC = createExpXc(mpoLink, srcSiteLinkNode, cenFrequency, frequencyString);
        }
//        if (inMemoryNode.containsKey(srcSiteLinkNodeId)) {
//            srcSiteLinkNode = inMemoryNode.get(srcSiteLinkNodeId); //如果光电混用的话，此node可能在上一步被ot改变了。
//        }
        List<CrossConnections> olsXcs = new ArrayList<>();
        List<CrossConnections> ochXcs = new ArrayList<>(newOp6Xc);
        if (expXC != null) {
            olsXcs.add(expXC);
            ochXcs.add(expXC);
        }
        Node updatedOlsNode;
        if (mpoLink != null) {
            updatedOlsNode = olsNodeService.updatedOlsNode(srcSiteLinkNode, Arrays.asList(newPrimaryOlsLink, mpoLink), muxMdPortTp, olsXcs);
        } else {
            updatedOlsNode = olsNodeService.updatedOlsNode(srcSiteLinkNode, Arrays.asList(newPrimaryOlsLink), muxMdPortTp, olsXcs);
        }

        updatedOlsNode = updateByteDanceSpec(updatedOlsNode, siteLink);

        inMemoryNode.put(updatedOlsNode.getNodeId().getValue(), updatedOlsNode);

        return OtRouteInfoNewOch.builder()
                .inMemoryNodes(inMemoryNode)
                .osLinks(newTotalLinks)
                .tpcXcs(newOtXcs)
                .ochXcs(ochXcs)
                .snapshotNodes(nodeSnapShots)
                .secondaryTp(op6BPortTp)
                .thirdTp(op6CPortTp)
                .muxMdPortTpSrcPair(muxMdPortTpSrcPair)
                .build();
    }

    private Node updateByteDanceSpec(Node siteLinkNode, Link sitelink) {

        if (yangModel.equals(NeYangModel.ByteDance)) {
            String lineTp = sitelink.getSource().getSourceTp().getValue();
            if (!siteLinkNode.getNodeId().getValue().equals(PhysicalTpIdNamingRule.getNodeId(lineTp))) {
                lineTp = sitelink.getDestination().getDestTp().getValue();
            }
            List<TerminationPoint> olsTps = siteLinkNode.getTerminationPoint();

            for (int i = 0; i < olsTps.size(); i++) {
                if (lineTp.equals(olsTps.get(i).getTpId().getValue())) {
                    TerminationPoint oldTp = olsTps.get(i);
                    TerminationPoint newOlsTp = tpRepo.getApcTp(oldTp);
                    olsTps.set(i, newOlsTp);
                }
            }
            siteLinkNode.getTerminationPoint().clear();
            siteLinkNode.getTerminationPoint().addAll(olsTps);
        }
        return siteLinkNode;
    }

    private CrossConnections createExpXc(Link mpoLink, Node siteLinkNode, Long cenFrequency, String frequencyString) throws NeDesignerException {
        return null;
//        if (!mpoLink.getLinkId().getValue().contains(EXP)) {
//            return null;
//        }
//
//        String expTpId = mpoLink.getSource().getSourceTp().getValue();
//        if (!expTpId.contains(EXP)) {
//            expTpId = mpoLink.getDestination().getDestTp().getValue();
//        }
//
//        Card iraCardInfo = nodeUtils.getCardInfoByTpId(siteLinkNode, expTpId);
//        return olsNodeService.createOchXC(siteLinkNode.getNodeId().getValue(), expTpId, iraCardInfo, cenFrequency, frequencyString);
    }

    private Card getOp6CardInfo(TunnelNewOchInput input, NeInfo tpcNeInfo, WDM_Band wdmBand) throws NeDesignerException {
//        Card op6CardInfo = tpcNeInfo.getCardByCardVendor(input.getOp6CardType());
        Card op6CardInfo = tpcNeInfo.getCardByCardType(input.getOp6CardType());
        if (input.getOp6CardType() == null || op6CardInfo == null) {
            op6CardInfo = tpcNeInfo.getDefaultCardByCardClass(OP_CARD_CLASS, wdmBand);
        }
        return op6CardInfo;
    }

    /**
     * 需满足以下两个条件：
     * <p>
     * 1. TPC node
     * <p>
     * 2. 同一个tpc node只能连接一个sitelink
     *
     * @param node
     * @param srcSiteLinkNodeId
     * @return
     */
    private boolean canTpcNodeReused(Node node, String srcSiteLinkNodeId) {
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        NodeType nodeType = physical.getNodeType();
        if (!nodeType.equals(NodeType.TD)) {
            return false;
        }
        String nodeId = node.getNodeId().getValue();
        Optional<InternalLinks> sitelinkConnect = physical.getInternalLinks().stream()
                .filter(internalLink -> !internalLink.getSrcTp().contains(nodeId) || !internalLink.getDstTp().contains(nodeId)).findAny();
        if (!sitelinkConnect.isPresent()) {
            return true;
        }
        InternalLinks internalLinksSiteLink = sitelinkConnect.get();
        String siteLinkTp = internalLinksSiteLink.getSrcTp().contains(nodeId) ? internalLinksSiteLink.getDstTp() : internalLinksSiteLink.getSrcTp();
        return siteLinkTp.contains(srcSiteLinkNodeId);
    }

    public OtRouteInfoNewOch allocateOtStartNewOch(Integer tunnelToCreate, String srcSiteId, TunnelNewOchInput input, Long cenFrequency, NeInfo tpcNeInfo, Map<String, Node> inMemoryNode,
                                                   String frequencyString, Node srcSiteLinkNode, Link siteLink) throws NeDesignerException {
        return allocateOtNewOch(tunnelToCreate, srcSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, srcSiteLinkNode, input.getClientMediumA(), siteLink);
    }

    public OtRouteInfoNewOch allocateOtNewOch(Integer tunnelToCreate, String srcSiteId, TunnelNewOchInput input, Long cenFrequency, NeInfo tpcNeInfo, Map<String, Node> inMemoryNode,
                                              String frequencyString, Node srcSiteLinkNode, Class<? extends ETHERNETCOMPLIANCECODE> clientMedium, Link siteLink) throws NeDesignerException {
        //prepare input
        Card otCardInfo = tpcNeInfo.getCardByCardType(input.getCardType());

        OtNewOchNodeInfo otNewOchNodeInfo = getOtNewOchNodeInfo(srcSiteId, input, tpcNeInfo,
                inMemoryNode, srcSiteLinkNode, otCardInfo, input.getServiceType());

        //pick md port,mpo port
        Pair<Pair<Node, Link>, Pair<String, String>> muxMdPair = null;
        try {
            muxMdPair = pickMdTpMpoTp(srcSiteId, frequencyString, srcSiteLinkNode, siteLink);
        } catch (NoAvailableTpException e) {
            log.error("Failed to allocateOtNewOch", e);
            throw new NeDesignerException(e.getMessage());
        }
        srcSiteLinkNode = muxMdPair.getLeft().getLeft();
        siteLink = muxMdPair.getLeft().getRight();//todo 这里的sitelink并没有最后输出，到时看需要。
        Pair<String, String> muxMdPortTpSrcPair = muxMdPair.getRight();
        String muxMdPortTp = muxMdPortTpSrcPair.getLeft();
        String muxMpoPortTp = muxMdPortTpSrcPair.getRight();

        //创建XC, OS link, transceiver
        String lPortTp = otNewOchNodeInfo.getLTp();
        Node otNode = otNewOchNodeInfo.getLTpNode();
        List<CrossConnections> newOtXcs = otXcService.createOtXCsForNewOch(lPortTp, otNode, tunnelToCreate, otCardInfo, input.getLineSignalRate(), input.getTunnelSignalRate(), input
                .getServiceType());
        Map<String, String> portIdFriendlyNameMap = otNode.getTerminationPoint().stream()
                .filter(tp -> tp.getAugmentation(TerminationPoint1.class)
                        .getPhysical()
                        .getPortType()
                        .equals(PortType.OTUClient) || tp.getAugmentation(TerminationPoint1.class).getPhysical()
                        .getPortType().equals(PortType.OTULine)).collect(Collectors.toMap(
                        tp -> tp.getTpId().getValue(), tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName()
                ));
        List<Equipments> newTransceivers = otTransceiverService.createTransceiversNewOch(otNode.getNodeId()
                .getValue(), newOtXcs, otCardInfo, clientMedium, portIdFriendlyNameMap);
        Link mpoLink = muxMpoPortTp == null ? null : getMpoLink(siteLink, muxMpoPortTp, muxMdPortTp);
        Link newOsLink = tunnelLinkService.createOsLink(lPortTp, muxMdPortTp, false);
        List<Link> newOsLinks = mpoLink == null ? Arrays.asList(newOsLink) : Arrays.asList(newOsLink, mpoLink);
        //update TPC node
        Node updatedOtNode = otNodeService.updatedOtNodeNewOch(otNode, Arrays.asList(newOsLink), newOtXcs, Collections.EMPTY_LIST, newTransceivers, input, cenFrequency, clientMedium);
        inMemoryNode.put(updatedOtNode.getNodeId().getValue(), updatedOtNode);

        //update OPC node
        if (inMemoryNode.containsKey(srcSiteLinkNode.getNodeId().getValue())) {
            srcSiteLinkNode = inMemoryNode.get(srcSiteLinkNode.getNodeId().getValue()); //如果光电混用的话，此node可能在上一步被ot改变了。
        }

        CrossConnections expXC = mpoLink == null ? null : createExpXc(mpoLink, srcSiteLinkNode, cenFrequency, frequencyString);
        List<CrossConnections> ochXcs = new ArrayList<>();
        if (expXC != null) {
            ochXcs.add(expXC);
        }
        Node updatedOlsNode = olsNodeService.updatedOlsNode(srcSiteLinkNode, newOsLinks, muxMdPortTp, ochXcs);
        updatedOlsNode = updateByteDanceSpec(updatedOlsNode, siteLink);
        inMemoryNode.put(updatedOlsNode.getNodeId().getValue(), updatedOlsNode);

        return OtRouteInfoNewOch.builder()
                .inMemoryNodes(inMemoryNode)
                .osLinks(newOsLinks)
                .tpcXcs(newOtXcs)
                .ochXcs(ochXcs)
                .snapshotNodes(otNewOchNodeInfo.getSnapshotNode())
                .muxMdPortTpSrcPair(muxMdPortTpSrcPair)
                .build();
    }

    private OtNewOchNodeInfo getOtNewOchNodeInfo(String srcSiteId, TunnelNewOchInput input, NeInfo tpcNeInfo, Map<String, Node> inMemoryNode, Node srcSiteLinkNode, Card otCardInfo,
                                                 SERVICETYPE servicetype)
            throws NeDesignerException {
        OtNewOchNodeInfo selectedNodeInfo = getUserDefinedOtNewOchNodeInfo(srcSiteId, input,
                tpcNeInfo, inMemoryNode, otCardInfo, servicetype);
        if (selectedNodeInfo != null) {
            return selectedNodeInfo;
        }

        Card emptyCardInfo = tpcNeInfo.getEmptyCard();
        String srcSiteLinkNodeId = srcSiteLinkNode == null ? null : srcSiteLinkNode.getNodeId().getValue();

        //选择顺序1： 首先看优先级最高的includeNodes可以pick重用的node不
        OtNewOchNodeInfo reusedOtCard = getReusedNode(srcSiteId, otCardInfo, emptyCardInfo, input.getReusedIncludeNodes(), inMemoryNode, servicetype, input
                .getExcludeNodes());

        //选择顺序2： 从inMemoryNode选取
        if (reusedOtCard == null) {
            Collection<Node> reUsedInMemoryNodePool = inMemoryNode.values();
            if (!input.getIsReusedMixed()) {
                reUsedInMemoryNodePool = reUsedInMemoryNodePool.stream()
                        .filter(n -> n.getAugmentation(Node1.class).getPhysical().getNodeType().equals(NodeType.TD))
                        .collect(Collectors.toList());
            }
//            if (!input.getIsReusedMixed() && srcSiteLinkNodeId != null) {
//                reUsedInMemoryNodePool = inMemoryNode.values().stream().filter(node -> canTpcNodeReused(node, srcSiteLinkNodeId))
//                        .collect(Collectors.toList());
//            }
            reusedOtCard = getReusedNode(srcSiteId, otCardInfo, emptyCardInfo, reUsedInMemoryNodePool,
                    inMemoryNode, servicetype, input.getExcludeNodes());
        }

        //选择顺序3：从reusedNodesInDbS选取可重用的
        if (reusedOtCard == null) {
            //first, check if current srcSiteLinkNode can be reused
            if (srcSiteLinkNodeId != null && input.getReusedNodesInDb()
                    .stream()
                    .filter(node -> node.getNodeId().getValue().equals(srcSiteLinkNodeId))
                    .findAny()
                    .isPresent()) {
                reusedOtCard = getReusedNode(srcSiteId, otCardInfo, emptyCardInfo,
                        Arrays.asList(srcSiteLinkNode), inMemoryNode, servicetype, input.getExcludeNodes());
            }

            if (reusedOtCard == null) {
//                List<Node> reusedNodeInDb = input.getReusedNodesInDb().stream().filter(node -> canTpcNodeReused(node, srcSiteLinkNodeId))
//                        .collect(Collectors.toList());
                List<Node> reusedNodeInDb = input.getReusedNodesInDb();
                reusedOtCard = getReusedNode(srcSiteId, otCardInfo, emptyCardInfo, reusedNodeInDb,
                        inMemoryNode, servicetype, input.getExcludeNodes());
            }
        }

        //没有可重用的，创建新的node
        if (reusedOtCard == null) {
            reusedOtCard = createNodeWithOtCard(srcSiteId, tpcNeInfo, input.getPlane(), input.getPlaneId(), input.getRiskGroupName(), otCardInfo, servicetype);
        }
        return reusedOtCard;
    }

    /**
     * Note: 这个方法会改变输入参数： srcSiteLinkNode和siteLink
     *
     * @param srcSiteId
     * @param frequencyString
     * @param srcSiteLinkNode
     * @param siteLink
     * @return
     * @throws NeDesignerException
     */
    private Pair<Pair<Node, Link>, Pair<String, String>> pickMdTpMpoTp(String srcSiteId, String frequencyString, Node srcSiteLinkNode, Link siteLink)
            throws NeDesignerException, NoAvailableTpException {
        return pickMdTpMpoTp(srcSiteId, frequencyString, srcSiteLinkNode, siteLink, null);
    }

    private Pair<Pair<Node, Link>, Pair<String, String>> pickMdTpMpoTp(String srcSiteId, String frequencyString, Node srcSiteLinkNode, Link siteLink, String preferredMdPortName)
            throws NeDesignerException, NoAvailableTpException {
        Pair<Node, Link> updatedParis = createMuxPanel_And_ExternalLink(srcSiteLinkNode, siteLink);
        srcSiteLinkNode = updatedParis.getLeft();
        siteLink = updatedParis.getRight();
        Set<String> srcMuxEquipIds = getSiteLinkMuxEquipId(siteLink, srcSiteId);
        Set<String> wssExpPortIds = nodeUtils.getWssUsedExpTpIds(srcSiteLinkNode);
        // Resolve the OCHP layout from this exact siteLink. Protected legs may have different 32/64 capacities.
        OchpResourceLayout resourceLayout = OchpResourceLayoutFactory.create(siteLink);
        // Keep the preferred M?D? index inside the selected Bone2.0 32/64 resource layout.
        Pair<String, String> muxMdPortTpSrcPair = tunnelSiteAllocate.pickMuxTpMpoTp(
                frequencyString, srcSiteLinkNode, srcMuxEquipIds, wssExpPortIds,
                resourceLayout, preferredMdPortName);//get free mux tp to connect to OT card L port
        log.debug("Selected OCHP M/D resource {} for siteLink {} with layout {}",
                muxMdPortTpSrcPair.getLeft(), siteLink.getLinkId().getValue(),
                resourceLayout.getClass().getSimpleName());
        return ImmutablePair.of(updatedParis, muxMdPortTpSrcPair);
    }

    /**
     * 调用这个方法时，已经是调用pickMdTpMpoTp没有可用tp的情况了，所以需要判断贯通的wss link有没有在用，没有就删除，然后用那个exp口。
     * <p>
     * Note: 这个方法只能用在REG
     *
     * @param srcSiteId
     * @param frequencyString
     * @param srcSiteLinkNode
     * @param siteLink
     * @return
     * @throws NeDesignerException
     * @throws NoAvailableTpException
     */
    private Pair<Pair<Pair<Node, Link>, Pair<String, String>>, String> pickMdTpMpoTpReg_RemoveWss(String srcSiteId, String frequencyString, Node srcSiteLinkNode, Link siteLink)
            throws NeDesignerException {
        Pair<Node, Link> updatedParis = createMuxPanel_And_ExternalLink(srcSiteLinkNode, siteLink);
        srcSiteLinkNode = updatedParis.getLeft();
        siteLink = updatedParis.getRight();
        Set<String> srcMuxEquipIds = getSiteLinkMuxEquipId(siteLink, srcSiteId);
        Set<String> wssLinkIds = nodeUtils.getWssLinkIds(srcSiteLinkNode);
        for (String wssLinkId : wssLinkIds) {
            Boolean wssInuse = ochLinkDao.existsWssLinkUsedInOch(wssLinkId);//todo:这里如果将来有性能瓶颈，也可用改为判断node的wss交叉，只是判断och可能更稳妥
            if (!wssInuse) {
                log.info("Remove not used Wss link:{} to release resource.", wssLinkId);
                srcSiteLinkNode = neNodeRepo.removeInternalLink(srcSiteLinkNode, wssLinkId);
                updatedParis = Pair.of(srcSiteLinkNode, siteLink);
                String wssTpId = PhysicalLinkIdNamingRule.getTpAId(wssLinkId)
                        .contains(srcSiteLinkNode.getNodeId().getValue()) ? PhysicalLinkIdNamingRule.getTpAId(wssLinkId)
                        : PhysicalLinkIdNamingRule.getTpZId(wssLinkId);
                try {
                    Pair<String, String> muxMdPortTpSrcPair = tunnelSiteAllocate.pickFreeMdTpByExpTp(frequencyString, srcSiteLinkNode, srcMuxEquipIds, wssTpId);
                    return ImmutablePair.of(ImmutablePair.of(updatedParis, muxMdPortTpSrcPair), wssLinkId);
                } catch (NoAvailableTpException e) {
                    log.warn("No available MD port for:{}", wssTpId, e);
                    continue;
                }
            }
        }
        throw new NeDesignerException("No available resource");
    }

    /**
     * 这个方法会修改输入参数：siteLinkNode，siteLink
     *
     * @param siteLinkNode
     * @param siteLink
     * @throws NeDesignerException
     */
    private Pair<Node, Link> createMuxPanel_And_ExternalLink(Node siteLinkNode, Link siteLink) throws NeDesignerException {
        Boolean hasMux = nodeUtils.hasMuxEquipments(siteLinkNode);
        if (hasMux) {
            return ImmutablePair.of(siteLinkNode, siteLink);
        }
        NeInfo neInfo = nodeUtils.getNeInfoByNode(siteLinkNode, NodeType.OD);
        WDM_Band wdmBand = tunnelUtils.getWdmBand(siteLink);
        Card muxCard = neInfo.getDefaultCardByCardClass(NeInfo.MUXPANEL_CARD_CLASS, wdmBand);
        Integer slot = muxCard.getPossibleSlot().get(0);//todo 先只考虑有一个muxpanel的情况
        String nodeId = siteLinkNode.getNodeId().getValue();
        Equipments muxPanelEquip = equipmentRepo.createCardEquipment(nodeId, muxCard, slot, neInfo);
        Map<String, TerminationPoint> muxCardPortNameTpMap = tpRepo.createCardTp(muxPanelEquip, muxCard, neInfo);
        Map<String, String> muxCardPortNameTpIdMap = muxCardPortNameTpMap.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getTpId().getValue()));
        List<TerminationPoint> tps = siteLinkNode.getTerminationPoint();
        tps.addAll(muxCardPortNameTpMap.values());
        List<TerminationPoint> panelTps = tps.stream().filter(tp -> tp.getAugmentation(TerminationPoint1.class)
                .getPhysical().getConnectionStatus().equals(ConnectionStatus.Idle) && tp.getAugmentation(TerminationPoint1.class)
                .getPhysical().getPortType().equals(PortType.PanelMUX)).sorted(Comparator.comparing(t -> t.getTpId().getValue())).collect(Collectors.toList());
        if (panelTps.isEmpty()) {
            throw new NeDesignerException("Failed to get available panelMux tp for node:" + nodeId);
        }
        String panelTpId = panelTps.get(0).getTpId().getValue();
        Map<String, String> panelPortNameTpIdMap = new HashMap<>();
        panelPortNameTpIdMap.put(PhysicalTpIdNamingRule.getPortNameByTpId(panelTpId), panelTpId);
        //create panel cable link
        Map<String, Map<String, List<ExternalLinkTo>>> panelCardMap = neInfo.getExternalLinkInfo().get(NeInfo.PANEL_CARD_TYPE);
        Map<String, List<ExternalLinkTo>> fromToMap = panelCardMap.get(muxCard.getCardType());
        Set<String> busyIds = new HashSet<>();
        LinkOutput cableLinkOutPut = linkService.createCLLinks(nodeId, fromToMap, panelPortNameTpIdMap, muxCardPortNameTpIdMap, busyIds);
        //create mpo link
        String iraTpId;
        boolean isSrc = true;
        if (siteLinkNode.getNodeId().getValue().contains(siteLink.getSource().getSourceNode().getValue())) {
            iraTpId = siteLink.getSource().getSourceTp().getValue();
        } else {
            iraTpId = siteLink.getDestination().getDestTp().getValue();
            isSrc = false;
        }
        String iraEquipId = PhysicalTpIdNamingRule.getEquipId(iraTpId);
        List<TerminationPoint> iraMpoTps = tps.stream()
                .filter(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef().equals(iraEquipId)
                        && tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.WSSMesh))
                .collect(Collectors.toList());
        Map<String, String> mpoPortNameTpIdMap = iraMpoTps.stream()
                .collect(Collectors.toMap(t -> PhysicalTpIdNamingRule.getPortNameByTpId(t.getTpId().getValue()), t -> t.getTpId().getValue()));
        Map<String, Map<String, List<ExternalLinkTo>>> muxCardMap = neInfo.getExternalLinkInfo().get(muxCard.getCardType());
        Card iraCardInfo = nodeUtils.getCardInfoByEquipId(siteLinkNode, iraEquipId);
        Map<String, List<ExternalLinkTo>> fromMuxToIraMap = muxCardMap.get(iraCardInfo.getCardType());
        LinkOutput mpoLinkOutPut = linkService.createOmsLinks(nodeId, fromMuxToIraMap, muxCardPortNameTpIdMap, mpoPortNameTpIdMap, busyIds);

        //update node
        List<InternalLinks> internalLinks = siteLinkNode.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        internalLinks.addAll(cableLinkOutPut.getInternalLinks());
        internalLinks.addAll(mpoLinkOutPut.getInternalLinks());

        List<Equipments> equipList = siteLinkNode.getAugmentation(Node1.class).getPhysical().getEquipments();
        equipList.add(muxPanelEquip);
        Node updateSiteLinkNode = neNodeRepo.refreshNode(siteLinkNode, equipList, tps, internalLinks, busyIds);

//        //handle busy tp
//        int size = tps.size();
//        for (int i = 0; i < size; i++) {
//            TerminationPoint tp = tps.get(i);
//            if (busyIds.contains(tp.getTpId().getValue())) {
//                TerminationPoint busyTp = tpRepo.getBusyTp(tp);
//                tps.set(i, busyTp);
//            }
//        }

        //update sitelink(这里并没有入库，只是方便后续tunnel allocate是统一的数据结构)
        List<AddDropLink> mpoAddDroplinks = linkService.createAddDropLinks(mpoLinkOutPut.getLinks());
        SiteBuilder siteLinkBuilder = new SiteBuilder(siteLink.getAugmentation(Link1.class).getSite());
        if (isSrc) {
            List<AddDropLink> updateLinks = Optional.ofNullable(siteLink)
                    .map(link -> link.getAugmentation(Link1.class))
                    .map(Link1::getSite)
                    .map(Site::getAExternal)
                    .map(AExternal::getAddDropLink)
                    .map(ArrayList::new) // 复制为可变集合
                    .orElseGet(ArrayList::new); // 如果任何一步为 null，则返回空列表
            updateLinks.addAll(mpoAddDroplinks);
            siteLinkBuilder.setAExternal(new AExternalBuilder().setAddDropLink(updateLinks).build());

        } else {
            List<AddDropLink> updateLinks = Optional.ofNullable(siteLink)
                    .map(link -> link.getAugmentation(Link1.class))
                    .map(Link1::getSite)
                    .map(Site::getZExternal)
                    .map(ZExternal::getAddDropLink)
                    .map(ArrayList::new) // 复制为可变集合
                    .orElseGet(ArrayList::new); // 如果任何一步为 null，则返回空列表
            updateLinks.addAll(mpoAddDroplinks);
            siteLinkBuilder.setZExternal(new ZExternalBuilder().setAddDropLink(updateLinks).build());
        }
        Link updatedSiteLink = new LinkBuilder(siteLink)
                .addAugmentation(Link1.class, new Link1Builder()
                        .setSite(siteLinkBuilder.build()).build()
                ).build();
        return ImmutablePair.of(updateSiteLinkNode, updatedSiteLink);
    }

    /**
     * When multiple MPO port, here just pick any
     *
     * @param siteLink
     * @param siteId
     * @return
     * @throws NeDesignerException
     */
    private Set<String> getSiteLinkMuxEquipId(Link siteLink, String siteId) throws NeDesignerException {
        String muxTpId;
        String srcDestTpId;
        if (siteLink.getSource().getSourceNode().getValue().contains(siteId)) {
            srcDestTpId = siteLink.getSource().getSourceTp().getValue();

        } else if (siteLink.getDestination().getDestNode().getValue().contains(siteId)) {
            srcDestTpId = siteLink.getDestination().getDestTp().getValue();
        } else {
            throw new NeDesignerException("Failed to get mux tp from siteLink: " + siteLink.getLinkId().getValue() + " by siteId: " + siteId);
        }
        if (srcDestTpId.endsWith("MPO")) { //cmux 卡的复用段，直接返回muxpanel的equip id
            return ImmutableSet.of(PhysicalTpIdNamingRule.getEquipId(srcDestTpId));
        }
        muxTpId = srcDestTpId;
        Site siteAddr = siteLink.getAugmentation(Link1.class).getSite();
        AExternal aExternal = siteAddr.getAExternal();
        ZExternal zExternal = siteAddr.getZExternal();

        List<AddDropLink> addDropLinkList = null;
        if (aExternal != null && !aExternal.getAddDropLink().isEmpty() && aExternal.getAddDropLink().get(0).getLinkRef().contains(siteId)) {
            addDropLinkList = aExternal.getAddDropLink();
        } else if (zExternal != null && !zExternal.getAddDropLink().isEmpty() && zExternal.getAddDropLink().get(0).getLinkRef().contains(siteId)) {
            addDropLinkList = zExternal.getAddDropLink();
        }
        if (addDropLinkList == null) {
            if (siteAddr.getGrid().equals(GridType._0)) {
                String siteNodeId = PhysicalTpIdNamingRule.getNodeId(srcDestTpId);
                log.error("SiteLink:{} missing add drop link ,but node:{} has mux panel.", siteLink.getLinkId().getValue(), siteNodeId);
                throw new NeDesignerException("Dirty data for siteLink:" + siteAddr.getFriendlyName());
            } else {
                return ImmutableSet.of(PhysicalTpIdNamingRule.getEquipId(muxTpId));//fix75 scenario
            }
        }

        Set<String> muxEquipIds = addDropLinkList.stream()
                .filter(l -> l.getConnnectorType().equals(EquipType.MUXPANEL))
                .map(l -> getMuxPanelEquipByAddDropLink(l))
                .collect(Collectors.toSet());
        return muxEquipIds;
    }

    private String getMuxPanelEquipByAddDropLink(AddDropLink addDropLink) {
        String linkId = addDropLink.getLinkRef();
        String muxPanelTPId = PhysicalLinkIdNamingRule.getTpAId(linkId);
        if (!muxPanelTPId.contains("MUX")) {
            muxPanelTPId = PhysicalLinkIdNamingRule.getTpZId(linkId);
        }
        return PhysicalTpIdNamingRule.getEquipId(muxPanelTPId);
    }

    private Link getMpoLink(Link siteLink, String muxMpoTp, String muxMdPortTp) throws NeDesignerException {
        String iraMpoTpId = getIraMpoTpId(siteLink, muxMpoTp, muxMdPortTp);
        Optional<AddDropLink> optionalMpo = siteLink
                .getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite()
                .getAExternal()
                .getAddDropLink()
                .stream()
                .filter(addDropLink -> addDropLink.getLinkRef().contains(muxMpoTp) &&
                        addDropLink.getLinkRef().contains(iraMpoTpId))
                .findAny();
        if (!optionalMpo.isPresent()) {
            optionalMpo = siteLink
                    .getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite()
                    .getZExternal()
                    .getAddDropLink()
                    .stream()
                    .filter(addDropLink -> addDropLink.getLinkRef().contains(muxMpoTp) &&
                            addDropLink.getLinkRef().contains(iraMpoTpId))
                    .findAny();
        }
        Link mpoLink;
        if (optionalMpo.isPresent()) {
            String linkId = optionalMpo.get().getLinkRef();
            mpoLink = phyLinkDao.getPhyLinkById(linkId);
            if (mpoLink == null) { //e.g. add muxPanel during tunnel allocate
                mpoLink = linkService.createLink(linkId, LinkType.OmsLink);
            }
        } else {
            mpoLink = linkService.createMpoLink(muxMpoTp, iraMpoTpId);
        }
        return mpoLink;
    }

    private String getIraMpoTpId(Link siteLink, String muxMpoTp, String muxMdPortTp) throws NeDesignerException {
        String productType = siteLink.getAugmentation(Link1.class).getSite().getProductType();
        String vendorName = siteLink.getAugmentation(Link1.class).getSite().getVendorName();
        NeInfo neInfo = neInfoConfig.getNeInfo(vendorName, productType, NodeType.OD.name());
        String mpoPortName = PhysicalTpIdNamingRule.getShortPortName(muxMpoTp);
        String mdPortName = PhysicalTpIdNamingRule.getShortPortName(muxMdPortTp);

        WDM_Band wdmBand = tunnelUtils.getWdmBand(siteLink);
        Card muxpanelCardInfo = neInfo.getDefaultCardByCardClass(NeInfo.MUXPANEL_CARD_CLASS, wdmBand);
        Optional<ExternalLink> externallLinkOptional = muxpanelCardInfo.getExternalLinks()
                .stream()
                .filter(externalLink -> {
                    try {
                        if (!NeInfoUtil.getNameList(externalLink.getFrom()).contains(mpoPortName)) {
                            return false;
                        }
                        if (externalLink.getMdPort() == null) {
                            return true;
                        }
                        return NeInfoUtil.getNameList(externalLink.getMdPort())
                                .contains(mdPortName);
                    } catch (NeDesignerException e) {
                        throw new RuntimeException(e);
                    }
                }).findAny();
        if (!externallLinkOptional.isPresent()) {
            log.error("Failed to get ExternalLink definition by neInfo:{} from {}, and with mdTp:{} ", neInfo.getJsonFileName(), muxMpoTp, muxMdPortTp);
            throw new NeDesignerException("Failed to get mpo link definition by port:" + mpoPortName);
        }
        ExternalLink externalLink = externallLinkOptional.get();
        List<String> fromPortNames = NeInfoUtil.getNameList(externalLink.getFrom());
        List<String> toPortNames = NeInfoUtil.getNameList(externalLink.getTo().get(0).getPort());
        String toPortName;
        if (externalLink.getMdPort() == null) {
            toPortName = toPortNames.get(fromPortNames.indexOf(mpoPortName));
        } else {
            toPortName = toPortNames.get(NeInfoUtil.getNameList(externalLink.getMdPort()).indexOf(mdPortName));
        }
        String siteLinkLineTp = siteLink.getSource().getSourceTp().getValue();
        String curentSiteId = PhysicalTpIdNamingRule.getSiteId(muxMpoTp);
        if (!PhysicalTpIdNamingRule.getSiteId(siteLinkLineTp).equals(curentSiteId)) {
            siteLinkLineTp = siteLink.getDestination().getDestTp().getValue();
        }
        String expOrMpoTpId = siteLinkLineTp.substring(0, siteLinkLineTp.lastIndexOf("-") + 1) + toPortName;
        return expOrMpoTpId;
    }

    public OtRouteInfoNewOch allocateOtEndNewOchPrimary(Integer tunnelToCreate, String destSiteId, TunnelNewOchInput input, Long cenFrequency, NeInfo tpcNeInfo, Map<String, Node> inMemoryNode,
                                                        String frequencyString, Node siteLinkNode, Link siteLink) throws NeDesignerException {
        OtRouteInfoNewOch otRouteInfoNewOch = allocateOtNewOchPrimary(tunnelToCreate, destSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNode, siteLink,
                input.getClientMediumZ());
        Collections.reverse(otRouteInfoNewOch.getOsLinks());
        return otRouteInfoNewOch;
    }

    public OtRouteInfoNewOch allocateOtEndNewOchSecondary(String op6BPortTp, TunnelNewOchInput input, Long cenFrequency, Map<String, Node> inMemoryNode,
                                                          String frequencyString, Node siteLinkNode, Link siteLink) throws NeDesignerException {
        return allocateOtEndNewOchSecondary(op6BPortTp, input, cenFrequency, inMemoryNode, frequencyString, siteLinkNode, siteLink, null);
    }

    public OtRouteInfoNewOch allocateOtEndNewOchSecondary(String op6BPortTp, TunnelNewOchInput input, Long cenFrequency, Map<String, Node> inMemoryNode,
                                                          String frequencyString, Node siteLinkNode, Link siteLink, String preferredMdPortName) throws NeDesignerException {
        OtRouteInfoNewOch otRouteInfoNewOch = allocateOtNewOchSecondary(op6BPortTp, input, cenFrequency, inMemoryNode, frequencyString, siteLinkNode,
                input.getClientMediumZ(), siteLink, preferredMdPortName);
        Collections.reverse(otRouteInfoNewOch.getOsLinks());
        return otRouteInfoNewOch;
    }

    public OtRouteInfoNewOch allocateOtStartNewOchSecondary(String op6BPortTp, TunnelNewOchInput input, Long cenFrequency, Map<String, Node> inMemoryNode, String frequencyString, Node
                                                                    srcSiteLinkNode,
                                                            Link siteLink) throws NeDesignerException {
        return allocateOtStartNewOchSecondary(op6BPortTp, input, cenFrequency, inMemoryNode, frequencyString, srcSiteLinkNode,
                siteLink, null);
    }

    public OtRouteInfoNewOch allocateOtStartNewOchSecondary(String op6BPortTp, TunnelNewOchInput input, Long cenFrequency, Map<String, Node> inMemoryNode, String frequencyString, Node
                                                                    srcSiteLinkNode,
                                                            Link siteLink, String preferredMdPortName) throws NeDesignerException {
        return allocateOtNewOchSecondary(op6BPortTp, input, cenFrequency, inMemoryNode, frequencyString, srcSiteLinkNode,
                input.getClientMediumA(), siteLink, preferredMdPortName);
    }

    public OtRouteInfoNewOch allocateOtNewOchSecondary(String op6BPortTp, TunnelNewOchInput input, Long cenFrequency, Map<String, Node> inMemoryNode,
                                                       String frequencyString, Node srcSiteLinkNode, Class<? extends ETHERNETCOMPLIANCECODE> clientMedium, Link siteLink) throws NeDesignerException {
        return allocateOtNewOchSecondary(op6BPortTp, input, cenFrequency, inMemoryNode, frequencyString, srcSiteLinkNode, clientMedium, siteLink, null);
    }

    public OtRouteInfoNewOch allocateOtNewOchSecondary(String op6BPortTp, TunnelNewOchInput input, Long cenFrequency, Map<String, Node> inMemoryNode,
                                                       String frequencyString, Node srcSiteLinkNode, Class<? extends ETHERNETCOMPLIANCECODE> clientMedium, Link siteLink,
                                                       String preferredMdPortName) throws NeDesignerException {
        //获取OP6卡所在的node，因为OP6卡肯定在primary阶段就创建好了
        String op6NodeId = PhysicalTpIdNamingRule.getNodeId(op6BPortTp);
        Node op6Node = inMemoryNode.get(op6NodeId);
        if (op6Node == null) {
            log.debug("get op6 node for db, during tunel binding");
            op6Node = phyNodeDao.getConfigPhyNodeById(op6NodeId);
        }

        //pick md port,mpo port
        String srcSiteId = PhysicalNodeIdNamingRule.getSiteId(srcSiteLinkNode.getNodeId().getValue());
        Pair<Pair<Node, Link>, Pair<String, String>> muxMdPair = null;
        try {
            muxMdPair = pickMdTpMpoTp(srcSiteId, frequencyString, srcSiteLinkNode, siteLink, preferredMdPortName);
        } catch (NoAvailableTpException e) {
            log.error("Failed to allocateOtNewOchSecondary", e);
            throw new NeDesignerException(e.getMessage());
        }
        srcSiteLinkNode = muxMdPair.getLeft().getLeft();
        siteLink = muxMdPair.getLeft().getRight();//todo 这里的sitelink并没有最后输出，到时看需要。
        Pair<String, String> muxMdPortTpSrcPair = muxMdPair.getRight();
        String muxMdPortTp = muxMdPortTpSrcPair.getLeft();
        String muxMpoTp = muxMdPortTpSrcPair.getRight();

        //创建op8-M?D?的os link
        Link newSecondaryOlsLink = tunnelLinkService.createOsLink(op6BPortTp, muxMdPortTp, false);

        Link mpoLink = muxMpoTp == null ? null : getMpoLink(siteLink, muxMpoTp, muxMdPortTp);

        //更新op6卡所在node
        Node updatedOp6Node = otNodeService.updatedOtNodeNewOch(op6Node, Arrays.asList(newSecondaryOlsLink), Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, input,
                cenFrequency, clientMedium);
        inMemoryNode.put(updatedOp6Node.getNodeId().getValue(), updatedOp6Node);

        //更新siteLink node
        if (updatedOp6Node.getNodeId().getValue().equals(srcSiteLinkNode.getNodeId().getValue())) {
            srcSiteLinkNode = updatedOp6Node;
        }
        List<Link> osLinks = mpoLink == null ? Arrays.asList(newSecondaryOlsLink) : Arrays.asList(newSecondaryOlsLink, mpoLink);
        CrossConnections expXC = mpoLink == null ? null : createExpXc(mpoLink, srcSiteLinkNode, cenFrequency, frequencyString);
        List<CrossConnections> ochXcs = new ArrayList<>();
        if (expXC != null) {
            ochXcs.add(expXC);
        }
        Node updatedOlsNode = olsNodeService.updatedOlsNode(srcSiteLinkNode, osLinks, muxMdPortTp, ochXcs);
        updatedOlsNode = updateByteDanceSpec(updatedOlsNode, siteLink);
        inMemoryNode.put(updatedOlsNode.getNodeId().getValue(), updatedOlsNode);

        return OtRouteInfoNewOch.builder()
                .inMemoryNodes(inMemoryNode)
                .osLinks(osLinks)
                .tpcXcs(Collections.EMPTY_LIST)
                .ochXcs(ochXcs)
                .snapshotNodes(Collections.EMPTY_LIST)
                .muxMdPortTpSrcPair(muxMdPortTpSrcPair)
                .build();
    }

    public OtRouteInfoNewOch allocateOtEndNewOch(Integer tunnelToCreate, String destSiteId, TunnelNewOchInput input, Long cenFrequency, NeInfo tpcNeInfo, Map<String, Node> inMemoryNode,
                                                 String frequencyString, Node siteLinkNode, Link siteLink) throws NeDesignerException {
        OtRouteInfoNewOch temp = allocateOtNewOch(tunnelToCreate, destSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNode, input
                .getClientMediumZ(), siteLink);
        Collections.reverse(temp.getOsLinks());
        return temp;
    }

    public OtRouteInfoNewOch allocateReg(TunnelNewOchInput input, NeInfo tpcNeInfo, String siteLinkId, String nextSiteLinkId, RegSiteInfo regSiteInfo, Map<String, Node> inMemoryNode,
                                         Map<String, Link> inMemorySiteLink, String frequencyString, Long cenFrequency)
            throws NeDesignerException {
        return allocateReg(input, tpcNeInfo, siteLinkId, nextSiteLinkId, regSiteInfo, inMemoryNode,
                inMemorySiteLink, frequencyString, cenFrequency, null);
    }

    public OtRouteInfoNewOch allocateReg(TunnelNewOchInput input, NeInfo tpcNeInfo, String siteLinkId, String nextSiteLinkId, RegSiteInfo regSiteInfo, Map<String, Node> inMemoryNode,
                                         Map<String, Link> inMemorySiteLink, String frequencyString, Long cenFrequency, String preferredMdPortName)
            throws NeDesignerException {
        //prepare input
        Card otCardInfo = tpcNeInfo.getCardByCardType(input.getCardType());
        String siteId = regSiteInfo.getSiteId();
        SERVICETYPE regServiceType = NeInfoUtil.getRegServiceType(otCardInfo, input.getLineSignalRate());
        // REG 电框不绑定复用段局向，仍按原有的 L 口和空槽位资源判断是否可以复用。
        OtNewOchNodeInfo otNewOchNodeInfo = getOtNewOchNodeInfo(siteId, input, tpcNeInfo,
                inMemoryNode, null, otCardInfo, regServiceType);
        List<Node> nodeSnapshots = new ArrayList<>();

        // REG: direction:AZ andZA
        //pick md port,mpo port
        Link siteLink1 = inMemorySiteLink.containsKey(siteLinkId) ? inMemorySiteLink.get(siteLinkId) : siteLinkDao.getSiteLinkById(siteLinkId);
        Link siteLink2 = inMemorySiteLink.containsKey(nextSiteLinkId) ? inMemorySiteLink.get(nextSiteLinkId) : siteLinkDao.getSiteLinkById(nextSiteLinkId);

//        String muxEquipId1 = getSiteLinkMuxEquipId(siteLink1, siteId).iterator().next();
        String muxNodeId1 = getSiteLinkNodeId(siteLink1, siteId);
        Node muxNode1 = inMemoryNode.containsKey(muxNodeId1) ? inMemoryNode.get(muxNodeId1) : phyNodeDao.getConfigPhyNodeById(muxNodeId1);
        if (!inMemoryNode.containsKey(muxNodeId1)) {
            nodeSnapshots.add(nodeUtils.getNodeCopy(muxNode1));
        }
        String lineTp1 = getSiteLinkLineTpId(siteLink1, muxNodeId1);

//        String muxEquipId2 = getSiteLinkMuxEquipId(siteLink2, siteId).iterator().next();
        String muxNodeId2 = getSiteLinkNodeId(siteLink2, siteId);
        Node muxNode2 = inMemoryNode.containsKey(muxNodeId2) ? inMemoryNode.get(muxNodeId2) : phyNodeDao.getConfigPhyNodeById(muxNodeId2);
        if (!inMemoryNode.containsKey(muxNodeId2)) {
            nodeSnapshots.add(nodeUtils.getNodeCopy(muxNode2));
        }
        String lineTp2 = getSiteLinkLineTpId(siteLink2, muxNodeId2);

        //pick md port,mpo port
        Pair<Pair<Node, Link>, Pair<String, String>> muxMdPair = null;
        List<String> removedResourceIds = new ArrayList<>();
        try {
            muxMdPair = pickMdTpMpoTp(siteId, frequencyString, muxNode1, siteLink1, preferredMdPortName);
        } catch (NoAvailableTpException e) {
            Pair<Pair<Pair<Node, Link>, Pair<String, String>>, String> pickResult = pickMdTpMpoTpReg_RemoveWss(siteId, frequencyString, muxNode1, siteLink1);
            muxMdPair = pickResult.getLeft();
            removedResourceIds.add(pickResult.getRight());
        }
        muxNode1 = muxMdPair.getLeft().getLeft();
        inMemoryNode.put(muxNodeId1, muxNode1);
        siteLink1 = muxMdPair.getLeft().getRight();//todo 这里的sitelink并没有最后输出，到时看需要。
        inMemorySiteLink.put(siteLinkId, siteLink1);
        Pair<String, String> muxMdPortTpPair1 = muxMdPair.getRight();
        String muxMdPortTp1 = muxMdPortTpPair1.getLeft();
        String muxMpoPortTp1 = muxMdPortTpPair1.getRight();

        Pair<Pair<Node, Link>, Pair<String, String>> muxMdPair2 = null;
        try {
            muxMdPair2 = pickMdTpMpoTp(siteId, frequencyString, muxNode2, siteLink2, preferredMdPortName);
        } catch (NoAvailableTpException e) {
            Pair<Pair<Pair<Node, Link>, Pair<String, String>>, String> pickResult2 = pickMdTpMpoTpReg_RemoveWss(siteId, frequencyString, muxNode2, siteLink2);
            muxMdPair2 = pickResult2.getLeft();
            removedResourceIds.add(pickResult2.getRight());
        }
        muxNode2 = muxMdPair2.getLeft().getLeft();
        inMemoryNode.put(muxNodeId2, muxNode2);
        siteLink2 = muxMdPair2.getLeft().getRight();//todo 这里的sitelink并没有最后输出，到时看需要。
        inMemorySiteLink.put(nextSiteLinkId, siteLink2);
        Pair<String, String> muxMdPortTpPair2 = muxMdPair2.getRight();
        String muxMdPortTp2 = muxMdPortTpPair2.getLeft();
        String muxMpoPortTp2 = muxMdPortTpPair2.getRight();

        //handle mpolink/expXc---left
        Link mpoLink1 = getMpoLink(siteLink1, muxMpoPortTp1, muxMdPortTp1);
        CrossConnections expXc1 = createExpXc(mpoLink1, muxNode1, cenFrequency, frequencyString);
        List<CrossConnections> ochXcs1 = new ArrayList<>();
        if (expXc1 != null) {
            ochXcs1.add(expXc1);
        }

        //create ext xc(line-md)---left
        List<String> muxNode1Tps = Arrays.asList(muxMdPortTp1, lineTp1);
        NeInfo opcNeInfo1 = nodeUtils.getNeInfoByNode(muxNode1, NodeType.OD);
        Boolean isAdditional1 = tunnelUtils.isAdditionalMux(siteLink1, muxMdPortTp1);
        CrossConnections extXc1 = tunnelSiteAllocate.createExtXcForNode(frequencyString, opcNeInfo1, BigInteger.valueOf(cenFrequency), muxNode1, muxNode1Tps, isAdditional1);
        extXc1 = getUpdatedRangeXcReg(extXc1);
        ochXcs1.add(extXc1);

        //create ext xc(line-md)---right
        List<CrossConnections> ochXcs2 = new ArrayList<>();
        List<String> muxNode2Tps = Arrays.asList(muxMdPortTp2, lineTp2);
        NeInfo opcNeInfo2 = nodeUtils.getNeInfoByNode(muxNode2, NodeType.OD);
        Boolean isAdditional2 = tunnelUtils.isAdditionalMux(siteLink2, muxMdPortTp2);
        CrossConnections extXc2 = tunnelSiteAllocate.createExtXcForNode(frequencyString, opcNeInfo2, BigInteger.valueOf(cenFrequency), muxNode2, muxNode2Tps, isAdditional2);
        extXc2 = getUpdatedRangeXcReg(extXc2);
        ochXcs2.add(extXc2);

        //handle mpolink/expXc---right
        Link mpoLink2 = getMpoLink(siteLink2, muxMpoPortTp2, muxMdPortTp2);
        CrossConnections expXc2 = createExpXc(mpoLink2, muxNode2, cenFrequency, frequencyString);
        if (expXc2 != null) {
            ochXcs2.add(expXc2);
        }

        //创建 OS link
        String lPortTp1 = otNewOchNodeInfo.getLTp();
        String lPortTp2 = otNewOchNodeInfo.getLTp2();
        Node otNode = otNewOchNodeInfo.getLTpNode();
        Node otNode2 = otNewOchNodeInfo.getLTp2Node() == null ? otNode
                : otNewOchNodeInfo.getLTp2Node();
        Link newOsLink1Az = tunnelLinkService.createRegLink(muxMdPortTp1, lPortTp1);
        Link newOsLink2Az = tunnelLinkService.createRegLink(lPortTp1, muxMdPortTp2);
        Link newOsLink1Za = tunnelLinkService.createRegLink(lPortTp2, muxMdPortTp1);
        Link newOsLink2Za = tunnelLinkService.createRegLink(muxMdPortTp2, lPortTp2);
        List<Link> newOsLinksTpcNode = Arrays.asList(newOsLink1Az, newOsLink2Az, newOsLink1Za, newOsLink2Za);
        List<Link> newOsLinksOpcNode1 = Arrays.asList(mpoLink1, newOsLink1Az, newOsLink1Za);
        List<Link> newOsLinksOpcNode2 = Arrays.asList(newOsLink2Az, newOsLink2Za, mpoLink2);
        List<Link> newOsLinkTotal = new ArrayList<>(newOsLinksOpcNode1);
        newOsLinkTotal.addAll(newOsLinksOpcNode2);

        //create reg xc
        String otNodeId = otNode.getNodeId().getValue();
        String otNodeId2 = otNode2.getNodeId().getValue();
        CrossConnections newRegXc1 = otXcService.createRegXc(lPortTp1, otNodeId, otCardInfo, regServiceType);
        CrossConnections newRegXc2 = otXcService.createRegXc(lPortTp2, otNodeId2, otCardInfo, regServiceType);
        List<CrossConnections> newRegXcs = Arrays.asList(newRegXc1, newRegXc2);

        //update TPC node
//        Node updatedOtNode = otNodeService.updatedOtNodeNewOch(otNode, newOsLinksTpcNode, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, input, cenFrequency,
//                null);//这里用不到C口，所以clientMedium为null
        if (otNodeId.equals(otNodeId2)) {
            Node updatedOtNode = otNodeService.updatedOtNodeNewOchReg(otNode, newOsLinksTpcNode, newRegXcs, input, cenFrequency, regServiceType);
            inMemoryNode.put(updatedOtNode.getNodeId().getValue(), updatedOtNode);
        } else {
            Node updatedOtNode = otNodeService.updatedOtNodeNewOchReg(otNode,
                    Arrays.asList(newOsLink1Az, newOsLink2Az), Arrays.asList(newRegXc1), input,
                    cenFrequency, regServiceType);
            Node updatedOtNode2 = otNodeService.updatedOtNodeNewOchReg(otNode2,
                    Arrays.asList(newOsLink1Za, newOsLink2Za), Arrays.asList(newRegXc2), input,
                    cenFrequency, regServiceType);
            inMemoryNode.put(updatedOtNode.getNodeId().getValue(), updatedOtNode);
            inMemoryNode.put(updatedOtNode2.getNodeId().getValue(), updatedOtNode2);
        }

        //update OPC node
        Node muxNode1Updated = olsNodeService.updatedOlsNode(muxNode1, newOsLinksOpcNode1, muxMdPortTp1, ochXcs1);
        muxNode1Updated = updateByteDanceSpec(muxNode1Updated, siteLink1);
        Node muxNode2Updated = olsNodeService.updatedOlsNode(muxNode2, newOsLinksOpcNode2, muxMdPortTp2, ochXcs2);
        muxNode2Updated = updateByteDanceSpec(muxNode2Updated, siteLink2);

        inMemoryNode.put(muxNodeId1, muxNode1Updated);
        inMemoryNode.put(muxNodeId2, muxNode2Updated);
        List<CrossConnections> ochXcs = new ArrayList<>(ochXcs1);
        ochXcs.addAll(newRegXcs);
        ochXcs.addAll(ochXcs2);

        nodeSnapshots.addAll(otNewOchNodeInfo.getSnapshotNode());

        return OtRouteInfoNewOch.builder()
                .inMemoryNodes(inMemoryNode)
                .osLinks(newOsLinkTotal)
                .tpcXcs(Collections.EMPTY_LIST)
                .ochXcs(ochXcs)
                .snapshotNodes(nodeSnapshots)
                .muxMdPortTpSrcPair(null)
                .removedResourceIds(removedResourceIds)
                .build();
    }

    // 方法本体
    private CrossConnections getUpdatedRangeXcReg(CrossConnections regMdXc) {
        List<Property> propertyList = new ArrayList<>(regMdXc.getWssChannel().getProperties().getProperty());

        propertyList.removeIf(p -> AUTO_CONTROL_RANGE.equals(p.getName()));
        propertyList.add(new PropertyBuilder().setName(AUTO_CONTROL_RANGE).setValue(AUTO_CONTROL_VALUE).build());

        return new CrossConnectionsBuilder(regMdXc)
                .setWssChannel(new WssChannelBuilder(regMdXc.getWssChannel())
                        .setProperties(new PropertiesBuilder().setProperty(propertyList).build())
                        .build())
                .build();
    }

    private String getSiteLinkNodeId(Link siteLink, String siteId) {
        String nodeAId = SiteLinkIdNamingRule.getNodeA(siteLink.getLinkId().getValue());
        if (nodeAId.contains(siteId)) {
            return nodeAId;
        }
        return SiteLinkIdNamingRule.getNodeZ(siteLink.getLinkId().getValue());
    }

    private String getSiteLinkLineTpId(Link siteLink, String nodeId) throws NeDesignerException {
        if (!siteLink.getAugmentation(Link1.class).getSite().getProtectionType().equals(ProtectionUnprotected.class)) {
            throw new NeDesignerException("Not supported protected siteLink.");
        }
        Optional<String> lineTpOptional = siteLink.getAugmentation(Link1.class)
                .getSite()
                .getExplictRoute()
                .getRoute()
                .get(0)
                .getPrimary()
                .getExplicitRouteObjects()
                .get(0)
                .getPathRouteObject()
                .stream()
                .filter(item -> item.getResourceType() instanceof Tp)
                        .
                map(item -> ((Tp) item.getResourceType()).getTpHop().getTpRef().getValue())
                .filter(tpId -> tpId.endsWith("LINE") && PhysicalTpIdNamingRule.getNodeId(tpId).equals(nodeId))
                .findAny();
        if (!lineTpOptional.isPresent()) {
            throw new NeDesignerException("Failed to get line tp for siteLink:" + siteLink.getLinkId().getValue());
        }
        return lineTpOptional.get();
    }


}
