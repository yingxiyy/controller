/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.node.view;


import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1Builder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;

import java.util.LinkedList;

/**
 * @author YYX
 * @date 2021-29-20
 */
public class ViewNode {
  private NodeId nodeId;

  private static ViewNodeDao viewNodeDao = SpringBeanFinder.getBean(ViewNodeDao.class);
  private static TopologyId siteTopologyId = new TopologyId(TopoNameConstants.Site_Topo_Key);

  public ViewNode (NodeId siteNodeId) {
    this.nodeId = siteNodeId;
  }

  public Node create(String friendlyName) {
    Node viewNode = viewNodeDao.getViewNodeById(nodeId.getValue());
    View viewNodeAttr;
    if (viewNode == null) {
      viewNodeAttr = new ViewBuilder()
              .setFriendlyName(friendlyName)
              .setAlarmState(AlarmSeverity.Unknown)
              .setPosX(-100 - (int) (Math.random() * 100))
              .setPosY(-100 - (int) (Math.random() * 100))
              .build();
    } else {
      viewNodeAttr = new ViewBuilder(viewNode.getAugmentation(Node1.class).getView())
              .setFriendlyName(friendlyName)
              .build();
    }

    viewNode = new NodeBuilder()
            .setNodeId(nodeId)
            .setKey(new NodeKey(nodeId))
            .setSupportingNode(new LinkedList<>())
            .addAugmentation(Node1.class,
                    new Node1Builder()
                            .setView(viewNodeAttr)
                            .build())
            .build();

    viewNode.getSupportingNode().add(new SupportingNodeBuilder()
            .setNodeRef(nodeId)
            .setTopologyRef(siteTopologyId)
            .setKey(new SupportingNodeKey(nodeId, siteTopologyId))
            .build());

    return viewNode;
  }

  public Node updateFriendlyName(Node viewNode, String friendlyName) {
    return new NodeBuilder(viewNode)
        .addAugmentation(Node1.class, new Node1Builder()
            .setView(new ViewBuilder(viewNode.getAugmentation(Node1.class).getView())
                .setFriendlyName(friendlyName)
                .build())
            .build())
        .build();
  }
}
