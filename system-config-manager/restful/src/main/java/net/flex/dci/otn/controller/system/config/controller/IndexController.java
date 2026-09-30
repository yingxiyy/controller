/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.controller;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.common.model.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2021/12/10 10:01
 */
@RestController
@Slf4j
@RequestMapping(value = "/")
public class IndexController {


    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> index() {
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


}
