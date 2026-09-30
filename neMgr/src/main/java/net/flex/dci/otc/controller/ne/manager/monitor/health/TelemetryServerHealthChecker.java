package net.flex.dci.otc.controller.ne.manager.monitor.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.enums.HealthStatus;
import net.flex.dci.otc.controller.rpc.client.rpcs.HealthChecker;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 12/11/2025 11:25 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TelemetryServerHealthChecker {

    private final HealthChecker healthChecker;

    public HealthStatus checkHealth(InstanceDetails instanceDetails) {
        log.info("check telemetry server:{} health status", instanceDetails.getId());
        return healthChecker.checkHealth(instanceDetails);
    }

}
