package net.flex.dci.otc.controller.ne.manager.monitor.leader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zkclient4boot.refactor.core.listener.AbstractLeaderElector;
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

    @Override
    protected void takeLeadership() {

    }

    @Override
    protected void loseLeadership() {

    }
}
