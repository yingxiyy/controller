package net.flex.dci.otn.controller.system.config.configuration;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.i18n.configuration.DciI18nProperties;
import net.flex.dci.otn.controller.system.config.common.model.I18nConfig;
import net.flex.dci.otn.controller.system.config.common.utils.i18n.I18nAlarmUtils;
import net.flex.dci.otn.controller.system.config.common.utils.i18n.I18nMailUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 10/12/2023 2:21 PM
 */

@Configuration
@Slf4j
public class SystemConfiguration {

    @Bean
    public I18nConfig initI18nConfig(DciI18nProperties dciI18nProperties) {
        log.debug("load the i18n configuration");
        I18nConfig i18nConfig = new I18nConfig();
        i18nConfig.setLang(dciI18nProperties.getLang());
        I18nAlarmUtils.setLangTag(dciI18nProperties.getLang());
        I18nMailUtils.setLangTag(dciI18nProperties.getLang());
        return i18nConfig;
    }
}
