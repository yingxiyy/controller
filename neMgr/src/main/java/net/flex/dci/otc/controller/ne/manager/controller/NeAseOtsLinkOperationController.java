package net.flex.dci.otc.controller.ne.manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * 2025/9/9
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/restconf/operations/")
public class NeAseOtsLinkOperationController {

    private final NeManager neManager;

    @PostMapping(value = "eml-manager:ne-operation-link", produces = "application/json;charset=UTF-8")
    public @ResponseBody String neOperationLink(@RequestBody String input) {
        log.info("ne operation Link:{}", input);
        return neManager.operationLink(input);
    }


    @PostMapping(value = "eml-manager:channel-ase-restore", produces = "application/json;charset=UTF-8")
    public @ResponseBody String channelAseRestore(@RequestBody String input) {
        log.info("channel ase restore:{}", input);
        return neManager.channelAseRestore(input);
    }
}
