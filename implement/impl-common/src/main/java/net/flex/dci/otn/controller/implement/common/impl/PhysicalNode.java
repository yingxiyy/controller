package net.flex.dci.otn.controller.implement.common.impl;

import static net.flex.dci.otc.common.util.Constant.PropKey_HostName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.implement.common.repaire.RepaireOnOch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.client.attributes.ClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClientBuilder;

/**
 * 这个地方看起来有些怪异，那是因为，有时候现场已经把业务。复用段创建好了， 然后才开始用dciworld 去规划 结果就会导致已经配置好的参数被规划系统的默认参数所替换。
 *
 * 所以这里needWrite2Ne 都是比对opNode 是否已经implement 了。 如果已经配置，那么就不会通过控制器再次下发 ！！！！！
 * 使用这个类就需要注意，因为有些已经implement 的tunnel 的修改，如果需要修改整个tunnel 的某些属性，不能用这个类
 *
 *
 * ！！！上面的描述不对，如果设备对象由于某些原因已经是adminUp, 这个情况下controller的默认配置就不能写入设备 所以正确的处理方式是，先看是否需要upload设备，
 * 需要LinkImplementState extractChangedCfgNode() 中implConfig.isUploadNeParam() 如果需要upload，通过op->cfg，
 * 这样就避免了默认参数不能写入网元的问题
 */
@Slf4j
public class PhysicalNode {
    public final static String XC_ERROR = "cannot find required XC";

    private final static PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    Map<String, ReadyResource> readyResourceMap;
    private ImplActionType actionType;
    private boolean isImplementAction;

    /**
     *
     */
    public PhysicalNode(ImplActionType actionType) {
        this.actionType = actionType;
        isImplementAction = actionType.equals(ImplActionType.Deimplement) ? false : true;
        readyResourceMap = new HashMap<>();
    }

    public static ImplementState getImplementState(Node node) {
        Physical phyNodeAttr = node.getAugmentation(Node1.class).getPhysical();
        return phyNodeAttr.getImplementState();
    }

    /**
     * 但是部分值可能已经写入网元了，所以真正需要写入网元的信息保存在extractChanged对象中。 已经写入网元的保存在ReadyResource对象
     */

    public static AdminStatus getAdminState(Node node) {
        Physical phyNodeAttr = node.getAugmentation(Node1.class).getPhysical();
        return phyNodeAttr.getAdminState();
    }

