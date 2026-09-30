package net.flex.dci.otn.controller.nms.nms.component.thumbnail.link;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDetailDto;
import net.flex.dci.otn.controller.nms.nms.dto.thumbnail.route.ThumbnailRouteDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.SiteBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.TpBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.site.SiteHopBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.sequence.RouteSequenceBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.sequence.RouteSequenceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OchLinkWithoutSiteLinkRoute {

    @Autowired
    protected SiteNodeDao siteNodeDao;

    public ThumbnailRouteDto checkingThumbnailRouteWithoutSiteLink(Link ochLink) {
        if (ochLink == null) {
            log.error("find the tunnel hasn't ochLink supported");
            return null;
        }
        List<SupportingLink> supportingLink = ochLink.getSupportingLink();
        if (supportingLink.stream().anyMatch(x -> x.getLinkRef().getValue().startsWith("SiteLink"))) {
            return null;
        }
        List<RouteSequence> primarySequences = new ArrayList<>();
        TopologyId phyTopoId = new TopologyId(Constants.PHY_TOPO_KEY);

        String srcSiteId = PhysicalNodeIdNamingRule.getSiteId(ochLink.getSource().getSourceNode().getValue());
        String dstSiteId = PhysicalNodeIdNamingRule.getSiteId(ochLink.getDestination().getDestNode().getValue());

        Node site = siteNodeDao.getSiteNodeById(srcSiteId);
        Site siteAttr = site.getAugmentation(Node1.class).getSite();
        primarySequences.add(new RouteSequenceBuilder()
                .setSequence(1L)
                .setKey(new RouteSequenceKey(1L))
                .setResourceType(new SiteBuilder()
                        .setSiteHop(
                                new SiteHopBuilder()
                                        .setSiteId(site.getNodeId())
                                        .setSiteType(LinkTerminationNodeType.valueOf(siteAttr.getSiteType().name()))
                                        .setSiteFriendlyName(siteAttr.getFriendlyName())
                                        .build())
                        .build())
                .build());

        primarySequences.add(new RouteSequenceBuilder()
                .setSequence(2L)
                .setKey(new RouteSequenceKey(2L))
                .setResourceType(new LinkBuilder()
                        .setLinkHop(
                                new LinkHopBuilder()
                                        .setTopologyRef(phyTopoId)
                                        .setLinkId(ochLink.getLinkId())
                                        .setLinkType("NORMAL")
                                        .setSource(srcSiteId)
                                        .setDestination(dstSiteId)
                                        .setFriendlyName(ochLink.getAugmentation(Link1.class).getOch().getFriendlyName())
                                        .build())
                        .build())
                .build());


        site = siteNodeDao.getSiteNodeById(dstSiteId);
        siteAttr = site.getAugmentation(Node1.class).getSite();
        primarySequences.add(new RouteSequenceBuilder()
                .setSequence(3L)
                .setKey(new RouteSequenceKey(3L))
                .setResourceType(new SiteBuilder()
                        .setSiteHop(
                                new SiteHopBuilder()
                                        .setSiteId(site.getNodeId())
                                        .setSiteType(LinkTerminationNodeType.valueOf(siteAttr.getSiteType().name()))
                                        .setSiteFriendlyName(siteAttr.getFriendlyName())
                                        .build())
                        .build())
                .build());

        return ThumbnailRouteDto.builder()
                .secondary(null)
                .primary(ThumbnailRouteDetailDto.builder()
                        .routeSequences(primarySequences)
                        .build())
                .build();
    }


}
