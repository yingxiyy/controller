package net.flex.dci.otn.controller.idc.manager;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @version 1.0
 * @date 2022/1/14 15:24
 */
@EnableDciClient
@SpringBootApplication
public class IdcManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdcManagerApplication.class, args);
    }

}
