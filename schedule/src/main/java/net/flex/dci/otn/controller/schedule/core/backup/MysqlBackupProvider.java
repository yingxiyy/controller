package net.flex.dci.otn.controller.schedule.core.backup;

import static net.flex.dci.otn.controller.schedule.utils.Constants.BACKUP_MYSQL_DIR;
import static net.flex.dci.otn.controller.schedule.utils.Constants.BACKUP_MYSQL_FILE;
import static net.flex.dci.otn.controller.schedule.utils.Constants.BACKUP_MYSQL_TAR;
import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.compressToTarGz;
import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.deleteDirectory;
import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.readStream;
import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.setSecurePermissions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.schedule.component.FtpManager;
import net.flex.dci.otn.controller.schedule.dto.MysqlConnectionInfo;
import net.flex.dci.otn.controller.schedule.properties.BackupProperties;
import net.flex.dci.otn.controller.schedule.properties.MysqlProperties;
import net.flex.dci.otn.controller.schedule.utils.ScheduleUtils;
import org.springframework.stereotype.Component;

/**
 * 2026/5/31
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class MysqlBackupProvider implements BackupProvider {

    private final BackupProperties backupProperties;

    private final MysqlProperties mysqlProperties;

    private final FtpManager ftpManager;

    public MysqlBackupProvider(BackupProperties backupProperties, MysqlProperties mysqlProperties,
            FtpManager ftpManager) {
        this.backupProperties = backupProperties;
        this.mysqlProperties = mysqlProperties;
        this.ftpManager = ftpManager;
    }

    @Override
    public void backup(String path) {
        log.info("start to backup mongodb datafile,local backup path:{}", path);
        Path backupPath = Paths.get(path);
        Path mysqlBackupPath = backupPath.resolve(BACKUP_MYSQL_DIR);
        Path mysqlTarFile = backupPath.resolve(BACKUP_MYSQL_TAR);
        try {
            Files.createDirectories(mysqlBackupPath);
            backupMysql(mysqlBackupPath.toAbsolutePath());
            compressToTarGz(mysqlBackupPath, mysqlTarFile);
            log.info("MongoDB backup compressed to: {}", mysqlTarFile);

            deleteDirectory(mysqlBackupPath);
            log.debug("Temporary directory deleted: {}", mysqlBackupPath);
            //upload to the ftp server
        } catch (Exception ex) {
            log.error("failed to backup the mysql ", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to backup the mysql data", ex);
        }
    }

    /**
     * backupMysql
     *
     * @param absolutePath
     */
    private void backupMysql(Path absolutePath) throws IOException, InterruptedException {
        MysqlConnectionInfo mysqlConnectionInfo = MysqlConnectionInfo.parseUrl(
                mysqlProperties.getUrl());
        Path configFile = createMysqlConfigFile(mysqlConnectionInfo);
        String outputFile = absolutePath.resolve(BACKUP_MYSQL_FILE).toFile()
                .getAbsolutePath();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    backupProperties.getTools().getMysql().getDump(),
                    "--defaults-extra-file=" + configFile.toAbsolutePath(),
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    "--quick",
                    "--result-file=" + outputFile,
                    mysqlConnectionInfo.getDatabase()
            );
            pb.redirectErrorStream(true);
            log.debug("Executing: mysqldump --defaults-extra-file=*** --all-databases ...");
            Process process = pb.start();
            String output = readStream(process.getInputStream());
//            int exitCode = process.waitFor();
//            if (exitCode != 0) {
//                String error = readStream(process.getInputStream());
//                throw new IOException("mysqldump failed with exit code " + exitCode + ": " + error);
//            }
//            log.info("mysqldump completed, output file: {}", outputFile);
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("MySQL backup timed out after 30 minutes");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException(
                        "mysqldump failed with exit code " + exitCode + ": " + output);
            }

            log.info("mysqldump completed successfully, output file: {}", outputFile);
        } finally {

            Files.deleteIfExists(configFile);
        }
    }

    private Path createMysqlConfigFile(MysqlConnectionInfo mysqlConnectionInfo) throws IOException {
        Path configFile = Files.createTempFile("mysql_", ".cnf");

        String content = String.format(
                "[client]\nuser=%s\npassword=%s\nhost=%s\nport=%d\n",
                mysqlProperties.getUsername(),
                mysqlProperties.getPassword(),
                mysqlConnectionInfo.getHost(),
                mysqlConnectionInfo.getPort()
        );
        Files.write(configFile, content.getBytes(StandardCharsets.UTF_8));
        setSecurePermissions(configFile);
        return configFile;
    }

    @Override
    public void restore(String rootPath) throws IOException {
        log.info("Starting MySQL restore from: {}", rootPath);
        Path tarFile = Paths.get(rootPath, "mysql.tar.gz");
        if (!Files.exists(tarFile)) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "Backup file not found: " + tarFile);
        }

        Path tempDir = null;
        try {

            tempDir = Files.createTempDirectory("mysql_restore_");

//            decompressTarGz(tarFile, tempDir);

            Path sqlFile = tempDir.resolve(BACKUP_MYSQL_FILE);
            if (!Files.exists(sqlFile)) {
                Path mysqlDir = tempDir.resolve("mysql");
                if (Files.exists(mysqlDir)) {
                    sqlFile = mysqlDir.resolve(BACKUP_MYSQL_FILE);
                }
            }
            if (!Files.exists(sqlFile)) {
                throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                        "No backup.sql found in archive");
            }

//            executeMysqlRestore(sqlFile.toString());
            log.info("MySQL restore completed successfully");
        } catch (Exception e) {
            log.error("MySQL restore failed", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "MySQL restore failed", e);
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return ScheduleUtils.isExecutable(backupProperties.getTools().getMysql().getDump())
                && ScheduleUtils.isExecutable(backupProperties.getTools().getMysql().getRestore());
    }
}
