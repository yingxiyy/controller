package net.flex.dci.otn.controller.resource.statistic.core.manager;

import static net.flex.dci.otn.controller.resource.statistic.core.utils.ResourceStatisticConstants.BATCH_SIZE;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.enums.CustomeLinkType;
import net.flex.dci.otn.controller.resource.statistic.enums.PortFlow;
import net.flex.dci.otn.controller.resource.statistic.rest.PhyLinkDetail;
import net.flex.dci.otn.topology.cache.manager.TopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import net.flex.dci.otn.topology.cache.model.TerminationPointCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
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
public class PhyLinkInventoryManager extends AbstractElementInventoryManager<PhyLinkDetail> {

    private final PhyLinkDao phyLinkDao;

    private final TopologyCacheManager topologyCacheManager;

    @Override
    public PhyLinkDetail getInventoryDetail(String id) {
        return null;
    }

    @Override
    public List<PhyLinkDetail> getInventoryDetails(List<String> ids) {
        log.debug("start to list phy link :{} inventory detail", ids.size());
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<PhyLinkDetail> phyLinkDetails = new ArrayList<>();
        List<List<String>> partitions = Lists.partition(ids, BATCH_SIZE);
        log.info("phy link export total:{}, divided into {} partition, each partition is {}",
                ids.size(), partitions.size(),
                BATCH_SIZE);
        for (int i = 0; i < partitions.size(); i++) {
            List<String> phyLinkIds = partitions.get(i);
            log.debug("start to process {} phyLinks,size:{}", i + 1, phyLinkIds.size());
            List<PhyLinkDetail> details = processSingleBatchSize(phyLinkIds);
            phyLinkDetails.addAll(details);
        }
        return phyLinkDetails;
    }

    @Override
    public List<PhyLinkDetail> getInventoryDetails(List<String> ids, List<String> subnetIds) {
        return Collections.emptyList();
    }

