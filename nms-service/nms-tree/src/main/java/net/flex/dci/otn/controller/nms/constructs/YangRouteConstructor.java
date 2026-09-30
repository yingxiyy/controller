/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.constructs;

import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_PORT_SIZE;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.XC_PREFIX;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.MuxCardPortFormatting;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.TpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.TpHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.SiteNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.ExplicitRouteHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.RouteUsageInclude;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;


/**
 * @author: xinyzhao
 * @date: 2021/4/2
 */
@Slf4j
public class YangRouteConstructor {

    private final List<RouteInfo> routeInfos;
    private final Set<Secondary> pendingList;
    private String muxChannelTpId;

    private final NetconfTopology netconfTopology;

    public YangRouteConstructor(NetconfTopology netconfTopology) {
        this.netconfTopology = netconfTopology;
        routeInfos = new LinkedList<>();
        pendingList = new HashSet<>();
    }

//    /**
//     * get detail route from route list
//     *
//     * @param routes
//     */
//    public void extractDetailRoute(List<Route> routes) throws Exception {
//        log.debug("start build detail route");
//        Collections.sort(routes, Comparator.comparing(Route::getIndex));
//        for (Route route : routes) {
//            RouteInfoBuilder rib = new RouteInfoBuilder();
//            rib.setIndex((short) (routeInfos.size() + 1));
//            rib.setKey(new RouteInfoKey(rib.getIndex()));
//
//            //Primary
//            Primary primary = route.getPrimary();
//            if (primary != null) {
//                rib.setPrimary(getPrimaryRouteSequence(primary.getExplicitRouteObjects(),
//                        primary.getCrossConnections(), 1l));
//            }
//            Secondary s = route.getSecondary();
//            if (s != null && s.getExplicitRouteObjects() != null && !s.getExplicitRouteObjects()
//                    .isEmpty()) {
//                rib.setSecondary(
//                        getSecondaryRouteSequence(s.getExplicitRouteObjects(),
//                                s.getCrossConnections(), 1L));
//                routeInfos.add(rib.build());
//            } else if (pendingList.size() == 1) {
//                //process pendingList as secondary.
//                s = (Secondary) pendingList.toArray()[0];
//                if (s != null && s.getExplicitRouteObjects() != null && !s.getExplicitRouteObjects()
//                        .isEmpty()) {
//                    rib.setSecondary(
//                            getSecondaryRouteSequence(s.getExplicitRouteObjects(),
//                                    s.getCrossConnections(), 1L));
//                    routeInfos.add(rib.build());
//                }
//                pendingList.clear();
//            }
//
//            routeInfos.add(rib.build());
//
//        }
//    }

    public void extractDetailRoute(RouteInfoDto routeInfo) throws Exception {
        log.debug("start build detail route");
        String source = routeInfo.getSource();
        String destination = routeInfo.getDestination();
        List<Route> routes = routeInfo.getRoutes();
        routes.sort(Comparator.comparing(Route::getIndex));
        for (Route route : routes) {
            RouteInfoBuilder rib = new RouteInfoBuilder();
            rib.setIndex((short) (routeInfos.size() + 1));
            rib.setKey(new RouteInfoKey(rib.getIndex()));

//            Primary
            Primary primary = route.getPrimary();
            if (primary != null) {
                rib.setPrimary(getPrimaryRouteSequence(primary.getExplicitRouteObjects(),
                        primary.getCrossConnections(), 1L, source, destination));
            }
            Secondary s = route.getSecondary();
            if (s != null && s.getExplicitRouteObjects() != null && !s.getExplicitRouteObjects()
                    .isEmpty()) {
                rib.setSecondary(
                        getSecondaryRouteSequence(s.getExplicitRouteObjects(),
                                s.getCrossConnections(), 1L, source, destination));
                routeInfos.add(rib.build());
            } else if (pendingList.size() == 1) {
                //process pendingList as secondary.
                s = (Secondary) pendingList.toArray()[0];
                if (s != null && s.getExplicitRouteObjects() != null && !s.getExplicitRouteObjects()
                        .isEmpty()) {
                    rib.setSecondary(
                            getSecondaryRouteSequence(s.getExplicitRouteObjects(),
                                    s.getCrossConnections(), 1L, source, destination));
                    routeInfos.add(rib.build());
                }
                pendingList.clear();
            }

            routeInfos.add(rib.build());

        }
    }
//
//    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary getSecondaryRouteSequence(
//            List<ExplicitRouteObjects> eros, List<CrossConnections> crossConnections, Long seq)
//            throws Exception {
//        List<CrossConnections> underLayerXC = new LinkedList<>();
//        List<RouteSequence> rss = convertEro2Rs(
//                eros, seq, underLayerXC);
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.SecondaryBuilder sb =
//                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.SecondaryBuilder();
//        sb.setRouteSequence(rss);
//        List<CrossConnections> xcList = new ArrayList<CrossConnections>();
//        List<CrossConnections> resultXcList = new ArrayList<CrossConnections>();
//        xcList.addAll(crossConnections);
//        xcList.addAll(underLayerXC);
//        Long seqNo = 1L;
//
//        List<String> siteList = new ArrayList<>();
//        TopologyId topologyRef = new TopologyId(Constants.PHY_TOPO_KEY);
//        for (CrossConnections xc : xcList) {

    /// /      CrossConnectionsBuilder xcBuilder=new CrossConnectionsBuilder(); /
    /// xcBuilder.fieldsFrom(xc);
//            String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
//            String ids[] = tpRef.split(POUND);
//            NodeId nodeId = new NodeId(ids[0] + POUND + ids[1]);
//            //NodeId nodeId = xc.getNodeRef();
//
//            CrossConnectionsBuilder xcBuilder = new CrossConnectionsBuilder();
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections neXc = netconfTopology
//                    .getXc(topologyRef, nodeId, xc.getCrossConnectionId());
//            if (neXc != null) {
//                if (convertYangXc2RouteXc(xcBuilder, neXc) == false) {
//                    continue;
//                }
//            } else {
//                log.error("find one error routeXC which cannot find in neXC.");
//                xcBuilder.fieldsFrom(xc);
//            }
//            xcBuilder.setSequence(seqNo);
//            seqNo++;
//            resultXcList.add(xcBuilder.build());
//
//            //all XC in secondary route used only on split node
//            addSiteSequence(siteList, nodeId);
//        }
//        sb.setCrossConnections(resultXcList);
//        sb.setSiteSequence(siteList);
//        return sb.build();
//    }
    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary getSecondaryRouteSequence(
            List<ExplicitRouteObjects> eros, List<CrossConnections> crossConnections, Long seq,
            String source, String destination)
            throws Exception {
        List<CrossConnections> underLayerXC = new LinkedList<>();
        List<RouteSequence> rss = convertEro2Rs(
                eros, seq, underLayerXC, source, destination);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.SecondaryBuilder sb =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.SecondaryBuilder();
        sb.setRouteSequence(rss);
        List<CrossConnections> xcList = new ArrayList<CrossConnections>();
        List<CrossConnections> resultXcList = new ArrayList<CrossConnections>();
        xcList.addAll(crossConnections);
        xcList.addAll(underLayerXC);
        Long seqNo = 1L;

        List<String> siteList = new ArrayList<>();
        TopologyId topologyRef = new TopologyId(Constants.PHY_TOPO_KEY);
        for (CrossConnections xc : xcList) {
//      CrossConnectionsBuilder xcBuilder=new CrossConnectionsBuilder();
//      xcBuilder.fieldsFrom(xc);
            String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
            String[] ids = tpRef.split(POUND);
            NodeId nodeId = new NodeId(ids[0] + POUND + ids[1]);
            //NodeId nodeId = xc.getNodeRef();

            CrossConnectionsBuilder xcBuilder = new CrossConnectionsBuilder();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections neXc = netconfTopology
                    .getXc(topologyRef, nodeId, xc.getCrossConnectionId());
            if (neXc != null) {
                if (!convertYangXc2RouteXc(xcBuilder, neXc)) {
                    continue;
                }
            } else {
                log.error("find one error routeXC which cannot find in neXC.");
                xcBuilder.fieldsFrom(xc);
            }
            xcBuilder.setSequence(seqNo);
            seqNo++;
            resultXcList.add(xcBuilder.build());

            //all XC in secondary route used only on split node
            addSiteSequence(siteList, nodeId.getValue());
        }
        sb.setCrossConnections(resultXcList);
        sb.setSiteSequence(siteList);
        return sb.build();
    }

