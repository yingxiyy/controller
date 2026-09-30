package net.flex.dci.otn.controller.nms.nms.component.viewTopo.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.LinkKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.CommonAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyLinkAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class OtsLinkViewTopo extends AbstractViewTopo {

//    private SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

//    private Map<String, Link> linkMap = new HashMap<>();
//    private Map<String, Node> nodeMap = new HashMap<>();

//    public OtsLinkViewTopo(
//            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks,
//            ViewLinkType viewLinkType, String plane) {
//        super(siteLinks, viewLinkType, plane);
//    }

    @Override
    public ViewLinkType supportViewLinkType() {
        return ViewLinkType.OtsLink;
    }

    @Override
    public GetViewLinkByPlaneStartwithOutput getTopo(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks,
            String plane) {
        log.debug("get ots link view topo by plane name:{}", plane);
        List<String> planeNames = siteLinkDao.listAllPlaneStartwith(plane);
        if (CollectionUtils.isEmpty(planeNames)) {
            return defaultReturn();
        }
        String shortestPlaneName = planeNames.stream()
                .min(Comparator.comparingInt(String::length))
                .orElse(null);
        ViewTopoDto viewTopoDto = getViewTopoDto(siteLinks);
        GetViewLinkByPlaneStartwithOutputBuilder outputBuilder = new GetViewLinkByPlaneStartwithOutputBuilder();
        outputBuilder.setPlane(shortestPlaneName);
        outputBuilder.setLink(viewTopoDto.getLinks());
        outputBuilder.setNode(viewTopoDto.getNodes());

        return outputBuilder.build();
    }

    @Override
    protected ViewTopoDto getViewTopoDto(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks) {
        List<String> otsLinkIds = siteLinks.stream()
                .flatMap(link -> link.getSupportingLink().stream())
                .map(SupportingLink::getLinkRef).map(Uri::getValue)
                .filter(PhysicalLinkIdNamingRule::isOtsLink).collect(Collectors.toList());
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> phyLinks = phyLinkDao.getAllPhyLinksByIds(
                otsLinkIds);
        Map<String, List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link>> viewLinkNodeMap =
                phyLinks.stream()
                        .collect(Collectors.groupingBy(link -> {
                            String neA = link.getSource().getSourceNode().getValue();
                            String neZ = link.getDestination().getDestNode().getValue();
                            String siteNodeA = PhysicalNodeIdNamingRule.getSiteId(neA);
                            String siteNodeZ = PhysicalNodeIdNamingRule.getSiteId(neZ);
                            String[] sites = {siteNodeA, siteNodeZ};
                            Arrays.sort(sites);
                            return String.format("%s---%s-(%s)",
                                    sites[0], sites[1], supportViewLinkType());

                        }));
        List<ViewLinkInfo> viewLinkInfos = viewLinkNodeMap.entrySet().stream()
                .map(this::getViewLinkInfo)
                .collect(Collectors.toList());
        List<Link> viewLinks = viewLinkInfos.stream().map(ViewLinkInfo::getLink)
                .collect(Collectors.toList());
        List<Node> viewNodes = viewLinkInfos.stream()
                .flatMap(viewLinkInfo -> viewLinkInfo.getNodes().stream()).collect(
                        Collectors.toList());

        return ViewTopoDto.builder().links(viewLinks).nodes(viewNodes).build();
    }

    private ViewLinkInfo getViewLinkInfo(
            Entry<String, List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link>> entry) {
        String viewLinkId = entry.getKey();
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> phyLinks = entry.getValue();
        List<LinkId> linkIds = phyLinks.stream().map(LinkAttributes::getLinkId)
                .collect(Collectors.toList());
        String otsLinkId = phyLinks.get(0).getLinkId().getValue();
        String sourceSite = PhysicalLinkIdNamingRule.getSiteAId(otsLinkId);
        String destSite = PhysicalLinkIdNamingRule.getSiteZId(otsLinkId);
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkId(new LinkId(viewLinkId));
        linkBuilder.setKey(new LinkKey(new LinkId(viewLinkId)));
        linkBuilder.setSource(new SourceBuilder().setSourceNode(new NodeId(sourceSite)).build());
        linkBuilder.setDestination(
                new DestinationBuilder().setDestNode(new NodeId(destSite)).build());
        linkBuilder.setSupportingLink(linkIds.stream()
                .map(linkId -> new SupportingLinkBuilder().setLinkRef(
                        linkId).build()).collect(Collectors.toList()));

        linkBuilder.setView(new ViewBuilder()
                .setAlarmState(getPhyLinkAlarmStatus(otsLinkId))
                .setLevel(supportViewLinkType())
                .setBundleNumber(phyLinks.size())
                .build());

        List<AlarmSeverity> alarmSeverities = phyLinks.stream()
                .map(link -> link.getAugmentation(Link1.class)).map(
                        PhyLinkAttributes::getPhysical).map(CommonAttributes::getAlarmState)
                .collect(
                        Collectors.toList());
        AlarmSeverity viewAlarmSeverity = getHigherAlarmSeverity(alarmSeverities);
        linkBuilder.setView(new ViewBuilder()
                .setAlarmState(viewAlarmSeverity)
                .setLevel(supportViewLinkType())
                .setBundleNumber(phyLinks.size())
                .build());
        Node source = buildViewNode(sourceSite);
        Node dest = buildViewNode(destSite);
        return ViewLinkInfo.builder().link(linkBuilder.build()).nodes(Arrays.asList(source, dest))
                .build();
    }

//    @Override
//    public GetViewLinkByPlaneStartwithOutput getTopo() {
//        List<String> planeNames = siteLinkDao.listAllPlaneStartwith(plane);
//        if (planeNames == null || planeNames.isEmpty()) {
//            return super.getTopo();
//        }
//
//        String shortestPlaneName = planeNames.stream()
//                .min(Comparator.comparingInt(String::length))
//                .orElse(null);
//
//        GetViewLinkByPlaneStartwithOutputBuilder outputBuilder = new GetViewLinkByPlaneStartwithOutputBuilder();
//        outputBuilder.setPlane(shortestPlaneName);
//        outputBuilder.setLink(getLink());
//        outputBuilder.setNode(getNode());
//
//        return outputBuilder.build();
//    }
//
//    @Override
//    protected List<Link> getLink() {
//        siteLinks.stream().forEach(siteLink -> {
//            convert2ViewLink(linkMap, siteLink);
//        });
//
//        return new ArrayList<>(linkMap.values());
//    }
//
//    //以siteLink 为入口， 找到上面的所有OTS link
//    private void convert2ViewLink(Map<String, Link> linkMap,
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink) {
//        LinkBuilder linkBuilder;
//
//        List<String> otsLinkIds = siteLink.getSupportingLink().stream()
//                .map(x -> x.getLinkRef().getValue())
//                .filter(x -> PhysicalLinkIdNamingRule.isOtsLink(x))
//                .collect(Collectors.toList());
//
//        otsLinkIds.forEach(otsLinkId -> convertOts2ViewLink(linkMap, otsLinkId));
//    }
//
//    private void convertOts2ViewLink(Map<String, Link> linkMap, String otsLinkId) {
//        LinkBuilder linkBuilder;
//
//        LinkId linkId = new LinkId(otsLinkId);
//        String neA = OchLinkIdNamingRule.nodeA(otsLinkId);
//        String neZ = OchLinkIdNamingRule.nodeZ(otsLinkId);
//        String siteNodeA = PhysicalNodeIdNamingRule.getSiteId(neA);
//        String siteNodeZ = PhysicalNodeIdNamingRule.getSiteId(neZ);
//        String viewLinkIdStr = String.format("%s---%s-(%s)", siteNodeA, siteNodeZ, viewLinkType);
//
//        if (linkMap.containsKey(viewLinkIdStr)) {
//            Link oldViewLink = linkMap.get(viewLinkIdStr);
//            linkBuilder = new LinkBuilder(oldViewLink);
//
//            linkBuilder.setView(new ViewBuilder()
////                  .setEventState(getEventStatus())
//                    .setAlarmState(getHigherAlarmSeverity(oldViewLink.getView().getAlarmState(),
//                            getPhyLinkAlarmStatus(otsLinkId)))
//                    .setBundleNumber(oldViewLink.getView().getBundleNumber() + 1)
//                    .build());
//        } else {
//            linkBuilder = new LinkBuilder();
//            linkBuilder.setLinkId(new LinkId(viewLinkIdStr));
//            linkBuilder.setKey(new LinkKey(new LinkId(viewLinkIdStr)));
//            linkBuilder.setSource(new SourceBuilder().setSourceNode(new NodeId(neA)).build());
//            linkBuilder.setDestination(
//                    new DestinationBuilder().setDestNode(new NodeId(neZ)).build());
//            linkBuilder.setSupportingLink(new ArrayList<>());
//
//            linkBuilder.setView(new ViewBuilder()
//                    .setAlarmState(getPhyLinkAlarmStatus(otsLinkId))
//                    .setLevel(viewLinkType)
//                    .setBundleNumber(1)
//                    .build());
//        }
//
//        List<SupportingLink> newSList = new ArrayList<>(linkBuilder.getSupportingLink());
//        newSList.add(new SupportingLinkBuilder()
//                .setLinkRef(linkId)
//                .setKey(new SupportingLinkKey(linkId))
//                .build());
//        linkBuilder.setSupportingLink(newSList);
//
//        linkMap.put(viewLinkIdStr, linkBuilder.build());
//
//        insertNode(nodeMap, neA);
//        insertNode(nodeMap, neZ);
//    }
//
//    @Override
//    protected List<Node> getNode() {
//        return new ArrayList<>(nodeMap.values());
//    }
//
//    private void insertNode(Map<String, Node> nodeMap, String newNeId) {
//        NodeBuilder nodeBuilder;
//        String viewNodeId = PhysicalNodeIdNamingRule.getNodeId(newNeId);
//
//        if (nodeMap.containsKey(viewNodeId)) {
//            Node oldNode = nodeMap.get(viewNodeId);
//            nodeBuilder = new NodeBuilder(oldNode);
//            nodeBuilder.setView(
//                    new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder(
//                            oldNode.getView())
//                            .setAlarmState(getHigherAlarmSeverity(oldNode.getView().getAlarmState(),
//                                    getPhyNodeAlarmStatus(newNeId)))
//                            .build());
//        } else {
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node viewNode = viewNodes.get(
//                    viewNodeId);
//
//            nodeBuilder = new NodeBuilder();
//            nodeBuilder.setNodeId(new NodeId(viewNodeId));
//            nodeBuilder.setKey(new NodeKey(new NodeId(viewNodeId)));
//            nodeBuilder.setSupportingNode(new ArrayList<>());
//            nodeBuilder.setView(
//                    new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder(
//                            viewNode.getAugmentation(Node1.class).getView())
//                            .setAlarmState(getPhyNodeAlarmStatus(newNeId))
//                            .build());
//        }
//
//        List<SupportingNode> newNodeList = new ArrayList(nodeBuilder.getSupportingNode());
//        newNodeList.add(new SupportingNodeBuilder()
//                .setNodeRef(new NodeId(viewNodeId))
//                .setTopologyRef(new TopologyId(SITE_TOPOID))
//                .setKey(new SupportingNodeKey(new NodeId(viewNodeId), new TopologyId(SITE_TOPOID)))
//                .build());
//
//        nodeBuilder.setSupportingNode(newNodeList);
//        nodeMap.put(viewNodeId, nodeBuilder.build());
//    }
}
