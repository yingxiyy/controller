package net.flex.dci.otn.controller.resource.statistic.export.file;

import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.FILE_FORMATTER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.properties.ExportFileProperties;
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
public class ExportFileServiceImpl implements ExportFileService {

    private final ExportFileProperties exportFileProperties;

    /**
     * init export file
     */
    @Override
    public void initExportDir() {
        log.info("init export file dir");
        Path rootDir = Paths.get(exportFileProperties.getPath());
        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            try {
                Files.createDirectories(rootDir);
                log.info("init export file dir success:{}", rootDir);
            } catch (IOException e) {
                log.error("initializing the export root file dir failed: {}", rootDir, e);
            }
        }
    }

    @Override
    public Path generateExportFilePath(String fileName, String suffix) {
        Path rootDir = Paths.get(exportFileProperties.getPath());

        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            try {
                Files.createDirectories(rootDir);
                log.info("Export root directory created successfully on demand: {}", rootDir);
            } catch (IOException e) {
                log.error("FAILED to create export directory: {}", rootDir, e);
                throw new RuntimeException(
                        "Export directory creation failed, please check disk permissions", e);
            }
        }

        String dateDir = LocalDate.now().format(FILE_FORMATTER);
        Path fullDir = rootDir.resolve(dateDir);

        if (!Files.exists(fullDir)) {
            try {
                Files.createDirectories(fullDir);
            } catch (IOException e) {
                log.error("FAILED to create date subdirectory: {}", fullDir, e);
                throw new RuntimeException("Export directory creation failed", e);
            }
        }

        String exportFileName = String.format("%s.%s", fileName, suffix);

        return fullDir.resolve(exportFileName);
    }


}
