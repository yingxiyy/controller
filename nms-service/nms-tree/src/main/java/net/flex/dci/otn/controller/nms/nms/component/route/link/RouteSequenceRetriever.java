package net.flex.dci.otn.controller.nms.nms.component.route.link;

import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_FIST_INDEX;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.cache.NmsCacheManager;
import net.flex.dci.otn.controller.nms.constructs.CMUX64Constructor;
import net.flex.dci.otn.controller.nms.constructs.MUXPANELConstructor;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.nms.enums.RouteHopType;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.TpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.TpHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.SiteNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/6/29 16:27
 */
@Slf4j
@Component
public class RouteSequenceRetriever {

    @Autowired
    private PhyLinkRouteSequenceRetriever phyLinkRouteSequenceRetriever;

    @Autowired
    private OchLinkRouteSequenceRetriever ochLinkRouteSequenceRetriever;

    @Autowired
    private SiteLinkRouteSequenceRetriever siteLinkRouteSequenceRetriever;

    @Autowired
    private NetconfTopology netconfTopology;

    @Autowired
    private NmsCacheManager nmsCacheManager;


    public RouteSequenceDto retrieveTpHopRouteSequence(PathRouteObject pathRouteObject,
            RouteSequenceDto routeSequenceDto) {
        ResourceType resourceType = pathRouteObject.getResourceType();
        String tpId = ((Tp) resourceType).getTpHop().getTpRef().getValue();
        TopologyId topologyId = pathRouteObject.getTopologyRef();
        RouteSequenceDto routeSequence = new RouteSequenceDto();
        routeSequence.setTpId(tpId);
        routeSequence.setTopologyRef(topologyId.getValue());
        routeSequence.setRouteHopType(RouteHopType.fromClazz(Tp.class));
        routeSequenceDto.setPrimary(routeSequence);
        routeSequenceDto = routeSequence;
        return routeSequenceDto;
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType convertTpHop2DetailResource(
            Tp tpHop) {
        String tpId = tpHop.getTpHop().getTpRef().getValue();
        if (!PhysicalTpIdNamingRule.isTpId(tpId)) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the route info is invalided");
        }
        String siteId = PhysicalTpIdNamingRule.getSiteId(tpId);
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);

        Node phyNode = netconfTopology.getNeNode(neId);
        Node siteNode = netconfTopology.getSiteNode(siteId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1 phyNodePhysical = phyNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class);

        List<TerminationPoint> terminationPoints = phyNode.getTerminationPoint();
        Map<String, TerminationPoint> terminationPointMap = terminationPoints.stream().collect(
                Collectors.toMap(terminationPoint -> terminationPoint.getTpId().getValue(),
                        terminationPoint -> terminationPoint));
        Map<String, Equipments> equipmentsMap = phyNodePhysical.getPhysical()
                .getEquipments()
                .stream()
                .collect(Collectors.toMap(Equipments::getEquipmentId, equipments -> equipments));

        Equipments refEquipments = equipmentsMap.get(equipId);
        EquipType equipType = refEquipments.getEquipType();

        TerminationPoint terminationPoint = getRefTerminationPoint(terminationPointMap, tpId,
                equipType);

        Node extractSiteNode = extractSiteNode(siteNode);
        SiteNodeBuilder siteNodeBuilder = new SiteNodeBuilder();
        siteNodeBuilder.fieldsFrom(extractSiteNode);
        siteNodeBuilder.fieldsFrom(extractSiteNode.getAugmentation(Node1.class));

        PhyTpBuilder phyTpBuilder = new PhyTpBuilder();
        phyTpBuilder.fieldsFrom(terminationPoint);
        phyTpBuilder.fieldsFrom(terminationPoint.getAugmentation(TerminationPoint1.class));

