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
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;

import java.util.*;

@Slf4j
public class DiscoveryDiscardImpl {

  private static DiscoveryDiscardImpl inst = new DiscoveryDiscardImpl();

  private static ZkResourceLock locker = new ZkResourceLock();

  private ChangedObject changedObject;

  private RouteInfo rInfo;

  private Map<String, RouteInfo> rInfoMap;

  private MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
  private TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
  private OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

  public static DiscoveryDiscardImpl getInstance() {
    return inst;
  }

  /**
  * 现在是discover-discard 一条Tunnel，写一次DB。
  *
  * @param tunnelId
  */
  public void doIt(String tunnelId) {
    try {
      discard(tunnelId);
    } catch (CommonException ce) {
      log.error("error happen ", ce);
      throw ce;
    }
  }

  private void discard(String tunnelId) {
    changedObject = new ChangedObject();
    Tunnel tunnel = changedObject.getChangedTunnel(tunnelId);
    if (tunnel == null) {
      log.debug("required tunnelId hasn't found in DB  {}", tunnelId);
    }

    rInfo = parseRoute(tunnel);

    lockResource(tunnelId);
    try {
      updateLink2Allocate();
      mongoTransaction.save(changedObject);
    } catch (CommonException ce) {
      throw ce;
    } catch (Exception e) {
      throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage());
    } finally {
      locker.unlock();
    }
  }

  private void updateLink2Allocate() {
    for (String id : rInfoMap.keySet()) {
      if (OchLinkIdNamingRule.isOchLink(id)) {
        Link ochLink = changedObject.getChangedOchLink(id);
        underLayer2Allocate(rInfoMap.get(id));

        ochLink = new LinkBuilder(ochLink)
              .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class,
                      new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder()
                              .setOch(new OchBuilder(ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch())
                                      .setImplementState(ImplementState.Allocate)
                                      .setAdminState(AdminStatus.Down)
                                      .build())
                              .build())
              .build();
        changedObject.addChangedOchLink(ochLink);
      } else if (SiteLinkIdNamingRule.isSiteLink(id)) {
        Link siteLink = changedObject.getChangedSiteLink(id);
        underLayer2Allocate(rInfoMap.get(id));

        siteLink = new LinkBuilder(siteLink)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                .setSite(new SiteBuilder(siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite())
                                        .setImplementState(ImplementState.Allocate)
                                        .setAdminState(AdminStatus.Down)
                                        .build())
                                .build())
                .build();
        changedObject.addChangedSiteLink(siteLink);
      } else if (TunnelIdNamingRule.isTunnelId(id)) {
        Tunnel tunnel = changedObject.getChangedTunnel(id);
        underLayer2Allocate(rInfoMap.get(id));

        tunnel = new TunnelBuilder(tunnel)
                .setImplementState(ImplementState.Allocate)
                .setAdminState(AdminStatus.Down)
                .build();
        changedObject.addChangedTunnel(tunnel);
      }
    }

    for (String id : rInfo.getPhyLinkIdList()) {
      //here is phy link
      Link phyLink = changedObject.getChangedPhyLink(id);

      phyLink = new LinkBuilder(phyLink)
              .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                      new Link1Builder()
                              .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(phyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical())
                                      .setImplementState(ImplementState.Allocate)
                                      .setAdminState(AdminStatus.Down)
                                      .build())
                              .build())
              .build();
      changedObject.addChangedPhyLink(phyLink);
    }
  }

  private void underLayer2Allocate(RouteInfo routeInfo) {
    log.debug("start to make undeerlayer resource to allocate");

    for (String eqId : routeInfo.getEqIdList()) {
      String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
      Node node = changedObject.getChangedPhyNode(nodeId);
      Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
      Optional<Equipments> optional = nodeAttr.getEquipments().stream().filter(eq -> eq.getEquipmentId().equals(eqId)).findAny();
      if (!optional.isPresent()) {
        log.error("impossible, the EQ must be inside node {}", eqId);
        continue;
      }
      allocateEq(node, optional.get());
    }

    for (String xcId : routeInfo.getXcIdList()) {
      String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
      Node node = changedObject.getChangedPhyNode(nodeId);
      Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
      Optional<CrossConnections> optional = nodeAttr.getCrossConnections().stream().filter(xc -> xc.getCrossConnectionId().getValue().equals(xcId)).findAny();
      if (!optional.isPresent()) {
        log.error("impossible, the XC must be inside node {}", xcId);
        continue;
      }
      allocateXc(node, optional.get());
    }

    for (String tpId : routeInfo.getTpIdList()) {
      String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
      Node node = changedObject.getChangedPhyNode(nodeId);

      Optional<TerminationPoint> optional = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(tpId)).findAny();
      if (!optional.isPresent()) {
        log.error("impossible, the TP must be inside node {}", tpId);
      }
      allocateTp(node, optional.get());
    }

    for (String phyLinkId : routeInfo.getPhyLinkIdList()) {
      String srcNodeId = PhysicalLinkIdNamingRule.getNodeAId(phyLinkId);
      Node srcNode = changedObject.getChangedPhyNode(srcNodeId);
      Physical srcNodeAttr = srcNode.getAugmentation(Node1.class).getPhysical();

      Optional<InternalLinks> optional = srcNodeAttr.getInternalLinks().stream().filter(il -> il.getLinkRef().equals(phyLinkId)).findAny();
      if (!optional.isPresent()) {
        log.error("impossible, the internalLink must be inside node {}", phyLinkId);
      }
      allocateInternalLink(srcNode, optional.get());

      String dstNodeId = PhysicalLinkIdNamingRule.getNodeAId(phyLinkId);
      if (srcNode.equals(dstNodeId)) {
        continue;
      }

      Node dstNode = changedObject.getChangedPhyNode(dstNodeId);
      Physical dstNodeAttr = dstNode.getAugmentation(Node1.class).getPhysical();

      optional = dstNodeAttr.getInternalLinks().stream().filter(il -> il.getLinkRef().equals(phyLinkId)).findAny();
      if (!optional.isPresent()) {
        log.error("impossible, the internalLink must be inside node {}", phyLinkId);
      }
      allocateInternalLink(dstNode, optional.get());
    }

    log.debug("make underlayer resource to allocate done");
  }


  private void allocateXc(Node cfgNode, CrossConnections cfg) {
    CrossConnections newXc = new CrossConnectionsBuilder(cfg)
            .setAdminState(AdminStatus.Down)
            .setImplementState(ImplementState.Allocate)
            .build();

    Physical cfgNodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();
    List<CrossConnections> newList = new ArrayList<>();
    Iterator<CrossConnections> iter = cfgNodeAttr.getCrossConnections().iterator();
    while (iter.hasNext()) {
      CrossConnections oldTp = iter.next();
      if (oldTp.getCrossConnectionId().getValue().equals(cfg.getCrossConnectionId().getValue())) {
        newList.add(newXc);
      } else {
        newList.add(oldTp);
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

  private void allocateTp(Node cfgNode, TerminationPoint cfg) {
    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical cfgTpAttr = cfg.getAugmentation(TerminationPoint1.class).getPhysical();
    TerminationPoint newTp = new TerminationPointBuilder(cfg)
            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(cfgTpAttr)
                            .setAdminState(AdminStatus.Down)
                            .setImplementState(ImplementState.Allocate)
                            .build())
                    .build())
            .build();

    List<TerminationPoint> newList = new ArrayList<>();
    Iterator<TerminationPoint> iter = cfgNode.getTerminationPoint().iterator();
    while (iter.hasNext()) {
      TerminationPoint oldTp = iter.next();
      if (oldTp.getTpId().getValue().equals(cfg.getTpId().getValue())) {
        newList.add(newTp);
      } else {
        newList.add(oldTp);
      }
    }
    cfgNode = new NodeBuilder(cfgNode).setTerminationPoint(newList).build();
    changedObject.addChangedPhyNode(cfgNode);
  }

  private void allocateEq(Node cfgNode, Equipments cfg) {
    Equipments newEq = new EquipmentsBuilder(cfg)
            .setAdminState(AdminStatus.Down)
            .setImplementState(ImplementState.Allocate)
            .build();

    Physical cfgNodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();
    List<Equipments> newList = new ArrayList<>();
    Iterator<Equipments> iter = cfgNodeAttr.getEquipments().iterator();
    while (iter.hasNext()) {
      Equipments oldEq = iter.next();
      if (oldEq.getEquipmentId().equals(cfg.getEquipmentId())) {
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
  }

  private void allocateInternalLink(Node cfgNode, InternalLinks cfg) {
    InternalLinks newIl = new InternalLinksBuilder(cfg)
            .setAdminState(AdminStatus.Down)
            .setImplementState(ImplementState.Allocate)
            .build();

    Physical cfgNodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();
    List<InternalLinks> newList = new ArrayList<>();
    Iterator<InternalLinks> iter = cfgNodeAttr.getInternalLinks().iterator();
    while (iter.hasNext()) {
      InternalLinks oldIl = iter.next();
      if (oldIl.getLinkRef().equals(cfg.getLinkRef())) {
        newList.add(newIl);
      } else {
        newList.add(oldIl);
      }
    }

    cfgNode = new NodeBuilder(cfgNode).addAugmentation(Node1.class, new Node1Builder()
                    .setPhysical(new PhysicalBuilder(cfgNode.getAugmentation(Node1.class).getPhysical())
                            .setInternalLinks(newList)
                            .build())
                    .build())
            .build();
    changedObject.addChangedPhyNode(cfgNode);
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
        if (! isLatestImpledTunnel(ochLink, tunnel.getTunnelId().getValue())) {
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
            Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
            if (! isLatestImpledOchLink(siteLink, ochLink.getLinkId().getValue())) {
              continue;
            }

            RouteInfo siterInfo = new RouteInfo();
            siterInfo.parse(siteLinkAttr.getExplictRoute().getRoute());
            rInfoMap.put(siteLink.getLinkId().getValue(), siterInfo);
            tunnelRouteInfo.extend(siterInfo);
          }
        }
      }
    }
    return tunnelRouteInfo;
  }

  private boolean isLatestImpledTunnel(Link ochLink, String tunnelId) {
    List<SupportedTunnel> supportedTunnelList = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class).getSupportedTunnel();
    List<String> tunnelIds = new ArrayList<>();
    for (SupportedTunnel sTunnel : supportedTunnelList) {
      if (sTunnel.getTunnelRef().getValue().equals(tunnelId)) {
        continue;
      }
      tunnelIds.add(sTunnel.getTunnelRef().getValue());
    }

    if (tunnelIds.size() > 0) {
      List<Tunnel> tunnelList = tunnelDao.listAllTunnelByIds(tunnelIds);
      for (Tunnel tunnel : tunnelList) {
        if (tunnel.getImplementState().equals(ImplementState.Implement) || tunnel.getImplementState().equals(ImplementState.PartialImplement)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean isLatestImpledOchLink(Link siteLink, String ochLinkId) {
    Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
    List<String> ochLinkIds = new ArrayList<>();
    for (SupportedLink sLink : siteLinkAttr.getSupportedLink()) {
      if (sLink.getLinkRef().getValue().equals(ochLinkId)) {
        continue;
      }
      ochLinkIds.add(sLink.getLinkRef().getValue());
    }

    if (ochLinkIds.size() > 0) {
      List<Link> ochLinkList = ochLinkDao.listAllOchLinksByIds(ochLinkIds);
      for (Link link : ochLinkList) {
        ImplementState implState = link.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch().getImplementState();
        if (implState.equals(ImplementState.Implement) || implState.equals(ImplementState.PartialImplement)) {
          return false;
        }
      }
    }
    return true;
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
