package net.flex.dci.otc.controller.ne.manager.monitor.handler;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.controller.ne.manager.monitor.leader.LeaderElector;
import net.flex.dci.otc.zkclient4boot.refactor.core.handler.InstanceChangeHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 6/9/2025 5:06 PM
 */
@Slf4j
public abstract class AbstractMapperChangeEventHandler implements InstanceChangeHandler {

    public abstract MapperType mapperType();

    @Autowired
    protected LeaderElector leaderElector;
}
