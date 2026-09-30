package net.flex.dci.otn.controller.nms.controller;

import static net.flex.dci.otn.controller.nms.utils.NetConfWildCard.NMS_RPC_REQUEST_WILDCARD;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/4/14 10:31
 */
@RestController
@Slf4j
@RequestMapping(NMS_RPC_REQUEST_WILDCARD)
public class NmsController {

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> executeNMSCmd() {
        return new ResponseEntity<>("ok", HttpStatus.OK);
    }

}
