package net.flex.dci.otn.controller.resource.statistic;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import net.flex.dci.otn.topology.cache.EnableDciCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @version 1.0
 * @date 10/24/2025 3:32 PM
 */
@SpringBootApplication
@EnableDciClient
@EnableDciCache
@EnableScheduling
public class ResourceStatisticApp {

    public static void main(String[] args) {
        SpringApplication.run(ResourceStatisticApp.class, args);
    }
}
