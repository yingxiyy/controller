/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.allocate.common.util.Constant;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RegSiteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TunnelUtilService {

    public static final String REG = "REG";
    @Autowired
    private OchLinkDao ochLinkDao;

    @Autowired
    private TunnelDao tunnelDao;

    @Autowired
    private NeDesigner neDesigner;

    public Pair<String, WDM_Band> getOpModeAndWdm(String vendorName, String productType, GridType frequenceWidth, String cardType, SERVICETYPE servicetype) throws NeDesignerException {
        Card card = neDesigner.getNeInfo(vendorName, productType, NodeType.TD.name())
                .getCardByCardType(cardType);
        return new ImmutablePair<String, WDM_Band>(NeInfoUtil.getOpMode(card, servicetype, frequenceWidth.getIntValue()), WDM_Band.fromString(card.getWdmBand()));
    }

    /**
     * 这里先不过滤REG，因为主要用来compute判断可用带宽的
     *
     * @param siteLinkRoute
     * @param param
     * @param opMode
     * @return
     */
    public List<Link> getReusedOchList_IgnoreReg(SiteLinkRoute siteLinkRoute, ParamCreaionBasic param, String opMode) {
        List<String> mandatoryLinks = new ArrayList<>();
        mandatoryLinks.addAll(siteLinkRoute.getPrimary());
        if (siteLinkRoute.getSecondary() != null && !siteLinkRoute.getSecondary().isEmpty()) {
            mandatoryLinks.addAll(siteLinkRoute.getSecondary());
        }
        if (siteLinkRoute.getThird() != null && !siteLinkRoute.getThird().isEmpty()) {
            mandatoryLinks.addAll(siteLinkRoute.getThird());
        }

        List<Link> reusedOch = ochLinkDao.filter(param.getSrcSite(), param.getDesSite(), mandatoryLinks, param.getProtectionType(), param.getRiskGroupName(), param
                .getPlaneName(), param.getOchOduGranularity(), param.getCardType(), param.getServiceType(), opMode);
        return reusedOch;
    }

    public List<Link> getReusedOchList(SiteLinkRoute siteLinkRoute, ParamCreaionBasic param, String opMode) {
        List<String> mandatoryLinks = new ArrayList<>();
        mandatoryLinks.addAll(siteLinkRoute.getPrimary());
        if (siteLinkRoute.getSecondary() != null && !siteLinkRoute.getSecondary().isEmpty()) {
            mandatoryLinks.addAll(siteLinkRoute.getSecondary());
        }
        if (siteLinkRoute.getThird() != null && !siteLinkRoute.getThird().isEmpty()) {
            mandatoryLinks.addAll(siteLinkRoute.getThird());
        }

        List<Link> reusedOchs = ochLinkDao.filter(param.getSrcSite(), param.getDesSite(), mandatoryLinks, param.getProtectionType(), param.getRiskGroupName(), param
                .getPlaneName(), param.getOchOduGranularity(), param.getCardType(), param.getServiceType(), opMode);

        Map<String, String> primarySiteTypes = getSiteTypes(siteLinkRoute.getPrimaryReg());
        Map<String, String> secondarySiteTypes = getSiteTypes(siteLinkRoute.getSecondaryReg());
        Map<String, String> thirdSiteTypes = getSiteTypes(siteLinkRoute.getThirdReg());

        List<Link> reusedOchReg = new ArrayList<>();

        for (Link reusedOch : reusedOchs) {
            Och och = reusedOch.getAugmentation(Link1.class).getOch();
            if (och.getExplictRoute() == null || och.getExplictRoute().getRoute() == null || och.getExplictRoute().getRoute().isEmpty()) {
                continue;
            }

            boolean routeMatched = true;
            for (Route ochRoute : och.getExplictRoute().getRoute()) {
                if (!matchOchRoute(ochRoute, siteLinkRoute, primarySiteTypes, secondarySiteTypes, thirdSiteTypes)) {
                    routeMatched = false;
                    break;
                }
            }
            if (!routeMatched) {
                continue;
            }

            reusedOchReg.add(reusedOch);

        }

        return reusedOchReg;
    }

    private boolean matchOchRoute(Route ochRoute, SiteLinkRoute siteLinkRoute, Map<String, String> primarySiteTypes,
                                  Map<String, String> secondarySiteTypes, Map<String, String> thirdSiteTypes) {
        if (ochRoute == null || ochRoute.getPrimary() == null) {
            return false;
        }

        // OCH protection type is only a coarse filter. Reuse must keep each leg's siteLink route and REG/ROADM shape unchanged.
        if (!matchRouteLeg(siteLinkRoute.getPrimary(), primarySiteTypes, ochRoute.getPrimary().getExplicitRouteObjects(),
                ochRoute.getPrimary().getCrossConnections())) {
            return false;
        }

        if (siteLinkRoute.getSecondary() == null || siteLinkRoute.getSecondary().isEmpty()) {
            if (ochRoute.getSecondary() != null) {
                return false;
            }
        } else {
            if (ochRoute.getSecondary() == null || !matchRouteLeg(siteLinkRoute.getSecondary(), secondarySiteTypes,
                    ochRoute.getSecondary().getExplicitRouteObjects(), ochRoute.getSecondary().getCrossConnections())) {
                return false;
            }
        }

        if (siteLinkRoute.getThird() == null || siteLinkRoute.getThird().isEmpty()) {
            return ochRoute.getThird() == null || ochRoute.getThird().isEmpty();
        }
        if (ochRoute.getThird() == null || ochRoute.getThird().size() != 1) {
            return false;
        }
        Third third = ochRoute.getThird().get(0);
        return matchRouteLeg(siteLinkRoute.getThird(), thirdSiteTypes, third.getExplicitRouteObjects(), third.getCrossConnections());
    }

    private boolean matchRouteLeg(List<String> requiredSiteLinks, Map<String, String> requiredSiteTypes,
                                  List<ExplicitRouteObjects> explicitRouteObjects, List<CrossConnections> crossConnections) {
        List<String> ochSiteLinks = getSiteLinkRoute(explicitRouteObjects);
        if (!ochSiteLinks.equals(requiredSiteLinks)) {
            return false;
        }

        Set<String> requiredRegSites = requiredSiteTypes.entrySet().stream()
                .filter(entry -> REG.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<String> ochRegSites = getRegSites(crossConnections);
        return ochRegSites.equals(requiredRegSites);
    }

    private Map<String, String> getSiteTypes(List<? extends RegSiteInfo> regSiteInfos) {
        if (regSiteInfos == null || regSiteInfos.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> siteTypes = new HashMap<>();
        for (RegSiteInfo regSiteInfo : regSiteInfos) {
            siteTypes.put(regSiteInfo.getSiteId(), regSiteInfo.getType());
        }
        return siteTypes;
    }

    private List<String> getSiteLinkRoute(List<ExplicitRouteObjects> explicitRouteObjects) {
        if (explicitRouteObjects == null || explicitRouteObjects.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> siteLinkIds = new ArrayList<>();
        for (ExplicitRouteObjects explicitRouteObject : explicitRouteObjects) {
            if (explicitRouteObject.getPathRouteObject() == null) {
                continue;
            }
            for (PathRouteObject pathRouteObject : explicitRouteObject.getPathRouteObject()) {
                if (pathRouteObject.getTopologyRef() == null || !Constant.SITE_TOPOID.equals(pathRouteObject.getTopologyRef().getValue())) {
                    continue;
                }
                if (!(pathRouteObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link)) {
                    continue;
                }
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link link =
                        (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pathRouteObject.getResourceType();
                if (link.getLinkHop() == null || link.getLinkHop().getLinkRef() == null) {
                    continue;
                }
                String linkId = link.getLinkHop().getLinkRef().getValue();
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    siteLinkIds.add(linkId);
                }
            }
        }
        return siteLinkIds;
    }

    private Set<String> getRegSites(List<CrossConnections> xcs) {
        if (xcs == null || xcs.isEmpty()) {
            return Collections.emptySet();
        }
        return xcs.stream().filter(xc -> LinkDirection.Unidirection.equals(xc.getDirection())
                        && xc.getSourceTp() != null && !xc.getSourceTp().isEmpty()
                        && xc.getSourceTp().get(0).getTpRef() != null)
                .map(xc -> PhysicalTpIdNamingRule.getSiteId(xc.getSourceTp().get(0).getTpRef().getValue())).collect(
                        Collectors.toSet());
    }

    public int getMatchedOchReusedOdu(Link ochLink, ParamCreaionBasic param, String vendorName, String productType) {
        Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

        if (ochLinkAttr.getAvailable() == null) {
            return 0;
        }

        if (!ochLinkAttr.getCardType().equals(param.getCardType())) {
            return 0;
        }

        if (!ochLinkAttr.getVendorName().equals(vendorName) || !ochLinkAttr.getProductType().equals(productType)) {
            return 0;
        }
        if (!ochLinkAttr.getOdukType().equals(param.getOdukType())) {
            return 0;
        }
        List<SupportedTunnel> ochLinkSupportedTunnels = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class).getSupportedTunnel();
        if (ochLinkSupportedTunnels == null || ochLinkSupportedTunnels.isEmpty()) {
            log.warn("Invalid och:{}, because no supported tunnel.", ochLink.getLinkId().getValue());
            return 0;
        }
        String tunnelId = ochLinkSupportedTunnels.get(0).getTunnelRef().getValue();
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (tunnel == null) {
            log.error("Invalid och:{}, because can't find supported tunnel by tunnelId.{}", ochLink.getLinkId().getValue(),tunnelId);
            return 0;
        }
        SERVICETYPE serviceType = tunnel.getServiceType();
        if (!param.getServiceType().equals(serviceType)) {
            return 0;
        }

        for (Available avaOdu : ochLinkAttr.getAvailable()) {
            if (avaOdu.getSupportedOduj().equals(param.getTunnelOdu())) {
                String available = avaOdu.getAvailableOdujSlot();
                if (available.isEmpty()) {
                    return 0;
                } else {
                    return available.split(Constant.OCH_AVAILABLE_ODU_SEPARATOR).length;
                }
            }
        }

        return 0;
    }

}
