package net.flex.dci.otc.controller.status.core.changer.alarm.detail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.alarm.SiteNodeAlarmState;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/4 14:55
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SiteNodeAlarmStateChanger implements IStateChanger<SiteNodeAlarmState> {

    private final SiteNodeDao siteNodeDao;
    private final NodeCacheManager nodeCacheManager;

    @Override
    public void changeState(SiteNodeAlarmState state) {
        log.debug("update the site node change,{}", state);
        String siteNodeId = state.getSiteNodeId();
        AlarmSeverity updateSeverity = state.getAlarmSeverity();
        siteNodeDao.updateSiteNodeAlarmSeverity(siteNodeId, updateSeverity);
        // 失效缓存
        nodeCacheManager.invalidateSiteNode(siteNodeId);
        log.debug("end to update the site node alarm state change");
    }
}
