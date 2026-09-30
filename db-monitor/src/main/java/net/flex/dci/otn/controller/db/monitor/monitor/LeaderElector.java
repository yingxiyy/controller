package net.flex.dci.otn.controller.db.monitor.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zkclient4boot.refactor.core.listener.AbstractLeaderElector;
import net.flex.dci.otn.controller.db.monitor.core.CDCListener;
import net.flex.dci.otn.controller.db.monitor.properties.DbMonitorProperties;
import org.springframework.stereotype.Component;

/**
 * 2026/9/19
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class LeaderElector extends AbstractLeaderElector {

    private final DbMonitorProperties dbMonitorProperties;

    private final CDCListener cdcListener;

    @Override
    protected void takeLeadership() {

        if (!dbMonitorProperties.getHa().isEnabled()) {
            return;
        }
        log.info("current instance take the leadership ");
        cdcListener.startEngine();
    }

    @Override
    protected void loseLeadership() {
        if (!dbMonitorProperties.getHa().isEnabled()) {
            return;
        }
        cdcListener.stopEngine();
    }
}
