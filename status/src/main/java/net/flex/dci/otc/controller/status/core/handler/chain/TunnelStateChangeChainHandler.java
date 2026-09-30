package net.flex.dci.otc.controller.status.core.handler.chain;

import static net.flex.dci.otc.controller.status.util.Constants.TUNNEL_ORDER;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link.TunnelAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.align.TunnelAlignStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.operation.TunnelOperationStateCalculator;
import net.flex.dci.otc.controller.status.core.handler.AbstractStateChangeChainHandler;
import net.flex.dci.otc.controller.status.dto.PhysicalStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.TunnelsAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.TunnelsAlarmState.TunnelAlarmState;
import net.flex.dci.otc.controller.status.dto.align.AlignStateResult;
import net.flex.dci.otc.controller.status.dto.align.TunnelsAlignState;
import net.flex.dci.otc.controller.status.dto.operation.OperationStateResult;
import net.flex.dci.otc.controller.status.dto.operation.TunnelsOperState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/7 11:29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelStateChangeChainHandler extends AbstractStateChangeChainHandler {

    private final NodeCacheManager nodeCacheManager;

    private final ConnectionCacheManager connectionCacheManager;

    private final OchLinkDao ochLinkDao;

    private final SiteLinkDao siteLinkDao;

    private final TunnelDao tunnelDao;

    private final TunnelAlarmStateCalculator tunnelAlarmStateCalculator;

    private final TunnelAlignStateCalculator tunnelAlignStateCalculator;

    private final TunnelOperationStateCalculator tunnelOperationStateCalculator;

    @Qualifier("linkStateExecutor")
    private final ExecutorService stateUpdateExecutor;

    @Override
    public int getOrder() {
        return TUNNEL_ORDER;
    }

    @Override
    public void stateChange(Node phyNode) {
        long start = System.currentTimeMillis();
        String nodeId = phyNode.getNodeId().getValue();
        log.info("[{}] start to handle the change for the tunnel", nodeId);

        long t1 = System.currentTimeMillis();
        List<LinkStateDto> refTunnels = getPhyNodeRefTunnel(phyNode);
        log.info("[{}] getPhyNodeRefTunnel cost={}ms, count={}", nodeId,
                System.currentTimeMillis() - t1, refTunnels.size());

        if (!refTunnels.isEmpty()) {
            List<String> tunnelIds = refTunnels.stream().map(LinkStateDto::getId)
                    .collect(Collectors.toList());
            long t2 = System.currentTimeMillis();
            processTunnelStateCalculation(refTunnels);
            log.info("[{}] processTunnelStateCalculation cost={}ms", nodeId,
                    System.currentTimeMillis() - t2);
        }

        log.info("[{}] TunnelStateChangeChainHandler total cost={}ms", nodeId,
                System.currentTimeMillis() - start);
        if (getNext() != null) {
            long t6 = System.currentTimeMillis();
            getNext().stateChange(phyNode);
            log.info("[{}] getNext().stateChange cost={}ms", nodeId,
                    System.currentTimeMillis() - t6);
        }
    }


    @Override
    public void alarmStateChange(List<String> linkIds) {
//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        log.debug("Tunnel alarmStateChange start, linkCount={}", linkIds.size());
        List<LinkStateDto> tunnels = getRefTunnels(linkIds);
        if (!tunnels.isEmpty()) {
            TunnelsAlarmState tunnelsAlarmState = tunnelAlarmStateCalculator.calculate(
                    tunnels);
            changeTunnelAlarmState(tunnelsAlarmState);
            log.info("TunnelStateChangeChainHandler.alarmStateChange cost={}ms, linkCount={}",
                    System.currentTimeMillis() - start, linkIds.size());
            if (getNext() != null) {
                getNext().alarmStateChange(linkIds);
            }
        }
//        }, stateUpdateExecutor);

    }

    @Override
    public void electricalLayerAlarmStateChange(List<String> linkIds, List<Alarm> alarmList) {
//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        log.debug("electricalLayer: TunnelHandler start, linkCount={}", linkIds.size());
        if (CollectionUtils.isEmpty(linkIds)) {
            log.info("tunnel alarm state change ref linkId is empty");
            return;
        }
        long tunnelCalcStart = System.currentTimeMillis();
        List<LinkStateDto> tunnels = getRefTunnels(linkIds);
        if (!tunnels.isEmpty()) {
            TunnelsAlarmState tunnelsAlarmState = tunnelAlarmStateCalculator.calculate(
                    tunnels);
            changeTunnelAlarmState(tunnelsAlarmState);
        }
        log.info("electricalLayer: Tunnel calculation cost={}ms, tunnelCount={}",
                System.currentTimeMillis() - tunnelCalcStart, tunnels.size());

//            if (getNext() != null) {
//                getNext().electricalLayerAlarmStateChange(linkIds, alarmList);
//            }
        log.info("electricalLayer: TunnelStateChangeChainHandler total cost={}ms",
                System.currentTimeMillis() - start);
//        }, stateUpdateExecutor);

    }

    @Override
    public void electricalLayerAlarmClearStateChange(List<String> tunnelIds) {

        log.debug("electrical layer alarm state change tunnelIds site:{}", tunnelIds);
        if (tunnelIds.isEmpty()) {
            log.debug("ref tunnel is null return");
            return;
        }
//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        List<LinkStateDto> tunnels = connectionCacheManager.batchGetAllTunnelsByIds(
                tunnelIds);
        TunnelsAlarmState tunnelsAlarmState = tunnelAlarmStateCalculator.calculate(
                tunnels);
        changeTunnelAlarmState(tunnelsAlarmState);
        log.info(
                "TunnelStateChangeChainHandler.electricalLayerAlarmClearStateChange cost={}ms, linkCount={}",
                System.currentTimeMillis() - start, tunnelIds.size());
//        }, stateUpdateExecutor);
//            if (getNext() != null) {
//                getNext().electricalLayerAlarmClearStateChange(tunnelIds);
//            }

    }


    @Override
    public void alarmClearStateChange(List<String> linkIds) {

//        List<LinkStateDto> tunnels = getRefTunnels(linkIds);
//
//        if (!tunnels.isEmpty()) {
//            TunnelsAlarmState tunnelsAlarmState = tunnelAlarmStateCalculator.calculate(
//                    tunnels);
//            changeTunnelAlarmState(tunnelsAlarmState);
//            if (getNext() != null) {
//                getNext().alarmClearStateChange(linkIds);
//            }
//        }
//        CompletableFuture.runAsync(() -> {
        log.debug("Tunnel alarmClearStateChange start, linkCount={}", linkIds.size());
        long start = System.currentTimeMillis();
        List<LinkStateDto> tunnels = getRefTunnels(linkIds);
        if (!tunnels.isEmpty()) {
            TunnelsAlarmState state = tunnelAlarmStateCalculator.calculate(tunnels);
            changeTunnelAlarmState(state);
        }
        log.info("TunnelStateChangeChainHandler.alarmClearStateChange cost={}ms, linkCount={}",
                System.currentTimeMillis() - start, linkIds.size());
//        }, stateUpdateExecutor);

    }

    @Override
    public void statusEventChange(List<String> linkIds) {
        long start = System.currentTimeMillis();
        log.info("start to handle status event change for tunnel, linkIds count={}",
                linkIds.size());
        List<LinkStateDto> refTunnels = getRefTunnels(linkIds);
        if (!refTunnels.isEmpty()) {
            processTunnelStateCalculation(refTunnels);
        }

        if (getNext() != null) {
            long t3 = System.currentTimeMillis();
            getNext().statusEventChange(linkIds);
            log.info("getNext().statusEventChange cost={}ms", System.currentTimeMillis() - t3);
        }
        log.info("TunnelStateChangeChainHandler.statusEventChange total cost={}ms",
                System.currentTimeMillis() - start);
    }

    private void processTunnelStateCalculation(List<LinkStateDto> refTunnels) {
        if (refTunnels.isEmpty()) {
            log.warn("ref tunnels is empty,do nothing and skip");
            return;
        }
        long calcStart = System.currentTimeMillis();
        CompletableFuture<TunnelsAlarmState> alarmFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    TunnelsAlarmState result = tunnelAlarmStateCalculator.calculate(refTunnels);
                    log.info("[TUNNEL-CALC] alarmStateCalc cost={}ms, tunnelCount={}",
                            System.currentTimeMillis() - t, refTunnels.size());
                    return result;
                }, stateUpdateExecutor);

        CompletableFuture<TunnelsAlignState> alignFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    TunnelsAlignState result = tunnelAlignStateCalculator.calculateAlignState(
                            refTunnels);
                    log.info("[TUNNEL-CALC] alignStateCalc cost={}ms",
                            System.currentTimeMillis() - t);
                    return result;
                }, stateUpdateExecutor);

        CompletableFuture<TunnelsOperState> operFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    TunnelsOperState result = tunnelOperationStateCalculator.calculate(refTunnels);
                    log.info("[TUNNEL-CALC] operStateCalc cost={}ms",
                            System.currentTimeMillis() - t);
                    return result;
                }, stateUpdateExecutor);

        long t1 = System.currentTimeMillis();
        CompletableFuture.allOf(alarmFuture, alignFuture, operFuture).join();
        log.info("[TUNNEL-CALC] parallelWait cost={}ms (includes queue time)",
                System.currentTimeMillis() - t1);
        TunnelsAlarmState tunnelsAlarmState = alarmFuture.join();
        TunnelsAlignState tunnelsAlignState = alignFuture.join();
        TunnelsOperState tunnelsOperState = operFuture.join();

        long t2 = System.currentTimeMillis();
        changeTunnelState(tunnelsAlignState, tunnelsAlarmState, tunnelsOperState);
        log.info("[TUNNEL-CALC] changeTunnelState cost={}ms", System.currentTimeMillis() - t2);
        log.info("[TUNNEL-CALC] total processTunnelStateCalculation cost={}ms",
                System.currentTimeMillis() - calcStart);

    }

    private void changeTunnelOperState(TunnelsOperState tunnelsOperState) {
        log.debug("change the site links oper status");
        OperationStateResult operationStateResult = OperationStateResult.builder()
                .tunnelsOperState(tunnelsOperState).build();
        stateChanger.changeState(
                PhysicalStateResult.builder().operationStateResult(operationStateResult).build());
    }

    private void changeTunnelAlarmState(TunnelsAlarmState tunnelsAlarmState) {
        log.debug("tunnel alarm state change");
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .tunnelAlarmState(tunnelsAlarmState).build();
        stateChanger.changeState(
                PhysicalStateResult.builder().alarmStateResult(alarmStateResult).build());
    }

    private void changeTunnelAlarmState(List<LinkStateDto> tunnels,
            AlarmSeverity newAlarmSeverity) {
        log.debug("change tunnel alarm state");
        List<TunnelAlarmState> tunnelAlarmStates = tunnels.stream().map(tunnel -> {
            AlarmSeverity alarmSeverity = tunnel.getAlarmSeverity();
            AlarmSeverity updateAlarmSeverity = StatusUtil.calculateAlarmSeverity(alarmSeverity,
                    newAlarmSeverity);
            return TunnelAlarmState.builder().tunnelId(tunnel.getId())
                    .alarmSeverity(updateAlarmSeverity).build();
        }).collect(Collectors.toList());
        stateChanger.changeState(PhysicalStateResult.builder().alarmStateResult(
                AlarmStateResult.builder().tunnelAlarmState(
                                TunnelsAlarmState.builder().tunnelAlarmStates(tunnelAlarmStates).build())
                        .build()).build());
    }

    private List<LinkStateDto> getRefTunnels(List<String> ochLinkIds) {
        List<String> tunnelIds = ochLinkIds.stream().filter(TunnelIdNamingRule::isTunnelId).collect(
                Collectors.toList());
        List<String> refOchLinkIds = ochLinkIds.stream().filter(OchLinkIdNamingRule::isOchLink)
                .collect(Collectors.toList());
        List<LinkStateDto> tunnels = new ArrayList<>();
        if (!refOchLinkIds.isEmpty()) {
            List<LinkStateDto> refTunnels = connectionCacheManager.getTunnelsByOchLinkIds(
                    refOchLinkIds);
            tunnels.addAll(refTunnels);
        }
        if (!tunnelIds.isEmpty()) {
            List<LinkStateDto> tunnelList = connectionCacheManager.batchGetAllTunnelsByIds(
                    tunnelIds);
            tunnels.addAll(tunnelList);
        }
        return tunnels;
    }


    private List<LinkStateDto> getPhyNodeRefTunnel(Node phyNode) {
        String phyNodeId = phyNode.getNodeId().getValue();
        log.debug("get phy node ref tunnel,phy node id is:{}", phyNodeId);
        Physical physical = phyNode.getAugmentation(Node1.class).getPhysical();
        NodeType nodeType = physical.getNodeType();
        List<LinkStateDto> tunnels = nodeCacheManager.getTunnelsByNe(nodeType, phyNodeId);
        return tunnels;
    }

    private void changeTunnelState(TunnelsAlignState tunnelsAlignState,
            TunnelsAlarmState tunnelsAlarmState,
            TunnelsOperState tunnelsOperState) {
        log.debug("start to change tunnel state");
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .tunnelAlarmState(tunnelsAlarmState).build();
        AlignStateResult alignStateResult = AlignStateResult.builder()
                .tunnelsAlignState(tunnelsAlignState).build();
        OperationStateResult operationStateResult = OperationStateResult.builder()
                .tunnelsOperState(tunnelsOperState).build();
        PhysicalStateResult physicalStateResult = PhysicalStateResult.builder()
                .alarmStateResult(alarmStateResult).alignStateResult(alignStateResult)
                .operationStateResult(operationStateResult).build();
        stateChanger.changeState(physicalStateResult);

        // Collect all link IDs that need cache invalidation
        List<String> updatedLinkIds = new ArrayList<>();

        // Invalidate cache for AlarmState
        if (tunnelsAlarmState != null && tunnelsAlarmState.getTunnelAlarmStates() != null) {
            updatedLinkIds.addAll(tunnelsAlarmState.getTunnelAlarmStates().stream()
                    .map(TunnelAlarmState::getTunnelId)
                    .collect(Collectors.toList()));
        }

        // Invalidate cache for AlignState
        if (tunnelsAlignState != null && tunnelsAlignState.getTunnelAlignStates() != null) {
            updatedLinkIds.addAll(tunnelsAlignState.getTunnelAlignStates().stream()
                    .map(tunnelAlignState -> tunnelAlignState.getTunnelId())
                    .collect(Collectors.toList()));
        }

        // Invalidate cache for OperState
        if (tunnelsOperState != null && tunnelsOperState.getTunnelOperStates() != null) {
            updatedLinkIds.addAll(tunnelsOperState.getTunnelOperStates().stream()
                    .map(TunnelsOperState.TunnelOperState::getTunnelId)
                    .collect(Collectors.toList()));
        }

        // Invalidate all collected links
        if (!updatedLinkIds.isEmpty()) {
            connectionCacheManager.invalidateLinksCache(updatedLinkIds);
        }
    }


}
