package net.flex.dci.otn.controller.allocate.designer.reallocate;

import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.springframework.stereotype.Service;

@Service
public class ReallocateTpRepo {

    public TerminationPoint reallocateTp(TerminationPoint tp, Equipments newEquip) {
        String oldTpId = tp.getTpId().getValue();
        String portName = PhysicalTpIdNamingRule.getPortNameByTpId(oldTpId);

        String newFriendlyName = NameGenerator.createTpFriendlyName(newEquip, portName);
        String newTpIdValue = NEIdGenerator.createTPId(newEquip, portName);

        TpId newTpId = new TpId(newTpIdValue);

        return new TerminationPointBuilder().setTpId(newTpId)
                .setKey(new TerminationPointKey(newTpId))
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder()
                                .setPhysical(new PhysicalBuilder(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                                        .setEquipmentRef(newEquip.getEquipmentId())
                                        .setNodeRef(newEquip.getNodeRef())
                                        .setFriendlyName(newFriendlyName)
                                        .build())
                                .build())
                .build();

    }

    /**
     * tpId example: "Site-1643017493568#Ne-1643017533595#LINECARD-1-1#PORT-1-1-LINEA",
     *                "Site-1673858334103#Ne-1689055882187#LINECARD-1-3#PORT-1-3-1-APSS"
     *
     * @param oldTpId
     * @param newEquipId
     * @return
     */
    public String getNewTpId(String oldTpId, String newEquipId) {
        String portName=PhysicalTpIdNamingRule.getPortNameByTpId(oldTpId);

        return PhysicalTpIdNamingRule.createTPId(newEquipId,portName);
    }

    /**
     * @param tp e.g. "Site-1643017481047#Ne-1643017533583#PANEL-1-40#PORT-1-40-MUX"
     * @param newEquipId e.g. "Site-1643017481047#Ne-1643017533583#MUXPANEL-1-50", "Site-1636955223010#Ne-1636955266316#MUX-1-50"
     * @return
     */
    public String getNewPanelTpId(String tp, String newEquipId) {
        String newNodeId = PhysicalEqpIdNamingRule.getNodeId(newEquipId);
        String[] oldTpArray = tp.split("#");
        String muxCardSlot = PhysicalEqpIdNamingRule.getSlotFromEquipId(newEquipId);

        //todo: 这里写死了，没有通过配置文件配置了，直接写死mux的slot是50，就连panel的MXU，否则连panel的MUX1
        if (muxCardSlot.equals("50")) {
            return String.format("%s#%s#%s", newNodeId, oldTpArray[2], oldTpArray[3]);
        }
        return String.format("%s#%s#%s", newNodeId, oldTpArray[2], oldTpArray[3]+"1");
    }
}
