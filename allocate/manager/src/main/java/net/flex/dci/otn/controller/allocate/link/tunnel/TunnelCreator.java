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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.namingrule.OchLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.common.service.MyExecutor;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelInput;
import net.flex.dci.otn.controller.allocate.link.och.OchLinkConstructor;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater;
import net.flex.dci.otn.controller.allocate.link.view.ViewLink;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.AvailableKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnelKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info.RouteBundleInfo;

@Slf4j
public class TunnelCreator {

    protected TaskInfoMessage taskInfoMessage;
//  private TaskInfoKafkaService kafka;
    //=========================================
    //parser from creation param, follow variable is key for create tunnel
    String vendorName;
    String productType;
    NodeType nodeType;
    int bundleNumber;
    List<String> primarySiteLinkIds;
    List<String> secondarySiteLinkIds;
    private ZkResourceLock locker;

    //==========================================
    //==================key value for doIt tunnel
    private ParamCreate param;
    private ChangedObject changedObject;
    private MultipleTransaction mongoTransaction;
    //===================

    public TunnelCreator() {
        locker = new ZkResourceLock();
        mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
    }

    public CreateTunnelOutput doIt(CreateTunnelInput input) throws CommonException {
        log.debug("start create tunnel");

        param = new ParamCreate();
        param.parser(input);

        //由于耗时, 把这个同步命令改为异步

        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        executor.lazyDo(new Runnable() {
            @Override
            public void run() {
                lazy();
            }
        });

        CreateTunnelOutputBuilder ob = new CreateTunnelOutputBuilder()
                .setTunnels(new ArrayList<>());
        return ob.build();
    }

    private void lazy() throws CommonException {
        log.debug("create point2point tunnel");

        List<Tunnel> createdTunnel = new LinkedList<>();
        List<RouteBundleInfo> routeBundleInfoList = param.getRouteBundleInfo();
        for (RouteBundleInfo bundle : routeBundleInfoList) {
            //一个bundleInfo 就是一个厂家需要建立的tunnel数
            createdTunnel.addAll(createBundleOfTunnel(bundle));
        }
    }

    /**
     * 一个bundle就是一个厂家，每个厂家都会有几条tunnel。 每个tunnel的路由是一样的（对于p2p 就是一条siteLink）
     *
     * @param bundleInfo
     */
    private List<Tunnel> createBundleOfTunnel(RouteBundleInfo bundleInfo) throws CommonException {
        log.debug("create bundle of tunne (one vendor)");
        List<Tunnel> tunnelList = new LinkedList<>();
        getParams(bundleInfo);

        for (int index = 0; index < bundleInfo.getBundleNumber(); index++) {
            try {
                lockResource();
                Tunnel tunnel = createTunnel();
                locker.unlock();
                tunnelList.add(tunnel);

                log.debug("create Tunnel done.");
                logMessage(tunnel, null);

            } catch (CommonException ce) {
                log.error("create tunnel error.", ce);
                logMessage(null, ce.getMessage());

                throw ce;
            } catch (Exception e) {
                log.error("create tunnel error.", e);
                logMessage(null, e.toString());

                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "create tunnel error: " + e.toString(), e);
            } finally {
                locker.unlock();
                log.debug("create Tunnel done.");
            }

        }

