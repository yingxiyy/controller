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
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.handler.NodeHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.RetrieveElementExtractor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodePagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodeUnstuffedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * get-phy-node get-phy-node-paged
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyPhyNode extends BaseNms {

    private final String GET_PHY_NODE = "nms:get-phy-node";

    private final String GET_PHY_NODE_PAGED = "nms:get-phy-node-paged";

    private final String GET_PHY_NODE_UNSTUFFED = "nms:get-phy-node-unstuffed";


    @Autowired
    private NodeHandler nodeHandler;

    public TopologyPhyNode(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        switch (cmd) {
            case GET_PHY_NODE:
                returnValue = getPhyNode(cmd, requestBody);
                break;
            case GET_PHY_NODE_PAGED:
                returnValue = getPhyNodePaged(cmd, requestBody);
                break;
            case GET_PHY_NODE_UNSTUFFED:
                returnValue = getPhyNodeUnStuffed(cmd, requestBody);
                break;
            default:
                throw new UnsupportedOperationException("unsupported phy node nms operation");
        }
        return returnValue;
    }

    //this method exports node only include equipments.
    private String getPhyNodeUnStuffed(String cmd, String requestBody) {
        log.info("start to get unstuffed phy node {}", requestBody);
        try {
            GetPhyNodeUnstuffedInput input = parseInput(cmd,
                    requestBody, GetPhyNodeUnstuffedInput.class);

            List<Node> nodes = nodeHandler.getUnStuffedPhyNode(input.getSiteNodeId());
            GetPhyNodeOutputBuilder outputBuilder = new GetPhyNodeOutputBuilder();
            outputBuilder.setNode(nodes);
            String result = serializeDataObject(cmd, outputBuilder.build());
            return result;
        } catch (CommonException ex) {
            throw new CommonException(ex.getType(), ex.getDetail());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());

        }
    }

//    private String getPhyNodePaged(String cmd, String requestBody) throws CommonException {
//        log.info("start to get phy node paged, request body {}", requestBody);
//        try {
//            GetPhyNodePagedInput input = () parseInput(cmd, requestBody);
//            PagedList page = nodeHandler.getPhyNodePaged(input);
//            Integer startPos = input.getStartPos() == null ? 0 : input.getStartPos();
//            GetPhyNodePagedOutputBuilder outputBuilder = new GetPhyNodePagedOutputBuilder();
//            outputBuilder.setNode((List<Node>) page.getPage(startPos, input.getHowMany()));
//            outputBuilder.setStartPos(startPos);
//            outputBuilder.setTotalRecords(page.getRecordsNumber());
//            return serializeDataObject(cmd, outputBuilder.build());
//        } catch (Exception ex) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
//        }
//    }


    private String getPhyNodePaged(String cmd, String requestBody) throws CommonException {
        log.info("start to get phy node paged, request body {}", requestBody);
        try {
            GetPhyNodePagedInput input = parseInput(cmd, requestBody, GetPhyNodePagedInput.class);
//            PagedList page = nodeHandler.getPhyNodePaged(input);
            RetrieveTopologyDto retrieveDto = RetrieveElementExtractor.nodeExtract(input);
            PageResult<Node> pageResult = nodeHandler.getPhyNodePagedNew(retrieveDto);
            Integer startPos = input.getStartPos() == null ? 0 : input.getStartPos();
            GetPhyNodePagedOutputBuilder outputBuilder = new GetPhyNodePagedOutputBuilder();
            outputBuilder.setNode(pageResult.getList());
            outputBuilder.setStartPos(startPos);
            outputBuilder.setTotalRecords(Math.toIntExact(pageResult.getTotal()));
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get current node paged reason:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    private String getPhyNode(String cmd, String requestBody) throws CommonException {
        log.info("start to get phy node {}", requestBody);
        try {
            GetPhyNodeInput input = parseInput(cmd, requestBody, GetPhyNodeInput.class);
            RetrieveTopologyDto retrieveDto = RetrieveElementExtractor.nodeExtract(input);
            List<Node> nodes = nodeHandler.listAllPhyNodes(retrieveDto);
            GetPhyNodeOutputBuilder outputBuilder = new GetPhyNodeOutputBuilder();
            outputBuilder.setNode(nodes);
            String result = serializeDataObject(cmd, outputBuilder.build());
            return result;
        } catch (CommonException ex) {
            throw new CommonException(ex.getType(), ex.getDetail());
        } catch (Exception ex) {
            log.error("failed to get phy node,the reason is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("failed to get phy node,the reason is %s", ex.getMessage()));

        }
    }
}
