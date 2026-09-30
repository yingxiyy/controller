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
import net.flex.dci.otn.controller.nms.nms.dto.tunnel.TunnelBetweenSitePagedInfo;
import net.flex.dci.otn.controller.nms.nms.handler.TunnelHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkRelatedTunnelsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkRelatedTunnelsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelBetweenSitePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelBetweenSitePagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelIdOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelPagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.connection.client.server.relation.Tunnel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.link.related.tunnels.output.RelatedTunnels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * getTunnelId
 *
 * @date: 2021/3/30
 */

@Slf4j
@Component
public class TopologyTunnel extends BaseNms {

    private final static String GET_TUNNEL_CMD = "nms:get-tunnel";


    private final static String GET_TUNNEL_PAGED_CMD = "nms:get-tunnel-paged";


    private final static String GET_TUNNEL_ID = "nms:get-tunnel-id";

    private final static String GET_RELATED_TUNNELS = "nms:get-site-link-related-tunnels";

    private final static String GET_TUNNEL_BETWEEN_SITE_PAGED = "nms:get-tunnel-between-site-paged";

    @Autowired
    private TunnelHandler tunnelHandler;

    public TopologyTunnel(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        if (cmd.equals(GET_TUNNEL_CMD)) {
            returnValue = executeCMD(cmd, requestBody);
        } else if (cmd.equals(GET_TUNNEL_ID)) {
            returnValue = getTunnelId(cmd, requestBody);
        } else if (cmd.equals(GET_TUNNEL_PAGED_CMD)) {
            returnValue = getTunnelPaged(cmd, requestBody);
        } else if (cmd.equals(GET_RELATED_TUNNELS)) {
            returnValue = getRelatedTunnels(cmd, requestBody);
        } else if (cmd.equals(GET_TUNNEL_BETWEEN_SITE_PAGED)) {
            returnValue = getTunnelBetweenSitePaged(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException("unsupported nms operation method for tunnel");
        }
        return returnValue;
    }

    /**
     * get tunnel between site paged
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getTunnelBetweenSitePaged(String cmd, String requestBody) {
        try {
            GetTunnelBetweenSitePagedInput input = parseInput(cmd, requestBody,
                    GetTunnelBetweenSitePagedInput.class);
            TunnelBetweenSitePagedInfo tunnelBetweenSitePagedInfo = this.tunnelHandler.getTunnelBetweenSitePaged(
                    input);
            GetTunnelBetweenSitePagedOutputBuilder outputBuilder = getGetTunnelBetweenSitePagedOutputBuilder(
                    tunnelBetweenSitePagedInfo, input);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get tunnel between the site ,the error is:{}", ex.getMessage(),
                    ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    private GetTunnelBetweenSitePagedOutputBuilder getGetTunnelBetweenSitePagedOutputBuilder(
            TunnelBetweenSitePagedInfo tunnelBetweenSitePagedInfo,
            GetTunnelBetweenSitePagedInput input) {
        PageResult<Tunnel> tunnelPageResult = tunnelBetweenSitePagedInfo.getTunnelPageResult();
        GetTunnelBetweenSitePagedOutputBuilder outputBuilder = new GetTunnelBetweenSitePagedOutputBuilder();
        outputBuilder.setTunnel(tunnelPageResult.getList());
        outputBuilder.setStartPos(input.getStartPos());
        outputBuilder.setTotalRecords(Math.toIntExact(tunnelPageResult.getTotal()));
        outputBuilder.setTotalBandwidth(tunnelBetweenSitePagedInfo.getTotalBandwidth());
        outputBuilder.setOccupiedChannelCount(
                tunnelBetweenSitePagedInfo.getOccupiedChannelCount());
        return outputBuilder;
    }

    private String getRelatedTunnels(String cmd, String requestBody) {
        try {
            GetSiteLinkRelatedTunnelsInput input = parseInput(cmd, requestBody,
                    GetSiteLinkRelatedTunnelsInput.class);
            List<RelatedTunnels> relatedTunnels = this.tunnelHandler.getSiteLinksRelativeTunnels(
                    input);
            GetSiteLinkRelatedTunnelsOutputBuilder outputBuilder = new GetSiteLinkRelatedTunnelsOutputBuilder();
            outputBuilder.setRelatedTunnels(relatedTunnels);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get related tunnel,the error is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }


    private String getTunnelPaged(String cmd, String requestBody) throws CommonException {
        try {
            GetTunnelPagedInput input = parseInput(cmd, requestBody, GetTunnelPagedInput.class);
            PageResult<Tunnel> pagedList = this.tunnelHandler.getTunnelPaged(input);
//            Integer pos = input.getStartPos() == null ? 0 : input.getStartPos();
            GetTunnelPagedOutputBuilder outputBuilder = new GetTunnelPagedOutputBuilder();
            outputBuilder.setTunnel(pagedList.getList());
            outputBuilder.setStartPos(input.getStartPos());
            outputBuilder.setTotalRecords(Math.toIntExact(pagedList.getTotal()));
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    /**
     * get tunnel id
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getTunnelId(String cmd, String requestBody) throws CommonException {
        try {
            GetTunnelIdInput input = parseInput(cmd, requestBody, GetTunnelIdInput.class);
            String tunnelId = this.tunnelHandler.getTunnelId(input);
            GetTunnelIdOutputBuilder outputBuilder = new GetTunnelIdOutputBuilder();
            outputBuilder.setTunnelId(tunnelId);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get tunnel id" + ex.getMessage());
        }
    }

    /**
     * execute nms rpc cmd
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String executeCMD(String cmd, String requestBody) throws CommonException {
        try {
            GetTunnelInput input = parseInput(cmd, requestBody, GetTunnelIdInput.class);
            List<Tunnel> tunnel = this.tunnelHandler.getTunnel(input);
            GetTunnelOutputBuilder outputBuilder = new GetTunnelOutputBuilder();
            outputBuilder.setTunnel(tunnel);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get tunnel " + ex.getMessage());
        }
    }
}
