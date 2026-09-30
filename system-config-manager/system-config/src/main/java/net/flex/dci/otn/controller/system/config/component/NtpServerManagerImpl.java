package net.flex.dci.otn.controller.system.config.component;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.NtpConfigDao;
import net.flex.dci.otc.mongo.mdoel.ntp.NtpConfig;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerConfig;
import net.flex.dci.otn.controller.system.config.common.utils.AsynchronousExecutor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ReassignNtpServerOutput;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class NtpServerManagerImpl extends AbstractSystemConfigManager implements NtpServerManager {

    private final NtpConfigDao ntpConfigDao;

    public NtpServerManagerImpl(NeManagerRpc neManagerRpc,NtpConfigDao ntpConfigDao) {
        super(neManagerRpc);
        this.ntpConfigDao = ntpConfigDao;
    }

    @Override
    public NtpConfig configNtpServer(NtpServerConfig ntpServerConfig,String author) {
        log.info("start to config the ntp server config");
        String ipv4NtpServer = ntpServerConfig.getIpv4Ntp();
        String ipv6NtpServer = ntpServerConfig.getIpv6Ntp();
        String timezone = ntpServerConfig.getTimezone();
        NtpConfig ntpConfig = ntpConfigDao.getNtpConfig();
        if(ipv4NtpServer!=null) {
            ntpConfig =ntpConfigDao.updateIpv4NtpAddress(ipv4NtpServer,author);
        }
        if(ipv6NtpServer!=null){
            ntpConfig = ntpConfigDao.updateIpv6NtpAddress(ipv6NtpServer,author);
        }
        if(StringUtils.hasText(timezone)) {
            ntpConfig = ntpConfigDao.updateTimezone(timezone,author);
        }
        //todo: reassign the ntp server address
        AsynchronousExecutor.execute(()->{
            log.info("async reassign ntp server and timezone to the current system managed ne");
            ReassignNtpServerOutput output = neManagerRpc.reAssignNtpServer();
            log.info("reassign ntp server finished result :{}",output);
        });
        return ntpConfig;
    }
}
