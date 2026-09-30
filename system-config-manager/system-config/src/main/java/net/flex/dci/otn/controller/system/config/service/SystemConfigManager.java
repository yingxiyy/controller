package net.flex.dci.otn.controller.system.config.service;

import java.util.List;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerOutput;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerOutput;
import net.flex.dci.otn.controller.system.config.common.model.TimezoneInfo;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
public interface SystemConfigManager {


    NtpServerOutput getCurrentNtpConfig();

    TelemetryServerOutput getCurrentTelemetryServerOutput();

    NtpServerOutput configNtpServer(NtpServerConfig ntpServerConfig,String author);

    TelemetryServerOutput configTelemetryServer(TelemetryServerConfig telemetryServerConfig,String author);

    List<TimezoneInfo> fetchTimezone();
}
