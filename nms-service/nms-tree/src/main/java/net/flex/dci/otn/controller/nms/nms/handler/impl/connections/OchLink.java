/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.connections;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.constructs.YangRouteConstructor;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.TpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHopBuilder;


/**
 * @author: xinyzhao
 * @date: 2021/4/6
 */
@Slf4j
public class OchLink extends AbstractTopoLink {

//    private SiteLink siteLink;

    public OchLink(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.siteLink = new SiteLink(netconfTopology);
    }

    @Override
    public List<Tunnel> getTunnels(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all Tunnels which passthrough the OCH Link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception("cannot find required link.");
        }

        List<Tunnel> output = new LinkedList<>();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1 link1 =
                ntLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class);
        for (SupportedTunnel sl : link1.getSupportedTunnel()) {
            Tunnel tunnel = netconfTopology.getTunnel(sl.getTunnelRef());
            if (tunnel != null) {
                output.add(tunnel);
            }
        }
        return output;
    }

    @Override
    public List<Tunnel> getTunnels(Link ntLink) throws Exception {
        List<Tunnel> output = new LinkedList<>();
        Set<Tunnel> rst = new HashSet<>();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 link1 = ntLink
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
        OchLink ochLink = new OchLink(netconfTopology);
        SiteLink siteLink = new SiteLink(netconfTopology);

        if (link1.getPhysical().getSupportedLink() != null) {
            // not all phy link has supported link (och or site link)
            for (SupportedLink sl : link1.getPhysical().getSupportedLink()) {
                if (sl.getTopologyRef().getValue().equals(SITE_TOPO_KEY)) {
                    rst.addAll(siteLink.getTunnels(sl.getTopologyRef(), sl.getLinkRef()));
                } else if (sl.getTopologyRef().getValue().equals(OCH_TOPO_KEY)) {
                    rst.addAll(ochLink.getTunnels(sl.getTopologyRef(), sl.getLinkRef()));
                }
            }
            output.addAll(rst);
        }
        return output;
    }


    private PathRouteObject getTpPro(Long index, TpId tpId) {
        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
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


    @Override
    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getSiteRacks(
            TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all Rack based on OCH Link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        List<Link> siteLinks = getSiteLinks(topologyRef, linkRef);

        List<Node> output = new LinkedList<>();
        Set<Node> rst = new HashSet<>();

        TopologyId siteTopoId = new TopologyId(Constants.SITE_TOPO_KEY);
        for (Link link : siteLinks) {
            rst.addAll(new SiteLink(netconfTopology).getSiteRacks(siteTopoId, link.getLinkId()));
        }
        output.addAll(rst);
        return output;
    }

    @Override
    public List<Link> getPhyLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all PHY link based on OCH Link. with link:{}", linkRef.getValue());

        Link ochNtLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ochNtLink == null) {
            throw new Exception(
                    "cannot find required OCH link: " + linkRef.getValue());
        }

        TopologyId phyTopoId = new TopologyId(PHY_TOPO_KEY);
        TopologyId siteTopoId = new TopologyId(Constants.SITE_TOPO_KEY);

        List<Link> output = new LinkedList<>();
        for (SupportingLink sl : ochNtLink.getSupportingLink()) {
            //och-link's supporting-link includes phy-link and site-like
            Link link = netconfTopology.getLink(phyTopoId, sl.getLinkRef());
            if (link != null && link.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                    != null) {
                //this supporting link is phy link, add directly
                output.add(link);
            } else {
                link = netconfTopology.getLink(siteTopoId, sl.getLinkRef());
                if (link != null && link.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                        != null) {
                    output.addAll(new SiteLink(netconfTopology).getPhyLinks(link));
                } else {
                    log.error(
                            "find an issue, OCH supporting link {}, cannot find in PHY topo and SITE topo.",
                            sl.getLinkRef().getValue());
                }
            }
        }

        return output;
    }


    @Override
    public List<Link> getSiteLinks(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all Site link based on OCH Link. with link:{}", linkRef.getValue());

        Link ochNtLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ochNtLink == null) {
            throw new Exception(
                    "cannot find required object: " + linkRef.getValue());
        }

        TopologyId siteTopoId = new TopologyId(Constants.SITE_TOPO_KEY);
        List<Link> output = new LinkedList<>();
        for (SupportingLink sl : ochNtLink.getSupportingLink()) {
            //och-link's supporing-link includes phy-link and site-link
            Link link = netconfTopology.getLink(siteTopoId, sl.getLinkRef());
            if (link != null && link.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    != null) {
                output.add(link);
            }
        }
        return output;
    }

    @Override
    public List<Node> getPhyNodes(TopologyId topologyRef, LinkId linkRef) throws Exception {
        log.debug("start get all PHY NE based on OCH Link. with topo:{}, link:{}",
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
        log.debug("start get all Site node based on OCH Link. with topo:{}, link:{}",
                topologyRef.getValue(), linkRef.getValue());

        Link ntLink = netconfTopology.getLink(topologyRef, linkRef);
        if (ntLink == null) {
            throw new Exception(
                    "cannot find required OCH link: " + linkRef.getValue());
        }

        Link1 ochLink = ntLink.getAugmentation(Link1.class);
        YangRouteConstructor route = new YangRouteConstructor(netconfTopology);
        return route.getAllSites(ochLink.getOch().getExplictRoute());
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
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 link1 = ntLink
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
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


}
