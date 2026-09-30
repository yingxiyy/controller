package net.flex.dci.otc.controller.status.core.processor.alarm;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.controller.status.core.handler.StateChangeChainHandler;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/2 16:50
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmClearStateProcessor {

    private final StateChangeChainHandler stateChainHandler;

    public void process(String key, List<Alarm> value, PhyNodeCache phyNodeCache) {
        stateChainHandler.executeAllAlarmClearStateChangeChainHandle(key, value, phyNodeCache);
    }
}
