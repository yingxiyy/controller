package net.flex.dci.otn.controller.schedule.utils;

import java.io.File;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.rpcs.FtpRpc;
import net.flex.dci.otc.mongo.dao.FtpServerDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
public class ConfigLoader {

    @Autowired(required = false)
    private FtpRpc ftpRpc;

    @Autowired(required = false)
    private FtpServerDao ftpServerDao;

    @Value("${controller.schedule.sftpServerName:}")
    private String sftpServerName;

    @Value("${controller.schedule.controllerDBBackupDir:/controllerDBBackup}")
    private String controllerDbBackupDir;

    @Value("${controller.schedule.sftpFileTTL:7}")
    private Integer sftpFileTTL;

    @Value("${controller.schedule.localBackupTTL:7}")
    private Integer localBackupTTL;

    @Value("${controller.tools.mongoDump:}")
    private String mongoDump;

    @Value("${controller.tools.mongoRestore:}")
    private String mongoRestore;

    @Value("${controller.tools.mysqlDump:}")
    private String mysqlDump;

    @Value("${controller.tools.mysqlClient:}")
    private String mysqlClient;

    private boolean mongoDumpAvailable = false;
    private boolean mongoRestoreAvailable = false;
    private boolean mysqlDumpAvailable = false;
    private boolean mysqlClientAvailable = false;
    private boolean ftpAvailable = false;

    @PostConstruct
    public void validateConfig() {
        mongoDumpAvailable = validateExecutable("mongoDump", mongoDump);
        mongoRestoreAvailable = validateExecutable("mongoRestore", mongoRestore);
        mysqlDumpAvailable = validateExecutable("mysqlDump", mysqlDump);
        mysqlClientAvailable = validateExecutable("mysqlClient", mysqlClient);

        ftpAvailable = (ftpRpc != null && ftpServerDao != null && sftpServerName != null && !sftpServerName.isEmpty());
        if (!ftpAvailable) {
            log.warn("FTP services not available. File upload/download will be disabled.");
        }

        if (!isAnyToolAvailable()) {
            log.warn("No database CLI tools found. Backup/restore functionality will be disabled.");
        }
    }

    private boolean validateExecutable(String name, String path) {
        if (path == null || path.trim().isEmpty()) {
            log.debug("Tool not configured: {}", name);
            return false;
        }

        File file = new File(path);

        if (!file.exists()) {
            log.warn("Tool not found: {} (path: {})", name, path);
            return false;
        }

        if (!file.canExecute()) {
            log.warn("Tool not executable: {} (path: {})", name, path);
            return false;
        }

        log.info("Tool available: {} -> {}", name, path);
        return true;
    }

    public boolean isAnyToolAvailable() {
        return mongoDumpAvailable || mongoRestoreAvailable || mysqlDumpAvailable || mysqlClientAvailable;
    }

    public boolean isMongoBackupAvailable() {
        return mongoDumpAvailable;
    }

    public boolean isMongoRestoreAvailable() {
        return mongoRestoreAvailable;
    }

    public boolean isMysqlBackupAvailable() {
        return mysqlDumpAvailable;
    }

    public boolean isMysqlRestoreAvailable() {
        return mysqlClientAvailable;
    }

    public boolean isFtpAvailable() {
        return ftpAvailable;
    }
}
