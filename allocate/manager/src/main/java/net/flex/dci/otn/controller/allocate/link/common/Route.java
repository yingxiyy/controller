/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.common;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.RouteUsageInclude;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHopBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author convert allocate route to yang route
 * @version 1.0
 */
@Slf4j
public class Route {

    private RouteInfo routeResource; //计算出来的结果

    private String tpA;
    private String tpZ;
    private boolean isRoadmSiteLink = false;

    AllocatorConfig configuration = SpringBeanFinder.getBean(AllocatorConfig.class);

    /**
     * 这个方法专门为ROADM site link 设计，因为ROADM site link的起止点需要特殊处理
     *
     * @return
     */
    public Route setRoadm() {
        this.isRoadmSiteLink = true;
        return this;
    }


    public enum RouteType {
        Tunnel,
        OchLink,
        SiteLink
    }

    ;

    private RouteType routeType;
    private boolean hasMpoAggregatingCard;  //used for siteLink
    private boolean hasDualFmux32;  //used for Bone2.0 64T dual-FMUX siteLink
    private boolean isC_L;
    private SiteType aType = SiteType.OTM;
    private SiteType zType = SiteType.OTM;

    public SiteType getaType() {
        return aType;
    }

    public SiteType getzType() {
        return zType;
    }

    public Route(RouteInfo routeResource, RouteType linkType) {
        this.routeResource = routeResource;
        this.routeType = linkType;
    }

    public ExplictRoute getExplictRoute(String aTp, String zTp) throws CommonException {
        tpA = aTp;
        tpZ = zTp;
        List<Link> mainRoute = null;
        if (routeType.equals(RouteType.SiteLink)) {
            hasMpoAggregatingCard = mpoAggregatingCardExisted(routeResource.getMain().getLinks());
            hasDualFmux32 = dualFmux32Existed(routeResource.getMain().getLinks());
            mainRoute = cutRoute(routeResource.getMain().getLinks(), aTp, zTp);
            settingAZType(mainRoute);
        } else {
            mainRoute = routeResource.getMain().getLinks();
        }
        RouteBuilder rb = new RouteBuilder();
        List<ExplicitRouteObjects> mainEroList = buildRoute(mainRoute, aTp, routeResource.getMain().getXcs());
        Primary primary = new PrimaryBuilder()
                .setExplicitRouteObjects(mainEroList)
                .setCrossConnections(getRouteXC(routeResource.getMain().getXcs(), mainRoute))
                .build();
        rb.setIndex((short) 1).setKey(new RouteKey((short) 1)).setPrimary(primary);

        ApsRouteSelection apsSelection = selectAEndApsRoute(routeResource.getMain().getXcs(),
                aTp,
                routeType == RouteType.SiteLink && routeResource.getSlave() != null
                        ? routeResource.getSlave().getLinks() : null,
                routeType == RouteType.SiteLink && routeResource.getThird() != null
                        ? routeResource.getThird().getLinks() : null);
        if (apsSelection != null) {
            CrossConnections apsXC = apsSelection.getApsXc();
            if (routeResource.getSlave() != null) {
                SecondaryBuilder secondaryBuilder = new SecondaryBuilder();
                String startTp = apsSelection.getSlaveStartTp();
//          if (routeType.equals(RouteType.SiteLink)) {
//            startTp = getApsBTp(apsXC);
//          } else {
//            startTp = getProtectionStartTp(getApsBTp(apsXC), routeResource.getSlave().getLinks());
//          }
                if (startTp == null) {
                    log.error("this protection link cannot find out start TP in slave links");
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required spare TP in slave links");
                }
                List<ExplicitRouteObjects> spareEroList = buildRoute(routeResource.getSlave().getLinks(), startTp,
                        routeResource.getSlave().getXcs());
                secondaryBuilder
                        .setExplicitRouteObjects(spareEroList)
                        .setCrossConnections(getRouteXC(routeResource.getSlave().getXcs(),
                                routeResource.getSlave().getLinks()));
                rb.setSecondary(secondaryBuilder.build());

                if (routeResource.getThird() != null) {
                    startTp = apsSelection.getThirdStartTp();
//            if (routeType.equals(RouteType.SiteLink)) {
//              startTp = getApsCTp(apsXC);
//            } else {
//              startTp = getProtectionStartTp(getApsCTp(apsXC), routeResource.getThird().getLinks());
//            }
                    if (startTp == null) {
                        log.error("this protection link cannot find out start TP in third links");
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required third TP in third links");
                    } else {
                        List<Third> thirdList = new ArrayList<>();

                        spareEroList = buildRoute(routeResource.getThird().getLinks(), startTp,
                                routeResource.getThird().getXcs());
                        Third third = new ThirdBuilder()
                                .setIndex((short) 0)
                                .setExplicitRouteObjects(spareEroList)
                                .setCrossConnections(getRouteXC(routeResource.getThird().getXcs(),
                                        routeResource.getThird().getLinks()))
                                .build();
                        thirdList.add(third);

                        rb.setThird(thirdList);
                    }
                }
            } else {
                rb.setSecondary(null);
            }
        }
        ExplictRouteBuilder eb = new ExplictRouteBuilder()
                .setRoute(new LinkedList<>());
        eb.getRoute().add(rb.build());

        return eb.build();
    }

