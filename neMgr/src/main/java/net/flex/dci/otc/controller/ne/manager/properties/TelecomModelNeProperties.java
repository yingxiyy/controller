package net.flex.dci.otc.controller.ne.manager.properties;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @version 1.0
 * @date 11/21/2023 4:11 PM
 */
@Configuration
@PropertySource(value = {
        "classpath:telecom-model-ne.properties",
        "file:${user.dir}/config/telecom-model-ne.properties"}, ignoreResourceNotFound = true)
@ConfigurationProperties(prefix = "board")
@Data
public class TelecomModelNeProperties {

    private List<String> nonBusinessBoard;
}
