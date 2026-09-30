/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.ftp.client;

import static net.flex.dci.otn.controller.sftp.manager.utils.FtpManagerUtil.extractDirectoryPath;
import static net.flex.dci.otn.controller.sftp.manager.utils.FtpManagerUtil.extractFileName;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.FileFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.sftp.manager.utils.SFTPUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.FileAttributes.Type;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.File;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.FileBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.FileKey;

@Slf4j
public class SFtpClient implements IClient {


    private SFTPUtil ftp;

    public SFtpClient() {

    }

    @Override
    public boolean testConnect(String host, int port, String username, String password) throws CommonException {
        log.debug("start to test connect by the host {} and port {},ftp type is sftp", host, port);

        boolean result = false;
        ftp = new SFTPUtil(host, port, username, password);
        try {
             ftp.login();
             result = true;
        } catch (Exception e) {
            log.error("------{}", e);
            result = false;
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        } finally {
            ftp.logout();
        }
        return result;
    }

    @Override
    public void connect(String host, int port, String username, String password)
            throws CommonException {
        long start = System.currentTimeMillis();
        log.debug("start sftp connect host:{} port:{} user:{}", host, port, username);
        ftp = new SFTPUtil(host, port, username, password);

        try {
            ftp.login();
            log.debug("finish sftp connect host:{} port:{} user:{} cost {} ms",
                    host, port, username, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("------{}", e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
    }

    @Override
    public void connect(FtpServer server) throws CommonException {
        try {
            connect(
                    server.getAddress(),
                    server.getPort().getValue().intValue(),
                    server.getUser(),
                    server.getPassword());
        } catch (Exception e) {
            log.error("------{}", e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
    }

    @Override
    public void disConnect() {
        ftp.logout();
    }

    @Override
    public List<File> listFilesOnServer(String remoteFolderName) throws CommonException {
        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "SFTP server hasn't connected.");
        }
        Vector<LsEntry> ftpFiles;
        try {
            ftpFiles = ftp.listFiles(remoteFolderName);
        } catch (Exception e) {
            log.error("Failed to list files from remote folder '{}'", remoteFolderName, e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
        if (ftpFiles == null || ftpFiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<File> files = convert2OutputFiles(ftpFiles);
        return files;
    }


    @SuppressWarnings("null")
    @Override
    public void putFilesToServer(String remotePath, String localFileName) throws CommonException {
        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "SFTP server hasn't connected.");
        }

        if (localFileName.contains("*")) {
            log.debug("the local file name {} include wild char.", localFileName);

            String path = localFileName.substring(0, localFileName.lastIndexOf('/'));
            String wildFileName = localFileName.substring(localFileName.lastIndexOf('/') + 1);
            java.io.File[] files = null;
            java.io.File dir = new java.io.File(path);
            FileFilter fileFilter = new WildcardFileFilter(wildFileName);
            files = dir.listFiles(fileFilter);

            if (files != null && files.length > 0) {
                for (java.io.File file : files) {
                    putSingleFileToServer(remotePath, file.getAbsolutePath());
                }
            }
        } else {
            putSingleFileToServer(remotePath, localFileName);
        }
    }

    @Override
    public void rmFilesFromServer(String remotePath, String remoteFileName) throws CommonException {
        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "SFTP server hasn't connected.");
        }

        try {
            ftp.changeDir(remotePath);
            
            if (containsWildcard(remoteFileName)) {
                removeByWildcard(remotePath, remoteFileName);
            } else {
                // 普通文件
                ftp.delete(remotePath, remoteFileName);
            }
        } catch (SftpException e) {
            String msg = String.format("Failed to remove file '%s/%s':%s", remotePath, remoteFileName,
                    e.getMessage());
            log.error(msg, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
    }

    @Override
    public void rmFolderFromServer(String remotePath) throws CommonException {
        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "SFTP server hasn't connected.");
        }


        try {
            rmFilesFromServer(remotePath, ".*");
            // Remove the directory (must be empty)
            ftp.rmdir(remotePath);
        } catch (SftpException e) {
            String msg = String.format("Failed to remove folder '%s': %s", remotePath, e.getMessage());
            log.error(msg, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
    }


    private void removeByWildcard(String remotePath, String pattern) throws SftpException {
        Vector<ChannelSftp.LsEntry> files = ftp.listFiles(remotePath);

        for (ChannelSftp.LsEntry entry : files) {
            String name = entry.getFilename();

            // 跳过 . 和 ..
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }

            // 只删除普通文件（安全）
            if (!entry.getAttrs().isReg()) {
                continue;
            }

            if (wildcardMatch(name, pattern)) {
                log.info("Removing remote file: {}", name);
                ftp.rm(name);
            }
        }
    }

    private boolean wildcardMatch(String filename, String pattern) {
        return FilenameUtils.wildcardMatch(filename, pattern);
    }

    private boolean containsWildcard(String name) {
        if (name == null) {
            return false;
        }
        return name.indexOf('*') >= 0 || name.indexOf('?') >= 0;
    }


    private void removeRemoteFolder(String remoteDir) throws SftpException {
        Vector<LsEntry> entries = ftp.listFiles(remoteDir);

        for (LsEntry entry : entries) {
            String name = entry.getFilename();

            // skip . and ..
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }

            String fullPath = remoteDir + "/" + name;

            if (entry.getAttrs().isDir()) {
                // recursive delete
                removeRemoteFolder(fullPath);
            } else {
                // delete file
                ftp.rm(fullPath);
            }
        }

        // now directory is empty
        ftp.rmdir(remoteDir);
    }

    @Override
    public void getFilesFromServer(String remoteFolderName, String localFolderName)
            throws CommonException {
        log.debug("Start downloading from SFTP folder '{}' to local folder '{}'",
                remoteFolderName, localFolderName);

        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "SFTP server hasn't connected.");
        }

        try {
            SftpATTRS attrs = ftp.lstat(remoteFolderName);
            if (attrs == null) {
                throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                        "Remote path does not exist: " + remoteFolderName);
            }
            if (attrs.isDir()) {
                downloadFolder(remoteFolderName, localFolderName);
            } else if (attrs.isReg()) {
                downloadSingleFile(remoteFolderName, localFolderName);
            } else {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Unsupported file type for path: " + remoteFolderName);
            }
            log.debug("Download from SFTP path '{}' completed.", remoteFolderName);

        } catch (SftpException e) {
            log.error("Failed to process remote path '{}': {}", remoteFolderName, e.getMessage(),
                    e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to process remote path: " + e.getMessage());
        }

        log.debug("Download from SFTP folder '{}' completed.", remoteFolderName);
    }

