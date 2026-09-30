/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.site;

import static net.flex.dci.otc.common.constants.BroadCastConstant.REMOVE_SITE_LINK;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.ocm.LinkRoute;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.annotation.TaskInfoEnable;
import net.flex.dci.otn.controller.allocate.common.OpNodeMerger;
import net.flex.dci.otn.controller.allocate.common.service.MyExecutor;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelRemover;
import net.flex.dci.otn.controller.allocate.link.view.ViewLink;
import net.flex.dci.otn.controller.allocate.network.NetworkRemover;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeRemover;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeUtils;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@TaskInfoEnable(title = "remove siteLink", resourceType = TaskInfoMessage.ResourceType.siteLink, actionType = TaskInfoMessage.ActionType.delete)
public class SiteLinkRemover {

    private static final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private final MultipleTransaction mongoTransaction;
    protected TaskInfoMessage taskInfoMessage;
    private boolean quite = true; //用于virtual siteLink的删除。 当此siteLink支撑的所有业务消失后，此siteLink也会被删除
    //  private TaskInfoKafkaService kafka;
    private Link removedSiteLink;
    private ChangedObject changedObject;

    public SiteLinkRemover() {
        mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
    }

    public void setQuite(boolean quite) {
        this.quite = quite;
    }

    /**
     * 创建SiteLink将修改如下内容
     * 1. siteLink本身
     * 2. siteTopo的riskGroup内容
     * 3. siteNode的supportingNode中添加siteLink涉及的物理网元（phyNode）
     * 4. siteNode的TP点添加siteLink路由中的OTS Link (网元间连接）涉及的TP
     * 5. siteNode的Rack添加， 包含rack中的网元
     * 6. siteLink路由中的phyLink添加supportedLink为此siteLink
     *
     */

    /**
     * 删除SiteLink， 检查是否还处于impl状态
     *
     * 对于creaate siteLink时的反操作
     *
     * 1. siteLink本身 2. siteTopo的riskGroup内容  (这个最后锁一次，否则整个Topo 影响范围大） 3.
     * siteNode的supportingNode中添加siteLink涉及的物理网元（phyNode） (因为不删除网元，所以rack继续保留） 4.
     * siteNode的TP点添加siteLink路由中的OTS Link (网元间连接）涉及的TP (因为不删除网元，所以rack继续保留） 5. siteNode的Rack添加，
     * 包含rack中的网元  (因为不删除网元，所以rack继续保留） 6. siteLink路由中的phyLink添加supportedLink为此siteLink 7.
     * 客户层的内容客户层自己锁
     *
     * @param removeData
     * @return
     * @throws CommonException
     */
    public RpcResultType doIt(RemoveLinkInput removeData) throws CommonException {
        log.info("remove site link {} (forceDB: {}) start...", removeData.getLinkId(),
                removeData.isForceDb());
        try {
            changedObject = new ChangedObject();
            check(removeData);

            MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
            executor.lazyDo(new Runnable() {
                @Override
                public void run() {
                    synchronized (TopoNameConstants.Network_Topo_Key) {
                        lazy(removedSiteLink, removeData.isForce(), removeData.isForceDb());
                    }
                }
            });
            return RpcResultType.Success;
        } catch (Exception ex) {
            log.error("failed to remove site link,the reason is:{}", ex.getMessage(), ex);
            throw ex;
        }
    }

