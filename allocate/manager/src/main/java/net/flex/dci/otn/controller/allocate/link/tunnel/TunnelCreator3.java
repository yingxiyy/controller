/*
 *
 *  * Copyright (c) 2021-2020 Network Flex Any Comp. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.OtCardType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.*;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.enums.CustomServiceType;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.Utils;
import net.flex.dci.otn.controller.allocate.common.namingrule.OchLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.common.service.MyExecutor;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.link.common.ReuseResourceChecker;
import net.flex.dci.otn.controller.allocate.link.och.OchLinkConstructor;
import net.flex.dci.otn.controller.allocate.link.och.SiteLinkAvaliableRebuild;
import net.flex.dci.otn.controller.allocate.link.phy.AddDropLinkConstructor;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkCreator;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater;
import net.flex.dci.otn.controller.allocate.link.view.ViewLink;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeMerge;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.http.nokia.com.cd.otc.policies.rev190319.VendorInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.ProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.line.attributes.ModelSpec;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLine;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLineBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.Segment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.SegmentBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.ZExternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnelKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.och.links.snapshot.OchLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.site.links.snapshot.SiteLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.NewOchTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.ReuesedOchTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static net.flex.dci.otc.common.constants.Constants.OP_MODE;
import static net.flex.dci.otc.common.constants.Constants.SERVICE_TYPE;

@Slf4j
public class TunnelCreator3 {

    private final ChangedObject changedObject;
    //  private TaskInfoKafkaService kafka;

    private final PhyLinkDao phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);
    private final SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

    //==========================================
    protected TaskInfoMessage taskInfoMessage;
    //=========================================
    //parser from creation param, follow variable is key for create tunnel
    private List<Tunnel> createdTunnel;
    private Node srcSiteNode;
    private Node dstSiteNode;
    //==================key value for doIt tunnel
    private ParamCreate param;
    //===================

    public TunnelCreator3() {
        changedObject = new ChangedObject();
    }

    public CreateTunnel3Output doIt(CreateTunnel3Input input) throws CommonException {
        log.debug("start create tunnel");

        try {
            param = new ParamCreate();
            param.parser(input);

            checkingProtection(input);

            checkSnapshot(input.getTunnelAllocateResult2());
            //由于耗时, 把这个同步命令改为异步

            MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
            executor.lazyDo(new Runnable() {
                @Override
                public void run() {
                    lazy(input);
                }
            });

        } catch (CommonException ce) {
            log.error("Create tunnel error",ce);
            throw ce;
        } catch (Exception e) {
            log.error("Create tunnel error",e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "create tunnel error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }

        CreateTunnel3OutputBuilder ob = new CreateTunnel3OutputBuilder()
                .setTunnels(new ArrayList<>());
        return ob.build();
    }

    private void checkingProtection(CreateTunnel3Input input) {
        if (input.getTunnelAllocateResult2() == null || input.getTunnelAllocateResult2().getVendor() == null ||
                input.getTunnelAllocateResult2().getVendor().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "the Vendor related route info is mandatory");
        }

        Vendor vendor = input.getTunnelAllocateResult2().getVendor().get(0);
        if (param.getProtectionType().equals(ProtectionBidir1To2.class)) {
            if (vendor.getNewOchTunnel() != null) {
                SiteLinkRoute siteLinkRoute = vendor.getNewOchTunnel().getSiteLinkRoute();
//                if (siteLinkRoute.getThird() == null || siteLinkRoute.getThird().isEmpty() ||
//                        siteLinkRoute.getSecondary() == null || siteLinkRoute.getSecondary().isEmpty() ||
//                        siteLinkRoute.getPrimary().isEmpty()) {
//                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "1:3 protection, must include 3 site links");
//                }
                if (
                        siteLinkRoute.getSecondary() == null || siteLinkRoute.getSecondary().isEmpty() ||
                        siteLinkRoute.getPrimary().isEmpty()) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "1:3 protection, must include primary and secondary site links");
                }
            }
        } else if (param.getProtectionType().equals(ProtectionBidir1To1.class)) {
            if (vendor.getNewOchTunnel() != null) {
                SiteLinkRoute siteLinkRoute = vendor.getNewOchTunnel().getSiteLinkRoute();
                if (siteLinkRoute.getSecondary() == null || siteLinkRoute.getSecondary().isEmpty() ||
                        siteLinkRoute.getPrimary().isEmpty()) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "1:2 protection, must include 2 site links");
                }
                if (siteLinkRoute.getThird() != null && !siteLinkRoute.getThird().isEmpty()) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "1:2 protection, the third site link is NOT necessary");
                }
            }
        } else if (param.getProtectionType().equals(ProtectionUnprotected.class)) {
            if (vendor.getNewOchTunnel() != null) {
                SiteLinkRoute siteLinkRoute = vendor.getNewOchTunnel().getSiteLinkRoute();
                if (siteLinkRoute.getPrimary().isEmpty()) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "unprotection, must include 1 site link");
                }
                if ((siteLinkRoute.getSecondary() != null && !siteLinkRoute.getSecondary().isEmpty()) ||
                        (siteLinkRoute.getThird() != null && !siteLinkRoute.getThird().isEmpty())) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "unprotection, second, third site link is NOT required");
                }
            }
        }
    }

    private void checkSnapshot(TunnelAllocateResult2 allocateResult) {
        ReuseResourceChecker reuseChecker = new ReuseResourceChecker(changedObject);
        reuseChecker.checkInitialNodeEnv(allocateResult.getReusedNodesSnapshot());
        reuseChecker.checkInitialOchLinkEnv(allocateResult.getOchLinksSnapshot());
        reuseChecker.checkInitialSiteLinkEnv(allocateResult.getSiteLinksSnapshot());
    }

    //锁ochLink, siteLink, 和利旧的设备
    private void lcokerResource(ZkResourceLock locker, TunnelAllocateResult2 tunnelAllocateResult) {
        List<ReusedNodesSnapshot> reusedNodesSnapshotList = tunnelAllocateResult.getReusedNodesSnapshot();
        if (reusedNodesSnapshotList == null || reusedNodesSnapshotList.isEmpty()) {
            //do nothing
        } else {
            for (ReusedNodesSnapshot reuseNode : reusedNodesSnapshotList) {
                locker.addResource(reuseNode.getNodeId().getValue());
            }
        }

        OchLinksSnapshot ochLinksSnapshot = tunnelAllocateResult.getOchLinksSnapshot();
        if (ochLinksSnapshot == null || ochLinksSnapshot.getOchLinks() == null
                || ochLinksSnapshot.getOchLinks().isEmpty()) {
            //do nothing
        } else {
            for (OchLinks reusedOchLink : ochLinksSnapshot.getOchLinks()) {
                locker.addResource(reusedOchLink.getLinkId());
            }
        }

        SiteLinksSnapshot siteLinksSnapshot = tunnelAllocateResult.getSiteLinksSnapshot();
        if (siteLinksSnapshot == null || siteLinksSnapshot.getSiteLinks() == null
                || siteLinksSnapshot.getSiteLinks().isEmpty()) {
            //do nothing
        } else {
            for (SiteLinks reusedSiteLink : siteLinksSnapshot.getSiteLinks()) {
                locker.addResource(reusedSiteLink.getLinkId());
            }
        }

        //下面的这些ochLink在上面ochSnapshot中已经包含了
//    List<Vendor> vendorList = tunnelAllocateResult.getVendor();
//    if (vendorList == null || vendorList.isEmpty()) {
//      //do nothing;
//    } else {
//      for (Vendor vendor : vendorList) {
//        List<ReuesedOchTunnel> reusedOchList = vendor.getReuesedOchTunnel();
//        if (reusedOchList == null || reusedOchList.isEmpty()) {
//          //do nothing
//        } else {
//          for (ReuesedOchTunnel och : reusedOchList) {
//            locker.addResource(och.getLinkId());
//          }
//        }
//      }
//    }

        locker.getLock();
    }

    private void lazy(CreateTunnel3Input input) throws CommonException {
        log.debug("create tunnel start...");

        srcSiteNode = changedObject.getChangedSiteNode(input.getSrcSite().getValue());
        dstSiteNode = changedObject.getChangedSiteNode(input.getDstSite().getValue());

        String srcFriendlyName = srcSiteNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite().getFriendlyName();
        String dstFriendlyName = dstSiteNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite().getFriendlyName();

        createdTunnel = new LinkedList<>();
        ZkResourceLock locker = new ZkResourceLock();
        String msg = null;
        try {
            lcokerResource(locker, input.getTunnelAllocateResult2());

            checkSnapshot(input.getTunnelAllocateResult2());
            removePhyLinkBasedOnInput(input.getTunnelAllocateResult2().getRemovedResoureceIds());

            convert2NtNode(input.getTunnelAllocateResult2().getNodes());
            for (Vendor vendor : input.getTunnelAllocateResult2().getVendor()) {
                createTunnelByVendor(vendor);
            }

            Utils.store2DB(changedObject);
//            对于被影响的网元需要 adapter 再次同步， 等待确定接口
//            Set<String> changedNodeList = new HashSet<>();
//            for (Tunnel tunnel : createdTunnel) {
//                changedNodeList.addAll(net.flex.dci.otn.controller.allocate.link.common.Route
//                        .getNodeIdOverRoute(tunnel.getExplictRoute().getRoute()));
//            }
//            OpNodeMerger opMerger = SpringBeanFinder.getBean(OpNodeMerger.class);
//            for (String nodeId : changedNodeList) {
//                opMerger.merge(nodeId);
//            }

            updateTaskInfo(input.getTaskInfoId(), input.getUiInfo());
            log.debug("tunnel creation done.");
        } catch (CommonException ce) {
            msg = ExceptionUtils.getRootCauseMessage(ce);
            log.error("tunnel creation fail", ce);

            throw ce;
        } catch (Exception e) {
            log.error("create tunnel error.", e);
            msg = ExceptionUtils.getRootCauseMessage(e);

            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "create tunnel error", e);
        } finally {
            try {
                locker.unlock();
                log.debug("unlock successfully");
                logMessage(param.getBundleNumber(), srcFriendlyName, dstFriendlyName, createdTunnel, msg);
            } catch (Exception e) {
                log.error("unlock error", e);
            }
        }
    }

    public static void printXc(Collection<Node> nodes) {
        List<String> msg = new ArrayList<>();
        nodes.forEach(node->{
            msg.addAll(node.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                    .map(xc -> xc.getCrossConnectionId().getValue())
                    .collect(Collectors.toList()));
        });
//        log.debug("cross connection in creation params: \n\n{}\n\n", String.join("\n", msg));
    }

    private void printXc(List<Nodes> nodes) {
        List<String> msg = new ArrayList<>();
        nodes.forEach(node->{
            msg.addAll(node.getPhysical().getCrossConnections().stream()
                    .map(xc -> xc.getCrossConnectionId().getValue())
                    .collect(Collectors.toList()));
        });
        log.debug("cross connection in creation params: \n\n{}\n\n", String.join("\n", msg));
    }

    private void removePhyLinkBasedOnInput(List<String> removedResoureceIds) {
        if (removedResoureceIds == null || removedResoureceIds.isEmpty()) {
            //do nothing
        } else {
            PhyLinkUtil phyLinkUtil = new PhyLinkUtil(changedObject);
            for (String id : removedResoureceIds) {
                //these link is useless wssLink
                phyLinkUtil.removePhyLink(id, null);
            }
        }
    }

    private void updateTaskInfo(BigInteger taskInfoId, String uiInfo) {
        if (taskInfoId == null) {
            return;
        }
        log.info("update taskIno data {}", taskInfoId);

        TaskInfoMessage msg = new TaskInfoMessage(taskInfoMessage);
        msg.setId(taskInfoId.longValue());
        msg.setErrorReason("confirmed");
        msg.setSuccessfully(true);
        msg.setDetail(uiInfo);
        msg.setEndTime(System.currentTimeMillis());
        msg.setRoot(true);
        msg.setActionType(null);
        msg.setGroupId(taskInfoMessage.getGroupId());

        TaskInfoMessager.sendMessage(msg);
    }

    private void createTunnelByVendor(Vendor vendor) {
        createTunnelOverReusedOchLink(vendor, vendor.getReuesedOchTunnel());
        createTunnelOverBrandNewOchLink(vendor, vendor.getNewOchTunnel());
    }

    private void createTunnelOverReusedOchLink(VendorInfo vendorInfo,
                                               List<ReuesedOchTunnel> reuesedOchTunnelList) {
        if (reuesedOchTunnelList == null || reuesedOchTunnelList.isEmpty()) {
            return;
        }
        log.debug("create tunnel over reused ochLink");
        for (ReuesedOchTunnel reusedOch : reuesedOchTunnelList) {
            Link ochLink = changedObject.getChangedOchLink(reusedOch.getLinkId());
            createTunnelWithTpcRoute(reusedOch.getTpcRoute(), ochLink);
        }
    }

    private void createTunnelWithTpcRoute(List<TpcRoute> tpcRouteList, Link ochLink) {
        log.debug("start build tunnel data");
        for (TpcRoute route : tpcRouteList) {
            Node srcNode = changedObject.getChangedPhyNode(
                    PhysicalTpIdNamingRule.getNodeId(route.getSourceTp()));
            Node dstNode = changedObject.getChangedPhyNode(
                    PhysicalTpIdNamingRule.getNodeId(route.getDestTp()));

            List<Node> nodeList = new ArrayList<>();
            nodeList.add(srcNode);
            nodeList.add(dstNode);

            List<Link> linkList = new ArrayList<>();
            linkList.add(ochLink);
            RouteInfo rInfo = RouteInfo.builder()
                    .slave(null)
                    .third(null)
                    .main(Route.builder()
                            .nodes(nodeList)
                            .xcs(route.getCrossConnections())
                            .links(linkList)
                            .build())
                    .build();

            String[] tpIds = {route.getSourceTp(), route.getDestTp()};
            Tunnel tunnel = new TunnelConstructor(param).create(tpIds, rInfo, ochLink, srcNode,
                    dstNode);
            ochLink = updateOchLinkAvaliable(ochLink, rInfo.getMain().getXcs().get(0));
            ochLink = updateOchLinkSupportedTunnel(ochLink, tunnel);

            changedObject.addChangedOchLink(ochLink);
            changedObject.addChangedTunnel(tunnel);

            createdTunnel.add(tunnel);
        }
    }

    private void createTunnelOverBrandNewOchLink(VendorInfo vendorInfo, NewOchTunnel newOchTunnel) {
        if (newOchTunnel == null) {
            return;
        }

        log.debug("createTunnelOverBrandNewOchLink");

        //只要可以创建业务，这个site就是OTM
        updateSiteNodeType2OTM(srcSiteNode);
        updateSiteNodeType2OTM(dstSiteNode);

//        ModelSpec modelSpec = new ModelSpecBuilder()
//                .setOpMode(newOchTunnel.getOpMode())
//                .setServiceType(param.getServiceType())
//                .build();
//        OpNodeMerger opMerger = SpringBeanFinder.getBean(OpNodeMerger.class);
        int index = 0;
        for (TunnelRouteInfos tunnelRouteInfos : newOchTunnel.getTunnelRouteInfos()) {
//            OchRoute ochRoute = tunnelRouteInfos.getOchRoute();
            Link ochLink = createBrandNewOchLink(vendorInfo, tunnelRouteInfos.getOchRoute(),
                    newOchTunnel.getSiteLinkRoute(), index);
//            updateModelSpec(ochRoute, modelSpec);
            updateModeSpec(ochLink, newOchTunnel.getOpMode());
            new ViewLink(this.changedObject, param.getPlaneId()).create(ochLink);

//            List<String> changedNodeList = net.flex.dci.otn.controller.allocate.link.common.Route
//                    .getNodeIdOverRoute(
//                            ochLink.getAugmentation(Link1.class).getOch().getExplictRoute()
//                                    .getRoute());
//            for (String nodeId : changedNodeList) {
//                opMerger.merge(nodeId);
//            }

            log.debug("after create ochLink");
            printXc(changedObject.getChangedPhyNodeList().values());

            createTunnelWithTpcRoute(tunnelRouteInfos.getTpcRoute(), ochLink);

            log.debug("after create tunnel");
            printXc(changedObject.getChangedPhyNodeList().values());
            index++;
        }

    }

    private void updateModeSpec(Link ochLink, String opMode) {
        ochLink.getAugmentation(Link1.class).getOch().getProperties().getProperty().add(new PropertyBuilder().setName(SERVICE_TYPE)
                .setValue(CustomServiceType.valueOf(param.getServiceType().name()).getValue()).build());
        ochLink.getAugmentation(Link1.class).getOch().getProperties().getProperty().add(new PropertyBuilder().setName(OP_MODE)
                .setValue(opMode).build());
    }

    private void updateModelSpec(OchRoute ochRoute, ModelSpec modelSpec) {
        updateModelSpec(ochRoute.getSourceTp(), modelSpec);
        updateModelSpec(ochRoute.getDestTp(), modelSpec);
    }

    private void updateModelSpec(String lineTp, ModelSpec modelSpec) {
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(lineTp));

        List<TerminationPoint> updatedTpList = node.getTerminationPoint().stream()
                .map(tp -> {
                    if (tp.getTpId().getValue().equals(lineTp)) {
                        Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
                        TerminationPoint newTp = new TerminationPointBuilder(tp).addAugmentation(
                                TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new PhysicalBuilder(tpAttr)
                                                .setOtuLine(new OtuLineBuilder(tpAttr.getOtuLine())
                                                        .setModelSpec(modelSpec)
                                                        .build()
                                                ).build()
                                        ).build()
                        ).build();
                        return newTp;
                    } else {
                        return tp;
                    }
                }).collect(Collectors.toList());

        Node updatedNode = new NodeBuilder(node)
                .setTerminationPoint(updatedTpList)
                .build();

        changedObject.addChangedPhyNode(updatedNode);
    }

    /**
     * newOchTunnel 数据结构是一个数组，包含所有这个OCH link 下的tunnel 但是只有一组数据包含了OCH的路由信息，需要找到它，并基于它创建ochLInk
     *
     * @param vendorInfo
     * @param ochRoute
     * @param siteLinkRoute include siteLinkId and wssLinkId
     * @param index
     */
    private Link createBrandNewOchLink(VendorInfo vendorInfo, OchRoute ochRoute,
                                       SiteLinkRoute siteLinkRoute, int index) {
        log.debug("create a brand new och link");

        String aNodeId = PhysicalTpIdNamingRule.getNodeId(ochRoute.getSourceTp());
        String zNodeId = PhysicalTpIdNamingRule.getNodeId(ochRoute.getDestTp());
        Node srcElectricalNode = changedObject.getChangedPhyNode(aNodeId);
        Node dstElectricalNode = changedObject.getChangedPhyNode(zNodeId);
        String ochFriendlyName = OchLinkFriendlyName.buildFriendlyName(srcElectricalNode,
                dstElectricalNode, ochRoute.getSourceTp(), ochRoute.getDestTp());

        boolean overNetwork;


        if (siteLinkRoute == null) {
            overNetwork = false;
            //考虑到电中继， nodes, 不仅仅是src/dst, 中间的全部都需要
            //对应的osLink 需要加入viewLink
            List<Link> primaryLinkList = convert2NtLink(ochRoute.getPrimary().getLinks());
            updateViewLink(primaryLinkList);
        } else {
            overNetwork = updateForNetwork(siteLinkRoute);
        }

        Long centerFrequncy = siteLinkRoute.getCentralFrequencies().get(index);
        RouteInfo ochInfo = changeOchFrequency(ochRoute, siteLinkRoute, centerFrequncy);

        //这个地方特殊处理，在改频率的时候把OCH 的A/Z点 已经放到ochInfo.getMain.getNodes 中，需要剔除，放特殊位置
        ochInfo.getMain().getNodes().removeIf(node -> node.getNodeId().getValue().equals(aNodeId));
        ochInfo.getMain().getNodes().removeIf(node -> node.getNodeId().getValue().equals(zNodeId));

        ochInfo.getMain().getNodes().add(0, changedObject.getChangedPhyNode(aNodeId));
        ochInfo.getMain().getNodes().add(changedObject.getChangedPhyNode(zNodeId));

        OchLinkConstructor ochLinkConstructor = new OchLinkConstructor(changedObject, param);
        Link ochLink = ochLinkConstructor.create(vendorInfo.getVendorName(),
                vendorInfo.getProductType(),
                ochRoute.getSourceTp(),
                ochRoute.getDestTp(),
                ochFriendlyName,
                overNetwork,
                ochInfo);   //frequency info can be extract from och XC.
        log.debug("create new ochLink {} at {}, related centerFreq is {}", ochFriendlyName,
                ochLink.getAugmentation(Link1.class).getOch().getLowerFrequency().getValue()
                        .toString(), centerFrequncy);

        // OCH supporting-link is the final source of truth. constructOchUnderLayerLink() may add
        // extra siteLinks for OS/OMS/WSS links, so siteLinkRoute alone is not enough here.
        if (siteLinkRoute != null) {
            updateSiteLink(ochLink, collectSiteLinkIdsFromSupportingLinks(ochLink.getSupportingLink()));

            List<String> allSiteLinkIds = new ArrayList<>(siteLinkRoute.getPrimary());
            if ( null != siteLinkRoute.getSecondary()) {
                allSiteLinkIds.addAll(siteLinkRoute.getSecondary());
            }
            if (null != siteLinkRoute.getThird()) {
                allSiteLinkIds.addAll(siteLinkRoute.getThird());
            }
            checkingNewPhyLink(ochLink, allSiteLinkIds);
        }

        //find out all TD node and insert into rack
        insertTdNode2Rack(ochInfo, siteLinkRoute);

        return ochLink;
    }

    private void insertTdNode2Rack(RouteInfo ochInfo, SiteLinkRoute siteLinkRoute) {
        List<Node> allNodes = new ArrayList<>(ochInfo.getMain().getNodes());
        if (ochInfo.getSlave() != null) {
            allNodes.addAll(ochInfo.getSlave().getNodes());
        }
        if (ochInfo.getThird() != null) {
            allNodes.addAll(ochInfo.getThird().getNodes());
        }
        List<Node> tdNodes = allNodes.stream()
                .filter(x -> x.getAugmentation(Node1.class).getPhysical().getNodeType().equals(NodeType.TD))
                .collect(Collectors.toList());

        List<String> allSiteLinks;
        if (siteLinkRoute == null) {
            allSiteLinks = null;
        } else {
            allSiteLinks = new ArrayList<>(siteLinkRoute.getPrimary());

            if (siteLinkRoute.getSecondary() != null) {
                allSiteLinks.addAll(siteLinkRoute.getSecondary());
            }
            if (siteLinkRoute.getThird() != null) {
                allSiteLinks.addAll(siteLinkRoute.getThird());
            }
        }

        tdNodes.forEach(node -> {
            String nodeId = node.getNodeId().getValue();
            updateNodeRelation(nodeId, allSiteLinks);
        });
    }

    //在ROADM--转OTM/REG的时候会加入新的MUX板卡，和与光放相关的连线，这些需要添加到数据库
    //基于ochLink中出现的新OMS Link， 继续查找网元上(这个OMS link 应该是同一个网元)是否有其他omsLink 需要同时添加 (findoutAllNewOmsLinks)
    private void checkingNewPhyLink(Link ochLink, List<String> allSiteLinkIds) {
        ochLink.getSupportingLink().forEach(sl -> {
            String phyLinkId = sl.getLinkRef().getValue();
            if (PhysicalLinkIdNamingRule.isOmsLink(phyLinkId)) {
                if (!phyLinkDao.isExistedPhyLinkId(phyLinkId)) {
                    String nodeId = PhysicalLinkIdNamingRule.getNodeAId(phyLinkId);
                    List<String> newLinks = findoutAllNewOmsLinks(nodeId); //based on this node

                    log.info("find new OMS link in och route {}", newLinks);
                    String siteLinkId = allSiteLinkIds.stream().filter(linkId -> linkId.contains(nodeId)).findAny().orElse(null);
                    if (siteLinkId == null) {
                        log.error("find a new phyLink but cannot find out related sitelink with it {}", phyLinkId);
                        return;
                    }

                    log.debug("find new phyLink {} related siteLink {}", phyLinkId, siteLinkId);

                    List<AddDropLink> extLinks = new ArrayList<>();
                    List<Link> newPhyLinks = buildPhysicalLink(ochLink, newLinks, siteLinkId);
                    if (!newPhyLinks.isEmpty()) {
                        Node node = changedObject.getChangedPhyNode(nodeId);

                        AddDropLinkConstructor addDropLinkConstructor = new AddDropLinkConstructor();
                        extLinks = addDropLinkConstructor.getLink2WSS(node, newPhyLinks);
                        extLinks.addAll(addDropLinkConstructor.getLinkBetweenMuxPanelCmux(node, newPhyLinks));
                    }

                    String aNode = SiteLinkIdNamingRule.getNodeA(siteLinkId);

                    Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkAttr =
                            siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
                    List<SupportingLink> supportingLinks = new ArrayList<>(siteLink.getSupportingLink());
                    supportingLinks.addAll(newPhyLinks.stream()
                            .map(x -> new SupportingLinkBuilder()
                                    .setLinkRef(new LinkId(x.getLinkId().getValue()))
                                    .setKey(new SupportingLinkKey(new LinkId(x.getLinkId().getValue())))
                                    .build()).collect(Collectors.toList()));

                    Link newSiteLink;
                    if (aNode.equals(nodeId)) {
                        List<AddDropLink> existed = siteLinkAttr.getAExternal().getAddDropLink();
                        existed.addAll(extLinks);
                        newSiteLink = new LinkBuilder(siteLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                                .setSite(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder(siteLinkAttr)
                                                        .setAExternal(new AExternalBuilder().setAddDropLink(existed).build())
                                                        .build())
                                                .build())
                                .setSupportingLink(supportingLinks)
                                .build();
                    } else {
                        List<AddDropLink> existed = siteLinkAttr.getZExternal().getAddDropLink();
                        existed.addAll(extLinks);
                        newSiteLink = new LinkBuilder(siteLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                                .setSite(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder(siteLinkAttr)
                                                        .setZExternal(new ZExternalBuilder().setAddDropLink(existed).build())
                                                        .build())
                                                .build())
                                .setSupportingLink(supportingLinks)
                                .build();
                    }
                    changedObject.addChangedSiteLink(newSiteLink);
                }
            }
        });

    }


    /**
     * this is specail for siteLink A/Z external link
     *
     * @param nodeId
     * @return
     */
    private List<String> findoutAllNewOmsLinks(String nodeId) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nodeAttr = node.getAugmentation(Node1.class)
                .getPhysical();

        List<String> newLinks = new ArrayList<>();
        nodeAttr.getInternalLinks().forEach(il -> {
            String linkId = il.getLinkRef();
            if (PhysicalLinkIdNamingRule.isOmsLink(linkId) && !phyLinkDao.isExistedPhyLinkId(linkId)) {
                newLinks.add(linkId);
            }
        });

        return newLinks;
    }

    //the omsLink is NE internalLink, thus A/Z is same one.
    //based on it create new phylink
    private List<Link> buildPhysicalLink(Link ochLink, List<String> omsLinkIds, String siteLinkId) {
        String firstOne = omsLinkIds.get(0);
        String nodeId = PhysicalLinkIdNamingRule.getNodeAId(firstOne);

        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site siteLinkAttr =
                siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();

        List<SupportedLink> supportedLinks = new ArrayList<>();
        supportedLinks.add(new SupportedLinkBuilder()
                .setLinkRef(new LinkId(siteLinkId))
                .setTopologyRef(new TopologyId(Constant.SITE_TOPOID))
                .setKey(new SupportedLinkKey(new LinkId(siteLinkId), new TopologyId(Constant.SITE_TOPOID)))
                .build());

        Node node = changedObject.getChangedPhyNode(nodeId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nodeAttr = node.getAugmentation(Node1.class)
                .getPhysical();

        InternalLinks il = nodeAttr.getInternalLinks().stream().filter(x -> x.getLinkRef().equals(firstOne)).findAny().orElse(null);

        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        List<Link> newPhyLinks = new ArrayList<>();
        omsLinkIds.forEach(omsLinkId -> {
            String tpA = PhysicalLinkIdNamingRule.getTpAId(omsLinkId);
            String tpZ = PhysicalLinkIdNamingRule.getTpZId(omsLinkId);
            TerminationPoint aTP = node.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().equals(tpA)).findAny().orElse(null);
            TerminationPoint zTP = node.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().equals(tpZ)).findAny().orElse(null);
            String friendNameDisplay = String.format("%s#%s---%s)", nodeAttr.getFriendlyName(),
                    aTP.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName(),
                    zTP.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName());


            Link newLink = new LinkBuilder()
                    .setLinkId(new LinkId(omsLinkId))
                    .setKey(new LinkKey(new LinkId(omsLinkId)))
                    .setSource(new SourceBuilder()
                            .setSourceNode(node.getNodeId())
                            .setSourceTp(new TpId(tpA))
                            .build())
                    .setDestination(new DestinationBuilder()
                            .setDestNode(node.getNodeId())
                            .setDestTp(new TpId(tpZ))
                            .build())
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                    .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder()
                                            .setSupportedLink(supportedLinks)
                                            .setPlaneName(siteLinkAttr.getPlaneName())
                                            .setPlaneId(siteLinkAttr.getPlaneId())
                                            .setOrderId(nodeAttr.getOrderId())
                                            .setRiskGroupName(siteLinkAttr.getRiskGroupName())
                                            .setDirection(il.getDirection())
                                            .setLinkType(il.getLinkType())
                                            .setFriendlyNameDisplay(friendNameDisplay)
                                            .setFriendlyName(friendNameDisplay)
                                            .setCreationTime(ochLinkAttr.getCreationTime())
                                            .setAlignmentStatus(AlignmentStatusType.Unknown)
                                            .setOperationalState(OperStatus.Unknown)
                                            .setAdminState(AdminStatus.Unknown)
                                            .setAlarmState(AlarmSeverity.Unknown)
                                            .setImplementState(ImplementState.Allocate)
                                            .build())
                                    .build())
                    .build();

            changedObject.addChangedPhyLink(newLink);
            newPhyLinks.add(newLink);
        });

        return newPhyLinks;
    }

    private List<AddDropLink> mergeAddDropLinks(List<AddDropLink> existing, List<AddDropLink> added) {
        LinkedHashMap<String, AddDropLink> byLinkRef = new LinkedHashMap<>();
        if (existing != null) {
            for (AddDropLink link : existing) {
                if (link != null && link.getLinkRef() != null) {
                    byLinkRef.put(link.getLinkRef(), link);
                }
            }
        }
        if (added != null) {
            for (AddDropLink link : added) {
                if (link != null && link.getLinkRef() != null) {
                    byLinkRef.put(link.getLinkRef(), link);
                }
            }
        }
        return new ArrayList<>(byLinkRef.values());
    }

    /**
     * 创建输入信息中可能包含最新的频率信息，与最开始计算的频点不一样，
     * 这个地方把所有涉及修改的频点都修改，并且入库
     *
     * @param ochRoute
     * @param siteLinkRoute
     * @param centerFrequncy
     * @return
     */
    private RouteInfo changeOchFrequency(OchRoute ochRoute, SiteLinkRoute siteLinkRoute, Long centerFrequncy) {
        //new function from Bytedance, user can change frequency at design step.
        Route mainRoute = getRoute(ochRoute.getPrimary(), siteLinkRoute.getPrimary(), centerFrequncy);

        Route secondaryRoute = getRoute(ochRoute.getSecondary(), siteLinkRoute.getSecondary(), centerFrequncy);
        Route thirdRoute = getRoute(ochRoute.getThird(), siteLinkRoute.getThird(), centerFrequncy);

        RouteInfo ochInfo = RouteInfo.builder()
                .main(mainRoute)
                .slave(secondaryRoute)
                .third(thirdRoute)
                .build();

//        changeLPortFrequence(ochRoute.getSourceTp(), centerFrequncy);
//        changeLPortFrequence(ochRoute.getDestTp(), centerFrequncy);

        log.debug("changeFrequency if required, and save all xc into node");
        List<CrossConnections> allXcs = new ArrayList<>();
        allXcs.addAll(mainRoute.getXcs());
        allXcs.addAll(secondaryRoute == null ? new ArrayList<>() : secondaryRoute.getXcs());
        allXcs.addAll(thirdRoute == null ? new ArrayList<>() : thirdRoute.getXcs());
        insertOchXc2Node(allXcs);

        return ochInfo;
    }

    private Node changeLPortFrequence(String tpId, Long centerFrequncy) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node electricalNode = changedObject.getChangedPhyNode(nodeId);
        TerminationPoint tmpTp = electricalNode.getTerminationPoint().stream()
                .filter(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine() != null && tp.getTpId().getValue().equals(tpId))
                .findAny().orElse(null);

        if (tmpTp == null) {
            //不是电卡的L口
            return null;
        }

        List<TerminationPoint> newTpList = electricalNode.getTerminationPoint().stream().map(tp -> {
            if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine() != null && tp.getTpId().getValue().equals(tpId)) {
                OtuLine newOtuLine = new OtuLineBuilder(tp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine())
                        .setCentralFrequency(new FrequencyType(BigInteger.valueOf(centerFrequncy)))
                        .build();
                TerminationPoint newTp = new TerminationPointBuilder(tp)
                        .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                .setPhysical(new PhysicalBuilder(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                                        .setOtuLine(newOtuLine)
                                        .build())
                                .build())
                        .build();
                return newTp;
            } else {
                return tp;
            }
        }).collect(Collectors.toList());

        Node newNode = new NodeBuilder(electricalNode).setTerminationPoint(newTpList).build();
        changedObject.addChangedPhyNode(newNode);

        return newNode;
    }

    private List<CrossConnectionAttributes> fetchAmplifierXc(String siteLinkId) {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        net.flex.dci.otn.controller.allocate.link.site.SiteLinkRoute route = new net.flex.dci.otn.controller.allocate.link.site.SiteLinkRoute(
                siteLink);

        return route.getAllXCs();
    }

    private Route getRoute(TunnelRoute2 ochRoute, List<String> siteLinkRoute, Long centerFrequncy) {
        if (ochRoute == null) {
            return null;
        }

        long span = getSpan(param.getFrequencyWidthOfLinePort());
        long lower = centerFrequncy - span;
        long upper = centerFrequncy + span;

        List<Node> nodes = getAllPhyNodesOverSiteLinkRoute(siteLinkRoute);

        //och 路由信息还需电层网元的信息
        List<Link> links = constructOchUnderLayerLink(ochRoute.getLinks(), siteLinkRoute);

        //基于link 检查是否有电中继的情况， 有的话， 对应L口的频率也需要修改
        List<String> regTps = fetchElectricTps(nodes, links);

        List<Node> newNodes = new ArrayList<>();
        regTps.forEach(regTp -> {
            Node tmp = changeLPortFrequence(regTp, centerFrequncy);
            if (tmp != null)
                newNodes.add(tmp);
        });

        List<CrossConnections> newXCs = new ArrayList<>();

        for (CrossConnections xc : ochRoute.getCrossConnections()) {
            try {
                Optional<Node> nodeOp = nodes.stream().filter(node -> node.getNodeId().getValue()
                        .equals(xc.getNodeRef().getValue())).findAny();
                if (nodeOp.isPresent()) {
                    CrossConnections newXC = CrossConnectionSlotNamingRule.setFrequencyScope(xc,
                            lower, upper, centerFrequncy);
                    newXCs.add(newXC);
                    Node newNode = changeXC(nodeOp.get(), xc, newXC);
                    newNodes.add(newNode);
                } else {
                    newXCs.add(xc); // this should be protection APS XC
                }
            } catch (Exception e) {
                return null;
            }
        }

        return Route.builder()
                .nodes(newNodes)
                .xcs(newXCs)
                .links(links)
                .build();
    }

    //算法思路是： links 中的点都必然出现在nodes中， 如果没有，那么就是电中继网元。
    //因为nodes 是基于复用段推导出来的
    private List<String> fetchElectricTps(List<Node> nodes, List<Link> links) {
        Set<String> regTps = new HashSet<>();

        links.forEach(link -> {
            String linkId = link.getLinkId().getValue();
            String aNodeId = PhysicalLinkIdNamingRule.getNodeAId(linkId);
            String zNodeId = PhysicalLinkIdNamingRule.getNodeZId(linkId);

            if (nodes.stream().noneMatch(node -> node.getNodeId().getValue().equals(aNodeId))) {
                String aTpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
                regTps.add(aTpId);
            }
            if (nodes.stream().noneMatch(node -> node.getNodeId().getValue().equals(zNodeId))) {
                String zTpId = PhysicalLinkIdNamingRule.getTpZId(linkId);
                regTps.add(zTpId);
            }
        });

        return new ArrayList<>(regTps);
    }

    private Node changeXC(Node node, CrossConnections oldXC, CrossConnections newXC) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        List<CrossConnections> oldXCs = nodeAttr.getCrossConnections();
        oldXCs.removeIf(xc -> xc.getCrossConnectionId().getValue()
                .equals(oldXC.getCrossConnectionId().getValue()));
        List<CrossConnections> newXCs = new ArrayList<>(oldXCs);
        newXCs.add(newXC);
        return new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                                nodeAttr)
                                .setCrossConnections(newXCs)
                                .build())
                .build())
                .build();
    }

    private long getSpan(GridType bandWidthOfLinePort) {
        switch (bandWidthOfLinePort) {
            case _50:
            case _75:
            case _100:
            case _150:
                return bandWidthOfLinePort.getIntValue() * 1000L / 2;
        }
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "unsupported bandwidth of Line port " + bandWidthOfLinePort);
    }

    private void updateViewLink(List<Link> linksList) {
        if (linksList != null) {
            ViewLink viewLinkMgr = new ViewLink(this.changedObject, param.getPlaneId());
            for (Link link : linksList) {
                viewLinkMgr.create(link);
            }
        }
    }

    private List<Node> getNode(List<Links> linkList) {
        Set<String> nodeIdList = new HashSet<>();
        for (Links link : linkList) {
            nodeIdList.add(link.getSource().getSourceNode().getValue());
            nodeIdList.add(link.getDestination().getDestNode().getValue());
        }
        List<Node> nodeList = new ArrayList<>();
        for (String nodeId : nodeIdList) {
            nodeList.add(changedObject.getChangedPhyNode(nodeId));
        }
        return nodeList;
    }

    private void insertOchXc2Node(List<CrossConnections> allXcs) {
        for (CrossConnections xc : allXcs) {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xc.getCrossConnectionId().getValue());
            Node node = changedObject.getChangedPhyNode(nodeId);
            List<CrossConnections> nodeXcList = node.getAugmentation(Node1.class).getPhysical()
                    .getCrossConnections();
            nodeXcList.add(xc);
            changedObject.addChangedPhyNode(new NodeBuilder(node)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                                            node.getAugmentation(Node1.class).getPhysical())
                                            .setCrossConnections(nodeXcList)
                                            .build())
                            .build())
                    .build());
        }
    }

    /**
     * 基于och link的起止点，找到对应的siteNode，然后设置它的siteType=OTM
     *
     * @param siteLinkRoute
     * @return 如何基层siteLink隶属于network, 返回true
     */
    private boolean updateForNetwork(SiteLinkRoute siteLinkRoute) {
        boolean overRoadm = false;
        overRoadm = isOverRoadm(siteLinkRoute.getPrimary());
        if (!overRoadm && siteLinkRoute.getSecondary() != null) {
            overRoadm = isOverRoadm(siteLinkRoute.getSecondary());
        }
        if (!overRoadm && siteLinkRoute.getThird() != null) {
            overRoadm = isOverRoadm(siteLinkRoute.getThird());
        }
        return overRoadm;
    }

    private boolean isOverRoadm(List<String> linkIds) {
        for (String linkId : linkIds) {
            if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                Link siteLink = changedObject.getChangedSiteLink(linkId);
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site linkAttr =
                        siteLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                                .getSite();
                if (linkAttr.getNetworkId() != null && !linkAttr.getNetworkId().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateProperties(List<Property> pros, String propName, String propValue) {
        Iterator<Property> iter = pros.iterator();
        while (iter.hasNext()) {
            Property pro = iter.next();
            if (pro.getName().equals(propName)) {
                iter.remove();
                break;
            }
        }
        Property pro = new PropertyBuilder()
                .setName(propName)
                .setValue(propValue)
                .build();
        pros.add(pro);
    }

    private void updateSiteNodeType2OTM(Node siteNode) {
        Site siteAttr = siteNode.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite();
        if (siteAttr.getSiteType().equals(SiteType.OTM)) {
            //do nothing.
        } else {
            siteAttr = new SiteBuilder(siteAttr).setSiteType(SiteType.OTM).build();
            Node updated = new NodeBuilder(siteNode)
                    .addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder()
                                    .setSite(siteAttr).build())
                    .build();
            changedObject.addChangedSiteNode(updated);
        }
    }

    private List<Node> getAllPhyNodesOverSiteLinkRoute(List<String> siteRouteList) {
        List<Node> rst = new ArrayList<>();
        if (siteRouteList == null) {
            return new ArrayList<>();
        }

        Set<Node> nodeList = new HashSet<>();
        for (String linkId : siteRouteList) {
            net.flex.dci.otc.common.util.RouteInfo rInfo = new net.flex.dci.otc.common.util.RouteInfo();
            Link siteLink = changedObject.getChangedSiteLink(linkId);

            rInfo.parse(siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite().getExplictRoute().getRoute());

            for (String nodeId : rInfo.getNodeIdList()) {
                nodeList.add(changedObject.getChangedPhyNode(nodeId));
            }
        }

        rst.addAll(nodeList);
        return rst;
    }

    private List<Link> constructOchUnderLayerLink(List<Links> osLinks,
                                                  List<String> siteLinkRoute) {
        List<Link> underLayerLinks = new ArrayList<>();
        if (osLinks == null || osLinks.isEmpty()) {
            return underLayerLinks;
        }
//
//        List<Link> osLinkList = convert2NtLink(osLinks);
//
//        List<Link> opticalLinkList = new ArrayList<>();
//        for (String linkId : opticalLayerLinkList) {
//            if (SiteLinkIdNamingRule.isSiteLink((linkId))) {
//                opticalLinkList.add(changedObject.getChangedSiteLink(linkId));
//            } else {
//                //wss-link
//                opticalLinkList.add(changedObject.getChangedPhyLink(linkId));
//            }
//        }
//        underLayerLinks.addAll(osLinkList);

        Set<String> hasAddedOpticalLinkIds = new HashSet<>();
        for (Links osLink : osLinks) {
            if (osLink.getPhysical().getLinkType().equals(LinkType.OsLink)) {
                underLayerLinks.add(convert2NtLink(osLink));
                appendSiteLinkForOsLink(osLink, siteLinkRoute, underLayerLinks, hasAddedOpticalLinkIds);
            } else if (osLink.getPhysical().getLinkType().equals(LinkType.OmsLink)) {
                List<SupportedLink> supportedLinkList = osLink.getPhysical().getSupportedLink();
                String siteLinkId;
                if (supportedLinkList == null || supportedLinkList.isEmpty()) {

                    //add sitelink as supported link to OS link
                    siteLinkId = getSiteLinkIdForOsLink(osLink, siteLinkRoute);
                    underLayerLinks.add(convert2NtLink(osLink, siteLinkId));


                    //add osLink as supporting link to siteLink, and update changedObject
                    Link osLinkSupportedSiteLink = changedObject.getChangedSiteLink(siteLinkId);

                    List<SupportingLink> supportingLinkList = new ArrayList<>(osLinkSupportedSiteLink
                            .getSupportingLink());
                    supportingLinkList.add(new SupportingLinkBuilder()
                            .setLinkRef(osLink.getLinkId())
                            .setKey(new SupportingLinkKey(osLink.getLinkId()))
                            .build());

                    Link newSiteLink = new LinkBuilder(osLinkSupportedSiteLink)
                            .setSupportingLink(supportingLinkList)
                            .build();
                    changedObject.addChangedSiteLink(newSiteLink);
                    log.debug("New supporting link added to siteLink, as {}", newSiteLink);

                } else {
                    siteLinkId = supportedLinkList.get(0).getLinkRef().getValue();
                    underLayerLinks.add(convert2NtLink(osLink));
                }

                //add siteLink
                if (hasAddedOpticalLinkIds.contains(siteLinkId)) {
                    //do nothing;
                } else {
                    underLayerLinks.add(changedObject.getChangedSiteLink(siteLinkId));
                    hasAddedOpticalLinkIds.add(siteLinkId);
                }


            } else if (osLink.getPhysical().getLinkType().equals(LinkType.WssLink)) {
                underLayerLinks.add(convert2NtLink(osLink));

                List<SupportedLink> supportedLinkList = osLink.getPhysical().getSupportedLink();
                if (supportedLinkList != null && !supportedLinkList.isEmpty()) {
                    String siteLinkId = supportedLinkList.get(0).getLinkRef().getValue();
                    if (hasAddedOpticalLinkIds.contains(siteLinkId)) {
                        siteLinkId = supportedLinkList.get(1).getLinkRef().getValue();
                    }
                    underLayerLinks.add(changedObject.getChangedSiteLink(siteLinkId));
                    hasAddedOpticalLinkIds.add(siteLinkId);
                }
            }
        }
//        underLayerLinks.addAll(osLinkList.size() / 2, opticalLinkList);  //把光层的线查到电层中间
        return underLayerLinks;
    }

    /**
     * 基于OS link， 检查某个端点是否是MUX，如果是，基于这个MUX找复用段siteLink，加入到underLayerLinks中
     * @param osLink
     * @param siteLinkRoute
     * @param underLayerLinks
     * @param hasAddedOpticalLinkIds
     */
    private void appendSiteLinkForOsLink(Links osLink, List<String> siteLinkRoute, List<Link> underLayerLinks, Set<String> hasAddedOpticalLinkIds) {
        String osLinkId = osLink.getLinkId().getValue();
        String aTp = PhysicalLinkIdNamingRule.getTpAId(osLinkId);
        String zTp = PhysicalLinkIdNamingRule.getTpZId(osLinkId);
        String aNodeId = PhysicalTpIdNamingRule.getNodeId(aTp);
        String zNodeId = PhysicalTpIdNamingRule.getNodeId(zTp);
        String aEqId = PhysicalTpIdNamingRule.getEquipId(aTp);
        String zEqId = PhysicalTpIdNamingRule.getEquipId(zTp);

        boolean mpoExisted = getMuxWithoutMPO(aNodeId, aEqId);
        if (mpoExisted) {
            return;
        }
        mpoExisted = getMuxWithoutMPO(zNodeId, zEqId);
        if (mpoExisted) {
            return;
        }

        List<String> siteLinkIds = siteLinkDao.getSiteLinkIdWhichContainsKeyInId(aEqId);
        if (siteLinkIds.isEmpty()) {
            return;
        }
        String siteLinkId = siteLinkIds.get(0);
        log.debug("find siteLink with mux {}", siteLinkIds);
        //add siteLink
        if (hasAddedOpticalLinkIds.contains(siteLinkId)) {
            //do nothing;
        } else {
            underLayerLinks.add(changedObject.getChangedSiteLink(siteLinkId));
            hasAddedOpticalLinkIds.add(siteLinkId);
        }
    }

    //only export without MPO ports
    private boolean getMuxWithoutMPO(String nodeId, String eqId) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        if  (node == null) {
            //the node hasn't created. this is acceptable
            return false;
        } else {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            Equipments eq = nodeAttr.getEquipments().stream().filter(x -> x.getEquipmentId().equals(eqId))
                    .findAny().orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "Failed to get equipment " + eqId + " from node " + nodeId));

            if (!eq.getEquipType().toString().contains("MUX"))
                return false;
            else {
                log.debug("the eqId {} type is {}", eqId, eq.getEquipType());
            }

            //continue check mpo port
            return node.getTerminationPoint().stream().anyMatch(tp->
                tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPCMPO) ||
                        tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPCMPOLAG)
            );
        }
    }

    private String getSiteLinkIdForOsLink(Links osLink, List<String> siteLinkRoute) {
        String nodeId = osLink.getSource().getSourceNode().getValue();
        Optional<String> optionalSiteLink = siteLinkRoute.stream()
                .filter(siteLinkId -> siteLinkId.contains(nodeId))
                .findAny();
        if (optionalSiteLink.isPresent()) {
            return optionalSiteLink.get();
        }

        log.error("Failed to get supported siteLink for {}, during siteLinkRoute:{}", osLink.getLinkId().getValue(), siteLinkRoute);

        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Failed to get supported siteLink for " + osLink.getLinkId().getValue());
    }


    private List<Link> convert2NtLink(List<Links> allocateLinkDataList) {
        List<Link> ntLinkList = new ArrayList<>();
        if (allocateLinkDataList != null) {
            for (Links baseLink : allocateLinkDataList) {
                Link ntLink = new LinkBuilder()
                        .setLinkId(baseLink.getLinkId())
                        .setKey(new LinkKey(baseLink.getLinkId()))
                        .setSource(baseLink.getSource())
                        .setDestination(baseLink.getDestination())
                        .addAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                        .setPhysical(baseLink.getPhysical())
                                        .build())
                        .build();

                ntLinkList.add(ntLink);
            }
        }

        return ntLinkList;
    }

    private Link convert2NtLink(Links allocatedLinkData) {
        Link ntLink = new LinkBuilder()
                .setLinkId(allocatedLinkData.getLinkId())
                .setKey(new LinkKey(allocatedLinkData.getLinkId()))
                .setSource(allocatedLinkData.getSource())
                .setDestination(allocatedLinkData.getDestination())
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                .setPhysical(allocatedLinkData.getPhysical())
                                .build())
                .build();

        return ntLink;
    }

    private Link convert2NtLink(Links allocatedLinkData, String siteLinkId) {

        List<SupportedLink> supportedLinks = allocatedLinkData.getPhysical().getSupportedLink();
        if (supportedLinks == null) {
            supportedLinks = new ArrayList<>();
        }
        supportedLinks.add(new SupportedLinkBuilder()
                .setLinkRef(new LinkId(siteLinkId))
                .setTopologyRef(new TopologyId(Constant.SITE_TOPOID))
                .setKey(new SupportedLinkKey(new LinkId(siteLinkId), new TopologyId(Constant.SITE_TOPOID)))
                .build());
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical physical = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(allocatedLinkData
                .getPhysical()).setSupportedLink(supportedLinks).build();

        Link ntLink = new LinkBuilder()
                .setLinkId(allocatedLinkData.getLinkId())
                .setKey(new LinkKey(allocatedLinkData.getLinkId()))
                .setSource(allocatedLinkData.getSource())
                .setDestination(allocatedLinkData.getDestination())
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                .setPhysical(physical)
                                .build())
                .build();

        log.debug("convert2NtLink as :{}", ntLink);

        return ntLink;
    }

    /**
     * 1. available frequency 2. suported link 3. bandwidth
     *
     * @param ochLink
     */
    private void updateSiteLink(Link ochLink, List<String> siteLinkIds) {
        if (siteLinkIds == null) {
            return;
        }

        List<Link> allSiteLinks = new LinkedList<>();
        for (String linkId : siteLinkIds) {
            if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                allSiteLinks.add(changedObject.getChangedSiteLink(linkId));
            }
        }

        SiteLinkAvaliableRebuild rebuild = new SiteLinkAvaliableRebuild(allSiteLinks);

        if (rebuild.checkOverlapAndRebuild(ochLink)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Invalid frequency, overlap with existed");
        }

        for (Link siteLink : allSiteLinks) {
            Link changedSiteLink = changedObject.getChangedSiteLink(
                    siteLink.getLinkId().getValue());
            SiteLinkOchUpdater updater = new SiteLinkOchUpdater(changedSiteLink);
            updater.addNewOch(ochLink);  //remove frequency ava, bandwidth, supportedLink...
            changedObject.addChangedSiteLink(updater.getSiteLink());
        }
    }

    static List<String> collectSiteLinkIdsFromSupportingLinks(List<SupportingLink> supportingLinks) {
        if (supportingLinks == null) {
            return Collections.emptyList();
        }

        Set<String> siteLinkIds = new LinkedHashSet<>();
        for (SupportingLink supportingLink : supportingLinks) {
            if (supportingLink.getLinkRef() == null) {
                continue;
            }
            String linkId = supportingLink.getLinkRef().getValue();
            if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                siteLinkIds.add(linkId);
            }
        }
        return new ArrayList<>(siteLinkIds);
    }


    /**
     * 这些点都需要写入数据库，所以直接放到changedObject
     *
     * @param allocateProvidedNodes
     * @return
     */
    private void convert2NtNode(List<Nodes> allocateProvidedNodes) {
        for (Nodes nodes : allocateProvidedNodes) {
//            changedObject.addChangedPhyNode(new NodeBuilder()
            Node newNode = new NodeBuilder()
                    .setNodeId(nodes.getNodeId())
                    .setKey(new NodeKey(nodes.getNodeId()))
                    .setSupportingNode(new ArrayList<>())
                    .setTerminationPoint(convert2NtTerminiationPoint(nodes.getTerminationPoint()))
                    .addAugmentation(Node1.class,
                            new Node1Builder().setPhysical(nodes.getPhysical()).build())
                    .build();
            newNode = extendXcAttribute(newNode);
            changedObject.addChangedPhyNode(newNode);
        }
    }

    /**
     * 电信模型 的TTI/Loopback/testSignal 属性不在PTP上，而是CTP 以400G板卡为例 Lport-PTP---OTU4c--4xODU4 (5个CTP都有这些属性，子ODU4的暂时不支持) Cport-PTP---ODU4 于Adaptert讨论或确定， adapter把L口的OTU4c, C口的ODU4上的属性放到PTP上。 这个方法可以不实现， C/L
     *
     * @param newNode
     * @return
     */
    private Node extendXcAttribute(Node newNode) {
//        if ()
        return newNode;
    }

    private List<TerminationPoint> convert2NtTerminiationPoint(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint> internalTpList) {
        List<TerminationPoint> ntTpList = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint internalTp : internalTpList) {
            ntTpList.add(new TerminationPointBuilder()
                    .setTpId(internalTp.getTpId())
                    .setKey(new TerminationPointKey(internalTp.getTpId()))
                    .addAugmentation(TerminationPoint1.class,
                            new TerminationPoint1Builder().setPhysical(internalTp.getPhysical())
                                    .build())
                    .build());
        }
        return ntTpList;
    }

    /**
     * 这些node里面有reused, 也有计算后需要新添加的node 对于新node 我们需要把他们加入对应的siteNode， 同时修改friendlyName 这些node都是通过上一步计算出来的结构， 对于reUsed， 前面已经在snapShot与数据库比对一致性 所以可以直接作为入库数据
     *
     * @param nodeId
     * @param allSiteLinkIds  业务穿越的所有siteLink
     * @return
     */
    private void updateNodeRelation(String nodeId, List<String> allSiteLinkIds) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        String siteLinkId;
        if (allSiteLinkIds == null || allSiteLinkIds.isEmpty()) {
            //virtual siteLink
            siteLinkId = Constant.VIRTUAL_PLANE;
        } else {
            siteLinkId = getSiteLinkId(nodeId, allSiteLinkIds);
        }
        updateSiteNode(node, siteLinkId);  //add the TPC into rack

        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(node.getNodeId().getValue());
        updatePhyNodeFirendlyName(node, changedObject.getChangedSiteNode(siteNodeId));
    }

    /**
     * 基于tpcNode， 查找应该挂接到那个siteLink上
     *
     * @param tpcNodeId
     * @param allSiteLinkIds
     * @return
     */
    private final static String key = "key";
    private String getSiteLinkId(String tpcNodeId, List<String> allSiteLinkIds) {
        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(tpcNodeId);
//    Node siteNode = changedObject.getChangedSiteNode(siteNodeId);

        //即使是OCHP， 涉及俩个siteLink， 网元还是归属于主siteLink的Rack
        //对于REG， 同时跨接两个siteLink, 这里是找到的第一个
        List<String> relatedSiteLinks = allSiteLinkIds.stream()
                .filter(linkId->linkId.contains(siteNodeId))
                .collect(Collectors.toList());

        if (relatedSiteLinks.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the TD node cannot find related siteLink should binding");
        }

        if (relatedSiteLinks.size() == 1) {
            return relatedSiteLinks.get(0);
        } else {
            //找到已经添加了REG的复用段，一直都用它，如果是第一个REG TPC节点，任选一个siteLinkId 都可以，因为它们的siteNode是同一个
            //当发现现有添加REG的RAG 没有位置的时候， 添加到另外一个复用段
            synchronized (key) {
                for (String existedSiteLinkId : relatedSiteLinks) {
                    if (SiteNodeCorrelateResource.hasSpaceInRack(
                            changedObject.getChangedSiteNode(siteNodeId), existedSiteLinkId)) {
                        return existedSiteLinkId;
                    }
                }
                return relatedSiteLinks.get(0);  //这种情况会放到负数位置
            }
        }
    }

    /**
     * insert the TPC to rack when this is new TPC 一个rack 一个方向，rackID 就是这个方向的siteLinkID 但是引入OPC网元重用以后，作为siteLink起点的网元，可能被用作其它siteLink的中间点（ILA/DGE） 这种情况下，我们仅仅在rack的friendly Name上有体现。
     *
     * @param tpcNode
     */
    private void updateSiteNode(Node tpcNode, String siteLinkId) {
        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(tpcNode.getNodeId().getValue());
        Node siteNode = changedObject.getChangedSiteNode(siteNodeId);

        SiteNodeCorrelateResource correlateResource = new SiteNodeCorrelateResource(siteNode);

//    简单看一个TPC网元只会连接一个机架（rack), 换句话就是 这个TPC网元上的所有L口都连接到同一个机架的OPC网元上
        correlateResource.insertRack(siteLinkId, tpcNode);

        changedObject.addChangedSiteNode(correlateResource.getSiteNode());
    }

    private void merge2OpDB() {
        Iterator<Node> iter = changedObject.getChangedPhyNodeList().values().iterator();

        while (iter.hasNext()) {
            Node cfgNode = iter.next();
            Node opNode = changedObject.getChangedPhyOpNode(cfgNode.getNodeId().getValue());
            if (opNode != null) {
                Node newOpNode = new PhyNodeMerge(cfgNode, opNode).add();
                changedObject.addChangedPhyOpNode(newOpNode);
            }
        }
    }

    /**
     * 由于create tunnel 可能会添加板卡，或者Transceiver，这些信息需要添加到OP树 因为UI看到的数据就是OP树的（如果有的话）
     *
     * @param createdTunnelList
     */
    private void merge2OpDB(List<Tunnel> createdTunnelList) {
        Set<String> nodeIdSet = TunnelUtil.getTunnelNode(createdTunnelList);

        for (String nodeId : nodeIdSet) {
            Node opNode = changedObject.getChangedPhyOpNode(nodeId);
            if (opNode != null) {
                Node cfgNode = changedObject.getChangedPhyNode(nodeId);
                Node newOpNode = new PhyNodeMerge(cfgNode, opNode).add();
                changedObject.addChangedPhyOpNode(newOpNode);
            }
        }
    }

    private void logMessage(int tunnelNumber, String srcFriendlyName, String dstFriendlyname,
                            List<Tunnel> createdTunnelList, String errorMessage) {
        String msg = String.format("batch creating (%s) services  %s, %s ", tunnelNumber, srcFriendlyName,
                dstFriendlyname);

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //创建成功
            isOk = true;
            extMsg = "successfully.";
        }
        if (taskInfoMessage != null) {
            taskInfoMessage.setEndTime(System.currentTimeMillis());
            if (isOk) {
                if (createdTunnelList.size() > 1) {
                    //send a summarize
                    taskInfoMessage.setResourceId(msg + System.currentTimeMillis());
                    taskInfoMessage.setResourceName(msg);
                    taskInfoMessage.setSuccessfully(isOk);
                    taskInfoMessage.setErrorReason(null);

                    TaskInfoMessager.sendMessage(taskInfoMessage);
                    taskInfoMessage.setDetail(null);  //存储单个tunnel的时候detail不需要了
                }

                for (Tunnel tunnel : createdTunnelList) {
                    taskInfoMessage.setResourceId(tunnel.getTunnelId().getValue());
                    taskInfoMessage.setResourceName(tunnel.getFriendlyName());
                    taskInfoMessage.setSuccessfully(isOk);

                    TaskInfoMessager.sendMessage(taskInfoMessage);
                }
            } else {
                taskInfoMessage.setResourceId(msg);
                taskInfoMessage.setResourceName(srcFriendlyName + "--" + dstFriendlyname);
                taskInfoMessage.setSuccessfully(isOk);
                taskInfoMessage.setErrorReason(errorMessage);

                TaskInfoMessager.sendMessage(taskInfoMessage);
            }
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("create tunnel")
                        .message(msg + extMsg)
                        .error(!isOk)
                        .build());
    }


    /**
     * 基于siteNode的名称生成phyNode的名称
     */
    private Node updatePhyNodeFirendlyName(Node phyNode, Node siteNode) {
        PhyNodeFriendlyName phyNodeFriendlyNameGenerator = SpringBeanFinder.getBean(
                PhyNodeFriendlyName.class);
        Node newNode = phyNodeFriendlyNameGenerator.updateFriendlyName(phyNode, siteNode);

        changedObject.addChangedPhyNode(newNode);
        return newNode;
    }

    private Link updateOchLinkSupportedTunnel(Link ochLink, Tunnel tunnel) {
        List<SupportedTunnel> list = ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                .getSupportedTunnel();
        list.add(new SupportedTunnelBuilder()
                .setTunnelRef(tunnel.getTunnelId())
                .setKey(new SupportedTunnelKey(tunnel.getTunnelId()))
                .build());

        ochLink = new LinkBuilder(ochLink)
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class,
                        new Link1Builder().setSupportedTunnel(list).build())
                .build();

        return ochLink;
    }

    /**
     * avaliable is based on xc L port slot  odu4x2=1/odu4=1
     *
     * @param ochLink
     * @param oduXC
     */
    private Link updateOchLinkAvaliable(Link ochLink, CrossConnections oduXC) {
        String odujSlot = TunnelConstructor.getOduSlot(oduXC);
        Och och = ochLink.getAugmentation(Link1.class).getOch();

        List<Available> newavaList = OtCardType.removeOchAvailable(och.getCardType(),
                och.getAvailable(), param.getTunnelOdu(), odujSlot);
        ochLink = new LinkBuilder(ochLink)
                .addAugmentation(Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder()
                                .setOch(new OchBuilder(och).setAvailable(newavaList)
                                        .build())
                                .build())
                .build();

        return ochLink;
    }

    public TunnelCreator3 setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;

        return this;
    }


    private SiteLinkRoute createVirtualSiteLink() {
        SiteLinkCreator<CreateLinkInput> siteLinkCreator = new SiteLinkCreator();
        List<Segment> segmentList = new ArrayList<>();
        segmentList.add(new SegmentBuilder()
                .setIndex(0)
                .setSource(srcSiteNode.getNodeId().getValue())
                .setDestination(dstSiteNode.getNodeId().getValue())
                .setSourceNodeType(LinkTerminationNodeType.SITE)
                .setDestinationNodeType(LinkTerminationNodeType.SITE)
                .setRole(RoutingType.Main)
                .setProvider(new ProviderBuilder()
                        .setAttenuation(BigDecimal.valueOf(20))
                        .setDistance(BigDecimal.valueOf(100))
                        .setFiberType(FiberType.G652)
                        .build())
                .build());

        CreateLinkInput input = new CreateLinkInputBuilder()
                .setFriendlyName(Constant.VIRTUAL_PLANE + System.currentTimeMillis())
                .setFrequencyGrid((short) 50)
                .setLinkModel("2")
                .setOrderId(Constant.VIRTUAL_PLANE)
                .setPlaneName(Constant.VIRTUAL_PLANE)
                .setRiskGroupName(Constant.VIRTUAL_PLANE)
                .setSingleFrequencyPower(BigDecimal.valueOf(-1))
                .setSegment(segmentList)
                .build();
        Link createdLink = siteLinkCreator.creationLogic(input, false);

        return null;
    }
}
