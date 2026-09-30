package net.flex.dci.otc.controller.ne.manager.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @version 1.0
 * @date 2022/8/15 16:28
 */
@Configuration
@ConfigurationProperties
@PropertySource(value = {"classpath:application.yaml", "classpath:application.properties",
        "file:${user.dir}/config/application.yml",
        "file:${user.dir}/config/application.properties"},
        ignoreResourceNotFound = true)
@Data
public class NeMonitorProperties {

    @Value("${monitor.interval}")
    private Long interval;

    @Value("${monitor.unit}")
    private String unit;


    @Value("${monitor.slice-count:5}")
    private int sliceCount = 5;

    @Value("${monitor.max-slice-count:20}")
    private int maxSliceCount = 20;

    @Value("${monitor.stagger-seconds:100}")
    private int staggerSeconds = 10;
}
