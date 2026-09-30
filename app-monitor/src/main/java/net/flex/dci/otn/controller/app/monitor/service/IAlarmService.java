package net.flex.dci.otn.controller.app.monitor.service;

import java.util.List;
import net.flex.dci.otc.zk.common.entity.DiskUsage;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;

/**
 * @version 1.0
 * @date 2022/6/28 16:52
 */
public interface IAlarmService {

    void generateAlarm(AlarmRecord alarmRecord);

    void clearAlarm(String alarmId);

    void clearAlarm(List<String> alarmIds);

    List<AlarmRecord> getAllAppAlarm();

    AlarmRecord buildAlarmWhenIsolate(String module, String detailId);

    String generateAlarmIdWhenIsolate(String module, String detailId);

    String generateAlarmIdWhenNtp(String module, String detailId);

    String generateAlarmIdWhenNtp(String serverId);


    AlarmRecord buildAlarmWhenNtp(String module, String detailId);

    AlarmRecord generateDiskUsageAlarm(String hostname, String mountPoint, DiskUsage diskInfo);

    void clearDiskUsageAlarm(String hostname, String mountPoint, DiskUsage diskInfo);

    void clearAppAlarm(InstanceDetails instanceDetail);

    AlarmRecord getCurrentAlarmById(String alarmId);

    AlarmRecord buildServerNtpAlarm(String serverId);

    List<AlarmRecord> getAlarmByAlarmGroup(String alarmGroup);
}
