/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsOutputBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 一次只能是一条OCH
 */
@Slf4j
@Service
public class TunnelTakeOver {

    @Autowired
    private RegTunnelGenerator regTunnelGenerator;
    @Autowired
    private TunnelGenerator tunnelGenerator;

    public TakeoverTunnelsOutput doIt(TakeoverTunnelsInput input, TaskInfoMessage taskInfoMessage) throws CommonException {
        try {
            if (input.isIsReg()) {
                regTunnelGenerator.doIt(input, taskInfoMessage);
            } else {
                tunnelGenerator.doIt(input, taskInfoMessage);
            }
        } catch (Exception e) {
            log.error("Failed to takeover tunnels for input:{}",input, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,e.getMessage());
        }

        return new TakeoverTunnelsOutputBuilder()
                .setReturnCode(RpcResultType.Success).build();
    }

}




