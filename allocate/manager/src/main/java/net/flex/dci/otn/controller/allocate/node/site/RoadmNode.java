package net.flex.dci.otn.controller.allocate.node.site;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetRoadmInfoOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetRoadmInfoOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.roadm.info.output.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.info.item.ODDevice;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.info.item.ODDeviceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.info.item.TDDevice;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.info.item.TDDeviceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink;

import java.util.*;

@Slf4j
public class RoadmNode {
  private String nodeId;
  private String tunnelId;


  private PhyNodeDao phyNodeDao;
  Map<String, Node> nodeMap;

  public RoadmNode(String nodeId, String tunnelId) {
    this.nodeId = nodeId;
    this.tunnelId = tunnelId;

  }

  /**
   * export the roadm node (site node)
   * supported siteLink, and relationship of them.
   */
  public GetRoadmInfoOutput getLinkInfo() {
    log.debug("get roadm link info on {}", nodeId);

    nodeMap = new HashMap<>();
    phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);

    SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
    SiteNodeDao siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);

    List<Link> siteLinkList = siteLinkDao.getSiteLinksEndWithSite(nodeId);
    Node siteNode = siteNodeDao.getSiteNodeById(nodeId);
    GetRoadmInfoOutputBuilder ob = new GetRoadmInfoOutputBuilder();

    if (siteNode != null && siteNode.getAugmentation(Node1.class).getSite().getSiteLinkRelation() != null) {
      List<SiteLinkRelation> relations = siteNode.getAugmentation(Node1.class).getSite().getSiteLinkRelation();
      ob = ob.setFriendlyName(siteNode.getAugmentation(Node1.class).getSite().getFriendlyName())
              .setSiteLinkRelation(extendRelationInfo(relations))
              .setSiteLink(extractSiteLinkInfo(siteNode, relations, siteLinkList))
              .setWssTpInfo(extractWssTpInfo(relations))
              .setTunnelSiteLinkIds(extractUnderLayerSiteLinkId(nodeId, tunnelId));
    }

    return ob.build();
  }

  /**
   * base tunnel routing info find out siteLinkID, and findout based on nodeId
   * 找出支撑这条tunnel的siteLink，且只是通告指定site的;
   *
   * @param nodeId
   * @param tunnelId
   * @return
   */
  private List<String> extractUnderLayerSiteLinkId(String nodeId, String tunnelId) {
    if (tunnelId == null || tunnelId.isEmpty()) {
      return null;
    }

    TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
    OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
    Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
    if (tunnel == null) {
      return null;
    }

    List<String> outputList = new ArrayList<>();
    for (SupportingLink sl : tunnel.getSupportingLink()) {
      Link link = ochLinkDao.getOchLinkByLinkId(sl.getLinkRef().getValue());
      for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink ochSl : link.getSupportingLink()) {
        String linkId = ochSl.getLinkRef().getValue();
        if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
          if (linkId.contains(nodeId)) {
            outputList.add(linkId);
          }
        }
      }
    }
    return outputList;
  }

  /**
   * 输出WSS TP相关信息
   * @param relations
   * @return
   */
  private List<WssTpInfo> extractWssTpInfo(List<SiteLinkRelation> relations) {
    List<WssTpInfo> outputList = new ArrayList<>();

    for (SiteLinkRelation relation : relations) {
      String wssLinkId = relation.getWssLinkIdBetweenAZ();
      String aTpId = PhysicalLinkIdNamingRule.getTpAId(wssLinkId);
      String zTpId = PhysicalLinkIdNamingRule.getTpZId(wssLinkId);

      outputList.add(new WssTpInfoBuilder().setTpId(aTpId)
              .setKey(new WssTpInfoKey(aTpId))
              .setNodeFriendlyName(getPhyNodeFriendlyName(aTpId))
              .setTpFriendlyName(getPhyTpFriendlyName(aTpId))
              .setEquipId(PhysicalTpIdNamingRule.getEquipId(aTpId))
              .build());


      outputList.add(new WssTpInfoBuilder().setTpId(zTpId)
              .setKey(new WssTpInfoKey(zTpId))
              .setNodeFriendlyName(getPhyNodeFriendlyName(zTpId))
              .setTpFriendlyName(getPhyTpFriendlyName(zTpId))
              .setEquipId(PhysicalTpIdNamingRule.getEquipId(zTpId))
              .build());
    }
    return outputList;
  }

  private String getPhyTpFriendlyName(String tpId) {
    String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
    for (TerminationPoint tp : nodeMap.get(nodeId).getTerminationPoint()) {
      if (tp.getTpId().getValue().equals(tpId)) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName();
      }
    }
    String[] ids = tpId.split("#");
    return ids[ids.length - 1];
  }


  private String getPhyNodeFriendlyName(String tpId) {
    String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
    if (!nodeMap.containsKey(nodeId)) {
      nodeMap.put(nodeId, phyNodeDao.getConfigPhyNodeById(nodeId));
    }
    return  nodeMap.get(nodeId).getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class).getPhysical().getFriendlyName();
  }

  /**
   * 基于relation中的site-link 提取对应的friendlyName
   *
   * @param siteNode
   * @param relations
   * @param siteLinkList
   * @return
   */
  private List<SiteLink> extractSiteLinkInfo(Node siteNode, List<SiteLinkRelation> relations, List<Link> siteLinkList) {
    List<SiteLink> outputList = new ArrayList<>();
    for (SiteLinkRelation relation : relations) {
      String siteLinkId = relation.getLinkaId();
      outputList.add(extractSiteLinkInfoById(siteNode, siteLinkId, siteLinkList));

      siteLinkId = relation.getLinkzId();
      outputList.add(extractSiteLinkInfoById(siteNode, siteLinkId, siteLinkList));
    }
    return outputList;
  }

  private SiteLink extractSiteLinkInfoById(Node siteNode, String siteLinkId, List<Link> siteLinkList) {
    String siteNodeId = siteNode.getNodeId().getValue();

    for (Link link : siteLinkList) {
      if (link.getLinkId().getValue().equals(siteLinkId)) {
        String phyNodeId;
        if (link.getSource().getSourceTp().getValue().contains(siteNodeId)) {
          phyNodeId = PhysicalTpIdNamingRule.getNodeId(link.getSource().getSourceTp().getValue());
        } else {
          phyNodeId = PhysicalTpIdNamingRule.getNodeId(link.getDestination().getDestTp().getValue());
        }
        Node opticalDevice = phyNodeDao.getConfigPhyNodeById(phyNodeId);
        Physical phyNodeAttr = opticalDevice.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class).getPhysical();
        ODDevice od = new ODDeviceBuilder()
                .setNodeId(opticalDevice.getNodeId().getValue())
                .setFriendlyName(phyNodeAttr.getFriendlyName())
                .setAlarmState(phyNodeAttr.getAlarmState())
                .build();

        Site siteLinkAttr = link.getAugmentation(Link1.class).getSite();
        Set<String > tdNodeIds = new HashSet<>();
        for (SupportedLink sl : siteLinkAttr.getSupportedLink()) {
          String linkId = sl.getLinkRef().getValue();
          if (OchLinkIdNamingRule.isOchLink(linkId)) {
            String nodeA = OchLinkIdNamingRule.nodeA(linkId);
            if (nodeA.contains(siteNodeId)) {
              tdNodeIds.add(nodeA);
            } else {
              String nodeZ = OchLinkIdNamingRule.nodeZ(linkId);
              tdNodeIds.add(nodeZ);
            }
          }
        }
        List<Node> electricDevices = phyNodeDao.listConfigPhyNodeByIds(tdNodeIds);
        List<TDDevice> tdDevices = new ArrayList<>();
        AlarmSeverity totalAlarmState = AlarmSeverity.Cleared;
        for (Node node : electricDevices) {
          Physical eletricNodeAttr = node.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class).getPhysical();
          tdDevices.add(new TDDeviceBuilder()
                  .setNodeId(node.getNodeId().getValue())
                  .setFriendlyName(eletricNodeAttr.getFriendlyName())
                  .setAlarmState(eletricNodeAttr.getAlarmState())
                  .build());
          if (!eletricNodeAttr.getAlarmState().equals(AlarmSeverity.Unknown) && !eletricNodeAttr.getAlarmState().equals(AlarmSeverity.SynchronizationException)) {
            if (eletricNodeAttr.getAlarmState().getIntValue() > totalAlarmState.getIntValue()) {
              totalAlarmState = eletricNodeAttr.getAlarmState();
            }
          }
        }
        return new SiteLinkBuilder().setLinkId(siteLinkId)
                .setKey(new SiteLinkKey(siteLinkId))
                .setFriendlyName(link.getAugmentation(Link1.class).getSite().getFriendlyName())
                .setODDevice(od)
                .setTDDevice(tdDevices)
                .setTDTotalAlarmState(totalAlarmState)
                .build();
      }
    }
    return null;
  }

  /**
   * 原始的siteLinkRelation信息中没有包含wssLink tp点信息，这里补充上
   * @param relations
   * @return
   */
  private List<SiteLinkRelation> extendRelationInfo(List<SiteLinkRelation> relations) {
    List<SiteLinkRelation> outputList = new ArrayList<>();
    for (SiteLinkRelation relation : relations) {
      String wssLinkId = relation.getWssLinkIdBetweenAZ();

      outputList.add(new SiteLinkRelationBuilder(relation)
              .setAtpWssLink(PhysicalLinkIdNamingRule.getTpAId(wssLinkId))
              .setZtpWssLink(PhysicalLinkIdNamingRule.getTpZId(wssLinkId))
              .build());
    }
    return outputList;
  }
}
