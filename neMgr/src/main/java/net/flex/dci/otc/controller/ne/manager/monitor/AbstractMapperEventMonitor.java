package net.flex.dci.otc.controller.ne.manager.monitor;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.controller.ne.manager.monitor.handler.MapperEventHandler;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.core.handler.ZkEventHandler;

/**
 * @version 1.0
 * @date 6/9/2025 4:05 PM
 */
@Slf4j
public abstract class AbstractMapperEventMonitor implements ZkEventHandler {


    protected final MapperEventHandler mapperManager;

    public AbstractMapperEventMonitor(MapperEventHandler mapperManager) {
        this.mapperManager = mapperManager;
    }

    protected abstract MapperType supportMapperType();

    @Override
    public List<String> supportModuleNames() {
        return supportMapperType().getModuleNames();
    }

    protected void handleMapperAdd(InstanceDetails detail) {
        log.debug("a mapper add ,the instance is:{}", detail);
        mapperManager.handleMapperAdd(supportMapperType(), detail);
    }

    protected void handleMapperRemove(InstanceDetails detail) {
        log.debug("a mapper remove,the instance is:{}", detail);
        mapperManager.handleMapperRemove(supportMapperType(), detail);
    }

    protected void handleMapperUpdate(InstanceDetails detail) {
        log.debug("a mapper update,the instance is:{}", detail);
        //todo: handle mapper update
    }
}
