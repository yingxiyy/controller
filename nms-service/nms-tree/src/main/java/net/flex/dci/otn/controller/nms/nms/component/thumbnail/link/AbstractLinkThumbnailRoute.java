package net.flex.dci.otn.controller.nms.nms.component.thumbnail.link;

import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.nms.nms.component.thumbnail.LinkThumbnailRoute;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDetailDto;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteEdge;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailSequenceDto;
import net.flex.dci.otn.controller.nms.nms.enums.ThumbnailEdgeSiteType;
import net.flex.dci.otn.controller.nms.nms.enums.ThumbnailLinkType;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.ResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.SiteBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.link.LinkHop;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.site.SiteHop;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.site.SiteHopBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.sequence.RouteSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FiberType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 2022/12/6 17:02
 */
@Slf4j
public abstract class AbstractLinkThumbnailRoute implements LinkThumbnailRoute {

    @Autowired
    protected PhyLinkDao phyLinkDao;

    @Autowired
    protected SiteNodeDao siteNodeDao;

    @Autowired
    protected SiteLinkDao siteLinkDao;

    public abstract String getLINK_TYPE();

    /**
     * get detail for the route sequence
     *
     * @param thumbnailSequenceDto
     * @return
     */
    protected ThumbnailRouteDto retrieveThumbnailRouteSequence(
            ThumbnailSequenceDto thumbnailSequenceDto) {
        log.debug("retrieve the all the thumbnail ");
        ThumbnailSequenceDto primary = thumbnailSequenceDto.getPrimary();
        ThumbnailSequenceDto secondary = thumbnailSequenceDto.getSecondary();
        ThumbnailSequenceDto tertiary = thumbnailSequenceDto.getTertiary();
        HashMap<String, Node> siteNodeMap = new HashMap<>();
        List<RouteSequence> primarySequences = retrieveDetailRouteSequence(primary, siteNodeMap);
        ThumbnailRouteDetailDto primaryDetail = ThumbnailRouteDetailDto.builder()
                .routeSequences(primarySequences).build();
        ThumbnailRouteDto.ThumbnailRouteDtoBuilder thumbnailRouteDtoBuilder = ThumbnailRouteDto.builder();
        thumbnailRouteDtoBuilder.primary(primaryDetail);
        if (secondary != null) {
            List<RouteSequence> secondarySequences = retrieveDetailRouteSequence(secondary,
                    siteNodeMap);
            ThumbnailRouteDetailDto secondaryDetail = ThumbnailRouteDetailDto.builder()
                    .routeSequences(secondarySequences).build();
            thumbnailRouteDtoBuilder.secondary(secondaryDetail);
        }
        if (tertiary != null) {
            List<RouteSequence> tertiarySequences = retrieveDetailRouteSequence(tertiary,
                    siteNodeMap);
            ThumbnailRouteDetailDto tertiaryDetail = ThumbnailRouteDetailDto.builder()
                    .routeSequences(tertiarySequences).build();
            thumbnailRouteDtoBuilder.tertiary(tertiaryDetail);
        }
        return thumbnailRouteDtoBuilder.build();
    }

