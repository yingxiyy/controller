package net.flex.dci.otn.controller.schedule.utils;

/**
 * @version 1.0
 * @date 2023/4/13 10:16
 */
public class ScheduleConstants {
    public static final String BACKUP_FOLDER = "backup";
    public static final String RESTORE_FOLDER = "restore";

    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";
    public static final String COMPLETE = "COMPLETE";
    public static final String ACTIVE_FAIL = "ACTIVE_FAIL";
    public static final String ACTIVE_COMPLETE = "ACTIVE_COMPLETE";
    public static final String UNKNOWN = "UNKNOWN";
    public static final long CHECK_INTERVAL = 2000;
    public static final int CHECK_TOTAL = 100;
    public static final int CHECK_ACTIVE_TOTAL = 1000;


    public static class NE_OPERATE_CMD {

        public static final String SW_ROLLBACK = "SwRollback";

        public static final String SW_ACTIVATE = "SwActivate";

        public static final String SW_COMMIT = "SwCommit";

        public static final String SW_DOWNLOAD = "SwDownload";

        public static final String DB_LOG_UPLOAD = "DbLogUpload";

        public static final String DB_RESTORE = "DbRestore";

        public static final String DB_BACKUP = "DbBackup";

    }


    public static class CTRL_DATABASE_CMD {

        public static final String DB_BACKUP = "CtrlDbBackup";

        public static final String DB_RESTORE = "CtrlDbRestore";
    }
}
