package net.flex.dci.otn.controller.idc.manager.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/1/14 16:18
 */
@Configuration
@Data
public class I18Properties {

    @Value("${i18n:zh_cn}")
    private String language;
}
