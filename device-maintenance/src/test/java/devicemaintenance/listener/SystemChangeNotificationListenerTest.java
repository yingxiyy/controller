package devicemaintenance.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.UpgradeWorkflow;
import devicemaintenance.dto.DeviceOperationStatus;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.UpgradeWorkflowRepository;
import devicemaintenance.service.BatchLogService;
import devicemaintenance.service.BatchUpgradeService;
import devicemaintenance.service.DeviceMaintenanceStatusService;
import devicemaintenance.service.SimpleNotificationListenerService;
import devicemaintenance.service.UnifiedBatchService;
import devicemaintenance.service.WorkflowManagementService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemChangeNotificationListenerTest {

    private static final LocalDateTime DOWNLOAD_STARTED = LocalDateTime.of(2026, 8, 25, 17, 20, 0);

    @Mock
    private DeviceTaskRepository deviceTaskRepository;

    @Mock
    private BatchRepository maintenanceBatchRepository;

    @Mock
    private SimpleNotificationListenerService debugService;

    @Mock
    private TaskInfoNotificationService taskInfoNotificationService;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScheduledFuture<Object> scheduledFuture;

    @Mock
    private UpgradeWorkflowRepository workflowRepository;

    @Mock
    private WorkflowManagementService workflowManagementService;

    @Mock
    private BatchUpgradeService batchUpgradeService;

    @Mock
    private UnifiedBatchService unifiedBatchService;

    @Mock
    private BatchLogService batchLogService;

    @Mock
    private DeviceMaintenanceStatusService deviceMaintenanceStatusService;

    private SystemChangeNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new SystemChangeNotificationListener(
            deviceTaskRepository,
            maintenanceBatchRepository,
            new ObjectMapper(),
            debugService,
            taskInfoNotificationService,
            taskScheduler,
            workflowRepository,
            maintenanceBatchRepository,
            workflowManagementService,
            batchUpgradeService,
            unifiedBatchService,
            batchLogService,
            deviceMaintenanceStatusService
        );
    }

    @Test
    void handleSystemChangeNotification_syncsBatchTargetVersionFromDownloadSoftwareVersion() throws Exception {
        DeviceTask downloadTask = new DeviceTask("task-1", "device-1", DeviceTask.TaskType.DOWNLOAD);
        downloadTask.setStatus(DeviceTask.TaskStatus.RUNNING);
        downloadTask.setBatchId("batch-1");
        downloadTask.setFilePath("/upload/deviceSoftware/huawei/908.pkg");
        downloadTask.setStartedTime(DOWNLOAD_STARTED);

        Batch batch = new Batch();
        batch.setBatchId("batch-1");
        batch.setTargetVersion("FROM_PACKAGE_NAME");

        when(deviceTaskRepository.findIncompleteTasksByDeviceAndType("device-1", DeviceTask.TaskType.DOWNLOAD))
            .thenReturn(Collections.singletonList(downloadTask));
        when(maintenanceBatchRepository.findById("batch-1")).thenReturn(Optional.of(batch));
        when(workflowRepository.findByDeviceId("device-1")).thenReturn(Collections.emptyList());
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        java.util.Map<String, String> notification = new java.util.HashMap<>();
        notification.put("neId", "device-1");
        notification.put("download.download-state", "COMPLETE");
        notification.put("download.file-name", "908.pkg");
        notification.put("download.download-time", operationTime(DOWNLOAD_STARTED.plusSeconds(10)));
        notification.put("download.software-version", "FROM_DEVICE_PARSE");
        String payload = new ObjectMapper().writeValueAsString(notification);

        listener.handleSystemChangeNotification(new ConsumerRecord<>("system-change", 0, 0L, null, payload));

        ArgumentCaptor<Batch> batchCaptor = ArgumentCaptor.forClass(Batch.class);
        verify(maintenanceBatchRepository).save(batchCaptor.capture());
        assertEquals("FROM_DEVICE_PARSE", batchCaptor.getValue().getTargetVersion());
        assertEquals("FROM_DEVICE_PARSE", downloadTask.getTargetVersion());
    }

    @Test
    void handleSystemChangeNotification_ignoresStateOnlyDownloadComplete() throws Exception {
        DeviceTask task = runningDownloadTask();
        stubDownloadTask(task);

        Map<String, String> notification = new HashMap<>();
        notification.put("neId", "device-1");
        notification.put("download.download-state", "COMPLETE");

        handle(notification);

        assertEquals(DeviceTask.TaskStatus.RUNNING, task.getStatus());
        verify(deviceTaskRepository, never()).save(any(DeviceTask.class));
    }

    @Test
    void handleSystemChangeNotification_completesCorrelatedDownload() throws Exception {
        DeviceTask task = runningDownloadTask();
        stubDownloadTask(task);
        when(workflowRepository.findByDeviceId("device-1")).thenReturn(Collections.emptyList());

        handle(terminalNotification("COMPLETE", "908.pkg", DOWNLOAD_STARTED.plusSeconds(10)));

        assertEquals(DeviceTask.TaskStatus.COMPLETED, task.getStatus());
        verify(deviceTaskRepository).save(task);
    }

    @Test
    void handleSystemChangeNotification_failsCorrelatedDownload() throws Exception {
        DeviceTask task = runningDownloadTask();
        stubDownloadTask(task);
        when(workflowRepository.findByDeviceId("device-1")).thenReturn(Collections.emptyList());

        handle(terminalNotification("FAIL", "908.pkg", DOWNLOAD_STARTED.plusSeconds(10)));

        assertEquals(DeviceTask.TaskStatus.FAILED, task.getStatus());
        verify(deviceTaskRepository).save(task);
    }

    @Test
    void handleSystemChangeNotification_ignoresTerminalStateForDifferentFile() throws Exception {
        DeviceTask task = runningDownloadTask();
        stubDownloadTask(task);

        handle(terminalNotification("COMPLETE", "other.pkg", DOWNLOAD_STARTED.plusSeconds(10)));

        assertEquals(DeviceTask.TaskStatus.RUNNING, task.getStatus());
        verify(deviceTaskRepository, never()).save(any(DeviceTask.class));
    }

    @Test
    void handleSystemChangeNotification_ignoresStaleTerminalState() throws Exception {
        DeviceTask task = runningDownloadTask();
        stubDownloadTask(task);

        handle(terminalNotification("FAIL", "908.pkg", DOWNLOAD_STARTED.minusMinutes(3)));

        assertEquals(DeviceTask.TaskStatus.RUNNING, task.getStatus());
        verify(deviceTaskRepository, never()).save(any(DeviceTask.class));
    }

    @Test
    void isWorkflowStepConfirmedByOp_doesNotAdvanceDownloadFromRollbackSnapshot() {
        Batch batch = new Batch();
        batch.setBatchId("batch-1");

        UpgradeWorkflow workflow = new UpgradeWorkflow("workflow-1", "batch-1", "device-1");
        workflow.setCurrentStep("DOWNLOAD");

        DeviceTask task = runningDownloadTask();
        task.setBatchId("batch-1");
        task.setWorkflowId("workflow-1");

        DeviceOperationStatus status = DeviceOperationStatus.builder()
            .softwareOperations(DeviceOperationStatus.SoftwareOperations.builder()
                .download(DeviceOperationStatus.SoftwareOperations.SoftwareDownload.builder()
                    .state("COMPLETE")
                    .fileName("908.pkg")
                    .downloadTime(operationTime(DOWNLOAD_STARTED))
                    .build())
                .build())
            .build();
        when(deviceMaintenanceStatusService.getDeviceOperationStatus("device-1")).thenReturn(status);

        assertFalse(listener.isWorkflowStepConfirmedByOp(
            batch,
            workflow,
            Collections.singletonList(task)
        ));
    }

    private DeviceTask runningDownloadTask() {
        DeviceTask task = new DeviceTask("task-1", "device-1", DeviceTask.TaskType.DOWNLOAD);
        task.setStatus(DeviceTask.TaskStatus.RUNNING);
        task.setFilePath("/upload/deviceSoftware/huawei/908.pkg");
        task.setStartedTime(DOWNLOAD_STARTED);
        return task;
    }

    private void stubDownloadTask(DeviceTask task) {
        when(deviceTaskRepository.findIncompleteTasksByDeviceAndType("device-1", DeviceTask.TaskType.DOWNLOAD))
            .thenReturn(Collections.singletonList(task));
    }

    private Map<String, String> terminalNotification(
            String state,
            String fileName,
            LocalDateTime operationTime) {
        Map<String, String> notification = new HashMap<>();
        notification.put("neId", "device-1");
        notification.put("download.download-state", state);
        notification.put("download.file-name", fileName);
        notification.put("download.download-time", operationTime(operationTime));
        return notification;
    }

    private void handle(Map<String, String> notification) throws Exception {
        String payload = new ObjectMapper().writeValueAsString(notification);
        listener.handleSystemChangeNotification(new ConsumerRecord<>("system-change", 0, 0L, null, payload));
    }

    private static String operationTime(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault())
            .toOffsetDateTime()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
