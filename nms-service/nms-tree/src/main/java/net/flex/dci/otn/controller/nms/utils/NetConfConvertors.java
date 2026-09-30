/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.links.between.two.sites.output.LinkInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.links.between.two.sites.output.LinkInfoBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.links.between.two.sites.output.LinkInfoKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.node.base.info.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otn.phy.topology.type.OtnPhyTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;

/**
 * filter for netconf convertors
 *
 * @author: xinyzhao
 * @date: 2021/4/13
 */
@Slf4j
public class NetConfConvertors {

    /**
     * convert link to link info
     *
     * @param matchedLinks
     * @return
     */
    public static List<LinkInfo> convert2LinkInfo(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> matchedLinks) {
        List<LinkInfo> infoList = new LinkedList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link : matchedLinks) {
            LinkInfoBuilder infob = new LinkInfoBuilder();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink = link
                    .getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
            infob.setFriendlyName(siteLink.getSite().getFriendlyName());
            infob.setPlaneName(siteLink.getSite().getPlaneName());
            infob.setLinkId(link.getLinkId().getValue());
            infob.setKey(new LinkInfoKey(infob.getLinkId()));
            infob.setGrid(siteLink.getSite().getGrid());
            infoList.add(infob.build());
        }
        return infoList;
    }


    /**
     * convert och link to output link
     *
     * @param links
     * @return
     */
    public static List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> convertOchLink2OutputLink(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> links) {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> output = new LinkedList<>();
        if (links == null) {
            return output;
        }

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link : links) {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkBuilder lb = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkBuilder();
            lb.fieldsFrom(link);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 link1 = link.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
            if (link1 != null) {
//                OchBuilder ob = new OchBuilder(link.getAugmentation(
//                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
//                        .getOch());

                // for basic info, discard some info.

                lb.setOch(link.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                        .getOch());
            }
//			lb.setSupportingLink(null);
            lb.setKey(
                    new LinkKey(link.getLinkId(), new TopologyId(OCH_TOPO_KEY)));

            output.add(lb.build());
        }
        return output;
    }

    /**
     * convert site link to output link
     *
     * @param links
     * @return
     */
    public static List<Link> convertSiteLink2OutputLink(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> links,
            NetconfTopology netconfTopology) {
        List<Link> output = new LinkedList<>();
        if (links == null) {
            return output;
        }

        // for basic info, discard some info.
        links.forEach(link -> {
            LinkBuilder lb = new LinkBuilder();
            lb.fieldsFrom(link);
            SiteBuilder sb = new SiteBuilder(link.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite());
            lb.setSupportingLink(null);
            sb.setAvailable(null);
            sb.setExplictRoute(null);
            sb.setProperties(netconfTopology.constructSiteLinkProperties(link).build());
            lb.setSite(sb.build());
            lb.setKey(
                    new LinkKey(link.getLinkId(), new TopologyId(SITE_TOPO_KEY)));
            output.add(lb.build());
        });
        return output;
    }

    /**
     * convert network link to output link
     *
     * @param links
     * @param netconfTopology
     * @return
     */
    public static List<Link> convertPhyLink2OutputPhyLink(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> links,
            NetconfTopology netconfTopology) {
        List<Link> output = new LinkedList<>();
        if (links == null) {
            return output;
        }

        // order by ots link at first;
        Collections.sort(links,
                new Comparator<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link>() {
                    @Override
                    public int compare(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link arg0,
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link arg1) {
                        if (arg0 == null
                                || arg0.getAugmentation(Link1.class).getPhysical().getLinkType()
                                == null) {
                            log.error("arg0 is null or {} linkType is null",
                                    arg0.getKey().getLinkId().getValue());
                            return 0;
                        }
                        if (arg1 == null
                                || arg1.getAugmentation(Link1.class).getPhysical().getLinkType()
                                == null) {
                            log.error("arg1 is null or {} linkType is null",
                                    arg1.getKey().getLinkId().getValue());
                            return 1;
                        }
                        return arg1.getAugmentation(Link1.class).getPhysical().getLinkType()
                                .compareTo(arg0.getAugmentation(Link1.class).getPhysical()
                                        .getLinkType());
                    }
                });

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link : links) {
            if (link.getLinkId().getValue().contains("LAG")) {
                //discard this virtual link.
                continue;
            }
            LinkBuilder lb = new LinkBuilder();
            lb.fieldsFrom(link);
            PhysicalBuilder pb = new PhysicalBuilder(
                    link.getAugmentation(Link1.class).getPhysical());
            pb.setProperties(netconfTopology.constructPhyLinkProperties(link).build());

            // for basic info, discard some info.
            lb.setSupportingLink(null);
            lb.setKey(
                    new LinkKey(link.getLinkId(), new TopologyId(PHY_TOPO_KEY)));
            lb.setPhysical(pb.build());

            output.add(lb.build());
        }
        return output;
    }

    /**
     * convert node
     */
