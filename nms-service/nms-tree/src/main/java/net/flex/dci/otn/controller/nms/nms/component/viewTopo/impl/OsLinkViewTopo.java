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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyLinkAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
//TODO this class hasn't test, should checking in REG mode
public class OsLinkViewTopo extends AbstractViewTopo {

//    private SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

//    private Map<String, Node> nodeMap = new HashMap<>();

//    public OsLinkViewTopo(
//            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks,
//            ViewLinkType viewLinkType, String plane) {
//        super(siteLinks, viewLinkType, plane);
//    }

    @Override
    public ViewLinkType supportViewLinkType() {
        return ViewLinkType.OsLink;
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
        Map<String, List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link>> viewLinkNodeMap =
                siteLinks.stream()
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
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks = entry.getValue();
        List<String> osLinkIds =
                siteLinks.stream().flatMap(siteLink -> siteLink.getSupportingLink().stream()).map(
                                SupportingLink::getLinkRef)
                        .map(Uri::getValue).filter(PhysicalLinkIdNamingRule::isOsLink).collect(
                                Collectors.toList());
        String otsLinkId = osLinkIds.get(0);
        String sourceSite = PhysicalLinkIdNamingRule.getSiteAId(otsLinkId);
        String destSite = PhysicalLinkIdNamingRule.getSiteZId(otsLinkId);
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkId(new LinkId(viewLinkId));
        linkBuilder.setKey(new LinkKey(new LinkId(viewLinkId)));
        linkBuilder.setSource(new SourceBuilder().setSourceNode(new NodeId(sourceSite)).build());
        linkBuilder.setDestination(
                new DestinationBuilder().setDestNode(new NodeId(destSite)).build());
        linkBuilder.setSupportingLink(osLinkIds.stream()
                .map(linkId -> new SupportingLinkBuilder().setLinkRef(
                        LinkId.getDefaultInstance(linkId)).build()).collect(Collectors.toList()));

        linkBuilder.setView(new ViewBuilder()
                .setAlarmState(getPhyLinkAlarmStatus(otsLinkId))
                .setLevel(supportViewLinkType())
                .setBundleNumber(osLinkIds.size())
                .build());
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> otsLinks = phyLinkDao.getAllPhyLinksByIds(
                osLinkIds);
        List<AlarmSeverity> alarmSeverities = otsLinks.stream()
                .map(link -> link.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class))
                .map(
                        PhyLinkAttributes::getPhysical).map(CommonAttributes::getAlarmState)
                .collect(
                        Collectors.toList());
        AlarmSeverity viewAlarmSeverity = getHigherAlarmSeverity(alarmSeverities);
        linkBuilder.setView(new ViewBuilder()
                .setAlarmState(viewAlarmSeverity)
                .setLevel(supportViewLinkType())
                .setBundleNumber(osLinkIds.size())
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
//        Map<String, Link> linkMap = new HashMap<>();
//
//        siteLinks.stream().forEach(siteLink -> {
//            convert2ViewLink(linkMap, siteLink);
//        });
//
//        return new ArrayList<>(linkMap.values());
//    }
//
//    private void convert2ViewLink(Map<String, Link> linkMap,
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink) {
//        LinkBuilder linkBuilder;
//
//        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
//
//        String siteLinkIdStr = siteLink.getLinkId().getValue();
//        LinkId linkId = new LinkId(siteLinkIdStr);
//        String neA = SiteLinkIdNamingRule.getNodeA(siteLinkIdStr);
//        String neZ = SiteLinkIdNamingRule.getNodeZ(siteLinkIdStr);
//        String siteNodeA = PhysicalNodeIdNamingRule.getSiteId(neA);
//        String siteNodeZ = PhysicalNodeIdNamingRule.getSiteId(neZ);
//        String viewLinkIdStr = String.format("%s---%s-(%s)", siteNodeA, siteNodeZ, viewLinkType);
//
//        if (linkMap.containsKey(viewLinkIdStr)) {
//            Link oldViewLink = linkMap.get(viewLinkIdStr);
//            linkBuilder = new LinkBuilder(oldViewLink);
//            List<SupportingLink> newSList = new ArrayList<>(linkBuilder.getSupportingLink());
//            newSList.add(new SupportingLinkBuilder()
//                    .setLinkRef(linkId)
//                    .setKey(new SupportingLinkKey(linkId))
//                    .build());
//            linkBuilder.setSupportingLink(newSList);
//
//            linkBuilder.setView(new ViewBuilder()
////                  .setEventState(getEventStatus())
//                    .setAlarmState(getHigherAlarmSeverity(oldViewLink.getView().getAlarmState(),
//                            siteLinkAttr.getAlarmState()))
//                    .setBundleNumber(oldViewLink.getView().getBundleNumber() + 1)
//                    .build());
//        } else {
//            linkBuilder = new LinkBuilder();
//            linkBuilder.setLinkId(new LinkId(viewLinkIdStr));
//            linkBuilder.setKey(new LinkKey(new LinkId(viewLinkIdStr)));
//            linkBuilder.setSource(new SourceBuilder().setSourceNode(new NodeId(neA)).build());
//            linkBuilder.setDestination(
//                    new DestinationBuilder().setDestNode(new NodeId(neZ)).build());
//
//            linkBuilder.setView(new ViewBuilder()
//                    .setAlarmState(siteLinkAttr.getAlarmState())
//                    .setLevel(viewLinkType)
//                    .setBundleNumber(1)
//                    .build());
//        }
//
//        linkMap.put(viewLinkIdStr, linkBuilder.build());
//
//        insertNode(nodeMap, neA);
//        insertNode(nodeMap, neZ);
//    }
//
//    @Override
//    protected List<Node> getNode() {
//        Map<String, Node> NodeMap = new HashMap<>();
//
//        siteLinks.stream().forEach(siteLink -> {
//            convert2ViewNode(NodeMap, siteLink);
//        });
//
//        return new ArrayList<>(NodeMap.values());
//    }
//
//    private void convert2ViewNode(Map<String, Node> nodeMap,
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink) {
//        String siteLinkIdStr = siteLink.getLinkId().getValue();
//        String nodeA = SiteLinkIdNamingRule.getNodeA(siteLinkIdStr);
//        String nodeZ = SiteLinkIdNamingRule.getNodeZ(siteLinkIdStr);
//
//        insertNode(nodeMap, nodeA);
//        insertNode(nodeMap, nodeZ);
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
