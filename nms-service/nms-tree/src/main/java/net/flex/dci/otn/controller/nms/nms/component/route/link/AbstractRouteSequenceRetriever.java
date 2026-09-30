package net.flex.dci.otn.controller.nms.nms.component.route.link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/6/29 16:23
 */
@Slf4j
public abstract class AbstractRouteSequenceRetriever implements IRouteSequenceRetriever {

    @Autowired
    protected NetconfTopology netconfTopology;

    @Autowired
    protected RouteSequenceRetriever routeSequenceRetriever;

    @Autowired
    protected CrossConnectionsDao crossConnectionsDao;


    /**
     * sub rout is tp phy link and site link
     *
     * @param pathRouteObjects
     * @param crossConnections
     * @return
     */
    protected RouteSequenceDto retrieveRoute(List<PathRouteObject> pathRouteObjects,
            List<CrossConnections> crossConnections, String source, String destination) {
        if (CollectionUtils.isEmpty(pathRouteObjects)) {
            log.warn("there have not route,do nothing");
            return null;
        }
        RouteSequenceDto node = new RouteSequenceDto();
        RouteSequenceDto current = node;
        List<CrossConnections> relateRouteCrossConnections = crossConnections.stream()
                .filter(xc -> xc.getAps() != null || xc.getAmplifier() != null).collect(
                        Collectors.toList());
//        node.getXcs().addAll(realXc);
        node.getXcIds()
                .addAll(relateRouteCrossConnections.stream()
                        .map(xc -> xc.getCrossConnectionId().getValue())
                        .collect(
                                Collectors.toList()));
        //assem there is only one explicitRouteObjects
//        List<PathRouteObject> pathRouteObjects = explicitRouteObjects.get(0).getPathRouteObject();
        for (PathRouteObject pro : pathRouteObjects) {
            ResourceType resourceType = pro.getResourceType();
            Class<?> clazz = resourceType.getImplementedInterface();

            if (clazz.isAssignableFrom(Tp.class)) {
                current = routeSequenceRetriever.retrieveTpHopRouteSequence(pro, current);
            } else if (clazz.isAssignableFrom(Link.class)) {
                current = routeSequenceRetriever.retrieveLinkRouteSequence(pro, current, source,
                        destination);
            }
        }
        node.getPrimary().setXcIds(node.getXcIds());

//        //when wssLink include, the siteLink related route should remove part (end part or startPart)
//        List<String> wssLinkIdList = getWssLinkIds(pathRouteObjects);
//        if (!wssLinkIdList.isEmpty()) {
//            refactorRoute(wssLinkIdList, node.getPrimary());
//        }
        return node.getPrimary();
    }

//    private void refactorRoute(List<String> wssLinkIdList, RouteSequenceDto primary) {
//        int wssLinkPos = 0;
//        String wssLinkId = wssLinkIdList.get(wssLinkPos++);
//        String wssAEqId = PhysicalTpIdNamingRule.getEquipId(
//                PhysicalLinkIdNamingRule.getTpAId(wssLinkId));
//        String wssZEqId = PhysicalTpIdNamingRule.getEquipId(
//                PhysicalLinkIdNamingRule.getTpZId(wssLinkId));
//
//        //go through the path, and cut some info.
//        boolean startCut = false;
//        RouteSequenceDto cutPos = null;
//        boolean sameCard = false;
//        boolean wssOneTpUsed = false;
//        List<CrossConnections> xcs = new ArrayList<>();
//        while (primary != null) {
//            if (primary.getClazz().isAssignableFrom(Tp.class)) {
//                String eqId = PhysicalTpIdNamingRule.getEquipId(primary.getTpId());
//                if (!startCut) {
//                    if (eqId.equals(wssAEqId) || eqId.equals(wssZEqId)) {
//                        startCut = true;
//                        cutPos = primary;
//                        xcs = new ArrayList<>();
//                    }
//                } else {
//                    if (eqId.equals(wssAEqId) || eqId.equals(wssZEqId)) {
//                        if (!sameCard) {
//                            sameCard = true;
//                        } else {
//                            //第二次出现同一张卡上的TP
//                            startCut = false;
//                            sameCard = false;
//
//                            if (wssOneTpUsed) {
//                                //wssLink 上的两个TP都用了， 准备下一个wssLink涉及的丢弃检查
//                                if (wssLinkPos < wssLinkIdList.size()) {
//                                    wssLinkId = wssLinkIdList.get(wssLinkPos++);
//                                    wssAEqId = PhysicalTpIdNamingRule.getEquipId(
//                                            PhysicalLinkIdNamingRule.getTpAId(wssLinkId));
//                                    wssZEqId = PhysicalTpIdNamingRule.getEquipId(
//                                            PhysicalLinkIdNamingRule.getTpZId(wssLinkId));
//
//                                    wssOneTpUsed = false;
//                                }
//                            } else {
//                                wssOneTpUsed = true;
//                            }
//                            cutPos.setPrimary(primary);
//                            cutPos.setXcs(xcs);
//                        }
//                    }
//                }
//            }
//            xcs.addAll(primary.getXcs());
//            primary = primary.getPrimary();
//        }
//    }
//
//    private List<String> getWssLinkIds(List<PathRouteObject> pathRouteObjects) {
//        log.debug("get route wss links");
//        List<String> wssLinkIds = pathRouteObjects.stream()
//                .filter(pathRouteObject -> pathRouteObject.getResourceType()
//                        .getImplementedInterface().isAssignableFrom(Link.class))
//                .map(pathRouteObject -> ((Link) pathRouteObject.getResourceType()).getLinkHop())
//                .filter(linkHop -> {
//                    String topologyRef = linkHop.getTopologyRef().getValue();
//                    String linkId = linkHop.getLinkRef().getValue();
//                    return topologyRef.equals(Constants.PHY_TOPO_KEY)
//                            && PhysicalLinkIdNamingRule.isWssLink(linkId);
//                }).map(linkHop -> linkHop.getLinkRef().getValue())
//                .collect(Collectors.toList());
//        return wssLinkIds;
////        List<String> siteLinkIds = pathRouteObjects.stream()
////                .filter(pathRouteObject -> pathRouteObject.getResourceType().getImplementedInterface().isAssignableFrom(Link.class))
////                .map(pathRouteObject -> ((Link) pathRouteObject.getResourceType()).getLinkHop())
////                .filter(linkHop -> {
////                    String topologyRef = linkHop.getTopologyRef().getValue();
////                    String linkId = linkHop.getLinkRef().getValue();
////                    return topologyRef.equals(Constants.SITE_TOPO_KEY) && PhysicalLinkIdNamingRule.isWssLink(linkId);
////                }).map(linkHop -> linkHop.getLinkRef().getValue())
////                .collect(Collectors.toList());
////
////        List<String> wssLinkIdList = new ArrayList<>();
////
////        int siteLinkNumber = 0;
////        for (PathRouteObject pro : pathRouteObjects) {
////            Class<?> clazz = pro.getResourceType().getImplementedInterface();
////
////            if (clazz.isAssignableFrom(Link.class)) {
////                LinkHop linkHop = ((Link) pro.getResourceType()).getLinkHop();
////                String topologyRef = linkHop.getTopologyRef().getValue();
////                String linkId = linkHop.getLinkRef().getValue();
////
////                if (topologyRef.equals(Constants.SITE_TOPO_KEY) && SiteLinkIdNamingRule.isSiteLink(linkId)) {
////                    siteLinkNumber++;
////                }
////            }
////        }
////        //the siteLink's number should more than 1, only in this situation, we can get wssLink
////        if (siteLinkNumber > 1) {
////            for (PathRouteObject pro : pathRouteObjects) {
////                Class<?> clazz = pro.getResourceType().getImplementedInterface();
////
////                if (clazz.isAssignableFrom(Link.class)) {
////                    LinkHop linkHop = ((Link) pro.getResourceType()).getLinkHop();
////                    String topologyRef = linkHop.getTopologyRef().getValue();
////                    String linkId = linkHop.getLinkRef().getValue();
////
////
////                    if (topologyRef.equals(Constants.PHY_TOPO_KEY) && PhysicalLinkIdNamingRule.isWssLink(linkId)) {
////                        wssLinkIdList.add(linkId);
////                    }
////                }
////            }
////        }
////        return wssLinkIdList;
//    }

