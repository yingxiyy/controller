/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.INMSRetrieveOperations;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.NmsRetrieverOperationsResolver;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel.TunnelRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.dto.tunnel.TunnelBetweenSitePageResult;
import net.flex.dci.otn.controller.nms.nms.dto.tunnel.TunnelBetweenSitePagedInfo;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyEquipment;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyTp;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.RetrieveElementExtractor;
import net.flex.dci.otn.controller.nms.utils.TunnelComparator;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkRelatedTunnelsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelBetweenSitePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.connection.client.server.relation.Tunnel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.link.related.tunnels.output.RelatedTunnels;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.link.related.tunnels.output.RelatedTunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @date: 2021/4/8
 */
@Slf4j
@Component
public class TunnelHandler extends AbstractBaseHandler {

    private final TunnelRetrieveHandler tunnelRetrieveHandler;

    private final NmsRetrieverOperationsResolver nmsRetrieverOperationsResolver;

    public TunnelHandler(NetconfTopology netconfTopology,
            TunnelRetrieveHandler tunnelRetrieveHandler,
            NmsRetrieverOperationsResolver nmsRetrieverOperationsResolver) {
        super(netconfTopology);
        this.tunnelRetrieveHandler = tunnelRetrieveHandler;
        this.nmsRetrieverOperationsResolver = nmsRetrieverOperationsResolver;
    }


    @Override
    public PageResult<Tunnel> getTunnelPaged(
            GetTunnelPagedInput input) throws Exception {
        RetrieveTopologyDto retrieveTopologyDto = RetrieveElementExtractor.connectionExtract(input);
        INMSRetrieveOperations retrieveOperations = nmsRetrieverOperationsResolver.resolve(
                retrieveTopologyDto);
//        PageResult<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> pageTunnelResult = retrieveOperations.retrieveAllTunnelPaged(
//                retrieveTopologyDto.getPageNum(), retrieveTopologyDto.getPageSize());
        PageResult<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> pageTunnelResult = retrieveOperations.retrieveAllTunnelPaged(
                retrieveTopologyDto);
        PageResult<Tunnel> pageResult = PageResult.<Tunnel>builder()
                .pageNum(pageTunnelResult.getPageNum())
                .pageSize(pageTunnelResult.getPageSize())
                .total(pageTunnelResult.getTotal())
                .list(nmsOutputConverters.convert2NmsOutput(pageTunnelResult.getList())).build();
        return pageResult;
    }

    @Override
    public String getTunnelId(GetTunnelIdInput input) throws Exception {
        String resourceId = input.getResourceId();
        String splits[] = resourceId.split("#");
        if (splits == null || (splits.length != 3 && splits.length != 4)) {
            if (splits.length == 2) {
                return Constants.EQUIP;
            } else {
                log.warn("Resource id {} is not a equipment id or termination point id.",
                        resourceId);
                return Constants.NODE;
            }
        }
        String neId = new StringBuilder().append(splits[0]).append("#").append(splits[1])
                .toString();

        Physical phy = netconfTopology.getPhysical(neId);
        if (phy == null) {
            log.warn("Ne {} does not exist for resource id {}.", neId, resourceId);
            return Constants.NODE;
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> tunnels = null;
        if (splits.length == 3) {//equip id
            tunnels = new PhyEquipment(netconfTopology)
                    .getTunnels(new TopologyId(Constants.PHY_TOPO_KEY), new NodeId(neId),
                            resourceId);
        } else {//tp id
            tunnels = new PhyTp(netconfTopology)
                    .getTunnels(new TopologyId(Constants.PHY_TOPO_KEY), new NodeId(neId),
                            new TpId(resourceId));
        }
        if (tunnels == null || tunnels.size() == 0) {
            if (splits.length == 2 || splits.length == 3 || splits.length == 4) {
                return Constants.EQUIP;
            } else {
                log.warn("No tunnel id is matched for {}.", resourceId);
                return Constants.NODE;
            }
        } else {
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> implTunnels = new ArrayList<>();
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel tunnel : tunnels) {
                if (tunnel.getImplementState() == ImplementState.Implement) {
                    implTunnels.add(tunnel);
                }
            }
            if (implTunnels.size() > 0) {
                Collections.sort(implTunnels, new TunnelComparator());
                return implTunnels.get(0).getTunnelId().getValue();
            } else {
                Collections.sort(tunnels, new TunnelComparator());
                return tunnels.get(0).getTunnelId().getValue();
            }
        }
    }


