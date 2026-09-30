/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.utils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Vector;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.sftp.manager.monitor.DirectoryUploadProgressMonitor;
import net.flex.dci.otn.controller.sftp.manager.monitor.DownloadFileProgressMonitor;
import net.flex.dci.otn.controller.sftp.manager.monitor.UploadFileProgressMonitor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SFTPUtil {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private ChannelSftp sftp;

    private Session session;
    private String username;
    private String password;
    private String privateKey;
    private String host;
    private int port;


    public SFTPUtil(String host, int port, String username, String password) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    public SFTPUtil(String host, int port, String username, String password, String privateKey) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.privateKey = privateKey;
        this.password = null;
    }

    public SFTPUtil() {
    }

    public boolean login() throws JSchException {
        long start = System.currentTimeMillis();
        log.debug("start sftp login host:{} port:{} user:{}", host, port, username);
        try {
            JSch jsch = new JSch();
            if (privateKey != null) {
                jsch.addIdentity(privateKey);
            }

            session = jsch.getSession(username, host, port);

            if (password != null) {
                session.setPassword(password);
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");

            session.setConfig(config);
            session.connect(30000);

            session.setServerAliveInterval(10000);

            Channel channel = session.openChannel("sftp");
            channel.connect(30000);

            sftp = (ChannelSftp) channel;
            log.debug("finish sftp login host:{} port:{} user:{} cost {} ms",
                    host, port, username, System.currentTimeMillis() - start);
            return true;
        } catch (JSchException e) {
            log.error("failed to connect SFTP server host:{} port:{} user:{} after {} ms",
                    host, port, username, System.currentTimeMillis() - start, e);
            return false;
        }
    }

    public void logout() {
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
            }
        }
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }


    /**
     * 将输入流的数据上传到sftp作为文件。文件完整路径=basePath+directory
     *
     * @param basePath 服务器的基础路径
     * @param directory 上传到该目录
     * @param sftpFileName sftp端文件名
     * @param input 输入流
     */
    public void upload(String basePath, String directory, String sftpFileName, InputStream input)
            throws SftpException {
        try {
            sftp.cd(basePath);
            sftp.cd(directory);
        } catch (SftpException e) {
            //目录不存在，则创建文件夹
            String[] dirs = directory.split("/");
            String tempPath = basePath;
            for (String dir : dirs) {
                if (null == dir || "".equals(dir)) {
                    continue;
                }
                tempPath += "/" + dir;
                try {
                    sftp.cd(tempPath);
                } catch (SftpException ex) {
                    sftp.mkdir(tempPath);
                    sftp.chmod(0755, tempPath);
                    sftp.cd(tempPath);
                }
            }
        }
        sftp.put(input, sftpFileName);  //上传文件
    }


    /**
     * 下载文件。
     *
     * @param directory 下载目录
     * @param downloadFile 下载的文件
     * @param saveFile 存在本地的路径
     */
    public void download(String directory, String downloadFile, String saveFile)
            throws SftpException, IOException {
        if (directory != null && !"".equals(directory)) {
            sftp.cd(directory);
        }
        File file = new File(saveFile);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            sftp.get(downloadFile, fos, new DownloadFileProgressMonitor());
        }
    }


    public void downloadFile(String remoteFilePath, String saveFile)
            throws SftpException, FileNotFoundException {
        File file = new File(saveFile);
        sftp.get(remoteFilePath, new FileOutputStream(file)); // 直接用完整路径
    }

    /**
     * 下载文件
     *
     * @param directory 下载目录
     * @param downloadFile 下载的文件名
     * @return 字节数组
     */
    public byte[] download(String directory, String downloadFile)
            throws SftpException, IOException {
        if (directory != null && !"".equals(directory)) {
            sftp.cd(directory);
        }
        InputStream is = sftp.get(downloadFile);

        byte[] fileData = IOUtils.toByteArray(is);

        return fileData;
    }


    /**
     * 删除文件
     *
     * @param directory 要删除文件所在目录
     * @param deleteFile 要删除的文件
     */
    public void delete(String directory, String deleteFile) throws SftpException {
        ensureRemoteDirectoryExists(directory);
        sftp.rm(deleteFile);
    }

    public void rm(String deleteFile) throws SftpException {
        sftp.rm(deleteFile);
    }

    public void rmdir(String deleteFile) throws SftpException {
        sftp.rmdir(deleteFile);
    }


    /**
     * 列出目录下的文件
     *
     * @param directory 要列出的目录
     * @param directory
     */
    public Vector<LsEntry> listFiles(String directory) throws SftpException {
        if (directory.startsWith("/")) {
            return sftp.ls(directory);
        } else {
            return sftp.ls(sftp.getHome() + "/" + directory);
        }
    }

