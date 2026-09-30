package net.flex.dci.otc.controller.rpc.client.rpcs;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.*;

/**
 *
 * 2025/10/4
 *
 * @author musa
 * @version 1.0
 **/
public interface FtpRpc {

    PutOutput putFile(String serverName, String remoteFolder, String fileName);

    GetOutput downloadFile(String serverName, String remoteFolder, String fileName);

    MkdirOutput mkdir(String serverName, String remoteFolder);

    ListOutput retrieveServerList(String serverName, String remoteFolder);

    RmOutput deleteFile(String serverName, String remoteFolder, String fileName);

    RmFolderOutput deleteFolder(String serverName, String remoteFolder);

}
