/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.node.phy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;

/**
 * @author YYX
 * @date 12/6/2021
 */
@Slf4j
public class PhyNodeUtil {

    public static TerminationPoint updateConnectionStatus(TerminationPoint tp,
            ConnectionStatus connectionStatus) {
        TerminationPoint1 oldTp1 = tp.getAugmentation(TerminationPoint1.class);
        Physical phyTpAttr = oldTp1.getPhysical();
        TerminationPoint newTp = new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder(oldTp1)
                        .setPhysical(
                                new PhysicalBuilder(phyTpAttr).setConnectionStatus(connectionStatus)
                                        .build())
                        .build())
                .build();

        return newTp;
    }

    public static Node removeOTTransceiver(Node node, String tpId) {
        //删除资源的时候应该已经处理了
        node = cleanTpAttr(node, tpId);
        node = removeXcByTpId(node, tpId);
        node = removeTransceiverOnPort(node, tpId);  //remove hardware

        return node;
    }

    private static Node removeXcByTpId(Node node, String tpId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        if (phyNodeAttr.getCrossConnections() == null) {
            return node;
        }
        Iterator<CrossConnections> iter = phyNodeAttr.getCrossConnections().iterator();
        boolean found = false;
        while (iter.hasNext()) {
            CrossConnections xc = iter.next();
            if (xc.getCrossConnectionId().getValue().contains(tpId)) {
                iter.remove();
                found = true;
                break;
            }
        }

        if (found) {
            return newPhyNode(node, phyNodeAttr);
        } else {
            return node;
        }
    }

    public static Node updateTpConnectionStatus(Node node, String tpId,
            ConnectionStatus connectionStatus) {
        int pos = 0;
        TerminationPoint newTp = null;
        Iterator<TerminationPoint> iter = node.getTerminationPoint().iterator();
        while (iter.hasNext()) {
            TerminationPoint tp = iter.next();
            if (tp.getTpId().getValue().equals(tpId)) {
                iter.remove();
                newTp = PhyNodeUtil.updateConnectionStatus(tp, connectionStatus);
                break;
            }
            pos++;
        }
        if (newTp != null) {
            node.getTerminationPoint().add(pos, newTp);
        }

        return node;
    }

    public static Node addInternalLinksAndBusyTps(Node node, List<InternalLinks> internalLinks,
            Set<String> busyTpIds) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical oldPhysical =
                node.getAugmentation(Node1.class).getPhysical();
        List<InternalLinks> newInternalLinks = new ArrayList<>(oldPhysical.getInternalLinks());
        newInternalLinks.addAll(internalLinks);

        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(tp -> {
            if (busyTpIds.contains(tp.getTpId().getValue())) {
                return updateAdminAndConnectionStatus(tp, AdminStatus.Up, ConnectionStatus.Busy);
            }
            return tp;
        }).collect(Collectors.toList());

        return new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder(node.getAugmentation(Node1.class))
                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(oldPhysical)
                                .setInternalLinks(newInternalLinks)
                                .build())
                        .build())
                .setTerminationPoint(newTpList)
                .build();
    }

    private static TerminationPoint updateAdminAndConnectionStatus(TerminationPoint tp,
            AdminStatus adminStatus, ConnectionStatus connectionStatus) {
        TerminationPoint1 oldTp1 = tp.getAugmentation(TerminationPoint1.class);
        Physical phyTpAttr = oldTp1.getPhysical();
        return new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder(oldTp1)
                        .setPhysical(new PhysicalBuilder(phyTpAttr)
                                .setAdminState(adminStatus)
                                .setConnectionStatus(connectionStatus)
                                .build())
                        .build())
                .build();
    }
    /**
     * based on tpId
     * 1 clear tp connectionStatus to idle and clean properties
     * 2 remove XC based on the tp
     * 3.remove internalLink based on this TP
     *
     * @param node
     * @param tpId
     * @return
     */
    public static Node cleanTpAttr(Node node, String tpId) {
        log.debug("clean up tp attribute {}", tpId);

        if (node.getTerminationPoint() == null || node.getTerminationPoint().isEmpty()) {
            log.debug("No TPs to clean, return.");
            return node;
        }

        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(tp->{
            if (tp.getTpId().getValue().equals(tpId)) {
                return cleanTpAttr(tp);
            }
            return tp;
        }).collect(Collectors.toList());

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream().map(xc-> {
            if (xc.getCrossConnectionId().getValue().contains(tpId)) {
                return null;
            }
            return xc;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        List<InternalLinks> newIlList = nodeAttr.getInternalLinks().stream().map(il->{
            if (il.getLinkRef().contains(tpId)) {
                return null;
            }
            return il;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return new NodeBuilder(node).setTerminationPoint(newTpList)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(nodeAttr)
                                .setCrossConnections(newXcList)
                                .setInternalLinks(newIlList)
                                .build())
                        .build())
                .build();
    }



    public static Node cleanTpStateOnly(Node node, String tpId) {
        log.debug("clean tp state only {}", tpId);

        if (node.getTerminationPoint() == null || node.getTerminationPoint().isEmpty()) {
            log.debug("No TPs to clean, return.");
            return node;
        }

        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(tp -> {
            if (tp.getTpId().getValue().equals(tpId)) {
                return cleanTpAttr(tp);
            }
            return tp;
        }).collect(Collectors.toList());

        return new NodeBuilder(node).setTerminationPoint(newTpList).build();
    }


    private static TerminationPoint cleanTpAttr(TerminationPoint tp) {
        Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();

        Properties tpFrequencyPro = null;
        if (tpAttr.getProperties() != null && tpAttr.getProperties().getProperty() != null) {
            for (Property pro : tpAttr.getProperties().getProperty()) {
                if (pro.getName().equals("slot") && pro.getValue().contains("/frequency")) {
                    List<Property> propList = new ArrayList<>();
                    propList.add(new PropertyBuilder(pro).build());
                    tpFrequencyPro = new PropertiesBuilder().setProperty(propList).build();
                }
            }
        }
        TerminationPoint newTp = new TerminationPointBuilder()
                .setTpId(tp.getTpId())
                .setTpRef(tp.getTpRef())
                .setKey(tp.getKey())
                .addAugmentation(TerminationPoint1.class,
                         new TerminationPoint1Builder().setPhysical(new PhysicalBuilder(tpAttr)
                                        .setAdminState(AdminStatus.Unknown)
                                        .setConnectionStatus(ConnectionStatus.Idle)
                                        .setOtuLine(null)
                                        .setOtuClient(null)
                                        .setProperties(tpFrequencyPro == null
                                                ? new PropertiesBuilder().setProperty(Collections.emptyList()).build()
                                                : tpFrequencyPro)
                                        .build())
                                .build())
                .build();
        return newTp;
    }

    public static Node removeXc(Node node, CrossConnectionAttributes xc) {
        String xcId = xc.getCrossConnectionId().getValue();
        if (xcId.endsWith("MPO")) {
            //这个是网络层合并出来的一个假TP，对应网元上是8个MPO<1..8>, 删除网元上的XC需要转换
            return removeNeXcById(node, getNeXcId(xc));
        } else {
            return removeNeXcById(node, xcId);
        }
    }

    private static Node removeNeXcById(Node node, String xcId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        if (phyNodeAttr.getCrossConnections() == null) {
            log.error("node xcList is empty, but required remove a xc {}", xcId);
            return node;
        }
        List<CrossConnections> newXcList = new ArrayList<>(phyNodeAttr.getCrossConnections());
        newXcList.removeIf(xc->xc.getCrossConnectionId().getValue().equals(xcId));
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical newPhyNodeAttr =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(phyNodeAttr)
                .setCrossConnections(newXcList).build();
        return newPhyNode(node, newPhyNodeAttr);
    }

    private static String getNeXcId(CrossConnectionAttributes xc) {
        List<String> tpIdList = new ArrayList<>();
        List<String> xcTpIdList = new ArrayList<>();

        for (SourceTp tp : xc.getSourceTp()) {
            xcTpIdList.add(tp.getTpRef().getValue());
        }
        for (DestinationTp tp : xc.getDestinationTp()) {
            xcTpIdList.add(tp.getTpRef().getValue());
        }
        for (String xcTpId : xcTpIdList) {
            if (xcTpId.endsWith("MPO")) {
                for (int index = 1; index <= 8; index++) {
                    String tpId = xcTpId.replaceAll("MPO", "MPO" + index);
                    tpIdList.add(tpId);
                }
            } else {
                tpIdList.add(xcTpId);
            }
        }

        return NEIdGenerator.createXCId(tpIdList, true);
    }

    public static Node removeTransceiverOnPort(Node node, String tpId) {
        //Site-1651140054324#Ne-1652008769856#LINECARD-1-3#PORT-1-3-C2
        //transceiverName should be Site-1651140054324#Ne-1652008769856#TRANSCEIVER-1-3-C2

        String transceiverName = PhysicalTpIdNamingRule.getTransceiverId(tpId, PortType.OTUClient);

        return removeEquipOnNode(node, transceiverName);
    }

    public static boolean hasXConEquipment(Node node, String equipId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        for (CrossConnections xc : phyNodeAttr.getCrossConnections()) {
            if (xc.getCrossConnectionId().getValue().contains(equipId)) {
                return true;
            }
        }
        return false;
    }

    public static Node removeMuxEquip(Node node, String removedEqId) {
        log.debug("remove mux equip {}", removedEqId);

        node = removeEquipOnNode(node, removedEqId);
        node = removeEquipResource(node, removedEqId);
        return node;
    }

    public static Node markSlotEmpty(Node node, String emptyEqId) {
        log.debug("mark the slot empty {}", emptyEqId);

        if (emptyEqId.contains("LINECARD")) {
            node = emptySlotOnNode(node, emptyEqId);
        } else {
            node = removeEquipOnNode(node, emptyEqId);
        }
        node = removeXcOnNode(node, emptyEqId);
        node = removeInternalLinkOnCard(node, emptyEqId);
        node = removeEquipResource(node, emptyEqId); //port
        return node;
    }

    private static Node removeXcOnNode(Node node, String removedEqId) {
      log.debug("remove XC like {}", removedEqId);

      org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr =
              node.getAugmentation(Node1.class).getPhysical();
      if (phyNodeAttr == null || phyNodeAttr.getEquipments() == null) {
        return node;
      }

      ArrayList<CrossConnections> newXcList = new ArrayList<>(phyNodeAttr.getCrossConnections());
      newXcList.removeIf(xc -> xc.getCrossConnectionId().getValue().contains(removedEqId));
      org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical newPhyNodeAttr =
              new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                      phyNodeAttr)
                      .setCrossConnections(newXcList)
                      .setStuffed(false).build();

      return newPhyNode(node, newPhyNodeAttr);
    }

    private static Node removeEquipResource(Node node, String emptyEqId) {
        node = removeTransceiverOnCard(node, emptyEqId);
        node = removePortOnCard(node, emptyEqId);

        //应该不需要，在删除link的时候已经删除了
//        node = removeXcOnCard(node, emptyEqId);
//        node = removeInternalLinkOnCard(node, emptyEqId);
//        node = removeOcmOnCard(node, emptyEqId);
        return node;
    }

    /**
     * remove MUX and Transceiver
     *
     * @param node
     * @param removedEqId
     * @return
     */
    public static Node removeEquipOnNode(Node node, String removedEqId) {
        log.debug("remove transceiver like {}", removedEqId);

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr =
                node.getAugmentation(Node1.class).getPhysical();
        if (phyNodeAttr == null || phyNodeAttr.getEquipments() == null) {
            return node;
        }
        Equipments eq = phyNodeAttr.getEquipments().stream()
                .filter(x->x.getEquipmentId().startsWith(removedEqId))
                .findAny().orElse(null);
        if (eq == null) {
          return node;
        }
        if (eq.getEquipType().equals(EquipType.Other)) {
          return node;
        }

        ArrayList<Equipments> newEqList = new ArrayList<>(phyNodeAttr.getEquipments());
        newEqList.removeIf(x -> x.getEquipmentId().startsWith(removedEqId));

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical newPhyNodeAttr =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                        phyNodeAttr)
                        .setEquipments(newEqList)
                        .setStuffed(false).build();
        return newPhyNode(node, newPhyNodeAttr);
    }

    private static Node removeOcmOnCard(Node node, String emptyEqId) {
        String slotId = PhysicalEqpIdNamingRule.getSlotFromEquipId(emptyEqId);

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        if (phyNodeAttr.getOCMGripGroups() == null) {
            return node;
        }

        Iterator<OCMGripGroups> iter = phyNodeAttr.getOCMGripGroups().iterator();
        while (iter.hasNext()) {
            OCMGripGroups ocm = iter.next();
            if (ocm.getSlot().equals(slotId)) {
                iter.remove();
            }
        }

        return newPhyNode(node, phyNodeAttr);
    }

    private static Node removeInternalLinkOnCard(Node node, String emptyEqId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        if (phyNodeAttr.getInternalLinks() == null) {
            return node;
        }

        Iterator<InternalLinks> iter = phyNodeAttr.getInternalLinks().iterator();
        while (iter.hasNext()) {
            InternalLinks il = iter.next();
            if (il.getLinkRef().contains(emptyEqId)) {
                iter.remove();
            }
        }

        return newPhyNode(node, phyNodeAttr);
    }

    private static Node removeXcOnCard(Node node, String emptyEqId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        if (phyNodeAttr.getCrossConnections() == null) {
            return node;
        }

        Iterator<CrossConnections> iter = phyNodeAttr.getCrossConnections().iterator();
        while (iter.hasNext()) {
            CrossConnections xc = iter.next();
            if (xc.getCrossConnectionId().getValue().contains(emptyEqId)) {
                iter.remove();
            }
        }

        return newPhyNode(node, phyNodeAttr);
    }

    private static Node removePortOnCard(Node node, String removedEqId) {
        log.debug("remove TPs like {}", removedEqId);
        if (node.getTerminationPoint() == null) {
            log.error("the node hasn't TerminationPoint list. {}", node.getNodeId());
            return node;
        }
        ArrayList<TerminationPoint> newTpList = new ArrayList<>(node.getTerminationPoint());
        newTpList.removeIf(tp -> tp.getTpId().getValue().contains(removedEqId));

        return new NodeBuilder(node).setTerminationPoint(newTpList).build();
    }

    private static Node removeTransceiverOnCard(Node node, String eqId) {
        String transceiverKey = eqId.replaceFirst("LINECARD", "TRANSCEIVER");
        return removeEquipOnNode(node, transceiverKey);
    }

    private static Node emptySlotOnNode(Node node, String emptyEqId) {
        log.debug("make the equipment as empty slot {}", emptyEqId);

        List<Equipments> newEqList = new ArrayList<>();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        if (phyNodeAttr.getEquipments() == null) {
            log.error("the node hasn't equipment list. {}", node.getNodeId());
            return node;
        }
        newEqList = phyNodeAttr.getEquipments().stream().map(eq -> {
            if (eq.getEquipmentId().equals(emptyEqId)) {
                Equipments emptySlot = new EquipmentsBuilder()
                        .setEquipmentId(emptyEqId)
                        .setEquipType(EquipType.EMPTY)
                        .setImplementState(ImplementState.Allocate)
                        .setAdminState(AdminStatus.Up)
                        .setSlot(eq.getSlot())
                        .setShelf(eq.getShelf())
                        .setCreationTime(eq.getCreationTime())
                        .setEquipTypeConfiged("BLANK")
                        .setEquipTypeVendorSpecific("BLANK")
                        .setOperationalState(OperStatus.Unknown)
                        .setAlarmState(AlarmSeverity.Unknown)
                        .setAlignmentStatus(AlignmentStatusType.Unknown)
                        .setFriendlyName(String.format("SLOT-%s-%s", eq.getShelf(), eq.getSlot()))
                        .setEmpty(true)
                        .setNodeRef(eq.getNodeRef())
                        .setProperties(
                                new PropertiesBuilder().setProperty(new ArrayList<>()).build())
                        .build();
                return emptySlot;
            }
            return eq;
        }).collect(Collectors.toList());

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical newPhyNodeAttr =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                        phyNodeAttr)
                        .setEquipments(newEqList)
                        .setStuffed(false).build();
        return newPhyNode(node, newPhyNodeAttr);
    }


    private static Node newPhyNode(Node node,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr) {
        return new NodeBuilder(node)
                .addAugmentation(Node1.class,
                        new Node1Builder(node.getAugmentation(Node1.class))
                                .setPhysical(phyNodeAttr)
                                .build())
                .build();
    }

    /**
     * node's IP is empty, line card is empty
     *
     * @param node
     * @return
     */
    public static boolean isEmptyNode(Node node) {
        if (node == null) {
            return true;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        boolean empty = true;
//    if (phyNodeAttr.getIp() == null || phyNodeAttr.getIp().isEmpty()) {
        //reallocate 的情况下，必须检查每个linecard
        if (phyNodeAttr.getEquipments() == null) {
            return true;
        }
        for (Equipments eq : phyNodeAttr.getEquipments()) {
            if (eq.getEquipmentId().contains("LINECARD")) {
                if (eq.getEquipType() != null && !eq.getEquipType().equals(EquipType.EMPTY)) {
                    empty = false;
                    break;
                }
            }
        }
//    }
        return empty;
    }

    public synchronized static Node addEquipUsedSymbol(Node node, List<String> eqIdList,
            boolean used) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        List<Equipments> cfgEqList = node.getAugmentation(Node1.class).getPhysical()
                .getEquipments();
        List<Equipments> newEqList = new ArrayList<>();

        Iterator<Equipments> iter = cfgEqList.iterator();
        while (iter.hasNext()) {
            Equipments eq = iter.next();
            for (String eqId : eqIdList) {
                if (eq.getEquipmentId().equals(eqId)) {
                    iter.remove();
                    newEqList.add(new EquipmentsBuilder(eq).setUsedInLink(used).build());
                }
            }
        }
        cfgEqList.addAll(newEqList);
        return newPhyNode(node, phyNodeAttr);
    }

    public synchronized static Node addXC(Node node, CrossConnections designXc) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        Iterator<CrossConnections> iter = phyNodeAttr.getCrossConnections().iterator();
        while (iter.hasNext()) {
            CrossConnections xc = iter.next();
            if (xc.getCrossConnectionId().getValue()
                    .equals(designXc.getCrossConnectionId().getValue())) {
                iter.remove();
                break;
            }
        }
        phyNodeAttr.getCrossConnections().add(designXc);

        return newPhyNode(node, phyNodeAttr);
    }


    public static Node mergeTp(Node node, TerminationPoint designTp) {
        Iterator<TerminationPoint> iter = node.getTerminationPoint().iterator();
        while (iter.hasNext()) {
            TerminationPoint tp = iter.next();
            if (tp.getTpId().getValue().equals(designTp.getTpId().getValue())) {
                Physical phyTpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
                if (phyTpAttr.getImplementState().equals(ImplementState.Implement)) {
                    //do nothing;
                    return node;
                } else {
                    iter.remove();
                }
                break;
            }
        }

        node.getTerminationPoint().add(designTp);

        return node;
    }

    public static Node mergeEq(Node node, Equipments designEq) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        Iterator<Equipments> iter = phyNodeAttr.getEquipments().iterator();
        while (iter.hasNext()) {
            Equipments eq = iter.next();
            if (eq.getEquipmentId().equals(designEq.getEquipmentId())) {
                if (eq.getImplementState().equals(ImplementState.Implement)) {
                    //do nothing;
                    return node;
                } else {
                    iter.remove();
                }
                break;
            }
        }
        phyNodeAttr.getEquipments().add(designEq);

        return newPhyNode(node, phyNodeAttr);
    }

    public static boolean hasIp(Node node) {
        if (node == null) {
            return false;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phyNodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        return phyNodeAttr.getIp() != null;
    }
}
