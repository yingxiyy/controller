package net.flex.dci.otn.controller.sftp.manager.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 6/6/2023 2:29 PM
 */
@RestController
@Slf4j

public class FtpServerController {

    @RequestMapping(value = "/restconf/config/ftp-server:**", method = {RequestMethod.GET,
            RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
    public ResponseEntity<?> executeFTPCmd() {
        return new ResponseEntity<>("ok", HttpStatus.OK);
    }


    @RequestMapping(value = "/restconf/config/ftp-server:ftp-servers/**", method = {RequestMethod.GET,
            RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
    public ResponseEntity<Object> executeConfigSFTPCmd() {
        return new ResponseEntity<>("this is topology request", HttpStatus.OK);
    }

}
