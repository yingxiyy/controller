/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.lifecycle.PhyResourceType.tp;
import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.convert;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.Constant.Reboot;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.common.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.implement.physical.nbi.IEquipment;
import net.flex.dci.otn.controller.implement.physical.util.Constants;
import net.flex.dci.otn.controller.implement.physical.util.PhysicalNodeUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.equip.attributes.Fan;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.equip.input.Equipments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/9 15:06
 */
@Slf4j
@Component
public class Equipment extends BaseImpl implements IEquipment {

    //    @Autowired
//    private EquipmentsDao equipmentsDao;
    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private EquipmentsDao equipmentsDao;

    @Autowired
    private NeManagerRpc neManagerRpc;

    public UpdateEquipOutput updatePhyEquipment(UpdateEquipInput input) throws CommonException {
        //assuming the ne Id is only have one and equipment id only have onr
        String neId = input.getEquipments().get(0).getNodeRef();
        String equipmentId = input.getEquipments().get(0).getEquipmentId();
        log.debug("start to update phy equipment ,ne id is {},equipment id is {}", neId,
                equipmentId);
        validEquipmentUpdateParams(input);
        AsynchronousExecutor.execute(() -> {
            updateEquipment(input);
        });

        return super.updatePhyEquipment(input);
    }

    /**
     * valid update equipment params
     *
     * @param input
     */
    private void validEquipmentUpdateParams(UpdateEquipInput input) {
        log.debug("start to valid update equipment update params");
        if (input.getEquipments().size() > 1) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the method only support change one equipment");
        }

