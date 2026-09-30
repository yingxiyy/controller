/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.ftp.client;

import java.util.List;
import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.File;

public interface IClient extends AutoCloseable {

    public boolean testConnect(String host, int port, String username, String password)
            throws CommonException;

    public void connect(String host, int port, String username, String password)
            throws CommonException;

    public void connect(FtpServer server) throws CommonException;

    public void disConnect();

    /**
     * @param remoteFolderName: folder name
     * @return
     * @throws CommonException
     */
    public List<File> listFilesOnServer(String remoteFolderName) throws CommonException;

    /**
     * @param remotePath : absolute path
     * @param localFileName : absolute path
     * @throws CommonException
     */
    public void putFilesToServer(String remotePath, String localFileName) throws CommonException;

    /**
     * @param remotePath : absolute path
     * @param remoteFileName : absolute path
     * @throws CommonException
     */
    public void rmFilesFromServer(String remotePath, String remoteFileName) throws CommonException;

    public void rmFolderFromServer(String remotePath) throws CommonException;

    public void getFilesFromServer(String remoteFolderName, String localFileName)
            throws CommonException;

    public void mkdir(String path) throws CommonException;


    boolean folderExists(String remoteFolder);


    void close() throws Exception;

}
