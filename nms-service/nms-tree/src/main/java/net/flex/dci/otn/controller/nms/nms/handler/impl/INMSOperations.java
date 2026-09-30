/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl;

import java.util.List;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.nms.handler.IBaseNMSOperations;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.Schedule;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

/**
 * @date: 2021/4/6
 */
public interface INMSOperations extends IBaseNMSOperations {


    /**
     * get tunnel
     *
     * @param topologyRef
     * @param nodeRef
     * @param rackRef
     * @param equipRef
     * @param tpRef
     * @param linkRef
     * @param tunnelRef
     * @return
     */
    default List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef, String rackRef,
            String equipRef, TpId tpRef,
            LinkId linkRef, String tunnelRef) {
        return null;
    }

    default List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef) throws Exception {
        return null;
    }


    default List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        return null;
    }

    default List<Tunnel> getTunnels(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        return null;
    }


    default List<Tunnel> getTunnels(TopologyId topologyRef, String tunnelRef) throws Exception {
        return null;
    }

    default List<Tunnel> getTunnels(TopologyId topologyRef, LinkId linkRef) throws Exception {
        return null;
    }

    default List<Tunnel> getTunnels(Link linkRef) throws Exception {
        return null;
    }

    default List<Route> getRoute(TopologyId topologyRef, LinkId linkRef) throws Exception {
        return null;
    }


    default List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, NodeId nodeRef) throws Exception {
        return null;
    }


    default List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        return null;
    }

    default List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        return null;
    }


    default List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, String tunnelRef) throws Exception {
        return null;
    }

    default List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, LinkId linkRef) throws Exception {
        return null;
    }

    default List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteNodes(
            TopologyId topologyRef, NodeId nodeRef) throws Exception {
        return null;
    }

    default List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteNodes(
            TopologyId topologyId, NodeId nodeRef, TpId tpRef) throws Exception {
        return null;
    }

    default List<Link> getPhyLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        return null;
    }

    default List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeId) throws Exception {
        return null;
    }

    default List<Link> getPhyLinks(Link link) {
        return null;
    }


    default List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        return null;
    }

    default List<Link> getPhyLinks(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        return null;
    }

    default List<Route> getRoute(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        return null;
    }

    default RouteInfoDto getRouteInfo(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        return null;
    }


    default List<Link> getSiteLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        return null;
    }

    default List<Link> getSiteLinks(TopologyId topologyRef, NodeId nodeId) throws Exception {
        return null;
    }

    default List<Link> getPhyLinks(TopologyId topologyRef, String tunnelRef) throws Exception {
        return null;
    }

    default List<Link> getSiteLinks(Link link) {
        return null;
    }


    default List<Link> getSiteLinks(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        return null;
    }

    default List<Link> getSiteLinks(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        return null;
    }

    default List<Link> getSiteLinks(TopologyId topologyRef, String tunnelRef) throws Exception {
        return null;
    }


    default List<Link> getOchLinks(TopologyId phyTopoId, LinkId linkId) throws Exception {
        return null;
    }


    default List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeId) throws Exception {
        return null;
    }

    default List<Link> getOchLinks(Link link) {
        return null;
    }


    default List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        return null;
    }

    default List<Link> getOchLinks(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        return null;
    }

    default List<Link> getOchLinks(TopologyId topologyRef, String tunnelRef) throws Exception {
        return null;
    }


    default List<Node> getPhyNodes(TopologyId phyTopoId, LinkId linkId) throws Exception {
        return null;
    }


    default List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeId) throws Exception {
        return null;
    }

    default List<Node> getPhyNodes(Link link) {
        return null;
    }


    default List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
        return null;
    }

    default List<Node> getPhyNodes(TopologyId topologyRef, NodeId nodeRef, TpId tpRef)
            throws Exception {
        return null;
    }

    default List<Node> getPhyNodes(TopologyId topologyRef, String tunnelRef) throws Exception {
        return null;
    }

    default List<Node> getSiteNodes(TopologyId topologyRef, NodeId nodeRef, String equipRef)
            throws Exception {
        return null;
    }

    default List<Node> getSiteNodes(TopologyId topologyRef, LinkId linkRef) throws Exception {
        return null;
    }

    default List<Node> getSiteNodes(TopologyId topologyRef, String tunnelRef) throws Exception {
        return null;
    }

    default List<Schedule> getSchedules() {
        return null;
    }


}
