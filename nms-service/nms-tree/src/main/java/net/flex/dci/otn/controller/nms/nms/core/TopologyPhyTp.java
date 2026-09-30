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
import net.flex.dci.otn.controller.nms.nms.handler.TerminationPointHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.PagedList;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetCardPortsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetCardPortsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyTpOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyTpPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyTpPagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRealMpoPortInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRealMpoPortOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.real.mpo.port.output.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * get-phy-tp get-phy-tp-paged
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyPhyTp extends BaseNms {

    private final String GET_PHY_TP = "nms:get-PhyTp";

    private final String GET_PHY_TP_PAGED = "nms:get-PhyTp-paged";

    private final String GET_REAL_MPO_PORT = "nms:get-real-mpo-port";

    private final String GET_CARD_PORTS = "nms:get-card-ports";

    @Autowired
    private TerminationPointHandler terminationPointHandler;

    public TopologyPhyTp(NetconfTopology netconfTopology,
            TerminationPointHandler terminationPointHandler) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        switch (cmd) {
            case GET_PHY_TP:
                returnValue = getPhyTp(cmd, requestBody);
                break;
            case GET_PHY_TP_PAGED:
                returnValue = getPhyTpPaged(cmd, requestBody);
                break;
            case GET_REAL_MPO_PORT:
                returnValue = getMPORealPort(cmd, requestBody);
                break;
            case GET_CARD_PORTS:
                returnValue = getCardPorts(cmd, requestBody);
                break;
            default:
                throw new UnsupportedOperationException(
                        "unknown nms operations for the termination point");
        }
        return returnValue;
    }

    /**
     * termination point physically locate on card
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getCardPorts(String cmd, String requestBody) {
        try {
            log.debug("get the card's physical termination point,the request body is :{}",
                    requestBody);
            GetCardPortsInput input = parseInput(cmd, requestBody, GetCardPortsInput.class);
            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.card.ports.output.TerminationPoint> physicalPorts = terminationPointHandler.getCardPhysicalPort(
                    input);
            GetCardPortsOutputBuilder outputBuilder = new GetCardPortsOutputBuilder();
            outputBuilder.setTerminationPoint(physicalPorts);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get the card physical port");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    /**
     * get real MPO real port
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getMPORealPort(String cmd, String requestBody) {
        try {
            log.debug("get mpo real port,request body is:{}", requestBody);
            GetRealMpoPortInput input = parseInput(cmd, requestBody, GetRealMpoPortInput.class);
            List<TerminationPoint> realMpoTps = terminationPointHandler.getRealMpoTpByVisualTp(
                    input);
            GetRealMpoPortOutputBuilder outputBuilder = new GetRealMpoPortOutputBuilder();
            outputBuilder.setTerminationPoint(realMpoTps);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get real Mpo ");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    /**
     * get phy tp paged
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getPhyTpPaged(String cmd, String requestBody) throws CommonException {
        try {
            GetPhyTpPagedInput input = parseInput(cmd, requestBody, GetPhyTpPagedInput.class);
            List<Node> nodes = terminationPointHandler.getPhyTpPaged(input);
            PagedList page = new PagedList(nodes);
            page.setFilter(input.getFilter());
            page.sort(input.getSortInfos());

            Integer startPos = input.getStartPos() == null ? 0 : input.getStartPos();
            GetPhyTpPagedOutputBuilder ob = new GetPhyTpPagedOutputBuilder();
            ob.setTotalRecords(page.getRecordsNumber());
            ob.setStartPos(startPos);
            ob.setNode(
                    (List<Node>) page
                            .getPage(startPos, input.getHowMany()));
            return serializeDataObject(cmd, ob.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    /**
     * get phy tp
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getPhyTp(String cmd, String requestBody) throws CommonException {
        try {
            GetPhyTpInput input = parseInput(cmd, requestBody, GetPhyTpInput.class);
            List<Node> nodes = terminationPointHandler
                    .getPhyTp(input);
            GetPhyTpOutputBuilder outputBuilder = new GetPhyTpOutputBuilder();
            outputBuilder.setNode(nodes);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }
}
