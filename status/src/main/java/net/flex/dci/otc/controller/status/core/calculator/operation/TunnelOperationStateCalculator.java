package net.flex.dci.otc.controller.status.core.calculator.operation;

import java.util.ArrayList;
import java.util.Arrays;
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
import net.flex.dci.otc.controller.status.dto.operation.TunnelsOperState;
import net.flex.dci.otc.controller.status.dto.operation.TunnelsOperState.TunnelOperState;
import net.flex.dci.otc.controller.status.util.LinkHelper;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/8 16:41
 */
@Component
@Slf4j
public class TunnelOperationStateCalculator extends
        AbstractLinkOperationStateCalculator<TunnelsOperState, LinkStateDto> {


    public TunnelOperationStateCalculator(LinkHelper linkHelper,
            ConnectionCacheManager connectionCacheManager, NodeCacheManager nodeCacheManager) {
        super(linkHelper, connectionCacheManager, nodeCacheManager);
    }

    @Override
    public TunnelsOperState calculate(List<LinkStateDto> tunnels) {
        log.debug("state to calculate tunnel operation state ");

        Set<String> allOchLinkIds = new HashSet<>();
        Set<String> allTpIds = new HashSet<>();
        for (LinkStateDto tunnel : tunnels) {
            allOchLinkIds.addAll(tunnel.getSupportingLink());
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

        List<TunnelOperState> tunnelOperStates = tunnels.stream()
                .map(tunnel -> calculateTunnelOperStates(tunnel, ochLinkMap, tpMap, equipMap))
                .collect(Collectors.toList());
        return TunnelsOperState.builder().tunnelOperStates(tunnelOperStates).build();
    }

    @Override
    public TunnelsOperState calculate(List<LinkStateDto> tunnels, OperStatus operStatus) {
        return null;
    }

    private TunnelOperState calculateTunnelOperStates(LinkStateDto tunnel,
            Map<String, LinkStateDto> ochLinkMap,
            Map<String, TerminationPoint> tpMap,
            Map<String, Equipments> equipMap) {
        log.debug("calculate Tunnel operStatus tunnelId :{}", tunnel.getId());
        String tunnelId = tunnel.getId();
        List<String> supportingLinks = tunnel.getSupportingLink();
        List<OperStatus> operStatuses = new ArrayList<>();
        for (String linkId : supportingLinks) {
            LinkStateDto ochLink = ochLinkMap.get(linkId);
            if (ochLink != null) {
                operStatuses.add(ochLink.getOperStatus());
            } else {
                operStatuses.add(OperStatus.Unknown);
            }
        }
        OperStatus clientPortOperStatues = getClientPortOperStatus(tunnel, tpMap, equipMap);
        OperStatus currentOperStatus = StatusUtil.calculateOperState(
                operStatuses);
        currentOperStatus = StatusUtil.calculateOperState(clientPortOperStatues,
                currentOperStatus);
        return TunnelOperState.builder().operStatus(currentOperStatus).tunnelId(tunnelId).build();
    }

    private OperStatus getClientPortOperStatus(LinkStateDto tunnel,
            Map<String, TerminationPoint> tpMap,
            Map<String, Equipments> equipMap) {
        String tunnelId = tunnel.getId();
        log.debug("get tunnel  state from tunnel:{}", tunnelId);
        String sourceTpId = TunnelIdNamingRule.getATp(tunnelId);
        String destTpId = TunnelIdNamingRule.getZtp(tunnelId);
        OperStatus sourceOperState = getTpRefOperStateFromCache(sourceTpId, tpMap, equipMap);
        OperStatus destOperState = getTpRefOperStateFromCache(destTpId, tpMap, equipMap);
        return StatusUtil.calculateOperState(sourceOperState, destOperState);
    }

    private OperStatus getTpRefOperStateFromCache(String tpId,
            Map<String, TerminationPoint> tpMap,
            Map<String, Equipments> equipMap) {
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);

        TerminationPoint tp = tpMap.get(tpId);
        Physical tpPhysical =
                tp != null ? tp.getAugmentation(TerminationPoint1.class).getPhysical() : null;
        OperStatus tpOper = tpPhysical != null && tpPhysical.getOperationalState() != null
                ? tpPhysical.getOperationalState() : OperStatus.Unknown;

        Equipments equipment = equipMap.get(neId + ":" + eqId);
        OperStatus eqOper = equipment != null && equipment.getOperationalState() != null
                ? equipment.getOperationalState() : OperStatus.Unknown;

        return StatusUtil.calculateOperState(Arrays.asList(tpOper, eqOper));
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
