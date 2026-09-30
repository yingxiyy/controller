/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.ftp;

import net.flex.dci.otn.controller.sftp.manager.ftp.client.FtpClient;
import net.flex.dci.otn.controller.sftp.manager.ftp.client.IClient;
import net.flex.dci.otn.controller.sftp.manager.ftp.client.SFtpClient;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.FtpType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;

public class FtpClientFactory {

    public static IClient getFtpClient(FtpType type) {
        switch (type) {
            case FTP:
                return new FtpClient();
            case SFTP:
                return new SFtpClient();
            default:
                throw new IllegalArgumentException("Unknown FTP type: " + type);
        }
    }

    public static IClient getFtpClient(FtpServer ftpServer) {
        FtpType type = ftpServer.getType();
        switch (type) {
            case FTP:
                IClient ftpClient = new FtpClient();
                ftpClient.connect(ftpServer);
                return ftpClient;
            case SFTP:
                IClient sftpClient = new SFtpClient();
                sftpClient.connect(ftpServer);
                return sftpClient;
            default:
                throw new IllegalArgumentException("Unknown FTP type: " + type);
        }
    }
}
