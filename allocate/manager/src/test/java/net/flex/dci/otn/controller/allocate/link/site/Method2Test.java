package net.flex.dci.otn.controller.allocate.link.site;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;

class Method2Test {

    @Test
    void usesMuxPanelMpoAtBothBone20Flex64Endpoints() {
        Node aNode = endpointNode("Site-A#Ne-A");
        Node zNode = endpointNode("Site-Z#Ne-Z");
        RouteInfo routeInfo = RouteInfo.builder()
                .main(Route.builder()
                        .nodes(Arrays.asList(aNode, zNode))
                        .links(Collections.emptyList())
                        .xcs(Collections.emptyList())
                        .build())
                .build();

        Method2 method = new Method2(true);

        assertEquals("Site-A#Ne-A#MUX-1-50#PORT-1-50-MPO1",
                method.getLinkSrcTermination(routeInfo).getSourceTp().getValue());
        assertEquals("Site-Z#Ne-Z#MUX-1-50#PORT-1-50-MPO1",
                method.getLinkDstTermination(routeInfo).getDestTp().getValue());
    }

    @Test
    void keepsLegacySecondLastLinkDestinationWhenMuxPanelModeIsDisabled() {
        Node aNode = endpointNode("Site-A#Ne-A");
        Node zNode = endpointNode("Site-Z#Ne-Z");
        Link expectedEndpointLink = link("endpoint",
                "Site-Z#Ne-Z#MUX-1-50#PORT-1-50-MPO1",
                "Site-Z#Ne-Z#LINECARD-1-3#PORT-1-3-MPO1");
        RouteInfo routeInfo = RouteInfo.builder()
                .main(Route.builder()
                        .nodes(Arrays.asList(aNode, zNode))
                        .links(Arrays.asList(
                                link("first", "Site-A#Ne-A#PANEL-1-40#PORT-1-40-MUX",
                                        "Site-A#Ne-A#MUX-1-50#PORT-1-50-MUX"),
                                expectedEndpointLink,
                                link("last", "Site-Z#Ne-Z#MUX-1-50#PORT-1-50-MUX",
                                        "Site-Z#Ne-Z#PANEL-1-40#PORT-1-40-MUX")))
                        .xcs(Collections.emptyList())
                        .build())
                .build();

        assertEquals(expectedEndpointLink.getDestination(),
                new Method2().getLinkDstTermination(routeInfo));
    }

    private Node endpointNode(String nodeId) {
        String muxEquipmentId = nodeId + "#MUX-1-50";
        Node1 physical = new Node1Builder()
                .setPhysical(new PhysicalBuilder()
                        .setEquipments(Arrays.asList(
                                new EquipmentsBuilder()
                                        .setEquipmentId(nodeId + "#LINECARD-1-3")
                                        .setEquipType(EquipType.FMUX32)
                                        .build(),
                                new EquipmentsBuilder()
                                        .setEquipmentId(muxEquipmentId)
                                        .setEquipType(EquipType.MUXPANEL)
                                        .build()))
                        .build())
                .build();
        return new NodeBuilder()
                .setNodeId(new NodeId(nodeId))
                .setTerminationPoint(Arrays.asList(
                        new TerminationPointBuilder()
                                .setTpId(new TpId(nodeId
                                        + "#LINECARD-1-3#PORT-1-3-MPO1"))
                                .build(),
                        new TerminationPointBuilder()
                                .setTpId(new TpId(muxEquipmentId + "#PORT-1-50-MPO2"))
                                .build(),
                        new TerminationPointBuilder()
                                .setTpId(new TpId(muxEquipmentId + "#PORT-1-50-MPO1"))
                                .build()))
                .addAugmentation(Node1.class, physical)
                .build();
    }

    private Link link(String id, String sourceTp, String destinationTp) {
        LinkId linkId = new LinkId(id);
        return new LinkBuilder()
                .setLinkId(linkId)
                .setSource(new SourceBuilder()
                        .setSourceNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(sourceTp)))
                        .setSourceTp(new TpId(sourceTp))
                        .build())
                .setDestination(new DestinationBuilder()
                        .setDestNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(destinationTp)))
                        .setDestTp(new TpId(destinationTp))
                        .build())
                .build();
    }
}
