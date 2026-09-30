package net.flex.dci.otn.controller.system.config.validator;

import net.flex.dci.otn.controller.system.config.common.dto.NtpServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerOutput;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
public interface SystemConfigValidator {

    void validateNtpServerConfig(NtpServerConfig ntpServerConfig);

    void validateTelemetryServerConfig(TelemetryServerConfig telemetryServerConfig);

}
