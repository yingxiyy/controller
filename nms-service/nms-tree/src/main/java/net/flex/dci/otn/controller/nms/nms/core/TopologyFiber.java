/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.handler.ResourceHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFiberAffectionInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFiberAffectionOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyFiber extends BaseNms {

    private static final String GET_FIBER_AFFECTION = "nms:get-fiber-affection";


    @Autowired
    private ResourceHandler resourceHandler;

    public TopologyFiber(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        if (cmd.equals(GET_FIBER_AFFECTION)) {
            returnValue = executeGetFiberAffection(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException("Unsupported operations for fiber affection");
        }
        return returnValue;
    }

    private String executeGetFiberAffection(String cmd, String requestBody) throws CommonException {
        try {
            GetFiberAffectionInput input = parseInput(cmd, requestBody,
                    GetFiberAffectionInput.class);
            GetFiberAffectionOutput getFiberAffectionOutput = this.resourceHandler
                    .getFiberAffection(input);
            return serializeDataObject(cmd, getFiberAffectionOutput);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }
}
