/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.ne;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.NEDefaultSystemConfig;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.FixEquipModel;
import net.flex.dci.otn.controller.allocate.ne.LoginInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NeNodeRepo {

    public static final String YANG_MODEL_PROPERTY_NAME = "yang-model";
    public static final String DEFAULT_SUPERVISION_STATUS = "unmonitored";
    @Autowired
    private NEDefaultSystemConfig neDefaultSystemConfig;
    @Autowired
    private EquipmentRepo equipmentRepo;
    @Autowired
    private TpRepo tpRepo;
    @Autowired
    private NEInfoConfig neInfoConfig;
    @Value("${server.yangModel}")
    private String yangModel;


    public Node createEmptyNode(String vendorName, String vendorType, String siteId, NodeType nodeType, List<FixEquipModel> fixEquipModels, Card emptyCard, String plane,String planeId, @NonNull String riskGroupName, NeSubType neSubType)
            throws NeDesignerException {
        //为了避免NE-ID重复
        try {
            TimeUnit.MILLISECONDS.sleep(5);
        } catch (InterruptedException e) {
            log.error("Failed to delay neID creation.", e);
        }
        String nodeId = NEIdGenerator.createNodeId(siteId);
        LoginInfo loginInfo = neDefaultSystemConfig.getLoginInfo(vendorName);
        String friendlyName = NameGenerator.createNodeFriendlyName(nodeId);
        List<Equipments> equipments = equipmentRepo.createFixedEquipment(nodeId, fixEquipModels, emptyCard);
        Properties properties = null;
        if (yangModel != null) {
            List<Property> modelProperties = new ArrayList<>();
            modelProperties.add(new PropertyBuilder().setName(YANG_MODEL_PROPERTY_NAME).setValue(yangModel).build());
            properties = new PropertiesBuilder().setProperty(modelProperties).build();
        }

        PhysicalBuilder physicalBuilder = new PhysicalBuilder()
                .setCreationTime(NEIdGenerator.getCurrentTime())
                .setImplementState(ImplementState.Allocate)
                .setVendorName(vendorName)
                .setVendorType(vendorType)
                .setLoginName(loginInfo == null ? null : loginInfo.getName())
                .setLoginPasswd(loginInfo == null ? null : loginInfo.getPasswd())
                .setPort(loginInfo == null ? null : new PortNumber(loginInfo.getPort()))
                .setAdminState(AdminStatus.Unknown)
                .setNodeType(nodeType)
                .setSystem(neDefaultSystemConfig.getSystemConfig())
                .setOperationalState(OperStatus.Unknown)
                .setAlarmState(AlarmSeverity.Unknown)
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .setProperties(properties)
                .setFriendlyName(friendlyName).setStuffed(false)
                .setEquipments(equipments)
                .setCrossConnections(new ArrayList<>())
                .setInternalLinks(new ArrayList<>())
                .setPlaneName(plane)
                .setPlaneId(planeId)
                .setRiskGroupName(riskGroupName)
                .setSupervisionStatus(SupervisionStatusType.Monitoring)
                .setCommunicationStatus(CommunicationStatusType.Broken)
                .setCustomedType(neSubType.getSubTypeName());

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        List<TerminationPoint> fixTps = tpRepo.createFixedTps(nodeId, fixEquipModels);

        Node outputNode = new NodeBuilder().setNodeId(new NodeId(nodeId))
                .setKey(new NodeKey(NodeId.getDefaultInstance(nodeId)))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(fixTps)
                .build();

        return outputNode;
    }


    public Node updateInternalLink_And_BusyTp(Node node, List<InternalLinks> internalLinks, @NonNull Set<String> busyTpIds) {
        List<InternalLinks> oldInternalLinks = node.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        oldInternalLinks.addAll(internalLinks);

        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical()).setInternalLinks(oldInternalLinks);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        List<TerminationPoint> tps = handleBusyTp(node.getTerminationPoint(), busyTpIds);

        Node outputNode = new NodeBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(tps)
                .build();

        return outputNode;
    }

    public Node refreshNode(Node node, List<InternalLinks> newInternalLinks, @NonNull Set<String> busyTpIds) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(newInternalLinks);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();
        List<TerminationPoint> tps = handleBusyTp(node.getTerminationPoint(), busyTpIds);

        Node outputNode = new NodeBuilder(node)
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(tps)
                .build();

        return outputNode;
    }

    private List<TerminationPoint> handleBusyTp(List<TerminationPoint> nodeTpList, Set<String> busyTpIds) {
        int size = nodeTpList.size();
        for (int i = 0; i < size; i++) {
            TerminationPoint tp = nodeTpList.get(i);
            if (busyTpIds.contains(tp.getTpId().getValue())) {
                TerminationPoint busyTp = tpRepo.getBusyTp(tp);
                nodeTpList.set(i, busyTp);
            }
        }
        return nodeTpList;
    }


    public Node refreshNode(Node node, List<Equipments> newEquips, List<TerminationPoint> newTps, List<InternalLinks> newInternalLinks, List<CrossConnections> newNodeXcs, Boolean isStuffed,
            List<OCMGripGroups> ocmGripGroupsList) {
        return refreshNode(node, newEquips, newTps, newInternalLinks, newNodeXcs, null, isStuffed, ocmGripGroupsList);
    }

    public Node refreshNode(Node node, List<Equipments> equipments, List<TerminationPoint> tps, List<InternalLinks> internalLinks, List<CrossConnections> xcs, Set<String> busyTpIds,
            Boolean stuffed, List<OCMGripGroups> ocmGripGroupsList) {

        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setEquipments(equipments)
                .setInternalLinks(internalLinks)
                .setCrossConnections(xcs)
                .setStuffed(stuffed)
                .setOCMGripGroups(ocmGripGroupsList);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();
        List<TerminationPoint> newTps = tps;
        if (busyTpIds != null && !busyTpIds.isEmpty()) {
            newTps = handleBusyTp(tps, busyTpIds);
        }

        Node outputNode = new NodeBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(newTps)
                .build();

        return outputNode;
    }

    public Node refreshNode(Node node, List<InternalLinks> newInternalLinks) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(newInternalLinks);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        Node outputNode = new NodeBuilder(node)
                .addAugmentation(Node1.class, phyNode)
                .build();

        return outputNode;
    }

    public Node refreshNodeEquips(Node node, List<Equipments> equipList) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setEquipments(equipList);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        Node outputNode = new NodeBuilder(node)
                .addAugmentation(Node1.class, phyNode)
                .build();

        return outputNode;
    }

    public Node refreshNodeBy_Tps_newEquip_checkStuffed(Node node, List<TerminationPoint> mewTpList, Equipments newEquip) throws NeDesignerException {
        Physical oldPhysical = node.getAugmentation(Node1.class).getPhysical();

        //replace existed empty card with new line card, check stuffed
        List<Equipments> equipList = oldPhysical.getEquipments();
        List<Equipments> newEquipList;
        Boolean stuffed = true;
        if (newEquip == null) {
            newEquipList = equipList;
            stuffed = oldPhysical.isStuffed();
        } else {
            newEquipList = new ArrayList<>();
            stuffed = true;
            for (Equipments equipment : equipList) {
                if (equipment.getSlot() != null && equipment.getSlot().equals(newEquip.getSlot())) {
                    if (!equipment.getEquipType().equals(EquipType.EMPTY)) {
                        log.error("Failed to create equip:{}.\n because exist other equip with same id exists already.:{}", newEquip, equipment);
                        throw new NeDesignerException("Failed to create equip, because other equip with same id, exists already:" + newEquip.getEquipmentId());
                    }
                    continue;

                }
                if (equipment.getEquipType().equals(EquipType.EMPTY)) {
                    stuffed = false;
                }
                newEquipList.add(equipment);
            }
            newEquipList.add(newEquip);
        }

        PhysicalBuilder physicalBuilder = new PhysicalBuilder(oldPhysical)
                .setEquipments(newEquipList)
                .setStuffed(stuffed);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();
        List<TerminationPoint> tpList = node.getTerminationPoint();
        tpList.addAll(mewTpList);

        Node outputNode = new NodeBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(tpList)
                .build();

        return outputNode;
    }

    public Node refreshNodeByXcs(Node node, List<CrossConnections> xcs) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setCrossConnections(xcs);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        Node outputNode = new NodeBuilder(node)
                .addAugmentation(Node1.class, phyNode)
                .build();

        return outputNode;
    }

    public Node refreshNodeByXcsTpsEquips(Node node, List<CrossConnections> xcs, List<TerminationPoint> tps, List<Equipments> equipList) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setCrossConnections(xcs)
                .setEquipments(equipList);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        Node outputNode = new NodeBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(tps)
                .build();

        return outputNode;
    }

    /**
     * 1. 补全相关信息, 当node的tpList为空时，才会调用此方法。
     *
     * @param vendorName
     * @param productType
     * @param tpcNeInfo
     * @param oldNode
     * @param nodeType
     * @param plane
     * @param riskGroupName
     * @return
     */
    public Node refreshEmptyNode(String vendorName, String productType, NeInfo tpcNeInfo, Node oldNode, NodeType nodeType, String plane,String planeId, String riskGroupName) throws NeDesignerException {

        String nodeId = oldNode.getNodeId().getValue();
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(oldNode.getAugmentation(Node1.class).getPhysical());
        if (physicalBuilder.getLoginName() == null || physicalBuilder.getLoginPasswd() == null) {
            LoginInfo loginInfo = neDefaultSystemConfig.getLoginInfo(vendorName);
            physicalBuilder.setLoginName(loginInfo == null ? null : loginInfo.getName());
            physicalBuilder.setLoginPasswd(loginInfo == null ? null : loginInfo.getPasswd());
            physicalBuilder.setPort(loginInfo == null ? null : new PortNumber(loginInfo.getPort()));
        }

        if (physicalBuilder.getCreationTime() == null) {
            physicalBuilder.setCreationTime(NEIdGenerator.getCurrentTime());
        }

        //不管node有没有这些值，强行更新
        List<FixEquipModel> fixEquipModels = tpcNeInfo.getFixEquipModel();
        List<Equipments> equipments = equipmentRepo.createFixedEquipment(nodeId, fixEquipModels, tpcNeInfo.getEmptyCard());

        physicalBuilder
                .setImplementState(ImplementState.Allocate)
                .setVendorName(vendorName)
                .setVendorType(productType)
                .setAdminState(AdminStatus.Unknown)
                .setNodeType(nodeType)
                .setSystem(neDefaultSystemConfig.getSystemConfig())
                .setOperationalState(OperStatus.Unknown)
                .setAlarmState(AlarmSeverity.Unknown)
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .setProperties(NameGenerator.getFakeProperty())
                .setStuffed(false)
                .setEquipments(equipments)
                .setCrossConnections(new ArrayList<>())
                .setInternalLinks(new ArrayList<>())
                .setPlaneName(plane)
                .setPlaneId(planeId)
                .setRiskGroupName(riskGroupName);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        List<TerminationPoint> fixTps = tpRepo.createFixedTps(nodeId, fixEquipModels);

        Node outputNode = new NodeBuilder().setNodeId(new NodeId(nodeId))
                .setKey(new NodeKey(NodeId.getDefaultInstance(nodeId)))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(fixTps)
                .build();

        return outputNode;
    }


    public Node createUnmonitoredNode(String siteId, NodeType nodeType, NeInfo neInfo, String planeName,String planeId, String riskGroupName, String ip, NeSubType neSubType) throws NeDesignerException {
        Node node = createEmptyNode(neInfo.getVendor(), neInfo.getProductType(), siteId, nodeType, neInfo.getFixEquipModel(), neInfo.getEmptyCard(), planeName, planeId,riskGroupName, neSubType);
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical());
        physicalBuilder.setSupervisionStatus(SupervisionStatusType.Unmonitored);
        if (ip != null && !ip.isEmpty()) {
            physicalBuilder.setIp(ip);
        }

        return new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder().setPhysical(physicalBuilder.build()).build())
                .build();
    }

    public Node createEmptyNodeWithIp(String siteId, String planeName, String planeId, String riskGroupName, String vendorName, String vendorType, String nodeIp, NodeType nodeType, NeSubType neSubType) throws NeDesignerException {
        NeInfo neInfo = getNeInfo(vendorName, vendorType, nodeType);
        return createUnmonitoredNode(siteId, nodeType, neInfo, planeName,planeId, riskGroupName, nodeIp,neSubType);
    }

    private NeInfo getNeInfo(String vendorName, String vendorType, NodeType nodeType) throws NeDesignerException {
        return neInfoConfig.getNeInfo(vendorName, vendorType, nodeType.name());
    }

    public Node refreshNode(Node node, List<Equipments> equipList, List<TerminationPoint> tps, List<InternalLinks> internalLinks, Set<String> busyTpIds) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                .setEquipments(equipList)
                .setInternalLinks(internalLinks)
                .setCustomedType(NeSubType.OPC_OTM.getSubTypeName());

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();
        List<TerminationPoint> newTps = tps;
        if (busyTpIds != null && !busyTpIds.isEmpty()) {
            newTps = handleBusyTp(tps, busyTpIds);
        }

        Node outputNode = new NodeBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(newTps)
                .build();

        return outputNode;
    }

    public Node removeInternalLink(Node node, String linkId) {
        List<InternalLinks> internalLinks = node.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        internalLinks = internalLinks.stream().filter(internalLink -> !internalLink.getLinkRef().equals(linkId)).collect(Collectors.toList());

        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical()).setInternalLinks(internalLinks);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        Node outputNode = new NodeBuilder(node)
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, phyNode)
                .build();

        return outputNode;
    }
}
