package devicemaintenance.controller;

import devicemaintenance.dto.SoftwareDownloadDto;
import devicemaintenance.dto.GetDeviceTasksRequest;
import devicemaintenance.entity.Batch;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.exception.InvalidParameterException;
import devicemaintenance.exception.OperationNotAllowedException;
import devicemaintenance.exception.ResourceNotFoundException;
import devicemaintenance.integration.TaskInfoNotificationService;
import devicemaintenance.repository.DeviceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备任务通用控制器
 * 提供所有类型设备任务的查询功能
 */
@RestController
@RequestMapping(value = "/restconf/operations", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@RequiredArgsConstructor
public class DeviceTaskController {

    private final DeviceTaskRepository deviceTaskRepository;
    private final devicemaintenance.repository.BatchRepository batchRepository;
    
    // TaskInfo集成（可选）
    @Autowired(required = false)
    private TaskInfoNotificationService taskInfoNotificationService;

    /**
     * 获取所有设备任务列表
     */
    @PostMapping(value = "/device-maintenance:get-device-tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DeviceTaskListResponse>> getAllDeviceTasks(
            @RequestBody(required = false) GetDeviceTasksRequest request) {
        
        // 处理空请求
        if (request == null) {
            request = new GetDeviceTasksRequest();
        }
        
        String deviceId = request.getDeviceId();
        String batchId = request.getBatchId();
        String taskType = request.getTaskType();
        String status = request.getStatus();
        Integer page = request.getPage() != null ? request.getPage() : 0;
        Integer size = request.getSize() != null ? request.getSize() : 20;
        
        log.info("获取设备任务列表: deviceId={}, batchId={}, taskType={}, status={}, page={}, size={}", 
                 deviceId, batchId, taskType, status, page, size);
        
        List<DeviceTask> tasks;
        
        // 根据条件查询 - 检查非空且非空白字符串
        boolean hasTaskType = taskType != null && !taskType.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty();
        boolean hasDeviceId = deviceId != null && !deviceId.trim().isEmpty();
        boolean hasBatchId = batchId != null && !batchId.trim().isEmpty();
        
        // ⭐ 优先处理 batchId 过滤
        if (hasBatchId) {
            tasks = deviceTaskRepository.findByBatchIdOrderByCreatedTimeDesc(batchId);
            if (isUpgradeBatch(batchId)) {
                tasks = collapseUpgradeBatchTasksByDevice(tasks);
            }
        } else if (hasTaskType && hasStatus && hasDeviceId) {
            // 三个条件都有
            DeviceTask.TaskType taskTypeEnum = DeviceTask.TaskType.valueOf(taskType);
            DeviceTask.TaskStatus statusEnum = DeviceTask.TaskStatus.valueOf(status);
            tasks = deviceTaskRepository.findByDeviceIdAndTaskTypeAndStatus(
                deviceId, taskTypeEnum, statusEnum);
        } else if (hasTaskType && hasDeviceId) {
            // 任务类型和设备ID
            DeviceTask.TaskType taskTypeEnum = DeviceTask.TaskType.valueOf(taskType);
            tasks = deviceTaskRepository.findByDeviceIdAndTaskType(deviceId, taskTypeEnum);
        } else if (hasTaskType && hasStatus) {
            // 任务类型和状态
            DeviceTask.TaskType taskTypeEnum = DeviceTask.TaskType.valueOf(taskType);
            DeviceTask.TaskStatus statusEnum = DeviceTask.TaskStatus.valueOf(status);
            tasks = deviceTaskRepository.findByTaskTypeAndStatus(taskTypeEnum, statusEnum);
        } else if (hasTaskType) {
            // 只有任务类型
            DeviceTask.TaskType taskTypeEnum = DeviceTask.TaskType.valueOf(taskType);
            tasks = deviceTaskRepository.findByTaskTypeOrderByCreatedTimeDesc(taskTypeEnum);
        } else if (hasDeviceId) {
            // 只有设备ID - 过滤掉 IDLE 状态的空任务
            tasks = deviceTaskRepository.findByDeviceIdOrderByCreatedTimeDesc(deviceId).stream()
                .filter(task -> task.getTaskType() != null)
                .collect(Collectors.toList());
        } else if (hasStatus) {
            // 只有状态
            DeviceTask.TaskStatus statusEnum = DeviceTask.TaskStatus.valueOf(status);
            tasks = deviceTaskRepository.findByStatusOrderByCreatedTimeDesc(statusEnum);
        } else {
            // 获取所有任务 - 过滤掉 IDLE 状态的空任务
            tasks = deviceTaskRepository.findAllByOrderByCreatedTimeDesc().stream()
                .filter(task -> task.getTaskType() != null)
                .collect(Collectors.toList());
        }

        // ⭐ 记录总数（分页前）
        int totalCount = tasks.size();
        
        // 简单分页
        int start = page * size;
        int end = Math.min(start + size, tasks.size());
        List<DeviceTask> pagedTasks = tasks.subList(Math.min(start, tasks.size()), end);
        
        // 转换为响应DTO
        List<DeviceTaskResponse> responses = pagedTasks.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        // ⭐ 构建新的响应格式（包含总数）
        DeviceTaskListResponse listResponse = new DeviceTaskListResponse();
        listResponse.setTasks(responses);
        listResponse.setTotalCount(totalCount);
        listResponse.setPage(page);
        listResponse.setSize(size);
        
        log.info("获取到{}个设备任务，返回第{}页，每页{}个", totalCount, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(
            String.format("Query successful, found %d task(s)", totalCount),
            listResponse
        ));
    }

    /**
     * 通过设备ID获取该设备的所有任务
     * POST /restconf/operations/device-maintenance:get-tasks-by-device-id
     * 
     * 功能说明：
     * - 查询指定设备的所有 DeviceTask 记录
     * - 按创建时间倒序排列（最新的在前）
     * - 自动过滤掉 taskType 为空的记录
     * - 支持分页
     */
    @PostMapping(value = "/device-maintenance:get-tasks-by-device-id", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DeviceTaskListResponse>> getTasksByDeviceId(
            @RequestBody GetTasksByDeviceIdRequest request) {

        if (request == null || request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            log.warn("Get tasks by device ID failed: deviceId is empty");
            throw new InvalidParameterException("deviceId cannot be empty");
        }

        String deviceId = request.getDeviceId().trim();
        Integer page = request.getPage() != null ? request.getPage() : 0;
        Integer size = request.getSize() != null ? request.getSize() : 20;
        
        log.info("Get tasks by device ID: deviceId={}, page={}, size={}", deviceId, page, size);

        // 查询该设备的所有任务（按创建时间倒序）
        List<DeviceTask> tasks = deviceTaskRepository.findByDeviceIdOrderByCreatedTimeDesc(deviceId).stream()
            .filter(task -> task.getTaskType() != null)  // 过滤掉空任务
            .collect(Collectors.toList());

        // 记录总数（分页前）
        int totalCount = tasks.size();
        
        // 简单分页
        int start = page * size;
        int end = Math.min(start + size, tasks.size());
        List<DeviceTask> pagedTasks = tasks.subList(Math.min(start, tasks.size()), end);
        
        // 转换为响应DTO
        List<DeviceTaskResponse> responses = pagedTasks.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        // 构建响应
        DeviceTaskListResponse listResponse = new DeviceTaskListResponse();
        listResponse.setTasks(responses);
        listResponse.setTotalCount(totalCount);
        listResponse.setPage(page);
        listResponse.setSize(size);
        
        log.info("Found {} tasks for device {}, returning page {} with {} items", 
                 totalCount, deviceId, page, responses.size());
        
        return ResponseEntity.ok(ApiResponse.success(
            String.format("Found %d task(s) for device", totalCount),
            listResponse
        ));
    }

    /**
     * 通过设备ID列表获取任务列表
     * POST /restconf/operations/device-maintenance:get-tasks-by-device-ids
     *
     * 功能说明：
     * - 查询指定设备ID列表的所有 DeviceTask 记录
     * - 按创建时间倒序排列（最新的在前）
     * - 自动过滤掉 taskType 为空的记录
     * - 包含 batchId 属性
     */
    @PostMapping(value = "/device-maintenance:get-tasks-by-device-ids", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DeviceTaskListResponse>> getTasksByDeviceIds(
            @RequestBody GetTasksByDeviceIdsRequest request) {

        if (request == null || request.getDeviceIds() == null || request.getDeviceIds().isEmpty()) {
            log.warn("Get tasks by device IDs failed: deviceIds is empty");
            throw new InvalidParameterException("deviceIds cannot be empty");
        }

        List<String> deviceIds = request.getDeviceIds().stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());

        if (deviceIds.isEmpty()) {
            log.warn("Get tasks by device IDs failed: all deviceIds are empty");
            throw new InvalidParameterException("deviceIds cannot contain all empty strings");
        }

        log.info("Get tasks by device IDs: deviceIds={}", deviceIds);

        // 查询设备ID列表的所有任务（按创建时间倒序）
        List<DeviceTask> tasks = deviceTaskRepository.findByDeviceIdInOrderByCreatedTimeDesc(deviceIds).stream()
                .filter(task -> task.getTaskType() != null)  // 过滤掉空任务
                .collect(Collectors.toList());

        // 转换为响应DTO
        List<DeviceTaskResponse> responses = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 构建响应
        DeviceTaskListResponse listResponse = new DeviceTaskListResponse();
        listResponse.setTasks(responses);
        listResponse.setTotalCount(tasks.size());
        listResponse.setPage(0);
        listResponse.setSize(tasks.size());

        log.info("Found {} tasks for devices {}", tasks.size(), deviceIds);

        return ResponseEntity.ok(ApiResponse.success(
            String.format("Found %d task(s) for devices", tasks.size()),
            listResponse
        ));
    }

    /**
     * 获取设备任务详情
     */
    @PostMapping(value = "/device-maintenance:get-device-task-detail", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DeviceTaskResponse>> getDeviceTaskDetail(
            @RequestBody(required = false) DeviceTaskDetailRequest request) {

        if (request == null || request.getTaskId() == null || request.getTaskId().trim().isEmpty()) {
            log.warn("获取设备任务详情失败: taskId 为空");
            throw new InvalidParameterException("taskId cannot be empty");
        }

        String taskId = request.getTaskId().trim();
        log.info("获取设备任务详情: taskId={}", taskId);

        Optional<DeviceTask> taskOpt = deviceTaskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new ResourceNotFoundException("Task does not exist: " + taskId);
        }
        
        DeviceTask task = taskOpt.get();
        DeviceTaskResponse response = convertToResponse(task);
        
        return jsonResponse(HttpStatus.OK,
                ApiResponse.success("Task detail retrieved successfully", response));
    }

    /**
     * 取消设备任务
     * 只能取消处于 SCHEDULED、PENDING 或 RUNNING 状态的任务
     */
    @PostMapping(value = "/device-maintenance:cancel-device-task", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> cancelDeviceTask(
            @RequestBody(required = false) CancelTaskRequest request) {

        if (request == null || request.getTaskId() == null || request.getTaskId().trim().isEmpty()) {
            log.warn("取消任务失败: taskId 为空");
            throw new InvalidParameterException("taskId cannot be empty");
        }

        String taskId = request.getTaskId().trim();
        log.info("取消设备任务: taskId={}", taskId);

        Optional<DeviceTask> taskOpt = deviceTaskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new ResourceNotFoundException("Task does not exist: " + taskId);
        }

        DeviceTask task = taskOpt.get();
        DeviceTask.TaskStatus currentStatus = task.getStatus();


        // 检查任务状态是否可以取消
        if (currentStatus == DeviceTask.TaskStatus.COMPLETED) {
            throw new OperationNotAllowedException("Task is completed and cannot be cancelled");
        }
        if (currentStatus == DeviceTask.TaskStatus.FAILED) {
            throw new OperationNotAllowedException("Task has already failed, no need to cancel");
        }
        if (currentStatus == DeviceTask.TaskStatus.CANCELLED) {
            throw new OperationNotAllowedException("Task has been cancelled");
        }

        // ✅ 可以取消的状态: NOT_START, SCHEDULED, PENDING, RUNNING
        task.setStatus(DeviceTask.TaskStatus.CANCELLED);
        task.setErrorMessage("Task cancelled by user");
        task.setUpdatedTime(java.time.LocalDateTime.now());
        deviceTaskRepository.save(task);

        log.info("任务取消成功: taskId={}, 原状态={}", taskId, currentStatus);
        return jsonResponse(HttpStatus.OK,
                ApiResponse.success("Task cancelled successfully", taskId));
    }

    /**
     * 删除设备任务
     * 只能删除处于 COMPLETED、FAILED、CANCELLED 或 EXPIRED 状态的任务
     * 
     * ⚠️ 一对多架构注意：删除会永久移除历史记录（包括 debugPayload）
     */
    @PostMapping(value = "/device-maintenance:delete-device-task", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> deleteDeviceTask(
            @RequestBody(required = false) DeleteTaskRequest request) {

        if (request == null || request.getTaskId() == null || request.getTaskId().trim().isEmpty()) {
            log.warn("删除任务失败: taskId 为空");
            throw new InvalidParameterException("taskId cannot be empty");
        }

        String taskId = request.getTaskId().trim();
        log.info("删除设备任务: taskId={}", taskId);

        Optional<DeviceTask> taskOpt = deviceTaskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new ResourceNotFoundException("Task does not exist: " + taskId);
        }

        DeviceTask task = taskOpt.get();
        DeviceTask.TaskStatus currentStatus = task.getStatus();

        // 检查任务状态是否可以删除
        if (task.getWorkflowId() != null && !task.getWorkflowId().trim().isEmpty()) {
            throw new OperationNotAllowedException(
                    "Workflow task cannot be deleted directly, please use remove-upgrade-workflow instead");
        }
        if (task.getBatchId() != null && !task.getBatchId().trim().isEmpty()) {
            batchRepository.findById(task.getBatchId())
                    .filter(batch -> batch.getBatchType() == devicemaintenance.entity.Batch.BatchType.UPGRADE)
                    .ifPresent(batch -> {
                        throw new OperationNotAllowedException(
                                "Upgrade batch task cannot be deleted directly, please use remove-upgrade-workflow instead");
                    });
        }

        if (currentStatus == DeviceTask.TaskStatus.NOT_START) {
            throw new OperationNotAllowedException("Task not started cannot be deleted, please cancel it first");
        }
        if (currentStatus == DeviceTask.TaskStatus.SCHEDULED) {
            throw new OperationNotAllowedException("Scheduled task cannot be deleted, please cancel it first");
        }
        if (currentStatus == DeviceTask.TaskStatus.PENDING) {
            throw new OperationNotAllowedException("Pending task cannot be deleted, please cancel it first");
        }
        if (currentStatus == DeviceTask.TaskStatus.RUNNING) {
            throw new OperationNotAllowedException("Running task cannot be deleted, please cancel it first");
        }

        // ✅ 可以删除的状态: COMPLETED, FAILED, CANCELLED, EXPIRED, IDLE
        
        // 从数据库删除任务
        deviceTaskRepository.delete(task);
        
        log.info("任务删除成功: taskId={}, 状态={}", taskId, currentStatus);
        return jsonResponse(HttpStatus.OK,
                ApiResponse.success("Task deleted successfully", taskId));
    }

    /**
     * 根据设备ID列表获取活跃的批次ID列表
     * POST /restconf/operations/device-maintenance:get-active-batchids-by-deviceids
     *
     * 功能说明：
     * - 查询指定设备ID列表关联的所有活跃批次
     * - 活跃批次：非 COMPLETED、COMPLETED_WITH_ERRORS、FAILED、CANCELLED 状态的批次
     * - 返回去重后的批次ID列表
     */
    @PostMapping(value = "/device-maintenance:get-active-batchids-by-deviceids", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<GetActiveBatchIdsResponse>> getActiveBatchIdsByDeviceIds(
            @RequestBody GetActiveBatchIdsByDeviceIdsRequest request) {

        if (request == null || request.getDeviceIds() == null || request.getDeviceIds().isEmpty()) {
            log.warn("Get active batch IDs by device IDs failed: deviceIds is empty");
            throw new InvalidParameterException("deviceIds cannot be empty");
        }

        List<String> deviceIds = request.getDeviceIds().stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());

        if (deviceIds.isEmpty()) {
            log.warn("Get active batch IDs by device IDs failed: all deviceIds are empty");
            throw new InvalidParameterException("deviceIds cannot contain all empty strings");
        }

        log.info("Get active batch IDs by device IDs: deviceIds={}", deviceIds);

        // 查询设备ID列表的所有任务
        List<DeviceTask> tasks = deviceTaskRepository.findByDeviceIdInOrderByCreatedTimeDesc(deviceIds).stream()
                .filter(task -> task.getTaskType() != null && task.getBatchId() != null)  // 过滤掉空任务和没有批次的任务
                .collect(Collectors.toList());

        log.info("Found {} tasks for devices", tasks.size());

        // 提取所有批次ID（去重）
        Set<String> batchIds = tasks.stream()
                .map(DeviceTask::getBatchId)
                .collect(Collectors.toSet());

        log.info("Found {} unique batch IDs", batchIds.size());

        // 查询所有批次，过滤出活跃批次（使用批量查询避免 N+1 问题）
        List<String> activeBatchIds = batchRepository.findAllById(batchIds).stream()
                .filter(batch -> {
                    // 活跃批次：非终态的批次
                    devicemaintenance.entity.Batch.BatchStatus status = batch.getStatus();
                    return status != devicemaintenance.entity.Batch.BatchStatus.COMPLETED
                        && status != devicemaintenance.entity.Batch.BatchStatus.COMPLETED_WITH_ERRORS
                        && status != devicemaintenance.entity.Batch.BatchStatus.FAILED
                        && status != devicemaintenance.entity.Batch.BatchStatus.CANCELLED;
                })
                .map(devicemaintenance.entity.Batch::getBatchId)
                .collect(Collectors.toList());

        log.info("Found {} active batch IDs: {}", activeBatchIds.size(), activeBatchIds);

        // 构建响应
        GetActiveBatchIdsResponse response = new GetActiveBatchIdsResponse();
        response.setBatchIds(activeBatchIds);

        return ResponseEntity.ok(ApiResponse.success(
            String.format("Found %d active batch(es)", activeBatchIds.size()),
            response
        ));
    }

    /**
     * 取消所有正在进行的任务（测试辅助接口）
     * 批量取消所有 NOT_START/SCHEDULED/PENDING/RUNNING 状态的任务
     */
    @PostMapping(value = "/device-maintenance:cancel-all-tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CancelAllResult>> cancelAllTasks() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🗑️ 取消所有正在进行的任务（测试辅助接口）");

        // 查找所有可取消的任务
        List<DeviceTask> allTasks = deviceTaskRepository.findAll();
        List<DeviceTask> cancellableTasks = allTasks.stream()
                .filter(task -> {
                    DeviceTask.TaskStatus status = task.getStatus();
                    return status == DeviceTask.TaskStatus.NOT_START
                        || status == DeviceTask.TaskStatus.SCHEDULED
                        || status == DeviceTask.TaskStatus.PENDING
                        || status == DeviceTask.TaskStatus.RUNNING;
                })
                .collect(Collectors.toList());

        log.info("📊 找到 {} 个可取消的任务", cancellableTasks.size());

        int successCount = 0;
        int failedCount = 0;

        for (DeviceTask task : cancellableTasks) {
            try {
                task.setStatus(DeviceTask.TaskStatus.CANCELLED);
                deviceTaskRepository.save(task);
                successCount++;
                log.info("  ✅ 已取消: taskId={}, type={}, status={}", 
                    task.getTaskId(), task.getTaskType(), task.getStatus());
            } catch (Exception e) {
                failedCount++;
                log.error("  ❌ 取消失败: taskId={}, error={}", task.getTaskId(), e.getMessage());
            }
        }

        CancelAllResult result = new CancelAllResult();
        result.setTotalFound(cancellableTasks.size());
        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);

        log.info("✅ 批量取消完成: 成功={}, 失败={}", successCount, failedCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return jsonResponse(HttpStatus.OK,
                ApiResponse.success("Batch cancel completed", result));
    }

    /**
     * 删除所有可删除的任务（测试辅助接口）
     * 批量删除所有 COMPLETED/FAILED/CANCELLED/EXPIRED 状态的任务
     * 
     * ⚠️ 一对多架构注意：批量删除会永久移除历史记录（包括 debugPayload）
     */
    @PostMapping(value = "/device-maintenance:delete-all-tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DeleteAllResult>> deleteAllTasks() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🗑️ 删除所有可删除的任务（测试辅助接口）");

        // 查找所有可删除的任务
        List<DeviceTask> allTasks = deviceTaskRepository.findAll();
        List<DeviceTask> deletableTasks = allTasks.stream()
                .filter(task -> {
                    DeviceTask.TaskStatus status = task.getStatus();
                    return status == DeviceTask.TaskStatus.COMPLETED
                        || status == DeviceTask.TaskStatus.FAILED
                        || status == DeviceTask.TaskStatus.CANCELLED
                        || status == DeviceTask.TaskStatus.EXPIRED
                        || status == DeviceTask.TaskStatus.IDLE;
                })
                .collect(Collectors.toList());

        log.info("📊 找到 {} 个可删除的任务", deletableTasks.size());

        int successCount = 0;
        int failedCount = 0;

        // ✅ 使用批量删除避免 Hibernate 事件监听器问题
        try {
            if (!deletableTasks.isEmpty()) {
                deviceTaskRepository.deleteAll(deletableTasks);
                successCount = deletableTasks.size();
                log.info("  ✅ 批量删除成功: {} 个任务", successCount);
                
                // 记录删除的任务详情
                for (DeviceTask task : deletableTasks) {
                    log.info("    - taskId={}, type={}, status={}", 
                        task.getTaskId(), task.getTaskType(), task.getStatus());
                }
            }
        } catch (Exception e) {
            failedCount = deletableTasks.size();
            log.error("  ❌ 批量删除失败: {}", e.getMessage(), e);
        }

        DeleteAllResult result = new DeleteAllResult();
        result.setTotalFound(deletableTasks.size());
        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);

        log.info("✅ 批量删除完成: 成功={}, 失败={}", successCount, failedCount);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return jsonResponse(HttpStatus.OK,
                ApiResponse.success("Batch delete completed", result));
    }

    private <T> ResponseEntity<ApiResponse<T>> jsonResponse(HttpStatus status, ApiResponse<T> body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /**
     * 转换为响应DTO - ⭐ 添加完整字段
     */
    private DeviceTaskResponse convertToResponse(DeviceTask task) {
        DeviceTaskResponse response = new DeviceTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setBatchId(task.getBatchId());
        response.setBatchName(task.getBatchName());
        response.setWorkflowId(task.getWorkflowId());
        response.setTaskType(task.getTaskType() != null ? task.getTaskType().name() : null);
        response.setDeviceId(task.getDeviceId());
        response.setDeviceName(task.getDeviceName());
        response.setDeviceIp(task.getDeviceIp());
        response.setVendorType(task.getVendorType());
        response.setVendorName(task.getVendorName());
        response.setSftpServerName(task.getSftpServerName());
        response.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        response.setFilePath(task.getFilePath());
        response.setBackupFilePath(task.getBackupFilePath());
        response.setBackupFileName(task.getBackupFileName());
        response.setCurrentVersion(task.getCurrentVersion());
        response.setPreviousVersion(task.getPreviousVersion());
        response.setTargetVersion(task.getTargetVersion());
        response.setRetryCount(task.getRetryCount());
        response.setErrorMessage(task.getErrorMessage());
        response.setDebugPayload(task.getDebugPayload());
        response.setScheduledTime(task.getScheduledTime());
        response.setCreatedTime(task.getCreatedTime());
        response.setUpdatedTime(task.getUpdatedTime());
        response.setStartedTime(task.getStartedTime());
        response.setCompletedTime(task.getCompletedTime());
        
        return response;
    }

    private boolean isUpgradeBatch(String batchId) {
        return batchRepository.findById(batchId)
                .map(batch -> batch.getBatchType() == Batch.BatchType.UPGRADE)
                .orElse(false);
    }

    private List<DeviceTask> collapseUpgradeBatchTasksByDevice(List<DeviceTask> tasks) {
        Map<String, List<DeviceTask>> tasksByDevice = new LinkedHashMap<>();
        for (DeviceTask task : tasks) {
            if (task.getDeviceId() == null || task.getDeviceId().trim().isEmpty()) {
                continue;
            }
            tasksByDevice.computeIfAbsent(task.getDeviceId(), key -> new ArrayList<>()).add(task);
        }

        List<DeviceTask> collapsedTasks = new ArrayList<>();
        for (List<DeviceTask> deviceTasks : tasksByDevice.values()) {
            DeviceTask latestTask = deviceTasks.get(0);
            for (DeviceTask task : deviceTasks) {
                copyMissingDisplayFields(latestTask, task);
            }
            collapsedTasks.add(latestTask);
        }
        return collapsedTasks;
    }

    private void copyMissingDisplayFields(DeviceTask target, DeviceTask source) {
        if (isBlank(target.getFilePath()) && !isBlank(source.getFilePath())) {
            target.setFilePath(source.getFilePath());
        }
        if (isBlank(target.getCurrentVersion()) && !isBlank(source.getCurrentVersion())) {
            target.setCurrentVersion(source.getCurrentVersion());
        }
        if (isBlank(target.getPreviousVersion()) && !isBlank(source.getPreviousVersion())) {
            target.setPreviousVersion(source.getPreviousVersion());
        }
        if (isBlank(target.getTargetVersion()) && !isBlank(source.getTargetVersion())) {
            target.setTargetVersion(source.getTargetVersion());
        }
        if (isBlank(target.getSftpServerName()) && !isBlank(source.getSftpServerName())) {
            target.setSftpServerName(source.getSftpServerName());
        }
        if (isBlank(target.getDeviceName()) && !isBlank(source.getDeviceName())) {
            target.setDeviceName(source.getDeviceName());
        }
        if (isBlank(target.getDeviceIp()) && !isBlank(source.getDeviceIp())) {
            target.setDeviceIp(source.getDeviceIp());
        }
        if (isBlank(target.getVendorType()) && !isBlank(source.getVendorType())) {
            target.setVendorType(source.getVendorType());
        }
        if (isBlank(target.getVendorName()) && !isBlank(source.getVendorName())) {
            target.setVendorName(source.getVendorName());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 设备任务查询请求
     */
    public static class DeviceTaskQueryRequest {
        private String taskType; // DOWNLOAD, BACKUP, UPGRADE, RESTORE
        private String status;   // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
        private String deviceId;
        private Integer page;
        private Integer size;

        // getters and setters
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }

    /**
     * 通过设备ID列表获取任务列表请求
     */
    public static class GetTasksByDeviceIdsRequest {
        private List<String> deviceIds;

        public List<String> getDeviceIds() { return deviceIds; }
        public void setDeviceIds(List<String> deviceIds) { this.deviceIds = deviceIds; }
    }

    /**
     * 根据设备ID列表获取活跃批次ID列表请求
     */
    public static class GetActiveBatchIdsByDeviceIdsRequest {
        private List<String> deviceIds;

        public List<String> getDeviceIds() { return deviceIds; }
        public void setDeviceIds(List<String> deviceIds) { this.deviceIds = deviceIds; }
    }

    /**
     * 活跃批次ID列表响应
     */
    public static class GetActiveBatchIdsResponse {
        private List<String> batchIds;

        public List<String> getBatchIds() { return batchIds; }
        public void setBatchIds(List<String> batchIds) { this.batchIds = batchIds; }
    }

    /**
     * 通过设备ID获取任务列表请求
     */
    public static class GetTasksByDeviceIdRequest {
        private String deviceId;
        private Integer page;  // 页码（从0开始）
        private Integer size;  // 每页大小

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }

    /**
     * 设备任务详情请求
     */
    public static class DeviceTaskDetailRequest {
        private String taskId;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
    }

    /**
     * 取消任务请求
     */
    public static class CancelTaskRequest {
        private String taskId;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
    }

    /**
     * 删除任务请求
     */
    public static class DeleteTaskRequest {
        private String taskId;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
    }

    /**
     * 批量取消任务结果
     */
    public static class CancelAllResult {
        private int totalFound;      // 找到的可取消任务总数
        private int successCount;    // 成功取消的数量
        private int failedCount;     // 失败的数量

        public int getTotalFound() { return totalFound; }
        public void setTotalFound(int totalFound) { this.totalFound = totalFound; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    }

    /**
     * 批量删除任务结果
     */
    public static class DeleteAllResult {
        private int totalFound;      // 找到的可删除任务总数
        private int successCount;    // 成功删除的数量
        private int failedCount;     // 失败的数量
        private int notifiedCount;   // 已通知 TaskInfo 的数量

        public int getTotalFound() { return totalFound; }
        public void setTotalFound(int totalFound) { this.totalFound = totalFound; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        public int getNotifiedCount() { return notifiedCount; }
        public void setNotifiedCount(int notifiedCount) { this.notifiedCount = notifiedCount; }
    }

    /**
     * ⭐ 设备任务列表响应（包含总数）
     */
    public static class DeviceTaskListResponse {
        private List<DeviceTaskResponse> tasks;
        private int totalCount;  // 总记录数
        private int page;        // 当前页码
        private int size;        // 每页大小

        public List<DeviceTaskResponse> getTasks() { return tasks; }
        public void setTasks(List<DeviceTaskResponse> tasks) { this.tasks = tasks; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }

    /**
     * ⭐ 设备任务响应（完整字段）
     */
    public static class DeviceTaskResponse {
        private String taskId;
        private String batchId;
        private String batchName;
        private String workflowId;
        private String taskType;
        private String deviceId;
        private String deviceName;
        private String deviceIp;
        private String vendorType;
        private String vendorName;
        private String sftpServerName;
        private String status;
        private String filePath;
        private String backupFilePath;
        private String backupFileName;
        private String currentVersion;
        private String previousVersion;
        private String targetVersion;
        private Integer retryCount;
        private String errorMessage;
        private String debugPayload;
        private Long scheduledTime;
        private java.time.LocalDateTime createdTime;
        private java.time.LocalDateTime updatedTime;
        private java.time.LocalDateTime startedTime;
        private java.time.LocalDateTime completedTime;

        // getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getBatchName() { return batchName; }
        public void setBatchName(String batchName) { this.batchName = batchName; }
        public String getWorkflowId() { return workflowId; }
        public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public String getDeviceIp() { return deviceIp; }
        public void setDeviceIp(String deviceIp) { this.deviceIp = deviceIp; }
        public String getVendorType() { return vendorType; }
        public void setVendorType(String vendorType) { this.vendorType = vendorType; }
        public String getVendorName() { return vendorName; }
        public void setVendorName(String vendorName) { this.vendorName = vendorName; }
        public String getSftpServerName() { return sftpServerName; }
        public void setSftpServerName(String sftpServerName) { this.sftpServerName = sftpServerName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getBackupFilePath() { return backupFilePath; }
        public void setBackupFilePath(String backupFilePath) { this.backupFilePath = backupFilePath; }
        public String getBackupFileName() { return backupFileName; }
        public void setBackupFileName(String backupFileName) { this.backupFileName = backupFileName; }
        public String getCurrentVersion() { return currentVersion; }
        public void setCurrentVersion(String currentVersion) { this.currentVersion = currentVersion; }
        public String getPreviousVersion() { return previousVersion; }
        public void setPreviousVersion(String previousVersion) { this.previousVersion = previousVersion; }
        public String getTargetVersion() { return targetVersion; }
        public void setTargetVersion(String targetVersion) { this.targetVersion = targetVersion; }
        public Integer getRetryCount() { return retryCount; }
        public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getDebugPayload() { return debugPayload; }
        public void setDebugPayload(String debugPayload) { this.debugPayload = debugPayload; }
        public Long getScheduledTime() { return scheduledTime; }
        public void setScheduledTime(Long scheduledTime) { this.scheduledTime = scheduledTime; }
        public java.time.LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(java.time.LocalDateTime createdTime) { this.createdTime = createdTime; }
        public java.time.LocalDateTime getUpdatedTime() { return updatedTime; }
        public void setUpdatedTime(java.time.LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
        public java.time.LocalDateTime getStartedTime() { return startedTime; }
        public void setStartedTime(java.time.LocalDateTime startedTime) { this.startedTime = startedTime; }
        public java.time.LocalDateTime getCompletedTime() { return completedTime; }
        public void setCompletedTime(java.time.LocalDateTime completedTime) { this.completedTime = completedTime; }
    }

    /**
     * 统一API响应格式
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse() { }

        public static <T> ApiResponse<T> success(String message, T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = true;
            response.message = message;
            response.data = data;
            return response;
        }

        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = false;
            response.message = message;
            response.data = null;
            return response;
        }

        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }
}
