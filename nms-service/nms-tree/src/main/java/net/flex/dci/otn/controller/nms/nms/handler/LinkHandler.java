/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_VIEW_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.VERTICAL_LINE;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.controller.nms.constructs.YangRouteConstructor;
import net.flex.dci.otn.controller.nms.nms.component.amplifier.AmplifierEndpointResolver;
import net.flex.dci.otn.controller.nms.nms.component.amplifier.AmplifierEndpointResolver.OtsLinkTerminationPointInfo;
import net.flex.dci.otn.controller.nms.nms.component.amplifier.AmplifierHandler;
import net.flex.dci.otn.controller.nms.nms.component.connection.ServiceBearer;
import net.flex.dci.otn.controller.nms.nms.component.connection.SiteLinkRelationCalculator;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.INMSRetrieveOperations;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.NmsRetrieverOperationsResolver;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.dto.link.OtsLinkAmplifierRefCache;
import net.flex.dci.otn.controller.nms.nms.dto.link.SiteLinkGeneralInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.DirectionMetrics;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OmsLinkOtsLinkInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OmsLinkPAInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkAmplifierCLInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkAmplifierInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkRamanInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsTerminalInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.PhyNodeInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.RouteContractInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.RouteSegment;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.WrappedOtsLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.OchLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.PhyLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteTunnel;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.ViewLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyEquipment;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyNe;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyTp;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteNode;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteRack;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteTp;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetConfConvertors;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.PagedList;
import net.flex.dci.otn.controller.nms.utils.RetrieveElementExtractor;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.ApsXCCache;
import net.flex.dci.otn.topology.cache.model.TunnelCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOtsLinksUnderOmsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRelatedSiteLinksInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkByNodeIpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkByNodeIpOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkOtsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkOtsOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkOtsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinksBetweenTwoSitesInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.related.site.links.output.RelatedSiteLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.related.site.links.output.RelatedSiteLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.links.between.two.sites.output.LinkInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.link.ots.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.link.ots.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkRole;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @date: 2021/4/8
 */
@Slf4j
@Component
public class LinkHandler extends AbstractBaseHandler {

    private final YangRouteConstructor yangRouteConstructor;

    @Autowired
    private AmplifierHandler amplifierHandler;

    @Autowired
    private DciTopologyCacheManager topologyCacheManager;

    @Autowired
    private ServiceBearer serviceBearer;

    @Autowired
    private NmsRetrieverOperationsResolver nmsRetrieverOperationsResolver;

    @Autowired
    private SiteLinkRelationCalculator siteLinkRelationCalculator;

    @Autowired
    private AmplifierEndpointResolver amplifierEndpointResolver;


