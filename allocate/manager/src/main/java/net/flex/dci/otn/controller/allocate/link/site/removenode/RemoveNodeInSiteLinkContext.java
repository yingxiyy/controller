package net.flex.dci.otn.controller.allocate.link.site.removenode;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.util.List;

class RemoveNodeInSiteLinkContext {
    final String siteLinkId;
    final String nodeId;
    final String siteNodeId;
    final String planeId;
    final Link siteLink;
    final Node removedPhyNode;
    final List<Link> ochLinks;
    Link leftSplitOtsLink;
    Link rightSplitOtsLink;
    Link mergedOtsLink;

    RemoveNodeInSiteLinkContext(String siteLinkId, String nodeId, String siteNodeId, String planeId,
            Link siteLink, Node removedPhyNode, List<Link> ochLinks) {
        this.siteLinkId = siteLinkId;
        this.nodeId = nodeId;
        this.siteNodeId = siteNodeId;
        this.planeId = planeId;
        this.siteLink = siteLink;
        this.removedPhyNode = removedPhyNode;
        this.ochLinks = ochLinks;
    }
}