    private void lazy(Link removedSiteLink, Boolean force, Boolean forceDb) throws CommonException {
        String siteLinkId = removedSiteLink.getLinkId().getValue();
        log.info("lazy remove site link id is:{} and forceDB: {}", siteLinkId, forceDb);

        Site linkAttr = removedSiteLink.getAugmentation(Link1.class).getSite();
        ZkResourceLock locker = new ZkResourceLock();
        changedObject = new ChangedObject();
        try {
            lockResource(locker, removedSiteLink);

            Set<String> needMergedNodes = updateTopologyData(removedSiteLink, force, forceDb);
            new ViewLink(changedObject, linkAttr.getPlaneId()).remove(removedSiteLink);
            new ScanLink(changedObject).remove(removedSiteLink);

            mongoTransaction.save(changedObject);

            OpNodeMerger opMerger = new OpNodeMerger();
            needMergedNodes.forEach(opMerger::merge);

            logMessage(removedSiteLink, null);

            log.info("remove site link done {}", siteLinkId);
        } catch (CommonException ce) {
            logMessage(removedSiteLink, ce.getMessage());
            log.error("remove site link {} fail.", siteLinkId, ce);
            throw ce;
        } catch (Exception e) {
            logMessage(removedSiteLink, e.toString());
            log.error("remove site link {} fail.", siteLinkId, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "remove siteLink error" + e, e);
        } finally {
            locker.unlock();
        }
    }

    /**
     * * 由于remove 可能会删除Transceiver，或者card，这些信息需要更新到OP树 * 因为UI看到的数据就是OP树的（如果有的话）, *
     * 一个equip(包括transceiver)被Link使用了由属性"used-in-link" = true 表达 * 数据合法性需要检查
     * 板卡类型相同，used-in-link=false; * * *
     * 对于删除来说，deImpl的时候对应的XC、internalLink等网元上的资源已经删除，而板卡是硬件资源，无法在删除， * 这里的merge实际上只是修改used-in-link属性
     * （为了方便这个修改移到删除资源程序， markEmpty) *
     *
     * @param removedSiteLink
     */
//    private void merge2OpDB(Link removedSiteLink) {
//        Set<String> nodeIdSet = LinkRoute.getNodesOverSiteLink(removedSiteLink);
//
//        for (String nodeId : nodeIdSet) {
//            Node opNode = changedObject.getChangedPhyOpNode(nodeId);
//            if (opNode != null) {
//                Node cfgNode = changedObject.getChangedPhyNode(nodeId);
//                Node newOpNode = new PhyNodeMerge(cfgNode, opNode).del();
//                changedObject.addChangedPhyOpNode(newOpNode);
//            }
//        }
//    }
    private void logMessage(Link removedSiteLink, String errorMessage) {
        Link1 linkAttr = removedSiteLink.getAugmentation(Link1.class);
        String msg = String.format("remove siteLink %s ", linkAttr.getSite().getFriendlyName());

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //成功
            isOk = true;
            extMsg = "successfully.";
        }

