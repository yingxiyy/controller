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
import net.flex.dci.otn.controller.nms.utils.PagedList;
import net.flex.dci.otn.controller.nms.utils.RetrieveElementExtractor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodePagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * get-site-node get-site-node-paged
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologySiteNode extends BaseNms {

    private static final String GET_SITE_NODE = "nms:get-site-node";

    private static final String GET_SITE_NODE_PAGED = "nms:get-site-node-paged";


    @Autowired
    private NodeHandler nodeHandler;

    public TopologySiteNode(NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        switch (cmd) {
            case GET_SITE_NODE:
                returnValue = getSiteNode(cmd, requestBody);
                break;
            case GET_SITE_NODE_PAGED:
                returnValue = getSiteNodePagedNew(cmd, requestBody);
                break;
            default:
                throw new UnsupportedOperationException(
                        "unsupported method for site nms operations");
        }
        return returnValue;
    }

    private String getSiteNodePagedNew(String cmd, String requestBody) throws CommonException {
        try {
            log.info("start to get site node paged");
            GetSiteNodePagedInput input = parseInput(cmd, requestBody, GetSiteNodePagedInput.class);
//            PagedList pagedList = this.nodeHandler.getSiteNodePaged(input);
            RetrieveTopologyDto retrieveTopologyDto = RetrieveElementExtractor.nodeExtract(
                    input);
            PageResult<Node> pageResult = this.nodeHandler.getSiteNodePagedNew(retrieveTopologyDto);
            GetSiteNodePagedOutputBuilder outputBuilder = new GetSiteNodePagedOutputBuilder();
            outputBuilder.setTotalRecords(Math.toIntExact(pageResult.getTotal()));
            Integer startPos = input.getStartPos() == null ? 0 : input.getStartPos();
            outputBuilder.setStartPos(startPos);
            outputBuilder.setNode(pageResult.getList());
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site node the reason is :" + ex.getMessage());
        }
    }

    private String getSiteNodePaged(String cmd, String requestBody) throws CommonException {
        try {
            log.info("start to get site node paged");
            GetSiteNodePagedInput input = parseInput(cmd, requestBody, GetSiteNodePagedInput.class);
            PagedList pagedList = this.nodeHandler.getSiteNodePaged(input);
            GetSiteNodePagedOutputBuilder outputBuilder = new GetSiteNodePagedOutputBuilder();
            outputBuilder.setTotalRecords(pagedList.getRecordsNumber());
            Integer startPos = input.getStartPos() == null ? 0 : input.getStartPos();
            outputBuilder.setStartPos(startPos);
            outputBuilder.setNode((List<Node>) pagedList.getPage(startPos, input.getHowMany()));
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site node the reason is :" + ex.getMessage());
        }
    }

    private String getSiteNode(String cmd, String requestBody) throws CommonException {
        try {
            log.info("start to get site node paged");
            GetSiteNodeInput input = parseInput(cmd, requestBody, GetSiteNodeInput.class);
//            PagedList pagedList = this.nodeHandler.getSiteNodePaged(input);
            RetrieveTopologyDto retrieveTopologyDto = RetrieveElementExtractor.nodeExtract(
                    input);
            List<Node> result = this.nodeHandler.getSiteNode(retrieveTopologyDto);
            GetSiteNodeOutputBuilder outputBuilder = new GetSiteNodeOutputBuilder();

            outputBuilder.setNode(result);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site node the reason is :" + ex.getMessage());
        }
    }
}
