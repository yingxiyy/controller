/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network;

import static net.flex.dci.otn.controller.allocate.network.AdditionalAllocator.EXP8_SUFFIX;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.RouteYangDataConverter;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.link.common.ReuseResourceChecker;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkAdditionalInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkAdditionalOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkAdditionalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.create.network.additional.input.Additional;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.create.network.additional.input.ReusedNodesSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.PhyNeFullInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.SiteLinksSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.SiteLinksSnapshotBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.site.links.snapshot.SiteLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdditionalCreator {

    @Autowired
    private SiteLinkDao siteLinkDao;
    @Autowired
    private PhyLinkDao phyLinkDao;
    @Autowired
    private PhyLinkFriendlyName phyLinkFriendlyName;
    @Autowired
    private JsonOutputer jsonOutputer;
    @Autowired
    private AdditionalAllocator additionalAllocator;

    private final MultipleTransaction multipleTransaction;

    @Autowired
    public AdditionalCreator(MultipleTransaction multipleTransaction) {
        this.multipleTransaction = multipleTransaction;
    }

    public CreateNetworkAdditionalOutput doIt(CreateNetworkAdditionalInput input, TaskInfoMessage createTaskInfo) {
        Link siteLink = siteLinkDao.getSiteLinkById(input.getSiteLinkId());
        additionalAllocator.validateSiteLink(siteLink);

        ZkResourceLock locker = new ZkResourceLock();
        log.debug("Begin to add additional for SiteLink: {}", input.getSiteLinkId());
        createTaskInfo.setResourceId("Additional siteLink " + System.currentTimeMillis());
        createTaskInfo.setResourceName(siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite().getFriendlyName());
        createTaskInfo.setActionTime(System.currentTimeMillis());
        try {
            //Step 1: lockRE, checkSnapshot
            Additional additional = input.getAdditional();
            lockResource(locker, additional, siteLink);
            ChangedObject changedObject = new ChangedObject();
            checkSnapshot(input.getReusedNodesSnapshot(), siteLink, changedObject);

            //Step2: update nodes, 因为后续constructNewPhyLinks里updateFriendlyName为依赖node，所以需要放第一步
            for (Nodes nodeAdd : additional.getNodes()) {
                Node node = RouteYangDataConverter.getNode(nodeAdd);
                changedObject.addChangedPhyNode(node);
                log.debug("updatedPhyNode: {}", jsonOutputer.formatNode(node));
            }

            //Step3: add new PhyLink
            List<Link> newPhyLinks = constructNewPhyLinks(additional, siteLink, changedObject);
            for (Link newPhyLink : newPhyLinks) {
                changedObject.addChangedPhyLink(newPhyLink);
                log.debug("new PhyLink: {}", newPhyLink);
            }

            //Step4: update siteLink
            Link updatedSiteLink = constructSiteLinkUpdated(additional, siteLink);
            changedObject.addChangedSiteLink(updatedSiteLink);
            log.debug("updatedSiteLink: {}", updatedSiteLink);

            //Step5:Save to db
            multipleTransaction.save(changedObject);
            log.debug("All changed objects has been updated to db");

            createTaskInfo.setSuccessfully(true);
            createTaskInfo.setEndTime(System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Failed to update to db for CreateNetworkAdditional:{}", input.getSiteLinkId(), e);
            createTaskInfo.setSuccessfully(false);
            createTaskInfo.setErrorReason(e.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "failed to  CreateNetworkAdditional" + e.getCause().getMessage(), e);
        } finally {
            locker.unlock();
            TaskInfoMessager.sendMessage(createTaskInfo);
        }
        return new CreateNetworkAdditionalOutputBuilder().setReturnCode(
                RpcResultType.Success).build();
    }

    private List<Link> constructNewPhyLinks(Additional additional, Link siteLink, ChangedObject changedObject) {
        String siteLinkId = siteLink.getLinkId().getValue();
        Site siteLinkAddr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        List<String> orderId = siteLinkAddr.getOrderId();
        String riskGroupName = siteLinkAddr.getRiskGroupName();
        String planeName = siteLinkAddr.getPlaneName();

        List<Link> newPhyLinks = new ArrayList<>();
        List<Link> additionalLinks = RouteYangDataConverter.getLinks(additional.getLinks());

        for (Link phyLink : additionalLinks) {
            String phyLinkId = phyLink.getLinkId().getValue();
            if (phyLinkDao.isExistedPhyLinkId(phyLinkId)) {
                continue;
            }
            Node srcNode = changedObject.getChangedPhyNode(phyLink.getSource().getSourceNode().getValue());
            Node dstNode = changedObject.getChangedPhyNode(phyLink.getDestination().getDestNode().getValue());
            phyLink = phyLinkFriendlyName.updateFriendlyName(phyLink, srcNode, dstNode);

            if (PhysicalLinkIdNamingRule.isOmsLink(phyLinkId)) {
                List<SupportedLink> supportedLinks = new ArrayList<>();
                supportedLinks.add(new SupportedLinkBuilder()
                        .setLinkRef(new LinkId(siteLinkId))
                        .setTopologyRef(new TopologyId(Constant.SITE_TOPOID))
                        .setKey(new SupportedLinkKey(new LinkId(siteLinkId), new TopologyId(Constant.SITE_TOPOID)))
                        .build());
                phyLink = new LinkBuilder(phyLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(
                                                phyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical())
                                                .setSupportedLink(supportedLinks)
                                                .setPlaneName(planeName)
                                                .setOrderId(orderId)
                                                .setRiskGroupName(riskGroupName)
                                                .build())
                                        .build())
                        .build();
            }
            newPhyLinks.add(phyLink);
        }
        return newPhyLinks;
    }

    /**
     * 1,  bandwidth;
     *
     * 2,  SupporttingLink;
     *
     * 3,  a/z external
     *
     * 4,  ExplictRoute
     *
     * @param additional
     * @param siteLink
     * @return
     */
    private Link constructSiteLinkUpdated(Additional additional, Link siteLink) {
        Site siteAddr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();

        //1,  bandwidth
        int updatedBandwidth = Integer.parseInt(siteAddr.getBandwidth()) + 32;

        List<AddDropLink> aExtneralAddDrops = siteAddr.getAExternal().getAddDropLink();
        List<AddDropLink> zExtneralAddDrops = siteAddr.getZExternal().getAddDropLink();
        List<SupportingLink> newSupportingLinkList = new ArrayList(siteLink.getSupportingLink());
        for (Links addLink : additional.getLinks()) {
            //2,  SupportedLink;
            newSupportingLinkList.add(new SupportingLinkBuilder()
                    .setLinkRef(addLink.getLinkId())
                    .setKey(new SupportingLinkKey(addLink.getLinkId()))
                    .build());

            //3,  a/z external
            if (addLink.getPhysical().getLinkType().equals(LinkType.CableLink)) {
                continue;
            }
            String addLinkId = addLink.getLinkId().getValue();
            EquipType connectorType = addLinkId.contains(EXP8_SUFFIX) ? EquipType.IRA : EquipType.MUXPANEL;
            AddDropLink addDroplink = new AddDropLinkBuilder().setLinkRef(addLinkId)
                    .setKey(new AddDropLinkKey(addLinkId))
                    .setConnnectorType(connectorType)
                    .build();
            if (PhysicalLinkIdNamingRule.getSiteAId(addLinkId).equals(siteLink.getSource().getSourceNode().getValue())) {
                aExtneralAddDrops.add(addDroplink);
            } else {
                zExtneralAddDrops.add(addDroplink);
            }
        }

        //4,  ExplictRoute---only add xcs
        List<Route> routeList = siteAddr.getExplictRoute().getRoute();
        Primary primaryOld = routeList.get(0).getPrimary();
        List<CrossConnections> xcs = primaryOld.getCrossConnections();
        xcs.addAll(getRouteXC(additional.getCrossConnections(), xcs.size() + 1));
        Primary primary = new PrimaryBuilder(primaryOld).setCrossConnections(xcs).build();
        Route routeUpdated = new RouteBuilder(routeList.get(0)).setPrimary(primary).build();
        routeList.set(0, routeUpdated);
        ExplictRoute explictRoute = new ExplictRouteBuilder()
                .setRoute(routeList).build();

        return new LinkBuilder(siteLink)
                .setSupportingLink(newSupportingLinkList)
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                .setSite(new SiteBuilder(siteAddr)
                                        .setImplementState(siteAddr.getImplementState().equals(ImplementState.Allocate) ? ImplementState.Allocate : ImplementState.PartialImplement)
                                        .setZExternal(new ZExternalBuilder(siteAddr.getZExternal()).setAddDropLink(zExtneralAddDrops).build())
                                        .setAExternal(new AExternalBuilder(siteAddr.getAExternal()).setAddDropLink(aExtneralAddDrops).build())
                                        .setBandwidth(String.valueOf(updatedBandwidth))
                                        .setExplictRoute(explictRoute)
                                        .build()
                                ).build()
                ).build();
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> getRouteXC(
            @NonNull List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> xcs, long index) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> result = new ArrayList<>(xcs
                .size());
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections xc : xcs) {
            result.add(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(xc)
                    .setSequence(index++)
                    .build());
        }
        return result;
    }

    private void checkSnapshot(
            List<ReusedNodesSnapshot> reusedNodesSnapshot,
            Link siteLink, ChangedObject changedObject) {
        ReuseResourceChecker reuseChecker = new ReuseResourceChecker(changedObject);
        reuseChecker.checkInitialNodeEnv(covert(reusedNodesSnapshot));

        SiteLinksSnapshot siteLinksSnapshot = new SiteLinksSnapshotBuilder().setSiteLinks(
                Arrays.asList(new SiteLinksBuilder()
                        .setLinkId(siteLink.getLinkId().getValue())
                        .setSite(new SiteBuilder(
                                siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                                        .getSite()).build())
                        .build())).build();
        reuseChecker.checkInitialSiteLinkEnv(siteLinksSnapshot);
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshot> covert(List<ReusedNodesSnapshot> reusedNodesSnapshots) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshot> result = new ArrayList<>();

        for (ReusedNodesSnapshot node : reusedNodesSnapshots) {
            PhyNeFullInfo ne = (PhyNeFullInfo) node;
            result.add(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshotBuilder(ne).build());
        }
        return result;
    }

    private void lockResource(ZkResourceLock locker, Additional additional, Link siteLink) {
        //here is phyNode, should convert
        for (Nodes node : additional.getNodes()) {
            locker.addResource(PhysicalNodeIdNamingRule.getSiteId(node.getNodeId().getValue()));
        }
        locker.addResource(siteLink.getLinkId().getValue());
        locker.getLock();
    }
}