    private List<RouteSequence> convertEro2Rs(List<ExplicitRouteObjects> explicitRouteObjects,
            Long seq, List<CrossConnections> underLayerXC) throws Exception {
        log.debug("start convert route objects to detail route objects.");
        List<RouteSequence> outputs = new LinkedList<>();
        if (explicitRouteObjects.isEmpty()) {
            return outputs;
        }

        ExplicitRouteObjects ero = explicitRouteObjects.get(0);
        if (ero == null) {
            return outputs;
        }

        boolean discard = seq > 1;
        //the start/end point of this link has reported in upper layer

        List<PathRouteObject> proList = ero.getPathRouteObject();
        Collections.sort(proList, Comparator.comparing(ExplicitRouteHop::getIndex));
        for (PathRouteObject pro : proList) {
            RouteSequenceBuilder ob = new RouteSequenceBuilder();

            if (pro.getResourceType().getImplementedInterface().getName()
                    .equals(Tp.class.getName())) {
                if (discard) {
                    if (pro.getIndex() == 1 || pro.getIndex() == proList.size()) {
                        continue;
                    }
                }

                ob.setKey(new RouteSequenceKey(seq));
                ob.setSequence(seq);
                ob.setTopologyRef(pro.getTopologyRef());
                ob.setResourceType(convertYangTpRoute2DetailRoute((Tp) pro.getResourceType()));
                outputs.add(ob.build());
                seq++;

            } else if (pro.getResourceType().getImplementedInterface().getName()
                    .equals(Link.class.getName())) {
                if (pro.getTopologyRef().getValue().equals(Constants.PHY_TOPO_KEY)) {
                    ob.setKey(new RouteSequenceKey(seq));
                    ob.setSequence(seq);
                    ob.setTopologyRef(pro.getTopologyRef());
                    ob.setResourceType(getYangPhyLinkDetailRoute(pro.getTopologyRef(),
                            (Link) pro.getResourceType()));
                    outputs.add(ob.build());
                    seq++;
                } else if (pro.getTopologyRef().getValue().equals(Constants.OCH_TOPO_KEY)) {
                    List<RouteSequence> ochRs = convertYangOchLinkRoute2DetailRoute(
                            pro.getTopologyRef(),
                            (Link) pro.getResourceType(), seq, underLayerXC);
                    outputs.addAll(ochRs);
                    seq += ochRs.size();
                } else if (pro.getTopologyRef().getValue()
                        .equals(Constants.SITE_TOPO_KEY)) {
                    List<RouteSequence> siteRs = convertYangSiteLinkRoute2DetailRoute(
                            pro.getTopologyRef(),
                            (Link) pro.getResourceType(), seq, underLayerXC);
                    outputs.addAll(siteRs);
                    seq += siteRs.size();
                }
            }
        }
        return outputs;
    }

    private List<RouteSequence> convertEro2Rs(List<ExplicitRouteObjects> explicitRouteObjects,
            Long seq, List<CrossConnections> underLayerXC, String source, String destination)
            throws Exception {
        log.debug("start convert route objects to detail route objects.");
        List<RouteSequence> outputs = new LinkedList<>();
        if (explicitRouteObjects.isEmpty()) {
            return outputs;
        }

        ExplicitRouteObjects ero = explicitRouteObjects.get(0);
        if (ero == null) {
            return outputs;
        }

        boolean discard = seq > 1;
        //the start/end point of this link has reported in upper layer

//        List<PathRouteObject> proList = ero.getPathRouteObject();
//        proList.sort(Comparator.comparing(ExplicitRouteHop::getIndex));
//        //todo:get direction for the route
//        PathRouteObject firstSeq = proList.get(0);
//        Tp resourceType = (Tp) firstSeq.getResourceType();
//        String routeSite = resourceType.getTpHop().getSiteRef().getValue();
        List<PathRouteObject> proList = getDirectProList(ero.getPathRouteObject(), source,
                destination);
        for (PathRouteObject pro : proList) {
            RouteSequenceBuilder ob = new RouteSequenceBuilder();

            if (pro.getResourceType().getImplementedInterface().getName()
                    .equals(Tp.class.getName())) {
                if (discard) {
                    if (pro.getIndex() == 1 || pro.getIndex() == proList.size()) {
                        continue;
                    }
                }

                ob.setKey(new RouteSequenceKey(seq));
                ob.setSequence(seq);
                ob.setTopologyRef(pro.getTopologyRef());
                ob.setResourceType(convertYangTpRoute2DetailRoute((Tp) pro.getResourceType()));
                outputs.add(ob.build());
                seq++;

            } else if (pro.getResourceType().getImplementedInterface().getName()
                    .equals(Link.class.getName())) {
                if (pro.getTopologyRef().getValue().equals(Constants.PHY_TOPO_KEY)) {
                    ob.setKey(new RouteSequenceKey(seq));
                    ob.setSequence(seq);
                    ob.setTopologyRef(pro.getTopologyRef());
                    ob.setResourceType(getYangPhyLinkDetailRoute(pro.getTopologyRef(),
                            (Link) pro.getResourceType(), source, destination));
                    outputs.add(ob.build());
                    seq++;
                } else if (pro.getTopologyRef().getValue().equals(Constants.OCH_TOPO_KEY)) {
                    List<RouteSequence> ochRs = convertYangOchLinkRoute2DetailRoute(
                            pro.getTopologyRef(),
                            (Link) pro.getResourceType(), seq, underLayerXC, source, destination);
                    outputs.addAll(ochRs);
                    seq += ochRs.size();
                } else if (pro.getTopologyRef().getValue()
                        .equals(Constants.SITE_TOPO_KEY)) {
                    List<RouteSequence> siteRs = convertYangSiteLinkRoute2DetailRoute(
                            pro.getTopologyRef(),
                            (Link) pro.getResourceType(), seq, underLayerXC, source, destination);
                    outputs.addAll(siteRs);
                    seq += siteRs.size();
                }
            }
        }
        return outputs;
    }

    private List<PathRouteObject> getDirectProList(List<PathRouteObject> pathRouteObjects,
            String source, String destination) {
        List<PathRouteObject> proList = pathRouteObjects;
        proList.sort(Comparator.comparing(ExplicitRouteHop::getIndex));
        //todo:get direction for the route
        PathRouteObject firstSeq = proList.get(0);
        Tp resourceType = (Tp) firstSeq.getResourceType();
        String routeSite = resourceType.getTpHop().getSiteRef().getValue();
        if (routeSite.equalsIgnoreCase(source)) {
            proList.sort(Comparator.comparing(ExplicitRouteHop::getIndex));
        } else {
            proList.sort(Comparator.comparing(ExplicitRouteHop::getIndex).reversed());
        }
        return proList;
    }

