package devicemaintenance.service;

import devicemaintenance.dto.BatchUpgradeDto;
import devicemaintenance.dto.DeviceOperationStatus;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.BatchDevice;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.UpgradeWorkflow;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.BatchDeviceRepository;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.UpgradeWorkflowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchUpgradeServiceTest {

    @Mock
    private BatchRepository upgradeBatchRepository;

    @Mock
    private BatchDeviceRepository batchDeviceRepository;

    @Mock
    private DeviceTaskRepository deviceTaskRepository;

    @Mock
    private SoftwareDownloadService softwareDownloadService;

    @Mock
    private DeviceBackupService deviceBackupService;

    @Mock
    private DeviceUpgradeService deviceUpgradeService;

    @Mock
    private SoftwareCommitService softwareCommitService;

    @Mock
    private TaskInfoNotificationService taskInfoNotificationService;

    @Mock
    private WorkflowManagementService workflowManagementService;

    @Mock
    private UpgradeWorkflowRepository workflowRepository;

    @Mock
    private DeviceAvailabilityService deviceAvailabilityService;

    @Mock
    private NeMgrIntegrationService neMgrIntegrationService;

    @Mock
    private DeviceTaskUniquenessService uniquenessService;

    @Mock
    private UnifiedBatchService unifiedBatchService;

    @Mock
    private TaskCreationHelper taskCreationHelper;

    @Mock
    private DeviceMaintenanceStatusService deviceMaintenanceStatusService;

    @Mock
    private Executor batchTaskExecutor;

    @InjectMocks
    private BatchUpgradeService batchUpgradeService;

    @Test
    void calculateBatchStatusPublic_keepsBatchStatus_whenActiveWorkflowsHaveMixedSteps() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setExecutionMode(Batch.ExecutionMode.AUTOMATIC);

        UpgradeWorkflow wf1 = new UpgradeWorkflow("wf-1", "batch-001", "device-1");
        wf1.setCurrentStep("DOWNLOAD");

        UpgradeWorkflow wf2 = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        wf2.setCurrentStep("BACKUP");

        Batch.BatchStatus status = batchUpgradeService.calculateBatchStatusPublic(
            batch,
            Arrays.asList(wf1, wf2)
        );

        assertEquals(Batch.BatchStatus.RUNNING, status);
    }

    @Test
    void calculateBatchStatusPublic_returnsRunning_whenSomeWorkflowsFailedButOthersStillRunning() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setExecutionMode(Batch.ExecutionMode.AUTOMATIC);

        UpgradeWorkflow failedWorkflow = new UpgradeWorkflow("wf-1", "batch-001", "device-1");
        failedWorkflow.setCurrentStep("DOWNLOAD");
        failedWorkflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_FAILED);

        UpgradeWorkflow runningWorkflow = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        runningWorkflow.setCurrentStep("DOWNLOAD");
        runningWorkflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.WORKFLOW_PENDING);

        when(workflowManagementService.areAllCurrentStepsCompleted(Arrays.asList(failedWorkflow, runningWorkflow)))
            .thenReturn(false);
        when(workflowManagementService.resolveCurrentStep(Arrays.asList(failedWorkflow, runningWorkflow)))
            .thenReturn("DOWNLOAD");
        when(workflowManagementService.hasRunningTasks(Arrays.asList(failedWorkflow, runningWorkflow), "DOWNLOAD"))
            .thenReturn(true);

        Batch.BatchStatus status = batchUpgradeService.calculateBatchStatusPublic(
            batch,
            Arrays.asList(failedWorkflow, runningWorkflow)
        );

        assertEquals(Batch.BatchStatus.RUNNING, status);
    }

    @Test
    void calculateBatchStatusPublic_returnsCompletedWithErrors_whenSomeWorkflowsFailedAndOthersCompleted() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setExecutionMode(Batch.ExecutionMode.AUTOMATIC);

        UpgradeWorkflow failedWorkflow = new UpgradeWorkflow("wf-1", "batch-001", "device-1");
        failedWorkflow.setCurrentStep("DOWNLOAD");
        failedWorkflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_FAILED);

        UpgradeWorkflow completedWorkflow = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        completedWorkflow.setCurrentStep("UPGRADE");
        completedWorkflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.WORKFLOW_COMPLETED);

        Batch.BatchStatus status = batchUpgradeService.calculateBatchStatusPublic(
            batch,
            Arrays.asList(failedWorkflow, completedWorkflow)
        );

        assertEquals(Batch.BatchStatus.COMPLETED_WITH_ERRORS, status);
    }

    @Test
    void calculateBatchStatusPublic_returnsFailed_whenAllWorkflowsFailed() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setExecutionMode(Batch.ExecutionMode.AUTOMATIC);

        UpgradeWorkflow failedWorkflow1 = new UpgradeWorkflow("wf-1", "batch-001", "device-1");
        failedWorkflow1.setCurrentStep("DOWNLOAD");
        failedWorkflow1.setComputedStatus(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_FAILED);

        UpgradeWorkflow failedWorkflow2 = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        failedWorkflow2.setCurrentStep("DOWNLOAD");
        failedWorkflow2.setComputedStatus(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_FAILED);

        Batch.BatchStatus status = batchUpgradeService.calculateBatchStatusPublic(
            batch,
            Arrays.asList(failedWorkflow1, failedWorkflow2)
        );

        assertEquals(Batch.BatchStatus.FAILED, status);
    }

    @Test
    void calculateBatchStatusPublic_returnsFailedInManualMode_whenAnyWorkflowFailed() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setStatus(Batch.BatchStatus.UPGRADING);
        batch.setExecutionMode(Batch.ExecutionMode.MANUAL);

        UpgradeWorkflow failedWorkflow = new UpgradeWorkflow("wf-1", "batch-001", "device-1");
        failedWorkflow.setCurrentStep("DOWNLOAD");
        failedWorkflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_FAILED);

        UpgradeWorkflow runningWorkflow = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        runningWorkflow.setCurrentStep("DOWNLOAD");
        runningWorkflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.WORKFLOW_PENDING);

        Batch.BatchStatus status = batchUpgradeService.calculateBatchStatusPublic(
            batch,
            Arrays.asList(failedWorkflow, runningWorkflow)
        );

        assertEquals(Batch.BatchStatus.FAILED, status);
    }

    @Test
    void calculateBatchStatusPublic_returnsReadyToCommit_whenAllAutomaticWorkflowsActivatedSuccessfully() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setExecutionMode(Batch.ExecutionMode.AUTOMATIC);

        UpgradeWorkflow workflow1 = new UpgradeWorkflow("wf-1", "batch-001", "device-1");
        workflow1.setCurrentStep("COMMIT");
        workflow1.setComputedStatus(UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS);

        UpgradeWorkflow workflow2 = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        workflow2.setCurrentStep("COMMIT");
        workflow2.setComputedStatus(UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS);

        Batch.BatchStatus status = batchUpgradeService.calculateBatchStatusPublic(
            batch,
            Arrays.asList(workflow1, workflow2)
        );

        assertEquals(Batch.BatchStatus.READY_TO_COMMIT, status);
    }

    @Test
    void calculateBatchStatusPublic_returnsRunning_whenAutomaticBatchHasFailedWorkflowEvenIfOthersAreReadyToCommit() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setExecutionMode(Batch.ExecutionMode.AUTOMATIC);

        UpgradeWorkflow activatedWorkflow1 = new UpgradeWorkflow("wf-1", "batch-001", "device-1");
        activatedWorkflow1.setCurrentStep("UPGRADE");
        activatedWorkflow1.setComputedStatus(UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS);

        UpgradeWorkflow activatedWorkflow2 = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        activatedWorkflow2.setCurrentStep("UPGRADE");
        activatedWorkflow2.setComputedStatus(UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS);

        UpgradeWorkflow failedWorkflow = new UpgradeWorkflow("wf-3", "batch-001", "device-3");
        failedWorkflow.setCurrentStep("DOWNLOAD");
        failedWorkflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_FAILED);

        when(workflowManagementService.areAllCurrentStepsCompleted(
            Arrays.asList(activatedWorkflow1, activatedWorkflow2, failedWorkflow)))
            .thenReturn(true);
        when(workflowManagementService.resolveCurrentStep(
            Arrays.asList(activatedWorkflow1, activatedWorkflow2, failedWorkflow)))
            .thenReturn("UPGRADE");
        when(workflowManagementService.getNextEnabledStep(batch, "UPGRADE"))
            .thenReturn(null);

        Batch.BatchStatus status = batchUpgradeService.calculateBatchStatusPublic(
            batch,
            Arrays.asList(activatedWorkflow1, activatedWorkflow2, failedWorkflow)
        );

        assertEquals(Batch.BatchStatus.RUNNING, status);
    }

    @Test
    void removeWorkflow_deletesBatchDeviceAssociation() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setStatus(Batch.BatchStatus.READY_TO_UPGRADE);

        UpgradeWorkflow remaining = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        remaining.setCurrentStep("UPGRADE");

        BatchDevice batchDevice = new BatchDevice("batch-001", "device-1", "Node-1");

        when(upgradeBatchRepository.findById("batch-001")).thenReturn(Optional.of(batch));
        when(batchDeviceRepository.findByBatchIdAndDeviceId("batch-001", "device-1"))
            .thenReturn(Optional.of(batchDevice));
        when(workflowManagementService.getWorkflowsByBatchId("batch-001"))
            .thenReturn(Collections.singletonList(remaining));

        batchUpgradeService.removeWorkflow("batch-001", "device-1");

        verify(workflowManagementService).removeWorkflow("batch-001", "device-1");
        verify(batchDeviceRepository).delete(batchDevice);
    }

    @Test
    void createBatchUpgrade_rejectsActiveCompleteDeviceBeforeSavingBatch() {
        BatchUpgradeDto.CreateBatchUpgradeRequest request = new BatchUpgradeDto.CreateBatchUpgradeRequest();
        request.setBatchName("batch-001");
        request.setDeviceIds(Arrays.asList("device-1", "device-2"));
        request.setUpgradeFilePath("/upgrade/image.bin");
        request.setExecutionMode("MANUAL");
        request.setScheduledMode("IMMEDIATE");
        request.setEnableDownload(false);
        request.setEnableBackup(false);
        request.setEnableUpgrade(true);

        when(deviceMaintenanceStatusService.getDeviceOperationStatus("device-1"))
            .thenReturn(operationStatus("Node-1", "COMPLETE"));
        when(deviceMaintenanceStatusService.getDeviceOperationStatus("device-2"))
            .thenReturn(operationStatus("Node-2", "active-complete"));

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> batchUpgradeService.createBatchUpgrade(request)
        );

        assertTrue(exception.getMessage().contains("active-complete"));
        assertTrue(exception.getMessage().contains("Node-2 [device-2]"));
        verify(upgradeBatchRepository, never()).save(any(Batch.class));
        verify(batchDeviceRepository, never()).saveAll(any());
        verify(workflowRepository, never()).saveAll(any());
    }

    @Test
    void retryWorkflow_executesDownloadEvenWhenOpSnapshotIsComplete() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setBatchName("download-retry");
        batch.setExecutionMode(Batch.ExecutionMode.MANUAL);
        batch.setStatus(Batch.BatchStatus.FAILED);
        batch.setSftpServerName("device");
        batch.setFilePath("/upload/deviceSoftware/huawei/908.pkg");

        DeviceTask failedTask = new DeviceTask("task-1", "device-1", DeviceTask.TaskType.DOWNLOAD);
        failedTask.setBatchId("batch-001");
        failedTask.setWorkflowId("workflow-1");
        failedTask.setStatus(DeviceTask.TaskStatus.FAILED);
        failedTask.setFilePath("/upload/deviceSoftware/huawei/908.pkg");
        failedTask.setSftpServerName("device");

        UpgradeWorkflow workflow = new UpgradeWorkflow("workflow-1", "batch-001", "device-1");
        workflow.setCurrentStep("DOWNLOAD");
        workflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_FAILED);

        DeviceOperationStatus opComplete = DeviceOperationStatus.builder()
            .softwareOperations(DeviceOperationStatus.SoftwareOperations.builder()
                .download(DeviceOperationStatus.SoftwareOperations.SoftwareDownload.builder()
                    .state("COMPLETE")
                    .build())
                .build())
            .build();

        when(upgradeBatchRepository.findById("batch-001")).thenReturn(Optional.of(batch));
        when(deviceTaskRepository.findByBatchIdAndDeviceId("batch-001", "device-1"))
            .thenReturn(Collections.singletonList(failedTask));
        when(workflowRepository.findByBatchIdAndDeviceId("batch-001", "device-1"))
            .thenReturn(Optional.of(workflow));
        when(deviceMaintenanceStatusService.getDeviceOperationStatus("device-1"))
            .thenReturn(opComplete);
        when(workflowManagementService.updateWorkflowStatus("workflow-1")).thenReturn(workflow);
        when(workflowManagementService.mapTaskTypeToStep(DeviceTask.TaskType.DOWNLOAD))
            .thenReturn("DOWNLOAD");
        when(workflowManagementService.getWorkflowsByBatchId("batch-001"))
            .thenReturn(Collections.singletonList(workflow));

        batchUpgradeService.retryWorkflow("batch-001", "device-1");

        verify(softwareDownloadService).executeScheduledTask(failedTask);
        assertEquals(DeviceTask.TaskStatus.PENDING, failedTask.getStatus());
    }

    private DeviceOperationStatus operationStatus(String deviceName, String upgradeState) {
        return DeviceOperationStatus.builder()
            .deviceName(deviceName)
            .softwareOperations(DeviceOperationStatus.SoftwareOperations.builder()
                .upgrade(DeviceOperationStatus.SoftwareOperations.SoftwareUpgrade.builder()
                    .state(upgradeState)
                    .build())
                .build())
            .build();
    }
}
