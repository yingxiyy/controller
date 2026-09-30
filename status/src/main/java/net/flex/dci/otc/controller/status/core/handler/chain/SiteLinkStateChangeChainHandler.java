package net.flex.dci.otc.controller.status.core.handler.chain;

import static net.flex.dci.otc.controller.status.util.Constants.SITE_LINK_ORDER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link.SiteLinkAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.align.SiteLinkAlignStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.operation.SiteLinkOperationStateCalculator;
import net.flex.dci.otc.controller.status.core.handler.AbstractStateChangeChainHandler;
import net.flex.dci.otc.controller.status.dto.PhysicalStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.SiteLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.SiteLinksAlarmState.SiteLinkAlarmState;
import net.flex.dci.otc.controller.status.dto.align.AlignStateResult;
import net.flex.dci.otc.controller.status.dto.align.SiteLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.SiteLinksAlignState.SiteLinkAlignState;
import net.flex.dci.otc.controller.status.dto.operation.OperationStateResult;
import net.flex.dci.otc.controller.status.dto.operation.SiteLinksOperState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/7 11:28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteLinkStateChangeChainHandler extends AbstractStateChangeChainHandler {

    private final SiteLinkDao siteLinkDao;

    private final OchLinkDao ochLinkDao;

    private final SiteLinkAlignStateCalculator siteLinkAlignStateCalculator;

    private final SiteLinkAlarmStateCalculator siteLinkAlarmStateCalculator;

    private final SiteLinkOperationStateCalculator siteLinkOperationStateCalculator;

    private final NodeCacheManager nodeCacheManager;

    private final ConnectionCacheManager connectionCacheManager;

    @Qualifier("linkStateExecutor")
    private final ExecutorService stateUpdateExecutor;

    @Override
    public int getOrder() {
        return SITE_LINK_ORDER;
    }

    @Override
    public void stateChange(Node phyNode) {
        long start = System.currentTimeMillis();
        String nodeId = phyNode.getNodeId().getValue();
        log.info("[{}] start to handle the site link state change", nodeId);

        Physical physical = phyNode.getAugmentation(Node1.class).getPhysical();
        NodeType nodeType = physical.getNodeType();

        if (nodeType == NodeType.TD || nodeType == NodeType.TPC4) {
            log.debug(
                    "[{}] electrical node type: {}, skip site link calculation, pass to next handler",
                    nodeId, nodeType);
            if (getNext() != null) {
                getNext().stateChange(phyNode);
            }
            return;
        }

        log.debug("[{}] calculate the phy node ref site link ALARM STATE", nodeId);
        long t1 = System.currentTimeMillis();
        List<LinkStateDto> siteLinks = getRefSiteLinks(phyNode);
        log.info("[{}] getRefSiteLinks cost={}ms, count={}", nodeId,
                System.currentTimeMillis() - t1, siteLinks.size());

        if (!siteLinks.isEmpty()) {
            List<String> siteLinkIds = siteLinks.stream().map(LinkStateDto::getId).collect(
                    Collectors.toList());
            processSiteLinkStateCalculation(siteLinks);
        }

        log.info("[{}] SiteLinkStateChangeChainHandler total cost={}ms", nodeId,
                System.currentTimeMillis() - start);
        if (getNext() != null) {
            long t6 = System.currentTimeMillis();
            getNext().stateChange(phyNode);
            log.info("[{}] getNext().stateChange cost={}ms", nodeId,
                    System.currentTimeMillis() - t6);
        }
    }

    /**
     * change ref site link alarm state
     *
     * @param linkIds
     */
    @Override
    public void alarmStateChange(List<String> linkIds) {
//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        log.debug("SiteLink alarmStateChange start, linkCount={}", linkIds.size());
        List<LinkStateDto> siteLinks = getRefSiteLinksByPhyLinkId(linkIds);
        if (!siteLinks.isEmpty()) {
            SiteLinksAlarmState siteLinksAlarmState = siteLinkAlarmStateCalculator.calculate(
                    siteLinks);
            changeSiteLinkAlarmState(siteLinksAlarmState);
        }
        log.info("SiteLinkStateChangeChainHandler.alarmStateChange cost={}ms, linkCount={}",
                System.currentTimeMillis() - start, linkIds.size());
        if (getNext() != null) {
            getNext().alarmStateChange(linkIds);
        }

//        }, stateUpdateExecutor);

    }


    @Override
    public void alarmClearStateChange(List<String> linkIds) {

//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        List<LinkStateDto> siteLinks = getRefSiteLinksByPhyLinkId(linkIds);
        if (!siteLinks.isEmpty()) {
            SiteLinksAlarmState siteLinksAlarmState = siteLinkAlarmStateCalculator.calculate(
                    siteLinks);
            changeSiteLinkAlarmState(siteLinksAlarmState);
        }
        log.info(
                "SiteLinkStateChangeChainHandler.alarmClearStateChange cost={}ms, linkCount={}",
                System.currentTimeMillis() - start, linkIds.size());
        if (getNext() != null) {
            getNext().alarmClearStateChange(linkIds);
        }
//        }, stateUpdateExecutor);

    }

    @Override
    public void statusEventChange(List<String> linkIds) {
        long start = System.currentTimeMillis();
        log.info("start to handle status event change for site link, phy linkIds count={}",
                linkIds.size());
        List<String> phyLinkIds = linkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isPhysicalLinkId).collect(Collectors.toList());
        List<LinkStateDto> siteLinks = getRefSiteLinksByPhyLinkId(phyLinkIds);
        List<String> siteLinkIds = siteLinks.stream().map(LinkStateDto::getId)
                .collect(Collectors.toList());
        if (!siteLinks.isEmpty()) {
            processSiteLinkStateCalculation(siteLinks);
        }
        if (getNext() != null) {
            long t3 = System.currentTimeMillis();
            getNext().statusEventChange(linkIds);
            log.info("getNext().statusEventChange cost={}ms", System.currentTimeMillis() - t3);
        }
        log.info("SiteLinkStateChangeChainHandler.statusEventChange total cost={}ms",
                System.currentTimeMillis() - start);
    }

    @Override
    public void electricalLayerAlarmStateChange(List<String> linkIds, List<Alarm> alarmList) {
        long start = System.currentTimeMillis();
        log.debug("electricalLayer: SiteLinkHandler start, linkCount={}", linkIds.size());
        if (getNext() != null) {
            getNext().electricalLayerAlarmStateChange(linkIds, alarmList);
        }
        log.info("electricalLayer: SiteLinkStateChangeChainHandler cost={}ms (skip)",
                System.currentTimeMillis() - start);
    }

    @Override
    public void electricalLayerAlarmClearStateChange(List<String> linkIds) {
        long start = System.currentTimeMillis();
        log.debug("electricalLayer: SiteLinkHandler clear start, linkCount={}", linkIds.size());
        if (getNext() != null) {
            getNext().electricalLayerAlarmClearStateChange(linkIds);
        }
        log.info("electricalLayer: SiteLinkStateChangeChainHandler clear cost={}ms (skip)",
                System.currentTimeMillis() - start);
    }

    public void processSiteLinkStateCalculation(List<LinkStateDto> siteLinks) {
        log.debug("process site link state calculation sizeLink:{}", siteLinks.size());
        if (siteLinks.isEmpty()) {
            return;
        }
        long calcStart = System.currentTimeMillis();
        CompletableFuture<SiteLinksAlignState> alignFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    SiteLinksAlignState result = siteLinkAlignStateCalculator.calculateAlignState(
                            siteLinks);
                    log.info("[SITELINK-CALC] alignStateCalc cost={}ms, siteLinkCount={}",
                            System.currentTimeMillis() - t, siteLinks.size());
                    return result;
                }, stateUpdateExecutor);

        CompletableFuture<SiteLinksAlarmState> alarmFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    SiteLinksAlarmState result = siteLinkAlarmStateCalculator.calculate(siteLinks);
                    log.info("[SITELINK-CALC] alarmStateCalc cost={}ms",
                            System.currentTimeMillis() - t);
                    return result;
                }, stateUpdateExecutor);

        CompletableFuture<SiteLinksOperState> operFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    SiteLinksOperState result = siteLinkOperationStateCalculator.calculate(
                            siteLinks);
                    log.info("[SITELINK-CALC] operStateCalc cost={}ms",
                            System.currentTimeMillis() - t);
                    return result;
                }, stateUpdateExecutor);

        long t1 = System.currentTimeMillis();
        CompletableFuture.allOf(alignFuture, alarmFuture, operFuture).join();
        log.info("[SITELINK-CALC] parallelWait cost={}ms (includes queue time)",
                System.currentTimeMillis() - t1);
        SiteLinksAlignState siteLinkAlignState = alignFuture.join();
        SiteLinksAlarmState siteLinksAlarmState = alarmFuture.join();
        SiteLinksOperState siteLinksOperState = operFuture.join();

        long t2 = System.currentTimeMillis();
        changeLinkState(siteLinkAlignState, siteLinksAlarmState, siteLinksOperState);
        log.info("[SITELINK-CALC] changeLinkState cost={}ms", System.currentTimeMillis() - t2);
        log.info("[SITELINK-CALC] total processSiteLinkStateCalculation cost={}ms",
                System.currentTimeMillis() - calcStart);
    }


    private void changeSiteLinkOperState(SiteLinksOperState siteLinksOperState) {
        log.debug("change the site links oper status");
        OperationStateResult operationStateResult = OperationStateResult.builder()
                .siteLinksOperState(siteLinksOperState).build();
        stateChanger.changeState(
                PhysicalStateResult.builder().operationStateResult(operationStateResult).build());
    }

    private void changeSiteLinkAlarmState(SiteLinksAlarmState siteLinksAlarmState) {
        log.debug("change the site links alarm state");
//        siteLinksAlarmState.getSiteLinkAlarmStates().forEach(siteLinkAlarmState -> {
//            siteLinkDao.updateSiteLinkAlarmState(siteLinkAlarmState.getSiteLinkId(),
//                    siteLinkAlarmState.getAlarmSeverity());
//        });
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .siteLinkAlarmState(siteLinksAlarmState).build();
        stateChanger.changeState(
                PhysicalStateResult.builder().alarmStateResult(alarmStateResult).build());
        // Invalidate cache after state update
        if (siteLinksAlarmState != null && siteLinksAlarmState.getSiteLinkAlarmStates() != null) {
            List<String> updatedLinkIds = siteLinksAlarmState.getSiteLinkAlarmStates().stream()
                    .map(SiteLinkAlarmState::getSiteLinkId)
                    .collect(Collectors.toList());
            connectionCacheManager.invalidateLinksCache(updatedLinkIds);
        }
    }

    private List<LinkStateDto> getRefSiteLinksByPhyLinkId(List<String> linkIds) {
        log.debug("start to get ref site link by phy link id:{}", linkIds);
        List<LinkStateDto> siteLinks = connectionCacheManager.getSiteLinkByPhyLinkId(linkIds);
        return siteLinks;
    }

    /**
     * judge the site link is protected or unprotected
     *
     * @param siteLinks
     * @param linkIds
     * @param alarmSeverity
     * @return
     */
    private List<SiteLinkAlarmState> calculateSiteLinkAlarmState(List<LinkStateDto> siteLinks,
            List<String> linkIds, AlarmSeverity alarmSeverity) {
        log.debug("start to calculate site link alarm state");
        List<SiteLinkAlarmState> siteLinkAlarmStates = new ArrayList<>();
        siteLinks.forEach(siteLink -> {
            SiteLinkAlarmState siteLinkAlarmState = siteLinkAlarmStateCalculator.calculateLinkAlarmState(
                    siteLink, linkIds, alarmSeverity);
            siteLinkAlarmStates.add(siteLinkAlarmState);
        });
        return siteLinkAlarmStates;
    }

    private void changeSiteLinkAlarmState(List<Link> siteLinks, AlarmSeverity newAlarmSeverity) {
        log.debug("change site link alarm state");
        List<SiteLinkAlarmState> siteLinkAlarmStates = siteLinks.stream().map(siteLink -> {
            AlarmSeverity alarmSeverity = siteLink.getAugmentation(Link1.class).getSite()
                    .getAlarmState();
            AlarmSeverity updateSeverity = StatusUtil.calculateAlarmSeverity(newAlarmSeverity,
                    alarmSeverity);
            return SiteLinkAlarmState.builder().siteLinkId(siteLink.getLinkId().getValue())
                    .alarmSeverity(updateSeverity).build();
        }).collect(Collectors.toList());
        stateChanger.changeState(PhysicalStateResult.builder().alarmStateResult(
                AlarmStateResult.builder().siteLinkAlarmState(
                        SiteLinksAlarmState.builder().siteLinkAlarmStates(siteLinkAlarmStates)
                                .build()).build()).build());
    }


    private List<LinkStateDto> getRefSiteLinks(Node phyNode) {
        Physical physical = phyNode.getAugmentation(Node1.class).getPhysical();
        NodeType nodeType = physical.getNodeType();
        if (nodeType.equals(NodeType.TD) || nodeType.equals(NodeType.TPC4)) {
            return Collections.emptyList();
        }
        List<LinkStateDto> siteLinks = nodeCacheManager.getSiteLinksByNeId(
                phyNode.getNodeId().getValue());

//        if (nodeType.equals(NodeType.OD)) {
//            //get opc ref site link
//            siteLinks = getOpcRefSiteLinks(phyNode);
//        } else if (nodeType.equals(NodeType.TD)) {
//            //get tpc ref site link
//            siteLinks = getTpcNodeRefSiteLinks(phyNode);
//        }
        return siteLinks;
    }


    private void changeLinkState(SiteLinksAlignState siteLinksAlignState,
            SiteLinksAlarmState siteLinksAlarmState,
            SiteLinksOperState siteLinksOperState) {
        log.debug("change the link state");
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .siteLinkAlarmState(siteLinksAlarmState).build();
        AlignStateResult alignStateResult = AlignStateResult.builder()
                .siteLinksAlignState(siteLinksAlignState).build();
        OperationStateResult operationStateResult = OperationStateResult.builder()
                .siteLinksOperState(siteLinksOperState).build();
        PhysicalStateResult physicalStateResult = PhysicalStateResult.builder()
                .alarmStateResult(alarmStateResult).alignStateResult(alignStateResult)
                .operationStateResult(operationStateResult).build();
        stateChanger.changeState(physicalStateResult);

        // Collect all link IDs that need cache invalidation
        List<String> updatedLinkIds = new ArrayList<>();

        // Invalidate cache for AlarmState
        if (siteLinksAlarmState != null && siteLinksAlarmState.getSiteLinkAlarmStates() != null) {
            updatedLinkIds.addAll(siteLinksAlarmState.getSiteLinkAlarmStates().stream()
                    .map(SiteLinkAlarmState::getSiteLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate cache for AlignState
        if (siteLinksAlignState != null && siteLinksAlignState.getSiteLinkAlignStates() != null) {
            updatedLinkIds.addAll(siteLinksAlignState.getSiteLinkAlignStates().stream()
                    .map(SiteLinkAlignState::getSiteLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate cache for OperState
        if (siteLinksOperState != null && siteLinksOperState.getSiteLinkOperStates() != null) {
            updatedLinkIds.addAll(siteLinksOperState.getSiteLinkOperStates().stream()
                    .map(SiteLinksOperState.SiteLinkOperState::getSiteLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate all collected links
        if (!updatedLinkIds.isEmpty()) {
            connectionCacheManager.invalidateLinksCache(updatedLinkIds);
        }
    }

}
