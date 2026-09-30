drop table IF EXISTS task_info ;
drop table IF EXISTS task_info_detail  ;

-- sotn.task_info definition

CREATE TABLE IF NOT EXISTS `task_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `resourceId` varchar(255) NOT NULL,
    `resourceName` varchar(255) NOT NULL,
    `resourceType` int(11) NOT NULL,
    `who` varchar(255),
    `actionTime` datetime(3) NOT NULL,
	`endTime` datetime(3),
    `actionType` int(11) NOT NULL,
    `successfully` bit(1) NOT NULL,
    `errorReason` varchar(255) DEFAULT NULL,
    `hasDetail` bit(1) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `resourceId_index` (`resourceId`),
    INDEX `resourceName_index` (`resourceName`),
    INDEX `resourceId_Time_index` (`resourceId`, `actionTime`),
    INDEX `who_Time_index` (`who`, `actionTime`),
    INDEX `resourceName_Time_index` (`resourceName`, `actionTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `task_info_detail` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `detail` mediumblob,
    `taskInfoId` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `taskinfoId_index` (`taskInfoId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;