    public List<RouteInfo> getRouteInfos() {
        return routeInfos;
    }

//    /**
//     * get primary route route sequence
//     *
//     * @param explicitRouteObjects
//     * @param crossConnections
//     * @param seq
//     * @return
//     */
//    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Primary getPrimaryRouteSequence(
//            List<ExplicitRouteObjects> explicitRouteObjects,
//            List<CrossConnections> crossConnections, Long seq) throws Exception {
//        List<CrossConnections> underLayerXC = new LinkedList<>();
//        List<RouteSequence> rss = getRouteSequence(explicitRouteObjects, seq, underLayerXC);
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.PrimaryBuilder pb =
//                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.PrimaryBuilder();
//        pb.setRouteSequence(rss);
//
//        pb.setCrossConnections(crossConnections);
//        List<CrossConnections> xcList = new ArrayList<CrossConnections>();
//        List<CrossConnections> resultXcList = new ArrayList<CrossConnections>();
//        xcList.addAll(crossConnections);
//        xcList.addAll(underLayerXC);
//        //for flex-grid tunnel, we have to generate a fake xc on MUX-panel
//        Long seqNo = 1L;
//
//        List<String> siteList = new ArrayList<>();
//        TopologyId topologyRef = new TopologyId(Constants.PHY_TOPO_KEY);
//        boolean split = false;
//        for (CrossConnections xc : xcList) {
//            String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
//            String ids[] = tpRef.split(POUND);
//            NodeId nodeId = new NodeId(ids[0] + POUND + ids[1]);
//            //NodeId nodeId = xc.getNodeRef();
//            String crossConnectionId = xc.getCrossConnectionId().getValue();
//            CrossConnectionsBuilder xcBuilder = new CrossConnectionsBuilder();
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections neXc = netconfTopology
//                    .getXc(topologyRef, nodeId, xc.getCrossConnectionId());
//            if (neXc != null) {
//                if (!convertYangXc2RouteXc(xcBuilder, neXc)) {
//                    continue;
//                }
//            } else {
//                if (crossConnectionId.endsWith(MPO_SUFFIX)) {
//                    CrossConnections virtualizeXC = CMUX64Constructor.virtualizeTheXC(xc,
//                            muxChannelTpId);
//                    xcBuilder.fieldsFrom(virtualizeXC);
//                } else {
//                    xcBuilder.fieldsFrom(xc);
//                }
//            }
//            xcBuilder.setSequence(seqNo);
//            seqNo++;
//            resultXcList.add(xcBuilder.build());
//            if (xcBuilder.getSourceTp().size() > 1 || xcBuilder.getDestinationTp().size() > 1) {
//                //this is separate point.
//                if (split) {
//                    //add last one site
//                    addSiteSequence(siteList, nodeId);
//                }
//                split = !split;
//            }
//            if (split) {
//                //add site when find split.
//                addSiteSequence(siteList, nodeId);
//            }
//        }
//        pb.setCrossConnections(resultXcList);
//        pb.setSiteSequence(siteList);
//
//        return pb.build();
//    }


    /**
     * get route sequence by direction
     *
     * @param explicitRouteObjects
     * @param crossConnections
     * @param seq
     * @param source
     * @param destination
     * @return
     * @throws Exception
     */
    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Primary getPrimaryRouteSequence(
            List<ExplicitRouteObjects> explicitRouteObjects,
            List<CrossConnections> crossConnections, Long seq, String source, String destination)
            throws Exception {
        List<CrossConnections> underLayerXC = new LinkedList<>();
        List<RouteSequence> rss = getRouteSequence(explicitRouteObjects, seq, underLayerXC, source,
                destination);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.PrimaryBuilder pb =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.PrimaryBuilder();
        pb.setRouteSequence(rss);

        pb.setCrossConnections(crossConnections);
        List<CrossConnections> xcList = new ArrayList<CrossConnections>();
        List<CrossConnections> resultXcList = new ArrayList<CrossConnections>();
        xcList.addAll(crossConnections);
        xcList.addAll(underLayerXC);
        //for flex-grid tunnel, we have to generate a fake xc on MUX-panel
        Long seqNo = 1L;

//        List<String> siteList = new ArrayList<>();
        Set<String> sites = new HashSet<>();
        boolean split = false;
        for (CrossConnections xc : xcList) {
            CrossConnectionsBuilder xcBuilder = new CrossConnectionsBuilder();
//            String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
//            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpRef);
//            //NodeId nodeId = xc.getNodeRef();
//            String crossConnectionId = xc.getCrossConnectionId().getValue();
//            if (crossConnectionId.endsWith(MPO_SUFFIX)) {
//                crossConnectionId = realizeCrossConnection(xc);
//            }
            CrossConn crossConn = realizeCrossConnection(xc);
            String crossConnectionId = crossConn.crossConnId;
            String nodeId = crossConn.nodeId;
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections neXc = netconfTopology
                    .getXc(nodeId, crossConnectionId);
            if (neXc != null) {
                if (!convertYangXc2RouteXc(xcBuilder, neXc)) {
                    continue;
                }
            } else {
                if (crossConnectionId.endsWith(MPO_SUFFIX)) {
                    CrossConnections virtualizeXC = CMUX64Constructor.virtualizeTheXC(xc,
                            muxChannelTpId);
                    xcBuilder.fieldsFrom(virtualizeXC);
                } else {
                    xcBuilder.fieldsFrom(xc);
                }
            }
            xcBuilder.setNodeRef(xc.getNodeRef());
            xcBuilder.setSequence(seqNo);
            seqNo++;
            resultXcList.add(xcBuilder.build());
            String siteId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
            sites.add(siteId);
//            if (xcBuilder.getSourceTp().size() > 1 || xcBuilder.getDestinationTp().size() > 1) {
//                //this is separate point.
//                if (split) {
//                    //add last one site
//                    addSiteSequence(siteList, nodeId);
//                }
//                split = !split;
//            }
//            if (split) {
//                //add site when find split.
//                addSiteSequence(siteList, nodeId);
//            }
        }
        pb.setCrossConnections(resultXcList);
        pb.setSiteSequence(new ArrayList<>(sites));

        return pb.build();
    }

    /**
     * transfer the cross connection
     *
     * @param xc
     * @return
     */
    private CrossConn realizeCrossConnection(CrossConnections xc) {
        List<String> sourceTpIds = xc.getSourceTp().stream().map(tp -> tp.getTpRef().getValue())
                .collect(
                        Collectors.toList());
        List<String> destTpIds = xc.getDestinationTp().stream().map(tp -> tp.getTpRef().getValue())
                .collect(
                        Collectors.toList());
        List<String> tpIds = new ArrayList<>();
        tpIds.addAll(sourceTpIds);
        tpIds.addAll(destTpIds);
        //the inner cross connection  only start at one card and only one ne
        String equipId = PhysicalTpIdNamingRule.getEquipId(destTpIds.get(0));
        String nodeId = PhysicalTpIdNamingRule.getNodeId(destTpIds.get(0));
        String crossConnectionId = xc.getCrossConnectionId().getValue();

        if (isContainsMPOTpId(tpIds)) {
            Equipments equipments = netconfTopology.getEquipment(equipId);
            String equipType = equipments.getEquipType() == null ? equipments.getEquipTypeConfiged()
                    : equipments.getEquipType().name();
            if (equipType.equals(EquipType.CMUX64.name())) {
                //refactor the cross connection,MPO
                crossConnectionId = buildRealizeCrossConnectionForCMUX64(sourceTpIds, destTpIds);
            }
        }

        return CrossConn.builder().nodeId(nodeId).crossConnId(crossConnectionId).build();
    }

    private String buildRealizeCrossConnectionForCMUX64(List<String> sourceTpIds,
            List<String> destTpIds) {
        int sourceSize = sourceTpIds.size();
        int destSize = destTpIds.size();
        StringBuilder crossConnectionIdBuilder = new StringBuilder(XC_PREFIX);
        String mpoTp = null;
        if (sourceSize == 1 && destSize != 1) {
            mpoTp = sourceTpIds.get(0);
            for (String destTpId : destTpIds) {
                crossConnectionIdBuilder.append(destTpId).append(HYPHEN);
            }
        } else if (destSize == 1 && sourceSize != 1) {
            for (String destTpId : destTpIds) {
                crossConnectionIdBuilder.append(destTpId).append(HYPHEN);
            }
            mpoTp = destTpIds.get(0);
        }
        for (int i = 1; i <= MPO_PORT_SIZE; i++) {
            String mpoTpReal = mpoTp + i;
            crossConnectionIdBuilder.append(mpoTpReal).append(HYPHEN);
        }
        return crossConnectionIdBuilder.substring(0, crossConnectionIdBuilder.length() - 1);
    }

    private boolean isContainsMPOTpId(List<String> tpIds) {
        for (String tpId : tpIds) {
            if (tpId.endsWith(MPO_SUFFIX)) {
                return true;
            }
        }
        return false;

    }


