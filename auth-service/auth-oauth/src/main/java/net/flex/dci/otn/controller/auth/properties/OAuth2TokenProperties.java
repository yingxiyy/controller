package net.flex.dci.otn.controller.auth.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @version 1.0
 * @date 2022/4/24 23:21
 */
@Configuration
@PropertySource(value = {
        "classpath:oauth2-token.properties",
        "file:${user.dir}/config/oauth2-token.properties"}, ignoreResourceNotFound = true)
@ConfigurationProperties(prefix = "oauth2")
@Slf4j
@Data
public class OAuth2TokenProperties {

    @Value("${oauth2.access.token.time}")
    private long accessTokenTime;

    @Value("${oauth2.access.token.time.unit}")
    private String accessTokenTimeUnit;

    @Value("${oauth2.refresh.token.time}")
    private long refreshTokenTime;

    @Value("${oauth2.refresh.token.time.unit}")
    private String refreshTokenTimeUnit;
}
