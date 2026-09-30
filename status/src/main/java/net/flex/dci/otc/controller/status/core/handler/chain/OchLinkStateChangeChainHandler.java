package net.flex.dci.otc.controller.status.core.handler.chain;

import static net.flex.dci.otc.controller.status.util.Constants.OCH_LINK_ORDER;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link.OchLinkAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.align.OchLinkAlignStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.operation.OchLinkOperationStateCalculator;
import net.flex.dci.otc.controller.status.core.handler.AbstractStateChangeChainHandler;
import net.flex.dci.otc.controller.status.dto.PhysicalStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.OchLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.OchLinksAlarmState.OchLinkAlarmState;
import net.flex.dci.otc.controller.status.dto.align.AlignStateResult;
import net.flex.dci.otc.controller.status.dto.align.OchLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.OchLinksAlignState.OchLinkAlignState;
import net.flex.dci.otc.controller.status.dto.operation.OchLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.OperationStateResult;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/7 11:29
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OchLinkStateChangeChainHandler extends AbstractStateChangeChainHandler {

    private final OchLinkAlignStateCalculator ochLinkAlignStateCalculator;

    private final OchLinkAlarmStateCalculator ochLinkAlarmStateCalculator;

    private final OchLinkOperationStateCalculator ochLinkOperationStateCalculator;

    private final OchLinkDao ochLinkDao;

    private final SiteLinkDao siteLinkDao;


    private final NodeCacheManager nodeCacheManager;

    private final ConnectionCacheManager connectionCacheManager;

    @Qualifier("linkStateExecutor")
    private final ExecutorService stateUpdateExecutor;


    @Override
    public int getOrder() {
        return OCH_LINK_ORDER;
    }

    @Override
    public void stateChange(Node phyNode) {
        long start = System.currentTimeMillis();
        String nodeId = phyNode.getNodeId().getValue();
        log.info("[{}] start to handle the state change for och link", nodeId);

        long t1 = System.currentTimeMillis();
        List<LinkStateDto> ochLinks = getRefOchLinks(phyNode);
        log.info("[{}] getRefOchLinks cost={}ms, count={}", nodeId, System.currentTimeMillis() - t1,
                ochLinks.size());
        if (!ochLinks.isEmpty()) {
            List<String> ochLinkIds = ochLinks.stream().map(LinkStateDto::getId)
                    .collect(Collectors.toList());
            long t2 = System.currentTimeMillis();
            processOchLinkStateCalculation(ochLinks);
            log.info("[{}] processOchLinkStateCalculation cost={}ms", nodeId,
                    System.currentTimeMillis() - t2);
        }

        log.info("[{}] OchLinkStateChangeChainHandler total cost={}ms", nodeId,
                System.currentTimeMillis() - start);
        if (getNext() != null) {
            long t6 = System.currentTimeMillis();
            getNext().stateChange(phyNode);
            log.info("[{}] getNext().stateChange cost={}ms", nodeId,
                    System.currentTimeMillis() - t6);
        }
    }

    @Override
    public void electricalLayerAlarmStateChange(List<String> linkIds, List<Alarm> alarmList) {
        CompletableFuture.runAsync(() -> {
            long start = System.currentTimeMillis();
            log.debug("electricalLayer: OchLinkHandler start, linkCount={}, alarmCount={}",
                    linkIds.size(), alarmList.size());
            List<String> finalElectricalLayerIds = new ArrayList<>();

            long ochFilterStart = System.currentTimeMillis();
            List<String> ochLinkIds = linkIds.stream().filter(OchLinkIdNamingRule::isOchLink)
                    .collect(
                            Collectors.toList());
            log.info("electricalLayer: OchLink filter cost={}ms, ochLinkCount={}",
                    System.currentTimeMillis() - ochFilterStart, ochLinkIds.size());

            if (!ochLinkIds.isEmpty()) {
                long ochCalcStart = System.currentTimeMillis();
                List<LinkStateDto> refOchLinks = connectionCacheManager.getOchLinksByIds(
                        ochLinkIds);
                OchLinksAlarmState ochLinksAlarmState = ochLinkAlarmStateCalculator.calculate(
                        refOchLinks);
                changeOchLinkAlarmState(ochLinksAlarmState);
                log.info("electricalLayer: OchLink calculation cost={}ms",
                        System.currentTimeMillis() - ochCalcStart);
                List<String> refTunnelIds = getOchLinkRefTunnelIds(ochLinkIds);
                finalElectricalLayerIds.addAll(refTunnelIds);
            }
            long tunnelFilterStart = System.currentTimeMillis();
            List<String> tunnelIds = linkIds.stream().filter(TunnelIdNamingRule::isTunnelId)
                    .collect(
                            Collectors.toList());
            log.info("electricalLayer: Tunnel filter cost={}ms, tunnelCount={}",
                    System.currentTimeMillis() - tunnelFilterStart, tunnelIds.size());
            finalElectricalLayerIds.addAll(tunnelIds);
            log.info("electricalLayer: OchLinkStateChangeChainHandler total cost={}ms",
                    System.currentTimeMillis() - start);
            if (getNext() != null) {
                getNext().electricalLayerAlarmStateChange(finalElectricalLayerIds, alarmList);
            }

        }, stateUpdateExecutor);

    }

    private List<String> getOchLinkRefTunnelIds(List<String> ochLinkIds) {
        List<LinkStateDto> tunnelStates = connectionCacheManager.getTunnelsByOchLinkIds(ochLinkIds);
        return tunnelStates.stream().map(LinkStateDto::getId).collect(
                Collectors.toList());
    }

    @Override
    public void electricalLayerAlarmClearStateChange(List<String> linkIds) {

        log.debug("electricalLayer: OchLinkHandler clear start, linkCount={}", linkIds.size());
        List<String> ochLinkIds = linkIds.stream().filter(OchLinkIdNamingRule::isOchLink).collect(
                Collectors.toList());
        CompletableFuture.runAsync(() -> {
            long start = System.currentTimeMillis();
            List<String> refTunnelIdsFromOch = new ArrayList<>();
            if (!ochLinkIds.isEmpty()) {
                long calcStart = System.currentTimeMillis();
                List<LinkStateDto> refOchLinks = connectionCacheManager.getOchLinksByIds(
                        ochLinkIds);

                OchLinksAlarmState ochLinksAlarmState = ochLinkAlarmStateCalculator.calculate(
                        refOchLinks);
                changeOchLinkAlarmState(ochLinksAlarmState);
                log.info("electricalLayer: OchLink clear calculation cost={}ms",
                        System.currentTimeMillis() - calcStart);
            }
            refTunnelIdsFromOch = getOchLinkRefTunnelIds(ochLinkIds);
            List<String> originalTunnelIds = linkIds.stream().filter(TunnelIdNamingRule::isTunnelId)
                    .collect(Collectors.toList());

            Set<String> finalTunnelIdsSet = new HashSet<>();
            finalTunnelIdsSet.addAll(originalTunnelIds);
            finalTunnelIdsSet.addAll(refTunnelIdsFromOch);
            List<String> finalTunnelIds = new ArrayList<>(finalTunnelIdsSet);
            if (getNext() != null) {
                getNext().electricalLayerAlarmClearStateChange(finalTunnelIds);
            }
            log.info("electricalLayer: OchLinkStateChangeChainHandler clear cost={}ms",
                    System.currentTimeMillis() - start);
        }, stateUpdateExecutor);
    }

    @Override
    public void alarmClearStateChange(List<String> linkIds) {

//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        log.debug("OchLink alarmClearStateChange start, linkCount={}", linkIds.size());
        List<LinkStateDto> refOchLinks = getRefOchLinksByPhyLinks(linkIds);
//            OchLinksAlarmState ochLinksAlarmState = ochLinkAlarmStateCalculator.calculate(
//                    refOchLinks);
//            changeOchLinkAlarmState(ochLinksAlarmState);
//            if (getNext() != null) {
//                List<String> ochLinkIds = refOchLinks.stream().map(LinkStateDto::getId)
//                        .collect(Collectors.toList());
//                getNext().alarmClearStateChange(ochLinkIds);
//            }
        if (!refOchLinks.isEmpty()) {
            OchLinksAlarmState ochLinksAlarmState = ochLinkAlarmStateCalculator.calculate(
                    refOchLinks);
            changeOchLinkAlarmState(ochLinksAlarmState);

            if (getNext() != null) {
                List<String> ochLinkIds = refOchLinks.stream().map(LinkStateDto::getId)
                        .collect(Collectors.toList());
                getNext().alarmClearStateChange(ochLinkIds);
            }
        }
        log.info("OchLinkStateChangeChainHandler.alarmClearStateChange cost={}ms, linkCount={}",
                System.currentTimeMillis() - start, linkIds.size());
//        }, stateUpdateExecutor);

    }

    @Override
    public void statusEventChange(List<String> linkIds) {
        long start = System.currentTimeMillis();
        log.info("start to handle status event change for och link, linkIds count={}",
                linkIds.size());
        List<LinkStateDto> ochLinks = getRefOchLinksByPhyLinks(linkIds);
        List<String> ochLinkIds = ochLinks.stream().map(LinkStateDto::getId)
                .collect(Collectors.toList());
        if (!ochLinkIds.isEmpty()) {
            processOchLinkStateCalculation(ochLinks);
        }

        if (getNext() != null) {
            long t3 = System.currentTimeMillis();
            List<String> tunnelIds = linkIds.stream().filter(TunnelIdNamingRule::isTunnelId)
                    .collect(
                            Collectors.toList());
            List<String> refLinkIds = new ArrayList<>();
            refLinkIds.addAll(ochLinkIds);
            refLinkIds.addAll(tunnelIds);
            getNext().statusEventChange(refLinkIds);
            log.info("getNext().statusEventChange cost={}ms", System.currentTimeMillis() - t3);
        }
        log.info("OchLinkStateChangeChainHandler.statusEventChange total cost={}ms",
                System.currentTimeMillis() - start);
    }

    private void processOchLinkStateCalculation(List<LinkStateDto> ochLinks) {
        if (ochLinks.isEmpty()) {
            return;
        }
        long calcStart = System.currentTimeMillis();
        CompletableFuture<OchLinksAlignState> alignFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    OchLinksAlignState result = ochLinkAlignStateCalculator.calculateAlignState(
                            ochLinks);
                    log.info("[OCH-CALC] alignStateCalc cost={}ms, ochCount={}",
                            System.currentTimeMillis() - t, ochLinks.size());
                    return result;
                }, stateUpdateExecutor);

        CompletableFuture<OchLinksAlarmState> alarmFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    OchLinksAlarmState result = ochLinkAlarmStateCalculator.calculate(ochLinks);
                    log.info("[OCH-CALC] alarmStateCalc cost={}ms",
                            System.currentTimeMillis() - t);
                    return result;
                }, stateUpdateExecutor);

        CompletableFuture<OchLinksOperState> operFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    OchLinksOperState result = ochLinkOperationStateCalculator.calculate(ochLinks);
                    log.info("[OCH-CALC] operStateCalc cost={}ms",
                            System.currentTimeMillis() - t);
                    return result;
                }, stateUpdateExecutor);

        long t1 = System.currentTimeMillis();
        CompletableFuture.allOf(alignFuture, alarmFuture, operFuture).join();
        log.info("[OCH-CALC] parallelWait cost={}ms (includes queue time)",
                System.currentTimeMillis() - t1);
        OchLinksAlignState ochLinksAlignState = alignFuture.join();
        OchLinksAlarmState ochLinksAlarmState = alarmFuture.join();
        OchLinksOperState ochLinksOperState = operFuture.join();

        long t2 = System.currentTimeMillis();
        changeOchLinksState(ochLinksAlarmState, ochLinksAlignState, ochLinksOperState);
        log.info("[OCH-CALC] changeOchLinksState cost={}ms", System.currentTimeMillis() - t2);
        log.info("[OCH-CALC] total processOchLinkStateCalculation cost={}ms",
                System.currentTimeMillis() - calcStart);
    }

    private void changeOchLinkOperState(OchLinksOperState ochLinksOperState) {
        log.debug("change the och link,och links operState is {}", ochLinksOperState);
        OperationStateResult operationStateResult = OperationStateResult.builder()
                .ochLinksOperState(ochLinksOperState).build();
        stateChanger.changeState(
                PhysicalStateResult.builder().operationStateResult(operationStateResult).build()
        );
    }

    private void changeOchLinkAlarmState(OchLinksAlarmState ochLinksAlarmState) {
        log.debug("change the och link ,och links alarm state is :{}", ochLinksAlarmState);
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .ochLinkAlarmState(ochLinksAlarmState).build();
        stateChanger.changeState(
                PhysicalStateResult.builder().alarmStateResult(alarmStateResult).build()
        );
        // Invalidate cache after state update
        if (ochLinksAlarmState != null && ochLinksAlarmState.getOchLinkAlarmStates() != null) {
            List<String> updatedLinkIds = ochLinksAlarmState.getOchLinkAlarmStates().stream()
                    .map(OchLinkAlarmState::getOchLinkId)
                    .collect(Collectors.toList());
            connectionCacheManager.invalidateLinksCache(updatedLinkIds);
        }
    }

    /**
     * @param linkIds phy link Ids
     */
    @Override
    public void alarmStateChange(List<String> linkIds) {

//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        log.debug("OchLink alarmStateChange start, linkCount={}", linkIds.size());
        List<LinkStateDto> refOchLinks = getRefOchLinksByPhyLinks(linkIds);
        if (!refOchLinks.isEmpty()) {
            List<String> ochLinkIds = refOchLinks.stream().map(LinkStateDto::getId)
                    .collect(Collectors.toList());

            OchLinksAlarmState ochLinksAlarmState = ochLinkAlarmStateCalculator.calculate(
                    refOchLinks);
            changeOchLinkAlarmState(ochLinksAlarmState);
            log.info("OchLinkStateChangeChainHandler.alarmStateChange cost={}ms, linkCount={}",
                    System.currentTimeMillis() - start, linkIds.size());
            if (getNext() != null) {
                getNext().alarmStateChange(ochLinkIds);
            }
        }

//        }, stateUpdateExecutor);

    }

    private List<LinkStateDto> getRefOchLinksByPhyLinks(List<String> linkIds) {
        List<String> phyLinkIds = linkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isPhysicalLinkId).collect(
                        Collectors.toList());
        List<LinkStateDto> siteLinks = connectionCacheManager.getSiteLinkByPhyLinkId(linkIds);
        List<String> siteLinkIds = siteLinks.stream().map(LinkStateDto::getId)
                .collect(
                        Collectors.toList());

        List<LinkStateDto> phyLinkRefOchs = connectionCacheManager.getOchLinkByPhyLinkId(
                phyLinkIds);
        List<LinkStateDto> siteLinkRefOchs = connectionCacheManager.getOchLinksBySiteLinkIds(
                siteLinkIds);

        List<LinkStateDto> ochLinks = new ArrayList<>();
        ochLinks.addAll(phyLinkRefOchs);
        ochLinks.addAll(siteLinkRefOchs);
        return ochLinks;
    }


    private void changeOchLinksState(OchLinksAlarmState ochLinksAlarmStates,
            OchLinksAlignState ochLinksAlignStates,
            OchLinksOperState ochLinksOperState) {
        log.debug("change the och links state");
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .ochLinkAlarmState(ochLinksAlarmStates).build();
        AlignStateResult alignStateResult = AlignStateResult.builder()
                .ochLinksAlignStates(ochLinksAlignStates).build();
        OperationStateResult operationStateResult = OperationStateResult.builder()
                .ochLinksOperState(ochLinksOperState).build();
        PhysicalStateResult physicalStateResult = PhysicalStateResult.builder()
                .alarmStateResult(alarmStateResult).alignStateResult(alignStateResult)
                .operationStateResult(operationStateResult).build();
        stateChanger.changeState(physicalStateResult);

        // Collect all link IDs that need cache invalidation
        List<String> updatedLinkIds = new ArrayList<>();

        // Invalidate cache for AlarmState
        if (ochLinksAlarmStates != null && ochLinksAlarmStates.getOchLinkAlarmStates() != null) {
            updatedLinkIds.addAll(ochLinksAlarmStates.getOchLinkAlarmStates().stream()
                    .map(OchLinkAlarmState::getOchLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate cache for AlignState
        if (ochLinksAlignStates != null && ochLinksAlignStates.getAlignStates() != null) {
            updatedLinkIds.addAll(ochLinksAlignStates.getAlignStates().stream()
                    .map(OchLinkAlignState::getOchLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate cache for OperState
        if (ochLinksOperState != null && ochLinksOperState.getOchLinkOperStates() != null) {
            updatedLinkIds.addAll(ochLinksOperState.getOchLinkOperStates().stream()
                    .map(OchLinksOperState.OchLinkOperState::getLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate all collected links
        if (!updatedLinkIds.isEmpty()) {
            connectionCacheManager.invalidateLinksCache(updatedLinkIds);
        }
    }


    private List<LinkStateDto> getRefOchLinks(Node phyNode) {
        Physical physical = phyNode.getAugmentation(Node1.class).getPhysical();
        NodeType nodeType = physical.getNodeType();
        List<LinkStateDto> ochLinks = new ArrayList<>();
        ochLinks = nodeCacheManager.getOchLinksByNe(nodeType, phyNode.getNodeId().getValue());
        return ochLinks;
    }


}
