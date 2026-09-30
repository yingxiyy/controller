/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.service;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes;

import java.util.ArrayList;
import java.util.List;

public class ReuseResource {
  private List<Node> reuseNodeList = new ArrayList<>();

  private PhyNodeDao phyNodeDao;

  public ReuseResource() {
    phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
  }

  public void addAndCheck(Nodes reuseNode) throws CommonException {
    Node dbNode = phyNodeDao.getConfigPhyNodeById(reuseNode.getNodeId());
    if (dbNode == null) {
      throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
          String.format("cannot find out reuse node %s", reuseNode.getNodeId()));
    }
    checkEquip(reuseNode, dbNode);
    reuseNodeList.add(dbNode);
  }

  /**
   * reuseNode 提供的是reuse时的初始状态，
   * 这个初始状态需要与数据库中取出来的一致。否则报错
   * @param reuseNode
   * @param dbNode
   */
  private void checkEquip(Nodes reuseNode, Node dbNode) {
    Physical phyAttr = dbNode.getAugmentation(Node1.class).getPhysical();
    for(PhyEquipAttributes reuseEq : reuseNode.getEquipments()) {
      boolean found = false;
      for(PhyEquipAttributes dbEq : phyAttr.getEquipments()) {
        if (reuseEq.getEquipmentId().equals(dbEq.getEquipmentId()) &&
            reuseEq.getEquipType().equals(dbEq.getEquipType())) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
            String.format("the node %s initial status has changed on %s", phyAttr.getFriendlyName(), reuseEq.getFriendlyName()));
      }
    }
  }
}
