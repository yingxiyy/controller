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
import net.flex.dci.otn.controller.nms.nms.handler.NodeHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateNodeLocationInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateNodeLocationOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyNe extends BaseNms {

    private static final String GET_NE_UNIT_LIST = "nms:get-ne-unit-list";

    private static final String UPDATE_NODE_LOCATION = "nms:update-node-location";


    @Autowired
    private NodeHandler nodeHandler;

    public TopologyNe(NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        if (cmd.equals(GET_NE_UNIT_LIST)) {
            returnValue = getNeUnitList(cmd, requestBody);
        } else if (cmd.equals(UPDATE_NODE_LOCATION)) {
            returnValue = updateNodeLocation(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException("unsupported nms operations for node or ne");
        }
        return returnValue;
    }

    /**
     * update node location
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String updateNodeLocation(String cmd, String requestBody) throws CommonException {
        log.info("update node location");
        try {
            UpdateNodeLocationInput input = parseInput(cmd, requestBody,
                    UpdateNodeLocationInput.class);
            UpdateNodeLocationOutput output = this.nodeHandler.updateNodeLocation(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to update node location,the reason is " + ex.getMessage());
        }
    }

    /**
     * get ne unit list
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getNeUnitList(String cmd, String requestBody) throws CommonException {
//        GetNeUnitListInput input = parseInput(cmd, requestBody);
//        GetNeUnitListOutputBuilder outputBuilder = new GetNeUnitListOutputBuilder();
//        outputBuilder.setUnit(neHandler.getNeUnitList(input));
//        return serializeDataObject(cmd, outputBuilder.build());
        return null;
    }
}
