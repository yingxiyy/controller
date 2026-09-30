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
import net.flex.dci.otn.controller.nms.exceptions.JSONParseException;
import net.flex.dci.otn.controller.nms.nms.component.route.LinkRouteHandler;
import net.flex.dci.otn.controller.nms.nms.handler.ThumbnailRouteHandler;
import net.flex.dci.otn.controller.nms.nms.handler.TunnelSiteRouteHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetDesignSimpleRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetDesignSimpleRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSimpleRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSimpleRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RouteDisplayInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RouteDisplayOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.output.RouteDisplayInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * nms operation for route
 *
 * cmd :nms:get-route
 *
 * @date: 2021/3/30
 */
@Slf4j
@Component
public class TopologyRoute extends BaseNms {

    private static final String GET_ROUTE_CMD = "nms:get-route";

    private static final String ROUTE_DISPLAY = "nms:route-display";

    private static final String GET_SIMPLE_ROUTE = "nms:get-simple-route";

    private static final String GET_TUNNEL_SITE_ROUTE = "nms:get-tunnel-site-route";

    private static final String GET_DESIGN_SIMPLE_ROUTE = "nms:get-design-simple-route";

    @Autowired
    private LinkRouteHandler routeHandler;

    @Autowired
    private ThumbnailRouteHandler thumbnailRouteHandler;

    @Autowired
    private TunnelSiteRouteHandler tunnelSiteRouteHandler;

    public TopologyRoute(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }


    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        log.info("start to handle the route nms operations ");
        String returnValue = null;
        if (cmd.equals(GET_ROUTE_CMD)) {
            returnValue = executeCMD(cmd, requestBody);
        } else if (cmd.equals(ROUTE_DISPLAY)) {
            returnValue = routeDisplay(cmd, requestBody);
        } else if (cmd.equals(GET_SIMPLE_ROUTE)) {
            returnValue = thumbnailRouteDisplay(cmd, requestBody);
        } else if (cmd.equals(GET_TUNNEL_SITE_ROUTE)) {
            returnValue = getTunnelSiteRouteDisplay(cmd, requestBody);
        } else if (cmd.equals(GET_DESIGN_SIMPLE_ROUTE)) {
            returnValue = getDesignThumbnailRouteDisplay(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException("unsupported route operations ");
        }
        return returnValue;
    }


    private String getTunnelSiteRouteDisplay(String cmd, String requestBody) {
        log.debug("get network tunnel ref  site detail route info");
        try {
            GetTunnelSiteRouteInput input = parseInput(cmd, requestBody,
                    GetTunnelSiteRouteInput.class);
            GetTunnelSiteRouteOutput output = tunnelSiteRouteHandler.getTunnelSiteNodeRoute(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            log.error("failed to get the site detail route the reason is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get the site route detail info,the reason is " + ex.getMessage());
        }
    }

    private String thumbnailRouteDisplay(String cmd, String requestBody) {
        log.debug("get thumbnail route for the request body");
        try {
            GetSimpleRouteInput input = parseInput(cmd, requestBody, GetSimpleRouteInput.class);
            GetSimpleRouteOutput output = thumbnailRouteHandler.getSimpleRoute(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            log.error("failed to get the thumbnail route the reason is :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get the thumbnail route the reason is:" + ex.getMessage());
        }

    }

    /**
     * get design thumbnail route display
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getDesignThumbnailRouteDisplay(String cmd, String requestBody) {
        log.debug("get thumbnail route for the request body:{}", requestBody);
        try {
            GetDesignSimpleRouteInput input = parseInput(cmd, requestBody,
                    GetDesignSimpleRouteInput.class);
            GetDesignSimpleRouteOutput output = thumbnailRouteHandler.getDesignThumbnailRoute(
                    input);
            return serializeDataObject(cmd, output);
        } catch (Exception e) {
            log.error("failed to get the design thumbnail route the reason is:{}", e.getMessage(),
                    e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get the design thumbnail route the reason is:" + e.getMessage(), e);
        }
    }

    /**
     * route display for the request
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String routeDisplay(String cmd, String requestBody)
            throws CommonException, JSONParseException {
        try {
            RouteDisplayInput input = parseInput(cmd, requestBody, RouteDisplayInput.class);
            List<RouteDisplayInfo> routes = routeHandler
                    .routeDisplay(input);
            RouteDisplayOutputBuilder outputBuilder = new RouteDisplayOutputBuilder();
            outputBuilder.setRouteDisplayInfo(routes);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get route the reason is :" + ex.getMessage());
        }
    }

    /**
     * get route for request body
     *
     * @param requestBody
     * @return
     */
    private String executeCMD(String cmd, String requestBody)
            throws CommonException, JSONParseException {
        try {
            long start = System.currentTimeMillis();
            GetRouteInput input = parseInput(cmd, requestBody, GetRouteInput.class);

            List<RouteInfo> routeInfos = routeHandler.getRoute(input);
            long cost = System.currentTimeMillis() - start;
            log.info("get current route cost:{} ms", cost);
            GetRouteOutputBuilder getRouteOutputBuilder = new GetRouteOutputBuilder();
            getRouteOutputBuilder.setRouteInfo(routeInfos);
            return serializeDataObject(cmd, getRouteOutputBuilder.build());
        } catch (Exception exception) {
            log.error("failed to get route the reason is:{}", exception.getMessage(), exception);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get route the reason is :" + exception
                            .getMessage());
        }
    }


}
