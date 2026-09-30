package net.flex.dci.otn.ne.upgrade.tool.components;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
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
public class TDNeSubTypeUpgrader implements NeSubTypeUpgrader {

    private final PhyNodeDao phyNodeDao;

    @Override
    public void upgradeNeSubType(List<Node> nodes) {
        log.info("upgrade neSubType for td node:{}", nodes.size());
        List<String> nodeIds = nodes.stream().map(NodeAttributes::getNodeId).map(Uri::getValue)
                .collect(
                        Collectors.toList());
        log.info("upgrade neSubType for td node:{}", nodeIds);
        nodes.forEach(this::upgradeNeSubType);
    }

    @Override
    public NodeType generalType() {
        return NodeType.TD;
    }

    @Override
    public void upgradeNeSubType(Node node) {
        log.info("upgrade ne subType neId:{}", node.getNodeId());
        String neId = node.getNodeId().getValue();
        Physical nodePhysical = node.getAugmentation(
                Node1.class).getPhysical();
        Set<EquipType> equipTypes = nodePhysical.getEquipments().stream()
                .map(PhyEquipAttributes::getEquipType).collect(
                        Collectors.toSet());
        NeSubType neSubType = null;
        if (equipTypes.contains(EquipType.OT)) {
            neSubType = NeSubType.EPC_OTM;
        }
        if (neSubType != null) {
            log.info("update ne subNeType neId:{} neSubType:{}", neId, neSubType);
            phyNodeDao.updatePhyNodeSubNeType(neId, neSubType);
        }
    }
}
