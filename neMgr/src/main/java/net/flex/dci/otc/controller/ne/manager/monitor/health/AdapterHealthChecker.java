package net.flex.dci.otc.controller.ne.manager.monitor.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.enums.HealthStatus;
import net.flex.dci.otc.controller.rpc.client.rpcs.HealthChecker;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 12/11/2025 11:20 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdapterHealthChecker {

    private final HealthChecker healthChecker;

    public HealthStatus checkHealth(Adapter adapter) {
        log.info("check current adapter:{} health status", adapter.getName());
        return healthChecker.checkHealth(adapter);
    }

}
