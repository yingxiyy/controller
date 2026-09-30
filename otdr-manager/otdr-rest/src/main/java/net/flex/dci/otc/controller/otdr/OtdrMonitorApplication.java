package net.flex.dci.otc.controller.otdr;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @version 1.0
 * @date 2022/7/18 14:49
 */
@SpringBootApplication
@EnableDciClient
public class OtdrMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtdrMonitorApplication.class, args);
    }

}
