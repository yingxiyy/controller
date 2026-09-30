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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.OpNodeMerger;
import net.flex.dci.otn.controller.allocate.common.namingrule.OchLinkFriendlyName;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.common.service.MyExecutor;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.link.common.ReuseNodeChecker;
import net.flex.dci.otn.controller.allocate.link.och.OchLinkConstructor;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeMerge;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel2Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel2OutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.create.tunnel._2.input.TunnelAllocateResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnelKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.Vendor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.RouteInfos;

@Slf4j
public class TunnelCreator2 {

    protected TaskInfoMessage taskInfoMessage;
    //  private TaskInfoKafkaService kafka;
    //=========================================
    //parser from creation param, follow variable is key for create tunnel
    String vendorName;
    String productType;
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

    public TunnelCreator2() {
        locker = new ZkResourceLock();
        mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);

        changedObject = new ChangedObject();

    }

    public CreateTunnel2Output doIt(CreateTunnel2Input input) throws CommonException {
        log.debug("start create tunnel");

        new ReuseNodeChecker(changedObject).checkInitialEnv(
                input.getTunnelAllocateResult().getReusedNodesSnapshot());

        param = new ParamCreate();
        param.parser(input);

        //由于耗时, 把这个同步命令改为异步

        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        executor.lazyDo(new Runnable() {
            @Override
            public void run() {
                lazy(input);
            }
        });

        CreateTunnel2OutputBuilder ob = new CreateTunnel2OutputBuilder()
                .setTunnels(new ArrayList<>());
        return ob.build();
    }

    private void lazy(CreateTunnel2Input input) throws CommonException {
        log.debug("create point2point tunnel");

        Node srcNode = changedObject.getChangedSiteNode(input.getSrcSite().getValue());
        Node dstNode = changedObject.getChangedSiteNode(input.getDstSite().getValue());
        String srcFriendlyName = srcNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite().getFriendlyName();
        String dstFriendlyname = dstNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite().getFriendlyName();

        List<Tunnel> createdTunnel = new LinkedList<>();
        try {
            TunnelAllocateResult allocateResource = input.getTunnelAllocateResult();

            lockResource(allocateResource);
            for (Vendor bundle : allocateResource.getVendor()) {
                //一个vendor 就是一个厂家需要建立的tunnel数
                createdTunnel.addAll(createBundleOfTunnel(bundle));
            }

//      merge2OpDB(createdTunnel);
//      merge2OpDB();
            mongoTransaction.save(changedObject);

            Set<String> changedNodeList = new HashSet<>();
            for (Tunnel tunnel : createdTunnel) {
                changedNodeList.addAll(net.flex.dci.otn.controller.allocate.link.common.Route
                        .getNodeIdOverRoute(tunnel.getExplictRoute().getRoute()));
            }
            OpNodeMerger opMerger = new OpNodeMerger();
            for (String nodeId : changedNodeList) {
                opMerger.merge(nodeId);
            }

            logMessage(param.getBundleNumber(), srcFriendlyName, dstFriendlyname, createdTunnel,
                    null);
            log.debug("tunnel creation done.");
        } catch (CommonException ce) {
            logMessage(param.getBundleNumber(), srcFriendlyName, dstFriendlyname, createdTunnel,
                    ce.getMessage());
            log.error("tunnel creation fail", ce);

            throw ce;
        } catch (Exception ce) {
            log.error("create tunnel error.", ce);
            logMessage(param.getBundleNumber(), srcFriendlyName, dstFriendlyname, createdTunnel,
                    ce.toString());

            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "create tunnel error: " + ce.toString(), ce);
        } finally {

            locker.unlock();
        }
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
        String msg = String.format("批量创建(%s)条业务  %s, %s ", tunnelNumber, srcFriendlyName,
                dstFriendlyname);

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //创建成功
            isOk = true;
            extMsg = "successfully.";
        }
        if (taskInfoMessage != null) {
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
     * 一个bundle就是一个厂家，每个厂家都会有几条tunnel。 每个tunnel的路由是一样的（对于p2p 就是一条siteLink）
     *
     * @param bundleInfo
     */
    private List<Tunnel> createBundleOfTunnel(Vendor bundleInfo) throws CommonException {
        log.debug("create bundle of tunne (one vendor) {}  {}", bundleInfo.getVendorName(),
                bundleInfo.getRouteInfos().size());
        List<Tunnel> tunnelList = new LinkedList<>();

        getBundleParams(bundleInfo);  //get siteLink info.

        for (RouteInfos route : bundleInfo.getRouteInfos()) {
            try {
                Tunnel tunnel = createTunnel(new AllocateRouteConvertor(route).convert());

                tunnelList.add(tunnel);
            } catch (CommonException ce) {
                throw ce;
            } catch (Exception ce) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "create tunnel error: " + ce.toString(), ce);
            }
        }
        return tunnelList;
    }

    private void lockResource(TunnelAllocateResult allocateResource) {
        for (Vendor vendor : allocateResource.getVendor()) {
            locker.addResource(vendor.getSiteLinkId());
        }
        locker.getLock();
    }

    /**
     * check which kind of tunnel should be created 修改后需要修改数据库的 if (新建OCH) { 新的OCHLink siteLink
     * 中添加supportedLInk siteLink中的可用频率变化 if (新TPC网元) { OCH Topo 中加新 OCH node siteNode 修改rack 和
     * supported node } OCH node 加 新TP } else { reuse OCH, siteLink 不做修改 } OCH 中添加suppotedTunnel,
     * 修改OCH的可用客户层数量
     *
     * @param route
     */
    private Tunnel createTunnel(RouteInfo route) {
        Tunnel tunnel;
        if (!secondarySiteLinkIds.isEmpty()) {
            tunnel = createOchpTunnel(route);
        } else if (primarySiteLinkIds.size() > 1) {
            tunnel = createWSSTunel(route);
        } else {
            //现在的输入只支持这个tunnel
            tunnel = createSimpleTunnel(route);
        }
        changedObject.addChangedTunnel(tunnel);

        return tunnel;
    }

    /**
     * simpleTunnel 点到点的直到tunnel， 中间只有一段siteLink， 不会存在secondaryLink 直接使用前期allocate的信息创建tunnel
     *
     * allocate算法原则 生成新TPC网元时，只生成必要的信息（所有辅助板卡，唯独没有业务板卡） 然后根据tunnel的需求， 增加新信息(新业务板卡， 新Transceiver,
     * 新交叉，新internalLink， 如果生成了新板卡， 基于板卡类型生成所有端口) 后面再次生成Tunnel资源，就是加Transceiver， XC 和 internalLink
     * (有InternalLink就需要创建OCH link)
     *
     * 1. new OT card, new C/L transceiver, new L----OPC link, new XC between C-L 2. reuse OT card,
     * reuse L port, new C transceiver, new XC between C-L 3. reuse OT card, new C/L transceiver,
     * new L----OPC link, new XC between C-L
     *
     * for OPC node has following conditions: (following diff this module is transparent) 1. fix
     * grid: mux--demux XC 2. flex grid: mux---OA XC (ne level) 3. update OPC node's ocm (frequency)
     * value
     *
     * @param info
     */
    private Tunnel createSimpleTunnel(RouteInfo info) throws CommonException {
        log.debug("create simple tunnel actually");

        //check UI 数据重复发送导致已经创建的siteLink被要求再次创建

        int lastOneXc = info.getMain().getXcs().size() - 1;
        CrossConnections oduXcA = info.getMain().getXcs().remove(0);
        CrossConnections oduXcZ = info.getMain().getXcs().remove(lastOneXc);

        String[] tpIds = TunnelConstructor.getTunnelTerminationPoint(oduXcA, oduXcZ);
        String tunnelId = TunnelIdNamingRule.generateId(tpIds[0], tpIds[1]);
        Tunnel existedTunnel = changedObject.getChangedTunnel(tunnelId);
        if (existedTunnel != null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "BOM required Tunnel has created");
        }
        Node[] tunnelNodes = new Node[2];
        tunnelNodes[0] = info.getMain().getNodes().get(0);
        tunnelNodes[1] = info.getMain().getNodes().get(info.getMain().getNodes().size() - 1);

        changedObject.addChangedPhyNode(tunnelNodes[0]);
        changedObject.addChangedPhyNode(tunnelNodes[1]);

        //无论conf数据库中是否有这个网元都需要用allocate中提供的数据，
        for (Node node : tunnelNodes) {
            Node updatedNode = changedObject.getChangedPhyNode(node.getNodeId().getValue());
            updateSiteNode(
                    updatedNode);  //add the TPC into rack when the node isn't include in site

            String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(
                    updatedNode.getNodeId().getValue());
            updatePhyNodeFirendlyName(updatedNode, changedObject.getChangedSiteNode(siteNodeId));
        }
        tunnelNodes[0] = changedObject.getChangedPhyNode(tunnelNodes[0].getNodeId().getValue());
        tunnelNodes[1] = changedObject.getChangedPhyNode(tunnelNodes[1].getNodeId().getValue());

        String aTp = CrossConnectionSlotNamingRule.getLinePortFromOduXc(oduXcA);
        String zTp = CrossConnectionSlotNamingRule.getLinePortFromOduXc(oduXcZ);
        Link ochLink = getOchLink(tunnelNodes[0], tunnelNodes[1], aTp, zTp,
                info); //remove oduXC for ochLink

        //add back for tunnel
        info.getMain().getXcs().add(0, oduXcA);
        info.getMain().getXcs().add(oduXcZ);
        Tunnel tunnel = new TunnelConstructor(param).create(tpIds, info, ochLink, tunnelNodes[0],
                tunnelNodes[1]);
        ochLink = updateOchLinkAvaliable(ochLink, info.getMain().getXcs().get(0));
        ochLink = updateOchLinkSupportedTunnel(ochLink, tunnel);
        changedObject.addChangedOchLink(ochLink);

