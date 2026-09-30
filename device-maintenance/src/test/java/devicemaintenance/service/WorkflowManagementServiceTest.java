package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.entity.UpgradeWorkflow;
import devicemaintenance.repository.DeviceTaskRepository;
import devicemaintenance.repository.UpgradeWorkflowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowManagementServiceTest {

    @Mock
    private UpgradeWorkflowRepository workflowRepository;

    @Mock
    private DeviceTaskRepository deviceTaskRepository;

    @InjectMocks
    private WorkflowManagementService workflowManagementService;

    @Test
    void updateWorkflowStatus_keepsCurrentStep_whenWorkflowFails() {
        UpgradeWorkflow workflow = new UpgradeWorkflow("wf-1", "batch-1", "device-1");
        workflow.setCurrentStep("DOWNLOAD");

        DeviceTask task = new DeviceTask("task-1", "device-1", DeviceTask.TaskType.DOWNLOAD);
        task.setBatchId("batch-1");
        task.setStatus(DeviceTask.TaskStatus.FAILED);

        when(workflowRepository.findById("wf-1")).thenReturn(Optional.of(workflow));
        when(deviceTaskRepository.findByBatchIdAndDeviceId("batch-1", "device-1"))
            .thenReturn(Collections.singletonList(task));
        when(workflowRepository.save(any(UpgradeWorkflow.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UpgradeWorkflow updated = workflowManagementService.updateWorkflowStatus("wf-1");

        assertEquals(UpgradeWorkflow.WorkflowStatus.DOWNLOAD_FAILED, updated.getStatus());
        assertEquals("DOWNLOAD", updated.getCurrentStep());
    }
}
