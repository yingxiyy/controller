package net.flex.dci.otc.controller.status.alarm.core.kafka.processor;

import java.util.List;
import net.flex.dci.otc.common.model.alarm.Alarm;

/**
 * @version 1.0
 * @date 2022/4/1 16:40
 */
public interface AlarmNotificationProcessor {

    void process(List<Alarm> alarms);
}
