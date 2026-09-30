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
import net.flex.dci.otn.controller.nms.nms.handler.ResourceHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.PagedList;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetBoardLldpInfoInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetBoardLldpInfoOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEquipmentInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEquipmentOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEquipmentPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackPagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.board.lldp.info.output.LldpInfos;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.springframework.stereotype.Component;

/**
 * get-equipment get-equipment-paged
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyEquipment extends BaseNms {

    private static final String GET_EQUIPMENT = "nms:get-equipment";
    private static final String GET_EQUIPMENT_PAGED = "nms:get-equipment-paged";
    private static final String GET_BORD_LLDP_INFO = "nms:get-board-lldp-info";
    private final ResourceHandler resourceHandler;

    public TopologyEquipment(NetconfTopology netconfTopology) {
        super(netconfTopology);
        this.resourceHandler = new ResourceHandler(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        if (cmd.equals(GET_EQUIPMENT)) {
            returnValue = getEquipment(cmd, requestBody);
        } else if (cmd.equals(GET_EQUIPMENT_PAGED)) {
            returnValue = getEquipmentsPaged(cmd, requestBody);
        } else if (cmd.equals(GET_BORD_LLDP_INFO)) {
            returnValue = getBoardLLDPInfo(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException(
                    "unsupported cmd for equipment nms operations ");
        }
        return returnValue;
    }

    /**
     * get board lldp info
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getBoardLLDPInfo(String cmd, String requestBody) {
        log.info("execute get board LLDP info requestBody {}", requestBody);
        try {
            GetBoardLldpInfoInput input = parseInput(cmd, requestBody, GetBoardLldpInfoInput.class);
            List<LldpInfos> lldpInfos = resourceHandler.getBoardLldpInfos(input);
            GetBoardLldpInfoOutputBuilder outputBuilder = new GetBoardLldpInfoOutputBuilder();
            outputBuilder.setLldpInfos(lldpInfos);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get board LLDP infos :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get board lldp infos the reason is:" + ex.getMessage());
        }
    }

    /**
     * get-equipment request
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getEquipment(String cmd, String requestBody) throws CommonException {
        log.info("execute equipment operations request");
        try {
            GetEquipmentInput input = parseInput(cmd, requestBody, GetEquipmentInput.class);
            List<Node> nodes = resourceHandler.getEquipment(input);
            GetEquipmentOutputBuilder outputBuilder = new GetEquipmentOutputBuilder();
            outputBuilder.setNode(nodes);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get equipment the reason is :" + ex.getMessage());
        }
    }

    private String getEquipmentsPaged(String cmd, String requestBody) throws CommonException {
        try {
            log.info("get equipments operations request paged ");
            GetEquipmentPagedInput input = parseInput(cmd, requestBody, GetEquipmentInput.class);
            PagedList pagedList = this.resourceHandler.getEquipmentPaged(input);
            Integer startPos = input.getStartPos() != null ? input.getStartPos() : 0;
            GetRackPagedOutputBuilder outputBuilder = new GetRackPagedOutputBuilder();
            outputBuilder.setNode((List<Node>) pagedList.getPage(startPos, input.getHowMany()));
            outputBuilder.setStartPos(startPos);
            outputBuilder.setTotalRecords(pagedList.getRecordsNumber());
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get equipment the reason is :" + ex.getMessage());
        }
    }
}
