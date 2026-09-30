package net.flex.dci.otn.controller.gateway.dispatch.loadbalance.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.gateway.dispatch.loadbalance.Balancer;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;

/**
 * @version 1.0
 * @date 2022/6/29 10:15
 */
@Slf4j
public class LeastConnectionBalancer implements Balancer {

    @Override
    public ServerInstance select(List<ServerInstance> serverInstances) {
        return null;
    }
}
