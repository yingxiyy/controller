package net.flex.dci.otc.controller.ne.manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.service.NeNetConfCmdExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * 2025/12/20
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@Slf4j
@RequestMapping("/restconf/operations/")
@RequiredArgsConstructor
public class NeNetConfCmdController {

    private final NeNetConfCmdExecutor neNetConfCmdExecutor;

    @PostMapping(value = "eml-manager:execute-netconf-command", produces = "application/json;charset=UTF-8")
    public @ResponseBody String executeNetConfCmd(@RequestBody String input) {
        log.info("execute the NetConf Command");
        String response = neNetConfCmdExecutor.executeNetConfCmd(input);
        return response;
    }
}
