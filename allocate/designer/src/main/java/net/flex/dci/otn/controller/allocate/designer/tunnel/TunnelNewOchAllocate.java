/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import static net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput.PROTECTED_1TO1;
import static net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput.PROTECTED_1TO2;
import static net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput.UN_PROTECTED;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.*;
import net.flex.dci.otn.controller.allocate.common.util.CommonUtils;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.RouteData;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelBindOutputData;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchOutput;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.OtRouteInfoNewOch;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfosBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.ThirdBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.LinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.LinksKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static net.flex.dci.otc.common.constants.Constants.OP_MODE;

@Service
@Slf4j
public class TunnelNewOchAllocate {

    private static final String L_PORT = "OTU-Line";

    @Autowired
    private OchLinkDao ochLinkDao;
    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private SiteLinkDao siteLinkDao;
    @Autowired
    private NodeUtils nodeUtils;
    @Autowired
    private OtReusedStrategy otReusedStrategy;
    @Autowired
    private OtNodeService otNodeService;
    @Autowired
    private NEInfoConfig neInfoConfig;
    @Autowired
    private TunnelUtils tunnelUtils;
    @Autowired
    private TunnelSiteAllocate tunnelSiteAllocate;
    @Autowired
    private OTAllocate otAllocate;
    @Autowired
    private LinkRepo linkRepo;
    @Autowired
    private XCRepo xcRepo;

    @Autowired
    private PhyLinkDao phyLinkDao;
    @Autowired
    private TunnelDao tunnelDao;
    @Autowired
    private RoadmService roadmService;


    private ImmutablePair<String, String> getThirdTpPair(Link ochLink, Och ochLinkAttr) throws NeDesignerException {
        String ochLinkId = ochLink.getLinkId().getValue();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route =
                ochLinkAttr.getExplictRoute().getRoute().get(0);
        if (!hasSecondaryLeg(route)) {
            throw new NeDesignerException("No secondary found for invalid OCH:" + ochLinkId);
        }

        Set<String> usedRouteTps = new HashSet<>();
        collectRouteTps(route.getPrimary().getExplicitRouteObjects(), usedRouteTps);
        collectRouteTps(route.getSecondary().getExplicitRouteObjects(), usedRouteTps);

        String sourceNodeId = ochLink.getSource().getSourceNode().getValue();
        String destNodeId = ochLink.getDestination().getDestNode().getValue();
        return ImmutablePair.of(findIdleApsLegDestination(sourceNodeId, usedRouteTps, 2, "third"),
                findIdleApsLegDestination(destNodeId, usedRouteTps, 2, "third"));
    }

    private ImmutablePair<String, String> getSecondaryTpPair(Link ochLink, Och ochLinkAttr) throws NeDesignerException {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route =
                ochLinkAttr.getExplictRoute().getRoute().get(0);
        if (route.getPrimary() == null || route.getPrimary().getExplicitRouteObjects() == null
                || route.getPrimary().getExplicitRouteObjects().isEmpty()) {
            throw new NeDesignerException("No primary found for invalid OCH:" + ochLink.getLinkId().getValue());
        }

        Set<String> usedRouteTps = new HashSet<>();
        collectRouteTps(route.getPrimary().getExplicitRouteObjects(), usedRouteTps);

        String sourceNodeId = ochLink.getSource().getSourceNode().getValue();
        String destNodeId = ochLink.getDestination().getDestNode().getValue();
        return ImmutablePair.of(findIdleApsLegDestination(sourceNodeId, usedRouteTps, 1, "secondary"),
                findIdleApsLegDestination(destNodeId, usedRouteTps, 1, "secondary"));
    }

    private void collectRouteTps(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects> eros,
                                 Set<String> usedRouteTps) {
        eros.stream()
                .flatMap(ero -> ero.getPathRouteObject().stream())
                .map(path -> path.getResourceType())
                .filter(resource -> resource instanceof Tp)
                .map(resource -> ((Tp) resource).getTpHop().getTpRef().getValue())
                .forEach(usedRouteTps::add);
    }

    private String findIdleApsLegDestination(String nodeId, Set<String> usedRouteTps, int legIndex,
                                             String legName) throws NeDesignerException {
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        List<String> matched = new ArrayList<>();
        for (CrossConnections xc : node.getAugmentation(Node1.class).getPhysical().getCrossConnections()) {
            if (xc.getAps() == null) {
                continue;
            }
            List<String> destinations = xc.getDestinationTp().stream()
                    .map(tp -> tp.getTpRef().getValue())
                    .collect(Collectors.toList());
            if (destinations.size() <= legIndex) {
                continue;
            }
            long usedDestinations = destinations.stream().limit(legIndex)
                    .filter(usedRouteTps::contains).count();
            if (usedDestinations < legIndex) {
                continue;
            }
            matched.add(destinations.get(legIndex));
        }
        if (matched.size() != 1) {
            throw new NeDesignerException(String.format(
                    "Cannot identify one APS XC for the %s leg on node %s, matched %d", legName, nodeId, matched.size()));
        }

        String legTpId = matched.get(0);
        TerminationPoint legTp = node.getTerminationPoint().stream()
                .filter(tp -> legTpId.equals(tp.getTpId().getValue()))
                .findFirst()
                .orElseThrow(() -> new NeDesignerException("Cannot find APS " + legName + " TP " + legTpId + " on node " + nodeId));
        ConnectionStatus connectionStatus = legTp.getAugmentation(TerminationPoint1.class)
                .getPhysical().getConnectionStatus();
        if (!ConnectionStatus.Idle.equals(connectionStatus)) {
            throw new NeDesignerException("APS " + legName + " TP is not idle: " + legTpId);
        }
        return legTpId;
    }

    private boolean hasSecondaryLeg(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route) {
        return route.getSecondary() != null
                && route.getSecondary().getExplicitRouteObjects() != null
                && !route.getSecondary().getExplicitRouteObjects().isEmpty();
    }

    private List<Node> getReusedRegNodesInDb(SiteLinkRoute bindingRoute, Och ochLinkAttr) {
        List<String> cardTypes = Collections.singletonList(ochLinkAttr.getCardType());
        List<String> portTypes = Collections.singletonList(L_PORT);
        Set<String> excludeNodeIds = Collections.emptySet();
        List<Node> reusedRegNodes = new ArrayList<>();

        // Bind-tunnel may create or reuse REG electrical nodes only for sites explicitly selected as REG.
        addReusedRegNodes(bindingRoute.getPrimaryReg(), ochLinkAttr, cardTypes, portTypes, excludeNodeIds,
                reusedRegNodes);
        addReusedRegNodes(bindingRoute.getSecondaryReg(), ochLinkAttr, cardTypes, portTypes, excludeNodeIds,
                reusedRegNodes);
        addReusedRegNodes(bindingRoute.getThirdReg(), ochLinkAttr, cardTypes, portTypes, excludeNodeIds,
                reusedRegNodes);
        return reusedRegNodes;
    }

    private void addReusedRegNodes(List<? extends RegSiteInfo> regSites, Och ochLinkAttr, List<String> cardTypes,
                                   List<String> portTypes, Set<String> excludeNodeIds, List<Node> reusedRegNodes) {
        if (regSites == null || regSites.isEmpty()) {
            return;
        }
        // PhyNodeDao's REG-reuse query expands planeId through the subnet tree and cannot accept null.
        // A legacy OCH without planeId must allocate a fresh REG node instead of reusing an
        // unconstrained node from another plane/risk group.
        if (ochLinkAttr.getPlaneId() == null || ochLinkAttr.getPlaneId().trim().isEmpty()) {
            log.info("Skip REG node reuse because OCH planeId is empty, planeName={}, riskGroupName={}",
                    ochLinkAttr.getPlaneName(), ochLinkAttr.getRiskGroupName());
            return;
        }
        for (RegSiteInfo regSite : regSites) {
            if (!LinkTerminationNodeType.REG.name().equals(regSite.getType())) {
                continue;
            }
            try {
                reusedRegNodes.addAll(otReusedStrategy.getReusedNodePoolFromDbReg(regSite.getSiteId(),
                        ochLinkAttr.getVendorName(), cardTypes, portTypes, ochLinkAttr.getPlaneId(),
                        ochLinkAttr.getRiskGroupName(), excludeNodeIds));
            } catch (NullPointerException e) {
                // Some legacy dumps contain a plane-id but no matching subnet-tree node. The db-mongo
                // reuse query dereferences getAllDescendants(planeId) without a null guard. Treat that
                // as "no reusable REG"; allocation below will create a correctly scoped fresh REG.
                log.warn("Skip REG node reuse because subnet tree is missing, siteId={}, planeId={}",
                        regSite.getSiteId(), ochLinkAttr.getPlaneId());
            }
        }
    }

