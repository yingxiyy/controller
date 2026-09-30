package net.flex.dci.otn.controller.nms.nms.component.viewTopo.impl;

import static net.flex.dci.otc.common.util.Constant.SITE_TOPOID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.NodeKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractViewTopo {

    //    protected List<Link> siteLinks;
//    protected ViewLinkType viewLinkType;
//    protected String plane;
//    protected Map<String, Node> viewNodes;
    @Autowired
    protected SiteLinkDao siteLinkDao;

    @Autowired
    protected SiteNodeDao siteNodeDao;

    @Autowired
    protected ViewNodeDao viewNodeDao;

    @Autowired
    protected PhyLinkDao phyLinkDao;

//    public AbstractViewTopo() {
////        this.siteLinks = siteLinks;
////        this.viewLinkType = viewLinkType;
////        this.plane = plane;
////
////        viewNodes = getAllViewNodes();
//    }

    public abstract ViewLinkType supportViewLinkType();

    public abstract GetViewLinkByPlaneStartwithOutput getTopo(List<Link> siteLinks, String plane);

    protected abstract ViewTopoDto getViewTopoDto(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks);

    protected GetViewLinkByPlaneStartwithOutput defaultReturn() {
        return new GetViewLinkByPlaneStartwithOutputBuilder().build();
    }


    protected org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Node buildViewNode(
            String viewNodeId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node viewNode = viewNodeDao.getViewNodeById(
                viewNodeId);

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(viewNodeId));
        nodeBuilder.setKey(new NodeKey(new NodeId(viewNodeId)));
        nodeBuilder.setView(
                new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder(
                        viewNode.getAugmentation(Node1.class).getView())
                        .build());
        List<SupportingNode> newNodeList = new ArrayList<>();
        newNodeList.add(new SupportingNodeBuilder()
                .setNodeRef(new NodeId(viewNodeId))
                .setTopologyRef(new TopologyId(SITE_TOPOID))
                .setKey(new SupportingNodeKey(new NodeId(viewNodeId), new TopologyId(SITE_TOPOID)))
                .build());

        nodeBuilder.setSupportingNode(newNodeList);
        return nodeBuilder.build();
    }

    protected List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Link> getLink() {
        return new ArrayList<>();
    }

    protected List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Node> getNode() {
        return new ArrayList<>();
    }

    private Map<String, Node> getAllViewNodes() {
//        return viewNodeDao.listViewNodes()
//                .stream()
//                .collect(Collectors.toMap(
//                        node -> node.getNodeId().getValue(),
//                        node -> node
//                ));
        return null;
    }

    protected AlarmSeverity getPhyNodeAlarmStatus(String nodeId) {
//        String alarmStatus = phyNodeDao.getAlarmStatus(nodeId);
//        return fromString(alarmStatus);
        return null;
    }

    protected AlarmSeverity getOchLinkAlarmStatus(String ochLinkId) {
//        String alarmStatus = ochLinkDao.getAlarmStatus(ochLinkId);
//        return fromString(alarmStatus);
        return null;
    }

    protected AlarmSeverity getPhyLinkAlarmStatus(String otsLinkId) {
//        String alarmStatus = phyLinkDao.getAlarmStatus(otsLinkId);
//        return fromString(alarmStatus);
        return null;
    }

    protected AlarmSeverity getBundleOchLinkAlarmStatus(List<String> ochLinkIds) {
//        List<String> alarmStatus = ochLinkDao.getAlarmStatus(ochLinkIds);
        AlarmSeverity highest = AlarmSeverity.Unknown;

//        for (String status : alarmStatus) {
//            AlarmSeverity severity = fromString(status);
//            highest = getHigherAlarmSeverity(highest, severity);
//        }

        return highest;
    }

    protected AlarmSeverity fromString(String value) {
        return Arrays.stream(AlarmSeverity.values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown AlarmSeverity: " + value));
    }


    protected AlarmSeverity getHigherAlarmSeverity(AlarmSeverity oldStatus,
            AlarmSeverity newStatus) {
        if (newStatus.getIntValue() > oldStatus.getIntValue()) {
            return newStatus;
        }
        return oldStatus;
    }

    protected AlarmSeverity getHigherAlarmSeverity(List<AlarmSeverity> alarmSeverities) {
        if (alarmSeverities == null || alarmSeverities.isEmpty()) {
            return AlarmSeverity.Unknown;
        }

        AlarmSeverity result = alarmSeverities.get(0);
        for (int i = 1; i < alarmSeverities.size(); i++) {
            result = getHigherAlarmSeverity(result, alarmSeverities.get(i));
        }
        return result;
    }

    @Data
    @Builder
    protected static class ViewTopoDto {

        private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Node> nodes;
        private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Link> links;
    }

    @Data
    @Builder
    protected static class ViewLinkInfo {

        private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Node> nodes;

        private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Link link;
    }

}
