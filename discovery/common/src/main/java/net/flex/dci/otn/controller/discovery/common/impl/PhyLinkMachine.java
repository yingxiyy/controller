package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;

@Slf4j
public class PhyLinkMachine {
  private ChangedObject changedObject;
  private String linkId;

  public PhyLinkMachine(ChangedObject changedObject, String linkId) {
    this.changedObject = changedObject;
    this.linkId = linkId;
  }

  public void markImpl() {
    log.trace("make phy link to impl {}", linkId);

    if (linkId.endsWith("MPO")) {
      for (int i=1; i<9; i++) {
        String sLinkId = linkId.replaceAll("-MPO", "-MPO" + i);
        markImpl(sLinkId);
      }
    } else {
      markImpl(linkId);
    }
  }

  private void markImpl(String sLinkId) {
    Link link = changedObject.getChangedPhyLink(sLinkId);
    Physical linkPhyAttr = link.getAugmentation(Link1.class).getPhysical();
    ImplementState implStatus = linkPhyAttr.getImplementState();
//    if (implStatus.equals(ImplementState.Implement)) {
//      return;
//    }
    
    Link newLink = new LinkBuilder(link).addAugmentation(Link1.class,
            new Link1Builder().setPhysical(new PhysicalBuilder(linkPhyAttr)
                    .setImplementState(ImplementState.Implement)
                    .setAdminState(AdminStatus.Up)
                    .build()).build()).build();

    changedObject.addChangedPhyLink(newLink);
  }
}
