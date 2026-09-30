package net.flex.dci.otn.controller.nms.nms.component.route;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
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
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/23 14:29
 */
@Slf4j
@Component
public class PhyLinkRoute extends AbstractLinkRoute {

    public PhyLinkRoute(NetconfTopology netconfTopology, RouteRetriever routeRetriever) {
        super(netconfTopology, routeRetriever);
    }


    @Override
    public List<RouteInfo> getRouteById(String id) throws CommonException {
        log.debug("get phy link route by id :{}", id);
        Link phyLink = netconfTopology.getPhyLink(id);
        if (phyLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("there have no route for the phyLink link :{0}", id));
        }
//        List<RouteInfo> routes = getPhyRoute(phyLink);
        RouteInfoDto routeInfoDto = getRouteInfoDto(phyLink);

        List<RouteInfo> routes = routeRetriever.retrieverRouteInfo(routeInfoDto);

        return routes;
    }

//    private List<RouteInfo> getPhyRoute(Link phyLink) {
//        TopologyId phyTopoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
//        String srcTp = phyLink.getSource().getSourceTp().getValue();
//        String dstTp = phyLink.getDestination().getDestTp().getValue();
//
//        List<String> siteList = new ArrayList<>();
//        siteList.add(PhysicalTpIdNamingRule.getSiteId(srcTp));
//        siteList.add(PhysicalTpIdNamingRule.getSiteId(dstTp));
//
//        RetrieverUtil retrieverUtil = RetrieverUtil.getInstance();
//        retrieverUtil.setNetConfTopo(netconfTopology);
//
//        List<RouteSequence> routeSequenceList = new ArrayList<>();
//        routeSequenceList.add(new RouteSequenceBuilder()
//                .setSequence(1L)
//                .setKey(new RouteSequenceKey(1L))
//                .setTopologyRef(phyTopoId)
//                .setResourceType(retrieverUtil.getTerminationPointResourceType(srcTp))
//                .build());
//
//        routeSequenceList.add(new RouteSequenceBuilder()
//                .setSequence(2L)
//                .setKey(new RouteSequenceKey(2L))
//                .setTopologyRef(phyTopoId)
//                .setResourceType(retrieverUtil.getLinkResourceType(phyLink))
//                .build());
//
//        routeSequenceList.add(new RouteSequenceBuilder()
//                .setSequence(3L)
//                .setKey(new RouteSequenceKey(3L))
//                .setTopologyRef(phyTopoId)
//                .setResourceType(retrieverUtil.getTerminationPointResourceType(dstTp))
//                .build());
//
//        RouteInfo ri = new RouteInfoBuilder()
//                .setIndex((short) 0)
//                .setKey(new RouteInfoKey((short) 0))
//                .setSecondary(null)
//                .setPrimary(new PrimaryBuilder()
//                        .setCrossConnections(new ArrayList<>())
//                        .setSiteSequence(siteList)
//                        .setRouteSequence(routeSequenceList)
//                        .build())
//                .build();
//
//        List<RouteInfo> routeInfoList = new ArrayList<>();
//        routeInfoList.add(ri);
//        return routeInfoList;
//    }


    @Override
    public String linkType() {
        return Constants.PHY_LINK;
    }

    private RouteInfoDto getRouteInfoDto(Link phyLink) {
        String source = PhysicalNodeIdNamingRule.getSiteId(
                phyLink.getSource().getSourceNode().getValue());
        String destination = PhysicalNodeIdNamingRule.getSiteId(
                phyLink.getDestination().getDestNode().getValue());
        return RouteInfoDto.builder()
                .source(source)
                .destination(destination)
                .routes(getRealRoute(constructPhyLinkRoute(phyLink)))
                .build();
    }

