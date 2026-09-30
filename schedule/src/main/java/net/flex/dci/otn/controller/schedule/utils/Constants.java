package net.flex.dci.otn.controller.schedule.utils;

import java.time.format.DateTimeFormatter;

/**
 * 2026/5/31
 *
 * @author musa
 * @version 1.0
 **/
public class Constants {

    public static String BACKUP_MONGODB_DIR = "mongodb";

    public static String BACKUP_MONGODB_TAR = "mongodb.tar.gz";

    public static String BACKUP_MYSQL_DIR = "mysql";

    public static String BACKUP_MYSQL_TAR = "mysql.tar.gz";

    public static String BACKUP_MYSQL_FILE = "ctrl-db.sql";

    public static Integer GLOBAL_BACKUP_TIMEOUT_MINUTES = 30;

    public static String CONTROLLER_DB_BACKUP_CRON = "0 0 1 * * ?";

    public static Integer BACKUP_TIMEOUT_MINUTES = 15;

    public static final String RESULT_SUCCESS = "success";

    public static final DateTimeFormatter BACKUP_DIR_FORMAT = DateTimeFormatter.ofPattern(
            "yyyyMMdd-HHmmss");
}
