package net.flex.dci.otn.controller.sftp.manager.message;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.ChangeType;
import net.flex.dci.otc.common.enums.ElementType;
import net.flex.dci.otc.common.model.element.ElementChangeNotification;
import net.flex.dci.otn.controller.sftp.manager.dto.FtpServerDto;
import net.flex.dci.otn.controller.tools.kafka.service.ElementChangeMessager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.springframework.stereotype.Component;

/**
 *
 * 2025/9/22
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class FtpMessageImpl implements FtpMessage {

    @Override
    public void notifyFtpServerCreate(FtpServer ftpServer) {
        log.debug("notify ftp server:{} created", ftpServer);
        FtpServerDto ftpServerDto = convert2FtpServerDto(ftpServer);
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(ChangeType.CREATE)
                .content(ftpServerDto)
                .elementType(ElementType.FTP_SERVER)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }


    @Override
    public void notifyFtpServerDelete(String ftpServerId) {
        log.debug("notify ftp server:{} delete ", ftpServerId);
        FtpServerDto ftpServerDto = FtpServerDto.builder().id(ftpServerId).build();
        ElementChangeNotification deleteElementChangeNotification = ElementChangeNotification.builder()
                .changeType(ChangeType.DELETE).content(ftpServerDto).elementType(
                        ElementType.FTP_SERVER)
                .build();
        ElementChangeMessager.publishChangeMessage(deleteElementChangeNotification);
    }

    @Override
    public void notifyFtpServerDelete(FtpServer ftpServer) {
        log.debug("notify ftp server:{} DELETED", ftpServer);
        FtpServerDto ftpServerDto = convert2FtpServerDto(ftpServer);
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .changeType(ChangeType.DELETE)
                .content(ftpServerDto)
                .elementType(ElementType.FTP_SERVER)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    @Override
    public void notifyFtpServerUpdate(FtpServer ftpServer) {
        log.debug("notify ftp server:{} update", ftpServer);
        FtpServerDto ftpServerDto = convert2FtpServerDto(ftpServer);
        ElementChangeNotification deleteElementChangeNotification = ElementChangeNotification.builder()
                .changeType(ChangeType.UPDATE).content(ftpServerDto).elementType(
                        ElementType.FTP_SERVER)
                .build();
        ElementChangeMessager.publishChangeMessage(deleteElementChangeNotification);
    }


    private FtpServerDto convert2FtpServerDto(FtpServer ftpServer) {
        FtpServerDto ftpServerDto = FtpServerDto.builder().id(ftpServer.getId())
                .name(ftpServer.getName())
                .address(ftpServer.getAddress())
                .port(ftpServer.getPort().getValue())
                .type(ftpServer.getType().name())
                .user(ftpServer.getUser())
                .password(ftpServer.getPassword())
                .build();
        return ftpServerDto;
    }
}
