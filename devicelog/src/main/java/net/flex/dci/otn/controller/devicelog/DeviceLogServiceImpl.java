package net.flex.dci.otn.controller.devicelog;

import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.DbOperationType.DBOPLOGUPLOAD;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.IpUtils;
import net.flex.dci.otc.common.util.IpUtils.IpVersion;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.configuration.ConfigLoader;
import net.flex.dci.otn.controller.pm.FTPClient;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.database.operate.input.SftpServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.database.operate.input.SftpServerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FileTransferProtocol;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceLogServiceImpl {

    public static final String DEVICE_LOG_FOLDER = "/upload/devicelog";

    private final PhyNodeDao phyNodeDao;
    private final FTPClient ftpClient;
    private final NeManagerRpc neRpc;

    public void download(String neId, HttpServletResponse response) throws CommonException {

        File localBaseDir = null;
        String neName = null;

        try {
            // 获取网元信息
            Node node = phyNodeDao.getOpPhyNodeById(neId);
            if (node == null) {
                throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                        "NE not found: " + neId);
            }
            Physical phy = node.getAugmentation(Node1.class).getPhysical();
            neName = phy.getFriendlyName();
            String neIp = phy.getIp();
            IpVersion ipVersion = IpUtils.getIpVersion(neIp);
            FtpServer ftpServer = ftpClient.pickupFtpServerByIpVersion(ipVersion);
            if (ftpServer == null) {
                log.error("No FTP server available for IP version: {}", ipVersion);
                throw new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR,
                        "No FTP server found for IP version: " + ipVersion);
            }
            // 准备目录
            localBaseDir = new File(ConfigLoader.PROCESSING_ROOT, neName + "_devicelog");
            cleanDirectory(localBaseDir);
            localBaseDir.mkdirs();

            String remoteDir = DEVICE_LOG_FOLDER + "/" + neName;
            ftpClient.createRemoteFolder(ftpServer, remoteDir);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String dateString = sdf.format(new Date());
            // RPC调用设备上报日志
            String fileName = neName + "_" + dateString + ".tar.gz";

            rpcCall(neId, ftpServer, remoteDir, neIp, fileName);

            // 下载文件
            ftpClient.downloadFile(ftpServer, remoteDir, localBaseDir.getAbsolutePath());

            // 删除远程文件
            ftpClient.removeRemoteFile(ftpServer, remoteDir, fileName);

            // 查找下载的文件
            File logFile = new File(localBaseDir, fileName);
            if (!logFile.exists()) {
                throw new CommonException(CommonExceptionType.FILE_NOT_FOUND_ERROR,
                        "Downloaded file not found: " + fileName);
            }

            // 发送文件到浏览器
            sendFileToBrowser(logFile, response);

        } catch (Exception e) {
            log.error("Device log download failed: {}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR,
                    "Device log download failed: " + e.getMessage(), e);
        } finally {
            // 清理本地目录
            if (localBaseDir != null) {
                cleanDirectory(localBaseDir);
            }
        }
    }

    private void rpcCall(String neId, FtpServer ftpServer, String path, String sourceAddress,
            String fileName) {

        SftpServer sftpServer = new SftpServerBuilder()
                .setAddress(ftpServer.getAddress())
                .setPort(ftpServer.getPort())
                .setUser(ftpServer.getUser())
                .setPassword(ftpServer.getPassword())
                .setUploadPath(path)
                .setDownloadPath(path)
                .setProtocol(FileTransferProtocol.SFTP)
                .setSourceAddress(sourceAddress)
                .build();

        NeDatabaseOperateInput input = new NeDatabaseOperateInputBuilder()
                .setDbOperation(DBOPLOGUPLOAD)
                .setNodeId(new NodeId(neId))
                .setFileName(fileName)
                .setSftpServer(sftpServer)
                .build();

        NeDatabaseOperateOutput output = null;
        try {
            output = neRpc.neDatabaseOperate(input);
        } catch (Exception e) {
            log.error("RPC call failed: {}, {}", neId, e.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Upload log failed.", e);
        }
        if (Objects.isNull(output) || StringUtils.containsIgnoreCase(output.getResult(),
                "failed")) {
            throw new RuntimeException("Upload log failed: " + output.getResult());
        }
    }

    private void sendFileToBrowser(File file, HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        response.setHeader("Content-Length", String.valueOf(file.length()));

        try (InputStream in = Files.newInputStream(file.toPath());
                OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    private void cleanDirectory(File dir) {
        if (!dir.exists()) {
            return;
        }
        try {
            Files.walk(dir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {
        }
    }
}
