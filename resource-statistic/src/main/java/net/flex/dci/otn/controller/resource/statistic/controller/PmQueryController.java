package net.flex.dci.otn.controller.resource.statistic.controller;

/**
 * 2026/9/13
 *
 * @author musa
 * @version 1.0
 **/

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/resource/pm/query")
public class PmQueryController {

//    private final PmMapper pmMapper;

    @RequestMapping(value = "/{ipAddress}", method = RequestMethod.GET)
    public ResponseEntity<?> getNeInventoryStatisticPaged(
            @PathVariable("ipAddress") String ipaddress) {
        log.info("get  ne latest pm ip address the:{}", ipaddress);
//        List<ChPmRow> chPmRows = pmMapper.latest(ChPmTable.CU, ipaddress, 0, 20);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }
}