    private List<RouteSequence> getRouteSequence(List<ExplicitRouteObjects> explicitRouteObjects,
            Long seq, List<CrossConnections> underlayXc, String source, String destination)
            throws Exception {
        log.debug("build detail route objects");
        List<RouteSequence> outputs = new LinkedList<>();
        if (explicitRouteObjects.isEmpty()) {
            return outputs;
        }
        ExplicitRouteObjects ero = explicitRouteObjects.get(0);
        if (ero == null) {
            return outputs;
        }

        boolean discardFlag = seq > 1L;

//        List<PathRouteObject> prl = ero.getPathRouteObject();
//        prl.sort(Comparator.comparing(ExplicitRouteHop::getIndex));
        List<PathRouteObject> prl = getDirectProList(ero.getPathRouteObject(), source,
                destination);
        boolean finalDiscardFlag = discardFlag;
        for (PathRouteObject pro : prl) {
            RouteSequenceBuilder rsb = new RouteSequenceBuilder();
            if (pro.getResourceType().getImplementedInterface().getName()
                    .equals(Tp.class.getName())) {
                if (finalDiscardFlag) {
                    if (pro.getIndex() == 1 || pro.getIndex() == prl.size()) {
                        continue;
                    }
                }
                rsb.setKey(new RouteSequenceKey(seq));
                rsb.setSequence(seq);
                rsb.setTopologyRef(pro.getTopologyRef());
                rsb.setResourceType(convertYangTpRoute2DetailRoute((Tp) pro.getResourceType()));
                outputs.add(rsb.build());
                seq++;
            } else if (pro.getResourceType().getImplementedInterface().getName()
                    .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class
                            .getName())) {
                String topoRef = pro.getTopologyRef().getValue();
                if (topoRef.equals(Constants.PHY_TOPO_KEY)) {
                    rsb.setKey(new RouteSequenceKey(seq));
                    rsb.setSequence(seq);
                    rsb.setTopologyRef(pro.getTopologyRef());
                    rsb.setResourceType(getYangPhyLinkDetailRoute(pro.getTopologyRef(),
                            (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pro
                                    .getResourceType(), source, destination));
                    outputs.add(rsb.build());
                    seq++;
                } else if (topoRef.equals(Constants.OCH_TOPO_KEY)) {
                    List<RouteSequence> ochRs = getYangOchLinkDetailRoute(pro.getTopologyRef(),
                            (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pro
                                    .getResourceType(), seq, underlayXc, source, destination);
                    outputs.addAll(ochRs);
                    seq += ochRs.size();
                } else if (topoRef.equals(Constants.SITE_TOPO_KEY)) {
                    List<RouteSequence> siteRs = getYangSiteLinkDetailRoute(pro.getTopologyRef(),
                            (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pro
                                    .getResourceType(), seq, underlayXc, source, destination);
                    outputs.addAll(siteRs);
                    seq += siteRs.size();
                }
            }

        }
        return outputs;
    }

//    private List<RouteSequence> getYangSiteLinkDetailRoute(TopologyId topologyId,
//            Link yangHop, Long seq, List<CrossConnections> underLayerXC) throws Exception {
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
//                netconfTopology.getSiteLink(yangHop.getLinkHop().getLinkRef().getValue());
//
//        if (topoLink == null) {
//            throw new Exception(
//                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
//                            + yangHop
//                            .getLinkHop().getLinkRef().getValue());
//        }
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink =
//                topoLink.getAugmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
//
//        Primary p = siteLink.getSite().getExplictRoute().getRoute().get(0).getPrimary();
//        underLayerXC.addAll(p.getCrossConnections());
//        List<RouteSequence> output = convertEro2Rs(p.getExplicitRouteObjects(), seq, underLayerXC);
//
//        Secondary s = siteLink.getSite().getExplictRoute().getRoute().get(0).getSecondary();
//        if (s != null && s.getExplicitRouteObjects() != null && !s.getExplicitRouteObjects()
//                .isEmpty()) {
//            pendingList.add(s);
//        }
//        return output;
//    }

    private List<RouteSequence> getYangSiteLinkDetailRoute(TopologyId topologyId,
            Link yangHop, Long seq, List<CrossConnections> underLayerXC, String source,
            String destination) throws Exception {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                netconfTopology.getSiteLink(yangHop.getLinkHop().getLinkRef().getValue());

        if (topoLink == null) {
            throw new Exception(
                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
                            + yangHop
                            .getLinkHop().getLinkRef().getValue());
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink =
                topoLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);

        Primary p = siteLink.getSite().getExplictRoute().getRoute().get(0).getPrimary();
        underLayerXC.addAll(p.getCrossConnections());
        List<RouteSequence> output = convertEro2Rs(p.getExplicitRouteObjects(), seq, underLayerXC,
                source, destination);

        Secondary s = siteLink.getSite().getExplictRoute().getRoute().get(0).getSecondary();
        if (s != null && s.getExplicitRouteObjects() != null && !s.getExplicitRouteObjects()
                .isEmpty()) {
            pendingList.add(s);
        }
        return output;
    }

    private List<RouteSequence> getYangOchLinkDetailRoute(TopologyId topologyId, Link yangHop,
            Long seq, List<CrossConnections> underlayXc) throws Exception {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                netconfTopology.getOchLink(
                        yangHop.getLinkHop().getLinkRef().getValue());

        if (topoLink == null) {
            throw new Exception(
                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
                            + yangHop
                            .getLinkHop().getLinkRef().getValue());
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLink =
                topoLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);

        List<RouteSequence> output = null;
        //
        //special for CMUX env. add XC for M?D? and MPO? on MUX-Panel card.
        //
        for (Route route : ochLink.getOch().getExplictRoute().getRoute()) {
            if (route.getPrimary() != null) {
                if (route.getPrimary().getCrossConnections() != null) {
                    underlayXc.addAll(route.getPrimary().getCrossConnections());
                }
                if (route.getPrimary().getExplicitRouteObjects() != null) {
                    output = convertEro2Rs(route.getPrimary().getExplicitRouteObjects(), seq,
                            underlayXc);
                }
            }
            if (route.getSecondary() != null) {
                if (route.getSecondary().getExplicitRouteObjects() != null && !route.getSecondary()
                        .getExplicitRouteObjects().isEmpty()) {
                    pendingList.add(route.getSecondary());
                }
            }
        }
        NeYangModel model = getNeYangmodel(topoLink.getSupportingLink(),
                topoLink.getSource().getSourceTp().getValue());
        underlayXc.addAll(generateMuxPanelXc(model, topoLink.getSupportingLink(),
                ochLink.getOch().getLowerFrequency(), ochLink.getOch().getUpperFrequency()));
        if (output == null) {
            output = new LinkedList<>();
        }

        return output;
    }

    private List<RouteSequence> getYangOchLinkDetailRoute(TopologyId topologyId, Link yangHop,
            Long seq, List<CrossConnections> underlayXc, String source, String destination)
            throws Exception {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                netconfTopology.getOchLink(
                        yangHop.getLinkHop().getLinkRef().getValue());

        if (topoLink == null) {
            throw new Exception(
                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
                            + yangHop
                            .getLinkHop().getLinkRef().getValue());
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLink =
                topoLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);

        List<RouteSequence> output = null;
        //
        //special for CMUX env. add XC for M?D? and MPO? on MUX-Panel card.
        //
        for (Route route : ochLink.getOch().getExplictRoute().getRoute()) {
            if (route.getPrimary() != null) {
                if (route.getPrimary().getCrossConnections() != null) {
                    underlayXc.addAll(route.getPrimary().getCrossConnections());
                }
                if (route.getPrimary().getExplicitRouteObjects() != null) {
                    output = convertEro2Rs(route.getPrimary().getExplicitRouteObjects(), seq,
                            underlayXc, source, destination);
                }
            }
            if (route.getSecondary() != null) {
                if (route.getSecondary().getExplicitRouteObjects() != null && !route.getSecondary()
                        .getExplicitRouteObjects().isEmpty()) {
                    pendingList.add(route.getSecondary());
                }
            }
        }

        NeYangModel model = getNeYangmodel(topoLink.getSupportingLink(),
                topoLink.getSource().getSourceTp().getValue());
        underlayXc.addAll(generateMuxPanelXc(model, topoLink.getSupportingLink(),
                ochLink.getOch().getLowerFrequency(), ochLink.getOch().getUpperFrequency()));
        if (output == null) {
            output = new LinkedList<>();
        }

        return output;
    }

    private ResourceType getYangPhyLinkDetailRoute(TopologyId topologyRef, Link yangHop)
            throws Exception {
        String linkId = yangHop.getLinkHop().getLinkRef().getValue();
        LinkHopBuilder lb = new LinkHopBuilder();
        if (linkId.endsWith(MPO_SUFFIX)) {
            lb = CMUX64Constructor.generateMpoLinkRouteDetail(muxChannelTpId, lb, linkId);
        } else {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                    netconfTopology.getPhyLink(
                            yangHop.getLinkHop().getLinkRef().getValue());

            if (topoLink == null) {
                throw new Exception(
                        "cannot find required phy link. topoId:" + topologyRef.getValue()
                                + ", linkId:"
                                + yangHop
                                .getLinkHop().getLinkRef().getValue());
            }

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLink =
                    topoLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);

            lb.fieldsFrom(topoLink);
            lb.fieldsFrom(phyLink);
            if (muxChannelTpId == null || muxChannelTpId.isEmpty()) {
                //muxChannelTpId = null happen on get route of site link. otherwise muxChannelTpId must be not null.
            } else {
                if (yangHop.getLinkHop().getLinkRef().getValue().contains("LAG")) {
                    DestinationBuilder db = new DestinationBuilder()
                            .setDestNode(topoLink.getDestination().getDestNode())
                            .setDestTp(new TpId(
                                    LAG2MPO(topoLink.getDestination().getDestTp().getValue())));
                    SourceBuilder sb = new SourceBuilder()
                            .setSourceNode(topoLink.getSource().getSourceNode())
                            .setSourceTp(
                                    new TpId(LAG2MPO(topoLink.getSource().getSourceTp()
                                            .getValue())));

                    lb.setDestination(db.build()).setSource(sb.build());
                }
            }
        }

//        if (muxChannelTpId == null || muxChannelTpId.isEmpty()) {
//            //muxChannelTpId = null happen on get route of site link. otherwise muxChannelTpId must be not null.
//        } else {
//            if (yangHop.getLinkHop().getLinkRef().getValue().contains("LAG")) {
//                DestinationBuilder db = new DestinationBuilder()
//                        .setDestNode(topoLink.getDestination().getDestNode())
//                        .setDestTp(new TpId(
//                                LAG2MPO(topoLink.getDestination().getDestTp().getValue())));
//                SourceBuilder sb = new SourceBuilder()
//                        .setSourceNode(topoLink.getSource().getSourceNode())
//                        .setSourceTp(
//                                new TpId(LAG2MPO(topoLink.getSource().getSourceTp().getValue())));
//
//                lb.setDestination(db.build()).setSource(sb.build());
//            }
//        }

        ResourceType rt = new LinkBuilder().setLinkHop(lb.build()).build();
        return rt;

    }


    private ResourceType getYangPhyLinkDetailRoute(TopologyId topologyRef, Link yangHop,
            String source, String destination)
            throws Exception {
        String linkId = yangHop.getLinkHop().getLinkRef().getValue();
        LinkHopBuilder lb = new LinkHopBuilder();
        if (linkId.endsWith(MPO_SUFFIX)) {
            lb = CMUX64Constructor.generateMpoLinkRouteDetail(muxChannelTpId, lb, linkId);
        } else {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                    netconfTopology.getPhyLink(
                            yangHop.getLinkHop().getLinkRef().getValue());

            if (topoLink == null) {
                throw new Exception(
                        "cannot find required phy link. topoId:" + topologyRef.getValue()
                                + ", linkId:"
                                + yangHop
                                .getLinkHop().getLinkRef().getValue());
            }

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLink =
                    topoLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);

            lb.fieldsFrom(topoLink);
            lb.fieldsFrom(phyLink);
            if (muxChannelTpId == null || muxChannelTpId.isEmpty()) {
                //muxChannelTpId = null happen on get route of site link. otherwise muxChannelTpId must be not null.
            } else {
                if (yangHop.getLinkHop().getLinkRef().getValue().contains("LAG")) {
                    DestinationBuilder db = new DestinationBuilder()
                            .setDestNode(topoLink.getDestination().getDestNode())
                            .setDestTp(new TpId(
                                    LAG2MPO(topoLink.getDestination().getDestTp().getValue())));
                    SourceBuilder sb = new SourceBuilder()
                            .setSourceNode(topoLink.getSource().getSourceNode())
                            .setSourceTp(
                                    new TpId(LAG2MPO(topoLink.getSource().getSourceTp()
                                            .getValue())));

                    lb.setDestination(db.build()).setSource(sb.build());
                }
            }
        }

//        if (muxChannelTpId == null || muxChannelTpId.isEmpty()) {
//            //muxChannelTpId = null happen on get route of site link. otherwise muxChannelTpId must be not null.
//        } else {
//            if (yangHop.getLinkHop().getLinkRef().getValue().contains("LAG")) {
//                DestinationBuilder db = new DestinationBuilder()
//                        .setDestNode(topoLink.getDestination().getDestNode())
//                        .setDestTp(new TpId(
//                                LAG2MPO(topoLink.getDestination().getDestTp().getValue())));
//                SourceBuilder sb = new SourceBuilder()
//                        .setSourceNode(topoLink.getSource().getSourceNode())
//                        .setSourceTp(
//                                new TpId(LAG2MPO(topoLink.getSource().getSourceTp().getValue())));
//
//                lb.setDestination(db.build()).setSource(sb.build());
//            }
//        }

        ResourceType rt = new LinkBuilder().setLinkHop(lb.build()).build();
        return rt;

    }

//    private void generateMpoLinkRouteDetail(LinkHopBuilder lb, String linkId) {
//        lb.setLinkId(LinkId.getDefaultInstance(linkId));
//        SiteLinkDto siteLinkDto = SiteLinkIdNamingRule.extractSiteLinkDetail(linkId);
//
//        DestinationBuilder db = new DestinationBuilder()
//                .setDestNode(NodeId.getDefaultInstance(siteLinkDto.getDestinationNodeId()))
//                .setDestTp(new TpId(siteLinkDto.getDestinationTp()));
//        SourceBuilder sb = new SourceBuilder()
//                .setSourceNode(NodeId.getDefaultInstance(siteLinkDto.getSourceNodeId()))
//                .setSourceTp(
//                        new TpId(siteLinkDto.getSourceTp()));
//        lb.setDestination(db.build()).setSource(sb.build());
//    }

//    private ResourceType getYangTpResourceRouteDetail(Tp yangHop) throws Exception {
//        String tpId = yangHop.getTpHop().getTpRef().getValue();
//        String[] ids = tpId.split(POUND);
//        if (ids.length != 4) {
//            throw new Exception(
//                    "tp ID format must be ");
//        }
//
//        String siteId = ids[0];
//        String neId = ids[0] + POUND + ids[1];
//        String equipId = ids[0] + POUND + ids[1] + POUND + ids[2];
//
//        Node phyTopoNode = netconfTopology.getPhyNode(neId);
//        Node filteredTopoNode = filterTpData(phyTopoNode, tpId);
//        Node1 filteredNe = filterEquipData(phyTopoNode.getAugmentation(Node1.class), equipId, tpId);
//
//        PhyNodeBuilder pb = new PhyNodeBuilder();
//        pb.fieldsFrom(filteredTopoNode);
//        pb.fieldsFrom(filteredNe);
//        Node siteTopoNode = netconfTopology.getSiteNode(siteId);
//        Node outputSiteTopoNode = filterSiteTopoNode(siteTopoNode);
//        SiteNodeBuilder sb = new SiteNodeBuilder();
//        sb.fieldsFrom(outputSiteTopoNode);
//        sb.fieldsFrom(outputSiteTopoNode.getAugmentation(
//                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class));
//
//        PhyTpBuilder tb = new PhyTpBuilder();
//        tb.fieldsFrom(filteredTopoNode.getTerminationPoint().get(0));
//        tb.fieldsFrom(
//                filteredTopoNode.getTerminationPoint().get(0)
//                        .getAugmentation(TerminationPoint1.class));
//
//        if (tb.getPhysical().getPortType().equals(PortType.MUXChannel)) {
//            muxChannelTpId = tb.getTpId().getValue();
//        }
//
//        if (tb.getPhysical().getPortType().equals(PortType.OPCMPOLAG)) {
//            if (muxChannelTpId == null || muxChannelTpId.isEmpty()) {
//                //muxChannelTpId = null happen on get route of site link. otherwise muxChannelTpId must be not null.
//            } else {
//                //in this case the port should change to related MPOn (n=1..8)
//                String mpoId = LAG2MPO(tpId);
//                TerminationPoint tp = netconfTopology
//                        .getTerminationPoint(Constants.PHY_TOPO_KEY, neId, mpoId);
//                if (tp == null) {
//                    throw new Exception(
//                            "cannot find out the MPO TP " + mpoId);
//                }
//
//                tb = new PhyTpBuilder();
//                tb.fieldsFrom(tp);
//                tb.fieldsFrom(tp.getAugmentation(TerminationPoint1.class));
//            }
//        }
//        ResourceType rt = new TpBuilder()
//                .setTpHop(new TpHopBuilder()
//                        .setSiteNode(sb.build())
//                        .setPhyNode(pb.build())
//                        .setPhyTp(tb.build())
//                        .build())
//                .build();
//        return rt;
//    }

