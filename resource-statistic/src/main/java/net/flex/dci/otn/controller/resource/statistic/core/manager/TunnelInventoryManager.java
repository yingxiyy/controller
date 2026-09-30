package net.flex.dci.otn.controller.resource.statistic.core.manager;

import static net.flex.dci.otn.controller.resource.statistic.core.utils.ResourceStatisticConstants.BATCH_SIZE;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.BATCH_TASK_TIMEOUT;
import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.LEG_REQUIRED;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.ProtectType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.resource.statistic.enums.ProtectionLevel;
import net.flex.dci.otn.controller.resource.statistic.enums.TunnelProtectionType;
import net.flex.dci.otn.controller.resource.statistic.rest.TunnelDetail;
import net.flex.dci.otn.topology.cache.manager.TopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import net.flex.dci.otn.topology.cache.model.TerminationPointCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/4/12
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelInventoryManager extends AbstractElementInventoryManager<TunnelDetail> {

    private final TunnelDao tunnelDao;

    private final OchLinkDao ochLinkDao;

    private final TopologyCacheManager topologyCacheManager;

    @Override
    public TunnelDetail getInventoryDetail(String id) {
        return null;
    }

    @Override
    public List<TunnelDetail> getInventoryDetails(List<String> ids) {
        log.debug("start to list tunnel :{} inventory detail", ids.size());
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<TunnelDetail> tunnelDetails = new ArrayList<>();
        List<List<String>> partitions = Lists.partition(ids, BATCH_SIZE);
        log.info("tunnel export total:{}, divided into {} partition, each partition is {}",
                ids.size(), partitions.size(),
                BATCH_SIZE);
        for (int i = 0; i < partitions.size(); i++) {
            List<String> tunnelIds = partitions.get(i);
            log.debug("start to process {} phyLinks,size:{}", i + 1, ids.size());
            List<TunnelDetail> details = processSingleBatchSize(tunnelIds);
            tunnelDetails.addAll(details);
        }
        return tunnelDetails;
    }

    @Override
    public List<TunnelDetail> getInventoryDetails(List<String> ids, List<String> subnetIds) {
        return Collections.emptyList();
    }

    private List<TunnelDetail> processSingleBatchSize(List<String> tunnelIds) {
        log.debug("processing single phyLinkIds batch:{}", tunnelIds);
        List<Tunnel> batchTunnels = tunnelDao.listAllTunnelByIds(tunnelIds);
        return enrichTunnels(batchTunnels);
    }

    private TunnelDetail extractTunnel(Tunnel tunnel, Link refOchLink,
            Map<String, PhyNodeCache> nodeMap,
            Map<String, TerminationPointCache> tpMap) {
        log.debug("extract tunnel info:{}", tunnel.getTunnelId().getValue());
        String tunnelId = tunnel.getTunnelId().getValue();
        String subnet = tunnel.getPlaneName();
        String tunnelName = tunnel.getFriendlyName();
        String serviceType = tunnel.getServiceType().name();

//        String protectionType = tunnel.getProtectionType().getSimpleName();
        String centreFrequency = PropertyTool.getValue(tunnel.getProperties(), "centreFrequency");
        String creationTime = tunnel.getCreationTime().getValue();
        String activationTime =
                tunnel.getActivationTime() == null ? null : tunnel.getActivationTime().getValue();
        String sourceTpId = TunnelIdNamingRule.getATp(tunnelId);
        String destTpId = TunnelIdNamingRule.getZtp(tunnelId);

        String sourceNodeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
        String destNodeId = PhysicalTpIdNamingRule.getNodeId(destTpId);

        PhyNodeCache sourceNodeCache = nodeMap.get(sourceNodeId);
        PhyNodeCache destNodeCache = nodeMap.get(destNodeId);
        TerminationPointCache sourceClientTpCache = tpMap.get(sourceTpId);
        TerminationPointCache destClientTpCache = tpMap.get(destTpId);
        //line info
        //ref och
        String sourceLineTpId = refOchLink.getSource().getSourceTp().getValue();
        String destLineTpId = refOchLink.getDestination().getDestTp().getValue();

        String protectionLevel = getTunnelProtectionLevel(tunnel, refOchLink);
        String protectionType = getTunnelProtectionType(tunnel);

        TerminationPointCache sourceLineTpCache = tpMap.get(sourceLineTpId);
        TerminationPointCache destLineTpCache = tpMap.get(destLineTpId);

        return TunnelDetail.builder()
                .tunnelId(tunnelId)
                .tunnelName(tunnelName)
                .protectionType(protectionType)
                .centreFrequency(centreFrequency)
                .creationTime(creationTime)
                .subnet(subnet)
                .serviceType(serviceType)
                .activationTime(activationTime)
                .protectionLevel(protectionLevel)
                .protectionType(protectionType)
                .clientSideRate(sourceClientTpCache.getProtocolRate())
                .lineSideRate(sourceLineTpCache.getProtocolRate())
                .sourceNeName(sourceNodeCache.getFriendlyName())
                .sourceSiteName(sourceNodeCache.getSiteName())
                .sourceClientTpName(sourceClientTpCache.getSimpleName())
                .sourceLineTpName(sourceLineTpCache.getSimpleName())
                .destNeName(destNodeCache.getFriendlyName())
                .destSiteName(destNodeCache.getSiteName())
                .destClientTpName(destClientTpCache.getSimpleName())
                .destLineTpName(destLineTpCache.getSimpleName())
                .build();
    }


    /**
     * get tunnel protection type
     *
     * @param tunnel
     * @return
     */
    private String getTunnelProtectionType(Tunnel tunnel) {
        log.debug("get tunnel protection type tunnelId:{}", tunnel.getTunnelId());
        Class<? extends ProtectionType> protectionType = tunnel.getProtectionType();
        String protectionTypeName = protectionType.getSimpleName();
        //is leg-required
        String legRequired = PropertyTool.getValue(tunnel.getProperties(), LEG_REQUIRED);
        String protectionTypeDisplay = TunnelProtectionType.getDisplayLabel(protectionTypeName,
                legRequired);
        return protectionTypeDisplay;
    }

    /**
     * temp method
     *
     * @param tunnel
     * @param refOchLink
     * @return
     */
    private String getTunnelProtectionLevel(Tunnel tunnel, Link refOchLink) {
        ProtectionLevel protectionLevel = ProtectionLevel.UNPROTECT;
        if (isOchLinkProtected(refOchLink)) {
            protectionLevel = ProtectionLevel.OCH;
        } else if (isSiteLinkProtected(refOchLink.getSupportingLink())) {
            protectionLevel = ProtectionLevel.OMS;
        }
        return protectionLevel.name();
    }

    private boolean isSiteLinkProtected(List<SupportingLink> supportingLink) {
        log.debug("detect the site link is protected or not ");
        List<String> refSiteLinkIds = supportingLink.stream()
                .map(sptl -> sptl.getLinkRef().getValue()).collect(
                        Collectors.toList());
        Map<String, SiteLinkCache> siteLinkCaches = topologyCacheManager.batchGetValues(
                refSiteLinkIds,
                SiteLinkCache.class);
        Optional<SiteLinkCache> protectedSiteLink = siteLinkCaches.values().stream()
                .filter(siteLinkCache -> siteLinkCache.getProtectionType().equals(
                        ProtectType.PROTECTED))
                .findAny();
        return protectedSiteLink.isPresent();
    }

    private boolean isOchLinkProtected(Link refOchLink) {
        log.debug("the och link is protected or not ,the och link id:{}", refOchLink.getLinkId());
        Och och = refOchLink.getAugmentation(Link1.class).getOch();
        return !och.getProtectionType().equals(ProtectionUnprotected.class);
    }

    public List<TunnelDetail> enrichTunnels(List<Tunnel> batchTunnels) {
        if (CollectionUtils.isEmpty(batchTunnels)) {
            return Collections.emptyList();
        }
        List<String> allNodeIds = new ArrayList<>();
        List<String> allTpIds = new ArrayList<>();
        List<String> ochLinkIds = new ArrayList<>();
        Map<String, String> tunnelOchLinkMap = new HashMap<>();
        for (Tunnel tunnel : batchTunnels) {
            //ochLink id ref och link id
            String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
            String tunnelId = tunnel.getTunnelId().getValue();
            String srcClineTp = TunnelIdNamingRule.getATp(tunnelId);
            String srcLineTp = OchLinkIdNamingRule.getTpAId(ochLinkId);
            String dstClientTp = TunnelIdNamingRule.getZtp(tunnelId);
            String dstLineTp = OchLinkIdNamingRule.getTpZId(ochLinkId);
            String srcNode = PhysicalTpIdNamingRule.getNodeId(srcClineTp);
            String dstNode = PhysicalTpIdNamingRule.getNodeId(dstClientTp);
            tunnelOchLinkMap.put(tunnelId, ochLinkId);
            ochLinkIds.add(ochLinkId);
            allTpIds.add(srcLineTp);
            allTpIds.add(dstLineTp);
            allTpIds.add(srcClineTp);
            allTpIds.add(dstClientTp);
            allNodeIds.add(srcNode);
            allNodeIds.add(dstNode);
        }
        List<Link> ochLinks = ochLinkDao.listAllOchLinksByIds(ochLinkIds);
        Map<String, Link> ochLinkIdMap = ochLinks.stream()
                .collect(Collectors.toMap(
                        link -> link.getLinkId().getValue(),
                        link -> link,
                        (existing, replacement) -> existing
                ));
        Map<String, PhyNodeCache> nodeMap = topologyCacheManager.batchGetValues(allNodeIds,
                PhyNodeCache.class);

        Map<String, TerminationPointCache> tpMap = topologyCacheManager.batchGetValues(allTpIds,
                TerminationPointCache.class);

        CompletableFuture<List<TunnelDetail>> future = CompletableFuture.supplyAsync(() -> {
            List<TunnelDetail> result = new ArrayList<>(batchTunnels.size());
            for (Tunnel tunnel : batchTunnels) {
                try {
                    String tunnelId = tunnel.getTunnelId().getValue();
                    Link refOchLink = ochLinkIdMap.get(tunnelOchLinkMap.get(tunnelId));
                    TunnelDetail detail = extractTunnel(tunnel, refOchLink, nodeMap, tpMap);
                    if (detail != null) {
                        result.add(detail);
                    }
                } catch (Exception e) {
                    log.error("process tunnel error: {}", tunnel.getTunnelId().getValue(), e);
                }
            }
            return result;
        }, asyncExecutor);

        try {
            return future.get(BATCH_TASK_TIMEOUT, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("batch process tunnel failed", e);
            throw new RuntimeException("Tunnel batch processing failed", e);
        }
    }
}
