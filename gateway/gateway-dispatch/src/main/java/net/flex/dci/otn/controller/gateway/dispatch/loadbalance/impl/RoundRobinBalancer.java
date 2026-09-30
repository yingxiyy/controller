package net.flex.dci.otn.controller.gateway.dispatch.loadbalance.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.zkclient4boot.refactor.utils.ZkUtils;
import net.flex.dci.otn.controller.gateway.dispatch.loadbalance.Balancer;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.utils.CloseableUtils;

/**
 * @version 1.0
 * @date 2022/4/14 15:24
 */
@Slf4j
public class RoundRobinBalancer implements Balancer {

    public static final String ROUND_PATH = "/round";

    @Override
    public ServerInstance select(List<ServerInstance> serverInstances) throws CommonException {
        if (serverInstances == null) {
            return null;
        }
        if (serverInstances.isEmpty()) {
            return null;
        }
        int size = serverInstances.size();
        log.debug("round robin balancer choose");
        int round = -1;
        SharedCount sharedCount = new SharedCount(ZkUtils.getClient(), ROUND_PATH,
                -1);
        boolean success = false;
        try {
            sharedCount.start();
            while (!success) {
                round = sharedCount.getCount();
                round++;
                if (round >= Integer.MAX_VALUE) {
                    round = -1;
                }
                success = sharedCount.trySetCount(sharedCount.getVersionedValue(), round);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(sharedCount);
        }
        int index = round % size;
        return serverInstances.get(index);
    }
}
