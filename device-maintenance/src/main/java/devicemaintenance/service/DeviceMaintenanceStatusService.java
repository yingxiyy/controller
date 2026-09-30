package devicemaintenance.service;

import devicemaintenance.dto.DeviceOperationStatus;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.repository.DeviceTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 设备维护状态检查服务
 * 提供设备当前维护状态的查询功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceMaintenanceStatusService {

    private final DeviceTaskRepository deviceTaskRepository;
    private final DeviceStatusQueryService deviceStatusQueryService;

    /**
     * 检查设备是否可以执行指定类型的维护任务
     * 
     * @param deviceId 设备ID
     * @param taskType 要执行的任务类型
     * @return 检查结果
     */
    public MaintenanceStatusCheckResult checkDeviceAvailableForTask(String deviceId, DeviceTask.TaskType taskType) {
        log.debug("检查设备维护状态: deviceId={}, taskType={}", deviceId, taskType);

        // 查询设备上正在进行的任务
        List<DeviceTask> ongoingTasks = deviceTaskRepository.findByDeviceIdAndStatus(
            deviceId, 
            DeviceTask.TaskStatus.RUNNING
        );

        if (ongoingTasks.isEmpty()) {
            log.debug("设备 {} 当前没有正在进行的任务，可以执行新任务", deviceId);
            return MaintenanceStatusCheckResult.available();
        }

        // 存在正在进行的任务，检查是否冲突
        DeviceTask ongoingTask = ongoingTasks.get(0);
        String conflictReason = checkTaskConflict(ongoingTask, taskType);
        
        if (conflictReason != null) {
            log.warn("设备 {} 正在执行 {} 任务，无法执行新的 {} 任务", 
                deviceId, ongoingTask.getTaskType(), taskType);
            return MaintenanceStatusCheckResult.unavailable(
                ongoingTask.getTaskType(),
                ongoingTask.getTaskId(),
                conflictReason
            );
        }

        return MaintenanceStatusCheckResult.available();
    }

    /**
     * 检查任务冲突
     *
     * @param ongoingTask 正在进行的任务
     * @param newTaskType 新任务类型
     * @return 冲突原因，如果无冲突则返回null
     */
    private String checkTaskConflict(DeviceTask ongoingTask, DeviceTask.TaskType newTaskType) {
        DeviceTask.TaskType ongoingType = ongoingTask.getTaskType();

        // 同类型任务冲突规则
        if (ongoingType == newTaskType) {
            return String.format("Device is executing %s task", getTaskTypeDisplayName(ongoingType));
        }

        // 不同类型任务冲突规则
        switch (newTaskType) {
            case BACKUP:
                // 备份任务与其他所有任务冲突
                return String.format("Device is executing %s task, cannot perform backup simultaneously",
                    getTaskTypeDisplayName(ongoingType));

            case RESTORE:
                // 恢复任务与其他所有任务冲突
                return String.format("Device is executing %s task, cannot perform restore simultaneously",
                    getTaskTypeDisplayName(ongoingType));

            case UPGRADE:
                // 升级任务与其他所有任务冲突
                return String.format("Device is executing %s task, cannot perform upgrade simultaneously",
                    getTaskTypeDisplayName(ongoingType));

            case DOWNLOAD:
                // 下载任务与升级、恢复冲突
                if (ongoingType == DeviceTask.TaskType.UPGRADE ||
                    ongoingType == DeviceTask.TaskType.RESTORE) {
                    return String.format("Device is executing %s task, cannot download software simultaneously",
                        getTaskTypeDisplayName(ongoingType));
                }
                break;

            case ROLLBACK:
                // 回滚任务与其他所有任务冲突
                return String.format("Device is executing %s task, cannot perform rollback simultaneously",
                    getTaskTypeDisplayName(ongoingType));

            case COMMIT:
                // 确认任务只与升级相关，其他任务不冲突
                if (ongoingType != DeviceTask.TaskType.UPGRADE) {
                    return String.format("Device is executing %s task, cannot perform upgrade commit",
                        getTaskTypeDisplayName(ongoingType));
                }
                break;
        }

        // 无冲突
        return null;
    }

    /**
     * 获取任务类型显示名称
     */
    private String getTaskTypeDisplayName(DeviceTask.TaskType taskType) {
        switch (taskType) {
            case BACKUP: return "backup";
            case RESTORE: return "restore";
            case UPGRADE: return "upgrade";
            case DOWNLOAD: return "download";
            case ROLLBACK: return "rollback";
            case COMMIT: return "commit";
            default: return taskType.name();
        }
    }

    /**
     * 获取设备当前维护状态详情
     * 
     * @param deviceId 设备ID
     * @return 维护状态详情
     */
    public DeviceMaintenanceStatus getDeviceMaintenanceStatus(String deviceId) {
        log.debug("获取设备维护状态详情: deviceId={}", deviceId);

        List<DeviceTask> ongoingTasks = deviceTaskRepository.findByDeviceIdAndStatus(
            deviceId, 
            DeviceTask.TaskStatus.RUNNING
        );

        if (ongoingTasks.isEmpty()) {
            return DeviceMaintenanceStatus.idle(deviceId);
        }

        DeviceTask ongoingTask = ongoingTasks.get(0);
        return DeviceMaintenanceStatus.busy(
            deviceId,
            ongoingTask.getTaskType(),
            ongoingTask.getTaskId(),
            ongoingTask.getStartedTime()
        );
    }

    /**
     * 维护状态检查结果
     */
    public static class MaintenanceStatusCheckResult {
        private final boolean available;
        private final DeviceTask.TaskType ongoingTaskType;
        private final String ongoingTaskId;
        private final String reason;

        private MaintenanceStatusCheckResult(boolean available, 
                                            DeviceTask.TaskType ongoingTaskType,
                                            String ongoingTaskId,
                                            String reason) {
            this.available = available;
            this.ongoingTaskType = ongoingTaskType;
            this.ongoingTaskId = ongoingTaskId;
            this.reason = reason;
        }

        public static MaintenanceStatusCheckResult available() {
            return new MaintenanceStatusCheckResult(true, null, null, null);
        }

        public static MaintenanceStatusCheckResult unavailable(
                DeviceTask.TaskType ongoingTaskType, 
                String ongoingTaskId,
                String reason) {
            return new MaintenanceStatusCheckResult(false, ongoingTaskType, ongoingTaskId, reason);
        }

        public boolean isAvailable() {
            return available;
        }

        public DeviceTask.TaskType getOngoingTaskType() {
            return ongoingTaskType;
        }

        public String getOngoingTaskId() {
            return ongoingTaskId;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * 设备维护状态
     */
    public static class DeviceMaintenanceStatus {
        private final String deviceId;
        private final boolean isBusy;
        private final DeviceTask.TaskType currentTaskType;
        private final String currentTaskId;
        private final java.time.LocalDateTime taskStartTime;

        private DeviceMaintenanceStatus(String deviceId, boolean isBusy,
                                        DeviceTask.TaskType currentTaskType,
                                        String currentTaskId,
                                        java.time.LocalDateTime taskStartTime) {
            this.deviceId = deviceId;
            this.isBusy = isBusy;
            this.currentTaskType = currentTaskType;
            this.currentTaskId = currentTaskId;
            this.taskStartTime = taskStartTime;
        }

        public static DeviceMaintenanceStatus idle(String deviceId) {
            return new DeviceMaintenanceStatus(deviceId, false, null, null, null);
        }

        public static DeviceMaintenanceStatus busy(String deviceId,
                                                   DeviceTask.TaskType taskType,
                                                   String taskId,
                                                   java.time.LocalDateTime startTime) {
            return new DeviceMaintenanceStatus(deviceId, true, taskType, taskId, startTime);
        }

        public String getDeviceId() {
            return deviceId;
        }

        public boolean isBusy() {
            return isBusy;
        }

        public DeviceTask.TaskType getCurrentTaskType() {
            return currentTaskType;
        }

        public String getCurrentTaskId() {
            return currentTaskId;
        }

        public java.time.LocalDateTime getTaskStartTime() {
            return taskStartTime;
        }
    }

    /**
     * 获取设备的操作状态
     * 从MongoDB op-phy-node集合中查询设备的维护操作状态
     *
     * @param deviceIdentifier 设备ID（Site-xxx#Ne-xxx）或设备名称
     * @return 设备操作状态，如果设备不存在返回null
     */
    public DeviceOperationStatus getDeviceOperationStatus(String deviceIdentifier) {
        log.info("获取设备操作状态: deviceIdentifier={}", deviceIdentifier);

        try {
            // 1. 查询设备物理节点
            Optional<Document> physicalNodeOpt = deviceStatusQueryService.findDevicePhysicalNode(deviceIdentifier);
            if (!physicalNodeOpt.isPresent()) {
                log.warn("未找到设备: deviceIdentifier={}", deviceIdentifier);
                return null;
            }

            // 2. 提取设备节点信息
            Optional<Document> deviceNodeOpt = deviceStatusQueryService.extractDeviceNode(physicalNodeOpt.get());
            if (!deviceNodeOpt.isPresent()) {
                log.warn("无法提取设备节点信息: deviceIdentifier={}", deviceIdentifier);
                return null;
            }

            Document deviceNode = deviceNodeOpt.get();

            // 3. 提取系统属性
            Optional<List<Document>> propertiesOpt = deviceStatusQueryService.extractSystemProperties(deviceNode);
            if (!propertiesOpt.isPresent()) {
                log.warn("无法提取系统属性: deviceIdentifier={}", deviceIdentifier);
                return null;
            }

            // 4. 解析设备操作状态
            return parseDeviceOperationStatus(deviceNode, propertiesOpt.get());

        } catch (Exception e) {
            log.error("获取设备操作状态失败: deviceIdentifier={}, error={}", deviceIdentifier, e.getMessage());
            throw new RuntimeException("Failed to get device operation status: " + e.getMessage(), e);
        }
    }

    /**
     * 获取所有主设备的操作状态
     * 主设备定义：neId格式为 Site-xxx#Ne-xxx（1个#分隔符）
     * 排除子设备：equipment-id格式为 Site-xxx#Ne-xxx#EQUIPMENT-xxx（2个或更多#）
     *
     * @return 所有主设备的操作状态列表
     */
    public List<DeviceOperationStatus> getAllDeviceOperationStatus() {
        return getAllDeviceOperationStatus(null);
    }

    /**
     * 获取所有主设备的操作状态（带过滤条件）
     * 主设备定义：neId格式为 Site-xxx#Ne-xxx（1个#分隔符）
     * 排除子设备：equipment-id格式为 Site-xxx#Ne-xxx#EQUIPMENT-xxx（2个或更多#）
     *
     * @param filters 过滤条件（可选），支持的字段：
     *                - operationalState: 运行状态（如 "up", "down"）
     *                - communicationStatus: 通信状态（如 "syncFinished", "syncFailed"）
     *                - deviceName: 设备名称（模糊匹配）
     *                - currentSoftware: 当前软件版本（精确匹配）
     * @return 过滤后的主设备操作状态列表
     */
    public List<DeviceOperationStatus> getAllDeviceOperationStatus(Map<String, String> filters) {
        log.info("获取所有主设备操作状态, 过滤条件: {}", filters);

        try {
            // 1. 查询所有主设备物理节点
            List<Document> allPhysicalNodes = deviceStatusQueryService.findAllMainDevicePhysicalNodes();
            log.info("找到 {} 个主设备", allPhysicalNodes.size());

            // 2. 解析每个设备的操作状态
            List<DeviceOperationStatus> result = new java.util.ArrayList<>();
            for (Document physicalNode : allPhysicalNodes) {
                try {
                    // 提取设备节点
                    Optional<Document> deviceNodeOpt = deviceStatusQueryService.extractDeviceNode(physicalNode);
                    if (!deviceNodeOpt.isPresent()) {
                        log.warn("无法提取设备节点信息，跳过");
                        continue;
                    }

                    Document deviceNode = deviceNodeOpt.get();

                    // 提取系统属性
                    Optional<List<Document>> propertiesOpt = deviceStatusQueryService.extractSystemProperties(deviceNode);
                    if (!propertiesOpt.isPresent()) {
                        log.warn("无法提取系统属性: deviceId={}, 跳过", deviceNode.getString("node-id"));
                        continue;
                    }

                    // 解析设备状态
                    DeviceOperationStatus status = parseDeviceOperationStatus(deviceNode, propertiesOpt.get());
                    
                    // 3. 应用过滤条件
                    if (matchesFilters(status, filters)) {
                        result.add(status);
                    }

                } catch (Exception e) {
                    log.error("解析设备状态失败，跳过该设备: error={}", e.getMessage());
                    // 继续处理下一个设备
                }
            }

            log.info("过滤后成功解析 {} 个设备的操作状态", result.size());
            return result;

        } catch (Exception e) {
            log.error("获取所有设备操作状态失败: error={}", e.getMessage());
            throw new RuntimeException("Failed to get all device operation statuses: " + e.getMessage(), e);
        }
    }

    /**
     * 检查设备状态是否匹配过滤条件
     */
    private boolean matchesFilters(DeviceOperationStatus status, Map<String, String> filters) {
        // 如果没有过滤条件，则匹配所有设备
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        // 遍历每个过滤条件，所有条件都必须满足（AND逻辑）
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String fieldName = filter.getKey();
            String expectedValue = filter.getValue();
            
            // 忽略空值
            if (expectedValue == null || expectedValue.trim().isEmpty()) {
                continue;
            }

            boolean fieldMatches = false;

            switch (fieldName) {
                case "operationalState":
                    fieldMatches = matchesField(status.getOperationalState(), expectedValue);
                    break;
                case "communicationStatus":
                    fieldMatches = matchesField(status.getCommunicationStatus(), expectedValue);
                    break;
                case "deviceName":
                    // 设备名称支持模糊匹配
                    fieldMatches = status.getDeviceName() != null 
                        && status.getDeviceName().toLowerCase().contains(expectedValue.toLowerCase());
                    break;
                case "currentSoftware":
                    fieldMatches = matchesField(status.getCurrentSoftware(), expectedValue);
                    break;
                case "currentDatabase":
                    fieldMatches = matchesField(status.getCurrentDatabase(), expectedValue);
                    break;
                default:
                    log.warn("未知的过滤字段: {}", fieldName);
                    // 未知字段不影响过滤结果
                    continue;
            }

            // 如果任一条件不匹配，则返回false
            if (!fieldMatches) {
                return false;
            }
        }

        // 所有条件都匹配
        return true;
    }

    /**
     * 检查字段值是否匹配（精确匹配，不区分大小写）
     */
    private boolean matchesField(String actualValue, String expectedValue) {
        if (actualValue == null) {
            return false;
        }
        return actualValue.equalsIgnoreCase(expectedValue);
    }

    /**
     * 解析设备操作状态
     */
    private DeviceOperationStatus parseDeviceOperationStatus(Document deviceNode, List<Document> properties) {
        // 提取基本信息
        String deviceId = deviceNode.getString("node-id");
        Document physical = deviceNode.get("otn-phy-topology:physical", Document.class);
        String deviceName = physical != null ? physical.getString("friendly-name") : null;
        String communicationStatus = physical != null ? physical.getString("communication-status") : null;
        String operationalState = physical != null ? physical.getString("operational-state") : null;

        // 将属性列表转换为Map便于查找
        Map<String, String> propertyMap = new HashMap<>();
        for (Document property : properties) {
            String name = property.getString("name");
            String value = property.getString("value");
            if (name != null) {
                propertyMap.put(name, value);
            }
        }

        // 构建设备操作状态
        return DeviceOperationStatus.builder()
            .deviceId(deviceId)
            .deviceName(convertEmptyToNull(deviceName))
            .communicationStatus(convertEmptyToNull(communicationStatus))
            .operationalState(convertEmptyToNull(operationalState))
            .currentSoftware(convertEmptyToNull(propertyMap.get("current-software")))
            .currentDatabase(convertEmptyToNull(propertyMap.get("current-database")))
            .systemDateTime(convertEmptyToNull(propertyMap.get("system.current-datetime")))
            .softwareOperations(buildSoftwareOperations(propertyMap))
            .databaseOperations(buildDatabaseOperations(propertyMap))
            .build();
    }

    /**
     * 构建软件操作状态
     */
    private DeviceOperationStatus.SoftwareOperations buildSoftwareOperations(Map<String, String> propertyMap) {
        // 构建下载操作状态
        DeviceOperationStatus.SoftwareOperations.SoftwareDownload download = 
            DeviceOperationStatus.SoftwareOperations.SoftwareDownload.builder()
                .state(convertEmptyToNull(propertyMap.get("download.download-state")))
                .fileName(convertEmptyToNull(propertyMap.get("download.file-name")))
                .softwareVersion(convertEmptyToNull(propertyMap.get("download.software-version")))
                .downloadTime(convertEmptyToNull(propertyMap.get("download.download-time")))
                .expectedDuration(convertEmptyToNull(propertyMap.get("download.expected-duration")))
                .build();

        // 构建升级操作状态
        DeviceOperationStatus.SoftwareOperations.SoftwareUpgrade upgrade = 
            DeviceOperationStatus.SoftwareOperations.SoftwareUpgrade.builder()
                .state(convertEmptyToNull(propertyMap.get("upgrade.upgrade-state")))
                .upgradeTime(convertEmptyToNull(propertyMap.get("upgrade.upgrade-time")))
                .rollbackFile(convertEmptyToNull(propertyMap.get("upgrade.rollback-file")))
                .rollbackSoftware(convertEmptyToNull(propertyMap.get("upgrade.rollback-software")))
                .build();

        return DeviceOperationStatus.SoftwareOperations.builder()
            .download(download)
            .upgrade(upgrade)
            .build();
    }

    /**
     * 构建数据库操作状态
     */
    private DeviceOperationStatus.DatabaseOperations buildDatabaseOperations(Map<String, String> propertyMap) {
        // 构建备份操作状态
        DeviceOperationStatus.DatabaseOperations.DatabaseBackup backup = 
            DeviceOperationStatus.DatabaseOperations.DatabaseBackup.builder()
                .state(convertEmptyToNull(propertyMap.get("backup.backup-state")))
                .backupFile(convertEmptyToNull(propertyMap.get("backup.backup-file")))
                .backupTime(convertEmptyToNull(propertyMap.get("backup.backup-time")))
                .backupDatabase(convertEmptyToNull(propertyMap.get("backup.backup-database")))
                .build();

        // 构建恢复操作状态
        DeviceOperationStatus.DatabaseOperations.DatabaseRestore restore = 
            DeviceOperationStatus.DatabaseOperations.DatabaseRestore.builder()
                .state(convertEmptyToNull(propertyMap.get("restore.restore-state")))
                .restoreTime(convertEmptyToNull(propertyMap.get("restore.restore-time")))
                .restoreFromFile(convertEmptyToNull(propertyMap.get("restore.backup-file")))
                .restoreDatabase(convertEmptyToNull(propertyMap.get("restore.restore-database")))
                .build();

        return DeviceOperationStatus.DatabaseOperations.builder()
            .backup(backup)
            .restore(restore)
            .build();
    }

    /**
     * 将空字符串转换为null
     */
    private String convertEmptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }
}
