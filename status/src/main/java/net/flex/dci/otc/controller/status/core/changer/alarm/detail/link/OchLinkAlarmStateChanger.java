package net.flex.dci.otc.controller.status.core.changer.alarm.detail.link;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.enums.LinkType;
import net.flex.dci.otc.controller.status.dto.alarm.OchLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.OchLinksAlarmState.OchLinkAlarmState;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/22/2023 10:25 AM
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class OchLinkAlarmStateChanger extends AbstractLinkAlarmStateChanger<OchLinksAlarmState> {

    private final OchLinkDao ochLinkDao;

    @Override
    public void changeState(OchLinksAlarmState state) {
        log.debug("start to handle the och link alarm state changer");
        List<OchLinkAlarmState> ochLinkAlarmStateList = state.getOchLinkAlarmStates();
        if (ochLinkAlarmStateList == null || ochLinkAlarmStateList.isEmpty()) {
            log.warn("the data is invalid ,discard it");
            return;
        }
        List<net.flex.dci.otc.mongo.dto.batch.OchLinkAlarmState> linkAlarmStates = ochLinkAlarmStateList.stream()
                .map(ochLinkAlarmState -> net.flex.dci.otc.mongo.dto.batch.OchLinkAlarmState.builder()
                        .ochLinkId(ochLinkAlarmState.getOchLinkId())
                        .alarmState(ochLinkAlarmState.getAlarmSeverity())
                        .build())
                .collect(Collectors.toList());
//        ochLinkAlarmStateList.forEach(this::changeOchAlarmState);
        ochLinkDao.bulkUpdateOchLinkAlarmStatus(linkAlarmStates);
        List<String> refOchLinkIds = ochLinkAlarmStateList.stream()
                .map(OchLinkAlarmState::getOchLinkId).collect(Collectors.toList());
        viewLinkAlarmStateEvaluate.evaluateAlarmState(refOchLinkIds, LinkType.OCH_LINK);
    }

//    private void changeOchAlarmState(OchLinkAlarmState ochLinkAlarmState) {
//        String ochLinkId = ochLinkAlarmState.getOchLinkId();
//        log.debug("start to update the och alarm state,och link id is:{}", ochLinkId);
//        AlarmSeverity alarmSeverity = ochLinkAlarmState.getAlarmSeverity();
//        ochLinkDao.updateOchLinkAlarmState(ochLinkId, alarmSeverity);
//    }
}
