package net.flex.dci.otn.controller.cli;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import net.flex.dci.otn.topology.cache.EnableDciCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * @version 1.0
 * @date 9/16/2025 4:38 PM
 */
@EnableDciCache
@SpringBootApplication
@EnableDciClient
public class CliServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CliServiceApplication.class, args);
    }
}
