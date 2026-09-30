package net.flex.dci.otc.controller.status.core.handler.chain;

import static net.flex.dci.otc.controller.status.util.Constants.PHY_LINK_ORDER;
import static net.flex.dci.otc.controller.status.util.NmlKeyHelper.convertSigToLine;
import static net.flex.dci.otc.controller.status.util.NmlKeyHelper.isSigPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link.PhyLinkAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.align.PhyLinkAlignStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.operation.PhyLinkOperationStateCalculator;
import net.flex.dci.otc.controller.status.core.handler.AbstractStateChangeChainHandler;
import net.flex.dci.otc.controller.status.dto.PhysicalStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.PhyLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.PhyLinksAlarmState.PhyLinkAlarmState;
import net.flex.dci.otc.controller.status.dto.align.AlignStateResult;
import net.flex.dci.otc.controller.status.dto.align.PhyLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.PhyLinksAlignState.PhyLinkAlignState;
import net.flex.dci.otc.controller.status.dto.operation.OperationStateResult;
import net.flex.dci.otc.controller.status.dto.operation.PhyLinksOperState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/7 11:28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhyLinkStateChangeChainHandler extends AbstractStateChangeChainHandler {


    private final PhyLinkAlarmStateCalculator phyLinkAlarmStateCalculator;

    private final PhyLinkAlignStateCalculator phyLinkAlignStateCalculator;

    private final PhyLinkOperationStateCalculator phyLinkOperationStateCalculator;

    private final ConnectionCacheManager connectionCacheManager;

    @Qualifier("linkStateExecutor")
    private final ExecutorService stateUpdateExecutor;

    @Override
    public int getOrder() {
        return PHY_LINK_ORDER;
    }

    @Override
    public void stateChange(Node phyNode) {

        String nodeId = phyNode.getNodeId().getValue();
        long start = System.currentTimeMillis();
        log.info("[{}] start to handle the state change for phy Link", nodeId);
        long t1 = System.currentTimeMillis();
        List<LinkStateDto> phyLinks = connectionCacheManager.getByNodeId(nodeId);
        log.info("[{}] get phyLinks cost={}ms, count={}", nodeId, System.currentTimeMillis() - t1,
                phyLinks.size());

        if (!phyLinks.isEmpty()) {
            List<String> linkIds = phyLinks.stream()
                    .map(LinkStateDto::getId)
                    .collect(Collectors.toList());

            long t2 = System.currentTimeMillis();
            processPhyLinkStateCalculation(linkIds);
            log.info("[{}] processPhyLinkStateCalculation cost={}ms", nodeId,
                    System.currentTimeMillis() - t2);
        }

        log.info("[{}] PhyLinkStateChangeChainHandler total cost={}ms", nodeId,
                System.currentTimeMillis() - start);
        if (getNext() != null) {
            long t6 = System.currentTimeMillis();
            getNext().stateChange(phyNode);
            log.info("[{}] getNext().stateChange cost={}ms", nodeId,
                    System.currentTimeMillis() - t6);
        }
    }

    @Override
    public void alarmStateChange(List<String> linkId, List<Alarm> alarmList) {

        log.debug("alarmStateChange start, linkCount={}, alarmCount={}", linkId.size(),
                alarmList.size());
//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        List<Alarm> affectLinkAlarm = filterRefAlarmList(alarmList);
        if (!linkId.isEmpty() && !affectLinkAlarm.isEmpty()) {
            List<LinkStateDto> phyLinks = connectionCacheManager.getPhyLinksByIds(linkId);
            Map<String, List<Alarm>> linkToAlarms = buildLinkToAlarmsMap(phyLinks,
                    affectLinkAlarm);
            changePhyLinkStateByAlarms(phyLinks, linkToAlarms);
            log.info("PhyLinkStateChangeChainHandler.alarmStateChange cost={}ms, linkCount={}",
                    System.currentTimeMillis() - start, linkId.size());
            if (getNext() != null) {
                getNext().alarmStateChange(linkId);
            }
        }

//        }, stateUpdateExecutor);

    }

    @Override
    public void electricalLayerAlarmStateChange(List<String> linkIds, List<Alarm> alarmList) {
//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        log.debug("electricalLayer: PhyLinkHandler start, linkCount={}", linkIds.size());
        List<String> electricalLayerIds = new ArrayList<>();
        List<String> phyLinkIds = linkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isPhysicalLinkId).collect(
                        Collectors.toList());
        List<String> tunnelIds = linkIds.stream().filter(TunnelIdNamingRule::isTunnelId)
                .collect(
                        Collectors.toList());
        electricalLayerIds.addAll(tunnelIds);
        if (!phyLinkIds.isEmpty()) {
            List<LinkStateDto> phyLinks = connectionCacheManager.getPhyLinksByIds(
                    phyLinkIds);
            changeRefPhyLinkStateByAlarms(phyLinks, alarmList);
            List<String> refOchLinkIds = getPhyLinkRefOchLinkIds(phyLinkIds);
            electricalLayerIds.addAll(refOchLinkIds);

        }
        log.info("electricalLayer: PhyLinkStateChangeChainHandler cost={}ms",
                System.currentTimeMillis() - start);
        if (getNext() != null) {
            getNext().electricalLayerAlarmStateChange(electricalLayerIds, alarmList);
        }

//        }, stateUpdateExecutor);

    }

    private void changeRefPhyLinkStateByAlarms(List<LinkStateDto> phyLinks, List<Alarm> alarmList) {
        log.debug("change ref phy link state by alarms size:{}", phyLinks.size());
        Map<String, List<Alarm>> linkToAlarms = buildLinkToAlarmsMap(phyLinks, alarmList);
        changePhyLinkStateByAlarms(phyLinks, linkToAlarms);
    }

    @Override
    public void electricalLayerAlarmClearStateChange(List<String> linkIds) {
        log.debug("electrical layer alarm clear state change ");
        List<String> tunnelIds = linkIds.stream().filter(TunnelIdNamingRule::isTunnelId).collect(
                Collectors.toList());
        List<String> phyLinkIds = linkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isPhysicalLinkId).collect(
                        Collectors.toList());
