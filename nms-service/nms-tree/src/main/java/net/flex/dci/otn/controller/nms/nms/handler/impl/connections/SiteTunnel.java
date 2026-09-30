/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.connections;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.constructs.YangRouteConstructor;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

/**
 * @author: xinyzhao
 * @date: 2021/4/2
 */
@Slf4j
public class SiteTunnel extends AbstractTopoLink {

//    private PhyLink phyLink;

//    private OchLink ochLink;

    public SiteTunnel(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public List<Route> getRoute(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        log.info("export the topologyTunnel route with topo:{},topologyTunnel:{}",
                topologyRef.getValue(), tunnelRef.getValue());
        Tunnel tunnel = netconfTopology.getTunnel(tunnelRef);
        if (tunnel == null) {
            throw new Exception("there have no route for the tunnel");
        }
        tunnel.getSourceTp();

        tunnel.getDestinationTp();
        return tunnel.getExplictRoute().getRoute();
    }

    @Override
    public RouteInfoDto getRouteInfo(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        log.info("export the topologyTunnel route with topo:{},topologyTunnel:{}",
                topologyRef.getValue(), tunnelRef.getValue());
        Tunnel tunnel = netconfTopology.getTunnel(tunnelRef);
        if (tunnel == null) {
            throw new Exception("there have no route for the tunnel");
        }
        String sourceSite = PhysicalTpIdNamingRule.getSiteId(
                tunnel.getSourceTp().get(0).getTpRef().getValue());
        String destSite = PhysicalTpIdNamingRule.getSiteId(
                tunnel.getDestinationTp().get(0).getTpRef().getValue());

        return RouteInfoDto.builder()
                .source(sourceSite)
                .destination(destSite)
                .routes(getRealRoute(tunnel.getExplictRoute().getRoute()))
                .LinkType(Constants.SITE_TUNNEL)
                .build();
    }

    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, String tunnelRef) throws Exception {
        log.debug("export the Tunnel itself. with topo:{}, tunnel:{}", topologyRef.getValue(),
                tunnelRef);
        Tunnel tunnel = netconfTopology.getTunnel(new Uri(tunnelRef));
        if (tunnel == null) {
            throw new Exception("cannot find required tunnel.");
        }

        List<Tunnel> output = new LinkedList<>();
        output.add(tunnel);
        return output;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, String tunnelRef) throws Exception {
        log.debug("start get all SITE Rack based on Tunnel. with topo:{}, tunnel:{}",
                topologyRef.getValue(), tunnelRef);
        Tunnel tunnel = netconfTopology.getTunnel(new Uri(tunnelRef));
        if (tunnel == null) {
            throw new Exception(
                    "cannot find required Tunnel link: " + tunnelRef);
        }

        List<Link> linkList = getPhyLinks(topologyRef, tunnelRef);

        List<Node> output = new LinkedList<>();
        Set<Node> rst = new HashSet<>();

        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        for (Link link : linkList) {
            rst.addAll(new PhyLink(netconfTopology).getSiteRacks(phyTopoId, link.getLinkId()));
        }
        output.addAll(rst);
        return output;
    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, String tunnelRef) throws Exception {
        log.debug("start get all physical node based on Tunnel. with topology:{}, tunnel:{}",
                topologyRef.getValue(), tunnelRef);

        List<Link> phyLinks = getPhyLinks(topologyRef, tunnelRef);

        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        List<Node> output = new LinkedList<>();
        Set<Node> rst = new HashSet<>();
        PhyLink phyLink = new PhyLink(netconfTopology);
        for (Link link : phyLinks) {
            rst.addAll(phyLink.getPhyNodes(phyTopoId, link.getLinkId()));
        }
        output.addAll(rst);
        return output;
    }


    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, String tunnelRef) throws Exception {
        log.debug(
                "start get all PHY link, which is supporting to this tunnel. topo:{}, tunnel:{}",
                topologyRef.getValue(), tunnelRef);

        Tunnel tunnel = netconfTopology.getTunnel(new Uri(tunnelRef));
        if (tunnel == null) {
            throw new Exception(
                    "cannot find required Tunnel link: " + tunnelRef);
        }

        List<Link> output = new LinkedList<>();
        Set<Link> rst = new HashSet<>();
        for (SupportingLink sl : tunnel.getSupportingLink()) {
            //Tunnels's supporting-link is och-link
            rst.addAll(
                    new OchLink(netconfTopology).getPhyLinks(sl.getTopologyRef(), sl.getLinkRef()));
        }
        output.addAll(rst);
        return output;
    }

    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, String tunnelRef) throws Exception {
        log.debug(
                "start get all site link, which is supporting to this tunnel. topo:{}, tunnel:{}",
                topologyRef.getValue(), tunnelRef);

        Tunnel tunnel = netconfTopology.getTunnel(new Uri(tunnelRef));
        if (tunnel == null) {
            throw new Exception(
                    "cannot find required Tunnel.");
        }

        Set<Link> rst = new HashSet<>();
        OchLink ochLink = new OchLink(netconfTopology);
        for (SupportingLink sl : tunnel.getSupportingLink()) {
            //tunnel's supporting-link is och-link
            rst.addAll(ochLink.getSiteLinks(sl.getTopologyRef(), sl.getLinkRef()));
        }

        List<Link> output = new LinkedList<>();
        output.addAll(rst);
        return output;
    }

    @Override
    public List<Link> getOchLinks(TopologyId topologyRef, String tunnelRef) throws Exception {
        log.debug(
                "start get all Och link, which is supporting to this tunnel. topo:{}, tunnel:{}",
                topologyRef.getValue(), tunnelRef);

        Tunnel tunnel = netconfTopology.getTunnel(new Uri(tunnelRef));
        if (tunnel == null) {
            throw new Exception(
                    "cannot find required Tunnel.");
        }

        List<Link> output = new LinkedList<>();
        for (SupportingLink sl : tunnel.getSupportingLink()) {
            //tunnel's supporting-link is och-link
            output.add(netconfTopology.getLink(sl.getTopologyRef(), sl.getLinkRef()));
        }

        return output;
    }

    @Override
    public List<Node> getSiteNodes(TopologyId topologyRef, String tunnelRef) throws Exception {
        log.debug("start get all SITE NE based on Tunnel. with topo:{}, tunnel:{}",
                topologyRef.getValue(), tunnelRef);
        Tunnel tunnel = netconfTopology.getTunnel(new Uri(tunnelRef));
        if (tunnel == null) {
            throw new Exception(
                    "cannot find required Tunnel link: " + tunnelRef);
        }

        YangRouteConstructor route = new YangRouteConstructor(netconfTopology);
        return route.getAllSites(tunnel.getExplictRoute());
    }

}
