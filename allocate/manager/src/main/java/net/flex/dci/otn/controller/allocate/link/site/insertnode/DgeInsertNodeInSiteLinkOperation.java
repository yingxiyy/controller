package net.flex.dci.otn.controller.allocate.link.site.insertnode;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.ChannelTargetPowerCalculator;
import net.flex.dci.otc.common.util.frequency.Constant;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

class DgeInsertNodeInSiteLinkOperation extends BaseInsertNodeInSiteLinkOperation {
    private static final long CURRENT_BANDWIDTH_MHZ = 100_000L;
    private static final double TARGET_OUTPUT_POWER_C = 22;
    private static final double TARGET_OUTPUT_TILT_C = -1.1;
    private static final double TARGET_OUTPUT_POWER_L = 21;
    private static final double TARGET_OUTPUT_TILT_L = -0.1;

    DgeInsertNodeInSiteLinkOperation(InsertNodeInSiteLinkContext context, ChangedObject changedObject) {
        super(context, changedObject);
    }

    @Override
    protected void updateOchXcsForRoute() {
        // DGE 特有：当前 siteLink 承载的所有 OCH 都会随 OTS 拆分变化；这里不再读取 OCH route 判断旧 OTS。
        // createInsertedNode() 已经创建 DGE 默认 amplifier XC；这里按 OCH 中心频率与 C/L 理论功率序列对齐，补业务/假波 WSS XC。
        context.dgeOchXcsToCreate.clear();
        context.ochXcsByOchLinkId.clear();
        if (context.ochLinks == null || context.ochLinks.isEmpty()) {
            return;
        }
        List<DgeOchXcToCreate> xcsToCreate = new ArrayList<>();
        for (Link ochLink : context.ochLinks) {
            xcsToCreate.add(buildXcToCreate(ochLink));
        }
        assignTargetPower(xcsToCreate.stream().filter(xc -> xc.cBand).collect(Collectors.toList()), true);
        assignTargetPower(xcsToCreate.stream().filter(xc -> !xc.cBand).collect(Collectors.toList()), false);
        context.dgeOchXcsToCreate.addAll(xcsToCreate);

        TerminationPoint eastTp = getInsertedLineTp("LINE_EAST");
        TerminationPoint westTp = getInsertedLineTp("LINE_WEST");
        Equipments equipment = getEquipment(eastTp);
        List<CrossConnections> ochXcs = context.createDgeOchXcs(eastTp, westTp, equipment);
        changedObject.addChangedPhyNode(context.appendInsertedNodeCrossConnections(ochXcs));
    }

    @Override
    protected void updateOchRoutes() {
        if (context.ochLinks == null || context.ochLinks.isEmpty()) {
            return;
        }
        for (Link ochLink : context.ochLinks) {
            List<CrossConnections> xcsToAdd = context.ochXcsByOchLinkId.get(ochLink.getLinkId().getValue());
            if (xcsToAdd == null || xcsToAdd.isEmpty()) {
                continue;
            }
            if (isDummyOch(ochLink)) {
                changedObject.addChangedOchLink(updateDummyOchRoute(ochLink, xcsToAdd));
                continue;
            }
            changedObject.addChangedOchLink(updateOchRoute(ochLink, xcsToAdd));
        }
    }

    private boolean isDummyOch(Link ochLink) {
        String ochLinkId = ochLink.getLinkId().getValue();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteAug =
                context.siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
        if (siteAug != null && siteAug.getSite() != null) {
            Site site = siteAug.getSite();
            if (site.getDummyLink() != null && site.getDummyLink().contains(ochLinkId)) {
                return true;
            }
        }
        Och och = getOch(ochLink);
        return och.getFriendlyName() != null && och.getFriendlyName().startsWith("ASE:");
    }

    private Link updateOchRoute(Link ochLink, List<CrossConnections> xcsToAdd) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 oldOchAug =
                ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
        if (oldOchAug == null || oldOchAug.getOch() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find och attribute on ochLink " + ochLink.getLinkId().getValue());
        }
        Och oldOch = oldOchAug.getOch();
        ExplictRoute oldRoute = oldOch.getExplictRoute();
        if (oldRoute == null || oldRoute.getRoute() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ochLink has no explict route: " + ochLink.getLinkId().getValue());
        }

