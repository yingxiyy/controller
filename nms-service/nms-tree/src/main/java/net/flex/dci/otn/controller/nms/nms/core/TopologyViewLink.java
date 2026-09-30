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
import net.flex.dci.otn.controller.nms.nms.handler.ViewTopologyHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/29 13:15
 */
@Slf4j
@Component
public class TopologyViewLink extends BaseNms {

    private static final String GET_VIEW_LINK_BY_PLANE = "nms:get-view-link-groupby-plane";

    private static final String LIST_VIEW_PLANE = "nms:list-view-plane";

    private static final String LIST_VIEW_LINK_BY_PLANE = "nms:get-view-link-by-plane";
    private static final String LIST_VIEW_LINK_BY_PLANE_STARTWITH = "nms:get-view-link-by-plane-startwith";

    @Autowired
    private ViewTopologyHandler viewTopologyHandler;

    public TopologyViewLink(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody) throws CommonException {
        log.debug("start to execute request for the view topology link,the cmd is {}", cmd);
        String returnValue = null;
        if (cmd.equals(GET_VIEW_LINK_BY_PLANE)) {
            returnValue = getViewLinkByPlane(cmd, requestBody);
        } else if (cmd.equals(LIST_VIEW_PLANE)) {
            returnValue = listViewPlane(cmd);
        } else if (cmd.equals(LIST_VIEW_LINK_BY_PLANE)) {
            returnValue = listViewLinkByPlane(cmd, requestBody);
        } else if (cmd.equals(LIST_VIEW_LINK_BY_PLANE_STARTWITH)) {
            returnValue = listViewLinkByPlaneStartwith(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException(
                    "unsupported nms operation method for view topology");
        }
        return returnValue;
    }

    private String listViewLinkByPlane(String cmd, String requestBody) {
        log.debug("start list view link by plane and link level");
        GetViewLinkByPlaneInput getViewLinkByPlaneInput = parseInput(cmd, requestBody,
                GetViewLinkByPlaneInput.class);
        GetViewLinkByPlaneOutput result = this.viewTopologyHandler.getViewLinkByPlane(
                getViewLinkByPlaneInput);
        return serializeDataObject(cmd, result);
    }

    private String listViewLinkByPlaneStartwith(String cmd, String requestBody) {
        log.debug("start list view link by plane and link level, findout all plane as one for startwith");
        GetViewLinkByPlaneStartwithInput input = parseInput(cmd, requestBody,
                GetViewLinkByPlaneStartwithInput.class);

        GetViewLinkByPlaneStartwithOutput result = this.viewTopologyHandler.getViewLinkByPlaneStartwith(input);
        return serializeDataObject(cmd, result);
    }

    /**
     * list all the view plane for the view topology
     *
     * @return
     */
    private String listViewPlane(String cmd) {
        log.debug("list all the view plane");
        ListViewPlaneOutput result = this.viewTopologyHandler.listAllViewPlane();
        return serializeDataObject(cmd, result);
    }

    private String getViewLinkByPlane(String cmd, String requestBody) throws CommonException {
        log.debug("start to get view link by plane ");
//        GetViewLinkGroupbyPlaneInput getViewLinkGroupbyPlaneInput = null;
//        if (!StringUtils.isEmpty(requestBody)) {
//            getViewLinkGroupbyPlaneInput = parseInput(cmd, requestBody,
//                    GetViewLinkGroupbyPlaneInput.class);
//        }
        GetViewLinkGroupbyPlaneOutput result = this.viewTopologyHandler.getViewLinkGroupedByPlane(
        );
        return serializeDataObject(cmd, result);

    }
}
