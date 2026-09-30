package net.flex.dci.otn.controller.subnet.manager;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
@EnableDciClient
@SpringBootApplication
public class SubNetManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubNetManagerApplication.class, args);
    }

}