    /**
     * retrieve detail route sequence
     *
     * @param thumbnailSequenceDto
     * @return
     */
    protected List<RouteSequence> retrieveDetailRouteSequence(
            ThumbnailSequenceDto thumbnailSequenceDto, HashMap<String, Node> siteNodeMap) {
        log.debug("start to retrieve the route sequence");
        List<RouteSequence> routeSequences = new ArrayList<>();
        HashMap<String, LinkTerminationNodeType> siteNodeTypeMap = new HashMap<>();
        LinkedList<ResourceType> siteResourceQueue = new LinkedList<>();
        LinkedList<ResourceType> linkResourceQueue = new LinkedList<>();
        while (thumbnailSequenceDto != null) {
            Class<?> refClazz = thumbnailSequenceDto.getRefClazz();
            ResourceType resource = null;
            if (refClazz.isAssignableFrom(Tp.class)) {
                String siteId = thumbnailSequenceDto.getNodeId();
                resource = buildSiteNodeResource(siteId, siteNodeMap);
                siteResourceQueue.offer(resource);
            } else if (refClazz.isAssignableFrom(Link.class)) {
                String linkId = thumbnailSequenceDto.getLinkId();
                resource = buildLinkResource(linkId, siteNodeTypeMap);
                linkResourceQueue.offer(resource);
            }
            thumbnailSequenceDto = thumbnailSequenceDto.getPrimary();
        }
        LinkedList<ResourceType> resourceQueue = buildRouteResourceQueue(siteResourceQueue,
                linkResourceQueue);
        AtomicLong seqNo = new AtomicLong(1L);
        routeSequences = resourceQueue.stream().map(resource -> {
            RouteSequenceBuilder routeSequenceBuilder = new RouteSequenceBuilder();
            routeSequenceBuilder.setSequence(seqNo.get());
            routeSequenceBuilder.setResourceType(resource);
            seqNo.getAndIncrement();
            return routeSequenceBuilder.build();
        }).collect(Collectors.toList());
        //reset site node type
        if (!siteNodeTypeMap.isEmpty()) {
            routeSequences = resetSiteHopNodeType(routeSequences, siteNodeTypeMap);
        }
        return routeSequences;
    }

    /**
     * build route detail resource queue
     *
     * @param siteResourceQueue fixed sequence
     * @param linkResourceQueue
     * @return
     */
    private LinkedList<ResourceType> buildRouteResourceQueue(
            LinkedList<ResourceType> siteResourceQueue,
            LinkedList<ResourceType> linkResourceQueue) {
        log.debug("build route resource queue");
//        List<ThumbnailRouteEdge> edges = getThumbnailEdgeDetails(linkResourceQueue);
        LinkedList<ResourceType> routeList = new LinkedList<>();
        int tpIndex = 0;
        int linkIndex = 0;
        while (tpIndex < siteResourceQueue.size() - 1 && linkIndex < linkResourceQueue.size()) {
            ResourceType srcSiteResource = siteResourceQueue.get(tpIndex);
            ResourceType destSiteResource = siteResourceQueue.get(tpIndex + 1);
            ResourceType linkSiteResource = linkResourceQueue.get(linkIndex);
            String srcSite = ((org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.Site) srcSiteResource).getSiteHop()
                    .getSiteId().getValue();
            String destSite = ((org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.Site) destSiteResource).getSiteHop()
                    .getSiteId().getValue();
            ResourceType linkResource = rewriteLinkResource(srcSite, destSite, linkSiteResource);
            if (!routeList.contains(srcSiteResource)) {
                routeList.add(srcSiteResource);
            }
            routeList.add(linkResource);
            if (!routeList.contains(destSiteResource)) {
                routeList.add(destSiteResource);
            }
            tpIndex += 1;
            linkIndex += 1;
        }
        return routeList;
    }

    private ResourceType rewriteLinkResource(String srcSite, String destSite,
            ResourceType linkResource) {
        LinkHop linkHop = ((org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.Link) linkResource).getLinkHop();
        String linkSrc = linkHop.getSource();
        String linkDest = linkHop.getDestination();
        ResourceType linkHopResource = null;
        if (srcSite.equals(linkSrc) && destSite.equals(linkDest)) {
            linkHopResource = linkResource;
        } else {

            LinkHopBuilder linkHopBuilder = new LinkHopBuilder(linkHop);

            linkHopBuilder.setSource(linkDest);
            linkHopBuilder.setDestination(linkSrc);
            LinkBuilder linkBuilder = new LinkBuilder();
            linkBuilder.setLinkHop(linkHopBuilder.build());
            linkHopResource = linkBuilder.build();
        }
        return linkHopResource;
    }


