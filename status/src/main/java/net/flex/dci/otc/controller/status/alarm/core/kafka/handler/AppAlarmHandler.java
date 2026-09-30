package net.flex.dci.otc.controller.status.alarm.core.kafka.handler;

import net.flex.dci.otc.common.model.alarm.AppAlarm;

/**
 * @version 1.0
 * @date 2022/6/7 14:03
 */
public interface AppAlarmHandler {

    void handle(AppAlarm appAlarm);
}
