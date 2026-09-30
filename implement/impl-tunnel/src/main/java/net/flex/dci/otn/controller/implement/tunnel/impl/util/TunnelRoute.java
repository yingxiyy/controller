package net.flex.dci.otn.controller.implement.tunnel.impl.util;

import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;

public class TunnelRoute {
  //tunnel 只有主路由，OCHP 保护是och link上的保护，它才有主备路由
  public static String getOchLinkId(Tunnel yangTunnel) throws CommonException {
    ExplictRoute explictRoute = yangTunnel.getExplictRoute();

    for (Route route : explictRoute.getRoute()) {
      Primary primary = route.getPrimary();
      for (ExplicitRouteObjects ExplicitRouteObjects : primary.getExplicitRouteObjects()) {
        for (PathRouteObject pathRouteObject : ExplicitRouteObjects.getPathRouteObject()) {
          if (pathRouteObject.getResourceType().getImplementedInterface().getName()
              .equals(Link.class.getName())) {
            Link ochLink = (Link) pathRouteObject.getResourceType();
            return ochLink.getLinkHop().getLinkRef().getValue();
          }
        }
      }
    }
    return null;
  }

}
