package net.flex.dci.otn.controller.system.config.service;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.NtpConfigDao;
import net.flex.dci.otc.mongo.dao.TelemetryServerConfigDao;
import net.flex.dci.otc.mongo.mdoel.ntp.NtpConfig;
import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryConfig;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerOutput;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerOutput;
import net.flex.dci.otn.controller.system.config.common.model.TimezoneInfo;
import net.flex.dci.otn.controller.system.config.common.properties.TimezoneProperties;
import net.flex.dci.otn.controller.system.config.component.NtpServerManager;
import net.flex.dci.otn.controller.system.config.component.TelemetryServerManager;
import net.flex.dci.otn.controller.system.config.utils.SystemConfigUtils;
import net.flex.dci.otn.controller.system.config.validator.SystemConfigValidator;
import org.springframework.stereotype.Service;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConfigManagerImpl implements SystemConfigManager{

    private final TelemetryServerConfigDao telemetryServerConfigDao;

    private final NtpConfigDao ntpConfigDao;

    private final SystemConfigValidator systemConfigValidator;

    private final NtpServerManager ntpServerManager;

    private final TelemetryServerManager telemetryServerManager;

    private final TimezoneProperties timezoneProperties;


    @Override
    public NtpServerOutput getCurrentNtpConfig() {
        log.debug("get current system ntp config");
        NtpConfig ntpConfig = ntpConfigDao.getNtpConfig();
        NtpServerOutput ntpServerOutput= SystemConfigUtils.convert2NtpServerOutput(ntpConfig);
        return ntpServerOutput;
    }

    @Override
    public TelemetryServerOutput getCurrentTelemetryServerOutput() {
        log.debug("get current default telemetry server configuration");
        TelemetryConfig serverConfig = telemetryServerConfigDao.getTelemetryConfig();
        TelemetryServerOutput configOutput = SystemConfigUtils.convert2TelemetryServerOutput(serverConfig);
        return configOutput;
    }

    @Override
    public NtpServerOutput configNtpServer(NtpServerConfig ntpServerConfig,String author) {
        log.info("config ntp server configuration,author:{}",author);
        systemConfigValidator.validateNtpServerConfig(ntpServerConfig);
        NtpConfig ntpConfig = ntpServerManager.configNtpServer(ntpServerConfig,author);
        NtpServerOutput ntpServerOutput = SystemConfigUtils.convert2NtpServerOutput(ntpConfig);
        return ntpServerOutput;
    }

    @Override
    public TelemetryServerOutput configTelemetryServer(
            TelemetryServerConfig telemetryServerConfig,String author) {
        log.info("config telemetry sever configuration,author:{}",author);
        systemConfigValidator.validateTelemetryServerConfig(telemetryServerConfig);
        TelemetryConfig telemetryConfig = telemetryServerManager.configTelemetryServer(telemetryServerConfig,author);
        TelemetryServerOutput telemetryServerOutput = SystemConfigUtils.convert2TelemetryServerOutput(telemetryConfig);
        return telemetryServerOutput;
    }

    @Override
    public List<TimezoneInfo> fetchTimezone() {
        List<TimezoneInfo> timezoneInfos = timezoneProperties.getTimezones();
        final String utcCode = "ZZ"; // “ZZ”是IANA中用于表示“国际”或“未指定地区”的伪国家码
        final String utcCoordinates = "+0000+00000"; // 代表格林威治/本初子午线的坐标
        final String utcTz = "UTC"; // 时区标识符
        boolean utcAlreadyExists = timezoneInfos.stream()
                .anyMatch(info -> utcTz.equals(info.getTz()) || "Etc/UTC".equals(info.getTz()));

        // 4. 如果不存在，则构造并添加UTC条目
        if (!utcAlreadyExists) {
            TimezoneInfo utcInfo = TimezoneInfo.builder()
                    .code(utcCode)
                    .coordinates(utcCoordinates)
                    .tz(utcTz)
                    .build();
            // 建议将UTC放在列表最前面，作为基准时区
            timezoneInfos.add(0, utcInfo);
        }
        return timezoneInfos;
    }
}
