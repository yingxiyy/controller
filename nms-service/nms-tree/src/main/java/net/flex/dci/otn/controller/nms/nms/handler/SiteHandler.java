/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMiddleSitesBetweenTwoSitesInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMiddleSitesBetweenTwoSitesOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMiddleSitesBetweenTwoSitesOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.Topology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/9
 */
@Slf4j
@Component
public class SiteHandler extends AbstractBaseHandler {

    public SiteHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public GetMiddleSitesBetweenTwoSitesOutput getMiddleSitesBetweenTwoSites(
            GetMiddleSitesBetweenTwoSitesInput input) throws Exception {
        log.info("start to get middle site between two sites");
        GetMiddleSitesBetweenTwoSitesOutputBuilder outputBuilder = new GetMiddleSitesBetweenTwoSitesOutputBuilder();
        LinkHandler linkHandler = new LinkHandler(netconfTopology);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> matchedLinks = linkHandler
                .getSiteLinksBetweenTwoSites(new TopologyId(TopoNameConstants.Site_Topo_Key),
                        input.getNodeIds(), input.getPlane());
        List<String> siteNodeIds = getSiteNodeIds(input.getNodeIds(), matchedLinks);
        outputBuilder.setTopologyRef(new TopologyId(TopoNameConstants.Site_Topo_Key));
        outputBuilder.setNodeIds(siteNodeIds);
        return outputBuilder.build();
    }

    @Override
    public List<Topology> getSiteTopo() throws Exception {
        TopologyId topoId = new TopologyId(TopoNameConstants.Site_Topo_Key);
        List<Topology> siteTopo = new LinkedList<>();

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology ntTopo = netconfTopology
                .getTopology(topoId);

        if (ntTopo == null) {
            throw new Exception(
                    "site topo isn't ready");
        }

        if (ntTopo.getNode() == null) {
            throw new Exception(
                    "site topo isn't ready, node list is empty");
        }
        List<Node> nodeList = new LinkedList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node : ntTopo
                .getNode()) {

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder siteNode = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(
                    node.getAugmentation(Node1.class).getSite());
            siteNode.setSupportingRack(null);

            NodeBuilder nb = new NodeBuilder();
            nb.fieldsFrom(node);
            nb.setSupportingNode(null)
                    .setSite(siteNode.build())
                    .setKey(new NodeKey(nb.getNodeId()));
            nodeList.add(nb.build());
        }
        if (ntTopo.getLink() == null) {
            throw new Exception(
                    "site topo isn't ready, link list is empty");
        }
        List<Link> linkList = new LinkedList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link : ntTopo
                .getLink()) {

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder siteLink = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder(
                    link.getAugmentation(Link1.class).getSite());
            siteLink.setAvailable(null).setExplictRoute(null).setSupportedLink(null);
            LinkBuilder lb = new LinkBuilder();
            lb.fieldsFrom(link);
            lb.setSupportingLink(null)
                    .setSite(siteLink.build())
                    .setKey(new LinkKey(lb.getLinkId()));
            linkList.add(lb.build());
        }

        TopologyBuilder tb = new TopologyBuilder().setTopologyId(ntTopo.getTopologyId());
        tb.setNode(nodeList).setLink(linkList).setKey(new TopologyKey(tb.getTopologyId()));

        siteTopo.add(tb.build());
        return siteTopo;
    }


    private List<String> getSiteNodeIds(List<String> nodeIds,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> matchedLinks) {
        List<String> output = new LinkedList<>();
        Set<String> siteIds = new HashSet<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link : matchedLinks) {
            Site siteLink = link.getAugmentation(Link1.class).getSite();
            siteIds.addAll(getSites(siteLink));
        }
        for (String id : siteIds) {
            if (id.equals(nodeIds.get(0)) || id.equals(nodeIds.get(1))) {
                continue;
            }
            output.add(id);
        }
        return output;
    }

    private Set<String> getSites(Site siteLink) {
        Set<String> siteIds = new HashSet<>();
        for (Route route : siteLink.getExplictRoute().getRoute()) {
            siteIds = getSitesObj(route.getPrimary().getExplicitRouteObjects());
            if (route.getSecondary() != null) {
                siteIds.addAll(getSitesObj(route.getSecondary().getExplicitRouteObjects()));
            }
        }
        return siteIds;
    }

    private Set<String> getSitesObj(List<ExplicitRouteObjects> explicitRouteObjects) {
        Set<String> siteIds = new HashSet<>();
        if (explicitRouteObjects == null) {
            return siteIds;
        }

        for (ExplicitRouteObjects ero : explicitRouteObjects) {
            for (PathRouteObject pro : ero.getPathRouteObject()) {
                if (pro.getResourceType().getImplementedInterface().getName()
                        .equals(Tp.class.getName())) {
                    Tp tp = (Tp) pro.getResourceType();
                    siteIds.add(tp.getTpHop().getSiteRef().getValue());
                }
            }
        }
        return siteIds;
    }
}
