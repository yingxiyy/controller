package net.flex.dci.otn.controller.implement.nbi;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.implement.service.ApsSwitchService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2025/8/12
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@RequiredArgsConstructor
@Slf4j
public class ApsController {

    private final ApsSwitchService apsSwitchService;

    @RequestMapping(value = "/restconf/operations/otn-phy-topology:batch-aps-switch", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public @ResponseBody String batchApsSwitch(@RequestBody String input,
            HttpServletRequest request) {
        log.info("batch aps switch the input is:{}", input);
        String result = apsSwitchService.batchApsSwitch(input, request);
        return result;
    }

    @RequestMapping(value = "/restconf/operations/otn-phy-topology:aps-switch-control", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public @ResponseBody String apsSwitchControl(@RequestBody String input,
            HttpServletRequest request) {
        log.info("aps switch control is :{}", input);
        String output = apsSwitchService.apsSwitchControl(input, request);
        return output;
    }

    @RequestMapping(value = "/restconf/operations/otn-phy-topology:restore-aps-path", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public @ResponseBody String restoreApsPath(@RequestBody String input,
            HttpServletRequest request) {
        String output = apsSwitchService.restoreApsPath(input, request);
        return output;
    }
}
