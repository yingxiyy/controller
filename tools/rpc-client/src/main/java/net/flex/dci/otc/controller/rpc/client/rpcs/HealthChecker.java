package net.flex.dci.otc.controller.rpc.client.rpcs;

import net.flex.dci.otc.controller.rpc.client.enums.HealthStatus;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;

/**
 *
 * @version 1.0
 * @date 12/11/2025 1:08 PM
 */
public interface HealthChecker {

    HealthStatus checkHealth(Adapter adapter);

    HealthStatus checkHealth(InstanceDetails instanceDetails);
}
