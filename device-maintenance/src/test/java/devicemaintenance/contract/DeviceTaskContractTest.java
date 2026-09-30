package devicemaintenance.contract;

import devicemaintenance.controller.DeviceTaskController;
import devicemaintenance.dto.GetDeviceTasksRequest;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.repository.BatchRepository;
import devicemaintenance.repository.DeviceTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeviceTaskContractTest {

    @Mock
    private DeviceTaskRepository deviceTaskRepository;

    @Mock
    private BatchRepository batchRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DeviceTaskController controller = new DeviceTaskController(deviceTaskRepository, batchRepository);
        mockMvc = ContractTestSupport.standaloneMvc(controller);
    }

    @Test
    void getDeviceTasks_success_contractLocked() throws Exception {
        DeviceTask task = new DeviceTask("task-001", "Site-001#Ne-001", DeviceTask.TaskType.BACKUP);
        task.setBatchId("batch-001");
        task.setBatchName("backup-batch");
        task.setWorkflowId("wf-001");
        task.setDeviceName("Node-1");
        task.setDeviceIp("10.0.0.1");
        task.setVendorType("CHASSIS");
        task.setVendorName("COHERENT");
        task.setSftpServerName("192.168.3.206:22");
        task.setStatus(DeviceTask.TaskStatus.RUNNING);
        task.setFilePath("/software/v2.0.1/upgrade.pkg");
        task.setBackupFilePath("/backup/node1/20260302/100000.db");
        task.setBackupFileName("100000.db");
        task.setCurrentVersion("v1.0.0");
        task.setTargetVersion("v2.0.1");
        task.setRetryCount(1);
        task.setErrorMessage("");
        task.setCreatedTime(LocalDateTime.of(2026, 3, 2, 10, 0, 0));
        task.setUpdatedTime(LocalDateTime.of(2026, 3, 2, 10, 5, 0));
        task.setStartedTime(LocalDateTime.of(2026, 3, 2, 10, 1, 0));
        task.setCompletedTime(LocalDateTime.of(2026, 3, 2, 10, 8, 0));
        task.setDebugPayload(null); // explicitly lock null output behavior
        task.setScheduledTime(1700000000123L); // lock long precision behavior

        when(deviceTaskRepository.findAllByOrderByCreatedTimeDesc()).thenReturn(Collections.singletonList(task));

        MvcResult result = mockMvc.perform(post("/restconf/operations/device-maintenance:get-device-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":0,\"size\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tasks[0].taskId").value("task-001"))
                .andExpect(jsonPath("$.data.tasks[0].status").value("RUNNING"))
                .andExpect(jsonPath("$.data.tasks[0].debugPayload").value(nullValue()))
                .andExpect(jsonPath("$.data.tasks[0].scheduledTime").value(1700000000123L))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andReturn();

        ContractTestSupport.assertSnapshot("get-device-tasks-success", result.getResponse().getContentAsString());
    }

    @Test
    void getDeviceTasks_upgradeBatch_collapsesHistoricalStepTasksByDevice() throws Exception {
        Batch batch = new Batch();
        batch.setBatchId("batch-upgrade-001");
        batch.setBatchType(Batch.BatchType.UPGRADE);

        DeviceTask backupTask = new DeviceTask("task-backup-001", "device-001", DeviceTask.TaskType.BACKUP);
        backupTask.setBatchId("batch-upgrade-001");
        backupTask.setWorkflowId("wf-001");
        backupTask.setDeviceName("Node-1");
        backupTask.setDeviceIp("10.0.0.1");
        backupTask.setStatus(DeviceTask.TaskStatus.RUNNING);
        backupTask.setBackupFilePath("/backup/20260703");
        backupTask.setCurrentVersion("POS_3.6.1.3");
        backupTask.setPreviousVersion("POS_3.6.1.3");
        backupTask.setCreatedTime(LocalDateTime.of(2026, 7, 3, 9, 57, 32));
        backupTask.setUpdatedTime(LocalDateTime.of(2026, 7, 3, 9, 57, 32));

        DeviceTask downloadTask = new DeviceTask("task-download-001", "device-001", DeviceTask.TaskType.DOWNLOAD);
        downloadTask.setBatchId("batch-upgrade-001");
        downloadTask.setWorkflowId("wf-001");
        downloadTask.setDeviceName("Node-1");
        downloadTask.setDeviceIp("10.0.0.1");
        downloadTask.setStatus(DeviceTask.TaskStatus.COMPLETED);
        downloadTask.setFilePath("/upload/deviceSoftware/Release_POS_3.6.2.1.32_20260624.tar");
        downloadTask.setCurrentVersion("POS_3.6.1.3");
        downloadTask.setPreviousVersion("POS_3.6.1.3");
        downloadTask.setTargetVersion("POS_3.6.2.1.32");
        downloadTask.setCreatedTime(LocalDateTime.of(2026, 7, 2, 17, 52, 36));
        downloadTask.setUpdatedTime(LocalDateTime.of(2026, 7, 2, 17, 52, 36));

        when(batchRepository.findById("batch-upgrade-001")).thenReturn(Optional.of(batch));
        when(deviceTaskRepository.findByBatchIdOrderByCreatedTimeDesc("batch-upgrade-001"))
                .thenReturn(Arrays.asList(backupTask, downloadTask));

        mockMvc.perform(post("/restconf/operations/device-maintenance:get-device-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":\"batch-upgrade-001\",\"page\":0,\"size\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.tasks[0].taskId").value("task-backup-001"))
                .andExpect(jsonPath("$.data.tasks[0].taskType").value("BACKUP"))
                .andExpect(jsonPath("$.data.tasks[0].status").value("RUNNING"))
                .andExpect(jsonPath("$.data.tasks[0].filePath")
                        .value("/upload/deviceSoftware/Release_POS_3.6.2.1.32_20260624.tar"))
                .andExpect(jsonPath("$.data.tasks[0].targetVersion").value("POS_3.6.2.1.32"));
    }

    @Test
    void cancelDeviceTask_missingTaskId_failureContractLocked() throws Exception {
        MvcResult result = mockMvc.perform(post("/restconf/operations/device-maintenance:cancel-device-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER: taskId cannot be empty"))
                .andExpect(jsonPath("$.message").value("taskId cannot be empty"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/restconf/operations/device-maintenance:cancel-device-task"))
                .andReturn();

        ContractTestSupport.assertSnapshot("cancel-device-task-missing-taskid-failure", result.getResponse().getContentAsString());
    }

    @Test
    void deleteDeviceTask_success_contractLocked() throws Exception {
        DeviceTask task = new DeviceTask("task-completed-001", "Site-001#Ne-001", DeviceTask.TaskType.BACKUP);
        task.setStatus(DeviceTask.TaskStatus.COMPLETED);

        when(deviceTaskRepository.findById("task-completed-001")).thenReturn(Optional.of(task));
        doNothing().when(deviceTaskRepository).delete(task);

        MvcResult result = mockMvc.perform(post("/restconf/operations/device-maintenance:delete-device-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"task-completed-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task deleted successfully"))
                .andExpect(jsonPath("$.data").value("task-completed-001"))
                .andReturn();

        ContractTestSupport.assertSnapshot("delete-device-task-success", result.getResponse().getContentAsString());
    }

    @Test
    void deleteDeviceTask_upgradeWorkflowTask_rejected() throws Exception {
        DeviceTask task = new DeviceTask("task-upgrade-001", "Site-001#Ne-001", DeviceTask.TaskType.DOWNLOAD);
        task.setStatus(DeviceTask.TaskStatus.FAILED);
        task.setBatchId("batch-upgrade-001");
        task.setWorkflowId("wf-001");

        when(deviceTaskRepository.findById("task-upgrade-001")).thenReturn(Optional.of(task));

        mockMvc.perform(post("/restconf/operations/device-maintenance:delete-device-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"task-upgrade-001\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(
                        "OPERATION_NOT_ALLOWED: Workflow task cannot be deleted directly, please use remove-upgrade-workflow instead"))
                .andExpect(jsonPath("$.message").value(
                        "Workflow task cannot be deleted directly, please use remove-upgrade-workflow instead"));
    }
}
