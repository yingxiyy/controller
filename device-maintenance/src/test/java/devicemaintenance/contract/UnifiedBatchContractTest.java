package devicemaintenance.contract;

import devicemaintenance.controller.UnifiedBatchController;
import devicemaintenance.dto.BatchDetailDto;
import devicemaintenance.service.BatchLogService;
import devicemaintenance.service.UnifiedBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UnifiedBatchContractTest {

    @Mock
    private UnifiedBatchService unifiedBatchService;

    @Mock
    private BatchLogService batchLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UnifiedBatchController controller = new UnifiedBatchController(unifiedBatchService, batchLogService);
        mockMvc = ContractTestSupport.standaloneMvc(controller);
    }

    @Test
    void getBatchDetail_success_contractLocked() throws Exception {
        BatchDetailDto.BatchDetailResponse response = new BatchDetailDto.BatchDetailResponse();
        response.setBatchId("batch-001");
        response.setBatchName("upgrade-batch-20260302-001");
        response.setBatchType("UPGRADE");
        response.setTaskType("BATCH_UPGRADE");
        response.setStatus("RUNNING");
        response.setDeviceCount(2);
        response.setSuccessCount(0);
        response.setFailedCount(0);
        response.setRunningCount(2);
        response.setCreatedTime(LocalDateTime.of(2026, 3, 2, 10, 0, 0));
        response.setUpdatedTime(LocalDateTime.of(2026, 3, 2, 10, 5, 0));
        response.setStartedTime(LocalDateTime.of(2026, 3, 2, 10, 1, 0));
        response.setCompletedTime(LocalDateTime.of(2026, 3, 2, 10, 10, 0));
        response.setCreatedBy("bill");
        response.setExecutionMode("MANUAL");
        response.setScheduledMode("IMMEDIATE");
        response.setEnableDownload(true);
        response.setEnableBackup(true);
        response.setEnableUpgrade(true);
        response.setMaxRetryCount(3);
        response.setFilePath("/software/v2.0.1/upgrade.pkg");
        response.setBasePath("/backup");
        response.setBackupBasePath("/backup/upgrade");
        response.setSftpServerName("192.168.3.206:22");
        response.setOperationInterval(1);
        response.setRemark("remark");
        response.setErrorMessage("");
        response.setDetail("{}");
        response.setWorkflows(Collections.emptyList());
        response.setDeviceTasks(Collections.emptyList());
        response.setIsWorkflowBatch(true);
        response.setScheduledTime(1700000000123L); // lock long precision behavior

        when(unifiedBatchService.getBatchDetail("batch-001")).thenReturn(response);

        MvcResult result = mockMvc.perform(post("/restconf/operations/device-maintenance:get-batch-detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":\"batch-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Batch detail retrieved successfully"))
                .andExpect(jsonPath("$.data.batchId").value("batch-001"))
                .andExpect(jsonPath("$.data.batchType").value("UPGRADE"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.isWorkflowBatch").value(true))
                .andExpect(jsonPath("$.data.scheduledTime").value(1700000000123L))
                .andExpect(jsonPath("$.timestamp").isString())
                .andReturn();

        ContractTestSupport.assertSnapshot("get-batch-detail-success", result.getResponse().getContentAsString());
    }

    @Test
    void getBatchDetail_missingBatchId_failureContractLocked() throws Exception {
        MvcResult result = mockMvc.perform(post("/restconf/operations/device-maintenance:get-batch-detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER: batchId cannot be empty"))
                .andExpect(jsonPath("$.message").value("batchId cannot be empty"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/restconf/operations/device-maintenance:get-batch-detail"))
                .andReturn();

        ContractTestSupport.assertSnapshot("get-batch-detail-missing-batchid-failure", result.getResponse().getContentAsString());
    }
}