    public static Node updateImplementState(Node node, ImplementState targetState) {
        log.debug("update Node ImplementState {} {}", node.getNodeId().getValue(),
                targetState.name());
        Physical phyNodeAttr = node.getAugmentation(Node1.class).getPhysical();
        NodeBuilder nodeBuilder = new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(phyNodeAttr)
                                .setImplementState(targetState)
                                .setAdminState(AdminStatus.Up)
                                .build())
                        .build());
        return nodeBuilder.build();
    }

    public Map<String, ReadyResource> getReadyResource() {
        return readyResourceMap;
    }

    public Node updateTpImplState(Node node, String tpId, AdminStatus adminState,
            ImplementState implState) {
        int pos = -1;
        TerminationPoint newTp = null;
        for (TerminationPoint tp : node.getTerminationPoint()) {
            pos++;
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr =
                    tp.getAugmentation(TerminationPoint1.class).getPhysical();
            if (tp.getTpId().getValue().equals(tpId)) {
                newTp = new TerminationPointBuilder(tp).addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder()
                                .setPhysical(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                                tpAttr)
                                                .setAdminState(
                                                        adminState == null ? (isImplementAction
                                                                ? AdminStatus.Up
                                                                : tpAttr.getAdminState())
                                                                : adminState)
                                                .setImplementState(implState)
                                                .build())
                                .build()).build();
                break;
            }
        }

        if (pos >= 0 && newTp != null) {
            List<TerminationPoint> newTpList = new ArrayList<>(node.getTerminationPoint());
            newTpList.removeIf(x->x.getTpId().getValue().equals(tpId));

            newTpList.add(pos, newTp);

            return new NodeBuilder(node).setTerminationPoint(newTpList).build();
        }
        return node;
    }

    public Node updateEqImplState(Node node, String eqId, AdminStatus adminState,
            ImplementState implState) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        int pos = -1;
        Equipments newEq = null;
        for (Equipments eq : nodeAttr.getEquipments()) {
            pos++;
            if (eq.getEquipmentId().equals(eqId)) {
                newEq = new EquipmentsBuilder(eq)
                        .setAdminState(adminState == null ? (isImplementAction ? AdminStatus.Up
                                : eq.getAdminState()) : adminState)
                        .setImplementState(implState)
                        .build();
                break;
            }
        }
        if (pos >= 0 && newEq != null) {
            List<Equipments> eqList = new ArrayList<>(nodeAttr.getEquipments());
            eqList.removeIf(x->x.getEquipmentId().equals(eqId));

            eqList.add(pos, newEq);
            Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(nodeAttr)
                                    .setEquipments(eqList)
                                    .build())
                            .build())
                    .build();

            return newNode;
        }
        return node;
    }

    public Node updateXcImplState(Node node, String xcId, AdminStatus adminState,
            ImplementState implState) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        int pos = -1;
        CrossConnections newXc = null;
        for (CrossConnections xc : nodeAttr.getCrossConnections()) {
            pos++;
            if (xc.getCrossConnectionId().getValue().equals(xcId)) {
                newXc = new CrossConnectionsBuilder(xc)
                        .setAdminState(adminState == null ? (isImplementAction ? AdminStatus.Up
                                : xc.getAdminState()) : adminState)
                        .setImplementState(implState)
                        .build();
                break;
            }
        }
        if (pos >= 0 && newXc != null) {
            List<CrossConnections> xcList = new ArrayList<>(nodeAttr.getCrossConnections());
            xcList.removeIf(x->x.getCrossConnectionId().getValue().equals(xcId));
            xcList.add(pos, newXc);
            Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(nodeAttr)
                                    .setCrossConnections(xcList)
                                    .build())
                            .build())
                    .build();

            return newNode;
        }
        return node;
    }

    /**
     * based on phyLink, find out Node related internalLink, and mark it
     *
     * @param node
     * @param linkId
     */
    public Node updateLinkImplState(Node node, String linkId, ImplementState implState) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        int pos = -1;
        InternalLinks newIl = null;
        for (InternalLinks il : nodeAttr.getInternalLinks()) {
            pos++;
            if (il.getLinkRef() != null && il.getLinkRef().equals(linkId)) {
                newIl = new InternalLinksBuilder(il)
                        .setImplementState(implState)
                        .build();
                break;
            }
        }
        if (pos >= 0 && newIl != null) {
            List<InternalLinks> ilList = new ArrayList<>(nodeAttr.getInternalLinks());
            ilList.remove(pos);
            ilList.add(pos, newIl);
            Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(nodeAttr)
                                    .setInternalLinks(ilList)
                                    .build())
                            .build())
                    .build();

            return newNode;
        }
        return node;
    }

    /**
     * extract does the TP should writting to NE, if yes, update changedNode
     *
     * @param orgCfgNode
     * @param cfgChangedNodeMap
     * @param tpId
     * @alignWithActionType when this is false, the TP adminStatus always down
     */
    public void constructCfgNodeByTp(Node orgCfgNode, Map<String, Node> cfgChangedNodeMap,
            String tpId, boolean alignWithActionType) {
        Node changedNode = getEmptyChangedNode(orgCfgNode, cfgChangedNodeMap);
        Node newNode = extractChangedTp(orgCfgNode, changedNode, tpId, alignWithActionType);
        cfgChangedNodeMap.put(orgCfgNode.getNodeId().getValue(), newNode);
    }

    //================================================================================================
    //
    //      start to process cfgNode
    //
    //================================================================================================

    private Node getEmptyChangedNode(Node orgNode, Map<String, Node> cfgNodeMap) {
        if (orgNode == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "almost impossible, the sitelink related node is null");
        }
        String nodeId = orgNode.getNodeId().getValue();
        if (cfgNodeMap.containsKey(nodeId)) {
            return cfgNodeMap.get(nodeId);
        } else {
            Physical nodeAttr = orgNode.getAugmentation(Node1.class).getPhysical();

            List<Property> propList = new ArrayList<>();
            propList.add(new PropertyBuilder().setName(PropKey_HostName)
                    .setKey(new PropertyKey(PropKey_HostName))
                    .setValue(nodeAttr.getFriendlyName())
                    .build());

            //node only can be setting to adminup.
            Node changedNode = new NodeBuilder()
                    .setNodeId(orgNode.getNodeId())
                    .setTerminationPoint(new ArrayList<>())
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(nodeAttr)
                                    .setInternalLinks(new ArrayList<>())
                                    .setCrossConnections(new ArrayList<>())
                                    .setEquipments(new ArrayList<>())
                                    .setOCMGripGroups(new ArrayList<>())
                                    .build())
                            .build())
                    .build();

            return changedNode;
        }
    }

    private Node extractChangedTp(Node node, Node changedNode, String tpId, boolean alignWithActionType) throws CommonException {
        TerminationPoint newTp = null;

        AdminStatus adminStatus = isImplementAction ? AdminStatus.Up : AdminStatus.Down;
        ImplementState implementState = isImplementAction ? ImplementState.Implement : ImplementState.Allocate;

        if (!alignWithActionType) {
            adminStatus = AdminStatus.Down;
            implementState = ImplementState.Allocate;
        }

        for (TerminationPoint tp : node.getTerminationPoint()) {
            if (tp.getTpId().getValue().equals(tpId)) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr =
                        tp.getAugmentation(TerminationPoint1.class).getPhysical();

                if (needWrite2Ne(tp)) {
//                    log.debug("found and need write to NE, TP: {}", tpId);
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder tpPb;
                        tpPb =
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(tpAttr)
                                        .setAdminState(adminStatus)
                                        .setImplementState(implementState)
                                        .setFriendlyName(tpAttr.getFriendlyName())
                                        .setOtuLine(tpAttr.getOtuLine())
                                        .setOtuClient(tpAttr.getOtuClient())
                                        .setWdm(tpAttr.getWdm())
                                        .setMuxChannel(tpAttr.getMuxChannel())
                                        .setAlarmState(null)
                                        .setAlignmentStatus(null)
                                        .setOperationalState(null)
                                        .setDeviceRef(null)
                                        .setDirection(null)
                                        .setEquipmentRef(null)
                                        .setGlobalIdentify(null)
                                        .setConnectionStatus(null);
                    

                    //特殊需要， 在deImpl 业务的时候， 需要把C口 的以太网模块移除，（为了下一次下发成功)
                    if (!isImplementAction) {
                        if (tpPb.getOtuClient() != null) {
                            OtuClient newClient = new OtuClientBuilder().setClient(
                                            new ClientBuilder()
                                                    .setEthComplianceCode(ETHUNDEFINED.class)
                                                    .build())
                                    .build();

                            tpPb.setOtuClient(newClient);
                            log.debug("insert new otuClient for setting ETHUNDEFINED");
                        } else {
                            tpPb.setDcn(null).setProperties(null).setIdc(null)
                                    .setOtuClient(null).setOtuLine(null)
                                    .setMuxChannel(null).setWdm(null).setOxc(null);
                        }
                    }

                    newTp = new TerminationPointBuilder(tp)
                            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                    .setPhysical(tpPb.setAlignmentStatus(null)
                                            .setAlarmState(null)
                                            .build())
                                    .build()).build();

                    ArrayList<TerminationPoint> newTpList = new ArrayList<>(
                            changedNode.getTerminationPoint());
                    if (!alignWithActionType) {
                        // 特殊端口的 Down 覆盖前面普通提取得到的 Up，不能把同一 TP 的两种状态都发给设备。
                        // 只调整本次下发列表，不删除 CFG 中的端口，也不改变普通下发/删除的提取逻辑。
                        newTpList.removeIf(existing -> tpId.equals(existing.getTpId().getValue()));
                    }
                    newTpList.add(newTp);
                    changedNode = new NodeBuilder(changedNode).setTerminationPoint(newTpList)
                            .build();
                } else {
                    ReadyResource readyResource = getReadyResourceWithNodeId(
                            node.getNodeId().getValue());
                    Optional<TerminationPoint> oTp = readyResource.getTpList().stream()
                            .filter(t -> t.getTpId().getValue().equals(tp.getTpId().getValue()))
                            .findFirst();
                    if (!oTp.isPresent()) {
                        readyResource.getTpList().add(tp);
                    }
                }

//                String transceiverID = PhysicalTpIdNamingRule.getTransceiverId(tpId, tpAttr.getPortType());
//                if (transceiverID != null) {
//                    changedNode = extractChangedEq(node, changedNode, transceiverID);
//                }

                return changedNode;
            }
        }

        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "cannot find out required TP " + tpId);
    }

    private ReadyResource getReadyResourceWithNodeId(String nodeId) {
        if (!readyResourceMap.containsKey(nodeId)) {
            readyResourceMap.put(nodeId, new ReadyResource());
        }
        return readyResourceMap.get(nodeId);
    }

    public void constructCfgNodeByEq(Node orgCfgNode, Map<String, Node> cfgChangedNodeMap,
            String eqId) {
        Node changedNode = getEmptyChangedNode(orgCfgNode, cfgChangedNodeMap);
        Node newNode = extractChangedEq(orgCfgNode, changedNode, eqId);
        cfgChangedNodeMap.put(orgCfgNode.getNodeId().getValue(), newNode);
    }

    private Node extractChangedEq(Node orgCfgNode, Node changedNode, String eqId) throws CommonException {
        Physical nodeAttr = orgCfgNode.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getEquipments() == null) {
            return changedNode;
        }

        Equipments newEq = null;
        for (Equipments eq : nodeAttr.getEquipments()) {
            if (eq.getEquipmentId().equals(eqId)) {
                if (needWrite2Ne(eq)) {
                    EquipmentsBuilder newEqBuilder = new EquipmentsBuilder(eq)
//                            .setEquipTypeConfiged(
//                                    eq.getEquipTypeVendorSpecific())   //需要考虑网元实际板卡于conf不一样
//                            .setEquipTypeConfiged(null)
                            .setAdminState(isImplementAction ? AdminStatus.Up : AdminStatus.Down)
                            .setImplementState(isImplementAction ? ImplementState.Implement
                                    : ImplementState.Allocate)
                            .setFriendlyName(eq.getFriendlyName())
                            .setAlarmState(null)
                            .setDeviceRef(null)
                            .setActivationTime(null)
                            .setEmpty(null)
                            .setEquipClass(null)
                            .setEquipTypeInstalled(null)
                            .setOperationalState(null)
                            .setAlignmentStatus(null)
                            .setRemoveable(null)
                            .setShelf(null)
                            .setSlot(null);

//                    if (!isImplementAction) { ///need check, this function is included in OTU_Client or where
//                        newEqBuilder.setProperties(null);
//                        if (eqId.contains("TRANSCEIVER") && !eqId.contains("OSC")) {
//                            List<Property> newList = new ArrayList<>();
//                            newList.add(new PropertyBuilder()
//                                    .setName("ethernet-pmd")
//                                    .setValue("ETH UNDEFINED")
//                                    .setKey(new PropertyKey("ethernet-pmd"))
//                                    .build());
//                            Properties prop = new PropertiesBuilder().setProperty(newList).build();
//                            newEqBuilder.setProperties(prop);
//                        }
//                    }
                    newEq = newEqBuilder.build();

                    Physical changedNodeAttr = changedNode.getAugmentation(Node1.class)
                            .getPhysical();
                    List<Equipments> eqList = changedNodeAttr.getEquipments();
                    eqList.add(newEq);
                    changedNode = new NodeBuilder(changedNode).addAugmentation(Node1.class,
                                    new Node1Builder()
                                            .setPhysical(new PhysicalBuilder(changedNodeAttr)
                                                    .setEquipments(eqList)
                                                    .build())
                                            .build())
                            .build();
                } else {
                    ReadyResource readyResource = getReadyResourceWithNodeId(
                            orgCfgNode.getNodeId().getValue());
                    Optional<Equipments> oEq = readyResource.getEqList().stream()
                            .filter(t -> t.getEquipmentId().equals(eq.getEquipmentId()))
                            .findFirst();
                    if (!oEq.isPresent()) {
                        readyResource.getEqList().add(eq);
                    }
                }

                return changedNode;
            }
        }
        //Some transceiver is fixed in card.
        if (eqId.contains("TRANSCEIVER") || eqId.contains("-LINEOSC")) {
//            String tpInfo[] = eqId.split("#");
//            String cardId = tpInfo[tpInfo.length - 1].replace("TRANSCEIVER", "LINECARD");
//            cardId = cardId.replace("-LINEOSC", "");
//            String eqpId = tpInfo[0] + "#" + tpInfo[1] + "#" + cardId;
//
//            Optional<Equipments> eqOp = nodeAttr.getEquipments().stream()
//                    .filter(x -> x.getEquipmentId().equals(eqpId)).findAny();
//            if (eqOp.isPresent()) {
//                Equipments eq = eqOp.get();
//                if (eq.getEquipType().equals(EquipType.WSS)) {
//                    log.error("discard error transceiver, this is WSS card" + eqId);
//                } else {
//                    log.error("cannot find transceiver and related card isn't wss " + eqId);
//                }
//            } else {
//                log.error("cannot find transceiver " + eqId);
//            }
            return changedNode;
        }

        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "cannot find out required EQ " + eqId);
    }

    public void constructCfgNodeByXc(Node orgCfgNode, Map<String, Node> cfgChangedNodeMap,
                                     String xcId, Properties properties) {
        Node changedNode = getEmptyChangedNode(orgCfgNode, cfgChangedNodeMap);
        Node newNode = extractChangedXc(orgCfgNode, changedNode, xcId, properties);
        cfgChangedNodeMap.put(orgCfgNode.getNodeId().getValue(), newNode);
    }

    private Node extractChangedXc(Node cfgNode, Node changedNode, String xcId, Properties properties) {
        Physical nodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();
//        if (xcId.endsWith("-MPO")) {
//            //split to real MPO port on device
//            String[] tmp = xcId.split("-Site-");
//            String tpId = "-Site-" + tmp[3];
//            String tmpXcId = tmp[0] + "-Site-" + tmp[1] + "-Site-" + tmp[2];
//            for (int i = 1; i<=8; i++) {
//                tmpXcId = tmpXcId + tpId + i;
//            }
//            xcId = tmpXcId;
//        }
        if (nodeAttr.getCrossConnections() == null) {
            return changedNode;
        }

        AdminStatus adminStatus = isImplementAction ? AdminStatus.Up : AdminStatus.Down;
        ImplementState implementState = isImplementAction ? ImplementState.Implement : ImplementState.Allocate;

        for (CrossConnections xc : nodeAttr.getCrossConnections()) {
            if (xc.getCrossConnectionId().getValue().equals(xcId)) {
                if (needWrite2Ne(xc)) {
                    CrossConnectionsBuilder xcBuilder = new CrossConnectionsBuilder(xc)
                            .setAdminState(adminStatus)
                            .setImplementState(implementState)
                            .setDescription(xc.getDescription())
                            .setOperationalState(null)
                            .setAps(xc.getAps())
                            .setAmplifier(xc.getAmplifier())
                            .setWssChannel(xc.getWssChannel())
                            .setProperties(properties);

                    if (!isImplementAction && nodeAttr.getNodeType().equals(NodeType.OD)) {
                        if (xcBuilder.getAps() != null || xcBuilder.getAmplifier() != null) {
                            //this is APS XC, Amplifier XC cannot be adminDown. skip it.
                            //复用段开通后，deImpl的时候这些是不关闭的
                            ReadyResource readyResource = getReadyResourceWithNodeId(
                                    cfgNode.getNodeId().getValue());
                            Optional<CrossConnections> oXC = readyResource.getXcList().stream()
                                    .filter(t -> t.getCrossConnectionId().getValue()
                                            .equals(xc.getCrossConnectionId().getValue()))
                                    .findFirst();
                            if (!oXC.isPresent()) {
                                readyResource.getXcList().add(xcBuilder.build());
                            }
                            return changedNode;
                        }
                        //所有的allocate, 如果目标已经是allocate, 不下发特殊属性
                        xcBuilder.setProperties(null).setAmplifier(null).setAps(null)
                                .setWssChannel(null);
                    }

                    Physical changedNodeAttr = changedNode.getAugmentation(Node1.class)
                            .getPhysical();
                    List<CrossConnections> xcList = changedNodeAttr.getCrossConnections();
                    xcList.add(xcBuilder.build());
                    changedNode = new NodeBuilder(changedNode).addAugmentation(Node1.class,
                                    new Node1Builder()
                                            .setPhysical(new PhysicalBuilder(changedNodeAttr)
                                                    .setCrossConnections(xcList)
                                                    .build())
                                            .build())
                            .build();
                } else {
                    ReadyResource readyResource = getReadyResourceWithNodeId(
                            cfgNode.getNodeId().getValue());
                    Optional<CrossConnections> oXC = readyResource.getXcList().stream()
                            .filter(t -> t.getCrossConnectionId().getValue()
                                    .equals(xc.getCrossConnectionId().getValue())).findFirst();
                    if (!oXC.isPresent()) {
                        readyResource.getXcList().add(xc);
                    }
                }

                return changedNode;
            }
        }

        if (actionType.equals(ImplActionType.Implement)) {
            String errMsg = String.format("%s %s", XC_ERROR, xcId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errMsg);
        } else {
            // Deimplement may still need to remove a device-side media-channel
            // after cfg has already lost the XC. Fall back to OP for the exact
            // target XC only; never copy OP's full XC list into changedObject.
            log.debug("cannot find the XC from cfg, try fetch it on OP {}", xcId);
            Node opNode = phyNodeDao.getOpPhyNodeById(PhysicalXcIdNamingRule.getNodeId(xcId));
            if (opNode != null && opNode.getAugmentation(Node1.class) != null
                    && opNode.getAugmentation(Node1.class).getPhysical() != null
                    && opNode.getAugmentation(Node1.class).getPhysical().getCrossConnections() != null) {
                Physical changedNodeAttr = changedNode.getAugmentation(Node1.class).getPhysical();
                List<CrossConnections> xcList = changedNodeAttr.getCrossConnections();
                DeimplementOpXcFallback.addOnlyTargetOpXc(xcList,
                        opNode.getAugmentation(Node1.class).getPhysical().getCrossConnections(), xcId);
                if (!xcList.isEmpty()) {
                    changedNode = new NodeBuilder(changedNode).addAugmentation(Node1.class,
                                    new Node1Builder()
                                            .setPhysical(new PhysicalBuilder(changedNodeAttr)
                                                    .setCrossConnections(xcList)
                                                    .build())
                                            .build())
                            .build();
                    return changedNode;
                }
            }
            log.error("cannot find required XC in cfg/op node during deimplement: {}", xcId);
        }

        return changedNode;
    }

    private void constructImplAseNode(Node cfgNode, Node changedNode,
            Map<String, Node> cfgNodeMap) {
        Physical nodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();

        List<CrossConnections> aseXcList = nodeAttr.getCrossConnections().stream()
                .filter(xc -> xc.getDescription().startsWith("ASE"))
                .map(xc -> {
                    CrossConnectionsBuilder xcBuilder = new CrossConnectionsBuilder(xc)
                            .setAdminState(AdminStatus.Up)
                            .setImplementState(ImplementState.Implement)
                            .setOperationalState(null)
                            .setWssChannel(xc.getWssChannel());
                    return xcBuilder.build();
                }).collect(Collectors.toList());

        Physical changedNodeAttr = changedNode.getAugmentation(Node1.class).getPhysical();
        changedNode = new NodeBuilder(changedNode).addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(changedNodeAttr)
                                .setCrossConnections(aseXcList)
                                .build())
                        .build())
                .build();
        log.debug("ASE XC has ready on node");

        //because the ASE xc is based on frequency, the A/Z TP is same for all of aseXC
        changedNode = makeTpUp(cfgNode.getTerminationPoint(), changedNode, aseXcList.get(0));
        log.debug("ASE TP has ready on node");

        cfgNodeMap.put(cfgNode.getNodeId().getValue(), changedNode);
    }

    private Node makeTpUp(List<TerminationPoint> orgTpList, Node changedNode,
            CrossConnections aseXC) {
        //make the special TP up
        String srcTp = aseXC.getSourceTp().get(0).getTpRef().getValue();
        String dstTp = aseXC.getDestinationTp().get(0).getTpRef().getValue();

        TerminationPoint newSrcTp = findAndMakeTpUp(orgTpList, srcTp);
        TerminationPoint newDstTp = findAndMakeTpUp(orgTpList, dstTp);

        List<TerminationPoint> newTpList = new ArrayList<>();
        newTpList.add(newSrcTp);
        newTpList.add(newDstTp);
        changedNode = new NodeBuilder(changedNode).setTerminationPoint(newTpList)
                .build();
        return changedNode;
    }

    private TerminationPoint findAndMakeTpUp(List<TerminationPoint> tpList, String tpId) {
        Optional<TerminationPoint> tpOp = tpList.stream()
                .filter(x -> x.getTpId().getValue().equals(tpId)).findFirst();
        if (tpOp.isPresent()) {
            TerminationPoint tp = tpOp.get();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                    TerminationPoint1.class).getPhysical();
            return new TerminationPointBuilder(tp)
                    .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                            .setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                            tpAttr)
                                            .setAdminState(AdminStatus.Up)
                                            .setImplementState(ImplementState.Implement)
                                            .build())
                            .build())
                    .build();
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find ASE XC related TP, this is impossible " + tpId);
        }
    }

    public void constructCfgNodeByLink(Node orgCfgNode, Map<String, Node> cfgNodeMap,
            String linkId) {
        Node changedNode = getEmptyChangedNode(orgCfgNode, cfgNodeMap);
        Node newNode = extractChangedLink(orgCfgNode, changedNode, linkId);
        cfgNodeMap.put(orgCfgNode.getNodeId().getValue(), newNode);
    }

    private Node extractChangedLink(Node cfgNode, Node changedNode, String linkId) {
        Physical nodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getInternalLinks() == null) {
            return changedNode;
        }

        InternalLinks newIl = null;
        for (InternalLinks il : nodeAttr.getInternalLinks()) {
//            if (linkId.endsWith("-MPO")) {
//                for (int i = 1; i <= 8; i++) {
//                    String newLinkId = linkId.replaceAll("-MPO", "-MPO" + i);
//                    changedNode = extractChangedLink(cfgNode, changedNode, newLinkId);
//                }
//                return changedNode;
//            }
            if (il.getLinkRef() == null) {
                continue;
            }

            if (il.getLinkRef().equals(linkId)) {
                if (needWrite2Ne(il)) {
                    newIl = new InternalLinksBuilder(il)
                            .setAdminState(isImplementAction ? AdminStatus.Up : AdminStatus.Down)
                            .setImplementState(isImplementAction ? ImplementState.Implement
                                    : ImplementState.Allocate)
                            .build();

                    Physical changedNodeAttr = changedNode.getAugmentation(Node1.class)
                            .getPhysical();
                    List<InternalLinks> ilList = changedNodeAttr.getInternalLinks();
                    ilList.add(newIl);
                    changedNode = new NodeBuilder(changedNode).addAugmentation(Node1.class,
                                    new Node1Builder()
                                            .setPhysical(new PhysicalBuilder(changedNodeAttr)
                                                    .setInternalLinks(ilList)
                                                    .build())
                                            .build())
                            .build();
                } else {
                    ReadyResource readyResource = getReadyResourceWithNodeId(
                            cfgNode.getNodeId().getValue());
                    Optional<InternalLinks> oIL = readyResource.getIlList().stream()
                            .filter(t -> t.getLinkRef().equals(il.getLinkRef())).findFirst();
                    if (!oIL.isPresent()) {
                        readyResource.getIlList().add(il);
                    }
                }
                return changedNode;
            }
        }

        log.error("impossible, cannot find required internalLink {}", linkId);
        //skip this error
        InternalLinks fake = new InternalLinksBuilder()
                .setLinkRef(linkId)
                .setLinkName(linkId)
                .build();

        ReadyResource readyResource = getReadyResourceWithNodeId(cfgNode.getNodeId().getValue());
        readyResource.getIlList().add(fake);

        return changedNode;
