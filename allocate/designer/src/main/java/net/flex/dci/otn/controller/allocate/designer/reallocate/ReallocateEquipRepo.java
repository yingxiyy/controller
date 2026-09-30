package net.flex.dci.otn.controller.allocate.designer.reallocate;

import static net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo.SHELF;
import static net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo.SLOTS_PROPERTY_NAME;
import static net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo.SLOT_DELIMITER;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsKey;
import org.springframework.stereotype.Service;

@Service
public class ReallocateEquipRepo {

    public Equipments reallocateEquip(Equipments oldEquip, String newEquipId, String newNodeId) {
        String newSlot = NEIdGenerator.getSlotFromEquipId(newEquipId);
        int newSlotInt = Integer.valueOf(newSlot).intValue();
        String newFriendlyName = NameGenerator.createEquipFriendlyName(oldEquip.getEquipTypeConfiged(), SHELF, newSlotInt);

        Properties property = reallocateProperties(oldEquip.getProperties(), newSlotInt);

        Equipments equipment = new EquipmentsBuilder(oldEquip)
                .setCreationTime(NEIdGenerator.getCurrentTime())
                .setEquipmentId(newEquipId)
                .setFriendlyName(newFriendlyName)
                .setKey(new EquipmentsKey(newEquipId))
                .setNodeRef(newNodeId)
                .setSlot(newSlot)
                .setProperties(property)
                .build();
        return equipment;
    }

    /**
     * Update slot info in property
     *
     * @param properties
     * @param newSlotInt
     * @return
     */
    private Properties reallocateProperties(final Properties properties, final int newSlotInt) {
        if (properties == null) {
            return null;
        }

        List<Property> propertyList = properties.getProperty();
        List<Property> newPropertyList = new ArrayList<>();

        for (Property property : propertyList) {
            if (property.getName().equals(SLOTS_PROPERTY_NAME)) {
                Property newSlotProperty = getSlotProperty(newSlotInt, property);
                newPropertyList.add(newSlotProperty);
                continue;
            }
            newPropertyList.add(property);

        }
        return new PropertiesBuilder().setProperty(newPropertyList).build();
    }

    private Property getSlotProperty(int newSlotInt, Property property) {
        int slotsSize = property.getValue().split(SLOT_DELIMITER).length;
        StringJoiner joiner = new StringJoiner(SLOT_DELIMITER);
        for (int i = 0; i < slotsSize; i++) {
            joiner.add(String.valueOf(newSlotInt + i));
        }
        return new PropertyBuilder().setName(SLOTS_PROPERTY_NAME).setValue(joiner.toString()).build();
    }

    /**
     * Short TransceiverName example:  e.g. TRANSCEIVER-1-3-L1, TRANSCEIVER-1-3-LINEOSC Transceiver Id example: e.g. Site-1643017493568#Ne-1643017533595#TRANSCEIVER-1-1-LINEAOSC
     *
     * @param oldTransceiver
     * @param newSlot
     * @param newNodeId
     * @return
     */
    public Equipments reallocateTransceiver(Equipments oldTransceiver, String newSlot, String newNodeId) {
        String oldTransceiverEquipId = oldTransceiver.getEquipmentId();
        String oldShortId = PhysicalEqpIdNamingRule.getShortEqupId(oldTransceiverEquipId);//e.g. TRANSCEIVER-1-3-LINEOSC
        String[] oldShortIds = oldShortId.split("-");

        String newTransceiverName = String.format("%s-%s-%s-%s", oldShortIds[0], oldShortIds[1], newSlot, oldShortIds[3]);
        String newTransceiverEquipId = PhysicalEqpIdNamingRule.createTransceiverEquipId(newNodeId, newTransceiverName);

        Equipments equipment = new EquipmentsBuilder(oldTransceiver)
                .setEquipmentId(newTransceiverEquipId)
                .setFriendlyName(newTransceiverName)
                .setKey(new EquipmentsKey(newTransceiverEquipId))
                .setNodeRef(newNodeId)
                .setSlot(newSlot)
                .build();

        return equipment;
    }

    /**
     * @param oldEquipId e.g. "Site-1643017481047#Ne-1643017566298#LINECARD-1-2"
     * @param newNodeId
     * @param newSlot
     * @return
     */
    public String reallocateEquipId(String oldEquipId, String newNodeId, String newSlot) {
        String oldShortId = PhysicalEqpIdNamingRule.getShortEqupId(oldEquipId);//e.g. LINECARD-1-2
        String[] oldShortIds = oldShortId.split("-");

        String newShortId = String.format("%s-%s-%s", oldShortIds[0], oldShortIds[1], newSlot);
        return PhysicalEqpIdNamingRule.createTransceiverEquipId(newNodeId, newShortId);
    }

    public Equipments changeEquipVendorSpecific(Equipments oldEquip, String newVendorType) {
        return new EquipmentsBuilder(oldEquip)
                .setCreationTime(NEIdGenerator.getCurrentTime())
                .setEquipTypeVendorSpecific(newVendorType)
                .build();
    }
}
