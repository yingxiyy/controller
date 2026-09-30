package net.flex.dci.otn.controller.nms.nms.component.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.ExternalEdge;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.InternalEdge;
import net.flex.dci.otn.controller.nms.nms.dto.link.SiteLinkGeneralInfo;
import net.flex.dci.otn.controller.nms.utils.ConnectionUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.related.site.links.output.RelatedSiteLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.related.site.links.output.RelatedSiteLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/5/26
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkRelationCalculatorImpl implements SiteLinkRelationCalculator {

    private final PhyNodeDao phyNodeDao;
    private final PhyLinkDao phyLinkDao;
    private final SiteLinkDao siteLinkDao;

    @Override
    public List<RelatedSiteLink> getSiteLinkRelation(List<Link> siteLinks) {
        log.info("get site link relation the site links size:{}", siteLinks);
        if (CollectionUtils.isEmpty(siteLinks)) {
            log.debug("No site links provided for relation calculation");
            return new ArrayList<>();
        }
        log.info("Starting relation calculation for {} site links", siteLinks.size());

        List<SiteLinkGeneralInfo> generalInfos = convertToGeneralInfo(siteLinks);
        List<RelatedSiteLink> allRelatedLinks = calculatedLinks(generalInfos);
        log.info("Relation calculation completed. Found {} unique related site links",
                allRelatedLinks.size());

        return allRelatedLinks;
    }

    private List<RelatedSiteLink> calculatedLinks(List<SiteLinkGeneralInfo> initialLinks) {
        if (CollectionUtils.isEmpty(initialLinks)) {
            return new ArrayList<>();
        }
        Map<String, List<SiteLinkGeneralInfo>> linksBySubnet = initialLinks.stream().collect(
                Collectors.groupingBy(SiteLinkGeneralInfo::getSubnetId));
        log.info("Initial links belong to {} subnets: {}", linksBySubnet.size(),
                linksBySubnet.keySet());
        Set<String> allRelatedLinkIds = new HashSet<>();
        for (Map.Entry<String, List<SiteLinkGeneralInfo>> entry : linksBySubnet.entrySet()) {
            String subnetId = entry.getKey();
            List<SiteLinkGeneralInfo> subnetInitialLinks = entry.getValue();
            log.info("Starting diffusion for subnet {}: {} initial links", subnetId,
                    subnetInitialLinks.size());
            Set<String> subnetVisitedLinks = new HashSet<>();
            Set<String> subnetVisitedNeIds = new HashSet<>();
            Queue<String> subnetNeQueue = new LinkedList<>();
            for (SiteLinkGeneralInfo link : subnetInitialLinks) {
                subnetVisitedLinks.add(link.getSiteLinkId());
                subnetNeQueue.add(link.getSourceNeId());
                subnetNeQueue.add(link.getDestinationNeId());
            }
            while (!subnetNeQueue.isEmpty()) {
                String currentNeId = subnetNeQueue.poll();
                if (subnetVisitedNeIds.contains(currentNeId)) {
                    continue;
                }
                subnetVisitedNeIds.add(currentNeId);
                Set<String> reachableRoadms = findAllReachableRoadmsFromEntryRoadm(currentNeId,
                        subnetId);
                List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsByPhyNodeId(
                        new ArrayList<>(reachableRoadms));
                if (CollectionUtils.isEmpty(siteLinkIds)) {
                    log.debug("Subnet {}: No site links found for ROADMs: {}", subnetId,
                            reachableRoadms);
                    continue;
                }
                List<ExternalEdge> externalEdges = ConnectionUtils.buildExternalEdges(siteLinkIds);
                log.debug("Subnet {}: Found {} external edges for ROADMs: {}",
                        subnetId, externalEdges.size(), reachableRoadms);
                for (ExternalEdge externalEdge : externalEdges) {
                    String siteLinkId = externalEdge.getSiteLinkId();
                    if (subnetVisitedLinks.contains(siteLinkId)) {
                        continue;
                    }
                    subnetVisitedLinks.add(siteLinkId);
                    log.debug("Subnet {}: Added related site link: {}", subnetId, siteLinkId);
                    NeSubType nextNeType;
                    String nextNeId;
                    if (reachableRoadms.contains(externalEdge.getDestNeId())) {
                        nextNeId = externalEdge.getSourceNeId();
                        nextNeType = externalEdge.getSourceNeSubType();
                    } else {
                        nextNeId = externalEdge.getDestNeId();
                        nextNeType = externalEdge.getDestNeSubType();
                    }
                    if (subnetVisitedNeIds.contains(nextNeId)) {
                        continue;
                    }

//                    subnetNeTypeMap.put(nextNeId, nextNeType);

                    subnetNeQueue.add(nextNeId);
                    log.debug("Subnet {}: Added next NE: {} (type: {}) to diffusion queue",
                            subnetId, nextNeId, nextNeType);
                }

            }
            log.info("Subnet {} diffusion completed. Found {} related site links",
                    subnetId, subnetVisitedLinks.size());
            allRelatedLinkIds.addAll(subnetVisitedLinks);
        }
        if (allRelatedLinkIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<RelatedSiteLink> relatedSiteLinks = buildRelatedSiteLinks(allRelatedLinkIds);
        return relatedSiteLinks;
    }

    private List<RelatedSiteLink> buildRelatedSiteLinks(Set<String> allRelatedLinkIds) {
        log.debug("build related link Ids size:{}", allRelatedLinkIds.size());
        List<LinkStateDto> relativeSiteLinkStates = siteLinkDao.getSiteLinkStateByIds(
                new ArrayList<>(allRelatedLinkIds));
        List<RelatedSiteLink> relatedSiteLinks = relativeSiteLinkStates.stream().map(siteLink -> {
            String siteLinkId = siteLink.getId();

            String friendlyName = siteLink.getFriendlyName();
            return new RelatedSiteLinkBuilder().setRelatedSiteLinkId(siteLinkId)
                    .setRelatedSiteLinkSubnet(siteLink.getSubnetName())
                    .setRelatedSiteLinkSubnetId(siteLink.getSubnetId())
                    .setRelatedSiteLinkName(friendlyName).build();
        }).collect(
                Collectors.toList());

        return relatedSiteLinks;

    }


    private Set<String> findAllReachableRoadmsFromEntryRoadm(String currentNeId, String subnetId) {
        log.debug("diffusion the reachable roadms from entry roadm current roadm is:{}",
                currentNeId);
        String siteId = PhysicalNodeIdNamingRule.getSiteId(currentNeId);
        List<String> wssLinkIds = phyLinkDao.retrieveAllPhyLinkIdBySiteIdAndSubnetIdLinkType(
                Collections.singletonList(siteId), subnetId,
                LinkType.WssLink);
        List<InternalEdge> wssInternalEdges = ConnectionUtils.buildWssInternalEdges(wssLinkIds);
        Map<String, List<InternalEdge>> adjacentInternalEdgeMap = buildAdjacentInternalEdgeMap(
                wssInternalEdges);
        Set<String> visitedRoadms = new HashSet<>();
        Set<String> visitedWssLinkIds = new HashSet<>();
        Queue<String> roadmQueue = new LinkedList<>();
        roadmQueue.add(currentNeId);
        visitedRoadms.add(currentNeId);
        //bfs
        while (!roadmQueue.isEmpty()) {
            String currentRoadmId = roadmQueue.poll();
            log.debug("Processing ROADM: {} in site: {}", currentRoadmId, siteId);
            List<InternalEdge> adjacentEdges = adjacentInternalEdgeMap.getOrDefault(
                    currentRoadmId, Collections.emptyList());
            for (InternalEdge edge : adjacentEdges) {
                if (visitedWssLinkIds.contains(edge.getInternalLinkId())) {
                    continue;
                }
                visitedWssLinkIds.add(edge.getInternalLinkId());
                String nextNeId = edge.getDestNeId().equals(currentRoadmId) ? edge.getSourceNeId()
                        : edge.getDestNeId();
                if (visitedRoadms.contains(nextNeId)) {
                    continue;
                }
                log.debug("Found reachable ROADM: {} from {}", nextNeId, currentRoadmId);
                visitedRoadms.add(nextNeId);
                roadmQueue.add(nextNeId);
            }
        }

        return visitedRoadms;
    }

    private Map<String, List<InternalEdge>> buildAdjacentInternalEdgeMap(
            List<InternalEdge> wssInternalEdges) {
        Map<String, List<InternalEdge>> adjacentInternalEdgeMap = new HashMap<>();
        for (InternalEdge internalEdge : wssInternalEdges) {
            String sourceNeId = internalEdge.getSourceNeId();
            String destNeId = internalEdge.getDestNeId();
            adjacentInternalEdgeMap.computeIfAbsent(sourceNeId, k -> new ArrayList<>())
                    .add(internalEdge);
            adjacentInternalEdgeMap.computeIfAbsent(destNeId, k -> new ArrayList<>())
                    .add(internalEdge);
        }
        return adjacentInternalEdgeMap;
    }

    private List<SiteLinkGeneralInfo> convertToGeneralInfo(List<Link> siteLinks) {
        List<String> allNeIds = new ArrayList<>();
        List<SiteLinkGeneralInfo> siteLinkGeneralInfos = siteLinks.stream().map(siteLink -> {
            String siteLinkId = siteLink.getLinkId().getValue();
            String sourceSiteId = siteLink.getSource().getSourceNode().getValue();
            String sourceTpId = siteLink.getSource().getSourceTp().getValue();
            String destTpId = siteLink.getDestination().getDestTp().getValue();
            String sourceNeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
            String destNeId = PhysicalTpIdNamingRule.getNodeId(destTpId);
            allNeIds.add(sourceNeId);
            allNeIds.add(destNeId);
            String destSiteId = siteLink.getDestination().getDestNode().getValue();
            Site site = siteLink.getAugmentation(Link1.class).getSite();
            String siteLinkName = site.getFriendlyName();
            String subnetId = site.getPlaneId();
            String subnetName = site.getPlaneName();
            return SiteLinkGeneralInfo.builder().siteLinkId(siteLinkId).siteLinkName(siteLinkName)
                    .sourceSiteId(sourceSiteId)
                    .destinationNeId(destNeId)
                    .sourceNeId(sourceNeId)
                    .destinationSiteId(destSiteId).subnetId(subnetId).subnetName(subnetName)
                    .build();
        }).collect(Collectors.toList());

        log.info("Converted {} site links to general info", siteLinkGeneralInfos.size());
        return siteLinkGeneralInfos;
    }

//    private List<ExternalEdge> buildWrapExternalEdges(List<String> siteLinkIds) {
//        List<ExternalEdge> externalEdges = ConnectionUtils.buildExternalEdges(siteLinkIds);
//        List<String> allNeIds = new ArrayList<>();
//        for (ExternalEdge externalEdge : externalEdges) {
//            allNeIds.add(externalEdge.getSourceNeId());
//            allNeIds.add(externalEdge.getDestNeId());
//        }
//        if (!allNeIds.isEmpty()) {
//            List<NeSubTypeInfo> neSubTypeInfos = phyNodeDao.listAllNeSubTypeByNeIds(
//                    new ArrayList<>(allNeIds));
//
//            Map<String, NeSubType> neSubTypeMap = neSubTypeInfos.stream()
//                    .collect(Collectors.toMap(
//                            NeSubTypeInfo::getNeId,
//                            NeSubTypeInfo::getNeSubType,
//                            (oldValue, newValue) -> newValue
//                    ));
//
//            for (ExternalEdge edge : externalEdges) {
//                edge.setSourceNeSubType(neSubTypeMap.get(edge.getSourceNeId()));
//                edge.setDestNeSubType(neSubTypeMap.get(edge.getDestNeId()));
//            }
//        }
//        return externalEdges;
//    }
}