    protected List<CrossConnections> getRealCrossConnection(
            List<CrossConnections> crossConnections) {
        log.debug("get real cross connection on ne");
        List<CrossConnections> realCrossConnection = new ArrayList<>();
        List<CrossConnections> externalCrossConnection = new ArrayList<>();
        List<CrossConnections> internalCrossConnection = new ArrayList<>();
        for (CrossConnections crossConnection : crossConnections) {
            if (CrossConnectionUtils.isInternalCrossConnection(crossConnection)) {
                internalCrossConnection.add(crossConnection);
            } else {
                externalCrossConnection.add(crossConnection);
            }
        }
        if (!internalCrossConnection.isEmpty()) {
            List<String> allXcIds = internalCrossConnection.stream()
                    .map(xc -> xc.getCrossConnectionId().getValue()).collect(Collectors.toList());
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs =
                    crossConnectionsDao.listAllRealXcByXcIds(allXcIds);
            Set<String> realXcIds = realXcs.stream().map(xc -> xc.getCrossConnectionId().getValue())
                    .collect(
                            Collectors.toSet());
            Set<String> missingXcIds = new HashSet<>(allXcIds);
            missingXcIds.removeAll(realXcIds);
            if (!missingXcIds.isEmpty()) {
                List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realListXcs = missingXcIds.stream()
                        .flatMap(xcId ->
                                crossConnectionsDao.getXCByXcIdRegexLike(
                                        xcId).stream()).collect(Collectors.toList());
                realXcs.addAll(realListXcs);
            }
            internalCrossConnection = realXcs.stream()
                    .map(realXc -> new CrossConnectionsBuilder(realXc).build()).collect(
                            Collectors.toList());
        }
//        internalCrossConnection = internalCrossConnection.stream().map(xc -> {
//            String crossConnectionId = xc.getCrossConnectionId().getValue();
//            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs = crossConnectionsDao.getXCByXcIdRegexLike(
//                    crossConnectionId);
//            return realXcs.isEmpty() ? xc : new CrossConnectionsBuilder(realXcs.get(0)).build();
//        }).collect(Collectors.toList());
        realCrossConnection.addAll(internalCrossConnection);
        realCrossConnection.addAll(externalCrossConnection);
        return realCrossConnection;
    }

    protected List<PathRouteObject> getPathRoutes(List<ExplicitRouteObjects> explicitRouteObjects
    ) {
        List<PathRouteObject> pathRouteObjects = explicitRouteObjects.get(0).getPathRouteObject();
        return pathRouteObjects;
    }

}
