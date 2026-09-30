/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.simple.route.object.resource.type.link.LinkHop;
import org.opendaylight.yang.gen.v1.http.nokia.com.cd.otc.policies.rev190319.route.restriction.ExcludeOtsLinks;
import org.opendaylight.yang.gen.v1.http.nokia.com.cd.otc.policies.rev190319.route.restriction.ExcludeOtsLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2InputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetSpcBackupRoutesInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetSpcBackupRoutesOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetSpcBackupRoutesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ReusedRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ReusedRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ShortPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ShortPathBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RiskGroupInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RouteRestriction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RouteRestrictionBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRateBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TunnelSpcManager {

    @Autowired
    private TunnelDao tunnelDao;
    @Autowired
    private TunnelComputer2 tunnelComputer2;
    @Autowired
    private OchLinkDao ochLinkDao;
    @Autowired
    private SiteNodeDao siteNodeDao;


    public GetSpcBackupRoutesOutput doIt(GetSpcBackupRoutesInput input) {
        String tunnelId = input.getTunnelId();
        Tunnel tunnel = getTunnel(tunnelId);
        Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnelId);
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        List<String> sitesInOrder = getSitesInOrder(och);
        String startSiteId = sitesInOrder.get(1);
        String endSiteId = sitesInOrder.get(sitesInOrder.size() - 2);

        Site startSite = siteNodeDao.getSiteNodeAttributeSite(startSiteId);
        List<SiteLinkRelation> startSiteLinkRelation = startSite.getSiteLinkRelation();
        if (startSiteLinkRelation == null || startSiteLinkRelation.isEmpty()) {
            log.error("Invalid start site:%s,%s, because no siteLink relation", startSiteId, startSite.getFriendlyName());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Site[%s] has no siteLink relation", startSite.getFriendlyName()));
        }

        Site endSite = siteNodeDao.getSiteNodeAttributeSite(endSiteId);
        List<SiteLinkRelation> endSiteLinkRelation = endSite.getSiteLinkRelation();
        if (endSiteLinkRelation == null || endSiteLinkRelation.isEmpty()) {
            log.error("Invalid start site:%s,%s, because no siteLink relation", endSiteId, endSite.getFriendlyName());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Site[%s] has no siteLink relation", endSite.getFriendlyName()));
        }

        List<ExcludeOtsLinks> primaryExcludeLinks = getPrimaryExcludeLinks(och);
        List<ExcludeOtsLinks> secondaryExcludeLinks = getSecondaryExcludeLinks(och);
        ComputeTunnels2Input computeTunnels2Input = constructComputeTunnelInput(tunnel, och, primaryExcludeLinks, secondaryExcludeLinks, startSiteId, endSiteId);

        ComputeTunnels2Output computeTunnelOutput = tunnelComputer2.doIt(computeTunnels2Input);
//        Set<String> tunnelSiteLinkIds = tunnel.getSupportingLink().stream().map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(Collectors.toSet());

        String primaryMandatorySiteLinkStart = primaryExcludeLinks.get(0).getLinkId();
        String primaryMandatorySiteLinkEnd = primaryExcludeLinks.get(primaryExcludeLinks.size() - 1).getLinkId();
        String secondaryMandatorySiteLinkStart = secondaryExcludeLinks == null ? null : secondaryExcludeLinks.get(0).getLinkId();
        String secondaryMandatorySiteLinkEnd = secondaryExcludeLinks == null ? null : secondaryExcludeLinks.get(primaryExcludeLinks.size() - 1).getLinkId();

        //filter routes

        List<ReusedRoute> reusedRoutes = computeTunnelOutput.getReusedRoute();
        List<ReusedRoute> reusedRouteSpc = new ArrayList<>();
        for (ReusedRoute reusedRoute : reusedRoutes) {
            List<String> primary = reusedRoute.getSiteLinkRoute().getPrimary();
            String wssLinkStart = getWssLink(startSiteLinkRelation, primaryMandatorySiteLinkStart, primary.get(0));
            if (wssLinkStart == null) {
                continue;
            }
            String wssLinkEnd = getWssLink(endSiteLinkRelation, primaryMandatorySiteLinkStart, primary.get(primary.size() - 1));
            if (wssLinkEnd == null) {
                continue;
            }
            List<String> primaryUpdate = getUpdatedPrimary(primaryMandatorySiteLinkStart, wssLinkStart, wssLinkStart, wssLinkEnd, primary);
            ReusedRoute reusedRouteUpdated = new ReusedRouteBuilder().setSiteLinkRoute(new SiteLinkRouteBuilder(reusedRoute.getSiteLinkRoute()).setPrimary(primaryUpdate).build()).build();
            reusedRouteSpc.add(reusedRouteUpdated);
        }
        List<ShortPath> shortPaths = computeTunnelOutput.getShortPath();
        List<ShortPath> shortPathSpc = new ArrayList<>();
        for (ShortPath shortPath : shortPaths) {
            List<String> primary = shortPath.getSiteLinkRoute().getPrimary();
            String wssLinkStart = getWssLink(startSiteLinkRelation, primaryMandatorySiteLinkStart,primary.get(0));
            if (wssLinkStart == null) {
                continue;
            }
            String wssLinkEnd = getWssLink(endSiteLinkRelation,primaryMandatorySiteLinkEnd,primary.get(primary.size() - 1));
            if (wssLinkEnd == null) {
                continue;
            }
            List<String> primaryUpdate = getUpdatedPrimary(primaryMandatorySiteLinkStart, wssLinkStart, primaryMandatorySiteLinkEnd, wssLinkEnd, primary);
            ShortPath shortPathUpdated = new ShortPathBuilder().setSiteLinkRoute(new SiteLinkRouteBuilder(shortPath.getSiteLinkRoute()).setPrimary(primaryUpdate).build()).build();
            shortPathSpc.add(shortPathUpdated);
        }

        return new GetSpcBackupRoutesOutputBuilder()
                .setReusedRoute(reusedRouteSpc)
                .setShortPath(shortPathSpc)
                .setReturnCode(RpcResultType.Success).build();

    }

    private List<String> getUpdatedPrimary(String primaryMandatorySiteLinkStart, String wssLinkStart, String primaryMandatorySiteLinkEnd, String wssLinkEnd, List<String> primary) {
        List<String>  updatePrimary=new ArrayList<>(primary);
        updatePrimary.add(0, wssLinkStart);
        updatePrimary.add(0, primaryMandatorySiteLinkStart);
        updatePrimary.add(wssLinkEnd);
        updatePrimary.add(primaryMandatorySiteLinkEnd);
        return updatePrimary;
    }

    private String getWssLink(List<SiteLinkRelation> siteLinkRelations, String primaryMandatorySiteLinkStart, String siteLink) {
        for (SiteLinkRelation siteLinkRelation : siteLinkRelations) {
            if (siteLinkRelation.getLinkaId().equals(primaryMandatorySiteLinkStart) && siteLinkRelation.getLinkzId().equals(siteLink)) {
                return siteLinkRelation.getWssLinkIdBetweenAZ();
            }
            if (siteLinkRelation.getLinkzId().equals(primaryMandatorySiteLinkStart) && siteLinkRelation.getLinkaId().equals(siteLink)) {
                return siteLinkRelation.getWssLinkIdBetweenAZ();
            }
        }
        return null;
    }

    private List<String> getSitesInOrder(Och och) {
        List<PathRouteObject> pathRouteObjects = och.getExplictRoute().getRoute().get(0).getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject();
        return pathRouteObjects.stream().
                filter(pathRouteObject -> pathRouteObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp)
                .map(
                        pathRouteObject -> ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) pathRouteObject.getResourceType()).getTpHop()
                                .getSiteRef().getValue()
                )
                .distinct()
                .collect(Collectors.toList());

    }

    private ComputeTunnels2Input constructComputeTunnelInput(Tunnel tunnel, Och och, List<ExcludeOtsLinks> primaryExcludeLinks, List<ExcludeOtsLinks> secondaryExcludeLinks, String startSite,
            String endSite) {
        ComputeTunnels2InputBuilder computeTunnels2InputBuilder = new ComputeTunnels2InputBuilder();
//        computeTunnels2InputBuilder.setSrcSite(new NodeId(PhysicalTpIdNamingRule.getSiteId(tunnel.getSourceTp().get(0).getTpRef().getValue())));
//        computeTunnels2InputBuilder.setDstSite(new NodeId(PhysicalTpIdNamingRule.getSiteId(tunnel.getDestinationTp().get(0).getTpRef().getValue())));
        computeTunnels2InputBuilder.setCustomer(tunnel.getCustomer());
        computeTunnels2InputBuilder.setOrderId(tunnel.getOrderId().get(0));
        computeTunnels2InputBuilder.setSignalRate(tunnel.getSignalRate());
        computeTunnels2InputBuilder.setRiskGroupInfo(Arrays.asList(
                new RiskGroupInfoBuilder()
                        .setRiskGroupName(tunnel.getRiskGroupName()).
                        setPlaneName(tunnel.getPlaneName()).build()));
        computeTunnels2InputBuilder.setRoutingPolicyId(tunnel.getRoutingPolicyId());

        computeTunnels2InputBuilder.setCardType(och.getCardType());
        computeTunnels2InputBuilder.setBundleNumber(1);
        computeTunnels2InputBuilder.setVendorOccupationRate(Arrays.asList(
                new VendorOccupationRateBuilder()
                        .setNodeType(NodeType.TD)
                        .setNumber(1)
                        .setProductType(och.getProductType())
                        .setVendorName(och.getVendorName())
                        .build()));
        computeTunnels2InputBuilder.setClientPhysicalMedium(tunnel.getClientPhysicalMedium());
        computeTunnels2InputBuilder.setLineSignalRate(TunnelUtil.getLineSignal(och.getOdukType()));
        computeTunnels2InputBuilder.setIsReusedTpc(true);//todo:temp set
        computeTunnels2InputBuilder.setIsReusedMixed(false);//todo:temp set

        computeTunnels2InputBuilder.setSrcSite(new NodeId(PhysicalTpIdNamingRule.getSiteId(startSite)));
        computeTunnels2InputBuilder.setDstSite(new NodeId(PhysicalTpIdNamingRule.getSiteId(endSite)));

//        String firstSiteLink = primaryExcludeLinks.get(0).getLinkId();
//        List<ExcludeOtsLinks> excludeSiteLinks = new ArrayList<>(primaryExcludeLinks);
//        excludeSiteLinks.remove(0);
//        excludeSiteLinks.remove(excludeSiteLinks.size() - 1);
//        if (secondaryExcludeLinks != null) {
//            excludeSiteLinks.addAll(secondaryExcludeLinks.subList(1, secondaryExcludeLinks.size() - 2));
//        }

        RouteRestriction rr = new RouteRestrictionBuilder()
          .setIndex(0)
          .setExcludeOtsLinks(primaryExcludeLinks)
          .build();
        List<RouteRestriction> rrList = new ArrayList<>();
        rrList.add(rr);
        computeTunnels2InputBuilder.setRouteRestriction(rrList);
//        computeTunnels2InputBuilder.setMandatorySiteLink(Arrays.asList(new MandatorySiteLinkBuilder().setLinkId(firstSiteLink).build()));
        return computeTunnels2InputBuilder.build();
    }

    private List<ExcludeOtsLinks> getPrimaryExcludeLinks(Och och) {
        List<PathRouteObject> pathRouteObjects = och.getExplictRoute().getRoute().get(0).getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject();
        return pathRouteObjects.stream().
                filter(pathRouteObject -> pathRouteObject.getTopologyRef().getValue().equals(Constant.SITE_TOPOID)).map(
                        pathRouteObject -> new ExcludeOtsLinksBuilder().setLinkId(
                                ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pathRouteObject.getResourceType()).getLinkHop()
                                        .getLinkRef().getValue()).build())
                .collect(Collectors.toList());
    }

    private List<ExcludeOtsLinks> getSecondaryExcludeLinks(Och och) {
        if (och.getExplictRoute().getRoute().get(0).getSecondary() == null) {
            return null;
        }
        return och.getExplictRoute().getRoute().get(0).getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject().stream().
                filter(pathRouteObject -> pathRouteObject.getTopologyRef().equals(Constant.SITE_TOPOID)).map(
                        pathRouteObject -> new ExcludeOtsLinksBuilder().setLinkId(((LinkHop) pathRouteObject.getResourceType()).getLinkId().getValue()).build())
                .collect(Collectors.toList());
    }


    private Tunnel getTunnel(String tunnelId) {
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (tunnel == null) {
            log.error("Invalid data. cannot find tunnel {} from db.", tunnelId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Invalid tunnel Id.");

        }
//        Class<? extends ProtectionType> protectionType = tunnel.getProtectionType();
//        if (protectionType == null || !protectionType.getName().equals(SPC.class.getName())) {
//            log.error("Invalid tunnel, only SPC supported, but has:{}", protectionType);
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Invalid tunnel, only SPC supported.");
//        }
        return tunnel;
    }
}
