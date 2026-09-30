/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.rest.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 * @date 2021/10/14 9:59
 */
@RestController
@Slf4j
@RequestMapping("/")
public class IndexController {

    @RequestMapping(method = RequestMethod.GET)
    public Mono<String> index() {
        return Mono.just(desc());
    }

    private String desc() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("<div style='color: blue'>sdn gateway has been started!</div>");
        return sb.toString();
    }
}
