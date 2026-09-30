package net.flex.dci.otn.controller.schedule.core.backup;

import static net.flex.dci.otn.controller.schedule.utils.Constants.BACKUP_MONGODB_DIR;
import static net.flex.dci.otn.controller.schedule.utils.Constants.BACKUP_MONGODB_TAR;
import static net.flex.dci.otn.controller.schedule.utils.Constants.BACKUP_TIMEOUT_MINUTES;
import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.compressToTarGz;
import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.deleteDirectory;
import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.readStream;
import static net.flex.dci.otn.controller.schedule.utils.ScheduleUtils.setSecurePermissions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.configuration.MongoDBProperties;
import net.flex.dci.otn.controller.schedule.properties.BackupProperties;
import net.flex.dci.otn.controller.schedule.utils.ScheduleUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2026/5/31
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class MongoDbBackupProvider implements BackupProvider {

    private final BackupProperties backupProperties;

    private final MongoDBProperties mongoDBProperties;


    public MongoDbBackupProvider(BackupProperties backupProperties,
            MongoDBProperties mongoDBProperties) {
        this.backupProperties = backupProperties;
        this.mongoDBProperties = mongoDBProperties;

    }

    @Override
    public void backup(String path) throws IOException, InterruptedException {
        log.info("start to backup mongodb datafile,local backup path:{}", path);
        Path backupRootPath = Paths.get(path);
        Path mongoBackupDir = backupRootPath.resolve(BACKUP_MONGODB_DIR);
        Path tarFile = backupRootPath.resolve(BACKUP_MONGODB_TAR);
        try {
            Files.createDirectories(mongoBackupDir);

            backupMongoDB(mongoBackupDir.toAbsolutePath());
            compressToTarGz(mongoBackupDir, tarFile);
            log.info("MongoDB backup compressed to: {}", tarFile);

            deleteDirectory(mongoBackupDir);
            log.debug("Temporary directory deleted: {}", mongoBackupDir);
            //upload to ftp server
        } catch (Exception e) {
            log.error("Failed to backup the mongodb: {}", mongoBackupDir, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Cannot create backup the mongodb", e);
        }
    }


    /**
     * backup mongodb
     *
     * @param path
     */
    private void backupMongoDB(Path path) throws IOException {
        log.debug("backup MongoDb the path:{}", path);
        Path configFile = null;
        try {
            configFile = createTempConfig();
            setSecurePermissions(configFile);
            ProcessBuilder pb = new ProcessBuilder(
                    backupProperties.getTools().getMongo().getDump(),
                    "--config=" + configFile.toAbsolutePath(),
                    "--out=" + path.toAbsolutePath()
            );
            pb.redirectErrorStream(true);
            log.debug("Executing: {} --uri=*** --out={}",
                    backupProperties.getTools().getMongo().getDump(), path);

            Process process = pb.start();
            String output = readStream(process.getInputStream());
            boolean finished = process.waitFor(BACKUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException(
                        "MongoDB backup timed out after " + BACKUP_TIMEOUT_MINUTES + " minutes");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("mongodump failed with exit code {}: {}", exitCode, output);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "mongodump failed with exit code " + exitCode + ": " + output);
            }

            log.info("MongoDB backup completed successfully to: {}", path);


        } catch (IOException | InterruptedException e) {
            log.error("MongoDB backup failed", e);
            Thread.currentThread().interrupt();
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "MongoDB backup failed", e);
        } finally {
            if (configFile != null) {
                Files.deleteIfExists(configFile);
            }
        }
    }


    private Path createTempConfig() throws IOException {
        Path configFile = Files.createTempFile("mongodump_", ".conf");
        String yaml = String.format(
                "uri: \"%s\"%n",
                buildMongoUri()
        );

        Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));
        return configFile;
    }


    /**
     * build mongo uri
     *
     * @return
     */
    private String buildMongoUri() {
        String servers = mongoDBProperties.getServers();
        String database = mongoDBProperties.getDatabase();
        String user = mongoDBProperties.getUser();
        String password = mongoDBProperties.getPwd();
        String authSource = MongoDBProperties.AUTH_DB;
        boolean authEnabled = StringUtils.hasText(user) && StringUtils.hasText(password);

        try {
            if (!authEnabled) {
                return String.format("mongodb://%s/%s", servers,
                        StringUtils.hasText(database) ? database : "");
            }
            String encodedUser = URLEncoder.encode(user, StandardCharsets.UTF_8.name());
            String encodedPwd = URLEncoder.encode(password, StandardCharsets.UTF_8.name());

            StringBuilder uri = new StringBuilder("mongodb://")
                    .append(encodedUser).append(":").append(encodedPwd)
                    .append("@").append(servers).append("/");

            if (StringUtils.hasText(database)) {
                uri.append(database);
            }
            String finalAuthSource = StringUtils.hasText(authSource) ? authSource : "admin";
            uri.append("?authSource=").append(finalAuthSource);
            return uri.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode MongoDB credentials", e);
        }
    }

    @Override
    public void restore(String path) {

    }

    @Override
    public boolean isAvailable() {
        return ScheduleUtils.isExecutable(backupProperties.getTools().getMongo().getDump())
                && ScheduleUtils.isExecutable(backupProperties.getTools().getMongo().getRestore());
    }
}
