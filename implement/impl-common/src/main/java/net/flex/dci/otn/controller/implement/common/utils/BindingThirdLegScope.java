package net.flex.dci.otn.controller.implement.common.utils;

import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BindingThirdLegScope {

    private final List<String> siteLinkIds;
    private final Set<String> routeNodeIds;

    private BindingThirdLegScope(List<String> siteLinkIds, Set<String> routeNodeIds) {
        this.siteLinkIds = siteLinkIds;
        this.routeNodeIds = routeNodeIds;
    }

    public static BindingThirdLegScope empty() {
        return new BindingThirdLegScope(Collections.emptyList(), Collections.emptySet());
    }

    public static BindingThirdLegScope fromSiteLinks(ChangedObject changedObject,
            List<String> siteLinkIds) {
        if (siteLinkIds == null || siteLinkIds.isEmpty()) {
            return empty();
        }

        Set<String> nodeIds = new LinkedHashSet<>();
        siteLinkIds.forEach(siteLinkId -> {
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            Site siteLinkAttr = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();
            RouteInfo siteLinkRouteInfo = new RouteInfo();
            siteLinkRouteInfo.parse(siteLinkAttr.getExplictRoute().getRoute());
            nodeIds.addAll(siteLinkRouteInfo.getNodeIdList());
        });

        return new BindingThirdLegScope(new ArrayList<>(siteLinkIds), nodeIds);
    }

    public static BindingThirdLegScope fromOchThirdRoute(ChangedObject changedObject,
            Link ochLink) {
        Set<String> siteLinkIds = new LinkedHashSet<>();
        if (ochLink == null || ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class) == null) {
            return empty();
        }

        Och ochLinkAttr = ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        if (ochLinkAttr == null || ochLinkAttr.getExplictRoute() == null
                || ochLinkAttr.getExplictRoute().getRoute() == null) {
            return empty();
        }

        // The persisted OCH third route is the stable scope for one bind-third-leg flow.
        for (Route route : ochLinkAttr.getExplictRoute().getRoute()) {
            if (route.getThird() == null) {
                continue;
            }
            for (Third third : route.getThird()) {
                siteLinkIds.addAll(getAllSiteLinksWithEro(third.getExplicitRouteObjects()));
            }
        }
        return fromSiteLinks(changedObject, new ArrayList<>(siteLinkIds));
    }

    public static boolean isBindingThirdLeg(Och ochLinkAttr) {
        // Partial provisions the third leg; Implement retries only the final ASE/marker cleanup.
        return (ochLinkAttr.getImplementState().equals(ImplementState.PartialImplement) ||
                ochLinkAttr.getImplementState().equals(ImplementState.Implement))
                && PropertyTool.existProperty(ochLinkAttr.getProperties(), "binding3rdLeg");
    }

    public static void includePrimaryApsXcs(RouteInfo rInfo, Och ochLinkAttr) {
        // Binding changes the existing primary APS XC properties, so download only those XCs again.
        for (Route route : ochLinkAttr.getExplictRoute().getRoute()) {
            if (route.getPrimary() == null || route.getPrimary().getCrossConnections() == null) {
                continue;
            }
            route.getPrimary().getCrossConnections().stream()
                    .filter(xc -> xc.getAps() != null)
                    .map(xc -> xc.getCrossConnectionId().getValue())
                    .forEach(xcId -> {
                        if (!rInfo.getXcIdList().contains(xcId)) {
                            rInfo.getXcIdList().add(xcId);
                        }
                        String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
                        if (!rInfo.getNodeIdList().contains(nodeId)) {
                            rInfo.getNodeIdList().add(nodeId);
                        }
                    });
        }
    }

    public static void keepOnlyProtectionCtpWithoutApsXc(RouteInfo rInfo, Och ochLinkAttr) {
        Set<String> apsXcIds = new HashSet<>();
        Set<String> apsTpIds = new HashSet<>();

        // Third-leg binding only needs the new protection C port adminUp.
        for (CrossConnectionAttributes xc : RouteExtractor.extractorXc(
                ochLinkAttr.getExplictRoute().getRoute())) {
            if (xc.getAps() == null) {
                continue;
            }
            apsXcIds.add(xc.getCrossConnectionId().getValue());
            xc.getSourceTp().forEach(tp -> apsTpIds.add(tp.getTpRef().getValue()));
            xc.getDestinationTp().forEach(tp -> apsTpIds.add(tp.getTpRef().getValue()));
        }

        rInfo.getXcIdList().removeIf(apsXcIds::contains);
        rInfo.getTpIdList().removeIf(tpId -> apsTpIds.contains(tpId)
                && !isProtectionCtp(tpId));
    }

    public static Set<String> trimNodeIdListToImpactedResources(RouteInfo rInfo) {
        Set<String> impactedNodes = getImpactedResourceNodeIds(rInfo);
        rInfo.getNodeIdList().removeIf(nodeId -> !impactedNodes.contains(nodeId));
        return impactedNodes;
    }

    public static Set<String> getImpactedResourceNodeIds(RouteInfo rInfo) {
        Set<String> impactedNodes = new HashSet<>();

        rInfo.getTpIdList().forEach(tpId ->
                impactedNodes.add(PhysicalTpIdNamingRule.getNodeId(tpId)));
        rInfo.getEqIdList().forEach(eqId ->
                impactedNodes.add(PhysicalEqpIdNamingRule.getNodeId(eqId)));
        rInfo.getXcIdList().forEach(xcId ->
                impactedNodes.add(PhysicalXcIdNamingRule.getNodeId(xcId)));
        rInfo.getPhyLinkIdList().forEach(phyLinkId -> {
            impactedNodes.add(PhysicalLinkIdNamingRule.getNodeAId(phyLinkId));
            impactedNodes.add(PhysicalLinkIdNamingRule.getNodeZId(phyLinkId));
        });

        return impactedNodes;
    }

    public boolean active() {
        return !siteLinkIds.isEmpty();
    }

    public static boolean active(BindingThirdLegScope scope) {
        return scope != null && scope.active();
    }

    public List<String> getSiteLinkIds() {
        return new ArrayList<>(siteLinkIds);
    }

    public boolean allowNode(String nodeId) {
        return routeNodeIds.isEmpty() || routeNodeIds.contains(nodeId);
    }

    public List<CrossConnectionAttributes> filterRouteXcs(List<CrossConnectionAttributes> xcList) {
        if (routeNodeIds.isEmpty()) {
            return xcList;
        }

        return xcList.stream()
                .filter(xc -> routeNodeIds.contains(
                        PhysicalXcIdNamingRule.getNodeId(xc.getCrossConnectionId().getValue())))
                .collect(Collectors.toList());
    }

    public void trimLogicServerLinks(RouteInfo rInfo) {
        if (!active()) {
            return;
        }
        rInfo.getLogicServerLinkIdList().removeIf(linkId ->
                SiteLinkIdNamingRule.isSiteLink(linkId) && !siteLinkIds.contains(linkId));
    }

    private static boolean isProtectionCtp(String tpId) {
        return tpId.matches(".*#PORT-[^#]+-[0-9]+C$");
    }

    private static List<String> getAllSiteLinksWithEro(
            List<ExplicitRouteObjects> explicitRouteObjects) {
        List<String> siteLinkIds = new ArrayList<>();
        if (explicitRouteObjects == null) {
            return siteLinkIds;
        }

        for (ExplicitRouteObjects ero : explicitRouteObjects) {
            if (ero.getPathRouteObject() == null) {
                continue;
            }
            for (PathRouteObject pathRouteObject : ero.getPathRouteObject()) {
                if (!pathRouteObject.getResourceType().getImplementedInterface().getName()
                        .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class.getName())) {
                    continue;
                }
                LinkHop hop = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pathRouteObject
                        .getResourceType()).getLinkHop();
                if (hop.getTopologyRef().getValue().equals(SiteTopology.QNAME.getLocalName())) {
                    siteLinkIds.add(hop.getLinkRef().getValue());
                }
            }
        }
        return siteLinkIds;
    }

}
