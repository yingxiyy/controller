package net.flex.dci.otn.controller.nms.nms.component.route.link;

import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/6/29 16:24
 */
@Component
@Slf4j
public class OchLinkRouteSequenceRetriever extends AbstractRouteSequenceRetriever {

    private static final Set<NodeType> VALID_NODE_TYPES = ImmutableSet.of(NodeType.OPC4,
            NodeType.OD);
    @Autowired
    private EquipmentsDao equipmentsDao;

    /**
     * retrieve och link route sequence support ochp
     *
     * @param pathRouteObject
     * @param source
     * @param destination
     * @return
     */
    @Override
    public RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject,
            String source,
            String destination) {
        log.debug("start to retrieve the route for the och link");
        LinkHop linkHop = ((Link) pathRouteObject.getResourceType()).getLinkHop();
        TopologyId topologyRef = linkHop.getTopologyRef();
        String linkId = linkHop.getLinkRef().getValue();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link ochLink
                = netconfTopology.getOchLink(linkId);
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "invalid route data,please contact administrator");
        }
        Link1 ochLinkAttr = ochLink.getAugmentation(Link1.class);
        List<Route> ochRoutes = ochLinkAttr.getOch()
                .getExplictRoute().getRoute();
        RouteSequenceDto routeSequenceDto = new RouteSequenceDto();
        for (Route route : ochRoutes) {
            Primary primaryRoute = route.getPrimary();
            Secondary secondaryRoute = route.getSecondary();
            List<Third> tertiary = route.getThird();
            List<PathRouteObject> primaryPathRouteObjects = getPathRoutes(
                    primaryRoute.getExplicitRouteObjects());
            RouteSequenceDto primarySequenceDto = retrieveRoute(
                    primaryPathRouteObjects, primaryRoute.getCrossConnections(),
                    source, destination);

            routeSequenceDto.setPrimary(primarySequenceDto);
            if (secondaryRoute != null) {
                List<PathRouteObject> secondaryPathRoute = getPathRoutes(
                        secondaryRoute.getExplicitRouteObjects());
                RouteSequenceDto secondarySequenceDto = retrieveRoute(
                        secondaryPathRoute,
                        secondaryRoute.getCrossConnections(), source, destination);
                routeSequenceDto.setSecondary(secondarySequenceDto);
            }
            if (!CollectionUtils.isEmpty(tertiary)) {
                List<RouteSequenceDto> tertiaryRouteSequenceDto = retrieveTertiaryRoute(tertiary,
                        source, destination);
                if (!CollectionUtils.isEmpty(tertiaryRouteSequenceDto)) {
                    routeSequenceDto.setTertiary(tertiaryRouteSequenceDto);
                }
            }

        }
        //generate mux panel cross connection

//        List<CrossConnections> xcs = new ArrayList<>(
//                generateMuxPanelXc(ochLink.getSupportingLink(),
//                        och.getLowerFrequency(), och.getUpperFrequency()));

        RouteSequenceDto rootRouteSequenceDto = new RouteSequenceDto();
        rootRouteSequenceDto.setTopologyRef(topologyRef.getValue());
        rootRouteSequenceDto.setLinkId(linkId);
        rootRouteSequenceDto.setSubSequenceDto(routeSequenceDto);
