package net.flex.dci.otn.controller.gateway.dispatch.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/4/14 16:05
 */
@Configuration
@ConfigurationProperties(prefix = "balancer")
@Data
public class LoadBalancerProperties {

    private String strategy;
}
