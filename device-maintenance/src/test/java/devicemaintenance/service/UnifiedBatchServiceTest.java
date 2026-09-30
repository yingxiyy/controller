package devicemaintenance.service;

import devicemaintenance.dto.BatchDetailDto;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.UpgradeWorkflow;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.UpgradeWorkflowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedBatchServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private DeviceTaskRepository deviceTaskRepository;

    @Mock
    private UpgradeWorkflowRepository upgradeWorkflowRepository;

    @Mock
    private WorkflowManagementService workflowManagementService;

    @Mock
    private BatchUpgradeService batchUpgradeService;

    @Mock
    private DeviceBackupService deviceBackupService;

    @Mock
    private DeviceRestoreService deviceRestoreService;

    @Mock
    private SoftwareDownloadService softwareDownloadService;

    @Mock
    private TaskInfoNotificationService taskInfoNotificationService;

    @InjectMocks
    private UnifiedBatchService unifiedBatchService;

    @Test
    void getBatchDetail_returnsRunning_whenAutomaticBatchHasMixedWorkflowSteps() {
        Batch batch = new Batch();
        batch.setBatchId("batch-001");
        batch.setBatchName("upgrade-batch");
        batch.setBatchType(Batch.BatchType.UPGRADE);
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setDeviceCount(2);
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
        batch.setRunningCount(2);
        batch.setCreatedTime(LocalDateTime.now());
        batch.setUpdatedTime(LocalDateTime.now());

        UpgradeWorkflow downloadWorkflow = new UpgradeWorkflow("wf-1", "batch-001", "device-1");
        downloadWorkflow.setCurrentStep("DOWNLOAD");

        UpgradeWorkflow backupWorkflow = new UpgradeWorkflow("wf-2", "batch-001", "device-2");
        backupWorkflow.setCurrentStep("BACKUP");

        doAnswer(invocation -> {
            for (UpgradeWorkflow workflow : invocation.<java.util.List<UpgradeWorkflow>>getArgument(0)) {
                workflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.WORKFLOW_PENDING);
            }
            return null;
        }).when(workflowManagementService).computeAndSetWorkflowStatuses(anyList());

        when(batchRepository.findById("batch-001")).thenReturn(Optional.of(batch));
        when(upgradeWorkflowRepository.findByBatchId("batch-001"))
            .thenReturn(Arrays.asList(downloadWorkflow, backupWorkflow));
        when(deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc("batch-001"))
            .thenReturn(Collections.emptyList());
        when(batchUpgradeService.calculateBatchStatusPublic(batch, Arrays.asList(downloadWorkflow, backupWorkflow)))
            .thenReturn(Batch.BatchStatus.RUNNING);

        BatchDetailDto.BatchDetailResponse detail = unifiedBatchService.getBatchDetail("batch-001");

        assertNotNull(detail);
        assertEquals("RUNNING", detail.getStatus());
        assertEquals(2, detail.getWorkflows().size());
        assertEquals("DOWNLOAD", detail.getWorkflows().get(0).getCurrentStep());
        assertEquals("BACKUP", detail.getWorkflows().get(1).getCurrentStep());
    }

    @Test
    void getBatchDetail_includesRetryDisplayStatusForWorkflowAndTask() {
        Batch batch = new Batch();
        batch.setBatchId("batch-002");
        batch.setBatchName("upgrade-batch");
        batch.setBatchType(Batch.BatchType.UPGRADE);
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setDeviceCount(1);
        batch.setCreatedTime(LocalDateTime.now());
        batch.setUpdatedTime(LocalDateTime.now());

        UpgradeWorkflow workflow = new UpgradeWorkflow("wf-1", "batch-002", "device-1");
        workflow.setCurrentStep("DOWNLOAD");
        workflow.setRetryCount(1);
        workflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_PENDING);

        DeviceTask task = new DeviceTask("task-1", "device-1", DeviceTask.TaskType.DOWNLOAD);
        task.setBatchId("batch-002");
        task.setStatus(DeviceTask.TaskStatus.PENDING);
        task.setRetryCount(1);

        doAnswer(invocation -> null)
            .when(workflowManagementService).computeAndSetWorkflowStatuses(anyList());

        when(batchRepository.findById("batch-002")).thenReturn(Optional.of(batch));
        when(upgradeWorkflowRepository.findByBatchId("batch-002"))
            .thenReturn(Collections.singletonList(workflow));
        when(deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc("batch-002"))
            .thenReturn(Collections.singletonList(task));
        when(batchUpgradeService.calculateBatchStatusPublic(batch, Collections.singletonList(workflow)))
            .thenReturn(Batch.BatchStatus.RUNNING);

        BatchDetailDto.BatchDetailResponse detail = unifiedBatchService.getBatchDetail("batch-002");

        assertNotNull(detail);
        assertEquals("RETRYING", detail.getWorkflows().get(0).getDisplayStatus());
        assertEquals("下载重试中", detail.getWorkflows().get(0).getStatusText());
        assertEquals("RETRYING", detail.getDeviceTasks().get(0).getDisplayStatus());
        assertEquals("下载重试中", detail.getDeviceTasks().get(0).getStatusText());
    }
    @Test
    void getBatchDetail_usesWorkflowStatusForStatusText_whenCurrentStepAlreadyAdvanced() {
        Batch batch = new Batch();
        batch.setBatchId("batch-003");
        batch.setBatchName("upgrade-batch");
        batch.setBatchType(Batch.BatchType.UPGRADE);
        batch.setStatus(Batch.BatchStatus.RUNNING);
        batch.setDeviceCount(1);
        batch.setCreatedTime(LocalDateTime.now());
        batch.setUpdatedTime(LocalDateTime.now());

        UpgradeWorkflow workflow = new UpgradeWorkflow("wf-1", "batch-003", "device-1");
        workflow.setCurrentStep("COMMIT");
        workflow.setComputedStatus(UpgradeWorkflow.WorkflowStatus.ACTIVATE_SUCCESS);

        DeviceTask task = new DeviceTask("task-1", "device-1", DeviceTask.TaskType.UPGRADE);
        task.setBatchId("batch-003");
        task.setStatus(DeviceTask.TaskStatus.COMPLETED);

        doAnswer(invocation -> null)
            .when(workflowManagementService).computeAndSetWorkflowStatuses(anyList());

        when(batchRepository.findById("batch-003")).thenReturn(Optional.of(batch));
        when(upgradeWorkflowRepository.findByBatchId("batch-003"))
            .thenReturn(Collections.singletonList(workflow));
        when(deviceTaskRepository.findByBatchIdOrderByCreatedTimeAsc("batch-003"))
            .thenReturn(Collections.singletonList(task));
        when(batchUpgradeService.calculateBatchStatusPublic(batch, Collections.singletonList(workflow)))
            .thenReturn(Batch.BatchStatus.READY_TO_COMMIT);

        BatchDetailDto.BatchDetailResponse detail = unifiedBatchService.getBatchDetail("batch-003");

        assertNotNull(detail);
        assertEquals("READY_TO_COMMIT", detail.getStatus());
        assertEquals(detail.getDeviceTasks().get(0).getStatusText(), detail.getWorkflows().get(0).getStatusText());
    }
}
