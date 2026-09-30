package net.flex.dci.otn.controller.system.config.controller;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerOutput;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerConfig;
import net.flex.dci.otn.controller.system.config.common.dto.TelemetryServerOutput;
import net.flex.dci.otn.controller.system.config.common.model.TimezoneInfo;
import net.flex.dci.otn.controller.system.config.service.SystemConfigManager;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@Slf4j
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigManager systemConfigManager;

    @RequestMapping(value = "/ntp", method = RequestMethod.POST)
    public ResponseEntity<?> configNtp(@RequestBody NtpServerConfig ntpServerConfig,
            HttpServletRequest request) {
        log.info("config ntp system configuration");
        String author = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        NtpServerOutput ntpServerOutput = systemConfigManager.configNtpServer(ntpServerConfig,
                author);
        return new ResponseEntity<>(Result.ok(ntpServerOutput), HttpStatus.OK);
    }

    @RequestMapping(value = "/northbound-telemetry", method = RequestMethod.POST)
    public ResponseEntity<?> configTelemetry(
            @RequestBody TelemetryServerConfig telemetryServerConfig, HttpServletRequest request) {
        log.info("config the telemetry server");
        String author = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        TelemetryServerOutput telemetryServerOutput = systemConfigManager.configTelemetryServer(
                telemetryServerConfig, author);
        return new ResponseEntity<>(Result.ok(telemetryServerOutput), HttpStatus.OK);
    }

    @RequestMapping(value = "/ntp", method = RequestMethod.GET)
    public ResponseEntity<?> getConfigNtp() {
        log.info("get current system ntp configuration");
        NtpServerOutput ntpServerOutput = systemConfigManager.getCurrentNtpConfig();
        return new ResponseEntity<>(Result.ok(ntpServerOutput), HttpStatus.OK);
    }

    @RequestMapping(value = "/northbound-telemetry", method = RequestMethod.GET)
    public ResponseEntity<?> getConfigTelemetry() {
        log.info("get current system telemetry server configuration");
        TelemetryServerOutput telemetryServerOutput = systemConfigManager.getCurrentTelemetryServerOutput();
        return new ResponseEntity<>(Result.ok(telemetryServerOutput), HttpStatus.OK);
    }

    @RequestMapping(value = "/timezone", method = RequestMethod.GET)
    public ResponseEntity<?> getTimezones() {
        log.info("get IANA Timezone list");
        List<TimezoneInfo> timezoneInfos = systemConfigManager.fetchTimezone();
        return new ResponseEntity<>(Result.ok(timezoneInfos), HttpStatus.OK);
    }
}
