package net.flex.dci.otn.controller.properties;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 2026/6/10
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "pm.upload")
public class PmUploadProperties {

    private String remoteDir = "/upload/historyPM";

    private String localProcessingRoot = "/tmp/pm-processing";

    private int maxConcurrentNe = 10;


    private Duration historyRange = Duration.ofDays(7);


    private Duration neProcessTimeout = Duration.ofMinutes(10);
}
