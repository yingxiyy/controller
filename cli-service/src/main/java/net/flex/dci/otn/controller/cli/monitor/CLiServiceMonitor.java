package net.flex.dci.otn.controller.cli.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.core.handler.ZkEventHandler;
import net.flex.dci.otn.controller.cli.component.CliSessionCacheHandler;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.springframework.stereotype.Component;

import java.util.List;

import static net.flex.dci.otn.controller.cli.utils.CliServiceUtils.CLI_SERVICE_MODULE;

/**
 *
 * @version 1.0
 * @date 9/18/2025 10:41 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CLiServiceMonitor implements ZkEventHandler {

    private final CliSessionCacheHandler cliSessionCacheHandler;

    @Override
    public void handle(Type type, InstanceDetails detail) {
        log.debug(
                "start to handle the cli service module change change type:{} and instance details is:{}",
                type, detail);
        switch (type) {
            case NODE_ADDED:
                log.debug("a new cli service instance initializing instance details:{}",
                        detail.getId());
                break;
            case NODE_REMOVED:
                log.debug("current cli service instance down:{}", detail.getId());
                cliSessionCacheHandler.handleInstanceRemove(detail);
                break;
        }

    }

    @Override
    public List<String> supportModuleNames() {
        return CLI_SERVICE_MODULE;
    }
}
