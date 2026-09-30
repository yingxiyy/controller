package net.flex.dci.otn.controller.system.config.validator;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerDto;
import net.flex.dci.otn.controller.system.config.common.model.TimezoneInfo;
import net.flex.dci.otn.controller.system.config.common.properties.TimezoneProperties;
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
@RequiredArgsConstructor
public class SystemConfigValidatorImpl implements SystemConfigValidator {

    private final TimezoneProperties timezoneProperties;


    @Override
    public void validateNtpServerConfig(NtpServerConfig ntpServerConfig) {
        String ipv4Address = ntpServerConfig.getIpv4Ntp();
        String ipv6Address = ntpServerConfig.getIpv6Ntp();
        String timezone = ntpServerConfig.getTimezone();
        if (!StringUtils.hasText(ipv6Address) && !StringUtils.hasText(ipv4Address)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ntp server like ipv4 and ipv6 address should not be null");
        }
        if (StringUtils.hasText(ipv4Address)) {
            checkIpv4(ipv4Address);
        }
        if (StringUtils.hasText(ipv6Address)) {
            checkIpv6(ipv6Address);
        }
        if (StringUtils.hasText(timezone)) {
            checkTimezone(timezone);
        }

    }

    /**
     * check time zone
     *
     * @param timezone
     */
    private void checkTimezone(String timezone) {
        log.debug("check the timezone:{}", timezone);
        List<TimezoneInfo> timezoneInfos = timezoneProperties.getTimezones();
        List<String> ts = timezoneInfos.stream().map(TimezoneInfo::getTz).collect(
                Collectors.toList());

        if (!timezone.equals("UTC") && !ts.contains(timezone)) {
            log.error("timezone is not IANA timezone");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Timezone :" + timezoneInfos + " is not IANA timezone");
        }
    }


    @Override
    public void validateTelemetryServerConfig(TelemetryServerConfig telemetryServerConfig) {
        TelemetryServerDto ipv4Server = telemetryServerConfig.getIpv4Server();
        TelemetryServerDto ipv6Server = telemetryServerConfig.getIpv6Server();
        if (ipv6Server == null && ipv4Server == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "config telemetry server ipv4 or ipv6 should not be null");
        }
        if (ipv4Server != null) {
            if (!StringUtils.hasText(ipv4Server.getAddress())) {
                log.debug("current ipv4 address telemetry not set,do nothing");
                return;
            }
            if (ipv4Server.getPort() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "config telemetry ipv4 server  port should not null");

            }
            if (!isValidateIpv4(ipv4Server.getAddress())) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the ipv4 telemetry server address is not in IPV4 format");
            }
        }

        if (ipv6Server != null) {
            if (!StringUtils.hasText(ipv6Server.getAddress())) {
                log.debug("current ipv6 address telemetry not set,do nothing");
                return;
            }
            if (ipv6Server.getPort() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "config telemetry ipv6 server  port should not null");

            }
            if (isValidateIpv6(ipv6Server.getAddress())) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the ipv6 telemetry server address is not in IPV4 format");
            }
        }
    }

    private void checkIpv6(String ipv6Address) {
        log.debug("check ipv6:{}", ipv6Address);
        if (isValidateIpv6(ipv6Address)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ipv6 address is not in IPV6 format");
        }
    }

    private boolean isValidateIpv6(String ipv6Address) {
        if (ipv6Address.contains("%")) {
            ipv6Address = ipv6Address.substring(0, ipv6Address.indexOf("%"));
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(ipv6Address);

            String hostAddress = inetAddress.getHostAddress();
            return (!(inetAddress instanceof Inet6Address) ||
                    !hostAddress.equals(ipv6Address)) && !hostAddress.contains(":");
        } catch (UnknownHostException e) {
            return true;
        }
    }

    private boolean isValidateIpv4(String ip) {
        try {
            InetAddress inet = InetAddress.getByName(ip);
            return inet.getHostAddress().equals(ip) && inet instanceof java.net.Inet4Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }


    private void checkIpv4(String ipv4Address) {
        log.debug("check ipv4 :{}", ipv4Address);
        if (!isValidateIpv4(ipv4Address)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ipv4 address is not in IPV4 format");
        }
    }
}
