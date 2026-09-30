/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.*;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import net.flex.dci.otn.controller.implement.common.repaire.RepaireOnOch;
import net.flex.dci.otn.controller.implement.common.utils.ApsProtectionState;
import net.flex.dci.otn.controller.implement.common.utils.BindingThirdLegScope;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Data
public class LinkImplementState {

    private final static SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

    public LinkImplementState setActionType(ImplActionType actionType) {
        this.actionType = actionType;

        return this;
    }

    public enum LinkType {
        None,
        ExLink,
        InLink,
        SiteLink,
        OchLink,
        Tunnel,
        /** 仅下发一条 OCH 保护腿的资源，不收敛任何业务 Link 状态。 */
        ProtectionLeg,
        PhyLink
    }

    protected ImplActionType actionType;
    protected LinkType linkType;
    protected String linkId;
    private String friendlyName;
    protected RouteInfo rInfo;
    //  private boolean isImplementAction;
    protected BiConsumer<Boolean, Throwable> onFinish;
    private final Set<String> ocmNodeIds = new HashSet<>();
    private final Map<String, String> retainedProtectionApsC = new LinkedHashMap<>();

    /**
     * 指定本次动作需要写入的 OCM 节点。该集合由调用方按目标腿 SiteLink 计算，避免按整条
     * OCH 路由扩大 OCM 下发范围。
     */
    public LinkImplementState setOcmNodeIds(Collection<String> nodeIds) {
        ocmNodeIds.clear();
        if (nodeIds != null) {
            ocmNodeIds.addAll(nodeIds);
        }
        return this;
    }

    /**
     * Supplies the retained APS XC and C-port scope for a 3->2 operation.
     * The empty default keeps every normal link flow unchanged.
     */
    public LinkImplementState setRetainedProtectionApsC(Map<String, String> apsCByXc) {
        retainedProtectionApsC.clear();
        if (apsCByXc != null) {
            retainedProtectionApsC.putAll(apsCByXc);
        }
        return this;
    }

    /**
     * impl/deImpl the assigned link.
     */
    public LinkImplementState(LinkType linkType, String linkId, String friendlyName, RouteInfo rInfo, BiConsumer<Boolean, Throwable> onFinish) {
        this.linkType = linkType;
        this.linkId = linkId;
        this.friendlyName = friendlyName;
        this.rInfo = rInfo;
        this.onFinish = onFinish;
    }

    /**
     * based on route find all tp, xc, il (grouping them with node)
     * each node is one thread, impl base on node steps
     */
    public void changeAs(ChangedObject changedObject, LifeCycleSevice lifeService) {
//    isImplementAction = actionType.equals(ActionType.Implement);

        removeImplementedWssLinks(changedObject);

        Map<String, PhysicalNode.ReadyResource> readyResourceMap = new HashMap<>();
        log.debug("very important step, build data for this action");
        Collection<Node> nodeList = extractChangedNode(changedObject, readyResourceMap);

        StepToe toe = new StepToe();
        for (Node node : nodeList) {
            toe.addToe(new ConfigNeSequence(changedObject, node, actionType, lifeService).prepare(readyResourceMap));
            //node info has updated in ConfigNeSequence
        }
        Step<StepRecord> step = new Step(toe);

        step.start(new SummaryLinkState(changedObject, linkType, linkId, rInfo, actionType, onFinish));
    }