    //计算复用段的A/Z 类型 （ROADM/OTM），到OA之前，出现IRA/WSS 就是ROADM
    private void settingAZType(List<Link> mainRoute) {
        for (int index = 0; index < mainRoute.size(); index++) {
            Link link = mainRoute.get(index);
            if (settingAZType(link, true)) {
                break;
            }
        }

        for (int index = mainRoute.size() - 1; index >= 0; index--) {
            Link link = mainRoute.get(index);
            if (settingAZType(link, false)) {
                break;
            }
        }
    }

    private boolean settingAZType(Link link, boolean isA) {
        boolean done = false;

        String tpId = link.getSource().getSourceTp().getValue();
        Equipments eq = getEqBasedOnTpId(tpId);
        if (eq.getEquipType().equals(EquipType.WSS) || eq.getEquipType().equals(EquipType.IRA)) {
            if (isA) {
                aType = SiteType.ROADM;
            } else {
                zType = SiteType.ROADM;
            }
            done = true;
        }
        if (eq.getEquipType().equals(EquipType.OA)) {
            if (isA) {
                aType = SiteType.OTM;
            } else {
                zType = SiteType.OTM;
            }
            done = true;
        }
        return done;
    }

    private Equipments getEqBasedOnTpId(String tpId) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);

        Node node = routeResource.getMain().getNodes().stream().filter(x -> x.getNodeId().getValue().equals(nodeId)).findAny().orElse(null);
        List<Equipments> eqList = node.getAugmentation(Node1.class).getPhysical().getEquipments();

        Equipments eq = eqList.stream().filter(x -> x.getEquipmentId().equals(eqId)).findAny().orElse(null);
        return eq;
    }

    private boolean mpoAggregatingCardExisted(List<Link> phyLinks) {
        for (Link phyLink : phyLinks) {
            String aTpId = phyLink.getSource().getSourceTp().getValue();
            String zTpId = phyLink.getDestination().getDestTp().getValue();

            String aEqId = PhysicalTpIdNamingRule.getEquipId(aTpId);
            String zEqId = PhysicalTpIdNamingRule.getEquipId(zTpId);

            String aNodeId = PhysicalTpIdNamingRule.getNodeId(aTpId);
            String zNodeId = PhysicalTpIdNamingRule.getNodeId(zTpId);

            boolean mpoAggregatingCardExisted = isMpoAggregatingCard(aNodeId, aEqId)
                    || isMpoAggregatingCard(zNodeId, zEqId);
            if (mpoAggregatingCardExisted) {
                return true;
            }
        }
        return false;
    }

    private boolean isMpoAggregatingCard(String nodeId, String eqId) {
        Node node = getPhyNode(nodeId);
        Equipments eq = node.getAugmentation(Node1.class).getPhysical().getEquipments()
                .stream().filter(x -> x.getEquipmentId().equals(eqId)).findAny().orElse(null);

        if (eq == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find required EQ in DB " + eqId);
        }

        return isMpoAggregatingEquipType(eq.getEquipType());
    }

    static boolean isMpoAggregatingEquipType(EquipType equipType) {
        return equipType == EquipType.CMUX64 || equipType == EquipType.FMUX32;
    }

    private boolean dualFmux32Existed(List<Link> phyLinks) {
        Map<String, Set<String>> fmux32ByNode = new HashMap<>();
        for (Link phyLink : phyLinks) {
            collectFmux32(fmux32ByNode, phyLink.getSource().getSourceTp().getValue());
            collectFmux32(fmux32ByNode, phyLink.getDestination().getDestTp().getValue());
        }
        return fmux32ByNode.values().stream()
                .anyMatch(equipments -> equipments.size() > 1);
    }

    private void collectFmux32(Map<String, Set<String>> fmux32ByNode, String tpId) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String equipmentId = PhysicalTpIdNamingRule.getEquipId(tpId);
        Node node = getPhyNode(nodeId);
        if (node == null || node.getAugmentation(Node1.class) == null
                || node.getAugmentation(Node1.class).getPhysical() == null) {
            return;
        }
        boolean fmux32 = node.getAugmentation(Node1.class).getPhysical()
                .getEquipments().stream()
                .anyMatch(equipment -> equipmentId.equals(equipment.getEquipmentId())
                        && equipment.getEquipType() == EquipType.FMUX32);
        if (fmux32) {
            fmux32ByNode.computeIfAbsent(nodeId, key -> new HashSet<>()).add(equipmentId);
        }
    }

    private Node getPhyNode(String nodeId) {
        Node node = routeResource.getMain().getNodes().stream().filter(x -> x.getNodeId().getValue().equals(nodeId)).findAny().orElse(null);
        if (node != null) {
            return node;
        }
        if (routeResource.getSlave() != null) {
            node = routeResource.getSlave().getNodes().stream().filter(x -> x.getNodeId().getValue().equals(nodeId)).findAny().orElse(null);
            if (node != null) {
                return node;
            }
        }
        if (routeResource.getThird() != null) {
            node = routeResource.getThird().getNodes().stream().filter(x -> x.getNodeId().getValue().equals(nodeId)).findAny().orElse(null);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * 对于ROADM网路中的复用段，起点不是compute-link 输出的mux--wss--.....wss-mux, 而是wss 所以这个method 就是基于给出的A/Z 裁剪, ....A=====Z....  所有A===Z 以外的都不需要
     *
     * @param links, a list of link
     * @param aTp
     * @param zTp
     * @return
     */
    private List<Link> cutRoute(List<Link> links, String aTp, String zTp) {
        List<Link> newRoute = new ArrayList<>();
        boolean startFound = false;
        boolean endingStart = false;
        //in CMUX64 situation, the A/Z of link is MPO1..8, but aTp/zTp is MPO
        //thus we change equal method to contains method
        for (Link link : links) {
            if (link.getSource().getSourceTp().getValue().contains(aTp) || link.getDestination().getDestTp().getValue().contains(aTp)) {
                startFound = true;
            }
            if (startFound) {
                if (!endingStart && (link.getSource().getSourceTp().getValue().contains(zTp) || link.getDestination().getDestTp().getValue().contains(zTp))) {
                    endingStart = true;
                }
                if (endingStart && (!link.getSource().getSourceTp().getValue().contains(zTp) && !link.getDestination().getDestTp().getValue().contains(zTp))) {
                    //this is last one
                    break;
                }
                newRoute.add(link);
            }
        }
        return newRoute;
    }

    private static String getApsBTp(CrossConnections apsXc) {
        if (apsXc.getSourceTp().size() > 1) {
            return apsXc.getSourceTp().get(1).getTpRef().getValue();
        } else if (apsXc.getDestinationTp().size() > 1) {
            return apsXc.getDestinationTp().get(1).getTpRef().getValue();
        }
        return null;
    }

    private static String getApsCTp(CrossConnections apsXc) {
        if (apsXc.getSourceTp().size() == 3) {
            return apsXc.getSourceTp().get(2).getTpRef().getValue();
        } else if (apsXc.getDestinationTp().size() == 3) {
            return apsXc.getDestinationTp().get(2).getTpRef().getValue();
        }
        return null;
    }


    private String getProtectionStartTp(String apsTp, List<Link> links) {
        for (Link link : links) {
            if (link.getSource().getSourceTp().getValue().equals(apsTp)) {
                return link.getDestination().getDestTp().getValue();
            }
            if (link.getDestination().getDestTp().getValue().equals(apsTp)) {
                return link.getSource().getSourceTp().getValue();
            }
        }
        return null;
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> getRouteXC(
            List<CrossConnections> xcs, List<Link> routeLinks) {
        if (xcs == null || xcs.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> routeTpIds = routeLinks == null ? Collections.emptySet()
                : routeLinks.stream()
                        .flatMap(link -> Arrays.asList(
                                link.getSource().getSourceTp().getValue(),
                                link.getDestination().getDestTp().getValue()).stream())
                        .collect(Collectors.toSet());
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcSeqList = new LinkedList<>();
        long index = 1;
        for (CrossConnections xc : xcs) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder xcSeqBuilder =
                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(xc);

            String xcId = xc.getCrossConnectionId().getValue();
            if (routeType.equals(RouteType.SiteLink)) {
                //original xcID for MPO include all MPO? ID
                if (hasMpoAggregatingCard && !isFmux32MpoXc(xc)) {
                    String[] tmp = xcId.split("MPO");
                    if (tmp.length > 1) {
                        //this is MPO XC, should change xcID, and contained TPs
                        //all MPO tp include in this xc will be removed and insert one MPO
                        boolean endpointListCollapsed = false;
                        if (xc.getSourceTp().size() > 1) {
                            //set XC's sourceTP
                            SourceTp mpo1 = xc.getSourceTp().get(0);
                            String mpo1Id = mpo1.getTpRef().getValue();
                            String mpoId = mpo1Id.substring(0, mpo1Id.length() - 1);

                            xcSeqBuilder.setSourceTp(new LinkedList<>());
                            xcSeqBuilder.getSourceTp().add(new SourceTpBuilder().
                                    setTpRef(new TpId(mpoId))
                                    .setSlot(mpo1.getSlot())
                                    .build());
                            endpointListCollapsed = true;
                        } else if (xc.getDestinationTp().size() > 1) {
                            //set XC's destinationTp
                            DestinationTp mpo1 = xc.getDestinationTp().get(0);
                            String mpo1Id = mpo1.getTpRef().getValue();
                            String mpoId = mpo1Id.substring(0, mpo1Id.length() - 1);

                            xcSeqBuilder.setDestinationTp(new LinkedList<>());
                            xcSeqBuilder.getDestinationTp().add(new DestinationTpBuilder().
                                    setTpRef(new TpId(mpoId))
                                    .setSlot(mpo1.getSlot())
                                    .build());
                            endpointListCollapsed = true;
                        }

                        if (endpointListCollapsed) {
                            List<String> collapsedTpRefs = new ArrayList<>();
                            collapsedTpRefs.addAll(xcSeqBuilder.getSourceTp().stream()
                                    .map(tp -> tp.getTpRef().getValue())
                                    .collect(Collectors.toList()));
                            collapsedTpRefs.addAll(xcSeqBuilder.getDestinationTp().stream()
                                    .map(tp -> tp.getTpRef().getValue())
                                    .collect(Collectors.toList()));
                            if (xc.getDirection() == LinkDirection.Bidirection) {
                                Collections.sort(collapsedTpRefs);
                            }
                            xcId = "XC-" + String.join("-", collapsedTpRefs);
                        }
                    }
                }

                prioritizeRouteVisibleSourceTp(xcSeqBuilder, routeTpIds);

                //this is normal XC.
                //nothing change.
            }

            xcSeqBuilder.setCrossConnectionId(new Uri(xcId)).setSequence(index++);

            xcSeqList.add(xcSeqBuilder.build());
        }

        return xcSeqList;
    }

    private boolean isFmux32MpoXc(CrossConnections xc) {
        List<String> sourceTpIds = xc.getSourceTp().stream()
                .map(tp -> tp.getTpRef().getValue())
                .collect(Collectors.toList());
        List<String> destinationTpIds = xc.getDestinationTp().stream()
                .map(tp -> tp.getTpRef().getValue())
                .collect(Collectors.toList());
        return isFourPortMpoGroup(sourceTpIds) || isFourPortMpoGroup(destinationTpIds);
    }

    private boolean isFourPortMpoGroup(List<String> tpIds) {
        if (tpIds.size() != 4) {
            return false;
        }

        String firstTpId = tpIds.get(0);
        int mpoIndex = firstTpId.lastIndexOf("MPO");
        if (mpoIndex < 0) {
            return false;
        }

        String mpoPrefix = firstTpId.substring(0, mpoIndex + 3);
        Set<String> uniqueTpIds = new HashSet<>(tpIds);
        for (int port = 1; port <= 4; port++) {
            if (!uniqueTpIds.contains(mpoPrefix + port)) {
                return false;
            }
        }
        return true;
    }

    private void prioritizeRouteVisibleSourceTp(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder xcBuilder,
            Set<String> routeTpIds) {
        List<SourceTp> sourceTps = xcBuilder.getSourceTp();
        if (sourceTps == null || sourceTps.size() < 2 || routeTpIds.isEmpty()) {
            return;
        }

        for (int index = 1; index < sourceTps.size(); index++) {
            if (routeTpIds.contains(sourceTps.get(index).getTpRef().getValue())) {
                List<SourceTp> reordered = new LinkedList<>();
                reordered.add(sourceTps.get(index));
                reordered.addAll(sourceTps.subList(0, index));
                reordered.addAll(sourceTps.subList(index + 1, sourceTps.size()));
                xcBuilder.setSourceTp(reordered);
                return;
            }
        }
    }

    /**
     * 构建ERO
     *
     * @param linkRoute
     * @param startTp
     * @return
     */
    private List<ExplicitRouteObjects> buildRoute(List<Link> linkRoute, String startTp,
            List<CrossConnections> routeXcs) {
        if (routeType.equals(RouteType.OchLink)) {
            checkCL(linkRoute);
        }
        List<ExplicitRouteObjects> eroList = new LinkedList<>();
        ExplicitRouteObjects ero = new ExplicitRouteObjectsBuilder()
                .setExplicitRouteUsage(RouteUsageInclude.class)
                .setPathRouteObject(buildPathRoutObject(linkRoute, startTp, routeXcs))
                .setKey(new ExplicitRouteObjectsKey(RouteUsageInclude.class))
                .build();
        eroList.add(ero);

        return eroList;
    }

    private void checkCL(List<Link> linkRoute) {
        Link siteLink = linkRoute.stream().filter(link -> SiteLinkIdNamingRule.isSiteLink(link.getLinkId().getValue())).findAny().orElse(null);

        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        if (siteLinkAttr.getLinkGroup().equals(WDM_Band.C_L.toString())) {
            isC_L = true;
        } else {
            isC_L = false;
        }
    }

    /**
     * 根据siteRoute 生成 点--线--点 的路由
     *
     * @param linkRoute
     * @param startTp
     */
    private List<PathRouteObject> buildPathRoutObject(List<Link> linkRoute, String startTp,
            List<CrossConnections> routeXcs) throws CommonException {
        List<String> mpoxMergeHappened = new ArrayList<>();
        Set<String> mergedMpoLinks = new HashSet<>();

        long index = 1;
        String actuallyMpon = null;
        List<PathRouteObject> proList = new ArrayList<>();

        TopologyId currentLayerTopoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
        if (routeType.equals(RouteType.Tunnel)) {
            if (tpA == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "tunnel A is empty, this is impossible");
            } else {
                proList.add(getTpPathRouteObject(currentLayerTopoId, new TpId(tpA), index++));
            }
        }

        for (Link link : linkRoute) {
            String startEqId = PhysicalTpIdNamingRule.getEquipId(startTp);

            String mergedMpoLinkKey = mergedMpoLinkKey(
                    link.getSource().getSourceTp().getValue(),
                    link.getDestination().getDestTp().getValue());
            if (hasDualFmux32 && mergedMpoLinkKey != null
                    && !mergedMpoLinks.add(mergedMpoLinkKey)) {
                // The same physical fanout already has one virtual MUX-FMUX link.
                continue;
            }
            if (!hasDualFmux32 && mpoxMergeHappened.stream()
                    .anyMatch(link.getLinkId().getValue()::contains)) {
                continue; //has mer
            }
            TopologyId underLayerTopoId = null;
            if (routeType.equals(RouteType.Tunnel)) {
                underLayerTopoId = new TopologyId(TopoNameConstants.Och_Topo_Key);
            } else if (routeType.equals(RouteType.SiteLink)) {
                underLayerTopoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
            } else if (routeType.equals(RouteType.OchLink)) {
                //包含3中link,
                // OS Link, 在linkRoutes.getLinks的起止位置
                // siteLink,
                // wssLink

                if (SiteLinkIdNamingRule.isSiteLink(link.getLinkId().getValue())) {
                    underLayerTopoId = new TopologyId(TopoNameConstants.Site_Topo_Key);
                } else {
                    underLayerTopoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
                }
            }

            TpId srcTpId = link.getSource().getSourceTp();
            TpId linkDstTpId = link.getDestination().getDestTp();
            TpId dstTpId;
            if (srcTpId.getValue().equals(startTp)) {
                dstTpId = linkDstTpId;
            } else if (linkDstTpId.getValue().equals(startTp)) {
                dstTpId = srcTpId;
                srcTpId = linkDstTpId;
            } else {
                TpId resolvedEndpoint = resolveSameEquipmentEndpoint(startTp, srcTpId, linkDstTpId, routeXcs);
                if (resolvedEndpoint != null) {
                    if (resolvedEndpoint.getValue().equals(srcTpId.getValue())) {
                        dstTpId = linkDstTpId;
                    } else {
                        dstTpId = srcTpId;
                        srcTpId = linkDstTpId;
                    }
                } else if (srcTpId.getValue().contains(startEqId)) {
                    dstTpId = linkDstTpId;
                } else {
                    dstTpId = srcTpId;
                    srcTpId = linkDstTpId;
                }
            }

            startTp = dstTpId.getValue();
            LinkId linkId = link.getLinkId();

            //site Link's route will include MPOn, but OCH's route link used is merged MPO port
            if (routeType.equals(RouteType.SiteLink) && hasMpoAggregatingCard && srcTpId.getValue().contains("MPO")) {
                if (isMPO(srcTpId)) {
                    //8个MPO端口 合并为一个虚拟端口
                    if (!hasDualFmux32) {
                        trackMergedMpoEquipments(mpoxMergeHappened, startEqId,
                                dstTpId.getValue(), tpZ);
                    }

                    srcTpId = convertMPOTP(srcTpId);
                    dstTpId = convertMPOTP(dstTpId);

                    linkId = new LinkId(PhysicalLinkIdNamingRule.createLinkId(srcTpId.getValue(), dstTpId.getValue(), LinkType.OmsLink));
                }
            } else if (routeType.equals(RouteType.OchLink) && !isC_L) {
                //构成OCH Link的 连接中如果存在MPO
                if (SiteLinkIdNamingRule.isSiteLink(link.getLinkId().getValue())) {
                    if (srcTpId.getValue().contains(PhysicalTpIdNamingRule.getSiteId(link.getSource().getSourceTp().getValue()))) {
                        srcTpId = link.getSource().getSourceTp();
                        dstTpId = link.getDestination().getDestTp();
                    } else {
                        srcTpId = link.getDestination().getDestTp();
                        dstTpId = link.getSource().getSourceTp();
                    }
                    if (srcTpId.getValue().contains("MPO")) {
                        srcTpId = new TpId(srcTpId.getValue() + actuallyMpon);
                    }
                    if (dstTpId.getValue().contains("MPO")) {
                        dstTpId = new TpId(dstTpId.getValue() + actuallyMpon);
                    }

                } else {
                    String[] tmp = srcTpId.getValue().split("MPO");
                    if (tmp.length > 1) {
                        actuallyMpon = tmp[1];
                        continue;
                    }
                }
            }

            proList.add(getTpPathRouteObject(new TopologyId(TopoNameConstants.Phy_Topo_Key), srcTpId, index++));
            proList.add(getLinkPathRouteObject(underLayerTopoId, linkId, index++));
            proList.add(getTpPathRouteObject(new TopologyId(TopoNameConstants.Phy_Topo_Key), dstTpId, index++));
        }

        if (routeType.equals(RouteType.Tunnel)) {
            if (tpZ == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "tunnel Z is empty, this is impossible");
            } else {
                proList.add(getTpPathRouteObject(currentLayerTopoId, new TpId(tpZ), index++));
            }
        }

        return proList;
    }

    static TpId resolveSameEquipmentEndpoint(String currentTp, TpId firstEndpoint, TpId secondEndpoint,
            List<CrossConnections> routeXcs) {
        String currentEquipmentId = PhysicalTpIdNamingRule.getEquipId(currentTp);
        if (currentTp.equals(firstEndpoint.getValue()) || currentTp.equals(secondEndpoint.getValue())
                || !currentEquipmentId.equals(PhysicalTpIdNamingRule.getEquipId(firstEndpoint.getValue()))
                || !currentEquipmentId.equals(PhysicalTpIdNamingRule.getEquipId(secondEndpoint.getValue()))
                || routeXcs == null) {
            return null;
        }

        for (CrossConnections xc : routeXcs) {
            boolean currentIsSource = xc.getSourceTp() != null && xc.getSourceTp().stream()
                    .anyMatch(tp -> currentTp.equals(tp.getTpRef().getValue()));
            boolean currentIsDestination = xc.getDestinationTp() != null && xc.getDestinationTp().stream()
                    .anyMatch(tp -> currentTp.equals(tp.getTpRef().getValue()));

            if (currentIsSource && xc.getDestinationTp() != null) {
                if (xc.getDestinationTp().stream()
                        .anyMatch(tp -> firstEndpoint.getValue().equals(tp.getTpRef().getValue()))) {
                    return firstEndpoint;
                }
                if (xc.getDestinationTp().stream()
                        .anyMatch(tp -> secondEndpoint.getValue().equals(tp.getTpRef().getValue()))) {
                    return secondEndpoint;
                }
            }
            if (currentIsDestination && xc.getDirection() == LinkDirection.Bidirection
                    && xc.getSourceTp() != null) {
                if (xc.getSourceTp().stream()
                        .anyMatch(tp -> firstEndpoint.getValue().equals(tp.getTpRef().getValue()))) {
                    return firstEndpoint;
                }
                if (xc.getSourceTp().stream()
                        .anyMatch(tp -> secondEndpoint.getValue().equals(tp.getTpRef().getValue()))) {
                    return secondEndpoint;
                }
            }
        }
        return null;
    }

    static ApsRouteSelection selectAEndApsRoute(List<CrossConnections> xcs, String aTp,
            List<Link> slaveLinks, List<Link> thirdLinks) {
        List<CrossConnections> candidates = xcs.stream()
                .filter(xc -> xc.getAps() != null
                        && aTp.contains(xc.getNodeRef().getValue()))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1 || slaveLinks == null || thirdLinks == null) {
            CrossConnections apsXc = candidates.get(0);
            return new ApsRouteSelection(apsXc, getApsBTp(apsXc), getApsCTp(apsXc));
        }

        List<ApsRouteSelection> matched = candidates.stream()
                .map(apsXc -> matchApsRoute(apsXc, slaveLinks, thirdLinks))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (matched.size() != 1) {
            String candidateIds = candidates.stream()
                    .map(Route::getCrossConnectionId)
                    .collect(Collectors.joining(","));
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("cannot uniquely match A-end APS to slave and third links, candidates: [%s], matched: %d",
                            candidateIds, matched.size()));
        }
        return matched.get(0);
    }

    private static ApsRouteSelection matchApsRoute(CrossConnections apsXc,
            List<Link> slaveLinks, List<Link> thirdLinks) {
        Set<String> fanOutTpIds = getFanOutTpIds(apsXc);
        if (fanOutTpIds.isEmpty()) {
            return null;
        }
        String slaveStartTp = findUniqueRouteTp(fanOutTpIds, slaveLinks);
        String thirdStartTp = findUniqueRouteTp(fanOutTpIds, thirdLinks);
        if (slaveStartTp == null || thirdStartTp == null
                || slaveStartTp.equals(thirdStartTp)) {
            return null;
        }
        return new ApsRouteSelection(apsXc, slaveStartTp, thirdStartTp);
    }

    private static Set<String> getFanOutTpIds(CrossConnections apsXc) {
        if (apsXc.getSourceTp().size() > apsXc.getDestinationTp().size()) {
            return apsXc.getSourceTp().stream()
                    .map(tp -> tp.getTpRef().getValue())
                    .collect(Collectors.toSet());
        }
        if (apsXc.getDestinationTp().size() > apsXc.getSourceTp().size()) {
            return apsXc.getDestinationTp().stream()
                    .map(tp -> tp.getTpRef().getValue())
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private static String findUniqueRouteTp(Set<String> candidateTpIds, List<Link> links) {
        Set<String> routeTpIds = links.stream()
                .flatMap(link -> Arrays.asList(link.getSource().getSourceTp().getValue(),
                        link.getDestination().getDestTp().getValue()).stream())
                .filter(candidateTpIds::contains)
                .collect(Collectors.toSet());
        return routeTpIds.size() == 1 ? routeTpIds.iterator().next() : null;
    }

    private static String getCrossConnectionId(CrossConnections apsXc) {
        return apsXc.getCrossConnectionId() == null ? "<unknown>"
                : apsXc.getCrossConnectionId().getValue();
    }

    static final class ApsRouteSelection {

        private final CrossConnections apsXc;
        private final String slaveStartTp;
        private final String thirdStartTp;

        private ApsRouteSelection(CrossConnections apsXc, String slaveStartTp,
                String thirdStartTp) {
            this.apsXc = apsXc;
            this.slaveStartTp = slaveStartTp;
            this.thirdStartTp = thirdStartTp;
        }

        CrossConnections getApsXc() {
            return apsXc;
        }

        String getSlaveStartTp() {
            return slaveStartTp;
        }

        String getThirdStartTp() {
            return thirdStartTp;
        }
    }

    static void trackMergedMpoEquipments(List<String> mergedEquipments, String startEquipmentId,
            String destinationTpId, String zTpId) {
        mergedEquipments.add(startEquipmentId);
        if (zTpId != null && destinationTpId.contains(zTpId)) {
            mergedEquipments.add(PhysicalTpIdNamingRule.getEquipId(destinationTpId));
        }
    }

    static String mergedMpoLinkKey(String firstTpId, String secondTpId) {
        if (!firstTpId.matches(".*MPO\\d+$") || !secondTpId.matches(".*MPO\\d+$")) {
            return null;
        }
        String firstMergedTp = firstTpId.replaceFirst("MPO\\d+$", "MPO");
        String secondMergedTp = secondTpId.replaceFirst("MPO\\d+$", "MPO");
        return firstMergedTp.compareTo(secondMergedTp) <= 0
                ? firstMergedTp + "|" + secondMergedTp
                : secondMergedTp + "|" + firstMergedTp;
    }

    private boolean isMergedMPO(TpId srcTpId) {
        if (srcTpId.getValue().substring(srcTpId.getValue().length() - 3).equals("MPO")) {
            return true;
        } else {
            return false;
        }
    }

    private TpId convertMPOTP(TpId phyTpId) {
        String[] tmp = phyTpId.getValue().split("MPO");
        if (tmp.length > 1) {
            return new TpId(tmp[0] + "MPO");
        }
        return phyTpId;
    }

    private boolean isMPO1(TpId tpId) {
        String[] tmp = tpId.getValue().split("MPO");
        if (tmp.length > 1 && tmp[1].equals("1")) {
            return true;
        }
        return false;
    }

    private boolean isMPO(TpId tpId) {
        if (hasMpoAggregatingCard && tpId.getValue().contains("MPO")) {
            return true;
        } else {
            return false;
        }
    }

    public static PathRouteObject getLinkPathRouteObject(TopologyId underLayerTopoId, LinkId linkId, long index) {
        PathRouteObjectBuilder proBuilder = new PathRouteObjectBuilder()
                .setIndex(index)
                .setTopologyRef(underLayerTopoId)
                .setResourceType(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.LinkBuilder().setLinkHop(new LinkHopBuilder()
                        .setLinkRef(linkId)
                        .setTopologyRef(underLayerTopoId)
                        .build()).build());
        proBuilder.setKey(new PathRouteObjectKey(proBuilder.getIndex()));

        return proBuilder.build();
    }

    public static PathRouteObject getTpPathRouteObject(TopologyId underLayerTopoId, TpId srcTpId, long index) {
        PathRouteObjectBuilder proBuilder = new PathRouteObjectBuilder()
                .setIndex(index)
                .setTopologyRef(underLayerTopoId)
                .setResourceType(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.TpBuilder().setTpHop(new TpHopBuilder()
                        .setTpRef(srcTpId)
                        .setEquipmentRef(PhysicalNodeIdNamingRule.getEquipId(srcTpId.getValue()))
                        .setNodeRef(new NodeId(PhysicalNodeIdNamingRule.getPhyNodeId(srcTpId.getValue())))
                        .setSiteRef(new NodeId(PhysicalNodeIdNamingRule.getSiteId(srcTpId.getValue())))
                        .build()).build());
        proBuilder.setKey(new PathRouteObjectKey(proBuilder.getIndex()));

        return proBuilder.build();
    }

    public static List<String> getNodeIdOverRoute(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route> linkRouteList) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcList = new ArrayList<>();

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route : linkRouteList) {
            xcList.addAll(route.getPrimary().getCrossConnections());
            if (route.getSecondary() != null) {
                xcList.addAll(route.getSecondary().getCrossConnections());
            }
        }

        Set<String> nodeIdList = xcList.stream().map(xc -> PhysicalTpIdNamingRule.getNodeId(xc.getSourceTp().get(0).getTpRef().getValue()))
                .collect(Collectors.toSet());

        return new ArrayList<>(nodeIdList);
    }
}
