/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.common.impl;

import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;

import java.util.List;

public class LinkMachine {

  protected ChangedObject changedObject;

  public LinkMachine(ChangedObject changedObject) {
    this.changedObject = changedObject;
  }

  protected void markEroImpl(List<ExplicitRouteObjects> explicitRouteObjectsList) {
    for (ExplicitRouteObjects ero : explicitRouteObjectsList) {
      for (PathRouteObject pro : ero.getPathRouteObject()) {
        if (pro.getResourceType().getImplementedInterface().getName().equals(Link.class.getName())) {
          Link link = (Link) pro.getResourceType();
          LinkHop hop = link.getLinkHop();
          if (pro.getTopologyRef().getValue().equals(TopoNameConstants.Och_Topo_Key)) {
            new OchLinkMachine(changedObject, hop.getLinkRef().getValue()).markImpl();
          } else if (pro.getTopologyRef().getValue().equals(TopoNameConstants.Site_Topo_Key)) {
            new SiteLinkMachine(changedObject, hop.getLinkRef().getValue()).markImpl();
          } else if (pro.getTopologyRef().getValue().equals(TopoNameConstants.Phy_Topo_Key)) {
            new PhyLinkMachine(changedObject, hop.getLinkRef().getValue()).markImpl();
          }
        } else if (pro.getResourceType().getImplementedInterface().getName().equals(Tp.class.getName())) {
          Tp tp = (Tp) pro.getResourceType();
          TpHop hop = tp.getTpHop();
          if (pro.getTopologyRef().getValue().equals(TopoNameConstants.Och_Topo_Key)) {
            new OchTpMachine(changedObject, hop.getTpRef()).markImpl();
          } else if (pro.getTopologyRef().getValue().equals(TopoNameConstants.Site_Topo_Key)) {
            new SiteTpMachine(changedObject, hop.getTpRef()).markImpl();
          } else if (pro.getTopologyRef().getValue().equals(TopoNameConstants.Phy_Topo_Key)) {
            new PhyTpMachine(changedObject, hop.getTpRef()).markImpl();
          }
        }
      }
    }
  }

  protected void markXcImpl(List<CrossConnections> crossConnectionList) {
    for (CrossConnections xc : crossConnectionList) {
      new CrossConnectionMachine(changedObject, xc).markImpl();
    }
  }

}
