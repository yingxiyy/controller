//package net.flex.dci.otc.controller.ne.manager.core.state.impl;
//
//import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
//import static net.flex.dci.otc.common.constants.Constants.LINE_CARD;
//import static net.flex.dci.otc.common.constants.Constants.TRANSCEIVER;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import lombok.Builder;
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
//import net.flex.dci.otc.controller.ne.manager.core.state.AlignStateSynchronize;
//import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
//import net.flex.dci.otc.mongo.dao.PhyNodeDao;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
//import org.springframework.stereotype.Component;
//
/// **
// * calculate the ne tp and equipment align state
// *
// * @version 1.0
// * @date 2022/5/31 10:00
// */
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class NeAlignStateSynchronizer implements AlignStateSynchronize {
//
//
//    private final PhyNodeDao phyNodeDao;
//
//    private final Map<String, AlignState> alignStateMap = new HashMap<>();
//
//
//    @Override
//    public void syncAlignState(Node node) {
//        log.info("synchronize the node state");
//        log.debug("synchronize the node equipment and termination point state from the real ne:{}",
//                node);
//        //calculate
//        //calculate the tp align state
//        AlignmentStatusType alignmentStatus = calculateNeAlignment(node);
//
//        List<TerminationPoint> updateAlignmentTps = syncTerminationPointAlignState(node);
//
//        PhysicalBuilder phyBuilder = new PhysicalBuilder();
//        phyBuilder.setAlignmentStatus(alignmentStatus);
//
//        NodeBuilder nodeBuilder = new NodeBuilder();
//        Node1Builder node1Builder = new Node1Builder();
//        node1Builder.setPhysical(phyBuilder.build());
//        nodeBuilder.setKey(new NodeKey(node.getNodeId()));
//        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());
//        nodeBuilder.setTerminationPoint(updateAlignmentTps);
//        phyNodeDao.saveOpPhyNode(nodeBuilder.build());
//        alignStateMap.clear();
//    }
//
//    private List<TerminationPoint> syncTerminationPointAlignState(Node node) {
//        log.debug("sync tp align state");
//        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
//        List<TerminationPoint> syncTps = new ArrayList<>();
//        terminationPoints.stream().forEach(terminationPoint -> {
//            TerminationPoint syncTp = syncTp(terminationPoint);
//            syncTps.add(syncTp);
//        });
//        return syncTps;
//    }
//
//    private TerminationPoint syncTp(TerminationPoint terminationPoint) {
//
//        log.debug("sync the termination point is :{}", terminationPoint.getTpId().getValue());
//        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder(
//                terminationPoint);
//        Physical tpPhysical = terminationPoint.getAugmentation(TerminationPoint1.class)
//                .getPhysical();
//        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder tpPhysicalBuilder =
//                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
//                        tpPhysical);
//
//        String tpId = terminationPoint.getTpId().getValue();
//        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
//        AlignState refEquAlignState = alignStateMap.get(equipId);
//        //todo:temp method
//        if (refEquAlignState == null) {
//            return terminationPoint;
//        }
//        EquipType refEquipType = refEquAlignState.getEquipType();
//        AlignmentStatusType alignmentStatus = refEquAlignState.alignmentStatusType;
//        if (refEquipType.equals(EquipType.OT)) {
//            String refTransceiverId = getRefTransceiverEquipId(equipId, tpId);
//            AlignState alignState = alignStateMap.get(refTransceiverId);
//            if (alignState != null) {
//                alignmentStatus = alignState.alignmentStatusType;
//            } else {
//                alignmentStatus = AlignmentStatusType.ModularLack;
//            }
//        }
//        tpPhysicalBuilder.setAlignmentStatus(alignmentStatus);
//
//        terminationPointBuilder.addAugmentation(TerminationPoint1.class,
//                new TerminationPoint1Builder().setPhysical(tpPhysicalBuilder.build()).build());
//        return terminationPointBuilder.build();
//
//    }
//
//    private String getRefTransceiverEquipId(String equipId, String tpId) {
//        log.debug("get ref transceiver equip id for port:{}", tpId);
//        String[] tpArrays = tpId.split(HYPHEN);
//        String portSimpleName = tpArrays[tpArrays.length - 1];
//        StringBuilder sb = new StringBuilder();
//        sb.append(equipId.replace(LINE_CARD, TRANSCEIVER))
//                .append(HYPHEN).append(portSimpleName);
//        return sb.toString();
//    }
//
//    /**
//     * calculate the ne alignmentState and  load map for ne
//     *
//     * @param node
//     * @return
//     */
//    private AlignmentStatusType calculateNeAlignment(Node node) {
//        log.debug("calculate the ne alignment for node:{}", node);
//        List<Equipments> equipments = node.getAugmentation(Node1.class).getPhysical()
//                .getEquipments();
//        List<AlignmentStatusType> alignmentStatusTypes = new ArrayList<>();
//        equipments.forEach(equipment -> {
//            AlignmentStatusType alignmentStatusType = equipment.getAlignmentStatus();
//            String equipId = equipment.getEquipmentId();
//            EquipType equipType = equipment.getEquipType();
//            alignmentStatusTypes.add(alignmentStatusType);
//            alignStateMap.put(equipId, AlignState.builder().equipType(equipType)
//                    .alignmentStatusType(alignmentStatusType).build());
//        });
//        return NeManagerUtils.calculateAlignStatus(alignmentStatusTypes);
//    }
//
//
//    @Data
//    @Builder
//    private static class AlignState implements Serializable {
//
//        private AlignmentStatusType alignmentStatusType;
//
//        private EquipType equipType;
//    }
//}
