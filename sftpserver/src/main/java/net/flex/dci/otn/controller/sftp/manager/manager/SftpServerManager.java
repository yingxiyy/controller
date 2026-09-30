package net.flex.dci.otn.controller.sftp.manager.manager;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.*;

/**
 * @version 1.0
 * @date 6/6/2023 1:14 PM
 */
public interface SftpServerManager {

    void createSftpServer(CreateFtpServerInput createFtpServerInput);

    void updateSftpServer(UpdateFtpServerInput updateFtpServerInput);

    void deleteSftpServer(DeleteFtpServerInput deleteFtpServerInput);

    MkdirOutput mkdir(MkdirInput mkdirInput);

    ListOutput retrieveServerDirectory(ListInput listInput);

    GetOutput downloadFile(GetInput getInput);

    PutOutput uploadFile(PutInput putInput);

    RmOutput rmFile(RmInput rmInput);

    RmFolderOutput rmFolder(RmFolderInput rmInput);
}
