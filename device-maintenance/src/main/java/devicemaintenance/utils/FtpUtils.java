package devicemaintenance.utils;

import devicemaintenance.integration.NeMgrIntegrationService.SftpServerDetails;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.database.operate.input.SftpServerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.software.operate.input.SftpServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FileTransferProtocol;

@Slf4j
public class FtpUtils {

    public static org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.database.operate.input.SftpServer buildDatabaseSftpServer(
            SftpServerDetails sftpInfo, String fileName, String path) {
        return buildDatabaseSftpServer(sftpInfo, fileName, path, sftpInfo.getAddress());
    }

    public static org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.database.operate.input.SftpServer buildDatabaseSftpServer(
            SftpServerDetails sftpInfo, String fileName, String path, String sourceAddress) {
        return new SftpServerBuilder()
                .setAddress(sftpInfo.getAddress())
                .setPort(new PortNumber(sftpInfo.getPort()))
                .setUser(sftpInfo.getUser())
                .setPassword(sftpInfo.getPassword())
                .setUploadPath(path)
                .setDownloadPath(path)
                .setProtocol(FileTransferProtocol.SFTP)
                .setSourceAddress(sourceAddress)
                .build();
    }

    public static SftpServer buildSftpServer(SftpServerDetails sftpInfo, String softwarePath) {
        return buildSftpServer(sftpInfo, softwarePath, sftpInfo.getAddress());
    }

    public static SftpServer buildSftpServer(SftpServerDetails sftpInfo, String softwarePath,
            String sourceAddress) {
        return new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.software.operate.input.SftpServerBuilder()
                .setAddress(sftpInfo.getAddress())
                .setPort(new PortNumber(sftpInfo.getPort()))
                .setUser(sftpInfo.getUser())
                .setPassword(sftpInfo.getPassword())
                .setDownloadPath(softwarePath)
                .setUploadPath(softwarePath)
                .setProtocol(FileTransferProtocol.SFTP)
                .setSourceAddress(sourceAddress)
                .build();
    }
}
