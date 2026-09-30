package net.flex.dci.otc.controller.status.alarm.core.kafka.handler.impl;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.model.alarm.AppAlarm;
import net.flex.dci.otc.common.model.alarm.CtrlAlarm;
import net.flex.dci.otc.controller.status.alarm.core.kafka.handler.AppAlarmHandler;
import net.flex.dci.otn.controller.tools.kafka.service.AlarmMessageSender;
import net.flex.dci.otn.db.jpa.entity.AlarmHistoryRecord;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import net.flex.dci.otn.db.jpa.service.dao.AlarmHistoryDaoService;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/6/7 14:03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppAlarmHandlerImpl implements AppAlarmHandler {


    private final AlarmDaoService alarmDaoService;

    private final AlarmHistoryDaoService alarmHistoryDaoService;

    @Override
    public void handle(AppAlarm appAlarm) {
        log.debug("start to handle app alarm");
        boolean isCleared = appAlarm.getIsClear();
        if (isCleared) {
            clearAppAlarm(appAlarm);
        } else {
            createAppAlarm(appAlarm);
        }
    }

    private void createAppAlarm(AppAlarm appAlarm) {
        log.info("load the app alarm :{}", appAlarm);
        AlarmRecord appAlarmRecord = alarmDaoService.getAlarm(appAlarm.getAlarmId());
        if (appAlarmRecord != null) {
            log.warn("current alarm is exited do nothing");
            return;
        }
        AlarmRecord alarm = new AlarmRecord();
        alarm.setAlarmId(appAlarm.getAlarmId());
        alarm.setAlarmText(appAlarm.getAlarmText());
        alarm.setSeverity(appAlarm.getSeverity());
        alarm.setAlarmGroup(appAlarm.getAlarmGroup());
        alarm.setAlarmTypeId(appAlarm.getAlarmTypeId());
        alarm.setComponentRef(
                appAlarm.getResourceRef() != null ? appAlarm.getResourceRef()
                        : "");
        alarm.setResourceRef(
                appAlarm.getResourceRef() != null ? appAlarm.getResourceRef()
                        : "");
        alarm.setCreationTime(System.currentTimeMillis());
        alarm.setNmlKey(appAlarm.getNmlKey());
        alarm.setNmlReceivedTime(System.currentTimeMillis());
        alarm.setSa(appAlarm.getSa());
        alarmDaoService.saveAlarmIfNotExist(alarm);
        //alarm notification process
        AlarmMessageSender.sendMessage(convert2CtrlAlarm(appAlarm), appAlarm.getAlarmId());

    }

    private void clearAppAlarm(AppAlarm appAlarm) {
        log.info("clear the app alarm :{}", appAlarm);
        String alarmId = appAlarm.getAlarmId();
//        String alarmTypeId = appAlarm.getAlarmTypeId();

//        if (alarmId.startsWith(DISK)) {
        AlarmRecord alarmRecord = alarmDaoService.getAlarm(alarmId);
        log.debug("record history alarm record");
        recordAppAlarm2HistoryAlarm(alarmRecord);
//        }
        alarmDaoService.deleteAlarmByAlarmId(alarmId);
//        String nmlKey = appAlarm.getNmlKey();

        //alarm notification process
        AlarmMessageSender.sendMessage(convert2CtrlAlarm(appAlarm), appAlarm.getAlarmId());
    }

    private void recordAppAlarm2HistoryAlarm(AlarmRecord alarmRecord) {
        if (alarmRecord == null) {
            return;
        }
        log.debug("record the disk Usage 2 history alarm:{}", alarmRecord);
        Long currentTimestamp = System.currentTimeMillis();
        AlarmHistoryRecord alarmHistoryRecord = new AlarmHistoryRecord();
        alarmHistoryRecord.setAlarmGroup(alarmRecord.getAlarmGroup());
        alarmHistoryRecord.setAlarmText(alarmRecord.getAlarmText());
        alarmHistoryRecord.setAlarmTypeId(alarmRecord.getAlarmTypeId());
        alarmHistoryRecord.setAlarmId(alarmRecord.getAlarmId());
        alarmHistoryRecord.setComponentRef(alarmRecord.getComponentRef());
        alarmHistoryRecord.setClearedTime(currentTimestamp);
        alarmHistoryRecord.setCreationTime(alarmRecord.getCreationTime());
        alarmHistoryRecord.setNmlReceivedTime(currentTimestamp);
        alarmHistoryRecord.setSa(false);
        alarmHistoryRecord.setSeverity(alarmRecord.getSeverity());
        alarmHistoryRecord.setResourceRef(alarmRecord.getResourceRef());
        alarmHistoryRecord.setNmlKey(alarmRecord.getNmlKey());
        alarmHistoryRecord.setArchivedTime(currentTimestamp);
        alarmHistoryDaoService.save(alarmHistoryRecord);
    }

    private CtrlAlarm convert2CtrlAlarm(AppAlarm appAlarm) {

        Alarm alarm = new Alarm.Builder()
                .group(appAlarm.getAlarmGroup())
                .id(appAlarm.getAlarmId())
                .text(appAlarm.getAlarmText())
                .alarmTypeId(appAlarm.getAlarmTypeId())
                .timeCreated(
                        appAlarm.getCreationTime() != null ? appAlarm.getCreationTime()
                                : null)
                .toopKey(appAlarm.getNmlKey())
                .ip(appAlarm.getIp())
                .serviceAffect(appAlarm.getSa())
                .severity(appAlarm.getSeverity() != null ? appAlarm.getSeverity().name() : null)
                .isClear(appAlarm.getIsClear())
                .resource(appAlarm.getResourceRef())
                .component(appAlarm.getComponentRef())
                .build();
        List<Alarm> newAlarm = new ArrayList<>();
        List<Alarm> deleteAlarm = new ArrayList<>();
        if (appAlarm.getIsClear()) {
            deleteAlarm.add(alarm);
        } else {
            newAlarm.add(alarm);
        }
        CtrlAlarm ctrlAlarm = CtrlAlarm.builder().newAlarms(newAlarm).clearAlarms(deleteAlarm)
                .eventTime(System.currentTimeMillis()).build();
        return ctrlAlarm;
    }
}
