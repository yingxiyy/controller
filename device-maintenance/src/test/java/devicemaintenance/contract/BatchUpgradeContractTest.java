package devicemaintenance.contract;

import devicemaintenance.controller.BatchUpgradeController;
import devicemaintenance.dto.BatchUpgradeDto;
import devicemaintenance.service.BatchUpgradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BatchUpgradeContractTest {

    @Mock
    private BatchUpgradeService batchUpgradeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BatchUpgradeController controller = new BatchUpgradeController(batchUpgradeService);
        mockMvc = ContractTestSupport.standaloneMvc(controller);
    }

    @Test
    void createUpgradeBatch_success_contractLocked() throws Exception {
        BatchUpgradeDto.BatchUpgradeResponse response = new BatchUpgradeDto.BatchUpgradeResponse();
        response.setSuccess(true);
        response.setMessage("created");
        response.setBatchId("batch-001");
        response.setBatchName("upgrade-batch-20260302-001");
        response.setUpgradeFilePath("/software/v2.0.1/upgrade.pkg");
        response.setSftpServerName("192.168.3.206:22");
        response.setOperationInterval(1);
        response.setDeviceCount(2);
        response.setExecutionMode("MANUAL");
        response.setScheduledMode("IMMEDIATE");
        response.setScheduledTime(1700000000123L);
        response.setEnableDownload(true);
        response.setEnableBackup(true);
        response.setEnableUpgrade(true);
        response.setMaxRetryCount(3);
        response.setDebug(false);
        response.setCurrentStep("DOWNLOAD");
        response.setStatus("RUNNING");
        response.setSuccessCount(0);
        response.setFailedCount(0);
        response.setCreatedTime(LocalDateTime.of(2026, 3, 2, 10, 0, 0));
        response.setUpdatedTime(LocalDateTime.of(2026, 3, 2, 10, 0, 0));
        response.setStartedTime(LocalDateTime.of(2026, 3, 2, 10, 1, 0));
        response.setCompletedTime(LocalDateTime.of(2026, 3, 2, 10, 10, 0));

        when(batchUpgradeService.createBatchUpgrade(any(BatchUpgradeDto.CreateBatchUpgradeRequest.class)))
                .thenReturn(response);

        String requestBody = "{"
                + "\"batchName\":\"upgrade-batch-20260302-001\","
                + "\"upgradeFilePath\":\"/software/v2.0.1/upgrade.pkg\","
                + "\"deviceIds\":[\"Site-001#Ne-001\",\"Site-001#Ne-002\"],"
                + "\"executionMode\":\"MANUAL\","
                + "\"scheduledMode\":\"IMMEDIATE\""
                + "}";

        MvcResult result = mockMvc.perform(post("/restconf/operations/device-maintenance:create-upgrade-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Batch upgrade task created successfully"))
                .andExpect(jsonPath("$.data.batchId").value("batch-001"))
                .andExpect(jsonPath("$.data.batchName").value("upgrade-batch-20260302-001"))
                .andExpect(jsonPath("$.data.scheduledTime").value(1700000000123L))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andReturn();

        ContractTestSupport.assertSnapshot("create-upgrade-batch-success", result.getResponse().getContentAsString());
    }

    @Test
    void createUpgradeBatch_duplicateName_failureContractLocked() throws Exception {
        when(batchUpgradeService.createBatchUpgrade(any(BatchUpgradeDto.CreateBatchUpgradeRequest.class)))
                .thenThrow(new IllegalArgumentException("Batch name already exists: upgrade-batch-20260302-001"));

        String requestBody = "{"
                + "\"batchName\":\"upgrade-batch-20260302-001\","
                + "\"upgradeFilePath\":\"/software/v2.0.1/upgrade.pkg\","
                + "\"deviceIds\":[\"Site-001#Ne-001\"],"
                + "\"executionMode\":\"MANUAL\","
                + "\"scheduledMode\":\"IMMEDIATE\""
                + "}";

        MvcResult result = mockMvc.perform(post("/restconf/operations/device-maintenance:create-upgrade-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER: Batch name already exists: upgrade-batch-20260302-001"))
                .andExpect(jsonPath("$.message").value("Batch name already exists: upgrade-batch-20260302-001"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/restconf/operations/device-maintenance:create-upgrade-batch"))
                .andReturn();

        ContractTestSupport.assertSnapshot("create-upgrade-batch-duplicate-failure", result.getResponse().getContentAsString());
    }
}