        return tunnelList;
    }

    public void logMessage(Tunnel removedTunnel, String errorMessage) {
        String msg = String.format("create tunnel %s ", removedTunnel.getFriendlyName());

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //成功
            isOk = true;
            extMsg = "successfully.";
        }

        if (taskInfoMessage != null) {
            if (isOk) {
                taskInfoMessage.setResourceId(removedTunnel.getTunnelId().getValue());
                taskInfoMessage.setResourceName(removedTunnel.getFriendlyName());
                taskInfoMessage.setSuccessfully(isOk);
            } else {
                taskInfoMessage.setSuccessfully(isOk);
                taskInfoMessage.setErrorReason(errorMessage);
            }
            TaskInfoMessager.sendMessage(taskInfoMessage);
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("create tunnel")
                        .message(msg + extMsg)
                        .error(!isOk)
                        .build());
    }


    private void lockResource() {
        /**
         * maybe change
         *    siteNode (rack info)
         *    siteLink (add supporting och Link)
         *    ochLink  (add supporting tunnel)
         *    phyNe change happen in neDesigner
         */

        for (String linkId : primarySiteLinkIds) {
            locker.addResource(linkId);
        }
        if (secondarySiteLinkIds != null) {
            for (String linkId : secondarySiteLinkIds) {
                locker.addResource(linkId);
            }
        }

        locker.getLock();
    }

    /**
     * check which kind of tunnel should be created 修改后需要修改数据库的 if (新建OCH) { 新的OCHLink siteLink
     * 中添加supportedLInk siteLink中的可用频率变化 if (新TPC网元) { OCH Topo 中加新 OCH node siteNode 修改rack 和
     * supported node } OCH node 加 新TP } else { reuse OCH, siteLink 不做修改 } OCH 中添加suppotedTunnel,
     * 修改OCH的可用客户层数量
     */
    private Tunnel createTunnel() {
        changedObject = new ChangedObject();
        Tunnel tunnel;
        if (!secondarySiteLinkIds.isEmpty()) {
            tunnel = createOchpTunnel();
        } else if (primarySiteLinkIds.size() > 1) {
            tunnel = createWSSTunel();
        } else {

            net.flex.dci.otn.controller.allocate.designer.model.RouteInfo info = getRouteResource();
            tunnel = createSimpleTunnel(info, param.getPlaneId());
        }
        changedObject.addChangedTunnel(tunnel);
        mongoTransaction.save(changedObject);

        return tunnel;
    }

    /**
     * neDesigner need lock the node (if exited) and only export new component for the tunnel. for
     * one TPC node has following conditions
     *
     * 1. new OT card, new C/L transceiver, new L----OPC link, new XC between C-L 2. reuse OT card,
     * reuse L port, new C transceiver, new XC between C-L 3. reuse OT card, new C/L transceiver,
     * new L----OPC link, new XC between C-L
     *
     * for OPC node has following conditions: (following diff this module is transparent) 1. fix
     * grid: mux--demux XC 2. flex grid: mux---OA XC (ne level) 3. update OPC node's ocm (frequency)
     * value
     */
    private Tunnel createSimpleTunnel(
            net.flex.dci.otn.controller.allocate.designer.model.RouteInfo info, String planeId)
            throws CommonException {
        log.debug("create simple tunnel actually");

        Node srcNode = info.getMain().getNodes().get(0);
        Node dstNode = info.getMain().getNodes().get(info.getMain().getNodes().size() - 1);

        for (Node node : info.getMain().getNodes()) {
            changedObject.addChangedPhyNode(node);
        }
        if (info.getSlave() != null && info.getSlave().getNodes() != null) {
            for (Node node : info.getSlave().getNodes()) {
                changedObject.addChangedPhyNode(node);
            }
        }

        //check does OCH link is required

        int lastOneXc = info.getMain().getXcs().size() - 1;
        CrossConnections oduXcA = info.getMain().getXcs().remove(0);
        CrossConnections oduXcZ = info.getMain().getXcs().remove(lastOneXc);
        String aTp = CrossConnectionSlotNamingRule.getLinePortFromOduXc(oduXcA);
        String zTp = CrossConnectionSlotNamingRule.getLinePortFromOduXc(oduXcZ);
        Link ochLink = getOchLink(srcNode, dstNode, aTp, zTp, info);

        //add back for tunnel
        info.getMain().getXcs().add(0, oduXcA);
        info.getMain().getXcs().add(oduXcZ);
        String[] tpIds = TunnelConstructor.getTunnelTerminationPoint(oduXcA, oduXcZ);
        Tunnel tunnel = new TunnelConstructor(param).create(tpIds, info, ochLink, srcNode, dstNode);
        ochLink = updateOchLinkAvaliable(ochLink, info.getMain().getXcs().get(0));
        ochLink = updateOchLinkSupportedTunnel(ochLink, tunnel);
        changedObject.addChangedOchLink(ochLink);
        new ViewLink(this.changedObject, planeId).create(ochLink);

//    saveBomInfo(tunnel, info);

        return tunnel;
    }

    private Link getOchLink(Node srcNode, Node dstNode, String aTp, String zTp,
            net.flex.dci.otn.controller.allocate.designer.model.RouteInfo info) {

        Link ochLink = findOchLinkWithLineTp(aTp, zTp);
        if (ochLink != null) {
            //the links will be NE TPC----OPC
            //here the value is empty, means no new links required.
            //this tunnel reuse existed OCH link
            log.debug("reuse ochLink {}", ochLink.getLinkId());
        } else {
            //should create a new OCH link
            log.debug("create new ochLink ");

            String ochFriendlyName = OchLinkFriendlyName.buildFriendlyName(srcNode, dstNode, aTp,
                    zTp);
            OchLinkConstructor ochLinkConstructor = new OchLinkConstructor(changedObject, param);
            net.flex.dci.otn.controller.allocate.designer.model.RouteInfo ochInfo = ochLinkConstructor.getOchRouteInfo(
                    info, primarySiteLinkIds, secondarySiteLinkIds);
            ochLink = ochLinkConstructor.create(vendorName, productType, aTp, zTp, ochFriendlyName,
                    false, ochInfo);
            log.debug("create new ochLink {} at {} ", ochFriendlyName,
                    ochLink.getAugmentation(Link1.class).getOch().getLowerFrequency().getValue()
                            .toString());

            changedObject.addChangedOchLink(ochLink);

            //add ochLink into siteLink data structure. such as bandwith, supportedLink, frequency
            updateSiteLink(ochLink);

            //change phyNode friendlyName if this is new NE.
            List<Link> changedPhyLinkList = new LinkedList<>();
            boolean isNewTpc = isNewTpcNode(srcNode);
            if (isNewTpc) {
                updateSiteNode(srcNode);  //add the TPC into rack
                String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(
                        srcNode.getNodeId().getValue());
                srcNode = updatePhyNodeFirendlyName(srcNode,
                        changedObject.getChangedSiteNode(siteNodeId));
            }

            isNewTpc = isNewTpcNode(dstNode);
            if (isNewTpc) {
                updateSiteNode(dstNode); //add into rack
                String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(
                        dstNode.getNodeId().getValue());
                dstNode = updatePhyNodeFirendlyName(dstNode,
                        changedObject.getChangedSiteNode(siteNodeId));
            }

            Link osLink1 = info.getMain().getLinks().get(0);
            new PhyLinkUtil(changedObject).addPhyLink(osLink1, param.getPlaneName(), null);

            Link osLink2 = info.getMain().getLinks().get(info.getMain().getLinks().size() - 1);
            new PhyLinkUtil(changedObject).addPhyLink(osLink2, param.getPlaneName(), null);
        }
        return ochLink;
    }

    private boolean isNewTpcNode(Node phyNode) {
        Physical phyNodeAttr = phyNode.getAugmentation(Node1.class).getPhysical();
        if (phyNodeAttr.getNodeType().equals(NodeType.TPC4)) {
//      PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
//      Node oldNode = phyNodeDao.getConfigPhyNodeById(phyNode.getNodeId().getValue());

            //for quicker, use neDesigner provided data check

//      if (oldNode == null) {
            if (phyNodeAttr.getCrossConnections().size()
                    == 1) { // only one XC between C port and L port
                return true;
            }
        }
        return false;
    }

    private net.flex.dci.otn.controller.allocate.designer.model.RouteInfo getRouteResource() {
        log.debug("build tunnel route info start...");
        TunnelInput input = prepareNeDesigner();
        net.flex.dci.otn.controller.allocate.designer.model.RouteInfo info = null;

        NeDesigner neDesigner = SpringBeanFinder.getBean(NeDesigner.class);
        try {
            info = neDesigner.allocateTunnel(input);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "error in neDesign " + e.getCause().getMessage(), e);
        }

        log.debug("build tunnel route info done.");
        return info;
    }

    /**
     * 1. available frequency 2. suported link 3. bandwidth
     *
     * @param ochLink
     */
    private void updateSiteLink(Link ochLink) {
        List<Link> allSiteLinks = new LinkedList<>();
        for (String linkId : primarySiteLinkIds) {
            allSiteLinks.add(changedObject.getChangedSiteLink(linkId));
        }
        if (secondarySiteLinkIds != null) {
            for (String linkId : primarySiteLinkIds) {
                allSiteLinks.add(changedObject.getChangedSiteLink(linkId));
            }
        }

        for (Link siteLink : allSiteLinks) {
            Link changedSiteLink = changedObject.getChangedSiteLink(
                    siteLink.getLinkId().getValue());
            SiteLinkOchUpdater updater = new SiteLinkOchUpdater(changedSiteLink);
            updater.addNewOch(ochLink);  //remove frequency ava, bandwidth, supportedLink...
            changedObject.addChangedSiteLink(updater.getSiteLink());
        }
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

    /**
     * insert the TPC to rack when this is new TPC
     *
     * @param tpcNode
     */
    private void updateSiteNode(Node tpcNode) {
        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(tpcNode.getNodeId().getValue());
        Node siteNode = changedObject.getChangedSiteNode(siteNodeId);

        SiteNodeCorrelateResource correlateResource = new SiteNodeCorrelateResource(siteNode);
        correlateResource.insertRack(primarySiteLinkIds.get(0), tpcNode);

        changedObject.addChangedSiteNode(correlateResource.getSiteNode());
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
        String slot = TunnelConstructor.getOduSlot(oduXC);

        Och och = ochLink.getAugmentation(Link1.class).getOch();
        List<Available> avaliableList = och.getAvailable();
        Available ava = avaliableList.get(0);

        String avaOdujString = ava.getAvailableOdujSlot();
        List<String> idsList = Arrays.asList(avaOdujString.split("-"))
                .stream().map(s -> s.trim()).collect(Collectors.toList());

        Iterator<String> iter = idsList.iterator();
        while (iter.hasNext()) {
            String id = iter.next();
            if (id.equals(slot)) {
                iter.remove();
                break;
            }
        }
        String newAvaString = idsList.stream().map(n -> String.valueOf(n))
                .collect(Collectors.joining("-"));

        och.getAvailable().clear();
        och.getAvailable().add(new AvailableBuilder()
                .setSupportedOduj(och.getSlotGranularity())
                .setAvailableOdujSlot(newAvaString)
                .setKey(new AvailableKey(och.getSlotGranularity()))
                .build());

        ochLink = new LinkBuilder(ochLink)
                .addAugmentation(Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder()
                                .setOch(och).build())
                .build();

        return ochLink;
    }

    /**
     * key is XC's slot info. the L port's slot is odu4x2=1/odu4=1
     *
     * @param aTp
     * @param zTp
     * @return
     */
    private Link findOchLinkWithLineTp(String aTp, String zTp) throws CommonException {
        String ochLinkId = OchLinkIdNamingRule.generateId(aTp, zTp);

        Link link = changedObject.getChangedOchLink(ochLinkId);
        if (link == null) {
            ochLinkId = OchLinkIdNamingRule.generateId(zTp, aTp);
            link = changedObject.getChangedOchLink(ochLinkId);
        }

        return link;
    }

    private TunnelInput prepareNeDesigner() {
        List<Node> reusedNodesInDbSrc = new ArrayList<>();
        List<Node> reusedNodesInDbDst = new ArrayList<>();

        TunnelInput input = TunnelInput.builder()
                .cardType(param.getCardType())
                .tunnelSignalRate(param.getTunnelSignalRate())
                .clientMedium(param.getClientMedium())
                .lineSignalRate(param.getLinePortSignalRate())
                .siteLinks(param.getPossibleSiteLinks())
                .vendorName(vendorName)
                .vendorType(productType)
                .reusedNodesInDbSrc(reusedNodesInDbSrc)
                .reusedNodesInDbDst(reusedNodesInDbDst)
                .build();

        return input;
    }

    private Tunnel createOchpTunnel() {
        //not supported in this version
        //each och link could pass through wss
        return null;
    }

    private Tunnel createWSSTunel() {
        //not supported in this version
        //each och link could pass through wss
        return null;
    }

    private void getParams(RouteBundleInfo bundleInfo) throws CommonException {
        vendorName = bundleInfo.getVendorName();
        productType = bundleInfo.getProductType();
        nodeType = bundleInfo.getNodeType();
        bundleNumber = bundleInfo.getBundleNumber();
        primarySiteLinkIds = new LinkedList<>();
        secondarySiteLinkIds = new LinkedList<>();

        primarySiteLinkIds = getPrimarySiteLinks(bundleInfo.getRouteInfo());
        secondarySiteLinkIds = getSecondarySiteLinks(bundleInfo.getRouteInfo());

        if (!secondarySiteLinkIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Only support primary route");
        }
        if (primarySiteLinkIds.size() != 1) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "primary route should only one siteLink");
        }
        if (vendorName == null || vendorName.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "vendorName is mandatory");
        }
        if (productType == null || productType.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "productType is mandatory");
        }
        if (bundleNumber <= 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "bundleNumber must larger than 1");
        }

      if (nodeType == null || !nodeType.equals(NodeType.TPC4)) {
        nodeType = NodeType.TPC4;
      }

