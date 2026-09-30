package net.flex.dci.otn.controller.resource.statistic.export.file;

import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.FILE_FORMATTER;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.properties.ExportFileProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 2026/6/10
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ExportFileClear {

    private final ExportFileProperties exportFileProperties;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredFiles() {
        log.info("start to clean the export expired files");
        Path exportFileDir = Paths.get(exportFileProperties.getPath());
        if (!Files.exists(exportFileDir) || !Files.isDirectory(exportFileDir)) {
            return;
        }

        LocalDate expireDate = LocalDate.now().minusDays(exportFileProperties.getTtl());
        log.info("Cleanup rule: Delete directories older than {} days (cutoff date: {})",
                exportFileProperties.getTtl(), expireDate.format(FILE_FORMATTER));
        int deletedDirCount = 0;
        int skippedDirCount = 0;
        long freedSpaceBytes = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(exportFileDir)) {
            for (Path dir : stream) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                String dirName = dir.getFileName().toString();
                try {
                    LocalDate dirDate = LocalDate.parse(dir.getFileName().toString(),
                            FILE_FORMATTER);
                    if (dirDate.isBefore(expireDate)) {
                        long dirSize = calculateDirSize(dir);
                        Files.walk(dir)
                                .sorted(Comparator.reverseOrder())
                                .forEach(path -> {
                                    try {
                                        Files.delete(path);
                                    } catch (IOException e) {
                                        log.error("remove file failed: {}", path, e);
                                    }
                                });
                        deletedDirCount++;
                        freedSpaceBytes += dirSize;
                        log.info("already delete expired dir: {}", dir);
                    }
                } catch (DateTimeParseException e) {
                    log.debug("Skipping non-date format directory: {}", dirName);
                    skippedDirCount++;
                }
            }
        } catch (IOException e) {
            log.error("failed to clear the expired file", e);
        }
        log.info("========== Expired export file cleanup task completed ==========");
        log.info("Statistics :Deleted:{} skipped non-date DirCount:{},freed {} MB Size",
                deletedDirCount, skippedDirCount, freedSpaceBytes / 1024 / 1024);
    }

    private long calculateDirSize(Path dir) {
        try {
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            log.warn("Failed to get file size: {}", p, e);
                            return 0;
                        }
                    })
                    .sum();

        } catch (IOException e) {
            log.warn("Failed to calculate directory size: {}", dir, e);
            return 0;
        }
    }
}
