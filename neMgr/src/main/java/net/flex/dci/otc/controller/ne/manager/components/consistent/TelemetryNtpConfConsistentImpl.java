package net.flex.dci.otc.controller.ne.manager.components.consistent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.IpAddressType;
import net.flex.dci.otc.controller.ne.manager.properties.TelemetryManagerConfigProperties;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.NtpConfigDao;
import net.flex.dci.otc.mongo.mdoel.ntp.NtpConfig;
import org.springframework.stereotype.Component;

/**
 * 2026/1/6
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class TelemetryNtpConfConsistentImpl implements TelemetryNtpConfConsistent {

    private final NtpConfigDao ntpConfigDao;

    private final TelemetryManagerConfigProperties managerConfigProperties;

    @Override
    public void ensureConfigConsistent() {
        log.debug("ensure config consistent to the telemetry and ntp configuration file");
        NtpConfig ntpConfig = ntpConfigDao.getNtpConfig();

        boolean ipv4Valid = ntpConfig.getIpv4NtpServer() != null
                && ntpConfig.getIpv4NtpServer().isValid();

        boolean ipv6Valid = ntpConfig.getIpv6NtpServer() != null
                && ntpConfig.getIpv6NtpServer().isValid();
        if (!ipv4Valid && !ipv6Valid) {
            loadNtpConfiguration2Db();
        }
    }

    /**
     * load ntp configuration to conf db only load the private ntp and timezone
     */
    private void loadNtpConfiguration2Db() {
        log.debug("load ntp configuration ip and timezone to the configuration database");
        String ntpAddress = managerConfigProperties.privateNtpAddress();
        IpAddressType ipAddressType = NeManagerUtils.getIpAddressType(ntpAddress);
        String timezone = managerConfigProperties.timezone();
        if (ipAddressType == IpAddressType.IPV4) {
            ntpConfigDao.updateIpv4NtpAddress(ntpAddress, null);
        } else if (ipAddressType == IpAddressType.IPV6) {
            ntpConfigDao.updateIpv6NtpAddress(ntpAddress, null);
        }
        ntpConfigDao.updateTimezone(timezone, null);
    }
}
