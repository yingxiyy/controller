package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

@Slf4j
public class TunnelMachine extends LinkMachine {
  private Tunnel tunnel;

  public TunnelMachine(ChangedObject changedObject, Tunnel tunnel) {
    super(changedObject);
    this.tunnel = tunnel;
  }

  public void markImpl() {
    log.trace("mark tunnel resource to impl status", tunnel.getTunnelId().getValue());
//    if (tunnel.getImplementState() == ImplementState.Implement)
//      return;

    for (Route route : tunnel.getExplictRoute().getRoute()) {
      markXcImpl(route.getPrimary().getCrossConnections());
      markEroImpl(route.getPrimary().getExplicitRouteObjects());

      if (route.getSecondary() != null) {
        markXcImpl(route.getSecondary().getCrossConnections());
        markEroImpl(route.getSecondary().getExplicitRouteObjects());
      }
    }

    Tunnel newTunnel = new TunnelBuilder(tunnel)
            .setImplementState(ImplementState.Implement)
            .setAdminState(AdminStatus.Up)
            .build();
    changedObject.addChangedTunnel(newTunnel);
  }


}