    /**
     * extract tunnels from input reuqest
     *
     * @param input
     * @return
     */
    @Override
    public List<Tunnel> getTunnel(GetTunnelInput input) throws Exception {
        RetrieveTopologyDto retrieveTopologyDto = RetrieveElementExtractor.connectionExtract(input);
        INMSRetrieveOperations retrieveOperations = nmsRetrieverOperationsResolver.resolve(
                retrieveTopologyDto);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> tunnels = retrieveOperations.retrieveAllTunnel(
                retrieveTopologyDto);
        return nmsOutputConverters.convert2NmsOutput(tunnels);
    }

    @Override
    public List<RelatedTunnels> getSiteLinksRelativeTunnels(GetSiteLinkRelatedTunnelsInput input) {
        log.info("get site link relative tunnels,the site link is:{}",
                input.getSiteLinkIds().size());
        log.debug("get site link relative tunnels ,the site link id is:{}", input.getSiteLinkIds());
        List<String> siteLinkIds = input.getSiteLinkIds();
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "get site links related tunnels request site linkIds should not be null");
        }
        List<String> ochLinkIds = netconfTopology.listAllOchLinkIdsBySiteLinkIds(siteLinkIds);
        if (CollectionUtils.isEmpty(ochLinkIds)) {
            log.warn("the site linkId ref tunnel site link:{} is not existed", siteLinkIds);
            return new ArrayList<>();
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> tunnels = netconfTopology.getTunnelsBaseOnOchLinks(
                ochLinkIds);
        List<RelatedTunnels> relatedTunnelList = tunnels.stream().map(tunnel -> {
            String tunnelId = tunnel.getTunnelId().getValue();
            String tunnelName = tunnel.getFriendlyName();
            String tunnelSubnet = tunnel.getPlaneName();
            String tunnelSubnetId = tunnel.getPlaneId();
            return new RelatedTunnelsBuilder().setRelatedTunnelId(tunnelId)
                    .setRelatedTunnelName(tunnelName)
                    .setRelatedTunnelSubnet(tunnelSubnet)
                    .setRelatedTunnelSubnetId(tunnelSubnetId)
                    .build();
        }).collect(Collectors.toList());

        return relatedTunnelList;
    }

    @Override
    public TunnelBetweenSitePagedInfo getTunnelBetweenSitePaged(
            GetTunnelBetweenSitePagedInput input) {
        log.info("retrieve all tunnel paged by site ,the site from :{} to :{}",
                input.getSourceSiteId(), input.getDestSiteId());
        log.debug("retrieve all tunnel paged,the input is:{}", input);
        validateGetTunnelBetweenSitePagedInput(input);
        String sourceSiteId = input.getSourceSiteId().getValue();
        String destSiteId = input.getDestSiteId().getValue();
        RetrieveTopologyDto retrieveTopologyDto = RetrieveElementExtractor.connectionExtract(input);
        TunnelBetweenSitePageResult pageTunnelResult = tunnelRetrieveHandler.retrieveAllTunnelBetweenTwoSitePaged(
                sourceSiteId, destSiteId,
                retrieveTopologyDto);
        PageResult<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> tunnelPageResult = pageTunnelResult.getTunnelPageResult();
        PageResult<Tunnel> pageResult = PageResult.<Tunnel>builder()
                .pageNum(tunnelPageResult.getPageNum())
                .pageSize(tunnelPageResult.getPageSize())
                .total(tunnelPageResult.getTotal())
                .list(nmsOutputConverters.convert2NmsOutput(tunnelPageResult.getList())).build();
        return TunnelBetweenSitePagedInfo.builder()
                .tunnelPageResult(pageResult)
                .totalBandwidth(pageTunnelResult.getTotalBandwidth())
                .occupiedChannelCount(pageTunnelResult.getOccupiedChannelCount())
                .build();
    }

    private void validateGetTunnelBetweenSitePagedInput(GetTunnelBetweenSitePagedInput input) {
        log.debug("validate get tunnel between site paged ");
        NodeId sourceNodeId = input.getSourceSiteId();
        NodeId destNodeId = input.getDestSiteId();
        if (sourceNodeId == null) {
            log.error("the source site id should not be null");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the source site id should not be null");
        }
        if (destNodeId == null) {
            log.error("the destination site id should not be null");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the destination site id should not be null");
        }
        if (!netconfTopology.existedSite(sourceNodeId.getValue())) {
            log.error("the source site :{} is not existed", sourceNodeId.getValue());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the source site:%s is not existed", sourceNodeId.getValue()));
        }
        if (!netconfTopology.existedSite(destNodeId.getValue())) {
            log.error("the source site :{} is not existed", sourceNodeId.getValue());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the source site:%s is not existed", sourceNodeId.getValue()));
        }
    }


}
