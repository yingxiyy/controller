package net.flex.dci.otn.controller.gateway.dispatch.loadbalance;

import java.util.List;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;

/**
 * @version 1.0
 * @date 2022/4/14 15:21
 */
public interface Balancer {

    public ServerInstance select(List<ServerInstance> serverInstances);
}