        Node drawNode = drawNode(phyNode, refEquipments, terminationPoint);
        PhyNodeBuilder phyNodeBuilder = new PhyNodeBuilder();
        phyNodeBuilder.fieldsFrom(drawNode);
        phyNodeBuilder.fieldsFrom(drawNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class));

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.ResourceType resourceType = new TpBuilder()
                .setTpHop(new TpHopBuilder()
                        .setSiteNode(siteNodeBuilder.build())
                        .setPhyNode(phyNodeBuilder.build())
                        .setPhyTp(phyTpBuilder.build())
                        .build())
                .build();
        return resourceType;
    }

    private TerminationPoint getRefTerminationPoint(
            Map<String, TerminationPoint> terminationPointMap, String tpId, EquipType equipType) {
        TerminationPoint terminationPoint = terminationPointMap.get(tpId);
        if (terminationPoint == null && tpId.endsWith(MPO_SUFFIX)) {
            switch (equipType) {
                case CMUX64:
                    tpId = tpId + MPO_FIST_INDEX;
                    terminationPoint = CMUX64Constructor.virtualizeMPOTP(
                            terminationPointMap.get(tpId),
                            tpId);
                    break;
                case MUXPANEL:
                    tpId = tpId + MPO_FIST_INDEX;
                    terminationPoint = MUXPANELConstructor.virtualizeMPOTP(
                            terminationPointMap.get(tpId),
                            tpId);
                    break;
            }
        }

        return terminationPoint;
    }

    private Node drawNode(Node phyNode, Equipments equipments, TerminationPoint terminationPoint) {
        Physical physical = phyNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                .getPhysical();

        List<Equipments> eqList = new LinkedList<>();
        eqList.add(equipments);

        Node1Builder node1Builder = new Node1Builder();
        node1Builder.setPhysical(
                new PhysicalBuilder(physical)
                        .setCrossConnections(null)
                        .setInternalLinks(null)
                        .setEquipments(eqList)
                        .setOCMGripGroups(null)
                        .setSystem(null)
                        .build());
        return new NodeBuilder().setNodeId(phyNode.getNodeId()).addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class,
                node1Builder.build()).setTerminationPoint(
                Collections.singletonList(terminationPoint)).build();
    }

    private Node extractSiteNode(Node siteNode) {
        NodeBuilder nodeBuilder = new NodeBuilder(siteNode);
        Node1 siteNodePhysical = filterSiteData(
                siteNode);
        nodeBuilder.setSupportingNode(null);
        nodeBuilder.addAugmentation(
                Node1.class,
                siteNodePhysical);

        return nodeBuilder.build();
    }

    private Node1 filterSiteData(
            Node siteTopoNode) {
        Node1 yangSite =
                siteTopoNode.getAugmentation(
                        Node1.class);

        SiteBuilder sb = new SiteBuilder(yangSite.getSite());
        sb.setSupportingRack(null);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder nb = new
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder();
        nb.setSite(sb.build());
        return nb.build();
    }


    public RouteSequenceDto retrieveLinkRouteSequence(PathRouteObject pathRouteObject,
            RouteSequenceDto routeSequenceDto, String source,
            String destination) {

        String topologyRef = pathRouteObject.getTopologyRef().getValue();
        log.debug("get pathRouteObject for the link {}", topologyRef);

        RouteSequenceDto linkRouteSequence = null;
        if (topologyRef.equals(Constants.PHY_TOPO_KEY)) {
            linkRouteSequence = phyLinkRouteSequenceRetriever.retrieveLinkRouteSequence(
                    pathRouteObject, source, destination);
        } else if (topologyRef.equals(Constants.OCH_TOPO_KEY)) {
            LinkHop linkHop = ((Link) pathRouteObject.getResourceType()).getLinkHop();
            String linkId = linkHop.getLinkRef().getValue();
            //load from cache
            linkRouteSequence = nmsCacheManager.getOchLinkRoute(linkId);
            if (linkRouteSequence == null) {
                linkRouteSequence = ochLinkRouteSequenceRetriever.retrieveLinkRouteSequence(
                        pathRouteObject, source, destination);
                nmsCacheManager.setOchLinkRoute(linkId, linkRouteSequence);
            }
        } else if (topologyRef.equals(Constants.SITE_TOPO_KEY)) {
            //assume the current route sequence dto is tp id
//            String srcSite = PhysicalTpIdNamingRule.getSiteId(routeSequenceDto.getTpId());
            linkRouteSequence = siteLinkRouteSequenceRetriever.retrieveBriefRouteSequence(
                    pathRouteObject);
            //special route method for the site link route
//            linkRouteSequence = siteLinkRouteSequenceRetriever.retrieveLinkRouteSequence(
//                    pathRouteObject, srcSite);
        }
        assert linkRouteSequence != null;
        routeSequenceDto = _retrieveLinkRouteSequence(routeSequenceDto, linkRouteSequence);
//        routeSequenceDto.setPrimary(linkRouteSequence);
//        routeSequenceDto = linkRouteSequence;
        return routeSequenceDto;
    }

    /**
     * @param routeSequenceDto
     * @param linkRouteSequence
     * @return
     */
    private RouteSequenceDto _retrieveLinkRouteSequence(RouteSequenceDto routeSequenceDto,
            RouteSequenceDto linkRouteSequence) {
        if (linkRouteSequence.getSubSequenceDto() == null) {
            routeSequenceDto.setPrimary(linkRouteSequence);
            routeSequenceDto = linkRouteSequence;
        } else {
            //link node retrieve

            RouteSequenceDto linkRouteSubSequenceDto = linkRouteSequence.getSubSequenceDto();
            RouteSequenceDto primarySequences = linkRouteSubSequenceDto.getPrimary();
            RouteSequenceDto secondarySequences = linkRouteSubSequenceDto.getSecondary();
            List<RouteSequenceDto> tertiarySequences = linkRouteSubSequenceDto.getTertiary();
//            List<CrossConnections> xcs = linkRouteSequence.getXcs();
            List<String> xcIds = linkRouteSubSequenceDto.getXcIds();
            if (!xcIds.isEmpty()) {
                primarySequences.getXcIds().addAll(xcIds);
            }

            if (secondarySequences != null) {
                routeSequenceDto.setSecondary(secondarySequences);
            }
            if (!CollectionUtils.isEmpty(tertiarySequences)) {
                routeSequenceDto.setTertiary(tertiarySequences);
            }
            while (primarySequences != null) {
                routeSequenceDto.setPrimary(primarySequences);
                routeSequenceDto = primarySequences;
                primarySequences = primarySequences.getPrimary();
            }
        }
        return routeSequenceDto;
    }


    public RouteSequenceDto retrievePhyLinkRouteSequence(PathRouteObject pro,
            RouteSequenceDto routeSequenceDto) {
        RouteSequenceDto phyLinkRouteSequence = phyLinkRouteSequenceRetriever.retrieveLinkRouteSequence(
                pro);
        routeSequenceDto = _retrieveLinkRouteSequence(routeSequenceDto, phyLinkRouteSequence);
//        routeSequenceDto.setPrimary(linkRouteSequence);
//        routeSequenceDto = linkRouteSequence;
        return routeSequenceDto;
    }
}
