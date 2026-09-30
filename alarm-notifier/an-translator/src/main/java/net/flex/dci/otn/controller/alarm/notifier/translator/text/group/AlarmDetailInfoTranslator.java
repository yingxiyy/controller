package net.flex.dci.otn.controller.alarm.notifier.translator.text.group;

import net.flex.dci.otn.controller.alarm.notifier.common.model.ToopAlarm;

/**
 * @version 1.0
 * @date 2022/3/15 12:43
 */
public interface AlarmDetailInfoTranslator {

    String translateAlarmText(ToopAlarm alarm);
}
