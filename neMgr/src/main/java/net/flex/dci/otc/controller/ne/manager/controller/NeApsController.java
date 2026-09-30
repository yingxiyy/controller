package net.flex.dci.otc.controller.ne.manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.service.NeApsManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 8/8/2025 2:32 PM
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/restconf/operations/")
public class NeApsController {

    private final NeApsManager neApsManager;

    @PostMapping(value = "eml-manager:clear-ne-aps-switch-logs")
    public @ResponseBody String clearNeApsSwitchLogs(@RequestBody String input) {
        log.info("clear ne aps switch logs input is :{}", input);
        return neApsManager.clearNeApsSwitchLogs(input);
    }

    @PostMapping(value = "eml-manager:manage-aps-switch")
    public @ResponseBody String manageApsSwitch(@RequestBody String input) {
        log.info("manage ne aps switch input is:{}", input);
        return neApsManager.manageApsSwitch(input);
    }
}