        boolean siteLinkFound = false;
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route> newRoutes =
                new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route : oldRoute.getRoute()) {
            RouteBuilder routeBuilder = new RouteBuilder(route);
            if (route.getPrimary() != null) {
                siteLinkFound = siteLinkFound || containsSiteLink(route.getPrimary().getExplicitRouteObjects());
                routeBuilder.setPrimary(updatePrimaryOchRoute(route.getPrimary(), xcsToAdd));
            }
            if (route.getSecondary() != null) {
                siteLinkFound = siteLinkFound || containsSiteLink(route.getSecondary().getExplicitRouteObjects());
                routeBuilder.setSecondary(updateSecondaryOchRoute(route.getSecondary(), xcsToAdd));
            }
            if (route.getThird() != null) {
                List<Third> newThirds = new ArrayList<>();
                for (Third third : route.getThird()) {
                    siteLinkFound = siteLinkFound || containsSiteLink(third.getExplicitRouteObjects());
                    newThirds.add(updateThirdOchRoute(third, xcsToAdd));
                }
                routeBuilder.setThird(newThirds);
            }
            newRoutes.add(routeBuilder.build());
        }
        if (!siteLinkFound) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ochLink route does not contain siteLink: " + context.siteLinkId);
        }

        ExplictRoute updatedRoute = new ExplictRouteBuilder(oldRoute).setRoute(newRoutes).build();
        return new LinkBuilder(ochLink)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder()
                                .setOch(new OchBuilder(oldOch)
                                        .setExplictRoute(updatedRoute)
                                        .build())
                                .build())
                .build();
    }

    private Link updateDummyOchRoute(Link ochLink, List<CrossConnections> xcsToAdd) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 oldOchAug =
                ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
        if (oldOchAug == null || oldOchAug.getOch() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find och attribute on ochLink " + ochLink.getLinkId().getValue());
        }
        Och oldOch = oldOchAug.getOch();
        ExplictRoute oldRoute = oldOch.getExplictRoute();
        if (oldRoute == null || oldRoute.getRoute() == null || oldRoute.getRoute().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "dummy ochLink has no explict route: " + ochLink.getLinkId().getValue());
        }

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route> newRoutes =
                new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route : oldRoute.getRoute()) {
            RouteBuilder routeBuilder = new RouteBuilder(route);
            if (route.getPrimary() != null) {
                routeBuilder.setPrimary(new PrimaryBuilder(route.getPrimary())
                        .setCrossConnections(mergeDummyOchRouteXcs(route.getPrimary().getCrossConnections(), xcsToAdd,
                                route.getPrimary().getExplicitRouteObjects(), ochLink))
                        .build());
            }
            if (route.getSecondary() != null) {
                routeBuilder.setSecondary(new SecondaryBuilder(route.getSecondary())
                        .setCrossConnections(mergeDummyOchRouteXcs(route.getSecondary().getCrossConnections(), xcsToAdd,
                                route.getSecondary().getExplicitRouteObjects(), ochLink))
                        .build());
            }
            if (route.getThird() != null) {
                List<Third> newThirds = new ArrayList<>();
                for (Third third : route.getThird()) {
                    newThirds.add(new ThirdBuilder(third)
                            .setCrossConnections(mergeDummyOchRouteXcs(third.getCrossConnections(), xcsToAdd,
                                    third.getExplicitRouteObjects(), ochLink))
                            .build());
                }
                routeBuilder.setThird(newThirds);
            }
            newRoutes.add(routeBuilder.build());
        }

        ExplictRoute updatedRoute = new ExplictRouteBuilder(oldRoute).setRoute(newRoutes).build();
        return new LinkBuilder(ochLink)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder()
                                .setOch(new OchBuilder(oldOch)
                                        .setExplictRoute(updatedRoute)
                                        .build())
                                .build())
                .build();
    }

    private Primary updatePrimaryOchRoute(Primary oldPrimary, List<CrossConnections> xcsToAdd) {
        if (!containsSiteLink(oldPrimary.getExplicitRouteObjects())) {
            return oldPrimary;
        }
        // Expand SiteLink only for XC ordering; retain the OCH route's abstraction level.
        return new PrimaryBuilder(oldPrimary)
                .setCrossConnections(mergeOchRouteXcs(oldPrimary.getCrossConnections(), xcsToAdd,
                        oldPrimary.getExplicitRouteObjects()))
                .build();
    }

    private Secondary updateSecondaryOchRoute(Secondary oldSecondary, List<CrossConnections> xcsToAdd) {
        if (!containsSiteLink(oldSecondary.getExplicitRouteObjects())) {
            return oldSecondary;
        }
        return new SecondaryBuilder(oldSecondary)
                .setCrossConnections(mergeOchRouteXcs(oldSecondary.getCrossConnections(), xcsToAdd,
                        oldSecondary.getExplicitRouteObjects()))
                .build();
    }

    private Third updateThirdOchRoute(Third oldThird, List<CrossConnections> xcsToAdd) {
        if (!containsSiteLink(oldThird.getExplicitRouteObjects())) {
            return oldThird;
        }
        return new ThirdBuilder(oldThird)
                .setCrossConnections(mergeOchRouteXcs(oldThird.getCrossConnections(), xcsToAdd,
                        oldThird.getExplicitRouteObjects()))
                .build();
    }

    private boolean containsSiteLink(List<ExplicitRouteObjects> eros) {
        if (eros == null) {
            return false;
        }
        for (ExplicitRouteObjects ero : eros) {
            if (ero.getPathRouteObject() == null) {
                continue;
            }
            for (PathRouteObject routeObject : ero.getPathRouteObject()) {
                if (isCurrentSiteLinkHop(routeObject)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCurrentSiteLinkHop(PathRouteObject routeObject) {
        if (routeObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link link =
                    (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) routeObject.getResourceType();
            return link.getLinkHop() != null && link.getLinkHop().getLinkRef() != null
                    && context.siteLinkId.equals(link.getLinkHop().getLinkRef().getValue());
        }
        return false;
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> mergeOchRouteXcs(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> oldXcs,
            List<CrossConnections> xcsToAdd, List<ExplicitRouteObjects> eros) {
        Link targetSiteLink = changedObject.getChangedSiteLink(context.siteLinkId);
        try {
            return RouteCrossConnectionOrder.mergeOch(oldXcs, xcsToAdd,
                    RouteCrossConnectionOrder.ochTps(eros, targetSiteLink,
                            RouteCrossConnectionOrder.insertedTps(xcsToAdd)),
                    RouteCrossConnectionOrder.physicalTps(eros),
                    RouteCrossConnectionOrder.relativeSideResolver(eros, context.siteLinkId, changedObject::getChangedSiteLink));
        } catch (RouteCrossConnectionOrder.UnresolvedOrderException e) {
            return RouteCrossConnectionOrder.mergeOchBetweenNeighbors(oldXcs, xcsToAdd, eros, targetSiteLink, null, null,
                    RouteCrossConnectionOrder.relativeSideResolver(eros, context.siteLinkId, changedObject::getChangedSiteLink));
        }
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> mergeDummyOchRouteXcs(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> oldXcs,
            List<CrossConnections> xcsToAdd, List<ExplicitRouteObjects> eros, Link ochLink) {
        Link targetSiteLink = changedObject.getChangedSiteLink(context.siteLinkId);
        List<String> insertedTps = RouteCrossConnectionOrder.insertedTps(xcsToAdd);
        try {
            List<String> routeTps = RouteCrossConnectionOrder.ochTps(eros, targetSiteLink, insertedTps);
            if (routeTps.isEmpty()) {
                if (eros != null && eros.stream().anyMatch(ero -> ero.getPathRouteObject() != null
                        && !ero.getPathRouteObject().isEmpty())) {
                    return oldXcs;
                }
                routeTps = RouteCrossConnectionOrder.siteTps(targetSiteLink,
                        ochLink.getSource().getSourceTp().getValue(),
                        ochLink.getDestination().getDestTp().getValue(), insertedTps);
            }
            return RouteCrossConnectionOrder.mergeOch(oldXcs, xcsToAdd, routeTps,
                    RouteCrossConnectionOrder.physicalTps(eros),
                    RouteCrossConnectionOrder.relativeSideResolver(eros, context.siteLinkId, changedObject::getChangedSiteLink));
        } catch (RouteCrossConnectionOrder.UnresolvedOrderException e) {
            return RouteCrossConnectionOrder.mergeOchBetweenNeighbors(oldXcs, xcsToAdd, eros, targetSiteLink,
                    ochLink.getSource() == null || ochLink.getSource().getSourceTp() == null
                            ? null : ochLink.getSource().getSourceTp().getValue(),
                    ochLink.getDestination() == null || ochLink.getDestination().getDestTp() == null
                            ? null : ochLink.getDestination().getDestTp().getValue(),
                    RouteCrossConnectionOrder.relativeSideResolver(eros, context.siteLinkId, changedObject::getChangedSiteLink));
        }
    }

    private DgeOchXcToCreate buildXcToCreate(Link ochLink) {
        Och och = getOch(ochLink);
        if (och.getLowerFrequency() == null || och.getUpperFrequency() == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot prepare DGE OCH XC without frequency on ochLink " + ochLink.getLinkId().getValue());
        }
        if (och.getUpperFrequency().getValue().compareTo(och.getLowerFrequency().getValue()) <= 0) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "invalid frequency range on ochLink " + ochLink.getLinkId().getValue());
        }
        // 现存 OCH link 已经在 och attributes 上保存 lower/upper frequency，DGE WSS XC 直接复用这个频谱范围。
        return new DgeOchXcToCreate(ochLink.getLinkId().getValue(), och.getLowerFrequency(),
                och.getUpperFrequency(), isCBand(och.getLowerFrequency()));
    }

    private void assignTargetPower(List<DgeOchXcToCreate> xcsToCreate, boolean cBand) {
        if (xcsToCreate.isEmpty()) {
            return;
        }
        List<DgeOchXcToCreate> sortedXcs = xcsToCreate.stream()
                .sorted(Comparator.comparing(xc -> xc.centreFrequency.getValue()))
                .collect(Collectors.toList());
        List<Double> sortedTheoryPowers = calculateTheoryPowers(cBand);
        if (sortedTheoryPowers.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot calculate DGE target power for " + (cBand ? "C" : "L") + " band");
        }

        for (int i = 0; i < sortedXcs.size(); i++) {
            DgeOchXcToCreate xcToCreate = sortedXcs.get(i);
            double theoryPower = sortedTheoryPowers.get(Math.min(i, sortedTheoryPowers.size() - 1));
            double targetBandwidth = xcToCreate.upperFrequency.getValue()
                    .subtract(xcToCreate.lowerFrequency.getValue()).doubleValue();
            double targetPower = theoryPower + 10.0 * Math.log10(targetBandwidth / CURRENT_BANDWIDTH_MHZ);
            xcToCreate.setTargetPower(targetPower);
        }
    }

    private List<Double> calculateTheoryPowers(boolean cBand) {
        long bandLower = cBand ? Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_C)
                : Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_L);
        long bandUpper = cBand ? Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_C)
                : Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_L);
        int totalSlots = cBand ? ChannelTargetPowerCalculator.C_BAND_TOTAL_SLOTS
                : ChannelTargetPowerCalculator.L_BAND_TOTAL_SLOTS;
        double targetOutputPower = cBand ? TARGET_OUTPUT_POWER_C : TARGET_OUTPUT_POWER_L;
        double targetOutputTilt = cBand ? TARGET_OUTPUT_TILT_C : TARGET_OUTPUT_TILT_L;

        Map<Long, Double> adjustValues = ChannelTargetPowerCalculator.calculateTargetPower(bandLower, bandUpper,
                CURRENT_BANDWIDTH_MHZ, totalSlots, bandLower, true, targetOutputPower, targetOutputTilt);
        // calculateTargetPower 返回全 C/L 波段理论值；这里按频率顺序和已有 OCH 中心频率顺序做一一映射。
        return adjustValues.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private boolean isCBand(FrequencyType lowerFrequency) {
        BigInteger lower = lowerFrequency.getValue();
        return lower.compareTo(new BigInteger(Constant.MaxLowerFrequency.MUX64_BD_C)) >= 0
                && lower.compareTo(new BigInteger(Constant.MaxUpperFrequency.MUX64_BD_C)) <= 0;
    }

    private Och getOch(Link ochLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochAug =
                ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
        if (ochAug == null || ochAug.getOch() == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find och attribute on ochLink " + ochLink.getLinkId().getValue());
        }
        return ochAug.getOch();
    }

    private TerminationPoint getInsertedLineTp(String portSuffix) {
        Node insertedNode = context.insertedPhyNode;
        if (insertedNode == null || insertedNode.getTerminationPoint() == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot create DGE OCH XC before inserted node is created");
        }
        return insertedNode.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().endsWith(portSuffix))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find inserted DGE line tp " + portSuffix + " on " + insertedNode.getNodeId().getValue()));
    }

    private Equipments getEquipment(TerminationPoint tp) {
        String equipmentId = PhysicalTpIdNamingRule.getEquipId(tp.getTpId().getValue());
        return context.insertedPhyNode.getAugmentation(Node1.class).getPhysical().getEquipments().stream()
                .filter(equipment -> equipment.getEquipmentId().equals(equipmentId))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find equipment " + equipmentId + " on " + context.insertedPhyNode.getNodeId().getValue()));
    }

}