//    private List<Route> getPhyLinkRoute(Link phyLink) {
//        List<PathRouteObject> proList = new ArrayList<>();
//
//        String srcTpId = phyLink.getSource().getSourceTp().getValue();
//        proList.add(new PathRouteObjectBuilder()
//                .setResourceType(new TpBuilder()
//                        .setTpHop(new TpHopBuilder()
//                                .setTpRef(phyLink.getSource().getSourceTp())
//                                .setNodeRef(new NodeId(PhysicalTpIdNamingRule.getNodeId(srcTpId)))
//                                .setSiteRef(new NodeId(PhysicalTpIdNamingRule.getSiteId(srcTpId)))
//                                .setEquipmentRef(PhysicalTpIdNamingRule.getEquipId(srcTpId))
//                                .build())
//                        .build())
//                .setIndex(0L)
//                .setKey(new PathRouteObjectKey(0L))
//                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                .build());
//
//        proList.add(new PathRouteObjectBuilder()
//                .setResourceType(new LinkBuilder()
//                        .setLinkHop(new LinkHopBuilder()
//                                .setLinkRef(phyLink.getLinkId())
//                                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                                .build())
//                        .build())
//                .setIndex(1L)
//                .setKey(new PathRouteObjectKey(1L))
//                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                .build());
//
//        String dstTpId = phyLink.getDestination().getDestTp().getValue();
//        proList.add(new PathRouteObjectBuilder()
//                .setResourceType(new TpBuilder()
//                        .setTpHop(new TpHopBuilder()
//                                .setTpRef(phyLink.getSource().getSourceTp())
//                                .setNodeRef(new NodeId(PhysicalTpIdNamingRule.getNodeId(dstTpId)))
//                                .setSiteRef(new NodeId(PhysicalTpIdNamingRule.getSiteId(dstTpId)))
//                                .setEquipmentRef(PhysicalTpIdNamingRule.getEquipId(dstTpId))
//                                .build())
//                        .build())
//                .setIndex(2L)
//                .setKey(new PathRouteObjectKey(2L))
//                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                .build());
//
//        //后续程序有问题， 一共3个点的路由，处理后就只有2个点了，最后一个点消失了， 所以这里直接加一个点看看
//        proList.add(new PathRouteObjectBuilder()
//                .setResourceType(new TpBuilder()
//                        .setTpHop(new TpHopBuilder()
//                                .setTpRef(phyLink.getSource().getSourceTp())
//                                .setNodeRef(new NodeId(PhysicalTpIdNamingRule.getNodeId(dstTpId)))
//                                .setSiteRef(new NodeId(PhysicalTpIdNamingRule.getSiteId(dstTpId)))
//                                .setEquipmentRef(PhysicalTpIdNamingRule.getEquipId(dstTpId))
//                                .build())
//                        .build())
//                .setIndex(3L)
//                .setKey(new PathRouteObjectKey(3L))
//                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                .build());
//
//        proList.add(new PathRouteObjectBuilder()
//                .setResourceType(new LinkBuilder()
//                        .setLinkHop(new LinkHopBuilder()
//                                .setLinkRef(phyLink.getLinkId())
//                                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                                .build())
//                        .build())
//                .setIndex(4L)
//                .setKey(new PathRouteObjectKey(4L))
//                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                .build());
//
//        List<ExplicitRouteObjects> explictRouteList = new ArrayList<>();
//        explictRouteList.add(new ExplicitRouteObjectsBuilder()
//                .setExplicitRouteUsage(RouteUsageInclude.class)
//                .setKey(new ExplicitRouteObjectsKey(RouteUsageInclude.class))
//                .setPathRouteObject(proList)
//                .build());
//        Primary primaryRoute = new PrimaryBuilder()
//                .setCrossConnections(new ArrayList<>())
//                .setExplicitRouteObjects(explictRouteList)
//                .build();
//
//        List<Route> routeList = new ArrayList<>();
//        routeList.add(new RouteBuilder()
//                .setIndex((short) 0)
//                .setKey(new RouteKey((short) 0))
//                .setPrimary(primaryRoute)
//                .setSecondary(null)
//                .build());
//
//        return routeList;
//    }

//    protected RouteInfoDto getRouteInfoDto(GetRouteInput getRouteInput) throws Exception {
//        log.debug("export the phy link route. with topo:{}, link:{}",
//                getRouteInput.getTopologyRef(),
//                getRouteInput.getLinkRef().getValue());
//        Link phyLink = netconfTopology.getPhyLink(getRouteInput.getLinkRef().getValue());
//        if (phyLink == null) {
//            throw new Exception("cannot find required link.");
//        }
//        String source = PhysicalNodeIdNamingRule.getSiteId(
//                phyLink.getSource().getSourceNode().getValue());
//        String destination = PhysicalNodeIdNamingRule.getSiteId(
//                phyLink.getSource().getSourceNode().getValue());
//        return RouteInfoDto.builder().routes(constructPhyLinkRoute(phyLink)).source(source)
//                .destination(destination).build();
//    }


    /**
     * construct phy link route
     *
     * @param phyLink
     * @return
     */
    private List<Route> constructPhyLinkRoute(Link phyLink) {
        List<Route> routes = new LinkedList<>();
        RouteBuilder rb = new RouteBuilder();
        short index = 0;
        rb.setIndex(index);
        rb.setKey(new RouteKey(index));

        PrimaryBuilder pb = new PrimaryBuilder();
        pb.setExplicitRouteObjects(buildEro(phyLink));
        pb.setCrossConnections(new ArrayList<>());
        rb.setPrimary(pb.build());

        routes.add(rb.build());
        return routes;
    }

    private List<ExplicitRouteObjects> buildEro(Link ntLink) {
        List<ExplicitRouteObjects> eroList = new LinkedList<>();

        ExplicitRouteObjectsBuilder ero = new ExplicitRouteObjectsBuilder();
        ero.setExplicitRouteUsage(RouteUsageInclude.class);
        ero.setKey(new ExplicitRouteObjectsKey(RouteUsageInclude.class));
        ero.setPathRouteObject(buildPro(ntLink));

        eroList.add(ero.build());
        return eroList;
    }

    private List<PathRouteObject> buildPro(Link link) {
        List<PathRouteObject> proList = new LinkedList<>();
        Long index = 1L;

        proList.add(buildTpPro(index, link.getSource().getSourceTp()));
        index++;
        proList.add(buildLinkPro(index, link));
        index++;
        proList.add(buildTpPro(index, link.getDestination().getDestTp()));

        return proList;
    }


    private PathRouteObject buildTpPro(Long index, TpId tpId) {
        log.debug("build tp pro");
        TopologyId phyTopoId = new TopologyId(Constants.PHY_TOPO_KEY);
        PathRouteObjectBuilder pb = new PathRouteObjectBuilder();
        String siteId = PhysicalTpIdNamingRule.getSiteId(tpId.getValue());
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId.getValue());
        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId.getValue());
        pb.setIndex(index);
        pb.setTopologyRef(phyTopoId);
        pb.setResourceType(new TpBuilder()
                .setTpHop(new TpHopBuilder().setTpRef(tpId).setNodeRef(new NodeId(neId))
                        .setSiteRef(new NodeId(siteId)).setEquipmentRef(equipId).build()).build());

        return pb.build();
    }

    private PathRouteObject buildLinkPro(Long index, Link link) {
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