    private boolean convertYangXc2RouteXc(CrossConnectionsBuilder xcBuilder,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections neXc)
            throws Exception {
        xcBuilder.fieldsFrom(neXc);
        xcBuilder.setSourceTp(distinctSourceTpList(neXc.getSourceTp()));
        xcBuilder.setDestinationTp(distinctDesTpList(neXc.getDestinationTp()));
        if (muxChannelTpId == null || muxChannelTpId.isEmpty()) {
            //muxChannelTpId = null happen on get route of site link. otherwise muxChannelTpId must be not null.
        } else {
            //this is one tunnel, if the tunnel is over on Flex-grid site link, we will do some trick.
            String[] ids = muxChannelTpId.split(POUND);

            if (xcBuilder.getCrossConnectionId().getValue().contains(ids[ids.length - 1]) &&
                    xcBuilder.getCrossConnectionId().getValue().contains("-LINE")) {
                //this XC crosses NE, we discard it when display
                return false;
            }
            if (xcBuilder.getCrossConnectionId().getValue().contains("LAG")) {
                if (xcBuilder.getSourceTp().size() > 0) {
                    String tpId = xcBuilder.getSourceTp().get(0).getTpRef().getValue();
                    if (tpId.contains("LAG")) {

                        List<SourceTp> tpList = new LinkedList<>();
                        tpList.add(new SourceTpBuilder().setTpRef(new TpId(LAG2MPO(tpId))).build());
                        xcBuilder.setSourceTp(tpList);
                    }
                } else if (xcBuilder.getDestinationTp().size() > 0) {
                    String tpId = xcBuilder.getDestinationTp().get(0).getTpRef().getValue();
                    if (tpId.contains("LAG")) {

                        List<DestinationTp> tpList = new LinkedList<>();
                        tpList.add(new DestinationTpBuilder().setTpRef(new TpId(LAG2MPO(tpId)))
                                .build());
                        xcBuilder.setDestinationTp(tpList);
                    }
                }
            }
        }

        return true;
    }

