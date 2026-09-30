package net.flex.dci.otc.controller.status.alarm.notification;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.model.alarm.CtrlAlarm;
import net.flex.dci.otc.controller.status.alarm.dto.WrappedAlarm;
import net.flex.dci.otc.controller.status.alarm.dto.WrappedCtrlAlarm;
import net.flex.dci.otc.controller.status.alarm.utils.AlarmConverterUtils;
import net.flex.dci.otn.controller.tools.kafka.service.AlarmMessageSender;
import org.springframework.stereotype.Component;

/**
 * 2025/12/27
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmNotifier {


    /**
     * alarm notifier
     *
     * @param ctrlAlarm
     */
    public void sendMessage(CtrlAlarm ctrlAlarm) {
        log.info("start to handle the CtrlAlarm :{} eventTime:{}", ctrlAlarm,
                ctrlAlarm.getEventTime());
        List<Alarm> clearedAlarms = ctrlAlarm.getClearAlarms();
        List<Alarm> raisedAlarms = ctrlAlarm.getNewAlarms();
        Long eventTime = ctrlAlarm.getEventTime();

        List<WrappedAlarm> clearedWrappedAlarms = wrappedAlarm(clearedAlarms);
        List<WrappedAlarm> raisedWrappedAlarms = wrappedAlarm(raisedAlarms);
        WrappedCtrlAlarm wrappedCtrlAlarm = buildWrappedCtrlAlarm(clearedWrappedAlarms,
                raisedWrappedAlarms, eventTime);

        String key = extractKeyFromWrappedCtrlAlarm(wrappedCtrlAlarm);
        AlarmMessageSender.sendMessage(wrappedCtrlAlarm, key);
    }

    private List<WrappedAlarm> wrappedAlarm(List<Alarm> alarms) {
        log.debug("wrapped alarm:{}", alarms);
        List<WrappedAlarm> wrappedAlarms = alarms.stream()
                .map(alarm -> WrappedAlarm.builder()
                        .index(alarm.getIndex())
                        .ip(alarm.getIp())
                        .id(alarm.getId())
                        .alarmTypeId(alarm.getAlarmTypeId())
                        .group(alarm.getGroup())
                        .component(alarm.getComponent())
                        .text(alarm.getText())
                        .isClear(alarm.getIsClear())
                        .resource(alarm.getResource())
                        .toopIp(alarm.getToopIp())
                        .toopKey(alarm.getToopKey())
                        .serviceAffect(alarm.getServiceAffect())
                        .severity(alarm.getSeverity())
                        .typeId(alarm.getTypeId())
                        .timeCreated(alarm.getTimeCreated())
                        .toopKeyName(AlarmConverterUtils.getAlarmNmlKeyName(alarm.getToopKey()))
                        .neId(AlarmConverterUtils.getAlarmNeId(alarm.getToopKey()))
                        .build())
                .collect(Collectors.toList());
        return wrappedAlarms;
    }

    private WrappedCtrlAlarm buildWrappedCtrlAlarm(List<WrappedAlarm> clearedWrappedAlarms,
            List<WrappedAlarm> raisedWrappedAlarms, Long eventTime) {
        WrappedCtrlAlarm wrappedCtrlAlarm = new WrappedCtrlAlarm();
        wrappedCtrlAlarm.setClearAlarms(clearedWrappedAlarms);
        wrappedCtrlAlarm.setNewAlarms(raisedWrappedAlarms);
        wrappedCtrlAlarm.setEventTime(eventTime);
        return wrappedCtrlAlarm;
    }


    private static String extractKeyFromWrappedCtrlAlarm(WrappedCtrlAlarm ctrlAlarm) {
        if (ctrlAlarm.getNewAlarms() != null && !ctrlAlarm.getNewAlarms().isEmpty()) {
            return ctrlAlarm.getNewAlarms().get(0).getNeId();
        }
        if (ctrlAlarm.getClearAlarms() != null && !ctrlAlarm.getClearAlarms().isEmpty()) {
            return ctrlAlarm.getClearAlarms().get(0).getNeId();
        }
        return "UNKNOWN";
    }
}
