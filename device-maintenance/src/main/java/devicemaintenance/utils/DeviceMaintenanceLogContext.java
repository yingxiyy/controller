package devicemaintenance.utils;

import devicemaintenance.entity.DeviceTask;
import java.util.UUID;
import org.slf4j.MDC;

public final class DeviceMaintenanceLogContext {

    private static final String TRACE_ID = "traceId";
    private static final String TASK_ID = "taskId";
    private static final String BATCH_ID = "batchId";
    private static final String BATCH_NAME = "batchName";
    private static final String WORKFLOW_ID = "workflowId";
    private static final String DEVICE_ID = "deviceId";
    private static final String DEVICE_NAME = "deviceName";
    private static final String DEVICE_IP = "deviceIp";
    private static final String TASK_TYPE = "taskType";
    private static final String STEP = "step";
    private static final String STATUS = "status";
    private static final String PHASE = "phase";
    private static final String ACTION = "action";

    private DeviceMaintenanceLogContext() {
    }

    public static void ensureTraceId() {
        if (isBlank(MDC.get(TRACE_ID))) {
            MDC.put(TRACE_ID, UUID.randomUUID().toString());
        }
    }

    public static void setTaskContext(DeviceTask task) {
        ensureTraceId();
        put(TASK_ID, task != null ? task.getTaskId() : null);
        put(BATCH_ID, task != null ? task.getBatchId() : null);
        put(BATCH_NAME, task != null ? task.getBatchName() : null);
        put(WORKFLOW_ID, task != null ? task.getWorkflowId() : null);
        put(DEVICE_ID, task != null ? task.getDeviceId() : null);
        put(DEVICE_NAME, task != null ? task.getDeviceName() : null);
        put(DEVICE_IP, task != null ? task.getDeviceIp() : null);
        put(TASK_TYPE, task != null && task.getTaskType() != null ? task.getTaskType().name() : null);
        put(STEP, task != null && task.getTaskType() != null ? task.getTaskType().name() : null);
        put(STATUS, task != null && task.getStatus() != null ? task.getStatus().name() : null);
    }

    public static void setIdentifiers(String taskId, String batchId, String workflowId, String deviceId,
            String deviceIp, String taskType) {
        ensureTraceId();
        put(TASK_ID, taskId);
        put(BATCH_ID, batchId);
        put(WORKFLOW_ID, workflowId);
        put(DEVICE_ID, deviceId);
        put(DEVICE_IP, deviceIp);
        put(TASK_TYPE, taskType);
        put(STEP, taskType);
    }

    public static void setBatchContext(String batchId, String batchName, String taskType, String status) {
        ensureTraceId();
        put(BATCH_ID, batchId);
        put(BATCH_NAME, batchName);
        put(TASK_TYPE, taskType);
        put(STATUS, status);
    }

    public static void setDeviceContext(String deviceId, String deviceName, String deviceIp,
            String taskType, String step, String status) {
        ensureTraceId();
        put(DEVICE_ID, deviceId);
        put(DEVICE_NAME, deviceName);
        put(DEVICE_IP, deviceIp);
        put(TASK_TYPE, taskType);
        put(STEP, step);
        put(STATUS, status);
    }

    public static void setStatus(String status) {
        put(STATUS, status);
    }

    public static void setStep(String step) {
        put(STEP, step);
    }

    public static void setPhase(String phase, String action) {
        put(PHASE, phase);
        put(ACTION, action);
    }

    public static void clearPhase() {
        MDC.remove(PHASE);
        MDC.remove(ACTION);
    }

    public static void clearAll() {
        clearPhase();
        MDC.remove(TRACE_ID);
        MDC.remove(TASK_ID);
        MDC.remove(BATCH_ID);
        MDC.remove(BATCH_NAME);
        MDC.remove(WORKFLOW_ID);
        MDC.remove(DEVICE_ID);
        MDC.remove(DEVICE_NAME);
        MDC.remove(DEVICE_IP);
        MDC.remove(TASK_TYPE);
        MDC.remove(STEP);
        MDC.remove(STATUS);
    }

    private static void put(String key, String value) {
        if (isBlank(value)) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
