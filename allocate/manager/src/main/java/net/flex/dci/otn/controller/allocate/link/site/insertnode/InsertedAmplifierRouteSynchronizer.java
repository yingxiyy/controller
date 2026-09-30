package net.flex.dci.otn.controller.allocate.link.site.insertnode;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InsertedAmplifierRouteSynchronizer {
    private InsertedAmplifierRouteSynchronizer() {
    }

    public static Link synchronize(Link siteLink, Node insertedNode) {
        Map<String, CrossConnections> finalAmplifierXcs = getFinalAmplifierXcs(insertedNode);
        if (finalAmplifierXcs.isEmpty()) {
            return siteLink;
        }

        Link1 siteLinkAugmentation = siteLink.getAugmentation(Link1.class);
        Site site = siteLinkAugmentation.getSite();
        ExplictRoute explicitRoute = site.getExplictRoute();
        Set<String> synchronizedIds = new HashSet<>();
        List<Route> updatedRoutes = new ArrayList<>();

        for (Route route : explicitRoute.getRoute()) {
            RouteBuilder routeBuilder = new RouteBuilder(route);
            if (route.getPrimary() != null) {
                routeBuilder.setPrimary(new PrimaryBuilder(route.getPrimary())
                        .setCrossConnections(synchronizeXcs(route.getPrimary().getCrossConnections(),
                                finalAmplifierXcs, synchronizedIds))
                        .build());
            }
            if (route.getSecondary() != null) {
                routeBuilder.setSecondary(new SecondaryBuilder(route.getSecondary())
                        .setCrossConnections(synchronizeXcs(route.getSecondary().getCrossConnections(),
                                finalAmplifierXcs, synchronizedIds))
                        .build());
            }
            if (route.getThird() != null) {
                List<Third> updatedThirds = new ArrayList<>();
                for (Third third : route.getThird()) {
                    updatedThirds.add(new ThirdBuilder(third)
                            .setCrossConnections(synchronizeXcs(third.getCrossConnections(),
                                    finalAmplifierXcs, synchronizedIds))
                            .build());
                }
                routeBuilder.setThird(updatedThirds);
            }
            updatedRoutes.add(routeBuilder.build());
        }

        if (!synchronizedIds.containsAll(finalAmplifierXcs.keySet())) {
            Set<String> missingIds = new HashSet<>(finalAmplifierXcs.keySet());
            missingIds.removeAll(synchronizedIds);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "SiteLink route is missing inserted amplifier XC " + missingIds);
        }

        ExplictRoute updatedExplicitRoute = new ExplictRouteBuilder(explicitRoute)
                .setRoute(updatedRoutes)
                .build();
        Site updatedSite = new SiteBuilder(site)
                .setExplictRoute(updatedExplicitRoute)
                .build();
        return new LinkBuilder(siteLink)
                .addAugmentation(Link1.class, new Link1Builder(siteLinkAugmentation)
                        .setSite(updatedSite)
                        .build())
                .build();
    }

    private static Map<String, CrossConnections> getFinalAmplifierXcs(Node insertedNode) {
        Map<String, CrossConnections> result = new HashMap<>();
        List<CrossConnections> xcs = insertedNode.getAugmentation(Node1.class)
                .getPhysical().getCrossConnections();
        if (xcs == null) {
            return result;
        }
        for (CrossConnections xc : xcs) {
            if (xc.getAmplifier() != null && xc.getCrossConnectionId() != null) {
                result.put(xc.getCrossConnectionId().getValue(), xc);
            }
        }
        return result;
    }

    static List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> synchronizeXcs(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> routeXcs,
            Map<String, CrossConnections> finalAmplifierXcs, Set<String> synchronizedIds) {
        if (routeXcs == null) {
            return null;
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> result =
                new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections routeXc : routeXcs) {
            String xcId = routeXc.getCrossConnectionId() == null
                    ? null : routeXc.getCrossConnectionId().getValue();
            CrossConnections finalXc = finalAmplifierXcs.get(xcId);
            if (finalXc == null) {
                result.add(routeXc);
                continue;
            }
            result.add(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(finalXc)
                    .setSequence(routeXc.getSequence())
                    .build());
            synchronizedIds.add(xcId);
        }
        return result;
    }
}