    private void downloadSingleFile(String remoteFolderName, String localFolderName) {
        try {
            log.debug("download single file :{}", remoteFolderName);
            java.io.File localFolder = changeDir(localFolderName, null);
            String fileName = extractFileName(remoteFolderName);
            String localFilePath =
                    localFolder.getAbsolutePath() + "/" + fileName;
            String remoteDir = extractDirectoryPath(remoteFolderName);
            log.debug("Downloading single file '{}' from '{}' to '{}'",
                    fileName, remoteDir, localFilePath);
            ftp.download(remoteDir, fileName, localFilePath);
            log.info("Successfully downloaded file '{}' to '{}'", fileName, localFilePath);
        } catch (SftpException | IOException e) {
            String msg = String.format("Failed to download file '%s':%s", remoteFolderName,
                    e.getMessage());
            log.error(msg, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
    }


    private void downloadFolder(String remoteFolderName, String localFolderName) {
        final Vector<LsEntry> ftpFiles;
        try {
            ftpFiles = ftp.listFiles(remoteFolderName);
        } catch (Exception e) {
            log.error("Failed to list remote folder '{}': {}", remoteFolderName, e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }

        if (ftpFiles == null || ftpFiles.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "No files found in remote folder: " + remoteFolderName);
        }

        java.io.File localFolder = changeDir(localFolderName, null);

        for (LsEntry ftpFile : ftpFiles) {
            String name = ftpFile.getFilename();

            // 跳过当前目录和上级目录
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }

            String remoteItemPath = remoteFolderName + "/" + name;
            if (ftpFile.getAttrs().isDir()) {
                // 递归下载子目录
                java.io.File localSubFolder = changeDir(localFolder.getAbsolutePath(), name);
                downloadFolder(remoteItemPath, localSubFolder.getAbsolutePath());
            } else if (ftpFile.getAttrs().isReg()) {
                // 下载文件
                String localFilePath = localFolder.getAbsolutePath() + "/" + name;
                try {
                    log.debug("Downloading file '{}' from '{}' to '{}'",
                            name, remoteFolderName, localFilePath);
                    ftp.download(remoteFolderName, name, localFilePath);
                } catch (Exception e) {
                    String msg = String.format("Failed to download file '%s/%s': %s",
                            remoteFolderName, name, e.getMessage());
                    log.error(msg, e);
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
                }
            }
        }
    }

    /**
     * provide mkdir -p function
     * @param remotePath
     * @throws CommonException
     */
    @Override
    public void mkdir(String remotePath) throws CommonException {
        long start = System.currentTimeMillis();
        log.debug("start sftp mkdir for remotePath:{}", remotePath);
        String path = remotePath.replace("\\", "/");

        boolean absolute = path.startsWith("/");
        String[] folders = path.split("/");

        try {
            if (absolute) {
                ftp.changeDir("/");
            }
            String current = absolute ? "/" : ftp.pwd();
            for (String folder : folders) {
                if (folder == null || folder.isEmpty()) {
                    continue;
                }

                if (!current.endsWith("/")) {
                    current += "/";
                }
                current += folder;

                try {
                    SftpATTRS attrs = ftp.stat(current);
                    if (!attrs.isDir()) {
                        throw new SftpException(
                                ChannelSftp.SSH_FX_FAILURE,
                                "Path exists but is not a directory: " + current
                        );
                    }

                    ftp.changeDir(current);
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        // 不存在 → 创建
                        try {
                            ftp.makeDir(current);
                            ftp.changeDir(current);
                        } catch (SftpException ex) {
                            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
                        }
                    } else {
                        // 其他错误（包括权限）
                        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage());
                    }
                }
            }
        } catch (SftpException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        log.debug("finish sftp mkdir for remotePath:{} cost {} ms",
                remotePath, System.currentTimeMillis() - start);
    }

