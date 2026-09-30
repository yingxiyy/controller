/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.OchLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.PhyLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteTunnel;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyEquipment;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyNe;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyTp;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteNode;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteRack;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteTp;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetConfConvertors;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.PagedList;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/8
 */
@Slf4j
@Component
public class RackHandler extends AbstractBaseHandler {

    public RackHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public PagedList getRackPaged(GetRackPagedInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        List<Node> nodes = this
                .getRacks(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
        PagedList pagedList = new PagedList(nodes);
        pagedList.setFilter(input.getFilter());
        pagedList.sort(input.getSortInfos());
        return pagedList;
    }

    @Override
    public List<Node> getRacks(GetRackInput input) throws Exception {

        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        return getRacks(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }

    private List<Node> getRacks(TopologyId topologyRef, NodeId nodeRef, String rackRef,
            String equipRef, TpId tpRef,
            LinkId linkRef, String tunnelRef) throws Exception {
        log.debug(
                "start get all Rack. topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
                topologyRef == null ? "null" : topologyRef.getValue(),
                nodeRef == null ? "null" : nodeRef.getValue(),
                rackRef == null ? "null" : rackRef, equipRef == null ? "null" : equipRef,
                tpRef == null ? "null" : tpRef.getValue(),
                linkRef == null ? "null" : linkRef.getValue(),
                tunnelRef == null ? "null" : tunnelRef);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> ntNodes = null;
        if (topologyRef == null) {
            throw new Exception("topologyRef is mandatory.");
        } else if (topologyRef.getValue().contains(Constants.SITE_TOPO_KEY)
                && nodeRef == null && rackRef == null
                && equipRef == null && tpRef == null && linkRef == null && tunnelRef == null) {
            Topology topo = this.netconfTopology.getTopology(topologyRef);
            if (topo == null) {
                throw new Exception(
                        "cannot find required Topology.");
            }
            ntNodes = topo.getNode();
        } else if (topologyRef != null && nodeRef != null && rackRef == null && equipRef == null
                && tpRef == null) {
            if (topologyRef.getValue().contains(Constants.SITE_TOPO_KEY)) {
                ntNodes = new SiteNode(netconfTopology).getSiteNodes(topologyRef, nodeRef);
            } else if (topologyRef.getValue().contains(Constants.PHY_TOPO_KEY)) {
                ntNodes = new PhyNe(netconfTopology).getSiteRacks(topologyRef, nodeRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(Constants.SITE_TOPO_KEY)
                && nodeRef != null && rackRef != null) {
            ntNodes = new SiteRack(netconfTopology).getSiteRacks(topologyRef, nodeRef, rackRef);
        } else if (topologyRef.getValue().contains(Constants.PHY_TOPO_KEY)
                && nodeRef != null && rackRef == null
                && equipRef != null) {
            ntNodes = new PhyEquipment(netconfTopology)
                    .getSiteRacks(topologyRef, nodeRef, equipRef);
        } else if (topologyRef != null && nodeRef != null && rackRef == null && tpRef != null) {
            if (topologyRef.getValue().equals(Constants.SITE_TOPO_KEY)) {
                ntNodes = new SiteTp(netconfTopology).getSiteRacks(topologyRef, nodeRef, tpRef);
            } else if (topologyRef.getValue().equals(Constants.PHY_TOPO_KEY)) {
                ntNodes = new PhyTp(netconfTopology).getSiteRacks(topologyRef, nodeRef, tpRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef != null && nodeRef == null && rackRef == null && tpRef == null
                && linkRef != null) {
            if (topologyRef.getValue().contains(Constants.SITE_TOPO_KEY)) {
                ntNodes = new SiteLink(netconfTopology).getSiteRacks(topologyRef, linkRef);
            } else if (topologyRef.getValue().contains(Constants.PHY_TOPO_KEY)) {
                ntNodes = new PhyLink(netconfTopology).getSiteRacks(topologyRef, linkRef);
            } else if (topologyRef.getValue().contains(Constants.OCH_TOPO_KEY)) {
                ntNodes = new OchLink(netconfTopology).getSiteRacks(topologyRef, linkRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(Constants.SITE_TOPO_KEY)
                && nodeRef == null && rackRef == null
                && tpRef == null && linkRef == null && tunnelRef != null) {
            ntNodes = new SiteTunnel(netconfTopology).getSiteRacks(topologyRef, tunnelRef);
        } else {
            throw new Exception(
                    "not supported parameter compose.");
        }

        return NetConfConvertors.convertNtNode2RackOutputNode(ntNodes);
    }


}
