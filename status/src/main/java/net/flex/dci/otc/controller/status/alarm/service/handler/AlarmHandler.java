package net.flex.dci.otc.controller.status.alarm.service.handler;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.db.jpa.entity.AlarmHistoryRecord;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmSourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/10 14:09
 */
@Slf4j
@Component
public class AlarmHandler implements IAlarmHandler {

    @Autowired
    private AlarmCurrentHandler alarmCurrentHandler;

    @Autowired
    private AlarmHistoryHandler alarmHistoryHandler;


    /**
     * list all alarm by conditions
     *
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public Page<AlarmRecord> listAlarmsByConditionPaged(int offset,
            int limit,
            AlarmConditionDto alarmConditionDto) {
        log.debug("start to list alarms by condition {},alarm type is current", alarmConditionDto);
        Page<AlarmRecord> pageResult = null;
        int pageNum = (int) Math.ceil((double) offset / (double) limit);
        pageResult = alarmCurrentHandler.listAlarmsByConditionPaged(pageNum, limit,
                alarmConditionDto);
        return pageResult;
    }


    @Override
    public Page<AlarmHistoryRecord> listHistoryAlarmsByConditionPaged(
            int offset, int limit, AlarmConditionDto alarmConditionDto) {
        log.debug("start to list alarms by condition {},alarm type is history", alarmConditionDto);
        Page<AlarmHistoryRecord> pageResult = alarmHistoryHandler.listHistoryAlarmsByConditionPaged(
                offset, limit,
                alarmConditionDto);
        return pageResult;
    }

    @Override
    public Page<AlarmHistoryRecord> listAllAlarmsByConditionPaged(
            int offset, int limit, AlarmConditionDto alarmConditionDto) {
        log.debug("start to list alarms by condition {},alarm type is history", alarmConditionDto);
        int pageNum = (int) Math.ceil((double) offset / (double) limit);
        Page<AlarmHistoryRecord> pageResult = alarmHistoryHandler.listAllAlarmsByConditionPaged(
                pageNum, limit,
                alarmConditionDto);
        return pageResult;
    }


    @Override
    public Object findAlarm(Long alarmIndex, AlarmSourceType alarmSourceType) {
        log.debug("get alarm detail by index :{}, type is {}", alarmIndex, alarmSourceType);
        Object alarmDetail = null;
        if (alarmSourceType.equals(AlarmSourceType.Current)) {
            alarmDetail = alarmCurrentHandler.findAlarm(alarmIndex);
        } else {
            alarmDetail = alarmHistoryHandler.findAlarm(alarmIndex);
        }
        return alarmDetail;
    }
}
