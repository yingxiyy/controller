/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.ftp.client;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.sftp.manager.utils.FTPUtil;
import org.apache.commons.net.ftp.FTPFile;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.FileAttributes;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.File;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.FileBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.list.output.FileKey;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static net.flex.dci.otn.controller.sftp.manager.utils.FtpManagerUtil.extractDirectoryPath;
import static net.flex.dci.otn.controller.sftp.manager.utils.FtpManagerUtil.extractFileName;

@Slf4j
public class FtpClient implements IClient {


    private FTPUtil ftp;

    public FtpClient() {
    }

    @Override
    public boolean testConnect(String host, int port, String username, String password)
            throws CommonException {
        ftp = new FTPUtil(host, port, username, password);
        try {
            boolean connected = ftp.connect();
            return connected;
        } catch (Exception e) {
            log.error("---test connect failed---{}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }

    }

    @Override
    public void connect(String host, int port, String username, String password)
            throws CommonException {
        ftp = new FTPUtil(host, port, username, password);

        try {
            ftp.connect();
        } catch (Exception e) {
            log.error("---connect server failed---{}", e.getMessage(), e);
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
            log.error("---connect ftp server failed---{}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }

    }

    @Override
    public void disConnect() {
        ftp.disconnect();
    }

    @Override
    public List<File> listFilesOnServer(String remoteFolderName) throws CommonException {
        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "FTP server hasn't connected.");
        }
        FTPFile[] ftpFiles;
        try {
            ftpFiles = ftp.listFiles(remoteFolderName);
        } catch (IOException e) {
            log.error("--list files failed--{}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
        return convert2OutputFile(ftpFiles);
    }

    private List<File> convert2OutputFile(FTPFile[] ftpFiles) {
        List<File> output = new LinkedList<>();
        for (FTPFile ftpFile : ftpFiles) {
            // 跳过当前目录和上级目录
            if (".".equals(ftpFile.getName()) || "..".equals(ftpFile.getName())) {
                continue;
            }
            if (!ftpFile.isDirectory() && !ftpFile.isFile()) {
                continue;
            }

            FileBuilder fb = new FileBuilder()
                    .setType(ftpFile.isDirectory() ? FileAttributes.Type.Folder
                            : FileAttributes.Type.File)
                    .setName(ftpFile.getName())
                    .setSize(ftpFile.getSize())
                    .setKey(new FileKey(ftpFile.getName()));
            fb.setCreationTime(
                    BigInteger.valueOf(ftpFile.getTimestamp().getTime().getTime()));
            output.add(fb.build());
        }
        return output;
    }


    @Override
    public void putFilesToServer(String remotePath, String localFileName) throws CommonException {
        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "FTP server hasn't connected.");
        }
        java.io.File localFile = new java.io.File(localFileName);
        if (!localFile.exists()) {
            String msg =
                    "Can't upload '" + localFile.getAbsolutePath() + "'. This file doesn't exist.";
            log.equals(msg);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }

        try {
            if (localFile.isFile()) {
                log.debug("put file {} to server: {}, remote path is {} ",
                        localFile.getAbsolutePath(), ftp.getHost(), remotePath);
                ftp.upload(remotePath + "/" + localFile.getName(), localFile);
            } else {
                log.debug("put folder {} to server: {}, remote path is {} ",
                        localFile.getAbsolutePath(), ftp.getHost(), remotePath);
                ftp.uploadDir(remotePath, localFile.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error("--put file to server failed--{}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
    }

    @Override
    public void rmFilesFromServer(String remotePath, String remoteFileName) throws CommonException {

        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "SFTP server hasn't connected.");
        }

        try {
            ftp.changeWorkingDirectory(remotePath);
            ftp.deleteFile(remoteFileName);
        } catch (IOException e) {
            String msg = String.format("Failed to remove file '%s/%s':%s", remotePath, remoteFileName,
                    e.getMessage());
            log.error(msg, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
    }

    @Override
    public void rmFolderFromServer(String remotePath) throws CommonException {
        //TODO
    }

    @Override
    public void getFilesFromServer(String remotePath, String localFolderName)
            throws CommonException {
        log.debug("Start downloading file from FTP folder '{}' to local folder '{}'",
                remotePath, localFolderName);
        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "FTP server hasn't connected.");
        }
        try {
            if (isRemoteDirectory(remotePath)) {
                downloadFtpFolder(remotePath, localFolderName);
            } else {
                downloadSingleFile(remotePath, localFolderName);
            }
            log.debug("Download from FTP path '{}' completed.", remotePath);
        } catch (IOException e) {
            log.error("FTP error while processing path '{}': {}", remotePath, e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "FTP error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process remote path '{}': {}", remotePath, e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Failed to process remote path: " + e.getMessage());
        }
    }

    private boolean isRemoteDirectory(String remotePath) {
        String currentWorkingDir = ftp.printWorkingDirectory();
        try {
            boolean isDirectory = ftp.changeWorkingDirectory(remotePath);
            if (currentWorkingDir != null) {
                ftp.changeWorkingDirectory(currentWorkingDir);
            }

            return isDirectory;
        } catch (Exception e) {
            log.debug("Path '{}' is not a directory or doesn't exist: {}", remotePath,
                    e.getMessage());
            return false;
        }
    }

    private void downloadFtpFolder(String remoteFilePath, String localFolderName)
            throws FileNotFoundException {
        java.io.File localFolder = changeDir(localFolderName, null);

        // 从远程文件路径中提取文件名
        String fileName = extractFileName(remoteFilePath);
        String localFilePath = localFolder.getAbsolutePath() + "/" + fileName;

        // 提取远程文件所在目录
        String remoteDir = extractDirectoryPath(remoteFilePath);

        log.debug("Downloading single FTP file '{}' from '{}' to '{}'",
                fileName, remoteDir, localFilePath);

        // 切换到远程文件所在目录
        String originalDir = ftp.printWorkingDirectory();
        if (remoteDir != null && !remoteDir.isEmpty() && !".".equals(remoteDir)) {
            if (!ftp.changeWorkingDirectory(remoteDir)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Cannot change to remote directory: " + remoteDir);
            }
        }

        try (OutputStream output = Files.newOutputStream(Paths.get(localFilePath))) {
            boolean success = ftp.retrieveFile(fileName, output);
            if (!success) {
                String reply = ftp.getReplyString();
                throw new IOException(
                        "FTP retrieveFile failed for " + fileName + ". Server reply: " + reply);
            }
            log.info("Successfully downloaded FTP file '{}' to '{}'", fileName, localFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // 恢复原始工作目录
            if (originalDir != null) {
                ftp.changeWorkingDirectory(originalDir);
            }
        }
    }

    private void downloadSingleFile(String remoteFolderName, String localFolderName) {
        log.debug("Start downloading from FTP folder '{}' to local folder '{}'",
                remoteFolderName, localFolderName);

        if (ftp == null || !ftp.isConnected()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "FTP server hasn't connected.");
        }

        try {
            FTPFile[] ftpFiles = ftp.listFiles(remoteFolderName);
            if (ftpFiles == null || ftpFiles.length == 0) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "No files found in remote folder: " + remoteFolderName);
            }

            java.io.File localFolder = changeDir(localFolderName, null);

            for (FTPFile ftpFile : ftpFiles) {
                String name = ftpFile.getName();

                // 跳过当前目录和上级目录
                if (".".equals(name) || "..".equals(name)) {
                    continue;
                }

                if (ftpFile.isDirectory()) {
                    // 递归下载子目录
                    java.io.File localSubFolder = changeDir(localFolder.getAbsolutePath(), name);
                    getFilesFromServer(remoteFolderName + "/" + name,
                            localSubFolder.getAbsolutePath());
                } else if (ftpFile.isFile()) {
                    // 下载文件
                    String localFilePath = localFolder.getAbsolutePath() + "/" + name;
                    try (OutputStream output = new FileOutputStream(localFilePath)) {
                        log.debug("Downloading file '{}' from '{}' to '{}'",
                                name, remoteFolderName, localFilePath);
                        boolean success = ftp.retrieveFile(remoteFolderName + "/" + name, output);
                        if (!success) {
                            throw new IOException("FTP retrieveFile returned false for " + name);
                        }
                    } catch (IOException e) {
                        String msg = String.format("Failed to download file '%s/%s': %s",
                                remoteFolderName, name, e.getMessage());
                        log.error(msg, e);
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
                    }
                }
            }
            log.debug("Download from FTP folder '{}' completed.", remoteFolderName);
        } catch (IOException e) {
            log.error("FTP error while downloading from '{}': {}", remoteFolderName, e.getMessage(),
                    e);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
    }

//    public void getFilesFromServeOld(String remoteFolderName, String localFolderName)
//            throws CommonException {
//        log.debug("Start downloading from FTP folder '{}' to local folder '{}'",
//                remoteFolderName, localFolderName);
//
//        if (ftp == null || !ftp.isConnected()) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "FTP server hasn't connected.");
//        }
//
//        try {
//            FTPFile[] ftpFiles = ftp.listFiles(remoteFolderName);
//            if (ftpFiles == null || ftpFiles.length == 0) {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        "No files found in remote folder: " + remoteFolderName);
//            }
//
//            java.io.File localFolder = changeDir(localFolderName, null);
//
//            for (FTPFile ftpFile : ftpFiles) {
//                String name = ftpFile.getName();
//
//                // 跳过当前目录和上级目录
//                if (".".equals(name) || "..".equals(name)) {
//                    continue;
//                }
//
//                if (ftpFile.isDirectory()) {
//                    // 递归下载子目录
//                    java.io.File localSubFolder = changeDir(localFolder.getAbsolutePath(), name);
//                    getFilesFromServer(remoteFolderName + "/" + name,
//                            localSubFolder.getAbsolutePath());
//                } else if (ftpFile.isFile()) {
//                    // 下载文件
//                    String localFilePath = localFolder.getAbsolutePath() + "/" + name;
//                    try (OutputStream output = new FileOutputStream(localFilePath)) {
//                        log.debug("Downloading file '{}' from '{}' to '{}'",
//                                name, remoteFolderName, localFilePath);
//                        boolean success = ftp.retrieveFile(remoteFolderName + "/" + name, output);
//                        if (!success) {
//                            throw new IOException("FTP retrieveFile returned false for " + name);
//                        }
//                    } catch (IOException e) {
//                        String msg = String.format("Failed to download file '%s/%s': %s",
//                                remoteFolderName, name, e.getMessage());
//                        log.error(msg, e);
//                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
//                    }
//                }
//            }
//            log.debug("Download from FTP folder '{}' completed.", remoteFolderName);
//        } catch (IOException e) {
//            log.error("FTP error while downloading from '{}': {}", remoteFolderName, e.getMessage(),
//                    e);
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
//        }
//    }

    private java.io.File changeDir(String parent, String newFolderName) {
        // 先确保父目录存在
        java.io.File local = new java.io.File(parent);
        if (!local.exists()) {
            boolean created = local.mkdirs();
            if (!created) {
                log.warn("Failed to create local directory: {}", parent);
            }
        }

        // 如果没有子目录名，直接返回父目录
        if (newFolderName == null || newFolderName.trim().isEmpty()) {
            return local;
        }

        // 拼接子目录路径
        String folderPath = parent + java.io.File.separator + newFolderName.trim();
        java.io.File subFolder = new java.io.File(folderPath);
        if (!subFolder.exists()) {
            boolean created = subFolder.mkdirs();
            if (!created) {
                log.warn("Failed to create local subdirectory: {}", folderPath);
            }
        }
        return subFolder;
    }


    @Override
    public void mkdir(String path) throws CommonException {
        try {
            ftp.makeDirs(path);
        } catch (IOException e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
    }

    @Override
    public boolean folderExists(String remoteFolder) {
        return false;
    }

    @Override
    public void close() throws Exception {
        ftp.disconnect();
    }

}