    private List<PhyLinkDetail> processSingleBatchSize(List<String> phyLinkIds) {
        log.debug("processing single phyLinkIds batch:{}", phyLinkIds);
        List<Link> batchLinks = phyLinkDao.getAllPhyLinksByIds(phyLinkIds);
        if (CollectionUtils.isEmpty(batchLinks)) {
            return Collections.emptyList();
        }
        List<String> allNodeIds = new ArrayList<>();
        List<String> allTpIds = new ArrayList<>();
        for (Link link : batchLinks) {
            allNodeIds.add(link.getSource().getSourceNode().getValue());
            allNodeIds.add(link.getDestination().getDestNode().getValue());
            allTpIds.add(link.getSource().getSourceTp().getValue());
            allTpIds.add(link.getDestination().getDestTp().getValue());
        }
        Map<String, PhyNodeCache> nodeMap = topologyCacheManager.batchGetValues(allNodeIds,
                PhyNodeCache.class);

        Map<String, TerminationPointCache> tpMap = topologyCacheManager.batchGetValues(allTpIds,
                TerminationPointCache.class);
        CompletableFuture<List<PhyLinkDetail>> future = CompletableFuture.supplyAsync(() -> {
            List<PhyLinkDetail> result = new ArrayList<>(batchLinks.size());
            for (Link link : batchLinks) {
                try {
                    PhyLinkDetail detail = extractPhyLink(link, nodeMap, tpMap);
                    if (detail != null) {
                        result.add(detail);
                    }
                } catch (Exception e) {
                    log.error("process phyLink error: {}", link.getLinkId().getValue(), e);
                }
            }
            return result;
        }, asyncExecutor);

        try {
            List<PhyLinkDetail> result = future.get();
            return result;
        } catch (ExecutionException e) {

            Throwable cause = e.getCause();
            log.error("whole the async task failed", cause);
            throw new RuntimeException("PhyLink batch processing failed", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("async task is interrupted", e);
            throw new RuntimeException("PhyLink batch processing interrupted", e);
        }
//        List<CompletableFuture<PhyLinkDetail>> futures = batchLinks.stream()
//                .map(phyLink -> CompletableFuture.supplyAsync(() -> {
//                    try {
//                        return extractPhyLink(phyLink);
//                    } catch (Exception e) {
//                        log.error("process node to PhyLink error", e);
//                        return null;
//                    }
//                }, asyncExecutor))
//                .collect(Collectors.toList());
//
//        // 3. 等待所有并行任务完成，并过滤null
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

    private PhyLinkDetail extractPhyLink(Link phyLink, Map<String, PhyNodeCache> nodeMap,
            Map<String, TerminationPointCache> tpMap) {
        log.debug("extract phy link the link id is:{}", phyLink.getLinkId().getValue());
        String phyLinkId = phyLink.getLinkId().getValue();
        Physical physical = phyLink.getAugmentation(Link1.class).getPhysical();
        String friendlyName = physical.getFriendlyName();
        String sourceNodeId = phyLink.getSource().getSourceNode().getValue();
        String destNodeId = phyLink.getDestination().getDestNode().getValue();
        String sourceTpId = phyLink.getSource().getSourceTp().getValue();
        String destTpId = phyLink.getDestination().getDestTp().getValue();

        LinkDirection linkDirection = physical.getDirection();
        LinkType linkType = physical.getLinkType();
        String subnet = physical.getPlaneName();
        net.flex.dci.otn.controller.resource.statistic.enums.LinkDirection direction =
                linkDirection.equals(LinkDirection.Bidirection)
                        ? net.flex.dci.otn.controller.resource.statistic.enums.LinkDirection.BIDIRECTIONAL
                        : net.flex.dci.otn.controller.resource.statistic.enums.LinkDirection.UNIDIRECTIONAL;
        PortFlow sourcePortFlow =
                linkDirection.equals(LinkDirection.Bidirection) ? PortFlow.BIDIRECTIONAL
                        : PortFlow.OUT;
        PortFlow destPortFlow =
                linkDirection.equals(LinkDirection.Bidirection) ? PortFlow.BIDIRECTIONAL
                        : PortFlow.IN;

        PhyNodeCache sourceNodeCache = nodeMap.get(sourceNodeId);
        PhyNodeCache destNodeCache = nodeMap.get(destNodeId);
        TerminationPointCache sourceTpCache = tpMap.get(sourceTpId);
        TerminationPointCache destTpCache = tpMap.get(destTpId);
        String sourceTp = buildTpName(sourceTpCache.getSimpleName(), sourcePortFlow);
        String destTp = buildTpName(destTpCache.getSimpleName(), destPortFlow);
        return PhyLinkDetail.builder()
                .phyLinkId(phyLinkId)
                .phyLinkName(friendlyName)
                .linkType(CustomeLinkType.forName(linkType.name()).getValue())
                .subnet(subnet)
                .direction(direction)
                .sourceNe(sourceNodeCache.getFriendlyName())
                .sourceSite(sourceNodeCache.getSiteName())
                .sourceTp(sourceTp)
                .sourcePortFlow(sourcePortFlow)
                .destinationSite(destNodeCache.getSiteName())
                .destinationNe(destNodeCache.getFriendlyName())
                .destinationTp(destTp)
                .destinationPortFlow(destPortFlow)
                .build();
    }

    /**
     * 构建端口名称
     * - 双向端口：不加后缀
     * - 单向 MD 端口：根据方向解析为 M 口或 D 口
     * - 单向非 MD 端口：加 #IN 或 #OUT 后缀
     */
    private String buildTpName(String simpleName, PortFlow portFlow) {
        if (portFlow.equals(PortFlow.BIDIRECTIONAL)) {
            return simpleName;
        }
        if (isMdPort(simpleName)) {
            // MD 端口根据方向解析为 M 口或 D 口
            return parseMdPortByDirection(simpleName, portFlow);
        }
        return simpleName + "#" + portFlow.name();
    }

    /**
     * 判断是否为 MD 端口（同时包含 M 和 D 标识） 例如：MUXPANEL_32C32L-1-50-M33D33
     */
    private boolean isMdPort(String tpName) {
        if (tpName == null) {
            return false;
        }
        String upperName = tpName.toUpperCase();
        int mIndex = upperName.indexOf('M');
        int dIndex = upperName.indexOf('D');
        return mIndex >= 0 && dIndex >= 0 && mIndex < dIndex;
    }

    /**
     * 解析 MD 端口，根据方向返回 M 口或 D 口 OUT 方向 -> D 口 (MUXPANEL_32C32L-1-50-M33D33 ->
     * MUXPANEL_32C32L-1-50-D33) IN 方向 -> M 口 (MUXPANEL_32C32L-1-50-M33D33 ->
     * MUXPANEL_32C32L-1-50-M33)
     */
    private String parseMdPortByDirection(String tpName, PortFlow portFlow) {
        // 匹配 M数字D数字 格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(.*?)([Mm])(\\d+)([Dd])(\\d+)(.*)");
        java.util.regex.Matcher matcher = pattern.matcher(tpName);

        if (matcher.matches()) {
            String prefix = matcher.group(1);  // MUXPANEL_32C32L-1-50-
            String channelNum = matcher.group(3); // 33
            String suffix = matcher.group(6);  // 后面的部分

            if (portFlow.equals(PortFlow.OUT)) {
                // OUT 方向用 D 口
                return prefix + "D" + channelNum + suffix;
            } else {
                // IN 方向用 M 口
                return prefix + "M" + channelNum + suffix;
            }
        }

        return tpName;
    }
}
