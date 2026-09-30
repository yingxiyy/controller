package net.flex.dci.otc.controller.status.alarm.service.handler;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.db.jpa.entity.AlarmHistoryRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmHistoryDaoService;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/10 14:10
 */
@Slf4j
@Component
public class AlarmHistoryHandler implements IAlarmHandler {

    @Autowired
    private AlarmHistoryDaoService alarmHistoryDaoService;

    @Override
    public Page<AlarmHistoryRecord> listHistoryAlarmsByConditionPaged(int offset, int limit,
            AlarmConditionDto alarmConditionDto) {
        log.debug("start to list history alarm by condition {}", alarmConditionDto);
        Page<AlarmHistoryRecord> pageAlarmHistory = alarmHistoryDaoService.listAlarmHistoryPagedByCondition(
                offset, limit, alarmConditionDto);
        return pageAlarmHistory;
    }

    @Override
    public Object findAlarm(Long alarmIndex) {
        log.debug("get alarm history detail,index is {} ", alarmIndex);
        AlarmHistoryRecord alarmRecord = alarmHistoryDaoService.findByIndex(alarmIndex);
        return alarmRecord;
    }

    @Override
    public Page<AlarmHistoryRecord> listAllAlarmsByConditionPaged(
            int offset, int limit, AlarmConditionDto alarmConditionDto) {
        log.debug("list all alarm by condition {}", alarmConditionDto);

        Page<AlarmHistoryRecord> alarmPaged = alarmHistoryDaoService.listAllAlarmPagedByCondition(
                offset, limit, alarmConditionDto);
        return alarmPaged;
    }
}
