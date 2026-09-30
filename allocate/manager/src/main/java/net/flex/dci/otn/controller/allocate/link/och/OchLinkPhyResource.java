/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.och;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.link.common.LinkPhyResource;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class OchLinkPhyResource extends LinkPhyResource {

  public OchLinkPhyResource(ChangedObject changedObject) {
    super(changedObject);
  }

  public void updatePhyLink(RouteInfo info, String planeName, String planeId) {
    //把涉及到的link都创建为物理连接, 主备link中都有siteLink的可能，需要剔除
    List<Link> underLayerLinks = info.getMain().getLinks();
    if (info.getSlave() != null && !info.getSlave().getLinks().isEmpty()) {
      underLayerLinks.addAll(info.getSlave().getLinks());
    }
    if (info.getThird() != null && !info.getThird().getLinks().isEmpty()) {
      underLayerLinks.addAll(info.getThird().getLinks());
    }

    PhyLinkUtil phyLinkUtil = new PhyLinkUtil(changedObject);
    for (Link link : underLayerLinks) {
      if (SiteLinkIdNamingRule.isSiteLink(link.getLinkId().getValue())) {
        continue;
      }

      phyLinkUtil.addPhyLink(link, planeName, planeId);
    }
  }

  protected List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> getAllRouteXCs(Link link) {
    Link1 ochLink = link.getAugmentation(Link1.class);

    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcList = new ArrayList<>();
    for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route linkRoute : ochLink.getOch().getExplictRoute().getRoute()) {
      for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc : linkRoute.getPrimary().getCrossConnections()) {
        xcList.add(xc);
      }
      if (linkRoute.getSecondary() != null) {
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc : linkRoute.getSecondary().getCrossConnections()) {
          xcList.add(xc);
        }
      }
    }
    return xcList;
  }
}
