package net.flex.dci.otn.controller.gateway.authorization.configuration;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/4/18 12:59
 */
@Configuration
@ConfigurationProperties(prefix = "secure.ignore")
@Data
public class IgnoreUrlConfiguration {

    private List<String> urls;
}
