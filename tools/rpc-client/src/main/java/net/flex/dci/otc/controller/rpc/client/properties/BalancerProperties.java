package net.flex.dci.otc.controller.rpc.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/4/14 16:00
 */
@Configuration
@ConfigurationProperties(prefix = "balancer")
@Data
public class BalancerProperties {

    private String strategy;
}
