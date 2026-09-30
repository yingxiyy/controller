package net.flex.dci.otn.controller.nms.nms.component.site.route.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.site.route.SiteRouteRetriever;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDetailDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDetailSequenceDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDto;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.SiteRouteDto.SiteRouteDtoBuilder;
import net.flex.dci.otn.controller.nms.nms.dto.site.route.TunnelSiteRouteInfoDto;
import net.flex.dci.otn.controller.nms.utils.CommonUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/12/19 11:35
 */
@Component
@Slf4j
public class SiteRouteRetrieverImpl implements SiteRouteRetriever {

    @Override
    public TunnelSiteRouteInfoDto retrieveTunnelSiteRoute(String tunnelId,
            RouteSequenceDto routeSequenceDto) {
        log.debug("retrieve all the route sequence for the tunnel");
        List<String> crossConnectionIds = new ArrayList<>();
//        List<CrossConnections> xcs = routeSequenceDto.getXcs()
        List<String> xcIds = routeSequenceDto.getXcIds();
        //primary cross connections
//        crossConnectionIds.addAll(
//                xcs.stream().map(xc -> xc.getCrossConnectionId().getValue()).collect(
//                        Collectors.toList()));
        crossConnectionIds.addAll(xcIds);
        //retrieve primary route 
        SiteRouteResult primaryRouteResult = retrieveRouteSequence(
                routeSequenceDto.getPrimary());
        primaryRouteResult.xcs.addAll(crossConnectionIds);
        SiteRouteResult secondSiteRouteResult = null;
        if (primaryRouteResult.getSecondary() != null) {
            secondSiteRouteResult = retrieveRouteSequence(
                    primaryRouteResult.getSecondary());
        }
        TunnelSiteRouteInfoDto tunnelSiteRouteInfoDto = buildSiteRouteDetail(tunnelId,
                primaryRouteResult,
                secondSiteRouteResult);
        return tunnelSiteRouteInfoDto;
    }


    private TunnelSiteRouteInfoDto buildSiteRouteDetail(String tunnelId,
            SiteRouteResult primaryRouteResult,
            SiteRouteResult secondSiteRouteResult) {

        log.debug("build the site route detail");
        Map<String, LinkedList<String>> primarySiteRouteMap = primaryRouteResult.getSiteRouteInfos();
        Map<String, LinkedList<String>> secondarySiteRouteMap = null;

        List<SiteRouteDto> siteRouteDtos = new ArrayList<>();
        Map<String, Set<String>> primaryXcIdMap = getCrossConnectionsMap(primaryRouteResult.xcs);
        Map<String, Set<String>> secondaryXcIdMap = null;
        if (secondSiteRouteResult != null) {
            secondarySiteRouteMap = secondSiteRouteResult.getSiteRouteInfos();
//            crossConnections.addAll(secondSiteRouteResult.xcs);
            secondaryXcIdMap = getCrossConnectionsMap(secondSiteRouteResult.xcs);
            Set<String> primarySet = primarySiteRouteMap.keySet();
            Set<String> secondarySet = secondarySiteRouteMap.keySet();
            //intersection site for both primary and secondary site
            Set<String> mergeSiteSet = CommonUtils.getIntersectionSetByGuava(primarySet,
                    secondarySet);
            Set<String> diffPrimarySet = CommonUtils.getDifferenceSetByGuava(primarySet,
                    secondarySet);
            Set<String> diffSecondarySet = CommonUtils.getDifferenceSetByGuava(secondarySet,
                    primarySet);

            List<SiteRouteDto> mergeSiteRouteDto = getSiteRouteDto(mergeSiteSet,
                    primarySiteRouteMap, primaryXcIdMap, secondarySiteRouteMap, secondaryXcIdMap);
            List<SiteRouteDto> diffPrimaryRouteDto = getPrimarySiteRouteDto(diffPrimarySet,
                    primarySiteRouteMap, primaryXcIdMap);
            List<SiteRouteDto> diffSecondaryRouteDto = getSecondarySiteRouteDto(diffSecondarySet,
                    secondarySiteRouteMap, secondaryXcIdMap);
            siteRouteDtos.addAll(mergeSiteRouteDto);
            siteRouteDtos.addAll(diffPrimaryRouteDto);
            siteRouteDtos.addAll(diffSecondaryRouteDto);
        } else {
            siteRouteDtos = getPrimarySiteRouteDto(primarySiteRouteMap.keySet(),
                    primarySiteRouteMap,
                    primaryXcIdMap);
        }

        return TunnelSiteRouteInfoDto.builder().siteRoutes(siteRouteDtos).tunnelId(tunnelId)
                .build();
    }

