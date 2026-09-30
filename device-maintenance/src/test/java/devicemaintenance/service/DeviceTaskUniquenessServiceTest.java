package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.repository.DeviceTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTaskUniquenessServiceTest {

    @Mock
    private DeviceTaskRepository deviceTaskRepository;

    @InjectMocks
    private DeviceTaskUniquenessService uniquenessService;

    @Test
    void checkAndGetAvailableTask_reusesCurrentBatchWorkflowWhenNightlyBackupIsLatestCompletedTask() {
        DeviceTask upgradeDownloadTask = new DeviceTask("task-download", "device-001", DeviceTask.TaskType.DOWNLOAD);
        upgradeDownloadTask.setBatchId("upgrade-batch");
        upgradeDownloadTask.setWorkflowId("workflow-001");
        upgradeDownloadTask.setStatus(DeviceTask.TaskStatus.COMPLETED);
        upgradeDownloadTask.setCreatedTime(LocalDateTime.of(2026, 7, 2, 17, 52, 36));
        upgradeDownloadTask.setUpdatedTime(LocalDateTime.of(2026, 7, 2, 17, 52, 36));

        DeviceTask nightlyBackupTask = new DeviceTask("task-nightly-backup", "device-001", DeviceTask.TaskType.BACKUP);
        nightlyBackupTask.setBatchId("nightly-backup-batch");
        nightlyBackupTask.setStatus(DeviceTask.TaskStatus.COMPLETED);
        nightlyBackupTask.setCreatedTime(LocalDateTime.of(2026, 7, 3, 2, 0, 0));
        nightlyBackupTask.setUpdatedTime(LocalDateTime.of(2026, 7, 3, 2, 30, 0));

        when(deviceTaskRepository.findByDeviceIdOrderByCreatedTimeDesc("device-001"))
                .thenReturn(Arrays.asList(nightlyBackupTask, upgradeDownloadTask));
        when(deviceTaskRepository.findByBatchIdAndDeviceId("upgrade-batch", "device-001"))
                .thenReturn(Collections.singletonList(upgradeDownloadTask));

        DeviceTask result = uniquenessService.checkAndGetAvailableTask(
                "device-001", DeviceTask.TaskType.BACKUP, "upgrade-batch");

        assertSame(upgradeDownloadTask, result);
    }

    @Test
    void checkAndGetAvailableTask_blocksWhenAnotherBatchTaskIsStillActive() {
        DeviceTask upgradeDownloadTask = new DeviceTask("task-download", "device-001", DeviceTask.TaskType.DOWNLOAD);
        upgradeDownloadTask.setBatchId("upgrade-batch");
        upgradeDownloadTask.setWorkflowId("workflow-001");
        upgradeDownloadTask.setStatus(DeviceTask.TaskStatus.COMPLETED);
        upgradeDownloadTask.setCreatedTime(LocalDateTime.of(2026, 7, 2, 17, 52, 36));
        upgradeDownloadTask.setUpdatedTime(LocalDateTime.of(2026, 7, 2, 17, 52, 36));

        DeviceTask runningBackupTask = new DeviceTask("task-nightly-backup", "device-001", DeviceTask.TaskType.BACKUP);
        runningBackupTask.setBatchId("nightly-backup-batch");
        runningBackupTask.setStatus(DeviceTask.TaskStatus.RUNNING);
        runningBackupTask.setCreatedTime(LocalDateTime.of(2026, 7, 3, 2, 0, 0));
        runningBackupTask.setUpdatedTime(LocalDateTime.of(2026, 7, 3, 2, 10, 0));

        when(deviceTaskRepository.findByDeviceIdOrderByCreatedTimeDesc("device-001"))
                .thenReturn(Arrays.asList(runningBackupTask, upgradeDownloadTask));

        assertThrows(RuntimeException.class, () -> uniquenessService.checkAndGetAvailableTask(
                "device-001", DeviceTask.TaskType.BACKUP, "upgrade-batch"));
    }
}
