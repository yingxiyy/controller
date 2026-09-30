package net.flex.dci.otc.controller.ne.manager.controller;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.service.impl.NeManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

/**
 * @version 1.0
 * @date 7/24/2023 4:04 PM
 */
@Slf4j
@RestController
@RequestMapping("/restconf/operations/")
public class NeCreateController implements Serializable {
    @Autowired
    private NeManagerImpl neManager;

    @PostMapping(value = {
            "otn-phy-topology:create-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody String createNe(@RequestBody String input) {
        log.info("register ne the ne info is {}", input);
        return neManager.createNe(input);
    }
}
