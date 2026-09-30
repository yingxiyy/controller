/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.node.phy;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class PhyNodeRemover {
  private PhyNodeDao phyNodeDao;
  private ChangedObject changedObject;

  public PhyNodeRemover(ChangedObject changedObject) {
    this.changedObject = changedObject;
    phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
  }

  public void removeInternalLink(String nodeId, String linkId) {
    log.debug("start remove node internal link {}, {}", nodeId, linkId);
    Node phyNode = changedObject.getChangedPhyNodeList().get(nodeId);
    if (phyNode == null) {
      phyNode = phyNodeDao.getConfigPhyNodeById(nodeId);
    }

    Physical phyAttr = phyNode.getAugmentation(Node1.class).getPhysical();
    List<InternalLinks> ilList = new ArrayList<>(phyAttr.getInternalLinks());
    ilList.removeIf(x->x.getLinkRef().equals(linkId));

    Node newNode = new NodeBuilder(phyNode)
            .addAugmentation(Node1.class, new Node1Builder()
                    .setPhysical(new PhysicalBuilder(phyAttr)
                            .setInternalLinks(ilList)
                            .build())
                    .build())
            .build();

    changedObject.addChangedPhyNode(newNode);
  }

  public void removeAllInternalLinks(String nodeId) {
    Node node = changedObject.getChangedPhyNodeList().get(nodeId);
    if (node == null) {
      node = phyNodeDao.getConfigPhyNodeById(nodeId);
    }

    if (!node.getAugmentation(Node1.class).getPhysical().getInternalLinks().isEmpty()) {
      node = new NodeBuilder(node)
              .addAugmentation(Node1.class, new Node1Builder()
                      .setPhysical(new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                              .setInternalLinks(new ArrayList<>())
                              .build())
                      .build())
              .build();
    }
    changedObject.getChangedPhyNodeList().put(nodeId, node);
  }

}
