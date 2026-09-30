package net.flex.dci.otn.ne.upgrade.tool.components;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * 2026/3/14
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ODNeSubTypeUpgrader implements NeSubTypeUpgrader {

    private final PhyNodeDao phyNodeDao;

    @Override
    public void upgradeNeSubType(List<Node> nodes) {
        log.info("upgrade neSubType nodes size:{}", nodes.size());
        List<String> nodeIds = nodes.stream().map(node -> node.getNodeId().getValue()).collect(
                Collectors.toList());
        log.debug("upgrade neSubType nodes id is:{}", nodeIds);
        nodes.forEach(this::upgradeNeSubType);
    }

    @Override
    public NodeType generalType() {
        return NodeType.OD;
    }

    @Override
    public void upgradeNeSubType(Node node) {
        log.debug("upgrade the ne subType,the node is:{}", node.getNodeId().getValue());
        String neId = node.getNodeId().getValue();
        Physical nodePhysical = node.getAugmentation(Node1.class).getPhysical();
        List<Equipments> refEquipments = nodePhysical.getEquipments();
        Set<EquipType> equipTypes = refEquipments.stream()
                .map(PhyEquipAttributes::getEquipType).collect(
                        Collectors.toSet());
        NeSubType neSubType = null;
        if (equipTypes.contains(EquipType.ILA)) {
            neSubType = NeSubType.OPC_ILA;
        } else if (equipTypes.contains(EquipType.DGE)) {
            neSubType = NeSubType.OPC_DGE;
        } else if (equipTypes.contains(EquipType.MUXPANEL) || equipTypes.contains(EquipType.MUX)
                || equipTypes.contains(EquipType.MUX32CL) || equipTypes.contains(
                EquipType.CMUX64)) {
            neSubType = NeSubType.OPC_OTM;
        } else if (!(equipTypes.contains(EquipType.MUXPANEL) || equipTypes.contains(EquipType.MUX)
                || equipTypes.contains(EquipType.MUX32CL) || equipTypes.contains(
                EquipType.CMUX64)) && equipTypes.contains(EquipType.IRA)) {
            neSubType = NeSubType.OPC_ROADM;
        }
        if (neSubType != null) {
            log.info("upgrade the ne:{} to neSubType:{}", neId, neSubType);
            phyNodeDao.updatePhyNodeSubNeType(neId, neSubType);
        }
    }
}
