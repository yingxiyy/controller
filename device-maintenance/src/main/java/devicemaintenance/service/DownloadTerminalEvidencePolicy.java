package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validates whether a download terminal notification belongs to the active
 * controller task. A current-state COMPLETE snapshot alone is deliberately
 * insufficient because a device rollback can restore that normal state.
 */
public final class DownloadTerminalEvidencePolicy {

    private static final long CLOCK_SKEW_MINUTES = 2L;

    private DownloadTerminalEvidencePolicy() {
    }

    public static Validation validate(DeviceTask task, Map<String, String> notification) {
        if (task == null || task.getStatus() != DeviceTask.TaskStatus.RUNNING) {
            return Validation.rejected("task-not-running");
        }
        if (task.getStartedTime() == null) {
            return Validation.rejected("missing-task-start");
        }

        String actualFileName = firstNonBlank(notification, "download.file-name", "file-name");
        if (actualFileName == null) {
            return Validation.rejected("missing-file-name");
        }

        String expectedFileName = baseName(task.getFilePath());
        if (expectedFileName == null || !expectedFileName.equals(baseName(actualFileName))) {
            return Validation.rejected("file-name-mismatch");
        }

        String operationTime = firstNonBlank(notification, "download.download-time", "download-time");
        if (operationTime == null) {
            return Validation.rejected("missing-operation-time");
        }

        final OffsetDateTime parsedOperationTime;
        try {
            parsedOperationTime = OffsetDateTime.parse(operationTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeException e) {
            return Validation.rejected("malformed-operation-time");
        }

        if (parsedOperationTime.toInstant().isBefore(
                task.getStartedTime()
                    .atZone(ZoneId.systemDefault())
                    .minusMinutes(CLOCK_SKEW_MINUTES)
                    .toInstant())) {
            return Validation.rejected("stale-operation-time");
        }

        return Validation.accepted();
    }

    public static boolean isLatestDownloadCompleted(List<DeviceTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return false;
        }

        Optional<DeviceTask> latestDownload = tasks.stream()
            .filter(task -> task != null && task.getTaskType() == DeviceTask.TaskType.DOWNLOAD)
            .max(Comparator.comparing(
                DownloadTerminalEvidencePolicy::effectiveTime,
                Comparator.nullsFirst(Comparator.naturalOrder())
            ).thenComparing(
                DeviceTask::getCreatedTime,
                Comparator.nullsFirst(Comparator.naturalOrder())
            ));

        return latestDownload.isPresent()
            && latestDownload.get().getStatus() == DeviceTask.TaskStatus.COMPLETED;
    }

    private static LocalDateTime effectiveTime(DeviceTask task) {
        return task.getUpdatedTime() != null ? task.getUpdatedTime() : task.getCreatedTime();
    }

    private static String firstNonBlank(Map<String, String> values, String primaryKey, String fallbackKey) {
        if (values == null) {
            return null;
        }
        String value = trimToNull(values.get(primaryKey));
        return value != null ? value : trimToNull(values.get(fallbackKey));
    }

    private static String baseName(String path) {
        String value = trimToNull(path);
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class Validation {
        private final boolean accepted;
        private final String reason;

        private Validation(boolean accepted, String reason) {
            this.accepted = accepted;
            this.reason = reason;
        }

        private static Validation accepted() {
            return new Validation(true, "accepted");
        }

        private static Validation rejected(String reason) {
            return new Validation(false, reason);
        }

        public boolean isAccepted() {
            return accepted;
        }

        public String getReason() {
            return reason;
        }
    }
}
