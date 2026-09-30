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
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import net.flex.dci.otn.controller.nms.nms.handler.LinkHandler;
import net.flex.dci.otn.controller.nms.nms.handler.QueryLinkHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRelatedSiteLinksInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRelatedSiteLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkByNodeIpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkByNodeIpOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkOtsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkOtsOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkPagedOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinksBetweenTwoSitesInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinksBetweenTwoSitesOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.related.site.links.output.RelatedSiteLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.links.between.two.sites.output.LinkInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * get-site-link get-site-link-by-node-ip
 *
 *
 * getSiteLinksBetweenTwoSites
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologySiteLink extends BaseNms {


    private static final String GET_SITE_LINK_PAGED = "nms:get-site-link-paged";
    private static final String GET_SITE_LINK_OTS = "nms:get-site-link-ots";
    private static final String GET_SITE_LINK_BY_NODEIP = "nms:get-site-link-by-nodeIp";
    private static final String GET_SITE_LINK_BETWEEN_TWO_SITES = "nms:get-site-links-between-two-sites";
    private static final String GET_SITE_LINK = "nms:get-site-link";
    private static final String GET_RELATED_SITE_LINKS = "nms:get-related-site-links";
    @Autowired
    private LinkHandler linkHandler;

    @Autowired
    private QueryLinkHandler queryLinkHandler;

    public TopologySiteLink(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        log.info("start to execute get topology sub site link nms operations");
        String returnValue = null;
        switch (cmd) {
            case GET_SITE_LINK:
                returnValue = getSiteLink(cmd, requestBody);
                break;
            case GET_SITE_LINK_PAGED:
                returnValue = getSiteLinkPaged(cmd, requestBody);
                break;
            case GET_SITE_LINK_OTS:
                returnValue = getSiteLinkOts(cmd, requestBody);
                break;
            case GET_SITE_LINK_BETWEEN_TWO_SITES:
                returnValue = getSiteLinkBetweenTwoSites(cmd, requestBody);
                break;
            case GET_SITE_LINK_BY_NODEIP:
                returnValue = getSiteLinkByNodeIP(cmd, requestBody);
                break;
            case GET_RELATED_SITE_LINKS:
                returnValue = getSiteLinkRelativeSiteLinks(cmd, requestBody);
                break;
            default:
                throw new UnsupportedOperationException(
                        "unknown nms operation cmd for the site link");
        }
        return returnValue;
    }

    private String getSiteLinkRelativeSiteLinks(String cmd, String requestBody)
            throws CommonException {
        log.info("start get site link relative site links");
        try {
            GetRelatedSiteLinksInput input = parseInput(cmd, requestBody,
                    GetRelatedSiteLinksInput.class);
            List<RelatedSiteLink> relatedSiteLinks = this.linkHandler.getRelatedSiteLink(input);
            GetRelatedSiteLinksOutputBuilder outputBuilder = new GetRelatedSiteLinksOutputBuilder();
            outputBuilder.setRelatedSiteLink(relatedSiteLinks);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get relative site links,the reason :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get relative site link,the reason is " + ex.getMessage(), ex);
        }

    }

    private String getSiteLinkByNodeIP(String cmd, String requestBody) throws CommonException {
        log.info("start to execute get site link by node ip cmd");
        try {
            GetSiteLinkByNodeIpInput input = parseInput(cmd,
                    requestBody, GetSiteLinkByNodeIpInput.class);
            GetSiteLinkByNodeIpOutputBuilder outputBuilder = linkHandler
                    .getSiteLinkByNodeIp(input);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site link by node ip the reason is " + ex.getMessage());
        }
    }

    private String getSiteLinkBetweenTwoSites(String cmd, String requestBody)
            throws CommonException {
        try {
            log.info("start to get site link between two sites ");
            GetSiteLinksBetweenTwoSitesInput input = parseInput(cmd, requestBody,
                    GetSiteLinksBetweenTwoSitesInput.class);
            List<LinkInfo> linkInfos = this.linkHandler.getSiteLinksBetweenTwoSites(input);
            GetSiteLinksBetweenTwoSitesOutputBuilder outputBuilder = new GetSiteLinksBetweenTwoSitesOutputBuilder();
            outputBuilder.setTopologyRef(new TopologyId(TopoNameConstants.Site_Topo_Key));
            outputBuilder.setLinkInfo(linkInfos);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site link the reason is :" + ex.getMessage(), ex);
        }
    }

    private String getSiteLinkOts(String cmd, String requestBody) throws CommonException {
        try {
            log.info("start to get site link ots");
            GetSiteLinkOtsInput input = SerializeUtil.parseRpcInput(RPC_NAMESPACE,
                    cmd, requestBody, GetSiteLinkOtsInput.class);
            GetSiteLinkOtsOutput output = this.linkHandler.getSiteLinkOts(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site link ots the reason is :" + ex.getMessage());
        }
    }

    private String getSiteLinkPaged(String cmd, String requestBody) throws CommonException {
        try {
            log.info("start to execute get site link paged nms operation cmd");
            GetSiteLinkPagedInput input = parseInput(cmd, requestBody, GetSiteLinkPagedInput.class);
            PageResult<Link> pagedList = this.linkHandler.getSiteLinkPaged(input);
            GetSiteLinkPagedOutputBuilder outputBuilder = new GetSiteLinkPagedOutputBuilder();
            outputBuilder.setLink(pagedList.getList());
            outputBuilder.setStartPos(input.getStartPos());
            outputBuilder.setTotalRecords(Math.toIntExact(pagedList.getTotal()));
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get site link ", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site link the reason is :" + ex.getMessage(), ex);
        }
    }

    private String getSiteLink(String cmd, String requestBody) throws CommonException {
        try {
            log.info("start to execute get-site-link nms operation cmd");
            GetSiteLinkInput input = parseInput(cmd, requestBody, GetSiteLinkInput.class);
            List<Link> links = this.linkHandler.getSiteLink(input);
            GetSiteLinkOutputBuilder outputBuilder = new GetSiteLinkOutputBuilder();
            outputBuilder.setLink(links);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site link the reason is:" + ex.getMessage());
        }
    }
}
