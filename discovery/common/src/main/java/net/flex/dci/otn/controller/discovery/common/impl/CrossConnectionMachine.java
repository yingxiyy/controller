/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class CrossConnectionMachine {
  private ChangedObject changedObject;
  private CrossConnections xc;

  public CrossConnectionMachine(ChangedObject changedObject, CrossConnections xc) {
    this.changedObject = changedObject;
    this.xc = xc;
  }

  public void markImpl() {
    log.trace("make to impl {}", xc);

    String nodeId = PhysicalXcIdNamingRule.getNodeId(xc.getCrossConnectionId().getValue());
    Node node = changedObject.getChangedPhyNode(nodeId);

    Physical phyNodeAttr = node.getAugmentation(Node1.class).getPhysical();
    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> xcList = phyNodeAttr.getCrossConnections();
    Iterator<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> iter = xcList.iterator();
    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections newXC = null;
    int pos = 0;
    while (iter.hasNext()) {
      org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections neXC = iter.next();

      String xcId = CrossConnectionMachine.getNeXcId(xc.getCrossConnectionId().getValue());
      if (xcId.equals(neXC.getCrossConnectionId().getValue())) {
        newXC = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder(neXC)
                .setImplementState(ImplementState.Implement)
                .build();
        iter.remove();
        break;
      }
      pos++;
    }

    if (newXC != null) {
      ArrayList<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> newXcList = new ArrayList<>();
      newXcList.addAll(xcList);
      newXcList.add(pos, newXC);
      Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
              .setPhysical(new PhysicalBuilder(phyNodeAttr)
                      .setCrossConnections(newXcList)
                      .build())
              .build()).build();

      changedObject.addChangedPhyNode(newNode);
    }
  }

  public static String getNeXcId(String routeXcId) {
    if (routeXcId.endsWith("-MPO")) {
      //convert to NE's xc format
      String key = "-Site-";
      String[] tmp = routeXcId.split(key);
      String newXcId = "";
      for (int i = 0; i < tmp.length - 1; i++) {
        newXcId = newXcId + tmp[i] + key;
      }
      for (int i = 1; i <= 8; i++) {
        newXcId = newXcId + tmp[tmp.length - 1] + i;
        if (i != 8) {
          newXcId += key;
        }
      }
      routeXcId = newXcId;
    }
    return routeXcId;
  }
}
