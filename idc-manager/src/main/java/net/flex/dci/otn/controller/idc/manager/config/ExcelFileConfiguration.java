package net.flex.dci.otn.controller.idc.manager.config;

import javax.validation.Validator;
import net.flex.dci.otn.controller.idc.manager.excel.ExcelReader;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * @version 1.0
 * @date 2022/1/20 10:47
 */
@Configuration
public class ExcelFileConfiguration {

    @Bean
    public MessageSource idcImportMessageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:i18n/en_us");
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }

    @Bean
    public ExcelReader excelReader(Validator validator, MessageSource idcImportMessageSource) {
        return new ExcelReader(validator, idcImportMessageSource);
    }
}