    //opNode 需要把implState写为ing, 展现给UI，正在做事
    //cfgNode 读取与TP，板卡相关,XC, internalLink，准备写入网元
    protected Collection<Node> extractChangedNode(ChangedObject changedObject, Map<String, PhysicalNode.ReadyResource> readyResourceMap) {
        try {
            return extractChangedCfgNode(changedObject, readyResourceMap);
        } catch (CommonException e) {
            if (e.getMessage().contains(PhysicalNode.XC_ERROR)) {
                log.error("!ERROR ", e);

                if (linkType.equals(LinkType.Tunnel) && !rInfo.getLogicServerLinkIdList().isEmpty()) {
                    String ochLinkId = rInfo.getLogicServerLinkIdList().stream()
                            .filter(OchLinkIdNamingRule::isOchLink)
                            .findAny().orElse(null);
                    String result = new RepaireOnOch().start(ochLinkId); //repair XC

                    log.info("try to recover XC data {}", result);
                    changedObject.unsetAllPhyNode();

                    return extractChangedCfgNode(changedObject, readyResourceMap);
                }
            }
            throw e;
        }
    }
    /**
     * 基于Link 路由，构建需要下发的Node信息
     * 人Info中的tpIdList, xcIdList, linkIdList 都是包含了路由（所有层级）的信息
     *
     * @param changedObject
     * @param readyResourceMap
     * @return
     */
    private Collection<Node> extractChangedCfgNode(ChangedObject changedObject, Map<String, PhysicalNode.ReadyResource> readyResourceMap) {
//    改为单独API触发，不自动upload
//
//    if (actionType.equals(ImplActionType.Implement)) {
//      ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);
//      if (implConfig.isUploadNeParam()) {
//        rewriteOp2Cfg(rInfo);
//      }
//    }
        log.info("Start extractChangedCfgNode base on routing info {}", rInfo);

        Map<String, Node> cfgNodeMap = new HashMap<>();

        PhysicalNode physicalNode = new PhysicalNode(actionType);
        List<String> msg = new ArrayList<>();

        //normal impl, deimpl
        Iterator<String> phyLinkIterator = rInfo.getPhyLinkIdList().iterator();
        while (phyLinkIterator.hasNext()) {
            String linkId = phyLinkIterator.next();
            //构建网元内部连接，为了方便写入网元
            if (OchLinkIdNamingRule.isOchLink(linkId) ||
                    SiteLinkIdNamingRule.isSiteLink(linkId)) {
                continue;
            }

            if (SharedPhysicalLinkUsage.isSharedByOchLinks(linkId) && actionType.equals(ImplActionType.Deimplement)) {
                // WSSLink and OMS/MPO links can be reused by other OCH links. OSLink is
                // owned by the current OCH path, so it is intentionally not handled here.
                if (hasOtherNonAllocateOchUsingLink(linkId)) {
                    // The outer implementor has already marked route resources as
                    // Deimplementing. This shared link is still used by another OCH, so
                    // keep it Implement and exclude it from this action's final summary.
                    restoreSkippedSharedPhyLink(changedObject, linkId);
                    phyLinkIterator.remove();
                    rInfo.getTpIdList().remove(PhysicalLinkIdNamingRule.getTpAId(linkId));
                    rInfo.getTpIdList().remove(PhysicalLinkIdNamingRule.getTpZId(linkId));
                    continue;
                }
            }

            try {
                String anodeId = PhysicalLinkIdNamingRule.getNodeAId(linkId);
                //osLink 有可能两端一个是光，一个是电， 同时读取后，在光电分离的情况下会导致老数据覆盖新数据的可能
                if (rInfo.getNodeIdList().contains(anodeId)) {
                    Node orgCfgNode = changedObject.getChangedPhyNode(anodeId);
                    physicalNode.constructCfgNodeByLink(orgCfgNode, cfgNodeMap, linkId);
                }

                String znodeId = PhysicalLinkIdNamingRule.getNodeZId(linkId);
                if (rInfo.getNodeIdList().contains(znodeId)) {
                    Node orgCfgNode = changedObject.getChangedPhyNode(znodeId);
                    physicalNode.constructCfgNodeByLink(orgCfgNode, cfgNodeMap, linkId);
                }
            } catch (Exception e) {
                log.error("cannot extract on linkId {}", linkId, e);
            }
        }

        for (String tpId : rInfo.getTpIdList()) {
            //在两条腿 加 第3条腿的 情况下，C 口/L口 在最开始就已经从rInfo中删除， 所以下面逻辑不用改
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            Node orgCfgNode = changedObject.getChangedPhyNode(nodeId);
            physicalNode.constructCfgNodeByTp(orgCfgNode, cfgNodeMap, tpId, true);
        }

        for (String eqId : rInfo.getEqIdList()) {
            String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
            Node orgCfgNode = changedObject.getChangedPhyNode(nodeId);
            physicalNode.constructCfgNodeByEq(orgCfgNode, cfgNodeMap, eqId);
        }
        msg.add(String.format("EQs: %s", rInfo.getEqIdList()));

        // Older allocated 1:2 OCHs may predate APS-level member properties.
        // Complete only APS XCs owned by this action before constructing the device payload.
        completeTwoLegProtectionApsProperties(changedObject);
        for (String xcId : rInfo.getXcIdList()) {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            Node orgCfgNode = changedObject.getChangedPhyNode(nodeId);
            physicalNode.constructCfgNodeByXc(orgCfgNode, cfgNodeMap, xcId, null);
        }
        msg.add(String.format("XCs: %s", rInfo.getXcIdList()));

        if (LinkType.ProtectionLeg.equals(linkType) && !retainedProtectionApsC.isEmpty()) {
            // The retained APS uses an Implement payload; mixing it into the removed-leg
            // Deimplement payload would remove an in-service XC.
            PhysicalNode retainedApsNode = new PhysicalNode(ImplActionType.Implement);
            retainedProtectionApsC.forEach((apsXcId, cTpId) -> {
                Node tpNode = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(cTpId));
                retainedApsNode.constructCfgNodeByTp(tpNode, cfgNodeMap, cTpId, false);

                Node xcNode = changedObject.getChangedPhyNode(PhysicalXcIdNamingRule.getNodeId(apsXcId));
                retainedApsNode.constructCfgNodeByXc(
                        updateTwoLegApsMemberState(xcNode, apsXcId),
                        cfgNodeMap, apsXcId, null);
            });
            readyResourceMap.putAll(retainedApsNode.getReadyResource());
        }

