package net.flex.dci.otc.controller.ne.manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.service.NeSupervisionManager;
import org.springframework.web.bind.annotation.*;

/**
 * @version 1.0
 * @date 7/30/2023 4:24 PM
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/restconf/operations/")
public class NeSupervisionController {

    private final NeSupervisionManager neSupervisionManager;

    @PostMapping(value = {
            "eml-manager:start-supervise-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody String startSuperviseNe(@RequestBody String input) {
        log.info("start supervise ne the input is {}", input);
        return neSupervisionManager.superviseNe(input);
    }

    @PostMapping(value = {
            "eml-manager:stop-supervise-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody String stopSuperviseNe(@RequestBody String input) {
        log.info("stop supervise ne the input is {}", input);
        return neSupervisionManager.stopSuperviseNe(input);
    }
}
