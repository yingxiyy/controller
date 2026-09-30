/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.connections;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyNe;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.RouteUsageInclude;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.TpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHopBuilder;

/**
 * @date: 2021/4/2
 */
@Slf4j
public class PhyLink extends AbstractTopoLink {


    public PhyLink(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }


    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all PHY NE based on PHY Link. with topo:{}, link:{}",
                topologyRef.getValue(),
                linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception(
                    "cannot find required PHY link: " + linkRef.getValue());
        }

        List<Node> output = new LinkedList<>();
        PhyNe phyNe = new PhyNe(netconfTopology);
        output.addAll(phyNe.getPhyNodes(topologyRef, ntLink.getSource().getSourceNode()));
        output.addAll(phyNe.getPhyNodes(topologyRef, ntLink.getDestination().getDestNode()));

        return output;
    }

    @Override
    public List<Node> getSiteNodes(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all SITE NE based on PHY Link. with topo:{}, link:{}",
                topologyRef.getValue(),
                linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception(
                    "cannot find required PHY link: " + linkRef.getValue());
        }

        List<Node> output = new LinkedList<>();
        PhyNe phyNe = new PhyNe(netconfTopology);
        output.addAll(phyNe.getSiteNodes(topologyRef, ntLink.getSource().getSourceNode()));
        output.addAll(phyNe.getSiteNodes(topologyRef, ntLink.getDestination().getDestNode()));

        return output;
    }


    @Override
    public List<Route> getRoute(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        log.debug("export the phy link route. with topo:{}, link:{}", topologyRef.getValue(),
                linkRef.getValue());
        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required link.");
        }
        return prepareRoute(ntLink);
    }

    @Override
    public RouteInfoDto getRouteInfo(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        log.debug("export the phy link route. with topo:{}, link:{}", topologyRef.getValue(),
                linkRef.getValue());
        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required link.");
        }
        String source = PhysicalNodeIdNamingRule.getSiteId(
                ntLink.getSource().getSourceNode().getValue());
        String destination = PhysicalNodeIdNamingRule.getSiteId(
                ntLink.getSource().getSourceNode().getValue());
        return RouteInfoDto.builder().routes(prepareRoute(ntLink)).source(source)
                .destination(destination).LinkType(Constants.PHY_LINK).build();
    }

    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all link based on phy ne,topo:{},link:{}", topologyRef.getValue(),
                linkRef.getValue());
        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required phy link based on " + linkRef.getValue());
        }
        return getTunnels(ntLink);
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all SITE NE based on PHY Link. with topo:{}, link:{}",
                topologyRef.getValue(),
                linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception(
                    "cannot find required PHY link: " + linkRef.getValue());
        }

        List<Node> output = new LinkedList<>();

        output.addAll(new PhyNe(this.netconfTopology)
                .getSiteNodes(topologyRef, ntLink.getSource().getSourceNode()));
        output.addAll(new PhyNe(this.netconfTopology)
                .getSiteNodes(topologyRef, ntLink.getDestination().getDestNode()));

        return output;
    }

    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("export PHY Link itself. with topo:{}, linkId: {}", topologyRef.getValue(),
                linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required Link.");
        }

        List<Link> output = new LinkedList<>();
        output.add(ntLink);
        return output;
    }

    /**
     * extracted tunnel based on physical link
     *
     * @param ntLink
     * @return
     */
    @Override
    public List<Tunnel> getTunnels(Link ntLink) throws Exception {
        List<Tunnel> output = new LinkedList<>();
        Set<Tunnel> tunnelSet = new HashSet<>();
        Link1 link1 = ntLink.getAugmentation(Link1.class);

        if (link1.getPhysical().getSupportedLink() != null) {
            // not all phy link has supported link (och or site link)
            for (SupportedLink sl : link1.getPhysical().getSupportedLink()) {
                if (sl.getTopologyRef().getValue().equals(SITE_TOPO_KEY)) {
                    tunnelSet.addAll(new SiteLink(this.netconfTopology)
                            .getTunnels(sl.getTopologyRef(), sl.getLinkRef()));
                } else if (sl.getTopologyRef().getValue().equals(OCH_TOPO_KEY)) {
                    tunnelSet.addAll(new OchLink(this.netconfTopology)
                            .getTunnels(sl.getTopologyRef(), sl.getLinkRef()));
                }
            }
            output.addAll(tunnelSet);
        }
        return output;
    }

    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all site link based on PHY Link. with topo:{}, link: {}",
                topologyRef.getValue(),
                linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required Link.");
        }

        List<Link> output = new LinkedList<>();
        Link1 phyLink = ntLink.getAugmentation(Link1.class);
        if (phyLink.getPhysical().getSupportedLink() != null) {
            for (SupportedLink sl : phyLink.getPhysical().getSupportedLink()) {
                if (sl.getTopologyRef().getValue().equals(SITE_TOPO_KEY)) {
                    ntLink = netconfTopology.getLink(sl.getTopologyRef(), sl.getLinkRef());
                    if (ntLink == null) {
                        throw new Exception(
                                "phyLink's supported-link is siteLink, but cannot find it.");
                    }
                    output.add(ntLink);
                }
            }
        }

        return output;
    }

    @Override
    public List<Link> getOchLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug(
                "start get all OCH links which passthrough the PHY Link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required link.");
        }

        List<Link> output = new LinkedList<>();
        Set<Link> rst = new HashSet<>();
        Link1 link1 = ntLink.getAugmentation(Link1.class);
        SiteLink siteLink = new SiteLink(netconfTopology);
        if (link1.getPhysical().getSupportedLink() != null) {
            for (SupportedLink sl : link1.getPhysical().getSupportedLink()) {
                if (sl.getTopologyRef().getValue().equals(SITE_TOPO_KEY)) {
                    rst.addAll(siteLink.getOchLinks(sl.getTopologyRef(), sl.getLinkRef()));
                } else if (sl.getTopologyRef().getValue().equals(OCH_TOPO_KEY)) {
                    rst.add(netconfTopology.getLink(sl.getTopologyRef(), sl.getLinkRef()));
                }
            }
        }

        output.addAll(rst);
        return output;
    }

    private List<Route> prepareRoute(Link ntLink) {
        List<Route> routes = new LinkedList<>();
        RouteBuilder rb = new RouteBuilder();
        short index = 0;
        rb.setIndex(index);
        rb.setKey(new RouteKey(index));

        PrimaryBuilder pb = new PrimaryBuilder();
        pb.setExplicitRouteObjects(getEro(ntLink));
        rb.setPrimary(pb.build());

        routes.add(rb.build());
        return routes;
    }

    private List<ExplicitRouteObjects> getEro(Link ntLink) {
        List<ExplicitRouteObjects> eroList = new LinkedList<>();

        ExplicitRouteObjectsBuilder ero = new ExplicitRouteObjectsBuilder();
        ero.setExplicitRouteUsage(RouteUsageInclude.class);
        ero.setKey(new ExplicitRouteObjectsKey(RouteUsageInclude.class));
        ero.setPathRouteObject(getPro(ntLink));

        eroList.add(ero.build());
        return eroList;
    }

    private List<PathRouteObject> getPro(Link link) {
        List<PathRouteObject> proList = new LinkedList<>();
        Long index = 1L;

        proList.add(getTpPro(index, link.getSource().getSourceTp()));
        index++;
        proList.add(getLinkPro(index, link));
        index++;
        proList.add(getTpPro(index, link.getDestination().getDestTp()));

        return proList;
    }


    private PathRouteObject getTpPro(Long index, TpId tpId) {
        TopologyId phyTopoId = new TopologyId(Constants.PHY_TOPO_KEY);
        PathRouteObjectBuilder pb = new PathRouteObjectBuilder();

        String[] ids = tpId.getValue().split("#");
        String siteId = ids[0];
        String neId = ids[0] + "#" + ids[1];
        String equipId = ids[0] + "#" + ids[1] + "#" + ids[2];

        pb.setIndex(index);
        pb.setTopologyRef(phyTopoId);
        pb.setResourceType(new TpBuilder()
                .setTpHop(new TpHopBuilder().setTpRef(tpId).setNodeRef(new NodeId(neId))
                        .setSiteRef(new NodeId(siteId)).setEquipmentRef(equipId).build()).build());

        return pb.build();
    }

    private PathRouteObject getLinkPro(Long index, Link link) {
        PathRouteObjectBuilder pb = new PathRouteObjectBuilder();
        TopologyId phyTopoId = new TopologyId(Constants.PHY_TOPO_KEY);

        pb.setIndex(index);
        pb.setTopologyRef(phyTopoId);
        pb.setResourceType(new LinkBuilder()
                .setLinkHop(
                        new LinkHopBuilder().setTopologyRef(phyTopoId).setLinkRef(link.getLinkId())
                                .build())
                .build());

        return pb.build();
    }


}
