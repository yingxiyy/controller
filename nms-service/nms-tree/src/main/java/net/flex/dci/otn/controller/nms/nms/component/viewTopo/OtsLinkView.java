package net.flex.dci.otn.controller.nms.nms.component.viewTopo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otn.controller.nms.constructs.ViewLinkPanelConstructor;
import net.flex.dci.otn.controller.nms.nms.dto.PlaneViewInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.viewlink.ViewLinkPanelDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 1/21/2024 4:56 PM
 */
@Component
@Slf4j
public class OtsLinkView extends AbstractViewLink {


    @Override
    public List<Topology> getRefViewLinks() {
        log.debug("get ots link view link");
        List<Link> otsLinkViewLinks = topology.listAllViewLinkByLinkLevel(
                supportViewLinkType().getLinkType());

        Map<String, PlaneViewInfoDto> planeLinkMap = getRefViewOtsLinkPlaned(otsLinkViewLinks);
        List<Topology> topologies = ViewLinkPanelConstructor.buildTopologies(planeLinkMap);
        return topologies;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> getRefViewLinkInfoByMultiplexLink(
            String plane, List<Link> siteLinks,
            List<Link> ochLinks) {
        log.debug("get the plane :{} ref ots link view link", plane);
        List<String> refOtsLinkIds = siteLinks.stream()
                .map(LinkAttributes::getSupportingLink)
                .map(supportingLinks -> supportingLinks.stream()
                        .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
                                Collectors.toList()))
                .flatMap(Collection::stream).filter(PhysicalLinkIdNamingRule::isOtsLink)
                .collect(Collectors.toList());
        List<String> refSiteIds = getSiteNodeIdsByMultiplexLink(plane, siteLinks, ochLinks);

        List<Link> viewLinks = topology.getRefViewLinkByLinkIds(refOtsLinkIds,
                supportViewLinkType().getLinkType());
        PlaneViewInfoDto planeViewInfoDto = buildViewInfoDto(plane,
                viewLinks, refSiteIds
        );
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> topologies = ViewLinkPanelConstructor.buildByPlaneTopologies(
                planeViewInfoDto);
        return topologies;
    }

    private Map<String, PlaneViewInfoDto> getRefViewOtsLinkPlaned(List<Link> otsLinkViewLinks) {
        log.debug("get plane ref ots link");
        Map<Link, List<String>> otsLinkRefLinkMap = otsLinkViewLinks.stream()
                .collect(Collectors.toMap(
                        link -> link,
                        link -> link.getSupportingLink().stream()
                                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                                .collect(Collectors.toList())
                ));
        List<PlaneViewInfoDto> planeViewInfoDtos = otsLinkRefLinkMap.entrySet().stream()
                .flatMap(entry -> {
                    Link viewLink = entry.getKey();
                    List<String> refOtsLinks = entry.getValue();
                    return getPlaneOtsLinkViewInfoDtoList(viewLink, refOtsLinks).stream();
                })
                .collect(Collectors.toList());
        List<PlaneViewInfoDto> mergedPlaneViewInfoDtos = mergedPlaneViewInfo(planeViewInfoDtos);
        return mergedPlaneViewInfoDtos.stream().collect(
                Collectors.toMap(PlaneViewInfoDto::getPlane, planeViewInfoDto -> planeViewInfoDto));
    }

    private List<PlaneViewInfoDto> getPlaneOtsLinkViewInfoDtoList(Link viewLink,
            List<String> refOtsLinkIds) {
        log.debug("get plane ots link view info dto list");
        List<Link> refOtsLinks = topology.getPhyLinksByIds(refOtsLinkIds);
        List<ViewLinkPanelDto> viewLinkPanelDtos = refOtsLinks.stream()
                .map(link -> {
                    List<Link> refSiteLinks = topology.listAllSiteLinkBasedOnPhyLinks(
                            Collections.singletonList(link.getLinkId().getValue()));
                    Link refSiteLink = refSiteLinks.get(0);
                    String plane = refSiteLink.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                            .getSite().getPlaneName();
                    return ViewLinkPanelDto.builder().viewLink(link).plane(plane).build();
                }).collect(Collectors.toList());
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

    @Override
    public NMSViewLinkType supportViewLinkType() {
        return NMSViewLinkType.OTS_LINK;
    }
}
