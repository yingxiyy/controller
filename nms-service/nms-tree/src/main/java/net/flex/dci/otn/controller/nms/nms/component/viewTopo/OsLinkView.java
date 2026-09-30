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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 1/21/2024 4:56 PM
 */
@Component
@Slf4j
public class OsLinkView extends AbstractViewLink {


    @Override
    public List<Topology> getRefViewLinks() {
        log.debug("get os link view link");
        List<Link> osLinkViewLinks = topology.listAllViewLinkByLinkLevel(
                supportViewLinkType().getLinkType());
        Map<String, PlaneViewInfoDto> planeLinkMap = getRefViewOsLinkPlaned(osLinkViewLinks);
        List<Topology> topologies = ViewLinkPanelConstructor.buildTopologies(planeLinkMap);
        return topologies;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> getRefViewLinkInfoByMultiplexLink(
            String plane, List<Link> siteLinks,
            List<Link> ochLinks) {
        log.debug("get the plane :{} ref ots link view link", plane);
        List<String> refOsLinkIds = ochLinks.stream()
                .map(LinkAttributes::getSupportingLink)
                .map(supportingLinks -> supportingLinks.stream()
                        .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
                                Collectors.toList()))
                .flatMap(Collection::stream).filter(PhysicalLinkIdNamingRule::isOsLink)
                .collect(Collectors.toList());
        List<String> refSiteIds = getSiteNodeIdsByMultiplexLink(plane, siteLinks, ochLinks);

        List<Link> viewLinks = topology.getRefViewLinkByLinkIds(refOsLinkIds,
                supportViewLinkType().getLinkType());
        PlaneViewInfoDto planeViewInfoDto = buildViewInfoDto(plane,
                viewLinks, refSiteIds
        );
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> topologies = ViewLinkPanelConstructor.buildByPlaneTopologies(
                planeViewInfoDto);
        return topologies;
    }

    private Map<String, PlaneViewInfoDto> getRefViewOsLinkPlaned(List<Link> osLinkViewLinks) {
        log.debug("get os link ref planed view os link");
        Map<Link, List<String>> osLinkRefLinkMap = osLinkViewLinks.stream()
                .collect(Collectors.toMap(
                        link -> link,
                        link -> link.getSupportingLink().stream()
                                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                                .collect(Collectors.toList())
                ));
        List<PlaneViewInfoDto> planeViewInfoDtos = osLinkRefLinkMap.entrySet().stream()
                .flatMap(entry -> {
                    Link viewLink = entry.getKey();
                    List<String> refOsLinkIds = entry.getValue();
                    return getOsPlaneViewInfoDtoList(viewLink, refOsLinkIds).stream();
                })
                .collect(Collectors.toList());
        List<PlaneViewInfoDto> mergedPlaneViewInfoDtos = mergedPlaneViewInfo(planeViewInfoDtos);
        return mergedPlaneViewInfoDtos.stream().collect(
                Collectors.toMap(PlaneViewInfoDto::getPlane, planeViewInfoDto -> planeViewInfoDto));
    }

    private List<PlaneViewInfoDto> getOsPlaneViewInfoDtoList(Link viewLink,
            List<String> refOsLinkIds) {
        log.debug("get os plane view link info dto for the os link ids:{}", refOsLinkIds);
        List<Link> osLinks = topology.getPhyLinksByIds(refOsLinkIds);
        List<ViewLinkPanelDto> viewLinkPanelDtos = osLinks.stream()
                .map(link -> {
                    List<Link> refOchLinks = topology.getOchLinksBasedOnPhyLinks(
                            Collections.singletonList(link.getLinkId().getValue()));
                    Link refOchLink = refOchLinks.get(0);
                    String plane = refOchLink.getAugmentation(
                                    Link1.class)
                            .getOch().getPlaneName();
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
        return NMSViewLinkType.OS_LINK;
    }
}
