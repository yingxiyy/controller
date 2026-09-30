package net.flex.dci.otc.controller.status.alarm.service.handler;

import net.flex.dci.otn.db.jpa.entity.AlarmHistoryRecord;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmSourceType;
import org.springframework.data.domain.Page;

/**
 * @version 1.0
 * @date 2022/1/10 14:44
 */
public interface IAlarmHandler {


    default Page<AlarmRecord> listAlarmsByConditionPaged(int offset,
            int limit,
            AlarmConditionDto alarmConditionDto) {
        return null;
    }

    default Page<AlarmHistoryRecord> listHistoryAlarmsByConditionPaged(
            int offset, int limit, AlarmConditionDto alarmConditionDto) {
        return null;
    }

    default Page<AlarmHistoryRecord> listAllAlarmsByConditionPaged(
            int offset, int limit, AlarmConditionDto alarmConditionDto) {
        return null;
    }

    default Object findAlarm(Long alarmIndex, AlarmSourceType alarmSourceType) {
        return null;
    }

    default Object findAlarm(Long alarmIndex) {
        return null;
    }

    default void refreshDeviceAlarm() {

    }
}
