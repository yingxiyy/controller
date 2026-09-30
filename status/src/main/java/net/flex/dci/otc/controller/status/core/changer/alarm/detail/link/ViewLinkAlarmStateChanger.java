package net.flex.dci.otc.controller.status.core.changer.alarm.detail.link;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.alarm.ViewLinkAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.ViewLinksAlarmState;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.dto.batch.ViewLinkAlarmStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/23/2023 3:46 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewLinkAlarmStateChanger implements IStateChanger<ViewLinksAlarmState> {

    private final ViewLinkDao viewLinkDao;

    @Override
    public void changeState(ViewLinksAlarmState state) {
        log.debug("start to change the view links Alarm state");
        List<ViewLinkAlarmState> viewLinkAlarmStates = state.getViewLinkAlarmStates();
        if (viewLinkAlarmStates.isEmpty()) {
            log.warn("there have nothing to update for the view link,discard is");
            return;
        }
//        viewLinkAlarmStates.forEach(this::updateViewLinkAlarmState);
        List<ViewLinkAlarmStatus> updateViewLinkAlarmStatus = viewLinkAlarmStates.stream().map(
                        viewLinkAlarmState -> ViewLinkAlarmStatus.builder()
                                .alarmSeverity(viewLinkAlarmState.getAlarmSeverity())
                                .viewLinkId(viewLinkAlarmState.getViewLinkId()).build()
                )
                .collect(
                        Collectors.toList());
        viewLinkDao.bulkUpdateViewLinkAlarmState(updateViewLinkAlarmStatus);
    }


    private void updateViewLinkAlarmState(ViewLinkAlarmState viewLinkAlarmState) {
        String viewLinkId = viewLinkAlarmState.getViewLinkId();
        log.debug("update the view link alarm state");
        AlarmSeverity alarmSeverity = viewLinkAlarmState.getAlarmSeverity();
        viewLinkDao.updateViewLinkAlarmState(viewLinkId, alarmSeverity);
    }
}