    private List<DestinationTp> distinctDesTpList(List<DestinationTp> destinationTps) {
        return destinationTps.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(
                        () -> new TreeSet<>(Comparator.comparing(tp -> tp.getTpRef().getValue()))),
                ArrayList::new));
    }

    private List<SourceTp> distinctSourceTpList(List<SourceTp> sourceTps) {
        return sourceTps.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(
                        () -> new TreeSet<>(Comparator.comparing(tp -> tp.getTpRef().getValue()))),
                ArrayList::new));
    }


    private String LAG2MPO(String tpId) throws Exception {
        String[] ids = tpId.split(POUND);
        if (ids.length != 4) {
            throw new Exception(
                    "tp ID format must be ");
        }
        String equipId = ids[0] + POUND + ids[1] + POUND + ids[2];
        String[] channelTpIds = muxChannelTpId.split(POUND);
        String[] channelKeys = channelTpIds[channelTpIds.length - 1].split("D"); //M1D1
        if (channelKeys.length != 2) {
            throw new Exception(
                    "cannot find out channelID on " + muxChannelTpId);
        }

        String channelId = channelKeys[1];
        Integer id = (Integer.parseInt(channelId) - 1) / 8 + 1;
        String[] slotIds = ids[2].split("-"); //ids[2] is equip part,  such as MUXPANEL-1-50
        String mpoId =
                equipId + POUND + "PORT" + "-" + slotIds[1] + "-" + slotIds[2] + "-" + "MPO" + id;
        return mpoId;
    }


    private void addSiteSequence(List<String> siteList, String nodeId) {
        log.debug("siteList {}, nodeId{}", siteList, nodeId);
        String[] ids = nodeId.split(POUND);
        boolean found = false;
        for (String siteId : siteList) {
            if (siteId.equals(ids[0])) {
                found = true;
                break;
            }
        }
        if (!found) {
            siteList.add(ids[0]);
        }
    }

    private ResourceType convertYangTpRoute2DetailRoute(Tp yangHop) throws Exception {
        String tpId = yangHop.getTpHop().getTpRef().getValue();
        String[] ids = tpId.split(POUND);
        if (ids.length != 4) {
            throw new Exception(
                    "tp ID format must be ");
        }

        String siteId = ids[0];
        String neId = ids[0] + POUND + ids[1];
        String equipId = ids[0] + POUND + ids[1] + POUND + ids[2];

        Node phyTopoNode = netconfTopology.getNeNode(neId);
        Node filteredTopoNode;
        if (tpId.endsWith(MPO_SUFFIX)) {
            String mpoId = CMUX64Constructor.getMPOTpId(tpId, muxChannelTpId);
            if (muxChannelTpId == null) {
                mpoId = mpoId + "1";
            }
            filteredTopoNode = filterTpData(phyTopoNode, mpoId);
        } else {
            filteredTopoNode = filterTpData(phyTopoNode, tpId);
        }

        Node1 filteredNe = filterEquipData(phyTopoNode.getAugmentation(Node1.class), equipId, tpId);

        PhyNodeBuilder pb = new PhyNodeBuilder();
        pb.fieldsFrom(filteredTopoNode);
        pb.fieldsFrom(filteredNe);

        Node siteTopoNode = netconfTopology.getSiteNode(siteId);
        Node outputSiteTopoNode = filterSiteTopoNode(siteTopoNode);
        SiteNodeBuilder sb = new SiteNodeBuilder();
        sb.fieldsFrom(outputSiteTopoNode);
        sb.fieldsFrom(outputSiteTopoNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class));

        PhyTpBuilder tb = new PhyTpBuilder();
        tb.fieldsFrom(filteredTopoNode.getTerminationPoint().get(0));
        tb.fieldsFrom(
                filteredTopoNode.getTerminationPoint().get(0)
                        .getAugmentation(TerminationPoint1.class));

        if (tb.getPhysical().getPortType().equals(PortType.MUXChannel)) {
            muxChannelTpId = tb.getTpId().getValue();
        }