    public TunnelBindOutputData allocateBinding(String tunnelId, SiteLinkRoute bindingRoute) throws NeDesignerException {

        // Binding recomputes from the UI-selected siteLink route; WSS links are expanded by REG/ROADM allocation.
        List<String> siteLinks = bindingRoute.getPrimary();
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        Map<String, Link> inMemorySiteLink = new HashMap<>();

        Link ochLink = ochLinkDao.getOchLinkByTunnelId(tunnelId);
        Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

        WDM_Band wdmBand = getWdmBand(ochLink.getSupportingLink());
        List<Node> reusedRegNodesInDb = getReusedRegNodesInDb(bindingRoute, ochLinkAttr);
        TunnelNewOchInput newOchInput = TunnelNewOchInput.builder()
//                    .op6CardType(param.getOpCardType())
                .cardType(ochLinkAttr.getCardType())
                .tunnelSignalRate(tunnel.getSignalRate())
                .clientMediumA(tunnel.getClientPhysicalMediumA())
                .clientMediumZ(tunnel.getClientPhysicalMediumZ())
                .lineSignalRate(getLineSignalRate(ochLink))
                .siteLinkRoute(bindingRoute)
                .vendorName(ochLinkAttr.getVendorName())
                .vendorType(ochLinkAttr.getProductType())
                .tunnelNumber(1)
                .tunnelNumberPerOch(1)
                .ochNumber(1)
                .totalInMemoryNode(Collections.EMPTY_MAP)
                .riskGroupName(ochLinkAttr.getRiskGroupName())
                .plane(ochLinkAttr.getPlaneName())
                .planeId(ochLinkAttr.getPlaneId())
                .srcSite(PhysicalTpIdNamingRule.getSiteId(ochLink.getSource().getSourceTp().getValue()))
                .destSite(PhysicalTpIdNamingRule.getSiteId(ochLink.getDestination().getDestTp().getValue()))
                .grid(GridType.valueOf("_" + bindingRoute.getFixGrid()))
                .reusedNodesInDb(reusedRegNodesInDb)
                .reusedIncludeNodes(Collections.EMPTY_LIST)
                .isReusedMixed(false)
                .serviceType(tunnel.getServiceType())
                .opMode(PropertyTool.getValue(ochLinkAttr.getProperties(), OP_MODE))
                .excludeNodes(Collections.EMPTY_SET)
                .wdmBand(wdmBand)
                .build();

        String srcSiteId = newOchInput.getSrcSite();
        String destSiteId = newOchInput.getDestSite();

        Available available = new AvailableBuilder()
                .setLowerFrequency(ochLinkAttr.getLowerFrequency())
                .setUpperFrequency(ochLinkAttr.getUpperFrequency())
                .build();

        String frequencyString = tunnelUtils.getTpSlotFrequencyString(available);
        BigInteger cenFrequency = tunnelUtils.getCentFreq(available);
        Map<String, Node> inMemoryNode = new HashMap<>();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route ochRoute =
                ochLinkAttr.getExplictRoute().getRoute().get(0);
        boolean bindingThirdLeg = hasSecondaryLeg(ochRoute);
        ImmutablePair<String, String> bindingTpPair = bindingThirdLeg
                ? getThirdTpPair(ochLink, ochLinkAttr)
                : getSecondaryTpPair(ochLink, ochLinkAttr);

        String bindingTpStart = bindingTpPair.left;
        String bindingTpEnd = bindingTpPair.right;
        String preferredMdPortName = getPreferredMdPortNameFromAroute(ochLinkAttr);

        // Keep the selected binding route/REG context while letting Bone2.0 choose its protection route objects.
        NeInfo tpcNeInfo = neInfoConfig.getNeInfo(ochLinkAttr.getVendorName(), ochLinkAttr.getProductType(), NodeType.TD.name());
        TempInfo bindingInfo = allocateSecondary(newOchInput, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, siteLinks,
                newOchInput.getPrimaryRoute(), cenFrequency.longValue(), frequencyString, bindingTpStart,
                bindingTpEnd,
                bindingThirdLeg ? TunnelSiteAllocate.ROUTE_LEG_THIRD : TunnelSiteAllocate.ROUTE_LEG_SECONDARY,
                bindingThirdLeg ? preferredMdPortName : null);

        // The APS XC belongs to the existing primary route; the new third route does not contain one.
        Set<String> apsXcIds = getPrimaryRouteApsXcIds(ochLinkAttr);
        updateApsMemberEnableOnNodes(inMemoryNode, apsXcIds, true);

        //output init
        return TunnelBindOutputData.builder()
                .links(bindingInfo.links)
                .routeLinks(bindingInfo.routeLinks)
                .nodes(new ArrayList<>(inMemoryNode.values()))
                .xcs(bindingInfo.ochXcs)
                .siteLinks(bindingInfo.nodeSiteLinkSnapshot)
                .ochLink(ochLink)
                .removedResourceIds(bindingInfo.removedResourceIds)
                .thirdSourceTp(bindingTpStart)
                .thirdDestTp(bindingTpEnd)
                .build();
    }

