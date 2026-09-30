/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@EntityScan
@EnableDciClient
@SpringBootApplication
@ComponentScan(basePackages = {
    "net.flex.dci.otn.controller.allocate.link.site.removenode",
    "net.flex.dci.otn.controller.allocate.common.namingrule",
    "net.flex.dci.otn.controller.implement"
})
public class ImplementApp {

    public static void main(String[] args) {
        SpringApplication.run(ImplementApp.class, args);
    }
}