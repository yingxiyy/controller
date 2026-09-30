/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.och;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.*;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater;
import net.flex.dci.otn.controller.allocate.link.view.ViewLink;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class OchLinkFromTunnelRemover {

  private ChangedObject changedObject;
  private Link ochLink;
  private Tunnel tunnel; //because this tunnel, I will remove the ochLink

  private final static OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
  private final static PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);

  public OchLinkFromTunnelRemover(ChangedObject changedObject, Tunnel tunnel, String ochLinkID) {
    this.changedObject = changedObject;
    this.tunnel = tunnel;
    this.ochLink = changedObject.getChangedOchLink(ochLinkID);
  }


//  public void remove(Link ochLink) {
//    this.ochLink = ochLink;
//    String ochLinkId = ochLink.getLinkId().getValue();
//    changedObject.getRemovedOchLinkIdList().add(ochLinkId);
//    String srcNodeId = ochLink.getSource().getSourceNode().getValue();
//    String dstNodeId = ochLink.getDestination().getDestNode().getValue();
//
//    Node srcNode = ochNodeDao.getOchNodeById(srcNodeId);
//    Node dstNode = ochNodeDao.getOchNodeById(dstNodeId);
//
//    if (srcNode.getTerminationPoint().size() == 1) {
//      //this TP is the removed OCH link's, thus will be remove.
//      //after remove this TP, the node shoud remove too.
//      changedObject.getRemovedOchNodeIdList().add(srcNodeId);
//    } else {
//      changedObject.getRemovedOchTpIdList().add(ochLink.getSource().getSourceTp().getValue());
//    }
//
//    if (dstNode.getTerminationPoint().size() == 1) {
//      //this TP is the removed OCH link's, thus will be remove.
//      //after remove this TP, the node shoud remove too.
//      changedObject.getRemovedOchNodeIdList().add(dstNodeId);
//    } else {
//      changedObject.getRemovedOchTpIdList().add(ochLink.getDestination().getDestTp().getValue());
//    }
//  }

  public void update(OduGranularity odujLevel, String odujSlot) {
    if (ochLink == null) {
        log.error("the ochLink is empty");
        return;
    }

    List<SupportedTunnel> ochLinkSupportedTunnels = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class).getSupportedTunnel();
    log.debug("the och supported tunnel number size: {}  -- {}", ochLinkSupportedTunnels.size(), ochLink.getLinkId().getValue());
    
    if (ochLinkSupportedTunnels.size() <= 1) {
      //this is latest one tunnel in the OCH link, remove the tunnel, will remove OCH link too,

        SupportedTunnel slTunnel = ochLinkSupportedTunnels.stream()
                .filter(x -> x.getTunnelRef().getValue().equalsIgnoreCase(tunnel.getTunnelId().getValue()))
                .findAny().orElse(null);
        if (slTunnel == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the latest tunnel is not me. " + tunnel.getTunnelId().getValue());
        }
      remove();
    } else {
      //change avaliable list, supportedTunnelList;
      addOchAvailable(odujLevel, odujSlot);
      removedSupportedTunnel(tunnel.getTunnelId().getValue());
      changedObject.addChangedOchLink(ochLink);
    }
  }




  //这个OCH不用了，检测是不是板卡上的最后一个OCH，如果是，移除板卡
  //同时需要考虑OLP3 的问题
  private void cleanEquipment(String nodeId, String eqId) {
    Node node = changedObject.getChangedPhyNode(nodeId);
    if (node == null) {
      //node has been removed when remove och, because it's the latest one och
      return;
    }

    try {
        //某些电层板卡有多个L口，需要先保证这是最后一个L口相关的OCH 才才开始删除板卡上的serviceType
        List<InternalLinks> ilList = node.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        if (ilList.stream().filter(il -> il.getLinkRef().contains(eqId) && il.getLinkType().equals(LinkType.OsLink)).count() == 0) {
            log.debug("no external link on this eq, remove it. {}", eqId);

            Node newNode = PhyNodeUtil.markSlotEmpty(node, eqId);

            changedObject.addChangedPhyNode(newNode);
        }
    } catch (Exception e) {
        log.error("error:", e);
    }
  }

  //check node's TP list, if only one, means this is lastest OCH link, remove the node
  //otherwise remove TP

  private void updateOchNode(Node ochNode, String tpId) {
    if (ochNode == null)
      return;

    if (ochNode.getTerminationPoint().size() > 1) {
      List<TerminationPoint> newTpList = new ArrayList<>();
      newTpList.addAll(ochNode.getTerminationPoint());

      Iterator<TerminationPoint> iter = newTpList.iterator();
      while (iter.hasNext()) {
        TerminationPoint tp = iter.next();
        if (tp.getTpId().getValue().equals(tpId)) {
          iter.remove();
          break;
        }
      }
      ochNode = new NodeBuilder(ochNode).setTerminationPoint(newTpList).build();
      changedObject.addChangedOchNode(ochNode);
    } else {
      changedObject.addRemoveOchNode(ochNode);

      //对应的OCH node都删除了，说明这个电层 网元已经不属于某个siteLink了，需要把它从对应的rack数据结构中移除
      //这个说法又错了，在光电同框的情况下，OCH网元没有了，不代表PHY NE也没有了，因为PHY NE上还有可能有光板卡
//      String siteLinkId = null;
//      for (SupportingLink sl : ochLink.getSupportingLink()) {
//        if (SiteLinkIdNamingRule.isSiteLink(sl.getLinkRef().getValue())) {
//          siteLinkId = sl.getLinkRef().getValue();
//
//          String siteNodeId = PhysicalTpIdNamingRule.getSiteId(tpId);
//          Node siteNode = changedObject.getChangedSiteNode(siteNodeId);
//          //ochNode的 nodeId 与 phyNode的是一样的
//          Node newSiteNode = SiteNodeUtils.removePhyNe(siteLinkId, siteNode, ochNode.getNodeId().getValue());
//          changedObject.addChangedSiteNode(newSiteNode);
//        }
//      }
    }
  }

    /**
     * this used for repaire DB
     * @param ochLink
     */
  public void removeOch(Link ochLink) {
      ochLink = ochLink;
      remove();
  }

  private void remove() {
    Set<String> impactedEletricEqs = new HashSet<>();

    String srcNodeId = ochLink.getSource().getSourceNode().getValue();
    String dstNodeId = ochLink.getDestination().getDestNode().getValue();

    List<String> siteLinkIds = ochLink.getSupportingLink().stream()
            .filter(sl -> SiteLinkIdNamingRule.isSiteLink(sl.getLinkRef().getValue()))
            .map(x -> x.getLinkRef().getValue())
            .collect(Collectors.toList());

    for (SupportingLink ochSupportingLink : ochLink.getSupportingLink()) {
      //find out siteLink, and change available frequency
      String linkId = ochSupportingLink.getLinkRef().getValue();
      if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
        Link siteLink = changedObject.getChangedSiteLink(linkId);
        if (siteLink == null) {
          try {
            throw new NullPointerException("this is impossible, the siteLink doesn't existed in DB");
          } catch (NullPointerException e) {
            log.error("!!! ", e);
            return;
          }
        }
        SiteLinkOchUpdater siteLinkUpdater = new SiteLinkOchUpdater(siteLink);
        siteLinkUpdater.removeOch(ochLink);
        changedObject.addChangedSiteLink(siteLinkUpdater.getSiteLink());

        //check, when the siteLink is virtual and hasn't any ochLink used, remove it.
        removeVirtualSiteLink(siteLinkUpdater);
      } else if (PhysicalLinkIdNamingRule.isOsLink(linkId)) {
        // OCH 删除时先记录受影响的电层板卡；是否删除 phyNode 要等 OS link 和板卡清理完成后再判断。
        String tpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
        collectImpactedElectricEquipment(tpId, siteLinkIds, impactedEletricEqs);
        tpId = PhysicalLinkIdNamingRule.getTpZId(linkId);
        collectImpactedElectricEquipment(tpId, siteLinkIds, impactedEletricEqs);

        log.info("remove phyLink {}", linkId);
        new PhyLinkUtil(changedObject).removePhyLink(linkId, tunnel.getPlaneId());
      } else {
        //this is wssLink, doesn't remove it in ochLink level, only remove it in siteLink level
      }
    }

    //update OCM channel-mapping;
