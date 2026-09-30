package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;

import java.util.ArrayList;
import java.util.Iterator;

@Slf4j
public class EquipMachine {
  private ChangedObject changedObject;
  private String equipId;

  public EquipMachine(ChangedObject changedObject, String equipId) {
    this.changedObject = changedObject;
    this.equipId = equipId;
  }

  public void markImpl() {
    log.trace("make to impl {}", equipId);

    String nodeId = PhysicalEqpIdNamingRule.getNodeId(equipId);
    Node node = changedObject.getChangedPhyNode(nodeId);

    Physical phyNodeAttr = node.getAugmentation(Node1.class).getPhysical();
    Iterator<Equipments> iter = phyNodeAttr.getEquipments().iterator();

    int pos = 0;
    Equipments newEquip = null;
    while (iter.hasNext()) {
      Equipments neEquip = iter.next();
      if (neEquip.getEquipmentId().equals(equipId)) {
//        if (neEquip.getImplementState().equals(ImplementState.Implement)) {
//          //the EQ has in Impl status
//          return;
//        }
        newEquip = new EquipmentsBuilder(neEquip)
                .setImplementState(ImplementState.Implement)
                .setAdminState(AdminStatus.Up).build();
        iter.remove();
        break;
      }
      pos ++;
    }

    if (newEquip != null) {
      ArrayList<Equipments> newEquipList = new ArrayList<>();
      newEquipList.addAll(phyNodeAttr.getEquipments());
      newEquipList.add(pos, newEquip);

      Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
              .setPhysical(new PhysicalBuilder(phyNodeAttr)
                      .setEquipments(newEquipList)
                      .build())
              .build()).build();

      changedObject.addChangedPhyNode(newNode);
    }
  }
}