        Equipments equipment = input.getEquipments().get(0);
        String refNodeId = equipment.getNodeRef();
        boolean isRegistered = phyNodeDao.existsOpNode(refNodeId);
        if (!isRegistered) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    "the device should be supervision");
        }
        //check equipment id is valid
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments equipments = equipmentsDao.getEquipmentByNodeAndEqId(
                refNodeId,
                equipment.getEquipmentId());
        if (equipments == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the equipment is invalid");
        }

        //checkCustomInfo length
        Properties properties = equipment.getProperties();
        if (properties != null) {
            String customInfo = PropertyTool.getValue(properties,
                    Constants.CUSTOM_INFO);
            if (customInfo != null) {
                if (customInfo.trim().length() > Constants.CUSTOMER_INFO_LENGTH) {
                    log.error("equipment :{},custom-info is larger than {} characters", tp,
                            Constants.CUSTOMER_INFO_LENGTH);
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "equipment custom info should be less than "
                                    + Constants.CUSTOMER_INFO_LENGTH);
                }

            }
        }
        //check the equipment is rebooting or nothing
        Node phyNode = phyNodeDao.getOpPhyNodeById(refNodeId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments refEquipments = getRefEquipment(
                phyNode, equipment.getEquipmentId());

        if (PhysicalNodeUtils.checkEquipmentRebooting(refEquipments)) {
            throw new CommonException(CommonExceptionType.NO_PERMISSION,
                    "required equipment " + equipment.getEquipmentId() + "is rebooting");
        }
        //if fan is not null
        Fan fan = equipment.getFan();
        if (fan != null) {
            String speedGrade = fan.getSpeedGrade();
            if (speedGrade == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the fan speed grade should not be null");
            }
        }
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments getRefEquipment(
            Node phyNode, String equipmentId) {
        log.debug("get update equipments");
        Physical physical = phyNode.getAugmentation(
                Node1.class).getPhysical();
        String friendlyName = physical.getFriendlyName();
        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> equipmentsMap =
                physical.getEquipments().stream().collect(
                        HashMap::new,
                        (map, equipments) -> map.put(equipments.getEquipmentId(), equipments),
                        HashMap::putAll);
        if (equipmentsMap.containsKey(equipmentId)) {
            return equipmentsMap.get(equipmentId);
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne name :" + friendlyName + " hasn't have the card:" + equipmentId);
        }
    }

    private void updateEquipment(UpdateEquipInput input) {
        log.debug("config equipment properties is {}", input);

        //assume there only support config on equipment
        Equipments refEquipment = input.getEquipments().get(0);
        String refNodeId = refEquipment.getNodeRef().trim();
        Node phyNode = null;
        try {
            phyNode = phyNodeDao.getConfigPhyNodeById(refNodeId);
        } catch (Exception e) {
            log.error("cannot get node", e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the phy node {0} is node exists", refNodeId));
        }
        if (phyNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the phy node {0} is node exists", refNodeId));
        }

        taskInfoMessage.setResourceId(refNodeId);

        String nodeName = phyNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        String eqName = getEquipmentFriendlyName(phyNode, refEquipment);
        String resourceName = nodeName + "/" + eqName;
        try {
            if (refEquipment.getAdminState() != null) {
                updateConfigEquipmentAdminState(refEquipment, refNodeId);
            }
            if (refEquipment.getProperties() != null) {
                updateConfigEquipmentProperties(refEquipment, refNodeId);
            }
            if (refEquipment.getFan() != null) {
                updateConfigFanEquipmentSpeed(refEquipment, refNodeId);
            }
            logMessage(BroadCastConstant.UPDATE_EQ, resourceName, BLANK);
        } catch (Exception ex) {
            log.error("failed to update equipment for the equip,reason is:{}", ex.getMessage(), ex);
            logMessage(BroadCastConstant.UPDATE_EQ, resourceName, ex.getMessage());
        }

//        for (Equipments equipments : input.getEquipments()) {
//            String nodeId = equipments.getNodeRef();
//            Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
//            if (equipments.getProperties() != null) {
//                configEquipmentProperties(equipments, node);
//            }
//            if (equipments.getAdminState() != null) {
//                configEquipmentAdminState(equipments, node);
//            }
//        }
    }

    private String getEquipmentFriendlyName(Node phyNode, Equipments refEquipment) {
        Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> opData = phyNode.getAugmentation(
                        Node1.class).getPhysical().getEquipments().parallelStream()
                .filter(x -> x.getEquipmentId().equals(refEquipment.getEquipmentId())).findAny();
        if (opData.isPresent()) {
            return opData.get().getFriendlyName();
        }
        return null;
    }

    private void updateConfigFanEquipmentSpeed(Equipments equipments, String phyNodeId) {
        log.debug("start to update config fan equipment speed");
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(phyNodeId));
        nodeBuilder.setKey(new NodeKey(NodeId.getDefaultInstance(phyNodeId)));
        nodeBuilder.addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder().setEquipments(
                                Collections.singletonList(updateEquipments(equipments))
                        ).build())
                .build());

        writeToNe(nodeBuilder.build());
    }

    /**
     * config update config equipment properties support properties is custom-info cold reboot warm
     * reboot
     */
    private void updateConfigEquipmentProperties(Equipments equipments, String phyNodeId) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(phyNodeId));
        nodeBuilder.setKey(new NodeKey(NodeId.getDefaultInstance(phyNodeId)));
        nodeBuilder.addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder().setEquipments(
                                Collections.singletonList(updateEquipments(equipments))
                        ).build())
                .build());

        writeToNe(nodeBuilder.build());

    }


    /**
     * config update config equipment admin state
     *
     * @param phyNodeId
     */
    private void updateConfigEquipmentAdminState(Equipments equipments, String phyNodeId) {
        log.debug("config equipment admin state");
        log.debug("start to send the configuration to ne, ne id :{}",
                phyNodeId);
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(phyNodeId));
        nodeBuilder.setKey(new NodeKey(NodeId.getDefaultInstance(phyNodeId)));
        nodeBuilder.addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder().setEquipments(
                                Collections.singletonList(updateEquipments(equipments))
                        ).build())
                .build());

        writeToNe(nodeBuilder.build());
    }

    /**
     * config ne equipments admin state
     *
     * @param equipments
     * @param node
     */
    private void configEquipmentAdminState(Equipments equipments,
            Node node) {
        //todo: if ne if implement
        log.debug("start to config the ne :{},equipments id admin state {}",
                node.getNodeId().getValue(), equipments.getAdminState());
        ImplementState state = getEquipImplState(
                node.getAugmentation(Node1.class).getPhysical().getEquipments(), equipments);
        AdminStatus adminState = equipments.getAdminState();

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments updateEquip
                = new EquipmentsBuilder(getEquip(node, equipments.getEquipmentId()))
                .setKey(new EquipmentsKey(equipments.getEquipmentId()))
                .setAdminState(adminState)
                .build();
        saveEquipments(updateEquip, node);
        writeConfigPropertiesToNe(node, updateEquip);
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments getEquip
            (
                    Node node, String equipId) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> equipList =
                new ArrayList<>(
                        node.getAugmentation(Node1.class).getPhysical().getEquipments());
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments eq : equipList) {
            if (eq.getEquipmentId().equalsIgnoreCase(equipId)) {
                return eq;
            }
        }
        return null;
    }

    private void updateEquipment(
            List
                    <org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments
                            > equiqList,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments
                    updatedEquip) {
        Iterator<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> it = equiqList.iterator();
        while (it.hasNext()) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments eq = it.next();
            if (updatedEquip.getEquipmentId().equalsIgnoreCase(eq.getEquipmentId())) {
                it.remove();
            }
        }
        equiqList.add(updatedEquip);
    }

    private void saveEquipments(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments
                    updateEquip,
            Node node) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments> equipList =
                new ArrayList<>(
                        node.getAugmentation(Node1.class).getPhysical().getEquipments());
        updateEquipment(equipList, updateEquip);
        NodeBuilder nodeBuilder = new NodeBuilder(node).addAugmentation(Node1.class,
                new Node1Builder(node.getAugmentation(Node1.class))
                        .setPhysical(
                                new PhysicalBuilder(
                                        node.getAugmentation(Node1.class).getPhysical())
                                        .setEquipments(equipList).build()).build());

        ChangedObject changedObject = new ChangedObject();
        changedObject.addChangedPhyNode(nodeBuilder.build());