//    new OcmUpdater(changedObject).updateOcmGroup(ochLink, true);  OCM 修改都移动到了deImpl 实现

    //remove XC OCHLink上的XC是不需要删除的，因为在删除Tunnel的时候用到的交叉就删除了
    //这种描述是错误的，因为Tunnel中删除的XC只是C--L 口的交叉，在Flex的情况下， OPC网元上的交叉需要单独删除
    removeXCinOchLink(ochLink);

    //这里清除网元的slot/transceiver
//    cleanPhyNeResourceOnLink(ochLink);  //remove hardware

    //if the ochLink is last one of ochNode, remove ochNode too.
    Node srcNode = changedObject.getChangedOchNode(srcNodeId);
    Node dstNode = changedObject.getChangedOchNode(dstNodeId);
    updateOchNode(srcNode, ochLink.getSource().getSourceTp().getValue());
    updateOchNode(dstNode, ochLink.getDestination().getDestTp().getValue());

    changedObject.addRemovedOchLink(ochLink.getLinkId().getValue());
    new ViewLink(changedObject, tunnel.getPlaneId()).remove(ochLink);

    for(String eqId : impactedEletricEqs) {
      String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
      cleanEquipment(nodeId, eqId);
    }

    impactedEletricEqs.stream()
        .map(PhysicalEqpIdNamingRule::getNodeId)
        .collect(Collectors.toSet())
        .forEach(this::removeEmptyElectricNode);
  }

  /**
   * OCH supporting-link 中的 siteLink 表示光层网元，OS link 另一端才是电层板卡。
   * 电层板卡是否删除由 cleanEquipment 判断，电层网元是否删除必须在所有板卡清理后判断。
   */
  private void collectImpactedElectricEquipment(String tpId, List<String> siteLinkIds, Set<String> impactedEletricEqs) {
    String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
    if (siteLinkIds.stream().noneMatch(id->id.contains(nodeId))) {
      String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
      impactedEletricEqs.add(eqId);
    }
  }

  private void removeEmptyElectricNode(String nodeId) {
    Node node = changedObject.getChangedPhyNode(nodeId);
    if (node == null) {
      return;
    }
    if (hasRemainingOchLinkOnNode(nodeId)) {
      log.debug("the electric node still has other OCH link, keep it in config DB {}", nodeId);
      return;
    }
    // 只有电层网元上的板卡都已经清空，并且没有 IP 绑定时，才从 rack 和 config DB 中删除网元。
    if (PhyNodeUtil.isEmptyNode(node)) {
      if (PhyNodeUtil.hasIp(node)) {
        log.debug("the empty electric node still has IP, keep it in config DB {}", nodeId);
        return;
      }
      log.info("the electric node is empty and has no IP, remove it {}", nodeId);
      changedObject.addRemovedPhyNode(nodeId);
      removeFromSiteNode(nodeId);
    }
  }

  private boolean hasRemainingOchLinkOnNode(String nodeId) {
    Set<String> removedOchLinkIds = changedObject.getRemovedOchLinkIdList();
    List<Link> ochLinkList = ochLinkDao.queryWithNode(nodeId);
    for (Link link : ochLinkList) {
      String linkId = link.getLinkId().getValue();
      // 同一个事务中已标记删除的 OCH 还在数据库查询结果里，不能继续算作 node 占用。
      if (!removedOchLinkIds.contains(linkId)) {
        return true;
      }
    }
    return false;
  }

  private void removeFromSiteNode(String nodeId) {
    String siteId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
    Node siteNode = changedObject.getChangedSiteNode(siteId);

    SiteNodeCorrelateResource correlatedResource = new SiteNodeCorrelateResource(siteNode);
    correlatedResource.updateRack_remove(null, null, nodeId);
    changedObject.addChangedSiteNode(correlatedResource.getSiteNode());
  }

  private void removeVirtualSiteLink(SiteLinkOchUpdater Updater) {
    boolean removed = Updater.checkAndRemove();
    if (removed) {
      changedObject.addRemovedSiteLink(Updater.getSiteLink());
    }
  }

