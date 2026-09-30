package net.flex.dci.otn.controller.nms.nms.component.viewTopo;

import static net.flex.dci.otc.common.util.Constant.GLOBAL_ROOT_NODE_ID;
import static net.flex.dci.otn.controller.nms.utils.CommonUtils.calculateAlarmSeverity;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.nms.nms.enums.NMSViewLinkType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.Topology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1Builder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 1/21/2024 4:42 PM
 */

@Component
@Slf4j
public class ViewTopology {

    private final LoadingCache<String, SubnetIndexHolder> SUBNET_INDEX_HOLDER_CACHE;


    private SubnetIndexHolder getSubnetHolder() {
        try {
            return SUBNET_INDEX_HOLDER_CACHE.get("all");
        } catch (Exception e) {
            log.error("Load subnet index failed, return empty", e);
            return SubnetIndexHolder.builder()
                    .id2NodeMap(new HashMap<>())
                    .parent2ChildIds(new HashMap<>())
                    .build();
        }
    }

    private Map<String, SubNetTreeNode> getSubnetIndex() {

        return getSubnetHolder().getId2NodeMap();

    }

    private final Map<NMSViewLinkType, AbstractViewLink> viewLinkHandlerMap;

    private final DefaultViewLink defaultViewLink;

    private final NetconfTopology topology;

    public ViewTopology(List<AbstractViewLink> viewLinks, DefaultViewLink defaultViewLink,
            NetconfTopology topology) {
        viewLinkHandlerMap = viewLinks.stream().collect(HashMap::new,
                (map, viewLink) -> map.put(viewLink.supportViewLinkType(), viewLink),
                HashMap::putAll);
        this.defaultViewLink = defaultViewLink;
        this.topology = topology;
        this.SUBNET_INDEX_HOLDER_CACHE = Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .refreshAfterWrite(25, TimeUnit.MINUTES)
                .build(key -> {
                    log.info("Loading full subnet index and adjacency map from topology");
                    List<SubNetTreeNode> allSubnets = topology.retrieveAllSubnets();
                    if (CollectionUtils.isEmpty(allSubnets)) {
                        return SubnetIndexHolder.builder()
                                .id2NodeMap(new HashMap<>())
                                .parent2ChildIds(new HashMap<>())
                                .build();
                    }
                    Map<String, SubNetTreeNode> id2NodeMap = allSubnets.stream()
                            .collect(Collectors.toMap(SubNetTreeNode::getSubNetId,
                                    Function.identity(),
                                    (v1, v2) -> v1));
                    Map<String, List<String>> parent2ChildIds = allSubnets.stream()
                            .filter(node -> StringUtils.hasText(node.getParentId()))
                            .collect(Collectors.groupingBy(SubNetTreeNode::getParentId,
                                    Collectors.mapping(SubNetTreeNode::getSubNetId,
                                            Collectors.toList())));

                    return SubnetIndexHolder.builder()
                            .id2NodeMap(id2NodeMap)
                            .parent2ChildIds(parent2ChildIds)
                            .build();
                });
    }

    public List<Topology> getLinkTopology(ViewLinkType viewLinkType) {
        NMSViewLinkType nmsViewLinkType = NMSViewLinkType.getNMSViewLinkType(viewLinkType);
        List<Topology> topologies = viewLinkHandlerMap.getOrDefault(nmsViewLinkType,
                        defaultViewLink)
                .getRefViewLinks();
        return topologies;
    }

    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> getLinkTopologyByPlaneAndLinkType(
            ViewLinkType viewLinkType,
            String plane) {
        List<Link> siteLinks = topology.listAllSiteLinkByPlane(plane);
        List<Link> ochLinks = topology.listAllOchLinkByPlane(plane);
        NMSViewLinkType nmsViewLinkType = NMSViewLinkType.getNMSViewLinkType(viewLinkType);
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> topologies = viewLinkHandlerMap.getOrDefault(
                        nmsViewLinkType,
                        defaultViewLink)
                .getRefViewLinkInfoByMultiplexLink(plane, siteLinks, ochLinks);
        return topologies;
    }