//    //上传文件测试
//    public static void main(String[] args) throws SftpException, IOException {
//        SFTPUtil sftp = new SFTPUtil("ip地址", 22, "用户名", "密码");
//        sftp.login();
//        File file = new File("D:\\图片\\t0124dd095ceb042322.jpg");
//        InputStream is = new FileInputStream(file);
//
//        sftp.upload("基础路径","文件路径", "test_sftp.jpg", is);
//        sftp.logout();
//    }
//
    public boolean isConnected() {
        return sftp != null && sftp.isConnected();
    }

    public void upLoadFile(String remoteDir, String localPath) {
        File file = new File(localPath);
        if (!file.exists()) {
            log.error("local file :{} not exists", localPath);
            return;
        }
        try {
            ensureRemoteDirectoryExists(remoteDir);
            copyFile(file, remoteDir);
            log.info("upload file finished:{}->{}", localPath, remoteDir);
        } catch (SftpException e) {
            log.error("SFTP operation failed: remoteDir={}, localPath={}", remoteDir, localPath, e);
            throw new RuntimeException(e);
        }
    }

    private void copyFile(File file, String remoteDir) {
        log.debug("upload file :{} to remoteDir:{}", file, remoteDir);
        try {
            if (file.isDirectory()) {
                copyDirectory(file, remoteDir);
            } else {
                uploadSingleFile(file, remoteDir);
            }
        } catch (IOException | SftpException e) {
            log.error("Failed to copy {} to {}", file.getAbsolutePath(), remoteDir, e);
            throw new RuntimeException("SFTP copy failed", e);
        }
    }

    private void copyDirectory(File dir, String remoteDir) throws SftpException, IOException {
        log.debug("uploading directory:{} file ->{}", dir, remoteDir);
        String newRemoteDir = remoteDir + "/" + dir.getName();;
        ensureRemoteDirectoryExists(newRemoteDir);
        File[] children = dir.listFiles();
        DirectoryUploadProgressMonitor directoryUploadProgressMonitor = new DirectoryUploadProgressMonitor(
                dir);
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    copyDirectory(child, newRemoteDir);
                } else {
                    uploadSingleFile(child, newRemoteDir);
                    directoryUploadProgressMonitor.fileUploaded(child.getAbsolutePath());
                }
            }
        }
    }


    private void uploadSingleFile(File file, String remoteDir) throws SftpException, IOException {
        log.debug("uploading file :{}->{}/{}", file.getAbsolutePath(), remoteDir,
                file.getName());
        ensureRemoteDirectoryExists(remoteDir);
        try (InputStream in = Files.newInputStream(file.toPath())) {
            sftp.put(in, file.getName(), new UploadFileProgressMonitor(file.length()),
                    ChannelSftp.OVERWRITE);
        }
        log.info("Uploaded file: {} -> {}/{}", file.getAbsolutePath(), remoteDir, file.getName());
    }

//    private void copyFile(File file, String pwd) {
//        if (file.isDirectory()) {
//            File[] list = file.listFiles();
//            try {
//                try {
//                    String fileName = file.getName();
//                    sftp.cd(pwd);
//                    log.debug("creating folder:" + sftp.pwd() + "/" + fileName);
//                    sftp.mkdir(fileName);
//                    log.debug("folder created successful:" + sftp.pwd() + "/" + fileName);
//                } catch (Exception e) {
//                    // TODO: handle exception
//                }
//                pwd = pwd + "/" + file.getName();
//                try {
//
//                    sftp.cd(file.getName());
//                } catch (SftpException e) {
//                    // TODO: handle exception
//                    e.printStackTrace();
//                }
//            } catch (Exception e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            for (int i = 0; i < list.length; i++) {
//                copyFile(list[i], pwd);
//            }
//        } else {
//
//            try {
//                sftp.cd(pwd);
//            } catch (SftpException e1) {
//                e1.printStackTrace();
//            }
//            log.debug("copying file:" + file.getAbsolutePath());
//            InputStream instream = null;
//            OutputStream outstream = null;
//            try {
//                outstream = sftp.put(file.getName());
//                instream = new FileInputStream(file);
//
//                byte b[] = new byte[1024];
//                int n;
//                try {
//                    while ((n = instream.read(b)) != -1) {
//                        outstream.write(b, 0, n);
//                    }
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//
//            } catch (SftpException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } finally {
//                try {
//                    outstream.flush();
//                    outstream.close();
//                    instream.close();
//
//                } catch (Exception e2) {
//                    // TODO: handle exception
//                    e2.printStackTrace();
//                }
//            }
//        }
//    }


    private void ensureRemoteDirectoryExists(String remoteDir) throws SftpException {
        String[] folders = remoteDir.split("/");
        String path = "";
        for (String folder : folders) {
            if (folder.isEmpty()) {
                continue;
            }
            path += "/" + folder;
            if (!remoteDirectoryExists(path)) {
                log.debug("Creating remote folder:{}", path);
                sftp.mkdir(path);
                sftp.chmod(0755, path);
            }
            sftp.cd(path);
        }
    }

    public void makeDir(String pathname) throws SftpException {
        sftp.mkdir(pathname);
        sftp.chmod(0755, pathname);
    }

    public void changeDir(String pathname) throws SftpException {
        sftp.cd(pathname);
    }

    public Object getHost() {
        return host;
    }

    public SftpATTRS lstat(String remotePath) throws SftpException {
        return sftp.lstat(remotePath);
    }

    public SftpATTRS stat(String remotePath) throws SftpException {
        return sftp.stat(remotePath);
    }

    public String pwd() throws CommonException {
        try {
            return sftp.pwd();
        } catch (SftpException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage());
        }
    }


    private boolean remoteDirectoryExists(String path) {
        try {
            SftpATTRS attrs = sftp.stat(path);
            return attrs != null && attrs.isDir();
        } catch (SftpException e) {
            return false;
        }
    }
}
