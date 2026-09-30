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
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.nms.constructs.YangRouteConstructor;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

/**
 * @author: xinyzhao
 * @date: 2021/4/2
 */
@Slf4j
public class SiteLink extends AbstractTopoLink {


    public SiteLink(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public List<Route> getRoute(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        log.info("start to export the route for the topology id {},site link {}", topologyRef,
                linkRef);

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required link.");
        }

        Link1 siteLink = ntLink.getAugmentation(Link1.class);

        return siteLink.getSite().getExplictRoute().getRoute();
    }

    @Override
    public RouteInfoDto getRouteInfo(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        log.info("start to export the route for the topology id {},site link {}", topologyRef,
                linkRef);

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required link.");
        }

        Link1 siteLink = ntLink.getAugmentation(Link1.class);
        String source = PhysicalNodeIdNamingRule.getSiteId(
                ntLink.getSource().getSourceNode().getValue());
        String destination = PhysicalNodeIdNamingRule.getSiteId(
                ntLink.getDestination().getDestNode().getValue());
        return RouteInfoDto.builder()
                .source(source)
                .destination(destination)
                .routes(getRealRoute(siteLink.getSite().getExplictRoute().getRoute())).LinkType(
                        Constants.SITE_LINK).build();
    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all PHY NE based on SITE Link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        List<Link> phyLinks = getPhyLinks(topologyRef, linkRef);
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
    public List<Node> getSiteNodes(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all Site NE based on SITE Link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception(
                    "cannot find required SITE link: " + linkRef.getValue());
        }

        Link1 siteLink = ntLink.getAugmentation(Link1.class);
        YangRouteConstructor route = new YangRouteConstructor(netconfTopology);
        return route.getAllSites(siteLink.getSite().getExplictRoute());
    }


    @Override
    public List<Link> getOchLinks(TopologyId phyTopoId, LinkId linkId) throws Exception {
        Link link = netconfTopology.getLink(phyTopoId, linkId);
        return getOchLinks(link);
    }

    @Override
    public List<Link> getOchLinks(Link ntLink) {
        List<Link> output = new LinkedList<>();

        Link1 siteLink = ntLink.getAugmentation(Link1.class);
        if (siteLink.getSite().getSupportedLink() != null) {
            for (SupportedLink sl : siteLink.getSite().getSupportedLink()) {
                Link link = netconfTopology.getLink(sl.getTopologyRef(), sl.getLinkRef());
                if (link == null) {
                    log.error(
                            "find a SITE link's {}, {} supported OCH link, doesn't included in OCH topo.",
                            sl.getTopologyRef(), sl.getLinkRef());
                } else {
                    output.add(link);
                }
            }
        }
        return output;
    }

    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("get site-link itself. with topo:{}, link:{}", topologyRef.getValue(),
                linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required link.");
        }

        List<Link> output = new LinkedList<>();
        output.add(ntLink);
        return output;
    }


    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start to get all tunnels underlay the site link,with toop:{},link:{}",
                topologyRef.getValue(), linkRef.getValue());
        Link link = netconfTopology.getLink(topologyRef, linkRef);
        if (link == null) {
            throw new Exception("cannot find required link underlay tunnels");
        }
        List<Tunnel> output = new LinkedList<>();
        OchLink ochLink = new OchLink(this.netconfTopology);
        Link1 link1 = link.getAugmentation(Link1.class);

        if (link1.getSite().getSupportedLink() != null) {
            for (SupportedLink supportedLink : link1.getSite().getSupportedLink()) {
                output.addAll(ochLink.getTunnels(supportedLink.getTopologyRef(),
                        supportedLink.getLinkRef()));
            }
        }
        return output;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all Site Rack based on SITE Link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception(
                    "cannot find required SITE link: " + linkRef.getValue());
        }

        List<Node> output = new LinkedList<>();
        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        Set<Node> rst = new HashSet<>();

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink sl : ntLink
                .getSupportingLink()) {
            rst.addAll(new PhyLink(netconfTopology).getSiteRacks(phyTopoId, sl.getLinkRef()));
        }
        output.addAll(rst);
        return output;
    }


    @Override
    public List<Link> getPhyLinks(Link ntLink) {
        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);

        List<Link> output = new LinkedList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink sl : ntLink
                .getSupportingLink()) {
            Link link = netconfTopology.getLink(phyTopoId, sl.getLinkRef());
            if (link == null && link.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                    != null) {
                log.error("find a Site-link's supporting link, which cannot find in phy topo.");
            } else {
                output.add(link);
            }
        }

        return output;
    }

    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all PHY link based on SITE Link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception(
                    "cannot find required SITE link: " + linkRef.getValue());
        }

        return getPhyLinks(ntLink);
    }

}
