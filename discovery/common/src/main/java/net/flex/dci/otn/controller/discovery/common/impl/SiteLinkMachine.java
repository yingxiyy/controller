package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

@Slf4j
public class SiteLinkMachine extends LinkMachine {
  private String linkId;

  public SiteLinkMachine(ChangedObject changedObject, String linkId) {
    super(changedObject);
    this.linkId = linkId;
  }

  public void markImpl() {
    log.trace("mark site link resource to impl status", linkId);

    Link link = changedObject.getChangedOchLink(linkId);
    Site siteLinkAttr = link.getAugmentation(Link1.class).getSite();
//    if (siteLinkAttr.getImplementState().equals(ImplementState.Implement)) {
//      return;
//    }

    for (Route route : siteLinkAttr.getExplictRoute().getRoute()) {
      markXcImpl(route.getPrimary().getCrossConnections());
      markEroImpl(route.getPrimary().getExplicitRouteObjects());

      if (route.getSecondary() != null) {
        markXcImpl(route.getSecondary().getCrossConnections());
        markEroImpl(route.getSecondary().getExplicitRouteObjects());
      }
    }

    Link newLink = new LinkBuilder(link).addAugmentation(Link1.class,
                    new Link1Builder().setSite(new SiteBuilder(siteLinkAttr)
                            .setImplementState(ImplementState.Implement)
                            .setAdminState(AdminStatus.Up)
                            .build()
                    ).build())
            .build();

    changedObject.addChangedSiteLink(newLink);

  }
}
