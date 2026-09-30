package net.flex.dci.otc.controller.rpc.client.balancer;

import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;

/**
 * @version 1.0
 * @date 2022/4/14 15:24
 */
@Slf4j
public class RandomBalancer implements Balancer {

    @Override
    public InstanceDetails select(List<InstanceDetails> serverInstances) throws CommonException {
        log.trace("start to use random balancer to load balancer");
        if (serverInstances == null) {
            return null;
        }
        int size = serverInstances.size();
        if (size == 0) {
            return null;
        }
        Random r = new Random();
        int index = r.nextInt(size);
        return serverInstances.get(index);
    }
}
