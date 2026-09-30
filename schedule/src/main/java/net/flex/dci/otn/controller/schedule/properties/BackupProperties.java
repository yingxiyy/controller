package net.flex.dci.otn.controller.schedule.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 2026/5/31
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "backup")
public class BackupProperties {

    private String localBackupDir;

    private String remoteBackupDir;

    private String sftpServer;

    private Integer ttl = 7;

    private final Tools tools = new Tools();

    @Getter
    @Setter
    public static class Tools {

        private MongoTools mongo = new MongoTools();
        private MysqlTools mysql = new MysqlTools();

        @Getter
        @Setter
        public static class MongoTools {

            private String dump;
            private String restore;
        }

        @Getter
        @Setter
        public static class MysqlTools {

            private String dump;
            private String restore;
        }
    }

}