//  private void cleanPhyNeResourceOnLink(Link ochLink) {
//    //ochp 的情况下不止L口，还有其他，所以必须通过其他方式，而不是原来的只用ochLink 的A/Z点
//
//    List<String> tpIdList = OchLinkUtil.getLinkTp(ochLink);
//    Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
//
//    for (String tpId : tpIdList) {  //OT card
//      String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
//      Node node = changedObject.getChangedPhyNode(nodeId);
//
//      if (node != null) {
//        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
//        if (!PhyNodeUtil.hasXConEquipment(node, equipId)) {
//          node = PhyNodeUtil.markSlotEmpty(node, equipId);
//        } else {
//          node = PhyNodeUtil.removeOTTransceiver(node, tpId);
//        }
//
//        changedObject.addChangedPhyNode(node);
//      }
////      opNodeProcess(node, equipId);
//    }
//
//    if (ochLinkAttr.getProtectionType() != null && ochLinkAttr.getProtectionType().getName().equals(ProtectionBidir1To1.class.getName())) {
//      for (CrossConnections xc : ochLinkAttr.getExplictRoute().getRoute().get(0).getPrimary().getCrossCaaaaonnections()) {
//        if (xc.getAps() != null) {
//          String tpId = xc.getSourceTp().get(0).getTpRef().getValue();
//
//          String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
//          Node node = changedObject.getChangedPhyNode(nodeId);
//          String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
//          boolean isEmpty = true;
//          for (TerminationPoint tp : node.getTerminationPoint()) {
//            if (tp.getTpId().getValue().contains(equipId)) {
//              Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
//              if (tpAttr.getConnectionStatus() != null && tpAttr.getConnectionStatus().equals(ConnectionStatus.Busy)) {
//                isEmpty = false;
//                break;
//              }
//            }
//          }
//          if (isEmpty) {
//            node = PhyNodeUtil.markSlotEmpty(node, equipId);
//            changedObject.addChangedPhyNode(node);
//          }
//        }
//      }
//    }
//
////    merge2OpDB(ochLink);
//    for (String tpId : tpIdList) {
//      String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
//      Node node = changedObject.getChangedPhyNode(nodeId);
//
//      if (PhyNodeUtil.isEmptyNode(node)) {
//        log.debug("this phyNode is empty device, remove it from siteNode and DB if it hasn't IP {}", nodeId);
//        //只有网元为空，才从rack删除
//        for (SupportingLink sl : ochLink.getSupportingLink()) {
//          if (SiteLinkIdNamingRule.isSiteLink(sl.getLinkRef().getValue())) {
//            String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
//            Node siteNode = changedObject.getChangedSiteNode(siteNodeId);
//            siteNode = SiteNodeUtils.removePhyNe(sl.getLinkRef().getValue(), siteNode, nodeId);
//            changedObject.addChangedSiteNode(siteNode);
//          }
//        }
//
//        if (!PhyNodeUtil.hasIp(node)) {
//          log.debug("the node hasn't IP, remove from config DB");
//          changedObject.addRemovedPhyNode(nodeId);
//        }
//      } else {
//        log.debug("the node still has IP, keep it in config DB");
//        changedObject.addChangedPhyNode(node);
//      }
//    }
//  }

