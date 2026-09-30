/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.common;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class LinkPhyResource {
  protected ChangedObject changedObject;

  public LinkPhyResource(ChangedObject changedObject) {
    this.changedObject = changedObject;
  }

  protected void mergeEq(Set<String> tpList, Route designRoute) {
    if (tpList == null || designRoute == null)
      return;
    
    for (String tpId : tpList) {
      String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
      Node node = changedObject.getChangedPhyNode(nodeId);

      String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
      for (Node designNode : designRoute.getNodes()) {
        boolean found = false;
        if (designNode.getNodeId().getValue().equals(nodeId)) {
          found = true;
          Physical designPhyNode = designNode.getAugmentation(Node1.class).getPhysical();
          for (Equipments designEq : designPhyNode.getEquipments()) {
            if (designEq.getEquipmentId().equals(eqId)) {
              Node updatedNode = PhyNodeUtil.mergeEq(node, designEq);
              changedObject.addChangedPhyNode(updatedNode);

              break;
            }
          }
        }
        if (found) {
          break;
        }
      }
    }
    }

  protected void mergeTransceiver(Set<String> tpList, Route designRoute) {
    if (tpList == null || designRoute == null)
      return;
    
    for (String tpId : tpList) {
      String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
      Node node = changedObject.getChangedPhyNode(nodeId);

      String transceiverId = PhysicalTpIdNamingRule.getTransceiverId(tpId, PortType.OTULine);
      if (transceiverId == null) {
        continue;
      }

      for (Node designNode : designRoute.getNodes()) {
        boolean found = false;
        if (designNode.getNodeId().getValue().equals(nodeId)) {
          found = true;
          Physical designPhyNode = designNode.getAugmentation(Node1.class).getPhysical();
          for (Equipments designEq : designPhyNode.getEquipments()) {
            if (designEq.getEquipmentId().equals(transceiverId)) {
              Node updatedNode = PhyNodeUtil.mergeEq(node, designEq);
              changedObject.addChangedPhyNode(updatedNode);

              break;
            }
          }
        }
        if (found) {
          break;
        }
      }
    }
  }

  protected Set<String> getAllTps(Link link, List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcList) {
    Set<String> tpList = new HashSet<>();

    for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc : xcList) {
      for (SourceTp tp : xc.getSourceTp()) {
        tpList.add(tp.getTpRef().getValue());
      }
      for (DestinationTp tp : xc.getDestinationTp()) {
        tpList.add(tp.getTpRef().getValue());
      }
    }
    tpList.add(link.getSource().getSourceTp().getValue());
    tpList.add(link.getDestination().getDestTp().getValue());

    return tpList;
  }

  protected void mergeTp(Set<String> tpList, Route designRoute) {
    if (tpList == null || designRoute == null)
      return;

    for (String tpId : tpList) {
      String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
      Node node = changedObject.getChangedPhyNode(nodeId);

      for (Node designNode : designRoute.getNodes()) {
        boolean found = false;
        if (designNode.getNodeId().getValue().equals(nodeId)) {
          found = true;
          for (TerminationPoint designTp : designNode.getTerminationPoint()) {
            if (designTp.getTpId().getValue().equals(tpId)) {
              Node updatedNode = PhyNodeUtil.mergeTp(node, designTp);
              changedObject.addChangedPhyNode(updatedNode);

              break;
            }
          }
        }
        if (found) {
          break;
        }
      }
    }
  }

  protected void copyXC(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcList, Route designRoute) throws CommonException {
    if (xcList == null || designRoute == null)
      return;
    
    for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc : xcList) {
      String nodeId = PhysicalXcIdNamingRule.getNodeId(xc.getCrossConnectionId().getValue());
      Node node = changedObject.getChangedPhyNode(nodeId);

      CrossConnections designXc = null;
      for (CrossConnections baseXc : designRoute.getXcs()) {
        if (xc.getCrossConnectionId().getValue().equals(baseXc.getCrossConnectionId().getValue())) {
          designXc = baseXc;
          break;
        }
      }
      if (designXc == null) {
        log.debug("this is impossible, OCH link route is coming from design");
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
            String.format("OCH Link related XC cannot find in allocate data %s", xc.getCrossConnectionId().getValue()));
      }

      Node updatedNode = PhyNodeUtil.addXC(node, designXc);
      changedObject.addChangedPhyNode(updatedNode);
    }
  }
}