    @Override
    public boolean folderExists(String remoteFolder) {
        long start = System.currentTimeMillis();
        log.debug("start folderExists for remoteFolder:{}", remoteFolder);

        if (ftp == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "SFTP client not initialized");
        }

        if (!ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "SFTP server hasn't connected");
        }

        try {
            SftpATTRS attrs = ftp.lstat(remoteFolder);
            boolean exists = attrs != null && attrs.isDir();
            log.debug("finish folderExists for remoteFolder:{} result:{} cost {} ms",
                    remoteFolder, exists, System.currentTimeMillis() - start);
            return exists;
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                log.debug("finish folderExists for remoteFolder:{} result:false cost {} ms",
                        remoteFolder, System.currentTimeMillis() - start);
                return false;
            }
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
    }


    @Override
    public void close() throws Exception {
        ftp.logout();
    }

    private java.io.File changeDir(String parent, String newFolderName) {
        java.io.File local = new java.io.File(parent);
        if (!local.exists()) {
            local.mkdir();
        }
        if (newFolderName == null) {
            return local;
        }

        newFolderName = newFolderName.trim();
        if (newFolderName.length() == 0) {
            return local;
        }

        String folder = parent + "/" + newFolderName;
        local = new java.io.File(folder);
        if (!local.exists()) {
            local.mkdir();
        }
        return local;
    }

    private void putSingleFileToServer(String remotePath, String localFileName)
            throws CommonException {
        java.io.File localFile = new java.io.File(localFileName);
        if (!localFile.exists()) {
            String msg =
                    "Can't upload '" + localFile.getAbsolutePath() + "'. This file doesn't exist.";
            log.debug(msg);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

        try {
            if (localFile.isFile()) {
                log.debug("put file {} to server: {}, remote path is {} ",
                        localFile.getAbsolutePath(), ftp.getHost(), remotePath);
                ftp.upLoadFile(remotePath, localFileName);
            } else {
                log.debug("put folder {} to server: {}, remote path is {} ",
                        localFile.getAbsolutePath(), ftp.getHost(), remotePath);
                ftp.upLoadFile(remotePath, localFileName);
            }
        } catch (Exception e) {
            log.error("failed to put single file to server reason is:{}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
    }


    private List<File> convert2OutputFiles(Vector<LsEntry> ftpFiles) {
        List<File> output = new LinkedList<>();
        for (LsEntry ftpFile : ftpFiles) {
            String name = ftpFile.getFilename();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            // 只处理普通文件和目录
            if (!ftpFile.getAttrs().isReg() && !ftpFile.getAttrs().isDir()) {
                continue;
            }

            FileBuilder fb = new FileBuilder()
                    .setType(ftpFile.getAttrs().isDir() ? Type.Folder : Type.File)
                    .setName(ftpFile.getFilename())
                    .setSize(ftpFile.getAttrs().getSize())
                    .setKey(new FileKey(ftpFile.getFilename()));
            fb.setCreationTime(
                    BigInteger.valueOf(ftpFile.getAttrs().getMTime() * 1000L));

            output.add(fb.build());
        }
        return output;
    }

}
