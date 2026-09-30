package net.flex.dci.otn.controller.sftp.manager.components;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.*;

/**
 * @version 1.0
 * @date 6/6/2023 2:23 PM
 */

public interface FtpManagerValidator {

    boolean testConnect(String host, int port, String username, String password, FtpType type);

    void validateUpdateSftpServer(UpdateFtpServerInput updateFtpServerInput);

    void validateMkdir(MkdirInput mkdirInput);

    void validateRetrieveServerDirectory(ListInput listInput);

    void validateDownloadInput(GetInput getInput);

    void validateUploadInput(PutInput putInput);

    void validateRmInput(RmInput rmInput);

    void validateRmFolderInput(RmFolderInput rmInput);
}
