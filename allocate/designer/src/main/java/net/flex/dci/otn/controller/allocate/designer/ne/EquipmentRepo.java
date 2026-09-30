/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.ne;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.FixEquipModel;
import net.flex.dci.otn.controller.allocate.ne.Param;
import net.flex.dci.otn.controller.allocate.ne.Port;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EquipmentRepo {

    public static final String CHASSIS = "CHASSIS";
    public static final String PANEL = "PANEL";
    public static final String CHASSIS_CLASS_PROPERTY_KEY = "chassis-class";
    @Autowired
    private JsonYangConverter jsonYangConverter;

    private static final Integer MAXIMUM_WIDTH_SLOT = 2;
    public static final String SLOT_DELIMITER = ",";
    public static final String SLOTS_PROPERTY_NAME = "slots";
    public static final String CHASSIS_CARD_TYPE = "CHASSIS";
    public static Integer SHELF = 1;


    public Equipments createCardEquipment(String nodeId, Card card, Integer slot, NeInfo neInfo) throws NeDesignerException {

        EquipType equipType = jsonYangConverter.getEquipTypeByCardClassAndType(
                neInfo.getVendor(), neInfo.getProductType(),
                neInfo.getCardClassByCardVendor(card.getVendorType()),
                card.getCardType());
        Set<Integer> equipUsedSlots = getEquipUsedSlots(slot, card.getWidth(), card.getHeight());
        boolean isReplacedAsEmpty = neInfo.isReplacedAsEmpty(card.getVendorType());

        //create card equipment
        return createEquipment(nodeId, equipType, card, slot, equipUsedSlots, isReplacedAsEmpty);

    }

    public List<Equipments> createCardTransceivers(String nodeId, Card card, Integer slot, NeInfo neInfo) throws NeDesignerException {
        return neInfo.getPort(card.getCardType()).entrySet().stream()
                .filter(portEntry -> portEntry.getValue().getTransceiver() != null && portEntry.getValue().getTransceiver().getInitiated())
                .map(portEntry -> {
                    try {
                        return createPortTransceiver(nodeId, portEntry, slot);
                    } catch (NeDesignerException e) {
                        log.error("Failed to create transceiver for nodeId:{}, portName:{}, slot:{}", nodeId, portEntry.getKey(), slot, e);
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    private Equipments createPortTransceiver(String nodeId, Entry<String, Port> portEntry, Integer slot) throws NeDesignerException {
        String portName = portEntry.getKey();
        Port port = portEntry.getValue();
        String transceiverName = NameGenerator.getTransceiverName(port.getTransceiver().getName(), portName, slot, SHELF);
        return createTransceiver(nodeId, transceiverName, transceiverName);//by default, use transceiverName as friendlyName
    }


    public Equipments createTransceiver(String nodeId, String transceiverName, String transceiverFriendlyName) throws NeDesignerException {
        return createTransceiver(nodeId, transceiverName, null, transceiverFriendlyName);
    }

    /**
     * @param nodeId
     * @param transceiverName e.g. TRANSCEIVER-1-3-L1, TRANSCEIVER-1-3-LINEOSC
     * @return
     */
    public Equipments createTransceiver(String nodeId, String transceiverName, Properties properties, String transceiverFriendlyName) throws NeDesignerException {
        Integer slot;
        try {
            slot = Integer.parseInt(transceiverName.split("-")[2]);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new NeDesignerException(
                    "Failed to create transceiver, because failed to get slot from invalid transceiverName: "
                            + transceiverName, e);
        }

        String eqpId = NEIdGenerator.createTransceiverEquipId(nodeId, transceiverName);
        if (transceiverFriendlyName == null) {
            transceiverFriendlyName = transceiverName;
        }

        Equipments equipment = new EquipmentsBuilder().setAdminState(AdminStatus.Unknown)
                .setAlarmState(AlarmSeverity.Unknown)
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .setCreationTime(NEIdGenerator.getCurrentTime())
                .setEquipmentId(eqpId)
                .setEquipType(EquipType.TRANSCEIVER)
                .setEquipTypeConfiged(EquipType.TRANSCEIVER.name())
                .setFriendlyName(transceiverFriendlyName)
                .setImplementState(ImplementState.Allocate)
                .setOperationalState(OperStatus.Unknown)
                .setKey(new EquipmentsKey(eqpId))
                .setNodeRef(nodeId)
                .setSlot(slot == null ? null : String.valueOf(slot))
                .setEmpty(false)
                .setShelf(String.valueOf(SHELF))
                .setProperties(properties == null ? NameGenerator.getFakeProperty() : properties)
                .build();

        return equipment;
    }


    public Equipments createEmptyEquipment(String nodeId, Integer slot, Card emptyCard) {
        return createEquipment(nodeId, EquipType.EMPTY, emptyCard, slot, new HashSet<>(Arrays.asList(slot)), true);
    }

    public Equipments createEquipment(String nodeId, EquipType equipType, Card cardInfo, Integer slot, Set<Integer> equipUsedSlots, Boolean isReplacedAsEmpty) {
        String cardType = cardInfo.getCardType();
        String vendorSpecific = cardInfo.getVendorType();
        String eqpId = NEIdGenerator.createEquipId(nodeId, equipType, cardType, slot, SHELF, isReplacedAsEmpty);
        String friendlyName = NameGenerator.createEquipFriendlyName(cardType, SHELF, slot);

        Properties property = createDefaultProperty(equipType, equipUsedSlots, isReplacedAsEmpty, cardInfo);

        Equipments equipment = new EquipmentsBuilder()
                .setAdminState(cardInfo.getAdminEnable() ? AdminStatus.Up : AdminStatus.Unknown)
                .setAlarmState(AlarmSeverity.Unknown)
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .setCreationTime(NEIdGenerator.getCurrentTime())
                .setEquipmentId(eqpId)
                .setEquipType(equipType)
                .setEquipTypeConfiged(cardType)
                .setEquipTypeVendorSpecific(vendorSpecific == null ? cardType : vendorSpecific)//UI要求的，为了画面板图
                .setFriendlyName(friendlyName)
                .setImplementState(ImplementState.Allocate)
                .setOperationalState(OperStatus.Unknown)
                .setKey(new EquipmentsKey(eqpId))
                .setNodeRef(nodeId)
                .setSlot(slot == null ? null : String.valueOf(slot))
                .setEmpty(equipType.equals(equipType.EMPTY))
                .setShelf(String.valueOf(SHELF))
                .setProperties(property)
                .build();
        return equipment;
    }

    private Properties createDefaultProperty(EquipType equipType, Set<Integer> equipUsedSlots, Boolean isReplacedAsEmpty, Card cardInfo) {
        List<Property> properties = new ArrayList<>();
        List<Param> params = cardInfo.getParams();
        if (isReplacedAsEmpty && !equipType.equals(EquipType.EMPTY)) {
            properties.add(convertUsedSlotsToProperty(equipUsedSlots));
            if (params != null) {
                for (Param param : params) {
                    properties.add(new PropertyBuilder().setName(param.getName()).setValue(param.getValue()).build());
                }
            }
        }
        properties.add(new PropertyBuilder().setName("WdmBand").setValue(cardInfo.getWdmBand()).build());
        return new PropertiesBuilder().setProperty(properties).build();
    }

    private Property convertUsedSlotsToProperty(Set<Integer> equipUsedSlots) {
        return new PropertyBuilder().setName(SLOTS_PROPERTY_NAME).setValue(equipUsedSlots.stream().map(String::valueOf).collect(Collectors.joining(SLOT_DELIMITER))).build();
    }

    private Set<Integer> convertPropertyToUsedSlots(Property equipUsedSlots) {
        Set<Integer> result = new HashSet<>();

        for (String slot : equipUsedSlots.getValue().split(SLOT_DELIMITER)) {
            result.add(Integer.parseInt(slot));
        }
        return result;
    }

    /**
     * 根据约定，equipment的展示顺序应该是： 1. Chassis， 2. 板卡（板卡槽位，数量就是emptyCard决定的） 3。其他业务卡，比如Mux,Mux panel 4. Panel 5. 其他辅助卡，比如FAN,PSU这些
     *
     * Note: 为了保证equip顺序统一，创建的时候就把空卡创建好，后面只需要替换即可
     *
     * @param nodeId
     * @param fixEquipModels
     * @param emptyCard
     * @return
     * @throws NeDesignerException
     */
    public List<Equipments> createFixedEquipment(String nodeId, List<FixEquipModel> fixEquipModels, Card emptyCard) throws NeDesignerException {

        Equipments chassis = null;
        List<Equipments> fixEquips = new ArrayList<>();
        for (FixEquipModel fixEquipModel : fixEquipModels) {
            List<String> equipNames = NeInfoUtil.getNameList(fixEquipModel.getName());

            for (String equipName : equipNames) {
                String eqpId = NEIdGenerator.createEquipIdForFix(nodeId, equipName);

                Equipments equip = new EquipmentsBuilder()
                        .setAdminState(AdminStatus.Unknown)
                        .setAlarmState(AlarmSeverity.Unknown)
                        .setAlignmentStatus(AlignmentStatusType.Unknown)
                        .setCreationTime(CommonUtil.converDateAndTime(new Date()))
                        .setEquipmentId(eqpId)
                        .setEquipType(EquipType.Other)
                        .setEquipTypeConfiged(fixEquipModel.getEquipTypeConfiged())
                        .setEquipTypeVendorSpecific(fixEquipModel.getEquipTypeConfiged())
                        .setFriendlyName(equipName)
                        .setImplementState(ImplementState.Allocate)
                        .setOperationalState(OperStatus.Unknown)
                        .setKey(new EquipmentsKey(eqpId))
                        .setNodeRef(nodeId)
                        .setEmpty(false)
                        .build();
                if (equip.getEquipTypeConfiged().equals(CHASSIS)) {
                    chassis = new EquipmentsBuilder(equip).setProperties(new PropertiesBuilder().setProperty(
                                    Arrays.asList(
                                            new PropertyBuilder().setName(CHASSIS_CLASS_PROPERTY_KEY)
                                                    .setValue(fixEquipModel.getChassisClass()).build()))
                            .build()).build();
                    continue;
                }
                if (equip.getEquipTypeConfiged().equals(PANEL)) {
                    fixEquips.add(0, equip);//put PANEL at firstly
                    continue;
                }
                fixEquips.add(equip);
            }

        }

        List<Equipments> equipments = new ArrayList<>();
        //add chassis at first
        if (chassis != null) {
            equipments.add(chassis);
        } else {
            log.warn("Failed to create Chassis, because no definition found in json file.");
        }
        //create empty card if needed
        List<Integer> emptyCardSlots = emptyCard.getPossibleSlot();
        for (Integer slot : emptyCardSlots) {
            equipments.add(createEmptyEquipment(nodeId, slot, emptyCard));
        }
        equipments.addAll(fixEquips);

        return equipments;
    }


    public Set<Integer> getEquipUsedSlots(Integer equipSlot, int width, int height) {

        Set<Integer> usedSlot = new HashSet<>();
        Set<Integer> widthSlots = new HashSet<>();

        for (int i = width; i > 0; i--) {
            widthSlots.add(equipSlot++);
        }
        usedSlot.addAll(widthSlots);

        for (int h = 1; h < height; h++) {
            for (Integer widthSlot : widthSlots) {
                usedSlot.add(widthSlot + MAXIMUM_WIDTH_SLOT);
            }
        }
        return usedSlot;
    }


    /**
     * Only return linecard usedSlots, and muxPanel
     *
     * @param equipments
     * @return
     */
    public Set<Integer> getLineCardUsedSlots(Equipments equipments) {
        if(equipments.getEquipType().equals(EquipType.MUXPANEL)){
            return new HashSet<>(Arrays.asList(Integer.parseInt(equipments.getSlot())));
        }
        if (equipments.getProperties() == null || equipments.getProperties().getProperty() == null) {
            return Collections.EMPTY_SET;
        }

        for (Property property : equipments.getProperties().getProperty()) {
            if (property.getName().equals(SLOTS_PROPERTY_NAME)) {
                return convertPropertyToUsedSlots(property);
            }
        }
        return Collections.EMPTY_SET;
    }

    public static Equipments getEquipment(Node node, String equipmentId) throws NeDesignerException {
        if (equipmentId == null) {
            throw new NeDesignerException("Failed to get equipment by null equipmentId");
        }
        try {
            return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(item -> item.getEquipmentId().equals(equipmentId)).findFirst().get();
        } catch (NullPointerException | NoSuchElementException e) {
            log.error("Failed to get equipment from node :%n{} %nby equipmentId:%n", node, equipmentId, e);
            throw new NeDesignerException(String.format("Failed to get equipment from node %s by equipmentId:%s", node, equipmentId), e);
        }
    }

    public String getCardVendorType(Equipments item) throws NeDesignerException {
        try {
            return item.getEquipTypeVendorSpecific();
        } catch (IllegalArgumentException e) {
            throw new NeDesignerException("Failed to get card vendor type by equipment: " + item, e);
        }
    }

    public static boolean isTransceiverExisted(Node node, String transceiverName) {
        return node.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(item -> (item.getEquipType() != null && item.getEquipType().equals(EquipType.TRANSCEIVER))
                        && (item.getEquipmentId() != null && item.getEquipmentId().contains(transceiverName)))
                .count() > 0;
    }

//    public List<Equipments> replaceAsEmptyEquips(Equipments equipment) {
//        Set<Integer> equipSlots = getEquipUsedSlotForEmpty(equipment);
//        List<Equipments> emptyEquipments = new ArrayList<>();
//        String nodeId = equipment.getNodeRef();
//        for (Integer slot : equipSlots) {
//            emptyEquipments.add(createEmptyEquipment(nodeId, slot, NeInfo.EMPTY_CARD_TYPE));
//        }
//        log.trace("Return empty equipments:{}\n, for :{}", emptyEquipments.stream().map(String::valueOf).collect(Collectors.joining(",")), equipment);
//        return emptyEquipments;
//    }

    public Set<Integer> getEquipUsedSlotForEmpty(Equipments equipments) {
        for (Property property : equipments.getProperties().getProperty()) {
            if (property.getName().equals(SLOTS_PROPERTY_NAME)) {
                return convertPropertyToUsedSlots(property);
            }
        }
        return Collections.EMPTY_SET;
    }

    public Equipments createPanel(String nodeId, Card panelCardInfo) {
        return createEquipment(nodeId, EquipType.Other, panelCardInfo, panelCardInfo.getPossibleSlot().get(0), null, false);
    }

    /**
     * 根据约定，equipment的展示顺序应该是： 1. Chassis， 2. 板卡（板卡槽位，数量就是emptyCard决定的） 3。其他业务卡，比如Mux,Mux panel 4. Panel 5. 其他辅助卡，比如FAN,PSU这些
     *
     * 所以这个方法提供的index就是给选项3，其他业务卡使用。 这个index，也就是最后一张板卡的后面
     *
     * @param emptyCard
     * @return
     */
    public int getTrafficCardIndex(Card emptyCard) {
        emptyCard.getPossibleSlot().stream().filter(slot -> slot != 50 && slot != 51);

        //因为index 0，默认给chassis了，所以板卡的index正好和槽位一致
        return emptyCard.getPossibleSlot().get(emptyCard.getPossibleSlot().size() - 1) + 1;

    }

    public Equipments createEquipment(String nodeId, Integer equipSlot, String cardVendorType, NeInfo neInfo) throws NeDesignerException {
        Card cardInfo = neInfo.getCardByCardVendor(cardVendorType);
        Set<Integer> newEquipSlots = getEquipUsedSlots(equipSlot, cardInfo.getWidth(), cardInfo.getHeight());
        String cardClass = neInfo.getCardClassByCardVendor(cardVendorType);
        EquipType equipType = EquipType.valueOf(cardClass);
        boolean isReplacedAsEmpty = neInfo.isReplacedAsEmpty(cardVendorType);

        return createEquipment(nodeId, equipType, cardInfo, equipSlot, newEquipSlots, isReplacedAsEmpty);
    }
}
