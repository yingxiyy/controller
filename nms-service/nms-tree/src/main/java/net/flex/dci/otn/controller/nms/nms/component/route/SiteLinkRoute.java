package net.flex.dci.otn.controller.nms.nms.component.route;

import static net.flex.dci.otc.common.util.YangConstants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.EXP;
import static net.flex.dci.otn.controller.nms.utils.Constants.LOGIC_DESCRIPTION_XC;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO;
import static net.flex.dci.otn.controller.nms.utils.Constants.MUX_TYPES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.constructs.MUXPANELConstructor;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.RouteInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.route.ConnectionPathInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.route.ExternalLinkInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.route.ExternalRouteDetailsDto;
import net.flex.dci.otn.controller.nms.nms.dto.route.MpoPortInfo;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import net.flex.dci.otn.controller.nms.utils.NMSUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.TpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHopBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @version 1.0
 * @date 2022/5/23 14:02
 */
@Slf4j
@Component
public class SiteLinkRoute extends AbstractLinkRoute {


    public SiteLinkRoute(NetconfTopology netconfTopology, RouteRetriever routeRetriever) {
        super(netconfTopology, routeRetriever);
    }

    //    @Override
    public List<RouteInfo> getRoute(GetRouteInput input) {
        log.debug("display the connection topology is :{} route",
                input.getTopologyRef().getValue());
        String topologyRef = input.getTopologyRef().getValue();
        LinkId linkId = input.getLinkRef();
        if (!topologyRef.equals(Constants.SITE_TOPO_KEY) || linkId == null) {
            return new ArrayList<>();
        }
        return null;
    }

