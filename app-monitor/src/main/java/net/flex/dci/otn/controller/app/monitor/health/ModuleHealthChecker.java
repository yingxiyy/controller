package net.flex.dci.otn.controller.app.monitor.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.enums.HealthStatus;
import net.flex.dci.otc.controller.rpc.client.rpcs.HealthChecker;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 12/11/2025 4:51 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModuleHealthChecker {

    private final HealthChecker healthChecker;

    public HealthStatus checkInstanceHealthStatus(InstanceDetails instance) {
        log.info("start to check instance health status");
        return healthChecker.checkHealth(instance);
    }
}
