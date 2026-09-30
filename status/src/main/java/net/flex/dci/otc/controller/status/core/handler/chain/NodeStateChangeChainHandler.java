package net.flex.dci.otc.controller.status.core.handler.chain;

import static net.flex.dci.otc.controller.status.util.Constants.NODE_ORDER;
import static net.flex.dci.otc.controller.status.util.NmlKeyHelper.convertSigToLine;
import static net.flex.dci.otc.controller.status.util.NmlKeyHelper.isSigPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.StatusChangeObjectType;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.RackAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.SiteNodeAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.calculator.alarm.detail.ViewNodeAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.enums.CustomAdminStatus;
import net.flex.dci.otc.controller.status.core.enums.CustomAlignmentStatus;
import net.flex.dci.otc.controller.status.core.enums.CustomOperationStatus;
import net.flex.dci.otc.controller.status.core.handler.AbstractStateChangeChainHandler;
import net.flex.dci.otc.controller.status.core.worker.SiteAggregateWorker;
import net.flex.dci.otc.controller.status.counter.CounterManager;
import net.flex.dci.otc.controller.status.dto.PhysicalStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.RackAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.SiteNodeAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.ViewNodeAlarmState;
import net.flex.dci.otc.controller.status.dto.nmlkey.NmlKeyDto;
import net.flex.dci.otc.controller.status.util.NmlKeyHelper;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otc.mongo.dto.NodeInfoDto;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * handle phy node view node site node alarm state
 *
 * @version 1.0
 * @date 2022/4/7 11:23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeStateChangeChainHandler extends AbstractStateChangeChainHandler {

    private final ViewNodeAlarmStateCalculator viewNodeAlarmStateCalculator;

    private final SiteNodeAlarmStateCalculator siteNodeAlarmStateCalculator;

    private final RackAlarmStateCalculator rackAlarmStateCalculator;


    private final PhyNodeDao phyNodeDao;


    private final NodeCacheManager nodeCacheManager;

    private final ConnectionCacheManager connectionCacheManager;

    private final EquipmentsDao equipmentsDao;

    private final AlarmDaoService alarmDaoService;

    private final CounterManager counterManager;

    private final SiteAggregateWorker siteAggregateWorker;


    @Qualifier("nodeStateExecutor")
    private final ExecutorService stateUpdateExecutor;

    @Override
    public int getOrder() {
        return NODE_ORDER;
    }

    @Override
    public void stateChange(Node phyNode) {
        long start = System.currentTimeMillis();
        String neId = phyNode.getNodeId().getValue();
        log.info("[{}] start to handle the node state change", neId);
        counterManager.refreshNeCounters(neId);
        siteAggregateWorker.submit(neId);
        long t4 = System.currentTimeMillis();
        log.info("[{}] updateRefNodeAlarmState cost={}ms", neId, System.currentTimeMillis() - t4);

        long totalCost = System.currentTimeMillis() - start;
        log.info("[{}] NodeStateChangeChainHandler total cost={}ms", neId, totalCost);
        if (totalCost > 2000) {
            log.warn("[MONITOR] slow stateChange: {} total={}ms", neId, totalCost);
        }
        if (getNext() != null) {
            getNext().stateChange(phyNode);
        }
    }

    /**
     * current status event change only tp or card
     *
     * @param statusChangeEvent
     */
    @Override
    public void statusEventChange(StatusChangeEvent statusChangeEvent) {
        long start = System.currentTimeMillis();
        StatusChangeObjectType objectType = statusChangeEvent.getStatusChangeObjectType();
        String objectId = statusChangeEvent.getObjectId();
        log.info(
                "[{}] start to handle the status change event for object Type is:{}",
                objectId, objectType);
        String adminStatusStr = statusChangeEvent.getAdminStatus();
        String operStateStr = statusChangeEvent.getOperStatus();
        String alignmentStateStr = statusChangeEvent.getAlignment();
        AdminStatus adminStatus =
                StringUtils.hasText(adminStatusStr) ? CustomAdminStatus.fromStateName(
                        adminStatusStr) : null;
        OperStatus operStatus =
                StringUtils.hasText(operStateStr) ? CustomOperationStatus.getOperStatusFromName(
                        operStateStr) : null;
        AlignmentStatusType alignmentStatusType = StringUtils.hasText(alignmentStateStr)
                ? CustomAlignmentStatus.getAlignmentStatusFromName(alignmentStateStr) : null;
        if (operStatus == null && alignmentStatusType == null) {
            log.warn("[{}] operation status change is null,discard it", objectId);
            return;
        }

        long t1 = System.currentTimeMillis();
        syncPartialConfigWithRunData(objectType, objectId);
        log.info("[{}] syncPartialConfigWithRunData cost={}ms", objectId,
                System.currentTimeMillis() - t1);

        long t2 = System.currentTimeMillis();
        List<String> refPhyLinkIds = getRefPhyLinkIds(objectType, objectId);
        log.info("[{}] getRefPhyLinkIds cost={}ms, count={}", objectId,
                System.currentTimeMillis() - t2, refPhyLinkIds.size());

        if (getNext() != null) {
            long t3 = System.currentTimeMillis();
            getNext().statusEventChange(refPhyLinkIds);
            log.info("[{}] getNext().statusEventChange cost={}ms", objectId,
                    System.currentTimeMillis() - t3);
        }

        log.info("[{}] NodeStateChangeChainHandler.statusEventChange total cost={}ms", objectId,
                System.currentTimeMillis() - start);
    }


    @Override
    public void alarmStateChange(String nmlKey, List<Alarm> alarmList, PhyNodeCache phyNodeCache) {
        long start = System.currentTimeMillis();
        log.debug("[{}] alarmStateChange start, alarmCount={}", nmlKey, alarmList.size());
        if (null == phyNodeCache) {
            log.warn("meaningless alarm discard it,skip");
            return;
        }
        NodeType nodeType = NodeType.valueOf(phyNodeCache.getPhyNodeType());
        List<Alarm> changedAlarms = filterChanged(nmlKey, alarmList, false);
        if (changedAlarms.isEmpty()) {
            counterManager.discardNeChange(nmlKey);
            log.debug("[{}] no effective alarm change, skip", nmlKey);
            return;
        }
        Set<String> phyLinkIds = new HashSet<>();
        Set<String> electricalTunnelIds = new HashSet<>();

        relativeThingsAlarmSeverityChange(nmlKey);
        List<Alarm> refTpAlarms = filterLinkAlarms(changedAlarms);
        if (refTpAlarms.isEmpty()) {
            log.debug("the ref tp alarms is empty,discard it and do nothing");
            return;
        }

        resolveAffectedLinkIds(refTpAlarms, phyLinkIds, electricalTunnelIds);
        if (nodeType.equals(NodeType.OD) || nodeType.equals(NodeType.OPC4)) {
            if (!phyLinkIds.isEmpty()) {
                List<String> phyLinkIdList = new ArrayList<>(phyLinkIds);
//                    recalculateRelativeThingAlarmStateByLinks(phyLinkIdList);
                if (getNext() != null) {
                    getNext().alarmStateChange(phyLinkIdList, alarmList);
                }
            }
        } else {
            List<String> affectedElectricalLayerIds = new ArrayList<>(electricalTunnelIds);
            affectedElectricalLayerIds.addAll(phyLinkIds);
            if (!affectedElectricalLayerIds.isEmpty() && getNext() != null) {
                long chainStart = System.currentTimeMillis();
                getNext().electricalLayerAlarmStateChange(affectedElectricalLayerIds,
                        alarmList);
                log.info("[{}] electric: electricalLayerAlarmStateChange chain cost={}ms",
                        nmlKey,
                        System.currentTimeMillis() - chainStart);
            }
        }
        log.info("[{}] alarmStateChange cost={}ms, nodeType={}", nmlKey,
                System.currentTimeMillis() - start, nodeType);

    }

    private List<Alarm> filterChanged(String neId, List<Alarm> alarmList, boolean isCleared) {
        List<Alarm> changedAlarm = new ArrayList<>();
        for (Alarm alarm : alarmList) {
            if (counterManager.updateAndCheckChanged(alarm.getToopKey(), neId,
                    AlarmSeverity.valueOf(alarm.getSeverity()), isCleared)) {
                changedAlarm.add(alarm);
            }
        }
        return changedAlarm;
    }

    private List<Alarm> filterLinkAlarms(List<Alarm> alarmList) {
        List<Alarm> alarms = new ArrayList<>();
        for (Alarm alarm : alarmList) {
            String toopKey = alarm.getToopKey();
            NmlKeyDto nmlKeyDto = NmlKeyHelper.getDetailInfoFromNmlKey(toopKey);
            if (nmlKeyDto != null && (nmlKeyDto.getTpId() != null
                    || nmlKeyDto.getEquipId() != null)) {
                alarms.add(alarm);
            }
        }
        log.debug("filter tp alarms count:{}", alarms.size());
        return alarms;
    }


    /**
     * 合并 getAffectedPhyLinkIds + getAffectedElectricalLayerIds，一次遍历完成两种查询 避免同一批 alarm 的 NmlKey 解析和
     * cache 查询重复两遍
     */
    private void resolveAffectedLinkIds(List<Alarm> alarmList,
            Set<String> phyLinkIds, Set<String> electricalIds) {
        Set<String> tpIds = new HashSet<>();
        Set<String> equipIds = new HashSet<>();
        Set<String> phyNodeIds = new HashSet<>();

        for (Alarm alarm : alarmList) {
            NmlKeyDto nmlKeyDto = NmlKeyHelper.getDetailInfoFromNmlKey(alarm.getToopKey());
            if (nmlKeyDto == null) {
                continue;
            }
            if (nmlKeyDto.getTpId() != null) {
                tpIds.add(nmlKeyDto.getTpId());
                if (isSigPort(nmlKeyDto.getTpId())) {
                    tpIds.add(convertSigToLine(nmlKeyDto.getTpId()));
                }
            } else if (nmlKeyDto.getEquipId() != null) {
                equipIds.add(nmlKeyDto.getEquipId());
            } else if (nmlKeyDto.getPhyNodeId() != null) {
                phyNodeIds.add(nmlKeyDto.getPhyNodeId());
            }
        }

        long queryStart = System.currentTimeMillis();

        Map<String, List<LinkStateDto>> tpLinks = connectionCacheManager.batchGetByTpIds(tpIds);
        Map<String, List<LinkStateDto>> tpTunnels = connectionCacheManager.batchGetTunnelByTpIds(
                tpIds);

        for (Map.Entry<String, List<LinkStateDto>> entry : tpLinks.entrySet()) {
            List<LinkStateDto> links = entry.getValue();
            if (links != null) {
                links.forEach(l -> phyLinkIds.add(l.getId()));
            }
        }
        for (Map.Entry<String, List<LinkStateDto>> entry : tpTunnels.entrySet()) {
            List<LinkStateDto> tunnels = entry.getValue();
            if (tunnels != null) {
                tunnels.forEach(t -> electricalIds.add(t.getId()));
            }
        }

        Map<String, List<LinkStateDto>> equipLinks = connectionCacheManager.batchGetByEquipIds(
                equipIds);
        for (Map.Entry<String, List<LinkStateDto>> entry : equipLinks.entrySet()) {
            List<LinkStateDto> links = entry.getValue();
            if (links != null) {
                links.forEach(l -> phyLinkIds.add(l.getId()));
            }
        }

        Map<String, List<LinkStateDto>> nodeLinks = connectionCacheManager.batchGetByNodeIds(
                phyNodeIds);
        for (Map.Entry<String, List<LinkStateDto>> entry : nodeLinks.entrySet()) {
            List<LinkStateDto> links = entry.getValue();
            if (links != null) {
                links.forEach(l -> phyLinkIds.add(l.getId()));
            }
        }

        log.debug(
                "resolveAffectedLinkIds: tpIds={}, equipIds={}, nodeIds={}, query cost={}ms, phyLinkIds={}, electricalIds={}",
                tpIds.size(), equipIds.size(), phyNodeIds.size(),
                System.currentTimeMillis() - queryStart, phyLinkIds.size(), electricalIds.size());
    }


    @Override
    public void alarmClearStateChange(String nmlKey, List<Alarm> alarmList,
            PhyNodeCache phyNodeCache) {
        long start = System.currentTimeMillis();

        log.debug("[{}] alarmClearStateChange start, alarmCount={}", nmlKey, alarmList.size());
        if (null == phyNodeCache) {
            log.warn("meaningless alarm discard it,skip");
            return;
        }

        NodeType nodeType = NodeType.valueOf(phyNodeCache.getPhyNodeType());
        List<Alarm> changedAlarms = filterChanged(nmlKey, alarmList, true);
        if (changedAlarms.isEmpty()) {
            counterManager.discardNeChange(nmlKey);
            log.debug("[{}] no effective alarm clear change, skip", nmlKey);
            return;
        }
        Set<String> phyLinkIds = new HashSet<>();
        Set<String> electricalTunnelIds = new HashSet<>();
        recalculateRelativeThingAlarmState(nmlKey);
        List<Alarm> refTpAlarms = filterLinkAlarms(changedAlarms);
        if (refTpAlarms.isEmpty()) {
            log.debug("the ref tp alarms is empty,discard it and do nothing");
            return;
        }
        resolveAffectedLinkIds(alarmList, phyLinkIds, electricalTunnelIds);
        if (nodeType.equals(NodeType.OD) || nodeType.equals(NodeType.OPC4)) {
            List<String> affectedPhyLinkIds = new ArrayList<>(phyLinkIds);
            log.info("affected phy link ids:{}", affectedPhyLinkIds);
            if (!phyLinkIds.isEmpty()) {
                if (getNext() != null) {
                    getNext().alarmClearStateChange(affectedPhyLinkIds);
                }
            }
        } else {
            List<String> affectedElectricalLayerIds = new ArrayList<>(electricalTunnelIds);
            affectedElectricalLayerIds.addAll(phyLinkIds);
            if (!affectedElectricalLayerIds.isEmpty()) {
                if (getNext() != null) {
                    getNext().electricalLayerAlarmClearStateChange(
                            new ArrayList<>(affectedElectricalLayerIds));
                }
            }
        }
        log.info("[{}] alarmClearStateChange cost={}ms", nmlKey,
                System.currentTimeMillis() - start);
    }


    private Set<String> getAffectedNeIdsFromLinks(List<String> linkIds) {
        Set<String> neIds = new HashSet<>();
        List<LinkStateDto> phyLinks = connectionCacheManager.getPhyLinksByIds(linkIds);
        for (LinkStateDto link : phyLinks) {
            String sourceNeId = PhysicalLinkIdNamingRule.getNodeAId(link.getId());
            String destNeId = PhysicalLinkIdNamingRule.getNodeZId(link.getId());
            neIds.add(sourceNeId);
            neIds.add(destNeId);
        }
        return neIds;
    }

    @Override
    public void phyNodeRemoveStateChange(String neId) {
        log.debug("start to calculate the physical ne remove state change,the neId is :{}", neId);
//        calculateNodeRelateRackSiteAlarmState(neId);
        siteAggregateWorker.submit(neId);
    }

    private void syncPartialConfigWithRunData(StatusChangeObjectType objectType, String objectId) {
        log.debug(
                "handle the sync partial config data with run data ,status change object :{} and objectId:{}",
                objectType, objectId);
        String neId = null;
        switch (objectType) {
            case CARD:
                neId = PhysicalEqpIdNamingRule.getNodeId(objectId);
                break;
            case DEVICE:
                neId = objectId;
                break;
            case TERMINATION_POINT:
                neId = PhysicalTpIdNamingRule.getNodeId(objectId);
                break;
        }
        NodeInfoDto neOpDto = phyNodeDao.getOpPhyNodeInfoById(neId);
//        log.info("[{}] Operational node change the state is:{}", neId, neOpDto);
        if (neOpDto == null) {
            log.warn("[{}] Operational node not found, skip config sync", neId);
            return;
        }
        AlarmSeverity opAlarmSeverity = calculateCurrentAlarmSeverityForPhyNode(neId);
        log.info("neId:{} current opAlarmSeverity:{}", neId, opAlarmSeverity);
        OperStatus opOperationalState = neOpDto.getOperStatus();
        AlignmentStatusType opAlignmentStatusType = neOpDto.getAlignment();
        AdminStatus opAdminStatus = neOpDto.getAdminStatus();
        long dbStart = System.currentTimeMillis();
        phyNodeDao.updateConfigNodeState(neId, opAlarmSeverity,
                opOperationalState, null,
                opAdminStatus, opAlignmentStatusType);
        log.debug("[{}] syncPartialConfigWithRunData DB write cost={}ms", neId,
                System.currentTimeMillis() - dbStart);
        nodeCacheManager.invalidateConfigNode(neId);
        nodeCacheManager.invalidateOpNode(neId);
        nodeCacheManager.invalidateAllTopologyCache(neId);
        connectionCacheManager.invalidateByNodeId(neId);
        log.info("[{}] invalidated all caches after DB write", neId);
    }

    private List<String> getRefPhyLinkIds(StatusChangeObjectType objectType, String objectId) {
        log.debug("get ref phy link ids objectType is:{} and objectId:{}", objectType, objectId);
        List<String> phyLinkIds = new ArrayList<>();
        List<LinkStateDto> refPhyLinks = new ArrayList<>();
        if (objectType == StatusChangeObjectType.TERMINATION_POINT) {
            refPhyLinks = getRefPhyLinkAndTunnelIdsByTpId(objectId);
        } else if (objectType == StatusChangeObjectType.CARD) {
            refPhyLinks = connectionCacheManager.getByEquipId(objectId);
        } else if (objectType == StatusChangeObjectType.DEVICE) {
            refPhyLinks = connectionCacheManager.getByNodeId(objectId);
        }
        phyLinkIds = refPhyLinks.stream().map(LinkStateDto::getId)
                .collect(Collectors.toList());
        return phyLinkIds;
    }

    private List<LinkStateDto> getRefPhyLinkAndTunnelIdsByTpId(String tpId) {
        log.info("get termination point ref links tp id:{}", tpId);
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = nodeCacheManager.getConfigNode(nodeId);
        List<LinkStateDto> linkStateDtoList = new ArrayList<>();
        if (node != null && node.getTerminationPoint() != null) {
            TerminationPoint tp = node.getTerminationPoint().stream()
                    .filter(t -> t.getTpId().getValue().equals(tpId))
                    .findFirst().orElse(null);

            if (tp != null) {
                PortType portType = tp.getAugmentation(TerminationPoint1.class)
                        .getPhysical().getPortType();
                if (portType.equals(PortType.OTUClient)) {
                    linkStateDtoList = connectionCacheManager.getTunnelByTpId(tpId);
                    log.info("[{}] getTunnelByTpId result count={}", tpId, linkStateDtoList.size());
                } else {
                    linkStateDtoList = connectionCacheManager.getByTpId(tpId);
                    log.info("[{}] getByTpId result count={}", tpId, linkStateDtoList.size());
                }
            }
        }
        return linkStateDtoList;
    }


    /**
     * recalculate the relative alarm state for tp eq and tp
     *
     * @param neId
     */
    private void recalculateRelativeThingAlarmState(String neId) {
        log.debug("recalculate relative alarm severity for tp eq node site");
        // alarm state change clear calculate start for the adapter both tp eq and node
        //site
        if (!counterManager.isNeSeverityChanged(neId)) {
            return;
        }
//        recalculateRefSiteAlarmState(neId);
        siteAggregateWorker.submit(neId);
    }


    private AlarmSeverity calculateCurrentAlarmSeverityForPhyNode(String phyNeId) {
        List<String> neIds = Collections.singletonList(phyNeId);
        List<AlarmRecord> alarmRecords = alarmDaoService.getCurrentAlarmsByNeIds(neIds);
        List<AlarmSeverity> alarmSeverities = alarmRecords.stream()
                .map(AlarmRecord::getSeverity).collect(Collectors.toList());
        AlarmSeverity currentAlarmState = StatusUtil.calculateAlarmStateByList(alarmSeverities);
        return currentAlarmState;
    }


    private void relativeThingsAlarmSeverityChange(String neId) {
        log.debug("relative things alarm severity change,change alarmSeverity is :{}",
                neId);
        if (!counterManager.isNeSeverityChanged(neId)) {
            log.debug("current ne alarm is not changed,discard it neId:{}", neId);
            return;
        }
//        String siteId = PhysicalNodeIdNamingRule.getSiteId(neId);
//        updateRefSiteNodeAlarmState(siteId, neId);
        siteAggregateWorker.submit(neId);
    }


    private void updateRefNodeAlarmState(List<RackAlarmState> rackAlarmState,
            List<ViewNodeAlarmState> viewNodeAlarmState,
            SiteNodeAlarmState siteNodeAlarmState) {
        log.debug("update the ref view node and site node alarm state");
        AlarmStateResult alarmStateResult = AlarmStateResult.builder()
                .rackAlarmStateList(rackAlarmState)
                .viewNodeAlarmStates(viewNodeAlarmState).siteNodeAlarmState(siteNodeAlarmState)
                .build();
        stateChanger.changeState(
                PhysicalStateResult.builder().alarmStateResult(alarmStateResult).build());
    }


}

