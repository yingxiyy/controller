/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.constructs.RouteDisplayConstructor;
import net.flex.dci.otn.controller.nms.constructs.YangRouteConstructor;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.nms.handler.impl.INMSOperations;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.PhyLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteTunnel;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RouteDisplayInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.output.RouteDisplayInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.TpHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/8
 */
@Slf4j
@Component
public class RouteHandler extends AbstractBaseHandler {

    private final ConcurrentHashMap<String, INMSOperations> handlerMap = new ConcurrentHashMap<>();

    @Autowired
    private RouteRetriever routeRetriever;

    public RouteHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);

        initHandlerMap();
    }


    private void initHandlerMap() {
        handlerMap.put(Constants.SITE_TOPO_KEY, new SiteLink(this.netconfTopology));
        handlerMap.put(Constants.TUNNEL_TOPO_KEY, new SiteTunnel(this.netconfTopology));
        handlerMap.put(Constants.PHY_TOPO_KEY, new PhyLink(this.netconfTopology));
    }


    @Override
    public List<RouteDisplayInfo> routeDisplay(RouteDisplayInput input) throws Exception {
        log.debug("start routeDisplay");
        RouteInfoDto route = getRouteInfo(input.getTopologyRef(), input.getLinkRef(),
                input.getTunnelRef());
        if (route == null || route.getRoutes().isEmpty()) {
            throw new Exception(
                    "the link hasn't route info.");
        }
        List<RouteInfo> routeInfos = transfer2DetailRoute(route);
        RouteDisplayConstructor rd = new RouteDisplayConstructor(netconfTopology,
                input.getTopologyRef(), input.getLinkRef(),
                input.getTunnelRef(), routeInfos);

        return rd.extractedRouteInfo();
    }

    @Override
    public List<RouteInfo> getRoute(GetRouteInput input)
            throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        LinkId linkRef = input.getLinkRef();
        Uri tunnelRef = input.getTunnelRef();
//        return transfer2DetailRoute(getRoute(topologyRef, linkRef, tunnelRef));
//        Map<String, LinkRoute> displayRouteMap = SpringBeanFinder.getBeansOfType(
//                LinkRoute.class);
//        List<RouteInfo> routes = new ArrayList<>();
//        for (LinkRoute displayRoute : displayRouteMap.values()) {
//            List<RouteInfo> displayRouteInfo = displayRoute.getRoute(input);
//            routes.addAll(displayRouteInfo);
//        }
//        return routes;
        List<RouteInfo> routeInfoList = transfer2DetailRoute(
                getRouteInfo(topologyRef, linkRef, tunnelRef));
        if (tunnelRef != null) {
            //特殊处理TPC设备直接， 网管数据库中表达的还是Tunnel基于siteLink (判断标准是OPC网元的属性virtual)
            //这里删除多余的路由数据
            //cPort, lPort, lLink---OPC..................OPC---lLink, lPort， cPort
            //XC 只保留TPC设备上的ODU交叉
            //and input value is sorted
            //in tunnel's RouteSequence, the 4th must be OPC NE

            RouteInfo route = routeInfoList.get(0);
            RouteSequence rs = route.getPrimary().getRouteSequence().get(3);
            if (rs.getResourceType().getImplementedInterface().getName()
                    .equals(Tp.class.getName())) {
                //this is must be :-)
                Tp tp = (Tp) rs.getResourceType();
                TpHop tpHop = tp.getTpHop();
                if (tpHop.getPhyNode().getPhysical().getPlaneName()
                        .equals(Constant.VIRTUAL_PLANE)) {
                    return extractSimpleTPCRoute(routeInfoList);
                }
            }
        }
        return routeInfoList;
    }

    private List<RouteInfo> extractSimpleTPCRoute(List<RouteInfo> routeInfoList) {
        RouteInfo route = routeInfoList.get(0);

        Iterator<RouteSequence> iter = route.getPrimary().getRouteSequence().iterator();
        int size = route.getPrimary().getRouteSequence().size();
        while (iter.hasNext()) {
            RouteSequence rs = iter.next();
            if (rs.getSequence() > 2 && rs.getSequence() <= size - 2) {
                iter.remove();
            }
        }
        //now the list only has 4 element, cPort, lPort, lPort, cPort
        Tp atp = (Tp) route.getPrimary().getRouteSequence().get(1).getResourceType();
        Tp ztp = (Tp) route.getPrimary().getRouteSequence().get(2).getResourceType();

        //position 2 will be the virtual siteLink (simulator it as phyLink, but friendlyName is siteLink's)
        RouteSequence newRS = getVirtualSiteLinkHop(3L, atp.getTpHop().getPhyTp().getTpId(),
                ztp.getTpHop().getPhyTp().getTpId());
        route.getPrimary().getRouteSequence().add(2, newRS);

        Iterator<CrossConnections> iterXC = route.getPrimary().getCrossConnections().iterator();
        while (iterXC.hasNext()) {
            CrossConnections routeXC = iterXC.next();
            if (routeXC.getSequence() > 2) {
                iterXC.remove();
            }
        }
        routeInfoList.clear();

        if (route.getSecondary() != null) {
            RouteInfo ri = new RouteInfoBuilder()
                    .setIndex((short) 1)
                    .setKey(new RouteInfoKey((short) 1))
                    .setPrimary(route.getPrimary())
                    .setSecondary(null)
                    .build();
            routeInfoList.add(ri);
        } else {
            routeInfoList.add(route);
        }
        return routeInfoList;
    }

    private RouteSequence getVirtualSiteLinkHop(Long sequence, TpId aTp, TpId zTp) {
        LinkId linkId = new LinkId(
                PhysicalLinkIdNamingRule.getName(LinkType.OsLink, aTp.getValue(), zTp.getValue()));

        Link topoLink = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder()
                .setLinkId(linkId)
                .setKey(new LinkKey(linkId))
                .setSource(new SourceBuilder().setSourceNode(
                                new NodeId(PhysicalTpIdNamingRule.getNodeId(aTp.getValue())))
                        .setSourceTp(aTp)
                        .build())
                .setDestination(new DestinationBuilder().setDestNode(
                                new NodeId(PhysicalTpIdNamingRule.getNodeId(zTp.getValue())))
                        .setDestTp(zTp)
                        .build())
                .addAugmentation(Link1.class, new Link1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setLinkType(LinkType.SiteLink)
                                .setFriendlyName(Constants.VIRTUAL_PLANE)
                                .setFriendlyNameDisplay(Constants.VIRTUAL_PLANE)
                                .build())
                        .build())
                .build();

        LinkHopBuilder lb = new LinkHopBuilder();
        lb.fieldsFrom(topoLink);
        lb.fieldsFrom(topoLink.getAugmentation(Link1.class));

        ResourceType rt = new LinkBuilder().setLinkHop(lb.build()).build();

        return new RouteSequenceBuilder()
                .setSequence(sequence)
                .setKey(new RouteSequenceKey(sequence))
                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
                .setResourceType(rt)
                .build();
    }

