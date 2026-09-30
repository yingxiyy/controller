package net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.controller.status.dto.alarm.TunnelsAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.TunnelsAlarmState.TunnelAlarmState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/2 17:14
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelAlarmStateCalculator extends
        AbstractLinkAlarmStateCalculator<TunnelsAlarmState, List<LinkStateDto>> {

    private final AlarmDaoService alarmDaoService;

    @Override
    public TunnelsAlarmState calculate(List<LinkStateDto> tunnels) {
        log.debug("start to calculate the tunnel alarm state ,tunnel id is :{}", tunnels);
        Set<String> allOchLinkIds = new HashSet<>();
        for (LinkStateDto tunnel : tunnels) {
            allOchLinkIds.addAll(tunnel.getSupportingLink());
        }

        Map<String, LinkStateDto> ochLinkMap = connectionCacheManager.batchGetOchLinksByIds(
                new ArrayList<>(allOchLinkIds));

        Set<String> allNmlKeys = new HashSet<>();
        for (LinkStateDto tunnel : tunnels) {
            allNmlKeys.addAll(getTunnelRefTpRefNmlKeys(tunnel));
        }
        Map<String, List<AlarmSeverity>> nmlKeyAlarmMap = alarmDaoService.getAlarmSeverityByNmlKeysWithMap(
                new ArrayList<>(allNmlKeys));
        List<TunnelAlarmState> tunnelAlarmStates = tunnels.stream()
                .map(tunnel -> calculateTunnelAlarm(tunnel, ochLinkMap, nmlKeyAlarmMap))
                .collect(Collectors.toList());

        return TunnelsAlarmState.builder().tunnelAlarmStates(tunnelAlarmStates).build();
    }


    @Override
    public TunnelsAlarmState calculate(String id) {
        return null;
    }


    private TunnelAlarmState calculateTunnelAlarm(LinkStateDto tunnel,
            Map<String, LinkStateDto> ochLinkMap,
            Map<String, List<AlarmSeverity>> nmlKeyAlarmMap) {

        String tunnelId = tunnel.getId();
        log.debug("calculate tunnel alarm ,tunnel is :{}", tunnelId);
        Set<String> ochLinkIds = new HashSet<>(tunnel.getSupportingLink());
        List<AlarmSeverity> alarmSeverities = ochLinkIds.stream()
                .map(ochLinkMap::get)
                .filter(Objects::nonNull)
                .map(LinkStateDto::getAlarmSeverity)
                .collect(Collectors.toList());

        AlarmSeverity currentAlarmSeverity = StatusUtil.calculateAlarmStateByList(alarmSeverities);

        List<String> nmlKeys = getTunnelRefTpRefNmlKeys(tunnel);
        List<AlarmSeverity> clientAlarms = nmlKeys.stream()
                .flatMap(key -> nmlKeyAlarmMap.getOrDefault(key, Collections.emptyList()).stream())
                .collect(Collectors.toList());
        AlarmSeverity clientAlarmSeverity = StatusUtil.calculateAlarmStateByList(clientAlarms);

        AlarmSeverity calculateSeverity = StatusUtil.calculateAlarmSeverity(
                currentAlarmSeverity, clientAlarmSeverity);

        return TunnelAlarmState.builder().tunnelId(tunnelId).alarmSeverity(calculateSeverity)
                .build();
    }

    private List<String> getTunnelRefTpRefNmlKeys(LinkStateDto tunnel) {
        List<String> nmlKeys = new ArrayList<>();
        String tunnelId = tunnel.getId();
        String sourceTpId = TunnelIdNamingRule.getATp(tunnelId);
        String destTpId = TunnelIdNamingRule.getZtp(tunnelId);
        String sourceTpEquipId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
        String destTpEquipId = PhysicalTpIdNamingRule.getEquipId(destTpId);
        String sourceNodeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
        String destNodeId = PhysicalTpIdNamingRule.getNodeId(destTpId);
        nmlKeys.add(sourceTpId);
        nmlKeys.add(destTpId);
        nmlKeys.add(sourceTpEquipId);
        nmlKeys.add(destNodeId);
        nmlKeys.add(destTpEquipId);
        nmlKeys.add(sourceNodeId);
        return nmlKeys;
    }

}
