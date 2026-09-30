package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.Wss;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 7/9/2025 3:38 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProgrammableMuxConstructor implements MuxConstructor {

    private final NetconfTopology netconfTopology;

    @Override
    public MUX constructMux(NeYangModel model, Equipments equipment, List<Link> ochLinks) {
        log.debug(" construct programmable mux equipType is :{} neYangModel ：{} actualType:{} ",
                equipment.getEquipmentId(), model, equipment.getEquipTypeInstalled());
        List<CrossConnections> refCrossConnections = getRefEquipmentCrossConnections(
                equipment.getEquipmentId());
        List<CrossConnections> programmableXcs = getProgrammableXcs(refCrossConnections);
        return new Wss(model, equipment.getEquipmentId(), ochLinks, programmableXcs);
    }

    private List<CrossConnections> getProgrammableXcs(List<CrossConnections> crossConnections) {
        log.debug("get programmable cross connections");
        return crossConnections.stream()
                .filter(xc -> xc.getWssChannel() != null).collect(
                        Collectors.toList());
    }

    private List<CrossConnections> getRefEquipmentCrossConnections(String equipmentId) {
        log.debug("get ref equipment cross connections:{}", equipmentId);
        String refNodeId = PhysicalEqpIdNamingRule.getNodeId(equipmentId);
        Node phyNode = netconfTopology.getPhyNode(refNodeId);
        List<CrossConnections> crossConnections = phyNode.getAugmentation(
                Node1.class).getPhysical().getCrossConnections();
        List<CrossConnections> refCrossConnections = crossConnections.stream()
                .filter(xc -> xc.getCrossConnectionId().getValue().contains(equipmentId)).collect(
                        Collectors.toList());
        return refCrossConnections;
    }

    @Override
    public List<EquipType> supportEquipType() {
        return Arrays.asList(EquipType.WSS, EquipType.IRA, EquipType.DGE);
    }
}
