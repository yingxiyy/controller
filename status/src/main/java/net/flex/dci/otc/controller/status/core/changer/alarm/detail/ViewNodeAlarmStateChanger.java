package net.flex.dci.otc.controller.status.core.changer.alarm.detail;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.alarm.ViewNodeAlarmState;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.dto.batch.ViewNodeAlarmStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/4 14:53
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ViewNodeAlarmStateChanger implements IStateChanger<ViewNodeAlarmState> {

    private final ViewNodeDao viewNodeDao;
    private final NodeCacheManager nodeCacheManager;

    @Override
    public void changeState(ViewNodeAlarmState viewNodeAlarmState) {
        log.debug("start to change alarm state for the view node,detail is :{}",
                viewNodeAlarmState);
        AlarmSeverity alarmSeverity = viewNodeAlarmState.getAlarmSeverity();
        String viewNodeId = viewNodeAlarmState.getViewNodeId();
        viewNodeDao.updateViewNodeAlarmState(viewNodeId, alarmSeverity);
        // 失效缓存：ViewNode变化可能影响siteNode和拓扑缓存
        nodeCacheManager.invalidateSiteNode(viewNodeId);
        log.debug("end to change view node detail ");
    }

    @Override
    public void changeBatchState(List<ViewNodeAlarmState> viewNodeAlarmStates) {
        log.debug("batch change the viewNode alarm state,the viewNode alarm states is:{}",
                viewNodeAlarmStates);
        List<ViewNodeAlarmStatus> viewNodeAlarmStatuses = viewNodeAlarmStates.stream()
                .map(viewNodeAlarmState -> ViewNodeAlarmStatus.builder()
                        .viewNodeId(viewNodeAlarmState.getViewNodeId())
                        .alarmSeverity(viewNodeAlarmState.getAlarmSeverity())
                        .build())
                .collect(Collectors.toList());
        viewNodeDao.bulkUpdateViewNodeAlarmState(viewNodeAlarmStatuses);
        // 批量失效缓存
        for (ViewNodeAlarmState viewNodeAlarmState : viewNodeAlarmStates) {
            nodeCacheManager.invalidateSiteNode(viewNodeAlarmState.getViewNodeId());
        }
        log.info("viewNode batch alarm state update completed, count={}",
                viewNodeAlarmStates.size());
    }
}