    private WDM_Band getWdmBand(List<SupportingLink> supportingLinks) {
        String siteLinkId = supportingLinks.stream().map(sl -> sl.getLinkRef().getValue())
                .filter(x -> SiteLinkIdNamingRule.isSiteLink(x))
                .findAny().orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "OchLink should include at least one siteLink"));

        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        if (siteLink == null) {
            new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Cannot find required siteLink " + siteLinkId);
        }
        return CommonUtils.getWDMBand(
                siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite());
    }

    private Class<? extends SignalProtocolType> getLineSignalRate(Link ochLink) throws NeDesignerException {
        String sourceNodeId = ochLink.getSource().getSourceNode().getValue();
        String sourceTpId = ochLink.getSource().getSourceTp().getValue();
        Node sourceNode = phyNodeDao.getConfigPhyNodeById(sourceNodeId);
        TerminationPoint sourceTp = sourceNode.getTerminationPoint().stream()
                .filter(tp -> sourceTpId.equals(tp.getTpId().getValue()))
                .findFirst()
                .orElseThrow(() -> new NeDesignerException("Cannot find OCH source TP " + sourceTpId));
        return sourceTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine().getSignalRate();
    }

    private String getPreferredMdPortNameFromAroute(Och ochLinkAttr) {
        try {
            List<ExplicitRouteObjects> eros = ochLinkAttr.getExplictRoute()
                    .getRoute()
                    .get(0)
                    .getPrimary()
                    .getExplicitRouteObjects();
            for (ExplicitRouteObjects ero : eros) {
                for (PathRouteObject path : ero.getPathRouteObject()) {
                    if (!(path.getResourceType() instanceof Tp)) {
                        continue;
                    }
                    String tpId = ((Tp) path.getResourceType()).getTpHop().getTpRef().getValue();
                    String portName = PhysicalTpIdNamingRule.getPortNameByTpId(tpId);
                    if (isMdPortName(portName)) {
                        // The added third leg keeps the M?D? index observed on the existing A route when possible.
                        return portName;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Cannot identify preferred M?D? port from OCH A route, fallback to normal picking.", e);
        }
        return null;
    }

    private boolean isMdPortName(String portName) {
        return portName != null && portName.matches("(?i)M\\d+D\\d+");
    }

    public TunnelNewOchOutput allocate(TunnelNewOchInput input) throws NeDesignerException {
        //output init
        List<Node> nodeSnapshots = new ArrayList<>();
        List<Link> siteLinkSnapshot = new ArrayList<>();
        Map<String, Node> inMemoryNode = new HashMap<>();
        Map<String, Link> inMemorySiteLink = new HashMap<>();
        List<TunnelRouteInfos> tunnelRouteInfoList = new ArrayList<>();
        List<String> removedResourceIds = new ArrayList<>();

        //input
        NeInfo tpcNeInfo = neInfoConfig.getNeInfo(input.getVendorName(), input.getVendorType(), NodeType.TD.name());
        String srcSiteId = input.getSrcSite();
        String destSiteId = input.getDestSite();

        //prepare
        inMemoryNode.putAll(input.getTotalInMemoryNode());

        Integer ochNumber = input.getOchNumber();
        Integer tunnelNumberPerOch = input.getTunnelNumberPerOch();
        Integer tunnelNumberLastOch = input.getTunnelNumber() % input.getTunnelNumberPerOch();
        if (tunnelNumberLastOch == 0) {
            tunnelNumberLastOch = tunnelNumberPerOch;//刚好每条och都是满配业务。
        }
        List<Long> cenFrequenciesAvailable = input.getSiteLinkRoute().getCentralFrequencies();

        RouteData primaryRoute = input.getPrimaryRoute();
        RouteData secondaryRoute = input.getSecondaryRoute();
        RouteData thirdRoute = input.getThirdRoute();
        String protectionTypeOch = input.getProtectionType();
        List<Long> usedCentralFrequencies = new ArrayList<>();

        for (int i = 0; i < ochNumber; i++) {
            //prepare
            Long cenFrequency = cenFrequenciesAvailable.get(i);
            usedCentralFrequencies.add(cenFrequency);
            Available freeFrequency = tunnelUtils.convertToUpperLowerFreq(input.getGrid(), cenFrequency);
            //同一条OCH上面所有需要frequencyString的交叉，都用这个
            String frequencyString = tunnelUtils.getTpSlotFrequencyString(freeFrequency);//e.g. /frequency=196025000,19675000
            Integer tunnelToCreate = i == ochNumber - 1 ? tunnelNumberLastOch : tunnelNumberPerOch;

            TempInfo primaryInfo = null;
            TempInfo secondaryInfo = null;
            TempInfo thirdInfo = null;
            switch (protectionTypeOch) {
                case UN_PROTECTED:
                    primaryInfo = allocateNonProtected(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, primaryRoute, cenFrequency, frequencyString, tunnelToCreate);
                    break;
                case PROTECTED_1TO1:
                    primaryInfo = allocatePrimary(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, primaryRoute.getLinkList(), primaryRoute, cenFrequency, frequencyString,
                            tunnelToCreate);
                    secondaryInfo = allocateSecondary(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, secondaryRoute.getLinkList(), secondaryRoute, cenFrequency,
                            frequencyString,
                            primaryInfo.secondaryTpStart,
                            primaryInfo.secondaryTpEnd, TunnelSiteAllocate.ROUTE_LEG_SECONDARY, null);
                    break;
                case PROTECTED_1TO2:
                    primaryInfo = allocatePrimary(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, primaryRoute.getLinkList(), primaryRoute, cenFrequency, frequencyString,
                            tunnelToCreate);
                    secondaryInfo = allocateSecondary(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, secondaryRoute.getLinkList(), secondaryRoute, cenFrequency,
                            frequencyString,
                            primaryInfo.secondaryTpStart,
                            primaryInfo.secondaryTpEnd, TunnelSiteAllocate.ROUTE_LEG_SECONDARY, null);
                    thirdInfo = allocateSecondary(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, thirdRoute.getLinkList(), thirdRoute, cenFrequency, frequencyString,
                            primaryInfo.thirdTpStart,
                            primaryInfo.thirdTpEnd, TunnelSiteAllocate.ROUTE_LEG_THIRD, null);
                    break;
                default:
                    throw new NeDesignerException("Unsupported protection type: " + protectionTypeOch);

            }

            if (ProtectionBidir1To2.class.equals(input.getRequestedProtectionType())) {
                // APS XC exists only in primary; create-tunnel-3 persists this route model into the node.
                updatePrimaryApsMemberEnable(primaryInfo, thirdInfo != null);
            }

            //build output
            tunnelRouteInfoList.add(buildOutPut(primaryInfo, secondaryInfo, thirdInfo));
            nodeSnapshots.addAll(primaryInfo.nodeSnapshots);
            siteLinkSnapshot.addAll(primaryInfo.nodeSiteLinkSnapshot);
            removedResourceIds.addAll(primaryInfo.removedResourceIds);

            if (secondaryInfo != null) {
                nodeSnapshots.addAll(secondaryInfo.nodeSnapshots);
                siteLinkSnapshot.addAll(secondaryInfo.nodeSiteLinkSnapshot);
                removedResourceIds.addAll(secondaryInfo.removedResourceIds);
            }
            if (thirdInfo != null) {
                nodeSnapshots.addAll(thirdInfo.nodeSnapshots);
                siteLinkSnapshot.addAll(thirdInfo.nodeSiteLinkSnapshot);
                removedResourceIds.addAll(thirdInfo.removedResourceIds);
            }
        }

        return TunnelNewOchOutput.builder()
                .inMemoryNode(inMemoryNode)
                .nodeSnapshot(nodeSnapshots)
                .siteLinkSnapshot(siteLinkSnapshot)
                .tunnelRouteInfos(tunnelRouteInfoList)
                .usedCentralFrequencies(usedCentralFrequencies)
                .removedResourceIds(removedResourceIds)
                .build();
    }

    private void updatePrimaryApsMemberEnable(TempInfo primaryInfo, boolean cEnable) {
        primaryInfo.ochXcs = primaryInfo.ochXcs.stream()
                .map(xc -> xc.getAps() == null ? xc : xcRepo.updateApsMemberEnable(xc, cEnable))
                .collect(Collectors.toList());
    }

    private Set<String> getPrimaryRouteApsXcIds(Och och) {
        Set<String> apsXcIds = new HashSet<>();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route =
                och.getExplictRoute().getRoute().get(0);
        route.getPrimary().getCrossConnections().stream()
                .filter(xc -> xc.getAps() != null)
                .forEach(xc -> apsXcIds.add(xc.getCrossConnectionId().getValue()));
        return apsXcIds;
    }

    //绑定3rd腿的时候把APS 的C口 c.enable=true
    private void updateApsMemberEnableOnNodes(Map<String, Node> inMemoryNode, Set<String> apsXcIds,
                                               boolean cEnable) {
        if (apsXcIds.isEmpty()) {
            return;
        }
        inMemoryNode.replaceAll((nodeId, node) -> {
            List<CrossConnections> xcs = node.getAugmentation(Node1.class).getPhysical()
                    .getCrossConnections();
            if (xcs.stream().noneMatch(xc -> apsXcIds.contains(xc.getCrossConnectionId().getValue()))) {
                return node;
            }
            List<CrossConnections> updatedXcs = xcs.stream()
                    .map(xc -> apsXcIds.contains(xc.getCrossConnectionId().getValue())
                            ? xcRepo.updateApsMemberEnable(xc, cEnable) : xc)
                    .collect(Collectors.toList());
            // Keep the persisted node model consistent with the APS XC in the allocate result.
            return new NodeBuilder(node)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                                    .setCrossConnections(updatedXcs)
                                    .build())
                            .build())
                    .build();
        });
    }

    /**
     * 哪个小用哪个，有以下两种场景：
     * <p>
     * 1. 比如L口同时支持200G，400G的，有4个100G的C口， 此时，如果业务选择200G，则返回的是clientLineRate；
     * <p>
     * 2. 比如L口支持200G， 部分C口支持10G，此时则返回满足条件的availableCPortNumber；
     *
     * @param input
     * @param tpcNeInfo
     * @return
     * @throws NeDesignerException
     */
    private Integer getTunnelNumberPerOch(TunnelNewOchInput input, NeInfo tpcNeInfo) throws NeDesignerException {
        Card otCardInfo = tpcNeInfo.getCardByCardType(input.getCardType());
        Integer availableCPortNumber = NeInfoUtil.getCPortNumberByTunnelSignalRate(otCardInfo, input.getTunnelSignalRate());
        return 0;
//        Integer clientLineRate = input.getClientLineRate();
//        return clientLineRate < availableCPortNumber?clientLineRate:
//        availableCPortNumber;

    }

    private TunnelRouteInfos buildOutPut(TempInfo primaryInfo, TempInfo secondaryInfo, TempInfo thirdInfo) throws NeDesignerException {

        //build tpc route
        List<TpcRoute> tpcRoutes = new ArrayList<>();

        List<CrossConnections> tpcXcsStart = primaryInfo.tpcXcsStart;
        int tpcXcSize = tpcXcsStart.size();
        List<CrossConnections> tpcXcsEnd = primaryInfo.tpcXcsEnd;
        if (tpcXcSize != tpcXcsEnd.size()) {
            log.error("tpcStart xc has size {} ,while tpcEnd xc has size {}.", tpcXcSize, tpcXcsEnd.size());
            throw new NeDesignerException("Internal error for allocate tunnel, because TPC XC size is not the same between source and dest.");
        }

        for (int x = 0; x < tpcXcSize; x++) {
            CrossConnections tpcXcStart = tpcXcsStart.get(x);
            CrossConnections tpcXcEnd = tpcXcsEnd.get(x);

            TpcRoute tpcRoute = new TpcRouteBuilder()
                    .setSourceTp(tpcXcStart.getSourceTp().get(0).getTpRef().getValue())
                    .setDestTp(tpcXcEnd.getSourceTp().get(0).getTpRef().getValue())
                    .setCrossConnections(Arrays.asList(tpcXcStart, tpcXcEnd)).build();
            tpcRoutes.add(tpcRoute);
        }

        String ochSrcTp = tpcXcsStart.get(0).getDestinationTp().get(0).getTpRef().getValue();
        String ochDstTp = tpcXcsEnd.get(0).getDestinationTp().get(0).getTpRef().getValue();

        //build och route
        Primary primary = new PrimaryBuilder().setCrossConnections(primaryInfo.ochXcs).setLinks(yangConvertToLinksList(primaryInfo.links)).build();

        Secondary secondary = null;
        if (secondaryInfo != null) {
            secondary = new SecondaryBuilder().setCrossConnections(secondaryInfo.ochXcs).setLinks(yangConvertToLinksList(secondaryInfo.links)).build();
        }

        Third third = null;
        if (thirdInfo != null) {
            third = new ThirdBuilder().setCrossConnections(thirdInfo.ochXcs).setLinks(yangConvertToLinksList(thirdInfo.links)).build();
        }
        OchRoute ochRoute = new OchRouteBuilder().setSourceTp(ochSrcTp).setDestTp(ochDstTp).setPrimary(primary).setSecondary(secondary).setThird(third).build();

        //build output
        return new TunnelRouteInfosBuilder().setOchRoute(ochRoute).setTpcRoute(tpcRoutes).build();
    }

    private List<Links> yangConvertToLinksList(List<Link> links) {
        if (links == null) {
            return null;
        }

        return links.stream().map(item -> yangConvertToLinks(item)).collect(Collectors.toList());
    }

    private Links yangConvertToLinks(Link link) {
        return new LinksBuilder().setLinkId(link.getLinkId())
                .setKey(new LinksKey(link.getKey().getLinkId()))
                .setSource(link.getSource())
                .setDestination(link.getDestination())
                .setPhysical(link.getAugmentation(Link1.class).getPhysical())
                .build();
    }

    private TempInfo allocateSecondary(TunnelNewOchInput input, Map<String, Node> inMemoryNode, Map<String, Link> inMemorySiteLink, NeInfo tpcNeInfo, String srcSiteId, String destSiteId,
            List<String> secondaryRoute, RouteData route,
            Long cenFrequency,
            String frequencyString, String secondaryTpStart, String secondaryTpEnd, String routeLeg,
            String preferredMdPortName) throws NeDesignerException {
        int secondarySize = secondaryRoute.size();
        if (secondarySize == 1) {
            return allocateSecondaryP2P(input, inMemoryNode, tpcNeInfo, srcSiteId, destSiteId,
                    secondaryRoute.get(0), cenFrequency, frequencyString, secondaryTpStart, secondaryTpEnd,
                    routeLeg, preferredMdPortName);
        }
        return allocateSecondaryNetWork(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, secondaryRoute, route, cenFrequency, frequencyString, secondaryTpStart,
                secondaryTpEnd, routeLeg, preferredMdPortName);

    }

    private TempInfo allocateSecondaryNetWork(TunnelNewOchInput input, Map<String, Node> inMemoryNode, Map<String, Link> inMemorySiteLink, NeInfo tpcNeInfo, String srcSiteId, String destSiteId,
            List<String> secondaryRoute,
            RouteData route, Long cenFrequency,
            String frequencyString, String secondaryTpStart, String secondaryTpEnd, String routeLeg,
            String preferredMdPortName) throws NeDesignerException {
        int secondarySize = secondaryRoute.size();
        List<Node> secondaryNodeSnapshots = new ArrayList<>();
        List<Link> secondarySiteLinkSnapshot = new ArrayList<>();
        List<CrossConnections> secondaryXcs = new ArrayList<>();
        List<CrossConnections> endOchExpXcs = new ArrayList<>();
        List<Link> secondaryLinks = new ArrayList<>();
        List<Link> completeRouteLinks = new ArrayList<>();
        List<Link> endLinks = new ArrayList<>();
        String startMuxMdPortTp = null;
        String endMuxMdPortTp = null;
        List<String> removedResourceIds = new ArrayList<>();

        int s = 0;
        while (s < secondarySize) {
//        for (int s = 0; s < secondarySize; s += 1) {
            String siteLinkId = secondaryRoute.get(s);
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            secondarySiteLinkSnapshot.add(siteLink);
            validateSiteLinkAvailableForOch(input, siteLink, siteLinkId, cenFrequency);

            String preWssLinkId = null;
//            String preWssLinkId = s == 0 ? null : secondaryRoute.get(s - 1);
//            String preWssTp = preWssLinkId == null ? null : getOwnWssTp(preWssLinkId, siteLinkId);
            String nextWssLinkId = null;
//            String nextWssLinkId = s == secondarySize - 1 ? null : secondaryRoute.get(s + 1);
//            String nextWssTp = nextWssLinkId == null ? null : getOwnWssTp(nextWssLinkId, siteLinkId);

            //起始点,终点： 需要额外创建OS link
            if (s == 0 || s == secondarySize - 1) {
                String siteId = s == 0 ? srcSiteId : destSiteId;
                String siteLinkNodeId = getNodeIdBySiteId(siteLink, siteId);

                Node siteLinkNode;
                if (inMemoryNode.containsKey(siteLinkNodeId)) {
                    siteLinkNode = inMemoryNode.get(siteLinkNodeId);
                } else {
                    siteLinkNode = phyNodeDao.getConfigPhyNodeById(siteLinkNodeId);
                    secondaryNodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNode));
                }

                //note：所有node相关的改变都在OT的allocate，复用段的allocate不改变node
                if (s == 0) {

                    OtRouteInfoNewOch allocateRouteInfo = otAllocate.allocateOtStartNewOchSecondary(secondaryTpStart, input, cenFrequency, inMemoryNode, frequencyString, siteLinkNode, siteLink,
                            preferredMdPortName);
                    secondaryLinks.addAll(allocateRouteInfo.getOsLinks());
                    completeRouteLinks.addAll(allocateRouteInfo.getOsLinks());
                    startMuxMdPortTp = allocateRouteInfo.getMuxMdPortTpSrcPair().getLeft();
                    secondaryXcs.addAll(allocateRouteInfo.getOchXcs());
                } else {

                    OtRouteInfoNewOch allocateRouteInfo = otAllocate.allocateOtEndNewOchSecondary(secondaryTpEnd, input, cenFrequency, inMemoryNode, frequencyString, siteLinkNode,
                            siteLink, preferredMdPortName);
                    secondaryLinks.addAll(allocateRouteInfo.getOsLinks());
                    endLinks.addAll(allocateRouteInfo.getOsLinks());
                    endMuxMdPortTp = allocateRouteInfo.getMuxMdPortTpSrcPair().getLeft();
                    endOchExpXcs.addAll(allocateRouteInfo.getOchXcs());

                }
            }

            //allocate XC for sitelink node
            List<CrossConnections> newSiteLinkXcs = tunnelSiteAllocate.allocateSegmentNetwork(startMuxMdPortTp, endMuxMdPortTp, siteLink, frequencyString, cenFrequency, preWssLinkId, nextWssLinkId, routeLeg);
            secondaryXcs.addAll(newSiteLinkXcs);
            secondaryXcs.addAll(endOchExpXcs);
            completeRouteLinks.add(siteLink);

            //handle REG
            if (s == secondarySize - 1) {//最后一条，不会有reg了
                break;
            }
            String nextSiteLinkId = secondaryRoute.get(s + 1);
            if (tunnelUtils.isWssLink(siteLinkId)) {
                s = s + 2;
                continue;//todo: 等做roadm再处理
            }
            String siteId = tunnelUtils.getRegSite(siteLinkId, nextSiteLinkId);
            OtRouteInfoNewOch regRouteInfoNewOch = allocateRegOrRoadm(input, route, tpcNeInfo, siteLinkId,
                    nextSiteLinkId, siteId, inMemoryNode, inMemorySiteLink, frequencyString, cenFrequency, preferredMdPortName);
            removedResourceIds.addAll(regRouteInfoNewOch.getRemovedResourceIds());
            inMemoryNode = regRouteInfoNewOch.getInMemoryNodes();
//            tpcXcsStart.addAll(regRouteInfoNewOch.getTpcXcs());
            secondaryLinks.addAll(regRouteInfoNewOch.getOsLinks());
            completeRouteLinks.addAll(regRouteInfoNewOch.getOsLinks());
            secondaryNodeSnapshots.addAll(regRouteInfoNewOch.getSnapshotNodes());
            secondaryXcs.addAll(regRouteInfoNewOch.getOchXcs());
            s++;

        }

        completeRouteLinks.addAll(endLinks);
        TempInfo result = new TempInfo(secondaryNodeSnapshots, secondarySiteLinkSnapshot,
                secondaryXcs, secondaryLinks, removedResourceIds);
        result.routeLinks = completeRouteLinks;
        return result;//secondary没有TPC XC
    }

    private TempInfo allocateSecondaryP2P(TunnelNewOchInput input, Map<String, Node> inMemoryNode, NeInfo tpcNeInfo, String srcSiteId, String destSiteId, String siteLinkId, Long cenFrequency,
            String frequencyString,
            String secondaryTpStart, String secondaryTpEnd, String routeLeg,
            String preferredMdPortName) throws NeDesignerException {

        List<Node> secondaryNodeSnapshots = new ArrayList<>();
        List<Link> secondarySiteLinkSnapshot = new ArrayList<>();
        List<CrossConnections> secondaryXcs = new ArrayList<>();
        List<Link> secondaryLinks = new ArrayList<>();

        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        secondarySiteLinkSnapshot.add(siteLink);
        validateSiteLinkAvailableForOch(input, siteLink, siteLinkId, cenFrequency);

        //起始点,终点： 需要额外创建OS link
        String siteLinkNodeIdSrc = getNodeIdBySiteId(siteLink, srcSiteId);
        String siteLinkNodeIdDst = getNodeIdBySiteId(siteLink, destSiteId);

        Node siteLinkNodeSrc;
        if (inMemoryNode.containsKey(siteLinkNodeIdSrc)) {
            siteLinkNodeSrc = inMemoryNode.get(siteLinkNodeIdSrc);
        } else {
            siteLinkNodeSrc = phyNodeDao.getConfigPhyNodeById(siteLinkNodeIdSrc);
            secondaryNodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNodeSrc));
        }

        Node siteLinkNodeDst;
        if (inMemoryNode.containsKey(siteLinkNodeIdDst)) {
            siteLinkNodeDst = inMemoryNode.get(siteLinkNodeIdDst);
        } else {
            siteLinkNodeDst = phyNodeDao.getConfigPhyNodeById(siteLinkNodeIdDst);
            secondaryNodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNodeDst));
        }

        //note：所有node相关的改变都在OT的allocate，复用段的allocate不改变node
        OtRouteInfoNewOch allocatePairStart = otAllocate.allocateOtStartNewOchSecondary(secondaryTpStart, input, cenFrequency, inMemoryNode, frequencyString, siteLinkNodeSrc, siteLink,
                preferredMdPortName);
        secondaryLinks.addAll(allocatePairStart.getOsLinks());
        String muxMdPortTpSrc = allocatePairStart.getMuxMdPortTpSrcPair().getLeft();
        secondaryXcs.addAll(allocatePairStart.getOchXcs());

        OtRouteInfoNewOch allocatePairEnd = otAllocate.allocateOtEndNewOchSecondary(secondaryTpEnd, input, cenFrequency, inMemoryNode, frequencyString, siteLinkNodeDst, siteLink,
                preferredMdPortName);
        secondaryLinks.addAll(allocatePairEnd.getOsLinks());
        String muxMdPortTpDst = allocatePairEnd.getMuxMdPortTpSrcPair().getLeft();

        //allocate XC for sitelink node
        //todo: for mpo4, new wss xc need
        List<CrossConnections> newSiteLinkXcs = tunnelSiteAllocate.allocateSegmentNetwork(muxMdPortTpSrc, muxMdPortTpDst, siteLink, frequencyString, cenFrequency, null, null, routeLeg);
        secondaryXcs.addAll(newSiteLinkXcs);
        secondaryXcs.addAll(allocatePairEnd.getOchXcs());

        List<Link> completeRouteLinks = new ArrayList<>(allocatePairStart.getOsLinks());
        completeRouteLinks.add(siteLink);
        completeRouteLinks.addAll(allocatePairEnd.getOsLinks());
        TempInfo result = new TempInfo(secondaryNodeSnapshots, secondarySiteLinkSnapshot,
                secondaryXcs, secondaryLinks, Collections.emptyList());
        result.routeLinks = completeRouteLinks;
        return result;//secondary没有TPC XC

    }

    private void validateSiteLinkAvailableForOch(TunnelNewOchInput input, Link siteLink, String siteLinkId,
                                                 Long cenFrequency) throws NeDesignerException {
        Available requiredFrequency = tunnelUtils.convertToUpperLowerFreq(input.getGrid(), cenFrequency);
        long lowerFrequency = requiredFrequency.getLowerFrequency().getValue().longValue();
        long upperFrequency = requiredFrequency.getUpperFrequency().getValue().longValue();

        // Binding and secondary allocation must validate the real OCH spectrum,
        // not a regenerated center-frequency list with different C+L alignment.
        if (!CommonUtils.isSiteLinkAvailableForOch(siteLink, lowerFrequency, upperFrequency)) {
            String msg = String.format("SiteLink %s cannot carry OCH frequency %d.", siteLinkId, cenFrequency);
            log.error(msg);
            throw new NeDesignerException(msg);
        }
    }


    private TempInfo allocatePrimary(TunnelNewOchInput input, Map<String, Node> inMemoryNode, Map<String, Link> inMemorySiteLink, NeInfo tpcNeInfo, String srcSiteId, String destSiteId,
            List<String> primaryRoute,
            RouteData route, Long cenFrequency, String frequencyString, Integer tunnelToCreate) throws NeDesignerException {
        int primarySize = primaryRoute.size();
        if (primarySize == 1) {
            return allocatePrimaryP2P(input, inMemoryNode, tpcNeInfo, srcSiteId, destSiteId, primaryRoute.get(0), cenFrequency, frequencyString, tunnelToCreate);
        }
        return allocatePrimaryNetWork(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, primaryRoute, route, cenFrequency, frequencyString, tunnelToCreate);
    }

    private TempInfo allocatePrimaryP2P(TunnelNewOchInput input, Map<String, Node> inMemoryNode, NeInfo tpcNeInfo, String srcSiteId, String destSiteId, String siteLinkId, Long cenFrequency,
            String frequencyString, Integer tunnelToCreate) throws NeDesignerException {

        List<CrossConnections> primaryTpcXcsStart = new ArrayList<>();
        List<CrossConnections> primaryTpcXcsEnd = new ArrayList<>();
        List<CrossConnections> primaryOchXcs = new ArrayList<>();
        List<Link> primaryLinks = new ArrayList<>();
        List<Node> primaryNodeSnapshots = new ArrayList<>();
        List<Link> primarySiteLinkSnapshot = new ArrayList<>();

        String secondaryTpStart = null;
        String secondaryTpEnd = null;

        List<CrossConnections> endOchXcs = null;
        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        primarySiteLinkSnapshot.add(siteLink);
        //validate siteLink frequency
//        List<Long> centralFrequencies;
//        try {
//            WDM_Band band = WDM_Band.fromString(tpcNeInfo.getCardByCardType(input.getCardType()).getWdmBand());
//            centralFrequencies = FrequencyAvailable.getFreeCentFrequency(Arrays.asList(siteLink), band, input.getGrid());
//        } catch (Exception e) {
//            String msg = "Failed to get centralFrequencies for link: " + siteLinkId;
//            log.error("Failed to get centralFrequencies for link:{}", siteLink, e);
//            throw new NeDesignerException(msg);
//        }
//        if (!centralFrequencies.contains(cenFrequency)) {
//            String msg = String.format("SiteLink %s has changed after tunnel computing, frequency %d is not available now, re-compute again.", siteLinkId, cenFrequency);
//            log.error(msg);
//            throw new NeDesignerException(msg);
//        }

        //起始点,终点： 需要额外创建TPC的交叉和OS link
        String siteLinkNodeIdSrc = getNodeIdBySiteId(siteLink, srcSiteId);
        String siteLinkNodeIdDst = getNodeIdBySiteId(siteLink, destSiteId);

        Node siteLinkNodeSrc;
        if (inMemoryNode.containsKey(siteLinkNodeIdSrc)) {
            siteLinkNodeSrc = inMemoryNode.get(siteLinkNodeIdSrc);
        } else {
            siteLinkNodeSrc = phyNodeDao.getConfigPhyNodeById(siteLinkNodeIdSrc);
            primaryNodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNodeSrc));
        }

        Node siteLinkNodeDst;
        if (inMemoryNode.containsKey(siteLinkNodeIdDst)) {
            siteLinkNodeDst = inMemoryNode.get(siteLinkNodeIdDst);
        } else {
            siteLinkNodeDst = phyNodeDao.getConfigPhyNodeById(siteLinkNodeIdDst);
            primaryNodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNodeDst));
        }

        //start
        OtRouteInfoNewOch otRouteInfoNewOchStart = otAllocate.allocateOtStartNewOchPrimary(tunnelToCreate, srcSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNodeSrc,
                siteLink);
        secondaryTpStart = otRouteInfoNewOchStart.getSecondaryTp();
        inMemoryNode = otRouteInfoNewOchStart.getInMemoryNodes();
        primaryTpcXcsStart.addAll(otRouteInfoNewOchStart.getTpcXcs());
        primaryOchXcs.addAll(otRouteInfoNewOchStart.getOchXcs());
        primaryLinks.addAll(otRouteInfoNewOchStart.getOsLinks());
        primaryNodeSnapshots.addAll(otRouteInfoNewOchStart.getSnapshotNodes());
        String muxMdPortTpSrc = otRouteInfoNewOchStart.getMuxMdPortTpSrcPair().getLeft();

        //end
        OtRouteInfoNewOch otRouteInfoNewOchEnd = otAllocate.allocateOtEndNewOchPrimary(tunnelToCreate, destSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNodeDst,
                siteLink);
        secondaryTpEnd = otRouteInfoNewOchEnd.getSecondaryTp();
        primaryTpcXcsEnd.addAll(otRouteInfoNewOchEnd.getTpcXcs());
        endOchXcs = otRouteInfoNewOchEnd.getOchXcs();
        primaryLinks.addAll(otRouteInfoNewOchEnd.getOsLinks());
        primaryNodeSnapshots.addAll(otRouteInfoNewOchEnd.getSnapshotNodes());
        String muxMdPortTpDst = otRouteInfoNewOchEnd.getMuxMdPortTpSrcPair().getLeft();

        //allocate XC for sitelink node
        List<CrossConnections> newSiteLinkXcs = tunnelSiteAllocate.allocateSegmentNetwork(muxMdPortTpSrc, muxMdPortTpDst, siteLink, frequencyString, cenFrequency, null, null);
        primaryOchXcs.addAll(newSiteLinkXcs);
        primaryOchXcs.addAll(endOchXcs);

        return new TempInfo(primaryNodeSnapshots, primarySiteLinkSnapshot, primaryTpcXcsStart, primaryTpcXcsEnd, primaryOchXcs, primaryLinks, Collections.emptyList(), secondaryTpStart, secondaryTpEnd,
                otRouteInfoNewOchStart.getThirdTp(), otRouteInfoNewOchEnd.getThirdTp());

    }

    private TempInfo allocatePrimaryNetWork(TunnelNewOchInput input, Map<String, Node> inMemoryNode, Map<String, Link> inMemorySiteLink, NeInfo tpcNeInfo, String srcSiteId, String destSiteId,
            List<String> primaryRoute,
            RouteData route, Long cenFrequency, String frequencyString, Integer tunnelToCreate) throws NeDesignerException {
        int primarySize = primaryRoute.size();

        List<CrossConnections> primaryTpcXcsStart = new ArrayList<>();
        List<CrossConnections> primaryTpcXcsEnd = new ArrayList<>();
        List<CrossConnections> primaryOchXcs = new ArrayList<>();
        List<Link> primaryLinks = new ArrayList<>();
        List<Node> primaryNodeSnapshots = new ArrayList<>();
        List<Link> primarySiteLinkSnapshot = new ArrayList<>();

        String secondaryTpStart = null;
        String secondaryTpEnd = null;
        String thirdTpStart = null;
        String thirdTpEnd = null;

        List<CrossConnections> endOchXcs = null;
        List<String> removedResourceIds = new ArrayList<>();
        int j = 0;
        while (j < primarySize) {
//        for (int j = 0; j < primarySize; j += 2) {
            String siteLinkId = primaryRoute.get(j);
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            primarySiteLinkSnapshot.add(siteLink);

//            String preWssLinkId = j == 0 ? null : primaryRoute.get(j - 1);
//            String nextWssLinkId = j == primarySize - 1 ? null : primaryRoute.get(j + 1);
            String preWssLinkId = null;
            String nextWssLinkId = null;
            String startMuxMdPortTp = null;
            String endMuxMdPortTp = null;

            //起始点,终点： 需要额外创建TPC的交叉和OS link
            if (j == 0 || j == primarySize - 1) {
                String siteId = j == 0 ? srcSiteId : destSiteId;
                String siteLinkNodeId = getNodeIdBySiteId(siteLink, siteId);

                Node siteLinkNode;
                if (inMemoryNode.containsKey(siteLinkNodeId)) {
                    siteLinkNode = inMemoryNode.get(siteLinkNodeId);
                } else {
                    siteLinkNode = phyNodeDao.getConfigPhyNodeById(siteLinkNodeId);
                    primaryNodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNode));
                }

                //note：所有node相关的改变都在OT的allocate，复用段的allocate不改变node
                if (j == 0) {

                    OtRouteInfoNewOch otRouteInfoNewOch = otAllocate.allocateOtStartNewOchPrimary(tunnelToCreate, srcSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString,
                            siteLinkNode, siteLink);
                    secondaryTpStart = otRouteInfoNewOch.getSecondaryTp();
                    thirdTpStart = otRouteInfoNewOch.getThirdTp();
                    inMemoryNode = otRouteInfoNewOch.getInMemoryNodes();
                    primaryTpcXcsStart.addAll(otRouteInfoNewOch.getTpcXcs());
                    primaryOchXcs.addAll(otRouteInfoNewOch.getOchXcs());
                    primaryLinks.addAll(otRouteInfoNewOch.getOsLinks());
                    primaryNodeSnapshots.addAll(otRouteInfoNewOch.getSnapshotNodes());
                    startMuxMdPortTp = otRouteInfoNewOch.getMuxMdPortTpSrcPair().getLeft();
                } else {
                    OtRouteInfoNewOch otRouteInfoNewOch = otAllocate.allocateOtEndNewOchPrimary(tunnelToCreate, destSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNode,
                            siteLink);
                    secondaryTpEnd = otRouteInfoNewOch.getSecondaryTp();
                    thirdTpEnd = otRouteInfoNewOch.getThirdTp();
                    primaryTpcXcsEnd.addAll(otRouteInfoNewOch.getTpcXcs());
                    endOchXcs = otRouteInfoNewOch.getOchXcs();
                    primaryLinks.addAll(otRouteInfoNewOch.getOsLinks());
                    primaryNodeSnapshots.addAll(otRouteInfoNewOch.getSnapshotNodes());
                    endMuxMdPortTp = otRouteInfoNewOch.getMuxMdPortTpSrcPair().getLeft();
                }

            }

            //allocate XC for sitelink node
            List<CrossConnections> newSiteLinkXcs = tunnelSiteAllocate.allocateSegmentNetwork(startMuxMdPortTp, endMuxMdPortTp, siteLink, frequencyString, cenFrequency, preWssLinkId, nextWssLinkId);
            primaryOchXcs.addAll(newSiteLinkXcs);

            //handle REG
            if (j == primarySize - 1) {//最后一条，不会有reg了
                break;
            }
            String nextSiteLinkId = primaryRoute.get(j + 1);
            if (tunnelUtils.isWssLink(siteLinkId)) {
                j = j + 2;
                continue;//todo: 等做roadm再处理
            }
            String siteId = tunnelUtils.getRegSite(siteLinkId, nextSiteLinkId);
            OtRouteInfoNewOch regRouteInfoNewOch = allocateRegOrRoadm(input, route, tpcNeInfo, siteLinkId,
                    nextSiteLinkId, siteId, inMemoryNode, inMemorySiteLink, frequencyString, cenFrequency);
            removedResourceIds.addAll(regRouteInfoNewOch.getRemovedResourceIds());
            inMemoryNode = regRouteInfoNewOch.getInMemoryNodes();
