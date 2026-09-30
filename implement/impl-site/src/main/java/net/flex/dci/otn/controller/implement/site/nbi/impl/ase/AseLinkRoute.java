package net.flex.dci.otn.controller.implement.site.nbi.impl.ase;

import lombok.Getter;
import net.flex.dci.otc.common.util.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class AseLinkRoute extends RouteInfo {

    private List<String> tpIdList = new ArrayList();
    private List<String> nodeIdList = new ArrayList();
    private List<String> phyLinkIdList = new ArrayList();
    private List<String> xcIdList = new ArrayList();
    private List<String> eqIdList = new ArrayList();
    private List<String> logicServerLinkIdList = new ArrayList();

    public AseLinkRoute(Collection<Node> opticalNodes) {
        List<Node> aseNodeList = findAseTP(opticalNodes);
        aseNodeList = findAseXC(aseNodeList);
        nodeIdList = aseNodeList.stream().map(x->x.getNodeId().getValue()).collect(Collectors.toList());
    }

    private List<Node> findAseTP(Collection<Node> opticalNodes) {
        List<Node> updatedNode = new ArrayList<>();
        for(Node node : opticalNodes) {
            List<TerminationPoint> tpList = node.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().contains("EXP33")).collect(Collectors.toList());
            Node newNode = new NodeBuilder(node).setTerminationPoint(tpList).build();
            updatedNode.add(newNode);

            tpIdList.addAll(tpList.stream().map(x->x.getTpId().getValue()).collect(Collectors.toList()));
        }
        return updatedNode;
    }

    private List<Node> findAseXC(Collection<Node> opticalNodes) {
        List<Node> updatedNode = new ArrayList<>();
        for(Node node : opticalNodes) {
            List<CrossConnections> xcList = node.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream().filter(xc -> xc.getDescription().startsWith("ASE")).collect(Collectors.toList());
            Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                    .setPhysical(new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                            .setCrossConnections(xcList)
                            .build())
                    .build()).build();
            updatedNode.add(newNode);
            xcIdList.addAll(xcList.stream().map(x->x.getCrossConnectionId().getValue()).collect(Collectors.toList()));
        }

        return updatedNode;
    }
}
