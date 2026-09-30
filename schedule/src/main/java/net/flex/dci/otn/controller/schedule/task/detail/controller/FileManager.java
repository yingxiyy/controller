//package net.flex.dci.otn.controller.schedule.task.detail.controller;
//
//import static net.flex.dci.otn.controller.schedule.task.detail.controller.ControllerImpl.RESULT_SUCCESS;
//import static net.flex.dci.otn.controller.schedule.utils.ScheduleConstants.BACKUP_FOLDER;
//import static net.flex.dci.otn.controller.schedule.utils.ScheduleConstants.RESTORE_FOLDER;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otc.common.exception.CommonExceptionType;
//import net.flex.dci.otc.controller.rpc.client.rpcs.FtpRpc;
//import net.flex.dci.otn.controller.schedule.utils.AuxTools;
//import net.flex.dci.otn.controller.schedule.utils.ConfigLoader;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.FileAttributes;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.GetOutput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ListOutput;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.PutOutput;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class FileManager {
//
//    private final ConfigLoader configLoader;
//    private final FtpRpc ftpRpc;
//
//    /**
//     * @param ftpServerName
//     * @param fileName
//     * @return the local folder path where the restore file is located
//     */
//    public String getRestoreFile(String ftpServerName, String fileName) {
//        String simpleFileName = validateExisting(fileName); //check remote file
//
//        GetOutput output = configLoader.getFtpRpc().downloadFile(ftpServerName,
//                fileName,
//                RESTORE_FOLDER);
//
//        if (output.getResult().equalsIgnoreCase(RESULT_SUCCESS)) {
//            unzipFile(RESTORE_FOLDER + File.separator + simpleFileName);
//        } else {
//            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR,
//                    String.format("Failed to download the restore DB file from FTP server: {} {}",
//                            ftpServerName, fileName)
//            );
//        }
//        return RESTORE_FOLDER;
//    }
//
//    public void unzipFile(String fileName) {
//        String cmd = String.format("tar -xzf %s -C %s",
//                fileName,
//                RESTORE_FOLDER
//        );
//        log.info("Unzipping the restore DB file with command: {}", cmd);
//        AuxTools.execLocal(cmd);
//    }
//
//    public void zipAndUpload(String ftpServerName, String remoteBackupFolder) {
//        File zipFile = zipFile();
//
//        if (!configLoader.isFtpAvailable()) {
//            log.warn("FTP not available. Backup file saved locally: {}", zipFile.getAbsolutePath());
//            return;
//        }
//
//        log.debug("Uploading the backup DB file {} to FTP server: {} {}/{}",
//                zipFile.getAbsolutePath(), ftpServerName, remoteBackupFolder, zipFile.getName());
//
//        PutOutput output = ftpRpc.putFile(ftpServerName,
//                remoteBackupFolder,
//                zipFile.getAbsolutePath()
//        );
//
//        if (!output.getResult().equalsIgnoreCase(RESULT_SUCCESS)) {
//            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR,
//                    String.format("Failed to upload the backup DB file to FTP server: {} {}/{}",
//                            ftpServerName, remoteBackupFolder, zipFile)
//            );
//        }
//    }
//
//    public File zipFile() {
//        String fileName = buildBackupFileName("ctrlDB");
//        String tempFilePath = fileName;
//
//        String cmd = String.format(
//                "tar -czf %s -C %s .",
//                tempFilePath,
//                BACKUP_FOLDER
//        );
//
//        log.info("Creating backup file: {}", tempFilePath);
//        log.info("Executing tar command: {}", cmd);
//
//        AuxTools.execLocal(cmd);
//
//        String finalPath = BACKUP_FOLDER + File.separator + fileName;
//        File tempFile = new File(tempFilePath);
//        File destFile = new File(finalPath);
//        if (!tempFile.renameTo(destFile)) {
//            log.error("Failed to move archive {} to {}", tempFilePath, finalPath);
//            throw new RuntimeException("Failed to move backup archive");
//        }
//
//        log.info("Backup archive created at {}", finalPath);
//        return destFile;
//    }
//
//    private String buildBackupFileName(String prefix) {
//        return String.format("%s-%s.tar.gz", prefix, AuxTools.getDateStr());
//    }
//
//    public void cleanFolder(String folderName) {
//        Path folder = Paths.get(folderName);
//        try {
//            if (Files.exists(folder)) {
//                Files.walk(folder)
//                        .sorted((a, b) -> b.compareTo(a)) // delete children before parents
//                        .forEach(p -> {
//                            try {
//                                Files.delete(p);
//                                System.out.println("Deleted: " + p);
//                            } catch (IOException e) {
//                                System.err.println("Failed to delete " + p + ": " + e.getMessage());
//                            }
//                        });
//            } else {
//                new File(folderName).mkdirs();
//            }
//        } catch (IOException e) {
//            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR,
//                    String.format("Failed to clean folder: %s, error: %s", folderName,
//                            e.getMessage())
//            );
//        }
//
//        log.debug("Every thing has removed from {}.", folder);
//    }
//
//    private String validateExisting(String remoteFileName) {
//        String remoteFolder = remoteFileName.substring(0, remoteFileName.lastIndexOf('/'));
//        String fileName = remoteFileName.substring(remoteFileName.lastIndexOf('/') + 1);
//
//        ListOutput output = configLoader.getFtpRpc()
//                .retrieveServerList(configLoader.getSftpServerName(), remoteFolder);
//
//        output.getFile().stream().filter(x -> x.getName().equals(fileName)).findAny()
//                .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        String.format("Cannot find the restore DB file on FTP server: {} {}/{}",
//                                configLoader.getSftpServerName(),
//                                configLoader.getControllerDbBackupDir(), fileName)
//                ));
//
//        return fileName;
//    }
//
//    public void cleanupOldSftpFiles(String ftpServerName, String remoteFolder,
//            Integer sftpFileTTL) {
//        if (!configLoader.isFtpAvailable()) {
//            log.debug("FTP not available, skipping cleanup");
//            return;
//        }
//
//        ListOutput output = configLoader.getFtpRpc()
//                .retrieveServerList(ftpServerName, remoteFolder);
//        cleanupOldSftpFiles(ftpServerName, remoteFolder, output.getFile(), sftpFileTTL);
//    }
//
//    private void cleanupOldSftpFiles(String ftpServerName, String remoteFolder,
//            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.File> files,
//            Integer sftpFileTTL) {
//        long expireDaysMillis = sftpFileTTL * 24 * 60 * 60 * 1000L;
//        long now = System.currentTimeMillis();
//
//        for (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.File file : files) {
//            if (!FileAttributes.Type.File.equals(file.getType())
//                    || file.getCreationTime() == null) {
//                continue;
//            }
//
//            long creationTime = normalizeEpochMillis(file.getCreationTime().longValue());
//            if (now - creationTime > expireDaysMillis) {
//                try {
//                    configLoader.getFtpRpc().deleteFile(
//                            ftpServerName,
//                            remoteFolder,
//                            file.getName()
//                    );
//                    log.info("Deleted old file from SFTP: {}", file.getName());
//                } catch (Exception e) {
//                    log.error("Failed to delete old file: {}", file.getName(), e);
//                }
//            }
//        }
//    }
//
//    private long normalizeEpochMillis(long rawTime) {
//        if (rawTime > 0 && rawTime < 1_000_000_000_000L) {
//            return rawTime * 1000L;
//        }
//        return rawTime;
//    }
//
//    public void cleanupOldLocalFiles(Integer localBackupTTL) {
//        File backupDir = new File(BACKUP_FOLDER);
//        if (!backupDir.exists() || !backupDir.isDirectory()) {
//            return;
//        }
//
//        long expireDaysMillis = localBackupTTL * 24 * 60 * 60 * 1000L;
//        long now = System.currentTimeMillis();
//
//        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".tar.gz"));
//        if (files == null) {
//            return;
//        }
//
//        for (File file : files) {
//            long lastModified = file.lastModified();
//            if (now - lastModified > expireDaysMillis) {
//                if (file.delete()) {
//                    log.info("Deleted old local backup: {}", file.getName());
//                } else {
//                    log.warn("Failed to delete old local backup: {}", file.getName());
//                }
//            }
//        }
//    }
//
//    public String findLocalRestoreFile(String zipFileName) {
//        File backupDir = new File(BACKUP_FOLDER);
//        File file = new File(backupDir, zipFileName);
//
//        if (!file.exists()) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "Restore file not found locally: " + zipFileName);
//        }
//
//        File restoreDir = new File(RESTORE_FOLDER);
//        restoreDir.mkdirs();
//
//        String cmd = String.format("tar -xzf %s -C %s", file.getAbsolutePath(), RESTORE_FOLDER);
//        log.info("Extracting local restore file: {}", cmd);
//        AuxTools.execLocal(cmd);
//
//        return RESTORE_FOLDER;
//    }
//}