        if (taskInfoMessage != null) {
            taskInfoMessage.setEndTime(System.currentTimeMillis());
            if (isOk) {
                taskInfoMessage.setResourceId(removedSiteLink.getLinkId().getValue());
                taskInfoMessage.setResourceName(linkAttr.getSite().getFriendlyName());
                taskInfoMessage.setSuccessfully(isOk);
            } else {
                taskInfoMessage.setSuccessfully(isOk);
                taskInfoMessage.setErrorReason(errorMessage);
            }
            TaskInfoMessager.sendMessage(taskInfoMessage);
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(REMOVE_SITE_LINK)
                        .message(msg + extMsg)
                        .error(!isOk)
                        .build());
    }

    /**
     * 只锁线资源
     *
     * @param locker
     * @param removedSiteLink
     */
    private void lockResource(ZkResourceLock locker, Link removedSiteLink) {
        Set<String> nodeList = LinkRoute.getNodesOverSiteLink(removedSiteLink);

        nodeList.forEach(x -> locker.addResource(PhysicalNodeIdNamingRule.getSiteId(x)));

        Site siteAttr = removedSiteLink.getAugmentation(Link1.class).getSite();

        locker.addResource(removedSiteLink.getLinkId().getValue());

        for (SupportedLink siteLinkSL : siteAttr.getSupportedLink()) {
            String ochLinkId = siteLinkSL.getLinkRef().getValue();
            if (OchLinkIdNamingRule.isOchLink(ochLinkId)) {
                locker.addResource(ochLinkId);

                OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
                Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
                if (ochLink != null) {
                    if (ochLink.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                            .getSupportedTunnel() != null) {
                        for (SupportedTunnel st : ochLink.getAugmentation(
                                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                                .getSupportedTunnel()) {
                            locker.addResource(st.getTunnelRef().getValue());
                        }
                    }
                }
            }
        }

        String srcSite = removedSiteLink.getSource().getSourceNode().getValue();
        String dstSite = removedSiteLink.getDestination().getDestNode().getValue();
        String[] viewLinkIds = ViewLink.getViewLinkId(srcSite, dstSite, ViewLinkType.SiteLink,
                siteAttr.getPlaneId());

        locker.addResource(viewLinkIds[0]);
        locker.addResource(viewLinkIds[1]);

        locker.getLock();

    }


    private void check(RemoveLinkInput removeData) throws CommonException {
        String siteLinkId = removeData.getLinkId();

        SiteLinkDao siteLinkDao = SpringBeanFinder.getBean((SiteLinkDao.class));
        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "required siteLink isn't existed");
        }
        removedSiteLink = siteLink;

        Site siteAttr = siteLink.getAugmentation(Link1.class).getSite();
        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(siteAttr.getExplictRoute().getRoute());

        if (!removeData.isForceDb()) {
            if (siteAttr != null) {
                if (ImplementState.Allocate != siteAttr.getImplementState()) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "the site link is still working");
                }
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "error siteLink info");
            }

            checkingNodeIp(rInfo.getNodeIdList());

            checkingElectricLevel();
        } else {

            checkNodeSupervision(rInfo.getNodeIdList());
            checkingElectricLevel();
        }
    }

    private void checkNodeSupervision(List<String> nodeIdList) {
        if (nodeIdList.isEmpty()) {
            return;
        }

        PhyNodeDao nodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
        String monitored = nodeIdList.stream().filter(nodeId -> {
                Node node = nodeDao.getConfigPhyNodeById(nodeId);
                if (node != null) {
                    Physical nodeAttr = node.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                            .getPhysical();
                    if (nodeAttr.getIp() != null) {
                        if (nodeAttr.getSupervisionStatus().equals(SupervisionStatusType.Monitoring)) {
                            return true;
                        }
                    }
                }
                return false;
            }).findAny().orElse(null);

        if (monitored != null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Please remove supervision on all NES over the siteLink");
        }
    }


    private void checkingNodeIp(List<String> nodeIdList) {
        if (nodeIdList.isEmpty()) {
            return;
        }

        PhyNodeDao nodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
        Optional<String> ipNodeOp = nodeIdList.stream().filter(nodeId -> {
            Node node = nodeDao.getConfigPhyNodeById(nodeId);
            if (node != null) {
                Physical nodeAttr = node.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                        .getPhysical();
                return nodeAttr.getIp() != null && !StringUtils.isEmpty(
                        nodeAttr.getIp());
            }
            return false;
        }).findAny();
        if (ipNodeOp.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "this site link still has ip biding on, please remove node ip which used in this siteLink");
        }
    }

    private void checkingElectricLevel() {
        Site siteLinkAttr = removedSiteLink.getAugmentation(Link1.class).getSite();

        Set<String> checkedNodes = new HashSet<>();
        siteLinkAttr.getSupportedLink().forEach(sl -> {
            String ochLinkId = sl.getLinkRef().getValue();
            String srcNode = OchLinkIdNamingRule.nodeA(ochLinkId);
            String dstNode = OchLinkIdNamingRule.nodeA(ochLinkId);

            Link ochLink = changedObject.getChangedOchLink(ochLinkId);
            if (ochLink == null) {
                log.warn("OCH link not found, linkRef: {}, skip current loop", ochLinkId);
                return;
            }
            Och ochAttr = ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                    .getOch();
            if (ochAttr.getImplementState() != ImplementState.Allocate) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        ochAttr.getFriendlyName() + " OCH link is still working ");
            }

            for (SupportedTunnel st : ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                    .getSupportedTunnel()) {
                String tunnelId = st.getTunnelRef().getValue();
                log.debug("remove och link supported tunnel {}", tunnelId);

                Tunnel tunnel = changedObject.getChangedTunnel(tunnelId);
                if (tunnel == null) {
                    log.error("this is impossible, the siteLink doesn't existed in DB");
                    continue;
                }
                if (tunnel.getImplementState() != ImplementState.Allocate) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            tunnel.getFriendlyName() + " tunnel is still working ");
                }
                checkedNodes.add(srcNode);
                checkedNodes.add(dstNode);
            }
        });
        checkingNodeIp(new ArrayList<>(checkedNodes));
    }

    /**
     * remove 所有客户层 remove 涉及OPC网元的内部连接
     *
     * @param removedSiteLink
     * @param force
     * @param forceDb
     * @return
     */
    private Set<String> updateTopologyData(Link removedSiteLink, Boolean force, Boolean forceDb)
            throws CommonException {
        log.debug("update topo data for remove site link start....");

        List<String> changedNodeList = net.flex.dci.otn.controller.allocate.link.common.Route
                .getNodeIdOverRoute(
                        removedSiteLink.getAugmentation(Link1.class).getSite().getExplictRoute()
                                .getRoute());

        Site siteAttr = removedSiteLink.getAugmentation(Link1.class).getSite();

        //find out all tunnels and remove them one by one

        OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
        TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
        TaskInfoMessage tunnelTaskInfoMessage = new TaskInfoMessage(taskInfoMessage);
        tunnelTaskInfoMessage.setResourceType(TaskInfoMessage.ResourceType.tunnel);
        tunnelTaskInfoMessage.setDetail(
                String.format("remove siteLink %s", siteAttr.getFriendlyName()));

        Set<String> needMergedNodes = new HashSet<>();
        for (SupportedLink sLink : siteAttr.getSupportedLink()) {
            String ochLinkId = sLink.getLinkRef().getValue();
            log.debug("should remove OCH link, {}", ochLinkId);

            Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
            if (ochLink == null) {
                continue;
            }

            Och ochAttr = ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                    .getOch();
            if (ochAttr == null) {
                log.debug("data corrupted. cannot find ochLink {}", ochLinkId);
            }

            for (SupportedTunnel st : ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                    .getSupportedTunnel()) {
                String tunnelId = st.getTunnelRef().getValue();
                log.debug("remove och link supported tunnel {}", tunnelId);

                Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
                if (tunnel == null) {
                    try {
                        throw new NullPointerException(
                                "this is impossible, the siteLink doesn't existed in DB");
                    } catch (NullPointerException e) {
                        log.error("!!! data corrupted. cannot find tunnel {}", tunnelId, e);
                        changedObject.addRemovedOchLink(ochLink.getLinkId().getValue());
                        changedObject.addRemovedSiteLink(removedSiteLink);
                        return needMergedNodes;
                    }
                }

                //这种情况下不能利用线程删除tunnel，否则不好控制
                TunnelRemover tunnelRemover = new TunnelRemover();
                tunnelRemover.syncRemove(tunnelTaskInfoMessage, tunnel, force, forceDb);
                needMergedNodes.addAll(tunnelRemover.getNeedMerged());
            }
        }

        //把siteLink从siteTopo的riskGroup中剔除
        removeSiteLinkFromRiskGroup(removedSiteLink);
        // SiteLinkRelation可能存在于历史或非network-id数据中，删除SiteLink时统一检查并回收WSS资源。
        new NetworkRemover(changedObject).remove(removedSiteLink);

        cleanPhyNeResourceOnLink(removedSiteLink);
        updateSiteNode(removedSiteLink);

        changedObject.addRemovedSiteLink(removedSiteLink);

//            对于被影响的网元需要 adapter 再次同步， 等待确定接口
//        OpNodeMerger opMerger = SpringBeanFinder.getBean(OpNodeMerger.class);
//        for (String nodeId : changedNodeList) {
//            opMerger.merge(nodeId);
//        }
        needMergedNodes.addAll(changedObject.getChangedPhyNodeList().keySet());
        log.debug("update topo data for remove site link done.");

        return needMergedNodes;
    }

    /**
     * based on route XC findout OA card and remove port, transceiver and card, fixed XC of card
     *
     * @param removedSiteLink
     */
    private void cleanPhyNeResourceOnLink(Link removedSiteLink) {
        removeSupportingLink(removedSiteLink);

        Site linkAttr = removedSiteLink.getAugmentation(Link1.class).getSite();
        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(linkAttr.getExplictRoute().getRoute());

        for (SupportingLink sl : removedSiteLink.getSupportingLink()) {
            String aTpId = PhysicalLinkIdNamingRule.getTpAId(sl.getLinkRef().getValue());
            String zTpId = PhysicalLinkIdNamingRule.getTpZId(sl.getLinkRef().getValue());

            String aEqId = PhysicalTpIdNamingRule.getEquipId(aTpId);
            String zEqId = PhysicalTpIdNamingRule.getEquipId(zTpId);
            if (!rInfo.getEqIdList().contains(aEqId)) {
                rInfo.getEqIdList().add(aEqId);
            }
            if (!rInfo.getEqIdList().contains(zEqId)) {
                rInfo.getEqIdList().add(zEqId);
            }
        }

        for (String eqId : rInfo.getEqIdList()) {
            String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
            emptySlot(nodeId, eqId);  //remove amplifier/aps card
        }

        for (String nodeId : rInfo.getNodeIdList()) {
            Node cfgNode = changedObject.getChangedPhyNode(nodeId);

            if (PhyNodeUtil.isEmptyNode(cfgNode)) {
                //只有网元为空，才从rack删除
                String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
                Node siteNode = changedObject.getChangedSiteNode(siteNodeId);
                if (siteNode != null) {
                    siteNode = SiteNodeUtils.removePhyNe(removedSiteLink.getLinkId().getValue(),
                            siteNode, nodeId);
                    changedObject.addChangedSiteNode(siteNode);
                } else {
                    log.error("the node related siteNode is not exist {}", siteNodeId);
                }

                changedObject.addRemovedPhyNode(nodeId);
            } else {
                changedObject.addChangedPhyNode(cfgNode);
            }
        }
    }


    private void emptySlot(String nodeId, String eqId) {
        Node cfgNode = changedObject.getChangedPhyNode(nodeId);
        if (null == cfgNode) {
            log.warn("the ne :{} already removed,donothing", nodeId);
            return;
        }
        cfgNode = PhyNodeUtil.markSlotEmpty(cfgNode, eqId);
        changedObject.addChangedPhyNode(cfgNode);
    }


    private void updateSiteNode(Link removedSiteLink) {
        log.debug("remove siteLink affects SiteNode info");
        //Rack
        //supporting ne
        //tp

        String removedSiteLinkFriendlyName = removedSiteLink.getAugmentation(Link1.class).getSite()
                .getFriendlyName();

        Set<String> nodeIdList = LinkRoute.getNodesOverSiteLink(removedSiteLink);
        for (String phyNodeId : nodeIdList) {
            String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(phyNodeId);
            Node siteNode = changedObject.getChangedSiteNode(siteNodeId);

            if (siteNode == null) {
                log.error("return direcly, the siteNode is not exist {}", siteNodeId);
                return;
            }
            //update rackId/rack friendlyName
            SiteNodeCorrelateResource correlateResource = new SiteNodeCorrelateResource(siteNode);
            correlateResource.updateRack_remove(removedSiteLink.getLinkId().getValue(),
                    removedSiteLinkFriendlyName, phyNodeId);
            siteNode = correlateResource.getSiteNode();

            removeSupportingNe(siteNode, phyNodeId);
            changedObject.addChangedSiteNode(siteNode);
        }

        String srcSiteNodeId = removedSiteLink.getSource().getSourceNode().getValue();
        String dstSiteNodeId = removedSiteLink.getDestination().getDestNode().getValue();
        String srcTpId = removedSiteLink.getSource().getSourceTp().getValue();
        String dstTpId = removedSiteLink.getDestination().getDestTp().getValue();
        Node srcSiteNode = changedObject.getChangedSiteNode(srcSiteNodeId);
        Node dstSiteNode = changedObject.getChangedSiteNode(dstSiteNodeId);
        removeSiteTp(srcSiteNode, srcTpId);
        removeSiteTp(dstSiteNode, dstTpId);
        changedObject.addChangedSiteNode(srcSiteNode);
        changedObject.addChangedSiteNode(dstSiteNode);
    }

    private void removeSiteTp(Node siteNode, String tpId) {
        log.debug("remove tp from site node TP list");
        Iterator<TerminationPoint> iter = siteNode.getTerminationPoint().iterator();
        while (iter.hasNext()) {
            TerminationPoint siteTp = iter.next();
            if (siteTp.getTpId().getValue().equals(tpId)) {
                iter.remove();
                break;
            }
        }
    }

    private void removeSupportingNe(Node siteNode, String phyNodeId) {
        log.debug("remove phy node from site node's supporing node list");
        Iterator<SupportingNode> iter = siteNode.getSupportingNode().iterator();
        while (iter.hasNext()) {
            SupportingNode sNode = iter.next();
            if (sNode.getNodeRef().getValue().equals(phyNodeId)) {
                iter.remove();
                break;
            }
        }
    }

    private Node removeFromRack(String linkId, Node siteNode, String phyNodeId) {
        log.debug("remove phy node from rack");
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site siteAttr = siteNode.getAugmentation(
                Node1.class).getSite();
        List<SupportingRack> rackList = siteAttr.getSupportingRack();
        Iterator<SupportingRack> rackIter = rackList.iterator();
        while (rackIter.hasNext()) {
            SupportingRack rack = rackIter.next();
            if (rack.getRackId().getValue().contains(linkId)) {
                //rackId 就siteLinkId
                Iterator<SupportingNe> iter = rack.getSupportingNe().iterator();

                while (iter.hasNext()) {
                    SupportingNe ne = iter.next();
                    if (ne.getNodeRef().getValue().equals(phyNodeId)) {
                        iter.remove();
                        break;
                    }
                }
                if (rack.getSupportingNe().isEmpty()) {
                    //这个Rack下已经没有NE了，直接删除
                    rackIter.remove();
                }
                break;
            }
        }
        siteNode = new NodeBuilder(siteNode).addAugmentation(Node1.class,
                new Node1Builder().setSite(new SiteBuilder(siteAttr)
                                .setSupportingRack(rackList)
                                .build())
                        .build()).build();

        return siteNode;
    }

    private void removeSupportingLink(Link removedSiteLink) {
        log.debug("remove siteLink related phy links");

        Site siteLinkAttr = removedSiteLink.getAugmentation(Link1.class).getSite();
        PhyNodeRemover phyNodeRemover = new PhyNodeRemover(changedObject);
        for (SupportingLink sl : removedSiteLink.getSupportingLink()) {
            //开始删除物理连接，
            new PhyLinkUtil(changedObject).removePhyLink(sl.getLinkRef().getValue(),
                    siteLinkAttr.getPlaneId());
        }
    }

    private void removeSiteLinkFromRiskGroup(Link removedSiteLink) {
        log.debug("checking site attribute for update risk group.");
        Site site = removedSiteLink.getAugmentation(Link1.class).getSite();
        String riskGroupName = site.getRiskGroupName();
        String planeName = site.getPlaneName();

        changedObject.changRiskGroup_del(riskGroupName, planeName, removedSiteLink.getLinkId());
    }

    public SiteLinkRemover setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
//        this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        return this;
    }
}
