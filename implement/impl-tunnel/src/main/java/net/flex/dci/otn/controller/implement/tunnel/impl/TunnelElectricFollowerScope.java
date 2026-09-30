package net.flex.dci.otn.controller.implement.tunnel.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * Same-OCH followers only own their service endpoint C ports and the direct C-L
 * XCs. This scope only narrows RouteInfo and locks; LinkImplementState still
 * decides the device payload and persisted resource states.
 */
final class TunnelElectricFollowerScope {

    private final RouteInfo routeInfo;

    TunnelElectricFollowerScope(RouteInfo routeInfo) {
        this.routeInfo = routeInfo;
    }

    static TunnelElectricFollowerScope fromTunnel(Tunnel tunnel) {
        RouteInfo fullRouteInfo = new RouteInfo();
        fullRouteInfo.parse(tunnel.getExplictRoute().getRoute());

        RouteInfo scopedRouteInfo = new RouteInfo();
        Set<String> serviceTpIds = serviceTpIds(tunnel);
        fullRouteInfo.getTpIdList().stream()
                .filter(serviceTpIds::contains)
                .forEach(tpId -> addDistinct(scopedRouteInfo.getTpIdList(), tpId));

        RouteExtractor.extractorXc(tunnel.getExplictRoute().getRoute()).stream()
                .filter(xc -> touchesAnyServiceTp(xc, serviceTpIds))
                .map(xc -> xc.getCrossConnectionId().getValue())
                .forEach(xcId -> addDistinct(scopedRouteInfo.getXcIdList(), xcId));

        scopedRouteInfo.getTpIdList().stream()
                .map(PhysicalTpIdNamingRule::getNodeId)
                .forEach(nodeId -> addDistinct(scopedRouteInfo.getNodeIdList(), nodeId));
        scopedRouteInfo.getXcIdList().stream()
                .map(PhysicalXcIdNamingRule::getNodeId)
                .forEach(nodeId -> addDistinct(scopedRouteInfo.getNodeIdList(), nodeId));

        return new TunnelElectricFollowerScope(scopedRouteInfo);
    }

    RouteInfo getRouteInfo() {
        return routeInfo;
    }

    Collection<String> lockResourceIds(String tunnelId) {
        Set<String> resourceIds = new LinkedHashSet<>();
        resourceIds.add(tunnelId);
        resourceIds.addAll(routeInfo.getTpIdList());
        resourceIds.addAll(routeInfo.getXcIdList());
        return resourceIds.stream().sorted().collect(Collectors.toList());
    }

    void removeFrom(RouteInfo commonRouteInfo) {
        // The OCH owner handles shared resources first. Its customer-side C ports
        // and service XCs are deferred to the same private stage used by followers.
        commonRouteInfo.getTpIdList().removeAll(routeInfo.getTpIdList());
        commonRouteInfo.getXcIdList().removeAll(routeInfo.getXcIdList());

        Set<String> nodeIds = new LinkedHashSet<>();
        commonRouteInfo.getTpIdList().stream()
                .map(PhysicalTpIdNamingRule::getNodeId)
                .forEach(nodeIds::add);
        commonRouteInfo.getXcIdList().stream()
                .map(PhysicalXcIdNamingRule::getNodeId)
                .forEach(nodeIds::add);
        commonRouteInfo.getEqIdList().stream()
                .map(PhysicalEqpIdNamingRule::getNodeId)
                .forEach(nodeIds::add);
        commonRouteInfo.getPhyLinkIdList().forEach(linkId -> {
            nodeIds.add(PhysicalLinkIdNamingRule.getNodeAId(linkId));
            nodeIds.add(PhysicalLinkIdNamingRule.getNodeZId(linkId));
        });
        commonRouteInfo.setNodeIdList(new java.util.ArrayList<>(nodeIds));
    }

    private static Set<String> serviceTpIds(Tunnel tunnel) {
        Set<String> tpIds = new LinkedHashSet<>();
        if (tunnel.getSourceTp() != null) {
            tunnel.getSourceTp().stream()
                    .map(tp -> tp.getTpRef())
                    .filter(tpRef -> tpRef != null)
                    .forEach(tpRef -> tpIds.add(tpRef.getValue()));
        }
        if (tunnel.getDestinationTp() != null) {
            tunnel.getDestinationTp().stream()
                    .filter(tp -> tp.getTpRef() != null)
                    .forEach(tp -> tpIds.add(tp.getTpRef().getValue()));
        }
        return tpIds;
    }

    private static boolean touchesAnyServiceTp(CrossConnectionAttributes xc,
            Set<String> serviceTpIds) {
        if (xc.getSourceTp() != null && xc.getSourceTp().stream()
                .filter(tp -> tp.getTpRef() != null)
                .anyMatch(tp -> serviceTpIds.contains(tp.getTpRef().getValue()))) {
            return true;
        }
        return xc.getDestinationTp() != null && xc.getDestinationTp().stream()
                .filter(tp -> tp.getTpRef() != null)
                .anyMatch(tp -> serviceTpIds.contains(tp.getTpRef().getValue()));
    }

    private static void addDistinct(List<String> values, String value) {
        if (value != null && !values.contains(value)) {
            values.add(value);
        }
    }
}
