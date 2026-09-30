package net.flex.dci.otn.controller.schedule.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zkclient4boot.refactor.core.listener.AbstractLeaderElector;
import net.flex.dci.otn.controller.schedule.component.DynamicScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 2026/8/18
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class LeaderElector extends AbstractLeaderElector {

    @Autowired
    @Lazy
    private DynamicScheduler dynamicScheduler;

    @Override
    protected void takeLeadership() {
        log.info("become leader, refresh all scheduled tasks");
        dynamicScheduler.refreshAllTasks();
    }

    @Override
    protected void loseLeadership() {
        log.info("lost leadership, refresh all scheduled tasks");
        dynamicScheduler.refreshAllTasks();
    }
}
