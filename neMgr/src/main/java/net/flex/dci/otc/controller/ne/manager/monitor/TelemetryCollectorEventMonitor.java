package net.flex.dci.otc.controller.ne.manager.monitor;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.controller.ne.manager.monitor.handler.MapperEventHandler;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 6/9/2025 3:23 PM
 */
@Component
@Slf4j
public class TelemetryCollectorEventMonitor extends AbstractMapperEventMonitor {

    public TelemetryCollectorEventMonitor(
            MapperEventHandler mapperManager) {
        super(mapperManager);
    }

    @Override
    public void handle(Type type, InstanceDetails detail) {
        log.debug("a telemetry collector change event happening,event type:{} and instance is:{}",
                type, detail);
        switch (type) {
            case NODE_ADDED:
                handleMapperAdd(detail);
                break;
            case NODE_REMOVED:
                handleMapperRemove(detail);
                break;
            case NODE_UPDATED:
                handleMapperUpdate(detail);
                break;
        }
    }


    @Override
    protected MapperType supportMapperType() {
        return MapperType.TELEMETRY_COLLECTOR;
    }
}
