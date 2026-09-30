package net.flex.dci.otc.controller.otdr.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.otdr.model.LatestOtdrResOutput;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResultOutput;
import net.flex.dci.otc.controller.otdr.model.graphics.ShowOtdrResOutput;
import net.flex.dci.otc.controller.otdr.service.OtdrResultService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/7/18 14:41
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class OtdrResultController {

    private final OtdrResultService otdrResultService;

    @RequestMapping(value = "/restconf/operations/otdr:get-otdr-result", method = RequestMethod.POST)
    public ResponseEntity<?> getNodeOtdrResult(@RequestBody String requestBody) {
        log.info("start to get otdr result ");
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


    @RequestMapping(value = "/restconf/operations/otdr:get-otdr-results", method = RequestMethod.POST)
    public ResponseEntity<?> getLinkOtdrResults(@RequestBody String requestBody) {
        log.info("start to get otdr results");
//        Object result = otdrResultService.getOTDRResults(requestBody);
        OtdrBrieflyResultOutput output = otdrResultService.getOTDRBrieflyResults(requestBody);
        return new ResponseEntity<>(output, HttpStatus.OK);
    }


    @RequestMapping(value = "/restconf/operations/otdr:get-otdr-detail", method = RequestMethod.POST)
    public ResponseEntity<?> getOtdrDetails(@RequestBody String requestBody) {
        Object result = otdrResultService.getOTDRDetail(requestBody);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/restconf/operations/otdr:get-otdr-res", method = RequestMethod.POST)
    public ResponseEntity<?> getTpOtdrResults(@RequestBody String requestBody) {
        LatestOtdrResOutput result = otdrResultService.getOtdrLatestResult(requestBody);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/restconf/operations/otdr:show-otdr-graphics", method = RequestMethod.POST)
    public ResponseEntity<?> compareOTDRResult(@RequestBody String requestBody) {
        ShowOtdrResOutput result = otdrResultService.showOtdrResultGraphics(requestBody);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