    /**
     * get thumbnail edge details
     *
     * @param linkResourceQueue
     * @return
     */
    private List<ThumbnailRouteEdge> getThumbnailEdgeDetails(
            LinkedList<ResourceType> linkResourceQueue) {
        List<ThumbnailRouteEdge> thumbnailRouteEdges = new ArrayList<>();
        for (ResourceType resourceType : linkResourceQueue) {
            LinkHop linkHop = ((org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.Link) resourceType).getLinkHop();
            String srcSite = linkHop.getSource();
            String destSite = linkHop.getDestination();
            ThumbnailRouteEdge srcEdge = ThumbnailRouteEdge.builder().edgeSiteType(
                    ThumbnailEdgeSiteType.SOURCE_SITE).edge(resourceType).siteId(srcSite).build();
            ThumbnailRouteEdge destEdge = ThumbnailRouteEdge.builder().edgeSiteType(
                    ThumbnailEdgeSiteType.DEST_SITE).edge(resourceType).siteId(destSite).build();
            thumbnailRouteEdges.add(srcEdge);
            thumbnailRouteEdges.add(destEdge);
        }
        return thumbnailRouteEdges;
    }

    private List<RouteSequence> resetSiteHopNodeType(List<RouteSequence> routeSequences,
            HashMap<String, LinkTerminationNodeType> siteNodeTypeMap) {
        return routeSequences.stream().map(routeSequence -> {
            ResourceType resource = routeSequence.getResourceType();
            Class<?> clazz = resource.getImplementedInterface();
            if (clazz.isAssignableFrom(
                    org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.Site.class)) {
                SiteHop siteHop = ((org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.Site) resource).getSiteHop();
                String siteId = siteHop.getSiteId().getValue();
                LinkTerminationNodeType siteNodeType = siteNodeTypeMap.get(siteId);
                SiteHopBuilder siteHopBuilder = new SiteHopBuilder(siteHop);
                siteHopBuilder.setSiteType(siteNodeType);

                SiteBuilder siteBuilder = new SiteBuilder();
                siteBuilder.setSiteHop(siteHopBuilder.build());
                routeSequence = new RouteSequenceBuilder(routeSequence).setResourceType(
                        siteBuilder.build()).build();
            }
            return routeSequence;
        }).collect(Collectors.toList());
    }

    private ResourceType buildSiteNodeResource(String siteId, HashMap<String, Node> siteNodeMap) {
        log.debug("build site node resource for site id :{}", siteId);
        Node siteNode = null;
        if (siteNodeMap.containsKey(siteId)) {
            siteNode = siteNodeMap.get(siteId);
        } else {
            siteNode = siteNodeDao.getSiteNodeById(siteId);
        }
        Site sitePhysical = siteNode.getAugmentation(Node1.class).getSite();
        String siteFriendlyName = sitePhysical.getFriendlyName();
        SiteType siteType = sitePhysical.getSiteType();
        SiteHopBuilder siteHopBuilder = new SiteHopBuilder();

        siteHopBuilder.setSiteId(NodeId.getDefaultInstance(siteId));
        siteHopBuilder.setSiteFriendlyName(siteFriendlyName);
        siteHopBuilder.setSiteType(LinkTerminationNodeType.valueOf(siteType.name()));
        SiteBuilder siteBuilder = new SiteBuilder();
        siteBuilder.setSiteHop(siteHopBuilder.build());
        return siteBuilder.build();
    }

