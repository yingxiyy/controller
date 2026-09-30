package net.flex.dci.otn.controller.nms;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import net.flex.dci.otn.topology.cache.EnableDciCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @version 1.0
 * @date 2022/4/12 20:40
 */
@EnableDciCache
@EnableDciClient
@SpringBootApplication
public class NMSApplication {

    public static void main(String[] args) {
        SpringApplication.run(NMSApplication.class, args);
    }

}
