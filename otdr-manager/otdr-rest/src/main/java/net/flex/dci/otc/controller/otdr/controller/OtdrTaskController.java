package net.flex.dci.otc.controller.otdr.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.otdr.model.otsLink.GetOmsLinkOtdrLatestResultOutputDto;
import net.flex.dci.otc.controller.otdr.service.OtdrResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2025/8/9
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@Slf4j
@RequiredArgsConstructor
public class OtdrTaskController {

    private final OtdrResultService otdrResultService;

    @RequestMapping(value = "/restconf/operations/otdr-task:get-oms-link-otdr-latest-result", method = RequestMethod.POST)
    public ResponseEntity<?> getOmsLinkOtdrLatestResult(@RequestBody String input) {
        log.info("get oms link otdr latest result,the input is:{}", input);
        GetOmsLinkOtdrLatestResultOutputDto result = otdrResultService.getOMSLinkOtdrLatestResultOutput(
                input);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
