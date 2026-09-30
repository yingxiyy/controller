package net.flex.dci.otn.controller.allocate.common;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.nodes.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.nodes.EquipmentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class YangDataConverter {

    public static List<Nodes> convertToUINodeList(List<Node> nodeList) {

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes> output = new ArrayList<>();
        for (Node node : nodeList) {
            output.add(convertToUINode(node));
        }
        return output;
    }

    public static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes convertToUINode(Node node) {

        List<Equipments> equipments = node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().map(item -> new EquipmentsBuilder(item).build()).collect(Collectors.toList());
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.NodesBuilder().setNodeId(node.getNodeId().getValue()).setEquipments(equipments).build();

    }
}
