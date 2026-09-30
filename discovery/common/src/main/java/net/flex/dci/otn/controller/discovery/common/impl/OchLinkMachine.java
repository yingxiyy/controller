package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

@Slf4j
public class OchLinkMachine extends LinkMachine {
  private String linkId;

  public OchLinkMachine(ChangedObject changedObject, String linkId) {
    super(changedObject);
    this.linkId = linkId;
  }

  public void markImpl() {
    log.trace("mark och link resource to impl status", linkId);
    Link link = changedObject.getChangedOchLink(linkId);
    Och ochLinkAttr = link.getAugmentation(Link1.class).getOch();
//    if (ochLinkAttr.getImplementState().equals(ImplementState.Implement)) {
//      return;
//    }

    for (Route route : ochLinkAttr.getExplictRoute().getRoute()) {
      markXcImpl(route.getPrimary().getCrossConnections());
      markEroImpl(route.getPrimary().getExplicitRouteObjects());

      if (route.getSecondary() != null) {
        markXcImpl(route.getSecondary().getCrossConnections());
        markEroImpl(route.getSecondary().getExplicitRouteObjects());
      }
    }

    Link newLink = new LinkBuilder(link).addAugmentation(Link1.class,
                    new Link1Builder().setOch(new OchBuilder(ochLinkAttr)
                                    .setImplementState(ImplementState.Implement)
                                    .setAdminState(AdminStatus.Up)
                                    .build())
                    .build())
            .build();

    changedObject.addChangedOchLink(newLink);
  }
}
