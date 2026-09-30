/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.Constant.Reboot;
import net.flex.dci.otc.common.util.Constant.RebootState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsKey;

/**
 * @version 1.0
 */
@Slf4j
public class PhysicalNodeUtils {

    public static Node setEquipAdminState(Node newNode, NodeId nodeId, String eqId,
            AdminStatus adminState) {
        Equipments eq = new EquipmentsBuilder()
                .setEquipmentId(eqId)
                .setKey(new EquipmentsKey(eqId))
                .setAdminState(adminState)
                .build();

        if (newNode == null) {
            List<Equipments> eqList = new LinkedList();
            eqList.add(eq);
            Node1 phyNode = new Node1Builder()
                    .setPhysical(new PhysicalBuilder().setEquipments(eqList).build())
                    .build();
            Node rst = new NodeBuilder()
                    .setNodeId(nodeId)
                    .setKey(new NodeKey(nodeId))
                    .addAugmentation(Node1.class, phyNode)
                    .build();
            return rst;
        } else {
            Equipments updateEq = null;
            Physical phyNodeAttr = newNode.getAugmentation(Node1.class).getPhysical();
            List<Equipments> eqList = phyNodeAttr.getEquipments();
            Iterator<Equipments> iter = eqList.iterator();
            int pos = 0;
            while (iter.hasNext()) {
                Equipments existedEq = iter.next();
                if (existedEq.getEquipmentId().equals(eqId)) {
                    updateEq = eq;
                    iter.remove();
                    break;
                }
                pos++;
            }

            if (updateEq == null) {  //new equip
                List<Property> proList = new LinkedList<>();
                updateEq = new EquipmentsBuilder()
                        .setEquipmentId(eqId)
                        .setKey(new EquipmentsKey(eqId))
                        .setAdminState(adminState)
                        .build();
            } else {
                updateEq = new EquipmentsBuilder(updateEq)
                        .setAdminState(adminState)
                        .build();
            }
            eqList.add(pos, updateEq);
            Node1 phyNode = new Node1Builder(newNode.getAugmentation(Node1.class))
                    .setPhysical(new PhysicalBuilder(phyNodeAttr).setEquipments(eqList).build())
                    .build();
            Node rst = new NodeBuilder(newNode)
                    .addAugmentation(Node1.class, phyNode)
                    .build();

            return rst;
        }
    }

    public static Node equipProperties(Node newNode, NodeId nodeId, String equipmentId, String key,
            String value) {
        if (newNode == null) {
            return newEquipProperties(nodeId, equipmentId, key, value);
        } else {
            return addEquipProperties(newNode, equipmentId, key, value);
        }
    }

    private static Node newEquipProperties(NodeId nodeId, String eqId, String key, String value) {
        PropertyBuilder pb = new PropertyBuilder()
                .setKey(new PropertyKey(key))
                .setName(key)
                .setValue(value);
        List<Property> proList = new LinkedList<>();
        proList.add(pb.build());
        Equipments eq = new EquipmentsBuilder()
                .setEquipmentId(eqId)
                .setKey(new EquipmentsKey(eqId))
                .setProperties(new PropertiesBuilder().setProperty(proList).build())
                .build();

        List<Equipments> eqList = new LinkedList();
        eqList.add(eq);
        Node1 phyNode = new Node1Builder()
                .setPhysical(new PhysicalBuilder().setEquipments(eqList).build())
                .build();
        Node rst = new NodeBuilder()
                .setNodeId(nodeId)
                .setKey(new NodeKey(nodeId))
                .addAugmentation(Node1.class, phyNode)
                .build();
        return rst;
    }

