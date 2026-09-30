/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.VoaUpdateModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.SwitchSpcInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.SwitchSpcOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.SwitchSpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHopBuilder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.flex.dci.otc.common.util.TopoNameConstants.Phy_Topo_Key;
import static net.flex.dci.otc.common.util.TopoNameConstants.Site_Topo_Key;

/**
 * @version 1.0
 */
@Slf4j
@Service
public class TunnelSwitch {
    private ChangedObject changedObject;
    private Link oldOchLink;
    private GridType ochGrid;
    private Long oldFrequency;
    private TaskInfoMessage taskInfoMessage;

    public TunnelSwitch setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
        return this;
    }

    //===================================================
	//
	// init env
	//
	//===================================================
	public SwitchSpcOutput start(SwitchSpcInput input) throws CommonException {
		log.info("start to switch tunnel route");
        try {
            changedObject = new ChangedObject();
            Tunnel tunnel = checkParam(input); //oldOchLink has retreived, and protectionType is SPC

            getOchParam(oldOchLink);
            Link newOchLink = getNewOch(input.getSiteLinkRoute());
            List<String> impledTunnelIdList = new ArrayList<>();
            List<String> notImpledTunnelIdList = new ArrayList<>();
            impactedTunnel(oldOchLink, impledTunnelIdList, notImpledTunnelIdList);
            deimplementAllTunnels(impledTunnelIdList);
            updatePhyNodeXcInfo(oldOchLink, newOchLink);
            updateOchLinkRouteInfo(oldOchLink, newOchLink);
            updateSiteLinkFrequencyInfo(oldOchLink, newOchLink);

            MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
//            for (String tunnelId : notImpledTunnelIdList) {
//                changedObject.unsetTunnel(tunnelId);
//            }
//            for (String tunnelId : impledTunnelIdList) {
//                changedObject.unsetTunnel(tunnelId);
//            }

            log.info("switch done");
            mongoTransaction.save(changedObject);
            log.info("has stored in DB");

            implementAllTunnels(impledTunnelIdList);
        }catch (Exception e) {
            log.error("error happen", e);
            throw e;
        }
        
        return new SwitchSpcOutputBuilder().setReturnCode(RpcResultType.Success).build();
	}

    private void updateSiteLinkFrequencyInfo(Link oldOchLink, Link newOchLink) {
        Och oldOchLinkAttr = oldOchLink.getAugmentation(Link1.class).getOch();
        List<SupportingLink> impactedSiteLinkList = oldOchLink.getSupportingLink().stream().filter(x ->
                SiteLinkIdNamingRule.isSiteLink(x.getLinkRef().getValue())).collect(Collectors.toList());
        for (SupportingLink sl : impactedSiteLinkList) {
            Link siteLink = changedObject.getChangedSiteLink(sl.getLinkRef().getValue());
            updateSiteLinkAvailable(siteLink, oldOchLinkAttr.getLowerFrequency(), oldOchLinkAttr.getUpperFrequency(), true);
            updateChangeSiteLinkBandwidth(siteLink, +1);
        }

        Och newOchLinkAttr = newOchLink.getAugmentation(Link1.class).getOch();
        impactedSiteLinkList = newOchLink.getSupportingLink().stream().filter(x ->
                SiteLinkIdNamingRule.isSiteLink(x.getLinkRef().getValue())).collect(Collectors.toList());
        for (SupportingLink sl : impactedSiteLinkList) {
            Link siteLink = changedObject.getChangedSiteLink(sl.getLinkRef().getValue());
            updateSiteLinkAvailable(siteLink, newOchLinkAttr.getLowerFrequency(), newOchLinkAttr.getUpperFrequency(), false);
            updateChangeSiteLinkBandwidth(siteLink, -1);
        }
    }


    /**
     * copy from net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater
     * @param delta
     */
    private void updateChangeSiteLinkBandwidth(Link siteLink, int delta) {
        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        siteLink = new LinkBuilder(siteLink)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                        .setSite(new SiteBuilder(siteLinkAttr)
                                .setBandwidth(String.valueOf(Integer.valueOf(siteLinkAttr.getBandwidth()) + delta))
                                .build()
                        ).build()
                ).build();
        changedObject.addChangedSiteLink(siteLink);
    }


    /**
     * copy from net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater
     * @param lowerFrequency
     * @param upperFrequency
     * @param increase
     */
    private void updateSiteLinkAvailable(Link siteLink, FrequencyType lowerFrequency, FrequencyType upperFrequency, boolean increase) {
        Available ava = new AvailableBuilder()
                .setLowerFrequency(lowerFrequency)
                .setUpperFrequency(upperFrequency)
                .setKey(new AvailableKey(lowerFrequency))
                .build();
        FrequencyAvailable frequencyAvailable = new FrequencyAvailable(siteLink);
        if (increase)
            frequencyAvailable.add(ava);
        else
            frequencyAvailable.remove(ava);


        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        siteLink = new LinkBuilder(siteLink)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                        .setSite(new SiteBuilder(siteLinkAttr)
                                .setAvailable(frequencyAvailable.getAvailableList())
                                .build()
                        ).build()
                ).build();
        changedObject.addChangedSiteLink(siteLink);
    }

    private void updateOchLinkRouteInfo(Link oldOchLink, Link newOchLink) {
        changedObject.addChangedOchLink(newOchLink);
    }

    private void updatePhyNodeXcInfo(Link oldOchLink, Link newOchLink) {
        Och oldOchLinkAttr = oldOchLink.getAugmentation(Link1.class).getOch();
        for (CrossConnections xc :oldOchLinkAttr.getExplictRoute().getRoute().get(0).getPrimary().getCrossConnections()) {
            if (!xc.isFixed()) {
                //for demo och 层的固定交叉都是M?D? 到 muxDmux 的
                Node node = changedObject.getChangedPhyNode(xc.getNodeRef().getValue());
                Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

                List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> newXcList =
                        nodeAttr.getCrossConnections().stream().filter(x->
                            !x.getCrossConnectionId().getValue().equals(xc.getCrossConnectionId().getValue())
                        ).collect(Collectors.toList());

                Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                                .setCrossConnections(newXcList).build())
                        .build())
                        .build();
                changedObject.addChangedPhyNode(newNode);
            }
        }

        Och newOchLinkAttr = newOchLink.getAugmentation(Link1.class).getOch();
        for (CrossConnections xc :newOchLinkAttr.getExplictRoute().getRoute().get(0).getPrimary().getCrossConnections()) {
            if (!xc.isFixed()) {
                //for demo och 层的固定交叉都是M?D? 到 muxDmux 的
                Node node = changedObject.getChangedPhyNode(xc.getNodeRef().getValue());
                Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

                List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> newXcList =
                        nodeAttr.getCrossConnections();
                newXcList.add(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder(xc)
                        .build());

                Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                                .setPhysical(new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                                        .setCrossConnections(newXcList).build())
                                .build())
                        .build();
                changedObject.addChangedPhyNode(newNode);
            }
        }


    }

    private void deimplementAllTunnels(List<String> impledTunnelIdList) {
        for (String tunnelId : impledTunnelIdList) {
            Tunnel tunnel = changedObject.getChangedTunnel(tunnelId);
            log.debug("start deImplement tunnel {}", tunnel.getFriendlyName());

            LifeCycleSevice lifeService = new LifeCycleSevice();
            lifeService.logStartLinkImpl(tunnel.getTunnelId().getValue(),
                    TaskInfoMessage.ResourceType.tunnel,
                    tunnel.getFriendlyName(),
                    TaskInfoMessage.ActionType.deimplement,
                    taskInfoMessage.getWho(), null);

            TunnelImplementor implementor = new TunnelImplementor(tunnelId, ImplementState.Allocate, lifeService);
            implementor.startSyncAction();
        }

        log.info("waiting implement complete.....");

        boolean allDone = false;
        while (!allDone) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                //do nothing
            }
            allDone = true; //start checking
            for (String tunnelId : impledTunnelIdList) {
                changedObject.unsetTunnel(tunnelId);
                Tunnel tunnel = changedObject.getChangedTunnel(tunnelId);
                if (tunnel.getImplementState().equals(ImplementState.Implement)) {
                    log.info("tunnel hasn't deimplement {}", tunnel.getFriendlyName());
                    allDone = false;
                    break;
                }
            }
        }

        log.info("all tunnel deImplement complete");
    }

    private void implementAllTunnels(List<String> impledTunnelIdList) {
        for (String tunnelId : impledTunnelIdList) {
            Tunnel tunnel = changedObject.getChangedTunnel(tunnelId);
            log.debug("start deImplement tunnel {}", tunnel.getFriendlyName());

            LifeCycleSevice lifeService = new LifeCycleSevice();
            lifeService.logStartLinkImpl(tunnel.getTunnelId().getValue(),
                    TaskInfoMessage.ResourceType.tunnel,
                    tunnel.getFriendlyName(),
                    TaskInfoMessage.ActionType.implement,
                    taskInfoMessage.getWho(), null);

            TunnelImplementor implementor = new TunnelImplementor(tunnelId, ImplementState.Implement, lifeService);
            implementor.startSyncAction();
        }
        log.info("all tunnel implement with new route again");
    }

    private void impactedTunnel(Link oldOchLink, List<String> impledTunnelIdList, List<String> notImpledTunnelIdList) {
        List<SupportedTunnel> supportedTunnel = oldOchLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class).getSupportedTunnel();
        supportedTunnel.stream().map(x->{
            Tunnel tunnel = changedObject.getChangedTunnel(x.getTunnelRef().getValue());
            if (tunnel.getImplementState().equals(ImplementState.Allocate)) {
                notImpledTunnelIdList.add(tunnel.getTunnelId().getValue());
            } else {
                impledTunnelIdList.add(tunnel.getTunnelId().getValue());
            }
            return null;
        }).count();
    }

    ;

    /**
     * 当前只支持不改变频率， 因为要支持改变频率， 那么需要CMUX板卡，
     * CMUX 板卡和 WSS9 两块板卡就把网元槽位占满了，
     * 现在我们还不支持在两个网元上完成一个TOADM节点
     * @param siteLinkRoute
     * @return
     */
    private Link getNewOch(SiteLinkRoute siteLinkRoute) {
        List<Long> centralFrequencies = null;

        for (String linkId : siteLinkRoute.getPrimary()) {
            if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                Link siteLink = changedObject.getChangedSiteLink(linkId);
                if (oldOchLink.getSupportingLink().stream().filter(
                        sl->sl.getLinkRef().getValue().equals(siteLink.getLinkId().getValue())).findAny().isPresent()) {
                    //跳过共用的siteLink 检查
                    continue;
                }

                if (centralFrequencies == null) {
                    centralFrequencies = new FrequencyAvailable(siteLink).getAllPossibleCentFrequency(ochGrid);
                } else {
                    List<Long> anotherLinkFrequencies = new FrequencyAvailable(siteLink).getAllPossibleCentFrequency(ochGrid);
                    centralFrequencies = centralFrequencies.stream()
                            .filter(anotherLinkFrequencies::contains)
                            .collect(Collectors.toList());
                }
            }
        }

        if (centralFrequencies == null || !centralFrequencies.contains(oldFrequency)) {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR, "only support change och route in FOADM now ");
        }

        Och ochLinkAttr = oldOchLink.getAugmentation(Link1.class).getOch();

        List<ExplicitRouteObjects>  newEroList = createRouteInfo(ochLinkAttr, siteLinkRoute);
        List<CrossConnections> newXcList = createXcInfo(ochLinkAttr, siteLinkRoute);
        ExplictRoute explictRoute = buildRoute(ochLinkAttr, newEroList, newXcList);

        List<SupportingLink> newSupportingLinkList = oldOchLink.getSupportingLink().stream().filter(x ->
                        PhysicalLinkIdNamingRule.isOsLink((x.getLinkRef().getValue())))
                .collect(Collectors.toList());
        newSupportingLinkList.addAll(
                siteLinkRoute.getPrimary().stream().map(x-> {
                LinkId linkId = new LinkId(x);
                return new SupportingLinkBuilder()
                        .setLinkRef(linkId)
                        .setKey(new SupportingLinkKey(linkId))
                        .build();
            }).collect(Collectors.toList())
        );
        return new LinkBuilder(oldOchLink).addAugmentation(Link1.class, new Link1Builder()
                    .setOch(new OchBuilder(ochLinkAttr)
                            .setExplictRoute(explictRoute)
                            .build())
                    .build())
                .setSupportingLink(newSupportingLinkList)
                .build();
    }

    private ExplictRoute buildRoute(Och ochLinkAttr, List<ExplicitRouteObjects> newEroList, List<CrossConnections> newXcList) {
        Primary primary = new PrimaryBuilder().setExplicitRouteObjects(newEroList)
                        .setCrossConnections(newXcList)
                        .build();

        List<Route> routeList = new ArrayList<>();
        routeList.add(new RouteBuilder(ochLinkAttr.getExplictRoute().getRoute().get(0))
                .setPrimary(primary)
                .build());

        return new ExplictRouteBuilder().setRoute(routeList).build();
    }

    /**
     * this is for demo, code is fixed, should change later
     *
     * @return
     */
    private List<CrossConnections> createXcInfo(Och oldOchLinkAttr, SiteLinkRoute siteLinkRoute) {
        List<CrossConnections> oldXcList = oldOchLinkAttr.getExplictRoute().getRoute().get(0).getPrimary().getCrossConnections();

        /**
         * first one item, and lastest one item are fixed in och link route, they are MUX card's XC
         * M?D? --- muxDmux
         */

        List<CrossConnections> newXcList = new ArrayList<>();
        
        newXcList.add(oldXcList.get(0));  //index=1 M1D1--MUXDMUX
        NodeId nextNodeId = oldXcList.get(0).getNodeRef();

        List<TpId> nextXcRelatedTps = null;
        int siteLinkRouteLength = siteLinkRoute.getPrimary().size();
        for (int i = 0; i < siteLinkRouteLength; i++) {
            String linkId = siteLinkRoute.getPrimary().get(i);
            if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                if (i == 0) {
                    nextXcRelatedTps = ochXcStartFromSiteLink(newXcList, nextNodeId, linkId);
                } else if (i == siteLinkRouteLength - 1) {
                    nextXcRelatedTps = ochXcStartFromWssLink(newXcList, nextXcRelatedTps, linkId);
                }
            } else {
                //wssLink, 涉及的两个端口都在同一个网元，这两个端口需要创建两个与OA-Line口的交叉，这个只能基于nextXcRelatedTps 判断
                nextXcRelatedTps = ochXcWithWssLink(newXcList, nextXcRelatedTps, linkId, siteLinkRoute.getPrimary().get(i + 1));
            }
        }

        int index = newXcList.size() + 1;
        newXcList.add(new CrossConnectionsBuilder(oldXcList.get(oldXcList.size() - 1))
                .setSequence(Long.valueOf(index))
                .setKey(new CrossConnectionsKey(Long.valueOf(index)))
                .build());  //M1D1--MUXDMUX
        return newXcList;
    }

    /**
     * WSS link 连接两个复用段，所以这个有可能是同一个网元上面两张WSS板卡，两张OA板卡
     * 也有可能是一个分别处于两个不同网元
     * 我们需要基于这个wssLink 创建两个xc, 分别对应wss--oa
     *
     * @param newXcList
     * @param nextXcRelatedTps
     * @param wssLinkId
     * @param nextSiteLinkId
     * @return
     */
    private List<TpId> ochXcWithWssLink(List<CrossConnections> newXcList, List<TpId> nextXcRelatedTps, String wssLinkId, String nextSiteLinkId) {
        String oaLineTp = nextXcRelatedTps.get(0).getValue();
        String wssTp = nextXcRelatedTps.get(1).getValue();
        String wssEquipStr = PhysicalTpIdNamingRule.getEquipId(wssTp);

        TpId nextWssTpId;
        NodeId nextXcNodeId;
        if (PhysicalLinkIdNamingRule.getTpAId(wssLinkId).contains(wssEquipStr)) {
            wssTp = PhysicalLinkIdNamingRule.getTpAId(wssLinkId);
            nextXcNodeId = new NodeId(PhysicalLinkIdNamingRule.getNodeZId(wssLinkId));
            nextWssTpId = new TpId(PhysicalLinkIdNamingRule.getTpZId(wssLinkId));
        } else {
            wssTp = PhysicalLinkIdNamingRule.getTpZId(wssLinkId);
            nextXcNodeId = new NodeId(PhysicalLinkIdNamingRule.getNodeAId(wssLinkId));
            nextWssTpId = new TpId(PhysicalLinkIdNamingRule.getTpAId(wssLinkId));
        }

        NodeId xcNodeId = new NodeId(PhysicalTpIdNamingRule.getNodeId(oaLineTp));
        CrossConnections xc = createWssXC(xcNodeId, new TpId(wssTp), new TpId(oaLineTp),  newXcList.size() + 1);
        newXcList.add(xc);

        List<TpId> xcRelatedTps = getOaWssTpInSiteLink(nextXcNodeId, nextSiteLinkId);
        xc = createWssXC(xcNodeId, xcRelatedTps.get(0), nextWssTpId, newXcList.size() + 1);
        newXcList.add(xc);

        return Arrays.asList(xcRelatedTps.get(2), xcRelatedTps.get(3));
    }


    /**
     * 前面一段是wssLink, 后面一段是siteLink
     * wss相关的交叉已经在wssLink 中处理，这里主要是生成siteLink 对应的交叉
     * 如
     * wssLink------siteLink
     * exp1--line========line---exp2
     * @param newXcList
     * @param nextXcRelatedTps
     * @param siteLinkId
     * @return
     */
    private List<TpId> ochXcStartFromWssLink(List<CrossConnections> newXcList, List<TpId> nextXcRelatedTps, String siteLinkId) {
        TpId prevwssTpId = nextXcRelatedTps.get(0);
        NodeId xcNodeId = new NodeId(PhysicalTpIdNamingRule.getNodeId(prevwssTpId.getValue()));
        List<TpId> xcRelatedTps = getOaWssTpInSiteLink(xcNodeId, siteLinkId);
        CrossConnections xc = createWssXC(xcNodeId, xcRelatedTps.get(0), prevwssTpId, newXcList.size() + 1);
        newXcList.add(xc);

        return Arrays.asList(xcRelatedTps.get(2), xcRelatedTps.get(3));
    }

    /**
     * 返回的是一对端口，(0)--OA Line port, (1)--WSS AddDrop port,
     * 根据WSS AddDrop port 结合后面的wssLink可以知道新的xc 如何创建
     *
     * @param newXcList
     * @param xcNodeId
     * @param siteLinkId
     * @return
     */
    private List<TpId> ochXcStartFromSiteLink(List<CrossConnections> newXcList, NodeId xcNodeId, String siteLinkId) {

        List<TpId> xcRelatedTps = getOaWssTpInSiteLink(xcNodeId, siteLinkId);
        CrossConnections xc = createWssXC(xcNodeId, xcRelatedTps.get(0), xcRelatedTps.get(1), newXcList.size() + 1);
        newXcList.add(xc);

        return Arrays.asList(xcRelatedTps.get(2), xcRelatedTps.get(3));
    }

    /**
     * 一个ROADM复用段涉及两端的OA 和 WSS 端口，找出并返回。
     * 与xcNodeId 相关的放0/1， 对端的放2/3
     *  如
     *  siteLink
     *  muxDmux---addDrop---Line========line---addDrop---muxDmux
     *
     *  返回的4个点参考上图
     *
     * @param xcNodeId
     * @param siteLinkId
     * @return
     */
    private List<TpId> getOaWssTpInSiteLink(NodeId xcNodeId, String siteLinkId) {
        TpId wssTpId = null;
        TpId oaLineTpId = null;
        TpId peerWssTpId = null;
        TpId peerOaLineTpId = null;

        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        List<PathRouteObject> proList = siteLinkAttr.getExplictRoute().getRoute().get(0).getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject();
        for (PathRouteObject pro : proList) {
            if (pro.getResourceType() instanceof Tp) {
                TpHop tpHop = ((Tp) pro.getResourceType()).getTpHop();

                if (tpHop.getTpRef().getValue().endsWith("LINE")) {
                    if (tpHop.getTpRef().getValue().contains(xcNodeId.getValue())) {
                        oaLineTpId = tpHop.getTpRef();
                    } else {
                        peerOaLineTpId = tpHop.getTpRef();
                    }
                } else if (tpHop.getTpRef().getValue().endsWith("ADDDROP")) {
                    if (tpHop.getTpRef().getValue().contains(xcNodeId.getValue())) {
                        wssTpId = tpHop.getTpRef();
                    } else {
                        peerWssTpId = tpHop.getTpRef();
                    }
                }
            }
        }
        if (wssTpId == null || oaLineTpId == null || peerOaLineTpId == null || peerWssTpId == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find pair of wss and oa port on siteLink " + siteLinkId);
        }

        return Arrays.asList(new TpId(oaLineTpId), new TpId(wssTpId), new TpId(peerOaLineTpId), new TpId(peerWssTpId));
    }

    private TpId getOaLineTpFromSiteLink(NodeId xcNodeId, String siteLinkId) {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        List<PathRouteObject> proList = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite().getExplictRoute().getRoute().get(0).getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject();

        for (PathRouteObject pro : proList) {
            if (pro.getResourceType() instanceof Tp) {
                TpHop tpHop = ((Tp) pro.getResourceType()).getTpHop();
                if (tpHop.getTpRef().getValue().endsWith("LINE") && tpHop.getTpRef().getValue().contains(xcNodeId.getValue())) {
                    return tpHop.getTpRef();
                }
            }
        }

        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("cannot find out OA LINE port in siteLink %s, %s", xcNodeId.getValue(), siteLinkId));
    }

    private String getTpOchSlot() {
        long step = getStep(ochGrid);
        // /frequency=196050000,196125000
        return String.format("/frequency=%d,%d", oldFrequency - step, oldFrequency + step);
    }

    private WssChannel getWssChannel() {
        long step = getStep(ochGrid);
        return new WssChannelBuilder()
                .setVoaUpdateModel(VoaUpdateModel.Manually)
                .setLowerFrequency(new FrequencyType(BigInteger.valueOf(oldFrequency - step)))
                .setUpperFrequency(new FrequencyType(BigInteger.valueOf(oldFrequency + step)))
                .setDestToSourceVoa(BigDecimal.ZERO)
                .setSourceToDestVoa(BigDecimal.ZERO)
                .build();
    }

    private CrossConnections createWssXC(NodeId xcNodeId, TpId oaLineTpId, TpId wssTpId, int index) {
        List<String> tpIdList = Arrays.asList(wssTpId.getValue(), oaLineTpId.getValue());
        String xcId = buildXcId(tpIdList, true);
        String description = String.format("%s/%d", PhysicalTpIdNamingRule.getShortTpByTpId(wssTpId.getValue()), oldFrequency);

        List<SourceTp> sTPs = new ArrayList<>();
        sTPs.add(new SourceTpBuilder()
                .setTpRef(wssTpId)
                .setSlot(getTpOchSlot())
                .build());
        List<DestinationTp> dTPs = new ArrayList<>();
        dTPs.add(new DestinationTpBuilder()
                .setTpRef(new TpId(oaLineTpId))
                .setSlot(getTpOchSlot())
                .build());

        CrossConnections xc = new CrossConnectionsBuilder()
                .setSequence((long)index)
                .setAdminState(AdminStatus.Unknown)
                .setImplementState(ImplementState.Allocate)
                .setOperationalState(OperStatus.Unknown)
                .setCrossConnectionId(new Uri(xcId))
                .setDescription(description)
                .setDestinationTp(dTPs)
                .setDirection(LinkDirection.Bidirection)
                .setFixed(false)
                .setKey(new CrossConnectionsKey((long)index))
                .setNodeRef(xcNodeId)
                .setSourceTp(sTPs)
                .setAmplifier(null)
                .setAps(null)
                .setWssChannel(getWssChannel())
                .build();

        return xc;
    }

    /**
     * copy from  net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
     * @param tpIds
     * @param biDirection
     * @return
     */
    private String buildXcId(List<String> tpIds, boolean biDirection) {
        List<String> ctpIds = tpIds.stream().map(x -> String.format("%s/%d", x, oldFrequency)).collect(Collectors.toList());
        if (biDirection) {
            return ctpIds.stream().sorted().collect(Collectors.joining("-", "XC-", ""));
        }
        return ctpIds.stream().collect(Collectors.joining("-", "XC-", ""));
    }

    private List<ExplicitRouteObjects> createRouteInfo(Och oldOchLinkAttr, SiteLinkRoute siteLinkRoute) throws CommonException {
        if (oldOchLinkAttr.getExplictRoute().getRoute().size() != 1) {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR, "only support one route in this version");
        }

        List<ExplicitRouteObjects> oldEroList = oldOchLinkAttr.getExplictRoute().getRoute().get(0).getPrimary().getExplicitRouteObjects();
        List<PathRouteObject> oldProList = oldEroList.get(0).getPathRouteObject();

        /**
         * first 4 items, and lastest 4 items are fixed in och link route, they are L port
         * TP: OT L port,
         * phyLink: L port to Mux card,
         * TP: Mux card, M?D? port
         * TP: Mux card MuxDmux port (this is key key point, help construct next pro)
         */

        List<PathRouteObject> newProList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            newProList.add(oldProList.get(i));
        }

        for (int i=0; i < siteLinkRoute.getPrimary().size(); i++) {
            String linkId = siteLinkRoute.getPrimary().get(i);
            insertLinkPro(newProList, linkId);
        }

        int index = newProList.size() + 1;
        for (int i = oldProList.size() - 3; i<oldProList.size(); i++) {
            newProList.add(new PathRouteObjectBuilder(oldProList.get(i))
                    .setKey(new PathRouteObjectKey(Long.valueOf(index)))
                    .setIndex(Long.valueOf(index++)).build());
        }
        List<ExplicitRouteObjects> newEro = new ArrayList<>();
        newEro.add(new ExplicitRouteObjectsBuilder(oldEroList.get(0)).setPathRouteObject(newProList).build());

        return newEro;
    }

    private void insertLinkPro(List<PathRouteObject> newProList, String linkId) {
        String topoType = null;
        String latestTpId = ((Tp)newProList.get(newProList.size() - 1).getResourceType()).getTpHop().getTpRef().getValue();
        String latestNodeId = PhysicalTpIdNamingRule.getNodeId(latestTpId);

        Link link;
        if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
            topoType = Site_Topo_Key;
            link = changedObject.getChangedSiteLink(linkId);
        } else if (PhysicalLinkIdNamingRule.isWssLink(linkId)) {
            topoType = Phy_Topo_Key;
            link = changedObject.getChangedPhyLink(linkId);
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "the route infomation of siteLinkRoute only support siteLink and wssLink, but now I get one " + linkId);
        }

        int index = newProList.size() + 1;
        if (link.getSource().getSourceTp().getValue().contains(latestNodeId)) {
            newProList.add(getTpPathRouteObject(new TopologyId(topoType), link.getSource().getSourceTp(), index++));
            newProList.add(getLinkPathRouteObject(new TopologyId(topoType), link.getLinkId(), index++));
            newProList.add(getTpPathRouteObject(new TopologyId(topoType), link.getDestination().getDestTp(), index++));
        } else {
            newProList.add(getTpPathRouteObject(new TopologyId(topoType), link.getDestination().getDestTp(), index++));
            newProList.add(getLinkPathRouteObject(new TopologyId(topoType), link.getLinkId(), index++));
            newProList.add(getTpPathRouteObject(new TopologyId(topoType), link.getSource().getSourceTp(), index++));
        }
    }

    private int insertLinkPro(List<PathRouteObject> newPro, String topoType, Link link, int index) {
        PathRouteObject latestPro = newPro.get(newPro.size() - 1);
        ResourceType resouce = latestPro.getResourceType();
        if (! (resouce instanceof Tp)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "the resource should be TP");
        }
        TpHop tpHop = ((Tp) resouce).getTpHop();
        String latestTpId = tpHop.getTpRef().getValue();
        String latestNodeId = PhysicalTpIdNamingRule.getNodeId(tpHop.getTpRef().getValue());

        if (!link.getSource().getSourceTp().getValue().equals(latestTpId) &&
            !link.getDestination().getDestTp().getValue().equals(latestTpId)) {
            if (link.getSource().getSourceNode().getValue().equals(latestNodeId)) {
                newPro.add(getTpPathRouteObject(new TopologyId(topoType), link.getSource().getSourceTp(), index++));
                newPro.add(getLinkPathRouteObject(new TopologyId(topoType), link.getLinkId(), index++));
                newPro.add(getTpPathRouteObject(new TopologyId(topoType), link.getDestination().getDestTp(), index++));
            } else {
                newPro.add(getTpPathRouteObject(new TopologyId(topoType), link.getDestination().getDestTp(), index++));
                newPro.add(getLinkPathRouteObject(new TopologyId(topoType), link.getLinkId(), index++));
                newPro.add(getTpPathRouteObject(new TopologyId(topoType), link.getSource().getSourceTp(), index++));
            }
        } else {
            if (link.getSource().getSourceNode().getValue().equals(latestNodeId)) {
                newPro.add(getLinkPathRouteObject(new TopologyId(topoType), link.getLinkId(), index++));
                newPro.add(getTpPathRouteObject(new TopologyId(topoType), link.getDestination().getDestTp(), index++));
            } else {
                newPro.add(getLinkPathRouteObject(new TopologyId(topoType), link.getLinkId(), index++));
                newPro.add(getTpPathRouteObject(new TopologyId(topoType), link.getSource().getSourceTp(), index++));
            }
        }
        return index;
    }


    private PathRouteObject getLinkPathRouteObject(TopologyId underLayerTopoId, LinkId linkId, long index) {
        PathRouteObjectBuilder proBuilder = new PathRouteObjectBuilder()
                .setIndex(index)
                .setTopologyRef(underLayerTopoId)
                .setResourceType( new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.LinkBuilder().setLinkHop(new LinkHopBuilder()
                        .setLinkRef(linkId)
                        .setTopologyRef(underLayerTopoId)
                        .build()).build());
        proBuilder.setKey(new PathRouteObjectKey(proBuilder.getIndex()));

        return proBuilder.build();
    }

    private PathRouteObject getTpPathRouteObject(TopologyId underLayerTopoId, TpId srcTpId, long index) {
        PathRouteObjectBuilder proBuilder = new PathRouteObjectBuilder()
                .setIndex(index)
                .setTopologyRef(underLayerTopoId)
                .setResourceType( new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.TpBuilder().setTpHop(new TpHopBuilder()
                        .setTpRef(srcTpId)
                        .setEquipmentRef(PhysicalNodeIdNamingRule.getEquipId(srcTpId.getValue()))
                        .setNodeRef(new NodeId(PhysicalNodeIdNamingRule.getPhyNodeId(srcTpId.getValue())))
                        .setSiteRef(new NodeId(PhysicalNodeIdNamingRule.getSiteId(srcTpId.getValue())))
                        .build()).build());
        proBuilder.setKey(new PathRouteObjectKey(proBuilder.getIndex()));

        return proBuilder.build();
    }


    private void insertEro(List<ExplicitRouteObjects> newEro, String siteTopoKey, Link changedSiteLink) {
    }

    /**
	 *
	 * @param input
	 * @return which siteLink will be operated
	 * @throws CommonException
	 */
	private Tunnel checkParam(SwitchSpcInput input) throws CommonException {
        if (input.getTunnelId() == null || input.getTunnelId().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "tunnelId is mandatory");
        }
        if (input.getSiteLinkRoute() == null || input.getSiteLinkRoute().getPrimary() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "new route is mandatory");
        }

        Tunnel tunnel = changedObject.getChangedTunnel(input.getTunnelId());
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required tunnel " + input.getTunnelId());
        }

        //tunnel的服务层（supportingLink）只可能有一条，所以直接get(0)
        oldOchLink = changedObject.getChangedOchLink(tunnel.getSupportingLink().get(0).getLinkRef().getValue());
        Och ochLinkAttr = oldOchLink.getAugmentation(Link1.class).getOch();
        if (ochLinkAttr.getProductType() != null && ochLinkAttr.getProtectionType().equals(SPC.class.getSimpleName())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the tunnel related och Link isn't SPC type");
        }
		return tunnel;
	}

    private void getOchParam(Link ochLink) throws CommonException {
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        long diff = ochLinkAttr.getUpperFrequency().getValue().longValue() - ochLinkAttr.getLowerFrequency().getValue().longValue();
        if ( diff == 75000) {
            ochGrid = GridType._75;
        } else if (diff == 50000) {
            ochGrid = GridType._50;
        } else if (diff == 100000) {
            ochGrid = GridType._100;
        } else {
            throw new CommonException(CommonExceptionType.MODULE_ERROR,
                    String.format("the ochLink's frequency scope has error %s, %s",
                            ochLinkAttr.getUpperFrequency().getValue().toString(),
                            ochLinkAttr.getLowerFrequency().getValue().toString()));
        }
        oldFrequency =  ochLinkAttr.getLowerFrequency().getValue().longValue() + diff / 2;
    }

    private long getStep(GridType gridType) {
        switch (gridType) {
            case _75:
                return 75000/2;
            case _50:
                return 50000/2;
            case _100:
                return 100000/2;
        }
        return 75000/2;
    }
}