    /**
     * get merge site route dto
     *
     * @param mergeSiteSet
     * @param primarySiteRouteMap
     * @param primaryXcIdMap
     * @param secondarySiteRouteMap
     * @param secondaryXcIdMap
     * @return
     */
    private List<SiteRouteDto> getSiteRouteDto(Set<String> mergeSiteSet,
            Map<String, LinkedList<String>> primarySiteRouteMap,
            Map<String, Set<String>> primaryXcIdMap,
            Map<String, LinkedList<String>> secondarySiteRouteMap,
            Map<String, Set<String>> secondaryXcIdMap) {
        List<SiteRouteDto> siteRouteDtos = new ArrayList<>();
        for (String site : mergeSiteSet) {
            //primary
            LinkedList<String> primaryRoute = primarySiteRouteMap.get(site);
            SiteRouteDtoBuilder siteRouteDtoBuilder = SiteRouteDto.builder();
            Set<String> crossConnections = primaryXcIdMap.get(site);
            SiteRouteDetailDto primaryRouteDto = SiteRouteDetailDto.builder()
                    .siteRouteDetails(primaryRoute.stream()
                            .map(route -> SiteRouteDetailSequenceDto.builder().refId(route)
                                    .build()).collect(Collectors.toList()))
                    .crossConnections(new ArrayList<>(crossConnections))
                    .siteId(site)
                    .build();
            siteRouteDtoBuilder.primary(primaryRouteDto);
            //secondary
//            if (secondarySiteRouteMap != null && secondarySiteRouteMap.containsKey(site)) {
            LinkedList<String> secondaryRoute = secondarySiteRouteMap.get(site);
            Set<String> secondaryXcs = secondaryXcIdMap.get(site);
            SiteRouteDetailDto secondaryRouteDto = SiteRouteDetailDto.builder()
                    .siteRouteDetails(secondaryRoute.stream()
                            .map(route -> SiteRouteDetailSequenceDto.builder().refId(route)
                                    .build()).collect(Collectors.toList()))
                    .crossConnections(new ArrayList<>(secondaryXcs))
                    .siteId(site)
                    .build();
            siteRouteDtoBuilder.secondary(secondaryRouteDto);
//            }
            siteRouteDtoBuilder.siteId(site);
            siteRouteDtos.add(siteRouteDtoBuilder.build());
        }
        return siteRouteDtos;
    }

    private List<SiteRouteDto> getSecondarySiteRouteDto(Set<String> sites,
            Map<String, LinkedList<String>> siteRouteMap, Map<String, Set<String>> xcMap) {
        List<SiteRouteDto> siteRouteDtos = new ArrayList<>();
        for (String site : sites) {
            LinkedList<String> routes = siteRouteMap.get(site);
            SiteRouteDtoBuilder siteRouteDtoBuilder = SiteRouteDto.builder();
            Set<String> crossConnections = xcMap.get(site);
            SiteRouteDetailDto secondaryRouteDto = SiteRouteDetailDto.builder()
                    .siteRouteDetails(routes.stream()
                            .map(route -> SiteRouteDetailSequenceDto.builder().refId(route)
                                    .build()).collect(Collectors.toList()))
                    .crossConnections(new ArrayList<>(crossConnections))
                    .siteId(site)
                    .build();
            siteRouteDtoBuilder.secondary(secondaryRouteDto);
            siteRouteDtoBuilder.siteId(site);
            siteRouteDtos.add(siteRouteDtoBuilder.build());
        }
        return siteRouteDtos;
    }


    private List<SiteRouteDto> getPrimarySiteRouteDto(Set<String> sites,
            Map<String, LinkedList<String>> siteRouteMap, Map<String, Set<String>> xcMap) {
        List<SiteRouteDto> siteRouteDtos = new ArrayList<>();
        for (String site : sites) {
            LinkedList<String> primaryRoute = siteRouteMap.get(site);
            SiteRouteDtoBuilder siteRouteDtoBuilder = SiteRouteDto.builder();
            Set<String> crossConnections = xcMap.get(site);
            SiteRouteDetailDto primaryRouteDto = SiteRouteDetailDto.builder()
                    .siteRouteDetails(primaryRoute.stream()
                            .map(route -> SiteRouteDetailSequenceDto.builder().refId(route)
                                    .build()).collect(Collectors.toList()))
                    .crossConnections(new ArrayList<>(crossConnections))
                    .siteId(site)
                    .build();
            siteRouteDtoBuilder.primary(primaryRouteDto);
            siteRouteDtoBuilder.siteId(site);
            siteRouteDtos.add(siteRouteDtoBuilder.build());
        }
        return siteRouteDtos;
    }