    private static Node addEquipProperties(Node node, String equipmentId, String key,
            String value) {
        PropertyBuilder pb = new PropertyBuilder()
                .setKey(new PropertyKey(key))
                .setName(key)
                .setValue(value);

        Physical phyNodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<Equipments> tmpList = phyNodeAttr.getEquipments();
        LinkedList<Equipments> eqList = new LinkedList<>();
        eqList.addAll(tmpList);
        Equipments updateEq = null;
        Iterator<Equipments> iter = eqList.iterator();
        int pos = 0;
        while (iter.hasNext()) {
            Equipments eq = iter.next();
            if (eq.getEquipmentId().equals(equipmentId)) {
                updateEq = eq;
                iter.remove();
                break;
            }
            pos++;
        }
        if (updateEq == null) {
            List<Property> proList = new LinkedList<>();
            proList.add(pb.build());
            updateEq = new EquipmentsBuilder()
                    .setEquipmentId(equipmentId)
                    .setKey(new EquipmentsKey(equipmentId))
                    .setProperties(new PropertiesBuilder().setProperty(proList).build())
                    .build();
        } else {
            if (updateEq.getProperties() != null) {
                List<Property> proList = updateEq.getProperties().getProperty();
                proList.add(pb.build());
                updateEq = new EquipmentsBuilder(updateEq)
                        .setProperties(new PropertiesBuilder().setProperty(proList).build())
                        .build();
            }
        }
        eqList.add(pos, updateEq);
        Node1 phyNode = new Node1Builder(node.getAugmentation(Node1.class))
                .setPhysical(new PhysicalBuilder(phyNodeAttr).setEquipments(eqList).build())
                .build();
        Node rst = new NodeBuilder(node)
                .addAugmentation(Node1.class, phyNode)
                .build();

        return rst;
    }

    public static boolean checkNodeRebooting(Node ntNode) {
        List<Equipments> equipmentList = ntNode.getAugmentation(Node1.class).getPhysical()
                .getEquipments();

        for (Equipments dbEq : equipmentList) {
            if (checkEquipmentRebooting(dbEq)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkEquipmentRebooting(Equipments eq) {
        boolean rebooting = false;
        if (eq.getProperties() == null || eq.getProperties().getProperty() == null) {
            return false;
        }

        try {
            for (Property pro : eq.getProperties().getProperty()) {
                if (Reboot.ColdReboot.equals(pro.getName()) || Reboot.WarmReboot.equals(
                        pro.getName())) {
                    String rebootState = pro.getValue();
                    if (rebootState != null) {
                        if (RebootState.start.equals(rebootState) || RebootState.rebooting.equals(
                                rebootState)) {
                            rebooting = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return rebooting;
    }


    public static boolean checkEquipmentRebooting(Node ntNode, String eqId) {
        List<Equipments> equipmentList = ntNode.getAugmentation(Node1.class).getPhysical()
                .getEquipments();

        for (Equipments dbEq : equipmentList) {
            if (dbEq.getEquipmentId().equalsIgnoreCase(eqId)) {
                return checkEquipmentRebooting(dbEq);
            }
        }
        return false;
    }

    public static boolean checkEquipmentsRebooting(Node node, Set<String> equipmentIds) {
        log.debug("start to check equipments Rebooting");
        List<Equipments> equipmentList = node.getAugmentation(Node1.class).getPhysical()
                .getEquipments();
        Map<String, Equipments> equipmentsMap = equipmentList.stream()
                .collect(HashMap::new, (map, equip) -> map.put(equip.getEquipmentId(), equip),
                        HashMap::putAll);
        boolean isRebooting = false;
        for (String equipmentId : equipmentIds) {
            if (checkEquipmentRebooting(equipmentsMap.get(equipmentId))) {
                isRebooting = true;
                break;
            }
        }
        return isRebooting;
    }


    public static String getNodeFriendlyName(Node node) {
        return node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
    }

    public static String getTerminationPointFriendlyName(TerminationPoint terminationPoint) {
        return terminationPoint.getAugmentation(TerminationPoint1.class).getPhysical()
                .getFriendlyName();
    }

    public static Node buildConfigNode(String neId, String apsCrossConnectionId,
            String apsName, ApsSwitch apsSwitch) {
        log.debug("build aps switch config send to ne:{} and aps crossConnection:{}", neId,
                apsCrossConnectionId);
        CrossConnectionsBuilder crossConnectionsBuilder = new CrossConnectionsBuilder();
        crossConnectionsBuilder.setCrossConnectionId(Uri.getDefaultInstance(apsCrossConnectionId));
        Aps apsAttribute = buildAps(apsName, apsSwitch);
        crossConnectionsBuilder.setAps(apsAttribute);
        Node configNode = new NodeBuilder()
                .setNodeId(NodeId.getDefaultInstance(neId))
                .addAugmentation(Node1.class,
                        new Node1Builder().setPhysical(new PhysicalBuilder()
                                        .setCrossConnections(
                                                Collections.singletonList(crossConnectionsBuilder.build()))
                                        .build())
                                .build())
                .build();
        return configNode;
    }

    private static Aps buildAps(String apsName, ApsSwitch apsSwitch) {
        ApsBuilder apsBuilder = new ApsBuilder(apsSwitch.getAps());
        apsBuilder.setName(apsName);
        return apsBuilder.build();
    }
}
