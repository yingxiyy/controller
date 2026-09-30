package net.flex.dci.otn.controller.system.config.translator.text;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import net.flex.dci.otn.controller.system.config.translator.text.group.AlarmGroupTextTranslator;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/15 11:32
 */
@Slf4j
@Component
@AllArgsConstructor
public class AlarmTextTranslator {

    private final AlarmGroupTextTranslator alarmGroupTextTranslator;

    public String translateText(ToopAlarm alarm) {
        return alarmGroupTextTranslator.translateAlarmText(alarm);
    }
}
