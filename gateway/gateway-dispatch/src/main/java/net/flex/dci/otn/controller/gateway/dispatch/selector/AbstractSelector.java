package net.flex.dci.otn.controller.gateway.dispatch.selector;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;

/**
 * @version 1.0
 * @date 2022/2/15 15:44
 */
@Slf4j
public abstract class AbstractSelector implements Selector {

    @Override
    public List<ServerInstance> selectByPath(String path) {
        log.info("select live instance by path:{} ", path);
        return null;
    }


    @Override
    public List<ServerInstance> selectByName(String name) {
        log.info("select live instance by server name:{}", name);
        return null;
    }

    protected ServerInstance convert2ServerInstance(InstanceDetails instanceDetails, String path) {
        return ServerInstance.builder().ip(instanceDetails.getMyIp())
                .port(instanceDetails.getPort())
                .path(path).build();
    }
}
