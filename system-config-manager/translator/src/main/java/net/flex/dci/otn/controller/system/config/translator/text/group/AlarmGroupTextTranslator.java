package net.flex.dci.otn.controller.system.config.translator.text.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.i18n.core.DciI18nMessage;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/15 12:41
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmGroupTextTranslator implements AlarmDetailInfoTranslator {

    private final DciI18nMessage dciI18nMessage;

    @Override
    public String translateAlarmText(ToopAlarm alarm) {
        log.info("start to translate alarm text :{},alarm group is :{}", alarm.getText(),
                alarm.getGroup());
        String alarmGroup = alarm.getGroup();
        String message = dciI18nMessage.getAlarmText(alarmGroup, alarm.getText());
        return message;
    }


}
