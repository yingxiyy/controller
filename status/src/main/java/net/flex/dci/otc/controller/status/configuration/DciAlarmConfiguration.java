package net.flex.dci.otc.controller.status.configuration;

import net.flex.dci.otc.controller.status.alarm.utils.AlarmConverterUtils;
import net.flex.dci.otc.i18n.core.DciI18nMessage;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/10/18 10:08
 */
@Configuration
public class DciAlarmConfiguration {

    @Bean
    public AlarmConverterUtils initAlarmUtil(DciI18nMessage dciI18nMessage,
            DciTopologyCacheManager dciTopologyCacheManager) {
        AlarmConverterUtils.setDciI18Message(dciI18nMessage);
        AlarmConverterUtils.setDciTopologyCacheManager(dciTopologyCacheManager);
        return null;
    }

}
