package devicemaintenance.service;

import devicemaintenance.dto.BatchDetailDto;
import devicemaintenance.utils.DeviceMaintenanceLogContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BatchLogService {

    private final UnifiedBatchService unifiedBatchService;

    public BatchLogService(@Lazy UnifiedBatchService unifiedBatchService) {
        this.unifiedBatchService = unifiedBatchService;
    }

    public void logBatchSnapshotByBatchId(String batchId, String trigger) {
        logBatchSnapshotByBatchId(batchId, trigger, null);
    }

    public void logBatchSnapshotByBatchId(String batchId, String trigger, String focusDeviceId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            return;
        }
        try {
            BatchDetailDto.BatchDetailResponse detail = unifiedBatchService.getBatchDetail(batchId);
            logBatchSnapshot(detail, trigger, focusDeviceId);
        } catch (Exception e) {
            log.warn("batch snapshot skipped trigger={} batchId={} reason={}", trigger, batchId, e.getMessage());
        }
    }

    public void logBatchSnapshot(BatchDetailDto.BatchDetailResponse detail, String trigger) {
        logBatchSnapshot(detail, trigger, null);
    }

    public void logBatchSnapshot(BatchDetailDto.BatchDetailResponse detail, String trigger, String focusDeviceId) {
        if (detail == null) {
            return;
        }

        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        DeviceMaintenanceLogContext.setBatchContext(
            detail.getBatchId(),
            detail.getBatchName(),
            detail.getTaskType(),
            detail.getStatus()
        );
        DeviceMaintenanceLogContext.setPhase("BATCH_SNAPSHOT", trigger);

        try {
            log.info("----------------------------------------------------------------");
            log.info("{} | batchId={} | batchName={} | type={} | status={} | devices={} | success={} | running={} | failed={} | trigger={}",
                LocalDateTime.now(),
                detail.getBatchId(),
                safe(detail.getBatchName()),
                detail.getBatchType(),
                detail.getStatus(),
                value(detail.getDeviceCount()),
                value(detail.getSuccessCount()),
                value(detail.getRunningCount()),
                value(detail.getFailedCount()),
                trigger);

            List<DeviceLogRow> rows = buildRows(detail);
            for (int i = 0; i < rows.size(); i++) {
                DeviceLogRow row = rows.get(i);
                DeviceMaintenanceLogContext.setDeviceContext(
                    row.deviceId,
                    row.deviceName,
                    row.deviceIp,
                    row.taskType,
                    row.currentStep,
                    row.status
                );
                String marker = Objects.equals(focusDeviceId, row.deviceId) ? "*" : " ";
                log.info("{} [{} / {}] deviceName={} | deviceId={} | ip={} | step={} | status={} | taskType={} | updated={}{}",
                    marker,
                    i + 1,
                    rows.size(),
                    safe(row.deviceName),
                    safe(row.deviceId),
                    safe(row.deviceIp),
                    safe(row.currentStep),
                    safe(row.status),
                    safe(row.taskType),
                    safe(row.updatedTime),
                    row.errorMessage == null ? "" : " | error=" + row.errorMessage);
            }
            log.info("----------------------------------------------------------------");
        } finally {
            restoreContext(previousContext);
        }
    }

    private List<DeviceLogRow> buildRows(BatchDetailDto.BatchDetailResponse detail) {
        Map<String, BatchDetailDto.WorkflowInfo> workflowByDevice = detail.getWorkflows() == null
            ? new LinkedHashMap<>()
            : detail.getWorkflows().stream().collect(Collectors.toMap(
                BatchDetailDto.WorkflowInfo::getDeviceId,
                workflow -> workflow,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        Map<String, List<BatchDetailDto.DeviceTaskInfo>> tasksByDevice = new LinkedHashMap<>();
        if (detail.getDeviceTasks() != null) {
            for (BatchDetailDto.DeviceTaskInfo task : detail.getDeviceTasks()) {
                tasksByDevice.computeIfAbsent(task.getDeviceId(), key -> new ArrayList<>()).add(task);
            }
        }

        List<String> orderedDeviceIds = new ArrayList<>();
        if (detail.getDeviceTasks() != null) {
            for (BatchDetailDto.DeviceTaskInfo task : detail.getDeviceTasks()) {
                if (!orderedDeviceIds.contains(task.getDeviceId())) {
                    orderedDeviceIds.add(task.getDeviceId());
                }
            }
        }
        for (String deviceId : workflowByDevice.keySet()) {
            if (!orderedDeviceIds.contains(deviceId)) {
                orderedDeviceIds.add(deviceId);
            }
        }

        List<DeviceLogRow> rows = new ArrayList<>();
        for (String deviceId : orderedDeviceIds) {
            BatchDetailDto.WorkflowInfo workflow = workflowByDevice.get(deviceId);
            List<BatchDetailDto.DeviceTaskInfo> taskList = tasksByDevice.getOrDefault(deviceId, new ArrayList<>());
            BatchDetailDto.DeviceTaskInfo latestTask = taskList.stream()
                .max(Comparator.comparing(this::lastTouchTime, Comparator.nullsLast(String::compareTo)))
                .orElse(null);

            DeviceLogRow row = new DeviceLogRow();
            row.deviceId = deviceId;
            row.deviceName = latestTask != null ? latestTask.getDeviceName() : null;
            row.deviceIp = latestTask != null ? latestTask.getDeviceIp() : null;
            row.taskType = latestTask != null ? latestTask.getTaskType() : null;
            row.currentStep = workflow != null && workflow.getCurrentStep() != null
                ? workflow.getCurrentStep()
                : (latestTask != null ? latestTask.getTaskType() : null);
            row.status = latestTask != null && latestTask.getStatusText() != null
                ? latestTask.getStatusText()
                : (latestTask != null && latestTask.getStatus() != null
                    ? latestTask.getStatus()
                    : workflowStatus(workflow));
            row.updatedTime = latestTask != null ? lastTouchTime(latestTask) : null;
            row.errorMessage = latestTask != null ? latestTask.getErrorMessage() : null;
            rows.add(row);
        }

        return rows;
    }

    private String workflowStatus(BatchDetailDto.WorkflowInfo workflow) {
        if (workflow == null) {
            return null;
        }
        return workflow.getStatusText() != null ? workflow.getStatusText() : workflow.getStatus();
    }

    private String lastTouchTime(BatchDetailDto.DeviceTaskInfo task) {
        if (task.getUpdatedTime() != null) {
            return task.getUpdatedTime().toString();
        }
        if (task.getCompletedTime() != null) {
            return task.getCompletedTime().toString();
        }
        if (task.getStartedTime() != null) {
            return task.getStartedTime().toString();
        }
        return task.getCreatedTime() != null ? task.getCreatedTime().toString() : null;
    }

    private String safe(Object value) {
        return value == null ? "na" : String.valueOf(value);
    }

    private String value(Integer value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private void restoreContext(Map<String, String> previousContext) {
        if (previousContext == null || previousContext.isEmpty()) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(previousContext);
    }

    private static class DeviceLogRow {
        private String deviceId;
        private String deviceName;
        private String deviceIp;
        private String taskType;
        private String currentStep;
        private String status;
        private String updatedTime;
        private String errorMessage;
    }
}
