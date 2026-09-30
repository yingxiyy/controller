/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import net.flex.dci.otc.mongo.dao.impl.AdapterDaoImpl;
import net.flex.dci.otc.mongo.dao.impl.TelemetryDaoImpl;
import net.flex.dci.otn.controller.nms.model.SiteLinkInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.attribute.Ne;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.SourceTp;

@Slf4j
public class DCISystemMap {


    private static DCISystemMap inst;

    //siteMap <siteNodeID, siteFriendlyName>
    private Map<String, String> siteMap = new HashMap<>();

    //siteMap <phyNodeID, siteLinkId>
    private Map<String, SiteLinkInfo> phyNodeSiteLinkMap = new HashMap<>();

    //siteMap <siteLinkId， List<phyNodeID>> all nodes on this siteLinks (include TPC, OPC)
    private Map<String, List<String>> siteLinkNodesMap = new HashMap<>();
    //siteLinkMap <siteLinkId， SiteLinkInfo>
    private Map<String, SiteLinkInfo> siteLinkInfoMap = new HashMap<>();
    private static DCISystemMap Instance = new DCISystemMap();

    public static DCISystemMap instance() {
        return Instance;
    }


    private NetconfTopology nt;

    private AdapterDao adapterDao;

    private TelemetryDao telemetryDao;

    private DCISystemMap() {
//        nt = SpringBeanFinder.getBean(MongoDao.class);
//        mongoDaoUtil = SpringBeanFinder.getBean(MongoDaoUtil.class);
        nt = SpringBeanFinder.getBean(NetconfTopology.class);
        adapterDao = SpringBeanFinder.getBean(AdapterDaoImpl.class);
        telemetryDao = SpringBeanFinder.getBean(TelemetryDaoImpl.class);
    }

    /**
     * export map <nodeId, SiteFriendlyName>
     *
     * @return
     */
    public Map<String, String> getSiteMap() {
        if (siteMap.isEmpty()) {
            buildSiteMap();
        }
        return this.siteMap;
    }

    public void buildSiteMap() {
        log.debug("build site map<siteID, siteFirendlyName>");
        List<Node> ntNodes = null;
        Topology topo = nt.getSiteTopology();
        if (topo == null) {
            return;
        }
        ntNodes = topo.getNode();

        if (ntNodes == null) {
            return;
        }
        for (Node nt : ntNodes) {
            siteMap.put(nt.getNodeId().getValue(),
                    nt.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                            .getSite().getFriendlyName());
        }
    }

    public void addSiteNode(Node node) {
        if (siteMap.isEmpty()) {
            buildSiteMap();
        }
        siteMap.put(node.getNodeId().getValue(),
                node.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                        .getSite().getFriendlyName());
    }

    public void removeSiteNode(Node node) {
        if (siteMap.isEmpty()) {
            buildSiteMap();
        }
        siteMap.remove(node.getNodeId().getValue());
    }

    /**
     * export the map
     *
     * @return key is phyNodeId, value is siteLinkInfo (linkId, plane, siteLinkFriendlyName)
     */
    public Map<String, SiteLinkInfo> getPhyNodeSiteLinkMap() {
        if (this.phyNodeSiteLinkMap.isEmpty()) {
            buildPhyNodeSiteLinkMap();
        }
        return this.phyNodeSiteLinkMap;
    }

