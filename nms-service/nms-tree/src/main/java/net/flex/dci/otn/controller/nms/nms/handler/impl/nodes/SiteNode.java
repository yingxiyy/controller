/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.nodes;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.PhyLink;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * @author: xinyzhao
 * @date: 2021/4/6
 */
@Slf4j
public class SiteNode extends AbstractTopoNode {


    public SiteNode(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }


    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all Tunnels which passthrogh the site NODE. with topo:{}, node: {}",
                topologyRef.getValue(), nodeRef.getValue());

        Node ntNode = netconfTopology.getSiteNode(nodeRef.getValue());
        if (ntNode == null) {
            throw new Exception(
                    "cannnot find required SITE Node");
        }

        Node1 siteNode = ntNode.getAugmentation(Node1.class);
        List<Tunnel> output = new LinkedList<>();
        Set<Tunnel> rst = new HashSet<>();

        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        if (siteNode.getSite() != null && siteNode.getSite().getSupportingRack() != null) {
            for (SupportingRack sr : siteNode.getSite().getSupportingRack()) {
                for (SupportingNe sn : sr.getSupportingNe()) {
                    rst.addAll(new PhyNe(netconfTopology).getTunnels(phyTopoId, sn.getNodeRef()));
                }
            }
        }

        output.addAll(rst);
        return output;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteNodes(
            TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all Site node based on site NODE. with topo:{}, node: {}",
                topologyRef.getValue(), nodeRef.getValue());

        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            throw new Exception(
                    "cannnot find required SITE Node");
        }

        List<Node> output = new LinkedList<>();
        output.add(ntNode);
        return output;
    }

    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all PHY link based on SITE NODE. with topo:{}, node: {}",
                topologyRef.getValue(), nodeRef.getValue());

        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            throw new Exception(
                    "cannnot find required Site Node");
        }

        List<Link> subPhyLinks = netconfTopology.listPhyLinkUnderSiteNode(ntNode);
//        List<Node> subPhyNode = netconfTopology.listPhyNodeUnderSite(nodeRef.getValue());
//        List<Link> subPhyLinks = netconfTopology.listPhyLinkByNodes(subPhyNode);
//        List<Link> output = new LinkedList<>();
//        Set<Link> rst = new HashSet<>();
//        PhyNe phyNe = new PhyNe(netconfTopology);
//        for (SupportingNode sn : ntNode.getSupportingNode()) {
//            if (!sn.getTopologyRef().getValue().equals(PHY_TOPO_KEY)) {
//                log.error("find a site node's supporting node, which isn't PHY node");
//                continue;
//            }
//            List<Link> neLinks = phyNe.getPhyLinks(sn.getTopologyRef(), sn.getNodeRef());
//            if (neLinks != null) {
//                rst.addAll(neLinks);
//            }
//        }

//        output.addAll(rst);
//        return output;
        return subPhyLinks;
    }

    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all site link based on SITE NODE. with topo:{}, node: {}",
                topologyRef.getValue(), nodeRef.getValue());

        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            throw new Exception(
                    "cannnot find required Site Node");
        }

        Set<Link> rst = new HashSet<>();
        List<Link> phyLinks = getPhyLinks(topologyRef, nodeRef);

        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        PhyLink phyLink = new PhyLink(netconfTopology);
        for (Link pl : phyLinks) {
            List<Link> siteLinks = phyLink.getSiteLinks(phyTopoId, pl.getLinkId());
            if (siteLinks != null) {
                rst.addAll(siteLinks);
            }
        }

        List<Link> output = new LinkedList<>();
        output.addAll(rst);
        return output;
    }


    @Override
    public List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug(
                "start get all OCH link which passthrogh the site NODE. with topo:{}, node: {}",
                topologyRef.getValue(), nodeRef.getValue());

        List<Link> phyLinks = getPhyLinks(topologyRef, nodeRef);
        PhyLink phyLink = new PhyLink(netconfTopology);
        List<Link> output = new LinkedList<>();
        Set<Link> rst = new HashSet<>();
        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        for (Link link : phyLinks) {
            rst.addAll(phyLink.getOchLinks(phyTopoId, link.getLinkId()));
        }
        output.addAll(rst);
        return output;
    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all PHY link based on SITE NODE. with topo:{}, node: {}",
                topologyRef.getValue(), nodeRef.getValue());

        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            throw new Exception(
                    "cannnot find required Site Node");
        }
        List<String> refPhyNodesIds = ntNode.getSupportingNode().stream()
                .filter(supportingNode -> supportingNode.getTopologyRef().getValue()
                        .equals(PHY_TOPO_KEY))
                .map(supportingNode -> supportingNode.getNodeRef().getValue())
                .collect(Collectors.toList());
        List<Node> phyNodes = netconfTopology.getPhyNodesByIds(refPhyNodesIds);
        return phyNodes;
    }


}