//    for (String linkId : primarySiteLinkIds) {
//      for (Link siteLink : param.getPossibleSiteLinks()) {
//        if (siteLink.getLinkId().getValue().equals(linkId)) {
//          primarySiteLinks.add(siteLink);
//        }
//      }
//    }
//    if (secondarySiteLinkIds != null) {
//      for (String linkId : secondarySiteLinkIds) {
//        for (Link siteLink : param.getPossibleSiteLinks()) {
//          if (siteLink.getLinkId().getValue().equals(linkId)) {
//            secondarySiteLinks.add(siteLink);
//          }
//        }
//      }
//    }
    }

    private List<String> getPrimarySiteLinks(List<RouteInfo> routeList) throws CommonException {
        List<String> siteLinkIds = new LinkedList<>();

        for (RouteInfo route : routeList) {
            siteLinkIds.addAll(
                    getSiteLinkWithRouteSequences(route.getPrimary().getRouteSequence()));
        }
        return siteLinkIds;
    }

    private List<String> getSecondarySiteLinks(List<RouteInfo> routeList) throws CommonException {
        List<String> siteLinkIds = new LinkedList<>();

        for (RouteInfo route : routeList) {
            if (route.getSecondary() != null) {
                siteLinkIds.addAll(
                        getSiteLinkWithRouteSequences(route.getSecondary().getRouteSequence()));
            }
        }
        return siteLinkIds;
    }

    private Collection<? extends String> getSiteLinkWithRouteSequences(
            List<RouteSequence> routeSequenceList) throws CommonException {
        List<String> siteLinkIds = new LinkedList<>();
        for (RouteSequence rs : routeSequenceList) {
            if (rs.getResourceType().getImplementedInterface().getName()
                    .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Link.class.getName())) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Link link =
                        (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Link) rs.getResourceType();
                siteLinkIds.add(link.getLinkHop().getLinkId().getValue());
            } else {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "only support Link hop when create-tunnel");
            }
        }
        return siteLinkIds;
    }

    public TunnelCreator setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
//    this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        return this;
    }
}
