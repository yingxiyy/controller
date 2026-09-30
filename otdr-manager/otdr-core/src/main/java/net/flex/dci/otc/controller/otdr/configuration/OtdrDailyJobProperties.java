package net.flex.dci.otc.controller.otdr.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @version 1.0
 * @date 2022/9/2 14:26
 */
@Configuration
@ConfigurationProperties
@PropertySource(value = {"classpath:application.yaml", "classpath:application.properties",
        "file:${user.dir}/config/application.yml",
        "file:${user.dir}/config/application.properties"},
        ignoreResourceNotFound = true)
@Data
public class OtdrDailyJobProperties {

    @Value("${daily.clear.time:03:00:00}")
    private String dailyClearTime;

}