//
//        if (tb.getPhysical().getPortType().equals(PortType.OPCMPOLAG)) {
//            if (muxChannelTpId == null || muxChannelTpId.isEmpty()) {
//                //muxChannelTpId = null happen on get route of site link. otherwise muxChannelTpId must be not null.
//            } else {
//                //in this case the port should change to related MPOn (n=1..8)
//                String mpoId = LAG2MPO(tpId);
//                TerminationPoint tp = netconfTopology
//                        .getTerminationPoint(Constants.PHY_TOPO_KEY, neId, mpoId);
//                if (tp == null) {
//                    throw new Exception(
//                            "cannot find out the MPO TP " + mpoId);
//                }
//
//                tb = new PhyTpBuilder();
//                tb.fieldsFrom(tp);
//                tb.fieldsFrom(tp.getAugmentation(TerminationPoint1.class));
//            }
//        }
        ResourceType rt = new TpBuilder()
                .setTpHop(new TpHopBuilder()
                        .setSiteNode(sb.build())
                        .setPhyNode(pb.build())
                        .setPhyTp(tb.build())
                        .build())
                .build();
        return rt;
    }


    private Node filterSiteTopoNode(Node siteTopoNode) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 siteNode = filterSiteData(
                siteTopoNode);
        NodeBuilder nb = new NodeBuilder(siteTopoNode);
        nb.setSupportingNode(null);
        nb.addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                siteNode);

        return nb.build();
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 filterSiteData(
            Node siteTopoNode) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 yangSite =
                siteTopoNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);

        SiteBuilder sb = new SiteBuilder(yangSite.getSite());
        sb.setSupportingRack(null);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder nb = new
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder();
        nb.setSite(sb.build());
        return nb.build();
    }

    private Node filterTpData(Node topoNode, String tpId) throws Exception {
        TerminationPoint matchedTp = null;
        for (TerminationPoint tp : topoNode.getTerminationPoint()) {
            if (tp.getTpId().getValue().equals(tpId)) {
                matchedTp = tp;
                break;
            }
        }
        if (matchedTp == null) {
            throw new Exception(
                    "a route TP cannot find out related ne tp list with " + tpId);
        }
        if (tpId.contains(MPO_SUFFIX) && muxChannelTpId == null) {
            matchedTp = CMUX64Constructor.virtualizeMPOTP(matchedTp, tpId);
        }
        List<TerminationPoint> tpList = new LinkedList<>();
        tpList.add(matchedTp);

        NodeBuilder output = new NodeBuilder(topoNode);
        output.setTerminationPoint(tpList);
        return output.build();
    }

    private Node1 filterEquipData(Node1 ne, String equipId, String tpId) throws Exception {
        Equipments machedEq = null;
        for (Equipments eq : ne.getPhysical().getEquipments()) {
            if (eq.getEquipmentId().equals(equipId)) {
                machedEq = eq;
                break;
            }
        }
        if (machedEq == null) {
            throw new Exception(
                    "a route TP cannot find out related equipment with " + equipId);
        }
        List<Equipments> eqList = new LinkedList<>();
        eqList.add(machedEq);

        Node1Builder output = new Node1Builder();
        output.setPhysical(
                new PhysicalBuilder(ne.getPhysical())
                        .setCrossConnections(null)
                        .setInternalLinks(null)
                        .setEquipments(eqList)
                        .setOCMGripGroups(null)
                        .setSystem(null)
                        .build());
        return output.build();
    }

