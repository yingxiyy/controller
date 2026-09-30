package net.flex.dci.otn.controller.allocate.link.site.insertnode;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InsertNodeInSiteLinkContext {
    final String siteLinkId;
    final String phyLinkId;
    final String siteNodeId;
    final String planeId;
    final LinkTerminationNodeType nodeType;
    final Link siteLink;
    final Link oldPhyLink;
    final List<Link> ochLinks;
    final List<DgeOchXcToCreate> dgeOchXcsToCreate = new ArrayList<>();
    final Map<String, List<CrossConnections>> ochXcsByOchLinkId = new HashMap<>();
    Node insertedPhyNode;
    Link aToInsertedOtsLink;
    Link insertedToZOtsLink;

    InsertNodeInSiteLinkContext(String siteLinkId, String phyLinkId, String siteNodeId,
            String planeId, LinkTerminationNodeType nodeType, Link siteLink, Link oldPhyLink, List<Link> ochLinks) {
        this.siteLinkId = siteLinkId;
        this.phyLinkId = phyLinkId;
        this.siteNodeId = siteNodeId;
        this.planeId = planeId;
        this.nodeType = nodeType;
        this.siteLink = siteLink;
        this.oldPhyLink = oldPhyLink;
        this.ochLinks = ochLinks;
    }

    void setInsertedPhyNode(Node insertedPhyNode) {
        this.insertedPhyNode = insertedPhyNode;
    }

    List<CrossConnections> createDgeOchXcs(TerminationPoint sourceTp, TerminationPoint destinationTp,
            Equipments equipment) {
        // createInsertedNode() 拿到新 DGE 的东西向 TP 后，一次性生成节点 XC 列表，并同步建立 OCH->XC 映射供 updateOchRoutes() 使用。
        List<CrossConnections> crossConnections = new ArrayList<>();
        ochXcsByOchLinkId.clear();
        for (DgeOchXcToCreate xcToCreate : dgeOchXcsToCreate) {
            CrossConnections crossConnection = xcToCreate.buildCrossConnection(sourceTp, destinationTp, equipment);
            crossConnections.add(crossConnection);
            ochXcsByOchLinkId.put(xcToCreate.ochLinkId, Collections.singletonList(crossConnection));
        }
        return crossConnections;
    }

    Node appendInsertedNodeCrossConnections(List<CrossConnections> crossConnections) {
        Physical physical = insertedPhyNode.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> newCrossConnections = new ArrayList<>();
        if (physical.getCrossConnections() != null) {
            newCrossConnections.addAll(physical.getCrossConnections());
        }
        newCrossConnections.addAll(crossConnections);

        insertedPhyNode = new NodeBuilder(insertedPhyNode)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(physical)
                                .setCrossConnections(newCrossConnections)
                                .build())
                        .build())
                .build();
        return insertedPhyNode;
    }
}
