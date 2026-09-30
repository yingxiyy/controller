package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadTerminalEvidencePolicyTest {

    private static final LocalDateTime STARTED_TIME = LocalDateTime.of(2026, 8, 25, 17, 20, 0);

    @Test
    void validate_rejectsStateOnlyTerminalNotification() {
        DownloadTerminalEvidencePolicy.Validation result =
            DownloadTerminalEvidencePolicy.validate(runningTask(), notification(null, null));

        assertFalse(result.isAccepted());
        assertEquals("missing-file-name", result.getReason());
    }

    @Test
    void validate_acceptsMatchingFreshTerminalNotification() {
        DownloadTerminalEvidencePolicy.Validation result = DownloadTerminalEvidencePolicy.validate(
            runningTask(),
            notification("908.pkg", operationTime(STARTED_TIME.plusSeconds(10)))
        );

        assertTrue(result.isAccepted());
        assertEquals("accepted", result.getReason());
    }

    @Test
    void validate_rejectsMismatchedFileName() {
        DownloadTerminalEvidencePolicy.Validation result = DownloadTerminalEvidencePolicy.validate(
            runningTask(),
            notification("other.pkg", operationTime(STARTED_TIME.plusSeconds(10)))
        );

        assertFalse(result.isAccepted());
        assertEquals("file-name-mismatch", result.getReason());
    }

    @Test
    void validate_rejectsStaleOperationTime() {
        DownloadTerminalEvidencePolicy.Validation result = DownloadTerminalEvidencePolicy.validate(
            runningTask(),
            notification("908.pkg", operationTime(STARTED_TIME.minusMinutes(3)))
        );

        assertFalse(result.isAccepted());
        assertEquals("stale-operation-time", result.getReason());
    }

    @Test
    void validate_rejectsMalformedOperationTime() {
        DownloadTerminalEvidencePolicy.Validation result = DownloadTerminalEvidencePolicy.validate(
            runningTask(),
            notification("908.pkg", "not-a-time")
        );

        assertFalse(result.isAccepted());
        assertEquals("malformed-operation-time", result.getReason());
    }

    @Test
    void validate_rejectsTaskThatIsNotRunning() {
        DeviceTask task = runningTask();
        task.setStatus(DeviceTask.TaskStatus.PENDING);

        DownloadTerminalEvidencePolicy.Validation result = DownloadTerminalEvidencePolicy.validate(
            task,
            notification("908.pkg", operationTime(STARTED_TIME.plusSeconds(10)))
        );

        assertFalse(result.isAccepted());
        assertEquals("task-not-running", result.getReason());
    }

    @Test
    void validate_rejectsTaskWithoutStartedTime() {
        DeviceTask task = runningTask();
        task.setStartedTime(null);

        DownloadTerminalEvidencePolicy.Validation result = DownloadTerminalEvidencePolicy.validate(
            task,
            notification("908.pkg", operationTime(STARTED_TIME.plusSeconds(10)))
        );

        assertFalse(result.isAccepted());
        assertEquals("missing-task-start", result.getReason());
    }

    @Test
    void isLatestDownloadCompleted_rejectsOlderCompletionWhenLatestTaskFailed() {
        DeviceTask olderCompleted = downloadTask(
            "old",
            DeviceTask.TaskStatus.COMPLETED,
            STARTED_TIME
        );
        DeviceTask latestFailed = downloadTask(
            "latest",
            DeviceTask.TaskStatus.FAILED,
            STARTED_TIME.plusMinutes(1)
        );

        assertFalse(DownloadTerminalEvidencePolicy.isLatestDownloadCompleted(
            Arrays.asList(olderCompleted, latestFailed)
        ));
    }

    @Test
    void isLatestDownloadCompleted_rejectsRunningLatestTask() {
        assertFalse(DownloadTerminalEvidencePolicy.isLatestDownloadCompleted(Collections.singletonList(
            downloadTask("latest", DeviceTask.TaskStatus.RUNNING, STARTED_TIME)
        )));
    }

    @Test
    void isLatestDownloadCompleted_acceptsCompletedLatestTask() {
        assertTrue(DownloadTerminalEvidencePolicy.isLatestDownloadCompleted(Collections.singletonList(
            downloadTask("latest", DeviceTask.TaskStatus.COMPLETED, STARTED_TIME)
        )));
    }

    @Test
    void isLatestDownloadCompleted_usesCreatedTimeToBreakUpdatedTimeTie() {
        DeviceTask olderCompleted = downloadTask(
            "old",
            DeviceTask.TaskStatus.COMPLETED,
            STARTED_TIME.plusMinutes(5)
        );
        olderCompleted.setCreatedTime(STARTED_TIME);

        DeviceTask newerFailed = downloadTask(
            "latest",
            DeviceTask.TaskStatus.FAILED,
            STARTED_TIME.plusMinutes(5)
        );
        newerFailed.setCreatedTime(STARTED_TIME.plusMinutes(1));

        assertFalse(DownloadTerminalEvidencePolicy.isLatestDownloadCompleted(
            Arrays.asList(olderCompleted, newerFailed)
        ));
    }

    private DeviceTask runningTask() {
        DeviceTask task = new DeviceTask("task-1", "device-1", DeviceTask.TaskType.DOWNLOAD);
        task.setStatus(DeviceTask.TaskStatus.RUNNING);
        task.setFilePath("/upload/deviceSoftware/huawei/908.pkg");
        task.setStartedTime(STARTED_TIME);
        return task;
    }

    private DeviceTask downloadTask(
            String taskId,
            DeviceTask.TaskStatus status,
            LocalDateTime updatedTime) {
        DeviceTask task = new DeviceTask(taskId, "device-1", DeviceTask.TaskType.DOWNLOAD);
        task.setStatus(status);
        task.setUpdatedTime(updatedTime);
        return task;
    }

    private Map<String, String> notification(String fileName, String operationTime) {
        Map<String, String> notification = new HashMap<>();
        if (fileName != null) {
            notification.put("download.file-name", fileName);
        }
        if (operationTime != null) {
            notification.put("download.download-time", operationTime);
        }
        return notification;
    }

    private String operationTime(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault())
            .toOffsetDateTime()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