//    private ResourceType convertYangPhyLinkRoute2DetailRoute(TopologyId topologyId, Link yangHop)
//            throws Exception {
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
//                netconfTopology.getPhyLink(yangHop.getLinkHop().getLinkRef().getValue());
//
//        if (topoLink == null) {
//            throw new Exception(
//                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
//                            + yangHop
//                            .getLinkHop().getLinkRef().getValue());
//        }
//        LinkHopBuilder lb = new LinkHopBuilder();
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLink =
//                topoLink.getAugmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
//
//        lb.fieldsFrom(topoLink);
//        lb.fieldsFrom(phyLink);
//        if (muxChannelTpId == null || muxChannelTpId.isEmpty()) {
//            //muxChannelTpId = null happen on get route of site link. otherwise muxChannelTpId must be not null.
//        } else {
//            if (yangHop.getLinkHop().getLinkRef().getValue().contains("LAG")) {
//                DestinationBuilder db = new DestinationBuilder()
//                        .setDestNode(topoLink.getDestination().getDestNode())
//                        .setDestTp(new TpId(
//                                LAG2MPO(topoLink.getDestination().getDestTp().getValue())));
//                SourceBuilder sb = new SourceBuilder()
//                        .setSourceNode(topoLink.getSource().getSourceNode())
//                        .setSourceTp(
//                                new TpId(LAG2MPO(topoLink.getSource().getSourceTp().getValue())));
//
//                lb.setDestination(db.build()).setSource(sb.build());
//            }
//        }
//
//        ResourceType rt = new LinkBuilder().setLinkHop(lb.build()).build();
//        return rt;
//    }

    private List<RouteSequence> convertYangOchLinkRoute2DetailRoute(TopologyId topologyId,
            Link yangHop, Long seq, List<CrossConnections> underLayerXC) throws Exception {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                netconfTopology.getOchLink(
                        yangHop.getLinkHop().getLinkRef().getValue());

        if (topoLink == null) {
            throw new Exception(
                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
                            + yangHop
                            .getLinkHop().getLinkRef().getValue());
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLink =
                topoLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);

        List<RouteSequence> output = null;
        //
        //special for CMUX env. add XC for M?D? and MPO? on MUX-Panel card.
        //
        for (Route route : ochLink.getOch().getExplictRoute().getRoute()) {
            if (route.getPrimary() != null) {
                if (route.getPrimary().getCrossConnections() != null) {
                    underLayerXC.addAll(route.getPrimary().getCrossConnections());
                }
                if (route.getPrimary().getExplicitRouteObjects() != null) {
                    output = convertEro2Rs(route.getPrimary().getExplicitRouteObjects(), seq,
                            underLayerXC);
                }
            }
            if (route.getSecondary() != null) {
                if (route.getSecondary().getExplicitRouteObjects() != null && !route.getSecondary()
                        .getExplicitRouteObjects().isEmpty()) {
                    pendingList.add(route.getSecondary());
                }
            }
        }
        NeYangModel model = getNeYangmodel(topoLink.getSupportingLink(),
                topoLink.getSource().getSourceTp().getValue());
        underLayerXC.addAll(generateMuxPanelXc(model, topoLink.getSupportingLink(),
                ochLink.getOch().getLowerFrequency(), ochLink.getOch().getUpperFrequency()));
        if (output == null) {
            output = new LinkedList<>();
        }

        return output;
    }

    private List<RouteSequence> convertYangOchLinkRoute2DetailRoute(TopologyId topologyId,
            Link yangHop, Long seq, List<CrossConnections> underLayerXC, String source,
            String destination) throws Exception {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                netconfTopology.getOchLink(
                        yangHop.getLinkHop().getLinkRef().getValue());

        if (topoLink == null) {
            throw new Exception(
                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
                            + yangHop
                            .getLinkHop().getLinkRef().getValue());
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLink =
                topoLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);

        List<RouteSequence> output = null;
        //
        //special for CMUX env. add XC for M?D? and MPO? on MUX-Panel card.
        //
        for (Route route : ochLink.getOch().getExplictRoute().getRoute()) {
            if (route.getPrimary() != null) {
                if (route.getPrimary().getCrossConnections() != null) {
                    underLayerXC.addAll(route.getPrimary().getCrossConnections());
                }
                if (route.getSecondary().getExplicitRouteObjects() != null) {
                    output = convertEro2Rs(route.getSecondary().getExplicitRouteObjects(), seq,
                            underLayerXC, source, destination);
                }
            }
            if (route.getSecondary() != null) {
                if (route.getSecondary().getExplicitRouteObjects() != null && !route.getSecondary()
                        .getExplicitRouteObjects().isEmpty()) {
                    pendingList.add(route.getSecondary());
                }
            }
        }
        NeYangModel model = getNeYangmodel(topoLink.getSupportingLink(),
                topoLink.getSource().getSourceTp().getValue());
        underLayerXC.addAll(generateMuxPanelXc(model, topoLink.getSupportingLink(),
                ochLink.getOch().getLowerFrequency(), ochLink.getOch().getUpperFrequency()));
        if (output == null) {
            output = new LinkedList<>();
        }

        return output;
    }

    private NeYangModel getNeYangmodel(List<SupportingLink> supportingLink, String source) {
        Optional<SupportingLink> op = supportingLink.stream()
                .filter(link -> PhysicalLinkIdNamingRule.isOsLink(link.getLinkRef().getValue()))
                .findAny();
        SupportingLink osLink = op.get();
        Node node = netconfTopology.getNeNode(
                PhysicalLinkIdNamingRule.getNodeZId(osLink.getLinkRef().getValue()));
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getNodeType().equals(NodeType.OPC4) || nodeAttr.getNodeType()
                .equals(NodeType.OD)) {
            return NeYangModel.getModel(nodeAttr);
        } else {
            node = netconfTopology.getNeNode(
                    PhysicalLinkIdNamingRule.getNodeAId(osLink.getLinkRef().getValue()));
            nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            if (nodeAttr.getNodeType().equals(NodeType.OPC4) || nodeAttr.getNodeType()
                    .equals(NodeType.OD)) {
                return NeYangModel.getModel(nodeAttr);
            }
        }
        return NeYangModel.Tencent;
    }

    private List<CrossConnections> generateMuxPanelXc(NeYangModel model,
            List<SupportingLink> supportingLinks,
            FrequencyType lower, FrequencyType upper) throws Exception {
        MuxCardPortFormatting formatting = new MuxCardPortFormatting(model);
        List<CrossConnections> newXcs = new LinkedList<>();
        Long index = 0L;
        for (SupportingLink slink : supportingLinks) {
            Pattern pattern = Pattern.compile(formatting.getPortMatchingRegex());
            Matcher matcher = pattern.matcher(slink.getLinkRef().getValue());
            if (matcher.find()) {
                index++;
                log.info("slink.getLinkRef().getValue() is {}", slink.getLinkRef().getValue());
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link ntLink =
                        netconfTopology
                                .getPhyLink(slink.getLinkRef().getValue());

                TpId srcTp = ntLink.getSource().getSourceTp();
                TpId dstTp = ntLink.getDestination().getDestTp();
                matcher = pattern.matcher(srcTp.getValue());
                if (matcher.find()) {
                    dstTp = new TpId(LAG2MPO(srcTp.getValue()));
                } else {
                    matcher = pattern.matcher(dstTp.getValue());
                    if (matcher.find()) {
                        srcTp = new TpId(LAG2MPO(dstTp.getValue()));
                    }
                }
                String frequency =
                        "/frequency=" + lower.getValue().toString() + "," + upper.getValue()
                                .toString();
                long centorFrequency =
                        (lower.getValue().longValue() + upper.getValue().longValue()) / 2L;
                Uri xcId = new Uri(
                        "XC-" + srcTp.getValue() + "/frequency=" + centorFrequency + "-" + dstTp
                                .getValue() + "/frequency=" + centorFrequency);

                List<SourceTp> srcTps = new LinkedList<>();
                srcTps.add(new SourceTpBuilder()
                        .setTpRef(srcTp)
                        .setSlot(frequency)
                        .build());

                List<DestinationTp> dstTps = new LinkedList<>();
                dstTps.add(new DestinationTpBuilder()
                        .setTpRef(dstTp)
                        .setSlot(frequency)
                        .build());
                NodeId refNodeId = NodeId.getDefaultInstance(
                        PhysicalTpIdNamingRule.getNodeId(srcTps.get(0).getTpRef().getValue()));
                CrossConnectionsBuilder cb = new CrossConnectionsBuilder()
                        .setNodeRef(refNodeId)
                        .setSourceTp(srcTps)
                        .setDestinationTp(dstTps)
                        .setAdminState(AdminStatus.Up)
                        .setDirection(LinkDirection.Bidirection)
                        .setFixed(true)
                        .setImplementState(ImplementState.Implement)
                        .setNodeRef(ntLink.getSource().getSourceNode())
                        .setOperationalState(OperStatus.Up)
                        .setCrossConnectionId(xcId)
                        .setKey(new CrossConnectionsKey(index));

                newXcs.add(cb.build());
            }
        }
        return newXcs;
    }

    private List<RouteSequence> convertYangSiteLinkRoute2DetailRoute(TopologyId topologyId,
            Link yangHop, Long seq, List<CrossConnections> underLayerXC) throws Exception {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                netconfTopology.getSiteLink(
                        yangHop.getLinkHop().getLinkRef().getValue());

        if (topoLink == null) {
            throw new Exception(
                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
                            + yangHop
                            .getLinkHop().getLinkRef().getValue());
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink =
                topoLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);

        Primary p = siteLink.getSite().getExplictRoute().getRoute().get(0).getPrimary();
        underLayerXC.addAll(p.getCrossConnections());
        List<RouteSequence> output = convertEro2Rs(p.getExplicitRouteObjects(), seq, underLayerXC);

        Secondary s = siteLink.getSite().getExplictRoute().getRoute().get(0).getSecondary();
        if (s != null && s.getExplicitRouteObjects() != null && !s.getExplicitRouteObjects()
                .isEmpty()) {
            pendingList.add(s);
//      RouteInfoBuilder rib = getSecondaryRouteSequence(s.getExplicitRouteObjects(), s.getCrossConnections(), 1L);
//      rib.setIndex((short) (routeInfo.get(routeInfo.size() - 1).getIndex() + 1));
//      rib.setKey(new RouteInfoKey(rib.getIndex()));
//      routeInfo.add(rib.build());
        }
        return output;
    }

    private List<RouteSequence> convertYangSiteLinkRoute2DetailRoute(TopologyId topologyId,
            Link yangHop, Long seq, List<CrossConnections> underLayerXC, String source,
            String destination) throws Exception {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link topoLink =
                netconfTopology.getSiteLink(
                        yangHop.getLinkHop().getLinkRef().getValue());

        if (topoLink == null) {
            throw new Exception(
                    "cannot find required link. topoId:" + topologyId.getValue() + ", linkId:"
                            + yangHop
                            .getLinkHop().getLinkRef().getValue());
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink =
                topoLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);

        Primary p = siteLink.getSite().getExplictRoute().getRoute().get(0).getPrimary();
        underLayerXC.addAll(p.getCrossConnections());
        List<RouteSequence> output = convertEro2Rs(p.getExplicitRouteObjects(), seq, underLayerXC,
                source, destination);

        Secondary s = siteLink.getSite().getExplictRoute().getRoute().get(0).getSecondary();
        if (s != null && s.getExplicitRouteObjects() != null && !s.getExplicitRouteObjects()
                .isEmpty()) {
            pendingList.add(s);
//      RouteInfoBuilder rib = getSecondaryRouteSequence(s.getExplicitRouteObjects(), s.getCrossConnections(), 1L);
//      rib.setIndex((short) (routeInfo.get(routeInfo.size() - 1).getIndex() + 1));
//      rib.setKey(new RouteInfoKey(rib.getIndex()));
//      routeInfo.add(rib.build());
        }
        return output;
    }

    public List<Node> getAllSites(ExplictRoute explictRoute) throws Exception {
        log.debug("start extract Site from route list");
        Set<Node> rst = new HashSet<>();
        for (Route route : explictRoute.getRoute()) {
            rst.addAll(getAllSitesOnEro(route.getPrimary().getExplicitRouteObjects()));
            if (route.getSecondary() != null) {
                rst.addAll(getAllSitesOnEro(route.getSecondary().getExplicitRouteObjects()));
            }
        }
        List<Node> output = new LinkedList<>();
        output.addAll(rst);
        return output;
    }

    private Collection<Node> getAllSitesOnEro(List<ExplicitRouteObjects> explicitRouteObjects)
            throws Exception {
        log.debug("start extract Site from ERO.");
        Set<Node> output = new HashSet<>();
        if (explicitRouteObjects != null) {
            for (ExplicitRouteObjects ero : explicitRouteObjects) {
                if (ero.getPathRouteObject() != null) {
                    for (PathRouteObject pro : ero.getPathRouteObject()) {
                        if (pro.getResourceType().getImplementedInterface().getName()
                                .equals(Tp.class.getName())) {
                            Tp tpHop = (Tp) pro.getResourceType();
                            String tpId = tpHop.getTpHop().getTpRef().getValue();
                            String[] ids = tpId.split(POUND);
                            if (ids.length != 4) {
                                throw new Exception(

                                        "tp ID format must be ");
                            }

                            String siteId = ids[0];
                            Node siteTopoNode = netconfTopology.getSiteNode(siteId);
                            if (siteTopoNode != null) {
                                output.add(siteTopoNode);
                            }
                        }
                    }
                }
            }
        }

        return output;
    }

    public void filterOutOTS(List<ExplicitRouteObjects> eroList,
            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> otsLinkList,
            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> specOtsList) {
        if (eroList == null) {
            return;
        }

        for (ExplicitRouteObjects ero : eroList) {
            if (ero.getExplicitRouteUsage().equals(RouteUsageInclude.class)) {
                List<PathRouteObject> proList = ero.getPathRouteObject();
                for (PathRouteObject pro : proList) {
                    if (pro.getResourceType().getImplementedInterface().getName()
                            .equals(Link.class.getName())) {
                        LinkHop linkHop = ((Link) pro.getResourceType()).getLinkHop();
                        for (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link otsLink : otsLinkList) {
                            if (otsLink.getLinkId().getValue()
                                    .equals(linkHop.getLinkRef().getValue())) {
                                specOtsList.add(otsLink);
                            }
                        }
                    }
                }
            }
        }
    }

    @Data
    @Builder
    public static class CrossConn implements Serializable {

        private String nodeId;

        private String crossConnId;


    }
}
