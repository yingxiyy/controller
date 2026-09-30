package net.flex.dci.otn.controller.schedule.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Data
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
@PropertySource(value = {
        "classpath:mysql.properties",
        "file:${user.dir}/config/mysql.properties"}, ignoreResourceNotFound = true)
public class MysqlProperties {

    private String url;

    private String username;

    private String password;

}