    public LinkHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
        this.yangRouteConstructor = new YangRouteConstructor(netconfTopology);
    }

    @Override
    public List<LinkInfo> getSiteLinksBetweenTwoSites(GetSiteLinksBetweenTwoSitesInput input)
            throws Exception {
        TopologyId topologyRef = new TopologyId(TopoNameConstants.Site_Topo_Key);
        List<String> nodeIds = input.getNodeIds();
        String plane = input.getPlane();
        return NetConfConvertors
                .convert2LinkInfo(this.getSiteLinksBetweenTwoSites(topologyRef, nodeIds, plane));
    }

    public List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> getSiteLinksBetweenTwoSites(
            TopologyId topologyRef, List<String> nodeIds,
            String plane)
            throws Exception {
        log.debug("start get all site links between two given sites, {}", nodeIds);
        if (nodeIds.size() != 2) {
            throw new Exception(
                    "the input nodeIds size must be two");
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks = netconfTopology.getSiteLinksBetweenSites(
                nodeIds.get(0), nodeIds.get(1));
        return siteLinks;
    }


    @Override
    public GetSiteLinkOtsOutput getSiteLinkOts(GetSiteLinkOtsInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        LinkId linkRef = input.getLinkRef();
        log.debug("GetSiteLinkOtsOutput {} {} ", topologyRef.getValue(), linkRef.getValue());
        GetSiteLinkInputBuilder siteLinkInputBuilder = new GetSiteLinkInputBuilder();
        siteLinkInputBuilder.setLinkRef(input.getLinkRef());
        siteLinkInputBuilder.setTopologyRef(input.getTopologyRef());
        List<Link> links = new SiteLink(
                netconfTopology)
                .getSiteLink(siteLinkInputBuilder.build());
        if (links == null || links.size() > 1) {
            throw new Exception("only support PHY OTS link");
        }

        Link siteLink = links.get(0);
        links = this
                .getPhyLink(siteLink.getTopologyRef(), null, null, null, null, siteLink.getLinkId(),
                        null);
        List<Link> otsLinkList = new LinkedList<>();
        for (Link link : links) {
            if (link.getPhysical().getLinkType().equals(LinkType.OtsLink)) {
                otsLinkList.add(link);
            }
        }

        List<Link> primary = new LinkedList<>();
        List<Link> secondary = new LinkedList<>();

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteTopoLink = netconfTopology
                .getLink(siteLink.getTopologyRef(), siteLink.getLinkId());
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route> routeList = siteTopoLink
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite()
                .getExplictRoute().getRoute();
        for (Route route : routeList) {
            yangRouteConstructor
                    .filterOutOTS(route.getPrimary().getExplicitRouteObjects(), otsLinkList,
                            primary);
            if (route.getSecondary() != null) {
                yangRouteConstructor
                        .filterOutOTS(route.getSecondary().getExplicitRouteObjects(), otsLinkList,
                                secondary);
            }
        }

        GetSiteLinkOtsOutputBuilder ob = new GetSiteLinkOtsOutputBuilder();
        ob.setLinkRef(siteLink.getLinkId()).setFriendlyName(siteLink.getSite().getFriendlyName());
        if (primary.size() > 0) {
            ob.setPrimary(new PrimaryBuilder().setLink(primary).build());
        }
        if (secondary.size() > 0) {
            ob.setSecondary(new SecondaryBuilder().setLink(secondary).build());
        }

        return ob.build();
    }

    @Override
    public GetSiteLinkByNodeIpOutputBuilder getSiteLinkByNodeIp(GetSiteLinkByNodeIpInput input)
            throws Exception {
        GetSiteLinkByNodeIpOutputBuilder ob = new GetSiteLinkByNodeIpOutputBuilder();
        List<Node> nodes = new NodeHandler(netconfTopology)
                .getPhyNode(new TopologyId(TopoNameConstants.Phy_Topo_Key),
                        null, null, null, null,
                        null, null);
        String nodeIp = input.getIp();

        for (Node node : nodes) {
            if (node.getPhysical() != null && node.getPhysical().getIp() != null && !""
                    .equals(node.getPhysical().getIp()) && nodeIp
                    .equals(node.getPhysical().getIp())) {
                ob = new QueryLinkHandler(netconfTopology).getSiteLinkByNodeIp(node);
                break;
            }
        }
        return ob;
    }

    public List<Link> getOchLinks(TopologyId topologyRef,
            NodeId nodeRef, String rackRef, String equipRef,
            TpId tpRef,
            LinkId linkRef, String tunnelRef) throws Exception {
        log.debug(
                "start get all OCH link topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
                topologyRef == null ? "null" : topologyRef.getValue(),
                nodeRef == null ? "null" : nodeRef.getValue(),
                rackRef == null ? "null" : rackRef, equipRef == null ? "null" : equipRef,
                tpRef == null ? "null" : tpRef.getValue(),
                linkRef == null ? "null" : linkRef.getValue(),
                tunnelRef == null ? "null" : tunnelRef);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> ntLinks = null;

        if (topologyRef == null) {
            throw new Exception("topologyRef is mandatory.");
        } else if (topologyRef.getValue().contains(OCH_TOPO_KEY)
                && nodeRef == null && rackRef == null
                && equipRef == null && tpRef == null && linkRef == null && tunnelRef == null) {
            // by OCH topo.
            Topology topo = netconfTopology.getTopology(topologyRef);
            if (topo == null) {
                throw new Exception(
                        "cannot find required Topology.");
            }
            ntLinks = topo.getLink();
        } else if (nodeRef != null && rackRef == null && equipRef == null && tpRef == null) {
            if (topologyRef.getValue().contains(SITE_TOPO_KEY)) {
                // by siteNode.
                ntLinks = new SiteNode(netconfTopology).getOchLinks(topologyRef, nodeRef);
            } else if (topologyRef.getValue().contains(PHY_TOPO_KEY)) {
                // by phyNE.
                ntLinks = new PhyNe(netconfTopology).getOchLinks(topologyRef, nodeRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(SITE_TOPO_KEY)
                && nodeRef != null && rackRef != null) {
            // by rack.
            ntLinks = new SiteRack(netconfTopology).getOchLinks(topologyRef, nodeRef, rackRef);
        } else if (topologyRef.getValue().contains(PHY_TOPO_KEY)
                && nodeRef != null && rackRef == null
                && equipRef != null) {
            // by equip.
            ntLinks = new PhyEquipment(netconfTopology).getOchLinks(topologyRef, nodeRef, equipRef);
        } else if (nodeRef != null && rackRef == null && tpRef != null) {
            if (topologyRef.getValue().equals(SITE_TOPO_KEY)) {
                // by siteTP.
                ntLinks = new SiteTp(netconfTopology).getOchLinks(topologyRef, nodeRef, tpRef);
            } else if (topologyRef.getValue().equals(PHY_TOPO_KEY)) {
                // by phyTp.
                ntLinks = new PhyTp(netconfTopology).getOchLinks(topologyRef, nodeRef, tpRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (nodeRef == null && rackRef == null && tpRef == null && linkRef != null) {
            if (topologyRef.getValue().contains(SITE_TOPO_KEY)) {
                // by siteLink.
                ntLinks = new SiteLink(netconfTopology).getOchLinks(topologyRef, linkRef);
            } else if (topologyRef.getValue().contains(PHY_TOPO_KEY)) {
                // by phyLink.
                ntLinks = new PhyLink(netconfTopology).getOchLinks(topologyRef, linkRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(SITE_TOPO_KEY) && nodeRef == null
                && rackRef == null && tpRef == null && tunnelRef != null) {
            // by tunnel.
            ntLinks = new SiteTunnel(netconfTopology).getOchLinks(topologyRef, tunnelRef);
        } else {
            throw new Exception(
                    "not supported parameter compose.");
        }

        return NetConfConvertors.convertOchLink2OutputLink(ntLinks);
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> getOchLinks(
            GetOchLinkInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        return this.getOchLinks(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }

    @Override
    public PagedList getOchLinksPaged(GetOchLinkPagedInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        List<Link> links = this
                .getOchLinks(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
        PagedList pagedList = new PagedList(links);
        pagedList.setFilter(input.getFilter());
        pagedList.sort(input.getSortInfos());
        return pagedList;
    }

    @Override
    public PageResult<Link> getOchLinksPagedNew(GetOchLinkPagedInput input) throws CommonException {
        RetrieveTopologyDto retrieveTopologyDto = RetrieveElementExtractor.connectionExtract(input);
        PageResult<Link> ochLinkPageResult = getOchLinksPaged(retrieveTopologyDto);
        return ochLinkPageResult;
    }


    @Override
    public PageResult<Link> getSiteLinkPaged(GetSiteLinkPagedInput input) throws CommonException {

        RetrieveTopologyDto retrieveTopologyDto = RetrieveElementExtractor.connectionExtract(input);
        PageResult<Link> linkPageResult = getSiteLinkPaged(retrieveTopologyDto);
        return linkPageResult;
    }


    @Override
    public List<Link> getSiteLink(GetSiteLinkInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        return getSiteLink(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }

    public List<Link> getSiteLink(
            TopologyId topologyRef,
            NodeId nodeRef, String rackRef, String equipRef,
            TpId tpRef,
            LinkId linkRef, String tunnelRef) throws Exception {

        log.debug(
                "start get all site link topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
                topologyRef == null ? "null" : topologyRef.getValue(),
                nodeRef == null ? "null" : nodeRef.getValue(),
                rackRef == null ? "null" : rackRef, equipRef == null ? "null" : equipRef,
                tpRef == null ? "null" : tpRef.getValue(),
                linkRef == null ? "null" : linkRef.getValue(),
                tunnelRef == null ? "null" : tunnelRef);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> ntLinks = new LinkedList<>();

        if (topologyRef == null) {
            throw new Exception("topologyRef is mandatory.");
        } else if (nodeRef == null && rackRef == null && equipRef == null && tpRef == null
                && linkRef == null && tunnelRef == null) {
            // based on topoRef export siteLink
            if (topologyRef.getValue().contains(SITE_TOPO_KEY)) {
//                Topology topo = netconfTopology.getTopology(topologyRef);
//                if (topo == null) {
//                    throw new Exception(
//                            "cannot find required Topology.");
//                }
                ntLinks = netconfTopology.listSiteLink();
            }
        } else if (nodeRef != null && rackRef == null && equipRef == null && tpRef == null) {
            if (topologyRef.getValue().contains(SITE_TOPO_KEY)) {
                ntLinks = new SiteNode(netconfTopology).getSiteLinks(topologyRef, nodeRef);
            } else if (topologyRef.getValue().contains(PHY_TOPO_KEY)) {
                ntLinks = new PhyNe(netconfTopology).getSiteLinks(topologyRef, nodeRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(SITE_TOPO_KEY)
                && nodeRef != null && rackRef != null) {
            ntLinks = new SiteRack(netconfTopology).getSiteLinks(topologyRef, nodeRef, rackRef);
        } else if (topologyRef.getValue().contains(PHY_TOPO_KEY)
                && nodeRef != null && rackRef == null
                && equipRef != null) {
            ntLinks = new PhyEquipment(netconfTopology)
                    .getSiteLinks(topologyRef, nodeRef, equipRef);
        } else if (nodeRef != null && rackRef == null && tpRef != null) {
            if (topologyRef.getValue().equals(SITE_TOPO_KEY)) {
                ntLinks = new SiteTp(netconfTopology).getSiteLinks(topologyRef, nodeRef, tpRef);
            } else if (topologyRef.getValue().equals(PHY_TOPO_KEY)) {
                ntLinks = new PhyTp(netconfTopology).getSiteLinks(topologyRef, nodeRef, tpRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (nodeRef == null && rackRef == null && tpRef == null && linkRef != null) {
            // based on link find out site-link
            if (topologyRef.getValue().equals(PHY_TOPO_KEY)) {
                ntLinks = new PhyLink(netconfTopology).getSiteLinks(topologyRef, linkRef);
            } else if (topologyRef.getValue().equals(SITE_TOPO_KEY)) {
                ntLinks = new SiteLink(netconfTopology).getSiteLinks(topologyRef, linkRef);
            } else if (topologyRef.getValue().equals(OCH_TOPO_KEY)) {
                ntLinks = new OchLink(netconfTopology).getSiteLinks(topologyRef, linkRef);
            } else if (topologyRef.getValue().equals(SITE_VIEW_TOPO_KEY)) {
                ntLinks = new ViewLink(netconfTopology).getSiteLinks(topologyRef, linkRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(SITE_TOPO_KEY) && nodeRef == null
                && rackRef == null && tpRef == null && tunnelRef != null) {
            // based on tunnel find out site-link
            ntLinks = new SiteTunnel(netconfTopology).getSiteLinks(topologyRef, tunnelRef);
        } else {
            throw new Exception(
                    "not supported parameter compose.");
        }

        return NetConfConvertors.convertSiteLink2OutputLink(ntLinks, this.netconfTopology);
    }


    public PagedList getPhyLinkPaged1(GetPhyLinkPagedInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        List<Link> links = getPhyLink(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef,
                tunnelRef);
        PagedList pagedList = new PagedList(links);
        pagedList.setFilter(input.getFilter());
        pagedList.sort(input.getSortInfos());
        return pagedList;
    }


    @Override
    public PageResult<Link> getPhyLinkPaged(GetPhyLinkPagedInput input) {
        RetrieveTopologyDto retrieveTopologyDto = RetrieveElementExtractor.connectionExtract(input);
        INMSRetrieveOperations retrieveOperations = nmsRetrieverOperationsResolver.resolve(
                retrieveTopologyDto);
        PageResult<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> pageLinkResult = retrieveOperations.retrieveAllLinkPaged(
                retrieveTopologyDto);
        PageResult<Link> pageResult = PageResult.<Link>builder()
                .pageNum(pageLinkResult.getPageNum())
                .pageSize(pageLinkResult.getPageSize())
                .total(pageLinkResult.getTotal())
//                .list(NetConfConvertors.convertPhyLink2OutputPhyLink(pageLinkResult.getList(),
//                        netconfTopology))
                .list(nmsOutputConverters.convert2NmsOutput(pageLinkResult.getList()))
                .build();
        return pageResult;
    }


    @Override
    public List<Link> getPhyLink(GetPhyLinkInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        return getPhyLink(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }

    @Override
    public OmsLinkOtsLinkInfoDto getOtsLinkByOMSLink(GetOtsLinksUnderOmsInput input) {
        log.debug("get ots links under oms link id:{}", input.getOmsLinkId());
        if (input.getOmsLinkId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "get ots links under oms,the oms link id should not be null");
        }
        String omsLinkId = input.getOmsLinkId().getValue();
        LinkRole linkRole = input.getLinkRole();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink = netconfTopology.getSiteLink(
                omsLinkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("current site link id:%s is not found", omsLinkId));
        }
        if (linkRole == null) {
            log.debug("current link role is null use default primary role");
            linkRole = LinkRole.Primary;
        }
        Site siteLinkPhysical = siteLink.getAugmentation(
                Link1.class).getSite();
        String omsLinkName = siteLinkPhysical.getFriendlyName();
        DateAndTime activationTime = siteLinkPhysical.getActivationTime();
        //get support tunnel count
//        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> ochLinks = netconfTopology.getOchLinksUnderSiteLinkId(
//                omsLinkId);
        List<String> refOchLinkIds = netconfTopology.listAllOchLinkIdsBySiteLinkIds(
                Collections.singletonList(omsLinkId));
//        long supportTunnelCount = getSupportTunnelCount(omsLinkId, ochLinks);
        SiteLinkRelayTunnelInfo relayTunnelInfo = getSupportTunnelCount(omsLinkId, refOchLinkIds);
        //ots link info
        List<String> otsLinkIds = getSiteLinkRefOtsLinkIdsByLinkRole(siteLink, linkRole);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> otsLinks = netconfTopology.getPhyLinksByIds(
                otsLinkIds);
//        List<Link> links = nmsOutputConverters.convert2NmsOutput(otsLinks);
        LinkedList<WrappedOtsLink> sortedWrappedOtsLink = getSiteLinkOtsLinkSorted(
                siteLinkPhysical.getExplictRoute(), otsLinks, linkRole);
        //ots link information
        List<OtsLinkAmplifierInfo> otsLinkAmplifierInfos = buildOtsLinkAmplifierInfo(
                sortedWrappedOtsLink);
        List<PhyNodeInfo> phyNodeInfos = constructOmsRefPhyNodeInfo(otsLinkAmplifierInfos);
        //routeContractInfo
//        List<OtsLinkAmplifierInfo> sortedOtsLinkAmplifierInfos = sortAmplifierInfoBySortedLinkIds(
//                otsLinkAmplifierInfos, sortedOtsLinkIds);
        OmsLinkPAInfo omsLinkPAInfo = getOmsLinkPAInfoFromOtsLinkAmplifierInfos(
                otsLinkAmplifierInfos);
        RouteContractInfoDto routeContractInfoDto = buildRouteContractInfo(activationTime,
                relayTunnelInfo, otsLinks);
        return OmsLinkOtsLinkInfoDto.builder()
                .omsLinkId(omsLinkId)
                .omsName(omsLinkName)
                .otsLinks(otsLinkAmplifierInfos)
                .paInfo(omsLinkPAInfo)
                .phyNodeInfos(phyNodeInfos)
                .routeContractInfoDto(routeContractInfoDto)
                .build();
    }

    @Override
    public List<RelatedSiteLink> getRelatedSiteLink(GetRelatedSiteLinksInput input) {
        List<String> siteLinkIds = input.getSiteLinkIds();
        log.info("get site link:{} related site links", siteLinkIds);
        if (CollectionUtils.isEmpty(siteLinkIds)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "get related site links,site link ids is null");
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks = netconfTopology.listAllSiteLinkByIds(
                siteLinkIds);
        if (CollectionUtils.isEmpty(siteLinks)) {
            log.error("current site links:{} are not existed", siteLinkIds);
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "get related site link base site link is not existed");
        }
//        List<RelatedSiteLink> basedSiteLinks = siteLinks.stream()
//                .map(siteLink -> {
//                    String siteLinkId = siteLink.getLinkId().getValue();
//                    Site site = siteLink.getAugmentation(Link1.class).getSite();
//                    String friendlyName = site.getFriendlyName();
//                    String subnetId = site.getPlaneId();
//                    String subnetName = site.getPlaneName();
//                    return
//                            new RelatedSiteLinkBuilder().setRelatedSiteLinkId(siteLinkId)
//                                    .setSelectedSiteLinkId(siteLinkId)
//                                    .setSelectedSiteLinkName(friendlyName)
//                                    .setRelatedSiteLinkSubnetId(subnetId)
//                                    .setRelatedSiteLinkSubnet(subnetName)
//                                    .setRelatedSiteLinkName(friendlyName).build();
//                }).collect(Collectors.toList());
//        List<RelatedSiteLink> relatedSiteLinks = new ArrayList<>();
//        relatedSiteLinks.addAll(basedSiteLinks);
        List<RelatedSiteLink> relatedSiteLinkList = getSiteLinksRelativeSiteLinks(siteLinks);

        return relatedSiteLinkList;
    }

    private List<RelatedSiteLink> getSiteLinksRelativeSiteLinks(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks) {
        log.info("get relative site link size:{}", siteLinks.size());
        List<String> siteLinkIds = siteLinks.stream().map(LinkAttributes::getLinkId)
                .map(Uri::getValue).collect(Collectors.toList());
        log.debug("get site link relative site link:{}", siteLinkIds);
        List<RelatedSiteLink> relatedSiteLinkInfos = siteLinkRelationCalculator.getSiteLinkRelation(
                siteLinks);
//        List<SiteLinkGeneralInfo> siteLinkGeneralInfos = siteLinks.stream().map(siteLink -> {
//            String siteLinkId = siteLink.getLinkId().getValue();
//            String sourceSiteId = siteLink.getSource().getSourceNode().getValue();
//            String destSiteId = siteLink.getDestination().getDestNode().getValue();
//            Site site = siteLink.getAugmentation(Link1.class).getSite();
//            String siteLinkName = site.getFriendlyName();
//            String subnetId = site.getPlaneId();
//            String subnetName = site.getPlaneName();
//            return SiteLinkGeneralInfo.builder().siteLinkId(siteLinkId).siteLinkName(siteLinkName)
//                    .sourceSiteId(sourceSiteId)
//                    .destinationSiteId(destSiteId).subnetId(subnetId).subnetName(subnetName)
//                    .build();
//        }).collect(Collectors.toList());
//        Set<RelatedSiteLink> relatedSiteLinkInfos = new HashSet<>();
//        for (SiteLinkGeneralInfo siteLinkGeneralInfo : siteLinkGeneralInfos) {
//            List<RelatedSiteLink> relatedSiteLinks = getSiteLinkRelatedSiteLink(
//                    siteLinkGeneralInfo);
//            relatedSiteLinkInfos.addAll(relatedSiteLinks);
//        }
        return new ArrayList<>(relatedSiteLinkInfos);
    }

    private List<RelatedSiteLink> getSiteLinkRelatedSiteLink(
            SiteLinkGeneralInfo siteLinkGeneralInfo) {
        log.info("get site link related site links,the site link id:{} subnetId:{} subnetName:{}",
                siteLinkGeneralInfo.getSiteLinkId(), siteLinkGeneralInfo.getSubnetId(),
                siteLinkGeneralInfo.getSubnetName());
        String sourceSiteId = siteLinkGeneralInfo.getSourceSiteId();
        String destSiteId = siteLinkGeneralInfo.getDestinationSiteId();
        String subnetId = siteLinkGeneralInfo.getSubnetId();
        String selectedSiteLinkId = siteLinkGeneralInfo.getSiteLinkId();
        String selectedSiteLinkSubnetName = siteLinkGeneralInfo.getSubnetName();
        String selectedSiteLinkSubnetId = siteLinkGeneralInfo.getSubnetId();
        String selectedSiteLinkName = siteLinkGeneralInfo.getSiteLinkName();
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> sourceSiteLinks = netconfTopology.retrieveAllRelatedSiteLinkBySiteAndSubnet(
                sourceSiteId, subnetId);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> destSiteLinks = netconfTopology.retrieveAllRelatedSiteLinkBySiteAndSubnet(
                destSiteId, subnetId);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> totalLinks = new ArrayList<>();
        if (sourceSiteLinks != null && !sourceSiteLinks.isEmpty()) {
            totalLinks.addAll(sourceSiteLinks);
        }
        if (destSiteLinks != null && !destSiteLinks.isEmpty()) {
            totalLinks.addAll(destSiteLinks);
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> distinctLinks = new ArrayList<>(
                totalLinks.stream()
                        .filter(link -> link != null && link.getLinkId() != null)
                        .collect(Collectors.toMap(
                                LinkAttributes::getLinkId,
                                link -> link,
                                (existing, replacement) -> existing
                        ))
                        .values());
        List<RelatedSiteLink> relatedSiteLinks = distinctLinks.stream().map(siteLink -> {
            String siteLinkId = siteLink.getLinkId().getValue();
            Site site = siteLink.getAugmentation(Link1.class).getSite();
            String friendlyName = site.getFriendlyName();
            return new RelatedSiteLinkBuilder().setRelatedSiteLinkId(siteLinkId)
                    .setSelectedSiteLinkId(selectedSiteLinkId)
                    .setSelectedSiteLinkName(selectedSiteLinkName)
                    .setRelatedSiteLinkSubnet(site.getPlaneName())
                    .setRelatedSiteLinkSubnetId(site.getPlaneId())
                    .setRelatedSiteLinkName(friendlyName).build();
        }).collect(
                Collectors.toList());
        return relatedSiteLinks;
    }

    private List<String> getSiteLinkRefOtsLinkIdsByLinkRole(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink,
            LinkRole linkRole) {
        log.debug("get current site link link role:{} ots linkId :{}", siteLink.getLinkId(),
                linkRole);
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        Class<? extends ProtectionType> protectionType = siteLinkAttr.getProtectionType();
        List<String> otsLinkIds = new ArrayList<>();
        Route siteLinkRoute = siteLinkAttr.getExplictRoute().getRoute().get(0);
        if (protectionType.isAssignableFrom(ProtectionUnprotected.class)) {
            otsLinkIds = getUnprotectSiteLinkOtsLinkIds(siteLinkRoute, linkRole);
        } else if (protectionType.isAssignableFrom(ProtectionBidir1To1.class)) {
            otsLinkIds = getProtectionBidr1To1OtsLinkIds(siteLinkRoute, linkRole);
        } else if (protectionType.isAssignableFrom(ProtectionBidir1To2.class)) {
            otsLinkIds = getProtectionBidr1To12OtsLinkIds(siteLinkRoute, linkRole);
        }

        return otsLinkIds;
    }

    private List<String> getProtectionBidr1To12OtsLinkIds(Route siteLinkRoute, LinkRole linkRole) {
        log.debug("get protection bidr 1 to 2 ots link ids");
        List<PathRouteObject> routeObjects = getRouteObjectsByRole(siteLinkRoute, linkRole);
        List<String> refOtsLinkIds = getOtsLinkByRouteObjects(routeObjects);
        return refOtsLinkIds;
    }

    private List<String> getProtectionBidr1To1OtsLinkIds(Route siteLinkRoute, LinkRole linkRole) {
        if (linkRole == LinkRole.Tertiary) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "current protection site link protection type is bidr 1 to 1 not support tertiary link role");
        }
        List<PathRouteObject> routeObjects = getRouteObjectsByRole(siteLinkRoute, linkRole);
        List<String> refOtsLinkIds = getOtsLinkByRouteObjects(routeObjects);
        return refOtsLinkIds;
    }

    private List<String> getUnprotectSiteLinkOtsLinkIds(Route siteLinkRoute, LinkRole linkRole) {
        if (linkRole == LinkRole.Tertiary || linkRole == LinkRole.Secondary) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "current protection site link protection type is unprotected not support secondary and tertiary link role");
        }
        List<PathRouteObject> routeObjects = getRouteObjectsByRole(siteLinkRoute, linkRole);
        List<String> refOtsLinkIds = getOtsLinkByRouteObjects(routeObjects);
        return refOtsLinkIds;
    }

    private List<PathRouteObject> getRouteObjectsByRole(Route siteLinkRoute, LinkRole linkRole) {
        switch (linkRole) {
            case Primary:
                return siteLinkRoute.getPrimary().getExplicitRouteObjects().get(0)
                        .getPathRouteObject();
            case Secondary:
                return siteLinkRoute.getSecondary().getExplicitRouteObjects().get(0)
                        .getPathRouteObject();
            case Tertiary:
                return siteLinkRoute.getThird().get(0).getExplicitRouteObjects().get(0)
                        .getPathRouteObject();
            default:
                throw new IllegalArgumentException("Unsupported link role: " + linkRole);
        }
    }

    private List<String> getOtsLinkByRouteObjects(List<PathRouteObject> routeObjects) {
        log.debug("get ots link ");
        List<String> otsLinkIds = new ArrayList<>();
        for (PathRouteObject routeObject : routeObjects) {
            ResourceType resourceType = routeObject.getResourceType();
            if (resourceType instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                    .tunnel.types.rev180515.resource.type.resource.type.Link) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link linkResource = (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) resourceType;
                String linkId = linkResource.getLinkHop().getLinkRef().getValue();
                if (PhysicalLinkIdNamingRule.isOtsLink(linkId)) {
                    otsLinkIds.add(linkId);
                }
            }
        }
        return otsLinkIds;
    }

    private SiteLinkRelayTunnelInfo getSupportTunnelCount(String siteLinkId,
            List<String> ochLinkIds) {
        log.debug("get support tunnel");
        if (CollectionUtils.isEmpty(ochLinkIds)) {
            log.debug("support tunnel list is empty");
            return SiteLinkRelayTunnelInfo.builder().build();
        }
//        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> ochLinkMap =
//                ochLinks.stream().collect(
//                        Collectors.toMap(ochLink -> ochLink.getLinkId().getValue(),
//                                ochLink -> ochLink));
        List<LinkStateDto> tunnelStateDtos = netconfTopology.getTunnelStateBaseOnOchLinks(
                ochLinkIds);
//                new ArrayList<>(ochLinkMap.keySet()));
        List<LinkStateDto> implementTunnels = tunnelStateDtos.stream()
                .filter(tunnelState -> tunnelState.getImplement().equals(ImplementState.Implement))
                .collect(
                        Collectors.toList());
//        Map<String, List<LinkStateDto>> ochLinkRefImplementTunnelMap = buildOchLinkRefLinkStateDto(
//                implementTunnels);
//        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link>
//                protectedTunnels = getProtectedTunnelMap(ochLinkRefImplementTunnelMap, ochLinkMap);
//        List<Tunnel> tunnels = netconfTopology.getTunnelsBaseOnOchLinks(
//                new ArrayList<>(ochLinkMap.keySet()));
//        List<Tunnel> implementTunnels = tunnels.stream()
//                .filter(tunnel -> tunnel.getImplementState().equals(
//                        ImplementState.Implement)).collect(Collectors.toList());
//        List<Tunnel> protectedTunnels = implementTunnels.stream()
//                .filter(tunnel -> !tunnel.getProtectionType().isAssignableFrom(
//                        ProtectionUnprotected.class)).collect(
//                        Collectors.toList());
//        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> protectTunnelOchMaps = buildImplementTunnelRefOchMap(
//                protectedTunnels,
//                ochLinks);
//        long unprotectedTunnelCount = implementTunnels.size() - protectedTunnels.size();
//        SiteLinkRelayTunnelInfo protectSiteLinkRelayTunnelInfo = getSupportProtectedTunnelCount(
//                siteLinkId, protectedTunnels);
        return SiteLinkRelayTunnelInfo.builder().azTunnelCount(
//                        unprotectedTunnelCount + protectSiteLinkRelayTunnelInfo.azTunnelCount)
                        implementTunnels.size())
                .zaTunnelCount(
//                        unprotectedTunnelCount + protectSiteLinkRelayTunnelInfo.zaTunnelCount)
                        implementTunnels.size())
                .build();
    }

    private Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> getProtectedTunnelMap(
            Map<String, List<LinkStateDto>> ochLinkRefImplementTunnelMap,
            Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> ochLinkMap) {
        List<String> protectedOchLinkIds = ochLinkMap.values().stream()
                .filter(ochLink -> !ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                        .getOch().getProtectionType().isAssignableFrom(ProtectionUnprotected.class))
                .map(ochLink -> ochLink.getLinkId().getValue()).collect(
                        Collectors.toList());
        List<LinkStateDto> protectedTunnelStates = protectedOchLinkIds.stream()
                .flatMap(ochLinkId -> ochLinkRefImplementTunnelMap.get(ochLinkId).stream())
                .collect(Collectors.toList());
        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link>
                tunnelRefOchLinkMap = new HashMap<>();
        for (LinkStateDto protectedTunnel : protectedTunnelStates) {
            //supportLink only have one for tunnel
            String tunnelId = protectedTunnel.getId();
            String supportOchLink = protectedTunnel.getSupportingLink().get(0);
            tunnelRefOchLinkMap.put(tunnelId, ochLinkMap.get(supportOchLink));
        }
        return tunnelRefOchLinkMap;
    }

    private Map<String, List<LinkStateDto>> buildOchLinkRefLinkStateDto(
            List<LinkStateDto> tunnelStateDtos) {
        if (CollectionUtils.isEmpty(tunnelStateDtos)) {
            return Collections.emptyMap();
        }
        Map<String, List<LinkStateDto>> ochLinkRefLinkStateDtoMap = new HashMap<>();
        for (LinkStateDto tunnelStateDto : tunnelStateDtos) {
            List<String> supportLinks = tunnelStateDto.getSupportingLink();
            supportLinks.forEach(
                    supportLink -> ochLinkRefLinkStateDtoMap.computeIfAbsent(supportLink,
                            k -> new ArrayList<>()).add(tunnelStateDto));
        }
        return ochLinkRefLinkStateDtoMap;
    }

    private Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> buildImplementTunnelRefOchMap(
            List<Tunnel> implementTunnels,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> ochLinks) {
        return new HashMap<>();
    }

    private SiteLinkRelayTunnelInfo getSupportProtectedTunnelCount(String siteLinkId,
            Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> protectTunnelOchMaps) {
        log.debug("get SupportProtected tunnel count by siteLinkId:{}", siteLinkId);

        Map<String, ApsPath> tunnelSiteLocationMap = precomputeTunnelLocations(siteLinkId,
                protectTunnelOchMaps);

        Map<String, ApsXCCache> apsXcCacheMap = new HashMap<>();
        Map<String, TunnelCache> tunnelCacheMap = topologyCacheManager.batchGetValues(
                new ArrayList<>(protectTunnelOchMaps.keySet()), TunnelCache.class);
        List<SiteLinkRelayTunnelInfo> siteLinkRelayTunnelInfos = protectTunnelOchMaps.keySet()
                .stream()
                .map(tunnel -> calculateTunnelSupport(tunnel, tunnelSiteLocationMap, tunnelCacheMap,
                        apsXcCacheMap)).collect(Collectors.toList());
        SiteLinkRelayTunnelInfo siteLinkRelayTunnelInfo = siteLinkRelayTunnelInfos.stream()
                .reduce(SiteLinkRelayTunnelInfo.builder()
                                .zaTunnelCount(0)
                                .azTunnelCount(0)
                                .build(),
                        (a, b) -> SiteLinkRelayTunnelInfo.builder()
                                .zaTunnelCount(a.getZaTunnelCount() + b.getZaTunnelCount())
                                .azTunnelCount(a.getAzTunnelCount() + b.getAzTunnelCount())
                                .build());
        return siteLinkRelayTunnelInfo;
    }

    /**
     * 预计算所有隧道的位置信息
     */
    private Map<String, ApsPath> precomputeTunnelLocations(String siteLinkId,
            Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> protectTunnelOchMaps) {
        Map<String, ApsPath> locationMap = new HashMap<>();
        for (String tunnelId : protectTunnelOchMaps.keySet()) {

            ApsPath siteLinkLocation = serviceBearer.getProtectedTunnelSiteLinkLocation(tunnelId,
                    siteLinkId, protectTunnelOchMaps.get(tunnelId));
            locationMap.put(tunnelId, siteLinkLocation);
        }
        return locationMap;
    }

    /**
     * 计算单个隧道的支持值
     */
    private SiteLinkRelayTunnelInfo calculateTunnelSupport(String tunnelId,
            Map<String, ApsPath> locationMap,
            Map<String, TunnelCache> tunnelCacheMap, Map<String, ApsXCCache> cacheMap) {
        ApsPath siteLocation = locationMap.get(tunnelId);

        if (siteLocation == null) {
            return SiteLinkRelayTunnelInfo.builder().build();
        }

        long t0 = System.currentTimeMillis();
        TunnelCache tunnelCache = tunnelCacheMap.get(tunnelId);
        long t1 = System.currentTimeMillis();
        if (t1 - t0 > 1) {
            log.info("[CACHE-TIMING] getValue tunnelCache: {}ms, tunnelId={}", t1 - t0, tunnelId);
        }
        String sourceApsXcId = tunnelCache.getSourceApsXCId();
        String destApsXcId = tunnelCache.getDestinationApsXCId();

        ApsXCCache sourceApsXCCache = cacheMap.computeIfAbsent(sourceApsXcId,
                id -> topologyCacheManager.getValue(id, ApsXCCache.class));
        ApsXCCache destApsXCCache = cacheMap.computeIfAbsent(destApsXcId,
                id -> topologyCacheManager.getValue(id, ApsXCCache.class));
//        //todo:when the ne is not supervision do not calculate
        //sourceApsXCCache => a->z count
        long azCount = sourceApsXCCache.getActivePath() == null ? 0
                : siteLocation.equals(ApsPath.valueOf(sourceApsXCCache.getActivePath())) ? 1 : 0;
        long zaCount = destApsXCCache.getActivePath() == null ? 0
                : siteLocation.equals(ApsPath.valueOf(destApsXCCache.getActivePath())) ? 1 : 0;
//        if (sourceApsXCCache.getActivePath() == null || destApsXCCache.getActivePath() == null) {
//            return 0;
//        }
//        ApsPath sourceActivePath = ApsPath.valueOf(sourceApsXCCache.getActivePath());
//        ApsPath destActivePath = ApsPath.valueOf(destApsXCCache.getActivePath());
//        return (siteLocation.equals(sourceActivePath) || siteLocation.equals(destActivePath)) ? 1
//                : 0;
        return SiteLinkRelayTunnelInfo.builder().zaTunnelCount(zaCount).azTunnelCount(azCount)
                .build();
    }


    private OmsLinkPAInfo getOmsLinkPAInfoFromOtsLinkAmplifierInfos(
            List<OtsLinkAmplifierInfo> otsLinkAmplifierInfos) {
        log.debug("build oms link pa info");
        int size = otsLinkAmplifierInfos.size();
        OtsLinkAmplifierInfo azPa = otsLinkAmplifierInfos.get(size - 1);
        OtsLinkAmplifierInfo zaPa = otsLinkAmplifierInfos.get(0);
        return OmsLinkPAInfo.builder().azPowerAmplifier(azPa).zaPowerAmplifier(zaPa).build();
    }

    private LinkedList<WrappedOtsLink> getSiteLinkOtsLinkSorted(ExplictRoute explictRoute,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> links,
            LinkRole linkRole) {
        log.debug("get ots link sorted by site link route");
        List<RouteSegment> routeSegments = extractRouteSegments(explictRoute, linkRole);
        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> linkById = links.stream()
                .collect(
                        Collectors.toMap(link -> link.getLinkId().getValue(), Function.identity()));
        LinkedList<WrappedOtsLink> result = new LinkedList<>();

        for (RouteSegment seg : routeSegments) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link = linkById.get(
                    seg.getLinkId());
            if (link == null) {
                log.warn("route link [{}] cannot found in the link list", seg.getLinkId());
                continue;
            }

            boolean sameDirection =
                    link.getSource().getSourceTp().getValue().equals(seg.getSourceTp()) &&
                            link.getDestination().getDestTp().getValue().equals(seg.getDestTp());

            // 如果是反向，则 sameDirection=false，可以在包装类中存方向标记
            result.add(WrappedOtsLink.builder().link(link).aligned(sameDirection).build());
        }

        return result;
    }


    /**
     * 提取路由段信息：srcTp、linkId、dstTp
     */
    private List<RouteSegment> extractRouteSegments(ExplictRoute explictRoute, LinkRole linkRole) {
        List<RouteSegment> segments = new ArrayList<>();
        Route route = explictRoute.getRoute().get(0);
        List<PathRouteObject> objs = getRouteObjectsByRole(route, linkRole);

        String prevTp = null;
        for (PathRouteObject obj : objs) {
            ResourceType rt = obj.getResourceType();
            if (rt instanceof Tp) {
                prevTp = ((Tp) rt).getTpHop().getTpRef().getValue();
            } else if (rt instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                    .tunnel.types.rev180515.resource.type.resource.type.Link) {

                String linkId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                        .tunnel.types.rev180515.resource.type.resource.type.Link) rt)
                        .getLinkHop().getLinkRef().getValue();

                if (!PhysicalLinkIdNamingRule.isOtsLink(linkId)) {
                    continue;
                }

                // 找下一个 TP
                int idx = objs.indexOf(obj);
                String nextTp = null;
                if (idx + 1 < objs.size()) {
                    ResourceType nextRt = objs.get(idx + 1).getResourceType();
                    if (nextRt instanceof Tp) {
                        nextTp = ((Tp) nextRt).getTpHop().getTpRef().getValue();
                    }
                }

                if (prevTp != null && nextTp != null) {
                    segments.add(
                            RouteSegment.builder().sourceTp(prevTp).linkId(linkId).destTp(nextTp)
                                    .build());
                }
                prevTp = nextTp; // 更新
            }
        }
        return segments;
    }

    /**
     * construct oms ref phy node info
     *
     * @param otsLinkAmplifierInfos
     * @return
     */
    private List<PhyNodeInfo> constructOmsRefPhyNodeInfo(
            List<OtsLinkAmplifierInfo> otsLinkAmplifierInfos) {
        log.debug("build phy node info");
        return otsLinkAmplifierInfos.stream()
                .flatMap(otsLinkAmplifierInfo -> {
                    OtsTerminalInfo source = otsLinkAmplifierInfo.getSource();
                    OtsTerminalInfo destination = otsLinkAmplifierInfo.getDestination();
                    PhyNodeInfo sourceNode = PhyNodeInfo.builder()
                            .ip(source.getNodeIp())
                            .name(source.getNodeName())
                            .nodeId(source.getNodeId())
                            .build();
                    PhyNodeInfo destNode = PhyNodeInfo.builder()
                            .ip(destination.getNodeIp())
                            .name(destination.getNodeName())
                            .nodeId(destination.getNodeId())
                            .build();
                    return Stream.of(sourceNode, destNode);
                }).distinct().collect(Collectors.toList());
    }

    /**
     * get ots link amplifierXc info
     *
     * @param links
     * @return
     */
    private List<OtsLinkAmplifierInfo> buildOtsLinkAmplifierInfo(List<WrappedOtsLink> links) {
        log.debug("get ots link amplifierXc info");
        //preload
        OtsLinkAmplifierRefCache refCache = preloadRefCache(links);
        List<OtsLinkAmplifierInfo> amplifierInfos = links.stream()
                .map(link -> getOtsLinkAmplifierInfo(link, refCache)).collect(Collectors.toList());
        return amplifierInfos;
    }

    /**
     * pre load
     *
     * @param links
     * @return
     */
    private OtsLinkAmplifierRefCache preloadRefCache(List<WrappedOtsLink> links) {
        log.debug("preload the ref cache for ots link");
        Set<String> tpIds = new HashSet<>();
        for (WrappedOtsLink w : links) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link otsLink = w.getLink();
            tpIds.add(otsLink.getSource().getSourceTp().getValue());
            tpIds.add(otsLink.getDestination().getDestTp().getValue());
        }
        Set<String> neIds = tpIds.stream()
                .map(PhysicalTpIdNamingRule::getNodeId)
                .collect(Collectors.toSet());
        Set<String> siteIds = tpIds.stream().map(PhysicalTpIdNamingRule::getSiteId)
                .collect(Collectors.toSet());
        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> phyNodeMap = netconfTopology.batchGetNeNodes(
                new ArrayList<>(neIds));
        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> siteNodeMap = netconfTopology.batchGetSiteNodesLight(
                new ArrayList<>(siteIds));
        return OtsLinkAmplifierRefCache.builder().phyNodeMap(phyNodeMap)
                .phyNodeSiteNodeMap(siteNodeMap).build();
    }

    /**
     * ots link amplifierXc info like bac bal
     *
     * @param otsLinkWrapped
     * @return
     */
    private OtsLinkAmplifierInfo getOtsLinkAmplifierInfo(WrappedOtsLink otsLinkWrapped,
            OtsLinkAmplifierRefCache refCache) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link otsLink = otsLinkWrapped.getLink();
        boolean isAlign = otsLinkWrapped.getAligned();
        String otsLinkId = otsLink.getLinkId().getValue();
        log.debug("get ots link amplifierXc info for the ots link:{}", otsLinkId);
        Physical otsLinkPhysical = otsLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                .getPhysical();
        String otsLinkName = otsLinkPhysical.getFriendlyName();
        OtsLinkTerminationPointInfo otsLinkTerminationPointInfo = amplifierEndpointResolver.resolveOtsLinkEndpoint(
                otsLinkId, refCache);
        String sourceTpId = otsLinkTerminationPointInfo.getEdfaSourceTpId();
        String destTpId = otsLinkTerminationPointInfo.getEdfaDestTpId();
        String sourceRamanTpId = otsLinkTerminationPointInfo.getRamanSourceTpId();
        String destRamanTpId = otsLinkTerminationPointInfo.getRamanDestTpId();

        OtsLinkAmplifierCLInfo azBACLAmplifier = amplifierHandler.getOtsLinkSourceBACLInfoByTp(
                sourceTpId, refCache);
        OtsLinkAmplifierCLInfo zaBACLAmplifier = amplifierHandler.getOtsLinkDestBACLInfoByTp(
                destTpId, refCache);
        OtsLinkAmplifierCLInfo azPACLAmplifier = amplifierHandler.getOtsLinkSourcePACLInfoByTp(
                destTpId, refCache);
        OtsLinkAmplifierCLInfo zaPACLAmplifier = amplifierHandler.getOtsLinkDestPACLInfoByTp(
                sourceTpId, refCache);
        //build raman amplifier
        OtsLinkRamanInfo azRamanAmplifier = amplifierHandler.getOtsLinkRamanInfoByTp(
                sourceRamanTpId, refCache);
        OtsLinkRamanInfo zaRamanAmplifier = amplifierHandler.getOtsLinkRamanInfoByTp(
                destRamanTpId, refCache);

        return OtsLinkAmplifierInfo.builder().otsLinkName(otsLinkName)
                .isAlign(isAlign)
                .otsLinkId(otsLinkId)
                .source(OtsTerminalInfo.builder().tpId(azBACLAmplifier.getTpId())
                        .tpName(azBACLAmplifier.getTpName())
                        .nodeId(azBACLAmplifier.getNodeId())
                        .nodeName(azBACLAmplifier.getNeName())
                        .nodeIp(azBACLAmplifier.getIp())
                        .siteId(azBACLAmplifier.getSiteId())
                        .siteName(azBACLAmplifier.getSiteName())
                        .build())
                .destination(OtsTerminalInfo.builder().tpId(zaBACLAmplifier.getTpId())
                        .tpName(zaBACLAmplifier.getTpName())
                        .nodeId(zaBACLAmplifier.getNodeId())
                        .nodeName(zaBACLAmplifier.getNeName())
                        .nodeIp(zaBACLAmplifier.getIp())
                        .siteId(zaBACLAmplifier.getSiteId())
                        .siteName(zaBACLAmplifier.getSiteName())
                        .build())
                .azC(azBACLAmplifier.getAmplifierC())
                .azL(azBACLAmplifier.getAmplifierL() != null ? azBACLAmplifier.getAmplifierL()
                        : null)
                .zaC(zaBACLAmplifier.getAmplifierC())
                .zaL(zaBACLAmplifier.getAmplifierL() != null ? zaBACLAmplifier.getAmplifierL()
                        : null)
                .azPaC(azPACLAmplifier != null ? azPACLAmplifier.getAmplifierC() : null)
                .azPaL(azPACLAmplifier != null ? azPACLAmplifier.getAmplifierL() : null)
                .zaPaC(zaPACLAmplifier != null ? zaPACLAmplifier.getAmplifierC() : null)
                .zaPaL(zaPACLAmplifier != null ? zaPACLAmplifier.getAmplifierL() : null)
                .azRaman(azRamanAmplifier != null ? azRamanAmplifier : null)
                .zaRaman(zaRamanAmplifier != null ? zaRamanAmplifier : null)
                .build();
    }


    /**
     * @param activationTime
     * @param relayTunnelInfo
     * @param otsLinks
     * @return
     */
    private RouteContractInfoDto buildRouteContractInfo(DateAndTime activationTime,
            SiteLinkRelayTunnelInfo relayTunnelInfo,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> otsLinks) {
        log.debug("build route contract info");
        Set<String> fiberProviderNames = new HashSet<>();
        BigDecimal aTozRouteLength = BigDecimal.ZERO;
        BigDecimal aTozDelay = BigDecimal.ZERO;
        BigDecimal zToaDelay = BigDecimal.ZERO;
        BigDecimal zToaRouteLength = BigDecimal.ZERO;
        BigDecimal aTozContractLength = BigDecimal.ZERO;
        BigDecimal zToaContractLength = BigDecimal.ZERO;
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link otsLink : otsLinks) {
            Physical otsLinkPhysical = otsLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                    .getPhysical();
            Provider linkProvider = otsLinkPhysical.getProvider();

            String fiberProviderName = linkProvider.getVendorName();
            if (StringUtils.hasText(fiberProviderName)) {
                fiberProviderNames.add(fiberProviderName);
            }
            aTozDelay = aTozDelay.add(linkProvider.getDelayAz() == null ? BigDecimal.ZERO
                    : linkProvider.getDelayAz());
            zToaDelay = zToaDelay.add(linkProvider.getDelayZa() == null ? BigDecimal.ZERO
                    : linkProvider.getDelayZa());
            aTozContractLength =
                    linkProvider.getDistanceAz() != null ? aTozContractLength.add(
                            linkProvider.getDistanceAz()) : aTozContractLength;
            zToaContractLength =
                    linkProvider.getDistanceZa() != null ? zToaContractLength.add(
                            linkProvider.getDistanceZa()) : zToaContractLength;
            //todo: length calculator no

        }
        DirectionMetrics aToz = DirectionMetrics.builder().contractLength(aTozContractLength)
                .routeLength(aTozRouteLength).routeDelay(aTozDelay)
                .serviceCount(relayTunnelInfo.azTunnelCount).build();
        DirectionMetrics zToa = DirectionMetrics.builder().contractLength(zToaContractLength)
                .routeLength(zToaRouteLength).routeDelay(zToaDelay)
                .serviceCount(relayTunnelInfo.zaTunnelCount).build();
        String providerName = getProviderName(fiberProviderNames);
        return RouteContractInfoDto.builder()
                .provider(providerName)
                .openDate(activationTime)
                .aToz(aToz)
                .zToa(zToa)
                .build();
    }

    private String getProviderName(Set<String> fiberProviderNames) {
        log.debug("generate the total provider name");
        StringBuilder sb = new StringBuilder();
        for (String provider : fiberProviderNames) {
            if (sb.length() > 0) {
                sb.append(VERTICAL_LINE);
            }
            sb.append(provider);
        }
        return sb.toString();
    }

    private PageResult<Link> getSiteLinkPaged(
            RetrieveTopologyDto retrieveDto) {
        log.info("start to get phy link paged pageNum:{},pageSize:{}", retrieveDto.getPageNum(),
                retrieveDto.getPageSize());
        INMSRetrieveOperations operations = nmsRetrieverOperationsResolver.resolve(retrieveDto);
        PageResult<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> pageResult = operations.retrieveAllSiteLinkPaged(
                retrieveDto);
        PageResult<Link> linkResult = new PageResult<>();
        linkResult.setList(nmsOutputConverters.convert2NmsOutput(pageResult.getList()));
        linkResult.setTotal(pageResult.getTotal());
        linkResult.setPageNum(pageResult.getPageNum());
        linkResult.setPages(pageResult.getPages());
        linkResult.setPageSize(pageResult.getPageSize());
        return linkResult;
    }

    private List<Link> getPhyLink(
            TopologyId topologyRef, NodeId nodeRef, String rackRef, String equipRef,
            TpId tpRef,
            LinkId linkRef, String tunnelRef) throws Exception {

        log.debug(
                "start get all phy link topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
                topologyRef == null ? "null" : topologyRef.getValue(),
                nodeRef == null ? "null" : nodeRef.getValue(),
                rackRef == null ? "null" : rackRef, equipRef == null ? "null" : equipRef,
                tpRef == null ? "null" : tpRef.getValue(),
                linkRef == null ? "null" : linkRef.getValue(),
                tunnelRef == null ? "null" : tunnelRef);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> ntLinks = null;

        if (topologyRef == null) {
            throw new Exception("topologyRef is mandatory.");
        } else if (topologyRef.getValue().contains(Constants.PHY_TOPO_KEY)
                && nodeRef == null && rackRef == null
                && equipRef == null && tpRef == null && linkRef == null && tunnelRef == null) {
//            Topology topo = netconfTopology.getTopology(topologyRef);
//            if (topo == null) {
//                throw new Exception(
//                        "cannot find required Topology.");
//            }
            ntLinks = netconfTopology.listPhyLink();
        } else if (nodeRef != null && rackRef == null && equipRef == null && tpRef == null) {
            if (topologyRef.getValue().contains(SITE_TOPO_KEY)) {
                ntLinks = new SiteNode(netconfTopology).getPhyLinks(topologyRef, nodeRef);
            } else if (topologyRef.getValue().contains(Constants.PHY_TOPO_KEY)) {
                ntLinks = new PhyNe(netconfTopology).getPhyLinks(topologyRef, nodeRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(SITE_TOPO_KEY)
                && nodeRef != null && rackRef != null) {
            ntLinks = new SiteRack(netconfTopology).getPhyLinks(topologyRef, nodeRef, rackRef);
        } else if (topologyRef.getValue().contains(PHY_TOPO_KEY)
                && nodeRef != null && rackRef == null
                && equipRef != null) {
            ntLinks = new PhyEquipment(netconfTopology).getPhyLinks(topologyRef, nodeRef, equipRef);
        } else if (nodeRef != null && rackRef == null && tpRef != null) {
            if (topologyRef.getValue().equals(SITE_TOPO_KEY)) {
                ntLinks = new SiteTp(netconfTopology).getPhyLinks(topologyRef, nodeRef, tpRef);
            } else if (topologyRef.getValue().equals(PHY_TOPO_KEY)) {
                ntLinks = new PhyTp(netconfTopology).getPhyLinks(topologyRef, nodeRef, tpRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (nodeRef == null && rackRef == null && tpRef == null && linkRef != null) {
            if (topologyRef.getValue().contains(SITE_TOPO_KEY)) {
                ntLinks = new SiteLink(netconfTopology).getPhyLinks(topologyRef, linkRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(SITE_TOPO_KEY) && nodeRef == null
                && rackRef == null && tpRef == null && tunnelRef != null) {
            ntLinks = new SiteTunnel(netconfTopology).getPhyLinks(topologyRef, tunnelRef);
        } else {
            throw new Exception(
                    "not supported parameter compose.");
        }

        return NetConfConvertors.convertPhyLink2OutputPhyLink(ntLinks, netconfTopology);
    }

    private PageResult<Link> getOchLinksPaged(RetrieveTopologyDto retrieveDto) {
        log.info("start to get phy link paged pageNum:{},pageSize:{}", retrieveDto.getPageNum(),
                retrieveDto.getPageSize());
        INMSRetrieveOperations operations = nmsRetrieverOperationsResolver.resolve(retrieveDto);
        PageResult<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> pageResult = operations.retrieveAllOchLinkPaged(
                retrieveDto);
        PageResult<Link> linkResult = new PageResult<>();
        linkResult.setList(NetConfConvertors.convertOchLink2OutputLink(pageResult.getList()));
        linkResult.setTotal(pageResult.getTotal());
        linkResult.setPageNum(pageResult.getPageNum());
        linkResult.setPages(pageResult.getPages());
        linkResult.setPageSize(pageResult.getPageSize());
        return linkResult;
    }

    @Data
    @Builder
    private static class SiteLinkRelayTunnelInfo implements Serializable {

        private long zaTunnelCount;

        private long azTunnelCount;
    }


}
