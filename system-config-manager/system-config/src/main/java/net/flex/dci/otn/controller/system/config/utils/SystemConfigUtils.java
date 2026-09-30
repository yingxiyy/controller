package net.flex.dci.otn.controller.system.config.utils;

import net.flex.dci.otc.mongo.mdoel.ntp.NtpConfig;
import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryConfig;
import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryServer;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerOutput;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerDto;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerOutput;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
public class SystemConfigUtils {

    public static NtpServerOutput convert2NtpServerOutput(NtpConfig ntpConfig) {
        NtpServerOutput ntpServerOutput = NtpServerOutput.builder().ipv4Address(
                        ntpConfig.getIpv4NtpServer() == null ? null
                                : ntpConfig.getIpv4NtpServer().getAddress())
                .ipv6Address(ntpConfig.getIpv6NtpServer() == null ? null
                        : ntpConfig.getIpv6NtpServer().getAddress())
                .updatedAt(ntpConfig.getUpdatedAt())
                .updatedBy(ntpConfig.getUpdatedBy())
                .timezone(ntpConfig.getTimezone())
                .build();
        return ntpServerOutput;
    }

    public static TelemetryServerOutput convert2TelemetryServerOutput(
            TelemetryConfig serverConfig) {
        TelemetryServerOutput serverOutput = TelemetryServerOutput.builder()
                .ipv6Server(convert2TelemetryServer(serverConfig.getIpv6Server()))
                .ipv4Server(convert2TelemetryServer(serverConfig.getIpv4Server()))
                .enable(serverConfig.isEnable())
                .updatedAt(serverConfig.getUpdatedAt())
                .updatedBy(serverConfig.getUpdatedBy())
                .build();
        return serverOutput;
    }

    private static TelemetryServerDto convert2TelemetryServer(TelemetryServer server) {
        if (server == null) {
            return null;
        }
        return TelemetryServerDto.builder().address(server.getServerAddress())
                .port(server.getPort()).build();
    }
}
