package net.flex.dci.otn.controller.implement.common.ase.ne;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;

import java.util.List;

@Slf4j
public class SpecificParamNode {
    protected ChangedObject changedObject;
    protected Node node;
    protected Physical nodeAttr;
    protected RouteInfo rInfo;

    public SpecificParamNode(ChangedObject changedObject, Node node, RouteInfo rInfo) {
        this.changedObject = changedObject;
        this.node = node;
        this.rInfo = rInfo;


        nodeAttr = node.getAugmentation(Node1.class).getPhysical();
    }

    protected void done() {
        changedObject.addChangedPhyNode(node);
    }

    protected void changeChassisType() {
        Equipments chassis = nodeAttr.getEquipments().stream().filter(x->x.getEquipmentId().contains("CHASSIS")).findAny().orElse(null);
        Properties newProperties = PropertyTool.addProperty(chassis.getProperties(), "chassis-class", nodeAttr.getNodeType().equals(NodeType.TD) ? "BONE_EPC" : "BONE_OPC");
        Equipments newEq = new EquipmentsBuilder(chassis)
                .setAdminState(AdminStatus.Up)
                .setImplementState(ImplementState.Implement)
                .setProperties(newProperties)
                .build();

        log.debug("change chassis type on node: {}: {}", nodeAttr.getFriendlyName(), nodeAttr.getNodeType().equals(NodeType.TD) ? "BONE_EPC" : "BONE_OPC");
        Physical changedNodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<Equipments> eqList = changedNodeAttr.getEquipments();
        eqList.add(newEq);
        node = new NodeBuilder(node).addAugmentation(Node1.class,
                        new Node1Builder()
                                .setPhysical(new PhysicalBuilder(changedNodeAttr)
                                        .setEquipments(eqList)
                                        .build())
                                .build())
                .build();

        // Chassis is not derived from TP/link route objects; include it explicitly once.
        if (!rInfo.getEqIdList().contains(chassis.getEquipmentId())) {
            rInfo.getEqIdList().add(chassis.getEquipmentId());
        }
    }
}
