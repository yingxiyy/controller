-- =====================================================
-- Device Maintenance 数据库表初始化脚本
-- 从上海2环境提取 (2025-12-18)
-- 目标环境: 北京 (117.134.16.112)
-- =====================================================

USE sotn;

-- =====================================================
-- 表1: dm_device_task - 设备任务表
-- =====================================================
CREATE TABLE IF NOT EXISTS `dm_device_task` (
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `batch_id` varchar(64) DEFAULT NULL COMMENT '所属批次ID（批次任务时才有值）',
  `batch_name` varchar(200) DEFAULT NULL COMMENT '批次名称，用于显示和TaskInfo绑定',
  `workflow_id` varchar(64) DEFAULT NULL COMMENT '关联的工作流ID（如果属于工作流）',
  `task_type` varchar(20) DEFAULT NULL COMMENT '任务类型',
  `device_id` varchar(100) NOT NULL COMMENT '设备ID',
  `device_name` varchar(200) DEFAULT NULL COMMENT '设备名称',
  `device_ip` varchar(50) DEFAULT NULL COMMENT '设备IP地址',
  `sftp_server_id` varchar(64) DEFAULT NULL COMMENT 'SFTP服务器ID（可选，未指定时使用默认配置）',
  `device_type` varchar(50) DEFAULT NULL COMMENT '设备类型',
  `vendor_type` varchar(50) DEFAULT NULL COMMENT '厂商类型',
  `vendor_name` varchar(50) DEFAULT NULL COMMENT '设备厂商名称（如 COHERENT, HUAWEI）',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：SCHEDULED-计划中（任务已创建，计划在之后执行），PENDING-待执行，RUNNING-执行中，COMPLETED-已完成，FAILED-失败，CANCELLED-已取消',
  `current_version` varchar(100) DEFAULT NULL COMMENT '当前版本',
  `previous_version` varchar(100) DEFAULT NULL COMMENT '升级前版本',
  `file_path` varchar(1000) DEFAULT NULL COMMENT '文件路径（升级文件路径或备份文件路径）',
  `backup_file_path` varchar(1000) DEFAULT NULL COMMENT '备份文件完整路径',
  `backup_file_name` varchar(500) DEFAULT NULL COMMENT '备份文件名',
  `restore_type` varchar(20) DEFAULT NULL COMMENT '恢复类型：FULL-全量恢复，CONFIG-配置恢复，DATA-数据恢复',
  `force_restore` tinyint(1) DEFAULT '0' COMMENT '是否强制恢复（覆盖当前配置）',
  `auto_restart` tinyint(1) DEFAULT '0' COMMENT '恢复后是否自动重启设备',
  `restart_status` varchar(20) DEFAULT NULL COMMENT '重启状态：PENDING-待重启，RESTARTING-重启中，SUCCESS-重启成功，FAILED-重启失败',
  `target_version` varchar(100) DEFAULT NULL COMMENT '目标版本（升级时使用）',
  `auto_backup` tinyint(1) DEFAULT '1' COMMENT '是否在升级前自动备份',
  `force_upgrade` tinyint(1) DEFAULT '0' COMMENT '是否强制升级（跳过版本检查）',
  `timeout_minutes` int DEFAULT '30' COMMENT '升级超时时间（分钟）',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `error_message` text COMMENT '错误信息',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `started_time` timestamp NULL DEFAULT NULL COMMENT '开始执行时间',
  `completed_time` timestamp NULL DEFAULT NULL COMMENT '完成时间',
  `scheduled_time` bigint DEFAULT NULL COMMENT '定时执行时间戳（毫秒）',
  `taskinfo_action_time` bigint DEFAULT NULL COMMENT 'TaskInfo通知使用的actionTime(毫秒时间戳)，用于删除通知时精确匹配原任务',
  `debug_payload` text COMMENT 'RPC请求payload（JSON格式，用于调试）',
  PRIMARY KEY (`task_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_task_type_status` (`task_type`,`status`),
  KEY `idx_task_batch_status` (`batch_id`,`status`),
  KEY `idx_scheduled_time` (`scheduled_time`),
  KEY `idx_status_scheduled_time` (`status`,`scheduled_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备任务表（统一管理升级、备份、恢复任务）';

-- =====================================================
-- 表2: dm_batch - 批次表
-- =====================================================
CREATE TABLE IF NOT EXISTS `dm_batch` (
  `batch_id` varchar(64) NOT NULL,
  `batch_name` varchar(200) NOT NULL,
  `batch_type` varchar(20) NOT NULL,
  `device_count` int NOT NULL DEFAULT '0',
  `status` varchar(50) NOT NULL DEFAULT 'PENDING',
  `status_detail` varchar(500) DEFAULT NULL,
  `success_count` int NOT NULL DEFAULT '0',
  `failed_count` int NOT NULL DEFAULT '0',
  `running_count` int NOT NULL DEFAULT '0',
  `detail` text,
  `error_message` text,
  `created_by` varchar(100) DEFAULT NULL,
  `created_time` datetime NOT NULL,
  `updated_time` datetime NOT NULL,
  `started_time` datetime DEFAULT NULL,
  `completed_time` datetime DEFAULT NULL,
  `batch_action_time` bigint DEFAULT NULL,
  `scheduled_time` bigint DEFAULT NULL COMMENT '定时执行时间戳（毫秒）',
  `scheduled_mode` varchar(20) DEFAULT 'IMMEDIATE',
  `sftp_server_name` varchar(100) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `debug` tinyint(1) NOT NULL DEFAULT '0',
  `base_path` varchar(500) DEFAULT NULL,
  `file_path` varchar(1000) DEFAULT NULL,
  `target_version` varchar(100) DEFAULT NULL COMMENT '目标版本（仅升级批次）',
  `backup_base_path` varchar(500) DEFAULT NULL,
  `execution_mode` varchar(20) DEFAULT NULL,
  `operation_interval` int DEFAULT '1',
  `max_retry_count` int DEFAULT '3',
  `enable_download` tinyint(1) DEFAULT '1',
  `enable_backup` tinyint(1) DEFAULT '1',
  `enable_upgrade` tinyint(1) DEFAULT '1',
  `enable_daily_task` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用每日循环执行',
  `daily_execution_time` varchar(10) DEFAULT NULL COMMENT '每日执行时间 (HH:mm格式，如 02:00)',
  PRIMARY KEY (`batch_id`),
  KEY `idx_batch_type` (`batch_type`),
  KEY `idx_status` (`status`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_batch_action_time` (`batch_action_time`),
  KEY `idx_batch_daily_task` (`batch_type`,`enable_daily_task`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- 表3: dm_batch_device - 批次设备关联表
-- =====================================================
CREATE TABLE IF NOT EXISTS `dm_batch_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `batch_id` varchar(64) NOT NULL COMMENT '批次任务ID',
  `device_id` varchar(100) NOT NULL COMMENT '设备ID',
  `device_name` varchar(200) DEFAULT NULL COMMENT '设备名称',
  `device_type` varchar(50) DEFAULT NULL COMMENT '设备类型',
  `vendor_type` varchar(50) DEFAULT NULL COMMENT '厂商类型',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `last_failed_step` varchar(20) DEFAULT NULL COMMENT '最后失败的步骤：DOWNLOAD/BACKUP/UPGRADE',
  `device_status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '设备状态：PENDING/IN_PROGRESS/SUCCESS/FAILED/MAX_RETRIES_EXCEEDED/CANCELLED',
  `last_updated_time` datetime DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_device` (`batch_id`,`device_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_batch_device_batch` (`batch_id`,`created_time`),
  KEY `idx_device_status` (`device_status`),
  KEY `idx_batch_retry` (`batch_id`,`retry_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批次设备关联表';

-- =====================================================
-- 表4: dm_upgrade_workflow - 升级工作流表
-- =====================================================
CREATE TABLE IF NOT EXISTS `dm_upgrade_workflow` (
  `workflow_id` varchar(64) NOT NULL COMMENT 'Workflow ID',
  `batch_id` varchar(64) NOT NULL COMMENT '所属批次ID',
  `device_id` varchar(64) NOT NULL COMMENT '设备ID',
  `current_step` varchar(20) DEFAULT NULL COMMENT '当前步骤',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT 'Workflow重试次数',
  `max_retry_count` int NOT NULL DEFAULT '3' COMMENT '最大重试次数',
  PRIMARY KEY (`workflow_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_batch_device` (`batch_id`,`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='升级工作流表';

-- =====================================================
-- 表5: dm_device_software_info - 设备软件版本信息表
-- =====================================================
CREATE TABLE IF NOT EXISTS `dm_device_software_info` (
  `info_id` varchar(64) NOT NULL COMMENT '信息ID',
  `device_id` varchar(100) NOT NULL COMMENT '设备ID',
  `device_name` varchar(200) DEFAULT NULL COMMENT '设备名称',
  `vendor_type` varchar(50) DEFAULT NULL COMMENT '厂商类型',
  `current_version` varchar(100) DEFAULT NULL COMMENT '当前软件版本',
  `previous_version` varchar(100) DEFAULT NULL COMMENT '上一个软件版本',
  `available_versions` text COMMENT '可用版本列表（JSON格式）',
  `last_upgrade_time` timestamp NULL DEFAULT NULL COMMENT '最后升级时间',
  `last_backup_time` timestamp NULL DEFAULT NULL COMMENT '最后备份时间',
  `backup_file_path` varchar(1000) DEFAULT NULL COMMENT '最新备份文件路径',
  `status` varchar(20) DEFAULT 'NORMAL' COMMENT '设备状态：NORMAL-正常，UPGRADING-升级中，ERROR-异常',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`info_id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_vendor_type` (`vendor_type`),
  KEY `idx_current_version` (`current_version`),
  KEY `idx_status` (`status`),
  KEY `idx_last_upgrade_time` (`last_upgrade_time`),
  KEY `idx_updated_time` (`updated_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备软件版本信息表';

-- =====================================================
-- 表6: dm_operation_history - 操作历史表
-- =====================================================
CREATE TABLE IF NOT EXISTS `dm_operation_history` (
  `history_id` varchar(64) NOT NULL COMMENT '历史记录ID',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型：DOWNLOAD-下载，BACKUP-备份，UPGRADE-升级，ROLLBACK-回滚，COMMIT-确认，RESTORE-恢复',
  `target_type` varchar(20) NOT NULL COMMENT '目标类型：BATCH-批次，TASK-任务，DEVICE-设备',
  `target_id` varchar(64) NOT NULL COMMENT '目标ID',
  `device_id` varchar(100) DEFAULT NULL COMMENT '设备ID',
  `device_name` varchar(200) DEFAULT NULL COMMENT '设备名称',
  `operation_status` varchar(20) NOT NULL COMMENT '操作状态：SUCCESS-成功，FAILED-失败，IN_PROGRESS-进行中',
  `operation_result` text COMMENT '操作结果详情',
  `error_message` text COMMENT '错误信息',
  `duration_ms` bigint DEFAULT NULL COMMENT '操作耗时（毫秒）',
  `operator` varchar(100) DEFAULT NULL COMMENT '操作人',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`history_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_target_type_id` (`target_type`,`target_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_operation_status` (`operation_status`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_history_device_time` (`device_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作历史表';

-- =====================================================
-- 表7: dm_task_execution_log - 任务执行日志表
-- =====================================================
CREATE TABLE IF NOT EXISTS `dm_task_execution_log` (
  `log_id` varchar(64) NOT NULL COMMENT '日志ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `batch_id` varchar(64) NOT NULL COMMENT '批次ID',
  `device_id` varchar(100) NOT NULL COMMENT '设备ID',
  `step_name` varchar(50) NOT NULL COMMENT '执行步骤：DOWNLOAD-下载，BACKUP-备份，UPGRADE-升级，ROLLBACK-回滚，COMMIT-确认，RESTORE-恢复',
  `step_status` varchar(20) NOT NULL COMMENT '步骤状态：STARTED-开始，IN_PROGRESS-进行中，SUCCESS-成功，FAILED-失败',
  `progress_percent` int DEFAULT '0' COMMENT '进度百分比',
  `log_message` text COMMENT '日志消息',
  `error_code` varchar(50) DEFAULT NULL COMMENT '错误代码',
  `error_message` text COMMENT '错误信息',
  `rpc_request` text COMMENT 'RPC请求内容',
  `rpc_response` text COMMENT 'RPC响应内容',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_step_name` (`step_name`),
  KEY `idx_step_status` (`step_status`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_log_task_step` (`task_id`,`step_name`,`created_time`),
  CONSTRAINT `fk_log_task` FOREIGN KEY (`task_id`) REFERENCES `dm_device_task` (`task_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务执行日志表';

-- =====================================================
-- 表结构创建完成
-- =====================================================
SELECT '✅ Device Maintenance 数据库表初始化完成' AS result;
SELECT CONCAT('表数量: ', COUNT(*)) AS table_count 
FROM information_schema.tables 
WHERE table_schema = 'sotn' AND table_name LIKE 'dm_%';