    /**
     * map cross connections map
     *
     * @param crossConnections
     * @return
     */
    private Map<String, Set<String>> getCrossConnectionsMap(List<String> crossConnections) {
        Map<String, Set<String>> crossConnectionsMap = new HashMap<>();
        for (String crossConnection : crossConnections) {
            String siteId = PhysicalXcIdNamingRule.getSiteId(crossConnection);
            Set<String> crossConnectionSet = crossConnectionsMap.getOrDefault(siteId,
                    new HashSet<>());
            crossConnectionSet.add(crossConnection);
            crossConnectionsMap.put(siteId, crossConnectionSet);
        }
        return crossConnectionsMap;
    }

    /**
     * retrieve  route sequence
     *
     * @param routeSequenceDto
     * @return
     */
    private SiteRouteResult retrieveRouteSequence(RouteSequenceDto routeSequenceDto) {
        RouteSequenceDto secondarySequenceDto = null;
        Map<String, LinkedList<String>> siteRouteMap = new HashMap<>();
        List<String> crossConnections = new ArrayList<>();
        while (routeSequenceDto != null) {
            RouteSequenceDto current = routeSequenceDto;
            Class<?> clazz = current.getRouteHopType().getClazz();
//            List<CrossConnections> xcs = routeSequenceDto.getXcs();
            List<String> xcIds = routeSequenceDto.getXcIds();
//            crossConnections.addAll(
//                    xcs.stream().map(xc -> xc.getCrossConnectionId().getValue()).collect(
//                            Collectors.toList()));
            crossConnections.addAll(xcIds);
            if (clazz.isAssignableFrom(Tp.class)) {
                //if is tp
                String tpId = current.getTpId();
                String refSiteId = PhysicalTpIdNamingRule.getSiteId(tpId);
                updateRouteSiteMap(siteRouteMap, refSiteId, tpId);
            } else if (clazz.isAssignableFrom(Link.class)) {
                //if is phyLink
                String phyLinkId = current.getLinkId();
                String srcTpId = PhysicalLinkIdNamingRule.getTpAId(phyLinkId);
                String destTpId = PhysicalLinkIdNamingRule.getTpZId(phyLinkId);
                updateRouteSiteMap(siteRouteMap, PhysicalTpIdNamingRule.getSiteId(srcTpId),
                        phyLinkId);
                updateRouteSiteMap(siteRouteMap, PhysicalTpIdNamingRule.getSiteId(destTpId),
                        phyLinkId);
            }

            if (routeSequenceDto.getSecondary() != null && secondarySequenceDto == null) {
                secondarySequenceDto = routeSequenceDto.getSecondary();
            }
            routeSequenceDto = routeSequenceDto.getPrimary();
        }

        return SiteRouteResult.builder().siteRouteInfos(siteRouteMap).xcs(crossConnections)
                .secondary(secondarySequenceDto).build();
    }


    private void updateRouteSiteMap(
            Map<String, LinkedList<String>> siteRouteMap, String refSiteId,
            String refObjectId) {
        if (siteRouteMap.containsKey(refSiteId)) {
            LinkedList<String> siteRouteDetailSequenceDtos = siteRouteMap.get(
                    refSiteId);
            if (!siteRouteDetailSequenceDtos.contains(refObjectId)) {
                siteRouteDetailSequenceDtos.offer(refObjectId);
            } else {
                siteRouteDetailSequenceDtos.remove(refObjectId);
                siteRouteDetailSequenceDtos.offer(refObjectId);
            }
            siteRouteMap.put(refSiteId, siteRouteDetailSequenceDtos);
        } else {
            LinkedList<String> siteRouteDetailSequenceDtos = new LinkedList<>();
            siteRouteDetailSequenceDtos.offer(refObjectId);
            siteRouteMap.put(refSiteId, siteRouteDetailSequenceDtos);
        }
    }


    @Data
    @Builder
    private static class SiteRouteResult implements Serializable {

        private RouteSequenceDto secondary;

        private Map<String, LinkedList<String>> siteRouteInfos;
        private List<String> xcs;
    }
}
