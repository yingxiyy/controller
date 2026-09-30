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
import net.flex.dci.otn.controller.nms.nms.handler.LinkHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkPagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * get-och-link get-och-link-paged
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyOCHLink extends BaseNms {

    private static final String GET_OCH_LINK = "nms:get-och-link";

    private static final String GET_OCH_LINK_PAGED = "nms:get-och-link-paged";

    @Autowired
    private LinkHandler linkHandler;

    public TopologyOCHLink(NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public String executeRequest(String cmd, String requestBody) throws CommonException {
        String returnValue = null;
        if (cmd.equals(GET_OCH_LINK)) {
            returnValue = getOchLink(cmd, requestBody);
        } else if (cmd.equals(GET_OCH_LINK_PAGED)) {
            returnValue = getOchLinkPaged(cmd, requestBody);
        }
        return returnValue;
    }

    /**
     * get och link paged
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getOchLinkPaged(String cmd, String requestBody) throws CommonException {
        try {
            GetOchLinkPagedInput input = parseInput(cmd, requestBody, GetOchLinkPagedInput.class);
            PageResult<Link> ochLinkPaged = this.linkHandler.getOchLinksPagedNew(
                    input);
//            PagedList pagedList = this.linkHandler.getOchLinksPagedNew(input);
//
            Integer start = input.getStartPos() == null ? 0 : input.getStartPos();
            GetOchLinkPagedOutputBuilder outputBuilder = new GetOchLinkPagedOutputBuilder();
            outputBuilder.setLink(ochLinkPaged.getList());
//            outputBuilder
//                    .setLink((List<Link>) pagedList.getPage(start, pagedList.getRecordsNumber()));
            outputBuilder.setStartPos(start);
            outputBuilder.setTotalRecords(Math.toIntExact(ochLinkPaged.getTotal()));
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    /**
     * get och link
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getOchLink(String cmd, String requestBody) throws CommonException {
        try {
            GetOchLinkInput input = parseInput(cmd, requestBody, GetOchLinkInput.class);
            List<Link> links = linkHandler.getOchLinks(input);
            GetOchLinkOutputBuilder outputBuilder = new GetOchLinkOutputBuilder();
            outputBuilder.setLink(links);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }
}
