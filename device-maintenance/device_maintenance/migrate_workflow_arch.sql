-- ══════════════════════════════════════════════════════════════════
-- 工作流架构重构 - 数据库迁移脚本
-- ══════════════════════════════════════════════════════════════════
-- 
-- 目的：简化 UpgradeWorkflow 实体，移除冗余字段
-- 原则：
--   - UpgradeWorkflow：只存流程控制信息
--   - DeviceTask：存所有任务细节
--   - UpgradeBatch：存全局配置
--
-- ══════════════════════════════════════════════════════════════════

USE sotn;

-- ──────────────────────────────────────────────────────────────────
-- 1. dm_device_task 表：增加关联字段
-- ──────────────────────────────────────────────────────────────────

-- 增加 workflow_id 字段（关联工作流）
ALTER TABLE dm_device_task 
ADD COLUMN workflow_id VARCHAR(64) NULL COMMENT '关联的工作流ID（如果属于工作流）'
AFTER batch_name;

-- 修改 task_type 允许 NULL（IDLE 状态时可能没有 taskType）
ALTER TABLE dm_device_task 
MODIFY COLUMN task_type VARCHAR(20) NULL COMMENT '任务类型';

-- ──────────────────────────────────────────────────────────────────
-- 2. dm_upgrade_workflow 表：删除冗余字段
-- ──────────────────────────────────────────────────────────────────

-- 删除设备信息（由 device_task 存储）
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS device_name;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS device_ip;

-- 删除重复的配置字段（由 batch 统一配置）
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS enable_download;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS enable_backup;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS enable_upgrade;

-- 删除详细状态字段（由 device_task 存储）
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS download_status;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS backup_status;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS upgrade_status;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS error_message;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS last_failed_step;

-- 删除时间戳（由 device_task 存储）
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS created_time;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS updated_time;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS started_time;
ALTER TABLE dm_upgrade_workflow DROP COLUMN IF EXISTS completed_time;

-- ──────────────────────────────────────────────────────────────────
-- 验证
-- ──────────────────────────────────────────────────────────────────

SELECT 
    'dm_upgrade_workflow剩余字段:' AS info,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'sotn'
  AND TABLE_NAME = 'dm_upgrade_workflow'
ORDER BY ORDINAL_POSITION;

SELECT 
    'dm_device_task新增字段:' AS info,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'sotn'
  AND TABLE_NAME = 'dm_device_task'
  AND COLUMN_NAME = 'workflow_id';

-- ══════════════════════════════════════════════════════════════════
-- 迁移完成
-- ══════════════════════════════════════════════════════════════════

