//package net.flex.dci.otn.controller.schedule.task.detail.controller;
//
//import static net.flex.dci.otn.controller.schedule.utils.ScheduleConstants.BACKUP_FOLDER;
//import static net.flex.dci.otn.controller.schedule.utils.ScheduleConstants.RESTORE_FOLDER;
//
//import java.io.File;
//import java.net.URLEncoder;
//import java.util.List;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otc.common.exception.CommonExceptionType;
//import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
//import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule.TaskType;
//import net.flex.dci.otn.controller.schedule.db.config.MongoConfiguration;
//import net.flex.dci.otn.controller.schedule.properties.MysqlProperties;
//import net.flex.dci.otn.controller.schedule.task.detail.DefaultTaskImpl;
//import net.flex.dci.otn.controller.schedule.utils.AuxTools;
//import net.flex.dci.otn.controller.schedule.utils.ConfigLoader;
//import net.flex.dci.otn.controller.schedule.utils.ScheduleConstants.CTRL_DATABASE_CMD;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.controller.rev190906.TaskData1;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.attributes.TaskData;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ControllerImpl extends DefaultTaskImpl {
//
//    public static final String RESULT_SUCCESS = "success";
//
//    private final ConfigLoader configLoader;
//    private final MongoConfiguration mongoConfig;
//    private final FileManager fileManager;
//    private final MysqlProperties mysqlConfig;
//
//    @Override
//    public void process(MoSchedule moSchedule) {
//        TaskData preData = moSchedule.convert(true).build().getTaskData();
//        TaskData1 taskData = preData.getAugmentation(TaskData1.class);
//
//        switch (moSchedule.getOperation().getSimpleName()) {
//            case CTRL_DATABASE_CMD.DB_BACKUP:
//                dbBackup(taskData.getFtpServerName(), taskData.getName());
//                break;
//            case CTRL_DATABASE_CMD.DB_RESTORE:
//                dbRestore(taskData.getFtpServerName(), taskData.getName());
//                break;
//            default:
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        "unknown Controller-Data action " + moSchedule.getOperation()
//                                .getSimpleName());
//        }
//    }
//
//    @Override
//    public TaskType getTaskType() {
//        return MoSchedule.TaskType.controllerData;
//    }
//
//    private void dbBackup(String ftpServerName, String remoteBackupFolder) {
//        if (!configLoader.isAnyToolAvailable()) {
//            log.error("No database CLI tools available. Cannot perform backup.");
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "Backup tools not available");
//        }
//
//        String localDir = BACKUP_FOLDER + File.separator;
//        fileManager.cleanFolder(localDir);
//
//        if (configLoader.isMongoBackupAvailable()) {
//            backupMongo(localDir);
//        } else {
//            log.warn("MongoDB dump tool not available, skipping MongoDB backup");
//        }
//
//        if (configLoader.isMysqlBackupAvailable()) {
//            backupMysql(localDir);
//        } else {
//            log.warn("MySQL dump tool not available, skipping MySQL backup");
//        }
//
//        File zipFile = fileManager.zipFile();
//
//        if (configLoader.isFtpAvailable() && ftpServerName != null && !ftpServerName.isEmpty()) {
//            fileManager.zipAndUpload(ftpServerName, remoteBackupFolder);
//            fileManager.cleanupOldSftpFiles(ftpServerName, remoteBackupFolder,
//                    configLoader.getSftpFileTTL());
//        } else {
//            log.info("No FTP configured. Backup saved locally: {}", zipFile.getAbsolutePath());
//            fileManager.cleanupOldLocalFiles(configLoader.getLocalBackupTTL());
//        }
//    }
//
//    private void dbRestore(String ftpServerName, String zipFileName) {
//        if (!configLoader.isAnyToolAvailable()) {
//            log.error("No database CLI tools available. Cannot perform restore.");
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "Restore tools not available");
//        }
//
//        fileManager.cleanFolder(RESTORE_FOLDER + File.separator);
//
//        String restoreFolder;
//        if (configLoader.isFtpAvailable() && ftpServerName != null && !ftpServerName.isEmpty()) {
//            restoreFolder = fileManager.getRestoreFile(ftpServerName, zipFileName);
//        } else {
//            restoreFolder = fileManager.findLocalRestoreFile(zipFileName);
//        }
//
//        if (configLoader.isMongoRestoreAvailable()) {
//            restoreMongo(restoreFolder);
//        } else {
//            log.warn("MongoDB restore tool not available, skipping MongoDB restore");
//        }
//
//        if (configLoader.isMysqlRestoreAvailable()) {
//            restoreMysql(restoreFolder);
//        } else {
//            log.warn("MySQL restore tool not available, skipping MySQL restore");
//        }
//    }
//
//    private void backupMongo(String backupDir) {
//        String cmd = String.format(
//                "%s --uri \"%s\" --out %s",
//                configLoader.getMongoDump(),
//                getMongoUri(),
//                backupDir
//        );
//
//        log.info("Executing MongoDB backup command: {}", cmd);
//        AuxTools.execLocal(cmd);
//    }
//
//    private void backupMysql(String backupDir) {
//        MysqlAux mysqlAux = new MysqlAux(mysqlConfig.getUrl());
//
//        String primaryHost = getPrimaryHost(mysqlAux);
//
//        String hostOnly = mysqlAux.getHostOnly(primaryHost);
//        int port = mysqlAux.getPort(primaryHost);
//        String db = mysqlAux.getDatabase();
//
//        String outputFile = backupDir + "/" + db + ".sql";
//
//        String cmd = buildMysqlDumpCommand(hostOnly, port, db, mysqlConfig.getUsername(),
//                mysqlConfig.getPassword(), outputFile);
//        log.info("Executing MySQL backup: {}", cmd);
//        AuxTools.execLocal(cmd);
//    }
//
//    private String getPrimaryHost(MysqlAux mysqlAux) {
//        List<String> hosts = mysqlAux.getHosts();
//
//        String primaryHost;
//        if (hosts.size() == 1) {
//            primaryHost = hosts.get(0);
//        } else {
//            primaryHost = mysqlAux.detectPrimary(hosts, mysqlConfig.getUsername(),
//                    mysqlConfig.getPassword());
//        }
//        return primaryHost;
//    }
//
//    private String buildMysqlDumpCommand(String host, int port, String db, String user,
//            String password, String outputFile) {
//        if (user != null && !user.isEmpty()) {
//            return String.format("%s -h%s -P%d -u%s -p%s --result-file=%s %s",
//                    configLoader.getMysqlDump(), host, port, user, password, outputFile, db);
//        } else {
//            return String.format("%s -h%s -P%d --result-file=%s %s",
//                    configLoader.getMysqlDump(), host, port, outputFile, db);
//        }
//    }
//
//    private String buildMysqlRestoreCommand(String host, int port, String db, String user,
//            String password, String sql) {
//        String dbPart = db != null ? db : "";
//        if (user != null && !user.isEmpty()) {
//            return String.format("%s -h%s -P%d -u%s -p%s %s -e \"%s\"",
//                    configLoader.getMysqlClient(), host, port, user, password, dbPart, sql);
//        } else {
//            return String.format("%s -h%s -P%d %s -e \"%s\"",
//                    configLoader.getMysqlClient(), host, port, dbPart, sql);
//        }
//    }
//
//    private void restoreMysql(String folderName) {
//        MysqlAux mysqlAux = new MysqlAux(mysqlConfig.getUrl());
//        String db = mysqlAux.getDatabase();
//        String sqlFile = folderName + File.separator + db + ".sql";
//
//        String primaryHost = getPrimaryHost(mysqlAux);
//
//        String hostOnly = mysqlAux.getHostOnly(primaryHost);
//        int port = mysqlAux.getPort(primaryHost);
//
//        AuxTools.execLocal(buildMysqlRestoreCommand(hostOnly, port, null, mysqlConfig.getUsername(),
//                mysqlConfig.getPassword(),
//                "DROP DATABASE IF EXISTS " + db + ";"));
//        AuxTools.execLocal(buildMysqlRestoreCommand(hostOnly, port, null, mysqlConfig.getUsername(),
//                mysqlConfig.getPassword(),
//                "CREATE DATABASE " + db + ";"));
//
//        AuxTools.execLocal(buildMysqlRestoreCommand(hostOnly, port, db, mysqlConfig.getUsername(),
//                mysqlConfig.getPassword(),
//                "< " + sqlFile));
//    }
//
//    private void restoreMongo(String folderName) {
//        String mongoRestoreDir = folderName + File.separator + mongoConfig.getDatabase();
//        String cmd = String.format(
//                "%s --uri \"%s\" --db %s --drop %s",
//                configLoader.getMongoRestore(),
//                getMongoUri(),
//                mongoConfig.getDatabase(),
//                mongoRestoreDir
//        );
//
//        log.info("Executing MongoDB restore command: {}", cmd);
//        AuxTools.execLocal(cmd);
//    }
//
//    private String getMongoUri() {
//        boolean authEnabled =
//                mongoConfig.getUser() != null && !mongoConfig.getUser().isEmpty()
//                        && mongoConfig.getPwd() != null && !mongoConfig.getPwd().isEmpty();
//
//        String user = "";
//        String pwd = "";
//        try {
//            if (authEnabled) {
//                user = URLEncoder.encode(mongoConfig.getUser(), "UTF-8");
//                pwd = URLEncoder.encode(mongoConfig.getPwd(), "UTF-8");
//            }
//        } catch (Exception e) {
//            log.error("Error encoding MongoDB credentials", e);
//            throw new RuntimeException(e);
//        }
//        String servers = mongoConfig.getServers();
//        String database = mongoConfig.getDatabase();
//        String authDb = "admin";
//
//        if (!authEnabled) {
//            return String.format("mongodb://%s/%s", servers, database);
//        }
//
//        return String.format(
//                "mongodb://%s:%s@%s/%s?authSource=%s",
//                user,
//                pwd,
//                servers,
//                database,
//                authDb
//        );
//    }
//}
