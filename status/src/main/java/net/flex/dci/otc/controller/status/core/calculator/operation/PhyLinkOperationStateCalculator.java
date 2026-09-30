package net.flex.dci.otc.controller.status.core.calculator.operation;

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
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.dto.operation.PhyLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.PhyLinksOperState.PhyLinkOperState;
import net.flex.dci.otc.controller.status.util.LinkHelper;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/8 16:40
 */
@Component
@Slf4j
public class PhyLinkOperationStateCalculator extends
        AbstractLinkOperationStateCalculator<PhyLinksOperState, LinkStateDto> {


    public PhyLinkOperationStateCalculator(LinkHelper linkHelper,
            ConnectionCacheManager connectionCacheManager,
            NodeCacheManager nodeCacheManager) {
        super(linkHelper, connectionCacheManager, nodeCacheManager);
    }

    @Override
    public PhyLinksOperState calculate(List<LinkStateDto> links) {
        log.debug(
                "start to calculate the phy links operation state,operation state ref link is :{}",
                links);

        Set<String> allNodeIds = new HashSet<>();
        Set<String> allTpIds = new HashSet<>();
        Set<String> allEquipIds = new HashSet<>();

        for (LinkStateDto link : links) {
            String linkId = link.getId();
            String srcTpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
            String destTpId = PhysicalLinkIdNamingRule.getTpZId(linkId);
            String sourceNodeId = PhysicalLinkIdNamingRule.getNodeAId(linkId);
            String destNodeId = PhysicalLinkIdNamingRule.getNodeZId(linkId);
            String sourceEquipId = PhysicalTpIdNamingRule.getEquipId(srcTpId);
            String destEquipId = PhysicalTpIdNamingRule.getEquipId(destTpId);

            allNodeIds.add(sourceNodeId);
            allNodeIds.add(destNodeId);
            allTpIds.add(srcTpId);
            allTpIds.add(destTpId);
            allEquipIds.add(sourceNodeId + ":" + sourceEquipId);
            allEquipIds.add(destNodeId + ":" + destEquipId);
        }

        Map<String, Node> nodeMap = nodeCacheManager.getOpNodes(new ArrayList<>(allNodeIds));
        Map<String, TerminationPoint> tpMap = batchGetTerminationPoints(nodeMap, allTpIds);
        Map<String, Equipments> equipMap = batchGetEquipments(nodeMap, allEquipIds);

        List<PhyLinkOperState> linkOperStates = links.stream()
                .map(link -> calculatePhyLinkOperState(link, nodeMap, tpMap, equipMap))
                .collect(Collectors.toList());
        log.debug("the ref link oper states is:{}", linkOperStates);
        return PhyLinksOperState.builder().phyLinkOperStates(linkOperStates).build();
    }

    @Override
    public PhyLinksOperState calculate(List<LinkStateDto> links, OperStatus operStatus) {
        log.debug("start to calculate the phy links operation state,operation state :{} links:{}",
                operStatus, links);
        List<PhyLinkOperState> linkOperStates = links.stream()
                .map(link -> calculateCurrentOperStatus(link, operStatus))
                .collect(Collectors.toList());
        log.debug("update the ref link oper states is:{}", linkOperStates);
        return PhyLinksOperState.builder().phyLinkOperStates(linkOperStates).build();
    }

    private PhyLinkOperState calculateCurrentOperStatus(LinkStateDto link, OperStatus operStatus) {
        String linkId = link.getId();
        log.debug("start to calculate current phy link oper state:{} and update operStatus is:{}",
                linkId, operStatus);
        OperStatus oldOperStatus = link.getOperStatus();
        OperStatus currentOperStatus = StatusUtil.calculateOperState(oldOperStatus, operStatus);
        return PhyLinkOperState.builder().phyLinkId(linkId).operStatus(currentOperStatus).build();

    }

    private PhyLinkOperState calculatePhyLinkOperState(LinkStateDto link,
            Map<String, Node> nodeMap,
            Map<String, TerminationPoint> tpMap,
            Map<String, Equipments> equipMap) {
        String linkId = link.getId();
        log.debug("start to calculate the phy link oper state:{}", linkId);
        String srcTpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
        String destTpId = PhysicalLinkIdNamingRule.getTpZId(linkId);
        String sourceNodeId = PhysicalLinkIdNamingRule.getNodeAId(linkId);
        String destNodeId = PhysicalLinkIdNamingRule.getNodeZId(linkId);
        String sourceEquipId = PhysicalTpIdNamingRule.getEquipId(srcTpId);
        String destEquipId = PhysicalTpIdNamingRule.getEquipId(destTpId);

        if (isPhyLinkDown(sourceNodeId, destNodeId, nodeMap)) {
            return PhyLinkOperState.builder().phyLinkId(linkId).operStatus(OperStatus.Down).build();
        }

        OperStatus equipmentOperStatus = calculateEquipmentOperStatus(sourceNodeId, sourceEquipId,
                destNodeId, destEquipId, equipMap);
        if (!equipmentOperStatus.equals(OperStatus.Up)) {
            return PhyLinkOperState.builder().phyLinkId(linkId).operStatus(equipmentOperStatus)
                    .build();
        }

        OperStatus tpOperStatus = calculateTerminationPointOperStatus(sourceNodeId, srcTpId,
                destNodeId, destTpId, tpMap);

        return PhyLinkOperState.builder().phyLinkId(linkId).operStatus(tpOperStatus).build();

    }

    private OperStatus calculateTerminationPointOperStatus(String sourceNodeId, String srcTpId,
            String destNodeId, String destTpId, Map<String, TerminationPoint> tpMap) {
        log.debug(
                "calculate terminationPointOperStatus sourceNodeId:{} sourceTp:{} destinationNode:{} destinationTpId:{}",
                sourceNodeId, srcTpId, destNodeId, destTpId);
        TerminationPoint sourceTp = tpMap.get(srcTpId);
        TerminationPoint destTp = tpMap.get(destTpId);
        OperStatus sourceTpOper =
                sourceTp != null ? sourceTp.getAugmentation(TerminationPoint1.class).getPhysical()
                        .getOperationalState() : OperStatus.Unknown;
        OperStatus destTpOper =
                destTp != null ? destTp.getAugmentation(TerminationPoint1.class).getPhysical()
                        .getOperationalState() : OperStatus.Unknown;
        return StatusUtil.calculateOperState(sourceTpOper, destTpOper);
    }

    private OperStatus calculateEquipmentOperStatus(String sourceNodeId, String sourceEquipId,
            String destNodeId, String destEquipId, Map<String, Equipments> equipMap) {
        String sourceKey = sourceNodeId + ":" + sourceEquipId;
        String destKey = destNodeId + ":" + destEquipId;
        Equipments sourceEquip = equipMap.get(sourceKey);
        Equipments destEquip = equipMap.get(destKey);
        OperStatus sourceEquipOper =
                sourceEquip != null ? sourceEquip.getOperationalState() : OperStatus.Unknown;
        OperStatus destEquipOper =
                destEquip != null ? destEquip.getOperationalState() : OperStatus.Unknown;
        return StatusUtil.calculateOperState(sourceEquipOper, destEquipOper);
    }

    private boolean isPhyLinkDown(String sourceNodeId, String destNodeId,
            Map<String, Node> nodeMap) {
        log.debug("is the phy link already down ?");
        Node sourceNode = nodeMap.get(sourceNodeId);
        Node destNode = nodeMap.get(destNodeId);
        if (sourceNode == null || destNode == null) {
            return true;
        }
        OperStatus sourceOper = sourceNode.getAugmentation(Node1.class).getPhysical()
                .getOperationalState();
        OperStatus destOper = destNode.getAugmentation(Node1.class).getPhysical()
                .getOperationalState();

        OperStatus operStatus = StatusUtil.calculateOperState(sourceOper, destOper);

        if (operStatus.equals(OperStatus.SynchronizationException)
                || operStatus.equals(OperStatus.NeCommunicationException)) {
            return true;
        }
        return false;
    }

    private Map<String, TerminationPoint> batchGetTerminationPoints(Map<String, Node> nodeMap,
            Set<String> tpIds) {
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

    private Map<String, Equipments> batchGetEquipments(Map<String, Node> nodeMap,
            Set<String> equipKeys) {
        Map<String, Equipments> result = new HashMap<>();
        for (String key : equipKeys) {
            String[] parts = key.split(":");
            String nodeId = parts[0];
            String eqId = parts[1];
            Node node = nodeMap.get(nodeId);
            if (node != null) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical = node.getAugmentation(
                        Node1.class).getPhysical();
                if (physical != null && physical.getEquipments() != null) {
                    for (Equipments equip : physical.getEquipments()) {
                        if (eqId.equals(equip.getEquipmentId())) {
                            result.put(key, equip);
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }
}
