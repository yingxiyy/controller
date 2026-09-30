package net.flex.dci.otc.controller.ne.manager.monitor.handler;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;

/**
 * @version 1.0
 * @date 6/10/2025 2:47 PM
 */
@Slf4j
public class DefaultMapperChangeEventHandler extends AbstractMapperChangeEventHandler {

    @Override
    public void handleNodeRemove(InstanceDetails instanceDetails) {
        log.warn("a mapper node:{} remove ,mapperType not support", instanceDetails);
    }

    @Override
    public void handleNodeAdded(InstanceDetails instanceDetails) {
        log.warn("a mapper node:{} added,mapperType not support", instanceDetails);
    }


    @Override
    public MapperType mapperType() {
        return MapperType.UNKNOWN;
    }
}
