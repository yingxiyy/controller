package net.flex.dci.otn.controller.sftp.manager.manager.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.FtpServerDao;
import net.flex.dci.otn.controller.sftp.manager.components.FtpManagerValidator;
import net.flex.dci.otn.controller.sftp.manager.ftp.FtpClientFactory;
import net.flex.dci.otn.controller.sftp.manager.ftp.client.IClient;
import net.flex.dci.otn.controller.sftp.manager.manager.SftpServerManager;
import net.flex.dci.otn.controller.sftp.manager.message.FtpMessage;
import net.flex.dci.otn.controller.sftp.manager.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.sftp.manager.utils.FtpManagerUtil;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.*;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServerKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.File;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.springframework.stereotype.Component;

import java.util.List;

import static net.flex.dci.otn.controller.sftp.manager.utils.FtpManagerUtil.RESULT_SUCCESS;

/**
 * @version 1.0
 * @date 6/6/2023 1:15 PM
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class SftpServerManagerImpl implements SftpServerManager {

    private final FtpServerDao ftpServerDao;

    private final FtpManagerValidator ftpManagerValidator;

    private final FtpMessage ftpMessage;
    private final KeyLockManager keyLockManager;

    @Override
    public void createSftpServer(CreateFtpServerInput createFtpServerInput) {
        log.info("start to create sftp server");
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(createFtpServerInput.getName());
        if (ftpServer != null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ftp server name is already used");
        }

        Object lock = keyLockManager.getLock(createFtpServerInput.getName());
        synchronized (lock) {
            AsynchronousExecutor.execute(() -> {
                _createSftpServer(createFtpServerInput);
            });
        }

        log.info("finish to create sftp server");
    }

    @Override
    public void updateSftpServer(UpdateFtpServerInput updateFtpServerInput) {
        log.debug("start to update sftp server,the input is :{}", updateFtpServerInput);
        ftpManagerValidator.validateUpdateSftpServer(updateFtpServerInput);

        Object lock = keyLockManager.getLock(updateFtpServerInput.getId());
        synchronized (lock) {
            AsynchronousExecutor.execute(() -> {
                _updateFtpServer(updateFtpServerInput);
            });
        }

        log.info("finish to create sftp server");
    }

    private void _updateFtpServer(UpdateFtpServerInput updateFtpServerInput) {
        log.debug("update the ftp server");
        String host = updateFtpServerInput.getAddress();
        Integer port = updateFtpServerInput.getPort().getValue();
        String password = updateFtpServerInput.getPassword();
        String user = updateFtpServerInput.getUser();
        FtpType ftpType = updateFtpServerInput.getType();
        String index = updateFtpServerInput.getId();
        FtpServer ftpServer = ftpServerDao.getFtpServerById(index);

        boolean isConnected = ftpManagerValidator.testConnect(host, port, user, password, ftpType);
        if (isConnected) {
            FtpServerBuilder builder = new FtpServerBuilder();
            builder.setName(ftpServer.getName());
            builder.setPort(PortNumber.getDefaultInstance(String.valueOf(port)));
            builder.setKey(new FtpServerKey(index));
            builder.setId(index);
            builder.setAddress(host);
            builder.setPassword(password);
            builder.setUser(user);
            builder.setType(ftpType);
            ftpMessage.notifyFtpServerUpdate(ftpServer);
            //notification
            FtpManagerUtil.notify(BroadCastConstant.UPDATE_SFTP_SERVER,
                    String.format(
                            "success to update ftp server host : %s,port : %d profiles",
                            host, port),
                    false);
        } else {
            log.error("failed to update the ftp server");
            FtpManagerUtil.notify(BroadCastConstant.UPDATE_SFTP_SERVER, String.format(
                    "failed to update ftp server host : %s,port : %d ,the reason is the ftp server is not reachable",
                    host, port), true);

        }

    }

    @Override
    public void deleteSftpServer(DeleteFtpServerInput deleteFtpServerInput) {
        log.debug("start to delete the ftp server,the id is:{}", deleteFtpServerInput.getId());

        Object lock = keyLockManager.getLock(deleteFtpServerInput.getId());
        synchronized (lock) {

            String ftpServerId = deleteFtpServerInput.getId();
            boolean existed = ftpServerDao.existsByFtpServerId(ftpServerId);
            if (existed) {
                log.debug("id is existed start to delete");
                FtpServer ftpServer = ftpServerDao.getFtpServerById(ftpServerId);
                ftpServerDao.deleteFtpServer(ftpServerId);
                ftpMessage.notifyFtpServerDelete(ftpServer);
                FtpManagerUtil.notify(BroadCastConstant.DELETE_SFTP_SERVER, String.format(
                        "success remove ftp server %s(%s:%d)",
                        ftpServer.getName(), ftpServer.getAddress(),
                        ftpServer.getPort().getValue()), false);
            }
            log.debug("finish to delete the ftp server");
        }
    }

    @Override
    public MkdirOutput mkdir(MkdirInput mkdirInput) {
        log.debug("mkdir :{} ftp server is:{}", mkdirInput, mkdirInput.getServerName());
        ftpManagerValidator.validateMkdir(mkdirInput);
        long start = System.currentTimeMillis();

        Object lock = keyLockManager.getLock(mkdirInput.getRemoteFolder());
        synchronized (lock) {

            String ftpServerName = mkdirInput.getServerName();
            String remoteFolder = mkdirInput.getRemoteFolder();
            FtpServer ftpServer = ftpServerDao.getFtpServerByName(ftpServerName);
            try {
                createRemoteFolder(ftpServer, remoteFolder);
                log.info("mkdir finished for ftp server:{} remoteFolder:{} in {} ms",
                        ftpServerName, remoteFolder, System.currentTimeMillis() - start);
                MkdirOutput mkdirOutput = new MkdirOutputBuilder().setResult(RESULT_SUCCESS).build();
                return mkdirOutput;
            } catch (Exception ex) {
                log.error("failed to create directory {} for ftp server:{} :{}", remoteFolder,
                        ftpServerName, ex.getMessage(), ex);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format("failed to create dir:%s for ftp server %s:%s", remoteFolder,
                                ftpServerName, ex.getMessage()));
            }
        }
    }

    @Override
    public ListOutput retrieveServerDirectory(ListInput listInput) {
        log.debug("retrieve ftp server directory, folder:{} ftp server:{}",
                listInput.getFolderName(), listInput.getFolderName());
        ftpManagerValidator.validateRetrieveServerDirectory(listInput);
        String serverName = listInput.getServerName();
        String remoteFolder = listInput.getFolderName();
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        List<File> files = retrieveFilesOnServer(ftpServer, remoteFolder);
        ListOutputBuilder listOutputBuilder = new ListOutputBuilder();
        listOutputBuilder.setFile(files);
        return listOutputBuilder.build();
    }

    @Override
    public GetOutput downloadFile(GetInput getInput) {
        log.debug("download file :{} from ftp server :{} to remoteFile:{}", getInput.getFileName(),
                getInput.getServerName(), getInput.getRemoteFolder());
        ftpManagerValidator.validateDownloadInput(getInput);

        Object lock = keyLockManager.getLock(getInput.getServerName());
        synchronized (lock) {
            _downLoadFile(getInput);
        }

        return new GetOutputBuilder().setResult(RESULT_SUCCESS).build();
    }

    @Override
    public PutOutput uploadFile(PutInput putInput) {
        log.debug("upload file:{} to ftp server:{} remoteFolder:{}", putInput.getFileName(),
                putInput.getServerName(), putInput.getRemoteFolder());
        ftpManagerValidator.validateUploadInput(putInput);

        Object lock = keyLockManager.getLock(putInput.getServerName());
        synchronized (lock) {
            _uploadFile(putInput);
        }

        return new PutOutputBuilder().setResult(RESULT_SUCCESS).build();
    }

    /**
     * upload file
     *
     * @param putInput
     */
    private void _uploadFile(PutInput putInput) {
        log.debug("start to upload file:{}", putInput);
        String fileName = putInput.getFileName();
        String serverName = putInput.getServerName();
        String remoteFolder = putInput.getRemoteFolder();
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        try (IClient serverClient = FtpClientFactory.getFtpClient(ftpServer)) {
            serverClient.putFilesToServer(remoteFolder, fileName);
            log.debug("Success to upload the file:{} to ftp server:{}", fileName, ftpServer);
            String message = String.format(
                    "Success to upload file or folder:%s to ftp server:%s remoteDir :%s",
                    remoteFolder, ftpServer.getName(), remoteFolder);
            //notify success
            FtpManagerUtil.notify(BroadCastConstant.UPLOAD_FILE, message, false);
        } catch (Exception e) {
            log.error("failed to upload file or folder:{} to ftp server:{}:{}", remoteFolder,
                    ftpServer,
                    e.getMessage(), e);
            String reason = String.format(
                    "failed to upload file or folder:%s from ftp server:%s folder:%s :%s",
                    remoteFolder, ftpServer.getName(), remoteFolder, e.getMessage());
            FtpManagerUtil.notify(BroadCastConstant.UPLOAD_FILE, reason, true);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, reason, e);
        }
    }

    /**
     * DOWNLOAD FILE
     *
     * @param getInput
     */
    private void _downLoadFile(GetInput getInput) {
        log.debug("start to download file:{}", getInput);
        String fileName = getInput.getFileName();
        String remoteFolder = getInput.getRemoteFolder();
        String serverName = getInput.getServerName();
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        try (IClient serverClient = FtpClientFactory.getFtpClient(ftpServer)) {
            serverClient.getFilesFromServer(remoteFolder, fileName);  //fileName means local file
            log.debug("Success to download the file:{}", fileName);
            String message = String.format(
                    "Success to download file or folder:%s from ftp server:%s",
                    remoteFolder, ftpServer.getName());
            //notify success
            FtpManagerUtil.notify(BroadCastConstant.DOWN_LOAD_FILE, message, false);
        } catch (Exception e) {
            log.error("failed to download file or folder:{} by ftp server:{}:{}", remoteFolder,
                    ftpServer,
                    e.getMessage(), e);
            String reason = String.format(
                    "failed to download file or folder:%s from ftp server:%s:%s",
                    remoteFolder, ftpServer.getName(), e.getMessage());
            FtpManagerUtil.notify(BroadCastConstant.DOWN_LOAD_FILE, reason, true);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, reason, e);
        }
    }

    private List<File> retrieveFilesOnServer(FtpServer ftpServer, String remoteFolder) {
        log.debug("retrieve the remoteFolder sub file:{} list on ftpServer:{}", remoteFolder,
                ftpServer);
        try (IClient serverClient = FtpClientFactory.getFtpClient(ftpServer)) {
            List<File> files = serverClient.listFilesOnServer(remoteFolder);
            return files;
        } catch (Exception e) {
            log.error("failed to retrieve directory:{} by ftp server:{}:{}", remoteFolder,
                    ftpServer,
                    e.getMessage(), e);
            String reason = String.format("failed to retrieve directory:%s by ftp server:%s:%s",
                    remoteFolder, ftpServer.getName(), e.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, reason, e);
        }
    }

    private void createRemoteFolder(FtpServer ftpServer, String remoteFolder) {
        log.debug("start to  create folder:{} on ftp server:{}", remoteFolder, ftpServer);
        long start = System.currentTimeMillis();
        try (IClient client = FtpClientFactory.getFtpClient(ftpServer)) {
            long folderExistsStart = System.currentTimeMillis();
            if (client.folderExists(remoteFolder)) {
                log.info("Directory already exists:{} checked in {} ms, total {} ms",
                        remoteFolder,
                        System.currentTimeMillis() - folderExistsStart,
                        System.currentTimeMillis() - start);
                return;
            }
            log.debug("folder does not exist, start mkdir for remoteFolder:{} after {} ms",
                    remoteFolder, System.currentTimeMillis() - start);
            long mkdirStart = System.currentTimeMillis();
            client.mkdir(remoteFolder);
            log.info("Directory created:{} mkdir cost {} ms, total {} ms",
                    remoteFolder,
                    System.currentTimeMillis() - mkdirStart,
                    System.currentTimeMillis() - start);
        } catch (Exception ex) {
            log.error("failed to create directory:{} by ftp server:{}:{}", remoteFolder, ftpServer,
                    ex.getMessage(), ex);
            String reason = String.format("failed to create directory:%s by ftp server:%s:%s",
                    remoteFolder, ftpServer.getName(), ex.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, reason, ex);
        }
    }


    private void _createSftpServer(CreateFtpServerInput createFtpServerInput) {
        log.debug("start to create the sftp server");

        String sftpName = createFtpServerInput.getName();
        String host = createFtpServerInput.getAddress();
        Integer port = createFtpServerInput.getPort().getValue();
        String password = createFtpServerInput.getPassword();
        String user = createFtpServerInput.getUser();
        FtpType ftpType = createFtpServerInput.getType();

        boolean isConnected = ftpManagerValidator.testConnect(host, port, user, password, ftpType);
        if (isConnected) {
            FtpServerBuilder builder = new FtpServerBuilder();
            builder.setName(sftpName);
            builder.setPort(PortNumber.getDefaultInstance(String.valueOf(port)));
            builder.setKey(new FtpServerKey(sftpName));
            builder.setAddress(host);
            builder.setPassword(password);
            builder.setUser(user);
            builder.setType(ftpType);
            FtpServer newFtpServer = ftpServerDao.newFtpServer(builder.build());
            //notification
            ftpMessage.notifyFtpServerCreate(newFtpServer);
            FtpManagerUtil.notify(BroadCastConstant.CREATE_SFTP_SERVER,
                    String.format("success to create ftp server host: %s, port: %d", host, port),
                    false);
        } else {
            //notification for test
            log.error("failed to create the ftp server");
            FtpManagerUtil.notify(BroadCastConstant.CREATE_SFTP_SERVER,
                    String.format(
                            "failed to create ftp server host: %s, port: %d, reason: not reachable",
                            host, port),
                    true);
        }
    }

    @Override
    public RmOutput rmFile(RmInput rmInput) {
        log.debug("remove file:{} from ftp server:{} remoteFolder:{}", rmInput.getFileName(),
                rmInput.getServerName(), rmInput.getRemoteFolder());
        ftpManagerValidator.validateRmInput(rmInput);

        Object lock = keyLockManager.getLock(rmInput.getServerName());
        synchronized (lock) {
            _rmFile(rmInput);
        }

        return new RmOutputBuilder().setResult(RESULT_SUCCESS).build();
    }

    @Override
    public RmFolderOutput rmFolder(RmFolderInput rmInput) {
        log.debug("remove folder from ftp server. {}/{}",
                rmInput.getServerName(), rmInput.getRemoteFolder());
        ftpManagerValidator.validateRmFolderInput(rmInput);

        Object lock = keyLockManager.getLock(rmInput.getServerName());
        synchronized (lock) {
            _rmFolder(rmInput);
        }

        return new RmFolderOutputBuilder().setResult(RESULT_SUCCESS).build();
    }

    /**
     * remove file
     *
     * @param rmInput
     */
    private void _rmFile(RmInput rmInput) {
        log.debug("start to remove file:{}", rmInput);
        String fileName = rmInput.getFileName();
        String serverName = rmInput.getServerName();
        String remoteFolder = rmInput.getRemoteFolder();
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        try (IClient serverClient = FtpClientFactory.getFtpClient(ftpServer)) {
            serverClient.rmFilesFromServer(remoteFolder, fileName);
            log.debug("Success remove the file:{} from ftp server {}/{}", fileName, ftpServer, remoteFolder);
            String message = String.format(
                    "Success to remove file: %s from ftp server:%s remoteDir :%s",
                    fileName, ftpServer.getName(), remoteFolder);
            //notify success
            FtpManagerUtil.notify(BroadCastConstant.UPLOAD_FILE, message, false);
        } catch (Exception e) {
            log.error("failed to remove file: {} from ftp server:{}/{}. {}", fileName,
                    ftpServer.getName(), remoteFolder,
                    e.getMessage(), e);
            String reason = String.format(
                    "failed to remove file: %s from ftp server:%s folder:%s :%s",
                    fileName, ftpServer.getName(), remoteFolder, e.getMessage());
            FtpManagerUtil.notify(BroadCastConstant.UPLOAD_FILE, reason, true);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, reason, e);
        }
    }

    /**
     * remove folder
     *
     * @param rmInput
     */
    private void _rmFolder(RmFolderInput rmInput) {
        log.debug("start to remove folder:{}", rmInput);
        String serverName = rmInput.getServerName();
        String remoteFolder = rmInput.getRemoteFolder();
        FtpServer ftpServer = ftpServerDao.getFtpServerByName(serverName);
        try (IClient serverClient = FtpClientFactory.getFtpClient(ftpServer)) {
            serverClient.rmFolderFromServer(remoteFolder);
            log.debug("Success remove from ftp server {}/{}", ftpServer, remoteFolder);
            String message = String.format(
                    "Success to from ftp server:%s remoteDir :%s",
                    ftpServer.getName(), remoteFolder);
            //notify success
            FtpManagerUtil.notify(BroadCastConstant.UPLOAD_FILE, message, false);
        } catch (Exception e) {
            log.error("failed to remove from ftp server {}/{}. {}", ftpServer.getName(), remoteFolder,
                    e.getMessage(), e);
            String reason = String.format(
                    "failed to remove folder:%s from ftp server:%s.  %s",
                    remoteFolder, ftpServer.getName(), e.getMessage());
            FtpManagerUtil.notify(BroadCastConstant.UPLOAD_FILE, reason, true);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, reason, e);
        }
    }
}