    /**
     * build link resource
     *
     * @param linkId
     * @param siteNodeTypeMap
     * @return
     */
    private ResourceType buildLinkResource(String linkId,
            HashMap<String, LinkTerminationNodeType> siteNodeTypeMap) {
        ResourceType resourceType = null;
        if (PhysicalLinkIdNamingRule.isOtsLink(linkId)) {
            resourceType = buildPhyLinkResource(linkId, siteNodeTypeMap);
        } else if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
            resourceType = buildSiteLinkResource(linkId);
        }
        return resourceType;
    }

    private ResourceType buildSiteLinkResource(String linkId) {
        log.debug("build site link resource ,for the link id is :{}", linkId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink = siteLinkDao.getSiteLinkById(
                linkId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkPhysical = siteLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
        Source siteLinkSource = siteLink.getSource();
        Destination siteLinkDest = siteLink.getDestination();
        ThumbnailLinkType linkType = getSiteLinkType(siteLinkPhysical);
        String sourceSiteId = PhysicalNodeIdNamingRule.getSiteId(
                siteLinkSource.getSourceNode().getValue());
        String destinationSiteId = PhysicalNodeIdNamingRule.getSiteId(
                siteLinkDest.getDestNode().getValue());
        String friendlyName = siteLinkPhysical.getFriendlyName();

        LinkHopBuilder linkHopBuilder = new LinkHopBuilder();
        linkHopBuilder.setLinkId(LinkId.getDefaultInstance(linkId));
        linkHopBuilder.setTopologyRef(TopologyId.getDefaultInstance(SITE_TOPO_KEY));
        linkHopBuilder.setFriendlyName(friendlyName);
        linkHopBuilder.setLinkType(linkType.name());

        linkHopBuilder.setSource(sourceSiteId);
        linkHopBuilder.setDestination(destinationSiteId);
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkHop(linkHopBuilder.build());
        return linkBuilder.build();
    }


    private ResourceType buildPhyLinkResource(String linkId,
            HashMap<String, LinkTerminationNodeType> siteNodeTypeMap) {
        log.debug("build ots link resource ,for the link id is :{}", linkId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link otsLink = phyLinkDao.getPhyLinkById(
                linkId);
        Source source = otsLink.getSource();
        Destination destination = otsLink.getDestination();
        String sourceSiteId = PhysicalNodeIdNamingRule.getSiteId(source.getSourceNode().getValue());
        String destinationSiteId = PhysicalNodeIdNamingRule.getSiteId(
                destination.getDestNode().getValue());
        Physical otsLinkPhysical = otsLink.getAugmentation(
                Link1.class).getPhysical();
        //link physical properties
        Provider otsLinkProvider = otsLinkPhysical.getProvider();
        FiberType fiberType = otsLinkProvider.getFiberType();
        BigDecimal attenuation = otsLinkProvider.getAttenuation();
        BigDecimal azDistance = otsLinkProvider.getDistanceAz();
        BigDecimal zaDistance = otsLinkProvider.getDistanceZa();
        BigDecimal distance = otsLinkProvider.getDistance();
        String friendlyName = otsLinkPhysical.getFriendlyName();
        //build linkHop
        LinkHopBuilder linkHopBuilder = new LinkHopBuilder();
        linkHopBuilder.setLinkId(LinkId.getDefaultInstance(linkId));
        linkHopBuilder.setTopologyRef(TopologyId.getDefaultInstance(PHY_TOPO_KEY));
        linkHopBuilder.setFriendlyName(friendlyName);
        linkHopBuilder.setLinkType(ThumbnailLinkType.OTS.name());
        if (Objects.nonNull(distance)) {
            linkHopBuilder.setDistance(distance.intValue());
        }
        linkHopBuilder.setDistanceAz(azDistance.intValue());
        linkHopBuilder.setDistanceZa(zaDistance.intValue());
        linkHopBuilder.setAttenuation(attenuation);
        linkHopBuilder.setSource(sourceSiteId);
        linkHopBuilder.setDestination(destinationSiteId);
        linkHopBuilder.setFiberType(fiberType);
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkHop(linkHopBuilder.build());
        //update site node type
        updateSiteNodeType(sourceSiteId, destinationSiteId, siteNodeTypeMap, otsLinkPhysical);
        return linkBuilder.build();
    }

    private void updateSiteNodeType(String sourceSiteId, String destinationSiteId,
            HashMap<String, LinkTerminationNodeType> siteNodeTypeMap, Physical otsLinkPhysical) {
        String sourceSiteType = PropertyTool.getValue(otsLinkPhysical.getProperties(),
                Constants.PROPERTY_SOURCE_SITE_TYPE);
        String destinationSiteType = PropertyTool.getValue(otsLinkPhysical.getProperties(),
                Constants.PROPERTY_DEST_SITE_TYPE);
        siteNodeTypeMap.put(sourceSiteId, LinkTerminationNodeType.valueOf(sourceSiteType));
        siteNodeTypeMap.put(destinationSiteId,
                LinkTerminationNodeType.valueOf(destinationSiteType));
    }


    private ThumbnailLinkType getSiteLinkType(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkPhysical) {
        String siteLinkModel = PropertyTool.getValue(siteLinkPhysical.getProperties(),
                Constants.SITE_LINK_MODE);
        return ThumbnailLinkType.getThumbnailLinkType(Integer.parseInt(siteLinkModel));
    }
}
