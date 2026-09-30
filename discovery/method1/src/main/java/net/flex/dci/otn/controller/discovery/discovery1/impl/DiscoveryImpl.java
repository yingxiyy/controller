/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.discovery1.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.*;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.discovery.common.util.TunnelDiscoveryResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.AmplifierBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.osc.attributes.OscBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.client.attributes.ClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLineBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.WdmBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;

import java.util.*;

@Slf4j
public class DiscoveryImpl {

  private static DiscoveryImpl inst = new DiscoveryImpl();

  private static ZkResourceLock locker = new ZkResourceLock();

  private ChangedObject changedObject;

  private TunnelDiscoveryResult result;
  private RouteInfo rInfo;

  private Map<String, RouteInfo> rInfoMap;

  private MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);;

  public static DiscoveryImpl getInstance() {
    return inst;
  }

  public TunnelDiscoveryResult getResult() {
    return result;
  }

  /**
  * 现在是discover 一条Tunnel，写一次DB。
  *
  * @param tunnelId
  */
  public synchronized void doIt(String tunnelId) {
    try {
      recover(tunnelId);
    } catch (CommonException ce) {
      log.error("error happen", ce);
      throw ce;
    }
  }

  private void recover(String tunnelId) {
    changedObject = new ChangedObject();
    Tunnel tunnel = changedObject.getChangedTunnel(tunnelId);
    if (tunnel == null) {
      log.debug("required tunnelId hasn't found in DB  {}", tunnelId);
    }

    result = TunnelDiscoveryResult.builder()
            .tunnelId(tunnelId)
            .friendlyName(tunnel.getFriendlyName())
            .finalState(ImplementState.Allocate)
            .nodesCompareResult(new ArrayList<>())
            .build();
    rInfo = parseRoute(tunnel);


    lockResource(tunnelId);
    try {
      for (String nodeId : rInfo.getNodeIdList()) {
        compareNode(nodeId);
      }
      updateLinkState();
      mongoTransaction.save(changedObject);
    } catch (CommonException ce) {
      throw ce;
    } catch (Exception e) {
      throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage());
    } finally {
      locker.unlock();
      log.info("resource has unlocked");
    }
  }

  private void updateLinkState() {
    for (String id : rInfoMap.keySet()) {
      if (OchLinkIdNamingRule.isOchLink(id)) {
        Link ochLink = changedObject.getChangedOchLink(id);
        log.debug("checking OCH Link impleState {}", id);
        ImplementState implState = getImplementState(rInfoMap.get(id));
        if (!implState.equals(ImplementState.Allocate)) {
          ochLink = new LinkBuilder(ochLink)
                  .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class,
                          new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder()
                                  .setOch(new OchBuilder(ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch())
                                          .setImplementState(implState)
                                          .setAdminState(implState==ImplementState.Allocate ? AdminStatus.Down: AdminStatus.Up)
                                          .build())
                                  .build())
                  .build();
          changedObject.addChangedOchLink(ochLink);
        }
      } else if (SiteLinkIdNamingRule.isSiteLink(id)) {
        Link siteLink = changedObject.getChangedSiteLink(id);
        log.debug("checking Site Link impleState {}", id);
        ImplementState implState = getImplementState(rInfoMap.get(id));
        if (!implState.equals(ImplementState.Allocate)) {
          siteLink = new LinkBuilder(siteLink)
                  .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                          new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                  .setSite(new SiteBuilder(siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite())
                                          .setImplementState(implState)
                                          .setAdminState(implState==ImplementState.Allocate ? AdminStatus.Down: AdminStatus.Up)
                                          .build())
                                  .build())
                  .build();
          changedObject.addChangedSiteLink(siteLink);
        }
      } else if (TunnelIdNamingRule.isTunnelId(id)) {
        Tunnel tunnel = changedObject.getChangedTunnel(id);
        log.debug("checking Tunnel impleState {}", id);
        ImplementState implState = getImplementState(rInfoMap.get(id));
        if (!implState.equals(ImplementState.Allocate)) {
          tunnel = new TunnelBuilder(tunnel)
                          .setImplementState(implState)
                          .setAdminState(implState==ImplementState.Allocate ? AdminStatus.Down: AdminStatus.Up)
                          .build();
          changedObject.addChangedTunnel(tunnel);
        }
        result.setFinalState(implState);
      }
    }

    for (String id : rInfo.getPhyLinkIdList()) {
      //here is phy link
      Link phyLink = changedObject.getChangedPhyLink(id);
      ImplementState implState = getImplementState(phyLink);

      phyLink = new LinkBuilder(phyLink)
              .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                      new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                              .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(phyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical())
                                      .setImplementState(implState)
                                      .setAdminState(implState==ImplementState.Allocate ? AdminStatus.Down: AdminStatus.Up)
                                      .build())
                              .build())
              .build();
      changedObject.addChangedPhyLink(phyLink);
    }
  }

  private ImplementState getImplementState(Link phyLink) {
    String srcTpId = phyLink.getSource().getSourceTp().getValue();
    String srcNodeId = PhysicalTpIdNamingRule.getNodeId(srcTpId);
    Node srcNode = changedObject.getChangedPhyNode(srcNodeId);

    TerminationPoint srcTp = srcNode.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(srcTpId)).findAny().get();

    String dstTpId = phyLink.getDestination().getDestTp().getValue();
    String dstNodeId = PhysicalTpIdNamingRule.getNodeId(dstTpId);
    Node dstNode = changedObject.getChangedPhyNode(dstNodeId);

    TerminationPoint dstTp = dstNode.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(dstTpId)).findAny().get();

    if (srcTp.getAugmentation(TerminationPoint1.class).getPhysical().getImplementState().equals(ImplementState.Implement) &&
            dstTp.getAugmentation(TerminationPoint1.class).getPhysical().getImplementState().equals(ImplementState.Implement)) {
      return ImplementState.Implement;
    } else if (srcTp.getAugmentation(TerminationPoint1.class).getPhysical().getImplementState().equals(ImplementState.Allocate) &&
            dstTp.getAugmentation(TerminationPoint1.class).getPhysical().getImplementState().equals(ImplementState.Allocate)) {
      return ImplementState.Allocate;
    } else {
      return ImplementState.PartialImplement;
    }
  }

  private ImplementState getImplementState(RouteInfo routeInfo) {
    int impledNumber = 0;

    log.debug("routeInfo size of eqList, xcList, tpList is {}, {}, {}. total are {}",
            routeInfo.getEqIdList().size(), routeInfo.getXcIdList().size(), routeInfo.getTpIdList().size(),
            routeInfo.getEqIdList().size() + routeInfo.getTpIdList().size() + routeInfo.getXcIdList().size());

    for (String eqId : routeInfo.getEqIdList()) {
      String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
      Node node = changedObject.getChangedPhyNode(nodeId);
      Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
      Optional<Equipments> optional = nodeAttr.getEquipments().stream().filter(eq -> eq.getEquipmentId().equals(eqId)).findAny();
      if (!optional.isPresent()) {
        log.error("impossible, the EQ mustbe inside node {}", eqId);
        continue;
      }
      if (optional.get().getImplementState().equals(ImplementState.Implement)) {
        impledNumber++;
      } else {
        log.debug("the EQ isn't implemented {}, reason {}", eqId, getEqReason(nodeId, eqId));
      }
    }

    for (String xcId : routeInfo.getXcIdList()) {
      String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
      Node node = changedObject.getChangedPhyNode(nodeId);
      Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
      Optional<CrossConnections> optional = nodeAttr.getCrossConnections().stream().filter(xc -> xc.getCrossConnectionId().getValue().equals(xcId)).findAny();
      if (!optional.isPresent()) {
        log.error("impossible, the XC mustbe inside node {}", xcId);
        continue;
      }
      if (optional.get().getImplementState().equals(ImplementState.Implement)) {
        impledNumber++;
      } else {
        log.debug("the XC isn't implemented {}, reason {}", xcId, getXcReason(nodeId, xcId));
      }
    }

    for (String tpId : routeInfo.getTpIdList()) {
      String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
      Node node = changedObject.getChangedPhyNode(nodeId);

      Optional<TerminationPoint> optional = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(tpId)).findAny();
      if (!optional.isPresent()) {
        log.error("impossible, the TP mustbe inside node {}", tpId);
      }
      if (optional.get().getAugmentation(TerminationPoint1.class).getPhysical().getImplementState().equals(ImplementState.Implement)) {
        impledNumber++;
      } else {
        log.debug("the TP isn't implemented {}, reason {}", tpId, getTpReason(nodeId, tpId));
      }
    }

    log.debug("after discover, find {} objects are implemented", impledNumber);
    if (impledNumber == 0) {
      return ImplementState.Allocate;
    } else if (impledNumber == routeInfo.getEqIdList().size() + routeInfo.getTpIdList().size() + routeInfo.getXcIdList().size()) {
      return ImplementState.Implement;
    } else {
      return ImplementState.PartialImplement;
    }
  }

  private String getEqReason(String nodeId, String eqId) {
    TunnelDiscoveryResult.NodeResult nodeResult = result.getNodesCompareResult().stream().filter(data -> data.getNodeBasic().getId().equals(nodeId)).findAny().get();
    TunnelDiscoveryResult.ObjectResult objResult = nodeResult.getEqList().stream().filter(data -> data.getId().equals(eqId)).findAny().get();
    if (objResult.isConflict()) {
      return "data conflict" + objResult.getDetailMessage();
    } else {
      return objResult.getDetailMessage();
    }
  }

  private String getXcReason(String nodeId, String xcId) {
    TunnelDiscoveryResult.NodeResult nodeResult = result.getNodesCompareResult().stream().filter(data -> data.getNodeBasic().getId().equals(nodeId)).findAny().get();
    TunnelDiscoveryResult.ObjectResult objResult = nodeResult.getXcList().stream().filter(data -> data.getId().equals(xcId)).findAny().get();
    if (objResult.isConflict()) {
      return "data conflict" + objResult.getDetailMessage();
    } else {
      return objResult.getDetailMessage();
    }
  }

  private String getTpReason(String nodeId, String tpId) {
    TunnelDiscoveryResult.NodeResult nodeResult = result.getNodesCompareResult().stream().filter(data -> data.getNodeBasic().getId().equals(nodeId)).findAny().get();
    TunnelDiscoveryResult.ObjectResult objResult = nodeResult.getTpList().stream().filter(data -> data.getId().equals(tpId)).findAny().get();
    if (objResult.isConflict()) {
      return "data conflict: " + objResult.getDetailMessage();
    } else {
      return objResult.getDetailMessage();
    }
  }

  /**
   * 返回false, 表明这个节点还没有被管理， 它下面的资源继续保持allocate 状态
   * 返回true,  网元已经被管理，需要比对OP数据库的信息于cfg的一致性
   *
   * @param nodeId
   * @return
   */
  private void compareNode(String nodeId) {
    Node cfgNode = changedObject.getChangedPhyNode(nodeId);
    if (cfgNode == null) {
      throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
              String.format("cannot find %s in config DB", nodeId));
    }
    String friendlyName = cfgNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();

    TunnelDiscoveryResult.ObjectResult objectResult = TunnelDiscoveryResult.ObjectResult.builder()
            .id(nodeId)
            .friendlyName(friendlyName)
            .conflict(false)
            .build();


    TunnelDiscoveryResult.NodeResult nodeResult = TunnelDiscoveryResult.NodeResult.builder()
            .nodeBasic(objectResult)
            .eqList(new ArrayList<>())
            .xcList(new ArrayList<>())
            .tpList(new ArrayList<>())
            .build();
    result.getNodesCompareResult().add(nodeResult);

    Node opNode = changedObject.getChangedPhyOpNode(nodeId);
    if (opNode == null) {
      log.debug("the node {} hasn't managed by adapter", friendlyName);
      objectResult.setConflict(false);
      objectResult.setDetailMessage("真实网元还未上线");
      return;
    }

    objectResult.setConflict(false);
    cfgNode = new NodeBuilder(cfgNode).addAugmentation(Node1.class, new Node1Builder()
            .setPhysical(new PhysicalBuilder(cfgNode.getAugmentation(Node1.class).getPhysical())
                        .setAdminState(AdminStatus.Up)
                        .setImplementState(ImplementState.Implement)
                        .build())
                .build())
            .build();
    changedObject.addChangedPhyNode(cfgNode);

    //cfgNode中得hostName 与 网元可能不一致，这个真实网元会被改写为cfgNode得值
    compareEq(nodeId);
  }

  /**
   * 比对板卡 equip-type, equipTypeConfiged
   * 如果是Transceiver 没有需要比对的，都认为通过
   * @param nodeId
   * @return
   */
  private void compareEq(String nodeId) {
    TunnelDiscoveryResult.NodeResult nodeResult = result.getNodesCompareResult().get(result.getNodesCompareResult().size() - 1);
    for (String eqId : rInfo.getEqIdList()) {
      if (!eqId.contains(nodeId)) {
        //不是这个网元的板卡
        continue;
      }

      Node cfgNode = changedObject.getChangedPhyNode(nodeId);
      Node opNode = changedObject.getChangedPhyOpNode(nodeId);
      Physical opNodeAttr = opNode.getAugmentation(Node1.class).getPhysical();
      Physical cfgNodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();

      Optional<Equipments> optionalCfg = cfgNodeAttr.getEquipments().stream().filter(eq -> eq.getEquipmentId().equals(eqId)).findAny();
      if (! optionalCfg.isPresent()) {
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("the eq %s used in route, but cannot find it in cfg node", eqId));
      }
      Equipments cfg = optionalCfg.get();

      TunnelDiscoveryResult.ObjectResult objectResult = TunnelDiscoveryResult.ObjectResult.builder()
              .id(eqId)
              .friendlyName(PhysicalEqpIdNamingRule.getShortEqupId(eqId))
              .build();
      nodeResult.getEqList().add(objectResult);

      Equipments newEq = null;
      Optional<Equipments> optionalOp = opNodeAttr.getEquipments().stream().filter(eq -> eq.getEquipmentId().equals(eqId)).findAny();
      if (! optionalOp.isPresent()) {
        objectResult.setConflict(false);
        objectResult.setDetailMessage("真实网元没有这个板卡");
      } else {
        Equipments op = optionalOp.get();
        if (cfg.getEquipType().equals(EquipType.TRANSCEIVER)) {
          objectResult.setConflict(false);
//          objectResult.setDetailMessage("Transceiver信息 匹配");
        } else {
          if (!cfg.getEquipType().equals(op.getEquipType()) || !cfg.getEquipTypeConfiged().equals(op.getEquipTypeConfiged())) {
            objectResult.setConflict(true);
            objectResult.setDetailMessage(
                    String.format("控制器上%板卡是 %s(%s), 真实网元板卡 %s(%s)",
                            cfg.getEquipType(), cfg.getEquipTypeConfiged(),
                            op.getEquipType(), op.getEquipTypeConfiged()));
          } else {
            objectResult.setConflict(false);
//            objectResult.setDetailMessage(
//                    String.format("板卡信息匹配 %s(%s)",
//                            cfg.getEquipType(), cfg.getEquipTypeConfiged()));
          }
        }

        newEq = new EquipmentsBuilder(cfg)
                .setAdminState(op.getAdminState())
                .setImplementState(op.getImplementState())
                .build();
      }

      if (newEq != null) {
        List<Equipments> newList = new ArrayList<>();
        Iterator<Equipments> iter = cfgNodeAttr.getEquipments().iterator();
        while (iter.hasNext()) {
          Equipments oldEq = iter.next();
          if (oldEq.getEquipmentId().equals(eqId)) {
            newList.add(newEq);
          } else {
            newList.add(oldEq);
          }
        }

        cfgNode = new NodeBuilder(cfgNode).addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(cfgNode.getAugmentation(Node1.class).getPhysical())
                                .setEquipments(newList)
                                .build())
                        .build())
                .build();
        changedObject.addChangedPhyNode(cfgNode);

        compareTp(nodeId, eqId);
        compareXc(nodeId, eqId);
      }
    }
  }

  private void compareXc(String nodeId, String eqId) {
    Node cfgNode = changedObject.getChangedPhyNode(nodeId);
    Node opNode = changedObject.getChangedPhyOpNode(nodeId);
    Physical opNodeAttr = opNode.getAugmentation(Node1.class).getPhysical();

    TunnelDiscoveryResult.NodeResult nodeResult = result.getNodesCompareResult().get(result.getNodesCompareResult().size() - 1);
    for (String xcId : rInfo.getXcIdList()) {
      if (!xcId.contains(eqId)) {
        //不是这个板卡的XC
        continue;
      }

      Physical cfgNodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();

      Optional<CrossConnections> optionalCfg = cfgNodeAttr.getCrossConnections().stream().filter(xc -> xc.getCrossConnectionId().getValue().equals(xcId)).findAny();
      if (!optionalCfg.isPresent()) {
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("the XC %s used in route, but cannot find it in cfg node", xcId));
      }
      CrossConnections cfg = optionalCfg.get();

      TunnelDiscoveryResult.ObjectResult objectResult = TunnelDiscoveryResult.ObjectResult.builder()
              .id(xcId)
              .friendlyName(cfg.getDescription())
              .build();
      nodeResult.getXcList().add(objectResult);

      Optional<CrossConnections> optionalOp = opNodeAttr.getCrossConnections().stream().filter(xc -> xc.getCrossConnectionId().getValue().equals(xcId)).findAny();
      if (!optionalOp.isPresent()) {
        objectResult.setConflict(false);
        objectResult.setDetailMessage("真实网元没有这个XC");
        if (cfg.getAmplifier() == null && cfg.getAps() == null) {
          String aCtp = cfg.getSourceTp().get(0).getTpRef().getValue() + cfg.getSourceTp().get(0).getSlot();
          String zCtp = cfg.getDestinationTp().get(0).getTpRef().getValue() + cfg.getDestinationTp().get(0).getSlot();

          Optional<CrossConnections> optionalConflicXc = opNodeAttr.getCrossConnections().stream().filter(xc -> {
            if (xc.getCrossConnectionId().getValue().contains(aCtp) || xc.getCrossConnectionId().getValue().contains(zCtp)) {
              return true;
            }
            return false;
          }).findAny();
          if (optionalConflicXc.isPresent()) {
            objectResult.setConflict(true);
            objectResult.setDetailMessage(String.format("控制器XC %s 与真实网元XC %s冲突", cfg.getDescription(), optionalConflicXc.get().getDescription()));
          }
        }
      } else {
        objectResult.setConflict(false);
        CrossConnections newXc = null;

        CrossConnections op = optionalOp.get();
        if (cfg.getAmplifier() != null) {
          if (op.getAmplifier().isEnable() && op.getAdminState().equals(AdminStatus.Up)) {
            newXc = processAmplifierXc(cfg, op);
          } else {
            objectResult.setDetailMessage(String.format("%s, 网元上没有开启", cfg.getDescription()));
          }
        } else if (cfg.getAps() != null) {
          if (op.getAdminState().equals(AdminStatus.Up)) {
            newXc = processApsXc(cfg, op);
          } else {
            objectResult.setDetailMessage(String.format("%s, 网元上没有开启", cfg.getDescription()));
          }
        } else if (op.getAdminState().equals(AdminStatus.Up)) {
          newXc = processNormalXc(cfg, op);
        } else {
          objectResult.setDetailMessage(String.format("%s, 网元上没有开启", cfg.getDescription()));
        }


        if (newXc != null) {
          List<CrossConnections> newList = new ArrayList<>();
          Iterator<CrossConnections> iter = cfgNodeAttr.getCrossConnections().iterator();
          while (iter.hasNext()) {
            CrossConnections oldXc = iter.next();
            if (oldXc.getCrossConnectionId().getValue().equals(xcId)) {
              newList.add(newXc);
            } else {
              newList.add(oldXc);
            }
          }
          cfgNode = new NodeBuilder(cfgNode)
                  .addAugmentation(Node1.class, new Node1Builder()
                          .setPhysical(new PhysicalBuilder(cfgNodeAttr)
                                  .setCrossConnections(newList)
                                  .build())
                          .build())
                  .build();

          changedObject.addChangedPhyNode(cfgNode);
        }

      }

    }
  }

  private CrossConnections processNormalXc(CrossConnections cfg, CrossConnections op) {
    String name = cfg.getDescription();

    CrossConnections xc = new CrossConnectionsBuilder(cfg)
            .setAdminState(op.getAdminState())
            .setImplementState(op.getImplementState())
            .build();

    return xc;
  }

  private CrossConnections processAmplifierXc(CrossConnections cfg, CrossConnections op) {
    String name = cfg.getDescription();

    CrossConnections xc = new CrossConnectionsBuilder(cfg)
            .setAmplifier(new AmplifierBuilder(cfg.getAmplifier())
                    .setAmpMode(op.getAmplifier().getAmpMode())
                    .setEnable(op.getAmplifier().isEnable())
                    .build())
            .setAdminState(op.getAdminState())
            .setImplementState(op.getImplementState())
            .build();

    return xc;
  }

  private CrossConnections processApsXc(CrossConnections cfg,  CrossConnections op) {
    String name = cfg.getDescription();

    List<Property> newPropList = new ArrayList<>();
    if (op.getProperties() != null && op.getProperties().getProperty() != null) {
      Property prop = getOpProperty(op.getProperties().getProperty(), "relative-switch-threshold");
      if (prop != null) {
        newPropList.add(prop);
      }
      prop = getOpProperty(op.getProperties().getProperty(), "secondary-switch-threshold");
      if (prop != null) {
        newPropList.add(prop);
      }
      prop = getOpProperty(op.getProperties().getProperty(), "primary-switch-threshold");
      if (prop != null) {
        newPropList.add(prop);
      }
      prop = getOpProperty(op.getProperties().getProperty(), "primary-switch-hysteresis");
      if (prop != null) {
        newPropList.add(prop);
      }
      prop = getOpProperty(op.getProperties().getProperty(), "relative-switch-threshold-offset");
      if (prop != null) {
        newPropList.add(prop);
      }
    }
    CrossConnections xc = new CrossConnectionsBuilder(cfg)
            .setAps(new ApsBuilder(cfg.getAps())
                    .setApsMode(op.getAps().getApsMode())
                    .setHoldOffTime(op.getAps().getHoldOffTime())
                    .setRevertive(op.getAps().isRevertive())
                    .setProperties(new PropertiesBuilder().setProperty(newPropList).build())
                    .build())
            .setAdminState(op.getAdminState())
            .setImplementState(op.getImplementState())
            .build();

    return xc;
  }

  private void compareTp(String nodeId, String eqId) {
    Node cfgNode = changedObject.getChangedPhyNode(nodeId);
    Node opNode = changedObject.getChangedPhyOpNode(nodeId);
    Physical opNodeAttr = opNode.getAugmentation(Node1.class).getPhysical();

    TunnelDiscoveryResult.NodeResult nodeResult = result.getNodesCompareResult().get(result.getNodesCompareResult().size() - 1);
    for (String tpId : rInfo.getTpIdList()) {
      if (!tpId.contains(eqId)) {
        //不是这个板卡的TP
        continue;
      }

      Physical cfgNodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();

      Optional<TerminationPoint> optionalCfg = cfgNode.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(tpId)).findAny();
      if (!optionalCfg.isPresent()) {
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("the TP %s used in route, but cannot find it in cfg node", tpId));
      }
      TerminationPoint cfg = optionalCfg.get();

      TunnelDiscoveryResult.ObjectResult objectResult = TunnelDiscoveryResult.ObjectResult.builder()
              .id(tpId)
              .friendlyName(PhysicalTpIdNamingRule.getShortTpByTpId(tpId))
              .build();
      nodeResult.getTpList().add(objectResult);

      Optional<TerminationPoint> optionalOp = opNode.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(tpId)).findAny();
      if (!optionalOp.isPresent()) {
        objectResult.setConflict(false);
        objectResult.setDetailMessage("真实网元没有这个TP");
      } else {
        TerminationPoint op = optionalOp.get();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical cfgTpAttr = cfg.getAugmentation(TerminationPoint1.class).getPhysical();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical opTpAttr = op.getAugmentation(TerminationPoint1.class).getPhysical();

        TerminationPoint newTp = null;
        switch (cfgTpAttr.getPortType()) {
          case OTUClient:
            newTp = processOtuClient(objectResult, cfg, cfgTpAttr, opTpAttr);
            break;
          case OTULine:
            newTp = processOtuLine(objectResult, cfg, cfgTpAttr, opTpAttr);
            break;
          case OALine:
            newTp = processOALine(objectResult, cfg, cfgTpAttr, opTpAttr);
            break;
          default:
            newTp = processNormalTp(objectResult, cfg, cfgTpAttr, opTpAttr);
            break;
        }

        if (newTp != null) {
          List<TerminationPoint> newList = new ArrayList<>();
          Iterator<TerminationPoint> iter = cfgNode.getTerminationPoint().iterator();
          while (iter.hasNext()) {
            TerminationPoint oldTp = iter.next();
            if (oldTp.getTpId().getValue().equals(tpId)) {
              newList.add(newTp);
            } else {
              newList.add(oldTp);
            }
          }
          cfgNode = new NodeBuilder(cfgNode).setTerminationPoint(newList).build();
          changedObject.addChangedPhyNode(cfgNode);
        }

      }
    }
  }

  private TerminationPoint processNormalTp(TunnelDiscoveryResult.ObjectResult objectResult, TerminationPoint cfg, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical cfgTpAttr, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical opTpAttr) {
    //到这里的时候已经是一致的了
    objectResult.setConflict(false);
    if (opTpAttr.getAdminState().equals(AdminStatus.Down) || opTpAttr.getImplementState().equals(ImplementState.Allocate)) {
      objectResult.setDetailMessage(String.format("TP %s, 网元没有配置adminUp", PhysicalTpIdNamingRule.getShortTpByTpId(cfg.getTpId().getValue())));
    }

    TerminationPoint newTp = new TerminationPointBuilder(cfg)
            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(cfgTpAttr)
                            .setAdminState(opTpAttr.getAdminState())
                            .setImplementState(opTpAttr.getImplementState())
                            .build())
                    .build())
            .build();

    return newTp;
  }


  private TerminationPoint processOtuClient(TunnelDiscoveryResult.ObjectResult objectResult, TerminationPoint cfg, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical cfgTpAttr, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical opTpAttr) {
    String tpId = cfg.getTpId().getValue();
    //C口重要属性moduleType(EthComplianceCode), signalRate,

    if (cfgTpAttr.getOtuClient() == null || cfgTpAttr.getProperties() == null || cfgTpAttr.getProperties().getProperty() == null) {
      throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "config DB hasn't find OT C port attribute " + tpId);
    }

    boolean matched = true;
    if (opTpAttr.getOtuClient() == null) {
      objectResult.setConflict(false);
      log.warn(String.format("TP %s, 网元没有配置", PhysicalTpIdNamingRule.getShortTpByTpId(tpId)));
      matched = false;
    } else {
      if (opTpAttr.getOtuClient().getClient() == null || opTpAttr.getOtuClient().getSignalRate() == null) {
        objectResult.setConflict(false);
        log.warn(String.format("TP %s, 网元没有配置", PhysicalTpIdNamingRule.getShortTpByTpId(tpId)));
        matched = false;
      } else if (!opTpAttr.getOtuClient().getSignalRate().getName().equals(cfgTpAttr.getOtuClient().getSignalRate().getName())) {
        objectResult.setConflict(true);
        objectResult.setDetailMessage(
                String.format("TP %s, 控制器配置速率是%s, 网元配置是%s", PhysicalTpIdNamingRule.getShortTpByTpId(tpId),
                        cfgTpAttr.getOtuClient().getSignalRate().getSimpleName(),
                        opTpAttr.getOtuClient().getSignalRate().getSimpleName()));
        matched = false;
      } else if (!opTpAttr.getOtuClient().getClient().getEthComplianceCode().getName().equals(cfgTpAttr.getOtuClient().getClient().getEthComplianceCode().getName())) {
        objectResult.setConflict(true);
        objectResult.setDetailMessage(
                String.format("TP %s, 控制器配置模块是%s, 网元配置是%s", PhysicalTpIdNamingRule.getShortTpByTpId(tpId),
                        cfgTpAttr.getOtuClient().getClient().getEthComplianceCode().getSimpleName(),
                        opTpAttr.getOtuClient().getClient().getEthComplianceCode().getSimpleName()));
        matched = false;
      }
    }

    if (!matched) {
      return null;
    }

    //cfgTp only setting 3 default value test-signal, tti-msg-auto, loopback-mode
    List<Property> newPropList = new ArrayList<>();
    if (opTpAttr.getProperties() != null && opTpAttr.getProperties().getProperty() != null) {
      Property prop = getOpProperty(opTpAttr.getProperties().getProperty(), "test-signal");
      if (prop != null) {
        newPropList.add(prop);
      }
      prop = getOpProperty(opTpAttr.getProperties().getProperty(), "tti-msg-auto");
      if (prop != null) {
        newPropList.add(prop);
      }
      prop = getOpProperty(opTpAttr.getProperties().getProperty(), "loopback-mode");
      if (prop != null) {
        newPropList.add(prop);
      }
    }

    //C口重要属性moduleType(EthComplianceCode), signalRate,
    //到这里的时候已经是一致的了
    TerminationPoint newTp = new TerminationPointBuilder(cfg)
            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(cfgTpAttr)
                            .setOtuClient(new OtuClientBuilder(cfgTpAttr.getOtuClient())
                                    .setClient(new ClientBuilder(cfgTpAttr.getOtuClient().getClient())
                                            .setFecMode(opTpAttr.getOtuClient().getClient().getFecMode() != null ?
                                                    opTpAttr.getOtuClient().getClient().getFecMode() :
                                                    cfgTpAttr.getOtuClient().getClient().getFecMode())
                                            .build())
                                    .build())
                            .setProperties(new PropertiesBuilder().setProperty(newPropList).build())
                            .setAdminState(opTpAttr.getAdminState())
                            .setImplementState(opTpAttr.getImplementState())
                            .build())
                    .build())
            .build();

    return newTp;
  }

  private TerminationPoint processOtuLine(TunnelDiscoveryResult.ObjectResult objectResult, TerminationPoint cfg, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical cfgTpAttr, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical opTpAttr) {
    String tpId = cfg.getTpId().getValue();

    if (cfgTpAttr.getOtuLine() == null || cfgTpAttr.getProperties() == null || cfgTpAttr.getProperties().getProperty() == null) {
      throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "config DB hasn't find OT L port attribute " + tpId);
    }

    boolean matched = true;
    if (opTpAttr.getOtuLine() == null) {
      objectResult.setConflict(false);
      log.warn(String.format("TP %s, 网元没有配置", PhysicalTpIdNamingRule.getShortTpByTpId(tpId)));
      matched = false;
    } else {
      if (opTpAttr.getOtuLine().getSignalRate() == null) {
        objectResult.setConflict(false);
        log.warn(String.format("TP %s, 网元没有配置", PhysicalTpIdNamingRule.getShortTpByTpId(tpId)));
        matched = false;
      } else if (!opTpAttr.getOtuLine().getSignalRate().getName().equals(cfgTpAttr.getOtuLine().getSignalRate().getName())) {
        objectResult.setConflict(true);
        objectResult.setDetailMessage(
                String.format("TP %s, 控制器配置速率是%s, 网元是%s", PhysicalTpIdNamingRule.getShortTpByTpId(tpId),
                        cfgTpAttr.getOtuLine().getSignalRate().getSimpleName(),
                        opTpAttr.getOtuLine().getSignalRate().getSimpleName()));
        matched = false;
      }
      if (opTpAttr.getOtuLine().getCentralFrequency() == null) {
        objectResult.setConflict(false);
        objectResult.setDetailMessage(
                String.format("TP %s, 控制器配置中心频率是%s, 网元没有配置", PhysicalTpIdNamingRule.getShortTpByTpId(tpId), cfgTpAttr.getOtuLine().getSignalRate()));
        matched = false;
      } else if (opTpAttr.getOtuLine().getCentralFrequency().getValue().longValue() != cfgTpAttr.getOtuLine().getCentralFrequency().getValue().longValue()) {
        objectResult.setConflict(true);
        objectResult.setDetailMessage(
                String.format("TP %s, 控制器配置中心频率是%s, 网元是%s", PhysicalTpIdNamingRule.getShortTpByTpId(tpId),
                        cfgTpAttr.getOtuLine().getCentralFrequency().getValue().longValue(),
                        opTpAttr.getOtuLine().getCentralFrequency().getValue().longValue()));
        matched = false;
      }
    }

    if (!matched) {
      return null;
    }

    // L口一共3个属性， centerFrequency, signalRate, targetOutputPower
    //centerFrequency, signalRate 必须与cfg 一致。 （到这一步已经表示一致了）
    TerminationPoint newTp = new TerminationPointBuilder(cfg)
            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(cfgTpAttr)
                            .setOtuLine(new OtuLineBuilder(cfgTpAttr.getOtuLine())
                                    .setTargetOutputPower(opTpAttr.getOtuLine().getTargetOutputPower())
                                    .build())
                            .setAdminState(opTpAttr.getAdminState())
                            .setImplementState(opTpAttr.getImplementState())
                            .build())
                    .build())
            .build();

    return newTp;
  }

  private TerminationPoint processOALine(TunnelDiscoveryResult.ObjectResult objectResult, TerminationPoint cfg, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical cfgTpAttr, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical opTpAttr) {
    objectResult.setConflict(false);
    TerminationPoint newTp = null;
    if (cfgTpAttr.getWdm() != null && cfgTpAttr.getWdm().getOsc() != null) {
      if (opTpAttr.getWdm() != null && opTpAttr.getWdm().getOsc() != null) {
        newTp = new TerminationPointBuilder(cfg)
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(cfgTpAttr)
                                .setWdm(new WdmBuilder()
                                        .setOsc(new OscBuilder().setAutoAttenuationMode(opTpAttr.getWdm().getOsc().isAutoAttenuationMode()).build())
                                        .build())
                                .setAdminState(opTpAttr.getAdminState())
                                .setImplementState(opTpAttr.getImplementState())
                                .build())
                        .build())
                .build();
      }
    } else {
      newTp = new TerminationPointBuilder(cfg)
              .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                      .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(cfgTpAttr)
                              .setAdminState(opTpAttr.getAdminState())
                              .setImplementState(opTpAttr.getImplementState())
                              .build())
                      .build())
              .build();
    }
    return newTp;
  }


  private Property getOpProperty(List<Property> propertyList, String key) {
    Optional<Property> optionalProp = propertyList.stream().filter(prop -> prop.getName().equals(key)).findAny();
    if (optionalProp.isPresent()) {
      Property prop = new PropertyBuilder()
              .setKey(new PropertyKey(key))
              .setName(key)
              .setValue(optionalProp.get().getValue())
              .build();
      return prop;
    }
    return null;
  }

  private RouteInfo parseRoute(Tunnel tunnel) {
    RouteInfo tunnelRouteInfo = new RouteInfo();
    rInfoMap = new HashMap<>();

    log.debug("start parse route");
    tunnelRouteInfo.parse(tunnel.getExplictRoute().getRoute());
    rInfoMap.put(tunnel.getTunnelId().getValue(), tunnelRouteInfo);
    log.debug("end of parse route");


    List<String> svrLinkIdList = new ArrayList<>();
    svrLinkIdList.addAll(tunnelRouteInfo.getLogicServerLinkIdList());
    for (String logicSvrLinkId : svrLinkIdList) {
      if (OchLinkIdNamingRule.isOchLink(logicSvrLinkId)) {
        log.debug("start checking on OchLink");

        Link ochLink = changedObject.getChangedOchLink(logicSvrLinkId);
        Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

        if (ochLinkAttr.getImplementState().equals(ImplementState.Implement)) {
          log.debug("the OCH link has implemented {} {}", ochLinkAttr.getFriendlyName(), ochLinkAttr.getLowerFrequency());
          changedObject.unsetOchLink(logicSvrLinkId);
          continue;
        }

        RouteInfo ochrInfo = new RouteInfo();
        ochrInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
        rInfoMap.put(ochLink.getLinkId().getValue(), ochrInfo);
        tunnelRouteInfo.extend(ochrInfo);

        //检查siteLink是否需要Implement，需要就扩展rInfo
        for (String logicSvrLinkId2 : ochrInfo.getLogicServerLinkIdList()) {
          if (SiteLinkIdNamingRule.isSiteLink(logicSvrLinkId2)) {
            Link siteLink = changedObject.getChangedSiteLink(logicSvrLinkId2);
            Site siteLinkAttr = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();

            if (siteLinkAttr.getImplementState().equals(ImplementState.Implement)) {
              log.debug("the Site link has implemented {} {}", siteLinkAttr.getFriendlyName(), siteLinkAttr.getGrid());
              changedObject.unsetSiteLink(logicSvrLinkId2);
            } else {
              RouteInfo siterInfo = new RouteInfo();
              siterInfo.parse(siteLinkAttr.getExplictRoute().getRoute());
              rInfoMap.put(siteLink.getLinkId().getValue(), siterInfo);
              tunnelRouteInfo.extend(siterInfo);
            }
          }
        }
      }
    }

    //add Chassis as special equip for each node
    for (String nodeId : tunnelRouteInfo.getNodeIdList()) {
      String chasisId = nodeId + "#CHASSIS-1";
      tunnelRouteInfo.getEqIdList().add(chasisId);
    }
    return tunnelRouteInfo;
  }


  private void lockResource(String tunnelId) {
    locker.addResource(tunnelId);
    for (String nodeId : rInfo.getNodeIdList()) {
      locker.addResource(nodeId);
    }

    for (String linkId : rInfo.getLogicServerLinkIdList()) {
      locker.addResource(linkId);
    }

    locker.getLock();
    log.info("resource has locked");
  }

}
