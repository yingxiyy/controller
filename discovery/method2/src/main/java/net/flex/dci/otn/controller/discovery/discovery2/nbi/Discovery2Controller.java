/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.discovery2.nbi;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.discovery.discovery2.impl.Discovery;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RecoverInput;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class Discovery2Controller extends AbstractNBIController {


    @PostMapping(value = "/restconf/operations/tunnel:recover", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public void discovery(@RequestBody String json) {
        RecoverInput input = formRpcInput(json, RecoverInput.class);
        log.info("recover nodeIds {}", input);

        new Discovery().start(input.getResourceId());
    }

}
