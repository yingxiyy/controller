package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

@Slf4j
public class PhyNodeMachine {
  private ChangedObject changedObject;
  private String nodeId;

  public PhyNodeMachine(ChangedObject changedObject, String nodeId) {
    this.changedObject = changedObject;
    this.nodeId = nodeId;
  }

  public void markImpl() {
    log.trace("make to impl {}", nodeId);

    Node node = changedObject.getChangedPhyNode(nodeId);
    Physical nodePhyAttr = node.getAugmentation(Node1.class).getPhysical();
//    if (nodePhyAttr.getImplementState().equals(ImplementState.Implement)) {
//      return;
//    }
    Node newNode = new NodeBuilder(node).addAugmentation(Node1.class,
            new Node1Builder().setPhysical(new PhysicalBuilder(nodePhyAttr)
                            .setImplementState(ImplementState.Implement)
                            .setAdminState(AdminStatus.Up)
                            .build())
                    .build())
            .build();

    changedObject.addChangedPhyNode(newNode);

    //hardcode equipment CHASSIS-1 as implement
    String chassisId = node.getNodeId().getValue() + "#CHASSIS-1";
    new EquipMachine(changedObject, chassisId).markImpl();
  }
}
