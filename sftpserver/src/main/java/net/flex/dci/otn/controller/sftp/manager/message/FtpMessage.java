package net.flex.dci.otn.controller.sftp.manager.message;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;

/**
 *
 * 2025/9/22
 *
 * @author musa
 * @version 1.0
 **/
public interface FtpMessage {

    void notifyFtpServerCreate(FtpServer ftpServer);

    void notifyFtpServerDelete(String ftpServerId);

    void notifyFtpServerDelete(FtpServer ftpServer);

    void notifyFtpServerUpdate(FtpServer ftpServer);
}
