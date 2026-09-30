package net.flex.dci.otn.controller.nms.nms.component.thumbnail;

import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.design.DesignRouteObject;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDetailDto;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;
import net.flex.dci.otn.controller.nms.nms.enums.ThumbnailLinkType;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.ResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.SiteBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.site.SiteHopBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.sequence.RouteSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 7/15/2025 4:29 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DesignThumbnailRouteImpl implements DesignThumbnailRoute {


    private final SiteLinkDao siteLinkDao;

    private final SiteNodeDao siteNodeDao;

    /**
     * get design thumbnail sequence by site link id
     *
     * @param primary
     * @param secondary
     * @param tertiary
     * @return
     */
    @Override
    public ThumbnailRouteDto getDesignThumbnailSequence(List<String> primary,
            List<String> secondary, List<String> tertiary) {
        log.debug(
                "get design thumbnail sequence by primary siteLinkIds:{} secondary siteLinkIds:{} tertiary siteLinkIds:{}",
                primary, secondary, tertiary);
        if (CollectionUtils.isEmpty(primary)) {
            log.error("primary siteLinkIds should by existed");
            return null;
        }
        List<DesignRouteObject> primaryRouteObjects = retrieveDesignRouteObject(primary);
        ThumbnailRouteDetailDto primaryDetail = buildThumbnailRouteDetail(primaryRouteObjects);
        List<DesignRouteObject> secondaryRouteObjects = retrieveDesignRouteObject(
                secondary);
        ThumbnailRouteDetailDto secondaryDetail = buildThumbnailRouteDetail(secondaryRouteObjects);
        List<DesignRouteObject> tertiaryRouteObjects = retrieveDesignRouteObject(tertiary);
        ThumbnailRouteDetailDto tertiaryDetail = buildThumbnailRouteDetail(tertiaryRouteObjects);
        return ThumbnailRouteDto.builder().primary(primaryDetail).secondary(secondaryDetail)
                .tertiary(tertiaryDetail).build();
    }

    private ThumbnailRouteDetailDto buildThumbnailRouteDetail(
            List<DesignRouteObject> routeObjects) {
        if (CollectionUtils.isEmpty(routeObjects)) {
            log.warn("build thumbnail route detail routeObjects is null,discard it");
            return null;
        }
        AtomicLong seqNo = new AtomicLong(1L);
        List<RouteSequence> simpleRouteSequence = routeObjects.stream().map(routeObject -> {
                    String id = routeObject.getId();
                    RetrieveType objectType = routeObject.getType();
                    RouteSequenceBuilder routeSequenceBuilder = new RouteSequenceBuilder();
                    if (objectType.equals(RetrieveType.SITE_NODE)) {
                        ResourceType siteResource = buildSiteResource(id);
                        routeSequenceBuilder.setSequence(seqNo.getAndIncrement());
                        routeSequenceBuilder.setResourceType(siteResource);
                    } else if (objectType.equals(RetrieveType.SITE_LINK)) {
                        ResourceType siteLinkResource = buildSiteLinkResource(id);
                        routeSequenceBuilder.setSequence(seqNo.getAndIncrement());
                        routeSequenceBuilder.setResourceType(siteLinkResource);
                    }
                    return routeSequenceBuilder.build();
                })
                .collect(Collectors.toList());
        return ThumbnailRouteDetailDto.builder().routeSequences(simpleRouteSequence).build();
    }

    private ResourceType buildSiteLinkResource(String id) {
        log.debug("build site link Resource by id:{}", id);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink = siteLinkDao.getSiteLinkById(
                id);
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
        linkHopBuilder.setLinkId(LinkId.getDefaultInstance(id));
        linkHopBuilder.setTopologyRef(TopologyId.getDefaultInstance(SITE_TOPO_KEY));
        linkHopBuilder.setFriendlyName(friendlyName);
        linkHopBuilder.setLinkType(linkType.name());

        linkHopBuilder.setSource(sourceSiteId);
        linkHopBuilder.setDestination(destinationSiteId);
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkHop(linkHopBuilder.build());
        return linkBuilder.build();
    }

    private ThumbnailLinkType getSiteLinkType(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkPhysical) {
        String siteLinkModel = PropertyTool.getValue(siteLinkPhysical.getProperties(),
                Constants.SITE_LINK_MODE);
        return ThumbnailLinkType.getThumbnailLinkType(Integer.parseInt(siteLinkModel));
    }

    private ResourceType buildSiteResource(String id) {
        log.debug("build site Resource by id:{}", id);
        Node site = siteNodeDao.getSiteNodeById(id);
        Site sitePhysical = site.getAugmentation(Node1.class).getSite();
        String siteFriendlyName = sitePhysical.getFriendlyName();
        SiteType siteType = sitePhysical.getSiteType();
        SiteHopBuilder siteHopBuilder = new SiteHopBuilder();
        siteHopBuilder.setSiteId(NodeId.getDefaultInstance(id));
        siteHopBuilder.setSiteFriendlyName(siteFriendlyName);
        siteHopBuilder.setSiteType(LinkTerminationNodeType.valueOf(siteType.name()));
        SiteBuilder siteBuilder = new SiteBuilder();
        siteBuilder.setSiteHop(siteHopBuilder.build());
        siteBuilder.setSiteHop(siteHopBuilder.build());
        return siteBuilder.build();
    }

    private List<DesignRouteObject> retrieveDesignRouteObject(List<String> siteLinkIds) {
        log.debug("retrieve design route object :{}", siteLinkIds);
        LinkedList<DesignRouteObject> routeObjectStack = new LinkedList<>();
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return new ArrayList<>();
        }
        for (String siteLinkId : siteLinkIds) {
            String srcSiteId = SiteLinkIdNamingRule.getSiteA(siteLinkId);
            DesignRouteObject srcSiteObject = DesignRouteObject.builder().id(srcSiteId).type(
                    RetrieveType.SITE_NODE).build();
            pushDesignRouteObject(routeObjectStack, srcSiteObject);
            DesignRouteObject siteLinkObject = DesignRouteObject.builder().id(siteLinkId)
                    .type(RetrieveType.SITE_LINK).build();
            pushDesignRouteObject(routeObjectStack, siteLinkObject);
            String destSiteId = SiteLinkIdNamingRule.getSiteZ(siteLinkId);

            DesignRouteObject destSiteObject = DesignRouteObject.builder().id(destSiteId)
                    .type(RetrieveType.SITE_NODE).build();
            pushDesignRouteObject(routeObjectStack, destSiteObject);
        }
        Iterator<DesignRouteObject> routeObjectIterator = routeObjectStack.descendingIterator();
        List<DesignRouteObject> result = new ArrayList<>();
        while (routeObjectIterator.hasNext()) {
            result.add(routeObjectIterator.next());
        }
        return result;
    }

    private void pushDesignRouteObject(LinkedList<DesignRouteObject> routeObjectStack,
            DesignRouteObject routeObject) {
        log.debug("push route Object :{}", routeObject);
        DesignRouteObject peekRouteObject = routeObjectStack.peek();

        if (routeObjectStack.isEmpty() || peekRouteObject.getType() != routeObject.getType()
                || !peekRouteObject.getId()
                .equals(routeObject.getId())) {
            routeObjectStack.push(routeObject);
        }
    }
}