//        rootRouteSequenceDto.setXcs(new ArrayList<>());
        rootRouteSequenceDto.setXcIds(new ArrayList<>());
        return rootRouteSequenceDto;
    }

    private List<RouteSequenceDto> retrieveTertiaryRoute(List<Third> tertiary, String source,
            String destination) {
        log.debug("retrieve tertiary route sequence detail,tertiary size:{}", tertiary.size());
        List<RouteSequenceDto> routeSequences = new ArrayList<>();
        for (Third third : tertiary) {
            List<PathRouteObject> pathRoute = getPathRoutes(third.getExplicitRouteObjects());
            RouteSequenceDto routeSequenceDto = retrieveRoute(pathRoute,
                    third.getCrossConnections(), source, destination);
            if (routeSequenceDto != null) {
                routeSequences.add(routeSequenceDto);
            }
        }
        return routeSequences;
    }

    private NeYangModel getNeYangmodel(List<SupportingLink> supportingLink) {
        log.debug("get och link support ne yang model");
        Optional<SupportingLink> supportingLinkOptional = supportingLink.stream()
                .filter(link -> PhysicalLinkIdNamingRule.isOsLink(link.getLinkRef().getValue()))
                .findAny();
        if (!supportingLinkOptional.isPresent()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "current och link supporting link data is error,there is no available osLink data is invalid");
        }
        SupportingLink osLink = supportingLinkOptional.get();
        String osLinkId = osLink.getLinkRef().getValue();
        String desNodeId = PhysicalLinkIdNamingRule.getNodeZId(osLinkId);
        String srcNodeId = PhysicalLinkIdNamingRule.getNodeAId(osLinkId);
        Node srcNode = netconfTopology.getNeNode(srcNodeId);
        Node desNode = netconfTopology.getNeNode(desNodeId);
        CompletableFuture<NeYangModel> srcFuture = CompletableFuture.supplyAsync(
                () -> getModelIfValid(srcNode));
        CompletableFuture<NeYangModel> desFuture = CompletableFuture.supplyAsync(
                () -> getModelIfValid(desNode));
        return desFuture.thenCombine(srcFuture, (z, a) ->
                        z != null ? z : (a != null ? a : NeYangModel.Tencent))
                .join();

    }

    private NeYangModel getModelIfValid(Node node) {
        if (node == null) {
            return null;
        }

        Node1 augmentation = node.getAugmentation(Node1.class);
        if (augmentation == null) {
            return null;
        }

        Physical nodeAttr = augmentation.getPhysical();
        if (nodeAttr == null) {
            return null;
        }

        NodeType nodeType = nodeAttr.getNodeType();
        if (VALID_NODE_TYPES.contains(nodeType)) {
            return NeYangModel.getModel(nodeAttr);
        }

        return null;
    }

    private List<CrossConnections> generateMuxPanelXc(
            List<SupportingLink> supportingLinks,
            FrequencyType lower, FrequencyType upper) {
        NeYangModel model = getNeYangmodel(supportingLinks);
        List<CrossConnections> muxChannelXCs = new LinkedList<>();
        List<String> muxPanelRefLinkIds = supportingLinks.stream()
                .filter(supportingLink -> isMuxPanelLink(model,
                        supportingLink.getLinkRef().getValue()))
                .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
                        Collectors.toList());
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> muxPhyLinks = netconfTopology.getPhyLinksByIds(
                muxPanelRefLinkIds);
        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> muxPhyLinkMap = muxPhyLinks.stream()
                .collect(Collectors.toMap(l -> l.getLinkId().getValue(), l -> l));

        for (String refLinkId : muxPanelRefLinkIds) {
            log.debug("get ref phy link id is:{}", refLinkId);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link muxPhyLink = muxPhyLinkMap.get(
                    refLinkId);
            if (muxPhyLink != null) {
                String sourceTpId = muxPhyLink.getSource().getSourceTp().getValue();
                String destTpId = muxPhyLink.getDestination().getDestTp().getValue();
                Equipments refMuxPanelEq = getMuxPanelEq(model, sourceTpId, destTpId);
                if (refMuxPanelEq != null && refMuxPanelEq.getEquipType()
                        .equals(EquipType.MUXPANEL)) {
                    PhyLinkTpInfo phyLinkTpInfo = realizePhyLinkTpInfo(model, sourceTpId,
                            destTpId);
                    CrossConnections crossConnections = CrossConnectionUtils.generateMuxCrossConnection(
                            phyLinkTpInfo.source, phyLinkTpInfo.dest, lower, upper);
                    muxChannelXCs.add(crossConnections);
                }
            }
        }
        return muxChannelXCs;
