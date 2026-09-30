/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.handler.RackHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.PagedList;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackPagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * get-rack get-rack-paged
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyRack extends BaseNms {

    private final String GET_RACK_CMD = "nms:get-rack";
    private final String GET_RACK_PAGED_CMD = "nms:get-rack-paged";

    @Autowired
    private RackHandler rackHandler;

    public TopologyRack(NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {

        String returnValue = null;
        if (cmd.equals(GET_RACK_CMD)) {
            returnValue = executeGetRack(cmd, requestBody);
        } else if (cmd.equals(GET_RACK_PAGED_CMD)) {
            returnValue = executeGetRackPaged(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException("unsupported nms operation for rack");
        }
        return returnValue;
    }

    private String executeGetRackPaged(String cmd, String requestBody) throws CommonException {

        log.info("start to get rack paged the request body is {}", requestBody);
        try {
            GetRackPagedInput input = parseInput(cmd, requestBody, GetRackPagedInput.class);
            PagedList pagedList = this.rackHandler.getRackPaged(input);
            Integer startPos = input.getStartPos() == null ? 0 : input.getStartPos();
            GetRackPagedOutputBuilder outputBuilder = new GetRackPagedOutputBuilder();
            outputBuilder.setNode((List<Node>) pagedList.getPage(startPos, input.getHowMany()));
            outputBuilder.setStartPos(startPos);
            outputBuilder.setTotalRecords(pagedList.getRecordsNumber());
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get rack,the reason is :" + ex.getMessage());
        }
    }

    private String executeGetRack(String cmd, String requestBody) throws CommonException {
        log.info("start to get rack the request body is {}", requestBody);
        try {
            GetRackInput input = parseInput(cmd, requestBody, GetRackInput.class);
            List<Node> racks = rackHandler.getRacks(input);
            GetRackOutputBuilder getRackOutputBuilder = new GetRackOutputBuilder();
            getRackOutputBuilder.setNode(racks);
            return serializeDataObject(cmd, getRackOutputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get rack,the reason is :" + ex.getMessage());
        }
    }
}
