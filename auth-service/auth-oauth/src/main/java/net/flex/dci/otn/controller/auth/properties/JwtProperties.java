package net.flex.dci.otn.controller.auth.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @version 1.0
 * @date 2022/4/15 15:48
 */
@Configuration

@PropertySource(value = {
        "classpath:jwt.yml",
        "file:${user.dir}/config/jwt.yml"}, ignoreResourceNotFound = true)
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    /**
     * Request Headers ： Authorization
     */
    @Value("${jwt.header:Authorization}")
    private String header;

    @Value("${jwt.token-start-with:Beaver }")
    private String tokenStartWith;

    @Value("${base64-secret:Zmpqc2Q9ODcyZHNkZjIzM2OkNFJFUkZhMzIzMjQzMlJkaWHzMnIyMzk4dTMyamZra3NXMjMxM1QyMzMmKiZqZUxKRiZeKjFHTExQO0RKODk4NDNqcmUoKGlvZXdqZktKRE8zMnhjY08yS0ZLTlNmZ2hqb3AtPTFSV0VIUkVXRVc=}")
    private String base64Secret;

    @Value("${jwt.token-validity-in-seconds:14400}")
    private Long tokenValidityInSeconds;

    @Value("${jwt.token-validity-in-seconds-for-remember-me:108000}")
    private Long tokenValidityInSecondsForRememberMe;

}
