/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.nodes;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.PhyLink;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Topology1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.SourceTp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author: xinyzhao
 * @date: 2021/4/6
 */
@Slf4j
public class PhyTp extends AbstractTopoNode {


    public PhyTp(
            NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start get all PHY Node based on PHY TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        if (netconfTopology.getTerminationPoint(topologyRef, nodeRef, tpRef) == null) {
            throw new Exception("cannot find required TP.");
        }

        //find the tp related phyNe and then filter out related link.
        List<Node> output = new LinkedList<>();
        Node ntNode = netconfTopology.getNode(topologyRef, nodeRef);
        if (ntNode == null) {
            log.error("cannot find siteTP related phy node.");
        } else {
            output.add(ntNode);
        }
        return output;
    }

    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start export site links based on phyTP. node:{}, tp:{} ", nodeRef.getValue(),
                tpRef);

        PhyLink phyLink = new PhyLink(netconfTopology);
        List<Link> phyLinks = getPhyLinks(topologyRef, nodeRef, tpRef);
        Set<Link> rst = new HashSet<>();
        for (Link link : phyLinks) {
            if (link.getSource().getSourceTp().getValue().equals(tpRef.getValue()) ||
                    link.getDestination().getDestTp().getValue().equals(tpRef.getValue())) {
                rst.addAll(phyLink.getSiteLinks(topologyRef, link.getLinkId()));
            }
        }

        List<Link> output = new LinkedList<>();
        output.addAll(rst);
        return output;
    }


    @Override
    public List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug(
                "start get all OCH links which passthrogh the PHY TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        List<Link> phyLinks = getPhyLinks(topologyRef, nodeRef, tpRef);
        PhyLink phyLink = new PhyLink(netconfTopology);
        List<Link> output = new LinkedList<>();
        Set<Link> rst = new HashSet<>();
        for (Link link : phyLinks) {
            rst.addAll(phyLink.getOchLinks(topologyRef, link.getLinkId()));
        }
        output.addAll(rst);
        return output;
    }


    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.info("start to get all tunnel based on equipment tp {}", tpRef.getValue());
        TerminationPoint tp = netconfTopology
                .getTerminationPoint(topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());
        if (tp == null) {
            throw new Exception("cannot find the required tp");
        }
        List<Tunnel> output = new LinkedList<>();
        Set<Tunnel> rst = new HashSet<>();
        List<Link> links = null;
        if (tpRef.getValue().contains("MPO")) {
            links = getPhyLinks(topologyRef, nodeRef, tpRef);
            if (links == null || links.isEmpty()) {
                return new LinkedList<>();
            }

            String mpoPort = getMuxPanelPort(links);
            if (mpoPort == null) {
                return new LinkedList<>();
            }

            String[] ids = mpoPort.split("MPO");
            int mpoId = Integer.valueOf(ids[1]) - 1;
            links = new LinkedList<>();
            for (int i = 1; i <= 8; i++) {
                int mdId = i + mpoId * 8;
                TpId mdRef = new TpId(ids[0] + "M" + mdId + "D" + mdId);
                links.addAll(getPhyLinks(topologyRef, nodeRef, mdRef));
            }
        } else {
            links = getPhyLinks(topologyRef, nodeRef, tpRef);
        }

        PhyLink phyLink = new PhyLink(this.netconfTopology);
        for (Link link : links) {
            rst.addAll(phyLink.getTunnels(link));
        }
        if (rst.isEmpty()) {
            //the tp maybe edge tp
            TopologyId siteTopoId = new TopologyId(SITE_TOPO_KEY);
            Topology ntSiteTopo = netconfTopology.getTopology(siteTopoId);
            Topology1 siteTopo = ntSiteTopo.getAugmentation(Topology1.class);
            for (Tunnel tl : siteTopo.getTunnel()) {
                for (SourceTp stp : tl.getSourceTp()) {
                    if (stp.getTpRef().getValue().equals(tpRef.getValue())) {
                        rst.add(tl);
                        break;
                    }
                }
                for (DestinationTp dtp : tl.getDestinationTp()) {
                    if (dtp.getTpRef().getValue().equals(tpRef.getValue())) {
                        rst.add(tl);
                        break;
                    }
                }
            }
        }
        output.addAll(rst);
        return output;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteNodes(
            TopologyId topologyRef, NodeId nodeRef, TpId tpRef) throws Exception {
        log.debug("start get all Site Node based on PHY TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        if (netconfTopology.getTerminationPoint(topologyRef, nodeRef, tpRef) == null) {
            throw new Exception("cannot find required TP.");
        }

        return new PhyNe(netconfTopology).getSiteNodes(topologyRef, nodeRef);
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start get all Site Rack based on PHY TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        if (netconfTopology.getTerminationPoint(topologyRef, nodeRef, tpRef) == null) {
            throw new Exception("cannot find required TP.");
        }

        return new PhyNe(netconfTopology).getSiteRacks(topologyRef, nodeRef);
    }


    public List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start get all PHY link based on Phy TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        if (netconfTopology
                .getTerminationPoint(topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue())
                == null) {
            throw new Exception("cannot find required TP.");
        }

        //find the internal link of phyNe and then filter out based on phyTP.
        List<Link> output = new LinkedList<>();
        List<Link> phyLinks = new PhyNe(netconfTopology).getPhyLinks(topologyRef, nodeRef);
        for (Link link : phyLinks) {
            if (link.getSource().getSourceTp().getValue().equals(tpRef.getValue()) ||
                    link.getDestination().getDestTp().getValue().equals(tpRef.getValue())) {
                output.add(netconfTopology.getLink(topologyRef, link.getLinkId()));
            }
        }
        return output;
    }


    private String getMuxPanelPort(List<Link> links) throws Exception {
        TopologyId phyTopoID = new TopologyId(PHY_TOPO_KEY);
        InstanceIdentifier<Equipments> path;
        for (Link link : links) {
            String[] ids = link.getSource().getSourceTp().getValue().split("#");
            String eqipRef = ids[0] + "#" + ids[1] + "#" + ids[2];
            Equipments eq = netconfTopology
                    .getEquipment(phyTopoID, link.getSource().getSourceNode(), eqipRef);
            if (eq.getEquipType().equals(EquipType.MUXPANEL)) {
                return link.getSource().getSourceTp().getValue();
            }

            link.getDestination().getDestTp().getValue().split("#");
            eqipRef = ids[0] + "#" + ids[1] + "#" + ids[2];
            eq = netconfTopology
                    .getEquipment(phyTopoID, link.getDestination().getDestNode(), eqipRef);
            if (eq.getEquipType().equals(EquipType.MUXPANEL)) {
                return link.getDestination().getDestTp().getValue();
            }
        }
        return null;
    }
}
