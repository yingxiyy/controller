package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.PickedOtResource;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.PickedOtTps;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SERVICETYPE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OtReusedStrategy {

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private NodeUtils nodeUtils;

    @Autowired
    private TunnelUtils tunnelUtils;

    @Autowired
    private JsonOutputer jsonOutputer;

    @Autowired
    private NEInfoConfig neInfoConfig;
    @Autowired
    private OtXcService otXcService;

    @Value("${server.yangModel}")
    private String yangModel;

    @Autowired
    private TunnelDao tunnelDao;

    @Autowired
    private OchLinkDao ochLinkDao;

    public List<Node> getReusedNodePoolFromDb(String siteId, Class<? extends SignalProtocolType> lineSignalRate, String cardType, String vendorName, String vendorType, Boolean isReusedMixed,
            @NonNull String siteLinkId, String plane, SERVICETYPE servicetype)
            throws NeDesignerException {

        List<Node> configNodeList;
        //符合vendor信息,且unstuff的设备（这里的unstuff也包括盘满，但是L口还有间隙的情况）。
        if (isReusedMixed) {
            configNodeList = phyNodeDao.getFilteredNodes(siteId, vendorName, vendorType, plane);
        } else {
            //只过滤tpc的设备
            configNodeList = phyNodeDao.getFilteredNodes(siteId, vendorName, vendorType, NodeType.TD.name(), plane);
        }
        List<Node> nodeList = checkOP(configNodeList);
        return getReusedNodePool(lineSignalRate, cardType, vendorName, vendorType, nodeList, siteLinkId, servicetype);

    }

    /**
     * 电框创建原则：
     *
     * a。一个电框对应一个方向, 这里的方向就是指业务的AZ点。一个电框的业务列表里面只会有一个AZ方向。
     *
     * b。不带保护和带保护的业务不复用一个电框
     *
     * c。带保护的业务有以下任意情况，不复用电框
     *
     * 1}。板卡L口数量不一样
     *
     * 2)。板卡占用槽位数不一样
     *
     * 3)。保护方式不一样
     *
     * d. 同一个TPC node只能接相同的siteLink
     */
    public List<Node> getReusedPortNodePoolFromDbNewOch(String srcSiteId, String dstSiteId, String vendorName, List<String> cardTypes, List<String> portTypes, String plane, String riskGroupName,
            Set<String> excludeNodeIds, NeSubType neSubType, Class<? extends ProtectionType> protectionType, SiteLinkRoute siteLinkRoute, String cardType, SERVICETYPE serviceType)
            throws NeDesignerException {
        //过滤有空闲的，没创建OCH的L端口或者OP6端口的那些 config node
        List<Node> reusedNodesInDb = phyNodeDao.getReUsedNodes(srcSiteId, dstSiteId, vendorName, cardTypes, portTypes, plane, riskGroupName, excludeNodeIds, neSubType);
        if (!yangModel.equals(NeYangModel.ByteDance.getName())) {//只有字节需要
            return reusedNodesInDb;
        }
        return filterProtectTypeAndReusedRule(srcSiteId, dstSiteId, reusedNodesInDb, protectionType, siteLinkRoute, cardType, serviceType);
    }

    private List<Node> filterProtectTypeAndReusedRule(String srcSiteId, String destSiteId, List<Node> reusedNodesInDb, Class<? extends ProtectionType> protectionType, SiteLinkRoute siteLinkRoute,
            String cardType, SERVICETYPE serviceType) throws NeDesignerException {

        List<Node> result = new ArrayList<>();
        for (Node node : reusedNodesInDb) {
            List<Tunnel> nodeTunnels = tunnelDao.queryWithNode(node.getNodeId().getValue());
            if (nodeTunnels == null || nodeTunnels.isEmpty()) {
                result.add(node);//此node没业务，直接添加
                continue;
            }

            //1.方向一样
            Tunnel selectedTunel = nodeTunnels.get(0);
            String selectedTunnelId = selectedTunel.getTunnelId().getValue();

            if (!tunnelUtils.isSameDirection(srcSiteId, destSiteId, selectedTunnelId)) {
                continue;
            }

            // 2. 保护方式需要一样
            if (!selectedTunel.getProtectionType().equals(protectionType)) {
                continue;
            }

            Link ochLink = ochLinkDao.getOchLinkByLinkId(
                    selectedTunel.getSupportingLink().get(0).getLinkRef().getValue());
            Och ochAddr = ochLink.getAugmentation(Link1.class).getOch();
            NeInfo tpcNeInfo = neInfoConfig.getNeInfo(ochAddr.getVendorName(), ochAddr.getProductType(), NodeType.TD.name());
            if (!protectionType.equals(ProtectionUnprotected.class)) {
                String ochCardType = ochAddr.getCardType();
                if (!cardType.equals(ochCardType)) {
                    //3.1  保护的情况下，OT card必须槽位一样
                    Card otCardInfoOch = tpcNeInfo.getCardByCardType(ochCardType);
                    Card otCardInfo = tpcNeInfo.getCardByCardType(cardType);
                    if (otCardInfoOch.getWidth() != otCardInfo.getWidth()) {
                        continue;
                    }

                    //3.2  保护的情况下，OT card的L口数量必须一样
                    long ochOtLPortCount =NeInfoUtil.getLPortCount(otCardInfoOch, selectedTunel.getServiceType());
                    long OtLPortCount = NeInfoUtil.getLPortCount(otCardInfo, serviceType);
                    if (ochOtLPortCount != OtLPortCount) {
                        continue;
                    }

                }
            }

            //4.  同一个TPC node只能接相同的siteLink
            Set<String> siteLinkIds = new HashSet<>();
            siteLinkIds.add(siteLinkRoute.getPrimary().get(0));
            siteLinkIds.add(siteLinkRoute.getPrimary().get(siteLinkRoute.getPrimary().size() - 1));
            if (siteLinkRoute.getSecondary() != null && !siteLinkRoute.getSecondary().isEmpty()) {
                siteLinkIds.add(siteLinkRoute.getSecondary().get(0));
                siteLinkIds.add(siteLinkRoute
                        .getSecondary()
                        .get(siteLinkRoute.getSecondary().size() - 1));
            }
            if (siteLinkRoute.getThird() != null && !siteLinkRoute.getThird().isEmpty()) {
                siteLinkIds.add(siteLinkRoute.getThird().get(0));
                siteLinkIds.add(siteLinkRoute.getThird().get(siteLinkRoute.getThird().size() - 1));
            }
            Set<String> ochSupportingLinks = ochLink.getSupportingLink().stream().map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(Collectors.toSet());
            if (!ochSupportingLinks.containsAll(siteLinkIds)) {
                continue;
            }

            result.add(node);//满足reuse条件

        }

        return result;
    }

    /**
     * 同一个tpc node连接的siteLink必须一样。所以对于无保护的，只能是一条，有保护的，那么可连接数最多是3
     *
     * @param srcSiteLinkNodeIds
     * @param connectedSiteLinkNodeIds
     * @return
     */
    private Boolean isSiteLinkMatched(Set<String> srcSiteLinkNodeIds, Set<String> connectedSiteLinkNodeIds) {
        int deltaSize = Math.abs(srcSiteLinkNodeIds.size() - connectedSiteLinkNodeIds.size());
        long diffSize = srcSiteLinkNodeIds.stream().filter(e -> !connectedSiteLinkNodeIds.contains(e)).count();
        return (deltaSize == diffSize);
    }


    private String getSiteLinkNodeId(String siteLinkId, String siteId) {
        if (PhysicalLinkIdNamingRule.getSiteAId(siteLinkId).equals(siteId)) {
            return PhysicalLinkIdNamingRule.getNodeAId(siteLinkId);
        }
        return PhysicalLinkIdNamingRule.getNodeZId(siteLinkId);
    }


    /**
     * excludeNodes主要是排除掉已经在memory中的node了，比如重用OCH改变的TPC node
     *
     * @param siteId
     * @param vendorName
     * @param isReusedMixed
     * @param plane
     * @param riskGroupName
     * @param excludeNodes
     * @return
     * @throws NeDesignerException
     */
    public List<Node> getReusedNodePoolFromDbNewOch(String siteId, String vendorName, Boolean isReusedMixed, String plane, String riskGroupName, Set<String> excludeNodes)
            throws NeDesignerException {
        //符合vendor信息,且unstuff的设备（这里的unstuffz指是否盘满）。
        if (isReusedMixed) {
            return phyNodeDao.getReUsedNodes(siteId, vendorName, plane, riskGroupName, excludeNodes);
        } else {
            //只过滤tpc的设备
            return phyNodeDao.getReUsedNodes(siteId, vendorName, plane, riskGroupName, excludeNodes, NodeType.TD);
        }
    }

    /**
     * 1. config/op 都存在，按照pickConfigOrOp的原则，选取一个
     * <p>
     * 2. config存在， op没有，直接添加config
     * <p>
     * 3. config不存在，op存在，此情况为异常情况，不予考虑
     *
     * @param configNodeList
     * @return
     */
    private List<Node> checkOP(List<Node> configNodeList) {
        List<Node> result = new ArrayList<>();
        Map<String, Node> configNodeMap = configNodeList.stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity()));
        List<Node> opNodes = phyNodeDao.listOperPhyNodeByIds(new ArrayList<>(configNodeMap.keySet()));
        for (Node opNode : opNodes) {
            Node configNode = configNodeMap.get(opNode.getNodeId().getValue());
            try {
                result.add(pickConfigOrOp(configNode, opNode));
            } catch (NeDesignerException e) {
                log.error("Drop invalid node: {}", opNode.getNodeId().getValue(), e);
                continue;
            }
            configNodeMap.remove(opNode.getNodeId().getValue());
        }
        if (!configNodeMap.isEmpty()) {
            result.addAll(configNodeMap.values());
        }
        return result;
    }

    /**
     * 1. config/op 网元一样， 如都只有一块业务板卡，这个时候添加新板卡，或者新Transceiver, 以op为准
     * <p>
     * 2. config的资源比OP网元多， 以config为准，因为多出来的资源理论上有tunnel， （或者tunnel删除，但是网元没有删除的情况）
     * <p>
     * 3. config的资源比OP网元少，以op为准，先用完OP网元的资源，达到情况1
     *
     * @param configNode
     * @param opNode
     * @return
     */
    private Node pickConfigOrOp(Node configNode, Node opNode) throws NeDesignerException {
        long emptySlotCountConfig = nodeUtils.getEmptySlotCount(configNode);
        long emptySlotCountOp = nodeUtils.getEmptySlotCount(opNode);
        if (emptySlotCountConfig > emptySlotCountOp) {
            return configNode;
        }
        return opNode;
    }

    public List<Node> getReusedNodePool(Class<? extends SignalProtocolType> lineSignalRate, String cardVendorType, String vendorName, String vendorType, List<Node> nodeList,
            @NonNull String siteLinkId, SERVICETYPE servicetype) throws NeDesignerException {
        Card otCardInfo = neInfoConfig.getNeInfo(vendorName, vendorType, NodeType.TD.name()).getCardByCardVendor(cardVendorType);
        List<Node> output = new ArrayList<>();

        for (Node node : nodeList) {
            Boolean canReused = canReused(node, otCardInfo, lineSignalRate, siteLinkId, servicetype);
            if (canReused) {
                output.add(node);
            }

        }
        return output;
    }

    public boolean canReused(Node node, Class<? extends SignalProtocolType> lineSignalRate, String cardVendorType, String vendorName, String vendorType, @NonNull String siteLinkId,
            SERVICETYPE servicetype)
            throws NeDesignerException {
        Card otCardInfo = neInfoConfig.getNeInfo(vendorName, vendorType, NodeType.TD.name()).getCardByCardVendor(cardVendorType);
        return canReused(node, otCardInfo, lineSignalRate, siteLinkId, servicetype);
    }

    /**
     * 此方法只是为了甄别是否可以利旧，出于性能考虑，和依次检查以下场景：
     * <p>
     * 1. 场景3: 有空槽位（这个检查起来最快）
     * <p>
     * 2. 场景2： cardType相同，L口速率匹配，L口空闲
     * <p>
     * 3. 场景1： cardType相同，L口速率匹配，L口已经有OS link(同一个sitelink)，但是L口还有空闲交叉时隙
     *
     * @param node
     * @param otCardInfo
     * @param lineSignalRate
     * @param siteLinkId
     * @return
     * @throws NeDesignerException
     */
    public Boolean canReused(Node node, Card otCardInfo, Class<? extends SignalProtocolType> lineSignalRate, @NonNull String siteLinkId, SERVICETYPE servicetype)
            throws NeDesignerException {
        PickedOtResource pickedOtResource = pickOtResource_Scenario3(node, otCardInfo);
        if (pickedOtResource != null) {
            return true;//场景3匹配
        }

        //过滤出同一个cardType的equip，重用板卡（场景1，2）需要
        Set<String> sameCardTypeEquipIds = node.getAugmentation(Node1.class)
                .getPhysical()
                .getEquipments()
                .stream()
                .filter(item -> item.getEquipTypeConfiged().equals(otCardInfo.getCardType()))
                .map(Equipments::getEquipmentId)
                .collect(Collectors.toSet());

        List<TerminationPoint> tps = node.getTerminationPoint();
        Map<String, List<CrossConnections>> xcGroup = nodeUtils.getOtXcsGroupByLPortTp(node);
        Map<String, InternalLinks> osLinkGroup = nodeUtils.getOtOsLinkGroupByLPort(node);

        for (TerminationPoint tp : tps) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical physical = tp.getAugmentation(TerminationPoint1.class)
                    .getPhysical();

            if (!physical.getPortType().equals(PortType.OTULine)) {//过滤L口
                continue;
            }

            //为场景二做准备
            if (sameCardTypeEquipIds.contains(tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef())) {
                pickedOtResource = pickOtResource_Scenario2(node, otCardInfo, lineSignalRate, tp, physical, servicetype);
                if (pickedOtResource != null) {
                    return true;//场景2匹配
                }
            }

            //匹配场景1：L口已经有一个当前siteLink的osLink了，L口速率匹配，L口还有空闲交叉时隙
            if (sameCardTypeEquipIds.contains(tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef())) {
                InternalLinks osLink = osLinkGroup.get(tp.getTpId().getValue());
                pickedOtResource = pickOtResource_Scenario1(otCardInfo, lineSignalRate, node, xcGroup, tp, physical, siteLinkId, osLink, servicetype);
                if (pickedOtResource != null) {
                    return true;//场景1匹配
                }
            }


        }
        return false;
    }

    /**
     * 符合vendor信息的TPC网元，且OT板卡匹配，然后，按顺序寻找：
     * <p>
     * 1. L口速率匹配，L口已经有OS link，但是L口还有空闲交叉时隙
     * <p>
     * 2. L口速率匹配，L口空闲
     * <p>
     * 3. 仍然有空槽位的node。
     * <p>
     * 4  没有可重用资源，这个也是可能的
     *
     * @param nodes
     * @param otCardInfo
     * @param lineSignalRate
     * @return
     * @throws NeDesignerException
     */

    public PickedOtResource pickedOtResource(List<Node> nodes, Card otCardInfo, Class<? extends SignalProtocolType> lineSignalRate, @NonNull String siteLinkId, SERVICETYPE servicetype)
            throws NeDesignerException {
        PickedOtResource scenario2 = null;//场景二使用,如果场景一首先凑效，此变量就没用

        PickedOtResource scenario3 = null; //场景三使用

        for (Node node : nodes) {
            List<TerminationPoint> tps = node.getTerminationPoint();
            Map<String, List<CrossConnections>> xcGroup = nodeUtils.getOtXcsGroupByLPortTp(node);
            Map<String, InternalLinks> osLinkGroup = nodeUtils.getOtOsLinkGroupByLPort(node);

            //过滤出同一个cardType的equip，重用板卡（场景1，2）需要
            Set<String> sameCardTypeEquipIds = node.getAugmentation(Node1.class)
                    .getPhysical()
                    .getEquipments()
                    .stream()
                    .filter(item -> item.getEquipTypeConfiged().equals(otCardInfo.getCardType()))
                    .map(Equipments::getEquipmentId)
                    .collect(Collectors.toSet());

            for (TerminationPoint tp : tps) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical physical = tp.getAugmentation(TerminationPoint1.class)
                        .getPhysical();

                if (!physical.getPortType().equals(PortType.OTULine)) {//过滤L口
                    continue;
                }

                //匹配场景1：L口速率匹配，L口还有空闲交叉时隙
                if (sameCardTypeEquipIds.contains(tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef())) {
                    InternalLinks osLink = osLinkGroup.get(tp.getTpId().getValue());
                    PickedOtResource scenario1 = pickOtResource_Scenario1(otCardInfo, lineSignalRate, node, xcGroup, tp, physical, siteLinkId, osLink, servicetype);
                    if (scenario1 != null) {
                        return scenario1;//场景1匹配
                    }
                }

                //为场景二做准备
                if (sameCardTypeEquipIds.contains(tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef())) {
                    if (scenario2 == null) {
                        scenario2 = pickOtResource_Scenario2(node, otCardInfo, lineSignalRate, tp, physical, servicetype);
                    }
                }

            }
            if (scenario3 == null) {
                scenario3 = pickOtResource_Scenario3(node, otCardInfo);
            }
        }
        //场景1没匹配上，场景二匹配上了
        if (scenario2 != null) {
            return scenario2;
        }

        //场景1，2没匹配上，场景3匹配上了
        if (scenario3 != null) {
            return scenario3;
        }

        //场景四： 没有找到可重用的资源
        String nodeIds = nodes.stream().map(item -> item.getNodeId().getValue()).collect(Collectors.joining(","));
        log.error("Failed to pick ot resource from reused node: {}, return null.", nodeIds);
        return null;
    }

    /**
     * 仍然有空槽位的node。
     *
     * @param node
     * @param otCardInfo
     * @return
     */
    private PickedOtResource pickOtResource_Scenario3(Node node, Card otCardInfo) {
        try {
            Integer newEquipSlot = nodeUtils.pickedAvailableSlot(node, otCardInfo);
        } catch (NeDesignerException e) {
            log.debug(e.getMessage());
            return null;
        }

        return PickedOtResource.builder().node(node).build();
    }

    /**
     * L口速率匹配，L口空闲,没有OCH
     *
     * @param node
     * @param otCardInfo
     * @param lineSignalRate
     * @param tp
     * @param physical
     * @return
     * @throws NeDesignerException
     */
    private PickedOtResource pickOtResource_Scenario2(Node node, Card otCardInfo, Class<? extends SignalProtocolType> lineSignalRate, TerminationPoint tp, Physical physical, SERVICETYPE servicetype)
            throws NeDesignerException {
        if (!physical.getConnectionStatus().equals(ConnectionStatus.Busy)) {
            String ltp = tp.getTpId().getValue();
            String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ltp);
            CrossConnection xcInfo = tunnelUtils.getOtXcInfo(otCardInfo, lPortName, lineSignalRate, physical.getOtuClient().getSignalRate(), servicetype);
            List<String> lPortXcLayers = NeInfoUtil.getXcLayers(xcInfo.getTo().getLayer());
            PickedOtTps pickedOtTps = pickAvailableLPortLayer(xcInfo, lPortXcLayers, null, ltp); //空闲的L端口是没有交叉的，所以tpXcs为null
            return PickedOtResource.builder().node(node).pickedOtTps(pickedOtTps).build();
        }
        return null;
    }

    /**
     * 匹配场景1：
     * <p>
     * 1. L口速率匹配
     * <p>
     * L口还有空闲交叉时隙
     * <p>
     * L口的这条osLink就是属于sitelink
     *
     * @param otCardInfo
     * @param lineSignalRate
     * @param node
     * @param xcGroup
     * @param tp
     * @param physical
     * @param siteLinkId
     * @param osLink
     * @return
     * @throws NeDesignerException
     */
    public PickedOtResource pickOtResource_Scenario1(Card otCardInfo, Class<? extends SignalProtocolType> lineSignalRate, Node node, Map<String, List<CrossConnections>> xcGroup, TerminationPoint tp,
            Physical physical, @NonNull String siteLinkId,
            InternalLinks osLink, SERVICETYPE servicetype) throws NeDesignerException {
        if (osLink == null) {
            return null; //此场景属于场景二cover，本场景不处理这种情况
        }

        String muxEquipId = nodeUtils.getMuxEquipIdByOsLink(osLink);
        if (!siteLinkId.contains(muxEquipId)) {
            return null;//当前的这个L口建立的osLink，不属于此siteLink
        }

        if (physical.getOtuLine() != null && physical.getOtuLine().getSignalRate().equals(lineSignalRate)) {//physical有OtuLine，就证明了这个L口至少有一个交叉了
            String ltp = tp.getTpId().getValue();
            List<CrossConnections> tpXcs = xcGroup.get(ltp);
            String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ltp);
            CrossConnection xcInfo = tunnelUtils.getOtXcInfo(otCardInfo, lPortName, lineSignalRate, physical.getOtuClient().getSignalRate(), servicetype);
            List<String> lPortXcLayers = NeInfoUtil.getXcLayers(xcInfo.getTo().getLayer());
            if (tpXcs.size() < lPortXcLayers.size()) { //L口还有空闲的layer可用
                PickedOtTps pickedOtTps = pickAvailableLPortLayer(xcInfo, lPortXcLayers, tpXcs, ltp);
                return PickedOtResource.builder().node(node).pickedOtTps(pickedOtTps).reusedLPort(true).build();
            }
        }
        return null;
    }

    /**
     * 1. 总是按照json文件的配置，按顺序选择第一个可用的layer
     * <p>
     * 2. L port的layer總是和C port对应的。 如果当前的layer的是空闲的，理论上对应的Cport也应该是空闲的
     * <p>
     * 3. 交叉的配置上面，from中的C端口，和to中的layer，都是按照顺序一一对应的。比如： "from": { "port": "C?,1,2,1", "layer": "/odu4=1" }, "to": { "port": "L1", "layer": "/odu4x2=1/odu4=?,1,2,1" } ,那么C1肯定对应第一个layer，C2对应第二个layer
     *
     * @param xcInfo
     * @param lPortXcLayers
     * @param tpXcs
     * @param ltp
     * @return
     */
    private PickedOtTps pickAvailableLPortLayer(CrossConnection xcInfo, List<String> lPortXcLayers, List<CrossConnections> tpXcs, String ltp) throws NeDesignerException {
        Set<String> usedLayers = tpXcs == null ? Collections.emptySet() : getUsedLayers(tpXcs);

        for (int i = 0; i < lPortXcLayers.size(); i++) {
            String layer = lPortXcLayers.get(i);
            if (!usedLayers.contains(layer)) {//按照json的配置顺序找到一个空闲的layer
                String cPortName = NeInfoUtil.getNameList(xcInfo.getFrom().getPort()).get(i);
                String availableCPortTp = nodeUtils.getCPortTp(cPortName, ltp);
                return PickedOtTps.builder().ctp(availableCPortTp).ltp(ltp).lPortSlot(layer).build();
            }
        }

        //通常情况下，此异常不会出现。 除非是板卡的json配置文件在操作前后发生了变化。
        throw new NeDesignerException("Failed to pick available C port for L port, when free L port layer available.");
    }


    private Set<String> getUsedLayers(List<CrossConnections> tpXcs) {
        return tpXcs.stream().map(xc -> xc.getDestinationTp().get(0).getSlot()).collect(Collectors.toSet());
    }

    /**
     * 当前因为没有类似先用DB中的node，这种利旧策略，所以，直接合并reusedNodesInDb和reusedNodesInMemory
     *
     * @param otCardInfo
     * @param lineSignalRate
     * @param reusedNodesInDb
     * @param reusedNodesInMemory
     * @return
     */
    public PickedOtResource getReusedNode(Card otCardInfo, Class<? extends SignalProtocolType> lineSignalRate, List<Node> reusedNodesInDb, List<Node> reusedNodesInMemory,
            @NonNull String siteLinkId, SERVICETYPE servicetype) throws NeDesignerException {
        List<Node> reusedNodes = new ArrayList<>();
        if (reusedNodesInDb != null && !reusedNodesInDb.isEmpty()) {
            reusedNodes.addAll(reusedNodesInDb);
        }
        if (reusedNodesInMemory != null && !reusedNodesInMemory.isEmpty()) {
            reusedNodes.addAll(reusedNodesInMemory);
        }

        if (reusedNodes.isEmpty()) {
            return null;
        }
        return pickedOtResource(reusedNodes, otCardInfo, lineSignalRate, siteLinkId, servicetype);
    }


    public List<CrossConnections> reUsedOch(String nodeId, String ltpId, List<CrossConnections> ltpXcs, Card otCardInfo, Class<? extends SignalProtocolType> lineSignalRate,
            Class<? extends SignalProtocolType> tunnelSignalRate, @NonNull Integer number, SERVICETYPE servicetype)
            throws NeDesignerException {
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ltpId);
        CrossConnection xcInfo = tunnelUtils.getOtXcInfo(otCardInfo, lPortName, lineSignalRate, tunnelSignalRate, servicetype);
        List<String> lPortXcLayers = NeInfoUtil.getXcLayers(xcInfo.getTo().getLayer());
        Set<String> usedLayers = ltpXcs == null ? Collections.emptySet() : getUsedLayers(ltpXcs);
        List<CrossConnections> newXcs = new ArrayList<>();
        for (int i = 0; i < lPortXcLayers.size(); i++) {
            String layer = lPortXcLayers.get(i);
            if (!usedLayers.contains(layer)) {//按照json的配置顺序找到一个空闲的layer
                String cPortName = NeInfoUtil.getNameList(xcInfo.getFrom().getPort()).get(i);
                String availableCPortTp = nodeUtils.getCPortTp(cPortName, ltpId);
                CrossConnections newXc = otXcService.createXC(nodeId, availableCPortTp, ltpId, xcInfo);
                newXcs.add(newXc);
                number--;
                if (number == 0) {
                    return newXcs;
                }
            }
        }
        if (number != 0) {
            String msg = String.format("Failed to reused och for tp:%s, because still have %d tunnel can't be created.", ltpId, number);
            log.error(msg);
            throw new NeDesignerException(msg);
        }
        return newXcs;
    }

    private Pair<String, Node> getReusedportByPortType(Node node, String cardVendor, PortType portType, SERVICETYPE serviceType) throws NeDesignerException {
        return getReusedportByPortType(node, cardVendor, portType, serviceType, null, null);
    }

    private Pair<String, Node> getReusedportByPortType(Node node, String cardVendor, PortType portType, SERVICETYPE serviceType, String excludeTpId, String protectionType) throws NeDesignerException {
        Set<String> sameCardVendorEquipIds = nodeUtils.getEquipIdsByCardVendor(node, cardVendor, serviceType);
        if (sameCardVendorEquipIds.isEmpty()) {
            return null;
        }
        List<TerminationPoint> tps = nodeUtils.getTps(node);
        String matcheTpId = null;
        for (TerminationPoint tp : tps) {
            if (excludeTpId != null && excludeTpId.equals(tp.getTpId().getValue())) {
                continue;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical physical = tp.getAugmentation(TerminationPoint1.class)
                    .getPhysical();

            if (!physical.getPortType().equals(portType)) {
                continue;
            }
            if (!physical.getConnectionStatus().equals(ConnectionStatus.Idle)) {
                continue;
            }

            String equipmentRef = tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef();
            if (sameCardVendorEquipIds.contains(equipmentRef)) {
                if (matchSameOlpRule(node, tp, protectionType)) {
                    if (matcheTpId == null || PhysicalTpIdNamingRule.getPortNameByTpId(matcheTpId)
                            .compareTo(PhysicalTpIdNamingRule.getPortNameByTpId(tp.getTpId().getValue())) > 0) {
                        matcheTpId = tp.getTpId().getValue();
                    }
                }

            }

        }
        if (matcheTpId != null) {
            return Pair.of(matcheTpId, node);
        }
        return null;
    }

    private boolean matchSameOlpRule(Node node, TerminationPoint tp, String protectionType) throws NeDesignerException {
        if (!tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPSig)) {
            return true;
        }
        if (!yangModel.equals(NeYangModel.ByteDance.getName())) {//只有字节需要1：3只能1：3
            return true;
        }

        //板卡配置了哪些口的交叉，哪些才能用。
        String tpId = tp.getTpId().getValue();
        Card opCardInfo = nodeUtils.getCardInfoByEquipId(node, PhysicalTpIdNamingRule.getEquipId(tpId));
        Set<String> validOpSigPortNames = opCardInfo.getCrossConnections().stream().map(xcInfo -> xcInfo.getFrom().getPort()).collect(Collectors.toSet());
        if (!validOpSigPortNames.contains(PhysicalTpIdNamingRule.getPortNameByTpId(tpId))) {
            return false;
        }

        //1:1只能和1：1的在一张卡，1：2的也只能和1：2的在一张卡
        String equipmentRef = tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef();
        Optional<TerminationPoint> optionalOpc = node.getTerminationPoint().stream()
                .filter(t -> t.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef().equals(equipmentRef) &&
                        t.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPC) &&
                        t.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().equals(ConnectionStatus.Busy)).findAny();
        if (protectionType.equals(TunnelNewOchInput.PROTECTED_1TO1)) {
            if (optionalOpc.isPresent()) {
                return false;
            }
        }
        if (protectionType.equals(TunnelNewOchInput.PROTECTED_1TO2)) {
            if (!optionalOpc.isPresent()) {
                return false;
            }
        }
        return true;

    }

    public Pair<String, Node> getReusedLport(Node node, String cardVendor, SERVICETYPE serviceType) throws NeDesignerException {
        return getReusedportByPortType(node, cardVendor, PortType.OTULine, serviceType);
    }

    public Pair<String, Node> getReusedOp6port(Node node, String cardVendor, String protectionType) throws NeDesignerException {
        return getReusedportByPortType(node, cardVendor, PortType.OPSig, null, null, protectionType);
    }


    public String getAnotherLPort(Pair<String, Node> pair, SERVICETYPE serviceType, String cardVendor) throws NeDesignerException {
        Pair<String, Node> resultPair = getReusedportByPortType(pair.getRight(), cardVendor, PortType.OTULine, serviceType, pair.getLeft(), null);
        if (resultPair != null) {
            return resultPair.getLeft();
        }
        return null;
    }

    public Pair<String, String> getReusedOp6LPortPair(Node node, Card op6CardInfo, Card otCardInfo, SERVICETYPE serviceType, List<String> op6SigTpIds) throws NeDesignerException {
        //        //op6CardInfo这里暂时先不用考虑，留在这里，以后有需要再加

        if (op6SigTpIds.isEmpty()) {
            return null;
        }

        Set<String> sameCardVendorEquipIdsOT = nodeUtils.getEquipIdsByCardVendor(node, otCardInfo.getVendorType(), serviceType);
        if (sameCardVendorEquipIdsOT.isEmpty()) {
            return null;
        }

        List<String> lineTpIds = nodeUtils.getTps(node).stream().filter(tp ->
                        tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTULine)
                                && tp.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().equals(ConnectionStatus.Idle)
                                && sameCardVendorEquipIdsOT.contains(tp.getAugmentation(TerminationPoint1.class).getPhysical().getEquipmentRef()))
                .map(tp -> tp.getTpId().getValue())
                .sorted()
                .collect(Collectors.toList());

        if (lineTpIds.isEmpty()) {
            return null;
        }

        for (String op6SigTpId : op6SigTpIds) {
            List<Integer> possibleOtSlots = getPossibleOtSlotsSorted(op6SigTpId, otCardInfo, serviceType);
            Optional<String> optionalLineTpId = lineTpIds.stream().filter(tpId -> possibleOtSlots.contains(PhysicalTpIdNamingRule.getSlotId(tpId))).findFirst();
            if (optionalLineTpId.isPresent()) {
                return Pair.of(op6SigTpId, optionalLineTpId.get());
            }
        }
        return null;
    }

    public List<Integer> getPossibleOtSlotsSorted(String op6SigTpId, Card otCardInfo, SERVICETYPE serviceType) throws NeDesignerException {
        int op6SlotId = PhysicalTpIdNamingRule.getSlotId(op6SigTpId);
        return getPossibleOtSlotsSorted(op6SlotId, otCardInfo, serviceType);
    }

    /**
     * 1.单槽位一个L口就是 OLP3 1/5;  234->1, 678->5
     *
     * 2.单槽位多于1个L口，就OLP3 1/3/5/7;  2->1, 4->3,6->5,8->7
     *
     * 3.双槽位2个L口，就OLP3 1/5; 3->1,7->5
     *
     * @param op6SlotId
     * @param otCardInfo
     * @param serviceType
     * @return
     */
    public List<Integer> getPossibleOtSlotsSorted(int op6SlotId, Card otCardInfo, SERVICETYPE serviceType) throws NeDesignerException {
        Integer width = otCardInfo.getWidth();
        long lPortCount = NeInfoUtil.getLPortCount(otCardInfo, serviceType);

        List<Integer> mapping;//mappingOtPossibleSlots
        switch (op6SlotId) {
            case 1:
                mapping = (width == 1) ? (lPortCount == 1 ? Arrays.asList(2, 3, 4) : Arrays.asList(2)) : Arrays.asList(3);
                break;

            case 3:
                mapping = (width == 1) ? (lPortCount == 1 ? Collections.emptyList() : Arrays.asList(4)) : Collections.emptyList();
                break;
            case 5:
                mapping = (width == 1) ? (lPortCount == 1 ? Arrays.asList(6, 7, 8) : Arrays.asList(6)) : Arrays.asList(7);
                break;
            case 7:
                mapping = (width == 1) ? (lPortCount == 1 ? Collections.emptyList() : Arrays.asList(8)) : Collections.emptyList();
                break;
            default:
                throw new NeDesignerException("op card only support 1,3,5,7 slot, not support slot:" + op6SlotId);

        }
        if (mapping.isEmpty()) {
            return Collections.emptyList();
        }
        return otCardInfo.getPossibleSlot().stream()
                .filter(mapping::contains)
                .sorted()
                .collect(Collectors.toList());
    }

    public Pair<Integer, Integer> getAvailableOpOtSlotPair(Node node, Card op6CardInfo, Card otCardInfo, SERVICETYPE serviceType) throws NeDesignerException {
        Set<Integer> usedSlots = nodeUtils.getLineCardUsedSlots(node);
        List<Integer> possibleSlotsOp = op6CardInfo.getPossibleSlot();

        for (Integer opSlot : possibleSlotsOp) {
            // OP 槽位已占用，跳过
            if (usedSlots.contains(opSlot)) {
                continue;
            }
            // 获取 OT 卡在该 OP 槽位下的可选 OT 槽位
            List<Integer> possibleSlotsOt = getPossibleOtSlotsSorted(opSlot, otCardInfo, serviceType);
            for (Integer otSlot : possibleSlotsOt) {
                boolean isAvailable = IntStream.range(0, otCardInfo.getWidth()) .noneMatch(offset -> usedSlots.contains(otSlot + offset));
                if (isAvailable) {
                    return Pair.of(opSlot,otSlot);
                }
            }
        }

        return null;
    }

    public List<Node> getReusedNodePoolFromDbReg(String siteId, String vendorName, List<String> cardTypes, List<String> portTypes, String planeId, String riskGroupName, Set<String> excludeNodeIds) {
        //过滤有空闲的，没创建OCH的L端口或者OP6端口的那些 config node
        List<Node> reusedNodesInDb = phyNodeDao.getReUsedNodes(siteId, vendorName, cardTypes, portTypes, planeId, riskGroupName, excludeNodeIds, NeSubType.EPC_REG);
        return  reusedNodesInDb;
    }
}
