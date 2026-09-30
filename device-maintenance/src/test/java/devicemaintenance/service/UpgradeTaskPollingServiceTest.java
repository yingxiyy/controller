package devicemaintenance.service;

import devicemaintenance.dto.DeviceOperationStatus;
import devicemaintenance.entity.DeviceTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpgradeTaskPollingServiceTest {

    private final UpgradeTaskPollingService service =
        new UpgradeTaskPollingService(null, null, null, null);

    @Test
    void determineTaskStatus_doesNotCompleteDownloadFromCurrentSnapshot() {
        assertNull(service.determineTaskStatus(
            task(DeviceTask.TaskType.DOWNLOAD),
            "COMPLETE",
            DeviceTask.TaskType.DOWNLOAD,
            new DeviceOperationStatus()
        ));
    }

    @Test
    void determineTaskStatus_stillFailsDownloadFromCurrentSnapshot() {
        assertEquals(DeviceTask.TaskStatus.FAILED, service.determineTaskStatus(
            task(DeviceTask.TaskType.DOWNLOAD),
            "FAIL",
            DeviceTask.TaskType.DOWNLOAD,
            new DeviceOperationStatus()
        ));
    }

    @Test
    void determineTaskStatus_preservesBackupCompletionMapping() {
        assertEquals(DeviceTask.TaskStatus.COMPLETED, service.determineTaskStatus(
            task(DeviceTask.TaskType.BACKUP),
            "COMPLETE",
            DeviceTask.TaskType.BACKUP,
            new DeviceOperationStatus()
        ));
    }

    private DeviceTask task(DeviceTask.TaskType taskType) {
        DeviceTask task = new DeviceTask("task-1", "device-1", taskType);
        task.setStatus(DeviceTask.TaskStatus.RUNNING);
        return task;
    }
}