//        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find out required Link " + linkId);
    }

    public void constructCfgNodeOcm(Node orgNode, Map<String, Node> cfgNodeMap) {
        Node changedNode = getEmptyChangedNode(orgNode, cfgNodeMap);
        Physical nodeAttr = orgNode.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getOCMGripGroups() != null && !nodeAttr.getOCMGripGroups().isEmpty()) {
            //has flex info. need update
            Physical changedNodeAttr = changedNode.getAugmentation(Node1.class).getPhysical();
            Node newNode = new NodeBuilder(changedNode).addAugmentation(Node1.class,
                            new Node1Builder()
                                    .setPhysical(new PhysicalBuilder(changedNodeAttr)
                                            .setOCMGripGroups(nodeAttr.getOCMGripGroups())
                                            .build())
                                    .build())
                    .build();

            cfgNodeMap.put(orgNode.getNodeId().getValue(), newNode);
        }
    }

    private boolean needWrite2Ne(CrossConnections cfgXc) {
        log.debug("checking XC: {} {}", cfgXc.getCrossConnectionId().getValue(), cfgXc.getImplementState());
        return true;
    }

    private boolean needWrite2Ne(InternalLinks cfgIL) {
        log.debug("checking il: {} {}", cfgIL.getLinkRef(), cfgIL.getImplementState());
        if (!isImplementAction && cfgIL.getLinkRef().contains("MPO")) {
            log.debug("toAllocate, and include MPO");
            return false;
        }
        return true;
    }

    private boolean needWrite2Ne(TerminationPoint cfgTp) {
        log.debug("checking TP: {} {}", cfgTp.getTpId().getValue(), cfgTp.getAugmentation(TerminationPoint1.class).getPhysical().getImplementState());
        return true;
        //TP point must rewrite again.
//        if (isImplementAction) {
//            return true;
//        }
//
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical cfgAttr =
//                cfgTp.getAugmentation(TerminationPoint1.class).getPhysical();
//        ImplementState target =
//                isImplementAction ? ImplementState.Implement : ImplementState.Allocate;
//
//        if (!cfgAttr.getImplementState().equals(target)) {
//            return true;
//        }
//
//        return false;
    }

    private boolean needWrite2Ne(Equipments cfgEq) {
        log.debug("checking EQ: {} {}", cfgEq.getEquipmentId(), cfgEq.getImplementState());
        return true;
//        ImplementState target =
//                isImplementAction ? ImplementState.Implement : ImplementState.Allocate;
////        if (target.equals(ImplementState.Allocate)) {
////            if (cfgEq.getEquipType().equals(EquipType.ILA) || cfgEq.getEquipType().equals(EquipType.OA)) {
////                //ILA , OA 板卡不能关闭, 有些场景通告OSC 通道过去的IP，关闭后网元就彻底脱管了
////              if (neAddressisOSCAddress(orgOpNode, cfgEq)) {
////                return false;
////              }
////            }
////            if (cfgEq.getEquipType().equals(EquipType.TRANSCEIVER) && cfgEq.getEquipmentId().contains("OSC")) {
////                return false;
////            }
////        }
//        if (!cfgEq.getImplementState().equals(target)) {
//            return true;
//        }
//
//        return false;
    }

    public void constructCfgNodeByOcm(Node orgCfgNode, Map<String, Node> cfgChangedNodeMap) {
        Node emptyNode = getEmptyChangedNode(orgCfgNode, cfgChangedNodeMap);
        cfgChangedNodeMap.put(orgCfgNode.getNodeId().getValue(), emptyNode);
    }

    //已经是目标状态的对象ID放在相关列表中
    public class ReadyResource {

        List<TerminationPoint> tpList;
        List<CrossConnections> xcList;
        List<Equipments> eqList;
        List<InternalLinks> ilList;

        public ReadyResource() {
            tpList = new ArrayList<>();
            xcList = new ArrayList<>();
            eqList = new ArrayList<>();
            ilList = new ArrayList<>();
        }

        public List<TerminationPoint> getTpList() {
            return tpList;
        }

        public List<CrossConnections> getXcList() {
            return xcList;
        }

        public List<Equipments> getEqList() {
            return eqList;
        }

        public List<InternalLinks> getIlList() {
            return ilList;
        }
    }
}

final class DeimplementOpXcFallback {
    private DeimplementOpXcFallback() {
    }

    static void addOnlyTargetOpXc(List<CrossConnections> changedXcs, List<CrossConnections> opXcs,
                                  String xcId) {
        opXcs.stream()
                .filter(xc -> xc.getCrossConnectionId().getValue().equals(xcId))
                .findFirst()
                .ifPresent(changedXcs::add);
    }
}
