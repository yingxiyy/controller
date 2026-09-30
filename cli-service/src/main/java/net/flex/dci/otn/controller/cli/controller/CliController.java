package net.flex.dci.otn.controller.cli.controller;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import net.flex.dci.otn.controller.cli.dto.ConnectRequest;
import net.flex.dci.otn.controller.cli.dto.ConnectSessionInfo;
import net.flex.dci.otn.controller.cli.service.CliService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @version 1.0
 * @date 9/16/2025 4:57 PM
 */
@RestController
@RequestMapping("/cli")
@RequiredArgsConstructor
public class CliController {

    private final CliService cliService;

    @RequestMapping(value = "/connect", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> connect(@RequestBody ConnectRequest connectRequest)
            throws IOException {
        ConnectSessionInfo connectSessionInfo = cliService.connect(connectRequest);
        return ResponseEntity.ok(Result.ok(connectSessionInfo));
    }
}
