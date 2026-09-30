package net.flex.dci.otc.controller.ne.manager.monitor.handler;

import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;

/**
 * @version 1.0
 * @date 6/9/2025 4:41 PM
 */
public interface MapperEventHandler {

    void handleMapperAdd(MapperType mapperType, InstanceDetails instance);

    void handleMapperRemove(MapperType mapperType, InstanceDetails instance);
}
