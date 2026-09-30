package net.flex.dci.otc.controller.status.core.calculator.alarm.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.IAlarmStateCalculator;
import net.flex.dci.otc.controller.status.core.enums.AlarmSeverityCode;
import net.flex.dci.otc.controller.status.dto.alarm.ViewNodeAlarmState;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.dto.PhyNodeAlarmDto;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/2 17:15
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewNodeAlarmStateCalculator implements
        IAlarmStateCalculator<ViewNodeAlarmState, Node> {

    private final ViewNodeDao viewNodeDao;


    private final PhyNodeDao phyNodeDao;

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private final NodeCacheManager nodeCacheManager;

    @Override
    public List<ViewNodeAlarmState> calculateRefs(String id) {
        log.info("start to calculate the phy node ref view node alarm state ,phy node id is:{}",
                id);
//        List<ViewNodeAlarmState> result = new ArrayList<>();
//        try {
//            long t1 = System.currentTimeMillis();
//            Node refPhyNode = nodeCacheManager.getConfigNode(id);
//            String subnetId = StatusUtil.getPhyNodeRefSubnetId(refPhyNode);
//            String siteId = PhysicalNodeIdNamingRule.getSiteId(id);
//            Node siteNode = nodeCacheManager.getSiteNode(siteId);
//            if (siteNode == null) {
//                log.info("this is useless alarm,discard it");
//                return result;
//            }
//            if (subnetId == null || subnetId.trim().isEmpty()) {
//                log.info("phy node {} has no subnet assigned, calculate site-level view node", id);
//                ViewNodeAlarmState siteLevelAlarmState = calculateSiteCurrentState(siteId);
//                if (siteLevelAlarmState != null) {
//                    result.add(siteLevelAlarmState);
//                }
//                return result;
//            }
//
//            SubNetTreeNode currentSubnet = subNetTreeNodeDao.findBySubNetId(
//                            subnetId)
//                    .orElseThrow(() -> new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                            "subnet:" + subnetId + " is not found"));
//            List<String> pathIds = currentSubnet.getPathIds();
//            if (pathIds.isEmpty()) {
//                log.warn("subnet {} has empty pathIds, skip", subnetId);
//                return result;
//            }
//
//            Collections.reverse(pathIds);
//            List<String> viewNodeIds = pathIds.stream()
//                    .map(subnet -> ViewNodeNamingRule.generatedIdWithPlaneId(siteId, subnet))
//                    .collect(Collectors.toList());
//            List<Node> viewNodes = viewNodeDao.listAllViewNodesByIds(viewNodeIds);
//            List<SubNetTreeNode> allDescendants = subNetTreeNodeDao.getAllDescendants(pathIds);
//            List<String> allSubnetIds = allDescendants.stream()
//                    .map(SubNetTreeNode::getSubNetId).collect(
//                            Collectors.toList());
//            List<PhyNodeAlarmDto> allAlarms = phyNodeDao.listPhyNodeAlarmInSubnetIds(siteId,
//                    allSubnetIds);
//            for (String ancestorId : pathIds) {
//                ViewNodeAlarmState state = calculateFromCache(ancestorId, siteId, viewNodes,
//                        allDescendants,
//                        allAlarms);
//                if (state != null) {
//                    result.add(state);
//                }
//
//            }
//        } catch (Exception e) {
//            log.error("calculate view node alarm state failed, phy node id:{}", id, e);
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "calculate view node alarm state failed", e);
//        }
//        return result;
        String siteId = PhysicalNodeIdNamingRule.getSiteId(id);
        return aggregateOneSite(siteId);
    }

    private ViewNodeAlarmState calculateFromCache(String subnetId, String siteId,
            List<Node> viewNodes,
            List<SubNetTreeNode> allDescendants, List<PhyNodeAlarmDto> allAlarms) {
        log.debug("calculate ancestor view node alarm state from batch data, siteId:{} subnetId:{}",
                siteId, subnetId);
        String viewNodeId = ViewNodeNamingRule.generatedIdWithPlaneId(siteId, subnetId);

        Node viewNode = viewNodes.stream()
                .filter(n -> n.getNodeId().getValue().equals(viewNodeId))
                .findFirst()
                .orElse(null);
        if (viewNode == null) {
            log.warn("ancestor view node not found, viewNodeId:{}, skip", viewNodeId);
            return null;
        }

        List<String> descendantSubnetIds = allDescendants.stream()
                .filter(d -> d.getPathIds() != null && d.getPathIds().contains(subnetId))
                .map(SubNetTreeNode::getSubNetId)
                .collect(Collectors.toList());

        List<PhyNodeAlarmDto> phyNodeAlarmDtos = allAlarms.stream()
                .filter(a -> descendantSubnetIds.contains(a.getSubnetId()))
                .collect(Collectors.toList());

        AlarmSeverity alarmSeverity = StatusUtil.calculateAlarmStateByList(
                phyNodeAlarmDtos.stream()
                        .map(PhyNodeAlarmDto::getAlarmSeverity)
                        .collect(Collectors.toList()));

        log.info("ancestor view node alarm state calculated, viewNodeId:{}, severity:{}",
                viewNodeId, alarmSeverity);
        return ViewNodeAlarmState.builder()
                .alarmSeverity(alarmSeverity)
                .viewNodeId(viewNodeId)
                .build();
    }


    @Override
    public ViewNodeAlarmState calculateSiteCurrentState(String siteId) {
        Node refSiteNode = nodeCacheManager.getSiteNode(siteId);
        if (null == refSiteNode) {
            log.warn("invalid site node :{},discard it", siteId);
            return null;
        }
        AlarmSeverity alarmSeverity = refSiteNode.getAugmentation(Node1.class).getSite()
                .getAlarmState();
        return ViewNodeAlarmState.builder().viewNodeId(siteId)
                .alarmSeverity(alarmSeverity).build();
    }


    @Override
    public List<ViewNodeAlarmState> calculateViewNodeAlarms(List<String> viewNodeIds) {
        log.debug("calculate current view node alarms state,the view NodeIds is:{}", viewNodeIds);
        if (CollectionUtils.isEmpty(viewNodeIds)) {
            return Collections.emptyList();
        }
        Set<String> siteIds = new HashSet<>();
        for (String vnId : viewNodeIds) {
            if (ViewNodeNamingRule.isViewNodeId(vnId)) {
                siteIds.add(ViewNodeNamingRule.parseViewNode(vnId).getSiteId());
            }
        }
        return recalcBySiteIds(siteIds);
    }

    private List<ViewNodeAlarmState> recalcBySiteIds(Set<String> siteIds) {
        List<ViewNodeAlarmState> result = new ArrayList<>();
        for (String siteId : siteIds) {
            try {
                result.addAll(aggregateOneSite(siteId));
            } catch (Exception e) {
                log.error("[view-node] site={} aggregate failed", siteId, e);
            }
        }
        return result;
    }

    public List<ViewNodeAlarmState> calculateViewNodeAlarmsByNeIds(Set<String> neIds) {
        if (CollectionUtils.isEmpty(neIds)) {
            return Collections.emptyList();
        }
        Map<String, Set<String>> bySite = neIds.stream().collect(
                Collectors.groupingBy(PhysicalNodeIdNamingRule::getSiteId, Collectors.toSet()));
        List<ViewNodeAlarmState> result = recalcBySiteIds(bySite.keySet());
        return result;
    }

    private List<ViewNodeAlarmState> aggregateOneSite(String siteId) {
        long start = System.currentTimeMillis();
        List<Node> viewNodes = viewNodeDao.listViewNodesBySiteId(siteId);
        if (CollectionUtils.isEmpty(viewNodes)) {
            return Collections.emptyList();
        }
        Map<String, String> subnetToViewNodeId = new HashMap<>();
        Map<String, AlarmSeverity> current = new HashMap<>();
        for (Node vn : viewNodes) {
            String id = vn.getNodeId().getValue();
            if (id.equals(siteId) || !ViewNodeNamingRule.isViewNodeId(id)) {
                continue;
            }
            subnetToViewNodeId.put(ViewNodeNamingRule.parseViewNode(id).getSubnetId(), id);
            current.put(id, currentSeverityOf(vn));
        }
        if (subnetToViewNodeId.isEmpty()) {
            return Collections.emptyList();
        }
        List<PhyNodeAlarmDto> alarms = phyNodeDao.listPhyNodeAlarmInSubnetIds(siteId,
                new ArrayList<>(subnetToViewNodeId.keySet()));
        Map<String, Integer> acc = new HashMap<>();
        for (PhyNodeAlarmDto alarm : alarms) {
            if (alarm.getSubnetId() == null || alarm.getAlarmSeverity() == null) {
                continue;
            }
            acc.merge(alarm.getSubnetId(),
                    AlarmSeverityCode.getSeverityCode(alarm.getAlarmSeverity()), (o, n) -> o | n);
        }
        if (!acc.isEmpty()) {
            Map<String, List<String>> paths = loadPaths(subnetToViewNodeId.keySet());
            for (Map.Entry<String, Integer> e : new ArrayList<>(acc.entrySet())) {
                Integer code = e.getValue();
                if (code == null || code == 0) {
                    continue;
                }
                for (String ancestor : paths.getOrDefault(e.getKey(), Collections.emptyList())) {
                    acc.merge(ancestor, code, (o, n) -> o | n);
                }
            }
        }
        List<ViewNodeAlarmState> result = new ArrayList<>();
        for (Map.Entry<String, String> e : subnetToViewNodeId.entrySet()) {
            AlarmSeverity target = toSeverity(acc.get(e.getKey()));
            if (!target.equals(current.get(e.getValue()))) {
                result.add(ViewNodeAlarmState.builder()
                        .viewNodeId(e.getValue()).alarmSeverity(target).build());
            }
        }
        log.info("[view-node] site={} viewNodes={} alarms={} changed={} cost={}ms",
                siteId, subnetToViewNodeId.size(), alarms.size(), result.size(),
                System.currentTimeMillis() - start);
        return result;
    }

    private AlarmSeverity toSeverity(Integer code) {
        return code == null || code == 0 ? AlarmSeverity.Cleared
                : AlarmSeverityCode.getSeverity(code);
    }

    private Map<String, List<String>> loadPaths(Set<String> subnetIds) {
        if (CollectionUtils.isEmpty(subnetIds)) {
            return Collections.emptyMap();
        }
        List<SubNetTreeNode> nodes = subNetTreeNodeDao.getSubNetBySubnetIds(
                new ArrayList<>(subnetIds));
        Map<String, List<String>> paths = new HashMap<>();
        for (SubNetTreeNode node : nodes) {
            paths.put(node.getSubNetId(), node.getPathIds() == null
                    ? Collections.<String>emptyList() : new ArrayList<>(node.getPathIds()));
        }
        return paths;
    }

    private AlarmSeverity currentSeverityOf(Node vn) {
        return vn.getAugmentation(
                        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1.class)
                .getView().getAlarmState();
    }


}
