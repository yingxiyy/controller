package net.flex.dci.otn.controller.schedule.component;

import static net.flex.dci.otn.controller.schedule.utils.Constants.BACKUP_DIR_FORMAT;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.rpc.client.rpcs.FtpRpc;
import net.flex.dci.otn.controller.schedule.properties.BackupProperties;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.FileAttributes;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ListOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.PutOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.File;
import org.springframework.stereotype.Component;

/**
 * 2026/5/31
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class FtpManager {

    private final FtpServerSelector ftpServerSelector;

    private final FtpRpc ftpRpc;

    private final BackupProperties backupProperties;


    public boolean isAvailable() {
        log.debug("is current ftp server is available");
        FtpServer ftpServer = ftpServerSelector.selectFtpServer();
        return ftpServer != null;
    }


    /**
     * 上传本地备份文件到远程 FTP/SFTP 服务器。
     *
     * @param localFile 本地备份文件路径（需要 sftpserver 能访问的绝对路径）
     * @param timestampDirName 远程时间戳子目录名
     */
    public void uploadFile(Path localFile, String timestampDirName) {
        FtpServer ftpServer = ftpServerSelector.selectFtpServer();
        if (ftpServer == null) {
            log.warn("no ftp server available, skip upload for: {}", localFile);
            return;
        }
        String serverName = ftpServer.getName();
        String remoteDir = backupProperties.getRemoteBackupDir() + "/" + timestampDirName;
        String absolutePath = localFile.toAbsolutePath().toString();
        String fileName = localFile.getFileName().toString();

        ensureRemoteDir(serverName, remoteDir);

        try {
            PutOutput putOutput = ftpRpc.putFile(serverName, remoteDir, absolutePath);
            log.info("uploaded file: {} to remote dir: {}, result: {}",
                    fileName, remoteDir, putOutput.getResult());
        } catch (Exception e) {
            log.error("failed to upload file: {} to remote: {}", fileName, remoteDir, e);
            throw e;
        }
    }

    private void ensureRemoteDir(String serverName, String remoteDir) {
        try {
            ftpRpc.mkdir(serverName, remoteDir);
            log.debug("Remote dir ensured: {}", remoteDir);
        } catch (Exception ex) {
            log.error("failed to create the remote dir", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("remote dir create failed: %s, error: %s", remoteDir,
                            ex.getMessage()));
        }
    }

    /**
     * 清理远程 FTP/SFTP 服务器上过期的备份目录。
     *
     * @return 清理的过期目录数量
     */
    public int cleanupRemoteExpiredBackups() {
        FtpServer ftpServer = ftpServerSelector.selectFtpServer();
        if (ftpServer == null) {
            log.warn("no ftp server available, skip remote cleanup");
            return 0;
        }
        String serverName = ftpServer.getName();
        String remoteBackupDir = backupProperties.getRemoteBackupDir();
        int ttlDays = backupProperties.getTtl();
        LocalDateTime expirationTime = LocalDateTime.now().minusDays(ttlDays);

        ListOutput listOutput;
        try {
            listOutput = ftpRpc.retrieveServerList(serverName, remoteBackupDir);
        } catch (Exception e) {
            log.error("failed to retrieve remote backup dir: {}", remoteBackupDir, e);
            return 0;
        }

        List<File> files = listOutput.getFile();
        if (files == null || files.isEmpty()) {
            log.debug("no files in remote backup dir: {}", remoteBackupDir);
            return 0;
        }

        int cleanedCount = 0;
        for (File file : files) {
            if (file.getType() != FileAttributes.Type.Folder) {
                continue;
            }
            String dirName = file.getName();
            try {
                LocalDateTime dirTime = LocalDateTime.parse(dirName, BACKUP_DIR_FORMAT);
                if (dirTime.isBefore(expirationTime)) {
                    String fullRemotePath = remoteBackupDir + "/" + dirName;
                    try {
                        ftpRpc.deleteFolder(serverName, fullRemotePath);
                        cleanedCount++;
                        log.info("removed expired remote backup dir: {}", fullRemotePath);
                    } catch (Exception e) {
                        log.error("failed to remove remote dir: {}", fullRemotePath, e);
                    }
                }
            } catch (DateTimeParseException e) {
                log.debug("skip non-backup remote dir: {}", dirName);
            }
        }
        return cleanedCount;
    }
}