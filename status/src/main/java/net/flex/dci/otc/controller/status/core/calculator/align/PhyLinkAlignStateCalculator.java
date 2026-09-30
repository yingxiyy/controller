package net.flex.dci.otc.controller.status.core.calculator.align;

import static net.flex.dci.otc.controller.status.util.Constants.VIRTUAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.dto.align.PhyLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.PhyLinksAlignState.PhyLinkAlignState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/7 15:26
 */
@Component
@Slf4j
public class PhyLinkAlignStateCalculator extends
        AbstractAlignStateCalculator<PhyLinksAlignState, LinkStateDto> {


    protected PhyLinkAlignStateCalculator(TerminationPointDao terminationPointDao,
            EquipmentsDao equipmentsDao, NodeCacheManager nodeCacheManager,
            PhyNodeDao phyNodeDao1) {
        super(terminationPointDao, equipmentsDao, nodeCacheManager, phyNodeDao1);
    }

    @Override
    public PhyLinksAlignState calculateAlignState(List<LinkStateDto> phyLinks) {
        log.info("start to calculate the phy link align state, count:{}", phyLinks.size());

        Set<String> allTpIds = new HashSet<>();
        Set<String> allNodeIds = new HashSet<>();
        for (LinkStateDto phyLink : phyLinks) {
            String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(phyLink.getId());
            String destTpId = PhysicalLinkIdNamingRule.getTpZId(phyLink.getId());
            allTpIds.add(sourceTpId);
            allTpIds.add(destTpId);
            allNodeIds.add(PhysicalTpIdNamingRule.getNodeId(sourceTpId));
            allNodeIds.add(PhysicalTpIdNamingRule.getNodeId(destTpId));
        }

        Map<String, Node> nodeMap = batchGetNodes(new ArrayList<>(allNodeIds));
        Map<String, TerminationPoint> tpMap = batchGetTerminationPointsFromNodes(allTpIds, nodeMap);

        List<PhyLinkAlignState> phyLinkAlignStates = phyLinks.stream()
                .map(link -> calculatePhyLinkAlignState(link, tpMap, nodeMap))
                .collect(Collectors.toList());
        log.info("ref phy link align states is:{}", phyLinkAlignStates);
        return PhyLinksAlignState.builder().phyLinkAlignStates(phyLinkAlignStates).build();
    }

    private PhyLinkAlignState calculatePhyLinkAlignState(LinkStateDto phyLink,
            Map<String, TerminationPoint> tpMap,
            Map<String, Node> nodeMap) {
        String phyLinkId = phyLink.getId();
        log.debug("calculate the phy link alignment state :{}", phyLinkId);
        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(phyLink.getId());
        String destinationTpId = PhysicalLinkIdNamingRule.getTpZId(phyLink.getId());

        AlignmentStatusType sourceType = getTpRefAlignmentFromCache(sourceTpId, tpMap, nodeMap);
        AlignmentStatusType destType = getTpRefAlignmentFromCache(destinationTpId, tpMap, nodeMap);
        AlignmentStatusType alignmentStatus = StatusUtil.calculateAlignStatus(sourceType, destType);
        log.debug("current phy link alignment state:{}", phyLinkId);
        return PhyLinkAlignState.builder().linkId(phyLinkId).alignmentStatusType(alignmentStatus)
                .build();
    }

    private AlignmentStatusType getTpRefAlignmentFromCache(String tpId,
            Map<String, TerminationPoint> tpMap,
            Map<String, Node> nodeMap) {
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
        String transceiverId = PhysicalTpIdNamingRule.getTransceiverIdWithNamingRule(tpId);

        Node ne = nodeMap.get(neId);
        if (ne == null) {
            log.warn("current ne id:{} is not found,alignmentStatus unknown", neId);
            return AlignmentStatusType.Unknown;
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                ne.getAugmentation(Node1.class).getPhysical();
        String vendorType = physical != null ? physical.getVendorType() : null;
        if (vendorType != null && vendorType.equals(VIRTUAL)) {
            log.debug("current ne is virtual ne,the neId:{}", neId);
            return AlignmentStatusType.Aligned;
        }

        TerminationPoint tp = tpMap.get(tpId);
        Physical tpPhysical =
                tp != null ? tp.getAugmentation(TerminationPoint1.class).getPhysical() : null;

        List<Equipments> equipments =
                physical != null ? physical.getEquipments() : new ArrayList<>();
        Equipments refEquipment = equipments.stream()
                .filter(equipment -> equipment.getEquipmentId().equals(eqId))
                .findAny().orElse(null);
        Equipments transceiver = equipments.stream()
                .filter(equip -> equip.getEquipmentId().equals(transceiverId))
                .findAny().orElse(null);

        List<AlignmentStatusType> alignmentStatusTypeList = new ArrayList<>();
        AlignmentStatusType tpAlign = tpPhysical != null && tpPhysical.getAlignmentStatus() != null
                ? tpPhysical.getAlignmentStatus() : AlignmentStatusType.Unknown;
        alignmentStatusTypeList.add(tpAlign);
        log.debug("tp:{} ref alignmentStatus :{}", tpId, tpAlign);
        AlignmentStatusType eqAlign = refEquipment != null ? refEquipment.getAlignmentStatus()
                : AlignmentStatusType.Unknown;
        alignmentStatusTypeList.add(eqAlign);
        log.debug("eq:{} ref alignmentStatus :{}", eqId, eqAlign);
        if (transceiver != null && transceiver.getAlignmentStatus() != null) {
            alignmentStatusTypeList.add(transceiver.getAlignmentStatus());
            log.debug("transceiver:{} ref alignmentStatus :{}", transceiver.getEquipmentId(),
                    transceiver.getAlignmentStatus());
        }

        return StatusUtil.calculateAlignStatus(alignmentStatusTypeList);
    }

    private Map<String, Node> batchGetNodes(List<String> nodeIds) {
        Map<String, Node> result = new HashMap<>();
        for (String nodeId : nodeIds) {
            Node node = nodeCacheManager.getOpNode(nodeId);
            if (node == null) {
                log.warn("failed to get node:{} operation cache,use config cache instead", nodeId);
                node = nodeCacheManager.getConfigNode(nodeId);
            }
            if (node != null) {
                result.put(nodeId, node);
            }
        }
        return result;
    }

    private Map<String, TerminationPoint> batchGetTerminationPointsFromNodes(Set<String> tpIds,
            Map<String, Node> nodeMap) {
        Map<String, TerminationPoint> result = new HashMap<>();
        for (String tpId : tpIds) {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            Node node = nodeMap.get(nodeId);
            if (node != null && node.getTerminationPoint() != null) {
                for (TerminationPoint tp : node.getTerminationPoint()) {
                    if (tpId.equals(tp.getTpId().getValue())) {
                        result.put(tpId, tp);
                        break;
                    }
                }
            }
        }
        return result;
    }

}