        // OCM 并不一定对应 RouteInfo 中的一条 XC（例如 ILA 节点）。保护腿撤销将 OCM
        // 节点单独传入，确保只写本腿复用段上的 OCM 数据。
        for (String nodeId : ocmNodeIds) {
            Node orgCfgNode = changedObject.getChangedPhyNode(nodeId);
            if (orgCfgNode != null) {
                physicalNode.constructCfgNodeOcm(orgCfgNode, cfgNodeMap);
            }
        }

        boolean isBindingThirdLeg = false;
        if (actionType.equals(ImplActionType.Implement)) {
            List<String> specTpIds = new ArrayList<>();

            if (linkType.equals(LinkType.Tunnel)) {
                Tunnel tunnel = changedObject.getChangedTunnel(linkId);
                Link ochLink = changedObject.getChangedOchLink(tunnel.getSupportingLink().get(0).getLinkRef().getValue());
                Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
                String ochLinkId = ochLink.getLinkId().getValue();

                log.debug("the linkType is Tunnel, and ochLink implement status is {}, {}", ochLinkAttr.getImplementState(), ochLinkId);

                // Non-ASE links open ports directly; ASE dummy-OCH links leave them for AdjustTunnel.
                boolean isAseBased = aseBasedOchLink(ochLink);  //this is ASE 假波based
                isBindingThirdLeg = BindingThirdLegScope.isBindingThirdLeg(ochLinkAttr);
                log.debug("should isAseBased {} and isBinding3rdLeg {}",
                        isAseBased, isBindingThirdLeg);

                boolean ochLinkImplemented = ochLinkAttr.getImplementState().equals(ImplementState.Implement);
                if (!isBindingThirdLeg && isAseBased) {
                    if (!ochLinkImplemented) {
                        specTpIds = extractApsXCRelatedTP(ochLinkAttr.getExplictRoute().getRoute());
                        if (OchLinkIdNamingRule.isOchBusinessLink(ochLinkId)) {
                            //光电分离的时候这个地方需要判断， 电层的L 口不能在这里设置

                            specTpIds.addAll(RouteExtractor.fetchOchLPort(changedObject, rInfo));
                        }
                    }

                    // ASE SiteLinks open service endpoint ports during adjust, including the first tunnel.
                    String tsTpId = tunnel.getSourceTp().get(0).getTpRef().getValue();
                    String tdTpId = tunnel.getDestinationTp().get(0).getTpRef().getValue();

                    if (rInfo.getTpIdList().stream().anyMatch(x -> x.equals(tsTpId))) {
                        specTpIds.add(tsTpId);
                    }
                    if (rInfo.getTpIdList().stream().anyMatch(x -> x.equals(tdTpId))) {
                        specTpIds.add(tdTpId);
                    }
                }
            }

            if (!specTpIds.isEmpty()) {
                log.debug("specTps need adminDown: {}", specTpIds);
                msg.add(String.format("TPs: %s", rInfo.getTpIdList()));

                specTpIds.forEach(tpId -> {
                    String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
                    Node orgCfgNode = changedObject.getChangedPhyNode(nodeId);
                    physicalNode.constructCfgNodeByTp(orgCfgNode, cfgNodeMap, tpId, false);
                });
            }

        }

