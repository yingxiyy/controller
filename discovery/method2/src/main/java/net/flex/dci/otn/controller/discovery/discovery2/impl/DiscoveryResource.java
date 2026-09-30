package net.flex.dci.otn.controller.discovery.discovery2.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

@Data
@Slf4j
public class DiscoveryResource {

    private PhyNodeDao phyNodeDao;
    private Map<String, Node> opNodeMap;
    private Map<String, Node> cfgNodeMap;

    private Map<String, Link> waitingSiteLinkList;  //key is siteLinkId
    private Map<String, List<Link>> waitingOchLinkList;  //key is siteLinkId, means OCHList contains in this siteLink
    private Map<String, List<Tunnel>> waitingTunnelList;         //key is OchLinkId, means tunnels contains in this ochLink

    public DiscoveryResource() {
        phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);

        opNodeMap = new HashMap<>();
        cfgNodeMap = new HashMap<>();

        waitingSiteLinkList = new HashMap<>();
        waitingOchLinkList = new HashMap<>();
        waitingTunnelList = new HashMap<>();
    }

    /**
     * 先找的OP node, 当opNode存在的时候discovery才可能
     *
     * @param nodeIdList
     */
    public void prepareResource(List<String> nodeIdList) {
        prepareNodeResource(nodeIdList);
        prepareLinkResource();
    }

    /**
     * based on OPC node, find out siteLink, and then OCH link, Tunnel and all Node
     *
     * @param
     */
    private void prepareLinkResource() {
        log.debug("find out related all links and TPC nodes based on siteLink");
        SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

        for (Node node : cfgNodeMap.values()) {
            String nodeId = node.getNodeId().getValue();
            List<Link> siteLinkList = siteLinkDao.queryWithNode(nodeId);
            if (siteLinkList == null || siteLinkList.isEmpty()) {
                continue;
            }
            for (Link link : siteLinkList) {
                waitingSiteLinkList.put(link.getLinkId().getValue(), link);
            }
        }
        //已经Impl了的siteLink上面的tunnel也需要discover
        waitingSiteLinkList.putAll(getImpledSiteLink(siteLinkDao.getSiteLinks()));

        OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
        for (String siteLinkId : waitingSiteLinkList.keySet()) {
            List<Link> ochLinkList = ochLinkDao.queryWithSiteLinkId(siteLinkId);
            if (ochLinkList == null || ochLinkList.isEmpty()) {
                continue;
            }
            waitingOchLinkList.put(siteLinkId, ochLinkList);
        }

        TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
        for (String siteLinkId : waitingOchLinkList.keySet()) {
            for (Link link : waitingOchLinkList.get(siteLinkId)) {
                String ochLinkId = link.getLinkId().getValue();
                List<Tunnel> tunnelList = tunnelDao.queryWithOchLinkId(ochLinkId);
                if (tunnelList == null || tunnelList.isEmpty()) {
                    continue;
                }

                List<Tunnel> unImpledTunnelList = new ArrayList<>();
                for (Tunnel tunnel : tunnelList) {
                    if (tunnel.getImplementState().equals(ImplementState.Implement)) {
                        continue;
                    } else {
                        unImpledTunnelList.add(tunnel);
                    }
                }

                waitingTunnelList.put(ochLinkId, unImpledTunnelList);
            }
        }

        //getAllTPC node based ochLink
        List<String> tpcNodeIds = new ArrayList<>();
        for (String siteLinkId : waitingOchLinkList.keySet()) {
            for (Link link : waitingOchLinkList.get(siteLinkId)) {
                String ochLinkId = link.getLinkId().getValue();
                String nodeA = OchLinkIdNamingRule.nodeA(ochLinkId);
                String nodeZ = OchLinkIdNamingRule.nodeZ(ochLinkId);
                if (!opNodeMap.containsKey(nodeA)) {
                    tpcNodeIds.add(nodeA);
                }
                if (!opNodeMap.containsKey(nodeZ)) {
                    tpcNodeIds.add(nodeZ);
                }
            }
        }
        if (tpcNodeIds.size() > 0) {
            prepareResource(tpcNodeIds);
        }
    }

    private Map<String, Link> getImpledSiteLink(List<Link> linkList) {
        Map<String, Link> linkMap = new HashMap<>();
        ;
        for (Link link : linkList) {
            if (link.getAugmentation(Link1.class).getSite().getImplementState()
                    .equals(ImplementState.Implement)) {
                linkMap.put(link.getLinkId().getValue(), link);
            }
        }
        return linkMap;
    }

    /**
     * 如果有nodeIdList, 就按nodeIDList查找op node数据库, 有数据的才有discovery的可能 基于opNode 数据库找出cfgNode export is
     * one opNodeList and cfgNodeList, the nodeID is same
     *
     * @param nodeIdList
     */
    private void prepareNodeResource(List<String> nodeIdList) {
        List<Node> opNodeList = new ArrayList<>();
        List<Node> cfgNodeList = new ArrayList<>();

        if (nodeIdList == null || nodeIdList.isEmpty()) {
            opNodeList = phyNodeDao.listOperPhyNodes();
        } else {
            for (String nodeId : nodeIdList) {
                Node node = phyNodeDao.getOpPhyNodeById(nodeId);
                if (node == null) {
                    log.debug("the node hasn't sync with device {}", nodeId);
                } else {
                    opNodeList.add(node);
                }
            }
        }

        //get correlated cfgNode
        Iterator<Node> iter = opNodeList.iterator();
        while (iter.hasNext()) {
            Node opNode = iter.next();
            String nodeId = opNode.getNodeId().getValue();
            Node cfgNode = phyNodeDao.getConfigPhyNodeById(nodeId);
            if (cfgNode == null) {
                log.debug("the node has opNode, but hasn't related configNode {}", nodeId);
                iter.remove();
            } else {
                cfgNodeList.add(cfgNode);
            }
        }

        opNodeMap = opNodeList.stream()
                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), node -> node));
        cfgNodeMap = cfgNodeList.stream()
                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), node -> node));
    }

    public Node getOpNode(String nodeId) {
        if (opNodeMap.containsKey(nodeId)) {
            return opNodeMap.get(nodeId);
        } else {
            return null;
        }
    }

}
