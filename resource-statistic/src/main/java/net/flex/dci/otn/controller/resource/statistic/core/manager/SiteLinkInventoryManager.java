package net.flex.dci.otn.controller.resource.statistic.core.manager;

import static net.flex.dci.otn.controller.resource.statistic.core.utils.ResourceStatisticConstants.BATCH_SIZE;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.BATCH_TASK_TIMEOUT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.resource.statistic.rest.SiteLinkDetail;
import net.flex.dci.otn.topology.cache.manager.TopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import net.flex.dci.otn.topology.cache.model.TerminationPointCache;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/4/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkInventoryManager extends AbstractElementInventoryManager<SiteLinkDetail> {

    private final SiteLinkDao siteLinkDao;

    private final TopologyCacheManager topologyCacheManager;


    @Override
    public SiteLinkDetail getInventoryDetail(String id) {
        return null;
    }

    @Override
    public List<SiteLinkDetail> getInventoryDetails(List<String> siteLinkIds) {
        log.debug("start to list all site link:{} inventory details", siteLinkIds);
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            return Collections.emptyList();
        }
        List<SiteLinkDetail> finalResult = new ArrayList<>();
        List<List<String>> partitions = Lists.partition(siteLinkIds, BATCH_SIZE);
        log.info("siteLink export total:{}, divided into {} partition, each partition is {}",
                siteLinkIds.size(), partitions.size(),
                BATCH_SIZE);
        for (int i = 0; i < partitions.size(); i++) {
            List<String> batchSiteLinkIds = partitions.get(i);
            log.debug("start process {} siteLinks，size:{}", i + 1, batchSiteLinkIds.size());

            List<SiteLinkDetail> batchResult = processSingleBatch(batchSiteLinkIds);
            finalResult.addAll(batchResult);
        }
        return finalResult;
    }

    @Override
    public List<SiteLinkDetail> getInventoryDetails(List<String> ids, List<String> subnetIds) {
        return Collections.emptyList();
    }

    private List<SiteLinkDetail> processSingleBatch(List<String> batchSiteLinkIds) {
        log.debug("processing batch site link size:{}", batchSiteLinkIds.size());
        List<Link> siteLinks = siteLinkDao.listAllSiteLinkByIds(batchSiteLinkIds);
        if (CollectionUtils.isEmpty(siteLinks)) {
            return Collections.emptyList();
        }
        List<String> allNodeIds = new ArrayList<>();
        List<String> allTpIds = new ArrayList<>();

        for (Link link : siteLinks) {
            String srcTp = link.getSource().getSourceTp().getValue();
            String dstTp = link.getDestination().getDestTp().getValue();
            String srcNode = PhysicalTpIdNamingRule.getNodeId(srcTp);
            String dstNode = PhysicalTpIdNamingRule.getNodeId(dstTp);

            allTpIds.add(srcTp);
            allTpIds.add(dstTp);
            allNodeIds.add(srcNode);
            allNodeIds.add(dstNode);
        }

        Map<String, PhyNodeCache> nodeMap = topologyCacheManager.batchGetValues(allNodeIds,
                PhyNodeCache.class);

        Map<String, TerminationPointCache> tpMap = topologyCacheManager.batchGetValues(allTpIds,
                TerminationPointCache.class);
        CompletableFuture<List<SiteLinkDetail>> future = CompletableFuture.supplyAsync(() -> {
            List<SiteLinkDetail> result = new ArrayList<>(siteLinks.size());
            for (Link link : siteLinks) {
                try {
                    SiteLinkDetail detail = extractSiteLink(link, nodeMap, tpMap);
                    if (detail != null) {
                        result.add(detail);
                    }
                } catch (Exception e) {
                    log.error("process siteLink error: {}", link.getLinkId().getValue(), e);
                }
            }
            return result;
        }, asyncExecutor);

        try {
            return future.get(BATCH_TASK_TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("batch process siteLink failed", e);
            throw new RuntimeException("SiteLink batch processing failed", e);
        }
//        List<CompletableFuture<SiteLinkDetail>> futures = siteLinks.stream()
//                .map(siteLink -> CompletableFuture.supplyAsync(() -> {
//                    try {
//                        return extractSiteLink(siteLink);
//                    } catch (Exception e) {
//                        log.error("process node to siteLink error", e);
//                        return null;
//                    }
//                }, asyncExecutor))
//                .collect(Collectors.toList());
//
//        return futures.stream()
//                .map(future -> {
//                    try {
//                        return future.get(10, TimeUnit.SECONDS);
//                    } catch (Exception e) {
//                        log.error("batch process timeout/error", e);
//                        return null;
//                    }
//                }).filter(Objects::nonNull)
//                .collect(Collectors.toList());
    }

    private SiteLinkDetail extractSiteLink(Link siteLink, Map<String, PhyNodeCache> nodeMap,
            Map<String, TerminationPointCache> tpMap) {
        log.debug("extract site link the link id is:{}", siteLink.getLinkId().getValue());
        String siteLinkId = siteLink.getLinkId().getValue();
        Site siteLinkPhysical = siteLink.getAugmentation(Link1.class).getSite();
        String friendlyName = siteLinkPhysical.getFriendlyName();
        String subnet = siteLinkPhysical.getPlaneName();
        String linkGroup = siteLinkPhysical.getLinkGroup();
        String order = siteLinkPhysical.getOrderId().get(0);
        DateAndTime creationTime = siteLinkPhysical.getCreationTime();
        DateAndTime activationTime = siteLinkPhysical.getActivationTime();
        String sourceTpId = siteLink.getSource().getSourceTp().getValue();
        String destTpId = siteLink.getDestination().getDestTp().getValue();
        String bandwidth = siteLinkPhysical.getBandwidth();
        ImplementState implementState = siteLinkPhysical.getImplementState();

        Class<? extends ProtectionType> protectionType = siteLinkPhysical.getProtectionType();

        String sourceNeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
        String destNeId = PhysicalTpIdNamingRule.getNodeId(destTpId);

        PhyNodeCache sourcePhyNodeCache = nodeMap.get(sourceNeId);
        PhyNodeCache destPhyNodeCache = nodeMap.get(destNeId);
        TerminationPointCache sourceTpCache = tpMap.get(sourceTpId);
        TerminationPointCache destTpCache = tpMap.get(destTpId);

        SiteLinkDetail siteLinkDetail = SiteLinkDetail.builder()
                .siteLinkName(friendlyName)
                .siteLinkId(siteLinkId)
                .subnet(subnet)
                .msModel(linkGroup)
                .bandwidth(bandwidth)
                .demandSource(order)
                .creationTime(creationTime.getValue())
                .activationTime(activationTime == null ? "" : activationTime.getValue())
                .protectionType(protectionType.getSimpleName())
                .sourceSiteName(sourcePhyNodeCache.getSiteName())
                .sourceSiteId(sourcePhyNodeCache.getSiteId())
                .destSiteName(destPhyNodeCache.getSiteName())
                .destSiteId(destPhyNodeCache.getSiteId())
                .sourceNeName(sourcePhyNodeCache.getFriendlyName())
                .sourceNeId(sourcePhyNodeCache.getId())
                .sourceTpName(sourceTpCache.getSimpleName())
                .sourceTpId(sourceTpCache.getId())
                .destNeId(destPhyNodeCache.getId())
                .destNeName(destPhyNodeCache.getFriendlyName())
                .destTpId(destTpCache.getId())
                .destTpName(destTpCache.getSimpleName())
                .implementState(implementState.name())
                .build();

        return siteLinkDetail;

    }
}
