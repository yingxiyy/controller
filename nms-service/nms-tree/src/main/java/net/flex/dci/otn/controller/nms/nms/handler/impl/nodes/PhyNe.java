/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.nodes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.PhyLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteLink;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * @author: xinyzhao
 * @date: 2021/4/6
 */
@Slf4j
public class PhyNe extends AbstractTopoNode {

//    private PhyLink phyLink;


    public PhyNe(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        phyLink = new PhyLink(netconfTopology);
    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all PHY Nodes based on PHY NODE. with topo:{}, node: {}",
                topologyRef.getValue(),
                nodeRef.getValue());
        List<Node> output = new LinkedList<>();
        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            log.error(
                    "cannnot find required PHY Node " + nodeRef);
            return output;
        }
        output.add(ntNode);
        return output;
    }

    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all PHY link based on PHY NODE. with topo:{}, node: {}",
                topologyRef.getValue(),
                nodeRef.getValue());

        Node ntNode = netconfTopology.getPhyNode(nodeRef.getValue());
        if (ntNode == null) {
            throw new Exception("cannnot find required PHY Node");
        }

        List<Link> output = new LinkedList<>();
        Node1 phyNode = ntNode.getAugmentation(Node1.class);
        if (phyNode == null) {
            log.error("find a phyNode, node:{}, it hasn't provide physical augment.",
                    ntNode.getNodeId().getValue());
            return output;
        }

        if (phyNode.getPhysical().getInternalLinks() != null) {
            for (InternalLinks il : phyNode.getPhysical().getInternalLinks()) {
                if (il.getLinkRef() == null) {
                    log.warn("Internal link {} has no link ref.", il.getLinkName());
                    continue;
                }
                Link ntLink = netconfTopology.getLink(topologyRef, new LinkId(il.getLinkRef()));
                if (ntLink == null) {
                    log.error("find a phyNode's internal link {} cannot find in phyTopo.",
                            il.getLinkRef());
                    continue;
                }

                output.add(ntLink);
            }
        }

        return output;
    }

    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all Site link based on PHY NODE. with topo:{}, node: {}",
                topologyRef.getValue(),
                nodeRef.getValue());

        Set<Link> output = new HashSet<>();
        List<Link> phyNtLinks = getPhyLinks(topologyRef, nodeRef);
        for (Link link : phyNtLinks) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLink = link
                    .getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
            if (phyLink.getPhysical().getSupportedLink() != null) {
                for (SupportedLink sl : phyLink.getPhysical().getSupportedLink()) {
                    Link client = netconfTopology.getLink(sl.getTopologyRef(), sl.getLinkRef());

                    if (client != null && client.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                            != null) {
                        output.add(client);
                    }
                }
            }
        }

        return new LinkedList<Link>(output);
    }

    @Override
    public List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all OCH links which passthrogh the PHY NE. with topo:{}, node: {}",
                topologyRef.getValue(), nodeRef.getValue());

        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            throw new Exception(
                    "cannnot find required PHY NE");
        }

        List<Link> output = new LinkedList<>();
        Set<Link> rst = new HashSet<>();
        List<Link> phyNtLinks = getPhyLinks(topologyRef, nodeRef);
        SiteLink siteLink = new SiteLink(netconfTopology);
        for (Link link : phyNtLinks) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLink = link
                    .getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
            if (phyLink.getPhysical().getSupportedLink() != null) {
                for (SupportedLink sl : phyLink.getPhysical().getSupportedLink()) {
                    Link client = netconfTopology.getLink(sl.getTopologyRef(), sl.getLinkRef());

                    if (client != null && client.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                            != null) {
                        rst.add(client);
                    } else if (client != null && client.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                            != null) {
                        rst.addAll(siteLink.getOchLinks(client));
                    }
                }
            }

        }
        output.addAll(rst);
        return output;
    }


    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all Tunnels which passthrogh the PHY NE. with topo:{}, node: {}",
                topologyRef.getValue(), nodeRef.getValue());

        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            throw new Exception("cannnot find required PHY NE");
        }

        List<Tunnel> output = new LinkedList<>();
        Set<Tunnel> rst = new HashSet<>();
        List<Link> phyNtLinks = getPhyLinks(topologyRef, nodeRef);

        for (Link link : phyNtLinks) {
            rst.addAll(new PhyLink(netconfTopology).getTunnels(link));
        }
        output.addAll(rst);
        return output;
    }


    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all SITE Rackbased on PHY NODE. with topo:{}, node: {}",
                topologyRef.getValue(),
                nodeRef.getValue());

        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            throw new Exception(
                    "cannnot find required PHY Node");
        }

        List<Node> output = new LinkedList<>();

        String[] ids = nodeRef.getValue().split("#");
        ntNode = netconfTopology
                .getNode(new TopologyId(Constants.SITE_TOPO_KEY), new NodeId(ids[0]));
        if (ntNode == null) {
            log.error("cannot find a site node related phy node.");
        } else {
            ntNode = filterOutRack(ntNode, nodeRef);
            output.add(ntNode);
        }
        return output;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteNodes(
            TopologyId topologyRef, NodeId nodeRef) throws Exception {
        log.debug("start get all SITE Nodes based on PHY NODE. with topo:{}, node: {}",
                topologyRef.getValue(),
                nodeRef.getValue());

        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            throw new Exception(
                    "cannnot find required PHY Node");
        }

        List<Node> output = new LinkedList<>();

        String[] ids = nodeRef.getValue().split("#");
        ntNode = netconfTopology
                .getNode(new TopologyId(Constants.SITE_TOPO_KEY), new NodeId(ids[0]));
        if (ntNode == null) {
            log.error("cannot find a site node related phy node.");
        } else {
            output.add(ntNode);
        }
        return output;
    }

    /**
     * export the network node which only include one special ne in the rack
     *
     * @param ntSiteNode
     * @param phyNeId
     * @return
     */
    private Node filterOutRack(Node ntSiteNode, NodeId phyNeId) {
        boolean found = false;
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 siteNode = ntSiteNode
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        Iterator<SupportingRack> iter = siteNode.getSite().getSupportingRack().iterator();
        while (iter.hasNext()) {
            SupportingRack rack = iter.next();
            if (!found) {
                for (SupportingNe sn : rack.getSupportingNe()) {
                    if (sn.getNodeRef().getValue().equals(phyNeId.getValue())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    iter.remove();
                }
            } else {
                iter.remove();
            }
        }
        return ntSiteNode;
    }
}
