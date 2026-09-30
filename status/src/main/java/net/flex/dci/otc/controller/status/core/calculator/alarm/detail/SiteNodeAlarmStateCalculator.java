package net.flex.dci.otc.controller.status.core.calculator.alarm.detail;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.calculator.alarm.AbstractAlarmStateCalculator;
import net.flex.dci.otc.controller.status.dto.alarm.SiteNodeAlarmState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/2 17:15
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteNodeAlarmStateCalculator extends
        AbstractAlarmStateCalculator<SiteNodeAlarmState, Node> {


    private final NodeCacheManager nodeCacheManager;

    @Override
    public SiteNodeAlarmState calculate(Node phyNode) {
        String phyNodeId = phyNode.getNodeId().getValue();
        log.debug("start to calculate the ref site node alarm state,phy node id is:{}", phyNodeId);
        String siteId = PhysicalNodeIdNamingRule.getSiteId(phyNodeId);
        Node siteNode = nodeCacheManager.getSiteNode(siteId);
        if (siteNode == null) {
            log.info("this is useless alarm,discard it");
            return null;
        }
//        List<Node> phyNodes = phyNodeDao.listConfigPhyNodeBySiteNodeId(
//                siteNode.getNodeId().getValue());
        List<String> refPhyNodeIds = siteNode.getSupportingNode().stream()
                .map(SupportingNode::getNodeRef).map(Uri::getValue)
                .collect(Collectors.toList());
//        List<>
//        AlarmSeverity currentAlarmState = StatusUtil.calculateAlarmSeverityByPhyNodes(phyNodes);
        AlarmSeverity currentAlarmState = calculateCurrentAlarmSeverity(refPhyNodeIds);
        return SiteNodeAlarmState.builder().siteNodeId(siteNode.getNodeId().getValue())
                .alarmSeverity(currentAlarmState).build();
    }

    @Override
    public SiteNodeAlarmState calculate(String id) {
        log.info("start to calculate the site node :{} current alarm state", id);
        String siteId = PhysicalNodeIdNamingRule.getSiteId(id);
        Node refSiteNode = nodeCacheManager.getSiteNode(siteId);
        if (null == refSiteNode) {
            log.warn("invalid site node:{},discard it", id);
            return null;
        }
//        AlarmSeverity siteAlarmSeverity = refSiteNode.getAugmentation(Node1.class).getSite()
//                .getAlarmState();
//        AlarmSeverity currentAlarmSeverity = StatusUtil.calculateAlarmSeverity(siteAlarmSeverity,
//                severity);
        List<String> refPhyNodeIds = refSiteNode.getSupportingNode().stream()
                .map(SupportingNode::getNodeRef).map(Uri::getValue)
                .collect(Collectors.toList());
        AlarmSeverity currentAlarmState = calculateCurrentAlarmSeverity(refPhyNodeIds);
        log.info("current siteId is the alarm severity is:{}", currentAlarmState);
        return SiteNodeAlarmState.builder().siteNodeId(siteId)
                .alarmSeverity(currentAlarmState).build();
    }


    @Override
    public SiteNodeAlarmState calculateSiteCurrentState(String siteId) {
        Node refSiteNode = nodeCacheManager.getSiteNode(siteId);
        if (null == refSiteNode) {
            log.warn("invalid site node :{},discard it", siteId);
            return null;
        }
        List<String> refPhyNodeIds = refSiteNode.getSupportingNode().stream()
                .map(SupportingNode::getNodeRef).map(Uri::getValue)
                .collect(Collectors.toList());
        log.info("[SITE-CALC] siteId={}, phyNodeCount={}, phyNodeIds={}",
                siteId, refPhyNodeIds.size(), refPhyNodeIds);
        AlarmSeverity currentAlarmState = calculateCurrentAlarmSeverity(refPhyNodeIds);
        log.info("[SITE-CALC] siteId={}, calculated severity={}", siteId, currentAlarmState);

        return SiteNodeAlarmState.builder().siteNodeId(siteId)
                .alarmSeverity(currentAlarmState).build();
    }
}