//    private List<Route> getRoute(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
//            throws Exception {
//        String topologyRefName = topologyRef.getValue();
//        String linkType = null;
//        List<Route> routes = null;
//        if (topologyRefName.equals(Constants.SITE_TOPO_KEY) && linkRef != null) {
//            linkType = Constants.SITE_TOPO_KEY;
//        } else if (topologyRefName.equals(Constants.SITE_TOPO_KEY) && tunnelRef != null) {
//            linkType = Constants.TUNNEL_TOPO_KEY;
//        } else if (topologyRefName.equals(Constants.PHY_TOPO_KEY) && linkRef != null) {
//            linkType = Constants.PHY_TOPO_KEY;
//        } else {
//            throw new Exception(
//                    "doesn't support retrieve route info on this object");
//        }
//        routes = handlerMap.get(linkType).getRoute(topologyRef, linkRef, tunnelRef);
//        return routes;
//    }

    private RouteInfoDto getRouteInfo(TopologyId topologyRef, LinkId linkRef, Uri tunnelRef)
            throws Exception {
        String topologyRefName = topologyRef.getValue();
        String linkType = null;
        if (topologyRefName.equals(Constants.SITE_TOPO_KEY) && linkRef != null) {
            linkType = Constants.SITE_TOPO_KEY;
        } else if (topologyRefName.equals(Constants.SITE_TOPO_KEY) && tunnelRef != null) {
            linkType = Constants.TUNNEL_TOPO_KEY;
        } else if (topologyRefName.equals(Constants.PHY_TOPO_KEY) && linkRef != null) {
            linkType = Constants.PHY_TOPO_KEY;
        } else {
            throw new Exception(
                    "doesn't support retrieve route info on this object");
        }
        RouteInfoDto routeInfo = handlerMap.get(linkType)
                .getRouteInfo(topologyRef, linkRef, tunnelRef);
        return routeInfo;
    }


    private List<RouteInfo> transfer2DetailRoute(RouteInfoDto routes) throws Exception {
        YangRouteConstructor yangRouteConstructor = new YangRouteConstructor(netconfTopology);
        yangRouteConstructor.extractDetailRoute(routes);
        return yangRouteConstructor.getRouteInfos();
//        RouteRetriever routeRetriever = SpringBeanFinder.getBean(RouteRetriever.class);
//        return routeRetriever.retrieverRouteInfo(routes);
    }

}
