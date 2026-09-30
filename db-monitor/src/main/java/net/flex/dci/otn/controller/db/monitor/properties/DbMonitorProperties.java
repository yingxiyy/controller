package net.flex.dci.otn.controller.db.monitor.properties;

import java.io.Serializable;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 2026/9/19
 *
 * @author musa
 * @version 1.0
 **/
@Data
@ConfigurationProperties(prefix = "db-monitor")
public class DbMonitorProperties implements Serializable {

    private Ha ha = new Ha();

    private Processor processor = new Processor();

    @Data
    public static class Ha implements Serializable {

        private boolean enabled = false;
    }

    @Data
    public static class Processor implements Serializable {

        private boolean enabled = false;

        private int partitions = 12;
    }
}
