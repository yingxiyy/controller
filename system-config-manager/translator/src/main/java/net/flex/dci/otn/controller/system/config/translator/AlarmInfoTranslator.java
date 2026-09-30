package net.flex.dci.otn.controller.system.config.translator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.cache.service.PhyNodeNameCache;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import net.flex.dci.otn.controller.system.config.translator.text.AlarmTextTranslator;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/15 11:17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmInfoTranslator {

    private final PhyNodeNameCache phyNodeNameCache;

    private final AlarmTextTranslator alarmTextTranslator;

    /**
     * translate toop alarm
     *
     * @param alarm
     * @return
     */
    public ToopAlarm translateAlarm(ToopAlarm alarm) {
        log.info("start to translate alarm");
        ToopAlarm.Builder translateAlarmBuilder = new ToopAlarm.Builder(alarm);
        translateAlarmBuilder.toopKey(translateKey(alarm.getToopKey()));
        translateAlarmBuilder.text(translateText(alarm));
        return translateAlarmBuilder.build();
    }

    private String translateText(ToopAlarm alarm) {
        return alarmTextTranslator.translateText(alarm);
    }

    private String translateKey(String toopKey) {
        String friendlyName = phyNodeNameCache.getFriendlyName(toopKey);
        if (friendlyName == null) {
            return toopKey;
        }
        return friendlyName;
    }
}
