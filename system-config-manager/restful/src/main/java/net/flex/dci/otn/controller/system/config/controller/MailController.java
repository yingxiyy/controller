/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.controller;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.common.model.MailReceiver;
import net.flex.dci.otn.controller.system.config.common.model.Result;
import net.flex.dci.otn.controller.system.config.email.dto.MailSenderConfigData;
import net.flex.dci.otn.controller.system.config.email.service.MailConfigureService;
import net.flex.dci.otn.controller.system.config.email.service.MailImplService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@RequestMapping("alarm/mail")
@Slf4j
public class MailController {

    @Autowired
    private MailConfigureService mailConfigureService;

    @Autowired
    private MailImplService mailImplService;

    @GetMapping(path = "/getRecipients", produces = "application/json")
    public ResponseEntity<Result> getRecipients() {
        return new ResponseEntity<>(Result.ok(mailConfigureService.getRecipients()), HttpStatus.OK);

    }

    @PostMapping(path = "/updateRecipients", consumes = "application/json")
    public ResponseEntity<Result> updateRecipients(@RequestBody List<String> recipients) {
        mailConfigureService.updateRecipients(recipients);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @GetMapping(path = "/getConfigure", produces = "application/json")
    public ResponseEntity<Result> getConfigure() {
        return new ResponseEntity<>(Result.ok(mailConfigureService.getMailSenderConfigData()),
                HttpStatus.OK);
    }

    @RequestMapping(path = "/updateConfigure", consumes = "application/json", method = RequestMethod.POST)
    public ResponseEntity<Result> updateConfigure(
            @RequestBody MailSenderConfigData mailSenderConfigData)
            throws GeneralSecurityException {
        mailConfigureService.updateMailSenderConfigData(mailSenderConfigData);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @GetMapping(path = "/test")
    public ResponseEntity<Result> test() {
        log.info("Send test mail ");
        mailImplService.test();
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @PostMapping(path = "/check/test")
    public ResponseEntity<Result> sendCheckMail(@RequestBody MailReceiver mailReceiver) {
        log.info("start to send test mail to the mail Receiver,mail address is:{}", mailReceiver);
        mailImplService.sendCheckMail(mailReceiver);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @GetMapping(path = "/check")
    public ResponseEntity<Result> check() {
        log.info("Send test mail to sender for self checking.");
        mailImplService.check();
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

}
