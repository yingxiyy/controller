/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.nodes;

import java.util.HashSet;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * @author: xinyzhao
 * @date: 2021/4/6
 */
@Slf4j
public class PhyEquipment extends AbstractTopoNode {

//    private PhyNe phyNe;
//    private PhyLink phyLink;

    public PhyEquipment(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        phyNe = new PhyNe(netconfTopology);
//        phyLink = new PhyLink(netconfTopology);
    }

    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef) {
        return null;
    }

    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef, String equipRef)
            throws Exception {
        log.debug(
                "start get all Tunnels which passthrogh the equipment. with topo:{}, node:{}, equip: {}",
                topologyRef.getValue(), nodeRef.getValue(), equipRef);

        if (netconfTopology.getEquipment(topologyRef, nodeRef, equipRef) == null) {
            throw new Exception(
                    "cannot find required equipment.");
        }

        List<Tunnel> output = new LinkedList<>();
        Set<Tunnel> rst = new HashSet<>();
        List<Link> links = getPhyLinks(topologyRef, nodeRef, equipRef);
        for (Link link : links) {
            rst.addAll(new PhyLink(netconfTopology).getTunnels(link));
        }
        output.addAll(rst);
        return output;
    }

    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeRef, String equipRef)
            throws Exception {
        log.debug("start get all PHY link based on Equipment. with topo:{}, node:{}, equip: {}",
                topologyRef.getValue(), nodeRef.getValue(), equipRef);

        if (netconfTopology.getEquipment(topologyRef, nodeRef, equipRef) == null) {
            throw new Exception(
                    "cannot find required equipment.");
        }

        //find the internal link of phyNe and then filter out based on equip.
        List<Link> output = new LinkedList<>();
        List<Link> phyLinks = new PhyNe(netconfTopology).getPhyLinks(topologyRef, nodeRef);
        for (Link link : phyLinks) {
            if (link.getSource().getSourceTp().getValue().startsWith(equipRef) ||
                    link.getDestination().getDestTp().getValue().startsWith(equipRef)) {
                output.add(netconfTopology.getLink(topologyRef, link.getLinkId()));
            }
        }
        return output;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, NodeId nodeRef, String equipRef)
            throws Exception {
        log.debug("start get all SITE rack based on Equipment. with topo:{}, node:{}, equip: {}",
                topologyRef.getValue(), nodeRef.getValue(), equipRef);

        if (netconfTopology.getEquipment(topologyRef, nodeRef, equipRef) == null) {
            throw new Exception("cannot find required equipment.");
        }

        return new PhyNe(netconfTopology).getSiteRacks(topologyRef, nodeRef);
    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeRef, String equipRef)
            throws Exception {
        log.debug("start get all PHY nodes based on Equipment. with topo:{}, node:{}, equip: {}",
                topologyRef.getValue(), nodeRef.getValue(), equipRef);

        if (netconfTopology.getEquipment(topologyRef, nodeRef, equipRef) == null) {
            throw new Exception(
                    "cannot find required equipment.");
        }

        return new PhyNe(netconfTopology).getSiteNodes(topologyRef, nodeRef);
    }


    @Override
    public List<Node> getSiteNodes(TopologyId topologyRef, NodeId nodeRef, String equipRef)
            throws Exception {
        log.debug(
                "start get all SITE nodes based on Equipment. with topo:{}, node:{}, equip: {}",
                topologyRef.getValue(), nodeRef.getValue(), equipRef);

        if (netconfTopology.getEquipment(topologyRef, nodeRef, equipRef) == null) {
            throw new Exception(
                    "cannot find required equipment.");
        }

        return new PhyNe(netconfTopology).getSiteNodes(topologyRef, nodeRef);
    }


    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, NodeId nodeRef, String equipRef)
            throws Exception {
        log.debug("start get all Site link based on Equipment. topo:{}, node:{}, equip:{} ",
                topologyRef.getValue(), nodeRef.getValue(), equipRef);

        PhyLink phyLink = new PhyLink(netconfTopology);
        List<Link> phyLinks = getPhyLinks(topologyRef, nodeRef, equipRef);
        Set<Link> rst = new HashSet<>();
        for (Link link : phyLinks) {
            if (link.getSource().getSourceTp().getValue().startsWith(equipRef) ||
                    link.getDestination().getDestTp().getValue().startsWith(equipRef)) {
                rst.addAll(phyLink.getSiteLinks(topologyRef, link.getLinkId()));
            }
        }

        List<Link> output = new LinkedList<>();
        output.addAll(rst);
        return output;
    }

    @Override
    public List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeRef, String equipRef)
            throws Exception {
        log.debug("start get all OCH link based on Equipment. topo:{}, node:{}, equip:{} ",
                topologyRef.getValue(), nodeRef.getValue(), equipRef);

        List<Link> phyLinks = getPhyLinks(topologyRef, nodeRef, equipRef);
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
