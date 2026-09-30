package net.flex.dci.otn.controller.auth;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @version 1.0
 * @date 2022/4/15 13:13
 */
@EnableDciClient
@SpringBootApplication
public class AuthServer {

    public static void main(String[] args) {
        SpringApplication.run(AuthServer.class, args);
    }
}