//    public static List<Node> convertSiteNode2OutputNode(
//            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> ntNodes) {
//        TopologyId topoId = new TopologyId(TopoNameConstants.Site_Topo_Key);
//        List<Node> output = NmsSiteOutputConverters.convert2NmsOutput(ntNodes);
//        if (ntNodes == null) {
//            return output;
//        }
//
//        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node : ntNodes) {
//            NodeBuilder nb = new NodeBuilder();
//            nb.fieldsFrom(node);
//            nb.setSupportingNode(null);
//
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder sb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(
//                    node.getAugmentation(
//                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
//                            .getSite());
//
//            // for basic info. discard some info.
//            sb.setSupportingRack(null);
//
//            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.node.base.info.TerminationPoint> tpList = new LinkedList<>();
//            if (node.getTerminationPoint() != null) {
//                for (TerminationPoint tp : node.getTerminationPoint()) {
//                    TerminationPointBuilder tb = new TerminationPointBuilder(tp);
//                    tb.fieldsFrom(tp.getAugmentation(
//                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.TerminationPoint1.class));
//                    tpList.add(tb.build());
//                }
//            }
//            nb.setTerminationPoint(tpList);
//            nb.setSite(sb.build());
//            nb.setTopologyRef(topoId);
//            nb.setKey(new NodeKey(node.getNodeId(), topoId));
//            output.add(nb.build());
//        }
//        return output;
//    }

    /**
     * convert network node to rev180816.nodes
     *
     * @param ntNodes
     * @return
     * @throws Exception
     */
    public static List<Node> convertNtNode2OutputNode(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> ntNodes,
            NetconfTopology netconfTopology)
            throws Exception {
        TopologyId topoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
        List<Node> output = new LinkedList<>();
        if (ntNodes == null) {
            return output;
        }

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node : ntNodes) {
            NodeBuilder nb = new NodeBuilder();
            nb.fieldsFrom(node);
            nb.setSupportingNode(null);

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder pb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                    node.getAugmentation(Node1.class).getPhysical());

            // for basic info. discard some info.
            // pb.setSystem(null);
            pb.setInternalLinks(null);
            pb.setCrossConnections(null);

            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.node.base.info.TerminationPoint> tpList = new LinkedList<>();
            if (node.getTerminationPoint() != null) {
                for (TerminationPoint tp : node.getTerminationPoint()) {
                    TerminationPointBuilder tb = new TerminationPointBuilder(tp);
                    tb.fieldsFrom(tp.getAugmentation(TerminationPoint1.class));
                    tpList.add(tb.build());
                }
                nb.setTerminationPoint(tpList);
            } else {
                log.error("find an error, the NE {}, hasn't any TP.",
                        node.getNodeId().getValue());
            }
            nb.setTopologyRef(topoId);
            nb.setKey(new NodeKey(node.getNodeId(), topoId));

            pb.setProperties(netconfTopology.constructPhyNodeProperties(node).build());
            nb.setPhysical(pb.build());
            output.add(nb.build());
        }
        return output;
    }

    /**
     * convert network node to rack output node
     *
     * @param ntNodes
     * @return
     */
    public static List<Node> convertNtNode2RackOutputNode(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> ntNodes) {
        TopologyId topoId = new TopologyId(Constants.SITE_TOPO_KEY);
        List<Node> output = new LinkedList<>();
        if (ntNodes == null) {
            return output;
        }

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node : ntNodes) {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeBuilder nb = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeBuilder();
            nb.fieldsFrom(node);
            nb.setSupportingNode(null);

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder sb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(
                    node.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                            .getSite());

            nb.setSite(sb.build());
            nb.setTopologyRef(topoId);
            nb.setKey(new NodeKey(node.getNodeId(), topoId));
            output.add(nb.build());
        }
        return output;
    }

    /**
     * convert to phy node
     *
     * @param convertNode
     * @return
     */
    public static org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node convertPhyNode(
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node convertNode) {
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node node =
                new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeBuilder()
                        .setTopologyRef(convertNode.getTopologyRef())
                        .setNodeId(convertNode.getNodeId())
                        .setPhysical(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder()
                                        .setDomainName(convertNode.getPhysical().getDomainName())
                                        .setFriendlyName(
                                                convertNode.getPhysical().getFriendlyName())
                                        .setIp(convertNode.getPhysical().getIp())
                                        .setPlaneName(convertNode.getPhysical().getPlaneName())
                                        .setRiskGroupName(
                                                convertNode.getPhysical().getRiskGroupName())
                                        .build())
                        .build();
        return node;
    }


    /**
     * convert to site Node
     *
     * @param convertNode
     * @return
     */
    public static org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node convertSiteNode(
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node convertNode) {
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node node =
                new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeBuilder()
                        .setTopologyRef(convertNode.getTopologyRef())
                        .setNodeId(convertNode.getNodeId())
                        .setSite(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder()
                                        .setAdminState(convertNode.getSite().getAdminState())
                                        .setAlarmState(convertNode.getSite().getAlarmState())
                                        .setDomainName(convertNode.getSite().getDomainName())
                                        .setFriendlyName(convertNode.getSite().getFriendlyName())
                                        .setImplementState(
                                                convertNode.getSite().getImplementState())
                                        .setIsVirtual(convertNode.getSite().isIsVirtual())
                                        .setOperationalState(
                                                convertNode.getSite().getOperationalState())
                                        .setSiteType(convertNode.getSite().getSiteType())
                                        .build())
                        .build();
        return node;
    }


    /**
     * convert opersite link to network link
     *
     * @param operSiteLink
     * @return
     * @throws Exception
     */
    public static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link convertLink(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link operSiteLink,
            NetconfTopology netconfTopology) throws Exception {
        List<CrossConnections> mainCrossConns = new ArrayList<CrossConnections>();
        List<CrossConnections> spareCrossConns = new ArrayList<CrossConnections>();
        List<Route> routes = new ArrayList<Route>();

        Site site = operSiteLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
        ExplictRoute explictRoute = operSiteLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite()
                .getExplictRoute();
        for (Route operRoute : explictRoute.getRoute()) {
            if (operRoute.getPrimary().getCrossConnections() != null) {
                for (CrossConnections operXc : operRoute.getPrimary().getCrossConnections()) {
                    String desTp = operXc.getDestinationTp().get(0).getTpRef().getValue();
                    String desTmp[] = desTp.split("#");
                    String desNeId = desTmp[0] + "#" + desTmp[1];
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections phyXc
                            = getCrossConnection(netconfTopology, new NodeId(desNeId),
                            operXc.getCrossConnectionId().getValue());
                    if (phyXc != null) {
                        CrossConnections xc = new CrossConnectionsBuilder(phyXc)
                                .setSequence(operXc.getSequence()).build();
                        mainCrossConns.add(xc);
                    }
                }
            }
            if (operRoute.getSecondary().getCrossConnections() != null) {
                for (CrossConnections operXc : operRoute.getSecondary().getCrossConnections()) {
                    String desTp = operXc.getDestinationTp().get(0).getTpRef().getValue();
                    String desTmp[] = desTp.split("#");
                    String desNeId = desTmp[0] + "#" + desTmp[1];
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections phyXc
                            = getCrossConnection(netconfTopology, new NodeId(desNeId),
                            operXc.getCrossConnectionId().getValue());
                    if (phyXc != null) {
                        CrossConnections xc = new CrossConnectionsBuilder(phyXc)
                                .setSequence(operXc.getSequence()).build();
                        spareCrossConns.add(xc);
                    }
                }
            }

            Route route = new RouteBuilder()
                    .setIndex(operRoute.getIndex())
                    .setPrimary(
                            new PrimaryBuilder()
                                    .setCrossConnections(mainCrossConns)
                                    .build()
                    )
                    .setSecondary(
                            new SecondaryBuilder()
                                    .setCrossConnections(spareCrossConns)
                                    .build()
                    )
                    .build();

            routes.add(route);
        }

        ExplictRoute exRoute = new ExplictRouteBuilder()
                .setRoute(routes)
                .build();

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink = new Link1Builder()
                .setSite(OperateSiteBuilder(site, exRoute))
                .build();

        Source source = new SourceBuilder(operSiteLink.getSource()).build();

        Destination destination = new DestinationBuilder(operSiteLink.getDestination()).build();

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder()
                .setLinkId(operSiteLink.getLinkId())
                .setSource(source)
                .setDestination(destination)
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                        siteLink)
                .build();

        return link;
    }

