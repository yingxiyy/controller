/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FTPUtil {

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final int DEFAULT_TIMEOUT = 60 * 1000;
    private static final String DAILY_FILE_PATH = "dailyFilePath";
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final transient Logger log = LoggerFactory.getLogger(this.getClass());
    private final FTPClient ftpClient;
    private volatile String ftpBasePath;

    public FTPUtil(String host, int port, String username, String password) {
        this(host, port, username, password, DEFAULT_CHARSET);
        setTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

    private FTPUtil(String host, int port, String username, String password, String charset) {
        ftpClient = new FTPClient();
        ftpClient.setControlEncoding(charset);
        this.host = StringUtils.isEmpty(host) ? "localhost" : host;
        this.port = (port <= 0) ? 21 : port;
        this.username = StringUtils.isEmpty(username) ? "anonymous" : username;
        this.password = password;
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ex) {
                // do nothing
            }
        }
    }

    public String getHost() {
        return host;
    }

    public void setTimeout(int defaultTimeout, int connectTimeout, int dataTimeout) {
        ftpClient.setDefaultTimeout(defaultTimeout);
        ftpClient.setConnectTimeout(connectTimeout);
        ftpClient.setDataTimeout(dataTimeout);
    }

    public boolean connect() throws IOException {
        try {
            ftpClient.connect(host, port);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                disconnect();
                log.error("Can't connect to server :{}", host);
                return false;
            }
            if (!ftpClient.login(username, password)) {
                disconnect();
                log.error("Can't login to server :{}", host);
                return false;
            }
            // set data transfer mode.
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            // Use passive mode to pass firewalls.
            ftpClient.enterLocalPassiveMode();
            initFtpBasePath();
            return true;
        } catch (UnknownHostException e) {
            throw new IOException("Can't find FTP server :" + host);
        }
    }

    private void initFtpBasePath() throws IOException {
        if (StringUtils.isEmpty(ftpBasePath)) {
            synchronized (this) {
                if (StringUtils.isEmpty(ftpBasePath)) {
                    ftpBasePath = ftpClient.printWorkingDirectory();
                }
            }
        }
    }

    public boolean isConnected() {
        return ftpClient.isConnected();
    }

    public String uploadFileToDailyDir(String fileName, InputStream inputStream)
            throws IOException {
        changeWorkingDirectory(ftpBasePath);
        SimpleDateFormat dateFormat = new SimpleDateFormat("/yyyy/MM/dd");
        String formatDatePath = dateFormat.format(new Date());
        String uploadDir = DAILY_FILE_PATH + formatDatePath;
        makeDirs(uploadDir);
        storeFile(fileName, inputStream);
        return formatDatePath + "/" + fileName;
    }

    public void downloadFileFromDailyDir(String dailyDirFilePath, OutputStream outputStream)
            throws IOException {
        changeWorkingDirectory(ftpBasePath);
        String ftpRealFilePath = DAILY_FILE_PATH + dailyDirFilePath;
        ftpClient.retrieveFile(ftpRealFilePath, outputStream);
    }

    /**
     * @param ftpFileName file or folder name on FTP server.
     * @param out
     * @throws IOException
     */
    public boolean retrieveFile(String ftpFileName, OutputStream out) throws IOException {
        try {
            FTPFile[] fileInfoArray = ftpClient.listFiles(ftpFileName);
            if (fileInfoArray == null || fileInfoArray.length == 0) {
                throw new FileNotFoundException(
                        "File '" + ftpFileName + "' was not found on FTP server.");
            }

            FTPFile fileInfo = fileInfoArray[0];
            if (fileInfo.getSize() > Integer.MAX_VALUE) {
                throw new IOException("File '" + ftpFileName + "' is too large.");
            }

            if (!ftpClient.retrieveFile(ftpFileName, out)) {
                throw new IOException("Error loading file '" + ftpFileName
                        + "' from FTP server. Check FTP permissions and path.");
            }
            out.flush();
            return true;
        } finally {
            closeStream(out);
        }
    }

    public void storeFile(String ftpFileName, InputStream in) throws IOException {
        try {
            if (!ftpClient.storeFile(ftpFileName, in)) {
                throw new IOException("Can't upload file '" + ftpFileName
                        + "' to FTP server. Check FTP permissions and path.");
            }
        } finally {
            closeStream(in);
        }
    }

    public void deleteFile(String ftpFileName) throws IOException {
        if (!ftpClient.deleteFile(ftpFileName)) {
            throw new IOException("Can't remove file '" + ftpFileName + "' from FTP server.");
        }
    }

    public void upload(String ftpFileName, File localFile) throws IOException {
        if (!localFile.exists()) {
            throw new IOException(
                    "Can't upload '" + localFile.getAbsolutePath() + "'. This file doesn't exist.");
        }

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(localFile));
            if (!ftpClient.storeFile(ftpFileName, in)) {
                throw new IOException("Can't upload file '" + ftpFileName
                        + "' to FTP server. Check FTP permissions and path.");
            }
        } finally {
            closeStream(in);
        }
    }

    public void uploadDir(String remotePath, String localPath) throws IOException {
        localPath = localPath.replace("\\\\", "/");
        File file = new File(localPath);
        if (file.exists()) {
            if (!ftpClient.changeWorkingDirectory(remotePath)) {
                ftpClient.makeDirectory(remotePath);
                ftpClient.changeWorkingDirectory(remotePath);
            }
            File[] files = file.listFiles();
            if (null != files) {
                for (File f : files) {
                    if (f.isDirectory() && !f.getName().equals(".") && !f.getName().equals("..")) {
                        uploadDir(remotePath + "/" + f.getName(), f.getPath());
                    } else if (f.isFile()) {
                        upload(remotePath + "/" + f.getName(), f);
                    }
                }
            }
        }
    }

    public void download(String ftpFileName, File localFile) throws IOException {
        OutputStream out = null;
        try {
            FTPFile[] fileInfoArray = ftpClient.listFiles(ftpFileName);
            if (fileInfoArray == null || fileInfoArray.length == 0) {
                throw new FileNotFoundException(
                        "File " + ftpFileName + " was not found on FTP server.");
            }

            FTPFile fileInfo = fileInfoArray[0];
            if (fileInfo.getSize() > Integer.MAX_VALUE) {
                throw new IOException("File " + ftpFileName + " is too large.");
            }

            out = new BufferedOutputStream(new FileOutputStream(localFile));
            if (!ftpClient.retrieveFile(ftpFileName, out)) {
                throw new IOException("Error loading file " + ftpFileName
                        + " from FTP server. Check FTP permissions and path.");
            }
            out.flush();
        } finally {
            closeStream(out);
        }
    }

    public boolean changeWorkingDirectory(String dir) {
        if (!ftpClient.isConnected()) {
            return false;
        }
        try {
            return ftpClient.changeWorkingDirectory(dir);
        } catch (IOException e) {
        }

        return false;
    }

    public void downloadDir(String remotePath, String localPath) throws IOException {
        localPath = localPath.replace("\\\\", "/");
        File file = new File(localPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        FTPFile[] ftpFiles = ftpClient.listFiles(remotePath);
        for (int i = 0; ftpFiles != null && i < ftpFiles.length; i++) {
            FTPFile ftpFile = ftpFiles[i];
            if (ftpFile.isDirectory() && !ftpFile.getName().equals(".") && !ftpFile.getName()
                    .equals("..")) {
                downloadDir(remotePath + "/" + ftpFile.getName(),
                        localPath + "/" + ftpFile.getName());
            } else {
                download(remotePath + "/" + ftpFile.getName(),
                        new File(localPath + "/" + ftpFile.getName()));
            }
        }
    }

    public List<String> listFileNames(String filePath) throws IOException {
        FTPFile[] ftpFiles = ftpClient.listFiles(filePath);
        List<String> fileList = new ArrayList<>();
        if (ftpFiles != null) {
            for (int i = 0; i < ftpFiles.length; i++) {
                FTPFile ftpFile = ftpFiles[i];
                if (ftpFile.isFile()) {
                    fileList.add(ftpFile.getName());
                }
            }
        }

        return fileList;
    }

    public void sendSiteCommand(String args) throws IOException {
        if (!ftpClient.isConnected()) {
            ftpClient.sendSiteCommand(args);
        }
    }

    public String printWorkingDirectory() {
        if (!ftpClient.isConnected()) {
            return "";
        }

        try {
            return ftpClient.printWorkingDirectory();
        } catch (IOException e) {
            // do nothing
        }

        return "";
    }

    public boolean changeToParentDirectory() {

        if (!ftpClient.isConnected()) {
            return false;
        }

        try {
            return ftpClient.changeToParentDirectory();
        } catch (IOException e) {
            // do nothing
        }

        return false;
    }

    public String printParentDirectory() {
        if (!ftpClient.isConnected()) {
            return "";
        }

        String w = printWorkingDirectory();
        changeToParentDirectory();
        String p = printWorkingDirectory();
        changeWorkingDirectory(w);

        return p;
    }

    public boolean makeDirectory(String pathname) throws IOException {
        return ftpClient.makeDirectory(pathname);
    }

    public void makeDirs(String pathname) throws IOException {
        pathname = pathname.replace("\\\\", "/");
        String[] pathnameArray = pathname.split("/");
        for (String each : pathnameArray) {
            if (StringUtils.isNotEmpty(each)) {
                ftpClient.makeDirectory(each);
                ftpClient.changeWorkingDirectory(each);
            }
        }
    }

    public void disconnect() {
        if (null != ftpClient && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ex) {
                // do nothing
            }
        }
    }

    public FTPFile[] listFiles(String folderName) throws IOException {
        FTPFile[] ftpFiles = ftpClient.listFiles(folderName);
        return ftpFiles;
    }

    public boolean lstat(String remotePath) throws IOException {
        FTPFile[] files = ftpClient.listFiles(remotePath);
        return files != null && files.length > 0;
    }

    public String getReplyString() {
        return ftpClient.getReplyString();
    }
}