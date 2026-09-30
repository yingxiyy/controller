package net.flex.dci.otc.controller.status.core.changer.alarm.detail;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.alarm.RackAlarmState;
import net.flex.dci.otc.mongo.dao.RackDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/3/2023 12:22 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RackAlarmStateChanger implements IStateChanger<RackAlarmState> {

    private final RackDao rackDao;

    private final NodeCacheManager nodeCacheManager;

    @Override
    public void changeState(RackAlarmState state) {
        log.debug("start to change the rack alarm state,the rack state is:{} the ref site is:{}",
                state.getRackId(), state.getSiteNodeId());
        String siteId = state.getSiteNodeId();
        String rackId = state.getRackId();
        AlarmSeverity alarmSeverity = state.getAlarmSeverity();
        rackDao.updateRackAlarmState(siteId, rackId, alarmSeverity);
        nodeCacheManager.invalidateRackCacheByNeIds(state.getNeIds());
    }

    public void changeBatchState(List<RackAlarmState> rackAlarmStates) {
        log.debug("batch change the rack alarm state,the rack alarm states is:{}",
                rackAlarmStates);
        List<net.flex.dci.otc.mongo.dto.batch.RackAlarmState> updateRackAlarmState = rackAlarmStates.stream()
                .map(rackAlarmState -> net.flex.dci.otc.mongo.dto.batch.RackAlarmState.builder()
                        .rackId(rackAlarmState.getRackId())
                        .siteId(rackAlarmState.getSiteNodeId())
                        .alarmSeverity(rackAlarmState.getAlarmSeverity())
                        .build()).collect(
                        Collectors.toList());
        rackDao.bulkUpdateRackAlarmState(updateRackAlarmState);
        List<String> allNeIds = rackAlarmStates.stream()
                .map(RackAlarmState::getNeIds)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        nodeCacheManager.invalidateRackCacheByNeIds(allNeIds);
    }


}
