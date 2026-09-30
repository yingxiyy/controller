package net.flex.dci.otc.controller.ne.manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.service.NeConfigurationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@Slf4j
@RequestMapping(value = "/restconf/operations/")
@RequiredArgsConstructor
public class NeConfigurationController {

    private final NeConfigurationManager neConfigurationManager;

    @PostMapping(value = "eml-manager:reassign-ntp-server", produces = "application/json;charset=UTF-8")
    public @ResponseBody String reassignNtpServer() {
        log.info("start to reassign ntp server to current system ne");
        String response = neConfigurationManager.reassignNtpServer();
        return response;
    }


    @PostMapping(value = "eml-manager:configure-northbound-telemetry", produces = "application/json;charset=UTF-8")
    public @ResponseBody String configureNorthboundTelemetry() {
        log.info("start to reassign ntp server to current system ne");
        String response = neConfigurationManager.configureNorthboundTelemetry();
        return response;
    }

}
