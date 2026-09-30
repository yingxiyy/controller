package net.flex.dci.otn.controller.pm;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.IpUtils;
import net.flex.dci.otc.common.util.IpUtils.IpVersion;
import net.flex.dci.otc.controller.rpc.client.rpcs.FtpRpc;
import net.flex.dci.otc.mongo.dao.FtpServerDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.GetOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.MkdirOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.RmFolderOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.RmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FTPClient {

    public static final String FTP_SUCCESS = "success";

    private final FtpServerDao ftpServerDao;
    private final FtpRpc ftpRpc;

    public FtpServer fetchFTPServer() {
        return ftpServerDao.getFtpServers().getFtpServer().get(0);
    }

    public FTPResult downloadFile(FtpServer ftpServer, String remoteFolder, String localFolder) {
        log.debug("download file from remote {}:{} -> {}", ftpServer.getName(), remoteFolder,
                localFolder);
        FTPResult result = new FTPResult();
        GetOutput output = ftpRpc.downloadFile(ftpServer.getName(), remoteFolder, localFolder);
        if (output.getResult().equalsIgnoreCase(FTP_SUCCESS)) {
            result.setSuccess(true);
            result.setFileName(remoteFolder);
            return result;
        } else {
            String msg = String.format("Failed to download file from FTP server: %s:%s",
                    remoteFolder, ftpServer.getName());
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public FTPResult createRemoteFolder(FtpServer ftpServer, String absoluteFolderName) {
        FTPResult result = new FTPResult();
        MkdirOutput output = ftpRpc.mkdir(ftpServer.getName(), absoluteFolderName);
        if (output.getResult().equalsIgnoreCase(FTP_SUCCESS)) {
            result.setSuccess(true);
            result.setFileName(absoluteFolderName);
            return result;
        } else {
            String msg = String.format("Failed to create folder %s on FTP server: %s",
                    absoluteFolderName, ftpServer.getName());
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public FTPResult cleanRemoteFolder(FtpServer ftpServer, String absoluteFolderName) {
        log.debug("remove remote files under folder {}:{}", ftpServer.getName(),
                absoluteFolderName);
        FTPResult result = new FTPResult();
        RmOutput output = ftpRpc.deleteFile(ftpServer.getName(), absoluteFolderName, ".*");
        if (output.getResult().equalsIgnoreCase(FTP_SUCCESS)) {
            result.setSuccess(true);
            result.setFileName(absoluteFolderName);
            return result;
        } else {
            String msg = String.format("Failed to remove files on FTP server. %s:%s",
                    absoluteFolderName, ftpServer.getName());
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public FTPResult removeRemoteFolder(FtpServer ftpServer, String remoteDir) {
        log.debug("remove remote folder {}:{}", ftpServer.getName(), remoteDir);
        FTPResult result = new FTPResult();
        RmFolderOutput output = ftpRpc.deleteFolder(ftpServer.getName(), remoteDir);
        if (output.getResult().equalsIgnoreCase(FTP_SUCCESS)) {
            result.setSuccess(true);
            result.setFileName(remoteDir);
            return result;
        } else {
            String msg = String.format("Failed to remove folder on FTP server. %s:%s", remoteDir,
                    ftpServer.getName());
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public FTPResult removeRemoteFile(FtpServer ftpServer, String remoteDir, String fileName) {
        log.debug("remove remote file {}:{}:{}", ftpServer.getName(), remoteDir, fileName);
        FTPResult result = new FTPResult();
        RmOutput output = ftpRpc.deleteFile(ftpServer.getName(), remoteDir, fileName);
        if (output.getResult().equalsIgnoreCase(FTP_SUCCESS)) {
            result.setSuccess(true);
            result.setFileName(remoteDir);
            return result;
        } else {
            String msg = String.format("Failed to remove file on FTP server. %s:%s",
                    ftpServer.getName(), remoteDir, fileName);
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public FtpServer pickupFtpServerByIpVersion(IpVersion ipVersion) {
        log.debug("pick up ftp server by ip version:{}", ipVersion);
        List<FtpServer> sftpServers = ftpServerDao.listFtpServers();
        List<FtpServer> ftpServers = sftpServers.stream()
                .filter(ftpServer -> IpUtils.getIpVersion(ftpServer.getAddress()).equals(ipVersion))
                .collect(Collectors.toList());
        FtpServer pickedServer = null;
        if (!ftpServers.isEmpty()) {
            int randomIdx = ThreadLocalRandom.current().nextInt(ftpServers.size());
            pickedServer = ftpServers.get(randomIdx);
            log.debug("Randomly selected FTP server: {}", pickedServer.getAddress());
        } else {
            log.warn("No FTP server found for IP version: {}", ipVersion);
        }
        return pickedServer;
    }


    @Data
    public static class FTPResult {

        private boolean isSuccess;
        private String fileName;
        private String errorInfo;
    }
}
