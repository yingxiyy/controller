/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.nodes;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * @author: xinyzhao
 * @date: 2021/4/6
 */
@Slf4j
public class SiteTp extends AbstractTopoNode {

//    private PhyTp phyTp;


    public SiteTp(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.phyTp = new PhyTp(netconfTopology);

    }

    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start to get site tp under layer tunnel");

        if (netconfTopology.getTerminationPoint(topologyRef, nodeRef, tpRef) == null) {
            throw new Exception("cannot find required TP.");
        }

        return new PhyTp(netconfTopology)
                .getTunnels(new TopologyId(PHY_TOPO_KEY), getToPhyNodeId(tpRef),
                        tpRef);
    }


    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start get all Site Node based on SITE TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        if (netconfTopology.getTerminationPoint(topologyRef, nodeRef, tpRef) == null) {
            throw new Exception("cannot find required TP.");
        }

        return new PhyTp(netconfTopology)
                .getSiteNodes(new TopologyId(PHY_TOPO_KEY), nodeRef, tpRef);
    }

    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start get all PHY link based on SITE TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        return new PhyTp(netconfTopology)
                .getPhyLinks(new TopologyId(PHY_TOPO_KEY), getToPhyNodeId(tpRef),
                        tpRef);
    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start get all PHY Node based on SITE TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        if (netconfTopology.getTerminationPoint(topologyRef, nodeRef, tpRef) == null) {
            throw new Exception("cannot find required TP.");
        }

        return new PhyTp(netconfTopology)
                .getSiteNodes(new TopologyId(PHY_TOPO_KEY), nodeRef, tpRef);
    }

    @Override
    public List<Node> getSiteNodes(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start get all Site Node based on SITE TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        if (netconfTopology.getTerminationPoint(topologyRef, nodeRef, tpRef) == null) {
            throw new Exception("cannot find required TP.");
        }

        return new PhyTp(netconfTopology)
                .getSiteNodes(new TopologyId(PHY_TOPO_KEY), nodeRef, tpRef);
    }


    @Override
    public List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        log.debug("start get all OCH link based on SITE TP. with topo:{}, node:{}, tp: {}",
                topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());

        return (new PhyTp(netconfTopology)
                .getOchLinks(new TopologyId(PHY_TOPO_KEY), getToPhyNodeId(tpRef),
                        tpRef));
    }


    private NodeId getToPhyNodeId(TpId tpRef) {
        String[] ids = tpRef.getValue().split("#");
        return new NodeId(ids[0] + "#" + ids[1]);
    }
}