    /**
     * list all view plane for the view topology
     *
     * @return
     */
    public List<String> listAllViewPLane() {
        log.info("list all the view plane ");
        //todo: get site ref all the plane
        List<String> siteLinkRefPlane = topology.listAllSiteLinkRefPlane();
        List<String> ochLinkRefPlane = topology.listAllOchLinkRefPlane();
        List<String> planeNames = Stream.concat(siteLinkRefPlane.stream(), ochLinkRefPlane.stream())
                .distinct().collect(
                        Collectors.toList());
        return planeNames;
    }

    public List<Node> listViewNodes(String subnetId) {
        log.info("list all view nodes by subnetId:{}", subnetId);
        List<Node> viewNodes = new ArrayList<>();
        boolean isRootFilter = isRootSubnetId(subnetId);
        List<String> subnetHierarchy;
        try {
            if (isRootFilter) {
                subnetHierarchy = new ArrayList<>();
            } else {
                subnetHierarchy = getSubnetHierarchy(subnetId);
                if (CollectionUtils.isEmpty(subnetHierarchy)) {
                    log.warn("subnet hierarchy is empty for {}, fallback to all view nodes",
                            subnetId);
                }
            }
        } catch (Exception e) {
            log.error("get subnet hierarchy error", e);
            subnetHierarchy = new ArrayList<>();
        }
//        List<String> subnetHierarchy =
//                isRootFilter ? new ArrayList<>() : SUBNET_HIERARCHY_CACHE.getIfPresent(
//                        subnetId)/*getSubnetHierarchy(subnetId)*/;
//        if (subnetHierarchy == null) {
//            subnetHierarchy = getSubnetHierarchy(subnetId);
//            SUBNET_HIERARCHY_CACHE.put(subnetId, subnetHierarchy);
//        }
        List<Node> currentViewNodes = subnetHierarchy.isEmpty() ? topology.listAllViewNodes()
                : topology.listAllViewNodesInSubnetHierarchy(subnetHierarchy);
        if (CollectionUtils.isEmpty(currentViewNodes)) {
            log.warn("no view node found for subnetId:{}", subnetId);
            return viewNodes;
        }
        int initialCapacity = currentViewNodes.size();
        Map<String, List<NodeWithLevel>> siteAllNodeMap = new HashMap<>(initialCapacity);
        List<Node> pureOldNoSubnetNodes = new ArrayList<>(initialCapacity);

        for (Node viewNode : currentViewNodes) {
            String viewNodeId = viewNode.getNodeId().getValue();
            View view = viewNode.getAugmentation(Node1.class).getView();
            String subnetIdVal = view.getSubnetId();
            boolean hasSubnet = StringUtils.hasText(subnetIdVal);
            if (!hasSubnet) {
                pureOldNoSubnetNodes.add(viewNode);
                continue;
            }

            String realSiteId = ViewNodeNamingRule.extractSiteId(viewNodeId);
            NodeWithLevel nodeWithLevel = NodeWithLevel.builder()
                    .subnetId(subnetIdVal)
                    .subnetLevel(view.getSubnetLevel())
                    .viewNode(viewNode)
                    .build();

            siteAllNodeMap.computeIfAbsent(realSiteId, k -> new ArrayList<>(4)).add(nodeWithLevel);
        }
        viewNodes = isRootFilter
                ? filterRootViewNodes(siteAllNodeMap, pureOldNoSubnetNodes)
                : filterClosestViewNodesBySubnet(siteAllNodeMap);
        log.info("list view nodes success: subnetId={}, isRoot={}, total={}",
                subnetId, isRootFilter, viewNodes.size());
        return viewNodes;
    }

