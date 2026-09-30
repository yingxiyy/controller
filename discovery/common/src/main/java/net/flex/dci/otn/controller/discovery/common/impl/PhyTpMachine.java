package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;

import java.util.Iterator;

@Slf4j
public class PhyTpMachine {
  private ChangedObject changedObject;
  private String tpId;

  public PhyTpMachine(ChangedObject changedObject, TpId tpId) {
    this.changedObject = changedObject;
    this.tpId = tpId.getValue();
  }

  public void markImpl() {
    log.trace("make to impl {}", tpId);

    if (tpId.endsWith("MPO")) {
      for (int i = 1; i < 9; i++) {
        String sTpId = tpId.replaceAll("-MPO", "-MPO" + i);
        markImpl(sTpId);
      }
    } else {
      markImpl(tpId);
    }
  }

  public void markImpl(String tpId) {
    String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
    Node node = changedObject.getChangedPhyNode(nodeId);

    int pos = 0;
    TerminationPoint newTp = null;
    Iterator<TerminationPoint> iter = node.getTerminationPoint().iterator();
    while (iter.hasNext()) {
      TerminationPoint neTp = iter.next();
      if (neTp.getTpId().getValue().equals(tpId)) {
        Physical tpPhyAttr = neTp.getAugmentation(TerminationPoint1.class).getPhysical();

        ImplementState implStatus = tpPhyAttr.getImplementState();
//        if (implStatus.equals(ImplementState.Implement)) {
//          //the TP has wroking in impl status
//          return;
//        }
        newTp = new TerminationPointBuilder(neTp).addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder().setPhysical(new PhysicalBuilder(tpPhyAttr)
                                        .setImplementState(ImplementState.Implement)
                                        .setAdminState(AdminStatus.Up)
                                        .build())
                                .build())
                .build();
        iter.remove();
        break;
      }
      pos++;
    }

    if (newTp != null) {
      //this TP will be mark as Impl, and related equip will be mark as impl too
      node.getTerminationPoint().add(pos, newTp);
      changedObject.addChangedPhyNode(node);
      new PhyNodeMachine(changedObject, nodeId).markImpl();
      new EquipMachine(changedObject, PhysicalTpIdNamingRule.getEquipId(tpId)).markImpl();
    }
  }
}