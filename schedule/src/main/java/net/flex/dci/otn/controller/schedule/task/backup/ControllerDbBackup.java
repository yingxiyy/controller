package net.flex.dci.otn.controller.schedule.task.backup;

import static net.flex.dci.otn.controller.schedule.utils.Constants.BACKUP_DIR_FORMAT;
import static net.flex.dci.otn.controller.schedule.utils.Constants.GLOBAL_BACKUP_TIMEOUT_MINUTES;

import cn.hutool.core.exceptions.ExceptionUtil;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
import net.flex.dci.otn.controller.schedule.component.FtpManager;
import net.flex.dci.otn.controller.schedule.core.backup.BackupProvider;
import net.flex.dci.otn.controller.schedule.enums.ExecuteResult;
import net.flex.dci.otn.controller.schedule.enums.TaskType;
import net.flex.dci.otn.controller.schedule.message.TaskInfoNotifier;
import net.flex.dci.otn.controller.schedule.properties.BackupProperties;
import net.flex.dci.otn.controller.schedule.task.TaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 2026/6/2
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerDbBackup implements TaskProcessor {

    @Autowired
    private List<BackupProvider> providers;

    private final FtpManager ftpManager;

    private final BackupProperties backupProperties;

    private final TaskInfoNotifier taskInfoNotifier;


    @Qualifier("backupTaskExecutor")
    private final Executor backupTaskExecutor;

    @Override
    public void process(MoSchedule moSchedule) {
        log.info("start to backup the controller db backup");
        long totalStartTime = System.currentTimeMillis();
        try {
            taskInfoNotifier.sendCtrlDbBackupStart(totalStartTime, moSchedule);
            Path localBackupDir = Paths.get(backupProperties.getLocalBackupDir());
            log.info("local backup dir is :{}", localBackupDir.toAbsolutePath());
//        String remoteBackupDir = backupProperties.getRemoteBackupDir();
            Path timestampBackupDir = createTimestampedBackupDir(localBackupDir);
            log.info("current backup dir: {}", timestampBackupDir.toAbsolutePath());
            List<BackupResult> results = backupControllerDb(timestampBackupDir);

            long successCount = results.stream().filter(BackupResult::isSuccess).count();
            long failureCount = results.size() - successCount;
            long totalTime = System.currentTimeMillis() - totalStartTime;

            log.info("all backup task finished，success: {}，failed: {}，total cost: {}ms",
                    successCount, failureCount, totalTime);
            uploadBackupFileToSftpServer(timestampBackupDir);
            int cleanedCount = cleanExpiredBackups(localBackupDir);
            log.info("clean expired backup finished ，total delete {} expired backup dir",
                    cleanedCount);
            moSchedule.setResult(ExecuteResult.Success.name());
            moSchedule.setResultInfo(String.format(
                    "all backup task finished，success: %d，failed: %d，total cost: %d ms",
                    successCount, failureCount, totalTime));

        } catch (Exception ex) {
            log.error("controller backup failed", ex);
            moSchedule.setResult(ExecuteResult.Failure.name());
            moSchedule.setResultInfo(
                    "controller db backup failed: " + ExceptionUtil.getRootCauseMessage(ex));
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Controller db backup failed", ex);
        } finally {
            taskInfoNotifier.sendCtrlDbBackupFinished(totalStartTime, moSchedule);
        }
    }

    /**
     * upload backup file to sftp server
     *
     * @param timestampBackupDir
     */
    private void uploadBackupFileToSftpServer(Path timestampBackupDir) {
        boolean isSftpAvailable = ftpManager.isAvailable();
        if (!isSftpAvailable) {
            log.warn("current sftp server is not available,use local backup instead");
            return;
        }
        String timestampDirName = timestampBackupDir.getFileName().toString();
        log.info("Start uploading backup files from: {}", timestampDirName);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(timestampBackupDir,
                "*.tar.gz")) {
            for (Path tarFile : stream) {
                try {
                    ftpManager.uploadFile(tarFile, timestampDirName);
                } catch (Exception e) {
                    log.error("Failed to upload: {}", tarFile.getFileName(), e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan backup directory: {}", timestampBackupDir, e);
            return;
        }
        try {
            int cleaned = ftpManager.cleanupRemoteExpiredBackups();
            log.info("Remote cleanup done, deleted {} expired backup(s)", cleaned);
        } catch (Exception e) {
            log.error("Remote cleanup failed", e);
        }
    }

    private List<BackupResult> backupControllerDb(Path timestampBackupDir)
            throws ExecutionException, InterruptedException, TimeoutException {
        log.debug("start to backup controller db to local path:{}",
                timestampBackupDir.toAbsolutePath());
        List<CompletableFuture<BackupResult>> futures = providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(
                        () -> executeSingleBackup(provider, timestampBackupDir),
                        backupTaskExecutor
                ))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(GLOBAL_BACKUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    private BackupResult executeSingleBackup(BackupProvider provider, Path baseBackupDir) {
        String providerName = provider.getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        log.info("provider :{} backup start", providerName);
        String providerBackupDirPath = baseBackupDir.toAbsolutePath().toString();
        try {
            provider.backup(providerBackupDirPath);
            long duration = System.currentTimeMillis() - startTime;
            log.info("backup success: {}，cost: {}ms", providerName, duration);
            return BackupResult.success(providerName, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("backup Failed: {}，cost: {}ms", providerName, duration, e);
            return BackupResult.failure(providerName, duration,
                    ExceptionUtil.getRootCauseMessage(e));
        }
    }

    private int cleanExpiredBackups(Path localBackupDir) {
        log.info("start to clean {} expired backup ", backupProperties.getTtl());
        int cleanedCount = 0;
        LocalDateTime expirationTime = LocalDateTime.now().minusDays(backupProperties.getTtl());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(localBackupDir)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    continue;
                }

                String dirName = path.getFileName().toString();
                try {
                    LocalDateTime dirTime = LocalDateTime.parse(dirName, BACKUP_DIR_FORMAT);

                    if (dirTime.isBefore(expirationTime)) {
                        log.info("remove expired directory: {}", path);
                        deleteDirectoryRecursively(path);
                        cleanedCount++;
                    }

                } catch (DateTimeParseException e) {
                    log.debug("skip non backup dir: {}", dirName);
                } catch (IOException e) {
                    log.error("remove directory failed: {}", path, e);
                }
            }
        } catch (IOException e) {
            log.error("walk through backup failed: {}", localBackupDir, e);
        }

        return cleanedCount;
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Path createTimestampedBackupDir(Path baseDir) throws IOException {
        String timestamp = LocalDateTime.now().format(BACKUP_DIR_FORMAT);
        Path backupDir = baseDir.resolve(timestamp);

        Files.createDirectories(backupDir);
        log.debug("success create folder: {}", backupDir);

        return backupDir;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.controllerDataBackup;
    }


    @Data
    @AllArgsConstructor
    private static class BackupResult {

        private String providerName;
        private boolean success;
        private long durationMs;
        private String errorMessage;

        public static BackupResult success(String providerName, long durationMs) {
            return new BackupResult(providerName, true, durationMs, null);
        }

        public static BackupResult failure(String providerName, long durationMs,
                String errorMessage) {
            return new BackupResult(providerName, false, durationMs, errorMessage);
        }
    }
}
