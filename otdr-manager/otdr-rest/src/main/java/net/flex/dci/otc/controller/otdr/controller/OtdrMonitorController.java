package net.flex.dci.otc.controller.otdr.controller;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.controller.otdr.service.OtdrService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/7/18 14:21
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class OtdrMonitorController {

    private final OtdrService otdrService;

    @RequestMapping(value = "/restconf/operations/otdr:get-otdr-monitor", method = RequestMethod.POST)
    public ResponseEntity<?> getOtdrMonitor(@RequestBody String requestBody) {
        log.info("start to get the otdr monitor status");

        Object result = otdrService.getOtdrMonitorStatus(requestBody);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/restconf/operations/otdr:start-otdr", method = RequestMethod.POST)
    public ResponseEntity<?> startOtdr(@RequestBody String requestBody,
            HttpServletRequest request) {
        log.info("start to start otdr ");
        String user = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        Object result = otdrService.startOtdrOutput(requestBody, user);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/restconf/operations/otdr:set-otdr-base", method = RequestMethod.POST)
    public ResponseEntity<?> setOtdrBase(@RequestBody String requestBody) {
        otdrService.setOtdrBaseBenchmark(requestBody);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }
}