        // ILA node has included in previous checking (tp, link, xc), but it requires OCM.
        boolean hasWssXcInThisAction = wssXCincluded(cfgNodeMap, rInfo.getXcIdList());
        // Only binding-3rd-leg routes are clipped to new siteLinks; normal implement keeps the original OCM range.
        BindingThirdLegScope ocmScope = isBindingThirdLeg
                ? BindingThirdLegScope.fromSiteLinks(changedObject, getRouteSiteLinkIds())
                : BindingThirdLegScope.empty();
        for (String nodeId : rInfo.getNodeIdList()) {
            if (!ocmScope.allowNode(nodeId)) {
                continue;
            }
            Node orgCfgNode = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = orgCfgNode.getAugmentation(Node1.class).getPhysical();
            if (nodeAttr.getNodeType().equals(NodeType.OD) && hasWssXcInThisAction) {
                physicalNode.constructCfgNodeOcm(orgCfgNode, cfgNodeMap);
            }
        }

        if (linkType.equals(LinkType.Tunnel)) {
            //现场发现有DGE 网元没有下发XC， 这个是一个严重错误，需要抛异常
            checkingDEGXC(changedObject, rInfo.getNodeIdList(), rInfo.getEqIdList(), rInfo.getXcIdList());
        }

        log.debug("following objects wll be set: \n{}", String.join("\n", msg));
        readyResourceMap.putAll(physicalNode.getReadyResource());
        return cfgNodeMap.values();
    }

    private Node updateTwoLegApsMemberState(Node node, String apsXcId) {
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> xcs = physical.getCrossConnections().stream()
                .map(xc -> xc.getCrossConnectionId().getValue().equals(apsXcId)
                        ? ApsProtectionState.restoreTwoLegMemberProperties(xc)
                        : xc)
                .collect(Collectors.toList());
        return new NodeBuilder(node).addAugmentation(Node1.class,
                new Node1Builder().setPhysical(new PhysicalBuilder(physical)
                        .setCrossConnections(xcs).build()).build()).build();
    }

    private void completeTwoLegProtectionApsProperties(ChangedObject changedObject) {
        if (!ImplActionType.Implement.equals(actionType) || !LinkType.Tunnel.equals(linkType)) {
            return;
        }

        Tunnel tunnel = changedObject.getChangedTunnel(linkId);
        Link ochLink = changedObject.getChangedOchLink(
                tunnel.getSupportingLink().get(0).getLinkRef().getValue());
        Och och = ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        Route route = och.getExplictRoute().getRoute().get(0);
        if (!ProtectionBidir1To2.class.equals(och.getProtectionType())
                || (route.getThird() != null && !route.getThird().isEmpty())
                || route.getPrimary() == null || route.getPrimary().getCrossConnections() == null) {
            return;
        }

        // A later tunnel on the same OCH does not own the OCH route and must not update its APS XC.
        Set<String> apsXcIds = route.getPrimary().getCrossConnections().stream()
                .filter(xc -> xc.getAps() != null)
                .map(xc -> xc.getCrossConnectionId().getValue())
                .filter(rInfo.getXcIdList()::contains)
                .collect(Collectors.toSet());
        for (String apsXcId : apsXcIds) {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(apsXcId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical physical = node.getAugmentation(Node1.class).getPhysical();
            if (physical.getCrossConnections() == null) {
                log.debug("skip completing APS member properties because node {} has no XC", nodeId);
                continue;
            }
            List<CrossConnections> updatedXcs = physical.getCrossConnections().stream()
                    .map(xc -> apsXcId.equals(xc.getCrossConnectionId().getValue())
                            ? completeTwoLegApsMemberProperties(xc) : xc)
                    .collect(Collectors.toList());
            log.debug("complete missing two-leg APS member properties on XC {}", apsXcId);
            changedObject.addChangedPhyNode(new NodeBuilder(node)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(physical)
                                    .setCrossConnections(updatedXcs)
                                    .build())
                            .build())
                    .build());
        }
    }

    private CrossConnections completeTwoLegApsMemberProperties(CrossConnections xc) {
        if (xc.getAps() == null) {
            return xc;
        }

        Properties properties = xc.getAps().getProperties();
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId> memberTps =
                new ArrayList<>();
        if (xc.getSourceTp() != null) {
            xc.getSourceTp().forEach(tp -> memberTps.add(tp.getTpRef()));
        }
        if (xc.getDestinationTp() != null) {
            xc.getDestinationTp().forEach(tp -> memberTps.add(tp.getTpRef()));
        }

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId tpId : memberTps) {
            String tpValue = tpId.getValue();
            String member = tpValue.substring(tpValue.lastIndexOf('-') + 1);
            String expected = null;
            if (member.endsWith("A") || member.endsWith("B")) {
                expected = "true";
            } else if (member.endsWith("C")) {
                expected = "false";
            }
            // Adapter maps APS member state only from the device-model ".enabled" suffix.
            String propertyName = member + ".enabled";
            if (expected != null && PropertyTool.getValue(properties, propertyName) == null) {
                // Preserve explicit values; this path only repairs legacy objects with missing APS properties.
                properties = PropertyTool.addProperty(properties, propertyName, expected);
            }
        }
        return new CrossConnectionsBuilder(xc)
                .setAps(new ApsBuilder(xc.getAps()).setProperties(properties).build())
                .build();
    }

    private List<String> getRouteSiteLinkIds() {
        if (rInfo.getLogicServerLinkIdList() == null) {
            return Collections.emptyList();
        }

        return rInfo.getLogicServerLinkIdList().stream()
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .collect(Collectors.toList());
    }

    private boolean wssXCincluded(Map<String, Node> cfgNodeMap, List<String> xcIdList) {
        Set<String> targetCenterFrequencies = xcIdList.stream()
                .map(this::extractXcCenterFrequency)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (targetCenterFrequencies.isEmpty()) {
            return false;
        }

        return cfgNodeMap.values().stream().anyMatch(node -> {
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            if (nodeAttr.getCrossConnections() == null) {
                return false;
            }

            // WSS XC may be recovered from OP into cfgNodeMap during deimplement.
            // Match by the center frequency encoded at the end of XC ID, so the OCM
            // decision follows the current action without depending on an exact cfg XC copy.
            return nodeAttr.getCrossConnections().stream()
                    .anyMatch(xc -> xc.getWssChannel() != null
                            && xc.getCrossConnectionId() != null
                            && targetCenterFrequencies.contains(
                            extractXcCenterFrequency(xc.getCrossConnectionId().getValue())));
        });
    }

    private String extractXcCenterFrequency(String xcId) {
        if (xcId == null) {
            return null;
        }
        int index = xcId.lastIndexOf('/');
        if (index < 0 || index == xcId.length() - 1) {
            return null;
        }

        String centerFrequency = xcId.substring(index + 1);
        // Only WSS/media-channel XCs encode numeric center frequency at the end.
        // Other XCs, such as APS paths ending with /A, /B, /C, must not trigger OCM.
        if (!centerFrequency.matches("\\d+")) {
            return null;
        }
        return centerFrequency;
    }

    private List<String> fetchOchLPort(ChangedObject changedObject) {
        log.debug("checking if need to disable OCH related LINE tp {}", rInfo.getNodeIdList());
        List<Node> nodes = rInfo.getNodeIdList().stream()
                .map(changedObject::getChangedPhyNode)
                .filter(node -> {
                    Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
                    return NodeType.TD.equals(nodeAttr.getNodeType());
                })
                .collect(Collectors.toList());


        //对于电中继，中间的电卡L口需要开启，同时光功率设置
        Set<String> tpList = nodes.stream()
                .map(node -> node.getNodeId().getValue())
                .flatMap(nodeId -> rInfo.getTpIdList().stream()
                        .filter(tpId -> tpId.contains(nodeId)))
                .collect(Collectors.toSet());

        //filter out protection related
        tpList = tpList.stream().filter(tpId-> {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            Node node = changedObject.getChangedPhyNode(nodeId);

            TerminationPoint tp = node.getTerminationPoint().stream()
                    .filter(x -> x.getTpId().getValue().equals(tpId)).findAny().orElse(null);

            if (tp == null) {
                log.error("Cannot find required TP in node {}", tpId);
                return false;
            }

            if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine() != null) {
                return true;
            }
            return false;
        }).collect(Collectors.toSet());

        log.debug("OchLink related TPs {}", tpList);
        return new ArrayList<>(tpList);
    }

    private void checkingDEGXC(ChangedObject changedObject, List<String> nodeIdList, List<String> eqIdList, List<String> xcIdList) {
        for (String nodeId: nodeIdList) {
            String matchedEqId = eqIdList.stream().filter(eqId->eqId.contains(nodeId)).findAny().orElse(null);
            if (matchedEqId == null || matchedEqId.contains("TRANSCEIVER")) {
                continue;
            }
            Node node = changedObject.getChangedPhyNode(nodeId);
            List<Equipments> eqList = node.getAugmentation(Node1.class).getPhysical().getEquipments();

            Equipments eq = eqList.stream().filter(x -> x.getEquipmentId().equals(matchedEqId)).findAny().orElse(null);
            if (eq == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "hasn't find required eq with id " + matchedEqId);
            }

            if (eq.getEquipType().equals(EquipType.DGE)) {
                String matchedXcId = xcIdList.stream().filter(xcId -> xcId.contains(matchedEqId)).findAny().orElse(null);
                if (matchedXcId == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "the DEG hasn't XC related in route" + matchedEqId);
                }

                List<CrossConnections> xcList = node.getAugmentation(Node1.class).getPhysical().getCrossConnections();
                CrossConnections xc = xcList.stream().filter(x -> x.getCrossConnectionId().getValue().equals(matchedXcId)).findAny().orElse(null);

                if (xc == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "the DEG has XC in ochLink route, but not existed in NE " + matchedEqId);
                }
            }
        }
    }

    /**
     * return false means the port should be disabled
     *
     * @param cache
     * @param shouldAdminDown
     * @return
     */
    private boolean shouldAdminEnableOnOtuLinePortAfter3rdLegBinding(ChangedObject cache, boolean shouldAdminDown) {
        if (linkType.equals(LinkType.Tunnel)) {
            Tunnel tunnel = cache.getChangedTunnel(linkId);
            String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
            Link ochLink = cache.getChangedOchLink(ochLinkId);
            Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
            boolean isBind3rdLink = PropertyTool.existProperty(ochLinkAttr.getProperties(), "binding3rdLeg");
            if (isBind3rdLink && ochLinkAttr.getImplementState().equals(ImplementState.Implement)) {
                return false;
            }
        }
        return shouldAdminDown;
    }

    /**
     *  只要是ASE 在implement的时候adminDown, 然后通过业务调测adminUp
     * @return ture, means the otuLine port should be disabled
     */
    private boolean aseBasedOchLink(Link ochLink) {
        ChangedObject cache = new ChangedObject();

        if (ochLink != null) {
            List<String> siteLinkIds = ochLink.getSupportingLink()
                    .stream().filter(x -> SiteLinkIdNamingRule.isSiteLink(x.getLinkRef().getValue()))
                    .map(x -> x.getLinkRef().getValue())
                    .collect(Collectors.toList());

            log.debug("the ochlink is based on sitelinks");
            if (! siteLinkIds.isEmpty()) {
                //默认tunnel 下层都是一样的， 不能C， C+L 混用
                for (String siteLinkId : siteLinkIds) {
                    Link siteLink = cache.getChangedSiteLink(siteLinkId);
                    Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
                    if (siteLinkAttr.getDummyLink() == null) {
                        log.debug("the sitelink without dummy och links");
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private List<String> extractApsXCRelatedTP(List<Route> route) {
        Set<String> specTpIds = new LinkedHashSet<>();

        List<CrossConnectionAttributes> xcList = RouteExtractor.extractorXc(route);
        for (CrossConnectionAttributes xc : xcList) {
            if (xc.getAps() != null) {
                log.debug("find aps XC");
                // APS protection1to2 has Sig/A/B/C ports. Even if one leg is not in the active route,
                // all APS endpoint ports stay adminDown during ASE implement and are opened by AdjustTunnel.
                xc.getSourceTp().forEach(s->{
                    String sTpId = s.getTpRef().getValue();
                    specTpIds.add(sTpId);
                });
                xc.getDestinationTp().forEach(s->{
                    String sTpId = s.getTpRef().getValue();
                    specTpIds.add(sTpId);
                });
            }
        }
        return new ArrayList<>(specTpIds);
    }

    private boolean hasOtherNonAllocateOchUsingLink(String linkId) {
        OchLinkDao ochDao = SpringBeanFinder.getBean(OchLinkDao.class);
        String ochLinkId = currentOchLinkId();
        long otherNonAllocateOchCount = ochLinkId == null
                ? ochDao.countBySupportingLinkRefAndNotAllocate(linkId)
                : ochDao.countBySupportingLinkRefAndNotAllocateExcludeOchLink(linkId, ochLinkId);
        return SharedPhysicalLinkUsage.shouldSkipOnDeimplement(linkId,
                Math.max(otherNonAllocateOchCount, 0));
    }

    private void restoreSkippedSharedPhyLink(ChangedObject changedObject, String linkId) {
        restorePhyLinkAsImplement(changedObject, linkId);
        restorePhyNodeAsImplement(changedObject, PhysicalLinkIdNamingRule.getNodeAId(linkId));
        restorePhyNodeAsImplement(changedObject, PhysicalLinkIdNamingRule.getNodeZId(linkId));
    }

    private void restorePhyLinkAsImplement(ChangedObject changedObject, String linkId) {
        Link phyLink = changedObject.getChangedPhyLink(linkId);
        if (phyLink == null) {
            return;
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLinkAug =
                phyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
        if (phyLinkAug == null || phyLinkAug.getPhysical() == null) {
            return;
        }

        Link restored = new LinkBuilder(phyLink)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder(phyLinkAug.getPhysical())
                                        .setImplementState(ImplementState.Implement)
                                        .setAdminState(AdminStatus.Up)
                                        .build())
                                .build())
                .build();
        changedObject.unsetPhyLink(linkId);
        changedObject.addChangedPhyLink(restored);
        log.debug("restore skipped shared phyLink as Implement: {}", linkId);
    }

    private void restorePhyNodeAsImplement(ChangedObject changedObject, String nodeId) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        if (node == null || node.getAugmentation(Node1.class) == null
                || node.getAugmentation(Node1.class).getPhysical() == null) {
            return;
        }

        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Node restored = new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setImplementState(ImplementState.Implement)
                                .setAdminState(AdminStatus.Up)
                                .build())
                        .build())
                .build();
        changedObject.unsetPhyNode(nodeId);
        changedObject.addChangedPhyNode(restored);
    }

    private String currentOchLinkId() {
        // OCH、保护腿和 Tunnel 动作都带有当前业务 OCH，用它排除自身后再计算共享 WSS/OMS 的占用。
        // SiteLink/PhyLink 动作没有这个上下文。
        if (linkType.equals(LinkType.OchLink) || linkType.equals(LinkType.ProtectionLeg)) {
            return linkId;
        }
        if (!linkType.equals(LinkType.Tunnel)) {
            return null;
        }
        if (rInfo.getLogicServerLinkIdList() == null) {
            return null;
        }
        return rInfo.getLogicServerLinkIdList().stream()
                .filter(OchLinkIdNamingRule::isOchLink)
                .findAny()
                .orElse(null);
    }

    private void removeImplementedWssLinks(ChangedObject changedObject) {
        if (!actionType.equals(ImplActionType.Implement)) {
            return;
        }

        // wssLink appears in business OCH routes and is shared by multiple OCH links.
        // Dummy OCH is local to a SiteLink route and should not contain wssLink, so
        // this guard only affects real business OCH reuse. wssChannel cross-connections
        // are separate OCH resources and are not filtered here. Once the wssLink
        // internal-link has been implemented, later OCHs using the same wssLink should
        // not download or update that shared wssLink again.
        rInfo.getPhyLinkIdList().removeIf(linkId -> {
            boolean implemented = isImplementedWssLink(changedObject, linkId);
            if (implemented) {
                log.debug("remove implemented WSS link from this action: {}", linkId);
            }
            return implemented;
        });
    }

    private boolean isImplementedWssLink(ChangedObject changedObject, String linkId) {
        if (!PhysicalLinkIdNamingRule.isWssLink(linkId)
                || !actionType.equals(ImplActionType.Implement)) {
            return false;
        }

        Link phyLink = changedObject.getChangedPhyLink(linkId);
        if (phyLink == null) {
            return false;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLinkAttr =
                phyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
        if (phyLinkAttr == null || phyLinkAttr.getPhysical() == null) {
            return false;
        }

        return ImplementState.Implement.equals(phyLinkAttr.getPhysical().getImplementState());
    }


    private boolean isVirtualSiteLink(Link ochLink) {
        SupportingLink sl = ochLink.getSupportingLink().stream()
                .filter(x -> SiteLinkIdNamingRule.isSiteLink(x.getLinkRef().getValue()))
                .findAny().orElseThrow(() -> new RuntimeException("cannot find out siteLink in supporting link of ochLink " + ochLink.getLinkId().getValue()));

        Link siteLink = siteLinkDao.getSiteLinkById(sl.getLinkRef().getValue());
        return siteLink.getAugmentation(Link1.class).getSite().getVendorName().equalsIgnoreCase("VIRTUAL");
    }

    private boolean hasTD(ChangedObject changedObject) {
       return rInfo.getNodeIdList().stream().anyMatch(nodeId->{
           Node node = changedObject.getChangedPhyNode(nodeId);
           return node.getAugmentation(Node1.class).getPhysical().getNodeType().equals(NodeType.TD);
       });
    }

}
