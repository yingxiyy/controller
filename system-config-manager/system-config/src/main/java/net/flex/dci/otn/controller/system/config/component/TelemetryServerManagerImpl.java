package net.flex.dci.otn.controller.system.config.component;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.TelemetryServerConfigDao;
import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerDto;
import net.flex.dci.otn.controller.system.config.common.utils.AsynchronousExecutor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigureNorthboundTelemetryOutput;
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
public class TelemetryServerManagerImpl extends AbstractSystemConfigManager implements
        TelemetryServerManager {

    private final TelemetryServerConfigDao telemetryServerConfigDao;

    public TelemetryServerManagerImpl(
            NeManagerRpc neManagerRpc, TelemetryServerConfigDao telemetryServerConfigDao) {
        super(neManagerRpc);
        this.telemetryServerConfigDao = telemetryServerConfigDao;
    }

    @Override
    public TelemetryConfig configTelemetryServer(TelemetryServerConfig telemetryServerConfig,
            String author) {
        log.info("config telemetry server by author:{} config is:{}", author,
                telemetryServerConfig);
        TelemetryConfig telemetryConfig = telemetryServerConfigDao.getTelemetryConfig();
        TelemetryServerDto ipv4Server = telemetryServerConfig.getIpv4Server();
        TelemetryServerDto ipv6Server = telemetryServerConfig.getIpv6Server();
        boolean enable = telemetryServerConfig.isEnable();
        if (ipv4Server != null && StringUtils.hasText(ipv4Server.getAddress())) {
            telemetryConfig = telemetryServerConfigDao.updateIpv4Server(ipv4Server.getAddress(),
                    ipv4Server.getPort(), author);
        }
        if (ipv6Server != null && StringUtils.hasText(ipv6Server.getAddress())) {
            telemetryConfig = telemetryServerConfigDao.updateIpv6Server(ipv6Server.getAddress(),
                    ipv6Server.getPort(), author);
        }
        //todo enable or not enable the northbound telemetry set
        telemetryConfig.setEnable(enable);
        telemetryConfig = telemetryServerConfigDao.saveTelemetryConfig(telemetryConfig);
        //todo send asynchronous job to set telemetry
        AsynchronousExecutor.execute(() -> {
            log.info("start to configure northbound telemetry asynchronous");
            ConfigureNorthboundTelemetryOutput configureResult = neManagerRpc.configureNorthboundTelemetry();
            log.info("finish configure northbound telemetry ,the result is:{} message:{}",
                    configureResult.getReturnCode(), configureResult.getStatusMessage());
        });
        return telemetryConfig;
    }
}