//  private void merge2OpDB(Link ochLink) {
//    Set<String> nodeIdSet = new HashSet<>();
//    List<String> tpIdList = OchLinkUtil.getLinkTp(ochLink);
//    for (String tpId : tpIdList) {
//      nodeIdSet.add(PhysicalTpIdNamingRule.getNodeId(tpId));
//    }
//
//    for (String nodeId : nodeIdSet) {
//      Node opNode = changedObject.getChangedPhyOpNode(nodeId);
//      if (opNode != null) {
//        Node cfgNode = changedObject.getChangedPhyNode(nodeId);
//        Node newOpNode = new PhyNodeMerge(cfgNode, opNode).del();
//        changedObject.addChangedPhyOpNode(newOpNode);
//      }
//    }
//  }

//  private void opNodeProcess(Node cfgNode, String equipId) {
//    Physical nodePhyAttr = cfgNode.getAugmentation(Node1.class).getPhysical();
//    Node opNode = null;
//    if (nodePhyAttr.getIp() != null && !nodePhyAttr.getIp().isEmpty()) {
//      opNode = changedObject.getChangedPhyOpNode(cfgNode.getNodeId().getValue());
//    }
//    if (opNode == null) {
//      return;
//    }
//
//    if (! PhyNodeUtil.hasXConEquipment(cfgNode, equipId)) {
//      opNode = PhyNodeUtil.markSlotEmpty(opNode, equipId);
//    } else {
//      opNode = PhyNodeUtil.removeTransceiverOnPort(opNode, equipId);
//    }
//
//    changedObject.addChangedPhyOpNode(opNode);
//  }


  private void removeXCinOchLink(Link ochLink) {
    Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

    for (Route route : ochLinkAttr.getExplictRoute().getRoute()) {
      if (route.getPrimary() != null) {
        removeXCOnNe(route.getPrimary().getCrossConnections());
      }
      if (route.getSecondary() != null) {
        removeXCOnNe(route.getSecondary().getCrossConnections());
      }
      if (route.getThird() != null) {
        route.getThird().forEach(third-> {
              removeXCOnNe(third.getCrossConnections());
            });
      }
    }
  }

  private void removeXCOnNe(List<CrossConnections> crossConnectionsList) {
    for (CrossConnections xc : crossConnectionsList) {
      String nodeId = xc.getNodeRef().getValue();
      Node node = changedObject.getChangedPhyNode(nodeId);
      if (node != null) {
        Node newNode = PhyNodeUtil.removeXc(node, xc);
        changedObject.addChangedPhyNode(newNode);
      }
    }
  }

  private void removedSupportedTunnel(String tunnelId) {
    List<SupportedTunnel> newList = new ArrayList<>();
    newList.addAll(ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class).getSupportedTunnel());

    Iterator<SupportedTunnel> iter = newList.iterator();
    while (iter.hasNext()) {
      SupportedTunnel st = iter.next();
      if (st.getTunnelRef().getValue().equals(tunnelId)) {
        iter.remove();
      }
    }

    ochLink = OchLinkUtil.setSupportedTunnel(ochLink, newList);
  }

  private void addOchAvailable(OduGranularity odujLevel, String odujSlot) {
    Och linkAttr = ochLink.getAugmentation(Link1.class).getOch();
    int pos = 0;
    Available oldAva= null;
    Iterator<Available> iter = linkAttr.getAvailable().iterator();
    while (iter.hasNext()) {
      Available ava = iter.next();
      if (ava.getSupportedOduj().equals(odujLevel)) {
        iter.remove();
        oldAva = ava;
        break;
      }
      pos++;
    }

    List <Available> newAvaList = new ArrayList<>();
    newAvaList.addAll(linkAttr.getAvailable());

    Available newAva;
    if (oldAva != null) {
      newAva = OchLinkUtil.addAvailable(oldAva, odujSlot);
      newAvaList.add(pos, newAva);
    } else {
      newAva = OchLinkUtil.newAvailable(odujLevel, odujSlot);
      newAvaList.add(newAva);
    }

    ochLink = new LinkBuilder(ochLink)
            .addAugmentation(Link1.class, new Link1Builder()
                    .setOch(new OchBuilder(linkAttr).setAvailable(newAvaList).build())
                    .build())
            .build();
  }


}