//        equipmentsDao.mergeEquipments(node.getNodeId().getValue(), updateEquip);
        MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
                MultipleTransaction.class);
        mongoTransaction.save(changedObject);
    }

    /**
     * config equipments properties
     *
     * @param equipments
     * @param node
     */
    private void configEquipmentProperties(Equipments equipments,
            Node node) {
        log.debug("start to config equipments properties ,equipment id is {},node id is {}",
                equipments.getEquipmentId(), node.getNodeId().getValue());
        String nodeId = equipments.getNodeRef();
        ImplementState state = getEquipImplState(
                node.getAugmentation(Node1.class).getPhysical().getEquipments(), equipments);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder
                updateEquipBuilder
                = new EquipmentsBuilder(getEquip(node, equipments.getEquipmentId()))
                .setKey(new EquipmentsKey(equipments.getEquipmentId()));
        for (Property prop : equipments.getProperties().getProperty()) {
            if (prop.getName().equalsIgnoreCase(Constant.EquipmentCustomerInfo)) {
//                PropertyBuilder pb = new PropertyBuilder()
//                        .setKey(new PropertyKey(prop.getName()))
//                        .setName(prop.getName())
//                        .setValue(prop.getValue());

                updateEquipProp(updateEquipBuilder, prop);
//                List<Property> proList = new LinkedList<>();
//                proList.add(pb.build());

//                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments updateEquip = new EquipmentsBuilder()
//                        .setKey(new EquipmentsKey(equipments.getEquipmentId()))
//                        .setProperties(new PropertiesBuilder().setProperty(proList).build())
//                        .build();

//                equipmentsDao.mergeEquipments(nodeId, updateEquip);
            } else if (prop.getName().equalsIgnoreCase(Reboot.ColdReboot) || prop.getName()
                    .equalsIgnoreCase(Reboot.WarmReboot)) {

            } else {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "not supported param " + prop.getName());
            }
        }

        saveEquipments(updateEquipBuilder.build(), node);
    }

    private void updateEquipProp(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder
                    eqBuilder,
            Property prop) {
        List<Property> props = new ArrayList<Property>();
        if (eqBuilder.getProperties() != null) {
            props.addAll(eqBuilder.getProperties().getProperty());
        }
        Iterator<Property> it = props.iterator();
        while (it.hasNext()) {
            Property pro = it.next();
            if (pro.getKey().equals(prop.getKey())) {
                it.remove();
            }
        }
        props.add(prop);
    }

    private ImplementState getEquipImplState(
            List
                    <org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments
                            > eqList,
            Equipments eqp) {
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments eq : eqList) {
            if (eq.getEquipmentId().equalsIgnoreCase(eqp.getEquipmentId())) {
                return ImplementState.Implement;
            }
        }
        return ImplementState.Plan;
    }

    private void writeConfigPropertiesToNe(Node node,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments
                    equipments) {
        log.debug("start to send the configuration to ne, ne id :{}",
                node.getNodeId().getValue());
        NodeBuilder updateNodeBuilder = new NodeBuilder(node);
        Node1 phyNode = new Node1Builder(node.getAugmentation(Node1.class))
                .setPhysical(
                        new PhysicalBuilder().setEquipments(Collections.singletonList(equipments))
                                .build())
                .build();
        updateNodeBuilder.addAugmentation(Node1.class, phyNode);
        writeToNe(updateNodeBuilder.build());
    }


    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments updateEquipments(
            Equipments equipment) {
        //change the update equipment taskType
        //log current update equipments action type should be changed or not
        if (isReboot(equipment)) {
            taskInfoMessage.setActionType(ActionType.deviceReboot);
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments updateEquipments = new EquipmentsBuilder()
                .setEquipmentId(equipment.getEquipmentId())
                .setKey(new EquipmentsKey(new EquipmentsKey(equipment.getEquipmentId())))
                .setAdminState(equipment.getAdminState())
                .setProperties(equipment.getProperties())
                .setFan(equipment.getFan())
                .build();
        return updateEquipments;
    }

    private boolean isReboot(Equipments equipment) {
        log.debug("detecting update equipment is reboot or not");
        //reboot state only exits in operational data
        if (equipment.getProperties() == null || equipment.getProperties().getProperty() == null) {
            log.debug("the operation is not reboot operation");
            return false;
        }
        List<Property> properties = equipment.getProperties().getProperty();
        return properties.stream()
                .map(Property::getName)
                .anyMatch(name -> name.contains(Reboot.ColdReboot)
                        || name.contains(Reboot.WarmReboot));
    }

    private void writeToNe(Node node) {
        log.info("start to write config to the end");
        ConfigNeOutput result = neManagerRpc.configNe(node);
        if (result.getFailObj() != null && result.getFailObj().getObject() != null) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    convert(result.getFailObj()));
        }
    }
}