//    /**
//     * convert tunnel to out put tunnel
//     *
//     * @param tunnels
//     * @param netconfTopology
//     * @return
//     * @throws Exception
//     */
//    public static List<Tunnel> convertTunnel2OutputTunnel(
//            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> tunnels,
//            NetconfTopology netconfTopology) throws Exception {
//        List<Tunnel> output = new LinkedList<>();
//        if (tunnels == null || tunnels.isEmpty()) {
//            return output;
//        }
//
//        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel tunnel : tunnels) {
//            TunnelBuilder tb = new TunnelBuilder();
//            tb.fieldsFrom(tunnel);
//
//            // for basic info, discard some info.
//            tb.setSupportingLink(null);
//            tb.setExplictRoute(null);
//
//            tb.setKey(new TunnelKey(tunnel.getTunnelId()));
//            tb.setProperties(netconfTopology.constructTunnelProperties(tunnel).build());
//
//            output.add(tb.build());
//        }
//        return output;
//    }

    private static Site OperateSiteBuilder(Site operSite, ExplictRoute exRoute) {
        Site site = new SiteBuilder()
                .setPlaneName(operSite.getPlaneName())
//                .setConnectionStatus(operSite.getConnectionStatus())
                .setGrid(operSite.getGrid())
                .setFriendlyName(operSite.getFriendlyName())
                .setBandwidth(operSite.getBandwidth())
                .setRiskGroupName(operSite.getRiskGroupName())
                .setExplictRoute(exRoute)
                .setOperationalState(operSite.getOperationalState())
                .build();
        return site;
    }


    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections getCrossConnection(
            NetconfTopology netconfTopology, NodeId nodeId, String xcId) throws Exception {

        return netconfTopology
                .getCrossConnection(new TopologyId(OtnPhyTopology.QNAME.getLocalName()), nodeId,
                        xcId);
    }

}