//    new TunnelPhyResource(changedObject).copyPhyResource(tunnel, info);

        return tunnel;
    }

    /**
     * 新tunnel可能会带来新板卡和新Transceiver
     *
     * @param topoNode
     * @param node
     * @return
     */
    private Node merge2TopoNode(Node topoNode, Node node) {
        if (node.getTerminationPoint().size() > 0) {
            topoNode.getTerminationPoint().addAll(node.getTerminationPoint());
        }
        topoNode.getAugmentation(Node1.class).getPhysical().getEquipments().addAll(
                node.getAugmentation(Node1.class).getPhysical().getEquipments());

        return topoNode;
    }

    private Link getOchLink(Node srcNode, Node dstNode, String aTp, String zTp, RouteInfo info) {
        log.debug("find OCH link");

        //check does OCH link is required
        Link ochLink = findOchLinkWithLineTp(aTp, zTp);
        if (ochLink != null) {
            //the links will be NE TPC----OPC
            //here the value is empty, means no new links required.
            //this tunnel reuse existed OCH link
            log.debug("reuse ochLink {}", ochLink.getLinkId());
        } else {
            //should create a new OCH link

            String ochFriendlyName = OchLinkFriendlyName.buildFriendlyName(srcNode, dstNode, aTp,
                    zTp);

            OchLinkConstructor ochLinkConstructor = new OchLinkConstructor(changedObject, param);
            RouteInfo ochInfo = ochLinkConstructor.getOchRouteInfo(info, primarySiteLinkIds,
                    secondarySiteLinkIds);
            ochLink = ochLinkConstructor.create(vendorName, "", aTp, zTp, ochFriendlyName, false,
                    ochInfo);
            log.debug("create new ochLink {} at {} ", ochFriendlyName,
                    ochLink.getAugmentation(Link1.class).getOch().getLowerFrequency().getValue()
                            .toString());

            //add ochLink into siteLink data structure. such as bandwith, supportedLink, frequency
            updateSiteLink(ochLink);
        }

        return ochLink;
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
            for (String linkId : secondarySiteLinkIds) {
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
        if (phyNode.getAugmentation(Node1.class).getPhysical().getFriendlyName()
                .equals(phyNode.getNodeId().getValue())) {
            Node newNode = phyNodeFriendlyNameGenerator.updateFriendlyName(phyNode, siteNode);

            changedObject.addChangedPhyNode(newNode);
            return newNode;
        }
        return phyNode;
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

    private Tunnel createOchpTunnel(RouteInfo route) {
        //not supported in this version
        //each och link could pass through wss
        return null;
    }

    private Tunnel createWSSTunel(RouteInfo route) {
        //not supported in this version
        //each och link could pass through wss
        return null;
    }

    /**
     * not support OCH now (this will import main/spare siteLink) not suport WSS OCH link (this will
     * require multiple siteLinks)
     *
     * @param bundleInfo
     * @throws CommonException
     */
    private void getBundleParams(Vendor bundleInfo) throws CommonException {
        vendorName = bundleInfo.getVendorName();
        bundleNumber = bundleInfo.getRouteInfos().size();
        primarySiteLinkIds = new ArrayList<>();
        secondarySiteLinkIds = new ArrayList<>();

        if (vendorName == null || vendorName.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "vendorName is mandatory");
        }
        if (bundleNumber <= 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "bundleNumber must larger than 1");
        }
        String siteLinkId = bundleInfo.getSiteLinkId();
        if (siteLinkId == null && siteLinkId.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink is mandatory");
        }
        changedObject.getChangedSiteLink(siteLinkId);
        primarySiteLinkIds.add(siteLinkId);
    }

    public TunnelCreator2 setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
//        this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        return this;
    }
}
