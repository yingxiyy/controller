SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'dm_device_task'
      AND column_name = 'previous_version'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE `dm_device_task` ADD COLUMN `previous_version` varchar(100) DEFAULT NULL COMMENT ''升级前版本'' AFTER `current_version`',
    'SELECT ''dm_device_task.previous_version already exists'' AS message'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
