package net.flex.dci.otn.controller.nms.nms.component.viewTopo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.dto.PlaneViewInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.ViewLinkDto;
import net.flex.dci.otn.controller.nms.nms.dto.ViewNodeDto;
import net.flex.dci.otn.controller.nms.nms.dto.ViewTopoDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSViewLinkType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 1/21/2024 5:14 PM
 */
@Slf4j
public abstract class AbstractViewLink implements ViewLink {


    @Autowired
    protected NetconfTopology topology;

    public abstract NMSViewLinkType supportViewLinkType();


    /**
     * merge data
     *
     * @param planeViewInfoDtos
     * @return
     */
    protected List<PlaneViewInfoDto> mergedPlaneViewInfo(List<PlaneViewInfoDto> planeViewInfoDtos) {
        Map<String, List<PlaneViewInfoDto>> planeMap = planeViewInfoDtos.stream()
                .collect(Collectors.groupingBy(PlaneViewInfoDto::getPlane));
        List<PlaneViewInfoDto> mergedPlaneViewInfoDtos = planeMap.values().stream()
                .map(viewInfoDtos -> {
                    List<ViewLinkDto> mergeLinks = mergeViewLinks(viewInfoDtos);
                    List<ViewNodeDto> mergeNodes = mergeViewNodes(viewInfoDtos);
                    String plane = viewInfoDtos.get(0).getPlane();
                    String topologyId = TopoNameConstants.Site_View_Topo_Key;
                    return PlaneViewInfoDto.builder().plane(plane).planeTopo(ViewTopoDto.builder()
                            .links(mergeLinks).nodes(mergeNodes).topologyId(topologyId)
                            .build()).build();
                }).collect(Collectors.toList());
        return mergedPlaneViewInfoDtos;
    }

    private List<ViewNodeDto> mergeViewNodes(List<PlaneViewInfoDto> viewInfoDtos) {
        List<ViewNodeDto> viewNodeDtos = viewInfoDtos.stream()
                .map(PlaneViewInfoDto::getPlaneTopo).filter(Objects::nonNull).map(
                        ViewTopoDto::getNodes).flatMap(Collection::stream)
                .collect(Collectors.toList());
        return new ArrayList<>(viewNodeDtos.stream()
                .collect(Collectors.toMap(ViewNodeDto::getNodeId, Function.identity(),
                        (node1, node2) -> node1))
                .values());
    }

    private List<ViewLinkDto> mergeViewLinks(List<PlaneViewInfoDto> viewInfoDtos) {
        List<ViewLinkDto> viewLinkDtos = viewInfoDtos.stream()
                .map(PlaneViewInfoDto::getPlaneTopo).filter(Objects::nonNull).map(
                        ViewTopoDto::getLinks).flatMap(Collection::stream)
                .collect(Collectors.toList());
        Map<String, ViewLinkDto> viewLinkDtoMap = viewLinkDtos.stream()
                .collect(Collectors.toMap(
                        ViewLinkDto::getLinkId,
                        Function.identity(),
                        (dto1, dto2) -> {
                            List<String> supportingLinks = new ArrayList<>(
                                    dto1.getSupportingLink());
                            supportingLinks.addAll(dto2.getSupportingLink());
                            return ViewLinkDto.builder()
                                    .LinkId(dto1.getLinkId())
                                    .destination(dto1.getDestination())
                                    .source(dto1.getSource())
                                    .supportingLink(supportingLinks)
                                    .alarmState(dto1.getAlarmState())
                                    .bundle(supportingLinks.size())
                                    .build();
                        }
                ));
        List<ViewLinkDto> mergedViewLinkDtos = new ArrayList<>(viewLinkDtoMap.values());
        return mergedViewLinkDtos;
    }

    protected PlaneViewInfoDto buildViewInfoDto(String plane, List<Link> viewLinks,
            List<String> refSiteIds) {
        log.debug("build {} view info dto", plane);
        PlaneViewInfoDto.PlaneViewInfoDtoBuilder planeViewInfoDtoBuilder = PlaneViewInfoDto.builder();
        planeViewInfoDtoBuilder.plane(plane);
        ViewTopoDto.ViewTopoDtoBuilder viewTopoDtoBuilder = ViewTopoDto.builder();
        viewTopoDtoBuilder.topologyId(TopoNameConstants.Site_View_Topo_Key);
        viewTopoDtoBuilder.links(viewLinks.stream().map(link -> {
            View viewLinkAttribute = link.getAugmentation(
                            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1.class)
                    .getView();
            return ViewLinkDto.builder()
                    .LinkId(link.getLinkId().getValue())
                    .source(link.getSource().getSourceNode().getValue())
                    .destination(link.getDestination().getDestNode().getValue())
                    .level(viewLinkAttribute.getLevel())
                    .alarmState(viewLinkAttribute.getAlarmState())
                    .bundle(viewLinkAttribute.getBundleNumber())
                    .supportingLink(link.getSupportingLink().stream()
                            .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
                                    Collectors.toList()))
                    .build();
        }).collect(Collectors.toList()));

        viewTopoDtoBuilder.nodes(refSiteIds.stream()
                .map(refSiteId -> ViewNodeDto.builder().nodeId(refSiteId).build())
                .collect(Collectors.toList()));
        planeViewInfoDtoBuilder.planeTopo(viewTopoDtoBuilder.build());
        return planeViewInfoDtoBuilder.build();
    }

    protected List<String> getSiteNodeIdsByMultiplexLink(
            String plane, List<Link> siteLinks,
            List<Link> ochLinks) {
        log.debug("get ref site nodeIds by multiplex link the plane name is:{}", plane);
        Set<String> siteIds = new HashSet<>();
        //get site link ref site Id
        for (Link siteLink : siteLinks) {
            List<String> supportingLinkIds = siteLink.getSupportingLink().stream()
                    .map(SupportingLink::getLinkRef)
                    .map(Uri::getValue).collect(
                            Collectors.toList());
            Set<String> refSiteIds = supportingLinkIds.stream().flatMap(linkId -> {
                String srcSiteId = PhysicalLinkIdNamingRule.getSiteAId(linkId);
                String destSiteId = PhysicalLinkIdNamingRule.getSiteZId(linkId);
                return Stream.of(srcSiteId, destSiteId);
            }).collect(Collectors.toSet());
            siteIds.addAll(refSiteIds);
        }
        //och link
        Set<String> ochRefSiteIds = ochLinks.stream()
                .flatMap(link -> {

                    String srcSiteId = PhysicalNodeIdNamingRule.getSiteId(
                            link.getSource().getSourceNode().getValue());
                    String destSiteId = PhysicalNodeIdNamingRule.getSiteId(
                            link.getDestination().getDestNode().getValue());
                    return Stream.of(srcSiteId, destSiteId);
                }).collect(Collectors.toSet());
        siteIds.addAll(ochRefSiteIds);
        return new ArrayList<>(siteIds);
    }
}
