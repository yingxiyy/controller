package net.flex.dci.otn.controller.nms.nms.component.viewTopo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.nms.constructs.ViewLinkPanelConstructor;
import net.flex.dci.otn.controller.nms.nms.dto.PlaneViewInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.viewlink.ViewLinkPanelDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.Topology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 1/21/2024 4:56 PM
 */
@Component
@Slf4j
public class SiteLinkView extends AbstractViewLink {


    @Override
    public List<Topology> getRefViewLinks() {
        log.debug("get os link view link");
        List<Link> siteLinkViewLinks = topology.listAllViewLinkByLinkLevel(
                supportViewLinkType().getLinkType());
        Map<String, PlaneViewInfoDto> planeLinkMap = getRefViewSiteLinkPlaned(siteLinkViewLinks);
        List<Topology> topologies = ViewLinkPanelConstructor.buildTopologies(planeLinkMap);

        return topologies;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> getRefViewLinkInfoByMultiplexLink(
            String plane, List<Link> siteLinks,
            List<Link> ochLinks) {
        log.debug("start to get ref view link info by Multiplex Link");
        List<String> siteLinkIds = siteLinks.stream().map(Link::getLinkId).map(Uri::getValue)
                .collect(
                        Collectors.toList());
        List<String> ochLinkIds = ochLinks.stream().map(Link::getLinkId).map(Uri::getValue).collect(
                Collectors.toList());
        List<String> refLinkIds = Stream.concat(siteLinkIds.stream(), ochLinkIds.stream())
                .collect(Collectors.toList());
//        List<String> refSiteIds = getSiteNodeIdsByMultiplexLink(plane, siteLinks, ochLinks);
        List<String> refSiteIds = Stream.concat(Stream.concat(siteLinks.stream()
                        .map(siteLink -> PhysicalNodeIdNamingRule.getSiteId(
                                siteLink.getSource().getSourceNode().getValue())), siteLinks.stream()
                        .map(siteLink -> PhysicalNodeIdNamingRule.getSiteId(
                                siteLink.getDestination().getDestNode().getValue()))),
                Stream.concat(ochLinks.stream()
                        .map(link -> PhysicalNodeIdNamingRule.getSiteId(
                                link.getSource().getSourceNode().getValue())), siteLinks.stream()
                        .map(link -> PhysicalNodeIdNamingRule.getSiteId(
                                link.getDestination().getDestNode().getValue())))).collect(
                Collectors.toList());
        List<Link> viewLinks = topology.getRefViewLinkByLinkIds(refLinkIds,
                supportViewLinkType().getLinkType());
        PlaneViewInfoDto planeViewInfoDto = buildViewInfoDto(plane,
                viewLinks, refSiteIds
        );
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> topologies = ViewLinkPanelConstructor.buildByPlaneTopologies(
                planeViewInfoDto);
        return topologies;
    }


    /**
     * get ref view site link planed
     *
     * @param siteLinkViewLinks
     * @return
     */
    private Map<String, PlaneViewInfoDto> getRefViewSiteLinkPlaned(List<Link> siteLinkViewLinks) {
        log.debug("get ref view site link planed");
        Map<Link, List<String>> viewSiteLinkMap = siteLinkViewLinks.stream()
                .collect(Collectors.toMap(
                        link -> link,
                        link -> link.getSupportingLink().stream()
                                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                                .collect(Collectors.toList())
                ));
        List<PlaneViewInfoDto> planeViewInfoDtos = viewSiteLinkMap.entrySet().stream()
                .flatMap(entry -> {
                    Link viewLink = entry.getKey();
                    List<String> refSiteLinkIds = entry.getValue();
                    return getPlaneViewInfoDtoList(viewLink, refSiteLinkIds).stream();
                })
                .collect(Collectors.toList());

        List<PlaneViewInfoDto> mergedPlaneViewInfoDtos = mergedPlaneViewInfo(planeViewInfoDtos);
        return mergedPlaneViewInfoDtos.stream().collect(
                Collectors.toMap(PlaneViewInfoDto::getPlane, planeViewInfoDto -> planeViewInfoDto));

    }


    private List<PlaneViewInfoDto> getPlaneViewInfoDtoList(Link viewLink,
            List<String> refSiteLinkIds) {
        log.debug("the ref view link id is:{},the ref site link id is :{}",
                viewLink.getLinkId().getValue(),
                refSiteLinkIds);
        List<Link> realSiteLinks = topology.listAllSiteLinkByIds(refSiteLinkIds);
        List<ViewLinkPanelDto> viewLinkPanelDtos = convert2ViewLinkPanelDtos(realSiteLinks);
        List<PlaneViewInfoDto> planeViewInfoDtos = viewLinkPanelDtos.stream()
                .map(viewLinkPanelDto -> {
                    PlaneViewInfoDto topo = ViewLinkPanelConstructor.constructDefaultViewTopo(
                            viewLinkPanelDto.getPlane());
                    topo.getPlaneTopo().getLinks()
                            .add(ViewLinkPanelConstructor.buildViewLinkDto(viewLink,
                                    viewLinkPanelDto.getViewLink()));

                    topo.getPlaneTopo().getNodes()
                            .addAll(ViewLinkPanelConstructor.buildViewNodeDto(
                                    viewLinkPanelDto.getViewLink()));
                    return topo;
                }).collect(
                        Collectors.toList());
        return planeViewInfoDtos;
    }

    private List<ViewLinkPanelDto> convert2ViewLinkPanelDtos(List<Link> realSiteLinks) {
        List<ViewLinkPanelDto> viewLinkPanelDtos = realSiteLinks.stream()
                .map(link -> ViewLinkPanelDto.builder().viewLink(link).plane(link.getAugmentation(
                        Link1.class).getSite().getPlaneName()).build()).collect(
                        Collectors.toList());
        return viewLinkPanelDtos;
    }

    @Override
    public NMSViewLinkType supportViewLinkType() {
        return NMSViewLinkType.SITE_LINK;
    }
}
