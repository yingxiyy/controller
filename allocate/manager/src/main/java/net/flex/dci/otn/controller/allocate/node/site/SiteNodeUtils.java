/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.node.site;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;

import java.util.Iterator;
import java.util.List;

/**
 * @author YYX
 * @date 12/11/2021
 */
@Slf4j
public class SiteNodeUtils {

  public static Node removePhyNe(String linkId, Node siteNode, String phyNodeId) {
    log.debug("remove phy ne from siteNode {}， {}", siteNode.getNodeId().getValue(), phyNodeId);
    siteNode = removeNeFromRack(linkId, siteNode, phyNodeId);
    return removeSupportingNe(siteNode, phyNodeId);
  }


  private static Node removeNeFromRack(String linkId, Node siteNode, String phyNodeId) {
    log.debug("remove phy node from rack");
    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site siteAttr = siteNode.getAugmentation(Node1.class).getSite();
    List<SupportingRack> rackList = siteAttr.getSupportingRack();
    Iterator<SupportingRack> rackIter = rackList.iterator();
    while (rackIter.hasNext()) {
      SupportingRack rack = rackIter.next();
      if (rack.getRackId().getValue().contains(linkId)) {
        //rackId 就siteLinkId
        Iterator<SupportingNe> iter = rack.getSupportingNe().iterator();

        while (iter.hasNext()) {
          SupportingNe ne = iter.next();
          if (ne.getNodeRef().getValue().equals(phyNodeId)) {
            iter.remove();
            break;
          }
        }
        break;
      }
    }
    siteNode = new NodeBuilder(siteNode).addAugmentation(Node1.class,
        new Node1Builder().setSite(new SiteBuilder(siteAttr)
                .setSupportingRack(rackList)
                .build())
            .build()).build();

    return siteNode;
  }

  private static Node removeSupportingNe(Node siteNode, String phyNodeId) {
    log.debug("remove phy node from site node's supporing node list");
    Iterator<SupportingNode> iter = siteNode.getSupportingNode().iterator();
    while (iter.hasNext()) {
      SupportingNode sNode = iter.next();
      if (sNode.getNodeRef().getValue().equals(phyNodeId)) {
        iter.remove();
        break;
      }
    }
    return siteNode;
  }
}
