package net.flex.dci.otn.controller.sftp.manager.components.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.FtpServerDao;
import net.flex.dci.otn.controller.sftp.manager.components.FtpManagerValidator;
import net.flex.dci.otn.controller.sftp.manager.ftp.FtpClientFactory;
import net.flex.dci.otn.controller.sftp.manager.ftp.client.IClient;
import net.flex.dci.otn.controller.sftp.manager.utils.FtpManagerUtil;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.*;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 6/7/2023 4:03 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FtpManagerValidatorImpl implements FtpManagerValidator {

    private final FtpServerDao ftpServerDao;

    @Override
    public boolean testConnect(String host, int port, String username, String password,
            FtpType type) {
        log.debug("start to test connect for the server:{},host:{}", host, port);
        FtpManagerUtil.notify(BroadCastConstant.VERIFYING_SFTP_SERVER,
                String.format("Verifying ftp server host : %s,port : %d", host, port), false);
        try (IClient client = FtpClientFactory.getFtpClient(type)) {
            return client.testConnect(host, port, username, password);
        } catch (Exception ex) {
            log.error("failed to test client connect,the ex is:{}", ex.getMessage(), ex);
            FtpManagerUtil.notify(BroadCastConstant.VERIFIED_FTP_SERVER,
                    String.format(
                            "Verifying ftp server host : %s,port : %d failed,the reason is: %s",
                            host, port, ex.getMessage()), true);
            return false;
        }

    }

    @Override
    public void validateUpdateSftpServer(UpdateFtpServerInput updateFtpServerInput) {
        log.debug("start to validate the update ftp server input");
//        String sftpServerName = updateFtpServerInput.getName();
        String host = updateFtpServerInput.getAddress();
        Integer port = updateFtpServerInput.getPort().getValue();
        String password = updateFtpServerInput.getPassword();
        String user = updateFtpServerInput.getUser();
        FtpType ftpType = updateFtpServerInput.getType();
        String index = updateFtpServerInput.getId();
//        if (sftpServerName == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "the ftp server name should not be null");
//        }
        if (host == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server host should not be null");
        }
        if (port == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server port should not be null");
        }

        if (password == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server password should not be null");
        }

        if (user == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server account should not be null");
        }

        if (ftpType == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server ftp type  should not be null");
        }
        if (index == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server id should not be null");
        }

        boolean existedFtpServer = ftpServerDao.existsByFtpServerId(updateFtpServerInput.getId());
        if (!existedFtpServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server is not existed");
        }

//        boolean existed = ftpServerDao.existsByFtpServerName(sftpServerName);
//        if (existed) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "the ftp server name  existed");
//        }
    }

    @Override
    public void validateMkdir(MkdirInput mkdirInput) {
        String serverName = mkdirInput.getServerName();
        String remoteFolder = mkdirInput.getRemoteFolder();
        if (serverName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server name should not be null");
        }

        if (remoteFolder == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the create remoteFolder should not be null");
        }
        if (remoteFolder.contains("..") || remoteFolder.contains(":") || remoteFolder.contains(
                "*")) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Remote folder path contains illegal characters");
        }
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        if (null == ftpServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("ftp server name:%s is not existed", serverName));
        }
    }

    @Override
    public void validateRetrieveServerDirectory(ListInput listInput) {
        String serverName = listInput.getServerName();
        String remoteFolder = listInput.getFolderName();
        if (serverName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server name should not be null");
        }
        if (remoteFolder == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the remote folder should not be null");
        }
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        if (null == ftpServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("ftp server name:%s is not existed", serverName));
        }
        try (IClient ftpClient = FtpClientFactory.getFtpClient(ftpServer)) {
            boolean existedFolder = ftpClient.folderExists(remoteFolder);
            if (!existedFolder) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("folder:%s is not exist on the server:%s", remoteFolder,
                                serverName));
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("failed to connect to the server:%s",
                            serverName));
        }

    }

    @Override
    public void validateDownloadInput(GetInput getInput) {
        String fileName = getInput.getFileName();
        String serverName = getInput.getServerName();
        String remoteFolder = getInput.getRemoteFolder();
        if (serverName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server name should not be null");
        }
        if (remoteFolder == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the remote folder should not be null");
        }
        if (fileName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the download file location should not be null");
        }
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        if (null == ftpServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("ftp server name:%s is not existed", serverName));
        }
        try (IClient ftpClient = FtpClientFactory.getFtpClient(ftpServer)) {
            boolean existedFolder = ftpClient.folderExists(remoteFolder);
            if (!existedFolder) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("folder:%s is not exist on the server:%s", remoteFolder,
                                serverName));
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("failed to connect to the server:%s",
                            serverName));
        }
    }

    @Override
    public void validateUploadInput(PutInput putInput) {
        String fileName = putInput.getFileName();
        String serverName = putInput.getServerName();
        String remoteFolder = putInput.getRemoteFolder();
        if (serverName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server name should not be null");
        }
        if (remoteFolder == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the remote folder should not be null");
        }
        if (fileName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the download file location should not be null");
        }
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        if (null == ftpServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("ftp server name:%s is not existed", serverName));
        }
    }

    @Override
    public void validateRmInput(RmInput rmInput) {
        String fileName = rmInput.getFileName();
        String serverName = rmInput.getServerName();
        String remoteFolder = rmInput.getRemoteFolder();
        if (serverName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server name should not be null");
        }
        if (remoteFolder == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the remote folder should not be null");
        }
        if (fileName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the download file location should not be null");
        }
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        if (null == ftpServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("ftp server name:%s is not existed", serverName));
        }
    }

    @Override
    public void validateRmFolderInput(RmFolderInput rmInput) {
        String serverName = rmInput.getServerName();
        String remoteFolder = rmInput.getRemoteFolder();
        if (serverName == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server name should not be null");
        }
        if (remoteFolder == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the remote folder should not be null");
        }
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        if (null == ftpServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("ftp server name:%s is not existed", serverName));
        }
    }
}
