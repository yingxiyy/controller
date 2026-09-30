package devicemaintenance.service;

import devicemaintenance.entity.DeviceTask;
import devicemaintenance.repository.DeviceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceTaskUniquenessService {

    private final DeviceTaskRepository deviceTaskRepository;

    @Transactional
    public DeviceTask checkAndGetAvailableTask(String deviceId, DeviceTask.TaskType taskType, String currentBatchId) {
        log.info("[Task availability] deviceId={}, requestedTaskType={}", deviceId, getTaskTypeDisplayName(taskType));

        List<DeviceTask> allTasks = deviceTaskRepository.findByDeviceIdOrderByCreatedTimeDesc(deviceId).stream()
            .filter(task -> task.getTaskType() != null)
            .collect(Collectors.toList());

        if (allTasks.isEmpty()) {
            log.info("  Device has no task history; allow creating a new task");
            return null;
        }

        DeviceTask latestTask = allTasks.stream()
            .max(taskTimeComparator())
            .orElse(allTasks.get(0));

        log.info("  Latest device task: taskId={}, taskType={}, status={}",
            latestTask.getTaskId(), latestTask.getTaskType(), latestTask.getStatus());

        if (isActive(latestTask) && !isCurrentBatchWorkflowTask(latestTask, currentBatchId)) {
            throwDeviceBusy(latestTask);
        }

        DeviceTask currentBatchWorkflowTask = findLatestCurrentBatchWorkflowTask(deviceId, currentBatchId);
        if (currentBatchWorkflowTask != null) {
            DeviceTask reusableTask = resolveWorkflowTask(currentBatchWorkflowTask, taskType, currentBatchId);
            if (reusableTask != null) {
                return reusableTask;
            }
        }

        DeviceTask workflowTask = resolveWorkflowTask(latestTask, taskType, currentBatchId);
        if (workflowTask != null) {
            return workflowTask;
        }

        if (isActive(latestTask)) {
            throwDeviceBusy(latestTask);
        }

        log.info("  Device is idle; allow creating a new task. latestTaskType={}, latestStatus={}",
            getTaskTypeDisplayName(latestTask.getTaskType()), latestTask.getStatus());
        return null;
    }

    private DeviceTask findLatestCurrentBatchWorkflowTask(String deviceId, String currentBatchId) {
        if (!hasText(currentBatchId)) {
            return null;
        }

        return deviceTaskRepository.findByBatchIdAndDeviceId(currentBatchId, deviceId).stream()
            .filter(task -> task.getTaskType() != null)
            .filter(this::isWorkflowTask)
            .max(taskTimeComparator())
            .orElse(null);
    }

    private DeviceTask resolveWorkflowTask(DeviceTask task, DeviceTask.TaskType taskType, String currentBatchId) {
        if (!isWorkflowTask(task)) {
            return null;
        }

        DeviceTask.TaskStatus currentStatus = task.getStatus();
        if (currentStatus == DeviceTask.TaskStatus.NOT_START) {
            if (!isSameBatch(currentBatchId, task.getBatchId())) {
                log.info("  Workflow NOT_START task belongs to another batch; taskBatchId={}, currentBatchId={}",
                    task.getBatchId(), currentBatchId);
                return null;
            }
            if (task.getTaskType() == taskType) {
                log.info("  Reusing workflow NOT_START task: taskId={}", task.getTaskId());
                return task;
            }
            String errorMsg = String.format(
                "Device has a pending %s task in workflow, can only execute this type of operation (requested: %s)",
                getTaskTypeDisplayName(task.getTaskType()),
                getTaskTypeDisplayName(taskType)
            );
            log.warn("  Workflow NOT_START task type mismatch: {}", errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (currentStatus == DeviceTask.TaskStatus.COMPLETED ||
            currentStatus == DeviceTask.TaskStatus.FAILED ||
            currentStatus == DeviceTask.TaskStatus.PENDING ||
            currentStatus == DeviceTask.TaskStatus.SCHEDULED ||
            currentStatus == DeviceTask.TaskStatus.RUNNING) {
            if (!isSameBatch(currentBatchId, task.getBatchId())) {
                log.info("  Workflow task belongs to another batch; taskBatchId={}, currentBatchId={}",
                    task.getBatchId(), currentBatchId);
                return null;
            }
            log.info("  Reusing workflow task: taskId={}, status={}, {} -> {}",
                task.getTaskId(),
                currentStatus,
                getTaskTypeDisplayName(task.getTaskType()),
                getTaskTypeDisplayName(taskType));
            return task;
        }

        return null;
    }

    private Comparator<DeviceTask> taskTimeComparator() {
        return Comparator
            .comparing(DeviceTask::getUpdatedTime, Comparator.nullsFirst(LocalDateTime::compareTo))
            .thenComparing(DeviceTask::getCreatedTime, Comparator.nullsFirst(LocalDateTime::compareTo));
    }

    private boolean isWorkflowTask(DeviceTask task) {
        return hasText(task.getWorkflowId());
    }

    private boolean isCurrentBatchWorkflowTask(DeviceTask task, String currentBatchId) {
        return isWorkflowTask(task) && isSameBatch(currentBatchId, task.getBatchId());
    }

    private boolean isActive(DeviceTask task) {
        DeviceTask.TaskStatus status = task.getStatus();
        return status == DeviceTask.TaskStatus.PENDING ||
            status == DeviceTask.TaskStatus.SCHEDULED ||
            status == DeviceTask.TaskStatus.RUNNING;
    }

    private void throwDeviceBusy(DeviceTask task) {
        String deviceDisplayName = hasText(task.getDeviceName()) ? task.getDeviceName() : task.getDeviceId();
        String errorMsg = String.format(
            "Device '%s' is executing %s operation, please do not repeat. Current task status: %s, created at: %s",
            deviceDisplayName,
            getTaskTypeDisplayName(task.getTaskType()),
            task.getStatus(),
            task.getCreatedTime()
        );
        log.warn("  Device is busy: {}", errorMsg);
        throw new RuntimeException(errorMsg);
    }

    private boolean isSameBatch(String currentBatchId, String taskBatchId) {
        if (currentBatchId == null) {
            return true;
        }
        if (taskBatchId == null) {
            return false;
        }
        return currentBatchId.equals(taskBatchId);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String getTaskTypeDisplayName(DeviceTask.TaskType taskType) {
        if (taskType == null) {
            return "unknown";
        }
        switch (taskType) {
            case BACKUP:
                return "backup";
            case RESTORE:
                return "restore";
            case DOWNLOAD:
                return "download";
            case UPGRADE:
                return "upgrade";
            case COMMIT:
                return "commit";
            case ROLLBACK:
                return "rollback";
            default:
                return taskType.name();
        }
    }
}
