package net.flex.dci.otn.controller.nms.nms.component.dimension;

import static net.flex.dci.otn.controller.nms.utils.ConnectionUtils.buildExternalEdge;
import static net.flex.dci.otn.controller.nms.utils.Constants.BLANK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dto.NeSubTypeInfo;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.ExternalEdge;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.InternalEdge;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.ReachabilityResult;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.ReachableSiteDetail;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.SiteNeInfo;
import net.flex.dci.otn.controller.nms.nms.component.dimension.dto.TpInfoDetail;
import net.flex.dci.otn.controller.nms.utils.ConnectionUtils;
import net.flex.dci.otn.controller.nms.utils.SiteRoleBitCalcUtil;
import net.flex.dci.otn.topology.cache.manager.TopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import net.flex.dci.otn.topology.cache.model.SiteCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.card.info.Port;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.card.info.PortBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.Reachability;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.ReachabilityBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.ExternalLinks;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.ExternalLinksBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.InternalLinks;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.ReachableSite;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.ReachableSiteBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.external.links.ExternalLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.external.links.ExternalLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.internal.links.InternalLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.internal.links.InternalLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.reachable.site.Site;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.reachability.reachable.site.SiteBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ne.info.Card;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ne.info.CardBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.info.Ne;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.info.NeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 2026/4/9
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteReachabilityImpl implements SiteReachability {

    private final TopologyCacheManager topologyCacheManager;

    private final SiteLinkDao siteLinkDao;

    private final PhyNodeDao phyNodeDao;

    private final PhyLinkDao phyLinkDao;

    private final SiteNodeDao siteNodeDao;


    @Override
    public Reachability getReachability(String subnetId, NodeId sourceSiteId,
            NodeId destinationSiteId) {
        log.debug("get reachability subnet :{} from :{} to :{}", subnetId, sourceSiteId,
                destinationSiteId);
        validateGetReachability(sourceSiteId, destinationSiteId, subnetId);
        boolean hasDest =
                destinationSiteId != null && StringUtils.hasText(destinationSiteId.getValue());
        ReachabilityResult reachabilityResult;
        if (hasDest) {
            reachabilityResult = bfsAllReachable(subnetId, sourceSiteId.getValue(),
                    destinationSiteId.getValue());
        } else {

            reachabilityResult = bfsAllReachable(subnetId, sourceSiteId.getValue(), BLANK);
        }
        Reachability reachability = buildReachabilityResult(reachabilityResult);
        return reachability;
    }

    /**
     * validate get reachability
     *
     * @param sourceSiteId
     * @param destinationSiteId
     * @param subnetId
     */
    private void validateGetReachability(NodeId sourceSiteId, NodeId destinationSiteId,
            String subnetId) {
        //detective current source site have otm subNeType
        List<NeSubTypeInfo> sourceSiteNeSubTypeInfos = phyNodeDao.listAllNeSubTypeBySiteAndSubnet(
                sourceSiteId.getValue(), subnetId);
        SiteType sourceSiteType = SiteRoleBitCalcUtil.calcByNeSubType(
                sourceSiteNeSubTypeInfos.stream().map(NeSubTypeInfo::getNeSubType).collect(
                        Collectors.toList()));
        if (!sourceSiteType.equals(SiteType.OTM)) {
            log.error("Source site {} does not contain OTM NE", sourceSiteId);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Source site must contain OTM network element");
        }

        if (destinationSiteId != null) {
            List<NeSubTypeInfo> destSiteNeSubTypeInfos = phyNodeDao.listAllNeSubTypeBySiteAndSubnet(
                    destinationSiteId.getValue(), subnetId);
            SiteType destSiteType = SiteRoleBitCalcUtil.calcByNeSubType(
                    destSiteNeSubTypeInfos.stream().map(NeSubTypeInfo::getNeSubType).collect(
                            Collectors.toList()));
            if (!destSiteType.equals(SiteType.OTM)) {
                log.error("Destination site {} does not contain OTM NE", sourceSiteId);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Destination site must contain OTM network element");
            }
        }

    }


    private ReachabilityResult bfsAllReachable(String subnetId, String sourceSiteId,
            String destinationSiteId) {
        log.info(
                "bfs get all reachable site info by subnetId:{} sourceSiteId:{} to destinationSite:{}",
                subnetId,
                sourceSiteId, destinationSiteId);
        Map<String, List<String>> neToSiteLinkIdsMap = preloadAllSiteLinkBySubnet(subnetId);
        Map<String, NeSubType> neSubTypeMap = preloadAllNeSubTypeBySubnet(subnetId);
        Set<String> allSiteLinkIds = neToSiteLinkIdsMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        List<String> preloadedWssLinkIds = phyLinkDao.retrieveAllWssLinkIdBySupportingSiteLinks(
                new ArrayList<>(allSiteLinkIds));
        boolean hasDestination = StringUtils.hasText(destinationSiteId);

        Set<String> visitedSiteIds = new HashSet<>();
        Set<String> visitedNeIds = new HashSet<>();
        Set<String> visitedSiteLinkIds = new HashSet<>();
        List<ExternalEdge> externalEdges = new ArrayList<>();
        List<InternalEdge> internalEdges = new ArrayList<>();
        Map<String, Set<String>> parentSiteMap = new HashMap<>();
        Map<String, Set<String>> childSiteMap = new HashMap<>();
        Map<String, List<ExternalEdge>> siteToEdgeMap = new HashMap<>();
        Queue<String> siteQueue = new LinkedList<>();
        siteQueue.offer(sourceSiteId);
        visitedSiteIds.add(sourceSiteId);
        Set<String> towDimensionSite = new HashSet<>();
        Map<String, Boolean> siteForwardAbilityMap = new HashMap<>();
        while (!siteQueue.isEmpty()) {
            String siteId = siteQueue.poll();
            log.debug("processing site:{} hop", siteId);

            boolean isTwoDimensionSite =
                    !siteId.equals(destinationSiteId) && isTwoDimensionSite(siteId, neSubTypeMap);
            if (isTwoDimensionSite) {
                towDimensionSite.add(siteId);
            }
            SiteNeInfo siteNeInfo = getAllReachableNeIds(siteId, neToSiteLinkIdsMap,
                    preloadedWssLinkIds);
//            Set<String> siteReachableNeIds = siteNeInfo.getNeIds();
            List<InternalEdge> internalEdgeList = siteNeInfo.getInternalEdges();
            internalEdges.addAll(internalEdgeList);
            Set<String> forwardNeIds = new HashSet<>();
            if (siteId.equals(sourceSiteId)) {
                visitedNeIds.addAll(siteNeInfo.getNeIds());
                forwardNeIds.addAll(siteNeInfo.getNeIds());
            } else {
                forwardNeIds = getForwardNeIds(siteId, parentSiteMap, siteToEdgeMap,
                        internalEdgeList);
                visitedNeIds.addAll(forwardNeIds);
            }
            boolean canForward = hasInternalConnection(internalEdgeList, forwardNeIds);
            siteForwardAbilityMap.put(siteId, canForward);
            log.debug("site:{} forward ability: {}", siteId, canForward);
            boolean isSourceSite = siteId.equals(sourceSiteId);
            boolean isDestSite = siteId.equals(destinationSiteId);
            boolean isAdjacent = isDestSite || isSourceSite;
            if (!isAdjacent && !canForward) {
                log.info("site:{} is intermediate site and cannot forward, skip outgoing edges",
                        siteId);
                continue;
            }
            List<ReachableSiteDetail> reachableSiteDetails = fetchReachableSiteDetail(siteId,
                    forwardNeIds, subnetId, isAdjacent, neToSiteLinkIdsMap, neSubTypeMap);
            for (ReachableSiteDetail reachableSiteDetail : reachableSiteDetails) {
                String toSite = reachableSiteDetail.getSiteId();

                List<ExternalEdge> reachableSiteEdges = reachableSiteDetail.getExternalEdges();

                childSiteMap.computeIfAbsent(siteId, k -> new HashSet<>()).add(toSite);
                parentSiteMap.computeIfAbsent(toSite, k -> new HashSet<>()).add(siteId);
                siteToEdgeMap.computeIfAbsent(siteId, k -> new ArrayList<>())
                        .addAll(reachableSiteEdges);
                List<ExternalEdge> realReachEdges = reachableSiteEdges.stream()
                        .filter(edge -> !visitedSiteLinkIds.contains(edge.getSiteLinkId()))
                        .collect(Collectors.toList());
                if (realReachEdges.isEmpty()) {
                    continue;
                }

                visitedSiteLinkIds.addAll(realReachEdges.stream()
                        .map(ExternalEdge::getSiteLinkId)
                        .collect(Collectors.toList()));
                externalEdges.addAll(reachableSiteEdges);
                if (!visitedSiteIds.contains(toSite)) {
                    visitedSiteIds.add(toSite);
                    siteQueue.offer(toSite);

                }
            }
        }

        if (hasDestination) {
            ReachabilityResult rawResult = buildPathResult(sourceSiteId, destinationSiteId,
                    parentSiteMap, childSiteMap, siteToEdgeMap, siteForwardAbilityMap,
                    internalEdges);
            rawResult.setTwoDimensionSite(towDimensionSite);
            return rawResult;
        }

        ReachabilityResult reachabilityResult = ReachabilityResult.builder()
                .sourceSiteId(sourceSiteId)
                .reachable(true)
                .reachableSites(visitedSiteIds)
                .reachableNes(visitedNeIds)
                .externalEdges(externalEdges)
                .internalEdges(internalEdges)
                .twoDimensionSite(towDimensionSite)
                .build();
        return reachabilityResult;
    }

    /**
     * preload all ne subType
     *
     * @param subnetId
     * @return
     */
    private Map<String, NeSubType> preloadAllNeSubTypeBySubnet(String subnetId) {
        log.debug("preload all ne subType by subnet:{}", subnetId);
        List<NeSubTypeInfo> allNes = phyNodeDao.listAllNeSubTypeBySubnet(subnetId);
        Map<String, NeSubType> neSubTypeMap = allNes.stream()
                .collect(Collectors.toMap(NeSubTypeInfo::getNeId, NeSubTypeInfo::getNeSubType));
        return neSubTypeMap;
    }

    /**
     * preload all site link by subnet
     *
     * @param subnetId
     * @return
     */
    private Map<String, List<String>> preloadAllSiteLinkBySubnet(String subnetId) {
        log.debug("preload all site link by subnet:{}", subnetId);
        List<String> refSiteLinkIds = siteLinkDao.retrieveAllSiteLinkIdBySubnetId(subnetId);
        List<String> filterDimensionSiteLinkIds = filterDimensionSiteLinkId(refSiteLinkIds);
        Map<String, List<String>> neToSiteLinkIds = new HashMap<>();
        for (String siteLinkId : filterDimensionSiteLinkIds) {
            String neA = SiteLinkIdNamingRule.getNodeA(siteLinkId);
            String neZ = SiteLinkIdNamingRule.getNodeZ(siteLinkId);
            neToSiteLinkIds.computeIfAbsent(neA, k -> new ArrayList<>()).add(siteLinkId);
            neToSiteLinkIds.computeIfAbsent(neZ, k -> new ArrayList<>()).add(siteLinkId);
        }
        log.info("preloaded {} site links for subnet:{}", refSiteLinkIds.size(), subnetId);
        return neToSiteLinkIds;
    }


    private List<String> filterDimensionSiteLinkId(List<String> siteLinkIds) {
        Set<String> siteIds = new HashSet<>();
        for (String siteLinkId : siteLinkIds) {
            String sourceSite = SiteLinkIdNamingRule.getSiteA(siteLinkId);
            String destSite = SiteLinkIdNamingRule.getSiteZ(siteLinkId);
            siteIds.add(sourceSite);
            siteIds.add(destSite);
        }
        List<String> dimensionLinkIds = siteNodeDao.retrieveAllDimensionLinkBySiteIds(siteIds);
        return siteLinkIds.stream().filter(dimensionLinkIds::contains).collect(
                Collectors.toList());
    }

    /**
     * get forward ne ids
     *
     * @param siteId
     * @param parentSiteMap
     * @return
     */
    private Set<String> getForwardNeIds(String siteId, Map<String, Set<String>> parentSiteMap,
            Map<String, List<ExternalEdge>> siteToEdgeMap,
            List<InternalEdge> internalEdges) {
        Set<String> reachableNeIds = new HashSet<>();
        Set<String> parentSites = parentSiteMap.getOrDefault(siteId, Collections.emptySet());
        for (String parentSite : parentSites) {
            List<ExternalEdge> parentSiteEdges = siteToEdgeMap.getOrDefault(parentSite,
                    Collections.emptyList());
            for (ExternalEdge edge : parentSiteEdges) {
                String sourceNeId = edge.getSourceNeId();
                String destNeId = edge.getDestNeId();
                String sourceSite = edge.getSourceSiteId();
                String destSite = edge.getDestSiteId();
                if (sourceSite.equals(parentSite) && destSite.equals(siteId)) {
                    reachableNeIds.add(destNeId);
                }
                if (destSite.equals(parentSite) && sourceSite.equals(siteId)) {
                    reachableNeIds.add(sourceNeId);
                }
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (InternalEdge ie : internalEdges) {
                if (ie.isBoundaryPort()) {
                    continue;
                }

                String a = ie.getSourceNeId();
                String z = ie.getDestNeId();
                boolean hasA = reachableNeIds.contains(a);
                boolean hasZ = reachableNeIds.contains(z);

                if (hasA && !hasZ) {
                    reachableNeIds.add(z);
                    changed = true;
                }
                if (hasZ && !hasA) {
                    reachableNeIds.add(a);
                    changed = true;
                }
            }
        }

        return reachableNeIds;
    }

    private boolean hasInternalConnection(List<InternalEdge> internalEdges, Set<String> siteNeIds) {
        if (internalEdges == null || internalEdges.isEmpty()) {
            return false;
        }

        for (InternalEdge edge : internalEdges) {
            if (edge.isBoundaryPort()) {
                continue;
            }

            String sourceNeId = edge.getSourceNeId();
            String destNeId = edge.getDestNeId();

            if (!sourceNeId.equals(destNeId) && siteNeIds.contains(sourceNeId)
                    && siteNeIds.contains(destNeId)) {
                return true;
            }
        }

        return siteNeIds.size() == 1;
    }


    private boolean isTwoDimensionSite(String siteId, Map<String, NeSubType> neSubTypeMap) {
        log.debug("current site :{} is two dimension site or not ", siteId);
        List<NeSubType> nodeSubTypeInfos = neSubTypeMap.entrySet().stream()
                .filter(entry -> entry.getKey().contains(siteId))
                .map(Entry::getValue)
                .collect(
                        Collectors.toList());
        if (nodeSubTypeInfos.size() != 2) {
            return false;
        }
        List<NeSubType> otmSubTypes = nodeSubTypeInfos.stream()
                .filter(neSubType -> neSubType.equals(NeSubType.OPC_OTM))
                .collect(Collectors.toList());
        return otmSubTypes.isEmpty();
    }

    /**
     * build path result reachable result
     *
     * @param sourceSiteId
     * @param destinationSiteId
     * @param parentSiteMap
     * @param childSiteMap
     * @param siteToEdgeMap
     * @param siteForwardAbilityMap
     * @param internalEdges
     * @return
     */
    private ReachabilityResult buildPathResult(String sourceSiteId, String destinationSiteId,
            Map<String, Set<String>> parentSiteMap,
            Map<String, Set<String>> childSiteMap,
            Map<String, List<ExternalEdge>> siteToEdgeMap,
            Map<String, Boolean> siteForwardAbilityMap, List<InternalEdge> internalEdges) {
        log.info("build path result from source :{} to destination:{}", sourceSiteId,
                destinationSiteId);
        Set<String> pathNeIds = new HashSet<>();
        List<ExternalEdge> pathExternalEdges = new ArrayList<>();
        Set<String> pathSites = new HashSet<>();

        Set<String> canReachDest = new HashSet<>();
        if (!CollectionUtils.isEmpty(parentSiteMap)) {
            Queue<String> reversQueue = new LinkedList<>();
            reversQueue.add(destinationSiteId);
            canReachDest.add(destinationSiteId);
            while (!reversQueue.isEmpty()) {
                String current = reversQueue.poll();
                Set<String> predecessors = parentSiteMap.get(current);
                if (CollectionUtils.isEmpty(predecessors)) {
                    continue;
                }
                for (String prev : predecessors) {
                    if (!canReachDest.contains(prev)) {
                        canReachDest.add(prev);
                        reversQueue.offer(prev);
                    }
                }
            }
            log.info("reverse cut tree finished,can reached destination site count:{}",
                    canReachDest.size());
        }
        if (!canReachDest.isEmpty() && !canReachDest.contains(sourceSiteId)) {
            log.warn("source site:{} cannot reach destination site:{} (reverse cut filter)",
                    sourceSiteId, destinationSiteId);
            return ReachabilityResult.builder()
                    .sourceSiteId(sourceSiteId)
                    .destSiteId(destinationSiteId)
                    .reachable(false)
                    .build();
        }

        List<List<String>> allValidPaths = findAllValidPathsDirected(sourceSiteId,
                destinationSiteId,
                childSiteMap, canReachDest, siteForwardAbilityMap);
        if (allValidPaths.isEmpty()) {
            return ReachabilityResult.builder()
                    .sourceSiteId(sourceSiteId)
                    .destSiteId(destinationSiteId)
                    .reachable(false)
                    .build();
        }
        for (List<String> path : allValidPaths) {
            pathSites.addAll(path);
        }
        Set<String> addedEdgeIds = new HashSet<>();
        for (List<String> path : allValidPaths) {
            for (int i = 0; i < path.size() - 1; i++) {
                String currentSite = path.get(i);
                String nextSite = path.get(i + 1);
                List<ExternalEdge> edgesToNext = siteToEdgeMap.getOrDefault(currentSite,
                                Collections.emptyList())
                        .stream()
                        .filter(edge -> {
                            boolean isMatch = (edge.getSourceSiteId().equals(currentSite)
                                    && edge.getDestSiteId().equals(nextSite))
                                    || (edge.getSourceSiteId().equals(nextSite)
                                    && edge.getDestSiteId().equals(currentSite));
                            return isMatch && !addedEdgeIds.contains(edge.getSiteLinkId());
                        })
                        .collect(Collectors.toList());
                for (ExternalEdge edge : edgesToNext) {
                    pathExternalEdges.add(edge);
                    addedEdgeIds.add(edge.getSiteLinkId());
                    pathNeIds.add(edge.getSourceNeId());
                    pathNeIds.add(edge.getDestNeId());

                }
            }
        }

        List<InternalEdge> pathInternalEdges = internalEdges.stream()
                .filter(internalEdge -> pathSites.contains(internalEdge.getSiteId())
                        && (pathNeIds.contains(internalEdge.getSourceNeId())
                        && pathNeIds.contains(internalEdge.getDestNeId()))
                        && (!internalEdge.isBoundaryPort()))
                .collect(Collectors.toList());

        Set<String> neWithEdges = new HashSet<>();
        pathExternalEdges.forEach(e -> {
            neWithEdges.add(e.getSourceNeId());
            neWithEdges.add(e.getDestNeId());
        });
        for (InternalEdge edge : pathInternalEdges) {
            if (neWithEdges.contains(edge.getSourceNeId())) {
                neWithEdges.add(edge.getDestNeId());
            }
            if (neWithEdges.contains(edge.getDestNeId())) {
                neWithEdges.add(edge.getSourceNeId());
            }
        }
        pathNeIds.retainAll(neWithEdges);

        ReachabilityResult reachabilityResult = ReachabilityResult.builder()
                .sourceSiteId(sourceSiteId)
                .reachable(true)
                .reachableSites(pathSites)
                .reachableNes(pathNeIds)
                .externalEdges(pathExternalEdges)
                .internalEdges(pathInternalEdges)
                .build();
        return reachabilityResult;
    }

    private List<List<String>> findAllValidPathsDirected(String sourceSiteId,
            String destinationSiteId, Map<String, Set<String>> childSiteMap,
            Set<String> canReachDest,
            Map<String, Boolean> siteForwardAbilityMap) {
        List<List<String>> allValidPaths = new ArrayList<>();
        Queue<PathNode> pathQueue = new LinkedList<>();
        PathNode startNode = new PathNode(new ArrayList<>(), new HashSet<>());
        startNode.path.add(sourceSiteId);
        startNode.visited.add(sourceSiteId);
        pathQueue.offer(startNode);
        while (!pathQueue.isEmpty()) {
            PathNode currentNode = pathQueue.poll();
            List<String> currentPath = currentNode.path;
            Set<String> visitedInPath = currentNode.visited;
            String lastSite = currentPath.get(currentPath.size() - 1);
            if (lastSite.equals(destinationSiteId)) {
                allValidPaths.add(new ArrayList<>(currentPath));
                continue;
            }
            Set<String> neighbors = childSiteMap.get(lastSite);
            if (CollectionUtils.isEmpty(neighbors)) {
                continue;
            }
            for (String neighbor : neighbors) {
                if (visitedInPath.contains(neighbor)) {
                    continue;
                }
                if (!canReachDest.isEmpty() && !canReachDest.contains(neighbor)) {
                    continue;
                }

                boolean isDestSite = neighbor.equals(destinationSiteId);
                Boolean canForward = siteForwardAbilityMap.getOrDefault(neighbor, false);
                if (!isDestSite && !canForward) {
                    log.debug("neighbor site:{} is intermediate site and cannot forward, skip",
                            neighbor);
                    continue;
                }
                List<String> newPath = new ArrayList<>(currentPath);
                newPath.add(neighbor);

                Set<String> newVisited = new HashSet<>(visitedInPath);
                newVisited.add(neighbor);
                pathQueue.offer(new PathNode(newPath, newVisited));
            }

        }

        return allValidPaths;
    }


    private List<ReachableSiteDetail> fetchReachableSiteDetail(String siteId,
            Set<String> siteReachableNeIds,
            String subnetId, boolean isAdjacent, Map<String, List<String>> neToSiteLinkIdsMap,
            Map<String, NeSubType> neSubTypeMap) {
        log.info("fetch reachable site details,the start neId size:{} subnetId:{} isAdjacent:{}",
                siteReachableNeIds.size(), subnetId, isAdjacent);
//        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsByPhyNodeIdsAndSubnet(
//                new ArrayList<>(siteReachableNeIds), subnetId);
        List<String> siteLinkIds = neToSiteLinkIdsMap.entrySet().stream()
                .filter(entry -> entry.getKey().contains(siteId))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
        List<ReachableSiteDetail> reachableSiteDetails = new ArrayList<>();
        Set<String> processedSiteLinkIds = new HashSet<>();
        Map<String, List<ExternalEdge>> reachableSiteWithExternalEdge = new HashMap<>();
        Set<String> validNeIds = new HashSet<>(siteReachableNeIds);
        if (isAdjacent) {
            validNeIds = siteReachableNeIds.stream()
                    .filter(neId -> {
                        NeSubType neType = neSubTypeMap.get(neId);
                        return NeSubType.OPC_OTM.equals(neType);
                    })
                    .collect(Collectors.toSet());
            log.debug("site {} is adjacent site，filter opc-otm site size：{}", siteId,
                    validNeIds.size());
        } else {
            //internal site
            validNeIds = siteReachableNeIds;
        }
        for (String siteLinkId : siteLinkIds) {
            if (processedSiteLinkIds.contains(siteLinkId)) {
                continue;
            }
            processedSiteLinkIds.add(siteLinkId);
            String sourceSiteId = SiteLinkIdNamingRule.getSiteA(siteLinkId);
            String destSiteId = SiteLinkIdNamingRule.getSiteZ(siteLinkId);
            String sourceNeId = SiteLinkIdNamingRule.getNodeA(siteLinkId);
            String destNeId = SiteLinkIdNamingRule.getNodeZ(siteLinkId);
            boolean isSourceValid = sourceSiteId.equals(siteId) && validNeIds.contains(sourceNeId);
            boolean isDestValid = destSiteId.equals(siteId) && validNeIds.contains(destNeId);
            if (!isSourceValid && !isDestValid) {
                continue;
            }

            String nextSiteId = null;
            if (sourceSiteId.equals(siteId)) {
                nextSiteId = destSiteId;
            } else {
                nextSiteId = sourceSiteId;
            }
            ExternalEdge externalEdge = buildExternalEdge(siteLinkId);
            reachableSiteWithExternalEdge.computeIfAbsent(nextSiteId, k -> new ArrayList<>())
                    .add(externalEdge);
        }
        for (Map.Entry<String, List<ExternalEdge>> entry : reachableSiteWithExternalEdge.entrySet()) {
            String nextSiteId = entry.getKey();
            List<ExternalEdge> edges = entry.getValue();

            ReachableSiteDetail detail = ReachableSiteDetail.builder()
                    .siteId(nextSiteId)
                    .externalEdges(edges)
                    .build();

            reachableSiteDetails.add(detail);
        }
        log.info("fetch reachable site details completed, found {} reachable sites from site:{}",
                reachableSiteDetails.size(), siteId);
        return reachableSiteDetails;
    }

    private NeSubType getNeSubType(String neId) {
        log.debug("get ne subType neId:{}", neId);
        PhyNodeCache phyNodeCache = topologyCacheManager.getValue(neId, PhyNodeCache.class);
        return NeSubType.fromCode(phyNodeCache.getNeSubType());
    }


    /**
     * get all reachable ne ids
     *
     * @param siteId
     * @param neToSiteLinkIdsMap preloaded NE -> siteLinkIds mapping
     * @param allWssLinkIds preloaded all wssLinkIds for the subnet
     * @return
     */
    private SiteNeInfo getAllReachableNeIds(String siteId,
            Map<String, List<String>> neToSiteLinkIdsMap, List<String> allWssLinkIds) {
        log.debug("get all reachable ne id,site id :{}", siteId);
        Set<String> reachableNeIds = new HashSet<>();
        List<InternalEdge> internalEdges = new ArrayList<>();

        for (String wssLinkId : allWssLinkIds) {
            String sourceNeId = PhysicalLinkIdNamingRule.getNodeAId(wssLinkId);
            String destNeId = PhysicalLinkIdNamingRule.getNodeZId(wssLinkId);
            if (sourceNeId.contains(siteId) && destNeId.contains(siteId)) {
                InternalEdge internalEdge = ConnectionUtils.buildWssInternalEdge(wssLinkId);
                internalEdges.add(internalEdge);
                reachableNeIds.add(sourceNeId);
                reachableNeIds.add(destNeId);
            }
        }

        List<String> siteLinkIds = neToSiteLinkIdsMap.entrySet().stream()
                .filter(entry -> entry.getKey().contains(siteId))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
        for (String siteLinkId : siteLinkIds) {
            String sourceSiteId = SiteLinkIdNamingRule.getSiteA(siteLinkId);
            String destSiteId = SiteLinkIdNamingRule.getSiteZ(siteLinkId);
            String sourceNeId = SiteLinkIdNamingRule.getNodeA(siteLinkId);
            String destNeId = SiteLinkIdNamingRule.getNodeZ(siteLinkId);
            if (sourceSiteId.equals(siteId)) {
                reachableNeIds.add(sourceNeId);
            }
            if (destSiteId.equals(siteId)) {
                reachableNeIds.add(destNeId);
            }

        }

        return SiteNeInfo.builder().neIds(reachableNeIds).internalEdges(internalEdges).build();

    }


    private Reachability buildReachabilityResult(ReachabilityResult reachabilityResult) {
        log.info("build reachability result");
        ReachabilityBuilder reachabilityBuilder = new ReachabilityBuilder();
        String sourceSite = reachabilityResult.getSourceSiteId();
        String destSite = reachabilityResult.getDestSiteId();
        SiteCache sourceSiteCache = topologyCacheManager.getValue(sourceSite, SiteCache.class);
        boolean reachable = reachabilityResult.isReachable();
        reachabilityBuilder.setSourceSiteId(NodeId.getDefaultInstance(sourceSite));
        reachabilityBuilder.setSourceSiteName(sourceSiteCache.getFriendlyName());
        reachabilityBuilder.setIsReachable(reachable);
        if (StringUtils.hasText(destSite)) {
            SiteCache destSiteCache = topologyCacheManager.getValue(destSite, SiteCache.class);
            reachabilityBuilder.setDestSiteId(NodeId.getDefaultInstance(destSite));
            reachabilityBuilder.setDestSiteName(destSiteCache.getFriendlyName());
        }

        if (reachable) {
//            ReachabilityResult mergedResult = mergeTwoDimensionSites(reachabilityResult);
            buildReachabilityEdgeInfo(reachabilityBuilder, reachabilityResult,
                    reachabilityResult.getTwoDimensionSite());
        }

        return reachabilityBuilder.build();
    }


    private void buildReachabilityEdgeInfo(ReachabilityBuilder reachabilityBuilder,
            ReachabilityResult reachabilityResult, Set<String> twoDimensionSiteId) {
        log.debug("build reachability result ");

        Set<String> reachableSites = reachabilityResult.getReachableSites();
        Set<String> reachableNes = reachabilityResult.getReachableNes();
        List<InternalEdge> internalEdges = reachabilityResult.getInternalEdges().stream()
                .filter(internalEdge -> !internalEdge.isBoundaryPort()).collect(
                        Collectors.toList());
        List<ExternalEdge> externalEdges = reachabilityResult.getExternalEdges();
        List<ExternalEdge> mergedExternalEdges = mergeExternalEdges(externalEdges,
                twoDimensionSiteId);
        Set<String> filteredSites = reachableSites.stream()
                .filter(siteId -> !twoDimensionSiteId.contains(siteId)).collect(
                        Collectors.toSet());
        Set<String> twoDimNeIds = externalEdges.stream()
                .filter(e -> twoDimensionSiteId.contains(e.getSourceSiteId()))
                .map(ExternalEdge::getSourceNeId)
                .collect(Collectors.toSet());
        twoDimNeIds.addAll(externalEdges.stream()
                .filter(e -> twoDimensionSiteId.contains(e.getDestSiteId()))
                .map(ExternalEdge::getDestNeId)
                .collect(Collectors.toList()));
        Set<String> filteredNes = reachableNes.stream()
                .filter(neId -> !twoDimNeIds.contains(neId))
                .collect(Collectors.toSet());
        List<InternalEdge> filteredInternalEdges = internalEdges.stream()
                .filter(edge -> !twoDimensionSiteId.contains(edge.getSiteId()))
                .collect(Collectors.toList());

        Map<String, List<InternalEdge>> neInternalEdgeMap = getNeInternalEdgeMap(
                filteredInternalEdges);
        Map<String, List<ExternalEdge>> neExternalEdgeMap = getNeExternalEdgeMap(
                mergedExternalEdges);
        ReachableSite reachableSite = buildReachableSite(filteredSites, filteredNes,
                neInternalEdgeMap, neExternalEdgeMap);
        ExternalLinks externalLinks = buildExternalLinks(mergedExternalEdges);
        InternalLinks internalLinks = buildInternalLinks(filteredInternalEdges);
        reachabilityBuilder.setExternalLinks(externalLinks);
        reachabilityBuilder.setInternalLinks(internalLinks);
        reachabilityBuilder.setReachableSite(reachableSite);
    }

    /**
     * merge external edges
     *
     * @param externalEdges
     * @param twoDimensionSiteId
     * @return
     */
    private List<ExternalEdge> mergeExternalEdges(List<ExternalEdge> externalEdges,
            Set<String> twoDimensionSiteId) {
        if (twoDimensionSiteId.isEmpty()) {
            return externalEdges;
        }
        Map<String, Map<String, List<ExternalEdge>>> adjacency = new HashMap<>();
        for (ExternalEdge externalEdge : externalEdges) {
            String srcSite = externalEdge.getSourceSiteId();
            String destSite = externalEdge.getDestSiteId();
            adjacency.computeIfAbsent(srcSite, k -> new HashMap<>())
                    .computeIfAbsent(destSite, k -> new ArrayList<>()).add(externalEdge);
            adjacency.computeIfAbsent(destSite, k -> new HashMap<>())
                    .computeIfAbsent(srcSite, k -> new ArrayList<>()).add(externalEdge);
        }

        List<ExternalEdge> mergedEdges = new ArrayList<>();
        Set<String> processed = new HashSet<>();
        for (ExternalEdge edge : externalEdges) {
            if (processed.contains(edge.getSiteLinkId())) {
                continue;
            }
            String src = edge.getSourceSiteId();
            String dst = edge.getDestSiteId();
            if (!twoDimensionSiteId.contains(src) && !twoDimensionSiteId.contains(dst)) {
                mergedEdges.add(edge);
                processed.add(edge.getSiteLinkId());
                continue;
            }
            if (twoDimensionSiteId.contains(src)) {
                continue;
            }
            List<ExternalEdge> path = new ArrayList<>();
            path.add(edge);
            processed.add(edge.getSiteLinkId());
            String current = dst;
            String prev = src;
            while (twoDimensionSiteId.contains(current)) {
                Map<String, List<ExternalEdge>> neighbors = adjacency.getOrDefault(current,
                        Collections.emptyMap());
                ExternalEdge nextEdge = null;
                String nextSite = null;
                for (Map.Entry<String, List<ExternalEdge>> entry : neighbors.entrySet()) {
                    String neighbor = entry.getKey();
                    List<ExternalEdge> edgeList = entry.getValue();
                    for (ExternalEdge e : edgeList) {
                        if (!neighbor.equals(prev) && !processed.contains(e.getSiteLinkId())) {
                            nextEdge = e;
                            nextSite = neighbor;
                            break;
                        }
                    }
                    if (nextEdge != null) {
                        break;
                    }
                }
                if (nextEdge == null) {
                    break;
                }

                path.add(nextEdge);
                processed.add(nextEdge.getSiteLinkId());
                prev = current;
                current = nextSite;

            }
            ExternalEdge first = path.get(0);
            ExternalEdge last = path.get(path.size() - 1);

            ExternalEdge merged = ExternalEdge.builder()
                    .siteLinkId(first.getSiteLinkId() + "_to_" + last.getSiteLinkId())
                    .sourceSiteId(first.getSourceSiteId())
                    .sourceNeId(first.getSourceNeId())
                    .sourceTpId(first.getSourceTpId())
                    .destSiteId(last.getDestSiteId())
                    .destNeId(last.getDestNeId())
                    .destTpId(last.getDestTpId())
                    .build();

            mergedEdges.add(merged);
        }

        return mergedEdges;
    }

    /**
     * build reachable site info
     *
     * @param reachableSites
     * @param reachableNes
     * @param neInternalEdgeMap
     * @param neExternalEdgeMap
     * @return
     */
    private ReachableSite buildReachableSite(Set<String> reachableSites, Set<String> reachableNes,
            Map<String, List<InternalEdge>> neInternalEdgeMap,
            Map<String, List<ExternalEdge>> neExternalEdgeMap) {
        log.info("build reachable site info,reachable sites:{}", reachableSites.size());
        log.info("build reachable site info,reachable site:{}", reachableSites);
        Map<String, List<TpInfoDetail>> internalEdgeTpInfoMap = buildInternalEdgeTpInfo(
                neInternalEdgeMap);
        Map<String, List<TpInfoDetail>> externalEdgeTpInfoMap = buildExternalEdgeTpInfo(
                neExternalEdgeMap);
        Map<String, List<TpInfoDetail>> neTpMap = Stream.of(internalEdgeTpInfoMap,
                        externalEdgeTpInfoMap)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(e.getValue()),
                        (list1, list2) -> {
                            list1.addAll(list2);
                            return list1;
                        }
                ));
        Map<String, List<String>> siteRefNeIdMap = buildSiteRefNeMap(reachableSites, reachableNes);
        ReachableSite reachableSite = buildReachableSiteInfo(siteRefNeIdMap, neTpMap);

        return reachableSite;
    }


    private Map<String, List<TpInfoDetail>> buildExternalEdgeTpInfo(
            Map<String, List<ExternalEdge>> neExternalEdgeMap) {
        log.info("get ne tpInfo  from internalEdge ");
        Map<String, List<TpInfoDetail>> neTpMap = new HashMap<>();
        for (Map.Entry<String, List<ExternalEdge>> entry : neExternalEdgeMap.entrySet()) {
            String neId = entry.getKey();
            List<ExternalEdge> externalEdges = entry.getValue();
            List<TpInfoDetail> tpInfoDetails = getExternalEdgeRefTpByNeId(externalEdges, neId);
            neTpMap.computeIfAbsent(neId, k -> new ArrayList<>()).addAll(tpInfoDetails);
        }
        return neTpMap;
    }


    private Map<String, List<TpInfoDetail>> buildInternalEdgeTpInfo(
            Map<String, List<InternalEdge>> neInternalEdgeMap) {
        log.info("get ne tpInfo from externalEdge ");
        Map<String, List<TpInfoDetail>> neTpMap = new HashMap<>();
        for (Map.Entry<String, List<InternalEdge>> entry : neInternalEdgeMap.entrySet()) {
            String neId = entry.getKey();
            List<InternalEdge> internalEdges = entry.getValue();
            List<TpInfoDetail> tpInfoDetails = getInternalEdgeRefTpByNeId(internalEdges, neId);
            neTpMap.computeIfAbsent(neId, k -> new ArrayList<>()).addAll(tpInfoDetails);
        }
        return neTpMap;
    }

    private List<TpInfoDetail> getExternalEdgeRefTpByNeId(List<ExternalEdge> externalEdges,
            String neId) {
        List<TpInfoDetail> tpInfoDetails = new ArrayList<>();
        for (ExternalEdge externalEdge : externalEdges) {
            String sourceNeId = externalEdge.getSourceNeId();
            String destNeId = externalEdge.getDestNeId();
            String sourceTpId = externalEdge.getSourceTpId();
            String destTpId = externalEdge.getDestTpId();
            TpInfoDetail tpInfoDetail;
            if (sourceNeId.equals(neId)) {
                tpInfoDetail = TpInfoDetail.builder()
                        .cardId(PhysicalTpIdNamingRule.getEquipId(sourceTpId))
                        .neId(sourceNeId)
                        .tpId(sourceTpId)
                        .build();
            } else {
                tpInfoDetail = TpInfoDetail.builder()
                        .cardId(PhysicalTpIdNamingRule.getEquipId(destTpId))
                        .neId(destNeId)
                        .tpId(destTpId)
                        .build();
            }
            tpInfoDetails.add(tpInfoDetail);
        }

        return tpInfoDetails;
    }

    private List<TpInfoDetail> getInternalEdgeRefTpByNeId(List<InternalEdge> internalEdges,
            String neId) {
        List<TpInfoDetail> tpInfoDetails = new ArrayList<>();
        for (InternalEdge internalEdge : internalEdges) {
            String sourceNeId = internalEdge.getSourceNeId();
            String destNeId = internalEdge.getDestNeId();
            String sourceTpId = internalEdge.getSourceTpId();
            String destTpId = internalEdge.getDestTpId();
            TpInfoDetail tpInfoDetail;
            if (sourceNeId.equals(neId)) {
                tpInfoDetail = TpInfoDetail.builder()
                        .cardId(PhysicalTpIdNamingRule.getEquipId(sourceTpId))
                        .tpId(sourceTpId)
                        .neId(sourceNeId)
                        .build();
            } else {
                tpInfoDetail = TpInfoDetail.builder()
                        .cardId(PhysicalTpIdNamingRule.getEquipId(destTpId))
                        .tpId(destTpId)
                        .neId(destNeId)
                        .build();

            }
            tpInfoDetails.add(tpInfoDetail);
        }

        return tpInfoDetails;
    }


    private Map<String, List<ExternalEdge>> getNeExternalEdgeMap(List<ExternalEdge> externalEdges) {
        log.debug("build the external edge map for the external edges size:{}", externalEdges);
        Map<String, List<ExternalEdge>> neEdgeMap = new HashMap<>();
        for (ExternalEdge edge : externalEdges) {
            neEdgeMap.computeIfAbsent(edge.getSourceNeId(), k -> new ArrayList<>()).add(edge);
            neEdgeMap.computeIfAbsent(edge.getDestNeId(), k -> new ArrayList<>()).add(edge);
        }
        return neEdgeMap;
    }

    private Map<String, List<InternalEdge>> getNeInternalEdgeMap(List<InternalEdge> internalEdges) {
        log.debug("build the internal edge map for the internal edges size:{}",
                internalEdges.size());
        Map<String, List<InternalEdge>> neEdgeMap = new HashMap<>();
        internalEdges.forEach(internalEdge -> {
            neEdgeMap.computeIfAbsent(internalEdge.getSourceNeId(), k -> new ArrayList<>())
                    .add(internalEdge);
            neEdgeMap.computeIfAbsent(internalEdge.getDestNeId(), k -> new ArrayList<>())
                    .add(internalEdge);
        });
        return neEdgeMap;
    }

    private InternalLinks buildInternalLinks(List<InternalEdge> internalEdges) {
        log.debug("build internal links the internal edges size :{}", internalEdges.size());
        List<InternalLink> internalLinkList = internalEdges.stream().map(internalEdge -> {
                    String wssLinkId = internalEdge.getInternalLinkId();
                    String siteId = internalEdge.getSiteId();
                    return new InternalLinkBuilder().setLinkId(LinkId.getDefaultInstance(wssLinkId))
                            .setSiteId(NodeId.getDefaultInstance(siteId))
                            .setDestinationNeId(
                                    NodeId.getDefaultInstance(internalEdge.getDestNeId()))
                            .setSourceNeId(NodeId.getDefaultInstance(internalEdge.getSourceNeId()))
                            .setSourcePortId(TpId.getDefaultInstance(internalEdge.getSourceTpId()))
                            .setDestinationPortId(TpId.getDefaultInstance(internalEdge.getDestTpId()))
                            .build();
                })
                .collect(Collectors.toList());
        return new InternalLinksBuilder().setInternalLink(internalLinkList).build();
    }

    private ExternalLinks buildExternalLinks(List<ExternalEdge> externalEdges) {

        log.debug("build external links the externalEdges size:{}", externalEdges);
        List<ExternalLink> externalLinks = externalEdges.stream().map(externalEdge -> {
            String siteLinkId = externalEdge.getSiteLinkId();
            String sourceSiteId = externalEdge.getSourceSiteId();
            String sourceNeId = externalEdge.getSourceNeId();
            String sourceTpId = externalEdge.getSourceTpId();
            String destSiteId = externalEdge.getDestSiteId();
            String destNeId = externalEdge.getDestNeId();
            String destTpId = externalEdge.getDestTpId();
            return new ExternalLinkBuilder().setLinkId(LinkId.getDefaultInstance(siteLinkId))
                    .setSourceSiteId(NodeId.getDefaultInstance(sourceSiteId))
                    .setDestinationSiteId(NodeId.getDefaultInstance(destSiteId))
                    .setSourceNeId(NodeId.getDefaultInstance(sourceNeId))
                    .setDestinationNeId(NodeId.getDefaultInstance(destNeId))
                    .setSourcePortId(TpId.getDefaultInstance(sourceTpId))
                    .setDestinationPortId(TpId.getDefaultInstance(destTpId))
                    .build();
        }).collect(Collectors.toList());

        return new ExternalLinksBuilder().setExternalLink(externalLinks).build();
    }


    private Map<String, List<String>> buildSiteRefNeMap(Set<String> reachableSites,
            Set<String> reachableNes) {
        log.debug("build site ref ne map, sites:{} nes:{}", reachableSites.size(),
                reachableNes.size());
        return reachableSites.stream()
                .collect(Collectors.toMap(
                        siteId -> siteId,
                        siteId -> reachableNes.stream()
                                .filter(neId -> neId.contains(siteId))
                                .collect(Collectors.toList())
                ));
    }

    private ReachableSite buildReachableSiteInfo(Map<String, List<String>> siteRefNeIdMap,
            Map<String, List<TpInfoDetail>> neTpMap) {
        log.debug("build reachable site info the site size:{}", siteRefNeIdMap.size());
        List<Site> sites = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : siteRefNeIdMap.entrySet()) {
            String siteId = entry.getKey();
            List<String> refNeIds = entry.getValue();
            Site site = buildSite(siteId, refNeIds, neTpMap);
            sites.add(site);
        }
        ReachableSiteBuilder reachableSiteBuilder = new ReachableSiteBuilder();
        reachableSiteBuilder.setSite(sites);
        return reachableSiteBuilder.build();
    }

    /**
     * build site
     *
     * @param siteId
     * @param refNeIds
     * @param neTpMap
     * @return
     */
    private Site buildSite(String siteId, List<String> refNeIds,
            Map<String, List<TpInfoDetail>> neTpMap) {
        log.debug("build the site reachable site:{} ref neIds:{}", siteId, refNeIds);
        SiteBuilder siteBuilder = new SiteBuilder();
        siteBuilder.setSiteId(NodeId.getDefaultInstance(siteId));
        SiteCache siteCache = topologyCacheManager.getValue(siteId, SiteCache.class);
        siteBuilder.setSiteName(siteCache.getFriendlyName());
        List<Ne> nes = buildRefNes(refNeIds, neTpMap);
        SiteType siteType = calculateSiteType(nes);
        siteBuilder.setNe(nes);
        siteBuilder.setSiteType(siteType);
        return siteBuilder.build();
    }

    private List<Ne> buildRefNes(List<String> refNeIds, Map<String, List<TpInfoDetail>> neTpMap) {
        log.debug("build ref ne site info the neIds:{}", refNeIds.size());
        List<Ne> nes = new ArrayList<>();
        for (String neId : refNeIds) {
            PhyNodeCache phyNodeCache = topologyCacheManager.getValue(neId, PhyNodeCache.class);
            List<TpInfoDetail> neRefTps = neTpMap.get(neId);
            List<Card> cardInfos = buildNeRefCard(neId, neRefTps);
            NeBuilder neBuilder = new NeBuilder();
            neBuilder.setNeId(NodeId.getDefaultInstance(neId));
            neBuilder.setCard(cardInfos);
            neBuilder.setNeSubtype(phyNodeCache.getNeSubType());
            nes.add(neBuilder.build());
        }
        return nes;
    }

    /**
     * build NeRef card
     *
     * @param neRefTps
     * @return
     */
    private List<Card> buildNeRefCard(String neId, List<TpInfoDetail> neRefTps) {
        log.info("build ne ref card info the neId:{}", neId);
        log.debug("build ne ref card info the ref ne tp size:{}", neRefTps.size());
        Map<String, List<TpInfoDetail>> cardRefTpMap = neRefTps.stream()
                .collect(Collectors.groupingBy(TpInfoDetail::getCardId));
        List<Card> cards = new ArrayList<>();
        for (Map.Entry<String, List<TpInfoDetail>> entry : cardRefTpMap.entrySet()) {
            String cardId = entry.getKey();
            List<TpInfoDetail> tpInfoDetails = entry.getValue();
            List<Port> refPorts = tpInfoDetails.stream().map(tpInfoDetail -> {
                String portId = tpInfoDetail.getTpId();
                return new PortBuilder().setPortId(portId).build();
            }).collect(Collectors.toList());
            CardBuilder cardBuilder = new CardBuilder();
            cardBuilder.setCardId(cardId);
            cardBuilder.setPort(refPorts);
            cards.add(cardBuilder.build());
        }
        return cards;
    }

    private SiteType calculateSiteType(List<Ne> nes) {
        log.debug("calculate site type nes size:{}", nes.size());
        SiteType siteType = SiteRoleBitCalcUtil.calcByNeSubType(
                nes.stream().map(neInfo -> NeSubType.fromCode(neInfo.getNeSubtype()))
                        .collect(Collectors.toList()));
        return siteType;
    }

    @Data
    private static class PathNode implements Serializable {

        List<String> path;
        Set<String> visited;

        PathNode(List<String> path, Set<String> visited) {
            this.path = path;
            this.visited = visited;
        }
    }

}
