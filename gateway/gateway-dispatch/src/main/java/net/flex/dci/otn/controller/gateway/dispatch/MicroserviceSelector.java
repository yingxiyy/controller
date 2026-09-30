package net.flex.dci.otn.controller.gateway.dispatch;

import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otn.controller.gateway.dispatch.loadbalance.Balancer;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;
import net.flex.dci.otn.controller.gateway.dispatch.selector.PathSelector;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @version 1.0
 * @date 2022/2/15 15:35
 */
@Slf4j
public class MicroserviceSelector {

    private static Balancer microserviceBalancer;

    public static void setBalancer(Balancer balancer) {
        microserviceBalancer = balancer;
    }


    /**
     * get direct service from path
     *
     * @param path
     * @return
     */
    public static ServerInstance selectByPath(String path) {
        log.debug("select live instance from path:{}", path);

        PathSelector pathSelector = new PathSelector();
        List<ServerInstance> serverInstance = pathSelector.selectByPath(path);
        //todo: load balancer method to get one instance
        return microserviceBalancer.select(serverInstance);

    }
}