//        CompletableFuture.runAsync(() -> {
        if (!phyLinkIds.isEmpty()) {
            List<LinkStateDto> phyLinks = connectionCacheManager.getPhyLinksByIds(
                    phyLinkIds);
            PhyLinksAlarmState phyLinksAlarmState = phyLinkAlarmStateCalculator.calculate(
                    phyLinks);
            changeLinkAlarmState(phyLinksAlarmState);

        }
        List<String> refOchLinkIds = getPhyLinkRefOchLinkIds(phyLinkIds);
        Set<String> totalElectricalLayerIds = new HashSet<>();
        totalElectricalLayerIds.addAll(refOchLinkIds);
        totalElectricalLayerIds.addAll(tunnelIds);
        if (!totalElectricalLayerIds.isEmpty()) {
            if (getNext() != null) {
                getNext().electricalLayerAlarmClearStateChange(
                        new ArrayList<>(totalElectricalLayerIds));
            }
        }
//        }, stateUpdateExecutor);
    }

    private List<String> getPhyLinkRefOchLinkIds(List<String> phyLinkIds) {
        if (phyLinkIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<LinkStateDto> ochLinkStateDtos = connectionCacheManager.getOchLinkByPhyLinkId(
                phyLinkIds);
        return ochLinkStateDtos.stream().map(LinkStateDto::getId).collect(
                Collectors.toList());
    }

    private Map<String, List<Alarm>> buildLinkToAlarmsMap(List<LinkStateDto> phyLinks,
            List<Alarm> alarmList) {
        Map<String, List<Alarm>> linkToAlarms = new HashMap<>();
        for (LinkStateDto phyLink : phyLinks) {
            String linkId = phyLink.getId();
            for (Alarm alarm : alarmList) {
                if (isAlarmAffectLink(alarm, linkId)) {
                    linkToAlarms.computeIfAbsent(linkId, k -> new ArrayList<>()).add(alarm);
                }
            }
        }
        return linkToAlarms;
    }

    private boolean isAlarmAffectLink(Alarm alarm, String linkId) {
//        String nmlKey = alarm.getToopKey();
//        return linkId.contains(nmlKey);

        String nmlKey = alarm.getToopKey();
        if (linkId.contains(nmlKey)) {
            return true;
        }

        if (isSigPort(nmlKey)) {
            return linkId.contains(convertSigToLine(nmlKey));
        }
        return false;
    }

    private void changePhyLinkStateByAlarms(List<LinkStateDto> phyLinks,
            Map<String, List<Alarm>> linkToAlarms) {
        List<PhyLinkAlarmState> alarmStates = phyLinks.stream()
                .filter(phyLink -> linkToAlarms.containsKey(phyLink.getId())).map(phyLink -> {
                    String phyLinkId = phyLink.getId();
                    AlarmSeverity currentAlarmSeverity = phyLink.getAlarmSeverity();
                    List<Alarm> linkAlarms = linkToAlarms.getOrDefault(phyLinkId,
                            Collections.emptyList());
                    AlarmSeverity alarmSeverity = StatusUtil.calculateAlarmSeverity(linkAlarms);
                    AlarmSeverity updateAlarmSeverity = StatusUtil.calculateAlarmSeverity(
                            currentAlarmSeverity, alarmSeverity);
                    return PhyLinkAlarmState.builder().phyLinkId(phyLinkId)
                            .alarmSeverity(updateAlarmSeverity).build();
                }).collect(Collectors.toList());
        if (!alarmStates.isEmpty()) {
            stateChanger.changeState(
                    PhysicalStateResult.builder().alarmStateResult(
                            AlarmStateResult.builder()
                                    .phyLinkAlarmState(PhyLinksAlarmState.builder()
                                            .phyLinksAlarmStates(alarmStates)
                                            .build()).build()).build());
            List<String> updatedLinkIds = alarmStates.stream()
                    .map(PhyLinkAlarmState::getPhyLinkId)
                    .collect(Collectors.toList());
            connectionCacheManager.invalidateLinksCache(updatedLinkIds);
        }
    }


    @Override
    public void alarmClearStateChange(List<String> phyLinkIds) {

        log.debug("alarmClearStateChange start, linkCount={}", phyLinkIds.size());
        if (phyLinkIds.isEmpty()) {
            return;
        }
//        CompletableFuture.runAsync(() -> {
        long start = System.currentTimeMillis();
        List<LinkStateDto> phyLinks = getRefPhyLink(phyLinkIds);
        PhyLinksAlarmState phyLinksAlarmState = phyLinkAlarmStateCalculator.calculate(
                phyLinks);
        changeLinkAlarmState(phyLinksAlarmState);
        log.info("PhyLinkStateChangeChainHandler.alarmClearStateChange cost={}ms, linkCount={}",
                System.currentTimeMillis() - start, phyLinkIds.size());
        if (getNext() != null) {
            getNext().alarmClearStateChange(phyLinkIds);
        }
//        }, stateUpdateExecutor);

    }

    private void changeLinkAlarmState(PhyLinksAlarmState phyLinksAlarmState) {
        log.debug("change link alarm state , {}", phyLinksAlarmState);
//        phyLinksAlarmState.getPhyLinksAlarmStates().forEach(phyLinkAlarmState -> {
//            phyLinkDao.updateLinkAlarmState(phyLinkAlarmState.getPhyLinkId(),
//                    phyLinkAlarmState.getAlarmSeverity());
//        });
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .phyLinkAlarmState(phyLinksAlarmState).build();
        stateChanger.changeState(
                PhysicalStateResult.builder().alarmStateResult(alarmStateResult).build());
        List<String> updatedLinkIds = phyLinksAlarmState.getPhyLinksAlarmStates().stream()
                .map(PhyLinkAlarmState::getPhyLinkId)
                .collect(Collectors.toList());
        connectionCacheManager.invalidateLinksCache(updatedLinkIds);
    }


    @Override
    public void statusEventChange(List<String> linkIds) {
        long start = System.currentTimeMillis();
        log.info("start to handle status event change for linkIds:{}, count={}", linkIds,
                linkIds.size());
        List<String> phyLinkIds = linkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isPhysicalLinkId).collect(Collectors.toList());
        if (!phyLinkIds.isEmpty()) {

            processPhyLinkStateCalculation(phyLinkIds);

        }

        if (getNext() != null) {
            long t3 = System.currentTimeMillis();
            getNext().statusEventChange(linkIds);
            log.info("getNext().statusEventChange cost={}ms", System.currentTimeMillis() - t3);
        }
        log.info("PhyLinkStateChangeChainHandler.statusEventChange total cost={}ms",
                System.currentTimeMillis() - start);
    }

    private void processPhyLinkStateCalculation(List<String> linkIds) {
        log.debug("process phy link state calculation :{}", linkIds);
        List<LinkStateDto> phyLinks = connectionCacheManager.getPhyLinksByIds(linkIds);
        if (phyLinks.isEmpty()) {
            return;
        }
        long calcStart = System.currentTimeMillis();
        CompletableFuture<PhyLinksAlarmState> alarmFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    PhyLinksAlarmState result = phyLinkAlarmStateCalculator.calculate(phyLinks);
                    log.info("[PHYLINK-CALC] alarmStateCalc cost={}ms, phyLinkCount={}",
                            System.currentTimeMillis() - t, phyLinks.size());
                    return result;
                }, stateUpdateExecutor);

        CompletableFuture<PhyLinksAlignState> alignFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    PhyLinksAlignState result = phyLinkAlignStateCalculator.calculateAlignState(
                            phyLinks);
                    log.info("[PHYLINK-CALC] alignStateCalc cost={}ms",
                            System.currentTimeMillis() - t);
                    return result;
                }, stateUpdateExecutor);

        CompletableFuture<PhyLinksOperState> operFuture = CompletableFuture.supplyAsync(
                () -> {
                    long t = System.currentTimeMillis();
                    PhyLinksOperState result = phyLinkOperationStateCalculator.calculate(phyLinks);
                    log.info("[PHYLINK-CALC] operStateCalc cost={}ms",
                            System.currentTimeMillis() - t);
                    return result;
                }, stateUpdateExecutor);

        long t1 = System.currentTimeMillis();
        CompletableFuture.allOf(alarmFuture, alignFuture, operFuture).join();
        log.info("[PHYLINK-CALC] parallelWait cost={}ms (includes queue time)",
                System.currentTimeMillis() - t1);
        PhyLinksAlarmState phyLinksAlarmState = alarmFuture.join();
        PhyLinksAlignState phyLinksAlignState = alignFuture.join();
        PhyLinksOperState phyLinksOperState = operFuture.join();

        long t2 = System.currentTimeMillis();
        changeLinkState(phyLinksAlarmState, phyLinksAlignState, phyLinksOperState);
        log.info("[PHYLINK-CALC] changeLinkState cost={}ms", System.currentTimeMillis() - t2);
        log.info("[PHYLINK-CALC] total processPhyLinkStateCalculation cost={}ms",
                System.currentTimeMillis() - calcStart);
    }


    /**
     * change phy link state
     *
     * @param phyLinks
     * @param alarmSeverity
     */
    private void changePhyLinkState(List<LinkStateDto> phyLinks, AlarmSeverity alarmSeverity) {
        log.debug("change phyLink state");
        List<PhyLinkAlarmState> alarmStates = phyLinks.stream().map(phyLink -> {
            AlarmSeverity currentAlarmSeverity = phyLink.getAlarmSeverity();
            AlarmSeverity updateAlarmSeverity = StatusUtil.calculateAlarmSeverity(
                    currentAlarmSeverity, alarmSeverity);
            return PhyLinkAlarmState.builder().phyLinkId(phyLink.getId())
                    .alarmSeverity(updateAlarmSeverity).build();
        }).collect(Collectors.toList());
        stateChanger.changeState(
                PhysicalStateResult.builder().alarmStateResult(
                        AlarmStateResult.builder().phyLinkAlarmState(PhyLinksAlarmState.builder()
                                .phyLinksAlarmStates(alarmStates)
                                .build()).build()).build());
        List<String> updatedLinkIds = alarmStates.stream()
                .map(PhyLinkAlarmState::getPhyLinkId)
                .collect(Collectors.toList());
        connectionCacheManager.invalidateLinksCache(updatedLinkIds);
    }

    private List<LinkStateDto> getRefPhyLink(List<String> phyLinkIds) {
        log.debug("get ref phy link,neId is :{}", phyLinkIds);
        List<LinkStateDto> phyLinks = new ArrayList<>();
//        if (nmlKeyDto.getTpId() != null) {
//            phyLinks = phyLinkCacheManager.getByTpId(nmlKeyDto.getTpId());
//        } else if (nmlKeyDto.getEquipId() != null) {
//            phyLinks = phyLinkCacheManager.getByEquipId(nmlKeyDto.getEquipId());
//        } else if (nmlKeyDto.getPhyNodeId() != null) {
//            phyLinks = phyLinkCacheManager.getByNodeId(nmlKeyDto.getPhyNodeId());
//        }
        phyLinks = connectionCacheManager.getPhyLinksByIds(phyLinkIds);
        return phyLinks;
    }

    private List<Alarm> filterRefAlarmList(List<Alarm> alarmList) {
        List<Alarm> alarms = new ArrayList<>();
//        for (Alarm alarm : alarmList) {
//            Boolean sa = alarm.getServiceAffect();
//            if (sa) {
//                if (alarmConfigurationProperties.isFilter()) {
//                    String alarmGroup = alarm.getGroup();
//                    String alarmText = alarm.getText();
//                    if (alarmConfigurationProperties.isContainsKeyWords(alarmText)) {
//                        alarms.add(alarm);
//                    } else if (alarmConfigurationProperties.isContainsKeyWords(alarmGroup)) {
//                        alarms.add(alarm);
//                    }
//                }
//            }
//        }
        alarms = alarmList;
        return alarms;
    }

    private void changeLinkOperState(PhyLinksOperState phyLinksOperState) {
        log.debug("start to change link operation state");
        OperationStateResult operationStateResult = OperationStateResult.builder()
                .phyLinksOperState(phyLinksOperState).build();
        stateChanger.changeState(
                PhysicalStateResult.builder().operationStateResult(operationStateResult).build());
    }


    private void changeLinkState(PhyLinksAlarmState phyLinksAlarmState,
            PhyLinksAlignState phyLinksAlignState, PhyLinksOperState phyLinksOperState) {
        log.debug("change the link state");
        AlarmStateResult alarmStateResult =
                phyLinksAlarmState == null ? null : AlarmStateResult.builder()
                        .phyLinkAlarmState(phyLinksAlarmState).build();
        AlignStateResult alignStateResult =
                phyLinksAlignState == null ? null : AlignStateResult.builder()
                        .phyLinksAlignState(phyLinksAlignState).build();
        OperationStateResult operationStateResult =
                phyLinksOperState == null ? null : OperationStateResult.builder()
                        .phyLinksOperState(phyLinksOperState).build();
        PhysicalStateResult physicalStateResult = PhysicalStateResult.builder()
                .alarmStateResult(alarmStateResult).alignStateResult(alignStateResult)
                .operationStateResult(operationStateResult).build();
        stateChanger.changeState(physicalStateResult);

        // Collect all link IDs that need cache invalidation
        List<String> updatedLinkIds = new ArrayList<>();

        // Invalidate cache for AlarmState
        if (phyLinksAlarmState != null && phyLinksAlarmState.getPhyLinksAlarmStates() != null) {
            updatedLinkIds.addAll(phyLinksAlarmState.getPhyLinksAlarmStates().stream()
                    .map(PhyLinkAlarmState::getPhyLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate cache for AlignState
        if (phyLinksAlignState != null && phyLinksAlignState.getPhyLinkAlignStates() != null) {
            updatedLinkIds.addAll(phyLinksAlignState.getPhyLinkAlignStates().stream()
                    .map(PhyLinkAlignState::getLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate cache for OperState
        if (phyLinksOperState != null && phyLinksOperState.getPhyLinkOperStates() != null) {
            updatedLinkIds.addAll(phyLinksOperState.getPhyLinkOperStates().stream()
                    .map(PhyLinksOperState.PhyLinkOperState::getPhyLinkId)
                    .collect(Collectors.toList()));
        }

        // Invalidate all collected links
        if (!updatedLinkIds.isEmpty()) {
            connectionCacheManager.invalidateLinksCache(updatedLinkIds);
        }
    }


}