//            tpcXcsStart.addAll(regRouteInfoNewOch.getTpcXcs());
            primaryLinks.addAll(regRouteInfoNewOch.getOsLinks());
            primaryNodeSnapshots.addAll(regRouteInfoNewOch.getSnapshotNodes());
            primaryOchXcs.addAll(regRouteInfoNewOch.getOchXcs());
            j++;

        }
        primaryOchXcs.addAll(endOchXcs);

        return new TempInfo(primaryNodeSnapshots, primarySiteLinkSnapshot, primaryTpcXcsStart, primaryTpcXcsEnd, primaryOchXcs, primaryLinks, removedResourceIds, secondaryTpStart, secondaryTpEnd,
                thirdTpStart,
                thirdTpEnd);

    }

    private TempInfo allocateNonProtected(TunnelNewOchInput input, Map<String, Node> inMemoryNode, Map<String, Link> inMemorySiteLink, NeInfo tpcNeInfo, String srcSiteId, String destSiteId,
            RouteData route,
            Long cenFrequency, String frequencyString, Integer tunnelToCreate) throws NeDesignerException {
        int routeSize = route.getLinkList().size();
        if (routeSize == 1) {
            return allocateNonProtectedP2P(input, inMemoryNode, tpcNeInfo, srcSiteId, destSiteId, route.getLinkList()
                    .get(0), cenFrequency, frequencyString, tunnelToCreate);

        }
        return allocateNonProtectedNetWork(input, inMemoryNode, inMemorySiteLink, tpcNeInfo, srcSiteId, destSiteId, route, cenFrequency, frequencyString, tunnelToCreate);


    }

    private TempInfo allocateNonProtectedNetWork(TunnelNewOchInput input, Map<String, Node> inMemoryNode, Map<String, Link> inMemorySiteLink, NeInfo tpcNeInfo, String srcSiteId, String destSiteId,
            RouteData route, Long cenFrequency,
            String frequencyString, Integer tunnelToCreate)
            throws NeDesignerException {
        List<CrossConnections> tpcXcsStart = new ArrayList<>();
        List<CrossConnections> tpcXcsEnd = new ArrayList<>();
        List<CrossConnections> ochXcs = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        List<Node> nodeSnapshots = new ArrayList<>();
        List<Link> siteLinkSnapshot = new ArrayList<>();
        List<String> routeLinks = route.getLinkList();
        int routeSize = routeLinks.size();
        List<CrossConnections> endExpLineXcs = new ArrayList<>();
        int j = 0;
        List<String> removedResourceIds = new ArrayList<>();
        while (j < routeSize) {
            String siteLinkId = routeLinks.get(j);
            if (tunnelUtils.isWssLink(siteLinkId)) {
                j++;
                continue;//todo: 等做roadm再处理
            }
            Link siteLink;
            if (inMemorySiteLink.containsKey(siteLinkId)) {
                siteLink = inMemorySiteLink.get(siteLinkId);
            } else {
                siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
                siteLinkSnapshot.add(siteLink);
            }
            //validate siteLink frequency
//            List<Long> centralFrequencies;
//            try {
//                WDM_Band band = WDM_Band.fromString(tpcNeInfo.getCardByCardType(input.getCardType()).getWdmBand());
//                centralFrequencies = FrequencyAvailable.getFreeCentFrequency(Arrays.asList(siteLink), band, input.getGrid());
//            } catch (Exception e) {
//                String msg = "Failed to get centralFrequencies for link: " + siteLinkId;
//                log.error("Failed to get centralFrequencies for link:{}", siteLink, e);
//                throw new NeDesignerException(msg);
//            }
//            if (!centralFrequencies.contains(cenFrequency)) {
//                String msg = String.format("SiteLink %s has changed after tunnel computing, frequency %d is not available now, re-compute again.", siteLinkId, cenFrequency);
//                log.error(msg);
//                throw new NeDesignerException(msg);
//            }

            //todo: 等roadm再处理
//            String preWssLinkId = j == 0 ? null : route.get(j - 1);
//            String nextWssLinkId = j == routeSize - 1 ? null : route.get(j + 1);
            String preWssLinkId = null;
            String nextWssLinkId = null;
            String startMuxMdPortTp = null;
            String endNuxMdPortTp = null;

            //起始点,终点： 需要额外创建TPC的交叉和OS link
            if (j == 0 || j == routeSize - 1) {
                String siteId = j == 0 ? srcSiteId : destSiteId;
                String siteLinkNodeId = getNodeIdBySiteId(siteLink, siteId);

                Node siteLinkNode;
                if (inMemoryNode.containsKey(siteLinkNodeId)) {
                    siteLinkNode = inMemoryNode.get(siteLinkNodeId);
                } else {
                    siteLinkNode = phyNodeDao.getConfigPhyNodeById(siteLinkNodeId);
                    nodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNode));
                }

                //note：所有node相关的改变都在OT的allocate，复用段的allocate不改变node
                if (j == 0) {
                    OtRouteInfoNewOch otRouteInfoNewOch = otAllocate.allocateOtStartNewOch(tunnelToCreate, srcSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNode,
                            siteLink);
                    inMemoryNode = otRouteInfoNewOch.getInMemoryNodes();
                    tpcXcsStart.addAll(otRouteInfoNewOch.getTpcXcs());
                    links.addAll(otRouteInfoNewOch.getOsLinks());
                    nodeSnapshots.addAll(otRouteInfoNewOch.getSnapshotNodes());
                    startMuxMdPortTp = otRouteInfoNewOch.getMuxMdPortTpSrcPair().getLeft();
                    ochXcs.addAll(otRouteInfoNewOch.getOchXcs());

                } else {
                    OtRouteInfoNewOch otRouteInfoNewOch = otAllocate.allocateOtEndNewOch(tunnelToCreate, destSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNode,
                            siteLink);
                    tpcXcsEnd.addAll(otRouteInfoNewOch.getTpcXcs());
                    links.addAll(otRouteInfoNewOch.getOsLinks());
                    nodeSnapshots.addAll(otRouteInfoNewOch.getSnapshotNodes());
                    endNuxMdPortTp = otRouteInfoNewOch.getMuxMdPortTpSrcPair().getLeft();
                    endExpLineXcs = otRouteInfoNewOch.getOchXcs();
                }
            }

            //allocate XC for sitelink node
            List<CrossConnections> newSiteLinkXcs = tunnelSiteAllocate.allocateSegmentNetwork(startMuxMdPortTp, endNuxMdPortTp, siteLink, frequencyString, cenFrequency, preWssLinkId, nextWssLinkId);
            ochXcs.addAll(newSiteLinkXcs);
            ochXcs.addAll(endExpLineXcs);

            //handle REG
            if (j == routeSize - 1) {//最后一条，不会有reg了
                break;
            }
            String nextSiteLinkId = routeLinks.get(j + 1);
            if (!inMemorySiteLink.containsKey(nextSiteLinkId)) {
                siteLinkSnapshot.add(siteLinkDao.getSiteLinkById(nextSiteLinkId));
            }
            if (tunnelUtils.isWssLink(siteLinkId)) {
                j = j + 2;
                continue;//todo: 等做roadm再处理
            }
            String siteId = tunnelUtils.getRegSite(siteLinkId, nextSiteLinkId);
            OtRouteInfoNewOch regRouteInfoNewOch = allocateRegOrRoadm(input, route, tpcNeInfo, siteLinkId,
                    nextSiteLinkId, siteId, inMemoryNode, inMemorySiteLink, frequencyString, cenFrequency);
            removedResourceIds.addAll(regRouteInfoNewOch.getRemovedResourceIds());
            inMemoryNode = regRouteInfoNewOch.getInMemoryNodes();
            tpcXcsStart.addAll(regRouteInfoNewOch.getTpcXcs());
            links.addAll(regRouteInfoNewOch.getOsLinks());
            nodeSnapshots.addAll(regRouteInfoNewOch.getSnapshotNodes());
            ochXcs.addAll(regRouteInfoNewOch.getOchXcs());
            j++;

        }

        return new TempInfo(nodeSnapshots, siteLinkSnapshot, tpcXcsStart, tpcXcsEnd, ochXcs, links, removedResourceIds);
    }

    private TempInfo allocateNonProtectedP2P(TunnelNewOchInput input, Map<String, Node> inMemoryNode, NeInfo tpcNeInfo, String srcSiteId, String destSiteId, String siteLinkId, Long cenFrequency,
            String frequencyString, Integer tunnelToCreate) throws NeDesignerException {
        List<CrossConnections> tpcXcsStart = new ArrayList<>();
        List<CrossConnections> tpcXcsEnd = new ArrayList<>();
        List<CrossConnections> ochXcs = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        List<Node> nodeSnapshots = new ArrayList<>();
        List<Link> siteLinkSnapshot = new ArrayList<>();

        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        siteLinkSnapshot.add(siteLink);
        //validate siteLink frequency
//        List<Long> centralFrequencies;
//        try {
//            WDM_Band band = WDM_Band.fromString(tpcNeInfo.getCardByCardType(input.getCardType()).getWdmBand());
//            centralFrequencies = FrequencyAvailable.getFreeCentFrequency(Arrays.asList(siteLink), band, input.getGrid());
//        } catch (Exception e) {
//            String msg = "Failed to get centralFrequencies for link: " + siteLinkId;
//            log.error("Failed to get centralFrequencies for link:{}", siteLink, e);
//            throw new NeDesignerException(msg);
//        }
//        if (!centralFrequencies.contains(cenFrequency)) {
//            log.warn("cenFrequency:{} is not included in centralFrequencies:{}, if it is reallocate ignore.", cenFrequency, centralFrequencies);
////            String msg = String.format("SiteLink %s has changed after tunnel computing, frequency %d is not available now, re-compute again.", siteLinkId, cenFrequency);
////            log.error(msg);
////            throw new NeDesignerException(msg);
//        }

        String siteLinkNodeIdSrc = getNodeIdBySiteId(siteLink, srcSiteId);
        String siteLinkNodeIdDst = getNodeIdBySiteId(siteLink, destSiteId);

        Node siteLinkNodeSrc;
        if (inMemoryNode.containsKey(siteLinkNodeIdSrc)) {
            siteLinkNodeSrc = inMemoryNode.get(siteLinkNodeIdSrc);
        } else {
            siteLinkNodeSrc = phyNodeDao.getConfigPhyNodeById(siteLinkNodeIdSrc);
//            nodeSnapshots.add(new NodeBuilder(siteLinkNodeSrc).build());
            nodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNodeSrc));
        }

        Node siteLinkNodeDst;
        if (inMemoryNode.containsKey(siteLinkNodeIdDst)) {
            siteLinkNodeDst = inMemoryNode.get(siteLinkNodeIdDst);
        } else {
            siteLinkNodeDst = phyNodeDao.getConfigPhyNodeById(siteLinkNodeIdDst);
            nodeSnapshots.add(nodeUtils.getNodeCopy(siteLinkNodeDst));
        }

        //start
        OtRouteInfoNewOch otRouteInfoNewOchStart = otAllocate.allocateOtStartNewOch(tunnelToCreate, srcSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNodeSrc,
                siteLink);
        inMemoryNode = otRouteInfoNewOchStart.getInMemoryNodes();
        tpcXcsStart.addAll(otRouteInfoNewOchStart.getTpcXcs());
        links.addAll(otRouteInfoNewOchStart.getOsLinks());
        nodeSnapshots.addAll(otRouteInfoNewOchStart.getSnapshotNodes());
        String muxMdPortTpSrc = otRouteInfoNewOchStart.getMuxMdPortTpSrcPair().getLeft();
        ochXcs.addAll(otRouteInfoNewOchStart.getOchXcs());
        //end
        OtRouteInfoNewOch otRouteInfoNewOchEnd = otAllocate.allocateOtEndNewOch(tunnelToCreate, destSiteId, input, cenFrequency, tpcNeInfo, inMemoryNode, frequencyString, siteLinkNodeDst, siteLink);
        tpcXcsEnd.addAll(otRouteInfoNewOchEnd.getTpcXcs());
        links.addAll(otRouteInfoNewOchEnd.getOsLinks());
        nodeSnapshots.addAll(otRouteInfoNewOchEnd.getSnapshotNodes());
        String muxMdPortTpDst = otRouteInfoNewOchEnd.getMuxMdPortTpSrcPair().getLeft();

        //allocate XC for sitelink node
        List<CrossConnections> newSiteLinkXcs = tunnelSiteAllocate.allocateSegmentNetwork(muxMdPortTpSrc, muxMdPortTpDst, siteLink, frequencyString, cenFrequency, null, null);
        ochXcs.addAll(newSiteLinkXcs);

        //add end expLine XC
        ochXcs.addAll(otRouteInfoNewOchEnd.getOchXcs());

        return new TempInfo(nodeSnapshots, siteLinkSnapshot, tpcXcsStart, tpcXcsEnd, ochXcs, links, Collections.emptyList());

    }


    private OtRouteInfoNewOch allocateRegOrRoadm(TunnelNewOchInput input, RouteData route, NeInfo tpcNeInfo,
            String siteLinkId, String nextSiteLinkId, String siteId, Map<String, Node> inMemoryNode,
            Map<String, Link> inMemorySiteLink, String frequencyString, Long cenFrequency) throws NeDesignerException {
        return allocateRegOrRoadm(input, route, tpcNeInfo, siteLinkId, nextSiteLinkId, siteId, inMemoryNode,
                inMemorySiteLink, frequencyString, cenFrequency, null);
    }

    private OtRouteInfoNewOch allocateRegOrRoadm(TunnelNewOchInput input, RouteData route, NeInfo tpcNeInfo,
            String siteLinkId, String nextSiteLinkId, String siteId, Map<String, Node> inMemoryNode,
            Map<String, Link> inMemorySiteLink, String frequencyString, Long cenFrequency, String preferredMdPortName) throws NeDesignerException {
        RegSiteInfo regSiteInfo = route.getSiteMap().get(siteId);
        // route-reg contains only sites explicitly selected as REG; missing sites keep the default ROADM handling.
        if (regSiteInfo == null || "ROADM".equals(regSiteInfo.getType())) {
            return roadmService.allocateRoadm(siteLinkId, nextSiteLinkId, siteId, inMemoryNode, frequencyString,
                    cenFrequency);
        }
        return otAllocate.allocateReg(input, tpcNeInfo, siteLinkId, nextSiteLinkId, regSiteInfo, inMemoryNode,
                inMemorySiteLink, frequencyString, cenFrequency, preferredMdPortName);
    }


    private String getNodeIdBySiteId(Link siteLink, String srcSiteId) {

        String siteId = siteLink.getSource().getSourceNode().getValue();
        if (siteId.equals(srcSiteId)) {
            return PhysicalTpIdNamingRule.getNodeId(siteLink.getSource().getSourceTp().getValue());
        }
        return PhysicalTpIdNamingRule.getNodeId(siteLink.getDestination().getDestTp().getValue());
    }


    class TempInfo {

        List<Node> nodeSnapshots;
        List<Link> nodeSiteLinkSnapshot;
        List<CrossConnections> tpcXcsStart;
        List<CrossConnections> tpcXcsEnd;
        List<CrossConnections> ochXcs;
        List<Link> links;
        List<Link> routeLinks;
        List<String> removedResourceIds;

        String secondaryTpStart;
        String secondaryTpEnd;

        String thirdTpStart;
        String thirdTpEnd;

        //for primary
        public TempInfo(List<Node> nodeSnapshots, List<Link> nodeSiteLinkSnapshot, List<CrossConnections> tpcXcsStart, List<CrossConnections> tpcXcsEnd, List<CrossConnections> ochXcs,
                List<Link> links, List<String> removedResourceIds, String secondaryTpStart, String secondaryTpEnd, String thirdTpStart, String thirdTpEnd) {
            this.nodeSnapshots = nodeSnapshots;
            this.nodeSiteLinkSnapshot = nodeSiteLinkSnapshot;
            this.tpcXcsStart = tpcXcsStart;
            this.tpcXcsEnd = tpcXcsEnd;
            this.ochXcs = ochXcs;
            this.links = links;
            this.routeLinks = links;
            this.removedResourceIds = removedResourceIds;
            this.secondaryTpStart = secondaryTpStart;
            this.secondaryTpEnd = secondaryTpEnd;
            this.thirdTpStart = thirdTpStart;
            this.thirdTpEnd = thirdTpEnd;
        }

        //for non protected
        public TempInfo(List<Node> nodeSnapshots, List<Link> nodeSiteLinkSnapshot, List<CrossConnections> tpcXcsStart, List<CrossConnections> tpcXcsEnd, List<CrossConnections> ochXcs,
                List<Link> links, List<String> removedResourceIds) {
            this.nodeSnapshots = nodeSnapshots;
            this.nodeSiteLinkSnapshot = nodeSiteLinkSnapshot;
            this.tpcXcsStart = tpcXcsStart;
            this.tpcXcsEnd = tpcXcsEnd;
            this.ochXcs = ochXcs;
            this.links = links;
            this.routeLinks = links;
            this.removedResourceIds = removedResourceIds;
        }

        //for secondary
        public TempInfo(List<Node> nodeSnapshots, List<Link> nodeSiteLinkSnapshot, List<CrossConnections> ochXcs,
                List<Link> links, List<String> removedResourceIds) {
            this.nodeSnapshots = nodeSnapshots;
            this.nodeSiteLinkSnapshot = nodeSiteLinkSnapshot;
            this.ochXcs = ochXcs;
            this.links = links;
            this.routeLinks = links;
            this.removedResourceIds = removedResourceIds;
        }
    }
}