    public List<Link> listViewLinks(String subnetId, ViewLinkType linkLevel) {
        log.info("list all view links by subnetId:{} and viewLinkType:{}", subnetId, linkLevel);
        List<Link> viewLinks = new ArrayList<>();
        boolean isRootFilter = isRootSubnetId(subnetId);
//        List<String> subnetHierarchy = isRootFilter ? new ArrayList<>(0) :
//                SUBNET_HIERARCHY_CACHE.getIfPresent(subnetId);
//        if (subnetHierarchy == null) {
//            subnetHierarchy = getSubnetHierarchy(subnetId);
//            SUBNET_HIERARCHY_CACHE.put(subnetId, subnetHierarchy);
//        }
        List<String> subnetHierarchy;
        try {
            if (isRootFilter) {
                subnetHierarchy = new ArrayList<>();
            } else {
                subnetHierarchy = getSubnetHierarchy(subnetId);
                if (CollectionUtils.isEmpty(subnetHierarchy)) {
                    log.warn("subnet hierarchy is empty for {}, fallback to all view links",
                            subnetId);
                }
            }
        } catch (Exception e) {
            log.error("get subnet hierarchy error", e);
            subnetHierarchy = new ArrayList<>();
        }
        List<Link> refViewLinks = isRootFilter ? topology.listAllViewLinkByLinkLevel(linkLevel)
                : topology.listAllViewLinkBySubnetAndLinkLevel(subnetHierarchy, linkLevel);
//        if (isRootFilter) {
//            viewLinks = filterRootViewLinks(refViewLinks);
//        }
        viewLinks = refViewLinks;
        return viewLinks;
    }


    private List<Node> filterRootViewNodes(Map<String, List<NodeWithLevel>> siteAllNodeMap,
            List<Node> pureOldNoSubnetNodes) {
        List<Node> result = new ArrayList<>(siteAllNodeMap.size());
        Set<String> processedSiteIds = new HashSet<>(siteAllNodeMap.size());
        siteAllNodeMap.forEach((realSiteId, nodeList) -> {
            log.debug("filter the top level realSite by subnet,the nodeList size:{}",
                    nodeList.size());
            List<NodeWithLevel> validSubnetNodes = nodeList.stream()
                    .filter(n -> StringUtils.hasText(n.getSubnetId()) && n.getSubnetLevel() != null)
                    .collect(Collectors.toList());
            if (validSubnetNodes.isEmpty()) {
                log.debug("Site {} has no valid subnet nodes,skip", realSiteId);
                return;
            }
            Node rootNode = buildVirtualParentNode(realSiteId, 0, validSubnetNodes);
            result.add(rootNode);
            processedSiteIds.add(realSiteId);
//                }

        });

        Map<String, Node> noSubnetSiteMap = new HashMap<>(16);
        for (Node node : pureOldNoSubnetNodes) {
            String realSiteId = ViewNodeNamingRule.extractSiteId(node.getNodeId().getValue());
            noSubnetSiteMap.putIfAbsent(realSiteId, node);

        }
        noSubnetSiteMap.forEach((siteId, node) -> {
            if (!processedSiteIds.contains(siteId)) {
                result.add(node);
            }
        });
        return result;
    }

    private Node buildVirtualParentNode(String realSiteId, int minLevel,
            List<NodeWithLevel> leafNodes) {
        log.debug("build the virtual parent node for real siteId:{} ", realSiteId);
        List<NodeWithLevel> validNodes = leafNodes.stream()
                .filter(n -> StringUtils.hasText(n.getSubnetId()) && n.getSubnetLevel() != null)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(validNodes)) {
            return leafNodes.get(0).getViewNode();
        }
        List<String> subnetIds = validNodes.stream()
                .map(NodeWithLevel::getSubnetId)
                .collect(Collectors.toList());
        String lcaId = findLowestCommonAncestor(subnetIds);
        Map<String, SubNetTreeNode> subnetIndex = getSubnetIndex();
        Map<String, List<Node>> parentToChildren = new HashMap<>();
        for (NodeWithLevel node : validNodes) {
            String nodeSubnetId = node.getSubnetId();
            if (nodeSubnetId.equals(lcaId)) {
                continue;
            }
            SubNetTreeNode subnet = subnetIndex.get(node.getSubnetId());

            if (subnet == null || !StringUtils.hasText(subnet.getParentId())) {
                continue;
            }
            parentToChildren.computeIfAbsent(subnet.getParentId(), k -> new ArrayList<>())
                    .add(node.getViewNode());
        }
        Node lcaNode = null;
        while (!parentToChildren.isEmpty()) {
            Map<String, List<Node>> nextLevel = new HashMap<>();
            for (Map.Entry<String, List<Node>> entry : parentToChildren.entrySet()) {
                String currentSubnetId = entry.getKey();
                List<Node> children = entry.getValue();
                SubNetTreeNode currentSubnet = subnetIndex.get(currentSubnetId);
                if (currentSubnet == null) {
                    continue;
                }
                Node currentViewNode = buildParentNode(realSiteId, currentSubnet, children);
                if (currentSubnetId.equals(lcaId)) {
                    lcaNode = currentViewNode;
                    continue;
                }
                if (StringUtils.hasText(currentSubnet.getParentId())) {
                    nextLevel.computeIfAbsent(currentSubnet.getParentId(), k -> new ArrayList<>())
                            .add(currentViewNode);
                }
            }
            parentToChildren = nextLevel;
        }
        if (lcaNode != null) {
            return lcaNode;
        }

