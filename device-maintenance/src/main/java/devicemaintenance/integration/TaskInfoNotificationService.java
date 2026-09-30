package devicemaintenance.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import devicemaintenance.entity.DeviceTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TaskInfo集成服务
 * 
 * 负责将device-maintenance的任务信息通知到taskInfo模块
 * 通过Kafka消息异步发送TaskInfoMessage
 * 
 * 已知限制:
 * - TaskInfoMessage 缺少 groupId/root/objectId/objectType 的 setter 方法
 * - 这是由于 common-model 中 Lombok 代码生成问题导致
 * - 解决方案: 将这些信息存储在 detail JSON 字段中
 * - 前端可以从 detail JSON 中提取这些信息进行分组和过滤
 * 
 * 参考: readme/taskInfo.md 了解 TaskInfo 模块详情
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskInfoNotificationService {

  private static final String DEFAULT_USER = "system";
  private static final String USER_HEADER = "user"; // Gateway转换后的用户名header

  private final ObjectMapper objectMapper = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  /**
   * 从当前HTTP请求中获取用户名
   * Gateway会将token转换为用户名并放入header中
   * 
   * @return 用户名，如果无法获取则返回"system"
   */
  private String getCurrentUser() {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        String user = request.getHeader(USER_HEADER);

        if (user != null && !user.trim().isEmpty()) {
          log.info("  📝 [Debug] 从Header获取到用户: {} (来自Header: {})", user, USER_HEADER);
          return user;
        }
      }
    } catch (Exception e) {
      log.debug("无法从RequestContext获取用户信息（可能是异步任务）: {}", e.getMessage());
    }

    log.info("  📝 [Debug] 未获取到用户，使用默认用户: {}", DEFAULT_USER);
    return DEFAULT_USER;
  }

  /**
   * 获取当前操作用户（支持从批次获取）
   * 1. 优先使用批次的 createdBy（适用于异步/定时任务场景）
   * 2. 其次从 HTTP request header 获取（适用于同步HTTP请求场景）
   * 3. 最后回退到默认用户
   */
  private String getCurrentUser(devicemaintenance.entity.Batch batch) {
    // 1. 优先使用批次保存的用户
    if (batch != null && batch.getCreatedBy() != null && !batch.getCreatedBy().trim().isEmpty()) {
      log.info("  📝 [Debug] 从批次获取到用户: {} (批次创建者)", batch.getCreatedBy());
      return batch.getCreatedBy();
    }

    // 2. 回退到从 HTTP header 获取
    return getCurrentUser();
  }

  /**
   * 通知任务开始（创建任务）
   * 
   * 当内部任务从PENDING变为RUNNING时调用
   * 
   * @param task 设备任务
   */
  public void notifyTaskStarted(DeviceTask task) {
    try {
      log.info("通知TaskInfo: 任务创建 - taskId={}, deviceId={}, type={}, status={}",
          task.getTaskId(), task.getDeviceId(), task.getTaskType(), task.getStatus());

      // ✅ 根据任务类型选择对应的 ActionType
      ActionType actionType = getActionTypeForTask(task);
      // ⭐ 创建通知必须传入真实用户（从 HTTP 请求获取）
      TaskInfoMessage message = convertToTaskInfoMessage(task, false, actionType, getCurrentUser());

      // ⭐ 保存 actionTime 到任务中，用于删除通知时精确匹配
      task.setTaskInfoActionTime(message.getActionTime());

      TaskInfoMessager.sendMessage(message);

      log.info("✅ TaskInfo创建通知已发送: user={}, actionType={}, actionTime={}, resourceId={}, successfully={}, endTime={}",
          message.getWho(), actionType, message.getActionTime(), message.getResourceId(),
          message.getSuccessfully(), message.getEndTime());
    } catch (Exception e) {
      log.error("发送TaskInfo通知失败: taskId=" + task.getTaskId(), e);
      // 不抛出异常，避免影响主业务流程
    }
  }

  /**
   * 通知任务完成（更新任务）
   * 
   * 当内部任务变为COMPLETED或FAILED时调用
   * 
   * @param task 设备任务
   */
  public void notifyTaskCompleted(DeviceTask task) {
    try {
      log.info("通知TaskInfo: 任务更新（完成） - taskId={}, deviceId={}, status={}",
          task.getTaskId(), task.getDeviceId(), task.getStatus());

      // ✅ 根据任务类型选择对应的 ActionType
      ActionType actionType = getActionTypeForTask(task);
      // ⭐ 更新通知传入 null，保持 TaskInfo 原有的 operator 值（避免在定时任务线程中变成 "system"）
      TaskInfoMessage message = convertToTaskInfoMessage(task, true, actionType, null);
      TaskInfoMessager.sendMessage(message);

      log.info("✅ TaskInfo完成通知已发送: user={}, actionType={}, actionTime={}, successfully={}, endTime={}, errorReason={}",
          message.getWho(), actionType, message.getActionTime(),
          message.getSuccessfully(), message.getEndTime(), message.getErrorReason());
    } catch (Exception e) {
      log.error("发送TaskInfo完成通知失败: taskId=" + task.getTaskId(), e);
      // 不抛出异常，避免影响主业务流程
    }
  }

  /**
   * 通知任务删除
   * 
   * 当任务被删除时调用
   * 
   * @param task 设备任务
   */
  public void notifyTaskDeleted(DeviceTask task) {
    try {
      log.info("通知TaskInfo: 任务删除 - taskId={}, deviceId={}",
          task.getTaskId(), task.getDeviceId());

      // ✅ 删除任务时使用 ActionType.delete
      TaskInfoMessage message = convertToTaskInfoMessageForDelete(task);

      TaskInfoMessager.sendMessage(message);

      log.info("TaskInfo删除通知已发送: actionType=delete");
      log.debug("  使用 ActionType.delete 语义明确");
      log.debug("  同时在 detail JSON 中标记 deleted=true 作为辅助信息");
    } catch (Exception e) {
      log.error("发送TaskInfo删除通知失败: taskId=" + task.getTaskId(), e);
      // 不抛出异常，避免影响主业务流程
    }
  }

  /**
   * 为删除操作转换TaskInfoMessage
   * 在detail JSON中添加deleted标志和deletedTime
   */
  private TaskInfoMessage convertToTaskInfoMessageForDelete(DeviceTask task) {
    // ⭐ 从HTTP请求获取真实用户
    String user = getCurrentUser();

    TaskInfoMessage message = new TaskInfoMessage(
        user, // ✅ 使用真实用户而不是"system"
        ResourceType.device,
        ActionType.delete, // ✅ 使用 ActionType.delete
        buildDetailJsonForDelete(task) // 使用特殊的detail构建方法
    );

    // 基本信息映射
    message.setResourceId(task.getDeviceId());
    message.setResourceName(task.getDeviceName() != null ? task.getDeviceName() : task.getDeviceId()); // 优先使用friendlyName

    // ⭐ 时间映射：使用创建通知时保存的 actionTime，确保能精确匹配原任务
    if (task.getTaskInfoActionTime() != null) {
      // 使用创建通知时保存的 actionTime（100% 一致）
      message.setActionTime(task.getTaskInfoActionTime());
      log.debug("  使用保存的 actionTime: {}", task.getTaskInfoActionTime());
    } else if (task.getStartedTime() != null) {
      // 备用方案：使用 startedTime
      message.setActionTime(task.getStartedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
      log.warn("  taskInfoActionTime 为空，使用 startedTime 作为 actionTime");
    } else {
      // 最后备用：使用当前时间（可能找不到原任务）
      message.setActionTime(System.currentTimeMillis());
      log.warn("  taskInfoActionTime 和 startedTime 都为空，使用当前时间（可能无法匹配原任务）");
    }

    // 设置结束时间为当前时间（标记任务已结束）
    message.setEndTime(System.currentTimeMillis());

    // 保持原任务的成功状态（不改变）
    boolean isSuccessful = !task.getStatus().name().equals("FAILED")
        && !task.getStatus().name().equals("CANCELLED");
    message.setSuccessfully(isSuccessful);

    // 不设置errorReason（任务本身没有错误，只是被删除了）

    return message;
  }

  /**
   * 为删除操作构建detail JSON
   * 添加deleted和deletedTime标志
   */
  private String buildDetailJsonForDelete(DeviceTask task) {
    try {
      Map<String, Object> detail = new HashMap<>();

      // 核心字段
      detail.put("taskId", task.getTaskId());
      detail.put("taskType", task.getTaskType().name());
      detail.put("deviceId", task.getDeviceId());
      detail.put("status", task.getStatus().name());

      // ⭐ 删除标志（前端可以用这个字段过滤）
      detail.put("deleted", true);
      detail.put("deletedTime", java.time.LocalDateTime.now().toString());
      detail.put("deletedTimestamp", System.currentTimeMillis());

      // 补充字段
      detail.put("objectId", task.getDeviceId());
      detail.put("objectType", task.getTaskType().name());

      // ⭐ 所有备份/恢复任务都有 batchId
      if (task.getBatchId() != null) {
        detail.put("batchId", task.getBatchId());
        detail.put("groupId", parseBatchIdToLong(task.getBatchId()));
        detail.put("root", false);
      } else {
        // 向后兼容：没有 batchId 的老任务
        try {
          detail.put("groupId", Long.parseLong(task.getTaskId()));
        } catch (NumberFormatException e) {
          detail.put("groupId", System.currentTimeMillis());
        }
        detail.put("root", true);
      }

      // 时间字段
      if (task.getCreatedTime() != null) {
        detail.put("createdTime", task.getCreatedTime().toString());
      }
      if (task.getScheduledTime() != null) {
        detail.put("scheduledTime", task.getScheduledTime().toString());
      }
      if (task.getStartedTime() != null) {
        detail.put("startedTime", task.getStartedTime().toString());
      }
      if (task.getCompletedTime() != null) {
        detail.put("completedTime", task.getCompletedTime().toString());
      }

      // 备份相关字段
      if (task.getBackupFilePath() != null) {
        detail.put("backupFilePath", task.getBackupFilePath());
      }
      if (task.getBackupFileName() != null) {
        detail.put("backupFileName", task.getBackupFileName());
      }

      // 批量任务ID
      if (task.getBatchId() != null) {
        detail.put("batchId", task.getBatchId());
      }

      return objectMapper.writeValueAsString(detail);
    } catch (JsonProcessingException e) {
      log.error("构建删除detail JSON失败", e);
      return "{}";
    }
  }

  /**
   * 转换DeviceTask到TaskInfoMessage
   * 
   * 字段映射说明:
   * - resourceId = deviceId (设备ID)
   * - resourceType = device (固定值)
   * - resourceName = deviceId (使用设备ID作为显示名称)
   * - actionType = 根据操作类型传入 (create/updateDevice/delete)
   * - actionTime = startedTime (任务开始时间)
   * - endTime = completedTime (任务完成时间)
   * - successfully = 根据status判断 (FAILED/CANCELLED为false，其他为true)
   * - errorReason = errorMessage (错误信息)
   * - groupId = batchId (批量任务ID) 或 taskId (单个任务)
   * - root = 根据是否批量任务判断 (批量子任务为false，单个任务为true)
   * - objectId = deviceId (对象ID，用于前端过滤)
   * - objectType = taskType (对象类型，如BACKUP/RESTORE/SOFTWARE_DOWNLOAD等)
   * - detail = 包含所有详细字段的JSON (taskId, status, 时间等)
   * 
   * @param task           设备任务
   * @param includeEndTime 是否包含结束时间
   * @param actionType     操作类型（create/updateDevice/delete）
   * @param createdBy      任务创建者（null 表示更新通知，保持原值）
   * @return TaskInfoMessage
   */
  private TaskInfoMessage convertToTaskInfoMessage(DeviceTask task, boolean includeEndTime, ActionType actionType,
      String createdBy) {
    // ⭐ 直接使用 createdBy（即使是 null）
    // 传入 null 时，TaskInfo 不会更新 who 字段，保持原值
    String user = createdBy;
    if (user != null && !user.isEmpty()) {
      log.debug("  📝 [Debug] 单任务通知 operator: {} (来源: 传入参数)", user);
    } else {
      log.debug("  📝 [Debug] createdBy 为空，传入 null 保持 TaskInfo 原有 operator");
    }

    TaskInfoMessage message = new TaskInfoMessage(
        user, // ✅ 使用真实用户而不是"system"
        ResourceType.device,
        actionType, // ✅ 使用传入的 actionType
        buildDetailJson(task));

    // 基本信息映射
    message.setResourceId(task.getDeviceId()); // deviceId → resourceId
    message.setResourceName(task.getDeviceName() != null ? task.getDeviceName() : task.getDeviceId()); // 优先使用friendlyName

    // ⭐ 时间映射：优先使用保存的 actionTime，确保所有通知都能匹配到同一个任务
    if (task.getTaskInfoActionTime() != null) {
      // 使用创建通知时保存的 actionTime（100% 一致）
      message.setActionTime(task.getTaskInfoActionTime());
    } else if (task.getStartedTime() != null) {
      // 备用方案：使用 startedTime（用于老数据兼容）
      message.setActionTime(task.getStartedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    } else {
      // 最后备用：使用当前时间
      message.setActionTime(System.currentTimeMillis());
    }

    // ⭐ 设置结束时间
    if (includeEndTime && task.getCompletedTime() != null) {
      Long endTime = task.getCompletedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
      message.setEndTime(endTime);
      log.debug("  📝 [Debug] 任务已完成，设置endTime: {} (completedTime={})",
          endTime, task.getCompletedTime());
    } else if (includeEndTime && task.getCompletedTime() == null) {
      log.warn("  ⚠️ [Debug] 要求包含endTime但completedTime为null！taskId={}, status={}",
          task.getTaskId(), task.getStatus());
    } else {
      log.debug("  📝 [Debug] 创建通知，不设置endTime (includeEndTime={})", includeEndTime);
    }

    // 状态映射：根据是否完成通知决定 successfully 含义
    // - 创建通知（includeEndTime=false）: successfully=false（任务未完成）
    // - 完成通知（includeEndTime=true）: successfully
    // 根据最终状态判断（FAILED/CANCELLED=false，COMPLETED=true）
    if (includeEndTime) {
      // 完成通知：根据最终状态判断
      boolean isSuccessful = task.getStatus() == DeviceTask.TaskStatus.COMPLETED;
      message.setSuccessfully(isSuccessful);
      log.debug("  📝 [Debug] 完成通知，successfully={} (status={})", isSuccessful, task.getStatus());
    } else {
      // 创建通知：successfully=false（任务未完成，还在进行中）
      // ⚠️ 与批量任务保持一致：successfully 表示任务是否完成，而非创建是否成功
      message.setSuccessfully(false);
      log.debug("  📝 [Debug] 创建通知，successfully=false（任务未完成）");
    }

    // 错误信息
    if (task.getErrorMessage() != null && !task.getErrorMessage().isEmpty()) {
      message.setErrorReason(task.getErrorMessage());
    }

    // ⚠️ 字段映射限制说明:
    // TaskInfoMessage 虽然使用了 @Data 注解，但由于以下原因缺少某些 setter 方法:
    // 1. groupId/root 字段有默认值，Lombok 可能不生成 setter
    // 2. objectId/objectType 字段定义后有多余分号，影响代码生成
    //
    // 解决方案: 将这些信息放入 detail JSON 中
    // - groupId/root: 通过 batchId 判断是否批量任务
    // - objectId/objectType: 设备ID和任务类型信息
    //
    // 前端可以从 detail JSON 中提取这些信息进行过滤和展示

    return message;
  }

  /**
   * 构建detail JSON字符串
   * 
   * 包含所有未直接映射到TaskInfoMessage的字段
   * 
   * 由于 TaskInfoMessage 缺少某些 setter 方法，以下字段也放入 detail JSON:
   * - objectId: 设备ID (用于前端过滤)
   * - objectType: 任务类型 (用于前端分类)
   * - groupId: 批量任务ID (用于任务分组)
   * - root: 是否根任务 (批量任务的主任务)
   * 
   * @param task 设备任务
   * @return JSON字符串
   */
  private String buildDetailJson(DeviceTask task) {
    try {
      Map<String, Object> detail = new HashMap<>();

      // 核心字段
      detail.put("taskId", task.getTaskId());
      detail.put("taskType", task.getTaskType().name());
      detail.put("deviceId", task.getDeviceId());
      detail.put("status", task.getStatus().name());

      // ⚠️ 补充字段: TaskInfoMessage 缺少的 setter 方法对应的字段
      detail.put("objectId", task.getDeviceId());
      detail.put("objectType", task.getTaskType().name());

      // ⭐ 所有备份/恢复任务都有 batchId（即使是单设备操作）
      if (task.getBatchId() != null) {
        detail.put("batchId", task.getBatchId());
        detail.put("groupId", parseBatchIdToLong(task.getBatchId()));
        detail.put("root", false); // 标记为子任务
      } else {
        // 向后兼容：没有 batchId 的老任务
        try {
          detail.put("groupId", Long.parseLong(task.getTaskId()));
        } catch (NumberFormatException e) {
          detail.put("groupId", System.currentTimeMillis());
        }
        detail.put("root", true); // 单个任务是根任务
      }

      // 时间字段
      if (task.getCreatedTime() != null) {
        detail.put("createdTime", task.getCreatedTime().toString());
      }
      if (task.getScheduledTime() != null) {
        detail.put("scheduledTime", task.getScheduledTime().toString());
      }
      if (task.getStartedTime() != null) {
        detail.put("startedTime", task.getStartedTime().toString());
      }
      if (task.getCompletedTime() != null) {
        detail.put("completedTime", task.getCompletedTime().toString());
      }

      // 备份/恢复相关字段（模拟 system-change 通知的结构）
      if (task.getTaskType() == DeviceTask.TaskType.BACKUP) {
        // 备份操作：构建类似 system-change 通知的结构
        Map<String, Object> backupDetail = new HashMap<>();

        // 状态映射
        String backupState = mapTaskStatusToDeviceState(task.getStatus());
        backupDetail.put("backup-state", backupState);

        // 文件信息
        if (task.getBackupFileName() != null) {
          backupDetail.put("backup-file", task.getBackupFileName());
        }
        if (task.getBackupFilePath() != null) {
          backupDetail.put("backup-path", task.getBackupFilePath());
        }

        // 时间信息
        if (task.getCompletedTime() != null) {
          backupDetail.put("backup-time", task.getCompletedTime().toString());
        } else if (task.getStartedTime() != null) {
          backupDetail.put("backup-time", task.getStartedTime().toString());
        }

        detail.put("backup", backupDetail);

        // 兼容旧格式
        if (task.getBackupFilePath() != null) {
          detail.put("backupFilePath", task.getBackupFilePath());
        }
        if (task.getBackupFileName() != null) {
          detail.put("backupFileName", task.getBackupFileName());
        }

      } else if (task.getTaskType() == DeviceTask.TaskType.RESTORE) {
        // 恢复操作：构建类似 system-change 通知的结构
        Map<String, Object> restoreDetail = new HashMap<>();

        // 状态映射
        String restoreState = mapTaskStatusToDeviceState(task.getStatus());
        restoreDetail.put("restore-state", restoreState);

        // 文件信息
        if (task.getBackupFilePath() != null) {
          restoreDetail.put("restore-from-file", task.getBackupFilePath());
        }

        // 时间信息
        if (task.getCompletedTime() != null) {
          restoreDetail.put("restore-time", task.getCompletedTime().toString());
        } else if (task.getStartedTime() != null) {
          restoreDetail.put("restore-time", task.getStartedTime().toString());
        }

        detail.put("restore", restoreDetail);

        // 兼容旧格式
        if (task.getBackupFilePath() != null) {
          detail.put("backupFilePath", task.getBackupFilePath());
        }

      } else {
        // 其他任务类型：保留原有字段
        if (task.getBackupFilePath() != null) {
          detail.put("backupFilePath", task.getBackupFilePath());
        }
        if (task.getBackupFileName() != null) {
          detail.put("backupFileName", task.getBackupFileName());
        }
      }

      // 批量任务ID (已在上面处理，这里无需重复)

      return objectMapper.writeValueAsString(detail);
    } catch (JsonProcessingException e) {
      log.error("构建detail JSON失败", e);
      return "{}";
    }
  }

  /**
   * 根据任务类型获取对应的 ActionType
   * 
   * 映射关系:
   * - DOWNLOAD → deviceSoftwareUpgrade (下载是升级流程的一部分)
   * - BACKUP → deviceDatabaseBackup
   * - RESTORE → deviceDatabaseRestore
   * - UPGRADE → deviceSoftwareUpgrade
   * - ROLLBACK → deviceSoftwareUpgrade (回滚也属于升级操作)
   * - COMMIT → deviceSoftwareUpgrade (确认也属于升级操作)
   * 
   * @param task 设备任务
   * @return 对应的 ActionType
   */
  private ActionType getActionTypeForTask(DeviceTask task) {
    switch (task.getTaskType()) {
      case BACKUP:
        return ActionType.deviceDatabaseBackup;
      case RESTORE:
        return ActionType.deviceDatabaseRestore;
      case ROLLBACK:
        return ActionType.deviceDatabaseRollback;
      case DOWNLOAD:
      case UPGRADE:
      case COMMIT:
        // 下载、升级、回滚、确认都属于软件升级操作
        return ActionType.deviceSoftwareUpgrade;
      default:
        // 其他未知类型使用通用的 updateDevice
        return ActionType.updateDevice;
    }
  }

  /**
   * 将任务状态映射为设备操作状态
   * 
   * 用于构建 detail JSON 中的设备状态信息（与 system-change 通知格式保持一致）
   * 
   * 映射关系:
   * - COMPLETED → COMPLETE
   * - FAILED → FAIL
   * - RUNNING → BACKING_UP / RESTORING / DOWNLOADING / ACTIVE (根据任务类型)
   * - PENDING → PENDING
   * - SCHEDULED → SCHEDULED
   * - CANCELLED → CANCELLED
   * 
   * @param status 任务状态
   * @return 设备操作状态字符串
   */
  private String mapTaskStatusToDeviceState(DeviceTask.TaskStatus status) {
    switch (status) {
      case COMPLETED:
        return "COMPLETE";
      case FAILED:
        return "FAIL";
      case RUNNING:
        // 对于 RUNNING 状态，可以根据具体任务类型返回不同的状态
        // 这里统一返回通用状态，如需细化可以传入 taskType 参数
        return "RUNNING";
      case PENDING:
        return "PENDING";
      case SCHEDULED:
        return "SCHEDULED";
      case CANCELLED:
        return "CANCELLED";
      default:
        return status.name();
    }
  }

  // ==================== 批次级别的 TaskInfo 通知 ====================

  /**
   * 通知批次任务开始（创建批次级别的父任务）
   * 
   * 用于批量备份/恢复操作，即使只有一个设备也创建批次任务
   * 
   * @param batchId         批次ID（可以是自动生成的UUID）
   * @param batchName       批次名称
   * @param taskType        任务类型（BACKUP/RESTORE）
   * @param deviceTasks     设备任务列表
   * @param batchActionTime 批次统一的 actionTime（毫秒时间戳）
   * @param createdBy       批次创建者（从 Batch.createdBy 读取，可为 null）
   * @return 批次的 actionTime，供子任务使用
   */
  public Long notifyBatchTaskStarted(
      String batchId,
      String batchName,
      DeviceTask.TaskType taskType,
      List<DeviceTask> deviceTasks,
      Long batchActionTime,
      String createdBy) {

    try {
      log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      log.info("📨 [TaskInfo批次通知] 创建批次任务");
      log.info("  批次ID: {}", batchId);
      log.info("  批次名称: {}", batchName);
      log.info("  任务类型: {}", taskType);
      log.info("  设备数量: {}", deviceTasks.size());

      // 确定 ActionType
      ActionType actionType = getActionTypeForTaskType(taskType);

      // 构建批次级别的 detail
      String batchDetail = buildBatchDetailJson(batchId, batchName, taskType, deviceTasks);

      // ⭐ 创建通知：优先使用 createdBy，回退到 getCurrentUser()
      // （创建通知必须有用户，不能传 null）
      String user = (createdBy != null && !createdBy.isEmpty()) ? createdBy : getCurrentUser();
      log.info("  📝 [Debug] 批次创建通知 operator: {} (来源: {})", user,
          (createdBy != null && !createdBy.isEmpty()) ? "批次创建者" : "当前 HTTP 请求");

      // 创建批次任务消息
      TaskInfoMessage message = new TaskInfoMessage(
          user, // ✅ 使用真实用户
          ResourceType.device,
          actionType,
          batchDetail);

      // ⭐ 使用批次ID作为 resourceId（不加前缀，保持简洁）
      message.setResourceId(batchId);
      message.setResourceName(batchName);

      // ⭐ 使用统一的批次 actionTime
      if (batchActionTime != null) {
        message.setActionTime(batchActionTime);
      } else {
        batchActionTime = System.currentTimeMillis();
        message.setActionTime(batchActionTime);
      }

      // ⭐ 批次创建通知：successfully=false 表示任务未完成，不设置 endTime 表示任务进行中
      // 与 UPGRADE 批次保持一致，避免 TaskInfo 误解语义
      // TaskInfo 通过 endTime 是否为空来判断任务是否完成：
      // - endTime == null → 任务进行中（创建/运行中/调度中）
      // - endTime != null → 任务已结束（成功/失败）
      message.setSuccessfully(false); // ✅ 任务刚创建，未完成
      // ❌ 不设置 endTime → TaskInfo 知道任务还在进行中

      // 发送消息
      TaskInfoMessager.sendMessage(message);

      log.info("  ✅ 批次任务已通知 TaskInfo");
      log.info("  ResourceId: BATCH-{}", batchId);
      log.info("  ActionTime: {}", batchActionTime);
      log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

      return batchActionTime;

    } catch (Exception e) {
      log.error("发送批次TaskInfo通知失败: batchId={}", batchId, e);
      // 不抛出异常，避免影响主业务流程
      return batchActionTime != null ? batchActionTime : System.currentTimeMillis();
    }
  }

  /**
   * 通知批次任务完成（更新批次状态）
   * 
   * @param batchId         批次ID
   * @param batchName       批次名称
   * @param taskType        任务类型
   * @param deviceTasks     设备任务列表（包含最新状态）
   * @param batchActionTime 批次的 actionTime（与创建时相同）
   * @param createdBy       批次创建者（从 Batch.createdBy 读取）
   */
  public void notifyBatchTaskCompleted(
      String batchId,
      String batchName,
      DeviceTask.TaskType taskType,
      List<DeviceTask> deviceTasks,
      Long batchActionTime,
      String createdBy) {

    try {
      log.info("📨 [TaskInfo批次通知] 更新批次任务状态");
      log.info("  批次ID: {}", batchId);
      log.info("  批次ActionTime: {}", batchActionTime);

      // 统计批次状态
      long completedCount = deviceTasks.stream()
          .filter(t -> t.getStatus() == DeviceTask.TaskStatus.COMPLETED)
          .count();
      long failedCount = deviceTasks.stream()
          .filter(t -> t.getStatus() == DeviceTask.TaskStatus.FAILED)
          .count();
      long runningCount = deviceTasks.stream()
          .filter(t -> t.getStatus() == DeviceTask.TaskStatus.RUNNING)
          .count();

      boolean allCompleted = (completedCount + failedCount) == deviceTasks.size();
      boolean batchSuccess = failedCount == 0 && allCompleted;

      ActionType actionType = getActionTypeForTaskType(taskType);
      String batchDetail = buildBatchDetailJson(batchId, batchName, taskType, deviceTasks);

      // ⭐ 更新通知：直接使用 createdBy（即使是 null）
      // 传入 null 时，TaskInfo 不会更新 who 字段，保持原值
      String user = createdBy;
      if (user != null && !user.isEmpty()) {
        log.info("  📝 [Debug] 使用批次创建者: {}", user);
      } else {
        log.info("  📝 [Debug] createdBy 为空，传入 null 保持 TaskInfo 原有 operator");
      }

      TaskInfoMessage message = new TaskInfoMessage(
          user, // ✅ 使用真实用户
          ResourceType.device,
          actionType,
          batchDetail);

      // ✅ 统一 resourceId 格式：直接使用 batchId（与创建通知保持一致）
      message.setResourceId(batchId);
      message.setResourceName(batchName);
      message.setActionTime(batchActionTime); // ⭐ 使用相同的 actionTime

      // ⭐ 设置结束时间（如果批次已完成）
      Long endTime = null;
      if (allCompleted) {
        endTime = System.currentTimeMillis();
        message.setEndTime(endTime);
        log.info("  📝 [Debug] 批次已完成，设置endTime: {}", endTime);
      } else {
        log.info("  📝 [Debug] 批次未完成，不设置endTime (completed={}, failed={}, running={})",
            completedCount, failedCount, runningCount);
      }
      message.setSuccessfully(batchSuccess);

      if (!batchSuccess && allCompleted) {
        message.setErrorReason(String.format("%d device task(s) failed in batch", failedCount));
      }

      TaskInfoMessager.sendMessage(message);

      log.info("  ✅ 批次状态已更新: 完成={}, 失败={}, 进行中={}",
          completedCount, failedCount, runningCount);

    } catch (Exception e) {
      log.error("发送批次完成通知失败: batchId={}", batchId, e);
    }
  }

  /**
   * 构建批次级别的 detail JSON
   */
  private String buildBatchDetailJson(
      String batchId,
      String batchName,
      DeviceTask.TaskType taskType,
      List<DeviceTask> deviceTasks) {

    try {
      Map<String, Object> detail = new HashMap<>();

      // 批次基本信息
      detail.put("batchId", batchId);
      detail.put("batchName", batchName);
      detail.put("taskType", taskType.name()); // ✅ BACKUP/RESTORE/ROLLBACK 使用简单格式，与 ActionType 保持一致
      detail.put("deviceCount", deviceTasks.size());

      // ⭐ 标记为根任务
      detail.put("root", true);
      detail.put("groupId", parseBatchIdToLong(batchId));
      detail.put("objectId", batchId);
      detail.put("objectType", "BATCH");

      // 统计信息
      long completedCount = deviceTasks.stream()
          .filter(t -> t.getStatus() == DeviceTask.TaskStatus.COMPLETED)
          .count();
      long failedCount = deviceTasks.stream()
          .filter(t -> t.getStatus() == DeviceTask.TaskStatus.FAILED)
          .count();
      long runningCount = deviceTasks.stream()
          .filter(t -> t.getStatus() == DeviceTask.TaskStatus.RUNNING)
          .count();
      long pendingCount = deviceTasks.stream()
          .filter(t -> t.getStatus() == DeviceTask.TaskStatus.PENDING ||
              t.getStatus() == DeviceTask.TaskStatus.SCHEDULED)
          .count();

      detail.put("successCount", completedCount); // ✅ 改名为 successCount 与升级批次一致
      detail.put("failedCount", failedCount);
      detail.put("runningCount", runningCount);
      detail.put("pendingCount", pendingCount);

      // 批次状态
      String batchStatus;
      if (completedCount + failedCount == deviceTasks.size()) {
        batchStatus = failedCount == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS";
      } else if (runningCount > 0) {
        batchStatus = "RUNNING";
      } else {
        batchStatus = "PENDING";
      }
      detail.put("status", batchStatus); // ✅ 改名为 status 与升级批次一致

      // ✅ 设备任务完整列表（与升级批次格式一致）
      List<Map<String, Object>> deviceTaskList = new ArrayList<>();
      for (DeviceTask task : deviceTasks) {
        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("taskId", task.getTaskId());
        taskInfo.put("batchId", task.getBatchId());
        taskInfo.put("workflowId", task.getWorkflowId());
        taskInfo.put("deviceId", task.getDeviceId());
        taskInfo.put("deviceName", task.getDeviceName());
        taskInfo.put("deviceIp", task.getDeviceIp());
        taskInfo.put("taskType", task.getTaskType().name());
        taskInfo.put("status", task.getStatus().name());
        taskInfo.put("filePath", task.getFilePath());
        taskInfo.put("backupFilePath", task.getBackupFilePath());
        taskInfo.put("backupFileName", task.getBackupFileName());
        taskInfo.put("retryCount", task.getRetryCount());
        taskInfo.put("errorMessage", task.getErrorMessage());
        taskInfo.put("createdTime", task.getCreatedTime() != null ? task.getCreatedTime().toString() : null);
        taskInfo.put("updatedTime", task.getUpdatedTime() != null ? task.getUpdatedTime().toString() : null);
        taskInfo.put("startedTime", task.getStartedTime() != null ? task.getStartedTime().toString() : null);
        taskInfo.put("completedTime", task.getCompletedTime() != null ? task.getCompletedTime().toString() : null);
        deviceTaskList.add(taskInfo);
      }
      detail.put("deviceTasks", deviceTaskList); // ✅ 改名为 deviceTasks 与升级批次一致

      return objectMapper.writeValueAsString(detail);

    } catch (JsonProcessingException e) {
      log.error("构建批次detail JSON失败", e);
      return "{}";
    }
  }

  /**
   * 根据任务类型获取对应的 ActionType
   */
  private ActionType getActionTypeForTaskType(DeviceTask.TaskType taskType) {
    switch (taskType) {
      case BACKUP:
        return ActionType.deviceDatabaseBackup;
      case RESTORE:
        return ActionType.deviceDatabaseRestore;
      case ROLLBACK:
        return ActionType.deviceDatabaseRollback;
      case DOWNLOAD:
      case UPGRADE:
      case COMMIT:
        return ActionType.deviceSoftwareUpgrade;
      default:
        return ActionType.updateDevice;
    }
  }

  /**
   * 将批次ID转换为Long类型的groupId
   */
  private Long parseBatchIdToLong(String batchId) {
    try {
      // 尝试解析为数字
      return Long.parseLong(batchId);
    } catch (NumberFormatException e) {
      // 如果不是数字，使用hashCode的绝对值（避免负数groupId）
      return Math.abs((long) batchId.hashCode());
    }
  }

  // ========== 批次升级专用通知方法 ==========

  /**
   * 通知 TaskInfo 创建批次升级任务
   * 
   * @param batchId         批次ID
   * @param batchName       批次名称
   * @param batchActionTime 批次创建时间戳
   * @param deviceCount     设备数量
   * @param upgradeFilePath 升级文件路径
   */
  public void notifyBatchUpgradeCreated(
      String batchId,
      String batchName,
      Long batchActionTime,
      int deviceCount,
      String upgradeFilePath,
      String createdBy) {

    try {
      log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      log.info("📨 [TaskInfo] 创建批次升级任务");
      log.info("  批次ID: {}", batchId);
      log.info("  批次名称: {}", batchName);
      log.info("  设备数量: {}", deviceCount);
      log.info("  升级文件: {}", upgradeFilePath);

      // ⭐ 创建时只发送基本信息（此时还没有 DeviceTask）
      // 详细信息会在后续的批次更新通知中发送（由 Kafka 监听器触发）
      Map<String, Object> detail = new HashMap<>();
      detail.put("batchId", batchId);
      detail.put("batchName", batchName);
      detail.put("deviceCount", deviceCount);
      detail.put("upgradeFilePath", upgradeFilePath);
      detail.put("root", true);
      detail.put("groupId", parseBatchIdToLong(batchId));
      detail.put("status", "READY_TO_PROCEED"); // MANUAL 模式创建时状态
      detail.put("taskType", "BATCH_UPGRADE");
      String detailJson = objectMapper.writeValueAsString(detail);
      log.info("  ✓ 使用基本信息构建创建通知（详细信息将在任务执行后更新）");

      // ⭐ 使用批次创建者（确保与 batch.createdBy 一致）
      String user = (createdBy != null && !createdBy.isEmpty()) ? createdBy : getCurrentUser();
      log.info("  📝 [Debug] 批次升级创建通知 operator: {} (来源: {})", user,
          (createdBy != null && !createdBy.isEmpty()) ? "批次创建者" : "当前HTTP请求");

      // 创建 TaskInfo 消息
      TaskInfoMessage message = new TaskInfoMessage(
          user, // ✅ 使用真实用户
          ResourceType.device,
          ActionType.deviceSoftwareUpgrade, // 批次升级使用软件升级类型
          detailJson);

      // ⭐ 关键绑定字段
      message.setResourceId(batchId); // 使用 batchId 作为 resourceId
      message.setResourceName(batchName); // 使用 batchName 作为显示名称
      message.setActionTime(batchActionTime); // 使用批次创建时间戳
      message.setSuccessfully(false); // ✅ 修复：任务刚创建，未完成

      // 发送消息
      TaskInfoMessager.sendMessage(message);

      log.info("  ✅ TaskInfo 通知已发送（包含完整详情）");
      log.info("  ResourceId: {}", batchId);
      log.info("  ResourceName: {}", batchName);
      log.info("  ActionTime: {}", batchActionTime);
      log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

    } catch (Exception e) {
      log.error("发送批次升级 TaskInfo 通知失败: batchId={}", batchId, e);
      // 不抛出异常，避免影响主业务流程
    }
  }

  /**
   * 通知 TaskInfo 更新批次升级任务状态
   * 
   * @param batchId         批次ID
   * @param batchName       批次名称
   * @param batchActionTime 批次创建时间戳
   * @param status          批次状态
   * @param successCount    成功数量
   * @param failedCount     失败数量
   * @param errorMessage    错误信息（可选）
   */
  /**
   * 通知 TaskInfo: 批次升级删除
   * 
   * @param batch 批次对象
   */
  /**
   * ⚠️ 已废弃：此方法硬编码了 taskType="BATCH_UPGRADE"，导致 BACKUP/RESTORE 批次删除通知错误
   * ✅ 请使用 notifyBatchDeleted() 方法，它会根据批次类型动态设置 taskType 和 ActionType
   * 
   * @deprecated 使用 {@link #notifyBatchDeleted(devicemaintenance.entity.Batch)} 替代
   */
  @Deprecated
  public void notifyBatchUpgradeDeleted(devicemaintenance.entity.Batch batch) {
    try {
      log.info("📨 [TaskInfo] 删除批次升级任务");
      log.info("  批次ID: {}", batch.getBatchId());
      log.info("  批次名称: {}", batch.getBatchName());

      // 构建 detail JSON (标记为删除)
      Map<String, Object> detail = new HashMap<>();
      detail.put("batchId", batch.getBatchId());
      detail.put("batchName", batch.getBatchName());
      detail.put("root", true);
      detail.put("groupId", parseBatchIdToLong(batch.getBatchId()));
      detail.put("status", "DELETED");
      detail.put("taskType", "BATCH_UPGRADE");
      detail.put("deleted", true); // 在 detail 中标记为删除
      detail.put("deletedTime", System.currentTimeMillis());

      String detailJson = objectMapper.writeValueAsString(detail);

      // ⭐ 使用批次创建者作为删除操作的 operator
      String user = (batch.getCreatedBy() != null && !batch.getCreatedBy().isEmpty())
          ? batch.getCreatedBy()
          : getCurrentUser();
      log.info("  📝 [Debug] 批次删除通知 operator: {} (来源: {})", user,
          (batch.getCreatedBy() != null && !batch.getCreatedBy().isEmpty()) ? "批次创建者" : "当前请求");

      // 创建删除消息 - 使用 ActionType.delete
      TaskInfoMessage message = new TaskInfoMessage(
          user, // ✅ 使用真实用户
          ResourceType.device,
          ActionType.delete, // ⭐ 使用 delete 动作类型
          detailJson);

      // 使用相同的绑定字段定位要删除的任务
      message.setResourceId(batch.getBatchId());
      message.setResourceName(batch.getBatchName());
      message.setActionTime(batch.getBatchActionTime()); // 使用创建时的时间戳定位任务
      message.setEndTime(System.currentTimeMillis()); // 标记任务结束时间
      message.setSuccessfully(false); // 删除操作不算"成功"

      // 发送消息
      TaskInfoMessager.sendMessage(message);

      log.info("  ✓ 已发送批次删除通知 (ActionType.delete)");

    } catch (Exception e) {
      log.error("发送批次删除通知失败: batchId={}", batch.getBatchId(), e);
      throw new RuntimeException("Failed to notify TaskInfo about batch deletion", e);
    }
  }

  public void notifyBatchUpgradeCompleted(
      String batchId,
      String batchName,
      Long batchActionTime,
      String status,
      int successCount,
      int failedCount,
      String errorMessage) {

    try {
      log.info("📨 [TaskInfo] 更新批次升级任务状态");
      log.info("  批次ID: {}", batchId);
      log.info("  状态: {}", status);
      log.info("  成功: {}", successCount);
      log.info("  失败: {}", failedCount);

      // 构建 detail JSON
      Map<String, Object> detail = new HashMap<>();
      detail.put("batchId", batchId);
      detail.put("batchName", batchName);
      detail.put("root", true);
      detail.put("groupId", parseBatchIdToLong(batchId));
      detail.put("status", status);
      detail.put("successCount", successCount);
      detail.put("failedCount", failedCount);
      detail.put("taskType", "BATCH_UPGRADE");

      if (errorMessage != null) {
        detail.put("errorMessage", errorMessage);
      }

      String detailJson = objectMapper.writeValueAsString(detail);

      // 创建 TaskInfo 消息
      TaskInfoMessage message = new TaskInfoMessage(
          DEFAULT_USER,
          ResourceType.device,
          ActionType.deviceSoftwareUpgrade,
          detailJson);

      // ⭐ 使用相同的绑定字段
      message.setResourceId(batchId);
      message.setResourceName(batchName);
      message.setActionTime(batchActionTime); // 使用创建时的时间戳，确保能找到任务
      message.setSuccessfully("COMPLETED".equals(status) && failedCount == 0);

      // 发送消息
      TaskInfoMessager.sendMessage(message);

      log.info("  ✅ TaskInfo 状态更新已发送");

    } catch (Exception e) {
      log.error("发送批次升级完成 TaskInfo 通知失败: batchId={}", batchId, e);
    }
  }

  /**
   * 发送通用的 TaskInfo 通知（支持自定义 detail JSON）
   * 用于需要发送完整批次详情的场景
   */
  public void sendTaskInfoNotification(
      String batchId,
      String batchName,
      Long batchActionTime,
      String taskType,
      String detailJson,
      boolean successfully) {
    sendTaskInfoNotification(batchId, batchName, batchActionTime, taskType, detailJson, successfully, null);
  }

  /**
   * 发送 TaskInfo 批次通知（支持指定用户）
   */
  public void sendTaskInfoNotification(
      String batchId,
      String batchName,
      Long batchActionTime,
      String taskType,
      String detailJson,
      boolean successfully,
      String createdBy) {

    try {
      log.info("📨 [TaskInfo] 发送批次通知");
      log.info("  批次ID: {}", batchId);
      log.info("  批次名称: {}", batchName);
      log.info("  批次ActionTime: {}", batchActionTime);
      log.info("  任务类型: {}", taskType);

      // ⭐ 直接使用 createdBy（即使是 null）
      // 传入 null 时，TaskInfo 不会更新 who 字段，保持原值
      String user = createdBy;
      if (user != null && !user.trim().isEmpty()) {
        log.info("  📝 [Debug] 使用批次创建者: {}", user);
      } else {
        log.info("  📝 [Debug] createdBy 为空，传入 null 保持 TaskInfo 原有 operator");
      }

      // 创建 TaskInfo 消息
      TaskInfoMessage message = new TaskInfoMessage(
          user, // ✅ 使用真实用户
          ResourceType.device,
          ActionType.deviceSoftwareUpgrade,
          detailJson);

      message.setResourceId(batchId);
      message.setResourceName(batchName);
      message.setActionTime(batchActionTime);
      message.setSuccessfully(successfully);

      // ⭐ 设置结束时间（如果批次已完成）
      if (successfully) {
        Long endTime = System.currentTimeMillis();
        message.setEndTime(endTime);
        log.info("  📝 [Debug] 批次已完成，设置endTime: {}", endTime);
      } else {
        log.info("  📝 [Debug] 批次未完成，不设置endTime (successfully={})", successfully);
      }

      // 发送消息
      TaskInfoMessager.sendMessage(message);

      log.info("  ✅ TaskInfo 通知已发送（endTime={}）", message.getEndTime());

    } catch (Exception e) {
      log.error("发送 TaskInfo 通知失败: batchId={}, taskType={}", batchId, taskType, e);
    }
  }

  /**
   * 更新Batch任务的detail（当删除了Batch中的某个设备任务时）
   * 
   * @param batchId         批次ID
   * @param batchName       批次名称
   * @param batchActionTime 批次创建时间（必须与创建时相同）
   * @param taskType        任务类型
   * @param updatedTaskList 更新后的任务列表
   */
  public void notifyBatchTaskUpdated(
      String batchId,
      String batchName,
      Long batchActionTime,
      DeviceTask.TaskType taskType,
      List<DeviceTask> updatedTaskList) {

    try {
      log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      log.info("📨 [TaskInfo批次通知] 更新批次任务detail");
      log.info("  批次ID: {}", batchId);
      log.info("  批次名称: {}", batchName);
      log.info("  任务类型: {}", taskType);
      log.info("  剩余任务数: {}", updatedTaskList.size());

      // 构建更新后的detail
      String updatedDetail = buildBatchDetailJson(batchId, batchName, taskType, updatedTaskList);

      ActionType actionType = getActionTypeForTaskType(taskType);

      // ⭐ 更新通知不设置 operator（传入 null），保持 TaskInfo 原有的 operator 值
      // （避免查询数据库，性能优化；TaskInfo 会保留第一次创建时设置的 operator）
      String user = null;
      log.info("  📝 [Debug] 更新通知不设置 operator（保持原值）");

      TaskInfoMessage message = new TaskInfoMessage(
          user, // ✅ 使用真实用户
          ResourceType.device,
          actionType,
          updatedDetail);

      // ⭐ 关键: 使用创建时的 batchId 和 actionTime，确保能关联到原任务
      message.setResourceId(batchId);
      message.setResourceName(batchName);
      message.setActionTime(batchActionTime); // ⭐ 必须与创建时相同

      // 不设置endTime，因为Batch可能还在进行中（只是减少了任务）
      message.setSuccessfully(true);

      TaskInfoMessager.sendMessage(message);

      log.info("  ✅ Batch detail 更新通知已发送");
      log.info("  ResourceId: {}", batchId);
      log.info("  ActionTime: {}", batchActionTime);
      log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

    } catch (Exception e) {
      log.error("发送Batch更新通知失败: batchId={}", batchId, e);
    }
  }

  /**
   * 标记Batch为已删除（手动删除Batch时调用）
   * 
   * 注意: 这不是删除TaskInfo中的任务对象，而是更新Batch的detail，标记为已删除
   * 
   * @param batch 批次对象
   */
  public void notifyBatchDeleted(devicemaintenance.entity.Batch batch) {
    try {
      log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      log.info("🗑️ [TaskInfo批次通知] 标记批次为已删除");
      log.info("  批次ID: {}", batch.getBatchId());
      log.info("  批次名称: {}", batch.getBatchName());

      // 构建detail，标记为已删除
      Map<String, Object> detail = new HashMap<>();
      detail.put("batchId", batch.getBatchId());
      detail.put("batchName", batch.getBatchName());
      detail.put("batchType", batch.getBatchType().name());

      // ✅ taskType: UPGRADE保持BATCH_UPGRADE，BACKUP/RESTORE使用简单格式
      if (batch.getBatchType() == devicemaintenance.entity.Batch.BatchType.UPGRADE) {
        detail.put("taskType", "BATCH_UPGRADE");
      } else {
        detail.put("taskType", batch.getBatchType().name()); // BACKUP 或 RESTORE
      }

      detail.put("deleted", true); // ⭐ 标记为已删除
      detail.put("deletedTime", java.time.LocalDateTime.now().toString());
      detail.put("deletedTimestamp", System.currentTimeMillis());
      detail.put("deviceTasks", new ArrayList<>()); // 空任务列表

      String detailJson = objectMapper.writeValueAsString(detail);

      // 根据批次类型确定ActionType
      ActionType actionType;
      switch (batch.getBatchType()) {
        case UPGRADE:
          actionType = ActionType.deviceSoftwareUpgrade;
          break;
        case BACKUP:
          actionType = ActionType.deviceDatabaseBackup;
          break;
        case RESTORE:
          actionType = ActionType.deviceDatabaseRestore;
          break;
        default:
          actionType = ActionType.updateDevice;
      }

      // ⭐ 使用批次创建者作为删除操作的 operator
      String user = (batch.getCreatedBy() != null && !batch.getCreatedBy().isEmpty())
          ? batch.getCreatedBy()
          : getCurrentUser();
      log.info("  📝 [Debug] BACKUP/RESTORE 批次删除通知 operator: {} (来源: {})", user,
          (batch.getCreatedBy() != null && !batch.getCreatedBy().isEmpty()) ? "批次创建者" : "当前请求");

      TaskInfoMessage message = new TaskInfoMessage(
          user, // ✅ 使用真实用户
          ResourceType.device,
          actionType, // ⭐ 使用原来的ActionType，不是delete
          detailJson);

      // ⭐ 关键: 使用创建时的 batchId 和 actionTime
      message.setResourceId(batch.getBatchId());
      message.setResourceName(batch.getBatchName());
      message.setActionTime(batch.getBatchActionTime()); // ⭐ 必须与创建时相同
      message.setEndTime(System.currentTimeMillis());
      message.setSuccessfully(true); // 任务本身是成功的，只是被删除了

      TaskInfoMessager.sendMessage(message);

      log.info("  ✅ Batch 删除标记已发送（更新detail）");
      log.info("  ResourceId: {}", batch.getBatchId());
      log.info("  ActionTime: {}", batch.getBatchActionTime());
      log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

    } catch (Exception e) {
      log.error("发送Batch删除标记失败: batchId={}", batch.getBatchId(), e);
    }
  }
}
