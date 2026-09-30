package net.flex.dci.otn.controller.schedule.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 2026/5/31
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class ScheduleUtils {

    public static boolean isExecutable(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.canExecute();
    }


    public static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.error("Failed to delete {}", path, e);
                        }
                    });
        }
    }

    /**
     * compress to tar gz
     *
     * @param sourceDir
     * @param targetTarFile
     */
    public static void compressToTarGz(Path sourceDir, Path targetTarFile)
            throws IOException, InterruptedException {
        log.debug("compress sourceDir:{} to targetTarFile:{}", sourceDir, targetTarFile);
        ProcessBuilder pb = new ProcessBuilder("tar", "-czf", targetTarFile.toString(), "-C",
                sourceDir.getParent().toString(), sourceDir.getFileName().toString());
        pb.redirectErrorStream(true);
        log.debug("Compressing {} to {}", sourceDir, targetTarFile);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errorOutput = readStream(process.getInputStream());
            throw new IOException("tar compression failed: " + errorOutput);
        }
    }

    public static String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static void setSecurePermissions(Path file) throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return; // Windows 用 ACL，比较复杂
        }
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(file, perms);
    }

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd");


    public static String getDateStr(Long timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }

        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                DEFAULT_ZONE_ID
        ).format(DATE_FORMATTER);
    }
}