    public void buildPhyNodeSiteLinkMap() {
        log.debug("build phy node site link map, <phyNodeId, siteLinkInfo>");

        Topology topo = nt.getSiteTopology();
        if (topo == null) {
            return;
        }

        List<Link> ntLinks = topo.getLink();
        if (ntLinks == null) {
            return;
        }

        for (Link siteNtLink : ntLinks) {
            SiteLinkInfo siteLinkInfo = addSiteLink(siteNtLink);
            if (siteLinkInfo == null) {
                continue;
            }

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink = siteNtLink
                    .getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);

            //find out och link and then tunnel for TPC ne
            if (siteLink.getSite().getSupportedLink() == null) {
                continue;
            }

            for (SupportedLink ochLinkId : siteLink.getSite().getSupportedLink()) {
//                Link ochNtLink = this.mongoDaoUtil.getLink(new TopologyId(OCH_TOPO_KEY),
//                        ochLinkId.getLinkRef().getValue(), DataStoreType.OPERATIONAL);
                Link ochNtLink = nt.getOchLink(ochLinkId.getLinkRef().getValue());
                if (ochNtLink == null) {
                    continue;
                }

                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1 ochLink;
                ochLink = ochNtLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class);
                if (ochLink == null) {
                    continue;
                }

                //find out OCH link related two TPC nes.
                phyNodeSiteLinkMap
                        .put(ochNtLink.getSource().getSourceNode().getValue(), siteLinkInfo);
                phyNodeSiteLinkMap
                        .put(ochNtLink.getDestination().getDestNode().getValue(), siteLinkInfo);
                List<String> phyNodeIds = siteLinkNodesMap.get(siteNtLink.getLinkId().getValue());
                if (phyNodeIds == null) {
                    phyNodeIds = new LinkedList<>();
                }
                phyNodeIds.add(ochNtLink.getSource().getSourceNode().getValue());
                phyNodeIds.add(ochNtLink.getDestination().getDestNode().getValue());
                siteLinkNodesMap.put(siteNtLink.getLinkId().getValue(), phyNodeIds);
            }
        }
        this.phyNodeSiteLinkMap = phyNodeSiteLinkMap;
    }

    public void addTunnel(Tunnel tunnel) {
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink tunnelSl : tunnel
                .getSupportingLink()) {
//            Link ochNtLink = this.mongoDaoUtil
//                    .getLink(new TopologyId(OCH_TOPO_KEY), tunnelSl.getLinkRef().getValue(),
//                            DataStoreType.OPERATIONAL);
            Link ochNtLink = nt.getOchLink(tunnelSl.getLinkRef().getValue());
            if (ochNtLink == null) {
                continue;
            }

            for (SupportingLink ochSl : ochNtLink.getSupportingLink()) {
//                Link siteNtLink = this.mongoDaoUtil
//                        .getLink(new TopologyId(SITE_TOPO_KEY), ochSl.getLinkRef().getValue(),
//                                DataStoreType.OPERATIONAL);
                Link siteNtLink = nt.getSiteLink(ochSl.getLinkRef().getValue());
                if (siteNtLink == null) {
                    continue;
                }

                SiteLinkInfo siteLinkInfo = siteLinkInfoMap.get(siteNtLink.getLinkId().getValue());
                if (siteLinkInfo == null) {
                    log.error("bug, the siteLinkInfo must be existed.");
                }

                extractDNodeId(tunnel.getDestinationTp(), siteLinkInfo, true);
                extractSNodeId(tunnel.getSourceTp(), siteLinkInfo, true);
            }
        }
    }

    public void removeTunnel(Tunnel tunnel) {
        extractDNodeId(tunnel.getDestinationTp(), null, false);
        extractSNodeId(tunnel.getSourceTp(), null, false);
    }

    private void extractDNodeId(List<DestinationTp> tpList, SiteLinkInfo siteLinkInfo,
            boolean add) {
        for (DestinationTp tp : tpList) {
            String[] ids = tp.getTpRef().getValue().split("#");
            String nodeId = ids[0] + "#" + ids[1];
            buildPhyNodeMap(nodeId, siteLinkInfo, add);
        }
    }

    private void extractSNodeId(List<SourceTp> tpList, SiteLinkInfo siteLinkInfo, boolean add) {

        for (SourceTp tp : tpList) {
            String[] ids = tp.getTpRef().getValue().split("#");
            String nodeId = ids[0] + "#" + ids[1];
            buildPhyNodeMap(nodeId, siteLinkInfo, add);
        }
    }

    private void buildPhyNodeMap(String nodeId, SiteLinkInfo siteLinkInfo, boolean add) {
        if (add) {
            phyNodeSiteLinkMap.put(nodeId, siteLinkInfo);

            List<String> phyNodeIds = siteLinkNodesMap.get(siteLinkInfo.getId());
            if (phyNodeIds == null) {
                phyNodeIds = new LinkedList<>();
            }
            phyNodeIds.add(nodeId);
            siteLinkNodesMap.put(siteLinkInfo.getId(), phyNodeIds);
        } else {
            //for tunnel, the nodeId can be reused with multiple tunnels.
            //thus remove the node in map when remove tunnel is failure.
            //for avoid this issue. I will extension getPhyNode function
            SiteLinkInfo siteLinkInfo1 = phyNodeSiteLinkMap.remove(nodeId);
            if (siteLinkInfo1 == null) {
                return;
            }
            List<String> phyNodeIds = siteLinkNodesMap.get(siteLinkInfo1.getId());
            if (phyNodeIds == null) {
                return;
            }
            Iterator<String> iter = phyNodeIds.iterator();
            while (iter.hasNext()) {
                String id = iter.next();
                if (id.equals(nodeId)) {
                    iter.remove();
                    break;
                }
            }
        }
    }

    public SiteLinkInfo addSiteLink(Link siteNtLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink = siteNtLink
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
        if (siteLink == null || siteLink.getSite() == null) {
            return null;
        }

        List<SupportingLink> supportingNtLinks = siteNtLink.getSupportingLink();
        if (supportingNtLinks == null) {
            return null;
        }

        SiteLinkInfo siteLinkInfo = new SiteLinkInfo(siteNtLink.getLinkId().getValue(),
                siteLink.getSite().getPlaneName(), siteLink.getSite().getFriendlyName());
        siteLinkInfoMap.put(siteNtLink.getLinkId().getValue(), siteLinkInfo);

        //find out OTS phy link for OPC ne
        for (SupportingLink sLink : supportingNtLinks) {
//            Link phyNtLink = this.mongoDaoUtil
//                    .getLink(new TopologyId(PHY_TOPO_KEY),
//                            sLink.getLinkRef().getValue(), DataStoreType.OPERATIONAL);
            Link phyNtLink = nt.getPhyLink(sLink.getLinkRef().getValue());
            if (phyNtLink != null) {
                Link1 phyLink;
                phyLink = phyNtLink.getAugmentation(
                        Link1.class);
                if (phyLink == null) {
                    continue;
                }
                if (phyLink.getPhysical().getLinkType().equals(LinkType.OtsLink)) {
                    //find out OTS means two OPC nes.
                    phyNodeSiteLinkMap
                            .put(phyNtLink.getSource().getSourceNode().getValue(), siteLinkInfo);
                    phyNodeSiteLinkMap
                            .put(phyNtLink.getDestination().getDestNode().getValue(), siteLinkInfo);

                    List<String> phyNodeIds = siteLinkNodesMap
                            .get(siteNtLink.getLinkId().getValue());
                    if (phyNodeIds == null) {
                        phyNodeIds = new LinkedList<>();
                    }
                    phyNodeIds.add(phyNtLink.getSource().getSourceNode().getValue());
                    phyNodeIds.add(phyNtLink.getDestination().getDestNode().getValue());
                    siteLinkNodesMap.put(siteNtLink.getLinkId().getValue(), phyNodeIds);
                }
            }
        }
        return siteLinkInfo;
    }

    public void removeSiteLink(Link siteNtLink) {
        List<String> phyNodeIds = siteLinkNodesMap.get(siteNtLink.getLinkId().getValue());
        for (String id : phyNodeIds) {
            phyNodeSiteLinkMap.remove(id);
        }
        siteLinkNodesMap.remove(siteNtLink.getLinkId().getValue());
        siteLinkInfoMap.remove(siteNtLink.getLinkId().getValue());
    }

    public void updateSiteLink(Link siteNtLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink = siteNtLink
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
        if (siteLink == null || siteLink.getSite() == null) {
            return;
        }

        List<SupportingLink> supportingNtLinks = siteNtLink.getSupportingLink();
        if (supportingNtLinks == null) {
            return;
        }

        SiteLinkInfo siteLinkInfo = siteLinkInfoMap.get(siteNtLink.getLinkId().getValue());
        if (siteLinkInfo == null) {
            return;
        }

        siteLinkInfo.setName(siteLink.getSite().getFriendlyName());
        siteLinkInfo.setName(siteLink.getSite().getPlaneName());
        siteLinkInfoMap.put(siteNtLink.getLinkId().getValue(), siteLinkInfo);
    }

    public void rebuildPhyNodeSiteLinkMap(String nodeId) {
//        Node ntNode = this.mongoDaoUtil.getPhyNode(nodeId, DataStoreType.OPERATIONAL);
        Node ntNode = nt.getPhyNode(nodeId);
        if (ntNode == null) {
            log.debug("the node {} has been removed.", nodeId);
            return;
        }
        Node1 phyNode = ntNode.getAugmentation(Node1.class);
        if (phyNode == null) {
            log.error("find a phyNode, node:{}, it hasn't provide physical augment.",
                    ntNode.getNodeId().getValue());
            return;
        }
        if (phyNode.getPhysical().getNodeType().equals(NodeType.OD)) {
            //OP4C doesn't need rebuild cache.
            //because when OPC4 cannot find site link, means no site link over it.
            return;
        }
        if (phyNode.getPhysical().getInternalLinks() == null) {
            //this is TPC4 and it hasn't internal link, means on tunnel over it.
            return;
        }

        for (InternalLinks il : phyNode.getPhysical().getInternalLinks()) {
            if (il.getLinkRef() == null) {
                log.warn("Internal link {} has no link ref.", il.getLinkName());
                continue;
            }
//            Link ntLink = this.mongoDaoUtil
//                    .getLink(new TopologyId(PHY_TOPO_KEY), il.getLinkRef(),
//                            DataStoreType.OPERATIONAL);
            Link ntLink = nt.getPhyLink(il.getLinkRef());
            if (ntLink == null) {
                log.error("find a phyNode's internal link {} cannot find in phyTopo.",
                        il.getLinkRef());
                continue;
            }

            Link1 phyLink = ntLink.getAugmentation(Link1.class);
            if (phyLink == null) {
                log.error("the phy link cannot get augment info {}",
                        ntLink.getLinkId().getValue());
                continue;
            }

            if (phyLink.getPhysical().getLinkType().equals(LinkType.OsLink)) {
                //the peer must ve OPC NE, we can get otsLinkInfo based the OPC NE
                if (ntLink.getSource().getSourceNode().getValue().equals(nodeId)) {
                    SiteLinkInfo siteLinkInfo = getPhyNodeSiteLinkMap()
                            .get(ntLink.getDestination().getDestNode().getValue());
                    if (siteLinkInfo != null) {
                        buildPhyNodeMap(nodeId, siteLinkInfo, true);
                        break;
                    }
                } else {
                    SiteLinkInfo siteLinkInfo = getPhyNodeSiteLinkMap()
                            .get(ntLink.getSource().getSourceNode().getValue());
                    if (siteLinkInfo != null) {
                        buildPhyNodeMap(nodeId, siteLinkInfo, true);
                        break;
                    }
                }
            }
        }
    }

    public Map<String, Adapter> getAdapterMap() {
        Map<String, Adapter> adapterMap = new HashMap<>();
        List<Adapter> adapters = adapterDao.getAdapters();
        for (Adapter adapter : adapters) {
            if (adapter.getNe() != null) {
                for (Ne ne : adapter.getNe()) {
                    adapterMap.put(ne.getNodeId().getValue(), adapter);
                }
            }
        }
//        }
        return adapterMap;
    }

    public Map<String, TelemetryServer> getCollectorMap() {
        Map<String, TelemetryServer> collectorMap = new HashMap<>();
        List<TelemetryServer> telemetryServers = telemetryDao.listTelemetryServers();
        for (TelemetryServer server : telemetryServers) {
            if (server.getNe() != null) {
                for (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.server.attribute.Ne ne : server
                        .getNe()) {
                    collectorMap.put(ne.getNodeId().getValue(), server);
                }
            }
        }
        return collectorMap;
    }
}


