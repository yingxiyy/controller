package net.flex.dci.otn.controller.allocate.node.view.graph;

import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1Builder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class GraphHelp {
    private ChangedObject changedObject;

    public GraphHelp(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    public void layout(Map<String, Node> changedViewNodeList) {
        int[] pos = new int[]{100, 100, 800, 800};

        doIt(changedViewNodeList, null, pos);
    }

    public void layout(Map<String, Node> changedSiteNodeList, Map<String, Link> changedSiteLinkList) {
        Map<String, Node> changedViewNodeList = getViewNodes(changedSiteLinkList.values());
        int[] pos = getPos(changedViewNodeList);

        doIt(changedViewNodeList, covert2LayoutLink(changedSiteLinkList.values()), pos);
    }

    private void doIt(Map<String, Node> changedViewNodeList, List<ImmutablePair<String, String>> linkList, int[] pos) {
        GraphLayout layout = new GraphLayout();

        Map<String, ImmutablePair<Integer, Integer>> internalNodes = layout
                .layout(new ArrayList<>(changedViewNodeList.keySet()), linkList,
                        pos[0], pos[1], pos[2], pos[3]);

        for (String key : internalNodes.keySet()) {
            Node viewNode = changedViewNodeList.get(key);
            changedObject.addChangedViewNode(updateCoord(viewNode, internalNodes.get(key).getLeft(), internalNodes.get(key).getRight()));
        }
    }

    private List<ImmutablePair<String, String>> covert2LayoutLink(Collection<Link> siteLinks) {
        return siteLinks.stream().map(link->{
            String srcTp = link.getSource().getSourceTp().getValue();
            String dstTp = link.getDestination().getDestTp().getValue();
            return new ImmutablePair<>(srcTp, dstTp);
        }).collect(Collectors.toList());
    }

    private Map<String, Node> getViewNodes(Collection<Link> siteLinks) {
        Map<String, Node> changedViewNodeList = new HashMap<>();

        for (Link siteLink : siteLinks) {
            String sNodeId = siteLink.getSource().getSourceNode().getValue();
            String dNodeId = siteLink.getDestination().getDestNode().getValue();

            changedViewNodeList.put(sNodeId, changedObject.getChangedViewNode(sNodeId));
            changedViewNodeList.put(dNodeId, changedObject.getChangedViewNode(dNodeId));
        }

        return changedViewNodeList;
    }

    private int[] getPos(Map<String, Node> changedViewNodeList) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Node node : changedViewNodeList.values()) {
            View view = node.getAugmentation(Node1.class).getView();
            if (view.getPosX() < minX) {
                minX = view.getPosX();
            }
            if (view.getPosX() > maxX) {
                maxX = view.getPosX();
            }
            if (view.getPosY() < minY) {
                minY = view.getPosY();
            }
            if (view.getPosY() > maxY) {
                maxY = view.getPosY();
            }
        }

        return new int[]{minX, minY, maxX, maxY};
    }

    private Node updateCoord(Node viewNode, Integer left, Integer right) {
        View viewNodeAttr = viewNode.getAugmentation(Node1.class).getView();

        return new NodeBuilder(viewNode)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setView(new ViewBuilder(viewNodeAttr)
                                .setPosX(left)
                                .setPosY(right)
                                .build())
                        .build())
                .build();
    }
}
