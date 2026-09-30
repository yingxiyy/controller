package net.flex.dci.otc.controller.status.core.calculator.align;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.dto.align.TunnelsAlignState;
import net.flex.dci.otc.controller.status.dto.align.TunnelsAlignState.TunnelAlignState;
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
 * @date 2022/4/8 12:40
 */
@Component
@Slf4j
public class TunnelAlignStateCalculator extends
        AbstractAlignStateCalculator<TunnelsAlignState, LinkStateDto> {


    private final NodeCacheManager nodeCacheManager;

    private final ConnectionCacheManager connectionCacheManager;

    protected TunnelAlignStateCalculator(TerminationPointDao terminationPointDao,
            EquipmentsDao equipmentsDao, NodeCacheManager nodeCacheManager,
            PhyNodeDao phyNodeDao1, NodeCacheManager nodeCacheManager1,
            ConnectionCacheManager connectionCacheManager) {
        super(terminationPointDao, equipmentsDao, nodeCacheManager, phyNodeDao1);
        this.nodeCacheManager = nodeCacheManager1;
        this.connectionCacheManager = connectionCacheManager;
    }


    @Override
    public TunnelsAlignState calculateAlignState(List<LinkStateDto> tunnels) {
        log.debug("start to calculate the tunnel alarm state ,tunnel id is :{}", tunnels);

        Set<String> allOchLinkIds = new HashSet<>();
        Set<String> allTpIds = new HashSet<>();
        for (LinkStateDto tunnel : tunnels) {
            for (String supportingLink : tunnel.getSupportingLink()) {
                allOchLinkIds.add(supportingLink);
            }
//            if (!tunnel.getSourceTp().isEmpty()) {
//                allTpIds.add(tunnel.getSourceTp().get(0).getTpRef().getValue());
//            }
//            if (!tunnel.getDestinationTp().isEmpty()) {
//                allTpIds.add(tunnel.getDestinationTp().get(0).getTpRef().getValue());
//            }
            String tunnelId = tunnel.getId();
            String sourceTpId = TunnelIdNamingRule.getATp(tunnelId);
            String destTpId = TunnelIdNamingRule.getZtp(tunnelId);
            allTpIds.add(sourceTpId);
            allTpIds.add(destTpId);
        }

        Map<String, LinkStateDto> ochLinkMap = batchGetOchLinks(allOchLinkIds);
        Set<String> nodeIds = allTpIds.stream()
                .map(PhysicalTpIdNamingRule::getNodeId)
                .collect(Collectors.toSet());
        Map<String, Node> nodeMap = nodeCacheManager.getOpNodes(new ArrayList<>(nodeIds));
        Map<String, TerminationPoint> tpMap = batchGetTerminationPointsFromNodes(allTpIds, nodeMap);
        Map<String, Equipments> equipMap = batchGetEquipmentsFromNodes(allTpIds, nodeMap);

        List<TunnelAlignState> tunnelAlarmStates = tunnels.stream()
                .map(tunnel -> calculateTunnelAlign(tunnel, ochLinkMap, tpMap, equipMap))
                .collect(Collectors.toList());
        return TunnelsAlignState.builder().tunnelAlignStates(tunnelAlarmStates).build();
    }

    private TunnelAlignState calculateTunnelAlign(LinkStateDto tunnel,
            Map<String, LinkStateDto> ochLinkMap,
            Map<String, TerminationPoint> tpMap,
            Map<String, Equipments> equipMap) {
        String tunnelId = tunnel.getId();
        log.debug("calculate tunnel align for tunnel, tunnel id is:{}", tunnelId);
        List<String> supportingLinks = tunnel.getSupportingLink();
        List<AlignmentStatusType> alignmentStatusTypes = new ArrayList<>();
        for (String linkId : supportingLinks) {
            LinkStateDto ochLink = ochLinkMap.get(linkId);
            if (ochLink == null) {
                alignmentStatusTypes.add(AlignmentStatusType.Unknown);
            } else {
                alignmentStatusTypes.add(
                        ochLink.getAlignment());
            }
        }
        AlignmentStatusType clientPortAlignment = getClientPortAlignmentStatus(tunnel, tpMap,
                equipMap);
        AlignmentStatusType currentAlignmentStatusType = StatusUtil.calculateAlignStatus(
                alignmentStatusTypes);
        currentAlignmentStatusType = StatusUtil.calculateAlignStatus(clientPortAlignment,
                currentAlignmentStatusType);
        return TunnelAlignState.builder().tunnelId(tunnelId)
                .alignmentStatusType(currentAlignmentStatusType).build();
    }

    private AlignmentStatusType getClientPortAlignmentStatus(LinkStateDto tunnel,
            Map<String, TerminationPoint> tpMap,
            Map<String, Equipments> equipMap) {
        String tunnelId = tunnel.getId();
        log.debug("get tunnel  state from tunnel:{}", tunnelId);
        String sourceTpId = TunnelIdNamingRule.getATp(tunnelId);
        String destTpId = TunnelIdNamingRule.getZtp(tunnelId);
        AlignmentStatusType sourceAlignmentStatusType = getTpRefAlignmentFromCache(sourceTpId,
                tpMap, equipMap);
        AlignmentStatusType destAlignmentStatusType = getTpRefAlignmentFromCache(destTpId, tpMap,
                equipMap);
        return StatusUtil.calculateAlignStatus(sourceAlignmentStatusType, destAlignmentStatusType);
    }

    private AlignmentStatusType getTpRefAlignmentFromCache(String tpId,
            Map<String, TerminationPoint> tpMap,
            Map<String, Equipments> equipMap) {
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
        String transceiverId = PhysicalTpIdNamingRule.getTransceiverIdWithNamingRule(tpId);

        TerminationPoint tp = tpMap.get(tpId);
        Physical tpPhysical =
                tp != null ? tp.getAugmentation(TerminationPoint1.class).getPhysical() : null;

        Node node = nodeCacheManager.getOpNode(neId);
        List<Equipments> equipments =
                node != null ? node.getAugmentation(Node1.class).getPhysical().getEquipments()
                        : new ArrayList<>();

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

        AlignmentStatusType eqAlign = refEquipment != null ? refEquipment.getAlignmentStatus()
                : AlignmentStatusType.Unknown;
        alignmentStatusTypeList.add(eqAlign);

        if (transceiver != null && transceiver.getAlignmentStatus() != null) {
            alignmentStatusTypeList.add(transceiver.getAlignmentStatus());
        }

        return StatusUtil.calculateAlignStatus(alignmentStatusTypeList);
    }

    private Map<String, LinkStateDto> batchGetOchLinks(Set<String> ochLinkIds) {
        return connectionCacheManager.batchGetOchLinksByIds(new ArrayList<>(ochLinkIds));
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

    private Map<String, Equipments> batchGetEquipmentsFromNodes(Set<String> tpIds,
            Map<String, Node> nodeMap) {
        Map<String, Equipments> result = new HashMap<>();
        Set<String> processedNodeIds = new HashSet<>();
        for (String tpId : tpIds) {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
            if (!processedNodeIds.contains(nodeId)) {
                Node node = nodeMap.get(nodeId);
                if (node != null) {
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                            node.getAugmentation(Node1.class).getPhysical();
                    if (physical != null && physical.getEquipments() != null) {
                        for (Equipments equip : physical.getEquipments()) {
                            result.put(nodeId + ":" + equip.getEquipmentId(), equip);
                        }
                    }
                }
                processedNodeIds.add(nodeId);
            }
        }
        return result;
    }
}