//        for (SupportingLink slink : supportingLinks) {
//            String regex = "M\\d+D\\d+";
//            Pattern pattern = Pattern.compile(regex);
//            Matcher matcher = pattern.matcher(slink.getLinkRef().getValue());
//            if (matcher.find()) {
//                index++;
//                log.info("slink.getLinkRef().getValue() is {}", slink.getLinkRef().getValue());
//                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link ntLink =
//                        netconfTopology
//                                .getPhyLink(slink.getLinkRef().getValue());
//
//                TpId srcTp = ntLink.getSource().getSourceTp();
//                TpId dstTp = ntLink.getDestination().getDestTp();
//                matcher = pattern.matcher(srcTp.getValue());
//                if (matcher.find()) {
//                    dstTp = new TpId(LAG2MPO(srcTp.getValue()));
//                } else {
//                    matcher = pattern.matcher(dstTp.getValue());
//                    if (matcher.find()) {
//                        srcTp = new TpId(LAG2MPO(dstTp.getValue()));
//                    }
//                }
//                String frequency =
//                        "/frequency=" + lower.getValue().toString() + "," + upper.getValue()
//                                .toString();
//                long centorFrequency =
//                        (lower.getValue().longValue() + upper.getValue().longValue()) / 2L;
//                Uri xcId = new Uri(
//                        "XC-" + srcTp.getValue() + "/frequency=" + centorFrequency + "-" + dstTp
//                                .getValue() + "/frequency=" + centorFrequency);
//
//                List<SourceTp> srcTps = new LinkedList<>();
//                srcTps.add(new SourceTpBuilder()
//                        .setTpRef(srcTp)
//                        .setSlot(frequency)
//                        .build());
//
//                List<DestinationTp> dstTps = new LinkedList<>();
//                dstTps.add(new DestinationTpBuilder()
//                        .setTpRef(dstTp)
//                        .setSlot(frequency)
//                        .build());
//                NodeId refNodeId = NodeId.getDefaultInstance(
//                        PhysicalTpIdNamingRule.getNodeId(srcTps.get(0).getTpRef().getValue()));
//                CrossConnectionsBuilder cb = new CrossConnectionsBuilder()
//                        .setNodeRef(refNodeId)
//                        .setSourceTp(srcTps)
//                        .setDestinationTp(dstTps)
//                        .setAdminState(AdminStatus.Up)
//                        .setDirection(LinkDirection.Bidirection)
//                        .setFixed(true)
//                        .setImplementState(ImplementState.Implement)
//                        .setNodeRef(ntLink.getSource().getSourceNode())
//                        .setOperationalState(OperStatus.Up)
//                        .setCrossConnectionId(xcId)
//                        .setKey(new CrossConnectionsKey(index));
//
//                newXcs.add(cb.build());
//            }
//        }
//        return newXcs;
    }

    private Equipments getMuxPanelEq(NeYangModel neYangModel, String sourceTpId,
            String destTpId) {
        log.debug("to find the mux panel card");

        Pattern pattern = Pattern.compile(neYangModel.muxPortMatchingRegex());
        Matcher matcherSrc = pattern.matcher(sourceTpId);
        Matcher matcherDest = pattern.matcher(destTpId);
        String muxCardId = null;
        if (matcherDest.find() && !matcherSrc.find()) {
            muxCardId = PhysicalTpIdNamingRule.getEquipId(destTpId);
        } else if (!matcherDest.find() && matcherSrc.find()) {
            muxCardId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
        }
        log.debug("the mux card id is :{}", muxCardId);
        if (muxCardId == null) {
            return null;
        }
        String nodeId = PhysicalEqpIdNamingRule.getNodeId(muxCardId);
        Equipments refMux = equipmentsDao.getEquipmentByNodeAndEqId(nodeId, muxCardId);
        return refMux;
    }

    private PhyLinkTpInfo realizePhyLinkTpInfo(NeYangModel neYangModel, String sourceTpId,
            String destTpId) {

        Pattern pattern = Pattern.compile(neYangModel.muxPortMatchingRegex());
        Matcher matcherSrc = pattern.matcher(sourceTpId);
        Matcher matcherDest = pattern.matcher(destTpId);
        String source = null;
        String dest = null;
        if (matcherSrc.find()) {
            source = sourceTpId;
            dest = generateMpoFromMUXId(neYangModel, source);
        } else if (matcherDest.find()) {
            dest = destTpId;
            source = generateMpoFromMUXId(neYangModel, dest);
        }

        return PhyLinkTpInfo.builder().source(source).dest(dest).build();
    }

    private String generateMpoFromMUXId(NeYangModel neYangModel, String muxTpId) {
        String[] tpKeys = muxTpId.split(neYangModel.muxChannelIdKeyword());
        String muxNum = tpKeys[tpKeys.length - 1];
        Integer mpoNum = (Integer.parseInt(muxNum)) / 8 + 1;
        return muxTpId.replaceFirst(neYangModel.muxPortMatchingRegex(), MPO_SUFFIX + mpoNum);
    }

    private boolean isMuxPanelLink(NeYangModel neYangModel, String linkId) {
        Pattern pattern = Pattern.compile(neYangModel.muxPortMatchingRegex());
        Matcher matcher = pattern.matcher(linkId);
        return matcher.find();
    }

    @Data
    @Builder
    private static class PhyLinkTpInfo {

        private String source;

        private String dest;
    }

}
