package net.flex.dci.otc.controller.status.core.changer.alarm.detail.link;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.dto.alarm.TunnelsAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.TunnelsAlarmState.TunnelAlarmState;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/22/2023 2:29 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelAlarmStateChanger extends AbstractLinkAlarmStateChanger<TunnelsAlarmState> {

    private final TunnelDao tunnelDao;

    @Override
    public void changeState(TunnelsAlarmState state) {
        log.debug("start to change alarm state for the tunnels");
        List<TunnelAlarmState> tunnelAlarmStates = state.getTunnelAlarmStates();
        if (tunnelAlarmStates == null || tunnelAlarmStates.isEmpty()) {
            log.warn("the change alarm state tunnel is meaningless,do nothing");
            return;
        }
        List<net.flex.dci.otc.mongo.dto.batch.TunnelAlarmState> updateTunnelAlarmState = tunnelAlarmStates.stream()
                .map(
                        tunnelAlarmState -> net.flex.dci.otc.mongo.dto.batch.TunnelAlarmState.builder()
                                .tunnelId(tunnelAlarmState.getTunnelId())
                                .alarmSeverity(tunnelAlarmState.getAlarmSeverity())
                                .build()
                ).collect(
                        Collectors.toList());
        tunnelDao.bulkUpdateTunnelAlarmState(updateTunnelAlarmState);
    }

    private void updateAlarmState(TunnelAlarmState tunnelAlarmState) {
        String tunnelId = tunnelAlarmState.getTunnelId();
        AlarmSeverity alarmSeverity = tunnelAlarmState.getAlarmSeverity();
        log.debug("update the tunnel tunnel id is:{},alarm severity is:{}", tunnelId,
                alarmSeverity);
        tunnelDao.updateTunnelAlarmState(tunnelId, alarmSeverity);
    }
}
