package net.flex.dci.otc.controller.rpc.client.balancer;

import java.util.List;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;

/**
 * @version 1.0
 * @date 2022/4/14 15:21
 */
public interface Balancer {

    public InstanceDetails select(List<InstanceDetails> serverInstances) throws Exception;
}
