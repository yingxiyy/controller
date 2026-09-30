package net.flex.dci.otn.controller.nms.nms.component.viewTopo.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.startwith.output.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class OchLinkViewTopo extends AbstractViewTopo {

//    private SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);

//    private GetViewLinkByPlaneStartwithOutputBuilder outputBuilder = new GetViewLinkByPlaneStartwithOutputBuilder();

//    private Map<String, Link> linkMap = new HashMap<>();
//    private Map<String, Node> nodeMap = new HashMap<>();

//    public OchLinkViewTopo(
//            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks,
//            ViewLinkType viewLinkType, String plane) {
//        super(siteLinks, viewLinkType, plane);
//    }

    @Override
    public ViewLinkType supportViewLinkType() {
        return ViewLinkType.OchLink;
    }

    @Override
    public GetViewLinkByPlaneStartwithOutput getTopo(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> siteLinks,
            String plane) {
        log.debug("get ochLink by plane:{} ", plane);
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
        return null;
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
//        GetViewLinkByPlaneStartwithOutputBuilder outputBuilder = new GetViewLinkByPlaneStartwithOutputBuilder();
//        outputBuilder.setPlane(shortestPlaneName);
//        outputBuilder.setLink(getLink());
//        outputBuilder.setNode(getNode());
//
//        return outputBuilder.build();
//    }

//    @Override
//    protected List<Link> getLink() {
//        siteLinks.stream().forEach(siteLink -> {
//            convert2ViewLink(linkMap, siteLink);
//        });
//
//        return new ArrayList<>(linkMap.values());
//    }
//
//    //以siteLink 为入口， 找到上面的所有OCH link,
//    //ochLink 有两种情况， 1. 长度和复用段一样， 2， 这是ROADM/REG 相关的ochLink, 长度超过siteLink
//    //情况1 直接计算长度一样的个数，告警级别
//    //情况2 这些ochLink 单独构成viewLink， 及相关的viewNode
//    private void convert2ViewLink(Map<String, Link> linkMap,
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link siteLink) {
//        LinkBuilder linkBuilder;
//
//        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
//
//        String siteLinkIdStr = siteLink.getLinkId().getValue();
//        String neA = SiteLinkIdNamingRule.getNodeA(siteLinkIdStr);
//        String neZ = SiteLinkIdNamingRule.getNodeZ(siteLinkIdStr);
//        String siteNodeA = PhysicalNodeIdNamingRule.getSiteId(neA);
//        String siteNodeZ = PhysicalNodeIdNamingRule.getSiteId(neZ);
//        String viewLinkIdStr = String.format("%s---%s-(%s)", siteNodeA, siteNodeZ, viewLinkType);
//
//        if (linkMap.containsKey(viewLinkIdStr)) {
//            Link oldViewLink = linkMap.get(viewLinkIdStr);
//            linkBuilder = new LinkBuilder(oldViewLink);
//
//            AlarmPair data = getBundle(siteLinkAttr.getSupportedLink(), neA, neZ);
//            linkBuilder.setView(new ViewBuilder()
////                  .setEventState(getEventStatus())
//                    .setAlarmState(data.getSeverity())
//                    .setBundleNumber(data.getNum())
//                    .build());
//        } else {
//            linkBuilder = new LinkBuilder();
//            linkBuilder.setLinkId(new LinkId(viewLinkIdStr));
//            linkBuilder.setKey(new LinkKey(new LinkId(viewLinkIdStr)));
//            linkBuilder.setSource(new SourceBuilder().setSourceNode(new NodeId(siteNodeA)).build());
//            linkBuilder.setDestination(
//                    new DestinationBuilder().setDestNode(new NodeId(siteNodeZ)).build());
//            linkBuilder.setSupportingLink(new ArrayList<>());
//
//            AlarmPair data = getBundle(siteLinkAttr.getSupportedLink(), neA, neZ);
//            if (data.getNum() == 0) {
//                return;
//            }
//            linkBuilder.setView(new ViewBuilder()
//                    .setLevel(viewLinkType)
//                    .setAlarmState(data.getSeverity())
//                    .setBundleNumber(data.getNum())
//                    .build());
//        }
//
//        //ochLink 不放列表， 只要原因是ochLink的名称，和如何nav 到对应列表的问题
////        List<SupportingLink> newSList = new ArrayList<>(linkBuilder.getSupportingLink());
////        newSList.add(new SupportingLinkBuilder()
////                .setLinkRef(linkId)
////                .setKey(new SupportingLinkKey(linkId))
////                .build());
////        linkBuilder.setSupportingLink(newSList);
//
//        linkMap.put(viewLinkIdStr, linkBuilder.build());
//
//        insertNode(nodeMap, neA);
//        insertNode(nodeMap, neZ);
//    }
//
//    /**
//     * och 不是简单的A---Z, 如果出现ROADM/REG的时候， och 就跨越了介个复用段
//     *
//     * @param supportedLink
//     * @param nodeA
//     * @param nodeZ
//     * @return
//     */
//    private AlarmPair getBundle(List<SupportedLink> supportedLink, String nodeA, String nodeZ) {
//        AtomicInteger number = new AtomicInteger();
//        List<String> p2pOchs = new ArrayList<>();
//
//        supportedLink.stream().forEach(x -> {
//            String ochLinkId = x.getLinkRef().getValue();
//            String ochNodeA = OchLinkIdNamingRule.nodeA(ochLinkId);
//            String ochNodeZ = OchLinkIdNamingRule.nodeZ(ochLinkId);
//
//            if ((ochNodeA.equals(nodeA) && ochNodeZ.equals(nodeZ)) ||
//                    (ochNodeA.equals(nodeZ) && ochNodeZ.equals(nodeA))) {
//                number.getAndIncrement();
//                p2pOchs.add(ochLinkId);
//            } else {
//                convertOch2ViewLink(linkMap, ochLinkId);
//            }
//        });
//
//        return new AlarmPair(number.get(), getBundleOchLinkAlarmStatus(p2pOchs));
//    }
//
//    private void convertOch2ViewLink(Map<String, Link> linkMap, String ochLinkIdStr) {
//        LinkBuilder linkBuilder;
//
//        LinkId linkId = new LinkId(ochLinkIdStr);
//        String neA = OchLinkIdNamingRule.nodeA(ochLinkIdStr);
//        String neZ = OchLinkIdNamingRule.nodeZ(ochLinkIdStr);
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
//                            getOchLinkAlarmStatus(ochLinkIdStr)))
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
//                    .setAlarmState(getOchLinkAlarmStatus(ochLinkIdStr))
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
//
//    @Data
//    @AllArgsConstructor
//    private static class AlarmPair {
//
//        private Integer num;
//
//        private AlarmSeverity severity;
//    }
}