        return leafNodes.stream()
                .filter(n -> lcaId.equals(n.getSubnetId()))
                .map(NodeWithLevel::getViewNode)
                .findFirst()
                .orElse(leafNodes.get(0).getViewNode());
//        String lcaViewNodeId = ViewNodeNamingRule.generatedIdWithPlaneId(realSiteId, lcaId);
//        Node lcaViewNode = topology.getViewNodeByViewNodeId(lcaViewNodeId);
//        return lcaViewNode != null ? lcaViewNode : leafNodes.get(0).getViewNode();
    }

    private String findLowestCommonAncestor(List<String> subnetIds) {
        if (CollectionUtils.isEmpty(subnetIds)) {
            return GLOBAL_ROOT_NODE_ID;
        }
        if (subnetIds.size() == 1) {
            return subnetIds.get(0);
        }
        Map<String, SubNetTreeNode> subNetTreeNodeMap = getSubnetIndex();
        String result = subnetIds.get(0);
        for (int i = 1; i < subnetIds.size(); i++) {
            result = lcaOfTwo(result, subnetIds.get(i), subNetTreeNodeMap);
            if (result.equals(GLOBAL_ROOT_NODE_ID)) {
                break;
            }
        }
        return result;
    }

    private String lcaOfTwo(String nodeA, String nodeB,
            Map<String, SubNetTreeNode> subNetTreeNodeMap) {
        SubNetTreeNode aNode = subNetTreeNodeMap.get(nodeA);
        SubNetTreeNode bNode = subNetTreeNodeMap.get(nodeB);
        if (aNode == null || bNode == null || aNode.getParentId() == null
                || bNode.getParentId() == null) {
            return GLOBAL_ROOT_NODE_ID;
        }
        int depthA = aNode.getLevel();
        int depthB = bNode.getLevel();
        if (depthA < depthB) {
            String temp = nodeA;
            nodeA = nodeB;
            nodeB = temp;
            int tempDepth = depthA;
            depthA = depthB;
            depthB = tempDepth;
        }
        int depthDiff = depthA - depthB;
        for (int i = 0; i < depthDiff; i++) {
            SubNetTreeNode current = subNetTreeNodeMap.get(nodeA);
            if (current == null || !StringUtils.hasText(current.getParentId())) {
                return GLOBAL_ROOT_NODE_ID;
            }
            nodeA = current.getParentId();
        }
        if (nodeA.equals(nodeB)) {
            return nodeA;
        }
        while (true) {
            SubNetTreeNode currA = subNetTreeNodeMap.get(nodeA);
            SubNetTreeNode currB = subNetTreeNodeMap.get(nodeB);
            if (currA == null || currB == null) {
                return GLOBAL_ROOT_NODE_ID;
            }
            String parentA = currA.getParentId();
            String parentB = currB.getParentId();
            if (!StringUtils.hasText(parentA) || !StringUtils.hasText(parentB)) {
                return GLOBAL_ROOT_NODE_ID;
            }
            if (parentA.equals(parentB)) {
                return parentA;
            }
            nodeA = parentA;
            nodeB = parentB;
        }
    }


    private Node buildParentNode(String realSiteId, SubNetTreeNode subNetTreeNode,
            List<Node> viewNodes) {
        log.debug("build parent Node for the view node");
        Node viewNode = viewNodes.get(0);
        String subnetId = subNetTreeNode.getSubNetId();
        Integer level = subNetTreeNode.getLevel();
        String subnetName = subNetTreeNode.getName();
        String viewNodeId = ViewNodeNamingRule.generatedIdWithPlaneId(realSiteId, subnetId);
        Node refViewNode = topology.getViewNodeByViewNodeId(viewNodeId);
        if (refViewNode != null) {
            return refViewNode;
        }
        AlarmSeverity alarmSeverity = calculateViewNodeAlarmSeverity(viewNodes);
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NodeId.getDefaultInstance(viewNodeId));
        View view = viewNode.getAugmentation(Node1.class).getView();
        ViewBuilder viewBuilder = new ViewBuilder(view);
        viewBuilder.setSubnetId(subnetId);
        viewBuilder.setSubnetLevel(level);
        viewBuilder.setSubnetName(subnetName);
        viewBuilder.setAlarmState(alarmSeverity);
        Node1Builder node1Builder = new Node1Builder();
        node1Builder.setView(viewBuilder.build());
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());
        Node viewRefNode = nodeBuilder.build();
        topology.updateViewNode(viewRefNode);
        return viewRefNode;
    }


    private AlarmSeverity calculateViewNodeAlarmSeverity(List<Node> viewNodes) {
        log.debug("recalculate Alarm severity ");
        List<AlarmSeverity> alarmSeverities = viewNodes.stream()
                .map(node -> node.getAugmentation(Node1.class).getView())
                .map(View::getAlarmState).collect(Collectors.toList());
        return calculateAlarmSeverity(alarmSeverities);
    }

    private List<Node> filterClosestViewNodesBySubnet(
            Map<String, List<NodeWithLevel>> siteAllNodeMap) {
        if (CollectionUtils.isEmpty(siteAllNodeMap)) {
            return new ArrayList<>();
        }

        List<Node> nodes = new ArrayList<>(siteAllNodeMap.size());
        for (Map.Entry<String, List<NodeWithLevel>> entry : siteAllNodeMap.entrySet()) {
            String realSiteId = entry.getKey();
            List<NodeWithLevel> nodeList = entry.getValue();

            if (CollectionUtils.isEmpty(nodeList)) {
                continue;
            }

            List<NodeWithLevel> validSubnetNodes = nodeList.stream()
                    .filter(n -> StringUtils.hasText(n.getSubnetId()) && n.getSubnetLevel() != null)
                    .collect(Collectors.toList());

            if (validSubnetNodes.isEmpty()) {

                nodes.add(nodeList.get(0).getViewNode());
                continue;
            }

            Node closestNode = buildVirtualParentNode(realSiteId, 0, validSubnetNodes);
            nodes.add(closestNode);

            log.debug("Site {} closest view node at subnet: {}",
                    realSiteId, closestNode.getNodeId().getValue());
        }

        return nodes;

    }

    private List<String> getSubnetHierarchy(String parentId) {
//        return topology.getPlaneDescendants(subnetId);
//        return getSubnetHolder().getId2NodeMap()
        List<String> result = new ArrayList<>();
        Map<String, List<String>> parent2ChildIds = getSubnetHolder().getParent2ChildIds();
        Queue<String> queue = new LinkedList<>();
        queue.offer(parentId);
        result.add(parentId);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            List<String> children = parent2ChildIds.get(currentId);
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            for (String childId : children) {
                result.add(childId);
                queue.offer(childId);
            }
        }
        return result;
//        Map<String, SubNetTreeNode> index = getSubnetIndex();
//        if (index.isEmpty()) {
//            return new ArrayList<>();
//        }
//        List<String> descendants = new ArrayList<>();
//        collectDescendants(subnetId, index, descendants);
//        return descendants;
    }


    private boolean isRootSubnetId(String subnetId) {
        return !StringUtils.hasText(subnetId) || subnetId.equals(GLOBAL_ROOT_NODE_ID);
    }


    @Data
    @Builder
    public static class NodeWithLevel implements Serializable {

        private String subnetId; // 允许为空（原有节点）
        private Integer subnetLevel; // 允许为空（原有节点）
        private Node viewNode;

    }

    /**
     * 子网索引持有类：打包ID索引 + 父子邻接表，一次加载，多处复用
     */
    @Data
    @Builder
    private static class SubnetIndexHolder {

        /**
         * 子网ID -> 节点对象 索引
         */
        private Map<String, SubNetTreeNode> id2NodeMap;
        /**
         * 父子网ID -> 直接子子网ID列表 邻接表
         */
        private Map<String, List<String>> parent2ChildIds;
    }


}
