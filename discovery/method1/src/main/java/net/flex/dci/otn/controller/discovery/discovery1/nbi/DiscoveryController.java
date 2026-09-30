/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.discovery1.nbi;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.discovery.discovery1.impl.Discovery;
import net.flex.dci.otn.controller.discovery.discovery1.impl.DiscoveryDiscard;
import net.flex.dci.otn.controller.webapp.Result;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RecoverDiscardInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RecoverInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RecoverOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DiscoveryController extends AbstractNBIController {

    private final Discovery discovery;

    private final DiscoveryDiscard discoveryDiscard;

    /**
     * 按照实际情况创建Tunnel/siteLink, 然后比对网络资源，一致的标识为impl
     *
     * @param json
     */
    @PostMapping(value = "/restconf/operations/tunnel:recover", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String discovery(@RequestBody String json, HttpServletRequest request) {
        RecoverInput input = formRpcInput(json, RecoverInput.class);
        log.info("recover tunnel {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.tunnel,
                TaskInfoMessage.ActionType.discovery,
                json);

        RecoverOutput output = discovery.setTaskInfo(taskInfoMessage).recoverTunnel(input);

        String result = formRpcOutput(output);
        result.replaceAll("\\\"", "\"");
        result.replaceAll("\\=", "=");
        return result;
    }

    /**
     * 数据库中的implement-state修改到allocate状态，不下发网元
     *
     * @param json
     */
    @PostMapping(value = "/restconf/operations/tunnel:recover-discard", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> discoveryDiscard(@RequestBody String json) {
        RecoverDiscardInput input = formRpcInput(json, RecoverDiscardInput.class);
        log.info("discard recovered tunnel {}", input);
        discoveryDiscard.discoveryDiscard(input);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

}