    @Override
    public List<RouteInfo> getRouteById(String id) throws CommonException {
        log.debug("get site link route by id :{}", id);
        Link siteLink = netconfTopology.getSiteLink(id);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("there have no route for the site link :%s", id));
        }
        RouteInfoDto routeInfoDto = getRouteInfoDto(siteLink);
        List<RouteInfo> routes = routeRetriever.retrieverRouteInfo(routeInfoDto);
        return routes;
    }


    private RouteInfoDto getRouteInfoDto(Link siteLink) {

        Link1 siteLinkAugmentation = siteLink.getAugmentation(Link1.class);
        Site siteLinkPhysical = siteLinkAugmentation.getSite();
        String sourceTpId = siteLink.getSource().getSourceTp().getValue();
        String destTpId = siteLink.getDestination().getDestTp().getValue();
        List<Route> siteLinkRoute = getSiteLinkRoute(siteLinkPhysical, sourceTpId, destTpId);
        String source = PhysicalNodeIdNamingRule.getSiteId(
                siteLink.getSource().getSourceNode().getValue());
        String destination = PhysicalNodeIdNamingRule.getSiteId(
                siteLink.getDestination().getDestNode().getValue());
        return RouteInfoDto.builder()
                .source(source)
                .destination(destination)
                .routes(siteLinkRoute)
                .connectionId(siteLink.getLinkId().getValue())
                .build();
    }

    private List<Route> getSiteLinkRoute(Site siteLinkPhysical, String sourceTpId,
            String destTpId) {
        log.debug("get current site link route,sourceTpId:{} and destTpId:{}", sourceTpId,
                destTpId);
        if (isDirectMuxConnection(sourceTpId, destTpId)) {
            return getRealRoute(siteLinkPhysical.getExplictRoute().getRoute());
        }
        //special for ira to add mux panel or not
        AExternal aExternal = siteLinkPhysical.getAExternal();
        ZExternal zExternal = siteLinkPhysical.getZExternal();

        List<AddDropLink> aExternalLink =
                aExternal == null ? new ArrayList<>() : aExternal.getAddDropLink();
        List<AddDropLink> zExternalLink =
                zExternal == null ? new ArrayList<>() : zExternal.getAddDropLink();

        ExternalRouteDetailsDto aExternalPathRoutes = buildPathRouteObject(aExternalLink,
                sourceTpId, true);
        ExternalRouteDetailsDto zExternalPathRoutes = buildPathRouteObject(zExternalLink, destTpId,
                false);
//        List<String> aConnectMuxLinkIds = filterConnectMuxLinkId(aExternal.getAddDropLink());
//        List<String> zConnectMuxLinkIds = filterConnectMuxLinkId(zExternal.getAddDropLink());

        if (aExternalPathRoutes == null && zExternalPathRoutes == null) {
            return getRealRoute(siteLinkPhysical.getExplictRoute().getRoute());
        }
//        String aMuxEquipId = getAddDropLinkMuxId(aConnectMuxLinkIds);
//        String zMuxEquipId = getAddDropLinkMuxId(zConnectMuxLinkIds);
        //add mux from source to end
        List<Route> explictRoute = reconstructRoute(siteLinkPhysical.getExplictRoute().getRoute(),
                aExternalPathRoutes, zExternalPathRoutes);
//        List<Route> explictRoute = reconstructRoute(siteLinkPhysical.getExplictRoute().getRoute(),
//                aMuxEquipId, zMuxEquipId, sourceTpId, destTpId);
        return getRealRoute(explictRoute);
    }

    private ExternalRouteDetailsDto buildPathRouteObject(List<AddDropLink> externalLink,
            String connectTp, boolean isSource) {
        if (externalLink.isEmpty()) {
            log.debug("current external path route object is null,discard it");
            return null;
        }
        String refNeId = PhysicalTpIdNamingRule.getNodeId(connectTp);
        Node refNe = netconfTopology.getNeNode(refNeId);
        List<TerminationPoint> terminationPoints = refNe.getTerminationPoint();
        List<Equipments> nodeEquipments = refNe.getAugmentation(Node1.class).getPhysical()
                .getEquipments();
        Map<String, TerminationPoint> terminationPointMap = terminationPoints.stream()
                .collect(Collectors.toMap(terminationPoint -> terminationPoint.getTpId().getValue(),
                        tp -> tp));
        Map<String, Equipments> equipmentsMap = nodeEquipments.stream().collect(
                Collectors.toMap(PhyEquipAttributes::getEquipmentId, equip -> equip));
        List<ExternalLinkInfoDto> externalLinkInfoDtos = externalLink.stream()
                .map(addDropLink -> buildExternalLinkInfoDto(addDropLink, terminationPointMap,
                        equipmentsMap)).collect(
                        Collectors.toList());
        String connectEquipId = PhysicalTpIdNamingRule.getEquipId(connectTp);
        Equipments connectEquipment = netconfTopology.getEquipment(connectEquipId);
        TerminationPoint terminationPoint = netconfTopology.getTerminationPoint(connectTp);
        Map<String, List<ExternalLinkInfoDto>> linkMap = buildExternalLinkMap(externalLinkInfoDtos);
        List<ConnectionPathInfoDto> totalPaths = getExternalRoute(connectEquipment, linkMap);
        List<List<PathRouteObject>> pathRoutes = new ArrayList<>();
        List<CrossConnections> crossConnections = new ArrayList<>();
        for (ConnectionPathInfoDto path : totalPaths) {
            ExternalRouteDetailsDto externalRouteDetailsDto = buildPathRouteObject(path,
                    terminationPoint, connectEquipment);
            crossConnections.addAll(externalRouteDetailsDto.getCrossConnections());
            pathRoutes.add(externalRouteDetailsDto.getPathRouteObjects());
        }

        List<PathRouteObject> pathRouteObjects = interleaveRouteObject(pathRoutes);
        if (isSource) {
            Collections.reverse(pathRouteObjects);
        }
        return ExternalRouteDetailsDto.builder().pathRouteObjects(pathRouteObjects)
                .crossConnections(crossConnections).build();
    }

    private List<PathRouteObject> interleaveRouteObject(List<List<PathRouteObject>> pathRoutes) {
        if (pathRoutes == null || pathRoutes.isEmpty()) {
            return new ArrayList<>();
        }
        int maxLength = 0;
        for (List<PathRouteObject> list : pathRoutes) {
            maxLength = Math.max(maxLength, list.size());
        }
        List<PathRouteObject> pathRouteObjects = new ArrayList<>();
        for (int col = 0; col < maxLength; col++) {
            for (List<PathRouteObject> pathRoute : pathRoutes) {
                if (col < pathRoute.size()) {
                    pathRouteObjects.add(pathRoute.get(col));
                }
            }
        }

        return pathRouteObjects;
    }


    private ExternalRouteDetailsDto buildPathRouteObject(ConnectionPathInfoDto path,
            TerminationPoint sourceTp, Equipments sourceEquip) {
        List<ExternalLinkInfoDto> edges = path.getEdges();
        if (edges == null || edges.isEmpty()) {
            log.warn("No edges in path");
            return ExternalRouteDetailsDto.builder().pathRouteObjects(new ArrayList<>()).build();
        }

        if (sourceEquip == null || StringUtils.isEmpty(sourceEquip.getEquipmentId())) {
            log.warn("Source equipment is null or empty");
            return ExternalRouteDetailsDto.builder().pathRouteObjects(new ArrayList<>()).build();
        }

        List<ExternalLinkInfoDto> sortedEdges = sortEdgesByConnection(edges,
                sourceEquip.getEquipmentId(), sourceTp.getTpId().getValue());
        if (!isPathContinuous(sortedEdges, sourceEquip.getEquipmentId())) {
            log.warn("Sorted edges do not form a continuous path");
            sortedEdges = edges;
        }
        List<PathRouteObject> routeObjects = new ArrayList<>();
        Map<String, List<TerminationPoint>> equipRefTpMap = new HashMap<>();
        equipRefTpMap.computeIfAbsent(sourceEquip.getEquipmentId(), k -> new ArrayList<>())
                .add(sourceTp);
        for (ExternalLinkInfoDto linkInfoDto : sortedEdges) {
            //build path object
            List<PathRouteObject> pathRouteObjects = new ArrayList<>();
            TerminationPoint linkSourceTp = linkInfoDto.getSourceTp();
            TerminationPoint linkDestTp = linkInfoDto.getDestTp();

            equipRefTpMap.computeIfAbsent(linkInfoDto.getSourceEquipId(), k -> new ArrayList<>())
                    .add(linkSourceTp);
            equipRefTpMap.computeIfAbsent(linkInfoDto.getDestEquipId(), k -> new ArrayList<>())
                    .add(linkDestTp);
            PathRouteObject sourceTpObject = generateLogicalTpRouteObject(
                    linkSourceTp.getTpId().getValue());
            PathRouteObject destTpObject = generateLogicalTpRouteObject(
                    linkDestTp.getTpId().getValue());
            PathRouteObject linkObject = generateLogicalPhyLinkRouteObject(linkInfoDto.getLinkId());
            pathRouteObjects.add(sourceTpObject);
            pathRouteObjects.add(linkObject);
            pathRouteObjects.add(destTpObject);
            routeObjects.addAll(pathRouteObjects);
        }
        //crossConnection generate
        List<CrossConnections> crossConnections = new ArrayList<>();
        for (Map.Entry<String, List<TerminationPoint>> entry : equipRefTpMap.entrySet()) {

            List<TerminationPoint> terminationPoints = entry.getValue();
            //assemble only have two terminationPoints
            if (terminationPoints.size() > 1) {
                TerminationPoint leftTp = terminationPoints.get(0);
                TerminationPoint rightTp = terminationPoints.get(1);
                PortType leftTpType = getTerminationPointType(leftTp);
                PortType rightTpType = getTerminationPointType(rightTp);
                if (leftTp.getTpId().getValue().contains(EXP) || rightTp.getTpId().getValue()
                        .contains(EXP)) {
                    log.debug("tp is exp port do nothing");
                    continue;
                }
                if (leftTpType == PortType.WSSMesh && rightTpType == PortType.OALine) {
                    List<CrossConnections> logicalCrossConnections = buildLogicalCrossConnections(
                            leftTp.getTpId().getValue(), rightTp.getTpId().getValue());
                    crossConnections.addAll(logicalCrossConnections);
                } else if (rightTpType == PortType.WSSMesh && leftTpType == PortType.OALine) {
                    List<CrossConnections> logicalCrossConnections = buildLogicalCrossConnections(
                            rightTp.getTpId().getValue(), leftTp.getTpId().getValue());
                    crossConnections.addAll(logicalCrossConnections);
                }
            }
        }

        return ExternalRouteDetailsDto.builder().pathRouteObjects(routeObjects)
                .crossConnections(crossConnections).build();
    }


    private List<ExternalLinkInfoDto> sortEdgesByConnection(List<ExternalLinkInfoDto> edges,
            String currentEquipId, String currentPortId) {
        List<ExternalLinkInfoDto> sorted = new ArrayList<>();
        List<ExternalLinkInfoDto> remaining = new ArrayList<>(edges);
        while (!remaining.isEmpty()) {
            ExternalLinkInfoDto matchedLink = findNextLink(remaining, currentEquipId,
                    currentPortId);

            if (matchedLink == null) {
                matchedLink = findNextLinkByEquipmentOnly(remaining, currentEquipId);

                if (matchedLink == null) {
                    log.warn(
                            "Cannot find continuous link from equipment {}, port {}. Remaining links: {}",
                            currentEquipId, currentPortId, remaining.size());
                    sorted.addAll(remaining);
                    break;
                }
            }

            sorted.add(matchedLink);
            remaining.remove(matchedLink);

            if (currentEquipId.equals(matchedLink.getSourceEquipId()) &&
                    (currentPortId == null || currentPortId.equals(
                            matchedLink.getSourceTp().getTpId().getValue()))) {
                currentEquipId = matchedLink.getDestEquipId();
                currentPortId = matchedLink.getDestTp().getTpId().getValue();
            } else {
                currentEquipId = matchedLink.getSourceEquipId();
                currentPortId = matchedLink.getSourceTp().getTpId().getValue();
            }
        }

        return sorted;
    }


    private ExternalLinkInfoDto findNextLink(List<ExternalLinkInfoDto> links,
            String currentEquipId,
            String currentPortId) {

        for (ExternalLinkInfoDto link : links) {
            boolean matchesSource = currentEquipId.equals(link.getSourceEquipId()) &&
                    (currentPortId == null || currentPortId.equals(
                            link.getSourceTp().getTpId().getValue()));

            boolean matchesDest = currentEquipId.equals(link.getDestEquipId()) &&
                    (currentPortId == null || currentPortId.equals(
                            link.getDestTp().getTpId().getValue()));

            if (matchesSource || matchesDest) {
                return link;
            }
        }

        return null;
    }

    private ExternalLinkInfoDto findNextLinkByEquipmentOnly(List<ExternalLinkInfoDto> links,
            String currentEquipId) {

        for (ExternalLinkInfoDto link : links) {
            boolean sourceMatches = currentEquipId.equals(link.getSourceEquipId());
            boolean destMatches = currentEquipId.equals(link.getDestEquipId());

            if (sourceMatches && !destMatches) {
                return link;
            }
            if (!sourceMatches && destMatches) {
                return link;
            }
        }

        for (ExternalLinkInfoDto link : links) {
            if (currentEquipId.equals(link.getSourceEquipId()) ||
                    currentEquipId.equals(link.getDestEquipId())) {
                return link;
            }
        }

        return null;
    }

    private boolean isPathContinuous(List<ExternalLinkInfoDto> edges, String startEquipId) {
        if (edges == null || edges.isEmpty()) {
            return true;
        }

        String currentEquipId = startEquipId;

        for (ExternalLinkInfoDto edge : edges) {
            boolean connected = false;

            if (currentEquipId.equals(edge.getSourceEquipId())) {
                currentEquipId = edge.getDestEquipId();
                connected = true;
            } else if (currentEquipId.equals(edge.getDestEquipId())) {
                currentEquipId = edge.getSourceEquipId();
                connected = true;
            }

            if (!connected) {
                log.warn("Path discontinuity found at edge {}: current equipment {} not connected",
                        edge.getLinkId(), currentEquipId);
                return false;
            }
        }

        return true;
    }


    private List<ConnectionPathInfoDto> getExternalRoute(Equipments connectEquipment,
            Map<String, List<ExternalLinkInfoDto>> linkMap) {
        log.debug("get external route for the external connection,source equip:{}",
                connectEquipment);
        List<ConnectionPathInfoDto> resultPathList = new ArrayList<>();
        if (connectEquipment == null || connectEquipment.getEquipmentId() == null || linkMap == null
                || linkMap.isEmpty()) {
            return resultPathList;
        }
        String startEquipmentId = connectEquipment.getEquipmentId();
        Queue<PathNode> queue = new LinkedList<>();
        queue.offer(
                new PathNode(startEquipmentId, new ArrayList<>(), new HashSet<>(
                        Collections.singleton(startEquipmentId))));
        while (!queue.isEmpty()) {
            PathNode node = queue.poll();
            String currentEquipId = node.getEquipId();
            List<ExternalLinkInfoDto> edges = node.getEdges();
            Set<String> visited = node.getVisitedEquipId();

            List<ExternalLinkInfoDto> currentDeviceLinks = linkMap.getOrDefault(currentEquipId,
                    new ArrayList<>());
            boolean hasUnvisitedNext = false;
            for (ExternalLinkInfoDto link : currentDeviceLinks) {
                String nextEquipId = Objects.equals(currentEquipId, link.getSourceEquipId())
                        ? link.getDestEquipId()
                        : link.getSourceEquipId();

                if (visited.contains(nextEquipId)) {
                    continue;
                }
                hasUnvisitedNext = true;
                Set<String> newVisited = new HashSet<>(visited);
                newVisited.add(nextEquipId);
                List<ExternalLinkInfoDto> newEdges = new ArrayList<>(edges);
                newEdges.add(link);
                queue.offer(new PathNode(nextEquipId, newEdges, newVisited));

            }
            if (!hasUnvisitedNext && !edges.isEmpty()) {
                buildConnectionPath(startEquipmentId, currentEquipId, edges, resultPathList);
            }

        }
        return resultPathList;
    }

    private void buildConnectionPath(String startEquipmentId, String endEquipId,
            List<ExternalLinkInfoDto> edges, List<ConnectionPathInfoDto> resultPathList) {
        if (edges.isEmpty()) {
            return;
        }
        ConnectionPathInfoDto connectionPathInfoDto = new ConnectionPathInfoDto();
        connectionPathInfoDto.setEdges(edges);
        connectionPathInfoDto.setStartEquipId(startEquipmentId);
        connectionPathInfoDto.setEndEquipId(endEquipId);
        resultPathList.add(connectionPathInfoDto);
    }

    private Map<String, List<ExternalLinkInfoDto>> buildExternalLinkMap(
            List<ExternalLinkInfoDto> externalLinkInfoDtos) {
        List<ExternalLinkInfoDto> mergedLinks = mergeMpoLinksInList(externalLinkInfoDtos);
        Map<String, List<ExternalLinkInfoDto>> linkMap = new HashMap<>();
        for (ExternalLinkInfoDto externalLinkInfoDto : mergedLinks) {
            linkMap.computeIfAbsent(externalLinkInfoDto.getSourceEquipId(),
                    k -> new ArrayList<>()).add(externalLinkInfoDto);
            linkMap.computeIfAbsent(externalLinkInfoDto.getDestEquipId(),
                    k -> new ArrayList<>()).add(externalLinkInfoDto);
        }
        return linkMap;
    }

    /**
     * mergeMpoLinks
     *
     * @param links
     * @return
     */
    private List<ExternalLinkInfoDto> mergeMpoLinksInList(
            List<ExternalLinkInfoDto> links) {
        if (links == null || links.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, List<ExternalLinkInfoDto>> linksByDevicePair = new HashMap<>();
        List<ExternalLinkInfoDto> nonMpoLinks = new ArrayList<>();
        for (ExternalLinkInfoDto link : links) {
            if (isMpoLink(link)) {
                String pairKey = generateDevicePairKey(link.getSourceEquip().getEquipmentId(),
                        link.getDestEquip().getEquipmentId());
                linksByDevicePair.computeIfAbsent(pairKey, k -> new ArrayList<>()).add(link);
            } else if (isMpoToExpLink(link)) {
                log.debug("skip mpo to exp external link: {}", link);
                continue;
            } else {
                nonMpoLinks.add(link);
            }
        }
        List<ExternalLinkInfoDto> result = new ArrayList<>(nonMpoLinks);

        for (Map.Entry<String, List<ExternalLinkInfoDto>> entry : linksByDevicePair.entrySet()) {
            List<ExternalLinkInfoDto> mpoLinks = entry.getValue();
            if (mpoLinks.size() == 1) {
                result.add(mpoLinks.get(0));
            } else {
                ExternalLinkInfoDto mergeLink = mergeMpoLinkGroup(mpoLinks);
                if (mergeLink != null) {
                    log.debug("Merge {} MPO links between {} and {}", mpoLinks.size(),
                            mergeLink.getSourceEquipId(), mergeLink.getDestEquipId());
                    result.add(mergeLink);
                }
            }
        }
        return result;
    }

    private boolean isMpoToExpLink(ExternalLinkInfoDto link) {
        if (link == null) {
            return false;
        }
        String sourcePort = link.getSourceTp().getTpId().getValue();
        String destPort = link.getDestTp().getTpId().getValue();
        boolean isMpo = sourcePort.contains(MPO) || destPort.contains(MPO);
        boolean isExp = sourcePort.contains(EXP) || destPort.contains(EXP);
        return isMpo && isExp;
    }

    private ExternalLinkInfoDto mergeMpoLinkGroup(List<ExternalLinkInfoDto> mpoLinks) {
        if (mpoLinks == null || mpoLinks.isEmpty()) {
            return null;
        }
        ExternalLinkInfoDto baseLink = mpoLinks.get(0);
        List<MpoPortInfo> sourcePorts = new ArrayList<>();
        List<MpoPortInfo> destPorts = new ArrayList<>();

        for (ExternalLinkInfoDto mpoLink : mpoLinks) {
            sourcePorts.add(MpoPortInfo.builder()
                    .equipmentId(mpoLink.getSourceEquipId())
                    .tpId(mpoLink.getSourceTp().getTpId().getValue())
                    .tp(mpoLink.getSourceTp())
                    .build());
            destPorts.add(MpoPortInfo.builder()
                    .equipmentId(mpoLink.getDestEquipId())
                    .tpId(mpoLink.getDestTp().getTpId().getValue())
                    .tp(mpoLink.getDestTp())
                    .build());
        }
        TerminationPoint mergedSourceTp = createMergedTerminationPoint(sourcePorts);
        TerminationPoint mergedDestTp = createMergedTerminationPoint(destPorts);

        String mergedLinkId = generateMergeLinkId(baseLink, mergedSourceTp.getTpId().getValue(),
                mergedDestTp.getTpId().getValue());

        return ExternalLinkInfoDto.builder()
                .linkId(mergedLinkId)
                .connectEquipType(baseLink.getConnectEquipType())
                .sourceEquipId(baseLink.getSourceEquipId())
                .destEquipId(baseLink.getDestEquipId())
                .destTp(mergedDestTp)
                .sourceTp(mergedSourceTp)
                .isMpoGroup(true)
                .mpoLinks(new ArrayList<>(mpoLinks))
                .build();
    }

    private String generateMergeLinkId(ExternalLinkInfoDto baseLink, String mergedSourceTpId,
            String mergedDestTpId) {
        String linkId = baseLink.getLinkId();
        log.debug("generate merge Mpo id:{}", linkId);
        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
        String destTpId = PhysicalLinkIdNamingRule.getTpZId(linkId);
        linkId = linkId.replaceAll(sourceTpId, mergedSourceTpId)
                .replaceAll(destTpId, mergedDestTpId);
        return linkId;
    }

    private TerminationPoint createMergedTerminationPoint(List<MpoPortInfo> portInfos) {
        if (portInfos == null || portInfos.isEmpty()) {
            return null;
        }
        MpoPortInfo mpoPortInfo = portInfos.get(0);
        String tpId = mpoPortInfo.getTpId();
        TerminationPoint tp = mpoPortInfo.getTp();
        TerminationPoint terminationPoint = MUXPANELConstructor.virtualizeMPOTP(tp, tpId);
        return terminationPoint;
    }

    private String generateDevicePairKey(String equip1, String equip2) {
        if (equip1 == null || equip2 == null) {
            return null;
        }
        if (equip1.compareTo(equip2) < 0) {
            return equip1 + "|" + equip2;
        } else {
            return equip2 + "|" + equip1;
        }
    }

    private boolean isMpoLink(ExternalLinkInfoDto link) {
        if (link == null) {
            return false;
        }
        String sourcePort = link.getSourceTp().getTpId().getValue();
        String destPort = link.getDestTp().getTpId().getValue();
        boolean sourceIsMpo = sourcePort.contains(MPO);
        boolean destIsMpo = destPort.contains(MPO);
        return sourceIsMpo && destIsMpo;
    }

    private ExternalLinkInfoDto buildExternalLinkInfoDto(AddDropLink addDropLink,
            Map<String, TerminationPoint> terminationPointMap,
            Map<String, Equipments> equipmentsMap) {
        log.debug("build the external link info the addrop link is:{}", addDropLink.getLinkRef());
        //add drop only in one ne
        EquipType connectType = addDropLink.getConnnectorType();
        String addDropLinkId = addDropLink.getLinkRef();

        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(addDropLinkId);
        String destTpId = PhysicalLinkIdNamingRule.getTpZId(addDropLinkId);
        String sourceEquipId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
        String destEquipId = PhysicalTpIdNamingRule.getEquipId(destTpId);

        TerminationPoint sourceTp = terminationPointMap.get(sourceTpId);
        TerminationPoint destTp = terminationPointMap.get(destTpId);

        Equipments sourceEquip = equipmentsMap.get(sourceEquipId);

        Equipments destEquip = equipmentsMap.get(destEquipId);

        return ExternalLinkInfoDto.builder().linkId(addDropLinkId).destTp(destTp)
                .sourceTp(sourceTp)
                .connectEquipType(connectType)
                .destEquip(destEquip)
                .destEquipId(destEquipId)
                .sourceEquip(sourceEquip)
                .sourceEquipId(sourceEquipId)
                .build();

    }

    private boolean isDirectMuxConnection(String sourceTpId, String destTpId) {
        log.debug("detecting current siteLink connection from:{} to :{} ", sourceTpId, destTpId);
        String sourceMuxEquipId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
        String destMuxEquipId = PhysicalTpIdNamingRule.getEquipId(destTpId);
        Equipments sourceEquip = netconfTopology.getEquipment(sourceMuxEquipId);
        Equipments destEquip = netconfTopology.getEquipment(destMuxEquipId);
        boolean sourceIsMux =
                sourceEquip != null && sourceEquip.getEquipType() == EquipType.MUXPANEL;
        boolean destIsMux = destEquip != null && destEquip.getEquipType() == EquipType.MUXPANEL;
        return sourceIsMux || destIsMux;
    }


    private List<String> filterConnectMuxLinkId(List<AddDropLink> addDropLinks) {
        List<String> addDropLinkIds = addDropLinks.stream()
                .map(AddDropLink::getLinkRef).collect(
                        Collectors.toList());
        if (CollectionUtils.isEmpty(addDropLinkIds)) {
            return new ArrayList<>();
        }
        List<String> connectMuxLinkIds = new ArrayList<>();
        for (String phyLinkId : addDropLinkIds) {
            String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(phyLinkId);
            String sourceEqId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
            String destTpId = PhysicalLinkIdNamingRule.getTpZId(phyLinkId);
            String destEqId = PhysicalTpIdNamingRule.getEquipId(destTpId);
            Equipments sourceEquipment = netconfTopology.getEquipment(sourceEqId);
            Equipments destEquipment = netconfTopology.getEquipment(destEqId);
            EquipType sourceEquipType = sourceEquipment.getEquipType();
            EquipType destEquipType = destEquipment.getEquipType();
            if (MUX_TYPES.contains(sourceEquipType) || MUX_TYPES.contains(destEquipType)) {
                connectMuxLinkIds.add(phyLinkId);
            }
        }
        return connectMuxLinkIds;
    }

    /**
     * method to add mux panel to otm site
     *
     * @param routes
     * @param aMuxEquipId
     * @param zMuxEquipId
     * @return
     */
    private List<Route> reconstructRoute(List<Route> routes, String aMuxEquipId,
            String zMuxEquipId, String sourceTpId, String destTpId) {
        List<Route> reConstructRoutes = new ArrayList<>();

        for (Route route : routes) {
            Primary primary = route.getPrimary();
            Primary reConstructPrimary = reConstructPrimaryRoute(primary, aMuxEquipId, zMuxEquipId,
                    sourceTpId, destTpId);
            RouteBuilder routeBuilder = new RouteBuilder();
            routeBuilder.setIndex(route.getIndex());
            routeBuilder.setKey(route.getKey());
            routeBuilder.setPrimary(reConstructPrimary);
            routeBuilder.setSecondary(route.getSecondary());
            routeBuilder.setThird(route.getThird());
            reConstructRoutes.add(routeBuilder.build());
        }

        return reConstructRoutes;
    }

    /**
     * method to add mux panel to otm site
     *
     * @param routes
     * @param aExternalPathRoutes
     * @param zExternalPathRoutes
     * @return
     */
    private List<Route> reconstructRoute(List<Route> routes,
            ExternalRouteDetailsDto aExternalPathRoutes,
            ExternalRouteDetailsDto zExternalPathRoutes) {
        List<Route> reConstructRoutes = new ArrayList<>();

        for (Route route : routes) {
            Primary primary = route.getPrimary();
            Primary reConstructPrimary = reConstructPrimaryRoute(primary, aExternalPathRoutes,
                    zExternalPathRoutes);
            RouteBuilder routeBuilder = new RouteBuilder();
            routeBuilder.setIndex(route.getIndex());
            routeBuilder.setKey(route.getKey());
            routeBuilder.setPrimary(reConstructPrimary);
            routeBuilder.setSecondary(route.getSecondary());
            routeBuilder.setThird(route.getThird());
            reConstructRoutes.add(routeBuilder.build());
        }

        return reConstructRoutes;
    }

    /**
     * reconstruct primary route
     *
     * @param primary
     * @param aExternalPathRoutes
     * @param zExternalPathRoutes
     * @return
     */
    private Primary reConstructPrimaryRoute(Primary primary,
            ExternalRouteDetailsDto aExternalPathRoutes,
            ExternalRouteDetailsDto zExternalPathRoutes) {
        log.debug("reConstruct primary route");
        List<CrossConnections> crossConnections = primary.getCrossConnections();
        //add crossConnections;
        if (aExternalPathRoutes != null) {
            crossConnections.addAll(aExternalPathRoutes.getCrossConnections());
        }
        if (zExternalPathRoutes != null) {
            crossConnections.addAll(zExternalPathRoutes.getCrossConnections());
        }
        List<ExplicitRouteObjects> reconstructRouteObject = reConstructExplicitRoute(
                primary.getExplicitRouteObjects(), aExternalPathRoutes, zExternalPathRoutes);

        PrimaryBuilder primaryBuilder = new PrimaryBuilder();
        primaryBuilder.setCrossConnections(crossConnections);
        primaryBuilder.setExplicitRouteObjects(reconstructRouteObject);
        return primaryBuilder.build();
    }

    private List<ExplicitRouteObjects> reConstructExplicitRoute(
            List<ExplicitRouteObjects> explicitRouteObjects,
            ExternalRouteDetailsDto aExternalPathRoutes,
            ExternalRouteDetailsDto zExternalPathRoutes) {
        log.debug("add new add drop path routes to the current route");

        List<ExplicitRouteObjects> routeObjects = new ArrayList<>();
        for (ExplicitRouteObjects explicitRouteObject : explicitRouteObjects) {
            List<PathRouteObject> pathRoute = explicitRouteObject.getPathRouteObject();
            List<PathRouteObject> reconstructPathRoute = reconstructPathRoute(pathRoute,
                    aExternalPathRoutes, zExternalPathRoutes);
            ExplicitRouteObjectsBuilder explicitRouteObjectsBuilder = new ExplicitRouteObjectsBuilder();
            explicitRouteObjectsBuilder.setPathRouteObject(reconstructPathRoute);
            explicitRouteObjectsBuilder.setExplicitRouteUsage(
                    explicitRouteObject.getExplicitRouteUsage());

            routeObjects.add(explicitRouteObjectsBuilder.build());
        }

        return routeObjects;

    }

    private List<PathRouteObject> reconstructPathRoute(List<PathRouteObject> pathRoute,
            ExternalRouteDetailsDto aExternalPathRoutes,
            ExternalRouteDetailsDto zExternalPathRoutes) {
        List<PathRouteObject> pathRouteObjects = new ArrayList<>();
        if (aExternalPathRoutes != null) {
            pathRouteObjects.addAll(aExternalPathRoutes.getPathRouteObjects());
        }
        pathRouteObjects.addAll(pathRoute);
        if (zExternalPathRoutes != null) {
            pathRouteObjects.addAll(zExternalPathRoutes.getPathRouteObjects());
        }
//        List<PathRouteObject> reassignIndexPathRoute = new ArrayList<>();
        List<PathRouteObject> reassignIndexPathRoute =
                IntStream.range(0, pathRouteObjects.size())
                        .mapToObj(i -> new PathRouteObjectBuilder(pathRouteObjects.get(i))
                                .setIndex(i + 1L)
                                .setKey(new PathRouteObjectKey(i + 1L))
                                .build())
                        .collect(Collectors.toList());

        return reassignIndexPathRoute;
    }

    /**
     * reconstruct primary route
     *
     * @param primary
     * @param aMuxEquipId
     * @param zMuxEquipId
     * @param sourceTpId
     * @param destTpId
     * @return
     */
    private Primary reConstructPrimaryRoute(Primary primary, String aMuxEquipId, String zMuxEquipId,
            String sourceTpId, String destTpId) {
        log.debug("reConstruct primary route");
        List<CrossConnections> crossConnections = primary.getCrossConnections();
        //add crossConnections;
        List<CrossConnections> logicalCrossConnections = generateLogicCrossConnections(aMuxEquipId,
                zMuxEquipId, sourceTpId, destTpId);
        List<ExplicitRouteObjects> reconstructRouteObject = reConstructExplicitRoute(
                primary.getExplicitRouteObjects(), aMuxEquipId, zMuxEquipId, sourceTpId, destTpId);
        crossConnections.addAll(logicalCrossConnections);
        PrimaryBuilder primaryBuilder = new PrimaryBuilder();
        primaryBuilder.setCrossConnections(crossConnections);
        primaryBuilder.setExplicitRouteObjects(reconstructRouteObject);
        return primaryBuilder.build();
    }

    private List<ExplicitRouteObjects> reConstructExplicitRoute(
            List<ExplicitRouteObjects> explicitRouteObjects, String aMuxEquipId, String zMuxEquipId,
            String sourceTpId, String destTpId) {
        log.debug("add new logical tp and link to the current route");

        List<ExplicitRouteObjects> routeObjects = new ArrayList<>();
        for (ExplicitRouteObjects explicitRouteObject : explicitRouteObjects) {
            List<PathRouteObject> pathRoute = explicitRouteObject.getPathRouteObject();
            List<PathRouteObject> reconstructPathRoute = reconstructPathRoute(pathRoute,
                    aMuxEquipId, zMuxEquipId, sourceTpId, destTpId);
            ExplicitRouteObjectsBuilder explicitRouteObjectsBuilder = new ExplicitRouteObjectsBuilder();
            explicitRouteObjectsBuilder.setPathRouteObject(reconstructPathRoute);
            explicitRouteObjectsBuilder.setExplicitRouteUsage(
                    explicitRouteObject.getExplicitRouteUsage());

            routeObjects.add(explicitRouteObjectsBuilder.build());
        }

        return routeObjects;

    }

    private List<PathRouteObject> reconstructPathRoute(List<PathRouteObject> pathRoute,
            String aMuxEquipId, String zMuxEquipId, String sourceTpId, String destTpId) {
        log.debug(
                "reconstruct path route add  logical route sourceTp:{} destTp:{} ,source connect EquipId:{} dest connect EquipId:{}",
                sourceTpId, destTpId, aMuxEquipId, zMuxEquipId);
        List<PathRouteObject> pathRouteObjects = new ArrayList<>();
        //source
        if (!ObjectUtils.isEmpty(aMuxEquipId)) {
            String alogicalMuxTpId = NMSUtils.generateLogicalMpoTpId(aMuxEquipId);
            String alogicalMuxConnectTpId = NMSUtils.generateLogicalMpoTpId(
                    PhysicalTpIdNamingRule.getEquipId(sourceTpId));
            String alogicalConnectMuxPhyLinkId = PhysicalLinkIdNamingRule.createLinkId(
                    alogicalMuxTpId, alogicalMuxConnectTpId, LinkType.OmsLink);
            PathRouteObject alogicalMuxTpPathRouteObject = generateLogicalTpRouteObject(
                    alogicalMuxTpId);
            PathRouteObject alogicalPhyLinkPathRouteObject = generateLogicalPhyLinkRouteObject(
                    alogicalConnectMuxPhyLinkId);
            PathRouteObject alogicalMuxConnectTpPathRouteObject = generateLogicalTpRouteObject(
                    alogicalMuxConnectTpId);
            pathRouteObjects.add(alogicalMuxTpPathRouteObject);
            pathRouteObjects.add(alogicalPhyLinkPathRouteObject);
            pathRouteObjects.add(alogicalMuxConnectTpPathRouteObject);
        }
        pathRouteObjects.addAll(pathRoute);
        if (!ObjectUtils.isEmpty(zMuxEquipId)) {
            String zlogicalMuxTpId = NMSUtils.generateLogicalMpoTpId(zMuxEquipId);
            String zlogicalMuxConnectTpId = NMSUtils.generateLogicalMpoTpId(
                    PhysicalTpIdNamingRule.getEquipId(destTpId));
            String zlogicalConnectMuxPhyLinkId = PhysicalLinkIdNamingRule.createLinkId(
                    zlogicalMuxTpId, zlogicalMuxConnectTpId, LinkType.OmsLink);

            PathRouteObject zlogicalMuxConnectTpPathRouteObject = generateLogicalTpRouteObject(
                    zlogicalMuxConnectTpId);
            PathRouteObject zlogicalConnectMuxPhyPathRouteObject = generateLogicalPhyLinkRouteObject(
                    zlogicalConnectMuxPhyLinkId);
            PathRouteObject zlogicalMuxTpPathRouteObject = generateLogicalTpRouteObject(
                    zlogicalMuxTpId);
            pathRouteObjects.add(zlogicalMuxConnectTpPathRouteObject);
            pathRouteObjects.add(zlogicalConnectMuxPhyPathRouteObject);
            pathRouteObjects.add(zlogicalMuxTpPathRouteObject);
        }
        List<PathRouteObject> finalPathRouteObjects = pathRouteObjects;
        List<PathRouteObject> reindexed =
                IntStream.range(0, pathRouteObjects.size())
                        .mapToObj(i -> new PathRouteObjectBuilder(finalPathRouteObjects.get(i))
                                .setIndex(i + 1L)
                                .setKey(new PathRouteObjectKey(i + 1L))
                                .build())
                        .collect(Collectors.toList());
        pathRouteObjects = reindexed;
        return pathRouteObjects;
    }

    private PathRouteObject generateLogicalPhyLinkRouteObject(String phyLinkId) {
        log.debug("generate logical physical link route object :{}", phyLinkId);
        PathRouteObjectBuilder pathRouteObjectBuilder = new PathRouteObjectBuilder();
        pathRouteObjectBuilder.setTopologyRef(TopologyId.getDefaultInstance(PHY_TOPO_KEY));
        LinkHopBuilder linkHopBuilder = new LinkHopBuilder();
        linkHopBuilder.setLinkRef(LinkId.getDefaultInstance(phyLinkId));
        linkHopBuilder.setTopologyRef(TopologyId.getDefaultInstance(PHY_TOPO_KEY));
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkHop(linkHopBuilder.build());
        pathRouteObjectBuilder.setResourceType(linkBuilder.build());
        return pathRouteObjectBuilder.build();
    }

    private PathRouteObject generateLogicalTpRouteObject(String tpId) {
        log.debug("generate logical tp route object :{}", tpId);
        String siteId = PhysicalTpIdNamingRule.getSiteId(tpId);
        String nodeRef = NMSUtils.getNodeRefFromTpId(tpId);
        String equipmentRef = NMSUtils.getEquipRefFromTpId(tpId);
        PathRouteObjectBuilder pathRouteObjectBuilder = new PathRouteObjectBuilder();
        pathRouteObjectBuilder.setTopologyRef(TopologyId.getDefaultInstance(PHY_TOPO_KEY));
        TpHopBuilder tpHopBuilder = new TpHopBuilder();
        tpHopBuilder.setTpRef(TpId.getDefaultInstance(tpId));
        tpHopBuilder.setSiteRef(NodeId.getDefaultInstance(siteId));
        tpHopBuilder.setNodeRef(NodeId.getDefaultInstance(nodeRef));
        tpHopBuilder.setEquipmentRef(equipmentRef);
        TpBuilder tpBuilder = new TpBuilder();
        tpBuilder.setTpHop(tpHopBuilder.build());
        pathRouteObjectBuilder.setResourceType(tpBuilder.build());
        return pathRouteObjectBuilder.build();
    }


    private List<CrossConnections> generateLogicCrossConnections(String aMuxEquipId,
            String zMuxEquipId, String sourceTpId, String destTpId) {
        log.debug("generate logic cross connection from mux to ila");
        List<CrossConnections> crossConnections = new ArrayList<>();
        if (aMuxEquipId != null) {
            String sourceEquipId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
            String alogicalMpoId = NMSUtils.generateLogicalMpoTpId(sourceEquipId);
            List<CrossConnections> sourceLogicCrossConnections = buildLogicalCrossConnections(
                    alogicalMpoId, sourceTpId);
            crossConnections.addAll(sourceLogicCrossConnections);
        }
        if (zMuxEquipId != null) {
            String destEquipId = PhysicalTpIdNamingRule.getEquipId(destTpId);
            String zlogicalMpoId = NMSUtils.generateLogicalMpoTpId(destEquipId);
            List<CrossConnections> destLogicCrossConnections = buildLogicalCrossConnections(
                    zlogicalMpoId, destTpId);
            crossConnections.addAll(destLogicCrossConnections);
        }
        return crossConnections;
    }

    /**
     * build logical cross connections;
     *
     * @param alogicalMpoId
     * @param tpId
     * @return
     */
    private List<CrossConnections> buildLogicalCrossConnections(String alogicalMpoId,
            String tpId) {
        log.debug("build logical cross connections between {} and {}", alogicalMpoId, tpId);
        String nodeId = PhysicalTpIdNamingRule.getNodeId(alogicalMpoId);
        CrossConnectionsBuilder azCrossConnectionsBuilder = new CrossConnectionsBuilder();
        azCrossConnectionsBuilder.setCrossConnectionId(
                CrossConnectionUtils.generateMuxCrossConnectionId(tpId, alogicalMpoId));
        azCrossConnectionsBuilder.setNodeRef(NodeId.getDefaultInstance(nodeId));
        azCrossConnectionsBuilder.setAdminState(AdminStatus.Unknown);
        azCrossConnectionsBuilder.setFixed(true);
        azCrossConnectionsBuilder.setDirection(LinkDirection.Unidirection);
        azCrossConnectionsBuilder.setSourceTp(Collections.singletonList(
                new SourceTpBuilder().setTpRef(TpId.getDefaultInstance(tpId)).build()));
        azCrossConnectionsBuilder.setDestinationTp(Collections.singletonList(
                new DestinationTpBuilder().setTpRef(TpId.getDefaultInstance(alogicalMpoId))
                        .build()));
        azCrossConnectionsBuilder.setDescription(LOGIC_DESCRIPTION_XC);

        CrossConnectionsBuilder zaCrossConnectionsBuilder = new CrossConnectionsBuilder();
        zaCrossConnectionsBuilder.setCrossConnectionId(
                CrossConnectionUtils.generateMuxCrossConnectionId(alogicalMpoId, tpId));
        zaCrossConnectionsBuilder.setNodeRef(NodeId.getDefaultInstance(nodeId));
        zaCrossConnectionsBuilder.setAdminState(AdminStatus.Unknown);
        zaCrossConnectionsBuilder.setFixed(true);
        zaCrossConnectionsBuilder.setDirection(LinkDirection.Unidirection);
        zaCrossConnectionsBuilder.setSourceTp(Collections.singletonList(
                new SourceTpBuilder().setTpRef(TpId.getDefaultInstance(alogicalMpoId)).build()));
        zaCrossConnectionsBuilder.setDestinationTp(Collections.singletonList(
                new DestinationTpBuilder().setTpRef(TpId.getDefaultInstance(tpId)).build()));
        zaCrossConnectionsBuilder.setDescription(LOGIC_DESCRIPTION_XC);
        return Arrays.asList(azCrossConnectionsBuilder.build(), zaCrossConnectionsBuilder.build());
    }

    private String getAddDropLinkMuxId(List<String> connectMuxLinkIds) {
        log.debug("get connect addDrop connect mux id by muxLinkIds:{}", connectMuxLinkIds);
        if (CollectionUtils.isEmpty(connectMuxLinkIds)) {
            return null;
        }
        String phyLinkId = connectMuxLinkIds.get(0);
        Link phyLink = netconfTopology.getPhyLink(phyLinkId);
        String sourceTpId = phyLink.getSource().getSourceTp().getValue();
        String sourceEqId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
        String destTpId = phyLink.getDestination().getDestTp().getValue();
        String destEqId = PhysicalTpIdNamingRule.getEquipId(destTpId);

        Equipments sourceEquipment = netconfTopology.getEquipment(sourceEqId);
        Equipments destEquipment = netconfTopology.getEquipment(destEqId);
        EquipType sourceEquipType = sourceEquipment.getEquipType();
        EquipType destEquipType = destEquipment.getEquipType();
        if (MUX_TYPES.contains(sourceEquipType)) {
            return sourceEquipment.getEquipmentId();
        }
        if (MUX_TYPES.contains(destEquipType)) {
            return destEquipment.getEquipmentId();
        }
        return null;
    }

    private PortType getTerminationPointType(TerminationPoint tp) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType();
    }


    @Override
    public String linkType() {
        return Constants.SITE_LINK;
    }

    @Data
    @AllArgsConstructor
    private static class PathNode {

        private String equipId;

        private List<ExternalLinkInfoDto> edges;

        private Set<String> visitedEquipId;
    }
}
