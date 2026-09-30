/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import net.flex.dci.otn.topology.cache.EnableDciCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @version 1.0
 */
@EnableDciClient
@EnableDciCache
@SpringBootApplication
public class ApsSwitchLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsSwitchLogApplication.class, args);
    }

}