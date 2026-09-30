package net.flex.dci.otc.controller.status.core.calculator.alarm.detail;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.AbstractAlarmStateCalculator;
import net.flex.dci.otc.controller.status.dto.alarm.RackAlarmState;
import net.flex.dci.otc.mongo.dto.SupportingNeDto;
import net.flex.dci.otc.mongo.dto.SupportingRackDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 8/2/2023 4:27 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RackAlarmStateCalculator extends
        AbstractAlarmStateCalculator<RackAlarmState, Node> {


    private final NodeCacheManager nodeCacheManager;


    @Override
    public RackAlarmState calculate(Node node) {
        String neId = node.getNodeId().getValue();
        log.debug("start to calculate phy node ref rack alarm state,the node id is:{}", neId);
        SupportingRackDto supportingRack = nodeCacheManager.getNeRefRackByNeId(neId);
        String supportingRackId = supportingRack.getRackId();
        log.debug("start to calculate the ref rack alarm state,rack id is:{}", supportingRackId);
        List<String> inRackNeIds = supportingRack.getSupportingNe().stream()
                .map(SupportingNeDto::getNodeRef).collect(Collectors.toList());
        String refSiteId = PhysicalNodeIdNamingRule.getSiteId(neId);
        if (inRackNeIds.isEmpty()) {
            log.info("this is useless alarm,discard it");
            return RackAlarmState.builder().rackId(supportingRackId)
                    .alarmSeverity(AlarmSeverity.Unknown).siteNodeId(refSiteId)
                    .neIds(inRackNeIds).build();
        }
        AlarmSeverity currentAlarmState = calculateCurrentAlarmSeverity(inRackNeIds);
        return RackAlarmState.builder().rackId(supportingRackId).alarmSeverity(currentAlarmState)
                .siteNodeId(refSiteId).neIds(inRackNeIds).build();
    }

    @Override
    public RackAlarmState calculate(String phyNodeId) {
        log.debug(
                "start to calculate current alarm change for the phyNode is is :{}",
                phyNodeId);
        SupportingRackDto supportingRackDto = nodeCacheManager.getNeRefRackByNeId(phyNodeId);
        log.debug("supporting rack is:{}", supportingRackDto);
        String refSiteId = PhysicalNodeIdNamingRule.getSiteId(phyNodeId);
        List<String> inRackNeIds = supportingRackDto.getSupportingNe().stream()
                .map(SupportingNeDto::getNodeRef).collect(
                        Collectors.toList());
        AlarmSeverity alarmSeverity = calculateCurrentAlarmSeverity(inRackNeIds);

        return RackAlarmState.builder().rackId(supportingRackDto.getRackId())
                .alarmSeverity(alarmSeverity).siteNodeId(refSiteId).neIds(inRackNeIds).build();
    }


    public List<RackAlarmState> calculateSiteRefRackCurrentState(String neId) {
        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(neId);
        return calculateSiteRefRackCurrentStateBySiteId(siteNodeId);
    }

    private RackAlarmState calculateRackCurrentAlarmState(String siteNodeId,
            SupportingRack supportingRack) {
        log.debug("calculate rack current alarm state,rack id:{}", supportingRack.getRackId());
        List<String> supportPhyNodeIds = Optional.ofNullable(supportingRack.getSupportingNe())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(ne -> ne.getNodeRef().getValue())
                .collect(Collectors.toList());
        String rackId = supportingRack.getRackId().getValue();
        if (CollectionUtils.isEmpty(supportPhyNodeIds)) {
            return RackAlarmState.builder().rackId(rackId).alarmSeverity(AlarmSeverity.Unknown)
                    .siteNodeId(siteNodeId).neIds(supportPhyNodeIds).build();
        }
        AlarmSeverity alarmSeverity = calculateCurrentAlarmSeverity(supportPhyNodeIds);
        return RackAlarmState.builder().rackId(rackId).alarmSeverity(alarmSeverity)
                .siteNodeId(siteNodeId).neIds(supportPhyNodeIds).build();
    }

    public List<RackAlarmState> calculateSiteRefRackCurrentStateBySiteId(String siteId) {
        return calculateSiteRefRackCurrentAlarmBySiteId(siteId);
    }


    private List<RackAlarmState> calculateSiteRefRackCurrentAlarmBySiteId(String siteNodeId) {
        log.debug("start to calculate site ref rack current Alarm State, site id:{}", siteNodeId);
        Node node = nodeCacheManager.getSiteNode(siteNodeId);
        List<SupportingRack> supportingRacks = node.getAugmentation(Node1.class).getSite()
                .getSupportingRack();
        List<RackAlarmState> rackAlarmStates = supportingRacks.stream()
                .map(supportingRack -> calculateRackCurrentAlarmState(siteNodeId, supportingRack))
                .collect(
                        Collectors.toList());
        return rackAlarmStates;
    }
}
