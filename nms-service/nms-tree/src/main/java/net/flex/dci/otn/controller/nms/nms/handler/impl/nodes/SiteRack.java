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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
 * @date: 2021/4/6
 */
@Slf4j
public class SiteRack extends AbstractTopoNode {

    public SiteRack(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        log.debug("start export PHY node which included in rack. topo:{}, node:{}, rack:{} ",
                topologyRef.getValue(), nodeRef.getValue(), rackRef);

        SupportingRack rack = netconfTopology.getRack(topologyRef, nodeRef, rackRef);
        if (rack == null) {
            throw new Exception("cannnot find required rack");
        }

        List<Node> output = new LinkedList<>();
        PhyNe phyNe = new PhyNe(netconfTopology);
        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        for (SupportingNe sn : rack.getSupportingNe()) {
            output.addAll(phyNe.getPhyNodes(phyTopoId, sn.getNodeRef()));
        }
        return output;
    }


    @Override
    public List<Node> getSiteNodes(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        log.debug("start export SITE node which contains the rack. topo:{}, node:{}, rack:{} ",
                topologyRef.getValue(), nodeRef.getValue(), rackRef);

        SupportingRack rack = netconfTopology.getRack(topologyRef, nodeRef, rackRef);
        if (rack == null) {
            throw new Exception("cannnot find required rack");
        }

        return new SiteNode(netconfTopology).getSiteNodes(topologyRef, nodeRef);
    }


    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        log.debug(
                "start get all Tunnels which passthrogh the site RACK. topo:{}, node:{}, rack:{} ",
                topologyRef.getValue(), nodeRef.getValue(), rackRef);

        SupportingRack rack = netconfTopology.getRack(topologyRef, nodeRef, rackRef);
        if (rack == null) {
            throw new Exception("cannnot find required SITE RACK");
        }

        List<Tunnel> output = new LinkedList<>();
        Set<Tunnel> rst = new HashSet<>();

        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        for (SupportingNe sn : rack.getSupportingNe()) {
            rst.addAll(new PhyNe(netconfTopology).getTunnels(phyTopoId, sn.getNodeRef()));
        }

        output.addAll(rst);
        return output;
    }


    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks
            (
                    TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        log.debug("start export the Rack itself. topo:{}, node:{}, rack:{} ",
                topologyRef.getValue(), nodeRef.getValue(), rackRef);

        SupportingRack rack = netconfTopology.getRack(topologyRef, nodeRef, rackRef);
        if (rack == null) {
            throw new Exception("cannnot find required rack");
        }

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> output = new LinkedList<>();

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node ntNode = netconfTopology
                .getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            log.error("cannot find a site node related phy node.");
        } else {
            ntNode = filterOutRack(ntNode, rackRef);
            output.add(ntNode);
        }
        return output;
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node filterOutRack
            (
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node
                            ntSiteNode,
                    String rackRef) {
        Node1 siteNode = ntSiteNode.getAugmentation(Node1.class);
        Iterator<SupportingRack> iter = siteNode.getSite().getSupportingRack().iterator();
        boolean found = false;
        while (iter.hasNext()) {
            SupportingRack rack = iter.next();
            if (!found) {
                if (rack.getRackId().getValue().equals(rackRef)) {
                    found = true;
                    break;
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

    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        log.debug("start export rack included PHY Link. topo:{}, node:{}, rack:{} ",
                topologyRef.getValue(), nodeRef.getValue(), rackRef);

        SupportingRack rack = netconfTopology.getRack(topologyRef, nodeRef, rackRef);
        if (rack == null) {
            throw new Exception("cannnot find required rack");
        }

        List<Link> output = new LinkedList<>();
        Set<Link> rst = new HashSet<>();
        PhyNe phyNe = new PhyNe(netconfTopology);
        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        for (SupportingNe sn : rack.getSupportingNe()) {
            rst.addAll(phyNe.getPhyLinks(phyTopoId, sn.getNodeRef()));
        }

        output.addAll(rst);
        return output;
    }


    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, NodeId nodeId, String rackRef)
            throws Exception {
        log.debug("start export rack included nes. topo:{}, node:{}, rack:{} ",
                topologyRef.getValue(), nodeId.getValue(), rackRef);

        SupportingRack rack = netconfTopology.getRack(topologyRef, nodeId, rackRef);
        if (rack == null) {
            throw new Exception("cannnot find required rack");
        }

        //get rack supporting nes. and merge all ne supported site link

        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        PhyNe phyNe = new PhyNe(netconfTopology);
        Set<Link> rst = new HashSet<>();
        for (SupportingNe sn : rack.getSupportingNe()) {
            rst.addAll(phyNe.getSiteLinks(phyTopoId, sn.getNodeRef()));
        }

        List<Link> output = new LinkedList<>();
        output.addAll(rst);
        return output;
    }

    @Override
    public List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        log.debug(
                "start get all OCH link which passthrogh the site RACK. topo:{}, node:{}, rack:{} ",
                topologyRef.getValue(), nodeRef.getValue(), rackRef);

        List<Link> phyLinks = getPhyLinks(topologyRef, nodeRef, rackRef);
        PhyLink phyLink = new PhyLink(netconfTopology);
        List<Link> output = new LinkedList<>();
        Set<Link> rst = new HashSet<>();
        for (Link link : phyLinks) {
            rst.addAll(phyLink.getOchLinks(topologyRef, link.getLinkId()));
        }
        output.addAll(rst);
        return output;
    }

}
