package net.flex.dci.otn.controller.schedule.component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.FtpServerDao;
import net.flex.dci.otn.controller.schedule.properties.BackupProperties;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ServerAttributes;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.springframework.stereotype.Component;

/**
 * 2026/5/31
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class FtpServerSelector {

    private final FtpServerDao ftpServerDao;

    private final BackupProperties backupProperties;

    public FtpServer selectFtpServer() {
        log.info("select a active ftp server");
        List<FtpServer> ftpServers = ftpServerDao.listFtpServers();
        if (ftpServers.isEmpty()) {
            log.warn("current ftp server is not set");
            return null;
        }
        Map<String, FtpServer> ftpServerNameMap = ftpServers.stream().collect(Collectors.toMap(
                ServerAttributes::getName, ftpServer -> ftpServer));
        String sftpServer = backupProperties.getSftpServer();
        if (!StringUtils.isEmpty(sftpServer)) {
            FtpServer selectFtpServer = ftpServerNameMap.getOrDefault(sftpServer, null);
            if (selectFtpServer != null) {
                return selectFtpServer;
            }
            log.warn("Configured FTP server '{}' not found in database, using default",
                    sftpServer);
        }
        FtpServer ftpServer = ftpServers.get(0);
        log.info("use ftp server:{}/{} to backup current controller db", ftpServer.getName(),
                ftpServer.getAddress());
        return ftpServer;
    }
}
