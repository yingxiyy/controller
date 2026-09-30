package net.flex.dci.otn.controller.resource.statistic.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @version 1.0
 * @date 10/31/2025 4:06 PM
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/resource/inventory/tunnel")
public class TunnelInventoryController {

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> retrieveTunnelInventoryPaged(
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit) {
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

}
