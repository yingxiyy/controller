/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.allocate.link.och.OchLinkFromTunnelRemover;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeMerge;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TunnelRemover {

    protected TaskInfoMessage taskInfoMessage;

    private final static OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
    private final static PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private final static TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);

    //==================key value for doIt tunnel
    private ChangedObject changedObject;
    private MultipleTransaction mongoTransaction;


    //===================

    public TunnelRemover() {
        mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
    }

    /**
     * UI 调用入口，由于UI可以一次选择几百上千条，调用批量删除的时候， UI降调用若干次删除restful命令，后台就是几百上千个线程，
     * 如果全部单独处理后台忙死。所以UI过来的删除命令，丢入队列排队处理
     *
     * 删除tunnel时会修改 OCH link 如果OCH link上没有tunnel了（当前tunnel时最后一条）， 删除ochLink 锁资源在队列那边
     *
     * @param removeData
     * @return
     */
    public RpcResultType asyncRemove(RemoveTunnelInput removeData) {
        check(removeData);

        RemovedTunnelQueue queue = SpringBeanFinder.getBean(RemovedTunnelQueue.class);
        queue.add(taskInfoMessage, removeData);

        return RpcResultType.Success;
    }

    /**
     * 这个调用来源于removeSiteLink, 资源已经在remoteSiteLink那里上锁了
     *
     * @param taskInfoMessage
     * @param tunnel
     */
    public void syncRemove(TaskInfoMessage taskInfoMessage, Tunnel tunnel, Boolean force, Boolean forceDb) {
        startRemove(taskInfoMessage, tunnel, force, forceDb);
    }

    /**
     * 删除了原有的锁机制，是因为如果这个方法是removeSiteLink（syncRemove）那边引起的，所有资源在siteLink那边已经加锁
     * 如果是UI调用removeTunnel引起的（asyncRemove）, 资源锁发生在removeTunnelQueue
     *
     * @param taskInfoMessage
     * @param removedTunnel
     * @param force
     * @param forceDb
     */
    public void startRemove(TaskInfoMessage taskInfoMessage, Tunnel removedTunnel, Boolean force, Boolean forceDb) {
        this.taskInfoMessage = taskInfoMessage;

        String tunnelId = removedTunnel.getTunnelId().getValue();
        log.debug("remove point2point tunnel {}, isForceDB {}", removedTunnel.getTunnelId().getValue(), forceDb);

        changedObject = new ChangedObject();
        checkIpBinding(removedTunnel); //这里的再次检测是必要的，批量删除的时候，前面的检测实际没有作用
        try {
            if (forceDb) {
                updateTopologyData(removedTunnel);
            } else {
                checkTunnelStatus(removedTunnel);
                updateTopologyData(removedTunnel);
            }
            mongoTransaction.save(changedObject);

            logMessage(removedTunnel, null);

//      List<String> removedNodeIdList = cleanPhyNeResourceOnLink(removedTunnel);
//      updateSiteNode(removedNodeIdList);
            log.info("tunnel remove done {}", tunnelId);
        } catch (CommonException e) {
            log.error("remove tunnel fail", e);
            logMessage(removedTunnel, e.getMessage());

            throw e;
        } catch (Exception e) {
            log.error("remove tunnel fail", e);
            logMessage(removedTunnel, e.toString());

            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "remove tunnel error" + e.toString(), e);
        }
    }

    private void checkTunnelStatus(Tunnel removedTunnel) {
        if (removedTunnel.getImplementState() != ImplementState.Allocate) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "The tunnel still in working " + removedTunnel.getFriendlyName());
        }
    }

    /**
     * 由于remove tunnel 可能会删除Transceiver，或者card，这些信息需要更新到OP树 因为UI看到的数据就是OP树的（如果有的话）,
     * 一个equip(包括transceiver)被Link使用了由属性"used-in-link" = true 表达 数据合法性需要检查
     * 板卡类型相同，used-in-link=false;
     *
     *
     * 对于删除来说，deImpl的时候对应的XC、internalLink等网元上的资源已经删除，而板卡是硬件资源，无法在删除， 上面一句话又错了 :-)
     * 引入reuse概念后，如果网元由正确IP地址（被adapter管理了，且OP数据库有内容，UI看到的网元信息都是OP数据库的） 在创建时，config树添加的内容也会添加到op树。
     * （没有impl，不是网元上的数据） 那么在删除是，这些数据也需要移除
     *
     *
     * 这里的merge实际上只是修改used-in-link属性 （为了方便这个修改移到删除资源程序， markEmpty)
     *
     * @param removedTunnel
     */
    private void merge2OpDB(Tunnel removedTunnel) {
        Set<String> nodeIdSet = new HashSet<>();
        List<String> tpIdList = TunnelUtil.getTunnelTp(removedTunnel);
        for (String tpId : tpIdList) {
            nodeIdSet.add(PhysicalTpIdNamingRule.getNodeId(tpId));
        }

        for (String nodeId : nodeIdSet) {
            Node opNode = changedObject.getChangedPhyOpNode(nodeId);
            if (opNode != null) {
                Node cfgNode = changedObject.getChangedPhyNode(nodeId);
                Node newOpNode = new PhyNodeMerge(cfgNode, opNode).del();
                changedObject.addChangedPhyOpNode(newOpNode);
            }
        }
    }


    public void logMessage(Tunnel removedTunnel, String errorMessage) {
        String msg = String.format("remove tunnel %s ", removedTunnel.getFriendlyName());

        String extMsg;
        boolean isOk = false;
        if (errorMessage == null) {
            //成功
            isOk = true;
            extMsg = "successfully.";
        } else {
            extMsg = String.format("fail. (%s)", errorMessage);
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
            taskInfoMessage.setEndTime(System.currentTimeMillis());
            TaskInfoMessager.sendMessage(taskInfoMessage);
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("remove tunnel")
                        .message(msg + extMsg)
                        .error(!isOk)
                        .build());
    }

    private void updateTopologyData(Tunnel tunnel) {
        removeTunnel(tunnel);

        for (SupportingLink sl : tunnel.getSupportingLink()) {
            String ochLinkId = sl.getLinkRef().getValue();
            if (!OchLinkIdNamingRule.isOchLink(ochLinkId)) {
                continue;
            }

            CrossConnections xc = tunnel.getExplictRoute().getRoute().get(0).getPrimary()
                    .getCrossConnections().get(0);
            new OchLinkFromTunnelRemover(changedObject, tunnel, ochLinkId)
                    .update(TunnelUtil.getOdujType(tunnel.getSignalRate()),
                            TunnelConstructor.getOduSlot(xc));
        }
    }

    private void removeTunnel(Tunnel tunnel) {
        // tunnel occupies C port's transceiver only

        List<String> tpIdList = TunnelUtil.getTunnelTp(tunnel);
        for (String tpId : tpIdList) {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            if (node != null) {
                node = PhyNodeUtil.removeOTTransceiver(node, tpId);
                changedObject.addChangedPhyNode(node);
            }

        }
        changedObject.addRemovedTunnel(tunnel);
    }

    private Tunnel check(RemoveTunnelInput removeData) throws CommonException {
        Tunnel tunnel = tunnelDao.getTunnelById(removeData.getTunnelId());
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "required tunnel isn't existed");
        }
        if (!removeData.isForceDb()) {
            if (!tunnel.getImplementState().equals(ImplementState.Allocate)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "required tunnel is still working");
            }

            //if this is the latest tunnel over L port, here will remove the och link together,
            //key question here is: I want to check if this is the latest phylink outside the electric node,
            //I want to remove the electric node,
            // ****thus I want to check the IP binding on the NE
            checkIpBinding(tunnel);
        }
        return tunnel;
    }

    private void checkIpBinding(Tunnel tunnel) {
        List<String> ochLinkIdList = tunnel.getSupportingLink().stream()
                .map(sl->sl.getLinkRef().getValue())
                .collect(Collectors.toList());

        ochLinkIdList.forEach(ochLinkId->{
            Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
            if (ochLink == null) {
                log.error("cannot find ochLink {}", ochLinkId);
                return;
            }

            List<SupportedTunnel> tunnelIdList = ochLink.getAugmentation(Link1.class).getSupportedTunnel();
            if (tunnelIdList.size() > 1) {
                log.warn("still has other tunnel");
                return;
            }
            String srcTp = ochLink.getSource().getSourceTp().getValue();
            String dstTp = ochLink.getDestination().getDestTp().getValue();
            String srcNode = PhysicalTpIdNamingRule.getNodeId(srcTp);
            String dstNode = PhysicalTpIdNamingRule.getNodeId(dstTp);

            Set<String> tdNodeIds = new HashSet<>(fetchAllTDNodeIds(ochLink.getSupportingLink()));
            tdNodeIds.add(srcNode);
            tdNodeIds.add(dstNode);

            // Each TD node must be checked independently. A shared A/Z node must not skip
            // an exclusive REG node that will be removed together with this OCH link.
            tdNodeIds.forEach(this::isLastoneAndIPisEmpty);
        });
    }

    //算法思路是： os-links 都是单波link, 也就是电层L口 出来的link, node 上应该和OCH 的A/Z 不一样的就是REG node。
    //但是
    private List<String> fetchAllTDNodeIds(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink> supportingLink) {
        Set<String> tdNodeIds = new HashSet<>();

        Set<String> odNodeIds = new HashSet<>();

        supportingLink.forEach(sl->{
            String linkId = sl.getLinkRef().getValue();
            if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                odNodeIds.add(SiteLinkIdNamingRule.getNodeA(linkId));
                odNodeIds.add(SiteLinkIdNamingRule.getNodeZ(linkId));
            }
        });

        supportingLink.forEach(sl->{
            String linkId = sl.getLinkRef().getValue();
            if (PhysicalLinkIdNamingRule.isOsLink(linkId)) {
                String nodeA = PhysicalLinkIdNamingRule.getNodeAId(linkId);
                String nodeZ = PhysicalLinkIdNamingRule.getNodeZId(linkId);
                if (!odNodeIds.contains(nodeA)) {
                    tdNodeIds.add(nodeA);
                }
                if (!odNodeIds.contains(nodeZ)) {
                    tdNodeIds.add(nodeZ);
                }
            }
        });
        return new ArrayList<>(tdNodeIds);
    }

    //算法思路是： links 中的点都必然出现在nodes中， 如果没有，那么就是电中继网元。
    //因为nodes 是基于复用段推导出来的
    private List<String> fetchRegenNodes(List<Node> nodes, List<Link> links) {
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

    private boolean isLastoneAndIPisEmpty(String nodeId) {
        Long ochLinkCount = ochLinkDao.countWithNode(nodeId);
        log.debug("node {} is referenced by {} OCH links before tunnel removal", nodeId, ochLinkCount);
        if (ochLinkCount == 1) {
            checkIpEmpty(nodeId);
            return true;
        }
        return false;
    }

    private void checkMonitoring(String nodeId) {
        log.info("checkMonitoring {}", nodeId);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (node == null) {
            return;
        }
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getSupervisionStatus().equals(SupervisionStatusType.Monitoring)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "please remove supervision on Node at first: " + nodeAttr.getFriendlyName());
        }
    }

    private void checkIpEmpty(String nodeId) {
        log.info("checkIpEmpty {}", nodeId);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (node == null) {
            return;
        }
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getIp() != null) {
            log.info("notify customer remove the IP, because this is the latest tunnel over this device {}", node.getNodeId());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "please remove IP address on Node at first: " + nodeAttr.getFriendlyName());
        }
    }

    public TunnelRemover setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
//    this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        return this;
    }

    public List<String> getNeedMerged() {
        return new ArrayList<>(changedObject.getChangedPhyNodeList().keySet());
    }

}
